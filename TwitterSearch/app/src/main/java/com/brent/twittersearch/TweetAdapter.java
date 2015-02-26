package com.brent.twittersearch;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.koushikdutta.ion.Ion;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Brent on 2/22/15.
 */
public class TweetAdapter extends ArrayAdapter<Tweet> {

    private static final String TAG = TweetAdapter.class.getSimpleName();
    Context context;
    List<Tweet> tweets;

    public TweetAdapter(Context context, List<Tweet> tweets) {
        super(context, R.layout.tweet_row, tweets);
        this.context = context;
        this.tweets = tweets;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        Tweet tweet = tweets.get(position);
        if (convertView == null) {
            convertView =
                    LayoutInflater.from(context).inflate(R.layout.tweet_row, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Get the author's profile image and load it into the ImageView
        Ion.with(holder.authorImage)
                .placeholder(R.drawable.twitter)
                .load(tweet.user.profileImageUrl);

        holder.authorScreenName.setText(tweet.user.screenName);

        // Get timestamp as an elapsed time
        final String TWITTER="EEE MMM dd HH:mm:ss ZZZZZ yyyy";
        SimpleDateFormat sf = new SimpleDateFormat(TWITTER);
        sf.setLenient(true);
        Date tweetDate = new Date();
        try {
            tweetDate = sf.parse(tweet.createdAt);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Date now = new Date();
        long diff = now.getTime() - tweetDate.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        String timeStamp;
        if (days > 0) {
            timeStamp = days + "d";
        }
        else if (hours > 0) {
            timeStamp = hours +"h";
        }
        else if (minutes > 0) {
            timeStamp = minutes + "m";
        }
        else {
            timeStamp = seconds + "s";
        }
        holder.timeStamp.setText(timeStamp);

        holder.tweetText.setText(tweet.text);

        // Prevent image from other row incorrectly showing
        holder.inlinePhoto.setImageDrawable(null);
        // Get inline photo if there is one
        if (tweet.entities != null && tweet.entities.media != null) {
            List<Media> media = tweet.entities.media;
            for (int i = 0; i < media.size(); ++i) {
                String mediaUrl = media.get(i).mediaUrl;
                Log.wtf(TAG, tweet.user.screenName + mediaUrl);
                if (mediaUrl != null) {
                    Ion.with(holder.inlinePhoto)
                            .load(mediaUrl);
                }
            }
        }

        holder.location.setText(tweet.user.location);

        holder.reply.setOnClickListener(replyClickListener);
        holder.reply.setTag(position);

        return convertView;
    }

    private View.OnClickListener replyClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Tweet tweet = getItem((Integer) v.getTag());
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            })
                    .setTitle("Tweet Information")
                    .setMessage(tweet.text + "\n Tweet ID: " + tweet.id + "\n User ID: " + tweet.user.id);
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    };

    static class ViewHolder {
        ImageView authorImage = null;
        TextView authorScreenName = null;
        TextView timeStamp = null;
        TextView tweetText = null;
        ImageView inlinePhoto = null;
        TextView location = null;
        TextView reply = null;

        ViewHolder(View row) {
            this.authorImage = (ImageView) row.findViewById(R.id.author_image);
            this.authorScreenName = (TextView) row.findViewById(R.id.author_screen_name);
            this.timeStamp = (TextView) row.findViewById(R.id.timestamp);
            this.tweetText = (TextView) row.findViewById(R.id.tweet_text);
            this.inlinePhoto = (ImageView) row.findViewById(R.id.inline_photo);
            this.location = (TextView) row.findViewById(R.id.location);
            this.reply = (TextView) row.findViewById(R.id.reply_button);
        }
    }
}
