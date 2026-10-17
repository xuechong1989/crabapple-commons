package xc.crabapple.core.file.pdf.watermark;

/*
 *  上传下载文件类型
 **/
public enum WatermarkEnum {
    //原图纸
    WEIRUANYAHEI(1, "字体_微软雅黑"),
    SONGTI(2, "字体_宋体"),
    HEITI(3, "字体_黑体"),
    FANGSONG(4, "字体_仿宋"),
    DANHANG(1, "区域_单行"),
    SHUANGHANG(2, "区域_双行"),
    PUHAN(3, "区域_铺满"),
    SUOYOU(1, "页面范围_所有界面"),
    JISHU(2, "页面范围_单数"),
    OUSHU(3, "页面范围_偶数"),
    USERNAME_DISABLE(0,"用户名称_不启用"),
    USERNAME_PRESENT(0,"用户名称_当前用户名称"),
    MOBILE_DISABLE(0,"手机号码_不启用"),
    MOBILE_PRESENT(0,"手机号码_手机号码"),
    MOBILE_SUB(0,"手机号码_手机号码后四位");

    private Integer code;

    private String desc;

    WatermarkEnum(final Integer code, final String desc) {
        this.code = code;
        this.desc = desc;
    }

    //获取code
    public Integer getCode() {
        return code;
    }

    //设置code
    public void setCode(final Integer code) {
        this.code = code;
    }

    //获取desc
    public String getDesc() {
        return desc;
    }

    //设置desc
    public void setDesc(final String desc) {
        this.desc = desc;
    }

    public static WatermarkEnum build(final int code) {
        for (WatermarkEnum v : WatermarkEnum.values()) {
            if (v.code == code) {
                return v;
            }
        }
        return null;
    }
}
