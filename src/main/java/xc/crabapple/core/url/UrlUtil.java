package xc.crabapple.core.url;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.device.EnvironmentUtil;
import xc.crabapple.core.encoder.UriEncoderUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.log.LogUtil;

/**
 * 2016-11-20 1.0.0
 */
public final class UrlUtil{

    /**
     * 2025-11-29
     * @param url
     * @return
     */
    public static final String getFileUrl(final String url){
        String localUrl = getStandardPath(url);
        if(EnvironmentUtil.isWindows()){
            localUrl = "file:///" + localUrl;
            return localUrl;
        }
        localUrl = "file://" + localUrl;
        return localUrl;
    }

    /**
     * 2025-02-18
     * 当前业务项目的外部依赖的第三方jar文件根目录绝对路径
     * 返回值如：D:/data/maven/repository/xc/crabapple/crabapple-commons/21-4.2.00/crabapple-commons-21-4.2.00.jar
     */
    public static final String getExJarRootPath(){
        try {
            ProtectionDomain protectionDomain = UrlUtil.class.getProtectionDomain();
            CodeSource codeSource = protectionDomain.getCodeSource();
            URL url = codeSource.getLocation();
            URI uri = url.toURI();
            String exJarRootPath = uri.getPath();
            exJarRootPath = UrlUtil.getStandardPath(exJarRootPath);
            return exJarRootPath;
        }catch (Throwable throwable){
            LogUtil.error("UrlUtil_getExJarRootPath_t1", throwable);
            return "";
        }
    }

    /**
     * 2024-11-10
     * 对指定的urlList集合排序，文件或目录路径排序，用法示例：
     * [d:/aaa/3.txt, d:/aaa/1.txt, d:/aaa/2.txt] ==> [d:/aaa/1.txt, d:/aaa/2.txt, d:/aaa/3.txt]
     * @param fileList
     * @return
     */
    public static final List<String> sortUrlList(final List<String> fileList){
        List<String> localFileList = FileUtil.sortUrlList(fileList);
        return localFileList;
    }

    /**
     * 2024-11-10
     * 对指定的urlList集合排序，文件或目录路径排序，用法示例：
     * [d:/aaa/3.txt, d:/aaa/1.txt, d:/aaa/2.txt] ==> [d:/aaa/1.txt, d:/aaa/2.txt, d:/aaa/3.txt]
     * @param fileList
     * @param orderType asc-正序，desc-倒序
     * @return
     */
    public static final List<String> sortUrlList(final List<String> fileList, final String orderType){
        List<String> localFileList = FileUtil.sortUrlList(fileList, orderType);
        return localFileList;
    }

    /**
     * 2023-09-27
     * 判断实际url是否模糊匹配模板url
     * 支持的匹配模式：
     *   精确匹配，/aaa/bbb
     *   前缀模糊匹配，星号/aaa/bbb
     *   后缀模糊匹配，/aaa/bbb/星号
     * @param realUrl 实际url
     * @param templateUrl 模板url
     * @return 匹配返回true，不匹配返回false
     */
    public static final boolean matchUrl(final String realUrl, final String templateUrl){
        if(StringUtil.isBlank(realUrl)){
            return false;
        }
        //
        String localTemplateUrl = StringUtil.trim(templateUrl);
        if(StringUtil.isBlank(localTemplateUrl)){
            return false;
        }
        //
        if(StringUtil.equalsOfOne(localTemplateUrl, "*", "/*", "/**")){
            return true;
        }
        //
        if(StringUtil.equals(localTemplateUrl, realUrl)){
            return true;
        }
        //
        if(StringUtil.lastsWith(localTemplateUrl, "/")){
            localTemplateUrl = StringUtil.deleteLast(localTemplateUrl, "/");
        }
        //
        if(StringUtil.startsWith(localTemplateUrl, "*")){
            localTemplateUrl = StringUtil.deleteFirst(localTemplateUrl, "*");
            if(!StringUtil.startsWith(localTemplateUrl, "/")){
                localTemplateUrl = "/" + localTemplateUrl;
            }
            if(StringUtil.lastsWith(realUrl, localTemplateUrl)){
                return true;
            }
        }
        //
        if(StringUtil.lastsWith(localTemplateUrl, "*")){
            localTemplateUrl = StringUtil.deleteLast(localTemplateUrl, "*");
            if(StringUtil.lastsWith(localTemplateUrl, "/")){
                localTemplateUrl = StringUtil.deleteLast(localTemplateUrl, "/");
            }
            if(!StringUtil.startsWith(localTemplateUrl, "/")){
                localTemplateUrl = "/" + localTemplateUrl;
            }
            if(StringUtil.startsWith(realUrl, localTemplateUrl)){
                return true;
            }
        }
        //
        if (StringUtil.equals(localTemplateUrl, realUrl)){
            return true;
        }
        return false;
    }

