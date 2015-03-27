package com.layer.atlas.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

import com.layer.atlas.R;

public class ParticipantChipView extends ImageButton {
    private final static int DEF_STYLE = R.attr.participantChipViewStyle;


    //==============================================================================================
    // Constructor
    //==============================================================================================

    public ParticipantChipView(Context context) {
        this(context, null, DEF_STYLE);
    }

    public ParticipantChipView(Context context, AttributeSet attrs) {
        this(context, attrs, DEF_STYLE);
    }

    public ParticipantChipView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }


    //==============================================================================================
    // Private
    //==============================================================================================

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        if (attrs == null) {
            return;
        }
    }
}
