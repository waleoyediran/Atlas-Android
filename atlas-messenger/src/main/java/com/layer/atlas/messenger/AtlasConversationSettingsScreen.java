package com.layer.atlas.messenger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.atlas.messenger.App101.Contact;
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
    
    private App101 app101;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atlas_screen_conversation_settings);
        
        this.app101 = (App101) getApplication();
        
        ImageView menuBtn = (ImageView) findViewById(R.id.atlas_actionbar_left_btn);
        menuBtn.setImageResource(R.drawable.atlas_ctl_btn_back);
        menuBtn.setVisibility(View.VISIBLE);
        menuBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        
        ((TextView)findViewById(R.id.atlas_actionbar_title_text)).setText("Details");
        
        CheckBox notificationsCheck = (CheckBox) findViewById(R.id.atlas_screen_conversation_settings_notifications_switch);
        notificationsCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (debug) Log.w(TAG, "onCheckedChanged() ");
            }
        });
        
        View btnBlockPerson = findViewById(R.id.atlas_screen_conversation_settings_block_person);
        View btnLeaveGroup = findViewById(R.id.atlas_screen_conversation_settings_leave_group);
        EditText textGroupName = (EditText) findViewById(R.id.atlas_screen_conversation_settings_groupname_text);
        
        HashSet<String> participants = new HashSet<String>(conv.getParticipants());
        participants.remove(app101.getLayerClient().getAuthenticatedUserId());
        if (participants.size() == 1) { // one-on-one
            btnBlockPerson.setVisibility(View.VISIBLE);
            btnLeaveGroup.setVisibility(View.GONE);
            textGroupName.setVisibility(View.GONE);
        } else {                        // multi
            btnBlockPerson.setVisibility(View.GONE);
            btnLeaveGroup.setVisibility(View.VISIBLE);
            textGroupName.setVisibility(View.VISIBLE);
        }
        
        ImageView galleryView = (ImageView) findViewById(R.id.atlas_screen_conversation_settings_gallery);
        Bitmap bmp;
        try {
            bmp = BitmapFactory.decodeStream(getAssets().open("gallery.png"));
            galleryView.setImageBitmap(bmp);
            galleryView.getLayoutParams().height = getWindowManager().getDefaultDisplay().getWidth();
            galleryView.setLayoutParams(galleryView.getLayoutParams());
        } catch (IOException e) {
            Log.e(TAG, "onCreate() Cannot show gallery", e);
        }
        
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
        updateValues();
    }
    
    private void updateValues() {
        
        // refresh names screen
        namesList.removeAllViews();
        
        Contact[] contacts = new Contact[conv.getParticipants().size()];
        int i = 0;
        for (String userId : conv.getParticipants()) {
            Contact c = app101.contactsMap.get(userId);
            contacts[i++] = c;
        }
        Arrays.sort(contacts, Contact.FIRST_LAST_EMAIL_ASCENDING);
        
        for (int iContact = 0; iContact < contacts.length; iContact++) {
            View convert = getLayoutInflater().inflate(R.layout.atlas_screen_conversation_settings_participant_convert, namesList, false);
            
            TextView avaText = (TextView) convert.findViewById(R.id.atlas_screen_conversation_settings_convert_ava);
            avaText.setText(App101.getContactInitials(contacts[iContact]));
            TextView nameText = (TextView) convert.findViewById(R.id.atlas_screen_conversation_settings_convert_name);
            nameText.setText(App101.getContactFirstAndLast(contacts[iContact]));
            
            convert.setTag(contacts[iContact]);
            convert.setOnLongClickListener(contactLongClickListener);
            
            namesList.addView(convert);
        }
        
    }
    
    private OnLongClickListener contactLongClickListener = new OnLongClickListener() {
        public boolean onLongClick(View v) {
            Contact contact = (Contact) v.getTag();
            conv.removeParticipants(contact.userId);
            Toast.makeText(v.getContext(), "Removing " + App101.getContactFirstAndLast(contact), Toast.LENGTH_LONG).show();
            updateValues();
            return true;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (debug) Log.w(TAG, "onActivityResult() requestCode: " + requestCode + ", resultCode: " + requestCode 
                + ", data: " + (data != null ? Log.toString(data.getExtras()) : "null"));
        if (requestCode == REQUEST_CODE_ADD_PARTICIPANT && resultCode == RESULT_OK) {
            String[] addedParticipants = data.getStringArrayExtra(AtlasParticipantPickersScreen.EXTRA_KEY_USERIDS_SELECTED);
            conv.addParticipants(addedParticipants);
            updateValues();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (debug) Log.w(TAG, "onResume() ");
        updateValues();
    }
    
}
