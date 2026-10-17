package xc.crabapple.core.cipher.hash;

import org.bouncycastle.crypto.digests.SM3Digest;
import xc.crabapple.core.cipher.bouncycastle.BouncyCastleUtil;
import xc.crabapple.core.cipher.sha.ShaUtil;
import xc.crabapple.core.cipher.sm3.Sm3Util;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.cipher.bcrypt.BcryptUtil;
import xc.crabapple.core.cipher.md5.MD5Util;
import xc.crabapple.core.cipher.ska.SkaUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HexFormat;

/**
 * 2022-08-24
 * 不可逆哈希计算工具类
 */
public final class HashUtil {

    // 基于bcrypt进行哈希计算，安全性可靠性防破解能力比基于MD5的哈希要高得多，推荐采用
    private static final String BCRYPT_SALT_START = "";  // 底层代码盐-开始
    private static final String BCRYPT_SALT_LAST = "";  // 底层代码盐-结束
    // 基于MD5进行哈希计算
    private static final String MD5_SALT_START = "MD5#demo@2023-08-04";  // 底层代码盐-开始
    private static final String MD5_SALT_LAST = "/demo-efile#MD5";  // 底层代码盐-结束
    private static final Integer MD5_ITERATORS = 10;  // 迭代次数

    /**
     * 2025-10-21
     * 计算字符串的SM3哈希值
     * @param plaintext 输入字符串
     * @return 64字符的十六进制哈希字符串
     */
    public static final String getBySm3(final String plaintext) {
        String hashValue = Sm3Util.hash(plaintext);
        return hashValue;
    }

    /**
     * 2025-10-21
     * 计算字节数组的SM3哈希值
     * @param plaintextBytes 输入字节数组
     * @return 64字符的十六进制哈希字符串
     */
    public static final String getBySm3(final byte[] plaintextBytes) {
        String hashValue = Sm3Util.hash(plaintextBytes);
        return hashValue;
    }

    /**
     * 2025-10-21
     * 计算文件的SM3哈希值
     * @param url 文件路径
     * @return 64字符的十六进制哈希字符串
     */
    public static final String getBySm3File(final String url) {
        String hashValue = Sm3Util.hashFile(url);
        return hashValue;
    }

    /**
     * 2025-08-20
     * 计算指定字符串的SHA值
     * @param plaintext
     * @return
     */
    public static final String getBySha1(final String plaintext){
        final String cipher = ShaUtil.encrypt1(plaintext);
        return cipher;
    }

    /**
     * 2025-08-20
     * 计算指定字符串的SHA值
     * @param plaintext
     * @return
     */
    public static final String getBySha224(final String plaintext){
        final String cipher = ShaUtil.encrypt224(plaintext);
        return cipher;
    }

    /**
     * 2025-08-20
     * 计算指定字符串的SHA值
     * @param plaintext
     * @return
     */
    public static final String getBySha256(final String plaintext){
        final String cipher = ShaUtil.encrypt256(plaintext);
        return cipher;
    }

    /**
     * 2025-08-20
     * 计算指定字符串的SHA值
     * @param plaintext
     * @return
     */
    public static final String getBySha384(final String plaintext){
        final String cipher = ShaUtil.encrypt384(plaintext);
        return cipher;
    }

    /**
     * 2025-08-20
     * 计算指定字符串的SHA值
     * @param plaintext
     * @return
     */
    public static final String getBySha512(final String plaintext){
        final String cipher = ShaUtil.encrypt512(plaintext);
        return cipher;
    }

    /**
     * 2025-08-20
     * 计算指定文件的SHA值
     * @param sourceUrl
     * @return
     */
    public static final String getBySha1File(final String sourceUrl){
        final String cipher = ShaUtil.encryptFile1(sourceUrl);
        return cipher;
    }

    /**
     * 2025-08-20
     * 计算指定文件的SHA值
     * @param sourceUrl
     * @return
     */
    public static final String getBySha224File(final String sourceUrl){
        final String cipher = ShaUtil.encryptFile224(sourceUrl);
        return cipher;
    }

