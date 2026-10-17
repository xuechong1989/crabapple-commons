package xc.crabapple.core.log;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.device.EnvironmentUtil;
import xc.crabapple.core.timer.TimerUtil;

import java.util.Base64;

/**
 * 日志库
 * 2019-05-09 1.0.0
 * 2025-06-27 添加byte[]参数自动转标准base64字符串后输出
 * log4j日志级别（共8种），从高到低
 * OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL
 */
public final class LogUtil {

    // 不可配置，创建日志对象的标准格式
    public static Logger p_logger = null;
    private static ObjectMapper p_objectMapper = null;
    // 可配置
    private static LogConfigBean p_logConfigBean = null;
    // logback日志等级
    public static final String DEBUG = "debug";
    public static final String INFO = "info";
    public static final String WARN = "warn";
    public static final String ERROR = "error";
    public static String currentLevel = DEBUG;  // 当前等级

    /**
     * 2022-08-08
     * 执行完该方法后，日志框架在本地java程序中也生效。
     */
    public static final void init(){
        p_logConfigBean = new LogConfigBean();
        p_logConfigBean.setType("logback");
        p_logConfigBean.setLevel(currentLevel);
        //
        init(p_logConfigBean);
    }

    /**
     * 2025-01-07
     */
    public static final void init(final String level){
        p_logConfigBean = new LogConfigBean();
        p_logConfigBean.setType("logback");
        p_logConfigBean.setLevel(level);
        //
        init(p_logConfigBean);
    }

    /**
     * 2025-01-07
     */
    public static final void init(final String type, final String level){
        p_logConfigBean = new LogConfigBean();
        p_logConfigBean.setType(type);
        p_logConfigBean.setLevel(level);
        //
        init(p_logConfigBean);
    }

    /**
     * 2017-05-09 1.0.0
     * 执行完该方法后，日志框架在本地java程序中也生效。
     */
    public static final void init(final LogConfigBean logConfigBean){
        LogConfigBean localLogConfigBean = logConfigBean;
        if(localLogConfigBean == null){
            localLogConfigBean = new LogConfigBean();
        }
        p_logConfigBean = localLogConfigBean;
        p_setUniBean(p_logConfigBean);
        LogConfig.init(p_logConfigBean);
        //
        p_logger = LoggerFactory.getLogger(LogUtil.class);
        //
        final String type = p_logConfigBean.getType();
        currentLevel = p_logConfigBean.getLevel();
        final String path = p_logConfigBean.getPath();
        print("LogUtil_init_1", "日志框架启动成功", type, currentLevel, path);
    }

    /**
     * 2019-07-12 1.0.1
     * 标准打印
     * 标准打印，无论日志级别是哪种，均可输出该语句
     * @param location
     * @param args
     */
    public static final void print(final String location, final Object... args){
        if(args == null || args.length == 0){
            String snow = TimerUtil.now(TimerUtil.FORMATTYPE_M1);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(location).append(", ").append(snow);
            String msg = stringBuilder.toString();
            System.out.println(msg);
            return;
        }
        //
        String snow = TimerUtil.now(TimerUtil.FORMATTYPE_M1);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(location).append(", ").append(snow);
        for(Object arg : args){
            if(arg == null){
                stringBuilder.append(", ").append("{该字段为空对象}");
                continue;
            }
            if(StringUtil.isBlank(arg.toString())){
                stringBuilder.append(", ").append("{该字段为空字符串}");
                continue;
            }
            if(arg instanceof byte[]){
                String base64Str = p_encodeBytes2str((byte[])arg);
                stringBuilder.append(", ").append(base64Str);
                continue;
            }
            String str = p_obj2str(arg);
            stringBuilder.append(", ").append(str);
        }
        String msg = stringBuilder.toString();
        System.out.println(msg);
    }

    /**
     * 2023-08-24
     * 标准打印
     * 标准打印，无论日志级别是哪种，均可输出该语句
     * @param location
     * @param args
     */
    public static final void println(final String location, final Object... args){
        if(args == null || args.length == 0){
            String snow = TimerUtil.now(TimerUtil.FORMATTYPE_M1);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(location).append(", ").append(snow);
            String msg = stringBuilder.toString();
            System.out.println(msg);
            return;
        }
        //
        String snow = TimerUtil.now(TimerUtil.FORMATTYPE_M1);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(location).append(", ").append(snow);
        for(Object arg : args){
            if(arg == null){
                stringBuilder.append("\n").append("{该字段为空对象}");
                continue;
            }
            if(StringUtil.isBlank(arg.toString())){
                stringBuilder.append("\n").append("{该字段为空字符串}");
                continue;
            }
            if(arg instanceof byte[]){
                String base64Str = p_encodeBytes2str((byte[])arg);
                stringBuilder.append("\n").append(base64Str);
                continue;
            }
            String str = p_obj2str(arg);
            stringBuilder.append("\n").append(str);
        }
        stringBuilder.append("\n");
        String msg = stringBuilder.toString();
        System.out.println(msg);
    }

