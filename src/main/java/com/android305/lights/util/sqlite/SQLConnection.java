package com.android305.lights.util.sqlite;

import com.android305.lights.util.Log;
import com.android305.lights.util.sqlite.table.Group;
import com.android305.lights.util.sqlite.table.Lamp;
import com.sun.istack.internal.NotNull;

import java.sql.*;

public class SQLConnection {
    //Hey you, read http://stackoverflow.com/questions/3424156/upgrade-sqlite-database-from-one-version-to-another for some upgrading guidelines
    private final static int DB_VERSION = 1;

    final static String TABLE_TIMER = "CREATE TABLE IF NOT EXISTS `timer` " +
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
            " `RGB`            TEXT              ," +
            " `GROUP`          INTEGER   NOT NULL);";

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

    public SQLConnection() {
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
            stmt.executeUpdate(TABLE_TIMER);
            stmt.close();

            Log.d("SQLite database connected.");

        } catch (ClassNotFoundException | SQLException e) {
            Log.e(e);
            System.exit(1);
        }
    }

    @NotNull
    private Connection checkConnection() throws SQLException {
        try {
            if (c == null || !c.isValid(3)) {
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
            Statement stmt = instance.createStatement();
            String sql = "INSERT INTO `group` (NAME) VALUES ('Front Lamps');";
            stmt.executeUpdate(sql);

            stmt = instance.createStatement();
            sql = "INSERT INTO `group` (NAME) VALUES ('Test Lamp');";
            stmt.executeUpdate(sql);

            stmt = instance.createStatement();
            sql = "INSERT INTO lamp (NAME,IP_ADDRESS,STATUS,INVERT,`GROUP`) VALUES ('Porch Lamp', '192.168.1.20', 1, 1, 1);";
            stmt.executeUpdate(sql);

            stmt = instance.createStatement();
            sql = "INSERT INTO lamp (NAME,IP_ADDRESS,STATUS,INVERT,`GROUP`) VALUES ('Front Lamps', '192.168.1.21', 1, 1, 1);";
            stmt.executeUpdate(sql);

            stmt = instance.createStatement();
            sql = "INSERT INTO lamp (NAME,IP_ADDRESS,STATUS,INVERT,`GROUP`) VALUES ('Test Lamp', '192.168.1.22', 1, 1, 2);";
            stmt.executeUpdate(sql);

            stmt = instance.createStatement();
            sql = "INSERT INTO timer (START,END,SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,`GROUP`) VALUES ('04:30:00', '06:30:00', 1, 1,1,1,1,1,1,1);";
            stmt.executeUpdate(sql);

            stmt = instance.createStatement();
            sql = "INSERT INTO timer (START,END,SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,`GROUP`) VALUES ('04:30:00', '06:30:00', 1, 1,1,1,1,1,1,2);";
            stmt.executeUpdate(sql);

            stmt = instance.createStatement();
            sql = "INSERT INTO timer (START,END,SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,`GROUP`) VALUES ('20:30:00', '20:31:00', 1, 1,1,1,1,1,1,2);";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
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
        if (c != null && c.isValid(3)) c.close();
    }
}
