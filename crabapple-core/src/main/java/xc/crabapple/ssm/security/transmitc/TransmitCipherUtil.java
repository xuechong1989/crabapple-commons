package xc.crabapple.ssm.security.transmitc;

import xc.crabapple.core.cache.LocalCacheUtil;
import xc.crabapple.core.cipher.md5.MD5Util;
import xc.crabapple.core.cipher.sm2.Sm2Util;
import xc.crabapple.core.cipher.sm4.Sm4Util;
import xc.crabapple.core.cipher.submerge.SubmergeUtil;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.data.dto.DtoBase;
import xc.crabapple.core.json.jackson.JacksonUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.servlet.ServletUtil;
import xc.crabapple.ssm.spring.SsmRequestWrapper;

import java.util.HashMap;
import java.util.Map;

/**
 * 2025-01-17
 * 前后端数据交互传输加解密
 * 1. 防止数据暴露
 * 2. 防止重放攻击/重播攻击/回放攻击（Replay Attack）
 * 3. 防止数据伪造，添加数据签名
 * 4. 防止中间人攻击MITM
 * 5. 防止跨站请求伪造攻击CSRF
 */
public final class TransmitCipherUtil {

    private static String p_enable = "yes";  // 加解密开关
    // c2s，客户端提供，客户端同步存储：p_c2s_submergeKey / p_c2s_sm2_publicKey / p_c2s_sm2_privateKey
    private static String p_c2s_submergeKey = "";
    private static String p_c2s_sm2_publicKey = "";
    private static String p_c2s_sm2_privateKey = "";
    private static Map<String, Integer> p_c2s_api_whitelistMap = null;
    // s2c，服务端提供，客户端同步存储：p_s2c_submergeKey / p_s2c_sm2_privateKey
    private static String p_s2c_submergeKey = "";
    private static String p_s2c_sm2_publicKey = "";
    private static String p_s2c_sm2_privateKey = "";
    private static Map<String, Integer> p_s2c_api_whitelistMap = null;

    /**
     * 2025-01-17
     */
    public static final void init(final String tcEnbale, final String tcC2sSubmergeKey, final String tcC2sSm2publicKey, final String tcC2sSm2privateKey, final String tcC2sApiWhitelist, final String tcS2cSubmergeKey, final String tcS2cSm2publicKey, final String tcS2cSm2privateKey, final String tcS2cApiWhitelist){
        if(!"yes".equals(tcEnbale)){
            p_enable = "no";
            return;
        }
        p_enable = tcEnbale;
        if(StringUtil.isBlankOfOne(tcC2sSubmergeKey, tcC2sSm2publicKey, tcC2sSm2privateKey, tcS2cSubmergeKey, tcS2cSm2publicKey, tcS2cSm2privateKey)){
            p_enable = "no";
            LogUtil.error("TransmitCipherUtil_init_1", "传参加密已启用但密钥未配置，已自动停用");
            return;
        }
        //
        // c2s，客户端提供，客户端同步存储
        p_c2s_submergeKey = tcC2sSubmergeKey;
        p_c2s_sm2_publicKey = tcC2sSm2publicKey;
        p_c2s_sm2_privateKey = tcC2sSm2privateKey;
        if(!StringUtil.isBlank(tcC2sApiWhitelist)){
            p_c2s_api_whitelistMap = new HashMap<>(32);
            String[] array = StringUtil.split(tcC2sApiWhitelist, ",");
            for(int i = 0; i < array.length; i++){
                String api = array[i];
                api = StringUtil.getPureStr(api);
                api = StringUtil.deleteAll(api, " ");
                p_c2s_api_whitelistMap.put(api, 1);
            }
        }
        // s2c
        p_s2c_submergeKey = tcS2cSubmergeKey;
        p_s2c_sm2_publicKey = tcS2cSm2publicKey;
        p_s2c_sm2_privateKey = tcS2cSm2privateKey;
        if(!StringUtil.isBlank(tcS2cApiWhitelist)){
            p_s2c_api_whitelistMap = new HashMap<>(32);
            String[] array = StringUtil.split(tcS2cApiWhitelist, ",");
            for(int i = 0; i < array.length; i++){
                String api = array[i];
                api = StringUtil.getPureStr(api);
                api = StringUtil.deleteAll(api, " ");
                p_s2c_api_whitelistMap.put(api, 1);
            }
        }
    }

