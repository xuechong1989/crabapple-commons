package xc.crabapple.core.cipher.aes;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.Base64Util;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.cipher.bouncycastle.BouncyCastleUtil;
import xc.crabapple.core.url.UrlUtil;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * 2017-02-22 1.0.0
 * 2025-01-16 代码整理
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
 */
public final class AesUtil {

    /**
     * 2025-01-16
     * 生成密钥，16位ASCII字符串
     */
    public static final String generateKey() {
        final String key = RandomUtil.getStr(16, false);
        return key;
    }

    /**
     * 2023-07-31
     * @param key 密钥，16位ASCII字符串
     * 加密
     */
    public static final <T extends Object> T encrypt(final String key, final Object plaintextObject) throws Throwable {
        BouncyCastleUtil.addProvider();
        AesBean aesBean = new AesBean();
        aesBean.setLevel(256);
        aesBean.setKey(key);
        aesBean.setValue(plaintextObject);
        T t = encrypt(aesBean);
        return t;
    }

    /**
     * 2023-07-31
     * @param key 密钥，16位ASCII字符串
     * 加密
     */
    public static final <T extends Object> T encrypt(final Integer level, final String key, final Object plaintextObject) throws Throwable {
        BouncyCastleUtil.addProvider();
        AesBean aesBean = new AesBean();
        aesBean.setLevel(level);
        aesBean.setKey(key);
        aesBean.setValue(plaintextObject);
        T t = encrypt(aesBean);
        return t;
    }

    /**
     * 2020-11-09 6.9.0
     * 加密
     */
    public static final <T extends Object> T encrypt(final AesBean aesBean) throws Throwable {
        BouncyCastleUtil.addProvider();
        String key = aesBean.getKey();
        Object value = aesBean.getValue();
        Integer level = aesBean.getLevel();
        String pattern = aesBean.getPattern();
        Integer padding = aesBean.getPadding();
        String fromUrl = aesBean.getFromUrl();
        File fromFile = aesBean.getFromFile();
        String toUrl = aesBean.getToUrl();
        File toFile = aesBean.getToFile();
        String pkcs = "AES/"+pattern+"/PKCS"+padding+"Padding";
        //
        if(level != 128 && level != 192 && level != 256){
            LogUtil.error("AesUtil_encrypt_2", "AES加密时level参数错误，取值必须为128/192/256", level);
            return null;
        }
        //
        if(value instanceof byte[]){
            byte[] value_b = (byte[]) value;
            Object object = p_encrypt_bytes2bytes(key, value_b, level, pkcs);
            T t = (T) object;
            return t;
        }
        if(value instanceof String){
            String value_s = (String) value;
            Object object = p_encrypt_str2str(key, value_s, level, pkcs);
            T t = (T) object;
            return t;
        }
        //
        if(StringUtil.isBlank(fromUrl)){
            if(fromFile == null || !fromFile.exists() || !fromFile.isFile()){
                LogUtil.error("AesUtil_decrypt_10", "没有待加密的文件");
                return null;
            }
            fromUrl = fromFile.getAbsolutePath();
        }
        if(!StringUtil.isBlank(fromUrl)){
            if(StringUtil.isBlank(toUrl)){
                if(toFile != null && toFile.exists() && toFile.isFile()){
                    toUrl = toFile.getAbsolutePath();
                }
            }
            Object object = p_encrypt_file2file(key, fromUrl, toUrl, level);
            T t = (T) object;
            return t;
        }
        LogUtil.error("AesUtil_encrypt_1", "加密失败，要存入的数据类型错误", aesBean);
        return null;
    }

    /**
     * 2023-07-31
     * @param key 密钥，16位ASCII字符串
     * 解密
     */
    public static final <T extends Object> T decrypt(final String key, final Object cipherObject) throws Throwable {
        BouncyCastleUtil.addProvider();
        AesBean aesBean = new AesBean();
        aesBean.setLevel(256);
        aesBean.setKey(key);
        aesBean.setValue(cipherObject);
        T t = decrypt(aesBean);
        return t;
    }

    /**
     * 2023-07-31
     * @param key 密钥，16位ASCII字符串
     * 解密
     */
    public static final <T extends Object> T decrypt(final Integer level, final String key, final Object cipherObject) throws Throwable {
        BouncyCastleUtil.addProvider();
        AesBean aesBean = new AesBean();
        aesBean.setLevel(level);
        aesBean.setKey(key);
        aesBean.setValue(cipherObject);
        T t = decrypt(aesBean);
        return t;
    }

