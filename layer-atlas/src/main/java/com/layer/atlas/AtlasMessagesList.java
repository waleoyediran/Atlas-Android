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
import com.layer.sdk.messaging.LayerObject;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

/**
 * @author Oleg Orlov
 * @since 13 May 2015
 */
public class AtlasMessagesList implements LayerChangeEventListener.MainThread {
    private static final String TAG = AtlasMessagesList.class.getSimpleName();
    private static final boolean debug = true;
    
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm a"); // TODO: localization required
    private static final SimpleDateFormat sdfDayOfWeek = new SimpleDateFormat("EEEE, LLL dd,"); // TODO: localization required

    private ListView messagesList;
    private BaseAdapter messagesAdapter;

    private ArrayList<Cell> viewItems = new ArrayList<Cell>();
    
    private LayerClient client;
    private Conversation conv;
    
    private ItemClickListener clickListener;
    
    public AtlasMessagesList(View rootView, LayerClient layerClient, final AtlasContactProvider contactsProvider) {
        this.client = layerClient;
        
        // --- message view
        messagesList = (ListView) rootView.findViewById(R.id.atlas_messages_list);
        messagesList.setAdapter(messagesAdapter = new BaseAdapter() {
            
            public View getView(int position, View convertView, ViewGroup parent) {
                final Cell cell = viewItems.get(position);
                MessagePart part = cell.messagePart;
                String userId = part.getMessage().getSender().getUserId();

                boolean myMessage = client.getAuthenticatedUserId().equals(userId);
                
                if (convertView == null) { 
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.atlas_view_messages_convert, parent, false);
                }
                
                View spacerTop = convertView.findViewById(R.id.atlas_view_messages_convert_spacer_top);
                spacerTop.setVisibility(cell.clusterItemId == cell.clusterHeadItemId && !cell.timeHeader ? View.VISIBLE : View.GONE); 
                
                View spacerBottom = convertView.findViewById(R.id.atlas_view_messages_convert_spacer_bottom);
                spacerBottom.setVisibility(cell.clusterTail ? View.VISIBLE : View.GONE); 
                
                // format date
                View timeBar = convertView.findViewById(R.id.atlas_view_messages_convert_timebar);
                TextView timeBarDay = (TextView) convertView.findViewById(R.id.atlas_view_messages_convert_timebar_day);
                TextView timeBarTime = (TextView) convertView.findViewById(R.id.atlas_view_messages_convert_timebar_time);
                if (cell.timeHeader) {
                    timeBar.setVisibility(View.VISIBLE);

                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    long todayMidnight = cal.getTimeInMillis();
                    long yesterMidnight = todayMidnight - (24 * 60 * 60 * 1000); // 24h less
                    Date sentAt = cell.messagePart.getMessage().getSentAt();
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
                
                Contact contact = contactsProvider.contactsMap.get(userId);
                TextView textAvatar = (TextView) convertView.findViewById(R.id.atlas_view_messages_convert_initials);
                View spacerRight = convertView.findViewById(R.id.atlas_view_messages_convert_spacer_right);
                if (myMessage) {
                    spacerRight.setVisibility(View.GONE);
                    textAvatar.setVisibility(View.INVISIBLE);
                } else {
                    spacerRight.setVisibility(View.VISIBLE);
                    String displayText = contact != null ? AtlasContactProvider.getContactInitials(contact) : "";
                    textAvatar.setText(displayText);
                    textAvatar.setVisibility(View.VISIBLE);
                }
                
                // processing cell
                bindCell(convertView, cell);

                // mark displayed message as read
                Message msg = part.getMessage();
                if (!msg.getSender().getUserId().equals(client.getAuthenticatedUserId())) {
                    msg.markAsRead();
                }
                
                return convertView;
            }
            
            public void bindCell(View convertView, final Cell cell) {
                
                View cellContainer = convertView.findViewById(R.id.atlas_view_messages_cell_container);
                View cellText = convertView.findViewById(R.id.atlas_view_messages_cell_text);
                ShapedFrameLayout cellCustom = (ShapedFrameLayout) convertView.findViewById(R.id.atlas_view_messages_cell_custom);
                MessagePart part = cell.messagePart;
                if (Atlas.MIME_TYPE_IMAGE_JPEG.equals(part.getMimeType()) || Atlas.MIME_TYPE_IMAGE_PNG.equals(part.getMimeType())) {
                    cellText.setVisibility(View.GONE);
                    cellCustom.setVisibility(View.VISIBLE);
                    ImageView imageView = (ImageView) cellCustom.findViewById(R.id.atlas_view_messages_cell_custom_image);
                    
                    // get BitmapDrawable
                    //BitmapDrawable EMPTY_DRAWABLE = new BitmapDrawable(Bitmap.createBitmap(new int[] { Color.TRANSPARENT }, 1, 1, Bitmap.Config.ALPHA_8));
                    int requiredWidth = cellContainer.getWidth();
                    int requiredHeight = cellContainer.getHeight();
                    final MessagePart messagePart = cell.messagePart;
                    Bitmap bmp = getBitmap(messagePart, requiredWidth, requiredHeight);
                    if (bmp != null) {
                        imageView.setImageBitmap(bmp);
                    } else {
                        imageView.setImageResource(R.drawable.image_stub);
                    }
                    
                    // clustering
                    
                    cellCustom.setCornerRadiusDp(16, 16, 16, 16);
                    boolean myMessage = client.getAuthenticatedUserId().equals(cell.messagePart.getMessage().getSender().getUserId());
                    if (myMessage) {
                        if (cell.clusterHeadItemId == cell.clusterItemId && !cell.clusterTail) {
                            cellCustom.setCornerRadiusDp(16, 16, 2, 16);
                            //cellCustom.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_bottom_right);
                        } else if (cell.clusterTail && cell.clusterHeadItemId != cell.clusterItemId) {
                            cellCustom.setCornerRadiusDp(16, 2, 16, 16);
                            //cellCustom.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_top_right);
                        } else if (cell.clusterHeadItemId != cell.clusterItemId && !cell.clusterTail) {
                            cellCustom.setCornerRadiusDp(16, 2, 2, 16);
                            //cellCustom.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_right);
                        }
                    } else {
                        if (cell.clusterHeadItemId == cell.clusterItemId && !cell.clusterTail) {
                            cellCustom.setCornerRadiusDp(16, 16, 16, 2);
                        } else if (cell.clusterTail && cell.clusterHeadItemId != cell.clusterItemId) {
                            cellCustom.setCornerRadiusDp(2, 16, 16, 16);
                        } else if (cell.clusterHeadItemId != cell.clusterItemId && !cell.clusterTail) {
                            cellCustom.setCornerRadiusDp(2, 16, 16, 2);
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
                    
                    boolean myMessage = client.getAuthenticatedUserId().equals(cell.messagePart.getMessage().getSender().getUserId());
                    TextView textMy = (TextView) cellText.findViewById(R.id.atlas_view_messages_convert_text);
                    TextView textOther = (TextView) cellText.findViewById(R.id.atlas_view_messages_convert_text_counterparty);
                    if (myMessage) {
                        textMy.setVisibility(View.VISIBLE);
                        textMy.setText(messagePartText);
                        textOther.setVisibility(View.GONE);
                        
                        textMy.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue);
                        if (cell.clusterHeadItemId == cell.clusterItemId && !cell.clusterTail) {
                            textMy.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_bottom_right);
                        } else if (cell.clusterTail && cell.clusterHeadItemId != cell.clusterItemId) {
                            textMy.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_top_right);
                        } else if (cell.clusterHeadItemId != cell.clusterItemId && !cell.clusterTail) {
                            textMy.setBackgroundResource(R.drawable.atlas_shape_rounded16_blue_no_right);
                        }
                    } else {
                        textOther.setVisibility(View.VISIBLE);
                        textOther.setText(messagePartText);
                        textMy.setVisibility(View.GONE);
                        
                        textOther.setBackgroundResource(R.drawable.atlas_shape_rounded16_gray);
                        if (cell.clusterHeadItemId == cell.clusterItemId && !cell.clusterTail) {
                            textOther.setBackgroundResource(R.drawable.atlas_shape_rounded16_gray_no_bottom_left);
                        } else if (cell.clusterTail && cell.clusterHeadItemId != cell.clusterItemId) {
                            textOther.setBackgroundResource(R.drawable.atlas_shape_rounded16_gray_no_top_left);
                        } else if (cell.clusterHeadItemId != cell.clusterItemId && !cell.clusterTail) {
                            textOther.setBackgroundResource(R.drawable.atlas_shape_rounded16_gray_no_left);
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
                Cell item = viewItems.get(position);
                if (clickListener!= null) {
                    clickListener.onItemClick(item);
                }
            }
        });
        // --- end of messageView
        
    }
    
    protected void cellForMessage(Message msg, ArrayList<Cell> destination) {
        
        final ArrayList<MessagePart> parts = new ArrayList<MessagePart>(msg.getMessageParts());
        
        for (int partNo = 0; partNo < parts.size(); partNo++ ) {
            final String mimeType = parts.get(partNo).getMimeType();
            if (Atlas.MIME_TYPE_IMAGE_PNG.equals(mimeType) || Atlas.MIME_TYPE_IMAGE_JPEG.equals(mimeType)) {
                    
                // 3 parts image support
                if ((partNo + 2 < parts.size()) && Atlas.MIME_TYPE_IMAGE_DIMENSIONS.equals(parts.get(partNo + 2).getMimeType())) {
                    String jsonDimensions = new String(parts.get(partNo + 2).getData());
                    try {
                        JSONObject jo = new JSONObject(jsonDimensions);
                        int width = jo.getInt("width");
                        int height = jo.getInt("height");
                        Cell imageCell = new ImageCell(parts.get(partNo), parts.get(partNo + 1), width, height);
                        destination.add(imageCell);
                        if (debug) Log.w(TAG, "cellForMessage() 3-image part found at partNo: " + partNo);
                        partNo++; // skip preview
                        partNo++; // skip dimenstions part
                    } catch (JSONException e) {
                        Log.e(TAG, "cellForMessage() cannot parse 3-part image", e);
                    }
                } else {
                    // regular image
                    destination.add(new ImageCell(parts.get(partNo)));
                    if (debug) Log.w(TAG, "cellForMessage() single-image part found at partNo: " + partNo);
                }
            } else {
                Cell cellData = new Cell(parts.get(partNo));
                if (debug) Log.w(TAG, "cellForMessage() default item: " + cellData);
                destination.add(cellData);
            }
        }
        
    }
    
    public void updateValues() {
        if (conv == null) return;
        
        long started = System.currentTimeMillis();
        
        List<Message> messages = client.getMessages(conv);
        viewItems.clear();
        if (messages.isEmpty()) return;
        
        ArrayList<Cell> messageItems = new ArrayList<AtlasMessagesList.Cell>();
        for (Message message : messages) {
            // System messages have `null` user ID
            if (message.getSender().getUserId() == null) continue;  

            List<MessagePart> parts = message.getMessageParts();
            messageItems.clear();
            cellForMessage(message, messageItems);
            viewItems.addAll(messageItems);
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
            Cell item = viewItems.get(i);
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

        if (debug) Log.d(TAG, "updateValues() parts finished in: " + (System.currentTimeMillis() - started));
        messagesAdapter.notifyDataSetChanged();

    }
    
    @Override
    public void onEventMainThread(LayerChangeEvent event) {
        if (conv == null) return;
        boolean updateValues = false;
        boolean jumpToBottom = false;
        for (LayerChange change : event.getChanges()) {
            if (change.getObjectType() == LayerObject.Type.MESSAGE) {
                Message msg = (Message) change.getObject();
                if (msg.getConversation().getId().equals(conv.getId())) {
                    updateValues = true;
                    if (change.getChangeType() == Type.DELETE || change.getChangeType() == Type.INSERT) {
                        jumpToBottom = true;
                    }
                }
            }
        }
        if (updateValues) updateValues();
        if (jumpToBottom) messagesList.smoothScrollToPosition(messagesAdapter.getCount() - 1);
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

    private static class ImageCell extends Cell {
        MessagePart previewPart;
        MessagePart fullPart;
        int width;
        int height;

        private ImageCell(MessagePart fullImagePart) {
            super(fullImagePart);
            this.fullPart = fullPart;
        }
        private ImageCell(MessagePart fullImagePart, MessagePart previewImagePart, int width, int height) {
            super(fullImagePart);
            this.fullPart = fullImagePart;
            this.previewPart = previewImagePart;
            this.width = width;
            this.height = height;
        }
    }

    public static class Cell {
        public final MessagePart messagePart;
        private int clusterHeadItemId;
        private int clusterItemId;
        private boolean clusterTail;
        private boolean timeHeader;
        
        public Cell(MessagePart messagePart) {
            this.messagePart = messagePart;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("[ ")
                .append("messagePart: ").append(messagePart.getMimeType())
                .append(": ").append(messagePart.getSize() < 2048 ? new String(messagePart.getData()) : messagePart.getSize() + " bytes" )
                .append(", clusterId: ").append(clusterHeadItemId)
                .append(", clusterItem: ").append(clusterItemId)
                .append(", clusterTail: ").append(clusterTail)
                .append(", timeHeader: ").append(timeHeader).append(" ]");
            return builder.toString();
        }

        public void onBind(View container) {
            
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
        public void onItemClick(Cell item);
    }

}
