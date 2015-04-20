package com.layer.atlas.messenger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.atlas.messenger.App101.Contact;
import com.layer.sdk.LayerClient;
import com.layer.sdk.LayerClient.DeletionMode;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.internal.utils.Log;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;

public class MainScreen extends Activity {
    private static final String TAG = MainScreen.class.getSimpleName();
    private static final boolean debug = true;

    private TextView filterText;
    private TextView userIdText;
    private ListView conversationsList;
    private BaseAdapter conversationsAdapter;
    private View chatBtn;
    
    private ArrayList<Conversation> conversations = new ArrayList<Conversation>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_screen);
        
        final App101 app = (App101) getApplication();
        LayerClient client = app.getLayerClient();
        if (debug) Log.i(TAG, "onCreate() layerClient: " + client);
        
        userIdText = (TextView) findViewById(R.id.main_screen_login);
        
        conversationsList = (ListView) findViewById(R.id.main_screen_list_conversations);
        conversationsList.setAdapter(conversationsAdapter = new BaseAdapter() {
            private int nextId = 0;
            private final HashMap id2converts = new HashMap();
            
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) { 
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.main_screen_list_convert, parent, false);
                    convertView.setTag(nextId++);
                    id2converts.put(convertView.getTag(), convertView);
                }
                
                final Uri convId = conversations.get(position).getId();
                if (false) Log.d(TAG, "getView() " + position + ", conv:" + convId + ": convert: " + convertView + ", total: " + id2converts.size());
                
                Conversation conv = app.getLayerClient().getConversation(convId);
                StringBuilder sb = new StringBuilder();
                for (String userId : conv.getParticipants()) {
                    String initials = App101.getContactFirstAndL(app.contactsMap.get(userId));
                    sb.append(initials != null ? initials : userId).append(", ");
                }
                
                TextView text = (TextView) convertView.findViewById(R.id.main_screen_list_conversations_convert_text);
                text.setText(sb);
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
                openChatScreen(MainScreen.this, conv);
            }
        });
        conversationsList.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Conversation conv = conversations.get(position);
                conv.delete(DeletionMode.ALL_PARTICIPANTS);
                updateValues();
                Toast.makeText(view.getContext(), "Deleted: " + conv, Toast.LENGTH_SHORT).show();;
                return true;
            }
        });
        
        filterText = (TextView) findViewById(R.id.main_screen_text_filter);
        filterText.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                updateValues();
            }
        });
        
        chatBtn = findViewById(R.id.main_screen_new_chat);
        chatBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                App101 app = (App101) getApplication();
                String[] codeNames = new String[] {"bear", "rabbit", "fish", "cat"};
//                Contact lady = app.contactsMap.get("hhlady@mailforspam.com");
//                Contact mom = app.contactsMap.get("hhmom@mailforspam.com");
//                Contact dad = app.contactsMap.get("hhdad@mailforspam.com");
                Contact lady = app.contactsMap.get("ulady@mailforspam.com");
                //Contact mom = app.contactsMap.get("umom@mailforspam.com");
                //Contact dad = app.contactsMap.get("udad@mailforspam.com");
                Conversation conv = app.getLayerClient().newConversation(
                        /*dad.userId,*/ lady.userId /*, mom.userId*/
                );
                long now = System.currentTimeMillis();
                int second = (int) (now / 1000) % 60;
                String codeName = codeNames[second % codeNames.length];
                final Message message = App101.message("Conversation opened: [" + codeName + "-" + second + "]"
                        + "\n at: " + new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss").format(new Date(now)), app.getLayerClient());
                conv.send(message);
                openChatScreen(MainScreen.this, conv);
            }
        });
        
    }
    
    private void updateValues() {
        App101 app = (App101) getApplication();
        if (app.getLayerClient().isAuthenticated()) {
            Contact contact = app.contactsMap.get(app.userId);
            userIdText.setText(contact != null ? app.getContactFirstAndL(contact) : app.userId);
            
            final List<Conversation> convs = app.getLayerClient().getConversations();
            if (debug) Log.d(TAG, "updateValues() conv: " + convs.size());
            conversations.clear();
            conversations.addAll(convs);
            Collections.sort(conversations, new Comparator<Conversation>() {
                public int compare(Conversation lhs, Conversation rhs) {
                    long leftRecievedAt = 0;
                    if (lhs != null && lhs.getLastMessage() != null) {
                        leftRecievedAt = lhs.getLastMessage().getReceivedAt().getTime();
                    } else {
                        Log.e(TAG, "updateValues() lhs.lastMessage: " + lhs.getLastMessage());
                    }
                    long rightReceivedAt = 0;
                    if (rhs != null && rhs.getLastMessage() != null) {
                        rightReceivedAt = rhs.getLastMessage().getReceivedAt().getTime();
                    } else {
                        Log.e(TAG, "updateValues() rhs.lastMessage: " + rhs.getLastMessage());
                    }
                    
                    return (int) (rightReceivedAt - leftRecievedAt);
                }
            });
            conversationsAdapter.notifyDataSetChanged();
        }
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

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.main_screen_menu_dump : {
                final App101 app = (App101) getApplication();
                new Thread(new Runnable() {
                    public void run() {
                        app.dumpDb();
                    }
                }).start();;
                
                return true;
            } 
            case R.id.action_settings : return true; 
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void initPushes() {
        Context context = this;
        LayerClient lc = LayerClient.newInstance(context, "");
        
        // Build the notification
//        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
//                .setSmallIcon(R.drawable.ic_launcher)
//                .setContentTitle(context.getResources().getString(R.string.app_name))
//                .setContentText("Some message")
//                .setAutoCancel(true)
//                .setLights(context.getResources().getColor(android.R.color.holo_orange_dark), 100, 1900)
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE);

        // Set the action to take when a user taps the notification
        Intent resultIntent = new Intent(context, MainScreen.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        resultIntent.putExtra("layer-conversation-id", 123);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);
//        mBuilder.setContentIntent(resultPendingIntent);
//
//        // Show the notification
//        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        mNotifyMgr.notify(1, mBuilder.build());
    }

    public void openChatScreen(Context context, Conversation conv) {
        Intent intent = new Intent(context, ChatScreen.class);
        intent.putExtra(App101.keys.CONVERSATION_URI, conv.getId().toString());
        startActivity(intent);
    }
}
