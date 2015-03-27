package com.layer.atlas.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.layer.atlas.R;

public class ConversationItemView extends RelativeLayout {
    private final static int DEF_STYLE = R.attr.defaultConversationItem;

    public ConversationItemView(Context context) {
        this(context, null, DEF_STYLE);
    }

    public ConversationItemView(Context context, AttributeSet attrs) {
        this(context, attrs, DEF_STYLE);
    }

    public ConversationItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        View.inflate(context, R.layout.atlas_item_conversation, this);

        // Try populating attributes from the layout xml
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AtlasConversationItem, defStyleAttr, R.style.AtlasConversationItem);
        try {
            // TODO
        } finally {
            a.recycle();
        }
    }

}
