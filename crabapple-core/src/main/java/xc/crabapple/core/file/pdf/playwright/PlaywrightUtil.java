package xc.crabapple.core.file.pdf.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Margin;
import com.microsoft.playwright.options.Media;
import com.microsoft.playwright.options.WaitUntilState;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.timer.TimerUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 2026-08-03 playwright替代wkhtmltopdf
 * Playwright工具类，实现HTML转PDF功能
 * 使用Microsoft Playwright Java SDK 1.61.0 + Chromium 149.0.7827.55
 * Chromium浏览器需通过部署文档预先安装，运行时不涉及网络下载，适用于内网无网络环境
 * 浏览器路径通过环境变量PLAYWRIGHT_BROWSERS_PATH指定
 */
public final class PlaywrightUtil {

    /**
     * 2026-08-03 playwright替代wkhtmltopdf
     * HTML转PDF主入口，处理HTML输入源（htmlUrl/htmlText/templateHtmlUrl），调用底层转换方法
     * @param configBean 配置
     * @return 成功返回htmlUrl，失败返回null
     */
    public static final String html2pdf(final PlaywrightConfigBean configBean) {
        final String homeUrl = configBean.getHomeUrl();
        String htmlUrlLocal = configBean.getHtmlUrl();
        String htmlTextLocal = configBean.getHtmlText();
        final String templateHtmlUrl = configBean.getTemplateHtmlUrl();
        final String htmlReplace = configBean.getHtmlReplace();
        final String pdfUrl = configBean.getPdfUrl();
        String cacheHtmlUrlLocal = configBean.getCacheHtmlUrl();
        // 判断是否为系统自动生成的临时缓存文件，需要在转换后清理
        boolean isTempCacheFile = "yes".equals(cacheHtmlUrlLocal) || StringUtil.isBlank(cacheHtmlUrlLocal);
        if ("yes".equals(cacheHtmlUrlLocal)) {
            cacheHtmlUrlLocal = p_getDefaultCacheHtmlUrl();
        }
        // 记录需要在转换后清理的临时文件路径
        String tempFileUrl = null;
        // 创建pdf输出目录
        final String pdfPath = UrlUtil.getPathByUrl(pdfUrl);
        FileUtil.dirMake(pdfPath);
        // 处理html输入源
        if (StringUtil.isBlank(htmlUrlLocal)) {
            if (StringUtil.isBlank(htmlTextLocal)) {
                LogUtil.error("PlaywrightUtil_html2pdf_1", "htmlUrl与htmlText均为空", homeUrl, pdfUrl);
                return null;
            }
            // 2026-08-10 htmlUrl为空时使用htmlText生成临时HTML文件，转换前给出警告日志
            LogUtil.warn("PlaywrightUtil_html2pdf_2", "htmlUrl为空，将使用htmlText生成临时HTML文件", homeUrl, pdfUrl);
            // 使用模板HTML时，读取模板内容并替换占位符
            if (!StringUtil.isBlank(templateHtmlUrl)) {
                final String templateHtmlText = FileUtil.fileReadText(templateHtmlUrl);
                if (StringUtil.isBlank(templateHtmlText)) {
                    LogUtil.error("PlaywrightUtil_html2pdf_4", "templateHtmlUrl文件不存在或内容为空", homeUrl, htmlUrlLocal, templateHtmlUrl);
                    return null;
                }
                if (!StringUtil.isBlank(htmlReplace)) {
                    htmlTextLocal = StringUtil.replaceAll(templateHtmlText, htmlReplace, htmlTextLocal);
                }
            } else {
                // 2026-08-10 templateHtmlUrl为空时直接使用htmlText，转换前给出警告日志
                LogUtil.warn("PlaywrightUtil_html2pdf_3", "templateHtmlUrl为空，将直接使用htmlText", homeUrl, pdfUrl);
            }
            configBean.setHtmlText(htmlTextLocal);
            // 设置缓存HTML路径
            if (isTempCacheFile) {
                if (StringUtil.isBlank(cacheHtmlUrlLocal)) {
                    cacheHtmlUrlLocal = p_getDefaultCacheHtmlUrl();
                }
                htmlUrlLocal = cacheHtmlUrlLocal;
                tempFileUrl = cacheHtmlUrlLocal;
            } else {
                htmlUrlLocal = cacheHtmlUrlLocal;
            }
            FileUtil.fileWriteNew(htmlUrlLocal, htmlTextLocal, true);
        } else {
            // htmlUrl已提供时，复制到缓存路径
            if (!StringUtil.isBlank(cacheHtmlUrlLocal)) {
                FileUtil.copy(htmlUrlLocal, cacheHtmlUrlLocal);
                if (isTempCacheFile) {
                    tempFileUrl = cacheHtmlUrlLocal;
                }
            }
        }
        // 执行转换
        Map<String, String> paramMap = configBean.getParamMap();
        int timeoutSeconds = configBean.getTimeoutSeconds();
        try {
            htmlUrlLocal = html2pdf(homeUrl, htmlUrlLocal, pdfUrl, paramMap, timeoutSeconds);
            if (htmlUrlLocal == null) {
                LogUtil.error("PlaywrightUtil_html2pdf_7", "HTML转PDF失败", homeUrl, htmlUrlLocal, pdfUrl);
            }
            return htmlUrlLocal;
        } finally {
            // 清理系统生成的临时缓存文件
            if (tempFileUrl != null) {
                boolean deleteFlag = FileUtil.fileDelete(tempFileUrl);
                if (!deleteFlag) {
                    LogUtil.warn("PlaywrightUtil_html2pdf_8", "临时缓存文件清理失败", tempFileUrl);
                }
            }
        }
    }

