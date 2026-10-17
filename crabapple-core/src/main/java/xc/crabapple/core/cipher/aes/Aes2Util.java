package xc.crabapple.core.cipher.aes;

import org.apache.commons.codec.binary.Base64;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.cipher.bouncycastle.BouncyCastleUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * 2023-07-13
 * 2025-01-16 代码整理
 * AES加解密工具类
 */
public final class Aes2Util {

    private static final String ALGORITHMS = "AES/ECB/PKCS5Padding";  // 算法名称/加密模式/数据填充方式，默认：AES/ECB/PKCS5Padding

    /**
     * 2025-01-16
     * 生成密钥，16位ASCII字符串
     */
    public static final String generateKey() {
        final String key = RandomUtil.getStr(16, false);
        return key;
    }

    /**
     * 加密
     * @param key 密钥，16位ASCII字符串
     * @param plaintextStr 加密的字符串
     */
    public static final String encrypt(final String key, final String plaintextStr) {
        BouncyCastleUtil.addProvider();
        try{
            //设置Cipher对象
            Cipher cipher = Cipher.getInstance(ALGORITHMS, BouncyCastleUtil.getBouncyCastleProvider());
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(), "AES"));
            //调用doFinal
            byte[] b = cipher.doFinal(plaintextStr.getBytes(StandardCharsets.UTF_8));
            // 转base64
            return Base64.encodeBase64String(b);
        }catch(Throwable throwable){
            LogUtil.error("Aes2Util_encrypt_t1", throwable, key);
            return null;
        }
    }

    /**
     * 解密
     * @param key 密钥，16位ASCII字符串
     * @param cipherStr 解密的字符串
     */
    public static final String decrypt(final String key, final String cipherStr) {
        BouncyCastleUtil.addProvider();
        try{
            //base64格式的key字符串转byte
            byte[] decodeBase64 = Base64.decodeBase64(cipherStr);
            //设置Cipher对象
            Cipher cipher = Cipher.getInstance(ALGORITHMS, BouncyCastleUtil.getBouncyCastleProvider());
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(), "AES"));
            //调用doFinal解密
            byte[] decryptBytes = cipher.doFinal(decodeBase64);
            return new String(decryptBytes);
        }catch(Throwable throwable){
            LogUtil.error("Aes2Util_decrypt_t1", throwable, key);
            return null;
        }
    }

}