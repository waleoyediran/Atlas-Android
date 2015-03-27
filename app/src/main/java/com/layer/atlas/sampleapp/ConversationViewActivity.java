package com.layer.atlas.sampleapp;

import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.layer.atlas.adapter.MessageQueryAdapter;
import com.layer.atlas.sampleapp.activity.BaseActivity;
import com.layer.atlas.view.ConversationView;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.ConversationOptions;
import com.layer.sdk.messaging.Message;

import java.util.List;


public class ConversationViewActivity extends BaseActivity implements MessageQueryAdapter.Listener {
    Conversation mConversation;
    ConversationView mConversationView;

    public ConversationViewActivity() {
        super(Utils.APP_ID, Utils.GCM_SENDER_ID);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setLayerClient(getLayerClient());
        setContentView(R.layout.activity_conversation_view);
        mConversationView = (ConversationView) findViewById(R.id.conversation_view);

        // Which conversation?
        String conversationIdString = getIntent().getStringExtra(Utils.EXTRA_CONVERSATION_ID);
        String[] participants = getIntent().getStringArrayExtra(Utils.EXTRA_PARTICIPANTS);
        if (conversationIdString == null) {
            // New conversation
            ConversationOptions options = new ConversationOptions().deliveryReceipts(participants.length <= 5);
            mConversation = getLayerClient().newConversation(options, participants);
        } else {
            // Existing conversation
            Uri conversationId = Uri.parse(conversationIdString);
            mConversation = getLayerClient().getConversation(conversationId);
        }

        mConversationView.set(getLayerClient(), mConversation, null, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAuthenticatedResume() {
        mConversationView.refresh();
    }


    //==============================================================================================
    // Conversation listener
    //==============================================================================================

    @Override
    public void onMessageSent(MessageQueryAdapter adapter, Message message) {

    }

    @Override
    public void onMessageSelected(MessageQueryAdapter adapter, Message message) {

    }

    @Override
    public void onMessageDeleted(MessageQueryAdapter adapter, Message message, LayerClient.DeletionMode deletionMode) {

    }

    @Override
    public int onRequestMessageItemHeight(MessageQueryAdapter adapter, Message message) {
        return 0;
    }

    @Override
    public List<Message> onRequestMessagesForMediaAttachment(MessageQueryAdapter adapter) {
        return null;
    }
}
