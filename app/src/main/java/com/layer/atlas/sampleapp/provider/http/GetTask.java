/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 3/21/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.atlas.sampleapp.provider.http;

import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class GetTask extends AsyncTask<String, Void, Response> {
    private final String mAppId;
    private final String mEmail;
    private final String mToken;
    private final ResponseRunnable mResultRunnable;

    public GetTask(String appId, String email, String token, ResponseRunnable runnable) {
        mAppId = appId;
        mEmail = email;
        mToken = token;
        mResultRunnable = runnable;
    }

    protected Response doInBackground(String... urls) {
        final String initialName = Thread.currentThread().getName();
        Thread.currentThread().setName("Provider GET");
        Response response = new Response();
        try {
            String url = urls[0];
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Accept", "application/json");
            httpGet.setHeader("X_LAYER_APP_ID", mAppId);
            if (mEmail != null) {
                httpGet.setHeader("X_AUTH_EMAIL", mEmail);
            }
            if (mToken != null) {
                httpGet.setHeader("X_AUTH_TOKEN", mToken);
            }
            HttpResponse httpResponse = new DefaultHttpClient().execute(httpGet);
            response.setStatusCode(httpResponse.getStatusLine().getStatusCode());
            response.setResponse(EntityUtils.toString(httpResponse.getEntity()));
            return response;
        } catch (Exception e) {
            response.setException(e);
            return response;
        } finally {
            Thread.currentThread().setName(initialName);
        }
    }

    protected void onPostExecute(Response response) {
        if (mResultRunnable == null) {
            return;
        }
        mResultRunnable.setResponse(response);
        mResultRunnable.run();
    }
}
