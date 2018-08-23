package com.sample.projects.samplegeofencing;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;

import static com.sample.projects.samplegeofencing.MapsActivity.REQUEST_ACCESS_FINE_LOCATION_CODE;

/**
 * Created by Chyron-MACBOOK on 4/13/18.
 */

public class MapsEventHandler {

    private Context context;
    private MapsInterface.Action actionInterface;

    public MapsEventHandler(Context context, MapsInterface.Action actionInterface) {
        this.context = context;
        this.actionInterface = actionInterface;
    }

    public boolean checkLocationServices() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


    public BroadcastReceiver locationBR = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            Handler handler = new Handler();
            if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
//                            actionInterface.getLastKnownLocation();
                            Toast.makeText(context, "Location is on", Toast.LENGTH_LONG).show();
                        }
                    }, 5000);
                } else {
                    Toast.makeText(context, "Location is off", Toast.LENGTH_SHORT).show();
//                    actionInterface.clearMap();
                }
            }
        }
    };
}
