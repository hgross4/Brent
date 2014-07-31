package com.brentgrossman.downloadNPR;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

public class DownloadStories extends IntentService {
	private static final int FOREGROUND_NOTIFICATION_ID = 549; //arbitrary number
	public static final String downloading = "downloading"; 
	public static final String downloadDone = "downloadDone";

	public DownloadStories() {
		super("DownloadStories");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Builder mBuilder;
		mBuilder = new NotificationCompat.Builder(this);
		mBuilder.setContentTitle("NPR stories download")
		.setTicker("Starting NPR stories download")
		.setSmallIcon(android.R.drawable.stat_sys_download);
		final Intent notificationIntent = new Intent(this, DownloadNPRActivity.class);
        final PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        mBuilder.setContentIntent(pi);
        final Notification notification = mBuilder.build();
		startForeground(FOREGROUND_NOTIFICATION_ID, notification);
		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet httpGet;
		Intent downloadBroadcast = new Intent(downloading);
		Cursor cursor = getContentResolver().query(CProvider.Stories.CONTENT_URI, 
				new String[] {CProvider.Stories._ID, CProvider.Stories.AUDIO_LINK}, 
				CProvider.Stories.SELECTED + " = ? ", new String[] {"1"}, null);
		if (cursor != null) {
			ContentValues values = new ContentValues();			
			SharedPreferences pref;
			pref = getSharedPreferences("NPRDownloadPreferences", Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = pref.edit();	
			int fileCounter = 0;
			String fileOrdinal = null;
			String fileNameFromUrl = null;
			while (cursor.moveToNext()) {
				String audioLink = cursor.getString(cursor.getColumnIndex(CProvider.Stories.AUDIO_LINK));
				httpGet = new HttpGet(audioLink);
				try {
					HttpResponse execute = client.execute(httpGet);
					InputStream content = execute.getEntity().getContent();
					byte[] buffer = new byte[1024];
					int length;
//					fileOrdinal = fileCounter < 10 ? "0" + fileCounter : "" + fileCounter;
					fileOrdinal = cursor.getString(cursor.getColumnIndex(CProvider.Stories._ID));
					fileNameFromUrl = fileOrdinal + "_" + audioLink.split("/")[8].split("\\?")[0];
					File audioFile = new File(getExternalFilesDir(null), fileNameFromUrl);
					FileOutputStream out = new FileOutputStream(audioFile);
					while ((length = content.read(buffer)) > 0) {
						out.write(buffer, 0, length);
					}
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				mBuilder.setProgress(25, fileCounter, false)
				.setTicker("Downloading " + fileNameFromUrl)
				.setContentText(fileNameFromUrl);
				startForeground(FOREGROUND_NOTIFICATION_ID, mBuilder.build());
				downloadBroadcast.putExtra(downloadDone, false);
				sendBroadcast(downloadBroadcast);
				// Set highlighted story to first story
				if (fileCounter == 0) {
					editor.putInt("listPosition", 0);
					editor.commit();
				}
				values.put(CProvider.Stories.FILE_NAME, fileNameFromUrl);
				values.put(CProvider.Stories.DOWNLOADED, 1);
				values.put(CProvider.Stories.SELECTED, 0);
				getContentResolver().update(CProvider.Stories.CONTENT_URI, values, CProvider.Stories._ID + " = ? ", 
						new String[] {cursor.getString(cursor.getColumnIndex(CProvider.Stories._ID))});
				++fileCounter;
			}
			mBuilder.setContentText("NPR stories download complete")
			.setProgress(0, 0, false) //remove the progress bar
			.setTicker("NPR stories download complete")
			.setSmallIcon(android.R.drawable.stat_sys_download_done);
			startForeground(FOREGROUND_NOTIFICATION_ID, mBuilder.build());
			downloadBroadcast.putExtra(downloadDone, true);
			sendStickyBroadcast(downloadBroadcast);
			cursor.close();
		}

	}

}
