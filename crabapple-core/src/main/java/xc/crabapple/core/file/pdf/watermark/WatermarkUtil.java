package xc.crabapple.core.file.pdf.watermark;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.device.EnvironmentUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 2017-06-30 1.0.0
 * 水印工具类
 * 给图片,pdf增添文字和图片水印
 */
public final class WatermarkUtil {

    private static final Float IAMGE_HEIGHT = 110F;  // 限制水印图片的的高度
    private static final Float IAMGE_WIDTH = 110F;  // 限制水印图片的的宽度

    /**
     * 2017-06-30 1.0.0
     */
    public static final String add(final WatermarkBean watermarkBean) {
        String content = watermarkBean.getContent();
        String urlFrom = watermarkBean.getUrlFrom();
        String urlTo = watermarkBean.getUrlTo();
        Float alpha = watermarkBean.getAlpha();
        if (alpha == null || alpha < 0F || alpha > 1F) {
            alpha = 0.3F;
            watermarkBean.setAlpha(alpha);
        }
        if (content == null) {
            LogUtil.error("WatermarkUtil_add_1", "content为空对象");
            return "error_1";
        }
        String[] imgType = {".jpg", ".jpeg", ".gif", ".ai", ".pdg", ".png", ".bmp", ".psd", ".pcx", ".dxf", ".cdr"};
        List<String> imgTypeList = Arrays.asList(imgType);
        if (content.contains(".")) {
            int lastIndexOf = content.lastIndexOf(".");
            String substr = content.substring(lastIndexOf, content.length());
            substr = substr.toLowerCase();
            if (imgTypeList.contains(substr)) {
                int lastIndexOf1 = urlFrom.lastIndexOf(".");
                String str = urlFrom.substring(lastIndexOf1, urlFrom.length());
                str = str.toLowerCase();
                boolean contains = str.contains(".pdf");
                boolean contains1 = imgTypeList.contains(str);
                if (urlFrom != null && contains) {
                    addImg2pdf2(watermarkBean);
                } else if (urlFrom != null && contains1) {
                    addImg2img(watermarkBean);
                } else if (urlFrom != null && (".html").equals(str)) {
                    //
                } else {
                    LogUtil.error("WatermarkUtil_add_4", "文件源地址不能为空或者地址不正确");
                    return "error_2";
                }
                return "success";
            }
            int lastIndexOf1 = urlFrom.lastIndexOf(".");
            String str = urlFrom.substring(lastIndexOf1, urlFrom.length());
            str = str.toLowerCase();
            boolean contains = str.contains(".pdf");
            boolean contains1 = imgTypeList.contains(str);
            if (urlFrom != null && contains) {
                addText2pdf(watermarkBean);
            } else if (urlFrom != null && contains1) {
                addText2img(watermarkBean);
            } else {
                LogUtil.error("WatermarkUtil_add_5", "文件源地址不能为空或者地址不正确");
                return "error_3";
            }
            return "success";
        }
        //
        int lastIndexOf1 = urlFrom.lastIndexOf(".");
        int len = urlFrom.length();
        String str = urlFrom.substring(lastIndexOf1, len);
        str = str.toLowerCase();
        boolean contains = (".pdf").equals(str);  //判断文件源是pdf
        boolean contains1 = imgTypeList.contains(str);  //判断文件源是图片
        if (urlFrom != null && contains) {
            addText2pdf(watermarkBean);
        } else if (urlFrom != null && contains1) {
            addText2img(watermarkBean);
        } else if (urlFrom != null && (".html").equals(str)) {
            //
        } else {
            LogUtil.error("WatermarkUtil_add_6", "文件源地址不能为空或者地址不正确");
            return "error_4";
        }
        return "success";
    }

