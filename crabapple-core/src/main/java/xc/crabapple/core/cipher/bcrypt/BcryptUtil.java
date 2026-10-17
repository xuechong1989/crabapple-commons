package xc.crabapple.core.cipher.bcrypt;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;

/**
 * 2023-08-04
 * bcrypt算法工具类
 * bcrypt算法是单向加密、无法解密。
 * http://www.mindrot.org/projects/jBCrypt/
 */
public final class BcryptUtil {

    /**
     * 2023-08-04
     * @param plaintext
     * @return
     */
    public static final String getHash(final String plaintext){
        final String cipher = getHash(plaintext, 10);
        return cipher;
    }

    /**
     * 2023-08-04
     * @param plaintext
     * @param log_rounds 取值范围是 大于等于4，小于等于30
     * @return
     */
    public static final String getHash(final String plaintext, final int log_rounds){
        if(StringUtil.isBlank(plaintext)){
            LogUtil.error("BcryptUtil_getHash_1", "plaintext不能为空");
            return "";
        }
        final int length = plaintext.length();
        if(length > 128){
            LogUtil.error("BcryptUtil_getHash_2", "plaintext长度范围为[1-128]，即ASCII码1-128位", plaintext, length);
            return "";
        }
        if(log_rounds < 4 || log_rounds > 30){
            LogUtil.error("BcryptUtil_getHash_3", "log_rounds取值范围为[4-30]", log_rounds);
            return "";
        }
        final String salt = BCrypt.gensalt(log_rounds);
        final String cipher = BCrypt.hashpw(plaintext, salt);
        return cipher;
    }

    /**
     * 2023-08-04
     * @param plaintext
     * @param hash
     * @return
     */
    public static final boolean check(final String plaintext, final String hash){
        if(StringUtil.isBlank(plaintext)){
            LogUtil.error("BcryptUtil_check_1", "plaintext不能为空");
            return false;
        }
        final int length = plaintext.length();
        if(length > 128){
            LogUtil.error("BcryptUtil_check_2", "plaintext长度范围为[1-128]，即ASCII码1-128位", plaintext, length);
            return false;
        }
        final boolean isEQ = BCrypt.checkpw(plaintext, hash);
        return isEQ;
    }

}