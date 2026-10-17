package xc.crabapple.core.collection;

import xc.crabapple.core.encoder.AsciiUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.math.MathUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.sort.SortUtil;
import xc.crabapple.core.url.UrlUtil;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2016-12-06 1.0.0
 */
public final class StringUtil{

    private static final Pattern PATTERN_INTEGER = Pattern.compile("^[-\\+]?[\\d]*$");

    /**
     * 2025-06-09
     * 给一个指定字符串中的指定位置，插入一个指定字符串。
     * 示例：("hello", 1, "AB") ==> hABello
     * @param originalStr
     * @param index
     * @param newStr
     * @return
     */
    public static final String insert(final String originalStr, final int index, final String newStr) {
        if(originalStr == null || "".equals(originalStr)) {
            if(newStr != null) {
                return newStr;
            }
            return originalStr;
        }
        //
        int localIndex = index;
        if(localIndex < 0){
            localIndex = 0;
        }else if(localIndex > originalStr.length()){
            localIndex = originalStr.length();
        }
        //
        final StringBuilder stringBuilder = new StringBuilder(originalStr);
        stringBuilder.insert(localIndex,  newStr);
        final String result = stringBuilder.toString();
        return result;
    }

    /**
     * 2024-09-27
     * 清空指定的stringBuilder对象。
     * @param stringBuilder
     * @return
     */
    public static final StringBuilder clear(final StringBuilder stringBuilder){
        if(stringBuilder == null){
            return null;
        }
        //
        int length = stringBuilder.length();
        stringBuilder.delete(0, length);
        return stringBuilder;
    }

    /**
     * 2023-08-08
     * 字符串转为字符串数组集合
     * @param str
     * @return
     */
    public static final List<String> str2list(final String str){
        if(str == null){
            return new ArrayList<>();
        }
        //
        if(equals(str, "")){
            List<String> list = new ArrayList<>();
            list.add("");
            return list;
        }
        //
        List<String> list = new ArrayList<>();
        for(int i = 0; i < str.length(); i++){
            char chr = str.charAt(i);
            String sss = char2str(chr);
            list.add(sss);
        }
        return list;
    }

    /**
     * 2022-03-25
     * 对传入的字符串（content）进行变量值替换（map） 采用默认的正则表达式：\\{(.*?)\\}
     * @param content 要处理的字符串
     * @param map 替换参数和值的集合
     * @return 替换后的字符串
     */
    public static final String replaceVariableMap(final String content, final Map<String, Object> map){
        String result = replaceVariableMap(content, map, "\\{(.*?)\\}");
        return result;
    }

    /**
     * 2022-03-25
     * @param template 要处理的字符串
     * @param map 替换参数和值的集合
     * @param regular 正则表达式
     * @return 替换后的字符串
     */
    public static final String replaceVariableMap(final String template, final Map<String, Object> map, final String regular){
        String localTemplate = template;
        Pattern regex = Pattern.compile(regular);
        Matcher regexMatcher = regex.matcher(localTemplate);
        while(regexMatcher.find()){
            String key = regexMatcher.group(1);
            String toReplace = regexMatcher.group(0);
            String value = (String) map.get(key);
            if(value != null){
                localTemplate = localTemplate.replace(toReplace, value);
            }else{
                localTemplate = localTemplate.replace(toReplace, "");
            }
        }
        return localTemplate;
    }

    /**
     * 2022-03-25
     * @param collection
     * @param splitChart
     * @return
     */
    public static final String getCollectionString(final Collection<String> collection, final String splitChart){
        if(collection == null){
            return "";
        }
        if(collection.isEmpty()){
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for(String element : collection){
            stringBuilder.append(element).append(splitChart);
        }
        if(stringBuilder.length() > 0){
            int len = stringBuilder.length();
            int index = len - 1;
            stringBuilder.deleteCharAt(index);
        }
        String result = stringBuilder.toString();
        return result;
    }

    /**
     * 2022-03-05
     * 删除末尾的所有换行符\n
     * @param stringBuilder
     */
    public static final String removeEndBreakLine(final StringBuilder stringBuilder){
        String content = stringBuilder.toString();
        String newContent = removeEndBreakLine(content);
        return newContent;
    }

    /**
     * 2022-03-05
     * 删除末尾的所有换行符\n
     * @param content
     */
    public static final String removeEndBreakLine(final String content){
        if(isBlank(content)){
            return content;
        }
        String localContent = content;
        while(true){
            int length = localContent.length();
            String end = localContent.substring(length - 1, length);
            if("\n".equals(end)){
                localContent  = localContent.substring(0, localContent.length() - 1);
                continue;
            }
            break;
        }
        return localContent;
    }

    /**
     * 2017-06-30 1.0.0
     * 获取字符长度，一个汉字作为 1 个字符, 一个英文字母作为 0.5 个字符
     * @param text
     * @return 字符长度，如：text="中国",返回 2；text="test",返回 2；text="中国ABC",返回 4.
     * https://www.cnblogs.com/qlqwjy/p/7530877.html
     */
    public static final Integer getLength(final String text){
        if(text == null || "".equals(text)){
            return 0;
        }
        int textLength = text.length();
        int length = textLength;
        try{
            for(int i = 0; i < textLength; i++){
                String str = String.valueOf(text.charAt(i));
                int n = str.getBytes("UTF-8").length;
                if(n > 1){
                    length++;
                }
            }
            int result = (length % 2 == 0) ? length / 2 : length / 2 + 1;
            return result;
        }catch(Throwable throwable){
            LogUtil.error("StringUtil_getLength_1", throwable, text);
            return -1;
        }
    }


    /**
     * 2020-03-31 5.0.0
     * 字符串转ascii码，之间用英文逗号分隔
     * @param str
     */
    public static final String str2asciis(final String str){
        final String asciis = AsciiUtil.str2asciiCodeStr(str);
        return asciis;
    }

    /**
     * 2020-03-31 5.0.0
     * 单个字符串转ascii码
     * @param str
     */
    public static final int str2ascii(final String str){
        final int asciiCode = AsciiUtil.sss2asciiCode(str);
        return asciiCode;
    }

    /**
     * 2020-03-31 5.0.0
     * 单个ascii码转字符串
     * @param asciiCode
     */
    public static final String ascii2str(final int asciiCode){
        final String str = AsciiUtil.asciiCode2sss(asciiCode);
        return str;
    }

    /**
     * 2020-03-31 5.0.0
     * @param str
     * @param i
     */
    public static final String strAt(final String str, final int i){
        if(str == null || "".equals(str)){
            return null;
        }
        if(i < 0){
            return null;
        }
        if(i >= str.length()){
            return null;
        }
        char chr = str.charAt(i);
        String sss = char2str(chr);
        return sss;
    }

    /**
     * 2020-03-18 5.0.0
     * 大写数字转小写数字，目前只支持个位数和十位数，正数
     * 例如：big2small("二十五") ==> 25
     * @param big
     */
    public static final Integer big2small(final String big){
        Integer result = MathUtil.big2small(big);
        return result;
    }

    /**
     * 2020-03-11 5.0.0
     * 小写数字转大写数字
     * 例如：small2big(25) ==> 二十五
     * @param num
     */
    public static final String small2big(final Integer num){
        String sss = MathUtil.small2big(num);
        return sss;
    }

    /**
     * 2016-12-06 1.0.0
     * 随机生成一个指定个数的字符串，包含数字和小写字母
     */
    public static final String random(final Integer count){
        String value = RandomUtil.getStr(count);
        return value;
    }

    /**
     * 2020-08-12 6.4.0
     * 数字格式化之前面添加0
     * @param n 原数字
     * @param num 期望的返回值的总字符数量
     * @return (String)
     */
    public static final String suplyZero(final Integer n, final Integer num){
        String s = n.toString();
        Long nn = Long.valueOf(s);
        String result = suplyZero(nn, num);
        return result;
    }

    /**
     * 2016-12-06 1.0.0
     * 2020-08-12 6.4.0
     * 数字格式化之前面添加0
     * @param n 原数字
     * @param num 期望的返回值的总字符数量
     * @return (String)
     */
    public static final String suplyZero(final Long n, final Integer num){
        String s = n.toString();
        int sn = s.length();
        if(sn >= num.intValue()){
            String str = n.toString();
            return str;
        }
        int suplyn = num.intValue() - sn;
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < suplyn; i++){
            sb.append("0");
        }
        sb.append(n);
        String result = sb.toString();
        return result;
    }

