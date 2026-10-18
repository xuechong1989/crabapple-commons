package xc.crabapple.ssm.mybatis.pagination;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.springframework.jdbc.datasource.DataSourceUtils;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.MapUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.servlet.ServletUtil;
import xc.crabapple.ssm.mybatis.MyBatisUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 2022-04-14
 * 2025-01-19 代码扩展与整理
 * 2025-01-21 添加默认值
 * 2025-09-11 分页相关代码整理
 */
public final class PaginationUtil {

    // 分页默认值
    public static Integer pageIndex = 0;  // 分页配置，第一页索引号，从0开始
    public static Integer pageSize = 20;  // 分页配置，每页条数

    /**
     * 2025-01-21
     * @param pageIndex 分页配置，第一页索引号，从0开始
     * @param pageSize 分页配置，每页条数
     */
    public static final void init(final Integer pageIndex, final Integer pageSize){
        PaginationUtil.pageIndex = pageIndex;
        PaginationUtil.pageSize = pageSize;
    }

    /**
     * 2025-01-19
     * 追加分页信息
     * @return
     */
    public static final Map<String, Object> append(){
        Map<String, Object> params = new HashMap<>();
        params = append(params);
        return params;
    }

    /**
     * 2025-01-19
     * 追加分页信息
     * 要求客户端传参：pageIndex / pageSize / sortField / sortOrder
     * @param params
     * @return
     */
    public static final Map<String, Object> append(final Map<String, Object> params){
        Integer pageIndex = ServletUtil.getInteger("pageIndex", PaginationUtil.pageIndex);
        Integer pageSize = ServletUtil.getInteger("pageSize", PaginationUtil.pageSize);
        //
        String sortField = ServletUtil.getString("sortField");
        String sortOrder = ServletUtil.getString("sortOrder");
        sortField = StringUtil.deleteAll(sortField, " ");
        sortOrder = StringUtil.deleteAll(sortOrder, " ");
        boolean flag1 = StringUtil.isBlank(sortField);
        boolean flag2 = StringUtil.isBlank(sortOrder);
        String orderBy = "";
        if(!flag1 && !flag2){
            orderBy = sortField + " " + sortOrder;
        }
        //
        Map<String, Object> localParams = append(pageIndex, pageSize, orderBy, params);
        return localParams;
    }

    /**
     * 2025-01-19
     * 追加分页信息
     * @return
     */
    public static final Map<String, Object> append(final Integer pageIndex){
        Map<String, Object> params = append(pageIndex, PaginationUtil.pageSize, "", null);
        return params;
    }

    /**
     * 2025-01-19
     * 追加分页信息
     * @return
     */
    public static final Map<String, Object> append(final Integer pageIndex, final Integer pageSize){
        Map<String, Object> params = append(pageIndex, pageSize, "", null);
        return params;
    }

    /**
     * 2025-01-19
     * 追加分页信息
     * @return
     */
    public static final Map<String, Object> append(final Integer pageIndex, final Map<String, Object> params){
        Map<String, Object> localParams = append(pageIndex, PaginationUtil.pageSize, "", params);
        return localParams;
    }

    /**
     * 2025-01-19
     * 追加分页信息
     * @return
     */
    public static final Map<String, Object> append(final Integer pageIndex, final Integer pageSize, final String orderBy){
        Map<String, Object> params = append(pageIndex, pageSize, orderBy, null);
        return params;
    }

    /**
     * 2025-01-19
     * 追加分页信息
     * @return
     */
    public static final Map<String, Object> append(final Integer pageIndex, final Integer pageSize, final Map<String, Object> params){
        Map<String, Object> localParams = append(pageIndex, pageSize, "", params);
        return localParams;
    }

    /**
     * 2025-01-19
     * 追加分页信息
     * @return
     */
    public static final Map<String, Object> append(final Integer pageIndex, final Integer pageSize, final String orderBy, final Map<String, Object> params){
        Map<String, Object> localParams = params;
        if(localParams == null){
            localParams = new HashMap<>();
        }
        //
        if(pageSize == 0){
            localParams.put("pageSize", 0);  // 判断是否分页的依据
            localParams.put("needTotal", "no");
            return localParams;
        }
        if(pageSize == Integer.MAX_VALUE){
            localParams.put("needTotal", "no");
            return localParams;
        }
        // 真正分页
        Integer startIndex = pageSize * pageIndex;
        localParams.put("startIndex", startIndex);  // 即offset
        localParams.put("pageIndex", pageIndex);
        localParams.put("pageSize", pageSize);
        localParams.put("needTotal", "yes");
        //
        localParams.put("orderBy", orderBy);
        //
        return localParams;
    }

