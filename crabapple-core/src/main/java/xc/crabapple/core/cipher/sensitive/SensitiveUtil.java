package xc.crabapple.core.cipher.sensitive;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.ListUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.json.jackson.JacksonUtil;
import xc.crabapple.core.log.LogUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2024-12-27
 * 敏感词处理工具类
 * 敏感词过滤器，以过滤速度优化为主。基于DFA算法（Deterministic Finite Automaton）实现。
 * DFA全称为：Deterministic Finite Automaton,即确定有穷自动机。
 * 其特征为：有一个有限状态集合和一些从一个状态通向另一个状态的边，每条边上标记有一个符号，其中一个状态是初态，某些状态是终态。
 * 但不同于不确定的有限自动机，DFA中不会有从同一状态出发的两条边标志有相同的符号。
 */
public final class SensitiveUtil {

    private static boolean p_initFlag = false;
    // 为2的n次方，考虑到敏感词大概在10k左右，这个数量应为词数的数倍，使得桶很稀疏。提高不命中时hash指向null的概率，加快访问速度。
    private static int DEFAULT_INITIAL_CAPACITY = 131072;
    private static SensitiveNode[] p_sensitiveNodeArray = null;  // 类似HashMap的桶，比较稀疏。使用2个字符的hash定位。
    private static ConcurrentHashMap<String, Integer> p_sensitivewordMap = null;

