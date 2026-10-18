package xc.crabapple.core.email;

import xc.crabapple.core.data.bean.BeanBase;

import java.util.HashMap;
import java.util.Map;

/**
 * 2019-12-05 2.2.0
 */
public class EmailBean extends BeanBase {

    private String host;  //邮件服务器地址，如smtp.example.com
    private Integer port;  //25
    private String fromName;  //账号名称，必须是@之前的所有部分，如your-account
    private String fromUrl;  //发件人邮箱地址，如your-account@example.com
    private String password;  //发件人邮箱密码，如your-password
    private String[] targetUrls;  //{"target1@example.com", "target2@example.com"}
    private String title;  //你好
    private String content;  //内容测试
    private String debug = "no";  //是否为debug模式，是调试模式-yes，不是调试模式-no
    private Map<String, String> params = null;

    public EmailBean(){
        params = new HashMap<>();
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(final Integer port) {
        this.port = port;
    }

    public String getFromUrl() {
        return fromUrl;
    }

    public void setFromUrl(final String fromUrl) {
        this.fromUrl = fromUrl;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(final String fromName) {
        this.fromName = fromName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String[] getTargetUrls() {
        return targetUrls;
    }

    public void setTargetUrls(final String[] targetUrls) {
        this.targetUrls = targetUrls;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public String getDebug() {
        return debug;
    }

    public void setDebug(final String debug) {
        this.debug = debug;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(final Map<String, String> params) {
        this.params = params;
    }

    public void setParam(final String key, final String value){
        params.put(key, value);
    }

    public String getParam(final String key){
        String value = params.get(key);
        return value;
    }

}
