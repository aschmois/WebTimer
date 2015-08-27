package com.android305.lights.util;

import org.json.JSONObject;

public class SessionResponse {

    private int code;
    private boolean error;
    private String message;
    private JSONObject data;

    public SessionResponse(int code, boolean error, String message, JSONObject data) {
        this.code = code;
        this.error = error;
        this.message = message;
        this.data = data;
    }

    public SessionResponse(int code, boolean error, String message) {
        this.code = code;
        this.error = error;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public JSONObject getData() {
        return data;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }
}
