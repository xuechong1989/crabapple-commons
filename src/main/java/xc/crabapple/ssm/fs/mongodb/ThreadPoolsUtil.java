package xc.crabapple.ssm.fs.mongodb;

import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * xuechong 20180511 1.1.0
 * 多线程池管理工具类
 * 多个线程池，相当于多个队伍，分开排队，即避免单个队伍太长，提升效率
 * 每个线程相当于工人，每个Runnable实例相当于原料（待处理的任务）
 * 线程数 = CPU可用核心数/(1 - 阻塞系数)，其中阻塞系数的取值在0和1之间。
 */
public final class ThreadPoolsUtil {

    private static Map<String, ThreadPoolExecutor> poolMap = new ConcurrentHashMap<String, ThreadPoolExecutor>(64);
    private static Map<String, List<YczqRunnable>> groupMap = new ConcurrentHashMap<String, List<YczqRunnable>>(64);
    private static List<YczqRunnable> nonGroupList = new ArrayList<YczqRunnable>();

    /**
     * xuechong 20180517 1.1.0
     * 创建一个定长线程池，在系统初始化时执行该方法
     * @param poolName 线程池名称，唯一标志
     * @param poolSize 实际构建的线程数
     * @return
     */
    public synchronized static ThreadPoolExecutor addPoolFix(String poolName, Integer poolSize){
        if(CollectionUtil.isBlank(poolName)){
            LogUtil.error("ThreadPoolUtil_addPoolFix_1", "poolName is null");
            return null;
        }
        ThreadPoolExecutor threadPoolExecutor = poolMap.get(poolName);
        if(threadPoolExecutor == null){
            //定长线程池：(理论核心线程数, 理论线程最大值, 线程空闲时持续时间，线程空闲时持续时间单位，阻塞队列集合)
            BlockingQueue blockingQueue = new LinkedBlockingQueue<YczqRunnable>(10000);  //有界队列，防OOM
            threadPoolExecutor = new ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, blockingQueue);
            poolMap.put(poolName, threadPoolExecutor);
        }
        return threadPoolExecutor;
    }

    /**
     * xuechong 20180514 1.1.0
     * 添加一个线程
     * @param poolName 线程池名称
     * @param groupName 组名称，如果不涉及到线程组阻塞，请传空对象null，否则可能造成内存泄漏
     * @param runnable 任务对象
     */
    public synchronized static void addRunnable(String poolName, String groupName, YczqRunnable runnable){
        ThreadPoolExecutor threadPoolExecutor = getExecutor(poolName);
        if(threadPoolExecutor == null){
            LogUtil.error("ThreadPoolsUtil_addRunnable_1", "threadPoolExecutor_is_null");
            return;
        }
        if(StringUtil.isBlank(groupName)){
            nonGroupList.add(runnable);
        }else{
            List<YczqRunnable> groupList = getGroupList(poolName, groupName);
            groupList.add(runnable);
        }
        threadPoolExecutor.execute(runnable);
    }

    /**
     * xuechong 20180515 1.1.0
     * 获取指定线程池中指定组集合对象
     * @param poolName 线程池名称
     * @param groupName 组名称
     * @return
     */
    public synchronized static List<YczqRunnable> getGroupList(String poolName, String groupName){
        String groupKey = poolName + "_" + groupName;
        List<YczqRunnable> groupList = groupMap.get(groupKey);
        if(groupList == null){
            groupList = new ArrayList<YczqRunnable>();
            groupMap.put(groupKey, groupList);
        }
        return groupList;
    }

    /**
     * xuechong 20180515 1.1.0
     * 通过name获得一个指定的Runnable实例，若获取的实例不为空则表明此任务已在待执行列表中且尚未执行
     * @param poolName 线程池名称
     * @param runnableName 任务对象名称
     * @return
     */
    public static YczqRunnable getRunnable(String poolName, String runnableName){
        if(CollectionUtil.isBlank(poolName)){
            LogUtil.error("ThreadPoolUtil_getRunnable_1", poolName, runnableName);
            return null;
        }
        if(CollectionUtil.isBlank(runnableName)){
            LogUtil.error("ThreadPoolUtil_getRunnable_2", poolName, runnableName);
            return null;
        }
        for(YczqRunnable yczqRunnable : nonGroupList){
            String name = yczqRunnable.getName();
            if(runnableName.equals(name)){
                return yczqRunnable;
            }
        }
        LogUtil.error("ThreadPoolUtil_getRunnable_3", poolName, runnableName);
        return null;
    }

    /**
     * xuechong 20180514 1.1.0
     * 销毁指定的线程池，执行后队列和缓存队列都清空，即threadPoolExecutor.getQueue()为空
     * @param poolName 线程池名称
     */
    public synchronized static void destroyPool(String poolName){
        ThreadPoolExecutor threadPoolExecutor = getExecutor(poolName);
        threadPoolExecutor.shutdownNow();
        Set<String> set = groupMap.keySet();
        Iterator<String> iterator = set.iterator();
        while(iterator.hasNext()){
            String groupName = iterator.next();
            destroyGroup(poolName, groupName);
        }
        poolMap.remove(poolName);
    }

    /**
     * xuechong 20190225 2.0.1
     * 从集合中移除一个指定的runnable实例（任务），而不销毁此对象，从nonGroup中删除（无分组）
     * @param poolName 线程池名称
     * @param runnableName 任务对象名称
     * @return
     */
    public synchronized static boolean removeRunnable(String poolName, String runnableName){
        if(CollectionUtil.isBlank(poolName)){
            LogUtil.error("ThreadPoolUtil_removeRunnable_1", poolName, runnableName);
            return false;
        }
        if(CollectionUtil.isBlank(runnableName)){
            LogUtil.error("ThreadPoolUtil_removeRunnable_2", poolName, runnableName);
            return false;
        }
        ThreadPoolExecutor threadPoolExecutor = getExecutor(poolName);
        BlockingQueue<Runnable> blockingQueue = threadPoolExecutor.getQueue();
        int size = nonGroupList.size();
        for(int i = size - 1; i >= 0; i--){
            YczqRunnable yczqRunnable = nonGroupList.get(i);
            String name = yczqRunnable.getName();
            if(StringUtil.isBlank(name)){
                return true;
            }
            if(runnableName.equals(name)){
                nonGroupList.remove(yczqRunnable);
                //yczqRunnable.destroy();  //与destroyRunnable方法的区别即在于此
                threadPoolExecutor.remove(yczqRunnable);
                blockingQueue.remove(yczqRunnable);
                return true;
            }
        }
        return false;
    }

    /**
     * xuechong 20180514 1.1.0
     * 销毁指定线程池中指定任务组
     * @param poolName 线程池名称
     * @param groupName 任务对象名称
     * @return
     */
    public synchronized static boolean destroyGroup(String poolName, String groupName){
        ThreadPoolExecutor threadPoolExecutor = getExecutor(poolName);
        List<YczqRunnable> groupList = getGroupList(poolName, groupName);
        if(CollectionUtil.isBlank(groupList)){
            return true;
        }
        for(int i = groupList.size() - 1; i >= 0; i--){
            YczqRunnable yczqRunnable = groupList.get(i);
            if(yczqRunnable != null){
                yczqRunnable.destroy();
            }
            threadPoolExecutor.remove(yczqRunnable);  //把任务移出线程队列
            groupList.remove(i);
        }
        String groupKey = poolName + "_" + groupName;
        groupMap.remove(groupKey);
        return true;
    }

    /**
     * xuechong 20180514 1.1.0
     * 多线程阻塞
     * 注意执行此方法的线程组中的每个任务run方法最后一定要执行super.runCompleted()或super.runExit()
     * @param poolName 线程池名称
     * @param groupName 组名称
     * @param isDestroy 是否销毁
     * @param keepAliveTime 所有任务的耗时总时间，单位是毫秒，为0时表示不控制时间
     * @return (List<YczqRunnable>) 返回尚未完成的任务列表
     */
    public synchronized static List<YczqRunnable> blockGroup(String poolName, String groupName, boolean isDestroy, long keepAliveTime){
        boolean isTimeout = false;
        if(keepAliveTime > 0){
            isTimeout = true;
        }
        List<YczqRunnable> groupList = getGroupList(poolName, groupName);
        if(CollectionUtil.isBlank(groupList)){
            return null;
        }
        boolean isDone;  //是否所有任务都已完成
        long start = System.currentTimeMillis();
        while(true){
            if(isTimeout){
                long consume = System.currentTimeMillis() - start;
                if(consume > keepAliveTime){  //已经超时
                    List<YczqRunnable> blackList = new ArrayList<YczqRunnable>();
                    for(YczqRunnable yczqRunnable : groupList){
                        int status = yczqRunnable.getStatus();
                        if(status > 0){  //表示仍然有尚未执行结束的任务
                            blackList.add(yczqRunnable);
                        }
                    }
                    if(isDestroy){
                        destroyGroup(poolName, groupName);
                    }
                    return blackList;
                }
            }
            isDone = true;
            for(YczqRunnable yczqRunnable : groupList){
                int status = yczqRunnable.getStatus();
                if(status > 0){  //表示仍然有尚未执行结束的任务
                    isDone = false;
                    break;
                }
            }
            if(isDone){
                if(isDestroy){
                    destroyGroup(poolName, groupName);
                }
                return null;
            }
            Thread.yield();
        }
    }

    /**
     * xuechong 20180516 1.1.0
     * 指定线程池中空闲线程的数量
     * @param poolName
     * @return
     */
    public static long idleThreadNum(String poolName){
        ThreadPoolExecutor threadPoolExecutor = getExecutor(poolName);
        int maximumPoolSize = threadPoolExecutor.getMaximumPoolSize();  //总线程数
        int queueSize = threadPoolExecutor.getQueue().size();  //排队等待处理的任务数
        int activeCount = threadPoolExecutor.getActiveCount();  //正在处理的任务数
        long num = maximumPoolSize - queueSize - activeCount;
        num = num > 0 ? num : 0;
        return num;
    }

    /**
     * xuechong 20180514 1.1.0
     * 获取一个线程池对象
     * @param poolName 线程池名称
     * @return
     */
    public static ThreadPoolExecutor getExecutor(String poolName){
        if(CollectionUtil.isBlank(poolName)){
            LogUtil.error("ThreadPoolUtil_getExecutor_1", "poolName is null");
            return null;
        }
        ThreadPoolExecutor threadPoolExecutor = poolMap.get(poolName);
        if(threadPoolExecutor == null){
            LogUtil.error("ThreadPoolUtil_getExecutor_2", "value is null", poolName);
        }
        return threadPoolExecutor;
    }

}
