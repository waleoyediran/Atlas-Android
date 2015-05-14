package com.layer.atlas.messenger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.atlas.Atlas.Contact;
import com.layer.sdk.LayerClient;
import com.layer.sdk.LayerClient.DeletionMode;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.Message.RecipientStatus;

/**
 * @author Oleg Orlov
 * @since 14 Apr 2015
 */
public class AtlasConversationsScreen extends Activity {
    private static final String TAG = AtlasConversationsScreen.class.getSimpleName();
    private static final boolean debug = true;

    private static final int REQUEST_CODE_LOGIN = 999;
    
    private ListView conversationsList;
    private BaseAdapter conversationsAdapter;
    private View btnNewConversation;
    
    private ArrayList<Conversation> conversations = new ArrayList<Conversation>();
    
    private App101 app;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atlas_screen_conversations);
        
        this.app = (App101) getApplication();
        final LayerClient client = app.getLayerClient();
        if (debug) Log.i(TAG, "onCreate() layerClient: " + client);

        // setup actionBar
        ((TextView)findViewById(R.id.atlas_actionbar_title_text)).setText("Conversations");
        ImageView menuBtn = (ImageView) findViewById(R.id.atlas_actionbar_left_btn);
        menuBtn.setImageResource(R.drawable.atlas_ctl_btn_menu);
        menuBtn.setVisibility(View.VISIBLE);
        menuBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), AtlasSettingsScreen.class);
                startActivity(intent);
            }
        });
        
        ImageView searchBtn = (ImageView) findViewById(R.id.atlas_actionbar_right_btn);
        searchBtn.setImageResource(R.drawable.atlas_ctl_btn_search);
        searchBtn.setVisibility(View.VISIBLE);
        searchBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(v.getContext(), "Title should be replaced by edit text here...", Toast.LENGTH_LONG).show();
            }
        });
        
        conversationsList = (ListView) findViewById(R.id.atlas_conversations_view);
        conversationsList.setAdapter(conversationsAdapter = new BaseAdapter() {
            
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) { 
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.atlas_view_conversations_convert, parent, false);
                }
                
                Uri convId = conversations.get(position).getId();
                Conversation conv = app.getLayerClient().getConversation(convId);
                
                ArrayList<String> allButMe = new ArrayList<String>(conv.getParticipants());
                allButMe.remove(client.getAuthenticatedUserId());
                
                StringBuilder sb = new StringBuilder();
                for (String userId : conv.getParticipants()) {
                    if (client.getAuthenticatedUserId().equals(userId)) {
                        continue;
                    }
                    Contact contact = app.contactsMap.get(userId);
                    String name = allButMe.size() > 1 ? App101.getContactFirstAndL(contact) : App101.getContactFirstAndLast(contact);
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(name != null ? name : userId);
                }
                TextView participants = (TextView) convertView.findViewById(R.id.atlas_conversation_view_convert_participant);
                participants.setText(sb);
                
                // avatar icons... 
                TextView textInitials = (TextView) convertView.findViewById(R.id.atlas_conversation_view_convert_initials);
                View textInitialsMulti = convertView.findViewById(R.id.atlas_conversation_view_convert_initials_multi);
                if (allButMe.size() < 2) {
                    String conterpartyUserId = allButMe.get(0);
                    Contact counterParty = app.contactsMap.get(conterpartyUserId);
                    textInitials.setText(App101.getContactInitials(counterParty));
                    textInitials.setVisibility(View.VISIBLE);
                    textInitialsMulti.setVisibility(View.GONE);
                } else {
                    TextView textInitialsLeft = (TextView) convertView.findViewById(R.id.atlas_conversation_view_convert_initials_left);
                    String leftUserId = allButMe.get(0);
                    Contact left = app.contactsMap.get(leftUserId);
                    textInitialsLeft.setText(App101.getContactInitials(left));
                    
                    TextView textInitialsRight = (TextView) convertView.findViewById(R.id.atlas_conversation_view_convert_initials_right);
                    String rightUserId = allButMe.get(1);
                    Contact right = app.contactsMap.get(rightUserId);
                    textInitialsRight.setText(App101.getContactInitials(right));
                    
                    textInitials.setVisibility(View.GONE);
                    textInitialsMulti.setVisibility(View.VISIBLE);
                }
                
                TextView textLastMessage = (TextView) convertView.findViewById(R.id.atlas_conversation_view_last_message);
                TextView timeView = (TextView) convertView.findViewById(R.id.atlas_conversation_view_convert_time);
                if (conv.getLastMessage() != null ) {
                    Message last = conv.getLastMessage();
                    String lastMessageText = App101.toString(last);
                    
                    textLastMessage.setText(lastMessageText);
                    
                    Date sentAt = last.getSentAt();
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                    if (sentAt == null) timeView.setText("...");
                    else                timeView.setText(sdf.format(sentAt));

                    String userId = last.getSender().getUserId();                   // could be null for system messages 
                    String myId = app.getLayerClient().getAuthenticatedUserId();
                    if ((userId != null) && !userId.equals(myId) && last.getRecipientStatus(myId) != RecipientStatus.READ) {
                        textLastMessage.setTypeface(null, Typeface.BOLD);
                        participants.setTypeface(null, Typeface.BOLD);
                    } else {
                        textLastMessage.setTypeface(null, Typeface.NORMAL);
                        participants.setTypeface(null, Typeface.NORMAL);
                    }
                } else {
                    textLastMessage.setText("");
                }
                
                return convertView;
            }
            public long getItemId(int position) {
                return position;
            }
            public Object getItem(int position) {
                return conversations.get(position);
            }
            public int getCount() {
                return conversations.size();
            }
        });
        
        conversationsList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Conversation conv = conversations.get(position);
                openChatScreen(conv, false);
            }
        });
        conversationsList.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Conversation conv = conversations.get(position);
                conv.delete(DeletionMode.ALL_PARTICIPANTS);
                updateValues();
                Toast.makeText(view.getContext(), "Deleted: " + conv, Toast.LENGTH_SHORT).show();;
                return true;
            }
        });
        
        btnNewConversation = findViewById(R.id.atlas_conversation_screen_new_conversation);
        btnNewConversation.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), AtlasMessagesScreen.class);
                intent.putExtra(AtlasMessagesScreen.EXTRA_CONVERSATION_IS_NEW, true);
                startActivity(intent);
                return;
            }
        });
        
        if (!app.getLayerClient().isAuthenticated()) {
            Intent intent = new Intent(this, AtlasLoginScreen.class);
            startActivityForResult(intent, 0);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_LOGIN) {
            
        }
    }

    private void updateValues() {
        App101 app = (App101) getApplication();
        LayerClient client = app.getLayerClient();
        if (app.getLayerClient().isAuthenticated()) {
            
            List<Conversation> convs = app.getLayerClient().getConversations();
            if (debug) Log.d(TAG, "updateValues() conv: " + convs.size());
            conversations.clear();
            for (Conversation conv : convs) {
                // no participants means we are removed from conversation (disconnected conversation)
                if (conv.getParticipants().size() == 0) continue;
                // only ourselves in participant list is possible to happen, but there is nothing to do with it
                // behave like conversation is disconnected
                if (conv.getParticipants().size() == 1 
                        && conv.getParticipants().contains(client.getAuthenticatedUserId())) continue;
                
                conversations.add(conv);
            }
            Collections.sort(conversations, new Comparator<Conversation>() {
                public int compare(Conversation lhs, Conversation rhs) {
                    long now = System.currentTimeMillis();
                    long leftSentAt = now;
                    if (lhs != null && lhs.getLastMessage() != null && lhs.getLastMessage().getSentAt() != null) {
                        leftSentAt = lhs.getLastMessage().getSentAt().getTime();
                    }
                    long rightSentdAt = now;
                    if (rhs != null && rhs.getLastMessage() != null && rhs.getLastMessage().getSentAt() != null) {
                        rightSentdAt = rhs.getLastMessage().getSentAt().getTime();
                    }
                    return (int) (rightSentdAt - leftSentAt);
                }
            });
            conversationsAdapter.notifyDataSetChanged();
        }
    }

    private LayerChangeEventListener.MainThread eventTracker;
    
    @Override
    protected void onResume() {
        super.onResume();
        App101 app = (App101) getApplication();
        
        app.getLayerClient().registerEventListener(eventTracker = new LayerChangeEventListener.MainThread() {
            public void onEventMainThread(LayerChangeEvent event) {
                updateValues();
            }
        });
        updateValues();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        App101 app = (App101) getApplication();
        app.getLayerClient().unregisterEventListener(eventTracker);
    }
    
    public void openChatScreen(Conversation conv, boolean newConversation) {
        Context context = this;
        Intent intent = new Intent(context, AtlasMessagesScreen.class);
        intent.putExtra(AtlasMessagesScreen.EXTRA_CONVERSATION_URI, conv.getId().toString());
        startActivity(intent);
    }
}
