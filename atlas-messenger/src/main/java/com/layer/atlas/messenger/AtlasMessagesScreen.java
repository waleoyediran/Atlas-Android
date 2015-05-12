package com.layer.atlas.messenger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.atlas.ParticipantPicker;
import com.layer.atlas.ShapedFrameLayout;
import com.layer.atlas.messenger.App101.Contact;
import com.layer.atlas.messenger.App101.keys;
import com.layer.sdk.LayerClient;
import com.layer.sdk.changes.LayerChange;
import com.layer.sdk.changes.LayerChange.Type;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.internal.utils.Log;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.listeners.LayerProgressListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.layer.transport.util.Streams;

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
    
    public static final String MIME_TYPE_ATLAS_LOCATION = "location/coordinate";
    public static final String MIME_TYPE_TEXT = "text/plain";
    public static final String MIME_TYPE_IMAGE_JPEG = "image/jpeg";
    public static final String MIME_TYPE_IMAGE_PNG = "image/png";
    
    private Conversation conv;
    private ArrayList<ViewItem> viewItems = new ArrayList<ViewItem>();
    
    private TextView messageText;
    private ListView messagesList;
    private View btnSend;
    private View btnUpload;
    private BaseAdapter messagesAdapter;
    
    private LocationManager locationManager;
    private Location lastKnownLocation;
    private Handler uiHandler;
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.uiHandler = new Handler();
        setContentView(R.layout.atlas_screen_messages);
        final App101 app = (App101) getApplication();

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
        
        String convUri = getIntent().getStringExtra(EXTRA_CONVERSATION_URI);
        if (convUri != null) {
            Uri uri = Uri.parse(convUri);
            conv = app.getLayerClient().getConversation(uri);
        }

        boolean convIsNew = getIntent().getBooleanExtra(EXTRA_CONVERSATION_IS_NEW, false);

        final View participantsPickerRoot = findViewById(R.id.atlas_screen_messages_participants_picker);
        final ParticipantPicker pp = new ParticipantPicker(this, participantsPickerRoot, app, null);
        if (convIsNew) {
            pp.setVisibility(View.VISIBLE);
        }
        
        btnUpload = findViewById(R.id.atlas_view_message_composer_upload);
        btnUpload.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                final PopupWindow popupWindow = new PopupWindow(v.getContext());
                popupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                LayoutInflater inflater = LayoutInflater.from(v.getContext());
                LinearLayout menu = (LinearLayout) inflater.inflate(R.layout.atlas_view_message_composer_menu, null);
                popupWindow.setContentView(menu);
                
                View convert = inflater.inflate(R.layout.atlas_view_message_composer_menu_convert, menu, false);
                ((TextView)convert.findViewById(R.id.altas_view_message_composer_convert_text)).setText("Location");
                menu.addView(convert);
                convert.setOnClickListener(new OnClickListener() {
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
                        MessagePart part = app.getLayerClient().newMessagePart(MIME_TYPE_ATLAS_LOCATION, locationString.getBytes());
                        Message message = app.getLayerClient().newMessage(Arrays.asList(part));
                        conv.send(message);
                        
                        if (debug) Log.w(TAG, "onSendLocation() loc:  " + locationString);
                        
                        popupWindow.dismiss();
                    }
                });
                
                convert = inflater.inflate(R.layout.atlas_view_message_composer_menu_convert, menu, false);
                ((TextView)convert.findViewById(R.id.altas_view_message_composer_convert_text)).setText("Photo");
                menu.addView(convert, 0);
                convert.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        // in onCreate or any event where your want the user to select a file
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CODE_GALLERY);
                        popupWindow.dismiss();
                    }
                });
                
                popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                popupWindow.setOutsideTouchable(true);
                int[] viewXYWindow = new int[2];  
                v.getLocationInWindow(viewXYWindow);
                
                menu.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                int menuHeight = menu.getMeasuredHeight();
                popupWindow.showAtLocation(v, Gravity.NO_GRAVITY, viewXYWindow[0], viewXYWindow[1] - menuHeight);
            }
        });
        
        messageText = (TextView) findViewById(R.id.atlas_view_message_composer_text);
        
        btnSend = findViewById(R.id.atlas_view_message_composer_send);
        btnSend.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String text = messageText.getText().toString();
                if (text.trim().length() > 0) {
                    
                    if (conv == null) { // create new one
                        String[] userIds = pp.getSelectedUserIds();
                        conv = app.getLayerClient().newConversation(userIds);
                        participantsPickerRoot.setVisibility(View.GONE);
                    }
                    ArrayList<MessagePart> parts = new ArrayList<MessagePart>();
                    String[] lines = text.split("\n+");
                    for (String line : lines) {
                        parts.add(app.getLayerClient().newMessagePart(line));
                    }
                    Message msg = app.getLayerClient().newMessage(parts);
                    conv.send(msg);
                    messageText.setText("");
                }
            }
        });
        
        // --- message view
        final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm a"); // TODO: localization required
        final SimpleDateFormat sdfDayOfWeek = new SimpleDateFormat("EEEE, LLL dd,"); // TODO: localization required
        messagesList = (ListView) findViewById(R.id.atlas_messages_view);
        messagesList.setAdapter(messagesAdapter = new BaseAdapter() {
            
            private static final int TYPE_ME = 0; 
            private static final int TYPE_OTHER = 1;
            
            public View getView(int position, View convertView, ViewGroup parent) {
                final ViewItem viewItem = viewItems.get(position);
                MessagePart part = viewItem.messagePart;
                String userId = part.getMessage().getSentByUserId();
                Contact contact = app.contactsMap.get(userId);
                
                int viewType = app.getLayerClient().getAuthenticatedUserId().equals(contact.userId) ? TYPE_ME : TYPE_OTHER;
                
                if (convertView == null) { 
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.atlas_view_messages_convert, parent, false);
                }
                
                View spacerTop = convertView.findViewById(R.id.atlas_view_messages_convert_spacer_top);
                spacerTop.setVisibility(viewItem.clusterItemId == viewItem.clusterHeadItemId && !viewItem.timeHeader ? View.VISIBLE : View.GONE); 
                
                View spacerBottom = convertView.findViewById(R.id.atlas_view_messages_convert_spacer_bottom);
                spacerBottom.setVisibility(viewItem.clusterTail ? View.VISIBLE : View.GONE); 
                
                // format date
                View timeBar = convertView.findViewById(R.id.atlas_view_messages_convert_timebar);
                TextView timeBarDay = (TextView) convertView.findViewById(R.id.atlas_view_messages_convert_timebar_day);
                TextView timeBarTime = (TextView) convertView.findViewById(R.id.atlas_view_messages_convert_timebar_time);
                if (viewItem.timeHeader) {
                    timeBar.setVisibility(View.VISIBLE);

                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    long todayMidnight = cal.getTimeInMillis();
                    long yesterMidnight = todayMidnight - (24 * 60 * 60 * 1000); // 24h less
                    Date sentAt = viewItem.messagePart.getMessage().getSentAt();
                    if (sentAt == null) sentAt = new Date();
                    
                    if (sentAt.getTime() > todayMidnight) {
                        timeBarDay.setText("Today"); 
                    } else if (sentAt.getTime() > yesterMidnight) {
                        timeBarDay.setText("Yesterday");
                    } else {
                        timeBarDay.setText(sdfDayOfWeek.format(sentAt));
                    }
                    timeBarTime.setText(sdf.format(sentAt.getTime()));
                } else {
                    timeBar.setVisibility(View.GONE);
                }
                
                TextView textAvatar = (TextView) convertView.findViewById(R.id.atlas_view_messages_convert_initials);
                View spacerRight = convertView.findViewById(R.id.atlas_view_messages_convert_spacer_right);
                if (viewType == TYPE_OTHER) {
                    spacerRight.setVisibility(View.VISIBLE);
                    String displayText = App101.getContactInitials(contact);
                    textAvatar.setText(displayText);
                    textAvatar.setVisibility(View.VISIBLE);
                } else {
                    spacerRight.setVisibility(View.GONE);
                    textAvatar.setVisibility(View.INVISIBLE);
                }
                
                
                // processing cell
                
                View cellContainer = convertView.findViewById(R.id.atlas_view_messages_cell_container);
                View cellText = convertView.findViewById(R.id.atlas_view_messages_cell_text);
                ShapedFrameLayout cellCustom = (ShapedFrameLayout) convertView.findViewById(R.id.atlas_view_messages_cell_custom);
                
                if (MIME_TYPE_IMAGE_JPEG.equals(part.getMimeType()) || MIME_TYPE_IMAGE_PNG.equals(part.getMimeType())) {
                    
                    cellText.setVisibility(View.GONE);
                    cellCustom.setVisibility(View.VISIBLE);
                    ImageView imageView = (ImageView) cellCustom.findViewById(R.id.atlas_view_messages_cell_custom_image);
                    
                    // get BitmapDrawable
                    //BitmapDrawable EMPTY_DRAWABLE = new BitmapDrawable(Bitmap.createBitmap(new int[] { Color.TRANSPARENT }, 1, 1, Bitmap.Config.ALPHA_8));
                    int requiredWidth = cellContainer.getWidth();
                    int requiredHeight = cellContainer.getHeight();
                    final MessagePart messagePart = viewItem.messagePart;
                    Bitmap bmp = getBitmap(messagePart, requiredWidth, requiredHeight);
                    if (bmp != null) {
                        imageView.setImageBitmap(bmp);
                    } else {
                        imageView.setImageResource(R.drawable.image_stub);
                    }
                    
                    // clustering
                    
                    cellCustom.setCornerRadiusDp(16, 16, 16, 16);

                    if (viewType == TYPE_OTHER) {
                        if (viewItem.clusterHeadItemId == viewItem.clusterItemId && !viewItem.clusterTail) {
                            cellCustom.setCornerRadiusDp(16, 16, 16, 2);
                        } else if (viewItem.clusterTail && viewItem.clusterHeadItemId != viewItem.clusterItemId) {
                            cellCustom.setCornerRadiusDp(2, 16, 16, 16);
                        } else if (viewItem.clusterHeadItemId != viewItem.clusterItemId && !viewItem.clusterTail) {
                            cellCustom.setCornerRadiusDp(2, 16, 16, 2);
                        }
                    } else {
                        if (viewItem.clusterHeadItemId == viewItem.clusterItemId && !viewItem.clusterTail) {
                            cellCustom.setCornerRadiusDp(16, 16, 2, 16);
                            //cellCustom.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_bottom_right);
                        } else if (viewItem.clusterTail && viewItem.clusterHeadItemId != viewItem.clusterItemId) {
                            cellCustom.setCornerRadiusDp(16, 2, 16, 16);
                            //cellCustom.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_top_right);
                        } else if (viewItem.clusterHeadItemId != viewItem.clusterItemId && !viewItem.clusterTail) {
                            cellCustom.setCornerRadiusDp(16, 2, 2, 16);
                            //cellCustom.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_right);
                        }
                    }
                    
                } else { /* MIME_TYPE_TEXT */                           // text and replaced by text
                    cellText.setVisibility(View.VISIBLE);
                    cellCustom.setVisibility(View.GONE);
                    
                    String messagePartText = null;
                    if (MIME_TYPE_TEXT.equals(part.getMimeType())) {
                        messagePartText = new String(part.getData());
                    } else if (MIME_TYPE_ATLAS_LOCATION.equals(part.getMimeType())){
                        String jsonLonLat = new String(part.getData());
                        
                        JSONObject json;
                        try {
                            json = new JSONObject(jsonLonLat);
                            double lon = json.getDouble("lon");
                            double lat = json.getDouble("lat");
                            messagePartText = "Location:\nlon: " + lon + "\nlat: " + lat;
                        } catch (JSONException e) {}
//                        String noBraces = jsonLonLat.replaceAll("[\\{\\}]", "");
//                        String[] latAndLon = noBraces.split("");
//                        String lon = jsonLonLat.substring(jsonLonLat.indexOf(""))
                    } else {
                        messagePartText = "attach, type: " + part.getMimeType() + ", size: " + part.getSize();
                    }
                    
                    TextView textMy = (TextView) cellText.findViewById(R.id.atlas_view_messages_convert_text);
                    TextView textOther = (TextView) cellText.findViewById(R.id.atlas_view_messages_convert_text_counterparty);
                    if (viewType == TYPE_OTHER) {
                        textOther.setVisibility(View.VISIBLE);
                        textOther.setText(messagePartText);
                        textMy.setVisibility(View.GONE);
                        
                        textOther.setBackgroundResource(R.drawable.atlas_shape_rounded16_gray);
                        if (viewItem.clusterHeadItemId == viewItem.clusterItemId && !viewItem.clusterTail) {
                            textOther.setBackgroundResource(R.drawable.atlas_shape_rounded16_gray_no_bottom_left);
                        } else if (viewItem.clusterTail && viewItem.clusterHeadItemId != viewItem.clusterItemId) {
                            textOther.setBackgroundResource(R.drawable.atlas_shape_rounded16_gray_no_top_left);
                        } else if (viewItem.clusterHeadItemId != viewItem.clusterItemId && !viewItem.clusterTail) {
                            textOther.setBackgroundResource(R.drawable.atlas_shape_rounded16_gray_no_left);
                        }

                    } else {
                        textMy.setVisibility(View.VISIBLE);
                        textMy.setText(messagePartText);
                        textOther.setVisibility(View.GONE);
                        
                        textMy.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue);
                        if (viewItem.clusterHeadItemId == viewItem.clusterItemId && !viewItem.clusterTail) {
                            textMy.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_bottom_right);
                        } else if (viewItem.clusterTail && viewItem.clusterHeadItemId != viewItem.clusterItemId) {
                            textMy.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_top_right);
                        } else if (viewItem.clusterHeadItemId != viewItem.clusterItemId && !viewItem.clusterTail) {
                            textMy.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_right);
                        }
                    }
                }

                // mark displayed message as read
                Message msg = part.getMessage();
                if (!msg.getSentByUserId().equals(app.getLayerClient().getAuthenticatedUserId())) {
                    msg.markAsRead();
                }
                
                return convertView;
            }
            
            Map<String, Bitmap> imageLruCache = Collections.synchronizedMap(new LinkedHashMap<String, Bitmap>(10, 0.75f, true) {
                private static final long serialVersionUID = 1L;
                protected boolean removeEldestEntry(Entry<String, Bitmap> eldest) {
                    if (this.size() > 10) return true;
                    return false;
                }
            });
            
            public Bitmap getBitmap(final MessagePart messagePart, int requiredWidth, int requiredHeight) {
                Bitmap cached = imageLruCache.get(messagePart.getId().toString());
                if (cached != null && cached.getWidth() >= requiredWidth / 2) {
                    if (debug) Log.i(TAG, "getBitmap() returned from cache! " + cached.getWidth() + "x" + cached.getHeight() + " " + cached.getByteCount() + " bytes" + " req: " + requiredWidth + "x" + requiredHeight + " for " + messagePart.getId());
                    return cached;
                }
                
                // load
                long started = System.currentTimeMillis();
                if ( requiredWidth <= 0 || requiredHeight <= 0) {
                    Display display = getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    requiredWidth = requiredWidth > 0 ? requiredWidth : size.x;
                    requiredHeight = requiredHeight > 0 ? requiredHeight : size.y;
                }
                messagePart.download(new LayerProgressListener() {
                    public void onProgressUpdate(MessagePart part, Operation operation, long transferredBytes) {
                        if (debug) Log.d(TAG, "onProgressUpdate() part: " + part+ " operation: " + operation+ " transferredBytes: " + transferredBytes);
                    }
                    public void onProgressStart(MessagePart part, Operation operation) {
                        if (debug) Log.d(TAG, "onProgressStart() part: " + part+ " operation: " + operation);
                    }
                    public void onProgressError(MessagePart part, Operation operation, Throwable cause) {
                        if (debug) Log.d(TAG, "onProgressError() part: " + part+ " operation: " + operation+ " cause: " + cause);
                    }
                    public void onProgressComplete(MessagePart part, Operation operation) {
                        if (debug) Log.d(TAG, "onProgressComplete() part: " + part+ " operation: " + operation);
                    }
                });
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(messagePart.getDataStream(), null, opts);
                int originalWidth = opts.outWidth;
                int originalHeight = opts.outHeight;
                int sampleSize = 1;
                while (opts.outWidth / (sampleSize * 2) > requiredWidth) {
                    sampleSize *= 2;
                }
                
                BitmapFactory.Options opts2 = new BitmapFactory.Options();
                opts2.inSampleSize = sampleSize;
                Bitmap bmp = BitmapFactory.decodeStream(messagePart.getDataStream(), null, opts2);
                if (bmp != null) {
                    if (debug) Log.d(TAG, "decodeImage() decoded " + bmp.getWidth() + "x" + bmp.getHeight() 
                            + " " + bmp.getByteCount() + " bytes" 
                            + " req: " + requiredWidth + "x" + requiredHeight 
                            + " original: " + originalWidth + "x" + originalHeight 
                            + " sampleSize: " + sampleSize
                            + " in " +(System.currentTimeMillis() - started) + "ms from: " + messagePart.getId());
                } else {
                    if (debug) Log.d(TAG, "decodeImage() not decoded " + " req: " + requiredWidth + "x" + requiredHeight 
                            + " in " +(System.currentTimeMillis() - started) + "ms from: " + messagePart.getId());
                }
                
                if (bmp != null) {
                    imageLruCache.put(messagePart.getId().toString(), bmp);
                }
                
                return bmp;
            }
            public long getItemId(int position) {
                return position;
            }
            public Object getItem(int position) {
                return viewItems.get(position);
            }
            public int getCount() {
                return viewItems.size();
            }
            
        });
        
        messagesList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ViewItem item = viewItems.get(position);
                if (MIME_TYPE_ATLAS_LOCATION.equals(item.messagePart.getMimeType())) {
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
        // --- end of messageView
        
        // location manager for inserting locations:
        this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }
    
    static class ViewItem {
        MessagePart messagePart;
        int clusterHeadItemId;
        int clusterItemId;
        boolean clusterTail;
        boolean timeHeader;
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("[ ")
                .append("messagePart: ").append(messagePart.getSize() < 2048 ? new String(messagePart.getData()) : messagePart.getSize() + " bytes" )
                .append(", clusterId: ").append(clusterHeadItemId)
                .append(", clusterItem: ").append(clusterItemId)
                .append(", clusterTail: ").append(clusterTail)
                .append(", timeHeader: ").append(timeHeader).append(" ]");
            return builder.toString();
        }
        
        
    }

    private void updateValues() {
        App101 app = (App101) getApplication();
        
        if (conv == null) {
            Log.e(TAG, "updateValues() no conversation set");
            return;
        }
        
        long started = System.currentTimeMillis();
        
        List<Message> messages = app.getLayerClient().getMessages(conv);
        viewItems.clear();
        for (Message message : messages) {
            List<MessagePart> parts = message.getMessageParts();
            for (MessagePart messagePart : parts) {
                ViewItem item = new ViewItem();
                item.messagePart = messagePart;
                viewItems.add(item);
            }
        }
        
        // calculate heads/tails
        int currentItem = 0;
        int clusterId = currentItem;
        String currentUser = null;
        long lastMessageTime = 0;
        Calendar calLastMessage = Calendar.getInstance();
        Calendar calCurrent = Calendar.getInstance();
        long clusterTimeSpan = 60 * 1000; // 1 minute
        long oneHourSpan = 60 * 60 * 1000; // 1 hour
        for (int i = 0; i < viewItems.size(); i++) {
            ViewItem item = viewItems.get(i);
            boolean newCluster = false;
            if (!item.messagePart.getMessage().getSentByUserId().equals(currentUser)) {
                newCluster = true;
            }
            Date sentAt = item.messagePart.getMessage().getSentAt();
            if (sentAt == null) sentAt = new Date();
            
            if (sentAt.getTime() - lastMessageTime > clusterTimeSpan) {
                newCluster = true;
            }
            
            if (newCluster) {
                clusterId = currentItem;
                if (i > 0) viewItems.get(i - 1).clusterTail = true;
            }
            
            // check time header is needed
            if (sentAt.getTime() - lastMessageTime > oneHourSpan) {
                item.timeHeader = true;
            }
            calCurrent.setTime(sentAt);
            if (calCurrent.get(Calendar.DAY_OF_YEAR) != calLastMessage.get(Calendar.DAY_OF_YEAR)) {
                item.timeHeader = true;
            }
            
            item.clusterHeadItemId = clusterId;
            item.clusterItemId = currentItem++;
            
            currentUser = item.messagePart.getMessage().getSentByUserId();
            lastMessageTime = sentAt.getTime();
            calLastMessage.setTime(sentAt);
            if (debug) Log.d(TAG, "updateValues() item: " + item);
        }
        viewItems.get(viewItems.size() - 1).clusterTail = true; // last one is always a tail
        
        Log.d(TAG, "updateValues() parts finished in: " + (System.currentTimeMillis() - started));
        messagesAdapter.notifyDataSetChanged();
        
        // update buddies:
        StringBuilder sb = new StringBuilder();
        for (String userId : conv.getParticipants()) {
            if (app.getLayerClient().getAuthenticatedUserId().equals(userId)) continue;
            Contact contact = app.contactsMap.get(userId);
            String initials = conv.getParticipants().size() > 2 ? App101.getContactFirstAndL(contact) : App101.getContactFirstAndLast(contact);
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
                    + ", data: " + (data == null ? "" : Log.toString(data.getExtras())) );
        
        if (resultCode != Activity.RESULT_OK) return;
        
        switch (requestCode) {
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
                    
                    String mimeType = MIME_TYPE_IMAGE_JPEG;
                    if (resultFileName.endsWith(".png")) mimeType = MIME_TYPE_IMAGE_PNG;
                    
                    // test file copy locally
                    try {
                        FileInputStream fisExternal = new FileInputStream(fileToUpload);
                        String testFileName = "copy" + String.format("%04d", System.currentTimeMillis()/1000L % 3600) + "-" + fileToUpload.getName();  
                        FileOutputStream fos = openFileOutput(testFileName, 0);
                        byte[] buffer = new byte[65536];
                        int bytesRead = 0;
                        int totalBytes = 0;
                        for (; (bytesRead = fisExternal.read(buffer)) != -1; totalBytes += bytesRead) {
                            fos.write(buffer, 0, bytesRead);
                        }
                        fos.close();
                        fisExternal.close();
                        if (debug) Log.w(TAG, "onActivityResult() copied " + totalBytes + " bytes into " + testFileName);
                        
                        LayerClient layerClient = ((App101) getApplication()).getLayerClient();
                        
                        FileInputStream fis;
                        fis = openFileInput(testFileName);
                        //Message msg = layerClient.newMessage(layerClient.newMessagePart(mimeType, fis, fileToUpload.length()));
                        byte[] content = Streams.readFully(fis);
                        Message msg = layerClient.newMessage(layerClient.newMessagePart(mimeType, content));
                        conv.send(msg);
                        fis.close();
                        File dir = getFilesDir();
                        File file = new File(dir, testFileName);
                        boolean deleted = file.delete();
                        if (debug) Log.w(TAG, "onActivityResult() uploaded " + fileToUpload.length() + " bytes. Tmp file " + testFileName + (deleted ? " " : " is not") + " deleted");
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

    private LayerChangeEventListener.MainThread eventTracker;
    
    @Override
    protected void onResume() {
        super.onResume();
        App101 app = (App101) getApplication();
        
        app.getLayerClient().registerEventListener(eventTracker = new LayerChangeEventListener.MainThread() {
            public void onEventMainThread(LayerChangeEvent event) {
                boolean refresh = false;
                for (LayerChange layerChange : event.getChanges()) {
                    if (layerChange.getChangeType() == Type.DELETE || layerChange.getChangeType() == Type.INSERT) {
                        refresh = true;
                        break;
                    }
                }
                if (refresh) {
                    updateValues();
                    messagesList.smoothScrollToPosition(messagesAdapter.getCount() - 1);
                }
            }
        });
        
        updateValues();
        messagesList.setSelection(messagesAdapter.getCount() - 1);

        
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
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        App101 app = (App101) getApplication();
        app.getLayerClient().unregisterEventListener(eventTracker);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (debug) Log.w(TAG, "onConfigurationChanged() newConfig: " + newConfig);
        updateValues();
        messagesList.smoothScrollToPosition(messagesAdapter.getCount() - 1);
    }

}
