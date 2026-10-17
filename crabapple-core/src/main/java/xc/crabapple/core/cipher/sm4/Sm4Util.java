package xc.crabapple.core.cipher.sm4;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.cipher.bouncycastle.BouncyCastleUtil;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.SecureRandom;

/**
 * 2023-06-26 构建国密4工具类
 * 2023-08-03 可配置化、重载方法
 * 2025-01-16 代码整理
 * 国密4，sm4加解密，本地没问题，线上出现问题：Cannot find any provider supporting AES/CBC/PKCS7Padding
 * 解决方案：
 * （1）jar包冲突，删除bcprov-jdk14-140.jar或bcprov-jdk15-140.jar等文件。
 * （2）修改JDK配置文件：%JAVA_HOME%/jre/lib/security/java.security
 *     security.provider.7由com.sun.security.sasl.Provider改为org.bouncycastle.jce.provider.BouncyCastleProvider
 * （3）在目录%JAVA_HOME%/jre/lib/ext中添加bcprov系列jar文件，如bcprov-jdk15-140.jar。
 */
public final class Sm4Util {

    private static String ENCODING = "UTF-8";  // 默认值常量，在实际使用IV/MODE/PADDING/KEY_SIZE均可动态配置
    private static String NAME = "SM4";
    private static String IV = "ASDFGHJKL8899000";
    private static int PADDING = 7;
    private static String MODE = "CBC";
    private static int KEY_SIZE = 128;   // CBC模式下自定义偏移量 16位字符

    /**
     * 2025-01-16
     * 生成密钥，16位ASCII字符串
     */
    public static final String generateKey() {
        final String key = RandomUtil.getStr(16, false);
        return key;
    }

    /**
     * 2023-06-27
     * 生成bsse64密钥
     */
    private static final String generateKeyBase64() {
        BouncyCastleUtil.addProvider();
        try{
            KeyGenerator keyGenerator = KeyGenerator.getInstance(NAME, BouncyCastleProvider.PROVIDER_NAME);
            SecureRandom secureRandom = new SecureRandom();
            keyGenerator.init(KEY_SIZE, secureRandom);
            SecretKey secretKey = keyGenerator.generateKey();
            byte[] bytes =  secretKey.getEncoded();
            final String key = Base64.toBase64String(bytes);
            return key;
        }catch(Throwable throwable){
            LogUtil.error("Sm4Util_generateBase64Key_t1", throwable);
            return null;
        }
    }

    /**
     * 2025-01-21
     * @return
     */
    public static final String generateIv(){
        final String iv = RandomUtil.getStr(16, false);
        return iv;
    }

    public static final String encrypt(final String key, final String plaintextStr) {
        final String cipherStr = encrypt(key, IV, plaintextStr, MODE, PADDING);
        return cipherStr;
    }

    public static final byte[] encrypt(final String key, final Object plaintextBytes) {
        if(plaintextBytes instanceof byte[]){
            byte[] bytes = (byte[]) plaintextBytes;
            final byte[] conTextBytes = encrypt(key, IV, bytes, MODE, PADDING);
            return conTextBytes;
        }
        return null;
    }

    public static final String encrypt(final String key, final String iv, final String plaintextStr) {
        final String cipherStr = encrypt(key, iv, plaintextStr, MODE, PADDING);
        return cipherStr;
    }

    public static final String encrypt(final String key, final String iv, final String plaintextStr, final String modeName) {
        final String cipherStr = encrypt(key, iv, plaintextStr, modeName, PADDING);
        return cipherStr;
    }

    /**
     * 2023-06-27
     * sm4字符串加密
     * @param key 密钥
     * @param plaintextStr 待加密字符串
     * @return 返回Base64编码的加密字符串
     * 密文长度不固定，会随着被加密字符串长度的变化而变化
     */
    public static final String encrypt(final String key, final String iv, final String plaintextStr, final String modeName, final Integer padding) {
        BouncyCastleUtil.addProvider();
        p_checkKey(key);
        if(StringUtil.isBlank(plaintextStr)) {
            LogUtil.error("Sm4Util_encrypt_1", "sm4 encrypt str is null");
            return null;
        }
        try{
            byte[] keyData = key.getBytes(ENCODING);
            byte[] srcData = plaintextStr.getBytes(ENCODING);
            byte[] cipherArray = p_encrypt(keyData, iv, srcData, modeName, padding);
            final String cipherStr = Base64.toBase64String(cipherArray);
            return cipherStr;
        }catch(Throwable throwable){
            LogUtil.error("Sm4Util_encrypt_t1", throwable);
            return null;
        }
    }

