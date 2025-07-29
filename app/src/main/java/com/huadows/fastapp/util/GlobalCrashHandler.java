// /app/src/main/java/com/huadows/fastapp/util/GlobalCrashHandler.java
package com.huadows.fastapp.util;

import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;

import com.huadows.fastapp.view.ErrorActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

public class GlobalCrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "GlobalCrashHandler";
    private static volatile GlobalCrashHandler sInstance;
    private Context mContext;
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private GlobalCrashHandler() {}

    public static GlobalCrashHandler getInstance() {
        if (sInstance == null) {
            synchronized (GlobalCrashHandler.class) {
                if (sInstance == null) {
                    sInstance = new GlobalCrashHandler();
                }
            }
        }
        return sInstance;
    }

    public void init(Context context) {
        mContext = context.getApplicationContext();
        // 获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        Log.e(TAG, "uncaughtException: ", e);
        handleException(e);

        // 如果系统提供了默认的处理器，则交给系统处理，否则就自己结束进程
        if (mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(t, e);
        } else {
            Process.killProcess(Process.myPid());
            System.exit(1);
        }
    }

    private void handleException(Throwable e) {
        // 将异常信息转换为字符串
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        String errorLog = sw.toString();

        // 启动错误展示Activity
        Intent intent = new Intent(mContext, ErrorActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(ErrorActivity.EXTRA_ERROR_TEXT, errorLog);
        mContext.startActivity(intent);
    }
}