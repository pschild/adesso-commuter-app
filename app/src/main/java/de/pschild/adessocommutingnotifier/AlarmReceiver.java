package de.pschild.adessocommutingnotifier;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Logger.log(context, "AlarmReceiver.onReceive");
    this.scheduleNextAlarm(context);
    this.startLocationService(context);
  }

  private void scheduleNextAlarm(Context context) {
    Calendar nextAlarm = Scheduler.calculateNextAlarm();
    Logger.log(context, "AlarmReceiver.scheduleNextAlarm for " + DateFormat.format("yyyy-MM-dd HH:mm:ss", nextAlarm).toString());

    Intent alarmIntent = new Intent(context, AlarmReceiver.class);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 42, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarm.getTimeInMillis(), pendingIntent);
  }

  private void startLocationService(Context context) {
    Logger.log(context, "AlarmReceiver.startLocationService");
    Intent serviceIntent = new Intent(context, LocationUpdatesService.class);
    context.startForegroundService(serviceIntent);
  }
}
