package xc.crabapple.core.file.pdf.wkhtmltopdf;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.url.UrlUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 2022-12-22
 * wk配置类
 */
public final class WkHtmlToPdfConfigBean {

    // windows格式如：
    // C:/Program Files/wkhtmltopdf/bin/wkhtmltopdf.exe
    // linux格式如：/opt/wkhtmltopdf/bin/wkhtmltopdf
    private String homeUrl;
    // 模板HTML文件路径
    private String templateHtmlUrl;
    // 替换通配符
    private String htmlReplace;
    // 源HTML文件路径
    private String htmlUrl;
    // 源HTML代码片段
    private String htmlText;
    // 转换为PDF之前的最终html文件的路径，若不配置则不保存，若设置为yes则使用内置默认路径
    private String cacheHtmlUrl = "yes";
    // 目标pdf文件绝对路径
    private String pdfUrl;
    // 2026-07-17 WkHtmlToPdfUtil优化
    // wkhtmltopdf执行超时时间（秒），默认300秒
    private int timeoutSeconds = 300;
    //
    private Map<String, String> paramMap;

    public WkHtmlToPdfConfigBean(){
        paramMap = new LinkedHashMap<>();
    }

    public void put(final String key, final String value){
        paramMap.put(key, value);
    }

    public Map<String, String> getParamMap() {
        return paramMap;
    }

    public String getTemplateHtmlUrl() {
        return templateHtmlUrl;
    }

    public void setTemplateHtmlUrl(final String templateHtmlUrl) {
        // 2026-08-10 空值保持null，避免路径标准化将null变成根目录
        String templateHtmlUrlLocal = StringUtil.trim(templateHtmlUrl);
        if (StringUtil.isBlank(templateHtmlUrlLocal)) {
            this.templateHtmlUrl = null;
            return;
        }
        templateHtmlUrlLocal = UrlUtil.getStandardPath(templateHtmlUrlLocal);
        this.templateHtmlUrl = templateHtmlUrlLocal;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(final String htmlUrl) {
        // 2026-08-10 空值保持null，避免路径标准化将null变成根目录
        String htmlUrlLocal = StringUtil.trim(htmlUrl);
        if (StringUtil.isBlank(htmlUrlLocal)) {
            this.htmlUrl = null;
            return;
        }
        htmlUrlLocal = UrlUtil.getStandardPath(htmlUrlLocal);
        this.htmlUrl = htmlUrlLocal;
    }

    public String getCacheHtmlUrl() {
        return cacheHtmlUrl;
    }

    public void setCacheHtmlUrl(final String cacheHtmlUrl) {
        this.cacheHtmlUrl = cacheHtmlUrl;
    }

    public String getHomeUrl() {
        return homeUrl;
    }

    public void setHomeUrl(final String homeUrl) {
        // 2026-08-10 空值保持null，避免路径标准化将null变成根目录
        String homeUrlLocal = StringUtil.trim(homeUrl);
        if (StringUtil.isBlank(homeUrlLocal)) {
            this.homeUrl = null;
            return;
        }
        homeUrlLocal = UrlUtil.getStandardPath(homeUrlLocal);
        this.homeUrl = homeUrlLocal;
    }

    public String getHtmlText() {
        return htmlText;
    }

    public void setHtmlText(final String htmlText) {
        this.htmlText = htmlText;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(final String pdfUrl) {
        // 2026-08-10 空值保持null，避免路径标准化将null变成根目录
        String pdfUrlLocal = StringUtil.trim(pdfUrl);
        if (StringUtil.isBlank(pdfUrlLocal)) {
            this.pdfUrl = null;
            return;
        }
        pdfUrlLocal = UrlUtil.getStandardPath(pdfUrlLocal);
        this.pdfUrl = pdfUrlLocal;
    }

    public String getHtmlReplace() {
        return htmlReplace;
    }

    public void setHtmlReplace(final String htmlReplace) {
        this.htmlReplace = htmlReplace;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(final int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

}
