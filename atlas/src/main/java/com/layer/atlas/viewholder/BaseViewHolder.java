package com.layer.atlas.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;

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
public abstract class BaseViewHolder<Titem> extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
    private Titem mInteractionTarget;
    private InteractionListener<Titem> mListener;

    public BaseViewHolder(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
    }

    /**
     * Allows this ViewHolder to report user interactions.
     *
     * @param listener The listener to report user interactions.
     */
    public void setInteractionListener(InteractionListener<Titem> listener) {
        mListener = listener;
    }

    /**
     * Allows this ViewHolder to report the target that was interacted with to its listener.
     *
     * @param target The object to report as the target of interactions.
     */
    public void setInteractionTarget(Titem target) {
        mInteractionTarget = target;
    }

    @Override
    public void onClick(View v) {
        mListener.onInteraction(mInteractionTarget, InteractionListener.InteractionType.SHORT_CLICK);
    }

    @Override
    public boolean onLongClick(View v) {
        mListener.onInteraction(mInteractionTarget, InteractionListener.InteractionType.LONG_CLICK);
        return true;
    }
}
