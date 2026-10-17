package xc.crabapple.ssm.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.Pool;
import xc.crabapple.core.cipher.aes.Aes3Util;
import xc.crabapple.core.cipher.sm4.Sm4Util;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.regex.RegexUtil;
import xc.crabapple.core.thread.ThreadUtil;
import xc.crabapple.core.timer.TimerUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2016-12-06 1.0.0
 * 基于Redis的分布式数据缓存工具类（服务驱动类）
 * 要严格管理好key
 * 每个key对应的数据量要小，结构要简单，类型尽量统一
 * ===============================================
 * hash sets和string(counter)推荐使用, sort sets和lists(queue)不推荐使用
 * 存储类型保存在3种以内，建议不要超过3种
 * jedis不支持List操作
 * 新浪微博使用案例
 * http://www.cnblogs.com/me115/p/3482783.html
 * jedis的使用实例
 * http://blog.csdn.net/u011129848/article/details/51527287
 */
public final class RedisUtil {

    private static Map<String, RedisConfigBean> p_serverMap = null;  // 连接集合，当前客户端可以同时连接多个redis服务器
    public static boolean connected = false;
    private static String p_cipherType = "";  // 加密方式，目前支持两种方式：sm4 / aes128。为空或no表示不加密
    private static String p_cipherKey = "crabapple-240508";  // 看加密方式对应秘钥，必须为16位ASCII字符串

    /**
     * 2017-10-09 1.0.0
     */
    public static final void init(final RedisConfigBean redisConfigBean) throws Throwable {
        String cipherType = redisConfigBean.getCipherType();
        cipherType = StringUtil.trim(cipherType);
        if(!StringUtil.equalsOfOne(cipherType, null, "", "no")) {
            p_cipherType = cipherType;
        }
        String cipherKey = redisConfigBean.getCipherKey();
        cipherKey = StringUtil.trim(cipherKey);
        if(!StringUtil.isBlank(cipherKey)) {
            p_cipherKey = cipherKey;
        }
        //
        String serverId = redisConfigBean.getServerId();
        if (StringUtil.isBlank(serverId)) {
            LogUtil.error("RedisUtil_init_1", "redis配置错误，serverId为空");
            return;
        }
        String hosts = redisConfigBean.getHosts();
        if (StringUtil.isBlank(hosts)) {
            LogUtil.error("RedisUtil_init_2", "redis配置错误，hosts为空");
            return;
        }
        if (CollectionUtil.isBlank(p_serverMap)) {
            p_serverMap = new ConcurrentHashMap<>(8);
        }
        if (!p_serverMap.containsKey(serverId)) {
            p_serverMap.put(serverId, redisConfigBean);
        }
        //真正初始化
        String model = redisConfigBean.getModel();
        if (RedisConfigBean.SINGLETON.equals(model)) {
            p_initJedisPool(redisConfigBean);
            connected = true;
            return;
        }
        if (RedisConfigBean.SENTINEL.equals(model)) {
            p_initJedisSentinelPool(redisConfigBean);
            connected = true;
            return;
        }
    }

    /**
     * 2018-06-15 1.1.0
     * 单体写入
     * 速度测试：CentOS7.7-redis5.0.8-哨兵模式下，批量插入3000（即3千）条数据耗时约800ms(即800毫秒)
     * 目前支持的数据类型有两类(接收变量必须为封装类型)：
     * 1.字符串类：Integer, Long, Float, Double, String
     * 2.序列化类：String[], byte[], HashMap, 实现Serializable接口的类实例
     */
    public static final String set(final RedisDao redisDao){
        String serverId = redisDao.getServerId();
        if (StringUtil.isBlank(serverId)) {
            serverId = p_getServerId();
            if (StringUtil.isBlank(serverId)) {
                LogUtil.error("RedisUtil_set_1", "serverId_is_null", redisDao);
                return "error-1";
            }
            redisDao.setServerId(serverId);
        }
        //
        final String key = redisDao.getKey();
        boolean flag = p_keyCheck(key);
        if (!flag) {
            return "error-2";
        }
        //
        Jedis jedis = p_getJedis(serverId);
        if (jedis == null) {
            LogUtil.error("RedisUtil_set_2", "jedis为空", key);
            return "error-3";
        }
        long duration = redisDao.getDuration();
        if (duration <= 0) {
            duration = Integer.MAX_VALUE;
        }
        SetParams setParams = new SetParams();
        //setParams.nx();  //nx是仅当key不存在时才set, xx是仅当key已存在时才set
        setParams.ex(duration);  //设置过期时间，ex方法表示秒，px方法表示毫秒
        String snow = TimerUtil.now();
        redisDao.setSaveTime(snow);
        try{
            boolean flag2 = p_encryptRedisDao(redisDao);
            if(!flag2){
                return "error-4";
            }
            //
            byte[] keyBytes = key.getBytes();
            byte[] redisDaoBytes = p_obj2bytes(redisDao);
            // 2026-08-26 自动登出系统时长过短
            // 删除set前的冗余jedis.del：SET key value EX 本身就是原子覆盖写，先del再set反而引入
            // "del成功、set失败（网络闪断等）导致key丢失且无补偿"的故障窗口
            jedis.set(keyBytes, redisDaoBytes, setParams);
            return "ok";
        }catch(Throwable throwable){
            LogUtil.error("RedisUtil_set_t1", throwable, redisDao);
            return "error-6";
        } finally {
            IOUtil.close(jedis);
        }
    }

