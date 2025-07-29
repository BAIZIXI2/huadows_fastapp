// 文件路径: app/src/main/java/com/huadows/fastapp/server/bean/AppInfo.java
package com.huadows.fastapp.server.bean;

public class AppInfo {
    public String name;
    public String packageName;
    public String versionName;
    public int versionCode;

    public AppInfo(String name, String packageName, String versionName, int versionCode) {
        this.name = name;
        this.packageName = packageName;
        this.versionName = versionName;
        this.versionCode = versionCode;
    }
}