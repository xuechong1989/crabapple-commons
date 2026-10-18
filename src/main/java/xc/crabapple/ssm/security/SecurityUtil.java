package xc.crabapple.ssm.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.CorsFilter;
import xc.crabapple.core.cipher.bouncycastle.BouncyCastleUtil;
import xc.crabapple.core.data.dto.DtoBase;
import xc.crabapple.ssm.security.crossdomain.CrossdomainUtil;
import xc.crabapple.ssm.security.csp.CspUtil;
import xc.crabapple.ssm.security.csrf.CsrfUtil;
import xc.crabapple.ssm.security.filter.RequestFilterUtil;
import xc.crabapple.ssm.security.limiter.LimiterUtil;
import xc.crabapple.ssm.security.sameorigin.SameoriginUtil;
import xc.crabapple.ssm.security.transmitc.TransmitCipherUtil;
import xc.crabapple.ssm.security.webshell.WebshellUtil;
import xc.crabapple.ssm.security.xss.XssUtil;
import xc.crabapple.ssm.spring.SsmRequestWrapper;
import xc.crabapple.ssm.uploader.UploaderUtil;

import java.util.Map;

/**
 * 2025-01-20
 * web开发系统安全工具类
 */
public final class SecurityUtil {

    /**
     * 2025-09-18
     */
    public static final void addProvider(){
        BouncyCastleUtil.addProvider();
    }

    /**
     * 2025-01-20
     * @param securityParamBean
     */
    public static final void init(final SecurityConfigBean securityParamBean){
        final String rootApi = securityParamBean.getRootApi();
        final String ipApiEnable = securityParamBean.getIpApiEnable();
        final String ipApiValue = securityParamBean.getIpApiValue();
        final String apiParamEnable = securityParamBean.getApiParamEnable();
        final String apiParamValue = securityParamBean.getApiParamValue();
        final String viewappUrlEnable = securityParamBean.getViewappUrlEnable();
        final String crossdomainValue = securityParamBean.getCrossdomainValue();
        final String sameoriginEnable = securityParamBean.getSameoriginEnable();
        final String sameOriginValue = securityParamBean.getSameOriginValue();
        final String xssEnable = securityParamBean.getXssEnable();
        final String xssApiWhitelist = securityParamBean.getXssApiWhitelist();
        final String csrfEnable = securityParamBean.getCsrfEnable();
        final Integer csrfTokenLength = securityParamBean.getCsrfTokenLength();
        final String csrfTokenSubmergeKey = securityParamBean.getCsrfTokenSubmergeKey();
        final String csrfTokenParamName = securityParamBean.getCsrfTokenParamName();
        final String csrfFirstApi = securityParamBean.getCsrfFirstApi();
        final String cspEnable = securityParamBean.getCspEnable();
        final Map<String, String> cspDefaultMap = securityParamBean.getCspDefaultMap();
        final String limiterEnable = securityParamBean.getLimiterEnable();
        final String limiterClientIdRule = securityParamBean.getLimiterClientIdRule();
        final String limiterClientIpRule = securityParamBean.getLimiterClientIpRule();
        final String uploaderWebshellEnable = securityParamBean.getUploaderWebshellEnable();
        final String uploaderWebshellParamName = securityParamBean.getUploaderWebshellParamName();
        final String tcEnbale = securityParamBean.getTcEnbale();
        final String tcC2sSubmergeKey = securityParamBean.getTcC2sSubmergeKey();
        final String tcC2sSm2publicKey = securityParamBean.getTcC2sSm2publicKey();
        final String tcC2sSm2privateKey = securityParamBean.getTcC2sSm2privateKey();
        final String tcC2sApiWhitelist = securityParamBean.getTcC2sApiWhitelist();
        final String tcS2cSubmergeKey = securityParamBean.getTcS2cSubmergeKey();
        final String tcS2cSm2publicKey = securityParamBean.getTcS2cSm2publicKey();
        final String tcS2cSm2privateKey = securityParamBean.getTcS2cSm2privateKey();
        final String tcS2cApiWhitelist = securityParamBean.getTcS2cApiWhitelist();
        //
        initRequestFilter(rootApi, ipApiEnable, ipApiValue, apiParamEnable, apiParamValue, viewappUrlEnable);
        initCrossdomain(crossdomainValue);
        initSameorigin(sameoriginEnable, sameOriginValue);
        initXss(xssEnable, xssApiWhitelist);
        initCsrf(csrfEnable, csrfTokenLength, csrfTokenSubmergeKey, csrfTokenParamName, csrfFirstApi);
        initCsp(cspEnable, cspDefaultMap);
        initLimiter(limiterEnable, limiterClientIdRule, limiterClientIpRule);
        initUploader(uploaderWebshellEnable, uploaderWebshellParamName);
        initTransmitCipher(tcEnbale, tcC2sSubmergeKey, tcC2sSm2publicKey, tcC2sSm2privateKey, tcC2sApiWhitelist, tcS2cSubmergeKey, tcS2cSm2publicKey, tcS2cSm2privateKey, tcS2cApiWhitelist);
    }

