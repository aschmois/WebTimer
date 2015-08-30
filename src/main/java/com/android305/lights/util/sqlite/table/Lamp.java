package com.android305.lights.util.sqlite.table;

import com.android305.lights.util.ConnectionResponse;
import com.android305.lights.util.Log;
import com.android305.lights.util.sqlite.SQLConnection;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Lamp {
    public final static int STATUS_OFF = 0;
    public final static int STATUS_ON = 1;
    public final static int STATUS_PENDING = 2;
    public final static int STATUS_ERROR = 3;

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
    private int status;
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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
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

    public Group getGroup() throws SQLException {
        if (group == null)
            group = Group.DBHelper.get(internalGroupId);
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

        public static Lamp[] getAll() throws SQLException {
            Statement selectGroupStmt = c.createStatement();
            ResultSet rs = selectGroupStmt.executeQuery("SELECT * FROM `lamp`;");
            ArrayList<Lamp> lamps = new ArrayList<>();
            while (rs.next()) {
                Lamp lamp = resultSetToLamp(rs);
                lamps.add(lamp);
            }
            selectGroupStmt.close();
            if (lamps.size() > 0) {
                return lamps.toArray(new Lamp[lamps.size()]);
            }
            return null;
        }

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

        public static Lamp getByName(String name) throws SQLException {
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

        public static Lamp[] getByGroupId(int groupId) throws SQLException {
            PreparedStatement selectStmt = c.prepareStatement("SELECT * FROM `lamp` WHERE `GROUP` = ?");
            selectStmt.setInt(1, groupId);
            ResultSet rs = selectStmt.executeQuery();
            ArrayList<Lamp> lamps = new ArrayList<>();
            while (rs.next()) {
                Lamp lamp = resultSetToLamp(rs);
                lamps.add(lamp);
            }
            selectStmt.close();
            if (lamps.size() > 0) {
                return lamps.toArray(new Lamp[lamps.size()]);
            }
            return null;
        }

        public static Lamp commit(Lamp lamp) throws SQLException, SQLConnection.SQLUniqueException {
            apply(lamp);
            Lamp l = getByName(lamp.name);
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

        public static void update(Lamp lamp) throws SQLException {
            PreparedStatement updateStmt = c.prepareStatement("UPDATE `lamp` SET `NAME` = ?, `IP_ADDRESS` = ?, `STATUS` = ?, `INVERT` = ?, `ERROR` = ?, `GROUP` = ? WHERE `ID` = ?;");
            updateStmt.setString(1, lamp.name);
            updateStmt.setString(2, lamp.ipAddress);
            updateStmt.setInt(3, lamp.status);
            updateStmt.setBoolean(4, lamp.invert);
            updateStmt.setString(5, lamp.error);
            updateStmt.setInt(6, lamp.internalGroupId);
            updateStmt.setInt(7, lamp.id);
            updateStmt.executeUpdate();
            updateStmt.close();
        }

        public static Lamp resultSetToLamp(ResultSet rs) throws SQLException {
            Lamp lamp = new Lamp();
            lamp.setId(rs.getInt("ID"));
            lamp.setName(rs.getString("NAME"));
            lamp.setIpAddress(rs.getString("IP_ADDRESS"));
            lamp.setStatus(rs.getInt("STATUS"));
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
            lamp.setStatus(parsed.getInt("status"));
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

    public void connect(boolean startLamp, int tries) throws SQLException {
        setStatus(Lamp.STATUS_PENDING);
        setError(null);
        Lamp.DBHelper.update(this);
        HttpURLConnection conn = null;
        try {
            String param;
            if (startLamp) {
                if (invert)
                    param = "0";
                else
                    param = "1";
            } else {
                if (invert)
                    param = "1";
                else
                    param = "0";
            }
            Log.d("Trying to " + (startLamp ? "turn on" : "turn off") + " the lamp at " + ipAddress);
            conn = (HttpURLConnection) new URL("http://" + ipAddress + "/gpio/" + param).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int response;
            if ((response = conn.getResponseCode()) != HttpURLConnection.HTTP_OK) {
                Log.e("Lamp is throwing an HTTP error: " + response);
                if (tries > 0) {
                    connect(startLamp, tries - 1);
                    return;
                }
                setStatus(Lamp.STATUS_ERROR);
                setError("HTTP Error: " + response);
            } else {
                Log.d((startLamp ? "Turned on" : "Turned off") + " the lamp at " + ipAddress);
                setStatus(startLamp ? Lamp.STATUS_ON : Lamp.STATUS_OFF);
                setError(null);
            }
        } catch (java.net.SocketTimeoutException e) {
            Log.w("Lamp timeout retrying... (" + tries + " tries left)", e);
            if (tries > 0) {
                connect(startLamp, tries - 1);
                return;
            }
            setStatus(Lamp.STATUS_ERROR);
            setError(e.getClass() + ": " + e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e("Can't reach lamp", e);
            setStatus(Lamp.STATUS_ERROR);
            setError(e.getClass() + ": " + e.getLocalizedMessage());
        } finally {
            if (conn != null)
                try {
                    conn.disconnect();
                } catch (Exception e) {
                }
        }
        Lamp.DBHelper.update(this);
    }

    public ConnectionResponse retrieveStatus(int tries) {
        ConnectionResponse connectionResponse = new ConnectionResponse();
        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            Log.d("Getting status of the lamp at " + "http://" + ipAddress + "/gpio/status");
            conn = (HttpURLConnection) new URL("http://" + ipAddress + "/gpio/status").openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int response;
            if ((response = conn.getResponseCode()) != HttpURLConnection.HTTP_OK) {
                Log.e("Lamp is throwing an HTTP error: " + response);
                if (tries > 0)
                    return retrieveStatus(tries - 1);
                connectionResponse.setError("HTTP Error: " + response);
                connectionResponse.setStatus(Lamp.STATUS_ERROR);
            } else {
                is = conn.getInputStream();
                String resp = IOUtils.toString(is, Charset.forName("UTF-8")).split("\n")[2].replace("</html>", "").trim(); //TODO: Update arduino code to remove html code
                Log.v(resp);
                int status = Integer.parseInt(resp);
                Log.d("The lamp at " + ipAddress + " is `" + ((status == 1 && !invert) || (status == 0 && invert) ? "On" : "Off") + "`");
                connectionResponse.setError(null);
                connectionResponse.setStatus((status == 1 && !invert) || (status == 0 && invert) ? Lamp.STATUS_ON : Lamp.STATUS_OFF);
            }
        } catch (java.net.SocketTimeoutException e) {
            Log.w("Lamp timeout retrying... (" + tries + " tries left) | " + e.getLocalizedMessage());
            if (tries > 0)
                return retrieveStatus(tries - 1);
            else
                Log.e(e);
            connectionResponse.setError(e.getClass() + ": " + e.getLocalizedMessage());
            connectionResponse.setStatus(Lamp.STATUS_ERROR);
        } catch (IOException | NumberFormatException e) {
            Log.e("Can't reach lamp", e);
            connectionResponse.setError(e.getClass() + ": " + e.getLocalizedMessage());
            connectionResponse.setStatus(Lamp.STATUS_ERROR);
        } finally {
            IOUtils.closeQuietly(is);
            try {
                if (conn != null)
                    conn.disconnect();
            } catch (Exception e) {
            }
        }
        return connectionResponse;
    }
}
