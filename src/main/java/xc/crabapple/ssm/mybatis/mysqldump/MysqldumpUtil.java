package xc.crabapple.ssm.mybatis.mysqldump;

import xc.crabapple.core.callback.CrabappleCallback;
import xc.crabapple.core.collection.ListUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.thread.Business;
import xc.crabapple.core.thread.ThreadUtil;
import xc.crabapple.core.timer.TimerUtil;
import xc.crabapple.core.url.UrlUtil;
import xc.crabapple.ssm.mybatis.MyBatisUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2024-09-25
 * 2026-06-02 信创国产化-达梦 添加DB类型检查，防止DM/Kingbase下误用MySQL专有操作
 * 处理mysql数据库中mysqldump指令导出的sql文件
 */
public final class MysqldumpUtil {

    /**
     * 2026-06-02 信创国产化-达梦
     * 检查当前数据库类型是否为MySQL，非MySQL时拒绝执行并返回false
     * @param methodName 调用方法名
     * @return true-可继续执行 false-已拒绝
     */
    private static final boolean p_checkMysqlOnly(final String methodName){
        if(!MyBatisUtil.DB_MYSQL.equals(MyBatisUtil.dbType)){
            LogUtil.error("MysqldumpUtil_" + methodName + "_dm1", "非MySQL数据库，MysqldumpUtil仅支持MySQL，当前dbType=" + MyBatisUtil.dbType);
            return false;
        }
        return true;
    }

    /**
     * 2025-02-12
     * 执行包含有mysqldump语句的bat文件
     * @param batUrl
     */
    public static final void executeDumpBat(final String batUrl){
        if(!p_checkMysqlOnly("executeDumpBat")){
            return;
        }
        File file = new File(batUrl);
        if(!file.exists()){
            LogUtil.error("MysqldumpUtil_executeDumpBat_1", "batUrl不存在", batUrl);
            return;
        }
        if(!file.isFile()){
            LogUtil.error("MysqldumpUtil_executeDumpBat_2", "batUrl不是文件", batUrl);
            return;
        }
        final List<String> list = FileUtil.fileReadList(batUrl);
        for(int i = 0; i < list.size(); i++){
            String line = list.get(i);
            if(StringUtil.isBlank(line)){
                continue;
            }
            line = StringUtil.getPureStr(line);
            line = StringUtil.trim(line);
            if(StringUtil.isBlank(line)){
                continue;
            }
            // 校验sql语句
            for(int j = 0; j < 10; j++){
                line = StringUtil.replaceAll(line, "  ", " ");
            }
            String[] array = StringUtil.split(line, " ");
            String first = array[0];
            first = StringUtil.toLowerCase(first);
            if(StringUtil.startsWith(first, "rem")){
                continue;
            }
            if(!StringUtil.startsWith(first, "mysqldump")){
                if(StringUtil.containOf(first, "/")){
                    array = StringUtil.split(first, "/");
                    String firstRight = array[array.length - 1];
                    if(!StringUtil.equalsOfOne(firstRight, "mysqldump", "mysqldump.exe")){
                        continue;
                    }
                }
            }
            executeDump(line);
        }
    }

