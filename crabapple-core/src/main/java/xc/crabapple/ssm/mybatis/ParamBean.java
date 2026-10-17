package xc.crabapple.ssm.mybatis;

/**
 * 组装sql参数bean
 * 2023-08-15
 */
public final class ParamBean {

    private String paramKey;
    private Symbol symbol;
    private String paramValue;

    public String getParamKey() {
        return paramKey;
    }

    public void setParamKey(final String paramKey) {
        this.paramKey = paramKey;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public void setSymbol(final Symbol symbol) {
        this.symbol = symbol;
    }

    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(final String paramValue) {
        this.paramValue = paramValue;
    }
}
