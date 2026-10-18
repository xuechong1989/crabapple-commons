package xc.crabapple.core.file.image.captcha.google;

import com.google.code.kaptcha.Constants;
import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.util.Config;
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
import java.awt.image.BufferedImage;
import java.util.Properties;

/**
 * 2023-05-19
 * 验证码工具类
 */
public final class GoogleCaptchaUtil {

	private static Producer p_captchaProducer = null;

	public static final void init(){
		if(p_captchaProducer != null){
			return;
		}
        // 2025-06-10 add
        // 需在调用AWT前执行
        System.setProperty("java.awt.headless", "true");
        //
		ImageIO.setUseCache(false);
		Properties properties = new Properties();
		properties.put("kaptcha.border", "no");
//		properties.put("kaptcha.border.color", "68,140,187");
		properties.put("kaptcha.textproducer.font.color", "33,93,147");
//		properties.put("kaptcha.image.width", "130");
//		properties.put("kaptcha.image.height", "50");
//		properties.put("kaptcha.textproducer.font.size", "40");
//		properties.put("kaptcha.textproducer.char.space", "4");
		properties.put("kaptcha.textproducer.char.length", "4");
//		properties.put("kaptcha.background.clear.from", "239,246,255");
//		properties.put("kaptcha.background.clear.to", "239,246,255");
//		properties.put("kaptcha.textproducer.font.names", "宋体,楷体,微软雅黑");
		Config config = new Config(properties);
		p_captchaProducer = config.getProducerImpl();
	}

    /**
     * 2023-05-19
     * 创建jpg或jpeg格式图片的验证码并推送给客户端
     */
    public static final void crateJpg2client(final HttpServletRequest request, final HttpServletResponse response){
        response.setDateHeader("Expires", 0);
        // Set standard HTTP/1.1 no-cache headers.
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        // Set IE extended HTTP/1.1 no-cache headers (use addHeader).
        response.addHeader("Cache-Control", "post-check = 0, pre-check = 0");
        // Set standard HTTP/1.0 no-cache header.
        response.setHeader("Pragma", "no-cache");
        // return a jpeg
        response.setContentType("image/jpeg");
        // create the text for the image
        String capText = createText();
        // store the text in the session
        final String key = getKey();
        request.getSession().setAttribute(key, capText);
        // create the image with the text
        BufferedImage bufferedImage = createImage(capText);
        ServletOutputStream servletOutputStream = null;
        // write the data out
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
     * 创建gif格式图片的验证码并推送给客户端
     */
    public static final void createGif2client(final HttpServletResponse response, final String key, final long expireTime, final int width, final int height, final int length){
        setHeader(response);
        // 三个参数分别为宽、高、位数
        GifCaptcha gifCaptcha = new GifCaptcha(width, height, length);
        // 设置类型：字母数字混合
        gifCaptcha.setCharType(Captcha.TYPE_DEFAULT);
        // 保存验证码
        String captchaValue = gifCaptcha.text();
        captchaValue = StringUtil.toLowerCase(captchaValue);
        // 2026-08-20 构建替代redis的本地API持久化缓存框架
        // 验证码改纯内存存储：短命一次性数据无需重启恢复，重启恢复旧验证码反而有安全隐患
        LocalCacheUtil.set(key, captchaValue, expireTime, true);
        ServletOutputStream servletOutputStream = null;
        // 输出图片流
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
        String key = Constants.KAPTCHA_SESSION_KEY;
        return key;
    }

    /**
     * 2023-05-19
     */
    public static final String getValue(final String key){
        Object object = LocalCacheUtil.get(key);
        if(object == null) {
            return null;
        }
        String value = object.toString();
        return value;
    }

    /**
     * 2023-05-19
     */
    public static final String createText(){
        String text = p_captchaProducer.createText();
        return text;
    }

    /**
     * 2023-05-19
     */
    public static final BufferedImage createImage(final String text){
        BufferedImage bufferedImage = p_captchaProducer.createImage(text);
        return bufferedImage;
    }

}