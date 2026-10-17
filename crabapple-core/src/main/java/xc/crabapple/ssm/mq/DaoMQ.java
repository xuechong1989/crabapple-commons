package xc.crabapple.ssm.mq;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.random.RandomUtil;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2020-08-05 6.4.0
 * 消息队列的中消息基类
 */
public final class DaoMQ extends Object implements Serializable {

    protected static final long serialVersionUID = 1L;

    private String id;
    private String from = "xc_crabapple";  //
    private String type = "xc_crabapple";  //不同种类的消息类别
    private String callbackHosts = "";
    private String callbackName = "";
    private Map<String, Object> callbackParams = null;
    private String content;  //实际内容

    public DaoMQ(final String currentHost){
        id = RandomUtil.getStr(32);
        callbackHosts = currentHost;
        callbackParams = new ConcurrentHashMap<>(16);
    }

    @Override
    public String toString() {
        return "DaoMQ{" +
                "id='" + id + '\'' +
                ", from='" + from + '\'' +
                ", type='" + type + '\'' +
                ", callbackHosts='" + callbackHosts + '\'' +
                ", callbackName='" + callbackName + '\'' +
                ", callbackParams=" + callbackParams +
                ", content='" + content + '\'' +
                '}';
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(final String from) {
        this.from = from;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getCallbackHosts() {
        return callbackHosts;
    }

    public void setCallbackHosts(final String callbackHosts) {
        this.callbackHosts = callbackHosts;
    }

    public String getCallbackName() {
        return callbackName;
    }

    public void setCallbackName(final String callbackName) {
        this.callbackName = callbackName;
    }

    public Map<String, Object> getCallbackParams() {
        return callbackParams;
    }

    public void setCallbackParams(final Map<String, Object> callbackParams) {
        this.callbackParams = callbackParams;
    }

    public <T extends Serializable> T getCallbackParam(final String key){
        Object value = this.callbackParams.get(key);
        T t = (T) value;
        return t;
    }

    public void setCallbackParam(final String key, final Object value) {
        if(StringUtil.isBlank(key)){
            return;
        }
        this.callbackParams.put(key, value);
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

}
