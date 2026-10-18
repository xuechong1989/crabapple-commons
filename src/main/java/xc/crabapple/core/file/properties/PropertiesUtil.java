package xc.crabapple.core.file.properties;

import xc.crabapple.core.cipher.md5.MD5Util;
import xc.crabapple.core.cipher.sm4.Sm4Util;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.device.EnvironmentUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.thread.Business;
import xc.crabapple.core.thread.ThreadUtil;
import xc.crabapple.core.tomcat.TomcatUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2017-03-10 1.0.0
 * 2024-07-15 新增加载路由conf文件机制，是产品型系统properties启动标准机制。
 * 2024-07-17 新增热更新properties文件
 * 2026-04-20 兼容单元测试环境，新增当前程序运行环境的根目录的参数
 */
public final class PropertiesUtil {

    public static final String ROUTER_SPLIT = "==>";
    private static String p_router_propertiesUrl = "";  // router机制下，当前工程所对应的properties文件地址
    private static Map<String, List<String>> p_propertiesListMap = null;
    private static Map<String, String> p_propertiesMap = null;

    /**
     * 2024-07-15
     * 通过路由conf文件（该文件本身为明文文件），加载properties配置文件
     * @param routerUrl 路由conf文件绝对路径，如/data/crabapple/router.conf
     */
    public static final String loadPropertiesByRouter(final String routerUrl){
        final String result = loadPropertiesByRouter(routerUrl, null);
        return result;
    }

    /**
     * 2024-07-10
     * @param routerUrl
     * @param sm4key
     * @return
     */
    public static final String loadPropertiesByRouter(final String routerUrl, final String sm4key){
        final String currentRootPath = TomcatUtil.getTomcatRootPath();
        final String result = loadPropertiesByRouter(routerUrl, currentRootPath, sm4key);
        return result;
    }

    /**
     * 2024-07-10
     * 2026-04-20 新增参数currentRootPath
     * 通过路由conf文件（路由conf文件本身为明文文件），加载properties配置文件
     * 是否解密的2个条件需同时满足：1.properties文件名以-security结尾。2.sm4秘钥sm4key值不为空对象或空字符串。
     * @param routerUrl 路由conf文件绝对路径，如 /data/crabapple/router.conf
     * @param currentRootPath 当前程序运行环境的根目录，如 d:/app/project
     * @param sm4key 路由conf文件中对应的properties文件的sm4解密秘钥，若为空对象或空字符串则无需解密
     */
    public static final String loadPropertiesByRouter(String routerUrl, String currentRootPath, final String sm4key){
        p_initMap();
        // 2026-05-29 路径兼容windows
        if(StringUtil.startsWith(routerUrl, "/")){
            if(EnvironmentUtil.isWindows()){
                routerUrl = "d:" + routerUrl;
            }
        }
        final boolean exists = FileUtil.exists(routerUrl);
        if(!exists) {
            LogUtil.println("PropertiesUtil_loadPropertiesByRouter_1", "路由conf文件不存在，请尽快配置", routerUrl, currentRootPath);
            return "error@rounter_conf_file_no_found";
        }
        //
        List<String> confList = FileUtil.fileReadList(routerUrl);
        boolean propertiesFlag = false;
        for(int i = 0; i < confList.size(); i++){
            String line = confList.get(i);
            line = StringUtil.getPureStr(line);
            line = StringUtil.trim(line);
            if(StringUtil.isBlank(line)){
                continue;
            }
            if(StringUtil.startsWith(line, "#")){
                continue;
            }
            final String[] array = StringUtil.split(line, ROUTER_SPLIT);
            String keyRootPath = array[0];
            keyRootPath = StringUtil.toLowerCaseFirst(keyRootPath);
            keyRootPath = StringUtil.trim(keyRootPath);
            if(StringUtil.isBlank(currentRootPath)){
                currentRootPath = TomcatUtil.getTomcatRootPath();
            }
            if(!StringUtil.equals(currentRootPath, keyRootPath)){
                continue;
            }
            String value = array[1];
            p_router_propertiesUrl = StringUtil.trim(value);
            if(!FileUtil.isRealFile(p_router_propertiesUrl)){
                LogUtil.error("PropertiesUtil_loadPropertiesByRouter_2", "properties文件不存在，请尽快配置", routerUrl, p_router_propertiesUrl);
                return "error@properties_file_no_found";
            }
            propertiesFlag = true;
            p_loadProperties(sm4key);
            break;
        }
        if(propertiesFlag){
            return "ok";
        }
        LogUtil.error("PropertiesUtil_loadPropertiesByRouter_3", "properties文件不存在，请尽快配置", routerUrl);
        return "error@properties_no_init";
    }

