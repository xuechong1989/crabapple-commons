package xc.crabapple.core.net.https;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.UriEncoderUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.json.jackson.JacksonUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.net.http.ContentTypeUtil;
import xc.crabapple.core.net.http.HttpBean;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 2020-11-15 7.0.0
 * https请求
 */
public final class HttpsUtil {

    private static final String DEFAULT_KEYSTORE_PASSWORD = "123456";  // 仅用于内存KeyStore初始化，非真实凭据

    /**
     * 2020-11-15 7.0.0
     * https请求统一方法，包括返回字符串或文件流
     * p_protocol_method_transmission_response
     */
    public static final String request(final HttpBean httpBean){
        return request( httpBean, "");
    }
    public static final String request(final HttpBean httpBean, final String cerPath){
        String protocol = httpBean.getProtocol();
        String method = httpBean.getMethod();
        String transmission = httpBean.getTransmission();
        String response = httpBean.getResponseType();
        //
        if("https".equals(protocol)){
            if("post".equals(method)){
                if("form".equals(transmission)){
                    if("str".equals(response)){
                        String result = p_https_post_form_str(httpBean,cerPath);
                        return result;
                    }
                }
                if("json".equals(transmission)){
                    if("str".equals(response)){
                        String result = p_https_post_json_str(httpBean,cerPath);
                        return result;
                    }
                }
            }else if("get".equals(method)){
                if("form".equals(transmission)){
                    if("str".equals(response)){
                        String result = p_https_get_form_str(httpBean,cerPath);
                        return result;
                    }
                }
            }
        }
        LogUtil.error("HttpsUtil_request_1", httpBean);
        return null;
    }

    /*
     * 处理https GET/POST请求
     * 请求地址、请求方法、参数
     * */