    public static final byte[] encrypt(final String key, final String iv, final byte[] plaintextBytes, final String modeName, final Integer padding) {
        BouncyCastleUtil.addProvider();
        p_checkKey(key);
        if(CollectionUtil.isBlank(plaintextBytes)) {
            LogUtil.error("Sm4Util_encrypt_2", "sm4 encrypt str is null");
            return null;
        }
        try{
            byte[] keyData = key.getBytes(ENCODING);
            byte[] cipherArray = p_encrypt(keyData, iv, plaintextBytes, modeName, padding);
            return cipherArray;
        }catch(Throwable throwable){
            LogUtil.error("Sm4Util_encrypt_t2", throwable);
            return null;
        }
    }

    public static final String decrypt(final String key, final String cipherStr) {
        final String str = decrypt(key, IV, cipherStr, MODE, PADDING);
        return str;
    }

    public static final String decrypt(final String key, final String iv, final String cipherStr) {
        final String str = decrypt(key, iv, cipherStr, MODE, PADDING);
        return str;
    }

    public static final String decrypt(final String key, final String iv, final String cipherStr, final String modeName) {
        final String str = decrypt(key, iv, cipherStr, modeName, PADDING);
        return str;
    }

    /**
     * 2023-06-27
     * sm4字符串解密
     * @param key 密钥
     * @param cipherStr 待解密字符串
     * @return 解密后的字符串
     */
    public static final String decrypt(final String key, final String iv, final String cipherStr, final String modeName, final Integer padding) {
        BouncyCastleUtil.addProvider();
        p_checkKey(key);
        if(StringUtil.isBlank(cipherStr)) {
            LogUtil.error("Sm4Util_decrypt_1", "sm4 decrypt cipherStr is null", key, iv, cipherStr, modeName, padding);
            return null;
        }
        try{
            byte[] keyData = key.getBytes(ENCODING);
            byte[] cipherData = Base64.decode(cipherStr);
            byte[] srcData = p_decrypt(keyData, iv, cipherData, modeName, padding);
            if(CollectionUtil.isBlank(srcData)){
                LogUtil.error("Sm4Util_decrypt_2", "数据为空", key, iv, cipherStr, modeName, padding);
                return null;
            }
            final String str = new String(srcData, ENCODING);
            return str;
        }catch(Throwable throwable){
            LogUtil.error("Sm4Util_decrypt_t1", throwable, key, iv, cipherStr, modeName, padding);
            return null;
        }
    }

    /**
     * 2024-05-27
     * 文件流解密
     * @param key
     * @param inputStream
     * @return
     */
    public static final byte[] decrypt(final String key, final InputStream inputStream) {
        return decrypt(key, IV, PADDING, MODE, inputStream);
    }

    /**
     * 2024-05-27
     * 文件流解密
     * @param key
     * @param iv
     * @param padding
     * @param modeName
     * @param inputStream
     * @return
     */
    public static final byte[] decrypt(final String key, final String iv, final Integer padding, final String modeName, final InputStream inputStream) {
        BouncyCastleUtil.addProvider();
        p_checkKey(key);
        try{
            byte[] fileBytes = IOUtils.toByteArray(inputStream);
            byte[] keyData = key.getBytes(ENCODING);
            byte[] decrypt = p_decrypt(keyData, iv, fileBytes, modeName, padding);
            return decrypt;
        }catch(Throwable throwable){
            LogUtil.error("Sm4Util_decrypt_t2", throwable);
            return null;
        }
    }

    public static final File encryptFile(final String key, final File targetFile) {
        File cipherFile = encryptFile(key, IV, targetFile, MODE, PADDING);
        return cipherFile;
    }

    public static final File encryptFile(final String key, final String iv, final File targetFile) {
        File cipherFile = encryptFile(key, iv, targetFile, MODE, PADDING);
        return cipherFile;
    }

