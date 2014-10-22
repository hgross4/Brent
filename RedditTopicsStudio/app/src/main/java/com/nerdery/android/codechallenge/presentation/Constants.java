package com.nerdery.android.codechallenge.presentation;

public final class Constants {

    /**
     * Splash screens delay value, the minimum amount of time the splash screen will be viewed for.
     * This value is in the constants page in case there are more than one splash screen in an
     * application and when debugging to get to your application faster this may be set to a lower
     * number than in prod.
     */
    public static final int SPLASH_SCREEN_DELAY = 3000; //ms

    /**
     * Suppress default constructor for noninstantiability
     */
    private Constants() {
        throw new AssertionError();
    }
}
