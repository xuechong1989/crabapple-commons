package xc.crabapple.ssm.redis;

import redis.clients.jedis.util.Pool;

/**
 * 2019-04-29 2.0.0
 */
public final class RedisConfigBean extends Object{

    // 三种模式
    public static final String SINGLETON = "singleton";  // 单机模式，默认方式
    public static final String SENTINEL = "sentinel";  // 哨兵模式
    public static final String CLUSTER = "cluster";  // 集群模式，目前不支持
    // jedis原生属性
    private String password = "no";  // 密码，no作为内部保留字，若为空字符串或no则表示不设置密码
    private Integer maxTotal = 1000000;  // 资源池中最大连接数，设为0表示无限制 默认值8 建议值
    private Integer maxIdle = 1000;  // 资源池允许最大空闲的连接数 默认值8 建议值
    private Integer minIdle = 100;  // 资源池确保最少空闲的连接数 默认值0 建议值
    private Integer timeout = Integer.MAX_VALUE;  // 尝试发起连接的超时时间，单位毫秒，默认为60000毫秒
    private Long maxWait = Long.MAX_VALUE;  // 当资源池连接用尽后，调用者的最大等待时间，单位毫秒
    private String blockWhenExhausted = "yes";  // 当资源池用尽后，调用者是否要等待。只有当为true时，maxWait才会生效，默认值true，建议使用默认值
    private String testOnBorrow = "yes";  // yes,no 向资源池借用连接时是否做连接有效性检测(ping)，无效连接会被移除 默认值 false 业务量很大时候建议设置为false(多一次ping的开销)。
    private String testOnReturn = "yes";  // yes,no 向资源池归还连接时是否做连接有效性检测(ping)，无效连接会被移除 默认值 false 业务量很大时候建议设置为false(多一次ping的开销)。
    private String testWhileIdle = "yes";  // yes,no 是否开启空闲资源监测, 默认值false, 建议值 true
    private String jmxEnabled = "yes";  // yes,no 是否开启jmx监控，可用于监控 默认值 true 建议开启，但应用本身也要开启
    private Long timeBetweenEvictionRuns = 30 * 1000L;  // 空闲资源的检测周期(单位为毫秒), 默认值30000, 建议设置，周期自行选择，也可使用JedisPoolConfig中的配置
    private Long minEvictableIdleTime = 30 * 1000L;  // 资源池中资源最小空闲时间(单位为毫秒)，达到此值后空闲资源将被移除, 默认值60000，可根据自身业务决定，大部分默认值即可，也可使用JeidsPoolConfig中的配置
    private Integer numTestsPerEvictionRun = 50;  // 做空闲资源检测时，每次的采样数， 默认值3， 可根据自身应用连接数进行微调,如果设置为-1，就是对所有连接做空闲监测
    // 自定义属性
    private String serverId = "crabapple";  // daoRedi中的比当前变量要优先级高
    private String hosts = "";  // 服务器地址，IP地址:哨兵端口（注意不是服务实例端口），格式如ip:port,ip:port,...，若是SINGLETON模式则只取第一个地址，逗号分隔，如192.168.1.101:6379,192.168.1.102:6379,192.168.1.103:6380
    private String model = SINGLETON;  // 模式类型，singleton, sentinel
    private Long reconnectIntervalMillis = 5000L;  // 两次重新连接之间的间隔时间，单位毫秒
    private Integer reconnectMaxCount = 3;  // 最大重连次数
    private Pool pool;  // 当前所用的连接池类型
    private String cipherType = "no";  // 加密方式，目前支持两种方式：sm4 / aes128。为空或no表示不加密
    private String cipherKey = "";  // 看加密方式对应秘钥，必须为16位ASCII字符串；默认不提供密钥

    public RedisConfigBean(){

    }

