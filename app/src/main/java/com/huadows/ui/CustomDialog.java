package com.huadows.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.huadows.fastapp.R;

public class CustomDialog extends BottomSheetDialog {

    private final Builder builder;
    private View customContentView;

    // 添加对视图的引用，以便外部可以访问
    private ProgressBar progressBarHorizontal;
    private TextView progressText;
    private TextView tvTitle;
    private TextView tvMessage;

    // --- 断点式分层缩放 ---
    private static final float COMPACT_WIDTH_DP_MAX = 359f;
    private static final float LARGE_WIDTH_DP_MIN = 600f;
    private static final float COMPACT_DIALOG_PADDING_SCALE = 0.4f;
    private static final float COMPACT_INTERNAL_SPACING_SCALE = 0.6f;
    private static final float COMPACT_CONTENT_SCALE = 0.85f;
    private static final float NORMAL_SCALE = 1.0f;
    private static final float LARGE_DIALOG_PADDING_SCALE = 1.1f;
    private static final float LARGE_INTERNAL_SPACING_SCALE = 1.1f;
    private static final float LARGE_CONTENT_SCALE = 1.2f;
    private static final int BASE_PADDING_DP = 24;
    private static final int BASE_ICON_SIZE_DP = 60;
    private static final int BASE_MARGIN_TOP_DP = 16;
    private static final int BASE_BUTTON_HEIGHT_DP = 48;
    private static final int BASE_BUTTON_SPACING_DP = 12;
    private static final int BASE_BUTTON_PADDING_HORIZONTAL_DP = 16;
    private static final int BASE_TITLE_TEXT_SIZE_SP = 20;
    private static final int BASE_MESSAGE_TEXT_SIZE_SP = 16;
    private static final int BASE_BUTTON_TEXT_SIZE_SP = 16;
    private DisplayMetrics displayMetrics;
    private float dialogPaddingScale, internalSpacingScale, contentScale;


    public static final int ALIGN_CENTER = 0;
    public static final int ALIGN_LEFT = 1;
    public static final int ALIGN_RIGHT = 2;

    @IntDef({ALIGN_CENTER, ALIGN_LEFT, ALIGN_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SingleButtonAlignment {}

    public interface OnDialogActionClickListener {
        void onConfirmClick();
        void onCancelClick();
    }

    private CustomDialog(@NonNull Context context, Builder builder) {
        super(context, R.style.AppBottomSheetDialogTheme);
        this.builder = builder;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initScaling();
        setContentView(R.layout.dialog_custom);

        final FrameLayout bottomSheet = findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            final BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            behavior.setPeekHeight(displayMetrics.heightPixels / 2);

            final ScrollView scrollView = findViewById(R.id.scroll_view);
            if (scrollView != null) {
                scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    behavior.setDraggable(scrollY == 0);
                });
            }
        }