    /**
     * 2025-01-19
     * 设置默认的排序字段和排序规则
     * @param params
     * @param defaultOrderBy 格式如 code_ ASC
     */
    public static final void setOrderBy(final Map<String, Object> params, final String defaultOrderBy){
        if(StringUtil.isBlank(defaultOrderBy)){
            LogUtil.warn("PaginationUtil_setOrderBy_1", "分页默认值为空");
            return;
        }
        Object object = params.get("orderBy");
        if(object == null){
            params.put("orderBy", defaultOrderBy);
            return;
        }
        String orderBy = object.toString();
        if(StringUtil.isBlank(orderBy)){
            params.put("orderBy", defaultOrderBy);
        }
    }

    /**
     * 2025-01-19
     * 获取指定集合的总长度，注意是分页时是总条目数（而非当前页条目数）
     * @param list
     * @return
     */
    public static final Integer getSize(final List<?> list){
        if(CollectionUtil.isBlank(list)){
            return 0;
        }
        if(list instanceof PageList){
            PageList pageList = (PageList) list;
            Integer size = pageList.getTotal();
            return size;
        }
        Integer size = list.size();
        return size;
    }

    /**
     * 2025-01-19
     * @param list
     * @return
     */
    public static final Page getPage(final List list){
        Page page = new Page();
        page.setSkipCountTotal(false);
        Integer total = getSize(list);
        page.setTotalItems(total);
        Integer pageIndex = ServletUtil.getInteger("pageIndex", PaginationUtil.pageIndex);
        Integer pageSize = ServletUtil.getInteger("pageSize", PaginationUtil.pageSize);
        page.setPageIndex(pageIndex);
        page.setPageSize(pageSize);
        return page;
    }

    /**
     * 2025-01-19
     * 物理分页
     * @param invocation
     * @param mappedStatement
     * @param paramMap
     * @param queryArgs
     * @param methodName
     * @param boundSql
     * @param standardSQL
     * @return
     */
    public static final Object getPageList(final Invocation invocation, final MappedStatement mappedStatement, final BoundSql boundSql, final Map<String, Object> paramMap, final Object[] queryArgs, final String methodName, final String standardSQL){
        try{
            Object pageSizeObj = MapUtil.get(paramMap, "pageSize");
            if(pageSizeObj == null){
                Object object = MyBatisUtil.proceed(invocation, queryArgs, paramMap, methodName, standardSQL);
                return object;
            }
            String pageSizeStr = pageSizeObj.toString();
            final Integer pageSize = Integer.valueOf(pageSizeStr);
            if(pageSize <= 0){
                Object object = MyBatisUtil.proceed(invocation, queryArgs, paramMap, methodName, standardSQL);
                return object;
            }
            //
            // 2026-06-04 超大片页量（>=99999）跳过SQL分页包装，但仍需应用orderBy排序，否则全量查询的排序参数会丢失
            if(pageSize == Integer.MAX_VALUE || pageSize >= 99999){
                String noPageSQL = standardSQL;
                Object orderByObj2 = MapUtil.get(paramMap, "orderBy");
                if(orderByObj2 != null){
                    final String orderByStr = orderByObj2.toString();
                    if(!StringUtil.isBlank(orderByStr)){
                        noPageSQL = PaginationUtil.getSortString(noPageSQL, orderByStr);
                    }
                }
                queryArgs[0] = p_mappedStatementHandler(mappedStatement, boundSql, noPageSQL);
                Object object = MyBatisUtil.proceed(invocation, queryArgs, paramMap, methodName, standardSQL);
                return object;
            }
            String pageSQL = standardSQL;
            Object object = MapUtil.get(paramMap, "orderBy");  // group by, sort
            if(object != null){
                final String orderBy = object.toString();
                if(!StringUtil.isBlank(orderBy)){
                    pageSQL = PaginationUtil.getSortString(pageSQL, orderBy);
                }
            }
            final Integer startIndex = MapUtil.get(paramMap, "startIndex");  // limit
            pageSQL = PaginationUtil.getLimitString(pageSQL, startIndex, pageSize);
            queryArgs[0] = p_mappedStatementHandler(mappedStatement, boundSql, pageSQL);
            //
            List list = null;
            try {
                Object proceedResult = MyBatisUtil.proceed(invocation, queryArgs, paramMap, methodName, standardSQL);
                if (proceedResult != null) {
                    list = (List) proceedResult;
                }
            } catch (Throwable throwable) {
                LogUtil.error("PaginationUtil_getPageList_call_t1", throwable, methodName, paramMap, standardSQL);
            }
            if (list == null) {
                list = new ArrayList();
            }
            // 处理总数
            final String needTotal = (String) paramMap.get("needTotal");
            if ("yes".equals(needTotal)) {
                final PageResult pageResult = PaginationUtil.getPageResult(mappedStatement, paramMap, standardSQL);
                PageList pageList = new PageList(list, pageResult);
                return pageList;
            }
            PageList pageList = new PageList(list);
            return pageList;
        }catch (Throwable throwable){
            LogUtil.error("PaginationUtil_getPageList_t2", throwable, methodName, paramMap, standardSQL);
            return null;
        }
    }

