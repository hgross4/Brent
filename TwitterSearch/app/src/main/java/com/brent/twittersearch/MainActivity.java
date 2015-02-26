package com.brent.twittersearch;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;


public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportFragmentManager().findFragmentById(android.R.id.content)==null) {
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, new TweetFragment())
                    .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Register for location updates
        LocationFinder.requestLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unregister for location updates to save battery
        // (and we don't need them when app isn't in foreground)
        LocationFinder.removeUpdates();
    }
}
