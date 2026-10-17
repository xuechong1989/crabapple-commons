package xc.crabapple.core.servlet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.compress.utils.IOUtils;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.data.CrabappleInnerData;
import xc.crabapple.core.encoder.UriEncoderUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.json.JsonUtil;
import xc.crabapple.core.json.jackson.JacksonUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.net.http.ContentTypeUtil;
import xc.crabapple.core.net.http.MimeTypeUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.regex.RegexUtil;
import xc.crabapple.core.terminal.TerminalUtil;
import xc.crabapple.core.timer.TimerUtil;
import xc.crabapple.core.tomcat.TomcatUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 2019-11-06 1.0.0
 * 转发与重定向的区别
 * forward-转发（跳转） .......... redirect-重定向
 * 在服务器端完成 ................ 在客户端完成
 * 速度快 ....................... 速度慢
 * 是同一次请求 .................. 是两次不同请求
 * 不会执行转发后的代码 ........... 会执行重定向之后的代码
 * 转发地址栏没有变化 ............. 地址栏有变化
 * 在同一台服务器下完成 ........... 可在不同服务器下完成
 */
public final class ServletUtil {

    // 2024-07-10
    public static ServletContext servletContext = null;

    // 2023-03-10
    private static ThreadLocal<HttpServletRequest> p_requestLocal = null;
    private static ThreadLocal<HttpServletResponse> p_responseLocal = null;

    /**
     * 2024-08-07
     * SSM工程首先执行该方法、才能正常使用该类的其他方法
     * @param sc
     */
    public static final void init(final ServletContext sc){
        p_requestLocal = new ThreadLocal<>();
        p_responseLocal = new ThreadLocal<>();
        servletContext = sc;
        TomcatUtil.init(sc);
    }

    /**
     * 2025-01-03
     * @param response
     * @param str
     */
    public static final void print(final HttpServletResponse response, final String str){
        PrintWriter printWriter = null;
        try{
            printWriter = response.getWriter();
            printWriter.print(str);
        }catch(Throwable throwable){

        }finally {
            IOUtil.close(printWriter);
        }
    }

    /**
     * 2024-07-25
     * 请求对象转为字节数组
     * @param request
     * @return
     */
    public static final byte[] request2bytes(final HttpServletRequest request){
        if(request == null){
            return null;
        }
        InputStream inputStream = null;
        try{
            inputStream = request.getInputStream();
            final byte[] bytes = IOUtil.inputstream2bytes(inputStream);
            return bytes;
        }catch (Throwable throwable){
            final String requestURI = getRequestURI("do");
            LogUtil.error("ServletUtil_request2bytes_t1", throwable, "缓存输入流字节数组失败", requestURI);
            return null;
        }finally {
            IOUtil.close(inputStream);
        }
    }

    /**
     * 2024-07-25
     * 字节数组转为输入流对象
     * @param bytes
     * @return
     */
    public static final ServletInputStream bytes2servletInputStream(final byte[] bytes){
        ServletInputStream servletInputStream = IOUtil.bytes2servletInputStream(bytes);
        return servletInputStream;
    }

    /**
     * 2024-07-25
     * 客户端json方式传参时设置值
     */
    public static final void getJsonParamMap(){
        HttpServletRequest request = getRequest();
        if(request == null){
            return;
        }
        getJsonParamMap(request);
    }

    /**
     * 2024-07-25
     * 2025-02-27 代码优化
     * 客户端json方式传参时设置值
     * @param request
     */
    public static final Map<String, Object> getJsonParamMap(final HttpServletRequest request){
        final String contentType = ContentTypeUtil.getContentType(request);  // 请求类型
        if(!StringUtil.containOf(contentType, ContentTypeUtil.application_json)){
            return null;
        }
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try{
            inputStream = request.getInputStream();
            if(inputStream == null){
                Map<String, Object> map = new HashMap<>();
                return map;
            }
            String text = IOUtil.inputstream2text(inputStream);
            boolean flag = JsonUtil.isJson(text);
            if(!flag){
                return null;
            }
            Map<String, Object> map = JacksonUtil.json2map(text);
            return map;
        }catch (Throwable throwable){
            LogUtil.error("ServletUtil_setRequestTypeJsonMap_t1", throwable);
            return null;
        }finally {
            IOUtil.close(bufferedReader, inputStreamReader, inputStream);
        }
    }

    /**
     * 2023-07-29
     * 发送给客户端标准json格式字符串
     */
    public static final boolean sendJson2client(final String json){
        HttpServletRequest request = getRequest();
        HttpServletResponse response = getResponse();
        final boolean flag = sendJson2client(request, response, json);
        return flag;
    }

    /**
     * 2020-03-30 5.0.0
     * 发送给客户端标准json格式字符串
     */
    public static final boolean sendJson2client(final HttpServletRequest request, final HttpServletResponse response, final String json){
        if(StringUtil.isBlank(json)){
            LogUtil.error("ServletUtil_sendJson2client_1", "内容为空");
            return false;
        }
        //
        setRequestResponse(request, response);
        PrintWriter printWriter = null;
        try{
            printWriter = response.getWriter();
            printWriter.write(json);
            return true;
        }catch(Throwable throwable){
            if(throwable instanceof IllegalStateException){
                String message = throwable.getMessage();
                if(!StringUtil.isBlank(message)){
                    if(StringUtil.containOf(message, "当前响应已经调用了方法getOutputStream")){
                        return true;
                    }
                }
            }
            LogUtil.error("ServletUtil_sendJson2client_t1", throwable);
            return false;
        }finally {
            IOUtil.close(printWriter);
        }
    }

    /**
     * 2021-04-16 7.5.0
     * 获取精简版request的地址字符串
     * org.apache.catalina.connector.RequestFacade@192de861  ==>  requestFacade@192de861
     */
    public static final String getRequestAddr(final HttpServletRequest request){
        String addr = request.toString();  //如org.apache.catalina.connector.RequestFacade@192de861
        addr = StringUtil.toLowerCase(addr);
        String[] array = StringUtil.split(addr, ".");
        addr = array[array.length - 1];  //如requestFacade@192de861
        return addr;
    }

    /**
     * 2019-11-06 1.0.0
     * @param request
     * @param key
     */
    public static final String getString(final HttpServletRequest request, final String key){
        final String value = getString(request, key, "");
        return value;
    }

    public static final String getString(final String key){
        HttpServletRequest request = getRequest();
        final String value = getString(request, key, "");
        return value;
    }

    public static final String getString(final String key, final String defaultValue){
        HttpServletRequest request = getRequest();
        final String value = getString(request, key, "");
        return value;
    }

    /**
     * 2019-11-06 1.0.0
     * 2024-07-11 添加例外设计，若指定的key无对应值则从固定key名称为text中寻找对应的值
     * @param request
     * @param key
     * @param defaultValue
     */
    public static final String getString(final HttpServletRequest request, final String key, final String defaultValue){
        if(request == null){
            return "";
        }
        //
        String value = request.getParameter(key);
        if(!StringUtil.isBlank(value)){
            return value;
        }
        // 例外设计
        if("text".equals(key)){
            return value;
        }
        final String text = request.getParameter("text");
        if(StringUtil.isBlank(text)){
            return value;
        }
        if(!StringUtil.startsWith(text, "{")){
            return value;
        }
        if(!StringUtil.lastsWith(text, "}")){
            return value;
        }
        Map<String, Object> map = JacksonUtil.json2map(text);
        if(!map.containsKey(key)){
            return value;
        }
        Object valueObj = map.get(key);
        if(valueObj == null){
            return defaultValue;
        }
        value = valueObj.toString();
        return value;
    }


    /**
     * 2019-11-06 1.0.0
     * @param request
     * @param key
     * @param defaultValue
     */
    public static final Integer getInteger(final HttpServletRequest request, final String key, final Integer defaultValue){
        String value = request.getParameter(key);
        if(StringUtil.isBlank(value)){
            return defaultValue;
        }
        value = value.trim();
        Integer valueInt = Integer.parseInt(value);
        return valueInt;
    }

