package xc.crabapple.core.encoder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 2026-10-17
 * Base64Util 单元测试
 */
public class Base64UtilTest {

    @Test
    public void encodeStr2str() {
        assertEquals("aGVsbG8=", Base64Util.encodeStr2str("hello"));
    }

    @Test
    public void decodeStr2str() {
        assertEquals("hello", Base64Util.decodeStr2str("aGVsbG8="));
    }

    @Test
    public void roundTrip() {
        String plaintext = "海棠果 crabapple 2026";
        String encoded = Base64Util.encodeStr2str(plaintext, "UTF-8");
        assertEquals(plaintext, Base64Util.decodeStr2str(encoded, "UTF-8"));
    }

    @Test
    public void bytesRoundTrip() {
        byte[] bytes = {1, 2, 3, 4, 5};
        assertArrayEquals(bytes, Base64Util.decodeBytes2bytes(Base64Util.encodeBytes2bytes(bytes)));
    }

    @Test
    public void isBase64() {
        assertTrue(Base64Util.isBase64(Base64Util.encodeStr2bytes("hello")));
        assertFalse(Base64Util.isBase64(new byte[]{'#', '#', '#'}));
    }

}
