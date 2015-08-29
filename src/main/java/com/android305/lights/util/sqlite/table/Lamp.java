package com.android305.lights.util.sqlite.table;

import com.android305.lights.util.sqlite.SQLConnection;

import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Lamp {
    public final static String QUERY = "CREATE TABLE IF NOT EXISTS `lamp` " +
            "(`ID` INTEGER PRIMARY KEY AUTOINCREMENT, " +
            " `NAME`           CHAR(20)  NOT NULL," +
            " `IP_ADDRESS`     CHAR(15)  NOT NULL," +
            " `STATUS`         INTEGER   NOT NULL," +
            " `INVERT`         INTEGER   NOT NULL," +
            " `ERROR`          TEXT              ," +
            " `GROUP`          INTEGER   NOT NULL);";

    private int id;
    private String name;
    private String ipAddress;
    private boolean status;
    private boolean invert;
    private String error = null;
    private Group group;

    private int internalGroupId;

    public Lamp() {
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

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Group getGroup() {
        //TODO: retrieve group if not yet loaded
        return group;
    }

    public int getInternalGroupId() {
        return internalGroupId;
    }

    public void setInternalGroupId(int internalGroupId) {
        this.internalGroupId = internalGroupId;
    }

    public static class DBHelper {
        private static SQLConnection c = SQLConnection.getInstance();

        public static Lamp get(int id) throws SQLException {
            PreparedStatement selectLampsStmt = c.prepareStatement("SELECT * FROM `lamp` WHERE `ID` = ?");
            selectLampsStmt.setInt(1, id);
            ResultSet rs = selectLampsStmt.executeQuery();
            if (rs.next()) {
                Lamp lamp = resultSetToLamp(rs);
                selectLampsStmt.close();
                return lamp;
            }
            return null;
        }

        public static Lamp get(String name) throws SQLException {
            PreparedStatement selectLampsStmt = c.prepareStatement("SELECT * FROM `lamp` WHERE `NAME` = ?");
            selectLampsStmt.setString(1, name);
            ResultSet rs = selectLampsStmt.executeQuery();
            if (rs.next()) {
                Lamp lamp = resultSetToLamp(rs);
                selectLampsStmt.close();
                return lamp;
            }
            return null;
        }

        public static Lamp commit(Lamp lamp) throws SQLException, SQLConnection.SQLUniqueException {
            apply(lamp);
            Lamp l = get(lamp.name);
            if (l == null)
                throw new SQLException("Lamp with the name of `" + lamp.name + "` was to be inserted but was never inserted.");
            return l;
        }

        public static void apply(Lamp lamp) throws SQLException, SQLConnection.SQLUniqueException {
            PreparedStatement selectLampStmt = c.prepareStatement("SELECT `ID` FROM `lamp` WHERE `NAME` = ?");
            selectLampStmt.setString(1, lamp.name);
            ResultSet rs = selectLampStmt.executeQuery();
            if (!rs.next()) {
                selectLampStmt.close();
                String sql = "INSERT INTO `lamp` (`NAME`,`IP_ADDRESS`,`STATUS`,`INVERT`,`GROUP`) VALUES (?, ?, ?, ?, ?);";
                PreparedStatement insertLampStmt = c.prepareStatement(sql);
                insertLampStmt.setString(1, lamp.name);
                insertLampStmt.setString(2, lamp.ipAddress);
                insertLampStmt.setInt(3, lamp.invert ? 1 : 0);
                insertLampStmt.setInt(4, lamp.invert ? 1 : 0);
                insertLampStmt.setInt(5, lamp.internalGroupId);
                insertLampStmt.executeUpdate();
                insertLampStmt.close();
            } else {
                selectLampStmt.close();
                throw new SQLConnection.SQLUniqueException("lamp", "NAME", lamp.name);
            }
        }

        public static Lamp resultSetToLamp(ResultSet rs) throws SQLException {
            Lamp lamp = new Lamp();
            lamp.setId(rs.getInt("ID"));
            lamp.setName(rs.getString("NAME"));
            lamp.setIpAddress(rs.getString("IP_ADDRESS"));
            lamp.setStatus(rs.getInt("STATUS") == 1);
            lamp.setInvert(rs.getInt("INVERT") == 1);
            lamp.setError(rs.getString("ERROR"));
            lamp.internalGroupId = rs.getInt("GROUP");
            return lamp;
        }
    }

    public JSONObject getParsed() {
        JSONObject lamp = new JSONObject();
        lamp.put("id", id);
        lamp.put("name", name);
        lamp.put("ip", ipAddress);
        lamp.put("status", status);
        lamp.put("invert", invert);
        lamp.put("error", error);
        lamp.put("group", internalGroupId);
        return lamp;
    }

    public static Lamp getLamp(JSONObject parsed) {
        Lamp lamp = new Lamp();
        if (parsed.has("id"))
            lamp.setId(parsed.getInt("id"));
        if (parsed.has("name"))
            lamp.setName(parsed.getString("name"));
        if (parsed.has("ip"))
            lamp.setIpAddress(parsed.getString("ip"));
        if (parsed.has("status"))
            lamp.setStatus(parsed.getBoolean("status"));
        if (parsed.has("invert"))
            lamp.setInvert(parsed.getBoolean("invert"));
        if (parsed.has("error"))
            lamp.setError(parsed.getString("error"));
        if (parsed.has("group"))
            lamp.setInternalGroupId(parsed.getInt("group"));
        return lamp;
    }

    @Override
    public String toString() {
        return "Lamp{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", status=" + status +
                ", invert=" + invert +
                ", error='" + error + '\'' +
                ", internalGroupId=" + internalGroupId +
                '}';
    }
}
