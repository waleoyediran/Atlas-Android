package com.layer.atlas.messenger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

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
    
    // participants picker
    private View participantsPicker;
    private EditText participantsFilter;
    private ListView participantsListView;
    private ViewGroup participantsNames;
    
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

        // START OF -------------------- Participant Picker ---------------------------------------- 
        participantsPicker = findViewById(R.id.atlas_screen_messages_participants_picker);
        participantsFilter = (EditText) findViewById(R.id.atlas_view_participants_picker_text);
        participantsListView = (ListView) findViewById(R.id.atlas_view_participants_picker_list);
        participantsNames = (ViewGroup) findViewById(R.id.atlas_view_participants_picker_names);
        if (convIsNew) {
            participantsPicker.setVisibility(View.VISIBLE);
            participantsFilter.requestFocus();
        }
        
        // log focuses
        final View scroller = findViewById(R.id.atlas_view_participants_picker_scroll);
        scroller.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (debug) Log.w(TAG, "scroller.onFocusChange() hasFocus: " + hasFocus);
            }
        });
        participantsNames.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (debug) Log.w(TAG, "names.onFocusChange()    hasFocus: " + hasFocus);
            }
        });
        
        // If filter.requestFocus is called from .onClickListener - filter receives focus, but
        // NamesLayout receives it immediately after that. So filter lose it.
        // XXX: scroller also receives focus 
        participantsNames.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (debug) Log.w(TAG, "names.onTouch() event: " + event);
                if (event.getAction() == MotionEvent.ACTION_DOWN)           // ACTION_UP never comes if  
                    participantsFilter.requestFocus();                      //   there is no .onClickListener
                return false;
            }
        });
        
        participantsFilter.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                View focused = participantsNames.hasFocus() ? participantsNames : participantsNames.findFocus();
                if (debug) Log.w(TAG, "filter.onFocusChange()   hasFocus: " + hasFocus + ", focused: " + focused);
                if (hasFocus) {
                    participantsListView.setVisibility(View.VISIBLE);
                }
                v.post(new Runnable() { // check focus runnable
                    @Override
                    public void run() {
                        if (debug) Log.w(TAG, "filter.onFocusChange.run()   filter.focus: " +  participantsFilter.hasFocus());
                        if (debug) Log.w(TAG, "filter.onFocusChange.run()    names.focus: " +  participantsNames.hasFocus());
                        if (debug) Log.w(TAG, "filter.onFocusChange.run() scroller.focus: " +  scroller.hasFocus());
                        
                        // check focus is on any descendants and hide list otherwise  
                        View focused = participantsNames.hasFocus() ? participantsNames : participantsNames.findFocus();
                        if (focused == null) {
                            participantsListView.setVisibility(View.GONE);
                            participantsFilter.setText("");
                        }
                    }
                });
            }
        });
        
        final Contact[] allContacts = app.contactsMap.values().toArray(new Contact[app.contactsMap.size()]);
        Arrays.sort(allContacts, Contact.FIRST_LAST_EMAIL_ASCENDING);
        final ArrayList<Contact> contacts = new ArrayList<App101.Contact>();
        contacts.addAll(Arrays.asList(allContacts));
         
        final ArrayList<Contact> selectedContacts = new ArrayList<App101.Contact>();
        final BaseAdapter contactsAdapter;
        participantsListView.setAdapter(contactsAdapter = new BaseAdapter() {
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.atlas_view_participants_picker_convert, parent, false);
                }
                
                TextView name = (TextView) convertView.findViewById(R.id.atlas_view_participants_picker_convert_name);
                TextView avatarText = (TextView) convertView.findViewById(R.id.atlas_view_participants_picker_convert_ava);
                Contact contact = contacts.get(position);
                
                name.setText(App101.getContactFirstAndLast(contact));
                avatarText.setText(App101.getContactInitials(contact));
                return convertView;
            }
            
            public long getItemId(int position) {
                return contacts.get(position).userId.hashCode();
            }
            public Object getItem(int position) {
                return contacts.get(position);
            }
            public int getCount() {
                return contacts.size();
            }
        });
        
        participantsListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Contact contact = contacts.get(position);
                selectedContacts.add(contact);
                refreshParticipants(app, selectedContacts);
                participantsFilter.setText("");
                participantsFilter.requestFocus();
            }

        });
        
        // track text and filter contact list
        participantsFilter.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (debug) Log.w(TAG, "beforeTextChanged() s: " + s + " start: " + start+ " count: " + count+ " after: " + after);
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debug) Log.w(TAG, "onTextChanged()     s: " + s + " start: " + start+ " before: " + before+ " count: " + count);
                
                final String filter = s.toString().toLowerCase();
                contacts.clear();
                for (Contact contact : allContacts) {
                    if (selectedContacts.contains(contacts)) continue; 
                    
                    if (contact.firstName != null && contact.firstName.toLowerCase().contains(filter)) {
                        contacts.add(contact); continue;
                    }
                    if (contact.lastName != null && contact.lastName.toLowerCase().contains(filter)) {
                        contacts.add(contact); continue;
                    }
                    if (contact.email != null && contact.email.toLowerCase().contains(filter)) {
                        contacts.add(contact); continue;
                    }
                }
                Collections.sort(contacts, new Contact.FilteringComparator(filter));
                contactsAdapter.notifyDataSetChanged();
            }
            public void afterTextChanged(Editable s) {
                if (debug) Log.w(TAG, "afterTextChanged()  s: " + s);
            }
        });
        
        // select last added participant when press "Backspace/Del"
        participantsFilter.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (debug) Log.w(TAG, "onKey() keyCode: " + keyCode + ", event: " + event);
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN
                        && participantsFilter.getText().length() == 0
                        && selectedContacts.size() > 0) {
                    
                    selectedContacts.remove(selectedContacts.size() - 1);
                    refreshParticipants(app, selectedContacts);
                    participantsFilter.requestFocus();
                }
                return false;
            }
        });
        // END OF ---------------------- Participant Picker ---------------------------------------- 
        
        messageText = (TextView) findViewById(R.id.atlas_messages_composer_text);
        
        btnSend = findViewById(R.id.atlas_messages_composer_send);
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
                
                int viewType = app.userId.equals(contact.userId) ? TYPE_ME : TYPE_OTHER;
                
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
    
    public void refreshParticipants(final App101 app, final ArrayList<Contact> selectedContacts) {
        
        // remove name_converts first. Better to keep editText in place rather than add/remove that force keyboard to blink
        for (int i = participantsNames.getChildCount() - 1; i >= 0; i--) {
            View child = participantsNames.getChildAt(i);
            if (child != participantsFilter) {
                participantsNames.removeView(child);
            }
        }
        if (debug) Log.w(TAG, "refreshParticipants() childs left: " + participantsNames.getChildCount());
        for (Contact contactToAdd : selectedContacts) {
            View contactView = LayoutInflater.from(participantsNames.getContext())
                    .inflate(R.layout.atlas_view_participants_picker_name_convert, participantsNames, false);
            
            TextView avaText = (TextView) contactView.findViewById(R.id.atlas_view_participants_picker_name_convert_ava);
            avaText.setText(app.getContactInitials(contactToAdd));
            TextView nameText = (TextView) contactView.findViewById(R.id.atlas_view_participants_picker_name_convert_name);
            nameText.setText(app.getContactFirstAndLast(contactToAdd));
            contactView.setTag(contactToAdd);
            
            participantsNames.addView(contactView, participantsNames.getChildCount() - 1);
            if (debug) Log.w(TAG, "refreshParticipants() child added: " + contactView + ", for: " + contactToAdd);
        }
        participantsNames.requestLayout();
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
            String initials = App101.getContactFirstAndL(app.contactsMap.get(userId));
            sb.append(initials != null ? initials : userId).append(", ");
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
