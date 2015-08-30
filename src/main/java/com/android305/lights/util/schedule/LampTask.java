package com.android305.lights.util.schedule;

import com.android305.lights.util.Log;
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
                Lamp[] lamps = Lamp.DBHelper.getByGroupId(groupId);
                if (lamps != null) {
                    for (Lamp l : lamps) {
                        l.connect(startLamp, 2);
                    }
                }
            }
        } catch (SQLException e) {
            Log.e(e);
        }
    }
}