    /**
     * 2025-08-20
     * 计算指定文件的SHA值
     * @param sourceUrl
     * @return
     */
    public static final String getBySha256File(final String sourceUrl){
        final String cipher = ShaUtil.encryptFile256(sourceUrl);
        return cipher;
    }

    /**
     * 2025-08-20
     * 计算指定文件的SHA值
     * @param sourceUrl
     * @return
     */
    public static final String getBySha384File(final String sourceUrl){
        final String cipher = ShaUtil.encryptFile384(sourceUrl);
        return cipher;
    }

    /**
     * 2025-08-20
     * 计算指定文件的SHA值
     * @param sourceUrl
     * @return
     */
    public static final String getBySha512File(final String sourceUrl){
        final String cipher = ShaUtil.encryptFile512(sourceUrl);
        return cipher;
    }

    /**
     * 2023-08-04
     */
    public static final String getByBcrypt(final String plaintext){
        final String hash = getByBcrypt(plaintext, BCRYPT_SALT_START, BCRYPT_SALT_LAST);
        return hash;
    }

    /**
     * 2023-08-04
     */
    public static final String getByBcrypt(final String plaintext, final String saltStart){
        final String hash = getByBcrypt(plaintext, saltStart, BCRYPT_SALT_LAST);
        return hash;
    }

    /**
     * 2023-08-04
     * @param plaintext
     * @param saltStart
     * @param saltLast
     */
    public static final String getByBcrypt(final String plaintext, final String saltStart, final String saltLast){
        if(StringUtil.isBlank(plaintext)){
            LogUtil.error("HashUtil_getByBcrypt_1", "plaintext不能为空");
            return "";
        }
        String effectiveSaltStart = saltStart;
        String effectiveSaltLast = saltLast;
        if(effectiveSaltStart == null){
            effectiveSaltStart = BCRYPT_SALT_START;
        }
        if(effectiveSaltLast == null){
            effectiveSaltLast = BCRYPT_SALT_LAST;
        }
        final int plaintextLenth = plaintext.length();
        final int saltStartLength = effectiveSaltStart.length();
        final int saltLastLength = effectiveSaltLast.length();
        final int totalLength = plaintextLenth + saltStartLength + saltLastLength;
        if(totalLength > 128){
            LogUtil.error("HashUtil_getByBcrypt_2", "待处理字符串长度范围为[1-128]位，即ASCII码1-128位", plaintextLenth, saltStartLength, saltLastLength, totalLength);
            return "";
        }
        //
        String encryptPlaintext = SkaUtil.encrypt(plaintext, 5);
        encryptPlaintext = effectiveSaltStart + encryptPlaintext + effectiveSaltLast;
        encryptPlaintext = SkaUtil.encrypt(encryptPlaintext, 8);
        //
        final String hash = BcryptUtil.getHash(encryptPlaintext);
        //
        String encryptHash = SkaUtil.encrypt(hash, 30);
        //
        return encryptHash;
    }

    /**
     * 2023-08-04
     */
    public static final boolean checkByBcrypt(final String plaintext, final String hash){
        boolean isEQ = checkByBcrypt(plaintext, BCRYPT_SALT_START, BCRYPT_SALT_LAST, hash);
        return isEQ;
    }

    /**
     * 2023-08-04
     */
    public static final boolean checkByBcrypt(final String plaintext, final String saltStart, final String hash){
        boolean isEQ = checkByBcrypt(plaintext, saltStart, BCRYPT_SALT_LAST, hash);
        return isEQ;
    }

