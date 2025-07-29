package com.huadows.fastappdebug;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log; // 保留 Log 用于 FileLog 自身的错误报告

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A simple file logger for Android applications.
 * Logs messages to a text file in the app's external private files directory.
 * Requires setup(Context, String) before log() calls.
 * File writer initialization is lazy (happens on the first successful log call after setup).
 */
public final class FileLog {

    private static final String TAG = "FileLog";

    // 使用 Application Context 以避免内存泄漏 (虽然使用了 @SuppressLint)
    @SuppressLint("StaticFieldLeak")
    private static Context appContext;
    private static String logFilename;

    private static BufferedWriter writer = null;
    private static File logFile = null; // Store the file object

    // 标记是否已经完成了配置 (调用了 setup 方法)
    private static volatile boolean isConfigured = false;
    // 标记文件写入器是否已经成功初始化并可用
    private static volatile boolean isWriterReady = false;


    // 用于同步写入和初始化操作
    private static final Object LOCK = new Object();

    // Private constructor to prevent instantiation
    private FileLog() {
    }

    /**
     * Configures the file logger with application context and filename.
     * This method must be called once early in the application's lifecycle,
     * typically in your Application class's onCreate().
     * The actual file writer will be initialized lazily on the first log() call.
     *
     * @param context  Application context.
     * @param filename The name of the log file (e.g., "app_log.txt").
     */
    public static void setup(Context context, String filename) {
        synchronized (LOCK) {
            if (isConfigured) {
                Log.w(TAG, "FileLog is already configured.");
                return;
            }

            if (context == null) {
                Log.e(TAG, "Context is null. FileLog setup failed.");
                return;
            }
            if (filename == null || filename.trim().isEmpty()) {
                Log.e(TAG, "Filename is null or empty. FileLog setup failed.");
                return;
            }

            appContext = context.getApplicationContext(); // 使用 Application Context
            logFilename = filename;
            isConfigured = true;

            Log.i(TAG, "FileLog configured with filename: " + logFilename);
            // Note: Writer is NOT initialized here, it happens on the first log call.
        }
    }

    /**
     * Logs a message to the file.
     * If the writer is not yet ready (first log call after setup), it attempts
     * to initialize the writer.
     * This method is thread-safe.
     *
     * @param tag     The log tag (usually the calling class name).
     * @param message The log message.
     */
    public static void log(String tag, String message) {
        // 先快速检查是否配置
        if (!isConfigured) {
            Log.e(TAG, "FileLog is not configured. Call FileLog.setup(Context, String) first. Message: [" + tag + "] " + message);
            return;
        }

        // 使用同步块来确保初始化和写入的线程安全
        synchronized (LOCK) {
            // 如果写入器尚未准备好，则尝试初始化它
            if (!isWriterReady) {
                attemptInitializeWriter();
                // 如果初始化失败，直接返回
                if (!isWriterReady) {
                    Log.e(TAG, "FileLog writer is not ready after initialization attempt. Message not logged: [" + tag + "] " + message);
                    return;
                }
            }

            // 写入器已准备好，开始写入日志
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            String timestamp = dateFormat.format(new Date());

            // 构造日志行，格式为: 时间戳 [TAG] 消息
            String logLine = timestamp + " [" + tag + "] " + message;

            try {
                writer.write(logLine);
                writer.newLine(); // 写入换行符
                writer.flush(); // 立即刷新写入器，确保日志被写入文件
                                // 注意：频繁刷新会影响性能，但能减少数据丢失风险。
            } catch (IOException e) {
                // 如果写入过程中发生错误，记录到 Logcat 并设置写入器为非就绪状态，以便下次尝试重新初始化
                Log.e(TAG, "Error writing to log file: " + (logFile != null ? logFile.getAbsolutePath() : "file is null"), e);
                isWriterReady = false; // 写入失败，标记写入器不再就绪
            }
        }
    }

    /**
     * Overloaded log method to log messages with an associated Throwable.
     * The stack trace of the throwable will be appended to the log message.
     *
     * @param tag     The log tag.
     * @param message The log message.
     * @param tr      The Throwable (e.g., Exception) to log.
     */
    public static void log(String tag, String message, Throwable tr) {
        // 将异常的堆栈信息转换为字符串，并附加到消息中
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        String stackTrace = sw.toString();
        log(tag, message + "\n" + stackTrace); // 调用原有的 log 方法
    }


    /**
     * Attempts to initialize the file writer.
     * This method is called internally by log() if the writer is not ready.
     * Assumes appContext and logFilename are already set (i.e., setup() was called).
     * Should be called within a synchronized block.
     */
    private static void attemptInitializeWriter() {
        // 再次检查，防止重复初始化
        if (isWriterReady) {
            return;
        }

        // 确保配置信息已设置
        if (appContext == null || logFilename == null || logFilename.trim().isEmpty()) {
            Log.e(TAG, "FileLog configuration missing during writer initialization attempt.");
            isConfigured = false; // 重置配置状态，可能需要重新 setup
            return;
        }

        try {
            // 获取应用外部私有文件目录
            File baseDir = appContext.getExternalFilesDir(null);

            if (baseDir == null) {
                Log.e(TAG, "External files directory is not available. FileLog writer initialization failed.");
                return;
            }

            // 确保目录存在
            if (!baseDir.exists()) {
                if (!baseDir.mkdirs()) {
                    Log.e(TAG, "Failed to create log directory: " + baseDir.getAbsolutePath());
                    return;
                }
            }

            logFile = new File(baseDir, logFilename);
            // 使用 FileWriter 的 append 模式 (true)
            writer = new BufferedWriter(new FileWriter(logFile, true));
            isWriterReady = true; // 初始化成功

            Log.i(TAG, "FileLog writer initialized successfully. Log file: " + logFile.getAbsolutePath());

        } catch (IOException e) {
            // 初始化失败，记录错误并保持写入器为非就绪状态
            Log.e(TAG, "Error initializing FileLog writer", e);
            isWriterReady = false;
            // 清理可能已部分创建的对象
            if (writer != null) {
                try { writer.close(); } catch (IOException ex) { /* ignore */ }
                writer = null;
            }
            logFile = null; // 将 logFile 也置空
        }
    }


    /**
     * Gets the log file object. Can be null if not configured or initialization failed.
     * Use this to access the log file (e.g., for sharing or debugging).
     * @return The log File object.
     */
    public static File getLogFile() {
        // 获取 logFile 时也需要同步，因为它可能在初始化过程中被设置或置空
        synchronized (LOCK) {
            return logFile;
        }
    }

    /**
     * Closes the log file writer. Optional.
     * Explicitly calling this might prevent further logging until re-initialization.
     */
    public static void close() {
        synchronized (LOCK) {
            if (writer != null) {
                try {
                    writer.close();
                    Log.i(TAG, "FileLog writer closed.");
                } catch (IOException e) {
                    Log.e(TAG, "Error closing log writer", e);
                } finally {
                    writer = null;
                    isWriterReady = false; // 关闭后标记为未就绪
                    logFile = null; // 关闭后将 logFile 也置空
                    isConfigured = false; // 关闭后重置配置状态
                    appContext = null;
                    logFilename = null;
                }
            } else {
                Log.w(TAG, "FileLog writer is already null or not initialized.");
                // 即使 writer 为 null，也要确保状态正确
                isWriterReady = false;
                isConfigured = false;
                appContext = null;
                logFilename = null;
            }
        }
    }
}