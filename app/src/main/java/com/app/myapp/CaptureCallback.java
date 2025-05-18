package com.app.myapp;

import android.graphics.Bitmap;

import org.opencv.core.Mat;

public interface CaptureCallback {
    void onImageCaptured(Bitmap bestImage);
    void onError(Throwable t);
}
