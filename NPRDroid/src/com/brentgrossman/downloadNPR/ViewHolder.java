package com.brentgrossman.downloadNPR;

import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

class ViewHolder {
	CheckBox storyCheckBox;
	TextView story = null;

	ViewHolder(View row) {
		storyCheckBox = (CheckBox) row.findViewById(R.id.story_check_box);
		story = (TextView) row.findViewById(R.id.story);
	}
}
