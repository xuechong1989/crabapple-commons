package xc.crabapple.core.sms.gateway;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.ListUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.sms.SmsData;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于第三方短信平台
 * 2022-07-05 update
 * 2023-01-23 update
 */
public class SmsGatewayUtil {

    private static Map<String, String> messageMap;
    private static HttpClient httpClient;
    private static String p_gatewayHostname;
    private static Integer p_gatewayPort;
    private static Integer p_gatewayConnectTimeout;
    private static Integer p_gatewaySocketTimeout;
    private static Integer p_gatewayConnectionRequestTimeout;
    private static String p_generation = "yes";
    private static String p_validLogin = "yes";
    private static String p_smsCodeCleanPoints = "****-**-** 03:00:00";
    private static String p_enterpriseID;
    private static String p_subPort;
    private static String p_loginName;
    private static String p_password;
    //
    private static String p_gatewayAddress = "";

    /**
     * 初始化
     * @param gatewayHostname
     * @param gatewayPort
     * @param gatewayAddress 短信平台地址，如 http://192.168.1.100:8001/sendSMS.action
     * @param gatewayConnectTimeout
     * @param gatewaySocketTimeout
     * @param gatewayConnectionRequestTimeout
     */
    public static final void init(final String gatewayHostname, final Integer gatewayPort, final String gatewayAddress, final Integer gatewayConnectTimeout, final Integer gatewaySocketTimeout, final Integer gatewayConnectionRequestTimeout, final String enterpriseID, final String subPort, final String loginName, final String password) {
        messageMap = new HashMap<>();
        messageMap.put("0", "成功");
        messageMap.put("1", "登录密码错误");
        messageMap.put("2", "企业ID或登录名错误");
        messageMap.put("3", "余额不足");
        messageMap.put("4", "用户归属服务器错误");
        messageMap.put("5", "帐户停用或不存在");
        messageMap.put("6", "内容为空");
        messageMap.put("7", "号码为空");
        messageMap.put("8", "号码超过最大限制数");
        messageMap.put("9", "内容包含关键字");
        messageMap.put("10", "非法操作导致ip被锁");
        messageMap.put("11", "访问过快");
        messageMap.put("12", "批量一对一参数格式错误");
        messageMap.put("13", "批量一对一出现重复信息(号码，内容同时重复)");
        messageMap.put("14", "签名未报备");
        messageMap.put("15", "单位时间内该号码超过最大发送限制");
        messageMap.put("16", "签名必须在【4，10】字符间");
        messageMap.put("17", "内容涉嫌营销内容");
        messageMap.put("18", "30秒内出现重复发送");
        messageMap.put("27", "内容超过4000字符");
        messageMap.put("28", "系统内部错误");
        //
        p_gatewayAddress = gatewayAddress;
        p_gatewayHostname = gatewayHostname;
        p_gatewayPort = gatewayPort;
        p_gatewayConnectTimeout = gatewayConnectTimeout;
        p_gatewaySocketTimeout = gatewaySocketTimeout;
        p_gatewayConnectionRequestTimeout = gatewayConnectionRequestTimeout;
        //
        p_enterpriseID = enterpriseID;
        p_subPort = subPort;
        p_loginName = loginName;
        p_password = password;
        //
        LogUtil.print("SmsGatewayUtil_init_1", "短信平台配置==>名称:短信平台, 生成:" + p_generation + ", 校验:" + p_validLogin + ", 定时:" + p_smsCodeCleanPoints);
    }

    /**
     * 发送短信校验码
     */
    public static final SmsData send(final String mobile, final String content) {
        if (StringUtil.isBlank(mobile)) {
            SmsData smsData = new SmsData(false, "001", "手机号不能为空", null);
            return smsData;
        }
        String[] array = new String[]{mobile};
        List<String> mobileList = Arrays.asList(array);
        SmsData smsData = p_sendBatch(content, mobileList);
        return smsData;
    }

