package com.layer.atlas.messenger;

import java.util.Arrays;
import java.util.HashSet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.atlas.Atlas;
import com.layer.atlas.Contact;
import com.layer.sdk.internal.utils.Log;
import com.layer.sdk.messaging.Conversation;

/**
 * @author Oleg Orlov
 * @since 23 Apr 2015
 */
public class AtlasConversationSettingsScreen extends Activity {
    private static final String TAG = AtlasConversationSettingsScreen.class.getSimpleName();
    private static final boolean debug = true;

    private static final int REQUEST_CODE_ADD_PARTICIPANT = 999;
    
    public static Conversation conv;
    private ViewGroup namesList;
    
    private View btnLeaveGroup;
    private EditText textGroupName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atlas_screen_conversation_settings);
        
        btnLeaveGroup = findViewById(R.id.atlas_screen_conversation_settings_leave_group);
        textGroupName = (EditText) findViewById(R.id.atlas_screen_conversation_settings_groupname_text);
        
        View btnAddParticipant = findViewById(R.id.atlas_screen_conversation_settings_add_participant);
        btnAddParticipant.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(AtlasConversationSettingsScreen.this, AtlasParticipantPickersScreen.class);
                final String[] skipUserIds = conv.getParticipants().toArray(new String[0]);
                intent.putExtra(AtlasParticipantPickersScreen.EXTRA_KEY_USERIDS_SKIP, skipUserIds);
                startActivityForResult(intent, REQUEST_CODE_ADD_PARTICIPANT);
            }
        });
        
        this.namesList = (ViewGroup) findViewById(R.id.atlas_screen_conversation_settings_participants_list);
        
        prepareActionBar();
    }

    private void updateValues() {
        
        App101 app101 = (App101) getApplication();
        
        String conversationTitle = (String) conv.getMetadata().get(Atlas.METADATA_KEY_CONVERSATION_TITLE);
        if (conversationTitle != null && conversationTitle.trim().length() > 0) {
            textGroupName.setText(conversationTitle.trim());
        } else {
            textGroupName.setText("");
        }
        
        // refresh names screen
        namesList.removeAllViews();
        
        HashSet<String> participants = new HashSet<String>(conv.getParticipants());
        participants.remove(app101.getLayerClient().getAuthenticatedUserId());
        Contact[] contacts = new Contact[participants.size()];
        int i = 0;
        for (String userId : participants) {
            Contact c = app101.getContactProvider().get(userId);
            contacts[i++] = c;
        }
        Arrays.sort(contacts, Contact.FIRST_LAST_EMAIL_ASCENDING);
        
        for (int iContact = 0; iContact < contacts.length; iContact++) {
            View convert = getLayoutInflater().inflate(R.layout.atlas_screen_conversation_settings_participant_convert, namesList, false);
            
            TextView avaText = (TextView) convert.findViewById(R.id.atlas_screen_conversation_settings_convert_ava);
            avaText.setText(contacts[iContact].getInitials());
            TextView nameText = (TextView) convert.findViewById(R.id.atlas_screen_conversation_settings_convert_name);
            nameText.setText(contacts[iContact].getFirstAndLast());
            
            convert.setTag(contacts[iContact]);
            convert.setOnLongClickListener(contactLongClickListener);
            
            namesList.addView(convert);
        }
        
        if (participants.size() == 1) { // one-on-one
            btnLeaveGroup.setVisibility(View.GONE);
        } else {                        // multi
            btnLeaveGroup.setVisibility(View.VISIBLE);
        }

    }
    
    private OnLongClickListener contactLongClickListener = new OnLongClickListener() {
        public boolean onLongClick(View v) {
            Contact contact = (Contact) v.getTag();
            conv.removeParticipants(contact.userId);
            Toast.makeText(v.getContext(), "Removing " + contact.getFirstAndLast(), Toast.LENGTH_LONG).show();
            updateValues();
            return true;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ADD_PARTICIPANT && resultCode == RESULT_OK) {
            String[] addedParticipants = data.getStringArrayExtra(AtlasParticipantPickersScreen.EXTRA_KEY_USERIDS_SELECTED);
            conv.addParticipants(addedParticipants);
            updateValues();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (debug) Log.w(TAG, "onResume() conv.metadata: " + Log.toString(conv.getMetadata()));
        updateValues();
    }
    
    protected void onPause() {
        super.onPause();
        
        String title = textGroupName.getText().toString().trim();
        conv.putMetadataAtKeyPath(Atlas.METADATA_KEY_CONVERSATION_TITLE, title);
    }
    
    private void prepareActionBar() {
        ImageView menuBtn = (ImageView) findViewById(R.id.atlas_actionbar_left_btn);
        menuBtn.setImageResource(R.drawable.atlas_ctl_btn_back);
        menuBtn.setVisibility(View.VISIBLE);
        menuBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        
        ((TextView)findViewById(R.id.atlas_actionbar_title_text)).setText("Details");
    }

}
