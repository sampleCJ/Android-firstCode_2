package com.guzhuo.cameraalbumtest;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity
        implements SensorEventListener {

    // 摄像头回调状态信号
    public static final int TAKE_PHOTO = 1;

    // 相片输出路径封装相关变量
    private ImageView picture;
    private Uri imageUri;  //图片的输出地址

    private SensorManager mSensorManager;
    // 为方向传感器，需要加速度传感器和地磁场传感器提供数据
    private Sensor mAccelerometerSensor;
    private Sensor mMagneticSensor;

    // 记录某一刻传感器值的中间变量
    private float[] mAccelerometerValues = new float[3];
    private float[] mMagneticVaglues = new float[3];
    private float[] mRotationMatrix_Cur = new float[9];
    private float[] mRotationMatrix_Prev = new float[9];

    // 以下变量用于辅助计算，手机偏移量
    private double[] mDeltaAvgAcc = new double[3];  // 任一切片时间内，加速度的变化量
    private double[] mAccVel;  // 由连续切片间，累加的速度
    private double[] mDeltaDisp;  // 由连续切片间，累加的位移
    private float[] mAcc_Prev = new float[3];  // 前一刻的加速度值
    private double mPrevTime;  // 前一切片末的时间
    private double mDeltaTime;  //前后刻的变化时间，一般作切片时间的区长

    private List<Double> mPointsDisp;  // 不同拍照地点之间的距离

    private TextView mTxtValue1;
    private TextView mTxtValue2;
    private TextView mTxtValue3;
    public static final String TAG = "fetchValues";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button takePhoto = (Button) findViewById(R.id.take_photo);
        picture = (ImageView) findViewById(R.id.picture);
        mTxtValue1 = (TextView) findViewById(R.id.txt_value1);
        mTxtValue2 = (TextView) findViewById(R.id.txt_value2);
        mTxtValue3 = (TextView) findViewById(R.id.txt_value3);

        // 实例化传感器管理器
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                 * 首次，初始化代数因子
                 * 他次，获取最新代数，及将 mDeltaDisp 置0
                 */
                initValues();

                // 创建 File 对象，用于存储由拍照得到的图片 output_image.jpg
                File outputImage = new File(getExternalCacheDir(), "output_image.jpg");

                try {
                    if (outputImage.exists()) {
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // 以时间戳命名拍照文件
                // todo

                if (Build.VERSION.SDK_INT >= 24) {
                    imageUri = FileProvider.getUriForFile(MainActivity.this,
                            "com.guzhuo.cameraalbumtest.fileprovider",
                            outputImage);
                } else {
                    imageUri = Uri.fromFile(outputImage);
                }

                // 启动相机程序
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");

                // 拍照后的保存路径
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

                // 启用快捷拍照，取消拍照后的确认预览
                intent.putExtra("android.intent.extra.quickCapture", true);
                startActivityForResult(intent, TAKE_PHOTO);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        // 为线性加速度传感器注册监听器
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_GAME);
        // 为加速度传感器注册监听器
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
        // 为地磁场传感器注册监听器
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_GAME);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销监听器
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // 如是，则表示现未进入计算状态
        if (mPointsDisp == null) return;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                double curTime = System.currentTimeMillis();  // 以微秒,记录当前时间
                mDeltaTime = (curTime - mPrevTime)/ 1000;  // 以秒,记录切片时间长度
                mRotationMatrix_Cur = calculateOrientation();  // 旋转矩阵，以帮助获得相对参考坐标系的加速度

                double mDeltaTime_Pow2 = mDeltaTime * mDeltaTime;

                // 应对 xyz 轴相关的代数运算
                for (int i = 0; i < event.values.length; i++) {
                    if (mRotationMatrix_Cur.length == 9) {
                        // 使用旋转矩阵辅助计算，切片时间内的平均加速度
                        // 矩阵乘法 (R1 * a1 + R0 * a0) * 0.5
                        // R1, R0: [3x3], a1, a0: [3x1]
                        mDeltaAvgAcc[i] = 0.5 *
                                (
                                        (mRotationMatrix_Cur[i*3] * event.values[0] + mRotationMatrix_Cur[i*3 + 1] * event.values[1] + mRotationMatrix_Cur[i*3 + 2] * event.values[2]) +
                                        (mRotationMatrix_Prev[i*3] * mAcc_Prev[0] + mRotationMatrix_Prev[i*3 + 1] * mAcc_Prev[1] + mRotationMatrix_Prev[i*3 + 2] * mAcc_Prev[2])
                                );
                    }else {
                        Log.w(TAG, "onSensorChanged: Cannot calculate mRotationMatrix 4x4");
                    }

                    // 切片时间内的位移量，视作匀加速运动
                    mDeltaDisp[i] = mAccVel[i] * mDeltaTime + 0.5 * mDeltaAvgAcc[i] * mDeltaTime_Pow2;

                    // 更新：本次切片时间内的平均速度，上一切片时间末的加速度
                    mAccVel[i] += mDeltaAvgAcc[i] * mDeltaTime;
                    mAcc_Prev[i] = event.values[i];
                }

                // 记录上一刻时间（两刻时间标定切片时间长度）
                mPrevTime = curTime;
                // 记录上一刻旋转矩阵
                mRotationMatrix_Prev = mRotationMatrix_Cur;

                // 在MainActivity上刷新
                mTxtValue2.setText("mDeltaAvgAcc[].model:" + String.valueOf(Math.sqrt(mDeltaAvgAcc[0] * mDeltaAvgAcc[0] + mDeltaAvgAcc[1] * mDeltaAvgAcc[1] + mDeltaAvgAcc[2] * mDeltaAvgAcc[2])));
                mTxtValue3.setText("mAccVel[].model:" + String.valueOf(Math.sqrt(mAccVel[0] * mAccVel[0] + mAccVel[1] * mAccVel[1] + mAccVel[2] * mAccVel[2])));
                mTxtValue1.setText("mDeltaDisp[].model:" + String.valueOf(Math.sqrt(mDeltaDisp[0] * mDeltaDisp[0] + mDeltaDisp[1] * mDeltaDisp[1] + mDeltaDisp[2] * mDeltaDisp[2])));

                Log.w(TAG, "onSensorChanged: mDeltaTime:" + mDeltaTime);

                mAccVel = new double[3];
                mDeltaDisp = new double[3];

                break;
            case Sensor.TYPE_ACCELEROMETER:
                mAccelerometerValues = event.values;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mMagneticVaglues = event.values;
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        // 将图片插入到系统图库
                        // todo

                        // 将拍摄的照片显示出来
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver()
                                .openInputStream(imageUri));
                        picture.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    // 广播通知图库刷新
                    // todo
                }
                break;
            default:
                break;
        }
    }


    /**
     * 使用方向传感器，及其他数据来源，计算方向
     */
    private float[] calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[9];

        SensorManager.getRotationMatrix(R, null, mAccelerometerValues, mMagneticVaglues);
        SensorManager.getOrientation(R, values);

        // 旋转矩阵 R 由弧度制变换为角度


        return R;
    }

    /**
     * 若在回调函数中，则用于判断
     * 首次拍照：初始化相关变量/ 代数因子
     *
     */
    private void initValues() {
        // GO!
        if (mPointsDisp == null) {
            // 首次拍照，初始化代数因子
            mPrevTime = System.currentTimeMillis();
            mAccVel = new double[3];
            mDeltaDisp = new double[3];
            mPointsDisp = new ArrayList<>();
        }else {
            fetchValues();
            // 将位移量置0
            mDeltaDisp = new double[3];
        }

    }


    /**
     * 将本次测距结果加入 mPointsDisp
     */
    private void fetchValues() {
        mPointsDisp.add(Math.sqrt(mDeltaDisp[0] * mDeltaDisp[0] + mDeltaDisp[1] * mDeltaDisp[1] + mDeltaDisp[2] * mDeltaDisp[2]));
        mTxtValue1.setText(mPointsDisp
                .get(mPointsDisp.size()-1)
                .toString());

        Log.w(TAG, "fetchValues: mAccVel: " + (mAccVel[0] * mAccVel[0] + mAccVel[1] * mAccVel[1] + mAccVel[2] * mAccVel[2]));
        Log.w(TAG, "fetchValues: mPointsDisp.size(" + mPointsDisp.size() + "), " + "mPointsDisp: " + mPointsDisp);
    }


}