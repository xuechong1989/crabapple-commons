package xc.crabapple.core.cache;

import xc.crabapple.core.cipher.sm3.Sm3Util;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.tomcat.TomcatUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 2026-08-20 构建替代redis的本地API持久化缓存框架
 * 本地持久化缓存工具类：仿照LocalCacheUtil使用风格（静态方法、模块分组、秒级TTL），
 * 内存ConcurrentHashMap全量承载 + 单文件append-only日志写穿持久化，JVM重启后自动恢复；
 * 纯进程内Java API调用，无HTTP、无TCP、无Redis服务器；存储层异常一律降级不阻断业务
 */
public final class LocalCacheUtil {

    public static final String DEFAULT_MODULE_NAME = "crabapple_default";    // 默认模块名（与LocalCacheUtil一致）
    public static final long FLUSH_MODE_SYNC = -1L;                         // 刷盘档位：每写force
    public static final long FLUSH_MODE_ASYNC = 0L;                         // 刷盘档位：仅close与Compaction时force
    private static final long DEFAULT_FLUSH_INTERVAL_MILLIS = 1000L;        // 默认PERIODIC周期force间隔
    private static final long DEFAULT_FLUSH_BUFFER_MILLIS = 1000L;          // 默认写缓冲周期刷出间隔（合并write系统调用）
    private static final int DEFAULT_CLEAR_COUNT_MAX = 4096;                // 默认每模块每次TTL清理上限（与LocalCacheUtil默认值一致）
    private static final long DEFAULT_CLEAR_INTERVAL_MILLIS = 60000L;       // 默认TTL后台清理周期60秒
    private static final long DEFAULT_MAX_FILE_SIZE = 64L * 1024L * 1024L;  // 默认Compaction触发阈值64MB
    private static final int MAX_MODULE_NAME_LENGTH = 256;                  // 模块名长度上限（UTF-8字节）

