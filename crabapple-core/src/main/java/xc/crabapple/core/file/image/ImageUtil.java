package xc.crabapple.core.file.image;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import net.coobird.thumbnailator.name.Rename;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.Base64Util;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.math.DoubleUtil;
import xc.crabapple.core.url.UrlUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Base64;
import java.util.Base64.Decoder;

/**
 * 2017-10-31 1.0.0
 * 文件生成缩略图
 * 图片与base64互转
 * https://www.xuebuyuan.com/3229489.html
 * https://blog.csdn.net/zmx729618/article/details/78729049
 */
public final class ImageUtil {

    /**
     * 2025-02-08
     * 创建一个背景透明的png图片文件
     * @param toPngUrl
     * @return
     */
    public static final boolean createPng(final String text, final String toPngUrl){
        final boolean flag = createPng(text, 100, 20, "SimSun", 14, Color.RED, toPngUrl);
        return flag;
    }

    /**
     * 2025-02-08
     * 创建一个背景透明的png图片文件
     * @param width
     * @param toPngUrl
     * @return
     */
    public static final boolean createPng(final String text, final Integer width, final String toPngUrl){
        final boolean flag = createPng(text, width, 30, "SimSun", 20, Color.RED, toPngUrl);
        return flag;
    }

    /**
     * 2025-02-08
     * 创建一个背景透明的png图片文件
     * @param width
     * @param height
     * @param toPngUrl
     * @return
     */
    public static final boolean createPng(final String text, final Integer width, final Integer height, final String toPngUrl){
        final boolean flag = createPng(text, width, height, "SimSun", 20, Color.RED, toPngUrl);
        return flag;
    }

    /**
     * 2025-02-08
     * 创建一个背景透明的png图片文件
     * @param width
     * @param height
     * @param fontName
     * @param toPngUrl
     * @return
     */
    public static final boolean createPng(final String text, final Integer width, final Integer height, final String fontName, final String toPngUrl){
        final boolean flag = createPng(text, width, height, fontName, 20, Color.RED, toPngUrl);
        return flag;
    }

    /**
     * 2025-02-08
     * 创建一个背景透明的png图片文件
     * @param width
     * @param height
     * @param fontName
     * @param fontSize
     * @param toPngUrl
     * @return
     */
    public static final boolean createPng(final String text, final Integer width, final Integer height, final String fontName, final Integer fontSize, final String toPngUrl){
        final boolean flag = createPng(text, width, height, fontName, fontSize, Color.RED, toPngUrl);
        return flag;
    }

