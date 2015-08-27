package com.android305.lights.util.schedule;

import com.android305.lights.util.Log;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
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

public class Timer {

    protected static Scheduler sched;

    public Timer() {
        try {
            LocalTime midnight = LocalTime.MIDNIGHT;
            ZoneId z = ZoneId.systemDefault();
            LocalDate today = LocalDate.now(z);
            LocalDateTime tomorrowMidnight = LocalDateTime.of(today, midnight).plusDays(1);
            sched = new StdSchedulerFactory().getScheduler();
            sched.start();
            JobDetail job = newJob(DailyTask.class).withIdentity("dailyTask").build();
            Trigger trigger = newTrigger().withIdentity("dailyTaskTrigger").startAt(Date.from(tomorrowMidnight.atZone(z).toInstant())).withSchedule(simpleSchedule().withIntervalInHours(24)
                    .repeatForever()).build();
            sched.scheduleJob(job, trigger);
            Log.d("Scheduled Daily Task at: " + tomorrowMidnight.toString());
            refreshTimerNow();

        } catch (SchedulerException e) {
            Log.e(e);
            System.exit(1);
        }
    }

    public void refreshTimerNow() throws SchedulerException {
        sched.scheduleJob(newJob(DailyTask.class).build(), newTrigger().startNow().build());
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