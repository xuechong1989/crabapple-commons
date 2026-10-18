package xc.crabapple.ssm.fs.mongodb;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.Security;

/**
 * xuechong 20170222 1.0.0
 * AES加密算法,256密钥KEY
 * java的aes192和256算法遇到Illegal key size or default parameters异常，由于jdk美国进出口控制，默认jdk做不到aes192和256
 * 解决方案：
 * 从网上下载local_policy.jar和US_export_policy.jar，注意jdk对应的版本
 * 在目录Java/jdk1.7.0_67/jre/lib/security，替换掉之前的这两个文件即可，另外在目录Java/jre7/lib/security中也可以替换掉之前的这两个文件
 *
 * 需要依赖包(共4个)：
 * local_policy.jar (maven)
 * US_export_policy.jar (maven)
 * bcprov-jdk15-1.45.jar (maven)
 *
 */
public final class AESUtil{

    public static final int KEY256 = 256;  //256位密钥
    public static final String AES = "AES";
    public static final String AES_ECB_PKCS5 = "AES/ECB/PKCS5Padding";
    public static final String AES_ECB_PKCS7 = "AES/ECB/PKCS7Padding";
    public static final String AES_ECB_PKCSNO = "AES/ECB/NoPadding";
    public static final String BC = "BC";  //仅在Cipher.getInstance方法使用时AES_ECB_PKCS的第二个参数

    /**
     * xuechong 20171013 1.0.0
     * @param mykey
     * @param content
     * @param keyLength
     * @return
     */
    public static byte[] encryptBytes(String mykey, byte[] content, int keyLength) throws Exception{
        KeyGenerator kgen = KeyGenerator.getInstance(AES);  //AES：请求的密钥算法的标准名称
        SecureRandom secureRandom = SecretKeyUtil.getSecureRandom(mykey);
        kgen.init(keyLength, secureRandom);
        SecretKey secretKey = kgen.generateKey();  //生成秘密（对称）密钥
        byte[] enCodeFormat = secretKey.getEncoded();  //返回基本编码格式的密钥
        //根据给定的字节数组构造一个密钥。enCodeFormat：密钥内容；Kernel.AES：与给定的密钥内容相关联的密钥算法的名称
        SecretKeySpec key = new SecretKeySpec(enCodeFormat, AES);
        Security.addProvider(new BouncyCastleProvider());  //将提供程序添加到下一个可用位置
        //创建一个实现指定转换的 Cipher对象，该转换由指定的提供程序提供。
        //"AES/ECB/PKCS7Padding"：转换的名称；"BC"：提供程序的名称
        Cipher cipher = Cipher.getInstance(AES_ECB_PKCS7, BC);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cryptograph = cipher.doFinal(content);
        byte[] byteResult = Base64Utils.encode(cryptograph);
        return byteResult;
    }

    /**
     * xuechong 20171013 1.0.0
     * @param mykey
     * @param cryptograph
     * @param keyLength
     * @return
     */
    public static byte[] decryptBytes(String mykey, byte[] cryptograph, int keyLength) throws Exception{
        KeyGenerator kgen = KeyGenerator.getInstance(AES);
        SecureRandom secureRandom = SecretKeyUtil.getSecureRandom(mykey);
        kgen.init(keyLength, secureRandom);
        SecretKey secretKey = kgen.generateKey();
        byte[] enCodeFormat = secretKey.getEncoded();
        SecretKeySpec key = new SecretKeySpec(enCodeFormat, AES);
        Security.addProvider(new BouncyCastleProvider());
        Cipher cipher = Cipher.getInstance(AES_ECB_PKCS7, BC);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] bs = Base64Utils.decode(cryptograph);
        byte[] content = cipher.doFinal(bs);
        return content;
    }

    /**
     * xuechong 20171013 1.0.0
     * @param mykey
     * @param content
     * @param keyLength
     * @return
     */
    public static byte[] encrypt(String mykey, String content, int keyLength) throws Exception{
        KeyGenerator kgen = KeyGenerator.getInstance(AES);  //AES：请求的密钥算法的标准名称
        SecureRandom secureRandom = SecretKeyUtil.getSecureRandom(mykey);
        kgen.init(keyLength, secureRandom);
        SecretKey secretKey = kgen.generateKey();  //生成秘密（对称）密钥
        byte[] enCodeFormat = secretKey.getEncoded();  //返回基本编码格式的密钥
        //根据给定的字节数组构造一个密钥。enCodeFormat：密钥内容；Kernel.AES：与给定的密钥内容相关联的密钥算法的名称
        SecretKeySpec key = new SecretKeySpec(enCodeFormat, AES);
        Security.addProvider(new BouncyCastleProvider());  //将提供程序添加到下一个可用位置
        //创建一个实现指定转换的 Cipher对象，该转换由指定的提供程序提供。
        //"AES/ECB/PKCS7Padding"：转换的名称；"BC"：提供程序的名称
        Cipher cipher = Cipher.getInstance(AES_ECB_PKCS7, BC);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] byteContent = content.getBytes("UTF-8");
        byte[] cryptograph = cipher.doFinal(byteContent);
        byte[] byteResult = Base64Utils.encode(cryptograph);
        return byteResult;
    }

}
