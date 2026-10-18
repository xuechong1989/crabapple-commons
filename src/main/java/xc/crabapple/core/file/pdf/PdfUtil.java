package xc.crabapple.core.file.pdf;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.url.UrlUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 2018-10-27 1.1.0
 * html与pdf互转
 * pdfbox官网 https://pdfbox.apache.org/download.html
 */
public final class PdfUtil {

    /**
     * 2022-12-22
     * 计算指定PDF文件的指定页的尺寸
     * @param pdfUrl    PDF文件绝对路径
     * @param pageIndex 页码索引，从1开始
     * @return
     */
    public static final Rectangle getRectangle(final String pdfUrl, final int pageIndex) {
        byte[] bytes = FileUtil.file2bytes(pdfUrl);
        if (CollectionUtil.isBlank(bytes)) {
            LogUtil.error("PdfUtil_getRectangle_1", "pdf文件内容为空", pdfUrl, pageIndex);
            return null;
        }
        PdfReader pdfReader = null;
        try{
            pdfReader = new PdfReader(bytes);
            Rectangle rectangle = pdfReader.getPageSize(pageIndex);
            return rectangle;
        }catch(Throwable throwable){
            LogUtil.error("PdfUtil_getRectangle_e1", throwable, pdfUrl, pageIndex);
            return null;
        }finally {
            if(pdfReader != null){
                pdfReader.close();
            }
        }
    }

    /**
     * 2020-08-06 6.4.0
     * 2024-12-04 代码重构
     * 指定文件名或路径是否为文档格式
     * @return
     */
    public static final boolean isPdf(final String nameOrUrl) {
        final boolean flag = FileUtil.isPdf(nameOrUrl);
        return flag;
    }

    /**
     * 2020-08-19 6.5.0
     * 2024-12-04 代码重构
     * 指定文件格式是否为文档格式
     * @param fileType 文件类型（不带点的后缀），如pdf
     */
    public static final boolean isPdfByFileType(final String fileType) {
        boolean flag = FileUtil.isPdfByFileType(fileType);
        return flag;
    }

    /**
     * 2020-05-19 6.0.0
     * html文件转换为pdf文件
     */
    public static final void htmlFile2pdf(final String htmlUrl, final String pdfUrl) {
        String htmlStr = FileUtil.fileReadText(htmlUrl);
        htmlStr2pdf(htmlStr, pdfUrl, null);
    }

    /**
     * 2020-05-19 6.0.0
     * html文件转换为pdf文件
     */
    public static final void htmlFile2pdf(final String htmlUrl, final String pdfUrl, final ConverterProperties converterProperties) {
        String htmlStr = FileUtil.fileReadText(htmlUrl);
        htmlStr2pdf(htmlStr, pdfUrl, converterProperties);
    }

    /**
     * 2020-05-19 6.0.0
     * 标准html格式字符串转换为pdf文件
     */
    public static final void htmlStr2pdf(final String htmlStr, final String pdfUrl) {
        //
        htmlStr2pdf(htmlStr, pdfUrl, null);
    }

    /**
     * 2020-05-19 6.0.0
     * 标准html格式字符串转换为pdf文件
     */
    public static final void htmlStr2pdf(final String htmlStr, final String pdfUrl, final ConverterProperties converterProperties) {
        String htmlStrLocal = StringUtil.getPureStr(htmlStr);
        htmlStrLocal = StringUtil.trim(htmlStrLocal);
        File pdfFile = new File(pdfUrl);
        String pdfPath = UrlUtil.getPathByLevel(pdfUrl, 1);
        FileUtil.dirMake(pdfPath);
        FileUtil.fileDelete(pdfFile);
        OutputStream outputStream = null;
        com.itextpdf.kernel.pdf.PdfWriter pdfWriter = null;
        try{
            outputStream = new FileOutputStream(pdfFile);
            pdfWriter = new com.itextpdf.kernel.pdf.PdfWriter(outputStream);
            if (converterProperties == null) {
                HtmlConverter.convertToPdf(htmlStrLocal, pdfWriter);
            } else {
                HtmlConverter.convertToPdf(htmlStrLocal, pdfWriter, converterProperties);
            }
        }catch(Throwable throwable){
            LogUtil.error("PdfUtil_htmlStr2pdf_t1", throwable, htmlStrLocal, pdfUrl);
        } finally {
            IOUtil.close(pdfWriter, outputStream);
        }
    }

