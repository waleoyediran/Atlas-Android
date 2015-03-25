package com.layer.atlas.atlasdefault;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.layer.atlas.model.Participant;
import com.layer.atlas.R;
import com.layer.atlas.viewholder.MessageViewHolder;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

import java.util.ArrayList;
import java.util.List;

public class DefaultMessageViewHolder extends MessageViewHolder {
    private final TextView mMessageTextView;

    public DefaultMessageViewHolder(View itemView) {
        super(itemView);
        mMessageTextView = (TextView) itemView.findViewById(R.id.atlas_message);
    }

    @Override
    public void setMessage(Context context, Message message) {
        List<String> strings = new ArrayList<String>();
        for (MessagePart messagePart : message.getMessageParts()) {
            if (messagePart.getMimeType().startsWith("text/")) {
                strings.add(new String(messagePart.getData()));
            } else {
                strings.add(messagePart.getMimeType());
            }
        }
        mMessageTextView.setText(TextUtils.join("\n", strings));
    }

    @Override
    public void setMessageSender(Context context, Participant participant) {

    }

    @Override
    public void setMessageAvatarItemVisible(Context context, boolean visible) {

    }
}
