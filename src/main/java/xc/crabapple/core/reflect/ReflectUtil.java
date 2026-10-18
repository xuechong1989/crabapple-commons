package xc.crabapple.core.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;

/**
 * 2017-03-24 1.0.0
 * 2024-08-16 新增反射相关方法
 * 反射工具类
 */
public final class ReflectUtil{

    /**
     * 2017-05-27 1.0.0
     * 2020-06-09 6.1.0
     * 执行某对象的方法
     * @param owner
     * @param methodName
     */
    public static final Object methodDynamic(final Object owner, final String methodName) throws Throwable{
        Object object = methodDynamic(owner, methodName, null);
        return object;
    }

    /**
     * 2017-05-27 1.0.0
     * 2020-06-09 6.1.0
     * 执行某对象的方法
     * @param owner
     * @param methodName
     * @param args 多态机制失效，基础数据类型与其对应的封装类型也失效，各参数类型必须完全保持一致
     */
    public static final Object methodDynamic(final Object owner, final String methodName, final ArrayList<Object> args) throws Throwable{
        if(CollectionUtil.isBlank(args)){
            Class<? extends Object> ownerClass = owner.getClass();
            Method method = ownerClass.getMethod(methodName);
            Object object = method.invoke(owner);
            return object;
        }
        //动态参数集合化，避免泛型丢失，导致参数类型不匹配 2021-04-16
        Class[] argsClass = new Class[1];
        argsClass[0] = args.getClass();
        //
        Class<? extends Object> ownerClass = owner.getClass();
        Method method = ownerClass.getMethod(methodName, argsClass);
        Object object = method.invoke(owner, args);
        return object;
    }

    /**
     * 2021-04-19 7.6.0
     * 仅限在crabapple内部使用
     * 执行某对象的方法
     * @param owner
     * @param methodName
     * @param args 多态机制失效，基础数据类型与其对应的封装类型也失效，各参数类型必须完全保持一致
     */
    public static final Object in_methodDynamic(final Object owner, final String methodName, final Object... args) throws Throwable{
        if(CollectionUtil.isBlank(args)){
            Class<? extends Object> ownerClass = owner.getClass();
            Method method = ownerClass.getMethod(methodName);
            Object object = method.invoke(owner);
            return object;
        }
        int n = args.length;
        Class[] argsClass = new Class[n];
        for(int i = 0, j = n; i < j; i++){
            Object arg = args[i];
            if(arg == null){
                continue;
            }
            argsClass[i] = arg.getClass();
        }
        //
        Class<? extends Object> ownerClass = owner.getClass();
        Method method = ownerClass.getMethod(methodName, argsClass);
        Object object = method.invoke(owner, args);
        return object;
    }

    /**
     * 2020-06-10 6.1.0
     * 执行某对象的方法
     * @param className
     * @param methodName
     */
    public static final Object methodDynamic(final String className, final String methodName) throws Throwable{
        Object object = methodDynamic(className, methodName, null);
        return object;
    }

    /**
     * 2017-05-27 1.0.0
     * 2020-06-09 6.1.0
     * 执行某对象的方法
     * @param className
     * @param methodName
     * @param args 多态机制失效，基础数据类型与其对应的封装类型也失效，各参数类型必须完全保持一致
     */
    public static final Object methodDynamic(final String className, final String methodName, final ArrayList<Object> args) throws Throwable{
        if(CollectionUtil.isBlank(args)){
            Class<?> ownerClass = Class.forName(className);
            Method method = ownerClass.getMethod(methodName);
            Object owner = getInstance(className);
            Object object = method.invoke(owner);
            return object;
        }
        //动态参数集合化，避免泛型丢失，导致参数类型不匹配 2021-04-16
        Class[] argsClass = new Class[1];
        argsClass[0] = args.getClass();
        //
        Class<?> ownerClass = Class.forName(className);
        Method method = ownerClass.getMethod(methodName, argsClass);
        Object owner = getInstance(className);
        Object object = method.invoke(owner, args);
        return object;
    }

