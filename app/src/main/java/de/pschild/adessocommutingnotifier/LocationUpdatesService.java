package de.pschild.adessocommutingnotifier;

import static java.lang.Math.floor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import de.pschild.adessocommutingnotifier.api.HttpClient;
import de.pschild.adessocommutingnotifier.api.model.AuthResult;
import de.pschild.adessocommutingnotifier.api.model.CommutingResult;
import de.pschild.adessocommutingnotifier.api.model.CommutingStatusResult;
import de.pschild.adessocommutingnotifier.api.model.Credentials;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.Calendar;
import java.util.Objects;

public class LocationUpdatesService extends Service {

  private boolean mDebugMode = false;

  private static final String CHANNEL_ID = "ForegroundServiceChannel";
  private static final int NOTIFICATION_ID = 12345678;

  private static final int SERVICE_LIFETIME = 1000 * 60 * 90;
  private static final int COMMUTING_MAX_DURATION = 1000 * 60 * 90;
  private static final int LOCATION_INTERVAL = 1000 * 60 * 1;
  private static final int REQUEST_INTERVAL = 1000 * 60 * 5;

  private long mServiceStartTime = 0;

  private boolean mIsCommuting = false;
  private long mCommutingStartTime = 0;

  private long mLastRequestTime = 0;

  private static final double[] HOME_COORDS = { 51.668189, 6.148282 }; // read from server? Cache?
  private static final double[] WORK_COORDS = { 51.4557381, 7.0101814 }; // read from server? Cache?
  private static final int MIN_MOVEMENT_FOR_REQUEST = 200; // meter
  private static final int MIN_DISTANCE_TO_TARGET = 200; // meter
  private static final float MIN_SPEED_FOR_REQUEST = 5f;
  private double[] mDestination = null;

  private Location mLastLocation = null;

  private FusedLocationProviderClient mFusedLocationClient;
  private LocationCallback mLocationCallback;

