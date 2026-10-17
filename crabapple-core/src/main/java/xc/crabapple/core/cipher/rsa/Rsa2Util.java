package xc.crabapple.core.cipher.rsa;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import org.apache.commons.codec.binary.Base64;
import xc.crabapple.core.encoder.Base64Util;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;

/*
 *  rsa工具类
 **/
public final class Rsa2Util {

    /*
     * 2023-08-01
     * 随机生成密钥对，返回公钥、私钥
     **/
    public static final Map<String, String> genKeyPair() {
        try{
            final Map<String, String> keyMap = new HashMap<>();
            // KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            // 初始化密钥对生成器，密钥大小为512-1024位
            keyPairGen.initialize(1024, new SecureRandom());
            // 生成一个密钥对，保存在keyPair中
            KeyPair keyPair = keyPairGen.generateKeyPair();
            // 得到私钥
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            // 得到公钥
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            String publicKeyString = new String(Base64.encodeBase64(publicKey.getEncoded()));
            // 得到私钥字符串
            String privateKeyString = new String(Base64.encodeBase64((privateKey.getEncoded())));
            // 将公钥和私钥保存到Map
            //0表示公钥
            keyMap.put("publicKey", publicKeyString);
            //1表示私钥
            keyMap.put("privateKey", privateKeyString);
            return keyMap;
        }catch(Throwable throwable){
            LogUtil.error("生成RSA密钥对异常", throwable);
        }
        return null;
    }

    /*
     * 2023-08-01
     * 加密
     * @param message   需要加密的字符串
     * @param publicKey 公钥
     **/
    public static final String encrypt(final String message, final String publicKey) {
        //base64编码的公钥
        ByteArrayOutputStream byteArrayOutputStream = null;
        try{
            final byte[] decoded = Base64.decodeBase64(publicKey);
            final RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
            //RSA加密
            final Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            final byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
            //字符串长度
            final int len = bytes.length;
            int offset = 0;//偏移量
            int i = 0;//所分的段数
            byteArrayOutputStream = new ByteArrayOutputStream();
            while (len > offset) {
                byte[] cache;
                if (len - offset > 117) {
                    cache = cipher.doFinal(bytes, offset, 117);
                } else {
                    cache = cipher.doFinal(bytes, offset, len - offset);
                }
                byteArrayOutputStream.write(cache);
                i++;
                offset = 117 * i;
            }
            final byte[] bytes2 = byteArrayOutputStream.toByteArray();
            final String encryptMessage = Base64.encodeBase64String(bytes2);
            final String result = encryptMessage.replaceAll("[\r\n]", "");
            return result;
        }catch(Throwable throwable){
            LogUtil.error("Rsa2Util_encrypt_t3", throwable, "使用公钥对数据加密异常");
            return null;
        }finally {
            IOUtil.close(byteArrayOutputStream);
        }
    }

    /*
     * 2023-08-01
     * 解密
     * @param message    需要解密的密文
     * @param privateKey 私钥
     **/
    public static final String decrypt(final String message, final String privateKey) {
        ByteArrayOutputStream byteArrayOutputStream = null;
        try{
            String effectiveMessage = message;
            if (effectiveMessage.contains(" ")) {
                effectiveMessage = effectiveMessage.replaceAll(" ", "+");
            }
            //base64编码的私钥
            final byte[] decoded = Base64.decodeBase64(privateKey);
            RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, priKey);
            //64位解码加密后的字符串
            final byte[] inputByte = Base64.decodeBase64(effectiveMessage.getBytes(StandardCharsets.UTF_8));
            final int len = inputByte.length;//密文
            int offset = 0;//偏移量
            int i = 0;//段数
            byteArrayOutputStream = new ByteArrayOutputStream();
            while (len - offset > 0) {
                byte[] cache;
                if (len - offset > 128) {
                    cache = cipher.doFinal(inputByte, offset, 128);
                } else {
                    cache = cipher.doFinal(inputByte, offset, len - offset);
                }
                byteArrayOutputStream.write(cache);
                i++;
                offset = 128 * i;
            }
            return new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
        }catch(Throwable throwable){
            LogUtil.error("Rsa2Util_decrypt_t4", throwable, "使用私钥对数据解密异常");
            return null;
        }finally {
            IOUtil.close(byteArrayOutputStream);
        }
    }

    /*
     * 2023-08-01
     * 签名
     * @param data    需要签名的内容
     * @param privateKey 私钥
     **/
    public static final String sign(final byte[] data, final String privateKey) throws Throwable {
        // 解密由base64编码的私钥
        String decode = Base64Util.decodeStr2str(privateKey);
        byte[] keyBytes =decode.getBytes("utf-8");
        // 构造PKCS8EncodedKeySpec对象
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        // KEY_ALGORITHM 指定的加密算法
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        // 取私钥对象
        PrivateKey priKey = keyFactory.generatePrivate(pkcs8KeySpec);
        // 用私钥对信息生成数字签名
        Signature signature = Signature.getInstance("NONEwithRSA");
        signature.initSign(priKey);
        signature.update(data);
        byte[] sign = signature.sign();
        String base64Str = Base64Util.encodeBytes2str(sign);
        return base64Str;
    }

    /*
     * 2023-08-01
     * 验签
     * @param data    需要签名的内容
     * @param publicKey 公钥
     * @param sign 签名后内容
     **/
    public static final boolean verify(final byte[] data, final String publicKey, final String sign) throws Throwable {
        // 解密由base64编码的公钥
        byte[] keyBytes = Base64Util.decodeStr2bytes(publicKey);
        // 构造X509EncodedKeySpec对象
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        // KEY_ALGORITHM 指定的加密算法
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        // 取公钥对象
        PublicKey pubKey = keyFactory.generatePublic(keySpec);
        Signature signature = Signature.getInstance("NONEwithRSA");
        signature.initVerify(pubKey);
        signature.update(data);
        // 验证签名是否有效
        byte[] signBytes = Base64Util.decodeStr2bytes(sign);
        return signature.verify(signBytes);
    }
}