    /**
     * 2021-04-19 7.6.0
     * 仅限在crabapple内部使用
     * 执行某对象的方法
     * @param className
     * @param methodName
     * @param args 多态机制失效，基础数据类型与其对应的封装类型也失效，各参数类型必须完全保持一致
     */
    public static final Object in_methodDynamic(final String className, final String methodName, final Object... args) throws Throwable{
        if(CollectionUtil.isBlank(args)){
            Class<?> ownerClass = Class.forName(className);
            Method method = ownerClass.getMethod(methodName);
            Object owner = getInstance(className);
            Object object = method.invoke(owner);
            return object;
        }
        int n = args.length;
        Class[] argsClass = new Class[n];
        for(int i = 0, j = n; i < j; i++){
            Object arg = args[i];
            if(arg == null){
                continue;
            }
            argsClass[i] = arg.getClass();
        }
        //
        Class<?> ownerClass = Class.forName(className);
        Method method = ownerClass.getMethod(methodName, argsClass);
        Object owner = getInstance(className);
        Object object = method.invoke(owner, args);
        return object;
    }

    /**
     * 2020-06-10 6.1.0
     * 执行某个类的静态方法
     * @param className
     * @param methodName
     */
    public static final Object methodStatic(final String className, final String methodName) throws Throwable{
        Object object = methodStatic(className, methodName, null);
        return object;
    }

    /**
     * 2017-05-27 1.0.0
     * 2020-06-09 6.1.0
     * 执行某个类的静态方法
     * @param className
     * @param methodName
     * @param args 多态机制失效，基础数据类型与其对应的封装类型也失效，各参数类型必须完全保持一致
     */
    public static final Object methodStatic(final String className, final String methodName, final ArrayList<Object> args) throws Throwable {
        if(CollectionUtil.isBlank(args)){
            Class<?> ownerClass = Class.forName(className);
            Method method = ownerClass.getMethod(methodName);
            Object object = method.invoke(null);
            return object;
        }
        //动态参数集合化，避免泛型丢失，导致参数类型不匹配 2021-04-16
        Class[] argsClass = new Class[1];
        argsClass[0] = args.getClass();
        //
        Class<?> ownerClass = Class.forName(className);
        Method method = ownerClass.getMethod(methodName, argsClass);
        Object object = method.invoke(null, args);
        return object;
    }

    /**
     * 2021-04-19 7.6.0
     * 仅限在crabapple内部使用
     * @param className
     * @param methodName
     * @param args 多态机制失效，基础数据类型与其对应的封装类型也失效，各参数类型必须完全保持一致
     */
    public static final Object in_methodStatic(final String className, final String methodName, final Object... args) throws Throwable {
        if(CollectionUtil.isBlank(args)){
            Class<?> ownerClass = Class.forName(className);
            Method method = ownerClass.getMethod(methodName);
            Object object = method.invoke(null);
            return object;
        }
        int n = args.length;
        Class[] argsClass = new Class[n];
        for(int i = 0, j = n; i < j; i++){
            Object arg = args[i];
            if(arg == null){
                continue;
            }
            argsClass[i] = arg.getClass();
        }
        //
        Class<?> ownerClass = Class.forName(className);
        Method method = ownerClass.getMethod(methodName, argsClass);
        Object object = method.invoke(null, args);
        return object;
    }

    /**
     * 2020-06-10 6.1.0
     * @param className 全包类名，如xc.crabapple.elastic.DaoElastic
     * @param className
     */
    public static final Object getInstance(final String className) throws Throwable {
        Object object = getInstance(className, null);
        return object;
    }

    /**
     * 2017-05-27 1.0.0
     * 2020-06-09 6.1.0
     * @param className 全包类名，如xc.crabapple.elastic.DaoElastic
     * @param args 要反射的类的构造方法的参数，注意是构造方法的，不是反射方法的
     */
    public static final Object getInstance(final String className, final ArrayList<Object> args) throws Throwable {
        if(CollectionUtil.isBlank(args)){
            Class<?> newoneClass = Class.forName(className);
            Constructor constructor = newoneClass.getConstructor();
            Object object = constructor.newInstance();
            return object;
        }
        //动态参数集合化，避免泛型丢失，导致参数类型不匹配 2021-04-16
        Class[] argsClass = new Class[1];
        argsClass[0] = args.getClass();
        //
        Class<?> newoneClass = Class.forName(className);
        Constructor constructor = newoneClass.getConstructor(argsClass);
        Object object = constructor.newInstance(args);
        return object;
    }