    /**
     * 2022-07-13
     * 返回一个标准的、经过编码的网络地址
     * @param address
     * @return
     */
    public static final String getStandardAddress(final String address){
        String addr = getStandardAddress(address, "");
        return addr;
    }

    /**
     * 2022-07-13
     * 返回一个标准的、经过编码的网络地址
     * @param address
     * @param params
     * @return
     */
    public static final String getStandardAddress(final String address, final String params){
        if(StringUtil.isBlank(address)){
            LogUtil.error("UrlUtil_getStandardAddress_1", "address为空");
            return "";
        }
        //
        if(StringUtil.isBlank(params)){
            String addr = UriEncoderUtil.encodeURIComponent(address, UriEncoderUtil.EXCLUDE_ADDRESS);
            return addr;
        }
        //
        if(StringUtil.containOf(address, "?")){
            String addr = address + "&" + params;
            addr = UriEncoderUtil.encodeURIComponent(addr, UriEncoderUtil.EXCLUDE_ADDRESS);
            return addr;
        }
        String addr = address + "?" + params;
        addr = UriEncoderUtil.encodeURIComponent(addr, UriEncoderUtil.EXCLUDE_ADDRESS);
        return addr;
    }

    /**
     * 2022-07-13
     * 返回一个标准的、经过编码的
     * @param address
     * @param paramMap
     * @return
     */
    public static final String getStandardAddress(final String address, final Map<String, Object> paramMap){
        if(StringUtil.isBlank(address)){
            LogUtil.error("UrlUtil_getStandardAddress_2", "address为空");
            return "";
        }
        //
        if(CollectionUtil.isBlank(paramMap)){
            String addr = UriEncoderUtil.encodeURIComponent(address, UriEncoderUtil.EXCLUDE_ADDRESS);
            return addr;
        }
        //
        String params = getParamsByMap(paramMap);
        if(StringUtil.isBlank(params)){
            String addr = UriEncoderUtil.encodeURIComponent(address, UriEncoderUtil.EXCLUDE_ADDRESS);
            return addr;
        }
        //
        if(StringUtil.containOf(address, "?")){
            String addr = address + "&" + params;
            addr = UriEncoderUtil.encodeURIComponent(addr, UriEncoderUtil.EXCLUDE_ADDRESS);
            return addr;
        }
        //
        String addr = address + "?" + params;
        addr = UriEncoderUtil.encodeURIComponent(addr, UriEncoderUtil.EXCLUDE_ADDRESS);
        return addr;
    }

