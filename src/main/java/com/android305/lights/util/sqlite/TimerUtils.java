package com.android305.lights.util.sqlite;

import com.android305.lights.ServerHandler;
import com.android305.lights.util.SessionResponse;
import com.android305.lights.util.sqlite.table.Group;
import com.android305.lights.util.sqlite.table.Lamp;
import com.android305.lights.util.sqlite.table.Timer;

import org.json.JSONObject;

import java.sql.SQLException;
import java.sql.Time;

public class TimerUtils {
    public static SessionResponse getTimer(JSONObject args) throws SQLException {
        int id = args.getJSONObject("timer").getInt("id");
        Timer timer = Timer.DBHelper.get(id);
        if (timer != null) {
            return new SessionResponse(ServerHandler.TIMER_GET_SUCCESS, false, "", timer.getParsed());
        } else {
            return new SessionResponse(ServerHandler.TIMER_GET_DOES_NOT_EXIST, true, "Timer with id `" + id + "` does not exist.");
        }
    }

    public static SessionResponse addTimer(long sessionId, JSONObject args) throws SQLException {
        Timer timer = Timer.getTimer(args.getJSONObject("timer"));
        int groupId = 0;
        try {
            timer = Timer.DBHelper.commit(timer);
            groupId = timer.getInternalGroupId();
            JSONObject data = new JSONObject();
            data.put("timer", timer.getParsed());
            return new SessionResponse(ServerHandler.TIMER_ADD_SUCCESS, false, "Timer added.", data);
        } finally {
            if (groupId != 0) {
                Group group = Group.DBHelper.getWithLampsAndTimers(groupId);
                ServerHandler.refreshGroup(sessionId, group);
            }
        }
    }

    public static SessionResponse editTimer(long sessionId, JSONObject args) throws SQLException {
        Timer timer = Timer.getTimer(args.getJSONObject("timer"));
        int groupId;
        if (timer.getInternalGroupId() > 0) {
            groupId = timer.getInternalGroupId();
        } else {
            return new SessionResponse(ServerHandler.TIMER_EDIT_GROUP_DOES_NOT_EXIST, true, "Group for timer does not exists.");
        }
        try {
            Timer.DBHelper.update(timer);
            JSONObject data = new JSONObject();
            data.put("timer", timer.getParsed());
            return new SessionResponse(ServerHandler.TIMER_EDIT_SUCCESS, false, "Timer edited.", data);
        } finally {
            Group group = Group.DBHelper.getWithLampsAndTimers(groupId);
            ServerHandler.refreshGroup(sessionId, group);
        }
    }

    public static SessionResponse deleteTimer(long sessionId, JSONObject args) throws SQLException {
        Timer timer = Timer.getTimer(args.getJSONObject("timer"));
        try {
            Timer.DBHelper.delete(timer);
            return new SessionResponse(ServerHandler.TIMER_DELETE_SUCCESS, false, "Timer deleted.");
        } finally {
            Group group = Group.DBHelper.getWithLampsAndTimers(timer.getInternalGroupId());
            ServerHandler.refreshGroup(sessionId, group);
        }
    }
}
