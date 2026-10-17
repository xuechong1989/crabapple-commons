package xc.crabapple.ssm.forward;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.UriEncoderUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.servlet.ServletUtil;

/**
 * 2022-11-16
 * 请求转发（反向）代理，类似于nginx的作用
 */
public final class RequestProxyUtil {

    /**
     * 2023-02-24
     * 2023-03-02 添加参数fromRequestURI
     * 转发、代理（即调用方URL不会变化），类似于nginx的作用
     * @param request
     * @param response
     * @param targetAddr 要代理的网络地址，格式如 http://127.0.0.1:10000
     */
    public static final void forward(final HttpServletRequest request, final HttpServletResponse response, final String targetAddr){
        forward(request, response, targetAddr, "", "");
    }

    /**
     * 2023-02-24
     * 2023-03-02 添加参数fromRequestURI
     * 转发、代理（即调用方URL不会变化），类似于nginx的作用
     * @param request
     * @param response
     * @param targetAddr 要代理的网络地址，格式如 http://127.0.0.1:10000
     */
    public static final void forward(final HttpServletRequest request, final HttpServletResponse response, final String fromRequestURI, final String targetAddr){
        forward(request, response, targetAddr, fromRequestURI, "");
    }

    /**
     * 2022-11-16
     * 2023-02-24 添加参数defaultParam
     * 2023-03-02 添加参数fromRequestURI
     * 2024-08-05 统一url编码
     * 转发、代理（即调用方URL不会变化），类似于nginx的作用
     * @param request
     * @param response
     * @param targetAddr 要代理的网络地址，格式如 http://127.0.0.1:10000
     * @param defaultParam 默认参数，格式如 aa=hello 或 aa=hello&bb=world
     */
    public static final void forward(final HttpServletRequest request, final HttpServletResponse response, final String fromRequestURI, final String targetAddr, final String defaultParam){
        String localTargetAddr = StringUtil.deleteAll(targetAddr, " ");
        if(StringUtil.isBlank(localTargetAddr)){
            LogUtil.error("RequestProxyUtil_forward_1", "targetAddr为空", localTargetAddr);
            return;
        }
        if(!"/".equals(localTargetAddr)){
            if(StringUtil.lastsWith(localTargetAddr, "/")){
                int length = localTargetAddr.length();
                length = length - 1;
                localTargetAddr = localTargetAddr.substring(0, length);
            }
        }
        //
        String requestURI = fromRequestURI;
        if(StringUtil.isBlank(requestURI)){
            requestURI = ServletUtil.getRequestURI();
        }
        requestURI = UriEncoderUtil.encodeURIComponent(requestURI, UriEncoderUtil.EXCLUDE_ADDRESS);
        //
        String url = "";
        String methodName = "";
        OutputStream outputStream = null;
        ServletInputStream servletInputStream = null;
        ClientHttpResponse clientHttpResponse = null;
        InputStream inputStream = null;
        ServletOutputStream servletOutputStream = null;
        try{
            URI uri = new URI(requestURI);
            String path = uri.getPath();
            String query = request.getQueryString();
            if(StringUtil.isBlank(query)){
                if(StringUtil.isBlank(defaultParam)){
                    query = "";
                }else{
                    query = defaultParam;
                }
            }else{
                query += ("&" + defaultParam);
            }
            if(StringUtil.isBlank(query)){
                url = localTargetAddr + path;
            }else{
                // 由于无法确定query是否为已编码的值，故先尝试解码如果成功则统一编码、如果不成功说明不是解码值，直接对其编码即可。确保query只被编码一次，避免重复编码。
                try{
                    String tmpQuery = UriEncoderUtil.decodeURIComponent(query);
                    if(!StringUtil.isBlank(tmpQuery)){
                        query = tmpQuery;
                    }
                }catch (Throwable throwable){

                }
                query = UriEncoderUtil.encodeURIComponent(query, UriEncoderUtil.EXCLUDE_PARAM);
                url = localTargetAddr + path + "?" + query;
            }
            URI newUri = new URI(url);
            methodName = request.getMethod();
            HttpMethod httpMethod = HttpMethod.valueOf(methodName);
            if(httpMethod == null){
                LogUtil.error("RequestProxyUtil_forward_2", "httpMethod为空", requestURI, url, methodName);
                return;
            }
            SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
            ClientHttpRequest clientHttpRequest = simpleClientHttpRequestFactory.createRequest(newUri, httpMethod);
            Enumeration<String> headerNames = request.getHeaderNames();
            HttpHeaders httpHeaders = clientHttpRequest.getHeaders();
            while(headerNames.hasMoreElements()){
                String headerName = headerNames.nextElement();
                Enumeration<String> enumeration = request.getHeaders(headerName);
                while(enumeration.hasMoreElements()){
                    String value = enumeration.nextElement();
                    httpHeaders.add(headerName, value);
                }
            }
            outputStream = clientHttpRequest.getBody();
            servletInputStream = request.getInputStream();
            StreamUtils.copy(servletInputStream, outputStream);
            clientHttpResponse = clientHttpRequest.execute();
            HttpStatusCode httpStatusCode = clientHttpResponse.getStatusCode();
            int status = httpStatusCode.value();
            response.setStatus(status);
            HttpHeaders httpHeaders1 = clientHttpResponse.getHeaders();
            Set<String> set = httpHeaders1.toSingleValueMap().keySet();
            Iterator<String> iterator = set.iterator();
            while(iterator.hasNext()){
                String key = iterator.next();
                List<String> valueList = httpHeaders1.get(key);
                for(String value : valueList){
                    response.setHeader(key, value);
                }
            }
            //
            // URI newURI = clientHttpRequest.getURI();
            // String newUriStr = newURI.toString();
            // 只负责转发
            inputStream = clientHttpResponse.getBody();
            servletOutputStream = response.getOutputStream();
            StreamUtils.copy(inputStream, servletOutputStream);
        }catch(Throwable throwable){
            LogUtil.error("RequestProxyUtil_forward_t1", throwable, requestURI, url, methodName);
        }finally{
            IOUtil.close(servletOutputStream, inputStream, clientHttpResponse, servletInputStream, outputStream);
        }
    }

