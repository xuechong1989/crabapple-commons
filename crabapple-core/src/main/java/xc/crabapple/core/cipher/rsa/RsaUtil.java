package xc.crabapple.core.cipher.rsa;

import xc.crabapple.core.encoder.Base64Util;
import xc.crabapple.core.log.LogUtil;

import javax.crypto.Cipher;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 2023-07-31
 * RSA是非对称加密算法
 */
public final class RsaUtil {

    public static final String KEY_ALGORITHM = "RSA";
    private static final String PUBLIC_KEY = "publicKey";
    private static final String PRIVATE_KEY = "privateKey";
    // 1024 bits 的 RSA 密钥对，最大加密明文大小
    private static final int MAX_ENCRYPT_BLOCK = 117;
    // 1024 bits 的 RSA 密钥对，最大解密密文大小
    private static final int MAX_DECRYPT_BLOCK = 128;

    /**
     * 2023-07-31
     * 生成密钥对
     * @param KEYSIZE 秘钥长度，如 1024
     */
    public static final Map<String, Object> generateKey(final int KEYSIZE) {
        try{
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            // 设置密钥对的bit数，越大越安全
            keyPairGenerator.initialize(KEYSIZE);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            // 获取公钥
            RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
            // 获取私钥
            RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
            Map<String, Object> keysMap = new HashMap<>(2);
            keysMap.put(PUBLIC_KEY, rsaPublicKey);
            keysMap.put(PRIVATE_KEY, rsaPrivateKey);
            return keysMap;
        }catch(Throwable throwable){
            LogUtil.error("RsaUtil_generateKey_t1", throwable);
            Map<String, Object> keysMap = new HashMap<>(2);
            keysMap.put(PUBLIC_KEY, "");
            keysMap.put(PRIVATE_KEY, "");
            return keysMap;
        }
    }

    /**
     * 2023-07-31
     * 公钥加密
     * @param publicKeyStr 公钥
     * @param str 待加密的明文字符串
     * @return 加密后的密文
     */
    public static final String encrypt(final String publicKeyStr, final String str) {
        try{
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            PublicKey publicKey = getPublicKey(publicKeyStr);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] bytes = str.getBytes("UTF-8");
            byte[] tempBytes = cipher.doFinal(bytes);
            final String cipherStr = Base64Util.encodeBytes2str(tempBytes);
            return cipherStr;
        }catch(Throwable throwable){
            LogUtil.error("RsaUtil_encrypt_t1", throwable, "RSA加密失败", publicKeyStr);
            return "";
        }
    }

    /**
     * 2023-07-31
     * 私钥解密
     * @param privateKeyStr 私钥
     * @param cipherStr 待解密的密文字符串
     * @return 解密后的明文
     */
    public static final String decrypt(final String privateKeyStr, final String cipherStr) {
        try{
            // 生成私钥
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            PrivateKey privateKey= getPrivateKey(privateKeyStr);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            // 密文解码
            byte[] bytes = cipherStr.getBytes("UTF-8");
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] tmpBytes = decoder.decode(bytes);
            byte[] tmp2Bytes = cipher.doFinal(tmpBytes);
            final String str = new String(tmp2Bytes);
            return str;
        }catch(Throwable throwable){
            // 安全要求：日志中不得输出私钥等密钥材料，仅记录异常与必要的定位信息
            LogUtil.error("RsaUtil_decrypt_t1", throwable, "RSA解密失败");
            return "";
        }
    }

    /**
     * 2023-07-31
     * 获取公钥字符串
     */
    public static final String getPublicKeyStr(final Map<String, Object> keyMap) {
        // 获得map中的公钥对象，转为key对象
        final Key key = (Key) keyMap.get(PUBLIC_KEY);
        // 编码返回字符串
        byte[] bytes = key.getEncoded();
        final String publicKeyStr = p_encryptBase64(bytes);
        return publicKeyStr;
    }

    /**
     * 2023-07-31
     * 获取私钥字符串
     */
    public static final String getPrivateKeyStr(final Map<String, Object> keyMap) {
        // 获得 map 中的私钥对象，转为 key 对象
        final Key key = (Key) keyMap.get(PRIVATE_KEY);
        // 编码返回字符串
        byte[] bytes = key.getEncoded();
        final String privateKeyStr = p_encryptBase64(bytes);
        return privateKeyStr;
    }

    /**
     * 2023-07-31
     * 获取公钥
     */
    public static final PublicKey getPublicKey(final String publicKeyStr) {
        try{
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] publicKeyByte = decoder.decode(publicKeyStr);
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKeyByte);
            final KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            final PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
            return publicKey;
        }catch(Throwable throwable){
            LogUtil.error("RsaUtil_getPublicKey_t1", throwable, "获取RSA公钥失败", publicKeyStr);
            return null;
        }
    }

    /**
     * 2023-07-31
     * 获取私钥
     */
    public static final PrivateKey getPrivateKey(final String privateKeyStr) {
        try{
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] privateKeyByte = decoder.decode(privateKeyStr);
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKeyByte);
            final KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            final PrivateKey privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
            return privateKey;
        }catch(Throwable throwable){
            // 安全要求：日志中不得输出私钥等密钥材料
            LogUtil.error("RsaUtil_getPrivateKey_t1", throwable, "获取RSA私钥失败");
            return null;
        }
    }

    /**
     * 2023-07-31
     * base64 编码返回加密字符串
     * @param keyBytes 需要编码的字节数组
     * @return 编码后的字符串
     */
    private static final String p_encryptBase64(final byte[] keyBytes) {
        Base64.Encoder encoder = Base64.getEncoder();
        final byte[] bytes = encoder.encode(keyBytes);
        final String keyStr = new String(bytes);
        return keyStr;
    }

    /**
     * 2023-07-31
     * base64 解码，返回字节数组
     * @param keyStr 待解码的字符串
     * @return 解码后的字节数组
     */
    private static final byte[] p_decryptBase64(final String keyStr) {
        Base64.Decoder decoder = Base64.getDecoder();
        final byte[] keyBytes = decoder.decode(keyStr);
        return keyBytes;
    }

}