    /**
     * 2026-08-03 playwright替代wkhtmltopdf
     * 将指定HTML文件转换为PDF文件
     * @param homeUrl 保留字段（Playwright不使用，为保持方法签名兼容）
     * @param htmlUrl html文件绝对路径
     * @param pdfUrl 目标pdf文件绝对路径
     * @param paramMap 自定义参数Map（key格式如--page-size、--margin-left等）
     * @param timeoutSeconds 执行超时时间（秒）
     * @return 成功返回htmlUrl，失败返回null
     */
    public static final String html2pdf(final String homeUrl, final String htmlUrl, final String pdfUrl, final Map<String, String> paramMap, final int timeoutSeconds) {
        long startTimeMillis = System.currentTimeMillis();
        LogUtil.print("PlaywrightUtil_html2pdf_11", "开始转换", htmlUrl, pdfUrl);
        // 2026-09-14 转PDF宋体名称兼容修复
        String contextHtml = FileUtil.fileReadText(htmlUrl);
        contextHtml = StringUtil.replaceAll(contextHtml, "font-family: 宋体;", "font-family: 宋体, simsun;");
        contextHtml = StringUtil.replaceAll(contextHtml, "font-family:宋体;", "font-family: 宋体, simsun;");
        FileUtil.fileWriteNew(htmlUrl, contextHtml, true);
        // 内网环境预检
        if (!p_checkBrowser()) {
            return null;
        }
        // 删除旧的pdf文件
        FileUtil.fileDelete(pdfUrl);
        // 创建Playwright实例和Chromium浏览器
        Playwright playwright = null;
        Browser browser = null;
        try {
            playwright = Playwright.create();
            // Chromium启动参数
            List<String> launchArgs = new ArrayList<>();
            launchArgs.add("--no-sandbox");
            launchArgs.add("--disable-dev-shm-usage");
            launchArgs.add("--disable-gpu");
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(true).setArgs(launchArgs);
            browser = playwright.chromium().launch(launchOptions);
            Page page = browser.newPage();
            // 设置默认超时
            page.setDefaultTimeout(timeoutSeconds * 1000L);
            // 设置媒体类型为screen，确保PDF样式与浏览器显示一致
            page.emulateMedia(new Page.EmulateMediaOptions().setMedia(Media.SCREEN));
            // 导航到HTML文件
            String fileUrl = Paths.get(htmlUrl).toUri().toString();
            Page.NavigateOptions navigateOptions = new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE);
            page.navigate(fileUrl, navigateOptions);
            // 处理javascript-delay参数（默认10000毫秒，确保JS渲染完成）
            long jsDelay = 10000L;
            if (paramMap != null) {
                String javascriptDelay = paramMap.get("--javascript-delay");
                if (!StringUtil.isBlank(javascriptDelay)) {
                    try {
                        jsDelay = Long.parseLong(javascriptDelay);
                    } catch (NumberFormatException e) {
                        LogUtil.warn("PlaywrightUtil_html2pdf_12", "javascript-delay参数格式无效，已使用默认值", javascriptDelay, jsDelay);
                    }
                }
                // 处理window-status参数
                String windowStatus = paramMap.get("--window-status");
                if (!StringUtil.isBlank(windowStatus)) {
                    try {
                        String safeStatus = windowStatus.replace("'", "\\'");
                        page.waitForFunction("window.status === '" + safeStatus + "'");
                    } catch (Throwable throwable) {
                        LogUtil.warn("PlaywrightUtil_html2pdf_14", "window-status等待超时，继续生成PDF", windowStatus, throwable);
                    }
                }
            }
            page.waitForTimeout(jsDelay);
            // 构建PDF选项并生成PDF
            Page.PdfOptions pdfOptions = p_buildPdfOptions(paramMap, pdfUrl);
            page.pdf(pdfOptions);
            // 记录转换结果
            long costTimeMillis = System.currentTimeMillis() - startTimeMillis;
            long fileSizeBytes = FileUtil.getFileSize(pdfUrl);
            LogUtil.print("PlaywrightUtil_html2pdf_15", "转换成功", costTimeMillis + "ms", fileSizeBytes + "bytes", htmlUrl, pdfUrl);
            return htmlUrl;
        } catch (Throwable throwable) {
            LogUtil.error("PlaywrightUtil_html2pdf_16", throwable, "HTML转PDF失败", htmlUrl, pdfUrl);
            return null;
        } finally {
            p_closeQuietly(browser);
            p_closeQuietly(playwright);
        }
    }

    /**
     * 2026-08-03 playwright替代wkhtmltopdf
     * 获取默认缓存HTML文件路径
     */
    private static final String p_getDefaultCacheHtmlUrl() {
        String now = TimerUtil.now(TimerUtil.FORMATTYPE_M3);
        String random = RandomUtil.getStr(16, false);
        String name = "pw_" + now + "_" + random + ".html";
        final String defaultCacheHtmlUrl = UrlUtil.getStandardPath("/data/crabapple/pw/" + name);
        return defaultCacheHtmlUrl;
    }

    /**
     * 2026-08-03 playwright替代wkhtmltopdf
     * 将参数Map转换为Playwright的PdfOptions
     * 默认值：页面宽302mm x 高423mm，左右边距0mm，上下边距10mm，缩放0.85
     * @param paramMap 参数Map
     * @param pdfUrl 输出PDF文件路径
     * @return Playwright PdfOptions对象
     */
    private static final Page.PdfOptions p_buildPdfOptions(final Map<String, String> paramMap, final String pdfUrl) {
        Page.PdfOptions pdfOptions = new Page.PdfOptions();
        // 设置输出文件路径
        pdfOptions.setPath(Paths.get(pdfUrl));
        // 打印背景色和背景图片
        pdfOptions.setPrintBackground(true);
        // 默认值
        String pageWidth = "302mm";
        String pageHeight = "423mm";
        String marginLeft = "0mm";
        String marginRight = "0mm";
        String marginTop = "10mm";
        String marginBottom = "10mm";
        String orientation = "Portrait";
        String zoom = "0.85";
        boolean preferCSSPageSize = false;
        // 从paramMap中读取参数，覆盖默认值
        if (paramMap != null) {
            String pageWidthValue = paramMap.get("--page-width");
            if (!StringUtil.isBlank(pageWidthValue)) {
                pageWidth = pageWidthValue;
            }
            String pageHeightValue = paramMap.get("--page-height");
            if (!StringUtil.isBlank(pageHeightValue)) {
                pageHeight = pageHeightValue;
            }
            String marginLeftValue = paramMap.get("--margin-left");
            if (!StringUtil.isBlank(marginLeftValue)) {
                marginLeft = marginLeftValue;
            }
            String marginRightValue = paramMap.get("--margin-right");
            if (!StringUtil.isBlank(marginRightValue)) {
                marginRight = marginRightValue;
            }
            String marginTopValue = paramMap.get("--margin-top");
            if (!StringUtil.isBlank(marginTopValue)) {
                marginTop = marginTopValue;
            }
            String marginBottomValue = paramMap.get("--margin-bottom");
            if (!StringUtil.isBlank(marginBottomValue)) {
                marginBottom = marginBottomValue;
            }
            String orientationValue = paramMap.get("--orientation");
            if (!StringUtil.isBlank(orientationValue)) {
                orientation = orientationValue;
            }
            String zoomValue = paramMap.get("--zoom");
            if (!StringUtil.isBlank(zoomValue)) {
                zoom = zoomValue;
            }
            String preferCSSPageSizeValue = paramMap.get("--prefer-css-page-size");
            if ("true".equalsIgnoreCase(preferCSSPageSizeValue) || "yes".equalsIgnoreCase(preferCSSPageSizeValue)) {
                preferCSSPageSize = true;
            }
        }
        // 应用页面尺寸
        pdfOptions.setPreferCSSPageSize(preferCSSPageSize);
        pdfOptions.setWidth(pageWidth);
        pdfOptions.setHeight(pageHeight);
        // 应用页边距
        Margin margin = new Margin();
        margin.setTop(marginTop);
        margin.setBottom(marginBottom);
        margin.setLeft(marginLeft);
        margin.setRight(marginRight);
        pdfOptions.setMargin(margin);
        // 应用方向
        boolean landscape = "Landscape".equalsIgnoreCase(orientation);
        pdfOptions.setLandscape(landscape);
        // 应用缩放
        try {
            double scale = Double.parseDouble(zoom);
            if (scale < 0.1) {
                scale = 0.1;
            }
            if (scale > 2.0) {
                scale = 2.0;
            }
            pdfOptions.setScale(scale);
        } catch (NumberFormatException e) {
            // 忽略无效格式，使用默认scale
        }
        return pdfOptions;
    }

    /**
     * 2026-08-03 playwright替代wkhtmltopdf
     * 检查Chromium浏览器是否已部署
     * 内网环境下浏览器需通过部署文档预先安装，代码不涉及任何网络下载或环境部署
     * @return true=浏览器已部署, false=浏览器未部署
     */
    private static final boolean p_checkBrowser() {
        // 优先从环境变量获取浏览器路径
        String browsersPath = System.getenv("PLAYWRIGHT_BROWSERS_PATH");
        if (StringUtil.isBlank(browsersPath)) {
            String osName = System.getProperty("os.name", "").toLowerCase();
            String userHome = System.getProperty("user.home", "");
            if (osName.contains("win")) {
                browsersPath = userHome + "\\AppData\\Local\\ms-playwright";
            } else {
                browsersPath = userHome + "/.cache/ms-playwright";
            }
        }
        // 检查目录是否存在
        File browsersDir = new File(browsersPath);
        if (!browsersDir.exists()) {
            LogUtil.error("PlaywrightUtil_p_checkBrowser_1", "Chromium浏览器目录不存在", browsersPath,
                    "请参照部署文档安装Chromium浏览器，并设置环境变量PLAYWRIGHT_BROWSERS_PATH");
            return false;
        }
        // 检查是否有chromium开头的子目录
        File[] children = browsersDir.listFiles();
        if (children == null || children.length == 0) {
            LogUtil.error("PlaywrightUtil_p_checkBrowser_2", "Chromium浏览器目录为空", browsersPath,
                    "请参照部署文档安装Chromium浏览器");
            return false;
        }
        boolean hasChromium = false;
        for (int i = 0; i < children.length; i++) {
            String childName = children[i].getName();
            if (childName.startsWith("chromium")) {
                hasChromium = true;
                break;
            }
        }
        if (!hasChromium) {
            LogUtil.error("PlaywrightUtil_p_checkBrowser_3", "未找到Chromium浏览器", browsersPath,
                    "请参照部署文档安装Chromium浏览器");
            return false;
        }
        return true;
    }

    /**
     * 2026-08-03 playwright替代wkhtmltopdf
     * 静默关闭资源
     */
    private static void p_closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable throwable) {
                LogUtil.warn("PlaywrightUtil_p_closeQuietly_1", "关闭资源异常，可能存在资源泄漏", closeable.getClass().getSimpleName(), throwable);
            }
        }
    }

}
