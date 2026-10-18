package xc.crabapple.core.net.tcp;

import xc.crabapple.core.log.LogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2018-10-24 1.0.0
 * TCP连接及数据交互时粘包处理工具类（粘包即半包）
 * 注意：本类是基于字符串收发数据为前提的
 */
public final class StickyPackUtil {

    private static Map<String, String> p_map = null;

    /**
     * 2018-10-24 1.0.0
     * 每一个完整的逻辑消息在最开始发送时：就以换行符\n结尾
     * @param clientId 每个客户端连接就是一个clientId，作为每个客户端的唯一标志
     * @param msg 一条可能粘包的网络信息
     * @return 返回值：要么是完整的一条信息，要么为null即空对象
     */
    public static final List<String> unpack(final String clientId, final String msg){
        if(p_map == null){
            p_map = new ConcurrentHashMap<>(16);
        }
        List<String> list = new ArrayList<>();
        String currentMsg = p_map.get(clientId);
        if(currentMsg == null){
            currentMsg = msg;
            p_map.put(clientId, currentMsg);
        }else{
            currentMsg = new StringBuilder(currentMsg).append(msg).toString();
        }
        int n = currentMsg.length();
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < n; i++){
            char c = currentMsg.charAt(i);
            if(c != '\n'){
                stringBuilder.append(c);
                String str = stringBuilder.toString();
                p_map.put(clientId, str);
            }else{
                String line = stringBuilder.toString();
                list.add(line);
                stringBuilder = stringBuilder.delete(0, stringBuilder.length());
                p_map.remove(clientId);
            }
        }
        if(list.size() == 0){
            list = null;
        }
        return list;
    }

    /**
     * 2018-10-24 1.0.0
     * 测试程序，输出结果如下：
     [1abcdefg]
     null
     [1你好啊]
     [2abcdefg]
     [2你好啊]
     [3abcdefg, 4abcdefg]
     [5abcdefg]
     [3你好啊, 4你好啊]
     [6abcdefg, 7abcdefg, 8abcdefg]
     [5你好啊, 6你好啊]
     [9abcdefg]
     */
    public static final void testing(){
        List<String> list = unpack("aa", "1abcdefg\n");
        LogUtil.print("StickyPackUtil_testing_1", list);
        list = unpack("aa", "2abcdefg");
        LogUtil.print("StickyPackUtil_testing_2", list);
        list = unpack("bb", "1你好啊\n2你");
        LogUtil.print("StickyPackUtil_testing_3", list);
        list = unpack("aa", "\n3abcdefg");
        LogUtil.print("StickyPackUtil_testing_4", list);
        list = unpack("bb", "好啊\n3你好啊");
        LogUtil.print("StickyPackUtil_testing_5", list);
        list = unpack("aa", "\n4abcdefg\n");
        LogUtil.print("StickyPackUtil_testing_6", list);
        list = unpack("aa", "5abcdefg\n6abc");
        LogUtil.print("StickyPackUtil_testing_7", list);
        list = unpack("bb", "\n4你好啊\n5你");
        LogUtil.print("StickyPackUtil_testing_8", list);
        list = unpack("aa", "defg\n7abcdefg\n8abcdefg\n9abcdef");
        LogUtil.print("StickyPackUtil_testing_9", list);
        list = unpack("bb", "好啊\n6你好啊\n");
        LogUtil.print("StickyPackUtil_testing_10", list);
        list = unpack("aa", "g\n");
        LogUtil.print("StickyPackUtil_testing_11", list);
    }

}
