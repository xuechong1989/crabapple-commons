package xc.crabapple.core.file.compress;

import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 2025-01-02 构建zip压缩与解压
 * 2025-02-14 新增7z压缩
 * 2025-05-08 重载zip压缩
 */
public final class CompressUtil {

    /**
     * 2025-02-14
     * 7z压缩
     * 把指定文件或目录压缩为7z格式文件。格式如：D:/app/z7/7z.exe a -t7z -mx7 -p123456 d:/aaa.7z d:/aaa
     * @param homeUrl 本地7z软件安装路径，格式如：D:/app/z7/7z.exe
     * @param password 压缩密码
     * @param level 压缩等级，0-不压缩，1-快速压缩，5-正常压缩，7-最大压缩，9-极限压缩
     * @param fromUrlOrPath 待压缩的文件或目录
     * @param to7zUrl 压缩后的7z文件绝对路径
     */
    public static final boolean z7(final String homeUrl, final String fromUrlOrPath, final String to7zUrl, final Integer level, final String password) {
        Process process = null;
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            String homeUrlLocal = UrlUtil.getStandardPath(homeUrl);
            String fromUrlOrPathLocal = UrlUtil.getStandardPath(fromUrlOrPath);
            String to7zUrlLocal = UrlUtil.getStandardPath(to7zUrl);
            String[] commandArray = {homeUrlLocal, "a", "-t7z", "-mx"+level, "-p" + password, "-mhe=on", to7zUrlLocal, fromUrlOrPathLocal};
            FileUtil.fileDelete(to7zUrlLocal);
            Runtime runtime = Runtime.getRuntime();
            process = runtime.exec(commandArray);
            inputStream = process.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream, "GBK");
            bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine())  != null) {
                // LogUtil.print("CompressUtil_z7_4", "[7z]", line);
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                LogUtil.print("CompressUtil_z7_1", "文件压缩成功", exitCode, fromUrlOrPathLocal, to7zUrlLocal, level, password);
                return true;
            }
            FileUtil.fileDelete(to7zUrlLocal);
            LogUtil.error("CompressUtil_z7_2", "文件压缩失败", exitCode, homeUrlLocal, fromUrlOrPathLocal, to7zUrlLocal, level, password);
            return false;
        }catch (Throwable throwable){
            FileUtil.fileDelete(to7zUrl);
            LogUtil.error("CompressUtil_z7_t1", throwable, "文件压缩异常", homeUrl, fromUrlOrPath, to7zUrl, level, password);
            return false;
        }finally {
            IOUtil.close(bufferedReader, inputStreamReader, inputStream);
            if(process != null){
                process.destroy();
            }
        }
    }

    /**
     * 2025-11-25
     * 压缩文件或文件夹
     * @param fromPath 源文件/文件夹路径
     */
    public static final void zip(final String fromPath) {
        String fromPathLocal = UrlUtil.getStandardPath(fromPath);
        String parentPath = UrlUtil.getPathByUrl(fromPathLocal);
        String filename = UrlUtil.getFileName(fromPathLocal, false);
        String toZipUrl = parentPath + "/" + filename + ".zip";
        zip(fromPathLocal, toZipUrl, true);
    }

    /**
     * 2025-11-25
     * 压缩文件或文件夹
     * @param fromPath 源文件/文件夹路径
     * @param isKeepDirStructure 是否保留目录结构
     */
    public static final void zip(final String fromPath, final boolean isKeepDirStructure) {
        String fromPathLocal = UrlUtil.getStandardPath(fromPath);
        String parentPath = UrlUtil.getPathByUrl(fromPathLocal);
        String filename = UrlUtil.getFileName(fromPathLocal, false);
        String toZipUrl = parentPath + "/" + filename + ".zip";
        zip(fromPathLocal, toZipUrl, isKeepDirStructure);
    }

    /**
     * 2025-11-25
     * 压缩文件或文件夹
     * @param fromPath 源文件/文件夹路径
     * @param toZipUrl 目标ZIP文件路径
     */
    public static final void zip(final String fromPath, final String toZipUrl) {
        zip(fromPath, toZipUrl, true);
    }

    /**
     * 2025-11-25
     * 压缩文件或文件夹，可选择是否保留目录结构
     * @param fromPath 源文件/文件夹路径
     * @param toZipUrl 目标ZIP文件路径
     * @param isKeepDirStructure 是否保留目录结构
     */
    public static final String zip(final String fromPath, final String toZipUrl, final boolean isKeepDirStructure) {
        String fromPathLocal = UrlUtil.getStandardPath(fromPath);
        String toZipUrlLocal = UrlUtil.getStandardPath(toZipUrl);
        //
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        ZipOutputStream zipOutputStream = null;
        try {
            File fromFile = new File(fromPathLocal);
            if (!fromFile.exists()) {
                return "error_1";
            }
            // 使用UTF-8编码防止中文乱码
            fileOutputStream = new FileOutputStream(toZipUrlLocal);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            zipOutputStream = new ZipOutputStream(bufferedOutputStream, StandardCharsets.UTF_8);
            if (fromFile.isDirectory()) {
                if (isKeepDirStructure) {
                    String result = p_zipWithFullStructure(fromFile, zipOutputStream, "");
                    return result;
                }
                String fromFileName = fromFile.getName();
                String result = p_zipDirectory(fromFile, fromFileName, zipOutputStream);
                return result;
            }
            String result = p_zipSingleFile(fromFile, zipOutputStream);
            return result;
        } catch (Throwable throwable) {
            LogUtil.error("CompressUtil_zip_t1", throwable, "文件或目录压缩为zip时异常", fromPathLocal, toZipUrlLocal, isKeepDirStructure);
            return "error_2";
        }finally {
            IOUtil.close(zipOutputStream, bufferedOutputStream, fileOutputStream);
        }
    }

    /**
     * 2025-11-25
     * 压缩单个文件
     */
    private static final String p_zipSingleFile(final File fromFile, final ZipOutputStream zipOutputStream) {
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            fileInputStream = new FileInputStream(fromFile);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            String fromFileName = fromFile.getName();
            ZipEntry zipEntry = new ZipEntry(fromFileName);
            zipOutputStream.putNextEntry(zipEntry);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = bufferedInputStream.read(buffer)) >= 0) {
                zipOutputStream.write(buffer, 0, length);
            }
            zipOutputStream.closeEntry();
            return "ok";
        }catch (Throwable throwable) {
            LogUtil.error("CompressUtil_p_zipSingleFile_t1", throwable, "文件或目录压缩为zip时异常", fromFile);
            return "error_4";
        }finally {
            IOUtil.close(bufferedInputStream, fileInputStream);
        }
    }

    /**
     * 2025-11-25
     * 压缩目录（相对路径）
     */
    private static final String p_zipDirectory(final File fromFile, final String fromFileName, final ZipOutputStream zipOutputStream) {
        try{
            File[] fileArray = fromFile.listFiles();
            if (fileArray == null){
                return "error_7";
            }
            // 处理空目录：创建目录条目
            if (fileArray.length == 0) {
                String entryPath = fromFileName + "/";
                ZipEntry zipEntry = new ZipEntry(entryPath);
                zipOutputStream.putNextEntry(zipEntry);
                zipOutputStream.closeEntry();
                return "error_8";
            }
            for (File file : fileArray) {
                String entryPath = fromFileName + "/" + file.getName();
                if (file.isDirectory()) {
                    // 为目录创建条目（以"/"结尾）
                    String dirEntryPath = entryPath + "/";
                    ZipEntry zipEntry = new ZipEntry(dirEntryPath);
                    zipOutputStream.putNextEntry(zipEntry);
                    zipOutputStream.closeEntry();
                    // 递归处理子目录
                    p_zipDirectory(file, entryPath, zipOutputStream);
                } else {
                    // 压缩文件
                    p_zipToDirZip(file, entryPath, zipOutputStream);
                }
            }
            return "ok";
        }catch (Throwable throwable) {
            LogUtil.error("CompressUtil_p_zipDirectory_t1", throwable, "文件或目录压缩为zip时异常", fromFile, fromFileName);
            return "error_5";
        }
    }

    /**
     * 2025-11-25
     * 保留完整目录结构的压缩方法
     */
    private static final String p_zipWithFullStructure(final File fromFile, final ZipOutputStream zipOutputStream, final String basePath) {
        try{
            File[] filesArray = fromFile.listFiles();
            if (filesArray == null){
                return "error_10";
            }
            for (File file : filesArray) {
                String entryName = basePath.isEmpty() ? file.getName() : basePath + "/" + file.getName();
                if (file.isDirectory()) {
                    // 创建目录条目
                    String dirEntry = entryName + "/";
                    zipOutputStream.putNextEntry(new ZipEntry(dirEntry));
                    zipOutputStream.closeEntry();
                    // 递归处理子目录
                    p_zipWithFullStructure(file, zipOutputStream, entryName);
                } else {
                    // 压缩文件
                    p_zipToDirZip(file, entryName, zipOutputStream);
                }
            }
            // 处理空目录
            if (filesArray.length == 0 && !basePath.isEmpty()) {
                String dirEntry = basePath + "/";
                zipOutputStream.putNextEntry(new ZipEntry(dirEntry));
                zipOutputStream.closeEntry();
            }
            return "ok";
        }catch (Throwable throwable) {
            LogUtil.error("CompressUtil_p_zipWithFullStructure_t1", throwable, "文件或目录压缩为zip时异常", fromFile, basePath);
            return "error_9";
        }
    }

    /**
     * 2025-11-25
     * 将文件压缩到ZIP中的指定路径
     */
    private static final String p_zipToDirZip(final File file, final String entryPath, final ZipOutputStream zipOutputStream) {
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            ZipEntry zipEntry = new ZipEntry(entryPath);
            zipOutputStream.putNextEntry(zipEntry);
            byte[] bytes = new byte[4096];
            int length;
            while ((length = bufferedInputStream.read(bytes)) >= 0) {
                zipOutputStream.write(bytes, 0, length);
            }
            zipOutputStream.closeEntry();
            return "ok";
        }catch (Throwable throwable) {
            LogUtil.error("CompressUtil_p_zipToDirZip_t1", throwable, "文件或目录压缩为zip时异常", file, entryPath);
            return "error_11";
        }finally {
            IOUtil.close(bufferedInputStream, fileInputStream);
        }
    }

    /**
     * 2025-05-08
     * zip解压
     * @param fromUrlOrPath 待解压文件名
     * @param toUrlOrPath 解压路径
     */
    public static final void unzip(final String fromUrlOrPath, final String toUrlOrPath) {
        ZipInputStream zipInputStream = null;
        try{
            File srcFile = new File(fromUrlOrPath);
            if (!srcFile.exists()) {
                LogUtil.error("CompressUtil_unzip_1", "文件或目录不存在", fromUrlOrPath, toUrlOrPath);
                return;
            }
            zipInputStream = new ZipInputStream(new FileInputStream(srcFile));
            ZipEntry zipEntry = null;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    continue;
                }
                File file = new File(toUrlOrPath, zipEntry.getName());
                if (!file.exists()) {
                    new File(file.getParent()).mkdirs();  // 创建此文件的上级目录
                }
                OutputStream outputStream = new FileOutputStream(file);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                int len = -1;
                byte[] buf = new byte[1024];
                while ((len = zipInputStream.read(buf)) != -1) {
                    bufferedOutputStream.write(buf, 0, len);
                }
                IOUtil.close(bufferedOutputStream, outputStream);
            }
        }catch (Throwable throwable){
            LogUtil.error("CompressUtil_unzip_t1", throwable, "解压失败", fromUrlOrPath, toUrlOrPath);
        }finally {
            IOUtil.close(zipInputStream);
        }
    }

}