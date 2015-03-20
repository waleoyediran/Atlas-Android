package com.layer.atlas.sampleapp.mock;

import android.net.Uri;

import com.layer.sdk.LayerClient;
import com.layer.sdk.listeners.LayerProgressListener;
import com.layer.sdk.listeners.LayerTypingIndicatorListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;

import java.util.List;
import java.util.Map;

public class MockConversation extends Conversation {
    public MockConversation() {
        super();
    }

    @Override
    public void addParticipants(List<String> strings) {

    }

    @Override
    public void addParticipants(String... participants) {
        super.addParticipants(participants);
    }

    @Override
    public void removeParticipants(List<String> strings) {

    }

    @Override
    public void removeParticipants(String... participants) {
        super.removeParticipants(participants);
    }

    @Override
    public void delete(LayerClient.DeletionMode deletionMode) {

    }

    @Override
    public void putMetadataAtKeyPath(String s, String s2) {

    }

    @Override
    public void putMetadata(Map<String, Object> stringObjectMap, boolean b) {

    }

    @Override
    public void removeMetadataAtKeyPath(String s) {

    }

    @Override
    public void send(Message message) {

    }

    @Override
    public void send(LayerTypingIndicatorListener.TypingIndicator typingIndicator) {

    }

    @Override
    public void send(Message message, LayerProgressListener listener) {

    }

    @Override
    public Uri getId() {
        return null;
    }

    @Override
    public List<String> getParticipants() {
        return null;
    }

    @Override
    public Message getLastMessage() {
        return null;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return null;
    }

    @Override
    public boolean isDeliveryReceiptsEnabled() {
        return false;
    }
}
