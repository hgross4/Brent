package com.brentgrossman.downloadNPR;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;


public class AvailableFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, OnClickListener {

	private ArrayList<String> stories = new ArrayList<String>();
	private static CustomAdapter storiesList;
	static SharedPreferences pref;
	private SimpleCursorAdapter adapter = null;
	private static final String[] PROJECTION = new String[] { CProvider.Stories._ID, CProvider.Stories.TITLE };
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)  {
		View rootView = inflater.inflate(R.layout.available_fragment, container, false);
		//		storiesList = new CustomAdapter(this.getActivity(), R.layout.row, stories);
		//		setListAdapter(storiesList);
		//		updateStoriesList();
		adapter = new SimpleCursorAdapter(getActivity(), R.layout.row, null, new String[] { CProvider.Stories.TITLE }, new int[] { R.id.story }, 0);
		setListAdapter(adapter);
		getLoaderManager().initLoader(0, null, this);
		Button me = (Button) rootView.findViewById(R.id.me);
		me.setOnClickListener(this);
		Button atc = (Button) rootView.findViewById(R.id.atc);
		atc.setOnClickListener(this);
		Button download = (Button) rootView.findViewById(R.id.download_button);
		download.setOnClickListener(this);
		CheckBox selectAll = (CheckBox) rootView.findViewById(R.id.select_all_check_box);
		selectAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				final ListView listView = getListView();
				for(int i=0; i < getListAdapter().getCount(); i++){
					View view = getViewByPosition(i, listView);
					CheckBox cb = (CheckBox)view.findViewById(R.id.story_check_box);
					if (isChecked) {
						cb.setChecked(true);
					}
					else {
						cb.setChecked(false);
					}
				}
			}

		});
		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		//		updateStoriesList();
	}

	private void updateStoriesList() {
		stories.clear();
		pref = this.getActivity().getSharedPreferences("NPRDownloadPreferences", Context.MODE_PRIVATE);
		String[] titles = pref.getString(DownloadService.storyTitles, "").split("\\|");
		for (int i = 0; i < titles.length; ++i) {
			if (titles[i] != null)
				stories.add(titles[i]);
		}
		storiesList.notifyDataSetChanged();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this.getActivity(), CProvider.Stories.CONTENT_URI, PROJECTION, 
				CProvider.Stories.DOWNLOADED + " IS NULL OR " + CProvider.Stories.DOWNLOADED + " != ? ", new String[] {"1"}, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		adapter.swapCursor(data);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);

	}

	@Override
	public void onClick(View v) {
		if (v.getId() != R.id.download_button) getStoriesList(v);
		else downloadStoryFiles();
	}

	public void getStoriesList(View view) {
		if (isOnline()) {
			String showChoice;
			if (view.getId() == R.id.me)
				showChoice = "me";
			else
				showChoice = "atc";
			Intent intent = new Intent(this.getActivity(), PopulateAvailable.class);
			intent.putExtra(PopulateAvailable.whichShow, showChoice);
			//			((Button) view.findViewById(R.id.me)).setEnabled(false);
			//			((Button) view.findViewById(R.id.atc)).setEnabled(false);
			this.getActivity().startService(intent);
		}
		else {
			Toast.makeText(this.getActivity(), "No Internet connection. DownloadNPR has terminated.", Toast.LENGTH_LONG).show();
			this.getActivity().finish(); // This may not work or be a good idea
		}
	}

	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) this.getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	private void downloadStoryFiles() {
		if (isOnline()) {
			Intent intent = new Intent(this.getActivity(), DownloadStories.class);
			this.getActivity().startService(intent);
		}
		else {
			Toast.makeText(this.getActivity(), "No Internet connection. DownloadNPR has terminated.", Toast.LENGTH_LONG).show();
			this.getActivity().finish(); // This may not work or be a good idea
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		ContentValues values = new ContentValues();
		int selected = 0;
		long [] checkedIds = getListView().getCheckedItemIds();
		if (checkedIds != null) {
			for (int i = 0; i < checkedIds.length; i++) {
				if (checkedIds[i] == id) {
					selected = 1;
					break;
				}
			}
		}
		values.put(CProvider.Stories.SELECTED, selected);
		this.getActivity().getContentResolver().update(CProvider.Stories.CONTENT_URI, values, CProvider.Stories._ID + " = ? ", new String[] {Long.toString(id)});
//		SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();
	}

	public View getViewByPosition(int position, ListView listView) {
		final int firstListItemPosition = listView.getFirstVisiblePosition();
		final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

		if (position < firstListItemPosition || position > lastListItemPosition ) {
			return listView.getAdapter().getView(position, listView.getChildAt(position), listView);
		} else {
			final int childIndex = position - firstListItemPosition;
			return listView.getChildAt(childIndex);
		}
	}

}
