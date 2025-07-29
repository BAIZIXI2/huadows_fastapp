package com.huadows.fastapp.adapter;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.huadows.fastapp.R;
import com.huadows.fastapp.bean.AppDataInfo;
import java.util.ArrayList;
import java.util.List;

public class AppDataAdapter extends RecyclerView.Adapter<AppDataAdapter.ViewHolder> {

    private final List<AppDataInfo> mApps;
    private OnSelectionChangedListener mListener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    public AppDataAdapter(List<AppDataInfo> apps, OnSelectionChangedListener listener) {
        this.mApps = apps;
        this.mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_data, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppDataInfo app = mApps.get(position);
        holder.icon.setImageDrawable(app.icon);
        holder.name.setText(app.appName);
        holder.packageName.setText(app.packageName);

        // 设置触摸动画
        setTouchAnimation(holder.itemView);

        // 设置 CheckBox 的状态，同时避免触发 onCheckedChangeListener
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(app.isSelected);
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            app.isSelected = isChecked;
            notifySelectionChanged();
        });

        holder.itemView.setOnClickListener(v -> {
            holder.checkBox.toggle();
        });
    }

    private void notifySelectionChanged() {
        if (mListener != null) {
            mListener.onSelectionChanged(getSelectedApps().size());
        }
    }

    public List<AppDataInfo> getSelectedApps() {
        List<AppDataInfo> selected = new ArrayList<>();
        for (AppDataInfo app : mApps) {
            if (app.isSelected) {
                selected.add(app);
            }
        }
        return selected;
    }

    @Override
    public int getItemCount() {
        return mApps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        TextView packageName;
        CheckBox checkBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
            packageName = itemView.findViewById(R.id.package_name);
            checkBox = itemView.findViewById(R.id.checkbox);
        }
    }

    /**
     * 为视图设置按下缩小、松开恢复的动画
     * @param view 需要设置动画的视图
     */
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
            // 返回 false，让 onClickListener / onLongClickListener 能够继续接收到事件
            return false;
        });
    }
}