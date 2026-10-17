package xc.crabapple.core.io;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.Base64Util;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.serialization.SerializationUtil;
import xc.crabapple.core.thread.ThreadUtil;
import xc.crabapple.core.url.UrlUtil;

/**
 * 2017-01-20 1.0.0
 */
public final class IOUtil{

    /**
     * 2024-04-20
     * @param url
     * @return
     */
    public static final OutputStream file2outputStream(final String url){
        String localUrl = UrlUtil.getStandardPath(url);
        File file = new File(localUrl);
        OutputStream outputStream = file2outputStream(file);
        return outputStream;
    }

    /**
     * 2024-04-20
     * @param file
     * @return
     */
    public static final OutputStream file2outputStream(final File file){
        if(!file.exists()){
            return null;
        }
        if(!file.isFile()){
            return null;
        }
        //
        try{
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            return fileOutputStream;
        }catch(Throwable throwable){
            String url = file.getAbsolutePath();
            LogUtil.error("IOUtil_file2outputStream_t1", throwable, url);
            return null;
        }
    }

    /**
     * 2023-07-19
     * @param url
     * @return
     */
    public static final InputStream file2inputStream(final String url){
        String localUrl = UrlUtil.getStandardPath(url);
        File file = new File(localUrl);
        InputStream inputStream = file2inputStream(file);
        return inputStream;
    }

    /**
     * 2024-04-20
     * @param file
     * @return
     */
    public static final InputStream file2inputStream(final File file){
        if(!file.exists()){
            return null;
        }
        if(!file.isFile()){
            return null;
        }
        //
        try{
            FileInputStream fileInputStream = new FileInputStream(file);
            return fileInputStream;
        }catch(Throwable throwable){
            String url = file.getAbsolutePath();
            LogUtil.error("IOUtil_file2inputStream_t1", throwable, url);
            return null;
        }
    }

    /**
     * 2020-11-14 7.0.0
     * @param inputStream
     * @return 若返回0则说明输入流异常
     */
    public static final int getInputstreamLength(final InputStream inputStream){
        try{
            int length = inputStream.available();
            int count = 0;
            while(true) {
                if(length > 0){
                    break;
                }
                if(count > 3){
                    break;
                }
                count++;
                length = inputStream.available();
                ThreadUtil.sleep(1000L);
            }
            return length;
        }catch(Throwable throwable){
            LogUtil.error("IOUtil_getInputstreamLength_t1", throwable);
            return 0;
        }
    }

    /**
     * 2024-11-21
     * 方法1：将指定的本地文件转为字节数组
     * @param url
     * @return
     */
    public static final byte[] file2bytes(String url){
        url = UrlUtil.getStandardPath(url);
        try{
            final Path path = Paths.get(url);
            final byte[] bytes = Files.readAllBytes(path);
            return bytes;
        }catch (Throwable throwable){
            LogUtil.error("IOUtil_file2bytes_t1", throwable, url);
            return null;
        }
    }

    /**
     * 2022-12-22
     * 方法2：将指定的本地文件转为字节数组
     */
    public static final byte[] file2bytes2(String url){
        url = UrlUtil.getStandardPath(url);
        final File file = new File(url);
        byte[] bytes = file2bytes(file);
        return bytes;
    }

    /**
     * 2022-12-22
     * 将file转成byte[]
     */
    public static final byte[] file2bytes(final File file){
        if(!file.exists()){
            LogUtil.error("IOUtil_file2bytes_1", "文件不存在", file);
            return null;
        }
        if(!file.isFile()){
            LogUtil.error("IOUtil_file2bytes_2", "不是文件", file);
            return null;
        }
        //
        FileInputStream fileInputStream = null;
        try{
            fileInputStream = new FileInputStream(file);
        }catch(Throwable throwable){
            close(fileInputStream);
            LogUtil.error("IOUtil_file2byte_t3", throwable);
            return null;
        }
        //
        byte[] bytes = file2bytes(fileInputStream, true);
        return bytes;
    }

