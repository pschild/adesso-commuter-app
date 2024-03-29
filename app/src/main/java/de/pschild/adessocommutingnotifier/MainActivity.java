package de.pschild.adessocommutingnotifier;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import de.pschild.adessocommutingnotifier.api.HttpClient;
import de.pschild.adessocommutingnotifier.api.model.AuthResult;
import de.pschild.adessocommutingnotifier.api.model.CommutingResult;
import de.pschild.adessocommutingnotifier.api.model.Credentials;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.io.File;

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

    Button button = findViewById(R.id.button2);
    button.setOnClickListener(v -> {
      this.login()
          .flatMap(res -> this.commute(res.token))
          .subscribe(
              res -> Logger.log(this, "Yey, success!"),
              throwable -> {
                Logger.log(this, "Oh no, there was an error: " + throwable.getMessage());
                throwable.printStackTrace();
              }
          );
    });

    Button startServiceBtn = findViewById(R.id.startServiceBtn);
    startServiceBtn.setOnClickListener(v -> {
      Intent serviceIntent = new Intent(this, LocationUpdatesService.class);
      serviceIntent.setAction("debug");
      startForegroundService(serviceIntent);
    });

    Button stopServiceBtn = findViewById(R.id.stopServiceBtn);
    stopServiceBtn.setOnClickListener(v -> {
      Intent serviceIntent = new Intent(this, LocationUpdatesService.class);
      serviceIntent.setAction("stop");
      startForegroundService(serviceIntent);
    });

    Button openLogBtn = findViewById(R.id.openLogBtn);
    openLogBtn.setOnClickListener(v -> {
      this.openLog();
    });
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

  private void saveToken(String token) {
    Logger.log(this, "Got token, saving...");
    final SharedPreferences prefs = getApplicationContext().getSharedPreferences("secrets",
        Context.MODE_PRIVATE);
    final SharedPreferences.Editor edit = prefs.edit();
    edit.putString("token", token);
    edit.apply();
  }

  private String loadToken() {
    SharedPreferences prefs = getApplicationContext().getSharedPreferences("secrets",
        Context.MODE_PRIVATE);
    return prefs.getString("token", null);
  }

  private void removeToken() {
    final SharedPreferences prefs = getApplicationContext().getSharedPreferences("secrets",
        Context.MODE_PRIVATE);
    final SharedPreferences.Editor edit = prefs.edit();
    edit.remove("token");
    edit.apply();
  }

  private Observable<AuthResult> login() {
    final String token = this.loadToken();
    if (token != null) {
      Logger.log(this, "Use local token");
      return Observable.just(new AuthResult(token, null));
    }

    Logger.log(this, "No local token available, logging in...");
    return HttpClient.getApiService().login(new Credentials(BuildConfig.user, BuildConfig.password))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .doOnNext(res -> this.saveToken(res.token));
  }

  private Observable<CommutingResult> commute(String authToken) {
    Logger.log(this, "Calling save endpoint...");
    return HttpClient.getApiService().commute("Bearer " + authToken, 51.1, 6.2, 51.2, 6.3)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .onErrorResumeNext(err -> {
          Logger.log(this, "Error: " + ((HttpException) err).code());
          if (((HttpException) err).code() == 401) {
            // unauthorized => login and retry
            this.removeToken();
            return this.login().flatMap(res -> this.commute(res.token));
          }
          return Observable.error(err);
        });
  }

  private void openLog() {
    File documentDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
    File file = new File(documentDir, "Log.txt");
    Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);

    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setDataAndType(uri, "*/*");
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    startActivity(intent);
  }
}
