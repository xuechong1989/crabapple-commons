package xc.crabapple.core.cipher.sm2;

import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithID;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Hex;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.cipher.bouncycastle.BouncyCastleUtil;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * 2025-01-16
 * 国密2，sm2加解密
 */
public final class Sm2Util {

    /**
     * 2025-01-16
     * 生成密钥对，16位ASCII字符串
     */
    public static final Map<String, String> generateKeyMap() {
        BouncyCastleUtil.addProvider();
        AsymmetricCipherKeyPair asymmetricCipherKeyPair = p_getAsymmetricCipherKeyPair();
        //提取公钥点
        ECPoint ecPoint = ((ECPublicKeyParameters) asymmetricCipherKeyPair.getPublic()).getQ();
        //公钥前面的02或者03表示是压缩公钥,04表示未压缩公钥,04的时候,可以去掉前面的04
        String pubKey = Hex.toHexString(ecPoint.getEncoded(false));
        BigInteger privateKey = ((ECPrivateKeyParameters) asymmetricCipherKeyPair.getPrivate()).getD();
        String priKey = privateKey.toString(16);
        Map<String, String> keyMap = new HashMap<>(2);
        keyMap.put("publicKey", pubKey);
        keyMap.put("privateKey", priKey);
        return keyMap;
    }

    /**
     * 生成SM2公私钥对
     */
    private static final AsymmetricCipherKeyPair p_getAsymmetricCipherKeyPair() {
        BouncyCastleUtil.addProvider();
        // 获取一条SM2曲线参数
        final String ecParamSpecName = "sm2p256v1";  // 椭圆曲线参数规范名称
        X9ECParameters sm2ECParameters = GMNamedCurves.getByName(ecParamSpecName);
        // 构造domain参数
        ECCurve ecCurve = sm2ECParameters.getCurve();
        ECPoint ecPoint = sm2ECParameters.getG();
        BigInteger bigInteger = sm2ECParameters.getN();
        ECDomainParameters domainParameters = new ECDomainParameters(ecCurve, ecPoint, bigInteger);
        // 1.创建密钥生成器
        ECKeyPairGenerator keyPairGenerator = new ECKeyPairGenerator();
        // 2.初始化生成器,带上随机数
        try {
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            keyPairGenerator.init(new ECKeyGenerationParameters(domainParameters, secureRandom));
        }catch(Throwable throwable){
            LogUtil.error("Sm2Util_genKeyPair0_t1", throwable);
            return null;
        }
        // 3.生成密钥对
        AsymmetricCipherKeyPair asymmetricCipherKeyPair = keyPairGenerator.generateKeyPair();
        return asymmetricCipherKeyPair;
    }

    public static final byte[] encrypt(final String publicKey, final byte[] plaintextBytes) {
        byte[] cipherBytes = encrypt(publicKey, plaintextBytes, "C1C3C2");
        return cipherBytes;
    }

    /**
     * SM2加密算法
     * @param publicKey 公钥
     * @param plaintextBytes 数据
     * @param mode SM2算法工作模式，可选值：C1C2C3 / C1C3C2
     */
    public static final byte[] encrypt(final String publicKey, final byte[] plaintextBytes, final String mode) {
        BouncyCastleUtil.addProvider();
        if (!p_publicKeyCheck(publicKey)) {
            return null;
        }
        if (plaintextBytes.length == 0) {
            return null;
        }
        //
        String effectiveMode = StringUtil.toUpperCase(mode);
        SM2Engine.Mode sm2engineMode = SM2Engine.Mode.C1C3C2;
        if("C1C3C2".equals(effectiveMode)){
            sm2engineMode = SM2Engine.Mode.C1C3C2;
        }else if("C1C2C3".equals(effectiveMode)){
            sm2engineMode = SM2Engine.Mode.C1C2C3;
        }
        // 获取一条SM2曲线参数
        final String ecParamSpecName = "sm2p256v1";  // 椭圆曲线参数规范名称
        X9ECParameters sm2ECParameters = GMNamedCurves.getByName(ecParamSpecName);
        // 构造domain参数
        ECDomainParameters domainParameters = new ECDomainParameters(sm2ECParameters.getCurve(), sm2ECParameters.getG(), sm2ECParameters.getN());
        //提取公钥点
        ECPoint pukPoint = sm2ECParameters.getCurve().decodePoint(Hex.decode(publicKey));
        // 公钥前面的02或者03表示是压缩公钥，04表示未压缩公钥, 04的时候，可以去掉前面的04
        ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(pukPoint, domainParameters);
        SM2Engine sm2Engine = new SM2Engine(sm2engineMode);
        sm2Engine.init(true, new ParametersWithRandom(publicKeyParameters, new SecureRandom()));
        try {
            byte[] cipherBytes = sm2Engine.processBlock(plaintextBytes, 0, plaintextBytes.length);
            return cipherBytes;
        }catch(Throwable throwable){
            LogUtil.error("Sm2Util_encrypt_t1", throwable);
            return null;
        }
    }

