package com.guzhuo.sharedpreferencestest;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button saveData = (Button)findViewById(R.id.save_data);
        saveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = getSharedPreferences("data", MODE_PRIVATE).edit();
                editor.putString("name", "Tom");
                editor.putInt("age", 28);
                editor.putBoolean("isMarried", false);
                editor.apply();
            }
        });

        Button restoreData = (Button)findViewById(R.id.restore_data);
        restoreData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("data", MODE_PRIVATE);
                String name = sharedPreferences.getString("name", "");
                int age = sharedPreferences.getString("age", 0);
                boolean isMarried = sharedPreferences.getBoolean("isMarried", false);
                Log.d("MainActivity onCreate", "name is: " + name);
                Log.d("MainActivity onCreate", "age is: " + age);
                Log.d("MainActivity onCreate", "isMarried: " + isMarried);
            }
        });
    }
}
