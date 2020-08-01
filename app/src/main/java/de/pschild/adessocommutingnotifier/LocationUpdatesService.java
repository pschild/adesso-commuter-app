package de.pschild.adessocommutingnotifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.json.JSONObject;

public class LocationUpdatesService extends Service {

  private static final String TAG = LocationUpdatesService.class.getSimpleName();
  private static final String CHANNEL_ID = "ForegroundServiceChannel";
  private static final int NOTIFICATION_ID = 12345678;

  private static final int SERVICE_LIFETIME = 1000 * 60 * 60;
  private static final int LOCATION_INTERVAL = 1000 * 60 * 2;
  private static final int LOCATION_FASTEST_INTERVAL = 1000 * 60 * 2;
  private long mServiceStartTime = 0;

  private FusedLocationProviderClient mFusedLocationClient;
  private LocationCallback mLocationCallback;

  @Override
  public void onCreate() {
    Log.d(TAG, "LocationUpdatesService.onCreate");
    Logger.log(getApplicationContext(), "LocationUpdatesService.onCreate");
    super.onCreate();

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    getLastLocation();
    createLocationRequest();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d(TAG, "LocationUpdatesService.onStartCommand");
    Logger.log(getApplicationContext(), "LocationUpdatesService.onStartCommand");

    mServiceStartTime = Calendar.getInstance().getTimeInMillis();
    createNotificationChannel();
    startForeground(NOTIFICATION_ID, this.buildNotification("waiting for 1st location..."));

    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel serviceChannel = new NotificationChannel(
          CHANNEL_ID,
          "Foreground Service Channel",
          NotificationManager.IMPORTANCE_DEFAULT
      );
      NotificationManager manager = getSystemService(NotificationManager.class);
      manager.createNotificationChannel(serviceChannel);
    }
  }

  private Notification buildNotification(String text) {
    Intent notificationIntent = new Intent(this, MainActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

    // TODO: set title, icon, etc.
    return new NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Pendel-Tracker aktiv")
        .setContentText(text)
//        .setOngoing(true)
        .setSmallIcon(R.drawable.baseline_person_pin_circle_white_48)
        .setContentIntent(pendingIntent)
        .build();
  }

  private void updateNotification(String text) {
    Notification notification = buildNotification(text);
    NotificationManager mNotificationManager = (NotificationManager) getSystemService(
        Context.NOTIFICATION_SERVICE);
    mNotificationManager.notify(NOTIFICATION_ID, notification);
  }

  private void createLocationRequest() {
    LocationRequest locationRequest = LocationRequest.create();
    locationRequest.setInterval(LOCATION_INTERVAL);
    locationRequest.setFastestInterval(LOCATION_FASTEST_INTERVAL);
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    mLocationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        if (locationResult == null) {
          return;
        }
        for (Location location : locationResult.getLocations()) {
          if (location != null) {
            if (shouldStop(location)) {
              stopService();
              return;
            }
            long runningFor = (Calendar.getInstance().getTimeInMillis() - mServiceStartTime) / 1000;
            updateNotification(runningFor + "s, [" + location.getLatitude() + ", " + location.getLongitude() + "]");
            callEndpoint(location.getLatitude(), location.getLongitude());
            Logger.log(getApplicationContext(), "Got new location: [" + location.getLatitude() + ", " + location.getLongitude() + "]");
          }
        }
      }
    };
    mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null);
  }

  private Task<Void> removeLocationRequest() {
    Log.d(TAG, "LocationUpdatesService.removeLocationRequest");
    try {
      return mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    } catch (SecurityException unlikely) {
      Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
    }
    return null;
  }

  private void getLastLocation() {
    mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
      @Override
      public void onSuccess(Location location) {
        if (location != null) {
          Logger.log(getApplicationContext(), "Got initial location: [" + location.getLatitude() + ", " + location.getLongitude() + "]");
          updateNotification("[" + location.getLatitude() + ", " + location.getLongitude() + "]");
          callEndpoint(location.getLatitude(), location.getLongitude());
        }
      }
    });
  }

  private void callEndpoint(double lat, double lng) {
    Date c = Calendar.getInstance().getTime();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    String formattedDate = simpleDateFormat.format(c);
    Log.d(TAG, "callEndpoint " + lat + ", " + lng + " @ " + formattedDate);
    Logger.log(getApplicationContext(), "Called Endpoint: [" + lat + ", " + lng + "]");

    RequestQueue queue = Volley.newRequestQueue(this);
    String url = BuildConfig.endpoint + "/logfromandroid/" + lat + "/" + lng;
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Method.POST, url, null,
        new Response.Listener<JSONObject>() {
          @Override
          public void onResponse(JSONObject response) {
            Log.i(TAG, response.toString());
            Logger.log(getApplicationContext(), "Success!" + response.toString());
          }
        },
        new Response.ErrorListener() {
          @Override
          public void onErrorResponse(VolleyError error) {
            Log.i(TAG, error.toString());
            Logger.log(getApplicationContext(), "Error! " + error.toString());
          }
        });
    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
        30000,
        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

    queue.add(jsonObjectRequest);
  }

  private boolean shouldStop(Location location) {
    long runningFor = (Calendar.getInstance().getTimeInMillis() - mServiceStartTime);

    // test
    float[] distance = new float[2];
    Location.distanceBetween(location.getLatitude(), location.getLongitude(), 51.668189, 6.148282, distance);
    Log.i(TAG, "Distance to home: " + distance[0]);

    return runningFor >= SERVICE_LIFETIME
//        || distance <= 100
//        || targetReached()
        ;
  }

  private void stopService() {
    Logger.log(getApplicationContext(), "Service is about to stop...");
    // Important: use addOnSuccessListener() to avoid an additional location update after the service has been stopped!
    // This caused the notification to re-appear.
    removeLocationRequest().addOnSuccessListener(new OnSuccessListener<Void>() {
      @Override
      public void onSuccess(Void aVoid) {
        Logger.log(getApplicationContext(), "Location request removed successfully");
        stopForeground(true);
        stopSelf();
        Logger.log(getApplicationContext(), "Service stopped!");
      }
    });
  }
}