        initViews();
    }

    private void initScaling() {
        displayMetrics = getContext().getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;

        if (screenWidthDp <= COMPACT_WIDTH_DP_MAX) {
            dialogPaddingScale = COMPACT_DIALOG_PADDING_SCALE;
            internalSpacingScale = COMPACT_INTERNAL_SPACING_SCALE;
            contentScale = COMPACT_CONTENT_SCALE;
        } else if (screenWidthDp >= LARGE_WIDTH_DP_MIN) {
            dialogPaddingScale = LARGE_DIALOG_PADDING_SCALE;
            internalSpacingScale = LARGE_INTERNAL_SPACING_SCALE;
            contentScale = LARGE_CONTENT_SCALE;
        } else {
            dialogPaddingScale = NORMAL_SCALE;
            internalSpacingScale = NORMAL_SCALE;
            contentScale = NORMAL_SCALE;
        }
    }

    private int dpToPx(float baseDp, float scale) {
        float finalDp = baseDp * scale;
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, finalDp, displayMetrics);
    }

    private float spToPx(float baseSp, float scale) {
        float finalSp = baseSp * scale;
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, finalSp, displayMetrics);
    }

    private void initViews() {
        LinearLayout dialogRootContainer = findViewById(R.id.dialog_root_container);
        ImageView ivIcon = findViewById(R.id.dialog_icon);
        this.tvTitle = findViewById(R.id.dialog_title); // 赋值给成员变量
        this.tvMessage = findViewById(R.id.dialog_message); // 赋值给成员变量
        Button btnConfirm = findViewById(R.id.btn_confirm);
        Button btnCancel = findViewById(R.id.btn_cancel);
        FrameLayout customViewContainer = findViewById(R.id.custom_view_container);
        LinearLayout buttonContainer = findViewById(R.id.button_container);
        LinearLayout progressContainer = findViewById(R.id.progress_container);
        TextView progressText = findViewById(R.id.progress_text);
        this.progressBarHorizontal = findViewById(R.id.progress_bar_horizontal);
        this.progressText = progressText;

        int horizontalPadding = dpToPx(BASE_PADDING_DP, dialogPaddingScale);
        int verticalPadding = dpToPx(BASE_PADDING_DP, internalSpacingScale);
        dialogRootContainer.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

        if (ivIcon != null) {
            ViewGroup.LayoutParams iconParams = ivIcon.getLayoutParams();
            iconParams.width = dpToPx(BASE_ICON_SIZE_DP, contentScale);
            iconParams.height = dpToPx(BASE_ICON_SIZE_DP, contentScale);
            ivIcon.setLayoutParams(iconParams);
        }
        if (tvTitle != null) {
            setMarginTop(tvTitle, BASE_MARGIN_TOP_DP, internalSpacingScale);
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, spToPx(BASE_TITLE_TEXT_SIZE_SP, contentScale));
        }
        if (tvMessage != null) {
            setMarginTop(tvMessage, BASE_MARGIN_TOP_DP / 2, internalSpacingScale);
            tvMessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, spToPx(BASE_MESSAGE_TEXT_SIZE_SP, contentScale));
        }
        if (customViewContainer != null) {
            setMarginTop(customViewContainer, BASE_MARGIN_TOP_DP, internalSpacingScale);
        }
        setMarginTop(buttonContainer, BASE_PADDING_DP, internalSpacingScale);
        updateButtonLayout(btnConfirm);
        updateButtonLayout(btnCancel);
        if (btnConfirm != null && btnCancel != null) {
            int spacing = dpToPx(BASE_BUTTON_SPACING_DP, internalSpacingScale) / 2;
            LinearLayout.LayoutParams cancelParams = (LinearLayout.LayoutParams) btnCancel.getLayoutParams();
            cancelParams.setMarginEnd(spacing);
            btnCancel.setLayoutParams(cancelParams);
            LinearLayout.LayoutParams confirmParams = (LinearLayout.LayoutParams) btnConfirm.getLayoutParams();
            confirmParams.setMarginStart(spacing);
            btnConfirm.setLayoutParams(confirmParams);
        }

        if (builder.showSpinner) {
            ProgressBar progressSpinner = findViewById(R.id.progress_spinner);
            if (progressContainer != null && progressSpinner != null) {
                progressContainer.setVisibility(View.VISIBLE);
                progressSpinner.setVisibility(View.VISIBLE);
                setMarginTop(progressContainer, BASE_MARGIN_TOP_DP, internalSpacingScale);
            }
            if (progressText != null && builder.spinnerText != null) {
                progressText.setText(builder.spinnerText);
                progressText.setTextSize(TypedValue.COMPLEX_UNIT_PX, spToPx(BASE_MESSAGE_TEXT_SIZE_SP, contentScale));
                progressText.setVisibility(View.VISIBLE);
            }
        }
        if (builder.showProgressBar) {
            if (progressBarHorizontal != null) {
                progressBarHorizontal.setVisibility(View.VISIBLE);
                setMarginTop(progressBarHorizontal, BASE_MARGIN_TOP_DP, internalSpacingScale);
            }
        }
        if (builder.icon != null) {
            ivIcon.setImageDrawable(builder.icon);
            ivIcon.setVisibility(View.VISIBLE);
        }
        if (builder.title != null) {
            tvTitle.setText(builder.title);
            tvTitle.setVisibility(View.VISIBLE);
        }
        if (builder.message != null) {
            tvMessage.setText(builder.message);
            tvMessage.setVisibility(View.VISIBLE);
        }
        if (customViewContainer != null) {
            if (builder.customView != null) {
                customViewContainer.addView(builder.customView);
                customViewContainer.setVisibility(View.VISIBLE);
                this.customContentView = builder.customView;
            } else if (builder.customViewResId != 0) {
                View inflatedView = LayoutInflater.from(getContext()).inflate(builder.customViewResId, customViewContainer, false);
                customViewContainer.addView(inflatedView);
                customViewContainer.setVisibility(View.VISIBLE);
                this.customContentView = inflatedView;
            }
        }

        setupButtons(btnConfirm, btnCancel, buttonContainer);
    }

    private void updateButtonLayout(Button button) {
        if (button == null) return;
        button.getLayoutParams().height = dpToPx(BASE_BUTTON_HEIGHT_DP, contentScale);
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, spToPx(BASE_BUTTON_TEXT_SIZE_SP, contentScale));
        int paddingHorizontal = dpToPx(BASE_BUTTON_PADDING_HORIZONTAL_DP, dialogPaddingScale);
        button.setPadding(paddingHorizontal, button.getPaddingTop(), paddingHorizontal, button.getPaddingBottom());
    }

    private void setMarginTop(View view, float topDp, float scale) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            p.topMargin = dpToPx(topDp, scale);
            view.setLayoutParams(p);
        }
    }

    private void setupButtons(Button btnConfirm, Button btnCancel, LinearLayout buttonContainer) {
        if (btnConfirm == null || btnCancel == null || buttonContainer == null) return;

        if (builder.confirmButtonText != null) btnConfirm.setText(builder.confirmButtonText);
        if (builder.cancelButtonText != null) btnCancel.setText(builder.cancelButtonText);
        btnConfirm.setVisibility(builder.confirmButtonVisible ? View.VISIBLE : View.GONE);
        btnCancel.setVisibility(builder.cancelButtonVisible ? View.VISIBLE : View.GONE);
        btnConfirm.setOnClickListener(v -> {
            if (builder.listener != null) builder.listener.onConfirmClick();
            dismiss();
        });
        btnCancel.setOnClickListener(v -> {
            if (builder.listener != null) builder.listener.onCancelClick();
            dismiss();
        });
        applyScaleAnimationOnTouch(btnConfirm);
        applyScaleAnimationOnTouch(btnCancel);
        boolean isSingleButton = builder.confirmButtonVisible ^ builder.cancelButtonVisible;
        if (isSingleButton) {
            Button visibleButton = builder.confirmButtonVisible ? btnConfirm : btnCancel;
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) visibleButton.getLayoutParams();
            params.weight = 0;
            params.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            params.setMarginStart(0);
            params.setMarginEnd(0);
            visibleButton.setLayoutParams(params);
            switch (builder.singleButtonAlignment) {
                case ALIGN_LEFT:
                    buttonContainer.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                    break;
                case ALIGN_RIGHT:
                    buttonContainer.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                    break;
                case ALIGN_CENTER:
                default:
                    buttonContainer.setGravity(Gravity.CENTER_HORIZONTAL);
                    break;
            }
        } else if (!builder.confirmButtonVisible && !builder.cancelButtonVisible) {
            buttonContainer.setVisibility(View.GONE);
        } else {
            buttonContainer.setGravity(Gravity.NO_GRAVITY);
        }
    }

    private void applyScaleAnimationOnTouch(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    public View getCustomView() {
        return customContentView;
    }
    public ProgressBar getProgressBar() {
        return this.progressBarHorizontal;
    }
    public void setProgress(int progress) {
        if (this.progressBarHorizontal != null && this.progressBarHorizontal.isShown()) {
            this.progressBarHorizontal.setProgress(progress);
        }
    }
    public void setProgressText(CharSequence text) {
        if (this.progressText != null && this.progressText.isShown()) {
            this.progressText.setText(text);
        }
    }

    public void setTitle(CharSequence title) {
        if (this.tvTitle != null) {
            this.tvTitle.setText(title);
            this.tvTitle.setVisibility(title == null || title.length() == 0 ? View.GONE : View.VISIBLE);
        }
    }

    public void setMessage(CharSequence message) {
        if (this.tvMessage != null) {
            this.tvMessage.setText(message);
            this.tvMessage.setVisibility(message == null || message.length() == 0 ? View.GONE : View.VISIBLE);
        }
    }

    public static class Builder {
        private final Context context;
        private Drawable icon;
        private CharSequence title;
        private CharSequence message;
        private CharSequence confirmButtonText = "确认";
        private CharSequence cancelButtonText = "取消";
        private OnDialogActionClickListener listener;
        private View customView;
        private @LayoutRes int customViewResId = 0;
        private boolean confirmButtonVisible = true;
        private boolean cancelButtonVisible = true;
        private @SingleButtonAlignment int singleButtonAlignment = ALIGN_CENTER;
        private boolean showSpinner = false;
        private boolean showProgressBar = false;
        private CharSequence spinnerText;

        public Builder(Context context) { this.context = context; }
        public Builder setIcon(Drawable icon) { this.icon = icon; return this; }
        public Builder setIcon(@DrawableRes int iconRes) { this.icon = ContextCompat.getDrawable(context, iconRes); return this; }
        public Builder setTitle(CharSequence title) { this.title = title; return this; }
        public Builder setTitle(@StringRes int titleRes) { this.title = context.getString(titleRes); return this; }
        public Builder setMessage(CharSequence message) { this.message = message; return this; }
        public Builder setMessage(@StringRes int messageRes) { this.message = context.getString(messageRes); return this; }
        public Builder setView(View view) { this.customView = view; this.customViewResId = 0; return this; }
        public Builder setView(@LayoutRes int layoutResId) { this.customView = null; this.customViewResId = layoutResId; return this; }
        public Builder setConfirmButton(CharSequence text) { this.confirmButtonText = text; return this; }
        public Builder setConfirmButton(@StringRes int textRes) { this.confirmButtonText = context.getString(textRes); return this; }
        public Builder setCancelButton(CharSequence text) { this.cancelButtonText = text; return this; }
        public Builder setCancelButton(@StringRes int textRes) { this.cancelButtonText = context.getString(textRes); return this; }
        public Builder setConfirmButtonVisible(boolean visible) { this.confirmButtonVisible = visible; return this; }
        public Builder setCancelButtonVisible(boolean visible) { this.cancelButtonVisible = visible; return this; }
        public Builder setSingleButtonAlignment(@SingleButtonAlignment int alignment) { this.singleButtonAlignment = alignment; return this; }
        public Builder setListener(OnDialogActionClickListener listener) { this.listener = listener; return this; }
        public Builder showSpinner(boolean show, @StringRes int textResId) { return showSpinner(show, context.getString(textResId)); }
        public Builder showSpinner(boolean show, CharSequence text) { this.showSpinner = show; this.spinnerText = text; if (show) { this.showProgressBar = false; } return this; }
        public Builder showProgressBar(boolean show) { this.showProgressBar = show; if (show) { this.showSpinner = false; } return this; }
        public CustomDialog build() { return new CustomDialog(context, this); }

        public CustomDialog show() {
            // ====================== 关键修复点 ======================
            // 在显示对话框前，检查其关联的Context（通常是Activity）是否仍然有效
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                if (activity.isFinishing() || activity.isDestroyed()) {
                    // Activity 已经或正在关闭，此时显示对话框会崩溃，所以直接返回 null
                    return null;
                }
            }
            // =======================================================
            CustomDialog dialog = build();
            dialog.show();
            return dialog;
        }
    }
}