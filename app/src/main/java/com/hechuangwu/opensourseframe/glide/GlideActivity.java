package com.hechuangwu.opensourseframe.glide;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.hechuangwu.opensourseframe.R;

import java.util.concurrent.ExecutionException;

public class GlideActivity extends AppCompatActivity {
    private String imageUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_glide );

    }

    //指定View
    public void loadImage(View view) {
        Glide.with( getApplicationContext() )
                .load( imageUrl )//指定加载路径
                .placeholder( R.mipmap.ic_launcher )//加载前默认图
                .error( R.mipmap.ic_launcher )//加载错误时显示的图片
                .override( 300, 300 )//指定图片大小
                .centerCrop()//缩放，太大会裁剪并填满整个控件
                .fitCenter()//缩放，会显示完整图片，但有可能不会填满整个控件
                .skipMemoryCache( true )//跳过内缓存，但仍会使用硬盘缓存
                .diskCacheStrategy( DiskCacheStrategy.NONE )//不进行磁盘缓存
                .diskCacheStrategy( DiskCacheStrategy.RESOURCE )//缓存全分辨率的图片
                .diskCacheStrategy( DiskCacheStrategy.ALL )//缓存所有版本
                .diskCacheStrategy( DiskCacheStrategy.DATA )//
                .diskCacheStrategy( DiskCacheStrategy.AUTOMATIC )//
                .priority( Priority.HIGH )//指定优先级，但没法保证一定按优先级加载
                .into( (ImageView) view );//显示目标
    }

    //同步加载，不指定view
    public Bitmap synloadImage() throws ExecutionException, InterruptedException {
       return Glide.with( getApplicationContext() )
                .asBitmap()//转bitmap
                .load( imageUrl )
                .submit( 20, 20 )//指定宽高
                .get();//获取
    }


    //异步步加载，不指定view
    public void asyloadImage()  {
         Glide.with( getApplicationContext() )
                .asBitmap()//转bitmap
                .load( imageUrl )
                .into( new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                } );

    }
}
