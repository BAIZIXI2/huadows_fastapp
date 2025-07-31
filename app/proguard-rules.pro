# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# ===================================================================
#      Keep Rules for BlackBox Virtualization Framework
# ===================================================================
# 保持 BlackBox 核心包下的所有内容，防止其内部逻辑被混淆
-keep class com.topjohnwu.** { *; }
-keep class top.niunaijun.blackbox.** { *; }
-keep interface top.niunaijun.blackbox.** { *; }

# 保持 BlackBox 的存根组件（Provider, Activity, Service）不被移除
# 这些是框架与系统交互的关键
-keep class top.niunaijun.blackbox.client.stub.** { *; }
-keep class top.niunaijun.blackbox.proxy.** { *; }

# 保持所有继承自 ContentProvider 的类，因为 BlackBox 通过 Provider 进行初始化
-keep class * extends android.content.ContentProvider

# 保持 Application 类和它的 attachBaseContext 方法
-keep class * extends android.app.Application {
    <init>();
    void attachBaseContext(android.content.Context);
}

# 保持四大组件，防止被 R8 当作未使用代码移除
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# 防止因为找不到可选的库（如 arouter）而警告/报错
-dontwarn com.alibaba.android.arouter.core.DexSplitter
-dontwarn com.alibaba.android.arouter.routes.**
# ======================= End of BlackBox Rules =======================