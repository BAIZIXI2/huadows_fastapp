package com.huadows.fastapp.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import com.huadows.fastapp.auth.IAuthService;
import com.huadows.fastapp.client.bean.ApiResponse;
import com.huadows.fastapp.client.bean.AppInfoBean;
import com.huadows.fastapp.client.bean.IconInfoBean;
import com.huadows.fastapp.client.bean.InstallResultBean;
import com.huadows.fastapp.client.bean.PackageInfoBean;


import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class FastAppClient {

    private static final String TAG = "FastAppClient";
    private final String serverAppPackage;
    private final String authServiceAction;
    private final String bootstrapActivityAction;
    private String baseUrl = "http://localhost"; // 基础URL，端口稍后填充

    private final Context mContext;
    private IAuthService mAuthService;
    private volatile String mToken;
    private volatile int mServerPort = 0; // 用于存储动态获取的端口
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Gson mGson = new Gson();
    private final OkHttpClient mOkHttpClient;

    // 连接状态标志
    private volatile boolean isConnected = false;
    private volatile boolean isConnecting = false;
    private volatile boolean isAppInstalled = true; // 假设应用默认是安装的，只有在尝试启动或绑定失败时才更新

    private static final int MAX_CONNECT_ATTEMPTS = 5; // 最大连接重试次数
    private static final long CONNECT_RETRY_DELAY_MS = 2000; // 连接重试间隔

    public static final String ERROR_APP_NOT_INSTALLED = "FastApp server application is not installed.";
    public static final String ERROR_CONNECTION_FAILED = "Failed to connect to FastApp server after multiple attempts.";

    // AIDL Bundle Keys
    public static final String KEY_PORT = "port";
    public static final String KEY_TOKEN = "token";


    public interface ConnectionCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static class Builder {
        private final Context context;
        private String serverAppPackage = "com.huadows.fastapp";
        private String authServiceAction = "com.huadows.fastapp.server.ExportedAuthService";
        private String bootstrapActivityAction = "com.huadows.fastapp.server.ProcessBootstrapActivity";
        private String baseUrl = "http://localhost";

        public Builder(Context context) {
            this.context = context.getApplicationContext();
        }

        public Builder setServerAppPackage(String serverAppPackage) {
            this.serverAppPackage = serverAppPackage;
            return this;
        }

        public Builder setAuthServiceAction(String authServiceAction) {
            this.authServiceAction = authServiceAction;
            return this;
        }

        public Builder setBootstrapActivityAction(String bootstrapActivityAction) {
            this.bootstrapActivityAction = bootstrapActivityAction;
            return this;
        }

        public Builder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public FastAppClient build() {
            return new FastAppClient(this);
        }
    }

    private FastAppClient(Builder builder) {
        this.mContext = builder.context;
        this.serverAppPackage = builder.serverAppPackage;
        this.authServiceAction = builder.authServiceAction;
        this.bootstrapActivityAction = builder.bootstrapActivityAction;
        this.baseUrl = builder.baseUrl; // 保存不带端口的基地址
        this.mOkHttpClient = createHttpClient();
    }

    private OkHttpClient createHttpClient() {
        Interceptor authInterceptor = chain -> {
            Request originalRequest = chain.request();
            Request.Builder newRequest = originalRequest.newBuilder();

            if (!TextUtils.isEmpty(mToken)) {
                newRequest.addHeader("Authorization", "Bearer " + mToken);
            }

            Response response = chain.proceed(newRequest.build());

            if (response.code() == 401) {
                Log.w(TAG, "Received 401 Unauthorized. Token might be invalid. Clearing token and attempting to reset connection state.");
                mToken = null;
                isConnected = false;
            }
            return response;
        };
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .build();
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        private final AtomicBoolean handledServiceConnection = new AtomicBoolean(false);

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (handledServiceConnection.getAndSet(true)) {
                return;
            }
            mAuthService = IAuthService.Stub.asInterface(service);
            mExecutor.execute(() -> {
                try {
                    Bundle result = mAuthService.performHandshake(mContext.getPackageName());
                    if (result != null) {
                        mServerPort = result.getInt(KEY_PORT, 0);
                        mToken = result.getString(KEY_TOKEN);

                        if (mServerPort > 0 && !TextUtils.isEmpty(mToken)) {
                            isConnected = true;
                            isConnecting = false;
                            Log.i(TAG, "AIDL Handshake successful. Port: " + mServerPort + ", Token obtained.");
                            notifyConnectionCallbacksSuccess();
                        } else {
                            throw new Exception("Handshake failed: Invalid port or token received.");
                        }
                    } else {
                        throw new Exception("Handshake failed: server returned null bundle.");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception during AIDL handshake: " + e.getMessage());
                    isConnecting = false;
                    notifyConnectionCallbacksFailure(e);
                    try {
                        mContext.unbindService(this);
                    } catch (IllegalArgumentException ex) {
                        Log.w(TAG, "Service was already unbound after handshake exception.", ex);
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "AIDL service disconnected unexpectedly.");
            mAuthService = null;
            mToken = null;
            isConnected = false;
            isConnecting = false;
            mServerPort = 0;
            handledServiceConnection.set(false);
            notifyConnectionCallbacksFailure(new Exception("Service disconnected unexpectedly."));
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.e(TAG, "AIDL service binding died. This usually means the server process was killed.");
            mAuthService = null;
            mToken = null;
            isConnected = false;
            isConnecting = false;
            mServerPort = 0;
            handledServiceConnection.set(false);
            notifyConnectionCallbacksFailure(new Exception("Service binding died unexpectedly (server process was killed?)."));
        }
    };

    private final List<ConnectionCallback> connectionCallbacks = new java.util.ArrayList<>();
    private final List<Runnable> pendingApiActions = new java.util.ArrayList<>();

    private void notifyConnectionCallbacksSuccess() {
        synchronized (connectionCallbacks) {
            for (ConnectionCallback cb : connectionCallbacks) {
                mUiHandler.post(cb::onSuccess);
            }
            connectionCallbacks.clear();
        }
        synchronized (pendingApiActions) {
            for (Runnable action : pendingApiActions) {
                action.run();
            }
            pendingApiActions.clear();
        }
    }

    private void notifyConnectionCallbacksFailure(Exception e) {
        synchronized (connectionCallbacks) {
            for (ConnectionCallback cb : connectionCallbacks) {
                mUiHandler.post(() -> cb.onFailure(e));
            }
            connectionCallbacks.clear();
        }
        synchronized (pendingApiActions) {
            pendingApiActions.clear();
        }
    }

    public void connect(ConnectionCallback callback) {
        synchronized (connectionCallbacks) {
            connectionCallbacks.add(callback);

            if (isConnected) {
                mUiHandler.post(() -> {
                    callback.onSuccess();
                    synchronized (connectionCallbacks) {
                        connectionCallbacks.remove(callback);
                    }
                });
                return;
            }

            if (isConnecting) {
                Log.d(TAG, "Already connecting. Adding callback to queue.");
                return;
            }
            isConnecting = true;

            mExecutor.execute(() -> startConnectionProcess(1));
        }
    }

    private void startConnectionProcess(final int currentAttempt) {
        if (!isAppInstalled) {
            Log.e(TAG, "FastApp server is not installed. Aborting connection attempt.");
            isConnecting = false;
            notifyConnectionCallbacksFailure(new Exception(ERROR_APP_NOT_INSTALLED));
            return;
        }

        if (currentAttempt > MAX_CONNECT_ATTEMPTS) {
            Log.e(TAG, "Failed to connect to FastApp server after " + MAX_CONNECT_ATTEMPTS + " attempts.");
            isConnecting = false;
            notifyConnectionCallbacksFailure(new Exception(ERROR_CONNECTION_FAILED));
            return;
        }

        Log.d(TAG, "Starting connection attempt " + currentAttempt + "...");

        try {
            Log.d(TAG, "Attempting to start bootstrap activity for " + serverAppPackage);
            Intent bootstrapIntent = new Intent();
            bootstrapIntent.setComponent(new ComponentName(serverAppPackage, bootstrapActivityAction));
            bootstrapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(bootstrapIntent);

            Thread.sleep(1000);

            Log.d(TAG, "Attempting to bind to auth service.");
            Intent bindServiceIntent = new Intent();
            bindServiceIntent.setComponent(new ComponentName(serverAppPackage, authServiceAction));
            boolean success = mContext.bindService(bindServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
            if (!success) {
                throw new IOException("Failed to bind to the auth service. Service not found or permission denied.");
            }
        } catch (android.content.ActivityNotFoundException e) {
            Log.e(TAG, ERROR_APP_NOT_INSTALLED + " (Activity not found): " + e.getMessage());
            isAppInstalled = false;
            isConnecting = false;
            notifyConnectionCallbacksFailure(new Exception(ERROR_APP_NOT_INSTALLED));
        } catch (SecurityException e) {
            Log.e(TAG, ERROR_APP_NOT_INSTALLED + " (Security exception): " + e.getMessage());
            isAppInstalled = false;
            isConnecting = false;
            notifyConnectionCallbacksFailure(new Exception(ERROR_APP_NOT_INSTALLED));
        } catch (Exception e) {
            Log.e(TAG, "Error during connection attempt " + currentAttempt + ": " + e.getMessage());
            isConnecting = false;
            mUiHandler.postDelayed(() -> mExecutor.execute(() -> startConnectionProcess(currentAttempt + 1)), CONNECT_RETRY_DELAY_MS);
        }
    }


    public void disconnect() {
        mExecutor.execute(() -> {
            synchronized (mServiceConnection) {
                if (isConnected || isConnecting) {
                    try {
                        mContext.unbindService(mServiceConnection);
                        Log.i(TAG, "Successfully unbound AIDL service.");
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "Service was not bound or already unbound when trying to disconnect.", e);
                    } finally {
                        mAuthService = null;
                        mToken = null;
                        mServerPort = 0;
                        isConnected = false;
                        isConnecting = false;
                        isAppInstalled = true;
                        synchronized (connectionCallbacks) {
                            connectionCallbacks.clear();
                        }
                        synchronized (pendingApiActions) {
                            pendingApiActions.clear();
                        }
                    }
                } else {
                    Log.i(TAG, "Client is already disconnected or not connected.");
                }
            }
        });
    }

    private <T> void ensureConnectedAndExecute(final Runnable apiCallAction, final ApiCallback<T> originalCallback, final int currentAttempt) {
        if (!isAppInstalled) {
            mUiHandler.post(() -> originalCallback.onFailure(new Exception(ERROR_APP_NOT_INSTALLED)));
            return;
        }

        if (isConnected && !TextUtils.isEmpty(mToken) && mServerPort > 0) {
            Log.d(TAG, "Already connected, executing API action directly.");
            apiCallAction.run();
            return;
        }

        if (isConnecting) {
            Log.d(TAG, "Connection is in progress, queuing API action.");
            synchronized (pendingApiActions) {
                pendingApiActions.add(apiCallAction);
            }
            return;
        }

        if (currentAttempt > MAX_CONNECT_ATTEMPTS) {
            Log.e(TAG, ERROR_CONNECTION_FAILED + " Current attempt: " + currentAttempt);
            isConnecting = false;
            mUiHandler.post(() -> originalCallback.onFailure(new Exception(ERROR_CONNECTION_FAILED)));
            return;
        }

        isConnecting = true;
        Log.i(TAG, "Connection required for API call. Attempting to connect (API triggered): " + currentAttempt);
        
        synchronized (pendingApiActions) {
             pendingApiActions.add(apiCallAction);
        }
        
        final ConnectionCallback connectCallbackForApi = new ConnectionCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Connection successful after API trigger. Pending actions will be executed.");
                isConnected = true;
                isConnecting = false;
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Connection failed for API call. Attempt: " + currentAttempt + ", Error: " + e.getMessage());
                isConnecting = false;

                if (e.getMessage() != null && e.getMessage().contains(ERROR_APP_NOT_INSTALLED)) {
                    mUiHandler.post(() -> originalCallback.onFailure(new Exception(ERROR_APP_NOT_INSTALLED)));
                    synchronized (pendingApiActions) {
                        pendingApiActions.clear();
                    }
                } else if (currentAttempt < MAX_CONNECT_ATTEMPTS) {
                    Log.d(TAG, "Retrying connection in " + CONNECT_RETRY_DELAY_MS + "ms for API call...");
                    mUiHandler.postDelayed(() -> mExecutor.execute(() -> ensureConnectedAndExecute(() -> {}, originalCallback, currentAttempt + 1)), CONNECT_RETRY_DELAY_MS);
                } else {
                    mUiHandler.post(() -> originalCallback.onFailure(new Exception(ERROR_CONNECTION_FAILED)));
                    synchronized (pendingApiActions) {
                        pendingApiActions.clear();
                    }
                }
            }
        };

        synchronized (connectionCallbacks) {
            connectionCallbacks.add(connectCallbackForApi);
        }
        mExecutor.execute(() -> startConnectionProcess(1));
    }

    // --- API Methods ---
    
    private <T> void makeApiCall(ApiCallback<T> callback, Runnable apiCallRunnable) {
        ensureConnectedAndExecute(apiCallRunnable, callback, 1);
    }

    private String getDynamicBaseUrl() {
        if (mServerPort <= 0) {
            throw new IllegalStateException("Server port is not initialized. Cannot make API calls.");
        }
        return baseUrl + ":" + mServerPort;
    }

    public void getAppList(int userId, ApiCallback<List<AppInfoBean>> callback) {
        class GetAppListAction implements Runnable {
            @Override
            public void run() {
                String url = getDynamicBaseUrl() + "/app/list?userId=" + userId;
                Request request = new Request.Builder().url(url).build();
                Type responseType = new TypeToken<List<AppInfoBean>>(){}.getType();
                mOkHttpClient.newCall(request).enqueue(createRetryableCallback(this, callback, responseType, "getAppList"));
            }
        }
        makeApiCall(callback, new GetAppListAction());
    }

    public void installApk(Uri apkUri, int userId, ApiCallback<InstallResultBean> callback) {
        // vvvvvvvvvvvv 修复：使用局部内部类并手动管理流生命周期 vvvvvvvvvvvv
        class InstallApkAction implements Runnable {
            @Override
            public void run() {
                InputStream inputStream = null; // 在 try-catch 外部声明
                try {
                    inputStream = mContext.getContentResolver().openInputStream(apkUri);
                    if (inputStream == null) {
                        throw new IOException("Failed to open input stream from URI: " + apkUri);
                    }

                    String fileName = getFileNameFromUri(apkUri);
                    RequestBody fileBody = createStreamingRequestBody(inputStream);

                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("userId", String.valueOf(userId))
                            .addFormDataPart("apk", fileName, fileBody)
                            .build();

                    Request request = new Request.Builder()
                            .url(getDynamicBaseUrl() + "/app/install")
                            .post(requestBody)
                            .build();

                    // 声明为 final 以便在回调中访问
                    final InputStream finalInputStream = inputStream;

                    Callback retryableCallback = new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            closeStream(finalInputStream); // 确保在失败时关闭流
                            if (e instanceof SocketTimeoutException) {
                                Log.w(TAG, "installApk API call timed out. Resetting connection and retrying.");
                                resetConnectionStateForRetry();
                                makeApiCall(callback, InstallApkAction.this);
                            } else {
                                mUiHandler.post(() -> callback.onFailure(e));
                            }
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            try (ResponseBody body = response.body()){
                                 if (body == null) {
                                     mUiHandler.post(() -> callback.onFailure(new IOException("Empty response body.")));
                                     return;
                                 }
                                 String json = body.string();
                                 InstallResultBean result = mGson.fromJson(json, InstallResultBean.class);
                                 if (response.isSuccessful() && "success".equals(result.status)) {
                                     mUiHandler.post(() -> callback.onSuccess(result));
                                 } else {
                                     mUiHandler.post(() -> callback.onFailure(new IOException("Install failed: " + result.message)));
                                 }
                             } catch (Exception ex) {
                                 mUiHandler.post(() -> callback.onFailure(ex));
                             } finally {
                                closeStream(finalInputStream); // 确保在响应处理完毕后关闭流
                             }
                        }
                    };
                    mOkHttpClient.newCall(request).enqueue(retryableCallback);
                } catch (Exception e) {
                    closeStream(inputStream); // 如果在请求发送前就发生异常，也要关闭流
                    mUiHandler.post(() -> callback.onFailure(e));
                }
            }
        }
        makeApiCall(callback, new InstallApkAction());
        // ^^^^^^^^^^^^ 修复结束 ^^^^^^^^^^^^
    }

    public void uninstall(String packageName, int userId, ApiCallback<ApiResponse> callback) {
        makeSimplePostRequest("/app/uninstall", packageName, userId, callback);
    }
    
    public void forceStop(String packageName, int userId, ApiCallback<ApiResponse> callback) {
        makeSimplePostRequest("/app/forceStop", packageName, userId, callback);
    }

    public void clearData(String packageName, int userId, ApiCallback<ApiResponse> callback) {
        makeSimplePostRequest("/app/clearData", packageName, userId, callback);
    }

    public void clearCache(String packageName, int userId, ApiCallback<ApiResponse> callback) {
        makeSimplePostRequest("/app/clearCache", packageName, userId, callback);
    }
    
    public void launchApp(String packageName, int userId, ApiCallback<Boolean> callback) {
        ensureConnectedAndExecute(() -> {
            if (mAuthService == null) {
                mUiHandler.post(() -> callback.onFailure(new Exception("AuthService is not available.")));
                return;
            }
            try {
                boolean success = mAuthService.launchApp(packageName, userId);
                mUiHandler.post(() -> callback.onSuccess(success));
            } catch (Exception e) {
                mUiHandler.post(() -> callback.onFailure(e));
            }
        }, callback, 1);
    }

    public void setDpi(String packageName, int userId, int dpi, ApiCallback<ApiResponse> callback) {
        class SetDpiAction implements Runnable {
            @Override
            public void run() {
                String url = getDynamicBaseUrl() + "/app/setDpi";
                RequestBody formBody = new FormBody.Builder()
                        .add("packageName", packageName)
                        .add("userId", String.valueOf(userId))
                        .add("dpi", String.valueOf(dpi))
                        .build();
                Request request = new Request.Builder().url(url).post(formBody).build();
                
                mOkHttpClient.newCall(request).enqueue(createRetryableCallback(this, callback, ApiResponse.class, "setDpi"));
            }
        }
        makeApiCall(callback, new SetDpiAction());
    }

    public void getPackageInfo(String packageName, int userId, ApiCallback<PackageInfoBean> callback) {
        class GetPackageInfoAction implements Runnable {
            @Override
            public void run() {
                String url = getDynamicBaseUrl() + "/app/getPackageInfo?packageName=" + packageName + "&userId=" + userId;
                Request request = new Request.Builder().url(url).build();
                mOkHttpClient.newCall(request).enqueue(createRetryableCallback(this, callback, PackageInfoBean.class, "getPackageInfo"));
            }
        }
        makeApiCall(callback, new GetPackageInfoAction());
    }

    public void getAppIcon(String packageName, int userId, ApiCallback<Bitmap> callback) {
        class GetAppIconAction implements Runnable {
            @Override
            public void run() {
                String url = getDynamicBaseUrl() + "/app/getAppInfoWithIconLink?packageName=" + packageName + "&userId=" + userId;
                Request request = new Request.Builder().url(url).build();

                Callback retryableCallback = new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        if (e instanceof SocketTimeoutException) {
                            Log.w(TAG, "getAppIcon API call timed out. Resetting connection and retrying.");
                            resetConnectionStateForRetry();
                            makeApiCall(callback, GetAppIconAction.this);
                        } else {
                            mUiHandler.post(() -> callback.onFailure(e));
                        }
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        try (ResponseBody body = response.body()){
                            if (!response.isSuccessful()) {
                                String error = body != null ? body.string() : "Unknown error";
                                mUiHandler.post(() -> callback.onFailure(new IOException("Request failed with code " + response.code() + ": " + error)));
                                return;
                            }
                            String json = body.string();
                            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                            if (jsonObject.has("iconUrl") && !jsonObject.get("iconUrl").isJsonNull()) {
                                String iconUrl = jsonObject.get("iconUrl").getAsString();
                                fetchAndDecodeIcon(iconUrl, callback);
                            } else {
                                String error = jsonObject.has("message") ? jsonObject.get("message").getAsString() : "Icon URL not found in response.";
                                Log.e(TAG, "getAppIcon: " + error);
                                mUiHandler.post(() -> callback.onFailure(new IOException(error)));
                            }
                        } catch (Exception e) {
                             mUiHandler.post(() -> callback.onFailure(e));
                        }
                    }
                };
                mOkHttpClient.newCall(request).enqueue(retryableCallback);
            }
        }
        makeApiCall(callback, new GetAppIconAction());
    }

    private void fetchAndDecodeIcon(String iconUrl, ApiCallback<Bitmap> callback) {
        Request request = new Request.Builder().url(iconUrl).build();
        
        mOkHttpClient.newCall(request).enqueue(new Callback() {
             @Override
             public void onFailure(@NonNull Call call, @NonNull IOException e) {
                  mUiHandler.post(() -> callback.onFailure(e));
             }

             @Override
             public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                 try (ResponseBody body = response.body()) {
                     if (!response.isSuccessful()) {
                         mUiHandler.post(() -> callback.onFailure(new IOException("Failed to fetch icon data with code " + response.code())));
                         return;
                     }
                     String json = body.string();
                     IconInfoBean iconInfo = mGson.fromJson(json, IconInfoBean.class);
                     if (iconInfo != null && iconInfo.icon != null) {
                         byte[] decodedBytes = Base64.decode(iconInfo.icon, Base64.DEFAULT);
                         Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                         if (bitmap != null) {
                            mUiHandler.post(() -> callback.onSuccess(bitmap));
                         } else {
                            mUiHandler.post(() -> callback.onFailure(new IOException("Failed to decode base64 icon string.")));
                         }
                     } else {
                         mUiHandler.post(() -> callback.onFailure(new IOException(iconInfo != null && iconInfo.error != null ? iconInfo.error : "Icon data not found in response.")));
                     }
                 } catch (Exception e) {
                      mUiHandler.post(() -> callback.onFailure(e));
                 }
             }
         });
    }

    public void downloadApk(String packageName, int userId, ApiCallback<Uri> callback) {
        // vvvvvvvvvvvv 修复：使用 mExecutor 确保在后台线程执行 vvvvvvvvvvvv
        class DownloadApkAction implements Runnable {
            @Override
            public void run() {
                mExecutor.execute(() -> {
                    String url = getDynamicBaseUrl() + "/app/downloadApk?packageName=" + packageName + "&userId=" + userId;
                    Request request = new Request.Builder().url(url).build();
                    Response response = null;
                    try {
                        response = mOkHttpClient.newCall(request).execute();

                        if (response.isSuccessful() && response.body() != null) {
                            String fileName = packageName + "_from_fastapp.apk";

                            try (InputStream in = response.body().byteStream()) {
                                if (in == null) {
                                     mUiHandler.post(() -> callback.onFailure(new IOException("Response body stream is null.")));
                                     return;
                                }

                                android.content.ContentResolver resolver = mContext.getContentResolver();
                                android.content.ContentValues contentValues = new android.content.ContentValues();
                                contentValues.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                                contentValues.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive");

                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    contentValues.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);
                                }

                                Uri collectionUri = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                                Uri itemUri = resolver.insert(collectionUri, contentValues);

                                if (itemUri == null) {
                                    mUiHandler.post(() -> callback.onFailure(new IOException("Failed to create MediaStore entry.")));
                                    return;
                                }

                                try (android.os.ParcelFileDescriptor pfd = resolver.openFileDescriptor(itemUri, "w");
                                     java.io.FileOutputStream out = new java.io.FileOutputStream(pfd.getFileDescriptor())) {
                                    byte[] buffer = new byte[4096];
                                    int bytesRead;
                                    while ((bytesRead = in.read(buffer)) != -1) {
                                        out.write(buffer, 0, bytesRead);
                                    }
                                    mUiHandler.post(() -> callback.onSuccess(itemUri));
                                } catch (IOException e) {
                                    resolver.delete(itemUri, null, null);
                                    throw e;
                                }
                            }
                        } else {
                            String errorBody = response.body() != null ? response.body().string() : "Empty Body";
                            final Response finalResponse = response;
                            mUiHandler.post(() -> callback.onFailure(new IOException("Download failed with HTTP " + finalResponse.code() + ": " + errorBody)));
                        }
                    } catch (SocketTimeoutException e) {
                        Log.w(TAG, "downloadApk API call timed out. Resetting connection and retrying.");
                        resetConnectionStateForRetry();
                        makeApiCall(callback, DownloadApkAction.this);
                    } catch (Exception e) {
                        mUiHandler.post(() -> callback.onFailure(e));
                    } finally {
                        if (response != null) {
                            response.close();
                        }
                    }
                });
            }
        }
        makeApiCall(callback, new DownloadApkAction());
        // ^^^^^^^^^^^^ 修复结束 ^^^^^^^^^^^^
    }

    // --- Helper Methods ---
    
    private void resetConnectionStateForRetry() {
        mExecutor.execute(() -> {
            synchronized (mServiceConnection) {
                if (mAuthService != null) {
                    try {
                        mContext.unbindService(mServiceConnection);
                    } catch (Exception ignored) {
                    }
                }
                mAuthService = null;
                mToken = null;
                mServerPort = 0;
                isConnected = false;
                isConnecting = false;
            }
        });
    }

    private <T> Callback createRetryableCallback(final Runnable originalApiAction, final ApiCallback<T> callback, final Type responseType, final String apiName) {
        return new Callback() {
            private final AtomicBoolean hasRetried = new AtomicBoolean(false);
            
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (e instanceof SocketTimeoutException && !hasRetried.getAndSet(true)) {
                    Log.w(TAG, apiName + " API call timed out. Resetting connection and retrying once.");
                    resetConnectionStateForRetry();
                    makeApiCall(callback, originalApiAction);
                } else {
                    if(e instanceof SocketTimeoutException) {
                        Log.e(TAG, apiName + " API call timed out again after retry. Failing.");
                    }
                    mUiHandler.post(() -> callback.onFailure(e));
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                handleJsonResponse(response, responseType, callback);
            }
        };
    }
    
    private void makeSimplePostRequest(String path, String packageName, int userId, ApiCallback<ApiResponse> callback) {
        class SimplePostAction implements Runnable {
            @Override
            public void run() {
                String url = getDynamicBaseUrl() + path;
                RequestBody formBody = new FormBody.Builder()
                        .add("packageName", packageName)
                        .add("userId", String.valueOf(userId))
                        .build();
                Request request = new Request.Builder().url(url).post(formBody).build();
                
                mOkHttpClient.newCall(request).enqueue(createRetryableCallback(this, callback, ApiResponse.class, path));
            }
        }
        makeApiCall(callback, new SimplePostAction());
    }
    
    private <T> void handleJsonResponse(Response response, Type type, ApiCallback<T> callback) throws IOException {
        try (ResponseBody body = response.body()) {
            if (body == null) {
                mUiHandler.post(() -> callback.onFailure(new IOException("Empty response body")));
                return;
            }
            String json = body.string();

            if (type.equals(ApiResponse.class)) {
                JsonElement jsonElement = JsonParser.parseString(json);
                if (jsonElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = jsonElement.getAsJsonPrimitive();
                    if (primitive.isBoolean()) {
                        boolean boolValue = primitive.getAsBoolean();
                        ApiResponse apiResponse = new ApiResponse();
                        if (boolValue) {
                            apiResponse.status = "success";
                            apiResponse.message = "操作成功";
                        } else {
                            apiResponse.status = "error";
                            apiResponse.message = "操作失败：服务端返回布尔假值";
                        }
                        mUiHandler.post(() -> callback.onSuccess((T) apiResponse));
                        return;
                    }
                }
            }

            if (!response.isSuccessful()) {
                try {
                    ApiResponse apiResponse = mGson.fromJson(json, ApiResponse.class);
                    String errorMessage = apiResponse != null && !TextUtils.isEmpty(apiResponse.message) ?
                                          apiResponse.message : ("Request failed with code " + response.code() + ": " + json);
                    mUiHandler.post(() -> callback.onFailure(new IOException(errorMessage)));
                } catch (Exception e) {
                    mUiHandler.post(() -> callback.onFailure(new IOException("Request failed with code " + response.code() + ". Response body: " + json, e)));
                }
                return;
            }
            T result = mGson.fromJson(json, type);
            mUiHandler.post(() -> callback.onSuccess(result));
        } catch (Exception e) {
            mUiHandler.post(() -> callback.onFailure(e));
        }
    }

    private RequestBody createStreamingRequestBody(final InputStream inputStream) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse("application/vnd.android.package-archive");
            }

            @Override
            public long contentLength() {
                // 流式传输，长度未知
                try {
                    return inputStream.available();
                } catch (IOException e) {
                    return -1;
                }
            }

            @Override
            public void writeTo(@NonNull BufferedSink sink) throws IOException {
                try (Source source = Okio.source(inputStream)) {
                    sink.writeAll(source);
                }
            }
        };
    }
    
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch(Exception e) {
                Log.w(TAG, "Could not get file name from content URI", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result != null ? result : "unknown.apk";
    }

    private void closeStream(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing input stream: " + e.getMessage());
            }
        }
    }
}