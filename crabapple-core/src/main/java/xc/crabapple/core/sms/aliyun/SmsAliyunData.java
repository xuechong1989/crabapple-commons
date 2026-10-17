package xc.crabapple.core.sms.aliyun;

import java.io.Serializable;

/**
 * 2026-10-14 接入阿里云短信平台
 * 阿里云短信平台发送结果数据
 */
public final class SmsAliyunData implements Serializable {

    public static final long serialVersionUID = 1L;

    private boolean success;
    private String code;
    private String message;
    private String validCode;

    public SmsAliyunData(){
    }

    public SmsAliyunData(boolean success, String code, String message, String validCode){
        this.success = success;
        this.code = code;
        this.message = message;
        this.validCode = validCode;
    }

    public boolean getSuccess(){
        return success;
    }

    public void setSuccess(boolean success){
        this.success = success;
    }

    public String getCode(){
        return code;
    }

    public void setCode(String code){
        this.code = code;
    }

    public String getMessage(){
        return message;
    }

    public void setMessage(String message){
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getValidCode() {
        return validCode;
    }

    public void setValidCode(String validCode) {
        this.validCode = validCode;
    }

}