    /**
     * 2017-06-30 1.0.0
     * 为图片添加文字水印
     */
    private static final String addText2img(final WatermarkBean watermarkBean) {
        String content = watermarkBean.getContent();
        String urlFrom = watermarkBean.getUrlFrom();
        String urlTo = watermarkBean.getUrlTo();
        Float alpha = watermarkBean.getAlpha();
        try{
            File file = new File(urlFrom);
            Image image = ImageIO.read(file);
            int width = image.getWidth(null);
            int height = image.getHeight(null);
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = bufferedImage.createGraphics();
            g.drawImage(image, 0, 0, width, height, null);
            //"黑体", Font.BOLD, 20
            String fontName = "黑体";
            int fontStyle = Font.BOLD;
            int fontSize = 20;
            g.setFont(new Font(fontName, fontStyle, fontSize));
            //设置字体颜色
            g.setColor(Color.BLACK);
            AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha);
            g.setComposite(alphaComposite);
            //文字,宽
            int width_1 = fontSize * StringUtil.getLength(content);
            //文字,高
            int height_1 = fontSize;
            int widthDiff = width - width_1;
            int heightDiff = height - height_1;
            int x = -5;
            int y = -5;
            if (x < 0) {
                x = widthDiff / 2;
            } else if (x > widthDiff) {
                x = widthDiff;
            }
            if (y < 0) {
                y = heightDiff / 2;
            } else if (y > heightDiff) {
                y = heightDiff;
            }
            g.drawString(content, x, y + height_1);
            g.dispose();
            ImageIO.write(bufferedImage, "jpg", new File(urlTo));
            return "success";
        }catch(Throwable throwable){
            LogUtil.error("WatermarkUtil_add2img2_t1", throwable);
            return "error";
        }
    }

    /**
     * 2017-06-30 1.0.0
     * 为图片添加图片logo水印
     */
    private static final void addImg2img(final WatermarkBean watermarkBean) {
        String content = watermarkBean.getContent();
        String urlFrom = watermarkBean.getUrlFrom();
        String urlTo = watermarkBean.getUrlTo();
        Float alpha = watermarkBean.getAlpha();
        try{
            File file = new File(urlFrom);
            Image image = ImageIO.read(file);
            int width = image.getWidth(null);
            int height = image.getHeight(null);
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = bufferedImage.createGraphics();
            g.drawImage(image, 0, 0, width, height, null);
            Image waterImage = ImageIO.read(new File(content));  //水印文件
            int width_1 = waterImage.getWidth(null);
            int height_1 = waterImage.getHeight(null);
            AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha);
            g.setComposite(alphaComposite);
            int widthDiff = width - width_1;
            int heightDiff = height - height_1;
            int x = -1;
            int y = -1;
            if (x < 0) {
                x = widthDiff / 2;
            } else if (x > widthDiff) {
                x = widthDiff;
            }
            if (y < 0) {
                y = heightDiff / 2;
            } else if (y > heightDiff) {
                y = heightDiff;
            }
            g.drawImage(waterImage, x, y, width_1, height_1, null);  //水印文件结束
            g.dispose();
            ImageIO.write(bufferedImage, "jpg", new File(urlTo));
        }catch(Throwable throwable){
            LogUtil.error("WatermarkUtil_add2img_t22", throwable, watermarkBean);
        }
    }

    /**
     * 2017-06-30 1.0.0
     * PDF加文字水印
     */
    private static final void addText2pdf(final WatermarkBean watermarkBean) {
        String content = watermarkBean.getContent();
        String urlFrom = watermarkBean.getUrlFrom();
        String urlTo = watermarkBean.getUrlTo();
        Float alpha = watermarkBean.getAlpha();
        Integer layer = watermarkBean.getLayer();
        PdfReader pdfReader = null;
        PdfStamper pdfStamper = null;
        FileOutputStream fileOutputStream = null;
        try{
            fileOutputStream = new FileOutputStream(urlTo);
            pdfReader = new PdfReader(urlFrom);
            pdfStamper = new PdfStamper(pdfReader, fileOutputStream);
            String prefixFont = "";
            if (EnvironmentUtil.isWindows()) {
                prefixFont = "C:/Windows/Fonts/";
            } else if (EnvironmentUtil.isLinux()) {
                prefixFont = "/usr/share/fonts/chinese/Truetype/";
            }
//            BaseFont baseFont = BaseFont.createFont(prefixFont + "msyh.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont baseFont = BaseFont.createFont("C:/Windows/Containers/" + "msyh.ttc,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            baseFont.setPostscriptFontName(BaseFont.notdef);
            int total = pdfReader.getNumberOfPages() + 1;
            PdfGState gs = new PdfGState();
            gs.setFillOpacity(alpha);  //透明度
            //gs.setAlphaIsShape(true);
            com.itextpdf.text.Rectangle pageRect = null;
            for (int i = 1; i < total; i++) {
                pageRect = pdfStamper.getReader().getPageSizeWithRotation(i);
                //计算水印X,Y坐标
                Float x = pageRect.getWidth() / 2;
                Float y = pageRect.getHeight() / 2;
                PdfContentByte pdfContentByte;
                if (layer == WatermarkBean.UNDER) {
                    pdfContentByte = pdfStamper.getUnderContent(i);  //水印在最下层
                } else if (layer == WatermarkBean.OVER) {
                    pdfContentByte = pdfStamper.getOverContent(i);  //水印在最上层
                } else {
                    pdfContentByte = pdfStamper.getUnderContent(i);  //水印在最下层
                }
                pdfContentByte.saveState();
                pdfContentByte.setGState(gs);
                //这里必须调用一次restoreState 否则设置无效
                pdfContentByte.restoreState();
                pdfContentByte.beginText();
                //水印字体大小 字号
                pdfContentByte.setFontAndSize(baseFont, x / 10);
                //水印字体颜色
                BaseColor baseColor = new BaseColor(238, 209, 212);
                pdfContentByte.setColorFill(baseColor);
                pdfContentByte.setColorStroke(baseColor);
                //水印文字成45度角倾斜
                pdfContentByte.showTextAligned(Element.ALIGN_CENTER, content, x / 2, y / 2, 55);
                pdfContentByte.showTextAligned(Element.ALIGN_CENTER, content, x / 2, y / 2 + y, 55);
                pdfContentByte.showTextAligned(Element.ALIGN_CENTER, content, x / 2 + x, y / 2, 55);
                pdfContentByte.showTextAligned(Element.ALIGN_CENTER, content, x / 2 + x, y / 2 + y, 55);
                //添加水印文字
                pdfContentByte.endText();
                pdfContentByte.setLineWidth(1F);
                pdfContentByte.stroke();
            }
        }catch(Throwable throwable){
            LogUtil.error("WatermarkUtil_add2pdf_t11", throwable, watermarkBean);
        } finally {
            try{
                if (pdfStamper != null) {
                    pdfStamper.close();
                }
            }catch(Throwable throwable){
                //ignore
            }
            try{
                if (pdfReader != null) {
                    pdfReader.close();
                }
            }catch(Throwable throwable){
                //ignore
            }
            IOUtil.close(fileOutputStream);
        }
    }

    /**
     * 2017-06-30 1.0.0
     * PDF添加图片水印
     */
    private static final String addImg2pdf(final WatermarkBean watermarkBean) {
        String content = watermarkBean.getContent();
        String urlFrom = watermarkBean.getUrlFrom();
        String urlTo = watermarkBean.getUrlTo();
        Float alpha = watermarkBean.getAlpha();
        Integer layer = watermarkBean.getLayer();
        Integer position = watermarkBean.getPosition();
        InputStream inputStream = null;
        OutputStream outputStream = null;
        PdfReader pdfReader = null;
        PdfStamper pdfStamper = null;
        try{
            //创建pdf输入输出流
            inputStream = new FileInputStream(urlFrom);
            pdfReader = new PdfReader(inputStream);
            outputStream = new FileOutputStream(urlTo);
            pdfStamper = new PdfStamper(pdfReader, outputStream);
            //获取正文的总页数
            int pageNum = pdfReader.getNumberOfPages();
            //设置透明度
            PdfGState gs = new PdfGState();
            gs.setFillOpacity(alpha);  //透明度
            //读取图片
            com.itextpdf.text.Image logo = com.itextpdf.text.Image.getInstance(content);
            //在pdf每页右上角添加水印
            for (int i = 1; i <= pageNum; i++) {
                PdfContentByte pdfContentByte;
                if (layer == WatermarkBean.UNDER) {
                    pdfContentByte = pdfStamper.getUnderContent(i);  //水印在最下层
                } else if (layer == WatermarkBean.OVER) {
                    pdfContentByte = pdfStamper.getOverContent(i);  //水印在最上层
                } else {
                    pdfContentByte = pdfStamper.getUnderContent(i);  //水印在最下层
                }
                pdfContentByte.setGState(gs);
                com.itextpdf.text.Rectangle rectangle = pdfReader.getPageSize(i);
                Float width = rectangle.getWidth();  //获取正文宽
                Float height = rectangle.getHeight();  //获取正文长
                //设置logo位置, x,y
                switch (position) {
                    case 1:
                        logo.setAbsolutePosition(width / 2 - logo.getWidth() / 2, height / 2);  //居中
                        break;
                    case 2:
                        logo.setAbsolutePosition(width - logo.getWidth(), 0);  //右下
                        break;
                    case 3:
                        logo.setAbsolutePosition(width / 2 - logo.getWidth() / 2, 0);  //正下
                        break;
                }
                pdfContentByte.addImage(logo);  //logo add到正文
            }
            return "success";
        }catch(Throwable throwable){
            LogUtil.error("WatermarkUtil_add2pdf_1", throwable, watermarkBean);
            return "error";
        } finally {
            try{
                if (pdfStamper != null) {
                    pdfStamper.close();
                }
            }catch(Throwable throwable){
                //ignore
            }
            try{
                if (pdfReader != null) {
                    pdfReader.close();
                }
            }catch(Throwable throwable){
                //ignore
            }
            IOUtil.close(outputStream, inputStream);
        }
    }

    /**
     * 2017-06-30 1.0.0
     * PDF添加图片水印  随机位置
     */
    private static final void addImg2pdf2(final WatermarkBean watermarkBean) {
        String content = watermarkBean.getContent();
        String urlFrom = watermarkBean.getUrlFrom();
        String urlTo = watermarkBean.getUrlTo();
        Float alpha = watermarkBean.getAlpha();
        Integer layer = watermarkBean.getLayer();
        InputStream inputStream = null;
        OutputStream outputStream = null;
        PdfReader pdfReader = null;
        PdfStamper pdfStamper = null;
        try{
            //1.创建pdf输入输出流
            inputStream = new FileInputStream(urlFrom);
            pdfReader = new PdfReader(inputStream);
            outputStream = new FileOutputStream(urlTo);
            pdfStamper = new PdfStamper(pdfReader, outputStream);
            int pageSize = pdfReader.getNumberOfPages();  //获取正文总页数
            Float pageHeight = pdfReader.getPageSize(1).getHeight();  //正文长
            Float pageWidth = pdfReader.getPageSize(1).getWidth();  //正文宽
            // 每两行显示两个 左边一个，右边一个
            int lineNum = (int) (pageHeight / IAMGE_HEIGHT);  //行数
            int middleY = Integer.parseInt(pageWidth.toString()) / 2;
            Random random = new Random();
            com.itextpdf.text.Image logo = com.itextpdf.text.Image.getInstance(content);  //插入水印
            logo.scaleAbsolute(IAMGE_WIDTH, IAMGE_HEIGHT * 184 / 455);
            logo.setAlignment(logo.UNDERLYING);  //在字下面
            PdfGState gs = new PdfGState();  // 设置透明度
            gs.setFillOpacity(alpha);  //透明度
            //为每页pdf 加水印
            for (int i = 1; i <= pageSize; i++) {
                for (int j = 0, k = 0; j < lineNum; j = j + 2, k++) {
                    int trueY;
                    while (true) {
                        trueY = random.nextInt(middleY);
                        if (trueY > IAMGE_WIDTH / 2 && trueY < (middleY - IAMGE_WIDTH)) {
                            break;
                        }
                    }
                    logo.setAbsolutePosition(trueY, j * IAMGE_HEIGHT + random.nextInt(Integer.parseInt(IAMGE_HEIGHT.toString())) - (k % 2) * 10);  //水印的位置
                    logo.setRotationDegrees(random.nextInt(360));  //旋转 角度
                    PdfContentByte pdfContentByte;
                    if (layer == WatermarkBean.UNDER) {
                        pdfContentByte = pdfStamper.getUnderContent(i);  //水印在最下层
                    } else if (layer == WatermarkBean.OVER) {
                        pdfContentByte = pdfStamper.getOverContent(i);  //水印在最上层
                    } else {
                        pdfContentByte = pdfStamper.getUnderContent(i);  //水印在最下层
                    }
                    pdfContentByte.setGState(gs);
                    pdfContentByte.addImage(logo);
                    while (true) {
                        trueY = random.nextInt(middleY) + middleY;
                        if (trueY > middleY + IAMGE_WIDTH / 2 && trueY < (2 * middleY - IAMGE_WIDTH)) {
                            break;
                        }
                    }
                    logo.setAbsolutePosition(trueY, j * IAMGE_HEIGHT + random.nextInt(Integer.parseInt(IAMGE_HEIGHT.toString())) - (k % 2) * 10);  // 水印的位置
                    logo.setRotationDegrees(random.nextInt(360));  //旋转 角度
                    pdfContentByte.addImage(logo);
                }
            }
        }catch(Throwable throwable){
            LogUtil.error("WatermarkUtil_add2pdf_t31", throwable, watermarkBean);
        } finally {
            try{
                if (pdfStamper != null) {
                    pdfStamper.close();
                }
            }catch(Throwable throwable){
                //ignore
            }
            try{
                if (pdfReader != null) {
                    pdfReader.close();
                }
            }catch(Throwable throwable){
                //ignore
            }
            IOUtil.close(outputStream, inputStream);
        }
    }

    /**
     * 2017-06-30 1.0.0
     * 图片缩放
     *
     * @param filePath 图片路径
     * @param height   高度
     * @param width    宽度
     * @param bb       比例不对时是否需要补白
     */
    private static final void imageResize(final String filePath, final int height, final int width, final boolean bb) {
        try{
            double ratio = 0;  //缩放比例
            File f = new File(filePath);
            BufferedImage bi = ImageIO.read(f);
            Image itemp = bi.getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH);
            //计算比例
            if ((bi.getHeight() > height) || (bi.getWidth() > width)) {
                if (bi.getHeight() > bi.getWidth()) {
                    ratio = ((Integer) height).doubleValue() / bi.getHeight();
                } else {
                    ratio = ((Integer) width).doubleValue() / bi.getWidth();
                }
                AffineTransformOp op = new AffineTransformOp(AffineTransform.getScaleInstance(ratio, ratio), null);
                itemp = op.filter(bi, null);
            }
            if (bb) {
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                g.setColor(Color.white);
                g.fillRect(0, 0, width, height);
                if (width == itemp.getWidth(null)) {
                    g.drawImage(itemp, 0, (height - itemp.getHeight(null)) / 2, itemp.getWidth(null), itemp.getHeight(null), Color.white, null);
                } else {
                    g.drawImage(itemp, (width - itemp.getWidth(null)) / 2, 0, itemp.getWidth(null), itemp.getHeight(null), Color.white, null);
                }
                g.dispose();
                itemp = image;
            }
            ImageIO.write((BufferedImage) itemp, "jpg", f);
        }catch(Throwable throwable){
            LogUtil.error("WatermarkUtil_imageResize_1", throwable, filePath, height, width, bb);
        }
    }

    /*
     * 2023-12-12
     *  pdf文件添加水印
     **/
    public static final String pdfAddWaterMark(final WatermarkBean bean) {
        String projectName = bean.getProjectName();//项目名称
        Integer fontSize = bean.getFontSize();//字体大小
        Integer wordSpace = bean.getWordSpace();//字间距
        Integer lineSpace = bean.getLineSpace();//行间距
        String inputFilePath = bean.getUrlFrom();//输入地址
        String outputFilePath = bean.getUrlTo();//输出地址
        Integer font = bean.getFont();//字体
        Integer rotation = bean.getRotation();//旋转角度
        Integer pageRange = bean.getPageRange();//页面范围
        Integer region = bean.getRegion();//区域
        String userName = bean.getUserName1();//姓名
        String mobile = bean.getMobile1();//手机号
        if (StringUtil.isBlankOfAll(projectName, userName, mobile)) {
            return "error1";
        }
        PdfReader pdfReader = null;
        PdfStamper pdfStamper = null;
        try{
            // 读取PDF文件流
            pdfReader = new PdfReader(inputFilePath);
            // 创建PDF文件的模板，可以对模板的内容修改，重新生成新PDF文件
            pdfStamper = new PdfStamper(pdfReader, new FileOutputStream(outputFilePath));
            // 水印的高和宽
            float projectNameWaterMarkHeight = 0;
            float projectNameWatermarkWeight = 0;
            float userNameWaterMarkHeight = 0;
            float userNameWatermarkWeight = 0;
            float mobileWaterMarkHeight = 0;
            float mobileWatermarkWeight = 0;
            if (rotation == 0) {
                if (!StringUtil.isBlank(projectName)) {
                    projectNameWaterMarkHeight = fontSize;
                    projectNameWatermarkWeight = projectName.length() * fontSize;
                }
                if (!StringUtil.isBlank(userName)) {
                    userNameWaterMarkHeight = fontSize;
                    userNameWatermarkWeight = userName.length() * fontSize;
                }
                if (!StringUtil.isBlank(mobile)) {
                    mobileWaterMarkHeight = fontSize;
                    mobileWatermarkWeight = mobile.length() * fontSize / 2;
                }
            } else {
                if (!StringUtil.isBlank(projectName)) {
                    int projectNameLen = projectName.length() * fontSize;
                    projectNameWaterMarkHeight = (float) Math.sqrt(projectNameLen * projectNameLen / 2);// 计算直角边长
                    projectNameWatermarkWeight = (float) Math.sqrt(projectNameLen * projectNameLen / 2);
                }
                if (!StringUtil.isBlank(userName)) {
                    int userNameLen = userName.length() * fontSize;
                    userNameWaterMarkHeight = (float) Math.sqrt(userNameLen * userNameLen / 2);
                    userNameWatermarkWeight = (float) Math.sqrt(userNameLen * userNameLen / 2);// 计算直角边长
                }
                if (!StringUtil.isBlank(mobile)) {
                    int mobileLen = mobile.length() / 2 * fontSize;
                    mobileWaterMarkHeight = (float) Math.sqrt(mobileLen * mobileLen / 2);
                    mobileWatermarkWeight = (float) Math.sqrt(mobileLen * mobileLen / 2);
                }
            }
            //heightInterval 行间距 单位像素
            int heightInterval;
            if (rotation == 0) {
                heightInterval = lineSpace * fontSize;
            } else {
                heightInterval = (int) ((lineSpace + 1) * fontSize * Math.sqrt(2));
            }
            //wordInterval 字间距 单位像素
            int wordInterval = 0;
            if (rotation == 0) {
                wordInterval = wordSpace * fontSize;
            } else {
                wordInterval = (int) Math.sqrt((wordSpace * fontSize) * (wordSpace * fontSize) / 2);
            }
            // 设置水印字体 默认windows+微软雅黑
            BaseFont baseFont = getFont(font);
            // 设置PDF内容的Graphic State 图形状态
            PdfGState pdfGraPhicState = new PdfGState();
            // 填充透明度
            pdfGraPhicState.setFillOpacity(0.3f);
            // 轮廓不透明度
            pdfGraPhicState.setStrokeOpacity(0.4f);
            // PDF页数
            int pdfPageNum = pdfReader.getNumberOfPages() + 1;
            // PDF文件内容字节
            PdfContentByte pdfContent;
            // PDF页面矩形区域
            com.itextpdf.text.Rectangle pageRectangle;
            //渲染所有页面,奇数页面 偶数页面
            if (pageRange == WatermarkEnum.SUOYOU.getCode()) {//所有页面
                for (int i = 1; i < pdfPageNum; i++) {
                    rendering(pdfReader, i, pdfStamper, pdfGraPhicState, baseFont, fontSize, projectNameWaterMarkHeight, projectNameWatermarkWeight, userNameWaterMarkHeight, userNameWatermarkWeight, mobileWaterMarkHeight, mobileWatermarkWeight, heightInterval, projectName, userName, mobile, rotation, region, wordInterval);
                }
            } else if (pageRange == WatermarkEnum.JISHU.getCode()) {//奇数页面
                for (int i = 1; i < pdfPageNum; i = i + 2) {
                    rendering(pdfReader, i, pdfStamper, pdfGraPhicState, baseFont, fontSize, projectNameWaterMarkHeight, projectNameWatermarkWeight, userNameWaterMarkHeight, userNameWatermarkWeight, mobileWaterMarkHeight, mobileWatermarkWeight, heightInterval, projectName, userName, mobile, rotation, region, wordInterval);
                }
            } else if (pageRange == WatermarkEnum.OUSHU.getCode()) {//偶数页面
                for (int i = 2; i < pdfPageNum; i = i + 2) {
                    rendering(pdfReader, i, pdfStamper, pdfGraPhicState, baseFont, fontSize, projectNameWaterMarkHeight, projectNameWatermarkWeight, userNameWaterMarkHeight, userNameWatermarkWeight, mobileWaterMarkHeight, mobileWatermarkWeight, heightInterval, projectName, userName, mobile, rotation, region, wordInterval);
                }
            }
        }catch(Throwable throwable){
            LogUtil.error("WatermarkUtil_pdfAddWaterMark_t584", throwable);
            return "error";
        } finally {
            if (pdfStamper != null) {
                try{
                    pdfStamper.close();
                }catch(Throwable throwable){
                    LogUtil.error("WatermarkUtil_pdfAddWaterMark_e615", throwable, "关闭pdfStamper流异常");
                }
            }
            if (pdfReader != null) {
                try{
                    pdfReader.close();
                }catch(Throwable throwable){
                    LogUtil.error("WatermarkUtil_pdfAddWaterMark_e621", throwable, "关闭pdfReader流异常");
                }
            }
        }
        return "success";
    }


    /*
     * 2023-12-13
     *  pdf文件进行渲染
     **/
    private static final void rendering(final PdfReader pdfReader, final int i, final PdfStamper pdfStamper, final PdfGState pdfGraPhicState, final BaseFont baseFont, final Integer fontSize, final float projectNameWaterMarkHeight, final float projectNameWatermarkWeight, final float userNameWaterMarkHeight, final float userNameWatermarkWeight, final float mobileWaterMarkHeight, final float mobileWatermarkWeight, final float heightInterval, final String projectName, final String userName, final String mobile, final Integer rotation, final Integer region, final int wordInterval) {
        PdfContentByte pdfContent;
        com.itextpdf.text.Rectangle pageRectangle;
        // 获取当前页面矩形区域
        pageRectangle = pdfReader.getPageSizeWithRotation(i);
        // 获取当前页内容，getOverContent表示之后会在页面内容的上方加水印  getOverContent表示之后会在页面内容的下方加水印
        pdfContent = pdfStamper.getOverContent(i);//最上层
        //pdfContent = pdfStamper.getUnderContent(i);//最下层
        pdfContent.saveState();
        // 设置水印透明度
        pdfContent.setGState(pdfGraPhicState);
        // 开启写入文本
        pdfContent.beginText();
        // 设置字体 字号
        pdfContent.setFontAndSize(baseFont, fontSize);
        //水印字体颜色 默认黑色
        BaseColor baseColor = new BaseColor(0, 0, 0);
        pdfContent.setColorFill(baseColor);
        float pageHeight = pageRectangle.getHeight();//页面高
        float pageWidth = pageRectangle.getWidth();//页面宽
        if (region == WatermarkEnum.DANHANG.getCode()) {//单行
            row(projectNameWaterMarkHeight, projectNameWatermarkWeight, userNameWaterMarkHeight, userNameWatermarkWeight, mobileWaterMarkHeight, mobileWatermarkWeight, projectName, userName, mobile, rotation, wordInterval, pageWidth, pdfContent, pageHeight);
        } else if (region == WatermarkEnum.SHUANGHANG.getCode()) {//双行
            twinRow(projectNameWaterMarkHeight, projectNameWatermarkWeight, userNameWaterMarkHeight, userNameWatermarkWeight, mobileWaterMarkHeight, mobileWatermarkWeight, projectName, userName, mobile, rotation, wordInterval, pageHeight, pageWidth, pdfContent);
        } else if (region == WatermarkEnum.PUHAN.getCode()) {//平铺(铺满)
            overspread(projectNameWaterMarkHeight, projectNameWatermarkWeight, userNameWaterMarkHeight, userNameWatermarkWeight, mobileWaterMarkHeight, mobileWatermarkWeight, heightInterval, projectName, userName, mobile, rotation, wordInterval, pageHeight, pageWidth, pdfContent);
        }
        // 停止写入文本
        pdfContent.endText();
    }

    private static final void twinRow(final float projectNameWaterMarkHeight, final float projectNameWatermarkWeight, final float userNameWaterMarkHeight, final float userNameWatermarkWeight, final float mobileWaterMarkHeight, final float mobileWatermarkWeight, final String projectName, final String userName, final String mobile, final Integer rotation, final int wordInterval, final float pageHeight, final float pageWidth, final PdfContentByte pdfContent) {
        if (rotation == 45) {
            //系统名称 用户名 手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && !StringUtil.isBlank(mobile)) {
                int height = (int) (pageHeight / 3);
                for (int width = 0; width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight + 3 * wordInterval; width = (int) (width + mobileWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height + projectNameWaterMarkHeight + wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width, height, rotation);
                    width = (int) (width + userNameWatermarkWeight + wordInterval);
                    height = (int) (height + userNameWaterMarkHeight + wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width, height, rotation);
                    height = (int) (height + mobileWaterMarkHeight + wordInterval);
                }
                height = 0;
                for (int width = (int) (pageWidth / 3); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight + 3 * wordInterval; width = (int) (width + mobileWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height + projectNameWaterMarkHeight + wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width, height, rotation);
                    width = (int) (width + userNameWatermarkWeight + wordInterval);
                    height = (int) (height + userNameWaterMarkHeight + wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width, height, rotation);
                    height = (int) (height + mobileWaterMarkHeight + wordInterval);
                }
            }
            //系统名称
            if (!StringUtil.isBlank(projectName) && StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                int height = (int) (pageHeight / 3);
                for (int width = 0; width < pageWidth + projectNameWatermarkWeight + wordInterval; width = (int) (width + projectNameWatermarkWeight + wordInterval)) {
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    height = (int) (height + projectNameWaterMarkHeight + wordInterval);
                }
                height = 0;
                for (int width = (int) (pageWidth / 3); width < pageWidth + wordInterval; width = (int) (width + projectNameWatermarkWeight + wordInterval)) {
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    height = (int) (height + projectNameWaterMarkHeight + wordInterval);
                }
            }
            //系统名称 用户名
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                int height = (int) (pageHeight / 3);
                for (int width = 0; width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval; width = (int) (width + userNameWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height + projectNameWaterMarkHeight + wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width, height, rotation);
                    height = (int) (height + userNameWaterMarkHeight + wordInterval);
                }
                height = 0;
                for (int width = (int) (pageWidth / 3); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval; width = (int) (width + userNameWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height + projectNameWaterMarkHeight + wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width, height, rotation);
                    height = (int) (height + userNameWaterMarkHeight + wordInterval);
                }
            }
            //系统名称  手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(mobile) && StringUtil.isBlank(userName)) {
                int height = (int) (pageHeight / 3);
                for (int width = 0; width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval; width = (int) (width + mobileWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height + projectNameWaterMarkHeight + wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width, height, rotation);
                    height = (int) (height + mobileWaterMarkHeight + wordInterval);
                }
                height = 0;
                for (int width = (int) (pageWidth / 3); width < pageWidth + projectNameWatermarkWeight + mobileWatermarkWeight + 2 * wordInterval; width = (int) (width + mobileWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height + projectNameWaterMarkHeight + wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width, height, rotation);
                    height = (int) (height + mobileWaterMarkHeight + wordInterval);
                }

            }
        } else if (rotation == -45) {
            //系统名称 用户名 手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && !StringUtil.isBlank(mobile)) {
                int height = (int) (pageHeight - pageHeight / 3);
                for (int width = 0; width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight + 3 * wordInterval; width = (int) (width + mobileWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height - projectNameWaterMarkHeight - wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width, height, rotation);
                    width = (int) (width + userNameWatermarkWeight + wordInterval);
                    height = (int) (height - userNameWaterMarkHeight - wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width, height, rotation);
                    height = (int) (height - mobileWaterMarkHeight - wordInterval);
                }
                height = (int) pageHeight;
                for (int width = (int) (pageWidth / 3); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight + 3 * wordInterval; width = (int) (width + mobileWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height - projectNameWaterMarkHeight - wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width, height, rotation);
                    width = (int) (width + userNameWatermarkWeight + wordInterval);
                    height = (int) (height - userNameWaterMarkHeight - wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width, height, rotation);
                    height = (int) (height - mobileWaterMarkHeight - wordInterval);
                }
            }
            //系统名称
            if (!StringUtil.isBlank(projectName) && StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                int height = (int) (pageHeight - pageHeight / 3);
                for (int width = 0; width < pageWidth + projectNameWatermarkWeight + wordInterval; width = (int) (width + projectNameWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    height = (int) (height - projectNameWaterMarkHeight - wordInterval);

                }
                height = (int) pageHeight;
                for (int width = (int) (pageWidth / 3); width < pageWidth + projectNameWatermarkWeight + wordInterval; width = (int) (width + projectNameWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    height = (int) (height - projectNameWaterMarkHeight - wordInterval);
                }
            }
            //系统名称 用户名
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                int height = (int) (pageHeight - pageHeight / 3);
                for (int width = 0; width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval; width = (int) (width + userNameWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height - projectNameWaterMarkHeight - wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width, height, rotation);
                    height = (int) (height - userNameWaterMarkHeight - wordInterval);
                }
                height = (int) pageHeight;
                for (int width = (int) (pageWidth / 3); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight + 3 * wordInterval; width = (int) (width + userNameWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height - projectNameWaterMarkHeight - wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width, height, rotation);
                    height = (int) (height - userNameWaterMarkHeight - wordInterval);
                }
            }
            //系统名称  手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(mobile) && StringUtil.isBlank(userName)) {
                int height = (int) (pageHeight - pageHeight / 3);
                for (int width = 0; width < pageWidth + projectNameWatermarkWeight + mobileWatermarkWeight + 2 * wordInterval; width = (int) (width + mobileWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height - projectNameWaterMarkHeight - wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width, height, rotation);
                    height = (int) (height - mobileWaterMarkHeight - wordInterval);
                }
                height = (int) pageHeight;
                for (int width = (int) (pageWidth / 3); width < pageWidth + projectNameWatermarkWeight + mobileWatermarkWeight + 2 * wordInterval; width = (int) (width + mobileWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height - projectNameWaterMarkHeight - wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width, height, rotation);
                    height = (int) (height - mobileWaterMarkHeight - wordInterval);
                }
            }
        } else if (rotation == 0) {
            //系统名称 用户名 手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && !StringUtil.isBlank(mobile)) {
                for (int width = (int) (0 - projectNameWatermarkWeight - userNameWatermarkWeight - mobileWatermarkWeight - 3 * wordInterval); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight + 3 * wordInterval)) {
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, pageHeight / 3, rotation);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width + projectNameWatermarkWeight + wordInterval, pageHeight / 3, rotation);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval, pageHeight / 3, rotation);
                }
                for (int width = (int) (0 - projectNameWatermarkWeight - userNameWatermarkWeight - mobileWatermarkWeight - 3 * wordInterval); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight + 3 * wordInterval)) {
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, pageHeight - pageHeight / 3, rotation);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width + projectNameWatermarkWeight + wordInterval, pageHeight - pageHeight / 3, rotation);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval, pageHeight - pageHeight / 3, rotation);
                }
            }
            //系统名称
            if (!StringUtil.isBlank(projectName) && StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                for (int width = (int) (0 - projectNameWatermarkWeight - wordInterval); width < pageWidth + projectNameWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + wordInterval)) {
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, pageHeight / 3, rotation);
                }
                for (int width = (int) (0 - projectNameWatermarkWeight - wordInterval); width < pageWidth + projectNameWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + wordInterval)) {
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, pageHeight - pageHeight / 3, rotation);
                }
            }
            //系统名称 用户名
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                for (int width = (int) (0 - projectNameWatermarkWeight - userNameWatermarkWeight - 2 * wordInterval); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval)) {
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, pageHeight / 3, rotation);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width + projectNameWatermarkWeight + wordInterval, pageHeight / 3, rotation);
                }
                for (int width = (int) (0 - projectNameWatermarkWeight - userNameWatermarkWeight - 2 * wordInterval); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval)) {
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, pageHeight - pageHeight / 3, rotation);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width + projectNameWatermarkWeight + wordInterval, pageHeight - pageHeight / 3, rotation);
                }
            }
            //系统名称  手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(mobile) && StringUtil.isBlank(userName)) {
                for (int width = (int) (0 - projectNameWatermarkWeight - mobileWatermarkWeight - 2 * wordInterval); width < pageWidth + projectNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + mobileWatermarkWeight + 2 * wordInterval)) {
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, pageHeight / 3, rotation);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width + projectNameWatermarkWeight + wordInterval, pageHeight / 3, rotation);
                }
                for (int width = (int) (0 - projectNameWatermarkWeight - mobileWatermarkWeight - 2 * wordInterval); width < pageWidth + projectNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + mobileWatermarkWeight + 2 * wordInterval)) {
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, pageHeight - pageHeight / 3, rotation);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width + projectNameWatermarkWeight + wordInterval, pageHeight - pageHeight / 3, rotation);
                }
            }
        }
    }

    private static final void row(final float projectNameWaterMarkHeight, final float projectNameWatermarkWeight, final float userNameWaterMarkHeight, final float userNameWatermarkWeight, final float mobileWaterMarkHeight, final float mobileWatermarkWeight, final String projectName, final String userName, final String mobile, final Integer rotation, final int wordInterval, final float pageWidth, final PdfContentByte pdfContent, final float pageHeight) {
        if (rotation == 45) {
            //系统名称 用户名 手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && !StringUtil.isBlank(mobile)) {
                int height = 0;
                for (int width = 0; width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight + 3 * wordInterval; width = (int) (width + mobileWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height + projectNameWaterMarkHeight + wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width, height, rotation);
                    width = (int) (width + userNameWatermarkWeight + wordInterval);
                    height = (int) (height + userNameWaterMarkHeight + wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width, height, rotation);
                    height = (int) (height + mobileWaterMarkHeight + wordInterval);
                }
            }
            //系统名称
            if (!StringUtil.isBlank(projectName) && StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                int height = 0;
                for (int width = 0; width < pageWidth + projectNameWatermarkWeight + wordInterval; width = (int) (width + projectNameWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    height = (int) (height + projectNameWaterMarkHeight + wordInterval);
                }
            }
            //系统名称 用户名
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                int height = 0;
                for (int width = 0; width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval; width = (int) (width + userNameWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height + projectNameWaterMarkHeight + wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width, height, rotation);
                    height = (int) (height + userNameWaterMarkHeight + wordInterval);
                }
            }
            //系统名称  手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(mobile) && StringUtil.isBlank(userName)) {
                int height = 0;
                for (int width = 0; width < pageWidth + projectNameWatermarkWeight + mobileWatermarkWeight + 2 * wordInterval; width = (int) (width + mobileWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height + projectNameWaterMarkHeight + wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width, height, rotation);
                    height = (int) (height + mobileWaterMarkHeight + wordInterval);
                }
            }
        } else if (rotation == -45) {
            //系统名称 用户名 手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && !StringUtil.isBlank(mobile)) {
                int height = (int) pageHeight;
                for (int width = 0; width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight + 3 * wordInterval; width = (int) (width + mobileWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height - projectNameWaterMarkHeight - wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width, height, rotation);
                    width = (int) (width + userNameWatermarkWeight + wordInterval);
                    height = (int) (height - userNameWaterMarkHeight - wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width, height, rotation);
                    height = (int) (height - mobileWaterMarkHeight - wordInterval);
                }
            }
            //系统名称
            if (!StringUtil.isBlank(projectName) && StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                int height = (int) pageHeight;
                for (int width = 0; width < pageWidth + projectNameWatermarkWeight + wordInterval; width = (int) (width + projectNameWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    height = (int) (height - projectNameWaterMarkHeight - wordInterval);
                }
            }
            //系统名称 用户名
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                int height = (int) pageHeight;
                for (int width = 0; width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval; width = (int) (width + userNameWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height - projectNameWaterMarkHeight - wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width, height, rotation);
                    height = (int) (height - userNameWaterMarkHeight - wordInterval);
                }
            }
            //系统名称  手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(mobile) && StringUtil.isBlank(userName)) {
                int height = (int) pageHeight;
                for (int width = 0; width < pageWidth + projectNameWatermarkWeight + mobileWatermarkWeight + 3 * wordInterval; width = (int) (width + mobileWatermarkWeight + wordInterval)) {
                    // 添加水印文字并旋转角度
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    width = (int) (width + projectNameWatermarkWeight + wordInterval);
                    height = (int) (height - projectNameWaterMarkHeight - wordInterval);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width, height, rotation);
                    height = (int) (height - mobileWaterMarkHeight - wordInterval);
                }
            }

        } else if (rotation == 0) {
            //系统名称 用户名 手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && !StringUtil.isBlank(mobile)) {
                for (int width = (int) (0 - projectNameWatermarkWeight - userNameWatermarkWeight - mobileWatermarkWeight - 3 * wordInterval); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight + 3 * wordInterval)) {
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, pageHeight / 2, rotation);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width + projectNameWatermarkWeight + wordInterval, pageHeight / 2, rotation);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval, pageHeight / 2, rotation);
                }
            }
            //系统名称
            if (!StringUtil.isBlank(projectName) && StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                for (int width = (int) (0 - projectNameWatermarkWeight - wordInterval); width < pageWidth + projectNameWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + wordInterval)) {
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, pageHeight / 2, rotation);
                }
            }
            //系统名称 用户名
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                for (int width = (int) (0 - projectNameWatermarkWeight - userNameWatermarkWeight - 2 * wordInterval); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval)) {
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, pageHeight / 2, rotation);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width + projectNameWatermarkWeight + wordInterval, pageHeight / 2, rotation);
                }
            }
            //系统名称  手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(mobile) && StringUtil.isBlank(userName)) {
                for (int width = (int) (0 - projectNameWatermarkWeight - userNameWatermarkWeight - mobileWatermarkWeight - 3 * wordInterval); width < pageWidth + projectNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + mobileWatermarkWeight + 2 * wordInterval)) {
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, pageHeight / 2, rotation);
                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width + projectNameWatermarkWeight + wordInterval, pageHeight / 2, rotation);
                }
            }

        }
    }

    private static final void overspread(final float projectNameWaterMarkHeight, final float projectNameWatermarkWeight, final float userNameWaterMarkHeight, final float userNameWatermarkWeight, final float mobileWaterMarkHeight, final float mobileWatermarkWeight, final float heightInterval, final String projectName, final String userName, final String mobile, final Integer rotation, final int wordInterval, final float pageHeight, final float pageWidth, final PdfContentByte pdfContent) {
        // 在高度和宽度维度每隔heightInterval距离添加一个水印
        if (rotation == 45) {
            //系统名称 用户名 手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && !StringUtil.isBlank(mobile)) {
//                for (float height = (int) (0 ); height < pageHeight +heightInterval; height = (int) (height + heightInterval)) {
//                    for (float width = (int) (0 ); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width =width +projectNameWatermarkWeight+userNameWatermarkWeight+mobileWatermarkWeight + 3*wordInterval) {
//                        pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
//                        System.out.println("width1 = " + width+"  height1 = " + height);
//                        pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width + projectNameWatermarkWeight + wordInterval, height + projectNameWaterMarkHeight + wordInterval , rotation);
//                        System.out.println("width2 = " + (width + projectNameWatermarkWeight + wordInterval)+"  height2 = " + (height + projectNameWaterMarkHeight + wordInterval));
//                        pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width + projectNameWatermarkWeight+userNameWatermarkWeight + 2*wordInterval, height+ projectNameWaterMarkHeight+userNameWaterMarkHeight +2*wordInterval , rotation);
//                        System.out.println("width3 = " + (width + projectNameWatermarkWeight+userNameWatermarkWeight + 2*wordInterval)+"  height3 = " + (height+ projectNameWaterMarkHeight+userNameWaterMarkHeight +2*wordInterval));
//                    }
//                }
                for (int width = (int) (0 - pageWidth - pageHeight); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + heightInterval)) {
                    int height = 0;
                    for (int w = width; w < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight + 3 * wordInterval; w = (int) (w + mobileWatermarkWeight + wordInterval)) {
                        // 添加水印文字并旋转角度
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, w, height, rotation);
                        w = (int) (w + projectNameWatermarkWeight + wordInterval);
                        height = (int) (height + projectNameWaterMarkHeight + wordInterval);
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, w, height, rotation);
                        w = (int) (w + userNameWatermarkWeight + wordInterval);
                        height = (int) (height + userNameWaterMarkHeight + wordInterval);
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, w, height, rotation);
                        height = (int) (height + mobileWaterMarkHeight + wordInterval);
                    }
                }
            }
            //系统名称
            if (!StringUtil.isBlank(projectName) && StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                for (int width = (int) (0 - pageWidth - pageHeight); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + heightInterval)) {
                    int height = 0;
                    for (int w = width; w < pageWidth + projectNameWatermarkWeight + wordInterval; w = (int) (w + projectNameWatermarkWeight + wordInterval)) {
                        // 添加水印文字并旋转角度
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, w, height, rotation);
                        height = (int) (height + projectNameWaterMarkHeight + wordInterval);
                    }
                }
            }
            //系统名称 用户名
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                for (int width = (int) (0 - pageWidth - pageHeight); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + heightInterval)) {
                    int height = 0;
                    for (int w = width; w < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval; w = (int) (w + userNameWatermarkWeight + wordInterval)) {
                        // 添加水印文字并旋转角度
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, w, height, rotation);
                        w = (int) (w + projectNameWatermarkWeight + wordInterval);
                        height = (int) (height + projectNameWaterMarkHeight + wordInterval);
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, w, height, rotation);
                        height = (int) (height + userNameWaterMarkHeight + wordInterval);
                    }
                }
            }
            //系统名称  手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(mobile) && StringUtil.isBlank(userName)) {
                for (int width = (int) (0 - pageWidth - pageHeight); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + heightInterval)) {
                    int height = 0;
                    for (int w = width; w < pageWidth + projectNameWatermarkWeight + mobileWatermarkWeight + 2 * wordInterval; w = (int) (w + mobileWatermarkWeight + wordInterval)) {
                        // 添加水印文字并旋转角度
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, w, height, rotation);
                        w = (int) (w + projectNameWatermarkWeight + wordInterval);
                        height = (int) (height + projectNameWaterMarkHeight + wordInterval);
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, w, height, rotation);
                        height = (int) (height + mobileWaterMarkHeight + wordInterval);
                    }
                }
            }

        } else if (rotation == -45) {
//            for (int height = (int) (0); height < pageHeight + projectNameWaterMarkHeight + userNameWaterMarkHeight + mobileWaterMarkHeight + heightInterval; height = (int) (height + heightInterval)) {
//                for (int width = (int) (0); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight + 3 * wordInterval)) {
//                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
//                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width + projectNameWatermarkWeight + wordInterval, height - projectNameWaterMarkHeight - wordInterval, rotation);
//                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval, height - projectNameWaterMarkHeight - userNameWaterMarkHeight - 2 * wordInterval, rotation);
//                }
//            }
            //系统名称 用户名 手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && !StringUtil.isBlank(mobile)) {
                for (int width = (int) (0 - pageWidth - pageHeight); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + heightInterval)) {
                    int height = (int) pageHeight;
                    for (int w = width; w < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight + 3 * wordInterval; w = (int) (w + mobileWatermarkWeight + wordInterval)) {
                        // 添加水印文字并旋转角度
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, w, height, rotation);
                        w = (int) (w + projectNameWatermarkWeight + wordInterval);
                        height = (int) (height - projectNameWaterMarkHeight - wordInterval);
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, w, height, rotation);
                        w = (int) (w + userNameWatermarkWeight + wordInterval);
                        height = (int) (height - userNameWaterMarkHeight - wordInterval);
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, w, height, rotation);
                        height = (int) (height - mobileWaterMarkHeight - wordInterval);
                    }
                }
            }
            //系统名称
            if (!StringUtil.isBlank(projectName) && StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                for (int width = (int) (0 - pageWidth - pageHeight); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + heightInterval)) {
                    int height = (int) pageHeight;
                    for (int w = width; w < pageWidth + projectNameWatermarkWeight + wordInterval; w = (int) (w + projectNameWatermarkWeight + wordInterval)) {
                        // 添加水印文字并旋转角度
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, w, height, rotation);
                        height = (int) (height - projectNameWaterMarkHeight - wordInterval);
                    }
                }
            }
            //系统名称 用户名
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                for (int width = (int) (0 - pageWidth - pageHeight); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + heightInterval)) {
                    int height = (int) pageHeight;
                    for (int w = width; w < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval; w = (int) (w + userNameWatermarkWeight + wordInterval)) {
                        // 添加水印文字并旋转角度
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, w, height, rotation);
                        w = (int) (w + projectNameWatermarkWeight + wordInterval);
                        height = (int) (height - projectNameWaterMarkHeight - wordInterval);
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, w, height, rotation);
                        height = (int) (height - userNameWaterMarkHeight - wordInterval);
                    }
                }
            }
            //系统名称  手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(mobile) && StringUtil.isBlank(userName)) {
                for (int width = (int) (0 - pageWidth - pageHeight); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + heightInterval)) {
                    int height = (int) pageHeight;
                    for (int w = width; w < pageWidth + projectNameWatermarkWeight + mobileWatermarkWeight + 3 * wordInterval; w = (int) (w + mobileWatermarkWeight + wordInterval)) {
                        // 添加水印文字并旋转角度
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, w, height, rotation);
                        w = (int) (w + projectNameWatermarkWeight + wordInterval);
                        height = (int) (height - projectNameWaterMarkHeight - wordInterval);
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, w, height, rotation);
                        height = (int) (height - mobileWaterMarkHeight - wordInterval);
                    }
                }
            }
        } else if (rotation == 0) {
//            for (int height = (int) (0 - projectNameWaterMarkHeight - heightInterval); height < pageHeight + projectNameWaterMarkHeight + heightInterval; height = (int) (height + heightInterval + projectNameWaterMarkHeight)) {
//                for (int width = (int) (0 - projectNameWatermarkWeight - userNameWatermarkWeight - mobileWatermarkWeight - 3 * wordInterval); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight + 3 * wordInterval)) {
//                    pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
//                    pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width + projectNameWatermarkWeight + wordInterval, height, rotation);
//                    pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval, height, rotation);
//                }
//            }
            //系统名称 用户名 手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && !StringUtil.isBlank(mobile)) {
                for (int height = (int) (0 - projectNameWaterMarkHeight - heightInterval); height < pageHeight + projectNameWaterMarkHeight + heightInterval; height = (int) (height + heightInterval + projectNameWaterMarkHeight)) {
                    for (int width = (int) (0 - projectNameWatermarkWeight - userNameWatermarkWeight - mobileWatermarkWeight - 3 * wordInterval); width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight + 3 * wordInterval)) {
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width + projectNameWatermarkWeight + wordInterval, height, rotation);
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval, height, rotation);
                    }
                }
            }
            //系统名称
            if (!StringUtil.isBlank(projectName) && StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                for (int height = (int) (0 - projectNameWaterMarkHeight - heightInterval); height < pageHeight + projectNameWaterMarkHeight + heightInterval; height = (int) (height + heightInterval + projectNameWaterMarkHeight)) {
                    for (int width = 0; width < pageWidth + projectNameWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + wordInterval)) {
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                    }
                }
            }
            //系统名称 用户名
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(userName) && StringUtil.isBlank(mobile)) {
                for (int height = (int) (0 - projectNameWaterMarkHeight - heightInterval); height < pageHeight + projectNameWaterMarkHeight + heightInterval; height = (int) (height + heightInterval + projectNameWaterMarkHeight)) {
                    for (int width = 0; width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + userNameWatermarkWeight + 2 * wordInterval)) {
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, userName, width + projectNameWatermarkWeight + wordInterval, height, rotation);
                    }
                }
            }
            //系统名称  手机号
            if (!StringUtil.isBlank(projectName) && !StringUtil.isBlank(mobile) && StringUtil.isBlank(userName)) {
                for (int height = (int) (0 - projectNameWaterMarkHeight - heightInterval); height < pageHeight + projectNameWaterMarkHeight + heightInterval; height = (int) (height + heightInterval + projectNameWaterMarkHeight)) {
                    for (int width = 0; width < pageWidth + projectNameWatermarkWeight + userNameWatermarkWeight + mobileWatermarkWeight; width = (int) (width + projectNameWatermarkWeight + mobileWatermarkWeight + 2 * wordInterval)) {
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, projectName, width, height, rotation);
                        pdfContent.showTextAligned(Element.ALIGN_LEFT, mobile, width + projectNameWatermarkWeight + wordInterval, height, rotation);
                    }
                }

            }
        }
    }

    /*
     * 2023-12-13
     *  获取字体
     **/
    private static final BaseFont getFont(final Integer font) throws Throwable {
        //默认微软雅黑
//        BaseFont baseFont = BaseFont.createFont("C:/Windows/Fonts/msyh.ttc,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        BaseFont baseFont = null;
        if (font == WatermarkEnum.WEIRUANYAHEI.getCode()) {//微软雅黑
            if (EnvironmentUtil.isWindows()) {
                baseFont = BaseFont.createFont("C:/Windows/Fonts/msyh.ttc,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } else if (EnvironmentUtil.isLinux()) {
                baseFont = BaseFont.createFont("/usr/share/fonts/chinese/Truetype/msyh.ttc", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
        } else if (font == WatermarkEnum.SONGTI.getCode()) {//宋体
            if (EnvironmentUtil.isWindows()) {
                baseFont = BaseFont.createFont("C:/Windows/Fonts/simsun.ttc,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } else if (EnvironmentUtil.isLinux()) {
                baseFont = BaseFont.createFont("/usr/share/fonts/chinese/Truetype/simsun.ttc", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
        } else if (font == WatermarkEnum.HEITI.getCode()) {//黑体
            if (EnvironmentUtil.isWindows()) {
                baseFont = BaseFont.createFont("C:/Windows/Fonts/simhei.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } else if (EnvironmentUtil.isLinux()) {
                baseFont = BaseFont.createFont("/usr/share/fonts/chinese/Truetype/simhei.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
        } else if (font == WatermarkEnum.FANGSONG.getCode()) {//仿宋
            if (EnvironmentUtil.isWindows()) {
                baseFont = BaseFont.createFont("C:/Windows/Fonts/simfang.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } else if (EnvironmentUtil.isLinux()) {
                baseFont = BaseFont.createFont("/usr/share/fonts/chinese/Truetype/simfang.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
        }
        return baseFont;
    }

}
