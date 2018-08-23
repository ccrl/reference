package com.sample.projects.samplegeofencing;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

/**
 * Created by Chyron-MACBOOK on 4/16/18.
 */

public class MapConstants {

    public static final String WALTERMART_ID = "WALTERMART";
    public static final String CITYLAND_ID = "CITYLAND";
    public static final String MAKATI_MED_ID = "MAKATI_MED";
    public static final String ORIENTAL_ID = "ORIENTAL";
    public static final String POST_OFFICE_ID = "POST-OFFICE";
    public static final String TELEPERFORMANCE_ID = "TELEPERFORMANCE";
    public static final String SHANG_ID = "SHANG_SALCEDO";
    public static final String WWCF_ID = "WWCF";
    public static final String PRIMAL_ID = "PRIMAL_APE";
    public static final String RCBC_ID = "RCBC";
    public static final String ZOO_ID = "ZOO";

    private MapConstants() {}

    private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;
    static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;
    public static final float GEOFENCE_RADIUS_IN_METERS = 150.0f;

    public static final HashMap<String, LatLng> GEOFENCE_LOCATIONS = new HashMap<>();

    static {
        GEOFENCE_LOCATIONS.put(WALTERMART_ID, new LatLng(  14.550899, 121.013246));
        GEOFENCE_LOCATIONS.put(CITYLAND_ID, new LatLng(14.549606, 121.012683));
        GEOFENCE_LOCATIONS.put(MAKATI_MED_ID, new LatLng(14.559336, 121.014498));
        GEOFENCE_LOCATIONS.put(ORIENTAL_ID, new LatLng(14.558798, 121.011830));
        GEOFENCE_LOCATIONS.put(POST_OFFICE_ID, new LatLng(14.561531, 121.014607));
        GEOFENCE_LOCATIONS.put(TELEPERFORMANCE_ID, new LatLng(14.560568, 121.015228));
        GEOFENCE_LOCATIONS.put(SHANG_ID, new LatLng(14.561454, 121.020394));
        GEOFENCE_LOCATIONS.put(WWCF_ID, new LatLng(14.560508, 121.018151));
        GEOFENCE_LOCATIONS.put(PRIMAL_ID, new LatLng(14.557645, 121.013351));
        GEOFENCE_LOCATIONS.put(RCBC_ID, new LatLng(14.560973, 121.016389));
        GEOFENCE_LOCATIONS.put(ZOO_ID, new LatLng(14.562303, 121.016162));
    }
}