    /**
     * 2024-06-12
     * tomcat启动时执行，将文件流解密的数据装进properties里面
     * @param sm4key 路由conf文件中对应的properties文件的sm4解密秘钥，若为空对象或空字符串则无需解密
     */
    private static final String p_loadProperties(final String sm4key) {
        InputStream inputStream = null;
        ByteArrayInputStream byteArrayInputStream = null;
        Properties properties = new Properties();
        try{
            File file = new File(p_router_propertiesUrl);
            Path path = file.toPath();
            inputStream = Files.newInputStream(path);
            boolean isDecrypt = false;
            String filename = FileUtil.getFileName(p_router_propertiesUrl, false);
            if(StringUtil.lastsWith(filename, "-security")){
                if(!StringUtil.isBlank(sm4key)){
                    isDecrypt = true;
                }
            }
            // 解密
            if(isDecrypt){
                byte[] bytes = Sm4Util.decrypt(sm4key, inputStream);
                byteArrayInputStream = new ByteArrayInputStream(bytes);
                properties.load(byteArrayInputStream);
                loadPropertiesObject(properties);
                return "ok";
            }
            // 不解密
            properties.load(inputStream);
            loadPropertiesObject(properties);
            return "ok";
        }catch(Throwable throwable){
            LogUtil.error("PropertiesUtil_p_loadProperties_t1", throwable, "properties文件解密失败", p_router_propertiesUrl);
            return "error";
        } finally {
            IOUtil.close(byteArrayInputStream, inputStream);
        }
    }

    /**
     * 2024-07-10
     * 更新路由conf文件
     * @param newLine
     */
    public static final String updateRouterConf(final String routerUrl, final String newLine){
        String[] arr = StringUtil.split(newLine, ROUTER_SPLIT);
        String key = arr[0];
        key = StringUtil.getPureStr(key);
        key = StringUtil.trim(key);
        boolean exist = false;
        String status = "ok@insert";
        //
        StringBuilder stringBuilder = new StringBuilder();
        List<String> confList = FileUtil.fileReadList(routerUrl);
        for(int i = 0; i < confList.size(); i++){
            final String line = confList.get(i);
            String pureLine = StringUtil.getPureStr(line);
            pureLine = StringUtil.trim(pureLine);
            if(StringUtil.isBlank(pureLine)){
                stringBuilder.append(line).append("\n");
                continue;
            }
            if(StringUtil.startsWith(pureLine, "#")){
                stringBuilder.append(line).append("\n");
                continue;
            }
            final String[] array = StringUtil.split(pureLine, ROUTER_SPLIT);
            String tomcatRootPath = array[0];
            tomcatRootPath = StringUtil.trim(tomcatRootPath);
            // 存在就更新，不存在就忽略，最后再追加
            if(StringUtil.equals(key, tomcatRootPath)){
                exist = true;
                status = "ok@update";
                stringBuilder.append(newLine).append("\n");
                continue;
            }
            stringBuilder.append(line).append("\n");
        }
        if(!exist){
            stringBuilder.append(newLine).append("\n");
        }
        final String result = stringBuilder.toString();
        FileUtil.fileWriteNew(routerUrl, result, false);
        // insert / update
        return status;
    }

