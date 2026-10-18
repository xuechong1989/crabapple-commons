package xc.crabapple.ssm.oracle;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.ssm.mybatis.MyBatisUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 2025-03-28
 * 2026-06-02 信创国产化-达梦 添加DB类型检查
 * oracle数据库备份
 */
public class OracleExpdpUtil {

    /**
     * 2026-06-02 信创国产化-达梦
     * 检查当前数据库类型是否为Oracle，非Oracle时拒绝执行并返回false
     */
    private static final boolean p_checkOracleOnly(final String methodName){
        if(!MyBatisUtil.DB_ORACLE.equals(MyBatisUtil.dbType)){
            LogUtil.error("OracleExpdpUtil_" + methodName + "_dm1", "非Oracle数据库，OracleExpdpUtil仅支持Oracle，当前dbType=" + MyBatisUtil.dbType);
            return false;
        }
        return true;
    }

    /**
     * 2025-03-28
     * 原有的导出指令：expdp HNCAA/HNCAA20240618@orcl directory=IMP_DIR dumpfile=HNCAA.dmp logfile=HNCAA.log
     * @param command 现在的导入指令 expdp HNCAA/HNCAA20240618@orcl directory=IMP_DIR dumpfile=HNCAA.dmp logfile=HNCAA.log --fileName=HNCAA--fileToPath=
     *                注意fileName要和前面的file一样
     * @param dbpath oracle配置的数据库地址。如：D:/app/oracle/impdir
     * @return
     */
    public static final boolean executeExpdp(final String command, final String dbpath){
        if(!p_checkOracleOnly("executeExpdp")){
            return false;
        }
//        command = "expdp HNCAA/HNCAA20240618@orcl directory=IMP_DIR dumpfile=HNCAA.dmp logfile=HNCAA.log";
//        command = "expdp HNCAA/HNCAA20240618@orcl directory=IMP_DIR dumpfile=HNCAA.dmp logfile=HNCAA.log --fileName=HNCAA--fileToPath=";
        if(StringUtil.isBlank(command)){
            LogUtil.error("OracleExpdpUtil_executeExpdp_1", "导出指令为空");
            return false;
        }
        Process process = null;
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        String toDmpUrl = null;
        String toLogUrl = null;
        String localCommand = command;
        try {
            String[] fileToPathSplit = StringUtil.split(localCommand, "--fileToPath=");
            localCommand = fileToPathSplit[0];
            String toNowUrl = fileToPathSplit[1];
            String[] fileNameSplit = StringUtil.split(localCommand, "--fileName=");
            localCommand = fileNameSplit[0];
            String fileName = fileNameSplit[1];
            toDmpUrl = dbpath + "/"+fileName + ".dmp";
            toLogUrl = dbpath + "/"+fileName + ".log";
            String toNowDmpUrl = toNowUrl + "/" + fileName+".dmp";
            String toNowLogUrl = toNowUrl + "/" + fileName+".log";
            //查询并删除原有的，如果存在则无法备份成功
            FileUtil.fileDelete(toDmpUrl);
            FileUtil.fileDelete(toLogUrl);
            // 开始备份
            LogUtil.print("OracleExpdpUtil_executeExpdp_2", "备份开始", localCommand);
            // 执行命令
            process = Runtime.getRuntime().exec(localCommand);
            inputStream = process.getErrorStream();
            inputStreamReader = new InputStreamReader(inputStream, "GBK");
            bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine())!=  null) {
                LogUtil.print("OracleExpdpUtil_executeExpdp_3", "[oracledump]", line);
            }
            // 等待命令执行完成
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                FileUtil.copy(toDmpUrl,toNowDmpUrl);
//                FileUtil.copy(toLogUrl,toNowLogUrl); //压缩时，是否包含。log后缀文件
                FileUtil.fileDelete(toDmpUrl);
                FileUtil.fileDelete(toLogUrl);
                LogUtil.print("OracleExpdpUtil_executeExpdp_4", "备份成功", localCommand, exitCode);
                return true;
            }
            FileUtil.fileDelete(toDmpUrl);
            FileUtil.fileDelete(toLogUrl);
            return false;
        } catch (Throwable throwable) {
            FileUtil.fileDelete(toDmpUrl);
            FileUtil.fileDelete(toLogUrl);
            LogUtil.error("OracleExpdpUtil_executeExpdp_t1", throwable, localCommand);
            return false;
        }finally {
            IOUtil.close(bufferedReader, inputStreamReader, inputStream);
            if(process != null){
                process.destroy();
            }
        }
    }

}
