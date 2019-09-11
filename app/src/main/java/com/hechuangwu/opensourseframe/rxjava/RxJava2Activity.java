package com.hechuangwu.opensourseframe.rxjava;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.hechuangwu.opensourseframe.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RxJava2Activity extends AppCompatActivity {
    private static final String TAG = "RxJava2Activity";
    private TextView mTv_content;
    private ImageView mIv_image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_rx_java );
        mTv_content = findViewById( R.id.tv_content );
        mIv_image = findViewById( R.id.iv_image );
//        rxJava2_0();
//        rxJava2_1();
//        rxJava2_2();
    }

    //RxJava创建
    private void rxJava2_0() {
        //方式一：构建被观察者
        Observable<String> observable = Observable.create( new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {//默认主线程调用
                Log.i( TAG, "subscribe:>>>1" );
                emitter.onNext( "1" );
                emitter.onNext( "2" );
                emitter.onNext( "3" );
                emitter.onComplete();
                emitter.onNext( "4" );
                Log.i( TAG, "subscribe:>>>2" );
            }
        } );

        //方式二：just构建被观察者
        Observable<String> observable2 = Observable.just( "hello", "cwhe" );

        //方式三：from构建被观察者
        String[] parameters = {"hello", "cwhe"};
        Observable<String> observable3 = Observable.fromArray( parameters );


        //创建观察者
        Observer observer = new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.i( TAG, "onSubscribe:>>>" + d );
            }

            @Override
            public void onNext(String o) {
                Log.i( TAG, "onNext:>>>" + o );
            }

            @Override
            public void onError(Throwable e) {
                Log.i( TAG, "onError:>>>" );
            }

            @Override
            public void onComplete() {
                Log.i( TAG, "onComplete:>>>" );
            }
        };
        //订阅
        observable.subscribe( observer );


    }

    //链式调用
    private void rxJava2_1() {
        Observable.create( new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
                String s = synRequest();
                Gson gson = new Gson();
                RxJavaModel rxJavaModel = gson.fromJson( s, RxJavaModel.class );
                emitter.onNext( rxJavaModel.getMsg() );
            }
        } )
        .subscribeOn( Schedulers.io() )//切换到io线程,但observer也被同时切换了,位置没有限制，但只能调用一次，会新建observable
        .observeOn( AndroidSchedulers.mainThread() )//将observer切换回主线程,指定的是它之后的线程，可以多次调用实现线程多次切换
        .subscribe( new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        mTv_content.setText( s );
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i( TAG, "onError:>>>"+e.getLocalizedMessage() );
                    }

                    @Override
                    public void onComplete() {

                    }
                } );
    }


    //实现图片遍历和过滤，生成bitmap
    @SuppressLint("CheckResult")
    private void rxJava2_2(){
        File folder = new File( Environment.getExternalStorageDirectory()+File.separator+"Image" );
        Observable.fromArray( folder )
                .flatMap( new Function<File, ObservableSource<File>>() {
                    @Override
                    public ObservableSource<File> apply(File file) {
                        return Observable.fromArray( file.listFiles() );
                    }
                } )
                .filter( new Predicate<File>() {
            @Override
            public boolean test(File file)  {
                return file.isDirectory();
            }
        } ).flatMap( new Function<File, ObservableSource<File>>() {
            @Override
            public ObservableSource<File> apply(File file)  {
                return Observable.fromArray( file.listFiles() );
            }
        } ).filter( new Predicate<File>() {
            @Override
            public boolean test(File file) throws Exception {
                return file.getName().contains( ".png" );
            }
        } ).map( new Function<File, Bitmap>() {
            @Override
            public Bitmap apply(File file)  {
                return getBitmapFile(file);
            }
        } ).subscribeOn( Schedulers.io() )
                .observeOn( AndroidSchedulers.mainThread() )
               .subscribe( new Consumer<Bitmap>() {
                   @Override
                   public void accept(Bitmap bitmap)  {
                       Log.i( TAG, "accept:>>>"+bitmap );
                   }

               } );
    }

    private Bitmap getBitmapFile(File file){
        FileInputStream fis = null;
        try {
            fis = new FileInputStream( file.getAbsolutePath() );
            return BitmapFactory.decodeStream( fis );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private String synRequest(){
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout( 5, TimeUnit.SECONDS )
                .build();
        Request request = new Request.Builder()
                .url( "https://www.apiopen.top/journalismApi" )
                .get()
                .build();
        Call call = okHttpClient.newCall( request );
        try {
            Response response = call.execute();
            String string = response.body().string();
            Log.i( TAG, "synRequest:>>>"+string );
            return string;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
