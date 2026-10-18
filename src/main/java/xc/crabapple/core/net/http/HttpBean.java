package xc.crabapple.core.net.http;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.data.bean.BeanBase;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 2019-08-07 1.2.3
 */
public final class HttpBean extends BeanBase {

    public static final String XC_CRABAPPLE_ONLY_USE_VALUE = "xc_crabapple_only_use_value";
    // 网络协议，http / https
    private String protocol = "http";
    // 请求方法，post / get / put
    private String method = "post";
    // 负载均衡算法 random round hash
    private String algorithm = "";
    // 负载均衡设置, 优先级高于address, ip:port,ip:port, 格式如192.168.1.1:8000,192.168.1.2:8000
    private String hosts = "";
    // 负载均衡设置, 接口名称，格式如/api/ms/fs/uploader
    private String name = "";
    // 高可用，正在使用的真正完整标准单独的host，格式如192.168.1.1:8080/ 或者 http://192.168.1.1:8080/
    private String usingHost = "";
    // 发送请求的完整URL，优先级低于hosts, 格式如192.168.1.1:8080/abc 或者 http://192.168.1.1:8080/abc
    private String address = "";
    // 高可用，正在使用的真正完整标准单独的地址，格式如192.168.1.1:8080/abc 或者 http://192.168.1.1:8080/abc
    private String usingAddress = "";
    // 是否使用高可用，yes-使用高可用，no-不使用高可用
    private String available = "yes";
    // 高可用
    private String availableName = "/xc_crabapple/alpha/available";
    // 传参类型， form / json, form-表单形式传参，json-json形式传参
    private String transmission = "form";
    // 请求参数，（当且仅当transmission为json时，key为xc_crabapple_only_use_value则只取value值）
    private Map<String, Object> paramMap;
    // 请求类型，normal / file, normal-普通传参，file-文件上传
    private String requestType = "normal";
    // 待上传的本地文件绝对路径，只有当requestType为file时有效
    private String fromUrl;
    // 待上传的本地文件对象，只有当requestType为file时有效
    private File fromFile;
    // 响应类型，str / file / in / out, str-字符串，file-下载流文件, in-inputStream, out-outputStream
    private String responseType = "str";
    // 下载到本地的文件的绝对路径，只有当responseType为file时有效
    private String targetUrl;
    // 只有当responseType为in时有效
    private InputStream inputStream;
    // 只有当responseType为out时有效
    private OutputStream outputStream;
    // 是否对地址编码, yes/no, 只对value进行编码
    private String boolUrlEncoder = "yes";
    // 请求超时时间，单位毫秒，若小于等于0表示不设置，执行默认
    private Long timeout = 3600000L;
    // HTTP协议请求头
    private Map<String, String> headerMap;
    // normal-外部正常调用 xc_crabapple_available_scan-内部检测
    private String callType = "normal";
    private Long requestCount = 1L;  //请求次数

    public HttpBean(){
        if(StringUtil.isBlank(algorithm) || (!"random".equals(algorithm) && !"round".equals(algorithm) && !"hash".equals(algorithm))){
            algorithm = "hash";
        }
        if(StringUtil.isBlank(availableName)){
            availableName = "/xc_crabapple/alpha/available";
        }
        //
        paramMap = new HashMap<>();
        headerMap = new HashMap<>();
        headerMap.put("Referer", "127.0.0.1");  // 2023-08-29 add
    }

    public String getBoolUrlEncoder() {
        return boolUrlEncoder;
    }

    public void setBoolUrlEncoder(final String boolUrlEncoder) {
        this.boolUrlEncoder = boolUrlEncoder;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(final Long timeout) {
        if(timeout == null || timeout <= 0){
            return;
        }
        long time = timeout.longValue();
        if(time > Integer.MAX_VALUE){
            this.timeout = Long.valueOf(Integer.MAX_VALUE);
            return;
        }
        this.timeout = timeout;
    }

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(final String transmission) {
        this.transmission = transmission;
    }

    public Map<String, String> getHeaderMap() {
        return headerMap;
    }

    public void setHeaderMap(final Map<String, String> headerMap){
        // 2023-08-29 add
        if(!headerMap.containsKey("Referer")){
            headerMap.put("Referer", "127.0.0.1");
        }
        this.headerMap = headerMap;
    }

    public String getHeader(final String key){
        String value = headerMap.get(key);
        return value;
    }

    public void setHeader(final String key, final String value){
        headerMap.put(key, value);
    }

    public Map<String, Object> getParamMap(){
        return paramMap;
    }

    public void setParamMap(final Map<String, Object> paramMap) {
        this.paramMap = paramMap;
    }

    public Object getParam(final String key) {
        Object value = paramMap.get(key);
        return value;
    }

    public void setParam(final String key, final Object value) {
        paramMap.put(key, value);
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(final String method) {
        this.method = method;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(final String requestType) {
        this.requestType = requestType;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(final String responseType) {
        this.responseType = responseType;
    }

    public String getFromUrl() {
        return fromUrl;
    }

    public void setFromUrl(final String fromUrl) {
        this.fromUrl = fromUrl;
    }

    public File getFromFile() {
        return fromFile;
    }

    public void setFromFile(final File fromFile) {
        this.fromFile = fromFile;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(final String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(final InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(final OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(final String algorithm) {
        this.algorithm = algorithm;
    }

    public String getHosts() {
        return hosts;
    }

    public void setHosts(final String hosts) {
        available = "yes";
        this.hosts = hosts;
    }

    public String getUsingHost(){
        return usingHost;
    }

    public void setUsingHost(final String usingHost) {
        this.usingHost = usingHost;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(final String address) {
        available = "no";
        this.address = address;
        usingAddress = address;
    }

    public String getUsingAddress() {
        return usingAddress;
    }

    public void setUsingAddress(final String usingAddress) {
        this.usingAddress = usingAddress;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(final String callType) {
        this.callType = callType;
    }

    public String getAvailable() {
        return available;
    }

    public void setAvailable(final String available) {
        this.available = available;
    }

    public String getAvailableName() {
        return availableName;
    }

    public void setAvailableName(final String availableName) {
        this.availableName = availableName;
    }

    public Long getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(final Long requestCount) {
        this.requestCount = requestCount;
    }

}