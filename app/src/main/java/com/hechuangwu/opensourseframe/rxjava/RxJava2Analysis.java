package com.hechuangwu.opensourseframe.rxjava;

/**
 * Created by cwh on 2019/9/10 0010.
 * 功能:
 */
public class RxJava2Analysis {
    /**


    //创建被观察者
    Observable<String> observable = Observable.create( new ObservableOnSubscribe<String>() {
        @Override
        public void subscribe(io.reactivex.ObservableEmitter<String> emitter) throws Exception {//观察者订阅执行完成，开始发送消息
            emitter.onNext( "1" );
            emitter.onNext( "2" );
            emitter.onNext( "3" );
            emitter.onComplete();
        }
    } );
    //创建观察者，实际是一个接口类
    Observer observer = new Observer<String>() {
        @Override
        public void onSubscribe(Disposable d) {//建立订阅关系回调

        }

        @Override
        public void onNext(String o) {
        }

        @Override
        public void onError(Throwable e) {
        }

        @Override
        public void onComplete() {
        }
    };

    //建立订阅关系
    observable.subscribe( observer );

        public final void subscribe(Observer<? super T> observer) {
            ....
            subscribeActual(observer);//实际订阅方法，由实现Observable的类实现，看下面分析
            ....
        }

    //Observable类
    public abstract class Observable<T> implements ObservableSource<T> {
            //Observable的create方法
        public static <T> Observable<T> create(ObservableOnSubscribe<T> source) {
            ObjectHelper.requireNonNull( source, "source is null" );
            return RxJavaPlugins.onAssembly( new ObservableCreate<T>( source ) );//ObservableOnSubscribe接口实际被ObservableCreate持有，ObservableCreate是Observable实际实现类
            ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                public final class ObservableCreate<T> extends Observable<T> {
                    final ObservableOnSubscribe<T> source;//传入的接口
                    public ObservableCreate(io.reactivex.ObservableOnSubscribe<T> source) {
                        this.source = source;
                    }
                }
                //实际订阅方法的实现
                @Override
                protected void subscribeActual(Observer<? super T> observer) {
                    ObservableCreate.CreateEmitter<T> parent = new ObservableCreate.CreateEmitter<T>(observer);//构造发射器，持有observer，发射器源码看下面分析
                    observer.onSubscribe(parent);
                    try {
                        source.subscribe(parent);//在此处回调了ObservableOnSubscribe的subscribe
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        parent.onError(ex);
                    }
                }
            ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
        }
    }

    //发射器类，用来发送事件
    public interface ObservableEmitter<T> extends Emitter<T> {
        //父类接口
        ----------------------------------------------------------------------------------------------------------------------------------------------------------------
            public interface Emitter<T> {
                void onNext(@NonNull T value);

                void onError(@NonNull Throwable error);//执行后observer不再接受此后发送的消息

                void onComplete();//执行后observer不再接受此后发送的消息
            }
         ----------------------------------------------------------------------------------------------------------------------------------------------------------------
        void setDisposable(@Nullable Disposable d);
        void setCancellable(@Nullable Cancellable c);
        boolean isDisposed();
        ObservableEmitter<T> serialize();
        boolean tryOnError(@NonNull Throwable t);
    }

    //ObservableEmitter实际实现类CreateEmitter
    static final class CreateEmitter<T>extends AtomicReference<Disposable> implements ObservableEmitter<T>, Disposable {
        @Override
        public void onNext(T t) {
            if (t == null) {
                onError(new NullPointerException("onNext called with null. Null values are generally not allowed in 2.x operators and sources."));
                return;
            }
            if (!isDisposed()) {
                observer.onNext(t);//向观察者传递消息
            }
        }
        @Override
        public void onError(Throwable t) {
            if (!tryOnError(t)) {//error处理
                RxJavaPlugins.onError(t);
            }
        }
        @Override
        public boolean tryOnError(Throwable t) {
            if (t == null) {
                t = new NullPointerException("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
            }
            if (!isDisposed()) {//如果complete过，这里为false
                try {
                    observer.onError(t);//向观察者发送error消息
                } finally {
                    dispose();//
                }
                return true;
            }
            return false;
        }
        @Override
        public void onComplete() {
            if (!isDisposed()) {//如果error过，这里为false
                try {
                    observer.onComplete();
                } finally {
                    dispose();
                }
            }
        }
        @Override
        public void dispose() {
            DisposableHelper.dispose(this);//调用Disposable的dispose
        }
         ----------------------------------------------------------------------------------------------------------------------------------------------------------------
            public enum DisposableHelper implements Disposable {
                public static boolean dispose(AtomicReference<io.reactivex.disposables.Disposable> field) {
                    Disposable current = field.get();//获取当前的dispose接口
                   Disposable d = DISPOSED;
                    if (current != d) {
                        current = field.getAndSet( d );
                        if (current != d) {
                            if (current != null) {
                                current.dispose();//调用dispose方法
                            }
                            return true;
                        }
                    }
                    return false;
                }
            }
         ----------------------------------------------------------------------------------------------------------------------------------------------------------------
    }

    //由上面分析，被观察者调用onComplete或者onError时（onComplete或者onError只会触发一个），会触发dispose方法
    //观察者和被观察者建立订阅关系时，也会持有Disposable，也可以调用dispose方法
    public interface Disposable {
        void dispose();//用于关闭被观察者向观察者发送消息，但不影响被观察者自身的发送
        boolean isDisposed();
    }



    //指定调度器
    .subscribeOn( Schedulers.io() )

    public final Observable<T> subscribeOn(Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableSubscribeOn<T>(this, scheduler));//实际持有调度器为ObservableSubscribeOn类，可以看到使用调度器，会重新创建Observable的子类ObservableSubscribeOn，而不是上面的ObservableCreate
        ----------------------------------------------------------------------------------------------------------------------------------------------------------------
        public final class ObservableSubscribeOn<T> extends AbstractObservableWithUpstream<T, T> {
            ----------------------------------------------------------------------------------------------------------------------------------------------------------------
                abstract class AbstractObservableWithUpstream<T, U> extends Observable<U> implements HasUpstreamObservableSource<T> {//ObservableSubscribeOn的父类
                    protected final ObservableSource<T> source;
                    AbstractObservableWithUpstream(ObservableSource<T> source) {
                        this.source = source;
                    }
                    @Override
                    public final ObservableSource<T> source() {
                        return source;
                    }
                }
            ----------------------------------------------------------------------------------------------------------------------------------------------------------------
            final Scheduler scheduler;//调度器
            public ObservableSubscribeOn(ObservableSource<T> source, Scheduler scheduler) {
                super( source );
                this.scheduler = scheduler;
            }
            @Override
            public void subscribeActual(final Observer<? super T> s) {
                final ObservableSubscribeOn.SubscribeOnObserver<T> parent = new ObservableSubscribeOn.SubscribeOnObserver<T>(s);//构建SubscribeOnObserver，持有observer
                s.onSubscribe(parent);
                parent.setDisposable(scheduler.scheduleDirect(new SubscribeTask(parent)));//SubscribeTask为一个runnable，SubscribeTask持有SubscribeOnObserver，SubscribeOnObserver持有observer，所以observer会运行在子线程中
                ----------------------------------------------------------------------------------------------------------------------------------------------------------------
                    public io.reactivex.disposables.Disposable scheduleDirect(@NonNull Runnable run) {
                        return scheduleDirect(run, 0L, TimeUnit.NANOSECONDS);
                    ----------------------------------------------------------------------------------------------------------------------------------------------------------------
                            public io.reactivex.disposables.Disposable scheduleDirect(@NonNull Runnable run, long delay, @NonNull TimeUnit unit) {
                                final Worker w = createWorker();
                                final Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
                                Scheduler.DisposeTask task = new Scheduler.DisposeTask(decoratedRun, w);
                                w.schedule(task, delay, unit);
                                return task;
                        ----------------------------------------------------------------------------------------------------------------------------------------------------------------
                                static final class DisposeTask implements io.reactivex.disposables.Disposable, Runnable, SchedulerRunnableIntrospection {
                                    final Runnable decoratedRun;
                                    @Override
                                    public void run() {
                                        try {
                                            decoratedRun.run();//执行run方法
                                       ...
                                    }

                                }
                            }
                    ----------------------------------------------------------------------------------------------------------------------------------------------------------------
            }

            static final class SubscribeOnObserver<T> extends AtomicReference<Disposable> implements Observer<T>, Disposable {
                private static final long serialVersionUID = 8094547886072529208L;
                final Observer<? super T> actual;//持有observer
                final AtomicReference<io.reactivex.disposables.Disposable> s;
                SubscribeOnObserver(Observer<? super T> actual) {
                    this.actual = actual;
                    this.s = new AtomicReference<io.reactivex.disposables.Disposable>();
                }
                @Override
                public void onSubscribe(io.reactivex.disposables.Disposable s) {
                    DisposableHelper.setOnce(this.s, s);
                }
                @Override
                public void onNext(T t) {
                    actual.onNext(t);
                }
                @Override
                public void onError(Throwable t) {
                    actual.onError(t);
                }
                @Override
                public void onComplete() {
                    actual.onComplete();
                }
                @Override
                public void dispose() {
                    DisposableHelper.dispose(s);
                    DisposableHelper.dispose(this);
                }
                @Override
                public boolean isDisposed() {
                    return DisposableHelper.isDisposed(get());
                }
                void setDisposable(io.reactivex.disposables.Disposable d) {
                    DisposableHelper.setOnce(this, d);
                }
            }

            final class SubscribeTask implements Runnable {
                private final SubscribeOnObserver<T> parent;
                SubscribeTask(SubscribeOnObserver<T> parent) {
                    this.parent = parent;
                }
                @Override
                public void run() { //SubscribeOnObserver在子线程中执行
                    source.subscribe(parent);
                }
            }
        }
       ----------------------------------------------------------------------------------------------------------------------------------------------------------------
    }

     Schedulers.io();//io线程，没有线程数量限制，内部会维护线程池
     Schedulers.computation();//多少个cpu就有多少线程
     Schedulers.newThread();//每次都开启新线程
     Schedulers.single();//单例

     AndroidSchedulers.mainThread();//RxAndroid特有，用于和主线程通信

       ----------------------------------------------------------------------------------------------------------------------------------------------------------------
    public static Scheduler mainThread() {
        return RxAndroidPlugins.onMainThreadScheduler(MAIN_THREAD);
    }
    ----------------------------------------------------------------------------------------------------------------------------------------------------------------
        public final class AndroidSchedulers {
            private static final Scheduler MAIN_THREAD = RxAndroidPlugins.initMainThreadScheduler(//获取主线程Scheduler
                    new Callable<Scheduler>() {
                        @Override public Scheduler call() throws Exception {
                            return AndroidSchedulers.MainHolder.DEFAULT;
                        }
                    });
            private static final class MainHolder {
                static final Scheduler DEFAULT = new HandlerScheduler(new Handler( Looper.getMainLooper()), false);//实际Scheduler实现类为HandlerScheduler
            }
             ----------------------------------------------------------------------------------------------------------------------------------------------------------------
                final class HandlerScheduler extends Scheduler {
                    private final Handler handler;//持有Handler
                    private final boolean async;

                    //父类方法
                    public Worker createWorker() {
                        return new HandlerScheduler.HandlerWorker(handler, async);
                    }
                    private static final class HandlerWorker extends Worker {
                        private final Handler handler;
                        private final boolean async;
                        private volatile boolean disposed;
                        HandlerWorker(Handler handler, boolean async) {
                            this.handler = handler;
                            this.async = async;
                        }
                        //执行方法
                        public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
                            if (run == null) throw new NullPointerException("run == null");
                            if (unit == null) throw new NullPointerException("unit == null");
                            if (disposed) {
                                return Disposables.disposed();
                            }
                            run = RxJavaPlugins.onSchedule(run);
                            ScheduledRunnable scheduled = new ScheduledRunnable(handler, run);
                            Message message = Message.obtain(handler, scheduled);
                            message.obj = this;
                            if (async) {
                                message.setAsynchronous(true);
                            }
                            handler.sendMessageDelayed(message, unit.toMillis(delay));
                            if (disposed) {
                                handler.removeCallbacks(scheduled);//实际通过android的handler与主线程通信
                                return Disposables.disposed();
                            }
                            return scheduled;
                        }
                        @Override
                        public void dispose() {
                            disposed = true;
                            handler.removeCallbacksAndMessages(this);
                        }
                        @Override
                        public boolean isDisposed() {
                            return disposed;
                        }
                    }
                }
             ----------------------------------------------------------------------------------------------------------------------------------------------------------------
        }

  observeOn(AndroidSchedulers.mainThread() )//将observer切换回主线程,指定的是它之后的线程，可以多次调用实现线程多次切换
                    ----------------------------------------------------------------------------------------------------------------------------------------------------------------
            public final io.reactivex.Observable<T> observeOn(Scheduler scheduler) {
                return observeOn(scheduler, false, bufferSize());
                ----------------------------------------------------------------------------------------------------------------------------------------------------------------
                    public final io.reactivex.Observable<T> observeOn(Scheduler scheduler, boolean delayError, int bufferSize) {
                        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
                        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
                        return RxJavaPlugins.onAssembly(new ObservableObserveOn<T>(this, scheduler, delayError, bufferSize));//构造ObservableObserveOn类
                    ----------------------------------------------------------------------------------------------------------------------------------------------------------------
                        public final class ObservableObserveOn<T> extends AbstractObservableWithUpstream<T, T> {
                            final Scheduler scheduler;
                            final boolean delayError;
                            final int bufferSize;
                            public ObservableObserveOn(ObservableSource<T> source, Scheduler scheduler, boolean delayError, int bufferSize) {
                                super( source );
                                this.scheduler = scheduler;
                                this.delayError = delayError;
                                this.bufferSize = bufferSize;
                            }
                            @Override
                            protected void subscribeActual(Observer<? super T> observer) {
                                if (scheduler instanceof TrampolineScheduler) {
                                    source.subscribe( observer );
                                } else {
                                    Scheduler.Worker w = scheduler.createWorker();//即上面的HandlerScheduler
                                    source.subscribe( new ObservableObserveOn.ObserveOnObserver<T>( observer, w, delayError, bufferSize ) );
                                    ---------------------------------------------------------------------------------------------------------------------------------------------------------------
                                        static final class ObserveOnObserver<T> extends BasicIntQueueDisposable<T>implements Observer<T>, Runnable {
                                            void schedule() {
                                                if (getAndIncrement() == 0) {
                                                    worker.schedule( this );//执行HandlerWorker的schedule方法，从这里切换回了主线程，看上面schedule方法
                                                }
                                            }
                                        }
                                    ---------------------------------------------------------------------------------------------------------------------------------------------------------------
                                }
                            }
                        }
    ---------------------------------------------------------------------------------------------------------------------------------------------------------------

     */
}
