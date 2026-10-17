package xc.crabapple.core.net.https;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

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