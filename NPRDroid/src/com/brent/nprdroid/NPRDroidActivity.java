package com.brent.nprdroid;

import java.util.ArrayList;

import com.brent.nprdroid.MusicService.State;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class NPRDroidActivity extends ListActivity implements OnClickListener,
		OnSeekBarChangeListener {
	private ArrayList<String> songs = new ArrayList<String>();
	private CustomAdapter songList;
	private String TAG = "NPRDroidActivity";
	private ImageButton rewindButton, playButton, nextButton;
	private MusicService musicService;
	boolean mBound = false, downloading = false;
	private int mInterval = 1000; // 1 second updates
	private Handler mHandler;
	private SeekBar seekBar;
	SharedPreferences pref;
	private int playerPosition;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		pref = getSharedPreferences("NPRDownloadPreferences",
				Context.MODE_PRIVATE);
		songList = new CustomAdapter(this, R.layout.row, songs);
		setListAdapter(songList);
		updateSongList();
		rewindButton = (ImageButton) findViewById(R.id.rewindButton);
		playButton = (ImageButton) findViewById(R.id.playButton);
		nextButton = (ImageButton) findViewById(R.id.nextButton);
		rewindButton.setOnClickListener(this);
		playButton.setOnClickListener(this);
		nextButton.setOnClickListener(this);
		seekBar = (SeekBar) findViewById(R.id.seekBar);
		seekBar.setOnSeekBarChangeListener(this);
		mHandler = new Handler();
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Bind to MusicService
		Intent intent = new Intent(this, MusicService.class);
		// intent.putExtra("messenger", new Messenger(handler));
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		startRepeatingTask();
		IntentFilter filter = new IntentFilter(DownloadService.downloading);
		registerReceiver(download, filter);
		if (downloading)
			updateSongList();
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopRepeatingTask();
		unregisterReceiver(download);
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Unbind from the service
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MusicService.NEXT_ITEM) {
				SharedPreferences.Editor editor = pref.edit();
				int newPosition = musicService.mRetriever.listPosition - 1;
				songList.notifyDataSetChanged(); // make newly playing story
													// highlighted in list,
													// others default color
				editor.putInt("listPosition", newPosition); // save this
															// position so its
															// list item can be
															// changed later
				editor.commit();
			}
		}
	};

	final Messenger mMessenger = new Messenger(handler);

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to MusicService, cast the IBinder and get
			// MusicService instance
			musicService = MusicService.getService();
			mBound = true;
			try {
				Message msg = Message.obtain(null,
						MusicService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				(new Messenger(service)).send(msg);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
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
			TextView textRemaining = (TextView) findViewById(R.id.textRemaining);
			TextView textDuration = (TextView) findViewById(R.id.textDuration);
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
				playerPosition = musicService.getPlayerPosition();
				int duration = musicService.getDuration();
				seekBar.setMax(duration);
				seekBar.setProgress(playerPosition);
				int remaining = duration - playerPosition;
				textRemaining.setText(timeString(remaining));
				textDuration.setText(timeString(duration));
			} else
				Log.i(TAG, "musicService is NULL!");
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

	private void updateSongList() {
		songs.clear();
		String[] titles = pref.getString(DownloadService.storyTitles, "")
				.split("\\|");
		for (int i = 0; i < titles.length; ++i) {
			if (titles[i] != null)
				songs.add(titles[i]);
		}
		songList.notifyDataSetChanged();
	}

	@Override
	public void onClick(View v) {
		if (v == playButton) {
			if (musicService.mState == State.Playing) {
				startService(new Intent(MusicService.ACTION_PAUSE));
			} else
				startService(new Intent(MusicService.ACTION_PLAY));
		} else if (v == rewindButton) {
			musicService.processSeekRequest((playerPosition - 30) * 1000);
		} else if (v == nextButton) {
			startService(new Intent(MusicService.ACTION_SKIP));
			nextButton.setImageResource(R.drawable.next_button_pressed);
			nextButton.setImageResource(0); // restores button to "normal",
											// where
											// R.drawable.next_button_normal
											// produces weird effects
			playButton.setImageResource(R.drawable.play_button_pressed);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(MusicService.ACTION_URL);
		intent.putExtra("fileName", songs.get(position).split(" ")[0]);
		intent.putExtra("listPosition", position);
		playButton.setImageResource(R.drawable.play_button_pressed);
		startService((intent));
	}

	public void readWebpage(View view) {
		if (isOnline()) {
			String showChoice;
			if (view.getId() == R.id.me)
				showChoice = "me";
			else
				showChoice = "atc";
			Intent intent = new Intent(this, DownloadService.class);
			intent.putExtra(DownloadService.whichShow, showChoice);
			((Button) findViewById(R.id.me)).setEnabled(false);
			((Button) findViewById(R.id.atc)).setEnabled(false);
			downloading = true;
			updateSongList();
			startService(intent);
		}
		else {
			Toast.makeText(getApplicationContext(), "No Internet connection. DownloadNPR has terminated.", Toast.LENGTH_LONG).show();
			finish();
		}
	}

	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	private BroadcastReceiver download = new BroadcastReceiver() {
		public void onReceive(Context ctxt, Intent i) {
			updateSongList();
			if (i.getBooleanExtra(DownloadService.downloadDone, false)) {
				((Button) findViewById(R.id.me)).setEnabled(true);
				((Button) findViewById(R.id.atc)).setEnabled(true);
				downloading = false;
			}
			removeStickyBroadcast(i);
		}
	};

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (fromUser == true)
			musicService.processSeekRequest(progress * 1000);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

}