    /**
     * 2025-01-17
     * 服务器端针对接收到客户端的数据进行解密
     * @return request
     */
    public static final SsmRequestWrapper c2sDecrypt(final SsmRequestWrapper request){
        // 是否加密开关
        if(!"yes".equals(p_enable)){
            return request;
        }
        // c2s白名单
        if(!CollectionUtil.isBlank(p_c2s_api_whitelistMap)){
            final String requestURI = ServletUtil.getRequestURI();
            if(p_c2s_api_whitelistMap.containsKey(requestURI)){
                return request;
            }
        }
        //
        final String cipher = ServletUtil.getString("security_cipherEnable");
        final String cipherKeyMeta = ServletUtil.getString("security_cipherKeyMeta");
        final String cipherC2sData = ServletUtil.getString("security_cipherC2sData");
        final String signC2sData = ServletUtil.getString("security_signC2sData");
        final String cipherDDOS = ServletUtil.getString("security_ddos");
        request.removeParameter("security_cipherEnable");
        request.removeParameter("security_cipherC2sData");
        request.removeParameter("security_cipherKeyMeta");
        request.removeParameter("security_signC2sData");
        request.removeParameter("security_ddos");
        if(StringUtil.isBlankOfOne(cipher, cipherKeyMeta, cipherC2sData, signC2sData, cipherDDOS)){
            request.setStatus("transmitcipher_error_1");
            return request;
        }
        //
        String plaintextCipher = SubmergeUtil.decrypt(p_c2s_submergeKey, cipher);
        plaintextCipher = Sm2Util.decrypt(p_c2s_sm2_privateKey, plaintextCipher);
        if (!StringUtil.equals(plaintextCipher, "yes")){
            request.setStatus("transmitcipher_error_2");
            return request;
        }
        // sm4密钥解密
        String plaintextKeyMeta = SubmergeUtil.decrypt(p_c2s_submergeKey, cipherKeyMeta);
        plaintextKeyMeta = Sm2Util.decrypt(p_c2s_sm2_privateKey, plaintextKeyMeta);
        String sm4key = StringUtil.substr(plaintextKeyMeta, 0, 16);
        String sm4iv = StringUtil.substr(plaintextKeyMeta, 16, 32);
        String sm4modeName = StringUtil.substr(plaintextKeyMeta, 32, 35);
        String sm4padding = StringUtil.substr(plaintextKeyMeta, 35, plaintextKeyMeta.length());
        Integer sm4paddingInt = Integer.valueOf(sm4padding);
        // 实际业务参数解密
        String plaintextC2sData = Sm4Util.decrypt(sm4key, sm4iv, cipherC2sData, sm4modeName, sm4paddingInt);  // 使用明文的sm4的key对加密的业务数据进行对称解密，得到明文业务数据
        Map<String, Object> paramMap = JacksonUtil.json2map(plaintextC2sData);
        for (Map.Entry<String, Object> paramEntry : paramMap.entrySet()) {
            Object entryValue = paramEntry.getValue();
            if (entryValue instanceof String){
                String paramValue = (String) entryValue;
                paramEntry.setValue(paramValue);
            }
        }
        request.addAllParameters(paramMap);
        // 校验实际业务数据的签名，防篡改
        String plaintextSignC2sData = SubmergeUtil.decrypt(p_c2s_submergeKey, signC2sData);
        plaintextSignC2sData = Sm2Util.decrypt(p_c2s_sm2_privateKey, plaintextSignC2sData);
        final String md5C2sData = MD5Util.getHash(plaintextC2sData);
        if(!StringUtil.equals(plaintextSignC2sData, md5C2sData)){
            final String requestURI = ServletUtil.getRequestURI();
            LogUtil.error("SecurityUtil_c2sDecrypt_1", "数据签名异常", requestURI, plaintextC2sData, plaintextSignC2sData, md5C2sData);
            ServletUtil.setAttribute("security_msg", "error_sign");
            return request;
        }
        // 重放攻击（Replay Attack），13位时间戳+64位随机数
        String plaintextDDOS = SubmergeUtil.decrypt(p_c2s_submergeKey, cipherDDOS);
        plaintextDDOS = Sm2Util.decrypt(p_c2s_sm2_privateKey, plaintextDDOS);
        boolean has = LocalCacheUtil.hasKey(plaintextDDOS);
        if(has){
            final String requestURI = ServletUtil.getRequestURI();
            LogUtil.error("SecurityUtil_c2sDecrypt_2", "重放攻击DDOS", requestURI, plaintextC2sData, plaintextSignC2sData, md5C2sData);
            ServletUtil.setAttribute("security_msg", "error_ddos");
            return request;
        }
        // 2026-08-20 构建替代redis的本地API持久化缓存框架
        // 重放防护标记改纯内存存储：短命安全标记无需重启恢复
        LocalCacheUtil.set(plaintextDDOS, "exist", 3600 * 24, true);
        //
        return request;
    }

