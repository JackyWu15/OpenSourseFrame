package com.hechuangwu.opensourseframe.leakcanary;

/**
 * Created by cwh on 2019/9/12 0012.
 * 功能:LeakCanary源码分析
 */
public class LeakCanaryAnalysis {
    /**
     *
    LeakCanary.install( this );
    ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
        public static RefWatcher install(@NonNull Application application) {//返回一个RefWatcher
            return refWatcher( application ).listenerServiceClass( DisplayLeakService.class ).excludedRefs( AndroidExcludedRefs.createAppDefaults().build() ).buildAndInstall();
            ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
            public final class AndroidRefWatcherBuilder extends RefWatcherBuilder<AndroidRefWatcherBuilder> {
                public RefWatcher buildAndInstall() {
                    if (LeakCanaryInternals.installedRefWatcher != null) {
                        throw new UnsupportedOperationException( "buildAndInstall() should only be called once." );
                    }
                    RefWatcher refWatcher = build();//构建RefWatcher
                    if (refWatcher != DISABLED) {
                        LeakCanaryInternals.setEnabledAsync( context, DisplayLeakActivity.class, true );//开启Activity监听
                        if (watchActivities) {//监听activity生命周期，默认为true
                            ActivityRefWatcher.install( context, refWatcher );//静态构造
                            ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                public final class ActivityRefWatcher {
                                    private final Application application;
                                    private final RefWatcher refWatcher;
                                    public static void installOnIcsPlus(@NonNull Application application, @NonNull RefWatcher refWatcher) {
                                        install( application, refWatcher );
                                    }
                                    public static void install(@NonNull Context context, @NonNull RefWatcher refWatcher) {//静态构造
                                        Application application = (Application) context.getApplicationContext();
                                        ActivityRefWatcher activityRefWatcher = new ActivityRefWatcher( application, refWatcher );//ActivityRefWatcher持有RefWatcher和Application
                                        application.registerActivityLifecycleCallbacks( activityRefWatcher.lifecycleCallbacks );
                                    }
                                    private final Application.ActivityLifecycleCallbacks lifecycleCallbacks = new ActivityLifecycleCallbacksAdapter() {//注册Activity生命周期回调
                                        @Override
                                        public void onActivityDestroyed(Activity activity) {
                                            refWatcher.watch( activity );//在销毁时，调用RefWatcher的watch，看下面RefWatcher分析
                                        }
                                    };

                                }
                            ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                        }
                        if (watchFragments) {
                            FragmentRefWatcher.Helper.install( context, refWatcher );
                        }
                    }
                    LeakCanaryInternals.installedRefWatcher = refWatcher;
                    return refWatcher;
                }
            ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
            }
        }

    public final class RefWatcher {
        public static final RefWatcher DISABLED = new RefWatcherBuilder<>().build();
        private final WatchExecutor watchExecutor;//用于执行检测内存泄漏
        private final DebuggerControl debuggerControl;//查询是否在调试，如果在调试就不检测内存泄漏
        private final GcTrigger gcTrigger;//是否最后一次gc
        private final HeapDumper heapDumper;//保存内存泄漏的堆
        private final HeapDump.Listener heapdumpListener;
        private final HeapDump.Builder heapDumpBuilder;
        private final Set<String> retainedKeys;//保存待检测和已经内存泄漏的key
        private final ReferenceQueue<Object> queue;//是否弱应用对象已经被gc回收
        public void watch(Object watchedReference) {
            watch( watchedReference, "" );//实际调用watch(Object watchedReference, String referenceName)
        }

        public void watch(Object watchedReference, String referenceName) {
            if (this == DISABLED) {
                return;
            }
            checkNotNull( watchedReference, "watchedReference" );
            checkNotNull( referenceName, "referenceName" );
            final long watchStartNanoTime = System.nanoTime();
            String key = UUID.randomUUID().toString();
            retainedKeys.add( key );//对引用添加一个key
            final KeyedWeakReference reference = new KeyedWeakReference( watchedReference, key, referenceName, queue );//构造一个弱应用
            ensureGoneAsync( watchStartNanoTime, reference );//开始分析这个引用
            ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                private void ensureGoneAsync(final long watchStartNanoTime, final KeyedWeakReference reference) {
                    watchExecutor.execute(new Retryable() {//开启线程执行
                        @Override public Retryable.Result run() {
                            return ensureGone(reference, watchStartNanoTime);//实际就是确保activity是否已经被回收
                            ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                    Retryable.Result ensureGone(final KeyedWeakReference reference, final long watchStartNanoTime) {
                                        long gcStartNanoTime = System.nanoTime();
                                        long watchDurationMs = NANOSECONDS.toMillis(gcStartNanoTime - watchStartNanoTime);//从调用watch到gc回收之间的时间
                                        removeWeaklyReachableReferences();//移除已经被回收的引用，剩下的就是需要回收的
                                        if (debuggerControl.isDebuggerAttached()) {//是否debug
                                            return RETRY;
                                        }
                                        if (gone(reference)) {//当前对象是可达的，说明不需要回收
                                            return DONE;
                                        }
                                        gcTrigger.runGc();//再次手动回收
                                        removeWeaklyReachableReferences();//移除已经被回收的引用
                                        if (!gone(reference)) {//不可达，说明已经泄漏了
                                            long startDumpHeap = System.nanoTime();
                                            long gcDurationMs = NANOSECONDS.toMillis(startDumpHeap - gcStartNanoTime);
                                            File heapDumpFile = heapDumper.dumpHeap();//将泄漏信息dump成文件
                                            if (heapDumpFile == RETRY_LATER) {
                                                return RETRY;
                                            }
                                            long heapDumpDurationMs = NANOSECONDS.toMillis(System.nanoTime() - startDumpHeap);
                                            HeapDump heapDump = heapDumpBuilder.heapDumpFile(heapDumpFile).referenceKey(reference.key)
                                                    .referenceName(reference.name)
                                                    .watchDurationMs(watchDurationMs)
                                                    .gcDurationMs(gcDurationMs)
                                                    .heapDumpDurationMs(heapDumpDurationMs)
                                                    .build();
                                            heapdumpListener.analyze(heapDump);//开始分析内存泄漏
                                            ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                                public final class ServiceHeapDumpListener implements HeapDump.Listener {//实际实现类为ServiceHeapDumpListener
                                                    private final Context context;
                                                    private final Class<? extends AbstractAnalysisResultService> listenerServiceClass;
                                                    public ServiceHeapDumpListener(final Context context,final Class<? extends AbstractAnalysisResultService> listenerServiceClass) {
                                                        this.listenerServiceClass = checkNotNull(listenerServiceClass, "listenerServiceClass");
                                                        this.context = checkNotNull(context, "context").getApplicationContext();
                                                    }
                                                    @Override
                                                    public void analyze(@NonNull HeapDump heapDump) {//分析方法
                                                        checkNotNull(heapDump, "heapDump");
                                                        HeapAnalyzerService.runAnalysis(context, heapDump, listenerServiceClass);//调用HeapAnalyzerService.runAnalysis，进入HeapAnalyzerService服务做分析
                                                        ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                                            public final class HeapAnalyzerService extends ForegroundService implements AnalyzerProgressListener {
                                                                private static final String LISTENER_CLASS_EXTRA = "listener_class_extra";
                                                                private static final String HEAPDUMP_EXTRA = "heapdump_extra";
                                                                public static void runAnalysis(Context context, HeapDump heapDump,Class<? extends AbstractAnalysisResultService> listenerServiceClass) {
                                                                    setEnabledBlocking(context, com.squareup.leakcanary.internal.HeapAnalyzerService.class, true);
                                                                    setEnabledBlocking(context, listenerServiceClass, true);
                                                                    Intent intent = new Intent(context, com.squareup.leakcanary.internal.HeapAnalyzerService.class);
                                                                    intent.putExtra(LISTENER_CLASS_EXTRA, listenerServiceClass.getName());
                                                                    intent.putExtra(HEAPDUMP_EXTRA, heapDump);
                                                                    ContextCompat.startForegroundService(context, intent);//开启服务
                                                                }
                                                                @Override
                                                                protected void onHandleIntentInForeground(@Nullable Intent intent) {
                                                                    if (intent == null) {
                                                                        CanaryLog.d("HeapAnalyzerService received a null intent, ignoring.");
                                                                        return;
                                                                    }
                                                                    String listenerClassName = intent.getStringExtra(LISTENER_CLASS_EXTRA);
                                                                    HeapDump heapDump = (HeapDump) intent.getSerializableExtra(HEAPDUMP_EXTRA);//堆文件
                                                                    HeapAnalyzer heapAnalyzer = new HeapAnalyzer(heapDump.excludedRefs, this, heapDump.reachabilityInspectorClasses);//构建堆内存分析器
                                                                    AnalysisResult result = heapAnalyzer.checkForLeak(heapDump.heapDumpFile, heapDump.referenceKey, heapDump.computeRetainedHeapSize);//开始分析
                                                                    ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                                                        public @NonNull AnalysisResult checkForLeak(@NonNull File heapDumpFile,@NonNull String referenceKey, boolean computeRetainedSize) {
                                                                            long analysisStartNanoTime = System.nanoTime();
                                                                            if (!heapDumpFile.exists()) {
                                                                                Exception exception = new IllegalArgumentException("File does not exist: " + heapDumpFile);
                                                                                return failure(exception, since(analysisStartNanoTime));
                                                                            }
                                                                            try {
                                                                                listener.onProgressUpdate(READING_HEAP_DUMP_FILE);
                                                                                HprofBuffer buffer = new MemoryMappedFileBuffer(heapDumpFile);//加堆文件加载到Hprof缓存中
                                                                                HprofParser parser = new HprofParser(buffer);//构造Hprof解析器
                                                                                listener.onProgressUpdate(PARSING_HEAP_DUMP);
                                                                                Snapshot snapshot = parser.parse();//泄漏的快照
                                                                                listener.onProgressUpdate(DEDUPLICATING_GC_ROOTS);
                                                                                deduplicateGcRoots(snapshot);
                                                                                listener.onProgressUpdate(FINDING_LEAKING_REF);
                                                                                Instance leakingRef = findLeakingReference(referenceKey, snapshot);//从快照中查找泄漏的引用
                                                                                ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                                                                        private Instance findLeakingReference(String key, Snapshot snapshot) {
                                                                                            ClassObj refClass = snapshot.findClass(KeyedWeakReference.class.getName());//获取对象的弱引用
                                                                                            if (refClass == null) {
                                                                                                throw new IllegalStateException( "Could not find the " + KeyedWeakReference.class.getName() + " class in the heap dump.");
                                                                                            }
                                                                                            List<String> keysFound = new ArrayList<>();//遍历引用的key
                                                                                            for (Instance instance : refClass.getInstancesList()) {
                                                                                                List<ClassInstance.FieldValue> values = classInstanceValues(instance);
                                                                                                Object keyFieldValue = fieldValue(values, "key");
                                                                                                if (keyFieldValue == null) {
                                                                                                    keysFound.add(null);
                                                                                                    continue;
                                                                                                }
                                                                                                String keyCandidate = asString(keyFieldValue);
                                                                                                if (keyCandidate.equals(key)) {//如果找到这个对象，那么返回这个泄漏对象
                                                                                                    return fieldValue(values, "referent");
                                                                                                }
                                                                                                keysFound.add(keyCandidate);
                                                                                            }
                                                                                            throw new IllegalStateException("Could not find weak reference with key " + key + " in " + keysFound);
                                                                                        }
                                                                                ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                                                                if (leakingRef == null) {//引用为空说明已回收
                                                                                    return noLeak(since(analysisStartNanoTime));
                                                                                }
                                                                                return findLeakTrace(analysisStartNanoTime, snapshot, leakingRef, computeRetainedSize);//不为空则查到泄漏最短的路径
                                                                                ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                                                                        private AnalysisResult findLeakTrace(long analysisStartNanoTime, Snapshot snapshot,Instance leakingRef, boolean computeRetainedSize) {
                                                                                            listener.onProgressUpdate(FINDING_SHORTEST_PATH);
                                                                                            ShortestPathFinder pathFinder = new ShortestPathFinder(excludedRefs);
                                                                                            ShortestPathFinder.Result result = pathFinder.findPath(snapshot, leakingRef);
                                                                                            if (result.leakingNode == null) {
                                                                                                return noLeak(since(analysisStartNanoTime));
                                                                                            }
                                                                                            listener.onProgressUpdate(BUILDING_LEAK_TRACE);
                                                                                            LeakTrace leakTrace = buildLeakTrace(result.leakingNode);//泄漏调用栈，即保存泄漏信息的对象
                                                                                            String className = leakingRef.getClassObj().getClassName();//类名
                                                                                            long retainedSize;//计算泄漏大小
                                                                                            if (computeRetainedSize) {
                                                                                                listener.onProgressUpdate(COMPUTING_DOMINATORS);
                                                                                                snapshot.computeDominators();
                                                                                                Instance leakingInstance = result.leakingNode.instance;
                                                                                                retainedSize = leakingInstance.getTotalRetainedSize();//计算出泄漏大小
                                                                                                if (SDK_INT <= N_MR1) {
                                                                                                    listener.onProgressUpdate(COMPUTING_BITMAP_SIZE);
                                                                                                    retainedSize += computeIgnoredBitmapRetainedSize(snapshot, leakingInstance);
                                                                                                }
                                                                                            } else {
                                                                                                retainedSize = AnalysisResult.RETAINED_HEAP_SKIPPED;
                                                                                            }

                                                                                            return leakDetected(result.excludingKnownLeaks, className, leakTrace, retainedSize,since(analysisStartNanoTime));
                                                                                        }
                                                                                ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                                                            } catch (Throwable e) {
                                                                                return failure(e, since(analysisStartNanoTime));
                                                                            }
                                                                        }
                                                                    ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                                                    AbstractAnalysisResultService.sendResultToListener(this, listenerClassName, heapDump, result);
                                                                }
                                                             }
                                                        ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                                    }
                                                }
                                            ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                        }
                                        return DONE;
                                    }
                            ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                        }
                    });
                }
            ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
        }
    }

     */
}
