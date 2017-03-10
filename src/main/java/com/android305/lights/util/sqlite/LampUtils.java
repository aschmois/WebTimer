package com.android305.lights.util.sqlite;

import com.android305.lights.ServerHandler;
import com.android305.lights.util.SessionResponse;
import com.android305.lights.util.sqlite.table.Group;
import com.android305.lights.util.sqlite.table.Lamp;

import org.json.JSONObject;

import java.sql.SQLException;

public class LampUtils {
    public static SessionResponse getLamp(JSONObject args) throws SQLException {
        int id = args.getJSONObject("lamp").getInt("id");
        Lamp lamp = Lamp.DBHelper.get(id);
        if (lamp != null) {
            return new SessionResponse(ServerHandler.LAMP_GET_SUCCESS, false, "", lamp.getParsed());
        } else {
            return new SessionResponse(ServerHandler.LAMP_GET_DOES_NOT_EXIST, true, "Lamp with id `" + id + "` does not exist.");
        }
    }

    public static SessionResponse addLamp(long sessionId, JSONObject args) throws SQLException {
        Lamp lampFromJSON = Lamp.getLamp(args.getJSONObject("lamp"));
        String name = lampFromJSON.getName();
        String ip = lampFromJSON.getIpAddress();
        boolean invert = lampFromJSON.isInvert();
        int groupId;
        if (lampFromJSON.getInternalGroupId() > 0) {
            groupId = lampFromJSON.getInternalGroupId();
        } else {
            try {
                Group group = Group.DBHelper.commit(new Group(name));
                groupId = group.getId();
            } catch (SQLConnection.SQLUniqueException e) {
                return new SessionResponse(ServerHandler.GROUP_ALREADY_EXISTS, true, "Group " + name + " already exists.");
            }
        }
        try {
            Lamp lamp = new Lamp();
            lamp.setName(name);
            lamp.setIpAddress(ip);
            lamp.setStatus(invert ? 1 : 0);
            lamp.setInvert(invert);
            lamp.setInternalGroupId(groupId);
            lamp = Lamp.DBHelper.commit(lamp);
            if (lamp != null) {
                JSONObject data = new JSONObject();
                data.put("lamp", lamp.getParsed());
                return new SessionResponse(ServerHandler.LAMP_ADD_SUCCESS, false, "Lamp " + name + " added.", data);
            } else {
                return new SessionResponse(ServerHandler.LAMP_SQL_ERROR, true, "Unknown SQL error. Lamp is missing.");
            }
        } catch (SQLConnection.SQLUniqueException e) {
            return new SessionResponse(ServerHandler.LAMP_ALREADY_EXISTS, true, "Lamp " + name + " already exists.");
        } finally {
            Group group = Group.DBHelper.getWithLampsAndTimers(groupId);
            ServerHandler.refreshGroup(sessionId, group);
        }
    }

    public static SessionResponse editLamp(long sessionId, JSONObject args) throws SQLException {
        Lamp lamp = Lamp.getLamp(args.getJSONObject("lamp"));
        String name = lamp.getName();
        String ip = lamp.getIpAddress();
        boolean invert = lamp.isInvert();
        int groupId;
        if (lamp.getInternalGroupId() > 0) {
            groupId = lamp.getInternalGroupId();
        } else {
            return new SessionResponse(ServerHandler.LAMP_EDIT_GROUP_DOES_NOT_EXIST, true, "Group for lamp" + name + " does not exists.");
        }
        try {
            lamp.setName(name);
            lamp.setIpAddress(ip);
            lamp.setInvert(invert);
            lamp.setInternalGroupId(groupId);
            Lamp.DBHelper.update(lamp);
            JSONObject data = new JSONObject();
            data.put("lamp", lamp.getParsed());
            return new SessionResponse(ServerHandler.LAMP_EDIT_SUCCESS, false, "Lamp " + name + " edited.", data);
        } finally {
            Group group = Group.DBHelper.getWithLampsAndTimers(groupId);
            ServerHandler.refreshGroup(sessionId, group);
        }
    }

    public static SessionResponse deleteLamp(long sessionId, JSONObject args) throws SQLException {
        Lamp lamp = Lamp.getLamp(args.getJSONObject("lamp"));
        try {
            Lamp.DBHelper.delete(lamp);
            return new SessionResponse(ServerHandler.LAMP_DELETE_SUCCESS, false, "Lamp " + lamp.getName() + " deleted.");
        } finally {
            Group group = Group.DBHelper.getWithLampsAndTimers(lamp.getInternalGroupId());
            ServerHandler.refreshGroup(sessionId, group);
        }
    }

    public static SessionResponse toggleLamp(long sessionId, JSONObject args) throws SQLException {
        int groupId = 0;
        try {
            int id = args.getJSONObject("lamp").getInt("id");
            Lamp lamp = Lamp.DBHelper.get(id);
            if (lamp != null) {
                groupId = lamp.getInternalGroupId();
                lamp.connect(lamp.getStatus() != Lamp.STATUS_ON, 3);
                JSONObject parsed = new JSONObject();
                parsed.put("lamp", lamp.getParsed());
                return new SessionResponse(ServerHandler.LAMP_TOGGLE_SUCCESS, false, "", parsed);
            } else {
                return new SessionResponse(ServerHandler.LAMP_TOGGLE_DOES_NOT_EXIST, true, "Lamp with id `" + id + "` does not exist.");
            }
        } finally {
            if (groupId != 0) {
                Group group = Group.DBHelper.getWithLampsAndTimers(groupId);
                ServerHandler.refreshGroup(sessionId, group);
            }
        }
    }
}
