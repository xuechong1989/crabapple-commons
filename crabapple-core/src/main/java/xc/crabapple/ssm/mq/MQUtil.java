package xc.crabapple.ssm.mq;

import xc.crabapple.core.log.LogUtil;

/**
 * 2020-12-19 7.1.0
 * 消息队列
 */
public final class MQUtil {

    private static String p_type = "";

    /**
     * 2020-12-19 7.1.0
     */
    public static final void init(){
        p_type = "local";
        if("local".equals(p_type)){
            LogUtil.print("MQUtil_init_1", "消息队列 ...... 本地");
            MQLocalUtil.init();
            return;
        }
        //
        if("redis".equals(p_type)){
            LogUtil.print("MQUtil_init_2", "消息队列 ...... 分布式");
            return;
        }
        //
        LogUtil.print("MQUtil_init_3", "消息队列 ...... 禁用");
    }

    /**
     * 2021-04-08 7.5.0
     * 队列是否为空
     */
    public static final boolean isBlank(){
        if("local".equals(p_type)){
            boolean flag = MQLocalUtil.isBlank();
            return flag;
        }
        if("redis".equals(p_type)){
            boolean flag = MQRedisUtil.isBlank();
            return flag;
        }
        //
        return true;
    }

    /**
     * 2020-12-19 7.1.0
     */
    public static final DaoMQ add(final DaoMQ daoMQ){
        if("local".equals(p_type)){
            MQLocalUtil.add(daoMQ);
            return daoMQ;
        }
        if("redis".equals(p_type)){
            MQRedisUtil.add(daoMQ);
            return daoMQ;
        }
        //
        return null;
    }

    /**
     * 2020-12-19 7.1.0
     */
    public static final DaoMQ remove(){
        if("local".equals(p_type)){
            DaoMQ daoMQ = MQLocalUtil.remove();
            return daoMQ;
        }
        if("redis".equals(p_type)){
            DaoMQ daoMQ = MQRedisUtil.remove();
            return daoMQ;
        }
        //
        return null;
    }

}
