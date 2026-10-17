package xc.crabapple.core.cipher.aes;

import org.apache.commons.codec.binary.Base64;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.cipher.bouncycastle.BouncyCastleUtil;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

/*
 * 2023-04-18
 * 2025-01-16 代码整理
 * 传参加解密
 **/
public final class Aes3Util {

    // 加密等级
    private static Integer LEVEL = 128;
    // 模式+补码
    private static String PKCS5PADDING = "AES/ECB/PKCS5Padding";
    private static String PKCS7PADDING = "AES/ECB/PKCS7Padding";

    /**
     * 2025-01-16
     * 生成密钥，16位ASCII字符串
     */
    public static final String generateKey() {
        final String key = RandomUtil.getStr(16, false);
        return key;
    }

    public static final String base64Encode(final byte[] bytes) {
        String base64 = Base64.encodeBase64String(bytes);
        return base64;
    }

    public static final byte[] base64Decode(final String base64Code) {
        byte[] bytes = Base64.decodeBase64(base64Code);
        return bytes;
    }

    /*
     * AES加密
     * @param key 密钥，16位ASCII字符串
     * @param plaintextStr 待加密密文
     **/
    public static final String encrypt(final String key, final String plaintextStr) throws Throwable {
        BouncyCastleUtil.addProvider();
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(LEVEL);
        Cipher cipher = Cipher.getInstance(PKCS5PADDING);
        cipher.init(1, new SecretKeySpec(key.getBytes(), "AES"));
        byte[] plaintextBytes = plaintextStr.getBytes("UTF-8");
        byte[] cipherBytes = cipher.doFinal(plaintextBytes);
        String cipherStr = base64Encode(cipherBytes);
        return cipherStr;
    }

    /*
     * AES加密
     * @param key 密钥，16位ASCII字符串
     * @param cipherStr 待加密密文
     **/
    public static final byte[] encryptRetureBytes(final String key, final String cipherStr) throws Throwable {
        BouncyCastleUtil.addProvider();
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(LEVEL);
        Cipher cipher = Cipher.getInstance(PKCS5PADDING);
        cipher.init(1, new SecretKeySpec(key.getBytes(), "AES"));
        byte[] cipherBytes = cipher.doFinal(cipherStr.getBytes("UTF-8"));
        return cipherBytes;
    }

    /**
     * @param key 密钥，16位ASCII字符串
     * @param plaintextBytes
     * @return
     * @throws Throwable
     */
    public static final byte[] encryptBytesReturnBytes(final String key, final byte[] plaintextBytes) throws Throwable {
        BouncyCastleUtil.addProvider();
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(LEVEL);
        Cipher cipher = Cipher.getInstance(PKCS5PADDING);
        cipher.init(1, new SecretKeySpec(key.getBytes(), "AES"));
        byte[] cipherBytes = cipher.doFinal(plaintextBytes);
        return cipherBytes;
    }

    /*
     * AES解密
     * @param key 密钥，16位ASCII字符串
     * @param cipherBytes 加密密文
     **/
    public static final String decrypt(final String key, final byte[] cipherBytes) throws Throwable {
        BouncyCastleUtil.addProvider();
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(LEVEL);
        Cipher cipher = Cipher.getInstance(PKCS5PADDING);
        cipher.init(2, new SecretKeySpec(key.getBytes(), "AES"));
        byte[] decryptBytes = cipher.doFinal(cipherBytes);
        String plaintextStr = new String(decryptBytes);
        return plaintextStr;
    }

    /*
     * AES解密
     * @param key 密钥，16位ASCII字符串
     * @param cipherStr 密文
     **/
    public static final String decrypt(final String key, final String cipherStr) throws Throwable {
        BouncyCastleUtil.addProvider();
        byte[] cipherBytes = base64Decode(cipherStr);
        String plaintextStr = decrypt(key, cipherBytes);
        return plaintextStr;
    }

    /*
     * AES解密
     * @param key 密钥，16位ASCII字符串
     * @param cipherBytes 密文
     **/
    public static final byte[] decryptBytes(final String key, final byte[] cipherBytes) throws Throwable {
        BouncyCastleUtil.addProvider();
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(LEVEL);
        Cipher cipher = Cipher.getInstance(PKCS5PADDING);
        cipher.init(2, new SecretKeySpec(key.getBytes(), "AES"));
        byte[] plaintextBytes = cipher.doFinal(cipherBytes);
        return plaintextBytes;
    }

    /**
     * AES加密
     * @param key 密钥，16位ASCII字符串
     * @param encryptPath 源文件地址
     * @param decryptPath 加密后文件地址
     */
    public static final boolean encryptFile(final String key, final String encryptPath, final String decryptPath) {
        BouncyCastleUtil.addProvider();
        File encryptFile = null;
        File decryptfile = null;
        CipherOutputStream cipherOutputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try{
            encryptFile = new File(encryptPath);
            if (!encryptFile.exists()) {
                LogUtil.error("待加密文件为空,请联系管理员!");
            }
            decryptfile = new File(decryptPath);
            if (decryptfile.exists()) {
                decryptfile.delete();
            }
            decryptfile.createNewFile();
            Cipher cipher = initAESCipher(key, Cipher.ENCRYPT_MODE);
            cipherOutputStream = new CipherOutputStream(new FileOutputStream(decryptfile), cipher);
            bufferedInputStream = new BufferedInputStream(new FileInputStream(encryptFile));
            byte[] buffer = new byte[1024];
            int bufferLength;
            while ((bufferLength = bufferedInputStream.read(buffer)) != -1) {
                cipherOutputStream.write(buffer, 0, bufferLength);
            }
            bufferedInputStream.close();
            cipherOutputStream.close();
        }catch(Throwable throwable){
            LogUtil.error("Aes3Util_encryptFile_t1", throwable);
            FileUtil.dirClear(decryptfile.getAbsolutePath());
            return false;
        }
        return true;
    }

