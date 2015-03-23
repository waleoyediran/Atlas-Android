package com.layer.atlas.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.layer.atlas.R;

public class SendBox extends LinearLayout {
    public SendBox(Context context) {
        super(context);
        init(context);
    }

    public SendBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SendBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View.inflate(context, R.layout.atlas_layout_sendbox, this);
    }
}
