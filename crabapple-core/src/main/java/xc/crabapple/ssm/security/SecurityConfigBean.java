package xc.crabapple.ssm.security;

import xc.crabapple.core.data.bean.BeanBase;

import java.util.Map;

/**
 * 2025-01-21
 */
public class SecurityConfigBean extends BeanBase {

    private String rootApi = "/api/";
    private String ipApiEnable = "no";
    private String ipApiValue = "*#*";
    private String apiParamEnable = "no";
    private String apiParamValue = "";
    private String viewappUrlEnable = "no";
    private String sameoriginEnable = "no";
    private String sameOriginValue = "";
    private String xssEnable = "no";
    private String xssApiWhitelist = "";
    private String csrfEnable = "no";
    private Integer csrfTokenLength = 128;
    private String csrfTokenSubmergeKey = null;
    private String csrfTokenParamName = "csrf_token";
    private String csrfFirstApi = null;
    private String cspEnable = "no";
    private Map<String, String> cspDefaultMap = null;
    private String limiterEnable = "no";
    private String limiterClientIdRule = "";
    private String limiterClientIpRule = "";
    private String crossdomainValue = "";
    private String uploaderWebshellEnable = "no";
    private String uploaderWebshellParamName = "targetScope";
    private String tcEnbale = "yes";
    private String tcC2sSubmergeKey = "REDACTED";
    private String tcC2sSm2publicKey = "049117323b8749a9c205848c091406cdd2f435397ddc20af4db73bbd94d87db7d066fdb03b1fb0ad658951f0abcc765a1d5a3ff2aa5798d21e6e2ff42b28e090c1";
    private String tcC2sSm2privateKey = "REDACTED";
    private String tcC2sApiWhitelist = "";
    private String tcS2cSubmergeKey = "REDACTED";
    private String tcS2cSm2publicKey = "04600a928e8ea65baf432efcfa665480396f15f043730432b3a13f65de22ce187b24cd952582ddbe84e971be645035919fe84b7de217d60c442bba34e1de984498";
    private String tcS2cSm2privateKey = "REDACTED";
    private String tcS2cApiWhitelist = "";

    public SecurityConfigBean(){ }

