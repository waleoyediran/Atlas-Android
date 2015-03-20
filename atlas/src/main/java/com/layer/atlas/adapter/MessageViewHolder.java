package com.layer.atlas.adapter;

import android.content.Context;
import android.view.View;

import com.layer.atlas.Participant;
import com.layer.atlas.adapter.BaseViewHolder;
import com.layer.sdk.messaging.Message;

/**
 * Created by Steven Jones on 3/14/2015.
 * Copyright (c) 2015 Layer, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public abstract class MessageViewHolder extends BaseViewHolder<Message> {
    protected MessageViewHolder(View itemView) {
        super(itemView);
    }

    /**
     * Tells the implementer to display a message.
     */
    public abstract void setMessage(Context context, Message message);

    /**
     * Informs the implementer of its sender.
     */
    public abstract void setMessageSender(Context context, Participant participant);

    /**
     * A boolean to determine whether or not the implementer should display an avatar item.
     */
    public abstract void setMessageAvatarItemVisible(Context context, boolean visible);
}
