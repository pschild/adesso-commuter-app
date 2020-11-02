package de.pschild.adessocommutingnotifier;

import android.content.Context;
import android.os.Environment;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logger {

  public static BufferedWriter out;

  /**
   * Logs the given text to the file at "storage/emulated/0/Android/data/de.pschild.adessocommutingnotifier/files/Documents/Log.txt".
   * Use `./adb.exe pull storage/emulated/0/Android/data/de.pschild.adessocommutingnotifier/files/Documents/Log.txt C:/Users/schild/Desktop/Log.txt`
   * to copy the logfile to your local HDD.
   * @param ctx
   * @param text
   */
  public static void log(Context ctx, String text) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    File documentDir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
    if (documentDir.canWrite()) {
      try {
        File logFile = new File(documentDir, "Log.txt");
        FileWriter logWriter = new FileWriter(logFile, true);
        out = new BufferedWriter(logWriter);
        out.write(sdf.format(Calendar.getInstance().getTime()) + ": " + text + "\n");
        out.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
