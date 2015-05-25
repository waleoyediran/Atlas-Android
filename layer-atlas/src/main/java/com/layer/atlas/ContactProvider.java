package com.layer.atlas;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ContactProvider {
    private final static String TAG = ContactProvider.class.getSimpleName();
    private final Map<String, Contact> mContactMap = new HashMap<String, Contact>();
    private final Callback mCallback;
    private final List<Listener> mListeners = new ArrayList<Listener>();
    private final SharedPreferences mSharedPreferences;
    private final AtomicInteger mRefreshRequestCounter = new AtomicInteger(0);

    public ContactProvider(Context context, Callback callback) {
        mCallback = callback;
        mSharedPreferences = context.getSharedPreferences("contacts", Context.MODE_PRIVATE);
        load();
    }

    /**
     * ContactProvider.Callback provides the mechanism for refreshing Contacts from an external
     * provider, such as a backend API.
     */
    public interface Callback {
        /**
         * Returns a list of all Contacts.  Called on a background thread.
         *
         * @return The complete list of Contacts.
         */
        List<Contact> getAllContacts();
    }

    /**
     * Refreshes all Contacts from the ContactProvider.Callback.
     */
    public synchronized void refresh() {
        // TODO: consider a time-based limiter to prevent rapid requests, or intelligent refresh
        int requests = mRefreshRequestCounter.get();
        if (requests > 1) return;   // Two covers race conditions.
        mRefreshRequestCounter.incrementAndGet();

        (new AsyncTask<Void, Void, List<Contact>>() {
            @Override
            protected List<Contact> doInBackground(Void... params) {
                List<Contact> contacts = mCallback.getAllContacts();
                if (contacts != null) set(contacts);
                return contacts;
            }

            @Override
            protected void onPostExecute(List<Contact> contact) {
                onContacts();
                mRefreshRequestCounter.decrementAndGet();
            }
        }).execute();
    }

    /**
     * Overwrites the current list of Contacts with the provided list.
     *
     * @param contacts New list of Contacts to apply.
     */
    public void set(List<Contact> contacts) {
        synchronized (mContactMap) {
            mContactMap.clear();
            for (Contact contact : contacts) {
                mContactMap.put(contact.userId, contact);
            }
        }
        save();
    }

    /**
     * Adds the given Contact.
     *
     * @param contact Contact to add.
     */
    public void add(Contact contact) {
        synchronized (mContactMap) {
            mContactMap.put(contact.userId, contact);
        }
        save();
    }

    /**
     * Adds all provided Contacts.
     *
     * @param contacts Contacts to add.
     */
    public void addAll(List<Contact> contacts) {
        synchronized (mContactMap) {
            for (Contact contact : contacts) {
                mContactMap.put(contact.userId, contact);
            }
        }
        save();
    }

    /**
     * Returns all cached Contacts.
     *
     * @return All cachec Contacts.
     */
    public List<Contact> getAll() {
        synchronized (mContactMap) {
            return new ArrayList<Contact>(mContactMap.values());
        }
    }

    /**
     * Returns the cached Contact with the given ID, or `null`.  If the contact is not cached,
     * refresh() is called.
     *
     * @param id The ID of the Contact to get or fetch.
     * @return The Contact with the given ID if it is cached, or null.
     */
    public Contact get(String id) {
        synchronized (mContactMap) {
            Contact contact = mContactMap.get(id);
            if (contact != null) {
                return contact;
            }
        }
        refresh();
        return null;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Persistence
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean load() {
        String jsonString = mSharedPreferences.getString("json", null);
        if (jsonString == null) return false;

        List<Contact> contacts;
        try {
            JSONArray contactsJson = new JSONArray(jsonString);
            contacts = new ArrayList<Contact>(contactsJson.length());
            for (int i = 0; i < contactsJson.length(); i++) {
                JSONObject contactJson = contactsJson.getJSONObject(i);
                Contact contact = new Contact();
                contact.userId = contactJson.optString("id");
                contact.firstName = contactJson.optString("first_name");
                contact.lastName = contactJson.optString("last_name");
                contact.email = contactJson.optString("email");
                contacts.add(contact);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error while saving", e);
            return false;
        }

        synchronized (mContactMap) {
            mContactMap.clear();
            for (Contact contact : contacts) {
                mContactMap.put(contact.userId, contact);
            }
        }

        return true;
    }

    private boolean save() {
        Collection<Contact> contacts;
        synchronized (mContactMap) {
            contacts = mContactMap.values();
        }

        JSONArray contactsJson;
        try {
            contactsJson = new JSONArray();
            for (Contact contact : contacts) {
                JSONObject contactJson = new JSONObject();
                contactJson.put("id", contact.userId);
                contactJson.put("first_name", contact.firstName);
                contactJson.put("last_name", contact.lastName);
                contactJson.put("email", contact.email);
                contactsJson.put(contactJson);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error while saving", e);
            return false;
        }

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString("json", contactsJson.toString());
        editor.apply();

        return true;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Listener
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * ContactProvider.Listener allows external classes to listen for Contact updates.
     */
    public interface Listener {
        /**
         * Alerts the Listener Contact updates.
         *
         * @param contacts The complete list of cached Contacts.
         */
        void onContactsRefreshed(List<Contact> contacts);
    }

    public void registerListener(Listener listener) {
        synchronized (mListeners) {
            if (mListeners.contains(listener)) return;
            mListeners.add(listener);
        }
    }

    public void unregisterListener(Listener listener) {
        synchronized (mListeners) {
            mListeners.remove(listener);
        }
    }

    private void onContacts() {
        final List<Contact> contacts = new ArrayList<Contact>(mContactMap.values());
        synchronized (mListeners) {
            for (Listener listener : mListeners) {
                listener.onContactsRefreshed(contacts);
            }
        }
    }
}
