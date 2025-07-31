package com.huadows.fastapp.server;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Bundle;
import android.util.Log;

import com.huadows.fastapp.App;
import com.huadows.fastapp.auth.IAuthService;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.system.ISystemService;

public class AuthServiceImpl extends IAuthService.Stub implements ISystemService {
    private static final String TAG = "AuthServiceImpl";
    private static AuthServiceImpl sService;

    public static final String KEY_PORT = "port";
    public static final String KEY_TOKEN = "token";

    // 白名单相关常量
    private static final String WHITELIST_FILENAME = "fastapp_api_white_package.json";
    private static final String PRIMARY_WHITELIST_URL = "https://huadows.cn/api/getFastappApiWhitePackage.php";
    private static final String FALLBACK_WHITELIST_URL = "https://baizixi2.github.io/fastapp_api_white_package.json";
    private static final int WHITELIST_FETCH_TIMEOUT_MS = 10000; // 10秒超时

    private final Context mContext;
    private static final Set<String> BUILT_IN_WHITELIST = new HashSet<>(Arrays.asList("com.huadows.store"));
    private volatile Set<String> mDynamicWhitelist = Collections.synchronizedSet(new HashSet<>());
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Object mWhitelistLock = new Object();

    private static volatile int sWebServerPort = 0;
    private static final Object sPortLock = new Object();

    private AuthServiceImpl() {
        this.mContext = App.getContext();
        if (this.mContext == null) {
            throw new IllegalStateException("AuthServiceImpl constructed before App context is ready.");
        }
        // 启动时只从缓存加载一次，后续更新由握手触发
        loadWhitelistFromCache();
    }

    public static synchronized AuthServiceImpl get() {
        if (sService == null) {
            sService = new AuthServiceImpl();
        }
        return sService;
    }

    public static int getWebServerPort() {
        return sWebServerPort;
    }

    @Override
    public void systemReady() {
        Log.d(TAG, "AuthService is ready.");
        //.clear();
        //.log(TAG, "AuthService is ready.");
    }

    @Override
    public Bundle performHandshake(String clientPackageName) {
        try {
            //.log(TAG, "--- New Handshake Request from: " + clientPackageName + " ---");

            // 1. 立即检查调用者是否在内置白名单中
            final int callingUid = Binder.getCallingUid();
            String[] packagesForUid = mContext.getPackageManager().getPackagesForUid(callingUid);
            boolean isBuiltIn = false;
            if (packagesForUid != null) {
                for (String pkg : packagesForUid) {
                    if (BUILT_IN_WHITELIST.contains(pkg)) {
                        isBuiltIn = true;
                        break;
                    }
                }
            }

            File cacheFile = getCacheFile();
            boolean hasCache = cacheFile.exists() && cacheFile.length() > 0;

            // 2. 根据不同情况决定白名单更新策略
            if (hasCache) {
                // 有缓存：非阻塞更新
                //.log(TAG, "Cache exists. Performing non-blocking whitelist update.");
                updateWhitelistFromNetwork(false, null);
            } else if (isNetworkConnected() && !isBuiltIn) {
                // 无缓存 & 有网络 & 非内置应用：阻塞更新
                //.log(TAG, "No cache and not a built-in app. Performing blocking whitelist update...");
                AtomicReference<Boolean> updateResult = new AtomicReference<>();
                updateWhitelistFromNetwork(true, updateResult); // 阻塞等待结果

                if (updateResult.get() == null || !updateResult.get()) {
                    //.log(TAG, "Handshake FAILED: Blocking whitelist update failed or timed out.");
                    return null;
                }
                //.log(TAG, "Blocking whitelist update successful.");
            } else {
                //.log(TAG, "No cache, but no network or is a built-in app. Skipping network update.");
            }

            // 3. 执行调用者合法性检查（使用当前最新的白名单）
            if (!isCallerLegitimate(clientPackageName)) {
                //.log(TAG, "Handshake FAILED: Caller validation failed for " + clientPackageName);
                return null;
            }

            // 4. 启动Web服务器并生成Token（与之前逻辑相同）
            synchronized (sPortLock) {
                if (sWebServerPort == 0) {
                    sWebServerPort = findAvailablePort();
                    if (sWebServerPort == -1) {
                        //.log(TAG, "Handshake FAILED: Could not find an available port.");
                        return null;
                    }
                    WebServerManager.get().start(mContext, sWebServerPort);
                    //.log(TAG, "Waiting for WebServer to start on port: " + sWebServerPort);
                    WebServerManager.get().awaitStart();
                    //.log(TAG, "WebServer started and ready.");
                }
            }

            String token = TokenManager.get().generateToken(clientPackageName);
            Bundle result = new Bundle();
            result.putInt(KEY_PORT, sWebServerPort);
            result.putString(KEY_TOKEN, token);
            //.log(TAG, "Handshake SUCCESS for " + clientPackageName + ". Returning port " + sWebServerPort + " and token.");
            return result;

        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            //.log(TAG, "FATAL ERROR during handshake: " + t.getMessage() + "\n" + sw);
            sWebServerPort = 0;
            return null;
        }
    }

    @Override
    public boolean launchApp(String packageName, int userId) {
        //.log(TAG, "--- New Launch App Request ---");
        if (!isCallerLegitimate(null)) {
            //.log(TAG, "Launch App FAILED: Caller validation failed for calling UID " + Binder.getCallingUid());
            return false;
        }
        //.log(TAG, "Launch App authorized for UID " + Binder.getCallingUid() + ", proceeding to launch: " + packageName);
        return BlackBoxCore.get().launchApk(packageName, userId);
    }

