package com.layer.atlas.adapter;

import android.content.Context;
import android.text.Spannable;

import com.layer.atlas.R;
import com.layer.atlas.atlasdefault.DefaultMessageDataSource;
import com.layer.atlas.atlasdefault.DefaultMessageViewHolderFactory;
import com.layer.atlas.model.Participant;
import com.layer.atlas.viewholder.MessageViewHolder;
import com.layer.atlas.viewholder.MessageViewHolderFactory;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.SortDescriptor;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
public class MessageQueryAdapter extends BaseQueryAdapter<Message, MessageViewHolder, MessageViewHolderFactory> {
    private static final long TIME_DELTA_GROUPED = 60 * 1000L;

    private final Context mContext;

    private final DataSource mDataSource;
    private final Listener mListener;

    private volatile int mGroupedSpacing;
    private volatile int mUngroupedSpacing;

    public MessageQueryAdapter(Context context, LayerClient client, Conversation conversation, MessageViewHolderFactory factory, DataSource dataSource, Listener listener) {
        super(client, Query.builder(Message.class)
                        .sortDescriptor(new SortDescriptor(Message.Property.POSITION, SortDescriptor.Order.ASCENDING))
                        .predicate(new Predicate(Message.Property.CONVERSATION, Predicate.Operator.EQUAL_TO, conversation))
                        .build(),
                factory == null ? new DefaultMessageViewHolderFactory() : factory);
        mContext = context;
        mDataSource = dataSource == null ? new DefaultMessageDataSource(client) : dataSource;
        mListener = listener;

        mGroupedSpacing = (int) context.getResources().getDimension(R.dimen.atlas_message_bubble_spacing_grouped);
        mUngroupedSpacing = (int) context.getResources().getDimension(R.dimen.atlas_message_bubble_spacing_ungrouped);
    }

    public void setSpacing(int groupedSpacing, int ungroupedSpacing) {
        mGroupedSpacing = groupedSpacing;
        mUngroupedSpacing = ungroupedSpacing;
    }

    /**
     * Update ViewHolder UI by requesting Message data from this Adapter's DataSource, and
     * binding it to the given ViewHolder.
     *
     * @param viewHolder The ViewHolder to update.
     * @param message    The Message to bind to the given ViewHolder.
     */
    @Override
    public void onBindViewHolder(MessageViewHolder viewHolder, Message message) {
        int position = getPosition(message);
        long timeDelta = 0L;
        if (position > 0) {
            Message previousMessage = getItem(position - 1);
            timeDelta = message.getReceivedAt().getTime() - previousMessage.getReceivedAt().getTime();
        }

        if (timeDelta <= TIME_DELTA_GROUPED) {
            viewHolder.itemView.setPadding(0, mGroupedSpacing, 0, 0);
        } else {
            viewHolder.itemView.setPadding(0, mUngroupedSpacing, 0, 0);
        }

        viewHolder.setMessage(mContext, message);
        viewHolder.setMessageAvatarItemVisible(mContext, true);
        viewHolder.setMessageSender(mContext, mDataSource.getParticipant(this, message.getSentByUserId()));
    }

    /**
     * Alert this Adapter's Listener to user interactions.
     *
     * @param target          The Message on which an interaction was performed.
     * @param interactionType The type of interaction performed.
     */
    @Override
    public void onInteraction(Message target, InteractionType interactionType) {
        switch (interactionType) {
            case SHORT_CLICK:
                mListener.onMessageSelected(MessageQueryAdapter.this, target);
                break;

            case LONG_CLICK:
                mListener.onMessageDeleted(MessageQueryAdapter.this, target, LayerClient.DeletionMode.ALL_PARTICIPANTS);
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
        public Participant getParticipant(MessageQueryAdapter adapter, String participantId);

        public Spannable getFormattedDate(MessageQueryAdapter adapter, Date date);

        public Spannable getFormattedReceipientStatus(MessageQueryAdapter adapter, Map<String, Message.RecipientStatus> recipientStatus);

        public Conversation getConversation(Collection<Participant> participants);
    }

    /**
     * Listener for providing user interaction feedback.
     */
    public interface Listener {
        public void onMessageSent(MessageQueryAdapter adapter, Message message);

        public void onMessageSelected(MessageQueryAdapter adapter, Message message);

        public void onMessageDeleted(MessageQueryAdapter adapter, Message message, LayerClient.DeletionMode deletionMode);

        public int onRequestMessageItemHeight(MessageQueryAdapter adapter, Message message);

        public List<Message> onRequestMessagesForMediaAttachment(MessageQueryAdapter adapter);
    }

}
