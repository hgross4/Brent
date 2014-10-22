package com.nerdery.android.codechallenge.presentation.activity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.koushikdutta.ion.Ion;
import com.nerdery.android.codechallenge.R;

public class TopicViewHolder {
	TextView scoreTv = null;
	TextView timeAndPoster = null;
	TextView numComments = null;
	ImageView icon = null;
	TextView title = null;

	TopicViewHolder(View row) {
		scoreTv = (TextView) row.findViewById(R.id.score);
		timeAndPoster = (TextView) row.findViewById(R.id.time_poster);
		numComments = (TextView) row.findViewById(R.id.comments_count);
		icon=(ImageView)row.findViewById(R.id.icon);
		title=(TextView)row.findViewById(R.id.title);
	}
	
	void populateViews(JsonObject data, int size) {
		String score = data.get("score").getAsString();
		int scoreInt = Integer.parseInt(score);
		if (scoreInt != -1) {
			scoreTv.setText(Html.fromHtml(score));

			Date timePosted = new Date(Long.parseLong(data.get("created_utc")
					.getAsString().replace(".0", ""))*1000);
			DateFormat formatter = new SimpleDateFormat("HH:mm", Locale.US);
			formatter.setTimeZone(TimeZone.getDefault());
			String timePostedFormatted = formatter.format(timePosted);
			timeAndPoster.setText(timePostedFormatted + " by " + data.get("author").getAsString());
			
			Ion.with(icon)
			.placeholder(R.drawable.logo_nerdery)
			.resize(size, size)
			.centerCrop()
			.error(R.drawable.logo_nerdery)
			.load(data.get("thumbnail").getAsString());
			
			numComments.setText(data.get("num_comments").getAsString() + " comments");
			
			title.setMovementMethod(LinkMovementMethod.getInstance());
			String titleLink = "<a href='" + data.get("url").getAsString() + "'> " + data.get("title").getAsString() + " </a>";
			title.setText(Html.fromHtml(titleLink));
		}
		else {
			scoreTv.setText("");
			timeAndPoster.setText("");
			timeAndPoster.setHeight(1);
			numComments.setText("");
			numComments.setHeight(1);
			icon.setImageDrawable(null);
			title.setMovementMethod(null);
			title.setText(data.get("title").getAsString());
		}
	}
}
