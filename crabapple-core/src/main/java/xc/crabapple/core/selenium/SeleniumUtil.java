package xc.crabapple.core.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.thread.ThreadUtil;

import java.io.File;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 2023-09-13
 * 2024-04-06 优化初始化方式，方式优化完善，依赖版本更新，备注说明整理
 * 2025-02-19
 * 基于[selenium+chromedriver]实现
 *
 * # chromedriver相关网站：
 *   https://getwebdriver.com/
 *   https://chromedriver.com/download#stable
 *   https://blog.csdn.net/linglong_L/article/details/136283810
 *
 * # 使用selenium+chromedriver技术的步骤：
 *   1. 安装chrome并记录chrome.exe绝对路径。
 *   2. 解压到当前目录：D:/browser/chrome/chromedriver-win64-133.0.6943.126.7z
 *   3. 手动打开bat文件：D:/browser/chrome/启动本地chrome正式版.bat
 *      注意启动bat文件前结束所有chrome与chromedriver进程！！！
 *   4. Java程序首先执行SeleniumUtil.init()后执行正常的爬虫业务。
 *
 * # 注意事项：
 *   1. chromedriver不用安装，只需要程序指向即可。
 *   2. 提升稳定性、方缓存的方法：打开浏览器控制台（F12） ==> 网络（Network）选项卡 ==> 勾选复选框 禁用缓存（Disable Cache）即可，程序运行后控制台可以一直开着。
 *   3. JDK / selenium / chrome / chromedriver有明确的版本对应关系。如：JDK17 / selenium-java(maven)-4.19.1 / chrome-123.0.6312.105 / chromedriver-123.0.6312.105
 *
 * # 代理测试网址
 *   https://ip.900cha.com/
 *   https://www.ip138.com/
 */
public final class SeleniumUtil {

    private static String isInit = "no";

    /**
     * 2024-04-06
     */
    public static final void init(){
        init("C:/Program Files/Google/Chrome/Application/chrome.exe", "D:/browser/chrome/chromedriver-win64-133.0.6943.126/chromedriver.exe");
    }

    /**
     * 2024-04-06
     * @param chromeUrl 本地chrome浏览器安装地址，如：C:/Program Files/Google/Chrome/Application/chrome.exe
     */
    public static final void init(final String chromeUrl){
        init(chromeUrl, "D:/browser/chrome/chromedriver-win64-133.0.6943.126/chromedriver.exe");
    }

    /**
     * 2024-04-06
     * @param chromeUrl 本地chrome浏览器安装地址，如：C:/Program Files/Google/Chrome/Application/chrome.exe
     * @param chromeDriverUrl 本地chrome驱动安装地址，如：D:/browser/chrome/chromedriver-win64-133.0.6943.126.7z解压后对应位置
     */
    public static final void init(final String chromeUrl, final String chromeDriverUrl){
        LogUtil.print("SeleniumUtil_init_100", "别忘了第一件事是手动打开bat文件：D:/browser/chrome/启动本地chrome正式版.bat");
        //
        if(!FileUtil.exists(chromeUrl)){
            LogUtil.error("SeleniumUtil_init_1", "chromeUrl不存在");
            return;
        }
        if(!FileUtil.isRealFile(chromeUrl)){
            LogUtil.error("SeleniumUtil_init_2", "chromeUrl不是文件");
            return;
        }
        if(!FileUtil.exists(chromeDriverUrl)){
            LogUtil.error("SeleniumUtil_init_3", "chromeDriverUrl不存在");
            return;
        }
        if(!FileUtil.isRealFile(chromeDriverUrl)){
            LogUtil.error("SeleniumUtil_init_4", "chromeDriverUrl不是文件");
            return;
        }
        //
        System.setProperty("webdriver.chrome.bin", chromeUrl);
        System.setProperty("webdriver.chrome.driver", chromeDriverUrl);
        //
        isInit = "yes";
    }

    /**
     * 2023-09-13
     * 51job弹窗滑块
     * @param webDriver
     */
    public static final void huakuai01(final WebDriver webDriver){
        final By by = By.xpath("//*[@id=\"WAF_NC_WRAPPER\"]");
        final boolean exist = isExist(webDriver, by);
        if(!exist){
            return;
        }
        Boolean flag = waitUntilPageContainElement(webDriver, by, 3000);
        if(!flag){
            return;
        }
        //
        Actions action = new Actions(webDriver);
        By by2 = By.xpath("//span[@class='nc_iconfont btn_slide']");
        WebElement moveButton = webDriver.findElement(by2);
        //移到滑块元素并悬停
        action.moveToElement(moveButton).clickAndHold(moveButton);
        // action.moveByOffset(306, 0).perform();
        action.dragAndDropBy(moveButton,305, 0).perform();
        action.release();
    }
    