    /**
     * 2023-08-04
     * 注意，bcrypt哈希算法每次计算的结果都是不一样的，故不能直接比较，需要使用bcrypt类提供的方法比较
     */
    public static final boolean checkByBcrypt(final String plaintext, final String saltStart, final String saltLast, final String hash){
        if(StringUtil.isBlank(plaintext)){
            LogUtil.error("HashUtil_checkByBcrypt_1", "plaintext不能为空");
            return false;
        }
        String effectiveSaltStart = saltStart;
        String effectiveSaltLast = saltLast;
        if(effectiveSaltStart == null){
            effectiveSaltStart = "";
        }
        if(effectiveSaltLast == null){
            effectiveSaltLast = "";
        }
        final int plaintextLenth = plaintext.length();
        final int saltStartLength = effectiveSaltStart.length();
        final int saltLastLength = effectiveSaltLast.length();
        final int totalLength = plaintextLenth + saltStartLength + saltLastLength;
        if(totalLength > 128){
            LogUtil.error("HashUtil_checkByBcrypt_2", "待处理字符串长度范围为[1-128]，即ASCII码1-128位", plaintextLenth, saltStartLength, saltLastLength, totalLength);
            return false;
        }
        //
        String encryptPlaintext = SkaUtil.encrypt(plaintext, 5);
        encryptPlaintext = effectiveSaltStart + encryptPlaintext + effectiveSaltLast;
        encryptPlaintext = SkaUtil.encrypt(encryptPlaintext, 8);
        //
        String decryptHash = SkaUtil.decrypt(hash, 30);
        //
        final boolean isEQ = BcryptUtil.check(encryptPlaintext, decryptHash);
        return isEQ;
    }

    /**
     * 2023-08-04
     */
    public static final String getByMD5(final String plaintext){
        final String hash = getByMD5(plaintext, MD5_SALT_START, MD5_SALT_LAST, MD5_ITERATORS);
        return hash;
    }

    /**
     * 2023-08-04
     */
    public static final String getByMD5(final String plaintext, final String saltStart){
        final String hash = getByMD5(plaintext, saltStart, MD5_SALT_LAST, MD5_ITERATORS);
        return hash;
    }

    /**
     * 2023-08-04
     */
    public static final String getByMD5(final String plaintext, final String saltStart, final String saltLast){
        final String hash = getByMD5(plaintext, saltStart, saltLast, MD5_ITERATORS);
        return hash;
    }

    /**
     * 2023-08-04
     * @param plaintext
     * @param saltStart
     * @param saltLast
     * @param iterators 迭代次数，该值越大，安全性越高，性能越底
     */
    public static final String getByMD5(final String plaintext, final String saltStart, final String saltLast, final Integer iterators){
        if(plaintext == null){
            return "";
        }
        String effectiveSaltStart = saltStart;
        String effectiveSaltLast = saltLast;
        Integer effectiveIterators = iterators;
        if(effectiveSaltStart == null){
            effectiveSaltStart = MD5_SALT_START;
        }
        if(effectiveSaltLast == null){
            effectiveSaltLast = MD5_SALT_LAST;
        }
        if(effectiveIterators <= 0){
            effectiveIterators = MD5_ITERATORS;
        }
        String str = SkaUtil.encrypt(plaintext, 30);
        str = effectiveSaltStart + str + effectiveSaltLast;
        String hash = MD5Util.getHash(str);
        for(int i = 0; i < effectiveIterators - 1; i++){
            hash = MD5Util.getHash(hash);
        }
        hash = SkaUtil.encrypt(hash, 4);
        return hash;
    }

    /**
     * 2023-08-04
     */
    public static final boolean checkByMD5(final String plaintext, final String hash){
        boolean isEQ = checkByMD5(plaintext, MD5_SALT_START, MD5_SALT_LAST, MD5_ITERATORS, hash);
        return isEQ;
    }

    /**
     * 2023-08-04
     */
    public static final boolean checkByMD5(final String plaintext, final String saltStart, final String hash){
        boolean isEQ = checkByMD5(plaintext, saltStart, MD5_SALT_LAST, MD5_ITERATORS, hash);
        return isEQ;
    }

    /**
     * 2023-08-04
     */
    public static final boolean checkByMD5(final String plaintext, final String saltStart, final String saltLast, final String hash){
        boolean isEQ = checkByMD5(plaintext, saltStart, saltLast, MD5_ITERATORS, hash);
        return isEQ;
    }