    /**
     * 2019-05-09 1.0.0
     * 日志等级体系
     * @param location
     * @param args
     */
    public static final void debug(final String location, final Object... args){
        boolean enable = isEnable(DEBUG);
        if(!enable){
            return;
        }
        if(args == null || args.length <= 0){
            String snow = TimerUtil.now(TimerUtil.FORMATTYPE_M1);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(location).append(", ").append(snow).append("\n");
            String msg = stringBuilder.toString();
            p_debug(msg);
            return;
        }
        String snow = TimerUtil.now(TimerUtil.FORMATTYPE_M1);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(location).append(", ").append(snow);
        for(Object arg : args){
            if(arg == null){
                stringBuilder.append("\n").append("{该字段为空对象}");
                continue;
            }
            if(StringUtil.isBlank(arg.toString())){
                stringBuilder.append("\n").append("{该字段为空字符串}");
                continue;
            }
            if(arg instanceof byte[]){
                String base64Str = p_encodeBytes2str((byte[])arg);
                stringBuilder.append("\n").append(base64Str);
                continue;
            }
            String str = p_obj2str(arg);
            stringBuilder.append("\n").append(str);
        }
        stringBuilder.append("\n");
        String msg = stringBuilder.toString();
        p_debug(msg);
    }

    /**
     * 2019-05-09 1.0.0
     * 日志等级体系
     * @param location
     * @param args
     */
    public static final void info(final String location, final Object... args){
        boolean enable = isEnable(INFO);
        if(!enable){
            return;
        }
        if(args == null || args.length <= 0){
            String snow = TimerUtil.now(TimerUtil.FORMATTYPE_M1);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(location).append(", ").append(snow).append("\n");
            String msg = stringBuilder.toString();
            p_info(msg);
            return;
        }
        String snow = TimerUtil.now(TimerUtil.FORMATTYPE_M1);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(location).append(", ").append(snow);
        for(Object arg : args){
            if(arg == null){
                stringBuilder.append("\n").append("{该字段为空对象}");
                continue;
            }
            if(StringUtil.isBlank(arg.toString())){
                stringBuilder.append("\n").append("{该字段为空字符串}");
                continue;
            }
            if(arg instanceof byte[]){
                String base64Str = p_encodeBytes2str((byte[])arg);
                stringBuilder.append("\n").append(base64Str);
                continue;
            }
            String str = p_obj2str(arg);
            stringBuilder.append("\n").append(str);
        }
        stringBuilder.append("\n");
        String msg = stringBuilder.toString();
        p_info(msg);
    }

    /**
     * 2019-05-09 1.0.0
     * 日志等级体系
     * @param location
     * @param args
     */
    public static final void warn(final String location, final Object... args){
        boolean enable = isEnable(WARN);
        if(!enable){
            return;
        }
        if(args == null || args.length <= 0){
            String snow = TimerUtil.now(TimerUtil.FORMATTYPE_M1);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(location).append(", ").append(snow).append("\n");
            String msg = stringBuilder.toString();
            p_warn(msg);
            return;
        }
        String snow = TimerUtil.now(TimerUtil.FORMATTYPE_M1);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(location).append(", ").append(snow);
        for(Object arg : args){
            if(arg == null){
                stringBuilder.append("\n").append("{该字段为空对象}");
                continue;
            }
            if(StringUtil.isBlank(arg.toString())){
                stringBuilder.append("\n").append("{该字段为空字符串}");
                continue;
            }
            if(arg instanceof byte[]){
                String base64Str = p_encodeBytes2str((byte[])arg);
                stringBuilder.append("\n").append(base64Str);
                continue;
            }
            String str = p_obj2str(arg);
            stringBuilder.append("\n").append(str);
        }
        stringBuilder.append("\n");
        String msg = stringBuilder.toString();
        p_warn(msg);
    }

