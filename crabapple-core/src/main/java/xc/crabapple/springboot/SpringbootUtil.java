package xc.crabapple.springboot;

import jakarta.servlet.ServletContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.properties.PropertiesUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.servlet.ServletUtil;
import xc.crabapple.core.tomcat.TomcatUtil;
import xc.crabapple.core.url.UrlUtil;
import xc.crabapple.ssm.mybatis.pagination.PaginationUtil;
import xc.crabapple.ssm.net.NetUtil;
import xc.crabapple.ssm.spring.ModuleManager;
import xc.crabapple.ssm.spring.SpringUtil;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 2024-07-16
 */
public final class SpringbootUtil {

    private static String p_jarRootPath = "";

    /**
     * 2025-02-10
     * 系统启动入口方法
     * @param applicationClass
     * @param moduleManagerClass
     * @param args
     * @param routerUrl
     * @param sm4key
     */
    public static final void start(final String systemName, final Class applicationClass, final Class moduleManagerClass, final String[] args, final String routerUrl, final String sm4key, final Integer pageIndex, final Integer pageSize){
        try {
            SpringUtil.systemName = systemName;
            SpringUtil.moduleManagerClass = moduleManagerClass;
            LogUtil.init("logback", "debug");
            setJarRootPath();
            SpringApplication springApplication = new SpringApplication(applicationClass);
            String result = PropertiesUtil.loadPropertiesByRouter(routerUrl, sm4key);
            if(!StringUtil.startsWith(result, "ok")){
                LogUtil.error("SpringbootUtil_start_1", "配置文件加载失败，系统无法启动", routerUrl, sm4key);
                throw new RuntimeException("配置文件加载失败，系统无法启动");
            }
            Map<String, Object> map = new HashMap<>();
            Map<String, String> propertiesMap = PropertiesUtil.getPropertiesMap();
            Set<String> keySet = propertiesMap.keySet();
            Iterator<String> iterator = keySet.iterator();
            while(iterator.hasNext()){
                String key = iterator.next();
                String value = propertiesMap.get(key);
                map.put(key, value);
            }
            springApplication.setDefaultProperties(map);
            // SsmPropertiesUtil.getPropertySourcesPlaceholderConfigurer();
            SpringUtil.applicationContext = springApplication.run(args);
            SpringUtil.webApplicationContext = (WebApplicationContext) SpringUtil.applicationContext;
            ServletContext servletContext = SpringUtil.webApplicationContext.getServletContext();
            ServletUtil.init(servletContext);
            ModuleManager.injection();
            NetUtil.init();
            PaginationUtil.init(pageIndex, pageSize);
        } catch (Throwable throwable) {
            LogUtil.error("SpringbootUtil_start_t1", throwable, routerUrl, sm4key);
        }
    }

    /**
     * 2024-07-16
     */
    public static final void setJarRootPath(String jarRootPath){
        p_jarRootPath = jarRootPath;
    }

    /**
     * 2024-07-16
     */
    public static final void setJarRootPath(){
        if(!StringUtil.isBlank(p_jarRootPath)){
            return;
        }
        //
        ApplicationHome applicationHome = new ApplicationHome();
        File file = applicationHome.getDir();
        Path path = file.toPath();
        p_jarRootPath = path.toString();
        p_jarRootPath = UrlUtil.getStandardPath(p_jarRootPath);
        p_jarRootPath = p_std(p_jarRootPath);
        TomcatUtil.setTomcatRootPath(p_jarRootPath);
    }

    /**
     * 2024-09-12
     * @return
     */
    public static final String getJarRootPath(){
        if(StringUtil.isBlank(p_jarRootPath)){
            setJarRootPath();
        }
        return p_jarRootPath;
    }

    /**
     * 2024-09-12
     * @param path
     * @return
     */
    private static String p_std(String path){
        for(int i = 65; i <= 90; i++){
            String upper = StringUtil.ascii2str(i);  // 获取一个大写字母
            if (!StringUtil.startsWith(path, upper)){
                continue;
            }
            if (!StringUtil.startsWith(path, upper + ":/")){
                continue;
            }
            int length = path.length();
            String right = StringUtil.substr(path, 3, length);
            String lower = StringUtil.toLowerCase(upper);
            path = lower + ":/" + right;
            break;
        }
        return path;
    }

    /**
     * 2025-01-09
     * 获取springboot运行时端口
     * @param webServerInitializedEvent
     * @return
     */
    public static final Integer getPort(final WebServerInitializedEvent webServerInitializedEvent){
        final WebServer webServer = webServerInitializedEvent.getWebServer();
        final Integer port = getPort(webServer);
        return port;
    }

    /**
     * 2025-01-09
     * 获取springboot运行时端口
     * @param webServer
     * @return
     */
    public static final Integer getPort(final WebServer webServer){
        final Integer port = webServer.getPort();
        return port;
    }

}