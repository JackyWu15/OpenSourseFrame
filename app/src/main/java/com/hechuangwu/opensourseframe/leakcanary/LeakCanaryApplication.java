package com.hechuangwu.opensourseframe.leakcanary;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by cwh on 2019/9/12 0012.
 * 功能:
 */
public class LeakCanaryApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install( this );
    }
}
