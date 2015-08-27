package com.android305.lights.util.schedule;

import com.android305.lights.util.Log;
import com.android305.lights.util.sqlite.SQLConnection;
import org.quartz.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
            Scheduler sched = Timer.sched;
            for (JobKey k : scheduledTasks) {
                try {
                    sched.interrupt(k);
                    sched.deleteJob(k);
                } catch (SchedulerException e) {
                    Log.e(e);
                }
            }
            scheduledTasks.clear();
            SQLConnection c = new SQLConnection();
            Statement stmt = c.createStatement();
            Calendar calendar = Calendar.getInstance();
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            String day = null;
            switch (dayOfWeek) {
                case Calendar.SUNDAY:
                    day = "SUNDAY";
                    break;
                case Calendar.MONDAY:
                    day = "MONDAY";
                    break;
                case Calendar.TUESDAY:
                    day = "TUESDAY";
                    break;
                case Calendar.WEDNESDAY:
                    day = "WEDNESDAY";
                    break;
                case Calendar.THURSDAY:
                    day = "THURSDAY";
                    break;
                case Calendar.FRIDAY:
                    day = "FRIDAY";
                    break;
                case Calendar.SATURDAY:
                    day = "SATURDAY";
                    break;
                default:
                    Log.e("This is embarrassing but we seem to have lost track of time. (Java has no idea what today is)");
                    System.exit(1);
                    break;
            }
            ResultSet rs = stmt.executeQuery("SELECT `GROUP`,`START`,`END` FROM `timer` WHERE `" + day + "` = 1 AND `RGB` IS NULL;");
            while (rs.next()) {
                try {
                    int id = rs.getInt("GROUP");
                    DateFormat df = new SimpleDateFormat("hh:mm:ss");
                    Date start = getDate(df.parse(rs.getString("START")));
                    Date end = getDate(df.parse(rs.getString(("END"))));
                    {
                        JobBuilder jobBuilder = newJob(LampTask.class);
                        jobBuilder.usingJobData(LampTask.GROUP_ID, id);
                        jobBuilder.usingJobData(LampTask.START_LAMP, true);
                        JobDetail job = jobBuilder.build();
                        TriggerBuilder builder = newTrigger();
                        builder.startAt(start);
                        builder.withSchedule(simpleSchedule().withMisfireHandlingInstructionNextWithRemainingCount());
                        sched.scheduleJob(job, builder.build());
                        scheduledTasks.add(job.getKey());
                        Log.v("Scheduled Lamp Task for group `" + id + "` to turn on at: " + start.toString());
                        //TODO: Misfired tasks (usually tasks that run at midnight)
                        //TODO: Turn on lamp if server was restarted and lamp should be on
                    }
                    {
                        JobBuilder jobBuilder = newJob(LampTask.class);
                        jobBuilder.usingJobData(LampTask.GROUP_ID, id);
                        jobBuilder.usingJobData(LampTask.START_LAMP, false);
                        JobDetail job = jobBuilder.build();
                        TriggerBuilder builder = newTrigger();
                        builder.startAt(end);
                        builder.withSchedule(simpleSchedule().withMisfireHandlingInstructionNextWithRemainingCount());
                        sched.scheduleJob(job, builder.build());
                        scheduledTasks.add(job.getKey());
                        Log.v("Scheduled Lamp Task for group `" + id + "` to turn off at: " + end.toString());
                        //TODO: Misfired tasks (usually tasks that run at midnight)
                        //TODO: Turn off lamp if server was restarted and lamp should be off
                    }
                } catch (SchedulerException | ParseException e) {
                    Log.e(e);
                }
            }
            rs.close();

            //TODO: RGB Lamps
            stmt.close();
            c.close();
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
