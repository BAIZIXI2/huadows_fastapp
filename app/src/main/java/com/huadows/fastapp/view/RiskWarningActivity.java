// 文件路径: app/src/main/java/com/huadows/fastapp/view/RiskWarningActivity.java
package com.huadows.fastapp.view;

import android.annotation.SuppressLint;
// 移除：不再需要 Context 和 SharedPreferences 的导入，因为逻辑已移至App类
// import android.content.Context;
// import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.huadows.fastapp.App; // 导入App类来调用其静态方法
import com.huadows.fastapp.R;

public class RiskWarningActivity extends AppCompatActivity {
    // 移除：不再需要这个成员变量
    // protected Context ctx;
    private Button buttonConfirm;
    private Button buttonCancel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_risk_warning);

        buttonConfirm = findViewById(R.id.button_confirm);
        buttonCancel = findViewById(R.id.button_cancel);
        
        // 移除：不再需要初始化 ctx
        // ctx = this.getApplicationContext();

        // 设置点击事件
        buttonConfirm.setOnClickListener(v -> {
            // 修正：直接调用App类的静态方法来保存状态
            App.setAgreedToRiskWarning(true); // 用户同意
            setResult(RESULT_OK); // 设置结果为OK
            finish(); // 关闭当前Activity
        });

        buttonCancel.setOnClickListener(v -> {
            // 修正：直接调用App类的静态方法来保存状态
            App.setAgreedToRiskWarning(false); // 用户取消
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
    }

}