    public static final File encryptFile(final String key, final String iv, final File targetFile, final String modeName) {
        File cipherFile = encryptFile(key, iv, targetFile, modeName, PADDING);
        return cipherFile;
    }

    /**
     * 2023-06-28
     * sm4文件对象加密
     * @param key 密钥
     * @param targetFile 待加密文件对象
     * @return 加密后的文件对象
     */
    public static final File encryptFile(final String key, final String iv, final File targetFile, final String modeName, final Integer padding) {
        BouncyCastleUtil.addProvider();
        p_checkKey(key);
        if(targetFile == null) {
            LogUtil.error("Sm4Util_encryptFile_1", "sm4 encryptFile targetFile is null");
            return null;
        }
        File fileResult = null;
        InputStream inputStream = null;
        FileInputStream fileInputStream = null;
        try{
            fileInputStream = new FileInputStream(targetFile);
            long length = targetFile.length();
            byte[] fileBytes = new byte[(int)length];
            fileInputStream.read(fileBytes);
            byte[] keyData = key.getBytes(ENCODING);
            byte[] encrypt = p_encrypt(keyData, iv, fileBytes, modeName, padding);
            inputStream = new ByteArrayInputStream(encrypt);
            fileResult = p_inputStream2file(inputStream, targetFile);
            return fileResult;
        }catch(Throwable throwable){
            LogUtil.error("Sm4Util_encryptFile_t1", throwable);
            return null;
        } finally {
            IOUtil.close(inputStream, fileInputStream);
        }
    }

    public static final String encryptFile(final String key, final String fromUrl) {
        final String result = encryptFile(key, IV, fromUrl, null, MODE, PADDING);
        return result;
    }

    public static final String encryptFile(final String key, final String fromUrl, final String toUrl) {
        final String result = encryptFile(key, IV, fromUrl, toUrl, MODE, PADDING);
        return result;
    }

    public static final String encryptFile(final String key, final String iv, final String fromUrl, final String toUrl) {
        final String result = encryptFile(key, iv, fromUrl, toUrl, MODE, PADDING);
        return result;
    }

    public static final String encryptFile(final String key, final String iv, final String fromUrl, final String toUrl, final String modeName) {
        final String result = encryptFile(key, iv, fromUrl, toUrl, modeName, PADDING);
        return result;
    }

    /**
     * 2023-08-01
     * sm4文件对象加密
     * @param key 密钥，是一个16个ASCII码的字符串，必填
     * @param fromUrl 待加密文件的路径地址 必填
     * @param toUrl 加密后文件的生成地址 非必填 如果未填写，则原文件被加密
     * @return 加密描述
     */
    public static final String encryptFile(final String key, final String iv, final String fromUrl, final String toUrl, final String modeName, final Integer padding) {
        BouncyCastleUtil.addProvider();
        p_checkKey(key);
        if(StringUtil.isBlank(fromUrl)) {
            LogUtil.error("Sm4Util_encryptFile_2", "sm4 encryptFile fromUrl is null");
            return null;
        }
        File plaintextFile = new File(fromUrl);
        long length = plaintextFile.length();
        if(length == 0) {
            LogUtil.error("Sm4Util_encryptFile_3", "sm4 encryptFile fromUrl->file is null");
            return null;
        }
        FileInputStream fileInputStream = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try{
            byte[] fileBytes = new byte[(int)length];
            fileInputStream = new FileInputStream(plaintextFile);
            fileInputStream.read(fileBytes);
            byte[] keyData = key.getBytes(ENCODING);
            byte[] encrypt = p_encrypt(keyData, iv, fileBytes, modeName, padding);
            inputStream = new ByteArrayInputStream(encrypt);
            if(toUrl == null || toUrl.length() <= 0) {
                outputStream = new FileOutputStream(fromUrl);
            } else {
                outputStream = new FileOutputStream(toUrl);
            }
            IOUtils.copy(inputStream, outputStream);
            return "success-encrypt";
        }catch(Throwable throwable){
            LogUtil.error("Sm4Util_encryptFile_t3", throwable, fromUrl);
            return null;
        } finally {
            IOUtil.close(inputStream, fileInputStream, outputStream);
        }
    }

