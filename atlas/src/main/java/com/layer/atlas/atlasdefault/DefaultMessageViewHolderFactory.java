package com.layer.atlas.atlasdefault;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.layer.atlas.R;
import com.layer.atlas.viewholder.MessageViewHolder;
import com.layer.atlas.viewholder.MessageViewHolderFactory;

public class DefaultMessageViewHolderFactory extends MessageViewHolderFactory {
    @Override
    public MessageViewHolder createViewHolder(ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        return new DefaultMessageViewHolder(inflater.inflate(R.layout.atlas_item_message, null));
    }
}