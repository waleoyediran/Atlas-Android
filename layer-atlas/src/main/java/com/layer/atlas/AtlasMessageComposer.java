package com.layer.atlas;

import java.util.ArrayList;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

/**
 * @author Oleg Orlov
 * @since 12 May 2015
 */
public class AtlasMessageComposer {
    
    private TextView messageText;
    private View btnSend;
    private View btnUpload;
    
    private Listener listener;
    private Conversation conv;
    private LayerClient layerClient;
    
    private ArrayList<MenuItem> menuItems = new ArrayList<MenuItem>(); 
    
    public AtlasMessageComposer(View rootView, LayerClient client) {
        this.layerClient = client;
        
        btnUpload = rootView.findViewById(R.id.atlas_message_composer_upload);
        btnUpload.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                final PopupWindow popupWindow = new PopupWindow(v.getContext());
                popupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                LayoutInflater inflater = LayoutInflater.from(v.getContext());
                LinearLayout menu = (LinearLayout) inflater.inflate(R.layout.atlas_view_message_composer_menu, null);
                popupWindow.setContentView(menu);
                                
                for (MenuItem item : menuItems) {
                    View itemConvert = inflater.inflate(R.layout.atlas_view_message_composer_menu_convert, menu, false);
                    TextView titleText = ((TextView)itemConvert.findViewById(R.id.altas_view_message_composer_convert_text));
                    titleText.setText(item.title);
                    itemConvert.setTag(item);
                    itemConvert.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            popupWindow.dismiss();
                            MenuItem item = (MenuItem) v.getTag();
                            if (item.clickListener != null) {
                                item.clickListener.onClick(v);
                            }
                        }
                    });
                    menu.addView(itemConvert);
                }
                popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                popupWindow.setOutsideTouchable(true);
                int[] viewXYWindow = new int[2];
                v.getLocationInWindow(viewXYWindow);
                
                menu.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                int menuHeight = menu.getMeasuredHeight();
                popupWindow.showAtLocation(v, Gravity.NO_GRAVITY, viewXYWindow[0], viewXYWindow[1] - menuHeight);
            }
        });
        
        messageText = (TextView) rootView.findViewById(R.id.atlas_message_composer_text);
        
        btnSend = rootView.findViewById(R.id.atlas_message_composer_send);
        btnSend.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                
                String text = messageText.getText().toString();
                
                if (text.trim().length() > 0) {
                    
                    ArrayList<MessagePart> parts = new ArrayList<MessagePart>();
                    String[] lines = text.split("\n+");
                    for (String line : lines) {
                        parts.add(layerClient.newMessagePart(line));
                    }
                    Message msg = layerClient.newMessage(parts);
                    
                    if (listener != null) {
                        boolean proceed = listener.beforeSend(msg);
                        if (!proceed) return;
                    }
                    if (conv == null) return;
                    
                    conv.send(msg);
                    messageText.setText("");
                }
            }
        });
    
    }

    public void registerMenuItem(String title, OnClickListener clickListener) {
        if (title == null) throw new NullPointerException("Item title must not be null");
        MenuItem item = new MenuItem();
        item.title = title;
        item.clickListener = clickListener;
        menuItems.add(item);
        btnUpload.setVisibility(View.VISIBLE);
    }
    
    public void setListener(Listener listener) {
        this.listener = listener;
    }
    
    public Conversation getConv() {
        return conv;
    }

    public void setConversation(Conversation conv) {
        this.conv = conv;
    }

    public interface Listener {
        public abstract boolean beforeSend(Message message);
    }
    
    private static class MenuItem {
        String title;
        OnClickListener clickListener;
    }
}
