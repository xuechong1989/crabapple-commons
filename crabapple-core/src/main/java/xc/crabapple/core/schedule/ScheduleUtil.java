package xc.crabapple.core.schedule;

import xc.crabapple.core.collection.ObjectUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.reflect.ReflectUtil;

import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 2025-01-18
 * 任务调度工具类
 */
public final class ScheduleUtil {

    private static boolean p_inited = false;
    private static ScheduledExecutorService p_scheduledExecutorService = null;

    /**
     * 2025-01-18
     * 初始化
     */
    private static final void p_init(){
        if(p_inited){
            return;
        }
        p_inited = true;
        //
        ThreadFactory threadFactory = Thread.ofPlatform().factory();
        p_scheduledExecutorService = new ScheduledThreadPoolExecutor(10, threadFactory);
    }

    /**
     * 2025-01-18
     * @param object
     * @param methodName
     */
    public static final void clockByDelay(final Object object, final String methodName){
        clockByDelay(object, methodName, null, 0L, 1000L);
    }

    /**
     * 2025-01-18
     * @param object
     * @param methodName
     * @param delay 延迟时间，单位毫秒
     */
    public static final void clockByDelay(final Object object, final String methodName, final Long delay){
        clockByDelay(object, methodName, null, 0L, delay);
    }

    /**
     * 2025-01-18
     * @param object
     * @param methodName
     * @param methodArgs
     * @param delay 延迟时间，单位毫秒
     */
    public static final void clockByDelay(final Object object, final String methodName, final ArrayList<Object> methodArgs, final Long delay){
        clockByDelay(object, methodName, methodArgs, 0L, delay);
    }

    /**
     * 2025-01-18
     * @param object
     * @param methodName
     * @param initialDelay 初始化后，延迟执行时间，单位毫秒
     * @param delay 延迟时间，单位毫秒
     */
    public static final void clockByDelay(final Object object, final String methodName, final Long initialDelay, final Long delay){
        clockByDelay(object, methodName, null, initialDelay, delay);
    }

    /**
     * 2025-01-18
     * @param object
     * @param methodName
     * @param methodArgs
     * @param initialDelay 初始化后，延迟执行时间，单位毫秒
     * @param delay 延迟时间，单位毫秒
     */
    public static final void clockByDelay(final Object object, final String methodName, final ArrayList<Object> methodArgs, final Long initialDelay, final Long delay){
        p_init();
        p_scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    ReflectUtil.methodDynamic(object, methodName, methodArgs);
                }catch (Throwable throwable){
                    LogUtil.error("ScheduleUtil_clockByDelay_t1", throwable, object, methodName);
                }
            }
        }, initialDelay, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 2025-01-18
     * @param cls
     * @param methodName
     */
    public static final void clockByDelay(final Class cls, final String methodName){
        clockByDelay(cls, methodName, null, 0L, 1000L);
    }

    /**
     * 2025-01-18
     * @param cls
     * @param methodName
     * @param delay 延迟时间，单位毫秒
     */
    public static final void clockByDelay(final Class cls, final String methodName, final Long delay){
        clockByDelay(cls, methodName, null, 0L, delay);
    }

    /**
     * 2025-01-18
     * @param cls
     * @param methodName
     * @param methodArgs
     * @param delay 延迟时间，单位毫秒
     */
    public static final void clockByDelay(final Class cls, final String methodName, final ArrayList<Object> methodArgs, final Long delay){
        clockByDelay(cls, methodName, methodArgs, 0L, delay);
    }

    /**
     * 2025-01-18
     * @param cls
     * @param methodName
     * @param initialDelay 初始化后，延迟执行时间，单位毫秒
     * @param delay 延迟时间，单位毫秒
     */
    public static final void clockByDelay(final Class cls, final String methodName, final Long initialDelay, final Long delay){
        clockByDelay(cls, methodName, null, initialDelay, delay);
    }

    /**
     * 2025-01-18
     * @param cls
     * @param methodName
     * @param methodArgs
     * @param initialDelay 初始化后，延迟执行时间，单位毫秒
     * @param delay 延迟时间，单位毫秒
     */
    public static final void clockByDelay(final Class cls, final String methodName, final ArrayList<Object> methodArgs, final Long initialDelay, final Long delay){
        p_init();
        p_scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                final String className = ObjectUtil.getClassname(cls);
                try {
                    ReflectUtil.methodStatic(className, methodName, methodArgs);
                }catch (Throwable throwable){
                    LogUtil.error("ScheduleUtil_clockByDelay_t5", throwable, className, methodName);
                }
            }
        }, initialDelay, delay, TimeUnit.MILLISECONDS);
    }

}