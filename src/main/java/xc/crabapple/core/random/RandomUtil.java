package xc.crabapple.core.random;

import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.ListUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.regex.RegexUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 2017-07-19 1.0.0
 * 随机数工具类
 */
public final class RandomUtil{

    private static final String[] ARRAY = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};

    private static final AtomicLong pki = new AtomicLong(0);


    /**
     * 2020-06-06 6.1.0
     * 把指定list中的元素重新随机排列
     * @param list
     */
    public static final List getRandomList(final List list){
        List resultList = ListUtil.getRandomList(list);
        return resultList;
    }

    /**
     * 2022-03-05
     * 创建一个指定长度的正整型数组（从索引0开始），各元素随机排序
     */
    public static final List<Integer> getRandomList(final int size){
        List<Integer> list = ListUtil.getRandomList(size);
        return list;
    }

    /**
     * 2022-03-05
     * 创建一个指定范围的正整型数组，各元素随机排序
     * @param fromIndex 开始索引，从0开始
     * @param toIndex 结束索引，结果的实际长度size = toIndex - fromIndex
     */
    public static final List<Integer> getRandomList(final int fromIndex, final int toIndex){
        List<Integer> list = ListUtil.getRandomList(fromIndex, toIndex);
        return list;
    }

    /**
     * 2019-08-20 2.3.5
     * 共36位，其中数字与小写字母32个，中划线4个，结构为8-4-4-4-12
     * 示例be591f1c-422e-4d32-a22b-088a494b0a17
     */
    public static final String getUUID(){
        UUID uuid = UUID.randomUUID();
        String s = uuid.toString();
        return s;
    }

    /**
     * 2020-09-19 6.6.0
     * 从指定字符串中随机取出指定个数的字符
     * getString("abcdefg", 3) ==> dcf
     * @param str
     * @param count
     */
    public static final String getString(final String str, final int count){
        if(StringUtil.isBlank(str)){
            return "";
        }
        if(count <= 0){
            return "";
        }
        if(str.length() < count){
            return "";
        }
        //
        int n = str.length();
        List<String> list = new ArrayList<>();
        for(int i = 0; i < n; i++){
            char c = str.charAt(i);
            String s = c + "";
            list.add(s);
        }
        list = RandomUtil.getRandomList(list);
        StringBuilder stringBuilder = new StringBuilder();
        list = list.subList(0, count);
        for(int i = 0; i < count; i++){
            String s = list.get(i);
            stringBuilder.append(s);
        }
        String result = stringBuilder.toString();
        return result;
    }

    /**
     * 2023-09-08
     * 返回含有完整13位毫秒时间戳的指定位数的随机数
     * @param count 指定生成随机数的个数。是总长度，即包含了时间戳13位长度了。该值必须大于等于13
     */
    public static final String getStrWithStamp(final Integer count){
        final String result = getStrWithStamp(count, false);
        return result;
    }

    /**
     * 2023-09-08
     * 返回含有完整13位毫秒时间戳的指定位数的随机数
     * @param count 指定生成随机数的个数。是总长度，即包含了时间戳13位长度了。该值必须大于等于13
     * @param toLowerCase 生成随机数是否全部强转为小写字母
     * @return
     */
    public static final String getStrWithStamp(final Integer count, final boolean toLowerCase){
        if(count < 13){
            LogUtil.error("RandomUtil_getStrWithStamp_1", "传参错误，目标随机数长度必须不小于13", count);
            return "";
        }
        //
        int localCount = count - 13;  //  完整的13位毫秒时间戳长度
        String result = getStr(localCount, toLowerCase);
        result += System.currentTimeMillis();
        return result;
    }

    /**
     * 2023-08-01
     * 随机生成一个指定个数的字符串，仅包含数字和小写字母
     */
    public static final String getStr(final Integer count){
        final String random = getStr(count, true);
        return random;
    }

    /**
     * 2017-07-19 1.0.0
     * 2019-08-20 2.3.5
     * 2023-08-01 配置返回值是否大小写敏感
     * 随机生成一个指定个数的字符串，仅包含：数字+小写字母+大写字母
     * @param count 指定生成随机数的个数
     * @param toLowerCase 生成随机数是否全部强转为小写字母
     */
    public static final String getStr(final Integer count, final boolean toLowerCase){
        if(count <= 0){
            return "";
        }
        //
        if(count == 22){
            String ssssss = RandomStr22.getStr();
            ssssss = StringUtil.replaceAll(ssssss, "-", "A");
            ssssss = StringUtil.replaceAll(ssssss, "_", "a");
            if(toLowerCase){
                ssssss = StringUtil.toLowerCase(ssssss);
            }
            return ssssss;
        }
        // mongodb的主键生成策略
        if(count == 24){
            String ssssss = RandomStr24.getStr();
            if(toLowerCase){
                ssssss = StringUtil.toLowerCase(ssssss);
            }else{
                ssssss = randomSetStrUpperCase(ssssss);
            }
            return ssssss;
        }
        // 2020-06-10 6.1.0 超级随机数，可确保全球唯一
        if(count == 28){
            long id = SnowflakeUtil.getLong();
            String sid = id + "";
            int n = count - sid.length();
            String first = getStr(n);
            String ssssss = first + sid;
            if(toLowerCase){
                ssssss = StringUtil.toLowerCase(ssssss);
            }else{
                ssssss = randomSetStrUpperCase(ssssss);
            }
            return ssssss;
        }
        //
        if(count == 32){
            UUID uuid = UUID.randomUUID();
            String ssssss = uuid.toString();
            ssssss = ssssss.replaceAll("-", "");
            if(toLowerCase){
                ssssss = StringUtil.toLowerCase(ssssss);
            }else{
                ssssss = randomSetStrUpperCase(ssssss);
            }
            return ssssss;
        }
        //2020-02-24 5.0.0
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < count; i++){
            Integer index = getInt(0, ARRAY.length - 1);
            String s = ARRAY[index];
            stringBuilder.append(s);
        }
        String ssssss = stringBuilder.toString();
        if(toLowerCase){
            ssssss = StringUtil.toLowerCase(ssssss);
        }
        return ssssss;
    }

    /**
     * 2023-08-03
     * 随机生成只包含指定数量的只占一位字节的ASCII码对应的字符
     * @param count
     * @return
     */
    public static final String getStrASCII(final Integer count){
        if(count <= 0){
            return "";
        }
        final StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < count; i++){
            final int asciiCode = getInt(33, 126);  // [33,126]表示打印字符ASCII码
            final char chr = (char) asciiCode;
            final String str = StringUtil.char2str(chr);
            stringBuilder.append(str);
        }
        final String randomStr = stringBuilder.toString();
        return randomStr;
    }

    /**
     * 2017-06-13 1.0.0
     * 2019-08-20 2.3.5
     * 2020-04-01 5.0.0 解决s为00时出现死循环的问题
     * 获得指定位数的随机数
     * @param count 要指定的位数，有意义的取值范围是 1 <= count <= 9
     */
    public static final Integer getInt(final Integer count){
        if(count <= 0){
            return -1;
        }
        if(count >= 10){
            return -2;
        }
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder();
        for(int k = 0; k < count; k++){
            int a = random.nextInt(9);
            if(k == 0){
                if(a == 0){
                    a = 1;
                }
            }
            stringBuilder.append(a);
        }
        String s = stringBuilder.toString();
        Integer i = Integer.valueOf(s);
        return i;
    }

    /**
     * 2019-08-20 2.3.5
     * 获得指定位数的随机数
     * @param count 要指定的位数，有意义的取值范围是 1 <= count <= 19
     */
    public static final Long getLong(final Integer count){
        if(count <= 0){
            return -1L;
        }
        if(count >= 20){
            return -2L;
        }
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder();
        for(int k = 0; k < count; k++){
            int a = random.nextInt(9);
            stringBuilder.append(a);
        }
        String s = stringBuilder.toString();
        while(s.startsWith("0")){
            s = s.substring(1, s.length()) + "0";
        }
        Long l = Long.valueOf(s);
        return l;
    }

    /**
     * 2023-10-04
     * 返回指定的两数及其之间的随机整数，[0, Long.MAX_VALUE]，可作为于负载均衡算法（取端口）
     */
    public static final Long getLong(final Long min, final Long max){
        long min2 = min.longValue();
        long max2 = max.longValue();
        double random = Math.random();
        long tmp = (long) (random * (max2 - min2 + 1) + min2);
        return tmp;
    }

    /**
     * 2017-06-23 1.0.0
     * 返回指定的两数及其之间的随机整数，[0, Integer.MAX_VALUE]，可作为于负载均衡算法（取端口）
     */
    public static final Integer getInt(final Integer min, final Integer max){
        double random = Math.random();
        int tmp = (int) (random * (max - min + 1) + min);
        return tmp;
    }

    /**
     * 2018-02-11 1.1.0
     * 从List中随机取出一个元素，可作为于负载均衡算法（取IP）
     * @param list
     */
    public static final Object getElementFromListWithRandom(final List<Object> list){
        if(CollectionUtil.isBlank(list)){
            return null;
        }
        int n = list.size();
        int from = 0;
        int to = n - 1;
        int target = getInt(from, to);
        for(int i = 0; i < n; i++){
            if(target == i){
                Object obj = list.get(i);
                return obj;
            }
        }
        return null;
    }

    /**
     * 2018-02-11 1.1.0
     * 从Map中随机取出一个元素，可作为于负载均衡算法（取IP）
     * 注：map的key必须为：prefix_序列号，且序列号必须为:从0开始,升序,连续的整型数
     * @param map
     */
    public static final Object getElementFromMapWithRandom(final Map<String, Object> map){
        if(CollectionUtil.isBlank(map)){
            return null;
        }
        Set<String> set = map.keySet();
        Iterator<String> iterator = set.iterator();
        String key = iterator.next();
        String[] array = StringUtil.split(key, "_");
        String prefix = array[0];
        int n = map.size();
        int from = 0;
        int to = n - 1;
        int target = getInt(from, to);
        key = prefix + "_" + target;
        Object obj = map.get(key);
        return obj;
    }

    /**
     * 2019-06-17 2.3.1
     * 自定义数据库主键生成器
     * 生成24位字符串，结构为：13位时间戳 + 3位服务器节点id + 当前线程id + 序列号
     * @param sid 区分单个服务器节点（如每个tomcat），三位数字型字符串，若不足三位则前面补零，如001
     */
    public static final String pkid(final String sid){
        long c = System.currentTimeMillis();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(c);
        stringBuilder.append(sid);
        Thread thread = Thread.currentThread();
        long tid = thread.getId();
        String tids = String.valueOf(tid);
        stringBuilder.append(tids);
        int n = 8 - tids.length();  //8 = 24 - 13 - 3
        double a = Math.pow(10, n);
        int aa = (int) a;
        long currentPki = pki.get();
        if(currentPki >= aa){
            pki.set(0);
            currentPki = 0;
        }
        String s = StringUtil.suplyZero(currentPki, n);
        stringBuilder.append(s);
        String str = stringBuilder.toString();
        pki.incrementAndGet();
        return str;
    }

    /**
     * 2023-08-01
     * 对指定字符串随机性的由小写字母设置为大写字母
     * 示例：xeabef123 ==> XeAbEF123
     * @param str
     * @return
     */
    public static final String randomSetStrUpperCase(final String str){
        if(StringUtil.isBlank(str)){
            return str;
        }
        //
        StringBuilder stringBuilder = new StringBuilder();
        Random random = new Random();
        for(int i = 0; i < str.length(); i++){
            final char chr = str.charAt(i);
            String sss = StringUtil.char2str(chr);
            if(!RegexUtil.isUpperLower(sss)){
                stringBuilder.append(sss);
                continue;
            }
            final int tmp = random.nextInt(9);
            if(tmp % 2 == 0){
                sss = sss.toUpperCase();
            }
            stringBuilder.append(sss);
        }
        String result = stringBuilder.toString();
        return result;
    }

}