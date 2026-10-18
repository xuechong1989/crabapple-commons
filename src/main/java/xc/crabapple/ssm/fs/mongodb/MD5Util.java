package xc.crabapple.ssm.fs.mongodb;

import xc.crabapple.core.io.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * xuechong 2026-06-02 MD5工具类
 */
public final class MD5Util {

    /**
     * xuechong 2026-06-02 计算文件MD5
     * @param file 文件对象
     * @return 32位小写MD5十六进制字符串
     */
    public static String getFileMd5(File file) throws Throwable {
        if (file == null) {
            throw new IllegalArgumentException("file参数不能为null");
        }
        MessageDigest messageDigest = getDigest();
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            byte[] buf = new byte[8192];
            int n;
            while ((n = inputStream.read(buf)) != -1) {
                messageDigest.update(buf, 0, n);
            }
        } finally {
            IOUtil.close(inputStream);
        }
        String md5 = getHex(messageDigest);
        return md5;
    }

    /**
     * xuechong 2026-06-02 创建MD5 MessageDigest实例，用于流式逐块计算
     * @return MessageDigest实例
     */
    public static MessageDigest getDigest() throws Throwable {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        return messageDigest;
    }

    /**
     * xuechong 2026-06-02 流式更新MD5计算，null安全
     * @param digest MessageDigest实例
     * @param bytes 数据字节数组
     * @param offset 起始偏移
     * @param len 数据长度
     */
    public static void update(MessageDigest digest, byte[] bytes, int offset, int len) {
        if (digest != null) {
            digest.update(bytes, offset, len);
        }
    }

    /**
     * xuechong 2026-06-02 完成MD5计算并返回十六进制字符串，null安全
     * @param digest MessageDigest实例
     * @return 32位小写MD5十六进制字符串，digest为null时返回null
     */
    public static String getHex(MessageDigest digest) {
        if (digest == null) {
            return null;
        }
        byte[] digestBytes = digest.digest();
        String md5 = bytesToHex(digestBytes);
        return md5;
    }

    /**
     * xuechong 2026-05-24 字节数组转十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString();
    }

}
