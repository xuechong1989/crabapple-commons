package xc.crabapple.core.phone;

import xc.crabapple.core.random.RandomUtil;

/**
 * 2025-10-24
 * 中国大陆手机号码生成器，生成标准11位手机号码
 * 支持中国移动、中国联通、中国电信的主流号段
 */
public final class PhoneUtil {

    private final static String CHINA_MOBILE = "中国移动";
    private final static String CHINA_UNICOM = "中国联通";
    private final static String CHINA_TELECOM = "中国电信";

    // 中国大陆主流运营商号段定义 (更新至2024年主流号段) [1,2,4](@ref)
    private static final String[] CHINA_MOBILE_ARRAY = {
            "134", "135", "136", "137", "138", "139", "144", "147", "148",
            "150", "151", "152", "157", "158", "159", "165", "172", "178",
            "182", "183", "184", "187", "188", "195", "197", "198"
    };

    private static final String[] CHINA_UNICOM_ARRAY = {
            "130", "131", "132", "140", "145", "146", "155", "156", "166",
            "167", "171", "175", "176", "185", "186", "196"
    };

    private static final String[] CHINA_TELECOM_ARRAY = {
            "133", "134", "141", "149", "153", "162", "173", "174", "177",
            "180", "181", "189", "190", "191", "193", "199"
    };

    /**
     * 2025-10-24
     * 生成指定运营商的随机手机号码
     * @param type 运营商名称，可选取值： 中国移动 / 中国联通 / 中国电信
     * @return 11位手机号码字符串
     */
    public static final String generate(final String type) {
        String prefix3 = p_getPrefix(type);
        String result = p_generate(prefix3);
        return result;
    }

    /**
     * 2025-10-24
     * 随机生成任意运营商的手机号码
     * @return 包含运营商和号码的数组，索引0为运营商名称，索引1为手机号码
     */
    public static final String generate() {
        String prefix3 = p_getPrefix();
        String result = p_generate(prefix3);
        return result;
    }

    private static final String p_generate(final String prefix){
        // 生成后8位数字
        StringBuilder stringBuilder = new StringBuilder(prefix);
        for (int i = 0; i < 8; i++) {
            int number = RandomUtil.getInt(0, 9);
            stringBuilder.append(number);
        }
        String result = stringBuilder.toString();
        return result;
    }

    /**
     * 2025-10-24
     * 验证手机号码格式是否有效 [5,8](@ref)
     * @param number 手机号码
     * @return 是否有效
     */
    public static final boolean check(final String number) {
        if (number == null || number.length() != 11) {
            return false;
        }
        // 检查是否以1开头
        if (number.charAt(0) != '1') {
            return false;
        }
        // 检查是否全部为数字
        for (int i = 0; i < number.length(); i++) {
            if (!Character.isDigit(number.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 2025-10-24
     * 根据号码前缀判断运营商
     * @param number 手机号码
     * @return 运营商名称，未知返回"未知运营商"
     */
    public static final String getCarrier(final String number) {
        if (!check(number)) {
            return "ERROR_无效号码";
        }
        String prefix = number.substring(0, 3);
        // 检查中国移动号段
        for (String mobilePrefix : CHINA_MOBILE_ARRAY) {
            if (mobilePrefix.equals(prefix)) {
                return CHINA_MOBILE;
            }
        }
        // 检查中国联通号段
        for (String unicomPrefix : CHINA_UNICOM_ARRAY) {
            if (unicomPrefix.equals(prefix)) {
                return CHINA_UNICOM;
            }
        }
        // 检查中国电信号段
        for (String telecomPrefix : CHINA_TELECOM_ARRAY) {
            if (telecomPrefix.equals(prefix)) {
                return CHINA_TELECOM;
            }
        }
        return "ERROR_无效运营商";
    }

    private static final String p_getPrefix() {
        int index = RandomUtil.getInt(0, 2);
        String type = CHINA_MOBILE;
        if(index == 1) {
            type = CHINA_UNICOM;
        }else if(index == 2) {
            type = CHINA_TELECOM;
        }
        String prefix = p_getPrefix(type);
        return prefix;
    }

    private static final String p_getPrefix(final String type) {
        String[] array = p_getArrayByType(type);
        int index2 = RandomUtil.getInt(0, array.length - 1);
        String prefix = array[index2];
        return prefix;
    }

    /**
     * 根据号码前缀获取对应的运营商前缀数组
     */
    private static final String[] p_getArrayByType(final String type) {
        if(CHINA_MOBILE.equals(type)){
            return CHINA_MOBILE_ARRAY;
        }
        if(CHINA_UNICOM.equals(type)){
            return CHINA_UNICOM_ARRAY;
        }
        if(CHINA_TELECOM.equals(type)){
            return CHINA_TELECOM_ARRAY;
        }
        return CHINA_MOBILE_ARRAY;
    }

}