package xc.crabapple.core.cipher.aes;

import org.apache.commons.codec.binary.Base64;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.cipher.bouncycastle.BouncyCastleUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * 2023-07-13
 * 2025-01-16 代码整理
 * AES加解密工具类
 */
public final class Aes2Util {

    private static final String ALGORITHMS = "AES/ECB/PKCS5Padding";  // 兼容旧数据：AES/ECB/PKCS5Padding
    private static final String ALGORITHMS_GCM = "AES/GCM/NoPadding";  // 推荐模式：AES/GCM，随机IV，含完整性校验
    private static final Integer GCM_IV_LENGTH = 12;  // GCM推荐IV长度，单位字节

    /**
     * 2025-01-16
     * 生成密钥，16位ASCII字符串
     */
    public static final String generateKey() {
        final String key = RandomUtil.getStr(16, false);
        return key;
    }

    /**
     * 加密（ECB模式，仅建议用于兼容旧数据）
     * 安全提示：ECB模式相同明文产生相同密文；新系统建议使用 encryptGcm（随机IV+完整性校验）
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
            LogUtil.error("Aes2Util_encrypt_t1", throwable);
            return null;
        }
    }

    /**
     * 解密（ECB模式，用于解密本类encrypt产生的历史密文）
     * 安全提示：新系统建议使用 encryptGcm/decryptGcm
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
            LogUtil.error("Aes2Util_decrypt_t1", throwable);
            return null;
        }
    }

    /**
     * 2026-10-17
     * 加密（推荐），AES/GCM模式，随机IV，密文格式：Base64(IV(12字节) + 密文 + 认证标签(16字节))
     * 安全提示：GCM含完整性校验，相同明文每次加密结果不同
     * @param key 密钥，16/24/32位ASCII字符串
     * @param plaintextStr 加密的字符串
     */
    public static final String encryptGcm(final String key, final String plaintextStr) {
        BouncyCastleUtil.addProvider();
        try{
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            if(!p_checkGcmKey(keyBytes)){
                return null;
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHMS_GCM, BouncyCastleUtil.getBouncyCastleProvider());
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(iv));
            byte[] cipherBytes = cipher.doFinal(plaintextStr.getBytes(StandardCharsets.UTF_8));
            byte[] resultBytes = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, resultBytes, 0, iv.length);
            System.arraycopy(cipherBytes, 0, resultBytes, iv.length, cipherBytes.length);
            return Base64.encodeBase64String(resultBytes);
        }catch(Throwable throwable){
            LogUtil.error("Aes2Util_encryptGcm_t1", throwable);
            return null;
        }
    }

    /**
     * 2026-10-17
     * 解密（推荐），对应encryptGcm
     * @param key 密钥，16/24/32位ASCII字符串
     * @param cipherStr encryptGcm产生的密文
     */
    public static final String decryptGcm(final String key, final String cipherStr) {
        BouncyCastleUtil.addProvider();
        try{
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            if(!p_checkGcmKey(keyBytes)){
                return null;
            }
            byte[] resultBytes = Base64.decodeBase64(cipherStr);
            if(resultBytes.length <= GCM_IV_LENGTH){
                LogUtil.error("Aes2Util_decryptGcm_1", "密文格式错误");
                return null;
            }
            byte[] iv = Arrays.copyOfRange(resultBytes, 0, GCM_IV_LENGTH);
            byte[] cipherBytes = Arrays.copyOfRange(resultBytes, GCM_IV_LENGTH, resultBytes.length);
            Cipher cipher = Cipher.getInstance(ALGORITHMS_GCM, BouncyCastleUtil.getBouncyCastleProvider());
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(iv));
            byte[] plaintextBytes = cipher.doFinal(cipherBytes);
            return new String(plaintextBytes, StandardCharsets.UTF_8);
        }catch(Throwable throwable){
            LogUtil.error("Aes2Util_decryptGcm_t1", throwable);
            return null;
        }
    }

    /**
     * 校验GCM密钥长度
     */
    private static final boolean p_checkGcmKey(final byte[] keyBytes){
        if(keyBytes == null || (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32)){
            LogUtil.error("Aes2Util_p_checkGcmKey_1", "密钥长度必须为16/24/32字节");
            return false;
        }
        return true;
    }

}