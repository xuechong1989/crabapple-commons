package xc.crabapple.core.cipher.aes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 2026-10-17
 * Aes2Util 单元测试
 */
public class Aes2UtilTest {

    private static final String KEY = "0123456789abcdef";

    @Test
    public void gcmRoundtrip() {
        String plaintext = "海棠果 hello 123";
        String cipherStr = Aes2Util.encryptGcm(KEY, plaintext);
        assertNotNull(cipherStr);
        assertEquals(plaintext, Aes2Util.decryptGcm(KEY, cipherStr));
    }

    @Test
    public void gcmRandomIv() {
        String plaintext = "same plaintext";
        String cipherStr1 = Aes2Util.encryptGcm(KEY, plaintext);
        String cipherStr2 = Aes2Util.encryptGcm(KEY, plaintext);
        assertNotEquals(cipherStr1, cipherStr2);
    }

    @Test
    public void gcmWrongKeyRejected() {
        String cipherStr = Aes2Util.encryptGcm(KEY, "secret");
        assertNull(Aes2Util.decryptGcm("fedcba9876543210", cipherStr));
    }

    @Test
    public void gcmKeyLengthValidated() {
        assertNull(Aes2Util.encryptGcm("short", "value"));
    }

    @Test
    public void legacyEcbCompat() {
        String plaintext = "legacy data";
        String cipherStr = Aes2Util.encrypt(KEY, plaintext);
        assertEquals(plaintext, Aes2Util.decrypt(KEY, cipherStr));
    }

}
