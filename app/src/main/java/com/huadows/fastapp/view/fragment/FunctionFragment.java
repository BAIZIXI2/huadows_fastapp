package com.huadows.fastapp.view.fragment;

import android.animation.ObjectAnimator;
import android.content.Intent; // 新增导入
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.huadows.fastapp.R;
import com.huadows.fastapp.view.AppDataManagerActivity; 
import com.huadows.fastapp.view.InstructionsActivity;

public class FunctionFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_function, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View buttonAppDataManagement = view.findViewById(R.id.button_app_data_management);
        View buttonUsageInstructions = view.findViewById(R.id.button_usage_instructions);

        setTouchAnimation(buttonAppDataManagement);
        setTouchAnimation(buttonUsageInstructions);

        // 为“应用数据管理”按钮设置点击事件
        buttonAppDataManagement.setOnClickListener(v -> {
            // 启动 AppDataManagerActivity
            Intent intent = new Intent(getActivity(), AppDataManagerActivity.class);
            startActivity(intent);
        });

        buttonUsageInstructions.setOnClickListener(v -> {
            // 启动使用说明界面
            Intent intent = new Intent(getActivity(), InstructionsActivity.class);
            startActivity(intent);
        });
    }

    private void setTouchAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    animateScale(v, 0.95f, 150);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    animateScale(v, 1.0f, 150);
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        v.performClick();
                    }
                    break;
            }
            return true;
        });
    }

    private void animateScale(View view, float scaleValue, long duration) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", scaleValue);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", scaleValue);
        scaleX.setDuration(duration);
        scaleY.setDuration(duration);
        scaleX.start();
        scaleY.start();
    }
}