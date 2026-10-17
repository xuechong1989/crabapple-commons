package xc.crabapple.ssm.mybatis.log;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.terminal.TerminalUtil;
import xc.crabapple.core.thread.ThreadUtil;
import xc.crabapple.core.timer.TimerUtil;

/**
 * 2023-05-21
 * 记录SQL日志
 */
public final class SqlLogUtil {

    private static long log_custom_sql_threshold = 0;
    private static String log_custom_sql_url = "";

    /**
     * 2023-05-21
     * @param threshold
     * @param sqlUrl
     */
    public static final void init(final long threshold, final String sqlUrl){
        log_custom_sql_threshold = threshold;
        log_custom_sql_url = sqlUrl;
    }

    /**
     * 2023-05-21
     * @param params
     * @param methodName
     * @param standardSQL
     * @param index
     * @param consume
     */
    public static final void showSql(final Map<String, Object> params, final String methodName, final String standardSQL, final int index, final long consume) {
        if (false) {
            return;
        }
        if (!LogUtil.isEnable(LogUtil.INFO)) {
            return;
        }
        // sql语句
        if (CollectionUtil.isBlank(params)) {
            return;
        }
        // 业务层面拦截 START
        if(StringUtil.lastsWith(methodName, "SysPropertiesMapper.selectSysProperties")){
            Object value = params.get("alias");
            if(value != null){
                String valueStr = value.toString();
                if(StringUtil.equalsOfOne(valueStr, "limiting.white", "limiting.rule")){
                    return;
                }
            }
        }
        if(StringUtil.lastsWith(methodName, "SysPropertiesMapper.getPropertiesByName")){
            return;
        }
        // 业务层面拦截 END
        String localStandardSQL = standardSQL;
        if (StringUtil.isBlank(localStandardSQL)) {
            localStandardSQL = "null";
        }
        //
        final String location = "SqlLogUtil_showSql_1";
        //
        final StringBuffer stringBuffer = new StringBuffer();
        final String clientInfo = TerminalUtil.getClientInfo();
        final String threadName = ThreadUtil.getThreadName();
        if(!StringUtil.isBlank(clientInfo)){
            stringBuffer.append(clientInfo).append("\n");
        }
        stringBuffer.append(threadName);
        stringBuffer.append("\nSQL ------------> ").append(index).append(" # time-consuming:").append(consume).append("ms");
        stringBuffer.append("\n").append(methodName);
        stringBuffer.append("\n").append(localStandardSQL);
        Set<String> set = params.keySet();
        Iterator<String> iterator = set.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object object = params.get(key);
            if (object == null) {
                stringBuffer.append("\n").append(key).append("=EMPTY_OBJECT");
                continue;
            }
            stringBuffer.append("\n").append(key).append("=").append(object.toString());
        }
        String str = stringBuffer.toString();
        LogUtil.info(location, str);
        //
        if (consume >= log_custom_sql_threshold) {
            String snow = TimerUtil.now();
            str = (location + ", " + snow + "\n" + str + "\n\n");
            FileUtil.fileWriteAppend(log_custom_sql_url, str, false);
        }
    }

}
