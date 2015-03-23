package com.layer.atlas.view;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

public abstract class BaseQueryView extends RecyclerView {
    public BaseQueryView(Context context) {
        super(context);
    }

    public BaseQueryView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseQueryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setLayout(int orientation, boolean reverseLayout) {
        setLayoutManager(new LinearLayoutManager(getContext(), orientation, reverseLayout));
    }
}