    public static final File decryptFile(final String key, final File cipherFile) {
        final File targetFile = decryptFile(key, IV, cipherFile, MODE, PADDING);
        return targetFile;
    }

    public static final File decryptFile(final String key, final String iv, final File cipherFile) {
        final File targetFile = decryptFile(key, iv, cipherFile, MODE, PADDING);
        return targetFile;
    }

    public static final File decryptFile(final String key, final String iv, final File cipherFile, final String modeName) {
        final File targetFile = decryptFile(key, iv, cipherFile, modeName, PADDING);
        return targetFile;
    }

    /**
     * 2023-06-28
     * sm4文件对象解密
     * @param key 密钥，是一个16个ASCII码的字符串，必填
     * @param cipherFile 待解密文件对象
     * @return 解密后的文件对象
     */
    public static final File decryptFile(final String key, final String iv, final File cipherFile, final String modeName, final Integer padding) {
        BouncyCastleUtil.addProvider();
        p_checkKey(key);
        if(cipherFile == null) {
            LogUtil.error("Sm4Util_decryptFile_1", "sm4 decryptFile cipherFile is null");
            return null;
        }
        InputStream inputStream = null;
        FileInputStream fileInputStream = null;
        try{
            fileInputStream = new FileInputStream(cipherFile);
            long length = cipherFile.length();
            byte[] fileBytes = new byte[(int)length];
            fileInputStream.read(fileBytes);
            byte[] keyData = key.getBytes(ENCODING);
            byte[] encrypt = p_decrypt(keyData, iv, fileBytes, modeName, padding);
            inputStream = new ByteArrayInputStream(encrypt);
            File fileResult = p_inputStream2file(inputStream, cipherFile);
            return fileResult;
        }catch(Throwable throwable){
            LogUtil.error("Sm4Util_decryptFile_t1", throwable, key);
            return null;
        } finally {
            IOUtil.close(inputStream, fileInputStream);
        }
    }

    public static final String decryptFile(final String key, final String fromUrl) {
        final String result = decryptFile(key, IV, fromUrl, null, MODE, PADDING);
        return result;
    }

    public static final String decryptFile(final String key, final String fromUrl, final String toUrl) {
        final String result = decryptFile(key, IV, fromUrl, toUrl, MODE, PADDING);
        return result;
    }

    public static final String decryptFile(final String key, final String iv, final String fromUrl, final String toUrl) {
        final String result = decryptFile(key, iv, fromUrl, toUrl, MODE, PADDING);
        return result;
    }

    public static final String decryptFile(final String key, final String iv, final String fromUrl, final String toUrl, final String modeName) {
        final String result = decryptFile(key, iv, fromUrl, toUrl, modeName, PADDING);
        return result;
    }

