package xc.crabapple.core.net.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.ObjectUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.UriEncoderUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.json.jackson.JacksonUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.net.https.HttpsUtil;
import xc.crabapple.core.servlet.ServletUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 2016-12-27 1.0.0
 * 基于http协议的网络处理工具类
 */
public final class HttpUtil{

    /**
     * 2025-02-11
     */
    public static final void init(){
        MimeTypeUtil.init();
    }

    /**
     * 2023-09-20
     * @param httpBean
     * @param <T>
     */
    public static final <T extends Object> T request(final HttpBean httpBean) throws Throwable{
        HttpServletRequest request = ServletUtil.getRequest();
        T t = request(request, httpBean);
        return t;
    }

    /**
     * 2020-12-30 7.1.0
     * @param httpBean
     * @param <T>
     */
    public static final <T extends Object> T request(final HttpServletRequest request, final HttpBean httpBean) throws Throwable{
        updateData(request, httpBean);
        try{
            T t = p_request(httpBean);
            return t;
        }catch(Throwable throwable){
            String available = httpBean.getAvailable();
            if("yes".equals(available)){
                final String callType = httpBean.getCallType();
                if("normal".equals(callType)){
                    final String hosts = httpBean.getHosts();
                    if(!StringUtil.isBlank(hosts)){
                        final String protocol = httpBean.getProtocol();
                        final String usingHost = httpBean.getUsingHost();
                        if(!StringUtil.isBlank(usingHost)){
                            final String availableName = httpBean.getAvailableName();
                            final String name = httpBean.getName();
                            LogUtil.print("HttpUtil_request_1", usingHost+"已放入黑名单，服务已不可用", protocol, hosts, name);
                            BalanceUtil.addBlack(protocol, usingHost, availableName);
                            Long requestCount = httpBean.getRequestCount();
                            requestCount++;
                            httpBean.setRequestCount(requestCount);
                            T t = request(request, httpBean);
                            return t;
                        }
                    }
                }
            }
            final String callType = httpBean.getCallType();
            if("normal".equals(callType)){
                Map<String,String> headerMap = httpBean.getHeaderMap();
                String address = httpBean.getAddress();
                String method = httpBean.getMethod();
                String usingAddress = httpBean.getUsingAddress();
                String transmission = httpBean.getTransmission();
                Long timeout = httpBean.getTimeout();
                String boolUrlEncoder = httpBean.getBoolUrlEncoder();
                LogUtil.error("HttpUtil_request_t1", throwable, headerMap, address, usingAddress, method, transmission, timeout, boolUrlEncoder);
                throw throwable;
            }
            return null;
        }
    }

    public static final void updateData(final HttpServletRequest request, final HttpBean httpBean){
        if(!StringUtil.isBlank(httpBean.getHosts())){
            String usingAddress = BalanceUtil.getAddress(request, httpBean.getProtocol(), httpBean.getHosts(), httpBean.getName(), httpBean.getAddress(), httpBean.getAlgorithm(), httpBean.getAvailable(), httpBean.getCallType());
            httpBean.setUsingAddress(usingAddress);
            String usingHost = BalanceUtil.getHostByAddress(usingAddress, httpBean.getName());
            httpBean.setUsingHost(usingHost);
            return;
        }
        String usingAddress = httpBean.getAddress();
        httpBean.setUsingAddress(usingAddress);
    }

    /**
     * 2020-10-23 6.8.0
     * 单次请求
     * http请求统一方法，包括返回字符串或文件流
     * p_protocol_method_transmission_response
     */
    private static final <T extends Object> T p_request(final HttpBean httpBean) throws Throwable{
        final String protocol = httpBean.getProtocol();
        final String hosts = httpBean.getHosts();
        final String name = httpBean.getName();
        final String usingHost = httpBean.getUsingHost();
        final String address = httpBean.getAddress();
        final String callType = httpBean.getCallType();
        final String usingAddress = httpBean.getUsingAddress();
        //正常调用
        if("normal".equals(callType)) {
            if(!StringUtil.isBlank(hosts)){
                if(StringUtil.isBlank(usingHost)) {
                    final String location = "xc_crabapple_HttpUtil_p_request_1";
                    LogUtil.error(location, "无可用的host", protocol, hosts, name, address, usingAddress);
                    throw new Throwable(location);
                }
            }else{
                if(StringUtil.isBlank(usingAddress)){
                    final String location = "xc_crabapple_HttpUtil_p_request_2";
                    LogUtil.error(location, "无可用的host", protocol, hosts, name, address, usingAddress);
                    throw new Throwable(location);
                }
            }
        }else if("xc_crabapple_available_scan".equals(callType)){
            if(StringUtil.isBlank(usingAddress)){
                final String location = "xc_crabapple_HttpUtil_p_request_3";
                LogUtil.error(location, "无可用的host", protocol, address, usingAddress);
                throw new Throwable(location);
            }
        }
        //
        Long requestCount = httpBean.getRequestCount();
        if(ObjectUtil.gts(requestCount, 1L)){
            LogUtil.print("HttpUtil_p_request_10", "有可用的host, 准备重连", protocol, hosts, name, address, usingAddress);
        }
        //
        final String method = httpBean.getMethod();
        final String transmission = httpBean.getTransmission();
        final String requestType = httpBean.getRequestType();
        final String responseType = httpBean.getResponseType();
        //
        if("http".equals(protocol)){
            if("file".equals(requestType)){
                if("post".equals(method)){
                    String result = uploadfileByPost(httpBean);
                    T t = (T) result;
                    return t;
                }
                if("put".equals(method)){
                    String result = uploadfileByPut(httpBean);
                    T t = (T) result;
                    return t;
                }
            }
            if("post".equals(method)){
                if("form".equals(transmission)){
                    if("str".equals(responseType)){
                        String result = http_post_form_str(httpBean);
                        T t = (T) result;
                        return t;
                    }
                    if("file".equals(responseType)){
                        String result = http_post_form_file(httpBean);
                        T t = (T) result;
                        return t;
                    }
                    if("in".equals(responseType)){
                        InputStream inputStream = http_post_form_in(httpBean);
                        T t = (T) inputStream;
                        return t;
                    }
                    if("out".equals(responseType)){
                        String result = http_post_form_out(httpBean);
                        T t = (T) result;
                        return t;
                    }
                }
                if("json".equals(transmission)){
                    if("str".equals(responseType)){
                        String result = http_post_json_str(httpBean);
                        T t = (T) result;
                        return t;
                    }
                    if("file".equals(responseType)){
                        String targetUrl = http_post_json_file(httpBean);
                        T t = (T) targetUrl;
                        return t;
                    }
                }
            }
            if("get".equals(method)){
                if("form".equals(transmission)){
                    if("str".equals(responseType)){
                        String result = http_get_form_str(httpBean);
                        T t = (T) result;
                        return t;
                    }
                    if("file".equals(responseType)){
                        String result = http_get_form_file(httpBean);
                        T t = (T) result;
                        return t;
                    }
                    if("in".equals(responseType)){
                        InputStream inputStream = http_get_form_in(httpBean);
                        T t = (T) inputStream;
                        return t;
                    }
                    if("out".equals(responseType)){
                        String result = http_get_form_out(httpBean);
                        T t = (T) result;
                        return t;
                    }
                }
            }
        }
        //
        final String location = "HttpUtil_p_request_100";
        LogUtil.error(location, address, usingAddress, protocol, hosts, name);
        throw new Throwable(location);
    }

