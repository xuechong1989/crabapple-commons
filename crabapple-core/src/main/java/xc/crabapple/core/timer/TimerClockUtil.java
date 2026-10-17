package xc.crabapple.core.timer;

import xc.crabapple.core.collection.ObjectUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.reflect.ReflectUtil;
import xc.crabapple.core.schedule.ScheduleUtil;

import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2016-12-10 1.0.0
 * 2020-06-15 6.1.0 由TimerManager重命名为TimerClockUtil
 * 2025-01-18 移除统一入口clock方法与bean对象传参
 * 计时器工具类
 * 秒，分，时，日，月，年，星期
 * 目前欠缺：星期
 */
public final class TimerClockUtil extends Thread{

    private static boolean p_inited = false;
    private static Map<String, SnowTask> p_taskMap = null;
    private static Map<String, String> p_keyMap = null;

    /**
     * 2025-01-18
     * @param cls
     * @param methodName
     * @param points 执行的时间点，格式如：****-**-** 01:30:00
     * @return
     */
    public static final String clockByPoints(final Class cls, final String methodName, final String points){
        String id = clockByPoints(null, cls, methodName, null, points, "no");
        return id;
    }

    /**
     * 2025-01-18
     * @param id
     * @param cls
     * @param methodName
     * @param points 执行的时间点，格式如：****-**-** 01:30:00
     * @return
     */
    public static final String clockByPoints(final String id, final Class cls, final String methodName, final String points){
        String localId = clockByPoints(id, cls, methodName, null, points, "no");
        return localId;
    }

    /**
     * 2025-01-18
     * @param cls
     * @param methodName
     * @param methodArgs
     * @param points 执行的时间点，格式如：****-**-** 01:30:00
     * @return
     */
    public static final String clockByPoints(final Class cls, final String methodName, final ArrayList<Object> methodArgs, final String points){
        String id = clockByPoints(null, cls, methodName, methodArgs, points, "no");
        return id;
    }

    /**
     * 2025-01-18
     * @param id
     * @param cls
     * @param methodName
     * @param methodArgs
     * @param points 执行的时间点，格式如：****-**-** 01:30:00
     * @return
     */
    public static final String clockByPoints(final String id, final Class cls, final String methodName, final ArrayList<Object> methodArgs, final String points){
        String localId = clockByPoints(id, cls, methodName, methodArgs, points, "no");
        return localId;
    }

    /**
     * 2025-01-18
     * @param cls
     * @param methodName
     * @param points 执行的时间点，格式如：****-**-** 01:30:00
     * @param immediately
     * @return
     */
    public static final String clockByPoints(final Class cls, final String methodName, final String points, final String immediately){
        String id = clockByPoints(null, cls, methodName, null, points, immediately);
        return id;
    }

    /**
     * 2017-05-27 1.0.0
     * 2025-01-18
     * 方法级计时器
     * @param id 计时器唯一标识
     * @param methodName 格式如xc.crabapple.demo.Test, 方法必须为public
     * @param methodArgs 回调方法的参数
     * @param points 执行的时间点，格式如：****-**-** 01:30:00
     */
    public static final String clockByPoints(final String id, final Class cls, final String methodName, final ArrayList<Object> methodArgs, final String points, final String immediately){
        p_init();
        String localId = id;
        if(StringUtil.isBlank(localId)){
            localId = RandomUtil.getStrWithStamp(32);
        }
        final String value = p_keyMap.get(localId);
        if(!StringUtil.isBlank(value)){
            final String className = ObjectUtil.getClassname(cls);
            LogUtil.error("TimerClockUtil_clockByPoints_1", localId, className, methodName, methodArgs);
            return localId;
        }
        p_keyMap.put(localId, "yes");
        //
        if("yes".equals(immediately)){
            final String className = ObjectUtil.getClassname(cls);
            try{
                ReflectUtil.methodStatic(className, methodName, methodArgs);
            }catch(Throwable throwable){
                LogUtil.error("TimerClockUtil_clockByPoints_t2", throwable, localId, className, methodName, immediately);
            }
        }
        //
        p_task(new SnowTask(localId){
            @Override
            public void executeP(){
                final String className = ObjectUtil.getClassname(cls);
                try{
                    ReflectUtil.methodStatic(className, methodName, methodArgs);
                }catch(Throwable throwable){
                    LogUtil.error("TimerClockUtil_clockByPoints_t3", throwable, className, methodName);
                }
            }
        }, points);
        return localId;
    }

