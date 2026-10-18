package xc.crabapple.core.file.image.captcha.google;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xc.crabapple.core.cache.LocalCacheUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.image.captcha.google.base.Captcha;
import xc.crabapple.core.file.image.captcha.google.type.GifCaptcha;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;

/**
 * 2023-05-19
 * 验证码工具类。
 *
 * 说明：原实现依赖 com.github.penggle:kaptcha 2.3.2，该版本存在未修复的
 * “使用弱随机数”安全问题。这里改用 JDK SecureRandom + Java2D 生成，
 * 不改变 createText/createImage/getKey 等公开方法签名。
 */
public final class GoogleCaptchaUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final int WIDTH = 130;
    private static final int HEIGHT = 50;
    private static final int LENGTH = 4;
    private static final String SESSION_KEY = "KAPTCHA_SESSION_KEY";

    public static final void init(){
        // 需在调用 AWT 前执行
        System.setProperty("java.awt.headless", "true");
        ImageIO.setUseCache(false);
    }

    /**
     * 2023-05-19
     * 创建 jpg 格式图片的验证码并推送给客户端
     */
    public static final void crateJpg2client(final HttpServletRequest request, final HttpServletResponse response){
        response.setDateHeader("Expires", 0);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.addHeader("Cache-Control", "post-check = 0, pre-check = 0");
        response.setHeader("Pragma", "no-cache");
        response.setContentType("image/jpeg");

        String capText = createText();
        final String key = getKey();
        request.getSession().setAttribute(key, capText);

        BufferedImage bufferedImage = createImage(capText);
        ServletOutputStream servletOutputStream = null;
        try {
            servletOutputStream = response.getOutputStream();
            ImageIO.write(bufferedImage, "jpg", servletOutputStream);
        }catch(Throwable throwable){
            LogUtil.error("VerificationCodeUtil_crateJpg2client_e1", throwable);
        } finally {
            IOUtil.close(servletOutputStream);
        }
    }

    public static final void setHeader(final HttpServletResponse response) {
        response.setContentType("image/gif");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0L);
    }

    /**
     * 2023-05-19
     * 创建 gif 格式图片的验证码并推送给客户端
     */
    public static final void createGif2client(final HttpServletResponse response, final String key, final long expireTime, final int width, final int height, final int length){
        setHeader(response);
        GifCaptcha gifCaptcha = new GifCaptcha(width, height, length);
        gifCaptcha.setCharType(Captcha.TYPE_DEFAULT);
        String captchaValue = gifCaptcha.text();
        captchaValue = StringUtil.toLowerCase(captchaValue);
        LocalCacheUtil.set(key, captchaValue, expireTime, true);
        ServletOutputStream servletOutputStream = null;
        try {
            servletOutputStream = response.getOutputStream();
            gifCaptcha.out(servletOutputStream);
        }catch(Throwable throwable){
            LogUtil.error("VerificationCodeUtil_createGif2client_e1", throwable);
        }finally {
            IOUtil.close(servletOutputStream);
        }
    }

    /**
     * 2023-05-19
     */
    public static final String getKey(){
        return SESSION_KEY;
    }

    /**
     * 2023-05-19
     */
    public static final String getValue(final String key){
        Object object = LocalCacheUtil.get(key);
        if(object == null) {
            return null;
        }
        return object.toString();
    }

    /**
     * 使用 SecureRandom 生成验证码文本。
     */
    public static final String createText(){
        StringBuilder text = new StringBuilder(LENGTH);
        for(int i = 0; i < LENGTH; i++){
            text.append(CHARS[RANDOM.nextInt(CHARS.length)]);
        }
        return text.toString();
    }

    /**
     * 使用 Java2D 生成验证码图片。
     */
    public static final BufferedImage createImage(final String text){
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try{
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(239, 246, 255));
            graphics.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线
            for(int i = 0; i < 8; i++){
                graphics.setColor(new Color(120 + RANDOM.nextInt(100), 140 + RANDOM.nextInt(80), 160 + RANDOM.nextInt(80)));
                graphics.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
            }

            String value = StringUtil.isBlank(text) ? createText() : text;
            graphics.setFont(new Font("SansSerif", Font.BOLD, 28));
            for(int i = 0; i < value.length(); i++){
                graphics.setColor(new Color(20 + RANDOM.nextInt(100), 40 + RANDOM.nextInt(100), 80 + RANDOM.nextInt(100)));
                int x = 18 + i * 28 + RANDOM.nextInt(4);
                int y = 34 + RANDOM.nextInt(6);
                graphics.drawString(String.valueOf(value.charAt(i)), x, y);
            }
        }finally{
            graphics.dispose();
        }
        return image;
    }

}