    /**
     * 获取设置了中文ttf字体的converterProperties
     */
    private static final ConverterProperties p_getCnConverterProperties() {
        ConverterProperties converterProperties = new ConverterProperties();
        //设置中文字体，ttf文件夹下SimSum-01和Dengb分别支持细字体和粗字体，缺一不可
        FontProvider fontProvider = new FontProvider();
        fontProvider.addDirectory("ttf/");
        converterProperties.setFontProvider(fontProvider);
        return converterProperties;
    }

    /**
     * 2022-08-12
     */
    public static final void pdfs2texts(final String pdfPath, final String targetPath) {
        if (StringUtil.isBlank(pdfPath)) {
            LogUtil.error("PdfUtil_pdfs2texts_1", "pdfPath_is_null");
            return;
        }
        if (!FileUtil.exists(pdfPath)) {
            LogUtil.error("PdfUtil_pdfs2texts_2", "pdfPath_is_not_exist");
            return;
        }
        if (StringUtil.isBlank(targetPath)) {
            LogUtil.error("PdfUtil_pdfs2texts_3", "targetPath_is_null");
            return;
        }
        //
        String pdfPathLocal = UrlUtil.getStandardPath(pdfPath);
        String targetPathLocal = UrlUtil.getStandardPath(targetPath);
        FileUtil.dirMake(targetPathLocal);
        //
        Map<String, List<String>> map = FileUtil.getDirChildren(pdfPathLocal);
        List<String> list = map.get("file");
        if (CollectionUtil.isBlank(list)) {
            LogUtil.error("PdfUtil_pdfs2texts_4", "list_is_null");
            return;
        }
        //
        for (String pdfUrl : list) {
            String filename = FileUtil.getFileName(pdfUrl, false);
            String targetUrl = targetPathLocal + "/" + filename + ".txt";
            if (FileUtil.exists(targetUrl)) {
                continue;
            }
            pdf2text(pdfUrl, targetUrl);
        }
    }

    /**
     * 2022-08-12
     * 把指定的pdf文件转为文本后输出到指定文本文件中
     */
    public static final void pdf2text(final String pdfUrl, final String targetUrl) {
        String content = pdf2text(pdfUrl);
        FileUtil.fileDelete(targetUrl);
        FileUtil.fileWriteNew(targetUrl, content, true);
    }

    /**
     * 2018-07-25 1.1.0
     * 从pdf文件中提取文本内容
     *
     * @param pdfUrl pdf文件地址，如D:/data/cache/tmp/The_Economist_2016-11-05.pdf
     */
    public static final String pdf2text(final String pdfUrl) {
        String pdfUrlLocal = UrlUtil.getStandardPath(pdfUrl);
        File file = new File(pdfUrlLocal);
        PDDocument document = null;
        try{
            document = Loader.loadPDF(file);
            document.getClass();
            // 是否加密
//            if(document.isEncrypted()) {
//                return null;
//            }
            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            stripper.setSortByPosition(true);
            PDFTextStripper tStripper = new PDFTextStripper();
            String pdfFileInText = tStripper.getText(document);
            String[] lines = pdfFileInText.split("\\r?\\n");
            StringBuilder stringBuilder = new StringBuilder();
            for (String line : lines) {
                stringBuilder.append(line).append("\n");
            }
            String result = stringBuilder.toString();
            return result;
        }catch(Throwable throwable){
            LogUtil.error("PdfUtil_pdf2text_1", throwable, pdfUrlLocal);
            return null;
        } finally {
            IOUtil.close(document);
        }
    }

