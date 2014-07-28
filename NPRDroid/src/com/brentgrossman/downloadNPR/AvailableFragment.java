package com.brentgrossman.downloadNPR;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
	private static final String[] items= { "lorem", "ipsum", "dolor", "sit", "amet", "consectetuer", "adipiscing", "elit", "morbi",
	      "vel", "ligula", "vitae", "arcu", "aliquet", "mollis", "etiam",
	      "vel", "erat", "placerat", "ante", "porttitor", "sodales",
	      "pellentesque", "augue", "purus" };
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)  {
		View rootView = inflater.inflate(R.layout.available_fragment, container, false);
//		storiesList = new CustomAdapter(this.getActivity(), R.layout.row, stories);
//		setListAdapter(storiesList);
//		updateStoriesList();
		adapter = new SimpleCursorAdapter(getActivity(), R.layout.row, null, new String[] { CProvider.Stories.TITLE }, new int[] { R.id.story}, 0);
//		setListAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, items));
		setListAdapter(adapter);
		getLoaderManager().initLoader(0, null, this);
		Button me = (Button) rootView.findViewById(R.id.me);
        me.setOnClickListener(this);
        Button atc = (Button) rootView.findViewById(R.id.atc);
        atc.setOnClickListener(this);
		return rootView;
	}
	
	@Override
	public void onResume() {
		super.onResume();
//		updateStoriesList();
	}

	public void readWebpage(View view) {
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
		readWebpage(v);
	}

}
