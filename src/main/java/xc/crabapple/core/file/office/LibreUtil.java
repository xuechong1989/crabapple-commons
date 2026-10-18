package xc.crabapple.core.file.office;

import org.apache.commons.io.IOUtils;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.DefaultOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.ExternalOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeManager;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.FileEncoderUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 2017-09-18 1.0.0
 * 2018-02-06 多端口集群形式
 *   2024-10-21 添加workDir配置
 * LibreOffice工具类
 */
public final class LibreUtil{

    private static final Map<String, OfficeDocumentConverter> map = new HashMap<>();
    private static Integer startPort = 0;  //起始端口
    private static Integer count = 1;  //端口数量

    /**
     * 2018-02-06 1.0.0
     *   2024-10-21 添加workDir配置
     * @param officehome
     * @param ports
     * @param workDir 工作目录 解决"无法启动该应用程序。
     *                [context="user"]caught unexpected com.sun.star.ucb.interactiveAugmentedIOException:a folder could not be created"
     * @return
     */
    public synchronized static final Integer init(final String officehome, final List<String> ports, final String workDir){
        if(StringUtil.isBlank(officehome)){
            LogUtil.error("LibreUtil_init_1", "officehome为空，libreoffice启动失败");
            return 1;
        }
        if(CollectionUtil.isBlank(ports)){
            LogUtil.error("LibreUtil_init_2", "ports为空");
            return 1;
        }
        count = ports.size();
        boolean isInit;
        for(int i = 0; i < count; i++){
            String ps = ports.get(i);
            int port = Integer.parseInt(ps);
            if(i == 0){
                isInit = true;
            }else{
                isInit = false;
            }
            Integer result = init(officehome, port, isInit, workDir);
            if(result != 0){
                return result;
            }
        }
        return 0;
    }

    /**
     * 2018-02-06 1.0.0
     *   2024-10-21 添加workDir配置
     * @param officehome
     * @param port
     * @param workDir 工作目录 解决"无法启动该应用程序。 [context="user"]caught unexpected com.sun.star.ucb.interactiveAugmentedIOException:a folder could not be created"
     * @return
     */
    public static final Integer init(final String officehome, final Integer port, final String workDir){
        Integer result = init(officehome, port, true, workDir);
        return result;
    }

    /**
     * 2018-02-06 1.0.0
     *   2024-10-21 添加workDir配置
     * @param officehome
     * @param port
     * @param isInit
     * @param workDir 工作目录 解决"无法启动该应用程序。 [context="user"]caught unexpected com.sun.star.ucb.interactiveAugmentedIOException:a folder could not be created"
     * @return
     */
    private static final Integer init(final String officehome, final Integer port, final Boolean isInit, final String workDir){
        if(isInit){
            startPort = port;
        }
        if(StringUtil.isBlank(officehome)){
            LogUtil.error("LibreUtil_init_11", "officehome为空，libreoffice启动失败");
            return 1;
        }
        if(port == 0){
            LogUtil.error("LibreUtil_init_12", "端口号为零, libreoffice启动失败");
            return 1;
        }
        if (StringUtil.isBlank(workDir)){
            LogUtil.error("LibreUtil_init_13", "工作目录workDir为空，libreoffice启动失败");
            return null;
        }
        //
        OfficeManager officeManager = p_officeManagerStart(officehome, port,workDir);
        if(officeManager == null){
            LogUtil.error("LibreUtil_init_14", "libreOffice启动失败", officehome, port);
            return 1;
        }
        OfficeDocumentConverter officeDocumentConverter = new OfficeDocumentConverter(officeManager);
        String key = "libre_" + port;
        map.put(key, officeDocumentConverter);
        LogUtil.print("LibreUtil_init_15", "libreOffice启动成功", officehome, port, workDir);
        return 0;
    }