    /**
     * 2021-04-12 7.5.0
     * 批处理写入
     *
     * @param list
     */
    public static final void setBatch(final List<RedisDao> list) {
        if (CollectionUtil.isBlank(list)) {
            return;
        }
        RedisDao tmpRedisDao = list.get(0);
        String tmpKey = tmpRedisDao.getKey();
        String serverId = tmpRedisDao.getServerId();
        if (StringUtil.isBlank(serverId)) {
            serverId = p_getServerId();
        }
        if (StringUtil.isBlank(serverId)) {
            LogUtil.error("RedisUtil_setBatch_1", "serverId_is_null", tmpRedisDao);
            return;
        }
        Jedis jedis = p_getJedis(serverId);
        if (jedis == null) {
            LogUtil.error("RedisUtil_setBatch_2", "jedis为空", tmpKey);
            return;
        }
        Pipeline pipeline = jedis.pipelined();
        try{
            for (RedisDao redisDao : list) {
                if (redisDao == null) {
                    continue;
                }
                final String key = redisDao.getKey();
                boolean flag = p_keyCheck(key);
                if (!flag) {
                    continue;
                }
                long duration = redisDao.getDuration();
                if (duration <= 0) {
                    duration = Integer.MAX_VALUE;
                }
                //
                SetParams setParams = new SetParams();
                //setParams.nx();  // nx是仅当key不存在时才set, xx是仅当key已存在时才set
                setParams.ex(duration);  //设置过期时间，ex方法表示秒，px方法表示毫秒
                String snow = TimerUtil.now();
                redisDao.setSaveTime(snow);
                //
                boolean flag2 = p_encryptRedisDao(redisDao);
                if(!flag2){
                    return;
                }
                //
                byte[] keyBytes = key.getBytes();
                byte[] redisDaoBytes = p_obj2bytes(redisDao);
                // 2026-08-26 自动登出系统时长过短
                // 删除set前的冗余pipeline.del：SET key value EX 本身就是原子覆盖写，先del再set反而引入
                // "del成功、set失败（网络闪断等）导致key丢失且无补偿"的故障窗口（与单体写入set同源问题）
                pipeline.set(keyBytes, redisDaoBytes, setParams);
            }
            pipeline.sync();
        }catch(Throwable throwable){
            LogUtil.error("RedisUtil_setBatch_t1", throwable);        } finally {
            IOUtil.close(pipeline, jedis);
        }
    }

    /**
     * 2021-03-31 7.4.0
     */
    public static final List<String> getKeyList() {
        String serverId = p_getServerId();
        List<String> list = getKeyList(serverId, null);
        return list;
    }

    /**
     * 2021-03-31 7.4.0
     */
    public static final List<String> getKeyList(final String keys) {
        String serverId = p_getServerId();
        List<String> list = getKeyList(serverId, keys);
        return list;
    }

