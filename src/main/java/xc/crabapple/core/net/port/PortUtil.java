package xc.crabapple.core.net.port;

import xc.crabapple.core.log.LogUtil;

import javax.management.AttributeValueExp;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;
import javax.management.StringValueExp;
import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.Set;

/**
 * 2025-01-09
 * 服务器端口管理工具类
 */
public class PortUtil {

    /**
     * 2025-01-09
     * 获取当前服务端口
     * 该方法不适用于springboot项目
     * @return
     */
    public static final String getPort() {
        try {
            StringBuilder sb = new StringBuilder();
            ObjectName objectName = new ObjectName("*:type=Connector,*");
            AttributeValueExp attributeValueExp = Query.attr("protocol");
            StringValueExp stringValueExp = Query.value("HTTP/1.1");
            QueryExp queryExp = Query.match(attributeValueExp, stringValueExp);
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            Set<ObjectName> objectNameSet = mBeanServer.queryNames(objectName, queryExp);
            Iterator<ObjectName> iterator = objectNameSet.iterator();
            while(iterator.hasNext()){
                ObjectName keyObjectName = iterator.next();
                String portStr = keyObjectName.getKeyProperty("port");
                if(sb.length() == 0){
                    sb.append(portStr);
                    continue;
                }
                sb.append(",").append(portStr);
            }
            String port = sb.toString();
            return port;
        } catch (Throwable throwable) {
            LogUtil.error("PortUtil_getPort_t1", throwable);
            return "-1";
        }
    }

}