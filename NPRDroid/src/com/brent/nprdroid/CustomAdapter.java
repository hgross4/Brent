package com.brent.nprdroid;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

class CustomAdapter extends ArrayAdapter<String> {
	SharedPreferences pref;
	CustomAdapter(Context context, int layout, ArrayList<String> list) {
		super(context, layout, list);
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
