package com.brent.nprdroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class DownloadService extends IntentService {
	public static final String broadcast = "nprdownload.broadcast";
	public static final String urlFileName = "file name from url";
	SharedPreferences pref;

	public DownloadService() {
		super("DownloadService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification.Builder mBuilder = new Notification.Builder(this);
		mBuilder.setContentTitle("NPR Stories download")
	    .setContentText("Download in progress")
	    .setSmallIcon(android.R.drawable.stat_sys_download);
		Intent progressIntent = new Intent(broadcast);
		int index = 1;
		ArrayList<String> urls = intent.getStringArrayListExtra("urls");
		for (String url : urls) {
			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(url);
			try {
				HttpResponse execute = client.execute(httpGet);
				InputStream content = execute.getEntity().getContent();
				byte[] buffer = new byte[1024];
				int length;
				String fileName = index > 9 ? "" + index + ".mp3" : "0" + index + ".mp3";
				File audioFile = new File(getExternalFilesDir(null), fileName);
				FileOutputStream out = new FileOutputStream(audioFile);
				while ((length = content.read(buffer)) > 0) {
					out.write(buffer, 0, length);
				}
				String fileNameFromUrl = url.split("/")[8];
				mBuilder.setProgress(25, index, false)
				.setContentText(fileNameFromUrl);                
                mNotifyManager.notify(0, mBuilder.getNotification());
                progressIntent.putExtra(urlFileName, fileNameFromUrl);
                sendBroadcast(progressIntent);
				++index;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mBuilder.setContentText("NPR Stories download complete")
		.setProgress(0, 0, false); //remove the progress bar
        mNotifyManager.notify(0, mBuilder.getNotification());
		pref = getSharedPreferences("NPRDownloadPreferences", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();	
		editor.putInt("listPosition", 0);	//save this position so its list item can be changed later
		editor.commit();
		progressIntent.putExtra(urlFileName, "complete");
		sendBroadcast(progressIntent);
	}

}
