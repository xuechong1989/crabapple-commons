package xc.crabapple.core.collection;

import xc.crabapple.core.random.RandomUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 2018-11-12 1.1.0
 */
public final class ListUtil {

    /**
     * 2025-11-06
     * 指定集合截取
     * List list = ListUtil.getList("aa", "bb", "cc", "dd", "ee");
     * List list2 = ListUtil.subList(list, 0, 5);  ==>  [aa, bb, cc, dd, ee]
     * List list2 = ListUtil.subList(list, 0, 4);  ==>  [aa, bb, cc, dd]
     * List list2 = ListUtil.subList(list, 3, 5);  ==>  [dd, ee]
     * List list2 = ListUtil.subList(list, 3, 3);  ==>  []
     * List list2 = ListUtil.subList(list, 4, 4);  ==>  []
     * List list2 = ListUtil.subList(list, 5, 5);  ==>  []
     * List list2 = ListUtil.subList(list, 1, 10);  ==>  [bb, cc, dd, ee]
     * @param list
     * @param startIndex
     * @param count
     * @return
     */
    public static final List subList(final List list, final int startIndex, final int count) {
        if(CollectionUtil.isBlank(list)){
            return list;
        }
        int effectiveStartIndex = startIndex;
        int effectiveCount = count;
        if(effectiveStartIndex < 0){
            effectiveStartIndex = 0;
        }
        if(effectiveCount < 0){
            effectiveCount = 0;
        }
        if(effectiveStartIndex > effectiveCount){
            effectiveStartIndex = effectiveCount;
        }
        int size = list.size();
        if(effectiveStartIndex >= size){
            return new ArrayList();
        }
        if(effectiveCount > size){
            effectiveCount = size;
        }
        //
        List newList = list.subList(effectiveStartIndex, effectiveCount);
        return newList;
    }

    /**
     * 2023-08-08
     * 判断指定字符串是否为可打印的ascii码对应的字符串
     * @param str
     * @return
     */
    public static final List<String> str2list(final String str){
        final List<String> list = StringUtil.str2list(str);
        return list;
    }

    /**
     * 2024-09-27
     * 分隔指定数组，可设置每组的元素个数
     * @param list 源数据集合
     * @param groupCount 组数
     * 使用示例：([a, b, c, d, e, f, g, h, i, j], 3)  ==>  [[a, b, c, d], [e, f, g], [h, i, j]]
     */
    public static final <T> List<List<T>> splitList(final List<T> list, final int groupCount) {
        if(CollectionUtil.isBlank(list)){
            return new ArrayList<>();
        }
        int effectiveGroupCount = groupCount;
        if(effectiveGroupCount <= 1){
            final List<List<T>> resultList = new ArrayList<>();
            resultList.add(list);
            return resultList;
        }
        //
        final int size = list.size();
        if(size < effectiveGroupCount){
            effectiveGroupCount = size;
        }
        //
        List<List<T>> dividedList = new ArrayList<>();
        int groupSize = size / effectiveGroupCount;
        int remainder = size % effectiveGroupCount;
        int start = 0;
        for (int i = 0; i < effectiveGroupCount; i++) {
            int end = start + groupSize;
            if(i < remainder){
                end++;
            }
            List<T> subList = list.subList(start, end);
            dividedList.add(subList);
            start = end;
        }
        return dividedList;
    }

