package xc.crabapple.core.data;

/**
 * 2024-12-23
 * 数据类型基础工具类
 */
public class DataTypeUtil {

    /**
     * 2024-12-23
     * 指定对象是否为基础数据类型、基础数据类型数组
     * @param object
     * @return
     */
    public static final boolean isBaseType(final Object object){
        if(object == null){
            return true;
        }
        if(object instanceof Boolean || object instanceof Byte || object instanceof Character || object instanceof Number || object instanceof String){
            return true;
        }
        if(object instanceof Boolean[] || object instanceof Byte[] || object instanceof Character[] || object instanceof Number[] || object instanceof String[]){
            return true;
        }
        //
        return false;
    }

    /**
     * 2025-06-27
     * 指定对象是否为基础数据类型
     * @param object
     * @return
     */
    public static final boolean isBaseSingleType(final Object object){
        if(object == null){
            return true;
        }
        if(object instanceof Boolean || object instanceof Byte || object instanceof Character || object instanceof Number || object instanceof String){
            return true;
        }
        //
        return false;
    }

}
