package xc.crabapple.core.encoder;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;

/**
 * 2016-12-27 1.0.0 新建
 * 2023-10-18 添加自定义编码解码
 * 2024-09-20 功能与兼容性扩展，支持万能解码，新增解码次数参数decodeCount
 * 2024-12-16 新增自定义编码符逻辑
 * 2025-02-18 整体代码逻辑梳理，定义私有标准纯粹编解码方法
 * 专门针对HTTP/HTTPS协议的地址进行自定义编码解码的工具类
 * 思路就是用自定义的唯一性通用ASCII码替代%，实现通讯统一标准，避免网络请求编解码时业务程序的URI受到各语言框架的影响。
 * 比如有的语言或框架内部实现了自动解码、而有的没有。
 * RFC3986规定，Url中只包含四种：英文字母[a-z, A-Z]、 数字[0-9]、 4个特殊字符[-_.~]、 保留字符!*'();:@&=+$,/?#[]
 *
 * 参考资料：
 *   RFC7230 与 RFC3986 标准规范
 *   https://www.cnblogs.com/panchanggui/p/9436348.html
 *   https://blog.csdn.net/qq_41024101/article/details/106413102
 *
 * 举例：
 * 字符  URL编码值
 * %    %25
 * 空格  %20（URL中空格也可用+表示）
 * +    %2B
 * ?    %3F
 * &    %26
 * =    %3D
 * (    %28
 * )    %29
 * {    %7B
 * }    %7D
 * <    %3C
 * >    %3E
 * /    %2F
 * \    %5C
 * |    %7C
 * "    %22
 * #    %23
 * ,    %2C
 * :    %3A
 * ;    %3B
 * @    %40
 */
public final class UriEncoderUtil {

    private static final String CRABAPPLE_BAIFENHAO_FLAG = "CRABAPPLEbaifenhaoFLAG";
    private static Map<String, String> p_encoderMapping = null;
    public static final String EXCLUDE_ADDRESS = ":/.?&=";  // 网络地址（如http地址）指定排除编码字符集合
    public static final String EXCLUDE_PARAM = "&=";  // 网络地址（如http地址）的参数指定排除编码字符集合

    /**
     * 2024-09-20
     * 提前给所有ascii码进行编码注册
     */
    private static final synchronized void p_init(){
        if(p_encoderMapping != null){
            return;
        }
        //
        p_encoderMapping = new HashMap<>();
        String str = AsciiUtil.getSpecialStr();
        int length = str.length();
        for(int i = 0; i < length; i++){
            final char chr = str.charAt(i);
            final String sss = StringUtil.char2str(chr);
            String encoderStr = p_encode(sss, "UTF-8");
            p_encoderMapping.put(sss, encoderStr);
        }
        //
        Map<String, String> map = new HashMap<>();
        Set<String> set = p_encoderMapping.keySet();
        Iterator<String> iterator = set.iterator();
        while(iterator.hasNext()){
            final String key = iterator.next();
            final String value = p_encoderMapping.get(key);
            map.put(value, key);
        }
        Set<String> set2 = map.keySet();
        Iterator<String> iterator2 = set2.iterator();
        while(iterator2.hasNext()){
            final String key = iterator2.next();
            final String value = map.get(key);
            p_encoderMapping.put(key, value);
        }
    }

    /**
     * 2016-12-27 1.0.0
     * 2023-10-18 添加自定义编码解码
     * 2024-09-20 功能与兼容性扩展，支持万能解码，新增解码次数参数decodeCount
     * 2024-12-16 新增自定义编码符逻辑
     * 符合RFC3986标准的"百分号URL编码"
     * 空格 ==> %20，而不是+，和浏览器的encodeURIComponent行为一致
     * ("http://www.baidu.com?p1=%+%2B&p2=A BBB国", ":/.?&=") ==> http://www.baidu.com?p1=%25%2B%252B&p2=A%20BBB%E5%9B%BD
     * @param plaintext 未编码的原字符串
     * @return
     */
    public static final String encodeURIComponent(final String plaintext){
        final String encoderStr = encodeURIComponent(plaintext, null, 1, null, false);
        return encoderStr;
    }