    /**
     * 2019-12-02 2.1.0
     * 表单传参形式请求
     */
    public static final String http_get_form_out(final HttpBean httpBean) throws Throwable{
        OutputStream outputStream = httpBean.getOutputStream();
        InputStream inputStream = null;
        try{
            inputStream = http_get_form_in(httpBean);
            IOUtil.inputstream2outputstream(inputStream, outputStream);
            return "success";
        }catch(Throwable throwable){
            String usingAddress = httpBean.getUsingAddress();
            LogUtil.error("HttpUtil_http_get_form_out_t1", throwable, usingAddress);
            throw throwable;
        }finally {
            IOUtil.close(inputStream);
        }
    }

    /**
     * 2019-12-02 2.1.0
     * 表单传参形式请求
     */
    public static final InputStream http_get_form_in(final HttpBean httpBean) throws Throwable{
        String usingAddress = httpBean.getUsingAddress();
        String boolUrlEncoder = httpBean.getBoolUrlEncoder();
        Long timeout = httpBean.getTimeout();
        Map<String, Object> paramMap = httpBean.getParamMap();
        Map<String, String> headerMap = httpBean.getHeaderMap();
        //
        Set<String> set = paramMap.keySet();
        Iterator<String> iterator = set.iterator();
        StringBuilder sparamsBuilder = new StringBuilder();
        int i = 0;
        while(iterator.hasNext()){
            String key = iterator.next();
            Object value = paramMap.get(key);
            if(value == null){
                value = "";
            }
            if("yes".equals(boolUrlEncoder)){
                value = UriEncoderUtil.encodeURIComponent(value.toString());
            }
            if(i > 0){
                sparamsBuilder.append("&");
            }
            sparamsBuilder.append(key).append("=").append(value);
            i++;
        }
        String sparams = sparamsBuilder.toString();
        if(!"".equals(sparams)){
            if (usingAddress.indexOf("?") == -1) {
                usingAddress += "?" + sparams;
            }else{
                usingAddress += "&" + sparams;
            }
        }
        //
        try{
            URL url = new URL(usingAddress);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("accept", "*/*");
            urlConnection.setRequestProperty("connection", "Keep-Alive");
            urlConnection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            Set<String> headerSet = headerMap.keySet();
            Iterator<String> headerIterator = headerSet.iterator();
            while(headerIterator.hasNext()){
                String key = headerIterator.next();
                Object value = headerMap.get(key);
                if(value == null){
                    continue;
                }
                String vvv = value.toString();
                urlConnection.setRequestProperty(key, vvv);
            }
            int timeoutInt = timeout.intValue();
            urlConnection.setConnectTimeout(timeoutInt);
            urlConnection.setReadTimeout(timeoutInt);
            urlConnection.connect();
            InputStream inputStream = urlConnection.getInputStream();
            return inputStream;
        }catch(Throwable throwable){
            LogUtil.error("HttpUtil_http_get_form_in_t1", throwable, usingAddress, timeout);
            throw throwable;
        }
    }

    /**
     * 2019-12-02 2.1.0
     * 表单传参形式请求
     */
    public static final String http_post_form_out(final HttpBean httpBean){
        OutputStream outputStream = httpBean.getOutputStream();
        InputStream inputStream = null;
        try{
            inputStream = http_post_form_in(httpBean);
            if(inputStream == null){
                return "error";
            }
            IOUtil.inputstream2outputstream(inputStream, outputStream);
            return "success";
        }catch(Throwable throwable){
            String usingAddress = httpBean.getUsingAddress();
            LogUtil.error("HttpUtil_http_post_form_out_t1", throwable, usingAddress);
            return null;
        }finally {
            IOUtil.close(inputStream);
        }
    }

    /**
     * 2016-12-27 1.0.0 实现返回字符
     * 2019-12-02 2.1.0 实现返回输入流
     * 表单传参形式请求
     * 同步请求，向指定URL发送POST方法的请求
     * @return 所代表远程资源的响应结果
     */
    public static final InputStream http_post_form_in(final HttpBean httpBean){
        String usingAddress = httpBean.getUsingAddress();
        String boolUrlEncoder = httpBean.getBoolUrlEncoder();
        Long timeout = httpBean.getTimeout();
        Map<String, String> headerMap = httpBean.getHeaderMap();
        Map<String, Object> paramMap = httpBean.getParamMap();
        OutputStream outputStream = null;
        PrintWriter printWriter = null;
        //设置参数
        Set<String> paramSet = paramMap.keySet();
        Iterator<String> paramIterator = paramSet.iterator();
        StringBuilder sparamsBuilder = new StringBuilder();
        int count = 0;
        while (paramIterator.hasNext()) {
            String key = paramIterator.next();
            Object value = paramMap.get(key);
            if (value == null) {
                value = "";
            }
            if ("yes".equals(boolUrlEncoder)) {
                value = UriEncoderUtil.encodeURIComponent(value.toString());
            }
            if (count > 0) {
                sparamsBuilder.append("&");
            }
            sparamsBuilder.append(key).append("=").append(value);
            count++;
        }
        String sparams = sparamsBuilder.toString();
        //
        try {
            URL url = new URL(usingAddress);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("accept", "*/*");
            urlConnection.setRequestProperty("Accept-Charset", "UTF-8");
            urlConnection.setRequestProperty("contentType", "UTF-8");
            urlConnection.setRequestProperty("connection", "Keep-Alive");
            urlConnection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            //设置header
            Set<String> headerSet = headerMap.keySet();
            Iterator<String> headerIterator = headerSet.iterator();
            while (headerIterator.hasNext()) {
                String key = headerIterator.next();
                Object value = headerMap.get(key);
                if (value == null) {
                    continue;
                }
                String vvv = value.toString();
                urlConnection.setRequestProperty(key, vvv);
            }
            //发送POST请求必须设置如下两行
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            int timeoutInt = timeout.intValue();
            urlConnection.setConnectTimeout(timeoutInt);  //超时时间
            urlConnection.setReadTimeout(timeoutInt);
            outputStream = urlConnection.getOutputStream();
            printWriter = new PrintWriter(outputStream);
            printWriter.print(sparams);  //发送请求参数
            printWriter.flush();
            InputStream inputStream = urlConnection.getInputStream();
            return inputStream;
        }catch(Throwable throwable){
            LogUtil.error("HttpUtil_http_post_form_in_t1", throwable, usingAddress, boolUrlEncoder, timeout);
            return null;
        }finally {
            IOUtil.close(printWriter, outputStream);
        }
    }

