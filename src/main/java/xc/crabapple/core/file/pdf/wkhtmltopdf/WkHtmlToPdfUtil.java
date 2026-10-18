package xc.crabapple.core.file.pdf.wkhtmltopdf;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.timer.TimerUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 2022-05-23
 * wkhtmltopdf工具类
 * wkhtmltopdf版本 v0.12.6
 * wkhtmltopdf的Linux版本中，解压后默认文件名为"wkhtmltox"，为统一命名解压后重命名为"wkhtmltopdf"，命令：mv wkhtmltox wkhtmltopdf
 * 官方帮助文档 https://wkhtmltopdf.org/usage/wkhtmltopdf.txt
 * 图片显示：
 *   https://qa.1r1g.com/sf/ask/1163911731/
 *   1.本地文件是绝对路径，且以file:///开头，如：<img src="file:///D:/app/tmp/桌面.jpg" />
 *   2.指令添加参数：--enable-local-file-access
 */
public final class WkHtmlToPdfUtil {

    // wkhtmltopdf默认参数，显式设置为null即不使用该字段
    private static final Map<String, String> S_DEFAULT_PARAM_MAP = p_initDefaultParamMap();

    /**
     * 2026-07-17 初始化wkhtmltopdf默认参数
     */
    private static final Map<String, String> p_initDefaultParamMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("--page-size", "A4");
        map.put("--page-height", "297mm");  // 423mm
        map.put("--page-width", "210mm");  // 302mm
        map.put("--margin-left", "10mm");  // 默认是10mm
        map.put("--margin-right", "10mm");  // 默认是10mm
        map.put("--margin-top", "10mm");  // 默认是10mm
        map.put("--margin-bottom", "10mm");  // 默认是10mm
        map.put("--minimum-font-size", "1");  // 最小的字体大小，默认5
        map.put("--disable-smart-shrinking", "");  // 禁用收缩，即不收缩
        map.put("--enable-smart-shrinking", null);  // 不启用收缩，即收缩
        map.put("--encoding", "UTF-8");
        map.put("--enable-javascript", "");
        map.put("--no-stop-slow-scripts", "");
        map.put("--dpi", "300");
        map.put("--image-dpi", "300");
        map.put("--image-quality", "100");
        map.put("--zoom", "1");
        map.put("--enable-local-file-access", "");  // 2024-12-02 null改为空字符串
        map.put("--no-outline", null);
        map.put("--orientation", "Portrait");  // Set orientation to Landscape or Portrait(default Portrait)
        map.put("--javascript-delay", null);  // 解决html源码中js延迟执行导致转换的pdf文件中无法渲染的问题
        map.put("--debug-javascript", null);  // 解决html源码中js延迟执行导致转换的pdf文件中无法渲染的问题
        map.put("--window-status", null);  // 解决html源码中js延迟执行导致转换的pdf文件中无法渲染的问题
        return map;
    }

    /**
     * 2022-05-28
     * 2022-09-21 增加模板为空的情况
     * 2022-12-22 可动态配置wk参数
     * 2026-07-30 wkhtmltopdf中文字体名与英文字体名映射关系（wkhtmltopdf 0.12.6无法正确解析中文字体名，必须使用英文名）：
     * --- Windows系统字体 ---
     * 宋体 → SimSun（simsun.ttc）
     * 新宋体 → NSimSun（simsun.ttc）
     * 黑体 → SimHei（simhei.ttf）
     * 微软雅黑 → Microsoft YaHei（msyh.ttc）
     * 微软雅黑 Light → Microsoft YaHei Light（msyhl.ttc）
     * 微软正黑体 → Microsoft JhengHei（msjh.ttc）
     * 微軟正黑體 → Microsoft JhengHei（msjh.ttc，繁体名）
     * 微軟正黑體 Light → Microsoft JhengHei Light（msjhl.ttc，繁体名）
     * 楷体 → KaiTi（simkai.ttf）
     * 楷体_GB2312 → KaiTi_GB2312（simkai.ttf）
     * 仿宋 → FangSong（simfang.ttf）
     * 仿宋_GB2312 → FangSong_GB2312（simfang.ttf）
     * 隶书 → LiSu（SIMLI.TTF）
     * 幼圆 → YouYuan（SIMYOU.TTF）
     * 等线 → DengXian（Deng.ttf）
     * 等线 Light → DengXian Light（Dengl.ttf）
     *
     * --- 华文字体（macOS/Windows） ---
     * 华文宋体 → STSong（STSONG.TTF）
     * 华文仿宋 → STFangsong（STFANGSO.TTF）
     * 华文楷体 → STKaiti（STKAITI.TTF）
     * 华文细黑 → STXihei（STXIHEI.TTF）
     * 华文黑体 → STHeiti（macOS）
     * 华文中宋 → STZhongsong（STZHONGS.TTF）
     * 华文新魏 → STXinwei（STXINWEI.TTF）
     * 华文行楷 → STXingkai（STXINGKA.TTF）
     * 华文隶书 → STLiti（STLITI.TTF）
     * 华文彩云 → STCaiyun（STCAIYUN.TTF）
     * 华文琥珀 → STHupo（STHUPO.TTF）
     *
     * --- 方正字体 ---
     * 方正舒体 → FZShuTi（FZSTK.TTF）
     * 方正姚体 → FZYaoti（FZYTK.TTF）
     * 方正黑体 → FZHeiTi
     * 方正宋体 → FZSongTi
     * 方正楷体 → FZKaiTi
     * 方正仿宋 → FZFangSong
     *
     * --- 汉仪字体 ---
     * 汉仪中黑 → HYZhongHei
     * 汉仪大黑 → HYDaHei
     * 汉仪中宋 → HYZhongSong
     * 汉仪大宋 → HYDaSong
     * 汉仪楷体 → HYKaiTi
     * 汉仪仿宋 → HYFangSong
     * 汉仪细黑 → HYXiHei
     * 汉仪细等线 → HYXiDengXian
     * 汉仪粗黑 → HYCuHei
     * 汉仪粗宋 → HYCuSong
     * 汉仪粗圆 → HYCuYuan
     * 汉仪中圆 → HYZhongYuan
     * 汉仪细圆 → HYXiYuan
     * 汉仪长美黑 → HYChangMeiHei
     * 汉仪长艺体 → HYChangYiTi
     * 汉仪长宋 → HYChangSong
     * 汉仪长楷 → HYChangKai
     * 汉仪长仿宋 → HYChangFangSong
     * 汉仪长等线 → HYChangDengXian
     * 汉仪长黑 → HYChangHei
     * 汉仪长圆 → HYChangYuan
     * 汉仪长中黑 → HYChangZhongHei
     * 汉仪长中宋 → HYChangZhongSong
     * 汉仪长中楷 → HYChangZhongKai
     * 汉仪长中仿宋 → HYChangZhongFangSong
     * 汉仪长中等线 → HYChangZhongDengXian
     * 汉仪长中圆 → HYChangZhongYuan
     *
     * --- macOS字体 ---
     * 苹方 → PingFang SC
     * 冬青黑体 → Hiragino Sans GB
     *
     * --- 跨平台开源字体 ---
     * 思源黑体 → Source Han Sans SC
     * 思源宋体 → Source Han Serif SC
     * 文泉驿正黑 → WenQuanYi Zen Hei（Linux）
     * 文泉驿微米黑 → WenQuanYi Micro Hei（Linux）
     */
    public static final String html2pdf(final WkHtmlToPdfConfigBean wkHtmlToPdfConfigBean) {
        final String homeUrl = wkHtmlToPdfConfigBean.getHomeUrl();
        String htmlUrlLocal = wkHtmlToPdfConfigBean.getHtmlUrl();
        String htmlTextLocal = wkHtmlToPdfConfigBean.getHtmlText();
        final String templateHtmlUrl = wkHtmlToPdfConfigBean.getTemplateHtmlUrl();
        final String htmlReplace = wkHtmlToPdfConfigBean.getHtmlReplace();
        final String pdfUrl = wkHtmlToPdfConfigBean.getPdfUrl();
        String cacheHtmlUrlLocal = wkHtmlToPdfConfigBean.getCacheHtmlUrl();
        // 标记是否为系统自动生成的临时缓存文件，需要在转换后清理
        boolean isTempCacheFile = "yes".equals(cacheHtmlUrlLocal) || StringUtil.isBlank(cacheHtmlUrlLocal);
        if ("yes".equals(cacheHtmlUrlLocal)) {
            cacheHtmlUrlLocal = p_getDefaultCacheHtmlUrl();
        }
        // 记录需要在转换后清理的临时文件路径（覆盖所有生成临时文件的分支）
        String tempFileUrl = null;
        // 创建pdf输出目录
        final String pdfPath = UrlUtil.getPathByUrl(pdfUrl);
        FileUtil.dirMake(pdfPath);
        // 处理html输入源
        if (StringUtil.isBlank(htmlUrlLocal)) {
            if (StringUtil.isBlank(htmlTextLocal)) {
                LogUtil.error("WkHtmlToPdfUtil_html2pdf_1", "htmlUrl与htmlText均为空", homeUrl, pdfUrl);
                return null;
            }
            // 2026-08-10 htmlUrl为空时使用htmlText生成临时HTML文件，转换前给出警告日志
            LogUtil.warn("WkHtmlToPdfUtil_html2pdf_2", "htmlUrl为空，将使用htmlText生成临时HTML文件", homeUrl, pdfUrl);
            if (!StringUtil.isBlank(templateHtmlUrl)) {
                final String templateHtmlText = FileUtil.fileReadText(templateHtmlUrl);
                if (StringUtil.isBlank(templateHtmlText)) {
                    LogUtil.error("WkHtmlToPdfUtil_html2pdf_4", "templateHtmlUrl文件不存在或内容为空", homeUrl, htmlUrlLocal, templateHtmlUrl);
                    return null;
                }
                if (!StringUtil.isBlank(htmlReplace)) {
                    htmlTextLocal = StringUtil.replaceAll(templateHtmlText, htmlReplace, htmlTextLocal);
                }
            } else {
                // 2026-08-10 templateHtmlUrl为空时直接使用htmlText，转换前给出警告日志
                LogUtil.warn("WkHtmlToPdfUtil_html2pdf_3", "templateHtmlUrl为空，将直接使用htmlText", homeUrl, pdfUrl);
            }
            wkHtmlToPdfConfigBean.setHtmlText(htmlTextLocal);
            if (isTempCacheFile) {
                // cacheHtmlUrl为空或"yes"时，生成临时路径供本次转换使用
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
            if (!StringUtil.isBlank(cacheHtmlUrlLocal)) {
                FileUtil.copy(htmlUrlLocal, cacheHtmlUrlLocal);
                if (isTempCacheFile) {
                    tempFileUrl = cacheHtmlUrlLocal;
                }
            }
        }
        // 2026-07-17 WkHtmlToPdfUtil优化
        // 执行转换，传入配置的超时时间
        Map<String, String> paramMap = wkHtmlToPdfConfigBean.getParamMap();
        int timeoutSeconds = wkHtmlToPdfConfigBean.getTimeoutSeconds();
        try {
            htmlUrlLocal = html2pdf(homeUrl, htmlUrlLocal, pdfUrl, paramMap, timeoutSeconds);
            return htmlUrlLocal;
        } finally {
            // 清理系统生成的临时缓存文件
            if (tempFileUrl != null) {
                FileUtil.fileDelete(tempFileUrl);
            }
        }
    }

    /**
     * 2025-11-17
     */
    private static final String p_getDefaultCacheHtmlUrl() {
        String now = TimerUtil.now(TimerUtil.FORMATTYPE_M3);
        String random = RandomUtil.getStr(16, false);
        String name = "wk_" + now + "_" + random + ".html";
        final String defaultCacheHtmlUrl = UrlUtil.getStandardPath("/data/crabapple/wk/" + name);
        return defaultCacheHtmlUrl;
    }

    /**
     * 2022-05-23
     * 2026-07-17 WkHtmlToPdfUtil优化 增加超时配置参数
     * 将指定HTML文件转换为PDF文件
     * @param homeUrl windows格式如：C:/Program Files/wkhtmltopdf/bin/wkhtmltopdf.exe，linux格式如：/opt/wkhtmltopdf/bin/wkhtmltopdf
     * @param htmlUrl html文件绝对路径
     * @param pdfUrl 目标pdf文件绝对路径
     * @param paramMap 自定义参数Map
     * @param timeoutSeconds 执行超时时间（秒）
     */
    public static final String html2pdf(final String homeUrl, final String htmlUrl, final String pdfUrl, final Map<String, String> paramMap, final int timeoutSeconds) {
        // 2026-07-17 WkHtmlToPdfUtil优化
        // 记录开始时间，用于计算转换耗时
        long startTimeMillis = System.currentTimeMillis();
        LogUtil.print("WkHtmlToPdfUtil_html2pdf_11", "开始转换", homeUrl, htmlUrl, pdfUrl);
        // 2026-09-14 转PDF宋体名称兼容修复
        String contextHtml = FileUtil.fileReadText(htmlUrl);
        contextHtml = StringUtil.replaceAll(contextHtml, "font-family: 宋体;", "font-family: 宋体, simsun;");
        contextHtml = StringUtil.replaceAll(contextHtml, "font-family:宋体;", "font-family: 宋体, simsun;");
        FileUtil.fileWriteNew(htmlUrl, contextHtml, true);
        // 删除旧的pdf文件
        FileUtil.fileDelete(pdfUrl);
        // 合并参数：创建新Map避免修改传入的paramMap，用户参数覆盖默认参数
        Map<String, String> localParamMap = new LinkedHashMap<>();
        if (paramMap != null) {
            localParamMap.putAll(paramMap);
        }
        for (String defaultKey : S_DEFAULT_PARAM_MAP.keySet()) {
            if (localParamMap.containsKey(defaultKey)) {
                continue;
            }
            String defaultValue = S_DEFAULT_PARAM_MAP.get(defaultKey);
            if (defaultValue == null) {
                continue;
            }
            localParamMap.put(defaultKey, defaultValue);
        }
        // 2026-07-17 WkHtmlToPdfUtil优化
        // 构建命令列表（List形式避免路径空格截断和命令注入）
        List<String> commandList = new ArrayList<>();
        commandList.add(homeUrl);
        for (Map.Entry<String, String> entry : localParamMap.entrySet()) {
            String value = entry.getValue();
            if (value == null) {
                continue;
            }
            commandList.add(entry.getKey());
            if (!"".equals(value)) {
                commandList.add(value);
            }
        }
        commandList.add(htmlUrl);
        commandList.add(pdfUrl);
        LogUtil.print("WkHtmlToPdfUtil_html2pdf_12", commandList);
        // 2026-07-30 解决退出码非0时无法定位失败原因的问题：原Redirect.DISCARD丢弃了stderr，改为重定向到临时文件以保留错误详情
        File stderrFile = null;
        Process process = null;
        try {
            stderrFile = File.createTempFile("wk_stderr_", ".txt");
            ProcessBuilder processBuilder = new ProcessBuilder(commandList);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            processBuilder.redirectError(stderrFile);
            process = processBuilder.start();
            // 2026-07-17 WkHtmlToPdfUtil优化
            // 主线程阻塞等待进程结束（超时时间可配置）
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                String timeoutText = "超时秒数=" + timeoutSeconds;
                String htmlText13 = "输入HTML=" + htmlUrl;
                String pdfText13 = "输出PDF=" + pdfUrl;
                LogUtil.error("WkHtmlToPdfUtil_html2pdf_13", "wkhtmltopdf执行超时", timeoutText, htmlText13, pdfText13);
                p_destroyProcessGracefully(process);
                process = null;
                return null;
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String stderrContent = p_readStderrFile(stderrFile);
                String errorType = p_extractWkErrorType(stderrContent);
                String errorMeaning = p_translateWkErrorType(errorType);
                String exitCodeText = "退出码=" + exitCode;
                String errorTypeText = "错误类型=" + errorType + errorMeaning;
                String htmlText14 = "输入HTML=" + htmlUrl;
                String pdfText14 = "输出PDF=" + pdfUrl;
                LogUtil.error("WkHtmlToPdfUtil_html2pdf_14", "wkhtmltopdf转换失败", exitCodeText, errorTypeText, htmlText14, pdfText14, stderrContent);
                p_printHtmlResourceList(htmlUrl);
                return null;
            }
            // 2026-07-17 WkHtmlToPdfUtil优化
            // 计算转换耗时并获取输出文件大小
            long costTimeMillis = System.currentTimeMillis() - startTimeMillis;
            long fileSizeBytes = FileUtil.getFileSize(pdfUrl);
            LogUtil.print("WkHtmlToPdfUtil_html2pdf_15", "转换成功", costTimeMillis + "ms", fileSizeBytes + "bytes", commandList);
            return htmlUrl;
        } catch (Throwable throwable) {
            LogUtil.error("WkHtmlToPdfUtil_html2pdf_16", throwable, homeUrl, htmlUrl, pdfUrl, commandList);
            return null;
        } finally {
            if (process != null) {
                p_destroyProcessGracefully(process);
            }
            // 2026-07-30 清理stderr临时文件，避免磁盘残留
            if (stderrFile != null) {
                stderrFile.delete();
            }
        }
    }

    /**
     * 2026-07-17
     * 统一销毁进程及子进程，关闭管道流
     * @param process
     */
    private static void p_destroyProcessGracefully(Process process) {
        if (process == null) {
            return;
        }
        // 2026-07-17 WkHtmlToPdfUtil优化
        // 1. 关闭 JVM 与进程交互的三个管道流（非常关键，防止 FD 泄漏）
        // 优化关闭顺序：先关闭输入流，再关闭输出流，避免子进程阻塞
        p_closeQuietly(process.getInputStream());
        p_closeQuietly(process.getErrorStream());
        p_closeQuietly(process.getOutputStream());
        // 2. 尝试优雅地终止主进程及所有子孙进程
        try {
            // 获取进程及其所有子孙进程的 Handle（JDK 9+ 特性）
            ProcessHandle handle = process.toHandle();
            // 递归销毁所有子进程
            Iterator<ProcessHandle> descendantIterator = handle.descendants().iterator();
            while (descendantIterator.hasNext()) {
                ProcessHandle descendant = descendantIterator.next();
                if (descendant.isAlive()) {
                    descendant.destroy(); // 尝试温和销毁
                }
            }
            // 销毁主进程
            if (process.isAlive()) {
                process.destroy();
            }
            // 3. 给进程 2 秒钟的时间优雅退出
            if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                // 4. 如果超时仍未退出，则使用 OS 级别信号强制杀死 (Kill -9 效果)
                Iterator<ProcessHandle> forciblyIterator = handle.descendants().iterator();
                while (forciblyIterator.hasNext()) {
                    ProcessHandle descendant = forciblyIterator.next();
                    if (descendant.isAlive()) {
                        descendant.destroyForcibly();
                    }
                }
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        } catch (Throwable throwable) {
            // 如果等待期间当前线程被中断，立刻强制杀死
            process.destroyForcibly();
            Thread.currentThread().interrupt(); // 保持中断状态
        }
    }

    /**
     * 2026-07-30 读取wkhtmltopdf进程的stderr错误输出（如字体缺失、资源加载失败等），截断防止日志溢出
     * 2026-08-28 编码整改-过滤进度噪音行并改为保头保尾截断
     * @param stderrFile
     * @return
     */
    private static String p_readStderrFile(java.io.File stderrFile) {
        if (stderrFile == null || !stderrFile.exists()) {
            return "";
        }
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(stderrFile.toPath());
            String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).trim();
            content = p_filterProgressLines(content);
            if (content.length() > 2000) {
                String head = content.substring(0, 400);
                String tail = content.substring(content.length() - 1400);
                content = head + "\n...(中间省略)...\n" + tail;
            }
            return content;
        } catch (Throwable throwable) {
            return "read stderr failed: " + throwable.getMessage();
        }
    }

    /**
     * 2026-08-28 编码整改-过滤stderr进度噪音行
     * @param content
     * @return
     */
    private static String p_filterProgressLines(String content) {
        String[] lineArray = StringUtil.split(content, "\n");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < lineArray.length; i++) {
            String line = StringUtil.trim(lineArray[i]);
            if (StringUtil.isBlank(line)) {
                continue;
            }
            boolean progressFlag = line.startsWith("[") && line.indexOf('%') > -1;
            boolean stageFlag = StringUtil.startsWith(line, new String[]{"Loading pages", "Printing pages", "Object ", "Preparing", "Page "});
            if (progressFlag || stageFlag) {
                continue;
            }
            stringBuilder.append(line).append("\n");
        }
        String pureContent = stringBuilder.toString().trim();
        return pureContent;
    }

    /**
     * 2026-08-28 编码整改-从stderr提取wkhtmltopdf退出错误类型
     * @param stderrContent
     * @return
     */
    private static String p_extractWkErrorType(String stderrContent) {
        if (StringUtil.isBlank(stderrContent)) {
            return "";
        }
        int index = stderrContent.indexOf("network error: ");
        if (index < 0) {
            return "";
        }
        String remain = stderrContent.substring(index + "network error: ".length()).trim();
        StringBuilder typeBuilder = new StringBuilder();
        for (int i = 0; i < remain.length(); i++) {
            char c = remain.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                typeBuilder.append(c);
                continue;
            }
            break;
        }
        String errorType = typeBuilder.toString();
        return errorType;
    }

    /**
     * 2026-08-28 编码整改-翻译wkhtmltopdf错误类型
     * @param errorType
     * @return
     */
    private static String p_translateWkErrorType(String errorType) {
        if (StringUtil.isBlank(errorType)) {
            return "(未能识别错误类型，见原始输出)";
        }
        if (StringUtil.equals(errorType, "ContentNotFoundError")) {
            return "(HTML引用的图片或CSS等资源返回404，检查资源地址是否存在)";
        }
        if (StringUtil.equals(errorType, "RemoteHostClosedError")) {
            return "(资源服务端在响应中途断开连接，常见于图片接口回源异常或同文件并发读写)";
        }
        if (StringUtil.equals(errorType, "ConnectionRefusedError")) {
            return "(目标地址端口无服务监听)";
        }
        if (StringUtil.equals(errorType, "HostNotFoundError")) {
            return "(域名解析失败)";
        }
        if (StringUtil.equals(errorType, "SslHandshakeFailedError")) {
            return "(HTTPS证书握手失败)";
        }
        if (StringUtil.equals(errorType, "AuthenticationRequiredError")) {
            return "(资源要求登录认证)";
        }
        return "(未收录的错误类型，见原始输出)";
    }

    /**
     * 2026-08-28 编码整改-失败时打印HTML资源引用清单
     * @param htmlUrl
     */
    private static void p_printHtmlResourceList(String htmlUrl) {
        if (StringUtil.isBlank(htmlUrl)) {
            return;
        }
        try {
            String htmlContent = FileUtil.fileReadText(htmlUrl);
            if (StringUtil.isBlank(htmlContent)) {
                return;
            }
            List<String> urlList = new ArrayList<>();
            p_collectUrlByTag(htmlContent, "src=\"", urlList);
            p_collectUrlByTag(htmlContent, "src='", urlList);
            p_collectUrlByTag(htmlContent, "href=\"", urlList);
            p_collectUrlByTag(htmlContent, "href='", urlList);
            List<String> pureUrlList = new ArrayList<>();
            for (int i = 0; i < urlList.size(); i++) {
                String url = StringUtil.trim(urlList.get(i));
                if (StringUtil.isBlank(url)) {
                    continue;
                }
                if (pureUrlList.contains(url)) {
                    continue;
                }
                pureUrlList.add(url);
            }
            if (pureUrlList.isEmpty()) {
                LogUtil.warn("WkHtmlToPdfUtil_p_printHtmlResourceList_1", "失败HTML无src与href资源引用", htmlUrl);
                return;
            }
            // 输出清单(最多20条)
            StringBuilder stringBuilder = new StringBuilder("共" + pureUrlList.size() + "个引用");
            int maxCount = pureUrlList.size() < 20 ? pureUrlList.size() : 20;
            for (int i = 0; i < maxCount; i++) {
                int indexNum = i + 1;
                stringBuilder.append("\n").append(indexNum).append(". ").append(pureUrlList.get(i));
            }
            if (pureUrlList.size() > 20) {
                stringBuilder.append("\n...其余").append(pureUrlList.size() - 20).append("个省略");
            }
            String listText = stringBuilder.toString();
            LogUtil.warn("WkHtmlToPdfUtil_p_printHtmlResourceList_1", "失败HTML资源引用清单", htmlUrl, listText);
        } catch (Throwable throwable) {
            LogUtil.warn("WkHtmlToPdfUtil_p_printHtmlResourceList_1", "读取失败HTML资源清单异常", htmlUrl);
        }
    }

    /**
     * 2026-08-28 编码整改-按标签提取HTML资源地址
     * @param htmlContent
     * @param tag
     * @param urlList
     */
    private static void p_collectUrlByTag(String htmlContent, String tag, List<String> urlList) {
        String quote = tag.endsWith("\"") ? "\"" : "'";
        int fromIndex = 0;
        while (true) {
            int tagIndex = htmlContent.indexOf(tag, fromIndex);
            if (tagIndex < 0) {
                return;
            }
            int start = tagIndex + tag.length();
            int end = htmlContent.indexOf(quote, start);
            if (end < 0) {
                return;
            }
            urlList.add(htmlContent.substring(start, end));
            fromIndex = end + 1;
        }
    }

    /**
     * 2026-07-17 静默关闭流的辅助方法
     */
    private static void p_closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable throwable) {
                // 静默忽略关闭流时的异常
            }
        }
    }

}
