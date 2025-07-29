package com.huadows.fastappdebug;
// ErrorActivity.java

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ErrorActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String errorInfo = getIntent().getStringExtra("error");

        ScrollView scrollView = new ScrollView(this);
        scrollView.setPadding(30, 50, 30, 30);

        TextView textView = new TextView(this);
        textView.setText(errorInfo);
        textView.setTextSize(14);
        textView.setTextIsSelectable(true);

        Button copyButton = new Button(this);
        copyButton.setText("复制错误信息");
        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Crash Info", errorInfo);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
        });

        // 布局组合
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(copyButton);
        layout.addView(textView);
        scrollView.addView(layout);

        setContentView(scrollView);
    }
}
