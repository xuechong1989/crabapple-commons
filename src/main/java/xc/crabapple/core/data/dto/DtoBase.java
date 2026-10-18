package xc.crabapple.core.data.dto;

import xc.crabapple.core.data.CrabappleData;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.timer.TimerUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 2019-11-06 1.0.0
 * 2024-12-26 统一集成
 * 2025-01-24 统一重新设置默认值
 * Data Transfer Object - DTO - 数据传输对象
 * 前后端数据交互标准格式
 */
public class DtoBase extends CrabappleData {

    private String clientId = null;
    private String success = "yes";
    private String message = "操作成功";
    private String code = "success";
    private String cipher = "yes";
    private String timer = null;
    private String version = "0.0.00";  // 2025-08-07 add
    private Map<String, Object> params = null;
    
    public DtoBase(){

    }

    public void setParam(final String key, final Object value){
        if(params == null){
            params = new HashMap<>();
        }
        params.put(key, value);
    }

    public String getClientId() {
        if(clientId == null || "".equals(clientId)){
            clientId = RandomUtil.getStrWithStamp(45, true);
        }
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public String getTimer() {
        if(timer == null || "".equals(timer)){
            // 2026-08-12 系统与所有数据库表均统一时间格式 去毫秒
            timer = TimerUtil.now(TimerUtil.FORMATTYPE1);
        }
        return timer;
    }

    public void setTimer(final String timer) {
        this.timer = timer;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getSuccess() {
        return success;
    }

    public void setSuccess(final String success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(final Map<String, Object> params) {
        this.params = params;
    }

    public String getCipher() {
        return cipher;
    }

    public void setCipher(final String cipher) {
        this.cipher = cipher;
    }

}