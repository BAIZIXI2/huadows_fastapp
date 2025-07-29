// 文件路径: app/src/main/java/com/huadows/fastapp/server/AuthServiceImpl.java
package com.huadows.fastapp.server;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.util.Log;

import com.huadows.fastapp.App;
import com.huadows.fastapp.auth.IAuthService;
import com.huadows.fastapp.util.FileLogger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.system.ISystemService;

public class AuthServiceImpl extends IAuthService.Stub implements ISystemService {
    private static final String TAG = "AuthServiceImpl";
    private static AuthServiceImpl sService;

    public static final String KEY_PORT = "port";
    public static final String KEY_TOKEN = "token";

    private final Context mContext;
    private final Set<String> mWhitelist = new HashSet<>();

    private static volatile int sWebServerPort = 0;
    private static final Object sPortLock = new Object();

    private AuthServiceImpl() {
        this.mContext = App.getContext();
        if (this.mContext == null) {
            throw new IllegalStateException("AuthServiceImpl constructed before App context is ready.");
        }
        mWhitelist.add("com.huadows.store");
        mWhitelist.add("com.huadows.fastappdebug");
    }

    public static synchronized AuthServiceImpl get() {
        if (sService == null) {
            sService = new AuthServiceImpl();
        }
        return sService;
    }

    /**
     * 新增: 返回当前 Web 服务器正在运行的端口。
     * @return 正在运行的端口号，如果服务器未运行则返回 0。
     */
    public static int getWebServerPort() {
        return sWebServerPort;
    }

    @Override
    public void systemReady() {
        Log.d(TAG, "AuthService is ready.");
        FileLogger.clear();
        FileLogger.log(TAG, "AuthService is ready.");
    }

    @Override
    public Bundle performHandshake(String clientPackageName) {
        try {
            FileLogger.log(TAG, "--- New Handshake Request ---");
            if (!isCallerLegitimate(clientPackageName)) {
                FileLogger.log(TAG, "Handshake FAILED: Caller validation failed for " + clientPackageName);
                return null;
            }

            synchronized (sPortLock) {
                if (sWebServerPort == 0) {
                    sWebServerPort = findAvailablePort();
                    if (sWebServerPort == -1) {
                        FileLogger.log(TAG, "Handshake FAILED: Could not find an available port.");
                        return null;
                    }
                    WebServerManager.get().start(mContext, sWebServerPort);
                    
                    // 等待 Web 服务器完全启动
                    FileLogger.log(TAG, "Waiting for WebServer to start on port: " + sWebServerPort);
                    WebServerManager.get().awaitStart();
                    
                    FileLogger.log(TAG, "WebServer started and ready.");
                }
            }

            String token = TokenManager.get().generateToken(clientPackageName);

            Bundle result = new Bundle();
            result.putInt(KEY_PORT, sWebServerPort);
            result.putString(KEY_TOKEN, token);

            FileLogger.log(TAG, "Handshake SUCCESS for " + clientPackageName + ". Returning port " + sWebServerPort + " and token.");
            return result;

        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            FileLogger.log(TAG, "FATAL ERROR during handshake: " + t.getMessage() + "\n" + sw);
            // 如果出错，重置端口，以便下次重试
            sWebServerPort = 0;
            return null;
        }
    }

    @Override
    public boolean launchApp(String packageName, int userId) {
        FileLogger.log(TAG, "--- New Launch App Request ---");
        if (!isCallerLegitimate(null)) {
            FileLogger.log(TAG, "Launch App FAILED: Caller validation failed for calling UID " + Binder.getCallingUid());
            return false;
        }
        FileLogger.log(TAG, "Launch App authorized for UID " + Binder.getCallingUid() + ", proceeding to launch: " + packageName);
        return BlackBoxCore.get().launchApk(packageName, userId);
    }

    private boolean isCallerLegitimate(String claimedPackageName) {
        final int callingUid = Binder.getCallingUid();
        FileLogger.log(TAG, "Validating caller. UID: " + callingUid + ", Claimed Package: " + claimedPackageName);
        if (mContext == null) {
            FileLogger.log(TAG, "Validation FAILED: mContext is null!");
            return false;
        }
        PackageManager pm = mContext.getPackageManager();
        String[] packages = pm.getPackagesForUid(callingUid);

        if (packages == null || packages.length == 0) {
            FileLogger.log(TAG, "Validation FAILED: UID " + callingUid + " has no associated packages.");
            return false;
        }
        FileLogger.log(TAG, "UID " + callingUid + " maps to packages: " + Arrays.toString(packages));

        for (String pkg : packages) {
            boolean packageMatch = (claimedPackageName == null) || pkg.equals(claimedPackageName);
            boolean whitelistMatch = mWhitelist.contains(pkg);
            if (packageMatch && whitelistMatch) {
                FileLogger.log(TAG, "Validation PASSED for package: " + pkg);
                return true;
            }
        }
        FileLogger.log(TAG, "Validation FAILED: No valid and whitelisted package found for UID " + callingUid);
        return false;
    }

    private int findAvailablePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            return -1;
        }
    }
}