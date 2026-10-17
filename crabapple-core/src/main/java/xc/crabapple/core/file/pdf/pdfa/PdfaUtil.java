package xc.crabapple.core.file.pdf.pdfa;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.file.pdf.PdfUtil;
import xc.crabapple.core.file.pdf.playwright.PlaywrightConfigBean;
import xc.crabapple.core.file.pdf.playwright.PlaywrightUtil;
import xc.crabapple.core.file.pdf.wkhtmltopdf.WkHtmlToPdfConfigBean;
import xc.crabapple.core.file.pdf.wkhtmltopdf.WkHtmlToPdfUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.url.UrlUtil;

import java.util.Map;

/**
 * 2022-05-31
 * 2025-07-07 内置spire类，使业务代码对spire类不可见。
 * PDF/A格式文件介绍：D:/docs/pdfa/PDFA格式文件介绍-2023-03-03.docx
 *
 * pdf/a格式相关的处理解决方案：
 * 1 pdfbox - 国外-免费-开源
 *   https://pdfbox.apache.org/
 *
 * 2 spire.pdf - 国内-收费
 *   https://www.e-iceblue.cn/
 *   https://www.e-iceblue.com/Buy/Spire.PDF.html
 *
 * 3 veraPDF / pdftron - 国外-收费
 *   https://www.pdftron.com
 *
 * 4 aspose - 国外-收费
 *   https://www.aspose.com/
 *
 * 在线验证PDF/A文件
 * https://online.visual-paradigm.com/cn/online-pdf-editor/pdfa-validator/
 * https://avepdf.com/zh/pdfa-validation
 *
 */
public final class PdfaUtil {

    /**
     * 2025-07-07
     */
    public static final void init(){

    }

    /**
     * 2022-05-28
     * 2022-09-21 增加模板为空的情况
     * 2025-07-07 真正引入spire类
     * 2026-08-03 playwright替代wkhtmltopdf 替换WkHtmlToPdfUtil为PlaywrightUtil
     * 2026-08-06 playwright替代wkhtmltopdf 支持切换wk与playwright配置
     * @param playwrightConfigBean 配置对象
     */
    public static final String html2pdfa(final PlaywrightConfigBean playwrightConfigBean) {
        // 按引擎配置执行HTML转PDF
        String htmlUrl = p_html2pdf(playwrightConfigBean);
        // 2026-08-10 转换失败时返回null，避免null被路径标准化为根目录
        if (StringUtil.isBlank(htmlUrl)) {
            return null;
        }
        htmlUrl = UrlUtil.getStandardPath(htmlUrl);
        String pdfUrl = playwrightConfigBean.getPdfUrl();
        pdfUrl = UrlUtil.getStandardPath(pdfUrl);
        String pdfName = FileUtil.getFileName(pdfUrl, false);
        String pdfPath = UrlUtil.getPathByUrl(pdfUrl);
        String tmpStr = RandomUtil.getStrWithStamp(16);
        String tmpPdfUrl = pdfPath + "/" + pdfName + "_" + tmpStr + ".pdf";
        FileUtil.rename(pdfUrl, tmpPdfUrl);
        final boolean success = PdfSpireUtil.pdf2pdfa(tmpPdfUrl, pdfUrl);
        if(success){
            FileUtil.delete(tmpPdfUrl);
        }
        // 2026-09-24 检测PDF格式文件是否标准规范与合法
        // 系统内部转换出的pdf同样做标准合法性检测：不标准按转换失败处理返回null，上层走既有失败反馈链立刻告知用户
        String standardReason = PdfUtil.checkStandard(pdfUrl);
        if (standardReason != null) {
            long finalSizeBytes = FileUtil.getFileSize(pdfUrl);
            LogUtil.error("PdfaUtil_html2pdfa_2", "转换出的PDF不是标准、合法的文件，本次转换按失败处理", standardReason, pdfUrl, htmlUrl, finalSizeBytes);
            return null;
        }
        return htmlUrl;
    }

