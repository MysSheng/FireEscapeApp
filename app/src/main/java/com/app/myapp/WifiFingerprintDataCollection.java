package com.app.myapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WifiFingerprintDataCollection extends AppCompatActivity implements SensorEventListener{

    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer, gyroscope;
    private WifiManager wifiManager;
    private TextView positionTextView, stepInfoTextView, scanCountTextView;
    private Button startScanningBtn, stopScanningBtn, sendToServerBtn, saveCsvBtn;

    private float[] gravity, geomagnetic;
    private float azimuth = 0f; // 方向角
    private float stepCount = 0;
    private float stepLength = 0.7f; // 假設每一步長 70cm
    private float x = 3, y = 5; // (x, y) 初始座標
    private List<String> collectedData = new ArrayList<>(); // 儲存 WiFi RSSI 和座標資料
    private int scanCount = 0; // 計算 WiFi 掃描次數

    private Boolean isScanning = false;
    private Handler handler = new Handler();

    // wifi Info 顯示
    private StringBuilder wifiInfo = new StringBuilder("No WiFi Data Available");

    // 伺服器設定
    private static final String SERVER_IP = MainActivity2.SERVER_IP;
    private static final int SERVER_PORT = MainActivity2.SERVER_PORT;

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

    private static final long WIFI_SCAN_INTERVAL = 5000; // Wi-Fi 掃描間隔 (5 秒)

    // Complementary Filter Using Gyroscope
    private float[] gyro = new float[3];
    private float fusedYaw = 0f;
    private long lastUpdateTime = -1;
    private static final float FILTER_ALPHA = 0.9f; // complementary filter blending constant

    // 箭頭
    private AzimuthArrowView azimuthArrowView;
    // 地圖
    private MapView_CSIE_1F mapView_CSIE_1F_small;

    private JoystickView joystickView;

    private ProgressBar wifiProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.wififingerprintdatacollection);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        stepInfoTextView = findViewById(R.id.StepInfoTextView);
        stepInfoTextView.setVisibility(View.VISIBLE);

        positionTextView = findViewById(R.id.positionTextView);
        startScanningBtn = findViewById(R.id.StartScanningBtn);
        stopScanningBtn = findViewById(R.id.StopScanningBtn);
        sendToServerBtn = findViewById(R.id.SendToServerBtn);
        saveCsvBtn = findViewById(R.id.SaveCsvBtn);
        azimuthArrowView = findViewById(R.id.azimuthArrowView);
        mapView_CSIE_1F_small = findViewById(R.id.mapView_CSIE_1F_small);
        joystickView = findViewById(R.id.joystickView);
        wifiProgressBar = findViewById(R.id.wifiProgressBar);
        scanCountTextView = findViewById(R.id.scanCountTextView);

        // 設置進度條
        wifiProgressBar.setMax(500);


        // 初始化感測器
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // 初始化 Wi-Fi
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, filter);

        // 設置按鈕
        startScanningBtn.setOnClickListener(v -> startScanning());
        stopScanningBtn.setOnClickListener(v -> stopScanning());
        sendToServerBtn.setOnClickListener(v -> sendDataToServer());
        saveCsvBtn.setOnClickListener(v -> saveDataAsCsv());
        joystickView.setOnMoveListener((dx, dy) -> {
            float adjustFactor = 0.8f; // 控制調整速度
            x += dx * adjustFactor;
            y += dy * adjustFactor;
            // 限制 x 和 y 在 0~57m 範圍內
            x = Math.max(0, Math.min(x, 57f));
            y = Math.max(0, Math.min(y, 57f));
            positionTextView.setText(String.format("X: %.2f m, Y: %.2f m", x, y));
            mapView_CSIE_1F_small.updateUserPosition(x, y);
        });

        // 開啟偵測器
        registerSensors();
    }

    private void saveDataAsCsv() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(path, "wifi_data.csv");

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            // 寫入 CSV 標題欄位
            writer.write("SSID,BSSID,RSSI,X,Y,Scan Count\n");

            for (String line : collectedData) {
                writer.write(line);
                writer.write("\n");
            }
            writer.flush();
            Toast.makeText(this, "Saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save CSV", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendDataToServer() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(path, "wifi_data.csv");

        if (!file.exists()) {
            Toast.makeText(this, "CSV file not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                 DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())) {

                // 傳送檔案名稱
                outputStream.writeUTF("wifi_data.csv");

                // 傳送檔案大小
                outputStream.writeLong(file.length());

                // 傳送檔案內容
                byte[] buffer = new byte[4096];
                int bytesRead;
                try (FileInputStream fis = new FileInputStream(file)) {
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

                outputStream.flush();
                runOnUiThread(() -> Toast.makeText(this, "CSV sent to server", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to send CSV", Toast.LENGTH_SHORT).show());
            }

        }).start();


        // 傳送 collectedData 裡的每一行數據
        for (String data : collectedData) {
            sendMessagesToServer(data + "\n");
        }
        sendMessagesToServer("End of Sending Data");
    }

    private void startScanning() {
        if (!isScanning) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return;
            }
            isScanning = true;
            handler.postDelayed(() -> wifiManager.startScan(), WIFI_SCAN_INTERVAL);
            Toast.makeText(this, "Wifi Scanning Started.\nPlease Scan 500 times", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopScanning() {
        isScanning = false;
        // unregisterSensors();
        Toast.makeText(this, "Scanning Stopped", Toast.LENGTH_SHORT).show();
    }

    // 註冊感測器監聽
    private void registerSensors() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI, 10000);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI, 10000);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI, 10000);
    }

    // 取消感測器監聽
    private void unregisterSensors() {
        sensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
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
            long currentTime = System.currentTimeMillis();
            float deltaT = (lastUpdateTime > 0) ? (currentTime - lastUpdateTime) / 1000f : 0f; // seconds

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

                if (lastUpdateTime > 0) {
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
                azimuthArrowView.setAzimuth(360 - azimuth);
            }
            lastUpdateTime = currentTime;
        }
    }

    // 主偵測器 使用動態步長估計
    private void detectStep(float[] values) {
        float accZ = values[2];
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

                stepInfoTextView.setText("");

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
    private void updatePosition() {
        double radian = Math.toRadians(azimuth);
        x += stepLength * Math.sin(radian);
        y -= stepLength * Math.cos(radian);
        // 限制 x 和 y 在 0~57m 範圍內
        x = Math.max(0, Math.min(x, 57f));
        y = Math.max(0, Math.min(y, 57f));
        positionTextView.setText(String.format("X: %.2f m, Y: %.2f m", x, y));
        mapView_CSIE_1F_small.updateUserPosition(x, y);
    }

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show();
                return;
            }

            List<ScanResult> results = wifiManager.getScanResults();
            wifiInfo.setLength(0); // 清空舊的掃描紀錄
            for (ScanResult result : results) {
                String ssid = result.SSID;
                String bssid = result.BSSID;
                int rssi = result.level;
                String data = ssid + "," + bssid + "," + rssi + "," + x + "," + y + "," + scanCount;
                collectedData.add(data);

                wifiInfo.append("X: ").append(x)
                        .append("m, Y: ").append(y)
                        .append("m\nSSID: ").append(result.SSID)
                        .append(", RSSI: ").append(result.level)
                        .append("\n");
            }

            scanCount++;
            wifiProgressBar.setProgress(scanCount);
            scanCountTextView.setText("Scan Count: " + scanCount);
            if (scanCount >= 500) {
                Toast.makeText(context, "Finished Scanning! Scanning Stopped", Toast.LENGTH_SHORT).show();
                isScanning = false;
            }

            // Toast.makeText(context, "Scan count: " + scanCount + ", Numbers of Wifi AP: " + results.size(), Toast.LENGTH_SHORT).show();
            System.out.println(wifiInfo.toString());

            if (isScanning) {
                handler.postDelayed(() -> wifiManager.startScan(), WIFI_SCAN_INTERVAL);
            }
        }
    };


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isScanning = false;
        sensorManager.unregisterListener(this);

        // 移除所有 handler 任務，防止掃描繼續
        handler.removeCallbacksAndMessages(null);

        // 清除蒐集資料
        collectedData.clear();
        accZValues.clear();
        accZTimestamp.clear();
        scanCount = 0;
        total_length = 0;
    }

    private void sendMessagesToServer(String message) {
        new Thread(() -> {
            try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                 DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())) {

                // 使用標準 UTF-8 編碼字節數組來發送訊息
                byte[] utf8Message = message.getBytes(StandardCharsets.UTF_8);
                outputStream.write(utf8Message); // 傳送字節數組
                outputStream.flush(); // 確保所有數據都被寫入輸出流

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

}