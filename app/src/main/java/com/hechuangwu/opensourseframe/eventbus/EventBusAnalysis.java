package com.hechuangwu.opensourseframe.eventbus;

/**
 * Created by cwh on 2019/9/16 0016.
 * 功能:
 */
public class EventBusAnalysis {
    /**
     *

    //获取EventBus
    EventBus.getDefault()
     //注册
    EventBus.getDefault().register( this );
    //发送
    EventBus.getDefault().post( new MessageEvent( "哈哈" ) );
    //事件总线类
    public class EventBus {
        //本线程状态，其他线程不能同享
        private final ThreadLocal<PostingThreadState> currentPostingThreadState = new ThreadLocal<PostingThreadState>() {
            @Override
            protected PostingThreadState initialValue() {
                return new PostingThreadState();
            }
        };

        private static final Map<Class<?>, List<Class<?>>> eventTypesCache = new HashMap<>();
        final static class PostingThreadState {
            final List<Object> eventQueue = new ArrayList<>();//列表
            //一些标志位
            boolean isPosting;//防止多次调用
            boolean isMainThread;//是否主线程
            Subscription subscription;
            Object event;
            boolean canceled;
        }

        //单例模式，但构造方法不是私有，说明事件总线有多条
        public EventBus() {
            this( DEFAULT_BUILDER );
        }
        public static EventBus getDefault() {
            if (defaultInstance == null) {
                synchronized (EventBus.class) {
                    if (defaultInstance == null) {
                        defaultInstance = new EventBus();
                    }
                }
            }
            return defaultInstance;
        }
        EventBus(EventBusBuilder builder) {
            logger = builder.getLogger();//日志类
            subscriptionsByEventType = new HashMap<>();//事件存储
            typesBySubscriber = new HashMap<>();
            stickyEvents = new ConcurrentHashMap<>();//粘性事件
            mainThreadSupport = builder.getMainThreadSupport();//主线程接口
            mainThreadPoster = mainThreadSupport != null ? mainThreadSupport.createPoster( this ) : null;//获取接口实现类，创建HandlerPoster，对应ThreadMode.MAIN看下面HandlerPoster分析
            backgroundPoster = new BackgroundPoster( this );//后台执行处理，实际为一个Runnable，对应ThreadMode.BACKGROUND,看下面分析
            asyncPoster = new AsyncPoster( this );//同样是一个Runnable，对应ThreadMode.ASYNC，看下面分析
            indexCount = builder.subscriberInfoIndexes != null ? builder.subscriberInfoIndexes.size() : 0;
            subscriberMethodFinder = new SubscriberMethodFinder( builder.subscriberInfoIndexes,builder.strictMethodVerification, builder.ignoreGeneratedIndex );//注解的寻找器
            //是否开启某个标志位
            logSubscriberExceptions = builder.logSubscriberExceptions;
            logNoSubscriberMessages = builder.logNoSubscriberMessages;
            sendSubscriberExceptionEvent = builder.sendSubscriberExceptionEvent;
            sendNoSubscriberEvent = builder.sendNoSubscriberEvent;
            throwSubscriberException = builder.throwSubscriberException;
            eventInheritance = builder.eventInheritance;
            executorService = builder.executorService;
        }

        //注册方法
        public void register(Object subscriber) {
            Class<?> subscriberClass = subscriber.getClass();//获取传入的订阅者
            List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);//找到所有订阅方法
            ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
            class SubscriberMethodFinder {
                private static final Map<Class<?>, List<SubscriberMethod>> METHOD_CACHE = new ConcurrentHashMap<>();//SubscriberMethod缓存
                private static final SubscriberMethodFinder.FindState[] FIND_STATE_POOL = new SubscriberMethodFinder.FindState[POOL_SIZE];//FindState数组

                List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
                    List<SubscriberMethod> subscriberMethods = METHOD_CACHE.get( subscriberClass );//查找缓存是否有，第一次为null
                    if (subscriberMethods != null) {
                        return subscriberMethods;
                    }
                    if (ignoreGeneratedIndex) {
                        subscriberMethods = findUsingReflection( subscriberClass );
                    }
                    //默认false
                    else {
                        subscriberMethods = findUsingInfo( subscriberClass );//生成订阅方法
                    }
                    if (subscriberMethods.isEmpty()) {
                        throw new EventBusException( "Subscriber " + subscriberClass + " and its super classes have no public methods with the @Subscribe annotation" );
                    } else {
                        METHOD_CACHE.put( subscriberClass, subscriberMethods );
                        return subscriberMethods;
                    }
                }

                //生成订阅方法
                private List<SubscriberMethod> findUsingInfo(Class<?> subscriberClass) {
                    SubscriberMethodFinder.FindState findState = prepareFindState();//获取FindState对象
                    findState.initForSubscriber(subscriberClass);//初始化
                    while (findState.clazz != null) {
                        findState.subscriberInfo = getSubscriberInfo(findState);
                        if (findState.subscriberInfo != null) {
                            SubscriberMethod[] array = findState.subscriberInfo.getSubscriberMethods();
                            for (SubscriberMethod subscriberMethod : array) {
                                if (findState.checkAdd(subscriberMethod.method, subscriberMethod.eventType)) {
                                    findState.subscriberMethods.add(subscriberMethod);
                                }
                            }
                        } else {
                            findUsingReflectionInSingleClass(findState);//通过反射解析所有订阅方法
                        }
                        findState.moveToSuperclass();
                    }
                    return getMethodsAndRelease(findState);
                }

                //获取FindState
                private SubscriberMethodFinder.FindState prepareFindState() {
                    synchronized (FIND_STATE_POOL) {
                        for (int i = 0; i < POOL_SIZE; i++) {
                            SubscriberMethodFinder.FindState state = FIND_STATE_POOL[i];//从池中取
                            if (state != null) {
                                FIND_STATE_POOL[i] = null;
                                return state;
                            }
                        }
                    }
                    return new SubscriberMethodFinder.FindState();//池中没有，新建
                }


                private SubscriberInfo getSubscriberInfo(org.greenrobot.eventbus.SubscriberMethodFinder.FindState findState) {
                    if (findState.subscriberInfo != null && findState.subscriberInfo.getSuperSubscriberInfo() != null) {
                        SubscriberInfo superclassInfo = findState.subscriberInfo.getSuperSubscriberInfo();
                        if (findState.clazz == superclassInfo.getSubscriberClass()) {
                            return superclassInfo;
                        }
                    }
                    if (subscriberInfoIndexes != null) {
                        for (SubscriberInfoIndex index : subscriberInfoIndexes) {
                            SubscriberInfo info = index.getSubscriberInfo(findState.clazz);
                            if (info != null) {
                                return info;
                            }
                        }
                    }
                    return null;
                }

                //反射解析订阅方法
                private void findUsingReflectionInSingleClass(org.greenrobot.eventbus.SubscriberMethodFinder.FindState findState) {
                    Method[] methods;
                    try {
                        methods = findState.clazz.getDeclaredMethods();
                    } catch (Throwable th) {
                        methods = findState.clazz.getMethods();
                        findState.skipSuperClasses = true;
                    }
                    for (Method method : methods) {//遍历所有方法
                        int modifiers = method.getModifiers();
                        if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIERS_IGNORE) == 0) {
                            Class<?>[] parameterTypes = method.getParameterTypes();
                            if (parameterTypes.length == 1) {
                                Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);//获取注解为Subscribe的方法
                                if (subscribeAnnotation != null) {
                                    Class<?> eventType = parameterTypes[0];
                                    if (findState.checkAdd(method, eventType)) {//检查ThreadMode
                                        ThreadMode threadMode = subscribeAnnotation.threadMode();
                                        //将方法名，threadMode，优先级，是否为sticky方法封装为SubscriberMethod对象，添加到subscriberMethods列表中
                                        findState.subscriberMethods.add(new SubscriberMethod(method, eventType, threadMode,subscribeAnnotation.priority(), subscribeAnnotation.sticky()));
                                    }
                                }
                            } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
                                String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                                throw new EventBusException("@Subscribe method " + methodName +"must have exactly 1 parameter but has " + parameterTypes.length);
                            }
                        } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
                            String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                            throw new EventBusException(methodName +" is a illegal @Subscribe method: must be public, non-static, and non-abstract");
                        }
                    }
                }

                //回收释放并缓存
                private List<SubscriberMethod> getMethodsAndRelease(SubscriberMethodFinder.FindState findState) {
                    List<SubscriberMethod> subscriberMethods = new ArrayList<>(findState.subscriberMethods);
                    findState.recycle();
                    synchronized (FIND_STATE_POOL) {
                        for (int i = 0; i < POOL_SIZE; i++) {
                            if (FIND_STATE_POOL[i] == null) {
                                FIND_STATE_POOL[i] = findState;//缓存findState
                                break;
                            }
                        }
                    }
                    return subscriberMethods;//返回subscriberMethods
                }
            }
            ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
            synchronized (this) {
                for (SubscriberMethod subscriberMethod : subscriberMethods) {
                    subscribe(subscriber, subscriberMethod);//获取到subscriberMethods列表后，调用订阅方法
                    ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                    private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
                        Class<?> eventType = subscriberMethod.eventType;//即MessageEvent
                        Subscription newSubscription = new Subscription(subscriber, subscriberMethod);
                        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);//查找一个CopyOnWriteArrayList<Subscription> ，如果没有则创建
                        if (subscriptions == null) {
                            subscriptions = new CopyOnWriteArrayList<>();
                            subscriptionsByEventType.put(eventType, subscriptions);//subscriptions列表放入map中
                        } else {
                            if (subscriptions.contains(newSubscription)) {
                                throw new EventBusException("Subscriber " + subscriber.getClass() + " already registered to event "+ eventType);
                            }
                        }
                        int size = subscriptions.size();
                        for (int i = 0; i <= size; i++) {
                            if (i == size || subscriberMethod.priority > subscriptions.get(i).subscriberMethod.priority) {//Subscription根据优先级添加，越高越先添加
                                subscriptions.add(i, newSubscription);
                                break;
                            }
                        }
                        List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);//eventType列表
                        if (subscribedEvents == null) {
                            subscribedEvents = new ArrayList<>();
                            typesBySubscriber.put(subscriber, subscribedEvents);//订阅者为key，列表为value存入
                        }
                        subscribedEvents.add(eventType);
                        if (subscriberMethod.sticky) {//是否粘性
                            if (eventInheritance) {
                                Set<Map.Entry<Class<?>, Object>> entries = stickyEvents.entrySet();
                                for (Map.Entry<Class<?>, Object> entry : entries) {
                                    Class<?> candidateEventType = entry.getKey();
                                    if (eventType.isAssignableFrom(candidateEventType)) {
                                        Object stickyEvent = entry.getValue();
                                        checkPostStickyEventToSubscription(newSubscription, stickyEvent);//粘性将执行checkPostStickyEventToSubscription
                                    }
                                }
                            } else {
                                Object stickyEvent = stickyEvents.get(eventType);
                                checkPostStickyEventToSubscription(newSubscription, stickyEvent);//粘性将执行checkPostStickyEventToSubscription
                            }
                        }
                        ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                        private void checkPostStickyEventToSubscription(Subscription newSubscription, Object stickyEvent) {
                            if (stickyEvent != null) {
                                postToSubscription(newSubscription, stickyEvent, isMainThread());//根据不同线程调用不同的方法
                                ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                    private void postToSubscription(Subscription  subscription, Object event, boolean isMainThread) {
                                        switch (subscription.subscriberMethod.threadMode) {
                                            case POSTING:
                                                invokeSubscriber(subscription, event);//执行invokeSubscriber()方法，就是直接反射调用；
                                                break;
                                            case MAIN:
                                                if (isMainThread) {//判断当前是否在UI线程
                                                    invokeSubscriber(subscription, event);//直接反射调用
                                                } else {
                                                    mainThreadPoster.enqueue(subscription, event);//把当前的方法加入到队列之中，然后通过handler去发送一个消息，在handler的handleMessage中去执行方法。具体逻辑在HandlerPoster.java中
                                                }
                                                break;
                                            case MAIN_ORDERED://与上面逻辑类似，顺序执行方法
                                                if (mainThreadPoster != null) {
                                                    mainThreadPoster.enqueue(subscription, event);
                                                } else {
                                                    invokeSubscriber(subscription, event);
                                                }
                                                break;
                                            case BACKGROUND:
                                                if (isMainThread) {
                                                    backgroundPoster.enqueue(subscription, event);//具体逻辑在BackgroundPoster
                                                } else {
                                                    invokeSubscriber(subscription, event);//直接反射调用
                                                }
                                                break;
                                            case ASYNC:
                                                asyncPoster.enqueue(subscription, event);//具体逻辑在AsyncPoster
                                                break;
                                            default:
                                                throw new IllegalStateException("Unknown thread mode: " + subscription.subscriberMethod.threadMode);
                                        }
                                    }
                                ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                            }
                        }
                        ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                    }
                    ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                }
            }
        }

        //发送方法
        public void post(Object event) {
            PostingThreadState postingState = currentPostingThreadState.get();
            List<Object> eventQueue = postingState.eventQueue;//获取列表
            eventQueue.add(event);//添加事件到列表中
            if (!postingState.isPosting) {
                postingState.isMainThread = isMainThread();//是否主线程
                postingState.isPosting = true;
                if (postingState.canceled) {
                    throw new EventBusException("Internal error. Abort state was not reset");
                }
                try {
                    while (!eventQueue.isEmpty()) {//事件不为空，则发送
                        postSingleEvent(eventQueue.remove(0), postingState);
                    }
                } finally {
                    postingState.isPosting = false;
                    postingState.isMainThread = false;
                }
            }
        }
        //发送消息
        private void postSingleEvent(Object event, PostingThreadState postingState) throws Error {
            Class<?> eventClass = event.getClass();//取出事件的类字节码
            boolean subscriptionFound = false;
            if (eventInheritance) {//默认为true
                List<Class<?>> eventTypes = lookupAllEventTypes(eventClass);//取出event列表
                int countTypes = eventTypes.size();
                for (int h = 0; h < countTypes; h++) {
                    Class<?> clazz = eventTypes.get(h);
                    subscriptionFound |= postSingleEventForEventType(event, postingState, clazz);
                }
            } else {
                subscriptionFound = postSingleEventForEventType(event, postingState, eventClass);
            }
            if (!subscriptionFound) {
                if (logNoSubscriberMessages) {
                    logger.log( Level.FINE, "No subscribers registered for event " + eventClass);
                }
                if (sendNoSubscriberEvent && eventClass != NoSubscriberEvent.class &&
                        eventClass != SubscriberExceptionEvent.class) {
                    post(new NoSubscriberEvent(this, event));
                }
            }
        }
        //取出Event及其父类和接口的class列表
        private static List<Class<?>> lookupAllEventTypes(Class<?> eventClass) {
            synchronized (eventTypesCache) {
                List<Class<?>> eventTypes = eventTypesCache.get(eventClass);//根据类字节码，从缓存map取事件类型
                if (eventTypes == null) {//没有缓存事件
                    eventTypes = new ArrayList<>();//初始化
                    Class<?> clazz = eventClass;
                    while (clazz != null) {
                        eventTypes.add(clazz);//把类添加入缓存
                        addInterfaces(eventTypes, clazz.getInterfaces());
                        clazz = clazz.getSuperclass();
                    }
                    eventTypesCache.put(eventClass, eventTypes);//将事件缓存
                }
                return eventTypes;
            }
        }

        //根据Event类型从subscriptionsByEventType中取出对应的subscriptions
        private boolean postSingleEventForEventType(Object event, PostingThreadState postingState, Class<?> eventClass) {
            CopyOnWriteArrayList<Subscription> subscriptions;
            synchronized (this) {
                subscriptions = subscriptionsByEventType.get(eventClass);
            }
            if (subscriptions != null && !subscriptions.isEmpty()) {
                for (Subscription subscription : subscriptions) {
                    postingState.event = event;
                    postingState.subscription = subscription;
                    boolean aborted = false;
                    try {
                        postToSubscription(subscription, event, postingState.isMainThread);//根据不同线程分发，看上面postToSubscription方法
                        aborted = postingState.canceled;
                    } finally {
                        postingState.event = null;
                        postingState.subscription = null;
                        postingState.canceled = false;
                    }
                    if (aborted) {
                        break;
                    }
                }
                return true;
            }
            return false;
        }
    }
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    public class HandlerPoster extends Handler implements Poster {
        private final PendingPostQueue queue;//即将执行的消息队列，内部通过List封装成一个链表模式，android的Message也是此种模式
        private final int maxMillisInsideHandleMessage;//存在于HandlerMessage最大时间值
        private final EventBus eventBus;
        private boolean handlerActive;//Handler是否已运行
        //消息处理方法
        @Override
        public void handleMessage(Message msg) {
            boolean rescheduled = false;
            try {
                long started = SystemClock.uptimeMillis();
                while (true) {
                    PendingPost pendingPost = queue.poll();//遍历从队列取事件
                    if (pendingPost == null) {
                        synchronized (this) {
                            pendingPost = queue.poll();
                            if (pendingPost == null) {
                                handlerActive = false;
                                return;
                            }
                        }
                    }
                    eventBus.invokeSubscriber(pendingPost);//将消息分发出去
                    long timeInMethod = SystemClock.uptimeMillis() - started;
                    if (timeInMethod >= maxMillisInsideHandleMessage) {
                        if (!sendMessage(obtainMessage())) {
                            throw new EventBusException("Could not send handler message");
                        }
                        rescheduled = true;//执行发送，说明正在运行
                        return;
                    }
                }
            } finally {
                handlerActive = rescheduled;
            }
        }
    }
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    final class BackgroundPoster implements Runnable, Poster {
        private final PendingPostQueue queue;
        private final EventBus eventBus;
        private volatile boolean executorRunning;
        BackgroundPoster(EventBus eventBus) {
            this.eventBus = eventBus;
            queue = new PendingPostQueue();
        }

        public void enqueue(Subscription subscription, Object event) {
            PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
            synchronized (this) {
                queue.enqueue(pendingPost);
                if (!executorRunning) {
                    executorRunning = true;
                    eventBus.getExecutorService().execute(this);
                }
            }
        }
        //同样从队列中取消息进行分发
        @Override
        public void run() {
            try {
                try {
                    while (true) {
                        PendingPost pendingPost = queue.poll(1000);
                        if (pendingPost == null) {
                            synchronized (this) {
                                pendingPost = queue.poll();
                                if (pendingPost == null) {
                                    executorRunning = false;
                                    return;
                                }
                            }
                        }
                        eventBus.invokeSubscriber(pendingPost);//消息分发
                    }
                } catch (InterruptedException e) {
                    eventBus.getLogger().log( Level.WARNING, Thread.currentThread().getName() + " was interruppted", e);
                }
            } finally {
                executorRunning = false;
            }
        }
    }
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    class AsyncPoster implements Runnable, Poster {
        private final PendingPostQueue queue;
        private final org.greenrobot.eventbus.EventBus eventBus;
        AsyncPoster(EventBus eventBus) {
            this.eventBus = eventBus;
            queue = new PendingPostQueue();
        }
        public void enqueue(Subscription subscription, Object event) {
            PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
            queue.enqueue(pendingPost);
            eventBus.getExecutorService().execute(this);
        }
        @Override
        public void run() {
            PendingPost pendingPost = queue.poll();//只获取了队列中第一个进行分发
            if(pendingPost == null) {
                throw new IllegalStateException("No pending post available");
            }
            eventBus.invokeSubscriber(pendingPost);
        }
    }
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------



     */
}
