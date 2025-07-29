package com.huadows.fastappdebug;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.huadows.fastapp.client.ApiCallback;
import com.huadows.fastapp.client.FastAppClient;
import com.huadows.fastapp.client.bean.ApiResponse;
import com.huadows.fastapp.client.bean.AppInfoBean;
import com.huadows.fastapp.client.bean.InstallResultBean;
import com.huadows.fastapp.client.bean.PackageInfoBean;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "FastAppDebugClient";
    private static final int USER_ID = 0;

    private EditText mPackageNameEditText;
    private EditText mDpiEditText;
    private TextView mLogTextView;
    private ImageView mIconImageView;

    private FastAppClient mFastAppClient; // 使用FastAppClient实例
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());

    private ActivityResultLauncher<String[]> mPermissionLauncher;
    private ActivityResultLauncher<Intent> mApkPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();
        setupActivityResultLaunchers();

        // 初始化客户端库
        // 假设服务端包名为 com.huadows.fastapp
        // AIDL服务 action 为 com.huadows.fastapp.server.ExportedAuthService
        // 引导Activity action 为 com.huadows.fastapp.server.ProcessBootstrapActivity
        // 服务端HTTP接口基础URL为 http://localhost
        mFastAppClient = new FastAppClient.Builder(this)
                .setServerAppPackage("com.huadows.fastapp")
                .setAuthServiceAction("com.huadows.fastapp.server.ExportedAuthService")
                .setBootstrapActivityAction("com.huadows.fastapp.server.ProcessBootstrapActivity")
                .setBaseUrl("http://localhost") // BaseUrl 不再需要端口
                .build();

        // 不再在onCreate中自动开始连接，连接会在第一次API调用时自动触发
        // 但为了初始UI显示和测试目的，仍然可以提供一个手动连接的入口
        // 或者在某个初始化操作后调用一次 mFastAppClient.connect(...)
        // 这里为了与您之前的startTestFlow一致，保留入口。
        startTestFlow();
    }

    private void initViews() {
        mPackageNameEditText = findViewById(R.id.et_package_name);
        mDpiEditText = findViewById(R.id.et_dpi);
        mLogTextView = findViewById(R.id.tv_log);
        mIconImageView = findViewById(R.id.iv_icon);
    }

    private void setupClickListeners() {
        findViewById(R.id.btn_handshake).setOnClickListener(this); // 这个按钮现在会触发连接
        findViewById(R.id.btn_get_list).setOnClickListener(this);
        findViewById(R.id.btn_launch_app).setOnClickListener(this);
        findViewById(R.id.btn_get_info).setOnClickListener(this);
        findViewById(R.id.btn_get_icon).setOnClickListener(this);
        findViewById(R.id.btn_uninstall).setOnClickListener(this);
        findViewById(R.id.btn_force_stop).setOnClickListener(this);
        findViewById(R.id.btn_clear_data).setOnClickListener(this);
        findViewById(R.id.btn_clear_cache).setOnClickListener(this);
        findViewById(R.id.btn_download_apk).setOnClickListener(this);
        findViewById(R.id.btn_set_dpi).setOnClickListener(this);
        findViewById(R.id.btn_install_apk).setOnClickListener(this);
    }

    private void setupActivityResultLaunchers() {
        mPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            if (Boolean.TRUE.equals(result.get(Manifest.permission.READ_EXTERNAL_STORAGE))) {
                Toast.makeText(this, "存储权限已获取", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "需要存储权限才能执行此操作", Toast.LENGTH_SHORT).show();
            }
        });

        mApkPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri apkUri = result.getData().getData();
                        if (apkUri != null) {
                            performInstallApk(apkUri);
                        }
                    }
                }
        );
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.btn_handshake) {
            // 手动触发连接，通常首次API调用会自动触发，但为了UI展示可以保留
            startTestFlow();
            return;
        }

        // 所有API调用现在都依赖FastAppClient内部的连接管理，无需在此处手动检查Token
        if (id == R.id.btn_get_list) {
            performGetAppList();
        } else if (id == R.id.btn_launch_app) {
            performLaunchApp();
        } else if (id == R.id.btn_get_info) {
            performGetPackageInfo();
        } else if (id == R.id.btn_get_icon) {
            performGetAppIcon();
        } else if (id == R.id.btn_uninstall) {
            performUninstall();
        } else if (id == R.id.btn_force_stop) {
            performForceStop();
        } else if (id == R.id.btn_clear_data) {
            performClearData();
        } else if (id == R.id.btn_clear_cache) {
            performClearCache();
        } else if (id == R.id.btn_download_apk) {
            performDownloadApk();
        } else if (id == R.id.btn_set_dpi) {
            performSetDpi();
        } else if (id == R.id.btn_install_apk) {
            triggerApkInstall();
        }
    }

    private void startTestFlow() {
        mLogTextView.setText("");
        logToScreen("▶️ 尝试连接服务端 (通过手动调用connect)...\n如果已连接，将直接通知成功。如果未连接，将尝试连接并握手。");
        // 明确调用connect，而不是依赖API调用触发，以在UI上展示初始连接状态
        mFastAppClient.connect(new FastAppClient.ConnectionCallback() {
            @Override
            public void onSuccess() {
                logToScreen("✅ 连接并握手成功！");
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                logToScreen("❌ 连接或握手失败！");
                logException(e);
            }
        });
    }

    private boolean isPackageNameEmpty(String packageName) {
        if (packageName.isEmpty()) {
            Toast.makeText(this, "请输入要操作的包名", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private void performGetAppList() {
        logToScreen("▶️ 正在获取应用列表...");
        mFastAppClient.getAppList(USER_ID, new ApiCallback<List<AppInfoBean>>() {
            @Override
            public void onSuccess(List<AppInfoBean> result) {
                logToScreen("✅ 解析成功，共获取到 " + result.size() + " 个应用。");
                for (AppInfoBean app : result) {
                    logToScreen("   - " + app.name + " (" + app.packageName + ")");
                }
            }
            @Override
            public void onFailure(@NonNull Exception e) {
                logApiCallError("获取列表", e);
            }
        });
    }

    private void performLaunchApp() {
        String packageName = mPackageNameEditText.getText().toString();
        if (isPackageNameEmpty(packageName)) return;

        // vvvvvvvvvvvv 关键修改 vvvvvvvvvvvv
        logToScreen("▶️ 正在尝试启动 (通过Binder): " + packageName);
        mFastAppClient.launchApp(packageName, USER_ID, new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (result) {
                    logToScreen("✅ 启动请求成功！");
                } else {
                    logToScreen("❌ 启动请求失败 (服务端返回 false)。");
                }
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                logApiCallError("启动应用", e);
            }
        });
        // ^^^^^^^^^^^^ 修改结束 ^^^^^^^^^^^^
    }

    private void performGetPackageInfo() {
        String packageName = mPackageNameEditText.getText().toString();
        if (isPackageNameEmpty(packageName)) return;
        logToScreen("▶️ 正在获取 " + packageName + " 的信息...");
        mFastAppClient.getPackageInfo(packageName, USER_ID, new ApiCallback<PackageInfoBean>() {
            @Override
            public void onSuccess(PackageInfoBean infoBean) {
                if (infoBean != null) {
                    logToScreen("✅ 获取信息成功！");
                    logToScreen("   - 应用名: " + infoBean.applicationLabel);
                    logToScreen("   - 包名: " + infoBean.packageName);
                    logToScreen("   - 版本名: " + infoBean.versionName);
                    logToScreen("   - 版本号: " + infoBean.versionCode);
                    logToScreen("   - 总大小: " + Formatter.formatFileSize(MainActivity.this, infoBean.totalSizeBytes));
                    logToScreen("   - 应用大小: " + Formatter.formatFileSize(MainActivity.this, infoBean.appSize));
                    logToScreen("   - 数据大小: " + Formatter.formatFileSize(MainActivity.this, infoBean.dataSize));
                    logToScreen("   - 缓存大小: " + Formatter.formatFileSize(MainActivity.this, infoBean.cacheSize));
                } else {
                    logToScreen("❌ 获取信息失败：服务端未找到该应用。");
                }
            }
            @Override
            public void onFailure(@NonNull Exception e) {
                logApiCallError("获取信息", e);
            }
        });
    }

    private void performGetAppIcon() {
        String packageName = mPackageNameEditText.getText().toString();
        if (isPackageNameEmpty(packageName)) return;
        logToScreen("▶️ 正在获取 " + packageName + " 的图标...");
        mFastAppClient.getAppIcon(packageName, USER_ID, new ApiCallback<Bitmap>() {
            @Override
            public void onSuccess(Bitmap result) {
                mIconImageView.setImageBitmap(result);
                logToScreen("✅ 图标加载并显示成功！");
            }
            @Override
            public void onFailure(@NonNull Exception e) {
                logApiCallError("获取图标", e);
            }
        });
    }

    private void performUninstall() {
        String packageName = mPackageNameEditText.getText().toString();
        if (isPackageNameEmpty(packageName)) return;
        logToScreen("▶️ 正在卸载: " + packageName);
        mFastAppClient.uninstall(packageName, USER_ID, new ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse result) {
                logToScreen("✅ 卸载请求成功: " + result.message);
            }
            @Override
            public void onFailure(@NonNull Exception e) {
                logApiCallError("卸载应用", e);
            }
        });
    }

    private void performForceStop() {
        String packageName = mPackageNameEditText.getText().toString();
        if (isPackageNameEmpty(packageName)) return;
        logToScreen("▶️ 正在强行停止: " + packageName);
        mFastAppClient.forceStop(packageName, USER_ID, new ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse result) {
                logToScreen("✅ 强停请求成功: " + result.message);
            }
            @Override
            public void onFailure(@NonNull Exception e) {
                logApiCallError("强行停止", e);
            }
        });
    }

    private void performClearData() {
        String packageName = mPackageNameEditText.getText().toString();
        if (isPackageNameEmpty(packageName)) return;
        logToScreen("▶️ 正在清除数据: " + packageName);
        mFastAppClient.clearData(packageName, USER_ID, new ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse result) {
                logToScreen("✅ 清除数据请求成功: " + result.message);
            }
            @Override
            public void onFailure(@NonNull Exception e) {
                logApiCallError("清除数据", e);
            }
        });
    }

    private void performClearCache() {
        String packageName = mPackageNameEditText.getText().toString();
        if (isPackageNameEmpty(packageName)) return;
        logToScreen("▶️ 正在清除缓存: " + packageName);
        mFastAppClient.clearCache(packageName, USER_ID, new ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse result) {
                logToScreen("✅ 清除缓存请求成功: " + result.message);
            }
            @Override
            public void onFailure(@NonNull Exception e) {
                logApiCallError("清除缓存", e);
            }
        });
    }

    private void performDownloadApk() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            mPermissionLauncher.launch(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE});
            return;
        }
        String packageName = mPackageNameEditText.getText().toString();
        if (isPackageNameEmpty(packageName)) return;

        logToScreen("▶️ 正在下载 " + packageName + " 的 APK...");
        mFastAppClient.downloadApk(packageName, USER_ID, new ApiCallback<Uri>() { // 注意这里是 downloadApk
            @Override
            public void onSuccess(Uri result) {
                logToScreen("✅ APK下载成功！已保存到: " + result.toString());
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                logApiCallError("下载APK", e);
            }
        });
    }

    private void performSetDpi() {
        String packageName = mPackageNameEditText.getText().toString();
        String dpiStr = mDpiEditText.getText().toString();
        if (isPackageNameEmpty(packageName) || TextUtils.isEmpty(dpiStr)) {
            Toast.makeText(this, "请输入包名和DPI值", Toast.LENGTH_SHORT).show();
            return;
        }
        int dpi = Integer.parseInt(dpiStr);
        logToScreen("▶️ 正在设置DPI: " + dpi);
        mFastAppClient.setDpi(packageName, USER_ID, dpi, new ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse result) {
                logToScreen("✅ 设置DPI请求成功: " + result.message);
            }
            @Override
            public void onFailure(@NonNull Exception e) {
                logApiCallError("设置DPI", e);
            }
        });
    }

    private void triggerApkInstall() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            mPermissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/vnd.android.package-archive");
            mApkPickerLauncher.launch(intent);
        }
    }

    private void performInstallApk(Uri apkUri) {
        logToScreen("▶️ 正在上传并安装APK...");
        mFastAppClient.installApk(apkUri, USER_ID, new ApiCallback<InstallResultBean>() {
            @Override
            public void onSuccess(InstallResultBean result) {
                if ("success".equals(result.status)) {
                    logToScreen("✅ 安装成功！包名: " + result.packageName);
                } else {
                    logToScreen("❌ 安装失败: " + result.message);
                }
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                logApiCallError("安装APK", e);
            }
        });
    }

    // --- UI & Logging Helpers ---
    private void logApiCallError(String taskName, Exception e) {
        mUiHandler.post(() -> { // 确保在UI线程更新LogTextView
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = "未知错误";
            }
            if (errorMsg.contains(FastAppClient.ERROR_APP_NOT_INSTALLED)) {
                logToScreen("❌ " + taskName + " - " + FastAppClient.ERROR_APP_NOT_INSTALLED);
            } else if (errorMsg.contains(FastAppClient.ERROR_CONNECTION_FAILED)) {
                logToScreen("❌ " + taskName + " - " + FastAppClient.ERROR_CONNECTION_FAILED + " " + errorMsg);
            } else {
                logToScreen("❌ " + taskName + " - 调用时发生异常。");
                logException(e); // 打印完整堆栈信息
            }
        });
    }

    private void logToScreen(String message) {
        mUiHandler.post(() -> mLogTextView.append(message + "\n\n"));
    }

    private void logException(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        logToScreen("   - 异常详情:\n" + sw.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 在Activity销毁时断开客户端连接
        if (mFastAppClient != null) {
            mFastAppClient.disconnect();
        }
    }
}