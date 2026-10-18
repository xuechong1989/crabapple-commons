package xc.crabapple.core.cookie;

/**
 * 2023-08-15
 * cookie配置类
 */
public final class CookieBean {

    private String name = null;
    private String value = "";
    private String domain = "";
    private String path = "/";
    private String comment = "";
    private Long maxAge = 3153600000L;  // 过期时间，单位秒， (3600*24*365)*100 = 31536000秒/年*100年 = 3153600000秒 = 100年
    private Integer version = 0;
    private Boolean httpOnly = true;
    private Boolean secure = false;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(final String domain) {
        this.domain = domain;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public Long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(final Long maxAge) {
        this.maxAge = maxAge;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public Boolean getHttpOnly() {
        return httpOnly;
    }

    public void setHttpOnly(final Boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    public Boolean getSecure() {
        return secure;
    }

    public void setSecure(final Boolean secure) {
        this.secure = secure;
    }

}