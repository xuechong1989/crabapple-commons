package xc.crabapple.core.math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import xc.crabapple.core.collection.ListUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;

/**
 * 2016-11-01 1.0.0
 * 2025-02-20 重构除法运算divide/divideReturnDouble方法
 */
public final class MathUtil{

    /**
     * 2025-02-26
     * 从一个整数的最低位（个位）开始替换指定个数的数字为0
     * @param num
     * @param zeroCount
     * 调用示例：
     * (12345, 1) ==> 12340
     * (12345, 2) ==> 12300
     * (12345, 3) ==> 12000
     */
    public static final Long shequ(final int num, final int zeroCount) {
        Integer divisor = (int) Math.pow(10, zeroCount);
        Long divisorLong = divisor.longValue();
        Long highPart = num / divisorLong;
        Long result = highPart * divisor;
        return result;
    }

    /**
     * 2025-02-26
     * @param num
     * @param zeroCount
     * @return
     */
    public static final Long sishewuru(final Integer num, final Integer zeroCount){
        Long result = sishewuru(num, zeroCount, true);
        return result;
    }

    /**
     * 2025-02-26
     * 四舍五入计算
     * @param num 原整型数字
     * @param zeroCount 保留结尾几个0
     * @param force 是否严格按照zeroCount参数指定的值计算返回值。true-严格计算，false-自动减少保留0的个数。
     * 调用示例：
     *    (182, 2, true) ==> 200
     *    (489, 1, true) ==> 490
     *    (489, 2, true) ==> 500
     *    (489, 3, true) ==> 0
     *    (489, 3, false) ==> 500
     *    (784, 1, true) ==> 780
     *    (784, 2, true) ==> 800
     *    (784, 3, true) ==> 1000
     *    (784, 4, true) ==> 0
     *    (784, 4, false) == 1000
     *    (16357, 3, true) ==> 16000
     * @return
     */
    public static final Long sishewuru(final Integer num, final Integer zeroCount, final boolean force){
        Integer localZeroCount = zeroCount;
        if(!force){
            String numStr = num.toString();
            int numCount = numStr.length();
            if(numCount <= localZeroCount){
                localZeroCount = Math.min(numCount, localZeroCount);
                char firstChr = numStr.charAt(0);
                String firstStr = String.valueOf(firstChr);
                Integer firstInt = Integer.valueOf(firstStr);
                if(firstInt <= 4){
                    localZeroCount--;
                }
            }
        }
        // 计算四舍五入的基数（10的zeros次幂）
        double ddd = Math.pow(10, localZeroCount);
        long baseLong = (long) ddd;
        // 四舍五入操作
        double numDouble = (double) num;
        long roundedNumber = Math.round(numDouble / baseLong) * baseLong;
        return roundedNumber;
    }

    /**
     * 2023-06-12 取余与取模计算示例
     * 索引  取余  取模
     * 0,    0,    0
     * 1,    0,    1
     * 2,    0,    2
     * 3,    1,    0
     * 4,    1,    1
     * 5,    1,    2
     * 6,    2,    0
     * 7,    2,    1
     */
    public static final void demo(){
        final int total = 8;  // 总数量
        final int countPerGroup = 3;  // 每组数量
        for(int i = 0; i < total; i++){
            LogUtil.print("MathUtil_demo_1", "取余与取模", i + ",    " + (i/countPerGroup) + ",    " + (i%countPerGroup));
        }
    }

    /**
     * 2023-06-12
     * @param list 源数据集合
     * @param countPerGroup 每组数量
     * 使用示例：(["a", "b", "c", "d", "e", "f", "g", "h", "i", "j"], 3)  ==> [["a", "b", "c", "d"], ["e", "f", "g", "h"], ["i", "j"]]
     */
    public static final List<List<String>> split(final List<String> list, final Integer countPerGroup){
        List<List<String>> resultList = ListUtil.split(list, countPerGroup);
        return resultList;
    }

    /**
     * 2022-12-22
     *
     * 基础知识请查看文件：D:/docs/签章签名-坐标与尺寸.txt
     *
     * 像素 px
     * 密度 dpi
     * 英寸 inch
     * 毫米 mm
     *
     * mm / 25.4 = inch
     * inch * 25.4 = mm
     * px / dpi = inch
     * px / dpi * 25.4 = mm
     * mm * dpi / 25.4 = px
     *
     * A4纸是由国际标准化组织的ISO-216定义，规格为：210mm × 297mm，世界上多数国家所使用的纸张尺寸都是采用这一国际标准。
     * 分辨率 72像素/英寸（DPI），A4纸像素宽高 595 × 842
     * 分辨率120像素/英寸（DPI），A4纸像素宽高 1487 × 2105
     * 分辨率150像素/英寸（DPI），A4纸像素宽高 1240 × 1754
     * 分辨率300像素/英寸（DPI），A4纸像素宽高 2479 × 3508
     */

