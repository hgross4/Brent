package com.brent.feedreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.brent.feedreader.util.SystemUiHider;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class MainActivity extends Activity implements AdapterView.OnItemSelectedListener {
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 5000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;	

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

	public static final String TAG = "FeedReader";	
	private Spinner titleSpinner;
	private WebView articleBodyView;
	private ArrayList<String> articleTitles;
	private ArrayList<Article> articles;
	private String appTitle;	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.activity_main);
		titleSpinner = (Spinner)findViewById(R.id.title_spinner); 
		titleSpinner.setOnItemSelectedListener(this);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		articleBodyView = (WebView) findViewById(R.id.article_body_webview);
		articleBodyView.getSettings().setBuiltInZoomControls(true); //this line allows zooming in and out of view
		articleBodyView.getSettings().setDisplayZoomControls(false);
		articleBodyView.getSettings().setJavaScriptEnabled(true);	//needed for progress indication
		articleBodyView.setWebChromeClient(new WebChromeClient() { 
			//Show progress when loading page, since it takes a little while
			public void onProgressChanged(WebView view, int progress) {
				MainActivity.this.setTitle("Loading page...");
				MainActivity.this.setProgress(progress * 100);
				if(progress == 100) {
					MainActivity.this.setTitle(appTitle);
				}
			}
		});

		// Get the articles. Network "stuff" needs to be done outside of the UI thread:
		if (isOnline()) {
			(new FetchArticlesTask()).execute("http://feeds2.feedburner.com/TheTechnologyEdge");
		}
		else {
			Toast.makeText(getApplicationContext(), "No Internet connection. FeedReader has terminated.", Toast.LENGTH_LONG).show();
			finish();
		}

		// Code from here to "end" generated automatically when project created,
		// to hide and show title and status bars (i.e., run app full screen).
		// All code in com.brent.feedreader.util package also automatically generated.

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, articleBodyView, HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
			// Cached values.
			int mControlsHeight;
			int mShortAnimTime;

			@Override
			@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
			public void onVisibilityChange(boolean visible) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
					// If the ViewPropertyAnimator API is available
					// (Honeycomb MR2 and later), use it to animate the
					// in-layout UI controls at the bottom of the
					// screen.
					if (mControlsHeight == 0) {
						mControlsHeight = controlsView.getHeight();
					}
					if (mShortAnimTime == 0) {
						mShortAnimTime = getResources().getInteger(
								android.R.integer.config_shortAnimTime);
					}
					controlsView
					.animate()
					.translationY(visible ? 0 : mControlsHeight)
					.setDuration(mShortAnimTime);
				} else {
					// If the ViewPropertyAnimator APIs aren't
					// available, simply show or hide the in-layout UI
					// controls.
					controlsView.setVisibility(visible ? View.VISIBLE
							: View.GONE);
				}

				if (visible && AUTO_HIDE) {
					// Schedule a hide().
					delayedHide(AUTO_HIDE_DELAY_MILLIS);
				}
			}
		});

		// Set up the user interaction to manually show or hide the system UI.
		articleBodyView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.title_spinner).setOnTouchListener(
				mDelayHideTouchListener);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	// End automatically generated code to run app full screen

	@Override
	public void onResume() {
		super.onResume();
	}

	public boolean isOnline() {
		ConnectivityManager cm =
			(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	private class FetchArticlesTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... urls) {
			BufferedReader reader=null;
			String rawXML = null;

			// Make the connection to the URL and get the xml as one big string
			try {
				URL url = new URL(urls[0]);
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

			// Parse the xml and populate the lists articleTitles (for the spinner) and articles (to store all article content)
			articleTitles = new ArrayList<String>();
			articles = new ArrayList<Article>();

			Article article = new Article();
			article.setTitle("Reader Instructions");
			article.setLink("");
			article.setTimestamp("");
			article.setBody("Touch screen to display article-selection list at bottom." +
					"<br><br>NOTE:<br>Video not supported. To play video, touch title link at top of article " +
			" to view article, and play its video, in your browser.<br><br>");
			articles.add(article);
			articleTitles.add("Reader Instructions");
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

			NodeList entries = doc.getElementsByTagName("entry");
			for (int i = 0; i < entries.getLength(); i++) {
				Element entry = (Element)entries.item(i);
				NodeList children = entry.getChildNodes();
				article = new Article();
				for (int j = 0; j < children.getLength(); j++) {
					Element child = (Element) children.item(j);					
					if (child.getNodeName().equals("title")) {
						String title = child.getFirstChild().getNodeValue();
						articleTitles.add(title);
						article.setTitle(title);
					}
					else if (child.getNodeName().equals("link") && child.getAttribute("rel").equals("alternate")) {						
						String articleLink = ("<a href=" + "`" + child.getAttribute("href") + "`" + "target=" + "`"+ "_blank" 
								+ "`" + ">" + child.getAttribute("title") + "</a>").replace('`', '"');
						article.setLink(articleLink);
					}
					else if (child.getNodeName().equals("content")) {
						article.setBody(child.getFirstChild().getNodeValue());
					}
					else if (child.getNodeName().equals("updated")) {
						article.setTimestamp(child.getFirstChild().getNodeValue());
					}
				}
				articles.add(article);
				Log.i(TAG, "title: " + articles.get(i + 1).getTitle());
			}

			Element feed = (Element) doc.getElementsByTagName("feed").item(0);
			NodeList feedChildren = feed.getChildNodes();
			for (int i = 0; i < feedChildren.getLength(); i++) {
				Element child = (Element) feedChildren.item(i);
				if (child.getNodeName().equals("title")) {
					appTitle = child.getFirstChild().getNodeValue();
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(String string) {
			// Populate the spinner with the article titles
			ArrayAdapter<String> aa = new ArrayAdapter<String>(MainActivity.this, 
					android.R.layout.simple_spinner_item, articleTitles);
			aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			titleSpinner.setAdapter(aa);
			MainActivity.this.setTitle(appTitle);
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View view, int position, long id) {
		// Load the article bodies and external links into the WebView for display
		String articleContent = "<br><br>" + articles.get(position).getLink() + "<br>" 
		+ articles.get(position).getTimestamp() + "<p>" + articles.get(position).getBody() + "<br><br>";
		articleBodyView.loadData(articleContent, "text/html", "UTF-8");
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}
}
