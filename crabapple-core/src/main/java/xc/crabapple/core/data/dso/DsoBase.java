package xc.crabapple.core.data.dso;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.data.CrabappleData;
import xc.crabapple.core.timer.TimerUtil;

/**
 * 2019-11-06 1.0.0
 * 2024-12-26 统一集成
 * Data Service Object - DSO - 数据服务层对象
 * 服务端各Service类之间的数据传递对象，4大属性，TCLE(首字母缩写)
 */
public class DsoBase extends CrabappleData {

    private String type;  //正确或错误等类型
    private String code;  //状态码
    private String location;  //唯一标志服，用以定位
    private String timer;  //
    private Object[] extras;  //对象附加信息，通常是日志

    public DsoBase(){

    }

    public Object[] getExtras() {
        return extras;
    }

    public void setExtras(final Object... extras) {
        this.extras = extras;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public String getTimer() {
        if(StringUtil.isBlank(timer)){
            timer = TimerUtil.now();
        }
        return timer;
    }

    public void setTimer(final String timer) {
        this.timer = timer;
    }

}