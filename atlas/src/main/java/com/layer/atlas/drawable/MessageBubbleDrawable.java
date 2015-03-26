package com.layer.atlas.drawable;

import android.graphics.drawable.GradientDrawable;

public class MessageBubbleDrawable extends GradientDrawable {
    public MessageBubbleDrawable() {
        init();
    }

    public MessageBubbleDrawable(Orientation orientation, int[] colors) {
        super(orientation, colors);
        init();
    }

    private void init() {
        setShape(RECTANGLE);
    }

    public static MessageBubbleDrawable with(float radius, int color) {
        MessageBubbleDrawable messageBubble = new MessageBubbleDrawable();
        messageBubble.setCornerRadii(new float[]{radius, radius, radius, radius, radius, radius, radius, radius});
        messageBubble.setColor(color);
        return messageBubble;
    }
}
