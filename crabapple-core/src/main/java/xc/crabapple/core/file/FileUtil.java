package xc.crabapple.core.file;

import xc.crabapple.core.callback.CrabappleCallback;
import xc.crabapple.core.cipher.md5.MD5PlusUtil;
import xc.crabapple.core.cipher.md5.MD5Util;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.MapUtil;
import xc.crabapple.core.collection.SetUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.device.EnvironmentUtil;
import xc.crabapple.core.device.OSInfo;
import xc.crabapple.core.encoder.FileEncoderUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.math.MathUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.sort.SortUtil;
import xc.crabapple.core.timer.TimerUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * 2022-08-13
 * 文件系统基础工具类
 */
public final class FileUtil {

    // windows系统下，以下字符串是系统关键字，不能以该字符串作为文件名，大小写均有限制，任何格式也同样有限制
    public static Map<String, Integer> windowsFilenameForbiddenMap;
    // linux系统下,不能以以下字符串作为文件名
    public static Set<String> linuxFilenameForbiddenSet;
    public static Set<String> windowsFilenameForbiddenSet;
    // 文件格式类型集合
    private static Map<String, Set<String>> p_fileTypeMap;

    /**
     * 2026-05-30
     * 获取Windows禁用文件名Map，若未初始化则自动初始化
     */
    public static Map<String, Integer> getWindowsFilenameForbiddenMap(){
        if(windowsFilenameForbiddenMap == null){
            windowsFilenameForbiddenMap = new HashMap<String, Integer>(32);
            windowsFilenameForbiddenMap.put("con", 1);
            windowsFilenameForbiddenMap.put("prn", 1);
            windowsFilenameForbiddenMap.put("aux", 1);
            windowsFilenameForbiddenMap.put("nul", 1);
            windowsFilenameForbiddenMap.put("com0", 1);
            windowsFilenameForbiddenMap.put("com1", 1);
            windowsFilenameForbiddenMap.put("com2", 1);
            windowsFilenameForbiddenMap.put("com3", 1);
            windowsFilenameForbiddenMap.put("com4", 1);
            windowsFilenameForbiddenMap.put("com5", 1);
            windowsFilenameForbiddenMap.put("com6", 1);
            windowsFilenameForbiddenMap.put("com7", 1);
            windowsFilenameForbiddenMap.put("com8", 1);
            windowsFilenameForbiddenMap.put("com9", 1);
            windowsFilenameForbiddenMap.put("lpt0", 1);
            windowsFilenameForbiddenMap.put("lpt1", 1);
            windowsFilenameForbiddenMap.put("lpt2", 1);
            windowsFilenameForbiddenMap.put("lpt3", 1);
            windowsFilenameForbiddenMap.put("lpt4", 1);
            windowsFilenameForbiddenMap.put("lpt5", 1);
            windowsFilenameForbiddenMap.put("lpt6", 1);
            windowsFilenameForbiddenMap.put("lpt7", 1);
            windowsFilenameForbiddenMap.put("lpt8", 1);
            windowsFilenameForbiddenMap.put("lpt9", 1);
        }
        return windowsFilenameForbiddenMap;
    }

    /**
     * 2024-11-27
     * 对比或比对两个指定文件的内容是否完全相同
     *
     * @param fromUrl
     * @param toUrl
     * @return
     */
    public static final boolean compareFile(final String fromUrl, final String toUrl) {
        final boolean flag = MD5PlusUtil.compareFile(fromUrl, toUrl, false);
        return flag;
    }

    /**
     * 2024-11-27
     * 对比或比对两个指定文件的内容是否完全相同
     *
     * @param fromUrl
     * @param toUrl
     * @param fast    是否只采用文件长度来判断二者完全相同，true-采用长度判断速度快但准确性稍差，false-采用长度+MD5同时判断速度慢准确地高
     * @return
     */
    public static final boolean compareFile(final String fromUrl, final String toUrl, final boolean fast) {
        final boolean flag = MD5PlusUtil.compareFile(fromUrl, toUrl, fast);
        return flag;
    }

    /**
     * 2024-11-27
     * 获取文件内容的自定义摘要信息
     *
     * @return
     */
    public static final String getPlusHash(final String url) {
        final String plusMd5 = MD5PlusUtil.getHash(url);
        return plusMd5;
    }

    /**
     * 2024-11-27
     * 获取指定文件的md5
     *
     * @param url
     * @return
     */
    public static final String getHash(final String url) {
        final String hash = MD5Util.getFileHash(url);
        return hash;
    }

    /**
     * 2024-11-27
     * 分块读取文件的分段标记
     *
     * @param fileSize
     * @param chunkCount 把文件分为几块
     * @return
     */
    public static final List<long[]> getSegMarkerList(final long fileSize, final int chunkCount) {
        final List<Long> averageList = MathUtil.average(fileSize, chunkCount);
        long startIndex = 0;
        List<long[]> list = new ArrayList<>();
        for (Long chunkSize : averageList) {
            long[] arr = new long[2];
            arr[0] = startIndex;
            arr[1] = chunkSize;
            if (chunkSize <= 0) {
                return list;
            }
            list.add(arr);
            //
            startIndex += chunkSize;
        }
        return list;
    }

    /**
     * 2024-11-27
     * 文件分块分割
     *
     * @param url
     * @param chunkCount 把文件分为几块
     * @return
     */
    public static final List<byte[]> chunks(final String url, final int chunkCount) {
        String localUrl = UrlUtil.getStandardPath(url);
        File file = new File(localUrl);
        if (!file.exists()) {
            return null;
        }
        final long fileSize = file.length();
        if (fileSize <= 0) {
            return null;
        }
        final List<long[]> segMarkerList = getSegMarkerList(fileSize, chunkCount);
        //
        List<byte[]> resultList = new ArrayList<>();
        for (int i = 0; i < segMarkerList.size(); i++) {
            long[] arr = segMarkerList.get(i);
            long startIndex2 = arr[0];
            long chunkSize2 = arr[1];
            byte[] bytes = getChunk(localUrl, startIndex2, chunkSize2);
            resultList.add(bytes);
        }
        //
        return resultList;
    }

    /**
     * 2024-11-27
     * 文件分块切割分割
     *
     * @param url
     * @param startIndex 开始的字节序列号，从0开始
     * @param chunkSize  每块的字节数
     * @return
     */
    public static final byte[] getChunk(final String url, final long startIndex, final long chunkSize) {
        ByteArrayOutputStream byteArrayOutputStream = null;
        RandomAccessFile randomAccessFile = null;
        try {
            // 字节数组合并
            byteArrayOutputStream = new ByteArrayOutputStream();
            randomAccessFile = new RandomAccessFile(url, "r");
            randomAccessFile.seek(startIndex);
            long perSize = 16384 * 100;  // 每次读取的字节数
            perSize = Math.min(chunkSize, perSize);
            int perSizeInt = Integer.parseInt(perSize + "");
            byte[] bufferBytes = new byte[perSizeInt];
            int bytesSize;
            int countSize = 0;
            while ((bytesSize = randomAccessFile.read(bufferBytes)) != -1) {
                countSize += bytesSize;
                byteArrayOutputStream.write(bufferBytes);
                long shengyu = chunkSize - countSize;  // 剩余要读取的字节
                if (shengyu <= 0) {
                    break;
                }
                if (shengyu < perSize) {
                    perSizeInt = Integer.parseInt(shengyu + "");
                    bufferBytes = new byte[perSizeInt];
                }
            }
            final byte[] bytes = byteArrayOutputStream.toByteArray();
            return bytes;
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_chunk_t1", throwable, url, startIndex, chunkSize);
            return null;
        } finally {
            IOUtil.close(randomAccessFile, byteArrayOutputStream);
        }
    }

    /**
     * 2024-11-10
     * 对指定的urlList集合排序，文件或目录路径排序，用法示例：
     * [d:/aaa/3.txt, d:/aaa/1.txt, d:/aaa/2.txt] ==> [d:/aaa/1.txt, d:/aaa/2.txt, d:/aaa/3.txt]
     *
     * @param fileList
     * @return
     */
    public static final List<String> sortUrlList(final List<String> fileList) {
        List<String> sortedList = sortUrlList(fileList, "asc");
        return sortedList;
    }

    /**
     * 2024-11-10
     * 对指定的urlList集合排序，文件或目录路径排序，用法示例：
     * [d:/aaa/3.txt, d:/aaa/1.txt, d:/aaa/2.txt] ==> [d:/aaa/1.txt, d:/aaa/2.txt, d:/aaa/3.txt]
     *
     * @param fileList
     * @param orderType asc-正序，desc-倒序
     * @return
     */
    public static final List<String> sortUrlList(final List<String> fileList, final String orderType) {
        final List<Map<String, Object>> sortList = new ArrayList<>();
        for (int i = 0; i < fileList.size(); i++) {
            String url = fileList.get(i);
            String fileName = getFileName(url, false);
            Long crabappleIndex = Long.valueOf(fileName);
            Map<String, Object> map = new HashMap<>();
            map.put("crabappleIndex", crabappleIndex);
            map.put("value", url);
            sortList.add(map);
        }
        //
        SortUtil.sortList(sortList, "crabappleIndex", orderType, "no");
        fileList.clear();
        for (int i = 0; i < sortList.size(); i++) {
            Map<String, Object> map = sortList.get(i);
            String url = MapUtil.get(map, "value");
            fileList.add(url);
        }
        sortList.clear();
        return fileList;
    }

    /**
     * 2024-04-24
     * 获取目录path1与path2中不同名的文件夹和目录（仅对比一级子目录）绝对路径
     *
     * @param path1
     * @param path2
     * @return
     */
    public static final Map<String, List<String>> comparePath(final String path1, final String path2) {
        String localPath1 = UrlUtil.getStandardPath(path1);
        String localPath2 = UrlUtil.getStandardPath(path2);
        Map<String, List<String>> resultMap = new HashMap<>();
        //
        Map<String, List<String>> map1 = getDirChildrenFirst(localPath1);
        Map<String, List<String>> map2 = getDirChildrenFirst(localPath2);
        //
        List<String> dirList1 = map1.get("dir");
        List<String> dirList2 = map2.get("dir");
        Map<String, Integer> dirMap = new HashMap<>();
        List<String> dirList = new ArrayList<>();
        for (int i = 0; i < dirList1.size(); i++) {
            String childPath1 = dirList1.get(i);
            dirMap.put(childPath1, 1);
        }
        for (int i = 0; i < dirList2.size(); i++) {
            String childPath2 = dirList2.get(i);
            String name2 = getFileName(childPath2, false);
            String key = path1 + "/" + name2;
            if (dirMap.containsKey(key)) {
                dirMap.remove(key);
                continue;
            }
            dirMap.put(childPath2, 1);
        }
        Set<String> dirSet = dirMap.keySet();
        Iterator<String> dirIterator = dirSet.iterator();
        while (dirIterator.hasNext()) {
            String dir = dirIterator.next();
            dirList.add(dir);
        }
        resultMap.put("dir", dirList);
//
        List<String> fileList1 = map1.get("file");
        List<String> fileList2 = map2.get("file");
        Map<String, Integer> fileMap = new HashMap<>();
        List<String> fileList = new ArrayList<>();
        for (int i = 0; i < fileList1.size(); i++) {
            String childPath1 = fileList1.get(i);
            fileMap.put(childPath1, 1);
        }
        for (int i = 0; i < fileList2.size(); i++) {
            String childPath2 = fileList2.get(i);
            String name2 = getFileName(childPath2, true);  // 包括文件后缀
            String key = path1 + "/" + name2;
            if (fileMap.containsKey(key)) {
                fileMap.remove(key);
                continue;
            }
            fileMap.put(childPath2, 1);
        }
        Set<String> fileSet = fileMap.keySet();
        Iterator<String> fileIterator = fileSet.iterator();
        while (fileIterator.hasNext()) {
            String file = fileIterator.next();
            fileList.add(file);
        }
        resultMap.put("file", fileList);
        //
        return resultMap;
    }

    /**
     * 2024-04-20
     *
     * @param url
     * @return
     */
    public static final OutputStream file2outputStream(final String url) {
        String localUrl = UrlUtil.getStandardPath(url);
        OutputStream outputStream = IOUtil.file2outputStream(localUrl);
        return outputStream;
    }

    /**
     * 2024-04-20
     *
     * @param file
     * @return
     */
    public static final OutputStream file2outputStream(final File file) {
        OutputStream outputStream = IOUtil.file2outputStream(file);
        return outputStream;
    }