    /**
     * 2023-06-12
     * 分隔指定数组，可设置每组的元素个数
     * @param list 源数据集合
     * @param countPerGroup 每组的元素个数
     * 使用示例：([a, b, c, d, e, f, g, h, i, j], 3)  ==>  [[a, b, c], [d, e, f], [g, h, i], [j]]
     */
    public static final List<List<String>> split(final List<String> list, final Integer countPerGroup){
        if(CollectionUtil.isBlank(list)){
            return new ArrayList<>();
        }
        if(countPerGroup <= 0){
            return new ArrayList<>();
        }
        //
        if(countPerGroup == 1){
            List<List<String>> resultList = new ArrayList<>();
            List<String> groupList = null;
            for(int i = 0; i < list.size(); i++){
                String object = list.get(i);
                groupList = new ArrayList<>();
                groupList.add(object);
                resultList.add(groupList);
            }
            return resultList;
        }
        //
        final int size = list.size();
        Integer effectiveCountPerGroup = countPerGroup;
        if(effectiveCountPerGroup > size){
            effectiveCountPerGroup = size;
        }
        //
        List<List<String>> resultList = new ArrayList<>();
        List<String> groupList = null;
        for(int i = 0; i < list.size(); i++){
            String object = list.get(i);
            int mo = i % effectiveCountPerGroup;
            if(mo == 0){
                groupList = new ArrayList<>();
                groupList.add(object);
                if(i == size - 1){
                    resultList.add(groupList);
                    return resultList;
                }
                continue;
            }
            //
            if(mo < (effectiveCountPerGroup - 1)){
                groupList.add(object);
                if(i == size - 1){
                    resultList.add(groupList);
                    return resultList;
                }
                continue;
            }
            //
            groupList.add(object);
            resultList.add(groupList);
            if(i == size - 1){
                return resultList;
            }
        }
        //
        return resultList;
    }

    /**
     * 2024-11-27
     * 创建一个线程安全的List对象
     * @return
     * @param <T>
     */
    public static final <T extends List> T getSyncList(){
        List<Object> list = Collections.synchronizedList(new ArrayList<>());
        T t = (T) list;
        return t;
    }

    /**
     * 2021-04-16 7.5.0
     * 2024-11-27 由create重命名为getList
     * 创建一个线程不安全的List对象并直接初始化
     * @param args
     * @param <T>
     */
    public static final <T extends List> T getList(Object... args){
        List<Object> list = new ArrayList<>();
        if(CollectionUtil.isBlank(args)){
            T t = (T) list;
            return t;
        }
        //
        for(Object object : args){
            list.add(object);
        }
        T t = (T) list;
        return t;
    }

    /**
     * 2021-04-07 7.5.0
     * 浅克隆
     * @param list
     * @return
     */
    public static final List copy(List list){
        if(CollectionUtil.isBlank(list)){
            return list;
        }
        List<Object> newList = new ArrayList<>();
        for(Object object : list){
            newList.add(object);
        }
        return newList;
    }

    /**
     * 2024-09-30
     * 从指定list集合中随机取出一个元素
     * @param list
     * @return
     */
    public static final <T> T getRandom(final List list){
        List randomList = p_getRandomListRepeat(list, 1);
        Object object = randomList.get(0);
        T t = (T) object;
        return t;
    }

    /**
     * 2024-09-30
     * @param list
     * @param targetSize
     * @param repeat
     * @return
     */
    public static final List getRandomList(final List list, final int targetSize, final boolean repeat){
        if(repeat){
            final List randomList = p_getRandomListRepeat(list, targetSize);
            return randomList;
        }
        //
        final List randomList = p_getRandomListNoRepeat(list, targetSize);
        return randomList;
    }

    /**
     * 2024-08-27
     * 从指定数组集合中随机的取出指定个数的元素（不可重复）
     * @param list
     * @param targetSize
     * @return
     */
    private static final List p_getRandomListNoRepeat(final List list, final int targetSize){
        if(list == null){
            return null;
        }
        if(CollectionUtil.isBlank(list)){
            return list;
        }
        if(targetSize <= 0){
            return null;
        }
        final int size = list.size();
        int effectiveTargetSize = targetSize;
        if(effectiveTargetSize > size){
            effectiveTargetSize = size;
        }
        //
        List randomList = new ArrayList();
        for(int i = 0; i < size; i++){
            Object object = list.get(i);
            randomList.add(object);
        }
        Collections.shuffle(randomList);
        for(int i = size - 1; i > size - effectiveTargetSize; i--){
            randomList.remove(i);
        }
        return randomList;
    }

