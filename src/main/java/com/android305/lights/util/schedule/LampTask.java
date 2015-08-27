package com.android305.lights.util.schedule;

import com.android305.lights.util.Log;
import com.android305.lights.util.sqlite.SQLConnection;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class LampTask implements InterruptableJob {
    public final static String GROUP_ID = "GROUP_ID";
    public final static String START_LAMP = "START_LAMP";

    public LampTask() {
    }

    private volatile Thread thisThread;
    private volatile String error;

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        if (thisThread != null) {
            thisThread.interrupt();
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        thisThread = Thread.currentThread();
        try {
            int groupId = (int) context.getJobDetail().getJobDataMap().get(GROUP_ID);
            boolean startLamp = (boolean) context.getJobDetail().getJobDataMap().get(START_LAMP);
            SQLConnection c = new SQLConnection();
            Statement stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT `IP_ADDRESS`, `INVERT` FROM `lamp` WHERE `GROUP` = " + groupId + ";");
            while (rs.next()) {
                String ip = rs.getString("IP_ADDRESS");
                boolean invert = rs.getBoolean("INVERT");
                if (ip != null) {
                    stmt = c.createStatement();
                    String sql = "UPDATE `lamp` set `STATUS` = 2, `ERROR` = NULL WHERE `GROUP`=" + groupId + ";";
                    stmt.executeUpdate(sql);
                    boolean connected = connect(ip, startLamp, invert, 3);
                    if (connected) {
                        stmt = c.createStatement();
                        sql = "UPDATE `lamp` set `STATUS` = " + (startLamp ? "1" : "0") + ", `ERROR` = NULL WHERE `GROUP`=" + groupId + ";";
                        stmt.executeUpdate(sql);
                    } else {
                        stmt = c.createStatement();
                        sql = "UPDATE `lamp` set `STATUS` = 3, `ERROR` = '" + error + "' WHERE `GROUP`=" + groupId + ";";
                        stmt.executeUpdate(sql);
                    }
                } else {
                    Log.w("Can't find lamp from group id: " + groupId);
                }
            }
            stmt.close();
            c.close();
        } catch (SQLException e) {
            Log.e(e);
        }
    }

    private boolean connect(String ip, boolean startLamp, boolean invert, int tries) {
        try {
            String param;
            if (startLamp) {
                if (invert) param = "0";
                else param = "1";
            } else {
                if (invert) param = "1";
                else param = "0";
            }
            Log.d("Trying to " + (startLamp ? "turn on" : "turn off") + " the lamp at " + ip);
            HttpURLConnection conn = (HttpURLConnection) new URL("http://" + ip + "/gpio/" + param).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int response;
            if ((response = conn.getResponseCode()) != HttpURLConnection.HTTP_OK) {
                Log.e("Lamp is throwing an HTTP error: " + response);
                error = "HTTP Error: " + response;
                conn.disconnect();
                return false;
            } else {
                Log.d((startLamp ? "Turned on" : "Turned off") + " the lamp at " + ip);
                conn.disconnect();
                return true;
            }
        } catch (java.net.SocketTimeoutException e) {
            Log.w("Lamp timeout retrying... (" + tries + " tries left)", e);
            if (tries > 0) return connect(ip, startLamp, invert, tries - 1);
            error = e.getLocalizedMessage();
        } catch (IOException e) {
            Log.e("Can't reach lamp", e);
            error = e.getLocalizedMessage();
        }
        return false;
    }
}
