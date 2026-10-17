package xc.crabapple.ssm.cache;

import xc.crabapple.core.cache.LocalCacheUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.timer.TimerUtil;
import xc.crabapple.ssm.redis.RedisConfigBean;
import xc.crabapple.ssm.redis.RedisDao;
import xc.crabapple.ssm.redis.RedisUtil;

import java.io.Serializable;
import java.util.List;

/**
 * 2025-01-26
 *  缓存工具类，集成本地缓存与Redis缓存
 */
public class CacheUtil {

    public static final String LOCAL = "local";  // 本地存储
    public static final String REDIS = "redis";  // redis存储
    // 是否启用redis
    private static String p_redisEnable = "no";
    private static String p_redisServerId = "crabapple";

    /**
     * 2025-01-26
     * @param localClearInterval 定时清理间隔时间，默认为600秒(即10分钟)，单位秒
     * @param redisEnable
     * @param redisServerId
     * @param redisHost
     * @param redisModel
     * @param redisPassword
     * @param redisReconnectMaxCount
     * @param redisCipherType
     * @param redisCipherKey
     */
    public static final void init(final Long localClearInterval, final String redisEnable, final String redisServerId, final String redisHost, final String redisModel, final String redisPassword, final Integer redisReconnectMaxCount, final String redisCipherType, final String redisCipherKey){
        if("yes".equals(redisEnable)){
            p_redisInit(redisServerId, redisHost, redisModel, redisPassword, redisReconnectMaxCount, redisCipherType, redisCipherKey);
        }
        // 2026-08-20 构建替代redis的本地API持久化缓存框架
        // LocalCacheUtil.init已删除moduleNameArray参数（原调用传null数组等价于默认模块）
        LocalCacheUtil.init(4096, localClearInterval);
        LogUtil.print("CacheUtil_init_1", "本地缓存初始化完成");
    }

    /**
     * 2025-01-26
     * @param redisServerId
     * @param redisHost
     * @param redisModel
     * @param redisPassword
     * @param redisReconnectMaxCount
     * @param redisCipherType
     * @param redisCipherKey
     */
    private static void p_redisInit(final String redisServerId, final String redisHost, final String redisModel, final String redisPassword, final Integer redisReconnectMaxCount, final String redisCipherType, final String redisCipherKey){
        p_redisServerId = redisServerId;
        //
        RedisConfigBean redisConfigBean = new RedisConfigBean();
        redisConfigBean.setServerId(redisServerId);
        redisConfigBean.setHosts(redisHost);  // ip:port
        redisConfigBean.setModel(redisModel);  // 单机模式
        redisConfigBean.setPassword(redisPassword);  // 密码
        redisConfigBean.setReconnectMaxCount(redisReconnectMaxCount);  // 重连次数
        redisConfigBean.setCipherType(redisCipherType);//加密方式
        redisConfigBean.setCipherKey(redisCipherKey);//加密密钥
        try {
            RedisUtil.init(redisConfigBean);
            LogUtil.print("CacheUtil_redisInit_1", "redis连接成功", redisServerId, redisHost, redisModel);
        } catch (Throwable throwable) {
            LogUtil.error("CacheUtil_redisInit_t1", throwable, "redis连接失败", redisServerId, redisHost, redisModel);
        }
    }

    /**
     * 2025-01-14
     * redis连接测试
     * @return
     */
    public static final boolean testConnection(){
        final String key = "testConnection";
        final String now = TimerUtil.now();
        final String value = "testConnectionValue_" + now;
        //
        if("yes".equals(p_redisEnable)){
            set(REDIS, key, value, 0);
            final RedisDao redisDao = get(REDIS, key);
            if(redisDao == null){
                LogUtil.error("CacheUtil_testConnection_2", "Redis缓存失败");
                return false;
            }
            Serializable serializable = redisDao.getContent();
            if(serializable == null || !(serializable instanceof String)){
                LogUtil.error("CacheUtil_testConnection_3", "Redis缓存失败");
                return false;
            }
            final String content = (String) serializable;
            if(!StringUtil.equals(value, content)){
                LogUtil.error("CacheUtil_testConnection_4", "本地缓存失败");
                return false;
            }
        }
        //
        set(LOCAL, key, value, 0);
        final String localValue = get(LOCAL, key);
        if(!StringUtil.equals(value, localValue)){
            LogUtil.error("CacheUtil_testConnection_1", "本地缓存失败");
            return false;
        }
        //
        return true;
    }

    /**
     * 2023-06-13
     * 存入缓存，默认本地存储
     * @param key key值，注意要遵循命名规范
     * @param value 要存入缓存的数据（本地存储通道要求实现Serializable，否则记日志拒绝缓存；
     *             共享可变对象须由调用方自行同步，详见LocalCacheUtil.set契约）
     */
    public static final void set(final String key, final Object value){
        if("yes".equals(p_redisEnable)){
            set(REDIS, key, value, 0L);
            return;
        }
        set(LOCAL, key, value, 0L);
    }

