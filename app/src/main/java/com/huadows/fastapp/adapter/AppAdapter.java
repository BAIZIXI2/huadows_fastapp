package com.huadows.fastapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.MotionEvent; // 新增导入

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.huadows.fastapp.R;
import com.huadows.fastapp.bean.AppInfo;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private final List<AppInfo> mApps;
    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;

    public interface OnItemClickListener {
        void onItemClick(AppInfo appInfo);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(AppInfo appInfo);
    }

    public AppAdapter(List<AppInfo> apps) {
        this.mApps = apps;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.mOnItemLongClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = mApps.get(position);
        holder.icon.setImageDrawable(app.icon);
        holder.name.setText(app.appName);
        holder.packageName.setText(app.packageName);

        // 为每个列表项设置触摸动画
        setTouchAnimation(holder.itemView);

        holder.itemView.setOnClickListener(v -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(app);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (mOnItemLongClickListener != null) {
                return mOnItemLongClickListener.onItemLongClick(app);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return mApps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        TextView packageName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
            packageName = itemView.findViewById(R.id.package_name);
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