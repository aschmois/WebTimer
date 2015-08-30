package com.android305.lights.util.schedule;

import com.android305.lights.util.Log;
import com.android305.lights.util.sqlite.table.Timer;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class DailyTask implements Job {

    public DailyTask() {
    }

    private static ArrayList<JobKey> scheduledTasks = new ArrayList<>();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
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
            for (Timer t : day) {
                try {
                    Date start = getDate(t.getStart());
                    Date end = getDate(t.getEnd());
                    if (t.getRGB() == null) {
                        {
                            JobBuilder jobBuilder = newJob(LampTask.class);
                            jobBuilder.usingJobData(LampTask.GROUP_ID, t.getInternalGroupId());
                            jobBuilder.usingJobData(LampTask.START_LAMP, true);
                            jobBuilder.usingJobData(LampTask.TIMER_ID, t.getId());
                            JobDetail job = jobBuilder.build();
                            TriggerBuilder builder = newTrigger();
                            builder.startAt(start);
                            builder.withSchedule(simpleSchedule().withMisfireHandlingInstructionNextWithRemainingCount());
                            sched.scheduleJob(job, builder.build());
                            scheduledTasks.add(job.getKey());
                            Log.v("Scheduled Lamp Task for group `" + t.getInternalGroupId() + "` to turn on at: " + start.toString());
                            //TODO: Misfired tasks (usually tasks that run at midnight)
                            //TODO: Turn on lamp if server was restarted and lamp should be on
                        }
                        {
                            JobBuilder jobBuilder = newJob(LampTask.class);
                            jobBuilder.usingJobData(LampTask.GROUP_ID, t.getInternalGroupId());
                            jobBuilder.usingJobData(LampTask.START_LAMP, false);
                            jobBuilder.usingJobData(LampTask.TIMER_ID, t.getId());
                            JobDetail job = jobBuilder.build();
                            TriggerBuilder builder = newTrigger();
                            builder.startAt(end);
                            builder.withSchedule(simpleSchedule().withMisfireHandlingInstructionNextWithRemainingCount());
                            sched.scheduleJob(job, builder.build());
                            scheduledTasks.add(job.getKey());
                            Log.v("Scheduled Lamp Task for group `" + t.getInternalGroupId() + "` to turn off at: " + end.toString());
                            //TODO: Misfired tasks (usually tasks that run at midnight)
                            //TODO: Turn off lamp if server was restarted and lamp should be off
                        }
                    }
                    //TODO: RGB Lamps
                } catch (SchedulerException e) {
                    Log.e(e);
                }
            }
            for (Timer t : dayBefore) {
                try {
                    Date start = getDate(t.getStart());
                    Date end = getDate(t.getEnd());
                    if (start.getTime() - end.getTime() > 0) {
                        if (t.getRGB() == null) {
                            {
                                JobBuilder jobBuilder = newJob(LampTask.class);
                                jobBuilder.usingJobData(LampTask.GROUP_ID, t.getInternalGroupId());
                                jobBuilder.usingJobData(LampTask.START_LAMP, false);
                                jobBuilder.usingJobData(LampTask.TIMER_ID, t.getId());
                                JobDetail job = jobBuilder.build();
                                TriggerBuilder builder = newTrigger();
                                builder.startAt(end);
                                builder.withSchedule(simpleSchedule().withMisfireHandlingInstructionNextWithRemainingCount());
                                sched.scheduleJob(job, builder.build());
                                scheduledTasks.add(job.getKey());
                                Log.v("Scheduled Lamp Task for group `" + t.getInternalGroupId() + "` to turn off at: " + end.toString());
                                //TODO: Misfired tasks (usually tasks that run at midnight)
                                //TODO: Turn off lamp if server was restarted and lamp should be off
                            }
                        }
                    }
                    //TODO: RGB Lamps
                } catch (SchedulerException e) {
                    Log.e(e);
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
