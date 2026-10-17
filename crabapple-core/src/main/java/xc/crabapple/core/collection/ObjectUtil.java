package xc.crabapple.core.collection;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.beanutils.converters.LongConverter;
import xc.crabapple.core.data.DataTypeUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.reflect.ReflectUtil;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 2017-10-21 1.0.0
 * 2024-07-16 重新整理copy方法，新增merge方法
 * 2024-08-16 新增copyByBeanUtils/copyByBeanUtils2方法
 */
public final class ObjectUtil{

    /**
     * 2025-05-16
     * 把源对象fromObject中的非空属性数据覆盖到目标对象toObject对应属性上。仅对get/set方法设置的属性有效。
     * 最终使用的就是目标对象toObject
     * @param fromObject 源对象
     * @param toObject 目标对象
     */
    public static final void merge(final Object fromObject, final Object toObject){
        if(toObject == null || toObject instanceof Class){
            LogUtil.error("ObjectUtil_merge_1", "目标对象toObject不能为空或Class类型，若深复制/深拷贝/深克隆与源对象fromObject同类型对象则使用clone方法。");
            return;
        }
        if(DataTypeUtil.isBaseType(toObject)){
            LogUtil.warn("ObjectUtil_merge_2", "目标对象toObject是基础数据类型，这对目标对象toObject无任何影响。");
            return;
        }
        if(fromObject == null || fromObject instanceof Class){
            LogUtil.warn("ObjectUtil_merge_3", "源对象fromObject为空或Class类型，这对目标对象toObject无任何影响。");
            return;
        }
        if(DataTypeUtil.isBaseType(fromObject)){
            LogUtil.warn("ObjectUtil_merge_4", "源对象fromObject是基础数据类型，这对目标对象toObject无任何影响。");
            return;
        }
        //
        p_merge(fromObject, toObject);
    }

    /**
     * 2025-05-16
     */
    private static final void p_merge(final Object fromObject, final Object toObject){
        ConvertUtilsBean convertUtilsBean = new ConvertUtilsBean();
        convertUtilsBean.register(new ObjectConverter(), Date.class);
        convertUtilsBean.register(new LongConverter(null), Long.class);
        PropertyUtilsBean propertyUtilsBean = new PropertyUtilsBean();
        BeanUtilsBean beanUtilsBean = new BeanUtilsBean(convertUtilsBean, propertyUtilsBean);
        //
        if (fromObject instanceof DynaBean){
            DynaProperty[] origDescriptors = ((DynaBean) fromObject).getDynaClass().getDynaProperties();
            for (int i = 0; i < origDescriptors.length; i++){
                String name = origDescriptors[i].getName();
                if (!propertyUtilsBean.isReadable(fromObject, name) || !propertyUtilsBean.isWriteable(toObject, name)){
                    continue;
                }
                Object value = ((DynaBean) fromObject).get(name);
                try{
                    beanUtilsBean.copyProperty(toObject, name, value);
                }catch (Throwable throwable){
                    LogUtil.error("ObjectUtil_p_merge_t1", throwable);
                }
            }
            return;
        }
        //
        if (fromObject instanceof Map){
            Iterator entries = ((Map) fromObject).entrySet().iterator();
            while(entries.hasNext()){
                Map.Entry entry = (Map.Entry) entries.next();
                String name = (String) entry.getKey();
                Object value = entry.getValue();
                if (!propertyUtilsBean.isWriteable(toObject, name)){
                    continue;
                }
                try{
                    beanUtilsBean.copyProperty(toObject, name, value);
                }catch (Throwable throwable){
                    LogUtil.error("ObjectUtil_p_merge_t2", throwable);
                }
            }
            return;
        }
        PropertyDescriptor[] origDescriptors = propertyUtilsBean.getPropertyDescriptors(fromObject);
        for (int i = 0; i < origDescriptors.length; i++){
            String name = origDescriptors[i].getName();
            if ("class".equals(name)){
                continue;
            }
            if (propertyUtilsBean.isReadable(fromObject, name) && propertyUtilsBean.isWriteable(toObject, name)){
                try {
                    Object value = propertyUtilsBean.getSimpleProperty(fromObject, name);
                    if (value != null){
                        if (value instanceof HashSet){
                            HashSet valMap = (HashSet) value;
                            if (!CollectionUtil.isBlank(valMap)){
                                beanUtilsBean.copyProperty(toObject, name, value);
                            }
                        } else {
                            beanUtilsBean.copyProperty(toObject, name, value);
                        }
                    }
                } catch (Throwable throwable){
                    LogUtil.error("ObjectUtil_p_merge_t3", throwable);
                }
            }
        }
    }

