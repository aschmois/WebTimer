package com.android305.lights.util.sqlite;

import com.android305.lights.ServerHandler;
import com.android305.lights.util.SessionResponse;
import com.android305.lights.util.sqlite.table.Group;
import com.android305.lights.util.sqlite.table.Lamp;
import org.json.JSONObject;

import java.sql.SQLException;

public class LampUtils {
    public static SessionResponse addLamp(JSONObject args) throws SQLException {
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
            lamp.setStatus(invert);
            lamp.setInvert(invert);
            lamp.setInternalGroupId(groupId);
            Lamp.DBHelper.apply(lamp);

            Group group = Group.DBHelper.getWithLamps(groupId);
            if (group != null) {
                return new SessionResponse(ServerHandler.LAMP_ADD_SUCCESS, false, "Lamp " + name + " added.", group.getParsed());
            } else {
                return new SessionResponse(ServerHandler.LAMP_SQL_ERROR, true, "Unknown SQL error. Group is missing.");
            }
        } catch (SQLConnection.SQLUniqueException e) {
            return new SessionResponse(ServerHandler.LAMP_ALREADY_EXISTS, true, "Lamp " + name + " already exists.");
        }
    }

    public static SessionResponse getLamp(JSONObject args) throws SQLException {
        Lamp lampFromJSON = Lamp.getLamp(args.getJSONObject("lamp"));
        Lamp lamp = Lamp.DBHelper.get(lampFromJSON.getId());
        if (lamp != null) {
            return new SessionResponse(ServerHandler.LAMP_GET_SUCCESS, false, "", lamp.getParsed());
        } else {
            return new SessionResponse(ServerHandler.LAMP_GET_DOES_NOT_EXIST, true, "Lamp with id `" + lampFromJSON.getId() + "` does not exist.");
        }
    }
}
