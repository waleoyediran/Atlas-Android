package com.layer.atlas.adapter;

import android.content.Context;
import android.view.View;

import com.layer.atlas.AvatarItem;
import com.layer.sdk.messaging.Conversation;

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
public abstract class ConversationViewHolder extends BaseViewHolder<Conversation> {
    public ConversationViewHolder(View itemView) {
        super(itemView);
    }

    /**
     * Sets this ViewHolder's Layer conversation.
     *
     * @param conversation The conversation to present.
     */
    public abstract void setConversation(Context context, Conversation conversation);

    /**
     * Sets this ViewHolder's conversation title.
     *
     * @param title The conversation title to display.
     */
    public abstract void setConversationTitle(Context context, String title);

    /**
     * Sets this ViewHolder's `AvatarItem`.
     *
     * @param avatarItem The object implementing the `AvatarItem` interface.
     */
    public abstract void setConversationAvatarItem(Context context, AvatarItem avatarItem);
}
