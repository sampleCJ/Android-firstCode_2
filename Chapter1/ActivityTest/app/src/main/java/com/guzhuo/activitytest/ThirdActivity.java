package com.guzhuo.activitytest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class ThirdActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ThirdActivity", "Task id is " + getTaskId());
        setContentView(R.layout.activity_third);
        Button button3 = (Button)findViewById(R.id.button_3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCollector.finishAll();
                // kill 掉当前进程的代码
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
    }
}
