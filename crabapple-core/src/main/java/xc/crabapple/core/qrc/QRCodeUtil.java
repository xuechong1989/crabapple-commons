package xc.crabapple.core.qrc;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.url.UrlUtil;

import javax.imageio.ImageIO;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

/**
 * 2020-10-18 6.7.0
 * 二维码生成与管理类
 */
public final class QRCodeUtil {

    /**
     * 2020-10-22 6.8.0
     * 构建与解析二维码
     */
    public static final void create(final QRCodeBean qrCodeBean){
        String returnType = qrCodeBean.getReturnType();
        //本地
        if("local".equals(returnType)){
            p_createLocal(qrCodeBean);
            return;
        }
        //网络流
        if("net".equals(returnType)){
            p_createNet(qrCodeBean);
            return;
        }
    }

    /**
     * 2020-10-22 6.8.0
     * @param qrCodeUrl 本地二维码图片文件的绝对路径
     * 解析二维码，返回二维码对应的字符串内容
     */
    public static final String getContent(final String qrCodeUrl){
        File file = new File(qrCodeUrl);
        BufferedImage bufferedImage;
        try{
            bufferedImage = ImageIO.read(file);
        }catch(Throwable throwable){
            LogUtil.error("QRCodeUtil_getContent_t1", throwable, qrCodeUrl);
            return "";
        }
        BufferedImageLuminanceSource bufferedImageLuminanceSource = new BufferedImageLuminanceSource(bufferedImage);
        HybridBinarizer hybridBinarizer = new HybridBinarizer(bufferedImageLuminanceSource);
        BinaryBitmap bitmap = new BinaryBitmap(hybridBinarizer);
        HashMap<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        MultiFormatReader multiFormatReader = new MultiFormatReader();
        try{
            Result result = multiFormatReader.decode(bitmap, hints);
            String content = result.getText();
            return content;
        }catch(Throwable throwable){
            LogUtil.error("QRCodeUtil_getContent_e2", throwable);
            return "";
        }
    }

    /**
     * 2020-10-22 6.8.0
     * 本地创建
     */
    private static final void p_createLocal(final QRCodeBean qrCodeBean){
        BufferedImage bufferedImage = p_bufferedImage(qrCodeBean);
        //插入logo图片
        String logoUrl = qrCodeBean.getLogoUrl();
        if(!StringUtil.isBlank(logoUrl)){
            p_insertLogo(qrCodeBean, bufferedImage);
        }
        //写入本地
        String qrCodeUrl = qrCodeBean.getQrCodeUrl();
        String qrCodePath = UrlUtil.getPathByUrl(qrCodeUrl);
        FileUtil.dirMake(qrCodePath);
        File qrCodeFile = new File(qrCodeUrl);
        try{
            ImageIO.write(bufferedImage, "jpg", qrCodeFile);
        }catch(Throwable throwable){
            LogUtil.error("QRCodeUtil_p_createLocal_e1", throwable);
        }
    }

    /**
     * 2020-10-22 6.8.0
     * 网络流
     */
    private static final void p_createNet(final QRCodeBean qrCodeBean){
        BufferedImage bufferedImage = p_bufferedImage(qrCodeBean);
        //插入logo图片
        String logoUrl = qrCodeBean.getLogoUrl();
        if(!StringUtil.isBlank(logoUrl)){
            p_insertLogo(qrCodeBean, bufferedImage);
        }
        //写入网络流
        try{
            HttpServletResponse response = qrCodeBean.getResponse();
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");  //不设置缓存
            response.setDateHeader("Expires", 0);
            response.setContentType("image/png");
            ServletOutputStream servletOutputStream = response.getOutputStream();
            ImageIO.write(bufferedImage, "png", servletOutputStream);
        }catch(Throwable throwable){
            LogUtil.error("QRCodeUtil_p_createNet_e1", throwable);
        }
    }

    private static final void p_insertLogo(final QRCodeBean qrCodeBean, final BufferedImage bufferedImage){
        Integer width = qrCodeBean.getWidth();
        Integer height = qrCodeBean.getHeight();
        String logoUrl = qrCodeBean.getLogoUrl();
        Integer logoWidth = qrCodeBean.getLogoWidth();
        Integer logoHeight = qrCodeBean.getLogoHeight();
        Float logoArcw = qrCodeBean.getLogoArcw();
        Float logoArch = qrCodeBean.getLogoArch();
        String needCompress = qrCodeBean.getNeedCompress();
        //
        File logoFile = new File(logoUrl);
        if(!logoFile.exists()){
            LogUtil.error("QRCodeUtil_p_insertLogo_1", "file_not_found", logoUrl);
            return;
        }
        Image logoImage = null;
        try{
            logoImage = ImageIO.read(logoFile);
        }catch(Throwable throwable){
            LogUtil.error("QRCodeUtil_p_insertLogo_t1", throwable, logoUrl);
            return;
        }
        int logoWWW = logoImage.getWidth(null);
        int logoHHH = logoImage.getHeight(null);
        if("yes".equals(needCompress)){  //压缩logo图片
            if(logoWWW > logoWidth){
                logoWWW = logoWidth;
            }
            if(logoHHH > logoHeight){
                logoHHH = logoHeight;
            }
            Image image = logoImage.getScaledInstance(logoWWW, logoHHH, Image.SCALE_SMOOTH);
            BufferedImage tag = new BufferedImage(logoWWW, logoHHH, BufferedImage.TYPE_INT_RGB);
            Graphics g = tag.getGraphics();
            g.drawImage(image, 0, 0, null); // 绘制缩小后的图
            g.dispose();
            logoImage = image;
        }
        Graphics2D graphics2D = bufferedImage.createGraphics();
        int x = (width - logoWWW) / 2;
        int y = (height - logoHHH) / 2;
        graphics2D.drawImage(logoImage, x, y, logoWWW, logoHHH, null);
        Shape shape = new RoundRectangle2D.Float(x, y, logoWWW, logoWWW, logoArcw, logoArch);
        BasicStroke basicStroke = new BasicStroke(3F);
        graphics2D.setStroke(basicStroke);
        graphics2D.draw(shape);
        graphics2D.dispose();
    }

    private static final BufferedImage p_bufferedImage(final QRCodeBean qrCodeBean){
        String content = qrCodeBean.getContent();
        String qrCodeUrl = qrCodeBean.getQrCodeUrl();
        Integer margin = qrCodeBean.getMargin();
        Integer width = qrCodeBean.getWidth();
        Integer height = qrCodeBean.getHeight();
        Integer rgbCode = qrCodeBean.getRgbCode();
        Integer rgbBackground = qrCodeBean.getRgbBackground();
        ErrorCorrectionLevel errorCorrectionLevel = qrCodeBean.getErrorCorrectionLevel();
        //
        HashMap<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, margin);  //边框距
        hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);  //矫错级别
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        BitMatrix bitMatrix = null;
        try{
            bitMatrix = multiFormatWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        }catch(Throwable throwable){
            LogUtil.error("QRCodeUtil_p_bufferedImage_t1", throwable, qrCodeUrl);
            return null;
        }
        int www = bitMatrix.getWidth();
        int hhh = bitMatrix.getHeight();
        BufferedImage bufferedImage = new BufferedImage(www, hhh, BufferedImage.TYPE_INT_RGB);
        for(int x = 0; x < www; x++){
            for(int y = 0; y < hhh; y++){
                boolean flag = bitMatrix.get(x, y);
                bufferedImage.setRGB(x, y, flag ? rgbCode : rgbBackground);
            }
        }
        return bufferedImage;
    }

}
