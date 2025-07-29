// 文件路径: app/src/main/java/com/huadows/fastapp/server/ProcessBootstrapActivity.java
package com.huadows.fastapp.server;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

/**
 * 这是一个透明的、会立即销毁的 Activity。
 * 它的唯一目的就是被外部调用 startActivity，从而强制系统创建 :blackbox 进程。
 */
public class ProcessBootstrapActivity extends Activity {
    private static final String TAG = "BootstrapActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ProcessBootstrapActivity started, finishing immediately.");
        // 立即结束自己，用户不会有任何感知
        finish();
    }
}