    /**
     * AES加密
     * @param key 密钥，16位ASCII字符串
     * @param encryptFile 源文件
     * @param decryptFile 加密后文件
     */
    public static final boolean encryptFile(final String key, final File encryptFile, final File decryptFile) {
        BouncyCastleUtil.addProvider();
        CipherOutputStream cipherOutputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try{
            if (!encryptFile.exists()) {
                LogUtil.error("待加密文件为空,请联系管理员!");
            }
            if (decryptFile.exists()) {
                decryptFile.delete();
            }
            decryptFile.createNewFile();
            Cipher cipher = initAESCipher(key, Cipher.ENCRYPT_MODE);
            cipherOutputStream = new CipherOutputStream(new FileOutputStream(decryptFile), cipher);
            bufferedInputStream = new BufferedInputStream(new FileInputStream(encryptFile));
            byte[] buffer = new byte[1024];
            int bufferLength;
            while ((bufferLength = bufferedInputStream.read(buffer)) != -1) {
                cipherOutputStream.write(buffer, 0, bufferLength);
            }
            return true;
        }catch(Throwable throwable){
            FileUtil.dirClear(decryptFile.getAbsolutePath());
            LogUtil.error("Aes3Util_encryptFile_t2", throwable);
            return false;
        }finally {
            IOUtil.close(bufferedInputStream, cipherOutputStream);
        }
    }

    /**
     * AES接密
     * @param key 密钥，16位ASCII字符串
     * @param encryptPath 加密后文件地址
     * @param decryptPath 解密后文件地址
     */
    public static final boolean decryptFile(final String key, final String encryptPath, final String decryptPath) {
        BouncyCastleUtil.addProvider();
        File encryptFile = null;
        File decryptFile = null;
        BufferedOutputStream outputStream = null;
        CipherInputStream inputStream = null;
        try{
            encryptFile = new File(encryptPath);
            if (!encryptFile.exists()) {
                LogUtil.error("待解密文件为空,请联系管理员!");
            }
            decryptFile = new File(decryptPath);
            if (decryptFile.exists()) {
                decryptFile.delete();
            }
            decryptFile.createNewFile();
            Cipher cipher = initAESCipher(key, Cipher.DECRYPT_MODE);
            outputStream = new BufferedOutputStream(new FileOutputStream(decryptFile));
            inputStream = new CipherInputStream(new FileInputStream(encryptFile), cipher);
            int bufferLength;
            byte[] buffer = new byte[1024];
            while ((bufferLength = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bufferLength);
            }
            return true;
        }catch(Throwable throwable){
            LogUtil.error("Aes3Util_decryptFile_t3", throwable);
            FileUtil.dirClear(decryptFile.getAbsolutePath());
            return false;
        }finally {
            IOUtil.close(inputStream, outputStream);
        }
    }

    /**
     * AES接密
     * @param key 密钥，16位ASCII字符串
     * @param encryptFile 加密后文件
     * @param decryptFile 解密后文件
     */
    public static final boolean decryptFile(final String key, final File encryptFile, final File decryptFile) {
        BouncyCastleUtil.addProvider();
        BufferedOutputStream outputStream = null;
        CipherInputStream inputStream = null;
        try{
            if (!encryptFile.exists()) {
                LogUtil.error("待解密文件为空,请联系管理员!");
            }
            if (decryptFile.exists()) {
                decryptFile.delete();
            }
            decryptFile.createNewFile();
            Cipher cipher = initAESCipher(key, Cipher.DECRYPT_MODE);
            outputStream = new BufferedOutputStream(new FileOutputStream(decryptFile));
            inputStream = new CipherInputStream(new FileInputStream(encryptFile), cipher);
            int bufferLength;
            byte[] buffer = new byte[1024];
            while ((bufferLength = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bufferLength);
            }
            return true;
        }catch(Throwable throwable){
            LogUtil.error("Aes3Util_decryptFile_t4", throwable);
            FileUtil.dirClear(decryptFile.getAbsolutePath());
            return false;
        }finally {
            IOUtil.close(inputStream, outputStream);
        }
    }

    public static final Cipher initAESCipher(final String password, final int cipherMode) {
        BouncyCastleUtil.addProvider();
        try{
            SecretKey key = p_getSecretKey(password);
            Cipher cipher = Cipher.getInstance(PKCS7PADDING);
            cipher.init(cipherMode, key);
            return cipher;
        }catch(Throwable throwable){
            LogUtil.error("Aes3Util_initAESCipher_t4", throwable);
            return null;
        }
    }

    private static final SecretKey p_getSecretKey(final String password) {
        BouncyCastleUtil.addProvider();
        int keyLength = 128;
        byte[] keyBytes = new byte[keyLength / 8];
        try{
            Arrays.fill(keyBytes, (byte) 0x0);
            byte[] passwordBytes = password.getBytes("UTF-8");
            int length = passwordBytes.length < keyBytes.length ? passwordBytes.length : keyBytes.length;
            System.arraycopy(passwordBytes, 0, keyBytes, 0, length);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
            return key;
        }catch(Throwable throwable){
            LogUtil.error("Aes3Util_p_getSecretKey_t4", throwable);
            return null;
        }
    }

}