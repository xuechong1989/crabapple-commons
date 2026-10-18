package xc.crabapple.core.device;

import java.io.Serializable;

/**
 * 2019-07-29 2.3.4
 * jvm相关信息
 */
public final class JvmInfo implements Serializable {

    public static final long serialVersionUID = 1L;

    private Runtime runtime;

    private Long maxMemory;  //java虚拟机（这个进程）能够从操纵系统挖到的最大内存（字节）
    private Long totalMemory;  //java虚拟机当前已经从操纵系统那里挖过来的内存大小（字节）
    //java程序运行中内存总是慢慢从操作系统那里挖，基本上是用多少挖多少，
    //但java虚拟机100％的情况下是会稍微多挖一点的，这些挖过来而又没有用上的内存，实际上就是freeMemory()，所以freeMemory()值一般很小，
    //但若在运行java程序时使用了-Xms，此时程序启动时就会无条件的从操作系统中挖-Xms后面定义的内存数，
    //此时挖来的内存可能大部分没用上，故此时freeMemory()会有些大（字节）
    private Long freeMemory;
    private Long usedMemory;  //java虚拟机当前已经使用的内存（字节）
    private String version;  //Java运行时环境版本
    private String vendor;  //Java运行时环境供应商
    private String vendorUrl;  //Java供应商的 URL
    private String home;  //Java安装目录
    private String vmSpecificationVersion;  //Java虚拟机规范版本
    private String vmSpecificationVendor;  //Java虚拟机规范供应商
    private String vmSpecificationName;  //Java虚拟机规范名称
    private String vmVersion;  //Java虚拟机实现版本
    private String vmVendor;  //Java虚拟机实现供应商
    private String vmName;  //Java虚拟机实现名称
    private String specificationVersion;  //Java运行时环境规范版本
    private String specificationVendor;  //Java运行时环境规范供应商
    private String specificationName;  //Java运行时环境规范名称
    private String classVersion;  //Java类格式版本号
    private String classPath;  //Java类路径
    private String libraryPath;  //加载库时搜索的路径列表
    private String ioTmpdir;  //默认的临时文件路径
    private String compiler;  //要使用的 JIT 编译器的名称
    private String extDirs;  //一个或多个扩展目录的路径

    public JvmInfo(){
        runtime = Runtime.getRuntime();
        settings();
    }

    public JvmInfo(final Runtime runtime){
        this.runtime = runtime;
        settings();
    }

    private void settings(){
        maxMemory = runtime.maxMemory();
        totalMemory = runtime.totalMemory();
        freeMemory = runtime.freeMemory();
        usedMemory = totalMemory - freeMemory;
        version = System.getProperty("java.version");
        vendor = System.getProperty("java.vendor");
        vendorUrl = System.getProperty("java.vendor.url");
        home = System.getProperty("java.home");
        vmSpecificationVersion = System.getProperty("java.vm.specificationVersion");
        vmSpecificationVendor = System.getProperty("java.vm.specification.vendor");
        vmSpecificationName = System.getProperty("java.vm.specification.name");
        vmVersion = System.getProperty("java.vm.version");
        vmVendor = System.getProperty("java.vm.vendor");
        vmName = System.getProperty("java.vm.name");
        specificationVersion = System.getProperty("java.specification.version");
        specificationVendor = System.getProperty("java.specification.vendor");
        specificationName = System.getProperty("java.specification.name");
        classVersion = System.getProperty("java.class.version");
        classPath = System.getProperty("java.class.path");
        libraryPath = System.getProperty("java.library.path");
        ioTmpdir = System.getProperty("java.io.tmpdir");
        compiler = System.getProperty("java.compiler");
        extDirs = System.getProperty("java.ext.dirs");
    }

    @Override
    public String toString() {
        return "JvmInfo{" +
                "runtime=" + runtime +
                ", maxMemory=" + maxMemory +
                ", totalMemory=" + totalMemory +
                ", freeMemory=" + freeMemory +
                ", usedMemory=" + usedMemory +
                ", version='" + version + '\'' +
                ", vendor='" + vendor + '\'' +
                ", vendorUrl='" + vendorUrl + '\'' +
                ", home='" + home + '\'' +
                ", vmSpecificationVersion='" + vmSpecificationVersion + '\'' +
                ", vmSpecificationVendor='" + vmSpecificationVendor + '\'' +
                ", vmSpecificationName='" + vmSpecificationName + '\'' +
                ", vmVersion='" + vmVersion + '\'' +
                ", vmVendor='" + vmVendor + '\'' +
                ", vmName='" + vmName + '\'' +
                ", specificationVersion='" + specificationVersion + '\'' +
                ", specificationVendor='" + specificationVendor + '\'' +
                ", specificationName='" + specificationName + '\'' +
                ", classVersion='" + classVersion + '\'' +
                ", classPath='" + classPath + '\'' +
                ", libraryPath='" + libraryPath + '\'' +
                ", ioTmpdir='" + ioTmpdir + '\'' +
                ", compiler='" + compiler + '\'' +
                ", extDirs='" + extDirs + '\'' +
                '}';
    }

    public Long getMaxMemory() {
        return maxMemory;
    }

    public Long getTotalMemory() {
        return totalMemory;
    }

    public Long getFreeMemory() {
        return freeMemory;
    }

    public Long getUsedMemory() {
        return usedMemory;
    }

    public String getVersion() {
        return version;
    }

    public String getVendor() {
        return vendor;
    }

    public String getVendorUrl() {
        return vendorUrl;
    }

    public String getHome() {
        return home;
    }

    public String getVmSpecificationVersion() {
        return vmSpecificationVersion;
    }

    public String getVmSpecificationVendor() {
        return vmSpecificationVendor;
    }

    public String getVmSpecificationName() {
        return vmSpecificationName;
    }

    public String getVmVersion() {
        return vmVersion;
    }

    public String getVmVendor() {
        return vmVendor;
    }

    public String getVmName() {
        return vmName;
    }

    public String getSpecificationVersion() {
        return specificationVersion;
    }

    public String getSpecificationVendor() {
        return specificationVendor;
    }

    public String getSpecificationName() {
        return specificationName;
    }

    public String getClassVersion() {
        return classVersion;
    }

    public String getClassPath() {
        return classPath;
    }

    public String getLibraryPath() {
        return libraryPath;
    }

    public String getIoTmpdir() {
        return ioTmpdir;
    }

    public String getCompiler() {
        return compiler;
    }

    public String getExtDirs() {
        return extDirs;
    }

}
