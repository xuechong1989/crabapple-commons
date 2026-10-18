package xc.crabapple.core.cipher.sm3;

import org.bouncycastle.crypto.digests.SM3Digest;
import xc.crabapple.core.cipher.bouncycastle.BouncyCastleUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HexFormat;

/**
 * 2025-10-21
 * 国密SM3算法工具类
 * 国密SM3安全性等同于SHA-256
 * 基于Bouncy Castle库实现，提供SM3哈希计算功能
 */
public final class Sm3Util {

    /**
     * 2025-10-21
     * 计算字符串的SM3哈希值
     * @param plaintext 输入字符串
     * @return 64字符的十六进制哈希字符串
     */
    public static final String hash(final String plaintext) {
        if(plaintext == null){
            LogUtil.error("Sm3Util_hash_1", "plaintext参数为空对象");
            return "";
        }
        byte[] plaintextBytes = plaintext.getBytes();
        String hashValue = hash(plaintextBytes);
        return hashValue;
    }

    /**
     * 2025-10-21
     * 计算字节数组的SM3哈希值
     * @param plaintextBytes 输入字节数组
     * @return 64字符的十六进制哈希字符串
     */
    public static final String hash(final byte[] plaintextBytes) {
        BouncyCastleUtil.addProvider();
        if(plaintextBytes == null || plaintextBytes.length == 0){
            LogUtil.error("Sm3Util_hash_5", "plaintext参数为空对象");
            return "";
        }
        try{
            SM3Digest sm3Digest = new SM3Digest();
            sm3Digest.update(plaintextBytes, 0, plaintextBytes.length);
            int digestSize = sm3Digest.getDigestSize();
            byte[] hash = new byte[digestSize];
            sm3Digest.doFinal(hash, 0);
            HexFormat hexFormat = HexFormat.of();
            String hashValue = hexFormat.formatHex(hash);
            hashValue = StringUtil.toLowerCase(hashValue);
            return hashValue;
        }catch (Throwable throwable){
            LogUtil.error("Sm3Util_hash_t3", throwable, "字节数组hash失败");
            return "";
        }
    }

    /**
     * 2025-10-21
     * 计算文件的SM3哈希值
     * @param url 文件路径
     * @return 64字符的十六进制哈希字符串
     */
    public static final String hashFile(final String url) {
        if (url == null) {
            LogUtil.error("Sm3Util_hashFile_1", "url为空对象", url);
            return "";
        }
        boolean success = FileUtil.isRealFile(url, false);
        if(!success){
            LogUtil.error("Sm3Util_hashFile_3", "文件不存在", url);
            return "";
        }
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(url);
            String hashValue = hashStream(inputStream);
            return hashValue;
        }catch (Throwable throwable){
            LogUtil.error("Sm3Util_hashFile_t1", throwable, "hash计算失败", url);
            return "";
        }finally {
            IOUtil.close(inputStream);
        }
    }

    /**
     * 2025-10-21
     * 计算输入流的SM3哈希值，支持超大文件哈希计算
     * @param inputStream 输入流
     * @return 64字符的十六进制哈希字符串
     */
    public static final String hashStream(final InputStream inputStream) {
        BouncyCastleUtil.addProvider();
        if (inputStream == null) {
            LogUtil.error("Sm3Util_hashStream_1", "inputStream为空对象");
            return "";
        }
        try{
            SM3Digest sm3Digest = new SM3Digest();
            // 固定大小的缓冲区，通常8KB是较好的平衡点
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                sm3Digest.update(buffer, 0, bytesRead);
            }
            int digestSize = sm3Digest.getDigestSize();
            byte[] hash = new byte[digestSize];
            sm3Digest.doFinal(hash, 0);
            HexFormat hexFormat = HexFormat.of();
            String hashValue = hexFormat.formatHex(hash);
            hashValue = StringUtil.toLowerCase(hashValue);
            return hashValue;
        }catch (Throwable throwable){
            LogUtil.error("Sm3Util_hashStream_t1", throwable, "hash计算失败");
            return "";
        }
    }

    /**
     * 2025-10-21
     * 验证字符串与哈希值是否匹配
     * @param plaintext 原始字符串
     * @param hashValue 要验证的哈希值
     * @return 如果匹配返回true，否则返回false
     */
    public static final boolean verify(final String plaintext, final String hashValue) {
        if (plaintext == null || "".equals(plaintext)) {
            return false;
        }
        if(hashValue == null || "".equals(hashValue)){
            return false;
        }
        if(hashValue.length() != 64){
            return false;
        }
        //
        String cipher = hash(plaintext);
        if(cipher == null || "".equals(cipher)){
            return false;
        }
        cipher = StringUtil.toLowerCase(cipher);
        if(!cipher.equals(hashValue)){
            return false;
        }
        return true;
    }

    /**
     * 2025-10-21
     * 验证字节数组与哈希值是否匹配
     * @param plaintextBytes 原始字节数组
     * @param hashValue 要验证的哈希值
     * @return 如果匹配返回true，否则返回false
     */
    public static final boolean verify(final byte[] plaintextBytes, final String hashValue) {
        if (plaintextBytes == null || plaintextBytes.length == 0) {
            return false;
        }
        if(hashValue == null || "".equals(hashValue)){
            return false;
        }
        if(hashValue.length() != 64){
            return false;
        }
        //
        String cipher = hash(plaintextBytes);
        if(cipher == null || "".equals(cipher)){
            return false;
        }
        cipher = StringUtil.toLowerCase(cipher);
        if(!cipher.equals(hashValue)){
            return false;
        }
        return true;
    }

}