    /**
     * 2025-01-19
     * @param mappedStatement
     * @param boundSql
     * @param sql
     * @return
     */
    private static final MappedStatement p_mappedStatementHandler(final MappedStatement mappedStatement, final BoundSql boundSql, final String sql){
        Configuration configuration = mappedStatement.getConfiguration();
        List<ParameterMapping> parameterMappingList = boundSql.getParameterMappings();
        Object parameterObject = boundSql.getParameterObject();
        BoundSql newBoundSql = new BoundSql(configuration, sql, parameterMappingList, parameterObject);
        for (ParameterMapping parameterMapping : parameterMappingList){
            final String property = parameterMapping.getProperty();
            if(boundSql.hasAdditionalParameter(property)){
                Object additionalParameter = boundSql.getAdditionalParameter(property);
                newBoundSql.setAdditionalParameter(property, additionalParameter);
            }
        }
        final SqlSource sqlSource = new SqlSource(){
            private BoundSql boundSql;
            public SqlSource setBoundSql(BoundSql boundSql){
                this.boundSql = boundSql;
                return this;
            }
            @Override
            public BoundSql getBoundSql(Object object){
                return this.boundSql;
            }
        }.setBoundSql(newBoundSql);
        String id = mappedStatement.getId();
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        MappedStatement.Builder builder = new MappedStatement.Builder(configuration, id, sqlSource, sqlCommandType);
        builder.resource(mappedStatement.getResource());
        builder.fetchSize(mappedStatement.getFetchSize());
        builder.statementType(mappedStatement.getStatementType());
        builder.keyGenerator(mappedStatement.getKeyGenerator());
        String[] keyProperties = mappedStatement.getKeyProperties();
        if(!CollectionUtil.isBlank(keyProperties)){
            StringBuilder stringBuilder = new StringBuilder();
            for (String keyProperty : keyProperties){
                stringBuilder.append(keyProperty).append(",");
            }
            int len = stringBuilder.length();
            stringBuilder.delete(len - 1, len);
            builder.keyProperty(stringBuilder.toString());
        }
        builder.timeout(mappedStatement.getTimeout());
        builder.parameterMap(mappedStatement.getParameterMap());
        builder.resultMaps(mappedStatement.getResultMaps());
        builder.resultSetType(mappedStatement.getResultSetType());
        builder.cache(mappedStatement.getCache());
        builder.flushCacheRequired(mappedStatement.isFlushCacheRequired());
        builder.useCache(mappedStatement.isUseCache());
        MappedStatement newMappedStatement = builder.build();
        return newMappedStatement;
    }

