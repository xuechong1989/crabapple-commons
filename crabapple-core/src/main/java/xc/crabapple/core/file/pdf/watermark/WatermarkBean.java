package xc.crabapple.core.file.pdf.watermark;

import xc.crabapple.core.data.bean.BeanBase;

/**
 * 2019-07-31
 */
public class WatermarkBean extends BeanBase {

    //
    public static final int UNDER = 1;  //水印在最下面
    public static final int OVER = 2;  //水印在最上面
    //
    public static final int LOGO_LOCATION_CENTER = 1;  //水印位置，居中
    public static final int LOGO_LOCATION_DOWNRIGHT = 2;  //水印位置，下右
    public static final int LOGO_LOCATION_DOWN = 3;  //水印位置，正下

    private String content = "";  //水印类型：img/text，.png(jpg,.gi)
    private String urlFrom;  //源文件地址
    private String urlTo;  //文件输出地址
    private Float alpha = 0.3F;  //透明度，取值范围是[0.0-1.0], 0.0为完全透明, 1.0为完全不透明
    private Integer layer = UNDER;
    private Integer position = LOGO_LOCATION_CENTER;  //水印位置
    private String projectName;  //系统名称 0 不启用 , (输入内容)
    private Integer userName;  //用户名名称 0 不启用 , 1 当前用户名称
    private Integer mobile;  //手机号码 0 不启用 , 1 手机号码 , 2 手机号码后四位
    private Integer font = WatermarkEnum.WEIRUANYAHEI.getCode();  //字体 1 微软雅黑 , 2 宋体 , 3 黑体 , 4 仿宋 默认微软雅黑
    private Integer fontSize = 12;  //字号
    private Integer rotation = 45;  //旋转角度取值范围pdf2text[-360,360] 默认45
    private Integer region = WatermarkEnum.PUHAN.getCode();  //区域 1 单行 , 2 双行 , 3 平铺(铺满) 默认单行
    private Integer wordSpace = 5;  //字间距(系统名称+用户名称+手机号码组成水印内容,字间距为三者中间的空格) 默认5
    private Integer lineSpace = 5;  //行间距 默认5
    private Integer pageRange = WatermarkEnum.SUOYOU.getCode();  //页面范围 1 所有页面 , 2 奇数页面 , 3 偶数页面 默认所有页面
    private boolean debug = false;  //是否启用

    private String userName1;  //用户名名称
    private String mobile1;  //手机号码

    public WatermarkBean() {
    }

    public WatermarkBean(final int UNDER, final int OVER, final int LOGO_LOCATION_CENTER, final int LOGO_LOCATION_DOWNRIGHT, final int LOGO_LOCATION_DOWN, final String content, final String urlFrom, final String urlTo, final Float alpha, final Integer layer, final Integer position, final String projectName, final Integer userName, final Integer mobile, final Integer font, final Integer fontSize, final Integer rotation, final Integer region, final Integer wordSpace, final Integer lineSpace, final Integer pageRange, final boolean debug, final String userName1, final String mobile1) {
        this.content = content;
        this.urlFrom = urlFrom;
        this.urlTo = urlTo;
        this.alpha = alpha;
        this.layer = layer;
        this.position = position;
        this.projectName = projectName;
        this.userName = userName;
        this.mobile = mobile;
        this.font = font;
        this.fontSize = fontSize;
        this.rotation = rotation;
        this.region = region;
        this.wordSpace = wordSpace;
        this.lineSpace = lineSpace;
        this.pageRange = pageRange;
        this.debug = debug;
        this.userName1 = userName1;
        this.mobile1 = mobile1;
    }

    /**
     * 获取
     *
     * @return content
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置
     *
     * @param content
     */
    public void setContent(final String content) {
        this.content = content;
    }

    /**
     * 获取
     *
     * @return urlFrom
     */
    public String getUrlFrom() {
        return urlFrom;
    }

    /**
     * 设置
     *
     * @param urlFrom
     */
    public void setUrlFrom(final String urlFrom) {
        this.urlFrom = urlFrom;
    }

    /**
     * 获取
     *
     * @return urlTo
     */
    public String getUrlTo() {
        return urlTo;
    }

    /**
     * 设置
     *
     * @param urlTo
     */
    public void setUrlTo(final String urlTo) {
        this.urlTo = urlTo;
    }

    /**
     * 获取
     *
     * @return alpha
     */
    public Float getAlpha() {
        return alpha;
    }

    /**
     * 设置
     *
     * @param alpha
     */
    public void setAlpha(final Float alpha) {
        this.alpha = alpha;
    }

