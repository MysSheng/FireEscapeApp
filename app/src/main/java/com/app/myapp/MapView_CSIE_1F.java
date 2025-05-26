package com.app.myapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

public class MapView_CSIE_1F extends View {
    private Paint paint;
    private Bitmap mapBitmap;
    private float userX , userY;

    // 設置觸控螢幕
    private float offsetX = 0, offsetY = 0;
    private float lastTouchX, lastTouchY;

    // 原圖片大小
    private final int originalMapWidth = 800; // ~= 57m
    private final int originalMapHeight = 800; // ~= 57m

    private int viewWidth, viewHeight;

    public MapView_CSIE_1F(Context context) {
        super(context);
        init();
    }

    public MapView_CSIE_1F(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED); // 設定標記顏色
        paint.setStyle(Paint.Style.FILL);
        mapBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.map_csie_1f);

        // 設定起點
        userX = (3 / 57f) * mapBitmap.getHeight();
        userY = (5 / 57f) * mapBitmap.getHeight();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        this.viewWidth = w;
        this.viewHeight = h;

        // 取得螢幕DPI
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density;

        // 計算 dp 對應的 px 大小
        int mapWidthPx = (int) (originalMapWidth * density);
        int mapHeightPx = (int) (originalMapHeight * density);

        // 縮放 Bitmap 以適應畫面
        mapBitmap = Bitmap.createScaledBitmap(mapBitmap, mapWidthPx, mapHeightPx, true);

//        // 設定 View 的 LayoutParams (讓地圖符合螢幕大小)
//        ViewGroup.LayoutParams params = getLayoutParams();
//        params.width = mapWidthPx;
//        params.height = mapHeightPx;
//        setLayoutParams(params);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 繪製地圖
        canvas.drawBitmap(mapBitmap, offsetX, offsetY, null);
        // 繪製使用者位置
        canvas.drawCircle(userX + offsetX, userY + offsetY, 20, paint);
    }

    public void updateUserPosition(float x, float y) {
        // 轉換成像素位置
        userX = (x / 57f) * mapBitmap.getHeight();
        userY = (y / 57f) * mapBitmap.getHeight();
        invalidate(); // 重新繪製
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - lastTouchX;
                float dy = event.getY() - lastTouchY;
                offsetX += dx;
                offsetY += dy;

                // 設定邊界 (確保地圖不超過範圍)
                offsetX = Math.max(Math.min(offsetX, 0), -mapBitmap.getWidth() + viewWidth);
                offsetY = Math.max(Math.min(offsetY, 0), -mapBitmap.getHeight() + viewHeight);

                lastTouchX = event.getX();
                lastTouchY = event.getY();
                invalidate(); // 重新繪製地圖
                return true;
        }
        return super.onTouchEvent(event);
    }

}
