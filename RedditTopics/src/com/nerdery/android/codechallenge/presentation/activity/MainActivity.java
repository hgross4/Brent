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
	private CommentsFragment commentsFragment = null;

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

		commentsFragment = 
				(CommentsFragment) getSupportFragmentManager().findFragmentById(R.id.comments);
		if (commentsFragment == null && findViewById(R.id.comments) != null) {
			commentsFragment = new CommentsFragment();
			getSupportFragmentManager().beginTransaction()
			.add(R.id.comments, commentsFragment).commit();
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
		if (commentsFragment != null && commentsFragment.isVisible()) {
			commentsFragment.loadImage(imageUrl);
			commentsFragment.loadComments(permalink, 25);
		}
		else {
			Intent i=new Intent(this, CommentsActivity.class);
			i.putExtra(CommentsActivity.PERMALINK, permalink);
			i.putExtra(CommentsActivity.URL, imageUrl);
			startActivity(i);
		}

	}

	@Override
	public boolean isPersistentSelection() {
		// TODO Auto-generated method stub
		return false;
	}
}
