package com.brent.nprdroid;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.MediaController;

public class MyMediaController extends MediaController {
    public MyMediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyMediaController(Context context, boolean useFastForward) {
        super(context, useFastForward);
    }

    public MyMediaController(Context context) {
        super(context);
    }

    @Override
    public void show(int timeout) {
        super.show(0);
    }
    
//    @Override
//    public void hide() {
//        // No code here, so the controller can't be hidden
//
//    }

}