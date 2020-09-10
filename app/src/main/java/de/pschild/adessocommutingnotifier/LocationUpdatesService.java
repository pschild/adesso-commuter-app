package de.pschild.adessocommutingnotifier;

import static java.lang.Math.floor;

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
import android.os.PowerManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import java.util.Calendar;

public class LocationUpdatesService extends Service {

  private static final String CHANNEL_ID = "ForegroundServiceChannel";
  private static final int NOTIFICATION_ID = 12345678;

  private static final int SERVICE_LIFETIME = 1000 * 60 * 90;
  private static final int COMMUTING_MAX_DURATION = 1000 * 60 * 90;
  private static final int LOCATION_INTERVAL = 1000 * 60 * 5;
  private static final int LOCATION_FASTEST_INTERVAL = 1000 * 60 * 5;

  private long mServiceStartTime = 0;

  private boolean mIsCommuting = false;
  private long mCommutingStartTime = 0;

  private static final double[] HOME_COORDS = { 51.668189, 6.148282 };
  private static final double[] WORK_COORDS = { 51.4557381, 7.0101814 };
  private static final int MIN_MOVEMENT_FOR_REQUEST = 500; // meter
  private static final int MIN_DISTANCE_TO_TARGET = 500; // meter
  private double[] mDestination = null;

  private Location mLastLocation = null;

  private PowerManager mPowerManager;
  private FusedLocationProviderClient mFusedLocationClient;
  private LocationCallback mLocationCallback;

  @Override
  public void onCreate() {
    Logger.log(getApplicationContext(), "LocationUpdatesService.onCreate");
    super.onCreate();

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

    // TODO: set destination based on current location when mIsCommuting turns true
    if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 12) {
      mDestination = WORK_COORDS;
      Logger.log(getApplicationContext(), "Destination is: WORK");
    } else {
      mDestination = HOME_COORDS;
      Logger.log(getApplicationContext(), "Destination is: HOME");
    }

    createLocationRequest();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Logger.log(getApplicationContext(), "LocationUpdatesService.onStartCommand");

    mServiceStartTime = Calendar.getInstance().getTimeInMillis();
    createNotificationChannel();
    startForeground(NOTIFICATION_ID, this.buildNotification("waiting for 1st location..."));

    Logger.log(getApplicationContext(), "PowerManager info ["
        + "isPowerSaveMode=" + mPowerManager.isPowerSaveMode() + ","
        + "isDeviceIdleMode=" + mPowerManager.isDeviceIdleMode() + ","
        + "]");

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
    // TODO: locationRequest.setInterval(1min); if !mIsCommuting
    locationRequest.setFastestInterval(LOCATION_FASTEST_INTERVAL);
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//    locationRequest.setSmallestDisplacement(1000L * 1); // 1.000m

    mLocationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        if (locationResult == null) {
          return;
        }
        for (Location currentLocation : locationResult.getLocations()) {
          if (currentLocation != null) {
            if (shouldStop(currentLocation)) {
              stopService();
              return;
            }

            long runningFor = (Calendar.getInstance().getTimeInMillis() - mServiceStartTime) / 1000;
            long commutingFor = (Calendar.getInstance().getTimeInMillis() - mCommutingStartTime) / 1000;
            updateNotification(""
                + "Svc: " + floor(runningFor / 60f) + "m, "
                + "Com: " + (mIsCommuting ? floor(commutingFor / 60f) + "m" : "-") + " "
                + "[" + currentLocation.getLatitude() + ", " + currentLocation.getLongitude() + "]");

            if (mLastLocation == null) {
              Logger.log(getApplicationContext(), "No last location... exiting!");
              mLastLocation = currentLocation;
              return;
            }

            // buffer: when last movement is less than specified, don't make request
            if (getDistance(currentLocation, mLastLocation) >= MIN_MOVEMENT_FOR_REQUEST) {
              if (!mIsCommuting) {
                mIsCommuting = true;
                mCommutingStartTime = Calendar.getInstance().getTimeInMillis();
                // TODO: locationRequest.setInterval(5min); if mIsCommuting
                Logger.log(getApplicationContext(), "Start commuting!");
              }
              Logger.log(getApplicationContext(), "Making request, as distance is " + getDistance(currentLocation, mLastLocation) + "m");
              callEndpoint(currentLocation.getLatitude(), currentLocation.getLongitude());
            } else {
              Logger.log(getApplicationContext(), "NOT making request, as distance is only " + getDistance(currentLocation, mLastLocation) + "m");
            }

            mLastLocation = currentLocation;
          }
        }
      }
    };
    mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null);
  }

  private Task<Void> removeLocationRequest() {
    try {
      return mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    } catch (SecurityException unlikely) {
      Logger.log(getApplicationContext(), "Lost location permission. Could not remove updates. " + unlikely);
    }
    return null;
  }

  private void callEndpoint(double lat, double lng) {
    Logger.log(getApplicationContext(), "Calling Endpoint with [" + lat + ", " + lng + "]...");

    RequestQueue queue = Volley.newRequestQueue(this);
    String url = BuildConfig.endpoint + "/from/" + lat + "," + lng + "/to/" + mDestination[0] + "," + mDestination[1];
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Method.GET, url, null,
        response -> Logger.log(getApplicationContext(), "Success! " + response.toString()),
        error -> Logger.log(getApplicationContext(), "Error! " + error.toString()));
    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
        30000,
        2,
        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

    queue.add(jsonObjectRequest);
  }

  private boolean shouldStop(Location currentLocation) {
    long serviceRunningDuration = (Calendar.getInstance().getTimeInMillis() - mServiceStartTime);
    long commutingDuration = (Calendar.getInstance().getTimeInMillis() - mCommutingStartTime);
    final boolean targetReached = targetReached(currentLocation);

    Logger.log(getApplicationContext(), ""
        + "isCommuting=" + mIsCommuting
        + ", serviceTimeLimitReached=" + (!mIsCommuting && serviceRunningDuration >= SERVICE_LIFETIME)
        + ", commutingTimeLimitReached=" + (mIsCommuting && commutingDuration >= COMMUTING_MAX_DURATION)
        + ", targetReached=" + targetReached);
    return targetReached
        || (!mIsCommuting && serviceRunningDuration >= SERVICE_LIFETIME)
        || (mIsCommuting && commutingDuration >= COMMUTING_MAX_DURATION);
  }

  private void stopService() {
    Logger.log(getApplicationContext(), "Service is about to stop...");
    Logger.log(getApplicationContext(), "PowerManager info ["
        + "isPowerSaveMode=" + mPowerManager.isPowerSaveMode() + ","
        + "isDeviceIdleMode=" + mPowerManager.isDeviceIdleMode() + ","
        + "]");

    mIsCommuting = false;

    // Important: use addOnSuccessListener() to avoid an additional location update after the service has been stopped!
    // This caused the notification to re-appear.
    removeLocationRequest().addOnSuccessListener(aVoid -> {
      Logger.log(getApplicationContext(), "Location request removed successfully");
      stopForeground(true);
      stopSelf();
      Logger.log(getApplicationContext(), "Service stopped!");
    });
  }

  private boolean targetReached(Location currentLocation) {
    Location targetLocation = new Location("");
    targetLocation.setLatitude(mDestination[0]);
    targetLocation.setLongitude(mDestination[1]);
    float distanceToTarget = getDistance(currentLocation, targetLocation);
    Logger.log(getApplicationContext(), "Distance to destination: " + distanceToTarget);
    return distanceToTarget <= MIN_DISTANCE_TO_TARGET;
  }

  private float getDistance(Location start, Location end) {
    float[] distance = new float[2];
    Location.distanceBetween(start.getLatitude(), start.getLongitude(), end.getLatitude(), end.getLongitude(), distance);
    return distance[0];
  }
}