    /**
     * 2018-12-12 1.0.1
     * 2024-10-21 java启动libreoffice失败，可以使用命令单独启动已经安装的libreoffice
     * 进入到安装的目录：cd /opt/libreoffice5.0/program
     * linux执行：./soffice --headless --accept="socket,host=localhost,port=2002;urp;" --nofirststartwizard &
     * windows执行：soffice.exe --headless --accept="socket,host=localhost,port=2002;urp;" --nofirststartwizard &
     * @param workDir 工作目录 解决"无法启动该应用程序。 [context="user"]caught unexpected com.sun.star.ucb.interactiveAugmentedIOException:a folder could not be created"
     * @return
     */
    private static final OfficeManager p_officeManagerStart(final String officehome, final Integer port, final String workDir){
        try{
            // 首先连接外部已启动的端口
            ExternalOfficeManagerConfiguration externalProcessOfficeManager = new ExternalOfficeManagerConfiguration();
            externalProcessOfficeManager.setConnectOnStart(true);
            externalProcessOfficeManager.setPortNumber(port);
            OfficeManager officeManager = externalProcessOfficeManager.buildOfficeManager();
            officeManager.start();
            return officeManager;
        }catch(Throwable throwable){
            LogUtil.print("LibreUtil_p_officeManagerStart_1", "没有已启动的服务端口, 即将通过java程序启动libreoffice", officehome, port);
            // 通过java程序启动libreoffice
            // 2024-10-21 添加工作目录 解决"无法启动该应用程序。 [context="user"]caught unexpected com.sun.star.ucb.interactiveAugmentedIOException:a folder could not be created"
            String workDirLocal = UrlUtil.getStandardPath(workDir);
            if (StringUtil.isBlank(workDirLocal)){
                LogUtil.error("LibreUtil_p_officeManagerStart_2", "工作目录workDir为空，liberoffice启动失败！");
                return null;
            }
            Boolean flag = FileUtil.dirMake(workDirLocal);
            if (!flag){
                LogUtil.error("LibreUtil_p_officeManagerStart_3", "工作目录workDir创建失败，liberoffice启动失败！");
                return null;
            }
            //
            try{
                DefaultOfficeManagerConfiguration config = new DefaultOfficeManagerConfiguration();
                config.setTaskExecutionTimeout(3600000L);
                config.setOfficeHome(officehome);
                config.setPortNumber(port);
                // 2024-10-21 添加工作目录 解决"无法启动该应用程序。 [context="user"]caught unexpected com.sun.star.ucb.interactiveAugmentedIOException:a folder could not be created"
                File workDirFile = new File(workDirLocal);
                config.setWorkDir(workDirFile);
                //
                OfficeManager officeManager = config.buildOfficeManager();
                officeManager.start();
                return officeManager;
            }catch(Throwable throwable2){
                LogUtil.error("LibreUtil_p_officeManagerStart_t2", throwable2, officehome, port, workDirLocal);
                return null;
            }
        }
    }

    /**
     * 2018-02-06 1.0.0
     * @param from 源文件绝对路径，如：/clouddb/tmp/hello.pptx
     * @param to 转换后的文件绝对路径，如：/clouddb/tmp/hello.pdf
     */
    public static final void convert(final String from, final String to) throws Throwable{
        File fileFrom = new File(from);
        File fileTo = new File(to);
        convert(fileFrom, fileTo);
    }

    /**
     * 2017-09-18 1.0.0
     * @param fileFrom
     * @param fileTo
     */
    public static final void convert(final File fileFrom, final File fileTo) throws Throwable{
        final String fileType = FileUtil.getFileType(fileFrom);
        boolean flag = OfficeUtil.isOfficeByFileType(fileType);
        if(flag){
            convert2pdf(fileFrom, fileTo);
            return;
        }
        if("txt".equals(fileType)){
            convert2txt(fileFrom, fileTo);
            return;
        }
        //
        LogUtil.error("LibreUtil_convert_1", fileFrom, fileTo, fileType, flag);
    }

    /**
     * 2017-09-18 1.0.0
     * @param from 源文件绝对路径，如：/clouddb/tmp/hello.pptx
     * @param to 转换后的文件绝对路径，如：/clouddb/tmp/hello.pdf
     */
    public static final void convert2pdf(final String from, final String to) throws Throwable{
        File fileFrom = new File(from);
        File fileTo = new File(to);
        convert2pdf(fileFrom, fileTo);
    }

    /**
     * 2017-09-18 1.0.0
     * @param fileFrom
     * @param fileTo
     */
    public static final void convert2pdf(final File fileFrom, final File fileTo){
        OfficeDocumentConverter converter = getConverter();
        converter.convert(fileFrom, fileTo);
    }

    /**
     * 2017-09-18 1.0.0
     * @param from
     * @param to
     */
    public static final void convert2txt(final String from, final String to){
        File fileFrom = new File(from);
        File fileTo = new File(to);
        convert2txt(fileFrom, fileTo);
    }

    /**
     * 2017-09-18 1.0.0
     * @param fileFrom
     * @param fileTo
     */
    public static final void convert2txt(final File fileFrom, final File fileTo){
        InputStream inputStream = null;
        OutputStream outputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try{
            inputStream = new FileInputStream(fileFrom);
            outputStream = new FileOutputStream(fileTo);
            IOUtils.write("<!doctype html>\r\n<html>\r\n<head><meta charset=\""+"UTF-8"+"\">\r\n</head>\r\n<body>\r\n", outputStream, "UTF-8");
            String encode = FileEncoderUtil.getEncoderType(fileFrom);
            inputStreamReader = new InputStreamReader(inputStream, encode);
            bufferedReader = new BufferedReader(inputStreamReader);
            String str;
            while((str = bufferedReader.readLine()) != null){
                String s = str.replaceAll(" ", "&nbsp;");
                IOUtils.write(s, outputStream, "UTF-8");
                IOUtils.write("</br>\r\n", outputStream, "UTF-8");
            }
            IOUtils.write("</body></html>", outputStream, "UTF-8");
        }catch(Throwable throwable){
            LogUtil.error("LibreUtil_convert2txt_1", throwable);
        }finally{
            IOUtil.close(bufferedReader, inputStreamReader, outputStream, inputStream);
        }
    }

    /**
     * 2017-09-18 1.0.0
     * @return
     */
    private static final OfficeDocumentConverter getConverter(){
        int port = RandomUtil.getInt(startPort, startPort + count - 1);
        String key = "libre_" + port;
        OfficeDocumentConverter converter = map.get(key);
        return converter;
    }

}