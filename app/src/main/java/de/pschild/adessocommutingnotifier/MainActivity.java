package de.pschild.adessocommutingnotifier;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();

  private AlarmManager alarmMgr;
  private PendingIntent alarmIntent;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.i(TAG, "MainActivity.onCreate");

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    alarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

    // when
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(System.currentTimeMillis());
    calendar.set(Calendar.HOUR_OF_DAY, 20);
    calendar.set(Calendar.MINUTE, 23);

    // what
    Intent intent = new Intent(this, AlarmReceiver.class);
    alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

    // schedule!
//    alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
    alarmMgr.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000 * 5, alarmIntent);

    // start foreground service on start of activity
//    Intent serviceIntent = new Intent(this, LocationUpdatesService.class);
//    serviceIntent.putExtra("inputExtra", "Foreground Service Example in Android");
//    startForegroundService(serviceIntent);
  }

  @Override
  protected void onStop() {
    super.onStop();
  }
}
