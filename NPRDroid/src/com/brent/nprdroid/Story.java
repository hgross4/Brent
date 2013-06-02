package com.brent.nprdroid;

public class Story {

	private int id;
	private String title, audioLink;
	
	int getId() {
		return id;
	}

	String getTitle() {
		return title;
	}

	String getAudioLink() {
		return audioLink;
	}

	void setId(int i) {
		id = i;
	}

	void setTitle(String t) {
		title = t;
	}

	void setAudioLink(String al) {
		audioLink = al;
	}

}
