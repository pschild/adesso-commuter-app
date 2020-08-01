package de.pschild.adessocommutingnotifier.jobscheduler;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;

public class TestJobService extends JobService {
  private static final String TAG = "SyncService";

  @Override
  public boolean onStartJob(JobParameters params) {
    Intent service = new Intent(getApplicationContext(), LocalWordService.class);
    getApplicationContext().startService(service);
    Util.scheduleJob(getApplicationContext()); // reschedule the job
    return true;
  }

  @Override
  public boolean onStopJob(JobParameters params) {
    return true;
  }

}
