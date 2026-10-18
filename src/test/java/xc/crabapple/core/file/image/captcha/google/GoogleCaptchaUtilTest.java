package xc.crabapple.core.file.image.captcha.google;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 2026-10-18
 * GoogleCaptchaUtil 单元测试
 */
public class GoogleCaptchaUtilTest {

    @Test
    public void createText() {
        GoogleCaptchaUtil.init();
        String text = GoogleCaptchaUtil.createText();
        assertEquals(4, text.length());
        assertTrue(text.matches("[2-9A-HJ-NP-Z]+"));
    }

    @Test
    public void createImage() {
        GoogleCaptchaUtil.init();
        BufferedImage image = GoogleCaptchaUtil.createImage("AB34");
        assertNotNull(image);
        assertEquals(130, image.getWidth());
        assertEquals(50, image.getHeight());
    }
}