    /**
     * 2016-12-06 1.0.0
     * 判断一个数组内是否有指定字符串
     */
    public static final boolean isInclude(final String[] args, final String sss){
        if(args == null || args.length == 0){
            return false;
        }
        for(int i = 0; i < args.length; i++){
            String value = args[i];
            boolean flag = ObjectUtil.equals(sss, value);
            if(flag){
                return true;
            }
        }
        return false;
    }

    /**
     * 2016-12-06 1.0.0
     * 判断一个字符串中是否同时含有指定的多个字符串
     */
    public static final boolean isInclude(final String src, final String[] sss){
        if(sss.length <= 0){
            return false;
        }
        for(int i = 0; i < sss.length; i++){
            String s = sss[i];
            if(s == null || "".equals(s)){
                return false;
            }
            if(src.indexOf(s) == -1){
                return false;
            }
        }
        return true;
    }

    /**
     * 2016-12-06 1.0.0
     * @param str
     * @param old
     * @param fresh
     */
    public static final String replaceAll(final String str, final String old, final String fresh){
        if(str == null || "".equals(str)){
            return str;
        }
        if(str.indexOf(old) == -1){
            return str;
        }
        String localStr = str.replace(old, fresh);
        return localStr;
    }

    /**
     * 2017-05-26 1.0.0
     * 2024-04-20 参数改为不定参数
     * 去掉所有的指定字符串
     */
    public static final String deleteAll(final String str, final String... args){
        if(str == null || "".equals(str)){
            return str;
        }
        if(args == null || args.length == 0){
            return str;
        }
        String localStr = str;
        for(String del : args){
            localStr = deleteAll(localStr, del);
        }
        return localStr;
    }

    /**
     * 2016-12-06 1.0.0
     * 2020-03-12 5.0.0
     * 2022-08-14 重写实现，性能极大优化
     * 去掉所有的指定字符串
     */
    public static final String deleteAll(final String str, final String del){
        if(str == null || "".equals(str)){
            return str;
        }
        if(del == null || "".equals(del)){
            return str;
        }
        if(str.indexOf(del) == -1){
            return str;
        }
        //
        String[] array = split(str, del);
        StringBuilder stringBuilder = new StringBuilder();
        for(String sss : array){
            stringBuilder.append(sss);
        }
        String result = stringBuilder.toString();
        return result;
    }

    /**
     * 2018-02-13 1.1.0
     * 2024-04-20 改为不定参数，修复参数重复时返回值不准确的漏洞
     * @param str
     * ("aaabcabde", null, "", "bc", "a") ==> bde
     */
    public static final String deleteFirst(final String str, final String... args){
        if(str == null || "".equals(str)){
            return str;
        }
        if(args == null || args.length == 0){
            return str;
        }
        // 参数过滤
        List<String> argList = new ArrayList<>();
        for(int i = 0; i < args.length; i++){
            String arg = args[i];
            if(arg == null || "".equals(arg)){
                continue;
            }
            if(str.indexOf(arg) == -1){
                continue;
            }
            argList.add(arg);
        }
        //
        String localStr = str;
        while(true){
            boolean flag = true;
            for(int i = 0; i < argList.size(); i++){
                String arg = argList.get(i);
                if(startsWith(localStr, arg)){
                    localStr = localStr.replaceFirst(arg, "");
                    flag = false;
                    break;
                }
            }
            if(flag){
                break;
            }
        }
        //
        return localStr;
    }

    /**
     * 2023-07-05
     * 正序删除指定数量的字符串
     * 示例：("hello", 2) ==> llo
     * @param str
     * @param delCount
     * @return
     */
    public static final String deleteFirst(final String str, final Integer delCount){
        if(str == null){
            return str;
        }
        //
        if(delCount <= 0){
            return str;
        }
        int length = str.length();
        if(length <= delCount){
            return "";
        }
        //
        String result = str.substring(delCount, length);
        return result;
    }

    /**
     * 2021-01-14 7.2.0
     * 2024-04-20 改为不定参数，修复参数重复时返回值不准确的漏洞
     * ("abcdefde", null, "", "f", "de") ==> abc
     * @param str
     * @param args
     * @return
     */
    public static final String deleteLast(final String str, final String... args){
        if(str == null || "".equals(str)){
            return str;
        }
        if(args == null || args.length == 0){
            return str;
        }
        // 参数过滤
        List<String> argList = new ArrayList<>();
        for(int i = 0; i < args.length; i++){
            String arg = args[i];
            if(arg == null || "".equals(arg)){
                continue;
            }
            if(str.indexOf(arg) == -1){
                continue;
            }
            argList.add(arg);
        }
        //
        String localStr = str;
        while(true){
            boolean flag = true;
            for(int i = 0; i < argList.size(); i++){
                String arg = argList.get(i);
                int length = localStr.length();
                int argLength = arg.length();
                if(length < argLength){
                    continue;
                }
                if(length == argLength){
                    if(localStr.equals(arg)){
                        return "";
                    }
                }
                if(lastsWith(localStr, arg)){
                    int endIndex = length - argLength;
                    localStr = substr(localStr, 0, endIndex);
                    flag = false;
                    break;
                }
            }
            if(flag){
                break;
            }
        }
        //
        return localStr;
    }

    /**
     * 2023-07-05
     * 倒序删除指定数量的字符串
     * 示例：("hello", 2) ==> hel
     * @param str
     * @param delCount
     * @return
     */
    public static final String deleteLast(final String str, final Integer delCount){
        if(str == null){
            return str;
        }
        //
        if(delCount <= 0){
            return str;
        }
        int length = str.length();
        if(length <= delCount){
            return "";
        }
        //
        int index = length - delCount;
        String result = str.substring(0, index);
        return result;
    }

    /**
     * 2016-12-06 1.0.0
     * 去除字符串左边空格
     */
    public static final String trimLeft(final String str){
        if(str == null){
            return str;
        }
        if("".equals(str)){
            return str;
        }
        int length = str.length();
        int i = 0;
        for(i = 0; i < length; i++){
            String s = String.valueOf(str.charAt(i));
            if(!" ".equals(s)){
                break;
            }
        }
        String localStr = str.substring(i);
        return localStr;
    }

