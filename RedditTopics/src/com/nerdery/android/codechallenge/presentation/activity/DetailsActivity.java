package com.nerdery.android.codechallenge.presentation.activity;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.nerdery.android.codechallenge.R;

import android.os.Bundle;

public class DetailsActivity extends SherlockFragmentActivity {

	public static final String PERMALINK = "permalink";
	public static final String URL = "url";
	private String commentsLink = null;
	private String imageUrl = null;
	private DetailsFragment detailsFragment = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		detailsFragment=
				(DetailsFragment)getSupportFragmentManager().findFragmentById(R.id.details);

		if (detailsFragment == null) {
			detailsFragment = new DetailsFragment();

			getSupportFragmentManager().beginTransaction()
			.add(android.R.id.content, detailsFragment)
			.commit();
		}

		commentsLink = getIntent().getStringExtra(PERMALINK);
		imageUrl = getIntent().getStringExtra(URL);
	}

	@Override
	public void onResume() {
		super.onResume();
		detailsFragment.loadImage(imageUrl);
		detailsFragment.loadComments(commentsLink, 25);
	}

}
