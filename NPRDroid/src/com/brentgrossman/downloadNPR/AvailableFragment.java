package com.brentgrossman.downloadNPR;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;



public class AvailableFragment extends ListFragment {
	
	private ArrayList<String> stories = new ArrayList<String>();
	private static CustomAdapter storiesList;
	static SharedPreferences pref;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)  {
		View rootView = inflater.inflate(R.layout.available_fragment, container, false);
		storiesList = new CustomAdapter(this.getActivity(), R.layout.row, stories);
		setListAdapter(storiesList);
		updateStoriesList();
		return rootView;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		updateStoriesList();
	}

	public void readWebpage(View view) {
		if (isOnline()) {
			String showChoice;
			if (view.getId() == R.id.me)
				showChoice = "me";
			else
				showChoice = "atc";
			Intent intent = new Intent(this.getActivity(), DownloadService.class);
			intent.putExtra(DownloadService.whichShow, showChoice);
			((Button) view.findViewById(R.id.me)).setEnabled(false);
			((Button) view.findViewById(R.id.atc)).setEnabled(false);
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

}