    /**
     * 2025-01-18
     * @param object
     * @param methodName
     * @param points 执行的时间点，格式如：****-**-** 01:30:00
     * @return
     */
    public static final String clockByPoints(final Object object, final String methodName, final String points){
        String id = clockByPoints(null, object, methodName, null, points, "no");
        return id;
    }

    /**
     * 2025-01-18
     * @param id
     * @param object
     * @param methodName
     * @param points 执行的时间点，格式如：****-**-** 01:30:00
     * @return
     */
    public static final String clockByPoints(final String id, final Object object, final String methodName, final String points){
        String localId = clockByPoints(id, object, methodName, null, points, "no");
        return localId;
    }

    /**
     * 2025-01-18
     * @param object
     * @param methodName
     * @param methodArgs
     * @param points 执行的时间点，格式如：****-**-** 01:30:00
     * @return
     */
    public static final String clockByPoints(final Object object, final String methodName, final ArrayList<Object> methodArgs, final String points){
        String id = clockByPoints(null, object, methodName, methodArgs, points, "no");
        return id;
    }

    /**
     * 2025-01-18
     * @param id
     * @param object
     * @param methodName
     * @param methodArgs
     * @param points 执行的时间点，格式如：****-**-** 01:30:00
     * @return
     */
    public static final String clockByPoints(final String id, final Object object, final String methodName, final ArrayList<Object> methodArgs, final String points){
        String localId = clockByPoints(id, object, methodName, methodArgs, points, "no");
        return localId;
    }

    /**
     * 2017-03-22 1.0.0
     * 2025-01-18
     * @param id
     * @param object
     * @param methodName
     * @param methodArgs
     * @param points 执行的时间点，格式如：****-**-** 01:30:00
     */
    public static final String clockByPoints(final String id, final Object object, final String methodName, final ArrayList<Object> methodArgs, final String points, final String immediately){
        p_init();
        String localId = id;
        if(StringUtil.isBlank(localId)){
            localId = RandomUtil.getStrWithStamp(32);
        }
        String value = p_keyMap.get(localId);
        if(!StringUtil.isBlank(value)){
            // 2026-08-14 系统与所有数据库表均统一时间格式
            // 不传 object/methodArgs 对象本身，只传类名——避免 Jackson 序列化触发 getter 副作用
            String dupClassName = object == null ? "null" : object.getClass().getName();
            LogUtil.error("TimerClockUtil_clockByPoints_11", "id值重复", localId, "对象类", dupClassName, "方法名", methodName);
            return localId;
        }
        p_keyMap.put(localId, "yes");
        //
        if("yes".equals(immediately)){
            try{
                ReflectUtil.methodDynamic(object, methodName, methodArgs);
            }catch(Throwable throwable){
                // 2026-08-14 系统与所有数据库表均统一时间格式 同上，不传对象只传类名
                String immClassName = object == null ? "null" : object.getClass().getName();
                LogUtil.error("TimerClockUtil_clockByPoints_t12", throwable,
                        "定时任务立即执行异常", "对象类", immClassName, "方法名", methodName);
            }
        }
        //
        p_task(new SnowTask(localId){
            @Override
            public void executeP(){
                try{
                    ReflectUtil.methodDynamic(object, methodName, methodArgs);
                }catch(Throwable throwable){
                    // 2026-08-14 系统与所有数据库表均统一时间格式
                    // 不传 object/methodArgs 对象本身，只传类名——避免 LogUtil Jackson 全量序列化触发 getter 副作用导致 Druid 连接泄漏
                    String objectClassName = object == null ? "null" : object.getClass().getName();
                    LogUtil.error("TimerClockUtil_clockByPoints_t13", throwable,
                            "定时任务执行异常", "对象类", objectClassName, "方法名", methodName);
                }
            }
        }, points);
        return localId;
    }

