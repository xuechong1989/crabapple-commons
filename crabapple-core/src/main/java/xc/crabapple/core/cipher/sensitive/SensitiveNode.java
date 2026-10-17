package xc.crabapple.core.cipher.sensitive;

import java.util.TreeSet;

/**
 * 敏感词节点，每个节点包含了以相同的2个字符开头的所有词
 */
public class SensitiveNode {

    public int headTwoCharMix;  // 头两个字符的mix，mix相同，两个字符相同
    public SensitiveNode next;  // 下一个节点
    public TreeSet<SensitiveString> treeSet = null;   // 所有以这两个字符开头的词表

    public SensitiveNode(final int headTwoCharMix) {
        p_treeSetInit();
        this.headTwoCharMix = headTwoCharMix;
    }

    public SensitiveNode(final int headTwoCharMix, final SensitiveNode parent) {
        p_treeSetInit();
        this.headTwoCharMix = headTwoCharMix;
        parent.next = this;
    }

    private void p_treeSetInit(){
        treeSet = new TreeSet<>();
    }

}