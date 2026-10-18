package xc.crabapple.core.phone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 2026-10-17
 * PhoneUtil 单元测试
 */
public class PhoneUtilTest {

    @Test
    public void generate() {
        String number = PhoneUtil.generate();
        assertEquals(11, number.length());
        assertTrue(PhoneUtil.check(number));
    }

    @Test
    public void generateByType() {
        String mobile = PhoneUtil.generate("中国移动");
        assertTrue(PhoneUtil.check(mobile));
        assertEquals("中国移动", PhoneUtil.getCarrier(mobile));
        String unicom = PhoneUtil.generate("中国联通");
        assertEquals("中国联通", PhoneUtil.getCarrier(unicom));
        String telecom = PhoneUtil.generate("中国电信");
        assertEquals("中国电信", PhoneUtil.getCarrier(telecom));
    }

    @Test
    public void check() {
        assertFalse(PhoneUtil.check(null));
        assertFalse(PhoneUtil.check("123"));
        assertFalse(PhoneUtil.check("23456789012"));
        assertTrue(PhoneUtil.check("13812345678"));
    }

    @Test
    public void getCarrier() {
        assertEquals("ERROR_无效号码", PhoneUtil.getCarrier("123"));
        assertEquals("中国移动", PhoneUtil.getCarrier("13812345678"));
        assertEquals("ERROR_无效运营商", PhoneUtil.getCarrier("12345678901"));
    }

}
