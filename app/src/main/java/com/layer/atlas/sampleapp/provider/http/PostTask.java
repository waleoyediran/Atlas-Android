/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 3/21/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.atlas.sampleapp.provider.http;

import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class PostTask extends AsyncTask<String, Void, Response> {
    private final String mAppId;
    private final String mEmail;
    private final String mToken;
    private final StringEntity mEntity;
    private final ResponseRunnable mResultRunnable;

    public PostTask(String appId, String email, String token, StringEntity entity, ResponseRunnable runnable) {
        mAppId = appId;
        mEmail = email;
        mToken = token;
        mEntity = entity;
        mResultRunnable = runnable;
    }

    protected Response doInBackground(String... urls) {
        final String initialName = Thread.currentThread().getName();
        Thread.currentThread().setName("Provider POST");
        Response response = new Response();
        try {
            String url = urls[0];
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("X_LAYER_APP_ID", mAppId);
            if (mEmail != null) {
                httpPost.setHeader("X_AUTH_EMAIL", mEmail);
            }
            if (mToken != null) {
                httpPost.setHeader("X_AUTH_TOKEN", mToken);
            }
            httpPost.setEntity(mEntity);
            HttpResponse httpResponse = new DefaultHttpClient().execute(httpPost);
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
