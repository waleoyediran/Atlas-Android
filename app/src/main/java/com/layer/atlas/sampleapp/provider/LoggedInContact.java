/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 5/30/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.atlas.sampleapp.provider;

import org.json.JSONException;
import org.json.JSONObject;

public class LoggedInContact extends Contact {
    private String mAuthToken;
    private String mIdentityToken;

    public LoggedInContact(String firstName, String lastName, String email, String userId,
                           String authToken, String identityToken) {
        super(firstName, lastName, email, userId);
        mAuthToken = authToken;
        mIdentityToken = identityToken;
    }

    public String getAuthToken() {
        return mAuthToken;
    }

    public String getIdentityToken() {
        return mIdentityToken;
    }

    public static LoggedInContact fromJSON(JSONObject json) {
        if (json == null) {
            return null;
        }

        try {
            JSONObject userObject = json.getJSONObject("user");
            String userId = userObject.optString("id", null);
            String firstName = userObject.optString("first_name", null);
            String lastName = userObject.optString("last_name", null);
            String email = userObject.optString("email", null);

            String authToken = json.optString("authentication_token", null);
            String identityToken = json.optString("layer_identity_token", null);

            return new LoggedInContact(firstName, lastName, email, userId, authToken, identityToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject user = new JSONObject();
        user.put("id", getUserId());
        user.put("first_name", getFirstName());
        user.put("last_name", getLastName());
        user.put("email", getEmail());

        JSONObject root = new JSONObject();
        root.put("authentication_token", getAuthToken());
        root.put("layer_identity_token", getIdentityToken());
        root.put("user", user);

        return root;
    }
}
