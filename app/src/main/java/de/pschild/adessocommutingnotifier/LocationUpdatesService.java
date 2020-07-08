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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class LocationUpdatesService extends Service {

  private static final String TAG = LocationUpdatesService.class.getSimpleName();
  private static final String CHANNEL_ID = "ForegroundServiceChannel";
  private static final int NOTIFICATION_ID = 12345678;

  private FusedLocationProviderClient fusedLocationClient;
  private LocationRequest locationRequest;

  @Override
  public void onCreate() {
    Log.d(TAG, "LocationUpdatesService.onCreate");
    super.onCreate();

    getLastLocation();
    createLocationRequest();
  }

  private void createLocationRequest() {
    locationRequest = LocationRequest.create();
    locationRequest.setInterval(10000);
    locationRequest.setFastestInterval(5000);
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    LocationCallback mLocationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        if (locationResult == null) {
          return;
        }
        for (Location location : locationResult.getLocations()) {
          if (location != null) {
            updateNotification("[" + location.getLatitude() + ", " + location.getLongitude() + "]");
          }
        }
      }
    };
    LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, mLocationCallback, null);
  }

  private void getLastLocation() {
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
      @Override
      public void onSuccess(Location location) {
        if (location != null) {
          updateNotification("[" + location.getLatitude() + ", " + location.getLongitude() + "]");
        }
      }
    });
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d(TAG, "LocationUpdatesService.onStartCommand");
    createNotificationChannel();

    startForeground(NOTIFICATION_ID, this.buildNotification("waiting for 1st location..."));

    runStopTimer();

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

  private Notification buildNotification(String text) {
    Intent notificationIntent = new Intent(this, MainActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

    return new NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Foreground Service")
        .setContentText(text)
//        .setOngoing(true)
        .setSmallIcon(R.drawable.ic_launcher_background)
        .setContentIntent(pendingIntent)
        .build();
  }

  private void updateNotification(String text) {
    Date c = Calendar.getInstance().getTime();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    String formattedDate = simpleDateFormat.format(c);
    Log.d(TAG, "LocationUpdatesService.updateNotification @" + formattedDate);
    Notification notification = buildNotification(text);
    NotificationManager mNotificationManager = (NotificationManager) getSystemService(
        Context.NOTIFICATION_SERVICE);
    mNotificationManager.notify(NOTIFICATION_ID, notification);
  }

  private void runStopTimer() {
    Log.d(TAG, "LocationUpdatesService.runStopTimer");
    // timeout
    new android.os.Handler().postDelayed(
        new Runnable() {
          public void run() {
            Log.d(TAG, "Stop timer triggered");
            stopSelf();
          }
        },
        1000 * 10);

    // interval
//    new Timer().scheduleAtFixedRate(new TimerTask() {
//      @Override
//      public void run() {
//        ...
//      }
//    }, 0, 2000);
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
}