    /**
     * 2022-07-13
     * 参数map转换为标准参数字符串
     * @param paramMap
     * @return 格式如 a=1&b=2
     */
    public static final String getParamsByMap(final Map<String, Object> paramMap){
        if(CollectionUtil.isBlank(paramMap)){
            return "";
        }
        Set<String> set = paramMap.keySet();
        Iterator<String> iterator = set.iterator();
        int i = 0;
        StringBuilder stringBuilder = new StringBuilder();
        while(iterator.hasNext()){
            i++;
            String key = iterator.next();
            Object value = paramMap.get(key);
            String valueStr = "";
            if(value != null){
                valueStr = value.toString();
                valueStr = UriEncoderUtil.encodeURIComponent(valueStr);
            }
            if(i == 1){
                stringBuilder.append(key).append("=").append(valueStr);
                continue;
            }
            stringBuilder.append("&").append(key).append("=").append(valueStr);
        }
        final String params = stringBuilder.toString();
        if(StringUtil.isBlank(params)){
            return "";
        }
        return params;
    }

    /**
     * 2022-05-14
     * HTTP请求地址解析器，字符串转换为List集合
     * @params params 格式如 a=1&b=2
     */
    public static final List<List<String>> getListByParams(final String params){
        List<List<String>> paramList = new ArrayList<>();
        //
        Map<String, String> paramMap = getMapByParams(params);
        //
        Set<String> set = paramMap.keySet();
        Iterator<String> iterator = set.iterator();
        while(iterator.hasNext()){
            String key = iterator.next();
            String value = paramMap.get(key);
            List<String> list = new ArrayList<>();
            list.add(key);
            list.add(value);
            paramList.add(list);
        }
        //
        return paramList;
    }

    /**
     * 2022-05-14
     * HTTP请求地址解析器，字符串转换为Map集合
     * @params params 格式如 a=1&b=2
     */
    public static final Map<String, String[]> getMapArrayByParams(final String params){
        Map<String, String[]> paramListMap = new HashMap<>();
        //
        Map<String, String> paramMap = getMapByParams(params);
        //
        Set<String> set = paramMap.keySet();
        Iterator<String> iterator = set.iterator();
        while(iterator.hasNext()){
            String key = iterator.next();
            String value = paramMap.get(key);
            String[] array = {value};
            paramListMap.put(key, array);
        }
        //
        return paramListMap;
    }

    /**
     * 2022-05-14
     * HTTP请求地址解析器，字符串转换为Map集合
     * @params params 格式如 a=1&b=2
     */
    public static final Map<String, String> getMapByParams(final String params){
        Map<String, String> paramMap = new HashMap<>();
        //
        if(StringUtil.isBlank(params)){
            return paramMap;
        }
        //
        List<String> paramList = new ArrayList<>();
        if(StringUtil.containOf(params, "&")){
            String[] array = StringUtil.split(params, "&");
            for(String param : array){
                paramList.add(param);
            }
        }else{
            paramList.add(params);
        }
        for(String param : paramList){
            //可以兼容两个或者多个个&&的情况
            if (StringUtil.isBlank(param)) {
                LogUtil.warn("crabapple-commons-UrlUtil_paramsParser_3", "参数不合法", params);
                continue;
            }
            if(!StringUtil.containOf(param, "=")){
                LogUtil.error("crabapple-commons-UrlUtil_paramsParser_1", "参数不合法", params);
                continue;
            }
            String[] arr = StringUtil.split(param, "=");
            String key = arr[0];
            key = StringUtil.deleteAll(key, " ");
            if("".equals(key)){
                LogUtil.error("UrlUtil_paramsParser_2", "key不能为空", params);
                continue;
            }
            String value = arr[1];
            value = UriEncoderUtil.decodeURIComponent(value);
            paramMap.put(key, value);
        }
        //
        return paramMap;
    }

    /**
     * 2016-11-20 1.0.0
     * e:/aa/bb/hello.txt ==> e:/aa/bb
     * @param url 全绝对路径
     */
    public static final String getPathByUrl(final String url){
        String localUrl = UrlUtil.getStandardPath(url);
        boolean flag = StringUtil.lastsWith(localUrl, "/");
        if(flag){
            localUrl = localUrl.substring(0, localUrl.length() - 1);
        }
        String[] arr = StringUtil.split(localUrl, "/");
        StringBuilder sb = new StringBuilder();
        int n = arr.length - 1;
        for(int i = 0; i < n; i++){
            if(i < n - 1){
                sb.append(arr[i]).append("/");
            }else{
                sb.append(arr[i]);
            }
        }
        String result = sb.toString();
        return result;
    }

