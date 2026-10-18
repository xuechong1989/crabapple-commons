package xc.crabapple.ssm.spring;

import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import xc.crabapple.core.timer.TimerUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 2024-12-28
 */
public class SsmResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @InitBinder
    public void initBinder(final WebDataBinder webDataBinder) {
        CustomNumberEditor customNumberEditor1 = new CustomNumberEditor(Integer.class, null, true);
        webDataBinder.registerCustomEditor(Integer.class, null, customNumberEditor1);
        //
        CustomNumberEditor customNumberEditor2 = new CustomNumberEditor(Long.class, null, true);
        webDataBinder.registerCustomEditor(Long.class, null, customNumberEditor2);
        //
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(TimerUtil.DATE_FORMAT_YMD);  // DATE_FORMAT_FULL
        simpleDateFormat.setLenient(false);
        CustomDateEditor customDateEditor3 = new CustomDateEditor(simpleDateFormat, true);
        webDataBinder.registerCustomEditor(Date.class, null, customDateEditor3);
        //
        ByteArrayMultipartFileEditor byteArrayMultipartFileEditor = new ByteArrayMultipartFileEditor();
        webDataBinder.registerCustomEditor(byte[].class, byteArrayMultipartFileEditor);
    }

    @Override
    public boolean supports(final MethodParameter returnType, final Class converterType) {
        return true;
    }

    /**
     * 2024-12-28
     * 服务器响应给客户端数据时统一执行入口，执行完controller方法后再执行此方法。
     * @param body
     * @param returnType
     * @param selectedContentType
     * @param selectedConverterType
     * @param request
     * @param response
     * @return
     */
    @Override
    public Object beforeBodyWrite(final Object body, final MethodParameter returnType, final MediaType selectedContentType, final Class selectedConverterType, final ServerHttpRequest request, final ServerHttpResponse response) {
        return body;
    }

}
