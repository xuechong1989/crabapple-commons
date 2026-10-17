package xc.crabapple.ssm.security.webshell;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.UriEncoderUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.servlet.ServletUtil;
import xc.crabapple.ssm.spring.SsmRequestWrapper;

/**
 * 2025-01-22
 * webshell攻击工具类
 */
public final class WebshellUtil {

    private static Map<String, String> p_fileTypeMappingMap;  // 文件上传接口，类型校验
    private static String p_enable = "no";
    private static String p_paramName = "targetScope";

    public static final void init(final String enbale, final String paramName){
        p_initMap();
        if(!StringUtil.isBlank(enbale)){
            p_enable = enbale;
        }
        if(!StringUtil.isBlank(paramName)){
            p_paramName = paramName;
        }
    }

    private static void p_initMap(){
        p_fileTypeMappingMap = new HashMap<>(64);
        p_fileTypeMappingMap.put("1","zip");
        p_fileTypeMappingMap.put("2","xls");
        p_fileTypeMappingMap.put("3","xlsx");
        p_fileTypeMappingMap.put("4","bpmn");
        p_fileTypeMappingMap.put("5","jpg");
        p_fileTypeMappingMap.put("6","jpeg");
        p_fileTypeMappingMap.put("7","gif");
        p_fileTypeMappingMap.put("8","png");
        p_fileTypeMappingMap.put("9","bmp");
        p_fileTypeMappingMap.put("10","docx");
        p_fileTypeMappingMap.put("11","doc");
        p_fileTypeMappingMap.put("12","mp4");
        p_fileTypeMappingMap.put("13","pdf");
        p_fileTypeMappingMap.put("14","dwg");
        p_fileTypeMappingMap.put("15","dxf");
        p_fileTypeMappingMap.put("16","ppt");
        p_fileTypeMappingMap.put("17","pptx");
        p_fileTypeMappingMap.put("18","mp3");
        p_fileTypeMappingMap.put("19","wma");
        p_fileTypeMappingMap.put("20","wav");
        p_fileTypeMappingMap.put("21","amr");
        p_fileTypeMappingMap.put("22","eml");
        p_fileTypeMappingMap.put("23","rar");
        p_fileTypeMappingMap.put("24","xbm");
        p_fileTypeMappingMap.put("25","tif");
        p_fileTypeMappingMap.put("26","jfif");
        p_fileTypeMappingMap.put("27","ico");
        p_fileTypeMappingMap.put("28","tiff");
        p_fileTypeMappingMap.put("29","svg");
        p_fileTypeMappingMap.put("30","svgz");
        p_fileTypeMappingMap.put("31","webp");
        p_fileTypeMappingMap.put("32","pjp");
        p_fileTypeMappingMap.put("33","apng");
        p_fileTypeMappingMap.put("34","pjpeg");
        p_fileTypeMappingMap.put("35","avif");
        p_fileTypeMappingMap.put("36","jasper");
        p_fileTypeMappingMap.put("37","pdfA");
        p_fileTypeMappingMap.put("38","xml");
        p_fileTypeMappingMap.put("39","rvt");
    }

    /**
     * 2025-01-22
     * @return
     */
    public static final Map<String, String> getFileTypeMappingMap(){
        return p_fileTypeMappingMap;
    }

    /**
     * 2025-01-22
     * 对p_fileTypeMappingMap的key与value互换位置
     * @return
     */
    public static final Map<String, String> revertFileTypeMappingMap(){
        Set<Map.Entry<String, String>> mapSet = p_fileTypeMappingMap.entrySet();
        Map<String, String> fileTypeMappingMap2 = new HashMap<>();
        for (Map.Entry<String, String> map : mapSet){
            String key = map.getKey();
            String value = map.getValue();
            fileTypeMappingMap2.put(value,key);
        }
        return fileTypeMappingMap2;
    }

    public static final String checkFileType(){
        final HttpServletRequest request = ServletUtil.getRequest();
        //
        if(request instanceof SsmRequestWrapper){
            SsmRequestWrapper myRequestWrapper = (SsmRequestWrapper) request;
            final String isUploader = myRequestWrapper.getUploader();
            if("yes".equals(isUploader)){
                final MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
                final String result = checkFileType(multipartHttpServletRequest);
                return result;
            }
            return "success";
        }
        //
        if(request instanceof MultipartHttpServletRequest){
            final MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
            final String result = checkFileType(multipartHttpServletRequest);
            return result;
        }
        //
        return "success";
    }

    /**
     * 2025-01-22
     * 校验文件类型，只有上传指定类型的文件才没有webshell风险
     * @param multipartHttpServletRequest
     * @return
     */
    public static final String checkFileType(final MultipartHttpServletRequest multipartHttpServletRequest){
        if(!"yes".equals(p_enable)){
            return "success";
        }
        //
        if (multipartHttpServletRequest == null){
            LogUtil.error("MultipartFileUtil_checkFileUpdateType_1","文件上传对象为空");
            return "webshell_error_1";
        }
        //
        String targetScopeStr = multipartHttpServletRequest.getParameter(p_paramName);
        targetScopeStr = UriEncoderUtil.decodeURIComponent(targetScopeStr, true);
        if (StringUtil.isBlank(targetScopeStr)){
            return "webshell_error_2";
        }
        List<String> list = StringUtil.splitList(targetScopeStr, ",");
        if (CollectionUtil.isBlank(list)){
            return "webshell_error_3";
        }
        List<String> fileTypeMappingList = new ArrayList<>();
        for (String str : list){
            String realType = p_fileTypeMappingMap.get(str);
            if (StringUtil.isBlank(realType)){
                continue;
            }
            fileTypeMappingList.add(realType);
        }
        if (CollectionUtil.isBlank(fileTypeMappingList)) {
            LogUtil.error("MultipartFileUtil_checkFileUpdateType_2","缺少文件类型校验参数");
            return "webshell_error_4";
        }
        Map<String, MultipartFile> fileMap = multipartHttpServletRequest.getFileMap();
        if (CollectionUtil.isBlank(fileMap)){
            LogUtil.error("MultipartFileUtil_checkFileUpdateType_3","没有获取到上传文件");
            return "webshell_error_5";
        }
        Set<Map.Entry<String, MultipartFile>> entries = fileMap.entrySet();
        List<MultipartFile> multipartFileList = new ArrayList<>();
        for (Map.Entry<String, MultipartFile> map :entries){
            MultipartFile multipartFile = map.getValue();
            if (multipartFile == null){
                continue;
            }
            multipartFileList.add(multipartFile);
        }
        if (CollectionUtil.isBlank(multipartFileList)){
            LogUtil.error("MultipartFileUtil_checkFileUpdateType_4","没有获取到上传文件");
            return "webshell_error_6";
        }
        for (MultipartFile multipartFile : multipartFileList){
            if(multipartFile == null){
                continue;
            }
            final String originalFilename = multipartFile.getOriginalFilename();
            final String fileType = FileUtil.getFileType(originalFilename);
            for(int i = 0; i < fileTypeMappingList.size(); i++){
                final String mappingFileType = fileTypeMappingList.get(i);
                if(StringUtil.equals(fileType, mappingFileType)){
                    return "success";
                }
            }
            return "webshell_error_7";
        }
        //
        return "success";
    }

}