package com.layer.atlas.impl;

import android.content.Context;
import android.view.LayoutInflater;

import com.layer.atlas.R;
import com.layer.atlas.adapter.ConversationAdapter;
import com.layer.atlas.adapter.ConversationViewHolder;
import com.layer.sdk.messaging.Conversation;

public class DefaultConversationViewHolderFactory implements ConversationAdapter.ViewHolderFactory {
    @Override
    public int getConversationViewType(ConversationAdapter adapter, Conversation conversation) {
        return 1;
    }

    @Override
    public ConversationViewHolder createConversationViewHolder(ConversationAdapter adapter, Context context, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return new DefaultConversationViewHolder(inflater.inflate(R.layout.atlas_item_conversation, null));
    }
}