    /**
     * 2025-02-12
     * 执行mysqldump语句，注意每个参数都不能包含空格。
     * @param command 格式如：
     *            mysqldump -h 127.0.0.1 -P 3306 -uroot -pyour-password --databases yourdb --result-file=D:/data/db/yourdb.sql
     *            D:/app/mysql/bin/mysqldump -h 127.0.0.1 -P 3306 -uroot -pyour-password --databases yourdb --result-file=D:/data/db/yourdb.sql
     */
    public static final boolean executeDump(final String command){
        if(!p_checkMysqlOnly("executeDump")){
            return false;
        }
        Process process = null;
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        String toSqlUrl = "";
        String localCommand = command;
        try {
            localCommand = StringUtil.getPureStr(localCommand);
            localCommand = StringUtil.trim(localCommand);
            if(StringUtil.isBlank(localCommand)){
                LogUtil.error("MysqldumpUtil_executeDump_1", "sql为空");
                return false;
            }
            // 开始备份
            LogUtil.print("MysqldumpUtil_executeDump_3", "备份开始", localCommand);
            //
            String[] array = StringUtil.split(localCommand, "--result-file=");
            localCommand = array[0];
            localCommand = StringUtil.trim(localCommand);
            toSqlUrl = array[1];
            toSqlUrl = StringUtil.trim(toSqlUrl);
            FileUtil.fileDelete(toSqlUrl);
            File file = new File(toSqlUrl);
            array = StringUtil.split(localCommand, " ");
            List<String> instructionList = ListUtil.array2list(array);
            ProcessBuilder processBuilder = new ProcessBuilder(instructionList);
            Map<String, String> environmentMap = processBuilder.environment();
            environmentMap.put("LANG", "en_US.GBK");
            processBuilder.redirectOutput(file);
            process = processBuilder.start();
            inputStream = process.getErrorStream();
            inputStreamReader = new InputStreamReader(inputStream, "GBK");
            bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine())!=  null) {
                LogUtil.print("MysqldumpUtil_executeDump_4", "[mysqldump]", line);
            }
            // 等待命令执行完成
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                LogUtil.print("MysqldumpUtil_executeDump_5", "备份成功", localCommand, exitCode);
                return true;
            }
            FileUtil.fileDelete(toSqlUrl);
            LogUtil.error("MysqldumpUtil_executeDump_6", "备份失败", localCommand, exitCode);
            return false;
        } catch (Throwable throwable) {
            FileUtil.fileDelete(toSqlUrl);
            LogUtil.error("MysqldumpUtil_executeDump_t1", throwable, localCommand);
            return false;
        }finally {
            IOUtil.close(bufferedReader, inputStreamReader, inputStream);
            if(process != null){
                process.destroy();
            }
        }
    }

    /**
     * 2024-09-28
     * @param urlList
     * @param threadNum
     * @param addr
     * @param dbName
     * @param username
     * @param password
     */
    public static final void importSql_create(final List<String> urlList, final int threadNum, final String addr, final String dbName, final String username, final String password){
        p_importSql(urlList, "create", threadNum, addr, dbName, username, password, null);
    }

    /**
     * 2024-09-28
     * @param urlList
     * @param threadNum
     * @param addr
     * @param dbName
     * @param username
     * @param password
     */
    public static final void importSql_insert(final List<String> urlList, final int threadNum, final String addr, final String dbName, final String username, final String password){
        p_importSql(urlList, "insert", threadNum, addr, dbName, username, password, null);
    }

    /**
     * 2024-09-28
     * @param urlList
     * @param threadNum
     * @param addr
     * @param dbName
     * @param username
     * @param password
     */
    public static final void importSql_createInsert(final List<String> urlList, final int threadNum, final String addr, final String dbName, final String username, final String password){
        p_importSql(urlList, "create", threadNum, addr, dbName, username, password, "all");
    }

    /**
     * 2024-09-28
     * 基于splitSqlfile方法的结果文件执行
     * @param urlList
     * @param handleType
     * @param threadNum
     * @param addr
     * @param dbName
     * @param username
     * @param password
     */
    private static void p_importSql(final List<String> urlList, final String handleType, final int threadNum, final String addr, final String dbName, final String username, final String password, final String scope){
        if(!p_checkMysqlOnly("p_importSql")){
            return;
        }
        final String address = "jdbc:mysql://" + addr + "/" + dbName + "?useUnicode=true&characterEncoding=utf-8&userSSL=false&serverTimezone=Asia/Shanghai";
        List<List<String>> splitList = ListUtil.splitList(urlList, threadNum);
        final Map<String, Long> countMap = new ConcurrentHashMap<>(2);
        countMap.put("completeCount", 0L);
        countMap.put("start", System.currentTimeMillis());
        int localThreadNum = threadNum;
        if(localThreadNum <= 0){
            localThreadNum = 1;
        }
        int splitSize = splitList.size();
        localThreadNum = Math.min(localThreadNum, splitSize);
        final int threadCount = localThreadNum;
        for(int i = 0; i < splitList.size(); i++){
            List<String> list = splitList.get(i);
            ThreadUtil.start(new Business("MysqldumpUtil_importSql_13", false){
                @Override
                public void process() {
                    p_importSql(list, handleType, address, dbName, username, password);
                    synchronized (countMap){
                        long completeCount = countMap.get("completeCount");
                        completeCount++;
                        countMap.put("completeCount", completeCount);
                        if(completeCount >= threadCount){
                            long start = countMap.get("start");
                            long end = System.currentTimeMillis();
                            long consume = end - start;
                            String consumeStr = "耗时" + consume + "ms";
                            LogUtil.println("MysqldumpUtil_importSql_1", "数据执行完成，线程池被已被销毁", address, handleType, threadCount, completeCount, consumeStr, scope);
                            if(!"all".equals(scope) || "insert".equals(scope)){

                            }
                            if("all".equals(scope) && "create".equals(handleType)){
                                p_importSql(urlList, "insert", 16, addr, dbName, username, password, null);
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * 2024-09-27
     * 单线程导入sql文件
     * 基于splitSqlfile方法的结果文件执行
     * @param urlList 包含一系列待导入的sql文件绝对路径的集合
     * @param handleType 操作类型，create-创建，insert-插入
     * @param address 数据库地址，格式如 192.168.1.100:3306/yourdb
     * @param username 要导入数据库的用户名
     * @param password 要导入数据库的密码
     */
    private static void p_importSql(final List<String> urlList, final String handleType, final String address, final String dbName, final String username, final String password){
        Connection connection = null;
        Statement statement = null;
        String sql = "";
        try {
            connection = DriverManager.getConnection(address, username, password);
            statement = connection.createStatement();
            p_statementOpen(statement, dbName);
            //
            for(int i = 0; i < urlList.size(); i++){
                String url = urlList.get(i);
                url = UrlUtil.getStandardPath(url);
                String filename = FileUtil.getFileName(url, false);
                if(!StringUtil.startsWith(filename, handleType)){
                    continue;
                }
                List<String> lineList = FileUtil.fileReadList(url);
                for(int j = 0; j < lineList.size(); j++){
                    String line = lineList.get(j);
                    line = StringUtil.getPureStr(line);
                    line = StringUtil.trim(line);
                    if(line == null || "".equals(line)){
                        continue;
                    }
                    sql = line;
                    try {
                        statement.execute(sql);
                    }catch (Throwable throwable){
                        LogUtil.error("MysqldumpUtil_importSql_t1", throwable, sql);
                        IOUtil.close(statement);
                        statement = connection.createStatement();
                        p_statementOpen(statement, dbName);
                    }
                }
            }
        } catch (Throwable throwable) {
            p_statementClose(statement, dbName);
            LogUtil.error("MysqldumpUtil_importSql_t2", throwable, handleType, sql);
        }finally {
            p_statementClose(statement, dbName);
            IOUtil.close(statement, connection);
        }
    }

    /**
     * 2024-09-28
     * @param statement
     * @param dbName
     */
    private static void p_statementOpen(final Statement statement, final String dbName){
        try {
            statement.execute("SET NAMES utf8mb4;");
            statement.execute("SET GLOBAL innodb_flush_log_at_trx_commit=0;");
            statement.execute("SET GLOBAL innodb_strict_mode=0;");
            statement.execute("SET GLOBAL innodb_buffer_pool_size=17179869184;");
            statement.execute("SET GLOBAL innodb_redo_log_capacity=1073741824;");
            statement.execute("SET unique_checks=0;");
            statement.execute("SET foreign_key_checks=0;");
            statement.execute("SET autocommit=0;");
            statement.execute("START TRANSACTION;");
            statement.execute("USE "+dbName+";");
        }catch (Throwable throwable){
            LogUtil.error("MysqldumpUtil_p_statementOpen_t1", throwable, dbName);
        }
    }

    /**
     * 2024-09-28
     * @param statement
     * @param dbName
     */
    private static void p_statementClose(final Statement statement, final String dbName){
        try {
            statement.execute("COMMIT;");
            statement.execute("SET autocommit=1;");
            statement.execute("SET foreign_key_checks=1;");
            statement.execute("SET unique_checks=1;");
            statement.execute("SET GLOBAL innodb_strict_mode=1;");
            statement.execute("SET GLOBAL innodb_flush_log_at_trx_commit=1;");
        }catch (Throwable throwable){
            LogUtil.error("MysqldumpUtil_p_statementClose_t1", throwable, dbName);
        }
    }

    /**
     * 2024-09-25
     * 基于指定的sql文件，生成标准化sql语句的sql文件
     * @param url
     * @return
     */
    public static final String splitSqlfile(final String dbName, final String url, final String targetPath){
        final Map<String, String> controlMap = new HashMap<>(8);
        controlMap.put("multiZhushi", "yes");
        controlMap.put("name", "000.sql");
        controlMap.put("create", "no");
        final StringBuilder stringBuilder = new StringBuilder();
        //
        FileUtil.fileReadLine(url, new CrabappleCallback(){
            @Override
            public Object execute(final Object object){
                if(object == null){
                    return null;
                }
                String line = object.toString();
                if(StringUtil.isBlank(line)){
                    return null;
                }
                line = StringUtil.trim(line);
                if(StringUtil.isBlank(line)){
                    return null;
                }
                //
                if(StringUtil.startsWith(line, "CREATE DATABASE ")){
                    return null;
                }
                if(StringUtil.startsWith(line, "USE `")){
                    return null;
                }
                if(StringUtil.startsWith(line, "LOCK TABLES ")){
                    return null;
                }
                if(StringUtil.equals(line, "UNLOCK TABLES;")){
                    return null;
                }
                //
                if(StringUtil.startsWith(line, "--")){
                    return null;
                }
                if(StringUtil.equals(line, "/*")){
                    controlMap.put("multiZhushi", "no");
                    return null;
                }
                if(StringUtil.equals(line, "*/")){
                    controlMap.put("multiZhushi", "yes");
                    return null;
                }
                String multiZhushi = controlMap.get("multiZhushi");
                if(StringUtil.equals(multiZhushi, "no")){
                    return null;
                }
                //
                if(StringUtil.startsWith(line, "/*") && StringUtil.lastsWith(line, "*/;")){
                    return null;
                }
                //
                if(StringUtil.startsWith(line, "SET", "set")){
                    return null;
                }
                //
                if(false){
                    // line = StringUtil.replaceAll(line, "", "");  // 数据库名称更改
                    line = StringUtil.replaceAll(line, " utf8mb3_general_ci ", " utf8mb4_unicode_ci ");
                    line = StringUtil.replaceAll(line, " utf8mb4_general_ci ", " utf8mb4_unicode_ci ");
                    line = StringUtil.replaceAll(line, " utf8mb4_bin ", " utf8mb4_unicode_ci ");
                    line = StringUtil.replaceAll(line, "CHARSET=utf8mb3 ", "CHARSET=utf8mb4 ");
                    line = StringUtil.replaceAll(line, " utf8mb3 ", " utf8mb4 ");
                    line = StringUtil.replaceAll(line, " utf8 ", " utf8mb4 ");
                }
                //
                // 开始创建表
                if(StringUtil.startsWith(line , "DROP TABLE IF EXISTS ")){
                    if(true){
                        String name = controlMap.get("name");
                        name = "create_table_".concat(name).concat(".sql");
                        String end = "\n\nCOMMIT;\nSET autocommit=1;\nSET foreign_key_checks=1;\nSET unique_checks=1;\nSET GLOBAL innodb_strict_mode=1;\nSET GLOBAL innodb_flush_log_at_trx_commit=1;\n";
                        p_appendfile(dbName, targetPath, name, end);
                    }
                    //
                    String name = StringUtil.deleteAll(line, "DROP TABLE IF EXISTS `", "`;");
                    name = name.trim();
                    controlMap.put("name", name);
                    name = "create_table_".concat(name).concat(".sql");
                    p_appendfile(dbName, targetPath, name, line.concat("\n"));
                    return null;
                }
                //
                if(StringUtil.startsWith(line, "CREATE TABLE ")){
                    controlMap.put("create", "yes");
                    stringBuilder.append(line);
                    return null;
                }
                if(StringUtil.startsWith(line, ") ENGINE")){
                    controlMap.put("create", "no");
                    String name = controlMap.get("name");
                    name = "create_table_".concat(name).concat(".sql");
                    stringBuilder.append(line);
                    String createSQL = stringBuilder.toString();
                    if(StringUtil.startsWith(name, "create_table_w_")){
                        createSQL = StringUtil.replaceAll(createSQL, ",) ENGINE", ") ENGINE");
                    }
                    p_appendfile(dbName, targetPath, name, createSQL);
                    stringBuilder.delete(0, stringBuilder.length());
                    return null;
                }
                String create = controlMap.get("create");
                if(StringUtil.equals(create, "yes")){
                    if(true){
                        String name = controlMap.get("name");
                        if(StringUtil.startsWith(name, "w_") && !StringUtil.containOf(line, "`ID_`")){
                            line = StringUtil.replaceAll(line, " varchar(20) ", " longtext ");
                            line = StringUtil.replaceAll(line, " varchar(64) ", " longtext ");
                            line = StringUtil.replaceAll(line, " varchar(94) ", " longtext ");
                            line = StringUtil.replaceAll(line, " varchar(100) ", " longtext ");
                            line = StringUtil.replaceAll(line, " varchar(103) ", " longtext ");
                            line = StringUtil.replaceAll(line, " varchar(200) ", " longtext ");
                            line = StringUtil.replaceAll(line, " varchar(400) ",  " longtext ");
                            line = StringUtil.replaceAll(line, " varchar(512) ", " longtext ");
                            line = StringUtil.replaceAll(line, " varchar(1024) ", " longtext ");
                            line = StringUtil.replaceAll(line, " varchar(4000) ", " longtext ");
                            line = StringUtil.replaceAll(line, " text ", " longtext ");
                            if(StringUtil.startsWith(line, "INDEX `") && StringUtil.containOf(line, "USING BTREE")){
                                return null;
                            }
                        }
                    }
                    stringBuilder.append(line);
                    return null;
                }
                //
                if(StringUtil.startsWith(line, "DROP PROCEDURE IF EXISTS ")){
                    if(false){
                        String name = controlMap.get("name");
                        name = "create_procedure_".concat(name).concat(".sql");
                        String end = "\n\nCOMMIT;\nSET autocommit=1;\nSET foreign_key_checks=1;\nSET unique_checks=1;\nSET GLOBAL innodb_strict_mode=1;\nSET GLOBAL innodb_flush_log_at_trx_commit=1;\n";
                        p_appendfile(dbName, targetPath, name, end);
                    }
                    String name = StringUtil.deleteAll(line, "DROP PROCEDURE IF EXISTS `", "`;");
                    name = name.trim();
                    controlMap.put("name", name);
                    name = "create_procedure_".concat(name).concat(".sql");
                    p_appendfile(dbName, targetPath, name, line);
                    return null;
                }
                //
                if(StringUtil.startsWith(line, "INSERT INTO ")){
                    String name = controlMap.get("name");
                    name = "insert_table_".concat(name).concat(".sql");
                    p_appendfile(dbName, targetPath, name, line);
                    return null;
                }
                //
                return null;
            }
        });
        return targetPath;
    }

    private static void p_appendfile(final String dbName, final String targetPath, final String name, final String line){
        final String targetUrl = targetPath.concat("/").concat(name);
        String localLine = line;
        if(!FileUtil.isRealFile(targetUrl)){
            String start = "SET NAMES utf8mb4;\nSET GLOBAL innodb_flush_log_at_trx_commit=0;\nSET GLOBAL innodb_strict_mode=0;\nSET unique_checks=0;\nSET foreign_key_checks=0;\nSET autocommit=0;\nSTART TRANSACTION;\n\nUSE "+dbName+";\n\n\n";
            localLine = start.concat(localLine);
        }
        FileUtil.fileWriteAppend(targetUrl, localLine.concat("\n"), false);
    }

    /**
     * 2024-09-25
     * 基于指定的sql文件，生成标准化sql语句的sql文件
     * @param url
     * @return
     */
    public static final String createStandardSqlfile(final String url){
        final String targetPath = FileUtil.getPathByUrl(url);
        final String filename = FileUtil.getFileName(url, false);
        final String targetName = filename + "_" + TimerUtil.getStamp() + ".sql";
        String tmpTargetUrl = targetPath + "/" + targetName;
        final String targetUrl = UrlUtil.getStandardPath(tmpTargetUrl);
        //
        final Map<String, String> controlMap = new HashMap<>();
        controlMap.put("multiZhushi", "yes");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\nSET NAMES utf8mb4;");
        stringBuilder.append("\nSET unique_checks=0;");
        stringBuilder.append("\nSET foreign_key_checks=0;");
        stringBuilder.append("\nSET autocommit=0;");
        stringBuilder.append("\nSTART TRANSACTION;");
        String preSql = stringBuilder.toString();
        final String result = preSql.concat("\n\n");
        FileUtil.fileWriteAppend(targetUrl, result, true);
        //
        FileUtil.fileReadLine(url, new CrabappleCallback(){
            @Override
            public Object execute(final Object object){
                if(object == null){
                    return null;
                }
                String line = object.toString();
                if(StringUtil.isBlank(line)){
                    return null;
                }
                line = StringUtil.trim(line);
                if(StringUtil.isBlank(line)){
                    return null;
                }
                //
                if(StringUtil.startsWith(line, "CREATE DATABASE ")){
                    return null;
                }
                if(StringUtil.startsWith(line, "USE `")){
                    return null;
                }
                if(StringUtil.startsWith(line, "LOCK TABLES ")){
                    return null;
                }
                if(StringUtil.equals(line, "UNLOCK TABLES;")){
                    return null;
                }
                //
                if(StringUtil.startsWith(line, "--")){
                    return null;
                }
                if(StringUtil.equals(line, "/*")){
                    controlMap.put("multiZhushi", "no");
                    return null;
                }
                if(StringUtil.equals(line, "*/")){
                    controlMap.put("multiZhushi", "yes");
                    return null;
                }
                String multiZhushi = controlMap.get("multiZhushi");
                if(StringUtil.equals(multiZhushi, "no")){
                    return null;
                }
                //
                if(StringUtil.startsWith(line, "/*") && StringUtil.lastsWith(line, "*/;")){
                    return null;
                }
                //
                //
                // line = StringUtil.replaceAll(line, "", "");  // 数据库名称更改
                line = StringUtil.replaceAll(line, " utf8mb3_general_ci ", " utf8mb4_unicode_ci ");
                line = StringUtil.replaceAll(line, " utf8mb4_general_ci ", " utf8mb4_unicode_ci ");
                line = StringUtil.replaceAll(line, " utf8mb4_bin ", " utf8mb4_unicode_ci ");
                line = StringUtil.replaceAll(line, "CHARSET=utf8mb3 ", "CHARSET=utf8mb4 ");
                line = StringUtil.replaceAll(line, " utf8mb3 ", " utf8mb4 ");
                line = StringUtil.replaceAll(line, " utf8 ", " utf8mb4 ");
                //
                line = "\n".concat(line);
                FileUtil.fileWriteAppend(targetUrl, line, true);
                //
                return null;
            }
        });
        //
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("\nCOMMIT;");
        stringBuilder2.append("\nSET autocommit=1;");
        stringBuilder2.append("\nSET foreign_key_checks=1;");
        stringBuilder2.append("\nSET unique_checks=1;");
        String result2 = stringBuilder2.toString();
        FileUtil.fileWriteAppend(targetUrl, result2, true);
        //
        return targetUrl;
    }

    /**
     * 2024-09-25
     * mysqldump命令导出的sql文件进行解析
     * 净化sql语句，标准化字符串处理，合并values语句
     * 存在内存溢出问题
     */
    public static final String mergeValuesSqlFile(final String url){
        final String targetPath = FileUtil.getTempPath();
        final Map<String, String> controlMap = new HashMap<>();
        controlMap.put("multiZhushi", "yes");
        controlMap.put("appendInsert", "");
        controlMap.put("tmpAppendInsert", "");
        final Map<String, Integer> appendInsertCountMap = new HashMap<>();
        appendInsertCountMap.put("count", 0);
        appendInsertCountMap.put("leftLength", 0);
        final List<String> nameList = new ArrayList<>();
        final StringBuilder stringBuilder = new StringBuilder();
        FileUtil.fileReadLine(url, new CrabappleCallback(){
            @Override
            public Object execute(final Object object){
                if(object == null){
                    return null;
                }
                String line = object.toString();
                if(StringUtil.isBlank(line)){
                    return null;
                }
                line = StringUtil.trim(line);
                if(StringUtil.isBlank(line)){
                    return null;
                }
                //
                if(StringUtil.startsWith(line, "CREATE DATABASE ")){
                    return null;
                }
                if(StringUtil.startsWith(line, "USE `")){
                    return null;
                }
                if(StringUtil.startsWith(line, "LOCK TABLES ")){
                    return null;
                }
                if(StringUtil.equals(line, "UNLOCK TABLES;")){
                    return null;
                }
                //
                if(StringUtil.startsWith(line, "--")){
                    return null;
                }
                if(StringUtil.equals(line, "/*")){
                    controlMap.put("multiZhushi", "no");
                    return null;
                }
                if(StringUtil.equals(line, "*/")){
                    controlMap.put("multiZhushi", "yes");
                    return null;
                }
                String multiZhushi = controlMap.get("multiZhushi");
                if(StringUtil.equals(multiZhushi, "no")){
                    return null;
                }
                //
                if(StringUtil.startsWith(line, "/*") && StringUtil.lastsWith(line, "*/;")){
                    return null;
                }
                //
                if(StringUtil.startsWith(line, "SET ")){
                    return null;
                }
                //
                // line = StringUtil.replaceAll(line, "", "");  // 数据库名称更改
                line = StringUtil.replaceAll(line, " utf8mb3_general_ci ", " utf8mb4_unicode_ci ");
                line = StringUtil.replaceAll(line, " utf8mb4_general_ci ", " utf8mb4_unicode_ci ");
                line = StringUtil.replaceAll(line, " utf8mb4_bin ", " utf8mb4_unicode_ci ");
                line = StringUtil.replaceAll(line, "CHARSET=utf8mb3 ", "CHARSET=utf8mb4 ");
                line = StringUtil.replaceAll(line, " utf8mb3 ", " utf8mb4 ");
                line = StringUtil.replaceAll(line, " utf8 ", " utf8mb4 ");
                // 开始创建表
                if(StringUtil.startsWith(line , "DROP TABLE IF EXISTS ")){
                    String name = StringUtil.deleteAll(line, "DROP TABLE IF EXISTS `", "`;");
                    name = name.trim();
                    nameList.add(name);
                    int tableCount = nameList.size();
                    if(tableCount == 1){
                        stringBuilder.append(line).append("\n");
                        controlMap.put("appendInsert", "");
                        appendInsertCountMap.put("count", 0);
                        appendInsertCountMap.put("leftLength", 0);
                        return null;
                    }
                    //
                    String oldName = nameList.get(nameList.size()-1);
                    final String targetUrl = targetPath+"/t_"+oldName+".sql";
                    String result = stringBuilder.toString();
                    p_filewrite(targetUrl, result);
                    stringBuilder.delete(0, stringBuilder.length());
                    stringBuilder.append(line).append("\n");
                    return null;
                }
                // 开始创建存储过程
                if(StringUtil.startsWith(line, "DROP PROCEDURE IF EXISTS ")){
                    return null;
                }
                // 开始插入语句
                if(StringUtil.startsWith(line, "INSERT INTO ")){
                    String appendInsert = controlMap.get("appendInsert");
                    if("".equals(appendInsert)){
                        final String curName = nameList.get(nameList.size()-1);
                        final String tmpline = line.substring(0, line.length() - 1);  // 去掉最后的分号;
                        final String left = "INSERT INTO `".concat(curName).concat("` VALUES ");
                        final Integer leftLength = left.length();
                        appendInsertCountMap.put("leftLength", leftLength);
                        controlMap.put("appendInsert", tmpline);
                        int size = byteSize(tmpline);
                        appendInsertCountMap.put("count", size);
                        return null;
                    }
                    //
                    int leftLength = appendInsertCountMap.get("leftLength");
                    String tmpline = line.substring(leftLength, line.length() - 1);
                    tmpline = ",".concat(tmpline);
                    int appendInsertSize = appendInsertCountMap.get("count");
                    int tmplineSize = byteSize(tmpline);
                    int size = appendInsertSize + tmplineSize;
                    appendInsertCountMap.put("count", size);
                    if(size > 1024*1024*16){  //
                        stringBuilder.append(appendInsert).append(";\n");
                        tmpline = StringUtil.deleteLast(line, ";");
                        controlMap.put("appendInsert", tmpline);  // 重新开始添加当前行
                        int tmplineSize2 = byteSize(tmpline);
                        appendInsertCountMap.put("count", tmplineSize2);
                        return null;
                    }
                    String tmpInsert = appendInsert.concat(tmpline);
                    controlMap.put("appendInsert", tmpInsert);
                    return null;
                }
                //
                String appendInsert = controlMap.get("appendInsert");
                if(!StringUtil.isBlank(appendInsert)){
                    appendInsert = appendInsert.concat(";");
                    stringBuilder.append(appendInsert).append("\n");
                    controlMap.put("appendInsert", "");
                    appendInsertCountMap.put("count", 0);
                    appendInsertCountMap.put("leftLength", 0);
                }
                stringBuilder.append(line).append("\n");
                //
                return null;
            }
        });
        //
        String appendInsert = controlMap.get("appendInsert");
        if(!StringUtil.isBlank(appendInsert)){
            String oldName = nameList.get(nameList.size()-1);
            final String targetUrl = targetPath+"/t_"+oldName+".sql";
            stringBuilder.append(appendInsert).append(";\n");
            String result = stringBuilder.toString();
            p_filewrite(targetUrl, result);
            controlMap.put("appendInsert", "");
            appendInsertCountMap.put("count", 0);
            appendInsertCountMap.put("leftLength", 0);
            stringBuilder.delete(0, stringBuilder.length());
        }
        //
        return targetPath;
    }

    private static final boolean p_filewrite(final String targetUrl, final String result){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\nSET NAMES utf8mb4;");
        stringBuilder.append("\nSET unique_checks=0;");
        stringBuilder.append("\nSET foreign_key_checks=0;");
        stringBuilder.append("\nSET autocommit=0;");
        stringBuilder.append("\nSTART TRANSACTION;");
        String preSql = stringBuilder.toString();
        String localResult = preSql.concat("\n").concat(result);
        FileUtil.fileWriteNew(targetUrl, localResult, true);
        return true;
    }

    public static final int byteSize(final String str){
        if(str == null || "".equals(str)){
            return 0;
        }
        byte[] bytes = str.getBytes();
        int size = bytes.length;
        return size;
    }

}
