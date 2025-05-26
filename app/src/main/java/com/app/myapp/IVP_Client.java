package com.app.myapp;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.SizeF;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ResolutionInfo;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import android.Manifest;


import androidx.activity.EdgeToEdge;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.AKAZE;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class IVP_Client {
    private final ImageCapture imageCapture;
    private final Executor executor;
    private final Handler handler;
    private final CaptureCallback callback;

    public IVP_Client(@NonNull Context ctx,
                      @NonNull ImageCapture imageCapture,
                      @NonNull CaptureCallback callback) {
        this.imageCapture   = imageCapture;
        this.executor       = Executors.newSingleThreadExecutor();
        this.handler        = new Handler(Looper.getMainLooper());
        this.callback       = callback;
    }

    public IVP_Client(Context ctx,
                      ImageCapture imageCapture,
                      Mat cameraMatrix) {
        this.imageCapture = imageCapture;
        this.executor = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());
        this.callback = new DefaultCallback(cameraMatrix);
    }

    /** Public API to trigger the capture. */
    public CompletableFuture<PointF> captureBurst(int numImages, long delayMillis) {
        CompletableFuture<PointF> result = new CompletableFuture<>();
        List<Bitmap> burstImages = new ArrayList<>();

        final Runnable[] r = new Runnable[1];
        r[0] = new Runnable() {
            int count = 0;

            @Override
            public void run() {
                if (count < numImages) {
                    imageCapture.takePicture(
                            executor,
                            new ImageCapture.OnImageCapturedCallback() {
                                @Override
                                public void onCaptureSuccess(@NonNull ImageProxy image) {
                                    try {
                                        Bitmap bmp = imageProxyToBitmap(image);
                                        int rot = image.getImageInfo().getRotationDegrees();
                                        if (rot != 0) {
                                            bmp = rotateBitmap(bmp, rot);
                                        }
                                        burstImages.add(bmp);
                                    } catch (Exception e) {
                                        callback.onError(e);
                                    } finally {
                                        image.close();
                                    }
                                    count++;
                                    handler.postDelayed(r[0], delayMillis);
                                }
                                @Override
                                public void onError(@NonNull ImageCaptureException exc) {
                                    callback.onError(exc);
                                    count++;
                                    handler.postDelayed(r[0], delayMillis);
                                }
                            }
                    );
                } else {
                    // Burst done → pick sharpest
                    Bitmap best = null;
                    double bestSharp = -1;
                    for (Bitmap b : burstImages) {
                        double s = computeLaplacianVariance(b);
                        if (s > bestSharp) {
                            bestSharp = s;
                            best = b;
                        }
                    }
                    if (best != null) {
                        callback.onImageCaptured(best, result);
                    } else {
                        callback.onError(new RuntimeException("No frames in burst"));
                    }
                }
            }
        };

        handler.post(r[0]);
        return result;
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private double computeLaplacianVariance(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        Mat laplacian = new Mat();
        Imgproc.Laplacian(mat, laplacian, CvType.CV_64F);
        MatOfDouble mean = new MatOfDouble(), stdDev = new MatOfDouble();
        Core.meanStdDev(laplacian, mean, stdDev);
        double variance = stdDev.get(0, 0)[0] * stdDev.get(0, 0)[0];
        mat.release();
        laplacian.release();
        return variance;
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        BitmapFactory.Options options = new BitmapFactory.Options();
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }
}

class DefaultCallback implements CaptureCallback {
    private final Mat cameraMatrix;
    private CompletableFuture<PointF> pending;

    public DefaultCallback(Mat cameraMatrix) {
        this.cameraMatrix = cameraMatrix;
    }

    @Override
    public void onImageCaptured(Bitmap bestImage, CompletableFuture<PointF> result) {
        Mat mat = new Mat();
        this.pending = result;
        Utils.bitmapToMat(bestImage, mat);
        Mat gray = new Mat();
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
        CLAHE clahe = Imgproc.createCLAHE(2.0, new org.opencv.core.Size(8, 8));
        Mat eq = new Mat();
        clahe.apply(gray, eq);

        // Create ORB feature detector
        AKAZE akaze = AKAZE.create();

        // Detect keypoints and compute descriptors
        MatOfKeyPoint kpts = new MatOfKeyPoint();
        Mat descriptors  = new Mat();

        akaze.detect(eq, kpts);
        akaze.compute(eq, kpts, descriptors);

        // Send keypoints and descriptors to server
        sendKeypointsToServer(kpts, descriptors, cameraMatrix);

        // Release resources
        mat.release();
        kpts.release();
        descriptors.release();
    }

