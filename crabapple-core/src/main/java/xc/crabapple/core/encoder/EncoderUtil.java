package xc.crabapple.core.encoder;

import xc.crabapple.core.collection.StringUtil;

/**
 * 2016-12-27 1.0.0
 * 2023-08-22 添加对应的解码方法
 */
public final class EncoderUtil{

    /**
     * 2020-03-31 5.0.0
     * 字符串转ascii码，之间用英文逗号分隔
     * @param str
     */
    public static final String str2asciis(final String str){
        final String asciis = AsciiUtil.str2asciiCodeStr(str);
        return asciis;
    }

    /**
     * 2020-03-31 5.0.0
     * 单个字符串转ascii码
     * @param str
     */
    public static final int str2ascii(final String str){
        int asciiCode = AsciiUtil.sss2asciiCode(str);
        return asciiCode;
    }

    /**
     * 2020-03-31 5.0.0
     * 单个ascii码转字符串
     * @param asciiCode
     */
    public static final String ascii2str(final int asciiCode){
        String str = AsciiUtil.asciiCode2sss(asciiCode);
        return str;
    }

    /**
     * 2016-12-27 1.0.0
     * @param str
     * @param from
     * @param to
     */
    public static final String transformEncoder(final String str, final String from, final String to){
        if(StringUtil.isBlank(str)){
            return str;
        }
        try{
            byte[] bytes = str.getBytes(from);
            String result = new String(bytes, to);
            return result;
        }catch(Throwable throwable){
            //LogUtil.error("EncoderUtil_transformEncoder_1", throwable, str, from, to);
            return str;
        }
    }

    /**
     * 2020-07-31 6.4.0
     * @param str
     * @param to
     */
    public static final String transformEncoder(final String str, final String to){
        if(StringUtil.isBlank(str)){
            return str;
        }
        try{
            byte[] bytes = str.getBytes();
            String result = new String(bytes, to);
            return result;
        }catch(Throwable throwable){
            //LogUtil.error("EncoderUtil_transformEncoder_1", throwable, str, to);
            return str;
        }
    }

    /**
     * 2020-07-31 6.4.0
     * 指定字符串转换为UTF-8编码
     * @param str
     */
    public static final String transformUTF8(final String str){
        if(StringUtil.isBlank(str)){
            return str;
        }
        try{
            byte[] bytes = str.getBytes();
            String result = new String(bytes, "UTF8");
            return result;
        }catch(Throwable throwable){
            //LogUtil.error("EncoderUtil_transformEncoder_1", throwable, str);
            return str;
        }
    }

}