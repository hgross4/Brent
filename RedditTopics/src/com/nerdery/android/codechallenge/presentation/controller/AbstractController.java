package com.nerdery.android.codechallenge.presentation.controller;

import com.nerdery.android.codechallenge.presentation.dialog.ErrorDialog;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;

/**
 * All controllers should extend this class, keeps common code
 *
 * @author Andrew
 */
public abstract class AbstractController {

    private FragmentActivity mActivity;

    /**
     * The activity the controller controls
     */
    public Activity getActivity() {
        return mActivity;
    }

    /**
     * Set the activity this controller controllers
     *
     * @param activity the activity this controller controllers
     */
    public void setActivity(FragmentActivity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("activity can not be null");
        }

        mActivity = activity;
    }

    /**
     * Call this from other controllers to save member variables and do other required setup (Add
     * more variables here to set as they are needed)
     *
     * @param activity activity that this controller controls
     */
    public final void onCreate(final FragmentActivity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("activity can not be null");
        }

        mActivity = activity;
    }

    /**
     * Go to an activity
     *
     * @param clazz The class of the activity to go to
     */
    public void goToActivity(Class<?> clazz) {

        if (clazz == null) {
            throw new IllegalArgumentException("clazz can not be null");
        }

        if (mActivity == null) {
            throw new IllegalStateException(
                    "onCreate must be called before this method may be called");
        }

        mActivity.startActivity(new Intent(mActivity, clazz));
    }

    /**
     * Go to an activity, calls finish on current activity so the back button will not navigate back
     * to it
     *
     * @param clazz The class of the activity to navigate to
     */
    public void goToActivityNoBackStack(Class<?> clazz) {
        this.goToActivity(clazz);
        mActivity.finish();
    }

    /**
     * Show Error Dialog
     *
     * Pops up an error dialog message on the screen.
     *
     * @param title The title / header of the error message that is displayed
     * @param message The message body of the dialog
     */
    public void showErrorDialog(String title, String message) {

        ErrorDialog error = ErrorDialog.newInstance(title, message);

        error.show(mActivity.getSupportFragmentManager(), "Error");

    }
}
