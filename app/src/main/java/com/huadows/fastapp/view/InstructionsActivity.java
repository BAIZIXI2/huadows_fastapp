package com.huadows.fastapp.view;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.huadows.fastapp.R;

public class InstructionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置Activity的布局文件
        setContentView(R.layout.activity_instructions);
        
        // 初始化返回按钮并设置点击监听器
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 调用finish()方法来关闭当前的Activity，返回到上一个页面
                finish();
            }
        });
    }
}