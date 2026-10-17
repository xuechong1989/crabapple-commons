package xc.crabapple.ssm.mq.redisson;

import xc.crabapple.core.log.LogUtil;

/**
 * 2020-12-12 7.1.0
 * 集成了本地锁与分布式锁(基于Redisson)，具体使用哪种由配置决定
 */
public final class LockUtil {

    private static String p_type = "";

    /**
     * 2020-12-12 7.1.0
     */
    public static final void init(){
        p_type = "local";
        //本地锁
        if("local".equals(p_type)){
            LockLocalUtil.init();
            return;
        }
        //分布式锁（redis的singleton模式）
        if("singleton".equals(p_type)){
            LockRedisUtil.singletonInit();
            return;
        }
        //分布式锁（redis的sentinel模式）
        if("sentinel".equals(p_type)){
            LockRedisUtil.sentinelInit();
            return;
        }
        //分布式锁（redis的cluster模式）
        if("cluster".equals(p_type)){
            LockRedisUtil.clusterInit();
            return;
        }
        //
        LogUtil.print("LockUtil_init_1", "同步锁 ...... 禁用");
        return;
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
     * @param timeout 超时时间，单位秒
     */
    public static final String lock(final String id, final long timeout){
        String localId = id;
        //本地锁
        if("local".equals(p_type)){
            localId = LockLocalUtil.lock(localId, timeout);
            return localId;
        }
        //分布式锁
        if("singleton".equals(p_type) || "sentinel".equals(p_type) || "cluster".equals(p_type)){
            localId = LockRedisUtil.lock(localId, timeout);
            return localId;
        }
        //
        LogUtil.error("LockUtil_lock_1", "同步锁禁用", p_type);
        return localId;
    }

    /**
     * 2020-12-12 7.1.0
     * @param id 一般一个方法用一个相同的id
     */
    public static final void unlock(final String id){
        //本地锁
        if("local".equals(p_type)){
            LockLocalUtil.unlock(id);
            return;
        }
        //分布式锁
        if("singleton".equals(p_type) || "sentinel".equals(p_type) || "cluster".equals(p_type)){
            LockRedisUtil.unlock(id);
            return;
        }
        //
        LogUtil.error("LockUtil_unlock_1", "同步锁禁用", p_type);
    }

}
