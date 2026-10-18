package xc.crabapple.core.cipher.aes;

import xc.crabapple.core.data.bean.BeanBase;
import xc.crabapple.core.log.LogUtil;

import java.io.File;

/**
 * 2020-11-09 6.9.0
 */
public final class AesBean extends BeanBase {

    private String key;  //秘钥
    private Object value;  //原文，明文或密文
    private Integer level = 256;  //加密等级，即密钥长度，取值为 128/192/256，默认值256
    //pkcs填充模式，算法/模式/补码，如AES/ECB/PKCS7Padding
    private String pattern = "CBC";  //模式，加密模式，可选值 ECB/CBC，默认值CBC
    private Integer padding = 7;  //补码，即偏移量，可选值 5/7/10，默认值7
    private String iv = "XCLasfoLGH34893047^*%$$las401354kjslg20230803";  //向量，增加加密算法的强度，该值就是一个普通字符串
    private String dataType = "string";  //bytes string file
    //file相关
    private String fromUrl;
    private File fromFile;
    private String toUrl;
    private File toFile;

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(final Object value) {
        if(value instanceof byte[]){
            dataType = "bytes";
        }else if(value instanceof String){
            dataType = "string";
        }else{
            LogUtil.error("AesUtil_setValue_1", "要存入的数据类型错误");
        }
        this.value = value;
    }

    public String getDataType() {
        return dataType;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(final Integer level) {
        this.level = level;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(final String pattern) {
        this.pattern = pattern;
    }

    public Integer getPadding() {
        return padding;
    }

    public void setPadding(final Integer padding) {
        this.padding = padding;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(final String iv) {
        this.iv = iv;
    }

    public String getFromUrl() {
        return fromUrl;
    }

    public void setFromUrl(final String fromUrl) {
        dataType = "file";
        this.fromUrl = fromUrl;
        fromFile = new File(fromUrl);
    }

    public File getFromFile() {
        return fromFile;
    }

    public void setFromFile(final File fromFile) {
        dataType = "file";
        this.fromFile = fromFile;
        fromUrl = fromFile.getAbsolutePath();
    }

    public String getToUrl() {
        return toUrl;
    }

    public void setToUrl(final String toUrl) {
        dataType = "file";
        this.toUrl = toUrl;
        toFile = new File(toUrl);
    }

    public File getToFile() {
        return toFile;
    }

    public void setToFile(final File toFile) {
        dataType = "file";
        this.toFile = toFile;
        toUrl = toFile.getAbsolutePath();
    }

}