package com.app.myapp;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.RectF;
import java.util.HashMap;
import java.util.Map;

public class GridMapView extends View {

    private Paint gridPaint;
    private int gridCols = 100; // 固定分成40列
    private int gridRows = 100; // 固定分成40行
    private float cellWidth = 4; // 每個格子的寬度
    private float cellHeight = 4; // 每個格子的高度
    private Matrix transformMatrix = new Matrix();
    private Bitmap[][] gridImages; // 用來存放每個格子的圖片
    private boolean showGrid = true; // 是否顯示網格線
    private Map<Integer, Float> scaleMap = new HashMap<>(); // 存放特定格子的縮放比例
    private Map<Integer, Float> rotationMap = new HashMap<>(); // 存放特定格子的旋轉角度
    private Map<Integer, Integer> alphaMap = new HashMap<>(); // 存放特定格子的透明度 (0~255)
    private Paint imagePaint = new Paint(); // 專門處理圖片的畫筆


    public GridMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint = new Paint();
        gridPaint.setColor(0xFF000000); // 黑色網格
        gridPaint.setStrokeWidth(1f);
        gridPaint.setStyle(Paint.Style.STROKE);

        // 初始化存放圖片的陣列
        gridImages = new Bitmap[gridCols][gridRows];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 套用縮放與平移矩陣
        canvas.save();
        canvas.concat(transformMatrix);

        int width = getWidth();
        int height = getHeight();


        // 繪製每個格子的圖片（如果有）
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                if (gridImages[col][row] != null) {
                    // 計算圖片的繪製範圍
                    /*
                    float left = col * cellWidth;
                    float top = row * cellHeight;
                    float right = left + cellWidth;
                    float bottom = top + cellHeight;*/
                    float scale = scaleMap.getOrDefault(row * gridCols + col, 1.0f); // 預設縮放 1.0
                    float rotation = rotationMap.getOrDefault(row * gridCols + col, 0.0f); // 預設旋轉 0°

                    //int alpha = alphaMap.getOrDefault(row * gridCols + col, 255); // 預設不透明 (255)
                    //imagePaint.setAlpha(alpha);

                    float scaledWidth = cellWidth * scale;
                    float scaledHeight = cellHeight * scale;

                    // 計算縮放後的圖片繪製範圍
                    float centerX = col * cellWidth + cellWidth / 2;
                    float centerY = row * cellHeight + cellHeight / 2;
                    float left = centerX - scaledWidth / 2;
                    float top = centerY - scaledHeight / 2;
                    float right = centerX + scaledWidth / 2;
                    float bottom = centerY + scaledHeight / 2;

                    Rect dstRect = new Rect((int) left, (int) top, (int) right, (int) bottom);
                    canvas.save(); // 儲存當前畫布狀態
                    canvas.rotate(rotation, centerX, centerY); // 以格子的中心旋轉
                    canvas.drawBitmap(gridImages[col][row], null, dstRect, null);
                    canvas.restore(); // 恢復畫布狀態，避免影響其他格子
                }
            }
        }

        // 繪製網格線（如果開啟）
        if (showGrid) {
            for (int i = 0; i <= gridRows; i++) {
                canvas.drawLine(0, i * cellHeight, width, i * cellHeight, gridPaint);
            }
            for (int j = 0; j <= gridCols; j++) {
                canvas.drawLine(j * cellWidth, 0, j * cellWidth, height, gridPaint);
            }
        }

        /* 畫水平線
        for (int i = 0; i <= gridRows; i++) {
            //canvas.drawLine(0, i * cellHeight, width, i * cellHeight, gridPaint);
        }
        for (int j = 0; j <= gridCols; j++) {
            //canvas.drawLine(j * cellWidth, 0, j * cellWidth, height, gridPaint);
        }*/

        canvas.restore();
    }

    // 設定 GridMap 的大小 (與圖片相同)
    public void setGridSize(int imgWidth, int imgHeight) {
        this.cellWidth = (float) imgWidth / gridCols;
        this.cellHeight = (float) imgHeight / gridRows;
        requestLayout();
        invalidate();
    }

    // 設定與 ImageView 相同的 Matrix 變換
    public void setTransformMatrix(Matrix matrix) {
        this.transformMatrix.set(matrix);
        invalidate();
    }

    // 設定特定格子的圖片
    public void setCellImage(int col, int row, Bitmap bitmap) {
        if (col >= 0 && col < gridCols && row >= 0 && row < gridRows) {
            gridImages[col][row] = bitmap;
            invalidate(); // 重新繪製
        }
    }

    // 設定網格線的可見度
    public void setGridVisibility(boolean visible) {
        this.showGrid = visible;
        invalidate(); // 重新繪製
    }

    // 設定特定格子的縮放比例
    public void setCellScale(int col, int row, float scale) {
        if (col >= 0 && col < gridCols && row >= 0 && row < gridRows) {
            scaleMap.put(row * gridCols + col, scale);
            invalidate();
        }
    }

    // 設定特定格子的旋轉角度
    public void setCellRotation(int col, int row, float angle) {
        if (col >= 0 && col < gridCols && row >= 0 && row < gridRows) {
            rotationMap.put(row * gridCols + col, angle);
            invalidate();
        }
    }
}