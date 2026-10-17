package xc.crabapple.core.cipher.des;

import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.Base64Util;
import xc.crabapple.core.log.LogUtil;

/**
 * 2017-02-10 1.0.0
 * DES加密算法，对称算法，双向，可逆
 * ECB-电码本模式，CBC-加密块链模式，OFB-输出反馈模式，CFB-加密反馈模式
 * 本例采用Base64形式处理
 * */
public final class DesUtil {

    /**
     * 2018-11-02 1.1.0
     * 加密
     * @param key 加密的密钥，必须是长度为8的字符串
     * @param value 待加密内容
     * @return (String) 返回密文
     */
    public static final String encrypt(final String key, final String value) throws Throwable{
        String result = encrypt(key, value, "DES/ECB/PKCS5Padding");
        return result;
    }

    /**
     * 2017-02-10 1.0.0
     * 加密
     * @param key 加密的密钥，必须是长度为8的字符串
     * @param value 待加密内容
     * @param transformation 填充方式，可选值如下
     *     DES/ECB/PKCS5Padding
     *     DES/ECB/PKCS7Padding
     *     DES/CBC/PKCS5Padding
     *     DES/CBC/PKCS7Padding
     *     DES/OFB/PKCS5Padding
     *     DES/OFB/PKCS7Padding
     *     DES/CFB/PKCS5Padding
     *     DES/CFB/PKCS7Padding
     * @return (String) 返回密文
     */
    public static final String encrypt(final String key, final String value, final String transformation) throws Throwable {
        SecureRandom random = new SecureRandom();
        byte[] bytes = key.getBytes("UTF-8");
        DESKeySpec desKey = new DESKeySpec(bytes);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey securekey = keyFactory.generateSecret(desKey);
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.ENCRYPT_MODE, securekey, random);
        byte[] bytes1 = value.getBytes("UTF-8");
        byte[] result = cipher.doFinal(bytes1);
        byte[] byteResult = Base64.encodeBase64URLSafe(result);
        String cipherStr = StringUtil.bytes2str(byteResult);
        return cipherStr;
    }

    /**
     * 2018-11-02 1.1.0
     * 解密
     * @param key 解密的密钥，必须是长度为8的字符串
     * @param value 要解密的密文
     * @return (String) 返回明文
     */
    public static final String decrypt(final String key, final String value) throws Throwable {
        String result = decrypt(key, value, "DES/ECB/PKCS5Padding");
        return result;
    }

    /**
     * 2017-02-10 1.0.0
     * 解密
     * @param key 解密的密钥，必须是长度为8的字符串
     * @param value 要解密的密文
     * @return (String) 返回明文
     */
    public static final String decrypt(final String key, final String value, final String transformation) throws Throwable {
        if(value == null || "".equals(value)){
            LogUtil.warn("DESUtil_decrypt_1", "value为空", key);
            return null;
        }
        byte[] content = value.getBytes("UTF-8");
        content = Base64.decodeBase64(content);
        SecureRandom random = new SecureRandom();
        byte[] bytes = key.getBytes("UTF-8");
        DESKeySpec desKey = new DESKeySpec(bytes);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey securekey = keyFactory.generateSecret(desKey);
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.DECRYPT_MODE, securekey, random);
        byte[] bytes2 = cipher.doFinal(content);
        String str = StringUtil.bytes2str(bytes2);
        return str;
    }

    /**
     * 方法2，加密方法，只能与decrypt2方法对应
     * @param key 必须是长度为8的字符串
     * @param value 待加密内容
     */
    public static final String encrypt2(final String key, final String value) {
        if (key == null || "".equals(key)) {
            LogUtil.warn("xc_crabapple_DESUtil_encrypt2_1", "key为空", key);
            return null;
        }
        if(key.length() != 8){
            LogUtil.warn("xc_crabapple_DESUtil_encrypt2_2", "key长度必须为8位", key);
            return null;
        }
        try{
            byte[] keyBytes = key.getBytes("UTF-8");
            SecretKey deskey = new SecretKeySpec(keyBytes, "DES");
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(1, deskey);
            byte[] valueBytes = value.getBytes("UTF-8");
            byte[] valueBytesCipher = cipher.doFinal(valueBytes);
            if (valueBytesCipher == null) {
                LogUtil.warn("xc_crabapple_DESUtil_encrypt2_3", "valueBytesCipher为空", key);
                return null;
            }
            String result = Base64Util.encodeBytes2str(valueBytesCipher);
            return result;
        }catch(Throwable throwable){
            LogUtil.error("DESUtil_encrypt2_t4", throwable, key);
            return null;
        }
    }

    /**
     * 方法2，加密方法，只能与encrypt2方法对应
     * @param key 必须是长度为8的字符串
     * @param value 密文
     * @return 返回明文
     */
    public static final String decrypt2(final String key, final String value) {
        if (key == null || "".equals(key)) {
            LogUtil.warn("xc_crabapple_DESUtil_decrypt2_1",  "key为空", "key="+key);
            return null;
        }
        if(key.length() != 8){
            LogUtil.warn("xc_crabapple_DESUtil_decrypt2_2", "key长度必须为8位", "key="+key);
            return null;
        }
        try{
            java.util.Base64.Decoder decoder = java.util.Base64.getDecoder();
            byte[] valueBytes = decoder.decode(value);
            Cipher cipher = Cipher.getInstance("DES");
            byte[] keyBytes = key.getBytes("UTF-8");
            SecretKey deskey = new SecretKeySpec(keyBytes, "DES");
            cipher.init(2, deskey);
            byte[] bytes = cipher.doFinal(valueBytes);
            if(bytes == null){
                LogUtil.warn("xc_crabapple_DESUtil_decrypt2_3",  "bytes为空", "key="+key);
                return null;
            }
            String result = StringUtil.bytes2str(bytes);
            return result;
        }catch(Throwable throwable){
            LogUtil.error("DESUtil_decrypt2_t4", throwable, key);
            return null;
        }
    }

}