    /**
     * 2025-01-17
     * 服务器端针对响应到客户端的数据进行加密
     * @param plaintextDtoBase
     * @return
     */
    public static final DtoBase s2cEncrypt(final DtoBase plaintextDtoBase){
        // 是否加密开关
        if(!"yes".equals(p_enable)){
            plaintextDtoBase.setCipher("no");
            return plaintextDtoBase;
        }
        plaintextDtoBase.setCipher("yes");
        // s2c白名单
        if(!CollectionUtil.isBlank(p_s2c_api_whitelistMap)){
            final String requestURI = ServletUtil.getRequestURI();
            if(p_s2c_api_whitelistMap.containsKey(requestURI)){
                plaintextDtoBase.setCipher("no");
                return plaintextDtoBase;
            }
        }
        //
        String cipherEnable = plaintextDtoBase.getCipher();
        cipherEnable = Sm2Util.encrypt(p_s2c_sm2_publicKey, cipherEnable);
        cipherEnable = SubmergeUtil.encrypt(p_s2c_submergeKey, cipherEnable);
        // 使用【sm2算法+submerge算法】加密sm2公钥
        final String sm4key = RandomUtil.getStr(16, false);
        final String sm4iv = RandomUtil.getStr(16, false);
        final String sm4modeName = "cbc";
        final Integer sm4padding = 7;
        final String plaintextSm4meta = sm4key + sm4iv + sm4modeName + sm4padding;
        String cipherKeyMeta = Sm2Util.encrypt(p_s2c_sm2_publicKey, plaintextSm4meta);
        cipherKeyMeta = SubmergeUtil.encrypt(p_s2c_submergeKey, cipherKeyMeta);
        // 使用【sm4算法】加密整体业务对象
        final String s2cDtoStr = JacksonUtil.obj2json(plaintextDtoBase);
        final String cipherS2cDtoStr = Sm4Util.encrypt(sm4key, sm4iv, s2cDtoStr, sm4modeName, sm4padding);
        // 使用【md5+submerge算法】对整体业务对象明文进行数字签名
        String signS2cDtoStr = MD5Util.getHash(s2cDtoStr);
        signS2cDtoStr = SubmergeUtil.encrypt(p_s2c_submergeKey, signS2cDtoStr);
        //
        DtoBase cipherDtoBase = plaintextDtoBase;
        cipherDtoBase.setParams(null);
        cipherDtoBase.setParam("security_cipherEnable", cipherEnable);
        cipherDtoBase.setParam("security_cipherKeyMeta", cipherKeyMeta);
        cipherDtoBase.setParam("security_cipherS2cDtoStr", cipherS2cDtoStr);
        cipherDtoBase.setParam("security_signS2cDtoStr", signS2cDtoStr);
        return cipherDtoBase;
    }

    /**
     * 2025-01-24
     * @return
     */
    public static final String getEnable(){
        return p_enable;
    }

}
