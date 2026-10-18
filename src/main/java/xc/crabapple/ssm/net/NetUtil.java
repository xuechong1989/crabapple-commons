package xc.crabapple.ssm.net;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.UriEncoderUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.net.http.HttpBean;
import xc.crabapple.core.net.http.HttpUtil;
import xc.crabapple.core.net.http.MimeTypeUtil;
import xc.crabapple.core.net.https.HttpsUtil;
import xc.crabapple.core.servlet.ServletUtil;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 2025-01-15
 * 2025-01-27 代码整理
 * 网络工具类，支持http与https网络请求
 */
public final class NetUtil {

    /**
     * 2025-02-10
     */
    public static final void init(){
        HttpUtil.init();
    }

    public static final String post_form_str(final HttpBean httpBean, final String cerPath) {
        final String protocol = httpBean.getProtocol();
        if ("http".equals(protocol)) {
            final String result = HttpUtil.http_post_form_str(httpBean);
            return result;
        }
        if ("https".equals(protocol)) {
            final String result = HttpsUtil.p_https_post_form_str(httpBean, cerPath);
            return result;
        }
        //
        final String address = httpBean.getAddress();
        LogUtil.error("NetUtil_post_form_str_1", "不支持的网络协议", protocol, address, cerPath);
        return null;
    }

    public static final String post_form_str(final HttpBean httpBean) {
        final String result = post_form_str(httpBean,"");
        return result;
    }

    public static final String requestByPost(final String address, final Map<String, String> headerMap, final Map<String, List<String>> cookieMap, final Map<String, Object> paramMap) {
        final String result = requestByPost(address, headerMap,  cookieMap, paramMap, "");
        return result;
    }

