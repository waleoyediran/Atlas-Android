package com.layer.atlas.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.layer.atlas.R;

/**
 * MessageInputToolbar extends LinearLayout and provides a message input EditText, a photo button,
 * and a send button.  External listeners can attach a Callback to receive interaction events.  Use
 * getText() and setText() to get and set the message input EditText value.
 */
public class MessageInputToolbar extends LinearLayout implements View.OnClickListener, TextWatcher {
    private final static int DEF_STYLE = R.attr.defaultMessageInputToolbarStyle;

    private EditText mMessageEditText;
    private ImageButton mLeftButton;
    private ImageButton mRightButton;
    private Callback mCallback;

    /**
     * MessageInputToolbar.Callback allows an observer to receive callbacks for user interactions on
     * this MessageInputToolbar.
     */
    public static interface Callback {
        /**
         * Notifies the callback of the user clicking on the left button.
         *
         * @param toolbar This MessageInputToolbar.
         * @param button  The button clicked.
         */
        public void onLeftButtonClick(MessageInputToolbar toolbar, View button);

        /**
         * Notifies the callback of the user clicking on the right button.
         *
         * @param toolbar This MessageInputToolbar.
         * @param button  The button clicked.
         */
        public void onRightButtonClick(MessageInputToolbar toolbar, View button);

        /**
         * Notifies the callback of the user typing in the message input.
         *
         * @param toolbar This MessageInputToolbar.
         * @param input   The changed message input.
         */
        public void onAfterTextChanged(MessageInputToolbar toolbar, Editable input);
    }


    //==============================================================================================
    // Constructor
    //==============================================================================================

    public MessageInputToolbar(Context context) {
        this(context, null, DEF_STYLE);
    }

    public MessageInputToolbar(Context context, AttributeSet attrs) {
        this(context, attrs, DEF_STYLE);
    }

    public MessageInputToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }


    //==============================================================================================
    // API
    //==============================================================================================

    /**
     * Attaches the given Callback to this MessageInputToolbar for receiving callbacks.
     *
     * @param callback
     */
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    /**
     * Gets the Editable text from the message input area.
     *
     * @return The Editable text from the message input area.
     */
    public Editable getText() {
        return mMessageEditText.getText();
    }

    /**
     * Sets the text in the message input area.
     *
     * @param text
     * @param bufferType
     */
    public void setText(CharSequence text, TextView.BufferType bufferType) {
        mMessageEditText.setText(text, bufferType);
    }


    //==============================================================================================
    // Private
    //==============================================================================================

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        View.inflate(context, R.layout.atlas_layout_message_input_toolbar, this);

        mLeftButton = (ImageButton) findViewById(R.id.atlas_left_button);
        mLeftButton.setOnClickListener(this);
        mRightButton = (ImageButton) findViewById(R.id.atlas_right_button);
        mRightButton.setOnClickListener(this);
        mMessageEditText = (EditText) findViewById(R.id.atlas_message_edit_text);

        if (attrs == null) {
            return;
        }

        // Try populating attributes from the layout xml
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MessageInputToolbar, defStyleAttr, R.style.AtlasMessageInputToolbar);
        try {
            Drawable leftDrawable = a.getDrawable(R.styleable.MessageInputToolbar_leftButtonDrawable);
            Drawable rightDrawable = a.getDrawable(R.styleable.MessageInputToolbar_rightButtonDrawable);
            String hint = a.getString(R.styleable.MessageInputToolbar_hint);
            setAttributes(leftDrawable, rightDrawable, hint);
        } finally {
            a.recycle();
        }
    }

    public void setAttributes(Drawable leftDrawable, Drawable rightDrawable, String hint) {
        mLeftButton.setImageDrawable(leftDrawable);
        mRightButton.setImageDrawable(rightDrawable);
        mMessageEditText.setHint(hint);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.atlas_left_button) {
            if (mCallback != null) {
                mCallback.onLeftButtonClick(this, v);
            }
        } else if (v.getId() == R.id.atlas_right_button) {
            if (mCallback != null) {
                mCallback.onRightButtonClick(this, v);
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (mCallback != null) {
            mCallback.onAfterTextChanged(this, s);
        }
    }
}
