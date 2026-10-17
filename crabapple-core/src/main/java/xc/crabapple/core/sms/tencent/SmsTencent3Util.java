package xc.crabapple.core.sms.tencent;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.sms.SmsData;

/**
 * 2023-01-23 update
 * 2023-09-20 重载send方法，屏蔽腾讯云短信API中自带的代理模式，使用自定义代理
 * 基于腾讯的短信平台, 3.0版本
 * 官方网址 https://www.tencentcloud.com/zh/products/sms
 */
public final class SmsTencent3Util {

    private static String p_secretId;
    private static String p_secretKey;
    private static String p_smsSdkAppId;
    private static String p_templateId;
    private static String p_signName;
    private static String p_proxyEnable;
    private static String p_protocol;
    private static String p_proxyIp;
    private static String p_proxyPort;

    /**
     * 2023-01-23 update
     * 初始化
     */
    public static final void init(final String secretId, final String secretKey, final String smsSdkAppId, final String templateId, final String signName, final String proxyEnable, final String protocol, final String proxyIp, final String proxyPort){
        p_secretId = secretId;
        p_secretKey = secretKey;
        p_smsSdkAppId = smsSdkAppId;
        p_templateId = templateId;
        p_signName = signName;
        p_proxyEnable = proxyEnable;
        p_protocol = protocol;
        p_proxyIp = proxyIp;
        p_proxyPort = proxyPort;
    }

    /**
     * 2023-01-23 update
     * 发送短信
     */
    public static final SmsData send(final String mobile, final String content) {
        SmsData smsTencent3Data = send(p_secretId, p_secretKey, p_smsSdkAppId, p_templateId, p_signName, p_proxyEnable, p_protocol, p_proxyIp, p_proxyPort, mobile, content);
        return smsTencent3Data;
    }

    /**
     * 2023-09-20
     * 发送短信总入口
     * @param secretId
     * @param secretKey
     * @param smsSdkAppId
     * @param templateId
     * @param signName
     * @param proxyEnable
     * @param protocol
     * @param proxyIp
     * @param proxyPort
     * @param mobile
     * @param content
     * @return
     */
    public static final SmsData send(final String secretId, final String secretKey, final String smsSdkAppId, final String templateId, final String signName, final String proxyEnable, final String protocol, final String proxyIp, final String proxyPort, final String mobile, final String content){
        try{
            Credential credential = new Credential(secretId, secretKey);
            HttpProfile httpProfile = new HttpProfile();
            if ("yes".equals(proxyEnable)) {
                Integer port = Integer.parseInt(proxyPort);
                httpProfile.setProtocol(protocol);
                httpProfile.setProxyHost(proxyIp);
                httpProfile.setProxyPort(port);
            }
            httpProfile.setReqMethod("POST");
            httpProfile.setConnTimeout(60);
            httpProfile.setEndpoint("sms.tencentcloudapi.com");
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setSignMethod("HmacSHA256");
            clientProfile.setHttpProfile(httpProfile);
            clientProfile.setDebug(false);
            SendSmsRequest sendSmsRequest = new SendSmsRequest();
            sendSmsRequest.setSmsSdkAppId(smsSdkAppId);
            sendSmsRequest.setSignName(signName);
            sendSmsRequest.setTemplateId(templateId);
            String[] templateParamArray = {content};
            sendSmsRequest.setTemplateParamSet(templateParamArray);
            String localMobile = mobile;
            if (!localMobile.startsWith("+86")) {
                localMobile = "+86" + localMobile;
            }
            String[] phoneNumberSet = {localMobile};
            sendSmsRequest.setPhoneNumberSet(phoneNumberSet);
            SmsClient smsClient = new SmsClient(credential, "ap-guangzhou", clientProfile);
            SendSmsResponse sendSmsResponse = smsClient.SendSms(sendSmsRequest);
            SendStatus[] sendStatusArray = sendSmsResponse.getSendStatusSet();
            if (CollectionUtil.isBlank(sendStatusArray)) {
                LogUtil.error("SmsTencent3Util_send_1", "返回结果为空！", content);
                SmsData smsTencent3Data = new SmsData(false, "001", "返回结果为空！", content);
                return smsTencent3Data;
            }
            SendStatus sendStatus = sendStatusArray[0];
            String code = sendStatus.getCode();
            code = StringUtil.toLowerCase(code);
            if (!"ok".equals(code)) {
                String message = sendStatus.getMessage();
                LogUtil.error("SmsTencent3Util_send_2", code, message, sendStatus);
                SmsData smsTencent3Data = new SmsData(false, code, message, content);
                return smsTencent3Data;
            }
            SmsData smsTencent3Data = new SmsData(true, code, "成功", content);
            return smsTencent3Data;
        }catch(Throwable throwable){
            LogUtil.error("SmsTencent3Util_send_t1", throwable, smsSdkAppId, templateId, signName, secretId, secretKey, proxyEnable, proxyPort, proxyIp, protocol);
            String message = throwable.getMessage();
            SmsData smsTencent3Data = new SmsData(false, "010", message, content);
            return smsTencent3Data;
        }
    }

}