package com.brentgrossman.downloadNPR;

import android.view.View;
import android.widget.TextView;

class ViewHolder {
	TextView story = null;

	ViewHolder(View row) {
		this.story=(TextView)row.findViewById(R.id.story);
	}
}
