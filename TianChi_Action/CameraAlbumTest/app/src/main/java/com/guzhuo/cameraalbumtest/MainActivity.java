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
    public static final int CHOOSE_PHOTO = 2;

    // 相片输出路径封装相关变量
    private ImageView picture;
    private Uri imageUri;  //图片的输出地址

    private SensorManager mSensorManager;
    // 为方向传感器，需要加速度传感器和地磁场传感器提供数据
    private Sensor mAccelerometerSensor;
    private Sensor mMagneticSensor;

    private float[] mAccelerometerValues = new float[3];
    private float[] mMagneticVaglues = new float[3];
    private float[] mRotationMatrix_Cur = new float[9];
    private float[] mRotationMatrix_Prev = new float[9];

    // 计算手机偏移量，相关变量
    private double[] mDeltaAvgAcc = new double[3];  // 任一切片时间内，加速度的变化量
    private double[] mAccVel;  // 由连续切片间，累加的速度
    private double[] mDeltaDisp;  // 由连续切片间，累加的位移
    private List<Double> mPointsDisp;  // 不同拍照地点之间的距离
    private float[] mAcc_Prev = new float[3];  // 前一刻的加速度值
    private double mPrevTime;  // 前一刻的时间
    private double mDeltaTime;  //前后刻的变化时间，一般作切片时间的区长

    private TextView mTxtValue1;
    private TextView mTxtValue2;
    private TextView mTxtValue3;
    public static final String TAG = "fetchValues";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button takePhoto = (Button) findViewById(R.id.take_photo);
        Button chooseFromAlbum = (Button) findViewById(R.id.choose_from_album);
        picture = (ImageView) findViewById(R.id.picture);

        // ---
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
                 * 此外，获取最新代数，及将 mDeltaDisp 置0
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

        chooseFromAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 点击 chooseFromAlbum 按钮，则动态申请 WRITE_EXTERNAL_STAROGE 权限，
                // 授予程序对 SD 卡读写的能力。
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                } else {
                    openAlbum();
                }
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
    protected void onStop() {
        super.onStop();
        // 若退出页面则注销监听器
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
                    // 使用旋转矩阵，计算切片时间内的平均加速度
                    // 等差系数 APCoefficient 的算式， APCofficient - 1 == i
                    mDeltaAvgAcc[i] = 0.5 *
                            (
                                    (mRotationMatrix_Cur[i*3] * event.values[i] + mRotationMatrix_Cur[i*3+1] * event.values[i+1] + mRotationMatrix_Cur[i*3+2] * event.values[i+2]) +
                                    (mRotationMatrix_Prev[i*3] * mAcc_Prev[i] + mRotationMatrix_Prev[i*3+1] * mAcc_Prev[i+1] + mRotationMatrix_Prev[i*3+2] * mAcc_Prev[i+2])
                            );
                    APCoefficient++;

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

                // 在终端上实时刷新
                mTxtValue2.setText("mDeltaAvgAcc[].model:" + String.valueOf(Math.sqrt(mDeltaAvgAcc[0] * mDeltaAvgAcc[0] + mDeltaAvgAcc[1] * mDeltaAvgAcc[1] + mDeltaAvgAcc[2] * mDeltaAvgAcc[2])));
                mTxtValue3.setText("mAccVel[].model:" + String.valueOf(Math.sqrt(mAccVel[0] * mAccVel[0] + mAccVel[1] * mAccVel[1] + mAccVel[2] * mAccVel[2])));
                mTxtValue1.setText("mDeltaDisp[].model:" + String.valueOf(Math.sqrt(mDeltaDisp[0] * mDeltaDisp[0] + mDeltaDisp[1] * mDeltaDisp[1] + mDeltaDisp[2] * mDeltaDisp[2])));

//                mAccVel = new double[3];
//                mDeltaDisp = new double[3];

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
            case CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    // 判断手机系统版本号
                    if (Build.VERSION.SDK_INT >= 19) {
                        // 4.4及以上系统使用此方法处理图片
                        handleImageOnKitKat(data);
                    } else {
                        // 4.4以下系统使用此方法处理图片
                        handleImageBeforeKitKat(data);
                    }
                }
                break;
            default:
                break;
        }
    }


    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        // 传回图片时，保证 onActivityResult() 进入 CHOOSE_PHOTO 的 case，以处理图片。
        startActivityForResult(intent, CHOOSE_PHOTO);  // 打开相册
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            // 若 document 类型的Uri，则通过 document id 处理
            String docId = DocumentsContract.getDocumentId(uri);
            // 若 Uri 的 authority 是 meida 格式，document id 则需再一次解析，
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                // 通过字符串分割的方法取出后半部分，才能得到真正的数字 id。
                String id = docId.split(":")[1];  // 解析数字格式的 id
                // 构建新的 Uri 和条件语句。
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://download/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // 若 content 类型的 Uri，则使用普通方式处理
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            // 若 file 类型的 Uri，直接获取图片路径
            imagePath = uri.getPath();
        }
        displayImage(imagePath);  // 根据图片路径显示图片
    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        // 通过 Uri 和 selection 来获取图片的实际路径
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private void displayImage(String imagePath) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            picture.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "[MainActivity-displayImage()]failed to get image", Toast.LENGTH_SHORT).show();
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
//        mPointsDisp.add(Math.sqrt(mDeltaDisp[0] * mDeltaDisp[0] + mDeltaDisp[1] * mDeltaDisp[1] + mDeltaDisp[2] * mDeltaDisp[2]));
//        mTxtValue1.setText(mPointsDisp
//                .get(mPointsDisp.size()-1)
//                .toString());

        Log.w(TAG, "fetchValues: mAccVel: " + (mAccVel[0] * mAccVel[0] + mAccVel[1] * mAccVel[1] + mAccVel[2] * mAccVel[2]));
        Log.w(TAG, "fetchValues: mPointsDisp.size(" + mPointsDisp.size() + "), " + "mPointsDisp: " + mPointsDisp);

    }


}