    // client utilities
    private void sendKeypointsToServer(MatOfKeyPoint keyPoints, Mat descriptors, Mat cameraMatrix) {
        try {
            // Convert keypoints and descriptors to KeypointData objects.
            List<KeypointData> keypointDataList = new ArrayList<>();
            KeyPoint[] keypointArray = keyPoints.toArray();

            if (descriptors.rows() != keypointArray.length) {
                Log.e("OpenCV", "Descriptor and keypoint count mismatch!");
                return;
            }

            for (int i = 0; i < keypointArray.length; i++) {
                KeyPoint kp = keypointArray[i];

                // Extract descriptor (32 bytes per keypoint in ORB)
                int[] descriptorArray = new int[descriptors.cols()];
                for (int j = 0; j < descriptors.cols(); j++) {
                    descriptorArray[j] = (int) descriptors.get(i, j)[0];
                }

                // Create KeypointData object.
                keypointDataList.add(new KeypointData((float) kp.pt.x, (float) kp.pt.y, descriptorArray));
            }

            // Convert cameraMatrix (Mat) to a 3x3 double array.
            double[] camMatArr = new double[9];
            cameraMatrix.get(0, 0, camMatArr);
            double[][] camMat2D = new double[3][3];
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    camMat2D[i][j] = camMatArr[i * 3 + j];
                }
            }

            // Pack both data into a payload object.
            PayloadData payload = new PayloadData(keypointDataList, camMat2D);

            // Convert payload to JSON using Gson.
            Gson gson = new Gson();
            String jsonData = gson.toJson(payload);

            // Send JSON data to server.
            sendDataToServer(jsonData);
        } catch (Exception e) {
            Log.e("OpenCV", "Error while sending keypoints: " + e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void sendDataToServer(final String jsonData) {
        new Thread(() -> {
            String SERVER_IP = "10.180.202.32";
            int SERVER_PORT = 34567;
            try (Socket socket = new Socket()) {
                // optional: set a timeout so we don’t block forever
                socket.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT), 5_000);
                socket.setSoTimeout(5_000);   // read-timeout

                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in  = new DataInputStream(socket.getInputStream());

                /* ---- SEND ---------------------------------------------------- */
                byte[] compressed = compressData(jsonData);
                out.writeInt(compressed.length);
                out.write(compressed);
                out.flush();

                /* ---- RECEIVE ------------------------------------------------- */
                int len = in.readInt();
                byte[] gz = in.readNBytes(len);
                JSONObject reply = new JSONObject(
                        new String(decompressData(gz), StandardCharsets.UTF_8));

                double x = reply.getDouble("x");
                double y = reply.getDouble("y");

                /* ---- HANDLE REPLY ON UI THREAD ------------------------------ */
                new Handler(Looper.getMainLooper()).post(() -> this.onResultReady(x, y));
            } catch (Exception e) {
                Log.e("IVP", "Socket error", e);
            }
        }).start();
    }

    private byte[] compressData(String jsonData) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        gzipOutputStream.write(jsonData.getBytes("UTF-8"));
        gzipOutputStream.close();
        return byteArrayOutputStream.toByteArray();
    }

    private static byte[] decompressData(byte[] gz) throws IOException {
        try (ByteArrayInputStream bin = new ByteArrayInputStream(gz);
             GZIPInputStream gin = new GZIPInputStream(bin);
             ByteArrayOutputStream bout = new ByteArrayOutputStream()) {

            byte[] buf = new byte[4096];
            int n;
            while ((n = gin.read(buf)) != -1) bout.write(buf, 0, n);
            return bout.toByteArray();
        }
    }

    // Datatype Definitions in order for the code to run properly
    public class PayloadData {
        public List<KeypointData> keypoints;
        public double[][] cameraMatrix;

        public PayloadData(List<KeypointData> keypoints, double[][] cameraMatrix) {
            this.keypoints = keypoints;
            this.cameraMatrix = cameraMatrix;
        }
    }

    public class KeypointData {
        public float x;
        public float y;
        public int[] descriptor;

        public KeypointData(float x, float y, int[] descriptor) {
            this.x = x;
            this.y = y;
            this.descriptor = descriptor;
        }

        @Override
        public String toString() {
            return "KeypointData{" +
                    "x=" + x +
                    ", y=" + y +
                    ", descriptor=" + Arrays.toString(descriptor) +
                    '}';
        }
    }

    @Override
    public void onError(Throwable t) {
        Log.d("IVP Error", t.toString());
    }

    @Override public void onResultReady(double x, double y) {
        if (pending != null) {
            pending.complete(new PointF((float) x, (float) y));
            pending = null;
        }
    }
}