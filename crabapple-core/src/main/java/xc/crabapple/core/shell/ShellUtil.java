package xc.crabapple.core.shell;

import xc.crabapple.core.device.EnvironmentUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.thread.Business;
import xc.crabapple.core.thread.ThreadUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 2017-05-11 1.0.0
 */
public final class ShellUtil{

    /**
     * 2020-05-25 6.0.0
     */
    public static final void execute(final ShellBean shellBean){
        String targetUrl = shellBean.getTargetUrl();
        String path = UrlUtil.getPathByUrl(targetUrl);
        FileUtil.dirMake(path);
        FileUtil.fileDelete(targetUrl);
        String block = shellBean.getBlock();
        //阻塞式
        if("yes".equals(block)){
            p_execute(shellBean);
            return;
        }
        //非阻塞式
        ThreadUtil.start(new Business("ShellUtil_execute_1", false){
            @Override
            public void process() {
                p_execute(shellBean);
            }
        });
    }

    /**
     * 2020-12-15 7.1.0
     */
    private static final void p_execute(final ShellBean shellBean){
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        Process process = null;
        String cmd = shellBean.getCmd();
        try{
            Runtime runtime = Runtime.getRuntime();
            if(EnvironmentUtil.isWindows()){
                process = runtime.exec(cmd);
            }else if(EnvironmentUtil.isLinux()){
                String[] cmderArray = new String[]{"bash", "-c", cmd};
                process = runtime.exec(cmderArray);
                //2020-12-16 7.1.0 add 等待调用的系统程序完成操作后，当前线程才能做下一步操作，该方法会阻塞当前线程知道调用程序运行结束
                process.waitFor();
                ThreadUtil.sleep(1000L);  //进一步确保系统程序执行完成
            }
            //
//            inputStream = process.getInputStream();
//            int length = IOUtil.getInputstreamLength(inputStream);
//            if(length <= 0){
//                inputStream = process.getErrorStream();
//                File file = FileUtil.inputstream2file(inputStream);
//                String url = file.getAbsolutePath();
//                url = UrlUtil.getStandardPath(url);
//                String text = FileUtil.fileReadText(url);
//                if(StringUtil.isBlank(text)){
//                    return;
//                }
//                if(!StringUtil.containOf(text, "[Warning] Using a password")){
//                    LogUtil.warn("xc_crabapple_ShellUtil_execute_1", "输入流长度为0，原因详见" + url, cmd, text);
//                    return;
//                }
//                return;
//            }
            //
        }catch(Throwable throwable){
            LogUtil.error("ShellUtil_execute_t1", throwable, cmd);
        }finally{
            if(process != null){
                process.destroy();
            }
            IOUtil.close(bufferedReader, inputStreamReader, inputStream);
        }
    }

}
