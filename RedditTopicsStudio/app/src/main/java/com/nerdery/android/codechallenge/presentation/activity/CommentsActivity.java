package com.nerdery.android.codechallenge.presentation.activity;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.nerdery.android.codechallenge.R;

import android.os.Bundle;

public class CommentsActivity extends SherlockFragmentActivity {

	public static final String PERMALINK = "permalink";
	public static final String URL = "url";
	private String commentsLink = null;
	private String imageUrl = null;
	private CommentsFragment commentsFragment = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		commentsFragment=
				(CommentsFragment)getSupportFragmentManager().findFragmentById(R.id.comments);

		if (commentsFragment == null) {
			commentsFragment = new CommentsFragment();

			getSupportFragmentManager().beginTransaction()
			.add(android.R.id.content, commentsFragment)
			.commit();
		}

		commentsLink = getIntent().getStringExtra(PERMALINK);
		imageUrl = getIntent().getStringExtra(URL);
	}

	@Override
	public void onResume() {
		super.onResume();
		commentsFragment.loadImage(imageUrl);
		commentsFragment.loadComments(commentsLink, 25);
	}

}
