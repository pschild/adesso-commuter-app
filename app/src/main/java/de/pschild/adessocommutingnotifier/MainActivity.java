package de.pschild.adessocommutingnotifier;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    requestPermissions();

    // unsure whether this is necessary.
    if (!isIgnoringBatteryOptimizations(this)) {
      Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
      intent.setData(Uri.parse("package:" + getPackageName()));
      startActivity(intent);
    }

    Logger.log(this, "MainActivity.onCreate");
    Scheduler.schedule(this);
  }

  private void requestPermissions() {
    ActivityCompat.requestPermissions(
        MainActivity.this,
        new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        },
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
