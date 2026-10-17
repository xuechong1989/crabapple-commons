package xc.crabapple.ssm.mq.redisson;

import java.util.concurrent.TimeUnit;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
import xc.crabapple.core.collection.ObjectUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;

/**
 * 2020-12-12 7.1.0
 * 分布式锁，基于Redisson
 * 采用redisson的WatchDog-看门狗，来解决死锁问题
 */
public final class LockRedisUtil {

    private static RedissonClient p_redissonClient;
    private static String p_hosts = "127.0.0.1:6379";
    private static String p_password = "";
    private static String p_masterName = "crabapple";

    public static final void setHosts(final String hosts){
        p_hosts = hosts;
    }

    public static final void setPassword(final String password){
        p_password = password;
    }

    public static final void setMasterName(final String masterName){
        p_masterName = masterName;
    }

    /**
     * 2020-12-12 7.1.0
     * 单例模式
     */
    public static final void singletonInit(){
        LogUtil.print("LockRedisUtil_singletonInit_1", "同步锁 ...... 分布式锁---单例模式---singleton");
        Config config = new Config();
        SingleServerConfig singleServerConfig = config.useSingleServer();
        String[] array = StringUtil.split(p_hosts, ",");
        singleServerConfig.setAddress("redis://" + array[0]);
        if(!StringUtil.isBlank(p_password)){
            singleServerConfig.setPassword(p_password);
        }
        try{
            p_redissonClient = Redisson.create(config);
        }catch(Throwable throwable){
            LogUtil.error("LockRedisUtil_singletonInit_t1", throwable);
            throw new RuntimeException("Redisson初始化失败", throwable);
        }
    }

    /**
     * 2020-12-12 7.1.0
     * 哨兵模式
     */
    public static final void sentinelInit(){
        LogUtil.print("LockRedisUtil_sentinelInit_1", "同步锁 ...... 分布式锁---哨兵模式---sentinel");
        Config config = new Config();
        SentinelServersConfig sentinelServersConfig = config.useSentinelServers();
        String[] array = StringUtil.split(p_hosts, ",");
        if(array.length <= 0){
            LogUtil.error("LockRedisUtil_sentinelInit_2", "xc_crabapple_redis_hosts错误，分布式锁初始化失败");
            return;
        }
        for(String sentinelHost : array){
            if(StringUtil.isBlank(sentinelHost)){
                continue;
            }
            sentinelServersConfig.addSentinelAddress("redis://" + sentinelHost);
        }
        sentinelServersConfig.setMasterName(p_masterName);
        if(!StringUtil.isBlank(p_password)){
            sentinelServersConfig.setPassword(p_password);
        }
        sentinelServersConfig.setReadMode(ReadMode.SLAVE);
        sentinelServersConfig.setMasterConnectionPoolSize(300);  //设置对于master节点的连接池中连接数最大为300
        sentinelServersConfig.setIdleConnectionTimeout(Integer.MAX_VALUE);  //如果当前连接池里的连接数量超过了最小空闲连接数，而同时有连接空闲时间超过了该数值，那么这些连接将会自动被关闭，并从连接池里去掉。时间单位是毫秒。
        sentinelServersConfig.setConnectTimeout(Integer.MAX_VALUE);  //同任何节点建立连接时的等待超时。时间毫秒
        sentinelServersConfig.setTimeout(Integer.MAX_VALUE);  //等待节点回复命令的时间。该时间从命令发送成功时开始计时
        sentinelServersConfig.setPingConnectionInterval(30000);
        sentinelServersConfig.setRetryInterval(5000);  //在一条命令发送失败以后，等待重试发送的时间间隔。单位毫秒
        try{
            p_redissonClient = Redisson.create(config);
        }catch(Throwable throwable){
            LogUtil.error("LockRedisUtil_sentinelInit_t1", throwable);
            throw new RuntimeException("Redisson初始化失败", throwable);
        }
    }

