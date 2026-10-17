package xc.crabapple.core.net.ip;

import xc.crabapple.core.servlet.ServletUtil;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 2016-12-09 1.0.0
 */
public final class IPUtil{

    /**
     * 2023-07-12
     */
    public static final String getClientIp(){
        final String clientIp = ServletUtil.getClientIp();
        return clientIp;
    }

    /**
     * 2016-12-09 1.0.0
     * 获得客户端真实ip地址
     * 如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串ip值，究竟哪个才是真正的用户端的真实ip呢？
     * 答案是取X-Forwarded-For中第一个非unknown的有效ip字符串。如：
     * X-Forwarded-For：192.168.1.110, 192.168.1.120, 192.168.1.130, 192.168.1.100
     * 用户真实ip为： 192.168.1.110
     * 每一个Nginx的location节点都应该按照以下配置
     * location = / {
     * 		proxy_pass  http://192.168.1.100:10000;
     * 		proxy_redirect off;
     * 		#host修改为真实的域名和端口
     * 		proxy_set_header Host $http_host;
     * 		#客户端真实ip
     * 		proxy_set_header X-Real-IP $remote_addr;
     * 		proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
     * 		#客户端真实协议(http/https)
     * 		proxy_set_header X-Forwarded-Proto $scheme;
     * }
     */
    public static final String getClientIp(final HttpServletRequest request){
        final String clientIp = ServletUtil.getClientIp(request);
        return clientIp;
    }

    /**
     * 2019-03-18 1.0.3
     * 获取外网ip
     * https://blog.csdn.net/ichenqingyun/article/details/81676244
     */
    public static final String getServerIpOutter(){
        final String ip = ServletUtil.getServerIpOutter();
        return ip;
    }

    /**
     * 2019-03-05 1.0.2
     * 方法1：推荐使用
     * 比较消耗时间，故使用全局变量进行本地缓存
     * see https://blog.csdn.net/gzy_0412/article/details/79916349
     */
    public static final String getServerIp() {
        final String ip = ServletUtil.getServerIp();
        return ip;
    }

    /**
     * 2019-03-05 1.0.2
     * 方法2
     * 比较消耗时间，故使用全局变量进行本地缓存
     * see https://blog.csdn.net/gzy_0412/article/details/79916349
     */
    public static final String getServerIp2() {
        final String serverIp = ServletUtil.getServerIp2();
        return serverIp;
    }

    /**
     * 2016-12-09 1.0.0
     * 获取当前服务器ip地址，此方法仅适用于windows系统
     * 比较消耗时间，故使用全局变量进行本地缓存
     */
    public static final String getServerIpWindows(){
        final String ip = ServletUtil.getServerIpWindows();
        return ip;
    }

    /**
     * 2023-05-17 随机获取一个标准的IPv4，格式如 117.70.41.218
     * 构造IP池
     * https://wen.baidu.com/question/1806978855962267707.html
     */
    public static final String getIPv4(){
        final String ip = ServletUtil.getIPv4();
        return ip;
    }

}