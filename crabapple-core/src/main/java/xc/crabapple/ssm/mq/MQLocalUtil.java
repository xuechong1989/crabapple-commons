package xc.crabapple.ssm.mq;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 2020-08-05 6.4.0
 * 本地消息队列
 */
public final class MQLocalUtil {

    private static boolean p_inited = false;  // 防止重复初始化
    private static ConcurrentLinkedQueue<DaoMQ> p_queue = null;

    /**
     * 2020-12-19 7.1.0
     */
    public static final void init(){
        if(p_inited){
            return;
        }
        p_inited = true;
        //
        p_queue = new ConcurrentLinkedQueue<>();
    }

    /**
     * 2019-07-24
     * 从队尾插入
     */
    public static final DaoMQ add(final DaoMQ daoMQ){
        if(daoMQ == null){
            return null;
        }
        p_queue.offer(daoMQ);
        return daoMQ;
    }

    /**
     * 2020-08-04 6.4.0
     * 从队头删除
     */
    public static final DaoMQ remove(){
        final DaoMQ daoMQ = p_queue.poll();
        return daoMQ;
    }

    /**
     * 2021-04-08 7.5.0
     * 队列是否为空
     */
    public static final boolean isBlank(){
        boolean flag = p_queue.isEmpty();
        return flag;
    }

}
