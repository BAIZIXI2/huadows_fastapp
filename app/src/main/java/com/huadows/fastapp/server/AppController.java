// 文件路径: app/src/main/java/com/huadows/fastapp/server/AppController.java
package com.huadows.fastapp.server;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.util.Log;

import com.google.gson.Gson;
import com.huadows.fastapp.server.bean.AppInfo;
import com.huadows.fastapp.server.bean.PackageInfoBean;
import com.huadows.fastapp.util.ImageUtils;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestMapping;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.RestController;
import com.yanzhenjie.andserver.framework.body.FileBody;
import com.yanzhenjie.andserver.framework.body.JsonBody;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.ResponseBody;
import com.yanzhenjie.andserver.http.multipart.MultipartFile;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.system.pm.BPackageManagerService;
import top.niunaijun.blackbox.entity.pm.BStorageInfo;
import top.niunaijun.blackbox.entity.pm.InstallResult;

@RestController
@RequestMapping(path = "/app")
public class AppController {

    private final Gson gson = new Gson();
    private static final String TAG = "AppController";


    @GetMapping("/list")
    public ResponseBody getInstalledApplications(@RequestParam("userId") int userId) {
        List<ApplicationInfo> installedList = BlackBoxCore.get().getInstalledApplications(0, userId);
        if (installedList == null) {
            return new JsonBody(gson.toJson(Collections.emptyList()));
        }

        PackageManager pm = BlackBoxCore.getPackageManager();
        List<AppInfo> appInfoList = installedList.stream()
                .map(info -> {
                    try {
                        PackageInfo packageInfo = BPackageManagerService.get().getPackageInfo(info.packageName, 0, userId);
                        String label = info.loadLabel(pm).toString();
                        return new AppInfo(
                                label,
                                info.packageName,
                                packageInfo != null ? packageInfo.versionName : "N/A",
                                packageInfo != null ? packageInfo.versionCode : 0
                        );
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to get list info for package: " + info.packageName, e);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        return new JsonBody(gson.toJson(appInfoList));
    }

    @GetMapping("/getPackageInfo")
    public ResponseBody getPackageInfo(@RequestParam("packageName") String packageName,
                                     @RequestParam("userId") int userId) {
        try {
            PackageInfo packageInfo = BPackageManagerService.get().getPackageInfo(packageName, PackageManager.GET_META_DATA, userId);
            if (packageInfo == null) {
                return new JsonBody(gson.toJson(null));
            }
            BStorageInfo storageInfo = BPackageManagerService.get().getStorageInfo(packageName, userId);
            String label;
            if (packageInfo.applicationInfo != null) {
                label = packageInfo.applicationInfo.loadLabel(BlackBoxCore.getPackageManager()).toString();
            } else {
                label = packageName;
            }
            PackageInfoBean bean = PackageInfoBean.from(packageInfo, label, storageInfo);
            return new JsonBody(gson.toJson(bean));
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to getPackageInfo due to RemoteException", e);
            return new JsonBody(gson.toJson(null));
        }
    }

    @GetMapping("/getAppInfoWithIconLink")
    public ResponseBody getAppInfoWithIconLink(HttpRequest request,
                                               @RequestParam("packageName") String packageName,
                                               @RequestParam("userId") int userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            ApplicationInfo info = BPackageManagerService.get().getApplicationInfo(packageName, 0, userId);
            if (info == null) {
                response.put("error", "App not found");
                return new JsonBody(gson.toJson(response));
            }
            Drawable iconDrawable = info.loadIcon(BlackBoxCore.getPackageManager());
            Bitmap iconBitmap = ImageUtils.drawableToBitmap(iconDrawable);

            // vvvvvvvvvvvv 关键修复 vvvvvvvvvvvv
            // 步骤 1: 直接从 AuthServiceImpl 获取服务器正在运行的端口
            int port = AuthServiceImpl.getWebServerPort();

            // 步骤 2: 硬编码为 localhost 地址来构建 Base URL
            String baseUrl = "http://localhost:" + port;

            // 步骤 3: 将正确构建的 baseUrl 传递给 IconCacheManager
            String iconUrl = IconCacheManager.get().generateIconUrl(baseUrl, packageName, userId, iconBitmap);
            // ^^^^^^^^^^^^ 修复结束 ^^^^^^^^^^^^

            response.put("packageName", info.packageName);
            response.put("label", info.loadLabel(BlackBoxCore.getPackageManager()).toString());
            response.put("iconUrl", iconUrl);
            return new JsonBody(gson.toJson(response));
        } catch (Exception e) {
            Log.e(TAG, "Failed to getAppInfoWithIconLink", e);
            response.put("error", e.getMessage());
            return new JsonBody(gson.toJson(response));
        }
    }

    // ... (其他方法保持不变)
    @GetMapping("/isInstalled")
    public boolean isInstalled(@RequestParam("packageName") String packageName,
                               @RequestParam("userId") int userId) {
        return BlackBoxCore.get().isInstalled(packageName, userId);
    }

    @PostMapping("/install")
    public ResponseBody installApk(@RequestParam("apk") MultipartFile apkFile,
                                   @RequestParam("userId") int userId) {
        Map<String, Object> response = new HashMap<>();
        if (apkFile == null || apkFile.isEmpty()) {
            response.put("status", "error");
            response.put("message", "APK file is missing.");
            return new JsonBody(gson.toJson(response));
        }

        File tempApk = new File(BlackBoxCore.getContext().getCacheDir(), UUID.randomUUID().toString() + ".apk");
        try {
            apkFile.transferTo(tempApk);
            Log.d(TAG, "APK uploaded to temporary file: " + tempApk.getAbsolutePath());

            InstallResult installResult = BlackBoxCore.get().installPackageAsUser(tempApk, userId);

            if (installResult.success) {
                response.put("status", "success");
                response.put("message", "Installation successful.");
                response.put("packageName", installResult.packageName);
            } else {
                response.put("status", "error");
                response.put("message", "Installation failed: " + installResult.msg);
                response.put("packageName", installResult.packageName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to install APK", e);
            response.put("status", "error");
            response.put("message", "An unexpected error occurred: " + e.getMessage());
        } finally {
            if (tempApk.exists()) {
                if (tempApk.delete()) {
                    Log.d(TAG, "Temporary APK file deleted.");
                } else {
                    Log.w(TAG, "Failed to delete temporary APK file.");
                }
            }
        }
        return new JsonBody(gson.toJson(response));
    }

    @PostMapping("/uninstall")
    public ResponseBody uninstall(@RequestParam("packageName") String packageName,
                                  @RequestParam("userId") int userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            BlackBoxCore.get().uninstallPackageAsUser(packageName, userId);
            response.put("status", "success");
            response.put("message", "卸载请求已发送");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return new JsonBody(gson.toJson(response));
    }

    @PostMapping("/forceStop")
    public ResponseBody forceStop(@RequestParam("packageName") String packageName,
                                  @RequestParam("userId") int userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            BlackBoxCore.get().stopPackage(packageName, userId);
            response.put("status", "success");
            response.put("message", "应用已停止");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return new JsonBody(gson.toJson(response));
    }
    
    @PostMapping("/clearData")
    public ResponseBody clearData(@RequestParam("packageName") String packageName,
                                  @RequestParam("userId") int userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            BlackBoxCore.get().clearPackage(packageName, userId);
            response.put("status", "success");
            response.put("message", "数据已清除");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return new JsonBody(gson.toJson(response));
    }

    @PostMapping("/clearCache")
    public ResponseBody clearCache(@RequestParam("packageName") String packageName,
                                   @RequestParam("userId") int userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean success = BlackBoxCore.get().clearPackageCache(packageName, userId);
            response.put("status", success ? "success" : "failure");
            response.put("message", success ? "缓存已清除" : "清除缓存失败");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return new JsonBody(gson.toJson(response));
    }
    
    @GetMapping("/downloadApk")
    public ResponseBody downloadApk(@RequestParam("packageName") String packageName,
                                    @RequestParam("userId") int userId) {
        try {
            PackageInfo pkgInfo = BlackBoxCore.get().getBPackageManager().getPackageInfo(packageName, 0, userId);
            if (pkgInfo == null || pkgInfo.applicationInfo == null) {
                throw new Exception("无法获取应用包信息");
            }
            File apkFile = new File(pkgInfo.applicationInfo.sourceDir);
            if (!apkFile.exists()) {
                throw new Exception("APK文件不存在");
            }
            return new FileBody(apkFile);
        } catch (Exception e) {
            Log.e(TAG, "Failed to download APK for " + packageName, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "下载失败: " + e.getMessage());
            return new JsonBody(gson.toJson(error));
        }
    }

    @PostMapping("/setDpi")
    public ResponseBody setDpi(@RequestParam("packageName") String packageName,
                               @RequestParam("userId") int userId,
                               @RequestParam("dpi") int dpi) {
        Map<String, Object> response = new HashMap<>();
        try {
            BlackBoxCore.get().setVirtualDPI(packageName, dpi, userId);
            response.put("status", "success");
            response.put("message", "DPI设置成功，请强行停止并重启应用以生效");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return new JsonBody(gson.toJson(response));
    }
}