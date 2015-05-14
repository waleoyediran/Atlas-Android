package com.layer.atlas.messenger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.layer.atlas.Atlas.Contact;
import com.layer.sdk.LayerClient;
import com.layer.sdk.LayerClient.Options;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.listeners.LayerConnectionListener;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

/**
 * @author Oleg Orlov
 * @since March 3, 2015
 */
public class App101 extends Application {
    /** */
    private static final String TAG = App101.class.getSimpleName();
    private static final boolean debug = true;
    
    private static final String UNIQING_APP_ID = "2043446e-ccfa-11e4-90af-1d6c000000f4";
    private static final String PROD_APP_ID = "9ec30af8-5591-11e4-af9e-f7a201004a3b";
    //private static final String APP_ID = DefaultConfig.kevin_standalone ? UNIQING_APP_ID : PROD_APP_ID;
    //private static final String APP_ID = "24f43c32-4d95-11e4-b3a2-0fd00000020d"; // staging
    private static final String APP_ID = PROD_APP_ID;

    private static final String GCM_SENDER_ID = "565052870572";
//    private static final String GCM_SENDER_ID = "565052870572"; // staging
    
//    private static final String USER_EMAIL = "hhdad@mailforspam.com";
    //private static final String USER_EMAIL = "ulady@mailforspam.com";
    //private static final String USER_EMAIL = "hhsample@mailforspam.com";
//    private static final String USER_EMAIL = "hhdadst@mailforspam.com";
    
    private static final String PROVIDER_ADDRESS = "http://layer-identity-provider.herokuapp.com";
    private static final String URL_SIGN_IN_RAILS = "https://layer-identity-provider.herokuapp.com/users/sign_in.json";
        
    public interface keys {
        public static final String CONVERSATION_URI = "conversation.uri";
        public static final String USER_ID = "user.id";
        public static final String AUTH_TOKEN = "auth.token";
        public static final String CONTACTS = "contact.list";
    }
    
    private LayerClient layerClient;

