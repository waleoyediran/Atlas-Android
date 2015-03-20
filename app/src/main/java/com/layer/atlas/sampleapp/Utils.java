package com.layer.atlas.sampleapp;

import android.content.Context;

import com.layer.atlas.sampleapp.provider.LoggedInContact;
import com.layer.atlas.sampleapp.provider.Provider;
import com.layer.atlas.sampleapp.provider.providers.RailsProvider;
import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerAuthenticationListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class Utils {
    private final static String APP_ID = "9ec30af8-5591-11e4-af9e-f7a201004a3b";
    private final static String GCM_SENDER_ID = "565052870572";
    private final static String PROVIDER_URL = "http://layer-identity-provider.herokuapp.com";
    private final static String EMAIL = "dvorak10@layer.com";
    private final static String PASSWORD = "password";

    private static RailsProvider sProvider;
    private static LayerClient sClient;

    public static interface Callback {
        public void onSuccess();

        public void onError();
    }

    public static LayerClient getLayerClient(Context context) {
        if (sClient == null) {
            LayerClient.setLogLevel(LayerClient.LogLevel.VERBOSE);
            sClient = LayerClient.newInstance(context, APP_ID,
                    new LayerClient.Options()
                            .googleCloudMessagingSenderId(GCM_SENDER_ID));
        }
        return sClient;
    }

    public static RailsProvider getProvider() {
        if (sProvider == null) {
            sProvider = new RailsProvider(APP_ID, PROVIDER_URL);
        }
        return sProvider;
    }

    public static void authenticate(final LayerClient client, final Callback callback) {
        LayerAuthenticationListener listener = new LayerAuthenticationListener() {
            @Override
            public void onAuthenticated(LayerClient client, String userId) {
                callback.onSuccess();
                //client.unregisterAuthenticationListener(this);
            }

            @Override
            public void onDeauthenticated(LayerClient client) {
                callback.onError();
            }

            @Override
            public void onAuthenticationChallenge(final LayerClient client, final String nonce) {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        LoggedInContact login = onChallenge(nonce);
                        client.answerAuthenticationChallenge(login.getIdentityToken());
                    }
                };
                new Thread(r).start();
            }

            @Override
            public void onAuthenticationError(LayerClient client, LayerException e) {
                callback.onError();
            }
        };
        client.registerAuthenticationListener(listener);
        client.authenticate();
    }

    public static LoggedInContact onChallenge(String nonce) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<LoggedInContact> login = new AtomicReference<>(null);
        getProvider().providerLogin(EMAIL, PASSWORD, nonce, new Provider.LoginCallback() {
            @Override
            public void onLoginSuccess(LoggedInContact loggedInContact) {
                login.set(loggedInContact);
                latch.countDown();
            }

            @Override
            public void onLoginError(String error) {
                throw new IllegalArgumentException(error);
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return login.get();
    }

}
