package com.brentgrossman.downloadnpr.internet;

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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.brentgrossman.downloadnpr.data.CProvider;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.SQLException;
import android.util.Log;
import android.widget.Toast;

public class PopulateAvailable extends IntentService {

	private static final String TAG = PopulateAvailable.class.getSimpleName();
	public static final String whichShow = "whichShow";
	private static final String queryUrl = 
			"http://api.npr.org/query?fields=title,audio&date=current&dateType=story&sort=assigned&output=NPRML&numResults=30&apiKey=MDA5NDM4NDA3MDEzMzY1MTMyNjA2NjIyMg001&id=";

	public PopulateAvailable() {
		super("PopulateAvailable");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		// Delete from database stories currently in list
		getContentResolver().delete(CProvider.Stories.CONTENT_URI, 
				CProvider.Stories.DOWNLOADED + " IS NULL OR " + CProvider.Stories.DOWNLOADED + " != ? ", new String[] {"1"});
		
		BufferedReader reader = null;
		String rawXML = null;
		// Make the connection to the URL and get the xml as one big string
		try {
			int showID = intent.getIntExtra(whichShow, 2);
			URL url = new URL(queryUrl + showID);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setReadTimeout(15000);
			connection.connect();
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuilder stringBuilder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line + "\n");
			}
			rawXML = stringBuilder.toString();
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

		// Parse the xml
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
		NodeList formatNodes = doc.getElementsByTagName("format");
		for (int i = 0; i < storyNodes.getLength(); i++) {
			String title = null;
			String audioLink = null;
			Element storyEntry = (Element)storyNodes.item(i);
			NodeList storyChildren = storyEntry.getChildNodes();
			Element formatEntry = (Element)formatNodes.item(i);
			if (formatEntry != null) {
				NodeList formatChildren = formatEntry.getChildNodes();
				for (int j = 0; j < formatChildren.getLength(); j++) {
					if (storyChildren.item(j).getNodeType() == Node.ELEMENT_NODE) {
						Element child = (Element) storyChildren.item(j);
						if (child.getNodeName().equals("title")) {
							title = child.getFirstChild().getNodeValue();
						}
					}
					if (formatChildren.item(j).getNodeType() == Node.ELEMENT_NODE) {
						Element child = (Element) formatChildren.item(j);
						if (child.getNodeName().equals("mp4")) {
							audioLink = child.getFirstChild().getNodeValue();
						}
					}
				}
				if (title != null && audioLink != null) {
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
			else {
				Toast.makeText(this, "Stories not available", Toast.LENGTH_LONG).show();
				Log.e(TAG, "Not all stories are available yet or something else is wrong on NPR's side");
			}
		}
	}
}