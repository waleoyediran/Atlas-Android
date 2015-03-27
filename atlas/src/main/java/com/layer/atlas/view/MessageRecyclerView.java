package com.layer.atlas.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.layer.atlas.R;
import com.layer.atlas.adapter.MessageQueryAdapter;

public class MessageRecyclerView extends BaseRecyclerView {
    private final static int DEF_STYLE = R.attr.messageRecyclerViewStyle;

    private int mGroupedSpacing;
    private int mUngroupedSpacing;

    public MessageRecyclerView(Context context) {
        this(context, null, DEF_STYLE);
    }

    public MessageRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, DEF_STYLE);
    }

    public MessageRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        // Try populating attributes from the layout xml
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MessageInputView, defStyleAttr, R.style.MessageRecyclerView);
        try {
            setAttributes((int) a.getDimension(R.styleable.MessageRecyclerView_groupedMessageSpacing, getResources().getDimension(R.dimen.atlas_message_bubble_spacing_grouped)),
                    (int) a.getDimension(R.styleable.MessageRecyclerView_ungroupedMessageSpacing, getResources().getDimension(R.dimen.atlas_message_bubble_spacing_grouped)));
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
        if (adapter instanceof MessageQueryAdapter) {
            ((MessageQueryAdapter) adapter).setSpacing(mGroupedSpacing, mUngroupedSpacing);
        }
        super.setAdapter(adapter);
    }
}