    /**
     * 2025-01-18
     * @param cls
     * @param methodName
     * @param delay 延迟时间，单位毫秒，如5000L
     * @return
     */
    public static final String clockByDelay(final Class cls, final String methodName, final Long delay){
        String id = clockByDelay(null, cls, methodName, null, 0L, delay);
        return id;
    }

    /**
     * 2025-01-18
     * @param id
     * @param cls
     * @param methodName
     * @param delay 延迟时间，单位毫秒，如5000L
     * @return
     */
    public static final String clockByDelay(final String id, final Class cls, final String methodName, final Long delay){
        String localId = clockByDelay(id, cls, methodName, null, 0L, delay);
        return localId;
    }

    /**
     * 2025-01-18
     * @param cls
     * @param methodName
     * @param initialDelay 第一次执行方法时的延迟时间执行，单位毫秒，如2000L
     * @param delay 延迟时间，单位毫秒，如5000L
     * @return
     */
    public static final String clockByDelay(final Class cls, final String methodName, final Long initialDelay, final Long delay){
        String id = clockByDelay(null, cls, methodName, null, initialDelay, delay);
        return id;
    }

    /**
     * 2025-01-18
     * @param id
     * @param cls
     * @param methodName
     * @param initialDelay 第一次执行方法时的延迟时间执行，单位毫秒，如2000L
     * @param delay 延迟时间，单位毫秒，如5000L
     * @return
     */
    public static final String clockByDelay(final String id, final Class cls, final String methodName, final Long initialDelay, final Long delay){
        String localId = clockByDelay(id, cls, methodName, null, initialDelay, delay);
        return localId;
    }

    /**
     * 2025-01-18
     * @param cls
     * @param methodName
     * @param methodArgs
     * @param delay 延迟时间，单位毫秒，如5000L
     * @return
     */
    public static final String clockByDelay(final Class cls, final String methodName, final ArrayList<Object> methodArgs, final Long delay){
        String id = clockByDelay(null, cls, methodName, methodArgs, 0L, delay);
        return id;
    }

    /**
     * 2025-01-18
     * @param id
     * @param cls
     * @param methodName
     * @param methodArgs
     * @param delay 延迟时间，单位毫秒，如5000L
     * @return
     */
    public static final String clockByDelay(final String id, final Class cls, final String methodName, final ArrayList<Object> methodArgs, final Long delay){
        String localId = clockByDelay(id, cls, methodName, methodArgs, 0L, delay);
        return localId;
    }

    /**
     * 2017-05-27 1.0.0
     * 2025-01-18
     * 方法级计时器
     * @param id 计时器唯一标识
     * @param methodName 格式如xc.crabapple.demo.Test, 方法必须为public
     * @param methodArgs 回调方法的参数
     * @param initialDelay 第一次执行方法时的延迟时间执行，单位毫秒，如2000L
     * @param delay 延迟时间，单位毫秒，如5000L
     */
    public static final String clockByDelay(final String id, final Class cls, final String methodName, final ArrayList<Object> methodArgs, final Long initialDelay, final Long delay){
        p_init();
        String localId = id;
        if(StringUtil.isBlank(localId)){
            localId = RandomUtil.getStrWithStamp(32);
        }
        String value = p_keyMap.get(localId);
        if(!StringUtil.isBlank(value)){
            final String className = ObjectUtil.getClassname(cls);
            LogUtil.error("TimerClockUtil_clockByDelay_1", localId, className, methodName, methodArgs);
            return localId;
        }
        p_keyMap.put(localId, "yes");
        //
        ScheduleUtil.clockByDelay(cls, methodName, methodArgs, initialDelay, delay);
        if(false){
            p_task(new SnowTask(localId){
                @Override
                public void executeI(){
                    final String className = ObjectUtil.getClassname(cls);
                    try{
                        ReflectUtil.methodStatic(className, methodName, methodArgs);
                    }catch(Throwable throwable){
                        LogUtil.error("TimerClockUtil_clockByDelay_t2", throwable, className, methodName);
                    }
                }
            }, initialDelay, delay);
        }
        return localId;
    }

