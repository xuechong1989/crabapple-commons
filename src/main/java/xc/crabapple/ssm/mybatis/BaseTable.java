package xc.crabapple.ssm.mybatis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.json.jackson.JacksonUtil;

/**
 * 默认表对象。
 */
public class BaseTable implements Serializable {

	public static final long serialVersionUID = 1L;

	// 表名
	private String name = "";
	// 表注释
	private String comment = "";
	private String entName = "";
	//表类型
	private String type = "";
	// 列列表
	private List<BaseColumn> columnList = new ArrayList<>();
	private boolean isMain = false;
	public static final String TYPE_TABLE = "table";

	public String getTableName(){
		return name;
	}

	public String getComment(){
		if (!StringUtil.isBlank(comment))
			comment = comment.replace("'", "''");
		return comment;
	}

	public void setComment(final String comment){
		this.comment = comment;
	}

	/**
	 * 添加列对象。
	 */
	public void addColumn(final BaseColumn model){
		this.columnList.add(model);
	}

	public List<BaseColumn> getColumnList(){
		return columnList;
	}

	public void setColumnList(final List<BaseColumn> columnList){
		this.columnList = columnList;
	}

	public String toString(){
		final String json = JacksonUtil.obj2json(this);
		return json;
	}

	public void setTableName(final String name){
		this.name = name;
	}

	public boolean isMain(){
		return this.isMain;
	}

	public void setMain(final boolean isMain){
		this.isMain = isMain;
	}

	public String getEntName(){
		return this.entName;
	}

	public void setEntName(final String entName){
		this.entName = entName;
	}

	public void setType(final String type){
		this.type = type;
	}

	public String getType(){
		return this.type;
	}

}
