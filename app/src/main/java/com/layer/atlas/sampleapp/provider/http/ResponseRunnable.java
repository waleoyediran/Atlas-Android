/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 3/21/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.atlas.sampleapp.provider.http;

public abstract class ResponseRunnable implements Runnable {
    private Response mResponse;

    public Response getResponse() {
        return mResponse;
    }

    public void setResponse(Response response) {
        mResponse = response;
    }
}
