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
    public final static String QUERY = "CREATE TABLE IF NOT EXISTS `group` " +
            "(`ID` INTEGER PRIMARY KEY AUTOINCREMENT, " +
            " `NAME`     CHAR(35) UNIQUE  NOT NULL);";

    private int id;
    private String name;

    private Lamp[] lamps;

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
        if (lamps == null) attachLamps();
        return lamps;
    }

    private void attachLamps() {
        try {
            lamps = DBHelper.getLamps(this);
        } catch (SQLException e) {
            Log.e(e);
            Log.w("Call DBHelper.getWithLamps(int) instead of DBHelper.get(int) to catch this error");
        }
    }

    public static class DBHelper {
        private static SQLConnection c = SQLConnection.getInstance();

        public static Group[] getAll() throws SQLException {
            Statement selectGroupStmt = c.createStatement();
            ResultSet rs = selectGroupStmt.executeQuery("SELECT * FROM `group`;");
            ArrayList<Group> groups = new ArrayList<>();
            while (rs.next()) {
                Group group = resultSetToGroup(rs);
                group.lamps = getLamps(group);
                groups.add(group);
            }
            selectGroupStmt.close();
            if (groups.size() > 0) {
                return groups.toArray(new Group[groups.size()]);
            }
            return null;
        }

        public static Group get(int id) throws SQLException {
            PreparedStatement selectGroupStmt = c.prepareStatement("SELECT * FROM `group` WHERE `ID` = ?;");
            selectGroupStmt.setInt(1, id);
            ResultSet rs = selectGroupStmt.executeQuery();
            if (rs.next()) {
                Group group = resultSetToGroup(rs);
                selectGroupStmt.close();
                return group;
            }
            return null;
        }

        public static Group getWithLamps(int id) throws SQLException {
            PreparedStatement selectGroupStmt = c.prepareStatement("SELECT * FROM `group` WHERE `ID` = ?;");
            selectGroupStmt.setInt(1, id);
            ResultSet rs = selectGroupStmt.executeQuery();
            if (rs.next()) {
                Group group = resultSetToGroup(rs);
                selectGroupStmt.close();
                group.lamps = getLamps(group);
                return group;
            }
            return null;
        }

        private static Lamp[] getLamps(Group group) throws SQLException {
            PreparedStatement selectLampsStmt = c.prepareStatement("SELECT * FROM `lamp` WHERE `GROUP` = ?;");
            selectLampsStmt.setInt(1, group.id);
            ResultSet rs = selectLampsStmt.executeQuery();
            ArrayList<Lamp> lamps = new ArrayList<>();
            while (rs.next()) {
                lamps.add(Lamp.DBHelper.resultSetToLamp(rs));
            }
            return lamps.toArray(new Lamp[lamps.size()]);
        }

        public static Group get(String name) throws SQLException {
            PreparedStatement selectLampsStmt = c.prepareStatement("SELECT * FROM `group` WHERE `NAME` = ?;");
            selectLampsStmt.setString(1, name);
            ResultSet rs = selectLampsStmt.executeQuery();
            if (rs.next()) {
                Group group = resultSetToGroup(rs);
                selectLampsStmt.close();
                return group;
            }
            return null;
        }

        public static Group commit(Group group) throws SQLException, SQLConnection.SQLUniqueException {
            apply(group);
            Group g = get(group.name);
            if (g == null) throw new SQLException("Group with the name of `" + group.name + "` was to be inserted but was never inserted.");
            return g;
        }

        public static void apply(Group group) throws SQLException, SQLConnection.SQLUniqueException {
            PreparedStatement selectLampStmt = c.prepareStatement("SELECT `ID` FROM `group` WHERE `NAME` = ?;");
            selectLampStmt.setString(1, group.name);
            ResultSet rs = selectLampStmt.executeQuery();
            if (!rs.next()) {
                selectLampStmt.close();
                String sql = "INSERT INTO `group` (`NAME`) VALUES (?);";
                PreparedStatement insertLampStmt = c.prepareStatement(sql);
                insertLampStmt.setString(1, group.name);
                insertLampStmt.executeUpdate();
                insertLampStmt.close();
            } else {
                selectLampStmt.close();
                throw new SQLConnection.SQLUniqueException("group", "NAME", group.name);
            }
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
        return group;
    }

    @Override
    public String toString() {
        return "Group{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", lamps=" + Arrays.toString(lamps) +
                '}';
    }
}
