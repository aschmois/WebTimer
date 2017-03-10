package com.android305.lights.util.sqlite;

import com.android305.lights.ServerHandler;
import com.android305.lights.util.SessionResponse;
import com.android305.lights.util.sqlite.table.Group;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;

public class GroupUtils {

    public static SessionResponse getGroups() throws SQLException {
        Group[] groups = Group.DBHelper.getAll();
        if (groups != null) {
            JSONObject groupParsed = new JSONObject();
            JSONArray array = new JSONArray();
            for (Group g : groups) {
                array.put(g.getParsed());
            }
            groupParsed.put("groups", array);
            return new SessionResponse(ServerHandler.GROUP_GET_ALL_SUCCESS, false, "", groupParsed);
        } else {
            return new SessionResponse(ServerHandler.GROUP_GET_ALL_DOES_NOT_EXIST, true, "No groups.");
        }
    }

    public static SessionResponse getGroup(JSONObject args) throws SQLException {
        int id = args.getJSONObject("group").getInt("id");
        Group group = Group.DBHelper.get(id);
        if (group != null) {
            return new SessionResponse(ServerHandler.GROUP_GET_SUCCESS, false, "", group.getParsed());
        } else {
            return new SessionResponse(ServerHandler.GROUP_GET_DOES_NOT_EXIST, true, "Group with id `" + id + "` does not exist.");
        }
    }

    public static SessionResponse addGroup(long sessionId, JSONObject args) throws SQLException {
        Group groupFromJson = Group.getGroup(args.getJSONObject("group"));
        String name = groupFromJson.getName();
        Group group = new Group();
        try {
            group.setName(name);
            group = Group.DBHelper.commit(group);
            if (group != null) {
                JSONObject data = new JSONObject();
                data.put("group", group.getParsed());
                return new SessionResponse(ServerHandler.GROUP_ADD_SUCCESS, false, "Group " + name + " added.", data);
            } else {
                return new SessionResponse(ServerHandler.GROUP_SQL_ERROR, true, "Unknown SQL error. Group is missing.");
            }
        } catch (SQLConnection.SQLUniqueException e) {
            return new SessionResponse(ServerHandler.GROUP_ALREADY_EXISTS, true, "Group " + name + " already exists.");
        } finally {
            if (group != null) {
                ServerHandler.refreshGroup(sessionId, group);
            }
        }
    }

    public static SessionResponse editGroup(long sessionId, JSONObject args) throws SQLException {
        Group group = Group.getGroup(args.getJSONObject("group"));
        String name = group.getName();
        try {
            group.setName(name);
            Group.DBHelper.update(group);
            JSONObject data = new JSONObject();
            data.put("group", group.getParsed());
            return new SessionResponse(ServerHandler.GROUP_EDIT_SUCCESS, false, "Group " + name + " edited.", data);
        } finally {
            ServerHandler.refreshGroup(sessionId, group);

        }
    }

    public static SessionResponse deleteGroup(long sessionId, JSONObject args) throws SQLException {
        Group group = Group.getGroup(args.getJSONObject("group"));
        try {
            Group.DBHelper.delete(group);
            return new SessionResponse(ServerHandler.GROUP_DELETE_SUCCESS, false, "Group " + group.getName() + " deleted.");
        } finally {
            ServerHandler.refreshGroup(sessionId, null);
        }
    }
}