    public static final void initRequestFilter(final String rootApi, final String ipApiEnable, final String ipApiValue, final String apiParamEnable, final String apiParamValue, final String viewappUrlEnable) {
        RequestFilterUtil.init(rootApi, ipApiEnable, ipApiValue, apiParamEnable, apiParamValue, viewappUrlEnable);
    }

    public static final void initCrossdomain(final String crossdomainValue){
        CrossdomainUtil.init(crossdomainValue);
    }

    public static final void initXss(final String enable, final String xssApiWhitelist){
        XssUtil.init(enable, xssApiWhitelist);
    }

    public static final void initCsrf(final String enable, final Integer length, final String csrfTokenSubmergeKey, final String csrfTokenParamName, final String firstApi){
        CsrfUtil.init(enable, length, csrfTokenSubmergeKey, csrfTokenParamName, firstApi);
    }

    public static final void initCsp(final String cspEnable, final Map<String, String> cspDefaultMap){
        CspUtil.init(cspEnable, cspDefaultMap);
    }

    public static final void initLimiter(final String enable, final String clientIdRule, final String clientIpRule){
        LimiterUtil.init(enable, clientIdRule, clientIpRule);
    }

    public static final void initSameorigin(final String sameoriginEnable, final String originInfo){
        SameoriginUtil.init(sameoriginEnable, originInfo);
    }

    public static final void initUploader(final String uploaderWebshellEnable, final String uploaderWebshellParamName){
        UploaderUtil.init(uploaderWebshellEnable, uploaderWebshellParamName);
    }

    public static final void initTransmitCipher(final String tcEnbale, final String tcC2sSubmergeKey, final String tcC2sSm2publicKey, final String tcC2sSm2privateKey, final String tcC2sApiWhitelist, final String tcS2cSubmergeKey, final String tcS2cSm2publicKey, final String tcS2cSm2privateKey, final String tcS2cApiWhitelist){
        TransmitCipherUtil.init(tcEnbale, tcC2sSubmergeKey, tcC2sSm2publicKey, tcC2sSm2privateKey, tcC2sApiWhitelist, tcS2cSubmergeKey, tcS2cSm2publicKey, tcS2cSm2privateKey, tcS2cApiWhitelist);
    }

    public static final String filter(final HttpServletRequest request, final String clientIp){
        final String result = RequestFilterUtil.filter(request, clientIp);
        return result;
    }

    /**
     *
     * @return
     */
    public static final String sameorigin(){
        final String flag = SameoriginUtil.check(null, null);
        return flag;
    }

    public static final String sameorigin(final HttpServletRequest request, final String clientIp){
        final String flag = SameoriginUtil.check(request, clientIp);
        return flag;
    }

    /**
     * 2025-01-08
     * 在请求过滤器中使用
     * @param response
     */
    public static final void crossdomain(final HttpServletResponse response){
        CrossdomainUtil.setResponse(response);
    }

    /**
     * 2025-01-08
     * 在请求过滤器中使用
     * @param response
     * @param crossdomainValue
     */
    public static final void crossdomain(final HttpServletResponse response, final String crossdomainValue){
        CrossdomainUtil.setResponse(response, crossdomainValue);
    }

    public static final CorsFilter crossdomainCorsFilter(){
        final CorsFilter corsFilter = CrossdomainUtil.getCorsFilter();
        return corsFilter;
    }

    public static final String csrfToken(){
        final String csrfToken = CsrfUtil.getCsrfToken();
        return csrfToken;
    }

    public static final String csrfCheck(){
        final String result = CsrfUtil.check();
        return result;
    }

    public static final void cspSetResponse(final HttpServletResponse response, final Map<String, String> cspMap){
        CspUtil.setResponse(response, cspMap);
    }

    public static final String limiterCheck(){
        final String result = LimiterUtil.check();
        return result;
    }

    public static final String uploaderWebshellCheck(){
        final String result = WebshellUtil.checkFileType();
        return result;
    }

    public static final SsmRequestWrapper tcC2sDecrypt(final SsmRequestWrapper request){
        final SsmRequestWrapper decryptedRequest = TransmitCipherUtil.c2sDecrypt(request);
        return decryptedRequest;
    }

    public static final DtoBase tcS2cEncrypt(final DtoBase plaintextDtoBase){
        final DtoBase cipherDtoBase = TransmitCipherUtil.s2cEncrypt(plaintextDtoBase);
        return cipherDtoBase;
    }

}