package com.huadows.fastapp.view;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.huadows.fastapp.R;
import com.huadows.fastapp.util.UriUtils;
import com.huadows.ui.CustomDialog;
import com.huadows.ui.CustomToast;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;

/**
 * 此 Activity 用于处理来自外部应用的 APK "打开方式" 请求。
 * 它本身是透明的，会立即弹出一个 CustomDialog 供用户确认。
 */
public class ExternalInstallActivity extends AppCompatActivity {

    private static final String TAG = "ExternalInstallActivity";
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private CustomDialog mDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null || intent.getData() == null) {
            finishWithToast("无效的安装请求");
            return;
        }

        Uri data = intent.getData();
        String apkPath = UriUtils.getPathFromUri(this, data);

        if (apkPath == null || !new File(apkPath).exists()) {
            finishWithToast("无法找到安装包文件");
            Log.e(TAG, "无法从URI找到APK文件: " + data);
            return;
        }

        showInstallConfirmDialog(apkPath);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 确保 Activity 销毁时对话框也关闭
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    private void showInstallConfirmDialog(String apkPath) {
        PackageManager pm = getPackageManager();
        PackageInfo packageInfo = pm.getPackageArchiveInfo(apkPath, 0);

        if (packageInfo == null) {
            finishWithToast("无法解析安装包");
            return;
        }

        packageInfo.applicationInfo.sourceDir = apkPath;
        packageInfo.applicationInfo.publicSourceDir = apkPath;

        Drawable icon = packageInfo.applicationInfo.loadIcon(pm);
        String name = packageInfo.applicationInfo.loadLabel(pm).toString();
        String version = "版本 " + packageInfo.versionName;

        View customView = LayoutInflater.from(this).inflate(R.layout.dialog_apk_install_confirm, null);
        ImageView appIcon = customView.findViewById(R.id.dialog_app_icon);
        TextView appName = customView.findViewById(R.id.dialog_app_name);
        TextView appVersion = customView.findViewById(R.id.dialog_app_version);

        appIcon.setImageDrawable(icon);
        appName.setText(name);
        appVersion.setText(version);

        mDialog = new CustomDialog.Builder(this)
                .setTitle("安装应用")
                .setView(customView)
                .setConfirmButton("安装")
                .setCancelButton("取消")
                .setAutoDismiss(false) // 关键：设置点击按钮后不自动关闭
                .setListener(new CustomDialog.OnDialogActionClickListener() {
                    @Override
                    public void onConfirmClick() {
                        // 点击“安装”后，从此方法开始处理安装逻辑
                        handleInstallAction(apkPath);
                    }

                    @Override
                    public void onCancelClick() {
                        // 点击“取消”后，关闭对话框并结束Activity
                        if (mDialog != null) mDialog.dismiss();
                        finish();
                    }
                })
                .show();

        // 监听对话框的取消事件（例如按返回键），确保 Activity 会随之关闭
        if (mDialog != null) {
            mDialog.setOnCancelListener(dialog -> finish());
        }
    }

    private void handleInstallAction(String apkPath) {
        if (mDialog == null) return;

        Button confirmButton = mDialog.getConfirmButton();
        Button cancelButton = mDialog.getCancelButton();

        if (confirmButton == null || cancelButton == null) return;

        // 更新UI：显示“安装中”并禁用按钮
        confirmButton.setText("正在安装...");
        confirmButton.setEnabled(false);
        cancelButton.setVisibility(View.GONE); // 隐藏取消按钮

        mExecutor.execute(() -> {
            final InstallResult result = BlackBoxCore.get().installPackageAsUser(new File(apkPath), 0);

            mMainHandler.post(() -> {
                if (result.success) {
                    confirmButton.setText("安装完成");
                    CustomToast.show(this, "安装成功", 2000, CustomToast.Level.SUCCESS);
                } else {
                    confirmButton.setText("安装失败");
                    CustomToast.show(this, "安装失败: " + result.msg, 3500, CustomToast.Level.ERROR);
                }
                
                // 重新启用按钮，并设置新的点击事件来关闭对话框和Activity
                confirmButton.setEnabled(true);
                confirmButton.setOnClickListener(v -> {
                    if (mDialog != null) mDialog.dismiss();
                    finish();
                });
            });
        });
    }

    private void installToVirtual(String apkPath) {
        // 这个方法现在被 handleInstallAction 取代
    }

    private void finishWithToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        finish();
    }
}