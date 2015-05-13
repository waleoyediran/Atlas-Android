package com.layer.atlas.messenger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.layer.atlas.messenger.App101.Contact;
import com.layer.atlas.messenger.App101.keys;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

public class ChatScreen extends Activity {

    private static final String TAG = ChatScreen.class.getSimpleName();
    private static final boolean debug = false;
    
    private Conversation conv;
    private ArrayList<Message> messages = new ArrayList<Message>();
    private BaseAdapter msgAdapter;
    
    private TextView messageText;
    private TextView buddiesText;
    private ListView messagesList;
    private View btnSend;
    private BaseAdapter messagesAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_screen);
        final App101 app = (App101) getApplication();

        buddiesText = (TextView) findViewById(R.id.chat_screen_text_buddies);
        
        messageText = (TextView) findViewById(R.id.chat_screen_text_message);
        
        btnSend = findViewById(R.id.chat_screen_btn_send);
        btnSend.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String text = messageText.getText().toString();
                if (text.trim().length() > 0) {
                    MessagePart mp = app.getLayerClient().newMessagePart(text);
                    Message msg = app.getLayerClient().newMessage(Arrays.asList(new MessagePart[] {mp}));
                    conv.send(msg);
                    messageText.setText("");
                }
            }
        });
        messagesList = (ListView) findViewById(R.id.chat_screen_list_messages);
        messagesList.setAdapter(messagesAdapter = new BaseAdapter() {
            private int nextId = 0;
            private final HashMap id2converts = new HashMap();
            
            private final int TYPE_ME = 0; 
            private final int TYPE_OTHER = 1;
            
            class ViewTag {
                int id;
                int type;
                ViewTag(int id, int type) {
                    this.id = id;
                    this.type = type;
                }
            }
            
            public View getView(int position, View convertView, ViewGroup parent) {
                Message msg = messages.get(position);
                String userId = msg.getSender().getUserId();
                Contact contact = app.contactsMap.get(userId);
                
                int typeRequired = app.getLayerClient().getAuthenticatedUserId().equals(contact.userId) ? TYPE_ME : TYPE_OTHER;
                
                if (convertView == null || ((ViewTag)convertView.getTag()).type != typeRequired) { 
                    convertView = LayoutInflater.from(parent.getContext()).inflate(
                            typeRequired == TYPE_ME ? R.layout.chat_screen_list_convert_me
                                                    : R.layout.chat_screen_list_convert, parent, false);
                    final ViewTag tag = new ViewTag(nextId++, typeRequired);
                    convertView.setTag(tag);
                    id2converts.put(tag.id, convertView);
                }
                if (false) Log.d(TAG, "getView() " + position + ", msg:" + msg + ": convert: " + convertView + ", total: " + id2converts.size());
                TextView text = (TextView) convertView.findViewById(R.id.chat_screen_list_convert_text);
                text.setText(ChatScreen.toString(msg));
                if (typeRequired == TYPE_OTHER) {
                    String displayText = App101.getContactInitials(contact);
                    TextView buddy = (TextView) convertView.findViewById(R.id.chat_screen_list_convert_text_buddy);
                    buddy.setText(displayText);
                }
                return convertView;
            }
            public long getItemId(int position) {
                return position;
            }
            public Object getItem(int position) {
                return messages.get(position);
            }
            public int getCount() {
                return messages.size();
            }
        });
    }
    
    private static String toString(Message msg) {
        StringBuilder sb = new StringBuilder();
        for (MessagePart mp : msg.getMessageParts()) {
            if ("text/plain".equals(mp.getMimeType())) {
                sb.append(new String(mp.getData()));
            }
        }
        return sb.toString();
    }

    /**  */
    private void updateValues() {
//        if (debug) Log.w(TAG, "updateValues() called from: " + Log.printStackTrace());
        App101 app = (App101) getApplication();
        
        if (conv == null) {
            initConversation();
            if (conv == null) return; 
        }
        List<Message> messages = app.getLayerClient().getMessages(conv);
        this.messages.clear();
        this.messages.addAll(messages);
        messagesAdapter.notifyDataSetChanged();
        
        // update buddies:
        StringBuilder sb = new StringBuilder();
        for (String userId : conv.getParticipants()) {
            String initials = App101.getContactFirstAndL(app.contactsMap.get(userId));
            sb.append(initials != null ? initials : userId).append(", ");
        }
        buddiesText.setText(sb);
    }
    
    private LayerChangeEventListener.MainThread eventTracker;
    
    @Override
    protected void onResume() {
        super.onResume();
        App101 app = (App101) getApplication();
        
        initConversation();
        
        app.getLayerClient().registerEventListener(eventTracker = new LayerChangeEventListener.MainThread() {
            public void onEventMainThread(LayerChangeEvent event) {
                updateValues();
                messagesList.smoothScrollToPosition(messagesAdapter.getCount() - 1);
            }
        });
        
        updateValues();
        messagesList.setSelection(messagesAdapter.getCount() - 1);
    }

    public void initConversation() {
        App101 app = (App101) getApplication();
        Intent ourIntent = getIntent();
//        if (false) Log.w(TAG, "onResume() act: " + ourIntent.getAction() + ", extras: " + Log.toString(ourIntent.getExtras(), "\n","\n"));
        String convUri = ourIntent.getStringExtra(keys.CONVERSATION_URI);
        Uri uri = Uri.parse(convUri);
        conv = app.getLayerClient().getConversation(uri);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        App101 app = (App101) getApplication();
        app.getLayerClient().unregisterEventListener(eventTracker);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (debug) Log.w(TAG, "onConfigurationChanged() newConfig: " + newConfig);
        updateValues();
        messagesList.smoothScrollToPosition(messagesAdapter.getCount() - 1);
    }
    
    

}
