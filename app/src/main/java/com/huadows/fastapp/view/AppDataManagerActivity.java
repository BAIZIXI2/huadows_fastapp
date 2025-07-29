package com.huadows.fastapp.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.huadows.fastapp.R;
import com.huadows.fastapp.adapter.AppDataAdapter;
import com.huadows.fastapp.bean.AppDataInfo;
import com.huadows.fastapp.bean.BackupManifest;
import com.huadows.fastapp.bean.ManifestAppInfo;
import com.huadows.fastapp.util.CryptoUtils;
import com.huadows.fastapp.util.FileUtils;
import com.huadows.fastapp.util.UriUtils;
import com.huadows.fastapp.util.ZipUtils;
import com.huadows.ui.CustomDialog;
import com.huadows.ui.CustomToast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.env.BEnvironment;

public class AppDataManagerActivity extends AppCompatActivity implements AppDataAdapter.OnSelectionChangedListener {

    private static final String TAG = "AppDataManagerActivity";
    private static final int USER_ID = 0;

    private RecyclerView mRecyclerView;
    private TextView mEmptyView;
    private View mBtnImport, mBtnExport; // 改为 View 类型以适应 LinearLayout
    private AppDataAdapter mAdapter;
    private final List<AppDataInfo> mApps = new ArrayList<>();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());

    private ActivityResultLauncher<String> mFilePickerLauncher;
    private ActivityResultLauncher<String[]> mPermissionLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_data_manager);

        initViews();
        setupActivityResultLaunchers();
        setupRecyclerView();
        setupClickListeners();
        loadApps();
    }

    private void initViews() {
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
        mRecyclerView = findViewById(R.id.recycler_view);
        mEmptyView = findViewById(R.id.empty_view);
        mBtnImport = findViewById(R.id.button_import);
        mBtnExport = findViewById(R.id.button_export);
    }

    private void setupRecyclerView() {
        mAdapter = new AppDataAdapter(mApps, this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
    }

    private void setupActivityResultLaunchers() {
        mPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            if (Boolean.TRUE.equals(result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
                CustomToast.show(this, "存储权限已获取", 2000, CustomToast.Level.SUCCESS);
            } else {
                CustomToast.show(this, "需要存储权限才能导出/导入数据", 3000, CustomToast.Level.WARNING);
            }
        });

        mFilePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                handleImport(uri);
            }
        });
    }

    /**
     * 为视图应用点击缩小、松开恢复的动画
     * @param view 要应用动画的视图
     */
    private void applyClickAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 按下时缩小
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // 松开或取消时恢复
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            // 返回 false 以确保 onClickListener 仍然会被触发
            return false;
        });
    }

    private void setupClickListeners() {
        // 应用点击动画
        applyClickAnimation(mBtnImport);
        applyClickAnimation(mBtnExport);

        mBtnImport.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                mFilePickerLauncher.launch("application/zip");
            }
        });

        mBtnExport.setOnClickListener(v -> {
            if (mAdapter.getSelectedApps().isEmpty()) {
                CustomToast.show(this, "请选择应用", 2000, CustomToast.Level.WARNING);
                return;
            }
            if (checkStoragePermission()) {
                handleExport();
            }
        });
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            mPermissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE});
            return false;
        }
    }

    private void loadApps() {
        mExecutor.execute(() -> {
            List<AppDataInfo> installedApps = new ArrayList<>();
            List<ApplicationInfo> packages = BlackBoxCore.get().getInstalledApplications(0, USER_ID);

            for (ApplicationInfo app : packages) {
                AppDataInfo info = new AppDataInfo();
                info.appName = app.loadLabel(getPackageManager()).toString();
                info.packageName = app.packageName;
                info.icon = app.loadIcon(getPackageManager());
                installedApps.add(info);
            }

            mUiHandler.post(() -> {
                mApps.clear();
                mApps.addAll(installedApps);
                mAdapter.notifyDataSetChanged();
                mEmptyView.setVisibility(mApps.isEmpty() ? View.VISIBLE : View.GONE);
                onSelectionChanged(0);
            });
        });
    }

    private void handleExport() {
        List<AppDataInfo> selectedApps = mAdapter.getSelectedApps();
        // 此处不再需要检查是否为空，因为点击事件已经处理

        final CustomDialog progressDialog = new CustomDialog.Builder(this)
                .setTitle("正在导出")
                .showSpinner(true, "请稍候...")
                .setCancelButtonVisible(false)
                .setConfirmButtonVisible(false)
                .build();
        progressDialog.show();

        mExecutor.execute(() -> {
            File stagingDir = new File(getCacheDir(), "backup_staging");
            try {
                FileUtils.deleteDir(stagingDir);
                stagingDir.mkdirs();

                BackupManifest manifest = new BackupManifest();
                manifest.backupTimestamp = System.currentTimeMillis();
                manifest.deviceModel = Build.MODEL;
                manifest.androidVersion = Build.VERSION.SDK_INT;
                manifest.backedUpApps = new ArrayList<>();

                for (AppDataInfo app : selectedApps) {
                    ManifestAppInfo manifestApp = new ManifestAppInfo();
                    manifestApp.appName = app.appName;
                    manifestApp.packageName = app.packageName;

                    File appDataStagingDir = new File(stagingDir, "data" + File.separator + app.packageName);

                    // 1. 处理内部数据
                    File internalDataDir = BEnvironment.getDataDir(app.packageName, USER_ID);
                    if (internalDataDir.exists()) {
                        File internalStaging = new File(appDataStagingDir, "internal");
                        FileUtils.copyFile(internalDataDir, internalStaging);
                        manifestApp.internalDataMd5 = CryptoUtils.calculateDirectoryMd5(internalStaging);
                    }

                    // 2. 处理外部数据
                    File externalDataDir = BEnvironment.getExternalDataDir(app.packageName, USER_ID);
                    if (externalDataDir.exists()) {
                        File externalStaging = new File(appDataStagingDir, "external");
                        FileUtils.copyFile(externalDataDir, externalStaging);
                        manifestApp.externalDataMd5 = CryptoUtils.calculateDirectoryMd5(externalStaging);
                    }
                    manifest.backedUpApps.add(manifestApp);
                }

                // 3. 创建 manifest.json
                createManifestJson(new File(stagingDir, "manifest.json"), manifest);

                // 4. 打包
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadDir.exists()) downloadDir.mkdirs();
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                File destZipFile = new File(downloadDir, "fastapp_backup_" + timestamp + ".zip");
                ZipUtils.zipDirectory(stagingDir, destZipFile);

                mUiHandler.post(() -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    new CustomDialog.Builder(this)
                            .setTitle("导出成功")
                            .setMessage("数据已导出到 " + destZipFile.getAbsolutePath())
                            .setConfirmButton("好的")
                            .setCancelButtonVisible(false)
                            .setSingleButtonAlignment(CustomDialog.ALIGN_CENTER)
                            .show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                mUiHandler.post(() -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    new CustomDialog.Builder(this)
                            .setTitle("导出失败")
                            .setMessage("发生错误: " + e.getMessage())
                            .setConfirmButton("好的")
                            .setCancelButtonVisible(false)
                            .show();
                });
            } finally {
                FileUtils.deleteDir(stagingDir);
            }
        });
    }

    private void handleImport(Uri zipUri) {
        mExecutor.execute(() -> {
            File tempZipFile = null;
            try {
                // 复制到缓存，避免直接操作原始文件
                tempZipFile = new File(getCacheDir(), "import_temp.zip");
                try (InputStream in = getContentResolver().openInputStream(zipUri)) {
                    FileUtils.copyFile(in, tempZipFile);
                }

                // 读取 manifest
                String manifestJson = ZipUtils.extractFileToString(tempZipFile, "manifest.json");
                if (manifestJson == null) {
                    throw new IOException("备份文件无效: 找不到 manifest.json");
                }
                
                BackupManifest manifest = parseManifestJson(manifestJson);
                
                StringBuilder importInfo = new StringBuilder("此备份包包含以下应用的数据：\n\n");
                for(ManifestAppInfo app : manifest.backedUpApps) {
                    importInfo.append("- ").append(app.appName).append(" (").append(app.packageName).append(")\n");
                }
                importInfo.append("\n导入将覆盖这些应用在虚拟环境中的现有数据，是否继续？");

                final File finalTempZipFile = tempZipFile;
                mUiHandler.post(() -> {
                    new CustomDialog.Builder(this)
                            .setTitle("确认导入")
                            .setMessage(importInfo.toString())
                            .setConfirmButton("继续导入")
                            .setCancelButton("取消")
                            .setListener(new CustomDialog.OnDialogActionClickListener() {
                                @Override
                                public void onConfirmClick() {
                                    importData(finalTempZipFile, manifest);
                                }
                                @Override
                                public void onCancelClick() {
                                    FileUtils.deleteDir(finalTempZipFile);
                                }
                            })
                            .show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                if(tempZipFile != null) FileUtils.deleteDir(tempZipFile);
                mUiHandler.post(() -> new CustomDialog.Builder(this)
                        .setTitle("导入失败")
                        .setMessage("解析备份文件失败: " + e.getMessage())
                        .setConfirmButton("好的")
                        .setCancelButtonVisible(false)
                        .show());
            }
        });
    }

    private void importData(File zipFile, BackupManifest manifest) {
        final CustomDialog progressDialog = new CustomDialog.Builder(this)
                .setTitle("正在导入")
                .showSpinner(true, "请稍候...")
                .setCancelButtonVisible(false)
                .setConfirmButtonVisible(false)
                .build();
        progressDialog.show();

        mExecutor.execute(() -> {
            File tempUnzipDir = new File(getCacheDir(), "unzip_temp");
            List<String> errors = new ArrayList<>();
            try {
                FileUtils.deleteDir(tempUnzipDir);
                tempUnzipDir.mkdirs();
                ZipUtils.unzip(zipFile, tempUnzipDir);
                
                for(ManifestAppInfo app : manifest.backedUpApps) {
                    if (!BlackBoxCore.get().isInstalled(app.packageName, USER_ID)) {
                        errors.add(app.appName + ": 未安装，已跳过。");
                        continue;
                    }

                    File appDataStagingDir = new File(tempUnzipDir, "data" + File.separator + app.packageName);
                    
                    // 验证内部数据MD5
                    File internalStaging = new File(appDataStagingDir, "internal");
                    if (internalStaging.exists()) {
                        String md5 = CryptoUtils.calculateDirectoryMd5(internalStaging);
                        if (!TextUtils.equals(md5, app.internalDataMd5)) {
                            errors.add(app.appName + ": 内部数据校验失败，已跳过。");
                            continue;
                        }
                    }

                    // 验证外部数据MD5
                    File externalStaging = new File(appDataStagingDir, "external");
                     if (externalStaging.exists()) {
                        String md5 = CryptoUtils.calculateDirectoryMd5(externalStaging);
                        if (!TextUtils.equals(md5, app.externalDataMd5)) {
                            errors.add(app.appName + ": 外部数据校验失败，已跳过。");
                            continue;
                        }
                    }
                    
                    // 清理并拷贝
                    File internalDataDir = BEnvironment.getDataDir(app.packageName, USER_ID);
                    File externalDataDir = BEnvironment.getExternalDataDir(app.packageName, USER_ID);
                    FileUtils.deleteDir(internalDataDir);
                    FileUtils.deleteDir(externalDataDir);

                    if (internalStaging.exists()) FileUtils.copyFile(internalStaging, internalDataDir);
                    if (externalStaging.exists()) FileUtils.copyFile(externalStaging, externalDataDir);
                }

                mUiHandler.post(() -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    StringBuilder resultMessage = new StringBuilder();
                    if(errors.isEmpty()) {
                        resultMessage.append("所有应用数据导入成功！\n\n请强行停止并重启相关应用以使数据生效。");
                    } else {
                        resultMessage.append("导入完成，但出现以下问题：\n\n");
                        for(String err : errors) {
                            resultMessage.append("- ").append(err).append("\n");
                        }
                    }
                    new CustomDialog.Builder(this)
                        .setTitle("导入完成")
                        .setMessage(resultMessage.toString())
                        .setConfirmButton("好的")
                        .setCancelButtonVisible(false)
                        .show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                mUiHandler.post(() -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    new CustomDialog.Builder(this)
                        .setTitle("导入失败")
                        .setMessage("发生严重错误: " + e.getMessage())
                        .setConfirmButton("好的")
                        .setCancelButtonVisible(false)
                        .show();
                });
            } finally {
                FileUtils.deleteDir(tempUnzipDir);
                FileUtils.deleteDir(zipFile); // 删除缓存的zip
            }
        });
    }

    @Override
    public void onSelectionChanged(int selectedCount) {
        mBtnExport.setEnabled(selectedCount > 0);
        mBtnExport.setAlpha(selectedCount > 0 ? 1.0f : 0.5f);
    }
    
    private void createManifestJson(File file, BackupManifest manifest) throws JSONException, IOException {
        JSONObject root = new JSONObject();
        root.put("backupTimestamp", manifest.backupTimestamp);
        root.put("deviceModel", manifest.deviceModel);
        root.put("androidVersion", manifest.androidVersion);
        
        JSONArray appsArray = new JSONArray();
        for(ManifestAppInfo app : manifest.backedUpApps) {
            JSONObject appObj = new JSONObject();
            appObj.put("appName", app.appName);
            appObj.put("packageName", app.packageName);
            appObj.put("internalDataMd5", app.internalDataMd5);
            appObj.put("externalDataMd5", app.externalDataMd5);
            appsArray.put(appObj);
        }
        root.put("backedUpApps", appsArray);

        try(FileWriter writer = new FileWriter(file)) {
            writer.write(root.toString(4)); // 格式化输出
        }
    }

    private BackupManifest parseManifestJson(String json) throws JSONException {
        BackupManifest manifest = new BackupManifest();
        JSONObject root = new JSONObject(json);
        manifest.backupTimestamp = root.optLong("backupTimestamp");
        manifest.deviceModel = root.optString("deviceModel");
        manifest.androidVersion = root.optInt("androidVersion");
        
        JSONArray appsArray = root.getJSONArray("backedUpApps");
        manifest.backedUpApps = new ArrayList<>();
        for(int i = 0; i < appsArray.length(); i++) {
            JSONObject appObj = appsArray.getJSONObject(i);
            ManifestAppInfo appInfo = new ManifestAppInfo();
            appInfo.appName = appObj.getString("appName");
            appInfo.packageName = appObj.getString("packageName");
            appInfo.internalDataMd5 = appObj.optString("internalDataMd5", null);
            appInfo.externalDataMd5 = appObj.optString("externalDataMd5", null);
            manifest.backedUpApps.add(appInfo);
        }
        return manifest;
    }
}