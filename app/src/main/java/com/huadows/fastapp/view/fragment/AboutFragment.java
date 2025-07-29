package com.huadows.fastapp.view.fragment;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.huadows.fastapp.R;

public class AboutFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView appIcon = view.findViewById(R.id.about_app_icon);
        TextView appName = view.findViewById(R.id.about_app_name);
        TextView appVersion = view.findViewById(R.id.about_app_version);

        try {
            PackageManager pm = requireActivity().getPackageManager();
            String packageName = requireActivity().getPackageName();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);

            appIcon.setImageDrawable(pm.getApplicationIcon(packageName));
            appName.setText(pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)));
            
            long versionCode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = packageInfo.getLongVersionCode();
            } else {
                versionCode = packageInfo.versionCode;
            }

            String versionString = "版本 " + packageInfo.versionName + " (" + versionCode + ")";
            appVersion.setText(versionString);
            
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            // 设置默认值以防出错
            appName.setText("快应用");
            appVersion.setText("版本未知");
        }
    }
}