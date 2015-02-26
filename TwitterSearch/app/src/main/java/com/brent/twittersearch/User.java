package com.brent.twittersearch;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Brent on 2/21/15.
 */
public class User {

    long id;

    @SerializedName("screen_name")
    String screenName;

    @SerializedName("profile_image_url")
    String profileImageUrl;

    String location;
}
