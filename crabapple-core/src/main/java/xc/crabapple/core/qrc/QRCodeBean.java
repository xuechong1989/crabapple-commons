package xc.crabapple.core.qrc;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import xc.crabapple.core.data.bean.BeanBase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 2020-10-22 6.8.0
 * 生成二维码参数配置
 */
public final class QRCodeBean extends BeanBase {

    private String returnType = "local";  //local / net, local-直接在本地生成图片，net-客户端通过接口调用形式返回图片流
    //
    private String content = "xc_crabapple";  //存放在二维码中的内容，如helloworld，或者一个完整网址
    private String logoUrl = "";  //嵌入二维码的本地图片路径，如d:/app/tmp/logo.jpg
    private String qrCodeUrl = "";  //生成的二维码的路径，如d:/app/tmp/a.jpg
    private String needCompress = "yes";  //是否压缩，yes no, 默认为yes
    private Integer margin = 2;  //边框距，取值为1到10
    private Integer width = 300;  //二维码宽度
    private Integer height = 300;  //二维码高度
    private Integer rgbCode = 0xFF000000;  //二维码的颜色，默认黑色
    private Integer rgbBackground = 0xFFFFFFFF;  //背景颜色，默认白色
    private Integer logoWidth = 60;  //logo图像的宽度
    private Integer logoHeight = 60;  //logo图像的高度
    private Float logoArcw = 6F;  //横向弯曲度
    private Float logoArch = 6F;  //纵向弯曲度
    private ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.H;  //矫错级别，L-7%码字, M-15%码字, Q-25%码字, H-30%码字，可被修正容量，百分比越大生成的二维码越复杂，可允许被遮挡的部分就可以越多
    private HttpServletRequest request;
    private HttpServletResponse response;

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(final String returnType) {
        this.returnType = returnType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(final String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(final String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getNeedCompress() {
        return needCompress;
    }

    public void setNeedCompress(final String needCompress) {
        this.needCompress = needCompress;
    }

    public Integer getMargin() {
        return margin;
    }

    public void setMargin(final Integer margin) {
        this.margin = margin;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(final Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(final Integer height) {
        this.height = height;
    }

    public Integer getRgbCode() {
        return rgbCode;
    }

    public void setRgbCode(final Integer rgbCode) {
        this.rgbCode = rgbCode;
    }

    public Integer getRgbBackground() {
        return rgbBackground;
    }

    public void setRgbBackground(final Integer rgbBackground) {
        this.rgbBackground = rgbBackground;
    }

    public Integer getLogoWidth() {
        return logoWidth;
    }

    public void setLogoWidth(final Integer logoWidth) {
        this.logoWidth = logoWidth;
    }

    public Integer getLogoHeight() {
        return logoHeight;
    }

    public void setLogoHeight(final Integer logoHeight) {
        this.logoHeight = logoHeight;
    }

    public Float getLogoArcw() {
        return logoArcw;
    }

    public void setLogoArcw(final Float logoArcw) {
        this.logoArcw = logoArcw;
    }

    public Float getLogoArch() {
        return logoArch;
    }

    public void setLogoArch(final Float logoArch) {
        this.logoArch = logoArch;
    }

    public ErrorCorrectionLevel getErrorCorrectionLevel() {
        return errorCorrectionLevel;
    }

    public void setErrorCorrectionLevel(final ErrorCorrectionLevel errorCorrectionLevel) {
        this.errorCorrectionLevel = errorCorrectionLevel;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(final HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public void setResponse(final HttpServletResponse response) {
        this.response = response;
    }

}
