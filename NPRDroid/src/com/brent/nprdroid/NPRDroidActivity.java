package com.brent.nprdroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class NPRDroidActivity extends Activity {

	String showChoice, URL;
	RadioGroup radioGroupShows;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		radioGroupShows = (RadioGroup) findViewById(R.id.radioGroupShows);
		//		The below lines commented because the button's onclick is specified in main.xml now
		//		Button buttonDownload = (Button) findViewById(R.id.buttonDownload);
		//		buttonDownload.setOnClickListener(new OnClickListener() {
		//			public void onClick(View v) {
		//				readWebpage(v);
		//			}
		//		});
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
						urlText.setText(urlText.getText() + urlCopy + "\n\n");
					}
				});						
				try {
					HttpResponse execute = client.execute(httpGet);
					InputStream content = execute.getEntity().getContent();
					byte[] buffer = new byte[1024];
				    int length;
					FileOutputStream out = new FileOutputStream(new File(getExternalFilesDir(null), index + ".mp3"));
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
		}
	}

	public void readWebpage(View view) {
		int selectedId = radioGroupShows.getCheckedRadioButtonId();
		RadioButton selectedButton = (RadioButton) findViewById(selectedId);
		if (selectedButton.getText().equals("Morning Edition")) showChoice = "me";
		else showChoice = "atc";
		Log.i("NPR", "button text: " + selectedButton.getText());
		Calendar calNow = Calendar.getInstance();
		int year = calNow.get(Calendar.YEAR);
		int intMonth = calNow.get(Calendar.MONTH) + 1;
		String month = intMonth > 9 ? "" + intMonth : "0" + intMonth;
		int intDay = calNow.get(Calendar.DATE);
		String day = intDay > 9 ? "" + intDay : "0" + intDay;
		String prepend;
		String URL[] = new String[30];
		for (int i = 0; i < 30; ++i) {
			if (i < 9) 
				prepend = "_0";
			else
				prepend = "_";
			URL[i] = "http://pd.npr.org/anon.npr-mp3/npr/" + showChoice + "/" + year + "/" + month + "/" + year + month + day + "_" + showChoice + prepend + (i + 1) + ".mp3";
			Log.i("NPR", URL[i]);
		}
		DownloadWebPageTask task = new DownloadWebPageTask();
		task.execute(URL);

	}
}