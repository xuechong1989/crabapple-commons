package xc.crabapple.core.math;

/**
 * 2024-02-29
 * 银行贷款、月供计算工具类
 * 贷款核算公式：
 * P - 本金
 * r - 月利率，年利率/12
 * R - 年利，如3.2%，即0.032，r * 12
 * n - 期数，即月份，一年即12
 * M - 月供
 * N - 还款总额
 * L - 总利息
 *
 * # 年利率
 * R = r * 12
 *
 * # 月供额
 * M = P * (r / (1 - (1+r)^(-n)))
 * M = P * (R / 12 / (1 - (1+R/12)^(-n)))
 * M = 30000 * (0.032 / 12 / (1 - (1+0.032/12)^(-12)))
 *
 * # 还款额
 * N = M * n
 *
 * # 总利息
 * L = N - P
 */
public final class LoanUtil {

    /**
     * 2024-02-29
     * 计算月供（等额本息）
     * 调用示例：
     * double result = yuegong(30000, 12, 0.032D);
     * System.out.println(result);
     * @param P 本金
     * @param n 期数
     * @param R 年利
     */
    public static final double yuegong(final int P, final int n, final double R){
        double yuegong = P * (R/12 / (1 - Math.pow(1+(R/12), -n)));
        return yuegong;
    }

}
