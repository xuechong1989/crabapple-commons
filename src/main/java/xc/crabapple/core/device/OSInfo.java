package xc.crabapple.core.device;

import java.io.Serializable;

/**
 * 2019-07-29 2.3.4
 */
public final class OSInfo implements Serializable {

    public static final long serialVersionUID = 1L;

    private String osName;  //操作系统的名称，windows/linux
    private String osArch;  //操作系统架构指令集
    private String osVersion;  //操作系统版本
    private String fileSeparator;  //文件分隔符（在 UNIX 系统中是"/"）
    private String pathSeparator;  //路径分隔符（在 UNIX 系统中是":"）
    private String lineSeparator;  //行分隔符（在 UNIX 系统中是"/n"）
    private String userName;  //用户的账户名称
    private String userHome;  //用户的主目录
    private String userDir;  //用户的当前工作目录

    public OSInfo(){
        settings();
    }

    private void settings(){
        osName = System.getProperty("os.name");
        osArch = System.getProperty("os.arch");
        osVersion = System.getProperty("os.version");
        fileSeparator = System.getProperty("file.separator");
        pathSeparator = System.getProperty("path.separator");
        lineSeparator = System.getProperty("line.separator");
        userName = System.getProperty("user.name");
        userHome = System.getProperty("user.home");
        userDir = System.getProperty("user.dir");
    }

    @Override
    public String toString() {
        return "OSInfo{" +
                "osName='" + osName + '\'' +
                ", osArch='" + osArch + '\'' +
                ", osVersion='" + osVersion + '\'' +
                ", fileSeparator='" + fileSeparator + '\'' +
                ", pathSeparator='" + pathSeparator + '\'' +
                ", lineSeparator='" + lineSeparator + '\'' +
                ", userName='" + userName + '\'' +
                ", userHome='" + userHome + '\'' +
                ", userDir='" + userDir + '\'' +
                '}';
    }

    public String getOsName() {
        return osName;
    }

    public String getOsArch() {
        return osArch;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getFileSeparator() {
        return fileSeparator;
    }

    public String getPathSeparator() {
        return pathSeparator;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserHome() {
        return userHome;
    }

    public String getUserDir() {
        return userDir;
    }

}
