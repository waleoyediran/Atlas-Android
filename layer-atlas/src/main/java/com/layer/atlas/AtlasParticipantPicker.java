package com.layer.atlas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.layer.atlas.Atlas.AtlasContactProvider;
import com.layer.atlas.Atlas.Contact;

/**
 * @author Oleg Orlov
 * @since 27 Apr 2015
 */
public class AtlasParticipantPicker {

    private static final String TAG = AtlasParticipantPicker.class.getSimpleName();
    private static final boolean debug = true;

    // participants picker
    private View rootView;
    private EditText textFilter;
    private ListView contactsList;
    private ViewGroup selectedContactsContainer;

    private ArrayList<Contact> selectedContacts = new ArrayList<Contact>();
    private TreeSet<String> skipUserIds = new TreeSet<String>();

    public AtlasParticipantPicker(Context context, View rootView, final AtlasContactProvider contactProvider, String[] userIdToSkip) {

        if (userIdToSkip != null) skipUserIds.addAll(Arrays.asList(userIdToSkip));

        final Contact[] allContacts = contactProvider.contactsMap.values().toArray(new Contact[contactProvider.contactsMap.size()]);
        Arrays.sort(allContacts, Contact.FIRST_LAST_EMAIL_ASCENDING);
        final ArrayList<Contact> contacts = new ArrayList<Contact>();
        for (Contact contact : allContacts) {
            if (skipUserIds.contains(contact.userId)) continue;
            contacts.add(contact);
        }

        // START OF -------------------- Participant Picker ----------------------------------------
        this.rootView = rootView;
        textFilter = (EditText) rootView.findViewById(R.id.atlas_view_participants_picker_text);
        contactsList = (ListView) rootView.findViewById(R.id.atlas_view_participants_picker_list);
        selectedContactsContainer = (ViewGroup) rootView.findViewById(R.id.atlas_view_participants_picker_names);

        if (rootView.getVisibility() == View.VISIBLE) {
            textFilter.requestFocus();
        }

        // log focuses
        final View scroller = rootView.findViewById(R.id.atlas_view_participants_picker_scroll);
        scroller.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (debug) Log.w(TAG, "scroller.onFocusChange() hasFocus: " + hasFocus);
            }
        });
        selectedContactsContainer.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (debug) Log.w(TAG, "names.onFocusChange()    hasFocus: " + hasFocus);
            }
        });

        // If filter.requestFocus is called from .onClickListener - filter receives focus, but
        // NamesLayout receives it immediately after that. So filter lose it.
        // XXX: scroller also receives focus 
        selectedContactsContainer.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (debug) Log.w(TAG, "names.onTouch() event: " + event);
                if (event.getAction() == MotionEvent.ACTION_DOWN) // ACTION_UP never comes if  
                    textFilter.requestFocus(); //   there is no .onClickListener
                return false;
            }
        });

        textFilter.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                View focused = selectedContactsContainer.hasFocus() ? selectedContactsContainer : selectedContactsContainer.findFocus();
                if (debug) Log.w(TAG, "filter.onFocusChange()   hasFocus: " + hasFocus + ", focused: " + focused);
                if (hasFocus) {
                    contactsList.setVisibility(View.VISIBLE);
                }
                v.post(new Runnable() { // check focus runnable
                    @Override
                    public void run() {
                        if (debug) Log.w(TAG, "filter.onFocusChange.run()   filter.focus: " + textFilter.hasFocus());
                        if (debug) Log.w(TAG, "filter.onFocusChange.run()    names.focus: " + selectedContactsContainer.hasFocus());
                        if (debug) Log.w(TAG, "filter.onFocusChange.run() scroller.focus: " + scroller.hasFocus());

                        // check focus is on any descendants and hide list otherwise  
                        View focused = selectedContactsContainer.hasFocus() ? selectedContactsContainer : selectedContactsContainer.findFocus();
                        if (focused == null) {
                            contactsList.setVisibility(View.GONE);
                            textFilter.setText("");
                        }
                    }
                });
            }
        });

        final BaseAdapter contactsAdapter;
        contactsList.setAdapter(contactsAdapter = new BaseAdapter() {
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.atlas_view_participants_picker_convert, parent, false);
                }

                TextView name = (TextView) convertView.findViewById(R.id.atlas_view_participants_picker_convert_name);
                TextView avatarText = (TextView) convertView.findViewById(R.id.atlas_view_participants_picker_convert_ava);
                Contact contact = contacts.get(position);

                name.setText(AtlasContactProvider.getContactFirstAndLast(contact));
                avatarText.setText(AtlasContactProvider.getContactInitials(contact));
                return convertView;
            }

            public long getItemId(int position) {
                return contacts.get(position).userId.hashCode();
            }

            public Object getItem(int position) {
                return contacts.get(position);
            }

            public int getCount() {
                return contacts.size();
            }
        });

        contactsList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Contact contact = contacts.get(position);
                selectedContacts.add(contact);
                refreshParticipants(selectedContacts);
                textFilter.setText("");
                textFilter.requestFocus();
            }

        });

        // track text and filter contact list
        textFilter.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (debug) Log.w(TAG, "beforeTextChanged() s: " + s + " start: " + start + " count: " + count + " after: " + after);
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debug) Log.w(TAG, "onTextChanged()     s: " + s + " start: " + start + " before: " + before + " count: " + count);

                final String filter = s.toString().toLowerCase();
                contacts.clear();
                for (Contact contact : allContacts) {
                    if (selectedContacts.contains(contacts)) continue;
                    if (skipUserIds.contains(contact.userId)) continue;

                    if (contact.firstName != null && contact.firstName.toLowerCase().contains(filter)) {
                        contacts.add(contact);
                        continue;
                    }
                    if (contact.lastName != null && contact.lastName.toLowerCase().contains(filter)) {
                        contacts.add(contact);
                        continue;
                    }
                    if (contact.email != null && contact.email.toLowerCase().contains(filter)) {
                        contacts.add(contact);
                        continue;
                    }
                }
                Collections.sort(contacts, new Contact.FilteringComparator(filter));
                contactsAdapter.notifyDataSetChanged();
            }

            public void afterTextChanged(Editable s) {
                if (debug) Log.w(TAG, "afterTextChanged()  s: " + s);
            }
        });

        // select last added participant when press "Backspace/Del"
        textFilter.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (debug) Log.w(TAG, "onKey() keyCode: " + keyCode + ", event: " + event);
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN && textFilter.getText().length() == 0 && selectedContacts.size() > 0) {

                    selectedContacts.remove(selectedContacts.size() - 1);
                    refreshParticipants(selectedContacts);
                    textFilter.requestFocus();
                }
                return false;
            }
        });
        // END OF ---------------------- Participant Picker ---------------------------------------- 

    }

    public void refreshParticipants(final ArrayList<Contact> selectedContacts) {

        // remove name_converts first. Better to keep editText in place rather than add/remove that force keyboard to blink
        for (int i = selectedContactsContainer.getChildCount() - 1; i >= 0; i--) {
            View child = selectedContactsContainer.getChildAt(i);
            if (child != textFilter) {
                selectedContactsContainer.removeView(child);
            }
        }
        if (debug) Log.w(TAG, "refreshParticipants() childs left: " + selectedContactsContainer.getChildCount());
        for (Contact contactToAdd : selectedContacts) {
            View contactView = LayoutInflater.from(selectedContactsContainer.getContext()).inflate(R.layout.atlas_view_participants_picker_name_convert, selectedContactsContainer, false);

            TextView avaText = (TextView) contactView.findViewById(R.id.atlas_view_participants_picker_name_convert_ava);
            avaText.setText(AtlasContactProvider.getContactInitials(contactToAdd));
            TextView nameText = (TextView) contactView.findViewById(R.id.atlas_view_participants_picker_name_convert_name);
            nameText.setText(AtlasContactProvider.getContactFirstAndLast(contactToAdd));
            contactView.setTag(contactToAdd);

            selectedContactsContainer.addView(contactView, selectedContactsContainer.getChildCount() - 1);
            if (debug) Log.w(TAG, "refreshParticipants() child added: " + contactView + ", for: " + contactToAdd);
        }
        if (selectedContacts.size() == 0) {
            LayoutParams params = new LayoutParams(textFilter.getLayoutParams());
            params.width = LayoutParams.MATCH_PARENT;
        }
        selectedContactsContainer.requestLayout();
    }

    public String[] getSelectedUserIds() {
        String[] userIds = new String[selectedContacts.size()];
        for (int i = 0; i < selectedContacts.size(); i++) {
            userIds[i] = selectedContacts.get(i).userId;
        }
        return userIds;
    }

    public void setVisibility(int visibility) {
        rootView.setVisibility(visibility);
        if (visibility == View.VISIBLE) {
            textFilter.requestFocus();
        }
    }

}