    /**
     * 2016-12-27 1.0.0
     * 2019-08-07 1.2.3
     * 2019-12-02 2.1.0
     * 表单传参形式请求
     * 同步请求，向指定URL发送GET方法的请求
     * 注：参数和返回值均为字符形式
     * @param httpBean
     * @return String 所代表远程资源的响应结果
     */
    public static final String http_get_form_str(final HttpBean httpBean) throws Throwable{
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try{
            inputStream = http_get_form_in(httpBean);
            if(inputStream == null){
                return "xc_crabapple_error_p_get2str_1";
            }
            inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while((line = bufferedReader.readLine()) != null){
                stringBuilder.append(line);
            }
            String result = stringBuilder.toString();
            return result;
        }catch(Throwable throwable){
            String usingAddress = httpBean.getUsingAddress();
            LogUtil.error("HttpUtil_http_get_form_str_t1", throwable, usingAddress);
            throw throwable;
        }finally {
            IOUtil.close(bufferedReader, inputStreamReader, inputStream);
        }
    }

    /**
     * 2019-12-02 2.1.0
     * 表单传参形式请求
     * 同步请求，向指定URL发送POST方法的请求
     * 注：参数和返回值均为字符形式
     * @return 所代表远程资源的响应结果
     */
    public static final String http_post_form_str(final HttpBean httpBean){
        String protocol = httpBean.getProtocol();
        if("https".equals(protocol)){
//            String results = postByHttps();
//            return results;
        }
        InputStream inputStream = http_post_form_in(httpBean);
        if(inputStream == null){
            return "xc_crabapple_error_p_post2str_1";
        }
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try{
            inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            bufferedReader = new BufferedReader(inputStreamReader);
            String line = "";
            StringBuilder stringBuilder = new StringBuilder();
            while((line = bufferedReader.readLine()) != null){
                stringBuilder.append(line);
            }
            String result = stringBuilder.toString();
            return result;
        }catch(Throwable throwable){
            String usingAddress = httpBean.getUsingAddress();
            LogUtil.error("HttpUtil_http_post_form_str_t1", throwable, usingAddress, protocol);
            return null;
        }finally {
            IOUtil.close(bufferedReader, inputStreamReader, inputStream);
        }
    }

    /**
     * 2019-12-14 2.3.0
     * json传参形式请求-post方式
     */
    public static final String http_post_json_str(final HttpBean httpBean) throws Throwable{
        String usingAddress = httpBean.getUsingAddress();
        Map<String, String> headerMap = httpBean.getHeaderMap();
        Map<String, Object> paramMap = httpBean.getParamMap();
        //设置参数
        String paramJson = "{}";
        Object object = paramMap.get(HttpBean.XC_CRABAPPLE_ONLY_USE_VALUE);
        if(object != null){
            paramJson = (String) object;
        }else{
            paramJson = JacksonUtil.obj2json(paramMap);
        }
        final Long timeout = httpBean.getTimeout();
        //
        CloseableHttpClient closeableHttpClient = null;
        CloseableHttpResponse closeableHttpResponse = null;
        try{
            closeableHttpClient = HttpsUtil.getHttpClient();
            HttpPost httpPost = new HttpPost(usingAddress);
            Timeout timeoutUnit = Timeout.of(timeout, TimeUnit.MILLISECONDS);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(timeoutUnit)
                    .setConnectionRequestTimeout(timeoutUnit)
                    .setResponseTimeout(timeoutUnit)
                    .build();
            httpPost.setConfig(requestConfig);
            Charset charset = ContentType.getCharset(ContentType.APPLICATION_JSON, StandardCharsets.UTF_8);
            StringEntity stringEntity = new StringEntity(paramJson, charset);
            //设置header
            Set<String> headerSet = headerMap.keySet();
            Iterator<String> iterator = headerSet.iterator();
            while(iterator.hasNext()){
                String key = iterator.next();
                Object value = headerMap.get(key);
                String vvv = value.toString();
                httpPost.setHeader(key, vvv);
            }
            httpPost.addHeader("Content-Type", ContentTypeUtil.application_json);
            //
            httpPost.setEntity(stringEntity);
            closeableHttpResponse = closeableHttpClient.execute(httpPost);
            if(closeableHttpResponse == null){
                return "";
            }
            HttpEntity httpEntity = closeableHttpResponse.getEntity();
            if(httpEntity == null){
                return "";
            }
            String result = EntityUtils.toString(httpEntity, "UTF-8");
            return result;
        }catch(Throwable throwable){
            LogUtil.error("HttpUtil_http_post_json_str_t1", throwable, usingAddress, timeout);
            throw throwable;
        }finally {
            IOUtil.close(closeableHttpResponse, closeableHttpClient);
        }
    }