    /**
     * 2018-10-27 1.1.0
     * pdf文件转多张png图片，每页pdf对应一张png图片文件，全部页数
     *
     * @param pdfUrl  pdf完整路径
     * @param imgPath 图片存放的文件夹
     * @param dpi     dpi越大转换后越清晰，相对转换速度越慢，默认105
     */
    public static final List<String> pdf2png(final String pdfUrl, final String imgPath, final int dpi) {
        if (StringUtil.isBlank(pdfUrl)) {
            LogUtil.warn("xc_crabapple_PdfUtil_pdf2png_1", "pdfUrl参数为空, pdfUrl=" + pdfUrl);
            return null;
        }
        if (!pdfUrl.endsWith(".pdf")) {
            LogUtil.warn("xc_crabapple_PdfUtil_pdf2png_2", "pdfUrl参数文件格式错误, pdfUrl=" + pdfUrl);
            return null;
        }
        if (!FileUtil.exists(pdfUrl)) {
            LogUtil.warn("xc_crabapple_PdfUtil_pdf2png_3", "pdfUrl文件不存在, pdfUrl=" + pdfUrl);
            return null;
        }
        int dpiLocal = dpi;
        if (dpiLocal <= 0) {
            dpiLocal = 105;
        }
        String imgPathLocal = UrlUtil.getStandardPath(imgPath);
        File pdfFile = new File(pdfUrl);
        String pdfParentPath = pdfFile.getParent();
        String pdfName = FileUtil.getFileName(pdfFile, false);
        if (StringUtil.isBlank(imgPathLocal)) {
            imgPathLocal = pdfParentPath + "/" + pdfName;  //获取图片存放的文件夹路径
        } else {
            imgPathLocal = imgPathLocal + "/" + pdfName;
        }
        imgPathLocal = UrlUtil.getStandardPath(imgPathLocal);
        FileUtil.dirMake(imgPathLocal);
        PDDocument pdDocument = null;
        PdfReader pdfReader = null;
        try{
            pdDocument = Loader.loadPDF(pdfFile);
            PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
            pdfReader = new PdfReader(pdfUrl);
            int pages = pdfReader.getNumberOfPages();
            StringBuilder stringBuilder = null;
            List<String> list = new ArrayList<>();
            String random = RandomUtil.getStrWithStamp(32);
            for (int i = 0; i < pages; i++) {
                String imgFilePathPrefix = imgPathLocal + "/" + pdfName;
                stringBuilder = new StringBuilder();
                stringBuilder.append(imgFilePathPrefix);
                stringBuilder.append("_");
                stringBuilder.append(random);
                stringBuilder.append("_");
                stringBuilder.append(i + 1);
                stringBuilder.append(".png");
                String imgUrl = stringBuilder.toString();
                list.add(imgUrl);
                File imgFile = new File(imgUrl);
                BufferedImage image = pdfRenderer.renderImageWithDPI(i, dpiLocal);
                ImageIO.write(image, "png", imgFile);
            }
            if (CollectionUtil.isBlank(list)) {
                return null;
            }
            return list;
        }catch(Throwable throwable){
            LogUtil.error("PdfUtil_pdf2png_t4", throwable, pdfUrl, imgPathLocal, dpiLocal);
            return null;
        } finally {
            if(pdfReader != null){
                pdfReader.close();
            }
            IOUtil.close(pdDocument);
        }
    }

    /**
     * 2024-05-16
     * @param pdfUrl
     * @return
     */
    public static final List<String> pdf2jpg(final String pdfUrl) {
        String targetPath = UrlUtil.getPathByUrl(pdfUrl);
        List<String> list = pdf2jpg(pdfUrl, targetPath);
        return list;
    }

    /**
     * 2018-10-27 1.1.0
     * 2024-05-16 添加参数targetPath
     * pdf转图片
     * @param pdfUrl
     * @param targetPath 输出到指定目录
     */
    public static final List<String> pdf2jpg(final String pdfUrl, final String targetPath) {
        List<String> list = new ArrayList<>();
        if (StringUtil.isBlank(pdfUrl)) {
            LogUtil.error("PdfUtil_pdf2jpg_1", "pdfUrl为空");
            return list;
        }
        String pdfUrlLocal = UrlUtil.getStandardPath(pdfUrl);
        if (!pdfUrlLocal.endsWith(".pdf")) {
            LogUtil.error("PdfUtil_pdf2jpg_2", "pdfUrl不是pdf格式文件");
            return list;
        }
        //
        String targetPathLocal = UrlUtil.getStandardPath(targetPath);
        String pdfName = FileUtil.getFileName(pdfUrlLocal, false);
        //经过测试,dpi为96,100,105,120,150,200中,105显示效果较为清晰,体积稳定,dpi越高图片体积越大，一般电脑显示分辨率为96
        final float DEFAULT_DPI = 150;
        PDDocument pdDocument = null;
        try{
            //图像合并使用参数
//            int[] singleImgRGB; //保存一张图片中的RGB数据
            int shiftHeight = 0;
            //保存每张图片的像素值
//            BufferedImage imageResult = null;
            //利用PdfBox生成图像
            File file = new File(pdfUrlLocal);
            pdDocument = Loader.loadPDF(file);
            PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
            //循环每个页码
            final int len = pdDocument.getNumberOfPages();
            String random = RandomUtil.getStrWithStamp(32);
            String imgPathName = pdfName + "_" + len + "_" + random;
            String childrenPath = targetPathLocal + "/" + imgPathName;
            FileUtil.dirMake(childrenPath);
            for (int i = 0; i < len; i++) {
                BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(i, DEFAULT_DPI, ImageType.RGB);
                int imageHeight = bufferedImage.getHeight();
                int imageWidth = bufferedImage.getWidth();
                //计算高度和偏移量，保存每页图片的像素值
                BufferedImage bufferedImage1 = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
                int[] singleImgRGB = bufferedImage.getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
                bufferedImage1.setRGB(0, shiftHeight, imageWidth, imageHeight, singleImgRGB, 0, imageWidth);
                String url = childrenPath + "/" + i + ".jpg";
                File outFile = new File(url);
                //若源pdf文件过大，会抛出异常：Maximum supported image dimension is 65500 pixels
                ImageIO.write(bufferedImage1, "jpg", outFile);
                list.add(url);
            }
            return list;
        }catch(Throwable throwable){
            LogUtil.error("PdfUtil_pdf2jpg_t1", throwable, pdfUrlLocal);
            return list;
        } finally {
            IOUtil.close(pdDocument);
        }
    }

