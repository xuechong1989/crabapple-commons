package xc.crabapple.core.device;

import java.io.File;
import java.io.Serializable;

/**
 * 2019-07-29 2.3.4
 * 当前操作系统的磁盘信息
 */
public final class DiskInfo implements Serializable {

    public static final long serialVersionUID = 1L;

    private File file;

    private Long totalSpace;  //总空间
    private Long usableSpace;  //可用空间
    private Long usedSpace;  //已使用空间
    private Long freeSpace;  //空闲空间

    public DiskInfo(){
        file = new File("/");  //获取磁盘分区列表
        settings();
    }

    public DiskInfo(final File file){
        this.file = file;
        settings();
    }

    private void settings(){
        totalSpace = file.getTotalSpace();
        usableSpace = file.getUsableSpace();
        usedSpace = totalSpace - usableSpace;
        freeSpace = file.getFreeSpace();
    }

    @Override
    public String toString() {
        return "DiskInfo{" +
                "file=" + file +
                ", totalSpace=" + totalSpace +
                ", usableSpace=" + usableSpace +
                ", usedSpace=" + usedSpace +
                ", freeSpace=" + freeSpace +
                '}';
    }

    public Long getTotalSpace() {
        return totalSpace;
    }

    public Long getUsableSpace() {
        return usableSpace;
    }

    public Long getUsedSpace() {
        return usedSpace;
    }

    public Long getFreeSpace() {
        return freeSpace;
    }

}
