package xc.crabapple.core.device;

import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.io.IOUtil;
import com.sun.management.OperatingSystemMXBean;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.Scanner;

/**
 * 2019-07-29 2.3.4
 * 当前操作系统的CPU信息
 */
public final class CpuInfo implements Serializable {

    public static final long serialVersionUID = 1L;

    private OperatingSystemMXBean operatingSystemMXBean;

    private Integer availableProcessors;  //cpu核心数
    private Long processCpuTime;  //
    private Double processCpuLoad;  //
    private Double systemCpuLoad;  //系统的负载情况，常用
    private String cpuId;  //

    public CpuInfo(){
        operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        settings();
    }

    public CpuInfo(final OperatingSystemMXBean operatingSystemMXBean){
        this.operatingSystemMXBean = operatingSystemMXBean;
        settings();
    }

    private void settings(){
        availableProcessors = operatingSystemMXBean.getAvailableProcessors();
        processCpuTime = operatingSystemMXBean.getProcessCpuTime();
        processCpuLoad = operatingSystemMXBean.getProcessCpuLoad();
        systemCpuLoad = operatingSystemMXBean.getSystemCpuLoad();
        cpuId = computeCpuId();
    }

    @Override
    public String toString() {
        return "CpuInfo{" +
                "operatingSystemMXBean=" + operatingSystemMXBean +
                ", availableProcessors=" + availableProcessors +
                ", processCpuTime=" + processCpuTime +
                ", processCpuLoad=" + processCpuLoad +
                ", systemCpuLoad=" + systemCpuLoad +
                ", cpuId='" + cpuId + '\'' +
                '}';
    }

    /**
     * 2017-03-09 1.0.0 - 2019-07-30 2.3.4
     */
    private String computeCpuId(){
        boolean flag = EnvironmentUtil.isWindows();
        //windows
        if(flag){
            OutputStream outputStream = null;
            InputStream inputStream = null;
            Scanner scanner = null;
            try{
                String[] array = new String[] {"wmic", "cpu", "get", "ProcessorId"};
                Runtime runtime = Runtime.getRuntime();
                Process process = runtime.exec(array);
                outputStream = process.getOutputStream();
                inputStream = process.getInputStream();
                scanner = new Scanner(inputStream);
                String property = scanner.next();
                String serial = scanner.next();
                return serial;
            }catch(Throwable throwable){
                LogUtil.error("CpuInfo_computeCpuId_t1", throwable);
                return "";
            }finally{
                IOUtil.close(scanner, inputStream, outputStream);
            }
        }
        //linux
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        LineNumberReader lineNumberReader = null;
        try{
            String cmd = "dmidecode -t processor | grep 'ID'";
            String[] array = {"/bin/sh", "-c", cmd};
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(array);
            inputStream = process.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);
            lineNumberReader = new LineNumberReader(inputStreamReader);
            String cpuId = lineNumberReader.readLine();
            return cpuId;
        }catch(Throwable throwable){
            LogUtil.error("CpuInfo_computeCpuId_t2", throwable);
            return "";
        }finally{
            IOUtil.close(lineNumberReader, inputStreamReader, inputStream);
        }
    }

    public Integer getAvailableProcessors() {
        return availableProcessors;
    }

    public Long getProcessCpuTime() {
        return processCpuTime;
    }

    public Double getProcessCpuLoad() {
        return processCpuLoad;
    }

    public Double getSystemCpuLoad() {
        return systemCpuLoad;
    }

    public String getCpuId() {
        return cpuId;
    }

}
