package xc.crabapple.core.net.https;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 2026-10-17
 * HttpsUtil 冒烟测试（仅验证对象构建，不涉及网络请求）
 */
public class HttpsUtilTest {

    @Test
    public void defaultHttpClient() {
        assertNotNull(HttpsUtil.getHttpClient());
    }

    @Test
    public void trustAllHttpClient() {
        assertNotNull(HttpsUtil.getTrustAllHttpClient());
    }

    @Test
    public void sslSocketFactory() {
        assertNotNull(HttpsUtil.getSSLSocketFactory());
        assertNotNull(HttpsUtil.getTrustAllSSLSocketFactory());
    }

}
