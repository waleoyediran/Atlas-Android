/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 5/30/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.atlas.sampleapp.provider;


import org.json.JSONObject;

public class Contact implements Comparable<Contact> {
    private String mFirstName;
    private String mLastName;
    private String mEmail;
    private String mUserId;

    public Contact(String firstName, String lastName, String email, String userId) {
        mFirstName = firstName;
        mLastName = lastName;
        mEmail = email;
        mUserId = userId;
    }

    public String getFirstName() {
        return mFirstName;
    }

    public String getLastName() {
        return mLastName;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getUserId() {
        return mUserId;
    }

    @Override
    public int compareTo(Contact other) {
        if (getLastName().equals(other.getLastName())) {
            return getFirstName().compareTo(other.getFirstName());
        }
        return getLastName().compareTo(other.getLastName());
    }

    public static Contact fromJSON(JSONObject json) {
        String userId = json.optString("id", null);
        String firstName = json.optString("first_name", null);
        String lastName = json.optString("last_name", null);
        String email = json.optString("email", null);
        return new Contact(firstName, lastName, email, userId);
    }
}