    /**
     * 2019-05-09 1.0.0
     * 日志等级体系
     * @param location
     * @param throwable
     * @param args
     */
    public static final void error(final String location, final Throwable throwable, final Object... args){
        final boolean enable = isEnable(ERROR);
        if(!enable){
            return;
        }
        if(throwable == null){
            return;
        }
        final String throwableStr = throwable.toString();
        if(StringUtil.isBlank(throwableStr)){
            return;
        }
        if(throwableStr.indexOf("ClientAbortException") != -1){
            return;
        }
        if(args == null || args.length <= 0){
            String snow = TimerUtil.now(TimerUtil.FORMATTYPE_M1);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(location).append(", ").append(snow).append("\n");
            String errorMsg = TraceUtil.getStackStr(throwable);
            stringBuilder.append("堆栈信息：\n").append(errorMsg).append("\n");
            String msg = stringBuilder.toString();
            p_error(msg);
            //
            if(EnvironmentUtil.isWindows()){
                throwable.printStackTrace();
            }
            return;
        }
        //
        String snow = TimerUtil.now(TimerUtil.FORMATTYPE_M1);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(location).append(", ").append(snow);
        for(Object arg : args){
            if(arg == null){
                stringBuilder.append("\n").append("{该字段为空对象}");
                continue;
            }
            if(StringUtil.isBlank(arg.toString())){
                stringBuilder.append("\n").append("{该字段为空字符串}");
                continue;
            }
            if(arg instanceof byte[]){
                String base64Str = p_encodeBytes2str((byte[])arg);
                stringBuilder.append("\n").append(base64Str);
                continue;
            }
            String str = p_obj2str(arg);
            stringBuilder.append("\n").append(str);
        }
        stringBuilder.append("\n");
        String errorMsg = TraceUtil.getStackStr(throwable);
        stringBuilder.append("堆栈信息：\n").append(errorMsg).append("\n");
        String msg = stringBuilder.toString();
        p_error(msg);
        //
        if(EnvironmentUtil.isWindows()){
            throwable.printStackTrace();
        }
    }

    /**
     * 2019-05-09 1.0.0
     * 日志等级体系
     * @param location
     * @param args
     */
    public static final void error(final String location, final Object... args){
        p_error(location, true, args);
    }

    /**
     * 2022-08-13
     * 日志等级体系
     * @param location
     * @param args
     */
    public static final void errorNo(final String location, final Object... args){
        p_error(location, false, args);
    }

    /**
     * 2019-05-09 1.0.0
     * @param location
     * @param args
     */
    private static final void p_error(final String location, boolean showStackTrace, final Object... args){
        boolean enable = isEnable(ERROR);
        if(!enable){
            return;
        }
        if(args == null || args.length <= 0){
            String snow = TimerUtil.now(TimerUtil.FORMATTYPE_M1);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(location).append(", ").append(snow).append("\n");
            if(showStackTrace){
                String stackTrace = TraceUtil.getStackStr();
                stringBuilder.append("堆栈信息：\n").append(stackTrace);
            }
            String msg = stringBuilder.toString();
            p_error(msg);
            return;
        }
        String snow = TimerUtil.now(TimerUtil.FORMATTYPE_M1);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(location).append(", ").append(snow);
        for(Object arg : args){
            if(arg == null){
                stringBuilder.append("\n").append("{该字段为空对象}");
                continue;
            }
            if(StringUtil.isBlank(arg.toString())){
                stringBuilder.append("\n").append("{该字段为空字符串}");
                continue;
            }
            if(arg instanceof byte[]){
                String base64Str = p_encodeBytes2str((byte[])arg);
                stringBuilder.append("\n").append(base64Str);
                continue;
            }
            String str = p_obj2str(arg);
            stringBuilder.append("\n").append(str);
        }
        if(showStackTrace){
            String stackTrace = TraceUtil.getStackStr();
            stringBuilder.append("\n堆栈信息：").append("\n").append(stackTrace);
        }
        stringBuilder.append("\n");
        String msg = stringBuilder.toString();
        p_error(msg);
    }

