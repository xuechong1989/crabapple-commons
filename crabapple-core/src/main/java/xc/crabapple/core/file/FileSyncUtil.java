package xc.crabapple.core.file;

import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 2024-10-29
 * 目录与文件同步
 */
public final class FileSyncUtil {

    private static final long MAX_SIZE = 1024L * 1024L * 1024L * 5L;  // 5GB

    /**
     * 2024-11-28
     * 性能测试，扫描本地一个指定目录下的所有文件，并分别计算对应的plusMd5
     * @return
     */
    public static final void test(){
        long start = System.currentTimeMillis();
        //
        int count = 0;
        Map<String, List<String>> fromMap = FileUtil.getDirChildren("D:/syncdir");
        List<String> fromFileList = fromMap.get("file");
        int size = fromFileList.size();
        if(!CollectionUtil.isBlank(fromFileList)) {
            for (int i = 0; i < size; i++) {
                String fromFileUrl = fromFileList.get(i);
                long start2 = System.currentTimeMillis();
                String plusMd5 = FileUtil.getPlusHash(fromFileUrl);
                count++;
                File file = new File(fromFileUrl);
                long fileSize = file.length();
                LogUtil.print("FileSyncUtil_test_2", count+"/"+size, fileSize, (System.currentTimeMillis()-start2)+"ms", fromFileUrl);
            }
        }
        //
        LogUtil.print("FileSyncUtil_test_10", "计算plusMd5完成，共处理文件："+count, "用时："+(System.currentTimeMillis()-start));
    }

    /**
     * 2024-10-31
     * 目录fromPath同步到目录toPath
     * @return
     */
    public static final boolean sync(){
        final long start = System.currentTimeMillis();
        final boolean flag = sync("D:/syncdir", "E:/syncdir", true);
        LogUtil.println("FileSyncUtil_sync_1000", "总耗时："+(System.currentTimeMillis()-start)+"ms");
        return flag;
    }

