package com.hechuangwu.opensourseframe;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by cwh on 2019/9/5 0005.
 * 功能:
 */
public class CustomerView extends View {
    public CustomerView(Context context) {
        super( context );
    }

    public CustomerView(Context context, AttributeSet attrs) {
        super( context, attrs );
    }

    public CustomerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super( context, attrs, defStyleAttr );
    }
    float progress = 0;


    // 创建 getter 方法
    public float getProgress() {
        return progress;
    }

    // 创建 setter 方法
    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();
    }


    public static float number = 1;
    public static boolean down;
    public  void setNumber(float number) {
        lastStop = stopY;
        animal();
    }

    float stopY;
    float lastStop;
    float startY = 300;
    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint =  new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor( Color.RED);
        paint.setStrokeWidth(20);

        if(down){
            stopY = lastStop-number*fraction*100;
        }else {
            stopY = startY-number*fraction*100;
        }
        canvas.drawLine(100,startY,100,stopY,paint);
    }

    private float fraction;
    public void animal(){
        ValueAnimator animator = ValueAnimator.ofFloat(0,1.0f);
        animator.setDuration(1000);
        animator.start();
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                fraction = animation.getAnimatedFraction();
                postInvalidate();
            }
        });
    }
}