    /**
     * 2020-11-10 6.9.0
     */
    public static final String http_post_json_file(final HttpBean httpBean) throws Throwable{
        String targetUrl = httpBean.getTargetUrl();
        FileUtil.fileDelete(targetUrl);
        String path = UrlUtil.getPathByUrl(targetUrl);
        FileUtil.dirMake(path);
        String usingAddress = httpBean.getUsingAddress();
        Map<String, Object> paramMap = httpBean.getParamMap();
        String paramJson = "{}";
        Object object = paramMap.get(HttpBean.XC_CRABAPPLE_ONLY_USE_VALUE);
        if(object != null){
            paramJson = (String) object;
        }else{
            paramJson = JacksonUtil.obj2json(paramMap);
        }
        Map<String, String> headerMap = httpBean.getHeaderMap();
        final Long timeout = httpBean.getTimeout();
        //
        CloseableHttpClient closeableHttpClient = null;
        CloseableHttpResponse closeableHttpResponse = null;
        try{
            closeableHttpClient = HttpsUtil.getHttpClient();
            HttpPost httpPost = new HttpPost(usingAddress);
            Timeout timeoutUnit = Timeout.of(timeout, TimeUnit.MILLISECONDS);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(timeoutUnit)
                    .setResponseTimeout(timeoutUnit)
                    .build();
            httpPost.setConfig(requestConfig);
            Charset charset = ContentType.getCharset(ContentType.APPLICATION_JSON, StandardCharsets.UTF_8);
            StringEntity stringEntity = new StringEntity(paramJson, charset);
            Set<String> headerSet = headerMap.keySet();
            Iterator<String> iterator = headerSet.iterator();
            while(iterator.hasNext()){
                String key = iterator.next();
                Object value = headerMap.get(key);
                String vvv = value.toString();
                httpPost.setHeader(key, vvv);
            }
            httpPost.setEntity(stringEntity);
            closeableHttpResponse = closeableHttpClient.execute(httpPost);
            int statusCode = closeableHttpResponse.getCode();
            if(statusCode == 200){
                HttpEntity entity = closeableHttpResponse.getEntity();
                byte[] bytes = EntityUtils.toByteArray(entity);
                IOUtil.bytes2file(bytes, targetUrl);
            }
            return targetUrl;
        }catch(Throwable throwable){
            LogUtil.error("HttpUtil_http_post_json_file_t1", throwable, usingAddress, targetUrl, path);
            throw throwable;
        }finally {
            IOUtil.close(closeableHttpResponse, closeableHttpClient);
        }
    }

    /**
     * 2018-08-10 1.0.0
     * 2019-12-02 2.1.0
     * 表单传参形式请求
     * 远程文件下载
     */
    public static final String http_get_form_file(final HttpBean httpBean) throws Throwable{
        String targetUrl = httpBean.getTargetUrl();
        if(StringUtil.isBlank(targetUrl)){
            final String location = "HttpUtil_http_get_form_file_1";
            LogUtil.error(location, "targetUrl为空");
            throw new Throwable(location + ", targetUrl为空");
        }
        boolean flag = FileUtil.fileNameIsLegal(targetUrl);
        if(!flag){
            return "xc_crabapple_error_p_get2file_1";
        }
        String path = UrlUtil.getPathByLevel(targetUrl, 1);
        FileUtil.dirMake(path);
        InputStream inputStream = http_get_form_in(httpBean);
        OutputStream outputStream = null;
        try{
            outputStream = new FileOutputStream(targetUrl);
            IOUtil.inputstream2outputstream(inputStream, outputStream);
            outputStream.flush();
            return "xc_crabapple_success";
        }catch(Throwable throwable){
            String usingAddress = httpBean.getUsingAddress();
            LogUtil.error("HttpUtil_http_get_form_file_t1", throwable, usingAddress, targetUrl);
            throw throwable;
        }finally {
            IOUtil.close(outputStream, inputStream);
        }
    }

    /**
     * 2019-12-02 2.1.0
     * 表单传参形式请求
     */
    public static final String http_post_form_file(final HttpBean httpBean) throws Throwable{
        String targetUrl = httpBean.getTargetUrl();
        boolean flag = FileUtil.fileNameIsLegal(targetUrl);
        if(!flag){
            return "xc_crabapple_error_p_form_post2file_1";
        }
        String path = UrlUtil.getPathByLevel(targetUrl, 1);
        FileUtil.dirMake(path);
        InputStream inputStream = http_get_form_in(httpBean);
        if(inputStream == null){
            return "xc_crabapple_error_p_form_post2file_2";
        }
        OutputStream outputStream = null;
        try{
            outputStream = new FileOutputStream(targetUrl);
            IOUtil.inputstream2outputstream(inputStream, outputStream);
            outputStream.flush();
            return "xc_crabapple_success";
        }catch(Throwable throwable){
            String usingAddress = httpBean.getUsingAddress();
            LogUtil.error("HttpUtil_http_post_form_file_t1", throwable, usingAddress, targetUrl, path);
            throw throwable;
        }finally {
            IOUtil.close(outputStream, inputStream);
        }
    }

    /**
     * 2023-09-02
     * @param address
     * @return
     */
    public static final String requestByPost(final String address){
        final String result = requestByPost(address, null, null, null);
        return result;
    }

    /**
     * 2023-09-02
     * @param address
     * @param paramMap
     * @return
     */
    public static final String requestByPost(final String address, final Map<String, Object> paramMap){
        final String result = requestByPost(address, null, null, paramMap);
        return result;
    }

    /**
     * 2023-09-02
     * @param address
     * @param headerMap
     * @param paramMap
     * @return
     */
    public static final String requestByPost(final String address, final Map<String, String> headerMap, final Map<String, Object> paramMap){
        final String result = requestByPost(address, headerMap, null, paramMap);
        return result;
    }

    /**
     * 2023-09-02
     * @param address
     * @param headerMap
     * @param cookieMap
     * @param paramMap
     * @return
     */
    public static final String requestByPost(final String address, final Map<String, String> headerMap, final Map<String, List<String>> cookieMap, final Map<String, Object> paramMap){
        CloseableHttpClient closeableHttpClient = HttpsUtil.getHttpClient(cookieMap);
        CloseableHttpResponse closeableHttpResponse = null;
        InputStream inputStream = null;
        try {
            HttpPost httpPost = new HttpPost(address);
            // 设置业务参数
            if(!CollectionUtil.isBlank(paramMap)){
                StringBuilder stringBuilder = new StringBuilder();
                Set<String> keySet = paramMap.keySet();
                Iterator<String> iterator = keySet.iterator();
                while(iterator.hasNext()) {
                    final String key = iterator.next();
                    final String value = (String) paramMap.get(key);
                    if(stringBuilder.length() > 0){
                        stringBuilder.append("&");
                    }
                    stringBuilder.append(key).append("=").append(value);
                }
                String params = stringBuilder.toString();
                Charset charset = ContentType.getCharset(ContentType.APPLICATION_JSON, StandardCharsets.UTF_8);
                StringEntity stringEntity = new StringEntity(params, charset);
                httpPost.setEntity(stringEntity);
            }
            // 设置请求头header
            if(!CollectionUtil.isBlank(headerMap)){
                headerMap.remove("if-none-match");
                headerMap.remove("if-modified-since");
                headerMap.remove("content-length");
                Set<String> keySet = headerMap.keySet();
                Iterator<String> iterator = keySet.iterator();
                while(iterator.hasNext()) {
                    final String key = iterator.next();
                    final String value = headerMap.get(key);
                    httpPost.setHeader(key, value);
                }
                headerMap.put("Content-Type", "application/x-www-form-urlencoded");
            }
            closeableHttpResponse = closeableHttpClient.execute(httpPost);
            //就是判断返回结果是否符合预期
            int statusCode = closeableHttpResponse.getCode();
            if (statusCode == 200){
                HttpEntity httpEntity = closeableHttpResponse.getEntity();
                inputStream = httpEntity.getContent();
                // String result = EntityUtils.toString(httpEntity,"utf-8");
                String result = IOUtil.inputstream2str(inputStream);
                return result;
            }
            LogUtil.errorNo("HttpUtil_requestByPost_1", "调用失败", address, statusCode);
            return "";
        }catch(Throwable throwable){
            LogUtil.error("HttpUtil_requestByPost_t1", throwable, "调用异常", address);
            return "";
        }finally {
            IOUtil.close(inputStream, closeableHttpResponse, closeableHttpClient);
        }
    }

