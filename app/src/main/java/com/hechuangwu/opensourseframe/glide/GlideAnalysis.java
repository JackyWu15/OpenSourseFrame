package com.hechuangwu.opensourseframe.glide;

/**
 * Created by cwh on 2019/9/11 0011.
 * 功能:Glide源码分析
 */
public class GlideAnalysis {
    /**
    //Model: url，file等
    //Data：原始数据流
    //Resource: 处理数据流的图片格式，如bitmap，png
    //TransformedResource：裁剪等处理后的资源
    //TranscodedResource: 转码后的资源，统一将bitmap转glideBitmapDrawable
    //Target：显示的目标
     第一步：1，通过GlideBuilder构建Glide，GlideBuilder成员变量RequestManagerRetriever请求检索器同时生成，build得到Glide，全局单例
             2，获取到RequestManagerRetriever检索器，并根据不同上下文，得到请求管理器RequestManager（Activity情况下特殊处理：Glide就使用了添加隐藏Fragment来监听Activity，如果Activity被销毁了，Fragment是可以监听到的）
     第二步：通过RequestManager的RequestBuilder保存路径在Object model中；
     第三步：1，glide.buildImageViewTarget()方法，构建出了一个GlideDrawableImageViewTarget对象
             2，构建一个Request，实际用来请求网络的对象，他的实际实现类为SingleRequest，这个Request保存了placeholder等参数
             3，先将SingleRequest交给RequestManager，RequestManager里有一个RequestTracker，来做排队
             4，SingleRequest里有一个Engine对象，从网络加载还是从缓存加载，都由他来处理
             5，EngineJob是Engine构造出来进行网络请求的，他持有了线程池和Runnable对象DecodeJob；
             6，DecodeJob又持有一个DataFetcher，执行DecodeJob的run方法时，将调用loadData请求网络


    Glide.with(getApplicationContext() )
    ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    public class Glide implements ComponentCallbacks2 {
        private final RequestManagerRetriever requestManagerRetriever;

        //with方法
        public static RequestManager with(@NonNull Context context) {//获取RequestManager
            return getRetriever( context ).get( context );
        }
         ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
            private static RequestManagerRetriever getRetriever (@Nullable Context context){
                return Glide.get( context ).getRequestManagerRetriever();//返回RequestManagerRetriever
            }

            public RequestManagerRetriever getRequestManagerRetriever () {
                return requestManagerRetriever;
            }
         ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
        }


        public class RequestManagerRetriever implements Handler.Callback {
            //根据不同上下文获取RequestManager
            public RequestManager get( Context context) {
                if (context == null) {
                    throw new IllegalArgumentException( "You cannot start a load on a null Context" );
                } else if (Util.isOnMainThread() && !(context instanceof Application)) {
                    if (context instanceof FragmentActivity) {
                        return get( (FragmentActivity) context );//上下文为FragmentActivity
                        ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                            public RequestManager get(FragmentActivity activity) {
                                if (Util.isOnBackgroundThread()) {
                                    return get(activity.getApplicationContext());
                                } else {
                                    assertNotDestroyed(activity);
                                    FragmentManager fm = activity.getSupportFragmentManager();
                                    return supportFragmentGet(activity, fm, null, isActivityVisible(activity));
                                    ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                        private RequestManager supportFragmentGet(Context context,FragmentManager fm,Fragment parentHint,boolean isParentVisible) {
                                            SupportRequestManagerFragment current = getSupportRequestManagerFragment(fm, parentHint, isParentVisible);//构造一个SupportRequestManagerFragment，通过fragment来监听生命周期
                                            RequestManager requestManager = current.getRequestManager();//返回当前RequestManager
                                            if (requestManager == null) {//如果没有RequestManager重新构造
                                                Glide glide = Glide.get(context);
                                                requestManager =factory.build(glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode(), context);
                                                current.setRequestManager(requestManager);//将空fragment和RequestManager进行关联，SupportRequestManagerFragment会持有Lifecycle,通过Lifecycle来管理空Fragment，从而管理RequestManager的生命周期
                                            }
                                            return requestManager;
                                        }
                                    ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                }
                            }
                    } else if (context instanceof Activity) {
                        return get( (Activity) context );//上下文为Activity
                    } else if (context instanceof ContextWrapper) {
                        return get( ((ContextWrapper) context).getBaseContext() );
                    }
                }
                return getApplicationManager( context );////上下文为Application时调用
                ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                    private RequestManager getApplicationManager(@NonNull Context context){//为单例
                        if (applicationManager == null) {
                            synchronized (this) {
                                if (applicationManager == null) {
                                    Glide glide = Glide.get( context.getApplicationContext() );
                                    applicationManager = factory.build( glide, new ApplicationLifecycle(), new EmptyRequestManagerTreeNode(), context.getApplicationContext() );
                                }
                            }
                        }
                    }
                ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
            }
        }

        //RequestManager是真正用于管理图片网络请求的类
        public class RequestManager implements LifecycleListener,ModelTypes<RequestBuilder<Drawable>> {
            public RequestBuilder<Drawable> load(@Nullable String string) {
              return asDrawable().load(string);
            }

        }

    .into( (ImageView) view );//显示目标
     ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    public ViewTarget<ImageView, TranscodeType> into(@NonNull ImageView view) {
        Util.assertMainThread();//是否在主线程
        BaseRequestOptions<?> requestOptions = this;
        //判断是否设置了图片类型，根据不同设置类型对图片进行转化
        if (!requestOptions.isTransformationSet()&& requestOptions.isTransformationAllowed()&& view.getScaleType() != null) {
            switch (view.getScaleType()) {
                case CENTER_CROP:
                    requestOptions = requestOptions.clone().optionalCenterCrop();
                    break;
                case CENTER_INSIDE:
                    requestOptions = requestOptions.clone().optionalCenterInside();
                    break;
                case FIT_CENTER:
                case FIT_START:
                case FIT_END:
                    requestOptions = requestOptions.clone().optionalFitCenter();
                    break;
                case FIT_XY:
                    requestOptions = requestOptions.clone().optionalCenterInside();
                    break;
                case CENTER:
                case MATRIX:
                default:
            }
        }

        //构建目标
        return into(glideContext.buildImageViewTarget(view, transcodeClass),  null,requestOptions, Executors.mainThreadExecutor());
        ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
        private <Y extends Target<TranscodeType>> Y into(@NonNull Y target,@Nullable RequestListener<TranscodeType> targetListener,BaseRequestOptions<?> options,Executor callbackExecutor) {
            Preconditions.checkNotNull(target);
            if (!isModelSet) {
                throw new IllegalArgumentException("You must call #load() before calling #into()");
            }
            Request request = buildRequest(target, targetListener, options, callbackExecutor);//创建新的Request
            Request previous = target.getRequest();//上一个request
            if (request.isEquivalentTo(previous)&& !isSkipMemoryCacheWithCompletePreviousRequest(options, previous)) {
                request.recycle();
                if (!Preconditions.checkNotNull(previous).isRunning()) {
                    previous.begin();
                }
                return target;
            }
            requestManager.clear(target);//
            target.setRequest(request);//将目标和新的Request绑定
            requestManager.track(target, request);

            return target;
        }
        ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    }
     ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
        */
 }

