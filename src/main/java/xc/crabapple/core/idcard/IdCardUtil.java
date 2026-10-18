package xc.crabapple.core.idcard;

import xc.crabapple.core.collection.ListUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 2022-07-21
 * 中国大陆18位身份证号生成与校验工具类
 * 1、号码的结构
 * 公民身份号码是特征组合码，由十七位数字本体码和一位校验码组成。从左至右依次为：六位数字地址码，
 * 八位数字出生日期码，三位数字顺序码和一位数字校验码。
 * 2、地址码(前六位数）
 * 表示编码对象常住户口所在县(市、旗、区)的行政区划代码，按GB/T2260的规定执行。
 * 3、出生日期码（第七位至十四位）
 * 表示编码对象出生的年、月、日，按GB/T7408的规定执行，年、月、日代码之间不用分隔符。
 * 4、顺序码（第十五位至十七位）
 * 表示在同一地址码所标识的区域范围内，对同年、同月、同日出生的人编定的顺序号，
 * 顺序码的奇数分配给男性，偶数分配给女性。
 * 5、校验码（第十八位数）
 * （1）十七位数字本体码加权求和公式 S = Sum(Ai Wi), i = 0, , 16 ，先对前17位数字的权求和 ;
 *     Ai:表示第i位置上的身份证号码数字值; Wi:表示第i位置上的加权因子 Wi: 7 9 10 5 8 4 2 1 6 3 7 9 10 5 8 4 2
 * （2）计算模 Y = mod(S, 11)
 * （3）通过模( 0 1 2 3 4 5 6 7 8 9 10)得到对应的校验码 Y:1 0 X 9 8 7 6 5 4 3 2
 */
public final class IdCardUtil {

    private static Map<String, String> p_provinceMap = null;

    /**
     * 2024-09-30
     * 随机生成指定个数的中国大陆18位身份证号
     * @param count
     * @return
     */
    public static final List<String> create(final int count){
        if(count <= 0){
            return new ArrayList<>();
        }
        int localCount = count;
        if(localCount > 10000){
            localCount = 10000;
        }
        //
        List<String> list = new ArrayList<>();
        for(int i = 0; i < localCount; i++){
            String idCard = create();
            list.add(idCard);
        }
        return list;
    }

    /**
     * 2024-09-30
     * 随机生成一个中国大陆18位身份证号
     * @return
     */
    public static final String create() {
        p_setProvinceMap();
        List<String> areaList = new ArrayList<>();
        Set<String> set = p_provinceMap.keySet();
        Iterator<String> iterator = set.iterator();
        while(iterator.hasNext()){
            String provinceCode = iterator.next();
            String areaCode = provinceCode + "0101";  // 目前不全，只列出了编号均为01的城市与区县。
            areaList.add(areaCode);
        }
        final String areaCode = ListUtil.getRandom(areaList);
        // 随机生成出生日期（这里假设在1900-2024年间）
        final Random random = new Random();
        int year = 1900 + random.nextInt(75);
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28); // 简化处理，不考虑每个月的实际天数
        String birthDate = String.format("%04d%02d%02d", year, month, day);
        // 生成顺序码，这里简单处理为001到999之间的随机数
        int next2 = random.nextInt(999);
        String sequenceCode = String.format("%03d", next2);
        // 拼接前17位
        String baseId = areaCode + birthDate + sequenceCode;
        // 计算校验码
        char checkDigit = p_calculateCheckDigit(baseId);
        // 返回完整的身份证号码
        final String idCard = baseId + checkDigit;
        return idCard;
    }

    private static char p_calculateCheckDigit(final String baseId) {
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] checkDigits = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            int weight = weights[i];
            int aaa = baseId.charAt(i) - '0';
            sum += aaa * weight;
        }
        char chr = checkDigits[sum % 11];
        return chr;
    }

    /**
     * 2024-09-30
     * 中国大陆18位身份证号校验
     * @param idCard
     * @return
     */
    public static final boolean check(final String idCard){
        if (StringUtil.isBlank(idCard)){
            return false;
        }
        // 长度
        final int length = idCard.length();
        if(length != 18){
            return false;
        }
        // 定义判别用户身份证号的正则表达式（15位或者18位，最后一位可以为字母）
        final String REGULAR_EXPRESSION = "(^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$)|(^[1-9]\\d{5}\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}$)";
        if(!idCard.matches(REGULAR_EXPRESSION)){
            return false;
        }
        //
        if(!p_checkLastNum(idCard)){
            return false;
        }
        // 校验省级代码
        final String provinceCode = idCard.substring(0, 2);  // 6位，地区代码
        p_setProvinceMap();
        if(!p_provinceMap.containsKey(provinceCode)){
            return false;
        }
        // 校验生日
        final String birthday = idCard.substring(6, 14);  // 8位，出生日期
        if(!p_checkBirthday(birthday)){
            return false;
        }
        return true;
    }

    public static final void p_setProvinceMap(){
        if(p_provinceMap != null){
            return;
        }
        //
        p_provinceMap = new HashMap(34);
        p_provinceMap.put("11","北京");
        p_provinceMap.put("12","天津");
        p_provinceMap.put("13","河北");
        p_provinceMap.put("14","山西");
        p_provinceMap.put("15","内蒙古");
        p_provinceMap.put("21","辽宁");
        p_provinceMap.put("22","吉林");
        p_provinceMap.put("23","黑龙江");
        p_provinceMap.put("31","上海");
        p_provinceMap.put("32","江苏");
        p_provinceMap.put("33","浙江");
        p_provinceMap.put("34","安徽");
        p_provinceMap.put("35","福建");
        p_provinceMap.put("36","江西");
        p_provinceMap.put("37","山东");
        p_provinceMap.put("41","河南");
        p_provinceMap.put("42","湖北");
        p_provinceMap.put("43","湖南");
        p_provinceMap.put("44","广东");
        p_provinceMap.put("45","广西");
        p_provinceMap.put("46","海南");
        p_provinceMap.put("50","重庆");
        p_provinceMap.put("51","四川");
        p_provinceMap.put("52","贵州");
        p_provinceMap.put("53","云南");
        p_provinceMap.put("54","西藏");
        p_provinceMap.put("61","陕西");
        p_provinceMap.put("62","甘肃");
        p_provinceMap.put("63","青海");
        p_provinceMap.put("64","宁夏");
        p_provinceMap.put("65","新疆");
        p_provinceMap.put("81","香港");
        p_provinceMap.put("82","澳门");
        p_provinceMap.put("83","台湾");
    }

    private static final boolean p_checkLastNum(final String idCard){
        try{
            char[] charArray = idCard.toCharArray();
            // 前十七位加权因子
            // 这是除以11后，可能产生的11位余数对应的验证码
            int sum = 0;
            int[] COEFFICIENT_ARRAY = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};  // 身份证校验码
            for(int i = 0; i < COEFFICIENT_ARRAY.length; i++){
                char chr = charArray[i];
                String chrStr = String.valueOf(chr);
                int current = Integer.parseInt(chrStr);
                int count = current * COEFFICIENT_ARRAY[i];
                sum += count;
            }
            char idCardLast = charArray[17];
            String idCardLastStr = String.valueOf(idCardLast);
            idCardLastStr = StringUtil.toUpperCase(idCardLastStr);
            final int idCardMod = sum % 11;
            // 身份证号的尾数规则
            final String[] IDENTITY_MANTISSA = {"1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2"};
            String mantissa = IDENTITY_MANTISSA[idCardMod];
            mantissa = StringUtil.toUpperCase(mantissa);
            if(mantissa.equals(idCardLastStr)){
                return true;
            }
            return false;
        }catch(Throwable throwable){
            LogUtil.error("IdCardUtil_p_checkLastNum_t1", throwable, idCard);
            return false;
        }
    }

    /**
     * 校验生日
     * @param birthday
     * @return
     */
    private static boolean p_checkBirthday(final String birthday){
        // 出生日期不能晚于当前时间，并且不能早于1900年
        try{
            final Date birthDate = new SimpleDateFormat("yyyyMMdd").parse(birthday);
            boolean flag = false;
            if(birthDate != null){
                final boolean beforeFlag = birthDate.before(new Date());
                final Date MINIMAL_BIRTH_DATE = new Date(-2209017600000L);  // 身份证的最小出生日期，1900年1月1日
                final boolean afterFlag = birthDate.after(MINIMAL_BIRTH_DATE);
                if(beforeFlag && afterFlag){
                    flag = true;
                }
            }
            // 出生日期中的年、月、日必须正确,比如月份范围是[1,12]，日期范围是[1,31]，还需要校验闰年、大月、小月的情况时，月份和日期相符合。
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
            final String realBirthdayPart = simpleDateFormat.format(birthDate);
            if(flag && birthday.equals(realBirthdayPart)){
                return true;
            }
            return false;
        }catch(Throwable throwable){
            LogUtil.error("IdCardUtil_p_checkBirthday_t1", throwable, birthday);
            return false;
        }
    }

}