    /**
     * 2023-09-02
     * @param address
     * @return
     */
    public static final String requestByGet(final String address){
        final String result = requestByGet(address, null, null, null);
        return result;
    }

    /**
     * 2023-09-02
     * @param address
     * @param paramMap
     * @return
     */
    public static final String requestByGet(final String address, final Map<String, Object> paramMap){
        final String result = requestByGet(address, null, null, paramMap);
        return result;
    }

    /**
     * 2023-09-02
     * @param address
     * @param headerMap
     * @param paramMap
     * @return
     */
    public static final String requestByGet(final String address, final Map<String, String> headerMap, final Map<String, Object> paramMap){
        final String result = requestByGet(address, headerMap, null, paramMap);
        return result;
    }

    /**
     * 2023-09-02
     * @param address
     * @param headerMap
     * @param cookieMap
     * @param paramMap
     * @return
     */
    public static final String requestByGet(final String address, final Map<String, String> headerMap, final Map<String, List<String>> cookieMap, final Map<String, Object> paramMap){
        CloseableHttpClient closeableHttpClient = HttpsUtil.getHttpClient(cookieMap);
        CloseableHttpResponse closeableHttpResponse = null;
        InputStream inputStream = null;
        String localAddress = address;
        try {
            // 设置业务参数
            if(!CollectionUtil.isBlank(paramMap)){
                StringBuilder stringBuilder = new StringBuilder();
                Set<String> keySet = paramMap.keySet();
                Iterator<String> iterator = keySet.iterator();
                while(iterator.hasNext()) {
                    final String key = iterator.next();
                    final String value = (String) paramMap.get(key);
                    if(stringBuilder.length() > 0){
                        stringBuilder.append("&");
                    }
                    stringBuilder.append(key).append("=").append(value);
                }
                final String params = stringBuilder.toString();
                if(StringUtil.containOfOne(localAddress, "?")){
                    localAddress += "&" + params;
                }else{
                    localAddress += "?" + params;
                }
            }
            HttpGet httpGet = new HttpGet(localAddress);
            // 设置请求头header
            if(!CollectionUtil.isBlank(headerMap)){
                headerMap.remove("if-none-match");
                headerMap.remove("if-modified-since");
                headerMap.remove("content-length");
                Set<String> keySet = headerMap.keySet();
                Iterator<String> iterator = keySet.iterator();
                while(iterator.hasNext()) {
                    final String key = iterator.next();
                    final String value = headerMap.get(key);
                    httpGet.setHeader(key, value);
                }
                headerMap.put("Content-Type", "application/x-www-form-urlencoded");
            }
            //声明一个client对象，用来进行方法的执行,并设置cookies信息
            //执行post的方法并得到响应结果
            closeableHttpResponse = closeableHttpClient.execute(httpGet);
            //就是判断返回结果是否符合预期
            int statusCode = closeableHttpResponse.getCode();
            if (statusCode == 200){
                HttpEntity httpEntity = closeableHttpResponse.getEntity();
                inputStream = httpEntity.getContent();
                // String result = EntityUtils.toString(httpEntity,"utf-8");
                String result = IOUtil.inputstream2str(inputStream);
                return result;
            }
            LogUtil.errorNo("HttpUtil_requestByGet_1", "调用失败", localAddress, statusCode);
            return "";
        }catch(Throwable throwable){
            LogUtil.error("HttpUtil_requestByGet_t1", throwable, "调用异常", localAddress);
            return "";
        }finally {
            IOUtil.close(inputStream, closeableHttpResponse, closeableHttpClient);
        }
    }

    /**
     * 2023-08-30
     * 文件上传
     * @param address
     * @param fileUrl 文件磁盘路径地址
     */
    public static final String uploadfileByPost(final String address, final String fileUrl) {
        final File file = new File(fileUrl);
        final String result = uploadfileByPost(address, null, null, file);
        return result;
    }

    /**
     * 2023-08-30
     * 文件上传
     * @param address
     * @param headerMap
     * @param fileUrl 文件磁盘路径地址
     */
    public static final String uploadfileByPost(final String address, final Map<String, String> headerMap, final String fileUrl) {
        final File file = new File(fileUrl);
        final String result = uploadfileByPost(address, headerMap, null, file);
        return result;
    }

    /**
     * 2023-08-30
     * 文件上传
     * @param address
     * @param headerMap
     * @param paramMap
     * @param fileUrl 文件磁盘路径地址
     */
    public static final String uploadfileByPost(final String address, final Map<String, String> headerMap, final Map<String, Object> paramMap, final String fileUrl) {
        final File file = new File(fileUrl);
        final String result = uploadfileByPost(address, headerMap, paramMap, file);
        return result;
    }

    /**
     * 2023-08-30
     * 文件上传
     * @param address
     * @param file
     */
    public static final String uploadfileByPost(final String address, final File file) {
        final String result = uploadfileByPost(address, null, null, file);
        return result;
    }

    /**
     * 2023-08-30
     * 文件上传
     * @param address
     * @param headerMap
     * @param file
     */
    public static final String uploadfileByPost(final String address, final Map<String, String> headerMap, final File file) {
        final String result = uploadfileByPost(address, headerMap, null, file);
        return result;
    }

    /**
     * 2023-08-30
     * 文件上传
     * @param httpBean
     */
    public static final String uploadfileByPost(final HttpBean httpBean){
        final String address = httpBean.getAddress();
        final Map<String, String> headerMap = httpBean.getHeaderMap();
        final Map<String, Object> paramMap = httpBean.getParamMap();
        File fromFile = httpBean.getFromFile();
        if(fromFile == null){
            final String fromUrl = httpBean.getFromUrl();
            fromFile = new File(fromUrl);
        }
        final String result = uploadfileByPost(address, headerMap, paramMap, fromFile);
        return result;
    }