    /**
     * 2016-12-27 1.0.0
     * 2023-10-18 添加自定义编码解码
     * 2024-09-20 功能与兼容性扩展，支持万能解码，新增解码次数参数decodeCount
     * 2024-12-16 新增自定义编码符逻辑
     * 符合RFC3986标准的"百分号URL编码"
     * 空格 ==> %20，而不是+，和浏览器的encodeURIComponent行为一致
     * ("http://www.baidu.com?p1=%+%2B&p2=A BBB国", ":/.?&=") ==> http://www.baidu.com?p1=%25%2B%252B&p2=A%20BBB%E5%9B%BD
     * @param plaintext 未编码的原字符串
     * @param useCustomFlag 自定义编码符
     * @return
     */
    public static final String encodeURIComponent(final String plaintext, final boolean useCustomFlag){
        final String encoderStr = encodeURIComponent(plaintext, null, 1, null, useCustomFlag);
        return encoderStr;
    }

    /**
     * 2016-12-27 1.0.0
     * 2023-10-18 添加自定义编码解码
     * 2024-09-20 功能与兼容性扩展，支持万能解码，新增解码次数参数decodeCount
     * 2024-12-16 新增自定义编码符逻辑
     * 符合RFC3986标准的"百分号URL编码"
     * 空格 ==> %20，而不是+，和浏览器的encodeURIComponent行为一致
     * ("http://www.baidu.com?p1=%+%2B&p2=A BBB国", ":/.?&=") ==> http://www.baidu.com?p1=%25%2B%252B&p2=A%20BBB%E5%9B%BD
     * @param plaintext 未编码的原字符串
     * @param excludeStr 排除编码字符集合，即不给这些字符进行编码（仅对可打印的ASCII码有效）。
     * @return
     */
    public static final String encodeURIComponent(final String plaintext, final String excludeStr){
        final String encoderStr = encodeURIComponent(plaintext, null, 1, excludeStr, false);
        return encoderStr;
    }

    /**
     * 2016-12-27 1.0.0
     * 2023-10-18 添加自定义编码解码
     * 2024-09-20 功能与兼容性扩展，支持万能解码，新增解码次数参数decodeCount
     * 2024-12-16 新增自定义编码符逻辑
     * 符合RFC3986标准的"百分号URL编码"
     * 空格 ==> %20，而不是+，和浏览器的encodeURIComponent行为一致
     * ("http://www.baidu.com?p1=%+%2B&p2=A BBB国", ":/.?&=") ==> http://www.baidu.com?p1=%25%2B%252B&p2=A%20BBB%E5%9B%BD
     * @param plaintext 未编码的原字符串
     * @param excludeStr 排除编码字符集合，即不给这些字符进行编码（仅对可打印的ASCII码有效）。
     * @param useCustomFlag 自定义编码符
     * @return
     */
    public static final String encodeURIComponent(final String plaintext, final String excludeStr, final boolean useCustomFlag){
        final String encoderStr = encodeURIComponent(plaintext, null, 1, excludeStr, useCustomFlag);
        return encoderStr;
    }

    /**
     * 2016-12-27 1.0.0
     * 2023-10-18 添加自定义编码解码
     * 2024-09-20 功能与兼容性扩展，支持万能解码，新增解码次数参数decodeCount
     * 2024-12-16 新增自定义编码符逻辑
     * 符合RFC3986标准的"百分号URL编码"
     * 空格 ==> %20，而不是+，和浏览器的encodeURIComponent行为一致
     * ("http://www.baidu.com?p1=%+%2B&p2=A BBB国", ":/.?&=") ==> http://www.baidu.com?p1=%25%2B%252B&p2=A%20BBB%E5%9B%BD
     * @param plaintext 未编码的原字符串
     * @param encodeCount 编码次数
     * @return
     */
    public static final String encodeURIComponent(final String plaintext, final int encodeCount){
        final String encoderStr = encodeURIComponent(plaintext, null, encodeCount, null, false);
        return encoderStr;
    }

    /**
     * 2016-12-27 1.0.0
     * 2023-10-18 添加自定义编码解码
     * 2024-09-20 功能与兼容性扩展，支持万能解码，新增解码次数参数decodeCount
     * 2024-12-16 新增自定义编码符逻辑
     * 符合RFC3986标准的"百分号URL编码"
     * 空格 ==> %20，而不是+，和浏览器的encodeURIComponent行为一致
     * ("http://www.baidu.com?p1=%+%2B&p2=A BBB国", ":/.?&=") ==> http://www.baidu.com?p1=%25%2B%252B&p2=A%20BBB%E5%9B%BD
     * @param plaintext 未编码的原字符串
     * @param encodeCount 编码次数
     * @param useCustomFlag 自定义编码符
     * @return
     */
    public static final String encodeURIComponent(final String plaintext, final int encodeCount, final boolean useCustomFlag){
        final String encoderStr = encodeURIComponent(plaintext, null, encodeCount, null, useCustomFlag);
        return encoderStr;
    }

