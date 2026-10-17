package xc.crabapple.core.file.text;

import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.url.UrlUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 2020-03-24 5.0.0
 * 2024-08-15 方法重写
 * 统计代码行数、代码行数统计、文件行数统计
 */
public final class StatisticLineUtil {

    /**
     * 2024-08-15
     * @param path 指定目录
     * @return int[] 元素0表示文件数量，元素1表示行数
     */
    public static final int[] getLinesByPath(final String path){
        final Set<String> textSet = FileUtil.getTextSet();
        final int[] intArray = getLinesByPath(path, textSet);
        return intArray;
    }

    /**
     * 2024-08-15
     * @param pathList 指定目录集合
     * @return int[] 元素0表示文件数量，元素1表示行数
     */
    public static final int[] getLinesByPath(final List<String> pathList){
        final Set<String> textSet = FileUtil.getTextSet();
        final int[] intArray = getLinesByPath(pathList, textSet);
        return intArray;
    }

    /**
     * 2024-08-15
     * @param pathList 指定目录集合
     * @param textSet
     * @return int[] 元素0表示文件数量，元素1表示行数
     */
    public static final int[] getLinesByPath(final List<String> pathList, final Set<String> textSet){
        if(CollectionUtil.isBlank(pathList)){
            final int[] intArray = {0, 0};
            return intArray;
        }
        int files = 0;
        int lines = 0;
        final int size = pathList.size();
        for(int i = 0; i < size; i++){
            final String path = pathList.get(i);
            final int[] array = getLinesByPath(path, textSet);
            final int tmpFiles = array[0];
            files += tmpFiles;
            final int tmpLines = array[1];
            lines += tmpLines;
        }
        final int[] intArray = {files, lines};
        return intArray;
    }

    /**
     * 2020-03-24 5.0.0
     * 获取指定目录下所有指定格式的文本文件的行数，包含空格及注释
     * @param path 指定目录
     * @param textSet 文件格式后缀，不包含点.，英文逗号分隔。如 java,jsp,html
     * @return int[] 元素0表示文件数量，元素1表示行数
     */
    public static final int[] getLinesByPath(final String path, final Set<String> textSet){
        if(StringUtil.isBlank(path)){
            final int[] intArray = {0, 0};
            return intArray;
        }
        String pathLocal = UrlUtil.getStandardPath(path);
        boolean realPath = FileUtil.isRealPath(pathLocal);
        if(!realPath){
            final int[] intArray = {0, 0};
            return intArray;
        }
        //
        if(CollectionUtil.isBlank(textSet)){
            final int[] intArray = {0, 0};
            return intArray;
        }
        //
        Map<String, List<String>> map = FileUtil.getDirChildren(pathLocal, textSet);
        List<String> fileList = map.get("file");
        if(CollectionUtil.isBlank(fileList)){
            final int[] intArray = {0, 0};
            return intArray;
        }
        //
        int lines = 0;
        for(String url : fileList){
            String fileType = FileUtil.getFileType(url);
            fileType = StringUtil.toLowerCase(fileType);
            //
            if(!textSet.contains(fileType)){
                continue;
            }
            final int[] array = getLinesByFile(url);
            final int tmpLines = array[1];
            lines += tmpLines;
        }
        final int fileCount = fileList.size();
        final int[] intArray = {fileCount, lines};
        return intArray;
    }

    /**
     * 2024-08-15
     * 获取单个指定的文本文件的行数，包含空格及注释
     * @param urlList 文件集合
     * @return int[] 元素0表示文件数量，元素1表示行数
     */
    public static final int[] getLinesByFile(final List<String> urlList){
        if(CollectionUtil.isBlank(urlList)){
            final int[] intArray = {0, 0};
            return intArray;
        }
        final int size = urlList.size();
        int lines = 0;
        for(int i = 0; i < size; i++){
            final String url = urlList.get(i);
            final int[] array = getLinesByFile(url);
            final int tmpLines = array[1];
            lines += tmpLines;
        }
        final int[] intArray = {size, lines};
        return intArray;
    }

    /**
     * 2020-03-24 5.0.0
     * 获取单个指定的文本文件的行数，包含空格及注释
     * @param url
     * @return int[] 元素0表示文件数量，元素1表示行数
     */
    public static final int[] getLinesByFile(final String url){
        if(StringUtil.isBlank(url)){
            final int[] intArray = {0, 0};
            return intArray;
        }
        boolean realFile = FileUtil.isRealFile(url);
        if(!realFile){
            final int[] intArray = {0, 0};
            return intArray;
        }
        //
        List<String> list = FileUtil.fileReadList(url);
        if(CollectionUtil.isBlank(list)){
            final int[] intArray = {0, 0};
            return intArray;
        }
        // 忽略空行
        int lines = 0;
        int size = list.size();
        for(int i = 0; i < size; i++){
            String line = list.get(i);
            line = StringUtil.deleteAll(line, " ");
            if(StringUtil.isBlank(line)){
                continue;
            }
            lines++;
        }
        final int[] intArray = {1, lines};
        return intArray;
    }

}