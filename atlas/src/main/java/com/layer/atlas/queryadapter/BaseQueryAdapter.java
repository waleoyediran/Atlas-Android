package com.layer.atlas.queryadapter;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.layer.atlas.viewholder.BaseViewHolder;
import com.layer.atlas.viewholder.BaseViewHolderFactory;
import com.layer.atlas.viewholder.InteractionListener;
import com.layer.sdk.LayerClient;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.Queryable;
import com.layer.sdk.query.RecyclerViewController;

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
public abstract class BaseQueryAdapter<Tquery extends Queryable, Tview extends BaseViewHolder<Tquery>, Tfactory extends BaseViewHolderFactory<Tquery, Tview>>
        extends RecyclerView.Adapter<Tview>
        implements RecyclerViewController.Callback, InteractionListener<Tquery> {

    private final Tfactory mFactory;
    private final RecyclerViewController<Tquery> mRecyclerViewController;

    public BaseQueryAdapter(LayerClient client, Query<Tquery> query, Tfactory factory) {
        mFactory = factory;
        mRecyclerViewController = client.newRecyclerViewController(query, null, this);
        setHasStableIds(false);
    }

    public void refresh() {
        mRecyclerViewController.execute();
    }

    @Override
    public int getItemCount() {
        return mRecyclerViewController.getItemCount();
    }

    public int getPosition(Tquery queryable) {
        return mRecyclerViewController.getPosition(queryable);
    }


    //==============================================================================================
    // ViewHolder
    //==============================================================================================

    public abstract void onBindViewHolder(Tview viewHolder, Tquery queryable);

    @Override
    public void onBindViewHolder(Tview viewHolder, int position) {
        Tquery item = mRecyclerViewController.getItem(position);
        viewHolder.setInteractionTarget(item);
        onBindViewHolder(viewHolder, item);
    }


    //==============================================================================================
    // ViewHolderFactory
    //==============================================================================================

    @Override
    public Tview onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Tview viewHolder = mFactory.createViewHolder(viewGroup, viewType);
        viewHolder.setInteractionListener(this);
        return viewHolder;
    }

    @Override
    public int getItemViewType(int position) {
        return mFactory.getViewType(mRecyclerViewController.getItem(position));
    }


    //==============================================================================================
    // InteractionListener
    //==============================================================================================

    /**
     * Optionally override to handle interactions.
     *
     * @param target          The Tquery
     * @param interactionType
     */
    @Override
    public void onInteraction(Tquery target, InteractionType interactionType) {
    }


    //==============================================================================================
    // Adapter notifiers
    //==============================================================================================

    @Override
    public void onQueryDataSetChanged(RecyclerViewController controller) {
        notifyDataSetChanged();
    }

    @Override
    public void onQueryItemChanged(RecyclerViewController controller, int position) {
        notifyItemChanged(position);
    }

    @Override
    public void onQueryItemRangeChanged(RecyclerViewController controller, int positionStart, int itemCount) {
        notifyItemRangeChanged(positionStart, itemCount);
    }

    @Override
    public void onQueryItemInserted(RecyclerViewController controller, int position) {
        notifyItemInserted(position);
    }

    @Override
    public void onQueryItemRangeInserted(RecyclerViewController controller, int positionStart, int itemCount) {
        notifyItemRangeInserted(positionStart, itemCount);
    }

    @Override
    public void onQueryItemRemoved(RecyclerViewController controller, int position) {
        notifyItemRemoved(position);
    }

    @Override
    public void onQueryItemRangeRemoved(RecyclerViewController controller, int positionStart, int itemCount) {
        notifyItemRangeRemoved(positionStart, itemCount);
    }

    @Override
    public void onQueryItemMoved(RecyclerViewController controller, int fromPosition, int toPosition) {
        notifyItemMoved(fromPosition, toPosition);
    }
}