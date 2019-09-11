package com.hechuangwu.opensourseframe;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.hechuangwu.opensourseframe.okhttp.OkHttpActivity;
import com.hechuangwu.opensourseframe.retrofit.RetrofitActivity;
import com.hechuangwu.opensourseframe.rxjava.RxJava2Activity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        CustomerView customerView = new CustomerView( this );
        ObjectAnimator.ofFloat( customerView,"progress",1,1 );
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
}
