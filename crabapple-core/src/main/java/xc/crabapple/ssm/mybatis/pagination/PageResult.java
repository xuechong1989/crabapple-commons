package xc.crabapple.ssm.mybatis.pagination;

import java.io.Serializable;

public final class PageResult implements Serializable {

    public static final long serialVersionUID = 1L;

    private static final int DEFAULT_SLIDERS_COUNT = 7;
    private int startIndex = 1;  // 页数
    private int pageSize = 10;  // 分页大小
    private int totalCount = 0;  // 总记录数

    public PageResult(final int startIndex, final int pageSize, final int totalCount){
        super();
        this.startIndex = computePageNo(startIndex);
        this.pageSize = pageSize;
        this.totalCount = totalCount;
    }

    /**
     * 取得当前页。
     */
    public int getStartIndex(){
        return startIndex;
    }

    public int getPageSize(){
        return pageSize;
    }

    /**
     * 取得总项数。即总条目数
     * @return 总项数
     */
    public int getTotalCount(){
        return totalCount;
    }

    /**
     * 是否是首页（第一页），第一页页码为1
     * @return 首页标识
     */
    public boolean isFirstPage(){
        return startIndex <=  1;
    }

    /**
     * 是否是最后一页
     * @return 末页标识
     */
    public boolean isLastPage(){
        return startIndex >=  getTotalPages();
    }

    public int getPrePage(){
        if (isHasPrePage()){
            return startIndex - 1;
        }
        return startIndex;
    }

    public int getNextPage(){
        if (isHasNextPage()){
            return startIndex + 1;
        } else {
            return startIndex;
        }
    }

    /**
     * 判断指定页码是否被禁止，也就是说指定页码超出了范围或等于当前页码。
     * @param startIndex 页码
     * @return boolean  是否为禁止的页码
     */
    public boolean isDisabledPage(final int startIndex){
        return ((startIndex < 1) || (startIndex > getTotalPages()) || (startIndex == this.startIndex));
    }

    /**
     * 是否有上一页
     *
     * @return 上一页标识
     */
    public boolean isHasPrePage(){
        return (startIndex - 1 >=  1);
    }

    /**
     * 是否有下一页
     *
     * @return 下一页标识
     */
    public boolean isHasNextPage(){
        return (startIndex + 1 <=  getTotalPages());
    }

    /**
     * 开始行，可以用于oracle分页使用 (1-based)。
     */
    public int getStartRow(){
        if (getPageSize() <=  0 || totalCount <=  0) return 0;
        return startIndex > 0 ? (startIndex - 1) * getPageSize() + 1 : 0;
    }

    /**
     * 结束行，可以用于oracle分页使用 (1-based)。
     */
    public int getEndRow(){
        return startIndex > 0 ? Math.min(pageSize * startIndex, getTotalCount()) : 0;
    }

    /**
     * offset，计数从0开始，可以用于mysql分页使用(0-based)
     */
    public int getOffset(){
        return startIndex > 0 ? (startIndex - 1) * getPageSize() : 0;
    }

    /**
     * 得到 总页数
     */
    public int getTotalPages(){
        if (totalCount <=  0){
            return 0;
        }
        if (pageSize <=  0){
            return 0;
        }

        int count = totalCount / pageSize;
        if (totalCount % pageSize > 0){
            count++;
        }
        return count;
    }

    public int computePageNo(final int startIndex){
        return computePageNumber(startIndex, pageSize, totalCount);
    }

    /**
     * 页码滑动窗口，并将当前页尽可能地放在滑动窗口的中间部位。
     */
    public Integer[] getSlider(){
        return slider(DEFAULT_SLIDERS_COUNT);
    }

    /**
     * 页码滑动窗口，并将当前页尽可能地放在滑动窗口的中间部位。
     * 注意:不可以使用 getSlider(1)方法名称，因为在JSP中会与 getSlider()方法冲突，报exception
     *
     * @return
     */
    public Integer[] slider(final int slidersCount){
        return generateLinkPageNumbers(getStartIndex(), (int) getTotalPages(), slidersCount);
    }

    private static final int computeLastPageNumber(final int totalItems, final int pageSize){
        if (pageSize <=  0) return 1;
        int result = (int) (totalItems % pageSize == 0 ?
                totalItems / pageSize
                : totalItems / pageSize + 1);
        if (result <=  1)
            result = 1;
        return result;
    }

    private static final int computePageNumber(final int startIndex, final int pageSize, final int totalItems){
        if (startIndex <=  1){
            return 1;
        }
        int result = computeLastPageNumber(totalItems, pageSize);
        if (startIndex > result || startIndex == Integer.MAX_VALUE){
            return result;
        }
        return startIndex;
    }

    private static final Integer[] generateLinkPageNumbers(final int currentPageNumber, final int lastPageNumber, final int count){
        int avg = count / 2;
        int startPageNumber = currentPageNumber - avg;
        if (startPageNumber <=  0){
            startPageNumber = 1;
        }
        int endPageNumber = startPageNumber + count - 1;
        if (endPageNumber > lastPageNumber){
            endPageNumber = lastPageNumber;
        }
        if (endPageNumber - startPageNumber < count){
            startPageNumber = endPageNumber - count;
            if (startPageNumber <=  0){
                startPageNumber = 1;
            }
        }
        java.util.List<Integer> result = new java.util.ArrayList<>();
        for (int i = startPageNumber; i <=  endPageNumber; i++){
            result.add(Integer.valueOf(i));
        }
        return result.toArray(new Integer[result.size()]);
    }

    @Override
    public String toString(){
        final StringBuilder sb = new StringBuilder();
        sb.append("Paginator");
        sb.append("{startIndex = ").append(startIndex);
        sb.append(", pageSize = ").append(pageSize);
        sb.append(", totalCount = ").append(totalCount);
        sb.append('}');
        return sb.toString();
    }

}
