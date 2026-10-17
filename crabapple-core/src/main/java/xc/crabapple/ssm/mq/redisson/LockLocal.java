package xc.crabapple.ssm.mq.redisson;

import xc.crabapple.core.log.LogUtil;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * 2020-12-12 7.1.0
 * 本地锁
 */
public final class LockLocal {

    private String id;  //一般一个方法用一个相同的id
    private Thread p_currentThread;  //当前拥有锁的线程
    private Boolean p_locked = false;
    private final Vector<Thread> p_vector = new Vector<>();  //存储阻塞线程

    /**
     * 2020-12-12 7.1.0
     * @param timeout 单位毫秒
     */
    public final void lock(final long timeout){
        synchronized(this){
            if(timeout <= 0){
               LogUtil.error("LockLocal_lock_1", "您输入的时间小于0");
               return;
            }
            try{
                long remain = timeout;
                long start = System.currentTimeMillis();
                long end = start + remain;
                while(p_locked){
                    Thread thread = Thread.currentThread();
                    if (remain <= 0){
                        //如果抢到锁了，则从阻塞队列中删除自己（如果没有返回false）
                        p_vector.remove(thread);
                    }
                    //时间复杂度o(n)
                    if(!p_vector.contains(thread)){
                        p_vector.add(thread);  //孤儿线程，阻塞队列线程
                    }
                    //若任务时间大于了所设置的锁超时时间，系统自动再分配增加指定数值的超时时间
                    if(remain <= 0){
                        LogUtil.error("LockLocal_lock_2", "任务执行时间已超过同步锁最大超时时间，系统已自动增加5000毫秒，请尽快把超时时间设置再大些！");
                        remain = 5000;
                    }
                    //该线程放弃关联引用对象的monitor的所有权，并释放monitor的所有权给其它线程争抢的机会
                    wait(remain);
                    remain = end - System.currentTimeMillis();  //剩余时间
                }
                Thread thread = Thread.currentThread();
                //如果抢到锁了，则从阻塞队列中删除自己（如果没有返回false）
                p_vector.remove(thread);
                p_locked = true;
                p_currentThread = thread;  //拿到锁的线程
            }catch(Throwable throwable){
                LogUtil.error("LockLocal_lock_t1", throwable, "", id, p_locked, timeout);
            }
        }
    }

    public final void unlock() {
        synchronized(this){
            //获得锁的线程解锁
            Thread thread = Thread.currentThread();
            if(p_currentThread == thread){
                p_locked = false;
                //唤醒睡眠线程强锁，这里唤醒一个也可以
                notify();
            }
        }
    }

    public final List<Thread> getBlockThreadList() {
        List<Thread> list = Collections.unmodifiableList(p_vector);
        return list;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

}
