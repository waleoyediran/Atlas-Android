package com.layer.atlas.atlasdefault;

import android.text.Spannable;
import android.text.SpannableString;

import com.layer.atlas.adapter.MessageQueryAdapter;
import com.layer.atlas.model.Participant;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

public class DefaultMessageDataSource implements MessageQueryAdapter.DataSource {
    private final LayerClient mClient;

    public DefaultMessageDataSource(LayerClient client) {
        mClient = client;
    }

    @Override
    public Participant getParticipant(MessageQueryAdapter adapter, String participantId) {
        return null;
    }

    @Override
    public Spannable getFormattedDate(MessageQueryAdapter adapter, Date date) {
        return new SpannableString(date.toString());
    }

    @Override
    public Spannable getFormattedReceipientStatus(MessageQueryAdapter adapter, Map<String, Message.RecipientStatus> recipientStatus) {
        return null;
    }

    @Override
    public int getMessageItemViewHeight(MessageQueryAdapter adapter, Message message) {
        return 0;
    }

    @Override
    public Conversation getConversation(MessageQueryAdapter adapter, Collection<Participant> participants) {
        return null;
    }
}