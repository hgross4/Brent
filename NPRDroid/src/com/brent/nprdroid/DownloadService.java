package com.brent.nprdroid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.app.IntentService;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class DownloadService extends IntentService {
	public static final String urlFileName = "file name from url";
	private static final int FOREGROUND_NOTIFICATION_ID = 10;
	private static final String TAG = "DownloadService";
	private Notification.Builder mBuilder;
	SharedPreferences pref;
	public static final String downloading = "downloading"; 
	public static final String downloadDone = "downloadDone"; 
	private static Intent downloadBroadcast = new Intent(downloading);
	public static final String whichShow = "whichShow";
	private static final String queryUrl = "http://api.npr.org/query?fields=title,audio&date=current&dateType=story&sort=assigned&output=NPRML&numResults=30&apiKey=MDA5NDM4NDA3MDEzMzY1MTMyNjA2NjIyMg001&id=";

	public DownloadService() {
		super("DownloadService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i(TAG, "onHandleIntent");
		BufferedReader reader=null;
		String rawXML = null;
		// Make the connection to the URL and get the xml as one big string
		try {
			int showID = intent.getExtras().getString(whichShow).equalsIgnoreCase("atc") ? 2 : 3;
			URL url = new URL(queryUrl + showID);
			Log.i(TAG, "url: " + url);
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("GET");
			connection.setReadTimeout(15000);
			connection.connect();
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuilder stringBuilder = new StringBuilder();
			String line = null;
			while ((line=reader.readLine()) != null) {
				stringBuilder.append(line + "\n");
			}
			rawXML = stringBuilder.toString();			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		// Parse the xml and populate the stories global list
		DocumentBuilder builder;
		Document doc = null;
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			doc = builder.parse(new InputSource(new StringReader(rawXML)));
		} catch (ParserConfigurationException e) {				
			e.printStackTrace();
		} catch (SAXException e) {				
			e.printStackTrace();
		} catch (IOException e) {				
			e.printStackTrace();
		}	

		NodeList storyNodes = doc.getElementsByTagName("story");
		Story story;
		for (int i = 0; i < storyNodes.getLength(); i++) {
			Element entry = (Element)storyNodes.item(i);
			NodeList children = entry.getChildNodes();
			story = new Story();
			for (int j = 0; j < children.getLength(); j++) {
				if (children.item(j).getNodeType() == Node.ELEMENT_NODE) {
					Element child = (Element) children.item(j);	
					Log.i(TAG, "child: " + child.getNodeName());
					if (child.getNodeName().equals("title")) {
						String title = child.getFirstChild().getNodeValue();
						story.setTitle(title);
					}
				}
			}
			MyApplication.stories.add(story);
		}
		
		NodeList formatNodes = doc.getElementsByTagName("format");
		for (int i = 0; i < formatNodes.getLength(); i++) {
			Element entry = (Element)formatNodes.item(i);
			NodeList children = entry.getChildNodes();
			for (int j = 0; j < children.getLength(); j++) {
				if (children.item(j).getNodeType() == Node.ELEMENT_NODE) {
					Element child = (Element) children.item(j);	
					Log.i(TAG, "child: " + child.getNodeName());
					if (child.getNodeName().equals("mp4")) {
						String audioLink = child.getFirstChild().getNodeValue();
						MyApplication.stories.get(i).setAudioLink(audioLink);
					}
				}
			}			
			Log.i(TAG, "title: " + MyApplication.stories.get(i).getTitle() + " audioLink: " + MyApplication.stories.get(i).getAudioLink());
		}
		
		// Delete the files currently in the directory
		String sdPath = getExternalFilesDir(null).getAbsolutePath() + "/";
		File sdPathFile = new File(sdPath);
		File[] files = sdPathFile.listFiles();
		if (files.length > 0) {
			for (File file : files) {
				file.delete();
			}
		}
		
		mBuilder = new Notification.Builder(this);
		mBuilder.setContentTitle("NPR stories download")
	    .setTicker("Starting NPR stories download")
	    .setSmallIcon(android.R.drawable.stat_sys_download);
		startForeground(FOREGROUND_NOTIFICATION_ID, mBuilder.getNotification());
		int index = 1;
//		ArrayList<String> urls = intent.getStringArrayListExtra("urls");
		for (int i = 0; i < MyApplication.stories.size(); ++i) {
			DefaultHttpClient client = new DefaultHttpClient();
			String url = MyApplication.stories.get(i).getAudioLink();
			Log.i(TAG, "url: " + url);
			HttpGet httpGet = new HttpGet(url);
			try {
				HttpResponse execute = client.execute(httpGet);
				InputStream content = execute.getEntity().getContent();
				byte[] buffer = new byte[1024];
				int length;
				String fileNameFromUrl = url.split("/")[8].split("\\?")[0];
				Log.i(TAG, "fileNameFromUrl: " + fileNameFromUrl);
//				String fileName = index > 9 ? "" + index + ".mp3" : "0" + index + ".mp3";
				File audioFile = new File(getExternalFilesDir(null), fileNameFromUrl);
				FileOutputStream out = new FileOutputStream(audioFile);
				while ((length = content.read(buffer)) > 0) {
					out.write(buffer, 0, length);
				}				
				mBuilder.setProgress(25, index, false)
				.setTicker("Downloading " + fileNameFromUrl)
				.setContentText(fileNameFromUrl);
				startForeground(FOREGROUND_NOTIFICATION_ID, mBuilder.getNotification());
				++index;
				sendBroadcast(downloadBroadcast);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mBuilder.setContentText("NPR stories download complete")
		.setProgress(0, 0, false) //remove the progress bar
		.setTicker("NPR stories download complete")
		.setSmallIcon(android.R.drawable.stat_sys_download_done);
		startForeground(FOREGROUND_NOTIFICATION_ID, mBuilder.getNotification());
		pref = getSharedPreferences("NPRDownloadPreferences", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();	
		editor.putInt("listPosition", 0);	//save this position so its list item can be changed later
		editor.commit();
		downloadBroadcast.putExtra(downloadDone, true);
		sendStickyBroadcast(downloadBroadcast);
	}

}
