package xc.crabapple.core.thread;

import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2017-11-25 1.0.0
 */
public final class ThreadUtil {

    // 当前线程内的数据集合
    private static Map<String, Map<String, Object>> p_currentThreadDataMap = null;
    //
    public static ConcurrentHashMap<String, Object[]> p_concurrentHashMap = null;

    /**
     * 2025-11-06
     * 初始化子线程集合与监听程序
     */
    public static final void init(){
        if(p_concurrentHashMap != null) {
            return;
        }
        p_concurrentHashMap = new ConcurrentHashMap<>();
        final StringBuilder stringBuilder = new StringBuilder();
        Thread globalThread = new Thread(){
            @Override
            public void run() {
                while(true){
                    try{
                        stringBuilder.delete(0, stringBuilder.length());
                        for (Map.Entry<String, Object[]> entry : p_concurrentHashMap.entrySet()) {
                            String key = entry.getKey();
                            Object[] array = entry.getValue();
                            Thread thread = (Thread) array[0];
                            Thread.State state = thread.getState();
                            String stateName = state.name();
                            if("TERMINATED".equals(stateName)){
                                p_concurrentHashMap.remove(key);
                                continue;
                            }
                            //
                            String name = thread.getName();
                            if(stringBuilder.length() == 0){
                                stringBuilder.append(name);
                            }else{
                                stringBuilder.append(", ").append(name);
                            }
                        }
                        final int size = p_concurrentHashMap.size();
                        final String result = stringBuilder.toString();
                        LogUtil.info("ThreadUtil_init_run_5", "当前未结束的子线程列表", size, result);
                        sleep(600000L);
                    }catch (Throwable throwable){
                        LogUtil.error("ThreadUtil_init_t1", throwable);
                    }
                }
            }
        };
        globalThread.setPriority(Thread.NORM_PRIORITY);
        globalThread.start();
    }

    /**
     * 2025-11-01
     * 启动子线程
     * @param business
     */
    public static final synchronized void start(final Business business) {
        init();
        //
        final String location = business.getLocation();
        final Boolean showLog = business.getShowLog();
        if(StringUtil.isBlank(location)){
            LogUtil.error("ThreadUtil_start_20", "子线程启动失败，业务定位符为空", location, showLog);
            return;
        }
        //
        if(showLog){
            LogUtil.print("ThreadUtil_start_1", "即将进入子线程", location, showLog);
        }
        String id = RandomUtil.getStrWithStamp(32, false);
        long addTime = System.currentTimeMillis();
        String name = location + "#X#C#" + addTime;
        Thread thread = new Thread(){
            @Override
            public void run() {
                if(showLog){
                    LogUtil.print("ThreadUtil_start_run_1", "子线程中处理业务，开始", location, showLog);
                }
                try{
                    business.process();
                }finally {
                    destroyDataMap(name);
                    if(showLog){
                        LogUtil.print("ThreadUtil_start_run_2", "子线程中处理业务，结束", location, showLog);
                    }
                }
            }
        };
        thread.setName(name);
        thread.setPriority(Thread.NORM_PRIORITY);
        Object[] array = {thread, business};
        p_concurrentHashMap.put(id, array);
        thread.start();
        if(showLog){
            LogUtil.print("ThreadUtil_start_2", "成功启动子线程", location, showLog);
        }
    }

    /**
     * 2023-09-22
     * 添加当前线程的指定数据
     */
    public static final void putData(final String key, final Object value){
        final String threadName = getThreadName();
        putData(threadName, key, value);
    }

    /**
     * 2023-09-22
     * 添加指定线程的指定数据
     */
    public static final void putData(final String threadName, final String key, final Object value){
        if(StringUtil.isBlank(key)){
            LogUtil.error("ThreadUtil_putData_1", "key为空", threadName);
            return;
        }
        if(value == null){
            LogUtil.error("ThreadUtil_putData_2", "value为空", threadName, key);
            return;
        }
        //
        final Map<String, Object> map = getDataMap(threadName);
        map.put(key, value);
    }

    /**
     * 2023-09-22
     * 获取当前线程的指定数据
     */
    public static final Object getData(final String key){
        final String threadName = getThreadName();
        final Object value = getData(threadName, key);
        return value;
    }

