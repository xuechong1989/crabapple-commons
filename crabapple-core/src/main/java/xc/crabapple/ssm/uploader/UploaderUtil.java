package xc.crabapple.ssm.uploader;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.net.http.ContentTypeUtil;
import xc.crabapple.core.servlet.ServletUtil;
import xc.crabapple.core.url.UrlUtil;
import xc.crabapple.ssm.security.webshell.WebshellUtil;

import java.io.File;
import java.io.InputStream;

/**
 * 2025-01-22
 * 文件上传工具类
 */
public final class UploaderUtil {

    private static boolean p_inited = false;
    private static StandardServletMultipartResolver p_standardServletMultipartResolver;

    public static final void init(final String webshellEnable, final String webshellParamName){
        if(p_inited){
            return;
        }
        p_inited = true;
        //
        p_initStandardServletMultipartResolver();
        WebshellUtil.init(webshellEnable, webshellParamName);
    }

    private static final void p_initStandardServletMultipartResolver(){
        p_standardServletMultipartResolver = new StandardServletMultipartResolver();
    }

    public static final StandardServletMultipartResolver getStandardServletMultipartResolver(){
        return p_standardServletMultipartResolver;
    }

    public static final boolean setMultipartHttpServletRequest(final HttpServletRequest request){
        final String contentType = ContentTypeUtil.getContentType(request);  // 请求类型
        if(StringUtil.isBlank(contentType)){
            return false;
        }
        //
        if(request instanceof MultipartHttpServletRequest){
            MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
            ServletUtil.setContext(multipartHttpServletRequest);
            return true;
        }
        if(StringUtil.startsWith(contentType, ContentTypeUtil.multipart_form_data)){
            MultipartHttpServletRequest multipartHttpServletRequest = UploaderUtil.httpServletRequest2multipartHttpServletRequest(request);
            ServletUtil.setContext(multipartHttpServletRequest);
            return true;
        }
        //
        return false;
    }

    /**
     * 2025-01-22
     * @return
     */
    public static final MultipartHttpServletRequest getMultipartHttpServletRequest(){
        MultipartHttpServletRequest multipartHttpServletRequest = httpServletRequest2multipartHttpServletRequest(null);
        return multipartHttpServletRequest;
    }

    /**
     * 2025-01-22
     * @return
     */
    public static final MultipartHttpServletRequest httpServletRequest2multipartHttpServletRequest(){
        HttpServletRequest request = ServletUtil.getRequest();
        MultipartHttpServletRequest multipartHttpServletRequest = httpServletRequest2multipartHttpServletRequest(request);
        return multipartHttpServletRequest;
    }

    /**
     * 2025-01-22
     * HttpServletRequest对象转换为MultipartHttpServletRequest对象
     * @return
     */
    public static final MultipartHttpServletRequest httpServletRequest2multipartHttpServletRequest(final HttpServletRequest request){
        HttpServletRequest localRequest = request;
        if(localRequest == null){
            localRequest = ServletUtil.getRequest();
        }
        final String contentType = ContentTypeUtil.getContentType(localRequest);  // 请求类型
        if(StringUtil.isBlank(contentType)){
            return null;
        }
        if(!StringUtil.startsWith(contentType, ContentTypeUtil.multipart_form_data)){
            return null;
        }
        if(localRequest instanceof MultipartHttpServletRequest){
            MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) localRequest;
            return multipartHttpServletRequest;
        }
        //
        StandardServletMultipartResolver standardServletMultipartResolver = UploaderUtil.getStandardServletMultipartResolver();
        MultipartHttpServletRequest multipartHttpServletRequest = standardServletMultipartResolver.resolveMultipart(localRequest);
        return multipartHttpServletRequest;
    }

    /**
     * 2023-06-08
     * MultipartFile对象转换为字节数组
     */
    public static final byte[] multipartFile2bytes(final MultipartFile multipartFile){
        if(multipartFile == null){
            return null;
        }
        //
        File file = multipartFile2file(multipartFile);
        byte[] bytes = FileUtil.file2bytes(file);
        return bytes;
    }

    /**
     * 2019-11-25
     * MultipartFile对象转换为InputStream对象
     */
    public static final InputStream multipartFile2inputStream(final MultipartFile multipartFile){
        if(multipartFile == null){
            return null;
        }
        try {
            InputStream inputStream = multipartFile.getInputStream();
            return inputStream;
        }catch(Throwable throwable){
            LogUtil.error("UploaderUtil_multipartFile2inputStream_e1", throwable);
            return null;
        }
    }

    /**
     * 2023-08-30
     * MultipartFile对象转换为File对象
     * @param multipartFile
     * @return
     */
    public static final File multipartFile2file(final MultipartFile multipartFile){
        File file = multipartFile2file(multipartFile, null, null);
        return file;
    }

    /**
     * 2023-08-30
     * MultipartFile对象转换为File对象
     * @param multipartFile
     * @param path
     */
    public static final File multipartFile2file(final MultipartFile multipartFile, final String path){
        File file = multipartFile2file(multipartFile, path, null);
        return file;
    }

    /**
     * 2019-11-25
     * MultipartFile对象转换为File对象
     * @param multipartFile
     * @param path
     * @param filename 文件名，带有后缀名的
     */
    public static final File multipartFile2file(final MultipartFile multipartFile, final String path, final String filename){
        if(multipartFile == null){
            LogUtil.error("UploaderUtil_multipartFile2file_1", "multipartFile对象为空", path, filename);
            return null;
        }
        //
        String localPath = path;
        String localFilename = filename;
        if(StringUtil.isBlank(localPath)){
            localPath = FileUtil.getTempPath();
        }else{
            localPath = UrlUtil.getStandardPath(localPath);
        }
        if(StringUtil.isBlank(localFilename)){
            localFilename = multipartFile.getOriginalFilename();
        }
        //
        final String url = localPath + "/" + localFilename;
        try {
            InputStream inputStream = multipartFile.getInputStream();
            File file = FileUtil.inputstream2file(inputStream, url);
            return file;
        }catch(Throwable throwable){
            LogUtil.error("UploaderUtil_multipartFile2file_t1", throwable, url);
            return null;
        }
    }

    /**
     * 2023-06-08
     * 把MultipartFile对象写入磁盘
     * @param file
     * @param url
     * @return
     */
    public static final String multipartFile2disk(final MultipartFile file, final String url){
        String path = FileUtil.getPathByUrl(url);
        FileUtil.dirMake(path);
        InputStream inputStream = null;
        try{
            if (file == null){
                return null;
            }
            inputStream = file.getInputStream();
            FileUtil.inputstream2disk(inputStream, url, true);
            return url;
        }catch(Throwable throwable){
            LogUtil.error("SysPropertiesServiceImpl_writeFileDisk_t1", throwable, url);
            return null;
        }finally {
            IOUtil.close(inputStream);
        }
    }

}
