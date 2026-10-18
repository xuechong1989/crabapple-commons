package xc.crabapple.ssm.mybatis;

/**
 * 查询参数
 */
public final class SortParam {

	public static final  String SORT_ASC = "ASC";
	public static final  String SORT_DESC = "DESC";
	// 字段属性名称
	private String property;
	// 字段方向排序
	private String direction;

	public SortParam(){

	}

	public SortParam(final String property, final String direction){
		this.property = property;
		this.direction = direction;
	}

	public String getProperty(){
		return property;
	}

	public void setProperty(final String property){
		this.property = property;
	}

	public String getSql(){
		String orderSql = this.property + " " + this.direction;
		return orderSql;
	}

	@Override
	public String toString(){
		return this.property + " " + this.direction;
	}

	public String getDirection(){
		return direction;
	}

	public void setDirection(final String direction){
		this.direction = direction;
	}

}
