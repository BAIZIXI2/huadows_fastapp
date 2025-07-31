package com.huadows.fastapp.view;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.huadows.fastapp.R;

public class ErrorActivity extends AppCompatActivity {

    public static final String EXTRA_ERROR_TEXT = "extra_error_text";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置新的布局文件
        setContentView(R.layout.activity_error);

        // 获取从 Intent 传递过来的错误信息
        final String errorText = getIntent().getStringExtra(EXTRA_ERROR_TEXT);

        // 初始化视图
        TextView errorTextView = findViewById(R.id.error_text);
        Button copyButton = findViewById(R.id.button_copy_log);
        Button exitButton = findViewById(R.id.button_force_exit);

        // 显示错误信息
        errorTextView.setText(errorText);

        // 设置“复制日志”按钮的点击事件
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("error_log", errorText);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(ErrorActivity.this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 设置“强制退出”按钮的点击事件
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 结束当前 Activity
                finishAffinity();
                // 杀死当前应用的所有进程
                Process.killProcess(Process.myPid());
                // 确保彻底退出
                System.exit(1);
            }
        });
    }

    @Override
    public void onBackPressed() {
        // 禁用返回键，让用户必须通过按钮退出
        // 如果需要允许返回，可以删除此方法
    }
}