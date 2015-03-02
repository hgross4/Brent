package com.brent.constactslist;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends FragmentActivity implements ContactsListFragment.SendDetailsListener {

    private DetailsFragment detailsFragment = null;
    private SlidingPaneLayout pane = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Contacts");

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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void sendDetails(Contact contact, boolean closePane) {
        detailsFragment.loadDetails(contact);
        if (closePane) {
            pane.closePane();
        }
    }
}
