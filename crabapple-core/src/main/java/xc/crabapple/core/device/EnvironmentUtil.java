package xc.crabapple.core.device;

import java.math.BigDecimal;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;

/**
 * 2017-03-09 1.0.0
 * 服务器端操作系统信息类库
 */
public final class EnvironmentUtil {

    //操作系统
    private static final String WINDOWS = "windows";
    private static final String LINUX = "linux";
    private static final String MAC = "mac";

    /**
     * 2017-03-09 1.0.0
     * 当前运行的操作系统是否为windows
     */
    public static final boolean isWindows(){
        boolean flag = p_isTargetOS(WINDOWS);
        return flag;
    }

    /**
     * 2017-03-09 1.0.0
     * 当前运行的操作系统是否为linux
     */
    public static final boolean isLinux(){
        boolean flag = p_isTargetOS(LINUX);
        return flag;
    }

    /**
     * 2020-05-19 6.0.0
     * 当前运行的操作系统是否为Mac
     */
    public static final boolean isMac(){
        boolean flag = p_isTargetOS(MAC);
        return flag;
    }

    /**
     * 2020-05-19 6.0.0
     * 当前运行的操作系统是否为linux
     */
    private static final boolean p_isTargetOS(final String osType){
        String osName = System.getProperty("os.name");
        osName = StringUtil.toLowerCase(osName);
        if(StringUtil.containOf(osName, osType)){
            return true;
        }
        if(StringUtil.containOf(osType, osName)){
            return true;
        }
        return false;
    }

    /**
     * 2019-07-30 2.3.4
     * 操作系统CPU信息
     */
    public static final CpuInfo getCpuInfo(){
        CpuInfo cpuInfo = new CpuInfo();
        return cpuInfo;
    }

    /**
     * 2017-03-09 1.0.0 - 2019-07-30 2.3.4
     * 操作系统磁盘信息
     */
    public static final DiskInfo getDiskInfo(){
        DiskInfo diskInfo = new DiskInfo();
        return diskInfo;
    }

    /**
     * 通过指定的阈值获取大于等于这个值的磁盘容量使用信息
     * @param gtEqValue 磁盘使用率的阙值，获取的是大于等于这个值的信息，取值范围0-100 大于等于的值 比率是100 即最大就是100
     * @return
     */
    public static final Map<String,String> getGtEqDiskUsedRatio(final Integer gtEqValue){
        Map<String, String> resultMap = new HashMap<>();
        if (gtEqValue == null) {
            LogUtil.error("EnvironmentUtil_getGtDiskUsedRatio_11", "没有获取到参数值");
            return resultMap;
        }
        Map<String, Integer> winDriverUseRatio = getDiskUsedRatio();
        if (CollectionUtil.isBlank(winDriverUseRatio)) {
            return resultMap;
        }
        Set<Map.Entry<String, Integer>> entries = winDriverUseRatio.entrySet();
        for (Map.Entry<String, Integer> map : entries) {
            Integer value = map.getValue();
            if (value >= gtEqValue) {
                String key = map.getKey();
                String valueStr = value + "%";
                resultMap.put(key, valueStr);
            }
        }
        return resultMap;
    }

    /**
     * 2025-08-14 代码重新整理，屏蔽日志
     * 获取当前系统的磁盘名以及对应的使用率 数值范围是0-100
     * @return
     */
    public static final Map<String, Integer> getDiskUsedRatio(){
        Map<String, Integer> map = new HashMap<>();
        //
        FileSystem fileSystem = FileSystems.getDefault();
        if (fileSystem == null) {
            // LogUtil.warn("EnvironmentUtil_getDiskUsedRatio_11", "没有获取默认系统文件！");
            return map;
        }
        Iterable<Path> pathIterable = fileSystem.getRootDirectories();
        if(pathIterable == null){
            // LogUtil.warn("EnvironmentUtil_getDiskUsedRatio_12", "没有获取根目录！");
            return map;
        }
        Iterator<Path> pathIterator = pathIterable.iterator();
        while(pathIterator.hasNext()){
            Path path = pathIterator.next();
            boolean isReadable = Files.isReadable(path);
            if(!isReadable){
                // LogUtil.warn("EnvironmentUtil_getDiskUsedRatio_10", throwable, "没有读取权限！", path);
                continue;
            }
            FileStore fileStore = null;
            try{
                fileStore = Files.getFileStore(path);
            }catch (Throwable throwable){
                // LogUtil.error("EnvironmentUtil_getDiskUsedRatio_t13", throwable, "获取文件列表失败！", path);
                continue;
            }
            if (fileStore == null) {
                // LogUtil.warn("EnvironmentUtil_getDiskUsedRatio_13", "获取文件列表失败！", path);
                continue;
            }
            // 跳过不支持的文件系统
            String type = fileStore.type();
            if (StringUtil.equalsOfOne(type, "tmpfs", "cdrom", "network")) {
                continue;
            }
            //
            Long totalSpace = 0L;
            Long unallocatedSpace = 0L;
            try {
                totalSpace = fileStore.getTotalSpace();
                unallocatedSpace = fileStore.getUnallocatedSpace();
            } catch (Throwable throwable) {
                // LogUtil.error("EnvironmentUtil_getDiskUsedRatio_13", throwable, "获取磁盘大小失败", path, fileStore, totalSpace, unallocatedSpace);
                continue;
            }
            if (totalSpace == null || totalSpace.equals(0L)) {
                continue;
            }
            Double percentDouble = (1 - ((unallocatedSpace * 1.0) / totalSpace));
            if(percentDouble == null || Double.isNaN(percentDouble) || Double.isInfinite(percentDouble)){
                // LogUtil.warn("EnvironmentUtil_getDiskUsedRatio_14", "获取磁盘大小数据异常", fileStore, totalSpace, unallocatedSpace);
                continue;
            }
            BigDecimal bigDecimal = new BigDecimal(percentDouble);
            bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP);
            percentDouble = bigDecimal.doubleValue();
            Integer percentInt = (int) (percentDouble * 100);
            final String name = fileStore.toString();
            map.put(name, percentInt);
        }
        return map;
    }

    /**
     * 2017-03-09 1.0.0 - 2019-07-30 2.3.4
     * JVM信息
     */
    public static final JvmInfo getJvmInfo(){
        JvmInfo jvmInfo = new JvmInfo();
        return jvmInfo;
    }

    /**
     * 2019-07-30 2.3.4
     * 操作系统内存信息
     */
    public static final MemoryInfo getMemoryInfo(){
        MemoryInfo memoryInfo = new MemoryInfo();
        return memoryInfo;
    }

    /**
     * 2019-07-30 2.3.4
     * 操作系统信息
     */
    public static final OSInfo getOSInfo(){
        OSInfo osInfo = new OSInfo();
        return osInfo;
    }

    /**
     * 2019-07-30 2.3.4
     * 当前线程信息
     */
    public static final ThreadInfo getThreadInfo(){
        ThreadInfo threadInfo = new ThreadInfo();
        return threadInfo;
    }

    /**
     * 2017-03-09 1.0.0 - 2019-07-30 2.3.4
     * 获得操作系统CPU、内存、JVM信息
     */
    public static final EnvironmentInfo getEnvironmentInfo(){
        EnvironmentInfo environmentInfo = new EnvironmentInfo();
        return environmentInfo;
    }

}

