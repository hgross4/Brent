package com.nerdery.android.codechallenge.presentation.activity;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.nerdery.android.codechallenge.R;

public class DetailsFragment extends SherlockFragment implements FutureCallback<JsonArray> {

	ArrayList<JsonObject> comments;
	ItemsAdapter commentsAdapter;
	private String permalink;
	private ListView commentsList;
	private ImageView image;
	private static final String loadMoreString = "{"
			+ "'kind':'t3',"
			+ "'data':{"
			+ "'author':'',"
			+ "'score':-1,"
			+ "'thumbnail':'',"
			+ "'name':'',"
			+ "'url':'',"
			+ "'body':'\nTouch for 25 more comments',"
			+ "'created_utc':-1,"
			+ "'num_comments':-1"
			+ "}"
			+ "}";

	@Override
	public View onCreateView(LayoutInflater inflater,
			ViewGroup container,
			Bundle savedInstanceState) {
		View result =
				inflater.inflate(R.layout.fragment_details, container, false);

		setRetainInstance(true);

		comments = new ArrayList<JsonObject>();
		
		commentsAdapter = new ItemsAdapter(comments);
		
		commentsList = (ListView) result.findViewById(R.id.comments_list);
		
		image = (ImageView) result.findViewById(R.id.image);
		
		return(result);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		commentsList.setAdapter(commentsAdapter);
		commentsList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// Load 25 more topics if the last row is touched
				ListAdapter listAdapter = commentsList.getAdapter();
				int listCount = listAdapter.getCount();
				if (position == listCount - 1) {
					comments.remove(position);
					commentsAdapter.notifyDataSetChanged();
					loadComments(permalink, listCount + 24);
				}
			}
		});
	}

	@Override
	public void onCompleted(Exception e, JsonArray json) {
		if (e != null) {
			Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG)
			.show();
			Log.e(getClass().getSimpleName(),
					"Exception from request to Reddit", e);
		}

		if (json != null) {
			JsonObject jsonObject = (JsonObject) json.get(1);
			JsonObject data = (JsonObject) jsonObject.getAsJsonObject().get("data");
			JsonArray items = data.getAsJsonArray("children");

			for (int i=0; i < items.size(); i++) {
				comments.add(items.get(i).getAsJsonObject());
			}

			JsonObject loadMoreJson = new JsonParser().parse(loadMoreString).getAsJsonObject();
			comments.add(loadMoreJson);
			commentsAdapter.notifyDataSetChanged();
		}
	}
	
	void loadImage(String imageUrl) {
		Ion.with(image)
		.placeholder(R.drawable.logo_nerdery)
		.error(R.drawable.logo_nerdery)
		.load(imageUrl);
	}

	void loadComments(String commentsLink, int limit) {
		permalink = commentsLink;
		commentsLink = "http://www.reddit.com" + commentsLink 
				+ ".json?limit=" + limit + "&sort=new";
		android.util.Log.wtf("TAG", commentsLink);
		if (limit == 25) { // clear the list, but not if we're adding 25 more
			comments.clear();
			commentsAdapter.notifyDataSetChanged();
		}
		Ion.with(getActivity(), commentsLink).asJsonArray().setCallback(this);
	}

	class ItemsAdapter extends ArrayAdapter<JsonObject> {

		ItemsAdapter(List<JsonObject> items) {
			super(getActivity(), R.layout.comments_row, R.id.title, items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
		        convertView =
		            LayoutInflater.from(getActivity()).inflate(R.layout.comments_row,
		                                                       parent, false);
			}
			JsonObject item = getItem(position);
			JsonObject data = item.getAsJsonObject("data");

			TextView commentsTv = (TextView) convertView.findViewById(R.id.comment);
			if (data.get("body") != null) {
				commentsTv.setText(data.get("body").getAsString());
			}

			return(convertView);
		}
	}

}
