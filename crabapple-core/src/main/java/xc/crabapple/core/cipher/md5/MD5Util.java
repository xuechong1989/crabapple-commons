package xc.crabapple.core.cipher.md5;

import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.Security;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 2017-03-17 1.0.0
 * 单向加密算法（消息摘要），单向不可逆
 */
public final class MD5Util{

    private static final char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * 2023-08-04
     * 对明文MD5指定次数之后与指定的md5相比较判断是否相等
     * @param plaintext
     * @param md5
     * @param times
     * @return
     */
    public static final boolean check(final String plaintext, final int times, final String md5){
        if(plaintext == null){
            return false;
        }
        //
        final String cipher = getHash(plaintext, times);
        if(cipher.equals(md5)){
            return true;
        }
        return false;
    }

    /**
     * 2023-08-04
     * 对明文MD5与指定的md5相比较判断是否相等
     * @param plaintext
     * @param md5
     * @return
     */
    public static final boolean check(final String plaintext, final String md5){
        if(plaintext == null){
            return false;
        }
        //
        final String cihper = getHash(plaintext);
        if (cihper.equals(md5)) {
            return true;
        }
        return false;
    }

    /**
     * 2023-08-04
     * 对明文反复加密md5指定次数，获取最终的md5值
     */
    public static final String getHash(final String plaintext, final int times){
        int effectiveTimes = times;
        if(effectiveTimes <= 0){
            effectiveTimes = 1;
        }
        String md5 = getHash(plaintext);
        effectiveTimes--;
        for(int i = 0; i < effectiveTimes; i++){
            md5 = getHash(md5);
        }
        return md5;
    }

    /**
     * 2017-03-17 1.0.0
     * 获取一个字符串的32位MD5值
     * digest()最后确定返回md5 hash值，返回值为8位字符串。因为md5 hash值是16位的hex值，实际上就是8位的字符
     * BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
     */
    public static final String getHash(final String plaintext) {
        if(plaintext == null){
            return "";
        }
        char charHexDigit[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        try{
            byte[] bytes = plaintext.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(bytes);
            byte[] bytes1 = md.digest();
            int j = bytes1.length;
            char[] cs = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte bbb = bytes1[i];
                cs[k++] = charHexDigit[bbb >>> 4 & 0xf];
                cs[k++] = charHexDigit[bbb & 0xf];
            }
            String md5 = chars2str(cs);
            md5 = md5.toLowerCase();
            return md5;
        }catch(Throwable throwable){
            LogUtil.error("MD5Util_getMD5_1", throwable, plaintext);
        }
        return null;
    }

    /**
     * 2017-03-17 1.0.0
     * @param bytes
     */
    public static final String getHash(final byte[] bytes){
        String str = bytes2str(bytes);
        if(str == null || "".equals(str)){
            return null;
        }
        String result = getHash(str);
        return result;
    }

    /**
     * 2017-03-17 1.0.0
     * 2024-11-27
     * 支持超大文件
     */
    public static final String getFileHash(final String url){
        if(url == null || "".equals(url)){
            return null;
        }
        String effectiveUrl = getStandardPath(url);
        final File file = new File(effectiveUrl);
        final String md5 = getFileHash(file);
        return md5;
    }

    /**
     * 2017-03-17 1.0.0
     * 2024-11-28 采用NIO机制API，效率较高，推荐使用
     * 支持超大文件
     */
    public static final String getFileHash(final File file){
        if(!file.exists()){
            String absolutePath = file.getAbsolutePath();
            String url = getStandardPath(absolutePath);
            LogUtil.error("MD5Util_getFileHash_1", "file_no_found", url);
            return null;
        }
        if(!file.isFile()){
            return null;
        }
        //
        final long fileSize = file.length();
        if(fileSize <= 0){
            final String md5 = getHash("");
            return md5;
        }
        //
        final String url = getStandardPath(file);
        int perSize = 1024 * 1024 * 10;  // 每次读取的字节数
        if(fileSize < perSize){
            perSize = Integer.parseInt(fileSize+"");
        }
        RandomAccessFile randomAccessFile = null;
        try{
            final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFile.seek(0);
            byte[] bufferBytes = new byte[perSize];
            while(true){
                final int readedSize = randomAccessFile.read(bufferBytes);
                if(readedSize == -1){
                    break;
                }
                if(readedSize == 0){
                    continue;
                }
                messageDigest.update(bufferBytes, 0, readedSize);
            }
            final byte[] digestBytes = messageDigest.digest();
            final String md5 = toHexString(digestBytes);
            return md5;
        }catch(Throwable throwable){
            LogUtil.error("MD5Util_getFileHash_t1", throwable, url);
            return null;
        }finally {
            IOUtil.close(randomAccessFile);
        }
    }

