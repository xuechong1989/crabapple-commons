package xc.crabapple.core.net.https;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * 2026-10-17
 * 信任全部证书的X509TrustManager
 * 安全提示：该实现不校验任何证书，存在中间人攻击风险；
 * 仅供"显式开启信任全部证书"的场景使用（内网自签名证书、测试环境等），生产环境请使用JVM默认校验
 */
public final class MyX509TrustManager implements X509TrustManager {

    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType){

    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType){

    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }

}