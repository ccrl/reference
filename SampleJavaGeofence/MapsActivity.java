package com.sample.projects.samplegeofencing;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.sample.projects.samplegeofencing.databinding.ActivityMapsBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        MapsInterface.Action {
//,
//    LocationListener
    private static final String TAG = "MAPS_ACTIVITY";

    private ActivityMapsBinding binding;
    private MapsEventHandler eventHandler;

    private GoogleMap mMap;
    private MyLocationModel myLocationModel;
    private GeofenceModel myGeofenceModel;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedProviderClient;
    private Location mLocation;
    private Marker myLocationMarker;
    private Marker myGeofenceMarker;
    private GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntentBR;
    private PendingIntent mGeofencePendingIntentIS;
    private Circle mGeofenceCircle;
    private List<Geofence> mGeofenceList;
    private NotificationManager notifManager;

    public static final int REQUEST_ACCESS_FINE_LOCATION_CODE = 01;
    private final int UPDATE_INTERVAL = 1000;
    private final int FASTEST_INTERVAL = 900;
    private static final long GEO_DURATION = 60 * 60 * 1000;
    private static final String GEOFENCE_REQ_ID = "TEST..";
    private final int GEOFENCE_REQ_CODE = 0;
    private static final String NOTIFICATION_MSG = "NOTIFICATION MSG";

    private int PLACE_PICKER_REQUEST = 1;

    // Create an Intent send by the notification
    public static Intent makeNotificationIntent(Context context, String msg) {
        Intent intent = new Intent(context, MapsActivity.class);
        intent.putExtra(NOTIFICATION_MSG, msg);
        return intent;
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(MapsActivity.this, getLayoutResourceId());

        eventHandler = new MapsEventHandler(this, this);

        myLocationModel = new MyLocationModel();
        myGeofenceModel = new GeofenceModel();
        mGeofenceList = new ArrayList<>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        buildGoogleApiClient();

        binding.mButtonTurnOnGeofence.setOnClickListener(onClickTurnOnGeofence());
        binding.mButtonTurnOffGeofence.setOnClickListener(onClickTurnOffGeofence());
        binding.mButtonTemp.setOnClickListener(onClickTemp());

        setupPlacePickerRequest();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
        stopGeofencing();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeIconInTaskbar();
    }

    //region SETUP
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (eventHandler.checkLocationServices()) {
            //todo: if location service is on, check permission if granted..
            checkPermission();
        } else {
            showLocationSettingDialog();
        }
    }

    public void setupPlacePickerRequest() {
        try {
            PlacePicker.IntentBuilder mBuilder = new PlacePicker.IntentBuilder();
            startActivityForResult(mBuilder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                String toastMsg = String.format("Place: %s", place.getName());
                Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
            }
        }

    }

    // endregion

    //region ACTION
    public void checkPermission() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_ACCESS_FINE_LOCATION_CODE);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void showLocationSettingDialog() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true); //this is the key ingredient to show dialog always when GPS is off

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        Log.i(TAG, "SUCCESS");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(MapsActivity.this, REQUEST_ACCESS_FINE_LOCATION_CODE);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ACCESS_FINE_LOCATION_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    getLastKnownLocation();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
        }
    }

    private void startLocationUpdates() {
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //todo: Change deprecated..
//        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        mFusedProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedProviderClient.requestLocationUpdates(mLocationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mLocation = locationResult.getLastLocation();
                myLocationModel.setMyLatitude(mLocation.getLatitude());
                myLocationModel.setMyLongitude(mLocation.getLongitude());

                Log.i("MapsActivity", "mLat: "+mLocation.getLatitude());
                Log.i("MapsActivity", "mLong: "+mLocation.getLongitude());

                setMyMarkerLocation(getMyLatitudeLocation(), getMyLongitudeLocation());
            }
        }, null);
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //todo: Change deprecated..
//        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        mFusedProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (mLocation != null) {
                    Log.d(TAG, "LAT: " + mLocation.getLatitude());
                    Log.d(TAG, "LONG: " + mLocation.getLongitude());
                    startLocationUpdates();
                } else {
                    startLocationUpdates();
                }
            }
        });
    }

    private void startGeofencing() {
        Log.i(TAG, "START GEOFENCING");
        GeofencingRequest geofenceRequest = createGeofenceRequest();
        addGeofence(geofenceRequest);
    }

    private void stopGeofencing() {
        Log.i(TAG, "STOP GEOFENCING");
        getLastKnownLocation();
        startLocationUpdates();
    }
    //endregion

    //region ONCLICK
    private View.OnClickListener onClickTurnOnGeofence() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                populateGeofencesList();
                startGeofencing();
            }
        };
    }

    private View.OnClickListener onClickTurnOffGeofence() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                removeIconInTaskbar();
                stopGeofencing();
            }
        };
    }

    private View.OnClickListener onClickTemp() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLocationUpdates();
            }
        };
    }
    //endregion

    //region GEOFENCE
    public void populateGeofencesList() {
        for (Map.Entry<String, LatLng> entry : MapConstants.GEOFENCE_LOCATIONS.entrySet()) {

            myGeofenceModel.setLatitude(entry.getValue().latitude);
            myGeofenceModel.setLongitude(entry.getValue().longitude);

            mGeofenceList.add(new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(entry.getKey())

                    // Set the circular region of this geofence.
                    .setCircularRegion(
                            entry.getValue().latitude,
                            entry.getValue().longitude,
                            MapConstants.GEOFENCE_RADIUS_IN_METERS
                    )

                    // Set the expiration duration of the geofence. This geofence gets automatically
                    // removed after this period of time.
                    .setExpirationDuration(MapConstants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                    // Set the transition types of interest. Alerts are only generated for these
                    // transition. We track entry and exit transitions in this sample.
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)

                    // Create the geofence.
                    .build());
            setMyGeofenceMarkerLocation(new LatLng(myGeofenceModel.getLatitude(), myGeofenceModel.getLongitude()));
            drawGeofence();
        }
    }

    private Geofence createGeofence(LatLng latLng, float radius) {
        Log.i(TAG, "CREATE GEOFENCE");
        return new Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                .setCircularRegion(latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration(GEO_DURATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    private GeofencingRequest createGeofenceRequest() {
        Log.i(TAG, "CREATE GEOFENCE REQUEST");
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(mGeofenceList)
                .build();
    }

    private PendingIntent createGeofencePendingIntentBR() {
        if (mGeofencePendingIntentBR != null) {
            return mGeofencePendingIntentBR;
        }
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        mGeofencePendingIntentBR = PendingIntent.getBroadcast(this,
                GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mGeofencePendingIntentBR.toString();
        Log.d(TAG, ""+mGeofencePendingIntentBR.toString());
        return mGeofencePendingIntentBR;
    }

    private void addGeofence(GeofencingRequest request) {
        Log.i(TAG, "ADD GEOFENCE");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        mGeofencingClient = LocationServices.getGeofencingClient(this);
        mGeofencingClient.addGeofences(
                request,
                createGeofencePendingIntentBR()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    showToast("BR Geofences Added");
                    showIconInTaskBar();
                }
            }
        });
    }

    private void drawGeofence() {
        CircleOptions circleOptions = new CircleOptions()
                .center(myGeofenceMarker.getPosition())
                .strokeColor(Color.argb(50, 70, 70, 70))
                .fillColor(Color.argb(100, 150, 150, 150))
                .radius(MapConstants.GEOFENCE_RADIUS_IN_METERS);
        mGeofenceCircle = mMap.addCircle(circleOptions);
    }

    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void showIconInTaskBar() {
        notifManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, MapsActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
        // build notification
        // the addAction re-use the same intent to keep the example short
        Notification notification  = new Notification.Builder(this)
                .setContentTitle("Sample Geofence")
                .setContentText("Geofence is still running.")
                .setSmallIcon(R.drawable.ic_directions_run_black_24dp)
                .setContentIntent(pIntent)
                .setAutoCancel(true).build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notifManager.notify(0, notification);
    }

    private void removeIconInTaskbar() {
        notifManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        notifManager.cancelAll();
    }
    //endregion

    //region MARKERS
    private void setMyMarkerLocation(double latitude, double longitude) {
        String title = latitude + ", " + longitude;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .title(title)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_directions_run_black_24dp));
        if (mMap != null) {
            if (myLocationMarker != null) {
                myLocationMarker.remove();
            }
            myLocationMarker = mMap.addMarker(markerOptions);
            float zoom = 16;
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), zoom);
            mMap.animateCamera(cameraUpdate);
        }
    }

    private void setMyGeofenceMarkerLocation(LatLng latLng) {
        String title = latLng.latitude + ", " + latLng.longitude;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        if (mMap != null) {
            myGeofenceMarker = mMap.addMarker(markerOptions);
        }
    }
    //endregion

    //region OVERRIDES
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected");
        getLastKnownLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "onConnectionSuspended");
        mGoogleApiClient.reconnect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed");

    }
    //endregion

    //region GETTERS AND SETTERS
    public int getLayoutResourceId() {
        return R.layout.activity_maps;
    }

    public double getMyLatitudeLocation() {
        return myLocationModel.getMyLatitude();
    }

    public double getMyLongitudeLocation() {
        return myLocationModel.getMyLongitude();
    }
    //endregion
}
