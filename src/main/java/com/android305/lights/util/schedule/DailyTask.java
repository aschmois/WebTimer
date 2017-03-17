package com.android305.lights.util.schedule;

import com.android305.lights.ServerHandler;
import com.android305.lights.util.Log;
import com.android305.lights.util.sqlite.table.Timer;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class DailyTask implements Job {
    final static String FIRST_TIME = "first_time";

    public DailyTask() {
    }

    private static ArrayList<JobKey> scheduledTasks = new ArrayList<>();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        boolean firstTime = (boolean) context.getJobDetail().getJobDataMap().get(FIRST_TIME);
        try {
            Scheduler sched = TimerScheduler.sched;
            for (JobKey k : scheduledTasks) {
                try {
                    sched.interrupt(k);
                    sched.deleteJob(k);
                } catch (SchedulerException e) {
                    Log.e(e);
                }
            }
            scheduledTasks.clear();
            for (Timer t : Timer.DBHelper.getAll()) {
                //clear status of all timers
                t.setStatus(0);
                Timer.DBHelper.update(t);
            }

            if (firstTime) {
                Timer[] all = Timer.DBHelper.getAll();
                for (Timer t : all) {
                    String days;
                    if (t.isEveryday()) {
                        days = "Everyday";
                    } else if (t.isWeekdays()) {
                        days = "Weekdays";
                    } else if (t.isWeekend()) {
                        days = "Weekend";
                    } else {
                        ArrayList<String> list = new ArrayList<>();
                        if (t.isSunday())
                            list.add("S");
                        if (t.isMonday())
                            list.add("M");
                        if (t.isTuesday())
                            list.add("T");
                        if (t.isWednesday())
                            list.add("W");
                        if (t.isThursday())
                            list.add("TH");
                        if (t.isFriday())
                            list.add("F");
                        if (t.isSaturday())
                            list.add("SA");
                        days = String.join(",", list);
                    }
                    Log.v("Group `" + t.getInternalGroupId() + "` schedule. Start: " + t.getStart() + " End: " + t.getEnd() + ". Days: " + days);
                }
            }
            ArrayList<Integer> ids = new ArrayList<>();
            Calendar calendar = Calendar.getInstance();
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            Timer[] dayBefore;
            Timer[] day;
            switch (dayOfWeek) {
                case Calendar.SUNDAY:
                    dayBefore = Timer.DBHelper.getSaturday();
                    day = Timer.DBHelper.getSunday();
                    break;
                case Calendar.MONDAY:
                    dayBefore = Timer.DBHelper.getSunday();
                    day = Timer.DBHelper.getMonday();
                    break;
                case Calendar.TUESDAY:
                    dayBefore = Timer.DBHelper.getMonday();
                    day = Timer.DBHelper.getTuesday();
                    break;
                case Calendar.WEDNESDAY:
                    dayBefore = Timer.DBHelper.getTuesday();
                    day = Timer.DBHelper.getWednesday();
                    break;
                case Calendar.THURSDAY:
                    dayBefore = Timer.DBHelper.getWednesday();
                    day = Timer.DBHelper.getThursday();
                    break;
                case Calendar.FRIDAY:
                    dayBefore = Timer.DBHelper.getThursday();
                    day = Timer.DBHelper.getFriday();
                    break;
                case Calendar.SATURDAY:
                    dayBefore = Timer.DBHelper.getFriday();
                    day = Timer.DBHelper.getSaturday();
                    break;
                default:
                    Log.e("This is embarrassing but we seem to have lost track of time. (Java has no idea what today is)");
                    System.exit(1);
                    return;
            }
            if (day != null) {
                HashMap<Integer, Timer> timerByGroup = new HashMap<>();
                HashMap<Integer, Boolean> statuses = new HashMap<>();
                LocalDateTime n = LocalDateTime.now();
                Date now = Date.from(n.atZone(ZoneId.systemDefault()).toInstant());
                for (Timer t : day) {
                    ids.add(t.getId());
                    try {
                        Date start = getToday(t.getStart());
                        Date end = getToday(t.getEnd());
                        boolean status;
                        if (end.before(start)) {
                            status = now.after(start);
                        } else {
                            status = now.after(start) && now.before(end);
                        }
                        t.setStatus(status ? 1 : 0);
                        Timer.DBHelper.update(t);
                        if (firstTime) {
                            if (statuses.containsKey(t.getInternalGroupId())) {
                                if (status) {
                                    statuses.put(t.getInternalGroupId(), true);
                                    timerByGroup.put(t.getInternalGroupId(), t);
                                }
                            } else {
                                statuses.put(t.getInternalGroupId(), status);
                                timerByGroup.put(t.getInternalGroupId(), t);
                            }
                        }
                        if (t.getRGB() == null) {
                            if (now.before(start)) {
                                schedule(t, true, start, false);
                            }
                            if (now.before(end)) {
                                schedule(t, false, end, false);
                            }
                        }
                        //TODO: RGB Lamps
                    } catch (SchedulerException e) {
                        Log.e(e);
                    }
                }
                for (Timer t : dayBefore) {
                    try {
                        if (ids.contains(t.getId())) {
                            Date start = getToday(t.getStart());
                            Date end = getToday(t.getEnd());
                            if (end.before(start) && (now.before(end) || (isMidnight(end) && isMidnight(now)))) {
                                boolean status = now.after(start);
                                t.setStatus(status ? 1 : 0);
                                Timer.DBHelper.update(t);
                                if (t.getRGB() == null) {
                                    if (!isMidnight(end)) {
                                        schedule(t, false, end, true);
                                    } else if (isMidnight(now)) {
                                        Log.v("Group `" + t.getInternalGroupId() + "` turns off at midnight, so let's do that now.");
                                        sched.scheduleJob(createLampJob(t, false), newTrigger().startNow().build());
                                    }
                                }
                            }
                            //TODO: RGB Lamps
                        }
                    } catch (SchedulerException e) {
                        Log.e(e);
                    }
                }

                for (Integer gId : statuses.keySet()) {
                    Timer t = timerByGroup.get(gId);
                    boolean status = statuses.get(gId);
                    Log.v("Group `" + gId + "` needs to be : " + (status ? "on" : "off"));
                    try {
                        sched.scheduleJob(createLampJob(t, status), newTrigger().startNow().build());
                    } catch (SchedulerException e) {
                        Log.e(e);
                    }
                }
            }
            ServerHandler.refreshGroup(null, null);
        } catch (SQLException e) {
            Log.e(e);
        }
    }

    private void schedule(Timer t, boolean start, Date time, boolean dayBefore) throws SchedulerException {
        Scheduler sched = TimerScheduler.sched;
        JobDetail job = createLampJob(t, start);
        Trigger trigger = newTrigger().startAt(time).withSchedule(simpleSchedule().withMisfireHandlingInstructionNextWithRemainingCount()).build();
        sched.scheduleJob(job, trigger);
        scheduledTasks.add(job.getKey());
        Log.v((dayBefore ? "Day Before: " : "") + "Scheduled Lamp Task for group `" + t.getInternalGroupId() + "` to turn " + (start ? "on" : "off") + " at: " + time.toString());
    }

    private JobDetail createLampJob(Timer t, boolean start) {
        return newJob(LampTask.class).usingJobData(LampTask.GROUP_ID, t.getInternalGroupId()).usingJobData(LampTask.START_LAMP, start).usingJobData(LampTask.TIMER_ID, t.getId()).build();

    }

    private Date getToday(Date time) {
        GregorianCalendar cTime = new GregorianCalendar();
        Calendar today = Calendar.getInstance();
        cTime.setTime(time);
        today.set(Calendar.HOUR_OF_DAY, cTime.get(Calendar.HOUR_OF_DAY));
        today.set(Calendar.MINUTE, cTime.get(Calendar.MINUTE));
        today.set(Calendar.SECOND, cTime.get(Calendar.SECOND));
        return today.getTime();
    }

    private boolean isMidnight(Date time) {
        GregorianCalendar cTime = new GregorianCalendar();
        cTime.setTime(time);
        return cTime.get(Calendar.HOUR_OF_DAY) == 0;

    }
}