    @Override
    public String toString() {
        return "RedisConfigBean{" +
                "password='" + password + '\'' +
                ", maxTotal=" + maxTotal +
                ", maxIdle=" + maxIdle +
                ", minIdle=" + minIdle +
                ", timeout=" + timeout +
                ", maxWait=" + maxWait +
                ", blockWhenExhausted='" + blockWhenExhausted + '\'' +
                ", testOnBorrow='" + testOnBorrow + '\'' +
                ", testOnReturn='" + testOnReturn + '\'' +
                ", testWhileIdle='" + testWhileIdle + '\'' +
                ", jmxEnabled='" + jmxEnabled + '\'' +
                ", timeBetweenEvictionRuns=" + timeBetweenEvictionRuns +
                ", minEvictableIdleTime=" + minEvictableIdleTime +
                ", numTestsPerEvictionRun=" + numTestsPerEvictionRun +
                ", serverId='" + serverId + '\'' +
                ", hosts='" + hosts + '\'' +
                ", model='" + model + '\'' +
                ", reconnectIntervalMillis=" + reconnectIntervalMillis +
                ", reconnectMaxCount=" + reconnectMaxCount +
                ", pool=" + pool +
                ", cipherType=" + cipherType +
                ", cipherKey=" + cipherKey +
                '}';
    }

    public String getPassword() { return password; }
    public void setPassword(final String password) { this.password = password; }
    public Integer getMaxTotal() { return maxTotal; }
    public void setMaxTotal(final Integer maxTotal) { this.maxTotal = maxTotal; }
    public Integer getMaxIdle() { return maxIdle; }
    public void setMaxIdle(final Integer maxIdle) { this.maxIdle = maxIdle; }
    public Integer getMinIdle() { return minIdle; }
    public void setMinIdle(final Integer minIdle) { this.minIdle = minIdle; }
    public Long getMaxWait() { return maxWait; }
    public void setMaxWait(final Long maxWait) { this.maxWait = maxWait; }
    public String getBlockWhenExhausted() { return blockWhenExhausted; }
    public void setBlockWhenExhausted(final String blockWhenExhausted) { this.blockWhenExhausted = blockWhenExhausted; }
    public String getTestOnReturn() { return testOnReturn; }
    public void setTestOnReturn(final String testOnReturn) { this.testOnReturn = testOnReturn; }
    public String getTestWhileIdle() { return testWhileIdle; }
    public void setTestWhileIdle(final String testWhileIdle) { this.testWhileIdle = testWhileIdle; }
    public String getJmxEnabled() { return jmxEnabled; }
    public void setJmxEnabled(final String jmxEnabled) { this.jmxEnabled = jmxEnabled; }
    public Long getTimeBetweenEvictionRuns() { return timeBetweenEvictionRuns; }
    public void setTimeBetweenEvictionRuns(final Long timeBetweenEvictionRuns) { this.timeBetweenEvictionRuns = timeBetweenEvictionRuns; }
    public Long getMinEvictableIdleTime() { return minEvictableIdleTime; }
    public void setMinEvictableIdleTime(final Long minEvictableIdleTime) { this.minEvictableIdleTime = minEvictableIdleTime; }
    public Integer getNumTestsPerEvictionRun() { return numTestsPerEvictionRun; }
    public void setNumTestsPerEvictionRun(final Integer numTestsPerEvictionRun) { this.numTestsPerEvictionRun = numTestsPerEvictionRun; }
    public Integer getTimeout() { return timeout; }
    public void setTimeout(final Integer timeout) { this.timeout = timeout; }
    public String getTestOnBorrow() { return testOnBorrow; }
    public void setTestOnBorrow(final String testOnBorrow) { this.testOnBorrow = testOnBorrow; }
    public String getServerId() { return serverId; }
    public void setServerId(final String serverId) { this.serverId = serverId; }
    public String getHosts() { return hosts; }
    public void setHosts(final String hosts) { this.hosts = hosts; }
    public String getModel() { return model; }
    public void setModel(final String model) { this.model = model; }
    public Long getReconnectIntervalMillis() { return reconnectIntervalMillis; }
    public void setReconnectIntervalMillis(final Long reconnectIntervalMillis) { this.reconnectIntervalMillis = reconnectIntervalMillis; }
    public Integer getReconnectMaxCount() { return reconnectMaxCount; }
    public void setReconnectMaxCount(final Integer reconnectMaxCount) { this.reconnectMaxCount = reconnectMaxCount; }
    public Pool getPool() { return pool; }
    public void setPool(final Pool pool) { this.pool = pool; }
    public String getCipherType() { return cipherType; }
    public void setCipherType(final String cipherType) { this.cipherType = cipherType; }
    public String getCipherKey() { return cipherKey; }
    public void setCipherKey(final String cipherKey) { this.cipherKey = cipherKey; }

}
