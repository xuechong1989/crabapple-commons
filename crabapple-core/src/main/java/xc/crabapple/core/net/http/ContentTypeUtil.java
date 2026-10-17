package xc.crabapple.core.net.http;

import jakarta.servlet.http.HttpServletRequest;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.servlet.ServletUtil;

/**
 * 2024-08-06
 * http协议中Content-Type字段管理工具类
 */
public final class ContentTypeUtil {

    // 常见的取值
    public static final String application_json = "application/json;charset=utf-8";
    public static final String application_x_www_form_urlencoded = "application/x-www-form-urlencoded;charset=utf-8";
    public static final String text_plain = "text/plain;charset=utf-8";
    public static final String multipart_form_data = "multipart/form-data";
    public static final String application_octet_stream = "application/octet-stream;charset=utf-8";

    /**
     * 2025-01-22
     * @return
     */
    public static final String getContentType(){
        final HttpServletRequest request = ServletUtil.getRequest();
        final String contentType = getContentType(request);
        return contentType;
    }

    /**
     * 2024-08-06
     * 返回标准纯小写的ContentType值
     * @param request
     * @return
     */
    public static final String getContentType(final HttpServletRequest request){
        HttpServletRequest requestLocal = request;
        if(requestLocal == null){
            requestLocal = ServletUtil.getRequest();
        }
        String contentType = requestLocal.getContentType();
        contentType = getContentType(contentType);
        return contentType;
    }

    /**
     * 2024-08-06
     * 返回标准纯小写的ContentType值
     * @param contentType
     * @return
     */
    public static final String getContentType(final String contentType){
        if(StringUtil.isBlank(contentType)){
            return "";
        }
        String contentTypeLocal = StringUtil.toLowerCase(contentType);
        return contentTypeLocal;
    }

}