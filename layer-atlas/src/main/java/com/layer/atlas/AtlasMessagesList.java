package com.layer.atlas;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.layer.atlas.Atlas.AtlasContactProvider;
import com.layer.atlas.Atlas.Contact;
import com.layer.sdk.LayerClient;
import com.layer.sdk.changes.LayerChange;
import com.layer.sdk.changes.LayerChange.Type;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.listeners.LayerProgressListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

/**
 * @author olegorlov
 * @since 13 May 2015
 */
public class AtlasMessagesList implements LayerChangeEventListener.MainThread {
    private static final String TAG = AtlasMessagesList.class.getSimpleName();
    private static final boolean debug = true;
    
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm a"); // TODO: localization required
    private static final SimpleDateFormat sdfDayOfWeek = new SimpleDateFormat("EEEE, LLL dd,"); // TODO: localization required

    private ListView messagesList;
    private BaseAdapter messagesAdapter;

    private ArrayList<CellDataItem> viewItems = new ArrayList<CellDataItem>();
    
    private LayerClient client;
    private Conversation conv;
    
    private ItemClickListener clickListener;
    
    public AtlasMessagesList(View rootView, LayerClient layerClient, final AtlasContactProvider contacts) {
        this.client = layerClient;  

        // --- message view
        messagesList = (ListView) rootView.findViewById(R.id.atlas_view_messages_list);
        messagesList.setAdapter(messagesAdapter = new BaseAdapter() {
            
            private static final int TYPE_ME = 0; 
            private static final int TYPE_OTHER = 1;
            
            public View getView(int position, View convertView, ViewGroup parent) {
                final CellDataItem viewItem = viewItems.get(position);
                MessagePart part = viewItem.messagePart;
                String userId = part.getMessage().getSender().getUserId();

                int viewType = client.getAuthenticatedUserId().equals(userId) ? TYPE_ME : TYPE_OTHER;
                
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
                
                Contact contact = contacts.contactsMap.get(userId);
                TextView textAvatar = (TextView) convertView.findViewById(R.id.atlas_view_messages_convert_initials);
                View spacerRight = convertView.findViewById(R.id.atlas_view_messages_convert_spacer_right);
                if (viewType == TYPE_OTHER) {
                    spacerRight.setVisibility(View.VISIBLE);
                    String displayText = contact != null ? AtlasContactProvider.getContactInitials(contact) : "";
                    textAvatar.setText(displayText);
                    textAvatar.setVisibility(View.VISIBLE);
                } else {
                    spacerRight.setVisibility(View.GONE);
                    textAvatar.setVisibility(View.INVISIBLE);
                }
                
                // processing cell
                bindCell(convertView, viewItem, viewType);

                // mark displayed message as read
                Message msg = part.getMessage();
                if (!msg.getSender().getUserId().equals(client.getAuthenticatedUserId())) {
                    msg.markAsRead();
                }
                
                return convertView;
            }
            
            public void bindCell(View convertView, final CellDataItem viewItem, int viewType) {
                
                View cellContainer = convertView.findViewById(R.id.atlas_view_messages_cell_container);
                View cellText = convertView.findViewById(R.id.atlas_view_messages_cell_text);
                ShapedFrameLayout cellCustom = (ShapedFrameLayout) convertView.findViewById(R.id.atlas_view_messages_cell_custom);
                MessagePart part = viewItem.messagePart;
                if (Atlas.MIME_TYPE_IMAGE_JPEG.equals(part.getMimeType()) || Atlas.MIME_TYPE_IMAGE_PNG.equals(part.getMimeType())) {
                    
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
                    if (Atlas.MIME_TYPE_TEXT.equals(part.getMimeType())) {
                        messagePartText = new String(part.getData());
                    } else if (Atlas.MIME_TYPE_ATLAS_LOCATION.equals(part.getMimeType())){
                        String jsonLonLat = new String(part.getData());
                        try {
                            JSONObject json = new JSONObject(jsonLonLat);
                            double lon = json.getDouble("lon");
                            double lat = json.getDouble("lat");
                            messagePartText = "Location:\nlon: " + lon + "\nlat: " + lat;
                        } catch (JSONException e) {}
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
                CellDataItem item = viewItems.get(position);
                if (clickListener!= null) {
                    clickListener.onItemClick(item);
                }
            }
        });
        // --- end of messageView
    }
    
    public void updateValues() {
        if (conv == null) return;
        
        long started = System.currentTimeMillis();
        
        List<Message> messages = client.getMessages(conv);
        viewItems.clear();
        if (messages.isEmpty()) return;

        for (Message message : messages) {
            if (message.getSender().getUserId() == null) continue;             // System messages have `null` user ID

            List<MessagePart> parts = message.getMessageParts();
            for (MessagePart messagePart : parts) {
                CellDataItem item = new CellDataItem();
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
            CellDataItem item = viewItems.get(i);
            boolean newCluster = false;
            if (!item.messagePart.getMessage().getSender().getUserId().equals(currentUser)) {
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
            
            currentUser = item.messagePart.getMessage().getSender().getUserId();
            lastMessageTime = sentAt.getTime();
            calLastMessage.setTime(sentAt);
            if (debug) Log.d(TAG, "updateValues() item: " + item);
        }
            viewItems.get(viewItems.size() - 1).clusterTail = true; // last one is always a tail

        Log.d(TAG, "updateValues() parts finished in: " + (System.currentTimeMillis() - started));
        messagesAdapter.notifyDataSetChanged();

    }
    
    @Override
    public void onEventMainThread(LayerChangeEvent event) {
        if (conv == null) return;
        for (LayerChange layerChange : event.getChanges()) {
            if (layerChange.getChangeType() == Type.DELETE || layerChange.getChangeType() == Type.INSERT) {
                updateValues();
                messagesList.smoothScrollToPosition(messagesAdapter.getCount() - 1);
                break;
            }
        }
    }

    public void jumpToLastMessage() {
        messagesList.smoothScrollToPosition(viewItems.size() - 1);
    }

    public Conversation getConversation() {
        return conv;
    }

    public void setConversation(Conversation conv) {
        this.conv = conv;
        updateValues();
        jumpToLastMessage();
    }
    
    public void setItemClickListener(ItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public static class CellDataItem {
        public MessagePart messagePart;
        private int clusterHeadItemId;
        private int clusterItemId;
        private boolean clusterTail;
        private boolean timeHeader;
        
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
            requiredWidth = requiredWidth > 0 ? requiredWidth : messagesList.getWidth();
            requiredHeight = requiredHeight > 0 ? requiredHeight : messagesList.getHeight();
        }
        if ( requiredWidth <= 0 || requiredHeight <= 0) {
//            Display display = getWindowManager().getDefaultDisplay();
//            Point size = new Point();
//            display.getSize(size);
            requiredWidth = requiredWidth > 0 ? requiredWidth : 100;    //size.x;
            requiredHeight = requiredHeight > 0 ? requiredHeight : 100; //size.y;
            
            
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
    
    public interface ItemClickListener {
        public void onItemClick(CellDataItem item);
    }

}