    public static final String requestByPost(final String address, final Map<String, String> headerMap, final Map<String, List<String>> cookieMap, final Map<String, Object> paramMap, final String cerPath) {
        // 设置cookie
        CookieStore cookieStore = new BasicCookieStore();
        if (!CollectionUtil.isBlank(cookieMap)) {
            Set<String> keySet = cookieMap.keySet();
            Iterator<String> iterator = keySet.iterator();
            while (iterator.hasNext()) {
                final String name = iterator.next();
                final List<String> vvvList = cookieMap.get(name);
                String value = vvvList.get(0);
                String domain = vvvList.get(1);
                String path = vvvList.get(2);
                HttpUtil.addCookie(cookieStore, name, value, domain, path, false);
            }
        }
        // 2023-11-21 添加超时时间配置，单位毫秒
        final Integer timeout = Integer.MAX_VALUE;
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        requestConfigBuilder.setConnectTimeout(timeout);
        requestConfigBuilder.setConnectionRequestTimeout(timeout);
        requestConfigBuilder.setSocketTimeout(timeout);
        RequestConfig requestConfig = requestConfigBuilder.build();
        // 2026-10-17 安全默认：未指定证书时使用JVM信任库与标准主机名校验；指定证书（自签名/内网）时跳过主机名校验
        SSLContext sslContext = HttpsUtil.p_getSSLContext(cerPath);
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);
        httpClientBuilder.setDefaultCookieStore(cookieStore);
        httpClientBuilder.setSSLContext(sslContext);
        if(!StringUtil.isBlank(cerPath)){
            httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        }
        CloseableHttpClient closeableHttpClient = httpClientBuilder.build();
        CloseableHttpResponse closeableHttpResponse = null;
        InputStream inputStream = null;
        try {
            HttpPost httpPost = new HttpPost(address);
            // 设置业务参数
            if (!CollectionUtil.isBlank(paramMap)) {
                StringBuilder stringBuilder = new StringBuilder();
                Set<String> keySet = paramMap.keySet();
                Iterator<String> iterator = keySet.iterator();
                while (iterator.hasNext()) {
                    final String key = iterator.next();
                    final String value = (String) paramMap.get(key);
                    if (stringBuilder.length() <= 0) {
                        stringBuilder.append(key + "=" + value);
                    } else {
                        stringBuilder.append("&" + key + "=" + value);
                    }
                }
                String params = stringBuilder.toString();
                StringEntity stringEntity = new StringEntity(params, "utf-8");
                httpPost.setEntity(stringEntity);
            }
            // 设置请求头header
            if (!CollectionUtil.isBlank(headerMap)) {
                headerMap.remove("if-none-match");
                headerMap.remove("if-modified-since");
                headerMap.remove("content-length");
                Set<String> keySet = headerMap.keySet();
                Iterator<String> iterator = keySet.iterator();
                while (iterator.hasNext()) {
                    final String key = iterator.next();
                    final String value = headerMap.get(key);
                    httpPost.setHeader(key, value);
                }
                headerMap.put("Content-Type", "application/x-www-form-urlencoded");
            }
            // 执行post的方法并得到响应结果
            closeableHttpResponse = closeableHttpClient.execute(httpPost);
            // 判断返回结果是否符合预期
            StatusLine statusLine = closeableHttpResponse.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity httpEntity = closeableHttpResponse.getEntity();
                inputStream = httpEntity.getContent();
                String result = IOUtil.inputstream2str(inputStream);
                return result;
            }
            LogUtil.error("NetUtil_requestByPost_1", "调用失败", address, statusCode);
            return "";
        } catch (Throwable throwable) {
            LogUtil.error("NetUtil_requestByPost_t1", throwable, "调用异常", address);
            return "";
        } finally {
            IOUtil.close(inputStream, closeableHttpResponse, closeableHttpClient);
        }
    }

    public static final String request(final HttpBean httpBean, final String cerPath) {
        String protocol = httpBean.getProtocol();
        if (protocol.equals("https")) {
            final String result = HttpsUtil.request(httpBean, cerPath);
            return result;
        }
        try {
            final String result = HttpUtil.request(httpBean);
            return result;
        } catch (Throwable throwable) {
            LogUtil.error("NetUtil_request_t176", throwable, cerPath);
            return null;
        }
    }

    public static final String request(final HttpBean httpBean) {
        String protocol = httpBean.getProtocol();
        if (protocol.equals("https")) {
            final String result = HttpsUtil.request(httpBean, "");
            return result;
        }
        try {
            final String result = HttpUtil.request(httpBean);
            return result;
        } catch (Throwable throwable) {
            LogUtil.error("NetUtil_request_t176", throwable);
            return null;
        }
    }

    public static final void forward(final HttpServletRequest request, final HttpServletResponse response, final String fromRequestURI, final String targetAddr, final String defaultParam) {
        forward(request, response, fromRequestURI, targetAddr, defaultParam, "");
    }

    public static final void forward(final HttpServletRequest request, final HttpServletResponse response, final String fromRequestURI, final String targetAddr, final String defaultParam, final String cerPath) {
        String localTargetAddr = StringUtil.deleteAll(targetAddr, " ");
        if (StringUtil.isBlank(localTargetAddr)) {
            LogUtil.error("NetUtil_forward_1", "targetAddr为空", localTargetAddr);
            return;
        }
        if (!"/".equals(localTargetAddr)) {
            if (StringUtil.lastsWith(localTargetAddr, "/")) {
                int length = localTargetAddr.length();
                length = length - 1;
                localTargetAddr = localTargetAddr.substring(0, length);
            }
        }
        //
        String requestURI = fromRequestURI;
        if (StringUtil.isBlank(requestURI)) {
            requestURI = ServletUtil.getRequestURI();
        }
        requestURI = UriEncoderUtil.encodeURIComponent(requestURI, UriEncoderUtil.EXCLUDE_ADDRESS);
        //
        String url = "";
        String methodName = "";
        OutputStream outputStream = null;
        ServletInputStream servletInputStream = null;
        CloseableHttpResponse clientHttpResponse = null;
//        ClientHttpResponse clientHttpResponse = null;
        InputStream inputStream = null;
        ServletOutputStream servletOutputStream = null;
        CloseableHttpClient closeableHttpClient = null;
        try {
            URI uri = new URI(requestURI);
            String path = uri.getPath();
            String query = request.getQueryString();
            if (StringUtil.isBlank(query)) {
                if (StringUtil.isBlank(defaultParam)) {
                    query = "";
                } else {
                    query = defaultParam;
                }
            } else {
                query += ("&" + defaultParam);
            }
            if (StringUtil.isBlank(query)) {
                url = localTargetAddr + path;
            } else {
                // 由于无法确定query是否为已编码的值，故先尝试解码如果成功则统一编码、如果不成功说明不是解码值，直接对其编码即可。确保query只被编码一次，避免重复编码。
                try {
                    String tmpQuery = UriEncoderUtil.decodeURIComponent(query);
                    if (!StringUtil.isBlank(tmpQuery)) {
                        query = tmpQuery;
                    }
                } catch (Throwable throwable) {
                    LogUtil.error("NetUtil_forward_t224", throwable, fromRequestURI);
                }
                query = UriEncoderUtil.encodeURIComponent(query, UriEncoderUtil.EXCLUDE_PARAM);
                url = localTargetAddr + path + "?" + query;
            }
            URI newUri = new URI(url);
            methodName = request.getMethod();
            HttpMethod httpMethod = HttpMethod.valueOf(methodName);
            if (httpMethod == null) {
                LogUtil.error("NetUtil_forward_2", "httpMethod为空", requestURI, url, methodName);
                return;
            }
            // 创建HttpClient
            closeableHttpClient = p_createHttpClient(cerPath);
            HttpUriRequest httpUriRequest = p_createHttpUriRequest(httpMethod, newUri);
            // 设置请求头
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    if (headerName.equalsIgnoreCase("Content-Length")) {
                        continue;
                    }
                    Enumeration<String> enumeration = request.getHeaders(headerName);
                    if (enumeration != null) {
                        while (enumeration.hasMoreElements()) {
                            String value = enumeration.nextElement();
                            httpUriRequest.addHeader(headerName, value);
                        }
                    }
                }
            }
            // 设置请求体
            if (HttpMethod.POST.equals(httpMethod) || HttpMethod.PUT.equals(httpMethod)) {
                servletInputStream = request.getInputStream();
                StringEntity entity = new StringEntity(StreamUtils.copyToString(servletInputStream, StandardCharsets.UTF_8), "UTF-8");
                ((HttpEntityEnclosingRequestBase) httpUriRequest).setEntity(entity);
            }
            // 执行请求
            clientHttpResponse = closeableHttpClient.execute(httpUriRequest);
            HttpStatus httpStatus = HttpStatus.valueOf(clientHttpResponse.getStatusLine().getStatusCode());
            int status = httpStatus.value();
            response.setStatus(status);
            // 设置响应头
            Header[] headers = clientHttpResponse.getAllHeaders();
            for (Header header : headers) {
                String header1 = response.getHeader(header.getName());
                if (!StringUtil.isBlank(header1)) {
                    continue;
                }
                response.setHeader(header.getName(), header.getValue());
            }
            // 复制响应体
            if (clientHttpResponse.getEntity() == null) {
                return;
            }
            inputStream = clientHttpResponse.getEntity().getContent();
            servletOutputStream = response.getOutputStream();
            StreamUtils.copy(inputStream, servletOutputStream);
        } catch (Throwable throwable) {
            LogUtil.error("NetUtil_forward_t281", throwable, requestURI, url, methodName);
        } finally {
            IOUtil.close(servletOutputStream, inputStream, clientHttpResponse, servletInputStream, closeableHttpClient);
        }
    }

    /**
     * 创建HttpClient
     * @param cerPath
     * @return
     */
    private static final CloseableHttpClient p_createHttpClient(final String cerPath) {
        try {
            // 2026-10-17 安全默认：未指定证书时使用标准主机名校验；指定证书（自签名/内网）时跳过主机名校验
            final SSLContext sslContext = HttpsUtil.p_getSSLContext(cerPath);
            final SSLConnectionSocketFactory sslConnectionSocketFactory = StringUtil.isBlank(cerPath)
                    ? new SSLConnectionSocketFactory(sslContext)
                    : new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
            final HttpClientBuilder httpClientBuilder = HttpClients.custom();
            httpClientBuilder.setSSLSocketFactory(sslConnectionSocketFactory);
            final CloseableHttpClient closeableHttpClient = httpClientBuilder.build();
            return closeableHttpClient;
        } catch (Throwable throwable) {
            LogUtil.error("NetUtil_createHttpClient_t297", throwable, cerPath);
            return null;
        }
    }

    /**
     * 创建HttpUriRequest
     * @param httpMethod
     * @param uri
     * @return
     */
    private static final HttpUriRequest p_createHttpUriRequest(final HttpMethod httpMethod, final URI uri) {
        final String name = httpMethod.name();
        switch (name) {
            case "GET":
                return new HttpGet(uri);
            case "POST":
                return new HttpPost(uri);
            case "PUT":
                return new HttpPut(uri);
            case "DELETE":
                return new HttpDelete(uri);
            case "HEAD":
                return new HttpHead(uri);
            case "OPTIONS":
                return new HttpOptions(uri);
            case "TRACE":
                return new HttpTrace(uri);
            case "PATCH":
                return new HttpPatch(uri);
            default:
                return new HttpGet(uri);
        }
    }

}