    /**
     * 2017-03-17 1.0.0
     * 2024-11-28 代码整理，一定程度提升速度
     * 基于经典文件输入流计算MD5值，效率较低，不推荐使用。
     * 支持超大文件
     * @param file
     * @return
     */
    public static final String getFileHash2(final File file){
        if(!file.exists()){
            String absolutePath = file.getAbsolutePath();
            String url = getStandardPath(absolutePath);
            LogUtil.error("MD5Util_getFileHash2_1", "file_no_found", url);
            return null;
        }
        if(!file.isFile()){
            return null;
        }
        //
        long fileSize = file.length();
        if(fileSize <= 0){
            final String md5 = getHash("");
            return md5;
        }
        //
        String url = getStandardPath(file);
        int perSize = 1024 * 1024 * 1;  // 每次读取的字节数
        if(fileSize < perSize){
            perSize = Integer.parseInt(fileSize+"");
        }
        FileInputStream fileInputStream = null;
        try{
            fileInputStream = new FileInputStream(file);
            byte[] bytes = new byte[perSize];
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            while(true){
                int readedSize = fileInputStream.read(bytes);
                if(readedSize == -1){
                    break;
                }
                if(readedSize == 0){
                    continue;
                }
                messageDigest.update(bytes, 0, readedSize);
            }
            final byte[] bytes2 = messageDigest.digest();
            final String md5 = toHexString(bytes2);
            return md5;
        }catch(Throwable throwable){
            LogUtil.error("MD5Util_getFileHash2_t1", throwable, url);
            return null;
        }finally {
            IOUtil.close(fileInputStream);
        }
    }

    /**
     * 2017-03-17 1.0.0
     * 基于经典文件输入流计算MD5值，效率较低，不推荐使用。
     * 支持超大文件
     * @param inputStream
     * @return
     */
    public static final String getFileHash2(final InputStream inputStream){
        final String md5 = getFileHash2(inputStream, true);
        return md5;
    }

    /**
     * 2017-03-17 1.0.0
     * 2024-11-27 代码整理
     * 基于经典文件输入流计算MD5值，效率较低，不推荐使用。
     * 支持超大文件
     */
    public static final String getFileHash2(final InputStream inputStream, final boolean close){
        int perSize = 1024 * 1024 * 1;  // 每次读取的字节数
        try{
            final byte[] bufferBytes = new byte[perSize];
            final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            int readedSize;
            while((readedSize = inputStream.read(bufferBytes)) != -1){
                if(readedSize == 0){
                    continue;
                }
                messageDigest.update(bufferBytes, 0, readedSize);
            }
            final byte[] digestBytes = messageDigest.digest();
            final String md5 = toHexString(digestBytes);
            return md5;
        }catch(Throwable throwable){
            LogUtil.error("MD5Util_getFileHash2_t2", throwable, close);
            return null;
        }finally {
            if(close){
                IOUtil.close(inputStream);
            }else{
                try{
                    inputStream.reset();
                }catch (Throwable throwable){
                    // ignore
                }
            }
        }
    }

    /**
     * 2024-11-29
     * @param url
     * @param startIndex
     * @param chunkSize
     * @return
     */
    public static final String getChunkMd5(final String url, final long startIndex, final long chunkSize){
        final File file = new File(url);
        String chunkMd5 = getChunkMd5(file, startIndex, chunkSize);
        return chunkMd5;
    }

    /**
     * 2024-11-27
     * 获取指定文件块的md5
     * @param file
     * @param startIndex
     * @param chunkSize
     * @return
     */
    public static final String getChunkMd5(final File file, final long startIndex, final long chunkSize){
        if(chunkSize <= 0){
            final String md5 = getHash("");
            return md5;
        }
        //
        final String url = UrlUtil.getStandardPath(file);
        RandomAccessFile randomAccessFile = null;
        try{
            randomAccessFile = new RandomAccessFile(url, "r");
            randomAccessFile.seek(startIndex);
            long perSize = 1024 * 1024 * 1;  // 每次读取的字节数
            perSize = Math.min(chunkSize, perSize);
            int perSizeInt = Integer.parseInt(perSize+"");
            byte[] bufferBytes = new byte[perSizeInt];
            long countSize = 0;
            final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            while (true){
                int readedSize = randomAccessFile.read(bufferBytes);
                if(readedSize == -1){
                    break;
                }
                if(readedSize == 0){
                    continue;
                }
                countSize += readedSize;
                messageDigest.update(bufferBytes, 0, readedSize);
                long shengyu = chunkSize - countSize;  // 剩余要读取的字节
                if(shengyu <= 0){
                    break;
                }
                if(shengyu < perSize){
                    perSizeInt = Integer.parseInt(shengyu+"");
                    bufferBytes = new byte[perSizeInt];
                }
            }
            final byte[] digestBytes = messageDigest.digest();
            final String chunkMd5 = toHexString(digestBytes);
            return chunkMd5;
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_chunk_t1", throwable, url, startIndex, chunkSize);
            return "";
        }finally {
            IOUtil.close(randomAccessFile);
        }
    }

