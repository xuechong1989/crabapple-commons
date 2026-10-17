package xc.crabapple.core.log;

/**
 * 2022-08-05
 */
public final class LogConfigBean {

    // 日志框架类型，可选值 logback / log4j2
    private String type = "logback";
    // 日志等级，可选值 debug / info / warn / error
    private String level = "warn";
    // 日志文件所在目录的绝对路径
    private String path = "/data/crabapple/logs";
    // 日志文件保存天数，单位天
    private Integer maxHistory = 36500;
    // 日志文件，单个文件大小上限，需要加上单位（KB/MB/GB）
    private String maxFileSize = "1024MB";

    public LogConfigBean(){

    }

    @Override
    public String toString() {
        return "LogConfigBean{" +
                "type='" + type + '\'' +
                ", level='" + level + '\'' +
                ", path='" + path + '\'' +
                ", maxHistory=" + maxHistory +
                ", maxFileSize='" + maxFileSize + '\'' +
                '}';
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(final String level) {
        this.level = level;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public Integer getMaxHistory() {
        return maxHistory;
    }

    public void setMaxHistory(final Integer maxHistory) {
        this.maxHistory = maxHistory;
    }

    public String getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(final String maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

}