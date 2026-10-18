package xc.crabapple.ssm.security.limiter;

import xc.crabapple.core.cache.LocalCacheUtil;
import xc.crabapple.core.collection.ListUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.net.ip.IPUtil;
import xc.crabapple.core.servlet.ServletUtil;
import xc.crabapple.core.thread.Business;
import xc.crabapple.core.thread.ThreadUtil;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2023-07-13
 * 2023-09-08 由clientIp+接口限制 改为 clientId限制
 * 2025-01-23 重构
 * 请求限流机制工具类
 */
public final class LimiterUtil {

    private static boolean p_inited = false;
    private static String p_enable = "no";
    private static long p_stamplist_expire = 3600L * 24L * 1L;
    private static ConcurrentHashMap<String, String> p_id_rule_map = null;
    private static ConcurrentHashMap<String, Long> p_id_lock_map = null;
    private static ConcurrentHashMap<String, String> p_ip_rule_map = null;
    private static ConcurrentHashMap<String, Long> p_ip_lock_map = null;

    /**
     * 2025-11-06
     * @param clientIdRule
     * @param clientIpRule
     */
    public static final void init(final String clientIdRule, final String clientIpRule){
        init("yes", clientIdRule, clientIpRule);
    }

    /**
     * 2023-07-13
     * 2025-01-23 重构
     */
    public static final void init(final String enable, final String clientIdRule, final String clientIpRule){
        if(p_inited){
            return;
        }
        p_inited = true;
        p_enable = enable;
        //
        p_id_rule_map = new ConcurrentHashMap<>(1024);
        p_id_lock_map = new ConcurrentHashMap<>(128);
        p_ip_rule_map = new ConcurrentHashMap<>(1024);
        p_ip_lock_map = new ConcurrentHashMap<>(128);
        p_initRule(p_id_rule_map, clientIdRule);
        p_initRule(p_ip_rule_map, clientIpRule);
        LogUtil.print("LimiterUtil_init_5", "限流器初始化完成，运行期间不得更改", clientIdRule, clientIpRule);
        //
        ThreadUtil.start(new Business("LimiterUtil_init_1", false){
            @Override
            public void process() {
                while(true) {
                    ThreadUtil.sleep(10000L);
                    lockMapUpdate(p_id_rule_map, p_id_lock_map);
                    lockMapUpdate(p_ip_rule_map, p_ip_lock_map);
                }
            }
        });
    }

    /**
     * 2023-07-13
     * 2025-01-23 重构
     * 更新限流规则
     */
    private static void p_initRule(final ConcurrentHashMap<String, String> ruleMap, final String clientxRule){
        String localClientxRule = StringUtil.getPureStr(clientxRule);
        localClientxRule = StringUtil.deleteAll(localClientxRule, " ");
        if(StringUtil.startsWith(localClientxRule, ",")){
            localClientxRule = StringUtil.deleteFirst(localClientxRule, 1);
        }
        if(StringUtil.lastsWith(localClientxRule, ",")){
            localClientxRule = StringUtil.deleteLast(localClientxRule, 1);
        }
        if(StringUtil.isBlank(localClientxRule)){
            localClientxRule = "*#*#500#60#10";
        }
        //
        final String[] array = StringUtil.split(localClientxRule, ",");
        for(int i = 0; i < array.length; i++){
            final String rule = array[i];
            final String[] arr = StringUtil.split(rule, "#");
            final String clientX = arr[0];  // clientId 或 clientIp
            final String api = arr[1];  // 接口标准名称
            final String ruleCountMax = arr[2];  // 限定时间，请求的最大次数
            final String ruleWithinTime = arr[3];  // 限定时间，单位秒
            final String ruleLockTime = arr[4];  // 锁定时间，单位秒
            final String key = clientX + "#" + api;
            final String ruleValue = ruleCountMax + "#" + ruleWithinTime + "#" + ruleLockTime;
            ruleMap.put(key, ruleValue);
        }
    }

    /**
     * 2023-07-13
     * 2025-01-23 重构
     * 更新锁定集合，由子线程调用
     */
    public static final void lockMapUpdate(final ConcurrentHashMap<String, String> ruleMap, final ConcurrentHashMap<String, Long> lockMap){
        final long nowStamp = System.currentTimeMillis();
        Set<String> lockSet = lockMap.keySet();
        Iterator<String> iterator = lockSet.iterator();
        while(iterator.hasNext()){
            final String key = iterator.next();  // clientId
            final Long reqStamp = lockMap.get(key);
            final String rule = p_getRule(ruleMap, key);  // 获取规则
            final String[] ruleArray = StringUtil.split(rule, "#");
            final String ruleLockTimeStr = ruleArray[2];  // 限定时间，请求的最大次数
            long ruleLockTime = Long.parseLong(ruleLockTimeStr);
            ruleLockTime *= 1000;
            final long cha = nowStamp - reqStamp;
            // 超过了锁定时间就从锁定名单中移除
            if(cha > ruleLockTime){
                lockMap.remove(key);
            }
        }
    }

    /**
     * 2025-01-23 重构
     * @return
     */
    public static final String check(){
        final String clientId = ServletUtil.getString("clientId");
        final String clientIp = IPUtil.getClientIp();
        final String result = check(clientId, clientIp);
        return result;
    }

