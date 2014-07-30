package com.brentgrossman.downloadNPR;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.brentgrossman.downloadNPR.MusicService.State;

public class DownLoadedFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, OnClickListener, OnSeekBarChangeListener {
	private ArrayList<String> stories = new ArrayList<String>();
	private static CustomAdapter storiesList;
	private SimpleCursorAdapter adapter = null;
	private String TAG = this.getClass().getSimpleName();
	private ImageButton rewindButton, playButton, nextButton;
	private static MusicService musicService;
	boolean mBound = false, downloading = false;
	private int mInterval = 1000; // 1 second updates
	private Handler mHandler;
	private SeekBar seekBar;
	static SharedPreferences pref;
	private int playerPosition;
	private int duration;
	private TextView textRemaining; 
	private TextView textDuration;
	private int seekBarProgress;
	private static final String[] PROJECTION = new String[] { CProvider.Stories._ID, CProvider.Stories.TITLE };

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)  {
		View rootView = inflater.inflate(R.layout.downloaded_fragment, container, false);
		pref = this.getActivity().getSharedPreferences("NPRDownloadPreferences", Context.MODE_PRIVATE);
		//		storiesList = new CustomAdapter(this.getActivity(), R.layout.row, R.id.story, stories);
		//		setListAdapter(storiesList);
		//		updateStoriesList();
		adapter = new SimpleCursorAdapter(getActivity(), R.layout.row, null, new String[] { CProvider.Stories.TITLE }, new int[] { R.id.story }, 0);
		setListAdapter(adapter);
		getLoaderManager().initLoader(0, null, this);
		rewindButton = (ImageButton) rootView.findViewById(R.id.rewindButton);
		playButton = (ImageButton) rootView.findViewById(R.id.playButton);
		nextButton = (ImageButton) rootView.findViewById(R.id.nextButton);
		rewindButton.setOnClickListener(this);
		playButton.setOnClickListener(this);
		nextButton.setOnClickListener(this);
		seekBar = (SeekBar) rootView.findViewById(R.id.seekBar);
		seekBar.setOnSeekBarChangeListener(this);
		mHandler = new Handler();
		textRemaining = (TextView) rootView.findViewById(R.id.textRemaining);
		textDuration = (TextView) rootView.findViewById(R.id.textDuration);
		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();
		// Bind to MusicService
		Intent intent = new Intent(this.getActivity(), MusicService.class);
		// intent.putExtra("messenger", new Messenger(handler));
		this.getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onResume() {
		super.onResume();
		startRepeatingTask();
		IntentFilter filter = new IntentFilter(DownloadService.downloading);
		this.getActivity().registerReceiver(download, filter);
		//		if (downloading) updateStoriesList();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopRepeatingTask();
		this.getActivity().unregisterReceiver(download);
	}

	@Override
	public void onStop() {
		super.onStop();
		// Unbind from the service
		if (mBound) {
			this.getActivity().unbindService(mConnection);
			mBound = false;
		}
	}

	static class messageHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MusicService.NEXT_ITEM) {
				SharedPreferences.Editor editor = pref.edit();
				int newPosition = musicService.mRetriever.listPosition - 1;
				// make newly playing story highlighted in list, others default color:
				storiesList.notifyDataSetChanged();
				// save this position so its list item can be changed later:
				editor.putInt("listPosition", newPosition);
				editor.commit();
			}
		}
	}

	final Messenger mMessenger = new Messenger(new messageHandler());

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to MusicService, cast the IBinder and get
			// MusicService instance
			musicService = MusicService.getService();
			mBound = true;
			try {
				Message msg = Message.obtain(null, MusicService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				(new Messenger(service)).send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
		}
	};

	Runnable mStatusChecker = new Runnable() {

		@Override
		public void run() {
			mHandler.postDelayed(mStatusChecker, mInterval);
			if (musicService != null) {
				switch (musicService.mState) {
				case Playing:
					playButton.setImageResource(R.drawable.pause_button_normal);
					break;
				case Paused:
					playButton.setImageResource(R.drawable.play_button_normal);
					break;
				case Preparing:
					playButton.setImageResource(R.drawable.play_button_pressed);
					break;
				default:
					playButton.setImageResource(R.drawable.play_button_normal);
					break;
				}
				duration = musicService.getDuration();
				seekBar.setMax(duration);
				playerPosition = musicService.getPlayerPosition();
				seekBar.setProgress(playerPosition);
				int remaining = (duration - playerPosition)/1000;
				textRemaining.setText(timeString(remaining));
				textDuration.setText(timeString(duration/1000));
			} else
				Log.e(TAG, "musicService is NULL!");
		}
	};

	void startRepeatingTask() {
		mStatusChecker.run();
	}

	void stopRepeatingTask() {
		mHandler.removeCallbacks(mStatusChecker);
	}

	private String timeString(int totalSeconds) {
		int minutes = (totalSeconds) / 60;
		int intSeconds = (totalSeconds) % 60;
		String seconds = intSeconds > 9 ? "" + intSeconds : "0" + intSeconds;
		return (minutes + ":" + seconds);
	}

	private void updateStoriesList() {
		stories.clear();
		String[] titles = pref.getString(DownloadService.storyTitles, "").split("\\|");
		for (int i = 0; i < titles.length; ++i) {
			if (titles[i] != null)
				stories.add(titles[i]);
		}
		storiesList.notifyDataSetChanged();
	}

	@Override
	public void onClick(View v) {
		if (v == playButton) {
			if (musicService.mState == State.Playing) {
				this.getActivity().startService(new Intent(MusicService.ACTION_PAUSE));
			} else
				this.getActivity().startService(new Intent(MusicService.ACTION_PLAY));
		} else if (v == rewindButton) {
			musicService.processSeekRequest((playerPosition - 30000));
		} else if (v == nextButton) {
			this.getActivity().startService(new Intent(MusicService.ACTION_SKIP));
			nextButton.setImageResource(R.drawable.next_button_pressed);
			nextButton.setImageResource(0); // restores button to "normal",
			// where
			// R.drawable.next_button_normal
			// produces weird effects
			playButton.setImageResource(R.drawable.play_button_pressed);
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(MusicService.ACTION_URL);
		Cursor cursor = this.getActivity().getContentResolver().query(CProvider.Stories.CONTENT_URI, 
				new String[] {CProvider.Stories.FILE_NAME}, 
				CProvider.Stories._ID + " = ? ", new String[] {Long.toString(id)}, null);
		if (cursor != null && cursor.moveToFirst()) {
			String fileName = cursor.getString(cursor.getColumnIndex(CProvider.Stories.FILE_NAME));
			Log.wtf(TAG, fileName);
			intent.putExtra("fileName", fileName);
			intent.putExtra("listPosition", position);
			playButton.setImageResource(R.drawable.play_button_pressed);
			this.getActivity().startService((intent));
		}
	}

	private BroadcastReceiver download = new BroadcastReceiver() {
		public void onReceive(Context ctxt, Intent i) {
			//			updateStoriesList();
			if (i.getBooleanExtra(DownloadService.downloadDone, false)) {
				downloading = false;
			}
			//				removeStickyBroadcast(i);
		}
	};

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser == true) {
			seekBarProgress = progress;
			int remaining = (duration - progress)/1000;
			textRemaining.setText(timeString(remaining));
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		musicService.processSeekRequest(seekBarProgress);	// Was causing mediaplayer to "hiccough" when in onProgressChanged
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(this.getActivity(), CProvider.Stories.CONTENT_URI, PROJECTION, 
				CProvider.Stories.DOWNLOADED + " = ? ", new String[] {"1"}, null);

	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		adapter.swapCursor(arg1);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		adapter.swapCursor(null);

	}
}