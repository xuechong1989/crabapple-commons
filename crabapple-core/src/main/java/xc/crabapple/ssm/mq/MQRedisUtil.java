package xc.crabapple.ssm.mq;

import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.json.jackson.JacksonUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.ssm.mq.redisson.LockUtil;
import xc.crabapple.ssm.redis.RedisDao;
import xc.crabapple.ssm.redis.RedisUtil;

import java.io.Serializable;
import java.util.List;

/**
 * 2020-08-04 6.4.0
 * 基于redis开发的分布式网络消息队列
 */
public final class MQRedisUtil {

    private static final String P_KEY_START = "xc_crabapple_mq_start";
    private static final String P_KEY_END = "xc_crabapple_mq_end";
    private static final String P_KEY_CONTENT = "xc_crabapple_mq_content";

    /**
     * 2020-10-05 6.7.0
     * 新增
     */
    public static final DaoMQ add(final DaoMQ daoMQ){
        p_handle("add", daoMQ);
        return daoMQ;
    }

    /**
     * 2020-10-05 6.7.0
     * 读取并删除
     */
    public static final DaoMQ remove(){
        DaoMQ daoMQ = p_handle("remove", null);
        return daoMQ;
    }

    /**
     * 2020-10-05 6.7.0
     */
    private static final DaoMQ p_handle(final String type, final DaoMQ daoMQ){
        if("add".equals(type)){
            String lockId = LockUtil.lock("xc_crabapple_MQRedisUtil_add", 60000);
            try{
                //更新索引
                String endStr = RedisUtil.getContent(P_KEY_END);
                long end = 0;
                if(StringUtil.isBlank(endStr)){
                    RedisDao startRedisDao = new RedisDao();
                    startRedisDao.setKey(P_KEY_START);
                    startRedisDao.setContent("0");
                    RedisUtil.set(startRedisDao);
                }else{
                    end = Long.parseLong(endStr);
                    end++;
                }
                RedisDao endRedisDao = new RedisDao();
                endRedisDao.setKey(P_KEY_END);
                endRedisDao.setContent(end + "");
                RedisUtil.set(endRedisDao);
                //新增内容
                String contentKey = P_KEY_CONTENT + "#" + end;
                String sdaoMQ = daoMQ.toString();
                RedisDao contentRedisDao = new RedisDao();
                contentRedisDao.setKey(contentKey);
                contentRedisDao.setContent(sdaoMQ);
                RedisUtil.set(contentRedisDao);
                return daoMQ;
            }catch(Throwable throwable){
                LogUtil.error("MQRedisUtil_p_handle_t1", throwable, daoMQ);
                return null;
            }finally {
                LockUtil.unlock(lockId);
            }
        }
        if("remove".equals(type)){
            String lockId = LockUtil.lock("xc_crabapple_MQRedisUtil_remove", 60000);
            try{
                //更新索引
                String startStr = RedisUtil.getContent(P_KEY_START);
                if(StringUtil.isBlank(startStr)){
                    return null;
                }
                String endStr = RedisUtil.getContent(P_KEY_END);
                if(StringUtil.isBlank(endStr)) {
                    return null;
                }
                long start = Long.parseLong(startStr);
                long end = Long.parseLong(endStr);
                if(start < end){
                    RedisDao startRedisDao = new RedisDao();
                    startRedisDao.setKey(P_KEY_START);
                    startRedisDao.setContent((start + 1) + "");
                    RedisUtil.set(startRedisDao);
                }else{
                    RedisUtil.remove(P_KEY_START);
                    RedisUtil.remove(P_KEY_END);
                }
                //删除内容
                String contentKey = P_KEY_CONTENT + "#" + start;
                RedisDao contentRedisDao = RedisUtil.get(contentKey);
                Serializable ssdaoMQ = contentRedisDao.getContent();
                String sdaoMQ = ssdaoMQ.toString();
                DaoMQ daoMQ1 = JacksonUtil.json2bean(sdaoMQ, DaoMQ.class);
                RedisUtil.remove(contentKey);
                return daoMQ1;
            }catch(Throwable throwable){
                LogUtil.error("MQRedisUtil_p_handle_t2", throwable, daoMQ);
                return null;
            }finally {
                LockUtil.unlock(lockId);
            }
        }
        //
        LogUtil.error("MQRedisUtil_p_handle_1", "type参数错误");
        return null;
    }

    /**
     * 2021-04-08 7.5.0
     * 队列是否为空
     * @return
     */
    public static final boolean isBlank(){
        List<String> list = RedisUtil.getKeyList(P_KEY_CONTENT);
        boolean flag = CollectionUtil.isBlank(list);
        return flag;
    }

}
