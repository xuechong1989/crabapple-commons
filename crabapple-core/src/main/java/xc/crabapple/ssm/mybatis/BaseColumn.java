package xc.crabapple.ssm.mybatis;

import xc.crabapple.core.collection.StringUtil;

import java.io.Serializable;

/**
 * 默认列对象。 用于产生数据库列。
 */
public class BaseColumn implements Serializable {

	public static final long serialVersionUID = 1L;

	// 字段常用变量
	/** 字符串 */
	public static final String COLUMN_TYPE_VARCHAR  = "varchar";
	/** 大文本 */
	public static final String COLUMN_TYPE_CLOB = "clob";
	/** 数字 */
	public static final String COLUMN_TYPE_NUMBER = "number";
	/** 整型 */
	public static final String COLUMN_TYPE_INT = "int";
	/** 日期 */
	public static final String COLUMN_TYPE_DATE = "date";
	// 用于date类型转datetime(3)的
	public static final String COLUMN_TYPE_DATETODATETIME3 = "dateToDatetime3";
	// text
	public static final String COLUMN_TYPE_TEXT = "text";

	// 列名
	private String name = "";
	// 列注释
	private String comment = "";
	// 是否主键
	private boolean isPk = false;
	// 是否外键
	private boolean isFk = false;
	// 是否可为空
	private boolean isNull = true;
	// 列类型
	private String columnType;
	// 字符串长度
	private int charLen = 0;
	// 小数位
	private int decimalLen = 0;
	// 整数位长度
	private int intLen = 0;
	// 外键reference table
	private String fkRefTable = "";
	// 外键reference column
	private String fkRefColumn = "";
	// 默认值
	private String defaultValue = "";
	// 列所有的表
	private String tableName = "";
	// 列所有的表
	private int isRequired = 0;
	// Select 'x' as label的label
	private String label;
	// 与ResutlSet的列索引等同。以1开始计数。
	private int index;
	private String originName = "";

	public String getFieldName(){
		return name;
	}

	public void setFieldName(final String name){
		this.name = name;
	}

	public String getComment(){
		if (!StringUtil.isBlank(comment))
			comment = comment.replace("'", "''");

		return comment;
	}

	public void setComment(final String comment){
		this.comment = comment;
	}

	public boolean getIsPk(){
		return isPk;
	}

	public void setIsPk(final boolean isPk){
		this.isPk = isPk;
	}

	public boolean getIsNull(){
		return isNull;
	}

	public void setIsNull(final boolean isNull){
		this.isNull = isNull;
	}

	public String getColumnType(){
		return columnType;
	}

	public void setColumnType(final String columnType){
		this.columnType = columnType;
	}

	public int getCharLen(){
		return charLen;
	}

	public void setCharLen(final int charLen){
		this.charLen = charLen;
	}

	public int getDecimalLen(){
		return decimalLen;
	}

	public void setDecimalLen(final int decimalLen){
		this.decimalLen = decimalLen;
	}

	public int getIntLen(){
		return intLen;
	}

	public void setIntLen(final int intLen){
		this.intLen = intLen;
	}

	public boolean getIsFk(){
		return isFk;
	}

	public void setIsFk(final boolean isFk){
		this.isFk = isFk;
	}

	public String getFkRefTable(){
		return fkRefTable;
	}

	public void setFkRefTable(final String fkRefTable){
		this.fkRefTable = fkRefTable;
	}

	public String getFkRefColumn(){
		return fkRefColumn;
	}

	public void setFkRefColumn(final String fkRefColumn){
		this.fkRefColumn = fkRefColumn;
	}

	public String getDefaultValue(){
		return defaultValue;
	}

	public void setDefaultValue(final String defaultValue){
		this.defaultValue = defaultValue;
	}

	public String getTableName(){
		return tableName;
	}

	public void setTableName(final String tableName){
		this.tableName = tableName;
	}

	public String getLabel(){
		return label;
	}

	public void setLabel(final String label){
		this.label = label;
	}

	public int getIndex(){
		return index;
	}

	public void setIndex(final int index){
		this.index = index;
	}

	public int getIsRequired(){
		return this.isRequired;
	}

	public void setIsRequired(final int isRequired){
		this.isRequired = isRequired;
	}

	public String getOriginName(){
		return this.originName;
	}

	public void setOriginName(final String name){
		this.originName = name;
	}

}