    public static final String p_https_get_form_str(final HttpBean httpBean){
       return p_https_get_form_str(httpBean,"");
    }
    public static final String p_https_get_form_str(final HttpBean httpBean, final String cerPath){
        String usingAddress = httpBean.getUsingAddress();
        Map<String, Object> paramMap = httpBean.getParamMap();
        HttpsURLConnection httpsURLConnection = null;
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try{
            StringBuilder urlParams = new StringBuilder();
            urlParams.append(usingAddress);
            if (!CollectionUtil.isBlank(paramMap)) {
                urlParams.append("?");
                for (String key : paramMap.keySet()) {
                    String value = (String) paramMap.get(key);
                    urlParams.append(key).append("=").append(value);
                    urlParams.append("&");
                }
                String urlParamsStr = urlParams.toString();
                usingAddress = urlParamsStr.substring(0, urlParamsStr.length() - 1);
            }
            SSLSocketFactory sslSocketFactory = getSSLSocketFactory(cerPath);
            URL url = new URL(usingAddress);
            httpsURLConnection = (HttpsURLConnection) url.openConnection();
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setDoInput(true);
            httpsURLConnection.setUseCaches(false);
            httpsURLConnection.setSSLSocketFactory(sslSocketFactory);
            httpsURLConnection.connect();
            inputStream = httpsURLConnection.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream, "utf-8");
            bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            while((line = bufferedReader.readLine()) != null){
                stringBuilder.append(line);
            }
            String result = stringBuilder.toString();
            return result;
        }catch(Throwable throwable){
            LogUtil.error("HttpsUtil_p_https_get_form_str_t1", throwable, httpBean);
            return null;
        }finally{
            IOUtil.close(bufferedReader, inputStreamReader, inputStream);
            if(httpsURLConnection != null){
                httpsURLConnection.disconnect();
            }
        }
    }

    /**
     * 2020-11-15 7.0.0
     */

    private static final String p_https_post_form_str(final HttpBean httpBean)  {
       return p_https_post_form_str(httpBean,"");
    }
    public static final String p_https_post_form_str(final HttpBean httpBean, final String cerPath)  {
        String usingAddress = httpBean.getUsingAddress();
        String boolUrlEncoder = httpBean.getBoolUrlEncoder();
        Map<String, String> headerMap = httpBean.getHeaderMap();
        Map<String, Object> paramMap = httpBean.getParamMap();
        //设置参数
        Set<String> paramSet = paramMap.keySet();
        Iterator<String> paramIterator = paramSet.iterator();
        StringBuilder sparamsBuilder = new StringBuilder();
        int i = 0;
        while (paramIterator.hasNext()) {
            String key = paramIterator.next();
            Object value = paramMap.get(key);
            if (value == null) {
                value = "";
            }
            if ("yes".equals(boolUrlEncoder)) {
                try{
                    value = UriEncoderUtil.encodeURIComponent(value.toString());
                }catch(Throwable throwable){
                    LogUtil.error("HttpsUtil_p_https_post_form_str_t1", throwable, httpBean);
                    return null;
                }
            }
            if (i > 0) {
                sparamsBuilder.append("&");
            }
            sparamsBuilder.append(key).append("=").append(value);
            i++;
        }
        String sparams = sparamsBuilder.toString();
        //
        CloseableHttpClient closeableHttpClient = null;
        CloseableHttpResponse closeableHttpResponse = null;
        try{
            Charset charset = ContentType.getCharset(ContentType.APPLICATION_FORM_URLENCODED, StandardCharsets.UTF_8);
            StringEntity stringEntity = new StringEntity(sparams, charset);
            HttpPost httpPost = new HttpPost(usingAddress);
            httpPost.setEntity(stringEntity);
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
                httpPost.setHeader(key, vvv);
            }
            httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
            //
            closeableHttpClient = getHttpClient(cerPath,null);
            closeableHttpResponse = closeableHttpClient.execute(httpPost);
            if(closeableHttpResponse == null){
                return null;
            }
            HttpEntity httpEntity = closeableHttpResponse.getEntity();
            if(httpEntity == null){
                return null;
            }
            String result = EntityUtils.toString(httpEntity, "UTF-8");
            return result;
        }catch(Throwable throwable){
            LogUtil.error("HttpsUtil_p_https_post_form_str_t2", throwable, httpBean);
            return null;
        }finally{
            IOUtil.close(closeableHttpResponse, closeableHttpClient);
        }
    }

    /**
     * 2020-11-15 7.0.0
     */
    private static final String p_https_post_json_str(final HttpBean httpBean){
        return p_https_post_json_str(httpBean,"");
    }
    private static final String p_https_post_json_str(final HttpBean httpBean, final String cerPath){
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
        CloseableHttpClient closeableHttpClient = null;
        CloseableHttpResponse closeableHttpResponse = null;
        try{
            HttpPost httpPost = new HttpPost(usingAddress);
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
            httpPost.addHeader("Accept", ContentTypeUtil.application_json);
            //
            Charset charset = ContentType.getCharset(ContentType.APPLICATION_JSON, StandardCharsets.UTF_8);
            StringEntity stringEntity = new StringEntity(paramJson, charset);
            httpPost.setEntity(stringEntity);
            //
            closeableHttpClient = getHttpClient(cerPath,null);
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
            LogUtil.error("HttpsUtil_p_https_post_json_str_t1", throwable, httpBean);
            return null;
        }finally{
            IOUtil.close(closeableHttpResponse, closeableHttpClient);
        }
    }

    /**
     * 2025-01-03 重写
     * @return
     */
    public static final CloseableHttpClient getHttpClient(){
        CloseableHttpClient closeableHttpClient = getHttpClient(null, null, null, null);
        return closeableHttpClient;
    }

    /**
     * 2025-01-03 重写
     * @return
     */
    public static final CloseableHttpClient getHttpClient(final Map<String, List<String>> cookieMap){
        CloseableHttpClient closeableHttpClient = getHttpClient(null, null, null, cookieMap);
        return closeableHttpClient;
    }

    /**
     * 2020-11-15 7.0.0
     * 2025-01-03 重写
     * 2025-08-11 超时时间配置最大值
     */
    public static final CloseableHttpClient getHttpClient(final String password, final String jksUrl, final String p12Url, final Map<String, List<String>> cookieMap){
        // 2026-10-17 安全默认：未指定证书时使用JVM信任库与标准主机名校验；如需信任全部证书请使用 getTrustAllHttpClient()
        SSLContext sslContext = StringUtil.isBlank(password, jksUrl, p12Url) ? p_getDefaultSSLContext() : p_getSSLContext(password, jksUrl, p12Url);
        if(sslContext == null){
            return null;
        }
        return p_buildHttpClient(new SSLConnectionSocketFactory(sslContext), cookieMap);
    }

    /**
     * 2026-10-17
     * 获取信任全部证书的HttpClient（跳过证书与主机名校验）
     * 安全提示：存在中间人攻击风险，仅用于内网自签名证书、测试等明确场景，生产环境请使用 getHttpClient()
     */
    public static final CloseableHttpClient getTrustAllHttpClient(){
        return getTrustAllHttpClient(null);
    }

    /**
     * 2026-10-17
     * 获取信任全部证书的HttpClient（跳过证书与主机名校验），携带cookie
     * 安全提示：存在中间人攻击风险，仅用于内网自签名证书、测试等明确场景
     */
    public static final CloseableHttpClient getTrustAllHttpClient(final Map<String, List<String>> cookieMap){
        SSLContext sslContext = p_getTrustAllSSLContext();
        if(sslContext == null){
            return null;
        }
        return p_buildHttpClient(new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE), cookieMap);
    }

    /**
     * 2026-10-17
     * 构建HttpClient
     */
    private static final CloseableHttpClient p_buildHttpClient(final SSLConnectionSocketFactory sslConnectionSocketFactory, final Map<String, List<String>> cookieMap){
        try{
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", new PlainConnectionSocketFactory())
                    .register("https", sslConnectionSocketFactory)
                    .build();
            PoolingHttpClientConnectionManager httpClientConnectionManager = new PoolingHttpClientConnectionManager(registry);
            httpClientConnectionManager.setMaxTotal(1000);
            HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager(httpClientConnectionManager);
            httpClientBuilder = httpClientBuilder.setConnectionManager(httpClientConnectionManager);
            // 2025-08-11 添加超时配置
            final Long timeout = 300L;
            RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
            requestConfigBuilder.setConnectTimeout(timeout, TimeUnit.SECONDS);
            requestConfigBuilder.setConnectionRequestTimeout(timeout, TimeUnit.SECONDS);
            requestConfigBuilder.setResponseTimeout(timeout, TimeUnit.SECONDS);
            RequestConfig requestConfig = requestConfigBuilder.build();
            httpClientBuilder.setDefaultRequestConfig(requestConfig);
            // 设置cookie
            CookieStore cookieStore = new BasicCookieStore();
            if(!CollectionUtil.isBlank(cookieMap)){
                Set<String> keySet = cookieMap.keySet();
                Iterator<String> iterator = keySet.iterator();
                while(iterator.hasNext()){
                    final String name = iterator.next();
                    final List<String> vvvList = cookieMap.get(name);
                    String value = vvvList.get(0);
                    String domain = vvvList.get(1);
                    String path = vvvList.get(2);
                    cookieStore = setCookieStore(cookieStore, name, value, domain, path, false);
                }
                httpClientBuilder.setDefaultCookieStore(cookieStore);
            }
            //
            final CloseableHttpClient closeableHttpClient = httpClientBuilder.build();
            return closeableHttpClient;
        }catch(Throwable throwable){
            LogUtil.error("HttpsUtil_p_buildHttpClient_t1", throwable);
            return null;
        }
    }

    /**
     * 2025-01-03
     * 创建SSLSocketFactory实例
     */
    public static final SSLSocketFactory getSSLSocketFactory() {
        SSLContext sslContext = p_getDefaultSSLContext();
        if(sslContext == null){
            return null;
        }
        return sslContext.getSocketFactory();
    }

    /**
     * 2025-01-03
     * 创建SSLSocketFactory实例
     * 2026-10-17 安全默认：未指定证书时使用JVM信任库与标准校验
     */
    public static final SSLSocketFactory getSSLSocketFactory(final String password, final String jksUrl, final String p12Url) {
        SSLContext sslContext = StringUtil.isBlank(password, jksUrl, p12Url) ? p_getDefaultSSLContext() : p_getSSLContext(password, jksUrl, p12Url);
        if(sslContext == null){
            return null;
        }
        return sslContext.getSocketFactory();
    }

    public static final SSLSocketFactory getSSLSocketFactory(final String cerPath) {
        SSLContext sslContext = p_getSSLContext(cerPath);
        if(sslContext == null){
            return null;
        }
        return sslContext.getSocketFactory();
    }

    /**
     * 2026-10-17
     * 获取信任全部证书的SSLSocketFactory
     * 安全提示：存在中间人攻击风险，仅用于内网自签名证书、测试等明确场景
     */
    public static final SSLSocketFactory getTrustAllSSLSocketFactory() {
        SSLContext sslContext = p_getTrustAllSSLContext();
        if(sslContext == null){
            return null;
        }
        return sslContext.getSocketFactory();
    }


    /**
     * 2025-01-03
     * 获取指定证书的sslContext对象
     * @param password
     * @param jksUrl
     * @param p12Url
     * @return
     */
    private static final SSLContext p_getSSLContext(final String password, final String jksUrl, final String p12Url){
        // 2026-10-17 安全默认：未指定证书时使用默认SSLContext（JVM信任库+标准校验），不再默认信任全部证书
        if(StringUtil.isBlank(password, jksUrl, p12Url)){
            return p_getDefaultSSLContext();
        }
        // 获取指定证书的sslContext对象
        try{
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            KeyStore keyStore = p_getKeyStore(password, p12Url, "PKCS12");
            keyManagerFactory.init(keyStore, password.toCharArray());
            // 初始化信任库
            KeyStore trustKeyStore = p_getKeyStore(password, jksUrl, "JKS");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustKeyStore);
            // 加载协议
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustManagerArray = trustManagerFactory.getTrustManagers();
            KeyManager[] keyManagerArray = keyManagerFactory.getKeyManagers();
            sslContext.init(keyManagerArray, trustManagerArray, null);
            return sslContext;
        }catch (Throwable throwable){
            LogUtil.error("HttpsUtil_getSSLContext_t1", throwable, jksUrl, p12Url);
            return null;
        }
    }

    /**
     * 2026-10-17
     * 获取默认SSLContext（JVM信任库+标准校验）
     */
    private static final SSLContext p_getDefaultSSLContext(){
        try{
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, new SecureRandom());
            return sslContext;
        }catch(Throwable throwable){
            LogUtil.error("HttpsUtil_p_getDefaultSSLContext_t1", throwable);
            return null;
        }
    }

    /**
     * 2026-10-17
     * 获取信任全部证书的SSLContext
     * 安全提示：跳过证书校验存在中间人攻击风险，仅供内网自签名证书、测试等明确场景使用
     */
    private static final SSLContext p_getTrustAllSSLContext(){
        try{
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustManagers = {new MyX509TrustManager()};
            sslContext.init(null, trustManagers, new SecureRandom());
            return sslContext;
        }catch(Throwable throwable){
            LogUtil.error("HttpsUtil_p_getTrustAllSSLContext_t1", throwable);
            return null;
        }
    }

    /**
     * 获取(密钥及证书)仓库
     * @param password 证书密码
     * @param jksUrl 证书路径
     * @param type 证书类型
     */
    private static final KeyStore p_getKeyStore(final String password, final String jksUrl, final String type){
        FileInputStream fileInputStream = null;
        try{
            fileInputStream = new FileInputStream(jksUrl);
            KeyStore keyStore = KeyStore.getInstance(type);  // 秘钥仓库
            keyStore.load(fileInputStream, password.toCharArray());
            return keyStore;
        }catch (Throwable throwable){
            LogUtil.error("HttpsUtil_getKeyStore_t1", throwable, jksUrl, type);
            return null;
        }finally {
            IOUtil.close(fileInputStream);
        }
    }

    /**
     * 2025-01-03
     * @param cookieStore
     * @param name
     * @param value
     * @param domain
     * @param path
     * @param secure
     */
    public static final CookieStore setCookieStore(final CookieStore cookieStore, final String name, final String value, final String domain, final String path, final boolean secure){
        if(StringUtil.isBlank(name)){
            return cookieStore;
        }
        BasicClientCookie basicClientCookie = new BasicClientCookie(name, value);
        basicClientCookie.setDomain(domain);  // 设置范围
        basicClientCookie.setPath(path);
        basicClientCookie.setSecure(secure);
        cookieStore.addCookie(basicClientCookie);
        return cookieStore;
    }

    public static final SSLContext p_getSSLContext(final String cerPath) {
        // 2026-10-17 安全默认：未指定证书时使用默认SSLContext（JVM信任库+标准校验），不再默认信任全部证书
        if (StringUtil.isBlank(cerPath)) {
            return p_getDefaultSSLContext();
        }
        ByteArrayInputStream bais = null;
        BufferedReader br = null;
        try {
            String text = FileUtil.fileReadText(cerPath);
            text = StringUtil.deleteAll(text, "\n", "-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----");
            // 将Base64编码的字符串解码为字节数组
            byte[] decoded = Base64.getDecoder().decode(text);
            // 加载证书
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            bais = new ByteArrayInputStream(decoded);
            X509Certificate cert = (X509Certificate) cf.generateCertificate(bais);
            // 创建一个新的 TrustManager 来信任这个证书
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance("jks");
            // 加载Keystore文件，需要提供访问Keystore的密码
            keyStore.load(null, DEFAULT_KEYSTORE_PASSWORD.toCharArray());
            keyStore.setCertificateEntry("cert-alias", cert);
            tmf.init(keyStore);
            TrustManager[] trustManagers = tmf.getTrustManagers();
            SSLContext sslContext = SSLContext.getInstance("TLS"); // 推荐使用 TLS 而不是 SSL
            sslContext.init(null, trustManagers, new SecureRandom());
            return sslContext;
        } catch (Throwable e) {
            LogUtil.error("HttpsUtil_p_getSSLContextByCer_e1", e);
            return null;
        }finally {
            IOUtil.close(bais, br);
        }
    }

    private static final CloseableHttpClient getHttpClient(final String cerPath, final Map<String, List<String>> cookieMap) {
        SSLContext sslContext = p_getSSLContext(cerPath);
        if(sslContext == null){
            return null;
        }
        // 未指定证书时使用标准主机名校验；指定证书（自签名/内网）时跳过主机名校验
        SSLConnectionSocketFactory sslConnectionSocketFactory = StringUtil.isBlank(cerPath)
                ? new SSLConnectionSocketFactory(sslContext)
                : new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        return p_buildHttpClient(sslConnectionSocketFactory, cookieMap);
    }
}
