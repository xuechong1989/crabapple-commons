package xc.crabapple.core.thread;

import xc.crabapple.core.log.LogUtil;

/**
 * 2025-11-01
 * 多线程下业务类
 */
public class Business {

    private String location = null;  // 业务定位符
    private Object data = null;  //
    private Boolean showLog = null;

    public Business(final String location) {
        this.location = location;
        this.data = null;
        this.showLog = true;
    }

    public Business(final String location, final Boolean showLog) {
        this.location = location;
        this.data = null;
        this.showLog = showLog;
    }

    public Business(final String location, final Object data, final Boolean showLog) {
        this.location = location;
        this.data = data;
        this.showLog = showLog;
    }

    /**
     * 2025-11-01
     * 业务过程
     */
    public void process(){
        LogUtil.error("Business_process_1", "必须重写该方法", location, showLog);
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public Object getData() {
        return data;
    }

    public void setData(final Object data) {
        this.data = data;
    }

    public Boolean getShowLog() {
        return showLog;
    }

    public void setShowLog(final Boolean showLog) {
        this.showLog = showLog;
    }

}