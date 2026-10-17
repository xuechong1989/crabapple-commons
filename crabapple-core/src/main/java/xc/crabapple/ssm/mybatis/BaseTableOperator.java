package xc.crabapple.ssm.mybatis;

import xc.crabapple.core.log.LogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 操作数据表基类。
 */
public class BaseTableOperator {

	public boolean hasData(final String tableName){
		String sql = "select count(*) from " + tableName;
		int rtn = MyBatisUtil.jdbcTemplate.queryForObject(sql, Integer.class);
		return rtn > 0;
	}

	public String replaceLineThrough(final String partition){
		return partition.toUpperCase().replaceAll("-", "");
	}

	public void createIndex(final String name, final String table, final String colName){
		String sql = "CREATE INDEX "+name+" ON "+table+" ("+colName+")";
		MyBatisUtil.jdbcTemplate.execute(sql);
	}

	public void dropColumn(final String tableName, final String column){
		String sql = getDropColumnSql(tableName,column);
		MyBatisUtil.jdbcTemplate.execute(sql);
	}

	public String getDropTableSql(final String tableName){
		String sql = "drop table " + tableName;
		return sql;
	}

	public String getDropColumnSql(final String tableName, final String column){
		String sql = "ALTER TABLE "+tableName+"   DROP COLUMN	" + column +";";
		return sql;
	}

	public List<String> getCreateTableSql(final BaseTable table){
		LogUtil.error("BaseTableOperator_getCreateTableSql_e1", "不支持此方法");
		return null;
	}

	public List<String> getAddColumnSql(final String tableName, final BaseColumn column){
		LogUtil.error("BaseTableOperator_getAddColumnSql_e1", "不支持此方法");
		return null;
	}

	public List<String> getUpdateColumnSql(final String tableName, final String columnName, final BaseColumn column){
		LogUtil.error("BaseTableOperator_getUpdateColumnSql_e1", "不支持此方法");
		return null;
	}

	public String getUpdateTableCommentSql(final String tableName, final String comment){
		LogUtil.error("BaseTableOperator_getUpdateTableCommentSql_e1", "不支持此方法");
		return null;
	}

	public String getAddForeignKeySql(final String pkTableName, final String fkTableName, final String pkField, final String fkField){
		LogUtil.error("BaseTableOperator_getAddForeignKeySql_e1", "不支持此方法");
		return null;
	}

	public String getDropForeignKeySql(final String tableName, final String keyName){
		LogUtil.error("BaseTableOperator_getDropForeignKeySql_e1", "不支持此方法");
		return null;
	}

	/**
	 * 获取字段类型
	 * @param columnType 字段类型
	 * @param charLen           字符串长度
	 * @param intLen 整型长度
	 * @param decimalLen        小数长度
	 */
	public String getColumnType(final String columnType, final int charLen, final int intLen, final int decimalLen){
		return null;
	}

	/**
	 * 获取字段类型
	 * @param column	字段
	 */
	public String getColumnType(final BaseColumn column){
		return null;
	}

	/**
	 * 根据Tble创建表。
	 * @param table 表
	 */
	public void createTable(final BaseTable table){
		return;
	}

	/**
	 * 根据表名删除表。
	 */
	public void dropTable(final String tableName){
		return;
	}

	/**
	 * 修改表的注释
	 * @param tableName 表名
	 * @param comment 表注释
	 */
	public void updateTableComment(final String tableName, final String comment){
		return;
	}

	/**
	 * 在表中添加列。
	 * @param tableName 表名
	 * @param column 字段
	 */
	public void addColumn(final String tableName, final BaseColumn column){
		return;
	}

	/**
	 * 更新列。
	 * 可以修改列名，字段类型，字段是否非空，字段的注释。
	 * @param tableName 表名
	 * @param columnName	列名
	 * @param column 字段
	 */
	public void updateColumn(final String tableName, final String columnName, final BaseColumn column){
		return;
	}

	/**
	 * 添加外键
	 * @param pkTableName	主键表
	 * @param fkTableName	外键表
	 * @param pkField	主键
	 * @param fkField	 外键
	 */
	public void addForeignKey(final String pkTableName, final String fkTableName, final String pkField, final String fkField){
		return;
	}

	/**
	 * 删除外键
	 */
	public void dropForeignKey(final String tableName, final String keyName){
		return;
	}

	/**
	 * 根据表名，取得相应的主键的列表
	 */
	public List<String> getPKColumns(final String tableName){
		return null;
	}

	/**
	 * 根据表名列表，取得相应的主键的列列表的Map。Map中:key = "name",value = 表名；key = columns，value =
	 * 相应的主键的列列表。
	 * @param tableNames	表名列表
	 */
	public Map<String, List<String>> getPKColumns(final List<String> tableNames){
		return null;
	}

	/**
	 * 判断表是否存在。
	 */
	public boolean isTableExist(final String tableName){
		return true;
	}

