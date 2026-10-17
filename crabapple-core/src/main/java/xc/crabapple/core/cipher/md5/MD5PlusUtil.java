package xc.crabapple.core.cipher.md5;

import xc.crabapple.core.collection.ListUtil;
import xc.crabapple.core.collection.MapUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.math.MathUtil;
import xc.crabapple.core.sort.SortUtil;
import xc.crabapple.core.thread.ThreadUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.File;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 2024-11-27
 * 获取文件内容的自定义摘要信息
 */
public class MD5PlusUtil {

    /**
     * 2025-01-01
     * @param fromUrl
     * @param toUrl
     * @return
     */
    public static final boolean compareFile(final String fromUrl, final String toUrl){
        final boolean flag = compareFile(fromUrl, toUrl, false);
        return flag;
    }

    /**
     * 2024-11-27
     * 2025-01-01 添加参数fast
     * 对比或比对两个指定文件的内容是否完全相同
     * @param fromUrl
     * @param toUrl
     * @param fast 是否只采用文件长度来判断二者完全相同，true-采用长度判断速度快但准确性稍差，false-采用长度+MD5同时判断速度慢准确地高
     * @return
     */
    public static final boolean compareFile(final String fromUrl, final String toUrl, final boolean fast){
        String effectiveFromUrl = UrlUtil.getStandardPath(fromUrl);
        File fromFile = new File(effectiveFromUrl);
        String effectiveToUrl = UrlUtil.getStandardPath(toUrl);
        File toFile = new File(effectiveToUrl);
        //
        if(!fromFile.exists() || !fromFile.isFile()){
            return false;
        }
        if(!toFile.exists() || !toFile.isFile()){
            return false;
        }
        //
        final long fromLength = fromFile.length();
        final long toLength = toFile.length();
        if(fromLength != toLength){
            return false;
        }else{
            if(fast){
                return true;
            }
        }
        //
        final String fromHash = getHash(effectiveFromUrl);
        final String toHash = getHash(effectiveToUrl);
        if(StringUtil.equals(fromHash, toHash)){
            return true;
        }
        return false;
    }

    /**
     * 2024-11-27
     * 获取文件内容的自定义摘要信息
     * @param url
     * @return
     */
    public static final String getHash(final String url){
        String effectiveUrl = UrlUtil.getStandardPath(url);
        final File file = new File(effectiveUrl);
        final String plusMd5 = getHash(file);
        return plusMd5;
    }

