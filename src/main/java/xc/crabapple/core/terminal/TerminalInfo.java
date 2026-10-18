package xc.crabapple.core.terminal;

/**
 * 2017-11-07 1.0.0
 */
public final class TerminalInfo {

    private String browserType;//浏览器类型
    private String browserVersion;//浏览器版本
    private String platformType;//平台类型
    private String platformSeries;//平台系列
    private String platformVersion;//平台版本

    public TerminalInfo(){}

    /**
     * 2017-11-07 1.0.0
     * @param browserType
     * @param browserVersion
     * @param platformType
     * @param platformSeries
     * @param platformVersion
     */
    public TerminalInfo(final String browserType, final String browserVersion, final String platformType, final String platformSeries, final String platformVersion){
        this.browserType = browserType;
        this.browserVersion = browserVersion;
        this.platformType = platformType;
        this.platformSeries = platformSeries;
        this.platformVersion = platformVersion;
    }

    public String getBrowserType() {
        return browserType;
    }
    public void setBrowserType(final String browserType) {
        this.browserType = browserType;
    }
    public String getBrowserVersion() {
        return browserVersion;
    }
    public void setBrowserVersion(final String browserVersion) {
        this.browserVersion = browserVersion;
    }
    public String getPlatformType() {
        return platformType;
    }
    public void setPlatformType(final String platformType) {
        this.platformType = platformType;
    }
    public String getPlatformSeries() {
        return platformSeries;
    }
    public void setPlatformSeries(final String platformSeries) {
        this.platformSeries = platformSeries;
    }
    public String getPlatformVersion() {
        return platformVersion;
    }
    public void setPlatformVersion(final String platformVersion) {
        this.platformVersion = platformVersion;
    }

}
