package xc.crabapple.core.sms.aliyun;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.json.jackson.JacksonUtil;
import xc.crabapple.core.json.jackson.MyObjectNode;
import xc.crabapple.core.log.LogUtil;

/**
 * 2026-10-14 接入阿里云短信平台
 * 基于阿里云的短信平台, V2.0 SDK（dysmsapi20170525 4.6.0）
 * 官方网址 https://www.aliyun.com/product/sms
 */
public final class SmsAliyunUtil {

    private static String p_accessKeyId;
    private static String p_accessKeySecret;
    private static String p_templateCode;
    private static String p_signName;
    private static String p_templateParamName;
    private static String p_proxyEnable;
    private static String p_protocol;
    private static String p_proxyIp;
    private static String p_proxyPort;

    /**
     * 2026-10-14 接入阿里云短信平台
     * 初始化
     * @param templateParamName 短信模板中的变量名, 如code
     * @param proxyEnable yes时启用HTTP正向代理
     * @param protocol 代理协议, 带冒号双斜杠, 如http://
     */
    public static final void init(final String accessKeyId, final String accessKeySecret, final String templateCode, final String signName, final String templateParamName, final String proxyEnable, final String protocol, final String proxyIp, final String proxyPort){
        p_accessKeyId = accessKeyId;
        p_accessKeySecret = accessKeySecret;
        p_templateCode = templateCode;
        p_signName = signName;
        p_templateParamName = templateParamName;
        p_proxyEnable = proxyEnable;
        p_protocol = protocol;
        p_proxyIp = proxyIp;
        p_proxyPort = proxyPort;
    }

    /**
     * 2026-10-14 接入阿里云短信平台
     * 发送短信（使用init缓存的参数）
     */
    public static final SmsAliyunData send(final String mobile, final String content) {
        SmsAliyunData smsAliyunData = send(p_accessKeyId, p_accessKeySecret, p_templateCode, p_signName, p_templateParamName, p_proxyEnable, p_protocol, p_proxyIp, p_proxyPort, mobile, content);
        return smsAliyunData;
    }

    /**
     * 2026-10-14 接入阿里云短信平台
     * 发送短信总入口（阿里云V2.0 SDK约定: HTTP 2xx才返回body, 一切4xx/5xx错误抛TeaException, 平台错误码从TeaException取）
     */
    public static final SmsAliyunData send(final String accessKeyId, final String accessKeySecret, final String templateCode, final String signName, final String templateParamName, final String proxyEnable, final String protocol, final String proxyIp, final String proxyPort, final String mobile, final String content){
        try{
            Config config = new Config();
            config.accessKeyId = accessKeyId;
            config.accessKeySecret = accessKeySecret;
            config.endpoint = "dysmsapi.aliyuncs.com";
            if ("yes".equals(proxyEnable)) {
                StringBuilder proxyBuilder = new StringBuilder();
                proxyBuilder.append(protocol);
                proxyBuilder.append(proxyIp);
                proxyBuilder.append(":");
                proxyBuilder.append(proxyPort);
                config.httpProxy = proxyBuilder.toString();
            }
            Client client = new Client(config);
            MyObjectNode templateParamNode = JacksonUtil.createMyObjectNode();
            templateParamNode.put(templateParamName, content);
            String templateParam = templateParamNode.toString();
            SendSmsRequest sendSmsRequest = new SendSmsRequest();
            sendSmsRequest.setTemplateCode(templateCode);
            sendSmsRequest.setSignName(signName);
            sendSmsRequest.setPhoneNumbers(mobile);
            sendSmsRequest.setTemplateParam(templateParam);
            SendSmsResponse sendSmsResponse = client.sendSms(sendSmsRequest);
            SendSmsResponseBody sendSmsResponseBody = sendSmsResponse.getBody();
            String code = sendSmsResponseBody.getCode();
            code = StringUtil.toLowerCase(code);
            if (!"ok".equals(code)) {
                String message = sendSmsResponseBody.getMessage();
                LogUtil.error("SmsAliyunUtil_send_2", code, message, templateCode, signName, templateParamName, mobile, content);
                SmsAliyunData smsAliyunData = new SmsAliyunData(false, code, message, content);
                return smsAliyunData;
            }
            SmsAliyunData smsAliyunData = new SmsAliyunData(true, code, "成功", content);
            return smsAliyunData;
        }catch(Throwable throwable){
            LogUtil.error("SmsAliyunUtil_send_t1", throwable, "发送短信异常", templateCode, signName, templateParamName, mobile, content);
            String errorCode = "010";
            if (throwable instanceof TeaException) {
                TeaException teaException = (TeaException)throwable;
                errorCode = teaException.getCode();
                if (errorCode == null) {
                    errorCode = "010";
                }
            }
            String errorMessage = throwable.getMessage();
            SmsAliyunData smsAliyunData = new SmsAliyunData(false, errorCode, errorMessage, content);
            return smsAliyunData;
        }
    }

}
