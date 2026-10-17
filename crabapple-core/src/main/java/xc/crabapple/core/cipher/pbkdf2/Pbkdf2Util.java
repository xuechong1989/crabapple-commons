package xc.crabapple.core.cipher.pbkdf2;

import xc.crabapple.core.log.LogUtil;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.Charset;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * 2023-08-04
 * pbkdf2算法工具类
 * pbkdf2算法是单向加密、无法解密。
 * 采用SHA256加密验证算法
 * PBKDF2算法校验网站：https://8gwifi.org/pbkdf.jsp
 */
public final class Pbkdf2Util {

    private static final String SALT = "demo@2023-08-04/efile";  // 默认盐值
    private static final Integer ITERATIONS = 23084;  // 默认迭代次数
    private static final Integer LENGTH = 512;  // 密文长度

    /**
     * 2023-08-04
     */
    public static final String getHash(final String plaintext) {
        String hash = getHash(plaintext, SALT, ITERATIONS, LENGTH);
        return hash;
    }

    /**
     * 2023-08-04
     */
    public static final String getHash(final String plaintext, final int iterations) {
        String hash = getHash(plaintext, SALT, iterations, LENGTH);
        return hash;
    }

    /**
     * 2023-08-04
     */
    public static final String getHash(final String plaintext, final String salt) {
        String hash = getHash(plaintext, salt, ITERATIONS, LENGTH);
        return hash;
    }

    /**
     * 2023-08-04
     */
    public static final String getHash(final String plaintext, final int iterations, final int keyLength) {
        String hash = getHash(plaintext, SALT, iterations, keyLength);
        return hash;
    }

    /**
     * 2023-08-04
     * 返回散列密码，以及算法、迭代次数和salt
     * @param plaintext 密码明文
     * @param salt 加盐
     * @param iterations 迭代计数
     */
    public static final String getHash(final String plaintext, final String salt, final int iterations, final int keyLength) {
        String hash = p_getHash(plaintext, salt, iterations, keyLength);
        return hash;
    }

    /**
     * 2023-08-04
     * 校验密码是否合法
     */
    public static final boolean check(final String plaintext, final String cipher) {
        boolean isEQ = check(plaintext, SALT, ITERATIONS, LENGTH, cipher);
        return isEQ;
    }

    /**
     * 2023-08-04
     * 校验密码是否合法
     */
    public static final boolean check(final String plaintext, final String salt, final String cipher) {
        boolean isEQ = check(plaintext, salt, ITERATIONS, LENGTH, cipher);
        return isEQ;
    }

    /**
     * 2023-08-04
     * 校验密码是否合法
     */
    public static final boolean check(final String plaintext, final String salt, final int iterations, final String cipher) {
        boolean isEQ = check(plaintext, salt, iterations, LENGTH, cipher);
        return isEQ;
    }

    /**
     * 2023-08-04
     * 校验密码是否合法
     * @param plaintext 密码明文
     * @param cipher 密码密文
     */
    public static final boolean check(final String plaintext, final String salt, final int iterations, final int keyLength, final String cipher) {
        final String hash = getHash(plaintext, salt, iterations, keyLength);
        if(hash.equals(cipher)){
            return true;
        }
        return false;
    }

    /**
     * 获取密文
     * @param plaintext 密码明文
     * @param salt 加盐
     * @param iterations 迭代计数
     */
    private static final String p_getHash(final String plaintext, final String salt, final int iterations, final int keyLength) {
        // 仅返回整个编码密码的最后一部分
        SecretKeyFactory keyFactory = null;
        try {
            // 可选项：PBKDF2WithHmacSHA1、PBKDF2WithHmacSHA256、PBKDF2WithHmacSHA224、PBKDF2WithHmacSHA512、PBKDF2WithHmacSHA384
            keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        }catch(Throwable throwable){
            LogUtil.error("Pbkdf2Util_p_getHash_t1", throwable, "无法检索");
            return "";
        }
        char[] chrArray = plaintext.toCharArray();
        byte[] saltBytes = salt.getBytes(Charset.forName("UTF-8"));
        KeySpec keySpec = new PBEKeySpec(chrArray, saltBytes, iterations, keyLength);
        SecretKey secret = null;
        try {
            secret = keyFactory.generateSecret(keySpec);
        }catch(Throwable throwable){
            LogUtil.error("Pbkdf2Util_p_getHash_t2", throwable, "无法生成密钥");
            return "";
        }
        byte[] rawHash = secret.getEncoded();
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] hashBase64 = encoder.encode(rawHash);
        final String hash = new String(hashBase64);
        return hash;
    }

}