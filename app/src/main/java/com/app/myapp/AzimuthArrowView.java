package com.app.myapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class AzimuthArrowView extends View {
    private float azimuth = 0f;
    private final Paint paint = new Paint();
    private final Path arrowPath = new Path();

    public AzimuthArrowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(0xFF0077CC);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(4);
        paint.setAntiAlias(true);
    }

    public void setAzimuth(float azimuth) {
        this.azimuth = azimuth;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        float arrowLength = Math.min(centerX, centerY) * 0.8f;

        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.rotate(azimuth);

        arrowPath.reset();
        arrowPath.moveTo(0, -arrowLength);
        arrowPath.lineTo(-arrowLength / 2, arrowLength / 4);
        arrowPath.lineTo(-arrowLength / 4, arrowLength / 4);
        arrowPath.lineTo(0, arrowLength / 2);
        arrowPath.lineTo(arrowLength / 4, arrowLength / 4);
        arrowPath.lineTo(arrowLength / 2, arrowLength / 4);

//        arrowPath.lineTo(arrowLength / 4, arrowLength / 3);
        arrowPath.close();

        canvas.drawPath(arrowPath, paint);
        canvas.restore();
    }
}
