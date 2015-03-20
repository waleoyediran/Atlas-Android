package com.layer.atlas.atlasdefault;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.layer.atlas.AvatarItem;
import com.layer.atlas.R;
import com.layer.atlas.viewholder.ConversationViewHolder;
import com.layer.sdk.messaging.Conversation;

public class DefaultConversationViewHolder extends ConversationViewHolder {
    private final ImageView mAvatarView;
    private final TextView mTitleTextView;

    public DefaultConversationViewHolder(View itemView) {
        super(itemView);
        mAvatarView = (ImageView) itemView.findViewById(R.id.atlas_avatar);
        mTitleTextView = (TextView) itemView.findViewById(R.id.atlas_title);
    }

    @Override
    public void setConversation(Context context, Conversation conversation) {
    }

    @Override
    public void setConversationTitle(Context context, String title) {
        mTitleTextView.setText(title);
    }

    @Override
    public void setConversationAvatarItem(Context context, AvatarItem avatarItem) {
        Bitmap bitmap = avatarItem.getAvatarBitmap();
        if (bitmap != null) {
            mAvatarView.setImageDrawable(null);
            mAvatarView.setImageBitmap(bitmap);
        } else {
            mAvatarView.setImageBitmap(null);
            mAvatarView.setImageDrawable(initialsToDrawable(context, avatarItem.getAvatarInitials()));
        }
    }

    private Drawable initialsToDrawable(Context context, String initials) {
        return context.getResources().getDrawable(R.drawable.atlas_avatar);
    }
}
