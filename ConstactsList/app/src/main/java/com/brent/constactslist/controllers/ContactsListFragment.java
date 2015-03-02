package com.brent.constactslist.controllers;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;


import com.brent.constactslist.model.Contact;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.ArrayList;
import java.util.List;

/**
 * The contacts list fragment.
 * <p/>
 * <p/>
 * Activities containing this fragment MUST implement the
 * {@link ContactsListFragment.SendDetailsListener} interface.
 */
public class ContactsListFragment extends ListFragment {

    private SendDetailsListener mListener;
    private List<Contact> mContacts = new ArrayList<>();
    static private final String STATE_CHECKED = "state_checked";

    public ContactsListFragment() {
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // Get the contacts
        String endpoint = "https://solstice.applauncher.com/external/contacts.json";
        Ion.with(getActivity())
                .load(endpoint)
                .as(new TypeToken<List<Contact>>() {
                })
                .setCallback(new FutureCallback<List<Contact>>() {
                    @Override
                    public void onCompleted(Exception e, List<Contact> contacts) {
                        if (e == null && contacts != null) {
                            mContacts = contacts;
                            setListAdapter(new ContactAdapter(getActivity(), contacts));
                            if (savedInstanceState != null) {
                                int position = savedInstanceState.getInt(STATE_CHECKED, -1);

                                if (position > -1) {
                                    getListView().setItemChecked(position, true);
                                    getListView().setSelection(position);
                                    if (null != mListener) {
                                        mListener.sendDetails(mContacts.get(position), false);
                                    }
                                }
                            }
                        }
                    }
                });
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (SendDetailsListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SendDetailsListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);

        state.putInt(STATE_CHECKED, getListView().getCheckedItemPosition());
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        l.setItemChecked(position, true);
        if (null != mListener) {
            mListener.sendDetails(mContacts.get(position), true);
        }
    }

    public interface SendDetailsListener {
        public void sendDetails(Contact contact, boolean closePane);
    }

}
