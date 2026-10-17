package xc.crabapple.core.tomcat;

import jakarta.servlet.ServletContext;
import org.apache.commons.io.IOUtils;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.device.EnvironmentUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.servlet.ServletUtil;
import xc.crabapple.core.url.UrlUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * 2024-09-11
 */
public final class TomcatUtil {

    // d:/app/tomcat/webapps/myapp
    private static String p_tomcatRootPath = "";

    // d:/app/tomcat/webapps/myapp/target/myapp
    private static String p_appRootPath = "";

    // d:/app/tomcat/webapps/myapp/target/myapp/WEB-INF/classes
    private static String p_classesRootPath = "";

    /**
     * 2024-09-11
     * @param servletContext
     */
    public static final void init(final ServletContext servletContext){
        setTomcatRootPath(servletContext);
        setAppRootPath(servletContext);
        setClassesRootPath(servletContext);
    }

    /**
     * 2023-07-19
     * 2024-07-10 返回值统一设置为磁盘符字母小写
     * 2024-09-11 统一整理代码
     * 2025-02-18 提升兼容性
     * 设置tomcat根目录
     * @param servletContext
     * @return 返回值格式如 d:/app/tomcat/webapps/myapp
     */
    public static final void setTomcatRootPath(final ServletContext servletContext){
        if(servletContext == null){
            LogUtil.error("TomcatUtil_setTomcatRootPath_1", "servletContext对象为空，请先执行ServletUtil.init方法");
            return;
        }
        if(!StringUtil.isBlank(p_tomcatRootPath)){
            return;
        }
        //
        String realPath = servletContext.getRealPath("/");
        realPath = UrlUtil.getStandardPath(realPath);
        if(StringUtil.containOf(realPath, "/target/")){
            String[] array = StringUtil.split(realPath, "/target/");
            realPath = array[0];
        }else if(StringUtil.containOf(realPath, "/src/main/")){
            String[] array = StringUtil.split(realPath, "/src/main/");
            realPath = array[0];
        }
        realPath = UrlUtil.getStandardPath(realPath);
        realPath = StringUtil.deleteLast(realPath, "/webapps/ROOT", "/src");
        p_tomcatRootPath = p_diskFirstLower(realPath);
        LogUtil.print("TomcatUtil_setTomcatRootPath_6", "服务中间件根目录初始化完成", p_tomcatRootPath);
    }

    /**
     * 2024-09-12
     * @param path
     */
    public static final void setTomcatRootPath(final String path){
        p_tomcatRootPath = path;
        LogUtil.print("TomcatUtil_setTomcatRootPath_8", "服务中间件根目录初始化完成", p_tomcatRootPath);
    }

    /**
     * 2024-07-10
     * 获取tomcat根目录
     * @return 返回值格式如 d:/app/tomcat/webapps/myapp
     */
    public static final String getTomcatRootPath(){
        if(!StringUtil.isBlank(p_tomcatRootPath)){
            return p_tomcatRootPath;
        }
        //
        setTomcatRootPath(ServletUtil.servletContext);
        return p_tomcatRootPath;
    }

    /**
     * 2024-09-11
     * 设置应用程序的根目录
     * @return 返回值格式如 d:/app/tomcat/webapps/myapp/target/myapp
     */
    public static final void setAppRootPath(final ServletContext servletContext){
        if(servletContext == null){
            LogUtil.error("TomcatUtil_setAppRootPath_1", "servletContext对象为空，请先执行ServletUtil.init方法");
            return;
        }
        if(!StringUtil.isBlank(p_appRootPath)){
            return;
        }
        //
        String realPath = servletContext.getRealPath("/");
        p_appRootPath = UrlUtil.getStandardPath(realPath);
    }

    /**
     * 2024-09-11
     * 获取应用程序的根目录
     * @return 返回值格式如 d:/app/tomcat/webapps/myapp/target/myapp
     */
    public static final String getAppRootPath(){
        if(!StringUtil.isBlank(p_appRootPath)){
            return p_appRootPath;
        }
        //
        setAppRootPath(ServletUtil.servletContext);
        return p_appRootPath;
    }

    /**
     * 2024-09-11
     * 设置classes的根目录
     * @return 返回值格式如 d:/app/tomcat/webapps/myapp/target/myapp/WEB-INF/classes
     */
    public static final void setClassesRootPath(final ServletContext servletContext){
        if(servletContext == null){
            LogUtil.error("TomcatUtil_setAppRootPath_1", "servletContext对象为空，请先执行ServletUtil.init方法");
            return;
        }
        if(!StringUtil.isBlank(p_classesRootPath)){
            return;
        }
        //
        String realPath = servletContext.getRealPath("/");
        realPath = UrlUtil.getStandardPath(realPath);
        realPath += "/WEB-INF/classes";
        p_classesRootPath = p_diskFirstLower(realPath);
    }

    /**
     * 2024-09-11
     * 获取classes的根目录
     * @return 返回值格式如 d:/app/tomcat/webapps/myapp/target/myapp/WEB-INF/classes
     */
    public static final String getClassesRootPath(){
        if(!StringUtil.isBlank(p_classesRootPath)){
            return p_classesRootPath;
        }
        //
        setClassesRootPath(ServletUtil.servletContext);
        return p_classesRootPath;
    }

