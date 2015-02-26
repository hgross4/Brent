package com.brent.twittersearch;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Brent on 11/5/14.
 */
public class LocationFinder {

    public static LocationManager locationManager =
            (LocationManager) TwitterSearchApplication.getAppContext()
            .getSystemService(Context.LOCATION_SERVICE);
    private static int MILLISECS_ELAPSED = 3600000; // 1 hour
    private static int METERS_TRAVERSED = 1000;

    private LocationFinder() {}

    private static final String TAG = LocationFinder.class.getSimpleName();
    private static LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            Log.wtf(TAG, location.toString());
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

    };

    public static String[] getLocation() {
        String provider;
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        Location location = locationManager.getLastKnownLocation(provider);
        if (location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if (location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        if (location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        }

        double longitude = 0;
        double latitude = 0;

        //Fix for no location null bug, might be a Genymotion only issue
        if (location == null) {
            Log.e("MAJOR LOCATION ERROR", "This shouldn't have happened but we have no location at this time.");
        }else{
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }

        return new String[]{String.valueOf(longitude), String.valueOf(latitude)};
    }

    public static Map<String,List<String>> getLocationParamMap(Context context){
        String[] location = getLocation();
        Map<String,List<String>> params = new HashMap<String,List<String>>();

        // If we don't actually have a location, just return a blank map.
        if(location[0].equalsIgnoreCase("0") || location[1].equalsIgnoreCase("0")){
            return params;
        }

        ArrayList<String> latitude = new ArrayList<String>();
        latitude.add(location[1]);
        ArrayList<String> longitude = new ArrayList<String>();
        longitude.add(location[0]);
        params.put("latitude",latitude);
        params.put("longitude",longitude);
        return params;
    }

    public static void requestLocationUpdates() {

        if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    MILLISECS_ELAPSED, METERS_TRAVERSED, locationListener);
        }
        if (locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)){
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    MILLISECS_ELAPSED, METERS_TRAVERSED, locationListener);
        }

    }

    public static void removeUpdates() {
        locationManager.removeUpdates(locationListener);
    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode = Settings.Secure.LOCATION_MODE_OFF;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }

            return (locationMode != Settings.Secure.LOCATION_MODE_OFF &&
                    locationMode != Settings.Secure.LOCATION_MODE_SENSORS_ONLY);

        }
        else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

}