    /**
     * 2016-12-27 1.0.0
     * 2023-10-18 添加自定义编码解码
     * 2024-09-20 功能与兼容性扩展，支持万能解码，新增解码次数参数decodeCount
     * 2024-12-16 新增自定义编码符逻辑
     * 符合RFC3986标准的"百分号URL编码"
     * 空格 ==> %20，而不是+，和浏览器的encodeURIComponent行为一致
     * ("http://www.baidu.com?p1=%+%2B&p2=A BBB国", ":/.?&=") ==> http://www.baidu.com?p1=%25%2B%252B&p2=A%20BBB%E5%9B%BD
     * @param plaintext 未编码的原字符串
     * @param encodeCount 编码次数
     * @param excludeStr 排除编码字符集合，即不给这些字符进行编码（仅对可打印的ASCII码有效）。
     * @return
     */
    public static final String encodeURIComponent(final String plaintext, final int encodeCount, final String excludeStr){
        final String encoderStr = encodeURIComponent(plaintext, null, encodeCount, excludeStr, false);
        return encoderStr;
    }

    /**
     * 2016-12-27 1.0.0
     * 2023-10-18 添加自定义编码解码
     * 2024-09-20 功能与兼容性扩展，支持万能解码，新增解码次数参数decodeCount
     * 2024-12-16 新增自定义编码符逻辑
     * 符合RFC3986标准的"百分号URL编码"
     * 空格 ==> %20，而不是+，和浏览器的encodeURIComponent行为一致
     * ("http://www.baidu.com?p1=%+%2B&p2=A BBB国", ":/.?&=") ==> http://www.baidu.com?p1=%25%2B%252B&p2=A%20BBB%E5%9B%BD
     * @param plaintext 未编码的原字符串
     * @param format 编码格式
     * @param encodeCount 编码次数
     * @param excludeStr 排除编码字符集合，即不给这些字符进行编码（仅对可打印的ASCII码有效）。
     * @param useCustomFlag 自定义编码符
     * @return
     */
    public static final String encodeURIComponent(final String plaintext, final String format, final int encodeCount, final String excludeStr, final boolean useCustomFlag){
        if(StringUtil.isBlank(plaintext)){
            return plaintext;
        }
        if(encodeCount <= 0){
            return plaintext;
        }
        String formatLocal = format;
        if(StringUtil.isBlank(formatLocal)){
            formatLocal = "UTF-8";
        }
        //
        try{
            String encoderStr = p_encode(plaintext, formatLocal);
            if(useCustomFlag){
                encoderStr = StringUtil.replaceAll(encoderStr, "%", CRABAPPLE_BAIFENHAO_FLAG);
            }
            if(encodeCount >= 2){
                for(int i = 2; i <= encodeCount; i++){
                    encoderStr = p_encode(encoderStr, formatLocal);
                }
            }
            // 处理排除字符集合
            if(!StringUtil.isBlank(excludeStr)){
                p_init();
                int length = excludeStr.length();
                for(int i = 0; i < length; i++){
                    char chr = excludeStr.charAt(i);
                    String text = StringUtil.char2str(chr);
                    if(!p_encoderMapping.containsKey(text)){
                        LogUtil.error("UriEncoderUtil_encodeURIComponent_1", "排除编码字符集合中存在尚未注册的字符", plaintext, encodeCount, excludeStr, text);
                        continue;
                    }
                    String encoderText = p_encoderMapping.get(text);
                    encoderStr = StringUtil.replaceAll(encoderStr, encoderText, text);
                }
            }
            //
            return encoderStr;
        }catch(Throwable throwable){
            LogUtil.error("UriEncoderUtil_encodeURIComponent_t1", throwable, "编码失败", plaintext, formatLocal, encodeCount);
            return null;
        }
    }

    /**
     * 2025-02-18
     * 对指定字符串编码
     * @param plaintext
     * @return
     */
    private static final String p_encode(final String plaintext, final String format){
        if(StringUtil.isBlank(plaintext)){
            return plaintext;
        }
        try{
            String encoderStr = URLEncoder.encode(plaintext, format);
            encoderStr = StringUtil.replaceAll(encoderStr, "+", "%20");
            return encoderStr;
        }catch (Throwable throwable){
            LogUtil.error("UriEncoderUtil_p_encode_t1", throwable, "编码失败", plaintext, format);
            return null;
        }
    }

