package de.pschild.adessocommutingnotifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.util.Timer;
import java.util.TimerTask;

public class LocationUpdatesService extends Service {

  private static final String TAG = LocationUpdatesService.class.getSimpleName();
  private static final String CHANNEL_ID = "ForegroundServiceChannel";
  private static final int NOTIFICATION_ID = 12345678;

  private int mCounter = 0;

  @Override
  public void onCreate() {
    Log.d(TAG, "LocationUpdatesService.onCreate");
    super.onCreate();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d(TAG, "LocationUpdatesService.onStartCommand");
    createNotificationChannel();

    startForeground(NOTIFICATION_ID, this.buildNotification(intent.getStringExtra("inputExtra")));

    doHeavyWorkOnABackgroundThread();

    //stopSelf();

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
    Notification notification = buildNotification(text);
    NotificationManager mNotificationManager = (NotificationManager) getSystemService(
        Context.NOTIFICATION_SERVICE);
    mNotificationManager.notify(NOTIFICATION_ID, notification);
  }

  private void doHeavyWorkOnABackgroundThread() {
    Log.d(TAG, "LocationUpdatesService.doHeavyWorkOnABackgroundThread");
    // timeout
//    new android.os.Handler().postDelayed(
//        new Runnable() {
//          public void run() {
//            Log.i(TAG, "This'll run 300 milliseconds later");
//            stopSelf();
//          }
//        },
//        5000);

    // interval
//    new Timer().scheduleAtFixedRate(new TimerTask() {
//      @Override
//      public void run() {
//        Log.i(TAG, "each 2s");
//        updateNotification("x=" + mCounter);
//        mCounter++;
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