    /**
     * 2018-02-13 1.1.0
     * ("e:/aa/bb/hello.txt", 1) ==> e:/aa/bb
     * ("D:/app/hello", 1) ==> D:/app
     * @param level 为0表示未对url做任何处理直接返回
     */
    public static final String getPathByLevel(final String url, final Integer level){
        if(StringUtil.isBlank(url)){
            return "";
        }
        if(level <= 0){
            return url;
        }
        String localUrl = getStandardPath(url);
        boolean flag = StringUtil.lastsWith(localUrl, "/");
        if(flag){
            localUrl = localUrl.substring(0, localUrl.length() - 1);
        }
        String[] array = StringUtil.split(localUrl, "/");
        StringBuilder sb = new StringBuilder();
        int n = array.length - level;
        if(n < 0){
            n = 0;
        }
        for(int i = 0; i < n; i++){
            String node = array[i];
            if(i < n -1){
                sb.append(node).append("/");
            }else{
                sb.append(node);
            }
        }
        String result = sb.toString();
        if("".equals(result)){
            result = "/";
        }
        return result;
    }

    /**
     * 2017-03-09 1.0.0
     * ("D:/app/project/src/main/java") ==>
     * D:/app/project/src/main
     * D:/app/project/src
     * D:/app/project
     * D:/app
     * D:
     * @param url
     */
    public static final List<String> getPathList(final String url){
        List<String> list = new ArrayList<>();
        if(StringUtil.isBlank(url)){
            return list;
        }
        int i = 1;
        while(true){
            String line = getPathByLevel(url, i);
            if("/".equals(line)){
                return list;
            }
            list.add(line);
            i++;
        }
    }

    /**
     * 2018-07-30 1.1.0
     * 根据文件名或绝对路径返回文件名，如 /clouddb/test.zip ==> test
     * @param nameOrUrl 文件文件名或绝对路径
     */
    public static final String getFileName(final String nameOrUrl){
        String name = FileUtil.getFileName(nameOrUrl);
        return name;
    }

    /**
     * 2018-07-30 1.1.0
     * 根据文件名或绝对路径返回文件名，如 /clouddb/test.zip ==> test
     * @param nameOrUrl 文件文件名或绝对路径
     * @param suffix 是否带有后缀名，true表示带，false表示不带
     */
    public static final String getFileName(final String nameOrUrl, final boolean suffix){
        String name = FileUtil.getFileName(nameOrUrl, suffix);
        return name;
    }

    /**
     * 2016-11-20 1.0.0
     * 2020-03-16 5.0.0
     * @param file
     */
    public static final String getStandardPath(final File file){
        String path = file.getAbsolutePath();
        path = getStandardPath(path);
        return path;
    }

