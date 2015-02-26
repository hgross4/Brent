package com.brent.twittersearch;

import android.app.Application;
import android.content.Context;

/**
 * Created by Brent on 2/22/15.
 */
public class TwitterSearchApplication extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();
    }

    public static Context getAppContext() {
        return context;
    }
}
