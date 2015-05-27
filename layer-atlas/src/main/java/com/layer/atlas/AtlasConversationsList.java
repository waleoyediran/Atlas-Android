package com.layer.atlas;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
    
    //styles
    private int titleTextColor;
    private int titleTextStyle;
    private Typeface titleTextTypeface;
    private int titleUnreadTextColor;
    private int titleUnreadTextStyle;
    private Typeface titleUnreadTextTypeface;
    private int subtitleTextColor;
    private int subtitleTextStyle;
    private Typeface subtitleTextTypeface;
    private int subtitleUnreadTextColor;
    private int subtitleUnreadTextStyle;
    private Typeface subtitleUnreadTextTypeface;
    private int cellBackgroundColor;
    private int cellUnreadBackgroundColor;
    private int dateTextColor;
    private int avatarTextColor;
    private int avatarBackgroundColor;
    
    public AtlasConversationsList(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseStyle(context, attrs, defStyle);
    }

    public AtlasConversationsList(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AtlasConversationsList(Context context) {
        super(context);
    }

    public void init(View rootView, final LayerClient layerClient, final ContactProvider contactProvider) {
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
                
                TextView textTitle = (TextView) convertView.findViewById(R.id.atlas_conversation_view_convert_participant);
                String conversationTitle = (String) conv.getMetadata().get(Atlas.METADATA_KEY_CONVERSATION_TITLE);
                if (conversationTitle != null && conversationTitle.trim().length() > 0) {
                    textTitle.setText(conversationTitle.trim());
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (String userId : conv.getParticipants()) {
                        if (layerClient.getAuthenticatedUserId().equals(userId)) {
                            continue;
                        }
                        Contact contact = contactProvider.get(userId);
                        if (contact == null) continue;
                        String name = allButMe.size() > 1 ? contact.getFirstAndL() : contact.getFirstAndLast();
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(name != null ? name : userId);
                    }
                    
                    textTitle.setText(sb);
                }
                
                // avatar icons... 
                TextView textInitials = (TextView) convertView.findViewById(R.id.atlas_view_conversations_list_convert_avatar_single_text);
                View avatarSingle = convertView.findViewById(R.id.atlas_view_conversations_list_convert_avatar_single);
                View avatarMulti = convertView.findViewById(R.id.atlas_view_conversations_list_convert_avatar_multi);
                if (allButMe.size() < 2) {
                    String conterpartyUserId = allButMe.get(0);
                    Contact counterParty = contactProvider.get(conterpartyUserId);
                    if (counterParty != null) textInitials.setText(counterParty.getInitials());
                    textInitials.setTextColor(avatarTextColor);
                    ((GradientDrawable) textInitials.getBackground()).setColor(avatarBackgroundColor);
                    avatarSingle.setVisibility(View.VISIBLE);
                    avatarMulti.setVisibility(View.GONE);
                } else {
                    TextView textInitialsLeft = (TextView) convertView.findViewById(R.id.atlas_view_conversations_list_convert_avatar_multi_left);
                    String leftUserId = allButMe.get(0);
                    Contact left = contactProvider.get(leftUserId);
                    if (left != null) textInitialsLeft.setText(left.getInitials());
                    textInitialsLeft.setTextColor(avatarTextColor);
                    ((GradientDrawable) textInitialsLeft.getBackground()).setColor(avatarBackgroundColor);
                    
                    TextView textInitialsRight = (TextView) convertView.findViewById(R.id.atlas_view_conversations_list_convert_avatar_multi_right);
                    String rightUserId = allButMe.get(1);
                    Contact right = contactProvider.get(rightUserId);
                    if (right != null) textInitialsRight.setText(right.getInitials());
                    textInitialsRight.setTextColor(avatarTextColor);
                    ((GradientDrawable) textInitialsRight.getBackground()).setColor(avatarBackgroundColor);
                    
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
                        textTitle.setTextColor(titleUnreadTextColor);
                        textTitle.setTypeface(titleUnreadTextTypeface, titleUnreadTextStyle);
                        textLastMessage.setTypeface(subtitleUnreadTextTypeface, subtitleUnreadTextStyle);
                        textLastMessage.setTextColor(subtitleUnreadTextColor);
                        convertView.setBackgroundColor(cellUnreadBackgroundColor);
                    } else {
                        textTitle.setTextColor(titleTextColor);
                        textTitle.setTypeface(titleTextTypeface, titleTextStyle);
                        textLastMessage.setTypeface(subtitleTextTypeface, subtitleTextStyle);
                        textLastMessage.setTextColor(subtitleTextColor);
                        convertView.setBackgroundColor(cellBackgroundColor);
                    }
                } else {
                    textLastMessage.setText("");
                    textTitle.setTextColor(titleTextColor);
                    textTitle.setTypeface(titleTextTypeface, titleTextStyle);
                    textLastMessage.setTypeface(subtitleTextTypeface, subtitleTextStyle);
                    textLastMessage.setTextColor(subtitleTextColor);
                    convertView.setBackgroundColor(cellBackgroundColor);
                }
                timeView.setTextColor(dateTextColor);
                
//                private int dateTextColor;
//                private int avatarTextColor;
//                private int avatarBackgroundColor;
                
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
        
        applyStyle();
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

    public void parseStyle(Context context, AttributeSet attrs, int defStyle) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AtlasConversationList, R.attr.AtlasConversationList, defStyle);
        this.titleTextColor = ta.getColor(R.styleable.AtlasConversationList_titleTextColor, context.getResources().getColor(R.color.atlas_text_black));
        this.titleTextStyle = ta.getInt(R.styleable.AtlasConversationList_titleTextStyle, Typeface.NORMAL);
        String titleTextTypefaceName = ta.getString(R.styleable.AtlasConversationList_titleTextTypeface); 
        this.titleTextTypeface  = titleTextTypefaceName != null ? Typeface.create(titleTextTypefaceName, titleTextStyle) : null;
        
        this.titleUnreadTextColor = ta.getColor(R.styleable.AtlasConversationList_titleUnreadTextColor, context.getResources().getColor(R.color.atlas_text_black));
        this.titleUnreadTextStyle = ta.getInt(R.styleable.AtlasConversationList_titleUnreadTextStyle, Typeface.BOLD);
        String titleUnreadTextTypefaceName = ta.getString(R.styleable.AtlasConversationList_titleUnreadTextTypeface); 
        this.titleUnreadTextTypeface  = titleUnreadTextTypefaceName != null ? Typeface.create(titleUnreadTextTypefaceName, titleUnreadTextStyle) : null;
        
        this.subtitleTextColor = ta.getColor(R.styleable.AtlasConversationList_subtitleTextColor, context.getResources().getColor(R.color.atlas_text_black));
        this.subtitleTextStyle = ta.getInt(R.styleable.AtlasConversationList_subtitleTextStyle, Typeface.NORMAL);
        String subtitleTextTypefaceName = ta.getString(R.styleable.AtlasConversationList_subtitleTextTypeface); 
        this.subtitleTextTypeface  = subtitleTextTypefaceName != null ? Typeface.create(subtitleTextTypefaceName, subtitleTextStyle) : null;
        
        this.subtitleUnreadTextColor = ta.getColor(R.styleable.AtlasConversationList_subtitleUnreadTextColor, context.getResources().getColor(R.color.atlas_text_black));
        this.subtitleUnreadTextStyle = ta.getInt(R.styleable.AtlasConversationList_subtitleUnreadTextStyle, Typeface.NORMAL);
        String subtitleUnreadTextTypefaceName = ta.getString(R.styleable.AtlasConversationList_subtitleUnreadTextTypeface); 
        this.subtitleUnreadTextTypeface  = subtitleUnreadTextTypefaceName != null ? Typeface.create(subtitleUnreadTextTypefaceName, subtitleUnreadTextStyle) : null;
        
        this.cellBackgroundColor = ta.getColor(R.styleable.AtlasConversationList_cellBackgroundColor, Color.TRANSPARENT); 
        this.cellUnreadBackgroundColor = ta.getColor(R.styleable.AtlasConversationList_cellUnreadBackgroundColor, Color.TRANSPARENT); 
        this.dateTextColor = ta.getColor(R.styleable.AtlasConversationList_dateTextColor, context.getResources().getColor(R.color.atlas_text_black)); 
        this.avatarTextColor = ta.getColor(R.styleable.AtlasConversationList_avatarTextColor, context.getResources().getColor(R.color.atlas_text_black)); 
        this.avatarBackgroundColor = ta.getColor(R.styleable.AtlasConversationList_avatarBackgroundColor, context.getResources().getColor(R.color.atlas_shape_avatar_gray));
        ta.recycle();
    }
    
    private void applyStyle() {
        conversationsAdapter.notifyDataSetChanged();
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
