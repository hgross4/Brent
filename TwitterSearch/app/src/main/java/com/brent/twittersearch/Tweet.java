package com.brent.twittersearch;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Brent on 2/21/15.
 */
public class Tweet implements Parcelable {

    int mData;

    long id;

    String text;

    User user;

    @SerializedName("created_at")
    String createdAt;

    Entity entities;

    public int describeContents() {
        return 0;
    }

    /** save object in parcel */
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mData);
    }

    public static final Parcelable.Creator<Tweet> CREATOR
            = new Parcelable.Creator<Tweet>() {
        public Tweet createFromParcel(Parcel in) {
            return new Tweet(in);
        }

        public Tweet[] newArray(int size) {
            return new Tweet[size];
        }
    };

    /** recreate object from parcel */
    private Tweet(Parcel in) {
        mData = in.readInt();
    }
}