    /**
     * 2023-01-25
     * 将FileInputStream转成byte[]
     */
    public static final byte[] file2bytes(final FileInputStream fileInputStream, final boolean isClose){
        if(fileInputStream == null){
            return null;
        }
        //
        ByteArrayOutputStream byteArrayOutputStream = null;
        try{
            byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fileInputStream.read(b)) != -1){
                byteArrayOutputStream.write(b, 0, n);
            }
            byte[] bytes = byteArrayOutputStream.toByteArray();
            return bytes;
        }catch(Throwable throwable){
            LogUtil.error("IOUtil_file2byte_t10", throwable);
            return null;
        } finally {
            if(isClose){
                close(byteArrayOutputStream, fileInputStream);
            }else{
                close(byteArrayOutputStream);
            }
        }
    }

    /**
     * 2020-11-10 6.9.0
     * 源字节数组生成本地文件，支持任何文件格式。
     * @param bytes 源字节数组
     * @param targetUrl 本地目标文件，任何格式
     */
    public static final String bytes2file(final byte[] bytes, String targetUrl){
        if(CollectionUtil.isBlank(bytes)){
            return "error-1";
        }
        targetUrl = UrlUtil.getStandardPath(targetUrl);
        FileUtil.fileDelete(targetUrl);
        String path = UrlUtil.getPathByUrl(targetUrl);
        FileUtil.dirMake(path);
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try{
            inputStream = new ByteArrayInputStream(bytes);
            outputStream = new FileOutputStream(targetUrl);
            inputstream2outputstream(inputStream, outputStream);
            outputStream.flush();
            return "success";
        }catch(Throwable throwable){
            LogUtil.error("IOUtil_bytes2file_t1", throwable, targetUrl);
            return "error-2";
        }finally {
            close(outputStream, inputStream);
        }
    }

    /**
     * 2023-01-25
     * 将file转成可网络传输的字节数组
     */
    public static final String file2bytesStr(final String url){
        byte[] bytes = file2bytes(url);
        String bytesStr = SerializationUtil.serializeObject2bytesStr(bytes);
        return bytesStr;
    }

    /**
     * 2023-01-25
     * 将file转成可网络传输的字节数组
     */
    public static final String file2bytesStr(File file){
        byte[] bytes = file2bytes(file);
        String bytesStr = SerializationUtil.serializeObject2bytesStr(bytes);
        return bytesStr;
    }

    /**
     * 2023-01-25
     * 将FileInputStream转成可网络传输的字节数组
     */
    public static final String file2bytesStr(final FileInputStream fileInputStream, final boolean isClose){
        byte[] bytes = file2bytes(fileInputStream, isClose);
        String bytesStr = SerializationUtil.serializeObject2bytesStr(bytes);
        return bytesStr;
    }

    /**
     * 2024-11-21
     * 获取指定本地文件内容的base64编码字符串
     * @param file
     * @return
     */
    public static final String file2base64Str(final File file){
        final byte[] sourceBytes = file2bytes(file);
        final String base64Str = Base64Util.encodeBytes2str(sourceBytes);
        return base64Str;
    }

    /**
     * 2024-11-21
     * 获取指定本地文件内容的base64编码字符串
     * @param url
     * @return
     */
    public static final String file2base64Str(final String url){
        final byte[] sourceBytes = file2bytes(url);
        final String base64Str = Base64Util.encodeBytes2str(sourceBytes);
        return base64Str;
    }

    /**
     * 2024-11-21
     * base64编码字符串生成指定本地文件
     * @param targetUrl
     * @return
     */
    public static final String base64Str2file(final String base64Str, String targetUrl){
        targetUrl = UrlUtil.getStandardPath(targetUrl);
        final byte[] sourceBytes = Base64Util.decodeStr2bytes(base64Str);
        final String result = bytes2file(sourceBytes, targetUrl);
        return result;
    }

    /**
     * 2024-07-25
     * 字节数组转为输入流对象
     * @param bytes
     * @return
     */
    public static final ServletInputStream bytes2servletInputStream(final byte[] bytes){
        ByteArrayInputStream byteArrayInputStream = bytes2byteArrayInputStream(bytes);
        ServletInputStream servletInputStream = new ServletInputStream() {
            @Override
            public boolean isFinished() {
                int available = byteArrayInputStream.available();
                if(available == 0){
                    return true;
                }
                return false;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public int read() throws IOException {
                int result = byteArrayInputStream.read();
                return result;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }
        };
        return servletInputStream;
    }

    /**
     * 2024-07-25
     * 字节数组转为输入流对象
     * @param bytes
     * @return
     */
    public static final ByteArrayInputStream bytes2byteArrayInputStream(final byte[] bytes){
        if(CollectionUtil.isBlank(bytes)){
            return null;
        }
        //
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        return byteArrayInputStream;
    }

    /**
     * 2024-07-25
     * 输入流转为字节数组
     * @param inputStream
     * @return
     */
    public static final byte[] inputstream2bytes(final InputStream inputStream){
        ByteArrayOutputStream byteArrayOutputStream = null;
        byte[] buffer = new byte[4096];
        int bytesRead;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            byte[] bytes = byteArrayOutputStream.toByteArray();
            return bytes;
        }catch (Throwable throwable){
            LogUtil.error("IOUtil_inputstream2bytes_t1", throwable);
            return null;
        }finally {
            close(byteArrayOutputStream);
        }
    }

    /**
     * 2023-09-01
     * 输入流对象复制拷贝
     * @param inputStream
     * @return
     */
    public static final List<ByteArrayInputStream> inputstreamCopy(final InputStream inputStream){
        List<ByteArrayInputStream> list = inputstreamCopy(inputStream, 2);
        return list;
    }

    /**
     * 2023-09-01
     * 输入流复制，输入流对象复制拷贝
     * @param inputStream
     * @param copyAmount 复制对象的数量
     * @return
     */
    public static final List<ByteArrayInputStream> inputstreamCopy(final InputStream inputStream, final Integer copyAmount){
        int localCopyAmount = copyAmount;
        if(localCopyAmount <= 0){
            localCopyAmount = 1;
        }
        byte[] bytes = inputstream2bytes(inputStream);
        List<ByteArrayInputStream> list = new ArrayList<>();
        for(int i = 0; i < localCopyAmount; i++){
            ByteArrayInputStream copyInputStream = new ByteArrayInputStream(bytes);
            list.add(copyInputStream);
        }
        return list;
    }

    /**
     * 2019-12-02 2.1.0
     * inputStream中的数据写入outputStream
     */
    public static final OutputStream inputstream2outputstream(final InputStream inputStream, final OutputStream outputStream){
        if(inputStream == null){
            return null;
        }
        try{
            byte[] buf = new byte[1024];
            int len = inputStream.read(buf);
            while(len != -1){
                outputStream.write(buf, 0, len);
                len = inputStream.read(buf);
            }
            return outputStream;
        }catch(Throwable throwable){
            LogUtil.error("IOUtil_inputstream2outputstream_t1", throwable);
            return null;
        }finally {
            close(inputStream);
        }
    }

    /**
     * 2024-07-25
     * 字符串转为输入流对象
     * @param str
     * @return
     */
    public static final InputStream str2byteArrayInputStream(final String str){
        byte[] bytes = StringUtil.str2bytes(str);
        ByteArrayInputStream byteArrayInputStream = bytes2byteArrayInputStream(bytes);
        return byteArrayInputStream;
    }

    /**
     * 2023-08-30
     * 输入流转为字符串
     * @param inputStream
     */
    public static final String inputstream2str(final InputStream inputStream) {
        final Charset charset = Charset.forName("UTF-8");
        final String result = inputstream2str(inputStream, charset);
        return result;
    }

    /**
     * 2023-08-30
     * 输入流对象转为字符串
     * @param inputStream
     * @param charset
     */
    public static final String inputstream2str(final InputStream inputStream, final Charset charset) {
        if (inputStream == null) {
            return "";
        }
        InputStreamReader reader = null;
        try{
            reader = new InputStreamReader(inputStream, charset);
            StringBuilder stringBuilder = new StringBuilder();
            char[] buffer = new char[4096];
            int bytesRead = -1;
            while ((bytesRead = reader.read(buffer)) != -1) {
                stringBuilder.append(buffer, 0, bytesRead);
            }
            String result = stringBuilder.toString();
            return result;
        }catch(Throwable throwable){
            LogUtil.error("IOUtil_inputstream2str_t2", throwable);
            return "";
        }finally {
            close(reader);
        }
    }

    /**
     * 2018-03-25 1.1.0
     * 输入流对象转换为File对象
     * @param inputStream 文件流
     * @return File
     */
    public static final File inputstream2file(final InputStream inputStream){
        String url = FileUtil.getTempFile();
        File file = inputstream2file(inputStream, url);
        return file;
    }

    /**
     * 2019-11-29 3.1.0
     * 输入流对象转换为File对象
     */
    public static final File inputstream2file(final InputStream inputStream, final String targetUrl){
        String toPath = UrlUtil.getPathByUrl(targetUrl);
        FileUtil.dirMake(toPath);
        File file = new File(targetUrl);
        OutputStream outputStream = null;
        int readLen = 0;
        try{
            int length = getInputstreamLength(inputStream);
            if(length <= 0){
                LogUtil.error("IOUtil_inputstream2file_1", "inputStream数据为空", targetUrl);
                return null;
            }
            byte[] buffer = new byte[4096];
            outputStream = new FileOutputStream(file);
            while ((readLen = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, readLen);
            }
            return file;
        }catch(Throwable throwable){
            LogUtil.error("IOUtil_inputstream2file_t1", throwable, targetUrl);
            return null;
        }finally {
            close(outputStream);
        }
    }

    /**
     * 2023-06-08
     * 把指定的输入流对象写入磁盘，即把输入流对象转为本地文件
     */
    public static final boolean inputstream2disk(final InputStream inputStream, final String targetUrl){
        final boolean success = inputstream2disk(inputStream, targetUrl, false);
        return success;
    }

    /**
     * 2023-06-08
     * 把指定的输入流对象写入磁盘，即把输入流对象转为本地文件
     */
    public static final boolean inputstream2disk(final InputStream inputStream, final String targetUrl, final boolean isClose){
        String localTargetUrl = UrlUtil.getStandardPath(targetUrl);
        String pdfPath = UrlUtil.getPathByUrl(localTargetUrl);
        FileUtil.dirMake(pdfPath);
        FileOutputStream fileOutputStream = null;
        try{
            fileOutputStream = new FileOutputStream(localTargetUrl);
            byte[] bytes = new byte[1024];
            int n = 0;
            while ((n = inputStream.read(bytes)) != -1){
                fileOutputStream.write(bytes, 0, n);
            }
            return true;
        }catch(Throwable throwable){
            LogUtil.error("IOUtil_inputstream2disk_t1", throwable, "写入磁盘失败", localTargetUrl, isClose);
            return false;
        }finally{
            close(fileOutputStream);
            if(isClose){
                close(inputStream);
            }
        }
    }

    /**
     * 2025-02-18
     * @param inputStream
     * @return
     */
    public static final String inputstream2text(final InputStream inputStream){
        String text = inputstream2text(inputStream, false);
        return text;
    }

    /**
     * 2025-02-18
     * @param inputStream
     * @param closeIn
     * @return
     */
    public static final String inputstream2text(final InputStream inputStream, final boolean closeIn){
        ByteArrayOutputStream byteArrayOutputStream = null;
        try{
            byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] bytes = new byte[8192];
            int len;
            while ((len = inputStream.read(bytes))  != -1) {
                byteArrayOutputStream.write(bytes,  0, len);
            }
            byte[] bytes2 = byteArrayOutputStream.toByteArray();
            String text = new String(bytes2,  "UTF-8");
            return text;
        }catch (Throwable throwable){
            LogUtil.error("IOUtil_inputstream2text_t1", throwable, closeIn);
            return null;
        }finally {
            close(byteArrayOutputStream);
            if(closeIn){
                close(inputStream);
            }
        }
    }

    /**
     * 2025-08-19
     * 输入流对象转换为标准base64字符串
     * @param inputStream
     * @return
     */
    public static final String inputstream2base64(final InputStream inputStream){
        final String base64 = inputstream2base64(inputStream, false);
        return base64;
    }

    /**
     * 2025-08-19
     * 输入流对象转换为标准base64字符串
     * @param inputStream
     * @param closeIn
     * @return
     */
    public static final String inputstream2base64(final InputStream inputStream, final boolean closeIn){
        if (inputStream == null) {
            return "";
        }
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] bytes = new byte[1024];
            int len = 0;
            while ((len = inputStream.read(bytes)) != -1) {
                byteArrayOutputStream.write(bytes, 0, len);
            }
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            Base64.Encoder encoder = Base64.getEncoder();
            String base64 = encoder.encodeToString(byteArray);
            return base64;
        } catch (Throwable throwable) {
            LogUtil.error("IOUtil_inputstream2base64_t1", throwable);
            return "";
        } finally {
            close(byteArrayOutputStream);
            if(closeIn){
                close(inputStream);
            }
        }
    }

    /**
     * 2025-02-18
     * @param inputStream
     * @return
     */
    public static final List<String> inputstream2list(final InputStream inputStream){
        final List<String> list = inputstream2list(inputStream, false);
        return list;
    }

    /**
     * 2025-02-18
     * @param inputStream
     * @param closeIn
     * @return
     */
    public static final List<String> inputstream2list(final InputStream inputStream, final boolean closeIn){
        List<String> list = new ArrayList<>();
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try{
            inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            bufferedReader = new BufferedReader(inputStreamReader);
            String line = null;
            while((line = bufferedReader.readLine()) != null){
                list.add(line);
            }
            return list;
        }catch(Throwable throwable){
            LogUtil.error("IOUtil_inputstream2list_t1", throwable, closeIn);
            return null;
        }finally{
            close(bufferedReader, inputStreamReader);
            if(closeIn){
                close(inputStream);
            }
        }
    }

    /**
     * 2017-01-20 1.0.0
     * @param autoCloseables
     */
    public static final void close(AutoCloseable... autoCloseables){
        if(autoCloseables == null){
            return;
        }
        if(autoCloseables.length <= 0){
            return;
        }
        for(AutoCloseable autoCloseable : autoCloseables){
            if(autoCloseable == null){
                continue;
            }
            if(autoCloseable instanceof Flushable){
                try{
                    ((Flushable) autoCloseable).flush();
                }catch(Throwable throwable){
                    //ignore
                }
            }
            try{
                autoCloseable.close();
            }catch(Throwable throwable){
                //ignore
            }
        }
    }

    /**
     * 2025-06-26
     * @param httpURLConnections
     */
    public static final void disconnect(HttpURLConnection... httpURLConnections){
        if(httpURLConnections == null){
            return;
        }
        if(httpURLConnections.length <= 0){
            return;
        }
        for(HttpURLConnection httpURLConnection : httpURLConnections){
            if(httpURLConnection == null){
                continue;
            }
            try{
                httpURLConnection.disconnect();
            }catch(Throwable throwable){
                //ignore
            }
        }
    }

}