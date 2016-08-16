package com.layer.atlas;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.layer.atlas.util.AvatarStyle;
import com.layer.atlas.util.EditTextUtil;
import com.layer.atlas.util.IdentityDisplayNameComparator;
import com.layer.atlas.util.views.EmptyDelEditText;
import com.layer.atlas.util.views.FlowLayout;
import com.layer.atlas.util.views.MaxHeightScrollView;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Identity;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.RecyclerViewController;
import com.layer.sdk.query.SortDescriptor;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AtlasAddressBar extends LinearLayout {
    private LayerClient mLayerClient;
    private Picasso mPicasso;

    private OnConversationClickListener mOnConversationClickListener;
    private OnParticipantSelectionChangeListener mOnParticipantSelectionChangeListener;

    private FlowLayout mSelectedParticipantLayout;
    private EmptyDelEditText mFilter;
    private RecyclerView mParticipantList;
    private AvailableConversationAdapter mAvailableConversationAdapter;
    private final Set<Identity> mSelectedParticipants = new LinkedHashSet<>();

    private boolean mShowConversations;

    // styles
    private int mInputTextSize;
    private int mInputTextColor;
    private Typeface mInputTextTypeface;
    private int mInputTextStyle;
    private int mInputUnderlineColor;
    private int mInputCursorColor;
    private int mListTextSize;
    private int mListTextColor;
    private Typeface mListTextTypeface;
    private int mListTextStyle;
    private Typeface mChipTypeface;
    private int mChipStyle;
    private AvatarStyle mAvatarStyle;

    public AtlasAddressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AtlasAddressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        parseStyle(context, attrs, defStyleAttr);

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.atlas_address_bar, this, true);
        mSelectedParticipantLayout = (FlowLayout) findViewById(R.id.selected_participant_group);
        mFilter = (EmptyDelEditText) findViewById(R.id.filter);
        mSelectedParticipantLayout.setStretchChild(mFilter);
        mParticipantList = (RecyclerView) findViewById(R.id.participant_list);
        ((MaxHeightScrollView) findViewById(R.id.selected_participant_scroll)).setMaximumHeight(getResources().getDimensionPixelSize(R.dimen.atlas_selected_participant_group_max_height));
        setOrientation(VERTICAL);
    }

    public AtlasAddressBar init(LayerClient layerClient, Picasso picasso) {
        mLayerClient = layerClient;
        mPicasso = picasso;

        RecyclerView.LayoutManager manager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mParticipantList.setLayoutManager(manager);
        mAvailableConversationAdapter = new AvailableConversationAdapter(mLayerClient, mPicasso);

        applyStyle();

        mParticipantList.setAdapter(mAvailableConversationAdapter);

        // Hitting backspace with an empty search string deletes the last selected participant
        mFilter.setOnEmptyDelListener(new EmptyDelEditText.OnEmptyDelListener() {
            @Override
            public boolean onEmptyDel(EmptyDelEditText editText) {
                removeLastSelectedParticipant();
                return true;
            }
        });

        // Refresh available participants and conversations with every search string change
        mFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable e) {
                refresh();
            }
        });
        return this;
    }

    public AtlasAddressBar addTextChangedListener(TextWatcher textWatcher) {
        mFilter.addTextChangedListener(textWatcher);
        return this;
    }

    public AtlasAddressBar removeTextChangedListener(TextWatcher textWatcher) {
        mFilter.removeTextChangedListener(textWatcher);
        return this;
    }

    public AtlasAddressBar setOnEditorActionListener(TextView.OnEditorActionListener listener) {
        mFilter.setOnEditorActionListener(listener);
        return this;
    }

    public AtlasAddressBar setOnConversationClickListener(OnConversationClickListener onConversationClickListener) {
        mOnConversationClickListener = onConversationClickListener;
        return this;
    }

    public AtlasAddressBar setOnParticipantSelectionChangeListener(OnParticipantSelectionChangeListener onParticipantSelectionChangeListener) {
        mOnParticipantSelectionChangeListener = onParticipantSelectionChangeListener;
        return this;
    }

    public AtlasAddressBar setSuggestionsVisibility(int visibility) {
        mParticipantList.setVisibility(visibility);
        return this;
    }

    public AtlasAddressBar setTypeface(Typeface inputTextTypeface, Typeface listTextTypeface,
                                       Typeface avatarTextTypeface, Typeface chipTypeface) {
        this.mInputTextTypeface = inputTextTypeface;
        this.mListTextTypeface = listTextTypeface;
        this.mChipTypeface = chipTypeface;
        this.mAvatarStyle.setAvatarTextTypeface(avatarTextTypeface);
        applyTypeface();
        return this;
    }

    public Set<Identity> getSelectedParticipants() {
        return new LinkedHashSet<>(mSelectedParticipants);
    }

    public AtlasAddressBar refresh() {
        if (mAvailableConversationAdapter == null) return this;
        mAvailableConversationAdapter.refresh(getSearchFilter(), mSelectedParticipants);
        return this;
    }

    public AtlasAddressBar setShowConversations(boolean showConversations) {
        this.mShowConversations = showConversations;
        return this;
    }

    public AtlasAddressBar setSelectedParticipants(Set<Identity> selectedParticipants) {
        mSelectedParticipants.addAll(selectedParticipants);
        return this;
    }

    public void requestFilterFocus() {
        mFilter.requestFocus();
    }

    private boolean selectParticipant(Identity participant) {
        if (mSelectedParticipants.contains(participant)) return true;
        if (mSelectedParticipants.size() >= 24) return false;
        mSelectedParticipants.add(participant);
        ParticipantChip chip = new ParticipantChip(getContext(), participant, mPicasso);
        mSelectedParticipantLayout.addView(chip, mSelectedParticipantLayout.getChildCount() - 1);
        mFilter.setText(null);
        refresh();
        if (mOnParticipantSelectionChangeListener != null) {
            mOnParticipantSelectionChangeListener.onParticipantSelectionChanged(this, new ArrayList<>(mSelectedParticipants));
        }
        return true;
    }

    private void unselectParticipant(ParticipantChip chip) {
        if (!mSelectedParticipants.contains(chip.mParticipant)) return;
        mSelectedParticipants.remove(chip.mParticipant);
        mSelectedParticipantLayout.removeView(chip);
        refresh();
        if (mOnParticipantSelectionChangeListener != null) {
            mOnParticipantSelectionChangeListener.onParticipantSelectionChanged(this, new ArrayList<>(mSelectedParticipants));
        }
    }

    private String getSearchFilter() {
        String s = mFilter.getText().toString();
        return s.trim().isEmpty() ? null : s;
    }

    private Identity removeLastSelectedParticipant() {
        ParticipantChip lastChip = null;
        for (int i = 0; i < mSelectedParticipantLayout.getChildCount(); i++) {
            View v = mSelectedParticipantLayout.getChildAt(i);
            if (v instanceof ParticipantChip) lastChip = (ParticipantChip) v;
        }
        if (lastChip == null) return null;
        unselectParticipant(lastChip);
        return lastChip.mParticipant;
    }

    private void parseStyle(Context context, AttributeSet attrs, int defStyle) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AtlasAddressBar, R.attr.AtlasAddressBar, defStyle);
        Resources resources = context.getResources();
        this.mInputTextSize = ta.getDimensionPixelSize(R.styleable.AtlasAddressBar_inputTextSize, resources.getDimensionPixelSize(R.dimen.atlas_text_size_input));
        this.mInputTextColor = ta.getColor(R.styleable.AtlasAddressBar_inputTextColor, resources.getColor(R.color.atlas_text_black));
        this.mInputTextStyle = ta.getInt(R.styleable.AtlasAddressBar_inputTextStyle, Typeface.NORMAL);
        String inputTextTypefaceName = ta.getString(R.styleable.AtlasAddressBar_inputTextTypeface);
        this.mInputTextTypeface = inputTextTypefaceName != null ? Typeface.create(inputTextTypefaceName, mInputTextStyle) : null;
        this.mInputCursorColor = ta.getColor(R.styleable.AtlasAddressBar_inputCursorColor, resources.getColor(R.color.atlas_color_primary_blue));
        this.mInputUnderlineColor = ta.getColor(R.styleable.AtlasAddressBar_inputUnderlineColor, resources.getColor(R.color.atlas_color_primary_blue));

        this.mListTextSize = ta.getDimensionPixelSize(R.styleable.AtlasAddressBar_listTextSize, resources.getDimensionPixelSize(R.dimen.atlas_text_size_secondary_item));
        this.mListTextColor = ta.getColor(R.styleable.AtlasAddressBar_listTextColor, resources.getColor(R.color.atlas_text_black));
        this.mListTextStyle = ta.getInt(R.styleable.AtlasAddressBar_listTextStyle, Typeface.NORMAL);
        String listTextTypefaceName = ta.getString(R.styleable.AtlasAddressBar_listTextTypeface);
        this.mListTextTypeface = listTextTypefaceName != null ? Typeface.create(listTextTypefaceName, mInputTextStyle) : null;

        this.mChipStyle = ta.getInt(R.styleable.AtlasAddressBar_chipStyle, Typeface.NORMAL);
        String chipTypefaceName = ta.getString(R.styleable.AtlasAddressBar_chipTypeface);
        this.mChipTypeface = chipTypefaceName != null ? Typeface.create(chipTypefaceName, mChipStyle) : null;

        AvatarStyle.Builder avatarStyleBuilder = new AvatarStyle.Builder();
        avatarStyleBuilder.avatarBackgroundColor(ta.getColor(R.styleable.AtlasAddressBar_avatarBackgroundColor, resources.getColor(R.color.atlas_avatar_background)));
        avatarStyleBuilder.avatarTextColor(ta.getColor(R.styleable.AtlasAddressBar_avatarTextColor, resources.getColor(R.color.atlas_avatar_text)));
        avatarStyleBuilder.avatarBorderColor(ta.getColor(R.styleable.AtlasAddressBar_avatarBorderColor, resources.getColor(R.color.atlas_avatar_border)));
        int avatarTextStyle = ta.getInt(R.styleable.AtlasAddressBar_avatarTextStyle, Typeface.NORMAL);
        String avatarTextTypefaceName = ta.getString(R.styleable.AtlasAddressBar_avatarTextTypeface);
        avatarStyleBuilder.avatarTextTypeface(inputTextTypefaceName != null ? Typeface.create(avatarTextTypefaceName, avatarTextStyle) : null);
        this.mAvatarStyle = avatarStyleBuilder.build();

        ta.recycle();
    }

    private void applyStyle() {
        mFilter.setTextColor(mInputTextColor);
        mFilter.setTextSize(TypedValue.COMPLEX_UNIT_PX, mInputTextSize);
        EditTextUtil.setCursorDrawableColor(mFilter, mInputCursorColor);
        EditTextUtil.setUnderlineColor(mFilter, mInputUnderlineColor);
        applyTypeface();
    }

    private void applyTypeface() {
        mFilter.setTypeface(mInputTextTypeface, mInputTextStyle);
        mAvailableConversationAdapter.notifyDataSetChanged();
    }

    /**
     * Automatically refresh on resume
     */
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != View.VISIBLE) return;
        refresh();
    }

    /**
     * Save selected participants
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (mSelectedParticipants.isEmpty()) return superState;
        SavedState savedState = new SavedState(superState);
        // TODO save off selected participants
//        savedState.mSelectedParticipantIds = new ArrayList<>(mSelectedParticipants);
        return savedState;
    }

    /**
     * Restore selected participants
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        // TODO restore selected participants
//        if (savedState.mSelectedParticipantIds != null) {
//            mSelectedParticipants.clear();
//            for (String participantId : savedState.mSelectedParticipantIds) {
//                selectParticipant(participantId);
//            }
//        }
    }

    private static class SavedState extends BaseSavedState {
        List<String> mSelectedParticipantIds;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeStringList(mSelectedParticipantIds);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        private SavedState(Parcel in) {
            super(in);
            mSelectedParticipantIds = in.createStringArrayList();
        }
    }

    /**
     * ParticipantChip implements the View used to populate the selected participant FlowLayout.
     */
    private class ParticipantChip extends LinearLayout {
        private Identity mParticipant;

        private AtlasAvatar mAvatar;
        private TextView mName;
        private ImageView mRemove;

        public ParticipantChip(Context context, Identity participant, Picasso picasso) {
            super(context);
            LayoutInflater inflater = LayoutInflater.from(context);
            Resources r = getContext().getResources();
            mParticipant = participant;

            // Inflate and cache views
            inflater.inflate(R.layout.atlas_participant_chip, this, true);
            mAvatar = (AtlasAvatar) findViewById(R.id.avatar);
            mName = (TextView) findViewById(R.id.name);
            mRemove = (ImageView) findViewById(R.id.remove);

            // Set Style
            mName.setTypeface(mChipTypeface);

            // Set layout
            int height = r.getDimensionPixelSize(R.dimen.atlas_chip_height);
            int margin = r.getDimensionPixelSize(R.dimen.atlas_chip_margin);
            FlowLayout.LayoutParams p = new FlowLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height);
            p.setMargins(margin, margin, margin, margin);
            setLayoutParams(p);
            setOrientation(HORIZONTAL);
            setBackgroundDrawable(r.getDrawable(R.drawable.atlas_participant_chip_background));

            // Initialize participant data
            mName.setText(participant.getDisplayName());
            mAvatar.init(picasso)
                    .setStyle(mAvatarStyle)
                    .setParticipants(participant);

            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    unselectParticipant(ParticipantChip.this);
                }
            });
        }
    }

    private enum Type {
        PARTICIPANT,
        CONVERSATION
    }

    /**
     * AvailableConversationAdapter provides items for individual Participants and existing
     * Conversations.  Items are filtered by a participant filter string and by a set of selected
     * Participants.
     */
    private class AvailableConversationAdapter extends RecyclerView.Adapter<AvailableConversationAdapter.ViewHolder> implements RecyclerViewController.Callback {
        private final LayerClient mLayerClient;
        private final Picasso mPicasso;
        private final RecyclerViewController<Conversation> mQueryController;

        private final List<Identity> mParticipants = new ArrayList<>();
        private final List<Identity> mAllParticipants = new ArrayList<>();

        public AvailableConversationAdapter(LayerClient client, Picasso picasso) {
            this(client, picasso, null);
        }

        public AvailableConversationAdapter(LayerClient client, Picasso picasso, Collection<String> updateAttributes) {
            mQueryController = client.newRecyclerViewController(null, updateAttributes, this);
            mLayerClient = client;
            mPicasso = picasso;
            setHasStableIds(false);

            initIdentities();
        }

        private void initIdentities() {
            Identity currentUser = mLayerClient.getAuthenticatedUser();
            Query.Builder<Identity> builder = Query.builder(Identity.class);
            if (currentUser != null) {
                builder.predicate(new Predicate(Identity.Property.USER_ID, Predicate.Operator.NOT_EQUAL_TO, currentUser.getUserId()));
            }
            final Query<Identity> identitiesQuery = builder.build();

            new AsyncTask<Void, Void, List<Identity>>() {

                @Override
                protected List<Identity> doInBackground(Void... params) {
                    return mLayerClient.executeQuery(identitiesQuery, Query.ResultType.OBJECTS);
                }

                @Override
                protected void onPostExecute(List<Identity> identities) {
                    mAllParticipants.clear();
                    mAllParticipants.addAll(identities);
                    AtlasAddressBar.this.refresh();
                }
            }.execute();
        }

        /**
         * Refreshes this adapter by filtering Conversations to return only those Conversations with
         * the given set of selected Participants.
         */
        public void refresh(String filter, Set<Identity> selectedParticipants) {
            // Apply text search filter to available participants
            Set<Identity> filteredIdentities = filter(filter);

            // Don't show participants we've already selected
            for (Identity selectedParticipant : selectedParticipants) {
                filteredIdentities.remove(selectedParticipant);
            }
            mParticipants.clear();
            mParticipants.addAll(filteredIdentities);
            Collections.sort(mParticipants, new IdentityDisplayNameComparator());

            // TODO: compute add/remove/move and notify those instead of complete notify
            notifyDataSetChanged();

            if (mShowConversations) {
                queryConversations(selectedParticipants);
            }
        }

        private Set<Identity> filter(String filter) {
            // With no filter, return all Participants
            if (filter == null) {
                return new HashSet<>(mAllParticipants);
            }

            Set<Identity> result = new HashSet<>();
            // Filter participants by substring matching first- and last- names
            filter = filter.toLowerCase();
            for (Identity participant : mAllParticipants) {
                boolean matches = false;
                if (participant.getDisplayName() != null && participant.getDisplayName().toLowerCase().contains(filter))
                    matches = true;
                if (matches) {
                    result.add(participant);
                }
            }
            return result;
        }

        private void queryConversations(Set<Identity> selectedParticipants) {
            // Filter down to only those conversations including the selected participants, hiding one-on-one conversations
            Query.Builder<Conversation> builder = Query.builder(Conversation.class)
                    .sortDescriptor(new SortDescriptor(Conversation.Property.LAST_MESSAGE_SENT_AT, SortDescriptor.Order.DESCENDING));
            if (selectedParticipants.isEmpty()) {
                builder.predicate(new Predicate(Conversation.Property.PARTICIPANT_COUNT, Predicate.Operator.GREATER_THAN, 2));
            } else {
                // TODO - This needs to support identities
//                builder.predicate(new CompoundPredicate(CompoundPredicate.Type.AND,
//                        new Predicate(Conversation.Property.PARTICIPANT_COUNT, Predicate.Operator.GREATER_THAN, 2),
//                        new Predicate(Conversation.Property.PARTICIPANTS, Predicate.Operator.IN, selectedParticipants)));
            }
            mQueryController.setQuery(builder.build()).execute();
        }


        //==============================================================================================
        // Adapter
        //==============================================================================================

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ViewHolder viewHolder = new ViewHolder(parent);
            viewHolder.mAvatar
                    .init(mPicasso)
                    .setStyle(mAvatarStyle);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            synchronized (mParticipants) {
                switch (getType(position)) {
                    case PARTICIPANT: {
                        position = adapterPositionToParticipantPosition(position);
                        Identity participant = mParticipants.get(position);
                        viewHolder.mTitle.setText(participant.getDisplayName());
                        viewHolder.itemView.setTag(participant);
                        viewHolder.itemView.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                selectParticipant((Identity) v.getTag());
                            }
                        });
                        viewHolder.mAvatar.setParticipants(participant);
                    }
                    break;

                    case CONVERSATION: {
                        position = adapterPositionToConversationPosition(position);
                        Conversation conversation = mQueryController.getItem(position);
                        Identity user = mLayerClient.getAuthenticatedUser();
                        List<String> names = new ArrayList<>();
                        Set<Identity> participants = conversation.getParticipants();
                        participants.remove(user);
                        for (Identity participant : participants) {
                            names.add(participant.getDisplayName());
                        }
                        viewHolder.mTitle.setText(TextUtils.join(", ", names));
                        viewHolder.itemView.setTag(conversation);
                        viewHolder.itemView.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (mOnConversationClickListener == null) return;
                                mOnConversationClickListener.onConversationClick(AtlasAddressBar.this, (Conversation) v.getTag());
                            }
                        });
                        viewHolder.mAvatar.setParticipants(participants);
                    }
                    break;
                }
            }
        }

        // first are participants; then are conversations
        Type getType(int position) {
            synchronized (mParticipants) {
                return (position < mParticipants.size()) ? Type.PARTICIPANT : Type.CONVERSATION;
            }
        }

        int adapterPositionToParticipantPosition(int position) {
            return position;
        }

        int adapterPositionToConversationPosition(int position) {
            synchronized (mParticipants) {
                return position - mParticipants.size();
            }
        }

        int conversationPositionToAdapterPosition(int position) {
            synchronized (mParticipants) {
                return position + mParticipants.size();
            }
        }

        @Override
        public int getItemCount() {
            synchronized (mParticipants) {
                return mQueryController.getItemCount() + mParticipants.size();
            }
        }


        //==============================================================================================
        // Conversation UI update callbacks
        //==============================================================================================

        @Override
        public void onQueryDataSetChanged(RecyclerViewController controller) {
            notifyDataSetChanged();
        }

        @Override
        public void onQueryItemChanged(RecyclerViewController controller, int position) {
            notifyItemChanged(conversationPositionToAdapterPosition(position));
        }

        @Override
        public void onQueryItemRangeChanged(RecyclerViewController controller, int positionStart, int itemCount) {
            notifyItemRangeChanged(conversationPositionToAdapterPosition(positionStart), itemCount);
        }

        @Override
        public void onQueryItemInserted(RecyclerViewController controller, int position) {
            notifyItemInserted(conversationPositionToAdapterPosition(position));
        }

        @Override
        public void onQueryItemRangeInserted(RecyclerViewController controller, int positionStart, int itemCount) {
            notifyItemRangeInserted(conversationPositionToAdapterPosition(positionStart), itemCount);
        }

        @Override
        public void onQueryItemRemoved(RecyclerViewController controller, int position) {
            notifyItemRemoved(conversationPositionToAdapterPosition(position));
        }

        @Override
        public void onQueryItemRangeRemoved(RecyclerViewController controller, int positionStart, int itemCount) {
            notifyItemRangeRemoved(conversationPositionToAdapterPosition(positionStart), itemCount);
        }

        @Override
        public void onQueryItemMoved(RecyclerViewController controller, int fromPosition, int toPosition) {
            notifyItemMoved(conversationPositionToAdapterPosition(fromPosition), conversationPositionToAdapterPosition(toPosition));
        }


        //==============================================================================================
        // Inner classes
        //==============================================================================================

        protected class ViewHolder extends RecyclerView.ViewHolder {
            private AtlasAvatar mAvatar;
            private TextView mTitle;

            public ViewHolder(ViewGroup parent) {
                super(LayoutInflater.from(parent.getContext()).inflate(R.layout.atlas_address_bar_item, parent, false));
                mAvatar = (AtlasAvatar) itemView.findViewById(R.id.avatar);
                mTitle = (TextView) itemView.findViewById(R.id.title);
                mTitle.setTextColor(mListTextColor);
                mTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, mListTextSize);
                mTitle.setTypeface(mListTextTypeface, mListTextStyle);
            }
        }
    }


    public interface OnConversationClickListener {
        void onConversationClick(AtlasAddressBar conversationLauncher, Conversation conversation);
    }

    public interface OnParticipantSelectionChangeListener {
        void onParticipantSelectionChanged(AtlasAddressBar conversationLauncher, List<Identity> participants);
    }
}
