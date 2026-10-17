package xc.crabapple.core.log;

/**
 * 2022-08-05
 */
public final class LogConfig {

    public static final void init(final LogConfigBean logConfigBean){
        String type = logConfigBean.getType();
        if("logback".equals(type)){
            LogbackConfig.init(logConfigBean);
            return;
        }
        if("log4j2".equals(type)){
            Log4j2Config.init(logConfigBean);
            return;
        }
        //
        System.out.println("crabapple日志框架启用失败。原因：type值错误，type="+type);
        System.out.println(logConfigBean);
    }

}
