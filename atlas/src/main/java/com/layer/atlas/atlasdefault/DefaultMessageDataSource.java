package com.layer.atlas.atlasdefault;

import android.text.Spannable;
import android.text.SpannableString;

import com.layer.atlas.model.Participant;
import com.layer.atlas.adapter.ConversationViewAdapter;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

public class DefaultMessageDataSource implements ConversationViewAdapter.DataSource {
    private final LayerClient mClient;

    public DefaultMessageDataSource(LayerClient client) {
        mClient = client;
    }

    @Override
    public Participant getParticipant(ConversationViewAdapter adapter, String participantId) {
        return null;
    }

    @Override
    public Spannable getFormattedDate(ConversationViewAdapter adapter, Date date) {
        return new SpannableString(date.toString());
    }

    @Override
    public Spannable getFormattedReceipientStatus(ConversationViewAdapter adapter, Map<String, Message.RecipientStatus> recipientStatus) {
        return null;
    }

    @Override
    public Conversation getConversation(Collection<Participant> participants) {
        return null;
    }
}