    /**
     * 2025-01-18
     * @param object
     * @param methodName
     * @param methodArgs
     * @param delay 延迟时间，单位毫秒，如5000L
     * @return
     */
    public static final String clockByDelay(final Object object, final String methodName, final ArrayList<Object> methodArgs, final Long delay){
        String id = clockByDelay(null, object, methodName, methodArgs, 0L, delay);
        return id;
    }

    /**
     * 2025-01-18
     * @param object
     * @param methodName
     * @param initialDelay 第一次执行方法时的延迟时间执行，单位毫秒，如2000L
     * @param delay 延迟时间，单位毫秒，如5000L
     * @return
     */
    public static final String clockByDelay(final Object object, final String methodName, final Long initialDelay, final Long delay){
        String id = clockByDelay(null, object, methodName, null, initialDelay, delay);
        return id;
    }

    /**
     * 2025-01-18
     * @param id
     * @param object
     * @param methodName
     * @param initialDelay 第一次执行方法时的延迟时间执行，单位毫秒，如2000L
     * @param delay 延迟时间，单位毫秒，如5000L
     * @return
     */
    public static final String clockByDelay(final String id, final Object object, final String methodName, final Long initialDelay, final Long delay){
        String localId = clockByDelay(id, object, methodName, null, initialDelay, delay);
        return localId;
    }

    /**
     * 2025-01-18
     * @param object
     * @param methodName
     * @param delay 延迟时间，单位毫秒，如5000L
     * @return
     */
    public static final String clockByDelay(final Object object, final String methodName, final Long delay){
        String id = clockByDelay(null, object, methodName, null, 0L, delay);
        return id;
    }

    /**
     * 2025-01-18
     * @param id
     * @param object
     * @param methodName
     * @param delay 延迟时间，单位毫秒，如5000L
     * @return
     */
    public static final String clockByDelay(final String id, final Object object, final String methodName, final Long delay){
        String localId = clockByDelay(id, object, methodName, null, 0L, delay);
        return localId;
    }

    /**
     * 2025-01-18
     * @param id
     * @param object
     * @param methodName
     * @param methodArgs
     * @param delay 延迟时间，单位毫秒，如5000L
     * @return
     */
    public static final String clockByDelay(final String id, final Object object, final String methodName, final ArrayList<Object> methodArgs, final Long delay){
        String localId = clockByDelay(id, object, methodName, methodArgs, 0L, delay);
        return localId;
    }

