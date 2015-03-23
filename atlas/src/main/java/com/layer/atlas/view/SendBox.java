package com.layer.atlas.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.layer.atlas.R;

public class SendBox extends LinearLayout {
    ImageButton mPhotoButton;
    ImageButton mSendButton;

    public SendBox(Context context) {
        super(context);
        init(context, null, 0);
    }

    public SendBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public SendBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        View.inflate(context, R.layout.atlas_layout_sendbox, this);
        mPhotoButton = (ImageButton)findViewById(R.id.atlas_photo_button);
        mSendButton = (ImageButton)findViewById(R.id.atlas_send_button);

        if (attrs == null) {
            return;
        }

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SendBox, defStyleAttr, 0);

        try {
            Drawable photoDrawable = a.getDrawable(R.styleable.SendBox_photoDrawable);
            mPhotoButton.setImageDrawable(photoDrawable);

            Drawable sendDrawable = a.getDrawable(R.styleable.SendBox_sendDrawable);
            mSendButton.setImageDrawable(sendDrawable);
        } finally {
            a.recycle();
        }

    }
}
