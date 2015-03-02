package com.brent.constactslist.controllers;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SlidingPaneLayout;

import com.brent.constactslist.R;
import com.brent.constactslist.controllers.ContactsListFragment;
import com.brent.constactslist.controllers.DetailsFragment;
import com.brent.constactslist.model.Contact;


public class MainActivity extends FragmentActivity implements ContactsListFragment.SendDetailsListener {

    private DetailsFragment detailsFragment = null;
    private SlidingPaneLayout pane = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        detailsFragment =
                (DetailsFragment)getSupportFragmentManager().findFragmentById(R.id.details);
        pane = (SlidingPaneLayout)findViewById(R.id.pane);
        pane.openPane();
    }

    @Override
    public void onBackPressed() {
        if (pane.isOpen()) {
            super.onBackPressed();
        }
        else {
            pane.openPane();
        }
    }

    @Override
    public void sendDetails(Contact contact, boolean closePane) {
        detailsFragment.loadDetails(contact);
        if (closePane) {
            pane.closePane();
        }
    }
}