    /**
     * 2017-03-17 1.0.0
     * @param digestBytes
     */
    public static final String toHexString(final byte[] digestBytes){
        StringBuilder stringBuilder = new StringBuilder(digestBytes.length * 2);
        for(int i = 0; i < digestBytes.length; i++){
            final byte bt = digestBytes[i];
            stringBuilder.append(hexChar[(bt & 0xf0) >>> 4]);
            stringBuilder.append(hexChar[bt & 0x0f]);
        }
        String md5 = stringBuilder.toString();
        md5 = md5.toLowerCase();
        return md5;
    }

    /**
     * 2017-03-17 1.0.0
     * 获取MessageDigest支持几种加密算法
     */
    private static final String[] getCryptolmpls(final String serviceType){
        Set result = new HashSet();
        Provider[] providers = Security.getProviders();
        for(int i = 0; i < providers.length; i++){
            Set keys = providers[i].keySet();
            for(Iterator it = keys.iterator(); it.hasNext(); ){
                String key = it.next().toString();
                key = key.split(" ")[0];
                if(key.startsWith(serviceType + ".")){
                    result.add(key.substring(serviceType.length() + 1));
                }else if(key.startsWith("Alg.Alias." + serviceType + ".")){
                    result.add(key.substring(serviceType.length() + 11));
                }
            }
        }
        int size = result.size();
        String[] arr = new String[size];
        String[] ok = (String[]) result.toArray(arr);
        return ok;
    }

    /**
     * 2019-04-15 1.0.3
     * 字节数组转换为字符串
     */
    private static final String bytes2str(final byte[] bytes){
        try{
            String str = new String(bytes, "UTF-8");
            return str;
        }catch(Throwable throwable){
            LogUtil.error("MD5Util_bytes2str_1", throwable);
        }
        return null;
    }

    /**
     * 2019-04-15 1.0.3
     * 字节数组转换为字符串
     */
    public static final String chars2str(final char[] cs){
        String str = new String(cs);
        try{
            byte[] bytes = str.getBytes("UTF-8");
            str = new String(bytes, "UTF-8");
            return str;
        }catch(Throwable throwable){
            LogUtil.error("MD5Util_chars2str_1", throwable);
            return null;
        }
    }

    /**
     * 2019-04-15 2.1.4
     * 字符串转换为utf-8
     * @param str
     */
    public static final String str2utf8(final String str){
        try{
            byte[] bytes = str.getBytes("UTF-8");
            String result = new String(bytes, "UTF-8");
            return result;
        }catch(Throwable throwable){
            LogUtil.error("MD5Util_str2utf8_1", throwable, str);
            return null;
        }
    }

    /**
     * 2016-11-20 1.0.0
     * @param file
     */
    private static final String getStandardPath(final File file){
        String path = file.getAbsolutePath();
        path = path.replace("\\", "/");
        return path;
    }

    /**
     * 2016-11-20 1.0.0
     * @param path
     */
    private static final String getStandardPath(final String path){
        if(path == null || "".equals(path)){
            return "";
        }
        String effectivePath = path.replace("\\", "/");
        if(isWindows()){
            String[] array = {"/C:", "/c:", "/D:", "/d:", "/E:", "/e:", "/F:", "/f:", "/G:", "/g:"};
            for(String disk : array){
                boolean flag = startsWith(effectivePath, disk);
                if(flag){
                    effectivePath = effectivePath.substring(1, effectivePath.length());
                    break;
                }
            }
        }else if(isLinux()){

        }
        boolean flag = effectivePath.endsWith("/");
        if(flag){
            effectivePath = effectivePath.substring(0, effectivePath.length() - 1);
        }
        return effectivePath;
    }

    /**
     * 2017-03-09 1.0.0
     * 当前运行的操作系统是否为windows
     */
    private static final boolean isWindows(){
        String name = getOSName();
        if(name.indexOf("windows") != -1){
            return true;
        }
        return false;
    }

    /**
     * 2017-03-09 1.0.0
     * 当前运行的操作系统是否为linux
     */
    private static final boolean isLinux(){
        String name = getOSName();
        if(name.indexOf("linux") != -1){
            return true;
        }
        return false;
    }

    /**
     * 2017-03-09 1.0.0
     * 例如：windows 7
     */
    private static final String getOSName(){
        String name = System.getProperty("os.name");
        name = name.toLowerCase();
        return name;
    }

    /**
     * 2018-02-13 1.1.0
     * 原字符串的开头部分是否为指定字符串
     */
    private static final Boolean startsWith(final String str, final String prefix){
        if(str == null || "".equals(str)){
            return false;
        }
        Boolean flag = str.startsWith(prefix);
        return flag;
    }

}
