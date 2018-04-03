package org.mydigitalschool.tracker;


import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;
import android.provider.Settings.Secure;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements LocationListener {
    final String TAG = "GPS";
    private final static int ALL_PERMISSIONS_RESULT = 101;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60;

    TextView tv_mainActivity_latitude, tv_mainActivity_longitude, tv_mainActivity_time, tv_mainActivity_phoneId, tv_mainActivity_response;
    String latitude, longitude, phoneId;
    LocationManager locationManager;
    Location loc;
    TelephonyManager tm;
    RequestQueue rq;

    ArrayList<String> permissions = new ArrayList<>();
    ArrayList<String> permissionsToRequest;
    ArrayList<String> permissionsRejected = new ArrayList<>();
    boolean isGPS = false;
    boolean isNetwork = false;
    boolean canGetLocation = true;


    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_mainActivity_latitude = findViewById(R.id.tv_mainActivity_latitude);
        tv_mainActivity_longitude = findViewById(R.id.tv_mainActivity_longitude);
        tv_mainActivity_time = findViewById(R.id.tv_mainActivity_time);
        tv_mainActivity_phoneId = findViewById(R.id.tv_mainActivity_phoneId);
        tv_mainActivity_response = findViewById(R.id.tv_mainActivity_response);

        locationManager = (LocationManager) getSystemService(Service.LOCATION_SERVICE);
        assert locationManager != null;
        isGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.INTERNET);
        permissionsToRequest = findUnAskedPermissions(permissions);



        String IMEINumber = Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
        phoneId = IMEINumber;
        tv_mainActivity_phoneId.setText(IMEINumber);

        if (!isGPS && !isNetwork) {
            Log.d(TAG, "GPS connection off");
            showSettingsAlert();
            getLastLocation();
        } else {
            Log.d(TAG, "GPS connection on");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (permissionsToRequest.size() > 0) {
                    requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
                            ALL_PERMISSIONS_RESULT);
                    canGetLocation = false;
                }
            }

            getLocation();
        }



    }
    @Override
    public void onLocationChanged(Location location) {
        updateUI(location);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {}

    @Override
    public void onProviderEnabled(String s) {
        getLocation();
    }

    @Override
    public void onProviderDisabled(String s) {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }


    private String getEnabledLocationProvider() {
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        // Returns the name of the provider that best meets the given criteria.
        // ==> "gps", "network",...
        String bestProvider = null;
        if (locationManager != null) {
            List<String> providers = locationManager.getProviders(true);
            Location bestLocation = null;
            try {
                for (String provider : providers) {
                    Location l = locationManager.getLastKnownLocation(provider);
                    if (l == null) {
                        continue;
                    }
                    if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                        bestLocation = l;
                        bestProvider = provider;
                    }
                }
                if (bestProvider == null)
                    return null;
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        boolean enabled = false;
        if (locationManager != null) {
            enabled = locationManager.isProviderEnabled(bestProvider);
        }

        if (!enabled) {
            return null;
        }

        return bestProvider;
    }

    private void getLocation() {
        try {
            if (canGetLocation) {
                if (isGPS) {
                    Log.d(TAG, "GPS on");
                    String locationProvider = this.getEnabledLocationProvider();
                    try {
                        // This code need permissions, asked before
                        locationManager.requestLocationUpdates(
                                locationProvider,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                        loc = locationManager
                                .getLastKnownLocation(locationProvider);
                        if (loc != null)
                            updateUI(loc);

                    }
                    // With Android API >= 23, need to catch SecurityException.
                    catch (SecurityException e) {
                        e.printStackTrace();
                        return;
                    }

                /*
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);*/

               /* if (locationManager != null) {
                    Log.d(TAG, "locationManager != null");
                    loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (loc != null) {
                        Log.d(TAG, "loc != null");
                        updateUI(loc);
                    }
                }*/
                } else if (isNetwork) {
                    Log.d(TAG, "Network on");
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                    if (locationManager != null) {
                        loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (loc != null)
                            updateUI(loc);
                    }
                } else {
                    loc.setLatitude(0);
                    loc.setLongitude(0);
                    updateUI(loc);
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
//    private void getLocation() {
//        try {
//            if (canGetLocation) {
//                if (isGPS) {
//                    Log.d(TAG, "GPS on");
//                    locationManager.requestLocationUpdates(
//                            LocationManager.GPS_PROVIDER,
//                            MIN_TIME_BW_UPDATES,
//                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
//
//                    if (locationManager != null) {
//                        loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//                        if (loc != null)
//                            updateUI(loc);
//                    }
//                } else if (isNetwork) {
//                    Log.d(TAG, "Network on");
//                    locationManager.requestLocationUpdates(
//                            LocationManager.NETWORK_PROVIDER,
//                            MIN_TIME_BW_UPDATES,
//                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
//
//                    if (locationManager != null) {
//                        loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//                        if (loc != null)
//                            updateUI(loc);
//                    }
//                } else {
//                    loc.setLatitude(0);
//                    loc.setLongitude(0);
//                    updateUI(loc);
//                }
//            }
//        } catch (SecurityException e) {
//            e.printStackTrace();
//        }
//    }

    private void getLastLocation() {
        try {
            Criteria criteria = new Criteria();
            String provider = locationManager.getBestProvider(criteria, false);
            Location location = locationManager.getLastKnownLocation(provider);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<String> findUnAskedPermissions(ArrayList<String> wanted) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (canAskPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private boolean canAskPermission() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (String perms : permissionsToRequest) {
                    if (!hasPermission(perms)) {
                        permissionsRejected.add(perms);
                    }
                }

                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            showMessageOKCancel(
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.toArray(
                                                        new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    });
                            return;
                        }
                    }
                } else {
                    canGetLocation = true;
                    getLocation();
                }
                break;
        }
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(R.string.gps_enabled);
        alertDialog.setMessage(R.string.gps_enabled_question);
        alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        alertDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alertDialog.show();
    }

    private void showMessageOKCancel(DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage("These permissions are mandatory for the application. Please allow access.")
                .setPositiveButton(R.string.yes, okListener)
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show();
    }

    @SuppressLint("SetTextI18n")
    private void updateUI(Location loc) {
        Log.d(TAG, "update");
        tv_mainActivity_latitude.setText(Double.toString(loc.getLatitude()));
        tv_mainActivity_longitude.setText(Double.toString(loc.getLongitude()));
        tv_mainActivity_time.setText(DateFormat.getTimeInstance().format(loc.getTime()));
        longitude = Double.toString(loc.getLongitude());
        latitude = Double.toString(loc.getLatitude());

        rq = Volley.newRequestQueue(this);
        String url ="http://5.51.221.85:1337/track/";

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("response", response);

                        tv_mainActivity_response.setText(response);
                        Log.d("longitude", longitude);
                        Log.d("latitude", latitude);
                        Log.d("response", response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("longitude", longitude);
                        Log.d("latitude", latitude);
                        Log.d("phoneId", phoneId);
                        //Log.d("Error.Response",response );
                    }
                }
        ) {
            @Override
            protected Map < String, String > getParams()
            {
                Map< String, String >  params;
                params = new HashMap<>();

                params.put("identification", phoneId);
                params.put("longitude", longitude);
                params.put("latitude", latitude);

                return params;
            }
        };
        rq.add(postRequest);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }
}