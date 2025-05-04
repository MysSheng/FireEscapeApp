package com.app.myapp;

//import android.support.annotation.DrawableRes;
//import android.support.annotation.NonNull;
//import android.support.constraint.ConstraintLayout;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;


import android.util.Log;

import org.opencv.android.OpenCVLoader;

public class IVP_Client {
    protected void opencvSetup() {
        if (!OpenCVLoader.initLocal()) {
            Log.e("OpenCV", "OpenCV initialization failed.");
        } else {
            Log.d("OpenCV", "OpenCV initialization succeeded.");
        }
    }
}
