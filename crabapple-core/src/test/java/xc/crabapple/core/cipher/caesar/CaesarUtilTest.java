package xc.crabapple.core.cipher.caesar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 2026-10-17
 * CaesarUtil 单元测试
 */
public class CaesarUtilTest {

    @Test
    public void encrypt() {
        assertEquals("=23_bb#中文E", CaesarUtil.encrypt("=12_aa#中文D", 1));
        assertEquals("a", CaesarUtil.encrypt("z", 1));
        assertEquals("A", CaesarUtil.encrypt("Z", 1));
        assertEquals("0", CaesarUtil.encrypt("9", 1));
    }

    @Test
    public void decrypt() {
        assertEquals("=12_aa#中文D", CaesarUtil.decrypt(CaesarUtil.encrypt("=12_aa#中文D", 1), 1));
        assertEquals("z", CaesarUtil.decrypt("a", 1));
    }

    @Test
    public void roundTrip() {
        String plaintext = "AbcDef123中文汉字XYZ";
        assertEquals(plaintext, CaesarUtil.decrypt(CaesarUtil.encrypt(plaintext)));
        assertEquals(plaintext, CaesarUtil.decrypt(CaesarUtil.encrypt(plaintext, 13), 13));
    }

}