    /**
     * 2024-07-17
     * @param propertiesUrl
     * @return
     */
    public static final boolean loadPropertiesFile(final String propertiesUrl){
        boolean success = loadPropertiesFile(propertiesUrl, false);
        return success;
    }

    /**
     * 2020-10-02 6.7.0
     * 初始化properties文件，具有重写的功能
     * @param propertiesUrl 指定properties文件的相对路径（不含该文件扩展名）
     * @param hotUpdate 是否启用热更新，true-启用，false-不启用。更详细的配置，请在工程中直接调用方法hotUpdatePropertiesFile
     */
    public static final boolean loadPropertiesFile(final String propertiesUrl, final boolean hotUpdate){
        if(StringUtil.isBlank(propertiesUrl)){
            LogUtil.error("PropertiesUtil_loadPropertiesFile_1", "properties文件地址为空", propertiesUrl);
            return false;
        }
        //
        p_initMap();
        //
        boolean flag = p_loadPropertiesFile(propertiesUrl, hotUpdate);
        if(flag){
            return true;
        }
        LogUtil.error("PropertiesUtil_loadPropertiesFile_2", propertiesUrl);
        return false;
    }

    /**
     * 2020-10-02 6.7.0
     * @param propertiesUrl 指定properties文件的相对路径（不含该文件扩展名）
     * @param hotUpdate 是否启用热更新，true-启用，false-不启用。更详细的配置，请在工程中直接调用方法hotUpdatePropertiesFile
     * @return
     */
    private static final boolean p_loadPropertiesFile(final String propertiesUrl, final boolean hotUpdate){
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        try{
            ClassLoader classLoader = PropertiesUtil.class.getClassLoader();
            inputStream = classLoader.getResourceAsStream(propertiesUrl);
            if(inputStream == null){
                inputStream = FileUtil.file2inputStream(propertiesUrl);
            }
            if(inputStream == null){
                return false;
            }
            inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            Properties properties = new Properties();
            properties.load(inputStreamReader);
            //
            Set<Object> set = properties.keySet();
            Iterator<Object> iterator = set.iterator();
            while(iterator.hasNext()){
                Object object = iterator.next();
                String key = (String) object;
                String value = properties.getProperty(key);
                value = StringUtil.getPureStr(value);
                value = StringUtil.trim(value);
                if(value == null){
                    value = "";
                }else if(StringUtil.containOf(value, " #")){
                    String[] array = StringUtil.split(value, " #");
                    value = array[0];
                    value = StringUtil.trim(value);
                }
                //
                p_propertiesMap.put(key, value);
            }
            //
            if(hotUpdate){
                hotUpdatePropertiesFile(propertiesUrl);
            }
            //
            return true;
        }catch(Throwable throwable){
            LogUtil.error("PropertiesUtil_p_loadPropertiesFile_t1", throwable, propertiesUrl);
            return false;
        }finally {
            IOUtil.close(inputStreamReader, inputStream);
        }
    }

    /**
     * 2024-06-11
     * 根据properties属性加载
     * @param properties 属性配置
     * @return boolean
     */
    public static final boolean loadPropertiesObject(final Properties properties){
        if(properties == null){
            LogUtil.error("PropertiesUtil_loadPropertiesObject_1", "properties为空");
            return false;
        }
        //
        p_initMap();
        //
        boolean flag = p_loadPropertiesObject(properties);
        if(flag){
            return true;
        }
        LogUtil.error("PropertiesUtil_loadPropertiesObject_2");
        return false;
    }

