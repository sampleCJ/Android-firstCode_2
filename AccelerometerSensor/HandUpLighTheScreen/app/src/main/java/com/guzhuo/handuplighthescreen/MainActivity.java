package com.guzhuo.handuplighthescreen;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

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

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mPowerManager = (PowerManager)getSystemService(POWER_SERVICE);
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "HandUpLightScreen::smartAwake");

        mSensorManager.registerListener(mSensorEventListener, mGravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    // 加速度传感器监听
    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            smartAwake(event);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    // 抬手亮屏幕
    private void smartAwake(SensorEvent event) {
        float[] values = event.values;
        float x = values[0];
        float y = values[1];
        float z = values[2];
        Log.i(TAG, "HandUpLightScreen::smartAwake:" +
                " x = " + x +
                " y = " + y +
                " z = " + z);

        subY = y - oldY;
        // 判断手机是否处于水平状态，并触发拿起动作
        if (Math.abs(x) < 3 && y > 0 && z < 9) {
            if (subY > 1) {
                shakeTime = System.currentTimeMillis();
                Log.i(TAG, "HandUpLightScreen::smartAwake: 1");
            }
            oldY = y;
        }

        // 判断手机处于面向人眼倾斜状态
        if (Math.abs(x) < 3 && y > 4 && y < 9 && z > 2 && z < 9) {
            showTime = System.currentTimeMillis();
            Log.i(TAG, "HandUpLightScreen::smartAwake: 2");
            if (showTime - shakeTime >= 0 && showTime -shakeTime < 200) {
                mWakeLock.acquire();
                mWakeLock.release();
                Log.i(TAG, "HandUpLightScreen::smartAwake: Awake");
            }
        }
    }

    /**
     * 当activity变为不可见时，选择停掉传感器的工作
     */
    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mSensorEventListener);
    }
}
