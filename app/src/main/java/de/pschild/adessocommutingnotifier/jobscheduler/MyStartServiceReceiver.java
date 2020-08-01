package de.pschild.adessocommutingnotifier.jobscheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyStartServiceReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Util.scheduleJob(context);
  }
}