    /**
     * 2016-12-06 1.0.0
     * 去除字符串右边空格
     */
    public static final String trimRight(final String str){
        if(str == null){
            return str;
        }
        if("".equals(str)){
            return str;
        }
        int length = str.length();
        int i = 0;
        for(i = length - 1; i >= 0; i--){
            String s = String.valueOf(str.charAt(i));
            if(!" ".equals(s)){
                break;
            }
        }
        String localStr = str.substring(0, i + 1);
        return localStr;
    }

    /**
     * 2016-12-06 1.0.0
     * 2025-03-28 去除全角空格"　"，在写程序时可以直接复制该双引号中的内容
     * 去除字符串两边空格
     * 全角空格："　"
     * 半角空格：" "
     */
    public static final String trim(final String str){
        if(str == null){
            return null;
        }
        String localStr = str.replace((char)12288, ' ');
        localStr = localStr.trim();
        //
        localStr = trimRight(localStr);
        localStr = trimLeft(localStr);
        return localStr;
    }

    /**
     * 2025-09-23
     * 去除指定字符串左右的字符串
     * @param str
     * @param args
     * @return
     */
    public static final String trim(final String str, final String... args){
        if(str == null){
            return str;
        }
        if(args == null || args.length == 0){
            return str;
        }
        String localStr = deleteFirst(str, args);
        localStr = deleteLast(localStr, args);
        return localStr;
    }

    /**
     * 2025-02-28
     * 删除指定字符串左右的特殊字符
     * @param str
     * @return
     */
    public static final String trimPure(final String str){
        if(str == null || "".equals(str)){
            return "";
        }
        String localStr = str;
        while(true){
            if(startsWith(localStr, " ", "\n", "\r", "\b", "\f", "\t", "\\u00A0")){
                localStr = deleteFirst(localStr, " ", "\n", "\r", "\b", "\f", "\t", "\\u00A0");
                continue;
            }
            if(lastsWith(localStr, " ", "\n", "\r", "\b", "\f", "\t", "\\u00A0")){
                localStr = deleteLast(localStr, " ", "\n", "\r", "\b", "\f", "\t", "\\u00A0");
                continue;
            }
            return localStr;
        }
    }

    /**
     * 2018-02-11 1.1.0
     * 获取标准字符串，去掉转义字符和左右空格
     * @param str
     */
    public static final String getStandardStr(final String str){
        String localStr = getPureStr(str);
        localStr = trim(localStr);
        return localStr;
    }

    /**
     * 2016-12-06 1.0.0
     * 2021-03-31 7.4.0 新增三种校验
     * 获取指定字符串在一个字符串中出现的次数
     */
    public static final int getCount(final String content, final String keyword){
        if(content == null || "".equals(content)){
            return 0;
        }
        if(keyword == null || "".equals(keyword)){
            return 0;
        }
        if(!containOf(content, keyword)){
            return 0;
        }
        //
        if(keyword.length() == 1){
            int count = 0;
            for(int i = 0; i < content.length(); i++){
                char ccc = content.charAt(i);
                String sss = ccc + "";
                if(sss.equals(keyword)){
                    count++;
                }
            }
            return count;
        }
        //
        String[] array = split(content, keyword);
        int count = array.length - 1;
        return count;
    }

    /**
     * 2016-12-06 1.0.0
     * @param str
     */
    public static final boolean isBlank(final String str){
        if(str == null){
            return true;
        }
        if("".equals(str)){
            return true;
        }
        return false;
    }

    /**
     * 2023-07-29
     * @param args
     * @return
     */
    public static final boolean isBlank(final String... args){
        boolean flag = isBlankOfOne(args);
        return flag;
    }

    /**
     * 2023-05-19
     * 判断指定参数集合中是否含有空对象或空字符串的参数
     * 示例 ("123", "") ==> true
     * @param args
     * @return
     */
    public static final boolean isBlankOfOne(final String... args){
        if(args == null || args.length <= 0){
            return true;
        }
        //
        for(int i = 0; i < args.length; i++){
            String arg = args[i];
            if(arg == null || "".equals(arg)){
                return true;
            }
        }
        //
        return false;
    }

    /**
     * 2023-05-19
     * 判断指定参数集合中是否全部是空对象或空字符串的参数
     * 示例:
     *   ("123", "") ==> false
     *   (null, "") ==> true
     * @param args
     * @return
     */
    public static final boolean isBlankOfAll(final String... args){
        if(args == null || args.length <= 0){
            return true;
        }
        //
        for(int i = 0; i < args.length; i++){
            String arg = args[i];
            if(arg != null && !"".equals(arg)){
                return false;
            }
        }
        //
        return true;
    }

    /**
     * 2016-12-06 1.0.0
     * 2020-03-02 5.0.0
     * 调用示例：
     * substr("abc_token_123", 4, 999) ==> token_123
     * @param str 源字符串
     * @param beginIndex 开始索引，从0开始
     * @param endIndex 结束索引，最大值是源字符串的长度
     */
    public static final String substr(final String str, final Integer beginIndex, final Integer endIndex){
        if(str == null || "".equals(str)){
            return str;
        }
        int length = str.length();
        int localBeginIndex = beginIndex;
        int localEndIndex = endIndex;
        if(localBeginIndex < 0){
            localBeginIndex = 0;
        }
        if(localBeginIndex > length){
            localBeginIndex = length;
        }
        if(localEndIndex < 0){
            localEndIndex = 0;
        }
        if(localEndIndex > length){
            localEndIndex = length;
        }
        //
        if(localBeginIndex >= localEndIndex){
            return "";
        }
        String localStr = str.substring(localBeginIndex, localEndIndex);
        return localStr;
    }

    /**
     * 2018-02-13 1.1.0
     * 原字符串的开头部分是否为指定字符串
     */
    private static final Boolean startsWith(final String str, final String prefix){
        if(str == null){
            return false;
        }
        if(prefix == null){
            return false;
        }
        Boolean flag = str.startsWith(prefix);
        return flag;
    }

    /**
     * 2018-02-13 1.1.0
     * 原字符串的开头部分是否为指定字符串
     */
    public static final boolean startsWith(final String str, final String... args){
        if(str == null){
            return false;
        }
        if(args == null || args.length == 0){
            return false;
        }
        for(String arg : args){
            boolean flag = startsWith(str, arg);
            if(flag){
                return true;
            }
        }
        return false;
    }

    /**
     * 2017-01-01 1.0.0
     * 原字符串的结尾部分是否为指定字符串
     */
    private static final boolean lastsWith(final String str, final String suffix){
        if(str == null){
            return false;
        }
        if("".equals(str)){
            return false;
        }
        if(suffix == null){
            return false;
        }
        if("".equals(suffix)){
            return false;
        }
        int n = suffix.length();
        if(str.length() < n){
            return false;
        }
        StringBuilder sb = new StringBuilder();
        for(int i = str.length() - n; i < str.length(); i++){
            String ccc = String.valueOf(str.charAt(i));
            sb.append(ccc);
        }
        String result = sb.toString();
        if(suffix.equals(result)){
            return true;
        }
        return false;
    }