    /**
     * 2023-09-22
     * 获取指定线程的指定数据
     */
    public static final Object getData(final String threadName, final String key){
        if(CollectionUtil.isBlank(p_currentThreadDataMap)){
            return null;
        }
        //
        Map<String, Object> map = p_currentThreadDataMap.get(threadName);
        if(map == null){
            return null;
        }
        Object value = map.get(key);
        return value;
    }

    /**
     * 2023-09-22
     * 获取当前线程的数据集合
     */
    public static final Map<String, Object> getDataMap(){
        final String threadName = getThreadName();
        Map<String, Object> map = getDataMap(threadName);
        return map;
    }

    /**
     * 2023-09-22
     * 获取指定线程的数据集合
     */
    public static final Map<String, Object> getDataMap(final String threadName){
        if(p_currentThreadDataMap == null){
            p_currentThreadDataMap = new ConcurrentHashMap<>();
        }
        Map<String, Object> map = p_currentThreadDataMap.get(threadName);
        if(map == null){
            map = new ConcurrentHashMap<>();
            p_currentThreadDataMap.put(threadName, map);
        }
        return map;
    }

    /**
     * 2023-09-22
     * 销毁当前线程的数据集合
     */
    public static final void destroyDataMap(){
        final String threadName = getThreadName();
        destroyDataMap(threadName);
    }

    /**
     * 2023-09-22
     * 销毁指定线程的数据集合
     * @param threadName
     */
    public static final void destroyDataMap(final String threadName){
        if(CollectionUtil.isBlank(p_currentThreadDataMap)){
            return;
        }
        //
        Map<String, Object> map = p_currentThreadDataMap.get(threadName);
        if(map != null){
            map.clear();
        }
        p_currentThreadDataMap.remove(threadName);
    }

    /**
     * 2023-09-22
     * 获取当前线程的名称（也是唯一标志位）
     * @return
     */
    public static final String getThreadName(){
        final Thread currentThread = Thread.currentThread();
        final String threadName = currentThread.getName();
        return threadName;
    }

    /**
     * 2023-09-22
     * 设置当前线程的名称（也是唯一标志位）
     */
    public static final String setThreadName(){
        final String threadName = setThreadName(null);
        return threadName;
    }

    /**
     * 2023-09-22
     * 设置当前线程的名称（也是唯一标志位）
     * @param threadName
     *        crabapple_thread_name_main_开头是主线程
     *        crabapple_thread_name_child_开头是子线程
     */
    public static final String setThreadName(final String threadName){
        String localThreadName = threadName;
        if(StringUtil.isBlank(localThreadName)){
            localThreadName = "crabapple_thread_name_main_" + RandomUtil.getStrWithStamp(32, false);
        }
        //
        final Thread currentThread = Thread.currentThread();
        currentThread.setName(localThreadName);
        return localThreadName;
    }

    /**
     * 2023-06-08
     * 判断当前线程是否为主线程，若是主线程返回true，否则返回false
     */
    public static final boolean isMain(){
        final Thread currentThread = Thread.currentThread();
        //
        String threadName = currentThread.getName();
        if(StringUtil.startsWith(threadName, "crabapple_thread_name_child_")){
            return false;
        }
        //
        ThreadGroup threadGroup = currentThread.getThreadGroup();
        String groupName = threadGroup.getName();
        if(StringUtil.equals(groupName, "main")){
            return true;
        }
        //
        boolean daemon = currentThread.isDaemon();
        if(daemon){
            return true;
        }
        //
        return false;
    }

    /**
     * 2023-10-04
     * 设置当前线程睡眠
     * @param seconds 单位秒
     * @return
     */
    public static final int sleep(final Integer seconds){
        final Long millis = seconds * 1000L;
        final int result = sleep(millis);
        return result;
    }

    /**
     * 2017-11-25 1.0.0
     * 设置当前线程睡眠
     * 在睡眠时由于其他操作导致该线程被终止时会抛出InterruptedException异常，同时返回值小于0
     * @param millis 单位毫秒
     */
    public static final int sleep(final Long millis){
        if(millis == null || millis.longValue() <= 0){
            return 0;
        }
        try{
            Thread.sleep(millis);
            return 0;
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            return -1;
        }catch(Throwable throwable){
            LogUtil.error("ThreadUtil_sleep_t1", throwable, millis);
            return -2;
        }
    }

}