package xc.crabapple.core.cipher.submerge;

import xc.crabapple.core.cipher.sha.ShaUtil;
import xc.crabapple.core.cipher.ska.SkaUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.AsciiUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.regex.RegexUtil;

/**
 * 2023-08-06
 * 2025-01-21 添加SHA-512前缀校验
 *
 * 字符串淹没算法（原创算法），该算法为对称加密算法
 * 适用于对简单字符串的膨胀性加密
 * 特性：相同明文+相同秘钥 时每次加密的结果均不同、但解密结果与原明文均相同，增加破解难度。
 *
 * # 获取密文的长度
 *   cihper.length = key.length + plaintext.length + [位数(plaintext.length) + 位数("XC".length) + 13(毫秒时间戳)]
 *   如：密钥key长度为10，明文plaintext是32位字符串，则密文长度cipher.length = 10 + 32 + [2 + 2 + 13] = 59。即密文长度为59。
 *
 * # 获取明文的长度
 *   若想获取指定长度的密文数据，则对上述进行逆运算即可：
 *   cihper.length - 位数("XC".length) - 13(毫秒时间戳) - key.length = plaintext.length + [位数(plaintext.length)]
 *   如：想获取密文长度为128的数据，且密钥长度为32，则：128 - 2 - 13 - 32 = 81 = plaintext.length + [位数(plaintext.length)]。即明文长度为79。
 *
 * 上述长度最后再加上256（SHA-512算法的两个密钥前缀128+128）。
 */
public final class SubmergeUtil {

    /**
     * 2025-01-16
     * 生成密钥，256位，仅包含：数字+小写字母+大写字母
     */
    public static final String generateKey() {
        final String key = RandomUtil.getStr(256, false);
        return key;
    }

    /**
     * 2023-08-08
     * 生成密钥，指定位数，仅包含：数字+小写字母+大写字母
     * @param keyLength 密钥长度最少128位
     * @return
     */
    public static final String generateKey(final int keyLength){
        if(keyLength < 128){
            LogUtil.error("", "密钥长度最少128位", keyLength);
            return "";
        }
        final String key = RandomUtil.getStr(keyLength, false);
        return key;
    }

