package xc.crabapple.ssm.security.sameorigin;

import jakarta.servlet.http.HttpServletRequest;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.servlet.ServletUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 2025-01-15
 * 同源检测管理工具类
 * 1. 防止跨站请求伪造攻击CSRF
 */
public class SameoriginUtil {

    private static String p_enable = "yes";
    private static List<String> p_sameoriginList = null;

    /**
     * 2025-01-15
     *
     * @param enable 同源检测机制开关，优先级最高，yes=开启校验，no=关闭校验。
     *
     * @param originInfo 客户端页面服务器白名单（实际的客户端服务器所在机器的IP地址或域名）。
     *                    服务器ip1, 服务器ip2, ......
     *                    格式举例：192.168.1.100, example.com, *
     *                    多个域以英文逗号,分割，里面是标准的ipv4或域名，*表示不做限制。
     */
    public static final void init(final String enable, final String originInfo){
        if(!"yes".equals(enable)){
            p_enable = "no";
            LogUtil.print("SameoriginUtil_init_5", "同源检测机制禁用", enable, originInfo);
            return;
        }
        p_enable = enable;
        LogUtil.print("SameoriginUtil_init_1", "同源检测机制开启", enable, originInfo);
        // 客户端页面服务器信息
        p_sameoriginList = new ArrayList<>(16);
        String localOriginInfo = StringUtil.getPureStr(originInfo);
        localOriginInfo = StringUtil.deleteAll(localOriginInfo, " ");
        String[] array = StringUtil.split(localOriginInfo, ",");
        if(!CollectionUtil.isBlank(array)){
            for(String server : array){
                p_sameoriginList.add(server);
            }
        }
    }

    /**
     * 2025-01-15
     * @return
     */
    public static final String check(){
        final String result = check(null, null);
        return result;
    }

    /**
     * 2023-08-29
     */
    public static final String check(final String clientIp){
        final String result = check(null, clientIp);
        return result;
    }

    /**
     * 2023-08-11
     * 检查客户端的http请求头中的origin/referer字段是否合法（客户端未传，传递的值不在白名单）
     */
    public static final String check(final HttpServletRequest request, final String clientIp){
        if(!"yes".equals(p_enable)){
            return "success";
        }
        //
        HttpServletRequest localRequest = request;
        if(localRequest == null){
            localRequest = ServletUtil.getRequest();
        }
        // 客户端页面服务器信息
        final String realOrigin = localRequest.getHeader("Origin");
        final String realReferfer = localRequest.getHeader("Referer");
        for(String origin : p_sameoriginList){
            if("*".equals(origin)){
                return "success";
            }
            if(StringUtil.startsWith(realOrigin, origin)){
                return "success";
            }
            if(StringUtil.startsWith(realReferfer, origin)){
                return "success";
            }
        }
        //
        return "sameorigin_error";
    }

}
