package xc.crabapple.ssm.mybatis.pagination;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class PageList<E> extends ArrayList<E> implements Serializable {

    public static final long serialVersionUID = 1L;

    private PageResult pageResult;
    //
	private Integer total = 1;
	private Integer pageSize = 1;

	public PageList(){

	}

	public PageList(final List<E> list){
		super(list);
	}

	public PageList(final PageResult pageResult){
		setPageResult(pageResult);
	}

	public PageList(final List<E> list, final PageResult pageResult){
		super(list);
		setPageResult(pageResult);
    }

	/**
	 * @param pageResult the pageResult to set
	 */
	public void setPageResult(final PageResult pageResult){
		PageResult localPageResult = pageResult;
		if(localPageResult == null){
			localPageResult = getPageResult();
		}
		this.pageResult = localPageResult;
		total = localPageResult.getTotalCount();
		pageSize = localPageResult.getStartIndex();
	}

	/**
	 * 得到分页器，通过Paginator可以得到总页数等值
	 */
	public PageResult getPageResult(){
		if(pageResult == null){
			int size = this.size();
			pageResult = new PageResult(1, size, size);
			return pageResult;
		}
		return pageResult;
	}

	public Integer getTotal(){
		if(pageResult == null){
			Integer total = super.size();
			return total;
		}
		Integer total = pageResult.getTotalCount();
		return total;
	}

	public Integer getPageIndex(){
		if(pageResult == null){
			return 0;
		}
		Integer pageIndex = pageResult.getStartIndex();
		return pageIndex;
	}

}