    /**
     * 2023-08-30
     * 文件上传
     * @param address
     * @param headerMap
     * @param paramMap
     * @param file
     */
    public static final String uploadfileByPost(final String address, final Map<String, String> headerMap, final Map<String, Object> paramMap, final File file) {
        if(file == null){
            return "";
        }
        if(!file.exists()){
            return "";
        }
        if(!file.isFile()){
            return "";
        }
        //
        final Charset charset = Charset.forName("UTF-8");
        HttpPost httpPost = new HttpPost(address);
        if(!CollectionUtil.isBlank(headerMap)) {
            Set<String> keySet = headerMap.keySet();
            Iterator<String> iterator = keySet.iterator();
            while(iterator.hasNext()){
                String key = iterator.next();
                String value = headerMap.get(key);
                httpPost.setHeader(key, value);
            }
        }
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        multipartEntityBuilder.setCharset(charset);  // 设置请求的编码格式
        FileBody fileBody = new FileBody(file);  // 把文件转换成流对象FileBody
        multipartEntityBuilder.addPart("file", fileBody);
        if(!CollectionUtil.isBlank(paramMap)){
            Set<Map.Entry<String, Object>> entrySet = paramMap.entrySet();
            for (Map.Entry<String, Object> requestParam : entrySet) {
                final String key = requestParam.getKey();
                final String value = (String) requestParam.getValue();
                multipartEntityBuilder.addTextBody(key, value);
            }
        }
        HttpEntity httpEntity = multipartEntityBuilder.build();  // 生成POST实体
        httpPost.setEntity(httpEntity);
        CloseableHttpClient closeableHttpClient = null;
        CloseableHttpResponse closeableHttpResponse = null;
        InputStream inputStream = null;
        //
        try {
            closeableHttpClient = HttpsUtil.getHttpClient();
            closeableHttpResponse = closeableHttpClient.execute(httpPost);
            HttpEntity resultHttpEntity = closeableHttpResponse.getEntity();
            inputStream = resultHttpEntity.getContent();
            final String result = IOUtil.inputstream2str(inputStream, charset);
            return result;
        }catch(Throwable throwable){
            LogUtil.error("HttpUtil_uploadfileByPost_t1", throwable, address);
            return "";
        }finally {
            IOUtil.close(inputStream, closeableHttpResponse, closeableHttpClient);
        }
    }

    /**
     * 2023-08-30
     * 文件上传
     * @param address
     * @param fileUrl 文件磁盘路径地址
     */
    public static final String uploadfileByPut(final String address, final String fileUrl) {
        final File file = new File(fileUrl);
        final String result = uploadfileByPut(address, null, null, file);
        return result;
    }

    /**
     * 2023-08-30
     * 文件上传
     * @param address
     * @param headerMap
     * @param fileUrl 文件磁盘路径地址
     */
    public static final String uploadfileByPut(final String address, final Map<String, String> headerMap, final String fileUrl) {
        final File file = new File(fileUrl);
        final String result = uploadfileByPut(address, headerMap, null, file);
        return result;
    }

    /**
     * 2023-08-30
     * 文件上传
     * @param address
     * @param headerMap
     * @param paramMap
     * @param fileUrl 文件磁盘路径地址
     */
    public static final String uploadfileByPut(final String address, final Map<String, String> headerMap, final Map<String, Object> paramMap, final String fileUrl) {
        final File file = new File(fileUrl);
        final String result = uploadfileByPut(address, headerMap, paramMap, file);
        return result;
    }

    /**
     * 2023-08-30
     * 文件上传
     * @param address
     * @param file
     */
    public static final String uploadfileByPut(final String address, final File file) {
        final String result = uploadfileByPut(address, null, null, file);
        return result;
    }

    /**
     * 2023-08-30
     * 文件上传
     * @param address
     * @param headerMap
     * @param file
     */
    public static final String uploadfileByPut(final String address, final Map<String, String> headerMap, final File file) {
        final String result = uploadfileByPut(address, headerMap, null, file);
        return result;
    }

    /**
     * 2023-08-30
     * 文件上传
     * @param httpBean
     */
    public static final String uploadfileByPut(final HttpBean httpBean){
        final String address = httpBean.getAddress();
        final Map<String, String> headerMap = httpBean.getHeaderMap();
        final Map<String, Object> paramMap = httpBean.getParamMap();
        File fromFile = httpBean.getFromFile();
        if(fromFile == null){
            final String fromUrl = httpBean.getFromUrl();
            fromFile = new File(fromUrl);
        }
        final String result = uploadfileByPut(address, headerMap, paramMap, fromFile);
        return result;
    }

    /**
     * 2023-08-30
     * 文件上传
     * @param address
     * @param headerMap
     * @param paramMap
     * @param file
     */
    public static final String uploadfileByPut(final String address, final Map<String, String> headerMap, final Map<String, Object> paramMap, final File file) {
        if(file == null){
            return "";
        }
        if(!file.exists()){
            return "";
        }
        if(!file.isFile()){
            return "";
        }
        //
        final Charset charset = Charset.forName("UTF-8");
        final HttpPut httpPut = new HttpPut(address);
        if(!CollectionUtil.isBlank(headerMap)) {
            Set<String> keySet = headerMap.keySet();
            Iterator<String> iterator = keySet.iterator();
            while(iterator.hasNext()){
                String key = iterator.next();
                String value = headerMap.get(key);
                httpPut.setHeader(key, value);
            }
        }
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        multipartEntityBuilder = multipartEntityBuilder.setCharset(charset);
        multipartEntityBuilder.addBinaryBody("file", file);
        if(!CollectionUtil.isBlank(paramMap)){
            Set<Map.Entry<String, Object>> entrySet = paramMap.entrySet();
            for (Map.Entry<String, Object> requestParam : entrySet) {
                final String key = requestParam.getKey();
                final String value = (String) requestParam.getValue();
                multipartEntityBuilder.addTextBody(key, value);
            }
        }
        HttpEntity httpEntity = multipartEntityBuilder.build();
        httpPut.setEntity(httpEntity);
        CloseableHttpClient closeableHttpClient = null;
        CloseableHttpResponse closeableHttpResponse = null;
        InputStream inputStream = null;
        //
        try{
            closeableHttpClient = HttpsUtil.getHttpClient();
            closeableHttpResponse = closeableHttpClient.execute(httpPut);
            HttpEntity resultHttpEntity = closeableHttpResponse.getEntity();
            inputStream = resultHttpEntity.getContent();
            final String result = IOUtil.inputstream2str(inputStream, charset);
            return result;
        }catch(Throwable throwable){
            LogUtil.error("HttpUtil_uploadfileByPut_t1", throwable, address);
            return "";
        }finally{
            IOUtil.close(inputStream, closeableHttpResponse, closeableHttpClient);
        }
    }

