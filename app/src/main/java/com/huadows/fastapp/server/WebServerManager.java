// 文件路径: app/src/main/java/com/huadows/fastapp/server/WebServerManager.java
package com.huadows.fastapp.server;

import android.content.Context;
import android.util.Log;

import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.Server;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Web服务器管理类，用于启动和停止AndServer。
 * 增加了启动同步机制，确保服务器启动完成后再执行后续逻辑。
 */
public class WebServerManager implements Server.ServerListener {
    private static final String TAG = "WebServerManager";
    private static final WebServerManager sInstance = new WebServerManager();
    private Server mServer;
    // 用于同步服务器启动状态的门闩
    private volatile CountDownLatch mStartLatch;

    private WebServerManager() {}

    public static WebServerManager get() {
        return sInstance;
    }

    /**
     * 启动Web服务器。
     * @param context 上下文
     * @param port 端口号
     */
    public void start(Context context, int port) {
        if (mServer != null && mServer.isRunning()) {
            Log.w(TAG, "WebServer is already running.");
            return;
        }

        // 每次启动前都重新初始化门闩
        mStartLatch = new CountDownLatch(1);

        mServer = AndServer.webServer(context)
                .port(port)
                .timeout(10, TimeUnit.SECONDS)
                // 设置监听器，以便在服务器启动/停止/异常时收到回调
                .listener(this)
                .build();
                
        // 启动服务器
        mServer.startup();
    }

    /**
     * 停止Web服务器。
     */
    public void stop() {
        if (mServer != null && mServer.isRunning()) {
            mServer.shutdown();
        }
        // 重置门闩，以防万一
        if (mStartLatch != null && mStartLatch.getCount() > 0) {
            mStartLatch.countDown();
        }
    }

    /**
     * 阻塞当前线程，直到 Web 服务器完全启动或启动超时。
     * @throws InterruptedException 如果线程在等待时被中断
     */
    public void awaitStart() throws InterruptedException {
        if (mStartLatch != null) {
            // 设置一个合理的超时时间（例如15秒），以防服务器因故无法启动导致无限期阻塞
            boolean success = mStartLatch.await(15, TimeUnit.SECONDS);
            if (!success) {
                Log.e(TAG, "WebServer startup timed out after 15 seconds.");
            }
        }
    }

    @Override
    public void onStarted() {
        Log.i(TAG, "WebServer has successfully started.");
        // 服务器成功启动，打开门闩，通知等待的线程
        if (mStartLatch != null) {
            mStartLatch.countDown();
        }
    }

    @Override
    public void onStopped() {
        Log.i(TAG, "WebServer has stopped.");
        // 如果服务器在等待启动期间被停止了，也应该打开门闩，避免死锁
        if (mStartLatch != null) {
            mStartLatch.countDown();
        }
    }

    @Override
    public void onException(Exception e) {
        Log.e(TAG, "An exception occurred on the WebServer.", e);
        // 如果服务器启动时发生异常，也应该打开门闩，避免死锁
        if (mStartLatch != null) {
            mStartLatch.countDown();
        }
    }
}