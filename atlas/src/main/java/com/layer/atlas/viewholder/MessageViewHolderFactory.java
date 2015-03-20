package com.layer.atlas.viewholder;

import android.view.ViewGroup;

import com.layer.sdk.messaging.Message;

public abstract class MessageViewHolderFactory extends BaseViewHolderFactory<Message, MessageViewHolder> {
    @Override
    public abstract MessageViewHolder createViewHolder(ViewGroup viewGroup, int viewType);
}