    /**
     * 2023-08-22 新建
     * 2023-09-17 添加编码
     * 2023-10-18 添加自定义编码解码
     * 2024-09-20 功能与兼容性扩展，支持万能解码，新增解码次数参数decodeCount
     * 2024-12-16 新增自定义编码符逻辑
     * @param encoderStr 已编码的字符串
     * @return 返回正确的解码值即原文，若解码失败则返回空字符串。
     */
    public static final String decodeURIComponent(final String encoderStr){
        final String plaintext = decodeURIComponent(encoderStr, null, false);
        return plaintext;
    }

    /**
     * 2023-08-22 新建
     * 2023-09-17 添加编码
     * 2023-10-18 添加自定义编码解码
     * 2024-09-20 功能与兼容性扩展，支持万能解码，新增解码次数参数decodeCount
     * 2024-12-16 新增自定义编码符逻辑
     * @param encoderStr 已编码的字符串
     * @param format 编码格式
     * @return 返回正确的解码值即原文，若解码失败则返回空字符串。
     */
    public static final String decodeURIComponent(final String encoderStr, String format){
        final String plaintext = decodeURIComponent(encoderStr, format, false);
        return plaintext;
    }

    /**
     * 2023-08-22 新建
     * 2023-09-17 添加编码
     * 2023-10-18 添加自定义编码解码
     * 2024-09-20 功能与兼容性扩展，支持万能解码，新增解码次数参数decodeCount
     * 2024-12-16 新增自定义编码符逻辑
     * @param encoderStr 已编码的字符串
     * @param useCustomFlag 自定义编码符
     * @return 返回正确的解码值即原文，若解码失败则返回空字符串。
     */
    public static final String decodeURIComponent(final String encoderStr, final boolean useCustomFlag){
        final String plaintext = decodeURIComponent(encoderStr, null, useCustomFlag);
        return plaintext;
    }

    /**
     * 2023-08-22 新建
     * 2023-09-17 添加编码
     * 2023-10-18 添加自定义编码解码
     * 2024-09-20 功能与兼容性扩展，支持万能解码，新增解码次数参数decodeCount。
     * 2024-12-16 新增自定义编码符逻辑
     * @param encoderStr 已编码的字符串
     * @param format 编码格式
     * @param useCustomFlag 自定义编码符
     * @return 返回正确的解码值即原文，若解码失败则返回空字符串。
     */
    public static final String decodeURIComponent(final String encoderStr, final String format, final boolean useCustomFlag){
        if(StringUtil.isBlank(encoderStr)){
            return encoderStr;
        }
        String formatLocal = format;
        if(StringUtil.isBlank(formatLocal)){
            formatLocal = "UTF-8";
        }
        //
        String encoderStrLocal = encoderStr;
        if(useCustomFlag){
            if(encoderStrLocal.indexOf(CRABAPPLE_BAIFENHAO_FLAG) == -1){
                return encoderStrLocal;
            }
            encoderStrLocal = StringUtil.replaceAll(encoderStrLocal, CRABAPPLE_BAIFENHAO_FLAG, "%");
        }
        //
        final String plaintext = p_decode(encoderStrLocal, formatLocal);
        return plaintext;
    }

    /**
     * 2025-02-18
     * 对指定字符串解码
     * @param encoderStr
     * @param format
     * @return
     */
    private static final String p_decode(final String encoderStr, final String format){
        if(StringUtil.isBlank(encoderStr)){
            return encoderStr;
        }
        if(encoderStr.indexOf("%") == -1){
            return encoderStr;
        }
        try{
            String encoderStrLocal = encoderStr.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
            encoderStrLocal = StringUtil.replaceAll(encoderStrLocal, "+", "%2B");
            encoderStrLocal = StringUtil.replaceAll(encoderStrLocal, "\\+", "%2B");
            String plaintext = URLDecoder.decode(encoderStrLocal, format);
            while(true){
                plaintext = StringUtil.replaceAll(plaintext, "%25", "%");
                if(plaintext.indexOf("%25") == -1){
                    break;
                }
            }
            return plaintext;
        }catch (Throwable throwable){
            LogUtil.error("UriEncoderUtil_p_decode_t1", throwable, "编码失败", encoderStr, format);
            return null;
        }
    }

}