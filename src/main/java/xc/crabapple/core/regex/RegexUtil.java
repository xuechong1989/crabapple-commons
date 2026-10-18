package xc.crabapple.core.regex;

import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2016-12-07 1.0.0
 * 2025-01-21 代码整理
 * 正则表达式
 */
public final class RegexUtil{

    private static final Pattern PATTERN_IN_NUMBER = Pattern.compile(".*\\d+.*");
    private static final Pattern PATTERN_IN_CHINESE = Pattern.compile("[\\u4e00-\\u9fa5]");
    private static final Pattern PATTERN_CHINESE_ONLY = Pattern.compile("^[\\u4e00-\\u9fa5]+$");
    private static final Pattern PATTERN_IN_UPPER = Pattern.compile(".*[A-Z]+.*");
    private static final Pattern PATTERN_IN_LOWER = Pattern.compile(".*[a-z]+.*");
    private static final Pattern PATTERN_IN_UPPER_LOWER = Pattern.compile(".*[a-zA-Z]+.*");

    /**
     * 2023-07-12
     * 是否为标准ip地址
     */
    public static final boolean isIp(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        String regex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])(\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)){3}$";
        if(str.matches(regex)){
            return true;
        }
        return false;
    }

    /**
     * 2018-11-13 1.1.0
     * 标准账号
     */
    public static final boolean isAccount(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        boolean flag = str.matches("[a-zA-Z0-9.@_]+");
        return flag;
    }

    /**
     * 2016-12-07 1.0.0
     * 标准密码
     */
    public static final boolean isPassword(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        boolean flag = matcher(str, "^\\w{6,20}$");
        return flag;
    }

    /**
     * 2016-12-07 1.0.0
     * 标准邮箱
     */
    public static final boolean isEmail(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        boolean flag = matcher(str, "([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)");
        return flag;
    }

    /**
     * 验证邮件是否符合格式。
     */
    public static final boolean vaildEmail(final String str) {
        final String EMAIL_REG_EX = "^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z0-9]{2,6}$";
        boolean flag = Pattern.matches(EMAIL_REG_EX, str);
        return flag;
    }

    /**
     * 2016-12-07 1.0.0
     * 标准手机号
     */
    public static final boolean isMobile(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
//        boolean flag = matcher(str, "^(13[0-9]|15[012356789]|17[678]|18[0-9]|14[57])[0-9]{8}$");
        if (str.length() != 11){
            return false;
        }
        if (!str.startsWith("1")){
            return false;
        }
        boolean flag = isNumber(str);
        return flag;
    }

    /**
     * 2016-12-07 1.0.0
     * 标准目录名
     * 不允许包含以下字符 / \ : * ? " < > |
     */
    public static final boolean isFolderName(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        boolean flag = isFileName(str);
        return flag;
    }

    /**
     * 2016-12-07 1.0.0
     * 标准文件名
     * 不允许包含以下字符 / \ : * ? " < > |
     */
    public static final boolean isFileName(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        if(!StringUtil.isBlank(str)){
            if(str.contains("\\")){
                return false;
            }
        }
        boolean flag = matcher(str, "[^\\\\/:\\*\\?\\\"<>\\|]+");
        return flag;
    }

    /**
     * 2016-12-07 1.1.0
     * 包含数字
     */
    public static final boolean inNumber(final String str) {
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        Matcher matcher = PATTERN_IN_NUMBER.matcher(str);
        boolean flag = matcher.matches();
        return flag;
    }

    /**
     * 2024-01-03
     * 包含数字
     * @param str
     * @return
     */
    public static final boolean inNumber2(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        Map<String, Boolean> map = new HashMap<>(10);
        for(int i = 0; i < 10; i++){
            map.put(i + "", true);
        }
        //
        for(int i = 0; i < str.length(); i++){
            char ccc = str.charAt(i);
            String sss = StringUtil.char2str(ccc);
            if(map.containsKey(sss)){
                return true;
            }
        }
        return false;
    }

    /**
     * 2016-12-07 1.0.0
     * 仅包含数字
     */
    public static final boolean isNumber(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        boolean flag = matcher(str, "^(-?[0-9]\\d*\\.?\\d*)|(-?0\\.\\d*[1-9])|(-?[0])|(-?[0]\\.\\d*)$");
        return flag;
    }

    /**
     * 2024-06-25
     * @param strArray
     * @return
     */
    public static final boolean isNumberOfOne(final String... strArray){
        if (strArray == null || "".equals(strArray)) {
            return false;
        }
        for(int i = 0; i < strArray.length; i++){
            final String str = strArray[i];
            boolean flag = isNumber(str);
            if(flag){
                return true;
            }
        }
        return false;
    }

    /**
     * 2024-06-25
     * @param strArray
     * @return
     */
    public static final boolean isNumberOfAll(final String... strArray){
        if (strArray == null || "".equals(strArray)) {
            return false;
        }
        for(int i = 0; i < strArray.length; i++){
            final String str = strArray[i];
            boolean flag = isNumber(str);
            if(!flag){
                return false;
            }
        }
        return true;
    }

    /**
     * 2024-01-03
     * 仅包含数字
     * @param str
     * @return
     */
    public static final boolean isNumber2(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        Map<String, Boolean> map = new HashMap<>(10);
        for(int i = 0; i < 10; i++){
            map.put(i + "", true);
        }
        //
        for(int i = 0; i < str.length(); i++){
            char ccc = str.charAt(i);
            String sss = StringUtil.char2str(ccc);
            if(!map.containsKey(sss)){
                return false;
            }
        }
        return true;
    }

    /**
     * 2025-10-07
     * 是否只包括26个大小写英文字母
     * @param str
     */
    public static final boolean isEnglish(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        final boolean flag = str.matches("^[a-zA-Z]+$");
        return flag;
    }

    /**
     * 2016-12-07 1.0.0
     * 是否包含中文
     */
    public static final boolean inChinese(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        Matcher matcher = PATTERN_IN_CHINESE.matcher(str);
        boolean flag = matcher.find();
        return flag;
    }

    /**
     * 2024-07-27
     * 是否含有中文
     * @param array
     * @return
     */
    public static final boolean inChineseOfOne(final String... array) {
        if(CollectionUtil.isBlank(array)){
            return false;
        }
        for(int i = 0; i < array.length; i++){
            String str = array[i];
            if(inChinese(str)){
                return true;
            }
        }
        return false;
    }

    /**
     * 2016-12-07 1.0.0
     * 仅包含中文
     */
    public static final boolean isChineseOnly(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        try{
            byte[] bytes = str.getBytes("UTF-8");
            String localStr = new String(bytes);
            Matcher matcher = PATTERN_CHINESE_ONLY.matcher(localStr);
            boolean flag = matcher.find();
            return flag;
        }catch(Throwable throwable){
            LogUtil.error("RegexUtil_isFullCH_1", throwable, str);
            return false;
        }
    }

    /**
     * 2025-01-21
     * 包含英文大写字母
     */
    public static final boolean inUpper(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        Matcher matcher = PATTERN_IN_UPPER.matcher(str);
        boolean flag = matcher.matches();
        return flag;
    }

    /**
     * 2025-01-21
     * 全英文大写字母
     */
    public static final boolean isUpper(final String str) {
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        boolean flag = str.matches("^[A-Z]+$");
        return flag;
    }

    /**
     * 2025-01-21
     * 包含英文小写字母
     */
    public static final boolean inLower(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        Matcher matcher = PATTERN_IN_LOWER.matcher(str);
        boolean flag = matcher.matches();
        return flag;
    }

    /**
     * 2025-01-21
     * 全英文小写字母
     */
    public static final boolean isLower(final String str) {
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        boolean flag = str.matches("^[a-z]+$");
        return flag;
    }

    /**
     * 2016-12-07 1.0.0
     * 包含英文大小写字母
     */
    public static final boolean inUpperLower(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        if(StringUtil.containOf(str, "-")){
            return false;
        }
        Matcher matcher = PATTERN_IN_UPPER_LOWER.matcher(str);
        boolean flag = matcher.matches();
        return flag;
    }

    /**
     * 2016-12-07 1.0.0
     * 全英文大小写字母
     */
    public static final boolean isUpperLower(final String str) {
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        boolean flag = str.matches("^[a-zA-Z]+$");
        return flag;
    }

    /**
     * 2023-06-13
     * 仅包含英文大小写、数字、下划线，且只能英文大小写居首
     */
    public static final boolean isUpperLowerNumUnderline(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        boolean flag = matcher(str, "[A-Za-z]+[A-Za-z0-9\\_]+");
        return flag;
    }

    /**
     * 2016-12-07 1.0.0
     * 正则匹配
     */
    public static final boolean matcher(final String str, final String regex){
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        boolean flag = matcher.matches();
        return flag;
    }

}
