package com.layer.atlas.messenger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.atlas.Atlas;
import com.layer.atlas.Atlas.AtlasContactProvider;
import com.layer.atlas.Atlas.Contact;
import com.layer.atlas.AtlasMessageComposer;
import com.layer.atlas.AtlasMessagesList;
import com.layer.atlas.AtlasMessagesList.CellDataItem;
import com.layer.atlas.AtlasMessagesList.ItemClickListener;
import com.layer.atlas.AtlasParticipantPicker;
import com.layer.atlas.messenger.App101.keys;
import com.layer.sdk.LayerClient;
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
    public static final int REQUEST_CODE_GALLERY  = 111;
    public static final int REQUEST_CODE_CAMERA   = 112;
        
    private Conversation conv;
    
    private LocationManager locationManager;
    private Location lastKnownLocation;
    private Handler uiHandler;
    
    private AtlasMessagesList messagesList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.uiHandler = new Handler();
        setContentView(R.layout.atlas_screen_messages);
        final App101 app = (App101) getApplication();

        boolean convIsNew = getIntent().getBooleanExtra(EXTRA_CONVERSATION_IS_NEW, false);
        String convUri = getIntent().getStringExtra(EXTRA_CONVERSATION_URI);
        if (convUri != null) {
            Uri uri = Uri.parse(convUri);
            conv = app.getLayerClient().getConversation(uri);
        }

        final View participantsPickerRoot = findViewById(R.id.atlas_screen_messages_participants_picker);
        final AtlasParticipantPicker pp = new AtlasParticipantPicker(participantsPickerRoot, app.contactProvider, null);
        if (convIsNew) {
            pp.setVisibility(View.VISIBLE);
        }
        
        final AtlasMessageComposer messageComposer = new AtlasMessageComposer(findViewById(R.id.atlas_screen_messages_message_composer), app.getLayerClient());
        messageComposer.setConversation(conv);
        messageComposer.setListener(new AtlasMessageComposer.Listener() {
            public boolean beforeSend(Message message) {
                if (conv == null) { // create new one
                    String[] userIds = pp.getSelectedUserIds();
                    conv = app.getLayerClient().newConversation(userIds);
                    participantsPickerRoot.setVisibility(View.GONE);
                    messageComposer.setConversation(conv);
                    messagesList.setConversation(conv);
                }
                
                // push
                Contact myContact = app.contactProvider.contactsMap.get(app.getLayerClient().getAuthenticatedUserId());
                String senderName = AtlasContactProvider.getContactFirstAndLast(myContact);
                Map<String, String> metadata = new HashMap<String, String>();
                boolean bug = true;
                String text = bug ? "LayerPush! Check message!"  : Atlas.Tools.toString(message);
                if (senderName != null && !senderName.isEmpty()) {
                    metadata.put(Message.ReservedMetadataKeys.PushNotificationAlertMessageKey.getKey(), senderName + ": " + text);
                } else {
                    metadata.put(Message.ReservedMetadataKeys.PushNotificationAlertMessageKey.getKey(), text);
                }
                message.setMetadata(metadata);
                
                return true;
            }
        });
        
        messageComposer.registerMenuItem("Photo", new OnClickListener() {
            public void onClick(View v) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE); 
                startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA); 
            }
        });

        messageComposer.registerMenuItem("Image", new OnClickListener() {
            public void onClick(View v) {
                // in onCreate or any event where your want the user to select a file
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CODE_GALLERY);
            }
        });
        
        messageComposer.registerMenuItem("Location", new OnClickListener() {
            public void onClick(View v) {
                if (conv == null) {
                    Toast.makeText(v.getContext(), "Inserting Location: Conversation is not created yet", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (lastKnownLocation == null) {
                    Toast.makeText(v.getContext(), "Inserting Location: Location is unknown yet", Toast.LENGTH_SHORT).show();
                    return;
                }
                String locationString = "{\"lat\"=" + lastKnownLocation.getLatitude() + "; \"lon\"=" + lastKnownLocation.getLongitude() + "}";
                MessagePart part = app.getLayerClient().newMessagePart(Atlas.MIME_TYPE_ATLAS_LOCATION, locationString.getBytes());
                Message message = app.getLayerClient().newMessage(Arrays.asList(part));
                conv.send(message);

                if (debug) Log.w(TAG, "onSendLocation() loc:  " + locationString);
            }
        });
        
        messagesList = new AtlasMessagesList(findViewById(R.id.atlas_screen_messages_messages_list), app.getLayerClient(), app.contactProvider);
        messagesList.setConversation(conv);
        messagesList.setItemClickListener(new ItemClickListener() {
            public void onItemClick(CellDataItem item) {
                if (Atlas.MIME_TYPE_ATLAS_LOCATION.equals(item.messagePart.getMimeType())) {
                    String jsonLonLat = new String(item.messagePart.getData());
                    JSONObject json;
                    try {
                        json = new JSONObject(jsonLonLat);
                        double lon = json.getDouble("lon");
                        double lat = json.getDouble("lat");
                        Intent openMapIntent = new Intent(Intent.ACTION_VIEW);
                        String uriString = String.format(Locale.ENGLISH, "geo:%f,%f?z=%d&q=%f,%f", lat, lon, 18, lat, lon);
                        final Uri geoUri = Uri.parse(uriString);
                        openMapIntent.setData(geoUri);
                        if (openMapIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(openMapIntent);
                            if (debug) Log.w(TAG, "onItemClick() starting Map: " + uriString);
                        } else {
                            if (debug) Log.w(TAG, "onItemClick() No Activity to start Map: " + geoUri);
                        }
                    } catch (JSONException ignored) {}
                }
            }
        });
        
        // location manager for inserting locations:
        this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        prepareActionBar();
    }
    
    private void updateValues() {
        App101 app = (App101) getApplication();
        
        if (conv == null) {
            Log.e(TAG, "updateValues() no conversation set");
            return;
        }
        
        messagesList.updateValues();
        
        // update buddies:
        StringBuilder sb = new StringBuilder();
        for (String userId : conv.getParticipants()) {
            if (app.getLayerClient().getAuthenticatedUserId().equals(userId)) continue;
            Contact contact = app.contactProvider.contactsMap.get(userId);
            String initials = conv.getParticipants().size() > 2 ? AtlasContactProvider.getContactFirstAndL(contact) : AtlasContactProvider.getContactFirstAndLast(contact);
            if (sb.length() > 0) sb.append(", ");
            sb.append(initials != null ? initials : userId);
        }
        TextView titleText = (TextView) findViewById(R.id.atlas_actionbar_title_text);
        titleText.setText(sb);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (debug) Log.w(TAG, "onActivityResult() requestCode: " + requestCode
                    + ", resultCode: " + resultCode
                    + ", uri: "  + (data == null ? "" : data.getData())
                    + ", data: " + (data == null ? "" : App101.toString(data.getExtras())) );
        
        if (resultCode != Activity.RESULT_OK) return;
        
        switch (requestCode) {
            case REQUEST_CODE_CAMERA  :
                Bitmap bmp = (Bitmap) data.getExtras().get("data"); 
                if (debug) Log.w(TAG, "onActivityResult() camera bitmap: " + bmp.getWidth() + "x" + bmp.getHeight() + ", " + bmp.getByteCount() + " bytes ");
                
                break;
            case REQUEST_CODE_GALLERY :
                // first check media gallery
                if (data == null) {
                    if (debug) Log.w(TAG, "onActivityResult() no data... :( ");
                    return;
                }
                Uri selectedImageUri = data.getData();
                // TODO: Mi4 requires READ_EXTERNAL_STORAGE permission for such operation
                String selectedImagePath = getGalleryImagePath(selectedImageUri);
                String resultFileName = selectedImagePath;
                if (selectedImagePath != null) {
                    if (debug) Log.w(TAG, "onActivityResult() image from gallery selected: " + selectedImagePath);
                } else if (selectedImageUri.getPath() != null) { 
                    if (debug) Log.w(TAG, "onActivityResult() image from file picker appears... "  + selectedImageUri.getPath());
                    resultFileName = selectedImageUri.getPath();
                }
                
                if (resultFileName != null) {
                    // create message and upload content
                    File fileToUpload = new File(resultFileName);
                    if (!fileToUpload.exists()) {
                        if (debug) Log.w(TAG, "onActivityResult() file to upload doesn't exist, path: " + resultFileName);
                        return;
                    }
                    
                    String mimeType = Atlas.MIME_TYPE_IMAGE_JPEG;
                    if (resultFileName.endsWith(".png")) mimeType = Atlas.MIME_TYPE_IMAGE_PNG;
                    
                    // test file copy locally
                    try {
                        LayerClient layerClient = ((App101) getApplication()).getLayerClient();

                        FileInputStream fis;
                        fis = new FileInputStream(fileToUpload);
//                        Message msg = layerClient.newMessage(layerClient.newMessagePart(mimeType, fis, fileToUpload.length()));
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();;
                        byte[] buffer = new byte[65536];
                        int bytesRead = 0;
                        int totalBytes = 0;
                        for (; (bytesRead = fis.read(buffer)) != -1; totalBytes += bytesRead) {
                            baos.write(buffer, 0, bytesRead);
                        }
                        fis.close();
                        byte[] content = baos.toByteArray();
                        baos.close();
                        if (debug) Log.w(TAG, "onActivityResult() loaded " + totalBytes + " into memory");
                        
                        Message msg = layerClient.newMessage(layerClient.newMessagePart(mimeType, content));
                        conv.send(msg);
                        if (debug) Log.w(TAG, "onActivityResult() uploaded " + fileToUpload.length() + " bytes");
                    } catch (Exception e) {
                        Log.e(TAG, "onActivityResult() cannot upload file: " + resultFileName, e);
                        return;
                    }
                }
                break;

            default :
                break;
        }
    }
    
    /**
     * pick file name from content provider with Gallery-flavor format
     */
    public String getGalleryImagePath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if (cursor == null) {
            return null;        // uri could be not suitable for ContentProviders, i.e. points to file 
        }
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    protected void onResume() {
        super.onResume();
        App101 app = (App101) getApplication();
        
        updateValues();
        messagesList.updateValues();
        messagesList.jumpToLastMessage();
        
        app.getLayerClient().registerEventListener(messagesList);
        
        // restore location tracking
        int requestLocationTimeout = 1 * 1000; // every second
        int distance = 100;
        Location loc = null;
        if (locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) { 
            loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (debug) Log.w(TAG, "onResume() location from gps: " + loc);
        }
        if (loc == null && locationManager.getProvider(LocationManager.NETWORK_PROVIDER) != null) {
            loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (debug) Log.w(TAG, "onResume() location from network: " + loc);
        } 
        if (loc != null && loc.getTime() < System.currentTimeMillis() + LOCATION_EXPIRATION_TIME) {
            locationTracker.onLocationChanged(loc);
        }
        if (locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, requestLocationTimeout, distance, locationTracker);
        }
        if (locationManager.getProvider(LocationManager.NETWORK_PROVIDER) != null) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, requestLocationTimeout, distance, locationTracker);
        }

    }
    
    private static final int LOCATION_EXPIRATION_TIME = 60 * 1000; // 1 minute 
    
    LocationListener locationTracker = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    lastKnownLocation = location;
                    if (debug) Log.w(TAG, "onLocationChanged() location: " + location);
                }
            });
        }
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        public void onProviderEnabled(String provider) {}
        public void onProviderDisabled(String provider) {}
    };
    
    @Override
    protected void onPause() {
        locationManager.removeUpdates(locationTracker);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        App101 app = (App101) getApplication();
        app.getLayerClient().unregisterEventListener(messagesList);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (debug) Log.w(TAG, "onConfigurationChanged() newConfig: " + newConfig);
        updateValues();
        messagesList.jumpToLastMessage();
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
    }

}