    /**
     * 2025-02-08
     * 创建一个背景透明的png图片文件
     * @param width
     * @param height
     * @param fontName
     * @param fontSize
     * @param fontColor
     * @param toPngUrl
     * @return
     */
    public static final boolean createPng(final String text, final Integer width, final Integer height, final String fontName, final Integer fontSize, final Color fontColor, final String toPngUrl){
        String textLocal = text;
        if(StringUtil.isBlank(textLocal)){
            textLocal = "雪地漫步2020";
        }
        Integer widthLocal = width;
        if(widthLocal == null || widthLocal <= 0){
            widthLocal = 130;
        }
        Integer heightLocal = height;
        if(heightLocal == null || heightLocal <= 0){
            heightLocal = 130;
        }
        String fontNameLocal = fontName;
        if(StringUtil.isBlank(fontNameLocal)){
            fontNameLocal = "SimSun";
        }
        Integer fontSizeLocal = fontSize;
        if(fontSizeLocal == null || fontSizeLocal <= 0){
            fontSizeLocal = 20;
        }
        Color fontColorLocal = fontColor;
        if(fontColorLocal == null){
            fontColorLocal = Color.RED;
        }
        // 创建一个背景透明的 BufferedImage 对象
        BufferedImage bufferedImage = new BufferedImage(widthLocal, heightLocal, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        try {
            // 设置抗锯齿，使绘制的文字更加平滑
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // 设置字体和颜色
            Font font = new Font(fontName, Font.BOLD, fontSize);
            graphics2D.setFont(font);
            graphics2D.setColor(fontColor);
            // 计算文本的宽度和高度
            FontMetrics metrics = graphics2D.getFontMetrics(font);
            int textWidth = metrics.stringWidth(text);
            int textHeight = metrics.getHeight();
            // 计算文本的绘制位置，使文本居中显示
            int x = (width - textWidth) / 2;
            int y = (height - textHeight) / 2 + metrics.getAscent();
            // 在图片上绘制文本
            graphics2D.drawString(text,  x, y);
            File pngFile = new File(toPngUrl);
            ImageIO.write(bufferedImage,  "png", pngFile);
            return true;
        } catch (Throwable throwable) {
            LogUtil.error("ImageUtil_createPng_t1", throwable);
            return false;
        } finally {
            // 释放图形上下文资源
            graphics2D.dispose();
        }
    }

    /**
     * 2023-05-16 根据base64前缀获取文件格式
     * 示例 data:image/jpeg;base64, ==> jpeg
     * @param base64str
     * @return
     */
    public static final String getImgSuffix(final String base64str){
        if(!StringUtil.containOf(base64str, ";base64,")){
            return "";
        }
        String[] array = StringUtil.split(base64str,";base64,");
        String first = array[0];
        String[] arr = StringUtil.split(first, "/");
        String suffix = arr[1];
        return suffix;
    }

    /**
     * 2023-04-19
     * 图片文件转换为base64字符串
     * @param imgUrl 图片绝对路径
     */
    public static final String img2base64(final String imgUrl){
        String base64 = img2base64(imgUrl, true);
        return base64;
    }
    
    /**
     * 2020-05-19 6.0.0
     * 图片文件转换为base64字符串
     * 在base64标准中是不包括前缀data:image/jpeg;base64,的
     * @param imgUrl 图片绝对路径
     * @param prefix 是否包含前缀'data:image/jpeg;base64,'，true-包含，false-不包含，html的img标签的src属性值可直接使用该值
     */
    public static final String img2base64(final String imgUrl, final boolean prefix){
        File imgFile = new File(imgUrl);
        if(!imgFile.exists()){
            LogUtil.error("ImageUtil_img2base64_1", "imgFile_is_not_exist", imgUrl, prefix);
            return "error-1";
        }
        if(!imgFile.isFile()){
            LogUtil.error("ImageUtil_img2base64_2", "imgFile_is_not_file", imgUrl, prefix);
            return "error-2";
        }
        String base64type = "jpeg";
        String suffix = FileUtil.getFileType(imgUrl);
        suffix = StringUtil.toLowerCase(suffix);
        if("jpg".equals(suffix)){
            base64type = "jpeg";
            suffix = "jpg";
        }else if("jpeg".equals(suffix)){
            base64type = "jpeg";
            suffix = "jpg";
        }else if("png".equals(suffix)){
            base64type = "png";
            suffix = "png";
        }else if("gif".equals(suffix)){
            base64type = "gif";
            suffix = "jpg";
        }else if("bmp".equals(suffix)){
            base64type = "bmp";
            suffix = "jpg";
        }else if("ico".equals(suffix)){
            base64type = "x-icon";
            suffix = "png";
        }else{
            base64type = "jpg";
            suffix = "jpg";
        }
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            BufferedImage bufferedImage = ImageIO.read(imgFile);
            byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, suffix, byteArrayOutputStream);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            String base64 = Base64Util.encodeBytes2str(bytes);
            base64 = StringUtil.trim(base64);
            if(prefix){
                base64 = "data:image/"+base64type+";base64," + base64;
                return base64;
            }
            return base64;
        }catch(Throwable throwable){
            LogUtil.error("ImageUtil_img2base64_t1", throwable, imgUrl, prefix);
            return "error-3";
        }finally {
            IOUtil.close(byteArrayOutputStream);
        }
    }

    /**
     * 2020-05-19 6.0.0
     * 2023-05-09 增加base64格式的兼容性
     * base64字符串转换为图片文件
     * 在base64标准中是不包括前缀data:image/jpeg;base64,的
     * @param base64str base64内容，无论是否以data:image/开头均可
     * @param imgUrl 图片绝对路径
     */
    public static final void base642img(final String base64str, final String imgUrl){
        String base64strLocal = base64str;
        String fileType = "";
        if(StringUtil.startsWith(base64strLocal, "data:image/")){
            String[] array = StringUtil.split(base64strLocal, ";base64,");
            base64strLocal = array[1];
            //
            String prefix = array[0];  // 格式如 data:image/png
            String[] arr = StringUtil.split(prefix, "/");
            fileType = arr[1];
        }else{
            fileType = FileUtil.getFileType(imgUrl);
            if(StringUtil.isBlank(fileType)){
                fileType = "jpg";
            }else if(StringUtil.equals(fileType, "ico")){
                fileType = "png";
            }
        }
        //
        Decoder decoder = Base64.getDecoder();
        ByteArrayInputStream byteArrayInputStream = null;
        String imgPath = UrlUtil.getPathByLevel(imgUrl, 1);
        FileUtil.dirMake(imgPath);
        try {
            byte[] bytes1 = decoder.decode(base64strLocal);
            byteArrayInputStream = new ByteArrayInputStream(bytes1);
            BufferedImage bufferedImage = ImageIO.read(byteArrayInputStream);
            File imgFile = new File(imgUrl);
            ImageIO.write(bufferedImage, fileType, imgFile);
        }catch(Throwable throwable){
            LogUtil.error("ImageUtil_base642img_t1", throwable, imgUrl);
        }finally{
            IOUtil.close(byteArrayInputStream);
        }
    }

    /**
     * 使用给定的图片生成指定大小的图片
     */
    public static final void test1(){
        try{
            Thumbnails.of("data/meinv.jpg").size(80,80).toFile("data/newmeinv.jpg");
        }catch(Throwable throwable){
            LogUtil.error("XXX_yyy_t1", throwable);
        }
    }

    /**
     * 对原图加水印,然后顺时针旋转90度,最后压缩为80%保存
     */
    public static final void test2(){
        try{
            Thumbnails.of("data/abc.jpg").
                    size(160,160). // 缩放大小
                    rotate(90). // 顺时针旋转90度
                    watermark(Positions.BOTTOM_RIGHT, ImageIO.read(new File("data/newmeinv.jpg")),0.5f). //水印位于右下角,半透明
                    outputQuality(0.8). // 图片压缩80%质量
                    toFile("data/abc_new.jpg");
        }catch(Throwable throwable){
            LogUtil.error("XXX_yyy_t1", throwable);
        }
    }

    /**
     * 生成缩略图到指定的目录
     */
    public static final void test3(){
        try{
            Thumbnails.of("data/abc.jpg","data/meinv.jpg").
                    //scalingMode(ScalingMode.BICUBIC).
                            scale(0.8). // 图片缩放80%, 不能和size()一起使用
                    toFiles(new File("data/new/"), Rename.NO_CHANGE);//指定的目录一定要存在,否则报错
        }catch(Throwable throwable){
            LogUtil.error("XXX_yyy_t1", throwable);
        }
    }

    /**
     * 将指定目录下所有图片生成缩略图
     */
    public static final void test4(){
        try{
            Thumbnails.of(new File("data/new").listFiles()).
                    //scalingMode(ScalingMode.BICUBIC).
                            scale(0.8). // 图片缩放80%, 不能和size()一起使用
                    toFiles(new File("data/new/"), Rename.SUFFIX_HYPHEN_THUMBNAIL);//指定的目录一定要存在,否则报错
        }catch(Throwable throwable){
            LogUtil.error("XXX_yyy_t1", throwable);
        }
    }


    /**
     * 2020-08-06 6.4.0
     * 通过改变图片品质和尺寸，从而减小文件占用空间大小
     * @param from
     */
    public static final String reset(final String from){
        String fromLocal = UrlUtil.getStandardPath(from);
        int[] widthHeightArray = getWidthHeight(fromLocal);
        int width = widthHeightArray[0];
        int height = widthHeightArray[1];
        if(width == 0 || height == 0){
            LogUtil.error("ImageUtil_reset_1", "width and height are boths 0", fromLocal);
            return "error-1";
        }
        double scale = 1;
        int min = Math.min(width, height);
        if(min > 10000){
            scale = 0.1;
        }else if(min > 8000){
            scale = 0.2;
        }else if(min > 6000){
            scale = 0.3;
        }else if(min > 4000){
            scale = 0.4;
        }else if(min > 2000){
            scale = 0.5;
        }
        //
        final long topSize = 1024000 * 2;
        double quality = 1;
        long size = FileUtil.getFileSize(fromLocal);
        if(size > topSize){
            double ddd = DoubleUtil.div(size, topSize, 2);
            if(ddd > 10){
                quality = 0.1;
            }else if(ddd > 8){
                quality = 0.2;
            }else if(ddd > 6){
                quality = 0.3;
            }else if(ddd > 4){
                quality = 0.4;
            }else if(ddd > 2){
                quality = 0.5;
            }else{
                quality = 0.6;
            }
        }else{
            return fromLocal;
        }
        String path = UrlUtil.getPathByUrl(fromLocal);
        String name = UrlUtil.getFileName(fromLocal, true);
        String newName = "xc_" + name;
        String to = path + "/" + newName;
        FileUtil.fileDelete(to);
        try{
            Thumbnails.of(fromLocal).
                    //scalingMode(ScalingMode.BICUBIC).
                    scale(scale).  //不能和size()一起使用
                    outputQuality(quality).
                    toFile(to);
            return to;
        }catch(Throwable throwable){
            LogUtil.error("ImageUtil_reset_t1", throwable, fromLocal);
            return "error-2";
        }
    }

    /**
     * 2020-08-03 6.4.0
     * 改变宽高尺寸，等比缩放，按照宽度或高度进行等比缩放
     */
    public static final boolean createImg(final String from, final String to, final String compareOf, final Integer widthOrHeight){
        if(!"w".equals(compareOf) && !"h".equals(compareOf)){
            LogUtil.error("ImageUtil_createImg_2", "compareOf值错误", compareOf);
            return false;
        }
        try{
            Thumbnails.Builder<File> builder = Thumbnails.of(from);
            if("w".equals(compareOf)){
                builder.size(widthOrHeight, 12340000);
            }else if("h".equals(compareOf)){
                builder.size(12340000, widthOrHeight);
            }
            builder.toFile(to);
            return true;
        }catch(Throwable throwable){
            LogUtil.error("ImageUtil_createImg_e2", throwable, from, to, compareOf, widthOrHeight);
            return false;
        }
    }

    /**
     * 2020-08-06 6.4.0
     * 2024-12-04 代码重构
     * 指定文件名或路径是否为图片格式
     * @param nameOrUrl 文件名（可带地址），都必须含有后缀名，如hello.txt或d:/app/hello.txt
     */
    public static final boolean isImage(final String nameOrUrl){
        final boolean flag = FileUtil.isImage(nameOrUrl);
        return flag;
    }

    /**
     * 2020-08-19 6.5.0
     * 2024-12-04 代码重构
     * 指定文件格式是否为图片格式
     * @param fileType 文件类型（不带点的后缀），如jpg
     */
    public static final boolean isImageByFileType(final String fileType){
        final boolean flag = FileUtil.isImageByFileType(fileType);
        return flag;
    }

    /**
     * 2020-08-06 6.4.0
     * 获取指定图片的宽度
     * @param url
     */
    public static final int getWidth(final String url){
        BufferedImage bufferedImage = p_getBufferedImage(url);
        if(bufferedImage == null){
            return 0;
        }
        int width = bufferedImage.getWidth();
        return width;
    }

    /**
     * 2020-08-06 6.4.0
     * 获取指定图片的高度
     * @param url
     */
    public static final int getHeight(final String url){
        BufferedImage bufferedImage = p_getBufferedImage(url);
        if(bufferedImage == null){
            return 0;
        }
        int height = bufferedImage.getHeight();
        return height;
    }

    /**
     * 2020-08-06 6.4.0
     * 获取图片宽高集合
     * @param url
     */
    public static final int[] getWidthHeight(final String url){
        BufferedImage bufferedImage = p_getBufferedImage(url);
        if(bufferedImage == null){
            int[] array = {0, 0};
            return array;
        }
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        int[] array = {width, height};
        return array;
    }

    /**
     * 2020-08-06 6.4.0
     */
    private static final BufferedImage p_getBufferedImage(final String url){
        if(StringUtil.isBlank(url)){
            return null;
        }
        String urlLocal = UrlUtil.getStandardPath(url);
        boolean flag = FileUtil.valid(urlLocal);
        if(!flag){
            return null;
        }
        flag = isImage(urlLocal);
        if(!flag){
            return null;
        }
        FileInputStream fileInputStream = null;
        try{
            fileInputStream = new FileInputStream(urlLocal);
            BufferedImage bufferedImage = ImageIO.read(fileInputStream);
            return bufferedImage;
        }catch(Throwable throwable){
            LogUtil.error("ImageUtil_p_getBufferedImage_t1", throwable, urlLocal);
            return null;
        }finally {
            IOUtil.close(fileInputStream);
        }
    }

}