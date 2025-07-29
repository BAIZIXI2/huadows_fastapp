package com.huadows.fastapp.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.huadows.fastapp.R;
import com.huadows.ui.CustomDialog;
import com.huadows.ui.CustomToast; // 引入自定义 Toast 类

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.BStorageInfo;

public class AppManageActivity extends AppCompatActivity {

    public static final String EXTRA_PACKAGE_NAME = "extra_package_name";
    private static final int USER_ID = 0;
    private static final int REQUEST_STORAGE_PERMISSION_CODE = 1001;

    private ImageView appIcon;
    private TextView appName;
    private TextView appVersion;
    private LinearLayout buttonUninstall;
    private LinearLayout buttonForceStop;
    private LinearLayout buttonClearData;
    private LinearLayout buttonClearCache;
    private LinearLayout buttonDownloadApk;
    private LinearLayout buttonModifyDpi;
    private TextView textTotalSize;
    private TextView textAppSize;
    private TextView textDataSize;
    private TextView textCacheSize;
    private ImageView backButton;

    private String mPackageName;
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_manage);

        mPackageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        if (mPackageName == null) {
            CustomToast.show(this, "无效的应用信息", 2000, CustomToast.Level.WARNING);
            finish();
            return;
        }

        initViews();
        loadAppInfo();
        loadStorageInfo();
        setupClickListeners();
    }

    private void initViews() {
        backButton = findViewById(R.id.back_button);
        appIcon = findViewById(R.id.app_icon);
        appName = findViewById(R.id.app_name);
        appVersion = findViewById(R.id.app_version);
        buttonUninstall = findViewById(R.id.button_uninstall);
        buttonForceStop = findViewById(R.id.button_force_stop);
        buttonClearData = findViewById(R.id.button_clear_data);
        buttonClearCache = findViewById(R.id.button_clear_cache);
        buttonDownloadApk = findViewById(R.id.button_download_apk);
        buttonModifyDpi = findViewById(R.id.button_modify_dpi);
        textTotalSize = findViewById(R.id.text_total_size);
        textAppSize = findViewById(R.id.text_app_size);
        textDataSize = findViewById(R.id.text_data_size);
        textCacheSize = findViewById(R.id.text_cache_size);

        setTouchAnimation(backButton);
        setTouchAnimation(buttonUninstall);
        setTouchAnimation(buttonForceStop);
        setTouchAnimation(buttonClearData);
        setTouchAnimation(buttonClearCache);
        setTouchAnimation(buttonDownloadApk);
        setTouchAnimation(buttonModifyDpi);
    }

    private void loadAppInfo() {
        mExecutor.execute(() -> {
            try {
                ApplicationInfo targetAppInfo = BlackBoxCore.get().getBPackageManager().getApplicationInfo(mPackageName, 0, USER_ID);
                if (targetAppInfo != null) {
                    PackageManager pm = getPackageManager();
                    Drawable icon = targetAppInfo.loadIcon(pm);
                    String name = targetAppInfo.loadLabel(pm).toString();

                    String versionName = BlackBoxCore.get().getPackageVersionName(mPackageName, USER_ID);
                    long versionCode = BlackBoxCore.get().getPackageVersionCode(mPackageName, USER_ID);

                    final String appVersionText = String.format("版本 %s (%d)", versionName, versionCode);

                    mUiHandler.post(() -> {
                        appIcon.setImageDrawable(icon);
                        appName.setText(name);
                        appVersion.setText(appVersionText);
                    });
                } else {
                    mUiHandler.post(() -> {
                        CustomToast.show(AppManageActivity.this, "无法加载应用信息: 应用未找到", 2000, CustomToast.Level.ERROR);
                        finish();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                mUiHandler.post(() -> {
                    CustomToast.show(AppManageActivity.this, "加载应用信息失败: " + e.getMessage(), 3500, CustomToast.Level.ERROR);
                    finish();
                });
            }
        });

        
    }

    private void loadStorageInfo() {
        mExecutor.execute(() -> {
            BStorageInfo storageInfo = BlackBoxCore.get().getStorageInfo(mPackageName, USER_ID);
            if (storageInfo != null) {
                mUiHandler.post(() -> {
                    textTotalSize.setText("总计：" + formatSize(storageInfo.totalSize));
                    textAppSize.setText("应用：" + formatSize(storageInfo.appSize));
                    textDataSize.setText("数据：" + formatSize(storageInfo.dataSize));
                    textCacheSize.setText("缓存：" + formatSize(storageInfo.cacheSize));
                });
            } else {
                mUiHandler.post(() -> {
                    textTotalSize.setText("总计：计算失败");
                    textAppSize.setText("应用：计算失败");
                    textDataSize.setText("数据：计算失败");
                    textCacheSize.setText("缓存：计算失败");
                });
            }
        });
    }

    private String formatSize(long size) {
        return Formatter.formatFileSize(this, size);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        buttonUninstall.setOnClickListener(v -> {
            new CustomDialog.Builder(this)
                    .setTitle("卸载应用")
                    .setMessage("你确定要卸载 " + appName.getText().toString() + " 吗？")
                    .setConfirmButton("确定")
                    .setCancelButton("取消")
                    .setListener(new CustomDialog.OnDialogActionClickListener() {
                        @Override
                        public void onConfirmClick() {
                            CustomToast.show(AppManageActivity.this, "正在卸载...", 2000, CustomToast.Level.INFO);
                            mExecutor.execute(() -> {
                                BlackBoxCore.get().uninstallPackageAsUser(mPackageName, USER_ID);
                                boolean isStillInstalled = BlackBoxCore.get().isInstalled(mPackageName, USER_ID);
                                mUiHandler.post(() -> {
                                    if (!isStillInstalled) {
                                        CustomToast.show(AppManageActivity.this, "卸载成功", 2000, CustomToast.Level.SUCCESS);
                                        setResult(RESULT_OK);
                                        finish();
                                    } else {
                                        CustomToast.show(AppManageActivity.this, "卸载失败", 2000, CustomToast.Level.ERROR);
                                    }
                                });
                            });
                        }

                        @Override
                        public void onCancelClick() {
                            // 用户点击取消，无需操作
                        }
                    })
                    .show();
        });

        buttonForceStop.setOnClickListener(v -> showForceStopDialog());
        buttonClearData.setOnClickListener(v -> showClearDataDialog());

        buttonClearCache.setOnClickListener(v -> {
            CustomToast.show(this, "正在清除缓存...", 2000, CustomToast.Level.INFO);
            mExecutor.execute(() -> {
                boolean success = BlackBoxCore.get().clearPackageCache(mPackageName, USER_ID);
                mUiHandler.post(() -> {
                    if (success) {
                        CustomToast.show(AppManageActivity.this, "缓存清除成功", 2000, CustomToast.Level.SUCCESS);
                        loadStorageInfo();
                    } else {
                        CustomToast.show(AppManageActivity.this, "缓存清除失败", 2000, CustomToast.Level.ERROR);
                    }
                });
            });
        });

        buttonDownloadApk.setOnClickListener(v -> checkAndDownloadApk());
        buttonModifyDpi.setOnClickListener(v -> showDpiDialog());
    }

    private void showDpiDialog() {
        // 1. 提前加载自定义布局
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_dpi, null);
        final EditText editTextDpi = view.findViewById(R.id.edit_text_dpi);
        final TextView textViewCurrentDpi = view.findViewById(R.id.text_current_dpi);

        // 2. 在后台线程获取当前DPI并更新UI
        mExecutor.execute(() -> {
            int currentDpi = BlackBoxCore.get().getVirtualDPI(mPackageName, USER_ID);
            mUiHandler.post(() -> {
                if (currentDpi == 0) {
                    textViewCurrentDpi.setText("当前 DPI: 默认 (" + getResources().getDisplayMetrics().densityDpi + ")");
                } else {
                    textViewCurrentDpi.setText("当前 DPI: " + currentDpi);
                }
            });
        });

        // 3. 使用CustomDialog并嵌入自定义视图
        new CustomDialog.Builder(this)
                .setTitle("修改DPI")
                .setView(view) // 嵌入视图
                .setConfirmButton("确认")
                .setCancelButton("取消")
                .setListener(new CustomDialog.OnDialogActionClickListener() {
                    @Override
                    public void onConfirmClick() {
                        String dpiStr = editTextDpi.getText().toString();
                        if (TextUtils.isEmpty(dpiStr)) {
                            // 如果输入为空，则视为恢复默认
                            setDpi(0);
                            return;
                        }
                        try {
                            int newDpi = Integer.parseInt(dpiStr);
                            setDpi(newDpi);
                        } catch (NumberFormatException e) {
                            CustomToast.show(AppManageActivity.this, "请输入有效的数字", 2000, CustomToast.Level.WARNING);
                        }
                    }

                    @Override
                    public void onCancelClick() {
                        // 用户点击取消，无需操作
                    }
                })
                .show();
    }

    private void setDpi(int newDpi) {
        CustomToast.show(this, "正在设置DPI为 " + (newDpi == 0 ? "默认" : newDpi), 2000, CustomToast.Level.INFO);
        mExecutor.execute(() -> {
            BlackBoxCore.get().setVirtualDPI(mPackageName, newDpi, USER_ID);
            mUiHandler.post(() -> {
                CustomToast.show(this, "设置成功，请强行停止并重启应用以生效", 3500, CustomToast.Level.SUCCESS);
            });
        });
    }

    private void checkAndDownloadApk() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION_CODE);
            } else {
                startDownload();
            }
        } else {
            startDownload();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDownload();
            } else {
                CustomToast.show(this, "需要存储权限才能下载文件", 2000, CustomToast.Level.WARNING);
            }
        }
    }

    private void startDownload() {
        // 使用CustomDialog显示进度
        final CustomDialog downloadDialog = new CustomDialog.Builder(this)
                .setTitle("下载APK")
                .setMessage("正在准备下载...")
                .setCancelButtonVisible(false) // 隐藏按钮
                .setConfirmButtonVisible(false)
                .showSpinner(true, "请稍候...") // 显示加载圈
                .build();
        downloadDialog.show();

        mExecutor.execute(() -> {
            try {
                PackageInfo pkgInfo = BlackBoxCore.get().getBPackageManager().getPackageInfo(mPackageName, 0, USER_ID);
                if (pkgInfo == null || pkgInfo.applicationInfo == null) {
                    throw new IOException("无法获取应用包信息");
                }

                File sourceFile = new File(pkgInfo.applicationInfo.sourceDir);

                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs();
                }

                long versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ?
                        pkgInfo.getLongVersionCode() : pkgInfo.versionCode;

                File destFile = new File(downloadDir, mPackageName + "_" + versionCode + ".apk");

                copyFile(sourceFile, destFile);

                mUiHandler.post(() -> {
                    downloadDialog.dismiss();
                    // 显示下载完成对话框
                    new CustomDialog.Builder(AppManageActivity.this)
                            .setTitle("下载完成")
                            .setMessage("APK已保存到：" + destFile.getAbsolutePath())
                            .setConfirmButton("好的")
                            .setCancelButtonVisible(false)
                            .setSingleButtonAlignment(CustomDialog.ALIGN_CENTER)
                            .show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                mUiHandler.post(() -> {
                    downloadDialog.dismiss();
                    CustomToast.show(AppManageActivity.this, "下载失败: " + e.getMessage(), 3500, CustomToast.Level.ERROR);
                });
            }
        });
    }

    private void copyFile(File source, File dest) throws IOException {
        try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    private void showForceStopDialog() {
        new CustomDialog.Builder(this)
                .setTitle("强行停止")
                .setMessage("如果强行停止某个应用，可能会导致其发生意外行为。您确定要强行停止 " + appName.getText().toString() + " 吗？")
                .setConfirmButton("确定")
                .setCancelButton("取消")
                .setListener(new CustomDialog.OnDialogActionClickListener() {
                    @Override
                    public void onConfirmClick() {
                        BlackBoxCore.get().stopPackage(mPackageName, USER_ID);
                        CustomToast.show(AppManageActivity.this, appName.getText().toString() + " 已被停止", 2000, CustomToast.Level.INFO);
                    }

                    @Override
                    public void onCancelClick() {}
                })
                .show();
    }

    private void showClearDataDialog() {
        new CustomDialog.Builder(this)
                .setTitle("清除数据")
                .setMessage("此操作将永久删除此应用的所有数据，包括所有文件、设置、帐户和数据库等。您确定要清除 " + appName.getText().toString() + " 的应用数据吗？")
                .setConfirmButton("确定")
                .setCancelButton("取消")
                .setListener(new CustomDialog.OnDialogActionClickListener() {
                    @Override
                    public void onConfirmClick() {
                        CustomToast.show(AppManageActivity.this, "正在清除数据...", 2000, CustomToast.Level.INFO);
                        mExecutor.execute(() -> {
                            BlackBoxCore.get().clearPackage(mPackageName, USER_ID);

                            mUiHandler.post(() -> {
                                CustomToast.show(AppManageActivity.this, "数据清除成功", 2000, CustomToast.Level.SUCCESS);
                                loadStorageInfo();
                            });
                        });
                    }

                    @Override
                    public void onCancelClick() {}
                })
                .show();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setTouchAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }
}