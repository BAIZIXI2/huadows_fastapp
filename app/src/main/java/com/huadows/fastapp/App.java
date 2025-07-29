// 文件路径: app/src/main/java/com/huadows/fastapp/App.java
package com.huadows.fastapp;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.huadows.fastapp.server.AuthServiceImpl;
// WebServerManager 仍然需要导入，但不再直接调用 start
import com.huadows.fastapp.server.WebServerManager;
import com.huadows.fastapp.util.GlobalCrashHandler;
import com.huadows.fastapp.view.InstallChooserActivity;

import java.io.File;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.configuration.ClientConfiguration;
import top.niunaijun.blackbox.core.system.ServiceManager;
import top.niunaijun.blackbox.utils.Slog;

public class App extends Application {

    private static final String TAG = "FastApp";
    private static Context sContext;

    public static final String PREFS_NAME = "FastAppPrefs";
    public static final String KEY_AGREE_RISK = "agree_risk_warning";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sContext = base;
        try {
            ClientConfiguration config = new ClientConfiguration() {
                @Override
                public String getHostPackageName() {
                    return base.getPackageName();
                }

                @Override
                public boolean requestInstallPackage(File file, int userId) {
                    Slog.d(TAG, "从虚拟应用收到安装请求: " + file.getAbsolutePath() + ", 用户ID: " + userId);

                    Intent intent = new Intent();
                    intent.setClass(App.getContext(), InstallChooserActivity.class);
                    intent.setData(Uri.fromFile(file));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    App.getContext().startActivity(intent);
                    return true;
                }
            };
            BlackBoxCore.get().doAttachBaseContext(base, config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        GlobalCrashHandler.getInstance().init(this);
        BlackBoxCore.get().doCreate();

        if (BlackBoxCore.get().isServerProcess()) {
            Log.d(TAG, "Server process (:blackbox) is initializing...");
            
            // 1. 注册认证服务
            ServiceManager.get().addService(ServiceManager.AUTH_SERVICE, AuthServiceImpl.get());
            
            // 2. Web 服务器不再此处启动，而是由 AuthServiceImpl 按需启动
            // WebServerManager.get().start(this); // <--- 移除此行
            
            Log.d(TAG, "AuthService registered. WebServer will start on demand.");

        } else if (BlackBoxCore.get().isMainProcess()) {
            Log.d(TAG, "Main process is initializing.");
        }
    }

    public static Context getContext() {
        return sContext;
    }

    public static boolean hasAgreedToRiskWarning() {
        SharedPreferences prefs = sContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_AGREE_RISK, false);
    }

    public static void setAgreedToRiskWarning(boolean agreed) {
        SharedPreferences prefs = sContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_AGREE_RISK, agreed);
        editor.apply();
    }
}