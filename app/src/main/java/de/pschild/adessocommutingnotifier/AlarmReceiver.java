package de.pschild.adessocommutingnotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

  private static final String TAG = AlarmReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.i(TAG, "AlarmReceiver.onReceive");

    // start foreground service
    Intent serviceIntent = new Intent(context, LocationUpdatesService.class);
    serviceIntent.putExtra("inputExtra", "Foreground Service from Alarm");
    context.startForegroundService(serviceIntent);
  }
}
