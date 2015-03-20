package com.layer.atlas.sampleapp.provider.providers.eit;

import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.util.UUID;

public class EitTask extends AsyncTask<Void, Void, String> {
    private final UUID mAppId;
    private final String mUserId;
    private final String mNonce;
    private final Callback mCallback;

    private Throwable mThrowable;

    public EitTask(UUID appId, String userId, String nonce, Callback callback) {
        mAppId = appId;
        mUserId = userId;
        mNonce = nonce;
        mCallback = callback;
    }

    @Override
    protected String doInBackground(Void... params) {
        final String initialName = Thread.currentThread().getName();
        Thread.currentThread().setName("Provider EIT");
        try {
            HttpPost post = new HttpPost("https://layer-identity-provider.herokuapp.com/identity_tokens");
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Accept", "application/json");

            JSONObject json = new JSONObject()
                    .put("app_id", mAppId.toString())
                    .put("user_id", mUserId)
                    .put("nonce", mNonce);
            post.setEntity(new StringEntity(json.toString()));

            HttpResponse response = (new DefaultHttpClient()).execute(post);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                mThrowable = new IllegalStateException("Bad status code: " + response.getStatusLine().getStatusCode());
                return null;
            }

            return (new JSONObject(EntityUtils.toString(response.getEntity()))).optString("identity_token");
        } catch (Exception e) {
            mThrowable = e;
        } finally {
            Thread.currentThread().setName(initialName);
        }
        return null;
    }

    @Override
    protected void onPostExecute(String eit) {
        if (mThrowable != null) {
            mCallback.onEitError(mThrowable.getMessage());
        } else {
            mCallback.onEitResponse(eit);
        }
    }

    public static interface Callback {
        public void onEitResponse(String eit);

        public void onEitError(String error);
    }
}
