package com.brentgrossman.downloadnpr.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
	
	private static final String DATABASE_NAME = "stories.db";

	public DBHelper(Context context) {
	    super(context, DATABASE_NAME, null, 1);
	  }

	@Override
	public void onCreate(SQLiteDatabase db) {
		Cursor c=db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='stories'", null);
	    
	    try {
	      if (c.getCount()==0) {
	        db.execSQL("CREATE TABLE stories (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
	        		+ "title TEXT UNIQUE, audio_link TEXT, file_name TEXT, selected INTEGER, downloaded INTEGER);");
	      }
	    }
	    finally {
	      c.close();
	    }
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	    db.execSQL("DROP TABLE IF EXISTS stories");
	    onCreate(db);
		
	}

}
