package xc.crabapple.core.cache;

/**
 * 2026-08-20 构建替代redis的本地API持久化缓存框架
 * 2026-08-28 本地持久化缓存框架问题修复
 * 持久化缓存内存条目：存储实际对象、绝对过期时间、日志序号、写入线程id
 */
public final class LocalCacheData {

    // 2026-08-28 本地持久化缓存框架问题修复
    // data与lazyValueBytes改为volatile：物化线程先setData后清lazyValueBytes，
    // Compaction与并发读取线程按"先读lazyValueBytes再读data"顺序访问时由volatile保证不出现两者同时为null的中间态误判
    private volatile Object data;      // 调用者存入的实际对象；重放恢复的懒条目在首次get物化前为null
    private volatile byte[] lazyValueBytes;      // 懒装填value字节（重放时V1/V2原始字节原样保留，不做任何转换丢弃）；非空表示尚未物化，物化成功或失败自清理后置null
    private volatile int lazyValueVersion;      // 懒字节记录格式版本（1或2）：物化按版本分派解码方式，Compaction按版本原样写回；与lazyValueBytes同批发布
    private long expireAt;    // 过期时间epoch毫秒，0表示永久有效
    private long sequence;    // 日志记录全局单调递增序号
    private Long threadId;    // 写入时的线程id，重启恢复的条目为null
    private boolean memoryOnly;   // 纯内存条目标志：true不落盘、崩溃即丢、重启不恢复

    public LocalCacheData(){

    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 判断条目在指定时间点是否已过期（仅now >= expireAt判定过期，系统时间回拨只会延迟过期）
     * @param now 当前时间epoch毫秒
     * @return true表示已过期
     */
    public boolean isExpired(final long now){
        if(this.expireAt <= 0L){
            return false;
        }
        boolean flag = now >= this.expireAt;
        return flag;
    }

    public Object getData() {
        Object value = this.data;
        return value;
    }

    public void setData(final Object data) {
        this.data = data;
    }

    /**
     * 2026-08-28 本地持久化缓存框架问题修复
     * 获取懒装填value字节（重放时V1/V2原始字节，无任何转换）；null表示已物化或已失败自清理
     */
    public byte[] getLazyValueBytes() {
        byte[] value = this.lazyValueBytes;
        return value;
    }

    public void setLazyValueBytes(final byte[] lazyValueBytes) {
        this.lazyValueBytes = lazyValueBytes;
    }

    /**
     * 2026-08-29 本地持久化缓存框架问题修复
     * 获取懒字节记录格式版本（1或2）；0表示非懒条目
     */
    public int getLazyValueVersion() {
        int value = this.lazyValueVersion;
        return value;
    }

    public void setLazyValueVersion(final int lazyValueVersion) {
        this.lazyValueVersion = lazyValueVersion;
    }

    public long getExpireAt() {
        long value = this.expireAt;
        return value;
    }

    public void setExpireAt(final long expireAt) {
        this.expireAt = expireAt;
    }

    public long getSequence() {
        long value = this.sequence;
        return value;
    }

    public void setSequence(final long sequence) {
        this.sequence = sequence;
    }

    public Long getThreadId() {
        Long value = this.threadId;
        return value;
    }

    public void setThreadId(final Long threadId) {
        this.threadId = threadId;
    }

    public boolean isMemoryOnly() {
        boolean value = this.memoryOnly;
        return value;
    }

    public void setMemoryOnly(final boolean memoryOnly) {
        this.memoryOnly = memoryOnly;
    }

}
