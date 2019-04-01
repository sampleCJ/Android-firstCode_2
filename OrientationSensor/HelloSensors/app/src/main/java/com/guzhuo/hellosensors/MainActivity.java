package com.guzhuo.hellosensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener {

    private SensorManager mSensorManager;
    private TextView mTextValue1;
    private TextView mTextValue2;
    private TextView mTextValue3;
    private TextView mTextValue4;
    private TextView mTextValue5;
    private TextView mTextValue6;
    private TextView mTextValue7;
    private TextView mTextValue8;
    private TextView mTextValue9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextValue1 = (TextView) findViewById(R.id.text_value1);
        mTextValue2 = (TextView) findViewById(R.id.text_value2);
        mTextValue3 = (TextView) findViewById(R.id.text_value3);
        mTextValue4 = (TextView) findViewById(R.id.text_value4);
        mTextValue5 = (TextView) findViewById(R.id.text_value5);
        mTextValue6 = (TextView) findViewById(R.id.text_value6);
        mTextValue7 = (TextView) findViewById(R.id.text_value7);
        mTextValue8 = (TextView) findViewById(R.id.text_value8);
        mTextValue9 = (TextView) findViewById(R.id.text_value9);

        // 获取传感器管理对象
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 为加速度传感器注册监听器
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
        // 为方向传感器注册监听器
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),  // 不建议的注册方式
                SensorManager.SENSOR_DELAY_GAME);
        // 为陀螺仪传感器注册监听器
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_GAME);
        // 为磁场传感器注册监听器
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_GAME);
        // 为重力传感器注册监听器
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_GAME);
        // 为线性加速度传感器注册监听器
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_GAME);
        // 为温度传感器注册监听器
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE),  // 不建议的注册方式
                SensorManager.SENSOR_DELAY_GAME);
        // 为光传感器注册监听器
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
                SensorManager.SENSOR_DELAY_GAME);
        // 为压力传感器注册监听器
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),
                SensorManager.SENSOR_DELAY_GAME);
    }


    @Override
    protected void onStop() {
        super.onStop();
        // 若退出页面则注销监听器
        mSensorManager.unregisterListener(this);
    }

    // 当传感器值变化时，回调该方法
    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;
        // 获取传感器类型
        int sensorType = event.sensor.getType();
        StringBuilder stringBuilder = new StringBuilder();

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                stringBuilder.append("加速度传感器返回数据：");
                stringBuilder.append("\nX轴加速度：");
                stringBuilder.append(values[0]);
                stringBuilder.append("\nY轴加速度：");
                stringBuilder.append(values[1]);
                stringBuilder.append("\nZ轴加速度：");
                stringBuilder.append(values[2]);

                mTextValue1.setText(stringBuilder.toString());
                break;
            case Sensor.TYPE_ORIENTATION:
                stringBuilder.append("\n方向传感器返回数据：");
                stringBuilder.append("\n绕Z轴旋转的角度：");  // event.values[0] --- Z轴
                stringBuilder.append(values[0]);
                stringBuilder.append("\n绕X轴旋转的角度：");
                stringBuilder.append(values[1]);
                stringBuilder.append("\n绕Y轴旋转的角度：");
                stringBuilder.append(values[2]);

                mTextValue2.setText(stringBuilder.toString());
                break;
            case Sensor.TYPE_GYROSCOPE:
                stringBuilder.append("\n陀螺仪传感器返回数据：");
                stringBuilder.append("\n绕X轴旋转的角速度：");
                stringBuilder.append(values[0]);
                stringBuilder.append("\n绕Y轴旋转的角速度：");
                stringBuilder.append(values[1]);
                stringBuilder.append("\n绕Z轴旋转的角速度：");
                stringBuilder.append(values[2]);

                mTextValue3.setText(stringBuilder.toString());
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                stringBuilder.append("\n磁场传感器返回数据：");
                stringBuilder.append("\nX轴方向上的磁场强度");
                stringBuilder.append(values[0]);
                stringBuilder.append("\nY轴方向上的磁场强度");
                stringBuilder.append(values[1]);
                stringBuilder.append("\nZ轴方向上的磁场强度");
                stringBuilder.append(values[2]);

                mTextValue4.setText(stringBuilder.toString());
                break;
            case Sensor.TYPE_GRAVITY:
                stringBuilder.append("\n重力传感器返回数据：");
                stringBuilder.append("\nX轴方向上的重力：");
                stringBuilder.append(values[0]);
                stringBuilder.append("\nY轴方向上的重力：");
                stringBuilder.append(values[1]);
                stringBuilder.append("\nZ轴方向上的重力：");
                stringBuilder.append(values[2]);

                mTextValue5.setText(stringBuilder.toString());
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                stringBuilder.append("\n线性加速度传感器返回数据：");
                stringBuilder.append("\nX轴方向上的线性加速度：");
                stringBuilder.append(values[0]);
                stringBuilder.append("\nY轴方向上的线性加速度：");
                stringBuilder.append(values[1]);
                stringBuilder.append("\nZ轴方向上的线性加速度：");
                stringBuilder.append(values[2]);

                mTextValue6.setText(stringBuilder.toString());
                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                stringBuilder.append("\n温度长传感器返回数据：");
                stringBuilder.append("\n当前温度：");
                stringBuilder.append(values[0]);

                mTextValue7.setText(stringBuilder.toString());
                break;
            case Sensor.TYPE_LIGHT:
                stringBuilder.append("\n光传感器返回数据：");
                stringBuilder.append("\n当前光强：");
                stringBuilder.append(values[0]);

                mTextValue8.setText(stringBuilder.toString());
                break;
            case Sensor.TYPE_PRESSURE:
                stringBuilder.append("\n压力传感器返回数据：");
                stringBuilder.append("\n当前压力：");
                stringBuilder.append(values[0]);

                mTextValue9.setText(stringBuilder.toString());
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
