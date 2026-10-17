package xc.crabapple.core.device;

import com.sun.management.OperatingSystemMXBean;

import java.io.Serializable;
import java.lang.management.ManagementFactory;

/**
 * 2019-07-29 2.3.4
 * 当前操作系统的内存信息
 */
public final class MemoryInfo implements Serializable {

    public static final long serialVersionUID = 1L;

    private OperatingSystemMXBean operatingSystemMXBean;

    private Long totalPhysicalMemorySize;  //总物理内存
    private Long freePhysicalMemorySize;  //可用物理内存
    private Long usedPhysicalMemorySize;  //已使用的物理内存

    public MemoryInfo(){
        operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        settings();
    }

    public MemoryInfo(final OperatingSystemMXBean operatingSystemMXBean){
        this.operatingSystemMXBean = operatingSystemMXBean;
        settings();
    }

    private void settings(){
        totalPhysicalMemorySize = operatingSystemMXBean.getTotalPhysicalMemorySize();
        freePhysicalMemorySize = operatingSystemMXBean.getFreePhysicalMemorySize();
        usedPhysicalMemorySize = operatingSystemMXBean.getTotalPhysicalMemorySize();
    }

    @Override
    public String toString() {
        return "MemoryInfo{" +
                "operatingSystemMXBean=" + operatingSystemMXBean +
                ", totalPhysicalMemorySize=" + totalPhysicalMemorySize +
                ", freePhysicalMemorySize=" + freePhysicalMemorySize +
                ", usedPhysicalMemorySize=" + usedPhysicalMemorySize +
                '}';
    }

    public Long getTotalPhysicalMemorySize() {
        return totalPhysicalMemorySize;
    }

    public Long getFreePhysicalMemorySize() {
        return freePhysicalMemorySize;
    }

    public Long getUsedPhysicalMemorySize() {
        return usedPhysicalMemorySize;
    }

}
