package xc.crabapple.core.cache;

import xc.crabapple.core.log.LogUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 2026-08-20 构建替代redis的本地API持久化缓存框架
 * 存储引擎：单文件append-only日志（数据文件即WAL）、进程锁、恢复重放、周期force、Compaction原子换名
 * 写入顺序铁则：先写日志后改内存，且两者在同一synchronized块内完成（同key并发写下内存最终值与日志最后一条记录一致）
 */
public final class LocalCacheStore {

    private static final String DATA_FILE_NAME = "data.db";                   // 活动数据文件名（append-only数据文件，用.db后缀避免被日志清理工具按*.log误删）
    private static final String DATA_FILE_NAME_LEGACY = "data.log";           // 旧版数据文件名（兼容迁移用）
    private static final String DATA_NEW_FILE_NAME = "data.db.new";           // Compaction临时文件名
    private static final String DATA_BAK_FILE_NAME = "data.db.bak";           // Compaction旧文件备份名
    private static final String LOCK_FILE_NAME = "persistent-cache.lock";      // 进程独占锁文件名
    private static final int FLUSH_BUFFER_LIMIT_BYTES = 256 * 1024;            // 写缓冲批量落盘阈值：满256KB立即刷出

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, LocalCacheData>> p_mapGlobal;   // 与门面共享的内存结构
    private final File p_dataDirectory;      // 数据目录
    private final File p_dataFile;           // 活动日志文件
    private final File p_dataNewFile;        // Compaction临时文件
    private final File p_dataBakFile;        // Compaction旧文件备份
    private final File p_lockFile;           // 进程锁文件
    private final Object p_appendLock = new Object();        // 追加同步锁：块内完成取号、写日志、发布内存
    private final boolean p_syncForce;       // true表示SYNC档：每次写完成后立即force
    private final ReentrantReadWriteLock p_fileLock = new ReentrantReadWriteLock();   // 文件句柄保护读写锁：append持读锁，rewrite持写锁（换文件期间阻塞写）

