package com.brentgrossman.downloadnpr.ui;

import com.brentgrossman.downloadnpr.R;
import com.brentgrossman.downloadnpr.data.CProvider;
import com.brentgrossman.downloadnpr.internet.DownloadStories;
import com.brentgrossman.downloadnpr.internet.PopulateAvailable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.widget.ToggleButton;

public class AvailableFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>,
		OnClickListener, AdapterView.OnItemSelectedListener {

	static SharedPreferences pref;
	private AvailableCursorAdapter2 adapter = null;
	private static final String[] PROJECTION =
			new String[] { CProvider.Stories._ID, CProvider.Stories.TITLE, CProvider.Stories.AUDIO_LINK };
	private TextView selectAllText;
	private CheckBox selectAllCheckBox;
	private ToggleButton selectAllToggle;
	private Button download;
	private Spinner showSpinner;
	private boolean isListRefresh;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)  {
		View rootView = inflater.inflate(R.layout.available_fragment, container, false);
		adapter = new AvailableCursorAdapter2(getActivity(), null, 0);
		setListAdapter(adapter);
		getLoaderManager().initLoader(0, null, this);
		showSpinner = (Spinner) rootView.findViewById(R.id.show_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
				R.array.shows_array, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		showSpinner.setAdapter(adapter);
		showSpinner.setOnItemSelectedListener(this);
		download = (Button) rootView.findViewById(R.id.download_button);
		download.setOnClickListener(this);
		selectAllToggle = (ToggleButton) rootView.findViewById(R.id.select_all_for_download_toggle);
		selectAllToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				selectAllStories(isChecked);
			}
		});
		return rootView;
	}
	
	private void selectAllStories(boolean yes) {
		ListView listView = getListView();
		for(int i=0; i < getListAdapter().getCount(); i++){
			listView.setItemChecked(i, yes);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter(DownloadStories.downloading);
		getActivity().registerReceiver(afterDownload, filter);
		showSpinner.setSelection(0);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		getActivity().unregisterReceiver(afterDownload);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this.getActivity(), CProvider.Stories.CONTENT_URI, PROJECTION, 
				CProvider.Stories.DOWNLOADED + " IS NULL OR " + CProvider.Stories.DOWNLOADED
						+ " != ? ", new String[] {"1"}, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		adapter.swapCursor(data);
		// If a different show has been selected, set the toggle button to off
		if (data.getCount() == 0) {
			selectAllToggle.setChecked(false);
			selectAllToggle.setChecked(false);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		ViewHolder holder = (ViewHolder) v.getTag();
		holder.storyCheckBox.setChecked(false);
		holder.story.setTextColor(Color.DKGRAY);
		long [] checkedIds = l.getCheckedItemIds();
		if (checkedIds != null) {
			for (int i = 0; i < checkedIds.length; i++) {
				if (checkedIds[i] == getListAdapter().getItemId(position)) {
					holder.storyCheckBox.setChecked(true);
					holder.story.setTextColor(Color.BLACK);
					break;
				}
			}
		}
	}

	@Override
	public void onClick(View v) {
		int viewId = v.getId();
		if (viewId == R.id.download_button) downloadStoryFiles();
	}

	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) this.getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		boolean isOnline = isOnline();
		if (isOnline && position > 0) {
			int showChoice = 0;
			switch (position) {
				case 1:
					showChoice = 3;
					break;
				case 2:
					showChoice = 2;
					break;
				case 3:
					showChoice = 58;
					break;
				case 4:
					showChoice = 13;
					break;
				case 5:
					showChoice = 60;
					break;
				case 6:
					showChoice = 57;
					break;
				case 7:
					showChoice = 35;
					break;
				case 8:
					showChoice = 7;
					break;
				case 9:
					showChoice = 10;
					break;
			}
			Intent intent = new Intent(this.getActivity(), PopulateAvailable.class);
			intent.putExtra(PopulateAvailable.whichShow, showChoice);
			this.getActivity().startService(intent);
		}
		else if (!isOnline) {
			Toast.makeText(this.getActivity(), "No Internet connection.", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {

	}

	private void downloadStoryFiles() {
		if (isOnline()) {
			Intent intent = new Intent(this.getActivity(), DownloadStories.class);
			long[] selectedStories = getListView().getCheckedItemIds();
			if (selectedStories.length > 0) {
				intent.putExtra("selectedStories", selectedStories);
				showSpinner.setEnabled(false);
				download.setEnabled(false);
				download.setText(R.string.downloading_files);
				selectAllToggle.setEnabled(false);
				this.getActivity().startService(intent);
			}
			else Toast.makeText(this.getActivity(), "No stories selected.", Toast.LENGTH_LONG).show();
		}
		else {
			Toast.makeText(this.getActivity(), "No Internet connection.", Toast.LENGTH_LONG).show();
		}
	}
	
	private BroadcastReceiver afterDownload = new BroadcastReceiver() { 
		public void onReceive(Context ctxt, Intent i) {
			if (i.getBooleanExtra(DownloadStories.downloadDone, false)) {
				showSpinner.setEnabled(true);
				download.setEnabled(true);
				download.setText(getResources().getText(R.string.download_selected_stories));
				selectAllToggle.setEnabled(true);
				selectAllToggle.setChecked(false);
			}
			getActivity().removeStickyBroadcast(i);
		}
	};

	class ViewHolder {
		CheckBox storyCheckBox;
		TextView story = null;

		ViewHolder(final View row) {
			storyCheckBox = (CheckBox) row.findViewById(R.id.story_check_box);
			story = (TextView) row.findViewById(R.id.story);
		}
	}

	class AvailableCursorAdapter2 extends CursorAdapter {

		public AvailableCursorAdapter2(Context context, Cursor c, int flags) {
			super(context, c, flags);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = LayoutInflater.from(context).inflate(R.layout.row, parent, false);
			ViewHolder viewHolder = new ViewHolder(view);
			view.setTag(viewHolder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			ViewHolder viewHolder = (ViewHolder) view.getTag();
			final int position = cursor.getPosition();

			String storyTitle = cursor.getString(cursor.getColumnIndex(CProvider.Stories.TITLE));
			String storyAudioLink =
					cursor.getString(cursor.getColumnIndex(CProvider.Stories.AUDIO_LINK));
			String storyDate = storyAudioLink.split("/")[8];
			storyDate = storyDate.split("_")[0];
			storyDate = storyDate.substring(4);
			storyDate = new StringBuilder(storyDate).insert(2, "/").toString();
			// TODO: Use joda time to determine how many days back story goes,
			// instead of just showing the date (e.g., "today", "yesterday", "3 days ago"
//			DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/dd");
//			DateTime dt = formatter.parseDateTime(string);
			viewHolder.story.setText(storyTitle + " - " + storyDate);
			viewHolder.storyCheckBox.setChecked(false);
			viewHolder.story.setTextColor(Color.DKGRAY);
			long [] checkedIds = getListView().getCheckedItemIds();
			if (checkedIds != null) {
				for (int i = 0; i < checkedIds.length; i++) {
					if (checkedIds[i] == getListAdapter().getItemId(position)) {
						viewHolder.storyCheckBox.setChecked(true);
						viewHolder.story.setTextColor(Color.BLACK);
						break;
					}
				}
			}
			final boolean isChecked = viewHolder.storyCheckBox.isChecked();
			viewHolder.storyCheckBox.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					getListView().setItemChecked(position, !isChecked);
				}
			});
		}
	}
}
