package xc.crabapple.core.net.http;

import jakarta.servlet.http.HttpServletRequest;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.ListUtil;
import xc.crabapple.core.collection.ObjectUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.net.ip.IPUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.thread.Business;
import xc.crabapple.core.thread.ThreadUtil;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2020-10-04 6.7.0
 * 负载均衡统一处理类
 * 从HttpUtil类中分离而来
 */
public final class BalanceUtil {

    public static final String RANDOM = "random";  //随机算法
    public static final String ROUND = "round";  //轮询算法
    public static final String HASH = "hash";  //哈希算法

    private static Map<String, Integer> p_roundMap = null;  //轮询算法
    private static Map<String, String> p_blackMap = null;

    /**
     * 2020-08-30 6.6.0
     * 负载均衡所有地址可用性检测，监控所有host链接是否可用，每个单独系统初始化时调用，且只允许调用一次
     * 调用示例：
     * String[] array = {"192.168.1.1:8080,192.168.1.1:8081,192.168.1.1:8082", "/api/ms/fs/uploader"};
     * String[] array2 = {"192.168.1.2:8080,192.168.1.2:8081,192.168.1.2:8082", "/api/ms/fs/uploader2"};
     * list.add(array);
     * list.add(array2);
     * HttpUtil.availableChecking(list);
     */
    public static final void init(final HttpServletRequest request){
        //
        p_init(request);
    }

    private static final void p_init(final HttpServletRequest request){
        if(p_blackMap == null){
            p_blackMap = new ConcurrentHashMap<>(1024);
        }else{
            LogUtil.error("BalanceUtil_p_init_1", "重复初始化");
            return;
        }
        ThreadUtil.start(new Business("BalanceUtil_p_init_3", false){
            @Override
            public void process() {
                Long total = 3000L;
                Long per = 3000L;
                if(total == null || ObjectUtil.lts(total, 3000)){
                    total = 3000L;
                }
                if(per == null || ObjectUtil.lts(per, 3000)){
                    per = 3000L;
                }
                while(true){
                    ThreadUtil.sleep(total);  //全部遍历整个黑名单集合一遍之后再次遍历的间隔时间snow2 = TimerUtil.now();
                    if(CollectionUtil.isBlank(p_blackMap)){
                        continue;
                    }
                    Set<String> set = p_blackMap.keySet();
                    Iterator<String> iterator = set.iterator();
                    while(iterator.hasNext()){
                        ThreadUtil.sleep(per);
                        final String key = iterator.next();  //如http://127.0.0.1:100001
                        final String value = p_blackMap.get(key);  //如/xc_crabapple/alpha/available
                        if(StringUtil.isBlank(value)){
                            continue;
                        }
                        final String address = key + value;
                        HttpBean httpBean = new HttpBean();
                        httpBean.setAddress(address);
                        httpBean.setCallType("xc_crabapple_available_scan");
                        try{
                            String result = HttpUtil.request(request, httpBean);
                            if(StringUtil.isBlank(result)){
                                continue;
                            }
                            String tmpValue = p_blackMap.get(key);
                            if(StringUtil.isBlank(tmpValue)){
                                continue;
                            }
                            p_blackMap.remove(key);
                            String host = key;
                            if(StringUtil.containOf(key, "//")){
                                String[] array = StringUtil.split(key, "//");
                                host = array[1];
                            }
                            LogUtil.print("BalanceUtil_p_init_2", host+"已从黑名单中移除，服务已可用");
                        }catch(Throwable throwable){
                            // ignore
                            if(throwable instanceof ConnectException){
                                continue;
                            }
                            LogUtil.error("BalanceUtil_p_init_e1", throwable);
                        }
                    }
                }
            }
        });
    }

    /**
     * 2020-10-05 6.7.0
     * 负载均衡化地址，返回值均有http://
     * ("192.168.1.1:8080,192.168.1.2:8080", "/api/fs/getMd5", null) ==> 192.168.1.2:8080
     * @param protocol 网络协议，值为http或https
     * @param hosts 优先级比address高，二者互斥，格式如192.168.1.1:8080,192.168.1.2:8080
     * @param name
     * @param address 优先级比hosts低，二者互斥，格式如192.168.1.2:8080/api/fs/getMd5
     * @param algorithm 采用的负载均衡算法的种类
     */
    public static final String getAddress(final HttpServletRequest request, final String protocol, final String hosts, final String name, final String address, final String algorithm, final String available, final String callType){
        String callTypeLocal = callType;
        if(StringUtil.isBlank(callTypeLocal)){
            callTypeLocal = "normal";
        }
        //负载均衡
        if(!StringUtil.isBlank(hosts)){
            String host = getHost(request, protocol, hosts, algorithm, available, callTypeLocal);
            if(StringUtil.isBlank(host)){
                return "";
            }
            String addr = host + name;
            if(!StringUtil.startsWith(addr, "http") && !StringUtil.startsWith(address, "https")){
                addr = protocol + "://" + addr;
            }
            return addr;
        }
        //单一调用
        if(StringUtil.isBlank(address)){
            return "";
        }
        if(!StringUtil.startsWith(address, "http") && !StringUtil.startsWith(address, "https")){
            String addr = protocol + "://" + address;
            return addr;
        }
        return address;
    }

