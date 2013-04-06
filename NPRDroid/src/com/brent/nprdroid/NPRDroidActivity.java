package com.brent.nprdroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

public class NPRDroidActivity extends ListActivity implements OnTouchListener {
	private String sdPath; 
	private List<String> songs = new ArrayList<String>();
	private MyMediaController mc;
	private int currentPosition = 0;
	private String TAG = "NPRDroidActivity";
	ImageButton playButton, pauseButton;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		updateSongList();
		playButton = (ImageButton) findViewById(R.id.playButton);
		pauseButton = (ImageButton) findViewById(R.id.pauseButton);
		playButton.setOnTouchListener(this);
		pauseButton.setOnTouchListener(this);
	}

	private void updateSongList() {
		sdPath = getExternalFilesDir(null).getAbsolutePath() + "/";
		Log.i(TAG , sdPath);
		File sdPathFile = new File(sdPath);
		File[] files = sdPathFile.listFiles();
		if (files.length > 0) {
			for (File file : files) {
				songs.add(file.getName());
			}
			ArrayAdapter<String> songList = new ArrayAdapter<String>(this, R.layout.song_item, songs);
			setListAdapter(songList);
		}
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v == playButton) {
			startService(new Intent(MusicService.ACTION_PLAY));
			switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            	playButton.setImageResource(R.drawable.play_button_pressed);
            	break;
            case MotionEvent.ACTION_UP:
            	pauseButton.setImageResource(R.drawable.pause_button_normal);
			}
		}
		else if (v == pauseButton) {
			startService(new Intent(MusicService.ACTION_PAUSE));
			switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            	pauseButton.setImageResource(R.drawable.pause_button_pressed);
            	break;
            case MotionEvent.ACTION_UP:
            	playButton.setImageResource(R.drawable.play_button_normal);
			}
		}
		return false;
		
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.i(TAG, "onListItemClick");
		currentPosition = position;
//		playSong(sdPath + songs.get(position));
		Intent intent = new Intent(MusicService.ACTION_URL);
		intent.setData(Uri.fromFile(new File(getExternalFilesDir(null), songs.get(position))));
		intent.putExtra("listPosition", position + 1);
		((TextView) v).setTextColor(Color.BLUE);
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
			updateSongList();
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
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
		String URL[] = new String[25];
		for (int i = 0; i < 25; ++i) {
			if (i < 9) 
				prepend = "_0";
			else
				prepend = "_";
			URL[i] = "http://pd.npr.org/anon.npr-mp3/npr/" + showChoice + "/" + year + "/" + month + "/" + year + month + day + "_" + showChoice + prepend + (i + 1) + ".mp3";
			Log.i("NPR", URL[i]);
		}
		(new DownloadWebPageTask()).execute(URL);

	}
	
}