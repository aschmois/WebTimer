package com.android305.lights.util.sqlite.table;

import com.android305.lights.util.sqlite.SQLConnection;

import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;

public class Timer {
    public final static String QUERY = "CREATE TABLE IF NOT EXISTS `timer` " +
            "(`ID` INTEGER PRIMARY KEY AUTOINCREMENT, " +
            " `START`          TEXT      NOT NULL," +
            " `END`            TEXT      NOT NULL," +
            " `SUNDAY`         INTEGER   NOT NULL," +
            " `MONDAY`         INTEGER   NOT NULL," +
            " `TUESDAY`        INTEGER   NOT NULL," +
            " `WEDNESDAY`      INTEGER   NOT NULL," +
            " `THURSDAY`       INTEGER   NOT NULL," +
            " `FRIDAY`         INTEGER   NOT NULL," +
            " `SATURDAY`       INTEGER   NOT NULL," +
            " `STATUS`         INTEGER   NOT NULL," +
            " `RGB`            TEXT              ," +
            " `GROUP`          INTEGER   NOT NULL);";

    private int id;
    private Time start;
    private Time end;
    private boolean sunday = true;
    private boolean monday = true;
    private boolean tuesday = true;
    private boolean wednesday = true;
    private boolean thursday = true;
    private boolean friday = true;
    private boolean saturday = true;
    private int status = 0;
    private String RGB = null;
    private Group group;
    private int internalGroupId;

    public Timer() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Time getStart() {
        return start;
    }

    public void setStart(Time start) {
        this.start = start;
    }

    public Time getEnd() {
        return end;
    }

    public void setEnd(Time end) {
        this.end = end;
    }

    public boolean isSunday() {
        return sunday;
    }

    public void setSunday(boolean sunday) {
        this.sunday = sunday;
    }

    public boolean isMonday() {
        return monday;
    }

    public void setMonday(boolean monday) {
        this.monday = monday;
    }

    public boolean isTuesday() {
        return tuesday;
    }

    public void setTuesday(boolean tuesday) {
        this.tuesday = tuesday;
    }

    public boolean isWednesday() {
        return wednesday;
    }

    public void setWednesday(boolean wednesday) {
        this.wednesday = wednesday;
    }

    public boolean isThursday() {
        return thursday;
    }

    public void setThursday(boolean thursday) {
        this.thursday = thursday;
    }

    public boolean isFriday() {
        return friday;
    }

    public void setFriday(boolean friday) {
        this.friday = friday;
    }

    public boolean isSaturday() {
        return saturday;
    }

