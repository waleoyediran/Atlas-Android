package com.layer.atlas.sampleapp.mock;

import android.net.Uri;

import com.layer.sdk.LayerClient;
import com.layer.sdk.listeners.LayerAuthenticationListener;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.listeners.LayerConnectionListener;
import com.layer.sdk.listeners.LayerPolicyListener;
import com.layer.sdk.listeners.LayerProgressListener;
import com.layer.sdk.listeners.LayerSyncListener;
import com.layer.sdk.listeners.LayerTypingIndicatorListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.ConversationOptions;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.layer.sdk.policy.Policy;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.Queryable;
import com.layer.sdk.query.RecyclerViewController;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MockLayerClient extends LayerClient {
    public MockLayerClient() {
        //super();
    }

    @Override
    public Conversation newConversation(ConversationOptions conversationOptions, List<String> strings) {
        return null;
    }

    @Override
    public Conversation newConversation(ConversationOptions options, String... participants) {
        return super.newConversation(options, participants);
    }

    @Override
    public Conversation newConversation(List<String> participants) {
        return super.newConversation(participants);
    }

    @Override
    public Conversation newConversation(String... participants) {
        return super.newConversation(participants);
    }

    @Override
    public List<Uri> getConversationIds() {
        return null;
    }

    @Override
    public Conversation getConversation(Uri uri) {
        return null;
    }

    @Override
    public List<Conversation> getConversations(List<Uri> uris) {
        return null;
    }

    @Override
    public List<Conversation> getConversations(Uri... ids) {
        return super.getConversations(ids);
    }

    @Override
    public List<Conversation> getConversations() {
        return null;
    }

    @Override
    public List<Conversation> getConversationsWithParticipants(List<String> strings) {
        return null;
    }

    @Override
    public List<Conversation> getConversationsWithParticipants(String... participants) {
        return super.getConversationsWithParticipants(participants);
    }

    @Override
    public Message newMessage(List<MessagePart> messageParts) {
        return null;
    }

    @Override
    public Message newMessage(MessagePart... messageParts) {
        return super.newMessage(messageParts);
    }

    @Override
    public MessagePart newMessagePart(String s, byte[] bytes) {
        return null;
    }

    @Override
    public MessagePart newMessagePart(String s, InputStream stream, long l) {
        return null;
    }

    @Override
    public MessagePart newMessagePart(String plainText) {
        return super.newMessagePart(plainText);
    }

    @Override
    public List<Uri> getMessageIds(Conversation conversation) {
        return null;
    }

    @Override
    public Message getMessage(Uri uri) {
        return null;
    }

    @Override
    public List<Message> getMessages(List<Uri> uris) {
        return null;
    }

    @Override
    public List<Message> getMessages(Uri... ids) {
        return super.getMessages(ids);
    }

    @Override
    public List<Message> getMessages(Conversation conversation) {
        return null;
    }

    @Override
    public LayerClient registerProgressListener(MessagePart messagePart, LayerProgressListener listener) {
        return null;
    }

    @Override
    public LayerClient unregisterProgressListener(MessagePart messagePart, LayerProgressListener listener) {
        return null;
    }

    @Override
    public Queryable get(Uri uri) {
        return null;
    }

    @Override
    public List executeQuery(Query<? extends Queryable> query, Query.ResultType resultType) {
        return null;
    }

    @Override
    public List<Uri> executeQueryForIds(Query<? extends Queryable> query) {
        return null;
    }

    @Override
    public List<? extends Queryable> executeQueryForObjects(Query<? extends Queryable> query) {
        return null;
    }

    @Override
    public Long executeQueryForCount(Query<? extends Queryable> query) {
        return null;
    }

    @Override
    public <T extends Queryable> RecyclerViewController<T> newRecyclerViewController(Query<T> tQuery, Collection<String> strings, RecyclerViewController.Callback callback) {
        return null;
    }

    @Override
    public LayerClient registerEventListener(LayerChangeEventListener layerChangeEventListener) {
        return null;
    }

    @Override
    public LayerClient unregisterEventListener(LayerChangeEventListener layerChangeEventListener) {
        return null;
    }

    @Override
    public String getAuthenticatedUserId() {
        return null;
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public LayerClient authenticate() {
        return null;
    }

    @Override
    public LayerClient deauthenticate() {
        return null;
    }

    @Override
    public void answerAuthenticationChallenge(String s) {

    }

    @Override
    public LayerClient registerAuthenticationListener(LayerAuthenticationListener layerAuthenticationListener) {
        return null;
    }

    @Override
    public LayerClient unregisterAuthenticationListener(LayerAuthenticationListener layerAuthenticationListener) {
        return null;
    }

    @Override
    public LayerClient connect() {
        return null;
    }

    @Override
    public LayerClient disconnect() {
        return null;
    }

    @Override
    public LayerClient registerConnectionListener(LayerConnectionListener layerConnectionListener) {
        return null;
    }

    @Override
    public LayerClient unregisterConnectionListener(LayerConnectionListener layerConnectionListener) {
        return null;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isConnecting() {
        return false;
    }

    @Override
    public LayerClient registerSyncListener(LayerSyncListener layerSyncListener) {
        return null;
    }

    @Override
    public LayerClient unregisterSyncListener(LayerSyncListener layerSyncListener) {
        return null;
    }

    @Override
    public LayerClient setAutoDownloadSizeThreshold(long l) {
        return null;
    }

    @Override
    public long getAutoDownloadSizeThreshold() {
        return 0;
    }

    @Override
    public LayerClient setAutoDownloadMimeTypes(Collection<String> strings) {
        return null;
    }

    @Override
    public Set<String> getAutoDownloadMimeTypes() {
        return null;
    }

    @Override
    public LayerClient setDiskCapacity(long l) {
        return null;
    }

    @Override
    public long getDiskCapacity() {
        return 0;
    }

    @Override
    public long getDiskUtilization() {
        return 0;
    }

    @Override
    public LayerClient registerTypingIndicator(LayerTypingIndicatorListener layerTypingIndicatorListener) {
        return null;
    }

    @Override
    public LayerClient unregisterTypingIndicator(LayerTypingIndicatorListener layerTypingIndicatorListener) {
        return null;
    }

    @Override
    public UUID getAppId() {
        return null;
    }

    @Override
    public void setGcmRegistrationId(String s, String s2) {

    }

    @Override
    public List<Policy> getPolicies() {
        return null;
    }

    @Override
    public boolean validatePolicy(Policy policy) {
        return false;
    }

    @Override
    public boolean addPolicy(Policy policy) {
        return false;
    }

    @Override
    public boolean insertPolicy(Policy policy, int i) {
        return false;
    }

    @Override
    public boolean removePolicy(Policy policy) {
        return false;
    }

    @Override
    public LayerClient registerPolicyListener(LayerPolicyListener layerPolicyListener) {
        return null;
    }

    @Override
    public LayerClient unregisterPolicyListener(LayerPolicyListener layerPolicyListener) {
        return null;
    }

    @Override
    public void addParticipants(Conversation conversation, List<String> strings) {

    }

    @Override
    public void addParticipants(Conversation conversation, String... participants) {
        super.addParticipants(conversation, participants);
    }

    @Override
    public void removeParticipants(Conversation conversation, List<String> strings) {

    }

    @Override
    public void removeParticipants(Conversation conversation, String... participants) {
        super.removeParticipants(conversation, participants);
    }

    @Override
    public void deleteConversation(Conversation conversation, DeletionMode deletionMode) {

    }

    @Override
    public void deleteConversation(Conversation conversation) {

    }

    @Override
    public void putMetadataAtKeyPath(Conversation conversation, String s, String s2) {

    }

    @Override
    public void putMetadata(Conversation conversation, Map<String, Object> stringObjectMap, boolean b) {

    }

    @Override
    public void removeMetadataAtKeyPath(Conversation conversation, String s) {

    }

    @Override
    public void sendMessage(Conversation conversation, Message message) {

    }

    @Override
    public void markMessageAsRead(Message message) {

    }

    @Override
    public void deleteMessage(Message message, DeletionMode deletionMode) {

    }

    @Override
    public void deleteMessage(Message message) {

    }

    @Override
    public void setMetadata(Message message, Map<String, String> stringStringMap) {

    }

    @Override
    public LayerClient sendTypingIndicator(Conversation conversation, LayerTypingIndicatorListener.TypingIndicator typingIndicator) {
        return null;
    }

    @Override
    public Integer getUnreadMessageCount(Conversation conversation) {
        return null;
    }
}