    /**
     * 2025-01-23 重构
     * @param clientId
     * @return
     */
    public static final String check(final String clientId){
        final String clientIp = IPUtil.getClientIp();
        final String result = check(clientId, clientIp);
        return result;
    }

    /**
     * 2025-01-23 重构
     * @param clientId
     * @param clientIp
     * @return
     */
    public static final String check(final String clientId, final String clientIp){
        final String idResult = checkClientId(clientId);
        if(!"success".equals(idResult)){
            return idResult;
        }
        final String ipResult = checkClientIp(clientIp);
        return ipResult;
    }

    /**
     * 2025-01-23 重构
     * @return
     */
    public static final String checkClientId(){
        final String clientId = ServletUtil.getString("clientId");
        final String result = p_checkClientX(p_id_rule_map, p_id_lock_map, clientId);
        return result;
    }

    /**
     * 2025-01-23 重构
     * @param clientId
     * @return
     */
    public static final String checkClientId(final String clientId){
        final String result = p_checkClientX(p_id_rule_map, p_id_lock_map, clientId);
        return result;
    }

    /**
     * 2025-01-23 重构
     * @return
     */
    public static final String checkClientIp(){
        final String clientIp = IPUtil.getClientIp();
        final String result = p_checkClientX(p_ip_rule_map, p_ip_lock_map, clientIp);
        return result;
    }

    /**
     * 2025-01-23 重构
     * @param clientIp
     * @return
     */
    public static final String checkClientIp(final String clientIp){
        final String result = p_checkClientX(p_ip_rule_map, p_ip_lock_map, clientIp);
        return result;
    }

    /**
     * 2023-07-13
     * 2023-09-08 新增clientIp参数
     * 2025-01-23 重构
     * 每次请求
     */
    private static String p_checkClientX(final ConcurrentHashMap<String, String> ruleMap, final ConcurrentHashMap<String, Long> lockMap, final String clientX){
        if(!"yes".equals(p_enable)){
            return "success";
        }
        //
        final String requestURI = ServletUtil.getRequestURI();
        String localClientX = clientX;
        if(StringUtil.isBlank(localClientX)){
            localClientX = "*";
        }
        final String key = localClientX + "#" + requestURI;
        if(lockMap.containsKey(key)){
            return "limiter_error_locked";
        }
        final long nowStamp = System.currentTimeMillis();
        if(!LocalCacheUtil.hasKey(key)){
            List<Long> stampList = ListUtil.getSyncList();
            stampList.add(nowStamp);
            // 2026-08-20 构建替代redis的本地API持久化缓存框架
            // 限流时间窗改纯内存存储：短命数据无需重启恢复，且跳过序列化消除共享List并发修改的CME竞态
            LocalCacheUtil.set(key, stampList, p_stamplist_expire, true);
            return "success";
        }
        //
        Object value = LocalCacheUtil.get(key);
        List<Long> reqStampList = (List<Long>) value;
        Integer reqStampSize = reqStampList.size();
        if(reqStampSize <= 0){
            reqStampList.add(nowStamp);
            LocalCacheUtil.set(key, reqStampList, p_stamplist_expire, true);
            return "success";
        }
        //
        final String rule = p_getRule(ruleMap, key);  // 获取规则
        String[] ruleArray = StringUtil.split(rule, "#");
        final String ruleCountMaxStr = ruleArray[0];  // 限定时间，请求的最大次数
        final long ruleCountMax = Long.parseLong(ruleCountMaxStr);
        final String ruleWithinTimeStr = ruleArray[1];  // 限定时间，单位秒
        long ruleWithinTime = Long.parseLong(ruleWithinTimeStr);
        ruleWithinTime *= 1000;  // 毫秒
        // 计算限定时间内的已请求数量
        long reqCount = 0;
        synchronized (reqStampList){
            int size = reqStampList.size();
            for(int i = size - 1; i >= 0; i--){
                long reqStamp = reqStampList.get(i);
                long cha = nowStamp - reqStamp;
                // 只统计限定时间之内的
                if(cha <= ruleWithinTime){
                    reqCount++;
                    continue;
                }
                // 限定时间之外的直接移除
                reqStampList.remove(i);
            }
        }
        //
        if(reqCount > ruleCountMax){
            lockMap.put(key, nowStamp);
            reqStampList.clear();
            LocalCacheUtil.set(key, reqStampList, p_stamplist_expire, true);
            return "limiter_error_locked";
        }
        //
        reqStampList.add(nowStamp);
        LocalCacheUtil.set(key, reqStampList, p_stamplist_expire, true);
        return "success";
    }

    /**
     * 2025-01-24
     * 获取规则
     * @param ruleMap
     * @return
     */
    private static String p_getRule(final ConcurrentHashMap<String, String> ruleMap, final String key){
        // 该判断同时包含了规则：clientx#api，*#api
        if(ruleMap.containsKey(key)){
            final String rule = ruleMap.get(key);
            return rule;
        }
        // 该判断同时包含了规则：clientx#*，*#*
        final String[] array = StringUtil.split(key, "#");
        final String clientX = array[0];
        // final String api = array[1];
        final String key2 = clientX + "#*";
        if(ruleMap.containsKey(key2)){
            final String rule2 = ruleMap.get(key2);
            return rule2;
        }
        // 包含规则：*#*
        final String rule3 = ruleMap.get("*#*");
        return rule3;
    }

}
