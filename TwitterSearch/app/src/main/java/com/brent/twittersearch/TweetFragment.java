package com.brent.twittersearch;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class TweetFragment extends Fragment {

    private static final String TAG = TweetFragment.class.getSimpleName();
    private static final String ACCESS_TOKEN = "access_token";
    private EditText keywords;
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "tweet_prefs";
    private TweetAdapter tweetAdapter;
    private long maxId = 0;
    private TextView getMore;
    private String searchTerms;
    private ArrayList<Tweet> tweets;


    public TweetFragment() {
        // Required empty public constructor
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList("tweets", tweets);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setRetainInstance(true);

        View rootView = inflater.inflate(R.layout.fragment_tweet, container, false);
        keywords = (EditText) rootView.findViewById(R.id.keywords);
        keywords.setOnEditorActionListener(searchEditorActionListener);
        keywords.setImeActionLabel("Search", KeyEvent.KEYCODE_ENTER);
        ImageView searchIcon = (ImageView) rootView.findViewById(R.id.search_icon);
        searchIcon.setOnClickListener(searchClickListener);
        ListView tweetList = (ListView) rootView.findViewById(R.id.tweet_list);
        getMore = (TextView) rootView.findViewById(R.id.get_more);
        getMore.setOnClickListener(getMoreClickListener);

        if(savedInstanceState != null && savedInstanceState.containsKey("tweets")) {
            tweets = savedInstanceState.getParcelableArrayList("tweets");
            tweetAdapter = new TweetAdapter(getActivity(), tweets);
            if (maxId > 0) {
                getMore.setVisibility(View.VISIBLE);
            }
        }
        else {
            tweets = new ArrayList<>();
        }

        tweetAdapter = new TweetAdapter(getActivity(), tweets);
        tweetList.setAdapter(tweetAdapter);

        preferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        getCredentials();

        return rootView;
    }

    private View.OnClickListener searchClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            doNewSearch();
        }
    };

    private TextView.OnEditorActionListener searchEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (event != null && event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            } else if (event == null
                    || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                doNewSearch();
            }
            return true;
        }
    };

    private void doNewSearch() {
        maxId = 0;
        getMore.setVisibility(View.GONE);
        searchTerms = keywords.getText().toString();
        load(searchTerms, maxId);
    }

    private View.OnClickListener getMoreClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            load(searchTerms, maxId);
        }
    };

    String accessToken;
    private void getCredentials() {
        accessToken = preferences.getString(ACCESS_TOKEN, "");
        if (accessToken.equals("")) {
            Ion.with(this)
                    .load("https://api.twitter.com/oauth2/token")
                            // embedding twitter api key and secret is a bad idea, but this isn't a real twitter app :)
                    .basicAuthentication("fx95oKhMHYgytSBmiAqQ", "0zfaijLMWMYTwVosdqFTL3k58JhRjZNxd2q0i9cltls")
                    .setBodyParameter("grant_type", "client_credentials")
                    .asJsonObject()
                    .setCallback(new FutureCallback<JsonObject>() {
                        @Override
                        public void onCompleted(Exception e, JsonObject result) {
                            if (e != null) {
                                Log.e(TAG, e.getLocalizedMessage());
                                return;
                            }
                            accessToken = result.get("access_token").getAsString();
                            SharedPreferences.Editor prefsEditor = preferences.edit();
                            prefsEditor.putString(ACCESS_TOKEN, accessToken);
                            prefsEditor.commit();
                        }
                    });
        }
    }

    Future<TweetCollection> loading;

    private void load(String keywords, final long maxId) {
        // don't attempt to load more if a load is already in progress
        if (loading != null && !loading.isDone() && !loading.isCancelled())
            return;

        // load the tweets
        String url = "https://api.twitter.com/1.1/search/tweets.json";
        String[] location = LocationFinder.getLocation();
        String longitude = location[0];
        String latitude = location[1];
        String radius = "5mi";

        FutureCallback tweetCallback = new FutureCallback<TweetCollection>() {
            @Override
            public void onCompleted(Exception e, TweetCollection tweetCollection) {
                if (e != null) {
                    Log.e(TAG, "" + e.getLocalizedMessage());
                    Toast.makeText(getActivity(),
                            getActivity().getString(R.string.internet_enabled) + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                // add the tweets
                if (tweetCollection != null && tweetCollection.tweets != null) {
                    List<Tweet> tweets = tweetCollection.tweets;
                    if (tweets.size() > 0) {
                        for (int i = 0; i < tweets.size(); i++) {
                            TweetFragment.this.tweets.add(tweets.get(i));
                        }
                        TweetFragment.this.maxId = tweets.get(tweets.size() - 1).id;
                        getMore.setVisibility(View.VISIBLE);
                        tweetAdapter.notifyDataSetChanged();
                    }

                    else {
                        String message = getActivity().getString(R.string.no_more_tweets);
                        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                        getMore.setVisibility(View.GONE);
                    }
                }
            }
        };


        if (maxId == 0) {
            TweetFragment.this.tweets.clear();
            loading = Ion.with(this)
                    .load(url)
                    .setHeader("Authorization", "Bearer " + accessToken)
                    .addQuery("q", keywords)
                    .addQuery("geocode", latitude + "," + longitude + "," + radius)
                    .as(new TypeToken<TweetCollection>() {
                    })
                    .setCallback(tweetCallback);
        }
        else {
            loading = Ion.with(this)
                    .load(url)
                    .setHeader("Authorization", "Bearer " + accessToken)
                    .addQuery("q", keywords)
                    .addQuery("geocode", latitude + "," + longitude + "," + radius)
                    .addQuery("max_id", Long.toString(maxId))
                    .as(new TypeToken<TweetCollection>() {
                    })
                    .setCallback(tweetCallback);
        }
    }


}
