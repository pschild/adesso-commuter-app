package de.pschild.adessocommutingnotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
      Logger.log(context, "BootReceiver.onReceive");
      Scheduler.schedule(context);
    }
  }
}
