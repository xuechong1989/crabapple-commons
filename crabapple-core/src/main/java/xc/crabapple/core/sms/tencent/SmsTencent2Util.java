package xc.crabapple.core.sms.tencent;

import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import com.github.qcloudsms.httpclient.ProxyHTTPClient;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.sms.SmsData;

/**
 * 2022-07-05 update
 * 2023-01-23 update
 * 基于腾讯的短信平台, 2.0版本
 */
public final class SmsTencent2Util {

    private static String host;
    private static int port;
    private static String scheme;
    private static int appid;
    private static String appkey;
    private static int templateId;
    private static String sign;

    /**
     * 2023-01-23 update
     * 初始化
     */
    public static final void init(final String host, final int port, final String scheme, final int appid, final String appkey, final int templateId, final String sign){
        SmsTencent2Util.host = host;
        SmsTencent2Util.port = port;
        SmsTencent2Util.scheme = scheme;
        SmsTencent2Util.appid = appid;
        SmsTencent2Util.appkey = appkey;
        SmsTencent2Util.templateId = templateId;
        SmsTencent2Util.sign = sign;
    }

    /**
     * 2023-01-23 update
     * 发送短信
     */
    public static final SmsData send(final String mobile, final String content) {
        try{
            String[] params = {content};
            ProxyHTTPClient proxyHTTPClient = new ProxyHTTPClient(host, port, scheme);
            SmsSingleSender smsSingleSender = new SmsSingleSender(appid, appkey, proxyHTTPClient);
            SmsSingleSenderResult smsSingleSenderResult = smsSingleSender.sendWithParam("86", mobile, templateId, params, sign, "", "");
            if (smsSingleSenderResult.result == 0) {
                SmsData smsTencent3Data = new SmsData(true, "000", "成功", content);
                return smsTencent3Data;
            }
            SmsData smsTencent3Data = new SmsData(false, "001", smsSingleSenderResult.errMsg, content);
            return smsTencent3Data;
        }catch(Throwable throwable){
            LogUtil.error("SmsTencentUtil_send_t1", throwable, host, port, scheme, appid, templateId, sign);
            String message = throwable.getMessage();
            SmsData smsTencent3Data = new SmsData(false, "010", message, content);
            return smsTencent3Data;
        }
    }

}