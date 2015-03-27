package com.layer.atlas.sampleapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuItem;

import com.layer.atlas.adapter.ConversationQueryAdapter;
import com.layer.atlas.sampleapp.activity.BaseActivity;
import com.layer.atlas.view.ConversationRecyclerView;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;

import java.util.List;


public class ConversationListActivity extends BaseActivity implements ConversationQueryAdapter.Listener {
    ConversationRecyclerView mConversationRecyclerView;
    ConversationQueryAdapter mConversationQueryAdapter;

    public ConversationListActivity() {
        super(Utils.APP_ID, Utils.GCM_SENDER_ID);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setLayerClient(getLayerClient());
        setContentView(R.layout.activity_conversation_list);
        mConversationRecyclerView = (ConversationRecyclerView) findViewById(R.id.conversation_list);
        mConversationRecyclerView.setLayout(LinearLayoutManager.VERTICAL, false);

        mConversationQueryAdapter = new ConversationQueryAdapter(this, getLayerClient(), null, null, this);
        mConversationRecyclerView.setAdapter(mConversationQueryAdapter);
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
        if (mConversationQueryAdapter != null) {
            mConversationQueryAdapter.refresh();
        }
    }


    //==============================================================================================
    // Conversation listener
    //==============================================================================================

    @Override
    public void onConversationSelected(ConversationQueryAdapter adapter, Conversation conversation) {
        Intent intent = new Intent(this, ConversationViewActivity.class);
        intent.putExtra(Utils.EXTRA_CONVERSATION_ID, conversation.getId().toString());
        intent.putExtra(Utils.EXTRA_PARTICIPANTS, conversation.getParticipants().toArray(new String[conversation.getParticipants().size()]));
        startActivity(intent);
    }

    @Override
    public void onConversationDeleted(ConversationQueryAdapter adapter, Conversation conversation, LayerClient.DeletionMode deletionMode) {

    }

    @Override
    public List<String> onConversationTextSearch(ConversationQueryAdapter adapter, String searchText) {
        return null;
    }
}