    private static final void p_init(){
        if(p_initFlag){
            return;
        }
        p_initFlag = true;
        DEFAULT_INITIAL_CAPACITY = 131072;
        p_sensitiveNodeArray = new SensitiveNode[DEFAULT_INITIAL_CAPACITY];
        p_sensitivewordMap = new ConcurrentHashMap<>(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * 2024-12-27 方法定义
     * 2025-04-17 真正实现
     * 从指定的文件中批量导入敏感词库
     * @param url
     * @return
     */
    public static final boolean addByFile(final String url){
        p_init();
        //
        List<String> list = FileUtil.fileReadList(url);
        for(int i = 0; i < list.size(); i++) {
            String line = list.get(i);
            line = StringUtil.getStandardStr(line);
            if (StringUtil.isBlank(line)) {
                continue;
            }
            add(line);
        }
        return true;
    }

    /**
     * 2024-12-27
     * 增加一个敏感词，如果词的长度（trim后）小于2，则丢弃
     * 此方法（构建）并不是主要的性能优化点。
     * @param word
     */
    public static final boolean add(final String word) {
        p_init();
        p_sensitivewordMap.put(word, 1);
        // 长度小于2的不加入
        if (word == null || word.trim().length() < 2) {
            return false;
        }
        // 两个字符的不考虑
        if (word.length() == 2 && word.matches("\\w\\w")) {
            return false;
        }
        SensitiveString sp = new SensitiveString(word.trim());
        // 计算头两个字符的hash
        int hash = sp.nextTwoCharHash(0);
        // 计算头两个字符的mix表示（mix相同，两个字符相同）
        int mix = sp.nextTwoCharMix(0);
        // 转为在hash桶中的位置
        int index = hash & (p_sensitiveNodeArray.length - 1);
        // 从桶里拿第一个节点
        SensitiveNode node = p_sensitiveNodeArray[index];
        if (node == null) {
            // 如果没有节点，则放进去一个
            node = new SensitiveNode(mix);
            // 并添加词
            node.treeSet.add(sp);
            // 放入桶里
            p_sensitiveNodeArray[index] = node;
        } else {
            // 如果已经有节点（1个或多个），找到正确的节点
            for (; node != null; node = node.next) {
                // 匹配节点
                if (node.headTwoCharMix == mix) {
                    node.treeSet.add(sp);
                    return true;
                }
                // 如果匹配到最后仍然不成功，则追加一个节点
                if (node.next == null) {
                    new SensitiveNode(mix, node).treeSet.add(sp);
                    return true;
                }
            }
        }
        return true;
    }

    /**
     * 2024-12-27
     * 检查plaintext是否含有敏感词，含有敏感词则返回true，不含敏感词则返回false
     * @param plaintext
     * @return
     */
    public static final ObjectNode check(final String plaintext){
        ObjectNode objectNode = JacksonUtil.createObjectNode();
        Map<String, Object> map = p_handler(plaintext, "*");
        List<String> wordlist = (List<String>) map.get("sensitiveList");
        String replacedtext = (String) map.get("replacedtext");
        if(CollectionUtil.isBlank(wordlist)){
            objectNode.put("sensitiveList", "");
            objectNode.put("replacedtext", "");
            objectNode.put("status", false);
            return objectNode;
        }
        objectNode.put("sensitiveList", JacksonUtil.obj2json(wordlist));
        objectNode.put("replacedtext", JacksonUtil.obj2json(replacedtext));
        objectNode.put("status", true);
        return objectNode;
    }

    /**
     * 2025-04-17
     * 含有敏感词则返回map集合，不含敏感词则返回null
     * @param plaintext
     * @return {"sensitiveList":[], "plaintext":"", "replacedtext":""}
     */
    public static final Map<String, Object> checkWithDetail(final String plaintext){
        Map<String, Object> map = p_handler(plaintext, "*");
        List<String> sensitiveList = (List<String>) map.get("sensitiveList");
        if(CollectionUtil.isBlank(sensitiveList)){
            return null;
        }
        return map;
    }

    /**
     * 2024-12-27
     * 对源句子plaintext进行敏感词处理
     * @param plaintext 源句子
     */
    public static final Map<String, Object> replace(final String plaintext) {
        final Map<String, Object> map = p_handler(plaintext, "*");
        return map;
    }

    /**
     * 2024-12-27
     * 2025-04-17 添加敏感词为一个字时的处理
     * 对源句子plaintext进行敏感词处理
     * @param plaintext 源句子
     * @param toReplaceStr 敏感词的替换字符
     * @return 返回一个map集合
     */
    private static final Map<String, Object> p_handler(final String plaintext, final String toReplaceStr) {
        Map<String, Object> map = new HashMap();
        map.put("sensitiveList", null);  // 敏感词列表
        map.put("plaintext", plaintext);  // 原句子
        map.put("replacedtext", plaintext);  // 替换之后的句子
        if(StringUtil.isBlank(plaintext)){
            return map;
        }
        //
        String effectiveToReplaceStr = StringUtil.trim(toReplaceStr);
        if(StringUtil.isBlank(effectiveToReplaceStr)){
            effectiveToReplaceStr = "*";
        }
        if(effectiveToReplaceStr.length() >= 2){
            LogUtil.error("", "toReplaceStr参数长度必须为1", plaintext, effectiveToReplaceStr);
            return map;
        }
        //
        int plaintextLength = plaintext.length();
        if(plaintextLength == 1){
            if(p_sensitivewordMap.containsKey(plaintext)){
                List<String> sensitiveList = new ArrayList<>();
                sensitiveList.add(plaintext);
                map.put("sensitiveList", sensitiveList);  // 敏感词列表
                map.put("plaintext", plaintext);  // 原句子
                map.put("replacedtext", "*");  // 替换之后的句子
                return map;
            }
        }
        //
        final char toChar = StringUtil.str2char(effectiveToReplaceStr);
        // 先转换为StringPointer
        SensitiveString stringPointer = new SensitiveString(plaintext);
        // 标示是否替换
        boolean replaced = false;
        // 匹配的起始位置
        int i = 0;
        while (i <= stringPointer.length - 2) {
            /*
             * 移动到下一个匹配位置的步进：
             * 如果未匹配为1，如果匹配是匹配的词长度
             */
            int step = 1;
            // 计算此位置开始2个字符的hash
            int hash = stringPointer.nextTwoCharHash(i);
            /*
             * 根据hash获取第一个节点，
             * 真正匹配的节点可能不是第一个，
             * 所以有后面的for循环。
             */
            SensitiveNode node = p_sensitiveNodeArray[hash & (p_sensitiveNodeArray.length - 1)];
            /*
             * 如果非敏感词，node基本为null。
             * 这一步大幅提升效率
             */
            if (node != null) {
                /*
                 * 如果能拿到第一个节点，
                 * 才计算mix（mix相同表示2个字符相同）。
                 * mix的意义和HashMap先hash再equals的equals部分类似。
                 */
                int mix = stringPointer.nextTwoCharMix(i);
                /*
                 * 循环所有的节点，如果非敏感词，
                 * mix相同的概率非常低，提高效率
                 */
                outer:
                for (; node != null; node = node.next) {
                    /*
                     * 对于一个节点，先根据头2个字符判断是否属于这个节点。
                     * 如果属于这个节点，看这个节点的词库是否命中。
                     * 此代码块中访问次数已经很少，不是优化重点
                     */
                    if (node.headTwoCharMix == mix) {
                        /*
                         * 查出比剩余sentence小的最大的词。
                         * 例如剩余sentence为"色情电影哪家强？"，
                         * 这个节点含三个词从小到大为："色情"、"色情电影"、"色情信息"。
                         * 则从"色情电影"开始向前匹配
                         */
                        SensitiveString stringPt = stringPointer.substring(i);
                        NavigableSet<SensitiveString> navigableSet = node.treeSet.headSet(stringPt, true);
                        if (navigableSet != null) {
                            for (SensitiveString tmpStringPointer : navigableSet.descendingSet()) {
                                /*
                                 * 仍然需要再判断一次，例如"色情信息哪里有？"，
                                 * 如果节点只包含"色情电影"一个词，
                                 * 仍然能够取到word为"色情电影"，但是不该匹配。
                                 */
                                if (stringPointer.nextStartsWith(i, tmpStringPointer)) {
                                    // 匹配成功，将匹配的部分，用replace制定的内容替代
                                    stringPointer.fill(i, i + tmpStringPointer.length, toChar);
                                    // 跳过已经替代的部分
                                    step = tmpStringPointer.length;
                                    String word = tmpStringPointer.toString();
                                    List<String> wordlist = (List<String>) map.get("sensitiveList");
                                    if(CollectionUtil.isBlank(wordlist)){
                                        wordlist = new ArrayList<>();
                                    }
                                    wordlist.add(word);
                                    map.put("sensitiveList", wordlist);
                                    // 标示有替换
                                    replaced = true;
                                    // 跳出循环（然后是while循环的下一个位置）
                                    break outer;
                                }
                            }
                        }
                    }
                }
            }
            // 移动到下一个匹配位置
            i += step;
        }
        // 如果没有替换，直接返回入参（节约String的构造copy）
        if (replaced) {
            String replacedtext = stringPointer.toString();
            map.put("replacedtext", replacedtext);
            return map;
        }
        return map;
    }

}