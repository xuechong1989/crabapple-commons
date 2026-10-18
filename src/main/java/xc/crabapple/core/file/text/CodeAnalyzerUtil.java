package xc.crabapple.core.file.text;

import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.log.LogUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CodeAnalyzerUtil {

    /**
     * 2023-06-15 接口扫描
     * 2023-08-02 代码整理
     * 统计所有接口、所有未被（PC客户端）使用的接口
     */
    public static final String[] statisticsApis(String javaPath, String viewPath, String viewMobilePath,
                                                String dataBaseApiPath, String allApiUrl, String noUsedUrl){
        //
        final long start = System.currentTimeMillis();
        // 统计所有API
        List<String> javaFileList = FileUtil.getDirChildrenFiles(javaPath, "java");
        List<String> apisList = new ArrayList<>();
        for(String url : javaFileList){
            String name = FileUtil.getFileName(url, false);
            if(!StringUtil.lastsWith(name, "Controller")){
                continue;
            }
            String text = FileUtil.fileReadText(url);
            List<String> apinameList = getApinameList(text);
            apisList.addAll(apinameList);

        }
        final StringBuilder allApiStringBuilder = new StringBuilder();
        for (String apiStr : apisList){
            allApiStringBuilder.append(apiStr).append("\n");
        }
        // 统计未使用的API
        List<String> noUsedApisList = new ArrayList<>();
        noUsedApisList.addAll(apisList);
        List<String> viewFileList = FileUtil.getDirChildrenFiles(viewPath, "js", "jsp", "html", "htm", "ftl");
        // 移动端页面文件
        List<String> viewMobileList = FileUtil.getDirChildrenFiles(viewMobilePath, "js", "html", "htm", "ftl","vue");
        viewFileList.addAll(viewMobileList);
        //
        // 数据库接口文件内容
        String dataBaseText = FileUtil.fileReadText(dataBaseApiPath);
        // noUsedApisList先存放所有接口，通过遍历一次所有文件移除掉使用的接口，剩下为未使用接口
        for (String url : viewFileList){
            if (CollectionUtil.isBlank(noUsedApisList)){
                break;
            }
            String text = FileUtil.fileReadText(url);
            // 获取当前文件的红讯接口
            Set<String> redXunApiList = new HashSet<>();
            if (StringUtil.containOfOne(text,"<redxun")){
                List<String> redunApiList = getRedxunApiList(text,"<redxun");
                redXunApiList.addAll(redunApiList);
            }
            if (StringUtil.containOfOne(text,"<rx")){
                List<String> rxApiList = getRedxunApiList(text, "<rx");
                redXunApiList.addAll(rxApiList);
            }
            // 还差第三方接口、存在/api/efile/遗漏的情况
            for (int i = noUsedApisList.size()-1; i >=0; i--) {
                String apiStr = noUsedApisList.get(i);
                // 测试接口判断
                if (StringUtil.containOf(apiStr,"/api/data/")){
                    noUsedApisList.remove(i);
                    continue;
                }
                // 红讯接口判断
                if (redXunApiList.contains(apiStr)) {
                    noUsedApisList.remove(i);
                    continue;
                }
                // 数据库库接口判断 /api/efile/sys/core/sysMenuMgr/del这种后面没标识的判断不了
                boolean isDataBaseContain = isContainApi(dataBaseText, apiStr);
                if(isDataBaseContain){
                    noUsedApisList.remove(i);
                    continue;
                }
                // 普通接口判断，会遗漏换行写的
                boolean isContain = isContainApi(text, apiStr);
                if(isContain){
                    noUsedApisList.remove(i);
                    continue;
                }
                // 动态接口判断，会遗漏换行写的
                if (StringUtil.containOfOne(apiStr,"{","}")){
                    apiStr = apiStr.replaceAll("\\{.*?\\}","[\\\\S]*?");
                    Boolean isContainDynamicApi = isContainDynamicApi(text, apiStr);
                    if(isContainDynamicApi){
                        noUsedApisList.remove(i);
                        continue;
                    }
                }
            }
        }
        final StringBuilder noUsedStringBuilder = new StringBuilder();
        for (String apiStr : noUsedApisList){
            noUsedStringBuilder.append(apiStr).append("\n");
        }
        // 写入文件
        final String allApiStr = allApiStringBuilder.toString();
        final String noUsedStr = noUsedStringBuilder.toString();
        FileUtil.fileWriteNew(allApiUrl, allApiStr, false);
        FileUtil.fileWriteNew(noUsedUrl, noUsedStr, false);
        //
        final long consume = System.currentTimeMillis() - start;
        LogUtil.print("CodeAnalyzerUtil_statisticsApis_1", "统计接口完成，耗时："+consume, allApiUrl, noUsedUrl);
        //
        final String[] array = {allApiUrl, noUsedUrl};
        return array;
    }

    private static final List<String> getApinameList(final String text){
        List<String> apiList = new ArrayList<>();
        //
        Pattern pattern = Pattern.compile("@RequestMapping[\\s\\S]*?\\([\\s\\S]*?value[\\s\\S]*?=[\\s\\S]*?\"([\\s\\S]*?)\"[\\s\\S]*?\\)");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return apiList;
        }
        // 获取 value 参数值
        String rootPath = matcher.group(1);
        while (matcher.find()) {
            String path = matcher.group(1);
            String apiName = rootPath + path;
            apiList.add(apiName);
        }
        return apiList;
    }

    private static final Boolean isContainApi(final String text, final String api){
        List<String> ruleList = new ArrayList<>();
        ruleList.add(api + "'");
        ruleList.add(api + "\"");
        ruleList.add(api + " \"");
        ruleList.add(api + ".do");
        ruleList.add(api + "?");
        //
        for(String rule : ruleList){
            if(StringUtil.containOf(text, rule)){
                return true;
            }
        }
        return false;
    }

    /**
     * 2023-08-03 动态接口判断
     * @param text
     * @param rgxApi
     * @return
     */
    public static final Boolean isContainDynamicApi(final String text, final String rgxApi){
        Pattern pattern = Pattern.compile(rgxApi);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()){
            return true;
        }
        return false;
    }

    /**
     * 2023-08-03 获取红讯的接口
     * @param text
     * @param preTag <redxun 、 <rx
     * @return
     */
    private static final List<String> getRedxunApiList(final String text, final String preTag){
        // 获取红讯数据
        Pattern pattern = Pattern.compile(preTag+"[\\s\\S]*?baseUrl=\"([\\s\\S]*?)\"[\\s\\S^>]*?/>");
        Matcher matcher = pattern.matcher(text);
        List<String> redXunApiList = new ArrayList<>();
        while (matcher.find()){
            String preApi = matcher.group(1);
            // 去掉特殊字符
            preApi = preApi.replaceAll("[\\ \\r\\n\\t]","");
            // 拼接红讯的增、删、改、查
            String delApi = preApi + "/del";
            String getApi = preApi + "/get";
            String editApi = preApi + "/edit";
            String saveApi = preApi + "/save";
            redXunApiList.add(delApi);
            redXunApiList.add(getApi);
            redXunApiList.add(editApi);
            redXunApiList.add(saveApi);
        }
        return redXunApiList;
    }

    /**
     * 2023-06-15 mapper扫描
     */
    public static final String[] statisticsMapper(String mapperPath, String javaScanPath,
                                                  String allMapperUrl, String noUsedMapperUrl){
        //
        final long start = System.currentTimeMillis();
        // 统计所有mapper
        List<String> javaMapperFileList = FileUtil.getDirChildrenFiles(mapperPath, "xml");
        List<String> allMapperList = new ArrayList<>();
        for(String url : javaMapperFileList){
            List<String> completeMapperName = createCompleteMapperName(url);
            allMapperList.addAll(completeMapperName);
        }
        final StringBuilder allMapperStringBuilder = new StringBuilder();
        for (String mapperStr : allMapperList){
            allMapperStringBuilder.append(mapperStr).append("\n");
        }
        // 统计未使用mapper
        List<String> noUsedMapperList = new ArrayList<>();
        noUsedMapperList.addAll(allMapperList);
        List<String> javaFileList = FileUtil.getDirChildrenFiles(javaScanPath, "java");
        // noUsedMapperList先存放所有mapper，通过遍历一次所有文件移除掉使用的mapper，剩下为未使用mapper
        for (String url : javaFileList){
            if (CollectionUtil.isBlank(noUsedMapperList)){
                break;
            }
            String text = FileUtil.fileReadText(url);
            for (int i = noUsedMapperList.size()-1; i >=0; i--) {
                String mapperStr = noUsedMapperList.get(i);
                // 加上MM
                mapperStr = "MM." + mapperStr;
                // 去掉测试
                if (StringUtil.containOf(mapperStr,"DemoMapper")) {
                    noUsedMapperList.remove(i);
                    continue;
                }
                // java中判断
                Boolean isContainMapper = isContainMapper(text, mapperStr);
                if (isContainMapper){
                    noUsedMapperList.remove(i);
                    continue;
                }
            }
        }
        final StringBuilder noUsedMapperStringBuilder = new StringBuilder();
        for (String mapperStr : noUsedMapperList){
            noUsedMapperStringBuilder.append(mapperStr).append("\n");
        }
        // 写入文件
        final String allMaperStr = allMapperStringBuilder.toString();
        final String noUsedMapperdStr = noUsedMapperStringBuilder.toString();
        FileUtil.fileWriteNew(allMapperUrl, allMaperStr, false);
        FileUtil.fileWriteNew(noUsedMapperUrl, noUsedMapperdStr, false);
        //
        final long consume = System.currentTimeMillis() - start;
        LogUtil.print("CodeAnalyzerUtil_statisticsMapper_1", "统计Mapper完成，耗时："+consume, allMapperUrl, noUsedMapperUrl);
        //
        final String[] array = {allMapperUrl, noUsedMapperUrl};
        return array;
    }

    /**
     * 2023-08-03 获取当前文件的所有mapper名 类名.方法名
     * @param url
     * @return
     */
    public static final List<String> createCompleteMapperName(final String url){
        List<String> completeMapperList = new ArrayList<>();
        String className = FileUtil.getFileName(url, false);
        // 首字母小写
        StringBuilder sb = new StringBuilder(className);
        char firstChar = className.charAt(0);
        firstChar = Character.toLowerCase(firstChar);
        sb.setCharAt(0,firstChar);
        className = sb.toString();
        // 拼接类名加方法
        String text = FileUtil.fileReadText(url);
        List<String> sqlXmlIdList = getSqlXmlId(text);
        if (CollectionUtil.isBlank(sqlXmlIdList)){
            return completeMapperList;
        }
        for (String methodNameStr:sqlXmlIdList){
            String completeName = className + "." + methodNameStr;
            completeMapperList.add(completeName);
        }
        return completeMapperList;
    }

    /**
     * 2023-08-03 判断当前文件是否使用指定mapper
     * @param mapperStr
     * @param text
     * @return
     */
    public static final Boolean isContainMapper(final String text, final String mapperStr){
        List<String> list = StringUtil.splitList(mapperStr, ".");
        StringBuilder RegxSb = new StringBuilder();
        if (CollectionUtil.isBlank(list)){
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            String str = list.get(i);
            if (i == 0){
                RegxSb.append(str);
                continue;
            }
            RegxSb.append("\\ *?").append("\\.").append("\\ *?").append(str);
        }
        String RegxStr = RegxSb.toString();
        RegxStr = RegxStr+"\\ *?\\(.*?\\)";
        Pattern pattern = Pattern.compile(RegxStr);
        Matcher matcher = pattern.matcher(text);
        boolean isContainMapperBool = matcher.find();
        if (isContainMapperBool){
            return true;
        }
        return false;
    }

    /**
     * 2023-08-01 获取当前xml中所有的id
     * @param text
     */
    public static final List<String> getSqlXmlId(final String text){
        List<String> list = new ArrayList<>();
        List<String> insertList = getSqlXmlIdByType(text, "insert");
        List<String> deleteList = getSqlXmlIdByType(text, "delete");
        List<String> selectList = getSqlXmlIdByType(text, "select");
        List<String> updateList = getSqlXmlIdByType(text, "update");
        // 合并
        list.addAll(insertList);
        list.addAll(deleteList);
        list.addAll(selectList);
        list.addAll(updateList);
        return list;
    }

    /**
     * 2023-08-03 获取指定类型的所有id
     * @param text
     * @param type insert delete select update
     * @return
     */
    public static final List<String> getSqlXmlIdByType(final String text, final String type){
        String RegxStr = "<"+type+"(.*)id=\"(.*?)\"";
        Pattern pattern = Pattern.compile(RegxStr);
        Matcher matcher = pattern.matcher(text);
        List<String> List = new ArrayList<>();
        while (matcher.find()){
            String idStr = matcher.group(2);
            List.add(idStr);
        }
        return List;
    }

//

}