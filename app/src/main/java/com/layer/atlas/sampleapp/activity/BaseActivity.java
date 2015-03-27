package com.layer.atlas.sampleapp.activity;

import android.support.v7.app.ActionBarActivity;

import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerAuthenticationListener;

public abstract class BaseActivity extends ActionBarActivity implements LayerAuthenticationListener {
    private static LayerClient sLayerClient;

    private static String sAppId;
    private static String sGcmSenderId;

    private volatile boolean mAlertAuthenticatedResume;

    protected BaseActivity(String appId, String gcmSenderId) {
        sAppId = appId;
        sGcmSenderId = gcmSenderId;
    }

    public LayerClient getLayerClient() {
        if (sLayerClient == null) {
            sLayerClient = LayerClient.newInstance(this, sAppId, new LayerClient.Options().googleCloudMessagingSenderId(sGcmSenderId));
        }
        return sLayerClient;
    }

    @Override
    protected void onResume() {
        super.onResume();

        LayerClient client = getLayerClient();
        mAlertAuthenticatedResume = true;
        client.registerAuthenticationListener(this);

        if (!client.isAuthenticated()) {
            // If we're not authenticated, authenticate.  This also connects if needed.
            client.authenticate();
        } else if (!client.isConnected()) {
            // If we're authenticated, but not connected, just connect.
            client.connect();
            onAuthenticatedResume();
        } else {
            // We're both authenticated and connected.
            onAuthenticatedResume();
        }
    }

    @Override
    protected void onPause() {
        getLayerClient().unregisterAuthenticationListener(this);
        super.onPause();
    }

    /**
     * Let this Activity know it is now authenticated and connected, after onResume().
     */
    public abstract void onAuthenticatedResume();

    @Override
    public void onAuthenticated(LayerClient client, String s) {
        if (mAlertAuthenticatedResume) {
            mAlertAuthenticatedResume = false;
            onAuthenticatedResume();
        }
    }

    @Override
    public void onDeauthenticated(LayerClient client) {

    }

    @Override
    public void onAuthenticationChallenge(LayerClient client, String s) {

    }

    @Override
    public void onAuthenticationError(LayerClient client, LayerException e) {

    }
}
