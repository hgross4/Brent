package com.brentgrossman.publicradiounplugged.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

public class CProvider extends ContentProvider {

	private static final int STORIES = 1;
	private static final int STORY_ID = 2;
	private static final UriMatcher MATCHER;
	private static final String TABLE = "stories";

	public static final class Stories implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://com.brentgrossman.publicradiounplugged/stories");
		// public static final String DEFAULT_SORT_ORDER = "title";
		public static final String TITLE = "title";
		public static final String AUDIO_LINK = "audio_link";
		public static final String FILE_NAME = "file_name";
		public static final String SELECTED = "selected";
		public static final String DOWNLOADED = "downloaded";
        public static final String PERCENTAGE_PLAYED = "percentage_played";
	}

	static {
		MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		MATCHER.addURI("com.brentgrossman.publicradiounplugged", "stories", STORIES);
		MATCHER.addURI("com.brentgrossman.publicradiounplugged", "stories/#", STORY_ID);
	}

	private DBHelper db = null;

	@Override
	public boolean onCreate() {
		db = new DBHelper(getContext());
		return ((db == null) ? false : true);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(TABLE);
		Cursor cursor = queryBuilder.query(db.getReadableDatabase(), projection, selection, selectionArgs, null, null,
				sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long rowID = db.getWritableDatabase().insert(TABLE, null, values);
		if (rowID > 0) {
			Uri uriFromInsert = ContentUris.withAppendedId(Stories.CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(uriFromInsert, null);
			return uriFromInsert;
		}
		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = db.getWritableDatabase().delete(TABLE, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int count = db.getWritableDatabase().update(TABLE, values, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

}
