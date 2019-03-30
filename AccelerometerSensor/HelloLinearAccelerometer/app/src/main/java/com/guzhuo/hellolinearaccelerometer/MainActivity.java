package com.guzhuo.hellolinearaccelerometer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener {

    public TextView tx1;
    public TextView tx2;
    public float[] angle = new float[3];
    public long lastTime;
    public String content;
    public SensorManager mSensorManager;
    public OnChangeListener mOnChangeListener;
    private Sensor mAccelerometerSensor;

    public List<Sensor> initSensor() {
        // 获取手机传感器列表
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        return mSensorManager.getSensorList(Sensor.TYPE_ALL);
    }

    public void initSensor(Context context, int type) {
        // initSensor(Sensor.TYPE)
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(type);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