    /**
     * 2017-03-22 1.0.0
     * 2025-01-18
     * @param id
     * @param object
     * @param methodName
     * @param methodArgs
     * @param initialDelay 第一次执行方法时的延迟时间执行，单位毫秒，如2000L
     * @param delay 延迟时间，单位毫秒，如5000L
     * @return
     */
    public static final String clockByDelay(final String id, final Object object, final String methodName, final ArrayList<Object> methodArgs, final Long initialDelay, final Long delay){
        p_init();
        String localId = id;
        if(StringUtil.isBlank(localId)){
            localId = RandomUtil.getStrWithStamp(32);
        }
        String value = p_keyMap.get(localId);
        if(!StringUtil.isBlank(value)){
            // 2026-08-14 系统与所有数据库表均统一时间格式
            // 不传 object/methodArgs 对象本身，只传类名——避免 Jackson 序列化触发 getter 副作用
            String dupClassName = object == null ? "null" : object.getClass().getName();
            LogUtil.error("TimerClockUtil_clockByDelay_11", "id值重复", localId, "对象类", dupClassName, "方法名", methodName);
            return localId;
        }
        p_keyMap.put(localId, "yes");
        //
        ScheduleUtil.clockByDelay(object, methodName, methodArgs, initialDelay, delay);
        if(false){
            p_task(new SnowTask(localId){
                @Override
                public void executeI(){
                    try{
                        ReflectUtil.methodDynamic(object, methodName, methodArgs);
                    }catch(Throwable throwable){
                        // 2026-08-14 系统与所有数据库表均统一时间格式 同上，不传对象只传类名
                        String delayClassName = object == null ? "null" : object.getClass().getName();
                        LogUtil.error("TimerClockUtil_clockByDelay_t12", throwable,
                                "定时任务延迟执行异常", "对象类", delayClassName, "方法名", methodName);
                    }
                }
            }, initialDelay, delay);
        }
        return localId;
    }

    private static final void p_init(){
        if(p_inited){
            return;
        }
        p_inited = true;
        //
        if(p_taskMap == null){
            p_taskMap = new ConcurrentHashMap<>(32);
        }
        if(p_keyMap == null){
            p_keyMap = new ConcurrentHashMap<>(32);
        }
    }

    /**
     * 2017-03-22 1.0.0
     * @param id
     */
    public static final void remove(final String id){
        if(StringUtil.isBlank(id)){
            return;
        }
        p_init();
        if(p_keyMap != null){
            p_keyMap.remove(id);
        }
        p_taskRemove(id);
    }

    /**
     * 2017-03-22 1.0.0
     * 启动计时器，时间点，毫秒
     * @param snowTask 任务实体类
     * @param initialDelay 第一次执行方法时的延迟时间执行，单位毫秒，如2000L
     * @param delay 延迟时间，单位毫秒，如5000L
     */
    private static final void p_task(final SnowTask snowTask, final Long initialDelay, final Long delay){
        String name = snowTask.getName();
        SnowTask snowTaskTmp = p_taskMap.get(name);
        if(snowTaskTmp != null){
            LogUtil.error("TimerClockUtil_p_task_2", name, initialDelay, delay);
            return;
        }
        p_taskMap.put(name, snowTask);
        Long localInitialDelay = initialDelay;
        if(localInitialDelay == null || localInitialDelay <= 0L){
            localInitialDelay = 0L;
        }
        Long localDelay = delay;
        if(localDelay == null || localDelay <= 0L){
            localDelay = 1000L;
        }
        snowTask.setDelay(localDelay);
        Timer timer = new Timer();
        timer.schedule(snowTask, localInitialDelay, localDelay);
    }

    /**
     * 2017-03-22 1.0.0
     * 启动计时器，时间点，毫秒
     * @param snowTask 任务实体类
     * @param points 执行的时间点，格式如：****-**-** 01:30:00
     */
    private static final void p_task(final SnowTask snowTask, final String points){
        String name = snowTask.getName();
        SnowTask snowTaskTmp = p_taskMap.get(name);
        if(snowTaskTmp != null){
            LogUtil.error("TimerClockUtil_p_task_4", name, points);
            return;
        }
        p_taskMap.put(name, snowTask);
        snowTask.setPoints(points);
        Timer timer = new Timer();
        timer.schedule(snowTask, 0L, 1000L);
    }

    /**
     * 2017-05-27 1.0.0
     * 取消计时器任务
     */
    private static final void p_taskRemove(final String taskName){
        SnowTask snowTask = p_taskMap.get(taskName);
        if(snowTask == null){
            return;
        }
        snowTask.cancel();
        p_taskMap.remove(taskName);
    }

}