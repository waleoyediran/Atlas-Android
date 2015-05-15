package com.layer.atlas.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.atlas.Atlas.AtlasContactProvider;
import com.layer.atlas.AtlasConversationsList;
import com.layer.atlas.AtlasConversationsList.ConversationClickListener;
import com.layer.atlas.AtlasConversationsList.ConversationLongClickListener;
import com.layer.sdk.LayerClient;
import com.layer.sdk.LayerClient.DeletionMode;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.messaging.Conversation;

/**
 * @author Oleg Orlov
 * @since 14 Apr 2015
 */
public class AtlasConversationsScreen extends Activity {
    private static final String TAG = AtlasConversationsScreen.class.getSimpleName();
    private static final boolean debug = true;

    private static final int REQUEST_CODE_LOGIN = 999;
    
    private View btnNewConversation;
    
    private App101 app;
    private AtlasContactProvider contactProvider;
    private AtlasConversationsList conversationsList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atlas_screen_conversations);
        
        this.app = (App101) getApplication();
        this.contactProvider = app.contactProvider;
        
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
        
        View conversationsRoot = findViewById(R.id.atlas_screen_conversations_conversations_list);
        this.conversationsList = new AtlasConversationsList(conversationsRoot, app.getLayerClient(), app.contactProvider);
        conversationsList.setClickListener(new ConversationClickListener() {
            public void onItemClick(Conversation conversation) {
                openChatScreen(conversation, false);
            }
        });
        conversationsList.setLongClickListener(new ConversationLongClickListener() {
            public void onItemLongClick(Conversation conversation) {
                conversation.delete(DeletionMode.ALL_PARTICIPANTS);
                updateValues();
                Toast.makeText(AtlasConversationsScreen.this, "Deleted: " + conversation, Toast.LENGTH_SHORT).show();;
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
        conversationsList.updateValues();
        
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
