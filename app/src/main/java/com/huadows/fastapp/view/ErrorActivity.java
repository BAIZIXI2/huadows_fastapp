// /app/src/main/java/com/huadows/fastapp/view/ErrorActivity.java
package com.huadows.fastapp.view;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class ErrorActivity extends Activity {

    public static final String EXTRA_ERROR_TEXT = "extra_error_text";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String errorText = getIntent().getStringExtra(EXTRA_ERROR_TEXT);

        // 创建根布局 LinearLayout
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        rootLayout.setPadding(32, 32, 32, 32);

        // 创建标题
        TextView title = new TextView(this);
        title.setText("抱歉，程序出现异常");
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        rootLayout.addView(title);

        // 创建复制按钮
        Button copyButton = new Button(this);
        copyButton.setText("复制错误信息");
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        buttonParams.gravity = Gravity.CENTER_HORIZONTAL;
        buttonParams.setMargins(0, 20, 0, 20);
        copyButton.setLayoutParams(buttonParams);
        rootLayout.addView(copyButton);

        // 创建滚动视图
        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0
        );
        scrollParams.weight = 1;
        scrollView.setLayoutParams(scrollParams);
        rootLayout.addView(scrollView);

        // 创建显示错误信息的TextView
        TextView errorTextView = new TextView(this);
        errorTextView.setText(errorText);
        errorTextView.setTextIsSelectable(true);
        scrollView.addView(errorTextView);

        // 设置复制按钮的点击事件
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("error_log", errorText);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(ErrorActivity.this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
        });

        // 将根布局设置为Activity的内容视图
        setContentView(rootLayout);
    }
}