    /**
     * 2019-11-06 1.0.0
     * @param request
     * @param key
     */
    public static final Integer getInteger(final HttpServletRequest request, final String key){
        Integer aaa = getInteger(request, key, null);
        return aaa;
    }

    /**
     * 从request中取得int值
     */
    public static final int getInteger(final String key){
        return getInteger(key, 0);
    }

    /**
     * 从request中取得int值,如果无值则返回缺省值
     */
    public static final int getInteger(final String key, final int defaultValue){
        String str = getString(key);
        if (StringUtil.isBlank(str)){
            return defaultValue;
        }
        return Integer.parseInt(str);
    }

    /**
     * 根据键值取得整形数组
     */
    public static final Integer[] getIntegerArray(final String key){
        HttpServletRequest request = getRequest();
        String[] arrayKey = request.getParameterValues(key);
        Integer[] arrayInt = new Integer[arrayKey.length];
        for (int i = 0; i < arrayKey.length; i++){
            arrayInt[i] = Integer.parseInt(arrayKey[i]);
        }
        return arrayInt;
    }

    /**
     * 从Request中取得long值
     */
    public static final long getLong(final String key){
        return getLong(key, 0);
    }

    /**
     * 从Request中取得long值,如果无值则返回缺省值
     */
    public static final long getLong(final String key, final long defaultValue){
        String str = getString(key);
        if (StringUtil.isBlank(str)){
            return defaultValue;
        }
        return Long.parseLong(str);
    }

    /**
     * 2019-11-06 1.0.0
     * @param request
     * @param key
     * @param defaultValue
     */
    public static final long getLong(final HttpServletRequest request, final String key, final long defaultValue){
        String value = request.getParameter(key);
        if(StringUtil.isBlank(value)){
            return defaultValue;
        }
        value = value.trim();
        final long valueLong = Long.parseLong(value);
        return valueLong;
    }

    /**
     * 2019-11-06 1.0.0
     * @param request
     * @param key
     */
    public static final long getLong(final HttpServletRequest request, final String key){
        long aaa = getLong(request, key, 0L);
        return aaa;
    }

    /**
     * 2019-11-06 1.0.0
     * @param request
     * @param key
     */
    public static final List<Long> getLongListByStr(final HttpServletRequest request, final String key){
        String value = request.getParameter(key);
        if(StringUtil.isBlank(value)){
            return null;
        }
        String[] arrayId = StringUtil.split(value, ",");
        List<Long> list = new ArrayList<>();
        for(int i = 0; i < arrayId.length; i++){
            String strValue = arrayId[i];
            try{
                Long valueLong = Long.parseLong(strValue);
                list.add(valueLong);
            }catch(Throwable throwable){
                LogUtil.error("FrameworkHttpUtil_getLongListByStr_1", throwable, key);
            }
        }
        return list;
    }

    /**
     * 取得长整形数组
     */
    public static final Long[] getLongArray(final String key){
        HttpServletRequest request = getRequest();
        String[] arrayKeys = request.getParameterValues(key);
        if (arrayKeys == null || arrayKeys.length == 0){
            return null;
        }
        Long[] arrayLong = new Long[arrayKeys.length];
        for (int i = 0; i < arrayKeys.length; i++){
            arrayLong[i] = Long.parseLong(arrayKeys[i]);
        }
        return arrayLong;
    }

    /**
     * 根据一串逗号分隔的长整型字符串取得长整形数组
     */
    public static final Long[] getLongArrayByStr(final String key){
        String sysUserId = getString(key);
        String[] arrayId = sysUserId.split(",");
        Long[] lArrayId = new Long[arrayId.length];
        for (int i = 0; i < arrayId.length; i++){
            lArrayId[i] = Long.parseLong(arrayId[i]);
        }
        return lArrayId;
    }

    /**
     * 根据一串逗号分隔的长整型字符串取得长整形数组
     */
    public static final String[] getStringArrayByStr(final String key){
        String ids = getString(key);
        if(StringUtil.isBlank(ids)){
            String[] array = new String[0];
            return array;
        }
        String[] arrayId = ids.split(",");
        return arrayId;
    }

    /**
     * 从Request中取得boolean值,如无值则返回缺省值 0
     */
    public static final Short getShort(final String key){
        return getShort(key, (short) 0);
    }

    /**
     * 从Request中取得Short值 对字符串,如无值则返回缺省值
     */
    public static final Short getShort(final String key, final Short defaultValue){
        String str = getString(key);
        if (StringUtil.isBlank(str)){
            return defaultValue;
        }
        return Short.parseShort(str);
    }

    public static final Float[] getFloatArray(final String key){
        HttpServletRequest request = getRequest();
        String[] arrayKey = request.getParameterValues(key);
        Float[] fArrayId = new Float[arrayKey.length];
        for (int i = 0; i < arrayKey.length; i++){
            fArrayId[i] = Float.parseFloat(arrayKey[i]);
        }
        return fArrayId;
    }

    /**
     * 从Request中取得float值
     */
    public static final float getFloat(final String key){
        return getFloat(key, 0);
    }

    /**
     * 从Request中取得float值,如无值则返回缺省值
     */
    public static final float getFloat(final String key, final float defaultValue){
        String str = getString(key);
        if (StringUtil.isBlank(str)){
            return defaultValue;
        }
        return Float.parseFloat(getString(key));
    }

    /**
     * 从Request中取得boolean值,如无值则返回缺省值 false, 如值为数字1，则返回true
     */
    public static final boolean getBoolean(final String key){
        return getBoolean(key, false);
    }

    /**
     * 从Request中取得boolean值 对字符串,如无值则返回缺省值, 如值为数字1，则返回true
     */
    public static final boolean getBoolean(final String key, final boolean defaultValue){
        String str = getString(key);
        if (StringUtil.isBlank(str)){
            return defaultValue;
        }
        if(RegexUtil.isNumber(str)){
            int value = Integer.parseInt(str);
            if(value == 1){
                return true;
            }
            return false;
        }
        boolean flag = Boolean.parseBoolean(str);
        return flag;
    }

    /**
     * 2019-11-06 1.0.0
     * @param request
     * @param key
     */
    public static final Boolean getBoolean(final HttpServletRequest request, final String key){
        Boolean flag = getBoolean(request, key, false);
        return flag;
    }

    /**
     * 2019-11-06 1.0.0
     * @param request
     * @param key
     * @param defaultValue
     */
    public static final Boolean getBoolean(final HttpServletRequest request, final String key, final Boolean defaultValue){
        if(request != null){
            String value = request.getParameter(key);
            if (StringUtil.isBlank(value)){
                return defaultValue;
            }
            Boolean valueBoolean = Boolean.valueOf(value);
            return valueBoolean;
        }
        return defaultValue;
    }

    /**
     * 从Request中取得Date值,如无值则返回缺省值null, 如果有值则返回 yyyy-MM-dd 格式的日期
     */
    public static final Date getDate(final String key){
        String str = getString(key);
        if (StringUtil.isBlank(str)){
            return null;
        }
        return TimerUtil.str2time(str);
    }

    /**
     * 2024-08-06
     * 判断是否Ajax请求
     */
    public static final boolean isAjax(){
        final HttpServletRequest request = getRequest();
        final boolean flag = isAjax(request);
        return flag;
    }

    /**
     * 2019-11-06 1.0.0
     * 判断是否Ajax请求
     * @param request 请求流
     * @return 是否ajax请求
     */
    public static final boolean isAjax(final HttpServletRequest request){
        String requestType = request.getHeader("X-Requested-With");
        if(StringUtil.isBlank(requestType)){
            requestType = request.getHeader("x-requested-with");
        }
        if(StringUtil.isBlank(requestType)){
            return false;
        }
        //
        requestType = StringUtil.toLowerCase(requestType);
        String xmlHttpRequest = StringUtil.toLowerCase("XMLHttpRequest");
        if(StringUtil.equals(xmlHttpRequest, requestType)){
            return true;
        }
        return false;
    }

