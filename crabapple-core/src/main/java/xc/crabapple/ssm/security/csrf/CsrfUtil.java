package xc.crabapple.ssm.security.csrf;

import xc.crabapple.core.cipher.sm4.Sm4Util;
import xc.crabapple.core.cipher.submerge.SubmergeUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.servlet.ServletUtil;

/**
 * 2025-01-21
 * 跨站请求伪造CSRF攻击（Cross-Site Request Forgery）
 *
 * https://blog.csdn.net/qq_43378996/article/details/123910614
 * https://tech.meituan.com/2018/10/11/fe-security-csrf.html
 *
 * 攻击者诱导受害者进入第三方网站，在第三方网站中，向被攻击网站发送跨站请求。利用受害者在被攻击网站已经获取的注册凭证，绕过后台的用户验证，
 * 达到冒充用户对被攻击的网站执行某项操作的目的。一个典型的CSRF攻击有着如下的流程：
 *   1. 受害者登录a.com，并保留了登录凭证（Cookie）。
 *   2. 攻击者引诱受害者访问了b.com。
 *   3. b.com 向 a.com 发送了一个请求：a.com/act=xx。浏览器会默认携带a.com的Cookie。
 *   4. a.com接收到请求后，对请求进行验证，并确认是受害者的凭证，误以为是受害者自己发送的请求。
 *   5. a.com以受害者的名义执行了act=xx。
 *   6. 攻击完成，攻击者在受害者不知情的情况下，冒充受害者，让a.com执行了自己定义的操作。
 *
 * 解决方案：
 *   1. CSRF自动防御策略：同源检测（Origin和Referer验证）。
 *   2. CSRF主动防御措施：Token验证，双重Cookie验证，Samesite Cookie。
 *   3. 保证页面的幂等性，后端接口不要在GET页面中做用户操作。
 */
public class CsrfUtil {

    private static String p_enable = "yes";
    // 系统取得csrfToken，传入客户端（一般是页面调用系统的第一个接口时），放入客户端缓存，后续客户端在调用所有接口时均要携带该值
    private static String p_csrfTokenPlaintext = null;
    private static String p_csrfTokenCipher = null;
    private static String p_csrfTokenParamName = "csrf_token";
    private static String p_csrfTokenSm4Key = null;
    private static String p_csrfTokenSm4Iv = null;
    private static String p_csrfTokenSubmergeKey = null;  // 客户端也会存一份
    private static String p_firstApi = null;  // 第一个接口，该接口会返回csrfToken给客户端

    /**
     * 2025-01-21
     * @param enable
     * @param csrfTokenSubmergeKey
     * @param firstApi 第一个接口，该接口会返回csrfToken给客户端
     */
    public static final void init(final String enable, final String csrfTokenSubmergeKey, final String firstApi){
        init(enable, 128, csrfTokenSubmergeKey, p_csrfTokenParamName, firstApi);
    }

    /**
     * 2025-01-21
     * @param enable
     * @param length
     * @param csrfTokenSubmergeKey 客户端也会存一份
     * @param csrfTokenParamName
     * @param firstApi 第一个接口，该接口会返回csrfToken给客户端
     */
    public static final void init(final String enable, final Integer length, final String csrfTokenSubmergeKey, final String csrfTokenParamName, final String firstApi){
        if(!"yes".equals(enable)){
            p_enable = "no";
            return;
        }
        p_enable = enable;
        //
        if(length < 128){
            LogUtil.error("CsrfUtil_init_1", "csrfToken长度最少128个字符串", length);
            return;
        }
        //
        p_csrfTokenParamName = csrfTokenParamName;
        p_csrfTokenSm4Key = Sm4Util.generateKey();
        p_csrfTokenSm4Iv = Sm4Util.generateIv();
        p_csrfTokenSubmergeKey = csrfTokenSubmergeKey;
        p_csrfTokenPlaintext = RandomUtil.getStr(length, false);
        p_csrfTokenCipher = Sm4Util.encrypt(p_csrfTokenSm4Key, p_csrfTokenSm4Iv, p_csrfTokenPlaintext, "CBC", 7);
        p_csrfTokenCipher = SubmergeUtil.encrypt(p_csrfTokenSubmergeKey, p_csrfTokenCipher);
        p_firstApi = firstApi;
    }

    /**
     * 2025-01-21
     * 系统取得csrfToken，传入客户端（一般是页面调用系统的第一个接口时），放入客户端缓存，后续客户端在调用所有接口时均要携带该值
     * @return
     */
    public static final String getCsrfToken(){
        if(!"yes".equals(p_enable)){
            return null;
        }
        if(StringUtil.isBlank(p_csrfTokenCipher)){
            final String requestURI = ServletUtil.getRequestURI();
            LogUtil.error("CsrfUtil_getCsrfToken_1", "csrfToken为空", requestURI);
            return null;
        }
        return p_csrfTokenCipher;
    }

    /**
     * 2025-01-21
     * csrf检测
     * @return
     */
    public static final String check(){
        if(!"yes".equals(p_enable)){
            return "success";
        }
        final String requestURI = ServletUtil.getRequestURI();
        if(StringUtil.equals(requestURI, p_firstApi)){
            return "success";
        }
        //
        final String paramCsrfToken = ServletUtil.getString(p_csrfTokenParamName);
        if(StringUtil.isBlank(paramCsrfToken)){
            LogUtil.error("CsrfUtil_check_1", "客户端传入的csrfToken为空，可能存在CSRF攻击", p_firstApi, requestURI, p_csrfTokenParamName);
            return "csrf_error_paramCsrfToken";
        }
        String csrfTokenPlaintext = SubmergeUtil.decrypt(p_csrfTokenSubmergeKey, paramCsrfToken);
        csrfTokenPlaintext = Sm4Util.decrypt(p_csrfTokenSm4Key, p_csrfTokenSm4Iv, csrfTokenPlaintext, "CBC", 7);
        if(StringUtil.equals(csrfTokenPlaintext, p_csrfTokenPlaintext)){
            return "success";
        }
        LogUtil.error("CsrfUtil_check_2", "csrfToken错误，可能存在CSRF攻击", p_firstApi, requestURI, p_csrfTokenParamName, paramCsrfToken, p_csrfTokenSubmergeKey, p_csrfTokenSm4Key, p_csrfTokenSm4Iv);
        return "csrf_error_not_equals";
    }

}
