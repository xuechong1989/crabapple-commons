package xc.crabapple.core.cache;

/**
 * 2026-08-20 构建替代redis的本地API持久化缓存框架
 * 持久化缓存配置类：init(LocalCacheConfigBean)配置化入口的载体，null字段使用默认值
 */
public final class LocalCacheConfigBean {

    private String dataPath;               // 数据目录，null时取{user.dir}\persistent-cache
    private Long flushIntervalMillis;      // 刷盘档位：-1每写force、0仅close时force、正数为周期毫秒（默认1000）
    private Integer clearCountMax;         // 每模块每次TTL清理最多清除的对象数量（默认4096，与LocalCacheUtil一致）
    private Long clearIntervalSeconds;     // TTL后台清理周期秒（默认60）
    private Long maxFileSizeBytes;         // Compaction触发阈值字节（默认64MB）
    private Long flushBufferMillis;        // 写缓冲周期刷出毫秒（默认1000；ASYNC档缓冲最迟落盘周期，PERIODIC档随force周期刷出，SYNC档不缓冲）

    public String getDataPath() {
        String value = this.dataPath;
        return value;
    }

    public void setDataPath(final String dataPath) {
        this.dataPath = dataPath;
    }

    public Long getFlushIntervalMillis() {
        Long value = this.flushIntervalMillis;
        return value;
    }

    public void setFlushIntervalMillis(final Long flushIntervalMillis) {
        this.flushIntervalMillis = flushIntervalMillis;
    }

    public Integer getClearCountMax() {
        Integer value = this.clearCountMax;
        return value;
    }

    public void setClearCountMax(final Integer clearCountMax) {
        this.clearCountMax = clearCountMax;
    }

    public Long getClearIntervalSeconds() {
        Long value = this.clearIntervalSeconds;
        return value;
    }

    public void setClearIntervalSeconds(final Long clearIntervalSeconds) {
        this.clearIntervalSeconds = clearIntervalSeconds;
    }

    public Long getMaxFileSizeBytes() {
        Long value = this.maxFileSizeBytes;
        return value;
    }

    public void setMaxFileSizeBytes(final Long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public Long getFlushBufferMillis() {
        Long value = this.flushBufferMillis;
        return value;
    }

    public void setFlushBufferMillis(final Long flushBufferMillis) {
        this.flushBufferMillis = flushBufferMillis;
    }

}