    /**
     * 2018-02-13 1.1.0
     * 原字符串的结尾部分是否为指定字符串
     */
    public static final boolean lastsWith(final String str, final String... args){
        if(str == null || "".equals(str)){
            return false;
        }
        if(args == null || args.length == 0){
            return false;
        }
        for(String arg : args){
            boolean flag = lastsWith(str, arg);
            if(flag){
                return true;
            }
        }
        return false;
    }

    /**
     * 2017-05-25 1.0.0
     * 2018-08-03 1.1.0 修复参数p为多个冲突字符时执行错误的问题
     * 2024-04-23 转义符处理并入该方法内
     * 经过转义字符高强度测试，如`\1234567890\\-=~!@#$%^&*()_+[]{}|;:,.''""/<>?
     * 正则表达式 相关转义符号都需要加\\
     */
    public static final String[] split(final String str, final String tag){
        if(str == null || "".equals(str)){
            String[] array = {};
            return array;
        }
        if(str.indexOf(tag) == -1){
            String[] array = {str};
            return array;
        }
        if(str.equals(tag)){
            String[] array = {};
            return array;
        }
        // 2024-04-23 由原字符串转换为含转义字符的字符串，避开Java有些字符串处理的方法执行错误
        String[] checks = {"{", "}", "[", "]", "(", ")", "\\", "+", "$", ".",  "|", "*", "^", "?"};
        StringBuilder stringBuilder = new StringBuilder();
        int n = tag.length();
        for(int i = 0; i < n; i++){
            char c = tag.charAt(i);
            String s = String.valueOf(c);
            for(int j = 0; j < checks.length; j++){
                String e = checks[j];
                if(e.equals(s)) {
                    s = ("\\") + s;
                    break;
                }
            }
            stringBuilder.append(s);
        }
        String localTag = stringBuilder.toString();
        // 开始正常处理
        String[] array = str.split(localTag);
        if(!str.endsWith(tag)){
            return array;
        }
        //
        int length = array.length;
        String[] arr = new String[length + 1];
        for(int i = 0; i < length; i++){
            arr[i] = array[i];
        }
        arr[length] = "";
        return arr;
    }

