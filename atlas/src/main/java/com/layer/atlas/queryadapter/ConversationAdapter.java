package com.layer.atlas.queryadapter;

import android.content.Context;

import com.layer.atlas.AvatarItem;
import com.layer.atlas.atlasdefault.DefaultConversationDataSource;
import com.layer.atlas.atlasdefault.DefaultConversationViewHolderFactory;
import com.layer.atlas.viewholder.ConversationViewHolder;
import com.layer.atlas.viewholder.ConversationViewHolderFactory;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.SortDescriptor;

import java.util.List;

/**
 * Created by Steven Jones on 3/14/2015.
 * Copyright (c) 2015 Layer, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class ConversationAdapter extends BaseQueryAdapter<Conversation, ConversationViewHolder, ConversationViewHolderFactory> {
    private final Context mContext;

    private final DataSource mDataSource;
    private final Listener mListener;

    public ConversationAdapter(Context context, LayerClient client, ConversationViewHolderFactory factory, DataSource dataSource, Listener listener) {
        super(client, Query.builder(Conversation.class)
                        .sortDescriptor(new SortDescriptor(Conversation.Property.LAST_MESSAGE_RECEIVED_AT, SortDescriptor.Order.DESCENDING))
                        .build(),
                factory == null ? new DefaultConversationViewHolderFactory() : factory);
        mContext = context;
        mDataSource = dataSource == null ? new DefaultConversationDataSource(client) : dataSource;
        mListener = listener;
    }

    /**
     * Update ViewHolder UI by requesting Conversation data from this Adapter's DataSource, and
     * binding it to the given ViewHolder.
     *
     * @param viewHolder   The ViewHolder to update.
     * @param conversation The Conversation to bind to the given ViewHolder.
     */
    @Override
    public void onBindViewHolder(ConversationViewHolder viewHolder, Conversation conversation) {
        viewHolder.setConversation(mContext, conversation);
        viewHolder.setConversationAvatarItem(mContext, mDataSource.getConversationAvatarItem(this, conversation));
        viewHolder.setConversationTitle(mContext, mDataSource.getConversationTitle(this, conversation));
    }

    /**
     * Alert this Adapter's Listener to user interactions.
     *
     * @param target          The Conversation on which an interaction was performed.
     * @param interactionType The type of interaction performed.
     */
    @Override
    public void onInteraction(Conversation target, InteractionType interactionType) {
        switch (interactionType) {
            case SHORT_CLICK:
                mListener.onConversationSelected(ConversationAdapter.this, target);
                break;

            case LONG_CLICK:
                mListener.onConversationDeleted(ConversationAdapter.this, target, LayerClient.DeletionMode.ALL_PARTICIPANTS);
                break;
        }
    }


    //==============================================================================================
    // Inner classes
    //==============================================================================================

    /**
     * DataSource to for gathering information to supply to ViewHolders.
     */
    public static interface DataSource {
        public String getConversationTitle(ConversationAdapter adapter, Conversation conversation);

        public AvatarItem getConversationAvatarItem(ConversationAdapter adapter, Conversation conversation);
    }

    /**
     * Listener for providing user interaction feedback.
     */
    public interface Listener {
        public void onConversationSelected(ConversationAdapter adapter, Conversation conversation);

        public void onConversationDeleted(ConversationAdapter adapter, Conversation conversation, LayerClient.DeletionMode deletionMode);

        public List<String> onConversationTextSearch(ConversationAdapter adapter, String searchText);
    }

}
