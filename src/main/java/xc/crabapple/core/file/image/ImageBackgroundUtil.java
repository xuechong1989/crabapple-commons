package xc.crabapple.core.file.image;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.io.IOUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Base64;

/*
 * 2023-11-01
 *  检测图片是否透明  noBackground()
 *  不透明图片转换为透明  imageToPng()
 **/
public final class ImageBackgroundUtil {

    // 色差范围0~255
    public static final int color_range = 210;

    /*
     * 2023-11-01
     * 图片设置透明背景颜色 imageBytes 原图
     **/
    public static final byte[] imageToPng(final byte[] imageBytes) throws Throwable {
        ByteArrayInputStream byteArrayInputStream = null;
        try{
            byteArrayInputStream = new ByteArrayInputStream(imageBytes);
            byte[] bytes = imageToPng(byteArrayInputStream);
            return bytes;
        }finally {
            IOUtil.close(byteArrayInputStream);
        }
    }

    /*
     * 2023-11-01
     * 图片设置透明背景颜色 file 原图
     **/
    public static final byte[] imageToPng(final File file) throws Throwable {
        BufferedImage bufferedImage = ImageIO.read(file);
        byte[] bytes = imageToPng(bufferedImage);
        return bytes;
    }

    /*
     * 2023-11-01
     * 图片设置透明背景颜色 is 原图
     **/
    public static final byte[] imageToPng(final InputStream is) throws Throwable {
        BufferedImage bufferedImage = ImageIO.read(is);
        byte[] bytes = imageToPng(bufferedImage);
        return bytes;
    }

    /*
     * 2023-11-01
     * 图片设置透明背景颜色 image 原图
     **/
    public static final byte[] imageToPng(final BufferedImage image) throws Throwable {
        if(image == null){
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = null;
        byte[] bytes = null;
        if(noBackground(image)){
            // 背景透明时,直接返回原图
            byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", byteArrayOutputStream);
            bytes = byteArrayOutputStream.toByteArray();
            return bytes;
        }
        try{
            byteArrayOutputStream = new ByteArrayOutputStream();
            // 高度和宽度
            int height = image.getHeight();
            int width = image.getWidth();
            // 初始化背景透明和内容透明的图片
            ImageIcon imageIcon = new ImageIcon(image);
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g2D = (Graphics2D) bufferedImage.getGraphics(); // 获取画笔
            // 绘制Image的图片
            g2D.drawImage(imageIcon.getImage(), 0, 0, null);
            int alpha = 0; // 图片透明度
            // 外层遍历是Y轴的像素
            for (int y = bufferedImage.getMinY(); y < bufferedImage.getHeight(); y++) {
                // 内层遍历是X轴的像素
                for (int x = bufferedImage.getMinX(); x < bufferedImage.getWidth(); x++) {
                    int rgb = bufferedImage.getRGB(x, y);
                    // 对当前颜色判断是否在指定区间内
                    if (colorInRange(rgb)){
                        alpha = 0;
                    }else{
                        // 设置为不透明
                        alpha = 255;
                    }
                    //最前两位为透明度
                    rgb = (alpha << 24) | (rgb & 0x00ffffff);
                    bufferedImage.setRGB(x, y, rgb);
                }
            }
            // 绘制设置了RGB的新图片,这一步感觉不用也可以,只是透明地方的深浅有变化而已，就像蒙了两层的感觉
            g2D.drawImage(bufferedImage, 0, 0, null);
            // 生成图片为PNG
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
            bytes = byteArrayOutputStream.toByteArray();
        }finally {
            IOUtil.close(byteArrayOutputStream);
        }
        return bytes;
    }

    /**
     * 判断是背景还是内容
     * @param color
     * @return
     */
    public static final boolean colorInRange(final int color) {
        int red = (color & 0xff0000) >> 16;// 获取color(RGB)中R位
        int green = (color & 0x00ff00) >> 8;// 获取color(RGB)中G位
        int blue = (color & 0x0000ff);// 获取color(RGB)中B位
        // 通过RGB三分量来判断当前颜色是否在指定的颜色区间内
        if (red >= color_range && green >= color_range && blue >= color_range){
            return true;
        }
        return false;
    }

    /*
     * 2023-11-01
     * 判断是否有背景颜色 imageBytes 原图
     **/
    public static final boolean noBackground(final byte[] imageBytes) throws Throwable {
        ByteArrayInputStream byteArrayOutputStream = null;
        try{
            byteArrayOutputStream = new ByteArrayInputStream(imageBytes);
            return noBackground(byteArrayOutputStream);
        }finally {
            IOUtil.close(byteArrayOutputStream);
        }
    }

    /*
     * 2023-11-01
     * 判断是否有背景颜色 imageData 原图
     **/
    public static final boolean noBackground(final String imageData) throws Throwable {
        boolean flag = noBackground(imageData,true);
        return flag;
    }

    /*
     * 2023-11-01
     * 判断是否有背景颜色 imageData 原图 prefix base64是否存在前缀
     **/
    public static final boolean noBackground(final String imageData, final Boolean prefix) throws Throwable {
        String imageDataLocal = imageData;
        if (prefix) {
            String[] arr = StringUtil.split(imageDataLocal, "base64,");
            imageDataLocal = arr[1];
        }
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] bytes = decoder.decode(imageDataLocal);
        ByteArrayInputStream byteArrayInputStream = null;
        try{
            byteArrayInputStream = new ByteArrayInputStream(bytes);
            boolean flag = noBackground(byteArrayInputStream);
            return flag;
        }finally {
            IOUtil.close(byteArrayInputStream);
        }
    }

    /*
     * 2023-11-01
     * 判断是否有背景颜色 is 原图
     **/
    public static final boolean noBackground(final InputStream inputStream) throws Throwable {
        BufferedImage bufferedImage = ImageIO.read(inputStream);
        boolean flag = noBackground(bufferedImage);
        return flag;
    }

    /*
     * 2023-11-01
     * 判断是否有背景颜色 bufferedImage 原图
     **/
    public static final boolean noBackground(final BufferedImage bufferedImage) {
        if(bufferedImage == null){
            return false;
        }
        int height = bufferedImage.getHeight();
        int width = bufferedImage.getWidth();
        for(int w = 0; w < width; w++){
            for(int h = 0; h < height; h++){
                int rgb = bufferedImage.getRGB(w, h);
                if(rgb >> 24 == 0){
                    return true;
                }
            }
        }
        return false;
    }

}