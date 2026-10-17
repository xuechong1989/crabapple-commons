package xc.crabapple.core.ocr.baidu2;

import xc.crabapple.core.data.bean.BeanBase;

/**
 * 2020-08-18 6.5.0
 */
public final class OcrBaidu2ConfigBean extends BeanBase {

    private String protocol = "http";  //网络协议，值为http或https，默认为http
    private String host;  //192.168.1.100:8017

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

}
