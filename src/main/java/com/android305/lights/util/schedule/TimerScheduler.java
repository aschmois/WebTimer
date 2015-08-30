package com.android305.lights.util.schedule;

import com.android305.lights.util.ConnectionResponse;
import com.android305.lights.util.Log;
import com.android305.lights.util.sqlite.table.Lamp;

import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import java.sql.Date;
import java.sql.SQLException;
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
            LocalTime midnight = LocalTime.MIDNIGHT;
            ZoneId z = ZoneId.systemDefault();
            LocalDate today = LocalDate.now(z);
            LocalDateTime tomorrowMidnight = LocalDateTime.of(today, midnight).plusDays(1);
            sched = new StdSchedulerFactory().getScheduler();
            sched.start();
            JobDetail job = newJob(DailyTask.class).withIdentity("dailyTask").build();
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
        sched.scheduleJob(newJob(DailyTask.class).build(), newTrigger().startNow().build());
        try {
            Thread.sleep(1000);
            // Delay for a little while lamp tasks are scheduled, this delay is not really necessary and not accurate,
            // it's only here to give the cpu a bit of a break
        } catch (InterruptedException e) {
        }
        try {
            Lamp[] lamps = Lamp.DBHelper.getAll();
            if (lamps != null) {
                for (Lamp l : lamps) {
                    ConnectionResponse response = l.retrieveStatus(2);
                    l.setStatus(response.getStatus());
                    l.setError(response.getError());
                    try {
                        Lamp.DBHelper.update(l);
                    } catch (SQLException e) {
                        Log.e(e);
                    }
                }
            }
        } catch (SQLException e) {
            Log.e(e);
        }
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