package com.layer.atlas.adapter;

import android.view.View;

import com.layer.atlas.Participant;
import com.layer.atlas.adapter.BaseViewHolder;

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
public abstract class ParticipantViewHolder extends BaseViewHolder<Participant> {
    public static enum SortType {
        FIRST_NAME,
        LAST_NAME
    }

    protected ParticipantViewHolder(View itemView) {
        super(itemView);
    }

    public abstract void setParticipant(Participant participant);

    public abstract void setParticipantSortType(SortType sortType);

    public abstract void setParticipantAvatarItemVisible(boolean visible);
}
