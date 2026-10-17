package xc.crabapple.core.file.text;

import java.util.List;
import java.util.Map;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;

/**
 * 2022-08-13
 * 统一字符串个数
 */
public final class StatisticStrUtil {

    /**
     * 2022-08-13
     * @param content
     * @param keyword
     * @return
     */
    public static final int getCount(final String content, final String keyword){
        int count = StringUtil.getCount(content, keyword);
        return count;
    }

    /**
     * 2022-08-13
     * @param url
     * @param keyword
     * @return
     */
    public static final int getCountByFile(final String url, final String keyword){
        boolean realFile = FileUtil.isRealFile(url);
        if(!realFile){
            return 0;
        }
        //
        String content = FileUtil.fileReadText(url);
        //
        int count = getCount(content, keyword);
        return count;
    }

    /**
     * 2022-08-13
     * @param path
     * @param keyword
     * @return
     */
    public static final int getCountByPath(final String path, final String keyword) {
        Map<String, List<String>> map = FileUtil.getDirChildren(path);
        List<String> fileList = map.get("file");
        if(CollectionUtil.isBlank(fileList)){
            return 0;
        }
        //
        int counts = 0;
        for(int i = 0; i < fileList.size(); i++){
            String url = fileList.get(i);
            int count = getCountByFile(url, keyword);
            counts += count;
        }
        return counts;
    }

}