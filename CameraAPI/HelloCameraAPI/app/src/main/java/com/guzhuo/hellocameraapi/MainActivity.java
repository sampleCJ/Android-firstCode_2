package com.guzhuo.hellocameraapi;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private int camearas = Camera.getNumberOfCameras();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // todo: Camera.release()
    }

    /**
     * 检测设备是否存在相机
     * @param context
     * @return
     */
    private boolean checkCameraHardWare(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        }else {
            // no camera on this device
            return false;
        }
    }
}
