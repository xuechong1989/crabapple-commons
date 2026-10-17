package xc.crabapple.core.file.pdf.pdfa;

import com.spire.pdf.PdfConformanceLevel;
import com.spire.pdf.PdfDocument;
import com.spire.pdf.conversion.PdfStandardsConverter;
import com.spire.pdf.license.LicenseProvider;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.url.UrlUtil;

/**
 * 2023-07-18 spire-pdf版本: v9.2.5
 * 2025-07-07 spire-pdf版本：10.6.2
 *
 * 可以用2024-07-14号（含）前的版本，spire.pdf-10.6.2（含）以前的版本
 *
 * 基于冰蓝科技 spire.PDF 收费版 进行开发，本地目录： D:/docs/pdfa/spire.PDF
 *
 * 官方购买地址： https://www.e-iceblue.cn/Buy/Spire-PDF-JAVA.html
 * 官方更新记录： https://www.e-iceblue.cn/spire-pdf-for-java-news.html
 * 在线验证PDF/A文件：
 * https://online.visual-paradigm.com/cn/online-pdf-editor/pdfa-validator/
 * https://avepdf.com/zh/pdfa-validation
 */
public final class PdfSpireUtil {

    private static boolean p_initialized = false;

    /**
     * 2023-07-18
     */
    private static final void p_init(){
        if(p_initialized){
            return;
        }
        p_initialized = true;
        // 有效期限 2023-07-14 --- 2024-07-14
        // 授权码：请从官方购买后填入，并解开上方注释
        // LicenseProvider.setLicenseKey("your-license-key");
    }

    /**
     * 2023-07-18
     * 2025-07-07 参数动态判断
     * 判断pdf文件是否为pdf/a通过字节
     * https://www.evget.com/article/2018/1/18/27683.html
     * @param param pdf文件的路径或者内容（byte[]），程序会自动判断和解析。
     */
    public static final String getPdfInfo(final Object param){
        if(param == null){
            return "no";
        }
        if(param instanceof String){
            final String pdfUrl = (String) param;
            final String pdfInfo = p_getPdfInfo(null, pdfUrl, null);
            return pdfInfo;
        }
        if(param instanceof byte[]){
            final byte[] bytes = (byte[]) param;
            final String pdfInfo = p_getPdfInfo(null, null, bytes);
            return pdfInfo;
        }
        return "no";
    }

    /**
     * 2023-07-18
     * 2025-07-07 添加参数，动态判断。
     * 判断pdf文件是否为PDF/A格式。
     * 返回值格式：
     *   若返回字符串：no，则不是PDF/A格式的文件。
     *   若返回字符串：以pdf_a_开头，如：pdf_a_2_b，则就是PDF/A格式文件。
     * https://www.evget.com/article/2018/1/18/27683.html
     */
    private static final String p_getPdfInfo(final PdfDocument pdfDocument, final String pdfUrl, final byte[] bytes){
        p_init();
        //
        PdfDocument pdfDocumentLocal = pdfDocument;
        if(pdfDocumentLocal == null){
            try{
                String pdfUrlLocal = UrlUtil.getStandardPath(pdfUrl);
                if(!StringUtil.isBlank(pdfUrlLocal)){
                    pdfDocumentLocal = new PdfDocument();
                    pdfDocumentLocal.loadFromFile(pdfUrlLocal);
                }else if(!CollectionUtil.isBlank(bytes)){
                    pdfDocumentLocal = new PdfDocument();
                    pdfDocumentLocal.loadFromBytes(bytes);
                }else{
                    LogUtil.error("PdfSpireUtil_p_getPdfInfo_1", "参数错误", pdfUrl);
                    return "no";
                }
            }catch(Throwable throwable){
                LogUtil.error("PdfSpireUtil_p_getPdfInfo_t1", throwable, "加载PDF文件失败", pdfUrl);
                return "no";
            }
        }
        //
        final PdfConformanceLevel pdfConformanceLevel = pdfDocumentLocal.getConformance();
        if(pdfConformanceLevel == null){
            return "no";
        }
        //
        String name = pdfConformanceLevel.name();
        name = StringUtil.toLowerCase(name);
        if("none".equals(name)){
            return "no";
        }
        return name;
    }

    /**
     * 2023-07-18
     * PDF文件转换为标准PDF/A
     * @param fromUrl
     * @param toUrl
     */
    public static final boolean pdf2pdfa(final String fromUrl, final String toUrl){
        final String type = "2b";  // 默认使用PDF/A的2B级别
        final boolean success = pdf2pdfa(fromUrl, toUrl, type);
        return success;
    }

    /**
     * 2023-07-18
     * PDF文件转换为标准PDF/A
     * 首选B级：若仅需保证文档视觉长期一致（如扫描图像、简单公文），B级（如PDF/A-2b）是国内档案馆最常接受的级别。
     * @param fromUrl
     * @param toUrl
     * @param type PDF/A更详细的等级，如 1a, 2a, 3a, 1b, 2b, 3b
     */
    public static final boolean pdf2pdfa(final String fromUrl, final String toUrl, final String type){
        p_init();
        //
        if(!FileUtil.isRealFile(fromUrl, true)){
            LogUtil.error("PdfSpireUtil_pdf2pdfa_44", "源文件不存在或大小为0", fromUrl, toUrl, type);
            return false;
        }
        //
        FileUtil.fileDelete(toUrl);
        PdfStandardsConverter converter = new PdfStandardsConverter(fromUrl);
        try{
            if("1a".equals(type)){
                converter.toPdfA1A(toUrl);
                if(FileUtil.isRealFile(toUrl, true)){
                    return true;
                }
                return false;
            }
            if("2a".equals(type)){
                converter.toPdfA2A(toUrl);
                if(FileUtil.isRealFile(toUrl, true)){
                    return true;
                }
                return false;
            }
            if("3a".equals(type)){
                converter.toPdfA3A(toUrl);
                if(FileUtil.isRealFile(toUrl, true)){
                    return true;
                }
                return false;
            }
            if("1b".equals(type)){
                converter.toPdfA1B(toUrl);
                if(FileUtil.isRealFile(toUrl, true)){
                    return true;
                }
                return false;
            }
            if("2b".equals(type)){
                converter.toPdfA2B(toUrl);
                if(FileUtil.isRealFile(toUrl, true)){
                    return true;
                }
                return false;
            }
            if("3b".equals(type)){
                converter.toPdfA3B(toUrl);
                if(FileUtil.isRealFile(toUrl, true)){
                    return true;
                }
                return false;
            }
            LogUtil.error("PdfSpireUtil_pdf2pdfa_1", "未知的类型", type, fromUrl, toUrl);
            return false;
        }catch(Throwable throwable){
            LogUtil.error("PdfSpireUtil_pdf2pdfa_t1", throwable, "转PDF/A失败", type, fromUrl, toUrl);
            return false;
        }
    }

}