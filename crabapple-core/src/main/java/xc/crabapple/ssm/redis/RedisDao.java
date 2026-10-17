package xc.crabapple.ssm.redis;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.data.dao.DaoBase;
import xc.crabapple.core.json.jackson.JacksonUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;

import java.io.Serializable;

/**
 * 2020-05-29 6.0.0
 */
public final class RedisDao extends DaoBase {

    private String id;
    private String serverId = "";
    private String key;  // 非空对象，非空字符串，key的命名规则： 1只含有两个下划线。2不得包含中文，中英文逗号，空格。 3标准格式：项目名_应用场景名_具体业务数据，如xc_crabapple_ws_xxxxxx
    private Serializable content;  // 实际内容，推荐使用xc_crabapple中的dco基类存入
    private long duration = 0L;  // 有效存储时间，0表示永久有效，单位秒
    private String cipher = "no";  // 是否加密，空对象或空字符串或no表示不加密， aes-128表示AES-128方式加密
    private String cipherKey = "crabapple";  // 加密码
    //非配置
    private String ip;  // 存入时的调用方ip
    private String saveTime;  // 存入时的时间点，精确到毫秒，格式如2020-06-15 17:35:58:123

    public RedisDao(){
        this.id = RandomUtil.getStr(32);
        this.ip = "127.0.0.1";
    }

    public RedisDao(final String ip){
        this.id = RandomUtil.getStr(32);
        if(StringUtil.isBlank(ip)){
            this.ip = "127.0.0.1";
        }else{
            this.ip = ip;
        }
    }

    public RedisDao(final String id, final String ip){
        if(StringUtil.isBlank(id)){
            this.id = RandomUtil.getStr(32);
        }else{
            this.id = id;
        }
        if(StringUtil.isBlank(ip)){
            this.ip = "127.0.0.1";
        }else{
            this.ip = ip;
        }
    }

    @Override
    public String toString(){
        String json = JacksonUtil.obj2json(this);
        return json;
    }

    public String getServerId() { return serverId; }
    public void setServerId(final String serverId) { this.serverId = serverId; }
    public String getKey() { return key; }
    public void setKey(final String key) { this.key = key; }
    public String getSaveTime() { return saveTime; }
    public void setSaveTime(final String saveTime) { this.saveTime = saveTime; }

    public long getDuration() {
        if(duration <= 0){
            duration = Integer.MAX_VALUE;
        }
        return duration;
    }

    public void setDuration(final long duration) {
        long localDuration = duration;
        if(localDuration <= 0){
            localDuration = Integer.MAX_VALUE;
        }
        this.duration = localDuration;
    }

    public Serializable getContent() {
        if(content == null){
            return null;
        }
        return content;
    }

    public void setContent(final Serializable content) {
        if(content == null){
            LogUtil.error("RedisDao_setContent_1", "content值为空，存储失败");
            return;
        }
        this.content = content;
    }

    public String getCipher() { return cipher; }
    public void setCipher(final String cipher) { this.cipher = cipher; }
    public String getCipherKey() { return cipherKey; }
    public void setCipherKey(final String cipherKey) { this.cipherKey = cipherKey; }
    public String getIp() { return ip; }
    public void setIp(final String ip) { this.ip = ip; }

}
