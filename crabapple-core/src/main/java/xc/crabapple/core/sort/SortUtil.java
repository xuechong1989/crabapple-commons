package xc.crabapple.core.sort;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.MapUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.EncoderUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.reflect.ReflectUtil;
import xc.crabapple.core.regex.RegexUtil;

/**
 * 2018-10-31 1.1.0
 * 2020-03-31 5.0.0 新增中文排序，新增数字型字符串排序
 * 2024-11-10 新增Map类型排序
 * 排序工具类
 * 支持的类型  Integer Long String(ascii, 中文) Map Object Array List
 */
public final class SortUtil {

    public static final Collator COLLATOR_CHINA = Collator.getInstance(Locale.CHINA);

    /**
     * 2020-03-30 5.0.0
     * 自定义字符串排序器
     * 优先级排序： ASCII规则 > 中文
     * @param arg0
     * @param arg1
     * @param digitalize 是否数字型字符串按照数字大小进行排序，是-yes, 否-no
     * @return arg0值小于arg1则返回-1，大于arg1则返回1，相等返回0
     */
    public static final int stringComparator(final String arg0, final String arg1, final String digitalize){
        if(StringUtil.isBlank(arg0)){
            return -1;
        }
        if(StringUtil.isBlank(arg1)){
            return 1;
        }
        if("yes".equals(digitalize)){
            boolean f0 = RegexUtil.isNumber(arg0);  //纯数字
            boolean f1 = RegexUtil.isNumber(arg1);  //纯数字
            if(f0 && f1){
                long a0 = Long.parseLong(arg0);
                long a1 = Long .parseLong(arg1);
                if(a0 < a1){
                    return -1;
                }
                if(a0 == a1){
                    return 0;
                }
                return 1;
            }
        }
        int n0 = arg0.length();
        int n1 = arg1.length();
        //单个字符串循环遍历
        //
        if(n0 > n1){
            for(int i = 0; i < n1; i++){
                String s0 = StringUtil.strAt(arg0, i);
                String s1 = StringUtil.strAt(arg1, i);
                int result = compareToSingle(s0, s1);
                if(result == 0){
                    continue;
                }
                return result;
            }
            return 1;
        }
        //
        if(n0 == n1){
            for(int i = 0; i < n0; i++){
                String s0 = StringUtil.strAt(arg0, i);
                String s1 = StringUtil.strAt(arg1, i);
                int result = compareToSingle(s0, s1);
                if(result == 0){
                    continue;
                }
                return result;
            }
            return 0;
        }
        //
        for(int i = 0; i < n0; i++){
            String s0 = StringUtil.strAt(arg0, i);
            String s1 = StringUtil.strAt(arg1, i);
            int result = compareToSingle(s0, s1);
            if(result == 0){
                continue;
            }
            return result;
        }
        return -1;
    }

    /**
     * 2020-03-31 5.0.0
     * 单个字符的比较
     * @param s0
     * @param s1
     * @return arg0值小于arg1则返回-1，大于arg1则返回1，相等返回0
     */
    public static final int compareToSingle(final String s0, final String s1){
        if(StringUtil.isBlank(s0)){
            if(StringUtil.isBlank(s1)){
                return 0;
            }
            return -1;
        }else if(StringUtil.isBlank(s1)){
            return 1;
        }
        if(s0.equals(s1)){
            return 0;
        }
        //
        //数字排序
        boolean f00 = RegexUtil.isNumber(s0);  //纯数字
        boolean f10 = RegexUtil.isNumber(s1);  //纯数字
        if(f00 && f10){
            long v0 = Long.parseLong(s0);
            long v1 = Long.parseLong(s1);
            if(v0 < v1){
                return -1;
            }else if(v0 == v1){
                return 0;
            }
            return 1;
        }
        //
        //中文排序
        //s0是中文
        boolean f02 = RegexUtil.isChineseOnly(s0);  //纯中文
        boolean f12 = RegexUtil.isChineseOnly(s1);  //纯中文
        if(f02){
            //s0是中文，s1是中文
            if(f12){
                //
                List<String> list = new ArrayList<>();
                list.add(s0);
                list.add(s1);
                Collections.sort(list, new Comparator<String>(){
                    @Override
                    public int compare(String a, String b){
                        int result = SortUtil.COLLATOR_CHINA.compare(s0, s1);
                        return result;
                    }
                });
                String first = list.get(0);
                if(first.equals(s0)){
                    return 1;
                }
                return -1;
            }
            //s0是中文，s1不是中文
            return 1;
        }
        //s0不是中文，s1是中文
        if(f12){
            return -1;
        }
        //
        //ascii处理
        int a0 = EncoderUtil.str2ascii(s0);
        int a1 = EncoderUtil.str2ascii(s1);
        if(a0 < a1){
            return -1;
        }else if(a0 == a1){
            return 0;
        }
        return 1;
    }

