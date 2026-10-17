package xc.crabapple.core.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.UriEncoderUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.servlet.ServletUtil;

/**
 * 2016-12-07 1.0.0
 * 2023-08-15 引入bean参数对象，整体更新
 * Cookie工具类
 */
public final class CookieUtil{

    /**
     * 2023-08-15
     * @param name cookie名称
     * @param value cookie值
     */
    public static final void add(final String name, final String value){
        HttpServletResponse response = ServletUtil.getResponse();
        CookieBean cookieBean = new CookieBean();
        cookieBean.setName(name);
        cookieBean.setValue(value);
        add(response, cookieBean);
    }

    /**
     * 2023-08-15
     * @param name cookie名称
     * @param value cookie值
     */
    public static final void add(final String name, final String value, final Boolean httpOnly){
        HttpServletResponse response = ServletUtil.getResponse();
        CookieBean cookieBean = new CookieBean();
        cookieBean.setName(name);
        cookieBean.setValue(value);
        cookieBean.setHttpOnly(httpOnly);
        add(response, cookieBean);
    }

    /**
     * 2016-12-07 1.0.0
     * 添加cookie
     * @param name cookie名称
     * @param value cookie值
     */
    public static final void add(final HttpServletResponse response, final String name, final String value){
        CookieBean cookieBean = new CookieBean();
        cookieBean.setName(name);
        cookieBean.setValue(value);
        add(response, cookieBean);
    }

    /**
     * 2023-08-15
     * 添加cookie
     * @param name cookie名称
     * @param value cookie值
     * @param httpOnly
     */
    public static final void add(final HttpServletResponse response, final String name, final String value, final Boolean httpOnly){
        CookieBean cookieBean = new CookieBean();
        cookieBean.setName(name);
        cookieBean.setValue(value);
        cookieBean.setHttpOnly(httpOnly);
        add(response, cookieBean);
    }

    public static final void add(final CookieBean cookieBean){
        HttpServletResponse response = ServletUtil.getResponse();
        add(response, cookieBean);
    }

    /**
     * 2016-12-07 1.0.0
     * 2023-08-15 引入bean参数对象
     * 添加cookie
     * @param response HttpServletResponse
     * @param cookieBean cookie配置对象
     */
    public static final void add(final HttpServletResponse response, final CookieBean cookieBean){
        if(response == null){
            return;
        }
        if(cookieBean == null){
            return;
        }
        //
        String name = cookieBean.getName();
        String value = cookieBean.getValue();
        String domain = cookieBean.getDomain();
        String path = cookieBean.getPath();
        String comment = cookieBean.getComment();
        Long maxAge = cookieBean.getMaxAge();
        Integer version = cookieBean.getVersion();
        Boolean httpOnly = cookieBean.getHttpOnly();
        Boolean secure = cookieBean.getSecure();
        //
        if(StringUtil.isBlank(name)){
            return;
        }
        try{
            name = UriEncoderUtil.encodeURIComponent(name);
            value = UriEncoderUtil.encodeURIComponent(value);
            //
            Cookie cookie = new Cookie(name, value);
            cookie.setDomain(domain);
            cookie.setPath(path);
            // cookie.setComment(comment);
            int maxAgeInt = maxAge.intValue();
            cookie.setMaxAge(maxAgeInt);
            // cookie.setVersion(version);
            cookie.setHttpOnly(httpOnly);
            cookie.setSecure(secure);
            response.addCookie(cookie);
        }catch(Throwable throwable){
            LogUtil.error("CookieUtil_add_t1", throwable, name, value, domain, path, maxAge, version, httpOnly, secure);
        }
    }

    /**
     * 2023-08-15
     * @param name cookie名称
     */
    public static final String get(final String name){
        final HttpServletRequest request = ServletUtil.getRequest();
        final String value = get(request, name);
        return value;
    }