    /**
     * 2024-06-11
     * 根据properties属性加载
     * @param properties 属性配置
     * @return boolean
     */
    private static final boolean p_loadPropertiesObject(final Properties properties){
        try{
            Enumeration<?> enumeration = properties.propertyNames();
            while(enumeration.hasMoreElements()){
                String key = (String) enumeration.nextElement();
                String value = properties.getProperty(key);
                value = StringUtil.getPureStr(value);
                value = StringUtil.trim(value);
                if(value == null){
                    value = "";
                }else if(StringUtil.containOf(value, " #")){
                    String[] array = StringUtil.split(value, " #");
                    value = array[0];
                    value = StringUtil.trim(value);
                }
                //
                p_propertiesMap.put(key, value);
            }
            return true;
        }catch(Throwable throwable){
            LogUtil.error("PropertiesUtil_p_loadPropertiesObject_t1", throwable);
            return false;
        }
    }

    /**
     * 2024-07-17
     * 热更新
     */
    public static final void hotUpdatePropertiesFile(){
        hotUpdatePropertiesFile(p_router_propertiesUrl, 10, null);
    }

    /**
     * 2024-07-17
     * 热更新
     * @param propertiesUrl
     */
    public static final void hotUpdatePropertiesFile(final String propertiesUrl){
        hotUpdatePropertiesFile(propertiesUrl, 10, null);
    }

    /**
     * 2024-07-17
     * 热更新
     * @param propertiesUrl
     * @param interval
     */
    public static final void hotUpdatePropertiesFile(final String propertiesUrl, final Integer interval){
        hotUpdatePropertiesFile(propertiesUrl, interval, null);
    }

    /**
     * 2024-07-17
     * 热更新
     * @param propertiesUrl
     * @param sm4key
     */
    public static final void hotUpdatePropertiesFile(final String propertiesUrl, final String sm4key){
        hotUpdatePropertiesFile(propertiesUrl, 10, sm4key);
    }

    /**
     * 2024-07-17
     * 热更新
     * @param propertiesUrl
     * @param interval 检测频度，单位秒
     * @param sm4key
     */
    public static final void hotUpdatePropertiesFile(final String propertiesUrl, final Integer interval, final String sm4key){
        ThreadUtil.start(new Business("PropertiesUtil_hotUpdaatePropertiesFile_1", false){
            @Override
            public void process(){
                String fileHash = MD5Util.getFileHash(propertiesUrl);
                long p_interval = interval * 1000L;
                if(p_interval < 2000){
                    p_interval = 2000;
                }
                while(true){
                    ThreadUtil.sleep(p_interval);
                    String newHash = MD5Util.getFileHash(propertiesUrl);
                    if(fileHash.equals(newHash)){
                        continue;
                    }
                    InputStream inputStream = null;
                    Properties properties = new Properties();
                    try{
                        if(StringUtil.isBlank(sm4key)){
                            inputStream = IOUtil.file2inputStream(propertiesUrl);
                        }else{
                            inputStream = Sm4Util.decryptFileReturnInputStream(sm4key, propertiesUrl);
                        }
                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                        properties.load(inputStreamReader);
                        loadPropertiesObject(properties);
                    }catch(Throwable throwable){
                        LogUtil.error("PropertiesUtil_hotUpdaatePropertiesFile_e1", "properties文件热更新失败", propertiesUrl, interval);
                    }
                }
            }
        });
    }

    private static final void p_initMap(){
        if(p_propertiesListMap == null){
            p_propertiesListMap = new HashMap<>(32);
        }
        if(p_propertiesMap == null){
            p_propertiesMap = new ConcurrentHashMap<>(512);
        }
    }

    /**
     * 2024-07-10
     * propertis对象转换为文件
     * @param properties
     * @param targetUrl
     * @return
     */
    public static final boolean properties2file(final Properties properties, final String targetUrl){
        boolean flag = properties2file(properties, targetUrl, true, null);
        return flag;
    }

    /**
     * 2024-07-10
     * propertis对象转换为文件
     * @param properties
     * @param targetUrl
     * @param isCover
     * @return
     */
    public static final boolean properties2file(final Properties properties, final String targetUrl, final boolean isCover){
        boolean flag = properties2file(properties, targetUrl, isCover, null);
        return flag;
    }

