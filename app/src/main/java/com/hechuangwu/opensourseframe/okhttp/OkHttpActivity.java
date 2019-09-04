package com.hechuangwu.opensourseframe.okhttp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.hechuangwu.opensourseframe.R;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;



/**
 *  源码解析：
 */
public class OkHttpActivity extends AppCompatActivity {
    private static final String TAG = "OkHttpActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_ok_http );

        asyRequest();//异步请求
    }

    //同步请求
    private void synRequest(){
        //实例化请求客户端
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout( 5, TimeUnit.SECONDS )
                .build();
        //实例化请求对象
        Request request = new Request.Builder()
                .url( "http://www.baidu.com" )
                .get()
                .build();
        //和服务端建立连接的call
        Call call = okHttpClient.newCall( request );
        try {
            //同步请求，返回体,同步会阻塞线程直到响应
            Response response = call.execute();
            String s = response.body().toString();
            Log.i( TAG, "synRequest:>>>"+s );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //异步请求
    private void asyRequest() {
        //实例化请求客户端
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout( 5, TimeUnit.SECONDS )
                .cache( new Cache( new File( "cache" ),20*1024*1024 ) )//缓存大小
                .build();
        //实例化请求对象
        Request request = new Request.Builder()
                .url( "http://www.baidu.com" )
                .get()
                .build();
        //和服务端建立连接的call
        Call call = okHttpClient.newCall( request );

        //异步请求
        call.enqueue( new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i( TAG, "onFailure:>>>" );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i( TAG, "onResponse:>>>"+response.body().string() );
            }
        } );
    }
}
