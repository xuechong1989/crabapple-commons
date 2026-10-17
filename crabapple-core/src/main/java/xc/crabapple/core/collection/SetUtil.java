package xc.crabapple.core.collection;

import java.util.HashSet;
import java.util.Set;

/**
 * 2024-12-04
 * Set集合类
 */
public class SetUtil {

    /**
     * 2024-12-04
     * 创建一个set集合并添加指定元素
     * 不添加空对象元素
     * @param args
     * @return
     */
    public static final Set add(Object... args){
        if(args == null){
            final Set<Object> set = new HashSet<>();
            return set;
        }
        //
        final Set<Object> set = new HashSet<>();
        for(Object arg : args){
            if(arg == null){
                continue;
            }
            set.add(arg);
        }
        return set;
    }

}