    /**
     * 2023-08-08
     * 从指定数组集合中随机的取出指定个数的元素（可重复）
     * @param list
     * @param targetSize
     * @return
     */
    private static final List p_getRandomListRepeat(final List list, final int targetSize){
        if(list == null){
            return null;
        }
        if(list.isEmpty()){
            return list;
        }
        if(targetSize <= 0){
            return null;
        }
        final int size = list.size();
        int effectiveTargetSize = targetSize;
        if(effectiveTargetSize > size){
            effectiveTargetSize = size;
        }
        //
        final List randomList = new ArrayList();
        for(int i = 0; i < effectiveTargetSize; i++){
            int index = RandomUtil.getInt(0, size - 1);
            Object object = list.get(index);
            randomList.add(object);
        }
        return randomList;
    }

    /**
     * 2022-03-05
     * 创建一个指定长度的正整型数组（从索引0开始），各元素随机排序
     */
    public static final List<Integer> getRandomList(final int size){
        if(size <= 0){
            List<Integer> randomList = new ArrayList<>();
            return randomList;
        }
        List<Integer> randomList = getRandomList(0, size);
        return randomList;
    }

    /**
     * 2022-03-05
     * 创建一个指定范围的正整型数组，各元素随机排序
     * @param fromIndex 开始索引，从0开始
     * @param toIndex 结束索引，结果的实际长度size = toIndex - fromIndex
     */
    public static final List<Integer> getRandomList(final int fromIndex, final int toIndex){
        List<Integer> randomList = new ArrayList<>();
        if(toIndex == 0 || fromIndex >= toIndex){
            return randomList;
        }
        //
        for(int i = fromIndex; i < toIndex; i++){
            randomList.add(i);
        }
        //
        randomList = getRandomList(randomList);
        return randomList;
    }

    /**
     * 2020-06-06 6.1.0
     * 2024-08-27 重新实现
     * 把指定list中的元素重新随机排列
     * @param list
     */
    public static final List getRandomList(final List list){
        if(CollectionUtil.isBlank(list)){
            return list;
        }
        Collections.shuffle(list);
        return list;
    }

    /**
     * 2020-03-30 5.0.0
     * List反转
     * @param list
     */
    public static final List reverse(final List list){
        if(CollectionUtil.isBlank(list)){
            return list;
        }
        List newList = new ArrayList<>();
        for(int i = list.size() - 1; i >= 0; i--){
            Object object = list.get(i);
            newList.add(object);
        }
        return newList;
    }

    /**
     * 2020-03-14 5.0.0
     * 数组转List集合
     */
    public static final List<String> array2list(final String[] array){
        if(CollectionUtil.isBlank(array)){
            List<String> list = new ArrayList<>();
            return list;
        }
        List<String> list = new ArrayList<>();
        for(String element : array){
            list.add(element);
        }
        return list;
    }

    /**
     * 2020-03-14 5.0.0
     * 2024-04-24 改为使用toArray方法转换
     * List集合转数组
     */
    public static final String[] list2array(final List<String> list){
        if(CollectionUtil.isBlank(list)){
            String[] array = {};
            return array;
        }
        //
        int size = list.size();
        String[] array = new String[size];
        list.toArray(array);
        return array;
    }

    /**
     * 2020-03-14 5.0.0
     * 数组去重
     */
    public static final List<String> removeRepeat(final List<String> list){
        if(CollectionUtil.isBlank(list)){
            List<String> emptyList = new ArrayList<>();
            return emptyList;
        }
        List<String> results = new ArrayList<>();
        Map<String, Boolean> map = new HashMap<>(list.size());
        for(int i = 0; i < list.size(); i++){
            String element = list.get(i);
            Boolean flag = map.get(element);
            if(flag == null || !flag){
                map.put(element, true);
                results.add(element);
            }
        }
        return results;
    }