    /**
     * 2016-12-07 1.0.0
     * 从浏览器自动提交的数据中获取cookie
     * @param name cookie名称
     * @return 若不存在则返回null
     */
    public static final String get(final HttpServletRequest request, final String name){
        if(request == null){
            return null;
        }
        if(StringUtil.isBlank(name)){
            return null;
        }
        //
        Cookie[] cookieArray = request.getCookies();
        if(CollectionUtil.isBlank(cookieArray)){
            return null;
        }
        //
        try{
            String encodedName = UriEncoderUtil.encodeURIComponent(name);
            for(Cookie cookie : cookieArray){
                if(cookie == null){
                    continue;
                }
                final String cookieName = cookie.getName();
                if(!encodedName.equals(cookieName)){
                    continue;
                }
                final String cookieValue = cookie.getValue();
                final String value = UriEncoderUtil.decodeURIComponent(cookieValue, "UTF-8");
                return value;
            }
            return null;
        }catch(Throwable throwable){
            LogUtil.error("CookieUtil_get_t1", throwable, name);
            return null;
        }
    }

    /**
     * 2023-09-02
     * 获取所有客户端传来的cookie
     * @return
     */
    public static final Map<String, List<String>> getMap(){
        HttpServletRequest request = ServletUtil.getRequest();
        Map<String, List<String>> map = getMap(request);
        return map;
    }

    /**
     * 2023-09-02
     * 获取所有客户端传来的cookie
     * @param request
     * @return
     */
    public static final Map<String, List<String>> getMap(final HttpServletRequest request){
        if(request == null){
            return null;
        }
        //
        Cookie[] cookieArray = request.getCookies();
        if(CollectionUtil.isBlank(cookieArray)){
            return null;
        }
        //
        Map<String, List<String>> cookieMap = new HashMap<>();
        List<String> vvvList = new ArrayList<>();
        try{
            for(Cookie cookie : cookieArray){
                if(cookie == null){
                    continue;
                }
                final String cookieName = cookie.getName();
                final String cookieValue = cookie.getValue();
                final String value = UriEncoderUtil.decodeURIComponent(cookieValue, "UTF-8");
                final String domain = cookie.getDomain();
                final String path = cookie.getPath();
                vvvList.add(value);
                vvvList.add(domain);
                vvvList.add(path);
                cookieMap.put(cookieName, vvvList);
            }
            return cookieMap;
        }catch(Throwable throwable){
            LogUtil.error("CookieUtil_getMap_e1", throwable);
            return null;
        }
    }

    /**
     * 2023-08-15
     * 移除cookie
     * @param name cookie名称
     */
    public static final void remove(final String name){
        final HttpServletRequest request = ServletUtil.getRequest();
        final HttpServletResponse response = ServletUtil.getResponse();
        final CookieBean cookieBean = new CookieBean();
        cookieBean.setName(name);
        remove(request, response, cookieBean);
    }

    /**
     * 2016-12-07 1.0.0
     * 移除cookie
     * @param name cookie名称
     */
    public static final void remove(final HttpServletRequest request, final HttpServletResponse response, final String name){
        final CookieBean cookieBean = new CookieBean();
        cookieBean.setName(name);
        remove(request, response, cookieBean);
    }

    /**
     * 2016-12-07 1.0.0
     * 移除cookie
     * @param cookieBean cookie配置对象
     */
    public static final void remove(final HttpServletRequest request, final HttpServletResponse response, final CookieBean cookieBean){
        if(request == null){
            return;
        }
        if(response == null){
            return;
        }
        String name = cookieBean.getName();
        if(StringUtil.isBlank(name)){
            return;
        }
        String path = cookieBean.getPath();
        String domain = cookieBean.getDomain();
        try{
            name = UriEncoderUtil.encodeURIComponent(name);
            Cookie cookie = new Cookie(name, null);
            cookie.setMaxAge(0);
            cookie.setDomain(domain);
            cookie.setPath(path);
            response.addCookie(cookie);
        }catch(Throwable throwable){
            LogUtil.error("CookieUtil_remove_t1", throwable, name, domain, path);
        }
    }

}