    /**
     * 2022-12-22
     * 毫米 ==> 英寸
     */
    public static final double mm2inch(final double mm){
        double inch = mm / 25.4;
        return inch;
    }

    /**
     * 2022-12-22
     * 英寸 ==> 毫米
     */
    public static final double inch2mm(final double inch){
        double mm = inch * 25.4;
        return mm;
    }

    /**
     * 2022-12-22
     * 像素、DPI ==> 英寸
     */
    public static final double px2inch(final double px, final int dpi){
        double inch = px / dpi;
        return inch;
    }

    /**
     * 2022-12-22
     * 像素、DPI ==> 毫米
     */
    public static final double px2mm(final double px, final int dpi){
        double mm = px / dpi * 25.4;
        return mm;
    }

    /**
     * 2022-12-22
     * 毫米、DPI ==> 像素
     */
    public static final double mm2px(final double mm, final int dpi){
        double px = mm * dpi / 25.4;
        return px;
    }

    /**
     * 2020-03-11 5.0.0
     * 小写数字转大写数字，目前只支持个位数和十位数，正数
     * 例如：small2big(25) ==> 二十五
     * @param num
     */
    public static final String small2big(final Integer num){
        if(num == null){
            return "num_is_null";
        }
        String str = num.toString();
        //个位数字
        if(str.length() == 1){
            String sss = p_small2big(str);
            return sss;
        }
        //十位数字
        if(str.length() == 2){
            StringBuilder stringBuilder = new StringBuilder();
            for(int i = 0; i < str.length(); i++){
                char c = str.charAt(i);
                String s = StringUtil.char2str(c);
                if(i == 0){  //十位上的数
                    if("1".equals(s)){
                        stringBuilder.append("十");
                    }else{
                        String big = p_small2big(s);
                        stringBuilder.append(big).append("十");
                    }
                    continue;
                }
                if(!"0".equals(s)){
                    String big = p_small2big(s);
                    stringBuilder.append(big);
                }
            }
            String sss = stringBuilder.toString();
            return sss;
        }
        return "no_supported";
    }

    /**
     * 2020-03-18 5.0.0
     * 大写数字转小写数字，目前只支持个位数和十位数，正数
     * 例如：big2small("二十五") ==> 25
     * @param big
     */
    public static final Integer big2small(final String big){
        if(StringUtil.isBlank(big)){
            return -1;
        }
        int length = big.length();
        if(length == 1){  //零，一，二，九，十
            Integer num = p_big2small(big);
            return num;
        }
        if(length == 2){  //十一，十九，二十，三十，九十
            String first = StringUtil.substr(big, 0, 1);
            String second = StringUtil.substr(big, 1, 2);
            if("十".equals(first)){  //十一，十九
                Integer second2 = p_big2small(second);
                String tmp = "1" + second2;
                Integer result = Integer.valueOf(tmp);
                return result;
            }
            Integer first2 = p_big2small(first);
            String tmp = first2 + "0";
            Integer result = Integer.valueOf(tmp);
            return result;
        }
        if(length == 3){  //二十一，二十二，三十一，九十九
            String first = StringUtil.substr(big, 0, 1);
            String third = StringUtil.substr(big, 2, 3);
            Integer first2 =  p_big2small(first);
            Integer third2 = p_big2small(third);
            String tmp = first2 + "" + third2;
            Integer result = Integer.valueOf(tmp);
            return result;
        }
        return -2;
    }

    /**
     * 2020-03-18 5.0.0
     * 大写数字转小写数字，单数字转换
     * @param big
     */
    private static final Integer p_big2small(final String big){
        if(StringUtil.isBlank(big)){
            return -3;
        }
        if("零".equals(big)){
            return 0;
        }
        if("一".equals(big)){
            return 1;
        }
        if("二".equals(big)){
            return 2;
        }
        if("三".equals(big)){
            return 3;
        }
        if("四".equals(big)){
            return 4;
        }
        if("五".equals(big)){
            return 5;
        }
        if("六".equals(big)){
            return 6;
        }
        if("七".equals(big)){
            return 7;
        }
        if("八".equals(big)){
            return 8;
        }
        if("九".equals(big)){
            return 9;
        }
        if("十".equals(big)){
            return 10;
        }
        return -4;
    }