    public void setSaturday(boolean saturday) {
        this.saturday = saturday;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getRGB() {
        return RGB;
    }

    public void setRGB(String RGB) {
        this.RGB = RGB;
    }

    public Group getGroup() throws SQLException {
        if (group == null)
            group = Group.DBHelper.get(internalGroupId);
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public int getInternalGroupId() {
        return internalGroupId;
    }

    public void setInternalGroupId(int internalGroupId) {
        this.internalGroupId = internalGroupId;
    }

    public static class DBHelper {
        private static SQLConnection c = SQLConnection.getInstance();

        public static Timer[] getAll() throws SQLException {
            return getAll("SELECT * FROM `timer`;");
        }

        private static Timer[] getAll(String sql) throws SQLException {
            Statement selectStmt = c.createStatement();
            ResultSet rs = selectStmt.executeQuery(sql);
            ArrayList<Timer> timers = new ArrayList<>();
            while (rs.next()) {
                Timer timer = resultSetToTimer(rs);
                timers.add(timer);
            }
            selectStmt.close();
            if (timers.size() > 0) {
                return timers.toArray(new Timer[timers.size()]);
            }
            return null;
        }

        public static Timer get(int id) throws SQLException {
            PreparedStatement selectStmt = c.prepareStatement("SELECT * FROM `timer` WHERE `ID` = ?;");
            selectStmt.setInt(1, id);
            ResultSet rs = selectStmt.executeQuery();
            if (rs.next()) {
                Timer timer = resultSetToTimer(rs);
                selectStmt.close();
                return timer;
            }
            return null;
        }

        public static Timer[] getSunday() throws SQLException {
            return getAll("SELECT * FROM `timer` WHERE `SUNDAY` = 1;");
        }

        public static Timer[] getMonday() throws SQLException {
            return getAll("SELECT * FROM `timer` WHERE `MONDAY` = 1;");
        }

        public static Timer[] getTuesday() throws SQLException {
            return getAll("SELECT * FROM `timer` WHERE `TUESDAY` = 1;");
        }

        public static Timer[] getWednesday() throws SQLException {
            return getAll("SELECT * FROM `timer` WHERE `WEDNESDAY` = 1;");
        }

        public static Timer[] getThursday() throws SQLException {
            return getAll("SELECT * FROM `timer` WHERE `THURSDAY` = 1;");
        }

        public static Timer[] getFriday() throws SQLException {
            return getAll("SELECT * FROM `timer` WHERE `FRIDAY` = 1;");
        }

        public static Timer[] getSaturday() throws SQLException {
            return getAll("SELECT * FROM `timer` WHERE `SATURDAY` = 1;");
        }

        public static Timer commit(Timer timer) throws SQLException {
            apply(timer);
            PreparedStatement selectStmt = c.prepareStatement("SELECT * FROM `timer` ORDER BY `ID` DESC LIMIT 1;");
            ResultSet rs = selectStmt.executeQuery();
            if (rs.next()) {
                Timer t = resultSetToTimer(rs);
                selectStmt.close();
                return t;
            }
            selectStmt.close();
            throw new SQLException("Timer was to be inserted but was never inserted.");
        }

        public static void apply(Timer timer) throws SQLException {
            String sql = "INSERT INTO `timer` (`START`,`END`,`SUNDAY`,`MONDAY`,`TUESDAY`,`WEDNESDAY`,`THURSDAY`,`FRIDAY`,`SATURDAY`,`STATUS`,`RGB`,`GROUP`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?);";
            PreparedStatement insertStmt = c.prepareStatement(sql);
            insertStmt.setString(1, timer.start.toString());
            insertStmt.setString(2, timer.end.toString());
            insertStmt.setBoolean(3, timer.sunday);
            insertStmt.setBoolean(4, timer.monday);
            insertStmt.setBoolean(5, timer.tuesday);
            insertStmt.setBoolean(6, timer.wednesday);
            insertStmt.setBoolean(7, timer.thursday);
            insertStmt.setBoolean(8, timer.friday);
            insertStmt.setBoolean(9, timer.saturday);
            insertStmt.setInt(10, timer.status);
            insertStmt.setString(11, timer.RGB);
            insertStmt.setInt(12, timer.internalGroupId);
            insertStmt.executeUpdate();
            insertStmt.close();
        }

        public static void update(Timer timer) throws SQLException {
            PreparedStatement updateStmt = c.prepareStatement("UPDATE `timer` SET `START` = ?, `END` = ?, `SUNDAY` = ?, `MONDAY` = ?, `TUESDAY` = ?, `WEDNESDAY` = ?, `THURSDAY` = ?, `FRIDAY` = ?, `SATURDAY` = ?, `STATUS` = ?, `RGB` = ?, `GROUP` = ? WHERE `ID` = ?;");
            updateStmt.setString(1, timer.start.toString());
            updateStmt.setString(2, timer.end.toString());
            updateStmt.setBoolean(3, timer.sunday);
            updateStmt.setBoolean(4, timer.monday);
            updateStmt.setBoolean(5, timer.tuesday);
            updateStmt.setBoolean(6, timer.wednesday);
            updateStmt.setBoolean(7, timer.thursday);
            updateStmt.setBoolean(8, timer.friday);
            updateStmt.setBoolean(9, timer.saturday);
            updateStmt.setInt(10, timer.status);
            updateStmt.setString(11, timer.RGB);
            updateStmt.setInt(12, timer.internalGroupId);
            updateStmt.setInt(13, timer.id);
            updateStmt.executeUpdate();
            updateStmt.close();
        }

        public static Timer resultSetToTimer(ResultSet rs) throws SQLException {
            Timer timer = new Timer();
            timer.setId(rs.getInt("ID"));
            timer.setStart(Time.valueOf(rs.getString("START")));
            timer.setEnd(Time.valueOf(rs.getString("END")));
            timer.setSunday(rs.getBoolean("SUNDAY"));
            timer.setMonday(rs.getBoolean("MONDAY"));
            timer.setTuesday(rs.getBoolean("TUESDAY"));
            timer.setWednesday(rs.getBoolean("WEDNESDAY"));
            timer.setThursday(rs.getBoolean("THURSDAY"));
            timer.setFriday(rs.getBoolean("FRIDAY"));
            timer.setSaturday(rs.getBoolean("SATURDAY"));
            timer.setStatus(rs.getInt("STATUS"));
            timer.setRGB(rs.getString("RGB"));
            timer.setInternalGroupId(rs.getInt("GROUP"));
            return timer;
        }

        public static void delete(Timer timer) throws SQLException {
            PreparedStatement deleteStmt = c.prepareStatement("DELETE FROM `timer` WHERE `ID` = ?;");
            deleteStmt.setInt(1, timer.id);
            deleteStmt.executeUpdate();
            deleteStmt.close();
        }
    }

    public JSONObject getParsed() {
        JSONObject timer = new JSONObject();
        timer.put("id", id);
        timer.put("start", start);
        timer.put("end", end);
        timer.put("sunday", sunday);
        timer.put("monday", monday);
        timer.put("tuesday", tuesday);
        timer.put("wednesday", wednesday);
        timer.put("thursday", thursday);
        timer.put("friday", friday);
        timer.put("saturday", saturday);
        timer.put("status", status);
        timer.put("group", internalGroupId);
        return timer;
    }

    public static Timer getTimer(JSONObject parsed) {
        Timer timer = new Timer();
        if (parsed.has("id"))
            timer.setId(parsed.getInt("id"));
        if (parsed.has("start"))
            timer.setStart(Time.valueOf(parsed.getString("start")));
        if (parsed.has("end"))
            timer.setEnd(Time.valueOf(parsed.getString("end")));
        if (parsed.has("sunday"))
            timer.setSunday(parsed.getBoolean("sunday"));
        if (parsed.has("monday"))
            timer.setMonday(parsed.getBoolean("monday"));
        if (parsed.has("tuesday"))
            timer.setTuesday(parsed.getBoolean("tuesday"));
        if (parsed.has("wednesday"))
            timer.setWednesday(parsed.getBoolean("wednesday"));
        if (parsed.has("thursday"))
            timer.setThursday(parsed.getBoolean("thursday"));
        if (parsed.has("friday"))
            timer.setFriday(parsed.getBoolean("friday"));
        if (parsed.has("saturday"))
            timer.setSaturday(parsed.getBoolean("saturday"));
        if (parsed.has("status"))
            timer.setStatus(parsed.getInt("status"));
        if (parsed.has("group"))
            timer.setInternalGroupId(parsed.getInt("group"));
        return timer;
    }

    @Override
    public String toString() {
        return "Timer{" +
                "id=" + id +
                ", start=" + start +
                ", end=" + end +
                ", sunday=" + sunday +
                ", monday=" + monday +
                ", tuesday=" + tuesday +
                ", wednesday=" + wednesday +
                ", thursday=" + thursday +
                ", friday=" + friday +
                ", saturday=" + saturday +
                ", status=" + status +
                ", RGB='" + RGB + '\'' +
                ", internalGroupId=" + internalGroupId +
                '}';
    }
}
