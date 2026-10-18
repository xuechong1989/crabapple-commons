package xc.crabapple.core.collection;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import xc.crabapple.core.log.LogUtil;

/**
 * 2020-08-04 6.4.0
 */
public final class MapUtil {

    /**
     * 2024-07-11
     * 2024-07-28 返回值更新为泛型
     * 从指定map中获取指定key的对应值
     * @param map
     * @param key
     * @return
     */
    public static final <T> T get(final Map<String, Object> map, final String key){
        if(CollectionUtil.isBlank(map)){
            return null;
        }
        if(StringUtil.isBlank(key)){
            return null;
        }
        //
        if(!map.containsKey(key)){
            return null;
        }
        //
        final Object value = map.get(key);
        if(value == null){
            return null;
        }
        T t = (T) value;
        return t;
    }

    /**
     * 2023-09-07
     * map浅拷贝
     * @param fromMap
     * @return
     */
    public static final Map copy(final Map fromMap){
        if(fromMap == null){
            return null;
        }
        Map toMap = new HashMap();
        toMap = copy(fromMap, toMap);
        return toMap;
    }

    /**
     * 2023-09-07
     * map浅拷贝
     * @param fromMap
     * @param toMap
     * @return
     */
    public static final Map copy(final Map fromMap, final Map toMap){
        if(fromMap == null){
            return null;
        }
        if(toMap == null){
            return null;
        }
        //
        Set keySet = fromMap.keySet();
        Iterator iterator = keySet.iterator();
        while(iterator.hasNext()){
            final Object key = iterator.next();
            final Object value = fromMap.get(key);
            toMap.put(key, value);
        }
        return toMap;
    }

    /**
     * 2022-04-22
     */
    public static final Map merge(final Map... maps){
        if(CollectionUtil.isBlank(maps)){
            Map newMap = new HashMap();
            return newMap;
        }
        //
        Map newMap = new HashMap();
        for(Map map : maps){
            Set set = map.keySet();
            Iterator iterator = set.iterator();
            while(iterator.hasNext()){
                Object key = iterator.next();
                Object value = map.get(key);
                newMap.put(key, value);
            }
        }
        return newMap;
    }

    /**
     * 2022-04-22
     * 不定参数转换为Map集合
     * @param args 数组长度必须为偶数
     */
    public static final Map<String, Object> array2map(final Object... args){
        if(CollectionUtil.isBlank(args)){
            Map<String, Object> params = new HashMap<>();
            return params;
        }
        //
        Map<String, Object> params = new HashMap<>();
        int length = args.length;
        if(length % 2 != 0){
            LogUtil.error("crabapple-core-MapUtil_getParams_1", length, args);
            return params;
        }
        //
        for(int i = 0; i < length; i++){
            if(i % 2 == 1){
                Object object = args[i - 1];
                String key = (String) object;
                Object value = args[i];
                params.put(key, value);
            }
        }
        return params;
    }

    /**
     * 2020-08-04 6.4.0
     * map型字符串转换为map对象
     * @param mapStr 该值必须是直接map.toString()，如{aaa=bbb, aaa2=bbb2}
     */
    public static final Map<String, Object> string2map(final String mapStr){
        String effectiveMapStr = StringUtil.trim(mapStr);
        if(StringUtil.isBlank(effectiveMapStr)){
            return new HashMap<>();
        }
        if(StringUtil.containOf(effectiveMapStr, "{") && StringUtil.containOf(effectiveMapStr, "}")){
            effectiveMapStr = effectiveMapStr.substring(effectiveMapStr.indexOf("{") + 1, effectiveMapStr.lastIndexOf("}"));
            effectiveMapStr = StringUtil.trim(effectiveMapStr);
        }
        if(StringUtil.isBlank(effectiveMapStr)){
            return new HashMap<>();
        }
        Map<String, Object> map = new HashMap<>();
        String[] pairs = StringUtil.split(effectiveMapStr, ",");
        for(int i = 0; i < pairs.length; i++){
            String pair = StringUtil.trim(pairs[i]);
            if(StringUtil.isBlank(pair)){
                continue;
            }
            int eqIndex = pair.indexOf("=");
            if(eqIndex < 0){
                continue;
            }
            String key = StringUtil.trim(pair.substring(0, eqIndex));
            String value = StringUtil.trim(pair.substring(eqIndex + 1));
            map.put(key, value);
        }
        return map;
    }

}