    /**
     * 2016-11-20 1.0.0
     * 2026-05-30 全面重构：Windows与Linux严格隔离，入口仅判断一次操作系统，各自独立处理
     * 返回标准化路径：反斜杠转正斜杠、合并连续斜杠、去除末尾斜杠、
     * Windows盘符修正（仅ASCII字母）、Windows自动补D盘、段名校验。
     *
     * <pre>
     * === 通用校验（Windows + Linux 均生效） ===
     *   null / "" / "   "                         → "/"               (边界输入)
     *   "/a/./b"                                   → "" + err          (.号段不允许)
     *   "/a/.. /b"                                 → "" + err          (..号段不允许)
     *   "/a/%00/b"                                 → "" + err          (空字节不允许)
     *   "hello"                                    → "hello"          (无分隔符，原样返回)
     *
     * === Linux 专属 ===
     *   "/"                     (Linux)           → "/"
     *   "/aaa/bbb"              (Linux)           → "/aaa/bbb"
     *   "/a//b///c/"            (Linux)           → "/a/b/c"
     *   "/a/con/b"              (Linux)           → "/a/con/b"        (Linux无禁用名限制)
     *
     * === Windows 专属 ===
     *   "/"                     (Windows)         → "D:/"
     *   "/aaa/bbb"              (Windows)         → "D:/aaa/bbb"
     *   "D:\\app\\hello"        (Windows)         → "D:/app/hello"
     *   "/a//b///c/"            (Windows)         → "D:/a/b/c"
     *   "/C:/foo/bar"           (Windows)         → "C:/foo/bar"
     *   "C:/foo"                (Windows)         → "C:/foo"
     *   "\\\\server\\share"     (Windows)         → "//server/share"  (UNC路径保留)
     *   "/a/con/b"              (Windows)         → "" + err          (Windows禁用名)
     * </pre>
     *
     * @param path 可以是目录，文件路径或一个普通字符串
     * @return 标准化后的路径，校验失败返回""
     */
    public static String getStandardPath(String path) {
        final String result = getStandardPath(path, true);
        return result;
    }

    /**
     * 2016-11-20 1.0.0
     * 2026-05-30 全面重构：Windows与Linux严格隔离，入口仅判断一次操作系统，各自独立处理
     * 2026-05-30 新增prependDrive参数，控制Windows下是否自动追加D:盘符
     * 返回标准化路径：反斜杠转正斜杠、合并连续斜杠、去除末尾斜杠、
     * Windows盘符修正（仅ASCII字母）、Windows自动补D盘、段名校验。
     *
     * <pre>
     * === 通用校验（Windows + Linux 均生效） ===
     *   null / "" / "   "                         → "/"               (边界输入)
     *   "/a/./b"                                   → "" + err          (.号段不允许)
     *   "/a/.. /b"                                 → "" + err          (..号段不允许)
     *   "/a/%00/b"                                 → "" + err          (空字节不允许)
     *   "hello"                                    → "hello"          (无分隔符，原样返回)
     *
     * === Linux 专属 ===
     *   "/"                     (Linux)           → "/"
     *   "/aaa/bbb"              (Linux)           → "/aaa/bbb"
     *   "/a//b///c/"            (Linux)           → "/a/b/c"
     *   "/a/con/b"              (Linux)           → "/a/con/b"        (Linux无禁用名限制)
     *
     * === Windows 专属 ===
     *   "/"                     (Windows)         → "D:/"
     *   "/aaa/bbb"              (Windows)         → "D:/aaa/bbb"
     *   "D:\\app\\hello"        (Windows)         → "D:/app/hello"
     *   "/a//b///c/"            (Windows)         → "D:/a/b/c"
     *   "/C:/foo/bar"           (Windows)         → "C:/foo/bar"
     *   "C:/foo"                (Windows)         → "C:/foo"
     *   "\\\\server\\share"     (Windows)         → "//server/share"  (UNC路径保留)
     *   "/a/con/b"              (Windows)         → "" + err          (Windows禁用名)
     * </pre>
     *
     * @param path 可以是目录，文件路径或一个普通字符串
     * @param prependDrive Windows下是否自动追加D:盘符，默认true
     * @return 标准化后的路径，校验失败返回""
     */
    public static String getStandardPath(String path, boolean prependDrive) {
        if (StringUtil.isBlank(path)) {
            return "/";
        }
        final String originalArg = path;
        path = StringUtil.trim(path);
        if ("".equals(path)) {
            return "/";
        }
        if (EnvironmentUtil.isWindows()) {
            String result = p_getStandardPathWindows(path, originalArg, prependDrive);
            return result;
        }
        String result = p_getStandardPathLinux(path, originalArg);
        return result;
    }

