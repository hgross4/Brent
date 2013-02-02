/*
 * npr.c
 *
 *  Created on: May 8, 2012
 *      Author: Brent
 *
 *  Has to be compiled using "gcc -lcurl npr.c"
 */

#define CURL_STATICLIB
#include <stdio.h>
#include <curl/curl.h>
#include <curl/types.h>
#include <curl/easy.h>
#include <string.h>
#include <ctime>
#include <iostream>
#include <jni.h>
#include <android/log.h>

size_t write_data(void *ptr, size_t size, size_t nmemb, FILE *stream) {
	size_t written;
	written = fwrite(ptr, size, nmemb, stream);
	return written;
}

void Java_com_brent_npr_NPRDroidActivity_downloadPodcasts(JNIEnv* env, jobject javaThis, jint choice) {
	time_t t = time(0);   // get time now
	struct tm * now = localtime( & t );
	CURL *curl;
	FILE *fp;
	CURLcode res;
	char *url = (char *)malloc(512);
	char urlArray[100], outfile[FILENAME_MAX], day[16], month[16], program[16];
	if (choice == 1) sprintf(program, "%s", "me");
	else sprintf(program, "%s", "atc");
	for (i = 1; i < 10; i++) {
		if (now->tm_mday < 10) sprintf(day, "%d%d", 0, now->tm_mday);
		else sprintf(day, "%d", now->tm_mday);
		if (now->tm_mon + 1 < 10) sprintf(month, "%d%d", 0, now->tm_mon + 1);
		else sprintf(month, "%d", now->tm_mon + 1);
		sprintf(urlArray, "http://pd.npr.org/anon.npr-mp3/npr/%s/%d/%s/%d%s%s_%s_0%d.mp3",
				program, now->tm_year + 1900, month, now->tm_year + 1900, month, day, program, i);
		strcpy(url, urlArray);
		__android_log_print(ANDROID_LOG_INFO, "URL", url);
		sprintf(outfile, "/sdcard/Android/data/com.dropbox.android/files/scratch/NPR audio/%d.mp3", i);
		__android_log_print(ANDROID_LOG_INFO, "output file",  outfile);
		curl = curl_easy_init();
		if (curl) {
			fp = fopen(outfile,"wb");
			curl_easy_setopt(curl, CURLOPT_URL, url);
			curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_data);
			curl_easy_setopt(curl, CURLOPT_WRITEDATA, fp);
			res = curl_easy_perform(curl);
			curl_easy_cleanup(curl);
			fclose(fp);
		}
	}
	__android_log_print(ANDROID_LOG_INFO, "downloadPodcasts", "download done");
}