    /**
     * 2025-01-03
     * 从项目迁移而来
     * GET请求
     * @param address url地址
     * @param params 参数结构为map。
     */
    public static final String getFromUrl(final String address, final Map<String, String> params){
        String rtn = getFromUrl(address, params, "utf-8");
        return rtn;
    }

    /**
     * 2025-01-03
     * 从项目迁移而来
     * @param address
     * @param params
     * @param charset
     * @return
     */
    public static final String getFromUrl(final String address, final Map<String, String> params, final String charset){
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;
        InputStream instream = null;
        try {
            String effectiveCharset = charset;
            if(StringUtil.isBlank(effectiveCharset)){
                effectiveCharset = "utf-8";
            }
            httpclient = HttpsUtil.getHttpClient();
            URIBuilder uriBuilder = new URIBuilder(address);
            uriBuilder.setCharset(Charset.forName(effectiveCharset));
            if(params !=  null){
                Iterator<String> keyIt = params.keySet().iterator();
                while(keyIt.hasNext()){
                    String key = keyIt.next();
                    String val = params.get(key);
                    uriBuilder.setParameter(key, val);
                }
            }
            URI uri = uriBuilder.build();
            HttpGet httpget = new HttpGet(uri);
            response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if(entity != null){
                instream = entity.getContent();
                StringWriter writer = new StringWriter();
                IOUtils.copy(instream, writer, effectiveCharset);
                return writer.toString();
            }
        }catch(Throwable throwable){
            LogUtil.error("HttpUtil_getFromUrl_t3", throwable, address);
            return null;
        }finally{
            IOUtil.close(instream, response, httpclient);
        }
        return null;
    }

    /**
     * 提交Json参数
     */
    public static final String postJson(final String address, final String jsonParams){
        String result = postJson(address, jsonParams, "utf-8");
        return result;
    }

    public static final String postJson(final String address, final String jsonParams, final String charset2){
        Charset charset = ContentType.getCharset(ContentType.APPLICATION_JSON, StandardCharsets.UTF_8);
        StringEntity stringEntity = new StringEntity(jsonParams, charset);  // 解决中文乱码问题
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-type", "application/json; charset=utf-8");
        String result = post(address, stringEntity, headers);
        return result;
    }

    /**
     * 发送请求。
     * @param address			address
     * @param httpEntity		请求数据
     * @param headerMap		请求HEADER MAP.
     */
    public static final String post(final String address, final HttpEntity httpEntity, final Map<String,String> headerMap){
        HttpPost httpPost = new HttpPost(address);
        httpPost.setEntity(httpEntity);
        if(!CollectionUtil.isBlank( headerMap)){
            for (Map.Entry<String, String> ent : headerMap.entrySet()){
                httpPost.addHeader(ent.getKey(),ent.getValue());
            }
        }
        CloseableHttpClient closeableHttpClient = HttpsUtil.getHttpClient();
        CloseableHttpResponse closeableHttpResponse = null;
        InputStream inputStream = null;
        DataInputStream dataInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        java.io.ByteArrayOutputStream byteArrayOutputStream = null;
        try{
            closeableHttpResponse = closeableHttpClient.execute(httpPost);
            HttpEntity httpEntity2 = closeableHttpResponse.getEntity();
            inputStream = httpEntity2.getContent();
            dataInputStream = new DataInputStream(inputStream);
            bufferedInputStream = new BufferedInputStream(dataInputStream);
            byteArrayOutputStream = new java.io.ByteArrayOutputStream();
            byte[] byteArray = new byte[1024];
            int tmp = 0;
            while((tmp = bufferedInputStream.read(byteArray)) !=  -1){
                byteArrayOutputStream.write(byteArray, 0, tmp);
            }
            final String result = byteArrayOutputStream.toString("UTF-8");
            return result;
        }catch(Throwable throwable){
            LogUtil.error("HttpUtil_post_t5", throwable, address);
        }finally{
            IOUtil.close(byteArrayOutputStream, bufferedInputStream, dataInputStream, inputStream, closeableHttpResponse, closeableHttpClient);
        }
        return null;
    }

    public static final List<String> postFromUrl(final String address, final Map<String, String> reqHeaders, final String content) {
        List<String> list = postFromUrl(address, reqHeaders, content, "utf-8");
        return list;
    }

    public static final List<String> postFromUrl(final String address, final Map<String, String> reqHeaders, final String content, final String charset) {
        HttpPost httpPost = new HttpPost(address);
        Charset charset2 = ContentType.getCharset(ContentType.APPLICATION_JSON, StandardCharsets.UTF_8);
        httpPost.setEntity(new StringEntity(content, charset2));
        List<String> list = sendRequest(httpPost, reqHeaders);
        return list;
    }

    /**
     * 向服务端发送请求并接收返回的数据。
     */
    public static final List<String> postFromUrl(final String address, final Map<String, String> reqHeaders, final Map<String, String> params){
        List<String> list = postFromUrl(address, reqHeaders, params, "utf-8");
        return list;
    }

    public static final List<String> postFromUrl(final String address, final Map<String, String> reqHeaders, final Map<String, String> params, final String charset2){
        HttpPost httpPost = new HttpPost(address);
        if(!CollectionUtil.isBlank(params)){
            List<NameValuePair> nvps = new ArrayList<>();
            Iterator<String> keyIt = params.keySet().iterator();
            while(keyIt.hasNext()){
                String key = keyIt.next();
                String val = params.get(key);
                nvps.add(new BasicNameValuePair(key, val));
            }
            Charset charset = ContentType.getCharset(ContentType.APPLICATION_JSON, StandardCharsets.UTF_8);
            try{
                UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(nvps, charset);
                httpPost.setEntity(urlEncodedFormEntity);
            }catch(Throwable throwable){
                LogUtil.error("HttpUtil_postFromUrl_t8", throwable, address);
                return null;
            }
        }
        List<String> list = sendRequest(httpPost, reqHeaders);
        return list;
    }

    /**
     * Post参数至指定URL，并且返回响应值
     * @param address		address
     * @param params	map参数
     */
    public static final String postFromUrl(final String address, final Map<String, String> params){
        List<NameValuePair> list = new ArrayList<>();
        Iterator<String> keyIt = params.keySet().iterator();
        while(keyIt.hasNext()){
            String key = keyIt.next();
            String val = params.get(key);
            list.add(new BasicNameValuePair(key, val));
        }
        try{
            UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(list);
            String result = post(address, urlEncodedFormEntity, null);
            return result;
        }catch(Throwable throwable){
            LogUtil.error("HttpUtil_getFromUrl_t4", throwable, address);
            return null;
        }
    }

