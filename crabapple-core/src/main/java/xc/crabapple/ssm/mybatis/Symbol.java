package xc.crabapple.ssm.mybatis;

/**
 * 符号枚举
 * 2023-08-15
 */
public enum Symbol {
    EQ("="),
    GT(">"),
    LT("<"),
    GTE(">="),
    LTE("<="),
    LIKE("like");


    private String type;

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    Symbol(final String type) {
        this.type = type;
    }
}
