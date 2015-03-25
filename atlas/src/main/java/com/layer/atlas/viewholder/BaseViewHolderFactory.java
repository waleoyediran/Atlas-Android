package com.layer.atlas.viewholder;

import android.view.ViewGroup;

public abstract class BaseViewHolderFactory<Titem, Tview extends BaseViewHolder<Titem>> {
    public int getViewType(Titem queryable) {
        return 1;
    }

    public abstract Tview createViewHolder(ViewGroup viewGroup, int viewType);
}
