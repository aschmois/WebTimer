package com.android305.lights.util;

import java.sql.Timestamp;
import java.util.Arrays;

public class Log {
    public static boolean DEBUG = false;
    public static boolean VERBOSE = false;

    /**
     * Needs {@link Log#DEBUG} to be true to print
     *
     * @param debug
     */
    public static void d(Object debug) {
        if (debug == null) printDebugString("null");
        else printDebugString(debug.toString());
    }

    /**
     * Needs {@link Log#DEBUG} to be true to print
     *
     * @param debug
     */
    public static void d(Object[] debug) {
        if (debug == null) printDebugString("null");
        else printDebugString(Arrays.toString(debug));
    }

    public static void w(String message) {
            Timestamp t = new Timestamp(System.currentTimeMillis());
            String time = t.toString();
            if (time.split("\\.")[1].length() == 2) time = time + "0";
            if (time.split("\\.")[1].length() == 1) time = time + "00";
            System.err.println("[" + time + "] [Warning] " + message);
    }

    public static void e(String message) {
            Timestamp t = new Timestamp(System.currentTimeMillis());
            String time = t.toString();
            if (time.split("\\.")[1].length() == 2) time = time + "0";
            if (time.split("\\.")[1].length() == 1) time = time + "00";
            System.err.println("[" + time + "] [Error] " + message);
    }

    public static void e(String msg, Exception e) {
            Timestamp t = new Timestamp(System.currentTimeMillis());
            String time = t.toString();
            if (time.split("\\.")[1].length() == 2) time = time + "0";
            if (time.split("\\.")[1].length() == 1) time = time + "00";
            System.err.println("[" + time + "] [ERROR] " + msg + ": " + e);
            StackTraceElement[] elem = e.getStackTrace();
            for (StackTraceElement el : elem) {
                System.err.println("[" + time + "] \t\tat " + el.toString());
            }
            if (e.getCause() != null) {
                System.err.println("[" + time + "] Caused by: " + e.getCause());
                elem = e.getCause().getStackTrace();
                for (StackTraceElement el : elem) {
                    System.err.println("[" + time + "] \t\tat " + el.toString());
                }
            }
    }

    public static void e(Throwable e) {
            Timestamp t = new Timestamp(System.currentTimeMillis());
            String time = t.toString();
            if (time.split("\\.")[1].length() == 2) time = time + "0";
            if (time.split("\\.")[1].length() == 1) time = time + "00";
            System.err.println("[" + time + "] [ERROR] " + e);
            StackTraceElement[] elem = e.getStackTrace();
            for (StackTraceElement el : elem) {
                System.err.println("[" + time + "] \t\tat " + el.toString());
            }
            if (e.getCause() != null) {
                System.err.println("[" + time + "] Caused by: " + e.getCause());
                elem = e.getCause().getStackTrace();
                for (StackTraceElement el : elem) {
                    System.err.println("[" + time + "] \t\tat " + el.toString());
                }
            }
    }

    public static void i(Object info) {
        if (info == null) printString("null");
        else printString(info.toString());
    }

    public static void i(Object[] info) {
        if (info == null) printString("null");
        else printString(Arrays.toString(info));
    }

    private static void printDebugString(String debug) {
        if (DEBUG) {
                Timestamp t = new Timestamp(System.currentTimeMillis());
                String time = t.toString();
                if (time.split("\\.")[1].length() == 2) time = time + "0";
                if (time.split("\\.")[1].length() == 1) time = time + "00";
                System.err.println("[" + time + "] [DEBUG] " + debug);
        }
    }

    private static void printString(String info) {
            Timestamp t = new Timestamp(System.currentTimeMillis());
            String time = t.toString();
            if (time.split("\\.")[1].length() == 2) time = time + "0";
            if (time.split("\\.")[1].length() == 1) time = time + "00";
            System.out.println("[" + time + "] " + info);
    }

    private static void printVerbose(String info) {
            Timestamp t = new Timestamp(System.currentTimeMillis());
            String time = t.toString();
            if (time.split("\\.")[1].length() == 2) time = time + "0";
            if (time.split("\\.")[1].length() == 1) time = time + "00";
            System.out.println("[" + time + "] [VERBOSE] " + info);
    }

    public static void v(Object info) {
        if (VERBOSE) {
            if (info == null) printVerbose("null");
            else printVerbose(info.toString());
        }
    }

    public static void v(Object[] info) {
        if (VERBOSE) {
            if (info == null) printVerbose("null");
            else printVerbose(Arrays.toString(info));
        }
    }

    public static void w(Exception e) {
            e("[Warning]", e);
    }

    public static void w(String msg, Exception e) {
            e("[Warning] " + msg, e);
    }

}
