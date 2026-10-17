package xc.crabapple.core.log;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.timer.TimerUtil;
import xc.crabapple.core.url.UrlUtil;

/**
 * 2022-08-05
 * 在IDEA控制台中日志变彩色颜色，在右上角：Edit Configurations ==> VM options 中输入 -Dlog4j.skipJansi=false
 */
public final class Log4j2Config {

    public static final void init(final LogConfigBean logConfigBean){
        final String level = logConfigBean.getLevel();
        String path = logConfigBean.getPath();
        path = UrlUtil.getStandardPath(path);
        //
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version='1.0' encoding='UTF-8'?>\n");
        stringBuilder.append("\n");
        stringBuilder.append("<configuration status='WARN' monitorInterval='30'>\n");
        stringBuilder.append("\n");
        stringBuilder.append("\t<properties>\n");
        stringBuilder.append("    <property name='FORMAT'>%highlight{[%p] %t %-d{yyyy-MM-dd HH:mm:ss.SSS} %n  %l%n  %m%n}{FATAL=red, ERROR=red, WARN=yellow, INFO=blue, DEBUG=green,TRACE=cyan}</property>\n");
        stringBuilder.append("</properties>\n");
        stringBuilder.append("\n");
        stringBuilder.append("<appenders>\n");
        stringBuilder.append("\n");
        stringBuilder.append("    <Console name='Console' target='SYSTEM_OUT'>\n");
        stringBuilder.append("        <PatternLayout pattern='${FORMAT}'/>\n");
        stringBuilder.append("    </Console>\n");
        stringBuilder.append("    \n");
        stringBuilder.append("    <RollingFile name='FILE_DEBUG' filePattern='${log.file.path}/$${date:yyyy-MM-dd}/debug-%d{yyyy-MM-dd}-%i.log'>\n");
        stringBuilder.append("        <!--控制台只输出level及以上级别的信息（onMatch），其他的直接拒绝（onMismatch）-->        \n");
        stringBuilder.append("        <ThresholdFilter level='DEBUG' onMatch='ACCEPT' />\n");
        stringBuilder.append("        <PatternLayout pattern='${FORMAT}' />\n");
        stringBuilder.append("        <Policies>\n");
        stringBuilder.append("            <TimeBasedTriggeringPolicy />\n");
        stringBuilder.append("            <SizeBasedTriggeringPolicy size='100 MB' />\n");
        stringBuilder.append("        </Policies>\n");
        stringBuilder.append("    </RollingFile>\n");
        stringBuilder.append(" \n");
        stringBuilder.append("    <RollingFile name='FILE_INFO' filePattern='${log.file.path}/$${date:yyyy-MM-dd}/info-%d{yyyy-MM-dd}-%i.log'>\n");
        stringBuilder.append("        <!--控制台只输出level及以上级别的信息（onMatch），其他的直接拒绝（onMismatch）-->        \n");
        stringBuilder.append("        <ThresholdFilter level='INFO' onMatch='ACCEPT' />\n");
        stringBuilder.append("        <PatternLayout pattern='${FORMAT}'/>\n");
        stringBuilder.append("        <Policies>\n");
        stringBuilder.append("            <TimeBasedTriggeringPolicy/>\n");
        stringBuilder.append("            <SizeBasedTriggeringPolicy size='100 MB'/>\n");
        stringBuilder.append("        </Policies>\n");
        stringBuilder.append("    </RollingFile>\n");
        stringBuilder.append(" \n");
        stringBuilder.append("    <RollingFile name='FILE_WARN' filePattern='${log.file.path}/$${date:yyyy-MM-dd}/warn-%d{yyyy-MM-dd}-%i.log'>\n");
        stringBuilder.append("       \t<ThresholdFilter level='WARN' onMatch='ACCEPT' />\n");
        stringBuilder.append("        <PatternLayout pattern='${FORMAT}'/>\n");
        stringBuilder.append("        <Policies>\n");
        stringBuilder.append("            <TimeBasedTriggeringPolicy/>\n");
        stringBuilder.append("            <SizeBasedTriggeringPolicy size='100 MB'/>\n");
        stringBuilder.append("        </Policies>\n");
        stringBuilder.append("    </RollingFile>\n");
        stringBuilder.append(" \n");
        stringBuilder.append("    <RollingFile name='FILE_ERROR' filePattern='${log.file.path}/$${date:yyyy-MM-dd}/error-%d{yyyy-MM-dd}-%i.log'>\n");
        stringBuilder.append("        <PatternLayout pattern='${FORMAT}'/>\n");
        stringBuilder.append("        <ThresholdFilter level='ERROR' onMatch='ACCEPT' />\n");
        stringBuilder.append("        <Policies>\n");
        stringBuilder.append("            <TimeBasedTriggeringPolicy/>\n");
        stringBuilder.append("            <SizeBasedTriggeringPolicy size='128 MB'/>\n");
        stringBuilder.append("        </Policies>\n");
        stringBuilder.append("    </RollingFile>\n");
        stringBuilder.append(" \n");
        stringBuilder.append("</appenders>\n");
        stringBuilder.append(" \n");
        stringBuilder.append("<loggers>\n");
        stringBuilder.append("\n");
        stringBuilder.append("    <root level='${log.level}'>\n");
        stringBuilder.append("        <appender-ref ref='Console'/>\n");
        stringBuilder.append("        <appender-ref ref='FILE_DEBUG'/>\n");
        stringBuilder.append("        <appender-ref ref='FILE_INFO'/>\n");
        stringBuilder.append("        <appender-ref ref='FILE_WARN'/>\n");
        stringBuilder.append("        <appender-ref ref='FILE_ERROR'/>\n");
        stringBuilder.append("    </root>\n");
        stringBuilder.append("\n");
        stringBuilder.append("    <logger name='ch' level='ERROR'/>\n");
        stringBuilder.append("    <logger name='com' level='ERROR' />\n");
        stringBuilder.append("    <logger name='cn' level='ERROR' />\n");
        stringBuilder.append("    <logger name='io' level='ERROR'/>\n");
        stringBuilder.append("    <logger name='net' level='ERROR'/>\n");
        stringBuilder.append("    <logger name='org' level='ERROR' />\n");
        stringBuilder.append("    <logger name='_org' level='ERROR' />\n");
        stringBuilder.append("    <logger name='redis' level='ERROR' />\n");
        stringBuilder.append("    <logger name='xc.begonia' level='${log.level}'/>\n");
        stringBuilder.append("    <logger name='xc.crabapple' level='${log.level}'/>\n");
        stringBuilder.append("\n");
        stringBuilder.append("</loggers>\n");
        stringBuilder.append(" \n");
        stringBuilder.append("</configuration>");
        //
        final String template = stringBuilder.toString();
        //
        String url = path + "/log4j2.xml";
        url = UrlUtil.getStandardPath(url);
        if(FileUtil.exists(url)){
            String now = TimerUtil.now(TimerUtil.FORMATTYPE_M3);
            String random = RandomUtil.getStr(8);
            String backup = "log4j2-" + now + "---" + random + ".xml";
            String newUrl = path + "/" + backup;
            FileUtil.rename(url, newUrl);
        }
        String content = StringUtil.replaceAll(template, "${log.level}", level);
        content = StringUtil.replaceAll(content, "${log.file.path}", path);
        FileUtil.fileDelete(url);
        FileUtil.fileWriteNew(url, content, true);
        configLoad(url);
    }

    public static final void configLoad(final String url){
    }


}