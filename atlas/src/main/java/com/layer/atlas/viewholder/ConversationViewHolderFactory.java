package com.layer.atlas.viewholder;

import android.view.ViewGroup;

import com.layer.sdk.messaging.Conversation;

public abstract class ConversationViewHolderFactory extends BaseViewHolderFactory<Conversation, ConversationViewHolder> {
    @Override
    public abstract ConversationViewHolder createViewHolder(ViewGroup viewGroup, int viewType);
}
