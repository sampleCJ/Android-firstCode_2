package com.guzhuo.handuplighthescreen;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ansen";
    private SensorManager mSensorManager;
    private PowerManager mPowerManager;
    private Sensor mGravitySensor;
    private PowerManager.WakeLock mWakeLock;
    private long shakeTime;  // 手机触发拿起动作时间
    private long showTime;
    private float oldY = 0;
    private float subY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager)
    }
}
