package xc.crabapple.ssm.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.net.ip.IPUtil;
import xc.crabapple.ssm.security.SecurityUtil;

/**
 * 2025-01-22
 * 管理多种类型的Request对象
 */
public class SsmRequestUtil {

    private static boolean p_inited = false;

    public static final void init(){
        if(p_inited){
            return;
        }
        p_inited = true;
        //
    }

    public static final SsmRequestWrapper getMyHttpServletRequestWrapper(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse){
        SsmRequestWrapper request = new SsmRequestWrapper(httpServletRequest, httpServletResponse);
        // 解密客户端传过来的参数
        request = SecurityUtil.tcC2sDecrypt(request);
        final String requestStatus = request.getStatus();
        if(StringUtil.startsWith(requestStatus, "transmitcipher_error")){
            return request;
        }
        // 限流机制
        final String limitedResult = SecurityUtil.limiterCheck();
        if(!"success".equals(limitedResult)){
            request.setStatus(limitedResult);
            return request;
        }
        // webshell检测
        final String webshellResult = SecurityUtil.uploaderWebshellCheck();
        if(!"success".equals(webshellResult)){
            request.setStatus(webshellResult);
            return request;
        }
        // clientIp与url检测，校验客户端IP地址（使用MyFilterUtil类中的方法可配置）
        final String clientIp = IPUtil.getClientIp(request);
        final String filterResult = SecurityUtil.filter(request, clientIp);
        if(!"success".equals(filterResult)){
            request.setStatus(filterResult);
            return request;
        }
        // 跨域设置
        SecurityUtil.crossdomain(httpServletResponse);
        // 同源检测
        final String sameoriginResult = SecurityUtil.sameorigin();
        if(!"success".equals(sameoriginResult)){
            request.setStatus(sameoriginResult);
            return request;
        }
        // csrf检测
        final String csrfResult = SecurityUtil.csrfCheck();
        if(!"success".equals(csrfResult)){
            request.setStatus(csrfResult);
            return request;
        }
        //
        request.setStatus("success");
        return request;
    }

}
