package com.huadows.fastappdebug;

import android.app.Application;
import android.content.Context; // 导入 Context
import android.content.Intent;
import android.util.Log; // 导入 Log 用于调试输出

import java.io.PrintWriter;
import java.io.StringWriter;

// 导入 FileLog 类
// 导入 AppSettingsManager 类


public class CrashHandlerApplication extends Application {

    private static final String TAG = "CrashApp"; // Application 类的 TAG

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate started."); // 添加日志

        // --- 初始化文件日志 (原有代码) ---
        // 在 Application 的 onCreate 中调用 FileLog.setup() 来配置日志器
        FileLog.setup(this, "app_log.txt"); // 指定日志文件名为 app_log.txt
        FileLog.log(TAG, "INFO: Application onCreate completed, FileLog configured."); // 记录一条日志来测试配置是否成功


       

        // --- 设置默认的未捕获异常处理器 (原有代码) ---
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {

            // --- 将崩溃信息写入文件日志 (原有代码) ---
            String crashStackTrace = getStackTrace(throwable);
            // 使用 ERROR 级别记录崩溃信息
            FileLog.log("CRASH", "ERROR: Uncaught Exception on thread " + thread.getName() + ": " + crashStackTrace);

            // --- 启动错误显示 Activity (您的原有逻辑) ---
            Intent intent = new Intent(getApplicationContext(), ErrorActivity.class); // ErrorActivity 确保已定义和导入
            intent.putExtra("error", crashStackTrace); // 将堆栈跟踪放入 Intent
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // --- 终止应用进程 (您的原有逻辑) ---
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        });

        Log.d(TAG, "DefaultUncaughtExceptionHandler set."); // 添加日志
        Log.d(TAG, "Application onCreate finished."); // 添加日志
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG, "Application onTerminate started."); // 添加日志

       
        Log.d(TAG, "Application onTerminate finished."); // 添加日志
    }


    // --- 获取堆栈跟踪的方法 (原有代码) ---
    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}