/***
  Copyright (c) 2013-2014 CommonsWare, LLC
  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
  by applicable law or agreed to in writing, software distributed under the
  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
  OF ANY KIND, either express or implied. See the License for the specific
  language governing permissions and limitations under the License.

  From _The Busy Coder's Guide to Android Development_
    http://commonsware.com/Android
 */

package com.nerdery.android.codechallenge.presentation.activity;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.nerdery.android.codechallenge.ContractListFragment;
import com.nerdery.android.codechallenge.R;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class TopicsFragment extends ContractListFragment<TopicsFragment.Contract> 
implements FutureCallback<JsonObject> {
	ItemsAdapter topicsAdapter;
	ArrayList<JsonObject> topics;
	private static final String loadMoreString = "{"
			+ "'kind':'t3',"
			+ "'data':{"
			+ "'author':'',"
			+ "'score':-1,"
			+ "'thumbnail':'',"
			+ "'name':'',"
			+ "'url':'',"
			+ "'title':'\nTouch for 25 more topics',"
			+ "'created_utc':-1,"
			+ "'num_comments':-1"
			+ "}"
			+ "}";

	@Override
	public View onCreateView(LayoutInflater inflater,
			ViewGroup container,
			Bundle savedInstanceState) {
		View result=
				super.onCreateView(inflater, container, savedInstanceState);

		setRetainInstance(true);

		topics = new ArrayList<JsonObject>();
		topicsAdapter = new ItemsAdapter(topics);

		loadTopics(25, null);

		setHasOptionsMenu(true);
		
		return(result);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		
		setListAdapter(topicsAdapter);
		
		ListView listView = getListView();
		listView.setDivider(new ColorDrawable(Color.parseColor("#BFEFFF")));
		listView.setDividerHeight(1);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {

		// Load 25 more topics if the last row is touched
		ListAdapter listAdapter = getListAdapter();
		int listCount = listAdapter.getCount();
		if (position == listCount - 1) {
			JsonObject last = (JsonObject) listAdapter.getItem(listCount - 2);
			String after = last.getAsJsonObject("data").get("name").getAsString();
			topics.remove(listCount - 1);
			loadTopics(25, after);
		}
		else {
			if (getContract().isPersistentSelection()) {
				getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
				l.setItemChecked(position, true);
			}
			else {
				getListView().setChoiceMode(ListView.CHOICE_MODE_NONE);
			}
			JsonObject item = (JsonObject) listAdapter.getItem(position);
			JsonObject data = item.getAsJsonObject("data");
			getContract().onTopicSelected(data.get("permalink").getAsString(), 
					data.get("url").getAsString());
		}
	}

	@Override
	public void onCompleted(Exception e, JsonObject json) {
		if (e != null) {
			Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG)
			.show();
			Log.e(getClass().getSimpleName(),
					"Exception from request to Reddit", e);
		}

		if (json != null) {
			JsonObject data = json.getAsJsonObject("data");
			JsonArray items = data.getAsJsonArray("children");

			for (int i=0; i < items.size(); i++) {
				topics.add(items.get(i).getAsJsonObject());
			}

			JsonObject loadMoreJson = new JsonParser().parse(loadMoreString).getAsJsonObject();
			topics.add(loadMoreJson);
			
			topicsAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.refresh) {
			ListAdapter listAdapter = getListAdapter();
			JsonObject last = (JsonObject) listAdapter.getItem(listAdapter.getCount() - 2);
			topics.clear();
			getListView().setSelection(0);
			String after = last.getAsJsonObject("data").get("name").getAsString();
			loadTopics(25, after);
		}
		return super.onOptionsItemSelected(item);
	}

	private void loadTopics(int count, String after) {
		String url = "http://www.reddit.com/.json?count=" + count;
		if (after != null) {
			url += "&after=" + after;
		}
		Ion.with(getActivity(), url).asJsonObject().setCallback(this);
	}

	class ItemsAdapter extends ArrayAdapter<JsonObject> {
		int size;

		ItemsAdapter(List<JsonObject> items) {
			super(getActivity(), R.layout.topics_row, R.id.title, items);
			size = getActivity().getResources().getDimensionPixelSize(R.dimen.icon);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row=super.getView(position, convertView, parent);
			JsonObject item = getItem(position);
			JsonObject data = item.getAsJsonObject("data");

			String score = data.get("score").getAsString();
			int scoreInt = Integer.parseInt(score);
			TextView scoreTv = (TextView) row.findViewById(R.id.score);
			TextView timeAndPoster = (TextView) row.findViewById(R.id.time_poster);
			TextView comments = (TextView) row.findViewById(R.id.comments_count);
			ImageView icon=(ImageView)row.findViewById(R.id.icon);
			TextView title=(TextView)row.findViewById(R.id.title);
			if (scoreInt != -1) {
				scoreTv.setText(Html.fromHtml(score));

				Date timePosted = new Date(Long.parseLong(data.get("created_utc")
						.getAsString().replace(".0", ""))*1000);
				DateFormat formatter = new SimpleDateFormat("HH:mm", Locale.US);
				formatter.setTimeZone(TimeZone.getDefault());
				String timePostedFormatted = formatter.format(timePosted);
				timeAndPoster.setText(timePostedFormatted + " by " + data.get("author").getAsString());

				comments.setText(data.get("num_comments").getAsString() + " comments");

				Ion.with(icon)
				.placeholder(R.drawable.logo_nerdery)
				.resize(size, size)
				.centerCrop()
				.error(R.drawable.logo_nerdery)
				.load(data.get("thumbnail").getAsString());

				title.setMovementMethod(LinkMovementMethod.getInstance());
				String titleLink = "<a href='" + data.get("url").getAsString() + "'> " + data.get("title").getAsString() + " </a>";
				title.setText(Html.fromHtml(titleLink));
			}
			else {
				scoreTv.setText("");
				timeAndPoster.setText("");
				timeAndPoster.setHeight(1);
				comments.setText("");
				comments.setHeight(1);
				icon.setImageDrawable(null);
				title.setMovementMethod(null);
				title.setText(data.get("title").getAsString());
			}

			return(row);
		}
	}

	interface Contract {
		void onTopicSelected(String permalink, String imageUrl);

		boolean isPersistentSelection();
	}
}
