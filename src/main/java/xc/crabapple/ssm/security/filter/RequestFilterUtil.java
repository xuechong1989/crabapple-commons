package xc.crabapple.ssm.security.filter;

import jakarta.servlet.http.HttpServletRequest;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.ListUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.net.ip.IPUtil;
import xc.crabapple.core.servlet.ServletUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 2025-01-19
 * 自定义请求过滤器处理工具类
 */
public final class RequestFilterUtil {

    private static String p_rootApi = "";
    private static String p_ipApiEnable = "no";
    private static List<String> p_ipApiList = null;
    private static String p_apiParamEnable = "no";
    private static String p_apiParamValue = null;
    private static String p_viewappUrlEnable = "no";
    private static List<String> p_viewappUrlList = null;

    /**
     * 2025-01-21
     */
    public static final void init(final String rootApi, final String ipApiEnable, final String ipApiValue, final String apiParamEnable, final String apiParamValue, final String viewappUrlEnable) {
        p_initRootApi(rootApi);
        p_initIpApi(ipApiEnable, ipApiValue);
        p_initApiParam(apiParamEnable, apiParamValue);
        p_initViewapp(viewappUrlEnable);
    }

    /**
     * 2025-01-21
     * @param rootApi
     */
    private static void p_initRootApi(final String rootApi){
        p_rootApi = rootApi;
    }

    /**
     * 2025-01-20
     */
    private static void p_initIpApi(final String ipApiEnable, final String ipApiValue) {
        String localIpApiEnable = ipApiEnable;
        if(!"yes".equals(localIpApiEnable)){
            localIpApiEnable = "no";
        }
        p_ipApiEnable = localIpApiEnable;
        // 客户端源头请求发起方信息
        p_ipApiList = new ArrayList<>(32);
        String localIpApiValue = StringUtil.getPureStr(ipApiValue);
        localIpApiValue = StringUtil.deleteAll(localIpApiValue, " ");
        String[] refererApiArray = StringUtil.split(localIpApiValue, ",");
        if(!CollectionUtil.isBlank(refererApiArray)){
            for(String ipApi : refererApiArray){
                p_ipApiList.add(ipApi);
            }
        }
    }

    /**
     * 2025-01-21
     */
    private static void p_initApiParam(final String apiParamEnable, final String apiParamValue){
        String localApiParamEnable = apiParamEnable;
        if(!"yes".equals(localApiParamEnable)){
            localApiParamEnable = "no";
        }
        p_apiParamEnable = localApiParamEnable;
        p_apiParamValue = apiParamValue;
    }

    /**
     * 2025-01-21
     */
    private static void p_initViewapp(final String viewappUrlEnable){
        String localViewappUrlEnable = viewappUrlEnable;
        if(!"yes".equals(localViewappUrlEnable)){
            localViewappUrlEnable = "no";
        }
        p_viewappUrlEnable = localViewappUrlEnable;
        p_viewappUrlList = ListUtil.getList(".html", ".js", ".css", ".png", ".jpg", ".gif", ".bmp", ".woff", ".tff", ".ttf", ".map", ".bcmap", ".ico", ".json", ".xml", ".txt", ".log",".svg",".woff2");
    }

    /**
     * 2025-01-21
     * @return
     */
    public static final String filter(){
        final String result = filter(null, null);
        return result;
    }

    /**
     * 2025-01-21
     * @param request
     * @return
     */
    public static final String filter(final HttpServletRequest request){
        final String result = filter(request, null);
        return result;
    }

    /**
     * 2025-01-21
     * @param clientIp
     * @return
     */
    public static final String filter(final String clientIp){
        String result = filter(null, clientIp);
        return result;
    }

    /**
     * 2025-01-15
     * 仅允许以指定前缀的接口请求
     */
    public static final String filter(final HttpServletRequest request, final String clientIp){
        boolean flag = p_filterRootApi();
        if(!flag){
            return "requestFilter_error_rootApi";
        }
        //
        flag = p_filterIpApi(request, clientIp);
        if(!flag){
            return "requestFilter_error_ipApi";
        }
        //
        flag = p_filterApiParam();
        if(!flag){
            return "requestFilter_error_apiParam";
        }
        //
        flag = p_filterViewapp();
        if(!flag){
            return "requestFilter_error_viewapp";
        }
        //
        return "success";
    }

    /**
     * 2025-01-15
     * 仅允许以指定前缀的接口请求
     */
    private static boolean p_filterRootApi() {
        // 如果不校验客户端资源url，则也不会校验统一接口名称前缀
        if(!"yes".equals(p_viewappUrlEnable)){
            return true;
        }
        //
        final String requestURI = ServletUtil.getRequestURI();
        if(StringUtil.equals(requestURI, "/")){
            return true;
        }
        if(StringUtil.startsWith(requestURI, p_rootApi)){
            return true;
        }
        return false;
    }

    /**
     * 2025-01-21
     * @return
     */
    private static boolean p_filterIpApi(){
        boolean flag = p_filterIpApi(null, null);
        return flag;
    }

    /**
     * 2025-01-21
     * @param request
     * @param clientIp
     * @return
     */
    private static boolean p_filterIpApi(final HttpServletRequest request, final String clientIp){
        if(!"yes".equals(p_ipApiEnable)){
            return true;
        }
        HttpServletRequest localRequest = request;
        if(localRequest == null){
            localRequest = ServletUtil.getRequest();
        }
        // 客户端源头请求发起方信息
        String localClientIp = clientIp;
        if(StringUtil.isBlank(localClientIp)){
            localClientIp = IPUtil.getClientIp(localRequest);
        }
        final String requestURI = ServletUtil.getRequestURI("do");
        for (String client : p_ipApiList){
            if (StringUtil.containOf(client,"#")){
                final String[] array = StringUtil.split(client, "#");
                final String ip = array[0];
                final String api = array[1];
                if(StringUtil.equalsOfOne(ip, "*", localClientIp) && StringUtil.equalsOfOne(api, "*", requestURI)){
                    return true;
                }
            }
        }
        //
        return false;
    }

    /**
     * 2025-01-21
     * @return
     */
    private static boolean p_filterApiParam(){
        if(!"yes".equals(p_apiParamEnable)){
            return true;
        }
        //
        if(StringUtil.isBlank(p_apiParamValue)){
            return false;
        }
        if (!StringUtil.containOf(p_apiParamValue,"=")){
            return false;
        }
        //
        final String[] array = StringUtil.split(p_apiParamValue, "=");
        final String configParamName = array[0];
        if("*".equals(configParamName)){
            return true;
        }
        String configParamValue = array[1];
        final String paramValue = ServletUtil.getString(configParamName);
        if(StringUtil.equals(paramValue, configParamValue)){
            return true;
        }
        //
        return false;
    }

    /**
     * 2025-01-15
     * 校验是否为客户端资源请求
     * @return
     */
    private static boolean p_filterViewapp(){
        if(!"yes".equals(p_viewappUrlEnable)){
            return true;
        }
        //
        final String requestURI = ServletUtil.getRequestURI();
        // 添加例外
        if("/favicon.ico".equals(requestURI)){
            return true;
        }
        for(String viewappUrl : p_viewappUrlList){
            if(StringUtil.lastsWith(requestURI, viewappUrl)){
                return false;
            }
        }
        return true;
    }

}