    /**
     * 2020-08-30 6.6.0
     * 负载均衡化地址，随机获取某一个可用的有效链接的地址
     * (http://192.168.1.1:8080/api/ms/fs/uploader 或 192.168.1.1:8080/api/ms/fs/uploader) ==> 192.168.1.1:8080
     * @param address
     */
    public static final String getHostByAddress(final String address, final String name){
        String addressLocal = address;
        if(StringUtil.isBlank(addressLocal)){
            return "";
        }
        if(StringUtil.startsWith(addressLocal, "http")){
            String[] array = StringUtil.split(addressLocal, "//");
            addressLocal = array[1];
        }
        String host = StringUtil.deleteLast(addressLocal, name);
        return host;
    }

    /**
     * 2020-12-14 7.1.0
     * 负载均衡核心算法唯一入口
     * @param hosts 格式如192.168.1.1,192.168.1.2,192.168.1.3
     * @param algorithm 负载均衡算法，目前已实现三种 random round hash
     */
    public static final String getHost(final HttpServletRequest request, final String protocol, final String hosts, final String algorithm, final String available, final String callType){
        String hostsLocal = StringUtil.deleteAll(hosts, " ");
        String[] array = StringUtil.split(hostsLocal, ",");
        List<String> list = new ArrayList<>();
        //高可用检测
        if("yes".equals(available)){
            if("normal".equals(callType)){
                for(String host : array){
                    host = StringUtil.deleteAll(host, " ");
                    if(StringUtil.isBlank(host)){
                        continue;
                    }
                    final String key = protocol + "://" + host;
                    String value = p_blackMap.get(key);
                    if(StringUtil.isBlank(value)){
                        list.add(host);
                    }
                }
            }else{
                list = ListUtil.array2list(array);
            }
        }else{
            list = ListUtil.array2list(array);
        }
        if(CollectionUtil.isBlank(list)){
            return "";
        }
        if(StringUtil.isBlank(algorithm)){
            String host = p_getHostByHash(request, list);
            return host;
        }
        if(RANDOM.equals(algorithm)){
            String host = p_getHostByRandom(list);
            return host;
        }
        if(ROUND.equals(algorithm)){
            String host = p_getHostByRound(list);
            return host;
        }
        if(HASH.equals(algorithm)){
            String host = p_getHostByHash(request, list);
            return host;
        }
        String host = p_getHostByHash(request, list);
        return host;
    }

    public static final void addBlack(final String protocol, final String usingHost, final String availableName){
        String protocolLocal = protocol;
        if(StringUtil.isBlank(usingHost)){
            return;
        }
        if(StringUtil.isBlank(protocolLocal)){
            boolean ssl_enabled = false;
            if(ssl_enabled){
                protocolLocal = "https";
            }else{
                protocolLocal = "http";
            }
        }
        final String key = protocolLocal + "://" + usingHost;
        p_blackMap.put(key, availableName);
    }

    /**
     * 2020-08-29 6.6.0
     * 方式1 随机算法，代表常量为random
     * 负载均衡化地址，随机获取某一个可用的有效链接的地址
     * (192.168.1.1:8080, 192.168.1.2:8080) ==> 192.168.1.2:8080
     */
    private static final String p_getHostByRandom(final List<String> list){
        int size = list.size();
        int index = RandomUtil.getInt(0, size - 1);
        String host = list.get(index);
        return host;
    }

    /**
     * 2020-12-14 7.1.0
     * 方式2 轮训算法，代表常量为round
     */
    private static final String p_getHostByRound(final List<String> list){
        if(p_roundMap == null){
            p_roundMap = new HashMap<>(64);
        }
        int size = list.size();
        //
        String content = "";
        for(int i = 0; i < size; i++){
            if(i == 0){
                content = list.get(i);
            }else{
                content += "," + list.get(i);
            }
        }
        Integer pos = p_roundMap.get(content);
        if(pos == null){
            pos = 0;
        }else if(pos >= size){
            pos = 0;
        }
        String host = list.get(pos);
        pos++;
        p_roundMap.put(content, pos);
        return host;
    }

    /**
     * 2020-12-14 7.1.0
     * 方式3 哈希算法，代表常量为hash
     */
    private static final String p_getHostByHash(final HttpServletRequest request, final List<String> list){
        String clientIp = IPUtil.getClientIp(request);
        if(StringUtil.isBlank(clientIp)){
            LogUtil.error("BalanceUtil_getHostByHash_1", "客户端ip获取失败，负载均衡失效，系统已采用地址列表第一个值");
            String host = list.get(0);
            return host;
        }
        int hashCode = clientIp.hashCode();
        if(hashCode < 0){
            hashCode = 0 - hashCode;
        }
        int size = list.size();
        int pos = hashCode % size;  //取余
        String host = list.get(pos);
        return host;
    }

}
