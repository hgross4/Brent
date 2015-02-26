package com.brent.twittersearch;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Brent on 2/21/15.
 */
public class TweetCollection {

    @SerializedName("statuses")
    public List<Tweet> tweets;
}
