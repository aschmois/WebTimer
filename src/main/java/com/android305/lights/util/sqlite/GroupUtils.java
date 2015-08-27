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
}
