/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 5/30/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.atlas.sampleapp.provider.providers;

import com.layer.atlas.sampleapp.provider.Contact;
import com.layer.atlas.sampleapp.provider.LoggedInContact;
import com.layer.atlas.sampleapp.provider.Provider;
import com.layer.atlas.sampleapp.provider.RegisteredContact;
import com.layer.atlas.sampleapp.provider.http.DeleteTask;
import com.layer.atlas.sampleapp.provider.http.GetTask;
import com.layer.atlas.sampleapp.provider.http.PostTask;
import com.layer.atlas.sampleapp.provider.http.Response;
import com.layer.atlas.sampleapp.provider.http.ResponseRunnable;

import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RailsProvider implements Provider {
    private LoggedInContact mLoggedInContact;
    private Map<String, Contact> mContactMap = new ConcurrentHashMap<String, Contact>();

    private final String mAppId;
    private final String mUrl;

    public RailsProvider(String appId, String url) {
        mAppId = appId;
        mUrl = url;
    }

    @Override
    public void clear(final ClearCallback callback) {
        ResponseRunnable deleteRunnable = new ResponseRunnable() {
            @Override
            public void run() {
                Response response = getResponse();
                if (response.getException() != null) {
                    response.getException().printStackTrace();
                    callback.onClearError("Provider Exception: " + response.getException().getMessage());
                    return;
                } else if (response.getStatusCode() != HttpStatus.SC_NO_CONTENT) {
                    callback.onClearError("Provider stevenUnexpected HTTP Status Code: " + response.getStatusCode());
                    return;
                }

                mLoggedInContact = null;
                mContactMap.clear();
                callback.onClearSuccess();
            }
        };

        httpDelete(getAppId(), getEmail(), getToken(), "users/all", deleteRunnable);
    }

    public String getProviderUrl() {
        return mUrl;
    }

    @Override
    public String getAppId() {
        return mAppId;
    }

    @Override
    public Contact getLoggedInContact() {
        return mLoggedInContact;
    }

    @Override
    public List<Contact> getAddressBook() {
        List<Contact> contacts = new LinkedList<Contact>();
        contacts.addAll(mContactMap.values());
        Collections.sort(contacts);
        return contacts;
    }

    @Override
    public void refreshAddressBook(final String userId, final AddressbookCallback callback) {
        ResponseRunnable postRunnable = new ResponseRunnable() {
            @Override
            public void run() {
                Response response = getResponse();
                if (response.getException() != null) {
                    response.getException().printStackTrace();
                    callback.onAddressBookError("Provider Exception: " + response.getException().getMessage());
                    return;
                } else if (response.getStatusCode() != HttpStatus.SC_OK) {
                    callback.onAddressBookError("Provider Unexpected HTTP Status Code: " + response.getStatusCode());
                    return;
                }

                mContactMap.clear();
                try {
                    JSONArray array = new JSONArray(response.getResponse());
                    for (int i = 0; i < array.length(); i++) {
                        Contact contact = Contact.fromJSON(array.getJSONObject(i));
                        if (mLoggedInContact == null && contact.getUserId() != null && contact.getUserId().equals(userId)) {
                            mLoggedInContact = new LoggedInContact(contact.getFirstName(), contact.getLastName(), contact.getEmail(), contact.getUserId(), null, null);
                        }
                        mContactMap.put(contact.getUserId(), contact);
                    }
                    callback.onAddressBookSuccess(getAddressBook());
                } catch (JSONException e) {
                    e.printStackTrace();
                    callback.onAddressBookError("Provider Exception: " + e.getMessage());
                }
            }
        };
        httpGet(getAppId(), getEmail(), getToken(), "users.json", postRunnable);
    }

    @Override
    public Contact getContact(String id) {
        return mContactMap.get(id);
    }

    @Override
    public void providerLogin(final String email,
                              final String password,
                              final String nonce,
                              final LoginCallback callback) {
        ResponseRunnable postRunnable = new ResponseRunnable() {
            @Override
            public void run() {
                Response response = getResponse();
                if (response.getException() != null) {
                    callback.onLoginError("Provider Exception: " + response.getException().getMessage());
                    response.getException().printStackTrace();
                    return;
                } else if (response.getStatusCode() != HttpStatus.SC_CREATED) {
                    try {
                        JSONObject object = new JSONObject(response.getResponse());
                        callback.onLoginError("Error (" + response.getStatusCode() + "): " + object.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.onLoginError("Provider Exception: " + e.getMessage());
                    }
                    return;
                }

                try {
                    JSONObject object = new JSONObject(response.getResponse());
                    LoggedInContact contact = LoggedInContact.fromJSON(object);
                    mLoggedInContact = contact;
                    callback.onLoginSuccess(contact);
                } catch (JSONException e) {
                    callback.onLoginError("Provider Response error: " + e.getMessage());
                }
            }
        };

        try {
            JSONObject rootObject = new JSONObject();
            JSONObject userObject = new JSONObject();
            rootObject.put("user", userObject);

            userObject.put("email", email);
            userObject.put("password", password);

            rootObject.put("nonce", nonce);

            StringEntity entity = new StringEntity(rootObject.toString(), "UTF-8");
            entity.setContentType("application/json");

            httpPost(getAppId(), getEmail(), getToken(), entity, "users/sign_in.json", postRunnable);
        } catch (JSONException e) {
            callback.onLoginError(e.getMessage());
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            callback.onLoginError(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void providerResume(LoggedInContact loggedInContact) {
        mLoggedInContact = loggedInContact;
    }

    @Override
    public void providerLogout() {
        mLoggedInContact = null;
    }

    @Override
    public void providerRegister(final String first,
                                 final String last,
                                 final String email,
                                 final String password,
                                 final RegisterCallback callback) {
        ResponseRunnable postRunnable = new ResponseRunnable() {
            @Override
            public void run() {
                Response response = getResponse();
                if (response.getException() != null) {
                    response.getException().printStackTrace();
                    callback.onRegisterError("Exception: " + response.getException().getMessage());
                    return;
                } else if (response.getStatusCode() != HttpStatus.SC_CREATED) {
                    try {
                        JSONObject object = new JSONObject(response.getResponse());
                        JSONObject errors = object.optJSONObject("errors");
                        callback.onRegisterError("Error (" + response.getStatusCode() + "): " + errors.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.onRegisterError("Exception: " + e.getMessage());
                    }
                    return;
                }

                try {
                    JSONObject object = new JSONObject(response.getResponse());
                    RegisteredContact contact = RegisteredContact.fromJSON(object);
                    callback.onRegisterSuccess(contact);
                } catch (JSONException e) {
                    e.printStackTrace();
                    callback.onRegisterError("Response error: " + e.getMessage());
                }
            }
        };

        try {
            JSONObject rootObject = new JSONObject();
            JSONObject userObject = new JSONObject();
            rootObject.put("user", userObject);

            userObject.put("first_name", first);
            userObject.put("last_name", last);
            userObject.put("email", email);
            userObject.put("password", password);
            userObject.put("password_confirmation", password);

            StringEntity entity = new StringEntity(rootObject.toString(), "UTF-8");
            entity.setContentType("application/json");

            httpPost(getAppId(), getEmail(), getToken(), entity, "users.json", postRunnable);
        } catch (JSONException e) {
            callback.onRegisterError(e.getMessage());
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            callback.onRegisterError(e.getMessage());
            e.printStackTrace();
        }
    }

    private String getEmail() {
        if (mLoggedInContact == null) {
            return null;
        }
        return mLoggedInContact.getEmail();
    }

    private String getToken() {
        if (mLoggedInContact == null) {
            return null;
        }
        return mLoggedInContact.getAuthToken();
    }

    private void httpDelete(String appId, String email, String token, String urlPath, ResponseRunnable runnable) {
        new DeleteTask(appId, email, token, runnable).execute(getProviderUrl() + "/" + urlPath);
    }

    private void httpGet(String appId, String email, String token, String urlPath, ResponseRunnable runnable) {
        new GetTask(appId, email, token, runnable).execute(getProviderUrl() + "/" + urlPath);
    }

    private void httpPost(String appId, String email, String token, StringEntity entity, String urlPath, ResponseRunnable runnable) {
        new PostTask(appId, email, token, entity, runnable).execute(getProviderUrl() + "/" + urlPath);
    }
}
