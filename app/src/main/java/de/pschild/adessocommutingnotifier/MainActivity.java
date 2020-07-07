package de.pschild.adessocommutingnotifier;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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

    requestPermissions();

    alarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

    // when
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(System.currentTimeMillis());
    calendar.set(Calendar.HOUR_OF_DAY, 6);
    calendar.set(Calendar.MINUTE, 30);

    // what
    Intent intent = new Intent(this, AlarmReceiver.class);
    alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

    // schedule!
    alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
//    alarmMgr.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME,SystemClock.elapsedRealtime() + 1000 * 5, alarmIntent);
  }

  private void requestPermissions() {
    ActivityCompat.requestPermissions(
        MainActivity.this,
        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
        34 // Used in checking for runtime permissions.
    );
  }

  @Override
  protected void onStop() {
    super.onStop();
  }
}
