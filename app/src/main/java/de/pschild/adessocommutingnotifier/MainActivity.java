package de.pschild.adessocommutingnotifier;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.i(TAG, "MainActivity.onCreate");

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    requestPermissions();

    // unsure whether this is necessary.
    if (!isIgnoringBatteryOptimizations(this)) {
      Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
      intent.setData(Uri.parse("package:" + getPackageName()));
      startActivity(intent);
    }

    // TODO: remove code duplication
    Calendar calendar = Calendar.getInstance();
    if (calendar.get(Calendar.HOUR_OF_DAY) < 12) {
      // in the morning, schedule next alarm for 15:30
      calendar.set(Calendar.HOUR_OF_DAY, 15);
      calendar.set(Calendar.MINUTE, 0);
      // in the afternoon, schedule next alarm for 05:30
    } else {
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
    Logger.log(this, "MainActivity.scheduleNextAlarm for " + DateFormat.format("yyyy-MM-dd HH:mm:ss", cal).toString());

    Intent alarmIntent = new Intent(this, AlarmReceiver.class);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 42, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, start, pendingIntent);
  }

  private void requestPermissions() {
    ActivityCompat.requestPermissions(
        MainActivity.this,
        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
        34 // Used in checking for runtime permissions.
    );
  }

  private boolean isIgnoringBatteryOptimizations(Context context) {
    PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
    }
    return true;
  }

  @Override
  protected void onStop() {
    super.onStop();
  }
}