    /**
     * 执行http请求
     */
    private static final List<String> sendRequest(final HttpUriRequestBase httpUriRequestBase, final Map<String, String> reqHeaders){
        if(!CollectionUtil.isBlank(reqHeaders)){
            for (Map.Entry<String, String> ent : reqHeaders.entrySet()){
                httpUriRequestBase.addHeader(ent.getKey(),ent.getValue());
            }
        }
        List<String> list = new ArrayList<>();
        CloseableHttpClient closeableHttpClient  = HttpsUtil.getHttpClient();
        CloseableHttpResponse closeableHttpResponse = null;
        InputStream inputStream = null;
        try{
            closeableHttpResponse = closeableHttpClient.execute(httpUriRequestBase);
            Integer statusCode = closeableHttpResponse.getCode();
            String statusCodeStr = statusCode.toString();
            list.add(statusCodeStr);
            HttpEntity httpEntity = closeableHttpResponse.getEntity();
            if(httpEntity != null){
                inputStream = httpEntity.getContent();
                StringWriter stringWriter = new StringWriter();
                Charset charset = ContentType.getCharset(ContentType.APPLICATION_JSON, StandardCharsets.UTF_8);
                IOUtils.copy(inputStream, stringWriter, charset);
                String result = stringWriter.toString();
                list.add(result);
            }
            return list;
        }catch(Throwable throwable){
            LogUtil.error("HttpUtil_sendRequest_t10", throwable);
            return null;
        }finally{
            IOUtil.close(inputStream, closeableHttpResponse, closeableHttpClient);
        }
    }

    /**
     * 发送删除请求。
     */
    public static final List<String> delFromUrl(final String address, final Map<String,String> reqHeaders){
        List<String> list = delFromUrl(address, reqHeaders, "utf-8");
        return list;
    }

    public static final List<String> delFromUrl(final String address, final Map<String,String> reqHeaders, final String charset){
        HttpDelete httpDel = new HttpDelete(address);
        List<String> list = sendRequest(httpDel, reqHeaders);
        return list;
    }

    /**
     * 2024-08-07 返回值改为boolean类型
     * 2025-08-19 方法重载
     * 下载文件
     * @param address
     */
    public static final boolean download(final String address){
        final boolean success = download(address, null);
        return success;
    }

    /**
     * 2024-08-07 返回值改为boolean类型
     * 2025-08-19 方法重载，兼容下载到本地与原始调用方
     * 下载文件
     */
    public static final boolean download(final String address, final String targetUrl){
        HttpServletResponse response = ServletUtil.getResponse();
        CloseableHttpClient closeableHttpClient = HttpsUtil.getHttpClient();
        CloseableHttpResponse httpResponse = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        PrintWriter printWriter = null;
        try{
            HttpGet httpGet = new HttpGet(address);
            httpResponse = closeableHttpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            Header[] contentTypes = httpResponse.getHeaders("Content-Type");
            if(CollectionUtil.isBlank(contentTypes)) {
                inputStream = httpEntity.getContent();
                final String dataFromCloud = IOUtil.inputstream2str(inputStream);
                printWriter = response.getWriter();
                printWriter.write(dataFromCloud);
                final String requestURI = ServletUtil.getRequestURI();
                LogUtil.error("HttpUtil_download_11", "云盘下载失败, contentTypes为空", requestURI, address, "来自云盘服务的数据："+dataFromCloud);
                return false;
            }
            Header head = contentTypes[0];
            if(CollectionUtil.isBlank(head)) {
                inputStream = httpEntity.getContent();
                final String dataFromCloud = IOUtil.inputstream2str(inputStream);
                printWriter = response.getWriter();
                printWriter.write(dataFromCloud);
                final String requestURI = ServletUtil.getRequestURI();
                LogUtil.error("HttpUtil_download_22", "云盘下载失败, head为空", requestURI, address, "来自云盘服务的数据："+dataFromCloud);
                return false;
            }
            final String contentType = head.getValue();
            final String type = MimeTypeUtil.getTypePure("ani");
            if(StringUtil.isBlank(type) || !StringUtil.containOf(contentType, type)){
                inputStream = httpEntity.getContent();
                final String dataFromCloud = IOUtil.inputstream2str(inputStream);
                printWriter = response.getWriter();
                printWriter.write(dataFromCloud);
                final String requestURI = ServletUtil.getRequestURI();
                LogUtil.print("HttpUtil_download_33", "云盘下载失败", dataFromCloud, requestURI, address, contentType, type);
                return false;
            }
            inputStream = httpEntity.getContent();
            long length = httpEntity.getContentLength();
            if(length <= 0){
                final String dataFromCloud = IOUtil.inputstream2str(inputStream);
                printWriter = response.getWriter();
                printWriter.write(dataFromCloud);
                final String requestURI = ServletUtil.getRequestURI();
                LogUtil.error("HttpUtil_download_44", "云盘下载失败, 文件长度等于0", requestURI, address, type, length);
                return false;
            }
            // 下载到本地
            String localTargetUrl = UrlUtil.getStandardPath(targetUrl);
            if(!StringUtil.isBlank(localTargetUrl)){
                IOUtil.inputstream2disk(inputStream, localTargetUrl);
                return true;
            }
            // 下载到浏览器或原始调用方
            outputStream = response.getOutputStream();
            byte[] buffer = new byte[4096];
            int readLength = 0;
            while((readLength = inputStream.read(buffer)) > 0){
                outputStream.write(buffer, 0, readLength);
            }
            return true;
        }catch(Throwable throwable){
            final String requestURI = ServletUtil.getRequestURI();
            LogUtil.error("HttpUtil_download_t66", throwable, requestURI, address);
            return false;
        }finally{
            IOUtil.close(printWriter, outputStream, inputStream, httpResponse, closeableHttpClient);
        }
    }

    public static final List<String> getFromUrlHreader(final String address, final Map<String, String> reqHeaders){
        final List<String> list = getFromUrlHreader(address, reqHeaders, "utf-8");
        return list;
    }

    public static final List<String> getFromUrlHreader(final String address, final Map<String, String> reqHeaders, final String charset){
        final HttpGet httpget = new HttpGet(address);
        final List<String> list = sendRequest(httpget, reqHeaders);
        return list;
    }

    public static final void addCookie(final CookieStore cookieStore, final String name, final String value, final String domain, final String path, final boolean secure){
        if(StringUtil.isBlank(name)){
            return;
        }
        BasicClientCookie basicClientCookie = new BasicClientCookie(name, value);
        basicClientCookie.setVersion(0);
        basicClientCookie.setDomain(domain);  // 设置范围
        basicClientCookie.setPath(path);
        basicClientCookie.setSecure(secure);
        cookieStore.addCookie(basicClientCookie);
    }

}