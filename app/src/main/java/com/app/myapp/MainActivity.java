package com.app.myapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

//import androidx.compose.ui.graphics.BlendMode;
//import androidx.constraintlayout.widget.ConstraintLayout;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ResolutionInfo;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
//import android.support.annotation.DrawableRes;
//import android.support.annotation.NonNull;
//import android.support.constraint.ConstraintLayout;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.util.SizeF;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.ImageView;


import io.github.sceneview.SceneView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.tensorflow.lite.Interpreter;


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
    private final int REQUEST_PERMISSION_CAMERA = 1001;
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
                    if (askForPermissions());
                        // openCamera();
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
    // variables for IVP
    private IVP_Client ivpClient;
    HandlerThread fuseThread;
    Handler mainH ;
    Handler fuseH ;
    final Object lock = new Object();



    // =============================================== Wifi指紋 & PDR定位 ========================================
    private JoystickView joystickView2;

    private WifiManager wifiManager;
    private static final String MODEL_URL = "http://" + MainActivity2.SERVER_IP + ":5000/download_model";
    private static final String SCALER_URL = "http://" + MainActivity2.SERVER_IP + ":5000/download_scaler";
    private File modelFile, scalerFile;
    private Interpreter tflite;

    // Scalar
    float [] mean, scale;


    private final String[] targetSSIDs = {"CSIE-WLAN", "CSIE-WLAN-Sparq", "CSIE-WLAN-Office", "CSIE-MOUSE"};

    private static final long WIFI_SCAN_INTERVAL = 5000; // Wi-Fi 掃描間隔 (5 秒)

    int wifi_gridx = -1;
    int wifi_gridy = -1;
    int pdr_gridx, pdr_gridy;


    // ========================== PDR 定位 ======================================================================

    private SensorManager sensorManagerPDR;
    private Sensor accelerometer, magnetometer, gyroscopePDR;
    private TextView positionTextView, stepInfoTextView, scanCountTextView;
    private Button startScanningBtn, stopScanningBtn, sendToServerBtn, saveCsvBtn;

    private float[] gravity, geomagnetic;
    private float azimuth = 0f; // 方向角
    private float stepCount = 0;
    private float stepLength = 0.7f; // 假設每一步長 70cm
    private float pdr_x = 38, pdr_y = 30; // (x, y) 初始座標
    private List<String> collectedData = new ArrayList<>(); // 儲存 WiFi RSSI 和座標資料
    private int scanCount = 0; // 計算 WiFi 掃描次數

    // 動態步長估計
    private float total_length = 0;

    private List<Float> accZValues = new ArrayList<>();
    private List<Long> accZTimestamp = new ArrayList<>();
    private long peakTimestamp;
    private float peakValue;
    private long valleyTimestamp;
    private float valleyValue;

    private boolean find_valley = false, find_peak = false;

    private static final float PEAK_VALLEY_THRESHOLD = 1.2f; // 峰谷差距 (m/s²) 門檻
    private static final float ACC_THRESHOLD = 11.0f; // 步伐偵測基本門檻
    private static final long STEP_INTERVAL_THRESHOLD = 350; // 兩步之間的最小間格 (ms)

    // Complementary Filter Using Gyroscope
    private float[] gyro = new float[3];
    private float fusedYaw = 0f;
    private long lastUpdateTime_PDR = -1;
    private static final float FILTER_ALPHA = 0.9f; // complementary filter blending constant



    // ===============================================================================================================



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


    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ==================================== IVP ==============================================
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV init FAILED");      // fall back to OpenCV Manager or abort
        } else {
            Log.d(TAG, "OpenCV init OK");
        }


        // wait for the camera to start, anc setup ivpClient when init success.
        PreviewView previewView = findViewById(R.id.textureView);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // 没有权限，申请
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.CAMERA },
                    REQUEST_CAMERA_PERMISSION
            );
        } else {
            // 已经有权限，初始化相机并启动 IVP 循环
            ListenableFuture<Pair<ImageCapture, Mat>> camFuture =
                    startCamera(previewView);
            camFuture.addListener(() -> {
                try {
                    Pair<ImageCapture, Mat> result = camFuture.get();   // already finished here
                    ImageCapture imageCap = result.first;
                    Mat camMat            = result.second;

                    ivpClient = new IVP_Client(this, imageCap, camMat);
                } catch (Exception e) {
                    Log.e("CamInit", "camera failed", e);
                }
            }, ContextCompat.getMainExecutor(this));
            startIVPLoop();
        }


        // ==================================== WiFi 定位 & PDR 定位 ==============================================
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        loadModelAndPredict();

        // 初始化感測器
        sensorManagerPDR = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManagerPDR.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManagerPDR.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscopePDR = sensorManagerPDR.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // 開啟偵測器
        registerSensors();

        joystickView2 = findViewById(R.id.joystickView2);

        joystickView2.setOnMoveListener((dx, dy) -> {
            float adjustFactor = 0.8f; // 控制調整速度

            pdr_x += dx * adjustFactor;
            pdr_y += dy * adjustFactor;
            // 限制 x 和 y 在 0~57m 範圍內
            pdr_x = Math.max(0, Math.min(pdr_x, 57f));
            pdr_y = Math.max(0, Math.min(pdr_y, 57f));

            // thread safe version for later fusion
            pdrGridX = Math.round((pdr_x / 57f) * 99);
            pdrGridY = Math.round((pdr_y / 57f) * 99);
            pdrReady = true;

            updatePositionFused();
            // Toast.makeText(MainActivity.this, "dx = " + dx + "dy = " + dy, Toast.LENGTH_SHORT).show();

        });

        // =============================================================================================
        // =================================== fusion thread setup ==================================================
