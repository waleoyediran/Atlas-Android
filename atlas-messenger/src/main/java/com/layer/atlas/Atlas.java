package com.layer.atlas;

import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;

/**
 * @author olegorlov
 * @since 12 May 2015
 */
public class Atlas {

    public static final String MIME_TYPE_ATLAS_LOCATION = "location/coordinate";
    public static final String MIME_TYPE_TEXT = "text/plain";
    public static final String MIME_TYPE_IMAGE_JPEG = "image/jpeg";
    public static final String MIME_TYPE_IMAGE_PNG = "image/png";

    private Conversation conv;
    private LayerClient layerClient;
    
    public Atlas(LayerClient layerClient) {
        this.layerClient = layerClient; 
    }

    public LayerClient getLayerClient() {
        return layerClient;
    }
    
    
}