    private FileChannel p_dataChannel = null;                // 活动日志追加通道
    private FileChannel p_lockChannel = null;                // 进程锁通道（持有即持有锁）
    private long p_sequence = 0L;                            // 当前最大日志序号
    private boolean p_memoryMode = false;                    // 内存降级模式：不落盘不恢复
    private volatile boolean p_closed = false;               // 关闭标志：close后禁止重开通道，防止后台Compaction与close竞态造成句柄泄漏
    private ByteArrayOutputStream p_writeBuffer = new ByteArrayOutputStream(FLUSH_BUFFER_LIMIT_BYTES);   // 写缓冲：非SYNC档先入内存，满256KB或周期/关闭时批量落盘（合并write系统调用；大记录顶格后超限重建收缩）
    private long p_writeBufferBytes = 0L;    // 写缓冲累计写入字节（用于swap后判断是否超限重建，防16MB大记录把常驻容量永久顶高）
    // 2026-08-28 本地持久化缓存框架问题修复
    // 恢复重放统计（仅p_recover执行线程访问，重放前清零、结束后打一行INFO汇总）
    private long p_replayPutCount = 0L;      // 重放装入条数（懒装填）
    private long p_replayExpireCount = 0L;   // 重放跳过的已过期条数
    private long p_replayDeleteCount = 0L;   // 重放应用的DELETE记录条数

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * @param dataDirectory 数据目录（不存在时自动创建）
     * @param mapGlobal 与门面共享的内存结构（发布、恢复直接操作该结构）
     * @param syncForce true表示SYNC档（每写force）；false由门面按周期force
     */
    public LocalCacheStore(final File dataDirectory, final ConcurrentHashMap<String, ConcurrentHashMap<String, LocalCacheData>> mapGlobal, final boolean syncForce){
        this.p_mapGlobal = mapGlobal;
        this.p_dataDirectory = dataDirectory;
        this.p_dataFile = new File(dataDirectory, DATA_FILE_NAME);
        this.p_dataNewFile = new File(dataDirectory, DATA_NEW_FILE_NAME);
        this.p_dataBakFile = new File(dataDirectory, DATA_BAK_FILE_NAME);
        this.p_lockFile = new File(dataDirectory, LOCK_FILE_NAME);
        this.p_syncForce = syncForce;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 打开存储：建目录、tryLock进程锁、恢复重放
     * 2026-08-27 自动登出系统时长过短 零容忍整改：
     * 目录创建失败、进程锁被占用（含同JVM重锁抛OverlappingFileLockException）一律抛IllegalStateException，
     * 不再降级内存模式静默运行——持久化失败必须当场暴露，由调用方中止启动（此前"打日志降级"被定性为自欺欺人）
     * @return true表示持久化模式
     */
    public final boolean open(){
        try{
            Files.createDirectories(this.p_dataDirectory.toPath());
        }catch(Throwable throwable){
            LogUtil.error("LocalCacheStore_open_1", throwable, "数据目录创建失败，零容忍抛异常终止", this.p_dataDirectory);
            throw new IllegalStateException("本地缓存数据目录创建失败（零容忍，不降级内存模式）：" + this.p_dataDirectory, throwable);
        }
        // 进程锁：跨JVM被占时tryLock返回null、同JVM重锁抛OverlappingFileLockException，两种情况均零容忍抛异常
        try{
            FileOutputStream lockStream = new FileOutputStream(this.p_lockFile, true);
            this.p_lockChannel = lockStream.getChannel();
            FileLock fileLock = this.p_lockChannel.tryLock();
            if(fileLock == null){
                LogUtil.error("LocalCacheStore_open_2", "进程锁被其他JVM占用，零容忍抛异常终止", this.p_dataDirectory);
                this.p_closeLockChannel();
                throw new IllegalStateException("本地缓存目录进程锁被其他JVM占用（零容忍，不降级内存模式）：" + this.p_dataDirectory);
            }
        }catch(IllegalStateException throwable){
            throw throwable;
        }catch(Throwable throwable){
            boolean overlapping = throwable instanceof OverlappingFileLockException;
            LogUtil.error("LocalCacheStore_open_3", throwable, "进程锁获取异常，零容忍抛异常终止", String.valueOf(overlapping), this.p_dataDirectory);
            this.p_closeLockChannel();
            throw new IllegalStateException("本地缓存目录进程锁获取异常（同JVM重锁=" + overlapping + "，零容忍，不降级内存模式）：" + this.p_dataDirectory, throwable);
        }
        // 恢复重放
        this.p_recover();
        return true;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 2026-09-21 tomcat停服后有时本地缓存更新问题导致重新登录
     * 委托六参重载（forceNow=false），本签名行为不变
     * PUT写入：锁外序列化与大小校验，读锁+synchronized块内完成取号、写日志、发布内存
     * @param moduleName 模块名
     * @param key 缓存key
     * @param value 实际对象（须实现Serializable）
     * @param durationSeconds 有效存储时间秒，0表示永久
     * @return 日志序号；序列化失败或超限时返回-1（不写入）
     */
    public final long appendPut(final String moduleName, final String key, final Object value, final long durationSeconds, final boolean memoryOnly){
        long appendResult = appendPut(moduleName, key, value, durationSeconds, memoryOnly, false);
        return appendResult;
    }

    /**
     * 2026-09-21 tomcat停服后有时本地缓存更新问题导致重新登录
     * PUT写入（按条同步刷盘开关）：forceNow=true时本条记录不走写缓冲，先把写缓冲整批落盘保序，
     * 再直写本条并立即force（与SYNC档同一写法，仅作用于本条），强制结束进程时最多损坏正在写的这一条；
     * 用于登录token这类停服后必须原样恢复的关键条目
     * @param moduleName 模块名
     * @param key 缓存key
     * @param value 实际对象（须实现Serializable）
     * @param durationSeconds 有效存储时间秒，0表示永久
     * @param memoryOnly true纯内存条目（不落盘）
     * @param forceNow true本条立即落盘并force（仅对持久化条目生效）；false走默认写缓冲路径
     * @return 日志序号；序列化失败或超限时返回-1（不写入）
     */
    public final long appendPut(final String moduleName, final String key, final Object value, final long durationSeconds, final boolean memoryOnly, final boolean forceNow){
        if(memoryOnly){
            ConcurrentHashMap<String, LocalCacheData> memoryModuleMap = this.p_getModuleMap(moduleName, true);
            if(memoryModuleMap == null){
                return 0L;
            }
            // 防旧值复活墓碑：当前条目为持久化条目（日志中存在旧PUT）时，在读锁+追加锁内复核并追加DELETE墓碑
            // （锁内复核消除TOCTOU：避免墓碑误删"判定之后才写入"的新持久化值），
            // 消除"remove/过期对memoryOnly条目不落DELETE导致重启旧值复活、压不压实结果不一致"的陷阱；
            // 覆盖纯内存条目或全新key时无墓碑需求，保持零IO无锁快路径
            LocalCacheData existEntry = memoryModuleMap.get(key);
            if(existEntry != null && !existEntry.isMemoryOnly()){
                this.p_fileLock.readLock().lock();
                try{
                    synchronized(this.p_appendLock){
                        // 锁内复核：条目仍为同一持久化条目才写墓碑（判定后被并发持久化写覆盖时不误删新值）
                        LocalCacheData confirmEntry = memoryModuleMap.get(key);
                        if(confirmEntry != null && !confirmEntry.isMemoryOnly()){
                            this.p_sequence = this.p_sequence + 1L;
                            long sequence = this.p_sequence;
                            if(!this.p_memoryMode){
                                long appendResult = this.p_appendRecord(LocalCacheRecord.RECORD_TYPE_DELETE, sequence, 0L, moduleName, key, null, false);
                                if(appendResult == -1L){
                                    LogUtil.error("LocalCacheStore_appendPut_5", "混合模式墓碑写入失败，重启可能复活旧持久化值", moduleName, key);
                                }
                            }
                        }
                    }
                }finally{
                    this.p_fileLock.readLock().unlock();
                }
            }
            // 发布内存条目：跳过序列化、编码与日志（value不要求Serializable），ConcurrentHashMap原子性保证线程安全；
            // 纯内存写与持久化写之间无严格先后序（并发时运行期以后写者为准，重启以日志序为准）
            long memoryExpireAt = 0L;
            if(durationSeconds > 0L){
                memoryExpireAt = System.currentTimeMillis() + durationSeconds * 1000L;
            }
            LocalCacheData memoryEntry = new LocalCacheData();
            memoryEntry.setData(value);
            memoryEntry.setExpireAt(memoryExpireAt);
            memoryEntry.setSequence(0L);
            Thread memoryThread = Thread.currentThread();
            memoryEntry.setThreadId(memoryThread.threadId());
            memoryEntry.setMemoryOnly(true);
            memoryModuleMap.put(key, memoryEntry);
            return 0L;
        }
        // 序列化与大小校验（锁外一次性完成）
        byte[] valueBytes;
        try{
            valueBytes = p_encodeValue(value);
        }catch(Throwable throwable){
            LogUtil.error("LocalCacheStore_appendPut_1", throwable, "value序列化失败，拒绝写入", moduleName, key);
            return -1L;
        }
        if(valueBytes.length > LocalCacheRecord.MAX_VALUE_LENGTH){
            LogUtil.error("LocalCacheStore_appendPut_2", "value超过16MB上限，拒绝写入", moduleName, key, String.valueOf(valueBytes.length));
            return -1L;
        }
        // key与模块名按UTF-8字节数精确校验（中文等多字节字符场景，超限拒绝且不发布内存）
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if(keyBytes.length > LocalCacheRecord.MAX_KEY_LENGTH){
            LogUtil.error("LocalCacheStore_appendPut_3", "key超过4KB上限，拒绝写入", moduleName, String.valueOf(keyBytes.length));
            return -1L;
        }
        byte[] moduleBytes = moduleName.getBytes(StandardCharsets.UTF_8);
        if(moduleBytes.length > LocalCacheRecord.MAX_MODULE_LENGTH){
            LogUtil.error("LocalCacheStore_appendPut_4", "moduleName超过256字节上限，拒绝写入", moduleName);
            return -1L;
        }
        // 计算绝对过期时间
        long expireAt = 0L;
        if(durationSeconds > 0L){
            expireAt = System.currentTimeMillis() + durationSeconds * 1000L;
        }
        // 读锁保护文件句柄，追加同步块保证记录不交错与序号单调
        this.p_fileLock.readLock().lock();
        try{
            synchronized(this.p_appendLock){
                // 块内取号保证日志序号单调
                this.p_sequence = this.p_sequence + 1L;
                long sequence = this.p_sequence;
                // 先写日志：append失败仅记日志（降级为本进程内存可见，重启丢此条）
                if(!this.p_memoryMode){
                    long appendResult = this.p_appendRecord(LocalCacheRecord.RECORD_TYPE_PUT, sequence, expireAt, moduleName, key, valueBytes, forceNow);
                    if(appendResult == -1L){
                        LogUtil.error("LocalCacheStore_appendPut_6", "日志追加失败，本条仅内存可见", moduleName, key);
                    }
                }
                // 后发布内存：保证内存不领先于日志
                LocalCacheData entry = new LocalCacheData();
                entry.setData(value);
                entry.setExpireAt(expireAt);
                entry.setSequence(sequence);
                Thread currentThread = Thread.currentThread();
                entry.setThreadId(currentThread.threadId());
                entry.setMemoryOnly(false);
                ConcurrentHashMap<String, LocalCacheData> moduleMap = this.p_getModuleMap(moduleName, true);
                if(moduleMap != null){
                    moduleMap.put(key, entry);
                }
                return sequence;
            }
        }finally{
            this.p_fileLock.readLock().unlock();
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * DELETE写入：key不存在时直接返回不产生记录（幂等）
     * @param moduleName 模块名
     * @param key 缓存key
     */
    public final void appendDelete(final String moduleName, final String key){
        this.p_fileLock.readLock().lock();
        try{
            synchronized(this.p_appendLock){
                // 幂等：不存在不产生DELETE记录
                ConcurrentHashMap<String, LocalCacheData> moduleMap = this.p_getModuleMap(moduleName, false);
                if(moduleMap == null){
                    return;
                }
                LocalCacheData existEntry = moduleMap.get(key);
                if(existEntry == null){
                    return;
                }
                // 纯内存条目：只删内存、不产生DELETE记录（日志中本无该条）
                if(existEntry.isMemoryOnly()){
                    moduleMap.remove(key);
                    return;
                }
                // 已过期条目视为不存在：不产生DELETE记录（方案：不存在的key不产生记录），
                // 仅移除内存；磁盘上的PUT记录由后续Compaction顺带清除
                long now = System.currentTimeMillis();
                boolean expired = existEntry.isExpired(now);
                if(expired){
                    moduleMap.remove(key);
                    return;
                }
                this.p_sequence = this.p_sequence + 1L;
                long sequence = this.p_sequence;
                if(!this.p_memoryMode){
                    long appendResult = this.p_appendRecord(LocalCacheRecord.RECORD_TYPE_DELETE, sequence, 0L, moduleName, key, null, false);
                    if(appendResult == -1L){
                        LogUtil.error("LocalCacheStore_appendDelete_1", "删除记录追加失败，仅移除内存", moduleName, key);
                    }
                }
                moduleMap.remove(key);
            }
        }finally{
            this.p_fileLock.readLock().unlock();
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * TTL过期清理写入：条件移除——仅当内存中当前条目与扫描时持有的条目为同一引用时才写DELETE并移除，
     * 防止误删清理窗口内新写入的同key值
     * @param moduleName 模块名
     * @param key 缓存key
     * @param expectEntry 清理线程扫描时持有的条目引用
     */
    public final void appendExpireDelete(final String moduleName, final String key, final LocalCacheData expectEntry){
        this.p_fileLock.readLock().lock();
        try{
            synchronized(this.p_appendLock){
                ConcurrentHashMap<String, LocalCacheData> moduleMap = this.p_getModuleMap(moduleName, false);
                if(moduleMap == null){
                    return;
                }
                // 条件移除：引用不匹配说明清理窗口内已被覆盖或删除，跳过
                LocalCacheData currentEntry = moduleMap.get(key);
                if(currentEntry != expectEntry){
                    return;
                }
                // 纯内存条目：只条件移除内存、不产生DELETE记录（日志中本无该条）
                if(currentEntry.isMemoryOnly()){
                    moduleMap.remove(key);
                    return;
                }
                this.p_sequence = this.p_sequence + 1L;
                long sequence = this.p_sequence;
                if(!this.p_memoryMode){
                    long appendResult = this.p_appendRecord(LocalCacheRecord.RECORD_TYPE_DELETE, sequence, 0L, moduleName, key, null, false);
                    if(appendResult == -1L){
                        LogUtil.error("LocalCacheStore_appendExpireDelete_1", "过期删除记录追加失败，仅移除内存", moduleName, key);
                    }
                }
                moduleMap.remove(key);
            }
        }finally{
            this.p_fileLock.readLock().unlock();
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 周期force：先刷出写缓冲再force（缓冲数据不刷出则force不覆盖，丢失窗口虚增）
     */
    public final void force(){
        if(this.p_memoryMode){
            return;
        }
        this.p_fileLock.readLock().lock();
        try{
            synchronized(this.p_appendLock){
                try{
                    if(this.p_writeBuffer.size() > 0 && this.p_dataChannel != null){
                        byte[] batchBytes = this.p_swapBuffer();
                        this.p_writeBatch(batchBytes);
                    }
                    if(this.p_dataChannel != null && this.p_dataChannel.isOpen()){
                        this.p_dataChannel.force(false);
                    }
                }catch(Throwable throwable){
                    LogUtil.error("LocalCacheStore_force_1", throwable, "周期force失败", this.p_dataFile);
                }
            }
        }finally{
            this.p_fileLock.readLock().unlock();
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 判断是否需要Compaction
     * @param maxFileSize 日志文件大小阈值字节
     * @return true表示超过阈值需要压实
     */
    public final boolean needCompact(final long maxFileSize){
        if(this.p_memoryMode){
            return false;
        }
        long fileLength = this.p_dataFile.length();
        boolean flag = fileLength > maxFileSize;
        return flag;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * Compaction：重写全部有效条目到新文件并原子换名（写锁全程独占，重写期间阻塞写、get不受影响）
     */
    public final void compact(){
        this.p_rewriteCore(null, false);
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 清空全部模块：写锁下重写空日志并清空内存（有意增强：LocalCacheUtil.clear()仅清默认模块，本方法清全部）
     */
    public final void rewriteClearAll(){
        this.p_rewriteCore(null, true);
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 清空指定模块：写锁下重写日志仅保留其他模块条目（模块数据由调用方确认存在后再调用）
     * @param moduleName 待清空的模块名
     */
    public final void rewriteExcludeModule(final String moduleName){
        this.p_rewriteCore(moduleName, false);
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 关闭存储：force、关闭日志通道、释放进程锁；幂等可重复调用；
     * 持写锁执行，与后台Compaction互斥，防止重开已关闭通道造成句柄泄漏
     */
    public final synchronized void close(){
        this.p_fileLock.writeLock().lock();
        try{
            this.p_closed = true;
            try{
                if(this.p_dataChannel != null){
                    // 关闭前先刷出写缓冲（缓冲数据不落盘则关闭即丢）
                    synchronized(this.p_appendLock){
                        if(this.p_writeBuffer.size() > 0){
                            byte[] batchBytes = this.p_swapBuffer();
                            this.p_writeBatch(batchBytes);
                        }
                    }
                    if(!this.p_memoryMode && this.p_dataChannel.isOpen()){
                        this.p_dataChannel.force(false);
                    }
                    this.p_dataChannel.close();
                    this.p_dataChannel = null;
                }
            }catch(Throwable throwable){
                LogUtil.error("LocalCacheStore_close_1", throwable, "关闭日志通道失败", this.p_dataFile);
            }
        }finally{
            this.p_fileLock.writeLock().unlock();
        }
        this.p_closeLockChannel();
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 是否处于内存降级模式
     * @return true表示内存模式（不落盘不恢复）
     */
    public final boolean isMemoryMode(){
        boolean flag = this.p_memoryMode;
        return flag;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 恢复入口：换名兜底、扫描重放、尾部截断、打开追加通道
     */
    private final void p_recover(){
        // 旧版data.log兼容迁移：存在旧名且新名不存在时换名为data.db
        this.p_migrateLegacyDataFile();
        // Compaction未竟换名兜底
        this.p_recoverFallbackRename();
        try{
            if(!this.p_dataFile.exists()){
                this.p_openDataChannel();
                LogUtil.info("LocalCacheStore_p_recover_1", "无数据文件，空库启动", this.p_dataDirectory);
                return;
            }
            // 读写模式扫描：重放装填内存，返回最后完整记录终点
            RandomAccessFile randomFile = new RandomAccessFile(this.p_dataFile, "rw");
            FileChannel channel = randomFile.getChannel();
            // 2026-08-28 本地持久化缓存框架问题修复
            // 重放统计清零（同一Store实例理论上仅open恢复一次，清零防御异常路径二次进入）
            this.p_replayPutCount = 0L;
            this.p_replayExpireCount = 0L;
            this.p_replayDeleteCount = 0L;
            try{
                long lastGoodOffset = this.p_replay(channel, true);
                long fileSize = channel.size();
                if(lastGoodOffset < fileSize){
                    LogUtil.warn("LocalCacheStore_p_recover_2", "检测到尾部不完整或损坏记录，截断至最后完整记录", String.valueOf(lastGoodOffset), String.valueOf(fileSize));
                    channel.truncate(lastGoodOffset);
                }
            }finally{
                channel.close();
                randomFile.close();
            }
            // 2026-08-28 本地持久化缓存框架问题修复
            // 重放汇总一行INFO：懒装填后重放不再有反序列化失败刷屏，汇总收敛恢复可观测性
            LogUtil.info("LocalCacheStore_p_recover_7", "本地缓存恢复完成（懒装填）", String.valueOf(this.p_replayPutCount), String.valueOf(this.p_replayExpireCount), String.valueOf(this.p_replayDeleteCount), String.valueOf(this.p_dataFile.length()));
            // 清理Compaction失败残留（data.db存在时new/bak均为垃圾，方案4.3.3恢复步骤5）
            if(this.p_dataNewFile.exists()){
                this.p_dataNewFile.delete();
                LogUtil.warn("LocalCacheStore_p_recover_5", "清理残留data.db.new", this.p_dataNewFile);
            }
            if(this.p_dataBakFile.exists()){
                this.p_dataBakFile.delete();
                LogUtil.warn("LocalCacheStore_p_recover_6", "清理残留data.db.bak", this.p_dataBakFile);
            }
            // 转为追加模式通道
            this.p_openDataChannel();
        }catch(Throwable throwable){
            LogUtil.error("LocalCacheStore_p_recover_3", throwable, "恢复失败，降级内存模式", this.p_dataFile);
            this.p_memoryMode = true;
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 旧版data.log兼容迁移：数据文件后缀改为.db后，已存在的旧data.log自动换名为data.db（仅当data.db不存在时）
     */
    private final void p_migrateLegacyDataFile(){
        File legacyFile = new File(this.p_dataDirectory, DATA_FILE_NAME_LEGACY);
        if(legacyFile.exists() && !this.p_dataFile.exists()){
            try{
                this.p_moveWithFallback(legacyFile, this.p_dataFile);
                LogUtil.info("LocalCacheStore_p_migrateLegacyDataFile_1", "旧版data.log已自动迁移为data.db", this.p_dataFile);
            }catch(Throwable throwable){
                LogUtil.error("LocalCacheStore_p_migrateLegacyDataFile_2", throwable, "旧版data.log迁移失败，忽略该旧文件", legacyFile);
            }
        }
        // 旧版（data.log时代）Compaction中断残留迁移：new/bak映射为新名后，由p_recoverFallbackRename按既有兜底链处理
        // （完整new换名恢复/不完整丢弃/bak兜底），避免升级后空库启动丢失旧数据
        File legacyNewFile = new File(this.p_dataDirectory, DATA_FILE_NAME_LEGACY + ".new");
        if(legacyNewFile.exists() && !this.p_dataNewFile.exists()){
            try{
                this.p_moveWithFallback(legacyNewFile, this.p_dataNewFile);
                LogUtil.info("LocalCacheStore_p_migrateLegacyDataFile_3", "旧版data.log.new已迁移为data.db.new", this.p_dataNewFile);
            }catch(Throwable throwable){
                LogUtil.error("LocalCacheStore_p_migrateLegacyDataFile_4", throwable, "旧版data.log.new迁移失败，忽略该旧文件", legacyNewFile);
            }
        }
        File legacyBakFile = new File(this.p_dataDirectory, DATA_FILE_NAME_LEGACY + ".bak");
        if(legacyBakFile.exists() && !this.p_dataBakFile.exists()){
            try{
                this.p_moveWithFallback(legacyBakFile, this.p_dataBakFile);
                LogUtil.info("LocalCacheStore_p_migrateLegacyDataFile_5", "旧版data.log.bak已迁移为data.db.bak", this.p_dataBakFile);
            }catch(Throwable throwable){
                LogUtil.error("LocalCacheStore_p_migrateLegacyDataFile_6", throwable, "旧版data.log.bak迁移失败，忽略该旧文件", legacyBakFile);
            }
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 换名兜底：data.db缺失时完成Compaction未竟事务（完整new换名、bak兜底、不完整new丢弃）
     */
    private final void p_recoverFallbackRename(){
        if(this.p_dataFile.exists()){
            return;
        }
        if(this.p_dataNewFile.exists()){
            boolean completeFlag = this.p_isFileComplete(this.p_dataNewFile);
            if(completeFlag){
                // new完整说明Compaction已force完成，换名成功与否都不再用bak（bak为更旧数据）
                try{
                    this.p_moveWithFallback(this.p_dataNewFile, this.p_dataFile);
                    LogUtil.info("LocalCacheStore_p_recoverFallbackRename_1", "完成Compaction未竟换名", this.p_dataFile);
                }catch(Throwable throwable){
                    LogUtil.error("LocalCacheStore_p_recoverFallbackRename_4", throwable, "new换名失败，将以空库启动", this.p_dataNewFile);
                }
                return;
            }
            this.p_dataNewFile.delete();
            LogUtil.warn("LocalCacheStore_p_recoverFallbackRename_2", "data.db.new不完整已丢弃", this.p_dataNewFile);
        }
        if(!this.p_dataFile.exists() && this.p_dataBakFile.exists()){
            try{
                this.p_moveWithFallback(this.p_dataBakFile, this.p_dataFile);
                LogUtil.error("LocalCacheStore_p_recoverFallbackRename_3", "log与new均不可用，用bak恢复", this.p_dataBakFile);
            }catch(Throwable throwable){
                LogUtil.error("LocalCacheStore_p_recoverFallbackRename_5", throwable, "bak换名失败，将以空库启动", this.p_dataBakFile);
            }
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 顺序扫描重放：逐条校验魔数、版本、长度上限、CRC32C；遇尾部不完整或损坏即停止
     * @param channel 待扫描通道
     * @param fillMap true表示装填内存结构；false仅做完整性检查
     * @return 最后一条完整记录的终点偏移（即应截断位置）
     */
    private final long p_replay(final FileChannel channel, final boolean fillMap){
        long offset = 0L;
        try{
            long fileSize = channel.size();
            while(true){
                // 剩余不足定长头：尾部不完整
                if(offset + LocalCacheRecord.HEADER_LENGTH > fileSize){
                    break;
                }
                ByteBuffer headerBuffer = this.p_readFully(channel, offset, LocalCacheRecord.HEADER_LENGTH);
                if(headerBuffer == null){
                    break;
                }
                int moduleLength = headerBuffer.getShort(LocalCacheRecord.OFFSET_MODULE_LENGTH) & 0xFFFF;
                int keyLength = headerBuffer.getInt(LocalCacheRecord.OFFSET_KEY_LENGTH);
                int valueLength = headerBuffer.getInt(LocalCacheRecord.OFFSET_VALUE_LENGTH);
                // 长度合法性：非负且不超上限，防伪造长度越界与OOM
                if(keyLength < 0 || keyLength > LocalCacheRecord.MAX_KEY_LENGTH){
                    break;
                }
                if(valueLength < 0 || valueLength > LocalCacheRecord.MAX_VALUE_LENGTH){
                    break;
                }
                if(moduleLength > LocalCacheRecord.MAX_MODULE_LENGTH){
                    break;
                }
                int totalLength = LocalCacheRecord.HEADER_LENGTH + moduleLength + keyLength + valueLength + LocalCacheRecord.CRC_LENGTH;
                if(offset + totalLength > fileSize){
                    break;
                }
                ByteBuffer recordBuffer = this.p_readFully(channel, offset, totalLength);
                if(recordBuffer == null){
                    break;
                }
                byte[] recordBytes = new byte[totalLength];
                recordBuffer.get(recordBytes);
                LocalCacheRecord record = LocalCacheRecord.decode(recordBytes);
                if(record == null){
                    break;
                }
                if(fillMap){
                    this.p_applyRecord(record);
                }
                offset = offset + totalLength;
            }
        }catch(Throwable throwable){
            LogUtil.error("LocalCacheStore_p_replay_1", throwable, "扫描重放异常，截断至已重放位置", String.valueOf(offset));
        }
        return offset;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 2026-08-28 本地持久化缓存框架问题修复
     * 应用单条重放记录：PUT懒装填（只装value字节不反序列化，首次get由materialize物化，杜绝启动期反序列化副作用）、DELETE移除、序号基准恢复
     * 懒装填原因：缓存值对象图可能存在readObject副作用（如Activiti流程定义内嵌TreeValueExpression重解析EL表达式，
     * 依赖尚未初始化的静态符号表），重放期反序列化会因启动时序确定性失败并每次重启重复报错
     * @param record 已通过校验的记录
     */
    private final void p_applyRecord(final LocalCacheRecord record){
        // 序号基准恢复为已见最大值
        if(record.getSequence() > this.p_sequence){
            this.p_sequence = record.getSequence();
        }
        int recordType = record.getRecordType();
        if(recordType == LocalCacheRecord.RECORD_TYPE_DELETE){
            ConcurrentHashMap<String, LocalCacheData> moduleMap = this.p_getModuleMap(record.getModuleName(), true);
            if(moduleMap != null){
                moduleMap.remove(record.getKey());
            }
            this.p_replayDeleteCount = this.p_replayDeleteCount + 1L;
            return;
        }
        // PUT：已过期记录直接跳过不装入内存（磁盘记录由后续Compaction自然清除）
        long now = System.currentTimeMillis();
        long expireAt = record.getExpireAt();
        if(expireAt > 0L && now >= expireAt){
            this.p_replayExpireCount = this.p_replayExpireCount + 1L;
            return;
        }
        // 2026-08-29 本地持久化缓存框架问题修复
        // 懒装填：value原始字节与记录格式版本原样保留（V1存V1、V2存V2，零转换零丢弃），
        // 物化按版本分派解码、Compaction按版本原样写回——避免任何转换开销字节（如V1补编码标志）
        // 把贴着16MB上限的合法业务数据推过线（重放decode已保证原始valueLength不超上限）
        byte[] lazyBytes = record.getValueBytes();
        LocalCacheData entry = new LocalCacheData();
        entry.setData(null);
        entry.setLazyValueBytes(lazyBytes);
        entry.setLazyValueVersion(record.getDataVersion());
        entry.setExpireAt(expireAt);
        entry.setSequence(record.getSequence());
        entry.setThreadId(null);
        ConcurrentHashMap<String, LocalCacheData> moduleMap = this.p_getModuleMap(record.getModuleName(), true);
        if(moduleMap != null){
            moduleMap.put(record.getKey(), entry);
        }
        this.p_replayPutCount = this.p_replayPutCount + 1L;
    }

    /**
     * 2026-08-28 本地持久化缓存框架问题修复
     * 懒条目首次读取物化：无锁快路径直接返回已物化对象；未物化条目在entry锁内解码，
     * 成功则setData并清懒字节（顺序：先setData后清字节，配合volatile保证Compaction读取不出现两者同时为null的中间态），
     * 失败则记一次ERROR、清懒字节（防并发get重复物化重复报错）、复用appendExpireDelete条件移除自清理
     * （引用比对防误删清理窗口内并发写入的新值，补写DELETE墓碑防坏记录下次重启复活）
     * @param moduleName 格式化后的模块名
     * @param key 缓存key
     * @param entry 待物化的内存条目
     * @return 物化后的对象；未物化且失败（已自清理）返回null
     */
    public final Object materialize(final String moduleName, final String key, final LocalCacheData entry){
        // 快路径：已物化或新写入条目零锁返回
        Object fastValue = entry.getData();
        if(fastValue != null){
            return fastValue;
        }
        synchronized(entry){
            // 锁内double-check：并发物化已完成则直接返回
            Object value = entry.getData();
            if(value != null){
                return value;
            }
            // 2026-08-29 本地持久化缓存框架问题修复
            // 按懒字节原始版本分派：V1纯JDK字节直接反序列化，V2按首字节编码标志解码
            byte[] lazyBytes = entry.getLazyValueBytes();
            // 懒字节为null：已被并发线程物化失败自清理，返回null（调用方回源重建）
            if(lazyBytes == null){
                return null;
            }
            int lazyVersion = entry.getLazyValueVersion();
            try{
                if(lazyVersion == 1){
                    value = p_deserialize(lazyBytes);
                }else{
                    value = p_decodeValue(lazyBytes);
                }
            }catch(Throwable throwable){
                entry.setLazyValueBytes(null);
                LogUtil.error("LocalCacheStore_materialize_1", throwable, "懒条目物化失败，自清理该条（调用方回源重建）", moduleName, key);
                this.appendExpireDelete(moduleName, key, entry);
                return null;
            }
            // 解码返回null（未知编码标志等，p_decodeValue已记日志）：同样自清理防每次get重复报错
            if(value == null){
                entry.setLazyValueBytes(null);
                LogUtil.error("LocalCacheStore_materialize_2", "懒条目物化返回空，自清理该条", moduleName, key);
                this.appendExpireDelete(moduleName, key, entry);
                return null;
            }
            entry.setData(value);
            entry.setLazyValueBytes(null);
            return value;
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 2026-09-21 tomcat停服后有时本地缓存更新问题导致重新登录
     * 新增forceNow参数：true时本条不走写缓冲，先把写缓冲整批落盘保序，再直写本条并立即force
     * 通道追加单条记录：循环写直至complete（处理partial write）；SYNC档写后立即force
     * @param forceNow true本条立即落盘并force；false走默认写缓冲路径
     * @return 成功返回1；失败返回-1
     */
    private final long p_appendRecord(final int recordType, final long sequence, final long expireAt, final String moduleName, final String key, final byte[] valueBytes, final boolean forceNow){
        if(this.p_dataChannel == null){
            LogUtil.error("LocalCacheStore_p_appendRecord_3", "日志通道未打开（可能已关闭），本条不写入", moduleName, key);
            return -1L;
        }
        try{
            byte[] recordBytes = LocalCacheRecord.encode(recordType, sequence, expireAt, moduleName, key, valueBytes);
            if(recordBytes == null){
                LogUtil.error("LocalCacheStore_p_appendRecord_1", "记录编码失败（长度超限）", moduleName, key);
                return -1L;
            }
            if(this.p_syncForce || forceNow){
                // SYNC档或按条同步刷盘：先把写缓冲整批落盘保序，再直写本条并立即force（不缓冲，零丢失）
                // SYNC档从不写缓冲、缓冲恒为空，冲缓冲动作只对forceNow条目实际生效
                if(this.p_writeBuffer.size() > 0){
                    byte[] batchBytes = this.p_swapBuffer();
                    this.p_writeBatch(batchBytes);
                }
                ByteBuffer buffer = ByteBuffer.wrap(recordBytes);
                while(buffer.hasRemaining()){
                    this.p_dataChannel.write(buffer);
                }
                this.p_dataChannel.force(false);
                return 1L;
            }
            // 非SYNC档：写入内存缓冲，满256KB立即批量落盘、否则等待周期flush/close（合并write系统调用，降低IO次数）
            this.p_writeBuffer.write(recordBytes);
            this.p_writeBufferBytes = this.p_writeBufferBytes + recordBytes.length;
            if(this.p_writeBuffer.size() >= FLUSH_BUFFER_LIMIT_BYTES){
                byte[] batchBytes = this.p_swapBuffer();
                this.p_writeBatch(batchBytes);
            }
            return 1L;
        }catch(Throwable throwable){
            LogUtil.error("LocalCacheStore_p_appendRecord_2", throwable, "通道写入异常", moduleName, key);
            return -1L;
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 取出写缓冲当前内容并复用缓冲（须在p_appendLock内调用，保证批次与记录顺序一致）
     */
    private final byte[] p_swapBuffer(){
        byte[] batchBytes = this.p_writeBuffer.toByteArray();
        this.p_writeBuffer.reset();
        // 收缩：本轮累计写入超过256KB阈值（说明缓冲被顶格扩容）时重建，常驻容量回落256KB
        //（BAOS写满翻倍扩容会使常驻达512KB甚至被大记录顶得更高，超限即重建回落）
        if(this.p_writeBufferBytes > FLUSH_BUFFER_LIMIT_BYTES){
            this.p_writeBuffer = new ByteArrayOutputStream(FLUSH_BUFFER_LIMIT_BYTES);
        }
        this.p_writeBufferBytes = 0L;
        return batchBytes;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 批量写出字节到数据通道（循环写处理partial write；须在p_appendLock内调用保证顺序）
     */
    private final void p_writeBatch(final byte[] batchBytes) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(batchBytes);
        while(buffer.hasRemaining()){
            this.p_dataChannel.write(buffer);
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 刷出写缓冲：将缓冲内记录批量写入数据通道（不force；ASYNC档后台周期任务与Compaction换文件前调用）
     */
    public final void flushBuffer(){
        if(this.p_memoryMode){
            return;
        }
        this.p_fileLock.readLock().lock();
        try{
            synchronized(this.p_appendLock){
                try{
                    if(this.p_writeBuffer.size() > 0 && this.p_dataChannel != null){
                        byte[] batchBytes = this.p_swapBuffer();
                        this.p_writeBatch(batchBytes);
                    }
                }catch(Throwable throwable){
                    LogUtil.error("LocalCacheStore_flushBuffer_1", throwable, "刷出写缓冲失败", this.p_dataFile);
                }
            }
        }finally{
            this.p_fileLock.readLock().unlock();
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 重写核心：写锁全程独占；快照内存有效条目写入new、force、关旧通道、原子换名、开新通道、删bak、清理内存
     * 失败处理：写new失败保留旧log内存不变；换名失败保留bak与new等下轮重试；尽力恢复通道，失败则降级内存模式
     * @param excludeModuleName 待排除的模块名（null表示compact保留全部）
     * @param clearAllMemory true表示清空全部模块（clear()语义）
     */
    private final void p_rewriteCore(final String excludeModuleName, final boolean clearAllMemory){
        if(this.p_memoryMode){
            // 内存模式仅清内存
            if(clearAllMemory){
                this.p_mapGlobal.clear();
            }else if(excludeModuleName != null){
                this.p_mapGlobal.remove(excludeModuleName);
            }
            return;
        }
        this.p_fileLock.writeLock().lock();
        try{
            // 拿到写锁后先判关闭标志：close()期间排队等待写锁的本方法不再执行，消除close后仍改写数据目录的竞态
            if(this.p_closed){
                return;
            }
            // 换文件前先把写缓冲刷到当前data.db（缓冲数据需先落旧文件，换名失败回退旧文件时不丢）
            this.flushBuffer();
            long now = System.currentTimeMillis();
            // 写data.db.new：重写有效条目（过期未清理条目一并清除；条目沿用原序号，保持序号基准不变）
            try{
                FileOutputStream newStream = new FileOutputStream(this.p_dataNewFile, false);
                FileChannel newChannel = newStream.getChannel();
                try{
                    Iterator<Map.Entry<String, ConcurrentHashMap<String, LocalCacheData>>> moduleIterator = this.p_mapGlobal.entrySet().iterator();
                    while(moduleIterator.hasNext()){
                        Map.Entry<String, ConcurrentHashMap<String, LocalCacheData>> moduleEntry = moduleIterator.next();
                        String moduleName = moduleEntry.getKey();
                        // 排除规则：clearAll排除全部；exclude排除指定模块；compact全保留
                        boolean excluded = false;
                        if(clearAllMemory){
                            excluded = true;
                        }else if(excludeModuleName != null && excludeModuleName.equals(moduleName)){
                            excluded = true;
                        }
                        if(excluded){
                            continue;
                        }
                        ConcurrentHashMap<String, LocalCacheData> moduleMap = moduleEntry.getValue();
                        if(moduleMap == null){
                            continue;
                        }
                        Iterator<Map.Entry<String, LocalCacheData>> entryIterator = moduleMap.entrySet().iterator();
                        while(entryIterator.hasNext()){
                            Map.Entry<String, LocalCacheData> entryMap = entryIterator.next();
                            LocalCacheData entry = entryMap.getValue();
                            if(entry == null){
                                continue;
                            }
                            // 过期未清理与纯内存条目不写入新文件（压实即顺带清除过期项；纯内存条目本就不在日志中）
                            boolean expired = entry.isExpired(now);
                            if(expired){
                                continue;
                            }
                            boolean entryMemoryOnly = entry.isMemoryOnly();
                            if(entryMemoryOnly){
                                continue;
                            }
                            // 2026-08-28 本地持久化缓存框架问题修复
                            // 条目按其来源版本原样写回（V1存V1、V2存V2），零转换零丢弃——
                            // 未物化懒条目直接用原始字节；已物化条目按来源版本重序列化（V1走纯JDK序列化不加编码标志字节），
                            // 贴着16MB上限的V1合法业务数据无论物化与否都不会因转换开销字节越线，Compaction不触发反序列化副作用
                            byte[] lazyBytes = entry.getLazyValueBytes();
                            byte[] valueBytes;
                            int recordVersion;
                            if(lazyBytes != null){
                                valueBytes = lazyBytes;
                                recordVersion = entry.getLazyValueVersion();
                            }else{
                                Object entryData = entry.getData();
                                if(entryData == null){
                                    // 物化失败已自清理的条目：跳过即从新文件清除（失败时已记过日志，此处不再刷）
                                    continue;
                                }
                                try{
                                    // 2026-09-28 本地缓存压缩重写写出非法记录导致重启清空缓存重新登录
                                    // 运行期写入条目的lazyValueVersion是默认0（只有磁盘重放装填才会赋值）：版本号必须与value编码方式配对，V1配1、V2配VERSION，禁止把0写进记录头
                                    int lazyVersion = entry.getLazyValueVersion();
                                    if(lazyVersion == 1){
                                        recordVersion = 1;
                                        valueBytes = p_serialize(entryData);
                                    }else{
                                        recordVersion = LocalCacheRecord.VERSION;
                                        valueBytes = p_encodeValue(entryData);
                                    }
                                }catch(Throwable throwable){
                                    String rewriteKey = entryMap.getKey();
                                    LogUtil.error("LocalCacheStore_p_rewriteCore_1", throwable, "重写时序列化失败，该条不写入新文件", moduleName, rewriteKey);
                                    continue;
                                }
                            }
                            byte[] recordBytes = LocalCacheRecord.encode(recordVersion, LocalCacheRecord.RECORD_TYPE_PUT, entry.getSequence(), entry.getExpireAt(), moduleName, entryMap.getKey(), valueBytes);
                            if(recordBytes == null){
                                String encodeKey = entryMap.getKey();
                                LogUtil.error("LocalCacheStore_p_rewriteCore_2", "重写时编码失败，该条不写入新文件", moduleName, encodeKey);
                                continue;
                            }
                            ByteBuffer buffer = ByteBuffer.wrap(recordBytes);
                            while(buffer.hasRemaining()){
                                newChannel.write(buffer);
                            }
                        }
                    }
                    newChannel.force(false);
                }finally{
                    newChannel.close();
                    newStream.close();
                }
            }catch(Throwable newThrowable){
                // 写new失败（磁盘满等）：删除不完整new、保留旧log、内存不变
                this.p_dataNewFile.delete();
                LogUtil.error("LocalCacheStore_p_rewriteCore_5", newThrowable, "写new失败，已删除不完整new，保留旧数据文件与内存不变", this.p_dataNewFile);
                return;
            }
            // 换名前必须先关闭旧通道（Windows不允许移动被占用文件）
            this.p_closeDataChannel();
            this.p_dataBakFile.delete();
            this.p_moveWithFallback(this.p_dataFile, this.p_dataBakFile);
            this.p_moveWithFallback(this.p_dataNewFile, this.p_dataFile);
            this.p_openDataChannel();
            this.p_dataBakFile.delete();
            // 重写成功后清理内存
            if(clearAllMemory){
                this.p_mapGlobal.clear();
            }else if(excludeModuleName != null){
                this.p_mapGlobal.remove(excludeModuleName);
            }
        }catch(Throwable throwable){
            LogUtil.error("LocalCacheStore_p_rewriteCore_3", throwable, "换名失败，保留bak与new下轮重试，内存不变", this.p_dataFile);
            // 尽力恢复通道可用（换名可能已完成）
            try{
                if(this.p_dataChannel == null || !this.p_dataChannel.isOpen()){
                    if(this.p_dataNewFile.exists() && !this.p_dataFile.exists()){
                        this.p_moveWithFallback(this.p_dataNewFile, this.p_dataFile);
                    }
                    this.p_openDataChannel();
                }
            }catch(Throwable recoverThrowable){
                LogUtil.error("LocalCacheStore_p_rewriteCore_4", recoverThrowable, "换名失败后恢复通道失败，降级内存模式", this.p_dataFile);
                this.p_memoryMode = true;
            }
        }finally{
            this.p_fileLock.writeLock().unlock();
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 以追加模式打开活动日志通道
     */
    private final void p_openDataChannel() throws IOException {
        if(this.p_closed){
            return;
        }
        FileOutputStream dataStream = new FileOutputStream(this.p_dataFile, true);
        this.p_dataChannel = dataStream.getChannel();
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 关闭活动日志通道（换名前必须调用，Windows文件占用约束）
     */
    private final void p_closeDataChannel() throws IOException {
        if(this.p_dataChannel != null){
            this.p_dataChannel.close();
            this.p_dataChannel = null;
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 关闭进程锁通道（通道关闭即释放文件锁）
     */
    private final void p_closeLockChannel(){
        try{
            if(this.p_lockChannel != null){
                this.p_lockChannel.close();
                this.p_lockChannel = null;
            }
        }catch(Throwable throwable){
            LogUtil.error("LocalCacheStore_p_closeLockChannel_1", throwable, "关闭进程锁通道失败", this.p_lockFile);
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 获取模块子表：不存在时按需创建（并发安全双重检查）
     * @param moduleName 模块名
     * @param createIfAbsent true表示不存在时创建
     * @return 模块子表；createIfAbsent为false且不存在时返回null
     */
    private final ConcurrentHashMap<String, LocalCacheData> p_getModuleMap(final String moduleName, final boolean createIfAbsent){
        ConcurrentHashMap<String, LocalCacheData> moduleMap = this.p_mapGlobal.get(moduleName);
        if(moduleMap != null){
            return moduleMap;
        }
        if(!createIfAbsent){
            return null;
        }
        synchronized(this.p_mapGlobal){
            moduleMap = this.p_mapGlobal.get(moduleName);
            if(moduleMap == null){
                moduleMap = new ConcurrentHashMap<String, LocalCacheData>(1024);
                this.p_mapGlobal.put(moduleName, moduleMap);
            }
            return moduleMap;
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 从指定偏移循环读满指定长度（处理partial read）
     * @return 读满的缓冲区；文件不足或异常返回null
     */
    private final ByteBuffer p_readFully(final FileChannel channel, final long position, final int length){
        try{
            ByteBuffer buffer = ByteBuffer.allocate(length);
            int total = 0;
            while(total < length){
                int read = channel.read(buffer, position + total);
                if(read < 0){
                    return null;
                }
                total = total + read;
            }
            buffer.flip();
            return buffer;
        }catch(Throwable throwable){
            LogUtil.error("LocalCacheStore_p_readFully_1", throwable, "通道读取异常", String.valueOf(position), String.valueOf(length));
            return null;
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 判断文件是否整体校验完整（每条记录均通过魔数、版本、CRC、长度校验且无尾部残余）
     */
    private final boolean p_isFileComplete(final File targetFile){
        try{
            RandomAccessFile randomFile = new RandomAccessFile(targetFile, "r");
            FileChannel channel = randomFile.getChannel();
            try{
                long lastGoodOffset = this.p_replay(channel, false);
                long fileSize = channel.size();
                boolean flag = lastGoodOffset == fileSize;
                return flag;
            }finally{
                channel.close();
                randomFile.close();
            }
        }catch(Throwable throwable){
            LogUtil.error("LocalCacheStore_p_isFileComplete_1", throwable, "完整性检查异常，视为不完整", targetFile);
            return false;
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 换名：优先ATOMIC_MOVE，不支持或失败时回退REPLACE_EXISTING普通移动
     */
    private final void p_moveWithFallback(final File sourceFile, final File targetFile) throws IOException {
        try{
            Files.move(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
        }catch(Throwable atomicThrowable){
            Files.move(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * value编码（V2快速路径）：String/Long/Integer/byte[]免JDK序列化直接写原始字节，
     * 首字节为编码标志（VALUE_CODE_*）；其他对象走JDK序列化（标志1）
     */
    private static final byte[] p_encodeValue(final Object value) throws IOException {
        // String：原始UTF-8字节
        if(value instanceof String){
            String stringValue = (String) value;
            byte[] payloadBytes = stringValue.getBytes(StandardCharsets.UTF_8);
            byte[] valueBytes = new byte[payloadBytes.length + 1];
            valueBytes[0] = (byte) LocalCacheRecord.VALUE_CODE_STRING;
            System.arraycopy(payloadBytes, 0, valueBytes, 1, payloadBytes.length);
            return valueBytes;
        }
        // Long：八字节大端
        if(value instanceof Long){
            Long longValue = (Long) value;
            byte[] valueBytes = new byte[9];
            valueBytes[0] = (byte) LocalCacheRecord.VALUE_CODE_LONG;
            ByteBuffer buffer = ByteBuffer.wrap(valueBytes, 1, 8);
            buffer.putLong(longValue.longValue());
            return valueBytes;
        }
        // Integer：四字节大端
        if(value instanceof Integer){
            Integer intValue = (Integer) value;
            byte[] valueBytes = new byte[5];
            valueBytes[0] = (byte) LocalCacheRecord.VALUE_CODE_INTEGER;
            ByteBuffer buffer = ByteBuffer.wrap(valueBytes, 1, 4);
            buffer.putInt(intValue.intValue());
            return valueBytes;
        }
        // byte[]：原样字节
        if(value instanceof byte[]){
            byte[] payloadBytes = (byte[]) value;
            byte[] valueBytes = new byte[payloadBytes.length + 1];
            valueBytes[0] = (byte) LocalCacheRecord.VALUE_CODE_BYTES;
            System.arraycopy(payloadBytes, 0, valueBytes, 1, payloadBytes.length);
            return valueBytes;
        }
        // 通用对象：JDK序列化
        byte[] payloadBytes = p_serialize(value);
        byte[] valueBytes = new byte[payloadBytes.length + 1];
        valueBytes[0] = (byte) LocalCacheRecord.VALUE_CODE_JDK;
        System.arraycopy(payloadBytes, 0, valueBytes, 1, payloadBytes.length);
        return valueBytes;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * value解码（V2快速路径）：按首字节编码标志分派，与p_encodeValue互逆
     */
    private static final Object p_decodeValue(final byte[] valueBytes) throws IOException, ClassNotFoundException {
        if(valueBytes == null || valueBytes.length < 1){
            return null;
        }
        int valueCode = valueBytes[0] & 0xFF;
        int payloadLength = valueBytes.length - 1;
        if(valueCode == LocalCacheRecord.VALUE_CODE_STRING){
            String value = new String(valueBytes, 1, payloadLength, StandardCharsets.UTF_8);
            return value;
        }
        if(valueCode == LocalCacheRecord.VALUE_CODE_LONG){
            ByteBuffer buffer = ByteBuffer.wrap(valueBytes, 1, payloadLength);
            Long value = buffer.getLong();
            return value;
        }
        if(valueCode == LocalCacheRecord.VALUE_CODE_INTEGER){
            ByteBuffer buffer = ByteBuffer.wrap(valueBytes, 1, payloadLength);
            Integer value = buffer.getInt();
            return value;
        }
        if(valueCode == LocalCacheRecord.VALUE_CODE_BYTES){
            byte[] value = new byte[payloadLength];
            System.arraycopy(valueBytes, 1, value, 0, payloadLength);
            return value;
        }
        if(valueCode == LocalCacheRecord.VALUE_CODE_JDK){
            byte[] payloadBytes = new byte[payloadLength];
            System.arraycopy(valueBytes, 1, payloadBytes, 0, payloadLength);
            Object value = p_deserialize(payloadBytes);
            return value;
        }
        // 未知编码标志：视为损坏丢弃并记错误日志（禁止静默丢弃）
        LogUtil.error("LocalCacheStore_p_decodeValue_1", "未知value编码标志，丢弃该条", String.valueOf(valueCode));
        return null;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * JDK序列化（项目RedisUtil.getContent同模式）
     */
    private static final byte[] p_serialize(final Object value) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
        try{
            objectStream.writeObject(value);
        }finally{
            objectStream.close();
        }
        byte[] valueBytes = byteStream.toByteArray();
        return valueBytes;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * JDK反序列化（仅反序列化本类写入的数据）
     */
    private static final Object p_deserialize(final byte[] valueBytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(valueBytes);
        ObjectInputStream objectStream = new ObjectInputStream(byteStream);
        try{
            Object value = objectStream.readObject();
            return value;
        }finally{
            objectStream.close();
        }
    }

}
