package xc.crabapple.core.net.https;

import xc.crabapple.core.encoder.Base64Util;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;

/**
 * 2024-12-18
 * 基于jdk自带的keytool工具开发
 */
public class KeytoolUtil {

    /**
     * 2024-12-18
     * jks文件转为key文件
     * keytool工具执行完下面的指令后，需要把jks文件转为key文件，因为jks文件不能直接被Nginx使用，需要转为key格式（jks是二进制格式，key是文本格式）
     *   keytool -genkeypair -alias hello -keyalg RSA -keysize 2048 -keystore d:/ca2/test.jks -validity 3650
     * @param alias 别名，如：hello
     * @param password 密码，如：123456
     * @param jksUrl jks文件绝对路径，如：d:/ca2/test.jks
     */
    public static final void jks2key(final String alias, final String password, final String jksUrl){
        FileInputStream fileInputStream = null;
        try {
            String jksUrlLocal = UrlUtil.getStandardPath(jksUrl);
            fileInputStream = new FileInputStream(jksUrlLocal);
            final KeyStore keyStore = KeyStore.getInstance("JKS");
            final char[] passwordChars = password.toCharArray();
            keyStore.load(fileInputStream, passwordChars);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, passwordChars);
            final byte[] privateKeyBytes = privateKey.getEncoded();
            final String base64Str = Base64Util.encodeBytes2str(privateKeyBytes);
            final String jksPath = UrlUtil.getPathByUrl(jksUrlLocal);
            FileUtil.dirMake(jksPath);
            final String jksName = UrlUtil.getFileName(jksUrlLocal, false);
            final String keyUrl = jksPath + "/" + jksName + ".key";
            FileUtil.delete(keyUrl);
            StringBuilder sb = new StringBuilder();
            sb.append("-----BEGIN RSA PRIVATE KEY-----\n");
            sb.append(base64Str);
            sb.append("\n-----END RSA PRIVATE KEY-----");
            String content = sb.toString();
            FileUtil.fileWriteNew(keyUrl, content, true);
        } catch (Throwable throwable){
            LogUtil.error("HttpsCA_jks2key_t1", throwable, "jks转key文件失败", alias, jksUrl);
        }finally {
            IOUtil.close(fileInputStream);
        }
    }

}