    /**
     * 2023-04-04 动态添加自定义过滤器
     * @param sc
     * @param filterName
     * @param filter
     * @param initParamsMap
     * @param urlPatternArray
     */
    public static final void addCustomFilter(final ServletContext sc, final String filterName, final Filter filter, final Map<String, String> initParamsMap, final String... urlPatternArray){
        try{
            FilterRegistration.Dynamic dynamic = sc.addFilter(filterName, filter);
            dynamic.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, urlPatternArray);
            if (CollectionUtil.isBlank(initParamsMap)){
                return;
            }
            Set<Map.Entry<String, String>> entries = initParamsMap.entrySet();
            for (Map.Entry<String, String> m : entries){
                String key = m.getKey();
                String value = m.getValue();
                if (StringUtil.isBlank(key) || StringUtil.isBlank(value)){
                    continue;
                }
                dynamic.setInitParameter(key,value);
            }
        }catch(Throwable throwable){
            LogUtil.error("ServletUtil_addCustomFilter_t1", throwable, filterName, initParamsMap, urlPatternArray);
        }
    }

    /**
     * 2022-07-13
     * 从下载接口获取文件名
     * @param httpURLConnection
     * @return
     */
    public static final String getFilenameFromHttpURLConnection(final HttpURLConnection httpURLConnection, final String addr){
        Map<String, List<String>> headerFieldMap = httpURLConnection.getHeaderFields();
        // 远程接口必须配置了HTTP协议的字段Content-Disposition才可以
        // 通过遍历的方式，兼容HTTP响应头的大小写差异
        List<String> dispositionList = null;
        Set<String> set = headerFieldMap.keySet();
        Iterator<String> iterator = set.iterator();
        while(iterator.hasNext()){
            final String key = iterator.next();
            String keyStr = StringUtil.toLowerCase(key);
            keyStr = StringUtil.trim(keyStr);
            if("content-disposition".equals(keyStr)){
                dispositionList = headerFieldMap.get(key);
                break;
            }
        }
        if(CollectionUtil.isBlank(dispositionList)){
            final String msg = "远程下载接口必须设置HTTP协议响应头字段Content-Disposition，且必须设置文件名";
            LogUtil.error("ServletUtil_getFilenameFromHttpURLConnection_1", msg, addr);
            return "";
        }
        String disposition = dispositionList.get(0);
        String[] array = StringUtil.split(disposition, "=");
        String downloadFilename = array[1];
        downloadFilename = StringUtil.trim(downloadFilename);
        return downloadFilename;
    }

    /**
     * 2020-09-08 6.6.0
     * 该方法是当前程序作为服务端、读取当前程序所在机器（服务器）磁盘中的文件，响应给客户端。
     * 单文件下载
     * 支持分段下载
     * @param request
     * @param response
     * @param name
     */
    public static final void downloadResponseSet(final HttpServletRequest request, final HttpServletResponse response, final String name){
        //
        downloadResponseSet(request, response, name, null, null);
    }

    /**
     * 2020-09-08 6.6.0
     * 该方法是当前程序作为服务端、读取当前程序所在机器（服务器）磁盘中的文件，响应给客户端。
     * 单文件下载
     * 支持分段下载
     * @param request
     * @param response
     * @param name
     * @param start
     */
    public static final void downloadResponseSet(final HttpServletRequest request, final HttpServletResponse response, final String name, final Long start){
        //
        downloadResponseSet(request, response, name, start, null);
    }

    /**
     * 2019-12-02 1.0.0
     * 该方法是当前程序作为服务端、读取当前程序所在机器（服务器）磁盘中的文件，响应给客户端。
     * 单文件下载
     * 支持分段下载
     * @param request
     * @param response
     * @param name
     * @param start
     * @param size
     */
    public static final void downloadResponseSet(final HttpServletRequest request, final HttpServletResponse response, final String name, final Long start, final Long size){
        if(response == null){
            return;
        }
        //
        Long localStart = start;
        Long localSize = size;
        if(localStart == null || localStart < 0){
            localStart = 0L;
        }
        if(localSize == null || localSize < 0){
            localSize = 0L;
        }
        // request为空直接返回
        if(request == null){
            response.setStatus(200);
            if(localSize > 0){
                response.setHeader("Content-Range", "bytes 0-" + (localSize-1) + "/" + localSize);
                response.setHeader("Content-Length", String.valueOf(localSize));
            }else{
                response.setHeader("Content-Range", "bytes 0-*/*");
            }
            setContentDisposition(response, name);
            return;
        }
        // 解析参数range
        String range = request.getHeader("Range");
        if(StringUtil.isBlank(range)){
            range = request.getHeader("range");
        }
        if(StringUtil.isBlank(range)){
            response.setStatus(200);
            if(localSize > 0){
                response.setHeader("Content-Range", "bytes 0-" + (localSize-1) + "/" + localSize);
                response.setHeader("Content-Length", String.valueOf(localSize));
            }else{
                response.setHeader("Content-Range", "bytes 0-*/*");
            }
            setContentDisposition(response, name);
            return;
        }
        //
        // 解析与计算
        String scope = range.replaceAll("bytes=", "");
        String[] array = StringUtil.split(scope, "-");
        if(array == null || array.length <= 0){
            localStart = 0L;
        }else{
            localStart = Long.parseLong(array[0]);
            if(localStart == null || localStart < 0){
                localStart = 0L;
            }
        }
        Long endIndex = 0L;
        if(localSize > 0){
            if(localSize < localStart){
                localSize = localStart;
            }
            endIndex = localSize - 1;
            if(array != null && array.length >= 1){
                String bbb = array[1];
                bbb = StringUtil.getPureStr(bbb);
                bbb = StringUtil.trim(bbb);
                if(!StringUtil.isBlank(bbb)) {
                    endIndex = Long.parseLong(bbb);  //范围是从0到localSize-1
                }else{
                    endIndex = localSize - 1;
                }
            }
        }
        // IOS设备
        boolean isIOS = TerminalUtil.isIos(request);
        if(isIOS){
            response.setStatus(206);
            if(localSize > 0){
                response.setHeader("Content-Range", "bytes " + localStart + "-" + endIndex + "/" + localSize);
            }else{
                response.setHeader("Content-Range", "bytes " + localStart + "-*/*");
            }
            setContentDisposition(response, name);
            return;
        }
        // 其它终端设备
        response.setStatus(206);
        if(localSize > 0){
            response.setHeader("Content-Range", "bytes=" + localStart + "-" + endIndex + "/" + localSize);
            response.setHeader("Content-Length", String.valueOf(endIndex + 1 - localStart));
        }else{
            response.setHeader("Content-Range", "bytes=" + localStart + "-*/*");
        }
        setContentDisposition(response, name);
    }

    /**
     * 2020-09-08 6.6.0
     * 该方法是当前程序作为服务端、读取当前程序所在机器（服务器）磁盘中的文件，响应给客户端。
     * @param response
     * @param name
     */
    public static final void downloadZipReady(final HttpServletResponse response, final String name){
        //
        downloadZipReady(response, name, null);
    }

    /**
     * 2019-12-02 1.0.0
     * 该方法是当前程序作为服务端、读取当前程序所在机器（服务器）磁盘中的文件，响应给客户端。
     * 多文件打包下载
     * 支持分段下载
     * @param response
     * @param name
     * @param size
     */
    public static final void downloadZipReady(final HttpServletResponse response, final String name, final Long size){
        if(response == null){
            return;
        }
        Long localSize = size;
        if(localSize == null || localSize < 0){
            localSize = 0L;
        }
        //
        response.setStatus(200);
        if(localSize > 0){
            response.setHeader("Content-Range", "bytes 0-" + (localSize - 1L) + "/" + localSize);
        }else{
            response.setHeader("Content-Range", "bytes 0-*/*");
        }
        setContentDisposition(response, name);
    }

    /**
     * 2022-07-12
     * 下载文件
     * @param address
     */
    public static final void downloadFile(final String address, final String targetUrl){
        File file = new File(targetUrl);
        FileUtil.dirMakeByUrl(targetUrl);
        FileOutputStream fileOutputStream = null;
        try{
            fileOutputStream = new FileOutputStream(file);
            downloadFile(address, null, null, fileOutputStream, null);
        }catch(Throwable throwable){
            LogUtil.error("ServletUtil_downloadFile_t5", throwable, address, targetUrl);
        }finally {
            IOUtil.close(fileOutputStream);
        }
    }

    /**
     * 2022-07-12
     * 下载文件
     * @param address
     */
    public static final void downloadFile(final String address, final Map<String, Object> paramMap, final String targetUrl){
        File file = new File(targetUrl);
        FileUtil.dirMakeByUrl(targetUrl);
        FileOutputStream fileOutputStream = null;
        try{
            fileOutputStream = new FileOutputStream(file);
            downloadFile(address, paramMap, null, fileOutputStream, null);
        }catch(Throwable throwable){
            LogUtil.error("ServletUtil_downloadFile_t4", throwable, address, targetUrl);
        }finally {
            IOUtil.close(fileOutputStream);
        }
    }

    /**
     * 2022-07-12
     * 下载文件
     * @param address
     */
    public static final void downloadFile(final String address, final Map<String, Object> paramMap, final HttpServletResponse response){        ServletOutputStream servletOutputStream = null;
        try{
            servletOutputStream = response.getOutputStream();
            downloadFile(address, paramMap, response, servletOutputStream, null);
        }catch(Throwable throwable){
            LogUtil.error("ServletUtil_downloadFile_t3", throwable, "下载失败", address);
            return;
        }finally {
            IOUtil.close(servletOutputStream);
        }
    }

    /**
     * 2022-07-12
     * 下载文件
     * @param address
     */
    public static final void downloadFile(final String address, final Map<String, Object> paramMap, final HttpServletResponse response, final String filename){
        ServletOutputStream servletOutputStream = null;
        try{
            servletOutputStream = response.getOutputStream();
            downloadFile(address, paramMap, response, servletOutputStream, filename);
        }catch(Throwable throwable){
            LogUtil.error("ServletUtil_downloadFile_t2", throwable, "下载失败", address);
            return;
        }finally {
            IOUtil.close(servletOutputStream);
        }
    }

    /**
     * 2022-07-12
     * 该方法既可作为服务端程序的下载，也可作为客户端程序的下载。
     * 下载文件
     * @param address
     */
    public static final void downloadFile(final String address, final Map<String, Object> paramMap, final HttpServletResponse response, final OutputStream outputStream, final String filename){
        String addr = UrlUtil.getStandardAddress(address, paramMap);
        InputStream inputStream = null;
        try{
            URL url = new URL(addr);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestProperty("accept", "*/*");
            httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setRequestMethod("GET");  // 默认为GET
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoOutput(true);  // post请求该值为true
            httpURLConnection.setDoInput(true);
            httpURLConnection.setConnectTimeout(Integer.MAX_VALUE);  // 设置连接超时时间
            int responseCode = httpURLConnection.getResponseCode();
            if(responseCode == 200){
                httpURLConnection.connect();
                inputStream = httpURLConnection.getInputStream();
            }else{
                final String msg = "下载异常";
                LogUtil.error("ServletUtil_downloadFile_11", msg, responseCode, address, filename);
                return;
            }
            if(response != null){
                String downloadFilename = filename;
                if(StringUtil.isBlank(downloadFilename)){
                    downloadFilename = getFilenameFromHttpURLConnection(httpURLConnection, addr);
                }
                if(StringUtil.isBlank(downloadFilename)){
                    LogUtil.error("ServletUtil_downloadFile_12", "downloadFilename为空", addr);
                    return;
                }
                downloadResponseSet(null, response, downloadFilename);
            }
            IOUtil.inputstream2outputstream(inputStream, outputStream);
        }catch(Throwable throwable){
            LogUtil.error("ServletUtil_downloadFile_e10", throwable, address);
        }finally {
            IOUtil.close(inputStream);
        }
    }

    /**
     * 2022-07-13
     * 该方法是当前程序作为服务端、读取当前程序所在机器（服务器）磁盘中的文件，响应给客户端。
     * 下载文件
     * @param address
     */
    public static final void downloadFileFromDisk(final String address){
        final String targetPath = FileUtil.getTempPath();
        downloadFileFromDisk(address, null, targetPath);
    }

    /**
     * 2022-07-13
     * 该方法是当前程序作为服务端、读取当前程序所在机器（服务器）磁盘中的文件，响应给客户端。
     * 下载文件
     * @param address
     */
    public static final void downloadFileFromDisk(final String address, final Map<String, Object> paramMap){
        final String targetPath = FileUtil.getTempPath();
        downloadFileFromDisk(address, paramMap, targetPath);
    }

    /**
     * 2022-07-13
     * 该方法是当前程序作为服务端、读取当前程序所在机器（服务器）磁盘中的文件，响应给客户端。
     * 下载文件
     * @param address
     * @param targetPath 结尾不能带/
     */
    public static final String downloadFileFromDisk(final String address, final Map<String, Object> paramMap, final String targetPath){
        String addr = UrlUtil.getStandardAddress(address, paramMap);
        FileOutputStream fileOutputStream = null;
        InputStream inputStream = null;
        try{
            URL url = new URL(addr);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestProperty("accept", "*/*");
            httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setRequestMethod("POST");  // 默认为GET
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoOutput(true);  // post请求该值为true
            httpURLConnection.setDoInput(true);
            httpURLConnection.setConnectTimeout(Integer.MAX_VALUE);  // 设置连接超时时间
            //
            int responseCode = httpURLConnection.getResponseCode();
            if(responseCode == 200){
                httpURLConnection.connect();
                inputStream = httpURLConnection.getInputStream();
            }else{
                final String msg = "下载异常";
                LogUtil.error("ServletUtil_downloadFileFromDisk_11", msg, responseCode, addr, targetPath);
                return "";
            }
            String downloadFilename = getFilenameFromHttpURLConnection(httpURLConnection, addr);
            if(StringUtil.isBlank(downloadFilename)){
                LogUtil.error("ServletUtil_downloadFileFromDisk_12", "downloadFilename为空", address, addr);
                return "";
            }
            final String targetUrl = targetPath + "/" + downloadFilename;
            FileUtil.dirMake(targetPath);
            fileOutputStream = new FileOutputStream(targetUrl);
            IOUtil.inputstream2outputstream(inputStream, fileOutputStream);
            return targetUrl;
        }catch(Throwable throwable){
            LogUtil.error("ServletUtil_downloadFileFromDisk_e10", throwable, address, addr, targetPath);
            return "";
        }finally {
            IOUtil.close(inputStream, fileOutputStream);
        }
    }

    /**
     * 2019-11-06 1.0.0
     * 外部请求下载单个文件
     * 该方法是当前程序作为服务端、读取当前程序所在机器（服务器）磁盘中的文件，响应给客户端。
     * @param serverFileUrl 指定要下载文件所在服务器的磁盘绝对路径地址
     */
    public static final void downloadFileFromDisk(final HttpServletRequest request, final HttpServletResponse response, final String serverFileUrl){
        String name = FileUtil.getFileName(serverFileUrl);
        ServletUtil.downloadFileFromDisk(request,response,serverFileUrl,name);
    }

    /**
     * 2019-11-06 1.0.0
     * 2023-10-09
     * 外部请求下载单个文件
     * 该方法是当前程序作为服务端、读取当前程序所在机器（服务器）磁盘中的文件，响应给客户端。
     * @param serverFileUrl 指定要下载文件所在服务器的磁盘绝对路径地址
     * @param downloadName 指定浏览器下载文件名称
     *
     */
    public static final void downloadFileFromDisk(final HttpServletRequest request, final HttpServletResponse response, String serverFileUrl, final String downloadName){
        if(!FileUtil.exists(serverFileUrl)){
            LogUtil.error("ServletUtil_downloadFileFromDisk_1", "file_not_found", serverFileUrl);
            return;
        }
        serverFileUrl = UrlUtil.getStandardPath(serverFileUrl);
        Long size = FileUtil.getFileSize(serverFileUrl);
        downloadResponseSet(request, response, downloadName, 0L, size);
        FileInputStream inputStream = null;
        OutputStream outputStream = null;
        try{
            inputStream = new FileInputStream(serverFileUrl);
            outputStream = response.getOutputStream();
            IOUtil.inputstream2outputstream(inputStream, outputStream);
        }catch(Throwable throwable){
            LogUtil.error("ServletUtil_downloadFileFromDisk_t1", throwable, serverFileUrl);
        }finally {
            IOUtil.close(outputStream, inputStream);
        }
    }

    /**
     * 2024-08-07 返回值改为boolean类型
     * @param response
     * @param str
     * @return
     */
    public static final boolean download(final HttpServletResponse response, final String str){
        OutputStream outputStream = null;
        try{
            byte[] bytes = str.getBytes("UTF-8");
            outputStream = response.getOutputStream();
            outputStream.write(bytes);  // 写入文件
            return true;
        }catch(Throwable throwable){
            final String requestURI = ServletUtil.getRequestURI();
            LogUtil.error("ServletUtil_download_t1", throwable, requestURI, str);
            return false;
        }finally {
            IOUtil.close(outputStream);
        }
    }

    /**
     * 2024-08-07 返回值改为boolean类型
     * @param response
     * @param inputStream
     * @return
     */
    public static final boolean download(final HttpServletResponse response, final InputStream inputStream){
        BufferedInputStream bufferedInputStream = null;
        ServletOutputStream servletOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try{
            bufferedInputStream = new BufferedInputStream(inputStream);
            servletOutputStream = response.getOutputStream();
            bufferedOutputStream = new BufferedOutputStream(servletOutputStream);
            byte[] bytes = new byte[8192];
            int bytesRead;
            while(-1 != (bytesRead = bufferedInputStream.read(bytes, 0, bytes.length))) {
                bufferedOutputStream.write(bytes, 0, bytesRead);
            }
            return true;
        }catch(Throwable throwable){
            final String requestURI = ServletUtil.getRequestURI();
            LogUtil.error("HttpClientUtil_download_t1", throwable, requestURI);
            return false;
        }finally{
            IOUtil.close(bufferedOutputStream, servletOutputStream, bufferedInputStream);
        }
    }

    /**
     * 2024-08-07 返回值改为boolean类型
     * 下载文件
     */
    public static final boolean download(final HttpServletResponse response, final File file){
        if(file != null && !file.exists()){
            LogUtil.error("HttpClientUtil_download_1", "文件不存在", file);
            return false;
        }
        OutputStream outputStream = null;
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try{
            outputStream = response.getOutputStream();
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            IOUtils.copy(bufferedInputStream, outputStream);
            return true;
        }catch(Throwable throwable){
            final String requestURI = ServletUtil.getRequestURI();
            LogUtil.error("HttpClientUtil_download_t1", throwable, requestURI, file);
            return false;
        }finally{
            IOUtil.close(bufferedInputStream, fileInputStream, outputStream);
        }
    }

    /**
     * 2024-08-07 返回值改为boolean类型
     * @param inputStream
     * @return
     */
    public static final boolean download(final InputStream inputStream){
        HttpServletResponse response = ServletUtil.getResponse();
        try{
            boolean success = inputToOut(inputStream, response.getOutputStream());
            return success;
        }catch(Throwable throwable){
            final String requestURI = ServletUtil.getRequestURI();
            LogUtil.error("FileUtil_downLoad_t29", throwable, requestURI);
            return false;
        }
    }

    /**
     * 2024-08-07 返回值改为boolean类型
     * @param inputStream
     * @param outputStream
     * @return
     */
    public static final boolean inputToOut(InputStream inputStream, OutputStream outputStream){
        BufferedInputStream bufferedInputStream = null;
        try{
            bufferedInputStream = new BufferedInputStream(inputStream);
            IOUtils.copy(bufferedInputStream, outputStream);
            return true;
        }catch(Throwable throwable){
            final String requestURI = ServletUtil.getRequestURI();
            LogUtil.error("FileUtil_inputToOut_t30", throwable, requestURI);
            return false;
        }finally{
            IOUtil.close(inputStream, bufferedInputStream, outputStream);
        }
    }

    /**
     * 2023-02-13
     * @param response
     * @param url
     */
    public static final void previewFileFromDisk(HttpServletResponse response, String url){
        long size = FileUtil.getFileSize(url);
        previewFileFromDisk(response, url, 0L, size);
    }

    /**
     * 2023-02-13
     * @param response
     * @param url
     * @param start
     * @param size
     */
    public static final void previewFileFromDisk(HttpServletResponse response, String url, Long start, Long size){
        url = UrlUtil.getStandardPath(url);
        if(!FileUtil.exists(url)){
            LogUtil.error("ServletUtil_previewFileFromDisk_1", "文件不存在", url, start, size);
            return;
        }
        //
        String name = FileUtil.getFileName(url);
        previewResponseSet(response, name, start, size);
        FileInputStream fileInputStream = null;
        ServletOutputStream servletOutputStream = null;
        try{
            fileInputStream = new FileInputStream(url);
            servletOutputStream = response.getOutputStream();
            IOUtil.inputstream2outputstream(fileInputStream, servletOutputStream);
        }catch(Throwable throwable){
            LogUtil.error("ServletUtil_previewFileFromDisk_t1", throwable, url, start, size);
        }finally {
            IOUtil.close(servletOutputStream, fileInputStream);
        }
    }

    /**
     * 2022-07-13
     * 预览文件
     * @param address
     */
    public static final void previewFile(final String address, final Map<String, Object> paramMap, final HttpServletResponse response){
        previewFile(address, paramMap, response, null);
    }

    /**
     * 2022-07-13
     * 预览文件
     * @param address
     */
    public static final void previewFile(final String address, final Map<String, Object> paramMap, final HttpServletResponse response, final String filename){
        String addr = UrlUtil.getStandardAddress(address, paramMap);
        if(response == null){
            LogUtil.error("ServletUtil_previewFile_1", "response为空", addr);
            return;
        }
        InputStream inputStream = null;
        ServletOutputStream servletOutputStream = null;
        try{
            URL url = new URL(addr);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestProperty("accept", "*/*");
            httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setRequestMethod("POST");  // 默认为GET
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoOutput(true);  // post请求该值为true
            httpURLConnection.setDoInput(true);
            httpURLConnection.setConnectTimeout(Integer.MAX_VALUE);  // 设置连接超时时间
            int responseCode = httpURLConnection.getResponseCode();
            if(responseCode == 200){
                httpURLConnection.connect();
                inputStream = httpURLConnection.getInputStream();
            }else{
                final String msg = "预览异常";
                LogUtil.error("ServletUtil_previewFile_3", msg, responseCode, addr);
                return;
            }
            String previewFilename = filename;
//            if(StringUtil.isBlank(previewFilename)){
//                previewFilename = getFilenameFromHttpURLConnection(httpURLConnection, addr);
//            }
            previewResponseSet(response, previewFilename, null, null);
            servletOutputStream = response.getOutputStream();
            IOUtil.inputstream2outputstream(inputStream, servletOutputStream);
        }catch(Throwable throwable){
            LogUtil.error("ServletUtil_previewFile_t1", throwable, addr);
        }finally {
            IOUtil.close(servletOutputStream, inputStream);
        }
    }

    /**
     * 20191106 1.0.0
     * 预览文件响应信息封装
     * @param response 响应对象
     * @param name 文件名
     * @param start  响应文件位置
     * @param size 响应文件大小
     */
    public static final void previewResponseSet(final HttpServletResponse response, final String name, Long start, Long size){
        if(start == null){
            start = 0L;
        }
        if(size == null || size < 0){
            size = 0L;
        }
        String mime = MimeTypeUtil.getMimeTypeByFileName(name);
        response.setContentType(mime + ";charset=UTF-8");
        if(size > 0){
            response.setContentLength((int)(size - start));
            response.setHeader("Content-Range", "bytes " + start + "-" + (size - 1) + "/" + size);
        }else{
            response.setHeader("Content-Range", "bytes " + start + "-*/*");
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Accept-Ranges", "bytes");
    }

    /**
     * 2019-11-06 1.0.0
     * @param request
     * @param pos
     * @param fileSize
     */
    public static final Long[] getRange(final HttpServletRequest request, Long pos, final Long fileSize){
        if(pos == null){
            pos = 0L;
        }
        String scope = "";
        long start = 0;
        long end = 0;
        try{
            String range = request.getHeader("Range");
            if(StringUtil.isBlank(range)){
                range = request.getHeader("range");
            }
            if(!StringUtil.isBlank(range)) {
                scope = range.replaceAll("bytes=", "");
                String[] array = StringUtil.split(scope, "-");
                start = Long.parseLong(array[0]);
                end = Long.parseLong(array[1]);
                if(start > 0){
                    pos = start;
                }
            }
        }catch(Throwable throwable){
            //ingore
        }
        if(end <= 0){
            end = fileSize;
        }
        Long[] array = {pos, end};
        return array;
    }

    /**
     * 2025-01-21
     */
    public static final Map<String, Object> getParamMap(){
        HttpServletRequest request = getRequest();
        final Map<String, Object> paramMap = getParamMap(request, "form");
        return paramMap;
    }

    /**
     * 2019-11-19 1.0.0
     */
    public static final Map<String, Object> getParamMap(HttpServletRequest request){
        Map<String, Object> paramMap = getParamMap(request, "form");
        return paramMap;
    }

    /**
     * 2019-11-06 1.0.0
     * 获得HttpServletRequest中所有参数和对应值
     * @param request
     * @param paramaterType
     */
    public static final Map<String, Object> getParamMap(final HttpServletRequest request, String paramaterType){
        final Map<String, Object> paramMap = new HashMap<>(8);
        if(request == null){
            return paramMap;
        }
        if(StringUtil.isBlank(paramaterType)){
            paramaterType = "form";
        }
        if("form".equals(paramaterType)){  //解析form表单传参
            Enumeration paramNames = request.getParameterNames();
            while(paramNames.hasMoreElements()){
                String name = (String) paramNames.nextElement();
                String[] values = request.getParameterValues(name);
                if(values.length == 1){
                    String value = values[0];
                    if(value.length() != 0){
                        paramMap.put(name, value);
                    }
                }
            }
            return paramMap;
        }
        return paramMap;
    }

    /**
     * 2019-11-19 1.0.0
     * @param request
     * @param key
     */
    public static final String getParam(final HttpServletRequest request, final String key){
        if(StringUtil.isBlank(key)){
            return null;
        }
        Map<String, Object> map = getParamMap(request);
        if(CollectionUtil.isBlank(map)){
            return null;
        }
        Object value = map.get(key);
        if(value == null){
            value = "null";
        }
        String svalue = value.toString();
        return svalue;
    }

    /**
     * 2019-11-06 1.0.0
     * @param request
     */
    public static final String getDomain(final HttpServletRequest request){
        StringBuffer stringBuffer = request.getRequestURL();
        String url = stringBuffer.toString();
        String uri = request.getRequestURI();
        uri = url.replaceAll(uri, "");
        return uri;
    }

    /**
     * 2023-09-05
     * @param suffixTypes 要删除的文件后缀格式集合（每个文件格式前没有点），如 doc,png,jpg
     * @return
     */
    public static final String getRequestURI(final String... suffixTypes){
        String requestURI = getRequestURI();
        if(CollectionUtil.isBlank(suffixTypes)){
            return requestURI;
        }
        //
        for(String suffix : suffixTypes){
            final String suffix2 = "." + suffix;
            if(!StringUtil.lastsWith(requestURI, suffix2)){
                continue;
            }
            requestURI = StringUtil.deleteLast(requestURI, suffix2);
            break;
        }
        requestURI = UrlUtil.getStandardApi(requestURI, false);
        return requestURI;
    }

    /**
     * 2023-07-13
     * @return 返回标准接口地址，格式如 /api/efile/login
     */
    public static final String getRequestURI(){
        HttpServletRequest request = getRequest();
        if(request == null){
            return "";
        }
        //
        String requestURI = getRequestURI(request);
        return requestURI;
    }

    /**
     * 2023-07-13
     * 2023-10-10 解决以;jsessionid=xxx结尾的问题
     * @param request
     * @return 返回标准接口或资源地址，格式如 /api/efile/login
     */
    public static final String getRequestURI(HttpServletRequest request){
        if(request == null){
            return "";
        }
        String requestURI = request.getRequestURI();
        // 2023-10-10 解决以;jsessionid=xxx结尾的问题
        if(StringUtil.containOf(requestURI, ";jsessionid=")){
            String[] array = StringUtil.split(requestURI, ";jsessionid=");
            requestURI = array[0];
        }
        //
        requestURI = UrlUtil.getStandardApi(requestURI, false);
        return requestURI;
    }

    /**
     * 2023-07-13
     * @param isLastSlash 是否以/结尾
     * @return 返回标准接口地址，格式如 /api/efile/login
     */
    public static final String getRequestURI(boolean isLastSlash){
        HttpServletRequest request = getRequest();
        if(request == null){
            return "";
        }
        //
        String requestURI = request.getRequestURI();
        String apiUri = UrlUtil.getStandardApi(requestURI, isLastSlash);
        return apiUri;
    }

    /**
     * 2020-08-31 6.6.0
     * @param request
     * @param isLastSlash 是否以/结尾
     * @return 返回标准接口地址，格式如 /api/efile/login
     */
    public static final String getRequestURI(HttpServletRequest request, boolean isLastSlash){
        if(request == null){
            return "";
        }
        //
        String requestURI = request.getRequestURI();
        String apiUri = UrlUtil.getStandardApi(requestURI, isLastSlash);
        return apiUri;
    }

    /**
     * 2019-11-06 1.0.0
     * servlet标准设置
     * @param request
     * @param response
     */
    public static final void setRequestResponse(final HttpServletRequest request, final HttpServletResponse response){
        setContext(request, response);
        p_setRequest(request);
        p_setResponse(request, response);
    }

    /**
     * 2020-04-01 5.0.0
     * @param request
     */
    private static final void p_setRequest(HttpServletRequest request){
        if(request == null){
            request = getRequest();
            if(request == null){
                return;
            }
        }
        //
        try{
            request.setCharacterEncoding("UTF-8");
        }catch(Throwable throwable){
            LogUtil.error("ServletUtil_p_setRequest_t1", throwable);
        }
    }

    /**
     * 2025-01-21
     */
    private static final void p_setResponse(){
        p_setResponse(null, null, "*");
    }

    /**
     * 2025-01-21
     * @param response
     */
    private static final void p_setResponse(final HttpServletRequest request, final HttpServletResponse response){
        p_setResponse(request, response, "*");
    }

    /**
     * 2020-03-30 5.0.0
     * 2025-01-21
     * 可解决的问题：
     *   数据乱码问题
     *   跨域问题
     *   统一异常处理问题
     * setHeader(name, value);  //若header中没有定义则添加，已定义则覆盖原值。无则加有则盖
     * addHeader(name, value);  //若header中没有定义则添加，已定义则原值不变。无则加有不盖
     * @param response
     */
    private static final void p_setResponse(HttpServletRequest request, HttpServletResponse response, final String crossdomainValue){
        if(request == null){
            request = getRequest();
            if(request == null){
                return;
            }
        }
        if(response == null){
            response = getResponse();
            if(response == null){
                return;
            }
        }
        //
        response.setStatus(200);  //设置状态码，默认按正常来看待，值为200
        response.setCharacterEncoding("UTF-8");
        response.setContentType(ContentTypeUtil.application_json);
        response.setHeader("Content-Type", ContentTypeUtil.application_json);
        // 设置跨域
        setCrossdomain(request, response, crossdomainValue);
    }

    /**
     * 2025-01-21
     */
    public static final void setCrossdomain(){
        setCrossdomain(null, null, "*");
    }

    /**
     * 2025-01-21
     * @param crossdomainValue
     */
    public static final void setCrossdomain(final String crossdomainValue){
        setCrossdomain(null, null, crossdomainValue);
    }

    /**
     * 2025-01-21
     * @param response
     */
    public static final void setCrossdomain(final HttpServletRequest request, final HttpServletResponse response){
        setCrossdomain(request, response, "*");
    }

    /**
     * 2025-01-15
     * 2025-08-06 标准化扩展
     * 跨域配置
     * addHeader(name, value), 若header中没有定义则添加，已定义则原值不变。
     * setHeader(name, value), 若header中没有定义则添加，已定义则覆盖原值。
     * @param request
     * @param response
     * @param crossdomainValue 以英文逗号分隔，格式如：*,http://localhost:8080,http://127.0.0.1:8080
     */
    public static final void setCrossdomain(HttpServletRequest request, HttpServletResponse response, String crossdomainValue){
        if(request == null){
            request = getRequest();
            if(request == null){
                return;
            }
        }
        if(response == null){
            response = getResponse();
            if(response == null){
                return;
            }
        }
        // 2025-08-06 安全-跨域配置
        crossdomainValue = StringUtil.getStandardStr(crossdomainValue);
        crossdomainValue = StringUtil.deleteAll(crossdomainValue, " ");
        if(StringUtil.isBlank(crossdomainValue) || "*".equals(crossdomainValue)){
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Credentials", "false");  // 是否允许携带凭证（如Cookie），默认值false
        }else{
            String[] crossdomainValueArray = StringUtil.split(crossdomainValue, ",");
            List<String> crossdomainValueList = Arrays.asList(crossdomainValueArray);
            if(crossdomainValueList.contains("*")){
                response.setHeader("Access-Control-Allow-Origin", "*");
                response.setHeader("Access-Control-Allow-Credentials", "false");  // 是否允许携带凭证（如Cookie），默认值false
            }else{
                final String origin = request.getHeader("Origin");  // 获取请求的Origin头
                if (origin != null && crossdomainValueList.contains(origin)) {  // 验证来源是否在允许列表中
                    response.setHeader("Access-Control-Allow-Origin", origin);  // 设置允许访问的特定域名
                    response.setHeader("Access-Control-Allow-Credentials", "true");  // 是否允许携带凭证（如Cookie），默认值false
                }else{
                    response.setHeader("Access-Control-Allow-Origin", "*");
                    response.setHeader("Access-Control-Allow-Credentials", "false");  // 是否允许携带凭证（如Cookie），默认值false
                }
            }
        }
        //
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, PATCH, DELETE, PUT");
        response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Cache-Control", "no-cache, must-revalidate");
        response.setHeader("X-Content-Type-Options", "nosniff");  // 2025-07-25 检测到目标X-Content-Type-Options响应头缺失
        response.setHeader("X-XSS-Protection", "1;mode=block");  // 2023-07-30 add
    }

    /**
     * 2019-12-06 1.0.0
     */
    public static final String getSession(final HttpServletRequest request){
        HttpSession httpSession = request.getSession(true);
        String session = httpSession.getId();
        return session;
    }

    /**
     * 2020-12-19 7.1.0
     */
    public static final String getCurrentHost(){
        final String currentHost = "";
        return currentHost;
    }

    public static final void setContext(final HttpServletRequest request){
        if(request == null){
            return;
        }
        long now = System.currentTimeMillis();
        request.setAttribute("crabapple_stamp", now+"");
        p_requestLocal.set(request);
    }

    public static final void setContext(final HttpServletResponse response){
        if(response == null){
            return;
        }
        p_responseLocal.set(response);
    }

    /**
     * 2023-03-10
     * 一般在实际项目中的过滤器类中调用
     * @param request
     * @param response
     */
    public static final void setContext(final HttpServletRequest request, final HttpServletResponse response){
        if(request != null){
            p_requestLocal.set(request);
        }
        if(response != null){
            p_responseLocal.set(response);
        }
    }

    /**
     * 清除http上下文。
     */
    public static final void cleanContext(){
        p_requestLocal.remove();
        p_responseLocal.remove();
    }

    /**
     * 获取HTTP请求对象。
     */
    public static final HttpServletRequest getRequest(){
        if(p_requestLocal == null){
//            LogUtil.warn("ServletUtil_getRequest_1", "p_requestLocal对象为空");
            return null;
        }
        //
        HttpServletRequest request = p_requestLocal.get();
        if(request == null){
            return null;
        }
        //
        return request;
    }

    /**
     * 获取HTTP响应对象。
     */
    public static final HttpServletResponse getResponse(){
        if(p_requestLocal == null){
//            LogUtil.warn("ServletUtil_getResponse_1", "p_responseLocal对象为空");
            return null;
        }
        //
        HttpServletResponse response = p_responseLocal.get();
        if(response == null){
            return null;
        }
        //
        return response;
    }

    /**
     * 2023-09-07
     * @param key
     * @return
     */
    public static final String getAttribute(final String key){
        final HttpServletRequest request = getRequest();
        final String value = getAttribute(request, key);
        return value;
    }

    /**
     * 2019-11-06 1.0.0
     * @param request
     * @param key
     */
    public static final String getAttribute(HttpServletRequest request, final String key){
        Object object = request.getAttribute(key);
        if(object == null){
            return "";
        }
        String value = object.toString();
        return value;
    }

    /**
     * 2023-05-22
     * @param key
     * @param value
     */
    public static final void setAttribute(final String key, final Object value){
        HttpServletRequest request = getRequest();
        if(request == null){
            LogUtil.error("ServletUtil_setAttribute_1", "request对象为空", key, value);
            return;
        }
        request.setAttribute(key, value);
    }

    /**
     * 2023-05-22
     * @param request
     * @param key
     * @param value
     */
    public static final void setAttribute(HttpServletRequest request, final String key, final Object value){
        if(request == null){
            LogUtil.error("ServletUtil_setAttribute_2", "request对象为空", key, value);
            return;
        }
        request.setAttribute(key, value);
    }

    /**
     * 2023-08-03
     */
    public static final String getUserAgent(){
        HttpServletRequest request = getRequest();
        if(request == null){
            return "";
        }
        //
        String userAgent = request.getHeader("user-agent");
        if(!StringUtil.isBlank(userAgent)){
            return userAgent;
        }
        userAgent = request.getHeader("User-Agent");
        if(!StringUtil.isBlank(userAgent)){
            return userAgent;
        }
        //
        return "";
    }

    /**
     * 2023-07-12
     */
    public static final String getClientInfo(){
        HttpServletRequest request = getRequest();
        final String clientInfo = getClientInfo(request);
        return clientInfo;
    }

    /**
     * 2019-11-06 1.0.0
     * User-Agent : IntelliJ IDEA/162.2032.8
     * IE11     User-Agent : Mozilla/5.0 (MSIE 8.0; Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko
     * Firefox  User-Agent : Mozilla/5.0 (Windows NT 6.1; WOW64; rv:45.0) Gecko/20100101 Firefox/45.0
     * Chrome   User-Agent : Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 Safari/537.36
     * android  User-Agent : okhttp/3.2.0
     */
    public static final String getClientInfo(final HttpServletRequest request){
        if(request == null){
            return "";
        }
        //
        final String protocol = request.getProtocol();
        final String method = request.getMethod();
        final String userAgent = request.getHeader("User-Agent");
        final String ip = getClientIp(request);
        // final String clientHost = request.getRemoteHost();
        final Integer clientPort = request.getRemotePort();
        final String clientUser = request.getRemoteUser();
        //
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(protocol).append("---");
        stringBuilder.append(method).append("---");
        stringBuilder.append(ip).append("---");
        // stringBuilder.append(clientHost).append("---");
        stringBuilder.append(clientPort).append("---");
        stringBuilder.append(clientUser).append("---");
        stringBuilder.append(userAgent);
        String result = stringBuilder.toString();
        return result;
    }

    /**
     * 2023-07-12
     */
    public static final String getClientIp(){
        HttpServletRequest request = getRequest();
        if(request == null){
            return "127.0.0.1";
        }
        String clientIp = getClientIp(request);
        return clientIp;
    }

    /**
     * 2016-12-09 1.0.0
     * 获得客户端真实ip地址
     * 如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串ip值，究竟哪个才是真正的用户端的真实ip呢？
     * 答案是取X-Forwarded-For中第一个非unknown的有效ip字符串。如：
     * X-Forwarded-For：192.168.1.110, 192.168.1.120, 192.168.1.130, 192.168.1.100
     * 用户真实ip为： 192.168.1.110
     * 每一个Nginx的location节点都应该按照以下配置
     * location = / {
     * 		proxy_pass  http://192.168.1.100:10000;
     * 		proxy_redirect off;
     * 		#host修改为真实的域名和端口
     * 		proxy_set_header Host $http_host;
     * 		#客户端真实ip
     * 		proxy_set_header X-Real-IP $remote_addr;
     * 		proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
     * 		#客户端真实协议(http/https)
     * 		proxy_set_header X-Forwarded-Proto $scheme;
     * }
     */
    public static final String getClientIp(HttpServletRequest request){
        if(request == null){
            request = getRequest();
            if(request == null){
                return "127.0.0.1";
            }
        }
        String ip = request.getHeader("X-Real-IP");  //nginx节点
        if(p_isIpLegal(ip)){
            return ip;
        }
        //
        ip = request.getHeader("x-forwarded-for");
        if(p_isIpLegal(ip)){
            return ip;
        }
        //
        ip = request.getHeader("Proxy-Client-IP");
        if(p_isIpLegal(ip)){
            return ip;
        }
        //
        ip = request.getHeader("HTTP_CLIENT_IP");
        if(p_isIpLegal(ip)){
            return ip;
        }
        //
        ip = request.getHeader("WL-Proxy-Client-IP");
        if(p_isIpLegal(ip)){
            return ip;
        }
        //
        ip = request.getRemoteAddr();
        if(p_isIpLegal(ip)){
            return ip;
        }
        //
        if("0:0:0:0:0:0:0:1".equals(ip)){
            return "127.0.0.1";
        }
        return "127.0.0.1";
    }

    /**
     * 2019-03-18 1.0.3
     * 获取外网ip
     * https://blog.csdn.net/ichenqingyun/article/details/81676244
     */
    public static final String getServerIpOutter(){
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try{
            URL url = new URL("http://checkip.amazonaws.com");
            inputStream = url.openStream();
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            String ip = bufferedReader.readLine();
            return ip;
        }catch(Throwable throwable){
            LogUtil.error("IPUtil_getServerIpOutter_1", throwable);
            return "";
        }finally {
            IOUtil.close(bufferedReader, inputStreamReader, inputStream);
        }
    }

    /**
     * 2019-03-05 1.0.2
     * 方法1：推荐使用
     * 比较消耗时间，故使用全局变量进行本地缓存
     * see https://blog.csdn.net/gzy_0412/article/details/79916349
     */
    public static final String getServerIp() {
        if(!StringUtil.isBlank(CrabappleInnerData.serverIp)){
            return CrabappleInnerData.serverIp;
        }
        try{
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> enumeration = networkInterface.getInetAddresses();
                while (enumeration.hasMoreElements()) {
                    InetAddress inetAddress = enumeration.nextElement();
                    boolean isLinkLocalAddress = inetAddress.isLinkLocalAddress();
                    boolean isLoopbackAddress = inetAddress.isLoopbackAddress();
                    boolean isIn = inetAddress instanceof Inet4Address;
                    if (!isLinkLocalAddress && !isLoopbackAddress && isIn) {
                        String ip = inetAddress.getHostAddress();
                        CrabappleInnerData.serverIp = ip;
                        return ip;
                    }
                }
            }
        }catch(Throwable throwable){
            LogUtil.error("IPUtil_getServerIp_1", throwable);
        }
        return "127.0.0.1";
    }

    /**
     * 2019-03-05 1.0.2
     * 方法2
     * 比较消耗时间，故使用全局变量进行本地缓存
     * see https://blog.csdn.net/gzy_0412/article/details/79916349
     */
    public static final String getServerIp2() {
        if(!StringUtil.isBlank(CrabappleInnerData.serverIp)){
            return CrabappleInnerData.serverIp;
        }
        try{
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            for (; nis.hasMoreElements();) {
                NetworkInterface networkInterface = nis.nextElement();
                Enumeration<InetAddress> ias = networkInterface.getInetAddresses();
                for (; ias.hasMoreElements();) {
                    InetAddress inetAddress = ias.nextElement();
                    boolean isIn = inetAddress instanceof Inet4Address;
                    String ip = inetAddress.getHostAddress();
                    boolean isEquals = ip.equals("127.0.0.1");
                    if (isIn && !isEquals) {
                        CrabappleInnerData.serverIp = ip;
                        return ip;
                    }
                }
            }
        }catch(Throwable throwable){
            LogUtil.error("IPUtil_getServerIp2_1", throwable);
        }
        return "127.0.0.1";
    }

    /**
     * 2016-12-09 1.0.0
     * 获取当前服务器ip地址，此方法仅适用于windows系统
     * 比较消耗时间，故使用全局变量进行本地缓存
     */
    public static final String getServerIpWindows(){
        if(!StringUtil.isBlank(CrabappleInnerData.serverIp)){
            return CrabappleInnerData.serverIp;
        }
        try{
            InetAddress inetAddress = InetAddress.getLocalHost();  //获取的是本地的ip地址 //PC-20140317PXKX/192.168.0.121
            String ip = inetAddress.getHostAddress();  //192.168.0.121
            CrabappleInnerData.serverIp = ip;
            return ip;
        }catch(Throwable throwable){
            LogUtil.error("IPUtil_getServerIpWindows_1", throwable);
        }
        return null;
    }

    /**
     * ip地址是否合法
     * @param ip
     */
    private static final boolean p_isIpLegal(String ip){
        if(StringUtil.isBlank(ip)){
            return false;
        }
        ip = ip.toLowerCase();
        if("unknown".equals(ip)){
            return false;
        }
        if("0:0:0:0:0:0:0:1".equals(ip)){
            return false;
        }
        // 2023-09-04 判断是否为合法的ip语法
        boolean flag = RegexUtil.isIp(ip);
        return flag;
    }

    /**
     * 2023-05-17 随机获取一个标准的IPv4，格式如 117.70.41.218
     * 构造IP池
     * https://wen.baidu.com/question/1806978855962267707.html
     */
    public static final String getIPv4(){
        Integer aaa = 1;
        while(true){
            aaa = RandomUtil.getInt(1, 254);
            if(aaa != 127){
                break;
            }
        }
        //
        Integer bbb = RandomUtil.getInt(0, 254);
        Integer ccc = RandomUtil.getInt(0, 254);
        Integer ddd = RandomUtil.getInt(1, 254);
        final StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(aaa).append(".").append(bbb).append(".").append(ccc).append(".").append(ddd);
        String ip = stringBuffer.toString();
        return ip;
    }

    /**
     * 2024-08-07
     * @param fileName
     */
    public static final void setContentDisposition(final String fileName){
        HttpServletResponse response = getResponse();
        setContentDisposition(response, fileName);
    }

    /**
     * 2024-08-07
     * @param response
     * @param fileName
     */
    public static final void setContentDisposition(HttpServletResponse response, String fileName){
        if(response == null){
            return;
        }
        if(StringUtil.isBlank(fileName)){
            return;
        }
        // 2025-08-22 禅道需求-109 数据字典添加特殊字符匹配规则需求
        fileName = FileUtil.pureFileName(fileName);
        //
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/octet-stream");
        // 在一个运行的应用程序中，file.encoding的值只有一个，并且值为入口函数所在类文件的保存编码的值。
        String fileEncoding = System.getProperty("file.encoding");
        fileEncoding = StringUtil.toLowerCase(fileEncoding);
        if(StringUtil.equals(fileEncoding, "gbk")){
            byte[] bytes = fileName.getBytes();
            try{
                fileName = new String(bytes, "ISO-8859-1");
            }catch (Throwable throwable){
                fileName = UriEncoderUtil.encodeURIComponent(fileName);
            }
        }else{
            fileName = UriEncoderUtil.encodeURIComponent(fileName);
        }
        //
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
    }

}