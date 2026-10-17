package xc.crabapple.core.cipher.ska;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.AsciiUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.cipher.caesar.CaesarUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 2023-07-30
 * SKA-自定义安全秘钥算法（Secret Key Algorithm）
 * 秘钥算法管理类，该类相当于算法盐
 * 在实际项目中所有加解密操作前均要使用该类对秘钥进行处理
 */
public final class SkaUtil {

    private static boolean p_inited = false;
    public static final String ENCRYPT_PRIVATE = "private";  // 加解密类型，private-私密（适用于服务器内部）
    public static final String ENCRYPT_PUBLIC = "public";  // 加解密类型，public-公开（适用于前后端交互）
    private static final Integer ITERATORS = 30;
    private static Map<Integer, Integer> p_ascMap = null;
    private static Map<Integer, Integer> p_descMap = null;
    private static Map<Integer, Integer> p_ascPubMap = null;
    private static Map<Integer, Integer> p_descPubMap = null;

    /**
     * 2023-07-31
     * 初始化自定义密码本
     * 非公开，私有映射本，仅可在系统内部或服务器之间处理，不能放置在客户端
     */
    private static synchronized final void p_init(){
        if(p_inited){
            return;
        }
        p_inited = true;
        // 私钥管理
        p_ascMap = new HashMap<>(94);
        // 数字 0-9，ASCII码：48-57
        p_ascMap.put(48, 54);  // 0
        p_ascMap.put(49, 52);  // 1
        p_ascMap.put(50, 53);  // 2
        p_ascMap.put(51, 57);  // 3
        p_ascMap.put(52, 48);  // 4
        p_ascMap.put(53, 55);  // 5
        p_ascMap.put(54, 56);  // 6
        p_ascMap.put(55, 50);  // 7
        p_ascMap.put(56, 49);  // 8
        p_ascMap.put(57, 51);  // 9
        // 大写字母A-Z，ASCII码为：65-90
        p_ascMap.put(65, 69);  // A
        p_ascMap.put(66, 75);  // B
        p_ascMap.put(67, 66);  // C
        p_ascMap.put(68, 83);  // D
        p_ascMap.put(69, 74);  // E
        p_ascMap.put(70, 77);  // F
        p_ascMap.put(71, 88);  // G
        p_ascMap.put(72, 65);  // H
        p_ascMap.put(73, 79);  // I
        p_ascMap.put(74, 86);  // J
        p_ascMap.put(75, 76);  // K
        p_ascMap.put(76, 87);  // L
        p_ascMap.put(77, 89);  // M
        p_ascMap.put(78, 67);  // N
        p_ascMap.put(79, 84);  // O
        p_ascMap.put(80, 72);  // P
        p_ascMap.put(81, 68);  // Q
        p_ascMap.put(82, 81);  // R
        p_ascMap.put(83, 90);  // S
        p_ascMap.put(84, 80);  // T
        p_ascMap.put(85, 71);  // U
        p_ascMap.put(86, 85);  // V
        p_ascMap.put(87, 73);  // W
        p_ascMap.put(88, 78);  // X
        p_ascMap.put(89, 82);  // Y
        p_ascMap.put(90, 70);  // Z
        // 小写字母a-z，ASCII码为：97-122
        p_ascMap.put(97, 104);  // a
        p_ascMap.put(98, 103);  // b
        p_ascMap.put(99, 101);  // c
        p_ascMap.put(100, 114);  // d
        p_ascMap.put(101, 98);  // e
        p_ascMap.put(102, 108);  // f
        p_ascMap.put(103, 115);  // g
        p_ascMap.put(104, 110);  // h
        p_ascMap.put(105, 107);  // i
        p_ascMap.put(106, 100);  // j
        p_ascMap.put(107, 109);  // k
        p_ascMap.put(108, 97);  // l
        p_ascMap.put(109, 120);  // m
        p_ascMap.put(110, 121);  // n
        p_ascMap.put(111, 99);  // o
        p_ascMap.put(112, 113);  // p
        p_ascMap.put(113, 119);  // q
        p_ascMap.put(114, 117);  // r
        p_ascMap.put(115, 102);  // s
        p_ascMap.put(116, 112);  // t
        p_ascMap.put(117, 106);  // u
        p_ascMap.put(118, 111);  // v
        p_ascMap.put(119, 118);  // w
        p_ascMap.put(120, 122);  // x
        p_ascMap.put(121, 105);  // y
        p_ascMap.put(122, 116);  // z
        // 特殊字符 33,34,35,36,37,38,39,40,41,42,43,44,45,46,47
        p_ascMap.put(33, 38);  // !
        p_ascMap.put(34, 39);  // "
        p_ascMap.put(35, 43);  // #
        p_ascMap.put(36, 42);  // $
        p_ascMap.put(37, 47);  // %
        p_ascMap.put(38, 37);  // &
        p_ascMap.put(39, 44);  // '
        p_ascMap.put(40, 33);  // (
        p_ascMap.put(41, 46);  // )
        p_ascMap.put(42, 40);  // *
        p_ascMap.put(43, 45);  // +
        p_ascMap.put(44, 34);  // ,
        p_ascMap.put(45, 41);  // -
        p_ascMap.put(46, 35);  // .
        p_ascMap.put(47, 36); // /
        // 特殊字符 58,59,60,61,62,63,64
        p_ascMap.put(58, 64);  // :
        p_ascMap.put(59, 61);  // ;
        p_ascMap.put(60, 59);  // <
        p_ascMap.put(61, 58);  // =
        p_ascMap.put(62, 60);  // >
        p_ascMap.put(63, 63);  // ?
        p_ascMap.put(64, 62);  // @
        // 特殊字符 91,92,93,94,95
        p_ascMap.put(91, 94);  // [
        p_ascMap.put(92, 91);  // \
        p_ascMap.put(93, 95);  // ]
        p_ascMap.put(94, 93);  // ^
        p_ascMap.put(95, 92);  // _
        p_ascMap.put(96, 96);  // `
        // 特殊字符 123,124,125,126
        p_ascMap.put(123, 125);  // {
        p_ascMap.put(124, 126);  // |
        p_ascMap.put(125, 123);  // }
        p_ascMap.put(126, 124);  // ~
        //
        p_descMap = new HashMap<>(94);
        final Set<Integer> set = p_ascMap.keySet();
        final Iterator<Integer> iterator = set.iterator();
        while(iterator.hasNext()){
            final Integer key = iterator.next();
            final Integer value = p_ascMap.get(key);
            p_descMap.put(value, key);
        }
        //
        // 公钥管理
        p_ascPubMap = new HashMap<>(94);
        // 数字 0-9，ASCII码：48-57
        p_ascPubMap.put(48, 53);  // 0
        p_ascPubMap.put(49, 50);  // 1
        p_ascPubMap.put(50, 51);  // 2
        p_ascPubMap.put(51, 49);  // 3
        p_ascPubMap.put(52, 54);  // 4
        p_ascPubMap.put(53, 48);  // 5
        p_ascPubMap.put(54, 57);  // 6
        p_ascPubMap.put(55, 52);  // 7
        p_ascPubMap.put(56, 56);  // 8
        p_ascPubMap.put(57, 55);  // 9
        // 大写字母A-Z，ASCII码为：65-90
        p_ascPubMap.put(65, 80);  // A
        p_ascPubMap.put(66, 66);  // B
        p_ascPubMap.put(67, 85);  // C
        p_ascPubMap.put(68, 89);  // D
        p_ascPubMap.put(69, 87);  // E
        p_ascPubMap.put(70, 68);  // F
        p_ascPubMap.put(71, 74);  // G
        p_ascPubMap.put(72, 73);  // H
        p_ascPubMap.put(73, 72);  // I
        p_ascPubMap.put(74, 71);  // J
        p_ascPubMap.put(75, 69);  // K
        p_ascPubMap.put(76, 78);  // L
        p_ascPubMap.put(77, 76);  // M
        p_ascPubMap.put(78, 65);  // N
        p_ascPubMap.put(79, 70);  // O
        p_ascPubMap.put(80, 79);  // P
        p_ascPubMap.put(81, 84);  // Q
        p_ascPubMap.put(82, 75);  // R
        p_ascPubMap.put(83, 81);  // S
        p_ascPubMap.put(84, 83);  // T
        p_ascPubMap.put(85, 67);  // U
        p_ascPubMap.put(86, 82);  // V
        p_ascPubMap.put(87, 88);  // W
        p_ascPubMap.put(88, 90);  // X
        p_ascPubMap.put(89, 86);  // Y
        p_ascPubMap.put(90, 77);  // Z
        // 小写字母a-z，ASCII码为：97-122
        p_ascPubMap.put(97, 122);  // a
        p_ascPubMap.put(98, 117);  // b
        p_ascPubMap.put(99, 110);  // c
        p_ascPubMap.put(100, 101);  // d
        p_ascPubMap.put(101, 121);  // e
        p_ascPubMap.put(102, 115);  // f
        p_ascPubMap.put(103, 118);  // g
        p_ascPubMap.put(104, 104);  // h
        p_ascPubMap.put(105, 102);  // i
        p_ascPubMap.put(106, 97);  // j
        p_ascPubMap.put(107, 100);  // k
        p_ascPubMap.put(108, 106);  // l
        p_ascPubMap.put(109, 108);  // m
        p_ascPubMap.put(110, 120);  // n
        p_ascPubMap.put(111, 116);  // o
        p_ascPubMap.put(112, 103);  // p
        p_ascPubMap.put(113, 111);  // q
        p_ascPubMap.put(114, 98);  // r
        p_ascPubMap.put(115, 112);  // s
        p_ascPubMap.put(116, 105);  // t
        p_ascPubMap.put(117, 114);  // u
        p_ascPubMap.put(118, 113);  // v
        p_ascPubMap.put(119, 99);  // w
        p_ascPubMap.put(120, 109);  // x
        p_ascPubMap.put(121, 119);  // y
        p_ascPubMap.put(122, 107);  // z
        // 特殊字符 33,34,35,36,37,38,39,40,41,42,43,44,45,46,47
        p_ascPubMap.put(33, 44);  // !
        p_ascPubMap.put(34, 47);  // "
        p_ascPubMap.put(35, 45);  // #
        p_ascPubMap.put(36, 43);  // $
        p_ascPubMap.put(37, 42);  // %
        p_ascPubMap.put(38, 38);  // &
        p_ascPubMap.put(39, 33);  // '
        p_ascPubMap.put(40, 41);  // (
        p_ascPubMap.put(41, 35);  // )
        p_ascPubMap.put(42, 34);  // *
        p_ascPubMap.put(43, 37);  // +
        p_ascPubMap.put(44, 39);  // ,
        p_ascPubMap.put(45, 46);  // -
        p_ascPubMap.put(46, 36);  // .
        p_ascPubMap.put(47, 40); // /
        // 特殊字符 58,59,60,61,62,63,64
        p_ascPubMap.put(58, 61);  // :
        p_ascPubMap.put(59, 62);  // ;
        p_ascPubMap.put(60, 58);  // <
        p_ascPubMap.put(61, 60);  // =
        p_ascPubMap.put(62, 59);  // >
        p_ascPubMap.put(63, 64);  // ?
        p_ascPubMap.put(64, 63);  // @
        // 特殊字符 91,92,93,94,96
        p_ascPubMap.put(91, 95);  // [
        p_ascPubMap.put(92, 94);  // \
        p_ascPubMap.put(93, 96);  // ]
        p_ascPubMap.put(94, 93);  // ^
        p_ascPubMap.put(95, 91);  // _
        p_ascPubMap.put(96, 92);  // `
        // 特殊字符 123,124,125,126
        p_ascPubMap.put(123, 124);  // {
        p_ascPubMap.put(124, 125);  // |
        p_ascPubMap.put(125, 126);  // }
        p_ascPubMap.put(126, 123);  // ~
        //
        p_descPubMap = new HashMap<>(94);
        final Set<Integer> pubSet = p_ascPubMap.keySet();
        final Iterator<Integer> pubIterator = pubSet.iterator();
        while(pubIterator.hasNext()){
            final Integer key = pubIterator.next();
            final Integer value = p_ascPubMap.get(key);
            p_descPubMap.put(value, key);
        }
    }

