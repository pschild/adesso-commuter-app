package de.pschild.adessocommutingnotifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class LocationUpdatesService extends Service {

  private static final String TAG = LocationUpdatesService.class.getSimpleName();
  public static final String CHANNEL_ID = "ForegroundServiceChannel";

  @Override
  public void onCreate() {
    Log.d(TAG, "LocationUpdatesService.onCreate");
    super.onCreate();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d(TAG, "LocationUpdatesService.onStartCommand");
    String input = intent.getStringExtra("inputExtra");
    createNotificationChannel();
    Intent notificationIntent = new Intent(this, MainActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this,
        0, notificationIntent, 0);

    Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Foreground Service")
        .setContentText(input)
        .setSmallIcon(R.drawable.ic_launcher_background)
        .setContentIntent(pendingIntent)
        .build();
    startForeground(1, notification);

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

  private void doHeavyWorkOnABackgroundThread() {
    Log.d(TAG, "LocationUpdatesService.doHeavyWorkOnABackgroundThread");
    new android.os.Handler().postDelayed(
        new Runnable() {
          public void run() {
            Log.i(TAG, "This'll run 300 milliseconds later");
            stopSelf();
          }
        },
        5000);
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
