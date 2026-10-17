package xc.crabapple.core.cipher.emoji;

import xc.crabapple.core.collection.StringUtil;

/**
 * 2017-08-23 1.0.0
 * emoji字符集处理
 */
public final class EmojiUtil{

    /**
     * 2017-08-23 1.0.0
     * @param str
     * @param strNew
     */
    public static final String emoji2other(final String str, final String strNew){
        if(StringUtil.isBlank(str)){
            return str;
        }
        String result = str.replaceAll("[\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]", strNew);
        return result;
    }

}