    /**
     * 2018-10-27 1.1.0
     * 目前只支持jpg格式
     *
     * @param pathImg
     */
    public static final Integer img2pdfByPath(final String pathImg) {
        Map<String, List<String>> map = FileUtil.getDirChildren(pathImg);
        ArrayList<String> list = (ArrayList) map.get("file");
        for (String url : list) {
            String path = UrlUtil.getPathByUrl(url);
            String name = FileUtil.getFileName(url);
            String fileType = FileUtil.getFileType(url);
            if ("jpg".equals(fileType)) {
                String from = url;
                String to = path + "/" + name + ".pdf";
                try{
                    img2pdf(from, to, false);
                }catch(Throwable throwable){
                    LogUtil.error("PdfUtil_img2pdfByPath_1", throwable, pathImg);
                }
            }
        }
        return 0;
    }

    /**
     * 2018-10-27 1.1.0
     * 目前只支持jpg格式
     *
     * @param pathImg
     * @param isDelImg
     */
    public static final Integer img2pdfByPath(final String pathImg, final boolean isDelImg) {
        Map<String, List<String>> map = FileUtil.getDirChildren(pathImg);
        ArrayList<String> list = (ArrayList) map.get("file");
        for (String url : list) {
            String path = UrlUtil.getPathByUrl(url);
            String name = FileUtil.getFileName(url);
            String fileType = FileUtil.getFileType(url);
            if ("jpg".equals(fileType)) {
                String from = url;
                String to = path + "/" + name + ".pdf";
                try{
                    img2pdf(from, to, isDelImg);
                }catch(Throwable throwable){
                    LogUtil.error("PdfUtil_img2pdfByPath_2", throwable, pathImg, isDelImg);
                }
            }
        }
        return 0;
    }

    /**
     * 2018-10-27 1.1.0
     *
     * @param from
     * @param to
     */
    public static final Integer img2pdf(final String from, final String to) {
        File fFrom = new File(from);
        File fTo = new File(to);
        int result = img2pdf(fFrom, fTo, false);
        return result;
    }

    /**
     * 2018-10-27 1.1.0
     *
     * @param from
     * @param to
     * @param isDelImg
     */
    public static final Integer img2pdf(final String from, final String to, final boolean isDelImg) {
        File fFrom = new File(from);
        File fTo = new File(to);
        int result = img2pdf(fFrom, fTo, isDelImg);
        return result;
    }

    /**
     * 2018-10-27 1.1.0
     *
     * @param fFrom
     * @param fTo
     */
    public static final Integer img2pdf(final File fFrom, final File fTo) {
        int result = img2pdf(fFrom, fTo, false);
        return result;
    }