    /**
     * 2022-10-23
     * 从指定索引位置的字符作为分隔符对源字符串进行分割
     * 示例：
     *   ("A#he##wo###B###", "##", 0) ==> ["A#he", "wo###B###"]
     *   ("A#he##wo###B###", "##", 1) ==> ["A#he##wo", "#B###"]
     *   ("A#he##wo###B###", "##", 2) ==> ["A#he##wo###B", "#"]
     * @param str
     * @param tag
     * @param index 从0开始
     * @return
     */
    public static final String[] split(final String str, final String tag, int index){
        String[] array = split(str, tag);
        if(array == null || array.length == 0){
            return array;
        }
        if(array.length <= 2){
            return array;
        }
        //
        int length = array.length;
        if(index < 0){
            index = 0;
        }
        if(index > length - 2){
            index = length - 2;
        }
        //
        String left = "";
        String right = "";
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < length; i++){
            String sss = array[i];
            //
            if(i < index){
                if(i == 0){
                    stringBuilder.append(sss);
                }else{
                    stringBuilder.append(tag).append(sss);
                }
                continue;
            }
            if(i == index){
                if(i == 0){
                    left = sss;
                }else{
                    stringBuilder.append(tag).append(sss);
                    left = stringBuilder.toString();
                }
                stringBuilder = new StringBuilder();
                continue;
            }
            if(i > index){
                int n = stringBuilder.length();
                if(n == 0){
                    stringBuilder.append(sss);
                }else{
                    if (!"".equals(sss)) {
                        stringBuilder.append(tag).append(sss);
                    }
                }
                if(i == length - 1){
                    right = stringBuilder.toString();
                }
                continue;
            }
        }
        //
        String[] arr = {left, right};
        //
        return arr;
    }

    /**
     * 2023-11-04
     * 使用指定的多个分隔符对源字符串进行分割
     * 示例：
     *   ("hellophello234p567phehellohe", true, "he", "234", "p")  ==>  [he, llo, p, he, llo, 234, p, 567, p, he, he, llo, he]
     *   ("hellophello234p567phehellohe", true, "he", "234", "p")  ==>  [llo, llo, 567, llo]
     * @param str 源字符串
     * @param includeTag 结果集中是否包含分隔符
     * @param tags 分隔符集合，若某个分隔符不在源字符串中，则忽略该分隔符
     * @return
     */
    public static final List<String> split(final String str, final boolean includeTag, final String... tags){
        List<String> tagList = ListUtil.array2list(tags);
        List<String> resultList = split(str, includeTag, tagList);
        return resultList;
    }

    /**
     * 2023-11-03
     * 使用指定的多个分隔符对源字符串进行分割
     * 示例：
     *   ("hellophello234p567phehellohe", true, "he", "234", "p")  ==>  [he, llo, p, he, llo, 234, p, 567, p, he, he, llo, he]
     *   ("hellophello234p567phehellohe", true, "he", "234", "p")  ==>  [llo, llo, 567, llo]
     * @param str 源字符串
     * @param includeTag 结果集中是否包含分隔符
     * @param tagList 分隔符集合，若某个分隔符不在源字符串中，则忽略该分隔符
     * @return
     */
    public static final List<String> split(final String str, final boolean includeTag, final List<String> tagList){
        if(str == null || "".equals(str)){
            return new ArrayList<>();
        }
        if(tagList == null || tagList.size() == 0){
            return new ArrayList<>();
        }
        // 真正生效的分隔符集合
        final List<String> usingTagList = new ArrayList<>();
        for(String tag : tagList){
            if(tag == null || "".equals(tag)){
                continue;
            }
            if(str.indexOf(tag) < 0){
                continue;
            }
            usingTagList.add(tag);
        }
        final int tagsSize = usingTagList.size();
        if(tagsSize == 0){
            return new ArrayList<>();
        }else if(tagsSize == 1){
            final String tag = usingTagList.get(0);
            if(str.equals(tag)){
                if(includeTag){
                    final List<String> resultList = new ArrayList<>();
                    resultList.add(str);
                    return resultList;
                }else{
                    return new ArrayList<>();
                }
            }
        }
        //
        List<List<Integer>> list = indexesOfes(str, usingTagList);
        List<Integer> seqList = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        for(int i = 0; i < list.size(); i++){
            final String tag = usingTagList.get(i);
            final int tagLenth = tag.length();
            List<Integer> indexList = list.get(i);
            for(int j = 0; j < indexList.size(); j++){
                final int index = indexList.get(j);
                final String seq = index + "#" + tagLenth;
                final String key = index + "";
                map.put(key, seq);
                seqList.add(index);
            }
        }
        //
        final List<String> resultList = new ArrayList<>();
        SortUtil.sortList(seqList);
        final int seqSize = seqList.size();
        for(int i = 0; i < seqSize; i++){
            final Integer index = seqList.get(i);
            // 如果分隔符不在开头，则强制从源字符串的开始进行遍历
            if(i == 0 && index > 0){
                final String section = substr(str, 0, index);
                if(!"".equals(section)){
                    resultList.add(section);
                }
            }
            //
            final String key = index + "";
            final String seq = map.get(key);
            String[] array = split(seq, "#");
            final String startStr = array[0];
            final int start = Integer.parseInt(startStr);
            final String lengths = array[1];
            final int length = Integer.parseInt(lengths);
            final int last = start + length;
            if(includeTag){
                final String section = substr(str, start, last);
                if(!"".equals(section)) {
                    resultList.add(section);
                }
            }
            //
            if(i < seqSize - 1){
                final int nextIndex = seqList.get(i + 1);
                final String nextKey = nextIndex + "";
                final String nextSeq = map.get(nextKey);
                String[] nextArray = split(nextSeq, "#");
                final String nextStartStr = nextArray[0];
                final int nextStart = Integer.parseInt(nextStartStr);
                final String nextSection = substr(str, last, nextStart);
                if(!"".equals(nextSection)){
                    resultList.add(nextSection);
                }
            }else{
                final String nextSection = substr(str, last, str.length());
                if(!"".equals(nextSection)){
                    resultList.add(nextSection);
                }
            }
        }
        return resultList;
    }

    /**
     * 2020-01-14 5.0.0
     * @param str
     * @param tag
     */
    public static final List<String> splitList(final String str, final String tag){
        String[] array = split(str, tag);
        List<String> list = new ArrayList<>();
        for(String s : array){
            list.add(s);
        }
        return list;
    }

    /**
     * 2017-05-26 1.0.0
     * 2024-04-13 去除NBSP字符
     * 去除转义字符
     */
    public static final String getPureStr(final String str){
        if(str == null){
            return null;
        }
        if("".equals(str)){
            return "";
        }
        String localStr = deleteAll(str, "\n", "\r", "\b", "\t");
        // 去除NBSP
        localStr = localStr.replaceAll("\\u00A0", "");
        return localStr;
    }

    /**
     * 2016-12-06 1.0.0
     * 原参数结构：{"type:测试水印", "pathin:d:/tmp/aaa.pdf", "pathout:d:/tmp/aaa2.pdf", "cover:"}
     */
    public static final Map<String, Object> paramHandler(final String[] param){
        if(param == null || param.length <= 0){
            return null;
        }
        Map<String, Object> map = new HashMap<String, Object>();
        for(String str : param){
            if(str == null || "".equals(str)){
                continue;
            }
            str = getPureStr(str);
            str = trim(str);
            int index = str.indexOf(":");
            if(index == -1){
                continue;
            }
            String key = str.substring(0, index);
            key = getPureStr(key);
            key = trim(key);
            if(key == null || "".equals(key)){
                continue;
            }
            String value = str.substring(index + 1, str.length());
            value = getPureStr(value);
            value = trim(value);
            if(value == null || "".equals(value)){
                value = null;
            }
            map.put(key, value);
        }
        return map;
    }

    /**
     * 2016-12-06 1.0.0
     * 字符串反转
     * abcdefg ==> gfedcba
     * @param str
     */
    public static final String reverse(final String str){
        if(str == null || "".equals(str)){
            return str;
        }
        StringBuilder sb = new StringBuilder();
        int n = str.length();
        for(int i = n - 1; i >= 0; i--){
            char c = str.charAt(i);
            sb.append(c);
        }
        String result = sb.toString();
        return result;
    }

    /**
     * 2016-12-06 1.0.0
     * aa,bb,cc,dd ==> dd,cc,bb,aa
     * @param str
     * @param tag
     */
    public static final String reverse(final String str, final String tag){
        if(str == null || "".equals(str)){
            return str;
        }
        if(tag == null || "".equals(tag)){
            return str;
        }
        if(str.indexOf(tag) == -1){
            return str;
        }
        StringBuilder sb = new StringBuilder();
        String[] arr = split(str, tag);
        for(int i = arr.length - 1; i >= 0; i--){
            sb.append(arr[i]).append(tag);
        }
        String result = sb.substring(0, sb.length() - 1);
        return result;
    }

    /**
     * 2016-12-06 1.0.0
     * 字节数组转换为字符串
     */
    public static final String bytes2str(final byte[] bytes){
        try{
            String str = new String(bytes, "UTF-8");
            return str;
        }catch(Throwable throwable){
            LogUtil.error("StringUtil_bytes2str_1", throwable);
            return null;
        }
    }

    /**
     * 2016-12-06 1.0.0
     * 将字节数组转换为String
     * @param bytes byte[]
     * @return String
     */
    public static final String bytes2str2(final byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder("");
        int length = bytes.length;
        for (int i = 0; i < length; i++) {
            char c = (char) (bytes[i]);
            stringBuilder.append(c);
        }
        String result = stringBuilder.toString();
        return result;
    }

    /**
     * 2016-12-06 1.0.0
     * 字符串转换为字节数组
     */
    public static final byte[] str2bytes(final String str){
        if(str == null || "".equals(str)){
            return null;
        }
        try{
            byte[] bytes = str.getBytes("UTF-8");
            return bytes;
        }catch(Throwable throwable){
            LogUtil.error("StringUtil_str2bytes_1", throwable, str);
            return null;
        }
    }

    /**
     * 2016-12-06 1.0.0
     * char类型转字符串
     */
    public static final String char2str(final char chr){
        String sss = String.valueOf(chr);
        try{
            byte[] bytes = sss.getBytes("UTF-8");
            sss = new String(bytes);
            return sss;
        }catch(Throwable throwable){
            LogUtil.error("StringUtil_char2str_1", throwable, chr);
            return null;
        }
    }

    /**
     * 2019-04-15 2.1.4
     * char[]转字符串
     * @param tag
     */
    public static final String chars2str(final char[] tag){
        String str = new String(tag);
        try{
            byte[] bytes = str.getBytes("UTF-8");
            str = new String(bytes, "UTF-8");
            return str;
        }catch(Throwable throwable){
            LogUtil.error("StringUtil_chars2str_1", throwable);
            return null;
        }
    }

    /**
     * 2016-12-06 1.0.0
     * 字符串转char类型
     */
    public static final char str2char(final String sss){
        if(sss == null || "".equals(sss)){
            return ' ';
        }
        char chr = sss.charAt(0);
        return chr;
    }

    /**
     * 2019-04-15 2.1.4
     * 字符串转换为utf-8
     * @param str
     */
    public static final String str2utf8(String str){
        try{
            byte[] bytes = str.getBytes("UTF-8");
            str = new String(bytes, "UTF-8");
            return str;
        }catch(Throwable throwable){
            LogUtil.error("StringUtil_str2utf8_1", throwable, str);
            return null;
        }
    }

    /**
     * 2017-01-01 1.0.0
     * 判断所给路径中toCata是否为tarCata的子孙
     * @param toCata 移动到文件夹Path
     * @param tarCata 目标文件夹Path
     * @return true是, false不是
     */
    public static final boolean checkPath(final String toCata, final String tarCata){
        boolean same = false;
        String []  tarList = split(tarCata, "/");
        String [] toList = split(toCata, "/");
        int tarsize = tarList.length;
        int tosize = toList.length;
        StringBuilder sb = new StringBuilder();
        if(tarsize > tosize){
            return false;
        }
        boolean flag = ObjectUtil.equals(toList[0], "-");
        if(!flag){
            for(int i = 1; i < tarsize; i++){
                sb.append("/").append(toList[i]);
            }
        }
        String target = sb.toString();
        if(tarCata.equals(target)){
            same = true;
        }
        return same;
    }

    /**
     * 2017-05-26 1.0.0
     * 整型数转换为3位逗号分割形式，正负数均支持
     * 1 ==> 1
     * 1234567890123L ==> 1,234,567,890,123
     * -123 ==> -123
     * -12345 ==> -12,345
     */
    public static final String numByEnglish(final Long num){
        String result = MathUtil.numByEnglish(num);
        return result;
    }

    /**
     * 2018-11-23 1.1.0
     * 如getBetweenStr("abc(defg)hijk", "(", ")") ==> defg
     * 注意：只能是从左往右前两个出现的begin和end
     * @param str
     * @param begin
     * @param end
     */
    public static final String getBetweenStr(final String str, final String begin, final String end){
        StringBuilder sb = new StringBuilder();
        boolean running = false;
        for(int j = 0; j < str.length(); j++){
            char c = str.charAt(j);
            String s = String.valueOf(c);
            if(!running){
                if(begin.equals(s)){
                    running = true;
                    continue;
                }
            }
            if(running){
                if(end.equals(s)){
                    running = false;
                    break;
                }
                sb.append(s);
            }
        }
        String title = sb.toString();
        return title;
    }

    /**
     * 2019-02-13 2.0.1
     * 判断参数是否为数值型字符串，如果是返回true，否则返回false
     * @param str
     */
    public static final boolean isNumeric(final String str) {
        if (str == null) {
            return false;
        }
        int n = str.length();
        for (int i = 0; i < n; i++) {
            char c = str.charAt(i);
            boolean b = Character.isDigit(c);
            if (!b) {
                return false;
            }
        }
        return true;
    }

    /**
     * 2024-12-23
     * @param object
     * @return
     */
    public static final String toStr(Object object) {
        if (object == null) {
            return "";
        }
        String str = object.toString();
        return str;
    }

    /**
     * 2024-12-26
     * @param objects
     * @return
     */
    public static final String toStr(Object... objects) {
        final String str = join(objects);
        return str;
    }

    /**
     * 2019-03-18 2.1.1
     * 2024-12-23 重命名
     * 对象转化为字符串
     * @param objects
     */
    public static final String join(Object... objects){
        if(objects == null){
            return "";
        }
        if(objects.length <= 0){
            return "";
        }
        //
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < objects.length; i++){
            Object object = objects[i];
            if(i >= 1){
                stringBuilder.append("\n");
            }
            if(object == null){
                stringBuilder.append("");
            }else{
                String str = object.toString();
                stringBuilder.append(str);
            }
        }
        String str = stringBuilder.toString();
        return str;
    }

    /**
     * 2019-04-13 2.1.3
     * json数据格式化，条理更清晰，可读性更好
     * @param jsonStr
     */
    public static final String jsonFormat(final String jsonStr){
        int level = 0;
        //存放格式化的json字符串
        StringBuilder stringBuilder = new StringBuilder();
        //将字符串中的字符逐个按行输出
        for(int i = 0; i < jsonStr.length(); i++) {
            //获取s中的每个字符
            char c = jsonStr.charAt(i);
            int n = stringBuilder.length();
            char cLast = 0;
            if(n >= 1){
                cLast = stringBuilder.charAt(n - 1);
            }
            //level大于0并且jsonForMatStr中的最后一个字符为\n,jsonForMatStr加入\t
            if (level > 0 && '\n' == cLast) {
                String strLevel = getStrLevel(level);
                stringBuilder.append(strLevel);
            }
            //遇到"{"和"["要增加空格和换行，遇到"}"和"]"要减少空格，以对应，遇到","要换行
            switch (c) {
                case '{':
                case '[':
                    stringBuilder.append(c).append("\n");
                    level++;
                    break;
                case ',':
                    stringBuilder.append(c).append("\n");
                    break;
                case '}':
                case ']':
                    stringBuilder.append("\n");
                    level--;
                    String strLevel = getStrLevel(level);
                    stringBuilder.append(strLevel).append(c);
                    break;
                default:
                    stringBuilder.append(c);
                    break;
            }
        }
        String result = stringBuilder.toString();
        return result;
    }

    /**
     * 2019-04-13 2.1.3
     * @param level
     */
    private static final String getStrLevel(final int level) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < level; i++) {
            stringBuilder.append("\t");
        }
        String str = stringBuilder.toString();
        return str;
    }

    /**
     * 2019-05-06 2.3.0
     * 把指定集合通过指定字符串拼接为一个新的字符串
     * 该方法参照jdk-1.8版本的String.join()方法编写，用以解决JDK版本兼容性问题
     * 使用示例：
     * Set<String> strings = new LinkedHashSet<>();
     * strings.add("Java"); strings.add("is");
     * strings.add("very"); strings.add("cool");
     * String message = join("-", strings);
     * //message returned is: "Java-is-very-cool"
     * @param tag
     * @param elements
     */
    public static final String join(final String tag, final Iterable<? extends CharSequence> elements){
        String localTag = tag;
        if(localTag == null){
            localTag = "";
        }
        if(elements == null){
            return "";
        }
        int i = 0;
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<? extends CharSequence> iterable = elements.iterator();
        while(iterable.hasNext()){
            CharSequence charSequence = iterable.next();
            String str;
            if(charSequence == null){
                str = "null";
            }else{
                str = charSequence.toString();
            }
            if(i == 0){
                stringBuilder.append(str);
            }else{
                stringBuilder.append(localTag).append(str);
            }
            i++;
        }
        String result = stringBuilder.toString();
        return result;
    }

    /**
     * 2019-06-17 2.3.1
     * 生成24位字符串，结构为：13位时间戳 + 3位服务器节点id + 当前线程id + 序列号
     * @param sid 区分单个服务器节点（如每个tomcat），三位数字型字符串，若不足三位则前面补零，如001
     */
    public static final String pkid(final String sid){
        String str = RandomUtil.pkid(sid);
        return str;
    }

    /**
     * 2019-07-13 2.3.2
     * 是否为数字类型字符串
     * @param str
     */
    public static final boolean isNumber(final String str) {
        boolean flag = PATTERN_INTEGER.matcher(str).matches();
        return flag;
    }

    /**
     * 2019-07-13 2.3.2
     * 是否为Integer类型字符串
     * @param str
     */
    public static final boolean isInteger(final String str){
        boolean flag = PATTERN_INTEGER.matcher(str).matches();
        if(flag){
            Long value = Long.valueOf(str);
            if(ObjectUtil.elts(value, Integer.MAX_VALUE)){
                return true;
            }
        }
        return false;
    }

    /**
     * 2019-07-13 2.3.2
     * 是否为Long类型字符串
     * @param str
     */
    public static final boolean isLong(final String str) {
        boolean flag = PATTERN_INTEGER.matcher(str).matches();
        if(flag){
            Long value = Long.valueOf(str);
            if(value > Integer.MAX_VALUE){
                return true;
            }
        }
        return false;
    }

    /**
     * 2019-07-22 2.3.3
     * 调用示例：
     * subSplit("a_b_c_d", "_", 3);  ==> a_b_c
     * @param str
     * @param split
     * @param num 截取个数
     */
    public static final String subSplit(final String str, final String split, final int num){
        String sss = subSplit(str, split, num, true);
        return sss;
    }

    /**
     * 2019-07-22 2.3.3
     * 调用示例：
     * subSplit("a_b_c_d", "_", 3, false);  ==> abc
     * @param str
     * @param split
     * @param num 截取个数
     * @param carry 是否携带分隔符
     */
    public static final String subSplit(final String str, final String split, int num, final boolean carry){
        if(str == null || "".equals(str)){
            return "";
        }
        if(str.indexOf(split) == -1){
            return "";
        }
        String[] array = split(str, split);
        StringBuilder stringBuilder = new StringBuilder();
        num = Math.min(num, array.length);
        if(carry){
            for(int i = 0; i < num; i++){
                String s = array[i];
                if(i == 0){
                    stringBuilder.append(s);
                }else{
                    stringBuilder.append(split).append(s);
                }
            }
            String sss = stringBuilder.toString();
            return sss;
        }
        for(int i = 0; i < num; i++){
            String s = array[i];
            stringBuilder.append(s);
        }
        String sss = stringBuilder.toString();
        return sss;
    }

    /**
     * 2019-12-09 3.0.4
     * 2022-10-13 由返回值boolean更新为返回值索引
     * @param str
     * @param tag
     */
    public static final int indexOf(final String str, final String tag){
        if(str == null){
            return -1;
        }
        if(tag == null){
            return -1;
        }
        if(str.equals(tag)){
            return 0;
        }
        //
        int index = str.indexOf(tag);
        return index;
    }

    /**
     * 2023-11-03
     * 在指定的字符串中查找并返回所有的索引集合
     * @param str
     * @param tags
     * @return
     */
    public static final List<List<Integer>> indexesOfes(final String str, final String... tags){
        final List<String> tagList = ListUtil.array2list(tags);
        final List<List<Integer>> list = indexesOfes(str, tagList);
        return list;
    }

    /**
     * 2023-11-03
     * 在指定的字符串中查找并返回所有的索引集合
     * 示例：("hellophello234p567phehellohe", "he", "234", "p")  ==>  [[0, 6, 19, 21, 26], [11], [5, 14, 18]]
     * @param str
     * @param tagList
     * @return
     */
    public static final List<List<Integer>> indexesOfes(final String str, final List<String> tagList){
        if(str == null || "".equals(str)){
            return new ArrayList<>();
        }
        if(tagList == null || tagList.size() == 0){
            return new ArrayList<>();
        }
        //
        List<List<Integer>> list = new ArrayList<>();
        for(String tag : tagList){
            List<Integer> indexList = indexesOf(str, tag);
            list.add(indexList);
        }
        return list;
    }

    /**
     * 2023-11-03
     * 在指定的字符串中查找并返回所有的索引集合
     * 示例：("hellophello234p567phehellohe", "he")  ==>  [0, 6, 19, 21, 26]
     * @param str
     * @param tag
     * @return
     */
    public static final List<Integer> indexesOf(final String str, final String tag){
        if(str == null || "".equals(str)){
            return new ArrayList<>();
        }
        if(tag == null || "".equals(tag)){
            return new ArrayList<>();
        }
        if(str.indexOf(tag) < 0){
            return new ArrayList<>();
        }
        //
        final List<Integer> indexList = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        String[] array = split(str, tag);
        for(int j = 0; j < array.length - 1; j++){
            String element = array[j];
            stringBuilder.append(element);
            int index = stringBuilder.length();
            indexList.add(index);
            stringBuilder.append(tag);
        }
        return indexList;
    }

    /**
     * 2022-10-13
     * @param str
     * @param tag
     */
    public static final boolean containOf(final String str, final String tag){
        if(str == null){
            return false;
        }
        if(str.equals(tag)){
            return true;
        }
        if(str.indexOf(tag) != -1){
            return true;
        }
        return false;
    }

    /**
     * 2023-08-11
     * 只要有一个包含，就返回true
     * @param str
     * @param list
     * @return
     */
    public static final boolean containOfOne(final String str, final List<String> list){
        String[] array = ListUtil.list2array(list);
        boolean flag = containOfOne(str, array);
        return flag;
    }

    /**
     * 2022-10-13
     * 只要有一个包含，就返回true
     * @param str
     * @param args
     * @return
     */
    public static final boolean containOfOne(final String str, final String... args){
        if(args == null){
            return false;
        }
        if(args.length == 0){
            return false;
        }
        //
        for(String arg : args){
            boolean flag = containOf(str, arg);
            if(flag){
                return true;
            }
        }
        //
        return false;
    }

    /**
     * 2023-03-05
     * 2025-01-27 新增当两个参数都是null时，返回true
     * @param str
     * @param tag
     */
    public static final boolean equals(final String str, final String tag){
        if(str == null && tag == null){
            return true;
        }
        if(str == null){
            return false;
        }
        if(tag == null){
            return false;
        }
        if(str.equals(tag)){
            return true;
        }
        return false;
    }

    /**
     * 2023-03-05
     * 只要有一个相等，就返回true
     * 示例：equalsOfOne("abc", "de", "abc") ==> true
     * @param str
     * @param args
     * @return
     */
    public static final boolean equalsOfOne(final String str, final String... args){
        if(args == null){
            return false;
        }
        if(args.length == 0){
            return false;
        }
        //
        for(String arg : args){
            boolean flag = equals(str, arg);
            if(flag){
                return true;
            }
        }
        //
        return false;
    }

    /**
     * 2024-06-25
     * 只有所有的都相等，才返回true
     * 示例：equalsOfAll("abc", "de", "abc") ==> false
     * @param str
     * @param args
     * @return
     */
    public static final boolean equalsOfAll(final String str, final String... args){
        if(args == null){
            return false;
        }
        if(args.length == 0){
            return false;
        }
        //
        for(String arg : args){
            boolean flag = equals(str, arg);
            if(!flag){
                return false;
            }
        }
        //
        return true;
    }

    /**
     * 2022-10-13
     * 只有包含所有，才返回true
     * @param str
     * @param args
     * @return
     */
    public static final boolean containOfAll(final String str, final String... args){
        if(args == null){
            return false;
        }
        if(args.length == 0){
            return false;
        }
        //
        for(String arg : args){
            boolean flag = containOf(str, arg);
            if(!flag){
                return false;
            }
        }
        //
        return true;
    }

    /**
     * 获得父路径
     * @param fullPath 如 0.1.2.3.
     */
    public static final String getParentPath(final String fullPath) {
        String subString = fullPath.substring(0, fullPath.length() - 1);
        int index = subString.lastIndexOf(".");
        if (index != -1) {
            String newStr = subString.substring(0, index + 1);
            return newStr;
        }
        return subString;
    }

    /**
     * 获得数组字符串中的字符集
     * @param path 如 1.2.3.4.则返回 '1','2','3','4'
     */
    public static final String getArrCharString(final String path) {
        StringBuilder sb = new StringBuilder();
        String[] arr = path.split("[.]");
        for (int i = 0; i < arr.length; i++) {
            if ("0".equals(arr[i]) || "".equals(arr)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append("'").append(arr[i]).append("'");
        }
        return sb.toString();
    }

    /**
     * 把带有分隔,符的String转成Set类型的集合
     * @param includeSplitStr 带有，的字符串，格式如abc,efg
     */
    public static final Set<String> toSet(final String includeSplitStr) {
        return toSet(includeSplitStr, ",");
    }

    /**
     * 把带有指定分隔符的String转成Set类型的集合
     *
     * @param includeSplitStr 带有，的字符串，格式如abc#efg或abc,efg
     * @param splitChars      如,或#
     * @return
     */
    public static final Set<String> toSet(final String includeSplitStr, final String splitChars) {
        Set<String> sets = new HashSet<>();
        String[] tmps = includeSplitStr.split(splitChars);
        for (String tmp : tmps) {
            sets.add(tmp);
        }
        return sets;
    }

    /**
     * 转化为List
     */
    public static final List<String> toList(final String includeSplitStr) {
        return toList(includeSplitStr, ",");
    }

    /**
     * 把带有指定分隔符的String转成Set类型的集合
     * @param includeSplitStr 带有，的字符串，格式如abc#efg或abc,efg
     * @param splitChars      如,或#
     */
    public static final List<String> toList(final String includeSplitStr, final String splitChars) {
        List<String> list = new ArrayList<>();
        if (includeSplitStr == null) {
            return list;
        }
        String[] tmps = includeSplitStr.split(splitChars);
        for (String tmp : tmps) {
            list.add(tmp);
        }
        return list;
    }

    /**
     * 格式化如下字符串 http://www.bac.com?a = ${a}&b = ${b}
     */
    public static final String format(final String message, final Map<String, Object> params) {
        String result = message;
        if (params == null || params.isEmpty()){
            return result;
        }
        Iterator<String> keyIts = params.keySet().iterator();
        while (keyIts.hasNext()) {
            String key = keyIts.next();
            Object value = params.get(key);
            if (value != null) {
                result = result.replace("${" + key + "}", value.toString());
            }
        }
        return result;
    }

    /**
     * 对字符串去掉前面的指定字符
     * @param content 待处理的字符串
     * @param prefix  要去掉前面的指定字符串
     */
    public static final String trimPrefix(final String content, final String prefix) {
        if (prefix == null || "".equals(prefix)){
            return content;
        }
        String resultStr = content;
        while (resultStr.startsWith(prefix)) {
            resultStr = resultStr.substring(prefix.length());
        }
        return resultStr;
    }

    /**
     * 对字符串去掉前面的指定字符
     * @param content 待处理的字符串
     * @param suffix  要去掉后面的指定字符串
     */
    public static final String trimSuffix(final String content, final String suffix) {
        if (suffix == null || "".equals(suffix)){
            return content;
        }
        String resultStr = content;
        while (resultStr.endsWith(suffix)) {
            resultStr = resultStr.substring(0, resultStr.length() - suffix.length());
        }
        return resultStr;
    }

    /**
     * 2018-10-31 1.1.0
     * 通过属性名获取对应包装类型的对应get方法名
     * 如：attributeWrap("age"); => getAge
     * @param attribute
     */
    public static final String attributeWrap(final String attribute){
        String first = attribute.substring(0, 1);
        String suffix = attribute.substring(1, attribute.length());
        first = first.toUpperCase();
        String wrap = "get" + first + suffix;
        return wrap;
    }

    /**
     * 2016-12-06 1.0.0
     * @param str
     */
    public static final String toUpperCase(final String str){
        if(str == null){
            return null;
        }
        String localStr = str.toUpperCase();
        return localStr;
    }

    /**
     * 2016-12-06 1.0.0
     * @param str
     */
    public static final String toLowerCase(final String str){
        if(str == null){
            return "";
        }
        String localStr = str.toLowerCase();
        return localStr;
    }

    /**
     * 2024-08-09
     * 2024-08-10
     * 将字符串首字符大写
     */
    public static final String toUpperCaseFirst(final String str) {
        final String newStr = p_toUpperOrLowerCaseFirst(str, true);
        return newStr;
    }

    /**
     * 2024-08-09
     * 2024-08-10
     * 将字符串首字符小写
     */
    public static final String toLowerCaseFirst(final String str) {
        final String newStr = p_toUpperOrLowerCaseFirst(str, false);
        return newStr;
    }

    /**
     * 2024-08-09
     * 2024-08-10
     * 把字符串的第一个字母转为大写或者小写
     * @param str     字符串
     * @param isUpper 是否大写
     */
    private static final String p_toUpperOrLowerCaseFirst(final String str, final boolean isUpper) {
        if(str == null || "".equals(str)){
            return "";
        }
        final char firstChar = str.charAt(0);
        final boolean letter = Character.isLetter(firstChar);
        // 不是字母的直接返回
        if (!letter){
            return str;
        }
        //
        String firstStr = char2str(firstChar);
        //
        final int size = str.length();
        if(size == 1){
            if (isUpper){
                firstStr = toUpperCase(firstStr);
                return firstStr;
            }
            // 小写
            firstStr = toLowerCase(firstStr);
            return firstStr;
        }
        //
        String rightStr = str.substring(1);
        // 大写
        if (isUpper){
            firstStr = toUpperCase(firstStr);
            final String newStr = firstStr + rightStr;
            return newStr;
        }
        // 小写
        firstStr = toLowerCase(firstStr);
        final String newStr = firstStr + rightStr;
        return newStr;
    }

    /**
     * 数组合并。
     */
    public static final String join(final String[] vals, final String separator) {
        if (vals == null || vals.length == 0){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vals.length; i++) {
            if (i == 0) {
                sb.append(vals[i]);
            } else {
                sb.append(separator).append(vals[i]);
            }
        }
        String val = sb.toString();
        return val;
    }

    public static final String join(final List<String> list, final String joinSperator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i == 0) {
                sb.append(list.get(i));
            } else {
                sb.append(joinSperator).append(list.get(i));
            }
        }
        String rtnString = sb.toString();
        return rtnString;
    }

    /**
     * 2022-07-13
     * 参数map转换为标准参数字符串
     * @param paramMap
     * @return 格式如 a=1&b=2
     */
    public static final String getParamsByMap(final Map<String, Object> paramMap){
        final String params = UrlUtil.getParamsByMap(paramMap);
        return params;
    }

    /**
     * 2022-05-14
     * HTTP请求地址解析器，字符串转换为List集合
     * @params paramsStr格式如 a=1&b=2
     */
    public static final List<List<String>> paramsParserGetList(final String params){
        List<List<String>> paramList = UrlUtil.getListByParams(params);
        return paramList;
    }

    /**
     * 2022-05-14
     * HTTP请求地址解析器，字符串转换为List集合
     * @params paramsStr格式如 a=1&b=2
     */
    public static final Map<String, String[]> getMapArrayByParams(final String params){
        Map<String, String[]> paramListMap = UrlUtil.getMapArrayByParams(params);
        return paramListMap;
    }

    /**
     * 2022-05-14
     * HTTP请求地址解析器，字符串转换为Map集合
     * @params paramsStr格式如 a=1&b=2
     */
    public static final Map<String, String> getMapByParams(final String params){
        Map<String, String> paramMap = UrlUtil.getMapByParams(params);
        return paramMap;
    }

    /**
     * 2024-09-05
     * 查看字符串是否被编码
     * @params paramsStr格式如 a=1&b=2
     */
    public static final boolean isUrlEncoded(final String str) {
        try {
            String decoded = URLDecoder.decode(str, "UTF-8");
            boolean encoded = !str.equals(decoded);
            return encoded;
        } catch (Throwable throwable) {
            LogUtil.error("StringUtil_isUrlEncoded_1", throwable, str);
            return false;
        }
    }
}