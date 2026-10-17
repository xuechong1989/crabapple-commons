package xc.crabapple.ssm.security.xss;

import jakarta.servlet.http.HttpServletRequest;
import xc.crabapple.core.cipher.md5.MD5Util;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.servlet.ServletUtil;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;

/**
 * 2024-07-25
 * 2025-11-21 类重写，提供完整测试用例
 * 2025-11-30 使用线程安全的API白名单集合
 * XSS攻击测试用例：把下面【】中的内容填到一个表单中，通过对应接口传参，存入数据库。然后在界面中打开显示时，如存在XSS漏洞则会弹出alert(1)
 * 【测试bug'" <img src=x onerror=alert(1)>】
 */
public final class XssUtil {

    private static final String ENABLE_YES = "yes";
    private static final String ENABLE_NO = "no";
    private static final String EMPTY_STRING = "";
    private static final String CHINESE_COMMA = "，";
    private static final String COMMA = ",";

    private static String p_enable = ENABLE_NO;
    private static String p_apiWhiteStrMd5 = EMPTY_STRING;
    private static CopyOnWriteArraySet<String> p_apiWhiteSet = null;
    private static Pattern PATTERN_HTML_TAG = null;
    private static Pattern PATTERN_HTML_EVENT = null;  // HTML事件属性检测（onclick, onload等）
    private static Pattern PATTERN_HTML_ATTR = null;  // 危险的HTML属性（style, href等可能包含XSS）
    private static Pattern PATTERN_SCRIPT = null;  // 脚本标签特定检测
    private static Pattern JAVASCRIPT_PROTOCOL_PATTERN = null;  // JavaScript协议检测（javascript:）
    private static Pattern PATTERN_HEX = null;  // 十六进制编码的XSS尝试检测
    private static Pattern PATTERN_SQL_1 = null;  // SQL注入关键字检测
    private static Pattern PATTERN_SQL_2 = null;  // SQL注入关键字检测

    /**
     * 2025-11-30 快捷初始化
     */
    public static final void init(){
        init(ENABLE_NO, null);
    }

    /**
     * 2025-11-30 快捷初始化
     */
    public static final void init(final String enable) {
        init(enable, null);
    }

