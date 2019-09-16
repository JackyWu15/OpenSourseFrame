package com.hechuangwu.opensourseframe.retrofit;

/**
 * Created by cwh on 2019/9/7 0007.
 * 功能: Retrofit源码分析
 */
public class RetrofitAnalysis {
    /**
    //构建者模式构建Retrofit，Retrofit的设计模式是外观模式，而Retrofit就是外观类，所有的操作都通过Retrofit类，让内部进行调用
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl( "https://api.github.com/" )//设置根地址
            .addConverterFactory( GsonConverterFactory.create() )//数据转换器
            .addCallAdapterFactory( RxJava2CallAdapterFactory.create() )//RxJava适配器
            .build();
    //动态代理
    RetrofitService retrofitService = retrofit.create( RetrofitService.class );

    //代理执行方法，返回Retrofit封装的okHttpCall
    Call okHttpCall = retrofitService.listRepos( "XXX" );

    //执行调用，实际由okHttp调用
    okHttpCall.enqueue( new Callback() {
        @Override
        public void onResponse(Call call, Response response) {

        }

        @Override
        public void onFailure(Call call, Throwable t) {

        }
    } );

    //Retrofit类
    public final class Retrofit {
        private final Map<Method, ServiceMethod<?>> serviceMethodCache = new ConcurrentHashMap<>();//
        //以下变量由Builder构建，看Builder
        final okhttp3.Call.Factory callFactory;
        final HttpUrl baseUrl;
        final List<Converter.Factory> converterFactories;
        final List<CallAdapter.Factory> callAdapterFactories;
        final Executor callbackExecutor;
        final boolean validateEagerly;

        //传入接口类，创建代理对象
        public <T> T create(final Class<T> service) {
            Utils.validateServiceInterface( service );//对接口的字节码进行验证
            if (validateEagerly) {//是否需要提前解析接口
                eagerlyValidateMethods( service );
            }
            ------------------------------------------------------------------------------------------------
            private void eagerlyValidateMethods (Class < ? > service){
                retrofit2.Platform platform = retrofit2.Platform.get();//Android平台
                for (Method method : service.getDeclaredMethods()) {
                    if (!platform.isDefaultMethod( method )) {//返回false，即loadServiceMethod一定执行
                        loadServiceMethod( method );
                    }
                }
            }

            //获取ServiceMethod
            ServiceMethod<?> loadServiceMethod (Method method){
                ServiceMethod<?> result = serviceMethodCache.get( method );//获取缓存
                if (result != null)
                    return result;//没有缓存
                synchronized (serviceMethodCache) {
                    result = serviceMethodCache.get( method );//再次判断
                    if (result == null) {
                        result = ServiceMethod.parseAnnotations( this, method );//构造ServiceMethod
                        serviceMethodCache.put( method, result );//放入缓存
                    }
                }
                return result;
            }
            ------------------------------------------------------------------------------------------------

            //动态代理模式
            return (T) Proxy.newProxyInstance( service.getClassLoader(), new Class<?>[]{service}, new InvocationHandler() {
                private final Platform platform = Platform.get();
                private final Object[] emptyArgs = new Object[0];

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getDeclaringClass() == Object.class) {//调用Object方法
                        return method.invoke( this, args );
                    }
                    if (platform.isDefaultMethod( method )) {//安卓平台的方法
                        return platform.invokeDefaultMethod( method, service, proxy, args );
                    }
                    return loadServiceMethod( method ).invoke( args != null ? args : emptyArgs );//获取ServiceMethod执行invoke方法，如果使用RxJavaAdapter返回observable，没使用返回OkHttpCall
                    ------------------------------------------------------------------------------------------------
                    abstract class ServiceMethod<T> {//抽象类，实际实现类为HttpServiceMethod

                        static <T> retrofit2.ServiceMethod<T> parseAnnotations(retrofit2.Retrofit retrofit, Method method) {//静态方法解析注解
                            RequestFactory requestFactory = RequestFactory.parseAnnotations( retrofit, method );
                            Type returnType = method.getGenericReturnType();//初步注解类型，@GET("users/{user}/repos")
                            if (Utils.hasUnresolvableType( returnType )) {
                                throw methodError( method, "Method return type must not include a type variable or wildcard: %s", returnType );
                            }
                            if (returnType == void.class) {
                                throw methodError( method, "Service methods cannot return void." );
                            }
                            return HttpServiceMethod.parseAnnotations( retrofit, method, requestFactory );//子类再次解析注解判断
                        }

                        abstract T invoke(Object[] args);
                    }
                    ------------------------------------------------------------------------------------------------
                    final class HttpServiceMethod<ResponseT, ReturnT> extends ServiceMethod<ReturnT> {
                        private final RequestFactory requestFactory;
                        private final okhttp3.Call.Factory callFactory;
                        private final CallAdapter<ResponseT, ReturnT> callAdapter;//适配器
                        private final Converter<ResponseBody, ResponseT> responseConverter;//转换器

                        //invoke方法调用
                        @Override
                        ReturnT invoke(Object[] args) {
                            //调用CallAdapter的adapt方法，如果没有使用RxJavaAdapter则默认callAdapter为DefaultCallAdapterFactory，adapt返回传入的OkHttpCall
                            //如果使用了RxJavaAdapter,则将OkHttpCall传入，返回observer(RxJava使用的是观察者模式，详细看RxJava的源码分析)，RxJavaAdapter的adapt调用，看下面RxJava2CallAdapter类
                            return callAdapter.adapt( new OkHttpCall<>( requestFactory, args, callFactory, responseConverter ) );
                        }
                         ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                        final class OkHttpCall<T> implements Call<T> {
                                            private final RequestFactory requestFactory;
                                            private final Object[] args;
                                            private final okhttp3.Call.Factory callFactory;
                                            private final Converter<ResponseBody, T> responseConverter;
                                            private volatile boolean canceled;
                                            private @Nullable
                                            okhttp3.Call rawCall;//okHttp的Call类，可以看到，最终请求是使用okHttp的Call进行请求，OkHttpCall只是再次封装了一层
                                            private @Nullable
                                            Throwable creationFailure;
                                            private boolean executed;

                                            OkHttpCall(RequestFactory requestFactory, Object[] args,
                                                       okhttp3.Call.Factory callFactory, Converter<ResponseBody, T> responseConverter) {
                                                this.requestFactory = requestFactory;
                                                this.args = args;
                                                this.callFactory = callFactory;
                                                this.responseConverter = responseConverter;
                                            }

                                            //异步请求
                                            @Override public void enqueue(final Callback<T> callback) {
                                                checkNotNull(callback, "callback == null");
                                                okhttp3.Call call;
                                                Throwable failure;

                                                synchronized (this) {
                                                    if (executed) throw new IllegalStateException("Already executed.");
                                                    executed = true;
                                                    call = rawCall;
                                                    failure = creationFailure;
                                                    if (call == null && failure == null) {
                                                        try {
                                                            call = rawCall = createRawCall();//获取ok的call
                                                        } catch (Throwable t) {
                                                            throwIfFatal(t);
                                                            failure = creationFailure = t;
                                                        }
                                                    }
                                                }
                                                if (failure != null) {
                                                    callback.onFailure(this, failure);
                                                    return;
                                                }
                                                if (canceled) {
                                                    call.cancel();
                                                }
                                                //通过ok进行网络异步请求
                                                call.enqueue(new okhttp3.Callback() {
                                                    @Override public void onResponse(okhttp3.Call call, okhttp3.Response rawResponse) {
                                                        Response<T> response;
                                                        try {
                                                            response = parseResponse(rawResponse);
                                                        } catch (Throwable e) {
                                                            throwIfFatal(e);
                                                            callFailure(e);
                                                            return;
                                                        }
                                                        try {
                                                            callback.onResponse( retrofit2.OkHttpCall.this, response);
                                                        } catch (Throwable t) {
                                                            t.printStackTrace();
                                                        }
                                                    }
                                                    @Override public void onFailure(okhttp3.Call call, IOException e) {
                                                        callFailure(e);
                                                    }

                                                    private void callFailure(Throwable e) {
                                                        try {
                                                            callback.onFailure( retrofit2.OkHttpCall.this, e);
                                                        } catch (Throwable t) {
                                                            t.printStackTrace();
                                                        }
                                                    }
                                                });
                                            }
                                            //获取ok的call
                                            private okhttp3.Call createRawCall() throws IOException {
                                                okhttp3.Call call = callFactory.newCall(requestFactory.create(args));
                                                ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                                        @Override public okhttp3.Call newCall(Request request) {
                                                            return RealCall.newRealCall(this, request, false );//最终获取到ok的RealCall
                                                        }
                                                ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

                                                if (call == null) {
                                                    throw new NullPointerException("Call.Factory returned null.");
                                                }
                                                return call;
                                            }
                                        }
                        ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

                        //再次解析注解判断
                        static <ResponseT, ReturnT> retrofit2.HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(Retrofit retrofit, Method method, RequestFactory requestFactory) {
                            CallAdapter<ResponseT, ReturnT> callAdapter = createCallAdapter( retrofit, method );//获取CallAdapter
                            Type responseType = callAdapter.responseType();
                            if (responseType == Response.class || responseType == okhttp3.Response.class) {
                                throw methodError( method, "'" + Utils.getRawType( responseType ).getName() + "' is not a valid response body type. Did you mean ResponseBody?" );
                            }
                            if (requestFactory.httpMethod.equals( "HEAD" ) && !Void.class.equals( responseType )) {
                                throw methodError( method, "HEAD method must use Void as response type." );
                            }
                            Converter<ResponseBody, ResponseT> responseConverter = createResponseConverter( retrofit, method, responseType );//获取数据转换工厂类，逻辑和获取适配器工厂类一样
                            Factory callFactory = retrofit.callFactory;
                            return new HttpServiceMethod<>( requestFactory, callFactory, callAdapter, responseConverter );//构造HttpServiceMethod
                        }

                        //获取CallAdapter
                        private static <ResponseT, ReturnT> CallAdapter<ResponseT, ReturnT> createCallAdapter(Retrofit retrofit, Method method) {
                            Type returnType = method.getGenericReturnType();//返回值类型
                            Annotation[] annotations = method.getAnnotations();//获取所有注解
                            try {
                                return (CallAdapter<ResponseT, ReturnT>) retrofit.callAdapter( returnType, annotations );//调用Retrofit的callAdapter获取callAdapter，看下面callAdapter方法
                            } catch (RuntimeException e) {
                                throw methodError( method, e, "Unable to create call adapter for %s", returnType );
                            }
                        }

                    }
                    ------------------------------------------------------------------------------------------------
                }
            } );
        }

        //返回CallAdapter
        public CallAdapter<?, ?> nextCallAdapter(CallAdapter.Factory skipPast, Type returnType, Annotation[] annotations) {
            checkNotNull( returnType, "returnType == null" );
            checkNotNull( annotations, "annotations == null" );
            int start = callAdapterFactories.indexOf( skipPast ) + 1;
            for (int i = start, count = callAdapterFactories.size(); i < count; i++) {
                CallAdapter<?, ?> adapter = callAdapterFactories.get( i ).get( returnType, annotations, this );//从缓存中获取可用CallAdapter返回，如果没有抛出异常，说明之前没添加
                if (adapter != null) {
                    return adapter;
                }
            }
            .....
        }

        //内部类构建者
        public static final class Builder {
            private final Platform platform;//获取平台，默认android平台
            okhttp3.Call.Factory callFactory;//ok工厂类
            HttpUrl baseUrl;//HttpUrl对象
            private final List<Converter.Factory> converterFactories = new ArrayList<>();//转换工厂列表
            private final List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>();//适配工厂列表
            Executor callbackExecutor;//执行器
            private boolean validateEagerly;//标志位

            //赋值平台
            Builder(Platform platform) {
                this.platform = platform;
            }

            //获取平台
            public Builder() {
                this( Platform.get() );//获取平台
                ------------------------------------------------------------------------------------------------
                class Platform {
                    private static final retrofit2.Platform PLATFORM = findPlatform();//静态工厂构造不同类型的平台，这是java提倡的

                    static retrofit2.Platform get() {
                        return PLATFORM;
                    }

                    private static retrofit2.Platform findPlatform() {
                        try {
                            Class.forName( "android.os.Build" );
                            if (Build.VERSION.SDK_INT != 0) {
                                return new retrofit2.Platform.Android();//默认android平台
                            }
                        } catch (ClassNotFoundException ignored) {
                        }
                        try {
                            Class.forName( "java.util.Optional" );
                            return new retrofit2.Platform.Java8();
                        } catch (ClassNotFoundException ignored) {
                        }
                        return new retrofit2.Platform();
                    }

                ------------------------------------------------------------------------------------------------
                }

                //设置baseUrl
                public retrofit2.Retrofit.Builder baseUrl (String baseUrl){
                    checkNotNull( baseUrl, "baseUrl == null" );//baseUrl必须有
                    return baseUrl( HttpUrl.get( baseUrl ) );
                    ------------------------------------------------------------------------------------------------
                    public static HttpUrl get (String url){ //将String类型baseUrl转成HttpUrl的类型
                        return new HttpUrl.Builder().parse( null, url ).build();
                    }

                    public retrofit2.Retrofit.Builder baseUrl (HttpUrl baseUrl){
                        checkNotNull( baseUrl, "baseUrl == null" );//HttpUrl是否为空
                        List<String> pathSegments = baseUrl.pathSegments();//HttpUrl构造过程会截碎片，这里获取碎片
                        if (!"".equals( pathSegments.get( pathSegments.size() - 1 ) )) {
                            throw new IllegalArgumentException( "baseUrl must end in /: " + baseUrl );//地址最后必须包含/
                        }
                        this.baseUrl = baseUrl;//赋值给成员变量
                        return this;
                    }

                    ------------------------------------------------------------------------------------------------
                }

                //将转换工厂对象加到列表中
                public retrofit2.Retrofit.Builder addConverterFactory (Converter.Factory factory){
                    converterFactories.add( checkNotNull( factory, "factory == null" ) );
                    return this;
                }

                //GsonConverterFactory的构造过程使用的是静态工厂构造，同样是java提倡的方式
                ------------------------------------------------------------------------------------------------
                public final class GsonConverterFactory extends Converter.Factory {
                    private final Gson gson;

                    private GsonConverterFactory(Gson gson) {
                        if (gson == null)
                            throw new NullPointerException( "gson == null" );
                        this.gson = gson;
                    }

                    public static retrofit2.converter.gson.GsonConverterFactory create() {//静态构造，传入Gson对象
                        return create( new Gson() );
                    }

                    public static retrofit2.converter.gson.GsonConverterFactory create(Gson gson) {
                        return new retrofit2.converter.gson.GsonConverterFactory( gson );
                    }
                }
                ------------------------------------------------------------------------------------------------

                //将将适配器工厂对象加到列表中
                public retrofit2.Retrofit.Builder addCallAdapterFactory (CallAdapter.Factory factory)
                {
                    callAdapterFactories.add( checkNotNull( factory, "factory == null" ) );
                    return this;
                }

                //RxJava2CallAdapterFactory的构造过程使用的是静态工厂构造，同样是java提倡的方式
                ------------------------------------------------------------------------------------------------
                public final class RxJava2CallAdapterFactory extends CallAdapter.Factory {
                    private final Scheduler scheduler;

                    private RxJava2CallAdapterFactory(Scheduler scheduler) {
                        this.scheduler = scheduler;
                    }

                    public static RxJava2CallAdapterFactory create() {
                        return new RxJava2CallAdapterFactory( null );
                    }

                    @Override
                    public CallAdapter<?> get(Type returnType, Annotation[] annotations, retrofit2.Retrofit retrofit) {
                        Class<?> rawType = getRawType( returnType );
                        if (rawType == Completable.class) {
                            return new RxJava2CallAdapter( Void.class, scheduler, false, true, false, false, false, true );//获取RxJava2CallAdapter
                        }
                        ......
                        return new RxJava2CallAdapter( responseType, scheduler, isResult, isBody, isFlowable, isSingle, isMaybe, false );
                    }
                     ------------------------------------------------------------------------------------------------

                    final class RxJava2CallAdapter implements CallAdapter<Object> {
                        @Override
                        public Type responseType() {
                            return responseType;
                        }
                        //观察者模式
                        @Override
                        public <R> Object adapt(Call<R> call) {//接口调用时执行
                            Observable<Response<R>> responseObservable = new CallObservable<>( call );//将okHttpCall封装进被观察者中
                            Observable<?> observable;
                            //根据类型构造不同被观察者
                            if (isResult) {
                                observable = new ResultObservable<>( responseObservable );
                            } else if (isBody) {
                                observable = new BodyObservable<>( responseObservable );
                            } else {
                                observable = responseObservable;
                            }
                            if (scheduler != null) {
                                observable = observable.subscribeOn( scheduler );//调用subscribeOn，执行订阅，此处看rxjava源码分析
                            }
                            return observable;//返回observable
                        }
                    }
                    ------------------------------------------------------------------------------------------------
                }
                ------------------------------------------------------------------------------------------------
                public interface CallAdapter<R, T> {//适配器抽象类

                    Type responseType();//解析返回类型

                    T adapt(Call<R> call);//Call转换为Java类型

                    abstract class Factory {
                        public abstract CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, retrofit2.Retrofit retrofit);//获取实际的adapter

                        protected static Type getParameterUpperBound(int index, ParameterizedType type) {
                            return Utils.getParameterUpperBound( index, type );
                        }

                        protected static Class<?> getRawType(Type type) {//获取原始数据类型
                            return Utils.getRawType( type );
                        }
                    }
                }
                ------------------------------------------------------------------------------------------------
                        ------------------------------------------------------------------------------------------------
                //Retrofit最终构建
                public retrofit2.Retrofit build () {
                    if (baseUrl == null) {
                        throw new IllegalStateException( "Base URL required." );//再次判断baseUrl，baseUrl必须有
                    }

                    okhttp3.Call.Factory callFactory = this.callFactory;
                    if (callFactory == null) {
                        callFactory = new OkHttpClient();//默认使用ok请求，OkHttpClient实现了Factory接口，即扩展了newCall方法，newCall方法调用返回的Call类，实际实现类为Recall，详细看看okHttp源码的分析
                    }

                    Executor callbackExecutor = this.callbackExecutor;//执行器，主线程和子线程的切换
                    if (callbackExecutor == null) {
                        callbackExecutor = platform.defaultCallbackExecutor();//如果没有，android默认为主线程
                    }
                    ------------------------------------------------------------------------------------------------
                    static class Android extends retrofit2.Platform {
                        @Override
                        public Executor defaultCallbackExecutor() {
                            return new retrofit2.Platform.Android.MainThreadExecutor();//android默认为主线程执行器
                        }
                    }
                    ------------------------------------------------------------------------------------------------

                            List < CallAdapter.Factory > callAdapterFactories = new ArrayList<>( this.callAdapterFactories );//适配器工厂列表
                    callAdapterFactories.addAll( platform.defaultCallAdapterFactories( callbackExecutor ) );//获取默认适配器工厂对象添加到列表中

                    List<Converter.Factory> converterFactories = new ArrayList<>( 1 + this.converterFactories.size() + platform.defaultConverterFactoriesSize() );

                    //将所有适配器工厂存放在一起
                    converterFactories.add( new BuiltInConverters() );
                    converterFactories.addAll( this.converterFactories );
                    converterFactories.addAll( platform.defaultConverterFactories() );

                    //构造Build
                    return new Retrofit( callFactory, baseUrl, unmodifiableList( converterFactories ), unmodifiableList( callAdapterFactories ), callbackExecutor, validateEagerly );
                }
            }


        }
    }

            *
            */
}
