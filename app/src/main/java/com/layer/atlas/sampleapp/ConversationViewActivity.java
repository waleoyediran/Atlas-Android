package com.layer.atlas.sampleapp;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.layer.atlas.queryadapter.MessageAdapter;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.ConversationOptions;
import com.layer.sdk.messaging.Message;

import java.util.List;


public class ConversationViewActivity extends BaseActivity implements MessageAdapter.Listener {
    RecyclerView mRecyclerView;
    MessageAdapter mAdapter;
    Conversation mConversation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_view);

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

        mRecyclerView = (RecyclerView) findViewById(R.id.layer_messages);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mAdapter = new MessageAdapter(this, getLayerClient(), mConversation, null, null, this);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    void authenticatedResume() {
        mAdapter.refresh();
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


    //==============================================================================================
    // Conversation listener
    //==============================================================================================

    @Override
    public void onMessageSent(MessageAdapter adapter, Message message) {

    }

    @Override
    public void onMessageSelected(MessageAdapter adapter, Message message) {

    }

    @Override
    public void onMessageDeleted(MessageAdapter adapter, Message message, LayerClient.DeletionMode deletionMode) {

    }

    @Override
    public int onRequestMessageItemHeight(MessageAdapter adapter, Message message) {
        return 0;
    }

    @Override
    public List<Message> onRequestMessagesForMediaAttachment(MessageAdapter adapter) {
        return null;
    }
}
