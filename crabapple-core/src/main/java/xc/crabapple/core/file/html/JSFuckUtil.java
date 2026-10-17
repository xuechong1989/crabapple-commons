package xc.crabapple.core.file.html;

import java.util.HashMap;
import java.util.Map;

/**
 * 2025-02-28
 * java实现jsfuck工具类
 */
public class JSFuckUtil {

    /**
     * 基础字符映射表（核心逻辑）
     */
    private static Map<Character, String> CHAR_TO_JSFUCK = null;

    public static final void init() {
        CHAR_TO_JSFUCK = new HashMap<>();
        // 初始化基本字符的 JSFuck 表达式
        CHAR_TO_JSFUCK.put('a', "(![]+[])[+!![]]");
        CHAR_TO_JSFUCK.put('b', "({}+[])[!![]+!![]]");
        CHAR_TO_JSFUCK.put('c', "({}+[])[!![]+!![]+!![]]");
        CHAR_TO_JSFUCK.put('d', "([][[]]+[])[!![]+!![]]");
        CHAR_TO_JSFUCK.put('e', "(![]+[])[!![]+!![]+!![]]");
        CHAR_TO_JSFUCK.put('f', "(![]+[])[+[]]");
        CHAR_TO_JSFUCK.put('i', "([][[]]+[])[!![]+!![]+!![]+!![]+!![]]");
        CHAR_TO_JSFUCK.put('j', "({}+[])[!![]+!![]+!![]]");
        CHAR_TO_JSFUCK.put('l', "(![]+[])[!![]+!![]]");
        CHAR_TO_JSFUCK.put('n', "([][[]]+[])[+!![]]");
        CHAR_TO_JSFUCK.put('o', "({}+[])[+!![]]");
        CHAR_TO_JSFUCK.put('r', "(!![]+[])[+[]]");
        CHAR_TO_JSFUCK.put('s', "(![]+[])[!![]+!![]+!![]]");
        CHAR_TO_JSFUCK.put('t', "(!![]+[])[+!![]]");
        CHAR_TO_JSFUCK.put('u', "(!![]+[])[!![]+!![]]");
        CHAR_TO_JSFUCK.put(' ', "({}+[])[!![]+!![]+!![]+!![]+!![]+!![]+!![]]");
        CHAR_TO_JSFUCK.put('[', "[][[]]");
        CHAR_TO_JSFUCK.put(']', "([]+[])[!![]+!![]+!![]+!![]+!![]]");
        CHAR_TO_JSFUCK.put('(', "([][[]]+[])[!![]+!![]+!![]+!![]]");
        CHAR_TO_JSFUCK.put(')', "([][[]]+[])[!![]+!![]+!![]+!![]+!![]]");
        CHAR_TO_JSFUCK.put('+', "+(+!![]+(+!![]))");
        // 数字 0-9
        CHAR_TO_JSFUCK.put('0', "+[]");
        CHAR_TO_JSFUCK.put('1', "+!![]");
        for (int i = 2; i <= 9; i++) {
            CHAR_TO_JSFUCK.put((char) ('0' + i), "+(" + "+!![]".repeat(i) + ")");
        }
    }

    /**
     * 递归生成复杂字符的表达式（如 'S' => 'String.fromCharCode(...)'）
     * @param c
     * @return
     */
    private static final String charToJSFuck(final char c) {
        if (CHAR_TO_JSFUCK.containsKey(c)) {
            return CHAR_TO_JSFUCK.get(c);
        }
        // 处理未直接映射的字符（通过 ASCII 码构造）
        return "String.fromCharCode(" + encodeNumber(c) + ")";
    }

    /**
     *  将数字转换为 JSFuck 表达式（例如 97 => +!![]+...）
     * @param num
     * @return
     */
    private static final String encodeNumber(final int num) {
        if (num == 0) return "+[]";
        StringBuilder sb = new StringBuilder();
        int numLocal = num;
        while (numLocal > 0) {
            int digit = numLocal % 10;
            sb.insert(0, CHAR_TO_JSFUCK.get((char) ('0' + digit)));
            if (numLocal >= 10) sb.insert(0, "+");
            numLocal /= 10;
        }
        return sb.toString();
    }

    /**
     * 编码完整字符串
     */
    public static final String encode(final String input) {
        StringBuilder output = new StringBuilder();
        for (char c : input.toCharArray()) {
            String jsFuckChar = charToJSFuck(c);
            output.append("+").append(jsFuckChar);
        }
        return output.length() > 0 ? output.substring(1) : "";
    }

}