    /**
     * 2025-11-21 实际初始化
     * @param enable 使用启用XSS攻击检测
     * @param apiWhiteStr API白名单，英文逗号分隔
     */
    public static final void init(final String enable, final String apiWhiteStr) {
        if (!ENABLE_YES.equals(enable)) {
            p_enable = ENABLE_NO;
            return;
        }
        p_enable = enable;
        p_apiWhiteSet = new CopyOnWriteArraySet<>();
        p_updateApiWhiteSet(apiWhiteStr);
        //
        PATTERN_HTML_TAG = Pattern.compile("<(\"[^\"]*\"|'[^']*'|[^'\">])*>", Pattern.CASE_INSENSITIVE);
        PATTERN_HTML_EVENT = Pattern.compile("\\s(onclick|oncontextmenu|ondblclick|onmousedown|onmouseenter|onmouseleave|onmousemove|onmouseover|onmouseout|onmouseup|onkeydown|onkeypress|onkeyup|onbeforeunload|onerror|onhashchange|onload|onpageshow|onpagehide|onresize|onscroll|onunload|onblur|onchange|onfocus|onfocusin|onfocusout|oninput|onreset|onsearch|onselect|onsubmit|oncopy|oncut|onpaste|onafterprint|onbeforeprint|ondrag|ondragend|ondragenter|ondragleave|ondragover|ondragstart|ondrop|onabort|oncanplay|oncanplaythrough|ondurationchange|onemptied|onended|onloadeddata|onloadedmetadata|onloadstart|onpause|onplay|onplaying|onprogress|onratechange|onseeked|onseeking|onstalled|onsuspend|ontimeupdate|onvolumechange|onwaiting|animationend|animationiteration|animationstart|transitionend|onmessage|onmousewheel|ononline|onoffline|onpopstate|onshow|onstorage|ontoggle|onwheel|CAPTURING-PHASE|AT-TARGET|BUBBLING-PHASE|bubbles|cancelable|currentTarget|eventPhase|timeStamp|initEvent|preventDefault|stopPropagation|addEventListener|dispatchEvent|removeEventListener|handleEvent|createEvent|altKey|clientX|clientY|ctrlKey|charCode|keyCode|metaKey|relatedTarget|screenX|screenY|shiftKey|initMouseEvent|initKeyboardEvent)\\s*=", Pattern.CASE_INSENSITIVE);
        PATTERN_HTML_ATTR = Pattern.compile("\\s(style|href|src|action|formaction)\\s*=", Pattern.CASE_INSENSITIVE);
        PATTERN_SCRIPT = Pattern.compile("<script\\b[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        JAVASCRIPT_PROTOCOL_PATTERN = Pattern.compile("javascript:\\s*.*", Pattern.CASE_INSENSITIVE);
        PATTERN_HEX = Pattern.compile("%3Cscript.*%3E|%3C.*%20on\\w+%3D", Pattern.CASE_INSENSITIVE);
        PATTERN_SQL_1 = Pattern.compile("\\s(select|update|union|and|or|delete|insert|trancate|char|into|substr|ascii|declare|exec|count|master|into|drop|execute)\\s*", Pattern.CASE_INSENSITIVE);
        PATTERN_SQL_2 = Pattern.compile("\\s(UNION\\s+SELECT|INSERT\\s+INTO|DROP\\s+TABLE|OR\\s+'?1'?\\s*=\\s*'?1|EXEC\\s*\\(|UPDATE.+SET|SELECT.+FROM|DELETE.+FROM)\\s", Pattern.CASE_INSENSITIVE);
    }

    /**
     * 2025-11-30 更新API白名单
     * @param apiWhiteStr API白名单，英文逗号分隔
     */
    private static final void p_updateApiWhiteSet(final String apiWhiteStr) {
        final String apiWhiteStrMd5 = MD5Util.getHash(apiWhiteStr);
        if(p_apiWhiteStrMd5.equals(apiWhiteStrMd5)) {
            return;
        }
        p_apiWhiteStrMd5 = apiWhiteStrMd5;
        p_apiWhiteSet.clear();
        String standardApiWhiteStr = StringUtil.getStandardStr(apiWhiteStr);
        if (standardApiWhiteStr == null) {
            return;
        }
        standardApiWhiteStr = StringUtil.replaceAll(standardApiWhiteStr, CHINESE_COMMA, COMMA);
        final String[] array = StringUtil.split(standardApiWhiteStr, COMMA);
        if(array == null || array.length == 0) {
            return;
        }
        for (int i = 0; i < array.length; i++) {
            final String rawApi = array[i];
            final String api = StringUtil.getStandardStr(rawApi);
            if (api == null || EMPTY_STRING.equals(api)) {
                continue;
            }
            if(p_apiWhiteSet.contains(api)) {
                continue;
            }
            p_apiWhiteSet.add(api);
        }
    }

    /**
     * 2025-11-30
     */
    public static final boolean detect() {
        final HttpServletRequest request = ServletUtil.getRequest();
        final boolean flag = detect(request, null);
        return flag;
    }

    /**
     * 2025-11-30
     * @param apiWhiteStr API白名单，英文逗号分隔
     * @return
     */
    public static final boolean detect(final String apiWhiteStr) {
        final HttpServletRequest request = ServletUtil.getRequest();
        final boolean flag = detect(request, apiWhiteStr);
        return flag;
    }

    /**
     * 2025-11-21
     * @param request
     * @param apiWhiteStr API白名单，英文逗号分隔
     * @return (boolean) 返回true表示检测出XSS攻击，返回false表示未检测出XSS攻击
     */
    public static final boolean detect(final HttpServletRequest request, final String apiWhiteStr) {
        if (!ENABLE_YES.equals(p_enable)) {
            return false;
        }
        if (request == null) {
            return false;
        }
        final Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap == null) {
            return false;
        }
        p_updateApiWhiteSet(apiWhiteStr);
        if (!p_apiWhiteSet.isEmpty()) {
            final String requestURI = ServletUtil.getRequestURI(request);
            if (p_apiWhiteSet.contains(requestURI)) {
                return false;
            }
        }
        //
        final Set<String> stringSet = parameterMap.keySet();
        final Iterator<String> iterator = stringSet.iterator();
        while (iterator.hasNext()) {
            final String key = iterator.next();
            final String[] values = parameterMap.get(key);
            if (values == null) {
                continue;
            }
            final String value = values[0];
            final boolean flag = detectValue(value);
            if (flag) {
                return true;
            }
        }
        return false;
    }

    /**
     * 2025-11-21
     * 检测输入字符串是否包含潜在的XSS攻击代码
     * @return (boolean) 返回true表示检测出XSS攻击，返回false表示未检测出XSS攻击
     */
    public static final boolean detectValue(final String str) {
        if (!ENABLE_YES.equals(p_enable)) {
            return false;
        }
        // 检测HTML标签
        if (PATTERN_HTML_TAG.matcher(str).find()) {
            return true;
        }
        // 检测到<script>标签
        if (PATTERN_SCRIPT.matcher(str).find()) {
            return true;
        }
        // 检测到事件处理属性（onclick等）
        if (PATTERN_HTML_EVENT.matcher(str).find()) {
            return true;
        }
        // 检测到javascript:协议
        if (JAVASCRIPT_PROTOCOL_PATTERN.matcher(str).find()) {
            return true;
        }
        // 检测到SQL注入关键词
        if (PATTERN_SQL_1.matcher(str).find()) {
            return true;
        }
        if (PATTERN_SQL_2.matcher(str).find()) {
            return true;
        }
        // 检测到危险的HTML属性
        if (PATTERN_HTML_ATTR.matcher(str).find()) {
            return true;
        }
        // 检测到十六进制编码的XSS尝试
        if (PATTERN_HEX.matcher(str).find()) {
            return true;
        }
        return false;
    }

}