    /**
     * 2018-10-30 1.1.0
     * 速度测试：CentOS7.7-redis5.0.8-哨兵模式下，返回100,000（即10万）条数据耗时约150ms
     *
     * @param serverId
     * @param keys     模糊匹配，限定要查询的一个或多个key
     */
    public static final List<String> getKeyList(final String serverId, final String keys) {
        String effectiveKeys = StringUtil.deleteAll(keys, " ");
        String[] kkk = {};
        if (!StringUtil.isBlank(effectiveKeys)) {
            kkk = StringUtil.split(effectiveKeys, ",");
        }
        String effectiveServerId = serverId;
        if (StringUtil.isBlank(effectiveServerId)) {
            effectiveServerId = p_getServerId();
        }
        Jedis jedis = p_getJedis(effectiveServerId);
        if (jedis == null) {
            LogUtil.error("RedisUtil_getKeyList_1", "jedis为空");
            List<String> list = new ArrayList<>();
            return list;
        }
        Set<String> allSet = new HashSet<>();
        try{
            if (CollectionUtil.isBlank(kkk)) {
                String cursor = "0";
                ScanParams scanParams = new ScanParams().match("*").count(100);
                do {
                    ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
                    allSet.addAll(scanResult.getResult());
                    cursor = scanResult.getCursor();
                } while (!"0".equals(cursor));
            } else {
                for (String k : kkk) {
                    if (StringUtil.isBlank(k)) {
                        continue;
                    }
                    String pattern = "*" + k + "*";
                    String cursor = "0";
                    ScanParams scanParams = new ScanParams().match(pattern).count(100);
                    do {
                        ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
                        allSet.addAll(scanResult.getResult());
                        cursor = scanResult.getCursor();
                    } while (!"0".equals(cursor));
                }
            }
        }catch(Throwable throwable){
            LogUtil.error("RedisUtil_getKeyList_t1", throwable);
            List<String> list = new ArrayList<>();
            return list;
        } finally {
            IOUtil.close(jedis);
        }
        List<String> list = new ArrayList<>();
        if (CollectionUtil.isBlank(allSet)) {
            return list;
        }
        Iterator<String> iterator = allSet.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            list.add(key);
        }
        allSet.clear();
        return list;
    }

    public static final List<String> getKeyListByServerId(final String serverId) {
        List<String> list = getKeyList(serverId, null);
        return list;
    }

    /**
     * 2021-03-31 7.4.0
     */
    public static final RedisDao get(final String key) {
        String serverId = p_getServerId();
        RedisDao redisDao = get(serverId, key);
        return redisDao;
    }

    /**
     * 2020-07-08 6.3.0
     * 获取指定的自定义类(实现Serializable接口的bean类)
     */
    public static final RedisDao get(final String serverId, final String key) {
        Jedis jedis = p_getJedis(serverId);
        if (jedis == null) {
            LogUtil.error("RedisUtil_get_1", "jedis为空", serverId, key);
            return null;
        }
        try{
            byte[] bytesKey = key.getBytes();
            Object object = jedis.get(bytesKey);
            if (object == null) {
                //LogUtil.warn("RedisUtil_get_2", "value为空", serverId, key);
                return null;
            }
            if (!(object instanceof byte[])) {
                //LogUtil.warn("RedisUtil_get_3", "bytes为空", serverId, key);
                return null;
            }
            byte[] bytes = (byte[]) object;
            RedisDao redisDao = p_bytes2dao(serverId, bytes);
            return redisDao;
        }catch(Throwable throwable){
            LogUtil.error("RedisUtil_get_t1", throwable, serverId, key);
            return null;
        } finally {
            IOUtil.close(jedis);
        }
    }

    /**
     * 2021-03-31 7.4.0
     */
    public static final <T extends Serializable> T getContent(final String key) {
        String serverId = p_getServerId();
        T t = getContent(serverId, key);
        return t;
    }

    /**
     * 2020-06-01 6.0.0
     * 首次引入根据泛型类型实现方法动态返回值对象
     *
     * @param key
     * @param <T>
     * @return 该方法返回值的类型与调用setContent方法时传参类型一致
     */
    public static final <T extends Serializable> T getContent(final String serverId, final String key) {
        RedisDao redisDao = get(serverId, key);
        if (redisDao == null) {
            //LogUtil.warn("RedisUtil_getContent_1", serverId, key);
            return null;
        }
        Serializable content = redisDao.getContent();
        if (content == null) {
            //LogUtil.warn("RedisUtil_getContent_2", serverId, key);
            return null;
        }
        T t = (T) content;
        return t;
    }

    /**
     * 2021-03-31 7.4.0
     */
    public static final List<RedisDao> getList() {
        List<RedisDao> list = getList(null, null);
        return list;
    }

    /**
     * 2021-03-31 7.4.0
     *
     * @param keys 模糊匹配，限定要查询的一个或多个key，英文逗号分隔，如"aaa_,xc_crabapple_bbb_"
     */
    public static final List<RedisDao> getList(final String keys) {
        String serverId = p_getServerId();
        List<RedisDao> list = getList(serverId, keys);
        return list;
    }

    /**
     * 2020-06-16 6.2.0
     * 速度测试：CentOS7.7-redis5.0.8-哨兵模式下，返回100,000（即10万）条数据耗时约5000ms（即5秒）
     *
     * @param serverId
     * @param keys     模糊匹配，限定要查询的一个或多个key，英文逗号分隔，如"aaa_,xc_crabapple_bbb_"
     */
    public static final List<RedisDao> getList(final String serverId, final String keys) {
        String effectiveKeys = StringUtil.deleteAll(keys, " ");
        String[] kkk = {};
        if (!StringUtil.isBlank(effectiveKeys)) {
            kkk = StringUtil.split(effectiveKeys, ",");
        }
        String effectiveServerId = serverId;
        if (StringUtil.isBlank(effectiveServerId)) {
            effectiveServerId = p_getServerId();
        }
        Jedis jedis = p_getJedis(effectiveServerId);
        if (jedis == null) {
            LogUtil.error("RedisUtil_getList_1", "jedis_is_null");
            return null;
        }
        Pipeline pipeline = jedis.pipelined();
        List<RedisDao> redisDaoList = new ArrayList<>();
        List<String> keyList = getKeyList(serverId, keys);
        List<Object> allList = null;
        try{
            for (String key : keyList) {
                if (!CollectionUtil.isBlank(kkk)) {
                    boolean flag = false;
                    for (String k : kkk) {
                        if (StringUtil.isBlank(k)) {
                            continue;
                        }
                        if (StringUtil.containOf(key, k)) {
                            flag = true;
                            break;
                        }
                    }
                    if (!flag) {
                        continue;
                    }
                }
                //
                byte[] bytesKey = key.getBytes();
                Response<byte[]> response = pipeline.get(bytesKey);
            }
            allList = pipeline.syncAndReturnAll();  //只有执行了上面的get方法，该方法返回值才有值
            if (CollectionUtil.isBlank(allList)) {
                return redisDaoList;
            }
        }catch(Throwable throwable){
            LogUtil.error("RedisUtil_getList_t1", throwable);
            IOUtil.close(jedis);
            return null;
        } finally {
            IOUtil.close(pipeline);
            keyList.clear();
        }
        try{
            for (Object object : allList) {
                if (object == null) {
                    continue;
                }
                if (!(object instanceof byte[])) {
                    continue;
                }
                byte[] bytes = (byte[]) object;
                RedisDao redisDao = p_bytes2dao(serverId, bytes);
                if (redisDao == null) {
                    continue;
                }
                redisDaoList.add(redisDao);
            }
        }catch(Throwable throwable){
            LogUtil.error("RedisUtil_getList_t5", throwable);
        } finally {
            IOUtil.close(jedis);
        }
        return redisDaoList;
    }

    /**
     * 2021-03-31 7.4.0
     */
    public static final List<RedisDao> getListByServerId(final String serverId) {
        List<RedisDao> list = getList(serverId, null);
        return list;
    }

    /**
     * 2021-03-31 7.4.0
     * 精确删除指定的key
     *
     * @param key
     */
    public static final void remove(final String key) {
        String serverId = p_getServerId();
        remove(serverId, key);
    }

    /**
     * 2017-10-09 1.0.0
     * 精确删除指定的key
     *
     * @param serverId
     * @param key
     */
    public static final void remove(final String serverId, final String key) {
        Jedis jedis = p_getJedis(serverId);
        if (jedis == null) {
            return;
        }
        try{
            jedis.del(key);
        }catch(Throwable throwable){
            LogUtil.error("RedisUtil_remove_t1", throwable, serverId, key);
        } finally {
            IOUtil.close(jedis);
        }
    }

    /**
     * 2021-03-31 7.4.0
     * 模糊匹配删除
     * @param keys 模糊匹配，限定要查询的一个或多个key
     */
    public static final void removeMatch(final String keys) {
        String serverId = p_getServerId();
        removeMatch(serverId, keys);
    }

    /**
     * 2021-03-31 7.4.0
     * 模糊匹配删除
     * @param keys 模糊匹配，限定要查询的一个或多个key
     */
    public static final void removeMatch(final String serverId, final String keys) {
        List<String> keyList = getKeyList(serverId, keys);
        if (CollectionUtil.isBlank(keyList)) {
            return;
        }
        Jedis jedis = p_getJedis(serverId);
        if (jedis == null) {
            return;
        }
        try{
            String[] keyArray = keyList.toArray(new String[0]);
            jedis.del(keyArray);
        }catch(Throwable throwable){
            LogUtil.error("RedisUtil_removeMatch_t1", throwable, serverId, keys);
        } finally {
            IOUtil.close(jedis);
        }
    }

    /**
     * 2021-03-31 7.4.0
     * 清空所有redis数据
     */
    public static final void removeAll() {
        String serverId = p_getServerId();
        removeAll(serverId);
    }

    /**
     * 2020-10-05 6.7.0
     * 清空所有redis数据
     */
    public static final void removeAll(final String serverId) {
        Jedis jedis = p_getJedis(serverId);
        if (jedis == null) {
            return;
        }
        try{
            jedis.flushAll();
        }catch(Throwable throwable){
            LogUtil.error("RedisUtil_removeAll_t1", throwable, serverId);
        } finally {
            IOUtil.close(jedis);
        }
    }

    /**
     * 2021-03-31 7.4.0
     */
    public static final boolean isRunning() {
        String serverId = p_getServerId();
        boolean flag = isRunning(serverId);
        return flag;
    }

    /**
     * 2020-06-12 6.1.0
     * 判断redis服务是否正常工作
     */
    public static final boolean isRunning(final String serverId) {
        Jedis jedis = p_getJedis(serverId);
        if (jedis == null) {
            return false;
        }
        try{
            return true;
        }finally{
            IOUtil.close(jedis);
        }
    }

    private static final List<String> p_hosts2list(final String hosts) {
        String effectiveHosts = StringUtil.getPureStr(hosts);
        effectiveHosts = StringUtil.deleteAll(effectiveHosts, " ");
        effectiveHosts = StringUtil.trim(effectiveHosts);
        List<String> list = new ArrayList<>();
        if (effectiveHosts.indexOf(",") == -1) {
            list.add(effectiveHosts);
            return list;
        }
        String[] array = StringUtil.split(effectiveHosts, ",");
        for (String host : array) {
            list.add(host);
        }
        return list;
    }

    /**
     * 2017-10-09 1.0.0
     * 初始化单例池
     */
    private static final void p_initJedisPool(final RedisConfigBean redisConfigBean) throws Throwable {
        String hosts = redisConfigBean.getHosts();
        if (StringUtil.isBlank(hosts)) {
            LogUtil.error("RedisUtil_p_initJedisPool_1", "hosts为空");
            return;
        }
        List<String> hostsList = p_hosts2list(hosts);
        String host = hostsList.get(0);
        String[] array = StringUtil.split(host, ":");
        String ip = array[0];
        String portTmp = array[1];
        int port = Integer.parseInt(portTmp);
        //
        int timeout = redisConfigBean.getTimeout();
        String password = redisConfigBean.getPassword();
        if (StringUtil.isBlank(password)) {
            password = null;
        }
        JedisPoolConfig jedisPoolConfig = p_getJedisPoolConfig(redisConfigBean);
        JedisPool jedisPool = new JedisPool(jedisPoolConfig, ip, port, timeout, password);
        //立即测试
        Jedis jedis = null;
        try{
            jedis = p_getJedisFromPool(jedisPool, redisConfigBean);
            redisConfigBean.setPool(jedisPool);
        }catch(Throwable throwable){
            String location = "RedisUtil_p_initJedisPool_t1";
            LogUtil.error(location, throwable, hosts);
            throw new Throwable(location, throwable);
        } finally {
            IOUtil.close(jedis);
        }
    }

    private static final void p_initJedisSentinelPool(final RedisConfigBean redisConfigBean) throws Throwable {
        String hosts = redisConfigBean.getHosts();
        if (StringUtil.isBlank(hosts)) {
            LogUtil.warn("RedisUtil_p_initJedisSentinelPool_1", "hosts为空");
            return;
        }
        String serverId = redisConfigBean.getServerId();
        int timeout = redisConfigBean.getTimeout();
        String password = redisConfigBean.getPassword();
        if (StringUtil.isBlank(password)) {
            password = null;
        }
        JedisPoolConfig jedisPoolConfig = p_getJedisPoolConfig(redisConfigBean);
        Set<String> set = p_hosts2set(hosts);
        JedisSentinelPool jedisSentinelPool = new JedisSentinelPool(serverId, set, jedisPoolConfig, timeout, password);
        //立即测试
        Jedis jedis = null;
        try{
            jedis = p_getJedisFromPool(jedisSentinelPool, redisConfigBean);
            redisConfigBean.setPool(jedisSentinelPool);
        }catch(Throwable throwable){
            final String location = "RedisUtil_p_initJedisSentinelPool_t1";
            LogUtil.error(location, throwable, redisConfigBean);
            throw new Throwable(location, throwable);
        } finally {
            IOUtil.close(jedis);
        }
    }

    /**
     * 2021-04-12 7.5.0
     * 统一公共配置
     */
    private static final JedisPoolConfig p_getJedisPoolConfig(final RedisConfigBean redisConfigBean) {
        int maxtotal = redisConfigBean.getMaxTotal();
        int maxidle = redisConfigBean.getMaxIdle();
        int minIdle = redisConfigBean.getMinIdle();
        long maxWait = redisConfigBean.getMaxWait();
        Duration maxWaitDuration = Duration.ofMillis(maxWait);
        long timeBetweenEvictionRuns = redisConfigBean.getTimeBetweenEvictionRuns();
        Duration timeBetweenEvictionRunsDuration = Duration.ofMillis(timeBetweenEvictionRuns);
        long minEvictableIdleTime = redisConfigBean.getMinEvictableIdleTime();
        Duration minEvictableIdleTimeDuration = Duration.ofMillis(minEvictableIdleTime);
        Integer numTestsPerEvictionRun = redisConfigBean.getNumTestsPerEvictionRun();
        boolean blockWhenExhausted = true;
        String blockWhenExhaustedTmp = redisConfigBean.getBlockWhenExhausted();
        if (!"yes".equals(blockWhenExhaustedTmp)) {
            blockWhenExhausted = false;
        }
        boolean testOnBorrow = true;
        String testOnBorrowTmp = redisConfigBean.getTestOnBorrow();
        if (!"yes".equals(testOnBorrowTmp)) {
            testOnBorrow = false;
        }
        boolean testOnReturn = true;
        String testOnReturnTmp = redisConfigBean.getTestOnReturn();
        if (!"yes".equals(testOnReturnTmp)) {
            testOnReturn = false;
        }
        boolean testWhileIdle = true;
        String testWhileIdleTmp = redisConfigBean.getTestWhileIdle();
        if (!"yes".equals(testWhileIdleTmp)) {
            testWhileIdle = false;
        }
        boolean jmxEnabled = true;
        String jmxEnabledTmp = redisConfigBean.getJmxEnabled();
        if (!"yes".equals(jmxEnabledTmp)) {
            jmxEnabled = false;
        }
        //
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(maxtotal);
        jedisPoolConfig.setMaxIdle(maxidle);
        jedisPoolConfig.setMinIdle(minIdle);
        jedisPoolConfig.setMaxWait(maxWaitDuration);
        jedisPoolConfig.setBlockWhenExhausted(blockWhenExhausted);
        jedisPoolConfig.setTestOnBorrow(testOnBorrow);
        jedisPoolConfig.setTestOnReturn(testOnReturn);
        jedisPoolConfig.setTestWhileIdle(testWhileIdle);
        jedisPoolConfig.setJmxEnabled(jmxEnabled);
        jedisPoolConfig.setTimeBetweenEvictionRuns(timeBetweenEvictionRunsDuration);
        jedisPoolConfig.setMinEvictableIdleTime(minEvictableIdleTimeDuration);
        jedisPoolConfig.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        return jedisPoolConfig;
    }

    /**
     * 2017-10-09 1.0.0
     */
    private static final Jedis p_getJedis(final String serverId) {
        if (CollectionUtil.isBlank(p_serverMap)) {
            LogUtil.error("RedisUtil_p_getJedis_1", "没有连接Redis服务器主机");
            return null;
        }
        RedisConfigBean redisConfigBean = p_serverMap.get(serverId);
        Pool pool = redisConfigBean.getPool();
        if (pool == null) {
            return null;
        }
        Jedis jedis = null;
        try{
            jedis = p_getJedisFromPool(pool, redisConfigBean);
            return jedis;
        }catch(Throwable throwable){
            IOUtil.close(jedis);
            LogUtil.error("RedisUtil_p_getJedis_t1", throwable, serverId);
            return null;
        }
    }

    /**
     * 2019-04-29 2.0.0
     * 设置重连机制
     */
    private static final Jedis p_getJedisFromPool(final Pool pool, final RedisConfigBean redisConfigBean) throws Throwable {
        long reconnectIntervalMillis = redisConfigBean.getReconnectIntervalMillis();
        long reconnectMaxCount = redisConfigBean.getReconnectMaxCount();
        String password = redisConfigBean.getPassword();
        try{
            Jedis jedis = (Jedis) pool.getResource();
            if (jedis != null) {
                if (!StringUtil.isBlank(password)) {
                    jedis.auth(password);
                }
            }
            return jedis;
        }catch(Throwable throwable){
            int count = 0;  //计数器
            while (true) {
                count++;
                if (count > reconnectMaxCount) {
                    final String location = "RedisUtil_p_getJedisFromPool_e1";
                    LogUtil.error(location, throwable, redisConfigBean, count, reconnectMaxCount);
                    throw new Throwable(location, throwable);
                }
                //
                LogUtil.println("RedisUtil_p_getJedisFromPool_2", "Redis连接失败，即将进行重新连接", redisConfigBean, count, reconnectMaxCount);
                ThreadUtil.sleep(reconnectIntervalMillis);
                Jedis jedis = null;
                try{
                    jedis = (Jedis) pool.getResource();
                    if (jedis != null) {
                        if (!StringUtil.isBlank(password)) {
                            jedis.auth(password);
                        }
                    }
                    return jedis;
                }catch(Throwable throwable2) {
                    IOUtil.close(jedis);
                    if (count > reconnectMaxCount) {
                        final String location = "RedisUtil_p_getJedisFromPool_e2";
                        LogUtil.error(location, throwable2, redisConfigBean, count, reconnectMaxCount);
                        throw new Throwable(location, throwable2);
                    }
                }
            }
        }
    }

    private static final Set<String> p_hosts2set(final String hosts) {
        String effectiveHosts = StringUtil.getPureStr(hosts);
        effectiveHosts = StringUtil.deleteAll(effectiveHosts, " ");
        effectiveHosts = StringUtil.trim(effectiveHosts);
        Set<String> set = new HashSet<>();
        if (effectiveHosts.indexOf(",") == -1) {
            set.add(effectiveHosts);
            return set;
        }
        String[] array = StringUtil.split(effectiveHosts, ",");
        for (String host : array) {
            set.add(host);
        }
        return set;
    }

    /**
     * 2018-02-13 1.1.0
     * 对象转Byte数组，必须是实现了java.io.Serializable接口的对象
     */
    private static final byte[] p_obj2bytes(final Object obj) {
        byte[] bytes = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try{
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(obj);
            objectOutputStream.flush();
            bytes = byteArrayOutputStream.toByteArray();
        }catch(Throwable throwable){
            LogUtil.error("RedisUtil_p_obj2bytes_t1", throwable);
        } finally {
            IOUtil.close(objectOutputStream, byteArrayOutputStream);
        }
        return bytes;
    }

    /**
     * 2018-02-13 1.1.0
     * Byte数组转对象
     */
    private static final Object p_bytes2obj(final byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        if (bytes.length <= 0) {
            return null;
        }
        //
        ByteArrayInputStream byteArrayInputStream = null;
        ObjectInputStream objectInputStream = null;
        try{
            byteArrayInputStream = new ByteArrayInputStream(bytes);
            if (byteArrayInputStream == null) {
                return null;
            }
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            if (objectInputStream == null) {
                return null;
            }
            Object object = objectInputStream.readObject();
            if (object == null) {
                return null;
            }
            return object;
        }catch(Throwable throwable){
            if (throwable instanceof InvalidClassException) {
                LogUtil.error("RedisUtil_p_bytes2obj_t1", throwable, "缓存中的数据类型结构与当前程序定义的不一致了");
                return null;
            }
            return null;
        } finally {
            IOUtil.close(objectInputStream, byteArrayInputStream);
        }
    }

    /**
     * 2021-03-31 7.4.0
     * 2026-08-26 自动登出系统时长过短
     * 防御修复：原逻辑TimerUtil.compareTo(now, expireTime)在expireTime解析失败（如超大duration经
     * moveTime算出6位年份"101960-04-04"）时str2time返回null，compareTo内部抛NPE被上层get的catch
     * 吞掉返回null——表现为"key在redis中存在却永远读出null"。现改为：先解析过期时间，解析失败按
     * 未过期处理（fail-open）并记日志；过期判定改为时间戳直接比较，消除格式字符串比较的不确定性
     * 2026-08-26 同日审核整改：①补空串前置判断——TimerUtil.str2time对
     * 空串/null会返回"当前时间"而非null，不拦截会导致本防御被静默绕过（既放行又打不到日志）；
     * ②当前时间改由str2time(TimerUtil.now())同源解析（与expireTime同为GMT+8解析路径），消除
     * System.currentTimeMillis()绝对时间比较在非东八区主机上的时区偏移
     */
    private static final RedisDao p_bytes2dao(final String serverId, final byte[] bytes) {
        Object object = p_bytes2obj(bytes);
        if (object == null) {
            return null;
        }
        RedisDao redisDao = (RedisDao) object;
        p_decryptRedisDao(redisDao);
        long duration = redisDao.getDuration();
        String saveTime = redisDao.getSaveTime();
        String expireTime = TimerUtil.moveTime(saveTime, duration, TimerUtil.SECOND);
        //
        if (StringUtil.isBlank(expireTime)) {
            LogUtil.error("RedisUtil_p_bytes2dao_1", "过期时间为空，按未过期处理", saveTime, duration);
            return redisDao;
        }
        Date expireDate = TimerUtil.str2time(expireTime);
        if (expireDate == null) {
            LogUtil.error("RedisUtil_p_bytes2dao_2", "过期时间解析失败，按未过期处理", saveTime, duration, expireTime);
            return redisDao;
        }
        Date nowDate = TimerUtil.str2time(TimerUtil.now());
        if (nowDate != null && nowDate.getTime() > expireDate.getTime()) {
            final String key = redisDao.getKey();
            remove(serverId, key);
            //LogUtil.warn("RedisUtil_p_bytes2dao_3", "已过期", connectId, key);
            return null;
        }
        return redisDao;
    }

    /**
     * 2023-06-13
     * 加密redisDao对象
     */
    private static final boolean p_encryptRedisDao(final RedisDao redisDao) {
        if (redisDao == null) {
            return false;
        }
        if (StringUtil.isBlank(p_cipherType)) {
            return true;
        }
        //
        if ("sm4".equals(p_cipherType)) {
            Serializable content = redisDao.getContent();
            byte[] contentBytes = p_obj2bytes(content);
            try{
                byte[] encryptContentBytes = Sm4Util.encrypt(p_cipherKey, contentBytes);
                redisDao.setContent(encryptContentBytes);
                return true;
            }catch(Throwable throwable){
                String key = redisDao.getKey();
            LogUtil.error("RedisUtil_p_encryptRedisDao_t1", "redisDao sm4加密失败", key);
                return false;
            }
        }
        //
        if("aes128".equals(p_cipherType)) {
            Serializable content = redisDao.getContent();
            byte[] contentBytes = p_obj2bytes(content);
            try{
                byte[] encryptContentBytes = Aes3Util.encryptBytesReturnBytes(p_cipherKey, contentBytes);
                redisDao.setContent(encryptContentBytes);
                return true;
            }catch(Throwable throwable){
                String key = redisDao.getKey();
            LogUtil.error("RedisUtil_p_encryptRedisDao_t2", "redisDao aes加密失败", key);
                return false;
            }
        }
        //
        return true;
    }

    /**
     * 2023-06-13
     * 解密redisDao对象
     */
    private static final void p_decryptRedisDao(final RedisDao redisDao) {
        if (redisDao == null) {
            return;
        }
//        final String cipher = redisDao.getCipher();
//        if (StringUtil.isBlank(cipher) || StringUtil.equalsOfOne(cipher, "no")) {
//            return;
//        }
        if (StringUtil.isBlank(p_cipherType)) {
            return;
        }
        //
        if ("sm4".equals(p_cipherType)) {
            try{
                Serializable encryptContent = redisDao.getContent();
                byte[] encryptContentBytes = (byte[]) encryptContent;
                byte[] bytes = Sm4Util.decryptBytes(p_cipherKey, encryptContentBytes);
                Serializable content = (Serializable) p_bytes2obj(bytes);
                redisDao.setContent(content);
            }catch(Throwable throwable){
                String key = redisDao.getKey();
                LogUtil.error("RedisUtil_p_decryptRedisDao_t1", throwable, "redisDao-sm4解密失败", key);
            }
            return;
        }
        //
        if ("aes128".equals(p_cipherType)) {
            try{
                Serializable encryptContent = redisDao.getContent();
                byte[] encryptContentBytes = (byte[]) encryptContent;
                byte[] bytes = Aes3Util.decryptBytes(p_cipherKey, encryptContentBytes);
                Serializable content = (Serializable) p_bytes2obj(bytes);
                redisDao.setContent(content);
            }catch(Throwable throwable){
                LogUtil.error("RedisUtil_p_decryptRedisDao_t2", throwable, "RedisDao解密失败");
            }
        }
    }

    /**
     * 2019-04-29 2.0.0
     * 关闭整个连接池
     */
    public static final void closePool(final String serverId) {
        if (CollectionUtil.isBlank(p_serverMap)) {
            LogUtil.error("RedisUtil_closePool_1", "没有连接Redis服务器主机");
            return;
        }
        RedisConfigBean redisConfigBean = p_serverMap.get(serverId);
        Pool pool = redisConfigBean.getPool();
        if (pool == null) {
            p_serverMap.remove(serverId);
            return;
        }
        try{
            pool.close();
        }catch(Throwable throwable){
            //ignore
        } finally {
            p_serverMap.remove(serverId);
        }
    }

    /**
     * 2021-04-12 7.5.0
     */
    private static final boolean p_keyCheck(final String key) {
//        if (StringUtil.isBlank(key)) {
//            LogUtil.error("RedisUtil_p_keyCheck_1", "key_is_null");
//            return false;
//        }
//        final String regulation = "key命名规则： 1.只能包含英文大小写（居首）/数字/下划线（2个）。 2.标准格式：项目名_模块名_业务名，如crabapple_ws_xxx";
//        //
//        if (!RegexUtil.isUpperLowerNumUnderline(key)) {
//            LogUtil.error("RedisUtil_p_keyCheck_2", "key_format_error", regulation, key);
//            return false;
//        }
//        int count = StringUtil.getCount(key, "_");
//        if (count != 2) {
//            LogUtil.error("RedisUtil_p_keyCheck_3", "key_format_error", regulation, key);
//            return false;
//        }
        //
        return true;
    }

    private static final String p_getServerId() {
        if (CollectionUtil.isBlank(p_serverMap)) {
            LogUtil.error("RedisUtil_p_getServerId_1", "没有连接Redis服务器主机");
            return null;
        }
        //
        Set<String> set = p_serverMap.keySet();
        Iterator<String> iterator = set.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            RedisConfigBean redisConfigBean = p_serverMap.get(key);
            if (redisConfigBean != null) {
                String serverId = redisConfigBean.getServerId();
                return serverId;
            }
        }
        return "";
    }

}