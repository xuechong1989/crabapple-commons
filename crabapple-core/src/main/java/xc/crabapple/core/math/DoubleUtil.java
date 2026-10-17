package xc.crabapple.core.math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import xc.crabapple.core.log.LogUtil;

/**
 * 2018-12-07
 */
public final class DoubleUtil {

    /**
     * 2018-12-07 2.0.0
     * 提供精确的加法运算，如add(1.2, 2.3) ==> 3.5
     * @param v1 被加数
     * @param v2 加数
     * @return 两个参数的和
     */
    public static final double add(final Double v1, final Double v2) {
        String s1 = Double.toString(v1);
        BigDecimal b1 = new BigDecimal(s1);
        String s2 = Double.toString(v2);
        BigDecimal b2 = new BigDecimal(s2);
        BigDecimal b = b1.add(b2);
        double result = b.doubleValue();
        return result;
    }

    /**
     * 2018-12-07 2.0.0
     * 提供精确的减法运算，sub(3.5, 2.1) ==> 1.4
     * @param v1 被减数
     * @param v2 减数
     * @return 两个参数的差
     */
    public static final double sub(final Double v1, final Double v2) {
        String s1 = Double.toString(v1);
        BigDecimal b1 = new BigDecimal(s1);
        String s2 = Double.toString(v2);
        BigDecimal b2 = new BigDecimal(s2);
        BigDecimal b = b1.subtract(b2);
        double result = b.doubleValue();
        return result;
    }

    /**
     * 2018-12-07 2.0.0
     * 提供精确的乘法运算，mul(3.5, 2) ==> 7.0
     * @param v1 被乘数
     * @param v2 乘数
     * @return 两个参数的积
     */
    public static final double mul(final Double v1, final Double v2) {
        String s1 = Double.toString(v1);
        BigDecimal b1 = new BigDecimal(s1);
        String s2 = Double.toString(v2);
        BigDecimal b2 = new BigDecimal(s2);
        BigDecimal b = b1.multiply(b2);
        double result = b.doubleValue();
        return result;
    }

    /**
     * 2020-08-06 6.4.0
     * 提供（相对）精确的除法运算。非整除时scale指定精度四舍五入。
     * @param v1 被除数
     * @param v2 除数
     * @param scale 表示表示需要精确到小数点以后几位。div(10, 3, 2) ==> 3.33
     * @return 两个参数的商
     */
    public static final double div(final Integer v1, final Integer v2, final Integer scale){
        Double d1 = Double.valueOf(v1.toString());
        Double d2 = Double.valueOf(v2.toString());
        double result = div(d1, d2, scale);
        return result;
    }

    /**
     * 2020-08-06 6.4.0
     * 提供（相对）精确的除法运算。非整除时scale指定精度四舍五入。
     * @param v1 被除数
     * @param v2 除数
     * @param scale 表示表示需要精确到小数点以后几位。div(10, 3, 2) ==> 3.33
     * @return 两个参数的商
     */
    public static final double div(final Long v1, final Long v2, final Integer scale){
        Double d1 = Double.valueOf(v1.toString());
        Double d2 = Double.valueOf(v2.toString());
        double result = div(d1, d2, scale);
        return result;
    }

    /**
     * 2018-12-07 2.0.0
     * 提供（相对）精确的除法运算。非整除时scale指定精度四舍五入。
     * @param v1 被除数
     * @param v2 除数
     * @param scale 表示表示需要精确到小数点以后几位。div(10, 3, 2) ==> 3.33
     * @return 两个参数的商
     */
    public static final double div(final Double v1, final Double v2, final Integer scale) {
        if (scale < 0) {
            LogUtil.error("DoubleUtil_div_1", "The scale must be a positive integer or zero");
            return 0;
        }
        String s1 = Double.toString(v1);
        BigDecimal b1 = new BigDecimal(s1);
        String s2 = Double.toString(v2);
        BigDecimal b2 = new BigDecimal(s2);
        BigDecimal b = b1.divide(b2, scale, RoundingMode.HALF_UP);
        double result = b.doubleValue();
        return result;
    }

    /**
     * 2018-12-07 2.0.0
     * 提供精确的小数位四舍五入处理。round(3.1415926, 3) ==> 3.142
     * @param v1 需要四舍五入的数字
     * @param scale 小数点后保留几位
     * @return 四舍五入后的结果
     */
    public static final double round(final Double v1, final Integer scale) {
        if (scale < 0) {
            LogUtil.error("DoubleUtil_round_1", "The scale must be a positive integer or zero", v1, scale);
            return 0;
        }
        String s1 = Double.toString(v1);
        BigDecimal b1 = new BigDecimal(s1);
        BigDecimal one = new BigDecimal("1");
        BigDecimal b = b1.divide(one, scale, RoundingMode.HALF_UP);
        double result = b.doubleValue();
        return result;
    }

}