    /**
     * 2018-05-11 1.1.0
     * 将图片转换成pdf文件
     *
     * @param fFrom    原文件，如d:/abc/aa.jpg
     * @param fTo      转换后的pdf文件，如d:/abc/aa.pdf
     * @param isDelImg 原图片在转换成功后是否删除，true为删除，false为不删除
     */
    public static final Integer img2pdf(final File fFrom, final File fTo, final boolean isDelImg) {
        boolean exist = fFrom.exists();
        if (!exist) {
            return 1;
        }
        Document document = new Document();
        FileOutputStream fileOutputStream = null;
        try{
            fileOutputStream = new FileOutputStream(fTo);
            PdfWriter.getInstance(document, fileOutputStream);
            //添加PDF文档的某些信息，比如作者，主题等等
            document.addAuthor("xc");
            document.addSubject("test pdf.");
            document.setPageSize(PageSize.A4);  //设置文档的大小
            document.open();  //打开文档
            //document.add(new Paragraph("JUST TEST ..."));  //写入一段文字
            String from = fFrom.getAbsolutePath();
            Image image = Image.getInstance(from);  //读取一个图片
            float imageHeight = image.getScaledHeight();
            float imageWidth = image.getScaledWidth();
            int i = 0;
            while (imageHeight > 500 || imageWidth > 500) {
                image.scalePercent(100 - i);
                i++;
                imageHeight = image.getScaledHeight();
                imageWidth = image.getScaledWidth();
            }
            image.setAlignment(Image.ALIGN_CENTER);
            //设置图片的绝对位置
            //image.setAbsolutePosition(0, 0);
            //image.scaleAbsolute(500, 400);
            //插入一个图片
            document.add(image);
            fileOutputStream.flush();
        }catch(Throwable throwable){
            LogUtil.error("PdfUtil_img2pdf_1", throwable, isDelImg);
        } finally {
            if (document != null) {
                try{
                    document.close();
                }catch(Throwable throwable){
                    //ignore
                }
            }
            IOUtil.close(fileOutputStream);
        }
        if (isDelImg) {
            FileUtil.fileDelete(fFrom);
        }
        return 0;
    }

