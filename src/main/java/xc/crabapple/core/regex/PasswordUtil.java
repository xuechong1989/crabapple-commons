package xc.crabapple.core.regex;

import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.ObjectUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.AsciiUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.log.LogUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2020-09-19 6.6.0
 * 密码管理器
 */
public final class PasswordUtil {

    //包含数字
    public static final String S_NUMBER = ".*\\d+.*";
    //包含英文大小写字母
    public static final String S_LOWER = ".*[a-z]+.*";
    public static final String S_UPPER = ".*[A-Z]+.*";
    //包含中文
    public static final String S_CHINESE = "[\\u4e00-\\u9fa5]";
    //不能用作密码的字符有~=;:`"&!|*^#?$<>()/\\
    private static final String SPECIAL = "_.,-+'@%[]{}";  //`-=\\][';/.,~!@#$%^&*()_+|}{\":?><
    //
    private static final String NUMBER = "0123456789";
    //
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";

    /**
     * 2020-09-19 6.6.0
     * 校验指定的密码是否合法
     * 规则1：
     *   不少于8位
     *   含有数字、大小写字母、特殊字符
     *   不出现两个及以上的相同字符
     *   无空格、无中文、无明确要求的特殊字符
     */
    public static final boolean check(final String pwd){
        if(StringUtil.isBlank(pwd)){
            return false;
        }
        if(pwd.length() < 8){
            return false;
        }
        if(StringUtil.containOf(pwd, " ")){
            return false;
        }
        //
        Pattern pattern = Pattern.compile(S_NUMBER);
        Matcher matcher = pattern.matcher(pwd);
        boolean flag = matcher.matches();
        if(!flag){
            return false;
        }
        //
        pattern = Pattern.compile(S_LOWER);
        matcher = pattern.matcher(pwd);
        flag = matcher.matches();
        if(!flag){
            return false;
        }
        //
        pattern = Pattern.compile(S_UPPER);
        matcher = pattern.matcher(pwd);
        flag = matcher.matches();
        if(!flag){
            return false;
        }
        //
        pattern = Pattern.compile(S_CHINESE);
        matcher = pattern.matcher(pwd);
        flag = matcher.find();
        if(flag){
            return false;
        }
        //
        flag = false;
        for(int i = 0; i < pwd.length(); i++){
            char c = pwd.charAt(i);
            String s = String.valueOf(c);
            if(StringUtil.containOf(SPECIAL, s)){
                flag = true;
                break;
            }
        }
        if(!flag){
            return false;
        }
        //
        return true;
    }

    /**
     * 2020-09-19 6.6.0
     * 校验指定的密码是否合法
     * 规则2：
     * 不少于指定位数
     * 必须有大小写英文字母、特殊符号、数字
     * 无空格、无中文
     */
    public static final boolean check2(final String pwd, final int length){
        if(StringUtil.isBlank(pwd)){
            return false;
        }
        if(pwd.length() < length){
            return false;
        }
        // 空格
        if(StringUtil.containOf(pwd, " ")){
            return false;
        }
        // 中文
        Pattern pattern = Pattern.compile(S_CHINESE);
        Matcher matcher = pattern.matcher(pwd);
        boolean flag = matcher.find();
        if(flag){
            return false;
        }
        // 数字
        pattern = Pattern.compile(S_NUMBER);
        matcher = pattern.matcher(pwd);
        flag = matcher.matches();
        if(!flag){
            return false;
        }
        // 小写字母
        pattern = Pattern.compile(S_LOWER);
        matcher = pattern.matcher(pwd);
        flag = matcher.matches();
        if(!flag){
            return false;
        }
        // 大写字母
        pattern = Pattern.compile(S_UPPER);
        matcher = pattern.matcher(pwd);
        flag = matcher.matches();
        if(!flag){
            return false;
        }
        // 特殊字符
        flag = false;
        flag = AsciiUtil.inSpecialStr(pwd);
        if(!flag){
            return false;
        }
        //
        return true;
    }

    /**
     * 2020-09-19 6.6.0
     * 生成指定位数的密码
     * 1 不得包含空格
     * 2 不包含中文
     * 3 必须由数字、大小写字母、特殊字符四种组成
     * @param count
     */
    public static final String get(final int count){
        if(count < 8){
            LogUtil.error("PasswordUtil_get_1", "密码长度至少8位", count);
            return "";
        }
        String lower = RandomUtil.getString(LOWER, 1);
        String upper = RandomUtil.getString(UPPER, 1);
        String number = RandomUtil.getString(NUMBER, 1);
        String special = RandomUtil.getString(SPECIAL, 1);
        int cha = count - 4;
        String other = RandomUtil.getStr(cha);
        String result = lower + upper + number + special + other;
        return result;
    }

    /**
     * 2021-02-25 7.3.0
     * @param count 要生成的字符个数
     * @param exclusions 排除的指定字符
     * 示例： 如若结果是hello123, args为llo1，则最后返回he23
     */
    public static final String get(final int count, final String... exclusions){
        if(CollectionUtil.isBlank(exclusions)){
            String pwd = get(count);
            return pwd;
        }
        while(true){
            boolean flag = true;
            String pwd = get(count);
            for(String exclusion : exclusions){
                if(StringUtil.containOf(pwd, exclusion)){
                    flag = false;
                    break;
                }
            }
            if(flag){
                return pwd;
            }
        }
    }

}
