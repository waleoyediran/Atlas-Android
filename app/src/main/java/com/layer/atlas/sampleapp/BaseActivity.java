package com.layer.atlas.sampleapp;

import android.support.v7.app.ActionBarActivity;

import com.layer.sdk.LayerClient;

public abstract class BaseActivity extends ActionBarActivity {
    private static LayerClient sLayerClient;

    public LayerClient getLayerClient() {
        if (sLayerClient == null) {
            LayerClient.setLogLevel(LayerClient.LogLevel.VERBOSE);
            sLayerClient = LayerClient.newInstance(this, Utils.APP_ID, new LayerClient.Options().googleCloudMessagingSenderId(Utils.GCM_SENDER_ID));
        }
        return sLayerClient;
    }

    @Override
    protected void onResume() {
        super.onResume();

        LayerClient client = getLayerClient();

        if (!client.isAuthenticated()) {
            Utils.authenticate(client, new Utils.Callback() {
                @Override
                public void onSuccess() {
                    authenticatedResume();
                }

                @Override
                public void onError() {

                }
            });
        } else if (!client.isConnected()) {
            client.connect();
            authenticatedResume();
        } else {
            authenticatedResume();
        }
    }

    abstract void authenticatedResume();
}
