package com.huadows.fastapp.view.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class FloatingPageIndicator extends View {
    private int mPageCount = 3; // 页面数量，可动态设置
    private int mCurrentPage = 0; // 当前选中页面索引

    // 调整后的 dp 单位的默认值：较小的圆点间隔和半径
    private float mDotSpacingDp = 15f; // dp 单位的圆点间隔
    private float mDotRadiusDp = 5f;  // dp 单位的圆点半径

    // 实际的像素值，将在构造函数中转换
    private float mDotSpacing;
    private float mDotRadius;

    private Paint mDotPaint;
    private Paint mIndicatorPaint;
    private float mAnimatedX; // 动画中当前圆心X坐标

    public FloatingPageIndicator(Context context) {
        super(context);
        init();
    }

    public FloatingPageIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FloatingPageIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        // 将 dp 转换为实际像素值
        mDotSpacing = dpToPx(mDotSpacingDp);
        mDotRadius = dpToPx(mDotRadiusDp);

        // 静态圆点（半透明白色）
        mDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDotPaint.setColor(0x80FFFFFF);

        // 当前指示圆点（纯白色）
        mIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mIndicatorPaint.setColor(0xFFFFFFFF);

        // 初始动画位置为第一个圆点的位置，待 onMeasure 后初始化
        mAnimatedX = -1;

        int paddingBottom = (int) dpToPx(4f);
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), paddingBottom);
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = (int)((mPageCount - 1) * mDotSpacing + mDotRadius * 2 + getPaddingLeft() + getPaddingRight());
        int height = (int)(mDotRadius * 2 + getPaddingTop() + getPaddingBottom());
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float startX = getPaddingLeft() + mDotRadius;
        float centerY = getPaddingTop() + mDotRadius;

        for (int i = 0; i < mPageCount; i++){
            float x = startX + i * mDotSpacing;
            canvas.drawCircle(x, centerY, mDotRadius, mDotPaint);
        }

        if (mAnimatedX < 0) {
            mAnimatedX = startX + mCurrentPage * mDotSpacing;
        }
        canvas.drawCircle(mAnimatedX, centerY, mDotRadius, mIndicatorPaint);
    }

    public void setPageCount(int count) {
        this.mPageCount = count;
        requestLayout();
        invalidate();
    }

    public void setCurrentPage(int currentPage) {
        if(currentPage < 0 || currentPage >= mPageCount){
            return;
        }
        float startX = getPaddingLeft() + mDotRadius + mCurrentPage * mDotSpacing;
        final float targetX = getPaddingLeft() + mDotRadius + currentPage * mDotSpacing;
        ValueAnimator animator = ValueAnimator.ofFloat(startX, targetX);
        animator.setDuration(300);
        animator.setInterpolator(new OvershootInterpolator());
        animator.addUpdateListener(animation -> {
            mAnimatedX = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
        mCurrentPage = currentPage;
    }
}