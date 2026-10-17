package xc.crabapple.core.qrc;

import com.google.zxing.LuminanceSource;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import xc.crabapple.core.log.LogUtil;

/**
 * 2020-10-18 6.7.0
 */
public final class BufferedImageLuminanceSource  extends LuminanceSource{

    private BufferedImage p_bufferedImage;
    private int p_left;
    private int p_top;

    public BufferedImageLuminanceSource(final BufferedImage bufferedImage){
        this(bufferedImage, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
    }

    public BufferedImageLuminanceSource(final BufferedImage bufferedImage, final int left, final int top, final int width, final int height){
        super(width, height);
        initApp(bufferedImage, left, top, width, height);
    }

    private final void initApp(final BufferedImage bufferedImage, final int left, final int top, final int width, final int height){
        int sourceWidth = bufferedImage.getWidth();
        int sourceHeight = bufferedImage.getHeight();
        int aaa = left + width;
        int bbb = top + height;
        boolean flag1 = aaa > sourceWidth;
        boolean flag2 = bbb > sourceHeight;
        if(flag1 || flag2){
            LogUtil.error("BufferedImageLuminanceSource_initApp_1", "Crop rectangle does not fit within bufferedImage data.");
            return;
        }
        for(int y = top; y < top + height; y++){
            for(int x = left; x < left + width; x++){
                if((bufferedImage.getRGB(x, y) & 0xFF000000) == 0){
                    bufferedImage.setRGB(x, y, 0xFFFFFFFF);
                }
            }
        }
        p_bufferedImage = new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics graphics = p_bufferedImage.getGraphics();
        graphics.drawImage(bufferedImage, 0, 0, null);
        graphics.dispose();
        p_left = left;
        p_top = top;
    }

    @Override
    public byte[] getRow(final int y, byte[] row){
        int height = getHeight();
        if(y < 0 || y >= height) {
            LogUtil.error("BufferedImageLuminanceSource_getRow_1", "Requested row is outside the bufferedImage", y);
            return null;
        }
        int width = getWidth();
        if(row == null || row.length < width){
            row = new byte[width];
        }
        p_bufferedImage.getRaster().getDataElements(p_left, p_top + y, width, 1, row);
        return row;
    }

    @Override
    public byte[] getMatrix(){
        int width = getWidth();
        int height = getHeight();
        int area = width * height;
        byte[] matrix = new byte[area];
        p_bufferedImage.getRaster().getDataElements(p_left, p_top, width, height, matrix);
        return matrix;
    }

    @Override
    public boolean isCropSupported(){
        return true;
    }

    @Override
    public LuminanceSource crop(final int left, final int top, final int width, final int height){
        int left2 = p_left + left;
        int top2 = p_top + top;
        BufferedImageLuminanceSource bufferedImageLuminanceSource = new BufferedImageLuminanceSource(p_bufferedImage, left2, top2, width, height);
        return bufferedImageLuminanceSource;
    }

    @Override
    public boolean isRotateSupported(){
        return true;
    }

    @Override
    public LuminanceSource rotateCounterClockwise(){
        int sourceWidth = p_bufferedImage.getWidth();
        int sourceHeight = p_bufferedImage.getHeight();
        AffineTransform transform = new AffineTransform(0.0, -1.0, 1.0, 0.0, 0.0, sourceWidth);
        BufferedImage rotatedImage = new BufferedImage(sourceHeight, sourceWidth, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics2D = rotatedImage.createGraphics();
        graphics2D.drawImage(p_bufferedImage, transform, null);
        graphics2D.dispose();
        int width = getWidth();
        int height = getHeight();
        int top2 = sourceWidth - (p_left + width);
        BufferedImageLuminanceSource bufferedImageLuminanceSource = new BufferedImageLuminanceSource(rotatedImage, p_top, top2, height, width);
        return bufferedImageLuminanceSource;
    }

}