    private static volatile boolean p_inited = false;               // 初始化完成标志（volatile保证初始化结果可见）
    private static volatile String p_dataPath = "";                 // 实际数据目录（含实例哈希层，重复init时返回）
    private static ConcurrentHashMap<String, ConcurrentHashMap<String, LocalCacheData>> p_mapGlobal = null;   // 模块分组内存结构
    private static LocalCacheStore p_store = null;             // 存储引擎
    private static ScheduledThreadPoolExecutor p_executor = null;   // 后台任务线程池（daemon具名）
    private static long p_flushMode = DEFAULT_FLUSH_INTERVAL_MILLIS;   // 刷盘档位：-1SYNC、0ASYNC、正数PERIODIC毫秒
    private static int p_clearCountMax = DEFAULT_CLEAR_COUNT_MAX;      // 每模块每次TTL清理上限（init可配）
    private static long p_clearIntervalMillis = DEFAULT_CLEAR_INTERVAL_MILLIS;   // TTL清理周期毫秒（init可配，参数单位秒）
    private static long p_maxFileSize = DEFAULT_MAX_FILE_SIZE;         // Compaction触发阈值字节（init可配）
    private static long p_flushBufferMillis = DEFAULT_FLUSH_BUFFER_MILLIS;   // 写缓冲周期刷出毫秒（init可配；ASYNC档缓冲最迟落盘周期）
    private static boolean p_hookRegistered = false;                // shutdown hook只注册一次标志

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 默认初始化（签名与LocalCacheUtil.init()一致）：数据目录取{user.dir}\persistent-cache，全默认配置
     */
    public static final String init() {
        final String localDataPath = init(null, null, null, null, null, null);
        return localDataPath;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 清理参数初始化（签名与LocalCacheUtil.init(Integer, Long)一致，clearIntervalSeconds单位秒）
     * @param clearCountMax 每模块每次TTL清理最多清除的对象数量，null或小于等于0取默认4096
     * @param clearIntervalSeconds TTL后台清理周期秒，null或小于等于0取默认60
     */
    public static final String init(final Integer clearCountMax, final Long clearIntervalSeconds) {
        final String localDataPath = init(null, null, clearCountMax, clearIntervalSeconds, null, null);
        return localDataPath;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 指定数据目录初始化（扩展签名）
     * @param dataPath 持久化数据目录
     */
    public static final String init(final String dataPath) {
        final String localDataPath = init(dataPath, null, null, null, null, null);
        return localDataPath;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 数据目录、刷盘档位初始化（扩展签名；moduleNameArray已删除，模块在首次set时懒创建）
     * @param dataPath 持久化数据目录，空时取{user.dir}\persistent-cache
     * @param flushIntervalMillis 刷盘档位：FLUSH_MODE_SYNC每写force、FLUSH_MODE_ASYNC仅close时force、正数为PERIODIC周期毫秒（默认1000）
     */
    public static final String init(final String dataPath, final Long flushIntervalMillis) {
        final String localDataPath = init(dataPath, flushIntervalMillis, null, null, null, null);
        return localDataPath;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 全参扩展初始化（数据目录、刷盘档位、清理上限、清理周期；moduleNameArray已删除，模块在首次set时懒创建）
     * @param dataPath 持久化数据目录，空时取{user.dir}\persistent-cache
     * @param flushIntervalMillis 刷盘档位：FLUSH_MODE_SYNC每写force、FLUSH_MODE_ASYNC仅close时force、正数为PERIODIC周期毫秒（默认1000）
     * @param clearCountMax 每模块每次TTL清理最多清除的对象数量，null或小于等于0取默认4096
     * @param clearIntervalSeconds TTL后台清理周期秒，null或小于等于0取默认60
     */
    public static final String init(final String dataPath, final Long flushIntervalMillis, final Integer clearCountMax, final Long clearIntervalSeconds) {
        final String localDataPath = init(dataPath, flushIntervalMillis, clearCountMax, clearIntervalSeconds, null, null);
        return localDataPath;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 配置Bean初始化（动态配置化入口：数据目录、刷盘档位、清理上限、清理周期、Compaction阈值均可配，null项用默认值）
     * @param configBean 配置对象
     */
    public static final String init(final LocalCacheConfigBean configBean) {
        String localDataPath = null;
        Long localFlushIntervalMillis = null;
        Integer localClearCountMax = null;
        Long localClearIntervalSeconds = null;
        Long localMaxFileSizeBytes = null;
        Long localFlushBufferMillis = null;
        if (configBean != null) {
            localDataPath = configBean.getDataPath();
            localFlushIntervalMillis = configBean.getFlushIntervalMillis();
            localClearCountMax = configBean.getClearCountMax();
            localClearIntervalSeconds = configBean.getClearIntervalSeconds();
            localMaxFileSizeBytes = configBean.getMaxFileSizeBytes();
            localFlushBufferMillis = configBean.getFlushBufferMillis();
        }
        localDataPath = init(localDataPath, localFlushIntervalMillis, localClearCountMax, localClearIntervalSeconds, localMaxFileSizeBytes, localFlushBufferMillis);
        return localDataPath;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 核心初始化（幂等：已初始化时忽略本次参数并记警告日志）；未显式调用时首次读写自动惰性初始化；
     * moduleNameArray已删除，模块在首次set时懒创建（LocalCacheStore.p_getModuleMap）
     * @param dataPath 持久化数据目录，空时取{user.dir}\persistent-cache
     * @param flushIntervalMillis 刷盘档位：FLUSH_MODE_SYNC每写force、FLUSH_MODE_ASYNC仅close时force、正数为PERIODIC周期毫秒（默认1000）
     * @param clearCountMax 每模块每次TTL清理最多清除的对象数量，null或小于等于0取默认4096
     * @param clearIntervalSeconds TTL后台清理周期秒，null或小于等于0取默认60
     * @param maxFileSizeBytes Compaction触发阈值字节，null或小于等于0取默认64MB
     * @param flushBufferMillis 写缓冲周期刷出毫秒，null或小于等于0取默认1000（ASYNC档缓冲最迟落盘周期；PERIODIC档随force周期刷出；SYNC档不缓冲）
     */
    private static final synchronized String init(final String dataPath, final Long flushIntervalMillis, final Integer clearCountMax, final Long clearIntervalSeconds, final Long maxFileSizeBytes, final Long flushBufferMillis) {
        if (p_inited) {
            LogUtil.warn("LocalCacheUtil_init_1", "重复初始化，忽略本次参数", p_dataPath);
            return p_dataPath;
        }
        // 刷盘档位
        long localFlushMode = DEFAULT_FLUSH_INTERVAL_MILLIS;
        if (flushIntervalMillis != null) {
            localFlushMode = flushIntervalMillis;
        }
        p_flushMode = localFlushMode;
        // 清理参数与Compaction阈值（无效值回退默认）
        int localClearCountMax = DEFAULT_CLEAR_COUNT_MAX;
        if (clearCountMax != null && clearCountMax > 0) {
            localClearCountMax = clearCountMax;
        }
        p_clearCountMax = localClearCountMax;
        long localClearIntervalMillis = DEFAULT_CLEAR_INTERVAL_MILLIS;
        if (clearIntervalSeconds != null && clearIntervalSeconds > 0L) {
            localClearIntervalMillis = clearIntervalSeconds * 1000L;
        }
        p_clearIntervalMillis = localClearIntervalMillis;
        long localMaxFileSize = DEFAULT_MAX_FILE_SIZE;
        if (maxFileSizeBytes != null && maxFileSizeBytes > 0L) {
            localMaxFileSize = maxFileSizeBytes;
        }
        p_maxFileSize = localMaxFileSize;
        // 数据目录默认值
        String localDataPath = dataPath;
        if (StringUtil.isBlank(localDataPath)) {
            localDataPath = "/data/crabapple/cache";
        }
        localDataPath = UrlUtil.getStandardPath(localDataPath);
        // 2026-08-27 自动登出系统时长过短 零容忍整改（漏洞修复）：
        // 实例标识兜底链：容器根路径拿不到（getRealPath异常环境返回空串或null，实测存在）时依次退到
        // JVM工作目录（同一启动脚本稳定、重启不变）、再退到固定标记default——三者任一参与SM3哈希，
        // 杜绝"哈希输入为空导致目录退化为无哈希层共享目录"；拼接后补一次标准化消除尾斜杠等非规范形态
        String instancePath = TomcatUtil.getTomcatRootPath();
        if (StringUtil.isBlank(instancePath)) {
            instancePath = System.getProperty("user.dir");
        }
        if (StringUtil.isBlank(instancePath)) {
            instancePath = "default";
        }
        final String onlyname = Sm3Util.hash(instancePath);
        localDataPath = UrlUtil.getStandardPath(localDataPath + "/" + onlyname);
        Boolean flag = FileUtil.dirMake(localDataPath);
        if (!flag) {
            LogUtil.error("LocalCacheUtil_init_4", "本地缓存目录创建失败", localDataPath);
            return "";
        }
        // 内存结构：默认模块始终存在，其他模块在首次set时懒创建（LocalCacheStore.p_getModuleMap）
        p_mapGlobal = new ConcurrentHashMap<String, ConcurrentHashMap<String, LocalCacheData>>(32);
        ConcurrentHashMap<String, LocalCacheData> defaultModuleMap = new ConcurrentHashMap<String, LocalCacheData>(1024);
        p_mapGlobal.put(DEFAULT_MODULE_NAME, defaultModuleMap);
        // 存储引擎：锁被占用或恢复失败时内部降级内存模式（业务可用优先）
        boolean syncForce = false;
        if (p_flushMode == FLUSH_MODE_SYNC) {
            syncForce = true;
        }
        File dataDirectory = new File(localDataPath);
        p_store = new LocalCacheStore(dataDirectory, p_mapGlobal, syncForce);
        // 2026-08-27 自动登出系统时长过短 零容忍整改：
        // open失败（目录创建失败/进程锁被占用/恢复失败）已在LocalCacheStore.open内部抛IllegalStateException，
        // 此处不再吞掉降级；openFlag=false仅作防御性兜底抛出（正常不可达）
        boolean openFlag = p_store.open();
        if (!openFlag) {
            LogUtil.error("LocalCacheUtil_init_2", "存储引擎打开失败，零容忍抛异常终止", localDataPath);
            throw new IllegalStateException("本地缓存存储引擎打开失败（零容忍，不降级内存模式）：" + localDataPath);
        }
        // 后台任务：daemon具名单线程池，TTL清理与周期force串行执行；内存降级模式跳过force任务
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable runnable) {
                Thread thread = new Thread(runnable, "crabapple-persistent-cache");
                thread.setDaemon(true);
                return thread;
            }
        };
        p_executor = new ScheduledThreadPoolExecutor(1, threadFactory);
        p_executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    LocalCacheUtil.monitor();
                } catch (Throwable throwable) {
                    LogUtil.error("LocalCacheUtil_init_t1", throwable, "TTL清理任务异常");
                }
            }
        }, p_clearIntervalMillis, p_clearIntervalMillis, TimeUnit.MILLISECONDS);
        if (p_flushMode > 0L && !p_store.isMemoryMode()) {
            p_executor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    try {
                        LocalCacheUtil.p_store.force();
                    } catch (Throwable throwable) {
                        LogUtil.error("LocalCacheUtil_init_t2", throwable, "周期force任务异常");
                    }
                }
            }, p_flushMode, p_flushMode, TimeUnit.MILLISECONDS);
        }
        // ASYNC档无force任务，单独调度写缓冲周期刷出（缓冲最迟flushBufferMillis落盘页缓存，合并write系统调用）
        long localFlushBufferMillis = DEFAULT_FLUSH_BUFFER_MILLIS;
        if (flushBufferMillis != null && flushBufferMillis > 0L) {
            localFlushBufferMillis = flushBufferMillis;
        }
        p_flushBufferMillis = localFlushBufferMillis;
        if (p_flushMode == FLUSH_MODE_ASYNC && !p_store.isMemoryMode()) {
            p_executor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    try {
                        LocalCacheUtil.p_store.flushBuffer();
                    } catch (Throwable throwable) {
                        LogUtil.error("LocalCacheUtil_init_t3", throwable, "写缓冲周期刷出任务异常");
                    }
                }
            }, p_flushBufferMillis, p_flushBufferMillis, TimeUnit.MILLISECONDS);
        }
        // shutdown hook只注册一次，兜底调用close
        if (!p_hookRegistered) {
            p_hookRegistered = true;
            Thread hookThread = new Thread() {
                @Override
                public void run() {
                    LocalCacheUtil.close();
                }
            };
            hookThread.setName("crabapple-persistent-cache-shutdown");
            Runtime.getRuntime().addShutdownHook(hookThread);
        }
        p_dataPath = localDataPath;
        p_inited = true;
        return localDataPath;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 写入（永久有效）
     * @param key 缓存key
     * @param value 实际对象（须实现Serializable）
     */
    public static final void set(final String key, final Object value) {
        set(null, key, value, 0L);
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 写入
     * @param key 缓存key
     * @param value 实际对象（须实现Serializable）
     * @param duration 有效存储时间秒，0表示永久
     */
    public static final void set(final String key, final Object value, final long duration) {
        set(null, key, value, duration);
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 写入指定模块
     * @param moduleName 模块名，空时使用默认模块
     * @param key 缓存key
     * @param value 实际对象（持久化时须实现Serializable，否则记日志拒绝缓存；共享可变对象（如并发修改中的List）
     *             须由调用方自行同步，否则序列化可能失败导致该次写仅内存生效、重启丢失——与RedisUtil.setContent同契约）
     * @param duration 有效存储时间秒，0表示永久
     */
    public static final void set(final String moduleName, final String key, final Object value, final long duration) {
        set(moduleName, key, value, duration, false);
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 写入（按条持久化开关）
     * @param key 缓存key
     * @param value 实际对象
     * @param duration 有效存储时间秒，0表示永久
     * @param memoryOnly true纯内存存储（不落盘、崩溃即丢、重启不恢复、value不要求Serializable）；false持久化存储
     */
    public static final void set(final String key, final Object value, final long duration, final boolean memoryOnly) {
        set(null, key, value, duration, memoryOnly);
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 2026-09-21 tomcat停服后有时本地缓存更新问题导致重新登录
     * 委托六参重载（forceNow=false），本签名行为不变
     * 写入指定模块（按条持久化开关；同key混合模式时运行期以后写者为准；
     * memoryOnly覆盖持久化条目时自动追加DELETE墓碑，重启后该key不存在——remove/过期/压实/重启四路径语义一致）
     * @param moduleName 模块名，空时使用默认模块
     * @param key 缓存key
     * @param value 实际对象（持久化时须实现Serializable，否则记日志拒绝缓存；共享可变对象须由调用方自行同步——与RedisUtil.setContent同契约）
     * @param duration 有效存储时间秒，0表示永久
     * @param memoryOnly true纯内存存储（不落盘、崩溃即丢、重启不恢复、value不要求Serializable）；false持久化存储
     */
    public static final void set(final String moduleName, final String key, final Object value, final long duration, final boolean memoryOnly) {
        set(moduleName, key, value, duration, memoryOnly, false);
    }

    /**
     * 2026-09-21 tomcat停服后有时本地缓存更新问题导致重新登录
     * 写入指定模块（按条持久化开关+按条同步刷盘开关）：forceNow=true时本条记录立即完整落盘
     * （存储引擎先把写缓冲整批落盘保序，再直写本条并force），强制结束进程时最多损坏正在写的这一条；
     * 用于登录token这类停服后必须原样恢复的关键条目，其余调用继续走默认写缓冲路径
     * @param moduleName 模块名，空时使用默认模块
     * @param key 缓存key
     * @param value 实际对象（持久化时须实现Serializable，否则记日志拒绝缓存；共享可变对象须由调用方自行同步——与RedisUtil.setContent同契约）
     * @param duration 有效存储时间秒，0表示永久
     * @param memoryOnly true纯内存存储（不落盘、崩溃即丢、重启不恢复、value不要求Serializable）；false持久化存储
     * @param forceNow true本条立即落盘并force（仅对持久化条目生效）；false走默认写缓冲路径
     */
    public static final void set(final String moduleName, final String key, final Object value, final long duration, final boolean memoryOnly, final boolean forceNow) {
        p_init();
        // 静态校验：空值直接返回；非Serializable与超长拒绝仅针对持久化条目（纯内存条目value不限类型）
        if (StringUtil.isBlank(key)) {
            return;
        }
        if (value == null) {
            return;
        }
        if (!memoryOnly) {
            if (!(value instanceof Serializable)) {
                LogUtil.error("LocalCacheUtil_set_1", "value未实现Serializable，拒绝写入", moduleName, key);
                return;
            }
        }
        if (key.length() > LocalCacheRecord.MAX_KEY_LENGTH) {
            LogUtil.error("LocalCacheUtil_set_2", "key超长拒绝写入", moduleName, String.valueOf(key.length()));
            return;
        }
        String localModuleName = p_formatModuleName(moduleName);
        if (localModuleName == null) {
            LogUtil.error("LocalCacheUtil_set_3", "moduleName超长拒绝写入", moduleName);
            return;
        }
        long localDuration = duration;
        if (localDuration < 0L) {
            localDuration = 0L;
        }
        // 本地引用判空：close()置空p_store时在途写入不抛NPE（缓存异常不阻断业务）
        LocalCacheStore localStore = p_store;
        if (localStore == null) {
            return;
        }
        localStore.appendPut(localModuleName, key, value, localDuration, memoryOnly, forceNow);
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 读取（读路径零磁盘零锁：内存直读；不存在或已过期返回null）
     * @param key 缓存key
     * @return 实际对象；不存在或已过期返回null
     */
    public static final Object get(final String key) {
        Object value = get(null, key);
        return value;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 2026-08-28 本地持久化缓存框架问题修复
     * 读取指定模块（读路径零磁盘零锁：内存直读；不存在或已过期返回null）
     * 重放恢复的懒条目在首次get时物化（反序列化推迟到业务期，静态依赖已就绪；失败自清理返回null由调用方回源重建）
     * @param moduleName 模块名，空时使用默认模块
     * @param key 缓存key
     * @return 实际对象；不存在或已过期返回null
     */
    public static final Object get(final String moduleName, final String key) {
        p_init();
        LocalCacheData entry = p_getEntry(moduleName, key);
        if (entry == null) {
            return null;
        }
        // 惰性过期：过期即视为不存在（后台线程稍后补DELETE记录）
        long now = System.currentTimeMillis();
        boolean expired = entry.isExpired(now);
        if (expired) {
            return null;
        }
        // 本地引用判空：close()并发防护（关闭后未物化的懒条目返回null，进程退出窗口可接受）
        LocalCacheStore localStore = p_store;
        if (localStore == null) {
            Object fallbackValue = entry.getData();
            return fallbackValue;
        }
        // 懒条目物化：与p_getEntry的模块名归一化规则一致（空模块名归一到默认模块）
        String localModuleName = moduleName;
        if (StringUtil.isBlank(localModuleName)) {
            localModuleName = DEFAULT_MODULE_NAME;
        }
        Object value = localStore.materialize(localModuleName, key, entry);
        return value;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 泛型读取（对标RedisUtil.getContent，免去调用方强转）
     * @param key 缓存key
     * @return 与写入时类型一致的对象；不存在或已过期返回null
     */
    public static final <T> T getContent(final String key) {
        T t = getContent(null, key);
        return t;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 泛型读取指定模块（对标RedisUtil.getContent，免去调用方强转）
     * @param moduleName 模块名，空时使用默认模块
     * @param key 缓存key
     * @return 与写入时类型一致的对象；不存在或已过期返回null
     */
    @SuppressWarnings("unchecked")
    public static final <T> T getContent(final String moduleName, final String key) {
        Object value = get(moduleName, key);
        if (value == null) {
            return null;
        }
        T t = (T) value;
        return t;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 是否包含指定的key（含过期判断，与get可见性一致）
     * @param key 缓存key
     * @return true表示存在且未过期
     */
    public static final boolean hasKey(final String key) {
        boolean flag = hasKey(null, key);
        return flag;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 是否包含指定的key（含过期判断，与get可见性一致）
     * @param moduleName 模块名，空时使用默认模块
     * @param key 缓存key
     * @return true表示存在且未过期
     */
    public static final boolean hasKey(final String moduleName, final String key) {
        p_init();
        LocalCacheData entry = p_getEntry(moduleName, key);
        if (entry == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        boolean expired = entry.isExpired(now);
        if (expired) {
            return false;
        }
        return true;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 删除（持久化DELETE记录）
     * @param key 缓存key
     */
    public static final void remove(final String key) {
        remove(null, key);
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 删除指定模块（持久化DELETE记录；key不存在时直接返回不产生记录，幂等）
     * @param moduleName 模块名，空时使用默认模块
     * @param key 缓存key
     */
    public static final void remove(final String moduleName, final String key) {
        p_init();
        if (StringUtil.isBlank(key)) {
            return;
        }
        String localModuleName = p_formatModuleName(moduleName);
        if (localModuleName == null) {
            return;
        }
        // 本地引用判空：close()并发防护
        LocalCacheStore localStore = p_store;
        if (localStore == null) {
            return;
        }
        localStore.appendDelete(localModuleName, key);
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 按前缀批量删除（对标RedisUtil.removeMatch，有意简化为前缀匹配，Redis原为通配模糊匹配）
     * @param keyPrefix key前缀
     */
    public static final void removeMatch(final String keyPrefix) {
        removeMatch(null, keyPrefix);
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 按前缀批量删除指定模块（对标RedisUtil.removeMatch，有意简化为前缀匹配，Redis原为通配模糊匹配）
     * @param moduleName 模块名，空时使用默认模块
     * @param keyPrefix key前缀
     */
    public static final void removeMatch(final String moduleName, final String keyPrefix) {
        p_init();
        if (p_mapGlobal == null) {
            return;
        }
        if (StringUtil.isBlank(keyPrefix)) {
            return;
        }
        String localModuleName = moduleName;
        if (StringUtil.isBlank(localModuleName)) {
            localModuleName = DEFAULT_MODULE_NAME;
        }
        ConcurrentHashMap<String, LocalCacheData> moduleMap = p_mapGlobal.get(localModuleName);
        if (moduleMap == null) {
            return;
        }
        // 先收集匹配key再逐个删除（逐个走appendDelete持久化）
        List<String> matchKeyList = new ArrayList<String>();
        Iterator<String> keyIterator = moduleMap.keySet().iterator();
        while (keyIterator.hasNext()) {
            String mapKey = keyIterator.next();
            if (mapKey == null) {
                continue;
            }
            boolean matched = mapKey.startsWith(keyPrefix);
            if (matched) {
                matchKeyList.add(mapKey);
            }
        }
        for (String matchKey : matchKeyList) {
            remove(localModuleName, matchKey);
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 清空全部模块（有意增强：LocalCacheUtil.clear()仅清默认模块，本方法写锁下重写空日志清全部）
     */
    public static final void clear() {
        p_init();
        // 本地引用判空：close()并发防护
        LocalCacheStore localStore = p_store;
        if (localStore == null) {
            return;
        }
        localStore.rewriteClearAll();
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 清空指定模块（模块不存在直接返回不重写；否则写锁下重写日志仅保留其他模块条目）
     * @param moduleName 模块名，空时使用默认模块
     */
    public static final void clear(final String moduleName) {
        p_init();
        if (p_mapGlobal == null) {
            return;
        }
        String localModuleName = moduleName;
        if (StringUtil.isBlank(localModuleName)) {
            localModuleName = DEFAULT_MODULE_NAME;
        }
        ConcurrentHashMap<String, LocalCacheData> moduleMap = p_mapGlobal.get(localModuleName);
        if (moduleMap == null) {
            return;
        }
        if (moduleMap.isEmpty()) {
            return;
        }
        // 本地引用判空：close()并发防护
        LocalCacheStore localStore = p_store;
        if (localStore == null) {
            return;
        }
        localStore.rewriteExcludeModule(localModuleName);
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 获取默认模块全部有效key（含惰性过期过滤）
     * @return 有效key列表
     */
    public static final List<String> getKeyList() {
        List<String> keyList = getKeyList(null);
        return keyList;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 获取指定模块全部有效key（含惰性过期过滤）
     * @param moduleName 模块名，空时使用默认模块
     * @return 有效key列表
     */
    public static final List<String> getKeyList(final String moduleName) {
        List<String> keyList = new ArrayList<String>();
        p_init();
        if (p_mapGlobal == null) {
            return keyList;
        }
        String localModuleName = moduleName;
        if (StringUtil.isBlank(localModuleName)) {
            localModuleName = DEFAULT_MODULE_NAME;
        }
        ConcurrentHashMap<String, LocalCacheData> moduleMap = p_mapGlobal.get(localModuleName);
        if (moduleMap == null) {
            return keyList;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, LocalCacheData>> entryIterator = moduleMap.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<String, LocalCacheData> entryMap = entryIterator.next();
            LocalCacheData entry = entryMap.getValue();
            if (entry == null) {
                continue;
            }
            boolean expired = entry.isExpired(now);
            if (expired) {
                continue;
            }
            String mapKey = entryMap.getKey();
            keyList.add(mapKey);
        }
        return keyList;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 获取默认模块有效key数量
     * @return 有效key数量
     */
    public static final int size() {
        int count = size(null);
        return count;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 获取指定模块有效key数量（含惰性过期过滤）
     * @param moduleName 模块名，空时使用默认模块
     * @return 有效key数量
     */
    public static final int size(final String moduleName) {
        List<String> keyList = getKeyList(moduleName);
        int count = keyList.size();
        return count;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 获取写入线程id（LocalCacheUtil既有能力的兼容保留）
     * @param key 缓存key
     * @return 写入线程id；空key返回-1L、不存在返回-2L（重启恢复条目无写入线程id，按不存在处理）
     */
    public static final Long getThreadId(final String key) {
        Long threadId = getThreadId(null, key);
        return threadId;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 获取指定模块写入线程id（LocalCacheUtil既有能力的兼容保留）
     * @param moduleName 模块名，空时使用默认模块
     * @param key 缓存key
     * @return 写入线程id；空key返回-1L、不存在返回-2L（重启恢复条目无写入线程id，按不存在处理）
     */
    public static final Long getThreadId(final String moduleName, final String key) {
        p_init();
        if (StringUtil.isBlank(key)) {
            return -1L;
        }
        LocalCacheData entry = p_getEntry(moduleName, key);
        if (entry == null) {
            return -2L;
        }
        Long threadId = entry.getThreadId();
        if (threadId == null) {
            return -2L;
        }
        return threadId;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 管理方法：立即触发一次Compaction（写锁全程独占）
     */
    public static final void compact() {
        p_init();
        // 本地引用判空：close()并发防护
        LocalCacheStore localStore = p_store;
        if (localStore == null) {
            return;
        }
        localStore.compact();
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 执行定时清理（由后台线程周期调用，亦可手动触发）：过期条目补写DELETE记录后条件移除，
     * 每模块每轮最多清除clearCountMax个（与LocalCacheUtil.monitor语义一致，剩余下轮继续）；
     * 顺带检查Compaction触发条件（内存降级模式下跳过force与Compaction，仅保留TTL清理）
     */
    public static final void monitor() {
        if (p_mapGlobal == null) {
            return;
        }
        long now = System.currentTimeMillis();
        // 本地引用：close()并发防护（判空后不再直接访问p_store，避免TOCTOU）
        LocalCacheStore localStore = p_store;
        // TTL清理：条件移除（按扫描时条目引用匹配），防止误删清理窗口内新写入的同key值
        Iterator<Map.Entry<String, ConcurrentHashMap<String, LocalCacheData>>> moduleIterator = p_mapGlobal.entrySet().iterator();
        while (moduleIterator.hasNext()) {
            Map.Entry<String, ConcurrentHashMap<String, LocalCacheData>> moduleEntry = moduleIterator.next();
            String moduleName = moduleEntry.getKey();
            ConcurrentHashMap<String, LocalCacheData> moduleMap = moduleEntry.getValue();
            if (moduleMap == null) {
                continue;
            }
            int clearCount = 0;
            Iterator<Map.Entry<String, LocalCacheData>> entryIterator = moduleMap.entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<String, LocalCacheData> entryMap = entryIterator.next();
                LocalCacheData entry = entryMap.getValue();
                if (entry == null) {
                    continue;
                }
                boolean expired = entry.isExpired(now);
                if (!expired) {
                    continue;
                }
                // 每模块每轮清理上限（与LocalCacheUtil一致），达到上限停止本轮、下轮继续
                if (clearCount >= p_clearCountMax) {
                    break;
                }
                if (localStore == null) {
                    entryIterator.remove();
                } else {
                    localStore.appendExpireDelete(moduleName, entryMap.getKey(), entry);
                }
                clearCount = clearCount + 1;
            }
        }
        // Compaction触发检查：日志超过阈值时后台压实
        if (localStore == null) {
            return;
        }
        boolean needCompact = localStore.needCompact(p_maxFileSize);
        if (needCompact) {
            localStore.compact();
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 是否处于内存降级模式（进程锁被占或恢复失败时不落盘、退出即全丢；运维监控用，生产建议显式init独立数据目录）
     * @return true表示当前实例仅内存运行，数据不持久化
     */
    public static final boolean isMemoryMode() {
        p_init();
        LocalCacheStore localStore = p_store;
        if (localStore == null) {
            return true;
        }
        boolean flag = localStore.isMemoryMode();
        return flag;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 关闭（线程安全且幂等）：停止后台任务、force日志、关闭通道、释放进程锁、清空内存结构；
     * close后再次调用API将自动重新惰性初始化（重放日志、重新加锁）
     */
    public static final synchronized void close() {
        if (!p_inited) {
            return;
        }
        p_inited = false;
        if (p_executor != null) {
            p_executor.shutdownNow();
            p_executor = null;
        }
        if (p_store != null) {
            p_store.close();
            p_store = null;
        }
        p_mapGlobal = null;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 惰性初始化快速路径（已完成时零开销直接返回）
     */
    private static final void p_init() {
        if (p_inited) {
            return;
        }
        init();
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 模块名规整：空时取默认模块；UTF-8字节超过256返回null（防MODULE_LEN编码溢出）
     */
    private static final String p_formatModuleName(final String moduleName) {
        String localModuleName = moduleName;
        if (StringUtil.isBlank(localModuleName)) {
            localModuleName = DEFAULT_MODULE_NAME;
        }
        byte[] moduleBytes = localModuleName.getBytes(StandardCharsets.UTF_8);
        if (moduleBytes.length > MAX_MODULE_NAME_LENGTH) {
            return null;
        }
        return localModuleName;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 获取内存条目（不做过期判断，由调用方按需惰性判断）
     */
    private static final LocalCacheData p_getEntry(final String moduleName, final String key) {
        if (p_mapGlobal == null) {
            return null;
        }
        if (StringUtil.isBlank(key)) {
            return null;
        }
        String localModuleName = moduleName;
        if (StringUtil.isBlank(localModuleName)) {
            localModuleName = DEFAULT_MODULE_NAME;
        }
        ConcurrentHashMap<String, LocalCacheData> moduleMap = p_mapGlobal.get(localModuleName);
        if (moduleMap == null) {
            return null;
        }
        LocalCacheData entry = moduleMap.get(key);
        return entry;
    }

}
