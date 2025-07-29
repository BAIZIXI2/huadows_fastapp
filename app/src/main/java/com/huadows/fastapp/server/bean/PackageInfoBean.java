// 文件路径: app/src/main/java/com/huadows/fastapp/server/bean/PackageInfoBean.java
package com.huadows.fastapp.server.bean;

import android.content.pm.PackageInfo;
import top.niunaijun.blackbox.entity.pm.BStorageInfo; // 引入 BStorageInfo

// 一个纯粹的 DTO（数据传输对象），用于API返回
public class PackageInfoBean {
    public String packageName;
    public String versionName;
    public int versionCode;
    public String applicationLabel; // 应用名称
    
    // 详细存储信息
    public long totalSizeBytes;     // 总大小
    public long appSize;            // 应用大小 (apk)
    public long dataSize;           // 数据大小
    public long cacheSize;          // 缓存大小

    public PackageInfoBean() {}

    /**
     * 从系统的 PackageInfo 和 BStorageInfo 对象转换
     * @param packageInfo 系统 PackageInfo 对象
     * @param label 加载好的应用标签字符串
     * @param storageInfo BlackBox 的存储信息对象
     */
    public static PackageInfoBean from(PackageInfo packageInfo, String label, BStorageInfo storageInfo) {
        if (packageInfo == null) {
            return null;
        }
        PackageInfoBean bean = new PackageInfoBean();
        bean.packageName = packageInfo.packageName;
        bean.versionName = packageInfo.versionName;
        bean.versionCode = packageInfo.versionCode;
        bean.applicationLabel = label;

        if (storageInfo != null) {
            bean.totalSizeBytes = storageInfo.totalSize;
            bean.appSize = storageInfo.appSize;
            bean.dataSize = storageInfo.dataSize;
            bean.cacheSize = storageInfo.cacheSize;
        }
        return bean;
    }
}