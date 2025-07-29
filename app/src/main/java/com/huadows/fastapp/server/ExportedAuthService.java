// 文件路径: app/src/main/java/com/huadows/fastapp/server/ExportedAuthService.java
package com.huadows.fastapp.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import top.niunaijun.blackbox.BlackBoxCore; // 导入 BlackBoxCore
import top.niunaijun.blackbox.core.system.ServiceManager; // 导入 ServiceManager

/**
 * 这是一个标准的、可导出的 Android Service。
 * 它现在的职责是从 BlackBox 的 ServiceManager 中获取已注册的 AuthService。
 */
public class ExportedAuthService extends Service {
    private static final String TAG = "ExportedAuthService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // ====================== 代码修改开始 ======================
        Log.d(TAG, "onBind: Client is binding. Fetching service from ServiceManager.");
        
        // 从 BlackBox 的 ServiceManager 获取已经注册并准备好的服务。
        // BlackBoxCore.get().getService() 会返回一个 IBinder 对象。
        IBinder binder = BlackBoxCore.get().getService(ServiceManager.AUTH_SERVICE);

        if (binder == null) {
            Log.e(TAG, "onBind: Failed to get AUTH_SERVICE from ServiceManager. It might not be registered yet.");
        } else {
            Log.i(TAG, "onBind: Successfully retrieved AUTH_SERVICE from ServiceManager.");
        }
        
        // 将获取到的 Binder 直接返回给客户端
        return binder;
        // ====================== 代码修改结束 ======================
    }
}