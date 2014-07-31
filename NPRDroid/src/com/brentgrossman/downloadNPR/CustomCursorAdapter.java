package com.brentgrossman.downloadNPR;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

class CustomCursorAdapter extends SimpleCursorAdapter {
	SharedPreferences pref;
	CustomCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
		super(context, layout, c, from, to, flags);
		pref = context.getSharedPreferences("NPRDownloadPreferences", Context.MODE_PRIVATE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = super.getView(position, convertView, parent);
		ViewHolder holder = (ViewHolder)row.getTag();		

		if (holder == null) {                         
			holder = new ViewHolder(row);
			row.setTag(holder);
		}

		if (position == pref.getInt("listPosition", 0)) {
			holder.story.setTextColor(Color.YELLOW);
		} 
		else {
			holder.story.setTextColor(Color.LTGRAY);
		}
		return(row);
	}
}