    /**
     * 2023-08-06
     * 2025-01-21 添加SHA-512前缀校验
     * 字符串淹没算法（原创算法）-编码
     * 对原字符串倒序、然后插入随机生成的字符串（包含随机数115位（数字+大小写字母）+毫秒时间戳13位）。
     * 插入规则：从随机数左起的第一个数字型字符开始插入，后续每隔两个字符后顺序插入。
     * @param key 秘钥，秘钥越长安全性越高，推荐值为128及以上
     * @param plaintext 原字符串明文
     * @return
     */
    public static final String encrypt(final String key, final String plaintext){
        if(plaintext == null){
            LogUtil.error("SubmergeUtil_encrypt_1", "明文不能为空");
            return "";
        }
        boolean isAscii = AsciiUtil.isPrintableAll(key);  // 是否为可打印类型的ascii码
        if(!isAscii){
            LogUtil.error("SubmergeUtil_encrypt_2", "秘钥必须为可打印ascii码对应字符串");
            return "";
        }
        final int keyLen = key.length();
        //
        String randomStr = RandomUtil.getStr(keyLen, false);
        final long stamp = System.currentTimeMillis();
        String random = randomStr + stamp;
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < random.length(); i++){
            final char chr = random.charAt(i);
            final String sss = StringUtil.char2str(chr);
            stringBuilder.append(sss);
            if(RegexUtil.isNumber(sss)){
                break;
            }
        }
        int plaintextIndex = plaintext.length() - 1;
        int count = -1;
        int index = stringBuilder.length();
        for(int i = index; i < random.length(); i++){
            count++;
            final char chr = random.charAt(i);
            final String sss = StringUtil.char2str(chr);
            if(count % 2 == 0){
                if(plaintextIndex >= 0){
                    char plaintChr = plaintext.charAt(plaintextIndex);
                    String plaintStr = StringUtil.char2str(plaintChr);
                    stringBuilder.append(plaintStr).append(sss);
                    plaintextIndex--;
                    continue;
                }
            }
            stringBuilder.append(sss);
        }
        // 补码
        for(int i = plaintextIndex; i >= 0; i--){
            char plaintChr = plaintext.charAt(i);
            String plaintStr = StringUtil.char2str(plaintChr);
            stringBuilder.append(plaintStr);
        }
        String cipher = stringBuilder.toString();
        final int cipherLen = cipher.length();
        // key混淆
        String asciiStr = AsciiUtil.str2asciiCodeStr(key);
        asciiStr = StringUtil.deleteAll(asciiStr, "0");
        final String[] asciiArray = StringUtil.split(asciiStr, ",");
        StringBuilder stringBuilder1 = null;
        for(int i = 0; i < asciiArray.length; i++){
            String ascii = asciiArray[i];
            final int asciiNum = Integer.parseInt(ascii);
            for(int j = 0; j < asciiNum; j++){
                stringBuilder1 = new StringBuilder();
                String left = StringUtil.substr(cipher, 0, 3);
                String center = StringUtil.substr(cipher, 3, cipherLen - 1);
                String right = StringUtil.substr(cipher, cipherLen - 1, cipherLen);
                stringBuilder1.append(right).append(center).append(left);
                cipher = stringBuilder1.toString();
            }
        }
        cipher += "XC" + plaintext.length();
        cipher = SkaUtil.encrypt(SkaUtil.ENCRYPT_PUBLIC, cipher, 10);
        final String shaKey = ShaUtil.encrypt512(key);
        final String shaCipher = ShaUtil.encrypt512(cipher);
        cipher = shaKey + shaCipher + cipher;
        return cipher;
    }

    /**
     * 2023-08-06
     * 2025-01-21 添加SHA-512前缀校验
     * 字符串淹没算法（原创算法）-解码
     * @param cipher
     * @param key
     */
    public static final String decrypt(final String key, final String cipher){
        if(key == null){
            LogUtil.error("SubmergeUtil_decrypt_1", "秘钥不能为空");
            return "";
        }
        if(cipher == null){
            LogUtil.error("SubmergeUtil_decrypt_2", "密文不能为空");
            return "";
        }
        boolean isAscii = AsciiUtil.isPrintableAll(key);  // 是否为可打印类型的ascii码
        if(!isAscii){
            LogUtil.error("SubmergeUtil_decrypt_4", "秘钥必须为可打印ascii码对应字符串");
            return "";
        }
        //
        int cipherLength = cipher.length();
        if(cipherLength <= 256){
            LogUtil.error("SubmergeUtil_decrypt_5", "密文格式错误", cipherLength);
            return "";
        }
        //
        String prefix = cipher.substring(0, 256);
        if(RegexUtil.inUpper(prefix)){
            LogUtil.error("SubmergeUtil_decrypt_6", "密文格式错误");
            return "";
        }
        String realKey = key;
        final String shaKey = cipher.substring(0, 128);
        final String curShaKey = ShaUtil.encrypt512(realKey);
        if(!StringUtil.equals(shaKey, curShaKey)){
            LogUtil.error("SubmergeUtil_decrypt_7", "密钥错误");
            return "";
        }
        String realCipher = cipher.substring(256, cipher.length());
        final String shaCipher = cipher.substring(128, 256);
        final String curShaCipher = ShaUtil.encrypt512(realCipher);
        if(!StringUtil.equals(shaCipher, curShaCipher)){
            LogUtil.error("SubmergeUtil_decrypt_8", "密文错误");
            return "";
        }
        //
        realCipher = SkaUtil.decrypt(SkaUtil.ENCRYPT_PUBLIC, realCipher, 10);
        final String[] array = StringUtil.split(realCipher, "XC");
        final String lastStr = array[array.length - 1];
        final int plaintextLen = Integer.parseInt(lastStr);
        realCipher = StringUtil.deleteLast(realCipher, "XC"+lastStr);
        int cipherLen = realCipher.length();
        // key混淆
        String asciiStr = AsciiUtil.str2asciiCodeStr(key);
        asciiStr = StringUtil.deleteAll(asciiStr, "0");
        final String[] asciiArray = StringUtil.split(asciiStr, ",");
        StringBuilder stringBuilder1 = null;
        for(int i = asciiArray.length - 1; i >= 0; i--){
            String kkkStr = asciiArray[i];
            final int asciiNum = Integer.parseInt(kkkStr);
            for(int j = 0; j < asciiNum; j++){
                stringBuilder1 = new StringBuilder();
                String left = StringUtil.substr(realCipher, 0, 1);
                String center = StringUtil.substr(realCipher, 1, cipherLen - 3);
                String right = StringUtil.substr(realCipher, cipherLen - 3, cipherLen);
                stringBuilder1.append(right).append(center).append(left);
                realCipher = stringBuilder1.toString();
            }
        }
        //
        int index = 0;
        for(int i = 0; i < cipherLen; i++){
            index++;
            final char chr = realCipher.charAt(i);
            final String sss = StringUtil.char2str(chr);
            if(RegexUtil.isNumber(sss)){
                break;
            }
        }
        final int randomLen = cipherLen - plaintextLen;
        int randomIndex = index;
        int zhengIndex = 0;
        int count = -1;
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = index; i < cipherLen; i++){
            count++;
            if(count % 3 == 0){
                zhengIndex = i;
                final char chr = realCipher.charAt(i);
                final String sss = StringUtil.char2str(chr);
                stringBuilder.append(sss);
                if(stringBuilder.length() >= plaintextLen){
                    break;
                }
            }else{
                // 消耗的随机数
                randomIndex++;
                if(randomIndex >= randomLen){
                    break;
                }
            }
        }
        String plaintext = stringBuilder.toString();
        if(plaintext.length() >= plaintextLen){
            stringBuilder = stringBuilder.reverse();
            plaintext = stringBuilder.toString();
            return plaintext;
        }
        //
        String last = StringUtil.substr(realCipher, zhengIndex + 1, cipherLen);
        stringBuilder = stringBuilder.reverse();
        StringBuilder stringBuilder2 = new StringBuilder();
        for(int i = last.length() - 1; i >= 0; i--){
            final char chr = last.charAt(i);
            final String sss = StringUtil.char2str(chr);
            stringBuilder2.append(sss);
            int accumulate = stringBuilder.length() + stringBuilder2.length();
            if(accumulate >= plaintextLen){
                plaintext = stringBuilder2.toString() + stringBuilder.toString();
                return plaintext;
            }
        }
        return "";
    }

}