package xc.crabapple.ssm.env;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.tomcat.TomcatUtil;

/**
 * 2025-01-27
 * 环境信息工具类
 */
public class EnvUtil {

    private static boolean p_idea = false;
    private static String p_env = "prod";

    /**
     * 2023-10-31 当前项目的运行环境
     */
    public static final void init(final String devRootPath, final String env) {
        p_setIdea(devRootPath);
        String localEnv = env;
        if(StringUtil.isBlank(localEnv)){
            localEnv = "prod";
        }
        p_env = StringUtil.getPureStr(localEnv);
        p_env = StringUtil.trim(p_env);
    }

    /**
     * 2024-07-15
     * @param devRootPath 开发环境相应目录，如：d:/app/
     * @return
     */
    private static final void p_setIdea(final String devRootPath){
        String localDevRootPath = devRootPath;
        if(StringUtil.isBlank(localDevRootPath)){
            localDevRootPath = "d:/app/";
        }
        final String tomcatRootPath = TomcatUtil.getTomcatRootPath();
        if(StringUtil.startsWith(tomcatRootPath, localDevRootPath)){
            p_idea = true;
        }else{
            p_idea = false;
        }
    }

    public static final boolean idea(){
        return p_idea;
    }

    public static final String env(){
        return p_env;
    }

}