    public static final String encrypt(final String publicKey, final String plaintextStr) {
        String cipherStr = encrypt(publicKey, plaintextStr, "C1C3C2");
        return cipherStr;
    }

    /**
     * SM2加密算法
     * @param publicKey 公钥
     * @param plaintextStr
     * @param mode SM2算法工作模式，可选值：C1C2C3 / C1C3C2
     */
    public static final String encrypt(final String publicKey, final String plaintextStr, final String mode) {
        BouncyCastleUtil.addProvider();
        if (!p_publicKeyCheck(publicKey)) {
            return null;
        }
        if (StringUtil.isBlank(plaintextStr)) {
            return null;
        }
        //
        String effectiveMode = StringUtil.toUpperCase(mode);
        SM2Engine.Mode sm2engineMode = SM2Engine.Mode.C1C3C2;
        if("C1C3C2".equals(effectiveMode)){
            sm2engineMode = SM2Engine.Mode.C1C3C2;
        }else if("C1C2C3".equals(effectiveMode)){
            sm2engineMode = SM2Engine.Mode.C1C2C3;
        }
        // 获取一条SM2曲线参数
        final String ecParamSpecName = "sm2p256v1";  // 椭圆曲线参数规范名称
        X9ECParameters sm2ECParameters = GMNamedCurves.getByName(ecParamSpecName);
        // 构造domain参数
        ECDomainParameters domainParameters = new ECDomainParameters(sm2ECParameters.getCurve(), sm2ECParameters.getG(), sm2ECParameters.getN());
        //提取公钥点
        ECPoint pukPoint = sm2ECParameters.getCurve().decodePoint(Hex.decode(publicKey));
        // 公钥前面的02或者03表示是压缩公钥，04表示未压缩公钥, 04的时候，可以去掉前面的04
        ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(pukPoint, domainParameters);
        SM2Engine sm2Engine = new SM2Engine(sm2engineMode);
        sm2Engine.init(true, new ParametersWithRandom(publicKeyParameters, new SecureRandom()));
        try {
            byte[] inBytes = plaintextStr.getBytes("utf-8");
            byte[] arrayOfBytes = sm2Engine.processBlock(inBytes, 0, inBytes.length);
            String cipherStr = Hex.toHexString(arrayOfBytes);
            return cipherStr;
        }catch(Throwable throwable){
            LogUtil.error("Sm2Util_encrypt_t2", throwable);
            return null;
        }
    }

    public static final String decrypt(final String privateKey, final String cipherStr) {
        String plaintextStr = decrypt(privateKey, cipherStr, "C1C3C2");
        return plaintextStr;
    }

    /**
     * SM2解密算法
     * @param privateKey 私钥
     * @param cipherStr 密文数据
     * @param mode SM2算法工作模式，可选值：C1C2C3 / C1C3C2
     */
    public static final String decrypt(final String privateKey, final String cipherStr, final String mode) {
        BouncyCastleUtil.addProvider();
        boolean flag = p_privateKeyCheck(privateKey);
        if (!flag) {
            return null;
        }
        if (StringUtil.isBlank(cipherStr)) {
            return null;
        }
        //
        String effectiveMode = StringUtil.toUpperCase(mode);
        SM2Engine.Mode sm2engineMode = SM2Engine.Mode.C1C3C2;
        if("C1C3C2".equals(effectiveMode)){
            sm2engineMode = SM2Engine.Mode.C1C3C2;
        }else if("C1C2C3".equals(effectiveMode)){
            sm2engineMode = SM2Engine.Mode.C1C2C3;
        }
        byte[] cipherDataBytes = Hex.decode(cipherStr);
        // 获取一条SM2曲线参数
        final String ecParamSpecName = "sm2p256v1";  // 椭圆曲线参数规范名称
        X9ECParameters sm2ECParameters = GMNamedCurves.getByName(ecParamSpecName);
        // 构造domain参数
        ECDomainParameters domainParameters = new ECDomainParameters(sm2ECParameters.getCurve(), sm2ECParameters.getG(), sm2ECParameters.getN());
        BigInteger privateKeyD = new BigInteger(privateKey, 16);
        ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(privateKeyD, domainParameters);
        SM2Engine sm2Engine = new SM2Engine(sm2engineMode);
        sm2Engine.init(false, privateKeyParameters);
        try {
            byte[] arrayOfBytes = sm2Engine.processBlock(cipherDataBytes, 0, cipherDataBytes.length);
            String plaintextStr = new String(arrayOfBytes, "utf-8");
            return plaintextStr;
        }catch(Throwable throwable){
            LogUtil.error("Sm2Util_decrypt_t1", throwable);
            return null;
        }
    }

