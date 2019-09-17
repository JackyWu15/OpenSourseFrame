package com.hechuangwu.opensourseframe.eventbus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.hechuangwu.opensourseframe.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class EventBusActivity extends AppCompatActivity {
    private static final String TAG = "EventBusActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_event_bus );


    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register( this );
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister( this );
    }

    public void send(View view) {
        EventBus.getDefault().post( new MessageEvent( "哈哈" ) );
        EventBus.getDefault().postSticky( new MessageEvent( "嘻嘻" ) );
    }

    @Subscribe(threadMode = ThreadMode.MAIN, priority = 2, sticky = true)
    public void onMessageEventPost(MessageEvent data) {
        Log.i( TAG, "onMessageEventPost:>>>"+data.getMsg() );
    }
}