    private static final SmsData p_sendBatch(final String content, final List<String> mobileList) {
        if (CollectionUtil.isBlank(mobileList)) {
            SmsData smsData = new SmsData(false, "001", "手机号不能为空!", null);
            return smsData;
        }
        if (mobileList.size() > 100) {
            String mobileListStr = ListUtil.toString(mobileList);
            LogUtil.error("SmsGatewayUtil_p_sendBatch_12", "短信平台，发送手机验证码失败", p_gatewayAddress, content, mobileListStr);
            SmsData smsData = new SmsData(false, "002", "手机号数量不能超出100!", null);
            return smsData;
        }
        if (httpClient == null) {
            httpClient = HttpClients.createDefault();
        }
        // 模版内容
        String emsTemplate = "[验证码]尊敬的用户，您的风电建设档案系统验证码是#{content}。为保障账户安全，请勿泄露。";
        String toBeSendContent = emsTemplate.replace("#{content}", content);
        StringBuilder mobileSb = new StringBuilder();
        for(int i = 0; i < mobileList.size(); i++){
            if(i > 0){
                mobileSb.append(",");
            }
            mobileSb.append(mobileList.get(i));
        }
        String strMobiles = mobileSb.toString();
        Map<String, Object> params = new HashMap<>();
        params.put("enterpriseID", p_enterpriseID);
        params.put("subPort", p_subPort);
        params.put("loginName", p_loginName);
        params.put("password", p_password);
        params.put("content", toBeSendContent);
        params.put("mobiles", strMobiles);
        List<NameValuePair> nameValuePairs = p_getNameValuePairs(params);
        String respCtn = "";
        try {
            HttpHost httpHost = new HttpHost(p_gatewayHostname, p_gatewayPort);
            HttpPost httpPost = new HttpPost(p_gatewayAddress);
            RequestConfig.Builder builder = RequestConfig.custom();
            builder.setProxy(httpHost);
            builder.setConnectTimeout(p_gatewayConnectTimeout);
            builder.setSocketTimeout(p_gatewaySocketTimeout);
            builder.setConnectionRequestTimeout(p_gatewayConnectionRequestTimeout);
            RequestConfig requestConfig = builder.build();
            httpPost.setConfig(requestConfig);
            UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairs, "UTF-8");
            httpPost.setEntity(urlEncodedFormEntity);
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity httpEntity = response.getEntity();
            respCtn = EntityUtils.toString(httpEntity);
        } catch (UnsupportedEncodingException e) {
            LogUtil.error("SmsGatewayUtil_p_sendBatch_e1", e, "短信平台，发送手机验证码失败", p_gatewayAddress, toBeSendContent, strMobiles);
            SmsData smsData = new SmsData(false, "003", "请求失败!", null);
            return smsData;
        } catch (ClientProtocolException e) {
            LogUtil.error("SmsGatewayUtil_p_sendBatch_e2", e, "短信平台，发送手机验证码失败", p_gatewayAddress, toBeSendContent, strMobiles);
            SmsData smsData = new SmsData(false, "004", "请求失败!", null);
            return smsData;
        } catch (IOException e) {
            LogUtil.error("SmsGatewayUtil_p_sendBatch_e3", e, "短信平台，发送手机验证码失败", p_gatewayAddress, toBeSendContent, strMobiles);
            SmsData smsData = new SmsData(false, "005", "请求失败!", null);
            return smsData;
        } catch (Throwable throwable) {
            LogUtil.error("SmsGatewayUtil_p_sendBatch_t1", throwable, "短信平台，发送手机验证码失败", p_gatewayAddress, toBeSendContent, strMobiles);
            SmsData smsData = new SmsData(false, "999", "请求失败!", null);
            return smsData;
        }
        org.jsoup.nodes.Document document = Jsoup.parse(respCtn);
        Elements elements = document.getElementsByTag("Response");
        Element element = elements.get(0);
        Elements result = element.getElementsByTag("Result");
        String resultContent = result.text();
        String message = messageMap.get(resultContent);
        if (!"0".equals(resultContent)) {
            LogUtil.error("SmsGatewayUtil_p_sendBatch_4", "短信平台，发送手机验证码失败", respCtn, p_enterpriseID, p_subPort, p_loginName, p_password, p_gatewayAddress, toBeSendContent, strMobiles);
            SmsData smsData = new SmsData(false, "006", message, resultContent);
            return smsData;
        }
        String string = ListUtil.toString(mobileList);
        LogUtil.println("SmsGatewayUtil_p_sendBatch_1", "短信平台，发送手机验证码成功", respCtn, string, content);
        SmsData smsData = new SmsData(true, "000", "成功!", content);
        return smsData;
    }

    private static final List<NameValuePair> p_getNameValuePairs(final Map<String, Object> params) {
        if(CollectionUtil.isBlank(params)){
            List<NameValuePair> nameValuePairList = new ArrayList<>();
            return nameValuePairList;
        }
        List<NameValuePair> nameValuePairList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if(entry == null){
                continue;
            }
            String key = entry.getKey();
            String value = "";
            Object object = entry.getValue();
            if(object != null){
                value = object.toString();
            }
            BasicNameValuePair basicNameValuePair = new BasicNameValuePair(key, value);
            nameValuePairList.add(basicNameValuePair);
        }
        return nameValuePairList;
    }

}