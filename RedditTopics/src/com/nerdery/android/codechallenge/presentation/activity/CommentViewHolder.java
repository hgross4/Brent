package com.nerdery.android.codechallenge.presentation.activity;

import com.nerdery.android.codechallenge.R;

import android.view.View;
import android.widget.TextView;

public class CommentViewHolder {
	private TextView commentTv = null;

	CommentViewHolder(View row) {
		commentTv = (TextView)row.findViewById(R.id.comment);
	}
	
	void setText(String comment) {
		commentTv.setText(comment);
	}
}
