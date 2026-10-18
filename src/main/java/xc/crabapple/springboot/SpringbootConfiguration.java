package xc.crabapple.springboot;

import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import xc.crabapple.core.log.LogUtil;

/**
 * 2025-02-10
 */
public class SpringbootConfiguration implements ApplicationContextAware, ApplicationListener<WebServerInitializedEvent> {

    public SpringbootConfiguration(){

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {

    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent webServerInitializedEvent) {
        try{
            start(webServerInitializedEvent);
        }catch (Throwable throwable){
            LogUtil.error("SpringbootConfiguration_onApplicationEvent_t1", throwable);
        }
    }

    /**
     * 2025-02-10
     * 由业务系统实现
     */
    public void start(WebServerInitializedEvent webServerInitializedEvent){

    }

}