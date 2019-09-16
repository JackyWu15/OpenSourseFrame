package com.hechuangwu.opensourseframe.leakcanary;

import android.content.Context;

/**
 * Created by cwh on 2019/9/12 0012.
 * 功能: 单例内存泄漏
 */
public class LeakCanarySingleton {
//静态变量和类的生命周期一致，而类被卸载需要满足下面三个条件：
//1，该类所有的实例都已经被回收，也就是java堆中不存在该类的任何实例。
//2，加载该类的ClassLoader已经被回收。
//3，该类对应的java.lang.Class对象没有任何地方被引用，无法在任何地方通过反射访问该类的方法。
//如果以上三个条件全部满足，jvm就会在方法区垃圾回收的时候对类进行卸载，类的卸载过程其实就是在方法区中清空类信息，java类的整个生命周期就结束了。

    private static LeakCanarySingleton mLeakCanarySingleton;
    private static Context mContext = null;//单例特殊，的生命周期和Application一样长，如果传入activity，当activity被关闭时，单例仍持有activity引用，导致activity不会被gc，造成内存泄漏，解决方式是传入Application
    private LeakCanarySingleton(Context context){
        this.mContext = context;
    }
    public static LeakCanarySingleton getInstance(Context context){
        if(mLeakCanarySingleton ==null){
            mLeakCanarySingleton = new LeakCanarySingleton(context);
            return mLeakCanarySingleton;
        }
        return mLeakCanarySingleton;
    }

}
