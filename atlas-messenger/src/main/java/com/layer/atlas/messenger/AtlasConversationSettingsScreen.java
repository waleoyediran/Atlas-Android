package com.layer.atlas.messenger;

import java.util.Arrays;
import java.util.HashSet;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

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
        
        View blockPersonView = findViewById(R.id.atlas_screen_conversation_settings_block_person);
        View leaveGroupView = findViewById(R.id.atlas_screen_conversation_settings_leave_group);
        
        HashSet<String> participants = new HashSet<String>(conv.getParticipants());
        participants.remove(app101.getLayerClient().getAuthenticatedUserId());
        if (participants.size() == 1) { // one-on-one
            blockPersonView.setVisibility(View.VISIBLE);
            leaveGroupView.setVisibility(View.GONE);
        } else {                        // multi
            blockPersonView.setVisibility(View.GONE);
            leaveGroupView.setVisibility(View.VISIBLE);
        }
        
        this.namesList = (ViewGroup) findViewById(R.id.atlas_screen_conversation_settings_participants_list);
        refreshNamesList();
    }
    
    private void refreshNamesList() {
        
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
            
            namesList.addView(convert);
        }
        
    }

}
