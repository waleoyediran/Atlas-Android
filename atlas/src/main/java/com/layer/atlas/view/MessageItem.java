package com.layer.atlas.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.layer.atlas.R;
import com.layer.atlas.drawable.MessageBubbleDrawable;

public class MessageItem extends RelativeLayout {
    private final static int DEF_STYLE = R.attr.defaultMessageItem;

    private ImageView mBubbleArrow;
    private TextView mTextView;

    public MessageItem(Context context) {
        this(context, null, DEF_STYLE);
    }

    public MessageItem(Context context, AttributeSet attrs) {
        this(context, attrs, DEF_STYLE);
    }

    public MessageItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        View.inflate(context, R.layout.atlas_item_message, this);

        mBubbleArrow = (ImageView) findViewById(R.id.atlas_message_bubble_arrow);
        mTextView = (TextView) findViewById(R.id.atlas_message);

        // Try populating attributes from the layout xml
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AtlasMessageItem, defStyleAttr, R.style.AtlasMessageItem);
        try {
            Resources r = context.getResources();
            int backgroundColor = a.getColor(R.styleable.AtlasMessageItem_messageBubbleBackgroundColor, r.getColor(R.color.atlas_message_bubble_background));
            int textColor = a.getColor(R.styleable.AtlasMessageItem_messageBubbleTextColor, r.getColor(R.color.atlas_message_bubble_text));
            float radius = a.getDimension(R.styleable.AtlasMessageItem_messageBubbleCornerRadius, r.getDimension(R.dimen.atlas_message_bubble_corner_radius));
            float paddingLeft = a.getDimension(R.styleable.AtlasMessageItem_messageBubblePaddingLeft, r.getDimension(R.dimen.atlas_message_bubble_padding_left));
            float paddingRight = a.getDimension(R.styleable.AtlasMessageItem_messageBubblePaddingRight, r.getDimension(R.dimen.atlas_message_bubble_padding_right));
            float paddingTop = a.getDimension(R.styleable.AtlasMessageItem_messageBubblePaddingTop, r.getDimension(R.dimen.atlas_message_bubble_padding_top));
            float paddingBottom = a.getDimension(R.styleable.AtlasMessageItem_messageBubblePaddingBottom, r.getDimension(R.dimen.atlas_message_bubble_padding_bottom));
            Drawable bubbleArrow = a.getDrawable(R.styleable.AtlasMessageItem_messageBubbleArrow);
            setAttributes(backgroundColor, textColor, radius, paddingLeft, paddingRight, paddingTop, paddingBottom, bubbleArrow);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            a.recycle();
        }
    }

    public void setAttributes(int backgroundColor, int textColor, float bubbleRadius, float paddingLeft, float paddingRight, float paddingTop, float paddingBottom, Drawable arrowDrawable) {
        mBubbleArrow.setImageDrawable(arrowDrawable);
        mBubbleArrow.setColorFilter(backgroundColor);
        mBubbleArrow.setPadding(0, 0, 0, (int) bubbleRadius);
        mTextView.setPadding((int) paddingLeft, (int) paddingRight, (int) paddingTop, (int) paddingBottom);
        mTextView.setBackgroundDrawable(MessageBubbleDrawable.with(bubbleRadius, backgroundColor));
        mTextView.setTextColor(textColor);
    }

}
