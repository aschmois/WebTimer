package com.android305.lights;

import com.android305.lights.util.Log;
import com.android305.lights.util.SessionResponse;
import com.android305.lights.util.sqlite.GroupUtils;
import com.android305.lights.util.sqlite.LampUtils;
import com.android305.lights.util.sqlite.SQLConnection;
import com.android305.lights.util.sqlite.TimerUtils;
import com.android305.lights.util.sqlite.table.Group;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.json.JSONObject;
import org.quartz.SchedulerException;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerHandler extends IoHandlerAdapter {
    private static ConcurrentHashMap<Long, IoSession> openedSessions = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public synchronized static void refreshGroup(Long exclude, Group group) {
        JSONObject groupParsed = new JSONObject();
        if (group == null) {
            groupParsed.put("group", (Object) null);
        } else {
            groupParsed.put("group", group.getParsed());
        }
        SessionResponse response = new SessionResponse(GROUP_REFRESH, false, "", groupParsed);

        for (Map.Entry<Long, IoSession> entry : openedSessions.entrySet()) {
            if (exclude == null || !entry.getKey().equals(exclude)) {
                writeJSON(entry.getValue(), -1, response);
            }
        }
    }

    public final static int ERROR_UNKNOWN = 1;

    public final static int AUTH_SUCCESS = 1000;
    public final static int ERROR_FAILED_AUTHENTICATION = 1001;
    public final static int ERROR_NOT_AUTHENTICATED = 1002;

    /* Group */
    public final static int GROUP_SQL_ERROR = 2000;
    public final static int GROUP_REFRESH = 2002;

    public final static int GROUP_ADD_SUCCESS = 2100;
    public final static int GROUP_ALREADY_EXISTS = 2101;

    public final static int GROUP_GET_SUCCESS = 2200;
    public final static int GROUP_GET_DOES_NOT_EXIST = 2201;

    public final static int GROUP_GET_ALL_SUCCESS = 2300;
    public final static int GROUP_GET_ALL_DOES_NOT_EXIST = 2301;

    public final static int GROUP_EDIT_SUCCESS = 2400;

    public final static int GROUP_DELETE_SUCCESS = 2500;

    /* Lamp */
    public final static int LAMP_SQL_ERROR = 3000;

    public final static int LAMP_ADD_SUCCESS = 3100;
    public final static int LAMP_ALREADY_EXISTS = 3101;

    public final static int LAMP_GET_SUCCESS = 3200;
    public final static int LAMP_GET_DOES_NOT_EXIST = 3201;

    public final static int LAMP_TOGGLE_SUCCESS = 3300;
    public final static int LAMP_TOGGLE_DOES_NOT_EXIST = 3301;

    public final static int LAMP_EDIT_SUCCESS = 3400;
    public final static int LAMP_EDIT_GROUP_DOES_NOT_EXIST = 3401;

    public final static int LAMP_DELETE_SUCCESS = 3500;

    /* Timer */
    public final static int TIMER_SQL_ERROR = 4000;

    public final static int TIMER_ADD_SUCCESS = 4100;

    public final static int TIMER_GET_SUCCESS = 4200;
    public final static int TIMER_GET_DOES_NOT_EXIST = 4201;

    public final static int TIMER_EDIT_SUCCESS = 4400;
    public final static int TIMER_EDIT_GROUP_DOES_NOT_EXIST = 4401;

    public final static int TIMER_DELETE_SUCCESS = 4500;

    private final String password;
    private HashMap<Long, Boolean> authenticated = new HashMap<>();
    private SQLConnection c;

    public ServerHandler(String password) {
        this.password = password;
        this.c = SQLConnection.getInstance();
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        Log.e(cause);
    }

    @Override
    public void messageReceived(IoSession session, Object encryptedMessage) throws Exception {
        Runnable run = () -> {
            String decryptedMessage = Server.enc.decrypt(encryptedMessage.toString().trim());
            if (!decryptedMessage.equals("ping")) {
                Log.d("REQUEST: " + decryptedMessage);
            } else {
                session.write(enc("pong"));
                return;
            }
            JSONObject args = new JSONObject(decryptedMessage);
            String action = args.getString("action");
            int actionId = args.getInt("action_id");
            switch (action) {
                case "authenticate":
                    if (!authenticated.containsKey(session.getId())) {
                        if (password.equals(args.getString("password"))) {
                            authenticated.put(session.getId(), true);
                            writeJSON(session, actionId, new SessionResponse(AUTH_SUCCESS, false, "Welcome"));
                        } else {
                            authenticated.put(session.getId(), false);
                            writeJSON(session, actionId, new SessionResponse(ERROR_FAILED_AUTHENTICATION, true, "Incorrect password"));
                        }
                    }
                    return;
                case "quit":
                    Log.v("Closing connection '" + session.getId() + "'...");
                    authenticated.put(session.getId(), false);
                    session.close(true);
                    return;
            }
            if (authenticated.containsKey(session.getId())) {
                switch (action) {
                    case "lamp":
                        lampAction(session, actionId, args);
                        break;
                    case "group":
                        groupAction(session, actionId, args);
                        break;
                    case "timer":
                        timerAction(session, actionId, args);
                        break;
                    default:
                        writeJSON(session, actionId, new SessionResponse(ERROR_UNKNOWN, true, "Unknown command: " + action));
                        break;
                }
            } else {
                writeJSON(session, actionId, new SessionResponse(ERROR_NOT_AUTHENTICATED, true, "Not authenticated"));
            }
        };
        new Thread(run).start();
    }

    private void lampAction(IoSession session, int actionId, JSONObject args) {
        try {
            switch (args.getString("secondary_action")) {
                case "add":
                    writeJSON(session, actionId, LampUtils.addLamp(session.getId(), args));
                    break;
                case "get":
                    writeJSON(session, actionId, LampUtils.getLamp(args));
                    break;
                case "edit":
                    writeJSON(session, actionId, LampUtils.editLamp(session.getId(), args));
                    break;
                case "delete":
                    writeJSON(session, actionId, LampUtils.deleteLamp(session.getId(), args));
                    break;
                case "toggle":
                    writeJSON(session, actionId, LampUtils.toggleLamp(session.getId(), args));
                    break;
                default:
                    writeJSON(session, actionId, new SessionResponse(ERROR_UNKNOWN, true, "set secondary_action to: add,get,edit,delete,toggle"));
                    break;
            }
        } catch (SQLException e) {
            Log.e(e);
            writeJSON(session, actionId, new SessionResponse(LAMP_SQL_ERROR, true, e.getMessage()));
        }
    }

    private void timerAction(IoSession session, int actionId, JSONObject args) {
        try {
            switch (args.getString("secondary_action")) {
                case "add":
                    writeJSON(session, actionId, TimerUtils.addTimer(session.getId(), args));
                    break;
                case "get":
                    writeJSON(session, actionId, TimerUtils.getTimer(args));
                    break;
                case "edit":
                    writeJSON(session, actionId, TimerUtils.editTimer(session.getId(), args));
                    break;
                case "delete":
                    writeJSON(session, actionId, TimerUtils.deleteTimer(session.getId(), args));
                    break;
                default:
                    writeJSON(session, actionId, new SessionResponse(ERROR_UNKNOWN, true, "set secondary_action to: add,get,edit,delete"));
                    break;
            }
            Server.server.getTimer().refreshTimerNow();
        } catch (SQLException | SchedulerException e) {
            Log.e(e);
            writeJSON(session, actionId, new SessionResponse(TIMER_SQL_ERROR, true, e.getMessage()));
        }
    }

    private void groupAction(IoSession session, int actionId, JSONObject args) {
        try {
            switch (args.getString("secondary_action")) {
                case "add":
                    writeJSON(session, actionId, GroupUtils.addGroup(session.getId(), args));
                    break;
                case "get":
                    writeJSON(session, actionId, GroupUtils.getGroup(args));
                    break;
                case "get-all":
                    writeJSON(session, actionId, GroupUtils.getGroups());
                    break;
                case "edit":
                    writeJSON(session, actionId, GroupUtils.editGroup(session.getId(), args));
                    break;
                case "delete":
                    writeJSON(session, actionId, GroupUtils.deleteGroup(session.getId(), args));
                    break;
                default:
                    writeJSON(session, actionId, new SessionResponse(ERROR_UNKNOWN, true, "set secondary_action to: add,get,get-all,update,delete"));
                    break;
            }
        } catch (SQLException e) {
            Log.e(e);
            writeJSON(session, actionId, new SessionResponse(GROUP_SQL_ERROR, true, e.getMessage()));
        }
    }

    private static String enc(String msg) throws EncryptionOperationNotPossibleException {
        return Server.enc.encrypt(msg);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        Log.v("Connection '" + session.getId() + "' closed.");
        c.close();
        openedSessions.remove(session.getId());
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        if (session.getIdleCount(status) > 10) {
            Log.v("Connection Idle. Closing Connection '" + session.getId() + "'");
            session.close(true);
        }
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        openedSessions.put(session.getId(), session);
        Log.d("New connection: " + session.getId());
        DateFormat format = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy", Locale.US);
        LocalDateTime now = LocalDateTime.now();
        Date out = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        session.write(enc(format.format(out)));
        session.getReadMessages();
    }

    private static void writeJSON(IoSession session, int actionId, SessionResponse response) throws EncryptionOperationNotPossibleException {
        if (session != null) {
            JSONObject json = new JSONObject();
            json.put("action_id", actionId);
            json.put("error", response.isError());
            json.put("code", response.getCode());
            json.put("message", response.getMessage());
            if (response.getData() != null)
                json.put("data", response.getData());
            if (Log.VERBOSE) {
                Log.v("RESPONSE: " + json.toString());
            }
            session.write(enc(json.toString()));
        }
    }

}