    /**
     * 2016-11-20 1.0.0
     * 2026-05-30 全面重写：Windows路径标准化，独立于Linux分支
     * Windows平台路径标准化：
     *   - 空字节检测、长度校验
     *   - 反斜杠转正斜杠、合并连续斜杠
     *   - UNC路径保留：//server/share → //server/share
     *   - 盘符修正：/C:/foo → C:/foo（仅ASCII字母A-Z/a-z），自动补D盘
     *   - 段名校验：纯点号段拒绝、Windows禁用名检测（CON/PRN/AUX/NUL/COM0-9/LPT0-9）
     *   - 空白段名跳过
     * @param path 已经过trim处理的路径
     * @param originalArg 原始输入参数，仅用于日志输出
     * @return 标准化后的Windows路径，校验失败返回""
     */
    private static String p_getStandardPathWindows(String path, final String originalArg, boolean prependDrive) {
        if (path.indexOf('\0') != -1) {
            LogUtil.error("UrlUtil_p_getStandardPathWindows_0", "路径包含空字节，拒绝处理", originalArg);
            return "";
        }
        if (path.length() > 4096) {
            LogUtil.error("UrlUtil_p_getStandardPathWindows_1", "路径长度超过限制4096", originalArg);
            return "";
        }
        if (!StringUtil.containOf(path, "/") && !StringUtil.containOf(path, "\\")) {
            return path;
        }
        path = path.replace("\\", "/");
        boolean isUnc = path.startsWith("//") && !path.startsWith("///");
        while (path.indexOf("//") != -1) {
            path = path.replace("//", "/");
        }
        if (isUnc) {
            StringBuilder uncSb = new StringBuilder();
            uncSb.append("/");
            uncSb.append(path);
            path = uncSb.toString();
        }
        if (path.length() >= 3 && path.charAt(0) == '/' && p_isAsciiLetter(path.charAt(1)) && path.charAt(2) == ':') {
            path = path.substring(1);
        }
        isUnc = path.startsWith("//");
        FileUtil.getWindowsFilenameForbiddenMap();
        String[] segments = StringUtil.split(path, "/");
        List<String> resolved = new ArrayList<String>();
        for (int i = 0; i < segments.length; i++) {
            String seg = segments[i];
            if ("".equals(seg)) {
                continue;
            }
            String trimmed = seg.trim();
            if (trimmed.length() < 1) {
                continue;
            }
            boolean allDots = true;
            for (int j = 0; j < trimmed.length(); j++) {
                if (trimmed.charAt(j) != '.') {
                    allDots = false;
                    break;
                }
            }
            if (allDots) {
                LogUtil.error("UrlUtil_p_getStandardPathWindows_2", "路径段不允许为纯点号", originalArg, seg);
                return "";
            }
            String name = StringUtil.toLowerCase(trimmed);
            int dotIndex = name.indexOf(".");
            if (dotIndex > 0) {
                name = name.substring(0, dotIndex);
            }
            if (FileUtil.windowsFilenameForbiddenMap.containsKey(name)) {
                LogUtil.error("UrlUtil_p_getStandardPathWindows_3", "路径段包含Windows禁用名称", originalArg, seg);
                return "";
            }
            resolved.add(seg);
        }
        StringBuilder sb = new StringBuilder();
        if (isUnc) {
            sb.append("//");
        } else if (path.startsWith("/")) {
            sb.append("/");
        }
        int size = resolved.size();
        for (int i = 0; i < size; i++) {
            sb.append(resolved.get(i));
            if (i < size - 1) {
                sb.append("/");
            }
        }
        path = sb.toString();
        if (path.endsWith("/") && !"/".equals(path)) {
            path = path.substring(0, path.length() - 1);
        }
        if (prependDrive && !isUnc && path.startsWith("/")) {
            StringBuilder tmpSb = new StringBuilder();
            tmpSb.append("D:");
            tmpSb.append(path);
            path = tmpSb.toString();
        }
        return path;
    }

