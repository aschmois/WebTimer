package com.android305.lights.util.schedule;

import com.android305.lights.util.Log;
import com.android305.lights.util.sqlite.table.Timer;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
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
    public final static String FIRST_TIME = "first_time";

    public DailyTask() {
    }

    private static ArrayList<JobKey> scheduledTasks = new ArrayList<>();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        boolean firstTime = (boolean) context.getJobDetail().getJobDataMap().get(FIRST_TIME);
        try {
            org.quartz.Scheduler sched = TimerScheduler.sched;
            for (JobKey k : scheduledTasks) {
                try {
                    sched.interrupt(k);
                    sched.deleteJob(k);
                } catch (SchedulerException e) {
                    Log.e(e);
                }
            }
            scheduledTasks.clear();
            HashMap<Integer, Boolean> timerStatuses = new HashMap<>();
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
                LocalDateTime n = LocalDateTime.now();
                Date now = Date.from(n.atZone(ZoneId.systemDefault()).toInstant());
                for (Timer t : day) {
                    ids.add(t.getId());
                    try {
                        Date start = getDate(t.getStart());
                        Date end = getDate(t.getEnd());
                        boolean status;
                        if (start.getTime() - end.getTime() > 0) {
                            status = now.after(start);
                        } else {
                            status = now.after(start) && now.before(end);
                        }
                        t.setStatus(status ? 1 : 0);
                        Timer.DBHelper.update(t);
                        if (firstTime && status) {
                            JobDetail job = newJob(LampTask.class).usingJobData(LampTask.GROUP_ID, t.getInternalGroupId())
                                                                  .usingJobData(LampTask.START_LAMP, true)
                                                                  .usingJobData(LampTask.TIMER_ID, t.getId())
                                                                  .build();
                            sched.scheduleJob(job, newTrigger().startNow().build());
                            timerStatuses.put(t.getId(), true);
                            //TODO: improve this logic
                        }
                        if (t.getRGB() == null) {
                            {
                                JobDetail job = newJob(LampTask.class).usingJobData(LampTask.GROUP_ID, t.getInternalGroupId())
                                                                      .usingJobData(LampTask.START_LAMP, true)
                                                                      .usingJobData(LampTask.TIMER_ID, t.getId())
                                                                      .build();
                                Trigger trigger = newTrigger().startAt(start).withSchedule(simpleSchedule().withMisfireHandlingInstructionNextWithRemainingCount()).build();
                                sched.scheduleJob(job, trigger);
                                scheduledTasks.add(job.getKey());
                                Log.v("Scheduled Lamp Task for group `" + t.getInternalGroupId() + "` to turn on at: " + start.toString());
                            }
                            {
                                JobDetail job = newJob(LampTask.class).usingJobData(LampTask.GROUP_ID, t.getInternalGroupId())
                                                                      .usingJobData(LampTask.START_LAMP, false)
                                                                      .usingJobData(LampTask.TIMER_ID, t.getId())
                                                                      .build();
                                Trigger trigger = newTrigger().startAt(end).withSchedule(simpleSchedule().withMisfireHandlingInstructionNextWithRemainingCount()).build();
                                sched.scheduleJob(job, trigger);
                                scheduledTasks.add(job.getKey());
                                Log.v("Scheduled Lamp Task for group `" + t.getInternalGroupId() + "` to turn off at: " + end.toString());
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
                            Date start = getDate(t.getStart());
                            Date end = getDate(t.getEnd());
                            if (start.getTime() - end.getTime() > 0) {
                                if (t.getRGB() == null) {
                                    JobDetail job = newJob(LampTask.class).usingJobData(LampTask.GROUP_ID, t.getInternalGroupId())
                                                                          .usingJobData(LampTask.START_LAMP, false)
                                                                          .usingJobData(LampTask.TIMER_ID, t.getId())
                                                                          .build();
                                    Trigger trigger = newTrigger().startAt(end).withSchedule(simpleSchedule().withMisfireHandlingInstructionNextWithRemainingCount()).build();
                                    sched.scheduleJob(job, trigger);
                                    scheduledTasks.add(job.getKey());
                                    Log.v("Scheduled Lamp Task for group `" + t.getInternalGroupId() + "` to turn off at: " + end.toString());
                                }
                            }
                            //TODO: RGB Lamps
                        }
                    } catch (SchedulerException e) {
                        Log.e(e);
                    }
                }
            }
        } catch (SQLException e) {
            Log.e(e);
        }
    }

    private Date getDate(Date time) {
        GregorianCalendar cTime = new GregorianCalendar();
        Calendar today = Calendar.getInstance();
        cTime.setTime(time);
        today.set(Calendar.HOUR_OF_DAY, cTime.get(Calendar.HOUR_OF_DAY));
        today.set(Calendar.MINUTE, cTime.get(Calendar.MINUTE));
        today.set(Calendar.SECOND, cTime.get(Calendar.SECOND));
        return today.getTime();
    }
}