    /**
     * 添加水印和图片
     * absoluteX 图片的x坐标
     * absoluteY 图片的y坐标
     * fitWidth 图片的宽度
     * fitHeight 图片的高度
     *
     * @return
     */
    public static final void setWatermark2(final String inputFilePath, final String outputFilePath, final String imgBase64, final float absoluteX, final float absoluteY, final int pageSize){
        try{
            // 加载PNG图片
            BufferedImage image = null;
            Base64.Decoder decoder = Base64.getDecoder();
            try{
                byte[] byteArray = decoder.decode(imgBase64);
                ByteArrayInputStream bai = new ByteArrayInputStream(byteArray);
                image = ImageIO.read(bai);
            }catch(Throwable throwable){
                LogUtil.error("PdfUtil_setWatermark2_t4", throwable, inputFilePath, outputFilePath);
            }
            // 打开PDF文件
            PDDocument document = null;
            PDPageContentStream contentStream = null;
            try{
                document = Loader.loadPDF(new File(inputFilePath));

                // 获取页数
                PDPage page = document.getPage(pageSize);

                // 创建页面内容流
                contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);

                // 将PNG图片转为PDImageXObject
                PDImageXObject ximage = LosslessFactory.createFromImage(document, image);

                // 在指定位置绘制图片
                float height = page.getArtBox().getHeight();
                float width = page.getArtBox().getWidth();
                int x = (int)((absoluteX/800*680+60)/1.33);
                int y = (int) ((1123-30-(absoluteY*0.8516))/ 1.33-114);

                contentStream.drawImage(ximage, x, y,133,114);

                // 关闭内容流
                contentStream.close();
                contentStream = null;

                // 保存PDF文件
                document.save(outputFilePath);

                LogUtil.info("PdfUtil_setWatermark2_1", "图片已成功粘贴到PDF文件中。");
            }finally{
                IOUtil.close(contentStream, document);
            }
        }catch(Throwable throwable){
            LogUtil.error("PdfUtil_setWatermark2_t5", throwable, inputFilePath, outputFilePath);
        }
//        //        1、获得PDF文件流对象
//        FileInputStream pdfInputStream = new FileInputStream(inputFilePath);
////        2、得到PDF文档对象
//        PDDocument  pdDocument = PDDocument.load(pdfInputStream);
////        3、由Document得到Page对象
//        PDPage page = pdDocument.getPage(0);
////        4、通过图片路径和PDF文档对象得到图片image对象
//        PDImageXObject image = PDImageXObject.createFromFile("D:/data/tmp/example.png", pdDocument);
////        5、创建pageStream对象
//        PDPageContentStream pageStream = new PDPageContentStream(pdDocument, page,PDPageContentStream.AppendMode.APPEND,false,false);
//
//        /**
//         * 可优化位置
//         */
////   6、pageStream对象绘制图片位置及大小，已PDF文件右下角为原点（x,y）是图片左下角左边，width、height是图片的长和宽
//        pageStream.drawImage(image, 496, 0,146,146);
//        pageStream.close();
////    7、保存PDF到指定路劲
//        pdDocument.save(outputFilePath);
////      8、关闭流
//        pdDocument.close();
//        pdfInputStream.close();

    }

    /**
     * 2026-07-20
     * 以字节流方式创建PdfReader
     * @param pdfUrl PDF文件完整路径
     * @Return PdfReader 成功返回PdfReader对象，失败返回null
     */
    public static final PdfReader getPdfReader(String pdfUrl) {
        try {
            byte[] bytes = FileUtil.file2bytes(pdfUrl);
            PdfReader pdfReader = new PdfReader(bytes);
            return pdfReader;
        } catch (Throwable throwable) {
            LogUtil.error("PdfUtil_getPdfReader_t1", throwable, "创建PdfReader失败", pdfUrl);
            return null;
        }
    }

    /**
     * 2026-07-20
     * 以字节流方式关闭PdfReader
     * @param pdfReader PdfReader对象
     * @Return boolean 成功返回true，失败返回false
     */
    public static final boolean closePdfReader(PdfReader pdfReader) {
        if(pdfReader == null){
            return true;
        }
        try {
            pdfReader.close();
            return true;
        } catch (Throwable throwable) {
            LogUtil.error("PdfUtil_closePdfReader_1", throwable, "关闭PdfReader失败");
            return false;
        }
    }

    /**
     * 2026-09-24 检测PDF格式文件是否标准规范与合法
     * 检测PDF文件是否标准合法：先做文件头尾结构快检，再用iText按索引链深检
     * 判定口径：严格按规范走索引链的阅读器，不靠全文扫描修复能否打开并得到至少一页
     * @param pdfUrl PDF文件完整路径
     * @return null表示标准合法；非null为不合格的中文原因
     */
    public static final String checkStandard(final String pdfUrl) {
        // 入参校验
        if (StringUtil.isBlank(pdfUrl)) {
            return "文件路径为空";
        }
        String pdfUrlLocal = UrlUtil.getStandardPath(pdfUrl);
        if (!FileUtil.exists(pdfUrlLocal)) {
            return "文件不存在";
        }
        // 第一层：头尾结构快检
        LogUtil.info("PdfUtil_checkStandard_1", "开始检测PDF是否标准合法", pdfUrlLocal);
        String reason = p_checkHeadTail(pdfUrlLocal);
        if (reason != null) {
            LogUtil.warn("PdfUtil_checkStandard_3", "PDF快检不合格", pdfUrlLocal, reason);
            return reason;
        }
        // 第二层：iText按索引链深检
        reason = p_checkByItext(pdfUrlLocal);
        if (reason != null) {
            LogUtil.warn("PdfUtil_checkStandard_4", "PDF深检不合格", pdfUrlLocal, reason);
            return reason;
        }
        // 全部通过
        LogUtil.info("PdfUtil_checkStandard_2", "PDF检测合格", pdfUrlLocal);
        return null;
    }

    /**
     * 2026-09-24 检测PDF格式文件是否标准规范与合法
     * 检测PDF字节数组是否标准合法，判定口径与文件路径版一致
     * @param pdfBytes PDF文件内容字节数组
     * @return null表示标准合法；非null为不合格的中文原因
     */
    public static final String checkStandard(final byte[] pdfBytes) {
        // 入参校验
        if (CollectionUtil.isBlank(pdfBytes)) {
            return "文件内容为空";
        }
        int bytesLength = pdfBytes.length;
        LogUtil.info("PdfUtil_checkStandard_5", "开始检测PDF是否标准合法(字节数组)", bytesLength);
        // 取头部1024字节做头检查
        int headLen = 1024;
        if (headLen > bytesLength) {
            headLen = bytesLength;
        }
        byte[] headBytes = Arrays.copyOfRange(pdfBytes, 0, headLen);
        // 取尾部1024字节做尾检查
        int tailLen = 1024;
        if (tailLen > bytesLength) {
            tailLen = bytesLength;
        }
        byte[] tailBytes = Arrays.copyOfRange(pdfBytes, bytesLength - tailLen, bytesLength);
        // 第一层：头尾结构快检
        String reason = p_checkHeadTailBytes(headBytes, tailBytes);
        if (reason != null) {
            LogUtil.warn("PdfUtil_checkStandard_7", "PDF快检不合格(字节数组)", reason);
            return reason;
        }
        // 第二层：iText按索引链深检
        reason = p_checkByItext(pdfBytes);
        if (reason != null) {
            LogUtil.warn("PdfUtil_checkStandard_8", "PDF深检不合格(字节数组)", reason);
            return reason;
        }
        // 全部通过
        LogUtil.info("PdfUtil_checkStandard_6", "PDF检测合格(字节数组)", bytesLength);
        return null;
    }

    /**
     * 2026-09-24 检测PDF格式文件是否标准规范与合法
     * 判断PDF文件是否标准合法
     * @param pdfUrl PDF文件完整路径
     * @return true表示标准合法
     */
    public static final boolean isStandard(final String pdfUrl) {
        String reason = checkStandard(pdfUrl);
        if (reason == null) {
            return true;
        }
        return false;
    }

    /**
     * 2026-09-24 检测PDF格式文件是否标准规范与合法
     * 判断PDF字节数组是否标准合法
     * @param pdfBytes PDF文件内容字节数组
     * @return true表示标准合法
     */
    public static final boolean isStandard(final byte[] pdfBytes) {
        String reason = checkStandard(pdfBytes);
        if (reason == null) {
            return true;
        }
        return false;
    }

    /**
     * 2026-09-24 检测PDF格式文件是否标准规范与合法
     * 读取文件头尾各1024字节做结构快检，整个文件不读进内存（60MB级大文件也只读2048字节）
     * @param pdfUrl PDF文件完整路径
     * @return null表示通过；非null为不合格的中文原因
     */
    private static final String p_checkHeadTail(final String pdfUrl) {
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(pdfUrl, "r");
            // 读头部1024字节
            long fileLength = randomAccessFile.length();
            if (fileLength <= 0) {
                return "文件内容为空";
            }
            int headLen = 1024;
            if (headLen > fileLength) {
                headLen = (int) fileLength;
            }
            byte[] headBytes = new byte[headLen];
            randomAccessFile.readFully(headBytes);
            // 读尾部1024字节
            long tailStart = fileLength - 1024;
            if (tailStart < 0) {
                tailStart = 0;
            }
            int tailLen = (int) (fileLength - tailStart);
            randomAccessFile.seek(tailStart);
            byte[] tailBytes = new byte[tailLen];
            randomAccessFile.readFully(tailBytes);
            // 头尾字节判定
            String reason = p_checkHeadTailBytes(headBytes, tailBytes);
            return reason;
        } catch (Throwable throwable) {
            LogUtil.error("PdfUtil_p_checkHeadTail_1", throwable, "头尾快检读文件异常", pdfUrl);
            return "文件读取失败";
        } finally {
            IOUtil.close(randomAccessFile);
        }
    }

    /**
     * 2026-09-24 检测PDF格式文件是否标准规范与合法
     * 头尾字节的具体判定：头部要有%PDF-版本标记，尾部要有%%EOF与其前的startxref数字偏移
     * @param headBytes 头部字节
     * @param tailBytes 尾部字节
     * @return null表示通过；非null为不合格的中文原因
     */
    private static final String p_checkHeadTailBytes(final byte[] headBytes, final byte[] tailBytes) {
        try {
            // 头部找%PDF-版本标记，规范要求版本号为1.x或2.x
            String head = new String(headBytes, "ISO-8859-1");
            int headIndex = head.indexOf("%PDF-");
            if (headIndex < 0) {
                return "文件头部没有PDF版本标记，不是PDF文件";
            }
            int versionStart = headIndex + 5;
            if (versionStart + 1 >= head.length()) {
                return "文件头部PDF版本号不完整";
            }
            char majorChar = head.charAt(versionStart);
            char dotChar = head.charAt(versionStart + 1);
            boolean versionOk = false;
            if (majorChar == '1' && dotChar == '.') {
                versionOk = true;
            }
            if (majorChar == '2' && dotChar == '.') {
                versionOk = true;
            }
            if (!versionOk) {
                return "文件头部PDF版本号不合法";
            }
            // 尾部找%%EOF标记，没有它说明文件没有正常收尾（截断或没写完）
            String tail = new String(tailBytes, "ISO-8859-1");
            int eofIndex = tail.lastIndexOf("%%EOF");
            if (eofIndex < 0) {
                return "文件结尾缺少%%EOF标记，文件不完整";
            }
            // %%EOF之前要有startxref，且后面跟数字偏移，阅读器靠它定位交叉引用表
            int startXrefIndex = tail.lastIndexOf("startxref", eofIndex);
            if (startXrefIndex < 0) {
                return "文件结尾缺少startxref标记，索引定位信息丢失";
            }
            int cursor = startXrefIndex + 9;
            while (cursor < tail.length()) {
                char c = tail.charAt(cursor);
                if (c == ' ' || c == '\r' || c == '\n' || c == '\t') {
                    cursor++;
                    continue;
                }
                break;
            }
            if (cursor >= tail.length()) {
                return "startxref后面没有索引偏移值";
            }
            char firstChar = tail.charAt(cursor);
            if (firstChar < '0' || firstChar > '9') {
                return "startxref后面的内容不是数字，索引定位信息损坏";
            }
            // 头尾结构完整
            return null;
        } catch (Throwable throwable) {
            LogUtil.error("PdfUtil_p_checkHeadTailBytes_1", throwable, "头尾字节判定异常");
            return "文件结构判定失败";
        }
    }

    /**
     * 2026-09-24 检测PDF格式文件是否标准规范与合法
     * 用iText按索引链深检文件版：打开不抛异常、没有被逼全文扫描重建（isRebuilt）、页数大于0
     * @param pdfUrl PDF文件完整路径
     * @return null表示通过；非null为不合格的中文原因
     */
    private static final String p_checkByItext(final String pdfUrl) {
        PdfReader pdfReader = null;
        try {
            pdfReader = new PdfReader(pdfUrl);
            String reason = p_checkReader(pdfReader);
            return reason;
        } catch (Throwable throwable) {
            LogUtil.error("PdfUtil_p_checkByItext_1", throwable, "iText打开文件异常", pdfUrl);
            String reason = p_buildParseException(throwable);
            return reason;
        } finally {
            closePdfReader(pdfReader);
        }
    }

    /**
     * 2026-09-24 检测PDF格式文件是否标准规范与合法
     * 用iText按索引链深检字节数组版，判定逻辑与文件版一致
     * @param pdfBytes PDF文件内容字节数组
     * @return null表示通过；非null为不合格的中文原因
     */
    private static final String p_checkByItext(final byte[] pdfBytes) {
        PdfReader pdfReader = null;
        try {
            pdfReader = new PdfReader(pdfBytes);
            String reason = p_checkReader(pdfReader);
            return reason;
        } catch (Throwable throwable) {
            int bytesLength = pdfBytes.length;
            LogUtil.error("PdfUtil_p_checkByItext_2", throwable, "iText打开字节数组异常", bytesLength);
            String reason = p_buildParseException(throwable);
            return reason;
        } finally {
            closePdfReader(pdfReader);
        }
    }

    /**
     * 2026-09-24 检测PDF格式文件是否标准规范与合法
     * 对已打开的PdfReader做判定：isRebuilt为true说明官方索引链已损坏（靠全文扫描救回）；页数为0是空壳文档
     * @param pdfReader 已成功打开的PdfReader
     * @return null表示通过；非null为不合格的中文原因
     */
    private static final String p_checkReader(final PdfReader pdfReader) {
        // 索引链完整性：isRebuilt为true代表文件自己的索引对不上，阅读器被迫重建
        boolean rebuilt = pdfReader.isRebuilt();
        if (rebuilt) {
            return "文件索引损坏，阅读器需要全文扫描重建才能读出内容";
        }
        // 页数：有头有尾但一页都没有的空壳文档判不合格
        int pages = pdfReader.getNumberOfPages();
        if (pages <= 0) {
            return "文档页数为0，没有任何页面内容";
        }
        // 深检通过
        return null;
    }

    /**
     * 2026-09-24 检测PDF格式文件是否标准规范与合法
     * 把打开PDF时抛出的异常拼成中文原因，带上异常类名与信息方便排查
     * @param throwable 打开PDF时抛出的异常
     * @return 不合格的中文原因
     */
    private static final String p_buildParseException(final Throwable throwable) {
        String className = throwable.getClass().getSimpleName();
        String message = throwable.getMessage();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PDF解析失败：");
        stringBuilder.append(className);
        if (message != null) {
            stringBuilder.append("：");
            stringBuilder.append(message);
        }
        String reason = stringBuilder.toString();
        return reason;
    }
}