    /**
     * 2020-12-12 7.1.0
     * 集群模式
     */
    public static final void clusterInit(){
        LogUtil.print("LockRedisUtil_clusterInit_1", "同步锁 ...... 分布式锁---集群模式---cluster");
        Config config = new Config();
        ClusterServersConfig clusterServersConfig = config.useClusterServers();
        String[] array = StringUtil.split(p_hosts, ",");
        if(array.length <= 0){
            LogUtil.error("LockRedisUtil_clusterInit_2", "xc_crabapple_redis_hosts错误，分布式锁初始化失败");
            return;
        }
        for(String clusterHost : array){
            if(StringUtil.isBlank(clusterHost)){
                continue;
            }
            clusterServersConfig.addNodeAddress("redis://" + clusterHost);
        }
        if(!StringUtil.isBlank(p_password)){
            clusterServersConfig.setPassword(p_password);
        }
        clusterServersConfig.setMasterConnectionPoolSize(500);  //设置对于master节点的连接池中连接数最大为500
        clusterServersConfig.setIdleConnectionTimeout(Integer.MAX_VALUE);  //如果当前连接池里的连接数量超过了最小空闲连接数，而同时有连接空闲时间超过了该数值，那么这些连接将会自动被关闭，并从连接池里去掉。时间单位是毫秒。
        clusterServersConfig.setConnectTimeout(Integer.MAX_VALUE);  //同任何节点建立连接时的等待超时。时间毫秒
        clusterServersConfig.setTimeout(Integer.MAX_VALUE);  //等待节点回复命令的时间。该时间从命令发送成功时开始计时
        clusterServersConfig.setPingConnectionInterval(30000);
        clusterServersConfig.setRetryInterval(1500);  //在一条命令发送失败以后，等待重试发送的时间间隔。单位毫秒
        try{
            p_redissonClient = Redisson.create(config);
        }catch(Throwable throwable){
            LogUtil.error("LockRedisUtil_clusterInit_t1", throwable);
            throw new RuntimeException("Redisson初始化失败", throwable);
        }
    }

    /**
     * 2020-12-12 7.1.0
     * @param id 一般一个方法用一个相同的id
     */
    public static final String lock(final String id){
        final String tmpId = lock(id, 0);
        return tmpId;
    }

    /**
     * 2020-12-12 7.1.0
     * @param id 一般一个方法用一个相同的id
     * @param timeout 超时时间，单位毫秒
     * @return 返回id
     */
    public static final String lock(final String id, final long timeout){
        if(StringUtil.isBlank(id)){
            LogUtil.error("LockRedisUtil_lock_1", "id_is_null");
            return id;
        }
        long localTimeout = timeout;
        if(localTimeout <= 0){
            Integer lockTimeout = 3000;
            if(lockTimeout == null || ObjectUtil.elts(lockTimeout, 0)){
                lockTimeout = 60000;
            }
            localTimeout = lockTimeout;
        }
        //
        try{
            RLock rLock = p_redissonClient.getLock(id);
            rLock.tryLock(localTimeout, TimeUnit.MILLISECONDS);
            return id;
        }catch(Throwable throwable){
            String msg = throwable.getMessage();
            if(StringUtil.isBlank(msg)){
                return id;
            }
            if(StringUtil.containOf(msg, "WRONGTYPE Operation against a key holding the wrong kind of value")){
                LogUtil.error("LockRedisUtil_lock_t1", throwable, "加锁失败，很可能由于redis中已存在重名的id", id, localTimeout);
                return id;
            }
            LogUtil.error("LockRedisUtil_lock_t1", throwable, id, localTimeout);
            return id;
        }
    }

    /**
     * 2020-12-12 7.1.0
     * @param id 一般一个方法用一个相同的id
     */
    public static final void unlock(final String id){
        try{
            RLock rLock = p_redissonClient.getLock(id);
            boolean flag = rLock.isLocked();
            if(flag){
                rLock.unlock();
            }
        }catch(Throwable throwable){
            String msg = throwable.getMessage();
            if(StringUtil.isBlank(msg)){
                return;
            }
            if(StringUtil.containOf(msg, "attempt to unlock lock, not locked by current thread by node id")){
                LogUtil.error("LockRedisUtil_unlock_t1", throwable, "任务执行时间超过了指定的超时时间");
                return;
            }
            LogUtil.error("LockRedisUtil_unlock_t2", throwable);
        }
    }

}
