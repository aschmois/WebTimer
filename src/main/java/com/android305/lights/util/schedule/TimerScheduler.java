package com.android305.lights.util.schedule;

import com.android305.lights.util.Log;

import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class TimerScheduler {

    protected static org.quartz.Scheduler sched;

    public TimerScheduler() {
        try {
            ZoneId z = ZoneId.systemDefault();
            LocalDate today = LocalDate.now(z);
            LocalDateTime tomorrowMidnight = LocalDateTime.of(today, LocalTime.MIDNIGHT).plusDays(1);
            sched = new StdSchedulerFactory().getScheduler();
            sched.start();
            JobDetail job = newJob(DailyTask.class).withIdentity("dailyTask").usingJobData(DailyTask.FIRST_TIME, false).build();
            Trigger trigger = newTrigger().withIdentity("dailyTaskTrigger")
                                          .startAt(Date.from(tomorrowMidnight.atZone(z).toInstant()))
                                          .withSchedule(simpleSchedule().withIntervalInHours(24).repeatForever())
                                          .build();
            sched.scheduleJob(job, trigger);
            Log.d("Scheduled Daily Task at: " + tomorrowMidnight.toString());
            refreshTimerNow();
        } catch (SchedulerException e) {
            Log.e(e);
            System.exit(1);
        }
    }

    public void refreshTimerNow() throws SchedulerException {
        sched.clear();
        sched.scheduleJob(newJob(DailyTask.class).usingJobData(DailyTask.FIRST_TIME, true).build(), newTrigger().startNow().build());
    }

    public void stop() {
        try {
            sched.clear();
            sched.shutdown(true);
        } catch (SchedulerException e) {
            Log.e(e);
        }
    }

    public boolean isRunning() {
        try {
            return sched.isStarted();
        } catch (SchedulerException e) {
            Log.e(e);
        }
        return false;
    }
}