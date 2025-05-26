package com.app.myapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class JoystickView extends View {
    private Paint basePaint, knobPaint;
    private float centerX, centerY, baseRadius, knobRadius;
    private PointF knobPosition = new PointF();
    private OnMoveListener moveListener;

    public interface OnMoveListener {
        void onMove(float dx, float dy);
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        basePaint.setColor(Color.DKGRAY);
        knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        knobPaint.setColor(Color.LTGRAY);
    }

    public void setOnMoveListener(OnMoveListener listener) {
        this.moveListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        centerX = w / 2f;
        centerY = h / 2f;
        baseRadius = Math.min(w, h) / 2f * 0.7f;
        knobRadius = baseRadius / 2f;
        knobPosition.set(centerX, centerY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint);
        canvas.drawCircle(knobPosition.x, knobPosition.y, knobRadius, knobPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float dx = event.getX() - centerX;
        float dy = event.getY() - centerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance > 0.8 * baseRadius) {
            dx *= 0.8 * baseRadius / distance;
            dy *= 0.8 * baseRadius / distance;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_DOWN:
                knobPosition.set(centerX + dx, centerY + dy);
                if (moveListener != null) moveListener.onMove(dx / baseRadius, dy / baseRadius);
                break;
            case MotionEvent.ACTION_UP:
                knobPosition.set(centerX, centerY);
                if (moveListener != null) moveListener.onMove(0, 0);
                break;
        }

        invalidate();

        return true;
    }

}
