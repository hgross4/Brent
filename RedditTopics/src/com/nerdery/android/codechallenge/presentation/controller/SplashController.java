package com.nerdery.android.codechallenge.presentation.controller;

import com.nerdery.android.codechallenge.presentation.Constants;
import com.nerdery.android.codechallenge.presentation.activity.MainActivity;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Controller for splash activity
 *
 * @author Andrew
 */
public class SplashController extends AbstractController {

    /**
     * Class this controller navigates to after both splash screen timer and setup of application
     * complete
     */
    private static final Class<?> mClassToNaviagateTo = MainActivity.class;

    /**
     * Minimum amount of time the splash screen is displayed for
     */
    private static final int mSplashScreenMinimumDisplayTime = Constants.SPLASH_SCREEN_DELAY;

    /**
     * Keeps track of the splash screen timer, is set to true after the timer completes
     */
    private boolean mIsSplashScreenTimerComplete = false;

    /**
     * Keeps track of the setup of the application, is set to true after the setup is complete
     */
    private boolean mIsSetupOfApplicationComplete = false;

    /**
     * keep handles to these to be able to cancel if onDestroy is called
     */
    private SetupOfApplicationAsyncTask mSetupOfApplication;

    /**
     * Timer that ensures the splash screen is displayed for a minimum amount of time
     */
    private Timer mTimer;

    /**
     * Splashscreen constructor
     *
     * @param activity the activity this controller controls
     */
    public SplashController(final FragmentActivity activity) {
        super.onCreate(activity);
    }

    /**
     * Launches the main activity after displaying the splash screen for at least displayTime
     * milliseconds.  This may be a longer time due to background processing that could occur to
     * initialize the application
     */
    public void launchActivityAfterDelay() {

        //call the setup of application tasks
        mSetupOfApplication = new SetupOfApplicationAsyncTask();
        mSetupOfApplication.execute(getActivity());

        //set time to have a minimum amount of time the splash
        //screen is shown for
        mTimer = new Timer();
        mTimer.schedule(getSplashScreenDisplayTimer(), mSplashScreenMinimumDisplayTime);
    }

    /**
     * Called to cancel the launch of the main activity, this should be called if the splash screen
     * is being destroyed
     */
    public void cancelLaunch() {
        if (mSetupOfApplication != null) {
            mSetupOfApplication.cancel(true);
        }

        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    /**
     * If both  isSplashScreenTimerComplete and isSetupOfApplicationComplete are both true launches
     * the main application screen
     */
    private void launchMainApplication() {

        if (mIsSetupOfApplicationComplete && mIsSplashScreenTimerComplete) {
            goToActivityNoBackStack(mClassToNaviagateTo);
        }
    }

    /**
     * @return TimerTask, when the timer completes it sets the mIsSplashScreenTimerComplete to true
     *         and runs launchMainApplication on the main application thread
     */
    private TimerTask getSplashScreenDisplayTimer() {
        return new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mIsSplashScreenTimerComplete = true;
                        launchMainApplication();
                    }
                });
            }
        };
    }

    /**
     * Class to allow tasks to happen in the background while the splash screen is showing. Use it
     * to call services ect.
     *
     * @author areitz
     */
    private class SetupOfApplicationAsyncTask extends AsyncTask<Context, Void, Void> {

        /**
         * Run setup of application here (connecting to services ect)
         *
         * @see android.os.AsyncTask#doInBackground(Object[])
         */
        @Override
        public Void doInBackground(Context... contexts) {
            /**
             * PLACE SETUP CODE HERE
             */

            return null;
        }

        /**
         * Runs after the doInBackground completes this runs back on the main thread
         *
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        public void onPostExecute(Void result) {
            mIsSetupOfApplicationComplete = true;
            launchMainApplication();
        }
    }
}
