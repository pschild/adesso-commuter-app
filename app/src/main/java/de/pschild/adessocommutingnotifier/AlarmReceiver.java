package de.pschild.adessocommutingnotifier;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.util.Log;
import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {

  private static final String TAG = AlarmReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.i(TAG, "AlarmReceiver.onReceive");
    Logger.log(context, "AlarmReceiver.onReceive");
    this.scheduleNextAlarm(context);
    this.startLocationService(context);
  }

  private void scheduleNextAlarm(Context context) {
//    if (isWeekend()) {
//      // TODO: if Sat. or Sun., schedule for Mon.
//      return;
//    }

    // TODO: remove code duplication
    Calendar calendar = Calendar.getInstance();
    if (calendar.get(Calendar.HOUR_OF_DAY) < 12) {
      // in the morning, schedule next alarm for 15:30
      calendar.set(Calendar.HOUR_OF_DAY, 15);
      calendar.set(Calendar.MINUTE, 0);
    } else {
      // in the afternoon, schedule next alarm for 05:30
      calendar.set(Calendar.HOUR_OF_DAY, 5);
      calendar.set(Calendar.MINUTE, 30);
    }
    calendar.set(Calendar.SECOND, 0);

    long start = calendar.getTimeInMillis();
    if (calendar.before(Calendar.getInstance())) {
      start = start + AlarmManager.INTERVAL_DAY;
    }

    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(start);
    Logger.log(context, "AlarmReceiver.scheduleNextAlarm for " + DateFormat.format("yyyy-MM-dd HH:mm:ss", cal).toString());

    Intent alarmIntent = new Intent(context, AlarmReceiver.class);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 42, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, start, pendingIntent);
  }

  private void startLocationService(Context context) {
    Logger.log(context, "AlarmReceiver.startLocationService");
    Intent serviceIntent = new Intent(context, LocationUpdatesService.class);
    context.startForegroundService(serviceIntent);
  }

  private boolean isWeekend() {
    Calendar calendar = Calendar.getInstance();
    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
    return (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY);
  }
}
