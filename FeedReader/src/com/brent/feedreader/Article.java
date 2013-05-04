package com.brent.feedreader;

public class Article {
	private String title, link, timestamp, body;
	
	String getTitle() {
		return title;
	}
	
	String getLink() {
		return link;
	}
	
	String getTimestamp() {
		return timestamp;
	}
	
	String getBody() {
		return body;
	}
	
	void setTitle(String t) {
		title = t;
	}
	
	void setLink(String l) {
		link = l;
	}
	
	void setTimestamp(String ts) {
		timestamp = ts;
	}
	
	void setBody(String b) {
		body = b;
	}
}
