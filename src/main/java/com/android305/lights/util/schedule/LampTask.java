package com.android305.lights.util.schedule;

import com.android305.lights.ServerHandler;
import com.android305.lights.util.Log;
import com.android305.lights.util.sqlite.table.Group;
import com.android305.lights.util.sqlite.table.Lamp;
import com.android305.lights.util.sqlite.table.Timer;

import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

import java.sql.SQLException;

public class LampTask implements InterruptableJob {
    public final static String TIMER_ID = "TIMER_ID";
    public final static String GROUP_ID = "GROUP_ID";
    public final static String START_LAMP = "START_LAMP";

    public LampTask() {
    }

    private volatile Thread thisThread;

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
            int timerId = (int) context.getJobDetail().getJobDataMap().get(TIMER_ID);
            int groupId = (int) context.getJobDetail().getJobDataMap().get(GROUP_ID);
            boolean startLamp = (boolean) context.getJobDetail().getJobDataMap().get(START_LAMP);
            Timer timer = Timer.DBHelper.get(timerId);
            if (timer != null) {
                timer.setStatus(startLamp ? 1 : 0);
                Timer.DBHelper.update(timer);
                Lamp[] lamps = Lamp.DBHelper.getByGroupId(groupId);
                Group tmp = null;
                if (lamps != null) {
                    for (Lamp l : lamps) {
                        if (tmp == null) {
                            tmp = l.getGroup();
                            tmp.getLamps();
                            tmp.getTimers();
                        }
                        l.connect(startLamp, 2);
                        if (tmp != l.getGroup()) {
                            ServerHandler.refreshGroup(null, tmp);
                            tmp = l.getGroup();
                            tmp.getLamps();
                            tmp.getTimers();
                        }
                    }
                    if (tmp != null) {
                        ServerHandler.refreshGroup(null, tmp);
                    }
                }
            }
        } catch (SQLException e) {
            Log.e(e);
        }
    }
}
