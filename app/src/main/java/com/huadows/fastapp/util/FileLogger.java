// 文件路径: app/src/main/java/com/huadows/fastapp/util/FileLogger.java
package com.huadows.fastapp.util;

import android.content.Context;
import android.util.Log;

import com.huadows.fastapp.App;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileLogger {

    private static final String FILENAME = "filelog.txt";
    private static File logFile;

    private static void initialize() {
        if (logFile == null) {
            Context context = App.getContext();
            if (context != null) {
                // getExternalFilesDir(null) -> /storage/emulated/0/Android/data/com.huadows.fastapp/files
                File filesDir = context.getExternalFilesDir(null);
                if (filesDir != null) {
                    if (!filesDir.exists()) {
                        filesDir.mkdirs();
                    }
                    logFile = new File(filesDir, FILENAME);
                }
            }
        }
    }

    public static synchronized void log(String tag, String message) {
        // 在写入前，也用系统 Logcat 输出一次，以防万一
        Log.d(tag, message);

        initialize();
        if (logFile == null) {
            Log.e("FileLogger", "Failed to initialize log file. Context might be null.");
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(logFile, true);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
            writer.append(timestamp)
                  .append(" [").append(tag).append("] ")
                  .append(message)
                  .append("\n");
        } catch (IOException e) {
            Log.e("FileLogger", "Failed to write to log file", e);
        }
    }
    
    public static void clear() {
        initialize();
        if (logFile != null && logFile.exists()) {
            logFile.delete();
        }
    }
}