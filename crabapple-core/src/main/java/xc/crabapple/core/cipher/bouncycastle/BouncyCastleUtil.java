package xc.crabapple.core.cipher.bouncycastle;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import xc.crabapple.core.log.LogUtil;

import java.security.Provider;
import java.security.Security;

/**
 * 2025-01-16
 */
public final class BouncyCastleUtil {

    private static boolean p_inited = false;
    private static BouncyCastleProvider p_bouncyCastleProvider = null;

    /**
     * 2025-01-16
     * 一个应用只执行一次该方法即可。
     */
    public static final void addProvider(){
        if(p_inited){
            return;
        }
        p_inited = true;
        //
        try{
            p_bouncyCastleProvider = new BouncyCastleProvider();
            Security.addProvider(p_bouncyCastleProvider);
        }catch (Throwable throwable){
            LogUtil.error("BouncyCastleUtil_addProvider_t1", "BouncyCastle添加provider失败", throwable);
            return;
        }
        // 检测
        try{
            Provider provider = Security.getProvider("BC");
            if (provider != null) {
                LogUtil.print("BouncyCastleUtil_addProvider_1", "BouncyCastle已添加provider");
            } else {
                LogUtil.error("BouncyCastleUtil_addProvider_2", "BouncyCastle未添加provider");
            }
        }catch (Throwable throwable){
            LogUtil.error("BouncyCastleUtil_addProvider_t2", "BouncyCastle检测provider是否添加时出现异常", throwable);
        }
    }

    /**
     * 2025-01-16
     */
    public static final BouncyCastleProvider getBouncyCastleProvider(){
        addProvider();
        return p_bouncyCastleProvider;
    }

}