package com.android305.lights.util.schedule;

import com.android305.lights.util.Log;
import com.android305.lights.util.sqlite.SQLConnection;

import org.apache.commons.io.IOUtils;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
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
            ResultSet rs = SQLConnection.getInstance().createStatement().executeQuery("SELECT `ID`,`IP_ADDRESS`,`INVERT` FROM `lamp`;");
            while (rs.next()) {
                int id = rs.getInt("ID");
                String ip = rs.getString("IP_ADDRESS");
                boolean invert = rs.getInt("INVERT") == 1;
                int connected = connect(ip, invert, 3);
                switch (connected) {
                    case 0:
                        if (invert)
                            connected = 1;
                        break;
                    case 1:
                        if (invert)
                            connected = 0;
                        break;
                }
                SQLConnection.getInstance().createStatement().executeUpdate("UPDATE `lamp` set `STATUS` = " + connected + ", `ERROR` = NULL WHERE `ID` = " + id);
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

    private int connect(String ip, boolean invert, int tries) {
        try {
            Log.d("Getting status of the lamp at " + "http://" + ip + "/gpio/status");
            HttpURLConnection conn = (HttpURLConnection) new URL("http://" + ip + "/gpio/status").openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int response;
            if ((response = conn.getResponseCode()) != HttpURLConnection.HTTP_OK) {
                Log.e("Lamp is throwing an HTTP error: " + response);
                conn.disconnect();
                return connect(ip, invert, tries - 1);
            } else {
                int status = 3;
                InputStream is = conn.getInputStream();
                try {
                    String resp = IOUtils.toString(is, Charset.forName("UTF-8")).split("\n")[2].replace("</html>", "").trim(); //TODO: Update arduino code to remove html code
                    Log.v(resp);
                    status = Integer.parseInt(resp);
                    Log.d("The lamp at " + ip + " is `" + ((status == 1 && !invert) || (status == 0 && invert) ? "On" : "Off") + "`");
                } finally {
                    IOUtils.closeQuietly(is);
                }
                conn.disconnect();
                return status;
            }
        } catch (java.net.SocketTimeoutException | NumberFormatException e) {
            Log.w("Lamp timeout retrying... (" + tries + " tries left) | " + e.getMessage());
            if (tries > 0)
                return connect(ip, invert, tries - 1);
            else
                Log.e(e);
        } catch (IOException e) {
            Log.e("Can't reach lamp", e);
        }
        return 3;
    }
}