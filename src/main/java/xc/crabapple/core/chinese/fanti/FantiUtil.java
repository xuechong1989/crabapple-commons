package xc.crabapple.core.chinese.fanti;

import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 2025-02-04
 * 简体字与繁体字之间的转换
 */
public final class FantiUtil {

    private static Map<String, String> p_fanti2simpleMap;
    private static Map<String, String> p_simple2fantiMap;

    /**
     * 2025-02-13
     * 简体转换为繁体
     * @param simple 要转换的简体字字符串
     * @return
     */
    public static final String simple2fanti(final String simple) {
        p_initMap();
        //
        if(StringUtil.isBlank(simple)){
            return simple;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < simple.length(); i++){
            char chr = simple.charAt(i);
            String sss = StringUtil.char2str(chr);
            String fantiSSS = p_simple2fantiMap.get(sss);
            if(StringUtil.isBlank(fantiSSS)){
                stringBuilder.append(sss);
                continue;
            }
            stringBuilder.append(fantiSSS);
        }
        final String fantiStr = stringBuilder.toString();
        return fantiStr;
    }

    /**
     * 2025-02-13
     * 繁体转换为简体
     * @param fanti 要转换的繁体字字符串
     * @return
     */
    public static final String fanti2simple(final String fanti) {
        p_initMap();
        //
        if(StringUtil.isBlank(fanti)){
            return fanti;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < fanti.length(); i++){
            char chr = fanti.charAt(i);
            String sss = StringUtil.char2str(chr);
            String simpleSSS = p_fanti2simpleMap.get(sss);
            if(StringUtil.isBlank(simpleSSS)){
                stringBuilder.append(sss);
                continue;
            }
            stringBuilder.append(simpleSSS);
        }
        final String simpleStr = stringBuilder.toString();
        return simpleStr;
    }

    /**
     * 2025-02-07
     * @return
     */
    private static final void p_initMap(){
        if(p_fanti2simpleMap != null){
            return;
        }
        p_fanti2simpleMap = new HashMap<>(2800);
        p_simple2fantiMap = new HashMap<>(2800);
        //
        List<String> list = getList();
        if(CollectionUtil.isBlank(list)){
            LogUtil.error("FantiUtil_getList_1", "繁体资源文件未加载");
            return;
        }
        //
        final int size = list.size();
        for(int i = 0; i < size; i++) {
            String line = list.get(i);
            line = StringUtil.trim(line);
            line = StringUtil.getPureStr(line);
            if (StringUtil.isBlank(line)) {
                continue;
            }
            String[] array = StringUtil.split(line, "=");
            String fanti = array[0];
            fanti = StringUtil.trim(fanti);
            String simple = array[1];
            simple = StringUtil.trim(simple);
            //
            p_fanti2simpleMap.put(fanti, simple);
            p_simple2fantiMap.put(simple, fanti);
        }
    }

    /**
     * 2025-02-07
     * 2025-02-18 引入读取jar文件内部的文件内容
     * 获取所有繁体简体对照表
     * @return
     */
    public static final List<String> getList(){
        List<String> list = null;
        try{
            final String exJarRootPath = UrlUtil.getExJarRootPath();
            if(StringUtil.lastsWith(exJarRootPath, ".jar")){
                JarFile jarFile = new JarFile(exJarRootPath);
                JarEntry jarEntry = jarFile.getJarEntry("xc/crabapple/core/chinese/fanti/fanti.txt");
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                list = IOUtil.inputstream2list(inputStream, true);
            }else{
                // 直接加载本地文件
            }
        }catch (Throwable throwable){
            // LogUtil.error("PinyinUtil_getList_t1", throwable);
            // 直接加载本地文件
            list = null;
        }
        // 直接加载本地文件
        if(CollectionUtil.isBlank(list)){
            URL url2 = FantiUtil.class.getResource("");
            String path = url2.getPath();
            path = UrlUtil.getStandardPath(path);
            String txtUrl = path + "/fanti.txt";
            txtUrl = UrlUtil.getStandardPath(txtUrl);
            list = FileUtil.fileReadList(txtUrl);
        }
        //
        if(CollectionUtil.isBlank(list)){
            LogUtil.error("FantiUtil_getList_1", "繁体资源文件未加载");
            return null;
        }
        //
        List<String> finalList = new ArrayList<>();
        final int size = list.size();
        for(int i = 0; i < size; i++) {
            String line = list.get(i);
            line = StringUtil.trim(line);
            line = StringUtil.getPureStr(line);
            if (StringUtil.isBlank(line)) {
                continue;
            }
            finalList.add(line);
        }
        return finalList;
    }

}