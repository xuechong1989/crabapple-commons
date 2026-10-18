package xc.crabapple.core.cipher.sensitive;

import xc.crabapple.core.data.CrabappleData;

public class SensitiveString extends CrabappleData implements CharSequence, Comparable<SensitiveString>{

    private final char[] value;
    private final int offset;
    public final int length;
    private int hash = 0;

    public SensitiveString(final String str){
        value = str.toCharArray();
        offset = 0;
        length = value.length;
    }

    public SensitiveString(final char[] value, final int offset, final int length){
        this.value = value;
        this.offset = offset;
        this.length = length;
    }

    /**
     * 计算该位置后（包含）2个字符的hash值
     * @param i 从 0 到 length - 2
     * @return hash值
     */
    public int nextTwoCharHash(final int i){
        return 31 * value[offset + i] + value[offset + i + 1];
    }

    /**
     * 计算该位置后（包含）2个字符和为1个int型的值
     * int值相同表示2个字符相同
     * @param i 从 0 到 length - 2
     * @return int值
     */
    public int nextTwoCharMix(final int i){
        return (value[offset + i] << 16) | value[offset + i + 1];
    }

    /**
     * 该位置后（包含）的字符串，是否以某个词（word）开头
     * @param i 从 0 到 length - 2
     * @param word 词
     * @return 是否？
     */
    public boolean nextStartsWith(final int i, final SensitiveString word){
        // 是否长度超出
        if(word.length > length - i){
            return false;
        }
        // 从尾开始判断
        for(int c =  word.length - 1; c >= 0; c --){
            if(value[offset + i + c] != word.value[word.offset + c]){
                return false;
            }
        }
        return true;
    }

    /**
     * 填充（替换）
     * @param begin 从此位置开始（含）
     * @param end 到此位置结束（不含）
     * @param fillWith 以此字符填充（替换）
     */
    public void fill(final int begin, final int end, final char fillWith){
        for(int i = begin; i < end; i ++){
            value[offset + i] = fillWith;
        }
    }

    public int length(){
        return length;
    }

    public char charAt(final int i){
        return value[offset + i];
    }

    public SensitiveString substring(final int begin){
        return new SensitiveString(value, offset + begin, length - begin);
    }

    public SensitiveString substring(final int begin, final int end){
        return new SensitiveString(value, offset + begin, end - begin);
    }

    @Override
    public String toString(){
        String str = new String(value, offset, length);
        return str;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0 && length > 0) {
            for (int i = 0; i < length; i++) {
                h = 31 * h + value[offset + i];
            }
            hash = h;
        }
        return h;
    }

    public boolean equals(final Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof SensitiveString) {
            SensitiveString that = (SensitiveString)anObject;
            if (length == that.length) {
                char v1[] = this.value;
                char v2[] = that.value;
                for(int i = 0; i < this.length; i ++){
                    if(v1[this.offset + i] != v2[that.offset + i]){
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(final SensitiveString that) {
        int len1 = this.length;
        int len2 = that.length;
        int lim = Math.min(len1, len2);
        char v1[] = this.value;
        char v2[] = that.value;

        int k = 0;
        while (k < lim) {
            char c1 = v1[this.offset + k];
            char c2 = v2[that.offset + k];
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        return substring(start, end);
    }

}