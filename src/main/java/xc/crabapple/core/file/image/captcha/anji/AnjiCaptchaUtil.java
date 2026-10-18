package xc.crabapple.core.file.image.captcha.anji;

import com.anji.captcha.model.common.Const;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import com.anji.captcha.util.Base64Utils;
import com.anji.captcha.util.FileCopyUtils;
import com.anji.captcha.util.ImageUtils;
import com.anji.captcha.util.StringUtils;
import xc.crabapple.core.log.LogUtil;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 2025-04-21 新建
 * 2025-04-28 整理
 * 点选字符验证码工具类
 */
public final class AnjiCaptchaUtil {
    
    private static CaptchaService p_captchaService;

    /**
     * 2025-04-28
     * @param clickWordBgimgPath
     */
    public static final void init(final String clickWordBgimgPath){
        init("local", clickWordBgimgPath);
    }

    /**
     * 2025-04-28
     * 项目初始化时被执行
     * @param cacheType 缓存位置，可选的取值：local
     * @param clickWordBgimgPath 点选底图路径，如：/data/crabapple/captcha/clickWordBgimg
     */
    public static final void init(final String cacheType, final String clickWordBgimgPath) {
        // 不允许重复初始化
        if(p_captchaService != null){
            return;
        }
        // 2025-06-10 add
        // 需在调用AWT前执行
        System.setProperty("java.awt.headless", "true");
        //
        Properties config = new Properties();
        // 参数设置
        config.put(Const.CAPTCHA_CACHETYPE, cacheType);  // 缓存local
        config.put(Const.CAPTCHA_TYPE, "clickWord");  // 验证码类型 实例化文字点选
        config.put(Const.ORIGINAL_PATH_PIC_CLICK, clickWordBgimgPath);  // 点选底图路径
        config.put(Const.CAPTCHA_AES_STATUS, "true");  // aes加密开关
        config.put(Const.CAPTCHA_WATER_FONT, "宋体");  // 右下角水印字体
        config.put(Const.CAPTCHA_WATER_MARK, "");  // 右下角水印文字
        config.put(Const.CAPTCHA_FONT_TYPE, "黑体");  // 点选文字验证码的文字字体
        config.put(Const.CAPTCHA_FONT_STYLE, Font.BOLD);  // 点选文字验证码的粗细
        config.put(Const.CAPTCHA_FONT_SIZE, "30");  // 点选文字验证码的大小
        config.put(Const.CAPTCHA_WORD_COUNT, "4");  // 点选文字个数
        config.put(Const.CAPTCHA_CACAHE_MAX_NUMBER, "1000");  // local缓存的阈值
        config.put(Const.CAPTCHA_TIMING_CLEAR_SECOND, "60");  // 定时清理过期local缓存，秒
        config.put(Const.REQ_FREQUENCY_LIMIT_ENABLE, "1");  // 接口限流开关 0禁用 1启用
        config.put(Const.REQ_GET_MINUTE_LIMIT, "10");  // get 接口 一分钟请求次数限制
//        config.put(Const.REQ_GET_LOCK_LIMIT, "10");  // 验证失败后，get接口锁定限制
//        config.put(Const.REQ_GET_LOCK_SECONDS, "5");  // 验证失败后，get接口锁定时间，秒
        config.put(Const.REQ_VALIDATE_MINUTE_LIMIT, "10");  // verify 接口 一分钟请求次数限制
        config.put(Const.REQ_CHECK_MINUTE_LIMIT, "10");  // check接口 一分钟请求次数限
        // 自定义点选底图路径
        final String originalPathPicClick = config.getProperty(Const.ORIGINAL_PATH_PIC_CLICK);
        if (StringUtils.isNotBlank(originalPathPicClick)) {
            //初始化底图
            config.put(Const.CAPTCHA_INIT_ORIGINAL, "true");
            final String originalPathJigsaw = config.getProperty(Const.ORIGINAL_PATH_JIGSAW);
            p_initializeBaseMap(originalPathJigsaw, originalPathPicClick);  // 点选
        }
        p_captchaService = CaptchaServiceFactory.getInstance(config);
    }

    private static final void p_initializeBaseMap(final String jigsaw, final String picClick) {
        Map<String, String> jigsawMap = p_getResourcesImagesFile(jigsaw);
        Map<String, String> picClickMap = p_getResourcesImagesFile(picClick);
        ImageUtils.cacheBootImage(jigsawMap, jigsawMap, picClickMap);
    }

    private static final Map<String, String> p_getResourcesImagesFile(final String path) {
        Map<String, String> imgMap = new HashMap<>();
        if (!StringUtils.isNotBlank(path)) {
            return imgMap;
        }
        try {
            File directory = new File(path);
            File[] files = directory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".png");  // 筛选.png文件
                }
            });
            if (files != null) {
                for (File file : files) {
                    byte[] bytes = FileCopyUtils.copyToByteArray(new FileInputStream(file));
                    String string = Base64Utils.encodeToString(bytes);
                    String filename = file.getName();
                    imgMap.put(filename, string);
                }
            }
            return imgMap;
        } catch (Throwable throwable) {
            LogUtil.error("AnjiCaptchaUtil_p_getResourcesImagesFile_t82", throwable, path);
            return imgMap;
        }
    }

    /**
     * 一般被业务项目的接口调用
     * @param captchaVO
     * @return
     */
    public static final ResponseModel get(final CaptchaVO captchaVO) {
        if(p_captchaService == null){
            LogUtil.error("AnjiCaptchaUtil_get_1", "请先初始化【AnjiCaptchaUtil.init】方法");
            return null;
        }
        ResponseModel responseModel = p_captchaService.get(captchaVO);
        return responseModel;
    }

    /**
     * 一般被业务项目的接口调用
     * @param captchaVO
     * @return
     */
    public static final ResponseModel check(final CaptchaVO captchaVO) {
        if(p_captchaService == null){
            LogUtil.error("AnjiCaptchaUtil_check_1", "请先初始化【AnjiCaptchaUtil.init】方法");
            return null;
        }
        ResponseModel responseModel = p_captchaService.check(captchaVO);
        return responseModel;
    }

    /**
     * 一般被业务项目的接口调用
     * @param captchaVO
     * @return
     */
    public static final ResponseModel verify(final CaptchaVO captchaVO) {
        if(p_captchaService == null){
            LogUtil.error("AnjiCaptchaUtil_verify_1", "请先初始化【AnjiCaptchaUtil.init】方法");
            return null;
        }
        ResponseModel responseModel = p_captchaService.verification(captchaVO);
        return responseModel;
    }

}