    /**
     * 2024-10-29
     * 目录fromPath同步到目录toPath
     * @param fromPath
     * @param toPath
     * @return
     */
    public static final boolean sync(final String fromPath, final String toPath, final boolean fast){
        LogUtil.print("FileSyncUtil_sync_1", "程序已启动", fromPath, toPath);
        //
        final String fromPathStd = UrlUtil.getStandardPath(fromPath);
        final String toPathStd = UrlUtil.getStandardPath(toPath);
        if(fromPathStd == null || toPathStd == null){
            return false;
        }
        if(!FileUtil.exists(fromPathStd)){
            return false;
        }
        // 场景1：目录toPath不存在
        if(!FileUtil.exists(toPathStd)){
            FileUtil.dirMake(toPathStd);
            Map<String, List<String>> fromMap = FileUtil.getDirChildrenFirst(fromPathStd);
            //
            List<String> fromDirList = fromMap.get("dir");
            if(!CollectionUtil.isBlank(fromDirList)){
                for(int i = 0; i < fromDirList.size(); i++) {
                    String fromDir = fromDirList.get(i);
                    final boolean flag = FileUtil.copy(fromDir, toPathStd);
                    if(!flag){
                        LogUtil.error("FileSyncUtil_sync_10", "复制失败", fromPathStd, fromDir, toPathStd);
                        return false;
                    }
                }
            }
            List<String> fromFileList = fromMap.get("file");
            if(!CollectionUtil.isBlank(fromFileList)){
                for(int i = 0; i < fromFileList.size(); i++) {
                    String fromFileUrl = fromFileList.get(i);
                    final boolean flag = FileUtil.copy(fromFileUrl, toPathStd);
                    if(!flag){
                        LogUtil.error("FileSyncUtil_sync_11", "复制失败", fromPathStd, fromFileUrl, toPathStd);
                        return false;
                    }
                }
            }
            LogUtil.error("FileSyncUtil_sync_100", "同步完成", fromPathStd, toPathStd);
            return true;
        }
        // 场景2：目录都存在，对等同步
        Map<String, List<String>> fromMap = FileUtil.getDirChildren(fromPathStd);
        List<String> fromDirList = fromMap.get("dir");
        if(!CollectionUtil.isBlank(fromDirList)){
            for(int i = 0; i < fromDirList.size(); i++){
                String fromDir = fromDirList.get(i);
                String revalateFromPath = StringUtil.deleteFirst(fromDir, fromPathStd);
                if(StringUtil.startsWith(revalateFromPath, "/.cache")){
                    continue;
                }
                String toDir = toPathStd + revalateFromPath;
                FileUtil.dirMake(toDir);
            }
        }
        final List<String> fromFileList = fromMap.get("file");
        if(!CollectionUtil.isBlank(fromFileList)){
            for(int i = 0; i < fromFileList.size(); i++){
                final String fromFileUrl = fromFileList.get(i);
                if(StringUtil.containOf(fromFileUrl, " ")){
                    LogUtil.error("FileSyncUtil_sync_400", "文件名包含非法字符，程序已中断", fromFileUrl);
                    System.exit(1);
                    return false;
                }
                LogUtil.print("FileSyncUtil_sync_300", "同步文件", fromFileUrl);
                final String revalateFromFileUrl = StringUtil.deleteFirst(fromFileUrl, fromPathStd);
                final String toFileUrl = toPathStd + revalateFromFileUrl;
                if(!FileUtil.exists(toFileUrl)){
                    FileUtil.copy(fromFileUrl, toFileUrl);
                    continue;
                }
                final boolean eq = FileUtil.compareFile(fromFileUrl, toFileUrl, fast);
                if(!eq){
                    FileUtil.delete(toFileUrl);
                    FileUtil.copy(fromFileUrl, toFileUrl);
                }
            }
        }
        // 场景3：目录toPath中的目录或文件比目录fromPath多
        final Map<String, List<String>> toMap = FileUtil.getDirChildren(toPathStd);
        final List<String> toDirList = toMap.get("dir");
        if(!CollectionUtil.isBlank(toDirList)){
            for(int i = 0; i < toDirList.size(); i++){
                String toDir = toDirList.get(i);
                String revalateToPath = StringUtil.deleteFirst(toDir, toPathStd);
                String fromDir = fromPathStd + revalateToPath;
                if(FileUtil.exists(fromDir)){
                    continue;
                }
                FileUtil.delete(toDir);
            }
        }
        final List<String> toFileList = toMap.get("file");
        if(!CollectionUtil.isBlank(toFileList)){
            for(int i = 0; i < toFileList.size(); i++){
                final String toFileUrl = toFileList.get(i);
                if(!FileUtil.exists(toFileUrl)){
                    continue;
                }
                final String revalateToFileUrl = StringUtil.deleteFirst(toFileUrl, toPathStd);
                final String fromFileStr = fromPathStd + revalateToFileUrl;
                if(FileUtil.exists(fromFileStr)){
                    continue;
                }
                FileUtil.delete(toFileUrl);
            }
        }
        //
        LogUtil.print("FileSyncUtil_sync_101", "同步完成", fromPathStd, toPathStd);
        return true;
    }

    /**
     * 2024-10-31
     * 批量重命名
     * @param path
     * @param year
     * @param startIndex
     */
    public static final void rename(final String path, final int year, final int startIndex){
        Map<String, List<String>> map = FileUtil.getDirChildren(path);
        List<String> fileList = map.get("file");
        for(int i = 0; i < fileList.size(); i++){
            String url = fileList.get(i);
            if(StringUtil.isBlank(url)){
                continue;
            }
            String filetype = FileUtil.getFileType(url);
            filetype = StringUtil.toLowerCase(filetype);
            String sss = StringUtil.suplyZero(startIndex + i, 6);
            String newUrl = path + "/" + year + "_" + sss + "." + filetype;
            FileUtil.rename(url, newUrl);
        }
    }

}