    /**
     * 2026-05-30
     * 判断字符是否为ASCII字母（A-Z或a-z），用于Windows盘符校验
     * @param c 待判定的字符
     * @return true表示是ASCII字母，false表示不是
     */
    private static boolean p_isAsciiLetter(final char c) {
        final boolean result = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
        return result;
    }

    /**
     * 2016-11-20 1.0.0
     * 2026-05-30 全面重写：Linux路径标准化，独立于Windows分支
     * Linux平台路径标准化：
     *   - 空字节检测、长度校验
     *   - 反斜杠转正斜杠、合并连续斜杠
     *   - 段名校验：纯点号段（. / .. / ...）拒绝
     *   - 空白段名跳过
     *   - Linux无Windows禁用名限制（CON等名称允许通过）
     * @param path 已经过trim处理的路径
     * @param originalArg 原始输入参数，仅用于日志输出
     * @return 标准化后的Linux路径，校验失败返回""
     */
    private static String p_getStandardPathLinux(String path, final String originalArg) {
        if (path.indexOf('\0') != -1) {
            LogUtil.error("UrlUtil_p_getStandardPathLinux_0", "路径包含空字节，拒绝处理", originalArg);
            return "";
        }
        if (path.length() > 4096) {
            LogUtil.error("UrlUtil_p_getStandardPathLinux_1", "路径长度超过限制4096", originalArg);
            return "";
        }
        if (!StringUtil.containOf(path, "/") && !StringUtil.containOf(path, "\\")) {
            return path;
        }
        path = path.replace("\\", "/");
        while (path.indexOf("//") != -1) {
            path = path.replace("//", "/");
        }
        String[] segments = StringUtil.split(path, "/");
        List<String> resolved = new ArrayList<String>();
        for (int i = 0; i < segments.length; i++) {
            String seg = segments[i];
            if ("".equals(seg)) {
                continue;
            }
            String trimmed = seg.trim();
            if (trimmed.length() < 1) {
                continue;
            }
            boolean allDots = true;
            for (int j = 0; j < trimmed.length(); j++) {
                if (trimmed.charAt(j) != '.') {
                    allDots = false;
                    break;
                }
            }
            if (allDots) {
                LogUtil.error("UrlUtil_p_getStandardPathLinux_2", "路径段不允许为纯点号", originalArg, seg);
                return "";
            }
            resolved.add(seg);
        }
        StringBuilder sb = new StringBuilder();
        if (path.startsWith("/")) {
            sb.append("/");
        }
        int size = resolved.size();
        for (int i = 0; i < size; i++) {
            sb.append(resolved.get(i));
            if (i < size - 1) {
                sb.append("/");
            }
        }
        path = sb.toString();
        if (path.endsWith("/") && !"/".equals(path)) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * 2023-07-13
     * @param apiUri
     * @return 返回标准接口地址，格式如 /api/efile/login
     */
    public static final String getStandardApi(final String apiUri){
        String localApiUri = getStandardApi(apiUri, false);
        return localApiUri;
    }

    /**
     * 2023-07-13
     * @param apiUri
     * @param isLastSlash 是否以/结尾
     * @return 返回标准接口地址，格式如 /api/efile/login
     */
    public static final String getStandardApi(final String apiUri, final boolean isLastSlash){
        if(StringUtil.isBlank(apiUri)){
            return "";
        }
        String localApiUri = StringUtil.trim(apiUri);
        if("/".equals(localApiUri)){
            return "/";
        }
        //
        localApiUri = StringUtil.replaceAll(localApiUri, "\\", "/");
        if(!StringUtil.startsWith(localApiUri, "/")){
            localApiUri = "/" + localApiUri;
        }
        if(StringUtil.lastsWith(localApiUri, "/")){
            if(isLastSlash){
                return localApiUri;
            }
            localApiUri = StringUtil.deleteLast(localApiUri,1);
            return localApiUri;
        }
        if(isLastSlash){
            localApiUri += "/";
            return localApiUri;
        }
        return localApiUri;
    }

}