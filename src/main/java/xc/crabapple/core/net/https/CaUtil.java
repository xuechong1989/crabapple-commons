package xc.crabapple.core.net.https;

import jakarta.xml.bind.DatatypeConverter;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 2024-12-19
 * 针对https的CA证书生成与操作类
 * CA证书自签名工具类
 * self-signed certificate
 * 一般的数字证书产品的主题通常含有如下字段：
 * 公用名称 (Common Name) 简称：CN 字段，对于SSL证书，一般为网站域名或IP地址；而对于代码签名证书则为申请单位名称；而对于客户端证书则为证书申请者的姓名；
 * 单位名称 (Organization Name) ：简称：O字段，对于SSL证书，一般为网站域名；而对于代码签名证书则为申请单位名称；而对于客户端单位证书则为证书申请者所在单位名称；
 * 证书申请单位所在地：
 * 所在城市 (Locality) 简称：L字段
 * 所在省份 (State/Provice) 简称：S字段
 * 所在国家 (Country) 简称：C字段，只能是国家字母缩写，如中国：CN
 * 其他一些字段：
 * 电子邮件 (Email) 简称：E字段
 * 多个姓名字段 简称：G字段
 * 介绍：Description字段
 * 电话号码：Phone字段，格式要求 + 国家区号 城市区号 电话号码，如： +86 732 88888888
 * 地址：STREET字段
 * 邮政编码：PostalCode字段
 * 显示其他内容 简称：OU字段
 */
public class CaUtil {

    /**
     * 2024-12-19
     * 创建一系列支持HTTPS的CA证书，如jks、key、cer/crt/pem、p12
     * @param alias 别名，如：hello
     * @param password 密码，如：123456
     * @param days CA证书有效天数，如：3650
     * @param dirName CA证书信息，如：CN=192.168.124.*, OU=dev, O=dev, L=city, ST=province, C=CN
     * @param dnsList 应用服务器（如nginx）所在机器的DNS，如：["223.5.5.5", "114.114.114.114"]
     * @param ipList 应用服务器（如nginx）所在机器的ip地址，如：["192.168.124.22", "127.0.0.1"]
     * @param jksUrl jks文件绝对路径，如：d:/ca2/test.jks
     * @param cerUrl cer/crt/pem文件绝对路径，如：d:/ca2/test.cer
     * @param p12Url p12文件绝对路径，如：d:/ca2/test.p12
     */
    public static final void createSSL(final String alias, final String password, final int days, final String dirName, final List<String> dnsList, final List<String> ipList, final String jksUrl, final String cerUrl, final String p12Url) {
        KeyPair keyPair = p_getKeyPair();
        X509Certificate x509Certificate = p_getX509Certificate(keyPair, days, dirName, dnsList, ipList);
        p_createJksFile(x509Certificate, keyPair, alias, password, jksUrl);
        p_createKeyFile(alias, password, jksUrl);
        p_createCerFile(x509Certificate, cerUrl);
        p_createP12File(x509Certificate, keyPair, alias, password, p12Url);
    }