    /**
     * 2025-01-19
     * @return
     */
    public static final PageResult getPageResult(final MappedStatement mappedStatement, final Map<String, Object> paramMap, final String sql) {
        try {
            BoundSql boundSql = mappedStatement.getBoundSql(paramMap);
            final Integer startIndex = (Integer) paramMap.get("startIndex");
            final Integer pageSize = (Integer) paramMap.get("pageSize");
            Integer count = 0;
            if(!CollectionUtil.isBlank(paramMap)){
                if(paramMap.containsKey("_skipCount")){
                    count = 0;
                }
            }
            String orderByPart = "";
            String localSql = sql;
            String loweredString = localSql.toLowerCase();
            int orderByIndex = loweredString.indexOf("order by");
            if(orderByIndex != -1){
                orderByPart = localSql.substring(orderByIndex);
            }
            if(!StringUtil.isBlank(orderByPart)){
                localSql = localSql.replace(orderByPart, "");
            }
            final String countSQL = "select count(1) from (" + localSql + ") tmp_count";
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            DataSource dataSource = mappedStatement.getConfiguration().getEnvironment().getDataSource();
            try{
                connection=  DataSourceUtils.getConnection(dataSource);
                preparedStatement = connection.prepareStatement(countSQL);
                DefaultParameterHandler handler = new DefaultParameterHandler(mappedStatement, paramMap, boundSql);
                handler.setParameters(preparedStatement);
                resultSet = preparedStatement.executeQuery();
                if(resultSet.next()){
                    count = resultSet.getInt(1);
                }
            } finally {
                IOUtil.close(resultSet, preparedStatement);
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
            PageResult pageResult = new PageResult(startIndex, pageSize, count);
            return pageResult;
        }catch (Throwable throwable){
            LogUtil.error("PaginationUtil_getPageResult_t1", throwable, sql);
            return null;
        }
    }

    /**
     * 2023-08-21
     * 2025-09-11 代码整理
     * 转换分页sql
     * @Param params
     * @Param rowBounds
     * @return 字符串sql
     */
    public static final String getLimitString(final Map<String, Object> params, final RowBounds rowBounds) {
        int offset;
        int limit;
        if(Objects.isNull(rowBounds)) {
            offset = PaginationUtil.pageIndex;
            limit = PaginationUtil.pageSize;
        }else {
            offset = rowBounds.getOffset();
            limit = rowBounds.getLimit();
        }
        String sql = MyBatisUtil.convertSql(params);
        sql = getLimitString(sql, offset, limit);
        return sql;
    }

    /**
     * 2023-06-16
     * 2025-09-11 代码整理
     * 2026-06-02 信创国产化-达梦/人大金仓 LIMIT语法兼容：DM使用LIMIT rows OFFSET offset
     * 2026-06-03 OOM修复：Oracle分页兼容，使用ANSI SQL标准OFFSET...FETCH语法
     * 参数改为封装类型，添加参数非空判断
     * 将sql变成分页sql语句,直接使用offset,limit的值作为占位符.</br> 源代码为:
     * p_getLimitString(sql, offset, String.valueOf(offset), limit, String.valueOf(limit))
     * @param sql sql语句
     * @param offset 记录编号
     * @param limit 页大小
     */
    public static final String getLimitString(final String sql, final Integer offset, final Integer limit){
        int localOffset = (offset == null || offset < 0) ? 0 : offset;
        int localLimit = (limit == null || limit <= 0) ? PaginationUtil.pageSize : limit;
        String localSql;
        // 信创国产化-达梦/人大金仓 使用ANSI SQL标准 LIMIT rows OFFSET offset
        if(StringUtil.equalsOfOne(MyBatisUtil.dbType, MyBatisUtil.DB_DM, MyBatisUtil.DB_KINGBASE)){
            localSql = sql + " LIMIT " + localLimit + " OFFSET " + localOffset;
        }else if(StringUtil.equalsOfOne(MyBatisUtil.dbType, MyBatisUtil.DB_ORACLE)){
            // 2026-06-03 Oracle 12c+ ANSI SQL标准分页语法 OFFSET n ROWS FETCH NEXT m ROWS ONLY
            localSql = sql + " OFFSET " + localOffset + " ROWS FETCH NEXT " + localLimit + " ROWS ONLY";
        }else{
            localSql = sql + " LIMIT " + localOffset + "," + localLimit;
        }
        return localSql;
    }

    /**
     * 2025-01-19
     * 将sql转换为带排序的SQL
     * @param sql SQL语句
     * @param orderBy 排序对象 格式如 ID_,CREATE_TIME_ DESC
     */
    public static final String getSortString(final String sql, final String orderBy){
        if(StringUtil.isBlank(orderBy)){
            return sql;
        }
        StringBuilder stringBuilder = new StringBuilder("SELECT * FROM (").append(sql).append(") TEMP_ORDER ORDER BY ");
        stringBuilder.append(orderBy).append(", ");
        stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
        String localSql = stringBuilder.toString();
        return localSql;
    }

    /**
     * 2024-07-28
     * 分页算法，规则是从0开始。
     * 示例： (100, 201) ==> {[0,100], [100,100], [200,1]}
     * @param pageSize 每页的数量
     * @param TOTAL 总数量
     * @return
     */
    public static final List<Integer[]> getList(final int pageSize, final int TOTAL){
        if(TOTAL <= 0){
            return null;
        }
        if(pageSize <= 0){
            return null;
        }
        int localPageSize = pageSize;
        if(TOTAL < localPageSize){
            localPageSize = TOTAL;
        }
        List<Integer[]> list = new ArrayList<>();
        for(int rowIndex = 0; rowIndex < TOTAL; rowIndex += localPageSize){
            int rows = TOTAL - rowIndex;
            if(rows > localPageSize){
                rows = localPageSize;
            }
            Integer[] intArray = {rowIndex, rows};
            list.add(intArray);
        }
        return list;
    }

}
