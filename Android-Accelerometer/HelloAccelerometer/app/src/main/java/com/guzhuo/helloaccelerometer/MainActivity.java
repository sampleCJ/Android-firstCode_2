package com.guzhuo.helloaccelerometer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView tvAcclerometer;
    private SensorManager mSensorManager;
    private float[] gravity = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvAcclerometer = (TextView)findViewById(R.id.tv_accelerometer);
        // 获取传感器SensorManager对象
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
    }


    /**
     * 传感器精度变化时回调
     * @param sensor
     * @param accuracy
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        ;
    }


    /**
     * 传感器数据变化时回调
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // 判断传感器类别
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:  // 类别：加速度传感器
                final float alpha = (float)0.8;

                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                String str_accelerometer = "加速度传感器\n" +
                        "x:" + (event.values[0] - gravity[0]) + "\n" +
                        "y:" + (event.values[1] - gravity[1]) + "\n" +
                        "z:" + (event.values[2] - gravity[2]);
                tvAcclerometer.setText(str_accelerometer);
                // 重力加速度 9.81 m/s^2，只受到重力作用，即自由下落的加速度
                break;
            case Sensor.TYPE_GRAVITY:  // 类别：重力传感器
                gravity[0] = event.values[0];
                gravity[1] = event.values[1];
                gravity[2] = event.values[2];
                break;
            default:
                break;
        }
    }


    /**
     * 界面获取焦点，按钮点击时回调
     */
    @Override
    protected void onResume() {
        super.onResume();

        // 注册加速度传感器
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),  // 类别：加速度传感器
                SensorManager.SENSOR_DELAY_UI);  // 采集频率

        // 注册重力传感器
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_FASTEST);

    }


    /**
     * 暂停Activity，界面获取焦点，按钮点击时回调
     */
    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
}