//        fuseThread = new HandlerThread("FusionThread",
//                android.os.Process.THREAD_PRIORITY_DEFAULT);
//        fuseThread.start();
//        mainH = new Handler(Looper.getMainLooper()); // 也可另開 HandlerThread
//        fuseH = new Handler(fuseThread.getLooper());
//        fuseH.post(fuseTask);

        SceneView sceneView = findViewById(R.id.sceneView);
        ARViewer.INSTANCE.setupSceneView(this, sceneView, (LifecycleOwner) this);
        ARViewer.INSTANCE.setModelTransform(0f,45f,0f);
        ARViewer.INSTANCE.setModelPosition(0f,-0.2f,0f);

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
//        mTextureView = findViewById(R.id.textureView);
        // mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);


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
                     updateUser(user_y, user_x);


//                     ivpClient.captureBurst(5, 120)
//                             .thenAccept(point -> {
//                                 Log.d("POSE", "x="+point.x+"  y="+point.y);
//                                 user_x = Math.round(point.x);
//                                 user_y = Math.round(point.y);
//                             });

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

    private final ArrayList<Point> points = new ArrayList<>();
    // 新增一個點
    public void addPoint(int x, int y) {
        points.add(new Point(x, y));
    }
    // 清除所有點
    public void clear() {
        points.clear();
    }


    private void updateUser(int x, int y) {
        GridMapView gridMapView = findViewById(R.id.gridMapView);
        user_x = x;
        user_y = y;
        now_x = 0;
        now_y = 0;
        Grid[][] escapeMap=fp.user_guide(user_x,user_y);
        gridMapView.post(() -> {
            // 清空地圖
//            for (int i = 0; i < 100; i++) {
//                for (int j = 0; j < 100; j++) {
//                    gridMapView.setCellImage(i, j, null);
//                    gridMapView.setCellScale(i, j, 1f);
//                    //gridMapView.setCellRotation(i,j,0);
//                    //gridMapView.gridToFront(i,j,0);
//                }
//            }
            for (Point p : points) {
                gridMapView.setCellImage(p.y,p.x,null);
                gridMapView.setCellScale(p.y,p.x,1f);
                gridMapView.setCellRotation(p.y,p.x,0);
                gridMapView.gridToFront(p.y,p.x,0);
            }
            clear();

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
            recalibrateMagneticNorth();
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
        // Log.d("yaju",dirToText(dir));
        addPoint(user_x,user_y);
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
        addPoint(x,y);
        GridMapView gridMapView = findViewById(R.id.gridMapView);
        gridMapView.gridToFront(y,x,1.2f);
        int dir=escapeMap[x][y].getDirection();
        // Log.d("yaju",dirToText(dir));
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

    public void showPath(int x,int y,int lastDirection,Grid[][] escapeMap){
        addPoint(x,y);
        GridMapView gridMapView = findViewById(R.id.gridMapView);
        gridMapView.gridToFront(y,x,1.0f);
        if (escapeMap[x][y].getType() == Grid.ROAD) {
            int dir = escapeMap[x][y].getDirection();
            // Log.d("yaju",dirToText(dir));
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
                addPoint(x+1,y);
                addPoint(x,y+1);
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
                addPoint(x+1,y);
                addPoint(x,y-1);
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
                addPoint(x-1,y);
                addPoint(x,y+1);
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
                addPoint(x-1,y);
                addPoint(x,y-1);
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
//        if (mTextureView.isAvailable()) {
//            if (askForPermissions()) ;
//                // openCamera();
//        }

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


    // 呼叫此方法以重新定位地磁方向（例如在切換場景後）
    public void recalibrateMagneticNorth() {
        isLastAccelerometerArrayCopied = false;
        isLastMagnetometerArrayCopied = false;
        lastUpdateTime = 0;
        currentDegree = 0f;

        // 清空 rotationMatrix 和 orientation（可選）
        rotationMatrix = new float[9];
        orientation = new float[3];

        // 若使用 AR 模型，也一併重置
        ARViewer.INSTANCE.setModelTransform(0f, 0f, 0f);

        // 若你的 gridMapView 也需要 reset
        GridMapView gridMapView = findViewById(R.id.gridMapView);
        gridMapView.setCellRotation(user_y, user_x, 0f);
    }

    private boolean isUsingMirroredModel = false;
    private float currentModelRotationY = 0f;

    @Override
    public void onSensorChanged(SensorEvent event) {

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
            float baseOffsetAngle = 21f;

            //相對當前格點的基準角度
            //關於GridMap的可視化
            Grid[][] escapeMap = fp.user_guide(user_x,user_y);
            int dir=escapeMap[user_x][user_y].getDirection();
            if (dir == Grid.UP_RIGHT) baseOffsetAngle += 45f;
            else if (dir == Grid.RIGHT) baseOffsetAngle += 90f;
            else if (dir == Grid.DOWN_RIGHT) baseOffsetAngle += 135f;
            else if (dir == Grid.DOWN) baseOffsetAngle += 180f;
            else if (dir == Grid.DOWN_LEFT) baseOffsetAngle += 225f;
            else if (dir == Grid.LEFT) baseOffsetAngle += 270f;
            else if (dir == Grid.UP_LEFT) baseOffsetAngle += 315f;

            // 計算調整後的角度，使其符合平面圖的方向
            float adjustedAzimuth = azimuthInDegree + baseOffsetAngle;
            // 確保角度保持在 0 - 360 之間
            if (adjustedAzimuth >= 360) {
                adjustedAzimuth %= 360;
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

            if(adjustedAzimuth>90&&adjustedAzimuth<270) {
                ARViewer.INSTANCE.loadModel("models/mirrow.glb");
                ARViewer.INSTANCE.setModelPosition(0f,-0.2f,0f);
                ARViewer.INSTANCE.setModelTransform(0f,adjustedAzimuth-180f,0f);
                isUsingMirroredModel=true;
            }
            else {
                ARViewer.INSTANCE.loadModel("models/direction_arrow.glb");
                ARViewer.INSTANCE.setModelPosition(0f,-0.2f,0f);
                ARViewer.INSTANCE.setModelTransform(0f,adjustedAzimuth,0f);
                isUsingMirroredModel=false;
            }
            gridMapView.setCellRotation(user_y,user_x,adjustedAzimuth);

            currentDegree = adjustedAzimuth;
            currentModelRotationY = 90-adjustedAzimuth;
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
                //ARViewer.INSTANCE.setModelTransform(0f,ARViewer.INSTANCE.getModelRotationY()+(float) dz_deg,0f);
                gridMapView.setCellRotation(user_y,user_x,gridMapView.getCellRotation(user_y,user_x) - (float) dz_deg);

                float rotationDelta = (float) -dz_deg;
                float temp = currentModelRotationY;
                currentModelRotationY += rotationDelta;

                boolean needSwitchModel = false;

                // 判斷是否要切換模型
                if (!isUsingMirroredModel && temp<90f &&currentModelRotationY > 90f) {
                    isUsingMirroredModel = true;
                    currentModelRotationY -= 180f;
                    needSwitchModel = true;
                }
                else if (!isUsingMirroredModel && temp>-90f &&currentModelRotationY < -90f) {
                    isUsingMirroredModel = true;
                    currentModelRotationY += 180f;
                    needSwitchModel = true;
                }
                else if (isUsingMirroredModel && currentModelRotationY > -90f && currentModelRotationY < 90f) {
                    isUsingMirroredModel = false;
                    currentModelRotationY += 180f;
                    needSwitchModel = true;
                }

                // 只有真的需要時才換模型
                if (needSwitchModel) {
                    String modelPath = isUsingMirroredModel ? "models/mirrow.glb" : "models/direction_arrow.glb";
                    ARViewer.INSTANCE.loadModel(modelPath);
                    ARViewer.INSTANCE.setModelPosition(0f, -0.2f, 0f);
                }

                // 無論是否換模型，更新旋轉
                ARViewer.INSTANCE.setModelTransform(0f, currentModelRotationY, 0f);
            }
        }


        // ================================== PDR定位 ===================================================

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                gravity = event.values.clone();
                detectStep(event.values);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                geomagnetic = event.values.clone();
                break;

            case Sensor.TYPE_GYROSCOPE:
                gyro = event.values.clone();
                break;
        }

        if (gravity != null && geomagnetic != null) {
            float deltaT = (lastUpdateTime_PDR > 0) ? (currentTime - lastUpdateTime_PDR) / 1000f : 0f; // seconds

            // Get yaw from magnetometer + accelerometer
            float[] R = new float[9];
            float[] I = new float[9];
            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                float yawMagAcc = (float) Math.toDegrees(orientation[0]);
                if (yawMagAcc < 0) yawMagAcc += 360;

                // Get yaw from gyroscope
                float gyroZ = gyro[2]; // 角速度
                float deltaYawGyro = gyroZ * deltaT;

                if (lastUpdateTime_PDR > 0) {
                    // fusedYaw = FILTER_ALPHA * (fusedYaw + deltaYawGyro) + (1 - FILTER_ALPHA) * yawMagAcc;
                    float predictedYaw = fusedYaw + deltaYawGyro;
                    float angleDiff = ((yawMagAcc - predictedYaw + 540) % 360) - 180;
                    fusedYaw = predictedYaw + (1 - FILTER_ALPHA) * angleDiff;

                    if (fusedYaw < 0) fusedYaw += 360;
                    if (fusedYaw >= 360) fusedYaw -= 360;
                } else {
                    fusedYaw = yawMagAcc;
                }

                azimuth = fusedYaw;
                // azimuthArrowView.setAzimuth(360 - azimuth);
            }
            lastUpdateTime_PDR = currentTime;
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


    private ListenableFuture<Pair<ImageCapture, Mat>> startCamera(PreviewView previewView) {
        SettableFuture<Pair<ImageCapture, Mat>> future = SettableFuture.create();
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
                String cameraId = getCameraId(CameraSelector.LENS_FACING_BACK);

                CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                Rect activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                SizeF sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                float[] focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);

                int fullWidth = activeArray.width();
                int fullHeight = activeArray.height();

                Log.d("Sensor", "Sensor size: " + sensorSize + " | Active array: " + fullWidth + "x" + fullHeight);

                int maxW = 1000;
                int maxH = 1000;

                // Calculate scale to fit within 1000x1000 while maintaining aspect ratio
                float scale = Math.min((float) maxW / fullWidth, (float) maxH / fullHeight);
                int scaledWidth = Math.round(fullWidth * scale);
                int scaledHeight = Math.round(fullHeight * scale);

                Size preferredSize = new Size(scaledWidth, scaledHeight);
                Log.d("AutoPreferredSize", "Scaled preferred size: " + preferredSize.getWidth() + "x" + preferredSize.getHeight());

                // Build ResolutionSelector
                ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                        .setResolutionStrategy(
                                new ResolutionStrategy(
                                        preferredSize,
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                                )
                        )
                        .build();

                // Set up the preview use case
                PreviewView preview = new PreviewView(this);
                androidx.camera.core.Preview cameraPreview = new androidx.camera.core.Preview.Builder()
                        .build();

                cameraPreview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Configure ImageCapture for 640x480 resolution
                ImageCapture imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setResolutionSelector(resolutionSelector)
                        .build();

                // Bind use cases to the lifecycle
                Camera camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, cameraPreview, imageCapture
                );
                ResolutionInfo info = imageCapture.getResolutionInfo();
                Size actualSize = info.getResolution();
                Log.d("FinalResolution", "Selected resolution: " + actualSize.getWidth() + "x" + actualSize.getHeight());
                Mat cameraMatrix = computeCameraMatrix(cameraId, actualSize);

                future.set(new Pair<>(imageCapture, cameraMatrix));

            } catch (Exception e) {
                e.printStackTrace();
                future.setException(e);
            }
        }, ContextCompat.getMainExecutor(this));

        return future;
    }

    private String getCameraId(int lensFacing) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == lensFacing) {
                    return id;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Mat computeCameraMatrix(String cameraId, Size actualSize) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Mat cameraMatrix = new Mat(3, 3, CvType.CV_64F);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            float[] focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            SizeF sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
            Rect activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

            int targetWidth = actualSize.getWidth();
            int targetHeight = actualSize.getHeight();

            double fx, fy, cx, cy;
            // Use intrinsic calibration if available.
            float[] intrinsics = characteristics.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION);
            if (intrinsics != null && intrinsics.length >= 5) {
                float calibFx = intrinsics[0];
                float calibFy = intrinsics[1];
                float calibCx = intrinsics[2];
                float calibCy = intrinsics[3];

                float scaleX = (float) targetWidth / activeArray.width();
                float scaleY = (float) targetHeight / activeArray.height();

                fx = calibFx * scaleX;
                fy = calibFy * scaleY;
                cx = calibCx * scaleX;
                cy = calibCy * scaleY;
            } else if (focalLengths != null && focalLengths.length > 0 && sensorSize != null) {
                // Fallback: compute from focal length and sensor size.
                float focalLength = focalLengths[0];
                float sensorWidth = sensorSize.getWidth();
                float sensorHeight = sensorSize.getHeight();

                fx = (focalLength * targetWidth) / sensorWidth;
                fy = (focalLength * targetHeight) / sensorHeight;
                cx = targetWidth / 2.0;
                cy = targetHeight / 2.0;
            } else {
                Log.e("CameraMatrix", "Focal length or sensor size not available.");
                return null;
            }

            // Fill in the camera matrix.
            cameraMatrix.put(0, 0, fx);
            cameraMatrix.put(0, 1, 0);
            cameraMatrix.put(0, 2, cx);
            cameraMatrix.put(1, 0, 0);
            cameraMatrix.put(1, 1, fy);
            cameraMatrix.put(1, 2, cy);
            cameraMatrix.put(2, 0, 0);
            cameraMatrix.put(2, 1, 0);
            cameraMatrix.put(2, 2, 1);

            Log.d("CameraMatrix", String.format("Camera Matrix: \n[[%.2f, 0, %.2f], [0, %.2f, %.2f], [0, 0, 1]]", fx, cx, fy, cy));
            return cameraMatrix;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ====================================================== Wifi 定位 ===================================================================

    private void loadModelAndPredict() {
        File modelDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        modelFile = new File(modelDir, "model.tflite");
        scalerFile = new File(modelDir, "scaler.json");

        // runOnUiThread(() -> Toast.makeText(this, modelFile.exists() == true ? "Exist" : "Doesn't exist", Toast.LENGTH_SHORT).show());

        if (modelFile.exists() && scalerFile.exists()) {
            new Thread(() -> {
                try {
                    tflite = new Interpreter(loadModelFile(modelFile));
                    loadScaler(scalerFile);
                    runOnUiThread(() -> Toast.makeText(this, "Model successfully loaded", Toast.LENGTH_SHORT).show());
                    scanAndPredict();
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(this, "Failed to load model", Toast.LENGTH_SHORT).show());
                }
            }).start();
        } else {
            downloadAndLoadModel();
        }
    }

    private void loadScaler(File scalerFile) throws IOException, JSONException {
        FileInputStream fis = new FileInputStream(scalerFile);
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(fis));


        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();

        JSONObject json = new JSONObject(sb.toString());
        JSONArray meanJson = json.getJSONArray("mean");
        JSONArray scaleJson = json.getJSONArray("scale");

        mean = new float[meanJson.length()];
        scale = new float[scaleJson.length()];

        for (int i = 0; i < meanJson.length(); i++) {
            mean[i] = (float) meanJson.getDouble(i);
            scale[i] = (float) scaleJson.getDouble(i);
        }
    }

    private void downloadAndLoadModel() {
        new Thread(() -> {
            try {
                URL model_url = new URL(MODEL_URL);
                HttpURLConnection model_connection = (HttpURLConnection) model_url.openConnection();
                model_connection.connect();

                File modelDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                modelFile = new File(modelDir, "model.tflite");

                Log.d("ModelDownload", "Downloaded model size = " + modelFile.length());

                try (BufferedInputStream in = new BufferedInputStream(model_connection.getInputStream());
                     FileOutputStream out = new FileOutputStream(modelFile)) {
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = in.read(buffer)) != -1) {
                        out.write(buffer, 0, count);
                    }
                }

                URL scaler_url = new URL(SCALER_URL);
                HttpURLConnection scalar_connection = (HttpURLConnection) scaler_url.openConnection();
                scalar_connection.connect();

                scalerFile = new File(modelDir, "scaler.json");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(scalar_connection.getInputStream()));
                     FileOutputStream out = new FileOutputStream(scalerFile)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        out.write(line.getBytes());
                    }
                }

                tflite = new Interpreter(loadModelFile(modelFile));
                loadScaler(scalerFile);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Model downloaded and loaded", Toast.LENGTH_SHORT).show();
                    scanAndPredict();
                });

            } catch (Exception e) {
                Log.e("ModelDownload", "Error", e);
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Model download failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private MappedByteBuffer loadModelFile(File file) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = 0;
        long declaredLength = file.length();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void scanAndPredict() {
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        registerReceiver(wifiScanReceiver, new android.content.IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        startScanLoop();
    }

    volatile int wifiGridX = -1, wifiGridY = -1;
    volatile boolean wifiReady = false;
    volatile float wifiX, wifiY;

    private void predict(float[] inputRssi) {
        if (tflite == null) {
            Toast.makeText(this, "Model not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < inputRssi.length; i++) {
            inputRssi[i] = (inputRssi[i] - mean[i]) / scale[i];
        }

        float[][] output = new float[1][2];
        tflite.run(inputRssi, output);
        wifiX = output[0][0];
        wifiY = output[0][1];

        // runOnUiThread(() -> Toast.makeText(this, "x = " + x + "y = " + y, Toast.LENGTH_SHORT).show());

        // 將 0~57 m 映射到 0~99 的格點
//        wifi_gridx = Math.max(0, Math.min(99, Math.round((x / 57f) * 99)));
//        wifi_gridy = Math.max(0, Math.min(99, Math.round((y / 57f) * 99)));


        // thread safe version for later fusion
        wifiGridX = Math.max(0, Math.min(99, Math.round((wifiX / 57f) * 99)));
        wifiGridY = Math.max(0, Math.min(99, Math.round((wifiY / 57f) * 99)));
        wifiReady = true;

    }

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) context, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return;
            }
            List<ScanResult> results = wifiManager.getScanResults();

            // 儲存 RSSI 加總與次數
            HashMap<String, Integer> sumMap = new HashMap<>();
            HashMap<String, Integer> countMap = new HashMap<>();

            for (ScanResult result: results) {
                String ssid = result.SSID;
                int rssi = result.level;

                sumMap.put(result.SSID, sumMap.getOrDefault(ssid, 0) + rssi);
                countMap.put(ssid, countMap.getOrDefault(ssid, 0) + 1);
            }

            float[] inputRssi = new float[targetSSIDs.length];
            for (int i = 0; i < targetSSIDs.length; i++) {
                String ssid = targetSSIDs[i];
                if (sumMap.containsKey(ssid)) {
                    float avevrage = (float) sumMap.get(ssid) / countMap.get(ssid);
                    inputRssi[i] = avevrage;
                } else {
                    inputRssi[i] = -100f; // 沒掃到就補 -100
                }

//                final int index = i;
//                runOnUiThread(() -> Toast.makeText(context, "SSID: + " + targetSSIDs[index] + ", RSSI: " + inputRssi[index], Toast.LENGTH_SHORT).show());
            }

            predict(inputRssi);
        }
    };

    private void startScanLoop() {
        registerReceiver(wifiScanReceiver, new android.content.IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        handler.post(wifiScanRunnable); // 開始循環
    }

    private Runnable wifiScanRunnable = new Runnable() {
        @Override
        public void run() {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return;
            }
            wifiManager.startScan(); // 觸發掃描，結果由 onReceive 處理
            handler.postDelayed(this, WIFI_SCAN_INTERVAL); // 安排下一次
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 移除所有 handler 任務，防止掃描繼續
        handler.removeCallbacksAndMessages(null);
        ivpScheduler.shutdownNow();
        fuseThread.quitSafely();
        unregisterReceiver(wifiScanReceiver);
        unregisterSensors();
    }


    // 註冊感測器監聽
    private void registerSensors() {
        sensorManagerPDR.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI, 10000);
        sensorManagerPDR.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI, 10000);
        sensorManagerPDR.registerListener(this, gyroscopePDR, SensorManager.SENSOR_DELAY_UI, 10000);
    }

    // 取消感測器監聽
    private void unregisterSensors() {
        sensorManager.unregisterListener(this);
    }


    // 主偵測器 使用動態步長估計
    private void detectStep(float[] values) {
        if (gravity == null) return;

        // Normalize gravity 向量
        float gNorm = (float) Math.sqrt(gravity[0]*gravity[0] + gravity[1]*gravity[1] + gravity[2]*gravity[2]);
        float gx = gravity[0] / gNorm;
        float gy = gravity[1] / gNorm;
        float gz = gravity[2] / gNorm;

        // 加速度投影到 gravity 方向（即地面垂直方向）
        float accZ = values[0]*gx + values[1]*gy + values[2]*gz;

        // float acceleration = (float) Math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2]);

        long currentTime = System.currentTimeMillis();

        // 更新 peak valley 列表
        updatePeakValley(accZ, currentTime);

        // 每次新資料到就嘗試偵測步伐
        checkAndDetectStep();

    }

    // 更新 peak / valley 資訊
    private void updatePeakValley(float accZ, long timestamp) {
        int n = accZValues.size();

        if (n >= 2) {
            float lastAccZ = accZValues.get(n - 1);
            float secondLastAccZ = accZValues.get(n - 2);

            // 上升轉折: lastAcc 是 valley
            if (lastAccZ <= secondLastAccZ && lastAccZ <= accZ) {
                if (!find_valley) {
                    find_valley = true;
                    valleyValue = lastAccZ;
                    valleyTimestamp = accZTimestamp.get(n - 1);
                } else {
                    if (lastAccZ < valleyValue) {
                        valleyValue = lastAccZ;
                        valleyTimestamp = accZTimestamp.get(n - 1);
                    }
                }
            }
            // 下降轉折: lastAcc 是 peak
            if (lastAccZ >= secondLastAccZ && lastAccZ >= accZ) {
                if (!find_peak) {
                    find_peak = true;
                    peakValue = lastAccZ;
                    peakTimestamp = accZTimestamp.get(n - 1);
                } else {
                    if (lastAccZ > valleyValue) {
                        peakValue = lastAccZ;
                        peakTimestamp = accZTimestamp.get(n - 1);
                    }
                }
            }
        }

        accZValues.add(accZ);
        accZTimestamp.add(timestamp);
        // 防止無限成長
        if (accZValues.size() > 100) accZValues.remove(0);
        if (accZTimestamp.size() > 100) accZTimestamp.remove(0);
    }

    // 根據 peak / valley 資料判斷是否形成步伐
    private void checkAndDetectStep() {
        if (find_peak && find_valley) {
            // 峰谷差
            float amplitude = Math.abs(peakValue - valleyValue);

            // 判斷是否符合步伐條件
            if (amplitude > PEAK_VALLEY_THRESHOLD && Math.abs(peakTimestamp - valleyTimestamp) > STEP_INTERVAL_THRESHOLD && peakValue > 10 && valleyValue < 9.5) {
                stepCount++;

                // stepInfoTextView.setText("");

                // Weinberg 步長估算公式
                stepLength = 0.4f * (float) Math.pow(amplitude, 0.25);
                total_length += stepLength;
                // Toast.makeText(this, "Step Length: " + stepLength, Toast.LENGTH_SHORT).show();
//                stepInfoTextView.setText("peakValue = " + peakValue + "\n"
//                                        + "valleyValue = " + valleyValue + "\n"
//                                        + "time = " + Math.abs(peakTimestamp - valleyTimestamp) + "\n"
//                                        + "total Length = " + total_length);

                updatePosition();

                // 計算新的步伐
                find_valley = false;
                find_peak = false;
                accZValues.clear();
                accZTimestamp.clear();
            }
        }
    }

    // 更新 x, y 座標
    volatile int pdrGridX  = -1;
    volatile int pdrGridY  = -1;
    volatile boolean pdrReady = false;
    private void updatePosition() {
        double radian = Math.toRadians(azimuth);
        pdr_x += stepLength * Math.sin(radian);
        pdr_y -= stepLength * Math.cos(radian);
        // 限制 x 和 y 在 0~57m 範圍內
        pdr_x = Math.max(0, Math.min(pdr_x, 57f));
        pdr_y = Math.max(0, Math.min(pdr_y, 57f));


        // thread safe version for later fusion
        pdrGridX = Math.round((pdr_x / 57f) * 99);
        pdrGridY = Math.round((pdr_y / 57f) * 99);
        P += SIGMA_STEP * SIGMA_STEP;
        pdrReady = true;

        updatePositionFused();
    }



    // IVP part-----------------------------------------------------------------------------------------
    volatile int ivpGridX  = -1;
    volatile int ivpGridY  = -1;
    volatile float ivpX, ivpY;
    volatile boolean ivpReady =  false;     // 毫秒

    ScheduledExecutorService ivpScheduler = Executors.newSingleThreadScheduledExecutor();
    void startIVPLoop() {
        Log.d("IVP_START", "ivp capture routine started");
        ivpScheduler.scheduleWithFixedDelay(() -> {
            try {
                ivpClient.captureBurst(5, 120)
                        .whenComplete((pt, ex) -> {
                            if (ex != null) {
                                Log.e("IVP", "burst failed", ex);
                            } else {
                                ivpX = pt.x;
                                ivpY = pt.y;
                                int gx = Math.round((pt.x / 57f) * 99);
                                int gy = Math.round((pt.y / 57f) * 99);

                                ivpGridX = gx;
                                ivpGridY = gy;
                                ivpReady = true;

                                Log.d("IVP", "RESPONSE → x=" + gx + " y=" + gy);
                            }
                        });
            } catch (Throwable t) {
                Log.e("IVP", "sync exception in scheduled task", t);
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            // 仅针对 CAMERA 权限请求
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PreviewView previewView = findViewById(R.id.textureView);
                // 用户同意了摄像头权限，开始初始化相机并启动 IVP
                ListenableFuture<Pair<ImageCapture, Mat>> camFuture =
                        startCamera(previewView);
                camFuture.addListener(() -> {
                    try {
                        Pair<ImageCapture, Mat> result = camFuture.get();   // already finished here
                        ImageCapture imageCap = result.first;
                        Mat camMat            = result.second;

                        ivpClient = new IVP_Client(this, imageCap, camMat);
                    } catch (Exception e) {
                        Log.e("CamInit", "camera failed", e);
                    }
                }, ContextCompat.getMainExecutor(this));
                startIVPLoop();
            } else {
                // 用户拒绝了权限，提示一下
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }



    // fusion part--------------------------------------------------------------------------------------
    float  P      = 1.0f;          // PDR 方差 (m²)
    final float SIGMA_STEP = 0.25f;// 每步 1σ ≈ 0.25 m
    final float MAX_DISTANCE_THRESHOLD = 5.0f; // 最大可接受偏移距離 (m)

    float  sigmaW, sigmaI;
    float xFused, yFused; // 公尺

     private void updatePositionFused() {
         int fused_gridx, fused_gridy;

         if (sigmaW > 2*Math.sqrt(P)) wifiReady = false;
         if (sigmaI > 2*Math.sqrt(P)) ivpReady = false;

         boolean okP = pdrReady;
         boolean okW = wifiReady;
         boolean okI = ivpReady;

         // wifi 位置與上一次偏差超過 3m 則不採用
         if (okW) {
             float dxW = wifiX - xFused;
             float dyW = wifiY - yFused;
             if (Math.hypot(dxW, dyW) > MAX_DISTANCE_THRESHOLD) okW = false;
         }
         // visual 位置與上一次偏差超過 3m 則不採用
         if (okI) {
             float dxI = ivpX - xFused;
             float dyI = ivpY - yFused;
             if (Math.hypot(dxI, dyI) > MAX_DISTANCE_THRESHOLD) okI = false;
         }

         if (!okP && !okW && !okI) {        // 沒東西可融合
             return;
         }

         /* ---------- 計算權重 ---------- */
         float wP = okP ? 1f / P : 0f;
         float wW = okW ? 1f / (sigmaW * sigmaW) : 0f;
         float wI = okI ? 1f / (sigmaI * sigmaI) : 0f;

         float sum = wP + wW + wI;
         wP /= sum;  wW /= sum;  wI /= sum;

         /* ---------- 加權平均 ---------- */
         xFused = wP * pdr_x + wW * wifiX + wI * ivpX;
         yFused = wP * pdr_y + wW * wifiY + wI * ivpY;



         fused_gridx = Math.round((xFused / 57f) * 99);
         fused_gridy = Math.round((yFused / 57f) * 99);

         // 更新 pdr 位置
         pdr_x = xFused;
         pdr_y = yFused;

         /* ---------- 更新 P ---------- */
         // bayes: P_new = 1 / (1/P + Σ 1/σ²)
         P = 1f / ( (okP?1f/P:0f) +
                 (okW?1f/(sigmaW*sigmaW):0f) +
                 (okI?1f/(sigmaI*sigmaI):0f) );

         /* ---------- 清 ready 旗標 ---------- */
         pdrReady = wifiReady = ivpReady = false;

         updateUser(fused_gridy, fused_gridx);
     }

//    final Runnable fuseTask = new Runnable() {
//        @Override public void run() {
//            if (sigmaW > 2*Math.sqrt(P)) wifiReady = false;
//            if (sigmaI > 2*Math.sqrt(P)) ivpReady = false;
//
//            synchronized (lock) {
//                boolean okP = pdrReady;
//                boolean okW = wifiReady;
//                boolean okI = ivpReady;
//
//                gxP = (float) pdrGridX;
//                gyP = (float) pdrGridY;
//                gxW = (float) wifiGridX;
//                gyW = (float) wifiGridY;
//                gxI = (float) ivpGridX;
//                gyI = (float) ivpGridY;
//
//                // wifi 位置與上一次偏差超過5格則不採用
//                if (okW) {
//                    float dxW = gxW - xFused;
//                    float dyW = gyW - yFused;
//                    if (Math.hypot(dxW, dyW) > MAX_DISTANCE_THRESHOLD) okW = false;
//                }
//                // visual 位置與上一次偏差超過5格則不採用
//                if (okI) {
//                    float dxI = gxI - xFused;
//                    float dyI = gyI - yFused;
//                    if (Math.hypot(dxI, dyI) > MAX_DISTANCE_THRESHOLD) okI = false;
//                }
//
//                if (!okP && !okW && !okI) {        // 沒東西可融合
//                    fuseH.postDelayed(this, 200);  // 200 ms 再試
//                    return;
//                }
//
//                /* ---------- 計算權重 ---------- */
//                float wP = okP ? 1f / P : 0f;
//                float wW = okW ? 1f / (sigmaW * sigmaW) : 0f;
//                float wI = okI ? 1f / (sigmaI * sigmaI) : 0f;
//
//                float sum = wP + wW + wI;
//                wP /= sum;  wW /= sum;  wI /= sum;
//
//                /* ---------- 加權平均 ---------- */
//                xFused = wP*gxP + wW*gxW + wI*gxI;
//                yFused = wP*gyP + wW*gyW + wI*gyI;
//
//                /* ---------- 更新 P ---------- */
//                // bayes: P_new = 1 / (1/P + Σ 1/σ²)
//                P = 1f / ( (okP?1f/P:0f) +
//                        (okW?1f/(sigmaW*sigmaW):0f) +
//                        (okI?1f/(sigmaI*sigmaI):0f) );
//
//                /* ---------- 清 ready 旗標 ---------- */
//                pdrReady = wifiReady = ivpReady = false;
//
////                // 將 fusedX fusedY 換回公尺並且更新 pdr 位置
////                pdr_x = Math.max(0f, Math.min(57f, (xFused / 99f) * 57f));
////                pdr_y = Math.max(0f, Math.min(57f, (yFused / 99f) * 57f));
//            }
//
//            /* ------- 上主執行緒改 UI ------- */
//            final int fx = Math.round(xFused);
//            final int fy = Math.round(yFused);
//            mainH.post(() -> updateUser(fx, fy));
////            mainH.post(() -> {
////                // 印出三個來源的座標值（格子）
////                Toast.makeText(MainActivity.this,
////                        "PDR: (" + gxP + ", " + gyP + ") " +
////                                "WiFi: (" + gxW + ", " + gyW + ") " +
////                                "Visual: (" + gxI + ", " + gyI + ")",
////                        Toast.LENGTH_SHORT).show();
////
////            });
////            mainH.post(() -> updateUser(30, 30));
//
//            fuseH.postDelayed(this, 200); // 5Hz
//        }
//    };
}
