package com.app.myapp;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class GridMapView extends FrameLayout {

    private int gridCols = 100;
    private int gridRows = 100;
    private float cellWidth = 4;
    private float cellHeight = 4;
    private ImageView[][] gridImageViews;
    private Matrix transformMatrix = new Matrix();

    public GridMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridImageViews = new ImageView[gridCols][gridRows];
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                ImageView imageView = new ImageView(getContext());
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);

                // 初始 LayoutParams
                LayoutParams params = new LayoutParams(
                        (int) cellWidth, (int) cellHeight
                );
                params.leftMargin = (int)(col * cellWidth);
                params.topMargin = (int)(row * cellHeight);

                imageView.setLayoutParams(params);
                gridImageViews[col][row] = imageView;
                addView(imageView);
            }
        }
    }

    private boolean isValid(int col, int row) {
        return col >= 0 && col < gridCols && row >= 0 && row < gridRows;
    }

    public void setGridSize(int imgWidth, int imgHeight) {
        this.cellWidth = (float) imgWidth / gridCols;
        this.cellHeight = (float) imgHeight / gridRows;

        // 更新每個 ImageView 的大小與位置
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                ImageView iv = gridImageViews[col][row];
                LayoutParams params = (LayoutParams) iv.getLayoutParams();
                params.width = (int) cellWidth;
                params.height = (int) cellHeight;
                params.leftMargin = (int) (col * cellWidth);
                params.topMargin = (int) (row * cellHeight);
                iv.setLayoutParams(params);
            }
        }

        requestLayout();
        invalidate();
    }

    public void setTransformMatrix(Matrix matrix) {
        this.transformMatrix.set(matrix);
        float[] values = new float[9];
        matrix.getValues(values);
        setTranslationX(values[Matrix.MTRANS_X]);
        setTranslationY(values[Matrix.MTRANS_Y]);

        // 啟用硬體層
        setLayerType(LAYER_TYPE_HARDWARE, null);
        postInvalidate();
    }

    public void setCellImage(int col, int row, Bitmap bitmap) {
        if (isValid(col, row)) {
            gridImageViews[col][row].setImageBitmap(bitmap);
        }
    }



    public void setCellScale(int col, int row, float scale) {
        if (isValid(col, row)) {
            gridImageViews[col][row].setScaleX(scale);
            gridImageViews[col][row].setScaleY(scale);
        }
    }

    public void setCellRotation(int col, int row, float angle) {
        if (isValid(col, row)) {
            gridImageViews[col][row].setRotation(angle);
        }
    }

    public float getCellRotation(int col,int row){
        if(isValid(col,row)) {
            return gridImageViews[col][row].getRotation();
        }
        return 0;
    }

    public void setCellAlpha(int col, int row, int alpha) {
        if (isValid(col, row)) {
            gridImageViews[col][row].setAlpha(Math.max(0, Math.min(255, alpha)) / 255f);
        }
    }

    public float getCellWidth() {
        return cellWidth;
    }

    public float getCellHeight() {
        return cellHeight;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // 先不載入，只取得尺寸
        BitmapFactory.decodeResource(res, resId, options);

        // 計算壓縮比例
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    Bitmap toGrayScale(Bitmap src) {
        Bitmap bmpGrayscale = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.RGB_565); // 比 ARGB_8888 更省記憶體
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0); // 彩度 0 = 灰階
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(src, 0, 0, paint);
        return bmpGrayscale;
    }

    // 在 GridMapView 中新增方法
    public void cleanup() {
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                gridImageViews[col][row].setImageBitmap(null);
            }
        }
    }

    public void clearCellImage(int col, int row) {
        if (isValid(col, row)) {
            Drawable drawable = gridImageViews[col][row].getDrawable();
            if (drawable instanceof BitmapDrawable) {
                Bitmap bmp = ((BitmapDrawable) drawable).getBitmap();
                if (bmp != null && !bmp.isRecycled()) {
                    bmp.recycle(); // 若是你自己 decode 的 bitmap 才需要
                }
            }
            gridImageViews[col][row].setImageDrawable(null);
        }
    }

    public void clearAllCells() {
        for (int col = 0; col < gridImageViews.length; col++) {
            for (int row = 0; row < gridImageViews[0].length; row++) {
                clearCellImage(col, row);
            }
        }
    }

}