    /**
     * 2023-07-31
     */
    public static final String encrypt(final String plaintext){
        final String cipher = encrypt(ENCRYPT_PRIVATE, plaintext, ITERATORS);
        return cipher;
    }

    /**
     * 2023-07-31
     */
    public static final String encrypt(final String plaintext, final Integer interators){
        final String cipher = encrypt(ENCRYPT_PRIVATE, plaintext, interators);
        return cipher;
    }

    /**
     * 2023-07-31
     */
    public static final String encrypt(final String encryptType, final String plaintext){
        final String cipher = encrypt(encryptType, plaintext, ITERATORS);
        return cipher;
    }

    /**
     * 2023-07-31
     * 非公开，私有映射本，仅可在系统内部或服务器之间处理，不能放置在客户端
     * @param encryptType 加解密类型
     * @param plaintext 待加密的明文
     * @param interators 迭代次数
     * @return
     */
    public static final String encrypt(final String encryptType, final String plaintext, final Integer interators){
        if (!StringUtil.equalsOfOne(encryptType, ENCRYPT_PRIVATE, ENCRYPT_PUBLIC)) {
            LogUtil.error("SkaUtil_encrypt_1", "必须指明加解密类型，公开或私密", encryptType);
            return null;
        }
        //
        if(plaintext == null){
            LogUtil.error("SkaUtil_encrypt_2", "明文不能为空", encryptType);
            return null;
        }
        //
        p_init();
        //
        Integer effectiveInterators = interators;
        if(effectiveInterators <= 0){
            effectiveInterators = ITERATORS;
        }
        // 凯撒算法
        String cipher = CaesarUtil.encrypt(plaintext, effectiveInterators);
        // 自定义密码本
        final StringBuilder stringBuilder = new StringBuilder();
        final int length = cipher.length();
        for(int i = 0; i < length; i++){
            final char chr = cipher.charAt(i);
            final Integer key = AsciiUtil.char2asciiCode(chr);
            Integer value = null;
            if(StringUtil.equals(encryptType, ENCRYPT_PRIVATE)){
                value = p_ascMap.get(key);
            }else if(StringUtil.equals(encryptType, ENCRYPT_PUBLIC)){
                value = p_ascPubMap.get(key);
            }else{
                LogUtil.error("SkaUtil_encrypt_3", "必须指明加解密类型",  encryptType);
                return "";
            }
            if(value == null){
                LogUtil.error("SkaUtil_encrypt_4", "加密值错误",  encryptType);
                return "";
            }
            final String sss = AsciiUtil.asciiCode2sss(value);
            stringBuilder.append(sss);
        }
        cipher = stringBuilder.toString();
        //
        return cipher;
    }

