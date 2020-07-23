package de.pschild.adessocommutingnotifier;

import android.app.AlarmManager;
import java.util.Calendar;

public class Scheduler {

  public static Calendar calculateNextAlarm() {
//    if (isWeekend()) {
//      // TODO: if Sat. or Sun., schedule for Mon.
//      return null;
//    }

    Calendar now = now();
    Calendar nextAlarm = Calendar.getInstance();

    final int hourOfDay = now.get(Calendar.HOUR_OF_DAY);
    final int minute = now.get(Calendar.MINUTE);
    if (hourOfDay < 2 || hourOfDay >= 15) {
      // if alarm appears before 2:00 of after 15:00, schedule next alarm for 2:00
      nextAlarm.set(Calendar.HOUR_OF_DAY, 2);
      nextAlarm.set(Calendar.MINUTE, 0);
    } else if (hourOfDay < 5 || (hourOfDay == 5 && minute < 30)) {
      // else if alarm appears before 5:30, schedule next alarm for 5:30
      nextAlarm.set(Calendar.HOUR_OF_DAY, 5);
      nextAlarm.set(Calendar.MINUTE, 30);
    } else {
      // else, schedule next alarm for 15:00
      nextAlarm.set(Calendar.HOUR_OF_DAY, 15);
      nextAlarm.set(Calendar.MINUTE, 0);
    }
    nextAlarm.set(Calendar.SECOND, 0);
    nextAlarm.set(Calendar.MILLISECOND, 0);

    long startTime = nextAlarm.getTimeInMillis();
    if (nextAlarm.before(now)) {
      startTime = startTime + AlarmManager.INTERVAL_DAY;
    }
    nextAlarm.setTimeInMillis(startTime);
    return nextAlarm;
  }

  public static Calendar now() {
    return Calendar.getInstance();
  }

  private static boolean isWeekend() {
    Calendar now = now();
    final int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
    return (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY);
  }

}
