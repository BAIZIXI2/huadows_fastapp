package top.niunaijun.blackbox.fake.install;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.File;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;

public class InstallInterceptor {
    public static final String TAG = "InstallInterceptor";

    /**
     * 检查并拦截APK安装意图
     * @param intent 要检查的意图
     * @return 如果已拦截并处理，返回 true；否则返回 false。
     */
    public static boolean intercept(Intent intent) {
        if (intent == null) {
            return false;
        }

        // 判断是否为 APK 安装意图
        if (Intent.ACTION_VIEW.equals(intent.getAction()) || "android.intent.action.INSTALL_PACKAGE".equals(intent.getAction())) {
            Uri data = intent.getData();
            String type = intent.getType();

            // 检查 MIME 类型是否为 APK
            if (data != null && "application/vnd.android.package-archive".equals(type)) {
                Log.d(TAG, "已拦截APK安装请求: " + data);

                // 黑盒框架为此提供了标准的回调机制，直接调用即可
                // BlackBoxCore会回调到你在App.java中配置的ClientConfiguration
                File file = new File(data.getPath()); // 注意：这里假设data是File Uri
                
                // 调用回调，通知宿主进行处理。
                // BlackBoxCore内部会处理好跨进程通信。
                boolean handled = BlackBoxCore.get().requestInstallPackage(file, BActivityThread.getUserId());
                if (handled) {
                    Log.d(TAG, "安装请求已由宿主处理。");
                    return true; // 返回 true，表示我们已经处理了这个 Intent
                }
            }
        }
        return false; // 不是安装意图，不拦截
    }
}