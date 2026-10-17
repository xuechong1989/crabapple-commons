package xc.crabapple.ssm.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.properties.PropertiesUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.net.ip.IPUtil;
import xc.crabapple.core.net.port.PortUtil;
import xc.crabapple.core.servlet.ServletUtil;
import xc.crabapple.core.url.UrlUtil;

import java.util.Locale;
import java.util.Map;

/**
 * 2022-04-13
 * 集成spring域springmvc框架
 */
public final class SpringUtil {

    public static ApplicationContext applicationContext = null;
    public static WebApplicationContext webApplicationContext = null;
    public static Class moduleManagerClass = null;
    public static String systemName = "";

    private static final String LOCALE_ATTRIBUTE_NAME = "currentLocale";
    private static final String SERVER_PORT_KEY = "server.port";
    private static final String DEFAULT_PORT = "8080";
    private static final String PORT_DELIMITER = ",";
    private static final String HTTPS_PORT = "443";
    private static final String HTTP_PROTOCOL = "http://";
    private static final String HTTPS_PROTOCOL = "https://";
    private static final String DEFAULT_API_NAME = "";

    /**
     * 2025-01-21
     */
    public static final Map<String, Object> getParamMap(){
        final Map<String, Object> paramMap = ServletUtil.getParamMap();
        return paramMap;
    }

    /**
     * 2025-01-21
     */
    public static final Map<String, Object> getParamMap(final HttpServletRequest request){
        final Map<String, Object> paramMap = ServletUtil.getParamMap(request);
        return paramMap;
    }

    /**
     * 2025-01-21
     * 获得HttpServletRequest中所有参数和对应值
     * @param request
     * @param paramaterType
     */
    public static final Map<String, Object> getParamMap(final HttpServletRequest request, final String paramaterType){
        final Map<String, Object> paramMap = ServletUtil.getParamMap(request, paramaterType);
        return paramMap;
    }

    /**
     * 2025-01-15
     * servlet标准设置
     * @param request
     * @param response
     */
    public static final void setRequestResponse(final HttpServletRequest request, final HttpServletResponse response){
        ServletUtil.setRequestResponse(request, response);
        //
        if(request != null){
            final Locale locale = RequestContextUtils.getLocale(request);
            ServletUtil.setAttribute(LOCALE_ATTRIBUTE_NAME, locale);
        }
    }

    /**
     * 2019-11-06 1.0.0
     * 获取请求报文，每次有实际请求时request对象才存在，不能在多线程下使用
     */
    public static final HttpServletRequest getRequest(){
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(servletRequestAttributes == null){
            servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        }
        if(servletRequestAttributes == null){
            return null;
        }
        final HttpServletRequest request = servletRequestAttributes.getRequest();
        return request;
    }

    /**
     * 2022-05-31 1.0.0
     * 获取请求报文，每次有实际请求时request对象才存在，不能在多线程下使用
     */
    public static final HttpServletResponse getResponse(){
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(servletRequestAttributes == null){
            servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        }
        if(servletRequestAttributes == null){
            return null;
        }
        final HttpServletResponse response = servletRequestAttributes.getResponse();
        return response;
    }

    /**
     * 2025-04-22
     * 系统启动成功提示语
     */
    public static final void banner(){
        banner(DEFAULT_API_NAME);
    }

    /**
     * 2025-02-11
     * 系统启动成功提示语
     */
    public static final void banner(final String apiName){
        LogUtil.print("SpringUtil_banner_1", "欢迎使用【"+systemName+"】，系统启动成功！");
        final String serverIp = IPUtil.getServerIp();
        String ports = PortUtil.getPort();
        if(StringUtil.isBlank(ports)){
            ports = PropertiesUtil.getValue(SERVER_PORT_KEY, DEFAULT_PORT);
        }
        final String[] portArray = StringUtil.split(ports, PORT_DELIMITER);
        final String standardApiName = UrlUtil.getStandardApi(apiName);
        if(!CollectionUtil.isBlank(portArray)){
            for(int i = 0; i < portArray.length; i++){
                final String port = portArray[i];
                String info = HTTP_PROTOCOL + serverIp + ":" + port + standardApiName;
                if(StringUtil.containOf(port, HTTPS_PORT)){
                    info = HTTPS_PROTOCOL + serverIp + ":" + port + standardApiName;
                }
                LogUtil.print("SpringUtil_banner_2", "访问地址："+info+"\n");
            }
        }
    }

}