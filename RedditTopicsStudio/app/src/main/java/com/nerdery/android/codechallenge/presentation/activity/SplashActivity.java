package com.nerdery.android.codechallenge.presentation.activity;

import com.nerdery.android.codechallenge.R;
import com.nerdery.android.codechallenge.presentation.controller.SplashController;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * Activity for displaying splash screen and running setup tasks for the app
 */
public class SplashActivity extends FragmentActivity {

    private SplashController mController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mController = new SplashController(this);
        mController.launchActivityAfterDelay();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mController.cancelLaunch();
    }
}
