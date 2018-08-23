package com.sample.projects.samplegeofencing;

import com.google.android.gms.location.Geofence;

/**
 * Created by Chyron-MACBOOK on 4/17/18.
 */

public class GeofenceModel {

    private double latitude;
    private double longitude;

    public GeofenceModel() {}

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
