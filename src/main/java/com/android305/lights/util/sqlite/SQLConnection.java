package com.android305.lights.util.sqlite;

import com.android305.lights.util.Log;
import com.android305.lights.util.sqlite.table.Group;
import com.android305.lights.util.sqlite.table.Lamp;
import com.android305.lights.util.sqlite.table.Timer;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;

public class SQLConnection {
    //Hey you, read http://stackoverflow.com/questions/3424156/upgrade-sqlite-database-from-one-version-to-another for some upgrading guidelines
    private final static int DB_VERSION = 1;

    public static class SQLUniqueException extends Exception {
        public SQLUniqueException(String table, String column, String value) {
            super("Column `" + column + "` from table `" + table + "` is unique. Insert value: " + value);
        }
    }

    private static SQLConnection instance = new SQLConnection();

    public static SQLConnection getInstance() {
        return instance;
    }

    private Connection c;

    private SQLConnection() {
        Statement stmt;
        try {
            Log.d("Initializing SQLite database...");
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:timer.db");

            stmt = c.createStatement();
            stmt.executeUpdate(Lamp.QUERY);
            stmt.close();

            stmt = c.createStatement();
            stmt.executeUpdate(Group.QUERY);
            stmt.close();

            stmt = c.createStatement();
            stmt.executeUpdate(Timer.QUERY);
            stmt.close();

            Log.d("SQLite database connected.");

        } catch (ClassNotFoundException | SQLException e) {
            Log.e(e);
            System.exit(1);
        }
    }

    private Connection checkConnection() throws SQLException {
        try {
            if (c == null || c.isClosed() || !c.isValid(3)) {
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:timer.db");
            }
            return c;
        } catch (ClassNotFoundException e) {
            Log.e(e);
        }
        throw new SQLException("Connection could not be completed");
    }

    public static void insertTestData() {
        try {
            instance.checkConnection().close();
        } catch (SQLException ignored) {
        }
        try {
            //noinspection ResultOfMethodCallIgnored
            new File("timer.db").delete();
        } catch (Exception ignored) {
        }
        instance = new SQLConnection();
        try {
            Group frontLamps = new Group();
            frontLamps.setName("Front Lamps");
            frontLamps = Group.DBHelper.commit(frontLamps);

            Group emptyGroup = new Group();
            emptyGroup.setName("Empty Group");
            Group.DBHelper.commit(emptyGroup);

            Lamp lamp = new Lamp();
            lamp.setName("Porch");
            lamp.setIpAddress("192.168.1.20");
            lamp.setInvert(false);
            lamp.setStatus(1);
            lamp.setInternalGroupId(frontLamps.getId());
            Lamp.DBHelper.apply(lamp);

            lamp = new Lamp();
            lamp.setName("Front");
            lamp.setIpAddress("192.168.1.23");
            lamp.setInvert(false);
            lamp.setStatus(1);
            lamp.setInternalGroupId(frontLamps.getId());
            Lamp.DBHelper.apply(lamp);

            /*lamp = new Lamp();
            lamp.setName("Test Lamp");
            lamp.setIpAddress("192.168.1.22");
            lamp.setInvert(false);
            lamp.setStatus(1);
            lamp.setInternalGroupId(testLamps.getId());
            Lamp.DBHelper.apply(lamp);*/

            Timer timer = new Timer();
            timer.setStart(Time.valueOf("04:45:00"));
            timer.setEnd(Time.valueOf("07:00:00"));
            timer.setInternalGroupId(frontLamps.getId());
            Timer.DBHelper.apply(timer);

            timer = new Timer();
            timer.setStart(Time.valueOf("18:00:00"));
            timer.setEnd(Time.valueOf("02:00:00"));
            timer.setInternalGroupId(frontLamps.getId());
            Timer.DBHelper.apply(timer);

            /*timer = new Timer();
            timer.setStart(Time.valueOf("04:30:00"));
            timer.setEnd(Time.valueOf("06:30:00"));
            timer.setInternalGroupId(testLamps.getId());
            Timer.DBHelper.apply(timer);*/
        } catch (SQLException | SQLUniqueException e) {
            Log.e(e);
        }
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return checkConnection().prepareStatement(sql);
    }

    public Statement createStatement() throws SQLException {
        return checkConnection().createStatement();
    }

    public void close() throws SQLException {
        if (c != null && c.isValid(3))
            c.close();
    }
}
