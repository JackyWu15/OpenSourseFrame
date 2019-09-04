package com.hechuangwu.opensourseframe.okhttp;

/**
 * Created by cwh on 2019/9/2 0002.
 * 功能: okHttp源码分析
 */
public class OkHttpAnalysis {
/**

 //通过构建者模式，构建OkHttpClient
       new OkHttpClient.Builder();

 //OkHttpClient构建类
   public static final class Builder {
     Dispatcher dispatcher;//分发器
     .......
     ConnectionPool connectionPool;//连接池
     ......
     public Builder() {
       dispatcher = new Dispatcher();
      .....
       connectionPool = new ConnectionPool();
     }

 //通过构建者模式，构建Request
   new Request.Builder()

 //Request构建类
   public static class Builder {
      ....
     String method;
     Headers.Builder headers;
      .....
     public Builder() {
         this.method = "GET";//默认get请求
         this.headers = new Headers.Builder();//请求头
     }

 //构建Call，与服务端建立连接
    okHttpClient.newCall( request );

 //实际上构造了RealCall
    public Call newCall(Request request) {
     return RealCall.newRealCall(this, request, false )
     }

 //调用静态方法构造RealCall
    static RealCall newRealCall(OkHttpClient client, Request originalRequest, boolean forWebSocket) {
     RealCall call = new RealCall(client, originalRequest, forWebSocket);//构造RealCall
     ....
     return call;
   }

 //RealCall构造
     private RealCall(OkHttpClient client, Request originalRequest, boolean forWebSocket) {
      this.client = client;//持有okHttpClient
      this.originalRequest = originalRequest;//持有Request
      this.forWebSocket = forWebSocket;//socket
      this.retryAndFollowUpInterceptor = new RetryAndFollowUpInterceptor(client, forWebSocket);// 拦截器
    ...
    }

第一种请求：同步请求
 ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //执行同步请求
    call.execute();

 // 由RealCall调用execute();
    final class RealCall implements Call {
        ......
        public Response execute() throws IOException {
             synchronized (this) {
             if (executed) throw new IllegalStateException("Already Executed");//同步请求只执行一次，如果执行过了executed为true，再次执行抛出异常
                 executed = true;
             }
             captureCallStackTrace();//捕捉异常的堆栈信息
             timeout.enter();
             eventListener.callStart(this);//事件监听，请求时被调用
             try {
                 client.dispatcher().executed(this); //获取到okHttpClient的Dispatcher分发器，将RealCall加入队列中
                    ------------------------------------------------------------------------------------------------
                        -> Dispatcher分发器(核心类）
                             public final class Dispatcher {
                                private final Deque<RealCall.AsyncCall> readyAsyncCalls = new ArrayDeque<>();//异步就绪队列
                                private final Deque<RealCall.AsyncCall> runningAsyncCalls = new ArrayDeque<>();//异步执行队列
                                private final Deque<RealCall> runningSyncCalls = new ArrayDeque<>();//同步请求队列
                                ......
                        -> 执行同步方法，RealCall添加到队列中
                                synchronized void executed(RealCall call) {
                                   runningSyncCalls.add(call);//加入同步请求队列
                                }
                                .....
                        }
                    ------------------------------------------------------------------------------------------------------
             Response result = getResponseWithInterceptorChain();//获取拦截器，此处看异步请求分析
             if (result == null) throw new IOException("Canceled");
                 return result;
             } catch (IOException e) {
                 e = timeoutExit(e);
                 eventListener.callFailed(this, e);
                 throw e;
             } finally {
                client.dispatcher().finished(this);//回收同步请求
                    ------------------------------------------------------------------------------------------------------
                        ->调用Dispatcher的finished
                        public final class Dispatcher {
                            .....
                            void finished(RealCall call) {
                                finished(runningSyncCalls, call);//实际执行了finished(Deque<T> calls, T call)
                             }

                        ->实际执行了finished(Deque<T> calls, T call)
                             private <T> void finished(Deque<T> calls, T call) {
                                 Runnable idleCallback;
                                 synchronized (this) {
                                     if (!calls.remove(call)) throw new AssertionError("Call wasn't in-flight!");//从当前队列移除这个同步请求，即，无论执行失败还是成功，当前请求都会被移除
                                     idleCallback = this.idleCallback;
                                 }
                                 boolean isRunning = promoteAndExecute();//是否还有其他请求正在执行

                                 if (!isRunning && idleCallback != null) {
                                     idleCallback.run();
                                }
                            }
                            ......
                         }
                    ------------------------------------------------------------------------------------------------------
             }
         }
    }


 第二种请求：异步请求
 ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //执行异步请求
    call.enqueue(Callback responseCallback)

    //由RealCall调用enqueue(Callback responseCallback);
     final class RealCall implements Call {
     ......
        public void enqueue(Callback responseCallback) {
             synchronized (this) {
                 if (executed) throw new IllegalStateException("Already Executed");//一个call只执行一次，如果执行过了executed为true，再次执行抛出异常
                    executed = true;
             }
             captureCallStackTrace();
             eventListener.callStart(this);
              // 调用Dispatcher的enqueue
             client.dispatcher().enqueue(new AsyncCall(responseCallback));
                -------------------------------------------------------------------------------------------------------
                    public final class Dispatcher {
                         private ExecutorService executorService;//线程池
                         private int maxRequests = 64;//最大请求数
                         private int maxRequestsPerHost = 5;//每个主机最大请求数
                         private final Deque<RealCall.AsyncCall> readyAsyncCalls = new ArrayDeque<>();//就绪的异步队列
                         private final Deque<RealCall.AsyncCall> runningAsyncCalls = new ArrayDeque<>();//正在执行的异步队列(包含取消以及没有执行完的请求）
                            ......
                    ->执行enqueue
                         void enqueue(AsyncCall call) {
                             synchronized (this) {
                                readyAsyncCalls.add(call);//AsyncCall为一个Runnable类，将AsyncCall加入就绪队列中
                             }
                            promoteAndExecute();//执行异步请求
                            --------------------------------------------------------------------------------------------------
                                 private boolean promoteAndExecute() {
                                     assert (!Thread.holdsLock(this));
                                     List<AsyncCall> executableCalls = new ArrayList<>();//打算要执行的请求列表
                                     boolean isRunning;//是否有请求正在执行
                                     //遍历就绪队列
                                     synchronized (this) {
                                         for (Iterator<AsyncCall> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
                                             AsyncCall asyncCall = i.next();//获取当前的AsyncCall用于执行

                                             if (runningAsyncCalls.size() >= maxRequests) break; // Max capacity.//请求数等于或超过最大数跳出循环
                                             if (runningCallsForHost(asyncCall) >= maxRequestsPerHost) continue; // 是否超过主机最大请求数

                                             i.remove();//从就绪中移除
                                             executableCalls.add(asyncCall);//将要执行的AsyncCall添加到执行列表中
                                             runningAsyncCalls.add(asyncCall);//添加到正在执行的异步队列
                                         }
                                         isRunning = runningCallsCount() > 0;正在执行的同步和异步请求是否大于0
                                     }

                                    //遍历准备要执行的请求列表
                                     for (int i = 0, size = executableCalls.size(); i < size; i++) {
                                         AsyncCall asyncCall = executableCalls.get(i);
                                     //调用AsyncCall的executeOn让线程池执行异步请求
                                         asyncCall.executeOn(executorService());//executorService()初始化线程池后，并调用asyncCall.executeOn
                                             ->executorService()初始化线程池(单例模式)
                                             --------------------------------------------------------------------------------------------------
                                                 public synchronized ExecutorService executorService() {
                                                 if (executorService == null) {
                                                 executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,new SynchronousQueue<Runnable>(), Util.threadFactory("OkHttp Dispatcher", false));
                                                 }
                                                 return executorService;
                                                 }
                                             -----------------------------------------------------------------------------------------------------

                                            ->调用asyncCall.executeOn()看下面的类分析
                                     }
                                --------------------------------------------------------------------------------------------------
                        }
                        return isRunning;
                        }
                    }
         }


            //RealCall的内部类AsyncCall类
             final class AsyncCall extends NamedRunnable {
                 private final Callback responseCallback;
                 AsyncCall(Callback responseCallback) {
                     super("OkHttp %s", redactedUrl());
                     this.responseCallback = responseCallback;
                 }
                 ......
                //线程池执行异步请求
                void executeOn(ExecutorService executorService) {
                    assert (!Thread.holdsLock(client.dispatcher()));
                    boolean success = false;
                    try {
                        executorService.execute(this);//传入runnable，由线程池执行run()->execute()
                         --------------------------------------------------------------------------------------------------------------------
                                         //AsyncCall继承的NamedRunnable
                                         public abstract class NamedRunnable implements Runnable {
                                             protected final String name;
                                             public NamedRunnable(String format, Object... args) {
                                                this.name = Util.format(format, args);
                                             }
                                             @Override
                                             public final void run() {
                                                 String oldName = Thread.currentThread().getName();
                                                 Thread.currentThread().setName(name);
                                                 try {
                                                    execute();//执行子类方法进行异步调用
                                                 } finally {
                                                     Thread.currentThread().setName(oldName);
                                                 }
                                             }
                                             protected abstract void execute();//子类重写，即AsyncCall重写
                                         }
                        --------------------------------------------------------------------------------------------------------------------
                        success = true;//是否成功请求
                    } catch (RejectedExecutionException e) {
                        InterruptedIOException ioException = new InterruptedIOException("executor rejected");
                        ioException.initCause(e);
                        eventListener.callFailed( RealCall.this, ioException);
                        responseCallback.onFailure(RealCall.this, ioException);
                    } finally {
                        if (!success) {
                            client.dispatcher().finished(this); // This call is no longer running!
                        }
                    }
                }

                 //NamedRunnable->run()->execute()线程执行异步请求
                protected void execute() {
                        boolean signalledCallback = false;
                        timeout.enter();
                        try {
                            Response response = getResponseWithInterceptorChain();//构造各种拦截器
                            ---------------------------------------------------------------------------------------------------------
                                 Response getResponseWithInterceptorChain() throws IOException {
                                     List<Interceptor> interceptors = new ArrayList<>();//拦截器列表
                                     interceptors.addAll(client.interceptors());//用户自定义的拦截器（非系统拦截器）
                                     interceptors.add(retryAndFollowUpInterceptor);//重定向拦截器
                                     interceptors.add(new BridgeInterceptor(client.cookieJar()));//桥接适配拦截器
                                     interceptors.add(new CacheInterceptor(client.internalCache()));//缓存拦截器
                                     interceptors.add(new ConnectInterceptor(client));//链接拦截器
                                     if (!forWebSocket) {
                                        interceptors.addAll(client.networkInterceptors());//网络拦截器（非系统拦截器）
                                     }
                                     interceptors.add(new CallServerInterceptor(forWebSocket));//服务拦截器
                                      //将拦截器列表构造成第一个拦截器链的链环,实际实现类为RealInterceptorChain(链环类）
                                     Interceptor.Chain chain = new RealInterceptorChain(interceptors, null, null, null, 0,originalRequest, this, eventListener, client.connectTimeoutMillis(),client.readTimeoutMillis(), client.writeTimeoutMillis());
                                    //调用proceed方法
                                    return chain.proceed(originalRequest);

                                    5个系统拦截器由上至下依次构造，而response由下往上依次返回，RealInterceptorChain()->RetryAndFollowUpInterceptor()->BridgeInterceptor()->CacheInterceptor()->ConnectInterceptor()->CallServerInterceptor()
                                    ---------------------------------------------------------------------------------------------------------
                                             public Response proceed(Request request) throws IOException {
                                                return proceed(request, streamAllocation, httpCodec, connection);
                                             }

                                             public Response proceed(Request request, StreamAllocation streamAllocation, HttpCodec httpCodec,RealConnection connection) throws IOException {
                                                ......//角标越界等异常判断
                                                //构建下一个链环
                                                 RealInterceptorChain next = new RealInterceptorChain(interceptors, streamAllocation, httpCodec,connection, index + 1, request, call, eventListener, connectTimeout, readTimeout,writeTimeout);
                                                 Interceptor interceptor = interceptors.get(index);//获取第一个拦截器，即RetryAndFollowUpInterceptor
                                                 Response response = interceptor.intercept(next);//执行intercept，将下一个链环传入，实际拦截器为RetryAndFollowUpInterceptor
                                             ---------------------------------------------------------------------------------------------------------
                                                    ->RetryAndFollowUpInterceptor的intercept方法
                                                         Response intercept(Chain chain) throws IOException {
                                                             Request request = chain.request();//从第一个拦截链环中获取请求对象Request
                                                             RealInterceptorChain realChain = (RealInterceptorChain) chain;//强转成第一个拦截器RealInterceptorChain
                                                             Call call = realChain.call();//获取Call对象

                                                            //建立执行请求的网络组件，建立流的链接，会在ConnectInterceptor拦截器中使用
                                                             StreamAllocation streamAllocation = new StreamAllocation(client.connectionPool(),createAddress(request.url()), call, eventListener, callStackTrace);
                                                                .....
                                                             Response response;
                                                             boolean releaseConnection = true;
                                                             try {
                                                                //可以看到，通过递归调用RealInterceptorChain构造下一个链环，并获取下一个拦截器，连接成一条拦截器链，下个拦截器为BridgeInterceptor
                                                                 response = realChain.proceed(request, streamAllocation, null, null);
                                                                 releaseConnection = false;
                                                             }

                                                             if (++followUpCount > MAX_FOLLOW_UPS) {//重连次数，默认为20次
                                                                 streamAllocation.release();
                                                                 throw new ProtocolException("Too many follow-up requests: " + followUpCount);
                                                             }
                                                         return response;
                                                         }
                                             ---------------------------------------------------------------------------------------------------------
                                                  ->BridgeInterceptor的intercept方法，可以看到桥接拦截器，主要任务是在Request中添加请求头信息
                                                         public final class BridgeInterceptor implements Interceptor {
                                                             public Response intercept(Chain chain) throws IOException {
                                                             Request userRequest = chain.request();
                                                             Request.Builder requestBuilder = userRequest.newBuilder();

                                                            RequestBody body = userRequest.body();
                                                             if (body != null) {
                                                                 MediaType contentType = body.contentType();
                                                             if (contentType != null) {
                                                                 requestBuilder.header("Content-Type", contentType.toString());
                                                             }
                                                             long contentLength = body.contentLength();
                                                             if (contentLength != -1) {
                                                                 requestBuilder.header("Content-Length", Long.toString(contentLength));
                                                                 requestBuilder.removeHeader("Transfer-Encoding");
                                                             } else {
                                                                 requestBuilder.header("Transfer-Encoding", "chunked");
                                                                 requestBuilder.removeHeader("Content-Length");
                                                             }
                                                             }
                                                             if (userRequest.header("Host") == null) {
                                                                 requestBuilder.header("Host", hostHeader(userRequest.url(), false));
                                                             }
                                                             if (userRequest.header("Connection") == null) {
                                                                requestBuilder.header("Connection", "Keep-Alive");
                                                             }
                                                             boolean transparentGzip = false;
                                                             if (userRequest.header("Accept-Encoding") == null && userRequest.header("Range") == null) {
                                                                 transparentGzip = true;
                                                                 requestBuilder.header("Accept-Encoding", "gzip");
                                                             }

                                                             List<Cookie> cookies = cookieJar.loadForRequest(userRequest.url());
                                                             if (!cookies.isEmpty()) {
                                                                 requestBuilder.header("Cookie", cookieHeader(cookies));
                                                            }
                                                             if (userRequest.header("User-Agent") == null) {
                                                                 requestBuilder.header("User-Agent", Version.userAgent());
                                                             }

                                                             Response networkResponse = chain.proceed(requestBuilder.build());//下一个拦截器为CacheInterceptor
                                                                ......
                                                             }

                                             ---------------------------------------------------------------------------------------------------------
                                                    ->CacheInterceptor使用到的缓存类
                                                        public final class Cache implements Closeable, Flushable {
                                                             final DiskLruCache cache;//实际上使用的是LRU算法
                                                                ......
                                                             //put保存方法
                                                             CacheRequest put(Response response) {
                                                             String requestMethod = response.request().method();//获取请求方式
                                                             if (HttpMethod.invalidatesCache(response.request().method())) {//请求为post,put,patch,move,delete等请求不需要缓存
                                                                 try {
                                                                    remove(response.request());
                                                                 } catch (IOException ignored) {
                                                                 }
                                                                  return null;
                                                             }
                                                             if (!requestMethod.equals("GET")) {//get请求才需要进行缓存
                                                                  return null;
                                                             }
                                                             Entry entry = new Entry(response);//封装了header信息的类，用来缓存编码，协议等头部信息
                                                             DiskLruCache.Editor editor = null;//
                                                             try {
                                                                editor = cache.edit(key(response.request().url()));//将请求地址用md5加密，封装为key
                                                             if (editor == null) {
                                                                 return null;
                                                             }
                                                                entry.writeTo(editor);//将Entry保存的header信息缓存到buffer中
                                                                return new CacheRequestImpl(editor);//最终会将header和body的缓存封装到CacheRequestImpl中
                                                            ------------------------------------------------------------------------------------------------------------------
                                                                         private final class CacheRequestImpl implements CacheRequest {//实现了CacheRequest接口，用于CacheInterceptor的调用
                                                                             private final DiskLruCache.Editor editor;//header信息缓存
                                                                             private Sink cacheOut;//Entry构造的body缓存
                                                                             private Sink body;//body缓存

                                                                             CacheRequestImpl(final DiskLruCache.Editor editor) {
                                                                             this.editor = editor;
                                                                             this.cacheOut = editor.newSink(ENTRY_BODY);//通过Entry构造的body缓存
                                                                             this.body = new ForwardingSink(cacheOut) {//将body缓存再封装一层为ForwardingSink
                                                                                     @Override
                                                                                     public void close() throws IOException {
                                                                                         synchronized (Cache.this) {
                                                                                             if (done) {
                                                                                                 return;
                                                                                             }
                                                                                             done = true;
                                                                                             writeSuccessCount++;
                                                                                             }
                                                                                             super.close();
                                                                                             editor.commit();
                                                                                             }
                                                                                             };
                                                                             }
                                                                              @Override
                                                                              public void abort() {  //CacheRequest接口方法,
                                                                              synchronized (Cache.this) {
                                                                                  if (done) {
                                                                                      return;
                                                                                  }
                                                                                      done = true;
                                                                                      writeAbortCount++;
                                                                              }
                                                                              Util.closeQuietly(cacheOut);
                                                                              try {
                                                                                 editor.abort();
                                                                              } catch (IOException ignored) {
                                                                                 }
                                                                              }
                                                                              @Override
                                                                              public Sink body() { //CacheRequest接口方法，返回缓存body，即ForwardingSink
                                                                                 return body;
                                                                              }
                                                                        }
                                                            ------------------------------------------------------------------------------------------------------------------
                                                             } catch (IOException e) {
                                                                 abortQuietly(editor);
                                                              return null;
                                                             }
                                                             }

                                                            //get获取缓存内容方法
                                                            Response get(Request request) {
                                                                String key = key(request.url());//获取key
                                                                DiskLruCache.Snapshot snapshot;//缓存快照
                                                                Entry entry;//缓存Entry
                                                                try {
                                                                    snapshot = cache.get(key);//通过key获取缓存快照
                                                                    if (snapshot == null) {
                                                                    return null;
                                                                     }
                                                                } catch (IOException e) {
                                                                    return null;
                                                                }
                                                                try {
                                                                    entry = new Entry(snapshot.getSource(ENTRY_METADATA));//如果缓存快照存在，那么构造Entry
                                                                } catch (IOException e) {
                                                                    Util.closeQuietly(snapshot);
                                                                    return null;
                                                                }
                                                                Response response = entry.response(snapshot);//通过快缓存照获取响应体
                                                                if (!entry.matches(request, response)) {
                                                                    Util.closeQuietly(response.body());
                                                                    return null;
                                                                }
                                                                    return response;
                                                            }
                                                        }
                                             ---------------------------------------------------------------------------------------------------------
                                              ->CacheInterceptor的intercept方法
                                                  public final class CacheInterceptor implements Interceptor {
                                                        Response intercept(Chain chain) throws IOException {
                                                        Response cacheCandidate = cache != null? cache.get(chain.request()): null;//尝试获取缓存
                                                        long now = System.currentTimeMillis();
                                                        CacheStrategy strategy = new CacheStrategy.Factory(now, chain.request(), cacheCandidate).get();//工厂模式和策略模式，通过网络还是缓存请求
                                                        Request networkRequest = strategy.networkRequest;//网络请求
                                                        Response cacheResponse = strategy.cacheResponse;//缓存请求
                                                        if (cache != null) {
                                                            cache.trackResponse(strategy);//缓存的命中情况
                                                        }
                                                        if (cacheCandidate != null && cacheResponse == null) {//没有缓存关闭资源
                                                            closeQuietly(cacheCandidate.body());
                                                        }
                                                        if (networkRequest == null && cacheResponse == null) {//没有网络也没有缓存，构建一个Response返回504
                                                            return new Response.Builder()
                                                                                .request(chain.request())
                                                                                .protocol(Protocol.HTTP_1_1)
                                                                                .code(504)
                                                                                .message("Unsatisfiable Request (only-if-cached)")
                                                                                .body(Util.EMPTY_RESPONSE)
                                                                                .sentRequestAtMillis(-1L)
                                                                                .receivedResponseAtMillis(System.currentTimeMillis())
                                                                                .build();
                                                        }
                                                        if (networkRequest == null) {//没有网络有缓存，则返回缓存
                                                            return cacheResponse.newBuilder()
                                                                                .cacheResponse(stripBody(cacheResponse))
                                                                                .build();
                                                        }
                                                        Response networkResponse = null;
                                                        try {
                                                            networkResponse = chain.proceed(networkRequest);//如果没有缓存，则获取下一个拦截器，为ConnectInterceptor
                                                        } finally {
                                                            if (networkResponse == null && cacheCandidate != null) {
                                                                closeQuietly(cacheCandidate.body());//关闭资源
                                                            }
                                                        }
                                                        }}
                                                        if (cacheResponse != null) { //有网络，这里再次判断是否要使用缓存
                                                            if (networkResponse.code() == HTTP_NOT_MODIFIED) {//状态码304，获取缓存
                                                                Response response = cacheResponse.newBuilder()
                                                                                                .headers(combine(cacheResponse.headers(), networkResponse.headers()))
                                                                                                .sentRequestAtMillis(networkResponse.sentRequestAtMillis())
                                                                                                .receivedResponseAtMillis(networkResponse.receivedResponseAtMillis())
                                                                                                .cacheResponse(stripBody(cacheResponse))
                                                                                                .networkResponse(stripBody(networkResponse))
                                                                                                .build();
                                                                    networkResponse.body().close();
                                                                    cache.trackConditionalCacheHit();
                                                                    cache.update(cacheResponse, response);
                                                                return response;
                                                            } else {
                                                                closeQuietly(cacheResponse.body());
                                                                }
                                                        }
                                                        Response response = networkResponse.newBuilder()//最终没有缓存，则构造网络请求
                                                                                            .cacheResponse(stripBody(cacheResponse))
                                                                                            .networkResponse(stripBody(networkResponse))
                                                                                            .build();
                                                        if (cache != null) {
                                                            if (HttpHeaders.hasBody(response) && CacheStrategy.isCacheable(response, networkRequest)) {//可以被缓存起来
                                                                CacheRequest cacheRequest = cache.put(response);//将缓存保存起来，即调用在上面分析的Cache类进行保存
                                                            return cacheWritingResponse(cacheRequest, response);
                                                        }

                                                        if (HttpMethod.invalidatesCache(networkRequest.method())) {//请求为post,put,patch,move,delete等请求不需要缓存
                                                            try {
                                                                cache.remove(networkRequest);
                                                            } catch (IOException ignored) {
                                                            }
                                                        }
                                                        }
                                                        return response;
                                                        }

                                              ---------------------------------------------------------------------------------------------------------------------------------
                                              ->ConnectInterceptor的intercept方法
                                                     public final class ConnectInterceptor implements Interceptor {
                                                          public Response intercept(Chain chain) throws IOException {
                                                               RealInterceptorChain realChain = (RealInterceptorChain) chain;
                                                               Request request = realChain.request();
                                                               StreamAllocation streamAllocation = realChain.streamAllocation();//在RetryAndFollowUpInterceptor拦截器创建，从网络io流中读取数据
                                                               boolean doExtensiveHealthChecks = !request.method().equals("GET");
                                                               HttpCodec httpCodec = streamAllocation.newStream(client, chain, doExtensiveHealthChecks);//HttpCodec为一个接口，对应HttpCodec1和HttpCodec2，即1.0和2.0，主要用于编码request和解码response，
                                                                              ---------------------------------------------------------------------------------------------------------------------------------
                                                                              public final class StreamAllocation  {
                                                                                  private final ConnectionPool connectionPool;//连接池，看下面连接池分析
                                                                                  private final Route route;
                                                                                  public HttpCodec newStream(OkHttpClient client, Interceptor.Chain chain, boolean doExtensiveHealthChecks) {
                                                                                        ......
                                                                                      RealConnection resultConnection = findHealthyConnection(connectTimeout, readTimeout,writeTimeout, pingIntervalMillis, connectionRetryEnabled, doExtensiveHealthChecks);
                                                                                                ---------------------------------------------------------------------------------------------------------------------------------
                                                                                                  private RealConnection findHealthyConnection(int connectTimeout, int readTimeout,int writeTimeout, int pingIntervalMillis, boolean connectionRetryEnabled,boolean doExtensiveHealthChecks) throws IOException {
                                                                                                      while (true) {
                                                                                                        RealConnection candidate = findConnection(connectTimeout, readTimeout, writeTimeout,pingIntervalMillis, connectionRetryEnabled);
                                                                                                              ---------------------------------------------------------------------------------------------------------------------------------
                                                                                                                  private RealConnection findConnection(int connectTimeout, int readTimeout, int writeTimeout,int pingIntervalMillis, boolean connectionRetryEnabled) throws IOException {
                                                                                                                        ......
                                                                                                                        Internal.instance.get(connectionPool, address, this, null);//从连接池中获取RealConnection
                                                                                                                          if (connection != null) {
                                                                                                                             foundPooledConnection = true;
                                                                                                                             result = connection;//保存RealConnection
                                                                                                                          } else {
                                                                                                                            selectedRoute = route;
                                                                                                                          }
                                                                                                                            .....
                                                                                                                          result.connect(connectTimeout, readTimeout, writeTimeout, pingIntervalMillis,connectionRetryEnabled, call, eventListener);//开始建立连接，一种隧道连接，一种socket连接
                                                                                                                            ......
                                                                                                                        Internal.instance.put(connectionPool, result);//使用完成放入连接池中
                                                                                                              ---------------------------------------------------------------------------------------------------------------------------------
                                                                                                ---------------------------------------------------------------------------------------------------------------------------------
                                                                                      HttpCodec resultCodec = resultConnection.newCodec(client, chain, this);//request编码和response解码的对象
                                                                                      ......
                                                                                    }
                                                                                    ......
                                                                                //连接池里被调用的acquire方法，看下面连接池分析
                                                                                  public void acquire(RealConnection connection, boolean reportedAcquired) {
                                                                                          assert (Thread.holdsLock(connectionPool));
                                                                                      if (this.connection != null) throw new IllegalStateException();
                                                                                          this.connection = connection;//将可用的RealConnection赋值给成员变量
                                                                                          this.reportedAcquired = reportedAcquired;//这里赋值为true
                                                                                          connection.allocations.add(new StreamAllocationReference(this, callStackTrace));//将弱引用添加到allocations列表中，通过判断这个集合的数量来判断是否已超过连接的最大值
                                                                                  }
                                                                              }
                                                                              ---------------------------------------------------------------------------------------------------------------------------------
                                                               RealConnection connection = streamAllocation.connection();//获取以上构造完成的RealConnection
                                                               return realChain.proceed(request, streamAllocation, httpCodec, connection);//下一个拦截器为CallServerInterceptor
                                                           }
                                                   }
                                             -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                             ->ConnectInterceptor使用的连接池ConnectionPool，用于存储RealConnection
                                                    public final class ConnectionPool {
                                                            ......
                                                            Deque<RealConnection> connections = new ArrayDeque<>();//线程池本质是一个Deque双队列
                                                            ......

                                                             private final Runnable cleanupRunnable = new Runnable() {//用于异步清理的runnable
                                                                 @Override public void run() {
                                                                     while (true) {//死循环
                                                                         long waitNanos = cleanup(System.nanoTime());//与下一次需要清理的间隔时间
                                                                            ----------------------------------------------------------------------------------------------------------------
                                                                                 long cleanup(long now) {//使用标记算法，来标记哪些对象需要进行清理
                                                                                     synchronized (this) {
                                                                                         for (Iterator<RealConnection> i = connections.iterator(); i.hasNext(); ) {//遍历Deque
                                                                                         RealConnection connection = i.next();
                                                                                         if (pruneAndGetAllocationCount(connection, now) > 0) {//通过引用计数算法来回收RealConnection
                                                                                   ----------------------------------------------------------------------------------------------------------------
                                                                                                     private int pruneAndGetAllocationCount(RealConnection connection, long now) {//引用计数算法是通过判断对象的引用个数是否为0，如果引用数为0，则对象被标记为清除的对象
                                                                                                         List<Reference<StreamAllocation>> references = connection.allocations;
                                                                                                         for (int i = 0; i < references.size(); ) {
                                                                                                             Reference<StreamAllocation> reference = references.get(i);
                                                                                                             if (reference.get() != null) {
                                                                                                                 i++;
                                                                                                                 continue;
                                                                                                             }
                                                                                                             StreamAllocation.StreamAllocationReference streamAllocRef = StreamAllocation.StreamAllocationReference) reference;
                                                                                                             String message = "A connection to " + connection.route().address().url()
                                                                                                             references.remove(i);
                                                                                                             connection.noNewStreams = true;
                                                                                                             if (references.isEmpty()) {
                                                                                                                 connection.idleAtNanos = now - keepAliveDurationNs;
                                                                                                                 return 0;//引用数为0，被标记为要清除
                                                                                                             }
                                                                                                         }
                                                                                                         return references.size();//返回引用计数
                                                                                                     }
                                                                                   ----------------------------------------------------------------------------------------------------------------

                                                                            ----------------------------------------------------------------------------------------------------------------
                                                                         if (waitNanos == -1) return;
                                                                         if (waitNanos > 0) {
                                                                             long waitMillis = waitNanos / 1000000L;
                                                                             waitNanos -= (waitMillis * 1000000L);
                                                                             synchronized (ConnectionPool.this) {
                                                                                 try {
                                                                                    ConnectionPool.this.wait(waitMillis, (int) waitNanos);//阻塞等待直到下一次清除
                                                                                 } catch (InterruptedException ignored) {
                                                                                 }
                                                                             }
                                                                         }
                                                                     }
                                                                 }
                                                             };
                                                            //get方法
                                                           RealConnection get(Address address, StreamAllocation streamAllocation, Route route) {
                                                               assert (Thread.holdsLock(this));
                                                               for (RealConnection connection : connections) {//遍历Deque，看是否有可使用的RealConnection
                                                                       if (connection.isEligible(address, route)) {
                                                                            streamAllocation.acquire(connection, true);//调用acquire方法
                                                                       return connection;
                                                                    }
                                                               }
                                                            return null;
                                                           }
                                                           ......
                                                           //put方法
                                                           void put(RealConnection connection) {
                                                               assert (Thread.holdsLock(this));
                                                               if (!cleanupRunning) {
                                                                   cleanupRunning = true;
                                                                   executor.execute(cleanupRunnable);//先做异步清理
                                                               }
                                                               connections.add(connection);//再将RealConnection加到Deque中
                                                           }
                                                    }
                                               }
                                              -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                               ->ConnectInterceptor的intercept方法
                                                     @Override
                                                     public Response intercept(Chain chain) throws IOException {
                                                         RealInterceptorChain realChain = (RealInterceptorChain) chain;//链环
                                                         HttpCodec httpCodec = realChain.httpStream();//编码和解码流
                                                         StreamAllocation streamAllocation = realChain.streamAllocation();//分配流资源
                                                         RealConnection connection = (RealConnection) realChain.connection();//连接的实际实现类
                                                         Request request = realChain.request();//请求体
                                                         long sentRequestMillis = System.currentTimeMillis();//当前请求时间
                                                         realChain.eventListener().requestHeadersStart(realChain.call());//请求监听
                                                         httpCodec.writeRequestHeaders(request);//向socket发送请求headers信息

                                                        Response.Builder responseBuilder = null;
                                                        if (HttpMethod.permitsRequestBody(request.method()) && request.body() != null) {//请求body不为null，可以执行请求body的发送
                                                             if ("100-continue".equalsIgnoreCase(request.header("Expect"))) {//如果Expect等于100-continue说明可以继续发送请求，属于特殊情况处理
                                                                 httpCodec.flushRequest();
                                                                 realChain.eventListener().responseHeadersStart(realChain.call());
                                                                 responseBuilder = httpCodec.readResponseHeaders(true);
                                                             }

                                                             if (responseBuilder == null) {
                                                             realChain.eventListener().requestBodyStart(realChain.call());
                                                             long contentLength = request.body().contentLength();//请求body长度
                                                             CountingSink requestBodyOut = new CountingSink(httpCodec.createRequestBody(request, contentLength));
                                                             BufferedSink bufferedRequestBody = Okio.buffer(requestBodyOut);//请求body信息
                                                             request.body().writeTo(bufferedRequestBody);//向socket发送请求body信息
                                                             bufferedRequestBody.close();//发送完毕关闭资源
                                                            .....
                                                        }
                                                         httpCodec.finishRequest();//整个网络请求执行完成
                                                         if (responseBuilder == null) {
                                                             realChain.eventListener().responseHeadersStart(realChain.call());
                                                             responseBuilder = httpCodec.readResponseHeaders(false);//开始读取服务器响应信息
                                                         }
                                                         Response response = responseBuilder
                                                                             .request(request)
                                                                             .handshake(streamAllocation.connection().handshake())
                                                                             .sentRequestAtMillis(sentRequestMillis)
                                                                             .receivedResponseAtMillis(System.currentTimeMillis())
                                                                             .build();//构建接收的Response
                                                         ......
                                                         response = response.newBuilder()
                                                                             .body(httpCodec.openResponseBody(response))
                                                                             .build();//解码封装成客户端需要的Response

                                                         if ("close".equalsIgnoreCase(response.request().header("Connection"))|| "close".equalsIgnoreCase(response.header("Connection"))) {
                                                             streamAllocation.noNewStreams();//关闭流等结束工作
                                                         }
                                                         ......
                                                        return response;//将Response一层层向上返回，即CallServerInterceptor()->ConnectInterceptor()->CacheInterceptor()->BridgeInterceptor()->RetryAndFollowUpInterceptor()->用户自定义拦截器
                                                     }

                                              -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                            if (retryAndFollowUpInterceptor.isCanceled()) { //重定向和重试取消，则调用失败回调
                                signalledCallback = true;
                                responseCallback.onFailure(RealCall.this, new IOException("Canceled"));//失败回调
                            } else {
                                signalledCallback = true;
                                responseCallback.onResponse(RealCall.this, response);//成功回调
                            }
                        } catch (IOException e) {
                            e = timeoutExit(e);
                            if (signalledCallback) {
                                // Do not signal the callback twice!
                                Platform.get().log(INFO, "Callback failure for " + toLoggableString(), e);
                            } else {
                                eventListener.callFailed(RealCall.this, e);
                                responseCallback.onFailure(RealCall.this, e);//失败回调
                            }
                        } finally {
                            client.dispatcher().finished(this);//无论失败成功都要回收
                        }
                    }
            }
    }

 */

}
