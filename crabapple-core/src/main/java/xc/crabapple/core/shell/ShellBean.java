package xc.crabapple.core.shell;

import xc.crabapple.core.data.bean.BeanBase;
import xc.crabapple.core.device.EnvironmentUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.url.UrlUtil;

/**
 * 2020-11-14 7.0.0
 */
public final class ShellBean extends BeanBase {

    //指令
    //windows下如cmd /c D:/app/mysql/bin/mysqldump -h127.0.0.1 -P3306 -uroot -pyour-password --opt yourdb --routines -F -l > D:/data/db/yourdb.sql
    //centos下如/usr/bin/mysqldump -h127.0.0.1 -P3306 -uroot -pyour-password --opt yourdb --routines -F -l > /data/db/yourdb.sql
    private String cmd = "";

    //若指定了导出的文件，则cmd追加--routines -F -l > D:/data/db/yourdb.sql
    private String targetUrl;

    //是否阻塞，若为非阻塞式则方法返回值为空字符串，yes-阻塞，no-非阻塞
    private String block = "no";

    public String getCmd() {
        String prefix = "";
        if(EnvironmentUtil.isWindows()){
            prefix = "cmd /c ";
        }else if(EnvironmentUtil.isLinux()){
            prefix = "";
        }
        cmd = prefix + cmd;
        targetUrl = UrlUtil.getStandardPath(targetUrl);
        if(StringUtil.isBlank(targetUrl)){
            targetUrl = FileUtil.getTempFile();
        }
        cmd += (" > " + targetUrl);
        return cmd;
    }

    public void setCmd(final String cmd) {
        this.cmd = cmd;
    }

    public String getTargetUrl() {
        targetUrl = UrlUtil.getStandardPath(targetUrl);
        return targetUrl;
    }

    public void setTargetUrl(final String targetUrl) {
        String localTargetUrl = UrlUtil.getStandardPath(targetUrl);
        this.targetUrl = localTargetUrl;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(final String block) {
        this.block = block;
    }

}
