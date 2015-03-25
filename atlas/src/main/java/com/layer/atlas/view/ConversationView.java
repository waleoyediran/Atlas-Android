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
import com.layer.atlas.adapter.ConversationViewAdapter;
import com.layer.atlas.viewholder.MessageViewHolderFactory;
import com.layer.sdk.LayerClient;
import com.layer.sdk.listeners.LayerTypingIndicatorListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class ConversationView extends RelativeLayout implements MessageInputToolbar.Callback {
    ConversationQueryView mConversationQueryView;
    MessageInputToolbar mMessageInputToolbar;
    ConversationViewAdapter mConversationViewAdapter;
    LayerClient mLayerClient;
    Conversation mConversation;
    boolean mInitialized = false;

    public ConversationView(Context context) {
        super(context);
        if (!mInitialized) {
            mInitialized = true;
            init(context, null, 0);
        }
    }

    public ConversationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!mInitialized) {
            mInitialized = true;
            init(context, attrs, 0);
        }
    }

    public ConversationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!mInitialized) {
            mInitialized = true;
            init(context, attrs, defStyleAttr);
        }
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        View.inflate(context, R.layout.atlas_layout_conversation_view, this);
        mConversationQueryView = (ConversationQueryView) findViewById(R.id.atlas_conversation_view);
        mMessageInputToolbar = (MessageInputToolbar) findViewById(R.id.atlas_message_input_toolbar);

        mConversationQueryView.setLayout(LinearLayoutManager.VERTICAL, false);
        mMessageInputToolbar.setCallback(this);

        // Try populating attributes from the layout xml
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ConversationView, defStyleAttr, 0);
        try {
            // ConversationQueryView
            float groupedSpacing = a.getDimension(R.styleable.ConversationView_conversationQueryViewGroupedMessageSpacing, getResources().getDimension(R.dimen.atlas_message_bubble_spacing_grouped));
            float ungroupedSpacing = a.getDimension(R.styleable.ConversationView_conversationQueryViewUngroupedMessageSpacing, getResources().getDimension(R.dimen.atlas_message_bubble_spacing_ungrouped));
            mConversationQueryView.setAttributes((int) groupedSpacing, (int) ungroupedSpacing);

            // MessageInputToolbar
            Drawable leftDrawable = a.getDrawable(R.styleable.ConversationView_messageInputToolbarLeftButtonDrawable);
            Drawable rightDrawable = a.getDrawable(R.styleable.ConversationView_messageInputToolbarRightButtonDrawable);
            String hint = a.getString(R.styleable.ConversationView_messageInputToolbarHint);
            mMessageInputToolbar.setAttributes(leftDrawable, rightDrawable, hint);
        } finally {
            a.recycle();
        }
    }

    public void set(LayerClient layerClient, Conversation conversation, MessageViewHolderFactory factory, ConversationViewAdapter.DataSource dataSource, ConversationViewAdapter.Listener listener) {
        mLayerClient = layerClient;
        mConversation = conversation;
        mConversationViewAdapter = new ConversationViewAdapter(getContext(), layerClient, conversation, factory, dataSource, listener);
        mConversationQueryView.setAdapter(mConversationViewAdapter);
        mConversationQueryView.setLayout(RecyclerView.VERTICAL, false);
    }

    public void refresh() {
        if (mConversationViewAdapter != null) {
            mConversationViewAdapter.refresh();
        }
    }

    //==============================================================================================
    // MessageInputToolbar Callback
    //==============================================================================================

    @Override
    public void onLeftButtonClick(MessageInputToolbar toolbar, View button) {

    }

    @Override
    public void onRightButtonClick(MessageInputToolbar toolbar, View button) {
        CharSequence text = mMessageInputToolbar.getText();
        sendTextMessage(text.toString());
        mMessageInputToolbar.setText("", TextView.BufferType.NORMAL);
    }

    @Override
    public void onAfterTextChanged(MessageInputToolbar toolbar, Editable input) {
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
    }
}
