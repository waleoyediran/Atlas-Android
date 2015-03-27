package com.layer.atlas.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.layer.atlas.R;
import com.layer.atlas.adapter.MessageQueryAdapter;
import com.layer.atlas.viewholder.MessageViewHolderFactory;
import com.layer.sdk.LayerClient;
import com.layer.sdk.listeners.LayerTypingIndicatorListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * ConversationView is a default layout that combines a MessageRecyclerView with a MessageInputView.
 */
public class ConversationView extends RelativeLayout implements MessageInputView.Callback {
    private final static int DEF_STYLE = R.attr.conversationViewStyle;

    Listener mListener;

    MessageRecyclerView mMessageRecyclerView;
    MessageInputView mMessageInputView;
    MessageQueryAdapter mMessageQueryAdapter;
    LayerClient mLayerClient;
    Conversation mConversation;

    /**
     * Listener for providing user interaction feedback.
     */
    public interface Listener extends MessageQueryAdapter.Listener {
        public void onMessageSent(ConversationView view, Message message);
    }

    public ConversationView(Context context) {
        this(context, null, DEF_STYLE);
    }

    public ConversationView(Context context, AttributeSet attrs) {
        this(context, attrs, DEF_STYLE);
    }

    public ConversationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        View.inflate(context, R.layout.atlas_layout_conversation_view, this);
        mMessageRecyclerView = (MessageRecyclerView) findViewById(R.id.atlas_conversation_view);
        mMessageInputView = (MessageInputView) findViewById(R.id.atlas_message_input_toolbar);

        mMessageRecyclerView.setLayout(LinearLayoutManager.VERTICAL, false);
        mMessageInputView.setCallback(this);

        // Try populating attributes from the layout xml
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ConversationView, defStyleAttr, R.style.ConversationView);
        try {
            // ConversationQueryView
            float groupedSpacing = a.getDimension(R.styleable.ConversationView_groupedMessageSpacing, getResources().getDimension(R.dimen.atlas_message_bubble_spacing_grouped));
            float ungroupedSpacing = a.getDimension(R.styleable.ConversationView_ungroupedMessageSpacing, getResources().getDimension(R.dimen.atlas_message_bubble_spacing_ungrouped));
            mMessageRecyclerView.setAttributes((int) groupedSpacing, (int) ungroupedSpacing);

            // MessageInputToolbar
            Drawable leftDrawable = a.getDrawable(R.styleable.ConversationView_leftButtonDrawable);
            Drawable rightDrawable = a.getDrawable(R.styleable.ConversationView_rightButtonDrawable);
            String hint = a.getString(R.styleable.ConversationView_hint);
            mMessageInputView.setAttributes(leftDrawable, rightDrawable, hint);
        } finally {
            a.recycle();
        }
    }

    public void set(LayerClient layerClient, Conversation conversation, MessageViewHolderFactory factory, MessageQueryAdapter.DataSource dataSource, Listener listener) {
        mLayerClient = layerClient;
        mConversation = conversation;
        mListener = listener;
        mMessageQueryAdapter = new MessageQueryAdapter(getContext(), layerClient, conversation, factory, dataSource, listener);
        mMessageRecyclerView.setAdapter(mMessageQueryAdapter);
        mMessageRecyclerView.setLayout(RecyclerView.VERTICAL, false);
    }

    public void refresh() {
        if (mMessageQueryAdapter != null) {
            mMessageQueryAdapter.refresh();
        }
    }

    //==============================================================================================
    // MessageInputToolbar Callback
    //==============================================================================================

    @Override
    public void onLeftButtonClick(MessageInputView toolbar, View button) {

    }

    @Override
    public void onRightButtonClick(MessageInputView toolbar, View button) {
        CharSequence text = mMessageInputView.getText();
        sendTextMessage(text.toString());
        mMessageInputView.setText("", TextView.BufferType.NORMAL);
    }

    @Override
    public void onAfterTextChanged(MessageInputView toolbar, Editable input) {
        if (input.length() > 0) {
            mConversation.send(LayerTypingIndicatorListener.TypingIndicator.STARTED);
        } else {
            mConversation.send(LayerTypingIndicatorListener.TypingIndicator.FINISHED);
        }
    }

    private void sendTextMessage(String text) {
        if (text == null || text.trim().equals("")) {
            return;
        }

        Message message = null;
        try {
            message = mLayerClient.newMessage(mLayerClient.newMessagePart("text/plain", text.getBytes("UTF-8")));
            String senderName = mLayerClient.getAuthenticatedUserId();
            Map<String, String> metadata = new HashMap<String, String>();
            if (senderName != null && !senderName.isEmpty()) {
                metadata.put(Message.ReservedMetadataKeys.PushNotificationAlertMessageKey.getKey(), senderName + ": " + text);
            } else {
                metadata.put(Message.ReservedMetadataKeys.PushNotificationAlertMessageKey.getKey(), text);
            }
            metadata.put(Message.ReservedMetadataKeys.PushNotificationSoundNameKey.getKey(), "buzz");
            message.setMetadata(metadata);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        mConversation.send(message);
        mConversation.send(LayerTypingIndicatorListener.TypingIndicator.FINISHED);
        if (mListener != null) {
            // TODO: async from isSent callback?
            mListener.onMessageSent(this, message);
        }
    }
}
