package xc.crabapple.core.ocr.baidu2;

import xc.crabapple.core.data.bean.BeanBase;

/**
 * 2021-04-07 7.5.0
 */
public final class OcrBaidu2Bean extends BeanBase {

    private Integer index = 1;
    private Integer x = 0;
    private Integer y = 0;
    private Integer width = 0;
    private Integer height = 0;
    private String content = "";
    private String formatContent = "";  //缩进处理后内容，即格式化之后的文本
    private Float size = 10.5F;  //字号大小，默认五号，即10.5磅
    private Double emptyCount = 0D;  //左侧空格数量

    public Integer getIndex() {
        return index;
    }

    public void setIndex(final Integer index) {
        this.index = index;
    }

    public Integer getX() {
        return x;
    }

    public void setX(final Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(final Integer y) {
        this.y = y;
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

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public String getFormatContent() {
        return formatContent;
    }

    public void setFormatContent(final String formatContent) {
        this.formatContent = formatContent;
    }

    public Float getSize() {
        return size;
    }

    public void setSize(final Float size) {
        this.size = size;
    }

    public Double getEmptyCount() {
        return emptyCount;
    }

    public void setEmptyCount(final Double emptyCount) {
        this.emptyCount = emptyCount;
    }
}
