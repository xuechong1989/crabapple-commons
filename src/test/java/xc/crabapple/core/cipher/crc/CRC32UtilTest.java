package xc.crabapple.core.cipher.crc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 2026-10-17
 * CRC32Util 单元测试
 */
public class CRC32UtilTest {

    @Test
    public void getCRC32() {
        assertEquals(3421780262L, CRC32Util.getCRC32("123456789"));
        assertEquals(0L, CRC32Util.getCRC32(""));
        assertNotEquals(CRC32Util.getCRC32("abc"), CRC32Util.getCRC32("abd"));
    }

}