	/**
	 * 判断指定表的分区是否存在。
	 */
	public boolean isExsitPartition(final String tableName, final String partition){
		return true;
	}

	/**
	 * 创建分区。
	 */
	public void createPartition(final String tableName, final String partition){
		return;
	}

	/**
	 * 是否支持分区。
	 * 判断表是否有创建分区。
	 */
	public boolean supportPartition(final String tableName){
		return true;
	}

	/**
	 * 根据旧表生成新的表名。
	 */
	public void createTableLike(final String newTable, final String oldTable, final String pk){

	}


	/**
	 * 2024-09-24 信创国产化-达梦 兼容mysql的information_schema
	 * 请继承实现
	 * @param databaseName
	 * @param likeTableName
	 * @return
	 */
	public List<String> getTableNameByLike(final String databaseName, final String likeTableName) {
		LogUtil.error("BaseTableOperator_getTableNameByLike_11","请在需要使用的数据库下实现该方法！");
		return new ArrayList<>();
	}

	/**
	 * 2024-09-24 信创国产化-达梦 兼容mysql的information_schema
	 * 请继承实现
	 * @param databaseName
	 * @param tableName
	 * @return
	 */
	public List<String> getColumnByTableName(final String databaseName, final String tableName) {
		LogUtil.error("BaseTableOperator_getColumnByTableName_11","请在需要使用的数据库下实现该方法！");
		return new ArrayList<>();
	}

	/**
	 * 2024-09-24 信创国产化-达梦 兼容mysql的information_schema
	 * 请继承实现
	 * @param tableName
	 * @return
	 */
	public List<String> getColumnByTableName(final String tableName){
		LogUtil.error("BaseTableOperator_getColumnByTableName_12","请在需要使用的数据库下实现该方法！");
		return new ArrayList<>();
	}

	/**
	 * 2024-09-24 信创国产化-达梦 兼容mysql的information_schema
	 * 请继承实现
	 * @param tableName
	 * @return
	 */
	public Integer getTablesCountByTableName(final String databaseName, final String tableName) {
		LogUtil.error("BaseTableOperator_getTablesCountByTableName_11","请在需要使用的数据库下实现该方法！");
		return null;
	}

	/**
	 * 2024-09-24 信创国产化-达梦 兼容mysql的information_schema
	 * 请继承实现
	 * @param tableName
	 * @return
	 */
	public Integer getTablesCountByTableName(final String tableName){
		LogUtil.error("BaseTableOperator_getTablesCountByTableName_12","请在需要使用的数据库下实现该方法！");
		return null;
	}

	/**
	 * 2024-09-24 信创国产化-达梦 兼容mysql的information_schema
	 * 请继承实现
	 * @param databaseName
	 * @param tableName
	 * @return
	 */
	public List<Map<String,Object>> getColumnNamAndComment(final String databaseName, final String tableName) {
		LogUtil.error("BaseTableOperator_getColumnNamAndComment_12","请在需要使用的数据库下实现该方法！");
		return new ArrayList<>();
	}

	/**
	 * 2024-09-24 信创国产化-达梦 兼容mysql的information_schema
	 * 请继承实现
	 * @param databaseName
	 * @param likeTableName
	 * @return
	 */
	public List<Map<String,String>> getTableNameAndColumnName(final String databaseName, final String likeTableName) {
		LogUtil.error("BaseTableOperator_getTableNameAndColumnName_12","请在需要使用的数据库下实现该方法！");
		return new ArrayList<>();
	}

	/**
	 * 2024-09-24 信创国产化-达梦 兼容mysql的information_schema
	 * 请继承实现
	 * @param databaseName
	 * @param likeTableName
	 * @return
	 */
	public List<Map<String,Object>> getTableNameAndColumnNameByDataTypeList(final String databaseName, final String likeTableName, final List<String> dataTypeList) {
		LogUtil.error("BaseTableOperator_getTableNameAndColumnNameByDataTypeList_12","请在需要使用的数据库下实现该方法！");
		return new ArrayList<>();
	}

	/**
	 * 2024-09-24 获取表的索引列
	 * 请继承实现
	 * @param tableName
	 * @param likeKeyName 索引名
	 * @return
	 */
	public List<Map<String,Object>> getIndexColumnByTableNameAndKeyName(final String tableName, final String likeKeyName) {
		LogUtil.error("BaseTableOperator_getIndexColumnByTableNameAndKeyName_12","请在需要使用的数据库下实现该方法！");
		return new ArrayList<>();
	}

	/**
	 * 2024-11-18 获取表的主键list
	 * @param cachePriKeyColumnMap
	 * @param tableName
	 * @return
	 */
	public List<String> getTablePriColum(final Map<String,List<String>> cachePriKeyColumnMap, final String tableName){
		LogUtil.error("BaseTableOperator_getTablePriColum_12","请在需要使用的数据库下实现该方法！");
		return new ArrayList<>();
	}
}
