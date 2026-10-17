package xc.crabapple.core.cipher.sha;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * 2017-04-28 1.0.0
 * 2024-04-10 添加参数注释、更新方法名
 * 2025-01-21 扩展重载方法，注释更新
 * SHA加密算法（消息摘要），单向不可逆
 */
public final class ShaUtil {

    private static final String[] p_hexDigits = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

    /**
     * 2025-08-20
     * 计算指定文件的SHA值
     * @param sourceUrl
     * @return
     */
    public static final String encryptFile1(final String sourceUrl){
        final String cipher = p_encryptFile(sourceUrl, "SHA-1");
        return cipher;
    }

    /**
     * 2025-08-20
     * 计算指定文件的SHA值
     * @param sourceUrl
     * @return
     */
    public static final String encryptFile224(final String sourceUrl){
        final String cipher = p_encryptFile(sourceUrl, "SHA-224");
        return cipher;
    }

    /**
     * 2025-08-20
     * 计算指定文件的SHA值
     * @param sourceUrl
     * @return
     */
    public static final String encryptFile256(final String sourceUrl){
        final String cipher = p_encryptFile(sourceUrl, "SHA-256");
        return cipher;
    }

    /**
     * 2025-08-20
     * 计算指定文件的SHA值
     * @param sourceUrl
     * @return
     */
    public static final String encryptFile384(final String sourceUrl){
        final String cipher = p_encryptFile(sourceUrl, "SHA-384");
        return cipher;
    }

    /**
     * 2025-08-20
     * 计算指定文件的SHA值
     * @param sourceUrl
     * @return
     */
    public static final String encryptFile512(final String sourceUrl){
        final String cipher = p_encryptFile(sourceUrl, "SHA-512");
        return cipher;
    }

