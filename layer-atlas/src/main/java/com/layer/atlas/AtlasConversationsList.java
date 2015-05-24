package com.layer.atlas;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.layer.atlas.Atlas.AtlasContactProvider;
import com.layer.atlas.Atlas.Contact;
import com.layer.sdk.LayerClient;
import com.layer.sdk.changes.LayerChange;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerAuthenticationListener;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.LayerObject;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.Message.RecipientStatus;

/**
 * @author Oleg Orlov
 * @since 14 May 2015
 */
public class AtlasConversationsList extends FrameLayout implements LayerChangeEventListener.MainThread {
    
    private static final String TAG = AtlasConversationsList.class.getSimpleName();
    private static final boolean debug = true;

    private ListView conversationsList;
    private BaseAdapter conversationsAdapter;

    private ArrayList<Conversation> conversations = new ArrayList<Conversation>();
    
    private LayerClient layerClient;
    
    private ConversationClickListener clickListener;
    private ConversationLongClickListener longClickListener;
    
    public AtlasConversationsList(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public AtlasConversationsList(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AtlasConversationsList(Context context) {
        super(context);
    }

    public void init(View rootView, final LayerClient layerClient, final AtlasContactProvider contactProvider) {
        if (layerClient == null) throw new IllegalArgumentException("LayerClient cannot be null");
        if (contactProvider == null) throw new IllegalArgumentException("ContactProvider cannot be null");
        
        this.layerClient = layerClient;
        
        // inflate childs:
        LayoutInflater.from(getContext()).inflate(R.layout.atlas_conversations_list, this);
        
        this.conversationsList = (ListView) rootView.findViewById(R.id.atlas_conversations_view);
        this.conversationsList.setAdapter(conversationsAdapter = new BaseAdapter() {
            
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) { 
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.atlas_view_conversations_list_convert, parent, false);
                }
                
                Uri convId = conversations.get(position).getId();
                Conversation conv = layerClient.getConversation(convId);
                
                ArrayList<String> allButMe = new ArrayList<String>(conv.getParticipants());
                allButMe.remove(layerClient.getAuthenticatedUserId());
                
                StringBuilder sb = new StringBuilder();
                for (String userId : conv.getParticipants()) {
                    if (layerClient.getAuthenticatedUserId().equals(userId)) {
                        continue;
                    }
                    Contact contact = contactProvider.contactsMap.get(userId);
                    String name = allButMe.size() > 1 ? AtlasContactProvider.getContactFirstAndL(contact) : AtlasContactProvider.getContactFirstAndLast(contact);
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(name != null ? name : userId);
                }
                TextView participants = (TextView) convertView.findViewById(R.id.atlas_conversation_view_convert_participant);
                participants.setText(sb);
                
                // avatar icons... 
                TextView textInitials = (TextView) convertView.findViewById(R.id.atlas_view_conversations_list_convert_avatar_single_text);
                View avatarSingle = convertView.findViewById(R.id.atlas_view_conversations_list_convert_avatar_single);
                View avatarMulti = convertView.findViewById(R.id.atlas_view_conversations_list_convert_avatar_multi);
                if (allButMe.size() < 2) {
                    String conterpartyUserId = allButMe.get(0);
                    Contact counterParty = contactProvider.contactsMap.get(conterpartyUserId);
                    textInitials.setText(AtlasContactProvider.getContactInitials(counterParty));
                    avatarSingle.setVisibility(View.VISIBLE);
                    avatarMulti.setVisibility(View.GONE);
                } else {
                    TextView textInitialsLeft = (TextView) convertView.findViewById(R.id.atlas_view_conversations_list_convert_avatar_multi_left);
                    String leftUserId = allButMe.get(0);
                    Contact left = contactProvider.contactsMap.get(leftUserId);
                    textInitialsLeft.setText(AtlasContactProvider.getContactInitials(left));
                    
                    TextView textInitialsRight = (TextView) convertView.findViewById(R.id.atlas_view_conversations_list_convert_avatar_multi_right);
                    String rightUserId = allButMe.get(1);
                    Contact right = contactProvider.contactsMap.get(rightUserId);
                    textInitialsRight.setText(AtlasContactProvider.getContactInitials(right));
                    
                    avatarSingle.setVisibility(View.GONE);
                    avatarMulti.setVisibility(View.VISIBLE);
                }
                
                TextView textLastMessage = (TextView) convertView.findViewById(R.id.atlas_conversation_view_last_message);
                TextView timeView = (TextView) convertView.findViewById(R.id.atlas_conversation_view_convert_time);
                if (conv.getLastMessage() != null ) {
                    Message last = conv.getLastMessage();
                    String lastMessageText = Atlas.Tools.toString(last);
                    
                    textLastMessage.setText(lastMessageText);
                    
                    Date sentAt = last.getSentAt();
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                    if (sentAt == null) timeView.setText("...");
                    else                timeView.setText(sdf.format(sentAt));

                    String userId = last.getSender().getUserId();                   // could be null for system messages 
                    String myId = layerClient.getAuthenticatedUserId();
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
                if (clickListener != null) clickListener.onItemClick(conv);
            }
        });
        conversationsList.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Conversation conv = conversations.get(position);
                if (longClickListener != null) longClickListener.onItemLongClick(conv);
                return true;
            }
        });
        
        // clean everything if deathenticated (client will explode on .getConversation())
        // and rebuilt everithing back after successful authentication  
        layerClient.registerAuthenticationListener(new LayerAuthenticationListener() {
            public void onDeauthenticated(LayerClient client) {
                if (debug) Log.w(TAG, "onDeauthenticated() ");
                updateValues();
            }
            public void onAuthenticated(LayerClient client, String userId) {
                updateValues();
            }
            public void onAuthenticationError(LayerClient client, LayerException exception) {}
            public void onAuthenticationChallenge(LayerClient client, String nonce) {}
        });
        
    }
    
    public void updateValues() {
        
        conversations.clear();                              // always clean, rebuild if authenticated 
        conversationsAdapter.notifyDataSetChanged();
        
        if (layerClient.isAuthenticated()) {
            
            List<Conversation> convs = layerClient.getConversations();
            if (debug) Log.d(TAG, "updateValues() conv: " + convs.size());
            for (Conversation conv : convs) {
                // no participants means we are removed from conversation (disconnected conversation)
                if (conv.getParticipants().size() == 0) continue;
                // only ourselves in participant list is possible to happen, but there is nothing to do with it
                // behave like conversation is disconnected
                if (conv.getParticipants().size() == 1 
                        && conv.getParticipants().contains(layerClient.getAuthenticatedUserId())) continue;
                
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
        }

    }

    @Override
    public void onEventMainThread(LayerChangeEvent event) {
        for (LayerChange change : event.getChanges()) {
            if (change.getObjectType() == LayerObject.Type.CONVERSATION
                    || change.getObjectType() == LayerObject.Type.MESSAGE) {
                updateValues();
                return;
            }
        }
    }
    
    public ConversationClickListener getClickListener() {
        return clickListener;
    }

    public void setClickListener(ConversationClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public ConversationLongClickListener getLongClickListener() {
        return longClickListener;
    }

    public void setLongClickListener(ConversationLongClickListener conversationLongClickListener) {
        this.longClickListener = conversationLongClickListener;
    }

    
    public interface ConversationClickListener {
        public void onItemClick(Conversation conversation);
    }
    
    public interface ConversationLongClickListener {
        public void onItemLongClick(Conversation conversation);
    }
}