    /**
     * 2023-08-01
     * sm4文件对象解密
     * @param key 密钥，是一个16个ASCII码的字符串，必填
     * @param fromUrl 待解密文件的路径地址 必填
     * @param toUrl 解密后文件的生成地址 非必填 如果未填写，则原文件被解密
     * @return 解密描述
     */
    public static final String decryptFile(final String key, final String iv, final String fromUrl, final String toUrl, final String modeName, final Integer padding) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try{
            inputStream = decryptFileReturnInputStream(key, iv, fromUrl, modeName, padding);
            if(StringUtil.isBlank(toUrl)) {
                outputStream = new FileOutputStream(fromUrl);
            } else {
                outputStream = new FileOutputStream(toUrl);
            }
            IOUtils.copy(inputStream, outputStream);
            return "success-decrypt";
        }catch(Throwable throwable){
            LogUtil.error("Sm4Util_decryptFile_t3", throwable, key);
            return null;
        } finally {
            IOUtil.close(outputStream, inputStream);
        }
    }

    /**
     * 2024-07-17
     * 解密指定文件，不产生新文件，直接返回输入流
     * @param key
     * @param fromUrl
     * @return
     */
    public static final InputStream decryptFileReturnInputStream(final String key, final String fromUrl) {
        InputStream inputStream = decryptFileReturnInputStream(key, IV, fromUrl, MODE, PADDING);
        return inputStream;
    }

    /**
     * 2024-07-17
     * 解密指定文件，不产生新文件，直接返回输入流
     * @param key
     * @param iv
     * @param fromUrl
     * @return
     */
    public static final InputStream decryptFileReturnInputStream(final String key, final String iv, final String fromUrl) {
        InputStream inputStream = decryptFileReturnInputStream(key, iv, fromUrl, MODE, PADDING);
        return inputStream;
    }

    /**
     * 2024-07-17
     * 解密指定文件，不产生新文件，直接返回输入流
     * @param key
     * @param iv
     * @param fromUrl
     * @param modeName
     * @return
     */
    public static final InputStream decryptFileReturnInputStream(final String key, final String iv, final String fromUrl, final String modeName) {
        InputStream inputStream = decryptFileReturnInputStream(key, iv, fromUrl, modeName, PADDING);
        return inputStream;
    }

    /**
     * 2024-07-17
     * 解密指定文件，不产生新文件，直接返回输入流
     * @param key
     * @param iv
     * @param fromUrl
     * @param modeName
     * @param padding
     * @return
     */
    public static final InputStream decryptFileReturnInputStream(final String key, final String iv, final String fromUrl, final String modeName, final Integer padding) {
        BouncyCastleUtil.addProvider();
        p_checkKey(key);
        if(StringUtil.isBlank(fromUrl)) {
            LogUtil.error("Sm4Util_decryptFileReturnInputStream_1", "sm4 encryptFile fromUrl is null");
            return null;
        }
        File cipherFile = new File(fromUrl);
        long length = cipherFile.length();
        if(length == 0) {
            LogUtil.error("Sm4Util_decryptFileReturnInputStream_2", "sm4 decryptFile fromUrl -> file is null");
            return null;
        }
        FileInputStream fileInputStream = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try{
            byte[] fileBytes = new byte[(int)length];
            fileInputStream = new FileInputStream(cipherFile);
            fileInputStream.read(fileBytes);
            byte[] keyData = key.getBytes(ENCODING);
            byte[] decrypt = p_decrypt(keyData, iv, fileBytes, modeName, padding);
            inputStream = new ByteArrayInputStream(decrypt);
            return inputStream;
        }catch(Throwable throwable){
            IOUtil.close(inputStream);
            LogUtil.error("Sm4Util_decryptFileReturnInputStream_t1", throwable, key);
            return null;
        } finally {
            IOUtil.close(fileInputStream, outputStream);
        }
    }

    public static final byte[] encryptBytes(final String key, final byte[] plainBytes) {
        byte[] ciperBytes = encryptBytes(key, IV, plainBytes, MODE, PADDING);
        return ciperBytes;
    }

    public static final byte[] encryptBytes(final String key, final String iv, final byte[] plainBytes) {
        byte[] ciperBytes = encryptBytes(key, iv, plainBytes, MODE, PADDING);
        return ciperBytes;
    }

    public static final byte[] encryptBytes(final String key, final String iv, final byte[] plainBytes, final String modeName) {
        byte[] ciperBytes = encryptBytes(key, iv, plainBytes, modeName, PADDING);
        return ciperBytes;
    }

    /**
     * 2023-07-06
     * sm4字节数组加密
     * @param key 密钥
     * @param plainBytes 待加密字节数组
     * @return 加密后的字节数组
     */
    public static final byte[] encryptBytes(final String key, final String iv, final byte[] plainBytes, final String modeName, final Integer padding) {
        BouncyCastleUtil.addProvider();
        p_checkKey(key);
        if(CollectionUtil.isBlank(plainBytes)) {
            LogUtil.error("Sm4Util_encryptBytes_1", "sm4 encryptBytes plaintextBytes is null", key);
            return null;
        }
        try{
            byte[] keyData = key.getBytes(ENCODING);
            byte[] bytes = p_encrypt(keyData, iv, plainBytes, modeName, padding);
            return bytes;
        }catch(Throwable throwable){
            LogUtil.error("Sm4Util_encryptBytes_t1", throwable, key);
            return null;
        }
    }

    public static final byte[] decryptBytes(final String key, final byte[] cipherBytes) {
        final byte[] bytes = decryptBytes(key, IV, cipherBytes, MODE, PADDING);
        return bytes;
    }

    public static final byte[] decryptBytes(final String key, final String iv, final byte[] cipherBytes) {
        final byte[] bytes = decryptBytes(key, iv, cipherBytes, MODE, PADDING);
        return bytes;
    }

    public static final byte[] decryptBytes(final String key, final String iv, final byte[] cipherBytes, final String modeName) {
        final byte[] bytes = decryptBytes(key, iv, cipherBytes, modeName, PADDING);
        return bytes;
    }

    /**
     * 2023-07-06
     * sm4字节数组解密
     * @param key 密钥
     * @param cipherBytes 待解密字节数组
     * @return 解密后的字节数组
     */
    public static final byte[] decryptBytes(final String key, final String iv, final byte[] cipherBytes, final String modeName, final Integer padding) {
        BouncyCastleUtil.addProvider();
        p_checkKey(key);
        if(CollectionUtil.isBlank(cipherBytes)) {
            LogUtil.error("Sm4Util_decryptBytes_5", "sm4 decryptBytes cipherBytes is null", key);
            return null;
        }
        try{
            byte[] keyData = key.getBytes(ENCODING);
            byte[] bytes = p_decrypt(keyData, iv, cipherBytes, modeName, padding);
            return bytes;
        }catch(Throwable throwable){
            LogUtil.error("Sm4Util_decryptBytes_t5", throwable, key);
            return null;
        }
    }

    /**
     * 2026-08-12 所有文件全部最终存储在云盘 13-3
     * 流式加密：将明文输入流加密后返回密文输入流（CipherInputStream 内部 update+doFinal 分段处理，
     * SM4/CBC 链式连续，加密结果与整包 encryptBytes 完全一致，历史文件解密兼容）
     * @param key 密钥，16位ASCII字符串
     * @param iv 偏移量，16位ASCII字符串
     * @param inputStream 明文输入流（返回流关闭时级联关闭）
     * @param modeName 模式（CBC/ECB）
     * @param padding 填充（7=PKCS7Padding）
     * @return 密文输入流，失败返回null
     */
    public static final InputStream encryptReturnInputStream(final String key, final String iv, final InputStream inputStream, final String modeName, final Integer padding) {
        BouncyCastleUtil.addProvider();
        p_checkKey(key);
        if(inputStream == null){
            LogUtil.error("Sm4Util_encryptReturnInputStream_1", "sm4 encryptReturnInputStream inputStream is null");
            return null;
        }
        try{
            byte[] keyData = key.getBytes(ENCODING);
            Cipher cipher = p_generateCipher(Cipher.ENCRYPT_MODE, keyData, iv, modeName, padding);
            if(cipher == null){
                LogUtil.error("Sm4Util_encryptReturnInputStream_2", "sm4 encryptReturnInputStream cipher is null", key);
                return null;
            }
            CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);
            return cipherInputStream;
        }catch(Throwable throwable){
            LogUtil.error("Sm4Util_encryptReturnInputStream_t1", throwable, key);
            return null;
        }
    }

    /**
     * 2026-08-12 所有文件全部最终存储在云盘 13-3
     * 流式解密：将密文输入流解密后返回明文输入流（CipherInputStream 分段处理，结尾自动 doFinal 移除 PKCS7 填充）
     * @param key 密钥，16位ASCII字符串
     * @param iv 偏移量，16位ASCII字符串
     * @param inputStream 密文输入流（返回流关闭时级联关闭）
     * @param modeName 模式（CBC/ECB）
     * @param padding 填充（7=PKCS7Padding）
     * @return 明文输入流，失败返回null
     */
    public static final InputStream decryptReturnInputStream(final String key, final String iv, final InputStream inputStream, final String modeName, final Integer padding) {
        BouncyCastleUtil.addProvider();
        p_checkKey(key);
        if(inputStream == null){
            LogUtil.error("Sm4Util_decryptReturnInputStream_1", "sm4 decryptReturnInputStream inputStream is null");
            return null;
        }
        try{
            byte[] keyData = key.getBytes(ENCODING);
            Cipher cipher = p_generateCipher(Cipher.DECRYPT_MODE, keyData, iv, modeName, padding);
            if(cipher == null){
                LogUtil.error("Sm4Util_decryptReturnInputStream_2", "sm4 decryptReturnInputStream cipher is null", key);
                return null;
            }
            CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);
            return cipherInputStream;
        }catch(Throwable throwable){
            LogUtil.error("Sm4Util_decryptReturnInputStream_t1", throwable, key);
            return null;
        }
    }

    private static final byte[] p_encrypt(final byte[] key, final String iv, final byte[] data, final String modeName, final Integer padding) {
        Cipher cipher = p_generateCipher(Cipher.ENCRYPT_MODE, key, iv, modeName, padding);
        try{
            byte[] bytes = cipher.doFinal(data);
            return bytes;
        }catch(Throwable throwable){
            LogUtil.error("Sm4Util_p_encrypt_t1", throwable, key);
            return null;
        }
    }

    private static final byte[] p_decrypt(final byte[] key, final String iv, final byte[] cipherText, final String modeName, final Integer padding) {
        Cipher cipher = p_generateCipher(Cipher.DECRYPT_MODE, key, iv, modeName, padding);
        try{
            byte[] bytes = cipher.doFinal(cipherText);
            return bytes;
        }catch(Throwable throwable){
            LogUtil.error("Sm4Util_p_decrypt_t1", throwable, key);
            return null;
        }
    }

    private static final Cipher p_generateCipher(final int mode, final byte[] key, final String iv, final String modeName, final Integer padding) {
        String effectiveIv = iv;
        if(effectiveIv == null || "".equals(effectiveIv)){
            effectiveIv = IV;
        }
        String effectiveModeName = modeName;
        if(effectiveModeName == null || "".equals(effectiveModeName)){
            effectiveModeName = MODE;
        }
        effectiveModeName = effectiveModeName.toUpperCase();
        Integer effectivePadding = padding;
        if(effectivePadding == null || (effectivePadding != 0 && effectivePadding != 5 && effectivePadding != 7)){
            effectivePadding = PADDING;
        }
        final String modePadding = p_getModePadding(effectiveModeName, effectivePadding);
        try{
            Cipher cipher = Cipher.getInstance(modePadding, BouncyCastleProvider.PROVIDER_NAME);
            Key sm4Key = new SecretKeySpec(key, NAME);
            IvParameterSpec ivParameterSpec = null;
            if("CBC".equals(effectiveModeName)){
                byte[] ivBytes = effectiveIv.getBytes("UTF-8");
                ivParameterSpec = new IvParameterSpec(ivBytes);
            }
            cipher.init(mode, sm4Key, ivParameterSpec);
            return cipher;
        }catch(Throwable throwable){
            LogUtil.error("Sm4Util_p_generateCipher_t1", throwable, key);
            return null;
        }
    }

    /**
     * 2023-08-03
     * 返回编码填充模式与偏移量
     * @param mode
     * @param padding
     * @return
     */
    private static final String p_getModePadding(final String mode, final Integer padding){
        if(!"ECB".equals(mode) && !"CBC".equals(mode)){
            return "error-1";
        }
        if(padding != 0 && padding != 5 && padding != 7){
            return "error-2";
        }
        //
        String paddingStr = "NoPadding";
        if(padding > 0){
            paddingStr = "PKCS"+padding+"Padding";
        }
        final String modePadding = "SM4/" + mode + "/" + paddingStr;
        return modePadding;
    }

    private static final File p_inputStream2file(final InputStream inputStream, final File targetFile) {
        OutputStream outputStream = null;
        try{
            outputStream = new FileOutputStream(targetFile);
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return targetFile;
        }catch(Throwable throwable){
            LogUtil.error("Sm4Util_p_inputStream2file_t1", throwable);
            return null;
        } finally {
            IOUtil.close(outputStream, inputStream);
        }
    }

    private static final void p_checkKey(final String key) {
        if(StringUtil.isBlank(key)) {
            LogUtil.error("Sm4Util_p_checkKey_1", "sm4 secret key is null");
            return;
        }
        if(key.length() != 16 || key.getBytes().length != 16) {
            LogUtil.error("Sm4Util_p_checkKey_2", "sm4 secret key is not 16 byte");
        }
    }

}