    /**
     * 2024-07-10
     * propertis对象转换为文件
     * @param properties
     * @param targetUrl
     * @param isCover
     * @param comments
     * @return
     */
    public static final boolean properties2file(final Properties properties, final String targetUrl, final boolean isCover, final String comments){
        if(properties == null){
            LogUtil.error("PropertiesUtil_properties2file_1", "properties对象为空", targetUrl);
            return false;
        }
        if(StringUtil.isBlank(targetUrl)){
            LogUtil.error("PropertiesUtil_properties2file_2", "targetUrl为空", targetUrl);
            return false;
        }
        if(isCover){
            boolean flag = FileUtil.delete(targetUrl);
            if(!flag){
                LogUtil.error("PropertiesUtil_properties2file_3", "isCover为true，targetUrl文件删除失败", targetUrl);
                return false;
            }
        }else{
            if(FileUtil.isRealFile(targetUrl)){
                LogUtil.error("PropertiesUtil_properties2file_4", "isCover为false，targetUrl文件已存在", targetUrl);
                return false;
            }
        }
        String commentsLocal = comments;
        if(StringUtil.isBlank(commentsLocal)){
            commentsLocal = "";
        }
        //
        if(!FileUtil.exists(targetUrl)){
            boolean flag = FileUtil.fileWriteNew(targetUrl, "", false);
            if(!flag){
                LogUtil.error("PropertiesUtil_properties2file_5", "targetUrl文件创建失败", targetUrl);
                return false;
            }
        }
        //
        FileWriter fileWriter = null;
        try{
            fileWriter = new FileWriter(targetUrl);
            properties.store(fileWriter, commentsLocal);
            return true;
        }catch(Throwable throwable){
            LogUtil.error("PropertiesUtil_properties2file_e1", targetUrl);
            return false;
        } finally {
            IOUtil.close(fileWriter);
        }
    }

    /**
     * 2018-04-18 1.1.0
     * 2026-08-28 编码整改-单参版改委托双参版
     * 获取指定properties文件的key值，getPropertyValue，默认不支持热读取（热部署）
     * @param key 键名
     */
    public static final String getValue(final String key){
        String value = getValue(key, "");
        return value;
    }

    /**
     * 2026-08-28 编码整改-空值时返回默认值
     * 获取指定properties文件的key值，未配置或值为空串时返回默认值
     * @param key 键名
     * @param defaultValue 默认值
     */
    public static final String getValue(final String key, final String defaultValue){
        String value = p_propertiesMap.get(key);
        if(StringUtil.isBlank(value)){
            return defaultValue;
        }
        return value;
    }

    /**
     * 获取布尔值属性。
     */
    public static final Boolean getBoolean(final String key){
        String str = getValue(key);
        if("".equals(str)){
            return false;
        }
        Boolean value = Boolean.parseBoolean(str);
        return value;
    }

    /**
     * 获取长整型属性值。
     */
    public static final Long getLong(final String key){
        String str = getValue(key);
        if("".equals(str)){
            return 0L;
        }
        Long value = Long.parseLong(str);
        return value;
    }

    /**
     * 获取整型属性。
     */
    public static final Integer getInt(final String key){
        String str = getValue(key);
        if("".equals(str)){
            return 0;
        }
        Integer value = Integer.parseInt(str);
        return value;
    }

    /**
     * 2024-07-17
     * @return
     */
    public static final String getRouterPropertiesUrl(){
        return p_router_propertiesUrl;
    }

    /**
     * 2022-10-21
     */
    public static final Map<String, List<String>> getPropertiesListMap(){
        return p_propertiesListMap;
    }

    /**
     * 2022-10-21
     */
    public static final Map<String, String> getPropertiesMap(){
        return p_propertiesMap;
    }

}