    /**
     * 2020-11-09 6.9.0
     * 解密
     */
    public static final <T extends Object> T decrypt(final AesBean aesBean) throws Throwable {
        BouncyCastleUtil.addProvider();
        String key = aesBean.getKey();
        Object value = aesBean.getValue();
        String dataType = aesBean.getDataType();
        Integer level = aesBean.getLevel();
        String pattern = aesBean.getPattern();
        Integer padding = aesBean.getPadding();
        String iv = aesBean.getIv();
        String fromUrl = aesBean.getFromUrl();
        File fromFile = aesBean.getFromFile();
        String toUrl = aesBean.getToUrl();
        File toFile = aesBean.getToFile();
        String pkcs = "AES/"+pattern+"/PKCS"+padding+"Padding";
        //
        if(level != 128 && level != 192 && level != 256){
            LogUtil.error("AesUtil_decrypt_2", "AES解密时level参数错误，取值必须为128/192/256", level);
            return null;
        }
        //
        if("bytes".equals(dataType)){
            byte[] value_b = (byte[]) value;
            Object object = p_decrypt_bytes2bytes(key, value_b, level, pkcs);
            T t = (T) object;
            return t;
        }
        if("string".equals(dataType)){
            String value_s = (String) value;
            if(StringUtil.isBlank(iv)){
                Object object = p_decrypt_str2str(key, value_s, level, pkcs);
                T t = (T) object;
                return t;
            }
            Object object = p_decrypt_str2str2(key, value_s, pkcs, iv);
            T t = (T) object;
            return t;
        }
        if("file".equals(dataType)){
            if(StringUtil.isBlank(fromUrl)){
                if(fromFile == null || !fromFile.exists() || !fromFile.isFile()){
                    LogUtil.error("AesUtil_decrypt_10", "没有待解密的文件");
                    return null;
                }
                fromUrl = fromFile.getAbsolutePath();
            }
            if(StringUtil.isBlank(toUrl)){
                if(toFile != null && toFile.exists() && toFile.isFile()){
                    toUrl = toFile.getAbsolutePath();
                }
            }
            Object object = p_decrypt_file2file(key, fromUrl, toUrl, level);
            T t = (T) object;
            return t;
        }
        LogUtil.error("AesUtil_decrypt_1", aesBean);
        return null;
    }

    /**
     * 2023-07-31
     * 文件加密，该方法会覆盖原文件
     * @param key 密钥，16位ASCII字符串
     * @param fromUrl 待加密文件
     */
    public static final <T extends Object> T encryptFile(final String key, final String fromUrl) throws Throwable {
        AesBean aesBean = new AesBean();
        aesBean.setLevel(256);
        aesBean.setKey(key);
        aesBean.setFromUrl(fromUrl);
        T t = encrypt(aesBean);
        return t;
    }

    /**
     * 2023-07-31
     * 文件加密
     * @param key 密钥，16位ASCII字符串
     * @param fromUrl 待加密文件
     * @param toUrl 加密后的文件，若该参数不传则会覆盖原文件
     */
    public static final <T extends Object> T encryptFile(final String key, final String fromUrl, final String toUrl) throws Throwable {
        AesBean aesBean = new AesBean();
        aesBean.setLevel(256);
        aesBean.setKey(key);
        aesBean.setFromUrl(fromUrl);
        aesBean.setToUrl(toUrl);
        T t = encrypt(aesBean);
        return t;
    }

    /**
     * 2023-07-31
     * 文件加密
     * @param level 长度，有128位，192位，256位
     * @param key 密钥，16位ASCII字符串
     * @param fromUrl 待加密文件
     * @param toUrl 加密后的文件，若该参数不传则会覆盖原文件
     */
    public static final <T extends Object> T encryptFile(final Integer level, final String key, final String fromUrl, final String toUrl) throws Throwable {
        AesBean aesBean = new AesBean();
        aesBean.setLevel(level);
        aesBean.setKey(key);
        aesBean.setFromUrl(fromUrl);
        aesBean.setToUrl(toUrl);
        T t = encrypt(aesBean);
        return t;
    }

    /**
     * 2023-07-31
     * 文件解密，该方法会覆盖原文件
     * @param key 密钥，16位ASCII字符串
     * @param fromUrl 待加密文件
     */
    public static final <T extends Object> T decryptFile(final String key, final String fromUrl) throws Throwable {
        AesBean aesBean = new AesBean();
        aesBean.setLevel(256);
        aesBean.setKey(key);
        aesBean.setFromUrl(fromUrl);
        T t = decrypt(aesBean);
        return t;
    }

