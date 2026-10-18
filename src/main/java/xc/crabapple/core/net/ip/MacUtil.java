package xc.crabapple.core.net.ip;

import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2017-04-26 1.0.0
 */
public final class MacUtil{

    //操作系统
    public static final String WINDOWS = "windows";
    public static final String LINUX = "linux";

    /**
     * 2018-10-11 1.0.0
     * 推荐使用
     * 获取网卡MAC地址，仅区分无线和有线
     * 此方法结果与命令行下的物理地址的值相同
     * MAC地址是网卡决定的，是固定的。
     * @param ip IP地址，格式如192.168.1.101（注意：不能写127.0.0.1或localhost）
     */
    public static final String getMacAddress(final String ip){
        List<byte[]> list = new ArrayList<>();
        try{
            //指定ip的必须保留，故必须添加上
            InetAddress inetAddress = InetAddress.getByName(ip);
            if(inetAddress != null){
                NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);
                if(networkInterface != null){
                    byte[] macs = networkInterface.getHardwareAddress();
                    if(!CollectionUtil.isBlank(macs)){
                        list.add(macs);
                    }else{
                        list.add(null);
                    }
                }else{
                    list.add(null);
                }
            }else{
                list.add(null);
            }
            //
            Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            while(networkInterfaceEnumeration.hasMoreElements()){
                NetworkInterface networkInterface2 = networkInterfaceEnumeration.nextElement();
                if(networkInterface2 == null){
                    continue;
                }
                byte[] macs = networkInterface2.getHardwareAddress();
                if(!CollectionUtil.isBlank(macs)){
                    list.add(macs);
                }
            }
            if(CollectionUtil.isBlank(list)){
                LogUtil.error("MacUtil_getMacAddress_1", ip);
                return "xxxxxx";
            }
            Map<String, String> map = new HashMap<>();  //去重
            StringBuilder macAddressBuilder = new StringBuilder();
            for(int j = 0; j < list.size(); j++){
                byte[] macs = list.get(j);
                String mac = "";
                if(j == 0){
                    if(CollectionUtil.isBlank(macs)){
                        mac = "xxxxxx";
                    }
                }
                if(!CollectionUtil.isBlank(macs)){
                    StringBuilder stringBuilder = new StringBuilder("");
                    for(int i = 0; i < macs.length; i++){
                        if(i != 0){
                            stringBuilder.append("-");
                        }
                        int temp = macs[i] & 0xff;  //字节转换为整数
                        String str = Integer.toHexString(temp);
                        int n = str.length();
                        if(n == 1){
                            stringBuilder.append("0").append(str);
                        }else {
                            stringBuilder.append(str);
                        }
                    }
                    mac = stringBuilder.toString();
                }
                String tmp = map.get(mac);
                if(StringUtil.isBlank(tmp)){
                    map.put(mac, "yes");
                }else{
                    continue;
                }
                if(j == 0){
                    macAddressBuilder.append(mac);
                }else{
                    macAddressBuilder.append(",").append(mac);
                }
            }
            String macAddress = macAddressBuilder.toString();
            return macAddress;
        }catch(Throwable throwable){
            LogUtil.error("MacUtil_getMacAddress_t1", throwable, ip);
            return "xxxxxx";
        }
    }

    /**
     * 2017-04-26 1.0.0
     * 不推荐使用
     * 2018-10-11 整理（方法名修改，删除参数）
     * 获取网卡MAC地址，仅区分网段（前三段IP必须相同，如192.168.1.xxx），
     * 由于要通过操作系统指令获取，此方法执行速度较慢
     * MAC地址是网卡决定的，是固定的。
     */
    public static final String getMacAddressARP(){
        try{
            InetAddress inetAddress = InetAddress.getLocalHost();
            String ip = inetAddress.getHostAddress();
            String macAddress = getMacAddressARP(ip);
            return macAddress;
        }catch(Throwable throwable){
            LogUtil.error("MacUtil_getMacAddressARP_t1", throwable);
        }
        return null;
    }

    /**
     * 2017-04-26 1.0.0
     * 2018-10-11 整理（方法名修改，删除参数）
     * 不推荐使用
     * 获取网卡MAC地址，仅区分网段（前三段IP必须相同，如192.168.1.xxx），
     * 由于要通过操作系统指令获取，此方法执行速度较慢
     * MAC地址是网卡决定的，是固定的。
     */
    public static final String getMacAddressARP(final String ip){
        boolean flag = isLinux();
        if(flag){
            String macAddress = getMacInLinux(ip);
            macAddress = macAddress.trim();
            return macAddress;
        }
        flag = isWindows();
        if(flag){
            String macAddress = getMacInWindows(ip);
            macAddress = macAddress.trim();
            return macAddress;
        }
        return null;
    }

    /**
     * 2017-04-26 1.0.0
     * 获取MAC地址
     */
    public static final String callCmd(final Object[] cmd){
        if(cmd == null){
            return "";
        }
        StringBuilder resultBuilder = new StringBuilder();
        String line = "";
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try{
            Runtime runtime = Runtime.getRuntime();
            String[] strCmd = new String[cmd.length];
            for(int i = 0; i < cmd.length; i++){
                strCmd[i] = cmd[i].toString();
            }
            Process process = runtime.exec(strCmd);
            inputStream = process.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            while((line = bufferedReader.readLine()) != null){
                resultBuilder.append(line);
            }
            process.waitFor();
            process.destroy();
        }catch(Throwable throwable){
            LogUtil.error("MacUtil_callCmd_t1", throwable, cmd);
        }finally {
            IOUtil.close(bufferedReader, inputStreamReader, inputStream);
        }
        String result = resultBuilder.toString();
        return result;
    }

    /**
     * 2017-04-26 1.0.0
     * @param cmd     第一个命令
     * @param another 第二个命令
     * @return 第二个命令的执行结果
     */
    public static final String callCmd(final String[] cmd, final String[] another){
        StringBuilder resultBuilder = new StringBuilder();
        String line = "";
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try{
            Runtime runtime = Runtime.getRuntime();
            Process process1 = runtime.exec(cmd);
            process1.waitFor();
            process1.destroy();
            Process process2 = runtime.exec(another);
            inputStream = process2.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            while((line = bufferedReader.readLine()) != null){
                resultBuilder.append(line);
            }
            process2.waitFor();
            process2.destroy();
        }catch(Throwable throwable){
            LogUtil.error("MacUtil_callCmd_t2", throwable, cmd, another);
        }finally {
            IOUtil.close(bufferedReader, inputStreamReader, inputStream);
        }
        String result = resultBuilder.toString();
        return result;
    }

    /**
     * 2017-04-26 1.0.0
     * @param ip 目标ip,一般在局域网内
     * @param sourceString 命令处理的结果字符串
     * @param macSeparator mac分隔符号
     * @return mac地址，用上面的分隔符号表示
     */
    public static final String filterMacAddress(final String ip, final String sourceString, final String macSeparator){
        String result = "";
        String regExp = "((([0-9,A-F,a-f]{1,2}" + macSeparator + "){1,5})[0-9,A-F,a-f]{1,2})";
        Pattern pattern = Pattern.compile(regExp);
        Matcher matcher = pattern.matcher(sourceString);
        while(matcher.find()){
            result = matcher.group(1);
            if(sourceString.indexOf(ip) <= sourceString.lastIndexOf(matcher.group(1))){
                break; //如果有多个IP,只匹配本IP对应的Mac.
            }
        }
        return result;
    }

    /**
     * 2017-04-26 1.0.0
     * @param ip 目标ip
     * @return Mac Address
     */
    public static final String getMacInWindows(final String ip){
        String result = "";
        String[] cmd = {
                "cmd",
                "/c",
                "ping " + ip
        };
        String[] another = {
                "cmd",
                "/c",
                "arp -a"
        };
        String cmdResult = callCmd(cmd, another);
        result = filterMacAddress(ip, cmdResult, "-");
        return result;
    }

    /**
     * 2017-04-26 1.0.0
     * @param ip 目标ip
     * @return Mac Address
     */
    public static final String getMacInLinux(final String ip){
        String result = "";
        String[] cmd = {
                "/bin/sh",
                "-c",
                "ping " + ip + " -c 2 && arp -a"
        };
        String cmdResult = callCmd(cmd);
        result = filterMacAddress(ip, cmdResult, ":");
        return result;
    }

    /**
     * 2017-03-09 1.0.0
     * 例如：windows 7
     */
    private static final String getName(){
        String name = System.getProperty("os.name");
        name = name.toLowerCase();
        return name;
    }

    /**
     * 2017-03-09 1.0.0
     * 当前运行的操作系统是否为windows
     */
    private static final boolean isWindows(){
        String name = getName();
        if(name.indexOf(WINDOWS) != -1){
            return true;
        }
        return false;
    }

    /**
     * 2017-03-09 1.0.0
     * 当前运行的操作系统是否为linux
     */
    private static final boolean isLinux(){
        String name = getName();
        if(name.indexOf(LINUX) != -1){
            return true;
        }
        return false;
    }

    /**
     * 2018-02-13 1.0.0
     * @param obj
     */
    private static final boolean isBlank(final Object obj){
        if(obj == null){
            return true;
        }
        if(obj instanceof Integer){
            Integer myint = (Integer)obj;
            if(myint == 0){
                return true;
            }
        }else if(obj instanceof Long){
            Long mylong = (Long)obj;
            if(mylong == 0L){
                return true;
            }
        }else if(obj instanceof String){
            if("".equals(obj.toString())){
                return true;
            }
        }else if(obj instanceof byte[]){
            byte[] array = (byte[])obj;
            if(array.length <= 0){
                return true;
            }
        }else if(obj instanceof String[]){
            String[] array = (String[])obj;
            if(array.length <= 0){
                return true;
            }
        }else if(obj instanceof Map){
            Map map = (Map)obj;
            if(map.size() <= 0){
                return true;
            }
        }else if(obj instanceof List){
            List list = (List)obj;
            if(list.isEmpty()){
                return true;
            }
        }
        return false;
    }

}
