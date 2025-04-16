package com.app.myapp;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
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
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import java.util.List;
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
    private int user_x = 18;
    private int user_y = 20;
    private float scaleFactor = 1.0f;
    private int imgWidth, imgHeight; // 儲存圖片的原始大小

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



        // 初步建構一個大室內空間
        CSIE_1F csie1F = new CSIE_1F();
        Grid[][] grid = csie1F.getGrid();
        int fire_x = 24, fire_y = 45;


        Planner fp = new Planner(grid, user_x, user_y, fire_x, fire_y);
        fp.addRoom(new Room(1, 0, 0, 27, 27));
        fp.addRoom(new Room(2, 24, 0, 27, 27));
        fp.addRoom(new Room(3, 48, 0, 24, 36));
        fp.addRoom(new Room(4, 0, 24, 51, 18));
        fp.addRoom(new Room(5, 0, 39, 27, 27));
        fp.addRoom(new Room(6, 24, 39, 27, 27));
        fp.addRoom(new Room(7, 48, 30, 24, 36));
        fp.setRunSpeed(2);
        fp.do_one_level();
        //fp.do_task();
        fp.testNavigator();
        ////////// fp.testEdge();
        fp.testOutgoingEdge();
        ////////// fp.user_guide(user_x, user_y);

        //關於GridMap的可視化
        //Grid[][] escapeMap =fp.returnMap();
        //Grid[][] escapeMap = fp.getGridMap();
        Grid[][] escapeMap = fp.test_one_level();

        ImageView imageView = findViewById(R.id.imageView);
        GridMapView gridMapView = findViewById(R.id.gridMapView);


        // 縮小到原始大小
        setZoomScale(1.0f*1.33f*1.33f*1.33f);
        for(int i=0;i<100;i++){
            for(int j=0;j<100;j++){
                gridMapView.setGridVisibility(false);
            }
        }
        gridMapView.setCellImage(user_y, user_x, BitmapFactory.decodeResource(getResources(),R.drawable.user_point));
        gridMapView.setCellScale(user_y, user_x, 2.5f);


        findUser(true);
        //抓定開頭
        int dir=escapeMap[user_x][user_y].getDirection();
        if(dir==Grid.UP) showPath(user_x-1,user_y,Grid.UP,escapeMap);
        if(dir==Grid.DOWN) showPath(user_x+1,user_y,Grid.DOWN,escapeMap);
        if(dir==Grid.LEFT) showPath(user_x,user_y-1,Grid.LEFT,escapeMap);
        if(dir==Grid.RIGHT) showPath(user_x,user_y+1,Grid.RIGHT,escapeMap);
        if(dir==Grid.UP_LEFT) showPath(user_x-1,user_y-1,Grid.UP_LEFT,escapeMap);
        if(dir==Grid.UP_RIGHT) showPath(user_x-1,user_y+1,Grid.UP_RIGHT,escapeMap);
        if(dir==Grid.DOWN_LEFT) showPath(user_x+1,user_y-1,Grid.DOWN_LEFT,escapeMap);
        if(dir==Grid.DOWN_RIGHT) showPath(user_x+1,user_y+1,Grid.DOWN_RIGHT,escapeMap);
        //連續更換位置測試
        //updateUser(85,85);
        //updateUser(3,3);
        //updateUser(29,73);
        //findUser(true);

        Button button1 = findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(now_scale>=1) {
                    if(now_scale<=1.33f){
                        setZoomScale(1f);
                        now_x/=now_scale;
                        now_y/=now_scale;
                        now_scale=1f;
                        gridMapView.post(() -> {
                            setMoveOffset(now_scale, now_x, now_y);
                        });
                    }
                    else {
                        setZoomScale(now_scale/1.33f);
                        now_scale/=1.33f;
                        now_x/=1.33f;
                        now_y/=1.33f;
                        gridMapView.post(() -> {
                            setMoveOffset(now_scale, now_x, now_y);
                        });
                    }
                }
            }
        });

        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(now_scale<=4) {
                    setZoomScale(now_scale*1.33f);
                    now_scale*=1.33f;
                    now_x*=1.33f;
                    now_y*=1.33f;
                    gridMapView.post(() -> {
                        setMoveOffset(now_scale, now_x, now_y);
                    });
                }
            }
        });

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
                now_scale = newScale;
                now_x *= scaleFactor;
                now_y *= scaleFactor;

                setMoveOffset(now_scale, now_x, now_y);
                return true;
            }
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                isScaling = true; // 正在縮放中，暫停滑動
                return true;
            }
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                // 不立即結束 scaling，等手指全放開後處理
            }
        });

        imageContainer.setOnTouchListener(new View.OnTouchListener() {
            private float lastTouchX;
            private float lastTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleDetector.onTouchEvent(event); // 讓 ScaleGestureDetector 處理縮放手勢

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


    }

    private boolean isScaling = false;

    private float now_scale = 1.0f*1.33f*1.33f*1.33f,now_x = 0,now_y=0;
    private float lastTouchX, lastTouchY;
    private float posX = 0f, posY = 0f; // 當前平移位置
    private float maxPosX, maxPosY; // 最大可平移距離
    private ScaleGestureDetector scaleDetector;

    private void updateUser(int x, int y) {
        GridMapView gridMapView = findViewById(R.id.gridMapView);
        this.user_x = x;
        this.user_y = y;
        gridMapView.post(() -> {
            // 清空地圖
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
                    gridMapView.setCellImage(i, j, null);
                    gridMapView.setCellScale(i, j, 1f);
                }
            }

            // 更新user位置
            gridMapView.setCellImage(this.user_x, this.user_y,
                    BitmapFactory.decodeResource(getResources(), R.drawable.user_point));
            gridMapView.setCellScale(this.user_x, this.user_y, 4.5f);

            now_x = 0;
            now_y = 0;

            // 確保在視圖完成佈局後再定位
            gridMapView.post(() -> {
                findUser(true);
            });
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

            // 使用post確保UI更新在主線程執行
            gridMapView.post(() -> {
                setMoveOffset(now_scale, now_x, now_y);
            });
        });
    }

    private void setZoomScale(float scale) {
        ImageView imageView = findViewById(R.id.imageView);
        GridMapView gridMapView = findViewById(R.id.gridMapView);

        // 設定 GridMap 預設大小等於圖片的大小
        imageView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (imageView.getDrawable() != null) {
                imgWidth = imageView.getDrawable().getIntrinsicWidth();
                imgHeight = imageView.getDrawable().getIntrinsicHeight();
                int viewWidth = imageView.getWidth();
                int viewHeight = imageView.getHeight();
                // 計算初始縮放比例，讓圖片完整顯示
                float scaleX = (float) viewWidth / imgWidth;
                float scaleY = (float) viewHeight / imgHeight;
                float initScale = Math.min(scaleX, scaleY);
                // 設定初始縮放
                scaleFactor = initScale;
                // 放大3倍
                scaleFactor *= scale;
                // 計算平移量，讓圖片居中顯示
                float scaledWidth = imgWidth * scaleFactor;
                float scaledHeight = imgHeight * scaleFactor;

                // 計算最大可平移範圍
                maxPosX = Math.max(0, (scaledWidth - imageView.getWidth()) / 2);
                maxPosY = Math.max(0, (scaledHeight - imageView.getHeight()) / 2);

                // 限制當前位置在合法範圍內
                posX = Math.max(-maxPosX, Math.min(maxPosX, posX));
                posY = Math.max(-maxPosY, Math.min(maxPosY, posY));



                float translateX = (viewWidth - scaledWidth) / 2;
                float translateY = (viewHeight - scaledHeight) / 2;
                // 應用變換
                //applyTransformation(imageView, gridMapView, translateX, translateY);
                // 設定 GridMap 大小與圖片匹配
                //gridMapView.setGridSize((int)(imgWidth * 1), (int)(imgHeight * 1));
                gridMapView.setGridSize((int) scaledWidth, (int) scaledHeight);
                // 調整 GridMapView 的大小
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int) scaledWidth, (int) scaledHeight);
                gridMapView.setLayoutParams(params);
            }
        });

    }

    private void setMoveOffset(float scale,float moveX,float moveY){
        ImageView imageView = findViewById(R.id.imageView);
        GridMapView gridMapView = findViewById(R.id.gridMapView);

        // 設定 GridMap 預設大小等於圖片的大小
        imageView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (imageView.getDrawable() != null) {
                imgWidth = imageView.getDrawable().getIntrinsicWidth();
                imgHeight = imageView.getDrawable().getIntrinsicHeight();
                int viewWidth = imageView.getWidth();
                int viewHeight = imageView.getHeight();
                // 計算初始縮放比例，讓圖片完整顯示
                float scaleX = (float) viewWidth / imgWidth;
                float scaleY = (float) viewHeight / imgHeight;
                float initScale = Math.min(scaleX, scaleY);
                // 設定初始縮放
                scaleFactor = initScale;
                // 放大3倍
                scaleFactor *= scale;
                // 計算平移量，讓圖片居中顯示
                float scaledWidth = imgWidth * scaleFactor;
                float scaledHeight = imgHeight * scaleFactor;

                // 計算最大可平移範圍
                maxPosX = Math.max(0, (scaledWidth - imageView.getWidth()) / 2);
                maxPosY = Math.max(0, (scaledHeight - imageView.getHeight()) / 2);

                // 限制當前位置在合法範圍內
                posX = Math.max(-maxPosX, Math.min(maxPosX, posX));
                posY = Math.max(-maxPosY, Math.min(maxPosY, posY));

                float translateX = (viewWidth - scaledWidth) / 2 + moveX;
                float translateY = (viewHeight - scaledHeight) / 2 + moveY;
                // 應用變換
                applyTransformation(imageView, gridMapView, translateX, translateY);
                // 設定 GridMap 大小與圖片匹配
                //gridMapView.setGridSize((int)(imgWidth * 1), (int)(imgHeight * 1));
                gridMapView.setGridSize((int) scaledWidth, (int) scaledHeight);
                // 調整 GridMapView 的大小
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int) scaledWidth, (int) scaledHeight);
                gridMapView.setLayoutParams(params);
            }
        });

    }

    private void applyTransformation(ImageView imageView, GridMapView gridMapView, float translateX, float translateY) {
        Matrix matrix = new Matrix();
        // 應用縮放
        matrix.postScale(scaleFactor, scaleFactor);
        // 應用平移
        matrix.postTranslate(translateX, translateY);
        // 設定 ImageView 的矩陣
        imageView.setImageMatrix(matrix);

        // 設定 GridMapView 的矩陣
        //gridMapView.setTransformMatrix(matrix);

        // GridMapView 只應用平移
        Matrix translationMatrix = new Matrix();
        translationMatrix.postTranslate(translateX, translateY);
        gridMapView.setTransformMatrix(translationMatrix);
    }

    public void showPath(int x,int y,int lastDirection,Grid[][] escapeMap){
        GridMapView gridMapView = findViewById(R.id.gridMapView);
        if (escapeMap[x][y].getType() == Grid.ROAD) {
            int dir = escapeMap[x][y].getDirection();

            if (lastDirection == Grid.UP) {
                if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.u_d));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                } else if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.ld_d));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.d_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.l_d));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.d_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                } else if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.lu_d));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.d_ru));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                }
            } else if (lastDirection == Grid.DOWN) {
                if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.u_d));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                } else if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.lu_u));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.u_ru));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.l_u));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.u_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                } else if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.ld_u));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.u_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                }
            } else if (lastDirection == Grid.LEFT) {
                if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.l_r));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.ru_r));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.r_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.u_r));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.d_r));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                } else if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.lu_r));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                } else if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.ld_r));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                }
            } else if (lastDirection == Grid.RIGHT) {
                if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.l_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                } else if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.lu_l));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                } else if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.l_ld));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.l_u));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.l_d));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.l_ru));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.l_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                }
            } else if (lastDirection == Grid.UP_LEFT) {
                if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.lu_rd));
                    gridMapView.setCellImage(y, x+1, BitmapFactory.decodeResource(getResources(), R.drawable.lu_rd_down));
                    gridMapView.setCellImage(y+1, x, BitmapFactory.decodeResource(getResources(), R.drawable.lu_rd_up));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.ru_rd));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.d_rd));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.r_rd));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                } else if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.ld_rd));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.u_rd));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.l_rd));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                }

            } else if (lastDirection == Grid.UP_RIGHT) {
                if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.lu_ld));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.ld_ru));
                    gridMapView.setCellImage(y, x-1, BitmapFactory.decodeResource(getResources(), R.drawable.ld_ru_down));
                    gridMapView.setCellImage(y-1, x, BitmapFactory.decodeResource(getResources(), R.drawable.ld_ru_up));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.ld_d));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.l_ld));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.ld_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.ld_u));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.ld_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                }

            } else if (lastDirection == Grid.DOWN_LEFT) {
                if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.ld_ru));
                    gridMapView.setCellImage(y, x-1, BitmapFactory.decodeResource(getResources(), R.drawable.ld_ru_up));
                    gridMapView.setCellImage(y+1, x, BitmapFactory.decodeResource(getResources(), R.drawable.lu_rd_down));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.ru_rd));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.u_ru));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.ru_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                } else if (dir == Grid.UP_LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.lu_ru));
                    showPath(x - 1, y - 1, Grid.UP_LEFT, escapeMap);
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.d_ru));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.l_ru));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                }

            } else if (lastDirection == Grid.DOWN_RIGHT) {
                if (dir == Grid.DOWN_LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.lu_rd));
                    showPath(x + 1, y - 1, Grid.DOWN_LEFT, escapeMap);
                } else if (dir == Grid.DOWN_RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.lu_rd));
                    gridMapView.setCellImage(y, x-1, BitmapFactory.decodeResource(getResources(), R.drawable.lu_rd_up));
                    gridMapView.setCellImage(y-1, x, BitmapFactory.decodeResource(getResources(), R.drawable.lu_rd_down));
                    showPath(x + 1, y + 1, Grid.DOWN_RIGHT, escapeMap);
                } else if (dir == Grid.UP) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.lu_u));
                    showPath(x - 1, y, Grid.UP, escapeMap);
                } else if (dir == Grid.LEFT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.lu_l));
                    showPath(x, y - 1, Grid.LEFT, escapeMap);
                } else if (dir == Grid.UP_RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.lu_ru));
                    showPath(x - 1, y + 1, Grid.UP_RIGHT, escapeMap);
                } else if (dir == Grid.DOWN) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.lu_d));
                    showPath(x + 1, y, Grid.DOWN, escapeMap);
                } else if (dir == Grid.RIGHT) {
                    gridMapView.setCellImage(y, x, BitmapFactory.decodeResource(getResources(), R.drawable.lu_r));
                    showPath(x, y + 1, Grid.RIGHT, escapeMap);
                }
            }

        }
    }

    //路徑UI
    /*
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


        //getInitialOrientation(); // 獲取初始方向

    }
    */
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
            float baseOffsetAngle = 68.0f;

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
            cellMap[user_x][user_y].startAnimation(oppositeRotateAnimation);

            currentDegree = -azimuthInDegree;
            lastUpdateTime = System.currentTimeMillis();
        }
        /*
        //地磁偵測
        final float alpha = 0.97f;
        synchronized (this){
            if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                mGravity[0] = alpha * mGravity[0] + (1-alpha)*event.values[0];
                mGravity[1] = alpha * mGravity[1] + (1-alpha)*event.values[1];
                mGravity[2] = alpha * mGravity[2] + (1-alpha)*event.values[2];
            }

            if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1-alpha)*event.values[0];
                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1-alpha)*event.values[1];
                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1-alpha)*event.values[2];
            }

            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R,I,mGravity,mGeomagnetic);

            if(success){
                float orientation[] = new float[3];
                SensorManager.getOrientation(R,orientation);
                getInitialAzimuth = (float) Math.toDegrees(orientation[0]);
                getInitialAzimuth = (getInitialAzimuth+360)*360;

                Animation anim = new RotateAnimation(-currentAzimuth,-getInitialAzimuth,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
                currentAzimuth = getInitialAzimuth;

                anim.setDuration(500);
                anim.setRepeatCount(0);
                anim.setFillAfter(true);

                imageView.startAnimation(anim);
            }
        }*/

        //旋轉偵測

        /*
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

                    // 更新 UI
                    //updateCalibrateButton(timeRemaining);
                } else {  // 校準完成
                    // 計算平均值
                    gyroBiasTemp[0] /= calibrationCount;
                    gyroBiasTemp[1] /= calibrationCount;
                    gyroBiasTemp[2] /= calibrationCount;

                    // 複製到 gyroBias
                    System.arraycopy(gyroBiasTemp, 0, gyroBias, 0, gyroBiasTemp.length);

                    // 更新 UI
                    //binding.gyroBiasX.setText(String.format(Locale.getDefault(), "%.2f", gyroBias[0]));
                    //binding.gyroBiasY.setText(String.format(Locale.getDefault(), "%.2f", gyroBias[1]));
                    //binding.gyroBiasZ.setText(String.format(Locale.getDefault(), "%.2f", gyroBias[2]));
                    //updateCalibrateButton();

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

                imageView.setRotation((float) gyroRotation[2]);
                //imageView.setRotation(imageView.getRotation() + +(float) dz_deg);

                cellMap[user_x][user_y].setRotation((float) -gyroRotation[2]);

                // 更新 UI
                //binding.gyroX.setText(String.format(Locale.getDefault(), "%.2f", gyroValues[0]));
                //binding.gyroY.setText(String.format(Locale.getDefault(), "%.2f", gyroValues[1]));
                //binding.gyroZ.setText(String.format(Locale.getDefault(), "%.2f", gyroValues[2]));

                //updateProgressBar((int) gyroRotation[2]);
            }
        }
        */

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

            // 檢查是否有人臉偵測功能
            int[] iFaceDetModes = camChar.get(
                    CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
            if (iFaceDetModes == null) {
                mbFaceDetAvailable = false;
                Toast.makeText(MainActivity.this, "不支援人臉偵測", Toast.LENGTH_LONG)
                        .show();
            } else {
                mbFaceDetAvailable = false;
                for (int mode : iFaceDetModes) {
                    if (mode == CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE) {
                        mbFaceDetAvailable = true;
                        miFaceDetMode = CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE;
                        break;   // Find the desired mode, so stop searching.
                    } else if (mode == CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL) {
                        // This is a candidate mode, keep searching.
                        mbFaceDetAvailable = true;
                        miFaceDetMode = CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL;
                    }
                }
            }

            if (mbFaceDetAvailable) {
                miMaxFaceCount = camChar.get(
                        CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT);

                Toast.makeText(MainActivity.this, "人臉偵測功能: " +
                                String.valueOf(miFaceDetMode) +
                                "\n人臉數最大值: " + String.valueOf(miMaxFaceCount),
                        Toast.LENGTH_LONG)
                        .show();
            }

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
