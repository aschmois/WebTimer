package com.android305.lights.util.sqlite.table;

import com.android305.lights.util.Log;
import com.android305.lights.util.sqlite.SQLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

public class Group {
    public final static String QUERY = "CREATE TABLE IF NOT EXISTS `group` " + "(`ID` INTEGER PRIMARY KEY AUTOINCREMENT, " + " `NAME`     CHAR(35) UNIQUE  NOT NULL);";

    private int id;
    private String name;

    private Lamp[] lamps;
    private Timer[] timers;

    public Group() {
    }

    public Group(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Lamp[] getLamps() {
        if (lamps == null)
            attachLamps();
        return lamps;
    }

    private void attachLamps() {
        try {
            lamps = DBHelper.getLamps(this);
        } catch (SQLException e) {
            Log.e(e);
            Log.w("Call DBHelper.getWithLampsAndTimers(int) instead of DBHelper.get(int) to catch this error");
        }
    }

    public Timer[] getTimers() {
        if (timers == null)
            attachTimers();
        return timers;
    }

    private void attachTimers() {
        try {
            timers = DBHelper.getTimers(this);
        } catch (SQLException e) {
            Log.e(e);
            Log.w("Call DBHelper.getWithLampsAndTimers(int) instead of DBHelper.get(int) to catch this error");
        }
    }

    public static Group getGroup(JSONObject parsed) {
        Group group = new Group();
        if (parsed.has("id"))
            group.setId(parsed.getInt("id"));
        if (parsed.has("name"))
            group.setName(parsed.getString("name"));
        return group;
    }

    public static class DBHelper {
        private static SQLConnection c = SQLConnection.getInstance();

        public static Group[] getAll() throws SQLException {
            Statement selectStmt = c.createStatement();
            ResultSet rs = selectStmt.executeQuery("SELECT * FROM `group`;");
            ArrayList<Group> groups = new ArrayList<>();
            while (rs.next()) {
                Group group = resultSetToGroup(rs);
                group.lamps = getLamps(group);
                group.timers = getTimers(group);
                groups.add(group);
            }
            selectStmt.close();
            if (groups.size() > 0) {
                return groups.toArray(new Group[groups.size()]);
            }
            return null;
        }

        public static Group get(int id) throws SQLException {
            PreparedStatement selectStmt = c.prepareStatement("SELECT * FROM `group` WHERE `ID` = ?;");
            selectStmt.setInt(1, id);
            ResultSet rs = selectStmt.executeQuery();
            if (rs.next()) {
                Group group = resultSetToGroup(rs);
                selectStmt.close();
                return group;
            }
            return null;
        }

        public static Group getWithLampsAndTimers(int id) throws SQLException {
            PreparedStatement selectStmt = c.prepareStatement("SELECT * FROM `group` WHERE `ID` = ?;");
            selectStmt.setInt(1, id);
            ResultSet rs = selectStmt.executeQuery();
            if (rs.next()) {
                Group group = resultSetToGroup(rs);
                selectStmt.close();
                group.lamps = getLamps(group);
                group.timers = getTimers(group);
                return group;
            }
            return null;
        }

        private static Lamp[] getLamps(Group group) throws SQLException {
            PreparedStatement selectStmt = c.prepareStatement("SELECT * FROM `lamp` WHERE `GROUP` = ?;");
            selectStmt.setInt(1, group.id);
            ResultSet rs = selectStmt.executeQuery();
            ArrayList<Lamp> lamps = new ArrayList<>();
            while (rs.next()) {
                lamps.add(Lamp.DBHelper.resultSetToLamp(rs));
            }
            return lamps.toArray(new Lamp[lamps.size()]);
        }

        private static Timer[] getTimers(Group group) throws SQLException {
            PreparedStatement selectStmt = c.prepareStatement("SELECT * FROM `timer` WHERE `GROUP` = ?;");
            selectStmt.setInt(1, group.id);
            ResultSet rs = selectStmt.executeQuery();
            ArrayList<Timer> timers = new ArrayList<>();
            while (rs.next()) {
                timers.add(Timer.DBHelper.resultSetToTimer(rs));
            }
            return timers.toArray(new Timer[timers.size()]);
        }

        public static Group get(String name) throws SQLException {
            PreparedStatement selectStmt = c.prepareStatement("SELECT * FROM `group` WHERE `NAME` = ?;");
            selectStmt.setString(1, name);
            ResultSet rs = selectStmt.executeQuery();
            if (rs.next()) {
                Group group = resultSetToGroup(rs);
                selectStmt.close();
                return group;
            }
            return null;
        }

        public static void delete(Group group) throws SQLException {
            PreparedStatement deleteStmt = c.prepareStatement("DELETE FROM `group` WHERE `ID` = ?;");
            deleteStmt.setInt(1, group.id);
            deleteStmt.executeUpdate();
            deleteStmt.close();
        }

        public static Group commit(Group group) throws SQLException, SQLConnection.SQLUniqueException {
            apply(group);
            Group g = get(group.name);
            if (g == null)
                throw new SQLException("Group with the name of `" + group.name + "` was to be inserted but was never inserted.");
            return g;
        }

        public static void apply(Group group) throws SQLException, SQLConnection.SQLUniqueException {
            PreparedStatement selectStmt = c.prepareStatement("SELECT `ID` FROM `group` WHERE `NAME` = ?;");
            selectStmt.setString(1, group.name);
            ResultSet rs = selectStmt.executeQuery();
            if (!rs.next()) {
                selectStmt.close();
                String sql = "INSERT INTO `group` (`NAME`) VALUES (?);";
                PreparedStatement insertStmt = c.prepareStatement(sql);
                insertStmt.setString(1, group.name);
                insertStmt.executeUpdate();
                insertStmt.close();
            } else {
                selectStmt.close();
                throw new SQLConnection.SQLUniqueException("group", "NAME", group.name);
            }
        }

        public static void update(Group group) throws SQLException {
            PreparedStatement updateStmt = c.prepareStatement("UPDATE `group` SET `NAME` = ? WHERE `ID` = ?;");
            updateStmt.setString(1, group.name);
            updateStmt.setInt(2, group.id);
            updateStmt.executeUpdate();
            updateStmt.close();
        }

        public static Group resultSetToGroup(ResultSet rs) throws SQLException {
            Group group = new Group();
            group.setId(rs.getInt("ID"));
            group.setName(rs.getString("NAME"));
            return group;
        }
    }

    public JSONObject getParsed() {
        JSONObject group = new JSONObject();
        group.put("id", id);
        group.put("name", name);
        if (lamps != null) {
            JSONArray parsedLamps = new JSONArray();
            for (Lamp lamp : lamps) {
                parsedLamps.put(lamp.getParsed());
            }
            group.put("lamps", parsedLamps);
        }
        if (timers != null) {
            JSONArray parsedTimers = new JSONArray();
            for (Timer timer : timers) {
                parsedTimers.put(timer.getParsed());
            }
            group.put("timers", parsedTimers);
        }
        return group;
    }

    @Override
    public String toString() {
        return "Group{" + "id=" + id + ", name='" + name + '\'' + ", lamps=" + Arrays.toString(lamps) + ", timers=" + Arrays.toString(timers) + '}';
    }
}