    /**
     * 2018-12-15 2.0.1
     * @param list
     */
    public static final String toString(final List<String> list){
        if(CollectionUtil.isBlank(list)){
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        String result = "";
        for(int i = 0; i < list.size(); i++){
            String str = list.get(i);
            if(str == null){
                str = "";
            }
            if(i == 0){
                stringBuilder.append(str);
                continue;
            }
            stringBuilder.append(", ").append(str);
        }
        result = stringBuilder.toString();
        return result;
    }

    /**
     * 2024-09-25 根据集合、字符串修饰符、分隔符生成字符串
     * @param list 元数据 例如 ["a","b"]
     * @param strModifier 字符串修饰符,可以为空 例如：'
     * @param separators 字符串分隔符号,可以为空 例如： ,
     * @return 例如：'a','b'
     */
    public static final String getString(final List<String> list, final String strModifier, final String separators){
        if (CollectionUtil.isBlank(list)){
            return "";
        }
        String effectiveStrModifier = strModifier;
        String effectiveSeparators = separators;
        if (StringUtil.isBlank(effectiveStrModifier)){
            effectiveStrModifier = "";
        }
        if (StringUtil.isBlank(effectiveSeparators)){
            effectiveSeparators = "";
        }
        boolean firstFlag = true;
        StringBuilder strSb = new StringBuilder();
        for (String string : list) {
            if (StringUtil.isBlank(string)) {
                continue;
            }
            if (firstFlag) {
                strSb.append(effectiveStrModifier).append(string).append(effectiveStrModifier);
                firstFlag = false;
                continue;
            }
            strSb.append(effectiveSeparators).append(effectiveStrModifier).append(string).append(effectiveStrModifier);
        }
        String string = strSb.toString();
        return string;
    }

    /**
     * 2018-11-12 1.1.0
     * 通过指定索引，获取该索引前后的指定数据的元素，若为奇数则指定index索引在中间，若为偶数则index索引在偏左侧位置
     * @param list
     * @param index 从0开始
     * @param amount 返回的数组长度
     * @param occupy 是否使用空对象null占位，true占位，false不占位
     */
    public static final List getListFromCenterIndex(final List list, final Integer index, final Integer amount, final Boolean occupy){
        if(list == null){
            return null;
        }
        if(CollectionUtil.isBlank(list)){
            return list;
        }
        Integer effectiveIndex = index;
        if(effectiveIndex < 0){
            effectiveIndex = 0;
        }
        if(effectiveIndex > list.size()){
            //effectiveIndex = list.size();
        }
        if(amount <= 0){
            List listNew = new ArrayList();
            return listNew;
        }
//        if(amount > list.size()){
//            amount = list.size();
//        }
        int leftAmount = (amount - 1) / 2;
        //leftAmount = amount % 2 == 0 ? leftAmount + 1 : leftAmount;
        int leftStart = effectiveIndex - leftAmount;
        if(leftStart < 0){
            leftStart = 0;
        }
        int leftSurply = leftAmount - effectiveIndex;
        leftSurply = leftSurply < 0 ? 0 : leftSurply;
        int rightAmount = 0;
        if(occupy){
            rightAmount = leftAmount;
        }else{
            rightAmount = leftAmount + leftSurply;
        }
        rightAmount = amount % 2 == 0 ? rightAmount + 1 : rightAmount;
        int rightEnd = effectiveIndex + rightAmount + 1;
        if(rightEnd > list.size()){
            rightEnd = list.size();
        }
        int rightSurply = rightAmount - (list.size() - effectiveIndex - 1);
        rightSurply = rightSurply < 0 ? 0 : rightSurply;
        if(!occupy){
            leftStart -= rightSurply;
        }
        List<String> listNew = list.subList(leftStart, rightEnd);  //[0, list.size()]
        if(occupy){
            for(int i = 0; i < leftSurply; i++){
                listNew.add(0, null);
            }
            for(int i = 0; i < rightSurply; i++){
                listNew.add(null);
            }
        }
        return listNew;
    }

}