    /**
     * 2026-08-06 playwright替代wkhtmltopdf
     * 按引擎配置执行HTML转PDF
     * @param playwrightConfigBean 配置对象
     * @return 成功返回htmlUrl，失败返回null
     */
    private static final String p_html2pdf(final PlaywrightConfigBean playwrightConfigBean) {
        // 获取业务代码传入的引擎
        String engine = playwrightConfigBean.getEngine();
        String pdfUrl = playwrightConfigBean.getPdfUrl();
        LogUtil.print("PdfaUtil_html2pdfa_1", "HTML转PDF引擎", engine, pdfUrl);
        // 按引擎转换
        String htmlUrl;
        if ("wkhtmltopdf".equals(engine)) {
            htmlUrl = p_html2pdfByWk(playwrightConfigBean);
        } else {
            htmlUrl = PlaywrightUtil.html2pdf(playwrightConfigBean);
        }
        return htmlUrl;
    }

    /**
     * 2026-08-06 playwright替代wkhtmltopdf
     * 使用wkhtmltopdf引擎转换，配置字段与PlaywrightConfigBean完全一致，逐字段拷贝
     * @param playwrightConfigBean 配置对象
     * @return 成功返回htmlUrl，失败返回null
     */
    private static final String p_html2pdfByWk(final PlaywrightConfigBean playwrightConfigBean) {
        final String homeUrl = playwrightConfigBean.getHomeUrl();
        String pdfUrl = playwrightConfigBean.getPdfUrl();
        if (StringUtil.isBlank(homeUrl)) {
            LogUtil.error("PdfaUtil_html2pdfByWk_1", "wkhtmltopdf引擎未配置wkhtmltopdf.bin.url", pdfUrl);
            return null;
        }
        // 获取playwright配置字段
        String templateHtmlUrl = playwrightConfigBean.getTemplateHtmlUrl();
        String htmlReplace = playwrightConfigBean.getHtmlReplace();
        String htmlUrl = playwrightConfigBean.getHtmlUrl();
        String htmlText = playwrightConfigBean.getHtmlText();
        String cacheHtmlUrl = playwrightConfigBean.getCacheHtmlUrl();
        int timeoutSeconds = playwrightConfigBean.getTimeoutSeconds();
        Map<String, String> paramMap = playwrightConfigBean.getParamMap();
        // 创建wkhtmltopdf配置
        WkHtmlToPdfConfigBean wkHtmlToPdfConfigBean = new WkHtmlToPdfConfigBean();
        wkHtmlToPdfConfigBean.setHomeUrl(homeUrl);
        wkHtmlToPdfConfigBean.setTemplateHtmlUrl(templateHtmlUrl);
        wkHtmlToPdfConfigBean.setHtmlReplace(htmlReplace);
        // 2026-08-10 htmlUrl为空时保持null，避免setter将null归一化为根目录
        if (!StringUtil.isBlank(htmlUrl)) {
            wkHtmlToPdfConfigBean.setHtmlUrl(htmlUrl);
        }
        wkHtmlToPdfConfigBean.setHtmlText(htmlText);
        wkHtmlToPdfConfigBean.setCacheHtmlUrl(cacheHtmlUrl);
        wkHtmlToPdfConfigBean.setPdfUrl(pdfUrl);
        wkHtmlToPdfConfigBean.setTimeoutSeconds(timeoutSeconds);
        // 拷贝wkhtmltopdf参数
        if (paramMap != null) {
            Map<String, String> wkParamMap = wkHtmlToPdfConfigBean.getParamMap();
            wkParamMap.putAll(paramMap);
        }
        final String resultHtmlUrl = WkHtmlToPdfUtil.html2pdf(wkHtmlToPdfConfigBean);
        return resultHtmlUrl;
    }

    /**
     * 2025-07-07
     * @param fromUrl
     * @param toUrl
     * @return
     */
    public static final boolean pdf2pdfa(final String fromUrl, final String toUrl){
        final boolean success = PdfSpireUtil.pdf2pdfa(fromUrl, toUrl);
        return success;
    }

    /**
     * 2025-07-07
     * @param param pdf文件的路径或者内容（byte[]），程序会自动判断和解析。
     * @return
     */
    public static final String getPdfInfo(final Object param){
        final String pdfInfo = PdfSpireUtil.getPdfInfo(param);
        return pdfInfo;
    }

}
