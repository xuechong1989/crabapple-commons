package xc.crabapple.ssm.mybatis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 数据表元数据抽象类。 用于读取数据库表的元数据信息。 1.查询数据库中的表列表。 2.取得表的详细数据。
 */
public class BaseTableMeta{

	/**
	 * 根据表名，使用模糊匹配，查询系统中的表。返回的用Map表示的数据对象。key = 表名，value = 表注解。
	 */
	public Map<String, String> getTablesByName(final String tableName){
		return null;
	}

	/**
	 * 根据表名查询系统中的表。 返回的Map中：key = 表名，value = 表comment
	 */
	public Map<String, String> getTablesByName(final List<String> names){
		return null;
	}

	/**
	 * 根据表名，取得表模型。此处对表名进行非模糊匹配。
	 * @param tableName		表名
	 * @return 表模型
	 */
	public BaseTable getTableByName(final String tableName){
		return null;
	}

	/**
	 * 根据表名,使用模糊匹配，查询系统中的表。返回的用Map表示的数据对象。key = 表名，value = 表注解。
	 */
	public List<BaseTable> getTableModelByName(final String tableName){
		return null;
	}

	public Map<String,List<BaseColumn>> getColumnsByTableName(final List<String> tableNames){
		return null;
	}

	public void setComlumns(final List<BaseTable> tableModels){
		List<String> tableNames = new ArrayList<>();
		// get all table names
		for (BaseTable table : tableModels){
			tableNames.add(table.getTableName());
		}
		// batch get table columns
		Map<String, List<BaseColumn>> tableColumnsMap = getColumnsByTableName(tableNames);
		// extract table columns from paraTypeMap by table name;
		for (Entry<String, List<BaseColumn>> entry : tableColumnsMap.entrySet()){
			// set TableModel's columns
			for (BaseTable table : tableModels){
				if (table.getTableName().equalsIgnoreCase(entry.getKey())){
					table.setColumnList(entry.getValue());
				}
			}
		}
	}

	public void convertToMap(final Map<String, List<BaseColumn>> map, final List<BaseColumn> columnModels){
		for (BaseColumn columnModel : columnModels){
			String tableName = columnModel.getTableName();
			if (map.containsKey(tableName)){
				map.get(tableName).add(columnModel);
			} else {
				List<BaseColumn> cols = new ArrayList<>();
				cols.add(columnModel);
				map.put(tableName, cols);
			}
		}
	}

}
