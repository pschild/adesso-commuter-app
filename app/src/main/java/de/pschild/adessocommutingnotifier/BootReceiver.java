package de.pschild.adessocommutingnotifier;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
      Logger.log(context, "BootReceiver.onReceive");
      // TODO: <DUPLICATE_CODE>
      Calendar nextAlarm = Scheduler.calculateNextAlarm();
      Logger.log(context, "BootReceiver.scheduleNextAlarm for " + DateFormat.format("yyyy-MM-dd HH:mm:ss", nextAlarm).toString());

      Intent alarmIntent = new Intent(context, AlarmReceiver.class);
      PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 42, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
      AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
      am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarm.getTimeInMillis(), pendingIntent);
      // TODO: </DUPLICATE_CODE>
    }
  }
}
