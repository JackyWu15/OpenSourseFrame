package com.hechuangwu.opensourseframe.retrofit;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.hechuangwu.opensourseframe.R;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.io.InputStream;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_retrofit );
        retrofitRequest1();//默认adapter请求方式
        retrofitRequest2();//添加RxJavaAdapter请求方式
    }


    private void retrofitRequest1(){
        Retrofit retrofit = new Retrofit.Builder()//建造者模式
                .baseUrl( "https://api.github.com/" )//网络请求根地址，必须有，且末尾带/
                .build();

        RetrofitService retrofitService = retrofit.create( RetrofitService.class );
        Call repos = retrofitService.listRepos( "XXX" );
        repos.enqueue( new Callback() {
            @Override
            public void onResponse(Call call, Response response) {

            }

            @Override
            public void onFailure(Call call, Throwable t) {

            }
        } );
    }

    private void retrofitRequest2(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl( "https://api.github.com/" )
                .addConverterFactory( GsonConverterFactory.create() )
                .addCallAdapterFactory( RxJava2CallAdapterFactory.create() )//RxJava适配器
                .build();

        retrofit.create( RxJavaService.class )
                .getFileCall( "xxxx" )
                .subscribeOn( Schedulers.io() )
                .unsubscribeOn( Schedulers.io() )
                .map( new Function<ResponseBody, InputStream>() {
                    @Override
                    public InputStream apply(ResponseBody responseBody) throws Exception {
                        return responseBody.byteStream();
                    }
                } )
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe( new Observer<InputStream>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(InputStream inputStream) {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                } );



    }
}