    private boolean isCallerLegitimate(String claimedPackageName) {
        final int callingUid = Binder.getCallingUid();
        //.log(TAG, "Validating caller. UID: " + callingUid + ", Claimed Package: " + claimedPackageName);
        if (mContext == null) {
            //.log(TAG, "Validation FAILED: mContext is null!");
            return false;
        }
        PackageManager pm = mContext.getPackageManager();
        String[] packages = pm.getPackagesForUid(callingUid);

        if (packages == null || packages.length == 0) {
            //.log(TAG, "Validation FAILED: UID " + callingUid + " has no associated packages.");
            return false;
        }
        //.log(TAG, "UID " + callingUid + " maps to packages: " + Arrays.toString(packages));

        for (String pkg : packages) {
            boolean packageMatch = (claimedPackageName == null) || pkg.equals(claimedPackageName);
            boolean whitelistMatch = BUILT_IN_WHITELIST.contains(pkg) || mDynamicWhitelist.contains(pkg);
            if (packageMatch && whitelistMatch) {
                //.log(TAG, "Validation PASSED for package: " + pkg);
                return true;
            }
        }
        //.log(TAG, "Validation FAILED: No valid and whitelisted package found for UID " + callingUid);
        return false;
    }

    private int findAvailablePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            return -1;
        }
    }

    // --- 更新后的白名单处理逻辑 ---

    /**
     * 从网络更新白名单。
     * @param blocking 是否阻塞等待结果。
     * @param resultHolder 用于接收结果的原子引用 (仅在阻塞模式下使用)。
     */
    private void updateWhitelistFromNetwork(boolean blocking, AtomicReference<Boolean> resultHolder) {
        Runnable task = () -> {
            boolean success = false;
            try {
                //.log(TAG, "Starting whitelist update (blocking=" + blocking + ")...");
                String newJson = fetchWhitelist(PRIMARY_WHITELIST_URL);
                if (newJson == null) {
                    //.log(TAG, "Primary URL failed, trying fallback...");
                    newJson = fetchWhitelist(FALLBACK_WHITELIST_URL);
                }

                if (newJson == null) {
                    //.log(TAG, "Both URLs failed. No network update.");
                    return; // 最终结果为 false
                }

                String oldJson = readCacheFile();
                if (newJson.equals(oldJson)) {
                    //.log(TAG, "Whitelist is up-to-date. No changes needed.");
                    success = true;
                    return;
                }

                Set<String> newWhitelist = parseWhitelistJson(newJson);
                writeCacheFile(newJson);

                synchronized (mWhitelistLock) {
                    mDynamicWhitelist = Collections.synchronizedSet(newWhitelist);
                }
                //.log(TAG, "Whitelist updated successfully from network. New size: " + newWhitelist.size());
                success = true;

            } catch (Exception e) {
                //.log(TAG, "Error processing new whitelist.", e);
            } finally {
                if (blocking && resultHolder != null) {
                    resultHolder.set(success);
                    synchronized (resultHolder) {
                        resultHolder.notifyAll(); // 唤醒等待的线程
                    }
                }
            }
        };

        if (blocking) {
            // 提交任务并阻塞等待
            mExecutor.execute(task);
            try {
                synchronized (resultHolder) {
                    resultHolder.wait(WHITELIST_FETCH_TIMEOUT_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                //.log(TAG, "Whitelist fetch was interrupted.", e);
            }
        } else {
            // 异步执行
            mExecutor.execute(task);
        }
    }

    private String fetchWhitelist(String urlString) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            } else {
                //.log(TAG, "Fetch failed from " + urlString + " with code: " + connection.getResponseCode());
            }
        } catch (IOException e) {
            //.log(TAG, "IOException while fetching from " + urlString, e);
        } finally {
            if (reader != null) try { reader.close(); } catch (IOException ignored) {}
            if (connection != null) connection.disconnect();
        }
        return null;
    }

    private void loadWhitelistFromCache() {
        try {
            String json = readCacheFile();
            if (json != null) {
                Set<String> cachedWhitelist = parseWhitelistJson(json);
                synchronized (mWhitelistLock) {
                    mDynamicWhitelist = Collections.synchronizedSet(cachedWhitelist);
                }
                //.log(TAG, "Loaded " + cachedWhitelist.size() + " packages from cache.");
            } else {
                //.log(TAG, "No whitelist cache found.");
            }
        } catch (Exception e) {
            //.log(TAG, "Error loading whitelist from cache.", e);
        }
    }

    private Set<String> parseWhitelistJson(String json) throws JSONException {
        Set<String> set = new HashSet<>();
        JSONArray jsonArray = new JSONArray(json);
        for (int i = 0; i < jsonArray.length(); i++) {
            set.add(jsonArray.getString(i));
        }
        return set;
    }

    private File getCacheFile() {
        return new File(mContext.getFilesDir(), WHITELIST_FILENAME);
    }

    private String readCacheFile() throws IOException {
        File file = getCacheFile();
        if (!file.exists()) {
            return null;
        }
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private void writeCacheFile(String json) throws IOException {
        File file = getCacheFile();
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(json);
        }
    }
    
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}