package com.huadows.ui;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.huadows.fastapp.R;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 增强版自定义 Toast（队列版）：
 * - 支持队列，新消息会排队等待，在前一条消息消失后显示。
 * - 滑入/滑出 + 弹性、加速、缩放动画。
 * - 宽度动态 wrap-content，但最大到屏幕边缘。
 * - 最多两行文字，超出省略。
 * - 可选文字 & 图标颜色。
 */
public class CustomToast {

    public enum Level {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    // 使用静态队列和锁来管理 Toast 请求
    private static final Queue<ToastRequest> toastQueue = new LinkedList<>();
    private static boolean isShowing = false;
    private static final Object lock = new Object();

    /**
     * 内部类，用于封装一次 Toast 请求的所有信息
     */
    private static class ToastRequest {
        Activity activity;
        String message;
        long stayMs;
        Level level;
        @Nullable @ColorInt Integer textColor;

        ToastRequest(Activity activity, String message, long stayMs, Level level, @Nullable @ColorInt Integer textColor) {
            this.activity = activity;
            this.message = message;
            this.stayMs = stayMs;
            this.level = level;
            this.textColor = textColor;
        }
    }
    
    /**
     * 最简调用：不指定颜色
     */
    public static void show(Activity activity, String message, long stayMs, Level level) {
        show(activity, message, stayMs, level, null);
    }

    /**
     * 完整调用：可以指定文字和图标颜色。
     * 此方法将请求加入队列，并触发处理流程。
     */
    public static void show(Activity activity, String message, long stayMs, Level level, @Nullable @ColorInt Integer textColor) {
        synchronized (lock) {
            // 1. 创建请求并加入队列
            toastQueue.add(new ToastRequest(activity, message, stayMs, level, textColor));
            
            // 2. 如果当前没有正在显示的 Toast，则开始处理队列
            if (!isShowing) {
                processNextToast();
            }
        }
    }

    /**
     * 处理队列中的下一个 Toast 请求
     */
    private static void processNextToast() {
        synchronized (lock) {
            // 如果队列为空，或正在显示，则直接返回
            if (toastQueue.isEmpty() || isShowing) {
                return;
            }

            // 取出队首的请求
            ToastRequest request = toastQueue.poll();
            if (request == null) {
                return; // 理论上不会发生，因为前面有 isEmpty 判断
            }

            // 检查 Activity 是否有效
            if (request.activity == null || request.activity.isFinishing() || request.activity.isDestroyed()) {
                // 如果 Activity 无效，则跳过此请求，并尝试处理下一个
                processNextToast();
                return;
            }

            // 标记为正在显示，并执行真正的显示逻辑
            isShowing = true;
            displayToast(request);
        }
    }

    /**
     * 真正执行 Toast 显示和动画的方法
     * @param request 包含所有 Toast 信息的请求对象
     */
    private static void displayToast(ToastRequest request) {
        // 确保在UI线程执行
        new Handler(Looper.getMainLooper()).post(() -> {
            FrameLayout root = request.activity.findViewById(android.R.id.content);
            View toastView = LayoutInflater.from(request.activity)
                    .inflate(R.layout.custom_toast, root, false);

            // 1. 计算屏幕宽度
            DisplayMetrics dm = request.activity.getResources().getDisplayMetrics();
            int screenWidth = dm.widthPixels;

            // 2. 设置文字
            TextView tv = toastView.findViewById(R.id.toast_text);
            tv.setText(request.message);
            tv.setMaxLines(2);
            tv.setEllipsize(TextUtils.TruncateAt.END);
            int padPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, dm);
            int iconSpace = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24 + 8, dm);
            tv.setMaxWidth(screenWidth - padPx * 2 - iconSpace);

            // 3. 设置图标
            ImageView iv = toastView.findViewById(R.id.toast_icon);
            switch (request.level) {
                case INFO:
                    iv.setImageResource(R.drawable.ic_tost_info);
                    break;
                case SUCCESS:
                    iv.setImageResource(R.drawable.ic_tost_success);
                    break;
                case WARNING:
                    iv.setImageResource(R.drawable.ic_tost_warning);
                    break;
                case ERROR:
                    iv.setImageResource(R.drawable.ic_tost_error);
                    break;
            }

            // 4. 设置颜色
            if (request.textColor != null) {
                tv.setTextColor(request.textColor);
                iv.setColorFilter(request.textColor, PorterDuff.Mode.SRC_IN);
            }

            // 5. 初始状态
            toastView.setAlpha(0f);
            toastView.setScaleX(0.8f);
            toastView.setScaleY(0.8f);

            // 6. 添加到根布局
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.CENTER_HORIZONTAL
            );
            // 顶部增加一个 margin，避免紧贴状态栏
            lp.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, dm);
            root.addView(toastView, lp);

            // 7. 动画流程
            toastView.post(() -> {
                int h = toastView.getHeight();
                toastView.setTranslationY(-h);

                toastView.animate()
                        .translationY(0)
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setInterpolator(new OvershootInterpolator(1.2f))
                        .setDuration(500)
                        .withEndAction(() -> {
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                toastView.animate()
                                        .translationY(-h)
                                        .alpha(0f)
                                        .scaleX(0.8f)
                                        .scaleY(0.8f)
                                        .setInterpolator(new AccelerateInterpolator())
                                        .setDuration(300)
                                        .withEndAction(() -> {
                                            root.removeView(toastView);
                                            // 关键步骤：动画结束后，释放锁并处理下一个
                                            synchronized (lock) {
                                                isShowing = false;
                                                processNextToast();
                                            }
                                        })
                                        .start();
                            }, request.stayMs);
                        })
                        .start();
            });
        });
    }
}