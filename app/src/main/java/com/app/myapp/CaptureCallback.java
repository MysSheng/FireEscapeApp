package com.app.myapp;

import android.graphics.Bitmap;
import android.graphics.PointF;

import org.opencv.core.Mat;

import java.util.concurrent.CompletableFuture;

public interface CaptureCallback {
    void onImageCaptured(Bitmap bestImage, CompletableFuture<PointF> result);
    void onError(Throwable t);
    void onResultReady(double x, double y);
}
