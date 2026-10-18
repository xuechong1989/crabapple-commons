package xc.crabapple.ssm.spring;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.UriEncoderUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.net.http.ContentTypeUtil;
import xc.crabapple.core.servlet.ServletUtil;
import xc.crabapple.ssm.uploader.UploaderUtil;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * 2024-07-25
 * 支持反复读取参数
 */
public final class SsmRequestWrapper extends HttpServletRequestWrapper {

    public String requestURI = "";
    private Map<String, String[]> p_parameterMap = null;
    private byte[] p_bytes = null;  // 缓存输入流对象
    private String p_uploader = "no";
    private String status = "no";

    public SsmRequestWrapper(final HttpServletRequest request, final String multiPartEnable){
        super(request);
        setRequestResponse(request, null, multiPartEnable);
    }

    public SsmRequestWrapper(final HttpServletRequest request, final HttpServletResponse response){
        super(request);
        setRequestResponse(request, response, "no");
    }

    /**
     * 重载构造方法
     */
    public SsmRequestWrapper(final HttpServletRequest request, final Map<String, Object> extendParams) {
        super(request);
        setRequestResponse(request, null, "no");
        // 这里将扩展参数写入参数表
        addAllParameters(extendParams);
    }

    /**
     * 2025-01-15
     * @param request 请求头信息
     * @param response
     */
    public void setRequestResponse(final HttpServletRequest request, final HttpServletResponse response, final String multiPartEnable) {
        SpringUtil.setRequestResponse(request, response);
        //
        requestURI = ServletUtil.getRequestURI("do");
        if(CollectionUtil.isBlank(p_parameterMap)){
            p_parameterMap = new HashMap<>(16);
        }
        // 添加客户端json形式传参
        Map<String, Object> jsonParamMap = ServletUtil.getJsonParamMap(request);
        if(!CollectionUtil.isBlank(jsonParamMap)){
            Set<String> set2 = jsonParamMap.keySet();
            Iterator<String> iterator2 = set2.iterator();
            while(iterator2.hasNext()){
                String key = iterator2.next();
                Object value = jsonParamMap.get(key);
                addParameter(key, value);
            }
        }
        // form表单形式传参
        Map<String, String[]> parameterMap = request.getParameterMap();
        if(!CollectionUtil.isBlank(parameterMap)){
            Set<String> parameterMapSet = parameterMap.keySet();
            Iterator<String> iterator = parameterMapSet.iterator();
            while (iterator.hasNext()){
                String key = iterator.next();
                String[] valueArray = parameterMap.get(key);
                p_parameterMap.put(key, valueArray);
            }
            p_map2bytes(true);
        }
        //
        SpringUtil.setRequestResponse(this, response);
        // 更新为上传类型的request对象
        if("yes".equals(multiPartEnable)){
            final boolean isUploader = UploaderUtil.setMultipartHttpServletRequest(this);
            if(isUploader){
                p_uploader = "yes";
            }
        }
    }

    public String getUploader(){
        return p_uploader;
    }

    public void setUploader(final String value){
        p_uploader = value;
    }

    /**
     * 2024-08-06
     */
    @Override
    public String getContentType(){
        String contentType = super.getContentType();
        contentType = ContentTypeUtil.getContentType(contentType);
        return contentType;
    }

    @Override
    public String getHeader(final String name) {
        try{
            final String value = super.getHeader(name);
            if (value == null) {
                return null;
            }
            return value;
        }catch (Throwable throwable){
            // LogUtil.error("SsmRequestWrapper_getHeader_t1", throwable, name);
            return null;
        }
    }

    /**
     * 在获取所有的参数名,必须重写此方法，否则对象中参数值映射不上
     */
    @Override
    public Enumeration<String> getParameterNames() {
        Set<String> set = p_parameterMap.keySet();
        Vector vector = new Vector<>(set);
        Enumeration<String> enumeration = vector.elements();
        return enumeration;
    }

    /**
     * 重写getParameter方法
     * @param name 参数名
     * @return 返回参数值
     */
    @Override
    public String getParameter(final String name) {
        String[] valueArray = p_parameterMap.get(name);
        if(CollectionUtil.isBlank(valueArray)){
            return null;
        }
        String value = valueArray[0];
        if(value == null){
            return null;
        }
        return value;
    }

    @Override
    public String[] getParameterValues(final String name) {
        String[] valueArray = p_parameterMap.get(name);
        if (valueArray == null || valueArray.length == 0) {
            return null;
        }
        return valueArray;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return p_parameterMap;
    }

    @Override
    public ServletInputStream getInputStream() {
        if(CollectionUtil.isBlank(p_bytes)){
            return null;
        }
        ServletInputStream servletInputStream = IOUtil.bytes2servletInputStream(p_bytes);
        return servletInputStream;
    }

    @Override
    public String getRequestURI(){
        return requestURI;
    }

    public void setBytes(final String str) {
        p_bytes = StringUtil.str2bytes(str);
        p_bytes2map();
    }

    /**
     * 增加多个参数
     * @param map 增加的多个参数
     */
    public void addAllParameters(final Map<String, Object> map) {
        if (CollectionUtil.isBlank(map)) {
            return;
        }
        Set<String> set = map.keySet();
        Iterator<String> iterator = set.iterator();
        while(iterator.hasNext()){
            String key = iterator.next();
            Object valueObject = map.get(key);
            addParameter(key, valueObject);
        }
    }