    /**
     * 2018-05-31
     * 此返回值由logback.xml控制(标准值)，debug -> error范围越小，标准值宽于或等于参数值时返回true
     * 示例：
     * 若当前log等级为warn，则：
     * isUpperLevel("debug");  //false
     * isUpperLevel("info");  //false
     * isUpperLevel("warn");  //true
     * isUpperLevel("error");  //true
     */
    public static final boolean isEnable(final String level) {
        String localLevel = StringUtil.trim(level);
        localLevel = StringUtil.toLowerCase(localLevel);
        //
        if (DEBUG.equals(currentLevel)) {
            if (StringUtil.equalsOfOne(localLevel, DEBUG, INFO, WARN, ERROR)) {
                return true;
            }
            return false;
        }
        if (INFO.equals(currentLevel)) {
            if (StringUtil.equalsOfOne(localLevel, INFO, WARN, ERROR)) {
                return true;
            }
            return false;
        }
        if (WARN.equals(currentLevel)) {
            if (StringUtil.equalsOfOne(localLevel, WARN, ERROR)) {
                return true;
            }
            return false;
        }
        if (ERROR.equals(currentLevel)) {
            if (ERROR.equals(localLevel)) {
                return true;
            }
            return false;
        }
        return false;
    }

    private static final void p_setUniBean(final LogConfigBean p_logConfigBean){
        String type = p_logConfigBean.getType();
        String type2 = p_setUniParam(type);
        p_logConfigBean.setType(type2);
        //
        String level = p_logConfigBean.getLevel();
        String level2 = p_setUniParam(level);
        p_logConfigBean.setLevel(level2);
    }

    private static final String p_setUniParam(final String param){
        String localParam = StringUtil.getPureStr(param);
        localParam = StringUtil.trim(localParam);
        localParam = StringUtil.toLowerCase(localParam);
        return localParam;
    }

    /**
     * 2022-10-21
     */
    private static final void p_debug(final String msg){
        if(p_logger == null){
            System.out.println(msg);
            return;
        }
        p_logger.debug(msg);
    }

    /**
     * 2022-10-21
     */
    private static final void p_info(final String msg){
        if(p_logger == null){
            System.out.println(msg);
            return;
        }
        p_logger.info(msg);
    }

    /**
     * 2022-10-21
     */
    private static final void p_warn(final String msg){
        if(p_logger == null){
            System.out.println(msg);
            return;
        }
        p_logger.warn(msg);
    }

    /**
     * 2022-10-21
     */
    private static final void p_error(final String msg){
        if(p_logger == null){
            System.out.println(msg);
            return;
        }
        p_logger.error(msg);
    }

    /**
     * 2024-12-23
     * 日志类内部专用对象转字符串，确保程序不报错，防止递归调用死循环
     * @param objects
     * @return
     */
    private static final String p_obj2str(final Object... objects){
        if(objects == null){
            return "";
        }
        if(objects.length <= 0){
            return "";
        }
        //
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < objects.length; i++){
            Object object = objects[i];
            if(i >= 1){
                stringBuilder.append("\n");
            }
            if(object == null){
                stringBuilder.append("");
            }else if(object instanceof String){
                stringBuilder.append((String) object);
            }else{
                final String json = p_obj2str(object, "no");
                stringBuilder.append(json);
            }
        }
        String str = stringBuilder.toString();
        return str;
    }

    /**
     * 2025-07-03 add
     * @param object
     * @param isRetain
     * @return
     */
    private static final String p_obj2str(Object object, String isRetain) {
        if(object == null){
            return "";
        }
        if(p_isBaseSingleType(object)){
            final String str = object.toString();
            return str;
        }
        if(p_objectMapper == null){
            p_objectMapper = JsonMapper.builder()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();
        }
        try{
            if ("yes".equals(isRetain)) {
                String json = p_objectMapper.writeValueAsString(object);
                if (json == null) {
                    final String str = object.toString();
                    return str;
                }
                return json;
            }
            String json = p_objectMapper.writeValueAsString(object);
            if (json == null) {
                final String str = object.toString();
                return str;
            }
            return json;
        }catch(Throwable throwable){
            final String str = object.toString();
            return str;
        }
    }

    /**
     * 2025-07-03 add
     * @param object
     * @return
     */
    private static boolean p_isBaseSingleType(Object object){
        if(object == null){
            return true;
        }
        if(object instanceof Boolean || object instanceof Byte || object instanceof Character || object instanceof Number || object instanceof String){
            return true;
        }
        //
        return false;
    }

    /**
     * 2025-07-03 add
     * @param sourceBytes
     * @return
     */
    private static String p_encodeBytes2str(final byte[] sourceBytes){
        final Base64.Encoder encoder = Base64.getEncoder();
        final String base64Str = encoder.encodeToString(sourceBytes);
        return base64Str;
    }
    
}