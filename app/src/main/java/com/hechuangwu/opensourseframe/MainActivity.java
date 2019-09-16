package com.hechuangwu.opensourseframe;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.hechuangwu.opensourseframe.glide.GlideActivity;
import com.hechuangwu.opensourseframe.leakcanary.LeakCanaryActivity;
import com.hechuangwu.opensourseframe.okhttp.OkHttpActivity;
import com.hechuangwu.opensourseframe.retrofit.RetrofitActivity;
import com.hechuangwu.opensourseframe.rxjava.RxJava2Activity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
    }

    public void retrofit(View view) {
        startActivity( new Intent( this, RetrofitActivity.class ) );
    }

    public void okHttp(View view) {
        startActivity( new Intent( this, OkHttpActivity.class ) );
    }

    public void rxJava(View view) {
        startActivity( new Intent( this, RxJava2Activity.class ) );
    }

    public void glide(View view) {
        startActivity( new Intent( this, GlideActivity.class ) );
    }

    public void leakCanary(View view) {
        startActivity( new Intent( this, LeakCanaryActivity.class ) );
    }

}