    /**
     * 2025-05-16
     * 深度克隆/复制/拷贝源对象fromObject，注意：新克隆对象必须与源对象fromObject类型相同。
     * 可同时克隆源对象及其父类的可访问属性与方法get/set配置的属性。不使用反射机制，更彻底，更安全，兼容性更好。
     * @param fromObject
     * @return
     * @param <T>
     */
    public static final <T> T clone(final T fromObject){
        if(fromObject == null || fromObject instanceof Class){
            return null;
        }
        if(DataTypeUtil.isBaseType(fromObject)){
            return fromObject;
        }
        //
        ByteArrayOutputStream byteArrayOutputStream = null;
        ObjectOutputStream objectOutputStream = null;
        ByteArrayInputStream byteArrayInputStream = null;
        ObjectInputStream objectInputStream = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(fromObject);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            byteArrayInputStream = new ByteArrayInputStream(bytes);
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            T t = (T) objectInputStream.readObject();
            return t;
        }catch (Throwable throwable){
            return null;
        }finally {
            IOUtil.close(objectInputStream, byteArrayInputStream, objectOutputStream, byteArrayOutputStream);
        }
    }

    /**
     * 2025-01-14
     * object对象转为类包全路径名称，如：new LocalCacheUtil() ==> xc.crabapple.core.cache.LocalCacheUtil
     * @param object
     * @return
     */
    public static final String getClassname(final Object object){
        final String classname = ClassUtil.getClassname(object);
        return classname;
    }

    /**
     * 2025-01-09
     * class对象转为类包全路径名称，如：LocalCacheUtil.class ==> xc.crabapple.core.cache.LocalCacheUtil
     * @param cls
     * @return
     */
    public static final String getClassname(final Class cls){
        final String classname = ClassUtil.getClassname(cls);
        return classname;
    }

    /**
     * 2024-07-16
     * 获取指定对象的指定属性名对应的值
     * @param object
     * @param fieldName
     * @return
     * @param <T>
     */
    public static final <T extends Object> T getValueByFieldName(final Object object, final String fieldName){
        Class cls = object.getClass();
        try{
            Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(object);
            if(value == null){
                return null;
            }
            T t = (T) value;
            return t;
        }catch(Throwable throwable){
            return null;
        }
    }

    /**
     * 2020-06-09 6.1.0
     * 集成了方法getDeclaredFields与方法getFields
     * @param cls
     */
    public static final List<Field> getFieldsList(final Class cls){
        List<Field> fieldList = new ArrayList<>();
        if(cls == null){
            return fieldList;
        }
        Class effectiveCls = cls;
        while(true){
            Field[] fieldArray = effectiveCls.getDeclaredFields();
            List<Field> tmpList = Arrays.asList(fieldArray);
            fieldList.addAll(tmpList);
            //得到父类,然后赋给自己，当父类为null的时候说明到达了最上层的父类(Object类)
            effectiveCls = effectiveCls.getSuperclass();
            if(effectiveCls == null){
                break;
            }
        }
        return fieldList;
    }

    /**
     * 2018-02-13 1.1.0
     * arg0是否等于arg1
     */
    public static final boolean equals(final Object arg0, final Object arg1){
        if(arg0 == null){
            return false;
        }
        if(arg1 == null){
            return false;
        }
        if(arg0 instanceof Integer){
            int aaa = ((Integer) arg0).intValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa == bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa == bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa == bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa == bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        if(arg0 instanceof Long){
            long aaa = ((Long) arg0).longValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa == bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa == bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa == bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa == bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        if(arg0 instanceof Float){
            float aaa = ((Float) arg0).floatValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa == bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa == bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa == bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa == bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        if(arg0 instanceof Double){
            double aaa = ((Double) arg0).doubleValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa == bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa == bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa == bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa == bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        if(arg0 instanceof String){
            String aaa = (String) arg0;
            if(arg1 instanceof String){
                String bbb = (String) arg1;
                if(aaa.equals(bbb)){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof StringBuilder){
                String bbb = arg1.toString();
                if(aaa.equals(bbb)){
                    return true;
                }
                return false;
            }
            return false;
        }
        if(arg0 instanceof StringBuilder){
            String aaa = arg0.toString();
            if(arg1 instanceof String){
                String bbb = (String) arg1;
                if(aaa.equals(bbb)){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof StringBuilder) {
                String bbb = arg1.toString();
                if(aaa.equals(bbb)){
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    /**
     * 2020-11-28 7.0.0
     * arg0是否小于等于arg1
     */
    public static final boolean elts(final Object arg0, final Object arg1){
        if(arg0 == null){
            return false;
        }
        if(arg1 == null){
            return false;
        }
        if(arg0 instanceof Integer){
            int aaa = ((Integer) arg0).intValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa <= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa <= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa <= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa <= bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        if(arg0 instanceof Long){
            long aaa = ((Long) arg0).longValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa <= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa <= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa <= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa <= bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        if(arg0 instanceof Float){
            float aaa = ((Float) arg0).floatValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa <= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa <= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa <= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa <= bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        if(arg0 instanceof Double){
            double aaa = ((Double) arg0).doubleValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa <= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa <= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa <= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa <= bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    /**
     * 2020-11-28 7.0.0
     * arg0是否大于等于arg1
     * @param arg0
     * @param arg1
     */
    public static final boolean egts(final Object arg0, final Object arg1){
        if(arg0 == null){
            return false;
        }
        if(arg1 == null){
            return false;
        }
        if(arg0 instanceof Integer){
            int aaa = ((Integer) arg0).intValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa >= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa >= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa >= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa >= bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        if(arg0 instanceof Long){
            long aaa = ((Long) arg0).longValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa >= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa >= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa >= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa >= bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        if(arg0 instanceof Float){
            float aaa = ((Float) arg0).floatValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa >= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa >= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa >= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa >= bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        if(arg0 instanceof Double){
            double aaa = ((Double) arg0).doubleValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa >= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa >= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa >= bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa >= bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    /**
     * 2020-11-28 7.0.0
     * arg0是否小于arg1
     * @param arg0
     * @param arg1
     */
    public static final boolean lts(final Object arg0, final Object arg1){
        if(arg0 == null){
            return false;
        }
        if(arg1 == null){
            return false;
        }
        if(arg0 instanceof Integer){
            int aaa = ((Integer) arg0).intValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa < bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa < bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa < bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa < bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        if(arg0 instanceof Long){
            long aaa = ((Long) arg0).longValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa < bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa < bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa < bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa < bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        if(arg0 instanceof Float){
            float aaa = ((Float) arg0).floatValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa < bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa < bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa < bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa < bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        if(arg0 instanceof Double){
            double aaa = ((Double) arg0).doubleValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa < bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa < bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa < bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa < bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    /**
     * 2020-11-28 7.0.0
     * arg0是否大于arg1
     * @param arg0
     * @param arg1
     */
    public static final boolean gts(final Object arg0, final Object arg1){
        if(arg0 == null){
            return false;
        }
        if(arg1 == null){
            return false;
        }
        if(arg0 instanceof Integer){
            int aaa = ((Integer) arg0).intValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa > bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa > bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa > bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa > bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        if(arg0 instanceof Long){
            long aaa = ((Long) arg0).longValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa > bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa > bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa > bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa > bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        if(arg0 instanceof Float){
            float aaa = ((Float) arg0).floatValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa > bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa > bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa > bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa > bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        if(arg0 instanceof Double){
            double aaa = ((Double) arg0).doubleValue();
            if(arg1 instanceof Integer){
                int bbb = ((Integer) arg1).intValue();
                if(aaa > bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Long){
                long bbb = ((Long) arg1).longValue();
                if(aaa > bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Float){
                float bbb = ((Float) arg1).floatValue();
                if(aaa > bbb){
                    return true;
                }
                return false;
            }
            if(arg1 instanceof Double){
                double bbb = ((Double) arg1).doubleValue();
                if(aaa > bbb){
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    /**
     * 2018-02-13 1.1.0
     * 对象转Byte数组，必须是实现了java.io.Serializable接口的对象
     */
    public static final byte[] obj2byteArray(final Object obj) {
        byte[] bytes = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try{
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(obj);
            objectOutputStream.flush();
            bytes = byteArrayOutputStream.toByteArray();
        }catch(Throwable throwable){
            LogUtil.error("ObjectUtil_obj2byteArray_1", throwable);
        } finally {
            IOUtil.close(objectOutputStream, byteArrayOutputStream);
        }
        return bytes;
    }

    /**
     * 2018-02-13 1.1.0
     * Byte数组转对象
     */
    public static final Object byteArray2obj(final byte[] bytes) {
        Object obj = null;
        ByteArrayInputStream byteArrayInputStream = null;
        ObjectInputStream objectInputStream = null;
        try{
            byteArrayInputStream = new ByteArrayInputStream(bytes);
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            obj = objectInputStream.readObject();
        }catch(Throwable throwable){
            LogUtil.error("ObjectUtil_byteArray2obj_t1", throwable);
        } finally {
            IOUtil.close(byteArrayInputStream, objectInputStream);
        }
        return obj;
    }

    /**
     * 2019-12-04 3.0.4
     * @param object
     */
    public static final String obj2str(final Object object){
        if(object == null){
            return null;
        }
        String str = object.toString();
        return str;
    }

    /**
     * 2019-12-04 3.0.4
     * @param object
     */
    public static final Integer obj2int(final Object object){
        if(object == null){
            return null;
        }
        String str = object.toString();
        if(StringUtil.lastsWith(str, ".0")){
            str = StringUtil.deleteFirst(str, ".0");
        }
        Integer value = Integer.valueOf(str);
        return value;
    }

    /**
     * 2019-12-04 3.0.4
     * @param object
     */
    public static final Long obj2long(final Object object){
        if(object == null){
            return null;
        }
        String str = object.toString();
        Long value = Long.valueOf(str);
        return value;
    }

    /**
     * 2019-12-04 3.0.4
     * @param str
     */
    public static final Integer str2int(final String str){
        if(str == null){
            return null;
        }
        if("".equals(str)){
            return null;
        }
        Integer value = Integer.valueOf(str);
        return value;
    }

    /**
     * 2019-12-04 3.0.4
     */
    public static final String int2str(final Integer value){
        if(value == null){
            return null;
        }
        String str = value.toString();
        return str;
    }

    /**
     * 2019-12-04 3.0.4
     * @param str
     */
    public static final Long str2long(final String str){
        if(str == null){
            return null;
        }
        if("".equals(str)){
            return null;
        }
        Long value = Long.valueOf(str);
        return value;
    }

    /**
     * 2019-12-04 3.0.4
     */
    public static final String long2str(final Long value){
        if(value == null){
            return null;
        }
        String str = value.toString();
        return str;
    }

    /**
     * 把实体类中的所有声明的字段及值转为Map
     * @param entity 实体对象
     * @return Map<String, Object>
     */
    public static final Map<String, Object> object2map(final Object entity){
        Map<String, Object> fieldMap = new HashMap<>();
        Class<?> cls = entity.getClass();
        for (; cls != Object.class; cls = cls.getSuperclass()){
            Field[] fs = cls.getDeclaredFields();
            for (Field f : fs){
                try {
                    Method m = cls.getDeclaredMethod("get" + StringUtil.toUpperCaseFirst(f.getName()), new Class[] {});
                    Object fieldVal = m.invoke(entity, new Object[] {});
                    fieldMap.put(f.getName(), fieldVal);
                } catch (Throwable throwable){
                    LogUtil.error("ObjectUtil_convertFieldToMap_e1", throwable);
                }
            }
        }
        return fieldMap;
    }

    /**
     * 2018-10-22 1.1.0
     * 删除collection集合中元素为null的对象
     * @param collection
     */
    public static final void removeIfNull(final Collection collection){
        if(collection == null){
            return;
        }
        if(collection.isEmpty()){
            return;
        }
        Iterator iterator = collection.iterator();
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if(object != null){
                continue;
            }
            iterator.remove();
        }
    }

    /**
     * 2018-10-22 1.1.0
     * 删除Map集合中元素为null的对象
     * @param map
     */
    public static final void removeIfNull(final Map map){
        if(map == null){
            return;
        }
        if(map.isEmpty()){
            return;
        }
        Set set = map.keySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            Object key = iterator.next();
            Object value = map.get(key);
            if(value != null){
                continue;
            }
            iterator.remove();
        }
    }

    /**
     * 2019-04-13 2.1.3
     * json数据格式化，条理更清晰，可读性更好
     * @param json
     */
    public static final String jsonFormat(final String json){
        String str = StringUtil.jsonFormat(json);
        return str;
    }

    /**
     * 2019-07-13 2.3.2
     * 2020-08-11 6.4.0
     * @param map
     * @param cls
     */
    public static final Object map2bean(final Map<String, Object> map, final Class<? extends Object> cls){
        if(CollectionUtil.isBlank(map)){
            return null;
        }
        BeanInfo beanInfo = null;
        try{
            beanInfo = Introspector.getBeanInfo(cls);  //获取类属性
            if(beanInfo == null){
                return null;
            }
        }catch(Throwable throwable){
            LogUtil.error("ObjectUtil_map2bean_t1", throwable);
            return null;
        }
        Object obj = ReflectUtil.getInstance(cls);
        if(obj == null){
            return null;
        }
        //给JavaBean对象的属性赋值
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        for(int i = 0; i < propertyDescriptors.length; i++){
            PropertyDescriptor descriptor = propertyDescriptors[i];
            String propertyName = descriptor.getName();
            if(StringUtil.isBlank(propertyName)){
                continue;
            }
            if(!map.containsKey(propertyName)){
                continue;
            }
            Object value = map.get(propertyName);
            if(value == null){
                continue;
            }
            Object[] args = {value};
            String propertyName2 = StringUtil.toUpperCaseFirst(propertyName);
            String propertyName3 = "set" + propertyName2;
            Class parameterType = value.getClass();
            try{
                Method method = cls.getMethod(propertyName3, parameterType);
                method.invoke(obj, args);
            }catch(Throwable throwable){
                LogUtil.error("ObjectUtil_map2bean_t2", throwable);
            }
        }
        return obj;
    }

    /**
     * 2019-07-13 2.3.2
     * @param bean
     */
    public static final Map<String, Object> bean2map(final Object bean) {
        Class<? extends Object> type = bean.getClass();
        Map<String, Object> returnMap = new HashMap<String, Object>();
        BeanInfo beanInfo = null;
        try{
            beanInfo = Introspector.getBeanInfo(type);
            if(beanInfo == null){
                return null;
            }
        }catch(Throwable throwable){
            LogUtil.error("ObjectUtil_bean2map_1", throwable);
            return null;
        }
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        for (int i = 0; i < propertyDescriptors.length; i++) {
            PropertyDescriptor descriptor = propertyDescriptors[i];
            String propertyName = descriptor.getName();
            if (!"class".equals(propertyName)) {
                Method readMethod = descriptor.getReadMethod();
                Object result = null;
                try{
                    Object[] args = new Object[0];
                    result = readMethod.invoke(bean, args);
                    if(result == null){
                        continue;
                    }
                }catch(Throwable throwable){
                    LogUtil.error("ObjectUtil_bean2map_t2", throwable);
                    continue;
                }
                if (result != null) {
                    returnMap.put(propertyName, result);
                }
            }
        }
        return returnMap;
    }

    /**
     * 2019-07-13 2.3.2
     * 是否为数字类型字符串
     * @param str
     */
    public static final boolean isNumber(final String str) {
        boolean flag = StringUtil.isNumber(str);
        return flag;
    }

    /**
     * 2019-07-13 2.3.2
     * 是否为Integer类型字符串
     * @param str
     */
    public static final boolean isInteger(final String str){
        boolean flag = StringUtil.isInteger(str);
        return flag;
    }

    /**
     * 2019-07-13 2.3.2
     * 是否为Long类型字符串
     * @param str
     */
    public static final boolean isLong(final String str) {
        boolean flag = StringUtil.isLong(str);
        return flag;
    }

}
