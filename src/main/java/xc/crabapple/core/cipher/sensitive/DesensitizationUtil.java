package xc.crabapple.core.cipher.sensitive;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;

/**
 * 2024-12-27
 * 脱敏处理工具类
 */
public class DesensitizationUtil {

    /**
     * 对手机号进行脱敏处理
     * @param mobile 手机号
     */
    public static final String mobile(final String mobile) {
        if(StringUtil.isBlank(mobile)){
            return mobile;
        }
        if (mobile.length() != 11) {
            return mobile;
        }
        String newMobile = replaceWithCenter(mobile, 4, "*");
        return newMobile;
    }

    /**
     * 对账号进行脱敏处理
     * @param account 账号
     */
    public static final String account(final String account) {
        if (StringUtil.isBlank(account)) {
            return account;
        }
        String newAccount = replaceWithCenter(account, 4, "*");
        return newAccount;
    }

    /**
     * 对邮箱进行脱敏处理
     * @param emailAddr 邮箱
     */
    public static final String email(final String emailAddr) {
        if (StringUtil.isBlank(emailAddr)) {
            return emailAddr;
        }
        if(!StringUtil.containOf(emailAddr, "@")){
            return emailAddr;
        }
        String newEmailAddr = replaceWithCenter(emailAddr, 4, "*");
        return newEmailAddr;
    }

    /**
     * 2024-12-27
     * 给定一个字符串，把中间指定个数字符串替换为指定符号，实现脱敏效果
     * @param plaintext
     * @return
     */
    public static final String replaceWithCenter(final String plaintext){
        final String newStr = replaceWithCenter(plaintext, 4, "*");
        return newStr;
    }

    /**
     * 2024-12-27
     * 给定一个字符串，把中间指定个数字符串替换为指定符号，实现脱敏效果
     * @param plaintext
     * @param maxReplaceCount
     * @return
     */
    public static final String replaceWithCenter(final String plaintext, final int maxReplaceCount){
        final String newStr = replaceWithCenter(plaintext, maxReplaceCount, "*");
        return newStr;
    }

    /**
     * 2024-12-27
     * 给定一个字符串，把中间指定个数字符串替换为指定符号，实现脱敏效果。
     * 示例：
     * ("a", 3, "*") ==> a
     * ("ab", 3, "*") ==> *b
     * ("abc", 3, "*") ==> a*c
     * ("abcd", 3, "*") ==> ***d
     * ("abcde", 3, "*") ==> a***e
     * ("abcdef", 3, "*") ==> a***ef
     * @param plaintext
     * @param maxReplaceCount
     * @param toReplaceStr
     * @return
     */
    public static final String replaceWithCenter(final String plaintext, final int maxReplaceCount, final String toReplaceStr){
        String effectiveToReplaceStr = StringUtil.trim(toReplaceStr);
        if(StringUtil.isBlank(effectiveToReplaceStr)){
            effectiveToReplaceStr = "*";
        }
        if(effectiveToReplaceStr.length() != 1){
            LogUtil.error("DesensitizationUtil_replaceWithCenter_1", "参数toReplaceStr必须只含有一个非空字符", plaintext, maxReplaceCount, effectiveToReplaceStr);
        }
        final int length = plaintext.length();
        if(length == 1){
            return plaintext;
        }
        if(length == 2){
            String right = plaintext.substring(1);
            String newStr = effectiveToReplaceStr + right;
            return newStr;
        }
        if(length == 3){
            String left = plaintext.substring(0, 1);
            String right = plaintext.substring(2);
            String newStr = left + effectiveToReplaceStr + right;
            return newStr;
        }
        int effectiveMaxReplaceCount = Math.min(length - 1, maxReplaceCount);
        // 计算替换开始的位置
        int startReplaceIndex = (length - effectiveMaxReplaceCount) / 2;
        // 如果字符串长度是偶数，startReplaceIndex会偏向左边，因此对于偶数长度的字符串，
        // 中间3个字符中的前几个会在左侧，后几个会在右侧
        // 替换中间3个字符为星号
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(plaintext.substring(0, startReplaceIndex));
        for (int i = 0; i < effectiveMaxReplaceCount; i++) {
            stringBuilder.append(effectiveToReplaceStr);
        }
        int beginIndex = startReplaceIndex + effectiveMaxReplaceCount;
        String str = plaintext.substring(beginIndex);
        stringBuilder.append(str);
        String newStr = stringBuilder.toString();
        return newStr;
    }

}