    /**
     * 2020-03-11 5.0.0
     * 小写数字转大写数字，单数字转换
     * @param snum
     */
    private static final String p_small2big(final String snum){
        if(StringUtil.isBlank(snum)){
            return "-";
        }
        Integer num = Integer.valueOf(snum);
        String sss = p_small2big(num);
        return sss;
    }

    /**
     * 2020-03-11 5.0.0
     * 小写数字转大写数字，单数字转换
     * @param num
     */
    private static final String p_small2big(final Integer num){
        if(num == 0){
            return "零";
        }
        if(num == 1){
            return "一";
        }
        if(num == 2){
            return "二";
        }
        if(num == 3){
            return "三";
        }
        if(num == 4){
            return "四";
        }
        if(num == 5){
            return "五";
        }
        if(num == 6){
            return "六";
        }
        if(num == 7){
            return "七";
        }
        if(num == 8){
            return "八";
        }
        if(num == 9){
            return "九";
        }
        return "-";
    }

    /**
     *
     * @param paramA
     * @param paramB
     * @return
     */
    public static final String divide(final Integer paramA, final Integer paramB){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, 2, false, false);
        return result;
    }

    /**
     *
     * @param paramA
     * @param paramB
     * @param decimalLength
     * @return
     */
    public static final String divide(final Integer paramA, final Integer paramB, final int decimalLength){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, decimalLength, false, false);
        return result;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @param decimalLength
     * @param percentage
     * @return
     */
    public static final String divide(final Integer paramA, final Integer paramB, final int decimalLength, final boolean percentage){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, decimalLength, percentage, false);
        return result;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @param decimalLength
     * @param percentage
     * @param plus
     * @return
     */
    public static final String divide(final Integer paramA, final Integer paramB, final int decimalLength, final boolean percentage, final boolean plus){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, decimalLength, percentage, plus);
        return result;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @return
     */
    public static final String divide(final Long paramA, final Long paramB){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, 2, false, false);
        return result;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @param decimalLength
     * @return
     */
    public static final String divide(final Long paramA, final Long paramB, final int decimalLength){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, decimalLength, false, false);
        return result;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @param decimalLength
     * @param percentage
     * @return
     */
    public static final String divide(final Long paramA, final Long paramB, final int decimalLength, final boolean percentage){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, decimalLength, percentage, false);
        return result;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @param decimalLength
     * @param percentage
     * @param plus
     * @return
     */
    public static final String divide(final Long paramA, final Long paramB, final int decimalLength, final boolean percentage, final boolean plus){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, decimalLength, percentage, plus);
        return result;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @return
     */
    public static final String divide(final Float paramA, final Float paramB){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, 2, false, false);
        return result;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @param decimalLength
     * @return
     */
    public static final String divide(final Float paramA, final Float paramB, final int decimalLength){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, decimalLength, false, false);
        return result;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @param decimalLength
     * @param percentage
     * @return
     */
    public static final String divide(final Float paramA, final Float paramB, final int decimalLength, final boolean percentage){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, decimalLength, percentage, false);
        return result;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @param decimalLength
     * @param percentage
     * @param plus
     * @return
     */
    public static final String divide(final Float paramA, final Float paramB, final int decimalLength, final boolean percentage, final boolean plus){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, decimalLength, percentage, plus);
        return result;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @return
     */
    public static final String divide(final Double paramA, final Double paramB) {
        final String result = divide(paramA, paramB, 2, false, false);
        return result;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @param decimalLength
     * @return
     */
    public static final String divide(final Double paramA, final Double paramB, final int decimalLength) {
        final String result = divide(paramA, paramB, decimalLength, false, false);
        return result;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @param decimalLength
     * @param percentage
     * @return
     */
    public static final String divide(final Double paramA, final Double paramB, final int decimalLength, final boolean percentage) {
        final String result = divide(paramA, paramB, decimalLength, percentage, false);
        return result;
    }

    /**
     * 2025-02-20
     * @param paramA 除数
     * @param paramB 被除数
     * @param decimalLength 保留几位小数
     * @param percentage 是否采用百分制
     * @param plus 当paramA大于0时结果左侧是否显示+号
     * @return
     */
    public static final String divide(final Double paramA, final Double paramB, final int decimalLength, final boolean percentage, final boolean plus) {
        if (paramB == 0) {
            LogUtil.error("", "除数不能为零", paramA, paramB, decimalLength);
            if(percentage){
                return "0%";
            }
            return "0";
        }
        //
        int localDecimalLength = decimalLength;
        if(localDecimalLength < 0){
            localDecimalLength = 0;
        }
        Double localParamA = paramA;
        if(percentage){
            localParamA *= 100;
        }
        BigDecimal bigDecimalA = new BigDecimal(localParamA);
        BigDecimal bigDecimalB = new BigDecimal(paramB);
        BigDecimal bigDecimal = bigDecimalA.divide(bigDecimalB,  localDecimalLength, RoundingMode.HALF_UP);  // 四舍五入
        double ddd = bigDecimal.doubleValue();
        // 0
        if(ddd == 0.0){
            if(localDecimalLength <= 0){
                if(percentage){
                    return "0%";
                }
                return "0";
            }
            String result = "0.0";
            for(int i = 1; i < localDecimalLength; i++){
                result += "0";
            }
            if(percentage){
                result += "%";
                return result;
            }
            return result;
        }
        // 非0
        String result = bigDecimal.toPlainString();
        if(percentage){
            result += "%";
            if(plus && localParamA > 0){
                result = "+" + result;
                return result;
            }
            return result;
        }
        if(plus && localParamA > 0){
            result = "+" + result;
            return result;
        }
        return result;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @return
     */
    public static final Double divideReturnDouble(final Integer paramA, final Integer paramB){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, 2, false, false);
        final Double ddd = Double.valueOf(result);
        return ddd;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @param decimalLength
     * @return
     */
    public static final Double divideReturnDouble(final Integer paramA, final Integer paramB, final int decimalLength){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, decimalLength, false, false);
        final Double ddd = Double.valueOf(result);
        return ddd;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @return
     */
    public static final Double divideReturnDouble(final Long paramA, final Long paramB){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, 2, false, false);
        final Double ddd = Double.valueOf(result);
        return ddd;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @param decimalLength
     * @return
     */
    public static final Double divideReturnDouble(final Long paramA, final Long paramB, final int decimalLength){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, decimalLength, false, false);
        final Double ddd = Double.valueOf(result);
        return ddd;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @return
     */
    public static final Double divideReturnDouble(final Float paramA, final Float paramB){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, 2, false, false);
        final Double ddd = Double.valueOf(result);
        return ddd;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @param decimalLength
     * @return
     */
    public static final Double divideReturnDouble(final Float paramA, final Float paramB, final int decimalLength){
        final Double pa = Double.valueOf(paramA);
        final Double pb = Double.valueOf(paramB);
        final String result = divide(pa, pb, decimalLength, false, false);
        final Double ddd = Double.valueOf(result);
        return ddd;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @return
     */
    public static final Double divideReturnDouble(final Double paramA, final Double paramB) {
        final String result = divide(paramA, paramB, 2, false, false);
        final Double ddd = Double.valueOf(result);
        return ddd;
    }

    /**
     * 2025-02-20
     * @param paramA
     * @param paramB
     * @param decimalLength
     * @return
     */
    public static final Double divideReturnDouble(final Double paramA, final Double paramB, final int decimalLength) {
        final String result = divide(paramA, paramB, decimalLength, false, false);
        final Double ddd = Double.valueOf(result);
        return ddd;
    }

    /**
     * 2017-06-13 1.0.0
     * 获得指定位数的随机数，位数数量为[1-9]包含
     * @param n 要指定的位数
     */
    public static final Integer random(final Integer n){
        Integer num = RandomUtil.getInt(n);
        return num;
    }

    /**
     * 2017-06-23 1.0.0
     * 返回指定的两数及其之间的随机整数，[0, Integer.MAX_VALUE]
     */
    public static final int randomBetween(final Integer min, final Integer max){
        int num = RandomUtil.getInt(min, max);
        return num;
    }

    /**
     * 2017-06-23 1.0.0
     * 按照四舍五入设置保留几位小数，如12.348 ==> 12.35
     * @param srcno 原数
     * @param num 小数点后保留几位
     */
    public static final String getDecimalNum(final Double srcno, final Integer num){
        BigDecimal bigDecimal = new BigDecimal(srcno);
        BigDecimal bigDecimal2 = bigDecimal.setScale(num, BigDecimal.ROUND_HALF_UP);
        double ddd = bigDecimal2.doubleValue();
        String result = String.valueOf(ddd);
        String[] arr = StringUtil.split(result, ".");
        String dec = arr[1];
        int n = num - dec.length();
        String zero = "";
        for(int i = 0; i < n; i++){
            zero += "0";
        }
        result += zero;
        return result;
    }

    /**
     * 2018-07-18 1.1.0
     * 占用百分比，第1个参数必须比第2个参数小才有意义
     * @param aaa 小
     * @param bbb 大
     * 调用示例：occupancyPercentage(2, 3) ==> 66.66%
     */
    public static final String occupancyPercentage(final Object aaa, final Object bbb){
        double ccc = Double.valueOf(aaa.toString()) / Double.valueOf(bbb.toString());
        final String ddd = MathUtil.getDecimalNum(ccc, 4);
        final String eee = StringUtil.substr(ddd, 2, 4);
        final String fff = StringUtil.substr(ddd, 4, 6);
        final String ggg = eee + "." + fff + "%";
        return ggg;
    }

    /**
     * 2018-07-18 1.1.0
     * 占用百分比，参数必须是小数才有意义
     * @param aaa
     * 调用示例：occupancyPercentage(0.2) ==> 20.00%
     */
    public static final String occupancyPercentage(final Double aaa){
        final String ddd = MathUtil.getDecimalNum(aaa, 4);
        final String eee = StringUtil.substr(ddd, 2, 4);
        final String fff = StringUtil.substr(ddd, 4, 6);
        final String ggg = eee + "." + fff + "%";
        return ggg;
    }

    /**
     * 2017-05-26 1.0.0
     * 2024-11-27 修复在整除时且块数与每块元素数相等时的问题
     * 均分算法（均衡算法），把sourceNumber分成chunks块，每块里面的元素数量尽可能均分
     * 示例：
     * (9, 3)  ==> 3,3,3
     * (20, 4) ==> 5,5,5,5
     * (16, 5) ==> 4,3,3,3,3
     * (19, 5) ==> 4,4,4,4,3
     * @param sourceNumber
     * @param chunks 块数
     */
    public static final List<Long> average(final long sourceNumber, final long chunks){
        List<Long> list = new ArrayList<>();
        if(chunks == 0){
            return list;
        }
        //
        if(chunks > sourceNumber){
            for(long i = 0; i < chunks; i++){
                long v = 1;
                if(i >= sourceNumber){
                    v = 0;
                }
                list.add(v);
            }
            return list;
        }
        //
        if(sourceNumber % chunks == 0){
            long value = sourceNumber / chunks;
            for(long i = 0; i < sourceNumber; i+=value){
                list.add(value);
            }
            return list;
        }
        //
        long tmp = sourceNumber;
        while(true){
            tmp++;
            if(tmp % chunks == 0){
                long shang1 = tmp / chunks;
                long shang2 = shang1 - 1;
                long cha2 = tmp - sourceNumber;
                long cha1 = chunks - cha2;
                for(long i = 0; i < cha1; i++){
                    list.add(shang1);
                }
                for(long i = 0; i < cha2; i++){
                    list.add(shang2);
                }
                return list;
            }
        }
    }

    /**
     * 2017-05-26 1.0.0
     * 已知总大小和每块大小，计算可以分多少块
     * @param total 总大小
     * @return chunkSize 每块大小
     */
    public static final int getChunkNum(final Long total, final Integer chunkSize){
        if(total >= 0 && total <= chunkSize){
            return 1;
        }else if(total > chunkSize){
            double num = (total * 1d) / chunkSize;
            int n = (int)num;
            if(num > n){
                return n + 1;
            }else{
                return n;
            }
        }else{
            return -1;
        }
    }

    /**
     * 2017-05-26 1.0.0
     * 跟据文件位置和每块大小，计算文件块位置
     * @param contentPos 文件流位置
     * @return chunkSize 每块大小
     */
    public static final int getChunkPos(final Long contentPos, final Integer chunkSize){
        if(contentPos >= 0 && contentPos < chunkSize){
            return 1;
        }else if(contentPos >= chunkSize){
            long num = contentPos / chunkSize;
            int n = (int)num;
            return n + 1;
        }else{
            return -1;
        }
    }

    /**
     * 2018-04-18 1.1.0
     * GB转字节
     */
    public static final Long gb2byte(final String sgb){
        Long gb = Long.parseLong(sgb);
        long result = gb2byte(gb);
        return result;
    }

    /**
     * 2017-05-26 1.0.0
     * GB转字节
     */
    public static final Long gb2byte(final Long gb){
        long result = 0L;
        if(gb == null){
            return result;
        }
        result = gb * 1024 * 1024 * 1024;
        return result;
    }

    /**
     * 2018-04-18 1.1.0
     * KB转字节
     */
    public static final Long kb2byte(final String skb){
        Long kb = Long.parseLong(skb);
        long result = kb2byte(kb);
        return result;
    }

    /**
     * 2017-05-26 1.0.0
     * KB转字节
     */
    public static final Long kb2byte(final Long kb){
        long result = 0L;
        if(kb == null){
            return result;
        }
        result = kb * 1024;
        return result;
    }

    /**
     * 2018-04-18 1.1.0
     * 字节转KB
     */
    public static final double byte2kb(final String sb){
        Long b = Long.parseLong(sb);
        double result = byte2kb(b);
        return result;
    }

    /**
     * 2017-05-26 1.0.0
     * 字节转KB
     */
    public static final double byte2kb(final Long b){
        double result = 0D;
        if(b == null){
            return result;
        }
        double d = b / 1024.00;
        BigDecimal bd = new BigDecimal(d);
        BigDecimal bd2 = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        result = bd2.doubleValue();
        return result;
    }

    /**
     * 2018-04-18 1.1.0
     * 字节转MB
     */
    public static final double byte2mb(final String sb){
        Long b = Long.parseLong(sb);
        double result = byte2mb(b);
        return result;
    }

    /**
     * 2017-05-26 1.0.0
     * 字节转MB
     */
    public static final double byte2mb(final Long b){
        double result = 0D;
        if(b == null){
            return result;
        }
        double d = b / 1024.00 / 1024.00;
        BigDecimal bd = new BigDecimal(d);
        BigDecimal bd2 = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        result = bd2.doubleValue();
        return result;
    }

    /**
     * 2017-05-26 1.0.0
     * 字节转GB
     */
    public static final double byte2gb(final Long b){
        double result = 0D;
        if(b == null){
            return result;
        }
        double d = b / 1024.00 / 1024.00 / 1024.00;
        BigDecimal bd = new BigDecimal(d);
        BigDecimal bd2 = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        result = bd2.doubleValue();
        return result;
    }

    /**
     * 2017-05-26 1.0.0
     * 获取字节转换 数据
     */
    public static final String getFilesSize(final Long size){
        String str = "";
        DecimalFormat df = new DecimalFormat("######0.00");
        double a;
        a = byte2kb(size);
        if(a < 1){
            str = size + " B";
        }else if(a >= 1 && a < 1024){
            str = df.format(a) + " KB";
        }else if(a >= 1024){
            a = byte2mb(size);
            if(a >= 1 && a < 1024){
                str = df.format(a) + " MB";
            }else if(a >= 1024){
                a = byte2gb(size);
                if(a >= 1 && a < 1024){
                    str = df.format(a) + " GB";
                }else if(a >= 1024){
                    a = a / 1024;
                    str = df.format(a) + " TB";
                }
            }
        }
        return str;
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
        Boolean minus = false;  //是否为负数
        Long localNum = num;
        if(localNum < 0){
            minus = true;
            localNum = -localNum;
        }
        String str = String.valueOf(localNum);
        str = StringUtil.getPureStr(str);
        if(StringUtil.isBlank(str)){
            if(minus){
                str = "-" + str;
            }
            return str;
        }
        int n = str.length();
        if(n <= 3){
            if(minus){
                str = "-" + str;
            }
            return str;
        }
        String result = "";
        for(int i = str.length() - 1; i >= 0; i--){
            char c = str.charAt(i);
            int j = n - i;
            if(j % 3 == 0){
                result = "," + c + result;
            }else{
                result = c + result;
            }
        }
        if(result.indexOf(",") == 0){
            result = result.substring(1, result.length());
        }
        if(minus){
            result = "-" + result;
        }
        return result;
    }

    /**
     * 2019-07-13 2.3.2
     * 是否为数字类型字符串
     * @param str
     */
    public static final boolean isNumber(final String str) {
        boolean flag = StringUtil.isNumber(str);
        return flag;
    }

    /**
     * 2019-07-13 2.3.2
     * 是否为Integer类型字符串
     * @param str
     */
    public static final boolean isInteger(final String str){
        boolean flag = StringUtil.isInteger(str);
        return flag;
    }

    /**
     * 2019-07-13 2.3.2
     * 是否为Long类型字符串
     * @param str
     */
    public static final boolean isLong(final String str) {
        boolean flag = StringUtil.isLong(str);
        return flag;
    }

}
