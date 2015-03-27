package com.layer.atlas.view;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import com.layer.atlas.R;

public class ParticipantFilterView extends EditText implements TextWatcher {
    private final static int DEF_STYLE = R.attr.participantFilterViewStyle;

    private Callback mCallback;

    /**
     * MessageInputToolbar.Callback allows an observer to receive callbacks for user interactions on
     * this MessageInputToolbar.
     */
    public static interface Callback {
        /**
         * Notifies the callback of the user typing in the message input.
         *
         * @param toolbar This MessageInputToolbar.
         * @param input   The changed message input.
         */
        public void onAfterTextChanged(ParticipantFilterView toolbar, Editable input);
    }


    //==============================================================================================
    // Constructor
    //==============================================================================================

    public ParticipantFilterView(Context context) {
        this(context, null, DEF_STYLE);
    }

    public ParticipantFilterView(Context context, AttributeSet attrs) {
        this(context, attrs, DEF_STYLE);
    }

    public ParticipantFilterView(Context context, AttributeSet attrs, int defStyleAttr) {
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


    //==============================================================================================
    // Private
    //==============================================================================================

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        if (attrs == null) {
            return;
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
