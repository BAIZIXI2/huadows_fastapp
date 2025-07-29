package com.huadows.fastapp.view.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.huadows.fastapp.R;
import com.huadows.fastapp.adapter.AppAdapter;
import com.huadows.fastapp.bean.AppInfo;
import com.huadows.fastapp.view.AppManageActivity;
import com.huadows.ui.CustomToast; // 引入自定义 Toast 类

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    private RecyclerView mRecyclerView;
    private TextView mEmptyView;
    private AppAdapter mAdapter;
    private final List<AppInfo> mApps = new ArrayList<>();
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());

    private ActivityResultLauncher<Intent> mApkPickerLauncher;
    private ActivityResultLauncher<String[]> mPermissionLauncher;
    private ActivityResultLauncher<Intent> mAppManageLauncher;

    private FloatingActionButton mFab;

    private static final String[] STORAGE_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActivityResultLaunchers();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        mRecyclerView = view.findViewById(R.id.recycler_view);
        mEmptyView = view.findViewById(R.id.empty_view);
        mFab = view.findViewById(R.id.fab_add_apk);

        setupRecyclerView();
        setupFab(mFab);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && mFab.isShown()) {
                    mFab.hide();
                } else if (dy < 0 && !mFab.isShown()) {
                    mFab.show();
                }
            }
        });

        loadApps();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 当从应用管理页返回时，可能应用已被卸载，需要刷新列表
        loadApps();
    }
    
    private void setupRecyclerView() {
        mAdapter = new AppAdapter(mApps);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(appInfo -> {
            BlackBoxCore.get().launchApk(appInfo.packageName, 0);
        });
        
        mAdapter.setOnItemLongClickListener(appInfo -> {
            Intent intent = new Intent(getActivity(), AppManageActivity.class);
            intent.putExtra(AppManageActivity.EXTRA_PACKAGE_NAME, appInfo.packageName);
            mAppManageLauncher.launch(intent);
            return true;
        });
    }

    private void setupFab(FloatingActionButton fab) {
        fab.setOnClickListener(v -> {
            if (requireActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openFilePicker();
            } else {
                mPermissionLauncher.launch(STORAGE_PERMISSIONS);
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.android.package-archive");
        mApkPickerLauncher.launch(intent);
    }

    private void setupActivityResultLaunchers() {
        mApkPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri apkUri = result.getData().getData();
                        if (apkUri != null) {
                            installApk(apkUri);
                        }
                    }
                });

        mPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            if (Boolean.TRUE.equals(result.get(Manifest.permission.READ_EXTERNAL_STORAGE))) {
                openFilePicker();
            } else {
                CustomToast.show(requireActivity(), "需要存储权限才能选择APK文件", 2000, CustomToast.Level.WARNING);
            }
        });
        
        mAppManageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                loadApps();
            }
        });
    }

    private void loadApps() {
        mExecutor.execute(() -> {
            List<AppInfo> installedApps = new ArrayList<>();
            List<ApplicationInfo> packages = BlackBoxCore.get().getInstalledApplications(0, 0);

            for (ApplicationInfo app : packages) {
                AppInfo info = new AppInfo();
                info.appName = app.loadLabel(requireActivity().getPackageManager()).toString();
                info.packageName = app.packageName;
                info.icon = app.loadIcon(requireActivity().getPackageManager());
                installedApps.add(info);
            }

            mUiHandler.post(() -> {
                mApps.clear();
                mApps.addAll(installedApps);
                mAdapter.notifyDataSetChanged();
                mEmptyView.setVisibility(mApps.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void installApk(Uri apkUri) {
        CustomToast.show(requireActivity(), "正在安装...", 2000, CustomToast.Level.INFO);
        mExecutor.execute(() -> {
            InstallResult result = BlackBoxCore.get().installPackageAsUser(apkUri, 0);

            mUiHandler.post(() -> {
                if (result.success) {
                    CustomToast.show(requireActivity(), "安装成功", 2000, CustomToast.Level.SUCCESS);
                    loadApps();
                } else {
                    String msg = "安装失败: " + result.msg;
                    CustomToast.show(requireActivity(), msg, 3500, CustomToast.Level.ERROR);
                }
            });
        });
    }
}