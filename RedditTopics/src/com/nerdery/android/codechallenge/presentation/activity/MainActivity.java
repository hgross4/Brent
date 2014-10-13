package com.nerdery.android.codechallenge.presentation.activity;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.nerdery.android.codechallenge.R;
import com.nerdery.android.codechallenge.TopicClickedEvent;

import de.greenrobot.event.EventBus;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Main activity of the application
 *
 * @author areitz
 */
public class MainActivity extends SherlockFragmentActivity 
implements TopicsFragment.Contract {
	private TopicsFragment topicsFragment = null;
	private DetailsFragment detailsFragment = null;

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

		detailsFragment = 
				(DetailsFragment) getSupportFragmentManager().findFragmentById(R.id.details);
		if (detailsFragment == null && findViewById(R.id.details) != null) {
			detailsFragment = new DetailsFragment();
			getSupportFragmentManager().beginTransaction()
			.add(R.id.details, detailsFragment).commit();
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

	@Override
	public void onTopicSelected(String permalink, String imageUrl) {
		if (detailsFragment != null && detailsFragment.isVisible()) {
			detailsFragment.loadImage(imageUrl);
			detailsFragment.loadComments(permalink, 25);
			android.util.Log.wtf("TAG", "permalink: " + permalink);
		}
		else {
			Intent i=new Intent(this, DetailsActivity.class);
			i.putExtra(DetailsActivity.PERMALINK, permalink);
			i.putExtra(DetailsActivity.URL, imageUrl);
			startActivity(i);
		}

	}

	@Override
	public boolean isPersistentSelection() {
		// TODO Auto-generated method stub
		return false;
	}
}