    /**
     * 2024-11-27
     * 获取文件内容的自定义摘要信息，可显著提升超大文件计算标准md5值的速度（大约10倍）
     * 对文件首先进行分块，通过多线程控制，每个子线程计算对应每块的md5值，然后按照块顺序对这些md5拼接为一个总字符串，然后计算总字符串的md5
     * @return
     */
    public static final String getHash(final File file){
        final long fileSize = file.length();
        final long BASE_SIZE = 1024 * 1024;  // 1MB
        if(fileSize <= BASE_SIZE * 3){
            final String md5 = MD5Util.getFileHash(file);
            final String plusMd5 = md5 + "#" + fileSize;
            return plusMd5;
        }
        if(true){
            // fileSize必须是BASE_SIZE的3倍及以上的大小
            final String signal = "crabapple_plusmd5_signal_demo";
            final String firstChunkMd5 = MD5Util.getChunkMd5(file, 0, BASE_SIZE);
//            final String centerChunkMd5 = MD5Util.getChunkMd5(file, (fileSize-BASE_SIZE)/2, BASE_SIZE);
            final String lastChunkMd5 = MD5Util.getChunkMd5(file, fileSize-BASE_SIZE, BASE_SIZE);
            final String allStr = signal + "@" + firstChunkMd5 + "@" + lastChunkMd5;
            final String md5 = MD5Util.getHash(allStr);
            final String plusMd5 = md5 + "#" + fileSize;
            return plusMd5;
        }
        //
        int chunkCount = p_chunkCount(fileSize, BASE_SIZE);
        //
        List<long[]> segMarkerList = FileUtil.getSegMarkerList(fileSize, chunkCount);
        int segMarkerSize = segMarkerList.size();
        final List<Map<String, Object>> syncList = ListUtil.getSyncList();
        // 不完整摘要
        if(true){
            long[] firstArray = segMarkerList.get(0);
            long[] middleArray = segMarkerList.get(segMarkerSize / 2);
            long[] lastArray = segMarkerList.get(segMarkerSize - 1);
            segMarkerList.clear();
            segMarkerList.add(firstArray);
            segMarkerList.add(middleArray);
            segMarkerList.add(lastArray);
            segMarkerSize = segMarkerList.size();
            chunkCount = segMarkerSize;
        }
        final String url = UrlUtil.getStandardPath(file);
        try{
            for(int i = 0; i < segMarkerSize; i++){
                final long[] array = segMarkerList.get(i);
                final long startIndex = array[0];
                final long chunkSize = array[1];
                new Thread(){
                    @Override
                    public void run(){
                        final String chunkMd5 = MD5Util.getChunkMd5(url, startIndex, chunkSize);
                        Map<String, Object> map = new HashMap<>(2);
                        map.put("startIndex", startIndex);
                        map.put("chunkMd5", chunkMd5);
                        syncList.add(map);
                    }
                }.start();
            }
        }catch (Throwable throwable){
            LogUtil.error("MD5PlusUtil_getHash_t1", "计算自定义文件摘要失败", url);
            return null;
        }
        while(true){
            int size = syncList.size();
            if(size >= chunkCount){
                break;
            }
            ThreadUtil.sleep(10L);
        }
        SortUtil.sortList(syncList, "startIndex", "asc", "yes");
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < syncList.size(); i++){
            Map<String, Object> map = syncList.get(i);
            final String chunkMd5 = MapUtil.get(map, "chunkMd5");
            stringBuilder.append(chunkMd5);
        }
        final String allMd5 = stringBuilder.toString();
        final String md5 = MD5Util.getHash(allMd5);
        final String plusMd5 = md5 + "#" + fileSize;
        return plusMd5;
    }

    /**
     * 2024-11-28
     * 根据文件大小进行分块
     * @param fileSize
     * @param BASE_SIZE
     * @return
     */
    private static final int p_chunkCount(final long fileSize, final long BASE_SIZE){
        Double ddd = MathUtil.divideReturnDouble(fileSize, BASE_SIZE, 0);
        int chunkCount = ddd.intValue();
        return chunkCount;
    }

    /**
     * 2024-11-27
     * 获取指定文件块的md5
     * @param url
     * @param startIndex
     * @param chunkSize
     * @return
     */
    public static final String getChunkMd5(final String url, final long startIndex, final long chunkSize){
        RandomAccessFile randomAccessFile = null;
        try{
            // 字节数组合并
            randomAccessFile = new RandomAccessFile(url, "r");
            randomAccessFile.seek(startIndex);
            long perSize = 16384 * 100;  // 每次读取的字节数
            perSize = Math.min(chunkSize, perSize);
            int perSizeInt = Integer.parseInt(perSize+"");
            byte[] bufferBytes = new byte[perSizeInt];
            long bytesSize;
            long countSize = 0;
            final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            while ((bytesSize = randomAccessFile.read(bufferBytes)) != -1){
                countSize += bytesSize;
                messageDigest.update(bufferBytes, 0, bufferBytes.length);
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
            String chunkMd5 = MD5Util.toHexString(digestBytes);
            chunkMd5 = chunkMd5.toLowerCase();
            return chunkMd5;
        } catch (Throwable throwable) {
            LogUtil.error("MD5PlusUtil_chunk_t1", throwable, url, startIndex, chunkSize);
            return "";
        }finally {
            IOUtil.close(randomAccessFile);
        }
    }

}