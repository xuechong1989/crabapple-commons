package xc.crabapple.ssm.mybatis.pagination;

import java.io.Serializable;

/**
 * 分页信息，该类只是给客户端jsp页面解析用的
 */
public final class Page implements Serializable {

    public static final long serialVersionUID = 1L;

    // 页码大小
    private Integer pageSize = 20;
    // 总记录数
    private Integer totalItems = 0;
    // 开始页码
    private Integer pageIndex = 0;
    // 忽略计算总记录数
    private boolean skipCountTotal = false;

    public Page(){

    }

    public Page(final boolean skipCountTotal){
        this.skipCountTotal = skipCountTotal;
    }

    /**
     * Page构造函数
     * @param pageIndex 页码索引 从零开始
     * @param pageSize 每页记录数
     */
    public Page(final Integer pageIndex, final Integer pageSize){
        this.pageIndex = pageIndex.intValue();
        this.pageSize = pageSize;
    }

    /**
     * Page构造函数
     * @param pageIndex 页码索引 从零开始
     * @param pageSize 每页记录数
     * @param skipCountTotal 是否忽略统计总记录数
     */
    public Page(final Integer pageIndex, final Integer pageSize, final boolean skipCountTotal){

    }

    public Integer getPageSize(){
        return pageSize;
    }

    public Integer getTotalItems(){
        return totalItems;
    }

    public Integer getStartIndex(){
        return pageIndex*pageSize;
    }

    public void setTotalItems(final Integer totalItems){
        this.totalItems = totalItems;
    }

    public void setPageSize(final Integer pageSize){
        this.pageSize = pageSize;
    }

    public Integer getPageIndex(){
        return pageIndex;
    }

    public void setPageIndex(final Integer pageIndex){
        this.pageIndex = pageIndex;
    }

    public Integer getTotalPages(){
        int page = this.totalItems/this.pageSize;
        return this.totalItems%this.pageSize == 0?page:page+1;
    }

    public boolean isSkipCountTotal(){
        return skipCountTotal;
    }

    public void setSkipCountTotal(final boolean skipCountTotal){
        this.skipCountTotal = skipCountTotal;
    }

}
