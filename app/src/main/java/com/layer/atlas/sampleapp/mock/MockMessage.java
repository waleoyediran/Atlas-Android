package com.layer.atlas.sampleapp.mock;

import android.net.Uri;

import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class MockMessage extends Message {
    public MockMessage() {
        super();
    }

    @Override
    public void delete(LayerClient.DeletionMode deletionMode) {

    }

    @Override
    public void setMetadata(Map<String, String> stringStringMap) {

    }

    @Override
    public void markAsRead() {

    }

    @Override
    public Uri getId() {
        return null;
    }

    @Override
    public long getPosition() {
        return 0;
    }

    @Override
    public Conversation getConversation() {
        return null;
    }

    @Override
    public List<MessagePart> getMessageParts() {
        return null;
    }

    @Override
    public boolean isSent() {
        return false;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public Date getSentAt() {
        return null;
    }

    @Override
    public Date getReceivedAt() {
        return null;
    }

    @Override
    public String getSentByUserId() {
        return null;
    }

    @Override
    public Map<String, RecipientStatus> getRecipientStatus() {
        return null;
    }

    @Override
    public RecipientStatus getRecipientStatus(String s) {
        return null;
    }
}
