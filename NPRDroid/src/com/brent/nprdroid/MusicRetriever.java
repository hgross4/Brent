/*   
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.brent.nprdroid;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Retrieves and organizes media to play. Before being used, you must call {@link #prepare()},
 * which will retrieve all of the music on the user's device (by performing a query on a content
 * resolver). After that, it's ready to retrieve a random song, with its title and URI, upon
 * request.
 */
public class MusicRetriever {
    final String TAG = "MusicRetriever";

    ContentResolver mContentResolver;

    // the items (songs) we have queried
    List<Item> mItems = new ArrayList<Item>();
    int listPosition = 0;
    Random mRandom = new Random();

    public MusicRetriever(ContentResolver cr) {
        mContentResolver = cr;
    }

    /**
     * Loads music data. This method may take long, so be sure to call it asynchronously without
     * blocking the main thread.
     */
    public void prepare() {
    	String sdPath = "/sdcard/Android/data/com.brent.nprdroid/files/";
		Log.i(TAG , sdPath);
		File sdPathFile = new File(sdPath);
		File[] files = sdPathFile.listFiles();
		if (files.length > 0) {
			for (File file : files) {
				Log.i(TAG, file.getName());
				mItems.add(new Item(0, null, file.getName(), null, 0));
			}
		}
//        Uri uri = MediaStore.Files.getContentUri("external"); //Uri.parse("/mnt/sdcard/Android/data/com.example.android.musicplayer/files/"); /*Uri.fromFile(new File(); android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;*/
//        Log.i(TAG, "Querying media...");
//        Log.i(TAG, "URI: " + uri.toString());
//
//        // Perform a query on the content resolver. The URI we're passing specifies that we
//        // want to query for all audio media on external storage (e.g. SD card)
//        String selectionMimeType = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
//        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3");
//        String[] selectionArgsMp3 = new String[]{ mimeType };
//        Cursor cur = mContentResolver.query(uri, null, selectionMimeType, selectionArgsMp3, null);
////        Cursor cur =  mContentResolver.query(uri, null, selection /*MediaStore.Audio.Media.IS_MUSIC + " = 1"*/, null, null);
//        Log.i(TAG, "Query finished. " + (cur == null ? "Returned NULL." : "Returned a cursor."));
//
//        if (cur == null) {
//            // Query failed...
//            Log.e(TAG, "Failed to retrieve music: cursor is null :-(");
//            return;
//        }
//        if (!cur.moveToFirst()) {
//            // Nothing to query. There is no music on the device. How boring.
//            Log.e(TAG, "Failed to move cursor to first row (no query results).");
//            return;
//        }
//
//        Log.i(TAG, "Listing...");
//
//        // retrieve the indices of the columns where the ID, title, etc. of the song are
//        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
//        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
//        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
//        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
//        int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);
//
//        Log.i(TAG, "Title column index: " + String.valueOf(titleColumn));
//        Log.i(TAG, "ID column index: " + String.valueOf(titleColumn));
//
//        // add each song to mItems
//        do {
//            Log.i(TAG, "ID: " + cur.getString(idColumn) + " Title: " + cur.getString(titleColumn) + "duration: " + cur.getLong(durationColumn));
//            mItems.add(new Item(
//                    cur.getLong(idColumn),
//                    cur.getString(artistColumn),
//                    cur.getString(titleColumn),
//                    cur.getString(albumColumn),
//                    cur.getLong(durationColumn)));
//        } while (cur.moveToNext());
//
//        Log.i(TAG, "Done querying media. MusicRetriever is ready.");
    }

    public ContentResolver getContentResolver() {
        return mContentResolver;
    }

    /** Returns a random Item. If there are no items available, returns null. */
    public Item getRandomItem() {
        if (mItems.size() <= 0) return null;
        return mItems.get(mRandom.nextInt(mItems.size()));
    }
    
    public Item getNextItem() {
    	if (mItems.size() <= 0 || listPosition == (mItems.size() - 1)) return null;
        return mItems.get(listPosition++);
    }

    public static class Item {
        long id;
        String artist;
        String title;
        String album;
        long duration;

        public Item(long id, String artist, String title, String album, long duration) {
            this.id = id;
            this.artist = artist;
            this.title = title;
            this.album = album;
            this.duration = duration;
        }

        public long getId() {
            return id;
        }

        public String getArtist() {
            return artist;
        }

        public String getTitle() {
            return title;
        }

        public String getAlbum() {
            return album;
        }

        public long getDuration() {
            return duration;
        }

        public Uri getURI() {
            return ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        }
    }
}
