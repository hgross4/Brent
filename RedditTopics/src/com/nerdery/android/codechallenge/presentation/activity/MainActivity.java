package com.nerdery.android.codechallenge.presentation.activity;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.nerdery.android.codechallenge.R;
import com.nerdery.android.codechallenge.presentation.TopicClickedEvent;

import de.greenrobot.event.EventBus;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Main activity of the application
 *
 * @author areitz
 */
public class MainActivity extends SherlockFragmentActivity {
	private TopicsFragment topicsFragment = null;
//	  private DetailsFragment detailsFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        topicsFragment = 
        		(TopicsFragment) getSupportFragmentManager().findFragmentById(R.id.topics);
        if (topicsFragment == null) {
        	topicsFragment = new TopicsFragment();
        	getSupportFragmentManager().beginTransaction()
                                .add(R.id.topics, topicsFragment).commit();
          }
    }
    
    @Override
    public void onResume() {
      super.onResume();
      EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
      EventBus.getDefault().unregister(this);
      super.onPause();
    }

    public void onEventMainThread(TopicClickedEvent event) {
      startActivity(new Intent(Intent.ACTION_VIEW,
                               Uri.parse(event.item.get("link")
                                                   .getAsString())));
    }
}