    /**
     * 2021-04-19 7.6.0
     * 仅限在crabapple内部使用
     * @param className 全包类名，如xc.crabapple.elastic.DaoElastic
     * @param args 要反射的类的构造方法的参数，注意是构造方法的，不是反射方法的
     */
    public static final Object in_getInstance(final String className, final Object... args) throws Throwable {
        if(CollectionUtil.isBlank(args)){
            Class<?> newoneClass = Class.forName(className);
            Constructor constructor = newoneClass.getConstructor();
            Object object = constructor.newInstance();
            return object;
        }
        Class[] argsClass = new Class[args.length];
        for(int i = 0, j = args.length; i < j; i++){
            Object arg = args[i];
            if(arg == null){
                continue;
            }
            argsClass[i] = arg.getClass();
        }
        //
        Class<?> newoneClass = Class.forName(className);
        Constructor constructor = newoneClass.getConstructor(argsClass);
        Object object = constructor.newInstance(args);
        return object;
    }

    /**
     * 2020-08-11 6.4.0
     * @param cls
     */
    public static final Object getInstance(final Class<? extends Object> cls){
        try{
            Constructor constructor = cls.getConstructor();  //升级前是type.newInstance(); 创建 JavaBean 对象
            if(constructor == null){
                return null;
            }
            //无参构造方法
            Object obj = constructor.newInstance();
            if(obj == null){
                LogUtil.error("ReflectUtil_getInstance_1");
            }
            return obj;
        }catch(Throwable throwable){
            LogUtil.error("ReflectUtil_getInstance_t1", throwable);
            return null;
        }
    }

    /**
     * 2024-08-16
     * 从实体对象中获取属性值
     */
    public static final Object getFieldValueFromObject(final Object obj, final String attName){
        if (obj == null){
            return null;
        }
        try {
            Method getMethod = obj.getClass().getDeclaredMethod("get" + StringUtil.toUpperCaseFirst(attName), new Class[] {});
            if(getMethod == null){
                return null;
            }
            Object val = getMethod.invoke(obj, new Object[] {});
            return val;
        } catch (Throwable throwable){
            LogUtil.error("ReflectUtil_getFieldValueFromObject_t1", throwable);
            return null;
        }
    }

    /**
     * 2024-08-16
     * 设置字段值
     */
    public static final void setFieldValue(final Object instObj, final String attName, final Object val){
        if (instObj == null){
            return;
        }
        Class<?> cls = instObj.getClass();
        Field field = null;
        Method setMethod = null;
        for (; cls != Object.class; cls = cls.getSuperclass()){
            try {
                setMethod = getSetMethod(cls,attName);
                if(setMethod !=  null){
                    setMethod.invoke(instObj, val);
                    break;
                }

                field = cls.getDeclaredField(attName);
                String attr = StringUtil.toUpperCaseFirst(attName);
                if (field != null){
                    setMethod = cls.getDeclaredMethod("set" + attr, new Class[] { field.getType() });
                    setMethod.invoke(instObj, val);
                    break;
                }
            } catch (Throwable throwable){
                LogUtil.error("ReflectUtil_setFieldValue_t1", throwable);
            }
        }
    }

    /**
     * 2024-08-16
     * @param cls
     * @param attName
     * @return
     */
    private static final Method getSetMethod(final Class cls, final String attName){
        String attr = StringUtil.toUpperCaseFirst(attName);
        Method[] aryMethod = cls.getDeclaredMethods();
        for(Method m : aryMethod){
            if( m.getName().equals("set" + attr)){
                return m;
            }
        }
        return null;
    }

    /**
     * 2024-08-16
     * 将数据进行转型。
     * @param typeName
     * 可能的值为：
     * int,
     * short,
     * long,
     * float,
     * double,
     * boolean,
     * String
     * @param value
     */
    public static final Object convertByActType(final String typeName, final String value){
        Object o = null;
        if (typeName.equals("int")){
            o = Integer.parseInt(value);
        } else if (typeName.equals("short")){
            o = Short.parseShort(value);
        } else if (typeName.equals("long")){
            o = Long.parseLong(value);
        } else if (typeName.equals("float")){
            o = Float.parseFloat(value);
        } else if (typeName.equals("double")){
            o = Double.parseDouble(value);
        } else if (typeName.equals("boolean")){
            o = Boolean.parseBoolean(value);
        } else if (typeName.equals("String")){
            o = value;
        }else{
            o = value;
        }
        return o;
    }

}