    /**
     * 2023-03-04 重定向
     * 重定向（即调用方URL会变化）
     * @param request
     * @param response
     * @param targetAddr 要代理的网络地址，格式如 http://127.0.0.1:10000
     */
    public static final void redirect(final HttpServletRequest request, final HttpServletResponse response, final String targetAddr){
        redirect(request, response, targetAddr, "", "");
    }

    /**
     * 2023-03-04 重定向
     * 重定向（即调用方URL会变化）
     * @param request
     * @param response
     * @param targetAddr 要代理的网络地址，格式如 http://127.0.0.1:10000
     */
    public static final void redirect(final HttpServletRequest request, final HttpServletResponse response, final String fromRequestURI, final String targetAddr){
        redirect(request, response, targetAddr, fromRequestURI, "");
    }

    /**
     * 2023-03-04 重定向
     * 2024-08-05 统一url编码
     * 重定向（即调用方URL会变化）
     * @param request
     * @param response
     * @param targetAddr 要代理的网络地址，格式如 http://127.0.0.1:10000
     * @param defaultParam 默认参数，格式如 aa=hello 或 aa=hello&bb=world
     */
    public static final String redirect(final HttpServletRequest request, final HttpServletResponse response, final String fromRequestURI, final String targetAddr, final String defaultParam){
        String localTargetAddr = StringUtil.deleteAll(targetAddr, " ");
        if(StringUtil.isBlank(localTargetAddr)){
            LogUtil.error("RequestProxyUtil_redirect_1", "targetAddr为空", localTargetAddr);
            return "";
        }
        if(!"/".equals(localTargetAddr)){
            if(StringUtil.lastsWith(localTargetAddr, "/")){
                int length = localTargetAddr.length();
                length = length - 1;
                localTargetAddr = localTargetAddr.substring(0, length);
            }
        }
        //
        String requestURI = fromRequestURI;
        if(StringUtil.isBlank(requestURI)){
            requestURI = ServletUtil.getRequestURI();
        }
        requestURI = UriEncoderUtil.encodeURIComponent(requestURI, UriEncoderUtil.EXCLUDE_ADDRESS);
        //
        String url = "";
        String methodName = "";
        OutputStream outputStream = null;
        ServletInputStream servletInputStream = null;
        ClientHttpResponse clientHttpResponse = null;
        InputStream inputStream = null;
        ServletOutputStream servletOutputStream = null;
        try{
            URI uri = new URI(requestURI);
            String path = uri.getPath();
            String query = request.getQueryString();
            if(StringUtil.isBlank(query)){
                if(StringUtil.isBlank(defaultParam)){
                    query = "";
                }else{
                    query = defaultParam;
                }
            }else{
                query += ("&" + defaultParam);
            }
            if(StringUtil.isBlank(query)){
                url = localTargetAddr + path;
            }else{
                query = UriEncoderUtil.encodeURIComponent(query, UriEncoderUtil.EXCLUDE_PARAM);
                url = localTargetAddr + path + "?" + query;
            }
            URI newUri = new URI(url);
            methodName = request.getMethod();
            HttpMethod httpMethod = HttpMethod.valueOf(methodName);
            if(httpMethod == null){
                LogUtil.error("RequestProxyUtil_redirect_2", "httpMethod为空", requestURI, url, methodName);
                return "";
            }
            SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
            ClientHttpRequest clientHttpRequest = simpleClientHttpRequestFactory.createRequest(newUri, httpMethod);
            Enumeration<String> headerNames = request.getHeaderNames();
            HttpHeaders httpHeaders = clientHttpRequest.getHeaders();
            while(headerNames.hasMoreElements()){
                String headerName = headerNames.nextElement();
                Enumeration<String> enumeration = request.getHeaders(headerName);
                while(enumeration.hasMoreElements()){
                    String value = enumeration.nextElement();
                    httpHeaders.add(headerName, value);
                }
            }
            outputStream = clientHttpRequest.getBody();
            servletInputStream = request.getInputStream();
            StreamUtils.copy(servletInputStream, outputStream);
            clientHttpResponse = clientHttpRequest.execute();
            HttpStatusCode httpStatusCode = clientHttpResponse.getStatusCode();
            int status = httpStatusCode.value();
            response.setStatus(status);
            HttpHeaders httpHeaders1 = clientHttpResponse.getHeaders();
            Set<String> set = httpHeaders1.toSingleValueMap().keySet();
            Iterator<String> iterator = set.iterator();
            while(iterator.hasNext()){
                String key = iterator.next();
                List<String> valueList = httpHeaders1.get(key);
                for(String value : valueList){
                    response.setHeader(key, value);
                }
            }
            //
            URI newURI = clientHttpRequest.getURI();
            // 只负责重定向
            String newUriStr = newURI.toString();
            LogUtil.print("RequestProxyUtil_redirect_3", "重定向==>"+newUriStr);
            return newUriStr;
        }catch(Throwable throwable){
            LogUtil.error("RequestProxyUtil_redirect_t1", throwable, requestURI, url, methodName);
            return "";
        }finally{
            IOUtil.close(servletOutputStream, inputStream, clientHttpResponse, servletInputStream, outputStream);
        }
    }

}