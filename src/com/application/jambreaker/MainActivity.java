package com.application.jambreaker;
 
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
 
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
 
public class MainActivity extends FragmentActivity {
    GoogleMap map;
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    String SENDER_ID = "383892465605";

    /**
     * Tag used on log messages.
     */
    static final String TAG = "GCMDemo";

    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    SharedPreferences prefs;
    Context context;

    String regid;
    private static final LatLng SIMS_DORADO = 
            new LatLng(1.315526,103.890842);
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();

        // Check device for Play Services APK. If check succeeds, proceed with
        //  GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);

            if (regid.isEmpty()) {
                registerInBackground();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
        map = ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map)).getMap();
        if (map == null) {
            Toast.makeText(this, "Google Maps not available", 
                Toast.LENGTH_LONG).show();
        }
    }
 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is
        // present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
 
        switch (item.getItemId()) {
 
        case R.id.menu_sethybrid:
        	if(map.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
        		map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        	}
        	else {
        		map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        	}
            break;
 
        case R.id.menu_showtraffic:
            map.setTrafficEnabled(true);
            break;
        
        case R.id.menu_zoomin:
            map.animateCamera(CameraUpdateFactory.zoomIn());
            break;
 
        case R.id.menu_zoomout:
            map.animateCamera(CameraUpdateFactory.zoomOut());
            break;
            
        case R.id.menu_gotolocation:
            CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(SIMS_DORADO) // Sets the center of the map to
                                            // Golden Gate Bridge
                .zoom(17)                   // Sets the zoom
                .bearing(90) // Sets the orientation of the camera to east
                .tilt(30)    // Sets the tilt of the camera to 30 degrees
                .build();    // Creates a CameraPosition from the builder
            map.animateCamera(CameraUpdateFactory.newCameraPosition(
                cameraPosition));
            break;
            
        case R.id.menu_addmarker:
        	 
            // ---using the default marker---
            /*
            map.addMarker(new MarkerOptions() 
                .position(GOLDEN_GATE_BRIDGE)
                .title("Golden Gate Bridge") .snippet("San Francisco")
                .icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            */
 
            map.addMarker(new MarkerOptions()
                .position(SIMS_DORADO)
                .title("My condo")
                .snippet("Singapore")
                .icon(BitmapDescriptorFactory
                .fromResource(R.drawable.ic_launcher)));
            break;
            
        case R.id.menu_getcurrentlocation:
            // ---get your current location and display a blue dot---
            map.setMyLocationEnabled(true);
 
            break;
 
        case R.id.menu_showcurrentlocation:
        	 Log.i(TAG,"Show location");
        	map.setMyLocationEnabled(true);
            Location myLocation = map.getMyLocation();
            LatLng myLatLng = new LatLng(myLocation.getLatitude(),
                    myLocation.getLongitude());
            
            Geocoder geocoder =
                    new Geocoder(context, Locale.getDefault());
            
            List<Address> addresses = null;
			try {
				addresses = geocoder.getFromLocation(myLocation.getLatitude(),
						myLocation.getLongitude(), 1);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            Address address = addresses.get(0);
           
            CameraPosition myPosition = new CameraPosition.Builder()
                    .target(myLatLng).zoom(17).bearing(90).tilt(30).build();
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(myPosition));
            Log.i(TAG, "Starting dialog now");
            AlertDialog dlgAlert =  new AlertDialog.Builder(this)
            .setMessage("Are you stuck in a JAM at " + String.format(
                    "%s, %s",
                    // If there's a street address, add it
                    address.getMaxAddressLineIndex() > 0 ?
                            address.getAddressLine(0) : "",
                   
                    // The country of the address
                    address.getCountryName()) + "?")
            .setNegativeButton("NO", null)
            .setPositiveButton("YES", new DialogInterface.OnClickListener() 
            {                   
				@Override
                public void onClick(DialogInterface arg0, int arg1) 
                {
                    try
                    {
                    	new AsyncTask<Object, Object, Object>() {
							@Override
							protected Object doInBackground(Object... params) {
								 String msg = "";
	                                try {
	                                    Bundle data = new Bundle();
	                                        data.putString("my_message", "Hello World");
	                                        data.putString("my_action",
	                                                "com.google.android.gcm.demo.app.ECHO_NOW");
	                                        String id = Integer.toString(msgId.incrementAndGet());
	                                        gcm.send(SENDER_ID + "@gcm.googleapis.com", id, data);
	                                        msg = "Sent message";
	                                } catch (IOException ex) {
	                                    msg = "Error :" + ex.getMessage();
	                                    Log.i(TAG, msg);
	                                }
	                                return msg;
							}
                        }.execute(null, null, null);

                    }//end try
                    catch(Exception e)
                    {
                        Toast.makeText(getBaseContext(),  "", Toast.LENGTH_LONG).show();
                        Log.i(TAG, "problem making dialog");
                    }//end catch
                }//end onClick()
            }).create();
            
            dlgAlert.show();
            break;
            
        }
 
        return true;
    }
    
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
    
    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        Log.i(TAG, "New reg id is "+ registrationId);
        return registrationId;
    }
    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }
    
    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Object, Object, Object>() {
           
			@Override
			protected Object doInBackground(Object... arg0) {
				String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    // The request to your server should be authenticated if your app
                    // is using accounts.
                    sendRegistrationIdToBackend(context, regid);

                    // For this demo: we don't need to send it because the device
                    // will send upstream messages to a server that echo back the
                    // message using the 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
			}
			
			 private void storeRegistrationId(Context context, String regid) {
				// TODO Auto-generated method stub
				
			}

			/**
		     * Stores the registration ID and app versionCode in the application's
		     * {@code SharedPreferences}.
		     *
		     * @param context application's context.
		     * @param regId registration ID
		     */
			private void sendRegistrationIdToBackend(Context context, String regId) {
				final SharedPreferences prefs = getGCMPreferences(context);
		        int appVersion = getAppVersion(context);
		        Log.i(TAG, "Saving regId on app version " + appVersion);
		        SharedPreferences.Editor editor = prefs.edit();
		        editor.putString(PROPERTY_REG_ID, regId);
		        editor.putInt(PROPERTY_APP_VERSION, appVersion);
		        editor.commit();				
			}
        }.execute(null, null, null);
        
    }
   
}