    /**
     * 2025-08-20
     * 计算指定文件的SHA值
     * @param sourceUrl
     * @param algorithm
     * @return
     */
    private static final String p_encryptFile(final String sourceUrl, final String algorithm){
        String effectiveSourceUrl = UrlUtil.getStandardPath(sourceUrl);
        final boolean isReal = FileUtil.isRealFile(effectiveSourceUrl, true);
        if(!isReal){
            LogUtil.error("ShaUtil_p_encryptFile_1", "非空文件不存在", effectiveSourceUrl, algorithm);
            return null;
        }
        if(!StringUtil.equalsOfOne(algorithm, "SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512")){
            LogUtil.error("ShaUtil_p_encryptFile_2", "SHA算法指定的类型不存在", algorithm);
            return null;
        }
        //
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            FileInputStream fis = new FileInputStream(effectiveSourceUrl);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte bt : hashBytes) {
                hexString.append(String.format("%02x", bt));
            }
            final String cipher = hexString.toString();
            return cipher;
        }catch (Throwable throwable){
            LogUtil.error("ShaUtil_p_encryptFile_t1", throwable, effectiveSourceUrl, algorithm);
            return null;
        }
    }

    /**
     * 2025-01-21
     * @param plaintext
     * @return
     */
    public static final String encrypt1(final String plaintext){
        final String cipher = p_encrypt(plaintext, "SHA-1");
        return cipher;
    }

    /**
     * 2025-01-21
     * @param plaintext
     * @return
     */
    public static final String encrypt224(final String plaintext){
        final String cipher = p_encrypt(plaintext, "SHA-224");
        return cipher;
    }

    /**
     * 2025-01-21
     * @param plaintext
     * @return
     */
    public static final String encrypt256(final String plaintext){
        final String cipher = p_encrypt(plaintext, "SHA-256");
        return cipher;
    }

    /**
     * 2025-01-21
     * @param plaintext
     * @return
     */
    public static final String encrypt384(final String plaintext){
        final String cipher = p_encrypt(plaintext, "SHA-384");
        return cipher;
    }

    /**
     * 2025-01-21
     * @param plaintext
     * @return
     */
    public static final String encrypt512(final String plaintext){
        final String cipher = p_encrypt(plaintext, "SHA-512");
        return cipher;
    }

    /**
     * 2017-04-28 1.0.0
     * SHA加密
     * @param plaintext 待加密的明文
     * @param algorithm SHA算法类型，可选值：SHA-1, SHA-224, SHA-256, SHA-384, SHA-512
     */
    private static final String p_encrypt(final String plaintext, final String algorithm){
        if(plaintext == null){
            return null;
        }
        if(!StringUtil.equalsOfOne(algorithm, "SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512")){
            LogUtil.error("ShaUtil_encrypt_2", "SHA算法指定的类型不存在", algorithm);
            return null;
        }
        try{
            final MessageDigest messageDigest = MessageDigest.getInstance(algorithm);  // 创建具有定算法名称的信息摘要
            final byte[] bytes = plaintext.getBytes("UTF-8");
            messageDigest.update(bytes);  //使用指定的字节数组对摘要进行最后更新
            final byte[] bytes2 = messageDigest.digest();  //完成摘要计算
            String cipher = p_byteArrayToHexString(bytes2);  //将得到的字节数组变成字符串返回
            cipher = StringUtil.toLowerCase(cipher);
            return cipher;
        }catch(Throwable throwable){
            LogUtil.error("ShaUtil_encrypt_t2", throwable, "SHA失败", plaintext, algorithm);
            return null;
        }
    }

    /**
     * 2025-01-21
     * @param plaintextBytes
     * @return
     */
    public static final byte[] encrypt1(final byte[] plaintextBytes){
        byte[] cipherBytes = p_encrypt(plaintextBytes, "SHA-1");
        return cipherBytes;
    }

    /**
     * 2025-01-21
     * @param plaintextBytes
     * @return
     */
    public static final byte[] encrypt224(final byte[] plaintextBytes){
        byte[] cipherBytes = p_encrypt(plaintextBytes, "SHA-224");
        return cipherBytes;
    }

    /**
     * 2025-01-21
     * @param plaintextBytes
     * @return
     */
    public static final byte[] encrypt256(final byte[] plaintextBytes){
        byte[] cipherBytes = p_encrypt(plaintextBytes, "SHA-256");
        return cipherBytes;
    }

    /**
     * 2025-01-21
     * @param plaintextBytes
     * @return
     */
    public static final byte[] encrypt384(final byte[] plaintextBytes){
        byte[] cipherBytes = p_encrypt(plaintextBytes, "SHA-384");
        return cipherBytes;
    }

    /**
     * 2025-01-21
     * @param plaintextBytes
     * @return
     */
    public static final byte[] encrypt512(final byte[] plaintextBytes){
        byte[] cipherBytes = p_encrypt(plaintextBytes, "SHA-512");
        return cipherBytes;
    }

    /**
     * 2017-04-28 1.0.0
     * SHA加密
     * @param plaintextBytes 待加密的明文
     * @param algorithm SHA算法类型，可选值：SHA-1, SHA-224, SHA-256, SHA-384, SHA-512
     */
    public static final byte[] p_encrypt(final byte[] plaintextBytes, final String algorithm){
        if(plaintextBytes == null){
            return null;
        }
        if(!StringUtil.equalsOfOne(algorithm, "SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512")){
            LogUtil.error("ShaUtil_encrypt_1", "SHA算法指定的类型不存在", algorithm);
            return null;
        }
        try{
            final MessageDigest messageDigest = MessageDigest.getInstance(algorithm);  //创建具有指定算法名称的信息摘要
            messageDigest.update(plaintextBytes);  //使用指定的字节数组对摘要进行最后更新
            byte[] cipherBytes = messageDigest.digest();  //完成摘要计算并返回
            return cipherBytes;
        }catch(Throwable throwable){
            LogUtil.error("ShaUtil_encrypt_t1", throwable, "SHA失败", algorithm);
            return null;
        }
    }

    /**
     * 2017-04-28 1.0.0
     * 转换字节数组为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static final String p_byteArrayToHexString(final byte[] bytes){
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < bytes.length; i++){
            String str = p_byteToHexString(bytes[i]);
            stringBuilder.append(str);
        }
        String cipher = stringBuilder.toString();
        cipher = StringUtil.toLowerCase(cipher);
        return cipher;
    }

    /**
     * 2017-04-28 1.0.0
     * 将一个字节转化成十六进制形式的字符串
     */
    private static final String p_byteToHexString(final byte b){
        int ret = b;
        if(ret < 0){
            ret += 256;
        }
        int m = ret / 16;
        int n = ret % 16;
        String result = p_hexDigits[m] + p_hexDigits[n];
        result = result.toLowerCase();
        return result;
    }

}