    /**
     * 2023-07-31
     * 文件解密
     * @param key 密钥，16位ASCII字符串
     * @param fromUrl 待加密文件
     * @param toUrl 加密后的文件，若该参数不传则会覆盖原文件
     */
    public static final <T extends Object> T decryptFile(final String key, final String fromUrl, final String toUrl) throws Throwable {
        AesBean aesBean = new AesBean();
        aesBean.setLevel(256);
        aesBean.setKey(key);
        aesBean.setFromUrl(fromUrl);
        aesBean.setToUrl(toUrl);
        T t = decrypt(aesBean);
        return t;
    }

    /**
     * 2023-07-31
     * 文件解密
     * @param level 长度，有128位，192位，256位
     * @param key 密钥，16位ASCII字符串
     * @param fromUrl 待加密文件
     * @param toUrl 加密后的文件，若该参数不传则会覆盖原文件
     */
    public static final <T extends Object> T decryptFile(final Integer level, final String key, final String fromUrl, final String toUrl) throws Throwable {
        AesBean aesBean = new AesBean();
        aesBean.setLevel(level);
        aesBean.setKey(key);
        aesBean.setFromUrl(fromUrl);
        aesBean.setToUrl(toUrl);
        T t = decrypt(aesBean);
        return t;
    }

    /**
     * 2017-10-13 1.0.0
     * @param key 密钥，16位ASCII字符串
     */
    private static final byte[] p_encrypt_bytes2bytes(final String key, final byte[] value, final int level, final String pkcs){
        BouncyCastleUtil.addProvider();
        try{
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");  //AES：请求的密钥算法的标准名称
            SecureRandom secureRandom = getSecureRandom(key);
            keyGenerator.init(level, secureRandom);
            SecretKey secretKey = keyGenerator.generateKey();  //生成秘密（对称）密钥
            byte[] bytes = secretKey.getEncoded();  //返回基本编码格式的密钥
            //根据给定的字节数组构造一个密钥。enCodeFormat：密钥内容；Kernel.AES：与给定的密钥内容相关联的密钥算法的名称
            SecretKeySpec secretKeySpec = new SecretKeySpec(bytes, "AES");
            //创建一个实现指定转换的Cipher对象，该转换由指定的提供程序提供。
            //"AES/ECB/PKCS7Padding"：转换的名称；"BC"：提供程序的名称
            Cipher cipher = Cipher.getInstance(pkcs, "BC");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] bytes2 = cipher.doFinal(value);
            byte[] bytes3 = Base64Util.encodeBytes2bytes(bytes2);
            return bytes3;
        }catch(Throwable throwable){
            LogUtil.error("AesUtil_p_encrypt_bytes2bytes_1", throwable, level, pkcs);
            return null;
        }
    }

    /**
     * 2017-10-13 1.0.0
     * @param key 密钥，16位ASCII字符串
     */
    private static final byte[] p_decrypt_bytes2bytes(final String key, final byte[] value, final int level, final String pkcs) throws Throwable {
        BouncyCastleUtil.addProvider();
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = getSecureRandom(key);
        keyGenerator.init(level, secureRandom);
        SecretKey secretKey = keyGenerator.generateKey();
        byte[] bytes = secretKey.getEncoded();
        SecretKeySpec secretKeySpec = new SecretKeySpec(bytes, "AES");
        Cipher cipher = Cipher.getInstance(pkcs, "BC");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        byte[] bytes2 = Base64Util.decodeBytes2bytes(value);
        byte[] bytes3 = cipher.doFinal(bytes2);
        return bytes3;
    }

    /**
     * 2017-10-13 1.0.0
     * @param key 密钥，16位ASCII字符串
     */
    private static final byte[] p_encrypt_str2bytes(final String key, final String value, final int level, final String pkcs) throws Throwable {
        BouncyCastleUtil.addProvider();
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");  //AES：请求的密钥算法的标准名称
        SecureRandom secureRandom = getSecureRandom(key);
        keyGenerator.init(level, secureRandom);
        SecretKey secretKey = keyGenerator.generateKey();  //生成秘密（对称）密钥
        byte[] bytes = secretKey.getEncoded();  //返回基本编码格式的密钥
        //根据给定的字节数组构造一个密钥。enCodeFormat：密钥内容；Kernel.AES：与给定的密钥内容相关联的密钥算法的名称
        SecretKeySpec secretKeySpec = new SecretKeySpec(bytes, "AES");
        //创建一个实现指定转换的 Cipher对象，该转换由指定的提供程序提供。
        //"AES/ECB/PKCS7Padding"：转换的名称；"BC"：提供程序的名称
        Cipher cipher = Cipher.getInstance(pkcs, "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] bytes2 = value.getBytes("UTF-8");
        byte[] bytes3 = cipher.doFinal(bytes2);
        byte[] bytes4 = Base64Util.encodeBytes2bytes(bytes3);
        return bytes4;
    }

    /**
     * 2017-10-13 1.0.0
     * @param key 密钥，16位ASCII字符串
     */
    private static final String p_decrypt_bytes2str(final String key, final byte[] value, final int level, final String pkcs) throws Throwable {
        BouncyCastleUtil.addProvider();
        try{
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            SecureRandom secureRandom = getSecureRandom(key);
            keyGenerator.init(level, secureRandom);
            SecretKey secretKey = keyGenerator.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec secretKeySpec = new SecretKeySpec(enCodeFormat, "AES");
            Cipher cipher = Cipher.getInstance(pkcs, "BC");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] bytes = Base64Util.decodeBytes2bytes(value);
            byte[] bytes2 = cipher.doFinal(bytes);
            String result = StringUtil.bytes2str(bytes2);
            return result;
        }catch(Throwable throwable){
            LogUtil.error("AesUtil_p_decrypt_bytes2str_1", throwable, level, pkcs);
            return null;
        }
    }

    /**
     * 2017-10-13 1.0.0
     * @param key 密钥，16位ASCII字符串
     */
    private static final String p_encrypt_str2str(final String key, final String value, final int level, final String pkcs) throws Throwable {
        BouncyCastleUtil.addProvider();
        byte[] bytes = p_encrypt_str2bytes(key, value, level, pkcs);
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < bytes.length; i++){
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if(hex.length() == 1){
                hex = '0' + hex;
            }
            stringBuilder.append(hex.toUpperCase());
        }
        String result = stringBuilder.toString();
        result = StringUtil.str2utf8(result);
        return result;
    }

    /**
     * 2017-10-13 1.0.0
     * @param key 密钥，16位ASCII字符串
     */
    private static final String p_decrypt_str2str(final String key, final String value, final int level, final String pkcs){
        BouncyCastleUtil.addProvider();
        if(StringUtil.isBlank(value)){
            return null;
        }
        int n = value.length();
        if(n < 1){
            return null;
        }
        try{
            int n2 = n / 2;
            byte[] bytes = new byte[n2];
            for(int i = 0; i < n2; i++){
                String highs = value.substring(i * 2, i * 2 + 1);
                int high = Integer.parseInt(highs, 16);
                String lows = value.substring(i * 2 + 1, i * 2 + 2);
                int low = Integer.parseInt(lows, 16);
                bytes[i] = (byte) (high * 16 + low);
            }
            String result = p_decrypt_bytes2str(key, bytes, level, pkcs);
            return result;
        }catch(Throwable throwable){
            LogUtil.error("AesUtil_p_decrypt_str2str_1", throwable, value, level, pkcs);
            return null;
        }
    }

    /**
     * 2020-11-09 6.9.0
     * 可用于微信小程序的aes解密
     * @param key 密钥，16位ASCII字符串
     * @param value 密文，被加密的数据
     * @param iv 向量
     */
    private static final String p_decrypt_str2str2(final String key, final String value, final String pkcs, final String iv){
        BouncyCastleUtil.addProvider();
        byte[] valueBytes = Base64.decodeBase64(value);  //被加密的数据
        byte[] keyBytes = Base64.decodeBase64(key);  //加密秘钥
        byte[] ivBytes = Base64.decodeBase64(iv);  //向量
        try{
            int base = 16;
            //如果密钥不足16位，那么就补足. 这个if中的内容很重要
            if (keyBytes.length % base != 0) {
                int groups = keyBytes.length / base + (keyBytes.length % base != 0 ? 1 : 0);
                byte[] tempBytes = new byte[groups * base];
                Arrays.fill(tempBytes, (byte) 0);
                System.arraycopy(keyBytes, 0, tempBytes, 0, keyBytes.length);
                keyBytes = tempBytes;
            }
            //初始化
            Cipher cipher = Cipher.getInstance(pkcs, "BC");
            SecretKeySpec spec = new SecretKeySpec(keyBytes, "AES");
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
            parameters.init(ivParameterSpec);
            cipher.init(Cipher.DECRYPT_MODE, spec, parameters);// 初始化
            byte[] bytes = cipher.doFinal(valueBytes);
            if(!CollectionUtil.isBlank(bytes)){
                String result = new String(bytes, "UTF-8");
                return result;
            }
            return null;
        }catch(Throwable throwable){
            LogUtil.error("AesUtil_p_decrypt_str2str2_1", throwable, value, pkcs);
            return null;
        }
    }

    /**
     * 2017-10-13 1.0.0
     * 加密指令为key，对文件urlFrom加密并把密文另存为文件urlTo
     * @param key 密钥，16位ASCII字符串
     * @param fromUrl 待加密文件
     * @param toUrl 加密后的文件，若该参数不传则会覆盖原文件
     * @param level 长度，有128位，192位，256位
     */
    private static final String p_encrypt_file2file(final String key, final String fromUrl, final String toUrl, final int level){
        BouncyCastleUtil.addProvider();
        if(StringUtil.isBlankOfOne(key, fromUrl)){
            LogUtil.error("AesUtil_p_encrypt_file2file_1", "AES加密文件时参数错误", fromUrl, toUrl);
            return "";
        }
        if(level != 128 && level != 192 && level != 256){
            LogUtil.error("AesUtil_p_encrypt_file2file_2", "AES加密文件时level参数错误", level);
            return "";
        }
        boolean isCoverOriginalFile = false;
        String localFromUrl = UrlUtil.getStandardPath(fromUrl);
        String localToUrl = UrlUtil.getStandardPath(toUrl);
        if(StringUtil.isBlank(localToUrl)){
            final String type = FileUtil.getFileType(localFromUrl);
            final String fromPath = FileUtil.getPathByUrl(localFromUrl);
            final long snow = System.currentTimeMillis();
            final String randomStr = RandomUtil.getStr(16);
            localToUrl = fromPath + "/" + randomStr + "_" + snow + "." + type;
            isCoverOriginalFile = true;
        }
        //
        String status = "";
        InputStream inputStream = null;
        CipherInputStream cipherInputStream = null;
        OutputStream outputStream = null;
        try{
            inputStream = new FileInputStream(localFromUrl);
            final Cipher cipher = Cipher.getInstance("AES");  //创建密码器
            final Key secretKey = getSecretKey2(key, level);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);  //初始化
            cipherInputStream = new CipherInputStream(inputStream, cipher);
            outputStream = new FileOutputStream(localToUrl);
            IOUtils.copy(cipherInputStream, outputStream);
            status = "success";
            return status;
        }catch(Throwable throwable){
            LogUtil.error("AesUtil_p_encrypt_file2file_t1", throwable, localFromUrl, localToUrl, level);
            return null;
        }finally {
            IOUtil.close(outputStream, cipherInputStream, inputStream);
            final boolean isSuccess = StringUtil.equals(status, "success");
            if (isCoverOriginalFile && isSuccess) {
                FileUtil.fileDelete(localFromUrl);
                FileUtil.rename(localToUrl, localFromUrl);
            }
        }
    }

    /**
     * 2017-10-13 1.0.0
     * 加密指令为key，对文件urlFrom解密并把明文另存为文件urlTo
     * @param key 密钥，16位ASCII字符串
     * @param fromUrl 待加密文件
     * @param toUrl 加密后的文件，若该参数不传则会覆盖原文件
     * @param level 长度，有128位，192位，256位
     */
    private static final String p_decrypt_file2file(final String key, final String fromUrl, final String toUrl, final int level){
        BouncyCastleUtil.addProvider();
        if(StringUtil.isBlankOfOne(key, fromUrl)){
            LogUtil.error("AesUtil_p_decrypt_file2file_1", "AES解密文件时参数错误", fromUrl, toUrl);
            return "";
        }
        if(level != 128 && level != 192 && level != 256){
            LogUtil.error("AesUtil_p_decrypt_file2file_2", "AES解密文件时level参数错误", level);
            return "";
        }
        boolean isCoverOriginalFile = false;
        String localFromUrl = UrlUtil.getStandardPath(fromUrl);
        String localToUrl = UrlUtil.getStandardPath(toUrl);
        if(StringUtil.isBlank(localToUrl)){
            final String type = FileUtil.getFileType(localFromUrl);
            final String fromPath = FileUtil.getPathByUrl(localFromUrl);
            final long snow = System.currentTimeMillis();
            final String randomStr = RandomUtil.getStr(16);
            localToUrl = fromPath + "/" + randomStr + "_" + snow + "." + type;
            isCoverOriginalFile = true;
        }
        //
        String status = "";
        InputStream inputStream = null;
        CipherInputStream cipherInputStream = null;
        OutputStream outputStream = null;
        try{
            inputStream = new FileInputStream(localFromUrl);
            Cipher cipher = Cipher.getInstance("AES");  //创建密码器
            Key secretKey = getSecretKey2(key, level);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);  //初始化
            cipherInputStream = new CipherInputStream(inputStream, cipher);
            outputStream = new FileOutputStream(localToUrl);
            IOUtils.copy(cipherInputStream, outputStream);
            status = "success";
            return status;
        }catch(Throwable throwable){
            LogUtil.error("AesUtil_p_decrypt_file2file_t1", throwable, "AES解密文件失败", localFromUrl, localToUrl, level);
            return null;
        }finally {
            IOUtil.close(outputStream, cipherInputStream, inputStream);
            final boolean isSuccess = StringUtil.equals(status, "success");
            if (isCoverOriginalFile && isSuccess) {
                FileUtil.fileDelete(localFromUrl);
                FileUtil.rename(localToUrl, localFromUrl);
            }
        }
    }

    /**
     * 2017-04-28 1.0.0
     * 方法1，获得密钥
     */
    public static final String getSecretKey(final String mykey){
        BouncyCastleUtil.addProvider();
        try{
            SecureRandom secureRandom = getSecureRandom(mykey);
            KeyGenerator kg = null;
            kg = KeyGenerator.getInstance("AES");
            kg.init(secureRandom);
            //kg.init(56, secureRandom);
            SecretKey secretKey = kg.generateKey();  //生成密钥
            byte[] bs = secretKey.getEncoded();
            String result = Base64Util.encodeBytes2str(bs);
            return result;
        }catch(Throwable throwable){
            LogUtil.error("AesUtil_getSecretKey_1", throwable);
            return null;
        }
    }

    /**
     * 2017-04-28 1.0.0
     * 方法2，获得密钥（云盘老版）
     * @param mykey 指定的指令，如myapp
     * @param keyLength 长度，有128位，256位
     */
    public static final Key getSecretKey2(final String mykey, final int keyLength){
        BouncyCastleUtil.addProvider();
        Key key = null;
        try{
            KeyGenerator kgen = KeyGenerator.getInstance("AES");  //创建AES的Key生产者
            SecureRandom secureRandom = getSecureRandom(mykey);
            kgen.init(keyLength, secureRandom);  //用户密码作为随机数初始化出128位的key生产者，加密没关系，SecureRandom是生成安全随机数序列，password.getBytes(Kernel.UTF8)是种子，只要种子相同，序列就一样，所以解密只要有password就行
            SecretKey secretKey = kgen.generateKey();  //根据用户密码，生成一个密钥
            byte[] enCodeFormat = secretKey.getEncoded();  //返回基本编码格式的密钥，如果此密钥不支持编码，则返回
            key = new SecretKeySpec(enCodeFormat, "AES");  //转换为AES专用密钥
            return key;
        }catch(Throwable throwable){
            LogUtil.error("AesUtil_getSecretKey2_1", throwable, keyLength);
            return key;
        }
    }

    /**
     * 2017-04-28 1.0.0
     * http://www.eoeandroid.com/thread-63284-2-1.html
     * http://www.myexception.cn/operating-system/466379.html
     * SecureRandom实现完全随操作系统本身的内部状态，除非调用方在调用getInstance方法之后又调用了setSeed方法，
     * 该实现在windows上每次生成的key都相同，但是在solaris或部分linux系统上则不同。
     * @param mykey
     */
    public static final SecureRandom getSecureRandom(final String mykey){
        BouncyCastleUtil.addProvider();
        try{
            byte[] bytes = mykey.getBytes("UTF-8");
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");  //"Crypto"
            secureRandom.setSeed(bytes);
            return secureRandom;
        }catch(Throwable throwable){
            LogUtil.error("AesUtil_getSecureRandom_1", throwable);
            return null;
        }
    }

}