package xc.crabapple.core.encoder;

import java.io.File;

/**
 * 2017-04-28 1.0.0
 * 自动获取文件的编码
 */
public final class FileEncoderUtil{

    /**
     * 2018-03-18 1.1.0
     * 得到文件的编码
     * @param url 文件路径，指向文件
     * @return 文件的编码
     */
    public static final String getEncoderType(final String url){
        final File file = new File(url);
        final String encoderType = getEncoderType(file);
        return encoderType;
    }

    /**
     * 2017-06-13 1.0.0
     * 2018-03-18 1.1.0
     * 2018-04-24 解决指定文件内容为空时报错的问题
     * 2020-12-16 解决超大文件（长度大于Integer.MAX_VALUE）解析报错，解决方案是添加判断，若是超大文件则直接返回UTF-8编码
     * 2024-05-24 校验是否小于零
     * 得到文件的编码
     * @param file 文件
     * @return 文件的编码
     */
    public static final String getEncoderType(final File file){
        final FileEncoder fileEncoder = new FileEncoder();
        final int value = fileEncoder.detectEncoding(file);
        if(value < 0){
            return "UTF-8";
        }
        final String encoderType = fileEncoder.getEncoderType(value);
        return encoderType;
    }

}