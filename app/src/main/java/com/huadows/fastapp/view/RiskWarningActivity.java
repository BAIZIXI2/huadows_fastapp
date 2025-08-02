package com.huadows.fastapp.view;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.huadows.fastapp.App; // 导入App类来设置SharedPreferences
import com.huadows.fastapp.R;

public class RiskWarningActivity extends AppCompatActivity {

    private Button buttonConfirm;
    private Button buttonCancel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_risk_warning);

        buttonConfirm = findViewById(R.id.button_confirm);
        buttonCancel = findViewById(R.id.button_cancel);

        // 设置点击事件
        buttonConfirm.setOnClickListener(v -> {
            setAgreedToRiskWarning(true); // 用户同意
            setResult(RESULT_OK); // 设置结果为OK
            finish(); // 关闭当前Activity
        });

        buttonCancel.setOnClickListener(v -> {
            setAgreedToRiskWarning(false); // 用户取消
            setResult(RESULT_CANCELED); // 设置结果为CANCELED
            finish(); // 关闭当前Activity
        });

        // 为按钮设置触摸动画
        setTouchAnimation(buttonConfirm);
        setTouchAnimation(buttonCancel);
    }

    /**
     * 为视图设置按下缩小、松开恢复的动画
     * @param view 需要设置动画的视图
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setTouchAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 按下时，缩小视图
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // 松开或取消时，恢复原始大小
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            // 返回 false，让 onClickListener 能够继续接收到事件
            return false;
        });
    }

    @Override
    public void onBackPressed() {
        // 禁用返回键，强制用户选择同意或取消
        // 如果需要允许返回键退出，可以移除此方法或调用 super.onBackPressed();
        // super.onBackPressed(); // uncomment this line to allow back button
    }

    public static boolean hasAgreedToRiskWarning() {
        SharedPreferences prefs = sContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_AGREE_RISK, false);
    }

    public static void setAgreedToRiskWarning(boolean agreed) {
        SharedPreferences prefs = sContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_AGREE_RISK, agreed);
        editor.apply();
    
}