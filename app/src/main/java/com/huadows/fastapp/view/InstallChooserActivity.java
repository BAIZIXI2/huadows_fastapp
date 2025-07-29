package com.huadows.fastapp.view;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.huadows.fastapp.BuildConfig;
import com.huadows.fastapp.R;
import com.huadows.fastapp.util.UriUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;

public class InstallChooserActivity extends AppCompatActivity {

    private static final String TAG = "InstallChooserActivity";
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private String mApkPath;

    private ImageView appIcon;
    private TextView appName;
    private TextView appVersion;
    private Button buttonOtherApps;
    private Button buttonInstallVirtual;
    private ImageView backButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置我们新的布局文件
        setContentView(R.layout.activity_install_chooser);

        // 初始化视图
        initViews();

        // 获取并处理传入的APK文件路径
        Uri data = getIntent().getData();
        if (data == null) {
            finishWithToast("无效的安装请求");
            return;
        }

        mApkPath = UriUtils.getPathFromUri(this, data);
        if (mApkPath == null || !new File(mApkPath).exists()) {
            finishWithToast("无法找到安装包文件");
            Log.e(TAG, "无法从URI找到APK文件: " + data);
            return;
        }

        // 解析APK信息并显示
        parseAndDisplayApkInfo(mApkPath);
        // 设置按钮的点击事件和动画
        setupClickListeners();
    }

    /**
     * 初始化所有视图控件
     */
    private void initViews() {
        backButton = findViewById(R.id.back_button);
        appIcon = findViewById(R.id.app_icon);
        appName = findViewById(R.id.app_name);
        appVersion = findViewById(R.id.app_version);
        buttonOtherApps = findViewById(R.id.button_other_apps);
        buttonInstallVirtual = findViewById(R.id.button_install_virtual);
    }

    /**
     * 解析APK文件，并把图标、名称、版本号显示在界面上
     * @param apkPath APK文件路径
     */
    private void parseAndDisplayApkInfo(String apkPath) {
        PackageManager pm = getPackageManager();
        // PackageManager.GET_META_DATA 可以获取更多信息，但这里只需要基本信息
        PackageInfo packageInfo = pm.getPackageArchiveInfo(apkPath, 0);

        if (packageInfo != null) {
            packageInfo.applicationInfo.sourceDir = apkPath;
            packageInfo.applicationInfo.publicSourceDir = apkPath;

            Drawable icon = packageInfo.applicationInfo.loadIcon(pm);
            String name = packageInfo.applicationInfo.loadLabel(pm).toString();
            String version = "版本 " + packageInfo.versionName;

            appIcon.setImageDrawable(icon);
            appName.setText(name);
            appVersion.setText(version);
        } else {
            Log.e(TAG, "无法解析APK文件: " + apkPath);
            // 解析失败，显示默认信息
            appName.setText("未知应用");
            appVersion.setText("版本未知");
        }
    }

    /**
     * 设置所有点击事件
     */
    private void setupClickListeners() {
        // 返回按钮
        backButton.setOnClickListener(v -> finish());

        // “其它应用”按钮
        buttonOtherApps.setOnClickListener(v -> openWithOtherApps(mApkPath));
        // “安装到快应用”按钮
        buttonInstallVirtual.setOnClickListener(v -> installToVirtual(mApkPath));

        // 为两个按钮设置触摸动画
        setTouchAnimation(buttonOtherApps);
        setTouchAnimation(buttonInstallVirtual);
    }

    /**
     * 调用系统，弹出“打开方式”选择器
     * @param apkPath APK文件路径
     */
    private void openWithOtherApps(String apkPath) {
        try {
            File apkFile = new File(apkPath);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            
            Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", apkFile);
            
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

            // 使用 Intent.createChooser 强制弹出选择器
            Intent chooser = Intent.createChooser(intent, "请选择一个应用");
            startActivity(chooser);

        } catch (Exception e) {
            Log.e(TAG, "openWithOtherApps failed", e);
            Toast.makeText(this, "无法启动应用选择器", Toast.LENGTH_SHORT).show();
        } finally {
            // 用户选择后，此界面也关闭
            finish();
        }
    }

    /**
     * 安装应用到虚拟环境
     * @param apkPath APK文件路径
     */
    private void installToVirtual(final String apkPath) {
        Toast.makeText(this, "正在安装到快应用...", Toast.LENGTH_SHORT).show();
        // 禁用按钮，防止重复点击
        buttonInstallVirtual.setEnabled(false);
        buttonOtherApps.setEnabled(false);

        mExecutor.execute(() -> {
            final InstallResult result = BlackBoxCore.get().installPackageAsUser(new File(apkPath), 0);

            mMainHandler.post(() -> {
                if (result.success) {
                    finishWithToast("安装成功");
                } else {
                    finishWithToast("安装失败: " + result.msg);
                }
            });
        });
    }
    
    /**
     * 为视图设置按下缩小、松开恢复的动画
     * @param view 需要设置动画的视图
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setTouchAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 按下时，缩小视图
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // 松开或取消时，恢复原始大小
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            // 返回 false，让 onClickListener 能够继续接收到事件
            return false; 
        });
    }

    /**
     * 显示一个Toast然后关闭Activity
     * @param message 要显示的消息
     */
    private void finishWithToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        finish();
    }
}