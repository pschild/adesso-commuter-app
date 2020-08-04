package de.pschild.adessocommutingnotifier;

import android.app.AlarmManager;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;

public class Scheduler {

  public static Calendar calculateNextAlarm() {
//    if (isWeekend()) {
//      // TODO: if Sat. or Sun., schedule for Mon.
//      return null;
//    }

    String[] triggerTimes = getTriggerTimes();
    Arrays.sort(triggerTimes, Comparator.comparing(LocalTime::parse));
    Calendar now = now();
    final int nowHour = now.get(Calendar.HOUR_OF_DAY);
    final int nowMinute = now.get(Calendar.MINUTE);

    String candidate = null;
    for (String time : triggerTimes) {
      String[] timeParts = time.split(":");
      int timeHour = Integer.parseInt(timeParts[0]);
      int timeMinute = Integer.parseInt(timeParts[1]);
      if (nowHour < timeHour || (nowHour == timeHour && nowMinute < timeMinute)) {
        candidate = time;
        break;
      }
    }

    if (candidate == null) {
      candidate = triggerTimes[0];
    }

    String[] candidateParts = candidate.split(":");
    Calendar nextAlarm = Calendar.getInstance();
    nextAlarm.set(Calendar.HOUR_OF_DAY, Integer.parseInt(candidateParts[0]));
    nextAlarm.set(Calendar.MINUTE, Integer.parseInt(candidateParts[1]));
    nextAlarm.set(Calendar.SECOND, 0);
    nextAlarm.set(Calendar.MILLISECOND, 0);

    long startTime = nextAlarm.getTimeInMillis();
    if (nextAlarm.before(now)) {
      startTime = startTime + AlarmManager.INTERVAL_DAY;
    }
    nextAlarm.setTimeInMillis(startTime);
    return nextAlarm;
  }

  public static String[] getTriggerTimes() {
    // format MUST be HH:mm (incl. leading zero) - order of entries does NOT matter
    return new String[]{"05:30", "15:00"};
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
