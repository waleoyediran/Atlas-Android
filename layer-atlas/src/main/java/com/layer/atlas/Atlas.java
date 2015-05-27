package com.layer.atlas;

import android.util.DisplayMetrics;
import android.util.TypedValue;

import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

import java.util.HashMap;

/**
 * @author Oleg Orlov
 * @since 12 May 2015
 */
public class Atlas {

    public static final String METADATA_KEY_CONVERSATION_TITLE = "conversationName";
    
    public static final String MIME_TYPE_ATLAS_LOCATION = "location/coordinate";
    public static final String MIME_TYPE_TEXT = "text/plain";
    public static final String MIME_TYPE_IMAGE_JPEG = "image/jpeg";
    public static final String MIME_TYPE_IMAGE_JPEG_PREVIEW = "image/jpeg+preview";
    public static final String MIME_TYPE_IMAGE_PNG = "image/png";
    public static final String MIME_TYPE_IMAGE_PNG_PREVIEW = "image/png+preview";
    public static final String MIME_TYPE_IMAGE_DIMENSIONS = "application/json+imageSize";

    private Conversation conv;
    private LayerClient layerClient;
    public HashMap<String, Contact> contactsMap = new HashMap<String, Contact>();

    public Atlas(LayerClient layerClient) {
        this.layerClient = layerClient;
    }

    public LayerClient getLayerClient() {
        return layerClient;
    }

    public static float[] getRoundRectRadii(float[] cornerRadiusDp, final DisplayMetrics displayMetrics) {
        float[] result = new float[8];
        for (int i = 0; i < cornerRadiusDp.length; i++) {
            result[i * 2] = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, cornerRadiusDp[i], displayMetrics);
            result[i * 2 + 1] = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, cornerRadiusDp[i], displayMetrics);
        }
        return result;
    }

    public static final class Tools {
        public static String toString(Message msg) {
            StringBuilder sb = new StringBuilder();
            int attaches = 0;
            for (MessagePart mp : msg.getMessageParts()) {
                if ("text/plain".equals(mp.getMimeType())) {
                    sb.append(new String(mp.getData()));
                } else {
                    sb.append("attach").append(attaches++)
                            .append(":").append(mp.getMimeType());
                }
            }
            return sb.toString();
        }
    }
    
}