    /**
     * 获取
     *
     * @return layer
     */
    public Integer getLayer() {
        return layer;
    }

    /**
     * 设置
     *
     * @param layer
     */
    public void setLayer(final Integer layer) {
        this.layer = layer;
    }

    /**
     * 获取
     *
     * @return position
     */
    public Integer getPosition() {
        return position;
    }

    /**
     * 设置
     *
     * @param position
     */
    public void setPosition(final Integer position) {
        this.position = position;
    }

    /**
     * 获取
     *
     * @return projectName
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * 设置
     *
     * @param projectName
     */
    public void setProjectName(final String projectName) {
        this.projectName = projectName;
    }

    /**
     * 获取
     *
     * @return userName
     */
    public Integer getUserName() {
        return userName;
    }

    /**
     * 设置
     *
     * @param userName
     */
    public void setUserName(final Integer userName) {
        this.userName = userName;
    }

    /**
     * 获取
     *
     * @return mobile
     */
    public Integer getMobile() {
        return mobile;
    }

    /**
     * 设置
     *
     * @param mobile
     */
    public void setMobile(final Integer mobile) {
        this.mobile = mobile;
    }

    /**
     * 获取
     *
     * @return font
     */
    public Integer getFont() {
        return font;
    }

    /**
     * 设置
     *
     * @param font
     */
    public void setFont(final Integer font) {
        this.font = font;
    }

    /**
     * 获取
     *
     * @return fontSize
     */
    public Integer getFontSize() {
        return fontSize;
    }

    /**
     * 设置
     *
     * @param fontSize
     */
    public void setFontSize(final Integer fontSize) {
        this.fontSize = fontSize;
    }

    /**
     * 获取
     *
     * @return rotation
     */
    public Integer getRotation() {
        return rotation;
    }

    /**
     * 设置
     *
     * @param rotation
     */
    public void setRotation(final Integer rotation) {
        this.rotation = rotation;
    }

    /**
     * 获取
     *
     * @return region
     */
    public Integer getRegion() {
        return region;
    }

    /**
     * 设置
     *
     * @param region
     */
    public void setRegion(final Integer region) {
        this.region = region;
    }

    /**
     * 获取
     *
     * @return wordSpace
     */
    public Integer getWordSpace() {
        return wordSpace;
    }

    /**
     * 设置
     *
     * @param wordSpace
     */
    public void setWordSpace(final Integer wordSpace) {
        this.wordSpace = wordSpace;
    }

    /**
     * 获取
     *
     * @return lineSpace
     */
    public Integer getLineSpace() {
        return lineSpace;
    }

    /**
     * 设置
     *
     * @param lineSpace
     */
    public void setLineSpace(final Integer lineSpace) {
        this.lineSpace = lineSpace;
    }

    /**
     * 获取
     *
     * @return pageRange
     */
    public Integer getPageRange() {
        return pageRange;
    }

    /**
     * 设置
     *
     * @param pageRange
     */
    public void setPageRange(final Integer pageRange) {
        this.pageRange = pageRange;
    }

    /**
     * 获取
     *
     * @return debug
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * 设置
     *
     * @param debug
     */
    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    /**
     * 获取
     *
     * @return userName1
     */
    public String getUserName1() {
        return userName1;
    }

    /**
     * 设置
     *
     * @param userName1
     */
    public void setUserName1(final String userName1) {
        this.userName1 = userName1;
    }

    /**
     * 获取
     *
     * @return debug
     */
    public String getMobile1() {
        return mobile1;
    }

    /**
     * 设置
     *
     * @param
     */
    public void setMobile1(final String mobile1) {
        this.mobile1 = mobile1;
    }
    public String toString() {
        return "WatermarkBean{UNDER = " + UNDER + ", OVER = " + OVER + ", LOGO_LOCATION_CENTER = " + LOGO_LOCATION_CENTER + ", LOGO_LOCATION_DOWNRIGHT = " + LOGO_LOCATION_DOWNRIGHT + ", LOGO_LOCATION_DOWN = " + LOGO_LOCATION_DOWN + ", content = " + content + ", urlFrom = " + urlFrom + ", urlTo = " + urlTo + ", alpha = " + alpha + ", layer = " + layer + ", position = " + position + ", projectName = " + projectName + ", userName = " + userName + ", mobile = " + mobile + ", font = " + font + ", fontSize = " + fontSize + ", rotation = " + rotation + ", region = " + region + ", wordSpace = " + wordSpace + ", lineSpace = " + lineSpace + ", pageRange = " + pageRange + ", debug = " + debug + "}";
    }

}