  @Override
  public void onCreate() {
    Logger.log(getApplicationContext(), "LocationUpdatesService.onCreate");
    super.onCreate();

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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

    if (Objects.equals(intent.getAction(), "debug")) {
      mDebugMode = true;
    }

    if (Objects.equals(intent.getAction(), "stop")) {
      this.stopService();
      return STOP_FOREGROUND_REMOVE;
    }

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
    locationRequest.setFastestInterval(LOCATION_INTERVAL);
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    mLocationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        if (locationResult == null) {
          return;
        }
        for (Location currentLocation : locationResult.getLocations()) {
          if (currentLocation != null) {
            Logger.log(getApplicationContext(), "accuracy=" + currentLocation.getAccuracy() + ", speed=" + currentLocation.getSpeed());

            if (shouldStop(currentLocation)) {
              // call one last time (if commuting)
              // TODO: send CANCELLED (not END) in case the timelimits are reached
              if (mIsCommuting) {
//                saveCoordinates(currentLocation.getLatitude(), currentLocation.getLongitude());
                saveCommutingStatus(CommutingState.END);
              } else {
                saveCommutingStatus(CommutingState.CANCELLED);
              }

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

            // buffer: make request only if (last movement is more than specified and speed is greater than 0) or commuting is active
            if (
                (getDistance(currentLocation, mLastLocation) >= MIN_MOVEMENT_FOR_REQUEST && currentLocation.getSpeed() >= MIN_SPEED_FOR_REQUEST)
                || mIsCommuting
                || mDebugMode
              ) {
              if (!mIsCommuting) {
                mIsCommuting = true;
                mCommutingStartTime = Calendar.getInstance().getTimeInMillis();
                Logger.log(getApplicationContext(), "***** Start commuting! *****");
                saveCommutingStatus(CommutingState.START);
              }

              if (shouldMakeRequest()) {
                Logger.log(getApplicationContext(),"Making request, as distance is " + getDistance(currentLocation, mLastLocation)+ "m");
                saveCoordinates(currentLocation.getLatitude(), currentLocation.getLongitude());
                mLastRequestTime = Calendar.getInstance().getTimeInMillis();
                mLastLocation = currentLocation;
              } else {
                Logger.log(getApplicationContext(), "Skipping request, as time limit reached");
              }
            } else {
              Logger.log(getApplicationContext(), "NOT making request, as distance is only " + getDistance(currentLocation, mLastLocation) + "m or speed is " + currentLocation.getSpeed());
            }
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

  private void saveCoordinates(double lat, double lng) {
    Logger.log(getApplicationContext(), "Calling Endpoint (saveCoordinates) with [" + lat + ", " + lng + "]...");

    this.login()
        .flatMap(res -> this.commute(res.token, lat, lng))
        .subscribe(
            res -> Logger.log(getApplicationContext(), "Success: average=" + res.average + "min"),
            throwable -> {
              Logger.log(getApplicationContext(), "Error: " + throwable.getMessage());
              throwable.printStackTrace();
            }
        );
  }

  private void saveCommutingStatus(CommutingState state) {
    Logger.log(getApplicationContext(), "Calling Endpoint (saveCommutingStatus) with " + state.label + "...");

    this.login()
        .flatMap(res -> this.updateCommutingStatus(res.token, state))
        .subscribe(
            res -> Logger.log(getApplicationContext(), "Success: newState=" + res.newState),
            throwable -> {
              Logger.log(getApplicationContext(), "Error: " + throwable.getMessage());
              throwable.printStackTrace();
            }
        );
  }

  private boolean shouldStop(Location currentLocation) {
    if (mDebugMode) {
      return false;
    }
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

  private boolean shouldMakeRequest() {
    if (mDebugMode) {
      return true;
    }
    long elapsedSecondsSinceLastRequest = (Calendar.getInstance().getTimeInMillis() - mLastRequestTime);
    return elapsedSecondsSinceLastRequest >= REQUEST_INTERVAL;
  }

  private void stopService() {
    Logger.log(getApplicationContext(), "Service is about to stop...");

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

  // <SharedPreferences>
  private void saveToken(String token) {
    Logger.log(getApplicationContext(), "Got token, saving...");
    final SharedPreferences prefs = getApplicationContext().getSharedPreferences("secrets",
        Context.MODE_PRIVATE);
    final SharedPreferences.Editor edit = prefs.edit();
    edit.putString("token", token);
    edit.apply();
  }

  private String loadToken() {
    SharedPreferences prefs = getApplicationContext().getSharedPreferences("secrets",
        Context.MODE_PRIVATE);
    return prefs.getString("token", null);
  }

  private void removeToken() {
    final SharedPreferences prefs = getApplicationContext().getSharedPreferences("secrets",
        Context.MODE_PRIVATE);
    final SharedPreferences.Editor edit = prefs.edit();
    edit.remove("token");
    edit.apply();
  }
  // </SharedPreferences>

  // <HTTP Calls>
  private Observable<AuthResult> login() {
    final String token = this.loadToken();
    if (token != null) {
      Logger.log(getApplicationContext(), "Use local token");
      return Observable.just(new AuthResult(token, null));
    }

    Logger.log(getApplicationContext(), "No local token available, logging in...");
    return HttpClient.getApiService().login(new Credentials(BuildConfig.user, BuildConfig.password))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .doOnNext(res -> this.saveToken(res.token));
  }

  private Observable<CommutingResult> commute(String authToken, double lat, double lng) {
    Logger.log(getApplicationContext(), "Calling saveCoordinates endpoint...");
    return HttpClient.getApiService().commute("Bearer " + authToken, lat, lng, mDestination[0], mDestination[1])
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .onErrorResumeNext(err -> {
          Logger.log(getApplicationContext(), "Error: code=" + ((HttpException) err).code());
          if (((HttpException) err).code() == 401) {
            // unauthorized => login and retry
            Logger.log(getApplicationContext(), "unauthorized => login and retry");
            this.removeToken();
            return this.login().flatMap(res -> this.commute(res.token, lat, lng));
          }
          return Observable.error(err);
        });
  }

  private Observable<CommutingStatusResult> updateCommutingStatus(String authToken, CommutingState state) {
    Logger.log(getApplicationContext(), "Calling updateCommutingStatus endpoint...");
    return HttpClient.getApiService().updateCommutingStatus("Bearer " + authToken, state.label)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .onErrorResumeNext(err -> {
          Logger.log(getApplicationContext(), "Error: " + ((HttpException) err).code());
          if (((HttpException) err).code() == 401) {
            // unauthorized => login and retry
            this.removeToken();
            return this.login().flatMap(res -> this.updateCommutingStatus(res.token, state));
          }
          return Observable.error(err);
        });
  }
  // </HTTP Calls>
}