    /**
     * 2024-09-11
     * 把windows下的磁盘盘符改为小写
     * @param realPath
     * @return
     */
    private static final String p_diskFirstLower(final String realPath){
        if(!EnvironmentUtil.isWindows()){
            return realPath;
        }
        // 2024-07-10
        String localRealPath = realPath;
        for(int i = 65; i <= 90; i++){
            String upper = StringUtil.ascii2str(i);  // 获取一个大写字母
            if (!StringUtil.startsWith(localRealPath, upper)){
                continue;
            }
            if (!StringUtil.startsWith(localRealPath, upper + ":/")){
                continue;
            }
            int length = localRealPath.length();
            String right = StringUtil.substr(localRealPath, 3, length);
            String lower = StringUtil.toLowerCase(upper);
            localRealPath = lower + ":/" + right;
            break;
        }
        return localRealPath;
    }

    /**
     * 2022-07-20
     * @param url
     * @return
     */
    public static final InputStream getInputStreamFromClassesPath(final String url){
        String localUrl = StringUtil.trim(url);
        if(StringUtil.isBlank(localUrl)){
            LogUtil.error("TomcatUtil_getInputStreamFromClassesPath_1", "url_is_null", localUrl);
            return null;
        }
        localUrl = UrlUtil.getStandardPath(localUrl, false);
        //
        InputStream inputStream = TomcatUtil.class.getResourceAsStream(localUrl);
        if(inputStream == null){
            LogUtil.error("TomcatUtil_getInputStreamFromClassesPath_2", localUrl);
            return null;
        }
        return inputStream;
    }

    /**
     * 2022-07-20
     * @param url
     * @return
     */
    public static final String getTextFromClassesPath(final String url){
        InputStream inputStream = null;
        try{
            inputStream = getInputStreamFromClassesPath(url);
            String result = IOUtils.toString(inputStream, "utf-8");
            return result;
        }catch(Throwable throwable){
            LogUtil.error("TomcatUtil_getTextFromClassesPath_t1", throwable, url);
            return "";
        } finally {
            IOUtil.close(inputStream);
        }
    }

    /**
     * 2022-07-20
     * @param url
     * @return
     */
    public static final BufferedImage getBufferedImageFromClassesPath(final String url){
        try{
            InputStream inputStream = getInputStreamFromClassesPath(url);
            if(inputStream == null){
                LogUtil.error("TomcatUtil_getBufferedImageFromClassesPath_1", url);
                return null;
            }
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            if(bufferedImage == null){
                LogUtil.error("TomcatUtil_getBufferedImageFromClassesPath_2", url);
                return null;
            }
            return bufferedImage;
        }catch(Throwable throwable){
            LogUtil.error("TomcatUtil_getBufferedImageFromClassesPath_t3", throwable, url);
            return null;
        }
    }

    /**
     * 2022-07-20
     * 获取(ssm项目)resources目录下文件的绝对路径
     * @param url
     * @return
     */
    public static final String getAbsoluteUrlFromClassesPath(final String url){
        String localUrl = StringUtil.trim(url);
        if(StringUtil.isBlank(localUrl)){
            LogUtil.error("TomcatUtil_getAbsoluteUrlFromClassesPath_1", "url_is_null", localUrl);
            return null;
        }
        localUrl = UrlUtil.getStandardPath(localUrl, false);
        //
        URL tmpUrl = TomcatUtil.class.getClassLoader().getResource(localUrl);
        String absoluteUrl = tmpUrl.getPath();
        if(StringUtil.isBlank(absoluteUrl) || absoluteUrl.length() <= 1){
            return absoluteUrl;
        }
        if(EnvironmentUtil.isWindows()){
            int length = absoluteUrl.length();
            absoluteUrl = StringUtil.substr(absoluteUrl, 1, length);
            return absoluteUrl;
        }
        return absoluteUrl;
    }

    /**
     * 2022-06-28
     * tomcat日志解析器
     * 从tomcat日志中获取含有tld文件的jar文件列表
     * 基于Tomcat-9.0.62
     * # 设置tomcat日志等级，可显示tomcat详细信息，日志内容可从idea控制台获取。
     * # 配置tomcat日志等级：tomcat/conf/logging.properties文件中追加下面一行：
     * # org.apache.jasper.servlet.TldScanner.level=FINE
     * @param tomcatLogUrl
     * @return
     */
    public static final String getJarIncludeTldTomcat(final String tomcatLogUrl){
        List<String> list = FileUtil.fileReadList(tomcatLogUrl);
        StringBuilder stringBuilder = new StringBuilder();
        for(String line : list){
            if(!StringUtil.containOf(line, "中找到了TLD文件")){
                continue;
            }
            final String LEFT = "/WEB-INF/lib/";
            final String RIGHT = ".jar";
            String[] array = StringUtil.split(line, LEFT);
            array = StringUtil.split(array[1], RIGHT);
            String jarName = array[0] + ".jar";
            jarName += ",\\\n";
            stringBuilder.append(jarName);
        }
        String result = stringBuilder.toString();
        result = StringUtil.substr(result, 0, result.length() - 3);
        // ClipboardUtil.setString(result);
        return result;
    }

}