    /**
     * 2018-11-01 1.1.0
     * @param integer
     * @param orderType
     */
    public static final Integer sortInteger(final Integer integer, final String orderType){
        if(integer == null){
            return null;
        }
        String str = integer.toString();
        str = sortString(str, orderType);
        Integer localInteger = Integer.valueOf(str);
        return localInteger;
    }

    /**
     * 2018-11-01 1.1.0
     * @param lng
     * @param orderType
     */
    public static final Long sortLong(final Long lng, final String orderType){
        if(lng == null){
            return null;
        }
        String str = lng.toString();
        str = sortString(str, orderType);
        Long localLng = Long.valueOf(str);
        return localLng;
    }

    /**
     * 2020-03-31 5.0.0
     * @param str
     * @param orderType
     */
    public static final String sortString(final String str, final String orderType){
        String result = sortString(str, orderType, "no");
        return result;
    }

    /**
     * 2018-11-01 1.1.0
     * @param str
     * @param orderType
     */
    public static final String sortString(final String str, final String orderType, final String digitalize){
        if(StringUtil.isBlank(str)){
            return str;
        }
        int n = str.length();
        String[] array = new String[n];
        for(int i = 0; i < n; i++){
            char c = str.charAt(i);
            String s = StringUtil.char2str(c);
            array[i] = s;
        }
        sortArrayString(array, orderType, digitalize);
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < n; i++){
            String s = array[i];
            stringBuilder.append(s);
        }
        String result = stringBuilder.toString();
        return result;
    }

    /**
     * 2018-11-01 1.1.0
     * @param array
     * @param orderType
     */
    public static final void sortArrayInteger(final Integer[] array, final String orderType){
        IntegerComparator integerComparator = new IntegerComparator(orderType);
        Arrays.sort(array, integerComparator);
    }

    /**
     * 2018-11-01 1.1.0
     * @param array
     * @param orderType
     */
    public static final void sortArrayLong(final Long[] array, final String orderType){
        LongComparator longComparator = new LongComparator(orderType);
        Arrays.sort(array, longComparator);
    }

    /**
     * 2018-11-01 1.1.0
     * @param array
     * @param orderType
     */
    public static final void sortArrayString(final String[] array, final String orderType, final String digitalize){
        StringComparator stringComparator = new StringComparator(orderType, digitalize);
        Arrays.sort(array, stringComparator);
    }

    /**
     * 2020-04-01 5.0.0
     * 分组排序，分组规则比排序规则优先级更高，即先排序参数groups中指定的，然后接上参数rules规则
     * @param list 数据源，如果list的元素是基础数据类型，则无需指定attribute
     * @param rules 排序规则，第三个参数digitalize可省略，格式如：{"attribute:orderType:digitalize", ""attribute:orderType"", ......}
     *              示例：{"name:desc:yes", "updateTime:asc:no"}
     * @param groups 分组规则，格式如"organ ###### 办公室,人力资源部,审业部"  //即把organ字段的值按这三个部门进行首先分组，先对这三个部门靠前排序
     */
    public static final List sortGroupList(final List list, final String[] rules, final String groups){
        List<Object> resultList = new ArrayList<>();
        if(CollectionUtil.isBlank(list)){
            return resultList;
        }
        if(StringUtil.isBlank(groups)){
            return resultList;
        }
        if(!StringUtil.containOf(groups, " ###### ")){
            return resultList;
        }
        String[] array = StringUtil.split(groups, " ###### ");
        if(array.length <= 1){
            return resultList;
        }
        String localGroups = StringUtil.getPureStr(groups);
        localGroups = StringUtil.trim(localGroups);
        String attribute = array[0];
        String values = array[1];
        String[] groupArray = StringUtil.split(values, ",");
        Map<String, String> groupMap = new HashMap<>();
        for(String v : groupArray){
            groupMap.put(v, v);
        }
        sortList(list, rules);
        //
        Map<String, List<Object>> tmpMap = new HashMap<>();
        for(int i = list.size() - 1; i >= 0; i--){
            Object object = list.get(i);
            String function = StringUtil.attributeWrap(attribute);
            Object value;
            try{
                value = ReflectUtil.methodDynamic(object, function);
            }catch(Throwable throwable){
                LogUtil.error("SortUtil_sortGroupList_t1", throwable, groups);
                continue;
            }
            if(CollectionUtil.isBlank(value)){
                value = "";
            }
            String vvv = value.toString();
            String g = groupMap.get(vvv);
            if(StringUtil.isBlank(g)){
                continue;
            }
            List<Object> groupList = tmpMap.get(vvv);
            if(groupList == null){
                groupList = new ArrayList<>();
                tmpMap.put(vvv, groupList);
            }
            groupList.add(object);
            list.remove(i);
        }
        //
        for(String v : groupArray){
            List<Object> list1 = tmpMap.get(v);
            if(CollectionUtil.isBlank(list1)){
                continue;
            }
            for(int i = list1.size() - 1; i >= 0; i--){
                Object object = list1.get(i);
                resultList.add(object);
            }
        }
        //
        resultList.addAll(list);
        return resultList;
    }

    /**
     * 2020-03-31 5.0.0
     * @param list 数据源，如果list的元素是基础数据类型，则无需指定attribute
     */
    public static final void sortList(final List list){
        //
        sortList(list, null, "asc", "yes");
    }

    /**
     * 2020331 5.0.0
     * 多字段排序，支持多字段排序，ascii排序，中文排序，分组排序，正序反序。
     * 简单易用，但比原生Comparator.comparing效率低30倍左右。
     * 实测效率数据：1000条数据，3个字段正反含中文排序，约耗时300ms。10000条数据，3个字段正反含中文排序，约耗时1000ms。
     * ### 实测案例
     * # 测试程序入口
     * private static void test5(){
     *     List<DtoTest> list = new ArrayList<>();
     *     for(int i = 0; i < 10000; i++){
     *         long id = RandomUtil.getInt(2);
     *         int age = RandomUtil.getInt(2);
     *         String name = RandomUtil.getStr(1);
     *         String nation = ChineseUtil.getRandomCH(5);
     *         String year = RandomUtil.getStr(10);
     *         createDtoTest(list, id, age, name, nation, year);
     *     }
     *     //
     *     long start = System.currentTimeMillis();
     *     String[] rules = {"id:asc", "name:asc", "nation:desc"};
     *     SortUtil.sortList(list, rules);
     *     //Comparator<DtoTest> ca = Comparator.comparing(DtoTest::getId);
     *     //Comparator<DtoTest> cb = ca.thenComparing(DtoTest::getName);
     *     //Collections.sort(list, cb);
     *     //trace(list);
     * }
     * # 构造dto对象
     * private static void createDtoTest(List<DtoTest> list, Long id, Integer age, String name, String nation, String time){
     *     DtoTest dtoTest = new DtoTest();
     *     dtoTest.setId(id);
     *     dtoTest.setAge(age);
     *     dtoTest.setName(name);
     *     dtoTest.setNation(nation);
     *     dtoTest.setTime(time);
     *     list.add(dtoTest);
     * }
     *
     * # 输出打印
     * private static void trace(List<DtoTest> list){
     *     for(int i = 0; i < list.size(); i++){
     *         DtoTest dtoTest = list.get(i);
     *         Long id = dtoTest.getId();
     *         Integer age = dtoTest.getAge();
     *         String name = dtoTest.getName();
     *         String nation = dtoTest.getNation();
     *         String time = dtoTest.getTime();
     *         String aaa = id + ", " + age + ", " + name + ", " + nation + ", " + time;
     *     }
     * }
     * # DtoTest.java
     * private Long id;
     * private Integer age;
     * private String name;
     * private String nation;
     * private String time;
     *
     * @param list 数据源，如果list的元素是基础数据类型，则无需指定attribute
     * @param rules 排序规则，第三个参数digitalize可省略，格式如：{"attribute:orderType:digitalize", ""attribute:orderType"", ......}
     *              示例：{"name:desc:yes", "updateTime:asc:no"}
     */
    public static final void sortList(final List list, final String[] rules){
        if(CollectionUtil.isBlank(list)){
            return;
        }
        if(CollectionUtil.isBlank(rules)){
            return;
        }
        //规则的参数修复
        for(int i = 0; i < rules.length; i++){
            String rule = rules[i];
            if(!StringUtil.containOf(rule, ":")){
                LogUtil.error("SortUtil_sortList_1", "参数错误， rules="+rules);
                return;
            }
            String[] array = StringUtil.split(rule, ":");
            if(array.length == 2){
                rule += ":no";
            }
            rules[i] = rule;
        }
        String rule0 = rules[0];
        String[] array0 = StringUtil.split(rule0, ":");
        String attribute0 = array0[0];
        String orderType0 = array0[1];
        String digitalize0 = array0[2];
        sortList(list, attribute0, orderType0, digitalize0);
        int n = list.size();
        int rulesN = rules.length;
        for(int i = 1; i < rulesN; i++){
            String rule = rules[i - 1];
            String[] array = StringUtil.split(rule, ":");
            String attribute = array[0];
            String orderType = array[1];
            String digitalize = array[2];
            Map<String, List<Object[]>> map = new HashMap<>(n);
            String appendValue = "";
            for(int j = 0; j < n; j++){
                Object object = list.get(j);  //对象
//                String function = StringUtil.attributeWrap(attribute);
//                Object value = ReflectUtil.invokeMethod(object, function, null);  //具体的字段得值
//                if(value == null){
//                    value = "-";
//                }
                for(int k = 0; k < i; k++){
                    String ruleK = rules[k];
                    String[] arrayK = StringUtil.split(ruleK, ":");
                    String attributeK = arrayK[0];
                    String functionK = StringUtil.attributeWrap(attributeK);
                    Object valueK = null;
                    try{
                        valueK = ReflectUtil.methodDynamic(object, functionK);  //具体的字段得值
                    }catch(Throwable throwable){
                        LogUtil.error("SortUtil_sortList_t1", throwable, attributeK);
                        continue;
                    }
                    if(k == 0){
                        appendValue = valueK.toString();
                    }else{
                        appendValue = new StringBuilder(appendValue).append(" ###### ").append(valueK).toString();
                    }
                }
                String akey = appendValue.toString();
                List<Object[]> list1 = map.get(akey);
                if(CollectionUtil.isBlank(list1)){
                    list1 = new ArrayList<>();
                    map.put(akey, list1);
                }
                Object[] objects = {j, object};  //要记录上索引，后续替换时才可以精确定位
                list1.add(objects);
            }
            //本次的
            String rule2 = rules[i];
            String[] array2 = StringUtil.split(rule2, ":");
            String attribute2 = array2[0];
            String orderType2 = array2[1];
            String digitalize2 = array2[2];
            Set<String> set = map.keySet();
            Iterator<String> iterator = set.iterator();
            while(iterator.hasNext()){
                String key = iterator.next();
                List<Object[]> list1 = map.get(key);
                int n1 = list1.size();
                if(n1 <= 1){
                    continue;
                }
                int startIndex = 0;
                int endIndex = 0;
                List<Object> list2 = new ArrayList<>();
                for(int j = 0; j < n1; j++){
                    Object[] objects = list1.get(j);
                    Object v0 = objects[0];
                    int index = Integer.parseInt(v0.toString());  //目前没用到
                    if(j == 0){
                        startIndex = index;
                    }else if(j == n1 - 1){
                        endIndex = index;
                    }
                    Object v1 = objects[1];
                    list2.add(v1);
                }
                sortList(list2, attribute2, orderType2, digitalize2);
                int index = 0;
                for(int j = startIndex; j <= endIndex; j++){
                    list.set(j, list2.get(index));
                    index++;
                }
            }
        }
    }

    /**
     * 2018-10-31 1.1.0
     * @param list 数据源，如果list的元素是基础数据类型，则无需指定attribute
     * @param orderType asc,desc
     */
    public static final void sortList(final List list, final String orderType){
        //
        sortList(list, null, orderType, "yes");
    }

    /**
     * 2020-03-31 5.0.0
     * @param list 数据源，如果list的元素是基础数据类型，则无需指定attribute
     * @param attribute
     * @param orderType asc,desc
     */
    public static final void sortList(final List list, final String attribute, final String orderType){
        //
        sortList(list, attribute, orderType,"yes");
    }

    /**
     * 2018-10-31 1.1.0
     * 2020-03-31 5.0.0 添加参数digitalize
     * @param list 数据源
     * @param attribute list中某对象下的属性名称（如果list中的元素为基础数据类型则忽略此参数）
     * @param orderType asc-正序，desc-倒序
     * @param digitalize 是否进行字符串数字化排序，是-yes，否-no
     */
    public static final void sortList(final List list, final String attribute, final String orderType, final String digitalize){
        //参数校验
        if(CollectionUtil.isBlank(list)){
            return;
        }
        String localOrderType = orderType;
        if(StringUtil.isBlank(localOrderType)){
            localOrderType = "asc";
        }
        localOrderType = StringUtil.toLowerCase(localOrderType);
        if(!StringUtil.isBlank(localOrderType) && !"asc".equals(localOrderType) && !"desc".equals(localOrderType)){
            LogUtil.error("SortUtil_sortList_12", "orderType值错误", localOrderType);
            return;
        }
        //基础数据类型
        Object object = list.get(0);
        if(object == null){
            return;
        }
        boolean flag = object instanceof Integer;
        if(flag){
            IntegerComparator integerComparator = new IntegerComparator(localOrderType);
            Collections.sort(list, integerComparator);
            return;
        }
        //
        flag = object instanceof Long;
        if(flag){
            LongComparator longComparator = new LongComparator(localOrderType);
            Collections.sort(list, longComparator);
            return;
        }
        //
        flag = object instanceof String;
        if(flag){
            StringComparator stringComparator = new StringComparator(localOrderType, digitalize);
            Collections.sort(list, stringComparator);
            return;
        }
        // 2024-11-10
        flag = object instanceof Map;
        if(flag){
            MapComparator mapComparator = new MapComparator(attribute, localOrderType, digitalize);
            Collections.sort(list, mapComparator);
            return;
        }
        //Bean对象类型
        BeanComparator beanComparator = new BeanComparator(attribute, localOrderType, digitalize);
        try{
            Collections.sort(list, beanComparator);
        }catch(Throwable throwable){
            String msg = throwable.toString();
            if(StringUtil.containOf(msg, "Comparison method violates its general contract")){
                return;
            }
            LogUtil.error("SortUtil_sortList_t1", throwable, beanComparator, msg);
        }
    }

    /**
     * 2018-10-31 1.1.0
     * Integer比较器
     */
    static class IntegerComparator implements Comparator<Integer> {
        private String orderType;  //asc,desc
        public IntegerComparator(final String orderType){
            //
            this.orderType = orderType;
        }
        @Override
        public int compare(final Integer arg0, final Integer arg1){
            int v0 = Integer.parseInt(arg0.toString());
            int v1 = Integer.parseInt(arg1.toString());
            int flag = 0;
            if(v0 < v1){
                flag = -1;
            }else if(v0 == v1){
                flag = 0;
            }else{
                flag = 1;
            }
            if("desc".equals(orderType)){
                flag = 0 - flag;
            }
            return flag;
        }
    }

    /**
     * 2018-10-31 1.1.0
     * Long比较器
     */
    static class LongComparator implements Comparator<Long>{
        private String orderType;  //asc,desc
        public LongComparator(final String orderType){
            this.orderType = orderType;
        }
        @Override
        public int compare(final Long arg0, final Long arg1){
            long v0 = Long.parseLong(arg0.toString());
            long v1 = Long.parseLong(arg1.toString());
            int flag = 0;
            if(v0 < v1){
                flag = -1;
            }else if(v0 == v1){
                flag = 0;
            }else{
                flag = 1;
            }
            if("desc".equals(orderType)){
                flag = 0 - flag;
            }
            return flag;
        }
    }

    /**
     * 2018-10-31 1.1.0
     * 2020-03-31 5.0.0 自定义字符串比较器
     * String比较器
     */
    static class StringComparator implements Comparator<String>{
        private String orderType;  //asc,desc
        private String digitalize;  //
        public StringComparator(final String orderType, final String digitalize){
            //
            this.orderType = orderType;
            this.digitalize = digitalize;
        }
        @Override
        public int compare(final String arg0, final String arg1){
            int flag = 0;
            if("yes".equals(digitalize)){
                flag = stringComparator(arg0, arg1, "yes");
            }else{
                flag = stringComparator(arg0, arg1, "no");
            }
            if("desc".equals(orderType)){
                flag = 0 - flag;
            }
            return flag;
        }
    }

    /**
     * 2024-11-10
     * Map比较器
     */
    static class MapComparator implements Comparator<Map<String, Object>>{
        private String attribute;  //属性
        private String orderType;  //asc,desc
        private String digitalize;  //
        public MapComparator(final String attribute, final String orderType, final String digitalize){
            this.attribute = attribute;
            this.orderType = orderType;
            this.digitalize = digitalize;
        }
        @Override
        public int compare(final Map<String, Object> arg0Map, final Map<String, Object> arg1Map){
            if(CollectionUtil.isBlank(arg0Map)){
                return 1;
            }
            if(CollectionUtil.isBlank(arg1Map)){
                return -1;
            }
            int flag = 0;
            if(!arg0Map.containsKey(attribute)){
                LogUtil.error("SortUtil_MapComparator_compare_1", "arg0Map中没有该key", attribute);
                return 1;
            }
            if(!arg1Map.containsKey(attribute)){
                LogUtil.error("SortUtil_MapComparator_compare_2", "arg1Map中没有该key", attribute);
                return 1;
            }
            Long crabappleIndex0 = MapUtil.get(arg0Map, attribute);
            Long crabappleIndex1 = MapUtil.get(arg1Map, attribute);
            if (crabappleIndex0 <= crabappleIndex1){
                flag = -1;
            }else{
                flag = 1;
            }
            if("desc".equals(orderType)){
                flag = 0 - flag;
            }
            return flag;
        }
    }

    /**
     * 2018-10-31 1.1.0
     * Bean比较器
     */
    static class BeanComparator implements Comparator<Object>{
        private String attribute;  //属性
        private String orderType;  //asc,desc
        private String function;  //方法
        private String digitalize;  //
        public BeanComparator(final String attribute, final String orderType, final String digitalize){
            this.attribute = attribute;
            this.orderType = orderType;
            this.function = StringUtil.attributeWrap(attribute);
            this.digitalize = digitalize;
        }
        @Override
        public int compare(final Object arg0, final Object arg1){
            Object value0 = null;
            try{
                value0 = ReflectUtil.methodDynamic(arg0, function);
            }catch(Throwable throwable){
                LogUtil.error("SortUtil_compare_t1", throwable);
            }
            Object value1 = null;
            try{
                value1 = ReflectUtil.methodDynamic(arg1, function);
            }catch(Throwable throwable){
                LogUtil.error("SortUtil_compare_t2", throwable);
            }
            if(value0 instanceof Integer){
                int v0 = Integer.parseInt(value0.toString());
                int v1 = Integer.parseInt(value1.toString());
                int flag = 0;
                if(v0 < v1){
                    flag = -1;
                }else if(v0 == v1){
                    flag = 0;
                }else{
                    flag = 1;
                }
                if("desc".equals(orderType)){
                    flag = 0 - flag;
                }
                return flag;
            }
            if(value0 instanceof Long){
                long v0 = Long.parseLong(value0.toString());
                long v1 = Long.parseLong(value1.toString());
                int flag = 0;
                if(v0 < v1){
                    flag = -1;
                }else if(v0 == v1){
                    flag = 0;
                }else{
                    flag = 1;
                }
                if("desc".equals(orderType)){
                    flag = 0 - flag;
                }
                return flag;
            }
            if(value0 instanceof String){
                String v0 = String.valueOf(value0.toString());
                String v1 = String.valueOf(value1.toString());
                int flag = 0;
                if("yes".equals(digitalize)){
                    flag = stringComparator(v0, v1, "yes");
                }else{
                    flag = stringComparator(v0, v1, "no");
                }
                if("desc".equals(orderType)){
                    flag = 0 - flag;
                }
                return flag;
            }
            return 0;
        }
    }

}