    /**
     * 2023-09-13
     * 51job详情页滑块
     * @param webDriver
     */
    public static final void huakuai02(final WebDriver webDriver){
        final By by = By.xpath("//*[@id=\"PC\"]");
        final boolean exist = isExist(webDriver, by);
        if(!exist){
            return;
        }
        Boolean flag = waitUntilPageContainElement(webDriver, by, 3000);
        if(!flag){
            return;
        }
        //
        Actions action = new Actions(webDriver);
        By by2 = By.xpath("//*[@id=\"nc_1_n1z\"]");
        WebElement moveButton = webDriver.findElement(by2);
        //移到滑块元素并悬停
        action.moveToElement(moveButton).clickAndHold(moveButton);
        // action.moveByOffset(306, 0).perform();
        action.dragAndDropBy(moveButton,305, 0).perform();
        action.release();
    }

    /**
     * 等待当前页面是否包含 text 文本 的元素
     * @param webDriver
     * @param text 包含的文本
     * @param seconds 等待时长，单位秒
     */
    public static final Boolean waitUntilPageContain(final WebDriver webDriver, final String text, final long seconds) {
        try {
            By by = By.xpath("//*[@text='" + text + "']");
            final boolean exist = isExist(webDriver, by);
            if(!exist){
                return false;
            }
            //
            WebDriverWait webDriverWait = new WebDriverWait(webDriver, Duration.ofMillis(seconds));
            ExpectedCondition<WebElement> elementExpectedCondition = ExpectedConditions.presenceOfElementLocated(by);
            webDriverWait.until(elementExpectedCondition);
            return true;
        }catch(Throwable throwable){
            LogUtil.error("SeleniumUtil_waitUntilPageContain_e1", throwable);
            return false;
        }
    }

    /**
     * 判断当前页面是否包含 XX element
     * @param webDriver
     * @param by 元素xpath路径
     * @param seconds 等待查找时间，单位秒
     */
    public static final Boolean waitUntilPageContainElement(final WebDriver webDriver, final By by, final long seconds) {
        final boolean exist = isExist(webDriver, by);
        if(!exist){
            return false;
        }
        //
        WebDriverWait webDriverWait = new WebDriverWait(webDriver, Duration.ofMillis(seconds));
        ExpectedCondition<WebElement> elementExpectedCondition = ExpectedConditions.presenceOfElementLocated(by);
        WebElement webElement = webDriverWait.until(elementExpectedCondition);
        if (webElement != null) {
            return true;
        }
        return false;
    }

    /**
     * 2023-09-13
     * 判断指定的元素是否存在
     * @param webDriver
     * @param by
     * @return
     */
    public static final boolean isExist(final WebDriver webDriver, final By by){
        try{
            WebElement webElement = webDriver.findElement(by);
            if(webElement == null){
                return false;
            }
            return true;
        }catch(Throwable throwable){
            return false;
        }
    }

