package com.brentgrossman.downloadNPR.ui;

import com.brentgrossman.downloadNPR.R;
import com.brentgrossman.downloadNPR.data.CProvider;
import com.brentgrossman.downloadNPR.internet.DownloadStories;
import com.brentgrossman.downloadNPR.internet.PopulateAvailable;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;


public class AvailableFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, OnClickListener {

	static SharedPreferences pref;
	private AvailableCursorAdapter adapter = null;
	private static final String[] PROJECTION = new String[] { CProvider.Stories._ID, CProvider.Stories.TITLE };
	private TextView selectAllText;
	private CheckBox selectAllCheckBox;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)  {
		View rootView = inflater.inflate(R.layout.available_fragment, container, false);
		adapter = new AvailableCursorAdapter(getActivity(), R.layout.row, null, new String[] { CProvider.Stories.TITLE }, new int[] { R.id.story }, 0);
		setListAdapter(adapter);
		getLoaderManager().initLoader(0, null, this);
		Button me = (Button) rootView.findViewById(R.id.me);
		me.setOnClickListener(this);
		Button atc = (Button) rootView.findViewById(R.id.atc);
		atc.setOnClickListener(this);
		Button download = (Button) rootView.findViewById(R.id.download_button);
		download.setOnClickListener(this);
		selectAllText = (TextView) rootView.findViewById(R.id.select_all_text);
		selectAllText.setOnClickListener(this);
		selectAllCheckBox = (CheckBox) rootView.findViewById(R.id.select_all_check_box);
		selectAllCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				final ListView listView = getListView();
				for(int i=0; i < getListAdapter().getCount(); i++){
					listView.setItemChecked(i, isChecked);
				}
				selectAllText.setTextColor(isChecked ? Color.WHITE : Color.LTGRAY);
			}
		});
		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
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
	public void onListItemClick(ListView l, View v, int position, long id) {
		ViewHolder holder = (ViewHolder) v.getTag();
		holder.storyCheckBox.setChecked(false);
		holder.story.setTextColor(Color.LTGRAY);
		long [] checkedIds = l.getCheckedItemIds();
		if (checkedIds != null) {
			for (int i = 0; i < checkedIds.length; i++) {
				if (checkedIds[i] == getListAdapter().getItemId(position)) {
					holder.storyCheckBox.setChecked(true);
					holder.story.setTextColor(Color.WHITE);
					break;
				}
			}
		}
	}

	@Override
	public void onClick(View v) {
		int viewId = v.getId();
		if (viewId == R.id.download_button) downloadStoryFiles();
		else if (viewId == R.id.select_all_text) {
			boolean isChecked = selectAllCheckBox.isChecked();
			selectAllCheckBox.setChecked(!isChecked);
			selectAllText.setTextColor(isChecked ? Color.LTGRAY : Color.WHITE);
		}
		else getStoriesList(v);
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
			long[] selectedStories = getListView().getCheckedItemIds();
			if (selectedStories.length > 0) {
				intent.putExtra("selectedStories", selectedStories);
				this.getActivity().startService(intent);
			}
			else Toast.makeText(this.getActivity(), "No stories selected.", Toast.LENGTH_LONG).show();
		}
		else {
			Toast.makeText(this.getActivity(), "No Internet connection. DownloadNPR has terminated.", Toast.LENGTH_LONG).show();
			this.getActivity().finish(); // This may not work or be a good idea
		}
	}

	class AvailableCursorAdapter extends SimpleCursorAdapter {
		AvailableCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			Log.wtf(getTag(), "getView");
			View row = super.getView(position, convertView, parent);
			ViewHolder holder = (ViewHolder)row.getTag();		
			if (holder == null) {                         
				holder = new ViewHolder(row);
				row.setTag(holder);
			}
			holder.storyCheckBox.setChecked(false);
			holder.story.setTextColor(Color.LTGRAY);
			long [] checkedIds = getListView().getCheckedItemIds();
			if (checkedIds != null) {
				for (int i = 0; i < checkedIds.length; i++) {
					if (checkedIds[i] == getListAdapter().getItemId(position)) {
						holder.storyCheckBox.setChecked(true);
						holder.story.setTextColor(Color.WHITE);
						break;
					}
				}
			}
			final boolean isChecked = holder.storyCheckBox.isChecked();
			holder.storyCheckBox.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					getListView().setItemChecked(position, !isChecked);
				}
			});
			return(row);
		}
	}

	class ViewHolder {
		CheckBox storyCheckBox;
		TextView story = null;

		ViewHolder(final View row) {
			storyCheckBox = (CheckBox) row.findViewById(R.id.story_check_box);
			story = (TextView) row.findViewById(R.id.story);
		}
	}

}