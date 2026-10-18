package xc.crabapple.core.sms;

import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.sms.aliyun.SmsAliyunData;
import xc.crabapple.core.sms.aliyun.SmsAliyunUtil;
import xc.crabapple.core.sms.gateway.SmsGatewayUtil;
import xc.crabapple.core.sms.tencent.SmsTencent3Util;

/**
 * 2025-01-27
 * 2026-10-16 接入阿里云短信平台
 * 短信工具类
 */
public final class SmsUtil {

    public static final String TENCENT = "tencent";
    public static final String GATEWAY = "gateway";
    // 2026-10-16 接入阿里云短信平台
    // 阿里云短信平台标识
    public static final String ALIYUN = "aliyun";
    private static String p_smsType = TENCENT;

    /**
     * 2025-09-14
     * @param secretId
     * @param secretKey
     * @param smsSdkAppId
     * @param templateId
     * @param signName
     * @param proxyEnable
     * @param protocol
     * @param proxyIp
     * @param proxyPort
     */
    public static final void initTencent(final String secretId, final String secretKey, final String smsSdkAppId, final String templateId, final String signName, final String proxyEnable, final String protocol, final String proxyIp, final String proxyPort) {
        p_smsType = TENCENT;
        SmsTencent3Util.init(secretId, secretKey, smsSdkAppId, templateId, signName, proxyEnable, protocol, proxyIp, proxyPort);
    }

    public static final void initGateway(final String hostname, final int port, final String address, final int connectTimeout, final int socketTimeout, final int connectionRequestTimeout, final String enterpriseID, final String subPort, final String loginName, final String password) {
        p_smsType = GATEWAY;
        SmsGatewayUtil.init(hostname, port, address, connectTimeout, socketTimeout, connectionRequestTimeout, enterpriseID, subPort, loginName, password);
    }

    /**
     * 2026-10-16 接入阿里云短信平台
     * 初始化阿里云短信平台
     * @param accessKeyId 阿里云AccessKey ID
     * @param accessKeySecret 阿里云AccessKey Secret
     * @param templateCode 短信模板code
     * @param signName 短信签名
     * @param templateParamName 模板变量名, 如code
     * @param proxyEnable yes时启用HTTP正向代理
     * @param protocol 代理协议, 带冒号双斜杠, 如http://
     * @param proxyIp 代理IP
     * @param proxyPort 代理端口
     */
    public static final void initAliyun(final String accessKeyId, final String accessKeySecret, final String templateCode, final String signName, final String templateParamName, final String proxyEnable, final String protocol, final String proxyIp, final String proxyPort) {
        p_smsType = ALIYUN;
        SmsAliyunUtil.init(accessKeyId, accessKeySecret, templateCode, signName, templateParamName, proxyEnable, protocol, proxyIp, proxyPort);
    }

    /**
     * 2023-01-23 update
     * 直连外网时使用
     */
    public static final SmsData send(final String mobile) {
        final Integer content = RandomUtil.getInt(6);
        final String contentStr = content.toString();
        //
        final SmsData smsData = send(mobile, contentStr);
        return smsData;
    }

    /**
     * 2023-01-23 update
     * 2026-10-16 接入阿里云短信平台 新增阿里云平台分支
     * 直连外网时使用
     */
    public static final SmsData send(final String mobile, final String content) {
        if(TENCENT.equals(p_smsType)){
            final SmsData smsData = SmsTencent3Util.send(mobile, content);
            return smsData;
        }
        // 2026-10-16 接入阿里云短信平台
        // 阿里云工具类返回SmsAliyunData, 四个字段一一对应拷贝成统一的SmsData返回
        if(ALIYUN.equals(p_smsType)){
            final SmsAliyunData smsAliyunData = SmsAliyunUtil.send(mobile, content);
            if(smsAliyunData == null){
                return null;
            }
            final boolean success = smsAliyunData.getSuccess();
            final String code = smsAliyunData.getCode();
            final String message = smsAliyunData.getMessage();
            final String validCode = smsAliyunData.getValidCode();
            final SmsData smsData = new SmsData();
            smsData.setSuccess(success);
            smsData.setCode(code);
            smsData.setMessage(message);
            smsData.setValidCode(validCode);
            return smsData;
        }
        final SmsData smsData = SmsGatewayUtil.send(mobile, content);
        return smsData;
    }

    /**
     * 2025-03-17
     * 测试短信发送
     */
    public static final boolean testSend(){
        final String mobile = "13800138000";
        final String content = "202501";
        final boolean flag = testSend(mobile, content);
        return flag;
    }

    /**
     * 2025-03-17
     * @param mobile
     * 测试短信发送
     */
    public static final boolean testSend(final String mobile){
        final String content = "202501";
        final boolean flag = testSend(mobile, content);
        return flag;
    }

    /**
     * 2025-01-15
     * 测试短信发送
     */
    public static final boolean testSend(final String mobile, final String content){
        SmsData smsData = send(mobile, content);
        boolean success = smsData.getSuccess();
        if(!success){
            LogUtil.error("SmsUtil_testSend_1", "测试短信发送失败", mobile, content, smsData);
            return false;
        }
        LogUtil.print("SmsUtil_testSend_2", "测试短信发送成功", mobile, content);
        return true;
    }

}