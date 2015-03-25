package com.layer.atlas.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.layer.atlas.R;
import com.layer.atlas.adapter.ConversationViewAdapter;

public class ConversationQueryView extends BaseQueryView {
    private int mGroupedSpacing;
    private int mUngroupedSpacing;
    boolean mInitialized = false;

    public ConversationQueryView(Context context) {
        super(context);
        if (!mInitialized) {
            mInitialized = true;
            init(context, null, 0);
        }
    }

    public ConversationQueryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!mInitialized) {
            mInitialized = true;
            init(context, attrs, 0);
        }
    }

    public ConversationQueryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (!mInitialized) {
            mInitialized = true;
            init(context, attrs, defStyle);
        }
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        // Try populating attributes from the layout xml
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MessageInputToolbar, defStyleAttr, 0);
        try {
            setAttributes((int) a.getDimension(R.styleable.ConversationQueryView_groupedMessageSpacing, getResources().getDimension(R.dimen.atlas_message_bubble_spacing_grouped)),
                    (int) a.getDimension(R.styleable.ConversationQueryView_ungroupedMessageSpacing, getResources().getDimension(R.dimen.atlas_message_bubble_spacing_grouped)));
        } finally {
            a.recycle();
        }
    }

    public void setAttributes(int groupedSpacing, int ungroupedSpacing) {
        mGroupedSpacing = groupedSpacing;
        mUngroupedSpacing = ungroupedSpacing;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if (adapter instanceof ConversationViewAdapter) {
            ((ConversationViewAdapter) adapter).setSpacing(mGroupedSpacing, mUngroupedSpacing);
        }
        super.setAdapter(adapter);
    }
}
