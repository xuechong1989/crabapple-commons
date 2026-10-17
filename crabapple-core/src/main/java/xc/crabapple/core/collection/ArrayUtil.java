package xc.crabapple.core.collection;

import java.util.List;

/**
 * 2018-05-10 1.1.0
 * 数组
 */
public final class ArrayUtil {

    /**
     * 2025-09-24
     * 数组转为字符串
     * @param array
     * @return
     */
    public static final String toString(final String[] array) {
        if(array == null || array.length == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < array.length; i++) {
            String str = array[i];
            if(str == null) {
                str = "null";
            }
            if(i == 0){
                stringBuilder.append(str);
                continue;
            }
            stringBuilder.append(",").append(str);
        }
        String result = stringBuilder.toString();
        return result;
    }

    /**
     * 2022-07-21
     * 实现在类似Javascript中的join方法
     */
    public static final String join(final Object[] arr, final String flag){
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0, len = arr.length; i < len; i++){
            Object object = arr[i];
            String str = String.valueOf(object);
            stringBuilder.append(str);
            if(i < len - 1){
                stringBuilder.append(flag);
            }
        }
        String str = stringBuilder.toString();
        return str;
    }

    /**
     * 2020-03-30 5.0.0
     * 数组反转
     */
    public static final Object[] reverse(final Object[] array){
        if(CollectionUtil.isBlank(array)){
            return array;
        }
        Object[] newArray = new Object[array.length];
        for(int i = array.length - 1; i >= 0; i--){
            Object object = array[i];
            newArray[array.length - 1 - i] = object;
        }
        return newArray;
    }

    /**
     * 2018-05-10 1.1.0
     * 合并数组
     * @param args 数组参数集合
     */
    public static final Object[] mergeByteArray(final Object[] ...args) {
        if(args == null){
            return null;
        }
        int n = args.length;
        if(n == 0){
            return null;
        }
        if(n == 1){
            return args[0];
        }
        int totalLength = 0;
        for(int i = 0; i < n; i++){
            Object[] arr = args[i];
            totalLength += arr.length;
        }
        Object[] arrNew = new Object[totalLength];
        int start = 0;
        for(int i = 0; i < n; i++){
            Object[] arr = args[i];
            System.arraycopy(arr, 0, arrNew, start, arr.length);
            start += arr.length;
        }
        return arrNew;
    }

    /**
     * 2020-03-14 5.0.0
     * 数组去重
     */
    public static final String[] removeRepeat(final String[] array){
        if(CollectionUtil.isBlank(array)){
            String[] arr = {};
            return arr;
        }
        List<String> list = array2list(array);
        list = ListUtil.removeRepeat(list);
        String[] effectiveArray = list2array(list);
        return effectiveArray;
    }

    /**
     * 2020-03-14 5.0.0
     * 数组转List集合
     */
    public static final List<String> array2list(final String[] array){
        List<String> list = ListUtil.array2list(array);
        return list;
    }

    /**
     * 2020-03-14 5.0.0
     * 2024-04-24 改为使用toArray方法转换
     * List集合转数组
     */
    public static final String[] list2array(final List<String> list){
        String[] array = ListUtil.list2array(list);
        return array;
    }

}
