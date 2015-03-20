package com.layer.atlas.sampleapp;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.layer.atlas.adapter.ConversationAdapter;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;

import java.util.List;


public class MainActivity extends ActionBarActivity
        implements ConversationAdapter.Listener {
    LayerClient mClient;
    RecyclerView mRecyclerView;
    ConversationAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mClient = Utils.getLayerClient(this);
        mRecyclerView = (RecyclerView) findViewById(R.id.layer_conversations);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mAdapter = new ConversationAdapter(this, mClient, null, null, this);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mClient.isAuthenticated()) {
            Utils.authenticate(mClient, new Utils.Callback() {
                @Override
                public void onSuccess() {
                    authenticatedResume();
                }

                @Override
                public void onError() {

                }
            });
        } else {
            mClient.connect();
            authenticatedResume();
        }
    }

    private void authenticatedResume() {
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
    public void onConversationSelected(ConversationAdapter adapter, Conversation conversation) {

    }

    @Override
    public void onConversationDeleted(ConversationAdapter adapter, Conversation conversation, LayerClient.DeletionMode deletionMode) {

    }

    @Override
    public List<String> onConversationTextSearch(ConversationAdapter adapter, String searchText) {
        return null;
    }
}