    /**
     * 2023-09-12
     * 示例1：基本入门、独立窗口
     */
    private static final void demo01(){
        if(!"yes".equals(isInit)){
            LogUtil.error("SeleniumUtil_demo01_1", "未初始化浏览器参数", isInit);
            return;
        }
        //
        WebDriver driver = null;
        try {
            driver = new ChromeDriver();
            driver.get("https://www.baidu.com/");
            String title = driver.getTitle();
            LogUtil.print("SeleniumUtil_demo01_2", "Web form", title);
            driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));
            WebElement textBox = driver.findElement(By.id("kw"));
            textBox.sendKeys("hello");
            WebElement submitButton = driver.findElement(By.id("su"));
            submitButton.click();
        }catch(Throwable throwable){
            LogUtil.error("SeleniumUtil_demo01_t1", throwable, "demo01执行异常");
        }finally {
            if(driver != null){
                driver.quit();
            }
        }
    }

    /**
     * 2023-09-13
     * 示例2：独立窗口、简单交互
     */
    private static final void demo02(){
        // 1、设置chrome浏览器驱动
        if(!"yes".equals(isInit)){
            LogUtil.error("SeleniumUtil_demo02_1", "未初始化浏览器参数", isInit);
            return;
        }
        // 3、初始化一个浏览器实例
        ChromeOptions options = new ChromeOptions();
        /*
            // 启动时自动最大化窗口
            options.addArguments("--start-maximized");
            // 禁用阻止弹出窗口
            options.addArguments("--disable-popup-blocking");
            // 启动无沙盒模式运行
            options.addArguments("no-sandbox");
            // 禁用扩展
            options.addArguments("disable-extensions");
            // 默认浏览器检查
            options.addArguments("no-default-browser-check");
            // 设置chrome 无头模式
            options.setHeadless(Boolean.TRUE);
            //不用打开图形界面。
            options.addArguments("--headless");
        */
        options.addArguments("disable-infobars");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");
//        options.addArguments("--disable-popup-blocking");  // 禁用弹出拦截
//        options.addArguments("no-sandbox");  // 取消沙盒模式
        //设置开发者模式启动
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--no-sandbox", "--disable-gpu", "--window-position=0,0", "--window-size=2560,1600");
        options.addArguments("--headless");
        //
        //去掉chrome 正受到自动测试软件的控制
//        option.addArguments("disable-infobars");
//        option.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");
        //设置开发者模式启动
        // option.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--no-sandbox", "--disable-gpu", "--window-position=0,0", "--window-size=2560,1600");
        options.addArguments("--headless");
        options.addArguments("--remote-allow-origins=*");
//        options.addArguments("no-default-browser-check");  // 禁止默认浏览器检查
//        options.addArguments("about:histograms");
//        options.addArguments("about:cache");
//        options.addArguments("disable-extensions");  // 禁用扩展
//        options.addArguments("disable-glsl-translator");  // 禁用GLSL翻译
//        options.addArguments("disable-translate");  // 禁用翻译
//        options.addArguments("--disable-gpu");  // 谷歌文档提到需要加上这个属性来规避bug
//        options.addArguments("--disable-dev-shm-usage");
//        options.addArguments("--hide-scrollbars");  // 隐藏滚动条, 应对一些特殊页面
//        options.addArguments("blink-settings=imagesEnabled=false");  // 不加载图片, 提升速度
//        options.addArguments("test-type");
        //
        WebDriver webDriver = new ChromeDriver(options);
        try{
        webDriver.manage().window().maximize();
        webDriver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(10));
        webDriver.get("https://www.baidu.com/");
        Date date = new Date();
        date.setTime(1710167670000L);
        webDriver.manage().addCookie(new Cookie("demoKey1","demoValue1", ".example.com", "/", date, false, false));
        webDriver.manage().addCookie(new Cookie("demoKey2","demoValue2", ".example.com", "/", date, false, true));
        // 4、加载网页程序（某度）
        // 5、通过ID选择器定位输入框,并设置搜索关键字"帅哥"
        webDriver.findElement(By.id("kw")).sendKeys("帅哥");
        // 6、通过Class类样式选择器定位,模拟点击事件并提交表单
        webDriver.findElement(By.className("s_btn")).submit();
        // 7、加载第二个页面：展示搜索页面元素信息
        WebElement webElement = webDriver.findElement(By.id("imgid"));
        // 8、定位搜索到所有图片元素
        List<WebElement> webElementList = webElement.findElements(By.className("imgbox"));
        // 9、异步获取图片地址
        // 10、模拟页面图片点击事件
        for(int i = 0; i < 2; i++){
            WebElement webElement1 = webElementList.get(i);
//                webElement1.click();
            WebElement currentImages = webElement1.findElement(By.className("main_img"));
            String src = currentImages.getAttribute("src");
            LogUtil.print("SeleniumUtil_demo02_2", "图片地址", src);
        }
        }catch(Throwable throwable){
            LogUtil.error("SeleniumUtil_demo02_t1", throwable);
        }finally{
            webDriver.quit();
        }
    }

    /**
     * 2023-09-13
     * 示例3：复用本地浏览器窗口、代理
     * 对windows操作系统设置代理，就不用在代码层面设置代理了
     */
    private static final void demo03(){
        if(!"yes".equals(isInit)){
            LogUtil.error("SeleniumUtil_demo03_1", "未初始化浏览器参数", isInit);
            return;
        }
        //
        ChromeOptions options = new ChromeOptions();
        // 复用本地浏览器窗口
        options.setExperimentalOption("debuggerAddress", "127.0.0.1:9222");
        // 代理（隧道代理）
        if(false){
            final String proxyUsername = "your-order-id";  // 隧道代理订单号
            final String proxyPassword = "your-password";  // 密码（用户中心-我的订单页面可查）
            final String proxyHost = "your-proxy-host";
            final Integer proxyPort = 50000;
            Authenticator.setDefault(new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(proxyUsername, proxyPassword.toCharArray());
                }
            });
            System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
            Proxy proxy = new Proxy();
            proxy.setHttpProxy(proxyHost+":"+proxyPort);
            proxy.setSocksUsername(proxyUsername);
            proxy.setSocksPassword(proxyPassword);
            options.addArguments("--proxy-server=http://"+proxyHost+":"+proxyPort);  // 必须是中文冒号
            File file = new File("D:/app/crawler/proxy/proxy.zip");
            options.addExtensions(file);
            options.setProxy(proxy);
            options.setCapability("proxy", proxy);
        }
        //
        WebDriver webDriver = null;
        try {
            webDriver = new ChromeDriver(options);
            while(true){
                webDriver.get("https://ip.900cha.com/");
                ThreadUtil.sleep(10000L);
            }
        }catch(Throwable throwable){
            LogUtil.error("SeleniumUtil_demo03_t1", throwable, "demo03执行异常");
        }finally {
            if(webDriver != null){
                webDriver.quit();
            }
        }
    }
    
}