    /**
     * 增加参数
     * @param name  参数名
     * @param valueObject 参数值
     */
    public void addParameter(final String name, final Object valueObject) {
        try{
            if (valueObject == null) {
                return;
            }
            //
            if(valueObject instanceof byte[]){
                byte[] bytes = (byte[]) valueObject;
                String valueStr = new String(bytes, "UTF-8");
                String[] array = {valueStr};
                p_parameterMap.put(name, array);
                p_map2bytes(true);
                return;
            }
            //
            if (valueObject instanceof String[]) {
                String[] array = (String[]) valueObject;
                p_parameterMap.put(name, array);
                p_map2bytes(true);
                return;
            }
            //
            if (valueObject instanceof Object[]) {
                Object[] array = (Object[]) valueObject;
                if(!CollectionUtil.isBlank(array)){
                    Object value = array[0];
                    if(value != null){
                        String valueStr = value.toString();
                        String[] arr = {valueStr};
                        p_parameterMap.put(name, arr);
                        p_map2bytes(true);
                        return;
                    }
                }
                p_parameterMap.put(name, null);
                p_map2bytes(true);
                return;
            }
            //
            String valueStr = valueObject.toString();
            String[] array = {valueStr};
            p_parameterMap.put(name, array);
            p_map2bytes(true);
        }catch (Throwable throwable){
            LogUtil.error("SsmRequestWrapper_addParameter_t1", throwable, name, valueObject);
        }
    }

    /**
     * 刪除参数
     */
    public void removeParameter(final String name) {
        if (StringUtil.isBlank(name)) {
            return;
        }
        p_parameterMap.remove(name);
        p_map2bytes(true);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * 2025-02-18
     * 对当前request对象对应的参数进行uri编码
     * @param useCustomFlag 是否使用自定义编码方式
     */
    public void encode(final boolean useCustomFlag){
        if(CollectionUtil.isBlank(p_parameterMap)){
            return;
        }
        //
        Map<String, String[]> map = new HashMap<>();
        Set<String> set = p_parameterMap.keySet();
        Iterator<String> iterator = set.iterator();
        while(iterator.hasNext()){
            String key = iterator.next();
            String value = "";
            String[] valueArray = p_parameterMap.get(key);
            if(!CollectionUtil.isBlank(valueArray)){
                value = valueArray[0];
                if(value != null){
                    value = UriEncoderUtil.encodeURIComponent(value, useCustomFlag);
                }
            }
            String[] newValueArray = {value};
            map.put(key, newValueArray);
        }
        Set<String> set2 = map.keySet();
        Iterator<String> iterator2 = set2.iterator();
        while(iterator2.hasNext()){
            String key = iterator2.next();
            String[] valueArray = map.get(key);
            p_parameterMap.put(key, valueArray);
        }
        //
        p_map2bytes(false);
    }

    /**
     * 2025-02-18
     */
    private void p_bytes2map(){
        p_parameterMap.clear();
        //
        if(CollectionUtil.isBlank(p_bytes)){
            return;
        }
        //
        String str = StringUtil.bytes2str(p_bytes);
        if(StringUtil.containOf(str, "&")){
            String[] array = StringUtil.split(str, "&");
            for(int i = 0; i < array.length; i++){
                String kv = array[i];
                if(StringUtil.containOf(kv, "=")){
                    String[] arr = StringUtil.split(kv, "=");
                    String key = arr[0];
                    String value = arr[1];
                    String[] valueArray = {value};
                    p_parameterMap.put(key, valueArray);
                }
            }
            return;
        }
        if(StringUtil.containOf(str, "=")){
            String[] arr = StringUtil.split(str, "=");
            String key = arr[0];
            String value = arr[1];
            String[] valueArray = {value};
            p_parameterMap.put(key, valueArray);
        }
    }

    /**
     * 2025-02-18
     */
    private void p_map2bytes(final boolean decode){
        p_bytes = new byte[0];
        //
        if(CollectionUtil.isBlank(p_parameterMap)){
            return;
        }
        // 解码
        if(decode){
            Map<String, String[]> map = new HashMap<>();
            Set<String> set = p_parameterMap.keySet();
            Iterator<String> iterator = set.iterator();
            while(iterator.hasNext()){
                String key = iterator.next();
                String value = "";
                String[] valueArray = p_parameterMap.get(key);
                if(!CollectionUtil.isBlank(valueArray)){
                    value = valueArray[0];
                    if(value != null){
                        value = UriEncoderUtil.decodeURIComponent(value, true);
                    }
                }
                String[] newValueArray = {value};
                map.put(key, newValueArray);
            }
            Set<String> set2 = map.keySet();
            Iterator<String> iterator2 = set2.iterator();
            while(iterator2.hasNext()){
                String key = iterator2.next();
                String[] valueArray = map.get(key);
                p_parameterMap.put(key, valueArray);
            }
        }
        // 更新p_bytes
        StringBuilder stringBuilder = new StringBuilder();
        Set<String> set2 = p_parameterMap.keySet();
        Iterator<String> iterator2 = set2.iterator();
        while(iterator2.hasNext()){
            String key = iterator2.next();
            String value = "";
            String[] valueArray = p_parameterMap.get(key);
            if(!CollectionUtil.isBlank(valueArray)){
                value = valueArray[0];
            }
            String kv = key + "=" + value;
            if(stringBuilder.length() == 0){
                stringBuilder.append(kv);
                continue;
            }
            stringBuilder.append("&").append(kv);
        }
        if(stringBuilder.length() > 0){
            String str = stringBuilder.toString();
            p_bytes = StringUtil.str2bytes(str);
        }
    }

}
