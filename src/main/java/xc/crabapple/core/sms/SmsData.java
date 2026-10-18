package xc.crabapple.core.sms;

import xc.crabapple.core.data.bean.BeanBase;

/**
 * 短信平台数据
 * 2022-07-05 udpate
 */
public final class SmsData extends BeanBase {

    private boolean success;
    private String code;
    private String message;
    private String validCode;

    public SmsData(){

    }

    public SmsData(final boolean success, final String code, final String message, final String validCode){
        this.success = success;
        this.code = code;
        this.message = message;
        this.validCode = validCode;
    }

    @Override
    public String toString() {
        return "SmsData{" +
                "success=" + success +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", validCode='" + validCode + '\'' +
                '}';
    }

    public boolean getSuccess(){
        return success;
    }

    public void setSuccess(final boolean success){
        this.success = success;
    }

    public String getCode(){
        return code;
    }

    public void setCode(final String code){
        this.code = code;
    }

    public String getMessage(){
        return message;
    }

    public void setMessage(final String message){
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getValidCode() {
        return validCode;
    }

    public void setValidCode(final String validCode) {
        this.validCode = validCode;
    }

}