    /**
     * 2023-07-31
     */
    public static final String decrypt(final String cipher){
        final String plaintext = decrypt(ENCRYPT_PRIVATE, cipher, ITERATORS);
        return plaintext;
    }

    /**
     * 2023-07-31
     */
    public static final String decrypt(final String cipher, final Integer interators){
        final String plaintext = decrypt(ENCRYPT_PRIVATE, cipher, interators);
        return plaintext;
    }

    /**
     * 2023-07-31
     */
    public static final String decrypt(final String encryptType, final String cipher){
        final String plaintext = decrypt(encryptType, cipher, ITERATORS);
        return plaintext;
    }

    /**
     * 2023-07-31
     * 非公开，私有映射本，仅可在系统内部或服务器之间处理，不能放置在客户端
     * @param encryptType 加解密类型
     * @param cipher 待解密的密文
     * @param interators 迭代次数
     * @return
     */
    public static final String decrypt(final String encryptType, final String cipher, final Integer interators){
        if (!StringUtil.equalsOfOne(encryptType, ENCRYPT_PRIVATE, ENCRYPT_PUBLIC)) {
            LogUtil.error("SkaUtil_decrypt_1", "必须指明加解密类型，公开或私密", encryptType);
            return null;
        }
        //
        if(cipher == null){
            LogUtil.error("SkaUtil_decrypt_2", "密文不能为空", encryptType);
            return null;
        }
        //
        p_init();
        //
        Integer effectiveInterators = interators;
        if(effectiveInterators <= 0){
            effectiveInterators = ITERATORS;
        }
        // 自定义密码本
        final StringBuilder stringBuilder = new StringBuilder();
        final int length = cipher.length();
        for(int i = 0; i < length; i++){
            final char chr = cipher.charAt(i);
            final Integer key = AsciiUtil.char2asciiCode(chr);
            Integer value = null;
            if(StringUtil.equals(encryptType, ENCRYPT_PRIVATE)){
                value = p_descMap.get(key);
            }else if(StringUtil.equals(encryptType, ENCRYPT_PUBLIC)){
                value = p_descPubMap.get(key);
            }else{
                LogUtil.error("SkaUtil_decrypt_3", "加解密类型错误",  encryptType, chr, i, cipher);
                return "";
            }
            if(value == null){
                LogUtil.error("SkaUtil_decrypt_4", "解密值为空",  encryptType, chr, i, cipher);
                return "";
            }
            final String sss = AsciiUtil.asciiCode2sss(value);
            stringBuilder.append(sss);
        }
        String str = stringBuilder.toString();
        // 凯撒算法
        str = CaesarUtil.decrypt(str, effectiveInterators);
        return str;
    }

}