    /**
     * 2023-07-19
     *
     * @param url
     * @return
     */
    public static final InputStream file2inputStream(final String url) {
        String localUrl = UrlUtil.getStandardPath(url);
        InputStream inputStream = IOUtil.file2inputStream(localUrl);
        return inputStream;
    }

    /**
     * 2024-04-20
     *
     * @param file
     * @return
     */
    public static final InputStream file2inputStream(final File file) {
        InputStream inputStream = IOUtil.file2inputStream(file);
        return inputStream;
    }

    /**
     * 2025-07-24
     * 判断指定的目录是否为空
     */
    public static final boolean isEmptyPath(final String targetPath) {
        final boolean success = isRealPath(targetPath);
        if (success) {
            File file = new File(targetPath);
            File[] files = file.listFiles();
            if (files != null && files.length == 0) {
                return true;
            }
            return false;
        }
        return false;
    }

    /**
     * 2022-08-13
     * 是否为真实存在的本地目录
     *
     * @param path
     * @return
     */
    public static final boolean isRealPath(final String path) {
        final boolean real = isRealPath(path, false);
        return real;
    }

    /**
     * 2022-08-13
     * 2025-07-25 添加参数checkEmpty
     * 是否为真实存在的本地目录
     *
     * @param path
     * @return
     */
    public static final boolean isRealPath(final String path, final boolean checkEmpty) {
        String localPath = UrlUtil.getStandardPath(path);
        if (StringUtil.isBlank(localPath)) {
            return false;
        }
        File file = new File(localPath);
        if (file == null) {  // 权限不足或无访问权
            return false;
        }
        if (!file.exists()) {
            return false;
        }
        if (!file.isDirectory()) {
            return false;
        }
        if (checkEmpty) {
            File[] files = file.listFiles();
            if (files != null && files.length == 0) {
                return false;
            }
            return true;  // 目录不为空，才算真正的目录
        }
        return true;  // 无论目录是否为空，都算真正的目录
    }

    /**
     * 2022-08-13
     * 是否为真实存在的本地文件
     *
     * @param url
     * @return
     */
    public static final boolean isRealFile(final String url) {
        final boolean success = isRealFile(url, false);
        return success;
    }

    /**
     * 2022-08-13
     * 2025-07-07 添加checkEmpty参数
     * 是否为真实存在的本地文件
     *
     * @param url
     * @param checkEmpty 是否检测文件大小
     * @return
     */
    public static final boolean isRealFile(final String url, final boolean checkEmpty) {
        String localUrl = UrlUtil.getStandardPath(url);
        if (StringUtil.isBlank(localUrl)) {
            return false;
        }
        File file = new File(localUrl);
        if (file == null) {
            return false;
        }
        if (!file.exists()) {
            return false;
        }
        if (!file.isFile()) {
            return false;
        }
        if (checkEmpty) {
            long fileSize = file.length();
            if (fileSize <= 0) {
                return false;
            }
        }
        //
        return true;
    }

    /**
     * 2022-12-22
     * 将file转成byte[]
     */
    public static final byte[] file2bytes(final String url) {
        String localUrl = UrlUtil.getStandardPath(url);
        if(!exists(localUrl)){
            LogUtil.error("FileUtil_file2bytes_1", "文件不存在", localUrl);
            return null;
        }
        byte[] bytes = IOUtil.file2bytes(localUrl);
        return bytes;
    }

    /**
     * 2022-12-22
     * 将file转成byte[]
     */
    public static final byte[] file2bytes(final File file) {
        byte[] bytes = IOUtil.file2bytes(file);
        return bytes;
    }

    /**
     * 2023-01-25
     * 将FileInputStream转成byte[]
     */
    public static final byte[] file2bytes(final FileInputStream fileInputStream, final boolean isClose) {
        byte[] bytes = IOUtil.file2bytes(fileInputStream, isClose);
        return bytes;
    }

    /**
     * 2024-11-21
     * 源字节数组生成本地文件，支持任何文件格式。
     *
     * @param bytes
     * @param targetUrl
     */
    public static final String bytes2file(final byte[] bytes, final String targetUrl) {
        String result = IOUtil.bytes2file(bytes, targetUrl);
        return result;
    }

    /**
     * 2023-01-25
     * 将file转成可网络传输的字节数组
     */
    public static final String file2bytesStr(final String url) {
        String bytesStr = IOUtil.file2bytesStr(url);
        return bytesStr;
    }

    /**
     * 2023-01-25
     * 将file转成可网络传输的字节数组
     */
    public static final String file2bytesStr(final File file) {
        String bytesStr = IOUtil.file2bytesStr(file);
        return bytesStr;
    }

    /**
     * 2023-01-25
     * 将FileInputStream转成可网络传输的字节数组
     */
    public static final String file2bytesStr(final FileInputStream fileInputStream, final boolean isClose) {
        String bytesStr = IOUtil.file2bytesStr(fileInputStream, isClose);
        return bytesStr;
    }

    /**
     * 2025-05-28
     * 获取指定本地文件内容的base64编码字符串
     *
     * @param file
     * @return
     */
    public static final String file2base64Str(final File file) {
        final String base64Str = IOUtil.file2base64Str(file);
        return base64Str;
    }

    /**
     * 2024-11-21
     * 获取指定本地文件内容的base64编码字符串
     *
     * @param url
     * @return
     */
    public static final String file2base64Str(final String url) {
        final String base64Str = IOUtil.file2base64Str(url);
        return base64Str;
    }

    /**
     * 2024-11-21
     * base64编码字符串生成指定本地文件
     *
     * @param targetUrl
     * @return
     */
    public static final String base64Str2file(final String base64Str, final String targetUrl) {
        final String result = IOUtil.base64Str2file(base64Str, targetUrl);
        return result;
    }

    /**
     * 2018-03-25 1.1.0
     * InputStream文件流对象转换为File对象
     *
     * @param inputStream 文件流
     * @return File
     */
    public static final File inputstream2file(final InputStream inputStream) {
        File file = IOUtil.inputstream2file(inputStream);
        return file;
    }

    /**
     * 2019-11-29 3.1.0
     * InputStream文件流对象转换为File对象
     */
    public static final File inputstream2file(final InputStream inputStream, final String url) {
        File file = IOUtil.inputstream2file(inputStream, url);
        return file;
    }

    /**
     * 2023-06-08
     * 把指定的文件输入流写入磁盘，即把输入流对象转为本地文件
     */
    public static final void inputstream2disk(final InputStream inputStream, final String url) {
        IOUtil.inputstream2disk(inputStream, url);
    }

    /**
     * 2023-06-08
     * 把指定的文件输入流写入磁盘，即把输入流对象转为本地文件
     */
    public static final void inputstream2disk(final InputStream inputStream, final String url, final boolean isClose) {
        IOUtil.inputstream2disk(inputStream, url, isClose);
    }

    /**
     * 2025-02-18
     *
     * @param inputStream
     * @return
     */
    public static final String inputstream2text(final InputStream inputStream) {
        final String text = IOUtil.inputstream2text(inputStream);
        return text;
    }

    /**
     * 2025-02-18
     *
     * @param inputStream
     * @param closeIn
     * @return
     */
    public static final String inputstream2text(final InputStream inputStream, final boolean closeIn) {
        final String text = IOUtil.inputstream2text(inputStream, closeIn);
        return text;
    }

    /**
     * 2025-02-18
     *
     * @param inputStream
     * @return
     */
    public static final List<String> inputstream2list(final InputStream inputStream) {
        final List<String> list = IOUtil.inputstream2list(inputStream);
        return list;
    }

    /**
     * 2025-02-18
     *
     * @param inputStream
     * @param closeIn
     * @return
     */
    public static final List<String> inputstream2list(final InputStream inputStream, final boolean closeIn) {
        final List<String> list = IOUtil.inputstream2list(inputStream, closeIn);
        return list;
    }

    /**
     * 2021-08-15 1.0.0
     * 文件分类
     */
    public static final String getCategoryByFileName(final String name) {
        String type = getFileType(name);
        String category = getCategoryByFileType(type);
        return category;
    }

