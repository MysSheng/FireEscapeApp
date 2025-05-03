package com.app.myapp;

import static java.security.AccessController.getContext;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
//import android.support.annotation.DrawableRes;
//import android.support.annotation.NonNull;
//import android.support.constraint.ConstraintLayout;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.OrientationEventListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;

import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.ImageView;



public class MainActivity extends AppCompatActivity implements SensorEventListener {
    //處理手機旋轉角度
    /*
    private String TAG = "MainActivity";
    private OrientationEventListener mOrientationListener;
    private final void startOrientationChangeListener(){
        mOrientationListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int rotation) {
                Log.e(TAG, " "+ rotation);
            }
        };
        mOrientationListener.enable();
    }*/



    //陀螺儀旋轉
    private static final String TAG = "MainActivity";
    private static final long CALIBRATION_TIME_MS = 60_000; // 1 minute in milliseconds
    private SensorManager sensorManager;
    private Sensor accelerometerSensor,magnetometerSensor;
    private Sensor gyroscope;

    private float[] gyroValues = new float[3];
    private float[] gyroBias = new float[3];
    private float[] gyroBiasTemp = new float[3];
    private float[] gyroRotation = new float[3];

    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];

    boolean isLastAccelerometerArrayCopied = false;
    boolean isLastMagnetometerArrayCopied = false;
    float currentDegree = 0f;
    private long lastUpdateTime = 0;
    private boolean isCalibrating = false;
    private int calibrationCount = 0;
    private long calibrationStartTime = 0;
    private final int REQUEST_PERMISSION_CAMERA = 100;
    private boolean mbFaceDetAvailable;
    private int miMaxFaceCount = 0;
    private int miFaceDetMode;
    private TextureView mTextureView = null;
    private Size mPreviewSize = null;
    private CameraDevice mCameraDevice = null;
    private CaptureRequest.Builder mPreviewBuilder = null;
    private CameraCaptureSession mCameraPreviewCaptureSession = null,
                                mCameraTakePicCaptureSession = null;
    // 當UI的TextureView建立時，會執行onSurfaceTextureAvailable()
    private TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                    // 檢查是否取得camera的使用權限
                    if (askForPermissions())
                        openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                }
            };
    private final int rowCount = 100;
    private final int columnCount = 100;
    private ImageView[][] cellMap = new ImageView[rowCount][columnCount]; // 儲存 TextView 參照
    private int user_x = 52;
    private int user_y = 60;
    private float scaleFactor = 1.0f;
    private int imgWidth, imgHeight; // 儲存圖片的原始大小
    // 初步建構一個大室內空間
    private  CSIE_1F csie1F = new CSIE_1F();
    private Grid[][] grid = csie1F.getGrid();
    private final int fire_x = 24, fire_y = 45;
    private Planner fp = new Planner(grid, user_x, user_y, fire_x, fire_y);

    //自動偵測位置
    private Handler handler = new Handler();
    private Runnable detectionTask = new Runnable() {
        @Override
        public void run() {
            detectSomething();  // 執行偵測
            handler.postDelayed(this, 2000); // 每 2 秒執行一次
        }
    };

    private void startDetection() {
        handler.postDelayed(detectionTask, 2000); // 延遲 2 秒開始
    }

    private void stopDetection() {
        handler.removeCallbacks(detectionTask); // 停止偵測
    }

    private void detectSomething() {
        Log.d("Detection", "正在偵測...");
        // 這裡放你的偵測邏輯
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //陀螺儀測試
        // 初始化SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // 初始化陀螺儀感測器
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        // 初始化地磁加速度
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        //旋轉測試
        //startOrientationChangeListener();
        mTextureView = findViewById(R.id.textureView);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);




        fp.setRunSpeed(2);
        fp.do_one_level();
        //關於GridMap的可視化
        Grid[][] escapeMap = fp.user_guide(user_x,user_y);

        ImageView imageView = findViewById(R.id.imageView);
        GridMapView gridMapView = findViewById(R.id.gridMapView);

        //float currentPitchAngle=30f,delta=20f;
        //imageView.setRotationX(Math.min(60, Math.max(0, currentPitchAngle + delta / 10)));
        //applyPitch(+30); // 模擬提升角度


        // 確保初始化完成後再執行 UI 操作
        findViewById(R.id.gridMapView).post(() -> {
            setZoomScale(1.0f * 1.33f * 1.33f * 1.33f, new Runnable() {
                @Override
                public void run() {
                    //設置user
                    gridMapView.setCellImage(user_y, user_x, BitmapFactory.decodeResource(getResources(), R.drawable.user_point));
                    gridMapView.setCellScale(user_y, user_x, 3.0f);
                    gridMapView.gridToFront(user_y,user_x,1.5f);
                    //設置exit
                    gridMapView.setCellImage(1, 28, BitmapFactory.decodeResource(getResources(), R.drawable.exit_ui));
                    gridMapView.setCellImage(68, 28, BitmapFactory.decodeResource(getResources(), R.drawable.exit_ui));
                    gridMapView.setCellImage(59, 55, BitmapFactory.decodeResource(getResources(), R.drawable.exit_ui));
                    gridMapView.setCellImage(88, 58, BitmapFactory.decodeResource(getResources(), R.drawable.exit_ui));
                    gridMapView.setCellImage(66, 90, BitmapFactory.decodeResource(getResources(), R.drawable.exit_ui));
                    gridMapView.setCellScale(1, 28, 3.0f);
                    gridMapView.setCellScale(68, 28, 3.0f);
                    gridMapView.setCellScale(59, 55, 3.0f);
                    gridMapView.setCellScale(88, 58, 3.0f);
                    gridMapView.setCellScale(66, 90, 3.0f);
                    findUser(true);
                    showPath(escapeMap);
                }
            });
        });

        findUser(true);
        showPath(escapeMap);

        //連續更換位置測試
        //updateUser(user_x,user_y);

        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                now_x=0;
                now_y=0;
                findUser(true);
            }
        });

        FrameLayout imageContainer = findViewById(R.id.imageContainer);

        // 讓 FrameLayout 可以取c得焦點，以接收按鍵事件
        imageContainer.setFocusableInTouchMode(true);
        imageContainer.requestFocus();

        //單指滑動地圖雙指縮放調整解析倍率
        scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor(); // 手勢的縮放倍率

                // 設定最大最小範圍
                float newScale = now_scale * scaleFactor;
                newScale = Math.max(1.0f, Math.min(newScale, 1.33f*1.33f*1.33f*1.33f*1.33f));

                // 更新比例與平移（為了穩定性，可根據需要調整 now_x/now_y 的比例）
                now_x *= newScale/now_scale;
                now_y *= newScale/now_scale;
                now_scale = newScale;

                setZoomScale(now_scale,new Runnable() {
                    @Override
                    public void run() {
                    }
                });
                setMoveOffset(now_scale, now_x, now_y);
                return true;
            }
        });

        imageContainer.setOnTouchListener(new View.OnTouchListener() {
            private float lastTouchX;
            private float lastTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleDetector.onTouchEvent(event); // 讓 ScaleGestureDetector 處理縮放手勢

                // 確保縮放已初始化
                if (!isZoomInitialized) return true;
                // 限制更新頻率
                if (System.currentTimeMillis() - lastUpdateTime < 16) { // ~60fps
                    return true;
                }
                lastUpdateTime = System.currentTimeMillis();

                // 當所有手指都放開後才清除 scaling 狀態
                if ((event.getActionMasked() == MotionEvent.ACTION_UP ||
                        event.getActionMasked() == MotionEvent.ACTION_CANCEL) &&
                        event.getPointerCount() <= 1) {
                    isScaling = false;
                }

                if (event.getPointerCount() == 1) { // 單指才處理拖動
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            lastTouchX = event.getX();
                            lastTouchY = event.getY();
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float currentX = event.getX();
                            float currentY = event.getY();

                            float deltaX = currentX - lastTouchX;
                            float deltaY = currentY - lastTouchY;

                            lastTouchX = currentX;
                            lastTouchY = currentY;

                            now_x += deltaX;
                            now_y += deltaY;

                            setMoveOffset(now_scale, now_x, now_y);
                            return true;
                    }
                }
                return true;
            }
        });


        //code for test
        EditText editTextX=findViewById(R.id.editTextNumber), editTextY=findViewById(R.id.editTextNumber1);
        Button setPositionButton=findViewById(R.id.test);
        setPositionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int x = Integer.parseInt(editTextX.getText().toString());
                    int y = Integer.parseInt(editTextY.getText().toString());
                    user_x = x;
                    user_y = y;
                    updateUser(user_x,user_y);

                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "請輸入有效的數字", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }
















    private boolean isScaling = false;

    private float now_scale = 1.0f*1.33f*1.33f*1.33f,now_x = 0,now_y=0;
    private float lastTouchX, lastTouchY;
    private float posX = 0f, posY = 0f; // 當前平移位置
    private float maxPosX, maxPosY; // 最大可平移距離
    private ScaleGestureDetector scaleDetector;
    private void updateUser(int x, int y) {
        GridMapView gridMapView = findViewById(R.id.gridMapView);
        user_x = x;
        user_y = y;
        now_x = 0;
        now_y = 0;
        Grid[][] escapeMap=fp.user_guide(user_x,user_y);
        gridMapView.post(() -> {
            // 清空地圖
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
                    gridMapView.setCellImage(i, j, null);
                    gridMapView.setCellScale(i, j, 1f);
                    gridMapView.setCellRotation(i,j,0);
                    gridMapView.gridToFront(i,j,0);
                }
            }

            gridMapView.invalidate();
            gridMapView.setCellImage(user_y, user_x, BitmapFactory.decodeResource(getResources(), R.drawable.user_point));
            gridMapView.setCellScale(user_y, user_x, 3.0f);
            gridMapView.gridToFront(user_y,user_x,1.5f);
            //設置exit
            gridMapView.setCellImage(1, 28, BitmapFactory.decodeResource(getResources(), R.drawable.exit_ui));
            gridMapView.setCellImage(68, 28, BitmapFactory.decodeResource(getResources(), R.drawable.exit_ui));
            gridMapView.setCellImage(59, 55, BitmapFactory.decodeResource(getResources(), R.drawable.exit_ui));
            gridMapView.setCellImage(88, 58, BitmapFactory.decodeResource(getResources(), R.drawable.exit_ui));
            gridMapView.setCellImage(66, 90, BitmapFactory.decodeResource(getResources(), R.drawable.exit_ui));
            gridMapView.setCellScale(1, 28, 3.0f);
            gridMapView.setCellScale(68, 28, 3.0f);
            gridMapView.setCellScale(59, 55, 3.0f);
            gridMapView.setCellScale(88, 58, 3.0f);
            gridMapView.setCellScale(66, 90, 3.0f);
            findUser(true);
            showPath(escapeMap);
            // 強制刷新
            gridMapView.invalidate();
        });
    }

    private void findUser() {
        findUser(false);
    }

    private void findUser(boolean forceRecalculate) {
        GridMapView gridMapView = findViewById(R.id.gridMapView);

        gridMapView.post(() -> {
            // 確保視圖已完成佈局
            if (gridMapView.getHeight() == 0 || gridMapView.getWidth() == 0) {
                gridMapView.post(() -> findUser(forceRecalculate));
                return;
            }
            if (forceRecalculate || (now_x == 0 && now_y == 0)) {
                float moveSpeed = gridMapView.getCellHeight();
                final float target_offset_x = (50 - user_y) * moveSpeed;
                final float target_offset_y = (50 - user_x) * moveSpeed;

                now_x = target_offset_x;
                now_y = target_offset_y;
            }

            setMoveOffset(now_scale, now_x, now_y);
        });
    }

    // 在類別中新增成員變數
    private boolean isZoomInitialized = false;
    private int cachedImgWidth = 0;
    private int cachedImgHeight = 0;
    private int cachedViewWidth = 0;
    private int cachedViewHeight = 0;

    // 修改 setZoomScale 方法，增加回調
    private void setZoomScale(float scale, Runnable onComplete) {
        ImageView imageView = findViewById(R.id.imageView);
        GridMapView gridMapView = findViewById(R.id.gridMapView);

        if (isZoomInitialized) {
            applyZoomScale(imageView, gridMapView, scale);
            if (onComplete != null) onComplete.run();
            return;
        }

        imageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                if (imageView.getDrawable() != null) {
                    cachedImgWidth = imageView.getDrawable().getIntrinsicWidth();
                    cachedImgHeight = imageView.getDrawable().getIntrinsicHeight();
                    cachedViewWidth = imageView.getWidth();
                    cachedViewHeight = imageView.getHeight();

                    isZoomInitialized = true;
                    now_scale = scale; // 確保 now_scale 與實際縮放一致
                    applyZoomScale(imageView, gridMapView, scale);
                    if (onComplete != null) onComplete.run();
                }
            }
        });
    }

    private void applyZoomScale(ImageView imageView, GridMapView gridMapView, float scale) {
        // 使用快取的尺寸計算
        float initScale = Math.min(
                (float) cachedViewWidth / cachedImgWidth,
                (float) cachedViewHeight / cachedImgHeight
        );

        scaleFactor = initScale * scale;

        // 計算縮放後尺寸
        float scaledWidth = cachedImgWidth * scaleFactor;
        float scaledHeight = cachedImgHeight * scaleFactor;

        // 計算平移量（居中顯示）
        float translateX = (cachedViewWidth - scaledWidth) / 2;
        float translateY = (cachedViewHeight - scaledHeight) / 2;

        // 更新最大平移範圍
        maxPosX = Math.max(0, (scaledWidth - cachedViewWidth) / 2);
        maxPosY = Math.max(0, (scaledHeight - cachedViewHeight) / 2);

        // 限制當前位置
        posX = Math.max(-maxPosX, Math.min(maxPosX, posX));
        posY = Math.max(-maxPosY, Math.min(maxPosY, posY));

        // 應用變換（使用重用 Matrix）
        reusableMatrix.reset();
        reusableMatrix.postScale(scaleFactor, scaleFactor);
        reusableMatrix.postTranslate(translateX, translateY);
        imageView.setImageMatrix(reusableMatrix);

        // 更新 GridMapView 大小（只在尺寸變化超過1%時更新）
        if (Math.abs(gridMapView.getWidth() - scaledWidth) / scaledWidth > 0.01 ||
                Math.abs(gridMapView.getHeight() - scaledHeight) / scaledHeight > 0.01) {
            gridMapView.setGridSize((int) scaledWidth, (int) scaledHeight);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    (int) scaledWidth, (int) scaledHeight);
            gridMapView.setLayoutParams(params);
        }
    }

    // 在類別中新增成員變數
    private boolean isLayoutListenerSet = false;
    private Matrix reusableMatrix = new Matrix(); // 重用 Matrix 物件

    private void setMoveOffset(float scale, float moveX, float moveY) {
        ImageView imageView = findViewById(R.id.imageView);
        GridMapView gridMapView = findViewById(R.id.gridMapView);

        if (!isLayoutListenerSet) {
            isLayoutListenerSet = true;
            imageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // 移除監聽器避免重複註冊
                    imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // 原有完整邏輯
                    if (imageView.getDrawable() != null) {
                        imgWidth = imageView.getDrawable().getIntrinsicWidth();
                        imgHeight = imageView.getDrawable().getIntrinsicHeight();
                        int viewWidth = imageView.getWidth();
                        int viewHeight = imageView.getHeight();

                        // 計算初始縮放比例
                        float scaleX = (float) viewWidth / imgWidth;
                        float scaleY = (float) viewHeight / imgHeight;
                        float initScale = Math.min(scaleX, scaleY);

                        scaleFactor = initScale;
                        scaleFactor *= scale;

                        // 計算縮放後尺寸
                        float scaledWidth = imgWidth * scaleFactor;
                        float scaledHeight = imgHeight * scaleFactor;

                        // 計算最大可平移範圍
                        maxPosX = Math.max(0, (scaledWidth - imageView.getWidth()) / 2);
                        maxPosY = Math.max(0, (scaledHeight - imageView.getHeight()) / 2);

                        // 限制當前位置
                        posX = Math.max(-maxPosX, Math.min(maxPosX, posX));
                        posY = Math.max(-maxPosY, Math.min(maxPosY, posY));

                        float translateX = (viewWidth - scaledWidth) / 2 + moveX;
                        float translateY = (viewHeight - scaledHeight) / 2 + moveY;

                        // 應用變換
                        applyTransformation(imageView, gridMapView, translateX, translateY);

                        // 設定 GridMap 大小
                        gridMapView.setGridSize((int) scaledWidth, (int) scaledHeight);

                        // 調整 GridMapView 大小
                        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                                (int) scaledWidth, (int) scaledHeight);
                        gridMapView.setLayoutParams(params);
                    }
                }
            });
        } else {
            // 直接執行更新邏輯（優化路徑）
            updateViewTransform(imageView, gridMapView, scale, moveX, moveY);
        }
    }

    private void updateViewTransform(ImageView imageView, GridMapView gridMapView, float scale, float moveX, float moveY) {
        if (imageView.getDrawable() != null) {
            // 重用已計算的尺寸資訊
            float scaledWidth = imgWidth * scaleFactor;
            float scaledHeight = imgHeight * scaleFactor;
            int viewWidth = imageView.getWidth();
            int viewHeight = imageView.getHeight();

            // 計算最大可平移範圍
            maxPosX = Math.max(0, (scaledWidth - imageView.getWidth()) / 2);
            maxPosY = Math.max(0, (scaledHeight - imageView.getHeight()) / 2);

            // 限制當前位置
            posX = Math.max(-maxPosX, Math.min(maxPosX, posX));
            posY = Math.max(-maxPosY, Math.min(maxPosY, posY));

            float translateX = (viewWidth - scaledWidth) / 2 + moveX;
            float translateY = (viewHeight - scaledHeight) / 2 + moveY;

            // 應用變換（使用重用 Matrix）
            reusableMatrix.reset();
            reusableMatrix.postScale(scaleFactor, scaleFactor);
            reusableMatrix.postTranslate(translateX, translateY);
            imageView.setImageMatrix(reusableMatrix);

            // GridMapView 只應用平移
            reusableMatrix.reset();
            reusableMatrix.postTranslate(translateX, translateY);
            gridMapView.setTransformMatrix(reusableMatrix);

            // 注意：這裡不再重複設定 gridMapView 的尺寸和佈局參數，
            // 因為在第一次初始化時已經設定，且尺寸不會頻繁變化
        }
    }

    private void applyTransformation(ImageView imageView, GridMapView gridMapView, float translateX, float translateY) {
        // 使用重用 Matrix 替代新建
        reusableMatrix.reset();
        reusableMatrix.postScale(scaleFactor, scaleFactor);
        reusableMatrix.postTranslate(translateX, translateY);
        imageView.setImageMatrix(reusableMatrix);

        // GridMapView 只應用平移
        reusableMatrix.reset();
        reusableMatrix.postTranslate(translateX, translateY);
        gridMapView.setTransformMatrix(reusableMatrix);
    }

    //test code
    private String dirToText(int dir){
        if(dir==Grid.UP) return "↑";
        if(dir==Grid.DOWN) return "↓";
        if(dir==Grid.LEFT) return "←";
        if(dir==Grid.RIGHT) return "→";
        if(dir==Grid.UP_LEFT) return "↖";
        if(dir==Grid.UP_RIGHT) return "↗";
        if(dir==Grid.DOWN_LEFT) return "↙";
        if(dir==Grid.DOWN_RIGHT) return "↘";
        return "x"; // 預設情況或其他未定義的方向
    }


    public void showPath(Grid[][] escapeMap){
        int dir=escapeMap[user_x][user_y].getDirection();
        Log.d("yaju",dirToText(dir));
        if(dir==Grid.UP) showPath(user_x-1,user_y,escapeMap);
        if(dir==Grid.DOWN) showPath(user_x+1,user_y,escapeMap);
        if(dir==Grid.LEFT) showPath(user_x,user_y-1,escapeMap);
        if(dir==Grid.RIGHT) showPath(user_x,user_y+1,escapeMap);
        if(dir==Grid.UP_LEFT) showPath(user_x-1,user_y-1,escapeMap);
        if(dir==Grid.UP_RIGHT) showPath(user_x-1,user_y+1,escapeMap);
        if(dir==Grid.DOWN_LEFT) showPath(user_x+1,user_y-1,escapeMap);
        if(dir==Grid.DOWN_RIGHT) showPath(user_x+1,user_y+1,escapeMap);
    }

    public void showPath(int x,int y,Grid[][] escapeMap){
        GridMapView gridMapView = findViewById(R.id.gridMapView);
        gridMapView.gridToFront(y,x,1.2f);
        int dir=escapeMap[x][y].getDirection();
        Log.d("yaju",dirToText(dir));
        gridMapView.setCellScale(y,x,1.7f);
        if (dir == Grid.UP) {
            gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.start_u));
            showPath(x - 1, y, Grid.UP, escapeMap);
        } else if (dir == Grid.DOWN){
            gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.start_d));
            showPath(x + 1, y, Grid.DOWN, escapeMap);
        } else if (dir == Grid.DOWN_LEFT) {
            gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.start_ld));
            showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
        } else if (dir == Grid.DOWN_RIGHT) {
            gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.start_rd));
            showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
        } else if (dir == Grid.LEFT) {
            gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.start_l));
            showPath(x, y - 1, Grid.LEFT, escapeMap);
        } else if (dir == Grid.RIGHT) {
            gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.start_r));
            showPath(x, y + 1, Grid.RIGHT, escapeMap);
        } else if (dir == Grid.UP_LEFT) {
            gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.start_lu));
            showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
        } else if (dir == Grid.UP_RIGHT) {
            gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.start_ru));
            showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
        }
    }

    /*public void showPath(int x,int y,int lastDirection,Grid[][] escapeMap){
        GridMapView gridMapView = findViewById(R.id.gridMapView);
        if (escapeMap[x][y].getType() == Grid.ROAD) {
            int dir = escapeMap[x][y].getDirection();
            gridMapView.setCellScale(y,x,1.7f);
            if (lastDirection == Grid.UP) {
                if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.u_d));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                    if(escapeMap[x-1][y].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_u));
                    }
                } else if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_d));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.d_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_d));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.d_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                } else if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_d));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.d_ru));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                }
            } else if (lastDirection == Grid.DOWN) {
                if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.u_d));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                } else if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_u));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.u_ru));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_u));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.u_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                } else if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_u));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.u_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                }
            } else if (lastDirection == Grid.LEFT) {
                if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_r));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ru_r));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.r_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.u_r));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.d_r));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                } else if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_r));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                } else if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_r));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                }
            } else if (lastDirection == Grid.RIGHT) {
                if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                } else if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_l));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                } else if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_ld));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_u));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_d));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_ru));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                }
            } else if (lastDirection == Grid.UP_LEFT) {
                gridMapView.setCellImage(y, x+1, getCachedBitmap(R.drawable.lu_rd_down));
                gridMapView.setCellImage(y+1, x, getCachedBitmap(R.drawable.lu_rd_up));
                if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_rd));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ru_rd));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.d_rd));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.r_rd));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                } else if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_rd));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.u_rd));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_rd));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                }
            } else if (lastDirection == Grid.UP_RIGHT) {
                gridMapView.setCellImage(y, x+1, getCachedBitmap(R.drawable.ld_ru_down));
                gridMapView.setCellImage(y-1, x, getCachedBitmap(R.drawable.ld_ru_up));
                if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_ld));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_ru));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_d));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_ld));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_u));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                }
            } else if (lastDirection == Grid.DOWN_LEFT) {
                gridMapView.setCellImage(y, x-1, getCachedBitmap(R.drawable.ld_ru_up));
                gridMapView.setCellImage(y+1, x, getCachedBitmap(R.drawable.ld_ru_down));
                if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_ru));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ru_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.u_ru));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ru_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                } else if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_ru));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.d_ru));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_ru));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                }
            } else if (lastDirection == Grid.DOWN_RIGHT) {
                gridMapView.setCellImage(y, x-1, getCachedBitmap(R.drawable.lu_rd_up));
                gridMapView.setCellImage(y-1, x, getCachedBitmap(R.drawable.lu_rd_down));
                if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_rd));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_u));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_l));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_ru));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_d));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                }
            }
        }
    }*/
    public void showPath(int x,int y,int lastDirection,Grid[][] escapeMap){
        GridMapView gridMapView = findViewById(R.id.gridMapView);
        gridMapView.gridToFront(y,x,1.0f);
        if (escapeMap[x][y].getType() == Grid.ROAD) {
            int dir = escapeMap[x][y].getDirection();
            Log.d("yaju",dirToText(dir));
            gridMapView.setCellScale(y,x,1.7f);
            if (lastDirection == Grid.UP) {
                if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.u_d));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                    if(escapeMap[x-1][y].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_d));
                    }
                } else if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_d));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                    if(escapeMap[x+1][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_d));
                    }
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.d_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                    if(escapeMap[x+1][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_d));
                    }
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_d));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                    if(escapeMap[x][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_d));
                    }
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.d_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                    if(escapeMap[x][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_d));
                    }
                } else if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_d));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                    if(escapeMap[x-1][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_d));
                    }
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.d_ru));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                    if(escapeMap[x-1][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_d));
                    }
                }
            } else if (lastDirection == Grid.DOWN) {
                if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.u_d));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                    if(escapeMap[x+1][y].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_u));
                    }
                } else if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_u));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                    if(escapeMap[x-1][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_u));
                    }
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.u_ru));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                    if(escapeMap[x-1][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_u));
                    }
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_u));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                    if(escapeMap[x][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_u));
                    }
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.u_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                    if(escapeMap[x][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_u));
                    }
                } else if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_u));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                    if(escapeMap[x+1][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_u));
                    }
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.u_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                    if(escapeMap[x+1][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_u));
                    }
                }
            } else if (lastDirection == Grid.LEFT) {
                if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_r));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                    if(escapeMap[x][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_r));
                    }
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ru_r));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                    if(escapeMap[x-1][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_r));
                    }
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.r_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                    if(escapeMap[x+1][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_r));
                    }
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.u_r));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                    if(escapeMap[x-1][y].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_r));
                    }
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.d_r));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                    if(escapeMap[x+1][y].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_r));
                    }
                } else if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_r));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                    if(escapeMap[x-1][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_r));
                    }
                } else if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_r));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                    if(escapeMap[x+1][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_r));
                    }
                }
            } else if (lastDirection == Grid.RIGHT) {
                if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                    if(escapeMap[x][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_l));
                    }
                } else if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_l));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                    if(escapeMap[x-1][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_l));
                    }
                } else if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_ld));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                    if(escapeMap[x+1][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_l));
                    }
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_u));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                    if(escapeMap[x-1][y].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_l));
                    }
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_d));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                    if(escapeMap[x+1][y].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_l));
                    }
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_ru));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                    if(escapeMap[x-1][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_l));
                    }
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                    if(escapeMap[x+1][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_l));
                    }
                }
            } else if (lastDirection == Grid.UP_LEFT) {
                gridMapView.setCellImage(y, x+1, getCachedBitmap(R.drawable.lu_rd_down));
                gridMapView.setCellImage(y+1, x, getCachedBitmap(R.drawable.lu_rd_up));
                if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_rd));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                    if(escapeMap[x-1][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_rd));
                    }
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ru_rd));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                    if(escapeMap[x-1][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_rd));
                    }
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.d_rd));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                    if(escapeMap[x+1][y].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_rd));
                    }
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.r_rd));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                    if(escapeMap[x][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_rd));
                    }
                } else if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_rd));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                    if(escapeMap[x+1][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_rd));
                    }
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.u_rd));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                    if(escapeMap[x-1][y].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_rd));
                    }
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_rd));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                    if(escapeMap[x][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_rd));
                    }
                }
            } else if (lastDirection == Grid.UP_RIGHT) {
                gridMapView.setCellImage(y, x+1, getCachedBitmap(R.drawable.ld_ru_down));
                gridMapView.setCellImage(y-1, x, getCachedBitmap(R.drawable.ld_ru_up));
                if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_ld));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                    if(escapeMap[x-1][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_ld));
                    }
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_ru));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                    if(escapeMap[x-1][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_ld));
                    }
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_d));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                    if(escapeMap[x+1][y].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_ld));
                    }
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_ld));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                    if(escapeMap[x][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_ld));
                    }
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                    if(escapeMap[x+1][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_ld));
                    }
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_u));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                    if(escapeMap[x-1][y].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_ld));
                    }
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                    if(escapeMap[x][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_ld));
                    }
                }
            } else if (lastDirection == Grid.DOWN_LEFT) {
                gridMapView.setCellImage(y, x-1, getCachedBitmap(R.drawable.ld_ru_up));
                gridMapView.setCellImage(y+1, x, getCachedBitmap(R.drawable.ld_ru_down));
                if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ld_ru));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                    if(escapeMap[x+1][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_ru));
                    }
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ru_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                    if(escapeMap[x+1][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_ru));
                    }
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.u_ru));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                    if(escapeMap[x-1][y].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_ru));
                    }
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.ru_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                    if(escapeMap[x][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_ru));
                    }
                } else if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_ru));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                    if(escapeMap[x-1][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_ru));
                    }
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.d_ru));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                    if(escapeMap[x+1][y].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_ru));
                    }
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.l_ru));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                    if(escapeMap[x][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_ru));
                    }
                }
            } else if (lastDirection == Grid.DOWN_RIGHT) {
                gridMapView.setCellImage(y, x-1, getCachedBitmap(R.drawable.lu_rd_up));
                gridMapView.setCellImage(y-1, x, getCachedBitmap(R.drawable.lu_rd_down));
                if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_rd));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                    if(escapeMap[x+1][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_lu));
                    }
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                    if(escapeMap[x+1][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_lu));
                    }
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_u));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                    if(escapeMap[x-1][y].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_lu));
                    }
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_l));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                    if(escapeMap[x][y-1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_lu));
                    }
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_ru));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                    if(escapeMap[x-1][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_lu));
                    }
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_d));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                    if(escapeMap[x+1][y].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_lu));
                    }
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.lu_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                    if(escapeMap[x][y+1].getType()==Grid.EXIT){
                        gridMapView.setCellImage(y, x, getCachedBitmap(R.drawable.end_lu));
                    }
                }
            }
        }
    }


    private Map<Integer, Bitmap> bitmapCache = new HashMap<>();
    private Bitmap getCachedBitmap(@DrawableRes int resId) {
        if (!bitmapCache.containsKey(resId)) {
            Bitmap bmp = GridMapView.decodeSampledBitmapFromResource(getResources(), resId, 8, 8);
            bitmapCache.put(resId, bmp);
        }
        return bitmapCache.get(resId);
    }

    //路徑UI
    @Override
    protected void onResume() {
        super.onResume();

        // 如果TextureView已經可以使用，就檢查Camera的使用權限
        // 然後開啟Camera
        if (mTextureView.isAvailable()) {
            if (askForPermissions())
                openCamera();
        }

        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);

        //測地磁角
       sensorManager.registerListener(this,accelerometerSensor,SensorManager.SENSOR_DELAY_NORMAL);
       sensorManager.registerListener(this,magnetometerSensor,SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    // 獲取初始朝向


    // 記錄初始朝向（第一次讀取到方位角時設置）
    private boolean isInitialOrientationSet = false;
    private float initialAzimuth = 0;

    // 加速度計 & 磁力計數據
    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];

    @Override
    public void onSensorChanged(SensorEvent event) {

        ImageView compassArrow = findViewById(R.id.compassArrow);
        GridMapView gridMapView =findViewById(R.id.gridMapView);

        if(event.sensor == accelerometerSensor){
            System.arraycopy(event.values,0,lastAccelerometer,0,event.values.length);
            isLastAccelerometerArrayCopied = true;
        }else if(event.sensor == magnetometerSensor){
            System.arraycopy(event.values,0,lastMagnetometer,0,event.values.length);
            isLastMagnetometerArrayCopied = true;
        }

        if(isLastMagnetometerArrayCopied && isLastAccelerometerArrayCopied && System.currentTimeMillis() - lastUpdateTime>250){
            SensorManager.getRotationMatrix(rotationMatrix,null,lastAccelerometer,lastMagnetometer);
            SensorManager.getOrientation(rotationMatrix,orientation);

            float azimuthInRadians = orientation[0];
            float azimuthInDegree = (float) Math.toDegrees(azimuthInRadians);


            // 平面圖的基準角度（上方對應的北方偏移角）
            float baseOffsetAngle = 0f;

            // 計算調整後的角度，使其符合平面圖的方向
            float adjustedAzimuth = azimuthInDegree + baseOffsetAngle;

            // 確保角度保持在 0 - 360 之間
            if (adjustedAzimuth >= 360) {
                adjustedAzimuth -= 360;
            } else if (adjustedAzimuth < 0) {
                adjustedAzimuth += 360;
            }

            RotateAnimation rotateAnimation =
                    new RotateAnimation(currentDegree,-azimuthInDegree,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
            rotateAnimation.setDuration(250);
            rotateAnimation.setFillAfter(true);

            RotateAnimation oppositeRotateAnimation =
                    new RotateAnimation(-currentDegree, azimuthInDegree,Animation.RELATIVE_TO_SELF, 0.5f,Animation.RELATIVE_TO_SELF, 0.5f);
            oppositeRotateAnimation.setDuration(250);
            oppositeRotateAnimation.setFillAfter(true);

            compassArrow.startAnimation(rotateAnimation);
            gridMapView.setCellRotation(user_y,user_x,azimuthInDegree);

            currentDegree = -azimuthInDegree;
            lastUpdateTime = System.currentTimeMillis();
        }

        //旋轉偵測


        long currentTime = System.currentTimeMillis();
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            if (isCalibrating) {
                // 初始化開始時間
                if (calibrationStartTime == 0) {
                    calibrationStartTime = currentTime;
                }

                // 計算剩餘時間
                long timeRemaining = CALIBRATION_TIME_MS - (currentTime - calibrationStartTime);

                if (timeRemaining > 0) {  // 還在校準中
                    // 累加校準數值
                    gyroBiasTemp[0] += event.values[0];
                    gyroBiasTemp[1] += event.values[1];
                    gyroBiasTemp[2] += event.values[2];

                    // 累加校準次數
                    calibrationCount++;
                } else {  // 校準完成
                    // 計算平均值
                    gyroBiasTemp[0] /= calibrationCount;
                    gyroBiasTemp[1] /= calibrationCount;
                    gyroBiasTemp[2] /= calibrationCount;

                    // 複製到 gyroBias
                    System.arraycopy(gyroBiasTemp, 0, gyroBias, 0, gyroBiasTemp.length);

                    // 重置校準狀態
                    isCalibrating = false;
                    calibrationCount = 0;
                    calibrationStartTime = 0;
                    gyroBiasTemp[0] = 0;
                    gyroBiasTemp[1] = 0;
                    gyroBiasTemp[2] = 0;
                }
            } else {
                // 初始化上次更新時間
                if (lastUpdateTime == 0) {
                    lastUpdateTime = currentTime;
                    return;
                }

                // 計算時間差
                double dt = (currentTime - lastUpdateTime) / 1000.0;
                lastUpdateTime = currentTime;

                // 計算弧度變化
                double dx = (event.values[0] - gyroBias[0] + gyroValues[0]) * dt / 2.0;
                double dy = (event.values[1] - gyroBias[1] + gyroValues[1]) * dt / 2.0;
                double dz = (event.values[2] - gyroBias[2] + gyroValues[2]) * dt / 2.0;

                // 計算角度變化
                double dx_deg = Math.toDegrees(dx);
                double dy_deg = Math.toDegrees(dy);
                double dz_deg = Math.toDegrees(dz);

                // 儲存當前的陀螺儀數值
                gyroValues[0] = event.values[0] - gyroBias[0];
                gyroValues[1] = event.values[1] - gyroBias[1];
                gyroValues[2] = event.values[2] - gyroBias[2];

                // 累加角度
                gyroRotation[0] += dx_deg;
                gyroRotation[1] += dy_deg;
                gyroRotation[2] += dz_deg;

                compassArrow.setRotation(compassArrow.getRotation() + +(float) dz_deg);
                gridMapView.setCellRotation(user_y,user_x,gridMapView.getCellRotation(user_y,user_x) - (float) dz_deg);
            }
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private boolean askForPermissions() {
        // App需要用的功能權限清單
        String[] permissions= new String[] {
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};

        // 檢查是否已經取得權限
        final List<String> listPermissionsNeeded = new ArrayList<>();
        boolean bShowPermissionRationale = false;

        for (String p: permissions) {
            int result = ContextCompat.checkSelfPermission(MainActivity.this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);

                // 檢查是否需要顯示說明
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        MainActivity.this, p))
                    bShowPermissionRationale = true;
            }
        }

        // 向使用者徵詢還沒有許可的權限
        if (!listPermissionsNeeded.isEmpty()) {
            if (bShowPermissionRationale) {
                AlertDialog.Builder altDlgBuilder =
                        new AlertDialog.Builder(MainActivity.this);
                altDlgBuilder.setTitle("提示");
                altDlgBuilder.setMessage("App需要您的許可才能執行。");
                altDlgBuilder.setIcon(android.R.drawable.ic_dialog_info);
                altDlgBuilder.setCancelable(false);
                altDlgBuilder.setPositiveButton("確定",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        listPermissionsNeeded.toArray(
                                                new String[listPermissionsNeeded.size()]),
                                        REQUEST_PERMISSION_CAMERA);
                            }
                        });
                altDlgBuilder.show();
            } else
                ActivityCompat.requestPermissions(MainActivity.this,
                        listPermissionsNeeded.toArray(
                                new String[listPermissionsNeeded.size()]),
                        REQUEST_PERMISSION_CAMERA);

            return false;
        }

        return true;
    }

    private void openCamera() {
        // 取得 CameraManager
        CameraManager camMgr = (CameraManager) getSystemService(CAMERA_SERVICE);

        try{
            // 取得相機背後的 camera
            String cameraId = camMgr.getCameraIdList()[0];
            CameraCharacteristics camChar =
                    camMgr.getCameraCharacteristics(cameraId);

            // 取得解析度
            StreamConfigurationMap map =
                    camChar.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];

            // 啟動 camera
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                camMgr.openCamera(cameraId, mCameraStateCallback, null);
        }
        catch(CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback mCameraStateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    mCameraDevice = cameraDevice;
                    startPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    Toast.makeText(MainActivity.this, "無法使用camera", Toast.LENGTH_LONG)
                            .show();
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {
                    Toast.makeText(MainActivity.this, "Camera開啟錯誤", Toast.LENGTH_LONG)
                            .show();
                }
            };

    // Camera的CaptureSession狀態改變時執行
    private CameraCaptureSession.StateCallback mCameraCaptureSessionCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(
                        @NonNull CameraCaptureSession cameraCaptureSession) {
                    closeAllCameraCaptureSession();

                    // 記下這個capture session，使用完畢要刪除
                    mCameraPreviewCaptureSession = cameraCaptureSession;

                    mPreviewBuilder.set(
                            CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    mPreviewBuilder.set(
                            CaptureRequest.STATISTICS_FACE_DETECT_MODE, miFaceDetMode);

                    HandlerThread backgroundThread =
                            new HandlerThread("CameraPreview");
                    backgroundThread.start();
                    Handler backgroundHandler =
                            new Handler(backgroundThread.getLooper());

                    try {
                        mCameraPreviewCaptureSession.setRepeatingRequest(
                                mPreviewBuilder.build(), null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(
                        @NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this,
                            "Camera預覽錯誤", Toast.LENGTH_LONG)
                            .show();
                }
            };

    private void startPreview() {
        // 從TextureView取得SurfaceTexture
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();

        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(),
                mPreviewSize.getHeight());

        // 依照TextureView的解析度建立一個 surface 給camera使用
        Surface surface = new Surface(surfaceTexture);

        // 設定camera的CaptureRequest和CaptureSession
        try {
            mPreviewBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e){
            e.printStackTrace();
        }

        mPreviewBuilder.addTarget(surface);

        try {
            mCameraDevice.createCaptureSession(Arrays.asList(surface),
                    mCameraCaptureSessionCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 建立新的Camera Capture Session之前
    // 呼叫這個方法，清除舊的Camera Capture Session
    private void closeAllCameraCaptureSession() {
        if (mCameraPreviewCaptureSession != null) {
            mCameraPreviewCaptureSession.close();
            mCameraPreviewCaptureSession = null;
        }

        if (mCameraTakePicCaptureSession != null) {
            mCameraTakePicCaptureSession.close();
            mCameraTakePicCaptureSession = null;
        }
    }
}
