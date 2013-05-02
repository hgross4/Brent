package com.brent.nprdroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.DownloadManager;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class NPRDroidActivity extends ListActivity implements OnClickListener, OnSeekBarChangeListener {	
	private String sdPath; 
	private ArrayList<String> songs = new ArrayList<String>();
	private String TAG = "NPRDroidActivity";
	private ImageButton rewindButton, playButton, pauseButton, nextButton;
	private Button me, atc;
	private MusicService musicService;
	boolean mBound = false;
	private int mInterval = 1000; // 1 second updates
	private Handler mHandler;
	private SeekBar seekBar;
	SharedPreferences pref;
	private DownloadManager downloadManager = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		updateSongList();
		me = (Button) findViewById(R.id.me);
		atc = (Button) findViewById(R.id.atc);
		rewindButton = (ImageButton) findViewById(R.id.rewindButton);
		playButton = (ImageButton) findViewById(R.id.playButton);
		pauseButton = (ImageButton) findViewById(R.id.pauseButton);
		nextButton = (ImageButton) findViewById(R.id.nextButton);
		rewindButton.setOnClickListener(this);
		playButton.setOnClickListener(this);
		pauseButton.setOnClickListener(this);
		nextButton.setOnClickListener(this);
		seekBar = (SeekBar) findViewById(R.id.seekBar);
		mHandler = new Handler();
		pref = getSharedPreferences("NPRDownloadPreferences", Context.MODE_PRIVATE);
		downloadManager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG, "onStart");
		// Bind to MusicService
		Intent intent = new Intent(this, MusicService.class);
		//		intent.putExtra("messenger", new Messenger(handler));
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);		
	}

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter f = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		f.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED);
		registerReceiver(onDownloadEvent, f);
		startRepeatingTask();
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopRepeatingTask();
		unregisterReceiver(onDownloadEvent);
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
			Log.i(TAG, "start handleMessage");
			if (msg.what == MusicService.NEXT_ITEM) {
				Log.i(TAG, "handler");
				SharedPreferences.Editor editor = pref.edit();			
				ListView listView = getListView();
				listView.invalidateViews();			
				int newPosition = musicService.mRetriever.listPosition - 1;			
				View vCurrent = listView.getChildAt(newPosition);
				Log.i(TAG, "handleMessage new position: " + newPosition);
				((TextView) vCurrent).setTextColor(Color.YELLOW);
				editor.putInt("listPosition", newPosition);	//save this position so its list item can be changed later
				editor.commit();
				listView.invalidateViews();
			}
		}
	};

	final Messenger mMessenger = new Messenger(handler);

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className,	IBinder service) {
			// We've bound to MusicService, cast the IBinder and get MusicService instance
			Log.i(TAG, "onServiceConnected");
			//			LocalBinder binder = (LocalBinder) service;
			musicService = MusicService.getService();
			mBound = true;			
			try {
				Message msg = Message.obtain(null, MusicService.MSG_REGISTER_CLIENT);
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
				int playerPosition = musicService.getPlayerPosition();
				int duration = musicService.getDuration();
				seekBar.setMax(duration);
				seekBar.setProgress(playerPosition);
				int remaining = duration - playerPosition;				
				textRemaining.setText(timeString(remaining));
				textDuration.setText(timeString(duration));
			}
			else Log.i(TAG, "musicService is NULL!");
		}
	};

	void startRepeatingTask() {
		mStatusChecker.run(); 
	}

	void stopRepeatingTask() {
		mHandler.removeCallbacks(mStatusChecker);
	}

	private String timeString(int totalSeconds) {
		int minutes = (totalSeconds)/60;
		int intSeconds = (totalSeconds)%60;
		String seconds = intSeconds > 9 ? "" + intSeconds : "0" + intSeconds;
		return (minutes + ":" + seconds);
	}

	private void updateSongList() {
		songs.clear();
		sdPath = getExternalFilesDir(null).getAbsolutePath() + "/";
		Log.i(TAG , sdPath);
		File sdPathFile = new File(sdPath);
		File[] files = sdPathFile.listFiles();
		Arrays.sort(files);
		if (files.length > 0) {
			for (File file : files) {
				songs.add(file.getName());
			}
			CustomAdapter songList = new CustomAdapter(this, R.layout.row, songs);
			setListAdapter(songList);
		}
	}

	@Override
	public void onClick(View v) {
		if (v == playButton) {
			startService(new Intent(MusicService.ACTION_PLAY));			
			playButton.setImageResource(R.drawable.play_button_pressed);   
			pauseButton.setImageResource(R.drawable.pause_button_normal);
		}
		else if (v == pauseButton) {
			startService(new Intent(MusicService.ACTION_PAUSE));
			pauseButton.setImageResource(R.drawable.pause_button_pressed);
			playButton.setImageResource(0);
			playButton.setImageResource(R.drawable.play_button_normal);
		}
		else if (v == nextButton) {
			startService(new Intent(MusicService.ACTION_SKIP));
			nextButton.setImageResource(R.drawable.next_button_pressed);
			nextButton.setImageResource(0); //restores button to "normal", where R.drawable.next_button_normal produces weird effects
			playButton.setImageResource(R.drawable.play_button_pressed);
			pauseButton.setImageResource(R.drawable.pause_button_normal);
		}	
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.i(TAG, "onListItemClick");
		Intent intent = new Intent(MusicService.ACTION_URL);
		intent.putExtra("fileName", songs.get(position));
		intent.putExtra("listPosition", position + 1);
		playButton.setImageResource(R.drawable.play_button_pressed);
		pauseButton.setImageResource(R.drawable.pause_button_normal);
		startService((intent));
	}

	private class DownloadWebPageTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			int index = 1;
			for (String url : urls) {
				DefaultHttpClient client = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(url);
				final String urlCopy = url;
				runOnUiThread(new Runnable() {
					public void run() {
						TextView urlText = (TextView) findViewById(R.id.urlText);
						urlText.setMovementMethod(new ScrollingMovementMethod());
						urlText.setText(urlCopy);
					}
				});						
				try {
					HttpResponse execute = client.execute(httpGet);
					InputStream content = execute.getEntity().getContent();
					byte[] buffer = new byte[1024];
					int length;
					File audioFile = new File(getExternalFilesDir(null), index + ".mp3");
					FileOutputStream out = new FileOutputStream(audioFile);
					while ((length = content.read(buffer)) > 0) {
						out.write(buffer, 0, length);
					}
					++index;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			updateSongList();
		}
	}
	
	public void readWebpage(View view) {	
		String showChoice;
		if (view.getId() == R.id.me) showChoice = "me";
		else showChoice = "atc";
		Log.i(TAG, "button text: " + ((Button)view).getText());
		Calendar calNow = Calendar.getInstance();
		int year = calNow.get(Calendar.YEAR);
		int intMonth = calNow.get(Calendar.MONTH) + 1;
		String month = intMonth > 9 ? "" + intMonth : "0" + intMonth;
		int intDay = calNow.get(Calendar.DATE);
		String day = intDay > 9 ? "" + intDay : "0" + intDay;
		String prepend;
		String urls[] = new String[25];
		for (int i = 0; i < 25; ++i) {
			if (i < 9) 
				prepend = "_0";
			else
				prepend = "_";
			urls[i] = "http://pd.npr.org/anon.npr-mp3/npr/" + showChoice + "/" + year + "/" + month + "/" + year + month + day + "_" + showChoice + prepend + (i + 1) + ".mp3";
			Log.i("NPR", urls[i]);
		}
//		(new DownloadWebPageTask()).execute(URL);
		downloadStories(view, urls);
	}

	private void downloadStories(View v, String[] urls) {
		for (String url : urls) {
			String[] urlSegments = url.split("/");
			String fileName = urlSegments[8];
			Uri uri=Uri.parse(url);
			DownloadManager.Request req=new DownloadManager.Request(uri);
			req.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI
					| DownloadManager.Request.NETWORK_MOBILE)
					.setAllowedOverRoaming(false)
					.setTitle(fileName)
					.setDescription("NPRDownload")
					.setDestinationInExternalFilesDir(this, null, fileName);
			downloadManager.enqueue(req);
			v.setEnabled(false);
			updateSongList();
		}
	}
	
	private BroadcastReceiver onDownloadEvent = new BroadcastReceiver() {
		public void onReceive(Context ctxt, Intent i) {
			if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(i.getAction())) {
//				Toast.makeText(ctxt, R.string.hi, Toast.LENGTH_LONG).show();
			}
			else {
				me.setEnabled(true);
				atc.setEnabled(true);
				Toast.makeText(ctxt, R.string.downloads_complete, Toast.LENGTH_LONG).show();
			}
		}
	};
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub

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