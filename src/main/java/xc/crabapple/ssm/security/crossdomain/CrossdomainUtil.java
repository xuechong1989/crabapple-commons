package xc.crabapple.ssm.security.crossdomain;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import xc.crabapple.core.servlet.ServletUtil;

/**
 * 2025-01-08
 * 跨域统一处理工具类
 * 1. 防止跨站请求伪造攻击CSRF
 */
public final class CrossdomainUtil {

    private static String p_crossdomainValue = "*";

    public static final void init(){
        init("*");
    }

    public static final void init(final String crossdomainValue){
        p_crossdomainValue = crossdomainValue;
    }

    /**
     * 2025-01-21
     * 在请求过滤器中使用
     */
    public static final void setResponse(){
        HttpServletRequest request = ServletUtil.getRequest();
        HttpServletResponse response = ServletUtil.getResponse();
        ServletUtil.setCrossdomain(request, response, p_crossdomainValue);
    }

    /**
     * 2025-01-21
     * 在请求过滤器中使用
     * @param crossdomainValue
     */
    public static final void setResponse(final String crossdomainValue){
        HttpServletRequest request = ServletUtil.getRequest();
        HttpServletResponse response = ServletUtil.getResponse();
        ServletUtil.setCrossdomain(request, response, crossdomainValue);
    }

    /**
     * 2025-01-21
     * 在请求过滤器中使用
     * @param response
     */
    public static final void setResponse(final HttpServletResponse response){
        HttpServletRequest request = ServletUtil.getRequest();
        ServletUtil.setCrossdomain(request, response, p_crossdomainValue);
    }

    /**
     * 2025-01-08
     * 在请求过滤器中使用
     * @param response
     * @param crossdomainValue
     */
    public static final void setResponse(final HttpServletResponse response, final String crossdomainValue){
        HttpServletRequest request = ServletUtil.getRequest();
        ServletUtil.setCrossdomain(request, response, crossdomainValue);
    }

    /**
     * 2025-08-06
     * 在请求过滤器中使用
     * @param response
     * @param crossdomainValue
     */
    public static final void setResponse(final HttpServletRequest request, final HttpServletResponse response, final String crossdomainValue){
        ServletUtil.setCrossdomain(request, response, crossdomainValue);
    }

    /**
     * 2025-01-08
     */
    public static final CorsFilter getCorsFilter(){
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedHeader(p_crossdomainValue);
        corsConfiguration.addAllowedMethod(p_crossdomainValue);
        corsConfiguration.addAllowedOriginPattern(p_crossdomainValue);
        corsConfiguration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
        CorsFilter corsFilter = new CorsFilter(urlBasedCorsConfigurationSource);
        return corsFilter;
    }

}
