package xc.crabapple.core.servlet;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogConfigBean;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.terminal.TerminalUtil;
import xc.crabapple.core.thread.ThreadUtil;
import xc.crabapple.core.timer.TimerUtil;

/**
 * 2019-11-06 1.0.0
 * 基于sle4j开发
 *
 * log4j日志级别，从高到低
 * OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL
 * logger.debug等方法如果封装在jar包中被主程序调用，主程序无法输出日志，故不能封装在其他项目
 *
 * DEBUG
 * SQL显示
 * 每个接口的返回值(通知器中实现)
 *
 * INFO
 * 接口调用详细信息(START-END)
 *
 * WARN
 * 业务拦截,如登录超时,参数不合法等等
 *
 * ERROR
 * 程序异常
 */
public final class ServletLogUtil {

    // 2021-04-15 7.5.0
    public static final Map<String, String> dbMap = new ConcurrentHashMap<>(2048);;
    // 2021-04-15 7.5.0
    public static final Map<String, Long> takeuptimeMap = new ConcurrentHashMap<>(2048);;

    /**
     * 2019-11-06 1.0.0
     * 日志初始化
     */
    public static final void init(final String level){
        String localLevel = level;
        if (CollectionUtil.isBlank(localLevel)) {
            localLevel = LogUtil.WARN;
        }
        LogConfigBean logConfigBean = new LogConfigBean();
        logConfigBean.setLevel(localLevel);
        LogUtil.init(logConfigBean);
    }

    /**
     * 2019-11-06 1.0.0
     * 此方法仅针对真正网络请求时执行有效
     */
    public static final void start(final String level, final HttpServletRequest request) {
        boolean flag = LogUtil.isEnable(level);
        if (!flag) {
            return;
        }
        String uri = ServletUtil.getRequestURI(request);
        if (StringUtil.isBlank(uri)) {
            return;
        }
        //
        String result = p_info(level, 0, request);
        if(StringUtil.isBlank(result)){
            return;
        }
        result = "START >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n" + result;
        if(LogUtil.DEBUG.equals(level)){
            LogUtil.debug(result);
        }else if(LogUtil.INFO.equals(level)){
            LogUtil.info(result);
        }else if(LogUtil.WARN.equals(level)){
            LogUtil.warn(result);
        }else if(LogUtil.ERROR.equals(level)){
            LogUtil.error(result);
        }
    }

    /**
     * 2019-11-06 1.0.0
     * 此方法仅针对真正网络请求时执行有效
     * @param level
     */
    public static final void end(final String level, final HttpServletRequest request) {
        boolean flag = LogUtil.isEnable(level);
        if (!flag) {
            return;
        }
        String uri = ServletUtil.getRequestURI(request);
        if (StringUtil.isBlank(uri)) {
            return;
        }
        long end = System.currentTimeMillis();
        final String takeuptimeKey = getTakeuptimeKey(request);
        Long start = takeuptimeMap.get(takeuptimeKey);
        takeuptimeMap.remove(takeuptimeKey);
        if(start == null){
            start = TimerUtil.getStamp();
        }
        start = start.longValue();
        long consume = end - start;
        String result = p_info(level, 1, request);
        result = "END\n" + result + "\n    " + consume + "ms";
        if(LogUtil.DEBUG.equals(level)){
            LogUtil.debug(result);
        }else if(LogUtil.INFO.equals(level)){
            LogUtil.info(result);
        }else if(LogUtil.WARN.equals(level)){
            LogUtil.warn(result);
        }else if(LogUtil.ERROR.equals(level)){
            LogUtil.error(result);
        }
    }

    /**
     * @param level
     * @param startORend 0-start，1-end
     * @param request
     * @return
     */
    private static final String p_info(final String level, final int startORend, final HttpServletRequest request){
        boolean flag = LogUtil.isEnable(level);
        if(!flag){
            return "";
        }
        if(request == null){
            return "";
        }
        String uri = ServletUtil.getRequestURI(request);
        if (StringUtil.isBlank(uri)) {
            return "";
        }
        final String threadName = ThreadUtil.getThreadName();
        final String requestKey = ServletLogUtil.getRequestKey(request);
        String snow = TimerUtil.now();
        String result = "    " + threadName + "__" + requestKey + "__" + snow;
        if(startORend == 0){
            String clientInfo = TerminalUtil.getClientInfo(request);
            result += "\n    " + clientInfo;
        }
        result += "\n    " + uri;
        if(startORend == 0){
            StringBuilder pv = new StringBuilder();
            Map<String, Object> map = ServletUtil.getParamMap(request, "form");
            Set set = map.keySet();
            Iterator iterator = set.iterator();
            int i = 0;
            while (iterator.hasNext()) {
                Object key = iterator.next();
                Object value = map.get(key);
                if(value == null){
                    value = "null";
                }
                String svalue = value.toString();
                if(i == 0){
                    pv.append(key).append(":").append(svalue);
                }else{
                    pv.append("\n    ").append(key).append(":").append(svalue);
                }
                i++;
            }
            String pvs = pv.toString();
            if(StringUtil.isBlank(pvs)){
                pvs = "no params";
            }
            result += "\n    " + pvs;
        }
        return result;
    }

    public static final String getRequestKey(final HttpServletRequest request){
        final String addr = ServletUtil.getRequestAddr(request);
        return addr;
    }

    public static final String getTakeuptimeKey(final HttpServletRequest request){
        final String uri = ServletUtil.getRequestURI(request);
        final String addr = ServletUtil.getRequestAddr(request);
        final String rs = "takeuptime#" + addr + "#" + uri;
        return rs;
    }

    public static final String getDbconnectKey(final HttpServletRequest request){
        final String uri = ServletUtil.getRequestURI(request);
        final String addr = ServletUtil.getRequestAddr(request);
        final String rs = "dbConnect#" + addr + "#" + uri;
        return rs;
    }

}