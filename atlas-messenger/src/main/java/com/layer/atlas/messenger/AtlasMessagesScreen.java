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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.layer.atlas.ParticipantPicker;
import com.layer.atlas.messenger.App101.Contact;
import com.layer.atlas.messenger.App101.keys;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.internal.utils.Log;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

/**
 * @author Oleg Orlov
 * @since 14 Apr 2015
 */
public class AtlasMessagesScreen extends Activity {

    private static final String TAG = AtlasMessagesScreen.class.getSimpleName();
    private static final boolean debug = true;
    
    public static final String EXTRA_CONVERSATION_IS_NEW = "conversation.new";
    public static final String EXTRA_CONVERSATION_URI = keys.CONVERSATION_URI;
    
    public static final int REQUEST_CODE_SETTINGS = 101;
    
    private Conversation conv;
    private ArrayList<Message> messages = new ArrayList<Message>();
    private BaseAdapter msgAdapter;
    
    private TextView messageText;
    private ListView messagesList;
    private View btnSend;
    private BaseAdapter messagesAdapter;
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atlas_screen_messages);
        final App101 app = (App101) getApplication();

        ImageView menuBtn = (ImageView) findViewById(R.id.atlas_actionbar_left_btn);
        menuBtn.setImageResource(R.drawable.atlas_ctl_btn_back);
        menuBtn.setVisibility(View.VISIBLE);
        menuBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        ((TextView)findViewById(R.id.atlas_actionbar_title_text)).setText("Messages");
        ImageView settingsBtn = (ImageView) findViewById(R.id.atlas_actionbar_right_btn);
        settingsBtn.setImageResource(R.drawable.atlas_ctl_btn_detail);
        settingsBtn.setVisibility(View.VISIBLE);
        settingsBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (conv == null) return; 
                AtlasConversationSettingsScreen.conv = conv;
                Intent intent = new Intent(v.getContext(), AtlasConversationSettingsScreen.class);
                startActivityForResult(intent, REQUEST_CODE_SETTINGS);
            }
        });
        
        String convUri = getIntent().getStringExtra(EXTRA_CONVERSATION_URI);
        if (convUri != null) {
            Uri uri = Uri.parse(convUri);
            conv = app.getLayerClient().getConversation(uri);
        }

        boolean convIsNew = getIntent().getBooleanExtra(EXTRA_CONVERSATION_IS_NEW, false);

        final View participantsPickerRoot = findViewById(R.id.atlas_screen_messages_participants_picker);
        final ParticipantPicker pp = new ParticipantPicker(this, participantsPickerRoot, app, null);
        if (convIsNew) {
            participantsPickerRoot.setVisibility(View.VISIBLE);
        }
        
        messageText = (TextView) findViewById(R.id.atlas_messages_composer_text);
        
        btnSend = findViewById(R.id.atlas_messages_composer_send);
        btnSend.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String text = messageText.getText().toString();
                if (text.trim().length() > 0) {
                    
                    if (conv == null) { // create new one
                        String[] userIds = pp.getSelectedUserIds();
                        conv = app.getLayerClient().newConversation(userIds);
                        participantsPickerRoot.setVisibility(View.GONE);
                    }
                    
                    MessagePart mp = app.getLayerClient().newMessagePart(text);
                    Message msg = app.getLayerClient().newMessage(Arrays.asList(new MessagePart[] {mp}));
                    conv.send(msg);
                    messageText.setText("");
                }
            }
        });
        messagesList = (ListView) findViewById(R.id.atlas_messages_view);
        messagesList.setAdapter(messagesAdapter = new BaseAdapter() {
            private int nextId = 0;
            private final HashMap id2converts = new HashMap();
            
            private static final int TYPE_ME = 0; 
            private static final int TYPE_OTHER = 1;
            
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
                String userId = msg.getSentByUserId();
                Contact contact = app.contactsMap.get(userId);
                
                int viewType = app.getLayerClient().getAuthenticatedUserId().equals(contact.userId) ? TYPE_ME : TYPE_OTHER;
                
                if (convertView == null) { 
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.atlas_view_messages_convert, parent, false);
                    ViewTag tag = new ViewTag(nextId++, viewType);
                    convertView.setTag(tag);
                    id2converts.put(tag.id, convertView);
                }
                if (false) Log.d(TAG, "getView() " + position + ", msg:" + msg + ": convert: " + convertView + ", total: " + id2converts.size());
                
                TextView textMy = (TextView) convertView.findViewById(R.id.atlas_view_messages_convert_text);
                TextView textOther = (TextView) convertView.findViewById(R.id.atlas_view_messages_convert_text_counterparty);
                TextView textAvatar = (TextView) convertView.findViewById(R.id.atlas_view_messages_convert_initials);
                if (viewType == TYPE_OTHER) {
                    textOther.setVisibility(View.VISIBLE);
                    textOther.setText(AtlasMessagesScreen.toString(msg));
                    String displayText = App101.getContactInitials(contact);
                    textAvatar.setText(displayText);
                    textAvatar.setVisibility(View.VISIBLE);
                    textMy.setVisibility(View.GONE);
                } else {
                    textMy.setVisibility(View.VISIBLE);
                    textMy.setText(AtlasMessagesScreen.toString(msg));
                    textOther.setVisibility(View.GONE);
                    textAvatar.setVisibility(View.GONE);
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
        if (debug) Log.w(TAG, "updateValues() called from: " + Log.printStackTrace());
        App101 app = (App101) getApplication();
        
        if (conv == null) {
            Log.e(TAG, "updateValues() no conversation set");
            return;
        }
        
        List<Message> messages = app.getLayerClient().getMessages(conv);
        this.messages.clear();
        this.messages.addAll(messages);
        messagesAdapter.notifyDataSetChanged();
        
        // update buddies:
        StringBuilder sb = new StringBuilder();
        for (String userId : conv.getParticipants()) {
            if (app.getLayerClient().getAuthenticatedUserId().equals(userId)) continue;
            Contact contact = app.contactsMap.get(userId);
            String initials = conv.getParticipants().size() > 2 ? App101.getContactFirstAndL(contact) : App101.getContactFirstAndLast(contact);
            if (sb.length() > 0) sb.append(", ");
            sb.append(initials != null ? initials : userId);
        }
        TextView titleText = (TextView) findViewById(R.id.atlas_actionbar_title_text);
        titleText.setText(sb);
    }
    
    private LayerChangeEventListener.MainThread eventTracker;
    
    @Override
    protected void onResume() {
        super.onResume();
        App101 app = (App101) getApplication();
        
        app.getLayerClient().registerEventListener(eventTracker = new LayerChangeEventListener.MainThread() {
            public void onEventMainThread(LayerChangeEvent event) {
                updateValues();
                messagesList.smoothScrollToPosition(messagesAdapter.getCount() - 1);
            }
        });
        
        updateValues();
        messagesList.setSelection(messagesAdapter.getCount() - 1);
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