    /**
     * 2023-08-04
     * MD5哈希值校验
     */
    public static final boolean checkByMD5(final String plaintext, final String saltStart, final String saltLast, final Integer iterators, final String hash){
        final String reHash = getByMD5(plaintext, saltStart, saltLast, iterators);
        if(reHash.equals(hash)){
            return true;
        }
        return false;
    }

    /**
     * 2025-08-20
     * 指定内容的SHA校验
     * @param plaintext
     * @param hash
     * @return
     */
    public static final boolean checkBySha1(final String plaintext, final String hash){
        final String cipher = ShaUtil.encrypt1(plaintext);
        if(StringUtil.equals(cipher, hash)){
            return true;
        }
        return false;
    }

    /**
     * 2025-08-20
     * 指定内容的SHA校验
     * @param plaintext
     * @param hash
     * @return
     */
    public static final boolean checkBySha224(final String plaintext, final String hash){
        final String cipher = ShaUtil.encrypt224(plaintext);
        if(StringUtil.equals(cipher, hash)){
            return true;
        }
        return false;
    }

    /**
     * 2025-08-20
     * 指定内容的SHA校验
     * @param plaintext
     * @param hash
     * @return
     */
    public static final boolean checkBySha256(final String plaintext, final String hash){
        final String cipher = ShaUtil.encrypt256(plaintext);
        if(StringUtil.equals(cipher, hash)){
            return true;
        }
        return false;
    }

    /**
     * 2025-08-20
     * 指定内容的SHA校验
     * @param plaintext
     * @param hash
     * @return
     */
    public static final boolean checkBySha384(final String plaintext, final String hash){
        final String cipher = ShaUtil.encrypt384(plaintext);
        if(StringUtil.equals(cipher, hash)){
            return true;
        }
        return false;
    }

    /**
     * 2025-08-20
     * 指定内容的SHA校验
     * @param plaintext
     * @param hash
     * @return
     */
    public static final boolean checkBySha512(final String plaintext, final String hash){
        final String cipher = ShaUtil.encrypt512(plaintext);
        if(StringUtil.equals(cipher, hash)){
            return true;
        }
        return false;
    }

    /**
     * 2025-08-20
     * 指定文件的SHA校验
     * @param sourceUrl
     * @param hash
     * @return
     */
    public static final boolean checkBySha1File(final String sourceUrl, final String hash){
        final String cipher = ShaUtil.encryptFile1(sourceUrl);
        if(StringUtil.equals(cipher, hash)){
            return true;
        }
        return false;
    }

    /**
     * 2025-08-20
     * 指定文件的SHA校验
     * @param sourceUrl
     * @param hash
     * @return
     */
    public static final boolean checkBySha224File(final String sourceUrl, final String hash){
        final String cipher = ShaUtil.encryptFile224(sourceUrl);
        if(StringUtil.equals(cipher, hash)){
            return true;
        }
        return false;
    }

    /**
     * 2025-08-20
     * 指定文件的SHA校验
     * @param sourceUrl
     * @param hash
     * @return
     */
    public static final boolean checkBySha256File(final String sourceUrl, final String hash){
        final String cipher = ShaUtil.encryptFile256(sourceUrl);
        if(StringUtil.equals(cipher, hash)){
            return true;
        }
        return false;
    }

    /**
     * 2025-08-20
     * 指定文件的SHA校验
     * @param sourceUrl
     * @param hash
     * @return
     */
    public static final boolean checkBySha384File(final String sourceUrl, final String hash){
        final String cipher = ShaUtil.encryptFile384(sourceUrl);
        if(StringUtil.equals(cipher, hash)){
            return true;
        }
        return false;
    }

    /**
     * 2025-08-20
     * 指定文件的SHA校验
     * @param sourceUrl
     * @param hash
     * @return
     */
    public static final boolean checkBySha512File(final String sourceUrl, final String hash){
        final String cipher = ShaUtil.encryptFile512(sourceUrl);
        if(StringUtil.equals(cipher, hash)){
            return true;
        }
        return false;
    }

}