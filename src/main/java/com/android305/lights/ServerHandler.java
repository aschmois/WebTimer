package com.android305.lights;

import com.android305.lights.util.Log;
import com.android305.lights.util.SessionResponse;
import com.android305.lights.util.sqlite.GroupUtils;
import com.android305.lights.util.sqlite.LampUtils;
import com.android305.lights.util.sqlite.SQLConnection;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class ServerHandler extends IoHandlerAdapter {
    public final static int ERROR_UNKNOWN = 1;

    public final static int AUTH_SUCCESS = 1000;
    public final static int ERROR_FAILED_AUTHENTICATION = 1001;
    public final static int ERROR_NOT_AUTHENTICATED = 1002;

    /* Group */
    public final static int GROUP_ERROR_USAGE = 2000;
    public final static int GROUP_SQL_ERROR = 2001;
    public final static int GROUP_REFRESH = 2002;

    public final static int GROUP_ADD_SUCCESS = 2100;
    public final static int GROUP_ALREADY_EXISTS = 2101;

    public final static int GROUP_GET_SUCCESS = 2200;
    public final static int GROUP_GET_DOES_NOT_EXIST = 2201;

    public final static int GROUP_GET_ALL_SUCCESS = 2300;
    public final static int GROUP_GET_ALL_DOES_NOT_EXIST = 2301;

    /* Lamp */
    public final static int LAMP_SQL_ERROR = 3000;

    public final static int LAMP_ADD_SUCCESS = 3100;
    public final static int LAMP_ALREADY_EXISTS = 3101;

    public final static int LAMP_GET_SUCCESS = 3200;
    public final static int LAMP_GET_DOES_NOT_EXIST = 3201;

    public final static int LAMP_TOGGLE_SUCCESS = 3300;
    public final static int LAMP_TOGGLE_DOES_NOT_EXIST = 3301;

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
    public void messageReceived(IoSession session, Object message) throws Exception {
        Runnable run = () -> {
            String original = Server.enc.decrypt(message.toString().trim());
            if (!original.equals("ping")) {
                Log.d(original);
            } else {
                session.write(enc("pong"));
                return;
            }
            JSONObject args = new JSONObject(original);
            String action = args.getString("action");
            int actionId = args.getInt("action_id");
            switch (action) {
                case "authenticate":
                    if (!authenticated.containsKey(session.getId())) {
                        if (password.equals(args.getString("password"))) {
                            authenticated.put(session.getId(), true);
                            writeJSON(session, original, actionId, new SessionResponse(AUTH_SUCCESS, false, "Welcome"));
                        } else {
                            authenticated.put(session.getId(), false);
                            writeJSON(session, original, actionId, new SessionResponse(ERROR_FAILED_AUTHENTICATION, true, "Incorrect password"));
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
                        lampAction(session, original, actionId, args);
                        break;
                    case "group":
                        groupAction(session, original, actionId, args);
                        break;
                    default:
                        writeJSON(session, original, actionId, new SessionResponse(ERROR_UNKNOWN, true, "Unknown command: " + action));
                        break;
                }
            } else {
                writeJSON(session, original, actionId, new SessionResponse(ERROR_NOT_AUTHENTICATED, true, "Not authenticated"));
            }
        };
        new Thread(run).start();
    }

    private void lampAction(IoSession session, String original, int actionId, JSONObject args) {
        try {
            switch (args.getString("secondary_action")) {
                case "add":
                    writeJSON(session, original, actionId, LampUtils.addLamp(args));
                    break;
                case "get":
                    writeJSON(session, original, actionId, LampUtils.getLamp(args));
                    break;
                case "update":
                    //TODO: Update lamp
                    break;
                case "delete":
                    //TODO: Delete lamp
                    break;
                case "toggle":
                    writeJSON(session, original, actionId, LampUtils.toggleLamp(args));
                    break;
                default:
                    writeJSON(session, original, actionId, new SessionResponse(ERROR_UNKNOWN, true, "set secondary_action to: add,get,update,delete,toggle"));
                    break;
            }
        } catch (SQLException e) {
            Log.e(e);
            writeJSON(session, original, actionId, new SessionResponse(LAMP_SQL_ERROR, true, e.getMessage()));
        }
    }

    private void groupAction(IoSession session, String original, int actionId, JSONObject args) {
        try {
            switch (args.getString("secondary_action")) {
                case "add":
                    //TODO: Add group
                    break;
                case "get":
                    //TODO: Get group
                    break;
                case "get-all":
                    writeJSON(session, original, actionId, GroupUtils.getGroups());
                    break;
                case "update":
                    //TODO: Update group
                    break;
                case "delete":
                    //TODO: Delete group
                    break;
                default:
                    writeJSON(session, original, actionId, new SessionResponse(ERROR_UNKNOWN, true, "set secondary_action to: add,get,get-all,update,delete"));
                    break;
            }
        } catch (SQLException e) {
            Log.e(e);
            writeJSON(session, original, actionId, new SessionResponse(LAMP_SQL_ERROR, true, e.getMessage()));
        }
    }

    private String enc(String msg) throws EncryptionOperationNotPossibleException {
        return Server.enc.encrypt(msg);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        Log.v("Connection '" + session.getId() + "' closed.");
        c.close();
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
        Log.d("New connection: " + session.getId());
        DateFormat format = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy", Locale.US);
        LocalDateTime now = LocalDateTime.now();
        Date out = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        session.write(enc(format.format(out)));
        session.getReadMessages();
    }

    private void writeJSON(IoSession session, String original, int actionId, SessionResponse response) throws EncryptionOperationNotPossibleException {
        if (session != null) {
            JSONObject json = new JSONObject();
            json.put("action_id", actionId);
            json.put("error", response.isError());
            json.put("code", response.getCode());
            json.put("message", response.getMessage());
            json.put("original", original);
            if (response.getData() != null)
                json.put("data", response.getData());
            if (Log.VERBOSE) {
                Log.v(json.toString());
            }
            session.write(enc(json.toString()));
        }
    }

}