    public static final byte[] decrypt(final String privateKey, final byte[] cipherBytes) {
        byte[] plaintextBytes = decrypt(privateKey, cipherBytes, "C1C3C2");
        return plaintextBytes;
    }

    /**
     * SM2解密算法
     * @param privateKey 私钥
     * @param cipherBytes 密文数据
     * @param mode SM2算法工作模式，可选值：C1C2C3 / C1C3C2
     */
    public static final byte[] decrypt(final String privateKey, final byte[] cipherBytes, final String mode) {
        BouncyCastleUtil.addProvider();
        if (!p_privateKeyCheck(privateKey)) {
            return null;
        }
        if (CollectionUtil.isBlank(cipherBytes)){
            return null;
        }
        //
        String effectiveMode = StringUtil.toUpperCase(mode);
        SM2Engine.Mode sm2engineMode = SM2Engine.Mode.C1C3C2;
        if("C1C3C2".equals(effectiveMode)){
            sm2engineMode = SM2Engine.Mode.C1C3C2;
        }else if("C1C2C3".equals(effectiveMode)){
            sm2engineMode = SM2Engine.Mode.C1C2C3;
        }
        // 获取一条SM2曲线参数
        final String ecParamSpecName = "sm2p256v1";  // 椭圆曲线参数规范名称
        X9ECParameters sm2ECParameters = GMNamedCurves.getByName(ecParamSpecName);
        // 构造domain参数
        ECDomainParameters domainParameters = new ECDomainParameters(sm2ECParameters.getCurve(), sm2ECParameters.getG(), sm2ECParameters.getN());
        BigInteger privateKeyD = new BigInteger(privateKey, 16);
        ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(privateKeyD, domainParameters);
        SM2Engine sm2Engine = new SM2Engine(sm2engineMode);
        sm2Engine.init(false, privateKeyParameters);
        try {
            byte[] plaintextBytes = sm2Engine.processBlock(cipherBytes, 0, cipherBytes.length);
            return plaintextBytes;
        }catch(Throwable throwable){
            LogUtil.error("Sm2Util_decrypt_t2", throwable);
            return null;
        }
    }

    /**
     * 私钥签名
     * @param privateKey 私钥
     * @param content    待签名内容
     */
    public static final String sign(final String privateKey, final String content) {
        BouncyCastleUtil.addProvider();
        if (!p_privateKeyCheck(privateKey)) {
            return null;
        }
        if (StringUtil.isBlank(content)) {
            return null;
        }
        final String parameterId = "1234567812345678";  // 签名ID，默认为1234567812345678
        // 待签名内容转为字节数组
        byte[] message = Hex.decode(content);
        // 获取一条SM2曲线参数
        final String ecParamSpecName = "sm2p256v1";  // 椭圆曲线参数规范名称
        X9ECParameters sm2ECParameters = GMNamedCurves.getByName(ecParamSpecName);
        // 构造domain参数
        ECDomainParameters domainParameters = new ECDomainParameters(sm2ECParameters.getCurve(), sm2ECParameters.getG(), sm2ECParameters.getN());
        BigInteger privateKeyD = new BigInteger(privateKey, 16);
        ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(privateKeyD, domainParameters);
        // 创建签名实例
        SM2Signer sm2Signer = new SM2Signer();
        // 初始化签名实例,带上ID,国密的要求
        try {
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            ParametersWithRandom parametersWithRandom = new ParametersWithRandom(privateKeyParameters, secureRandom);
            byte[] bytes = Strings.toByteArray(parameterId);
            ParametersWithID parametersWithID = new ParametersWithID(parametersWithRandom, bytes);
            sm2Signer.init(true, parametersWithID);
            sm2Signer.update(message, 0, message.length);
            //生成签名,签名分为两部分r和s,分别对应索引0和1的数组
            byte[] signBytes = sm2Signer.generateSignature();
            String sign = Hex.toHexString(signBytes);
            return sign;
        }catch(Throwable throwable){
            LogUtil.error("Sm2Util_sign_t1", throwable);
            return null;
        }
    }

