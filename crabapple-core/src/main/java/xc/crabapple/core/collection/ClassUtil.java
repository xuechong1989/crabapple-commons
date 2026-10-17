package xc.crabapple.core.collection;

/**
 * 2025-01-14
 * Class管理工具类
 */
public class ClassUtil {

    /**
     * 2025-01-14
     * object对象转为类包全路径名称，如：new ClassUtil() ==> xc.crabapple.core.collection.ClassUtil
     * @param object
     * @return
     */
    public static final String getClassname(final Object object){
        if(object == null){
            return "";
        }
        final Class cls = object.getClass();
        final String classname = getClassname(cls);
        return classname;
    }

    /**
     * 2025-01-09
     * class对象转为类包全路径名称，如：ClassUtil.class ==> xc.crabapple.core.collection.ClassUtil
     * @param cls
     * @return
     */
    public static final String getClassname(final Class cls){
        if(cls == null){
            return "";
        }
        String clsStr = cls.toString();
        String[] array = StringUtil.split(clsStr, " ");
        final String classname = array[1];
        return classname;
    }

}