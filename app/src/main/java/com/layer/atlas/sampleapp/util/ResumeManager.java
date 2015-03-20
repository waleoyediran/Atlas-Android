package com.layer.atlas.sampleapp.util;

import com.layer.atlas.sampleapp.provider.LoggedInContact;
import com.layer.atlas.sampleapp.provider.Provider;
import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerAuthenticationListener;
import com.layer.sdk.listeners.LayerConnectionListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;


public class ResumeManager implements LayerAuthenticationListener, LayerConnectionListener {

    public static interface Callback {
        public void onProgress(String message, int max, int current);

        public void onSuccess(String userId);

        public void onFailure(String message);
    }

    public static class ProviderCredentials {
        private final String mEmail;
        private final String mPassword;

        public ProviderCredentials(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        public String getEmail() {
            return mEmail;
        }

        public String getPassword() {
            return mPassword;
        }

        public boolean isValid() {
            return mEmail != null && mPassword != null;
        }
    }

    private final LayerClient mClient;
    private final Provider mProvider;
    private final ProviderCredentials mProviderCredentials;
    private Callback mCallback;
    private int mMaxStep = 0;
    private final AtomicInteger mStep = new AtomicInteger(0);

    public ResumeManager(LayerClient client,
                         Provider provider,
                         ProviderCredentials providerCredentials) {
        mClient = client;
        mProvider = provider;
        mProviderCredentials = providerCredentials;

//        try {
//            JSONObject json = App.AppPreferences.getLastUser();
//            LoggedInContact loggedInContact = LoggedInContact.fromJSON(json);
//            mProvider.providerResume(loggedInContact);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
        mProvider.providerResume(null);

    }

    private ResumeManager register() {
        mClient.registerAuthenticationListener(this).registerConnectionListener(this);
        return this;
    }

    private ResumeManager unregister() {
        mClient.unregisterAuthenticationListener(this).unregisterConnectionListener(this);
        return this;
    }

    /**
     * Returns an estimate of the progress reports to anticipate.
     *
     * @param isConnect
     * @param callback
     * @return
     */
    public void resume(boolean isConnect, Callback callback) {
        if (isConnect) {
            mCallback = callback;
            if (!mClient.isConnected()) {
                register();
                mMaxStep = 3;
                mCallback.onProgress("Layer connecting...", mMaxStep, mStep.getAndIncrement());
                mClient.connect();
            } else if (!mClient.isAuthenticated()) {
                register();
                mMaxStep = 2;
                mCallback.onProgress("Layer authenticating...", mMaxStep, mStep.getAndIncrement());
                mClient.authenticate();
            } else {
                unregister();
                mMaxStep = 1;
                mCallback = null;
                callback.onSuccess(mClient.getAuthenticatedUserId());
            }
        } else {
            mMaxStep = 0;
            mCallback = null;
            if (mClient.isAuthenticated()) {
                unregister();
                callback.onSuccess(mClient.getAuthenticatedUserId());
            } else {
                unregister();
                callback.onFailure("No session to resume");
            }
        }
    }

    private void providerLogin(final LayerClient client, final String nonce) {
        mProvider.providerLogin(mProviderCredentials.getEmail(), mProviderCredentials.getPassword(), nonce, new Provider.LoginCallback() {
            @Override
            public void onLoginSuccess(LoggedInContact loggedInContact) {
                if (mCallback != null) {
                    mCallback.onProgress("Provider logged in, answering challenge...", mMaxStep, mStep.getAndIncrement());
                }
                client.answerAuthenticationChallenge(loggedInContact.getIdentityToken());
                System.out.println(loggedInContact.getIdentityToken().toString());

//                try {
//                    App.AppPreferences.setLastUser(loggedInContact.toJSON());
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
            }

            @Override
            public void onLoginError(String error) {
                unregister();
                if (mCallback != null) {
                    mCallback.onFailure(error);
                    mCallback = null;
                }
            }
        });
    }

    @Override
    public void onConnectionConnected(LayerClient client) {
        if (mClient.isAuthenticated()) {
            unregister();
            if (mCallback != null) {
                mCallback.onSuccess(mClient.getAuthenticatedUserId());
            }
        } else {
            if (mCallback != null) {
                mCallback.onProgress("Layer connected, authenticating...", mMaxStep, mStep.getAndIncrement());
            }
            mClient.authenticate();
        }
    }

    @Override
    public void onConnectionDisconnected(LayerClient client) {

    }

    @Override
    public void onConnectionError(LayerClient client, LayerException exception) {

    }

    @Override
    public void onAuthenticated(LayerClient client, String userId) {
        unregister();
        if (mCallback != null) {
            mCallback.onSuccess(userId);
            mCallback = null;
        }

        HashSet<String> autoDLMimeTypes = new HashSet<String>();
        autoDLMimeTypes.add("text/plain");
        client.setAutoDownloadMimeTypes(autoDLMimeTypes);
    }

    @Override
    public void onDeauthenticated(LayerClient client) {
        unregister();
        if (mCallback != null) {
            mCallback.onFailure("Deauthenticated");
            mCallback = null;
        }
    }

    @Override
    public void onAuthenticationChallenge(final LayerClient client, String nonce) {
        if (mCallback != null) {
            mCallback.onProgress("Layer challenged, logging in with provider...", mMaxStep, mStep.getAndIncrement());
        }
        providerLogin(client, nonce);
    }

    @Override
    public void onAuthenticationError(LayerClient client, LayerException exception) {
        unregister();
        if (mCallback != null) {
            mCallback.onFailure(exception.getMessage());
            mCallback = null;
        }
    }
}
