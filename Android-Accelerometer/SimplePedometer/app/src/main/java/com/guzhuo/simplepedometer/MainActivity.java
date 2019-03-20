package com.guzhuo.simplepedometer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    private SensorManager sensorManager;
    private Sensor mSensorAccelerometer;
    private TextView tv_step;
    private Button btn_start;
    private int step = 0;  // 步数
    private double oriValue = 0;  // 原始值
    private double lstValue = 0;  // 上次值
    private double curValue = 0;  // 当前值
    private boolean motiveState = true;  // 是否处于运动态
    private boolean processState = false;  // 标识当前是否已在计步

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化传感器
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_UI);
        bindViews();
    }

    private void bindViews() {
        tv_step = (TextView) findViewById(R.id.tv_step);
        btn_start = (Button) findViewById(R.id.btn_start);
        btn_start.setOnClickListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        double range = 1;  // 设计一个精度范围
        float[] value = event.values;
        curValue = magnitude(value[0], value[1], value[2]);  // 计算当前模
        // 向上加速的状态
        if (motiveState) {
            // 更新峰值
            if (curValue >= lstValue)
                lstValue = curValue;
            else {
                // 检测一次峰值
                if (Math.abs(curValue - lstValue) > range) {
                    oriValue = curValue;
                    motiveState = false;
                }
            }
        }

        // 向下加速的状态
        if (motiveState) {
            // 更新峰值
            if (curValue <= lstValue)
                lstValue = curValue;
            else {
                if (Math.abs(curValue - lstValue) > range) {
                    oriValue = curValue;
                    if (processState) {
                        step++;
                        tv_step.setText(step);  // 读数更新
                    }
                    motiveState = true;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // skip
    }

    @Override
    public void onClick(View v) {
        step = 0;
        tv_step.setText("0");
        if (processState) {
            btn_start.setText("开始");
            processState = false;
        } else {
            btn_start.setText("停止");
            processState = true;
        }
    }

    // 向量求模
    public double magnitude(float x, float y, float z) {
        double magnitude = 0;
        magnitude = Math.sqrt(x * x + y * y + z * z);
        return magnitude;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }
}
