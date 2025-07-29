package com.huadows.fastapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.huadows.fastapp.App;
import com.huadows.fastapp.R;
import com.huadows.fastapp.adapter.SectionsPagerAdapter;
import com.huadows.fastapp.view.fragment.AboutFragment;
import com.huadows.fastapp.view.fragment.FunctionFragment;
import com.huadows.fastapp.view.fragment.HomeFragment;
import com.huadows.fastapp.view.widget.FloatingPageIndicator;

public class MainActivity extends AppCompatActivity {

    private ViewPager mViewPager;
    private FloatingPageIndicator mIndicator;
    private View mRootLayout;

    // 用于处理风险警告页面的结果
    private ActivityResultLauncher<Intent> mRiskWarningLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 必须先注册启动器
        setupActivityResultLaunchers();

        // 检查是否同意风险警告
        if (!App.hasAgreedToRiskWarning()) {
            Intent intent = new Intent(this, RiskWarningActivity.class);
            mRiskWarningLauncher.launch(intent);
        } else {
            // 如果已同意，则直接初始化主界面内容
            initViewPager();
        }
    }

    private void setupActivityResultLaunchers() {
        mRiskWarningLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && App.hasAgreedToRiskWarning()) {
                initViewPager();
            } else {
                // 用户不同意风险协议，则退出应用
                finish();
            }
        });
    }

    private void initViewPager() {
        mRootLayout = findViewById(R.id.main_root_layout);
        mViewPager = findViewById(R.id.view_pager);
        mIndicator = findViewById(R.id.indicator);

        // 确保主布局可见（如果初始是隐藏的话）
        mRootLayout.setVisibility(View.VISIBLE);

        // 设置ViewPager适配器
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new HomeFragment(), "首页");
        adapter.addFragment(new FunctionFragment(), "功能");
        adapter.addFragment(new AboutFragment(), "关于");
        mViewPager.setAdapter(adapter);

        // 关联指示器
        mIndicator.setPageCount(adapter.getCount());

        // 监听ViewPager页面切换
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mIndicator.setCurrentPage(position);
            }
        });

        // 默认显示第一页（首页）
        mViewPager.setCurrentItem(0);
        mIndicator.setCurrentPage(0);
    }
}