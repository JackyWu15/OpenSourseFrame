package com.hechuangwu.opensourseframe.leakcanary;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

import com.hechuangwu.opensourseframe.R;

import java.lang.ref.WeakReference;

public class LeakCanaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_leak_canary );


        singletonLeak();//单例内存泄漏
        handlerLeak();//非静态内部类应用外部类，造成内存泄漏
        staticLeak();//非静态内部类创建静态实例，造成内存泄漏
    }


    private void singletonLeak(){
        LeakCanarySingleton.getInstance( this );
    }


    //非静态内部类默认持有外部类引用，当activity销毁时，Handler仍旧持有activity，解决方法加static
    private final  Handler mLeakyHandler = new Handler( ) {
        @Override
        public void handleMessage(Message msg) {
        }
    };
    //如果静态内部类要持有activity，那么通过弱引用引入
    static class MyHandler extends Handler{
        WeakReference<LeakCanaryActivity> weakActivity;
        public MyHandler(LeakCanaryActivity activity){
            this.weakActivity = new WeakReference<>( activity );
        }
        @Override
        public void handleMessage(Message msg) {
            LeakCanaryActivity leakCanaryActivity = weakActivity.get();

        }
    }
    private void handlerLeak(){
        mLeakyHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
            }
        }, 1000 * 60 * 10);
    }


    //非静态内部类默认持有外部类，静态实例的生命周期和应用的生命周期一样长，导致静态实例一直持有外部类
    private static InnerClass mInnerClass = null;
    private void staticLeak(){
        if (mInnerClass == null) {
            mInnerClass = new InnerClass();
        }
    }
    class InnerClass {
    }


}
