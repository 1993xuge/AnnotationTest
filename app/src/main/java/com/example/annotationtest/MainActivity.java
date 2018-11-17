package com.example.annotationtest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import com.example.annotation.InjectView;

public class MainActivity extends AppCompatActivity {

    @InjectView(R.id.tv_test)
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //使用findViewById注入被InjectView修饰的成员变量
        com.example.processor.injector.ViewInjector.inject(this);

        textView.setText("hahha");
    }
}
