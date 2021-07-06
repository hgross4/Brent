package com.brentgrossman.publicradiounplugged.internet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.brentgrossman.publicradiounplugged.data.CProvider;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.SQLException;
import android.util.Log;

public class PopulateAvailable extends IntentService {

	private static final String TAG = PopulateAvailable.class.getSimpleName();
	public static final String whichShow = "whichShow";
	private static final String queryUrl = 
			"http://api.npr.org/query?fields=title,audio&date=current&dateType=story&sort=assigned&output=json&numResults=30&apiKey=MDA5NDM4NDA3MDEzMzY1MTMyNjA2NjIyMg001&id=";

	public PopulateAvailable() {
		super("PopulateAvailable");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		retrieveStoryData(intent);
	}

	private void retrieveStoryData(Intent intent) {
		// Delete from database stories currently in list
		getContentResolver().delete(CProvider.Stories.CONTENT_URI,
				CProvider.Stories.DOWNLOADED + " IS NULL OR " + CProvider.Stories.DOWNLOADED + " != ? ", new String[] {"1"});

		BufferedReader reader = null;
		String storyDataString = null;
		// Make the connection to the URL and get the xml as one big string
		try {
			assert intent != null;
			int showID = intent.getIntExtra(whichShow, 2);
			URL url = new URL(queryUrl + showID);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setReadTimeout(15000);
			connection.connect();
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuilder stringBuilder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line).append("\n");
			}
			storyDataString = stringBuilder.toString();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		getStoryInfoFromJson(storyDataString);
	}

	private void getStoryInfoFromJson(String storyDataString) {
		try {
			JSONObject storyJson = new JSONObject(storyDataString);
			JSONArray stories = storyJson.getJSONObject("list").getJSONArray("story");
			for (int i=0; i < stories.length(); i++) {
				JSONObject story = stories.getJSONObject(i);
				String title = story.getJSONObject("title").getString("$text");
				// todo: check if "mediastream" key exists for story, and if not, continue to the next story
				String audioLink = story.getJSONArray("audio")
						.getJSONObject(0)
						.getJSONObject("format")
						.getJSONObject("mediastream")
						.getString("$text");
				audioLink = "https://ondemand.npr.org/npr-mp4/npr/" + audioLink.split("/npr/")[1];
				audioLink = audioLink.replace("mp3", "mp4");
				if (title != null) {
					saveStoryInfo(title, audioLink);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void saveStoryInfo(String title, String audioLink) {
		ContentValues values = new ContentValues();
		values.put(CProvider.Stories.TITLE, title);
		values.put(CProvider.Stories.AUDIO_LINK, audioLink);
		try {
			getContentResolver().insert(CProvider.Stories.CONTENT_URI, values);
		} catch (SQLException e) {
			Log.e("PopulateAvailable", "Row could not be inserted: " + e.getLocalizedMessage());
		}
	}
}