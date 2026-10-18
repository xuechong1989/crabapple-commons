package xc.crabapple.core.device;

import com.sun.management.OperatingSystemMXBean;

import java.io.Serializable;
import java.lang.management.ManagementFactory;

/**
 * 2019-07-30 2.3.4
 * 当前jvm所在环境信息
 */
public final class EnvironmentInfo implements Serializable {

    public static final long serialVersionUID = 1L;

    //
    private OperatingSystemMXBean operatingSystemMXBean;
    private Runtime runtime;
    //
    private CpuInfo cpuInfo;
    private DiskInfo diskInfo;
    private MemoryInfo memoryInfo;
    private JvmInfo jvmInfo;
    private ThreadInfo threadInfo;
    private OSInfo osInfo;

    public EnvironmentInfo() {
        operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        runtime = Runtime.getRuntime();
        settings();
    }

    public EnvironmentInfo(final OperatingSystemMXBean operatingSystemMXBean, final Runtime runtime) {
        this.operatingSystemMXBean = operatingSystemMXBean;
        this.runtime = runtime;
        settings();
    }

    private void settings() {
        cpuInfo = new CpuInfo(operatingSystemMXBean);
        diskInfo = new DiskInfo();
        memoryInfo = new MemoryInfo(operatingSystemMXBean);
        jvmInfo = new JvmInfo(runtime);
        threadInfo = new ThreadInfo();
        osInfo = new OSInfo();
    }

    @Override
    public String toString() {
        return "EnvironmentInfo{" +
                "operatingSystemMXBean=" + operatingSystemMXBean +
                ", runtime=" + runtime +
                ", cpuInfo=" + cpuInfo +
                ", diskInfo=" + diskInfo +
                ", memoryInfo=" + memoryInfo +
                ", jvmInfo=" + jvmInfo +
                ", threadInfo=" + threadInfo +
                ", osInfo=" + osInfo +
                '}';
    }

    public OSInfo getOsInfo() {
        return osInfo;
    }

    public JvmInfo getJvmInfo() {
        return jvmInfo;
    }

    public CpuInfo getCpuInfo() {
        return cpuInfo;
    }

    public MemoryInfo getMemoryInfo() {
        return memoryInfo;
    }

    public DiskInfo getDiskInfo() {
        return diskInfo;
    }

    public ThreadInfo getThreadInfo() {
        return threadInfo;
    }

}