    /**
     * 2024-12-19
     * 创建keyPair对象
     * @return
     */
    private static final KeyPair p_getKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            return keyPair;
        } catch (Throwable throwable) {
            LogUtil.error("CaUtil_p_getKeyPair_t1", throwable, "创建keyPair对象失败");
            return null;
        }
    }

    /**
     * 2024-12-19
     * 创建x509Certificate对象
     * @param keyPair
     * @param days CA证书有效天数，如：3650
     * @param dirName CA证书信息，如：CN=192.168.124.*, OU=dev, O=dev, L=city, ST=province, C=CN
     * @param dnsList 应用服务器（如nginx）所在机器的DNS，如：["223.5.5.5", "114.114.114.114"]
     * @param ipList 应用服务器（如nginx）所在机器的ip地址，如：["192.168.124.22", "127.0.0.1"]
     * @return
     */
    private static final X509Certificate p_getX509Certificate(final KeyPair keyPair, final int days, final String dirName, final List<String> dnsList, final List<String> ipList) {
        try {
            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();
            JcaContentSignerBuilder jcaContentSignerBuilder = new JcaContentSignerBuilder("SHA256WithRSAEncryption");
            ContentSigner contentSigner = jcaContentSignerBuilder.build(privateKey);
            List<ASN1Encodable> asn1EncodableList = new ArrayList<>();
            if(!CollectionUtil.isBlank(dnsList)){
                for (int i = 0; i < dnsList.size(); i++) {
                    String dns = dnsList.get(i);
                    GeneralName generalName = new GeneralName(GeneralName.dNSName, dns);
                    asn1EncodableList.add(generalName);
                }
            }
            if(!CollectionUtil.isBlank(ipList)){
                for (int i = 0; i < ipList.size(); i++) {
                    String ip = ipList.get(i);
                    GeneralName generalName = new GeneralName(GeneralName.iPAddress, ip);
                    asn1EncodableList.add(generalName);
                }
            }
            int size = asn1EncodableList.size();
            ASN1Encodable[] asn1EncodableArray = new ASN1Encodable[size];
            asn1EncodableList.toArray(asn1EncodableArray);
            DERSequence derSequence = new DERSequence(asn1EncodableArray);
            JcaX509v3CertificateBuilder jcaX509v3CertificateBuilder = new JcaX509v3CertificateBuilder(
                    new X500Name(dirName),
                    BigInteger.valueOf(System.currentTimeMillis()),
                    new Date(),
                    new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days)),
                    new X500Name(dirName),
                    publicKey
            );
            jcaX509v3CertificateBuilder.addExtension(Extension.subjectAlternativeName, true, derSequence);
            X509CertificateHolder x509CertificateHolder = jcaX509v3CertificateBuilder.build(contentSigner);
            JcaX509CertificateConverter jcaX509CertificateConverter = new JcaX509CertificateConverter();
            X509Certificate x509Certificate = jcaX509CertificateConverter.getCertificate(x509CertificateHolder);
            return x509Certificate;
        } catch (Throwable throwable) {
            LogUtil.error("CaUtil_p_getX509Certificate_t1", throwable, "创建x509Certificate对象失败");
            return null;
        }
    }

    /**
     * 2024-12-19
     * 创建jks文件
     * @param x509Certificate
     * @param keyPair
     * @param alias 别名，如：hello
     * @param password 密码，如：123456
     * @param jksUrl jks文件绝对路径，如：d:/ca2/test.jks
     * @return
     */
    private static final void p_createJksFile(final X509Certificate x509Certificate, final KeyPair keyPair, final String alias, final String password, final String jksUrl) {
        FileOutputStream fileOutputStream = null;
        try {
            char[] passwordCharArray = password.toCharArray();
            PrivateKey privateKey = keyPair.getPrivate();
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            Certificate[] certificateArray = new Certificate[]{x509Certificate};
            KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(privateKey, certificateArray);
            KeyStore.PasswordProtection passwordProtection = new KeyStore.PasswordProtection(passwordCharArray);
            keyStore.setEntry(alias, privateKeyEntry, passwordProtection);
            String jksUrlLocal = UrlUtil.getStandardPath(jksUrl);
            String jksPath = UrlUtil.getPathByUrl(jksUrlLocal);
            FileUtil.dirMake(jksPath);
            FileUtil.delete(jksUrlLocal);
            fileOutputStream = new FileOutputStream(jksUrlLocal);
            keyStore.store(fileOutputStream, passwordCharArray);
        } catch (Throwable throwable) {
            LogUtil.error("CaUtil_p_createJksFile_t1", throwable, "创建jks文件失败");
        } finally {
            IOUtil.close(fileOutputStream);
        }
    }

    /**
     * 2024-12-19
     * jks文件转为key文件
     * @param alias 别名，如：hello
     * @param password 密码，如：123456
     * @param jksUrl jks文件绝对路径，如：d:/ca2/test.jks
     */
    private static final void p_createKeyFile(final String alias, final String password, final String jksUrl) {
        KeytoolUtil.jks2key(alias, password, jksUrl);
    }

    /**
     * 2024-12-19
     * 创建cer/crt/pem文件
     * Windows下叫cer，Linux下叫crt
     * 根据操作系统的不同，直接更改后缀名即可，如windows为cer，linux为crt
     * @param x509Certificate
     * @param cerUrl cer/crt/pem文件绝对路径，如：d:/ca2/test.cer
     */
    private static final void p_createCerFile(final X509Certificate x509Certificate, final String cerUrl) {
        FileWriter fileWriter = null;
        try {
            byte[] bytes = x509Certificate.getEncoded();
            String base64Str = DatatypeConverter.printBase64Binary(bytes);
            base64Str = base64Str.replaceAll("(.{64})", "$1\n");
            StringBuilder sb = new StringBuilder();
            sb.append("-----BEGIN CERTIFICATE-----\n");
            sb.append(base64Str);
            sb.append("\n-----END CERTIFICATE-----\n");
            String content = sb.toString();
            String cerUrlLocal = UrlUtil.getStandardPath(cerUrl);
            String cerPath = UrlUtil.getPathByUrl(cerUrlLocal);
            FileUtil.dirMake(cerPath);
            FileUtil.delete(cerUrlLocal);
            fileWriter = new FileWriter(cerUrlLocal);
            fileWriter.write(content);
        } catch (Throwable throwable) {
            LogUtil.error("CaUtil_p_createCerFile_t1", throwable, "创建cer/crt/pem文件失败");
        } finally {
            IOUtil.close(fileWriter);
        }
    }

    /**
     * 2024-12-19
     * 创建p12文件
     * @param x509Certificate
     * @param keyPair
     * @param alias
     * @param password
     * @param p12Url p12文件绝对路径，如：d:/ca2/test.p12
     * @return
     */
    private static final void p_createP12File(final X509Certificate x509Certificate, final KeyPair keyPair, final String alias, final String password, final String p12Url) {
        FileOutputStream fileOutputStream = null;
        try {
            char[] passwordCharArray = password.toCharArray();
            PrivateKey privateKey = keyPair.getPrivate();
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            Certificate[] certificateArray = new Certificate[]{x509Certificate};
            KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(privateKey, certificateArray);
            KeyStore.PasswordProtection passwordProtection = new KeyStore.PasswordProtection(passwordCharArray);
            keyStore.setEntry(alias, privateKeyEntry, passwordProtection);
            String p12UrlLocal = UrlUtil.getStandardPath(p12Url);
            String p12Path = UrlUtil.getPathByUrl(p12UrlLocal);
            FileUtil.dirMake(p12Path);
            FileUtil.delete(p12UrlLocal);
            fileOutputStream = new FileOutputStream(p12UrlLocal);
            keyStore.store(fileOutputStream, passwordCharArray);
        } catch (Throwable throwable) {
            LogUtil.error("CaUtil_p_createP12File_t1", throwable, "创建p12文件失败");
        } finally {
            IOUtil.close(fileOutputStream);
        }
    }

}