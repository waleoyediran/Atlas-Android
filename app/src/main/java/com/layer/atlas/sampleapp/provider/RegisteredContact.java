/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 5/30/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.atlas.sampleapp.provider;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisteredContact extends Contact {
    private String mAuthToken;
    private String mPassword;

    public RegisteredContact(String firstName, String lastName, String email, String userId, String password, String authToken) {
        super(firstName, lastName, email, userId);
        mPassword = password;
        mAuthToken = authToken;
    }

    public String getAuthToken() {
        return mAuthToken;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    public String getPassword() {
        return mPassword;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", getUserId());
        json.put("first_name", getFirstName());
        json.put("last_name", getLastName());
        json.put("email", getEmail());
        json.put("authentication_token", getAuthToken());
        json.put("created_at", null);
        json.put("updated_at", null);
        json.put("password", getPassword());
        return json;
    }

    public static RegisteredContact fromJSON(JSONObject json) {
        String userId = json.optString("id", null);
        String firstName = json.optString("first_name", null);
        String lastName = json.optString("last_name", null);
        String email = json.optString("email", null);
        String authToken = json.optString("authentication_token", null);
        String createdAt = json.optString("created_at", null);
        String updatedAt = json.optString("updated_at", null);
        String password = json.optString("password", null);
        return new RegisteredContact(firstName, lastName, email, userId, password, authToken);
    }
}