    public String getRootApi() { return rootApi; }
    public void setRootApi(final String rootApi) { this.rootApi = rootApi; }
    public String getIpApiEnable() { return ipApiEnable; }
    public void setIpApiEnable(final String ipApiEnable) { this.ipApiEnable = ipApiEnable; }
    public String getIpApiValue() { return ipApiValue; }
    public void setIpApiValue(final String ipApiValue) { this.ipApiValue = ipApiValue; }
    public String getApiParamEnable() { return apiParamEnable; }
    public void setApiParamEnable(final String apiParamEnable) { this.apiParamEnable = apiParamEnable; }
    public String getApiParamValue() { return apiParamValue; }
    public void setApiParamValue(final String apiParamValue) { this.apiParamValue = apiParamValue; }
    public String getViewappUrlEnable() { return viewappUrlEnable; }
    public void setViewappUrlEnable(final String viewappUrlEnable) { this.viewappUrlEnable = viewappUrlEnable; }
    public String getSameoriginEnable() { return sameoriginEnable; }
    public void setSameoriginEnable(final String sameoriginEnable) { this.sameoriginEnable = sameoriginEnable; }
    public String getSameOriginValue() { return sameOriginValue; }
    public void setSameOriginValue(final String sameOriginValue) { this.sameOriginValue = sameOriginValue; }
    public String getXssEnable() { return xssEnable; }
    public void setXssEnable(final String xssEnable) { this.xssEnable = xssEnable; }
    public String getXssApiWhitelist() { return xssApiWhitelist; }
    public void setXssApiWhitelist(final String xssApiWhitelist) { this.xssApiWhitelist = xssApiWhitelist; }
    public String getCsrfEnable() { return csrfEnable; }
    public void setCsrfEnable(final String csrfEnable) { this.csrfEnable = csrfEnable; }
    public Integer getCsrfTokenLength() { return csrfTokenLength; }
    public void setCsrfTokenLength(final Integer csrfTokenLength) { this.csrfTokenLength = csrfTokenLength; }
    public String getCsrfTokenSubmergeKey() { return csrfTokenSubmergeKey; }
    public void setCsrfTokenSubmergeKey(final String csrfTokenSubmergeKey) { this.csrfTokenSubmergeKey = csrfTokenSubmergeKey; }
    public String getCsrfTokenParamName() { return csrfTokenParamName; }
    public void setCsrfTokenParamName(final String csrfTokenParamName) { this.csrfTokenParamName = csrfTokenParamName; }
    public String getCsrfFirstApi() { return csrfFirstApi; }
    public void setCsrfFirstApi(final String csrfFirstApi) { this.csrfFirstApi = csrfFirstApi; }
    public String getCspEnable() { return cspEnable; }
    public void setCspEnable(final String cspEnable) { this.cspEnable = cspEnable; }
    public Map<String, String> getCspDefaultMap() { return cspDefaultMap; }
    public void setCspDefaultMap(final Map<String, String> cspDefaultMap) { this.cspDefaultMap = cspDefaultMap; }
    public String getLimiterEnable() { return limiterEnable; }
    public void setLimiterEnable(final String limiterEnable) { this.limiterEnable = limiterEnable; }
    public String getLimiterClientIdRule() { return limiterClientIdRule; }
    public void setLimiterClientIdRule(final String limiterClientIdRule) { this.limiterClientIdRule = limiterClientIdRule; }
    public String getLimiterClientIpRule() { return limiterClientIpRule; }
    public void setLimiterClientIpRule(final String limiterClientIpRule) { this.limiterClientIpRule = limiterClientIpRule; }
    public String getCrossdomainValue() { return crossdomainValue; }
    public void setCrossdomainValue(final String crossdomainValue) { this.crossdomainValue = crossdomainValue; }
    public String getUploaderWebshellEnable() { return uploaderWebshellEnable; }
    public void setUploaderWebshellEnable(final String uploaderWebshellEnable) { this.uploaderWebshellEnable = uploaderWebshellEnable; }
    public String getUploaderWebshellParamName() { return uploaderWebshellParamName; }
    public void setUploaderWebshellParamName(final String uploaderWebshellParamName) { this.uploaderWebshellParamName = uploaderWebshellParamName; }
    public String getTcEnbale() { return tcEnbale; }
    public void setTcEnbale(final String tcEnbale) { this.tcEnbale = tcEnbale; }
    public String getTcC2sSubmergeKey() { return tcC2sSubmergeKey; }
    public void setTcC2sSubmergeKey(final String tcC2sSubmergeKey) { this.tcC2sSubmergeKey = tcC2sSubmergeKey; }
    public String getTcC2sSm2publicKey() { return tcC2sSm2publicKey; }
    public void setTcC2sSm2publicKey(final String tcC2sSm2publicKey) { this.tcC2sSm2publicKey = tcC2sSm2publicKey; }
    public String getTcC2sSm2privateKey() { return tcC2sSm2privateKey; }
    public void setTcC2sSm2privateKey(final String tcC2sSm2privateKey) { this.tcC2sSm2privateKey = tcC2sSm2privateKey; }
    public String getTcS2cSubmergeKey() { return tcS2cSubmergeKey; }
    public void setTcS2cSubmergeKey(final String tcS2cSubmergeKey) { this.tcS2cSubmergeKey = tcS2cSubmergeKey; }
    public String getTcS2cSm2publicKey() { return tcS2cSm2publicKey; }
    public void setTcS2cSm2publicKey(final String tcS2cSm2publicKey) { this.tcS2cSm2publicKey = tcS2cSm2publicKey; }
    public String getTcS2cSm2privateKey() { return tcS2cSm2privateKey; }
    public void setTcS2cSm2privateKey(final String tcS2cSm2privateKey) { this.tcS2cSm2privateKey = tcS2cSm2privateKey; }
    public String getTcC2sApiWhitelist() { return tcC2sApiWhitelist; }
    public void setTcC2sApiWhitelist(final String tcC2sApiWhitelist) { this.tcC2sApiWhitelist = tcC2sApiWhitelist; }
    public String getTcS2cApiWhitelist() { return tcS2cApiWhitelist; }
    public void setTcS2cApiWhitelist(final String tcS2cApiWhitelist) { this.tcS2cApiWhitelist = tcS2cApiWhitelist; }

}