    /**
     * 2023-06-13
     * 存入缓存，默认本地存储
     * @param key key值，注意要遵循命名规范
     * @param value 要存入缓存的数据（本地存储通道要求实现Serializable，否则记日志拒绝缓存；
     *             共享可变对象须由调用方自行同步，详见LocalCacheUtil.set契约）
     * @param duration 有效存储时间，0表示永久有效，单位秒
     */
    public static final void set(final String key, final Object value, final long duration){
        if("yes".equals(p_redisEnable)){
            set(REDIS, key, value, duration);
            return;
        }
        set(LOCAL, key, value, duration);
    }

    /**
     * 2023-08-08
     * 存入缓存，默认本地存储
     * @param target 存入的位置， local-仅存入本地， redis-仅存入redis中
     * @param key key值，注意要遵循命名规范
     * @param value 要存入缓存的数据
     */
    public static final void set(final String target, final String key, final Object value){
        set(target, key, value, 0);
    }

    /**
     * 2023-06-13
     * 存入缓存
     * @param target 存入的位置， local-仅存入本地， redis-仅存入redis中
     * @param key key值，注意要遵循命名规范
     * @param value 要存入缓存的数据
     * @param duration 有效存储时间，0表示永久有效，单位秒
     */
    public static final void set(final String target, final String key, final Object value, final long duration){
        String localKey = getStandardKey(key);
        // redis存储
        if(REDIS.equals(target)) {
            if(StringUtil.equals(p_redisEnable, "yes")){
                if(!(value instanceof Serializable)){
                    LogUtil.error("CacheUtil_set_10", "Redis只允许存入Serializable对象", target, localKey, value);
                    return;
                }
                Serializable serializable = (Serializable) value;
                RedisDao redisDao = new RedisDao();
                redisDao.setCipher("no");
                redisDao.setServerId(p_redisServerId);
                redisDao.setKey(localKey);
                redisDao.setContent(serializable);
                redisDao.setDuration(duration);
                RedisUtil.set(redisDao);
                return;
            }
        }
        // 本地存储
        if(LOCAL.equals(target)){
            LocalCacheUtil.set(localKey, value, duration);
            return;
        }
        //
        LogUtil.error("CacheUtil_set_15", "未指定存储位置，处理失败", target, localKey);
    }

    /**
     * 从缓存获取数据
     */
    public static final <T extends Object> T get(final String key){
        if("yes".equals(p_redisEnable)){
            Object object = get(REDIS, key);
            T t = (T) object;
            return t;
        }
        // 本地存储
        Object object = get(LOCAL, key);
        T t = (T) object;
        return t;
    }

    /**
     * 2023-07-19
     */
    public static final <T extends Object> T get(final String target, final String key){
        String localKey = getStandardKey(key);
        //
        if(REDIS.equals(target)){
            if("yes".equals(p_redisEnable)){
                RedisDao redisDao = RedisUtil.get(p_redisServerId, localKey);
                T t = (T) redisDao;
                return t;
            }
        }
        // 本地存储
        if(LOCAL.equals(target)){
            Object object = LocalCacheUtil.get(localKey);
            T t = (T) object;
            return t;
        }
        //
        LogUtil.error("CacheUtil_get_10", "未指定存储位置，处理失败", target, localKey);
        return null;
    }

    /**
     * 2023-08-28
     * 2025-01-27 重命名
     * @return
     */
    public static final List<String> getLocalCacheKeyList(){
        List<String> list = LocalCacheUtil.getKeyList();
        return list;
    }

    /**
     * 删除缓存
     */
    public static final boolean remove(final String key){
        if("yes".equals(p_redisEnable)){
            remove(REDIS, key);
        }
        // 本地存储
        boolean flag = remove(LOCAL, key);
        return flag;
    }

    /**
     * 2023-07-21
     * 删除缓存
     * @param key key值，注意要遵循命名规范
     */
    public static final boolean remove(final String target, final String key){
        String localKey = getStandardKey(key);
        // redis存储
        if(REDIS.equals(target)){
            if("yes".equals(p_redisEnable)){
                RedisUtil.remove(p_redisServerId, localKey);
            }
        }
        // 本地存储
        if(LOCAL.equals(target)){
            LocalCacheUtil.remove(localKey);
        }
        //
        return true;
    }

    /**
     * 获取标准缓存key（即key全称）
     */
    public static final String getStandardKey(final String key){
        final String standardKey = p_redisServerId + "_" + key;
        return standardKey;
    }

}