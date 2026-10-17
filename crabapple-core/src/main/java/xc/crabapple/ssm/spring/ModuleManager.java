package xc.crabapple.ssm.spring;

import org.springframework.context.ApplicationContext;
import xc.crabapple.core.collection.ObjectUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.reflect.ReflectUtil;

import java.lang.reflect.Field;

/**
 * 2025-01-07
 * 模块管理器
 */
public class ModuleManager {

    /**
     * 2025-02-11
     * @return
     */
    public static final Object injection(){
        final Object object = injection(null, null);
        return object;
    }

    /**
     * 2025-01-08
     * @param moduleManagerClass
     * @return
     */
    public static final Object injection(final Class moduleManagerClass){
        final Object object = injection(null, moduleManagerClass);
        return object;
    }

    /**
     * 2025-01-07
     * @param applicationContext
     * @param moduleManagerClass
     * @return
     */
    public static final Object injection(final ApplicationContext applicationContext, final Class moduleManagerClass){
        ApplicationContext localApplicationContext = applicationContext;
        Class localModuleManagerClass = moduleManagerClass;
        if(localApplicationContext == null){
            localApplicationContext = SpringUtil.applicationContext;
        }
        if(localApplicationContext == null){
            return null;
        }
        if(localModuleManagerClass == null){
            localModuleManagerClass = SpringUtil.moduleManagerClass;
        }
        if(localModuleManagerClass == null){
            return null;
        }
        //
        final String classname = ObjectUtil.getClassname(localModuleManagerClass);
        final Object object = p_injection(localApplicationContext, classname);
        return object;
    }

    /**
     * 2024-04-27
     * 2025-04-28 兼容Service/ServiceImpl结尾的类名称；初始化时找不到的bean对象忽略。
     * 注入对象，用于MM模块管理器
     * @param applicationContext
     * @param classname
     */
    private static final Object p_injection(final ApplicationContext applicationContext, final String classname){
        String beanId = "";
        try{
            Object mm = ReflectUtil.getInstance(classname);
            //
            Field[] fieldList = mm.getClass().getDeclaredFields();
            for (Field field : fieldList) {
                field.setAccessible(true);
                final String fieldName = field.getName();
                beanId = fieldName;
                //
                if (StringUtil.isBlank(beanId)) {
                    continue;
                }
                //
                if ("instance".equals(beanId)) {
                    continue;
                }
                //
                try{
                    Object object = applicationContext.getBean(beanId);
                    if(object != null){
                        field.set(mm, object);  // 为指定对象(instance)的全局变量(field)赋值(object)（包括静态变量）
                        continue;
                    }
                }catch(Throwable throwable){

                }
                //
                if (StringUtil.lastsWith(fieldName, "Service")) {
                    beanId += "Impl";
                    try{
                        Object object = applicationContext.getBean(beanId);
                        if (object != null) {
                            field.set(mm, object);
                            continue;
                        }
                    }catch (Throwable throwable){

                    }
                }
            }
            return mm;
        }catch(Throwable throwable){
            LogUtil.error("ModuleManager_p_injection_t1", throwable, "Spring注入对象失败", beanId);
            return null;
        }
    }

}
