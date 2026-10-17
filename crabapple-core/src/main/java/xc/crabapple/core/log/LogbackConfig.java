package xc.crabapple.core.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.LoggerFactory;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.timer.TimerUtil;
import xc.crabapple.core.url.UrlUtil;

/**
 * 2022-08-05 版本：JDK-1.8 + slf4j-1.7.36 + logback-1.2.12
 * logback配置
 */
public final class LogbackConfig {

    /**
     * log.type=logback
     * log.level=debug  OK
     * log.file.path=/data/crabapple/logs  OK
     * log.file.maxHistory=90  OK
     * log.file.maxFileSize=512MB  OK
     */
    public static final void init(final LogConfigBean logConfigBean){
        final String level = logConfigBean.getLevel();
        String path = logConfigBean.getPath();
        final Integer maxHistory = logConfigBean.getMaxHistory();
        final String maxFileSize = logConfigBean.getMaxFileSize();
        path = UrlUtil.getStandardPath(path);
        //
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version='1.0' encoding='UTF-8'?>\n");
        stringBuilder.append("<!DOCTYPE configuration>\n");
        stringBuilder.append("\n");
        stringBuilder.append("<configuration scan='true' scanPeriod='60 seconds' debug='false'>\n");
        stringBuilder.append("\n");
        //
        // 2026-08-21 构建替代redis的本地API持久化缓存框架
        // OnConsoleStatusListener会把logback自身状态消息（|-INFO in ch.qos.logback...）全部打到控制台，
        // Tomcat启动刷屏；改为NopStatusListener静默，logback配置问题改为按需开启排查
        stringBuilder.append("    <statusListener class=\"ch.qos.logback.core.status.NopStatusListener\" />\n\n");
        //stringBuilder.append("    <!-- 控制台 -->\n");
        stringBuilder.append("    <appender name='CONSOLE_DEBUG' class='ch.qos.logback.core.ConsoleAppender'>\n");
        stringBuilder.append("        <encoder>\n");
        stringBuilder.append("            <charset>UTF-8</charset>\n");
        stringBuilder.append("            <pattern>%boldWhite(%-5level #) %boldWhite([%date{yyyy-MM-dd HH:mm:ss.SSS}] #) %boldWhite(%thread #) %boldWhite(%logger) %boldWhite(%n%msg%n)</pattern>\n");
        stringBuilder.append("        </encoder>\n");
        stringBuilder.append("        <filter class='ch.qos.logback.classic.filter.LevelFilter'>\n");
        stringBuilder.append("            <level>DEBUG</level>\n");
        stringBuilder.append("            <onMatch>ACCEPT</onMatch>\n");
        stringBuilder.append("            <onMismatch>DENY</onMismatch>\n");
        stringBuilder.append("        </filter>\n");
        stringBuilder.append("    </appender>\n");
        stringBuilder.append("    <appender name='CONSOLE_INFO' class='ch.qos.logback.core.ConsoleAppender'>\n");
        stringBuilder.append("        <encoder>\n");
        stringBuilder.append("            <charset>UTF-8</charset>\n");
        stringBuilder.append("            <pattern>%boldGreen(%-5level #) %boldGreen([%date{yyyy-MM-dd HH:mm:ss.SSS}] #) %boldGreen(%thread #) %boldGreen(%logger) %boldGreen(%n%msg%n)</pattern>\n");
        stringBuilder.append("        </encoder>\n");
        stringBuilder.append("        <filter class='ch.qos.logback.classic.filter.LevelFilter'>\n");
        stringBuilder.append("            <level>INFO</level>\n");
        stringBuilder.append("            <onMatch>ACCEPT</onMatch>\n");
        stringBuilder.append("            <onMismatch>DENY</onMismatch>\n");
        stringBuilder.append("        </filter>\n");
        stringBuilder.append("    </appender>\n");
        stringBuilder.append("    <appender name='CONSOLE_WARN' class='ch.qos.logback.core.ConsoleAppender'>\n");
        stringBuilder.append("        <encoder>\n");
        stringBuilder.append("            <charset>UTF-8</charset>\n");
        stringBuilder.append("            <pattern>%boldYellow(%-5level #) %boldYellow([%date{yyyy-MM-dd HH:mm:ss.SSS}] #) %boldYellow(%thread #) %boldYellow(%logger) %boldYellow(%n%msg%n)</pattern>\n");
        stringBuilder.append("        </encoder>\n");
        stringBuilder.append("        <filter class='ch.qos.logback.classic.filter.LevelFilter'>\n");
        stringBuilder.append("            <level>WARN</level>\n");
        stringBuilder.append("            <onMatch>ACCEPT</onMatch>\n");
        stringBuilder.append("            <onMismatch>DENY</onMismatch>\n");
        stringBuilder.append("        </filter>\n");
        stringBuilder.append("    </appender>\n");
        stringBuilder.append("    <appender name='CONSOLE_ERROR' class='ch.qos.logback.core.ConsoleAppender'>\n");
        stringBuilder.append("        <encoder>\n");
        stringBuilder.append("            <charset>UTF-8</charset>\n");
        stringBuilder.append("            <pattern>%boldRed(%-5level #) %boldRed([%date{yyyy-MM-dd HH:mm:ss.SSS}] #) %boldRed(%thread #) %boldRed(%logger) %boldRed(%n%msg%n)</pattern>\n");
        stringBuilder.append("        </encoder>\n");
        stringBuilder.append("        <filter class='ch.qos.logback.classic.filter.LevelFilter'>\n");
        stringBuilder.append("            <level>ERROR</level>\n");
        stringBuilder.append("            <onMatch>ACCEPT</onMatch>\n");
        stringBuilder.append("            <onMismatch>DENY</onMismatch>\n");
        stringBuilder.append("        </filter>\n");
        stringBuilder.append("    </appender>\n");
        stringBuilder.append("\n");
        //
        if(true){
            //stringBuilder.append("    <!-- 文件 -->\n");
            stringBuilder.append("    <appender name='FILE_DEBUG' class='ch.qos.logback.core.rolling.RollingFileAppender'>\n");
            stringBuilder.append("        <encoder>\n");
            stringBuilder.append("            <charset>UTF-8</charset>\n");
            stringBuilder.append("            <pattern>%-5level %d{yyyy-MM-dd HH:mm:ss.SSS} %logger %n%msg%n%n</pattern>\n");
            stringBuilder.append("        </encoder>\n");
            stringBuilder.append("        <filter class='ch.qos.logback.classic.filter.LevelFilter'>\n");
            stringBuilder.append("            <level>DEBUG</level>\n");
            stringBuilder.append("            <onMatch>ACCEPT</onMatch>\n");
            stringBuilder.append("            <onMismatch>DENY</onMismatch>\n");
            stringBuilder.append("        </filter>\n");
            stringBuilder.append("        <rollingPolicy class='ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy'>\n");
            stringBuilder.append("            <fileNamePattern>${log.file.path}/%d{yyyy-MM-dd}/1-debug#%d{yyyy-MM-dd}#%i.log</fileNamePattern>\n");
            stringBuilder.append("            <maxHistory>${log.file.maxHistory}</maxHistory>\n");
            stringBuilder.append("            <maxFileSize>${log.file.maxFileSize}</maxFileSize>\n");
            stringBuilder.append("            <totalSizeCap>1024GB</totalSizeCap>\n");
            stringBuilder.append("        </rollingPolicy>\n");
            stringBuilder.append("    </appender>\n");
            stringBuilder.append("    <appender name='FILE_INFO' class='ch.qos.logback.core.rolling.RollingFileAppender'>\n");
            stringBuilder.append("        <encoder>\n");
            stringBuilder.append("            <charset>UTF-8</charset>\n");
            stringBuilder.append("            <pattern>%-5level %d{yyyy-MM-dd HH:mm:ss.SSS} %logger %n%msg%n%n</pattern>\n");
            stringBuilder.append("        </encoder>\n");
            stringBuilder.append("        <filter class='ch.qos.logback.classic.filter.LevelFilter'>\n");
            stringBuilder.append("            <level>INFO</level>\n");
            stringBuilder.append("            <onMatch>ACCEPT</onMatch>\n");
            stringBuilder.append("            <onMismatch>DENY</onMismatch>\n");
            stringBuilder.append("        </filter>\n");
            stringBuilder.append("        <rollingPolicy class='ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy'>\n");
            stringBuilder.append("            <fileNamePattern>${log.file.path}/%d{yyyy-MM-dd}/2-info#%d{yyyy-MM-dd}#%i.log</fileNamePattern>\n");
            stringBuilder.append("            <maxHistory>${log.file.maxHistory}</maxHistory>\n");
            stringBuilder.append("            <maxFileSize>${log.file.maxFileSize}</maxFileSize>\n");
            stringBuilder.append("            <totalSizeCap>1024GB</totalSizeCap>\n");
            stringBuilder.append("        </rollingPolicy>\n");
            stringBuilder.append("    </appender>\n");
            stringBuilder.append("    <appender name='FILE_WARN' class='ch.qos.logback.core.rolling.RollingFileAppender'>\n");
            stringBuilder.append("        <encoder>\n");
            stringBuilder.append("            <charset>UTF-8</charset>\n");
            stringBuilder.append("            <pattern>%-5level %d{yyyy-MM-dd HH:mm:ss.SSS} %logger %n%msg%n%n</pattern>\n");
            stringBuilder.append("        </encoder>\n");
            stringBuilder.append("        <filter class='ch.qos.logback.classic.filter.LevelFilter'>\n");
            stringBuilder.append("            <level>WARN</level>\n");
            stringBuilder.append("            <onMatch>ACCEPT</onMatch>\n");
            stringBuilder.append("            <onMismatch>DENY</onMismatch>\n");
            stringBuilder.append("        </filter>\n");
            stringBuilder.append("        <rollingPolicy class='ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy'>\n");
            stringBuilder.append("            <fileNamePattern>${log.file.path}/%d{yyyy-MM-dd}/3-warn#%d{yyyy-MM-dd}#%i.log</fileNamePattern>\n");
            stringBuilder.append("            <maxHistory>${log.file.maxHistory}</maxHistory>\n");
            stringBuilder.append("            <maxFileSize>${log.file.maxFileSize}</maxFileSize>\n");
            stringBuilder.append("            <totalSizeCap>1024GB</totalSizeCap>\n");
            stringBuilder.append("        </rollingPolicy>\n");
            stringBuilder.append("    </appender>\n");
            stringBuilder.append("    <appender name='FILE_ERROR' class='ch.qos.logback.core.rolling.RollingFileAppender'>\n");
            stringBuilder.append("        <encoder>\n");
            stringBuilder.append("            <charset>UTF-8</charset>\n");
            stringBuilder.append("            <pattern>%-5level %d{yyyy-MM-dd HH:mm:ss.SSS} %logger %n%msg%n%n</pattern>\n");
            stringBuilder.append("        </encoder>\n");
            stringBuilder.append("        <filter class='ch.qos.logback.classic.filter.LevelFilter'>\n");
            stringBuilder.append("            <level>ERROR</level>\n");
            stringBuilder.append("            <onMatch>ACCEPT</onMatch>\n");
            stringBuilder.append("            <onMismatch>DENY</onMismatch>\n");
            stringBuilder.append("        </filter>\n");
            stringBuilder.append("        <rollingPolicy class='ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy'>\n");
            stringBuilder.append("            <fileNamePattern>${log.file.path}/%d{yyyy-MM-dd}/4-error#%d{yyyy-MM-dd}#%i.log</fileNamePattern>\n");
            stringBuilder.append("            <maxHistory>${log.file.maxHistory}</maxHistory>\n");
            stringBuilder.append("            <maxFileSize>${log.file.maxFileSize}</maxFileSize>\n");
            stringBuilder.append("            <totalSizeCap>1024GB</totalSizeCap>\n");
            stringBuilder.append("        </rollingPolicy>\n");
            stringBuilder.append("    </appender>\n");
            stringBuilder.append("\n");
        }
        //
        //stringBuilder.append("    <!-- root -->\n");
        stringBuilder.append("    <root>\n");
        stringBuilder.append("        <appender-ref ref='CONSOLE_DEBUG'/>\n");
        stringBuilder.append("        <appender-ref ref='CONSOLE_INFO'/>\n");
        stringBuilder.append("        <appender-ref ref='CONSOLE_WARN'/>\n");
        stringBuilder.append("        <appender-ref ref='CONSOLE_ERROR'/>\n");
        stringBuilder.append("        <appender-ref ref='FILE_DEBUG'/>\n");
        stringBuilder.append("        <appender-ref ref='FILE_INFO'/>\n");
        stringBuilder.append("        <appender-ref ref='FILE_WARN'/>\n");
        stringBuilder.append("        <appender-ref ref='FILE_ERROR'/>\n");
        stringBuilder.append("    </root>\n");
        stringBuilder.append("\n");
        //stringBuilder.append("    <!-- logger 只对团队自己写的代码做动态配置管理 -->\n");
        stringBuilder.append("    <logger name='ch' level='ERROR'/>\n");
        stringBuilder.append("    <logger name='com' level='ERROR'/>\n");
        stringBuilder.append("    <logger name='cn' level='ERROR'/>\n");
        stringBuilder.append("    <logger name='freemarker.cache' level='ERROR'/>\n");
        stringBuilder.append("    <logger name='io' level='ERROR'/>\n");
        stringBuilder.append("    <logger name='net' level='ERROR'/>\n");
        stringBuilder.append("    <logger name='org' level='ERROR' />\n");
        stringBuilder.append("    <logger name='_org' level='ERROR' />\n");
        stringBuilder.append("    <logger name='redis' level='ERROR'/>\n");
        stringBuilder.append("    <logger name='xc.crabapple' level='${log.level}'/>\n");
        stringBuilder.append("\n");
        stringBuilder.append("</configuration>");
        //
        final String template = stringBuilder.toString();
        //
        String url = path + "/logback.xml";
        url = UrlUtil.getStandardPath(url);
        if(FileUtil.exists(url)){
            String now = TimerUtil.now(TimerUtil.FORMATTYPE_M3);
            String random = RandomUtil.getStr(8);
            String backup = "logback-" + now + "---" + random + ".xml";
            String newUrl = path + "/" + backup;
            FileUtil.rename(url, newUrl);
        }
        String content = StringUtil.replaceAll(template, "${log.level}", level);
        content = StringUtil.replaceAll(content, "${log.file.path}", path);
        content = StringUtil.replaceAll(content, "${log.file.maxHistory}", maxHistory.toString());
        content = StringUtil.replaceAll(content, "${log.file.maxFileSize}", maxFileSize);
        FileUtil.fileDelete(url);
        FileUtil.fileWriteNew(url, content, true);
        configLoad(url);
    }

    public static final void configLoad(String url){
        url = UrlUtil.getStandardPath(url);
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        loggerContext.reset();
        try{
            configurator.doConfigure(url);
            StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
        }catch(Throwable throwable){
            System.out.println("url=" + url);
            throwable.printStackTrace();
        }
    }

}