    public String login;
    public String authToken;
    public HashMap<String, Contact> contactsMap = new HashMap<String, Contact>();
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        loadPreferences();
    }

    public boolean isAuthenticated() {
        return authToken != null;
    }
    
    public LayerClient getLayerClient() {
        if (layerClient == null) {
            layerClient = initLayerClient();
        }
        return layerClient;
    }

    private LayerClient initLayerClient() {
        final LayerClient resultClient = (LayerClient.newInstance(this, APP_ID, new Options()
//                .broadcastPushInForeground(true)
//                .googleCloudMessagingSenderId(GCM_SENDER_ID)
        ));
        
        if (debug) Log.w(TAG, "onCreate() client created");
        
        resultClient.registerConnectionListener(new LayerConnectionListener() {
            public void onConnectionConnected(LayerClient client) {
                Log.w(TAG, "onConnectionConnected() ");
            }
            
            public void onConnectionDisconnected(LayerClient client) {
                Log.e(TAG, "onConnectionDisconnected() ");
            }
            
            public void onConnectionError(LayerClient client, LayerException exception) {
                Log.e(TAG, "onConnectionError() ", exception);
            }
        });
                
        resultClient.registerEventListener(new LayerChangeEventListener.MainThread() {
            public void onEventMainThread(LayerChangeEvent event) {
                if (debug) Log.w(TAG, "onEventMainThread() event: " + event);
            }
        });
        
        if (!resultClient.isAuthenticated()) resultClient.authenticate();
        else if (!resultClient.isConnected()) resultClient.connect();
        if (debug) Log.w(TAG, "onCreate() Layer launched");
        
        
        if (debug) Log.d(TAG, "onCreate() Refreshing Contacts");
        new Thread(new Runnable() {
            public void run() {
                synchronized (App101.this) {
                    while (authToken == null) {
                        try {
                            App101.this.wait(1000);
                        } catch (InterruptedException e) {}
                    }
                }
                
                try {
                    String responseString = requestJson(
                            "https://layer-identity-provider.herokuapp.com/users.json", new String[][] {
                            {"X_LAYER_APP_ID", APP_ID}, 
                            {"X_AUTH_TOKEN", authToken}, 
                            {"X_AUTH_EMAIL", login}});
                    if (debug) Log.w(TAG, "contacts() result: " + responseString);
                    savePref(keys.CONTACTS, responseString);
                    
                    loadContacts(responseString, contactsMap);
                    dumpDb();
                } catch (Exception e) {
                    Log.e(TAG, "contacts() ", e);
                }
            }


        }).start();
        return resultClient;
    }
    
    public void dumpDb() {
//        LayerClientImpl lci = (LayerClientImpl) getLayerClient();
//        SQLiteDatabase db = lci.getSyncPersistence().getWritableDatabase();
//        Dbt.dumpNonEmptyNames(db);
//        if (true) return;
//        Dbt.dump(db, "syncable_changes");
//        Dbt.dump(db, "synced_changes");
//        Dbt.dump(db, "conversations");
//        Dbt.dump(db, "events");
//        Dbt.dump(db, "streams");
    }
    
    private App101 savePref(String key, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString(key, value).commit();
        return this;
    }
    public void savePrefs() {
        savePref(keys.AUTH_TOKEN, authToken);
        savePref(keys.USER_ID, login);
    }
    
    private void loadPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        authToken = prefs.getString(keys.AUTH_TOKEN, null);
        login = prefs.getString(keys.USER_ID, null);
        String contactsJson = prefs.getString(keys.CONTACTS, null); 
        if (contactsJson != null) {
            loadContacts(contactsJson, contactsMap);
        }
        
    }
    
    public static String toString(Message msg) {
        StringBuilder sb = new StringBuilder();
        int attaches = 0;
        for (MessagePart mp : msg.getMessageParts()) {
            if ("text/plain".equals(mp.getMimeType())) {
                sb.append(new String(mp.getData()));
            } else {
                sb.append("attach").append(attaches++)
                .append(":").append(mp.getMimeType());
            }
        }
        return sb.toString();
    }

    /**
     * 
     * @return [ eit, authToken, error ]
     */
    public static String[] requestToken(String userEmail, String userPassw, final String nonce) throws JSONException {
        JSONObject rootObject = new JSONObject();
        rootObject.put("nonce", nonce);
        rootObject.put("user", new JSONObject()
        .put("email", userEmail)
        .put("password", userPassw));
        
        String responseEntity = requestJson(URL_SIGN_IN_RAILS, 
                new String[][] {{"X_LAYER_APP_ID", APP_ID}}, rootObject.toString());
        
        if (debug) Log.d(TAG, "onAuthenticationChallenge() responseEntity:\n" + responseEntity);
        
        final JSONObject jsonResp = new JSONObject(responseEntity);
        
        String eit = jsonResp.optString("layer_identity_token");
        if (debug) Log.w(TAG, "onAuthenticationChallenge() token: \n" + eit + "\n");
        
        String authToken = jsonResp.optString("authentication_token");
        
        String error = jsonResp.optString("error", null);
        
        return new String[] {eit, authToken, error};
    }

    // -------    Tools   -----
    //
    //
    public static String getContactInitials(Contact contact) {
        if (contact == null) return null;
        StringBuilder sb = new StringBuilder();
        sb.append(contact.firstName != null && contact.firstName.trim().length() > 0 ? contact.firstName.trim().charAt(0) : "");
        sb.append(contact.lastName != null  && contact.lastName.trim().length() > 0 ?  contact.lastName.trim().charAt(0) : "");
        return sb.toString();
    }
    
    public static String getContactFirstAndL(Contact contact) {
        if (contact == null) return null;
        StringBuilder sb = new StringBuilder();
        if (contact.firstName != null && contact.firstName.trim().length() > 0) {
            sb.append(contact.firstName.trim()).append(" ");
        }
        if (contact.lastName != null && contact.lastName.trim().length() > 0) {
            sb.append(contact.lastName.trim().charAt(0));
            sb.append(".");
        }
        return sb.toString();
    }
    
    public static String getContactFirstAndLast(Contact contact) {
        if (contact == null) return null;
        StringBuilder sb = new StringBuilder();
        if (contact.firstName != null && contact.firstName.trim().length() > 0) {
            sb.append(contact.firstName.trim()).append(" ");
        }
        if (contact.lastName != null && contact.lastName.trim().length() > 0) {
            sb.append(contact.lastName.trim());
        }
        return sb.toString();
    }

    public static Map<String, Contact> loadContacts(String jsonContactList, Map<String, Contact> where) {
        HashMap<String, Contact> result = new HashMap<String, Contact>();
        try {
            JSONArray jsonArr = new JSONArray(jsonContactList);
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jObject = jsonArr.getJSONObject(i);
                Contact contact = Contact.fromRecord(jObject);
                result.put(contact.userId, contact);
            }
        } catch (JSONException e) {
            Log.e(TAG, "loadContacts() json parsing error. json: " + jsonContactList, e);
            return null;
        }
        if (debug) Log.d(TAG, "loadContacts() " + result.size() + " contacts loaded");
        
        if (where != null) {
            where.putAll(result);
            return where;
        }
        return result;
    }

    public static Message message(String text, LayerClient layerClient) {
        MessagePart messagePart = layerClient.newMessagePart(text);
        Message result = layerClient.newMessage(messagePart);
        return result;
    }
    
    public static String requestJson(String url) {
        return requestJson(url, null, null, false);
    }
    public static String requestJson(String url, String[][] headers) {
        return requestJson(url, headers, null, false);
    }
    public static String requestJson(String url, String[][] headers, boolean post) {
        return requestJson(url, headers, null, post);
    }
    public static String requestJson(String url, String requestBody) {
        return requestJson(url, null, requestBody, true);
    }
    public static String requestJson(String url, String[][] headers, String requestBody) {
        return requestJson(url, headers, requestBody, true);
    }
    public static String requestJson(String url, String[][] headers, String requestBody, boolean post) {
        HttpRequest req = post ? new HttpPost(url) : new HttpGet(url);
        req.setHeader("Content-Type", "application/json");
        req.setHeader("Accept", "application/json");
        if (headers != null) {
            for (String[] header : headers) {
                req.setHeader(header[0], header[1]);
            }
        }
        try {
            if (post && requestBody != null) {
                StringEntity entity = new StringEntity(requestBody, "UTF-8");
                entity.setContentType("application/json");
                ((HttpPost)req).setEntity(entity);
            }
            HttpResponse response = (new DefaultHttpClient()).execute(post ? ((HttpPost)req): ((HttpGet)req));
            String responseString = EntityUtils.toString(response.getEntity());
            return responseString;
        } catch (Exception e) {
            Log.e(TAG, "requestJson() url: " + url, e);
            return null;
        }
    }
    
    public static StringBuilder toString(SQLiteDatabase db, String table) {
        StringBuilder sb = new StringBuilder();
        Cursor cur = db.query(table, null, null, null, null, null, null);
        int rowNum = 0;
        while (cur.moveToNext()) {
            sb.append(rowNum == 0 ? "" : "\n").append(rowNum).append(": ");
            for (int i = 0; i < cur.getColumnCount(); i++) {
                sb.append(i == 0 ? "; ":"").append(cur.getString(i));
            }
        }
        return sb;
    }

    /**
     * Converts a Bundle to the human readable string.
     *
     * @param items the collection for example, {@link java.util.ArrayList}, {@link java.util.HashSet} etc.
     * @return the converted string
     */
    public static String toString(Bundle bundle) {
        return toString(bundle, ", ", "");
    }
    
    public static String toString(Bundle bundle, String separator, String firstSeparator) {
        if (bundle == null) return "null";
        StringBuilder sb = new StringBuilder("[");
        int i = 0;
        for (Iterator<String> itKey = bundle.keySet().iterator(); itKey.hasNext(); i++) {
            String key = itKey.next();
            sb.append(i == 0 ? firstSeparator : separator).append(i).append(": ");
            sb.append(key).append(" : ").append(bundle.get(key));
        }
        sb.append("]");
        return sb.toString();
    }

}
