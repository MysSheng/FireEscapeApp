package com.app.myapp;

import android.Manifest;
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
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.OrientationEventListener;
import android.view.ViewGroup;
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


//import androidx.appcompat.app.AppCompatActivity;

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

    private int user_x = 9;
    private int user_y = 45;

    private float gridWidth;
    private float gridHeight;

    private float gridSize = 0; // 儲存 Grid 的大小

    private ImageView userMarker; // 新增的使用者箭頭圖示
    //private GridLayout gridLayout = findViewById(R.id.gridLayout);
    private GridLayout gridLayout ;




    private Matrix matrix = new Matrix();

    private float scaleFactor = 1.0f;
    private float translateX = 0f;
    private float translateY = 0f;
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
        Grid[][] grid = new Grid[100][100];
        int fire_x = 27, fire_y = 0;
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                grid[i][j] = new Grid(i, j);
            }
        }
        for (int i = 0; i < 72; i++) {
            grid[i][0].setType(Grid.WALL);
            grid[i][24].setType(Grid.WALL);
            grid[i][39].setType(Grid.WALL);
            grid[i][63].setType(Grid.WALL);
        }
        for (int i = 0; i < 27; i++) {
            grid[0][i].setType(Grid.WALL);
            grid[24][i].setType(Grid.WALL);
            grid[48][i].setType(Grid.WALL);
            grid[69][i].setType(Grid.WALL);
        }
        for (int i = 39; i < 66; i++) {
            grid[0][i].setType(Grid.WALL);
            grid[24][i].setType(Grid.WALL);
            grid[48][i].setType(Grid.WALL);
            grid[69][i].setType(Grid.WALL);
        }
        for (int i = 51; i < 69; i++) {
            grid[i][24].setType(Grid.ROAD);
            grid[i][39].setType(Grid.ROAD);
        }
        grid[69][27].setType(Grid.WALL);
        grid[69][36].setType(Grid.WALL);
        grid[63][27].setType(Grid.WALL);
        grid[66][27].setType(Grid.WALL);
        grid[63][36].setType(Grid.WALL);
        grid[66][36].setType(Grid.WALL);
        grid[69][30].setType(Grid.EXIT);
        grid[48][27].setType(Grid.EXIT);
        grid[48][36].setType(Grid.EXIT);
        grid[18][24].setType(Grid.EXIT);
        grid[18][39].setType(Grid.EXIT);
        grid[30][24].setType(Grid.EXIT);
        grid[30][39].setType(Grid.EXIT);
        grid[0][30].setType(Grid.EXIT);
        grid[24][12].setType(Grid.EXIT);
        grid[27][0].setType(Grid.FIRE);
        Planner fp = new Planner(grid, user_x, user_y, fire_x, fire_y);
        fp.addRoom(new Room(1, 0, 0, 27, 27));
        fp.addRoom(new Room(2, 24, 0, 27, 27));
        fp.addRoom(new Room(3, 48, 0, 24, 36));
        fp.addRoom(new Room(4, 0, 24, 51, 18));
        fp.addRoom(new Room(5, 0, 39, 27, 27));
        fp.addRoom(new Room(6, 24, 39, 27, 27));
        fp.addRoom(new Room(7, 48, 30, 24, 36));
        fp.setRunSpeed(2);
        //fp.do_one_level();
        fp.do_task();
        fp.testNavigator();
        ////////// fp.testEdge();
        fp.testOutgoingEdge();
        ////////// fp.user_guide(user_x, user_y);

        //關於GridMap的可視化
        Grid[][] escapeMap =fp.returnMap();
        //Grid[][] escapeMap = fp.getGridMap();

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
                applyTransformation(imageView,gridMapView);

                // 設定 GridMap 大小與圖片匹配
                gridMapView.setGridSize(imgWidth, imgHeight);

                // 調整 GridMapView 的大小
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(imgWidth, imgHeight);
                gridMapView.setLayoutParams(params);
            }
        });

        Bitmap userBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.user_sign);
        Bitmap roadBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.u_d_removebg);
        gridMapView.setCellImage(13, 39, userBitmap);
        for(int i=0;i<=38;i++){
            gridMapView.setCellImage(13, i, roadBitmap);
        }
        gridMapView.setCellScale(13, 39, 3.5f);
        //gridMapView.setCellRotation(13, 39, 45.0f);

        for(int i=0;i<100;i++){
            for(int j=0;j<100;j++){
                gridMapView.setGridVisibility(false);
            }
        }

        /*

        gridLayout = findViewById(R.id.gridLayout);
        gridLayout.setRowCount(rowCount);
        gridLayout.setColumnCount(columnCount);
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnCount; j++) {
                ImageView cell = new ImageView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = 0;
                params.rowSpec = GridLayout.spec(i, 1f);
                params.columnSpec = GridLayout.spec(j, 1f);
                cell.setLayoutParams(params);
                cell.setBackgroundResource(R.drawable.grid_cell); // 設定背景為白底黑框
                GradientDrawable drawable = (GradientDrawable) cell.getBackground();
                drawable.setColor(changeColor(escapeMap[i][j].getType()));
                drawable.setStroke(0, Color.TRANSPARENT); // 移除邊線

                //用於變換完成的還原
                //ImageView temp = new ImageView(this);
                //temp.setLayoutParams(params);
                //temp.setBackground(cell.getBackground()); // 複製背景

                // 建立深度拷貝
                ImageView temp = deepCopyImageView(cell);

                if(escapeMap[i][j].isSelected()&&(escapeMap[i][j].getX()!=user_x||escapeMap[i][j].getY()!=user_y))
                    drawable.setColor(Color.YELLOW);
                //點擊變換
                int finalI = i;
                int finalJ = j;
                cell.setOnClickListener(v -> {
                    //還原
                    //cellMap[user_x][user_y].setBackgroundColor(Color.TRANSPARENT);
                    //params.width/=30;
                    //params.height/=30;
                    //cellMap[user_x][user_y].setLayoutParams(params);
                    //cellMap[user_x][user_y].setLayoutParams(params);
                    //cellMap[user_x][user_y].setImageResource(-1);
                    cellMap[9][45]=temp;
                    //更新
                    cell.setImageResource(R.drawable.user_sign);
                    cell.setBackgroundColor(Color.WHITE);
                    GridLayout.LayoutParams paramsPlus = (GridLayout.LayoutParams) cellMap[user_x][user_y].getLayoutParams();
                    paramsPlus.width = 30;  // 讓這格變大
                    paramsPlus.height = 30;
                    cell.setLayoutParams(paramsPlus);
                    user_x=finalI;
                    user_y=finalJ;
                });
                gridLayout.addView(cell);
                cellMap[i][j] = cell; // 存入二維陣列
            }

        }

        setUserLoc(user_x,user_y);



         */

        /*
        //Button btnTakePicture = findViewById(R.id.btnTakePicture);
        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });*/
    }

    private void applyTransformation(ImageView imageView,GridMapView gridMapView) {
        matrix.reset();
        matrix.postScale(scaleFactor, scaleFactor);
        matrix.postTranslate(translateX, translateY);

        imageView.setImageMatrix(matrix);
        gridMapView.setTransformMatrix(matrix);
    }

    private ImageView deepCopyImageView(ImageView original) {
        ImageView copy = new ImageView(original.getContext());

        // 複製 LayoutParams
        GridLayout.LayoutParams originalParams = (GridLayout.LayoutParams) original.getLayoutParams();
        GridLayout.LayoutParams copyParams = new GridLayout.LayoutParams(originalParams);
        copy.setLayoutParams(copyParams);

        // 複製背景
        Drawable originalBackground = original.getBackground();
        if (originalBackground != null) {
            copy.setBackground(originalBackground.getConstantState().newDrawable().mutate());
            copy.setBackground(null);
        }

        // 複製圖片 (如果有設定)
        Drawable originalDrawable = original.getDrawable();
        if (originalDrawable != null) {
            copy.setImageDrawable(originalDrawable.getConstantState().newDrawable().mutate());
            copy.setImageDrawable(null);
        }

        // 複製其他屬性
        copy.setPadding(original.getPaddingLeft(), original.getPaddingTop(),
                original.getPaddingRight(), original.getPaddingBottom());
        copy.setScaleType(original.getScaleType());
        copy.setContentDescription(original.getContentDescription());

        return copy;
    }

    private void setUserLoc(int x,int y){
        //cellMap[user_x][user_y].setBackgroundColor(Color.WHITE);
        //更改新格子
        cellMap[x][y].setImageResource(R.drawable.user_sign);
        cellMap[x][y].setBackgroundColor(Color.WHITE);
        GridLayout.LayoutParams params = (GridLayout.LayoutParams) cellMap[user_x][user_y].getLayoutParams();
        params.width = (int) 30;  // 讓這格變大
        params.height = (int) 30;
        cellMap[x][y].setLayoutParams(params);
        user_x=x;
        user_y=y;
    }

    // 計算 Grid 大小後更新使用者箭頭
    private void updateUserMarkerSize() {
        if (gridSize > 0) {
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams((int) gridSize, (int) gridSize);
            userMarker.setLayoutParams(params);
            userMarker.setVisibility(View.VISIBLE);
        }
    }

    // 移動使用者位置
    public void moveUser(int x, int y) {
        if (gridSize == 0) return; // 確保 Grid 大小已經計算好

        userMarker.setX(gridLayout.getX() + (y * gridSize));
        userMarker.setY(gridLayout.getY() + (x * gridSize));
    }

    private int changeColor(int type){
        if(type==Grid.WALL) return Color.BLACK;
        else if(type==Grid.FIRE) return Color.RED;
        else if(type==Grid.ROAD) return Color.WHITE;
        else if(type==Grid.EXIT) return Color.BLUE;
        else return Color.WHITE;
    }

    // 將 direction 數字轉換為文字表示
    private static String getDirectionText(int direction) {
        switch (direction) {
            case Grid.UP:
                return "↑";
            case Grid.DOWN:
                return "↓";
            case Grid.LEFT:
                return "←";
            case Grid.RIGHT:
                return "→";
            case Grid.UP_LEFT:
                return "↖";
            case Grid.UP_RIGHT:
                return "↗";
            case Grid.DOWN_LEFT:
                return "↙";
            case Grid.DOWN_RIGHT:
                return "↘";
            default:
                return "x"; // 無方向
        }
    }

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

    private void takePicture() {
        if(mCameraDevice == null) {
            Toast.makeText(MainActivity.this, "Camera錯誤", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        // 準備影像檔
        final File file = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .getPath(), "photo.jpg");

        // 準備OnImageAvailableListener
        ImageReader.OnImageAvailableListener imgReaderOnImageAvailable =
                new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader imageReader) {
                        // 把影像資料寫入檔案
                        Image image = null;
                        try {
                            image = imageReader.acquireLatestImage();
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[buffer.capacity()];
                            buffer.get(bytes);

                            OutputStream output = null;
                            try {
                                output = new FileOutputStream(file);
                                output.write(bytes);
                            } finally {
                                if (null != output)
                                    output.close();
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (image != null)
                                image.close();
                        }
                    }
                };

        // 取得 CameraManager
        CameraManager camMgr = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            CameraCharacteristics camChar =
                    camMgr.getCameraCharacteristics(mCameraDevice.getId());

            // 設定拍照的解析度
            Size[] jpegSizes = null;
            if (camChar != null)
                jpegSizes = camChar.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);

            int picWidth = 640;
            int picHeight = 480;
            if (jpegSizes != null && jpegSizes.length > 0) {
                picWidth = jpegSizes[0].getWidth();
                picHeight = jpegSizes[0].getHeight();
            }

            // 設定照片要輸出給誰
            // 1. 儲存為影像檔； 2. 輸出給UI的TextureView顯示
            ImageReader imgReader = ImageReader.newInstance(
                    picWidth, picHeight, ImageFormat.JPEG, 1);

            // 準備拍照用的thread
            HandlerThread thread = new HandlerThread("CameraTakePicture");
            thread.start();
            final Handler backgroudHandler = new Handler(thread.getLooper());

            // 把OnImageAvailableListener和thread設定給ImageReader
            imgReader.setOnImageAvailableListener(
                    imgReaderOnImageAvailable, backgroudHandler);

            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(imgReader.getSurface());
            outputSurfaces.add(new Surface(mTextureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(
                            CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imgReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE,
                    CameraMetadata.CONTROL_MODE_AUTO);

            // 決定照片的方向（直的或橫的）
            SparseIntArray PICTURE_ORIENTATIONS = new SparseIntArray();
            PICTURE_ORIENTATIONS.append(Surface.ROTATION_0, 90);
            PICTURE_ORIENTATIONS.append(Surface.ROTATION_90, 0);
            PICTURE_ORIENTATIONS.append(Surface.ROTATION_180, 270);
            PICTURE_ORIENTATIONS.append(Surface.ROTATION_270, 180);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                    PICTURE_ORIENTATIONS.get(rotation));

            // 準備拍照的callback
            final CameraCaptureSession.CaptureCallback camCaptureCallback =
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(CameraCaptureSession session,
                                                       CaptureRequest request, TotalCaptureResult result) {
                            super.onCaptureCompleted(session, request, result);

                            Integer mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE);
                            Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
                            if(faces != null && mode != null)
                                Toast.makeText(MainActivity.this, "人臉: " +
                                        faces.length, Toast.LENGTH_SHORT).show();

                            // 播放快門音效檔
                            Uri uri = Uri.parse("android.resource://" +
                                    getPackageName() + "/" + R.raw.sound_camera_shutter);
                            MediaPlayer mp = MediaPlayer.create(MainActivity.this, uri);
                            mp.start();

                            Toast.makeText(MainActivity.this, "拍照完成\n影像檔: " +
                                    file, Toast.LENGTH_SHORT).show();
                            startPreview();
                        }

                        @Override
                        public void onCaptureProgressed(CameraCaptureSession session,
                                                        CaptureRequest request, CaptureResult partialResult) {
                        }
                    };

            // 最後一步就是建立Capture Session
            // 然後啟動拍照
            mCameraDevice.createCaptureSession(outputSurfaces,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            try {
                                closeAllCameraCaptureSession();

                                // 記下這個capture session，使用完畢要刪除
                                mCameraTakePicCaptureSession = cameraCaptureSession;

                                cameraCaptureSession.capture(captureBuilder.build(),
                                        camCaptureCallback, backgroudHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(MainActivity.this, "拍照起始錯誤", Toast.LENGTH_LONG)
                                    .show();                }
                    },
                    backgroudHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