    /**
     * 验证签名
     * @param publicKey 公钥
     * @param content 待签名内容
     * @param sign 签名值
     * @return
     */
    public static final boolean verify(final String publicKey, final String content, final String sign) {
        BouncyCastleUtil.addProvider();
        if (!p_publicKeyCheck(publicKey)) {
            return false;
        }
        if (StringUtil.isBlank(content)) {
            return false;
        }
        final String parameterId = "1234567812345678";  // 签名ID，默认为1234567812345678
        // 待签名内容
        byte[] message = Hex.decode(content);
        byte[] signData = Hex.decode(sign);
        // 获取一条SM2曲线参数
        final String ecParamSpecName = "sm2p256v1";  // 椭圆曲线参数规范名称
        X9ECParameters sm2ECParameters = GMNamedCurves.getByName(ecParamSpecName);
        // 构造domain参数
        ECDomainParameters domainParameters = new ECDomainParameters(sm2ECParameters.getCurve(), sm2ECParameters.getG(), sm2ECParameters.getN());
        //提取公钥点
        ECPoint pukPoint = sm2ECParameters.getCurve().decodePoint(Hex.decode(publicKey));
        // 公钥前面的02或者03表示是压缩公钥，04表示未压缩公钥, 04的时候，可以去掉前面的04
        ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(pukPoint, domainParameters);
        //创建签名实例
        SM2Signer sm2Signer = new SM2Signer();
        byte[] bytes = Strings.toByteArray(parameterId);
        ParametersWithID parametersWithID = new ParametersWithID(publicKeyParameters, bytes);
        sm2Signer.init(false, parametersWithID);
        sm2Signer.update(message, 0, message.length);
        //验证签名结果
        boolean verify = sm2Signer.verifySignature(signData);
        return verify;
    }

    /**
     * 将未压缩公钥压缩成压缩公钥
     * 公钥前面的02或者03表示是压缩公钥，04表示未压缩公钥, 04的时候，可以去掉前面的04
     * @param publicKey 未压缩公钥(16进制,不要带头部04)
     */
    private static final String compressPublicKey(final String publicKey) {
        BouncyCastleUtil.addProvider();
        String effectivePublicKey = "04" + publicKey;    //将未压缩公钥加上未压缩标识.
        // 获取一条SM2曲线参数
        final String ecParamSpecName = "sm2p256v1";  // 椭圆曲线参数规范名称
        X9ECParameters sm2ECParameters = GMNamedCurves.getByName(ecParamSpecName);
        // 提取公钥点
        ECPoint pukPoint = sm2ECParameters.getCurve().decodePoint(Hex.decode(effectivePublicKey));
        String compressPubKey = Hex.toHexString(pukPoint.getEncoded(Boolean.TRUE));
        return compressPubKey;
    }

    /**
     * 将压缩的公钥解压为非压缩公钥
     * @param compressKey 压缩公钥
     */
    private static final String unCompressPublicKey(final String compressKey) {
        BouncyCastleUtil.addProvider();
        // 获取一条SM2曲线参数
        final String ecParamSpecName = "sm2p256v1";  // 椭圆曲线参数规范名称
        X9ECParameters sm2ECParameters = GMNamedCurves.getByName(ecParamSpecName);
        //提取公钥点
        ECPoint pukPoint = sm2ECParameters.getCurve().decodePoint(Hex.decode(compressKey));
        String pubKey = Hex.toHexString(pukPoint.getEncoded(Boolean.FALSE));
        //去掉前面的04   (04的时候，可以去掉前面的04)
        pubKey = pubKey.substring(2);
        return pubKey;
    }

    /**
     * 读取SM2证书公钥
     * @param inStream
     */
    private static final String p_readSM2(final InputStream inStream) {
        BouncyCastleUtil.addProvider();
        try {
            // 创建X509工厂类
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", "BC");
            // 创建证书对象
            X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(inStream);
            PublicKey publicKey = cert.getPublicKey();
            byte[] publicKeyBytes = publicKey.getEncoded();
            String base64 = Hex.toHexString(publicKeyBytes);
            // 公钥前缀，该字符串是固定的。通过代码读取SM2证书中的公钥，在公钥前面会有该前缀。在进行验签时应该用剔除掉该前缀的公钥进行验签。
            String pubKey = base64.replace("3059301306072a8648ce3d020106082a811ccf5501822d034200", "");
            return pubKey;
        }catch(Throwable throwable){
            LogUtil.error("Sm2Util_p_readSM2_t1", throwable);
            return null;
        }
    }

    private static final boolean p_publicKeyCheck(final String publicKey) {
        BouncyCastleUtil.addProvider();
        if (publicKey == null || "".equals(publicKey)) {
            return false;
        }
        if (publicKey.length() == 130) {
            if (!publicKey.startsWith("04")) {
                return false;
            }
        } else if (publicKey.length() == 128) {

        } else {
            return false;
        }
        return true;
    }

    private static final boolean p_privateKeyCheck(final String privateKey) {
        if (StringUtil.isBlank(privateKey)) {
            return false;
        }
        if (privateKey.length() == 66) {
            if (!privateKey.startsWith("00")) {
                return false;
            }
        } else if (privateKey.length() == 64) {

        } else {
            return false;
        }
        return true;
    }

}