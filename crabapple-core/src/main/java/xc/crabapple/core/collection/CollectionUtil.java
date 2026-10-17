package xc.crabapple.core.collection;

import tools.jackson.databind.node.ArrayNode;
import java.util.Collection;
import java.util.Map;

/**
 * 2018-02-13
 */
public final class CollectionUtil {

    /**
     * 2018-02-13 1.1.0
     * 2022-07-15 代码整理
     * 2024-07-18 添加ArrayNode类型判断
     * @param object
     */
    public static final boolean isBlank(final Object object){
        if(object == null){
            return true;
        }
        // 基本数据
        if(object instanceof Integer){
            Integer myint = (Integer) object;
            if(myint == 0){
                return true;
            }
            return false;
        }
        if(object instanceof Long){
            Long mylong = (Long) object;
            if(mylong == 0L){
                return true;
            }
            return false;
        }
        if(object instanceof String){
            if("".equals(object.toString())){
                return true;
            }
            return false;
        }
        // 数组
        if(object instanceof byte[]){
            byte[] bytes = (byte[]) object;
            if(bytes.length <= 0){
                return true;
            }
            return false;
        }
        if(object instanceof String[]){
            String[] array = (String[]) object;
            if(array.length <= 0){
                return true;
            }
            return false;
        }
        if(object instanceof Object[]){
            Object[] array = (Object[]) object;
            if(array.length <= 0){
                return true;
            }
            return false;
        }
        // 集合
        if(object instanceof Map){
            Map map = (Map) object;
            if(map.isEmpty()){
                return true;
            }
            return false;
        }
        if(object instanceof Collection){
            Collection collection = (Collection) object;
            if(collection.isEmpty()){
                return true;
            }
            return false;
        }
        if(object instanceof ArrayNode){
            ArrayNode arrayNode = (ArrayNode) object;
            if(arrayNode.isEmpty()){
                return true;
            }
            return false;
        }
        //
        return false;
    }

}