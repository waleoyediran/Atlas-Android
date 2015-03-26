package com.layer.atlas.atlasdefault;

import android.view.ViewGroup;

import com.layer.atlas.view.MessageItem;
import com.layer.atlas.viewholder.MessageViewHolder;
import com.layer.atlas.viewholder.MessageViewHolderFactory;

public class DefaultMessageViewHolderFactory extends MessageViewHolderFactory {
    @Override
    public MessageViewHolder createViewHolder(ViewGroup viewGroup, int viewType) {
        return new DefaultMessageViewHolder(new MessageItem(viewGroup.getContext()));
    }
}
