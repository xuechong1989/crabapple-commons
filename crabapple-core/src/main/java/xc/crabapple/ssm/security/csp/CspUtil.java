package xc.crabapple.ssm.security.csp;

import jakarta.servlet.http.HttpServletResponse;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.servlet.ServletUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 2025-01-26
 * 内容安全策略CSP（Content Security Policy）是一种HTTP响应头，可大大减少现代浏览器中的代码注入攻击，如XSS、点击劫持等。
 * Web服务器通过Content-Security-Policy Header部指定了浏览器可以渲染的资源的列表。这些资源可以是浏览器渲染的任何内容，如CSS、JavaScript、图像等。
 * 服务器设置代码示例：
 *     response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src *;");
 * 也可以在客户端页面设置meta标签，设置CSP：
 *     <meta http-equiv="Content-Security-Policy" content="default-src 'self'; img-src 'self' cdn.example.com ;">
 */
public class CspUtil {

    private static String p_enable = "no";
    private static Map<String, String> p_defaultCspMap = null;

    /**
     * 2025-01-26
     * @param enable 是否启用csp功能，yes=启用，no=不启用
     * @param defaultCspMap 默认值，一般为空即可
     */
    public static final void init(final String enable, final Map<String, String> defaultCspMap) {
        if(!"yes".equals(enable)){
            p_enable = "no";
            return;
        }
        p_enable = enable;
        //
        if(!CollectionUtil.isBlank(defaultCspMap)){
            p_defaultCspMap = defaultCspMap;
        }else{
            p_defaultCspMap = new HashMap<>(16);
            p_defaultCspMap.put("default-src", "'self'");
            p_defaultCspMap.put("script-src", "'self' 'unsafe-inline'");
            p_defaultCspMap.put("style-src", "'self' 'unsafe-inline'");
            p_defaultCspMap.put("img-src", "*");
            p_defaultCspMap.put("font-src", "'self'");
            p_defaultCspMap.put("object-src", "'none'");
            p_defaultCspMap.put("frame-src", "'none'");
            p_defaultCspMap.put("connect-src", "'self'");
            p_defaultCspMap.put("media-src", "'self'");
            p_defaultCspMap.put("base-uri", "'self'");
        }
    }

    /**
     * 2025-01-26
     */
    public static final void setResponse(){
        final HttpServletResponse response = ServletUtil.getResponse();
        setResponse(response, null);
    }

    /**
     * 2025-01-26
     * @param response
     */
    public static final void setResponse(final HttpServletResponse response){
        setResponse(response, null);
    }

    /**
     * 2025-01-26
     * @param cspMap
     */
    public static final void setResponse(final Map<String, String> cspMap){
        final HttpServletResponse response = ServletUtil.getResponse();
        setResponse(response, cspMap);
    }

    /**
     * 2025-01-26
     * @param response
     * @param cspMap
     */
    public static final void setResponse(final HttpServletResponse response, final Map<String, String> cspMap){
        if(!"yes".equals(p_enable)){
            return;
        }
        Map<String, String> localCspMap = cspMap;
        if(localCspMap == null){
            localCspMap = new HashMap<>(16);
        }
        final Set<String> defaultCspSet = p_defaultCspMap.keySet();
        final Iterator<String> defaultCspIterator = defaultCspSet.iterator();
        while(defaultCspIterator.hasNext()){
            final String key = defaultCspIterator.next();
            if(!localCspMap.containsKey(key)){
                final String value = p_defaultCspMap.get(key);
                localCspMap.put(key, value);
            }
        }
        //
        final Set<String> cspSet = localCspMap.keySet();
        final Iterator<String> cspIterator = cspSet.iterator();
        final StringBuilder stringBuilder = new StringBuilder();
        while(cspIterator.hasNext()){
            String key = cspIterator.next();
            key = StringUtil.trim(key);
            String value = localCspMap.get(key);
            value = StringUtil.trim(value);
            final String kv = key + " " + value + ";";
            if(stringBuilder.length() <= 0){
                stringBuilder.append(kv);
                continue;
            }
            stringBuilder.append(" ").append(kv);
        }
        final String result = stringBuilder.toString();
        response.setHeader("Content-Security-Policy", result);
    }

}
