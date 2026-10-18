package xc.crabapple.ssm.fs.mongodb;

import xc.crabapple.core.log.LogUtil;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.SecureRandom;

/**
 * xuechong 20170428 1.0.0
 * 密钥生成器
 */
public final class SecretKeyUtil{

    /**
     * xuechong 20170428 1.0.0
     * 方法2，获得密钥（云盘老版）
     * @param mykey 指定的指令，如cloudkd
     * @param keyLength 长度，有128位，256位
     */
    public static Key getSecretKey2(String mykey, int keyLength){
        Key key = null;
        try{
            KeyGenerator kgen = KeyGenerator.getInstance("AES");  //创建AES的Key生产者
            SecureRandom secureRandom = getSecureRandom(mykey);
            kgen.init(keyLength, secureRandom);  //用户密码作为随机数初始化出128位的key生产者，加密没关系，SecureRandom是生成安全随机数序列，password.getBytes(Kernel.UTF8)是种子，只要种子相同，序列就一样，所以解密只要有password就行
            SecretKey secretKey = kgen.generateKey();  //根据用户密码，生成一个密钥
            byte[] enCodeFormat = secretKey.getEncoded();  //返回基本编码格式的密钥，如果此密钥不支持编码，则返回
            key = new SecretKeySpec(enCodeFormat, "AES");  //转换为AES专用密钥
            return key;
        }catch(Exception e){
            LogUtil.error("SecretKeyUtil_getSecretKey2_1", e, mykey, keyLength);
            return key;
        }
    }

    /**
     * xuechong 20170428 1.0.0
     * http://www.eoeandroid.com/thread-63284-2-1.html
     * http://www.myexception.cn/operating-system/466379.html
     * SecureRandom实现完全随操作系统本身的内部状态，除非调用方在调用getInstance方法之后又调用了setSeed方法，
     * 该实现在windows上每次生成的key都相同，但是在solaris或部分linux系统上则不同。
     * @param mykey
     */
    public static SecureRandom getSecureRandom(String mykey){
        try{
            byte[] bytes = mykey.getBytes("UTF-8");
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");  //"Crypto"
            secureRandom.setSeed(bytes);
            return secureRandom;
        }catch(Exception e){
            LogUtil.error("SecretKeyUtil_getSecureRandom_1", e, mykey);
            return null;
        }
    }

}
