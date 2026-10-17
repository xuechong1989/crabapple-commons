package xc.crabapple.core.cipher.caesar;

/**
 * 2023-07-30
 * 凯撒加密算法
 * 仅处理：数字0-9，小写字母a-z，大写字母A-Z
 */
public final class CaesarUtil {

    /**
     * 2023-07-30
     * 加密
     * 仅处理：数字0-9，小写字母a-z，大写字母A-Z
     */
    public static final String encrypt(final String plaintext){
        final String cipher = encrypt(plaintext, 30);
        return cipher;
    }

    /**
     * 2023-07-30
     * 仅处理：数字0-9，小写字母a-z，大写字母A-Z
     * 加密
     * 示例： ("=12_aa#中文D", 1) ==> =23_bb#中文E
     * @param plaintext 待加密的明文
     * @param offset 偏移量
     */
    public static final String encrypt(final String plaintext, final int offset){
        final StringBuilder stringBuilder = new StringBuilder();
        //
        for(int i = 0; i < plaintext.length(); i++){
            char chr = plaintext.charAt(i);
            // 小写字母的转换
            if(chr >= 'a' && chr <= 'z'){
                chr += offset % 26;
                //  向左超界
                if(chr < 'a'){
                    chr += 26;
                }
                //  向右超界
                if(chr > 'z'){
                    chr -= 26;
                }
                stringBuilder.append(chr);
                continue;
            }
            // 大写字母的转换
            if(chr >= 'A' && chr <= 'Z'){
                chr += offset % 26;
                //  向左超界
                if(chr < 'A'){
                    chr += 26;
                }
                //  向右超界
                if(chr > 'Z'){
                    chr -= 26;
                }
                stringBuilder.append(chr);
                continue;
            }
            // 数字的转换
            if(chr >= '0' && chr <= '9'){
                chr += offset % 10;
                //  向左超界
                if(chr < '0'){
                    chr += 10;
                }
                //  向右超界
                if(chr > '9'){
                    chr -= 10;
                }
                stringBuilder.append(chr);
                continue;
            }
            //
            stringBuilder.append(chr);
        }
        //
        final String cipher = stringBuilder.toString();
        return cipher;
    }

    /**
     * 2023-07-30
     * 仅处理：数字0-9，小写字母a-z，大写字母A-Z
     * 解密
     */
    public static final String decrypt(final String cipher){
        final String str = decrypt(cipher, 30);
        return str;
    }

    /**
     * 2023-07-30
     * 仅处理：数字0-9，小写字母a-z，大写字母A-Z
     * 解密
     * 示例： ("=12_aa#中文D", 1) ==> =23_bb#中文E
     * 如果密文是向前移位，解密时就向后移位。
     * 如果密文是向后移位，解密时就向前移位。
     * @param cipher 待解密的密文
     * @param offset 偏移量
     */
    public static final String decrypt(final String cipher, final int offset){
        final StringBuilder stringBuilder = new StringBuilder();
        //
        int effectiveOffset = 0 - offset;
        //
        for(int i = 0; i < cipher.length(); i++){
            char chr = cipher.charAt(i);
            // 小写字母的转换
            if(chr >= 'a' && chr <= 'z'){
                chr += effectiveOffset % 26;
                //  向左超界
                if(chr < 'a'){
                    chr += 26;
                }
                //向右超界
                if(chr > 'z'){
                    chr -= 26;
                }
                stringBuilder.append(chr);
                continue;
            }
            // 大写字母的转换
            if(chr >= 'A' && chr <= 'Z'){
                chr += effectiveOffset % 26;
                //  向左超界
                if(chr < 'A'){
                    chr += 26;
                }
                //  向右超界
                if(chr > 'Z'){
                    chr -= 26;
                }
                stringBuilder.append(chr);
                continue;
            }
            // 数字的转换
            if(chr >= '0' && chr <= '9'){
                chr += effectiveOffset % 10;
                //  向左超界
                if(chr < '0'){
                    chr += 10;
                }
                //  向右超界
                if(chr > '9'){
                    chr -= 10;
                }
                stringBuilder.append(chr);
                continue;
            }
            //
            stringBuilder.append(chr);
        }
        //
        final String str = stringBuilder.toString();
        return str;
    }

}