    /**
     * 2021-08-15 1.0.0
     * 文件分类，如mp3 => audio
     */
    public static final String getCategoryByFileType(final String fileType) {
        if (StringUtil.isBlank(fileType)) {
            return "other";
        }
        //
        p_fileTypeMapInit();
        //
        String localFileType = StringUtil.toLowerCase(fileType);
        Set<String> keySet = p_fileTypeMap.keySet();
        Iterator<String> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Set<String> valueSet = p_fileTypeMap.get(key);
            Iterator<String> valueIterator = valueSet.iterator();
            while (valueIterator.hasNext()) {
                String ftype = valueIterator.next();
                if (ftype.equals(localFileType)) {
                    return key;
                }
            }
        }
        //
        return "other";
    }

    /**
     * 2024-12-04
     * 获取文本格式类型集合
     *
     * @return
     */
    public static final Set<String> getTextSet() {
        p_fileTypeMapInit();
        //
        final Set<String> textSet = p_fileTypeMap.get("text");
        return textSet;
    }

    /**
     * 2020-08-06 6.4.0
     * 2024-12-04 代码重构
     * 指定文件名或路径是否为文本格式
     *
     * @param nameOrUrl 文件名（可带地址），都必须含有后缀名，如hello.txt或d:/app/hello.txt
     */
    public static final boolean isText(final String nameOrUrl) {
        p_fileTypeMapInit();
        //
        final Set<String> textSet = p_fileTypeMap.get("text");
        String fileType = getFileType(nameOrUrl);
        fileType = StringUtil.toLowerCase(fileType);
        if (textSet.contains(fileType)) {
            return true;
        }
        return false;
    }

    /**
     * 2020-08-19 6.5.0
     * 2024-12-04 代码重构
     * 指定文件格式是否为文本格式
     *
     * @param fileType 文件类型（不带点的后缀），如txt
     */
    public static final boolean isTextByFileType(final String fileType) {
        String localFileType = StringUtil.toLowerCase(fileType);
        localFileType = StringUtil.deleteAll(localFileType, ".");
        final Set<String> textSet = p_fileTypeMap.get("text");
        if (textSet.contains(localFileType)) {
            return true;
        }
        return false;
    }

    /**
     * 2024-12-04
     * 获取office格式类型集合
     *
     * @return
     */
    public static final Set<String> getOfficeSet() {
        p_fileTypeMapInit();
        //
        final Set<String> officeSet = p_fileTypeMap.get("office");
        return officeSet;
    }

    /**
     * 2020-08-06 6.4.0
     * 2024-12-04 代码重构
     * 指定文件名或路径是否为office格式
     *
     * @return
     */
    public static final boolean isOffice(final String nameOrUrl) {
        p_fileTypeMapInit();
        //
        Set<String> officeSet = p_fileTypeMap.get("office");
        String fileType = getFileType(nameOrUrl);
        fileType = StringUtil.toLowerCase(fileType);
        if (officeSet.contains(fileType)) {
            return true;
        }
        return false;
    }

    /**
     * 2020-08-19 6.5.0
     * 2024-12-04 代码重构
     * 指定文件格式是否为office格式
     *
     * @param fileType 文件类型（不带点的后缀），如docx
     */
    public static final boolean isOfficeByFileType(final String fileType) {
        String localFileType = StringUtil.toLowerCase(fileType);
        localFileType = StringUtil.deleteAll(localFileType, ".");
        final Set<String> officeSet = p_fileTypeMap.get("office");
        if (officeSet.contains(localFileType)) {
            return true;
        }
        return false;
    }

    /**
     * 2024-12-04
     * 获取pdf格式类型集合
     *
     * @return
     */
    public static final Set<String> getPdfSet() {
        p_fileTypeMapInit();
        //
        final Set<String> pdfSet = p_fileTypeMap.get("pdf");
        return pdfSet;
    }

    /**
     * 2020-08-06 6.4.0
     * 2024-12-04 代码重构
     * 指定文件名或路径是否为文档格式
     *
     * @return
     */
    public static final boolean isPdf(final String nameOrUrl) {
        p_fileTypeMapInit();
        //
        Set<String> pdfSet = p_fileTypeMap.get("pdf");
        String fileType = getFileType(nameOrUrl);
        fileType = StringUtil.toLowerCase(fileType);
        if (pdfSet.contains(fileType)) {
            return true;
        }
        return false;
    }

    /**
     * 2020-08-19 6.5.0
     * 2024-12-04 代码重构
     * 指定文件格式是否为文档格式
     *
     * @param fileType 文件类型（不带点的后缀），如pdf
     */
    public static final boolean isPdfByFileType(final String fileType) {
        String localFileType = StringUtil.toLowerCase(fileType);
        localFileType = StringUtil.deleteAll(localFileType, ".");
        final Set<String> pdfSet = p_fileTypeMap.get("pdf");
        if (pdfSet.contains(localFileType)) {
            return true;
        }
        return false;
    }

    /**
     * 2024-12-04
     * 获取图片格式类型集合
     *
     * @return
     */
    public static final Set<String> getImageSet() {
        p_fileTypeMapInit();
        //
        final Set<String> imgSet = p_fileTypeMap.get("img");
        return imgSet;
    }

    /**
     * 2020-08-06 6.4.0
     * 2024-12-04
     * 指定文件名或路径是否为图片格式
     *
     * @return
     */
    public static final boolean isImage(final String nameOrUrl) {
        p_fileTypeMapInit();
        //
        Set<String> imgSet = p_fileTypeMap.get("img");
        String fileType = getFileType(nameOrUrl);
        fileType = StringUtil.toLowerCase(fileType);
        if (imgSet.contains(fileType)) {
            return true;
        }
        return false;
    }

    /**
     * 2020-08-06 6.4.0
     * 2024-12-04
     * 指定文件格式是否为图片格式
     *
     * @param fileType 文件类型（不带点的后缀），如jpg
     */
    public static final boolean isImageByFileType(final String fileType) {
        String localFileType = StringUtil.toLowerCase(fileType);
        localFileType = StringUtil.deleteAll(localFileType, ".");
        final Set<String> imgSet = p_fileTypeMap.get("img");
        if (imgSet.contains(localFileType)) {
            return true;
        }
        return false;
    }

    /**
     * 2024-12-04
     * 获取音频格式类型集合
     *
     * @return
     */
    public static final Set<String> getAudioSet() {
        p_fileTypeMapInit();
        //
        final Set<String> audioSet = p_fileTypeMap.get("audio");
        return audioSet;
    }

    /**
     * 2024-12-04
     * 指定文件名或路径是否为音频格式
     *
     * @return
     */
    public static final boolean isAudio(final String nameOrUrl) {
        p_fileTypeMapInit();
        //
        Set<String> audioSet = p_fileTypeMap.get("audio");
        String fileType = getFileType(nameOrUrl);
        fileType = StringUtil.toLowerCase(fileType);
        if (audioSet.contains(fileType)) {
            return true;
        }
        return false;
    }

    /**
     * 2024-12-04
     * 指定文件格式是否为音频格式
     *
     * @param fileType 文件类型（不带点的后缀），如mp3
     */
    public static final boolean isAudioByFileType(final String fileType) {
        String localFileType = StringUtil.toLowerCase(fileType);
        localFileType = StringUtil.deleteAll(localFileType, ".");
        final Set<String> audioSet = p_fileTypeMap.get("audio");
        if (audioSet.contains(localFileType)) {
            return true;
        }
        return false;
    }

    /**
     * 2024-12-04
     * 获取视频格式类型集合
     *
     * @return
     */
    public static final Set<String> getVideoSet() {
        p_fileTypeMapInit();
        //
        final Set<String> videoSet = p_fileTypeMap.get("video");
        return videoSet;
    }

    /**
     * 2024-12-04
     * 指定文件名或路径是否为视频格式
     *
     * @return
     */
    public static final boolean isVideo(final String nameOrUrl) {
        p_fileTypeMapInit();
        //
        Set<String> videoSet = p_fileTypeMap.get("video");
        String fileType = getFileType(nameOrUrl);
        fileType = StringUtil.toLowerCase(fileType);
        if (videoSet.contains(fileType)) {
            return true;
        }
        return false;
    }

    /**
     * 2024-12-04
     * 指定文件格式是否为视频格式
     *
     * @param fileType 文件类型（不带点的后缀），如mp4
     */
    public static final boolean isVideoByFileType(final String fileType) {
        String localFileType = StringUtil.toLowerCase(fileType);
        localFileType = StringUtil.deleteAll(localFileType, ".");
        final Set<String> videoSet = p_fileTypeMap.get("video");
        if (videoSet.contains(localFileType)) {
            return true;
        }
        return false;
    }

    /**
     * 2024-12-04
     * 文件格式类型集合初始化
     */
    private static final synchronized void p_fileTypeMapInit() {
        if (p_fileTypeMap != null) {
            return;
        }
        //
        p_fileTypeMap = new HashMap<>();
        //
        Set<String> textSet = SetUtil.add("txt", "tex", "md", "htm", "html", "css", "js",
                "java", "jsp", "vue", "json", "xml",
                "properties", "yml", "yaml", "conf", "cnf", "cfg", "ini", "rtf", "csv", "log");
        p_fileTypeMap.put("text", textSet);
        //
        Set<String> officeSet = SetUtil.add("doc", "docx", "xls", "xlsx", "ppt", "pptx");
        p_fileTypeMap.put("office", officeSet);
        //
        Set<String> pdfSet = SetUtil.add("pdf", "ofd");
        p_fileTypeMap.put("pdf", pdfSet);
        //
        Set<String> imgSet = SetUtil.add("jpg", "jpeg", "gif", "png", "bmp", "ico", "tiff", "tif", "svg", "webp",
                "eps", "ai", "heic", "heif", "raw", "psd", "dds");
        p_fileTypeMap.put("img", imgSet);
        //
        Set<String> audioSet = SetUtil.add("mp3", "mp2", "aac", "ogg", "opus", "wav", "wma", "amr", "m4a",
                "flac", "alac", "pcm", "aiff", "dts", "ac3", "ape", "midi", "ra", "cda", "vqf");
        p_fileTypeMap.put("audio", audioSet);
        //
        Set<String> videoSet = SetUtil.add("mp4", "mpg", "mpeg", "avi", "av1", "mov", "wmv", "vob", "rmvb", "rm",
                "asf", "asx", "3gp", "3g2", "webm", "mkv", "flv", "ts", "vp9", "ogv", "xvid", "divx");
        p_fileTypeMap.put("video", videoSet);
    }

    /**
     * 2017-08-23 1.0.0
     * 若为目录则返回为空字符串
     * 跟据文件地址获取文件类型（后缀），全小写字符，注意不带点，如 doc docx
     * @param url 保证右侧是个文件名即可，若无后缀名则返回空字符串。如：d:/tmp/aa.txt 或 aa.txt
     */
    public static final String getFileType(final String url) {
        String localUrl = UrlUtil.getStandardPath(url);
        if (StringUtil.isBlank(localUrl)) {
            return "";
        }
        int dot = localUrl.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        int n = localUrl.length();
        if (dot >= n - 1) {
            return "";
        }
        String fileType = localUrl.substring(dot + 1);
        fileType = StringUtil.toLowerCase(fileType);
        fileType = StringUtil.getStandardStr(fileType);
        return fileType;
    }

    /**
     * 2017-09-18 1.0.0
     * 若为目录则返回为空字符串
     * 跟据文件地址获取文件类型（后缀），如doc,docx
     *
     * @param file 保证右侧是个文件名即可
     */
    public static final String getFileType(final File file) {
        final String url = UrlUtil.getStandardPath(file);
        final String fileType = getFileType(url);
        return fileType;
    }

    /**
     * 2018-10-27 1.1.0
     * 根据文件名或绝对路径返回文件名，如 /clouddb/test.zip ==> test
     *
     * @param file 文件对象
     */
    public static final String getFileName(final File file) {
        final String fileName = file.getName();
        final String name = getFileName(fileName, true);
        return name;
    }

    /**
     * 2018-10-27 1.1.0
     * 根据文件名或绝对路径返回文件名，如 /clouddb/test.zip ==> test
     *
     * @param file   文件对象
     * @param suffix 是否带有后缀名，true表示带(默认)，false表示不带
     */
    public static final String getFileName(final File file, final boolean suffix) {
        String fileName = file.getName();
        String name = getFileName(fileName, suffix);
        return name;
    }

    /**
     * 2018-07-30 1.1.0
     * 根据文件名或绝对路径返回文件名，如 /clouddb/test.zip ==> test
     *
     * @param nameOrUrl 文件文件名或绝对路径
     */
    public static final String getFileName(final String nameOrUrl) {
        String name = getFileName(nameOrUrl, true);
        return name;
    }

    /**
     * 2018-07-30 1.1.0
     * 根据文件名或绝对路径返回文件名，如 /clouddb/test.zip ==> test
     *
     * @param urlOrName 文件文件名或绝对路径 (可以是目录，文件路径或一个普通字符串)
     * @param suffix    是否带有后缀名，true表示带(默认)，false表示不带
     */
    public static final String getFileName(final String urlOrName, final boolean suffix) {
        if (StringUtil.isBlank(urlOrName)) {
            return "";
        }
        String localUrlOrName = urlOrName;
        if (!StringUtil.containOf(localUrlOrName, "/")) {
            if (!StringUtil.containOf(localUrlOrName, "\\")) {
                if (suffix) {
                    return localUrlOrName;
                }
                if (localUrlOrName.indexOf(".") != -1) {
                    int index = localUrlOrName.lastIndexOf(".");
                    localUrlOrName = localUrlOrName.substring(0, index);
                }
                return localUrlOrName;
            }
        }
        String name = "";
        localUrlOrName = UrlUtil.getStandardPath(localUrlOrName);
        if (localUrlOrName.indexOf("/") != -1) {
            String[] arr = StringUtil.split(localUrlOrName, "/");
            int size = arr.length;
            name = arr[size - 1];
        } else {
            name = localUrlOrName;
        }
        if (suffix) {
            return name;
        }
        if (name.indexOf(".") != -1) {
            int index = name.lastIndexOf(".");
            name = name.substring(0, index);
        }
        return name;
    }

    /**
     * 2018-07-30 1.1.0
     * 获取文件大小，单位字节
     */
    public static final long getFileSize(final String url) {
        String localUrl = UrlUtil.getStandardPath(url);
        File file = new File(localUrl);
        long size = getFileSize(file);
        return size;
    }

    /**
     * 2018-07-30 1.1.0
     * 获取文件大小，单位字节
     */
    public static final long getFileSize(final File file) {
        if (file == null) {
            return 0;
        }
        if (!file.exists()) {
            return 0;
        }
        if (!file.isFile()) {
            return 0;
        }
        long size = file.length();
        return size;
    }

    /**
     * 2020-08-06 6.4.0
     * 获取文件大小，单位字节
     */
    public static final long getFileSize2(final String url) {
        String localUrl = UrlUtil.getStandardPath(url);
        File file = new File(localUrl);
        long size = getFileSize2(file);
        return size;
    }

    /**
     * 2020-08-06 6.4.0
     * 获取文件大小，单位字节
     */
    public static final long getFileSize2(final File file) {
        if (file == null) {
            return 0;
        }
        if (!file.exists()) {
            return 0;
        }
        if (!file.isFile()) {
            return 0;
        }
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            byte[] testByte = new byte[100];
            int readCount = inputStream.read(testByte);
            if (readCount <= 0) {
                return 0;
            }
            return readCount;
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_getFileSize2_t1", throwable);
            return 0;
        } finally {
            IOUtil.close(inputStream);
        }
    }

    /**
     * 2017-05-16 1.0.0
     * 文件存在而且大小不为0，说明该文件有效
     */
    public static final boolean valid(final String url) {
        String localUrl = UrlUtil.getStandardPath(url);
        File file = new File(localUrl);
        boolean flag = valid(file);
        return flag;
    }

    /**
     * 2017-05-16 1.0.0
     * 文件存在而且大小不为0，说明该文件有效
     */
    public static final boolean valid(final File file) {
        long size = getFileSize(file);
        if (size <= 0) {
            return false;
        }
        return true;
    }

    /**
     * 2019-11-29 3.1.0
     */
    public static final String getFileUrl(final File file) {
        if (file == null) {
            return "";
        }
        String url = file.getAbsolutePath();
        url = UrlUtil.getStandardPath(url);
        return url;
    }

    /**
     * 2018-08-08 1.1.0
     * 获取指定文件的创建时间，13位标准时间戳格式
     *
     * @param pathOrUrl 目录或文件绝对路径
     */
    public static final long getFileCreateTime(final String pathOrUrl) {
        String localPathOrUrl = UrlUtil.getStandardPath(pathOrUrl);
        boolean flag = exists(localPathOrUrl);
        if (!flag) {
            return 0;
        }
        Path path = Paths.get(localPathOrUrl);
        BasicFileAttributeView basicFileAttributeView = Files.getFileAttributeView(path, BasicFileAttributeView.class);
        BasicFileAttributes basicFileAttributes = null;
        try {
            basicFileAttributes = basicFileAttributeView.readAttributes();
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_getFileCreateTime_1", throwable, localPathOrUrl);
            return 0;
        }
        FileTime fileTime = basicFileAttributes.creationTime();
        long createTime = fileTime.toMillis();
        return createTime;
    }

    /**
     * 2023-09-23
     * 获取文件夹的名称
     * /aa/bb/cc ==> cc
     */
    public static final String getPathName(final String path) {
        String localPath = UrlUtil.getStandardPath(path);
        localPath = StringUtil.trim(localPath);
        //
        if (!StringUtil.containOf(localPath, "/")) {
            return localPath;
        }
        if ("/".equals(localPath)) {
            return "";
        }
        String[] array = StringUtil.split(localPath, "/");
        String pathName = array[array.length - 1];
        return pathName;
    }

    /**
     * 2018-08-08 1.1.0
     * 获取指定文件的最后修改时间，13位标准时间戳格式
     *
     * @param pathOrUrl 目录或文件绝对路径
     */
    public static final long getFileLastModifiedTime(final String pathOrUrl) {
        String localPathOrUrl = UrlUtil.getStandardPath(pathOrUrl);
        boolean flag = exists(localPathOrUrl);
        if (!flag) {
            return 0;
        }
        Path path = Paths.get(localPathOrUrl);
        BasicFileAttributeView basicFileAttributeView = Files.getFileAttributeView(path, BasicFileAttributeView.class);
        BasicFileAttributes basicFileAttributes = null;
        try {
            basicFileAttributes = basicFileAttributeView.readAttributes();
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_getFileLastModifiedTime_1", throwable, localPathOrUrl);
            return 0;
        }
        FileTime fileTime = basicFileAttributes.lastModifiedTime();
        long lastModifiedTime = fileTime.toMillis();
        return lastModifiedTime;
    }

    /**
     * 2017-05-16 1.0.0
     * 把指定的文件或目录复制到指定目录下
     * 文件 ==> 文件
     * 文件 ==> 目录
     * 目录 ==> 目录
     *
     * @param fromUrl
     * @param intoUrl
     */
    public static final boolean copy(final String fromUrl, final String intoUrl) {
        String localFromUrl = UrlUtil.getStandardPath(fromUrl);
        String localIntoUrl = UrlUtil.getStandardPath(intoUrl);
        File fromFile = new File(localFromUrl);
        File intoFile = new File(localIntoUrl);
        boolean flag = copy(fromFile, intoFile, true);
        return flag;
    }

    /**
     * 2024-04-30
     * 把指定的文件或目录复制到指定目录下
     * 文件 ==> 文件
     * 文件 ==> 目录
     * 目录 ==> 目录
     *
     * @param fromUrl
     * @param intoUrl
     */
    public static final boolean copy(final String fromUrl, final String intoUrl, final boolean forceOverwrite) {
        String localFromUrl = UrlUtil.getStandardPath(fromUrl);
        String localIntoUrl = UrlUtil.getStandardPath(intoUrl);
        File fromFile = new File(localFromUrl);
        File intoFile = new File(localIntoUrl);
        boolean flag = copy(fromFile, intoFile, forceOverwrite);
        return flag;
    }

    /**
     * 2024-04-30
     * 文件 ==> 文件
     * 文件 ==> 目录
     * 目录 ==> 目录
     *
     * @param fromFile
     * @param intoFile
     * @return
     */
    public static final boolean copy(final File fromFile, final File intoFile) {
        boolean flag = copy(fromFile, intoFile, true);
        return flag;
    }

    /**
     * 2017-05-16 1.0.0
     * 2024-04-30 功能重构
     * 把指定的文件或目录复制到指定目录下
     * 文件 ==> 文件
     * 文件 ==> 目录
     * 目录 ==> 目录
     *
     * @param fromFile
     * @param toFile
     * @param forceOverwrite 是否强制覆盖，true-删除原toFile，false-不删除，会直接返回false。[文件 ==> 文件]有效
     */
    public static final boolean copy(final File fromFile, final File toFile, final boolean forceOverwrite) {
        if (!fromFile.exists()) {
            String fromUrl = fromFile.getAbsolutePath();
            fromUrl = UrlUtil.getStandardPath(fromUrl);
            LogUtil.error("FileUtil_copy_1", "复制失败，fromFile不存在", fromUrl, forceOverwrite);
            return false;
        }
        //
        final boolean fromFlag = fromFile.isDirectory();
        File localToFile = toFile;
        boolean toFlag = localToFile.isDirectory();
        if (fromFlag) {
            final String toFilename = getFileName(localToFile);
            if (StringUtil.containOf(toFilename, ".")) {
                String path = localToFile.getAbsolutePath();
                final String parentPath = getPathByUrl(path);
                dirMake(parentPath);
            } else {
                if (!localToFile.exists()) {
                    dirMake(localToFile);
                    String path = localToFile.getAbsolutePath();
                    localToFile = new File(path);
                }
            }
        } else {
            if (localToFile.exists()) {
                delete(localToFile);
            }
            toFlag = false;
        }
        // 目录 ==> 目录
        if (fromFlag && toFlag) {
            String intoPath = UrlUtil.getStandardPath(localToFile);
            String fromName = getFileName(fromFile);
            String targetPath = intoPath + "/" + fromName;
            File targetFile = new File(targetPath);
            boolean flag = p_dir2dir(fromFile, targetFile, forceOverwrite);
            return flag;
        }
        // 文件 ==> 文件
        if (!fromFlag && !toFlag) {
            boolean flag = p_file2file(fromFile, localToFile, forceOverwrite);
            return flag;
        }
        // 文件 ==> 目录
        if (!fromFlag && toFlag) {
            String fromUrl = fromFile.getAbsolutePath();
            fromUrl = UrlUtil.getStandardPath(fromUrl);
            String fromName = UrlUtil.getFileName(fromUrl, true);
            String intoPath = localToFile.getAbsolutePath();
            intoPath = UrlUtil.getStandardPath(intoPath);
            String intoUrl = intoPath + "/" + fromName;
            boolean flag = p_file2file(fromUrl, intoUrl, forceOverwrite);
            return flag;
        }
        //
        LogUtil.error("FileUtil_copy_3", "复制失败，目录不能复制到非目录", fromFile, localToFile, forceOverwrite);
        return false;
    }

    /**
     * 2024-04-11
     * 本地磁盘文件或目录剪切迁移
     *
     * @param fromUrl
     * @param toUrl
     * @return
     */
    public static final boolean mv(final String fromUrl, final String toUrl) {
        String localFromUrl = UrlUtil.getStandardPath(fromUrl);
        String localToUrl = UrlUtil.getStandardPath(toUrl);
        File fromFile = new File(localFromUrl);
        File toFile = new File(localToUrl);
        boolean flag = mv(fromFile, toFile);
        return flag;
    }

    /**
     * 2024-04-11
     * 本地磁盘文件或目录剪切迁移
     *
     * @param fromFile
     * @param toFile
     * @return
     */
    public static final boolean mv(final File fromFile, final File toFile) {
        boolean flag = copy(fromFile, toFile);
        if (flag) {
            delete(fromFile);
            return true;
        }
        return false;
    }

    /**
     * 2022-01-06
     * 文件重命名
     *
     * @param fromUrl
     * @param toUrl
     * @return
     */
    public static final boolean rename(final String fromUrl, final String toUrl) {
        String localFromUrl = UrlUtil.getStandardPath(fromUrl);
        String localToUrl = UrlUtil.getStandardPath(toUrl);
        File fromFile = new File(localFromUrl);
        File toFile = new File(localToUrl);
        boolean flag = rename(fromFile, toFile);
        return flag;
    }

    /**
     * 2022-01-06
     * 文件重命名
     *
     * @param fromFile
     * @param toFile
     * @return
     */
    public static final boolean rename(final File fromFile, final File toFile) {
        boolean isExists = toFile.exists();  // 确保新的文件名不存在
        if (isExists) {
            return false;
        }
        boolean flag = fromFile.renameTo(toFile);
        if (flag) {
            return true;
        }
        return false;
    }

    /**
     * 2016-11-20 1.0.0
     * e:/aa/bb/hello.txt ==> e:/aa/bb
     *
     * @param url 全绝对路径
     */
    public static final String getPathByUrl(final String url) {
        String path = UrlUtil.getPathByUrl(url);
        return path;
    }

    /**
     * 2022-07-12
     *
     * @param url
     */
    public static final boolean dirMakeByUrl(final String url) {
        String targetPath = getPathByUrl(url);
        boolean flag = dirMake(targetPath);
        return flag;
    }

    /**
     * 2017-05-16 1.0.0
     * 2024-10-21 修改为boolean类型返回值
     * 创建一个指定的目录，若上层多级父级都没有就全部创建出来
     *
     * @param path 必须是一个目录
     */
    public static final boolean dirMake(final String path) {
        String localPath = UrlUtil.getStandardPath(path);
        File file = new File(localPath);
        boolean flag = dirMake(file);
        return flag;
    }

    /**
     * 2017-05-16 1.0.0
     * 2023-05-26 方法mkdir改为mkdirs
     *
     * @param file
     */
    public static final boolean dirMake(final File file) {
        if (file.exists() && file.isDirectory()) {
            return true;
        }
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            String absolutePath = parentFile.getAbsolutePath();
            dirMake(absolutePath);
        }
        boolean flag = file.mkdirs();
        return flag;
    }


    /**
     * 2017-05-16 1.0.0
     * 创建空文件夹
     * 文件夹总数量：n = [(num ^ deepMax - 1) / (num - 1)] * num
     *
     * @param dir     目录
     * @param num     单个目录下多少个子目录
     * @param deepMax 目录最大层级
     * @param deep    当前层级，默认值为1
     */
    public static final void dirMakeAll(final File dir, final int num, final int deepMax, final int deep) {
        for (int i = 0; i < num; i++) {
            String dname = RandomUtil.getStr(5);
            String dirNew = dir + "/" + dname;
            dirMake(dirNew);
        }
        if (deep < deepMax) {
            File[] files = dir.listFiles();
            for (File file : files) {
                dirMakeAll(file, num, deepMax, deep + 1);
            }
        }
    }

    /**
     * 2017-05-16 1.0.0
     * 递归删除目录下的所有文件及子目录下所有文件
     */
    public static final boolean dirDelete(final String path) {
        String localPath = UrlUtil.getStandardPath(path);
        File file = new File(localPath);
        boolean flag = dirDelete(file);
        return flag;  //目录此时为空，可以删除
    }

    /**
     * 2017-05-16 1.0.0
     *
     * @param file
     */
    public static final boolean dirDelete(final File file) {
        if (file == null) {
            return true;
        }
        if (!file.exists()) {
            return true;
        }
        if (file.isDirectory()) {
            File[] fileChildren = file.listFiles();
            for (int i = 0; i < fileChildren.length; i++) {
                File fileChild = fileChildren[i];
                String dirChild = fileChild.getAbsolutePath();
                boolean success = dirDelete(dirChild);
//                if(!success){
//                    return false;
//                }
            }
        }
        boolean flag = file.delete();
        if (!flag) {
            String path = file.getAbsolutePath();
            LogUtil.warn("FileUtil_dirDelete_1", "删除文件或目录失败", path);
            return false;
        }
        return flag;
    }

    /**
     * 2017-05-16 1.0.0
     *
     * @param path
     */
    public static final boolean dirClear(final String path) {
        String localPath = UrlUtil.getStandardPath(path);
        File file = new File(localPath);
        boolean flag = dirClear(file);
        return flag;
    }

    /**
     * 2017-05-16 1.0.0
     *
     * @param file
     */
    public static final boolean dirClear(final File file) {
        dirDelete(file);
        boolean flag = dirMake(file);  //无论成功或失败，都要保证此目录要存在
        return flag;
    }

    /**
     * 2017-05-16 1.0.0
     * 2024-04-30 重命名，新增forceOverwrite参数
     * 会把fromFile目录下的所有子孙文件（递归）放到toFile目录下
     *
     * @param fromPath
     * @param toPath
     * @param forceOverwrite
     */
    private static final boolean p_dir2dir(final String fromPath, final String toPath, final boolean forceOverwrite) {
        String localFromPath = UrlUtil.getStandardPath(fromPath);
        String localToPath = UrlUtil.getStandardPath(toPath);
        File fromFile = new File(localFromPath);
        File toFile = new File(localToPath);
        boolean flag = p_dir2dir(fromFile, toFile, forceOverwrite);
        return flag;
    }

    /**
     * 2017-05-16 1.0.0
     * 2024-04-30 重命名，新增forceOverwrite参数
     * 会把fromFile目录下的所有子孙文件（递归）放到toFile目录下
     *
     * @param fromFile
     * @param toFile
     * @param forceOverwrite
     */
    private static final boolean p_dir2dir(final File fromFile, final File toFile, final boolean forceOverwrite) {
        if (fromFile == null || toFile == null) {
            LogUtil.warn("FileUtil_p_dir2dir_1", "目录复制失败", fromFile, toFile);
            return false;
        }
        if (!fromFile.exists()) {
            LogUtil.warn("FileUtil_p_dir2dir_2", "目录复制失败，fromFile不存在", fromFile);
            return false;
        }
        if (!fromFile.isDirectory()) {
            LogUtil.warn("FileUtil_p_dir2dir_3", "目录复制失败，fromFile不是目录", fromFile);
            return false;
        }
        //
        if (forceOverwrite) {
            if (toFile.exists()) {
                boolean flag = dirClear(toFile);
                if (!flag) {
                    LogUtil.warn("FileUtil_p_dir2dir_4", "目录复制失败，toFile清空失败", fromFile);
                    return false;
                }
            } else {
                boolean flag = dirMake(toFile);
                if (!flag) {
                    LogUtil.warn("FileUtil_p_dir2dir_14", "目录复制失败，toFile创建失败", fromFile);
                    return false;
                }
            }
        }
        //
        String fromUrl = UrlUtil.getStandardPath(fromFile);
        String toUrl = UrlUtil.getStandardPath(toFile);
        Map<String, List<String>> map = p_getDirChildren(fromUrl, null, null);
        List<String> listDir = map.get("dir");
        for (String fromUrl_d : listDir) {
            String relative = StringUtil.deleteAll(fromUrl_d, fromUrl);
            String toUrl_d = toUrl + relative;
            dirMake(toUrl_d);
        }
        List<String> listFile = map.get("file");
        for (String fromUrl_f : listFile) {
            String relative = StringUtil.deleteAll(fromUrl_f, fromUrl);
            String toUrl_f = toUrl + relative;
            boolean flag = p_file2file(fromUrl_f, toUrl_f, true);
            if (!flag) {
                return false;
            }
        }
        return true;
    }

    /**
     * 2018-10-29 1.1.0
     * 根据文件路径读取byte[] 数组
     */
    public static final byte[] fileReadWithBytes(final String url) {
        String localUrl = UrlUtil.getStandardPath(url);
        File file = new File(localUrl);
        if (!file.exists()) {
            LogUtil.error("FileUtil_fileReadWithBytes_1", "file_no_found", localUrl);
            return null;
        }
        int length = (int) file.length();
        ByteArrayOutputStream byteArrayOutputStream = null;
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            byteArrayOutputStream = new ByteArrayOutputStream(length);
            short bufSize = 1024;
            byte[] buffer = new byte[bufSize];
            int len1;
            while (-1 != (len1 = bufferedInputStream.read(buffer, 0, bufSize))) {
                byteArrayOutputStream.write(buffer, 0, len1);
            }
            byte[] bytes = byteArrayOutputStream.toByteArray();
            return bytes;
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_fileReadWithBytes_1", throwable, url);
            return null;
        } finally {
            IOUtil.close(byteArrayOutputStream, bufferedInputStream, fileInputStream);
        }
    }

    /**
     * 2017-05-16 1.0.0
     * 文件复制到文件
     */
    private static final boolean p_file2file(final String fromUrl, final String toUrl, final boolean forceOverwrite) {
        String localFromUrl = UrlUtil.getStandardPath(fromUrl);
        String localToUrl = UrlUtil.getStandardPath(toUrl);
        File fromFile = new File(localFromUrl);
        File toFile = new File(localToUrl);
        boolean flag = p_file2file(fromFile, toFile, forceOverwrite);
        return flag;
    }

    /**
     * 2017-05-16 1.0.0
     * 2024-10-30 突破FileChannel类2GB限制，支持传输超大文件
     * 文件复制到文件
     */
    private static final boolean p_file2file(final File fromFile, final File toFile, final boolean forceOverwrite) {
        if (fromFile == null) {
            LogUtil.error("FileUtil_p_file2file_1", "文件复制失败，fromFile为空");
            return false;
        }
        if (!fromFile.exists()) {
            String fromUrl = fromFile.getAbsolutePath();
            LogUtil.error("FileUtil_p_file2file_2", "文件复制失败，fromFile不存在", fromUrl);
            return false;
        }
        if (!fromFile.isFile()) {
            String fromUrl = fromFile.getAbsolutePath();
            LogUtil.error("FileUtil_p_file2file_3", "文件复制失败，fromFile不是文件", fromUrl);
            return false;
        }
        if (toFile == null) {
            String toUrl = fromFile.getAbsolutePath();
            LogUtil.error("FileUtil_p_file2file_4", "文件复制失败，toFile为空", toUrl);
            return false;
        }
        if (forceOverwrite) {
            if (toFile.exists()) {
                boolean flag = toFile.delete();
                if (!flag) {
                    LogUtil.error("FileUtil_p_file2file_5", "文件复制失败，toFile删除失败", toFile, forceOverwrite);
                    return false;
                }
            }
        } else {
            if (toFile.exists()) {
                LogUtil.error("FileUtil_p_file2file_6", "文件复制失败，toFile已存在", toFile, forceOverwrite);
                return false;
            }
        }
        //
        final long thresholdFileSize = 1024 * 1024 * 1024 * 2L;  // FileChannel规定每次最大只能传输2GB大小
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        FileChannel inFileChannel = null;
        FileChannel outFileChannel = null;
        String fromUrl = fromFile.getAbsolutePath();
        String toUrl = toFile.getAbsolutePath();
        try {
            boolean flag = fileCreate(toUrl);
            if (!flag) {
                LogUtil.error("FileUtil_p_file2file_7", "文件复制失败，toUrl创建失败", toUrl);
                return false;
            }
            //
            fileInputStream = new FileInputStream(fromFile);
            fileOutputStream = new FileOutputStream(toFile);
            inFileChannel = fileInputStream.getChannel();
            outFileChannel = fileOutputStream.getChannel();
            final long inSize = inFileChannel.size();
            long position = 0L;
            while (position < inSize) {
                long byteLength = inFileChannel.transferTo(position, thresholdFileSize, outFileChannel);
                position += byteLength;
            }
            return true;
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_p_file2file_t5", throwable, "文件复制失败", fromUrl, toUrl);
            return false;
        } finally {
            IOUtil.close(outFileChannel, inFileChannel, fileOutputStream, fileInputStream);
        }
    }

    /**
     * 2017-05-16 1.0.0
     *
     * @param url 必须为文件
     */
    public static final boolean fileCreate(final String url) {
        if (StringUtil.isBlank(url)) {
            return false;
        }
        String localUrl = UrlUtil.getStandardPath(url);
        File file = new File(localUrl);
        if (file.exists()) {
            return true;  // windows、linux系统中相同目录下文件和目录均不能相互重名
        }
        boolean flag = fileNameIsLegal(localUrl);
        if (!flag) {
            return false;
        }
        String path = UrlUtil.getPathByLevel(localUrl, 1);
        dirMake(path);
        try {
            file.createNewFile();
            return true;
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_fileCreate_t1", throwable, url);
            return false;
        }
    }

    /**
     * 2024-04-11
     * 本地磁盘文件或目录删除
     *
     * @param url
     * @return
     */
    public static final boolean delete(final String url) {
        boolean flag = fileDelete(url);
        return flag;
    }

    /**
     * 2024-04-11
     * 本地磁盘文件或目录删除
     *
     * @param file
     * @return
     */
    public static final boolean delete(final File file) {
        boolean flag = fileDelete(file);
        return flag;
    }

    /**
     * 2017-05-16 1.0.0
     * 本地磁盘文件或目录删除
     * delete方法无效的原因：1.文件流没有关闭；2.手工gc
     */
    public static final boolean fileDelete(final String url) {
        String localUrl = UrlUtil.getStandardPath(url);
        File file = new File(localUrl);
        boolean flag = fileDelete(file);
        return flag;
    }

    /**
     * 2017-05-16 1.0.0
     * 2024-04-11 集成目录删除
     * delete方法无效的原因：1.文件流没有关闭；2.手工gc
     */
    public static final boolean fileDelete(final File file) {
        if (file == null) {
            return true;
        }
        if (!file.exists()) {
            return true;
        }
        if (file.isDirectory()) {
            boolean flag = dirDelete(file);
            return flag;
        }
        //
        boolean flag = file.delete();
        if (!flag) {
            String url = file.getAbsolutePath();
            LogUtil.error("FileUtil_fileDelete_1", "删除文件失败", url);
            return false;
        }
        return flag;
    }

    /**
     * 2017-05-16 1.0.0
     *
     * @param url
     */
    public static final boolean exists(final String url) {
        File file = new File(url);
        boolean flag = exists(file);
        return flag;
    }

    /**
     * 2024-04-29
     * 指定的多个文件或目录是否存在，有一个存在则返回true
     *
     * @param urlArray
     */
    public static final boolean existsOfOne(final String... urlArray) {
        if (urlArray == null || urlArray.length == 0) {
            return false;
        }
        //
        for (int i = 0; i < urlArray.length; i++) {
            String url = urlArray[i];
            File file = new File(url);
            boolean flag = file.exists();
            if (flag) {
                return true;
            }
        }
        //
        return false;
    }

    /**
     * 2024-04-29
     * 指定的多个文件或目录是否全部存在，有一个不存在则返回false
     *
     * @param urlArray
     */
    public static final boolean existsOfAll(final String... urlArray) {
        if (urlArray == null || urlArray.length == 0) {
            return false;
        }
        //
        for (int i = 0; i < urlArray.length; i++) {
            String url = urlArray[i];
            File file = new File(url);
            boolean flag = file.exists();
            if (!flag) {
                return false;
            }
        }
        //
        return true;
    }

    /**
     * 2017-05-16 1.0.0
     *
     * @param file
     */
    public static final boolean exists(final File file) {
        if (file.exists()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 2024-04-29
     * 指定的多个文件或目录是否存在，有一个存在则返回true
     *
     * @param fileArray
     * @return
     */
    public static final boolean existsOfOne(final File... fileArray) {
        if (fileArray == null || fileArray.length == 0) {
            return false;
        }
        //
        for (File file : fileArray) {
            if (file.exists()) {
                return true;
            }
        }
        //
        return false;
    }

    /**
     * 2024-04-29
     * 指定的多个文件或目录是否全部存在，有一个不存在则返回false
     *
     * @param fileArray
     * @return
     */
    public static final boolean existsOfAll(final File... fileArray) {
        if (fileArray == null || fileArray.length == 0) {
            return false;
        }
        //
        for (File file : fileArray) {
            if (!file.exists()) {
                return false;
            }
        }
        //
        return true;
    }

    /**
     * 2018-11-20 1.1.0
     * 递归获取指定目录下所有子孙目录和子孙文件
     * 调用方法：
     * Map<String, List<String>> map = getDirChildren("e:/a");
     * ArrayList<String> listDir = (ArrayList) map.get("dir");
     * ArrayList<String> listFile = (ArrayList) map.get("file");
     *
     * @param path 指定的目录路径
     */
    public static final Map<String, List<String>> getDirChildren(final String path) {
        Map<String, List<String>> map = p_getDirChildren(path, null, null);
        return map;
    }

    /**
     * 2024-08-15
     *
     * @param path        指定目录
     * @param restrictSet 限定的文件格式后缀，不包含点.
     * @return
     */
    public static final Map<String, List<String>> getDirChildren(final String path, final Set<String> restrictSet) {
        Map<String, List<String>> map = p_getDirChildren(path, null, restrictSet);
        return map;
    }

    /**
     * 2025-09-02
     * 递归获取所有目录
     * @param path
     * @return
     */
    public static final List<String> getDirChildrenDir(final String path) {
        Map<String, List<String>> map = p_getDirChildren(path, null, null);
        if (map == null) {
            return null;
        }
        List<String> list = map.get("dir");
        return list;
    }

    /**
     * 2025-09-02
     * 递归获取所有文件
     * @param path
     * @return
     */
    public static final List<String> getDirChildrenFile(final String path) {
        Map<String, List<String>> map = p_getDirChildren(path, null, null);
        if (map == null) {
            return null;
        }
        List<String> list = map.get("file");
        return list;
    }

    /**
     * 2017-05-16 1.0.0
     * 递归获取指定目录下所有子孙目录和子孙文件
     * 调用方法：
     * Map<String, List<String>> map = getDirChildren("e:/a", null);
     * List<String> list = (ArrayList)map.get("file");
     *
     * @param path
     * @param resultMap
     * @param restrictSet 限定的文件格式后缀，不包含点.
     */
    private static final Map<String, List<String>> p_getDirChildren(final String path, final Map<String, List<String>> resultMap, final Set<String> restrictSet) {
        Map<String, List<String>> localResultMap = resultMap;
        if (localResultMap == null) {
            localResultMap = new HashMap<>(2);
            localResultMap.put("dir", new ArrayList<>());
            localResultMap.put("file", new ArrayList<>());
        }
        String localPath = UrlUtil.getStandardPath(path);
        File file = new File(localPath);
        if (file.isDirectory()) {
            File[] childList = file.listFiles();
            if (childList != null) {
                for (int i = 0; i < childList.length; i++) {
                    final File childFile = childList[i];
                    final String originalName = childFile.getName();
                    String childPathOrUrl = childFile.getAbsolutePath();
                    childPathOrUrl = UrlUtil.getStandardPath(childPathOrUrl);
                    final String newName = UrlUtil.getFileName(childPathOrUrl);
                    if(!originalName.equals(newName)){
                        LogUtil.error("FileUtil_p_getDirChildren_1", "文件名有误，系统已退出", originalName, newName, childFile);
                        System.exit(1);
                        return localResultMap;
                    }
                    p_getDirChildren(childPathOrUrl, localResultMap, restrictSet);
                }
            }
            localResultMap.get("dir").add(localPath);
        } else if (file.isFile()) {
            if (restrictSet == null) {
                localResultMap.get("file").add(localPath);
            } else {
                String type = getFileType(localPath);
                type = StringUtil.toLowerCase(type);
                if (restrictSet.contains(type)) {
                    localResultMap.get("file").add(localPath);
                }
            }
        }
        return localResultMap;
    }

    /**
     * 2020-08-19 6.5.0
     * 获取指定目录下所有子目录和子文件（仅一级目录，非递归到孙及以下）
     * 调用方法：
     * Map<String, List<String>> map = getDirChildrenFirst("e:/a");
     * List<String> list = (ArrayList)map.get("file");
     *
     * @param path
     */
    public static final Map<String, List<String>> getDirChildrenFirst(final String path) {
        Map<String, List<String>> map = getDirChildrenFirst(path, null);
        return map;
    }

    /**
     * 2020-08-19 6.5.0
     * 获取指定目录下所有子目录和子文件（仅一级目录，非递归到孙及以下）
     * 调用方法：
     * Map<String, List<String>> map = getDirChildrenFirst("e:/a", null);
     * List<String> list = (ArrayList)map.get("file");
     *
     * @param path
     * @param map
     */
    public static final Map<String, List<String>> getDirChildrenFirst(final String path, final Map<String, List<String>> map) {
        Map<String, List<String>> localMap = map;
        if (localMap == null) {
            localMap = new HashMap<>();
            localMap.put("dir", new ArrayList<>());
            localMap.put("file", new ArrayList<>());
        }
        String localPath = UrlUtil.getStandardPath(path);
        File file = new File(localPath);
        if (file.isDirectory()) {
            File[] childList = file.listFiles();
            if (childList != null) {
                for (int i = 0; i < childList.length; i++) {
                    File childFile = childList[i];
                    if (!childFile.exists()) {
                        continue;
                    }
                    String childPath = childFile.getAbsolutePath();
                    childPath = UrlUtil.getStandardPath(childPath);
                    if (childFile.isDirectory()) {
                        localMap.get("dir").add(childPath);
                        continue;
                    }
                    if (childFile.isFile()) {
                        localMap.get("file").add(childPath);
                    }
                }
            }
        } else if (file.isFile()) {
            localMap.get("file").add(localPath);
        }
        return localMap;
    }

    /**
     * 2023-07-19
     * 获取指定目录中的指定格式的文件的绝对路径集合
     *
     * @param path
     * @param targetTypeArray
     * @return
     */
    public static final List<String> getDirChildrenFirstFiles(final String path, final String... targetTypeArray) {
        if (StringUtil.isBlank(path)) {
            return null;
        }
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        if (!file.isDirectory()) {
            return null;
        }
        if (CollectionUtil.isBlank(targetTypeArray)) {
            return null;
        }
        final Map<String, Integer> targetTypeMap = new HashMap<>(targetTypeArray.length);
        for (int i = 0; i < targetTypeArray.length; i++) {
            String targetType = targetTypeArray[i];
            targetType = StringUtil.deleteAll(targetType, " ");
            if (StringUtil.isBlank(targetType)) {
                continue;
            }
            targetType = StringUtil.toLowerCase(targetType);
            targetTypeMap.put(targetType, 1);
        }
        //
        List<String> list = new ArrayList<>();
        File[] childList = file.listFiles();
        for (int i = 0; i < childList.length; i++) {
            File childFile = childList[i];
            if (!childFile.exists()) {
                continue;
            }
            if (!childFile.isFile()) {
                continue;
            }
            String type = getFileType(childFile);
            type = StringUtil.toLowerCase(type);
            final Boolean flag = targetTypeMap.containsKey(type);
            if (!flag) {
                continue;
            }
            String targetUrl = childFile.getAbsolutePath();
            targetUrl = UrlUtil.getStandardPath(targetUrl);
            list.add(targetUrl);
        }
        return list;
    }

    /**
     * 2023-07-19
     * 获取指定目录中的指定格式的文件的绝对路径集合
     *
     * @param path
     * @param targetTypeArray
     * @return
     */
    public static final List<String> getDirChildrenFiles(final String path, final String... targetTypeArray) {
        if (StringUtil.isBlank(path)) {
            return null;
        }
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        if (!file.isDirectory()) {
            return null;
        }
        if (CollectionUtil.isBlank(targetTypeArray)) {
            return null;
        }
        final Map<String, Integer> targetTypeMap = new HashMap<>(targetTypeArray.length);
        for (int i = 0; i < targetTypeArray.length; i++) {
            String targetType = targetTypeArray[i];
            targetType = StringUtil.deleteAll(targetType, " ");
            if (StringUtil.isBlank(targetType)) {
                continue;
            }
            targetType = StringUtil.toLowerCase(targetType);
            targetTypeMap.put(targetType, 1);
        }
        //
        List<String> list = new ArrayList<>();
        Map<String, List<String>> map = p_getDirChildren(path, null, null);
        List<String> fileList = map.get("file");
        for (int i = 0; i < fileList.size(); i++) {
            String url = fileList.get(i);
            File childFile = new File(url);
            if (!childFile.exists()) {
                continue;
            }
            if (!childFile.isFile()) {
                continue;
            }
            String type = getFileType(childFile);
            type = StringUtil.toLowerCase(type);
            final Boolean flag = targetTypeMap.containsKey(type);
            if (!flag) {
                continue;
            }
            String targetUrl = childFile.getAbsolutePath();
            targetUrl = UrlUtil.getStandardPath(targetUrl);
            list.add(targetUrl);
        }
        return list;
    }

    public static final boolean writeByteArray(final String url, final OutputStream outputStream) {
        File file = new File(url);
        if (!file.exists()) {
            return false;
        }
        if (!file.isFile()) {
            return false;
        }
        boolean flag = writeByteArray(file, outputStream);
        return flag;
    }

    /**
     * 2017-05-16 1.0.0
     */
    public static final boolean writeByteArray(final File file, final OutputStream outputStream) {
        if (file == null) {
            LogUtil.error("FileUtil_writeByteArray_1", "file_is_null");
            return false;
        }
        if (!file.exists()) {
            LogUtil.error("FileUtil_writeByteArray_2", "file_is_not_exist");
            return false;
        }
        long buf_sizel = file.length();
        if (buf_sizel <= 0L) {
            LogUtil.error("FileUtil_writeByteArray_3", "file.length_is_0");
            return false;
        }
        int buf_size = ((Long) buf_sizel).intValue();
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            byte[] buffer = new byte[buf_size];
            int len = 0;
            while (-1 != (len = bufferedInputStream.read(buffer, 0, buf_size))) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
            return true;
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_writeByteArray_t4", throwable, file);
            return false;
        } finally {
            IOUtil.close(bufferedInputStream, fileInputStream, outputStream);
        }
    }

    /**
     * 2017-05-16 1.0.0
     * 跟据文件大小计算文件块数
     *
     * @param fileSize
     * @return chunkNum 文件块数
     */
    public static final int computeChunkNum(final long fileSize, final int chunkSize) {
        if (fileSize >= 0 && fileSize <= chunkSize) {
            return 1;
        } else if (fileSize > chunkSize) {
            double num = (fileSize * 1d) / chunkSize;
            int n = (int) num;
            if (num > n) {
                return n + 1;
            } else {
                return n;
            }
        } else {
            return -1;
        }
    }

    /**
     * 2024-05-24
     *
     * @return
     */
    public static final String getCharsetName(final boolean useCharsetNameOs) {
        if (useCharsetNameOs) {
            String charsetName = getCharsetNameOs();
            return charsetName;
        }
        return "UTF-8";
    }

    /**
     * 2024-05-24
     * 获取当前操作系统的系统编码
     * java读入文件时的字符编码是根据系统的默认编码来的
     *
     * @return 如UTF-8
     */
    public static final String getCharsetNameOs() {
        Charset charset = Charset.defaultCharset();
        String charsetName = charset.displayName();
        return charsetName;
    }

    /**
     * 2017-06-13 1.0.0
     * 得到文件的编码
     *
     * @param file 文件
     * @return 文件的编码
     */
    public static final String getCharsetNameFile(final File file) {
        String encoderType = FileEncoderUtil.getEncoderType(file);
        return encoderType;
    }

    /**
     * 2017-06-13 1.0.0
     * 获得指定文件的编码格式
     *
     * @param url
     */
    public static final String getCharsetNameFile(final String url) {
        String encoderType = FileEncoderUtil.getEncoderType(url);
        return encoderType;
    }

    /**
     * 2019-11-29 3.1.0
     * 自动创建一个临时文件，可确保该文件真实存在于磁盘
     */
    public static final String getTempFile() {
        String url = getTempFile(null, null);
        return url;
    }

    /**
     * 2022-05-25
     *
     * @param suffix 文件后缀，如txt
     */
    public static final String getTempFile(final String suffix) {
        String url = getTempFile(null, suffix);
        return url;
    }

    /**
     * 2019-11-29 3.1.0
     * 自动创建一个临时文件，可确保该文件真实存在于磁盘
     *
     * @param name   文件名，不包括.后缀
     * @param suffix 文件后缀，如txt
     */
    public static final String getTempFile(final String name, final String suffix) {
        String localSuffix = suffix;
        if (StringUtil.isBlank(localSuffix)) {
            localSuffix = "xc";
        }
        String localName = name;
        if (StringUtil.isBlank(localName)) {
            String snow = TimerUtil.now(TimerUtil.FORMATTYPE_M3);
            String random = RandomUtil.getStr(8);
            localName = "xc_crabapple_" + snow + "_" + random + "." + localSuffix;
        } else {
            localName += "." + localSuffix;
        }
        if (EnvironmentUtil.isWindows()) {
            String url = "/data/crabapple/" + localName;
            fileCreate(url);
            return url;
        }
        if (EnvironmentUtil.isLinux()) {
            String url = "/data/crabapple/" + localName;
            fileCreate(url);
            return url;
        }
        OSInfo osInfo = EnvironmentUtil.getOSInfo();
        String osName = osInfo.getOsName();
        LogUtil.error("FileUtil_getTempFile_1", osName);
        return "";
    }

    /**
     * 2020-08-03 6.4.0
     * 自动创建一个临时目录，可确保该目录真实存在于磁盘
     */
    public static final String getTempPath() {
        String tempPath = getTempPath(null);
        return tempPath;
    }

    /**
     * 2020-08-03 6.4.0
     * 自动创建一个临时目录，可确保该目录真实存在于磁盘
     */
    public static final String getTempPath(final String name) {
        String localName = name;
        if (StringUtil.isBlank(localName)) {
            String snow = TimerUtil.now(TimerUtil.FORMATTYPE_M3);
            String random = RandomUtil.getStr(8);
            localName = "xc_crabapple_" + snow + "_" + random;
        }
        if (EnvironmentUtil.isWindows()) {
            String path = "c:/crabapple/" + localName;
            dirMake(path);
            return path;
        }
        if (EnvironmentUtil.isLinux()) {
            String path = "/crabapple/" + localName;
            dirMake(path);
            return path;
        }
        OSInfo osInfo = EnvironmentUtil.getOSInfo();
        String osName = osInfo.getOsName();
        LogUtil.error("FileUtil_getTempPath_1", osName);
        return "";
    }

    /**
     * 2020-04-16 5.0.0
     * windows系统下，以下字符串是系统关键字，不能以该字符串作为文件名，大小写均有限制，任何格式也同样有限制
     *
     * @param urlOrName 可以是目录，文件路径或一个普通字符串
     */
    public static final boolean fileNameIsLegal(final String urlOrName) {
        if (EnvironmentUtil.isLinux()) {
            return true;
        }
        if (EnvironmentUtil.isWindows()) {
            getWindowsFilenameForbiddenMap();
            String urlOrNameTmp = UrlUtil.getStandardPath(urlOrName);
            String name = getFileName(urlOrNameTmp, false);
            name = StringUtil.toLowerCase(name);
            Integer value = windowsFilenameForbiddenMap.get(name);
            if (value == null) {
                return true;
            }
            LogUtil.error("FileUtil_fileNameIsLegal_1", "文件名是windows系统关键字, urlOrName=" + urlOrName);
            return false;
        }
        return true;
    }

    /**
     * 2024-05-24
     *
     * @param file
     * @return
     */
    public static final String fileReadText(final File file) {
        String content = fileReadText(file, "UTF-8");
        return content;
    }

    /**
     * 2024-05-24
     *
     * @param file
     * @param useCharsetNameOs 是否使用当前操作系统的系统编码
     * @return
     */
    public static final String fileReadText(final File file, final boolean useCharsetNameOs) {
        String charsetName = getCharsetName(useCharsetNameOs);
        String content = fileReadText(file, charsetName);
        return content;
    }

    /**
     * 2020-11-14 7.0.0
     *
     * @param file
     */
    public static final String fileReadText(final File file, final String charsetName) {
        String url = file.getAbsolutePath();
        String text = fileReadText(url, charsetName);
        return text;
    }

    /**
     * 2024-05-24
     *
     * @param url
     * @return
     */
    public static final String fileReadText(final String url) {
        String content = fileReadText(url, "UTF-8");
        return content;
    }

    /**
     * 2024-05-24
     *
     * @param url
     * @param useCharsetNameOs 是否使用当前操作系统的系统编码
     * @return
     */
    public static final String fileReadText(final String url, final boolean useCharsetNameOs) {
        String charsetName = getCharsetName(useCharsetNameOs);
        String content = fileReadText(url, charsetName);
        return content;
    }

    /**
     * 2025-08-22
     *
     * @param urlOrName 净化文件名去除特殊字符
     * @return
     */
    public static final String pureFileName(final String urlOrName) {
        if (StringUtil.isBlank(urlOrName)) {
            return null;
        }
        if (EnvironmentUtil.isLinux()) {
            if (CollectionUtil.isBlank(linuxFilenameForbiddenSet)) {
                linuxFilenameForbiddenSet = new HashSet<>(32);
                linuxFilenameForbiddenSet.add("\\");
                linuxFilenameForbiddenSet.add("/");
                linuxFilenameForbiddenSet.add("<");
                linuxFilenameForbiddenSet.add(">");
                linuxFilenameForbiddenSet.add("!");
                linuxFilenameForbiddenSet.add("*");
                linuxFilenameForbiddenSet.add("?");
                linuxFilenameForbiddenSet.add("[");
                linuxFilenameForbiddenSet.add("]");
                linuxFilenameForbiddenSet.add(";");
                linuxFilenameForbiddenSet.add("|");
                linuxFilenameForbiddenSet.add("&");
                linuxFilenameForbiddenSet.add("$");
                linuxFilenameForbiddenSet.add("`");
                linuxFilenameForbiddenSet.add("'");
                linuxFilenameForbiddenSet.add("\"");
                linuxFilenameForbiddenSet.add("(");
                linuxFilenameForbiddenSet.add(")");
                linuxFilenameForbiddenSet.add("{");
                linuxFilenameForbiddenSet.add("}");
                linuxFilenameForbiddenSet.add("~");
                linuxFilenameForbiddenSet.add("#");
                linuxFilenameForbiddenSet.add("@");
                linuxFilenameForbiddenSet.add("!");
            }
            String name = StringUtil.getStandardStr(urlOrName);
            for (String value : linuxFilenameForbiddenSet) {
                if (!name.contains(value)) {
                    continue;
                }
                name = StringUtil.replaceAll(name, value, "");
                LogUtil.warn("FileUtil_pureFileName_1", "文件名包含linux系统关键字, key=" + value + " urlOrName=" + urlOrName);
            }
            return name;
        }
        if (EnvironmentUtil.isWindows()) {
            if (CollectionUtil.isBlank(windowsFilenameForbiddenSet)) {
                windowsFilenameForbiddenSet = new HashSet<>(32);
                windowsFilenameForbiddenSet.add("con");
                windowsFilenameForbiddenSet.add("prn");
                windowsFilenameForbiddenSet.add("aux");
                windowsFilenameForbiddenSet.add("nul");
                windowsFilenameForbiddenSet.add("com0");
                windowsFilenameForbiddenSet.add("com1");
                windowsFilenameForbiddenSet.add("com2");
                windowsFilenameForbiddenSet.add("com3");
                windowsFilenameForbiddenSet.add("com4");
                windowsFilenameForbiddenSet.add("com5");
                windowsFilenameForbiddenSet.add("com6");
                windowsFilenameForbiddenSet.add("com7");
                windowsFilenameForbiddenSet.add("com8");
                windowsFilenameForbiddenSet.add("com9");
                windowsFilenameForbiddenSet.add("lpt0");
                windowsFilenameForbiddenSet.add("lpt1");
                windowsFilenameForbiddenSet.add("lpt2");
                windowsFilenameForbiddenSet.add("lpt3");
                windowsFilenameForbiddenSet.add("lpt4");
                windowsFilenameForbiddenSet.add("lpt5");
                windowsFilenameForbiddenSet.add("lpt6");
                windowsFilenameForbiddenSet.add("lpt7");
                windowsFilenameForbiddenSet.add("lpt8");
                windowsFilenameForbiddenSet.add("lpt9");
                windowsFilenameForbiddenSet.add("\\");
                windowsFilenameForbiddenSet.add("/");
                windowsFilenameForbiddenSet.add(":");
                windowsFilenameForbiddenSet.add("*");
                windowsFilenameForbiddenSet.add("?");
                windowsFilenameForbiddenSet.add("\"");
                windowsFilenameForbiddenSet.add("<");
                windowsFilenameForbiddenSet.add(">");
                windowsFilenameForbiddenSet.add("|");
            }
            String name = StringUtil.getStandardStr(urlOrName);
            for (String value : windowsFilenameForbiddenSet) {
                if (!name.contains(value)) {
                    continue;
                }
                name = StringUtil.replaceAll(name, value, "");
                LogUtil.warn("FileUtil_pureFileName_2", "文件名包含windows系统关键字, key=" + value + " urlOrName=" + urlOrName);
            }
            return name;
        }
        return urlOrName;
    }

    /**
     * 2017-05-16 1.0.0
     * 2018-03-07
     * 读取文件内容为字符串的文件
     * 用于包装InputStreamReader,提高处理性能。因为BufferedReader有缓冲的，而InputStreamReader没有
     */
    public static final String fileReadText(final String url, final String charsetName) {
        String localUrl = UrlUtil.getStandardPath(url);
        File file = new File(localUrl);
        if (!file.exists()) {
            return null;
        }
        if (!file.isFile()) {
            return null;
        }
        String localCharsetName = charsetName;
        if (StringUtil.isBlank(localCharsetName)) {
            localCharsetName = "UTF-8";
        }
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileInputStream = new FileInputStream(localUrl);
            inputStreamReader = new InputStreamReader(fileInputStream, localCharsetName);
            bufferedReader = new BufferedReader(inputStreamReader);
            String line = "";
            StringBuilder stringBuilder = new StringBuilder();
            boolean flag = true;
            while ((line = bufferedReader.readLine()) != null) {
                if (flag) {
                    flag = false;
                    stringBuilder.append(line);
                    continue;
                }
                stringBuilder.append("\n").append(line);
            }
            String str = stringBuilder.toString();
            return str;
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_fileReadStr_1", throwable, url);
            return null;
        } finally {
            IOUtil.close(bufferedReader, inputStreamReader, fileInputStream);
        }
    }

    /**
     * 2024-05-24
     *
     * @param inputStream
     * @return
     */
    public static final String fileReadText(final InputStream inputStream) {
        String content = fileReadText(inputStream, "UTF-8");
        return content;
    }

    /**
     * 2024-05-24
     *
     * @param inputStream
     * @param useCharsetNameOs 是否使用当前操作系统的系统编码
     * @return
     */
    public static final String fileReadText(final InputStream inputStream, final boolean useCharsetNameOs) {
        String charsetName = getCharsetName(useCharsetNameOs);
        String content = fileReadText(inputStream, charsetName);
        return content;
    }

    /**
     * 2017-05-16 1.0.0
     * 2018-03-07
     * 读取文件内容为字符串的文件
     * 用于包装InputStreamReader,提高处理性能。因为BufferedReader有缓冲的，而InputStreamReader没有
     */
    public static final String fileReadText(final InputStream inputStream, final String charsetName) {
        if (inputStream == null) {
            return null;
        }
        String localCharsetName = charsetName;
        if (StringUtil.isBlank(localCharsetName)) {
            localCharsetName = "UTF-8";
        }
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            inputStreamReader = new InputStreamReader(inputStream, localCharsetName);
            bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            boolean flag = true;
            while ((line = bufferedReader.readLine()) != null) {
                if (flag) {
                    flag = false;
                    stringBuilder.append(line);
                    continue;
                }
                stringBuilder.append("\n").append(line);
            }
            String str = stringBuilder.toString();
            return str;
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_fileReadStr_t2", throwable);
            return null;
        } finally {
            IOUtil.close(bufferedReader, inputStreamReader);
        }
    }

    /**
     * 2024-05-24
     *
     * @param url
     * @return
     */
    public static final String fileReadJson(final String url) {
        String content = fileReadJson(url, "UTF-8");
        return content;
    }

    /**
     * 2024-05-24
     *
     * @param url
     * @param useCharsetNameOs 是否使用当前操作系统的系统编码
     * @return
     */
    public static final String fileReadJson(final String url, final boolean useCharsetNameOs) {
        String charsetName = getCharsetName(useCharsetNameOs);
        String content = fileReadJson(url, charsetName);
        return content;
    }

    /**
     * 2018-03-07 1.1.0
     * 读取标准json内容的文件
     * 用于包装InputStreamReader,提高处理性能。因为BufferedReader有缓冲的，而InputStreamReader没有
     */
    public static final String fileReadJson(final String url, final String charsetName) {
        File file = new File(url);
        if (!file.exists()) {
            return null;
        }
        if (!file.isFile()) {
            return null;
        }
        String localCharsetName = charsetName;
        if (StringUtil.isBlank(localCharsetName)) {
            localCharsetName = "UTF-8";
        }
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileInputStream = new FileInputStream(url);
            inputStreamReader = new InputStreamReader(fileInputStream, localCharsetName);
            bufferedReader = new BufferedReader(inputStreamReader);
            String line = null;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                line = StringUtil.getPureStr(line);
                line = StringUtil.trim(line);
                if (StringUtil.isBlank(line)) {
                    continue;
                }
                boolean flag = line.startsWith("#");
                if (flag) {
                    continue;
                }
                stringBuilder.append(line);
            }
            String str = stringBuilder.toString();
            return str;
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_fileReadJson_1", throwable, url);
            return null;
        } finally {
            IOUtil.close(bufferedReader, inputStreamReader, fileInputStream);
        }
    }

    /**
     * 2024-05-24
     *
     * @param url
     * @return
     */
    public static final List<String> fileReadList(final String url) {
        List<String> list = fileReadList(url, "UTF-8");
        return list;
    }

    /**
     * 2024-05-24
     *
     * @param url
     * @param useCharsetNameOs 是否使用当前操作系统的系统编码
     * @return
     */
    public static final List<String> fileReadList(final String url, final boolean useCharsetNameOs) {
        String charsetName = getCharsetName(useCharsetNameOs);
        List<String> list = fileReadList(url, charsetName);
        return list;
    }

    /**
     * 2017-05-16 1.0.0
     * 读取文件内容为字符串的文件
     * 用于包装InputStreamReader,提高处理性能。因为BufferedReader有缓冲的，而InputStreamReader没有
     */
    public static final List<String> fileReadList(final String url, final String charsetName) {
        String localUrl = UrlUtil.getStandardPath(url);
        List<String> list = new ArrayList<>();
        File file = new File(localUrl);
        if (!file.exists()) {
            LogUtil.error("FileUtil_fileReadList_1", "file_no_found", localUrl);
            return list;
        }
        if (!file.isFile()) {
            return list;
        }
        String localCharsetName = charsetName;
        if (StringUtil.isBlank(localCharsetName)) {
            localCharsetName = "UTF-8";
        }
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileInputStream = new FileInputStream(localUrl);
            inputStreamReader = new InputStreamReader(fileInputStream, localCharsetName);
            bufferedReader = new BufferedReader(inputStreamReader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                list.add(line);
            }
            return list;
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_fileReadList_t1", throwable, url);
            return null;
        } finally {
            IOUtil.close(bufferedReader, inputStreamReader, fileInputStream);
        }
    }

    /**
     * 2024-05-24
     *
     * @param url
     * @return
     */
    public static final Map<String, String> fileReadMap(final String url) {
        Map<String, String> map = fileReadMap(url, "UTF-8");
        return map;
    }

    /**
     * 2024-05-24
     *
     * @param url
     * @param useCharsetNameOs 是否使用当前操作系统的系统编码
     * @return
     */
    public static final Map<String, String> fileReadMap(final String url, final boolean useCharsetNameOs) {
        String charsetName = getCharsetName(useCharsetNameOs);
        Map<String, String> map = fileReadMap(url, charsetName);
        return map;
    }

    /**
     * 2017-05-16 1.0.0
     * 读取文件内容为字符串的文件
     * 用于包装InputStreamReader,提高处理性能。因为BufferedReader有缓冲的，而InputStreamReader没有
     *
     * @param url
     * @param charsetName 内容编码
     */
    public static final Map<String, String> fileReadMap(final String url, final String charsetName) {
        Map<String, String> map = new HashMap<String, String>();
        File file = new File(url);
        if (!file.exists()) {
            return map;
        }
        if (!file.isFile()) {
            return map;
        }
        String localCharsetName = charsetName;
        if (StringUtil.isBlank(localCharsetName)) {
            localCharsetName = "UTF-8";
        }
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileInputStream = new FileInputStream(url);
            inputStreamReader = new InputStreamReader(fileInputStream, localCharsetName);
            bufferedReader = new BufferedReader(inputStreamReader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                line = StringUtil.getPureStr(line);
                if (StringUtil.isBlank(line)) {
                    continue;
                }
                map.put(line, "exist");
            }
            return map;
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_fileReadMap_1", throwable, url);
            return null;
        } finally {
            IOUtil.close(bufferedReader, inputStreamReader, fileInputStream);
        }
    }

    /**
     * 2024-09-23
     * 逐行读取，不保留在内存，适合无限大文本文件读取（超大文本文件读取）
     *
     * @param url
     */
    public static final void fileReadLine(final String url, final CrabappleCallback crabappleCallback) {
        FileInputStream fileInputStream = null;
        Scanner scanner = null;
        try {
            File file = new File(url);
            fileInputStream = new FileInputStream(file);
            scanner = new Scanner(fileInputStream, "UTF-8");
            while (scanner.hasNextLine()) {
                if (crabappleCallback == null) {
                    continue;
                }
                String line = scanner.nextLine();
                crabappleCallback.execute(line);
            }
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_fileReadLine_t101", throwable, url);
        } finally {
            IOUtil.close(fileInputStream, scanner);
        }
    }

    /**
     * 2024-05-24
     *
     * @param url
     * @param lineIndex
     * @param it
     * @param initContent
     * @return
     */
    public static final String fileReadLine(final String url, final long lineIndex, final boolean it, final String initContent) {
        String content = fileReadLine(url, lineIndex, it, initContent, "UTF-8");
        return content;
    }

    /**
     * 2024-05-24
     *
     * @param url
     * @param lineIndex
     * @param it
     * @param initContent
     * @param useCharsetNameOs 是否使用当前操作系统的系统编码
     * @return
     */
    public static final String fileReadLine(final String url, final long lineIndex, final boolean it, final String initContent, final boolean useCharsetNameOs) {
        String charsetName = getCharsetName(useCharsetNameOs);
        String content = fileReadLine(url, lineIndex, it, initContent, charsetName);
        return content;
    }

    /**
     * 2017-05-16 1.0.0
     * lineIndex从1开始
     *
     * @param url
     * @param it          true表示如果文件不存在则新建并写入内容initContent
     * @param initContent 初始内容，仅在it为true时生效
     * @param charsetName 内容编码
     * @parm lineIndex
     */
    public static final String fileReadLine(final String url, final long lineIndex, final boolean it, final String initContent, final String charsetName) {
        File file = new File(url);
        if (!file.exists()) {
            if (it) {
                fileWriteNew(url, initContent, false);
                return initContent;
            }
            return null;
        }
        if (!file.isFile()) {
            return null;
        }
        String localCharsetName = charsetName;
        if (StringUtil.isBlank(localCharsetName)) {
            localCharsetName = "UTF-8";
        }
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileInputStream = new FileInputStream(url);
            inputStreamReader = new InputStreamReader(fileInputStream, localCharsetName);
            bufferedReader = new BufferedReader(inputStreamReader);
            String line = null;
            long index = 1;
            while ((line = bufferedReader.readLine()) != null) {
                if (index == lineIndex) {
                    return line;
                }
                index++;
            }
            return line;
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_fileReadLine_1", throwable, url, lineIndex, it, initContent);
            return null;
        } finally {
            IOUtil.close(bufferedReader, inputStreamReader, fileInputStream);
        }
    }

    /**
     * 2024-05-24
     *
     * @param url
     * @param lineIndex
     * @param it
     * @param initContent
     * @return
     */
    public static final String fileReadLine2(final String url, final long lineIndex, final boolean it, final String initContent) {
        String content = fileReadLine2(url, lineIndex, it, initContent, "UTF-8");
        return content;
    }

    /**
     * 2024-05-24
     *
     * @param url
     * @param lineIndex
     * @param it
     * @param initContent
     * @return
     */
    public static final String fileReadLine2(final String url, final long lineIndex, final boolean it, final String initContent, final boolean useCharsetNameOs) {
        String charsetName = getCharsetName(useCharsetNameOs);
        String content = fileReadLine2(url, lineIndex, it, initContent, charsetName);
        return content;
    }

    /**
     * 2017-05-16 1.0.0
     * lineIndex从1开始
     *
     * @param url
     * @param it          true表示如果文件不存在则新建并写入内容initContent
     * @param initContent 初始内容，仅在it为true时生效
     * @parm lineIndex
     */
    public static final String fileReadLine2(final String url, final long lineIndex, final boolean it, final String initContent, final String charsetName) {
        String localCharsetName = charsetName;
        if (StringUtil.isBlank(localCharsetName)) {
            localCharsetName = "UTF-8";
        }
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            File file = new File(url);
            if (!file.exists()) {
                if (it) {
                    fileWriteNew(url, initContent, false);
                    return initContent;
                }
                return null;
            }
            if (!file.isFile()) {
                return null;
            }
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            inputStreamReader = new InputStreamReader(bufferedInputStream, localCharsetName);
            bufferedReader = new BufferedReader(inputStreamReader, 5 * 1024 * 1024);  //用5M的缓冲读取文本文件
            String line = "";
            long i = 1L;
            while ((line = bufferedReader.readLine()) != null) {
                if (lineIndex == i) {
                    return line;
                }
                i++;
            }
            return line;
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_fileReadLine_2", throwable, url, lineIndex, it, initContent);
            return null;
        } finally {
            IOUtil.close(bufferedReader, inputStreamReader, bufferedInputStream, fileInputStream);
        }
    }

    /**
     * 2022-03-05
     * 2024-09-27 代码整理统一重载
     * 支持超大文本文件写入、支持超大文本文件频繁读写
     * 写入指定文件数据，重写
     *
     * @param url     文件地址
     * @param content 新内容
     */
    public static final boolean fileWriteNew(final String url, final String content) {
        boolean flag = p_fileWrite(url, content, true, false);
        return flag;
    }

    /**
     * 2017-05-16 1.0.0
     * 2024-09-27 代码整理统一重载
     * 支持超大文本文件写入、支持超大文本文件频繁读写
     * 写入指定文件数据，重写
     *
     * @param url             文件地址
     * @param content         新内容
     * @param deleteBreakLine 是否删除结尾换行符\n
     */
    public static final boolean fileWriteNew(final String url, final String content, final boolean deleteBreakLine) {
        boolean flag = p_fileWrite(url, content, true, deleteBreakLine);
        return flag;
    }

    /**
     * 2025-10-16
     * 方法重载
     * @param url
     * @param content
     * @param isConver
     * @param deleteBreakLine
     * @return
     */
    public static final boolean fileWriteNew(final String url, final String content, final boolean isConver, final boolean deleteBreakLine){
        boolean flag = p_fileWrite(url, content, isConver, deleteBreakLine);
        return flag;
    }

    /**
     * 2022-03-05
     * 2024-09-27 代码整理统一重载
     * 支持超大文本文件写入、支持超大文本文件频繁读写
     *
     * @param url     文件地址
     * @param content 新内容
     */
    public static final boolean fileWriteAppend(final String url, final String content) {
        boolean flag = p_fileWrite(url, content, false, false);
        return flag;
    }

    /**
     * 2022-03-05
     * 2024-09-27 代码整理统一重载
     * 支持超大文本文件写入、支持超大文本文件频繁读写
     *
     * @param url             文件地址
     * @param content         新内容
     * @param deleteBreakLine 是否删除结尾换行符\n
     */
    public static final boolean fileWriteAppend(final String url, final String content, final boolean deleteBreakLine) {
        boolean flag = p_fileWrite(url, content, false, deleteBreakLine);
        return flag;
    }

    /**
     * 2017-05-16 1.0.0
     * 2024-09-27 代码整理统一重载
     * 支持超大文本文件写入、支持超大文本文件频繁读写
     *
     * @param url             文件地址
     * @param content         新内容
     * @param isConver        true-覆盖原有内容（即删除原文件，新建文件），false-追加内容
     * @param deleteBreakLine 是否删除结尾换行符\n
     */
    private static final boolean p_fileWrite(final String url, final String content, final boolean isConver, final boolean deleteBreakLine) {
        String localUrl = UrlUtil.getStandardPath(url);
        if (isConver) {
            boolean flag = fileDelete(localUrl);
            if (!flag) {
                return false;
            }
        }
        boolean flag = fileCreate(localUrl);
        if (!flag) {
            return false;
        }
        String localContent = content;
        if (StringUtil.isBlank(localContent)) {
            localContent = "";
        }
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(localUrl, true);  // 第2个参数true表示以追加形式写文件
            if (deleteBreakLine) {
                localContent = StringUtil.removeEndBreakLine(localContent);
            }
            fileWriter.write(localContent);
            return true;
        } catch (Throwable throwable) {
            LogUtil.error("FileUtil_p_fileWrite_t1", throwable, url);
            return false;
        } finally {
            IOUtil.close(fileWriter);
        }
    }

    /**
     * 2019-11-29 3.1.0
     *
     * @param str
     */
    public static final File text2file(final String str) {
        File file = text2file(str, null);
        return file;
    }

    /**
     * 2019-11-29 3.1.0
     *
     * @param str
     */
    public static final File text2file(final String str, final String name) {
        if (StringUtil.isBlank(str)) {
            return null;
        }
        String url = getTempFile(name);
        fileWriteNew(url, str, false);
        File file = new File(url);
        return file;
    }

}