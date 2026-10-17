package xc.crabapple.ssm.mybatis;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.druid.proxy.jdbc.NClobProxy;
import dm.jdbc.driver.DmdbNClob;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.MapUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.regex.RegexUtil;
import xc.crabapple.core.timer.TimerUtil;
import xc.crabapple.ssm.mybatis.interceptor.DBCipherUtil;
import xc.crabapple.ssm.mybatis.pagination.PaginationUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 2023-05-21
 * 2025-09-11 分页相关代码整理
 */
public final class MyBatisUtil {

    private static DruidDataSource p_druidDataSource;
    public static JdbcTemplate jdbcTemplate;
    public static DataSourceTransactionManager dataSourceTransactionManager;
    public static NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    public static MybatisSqlSessionFactoryBean mybatisSqlSessionFactoryBean;
    public static SqlSessionFactory sqlSessionFactory;
    public static SqlSessionTemplate sqlSessionTemplate;
    public static TransactionInterceptor transactionInterceptor;
    public static DefaultPointcutAdvisor defaultPointcutAdvisor;
    public static BeanNameAutoProxyCreator beanNameAutoProxyCreator;
    // 数据库类型
    public static final String DB_ORACLE = "oracle";
    public static final String DB_MYSQL = "mysql";
    public static final String DB_SQLSERVER2005 = "sqlserver2005";
    public static final String DB_SQLSERVER2008 = "sqlserver2008";
    public static final String DB_DB2 = "db2";
    public static final String DB_DM = "dm";
    public static final String DB_KINGBASE = "kingbase";
    // 当前使用的数据库类型
    public static String dbType = DB_MYSQL;
    public static String dbVersion = "8";  // 数据库版本
    // 2023-08-18 关键字
    private static final String keywordStart = "#{";
    private static final String keywordEnd = "}";
    private static final String commaKeyword = ",";
    // 2026-10-12 MySQL下SQL拼接未转义反斜杠导致表单保存失败
    // 数据库是否MySQL的判定结果缓存：s_mysqlDbTypeUrl记录判定时的连接串，连接串变化时重新判定
    private static String s_mysqlDbTypeUrl = null;
    private static Boolean s_mysqlDbType = null;
    //
    public static final String BATCH_SQLSESSION = "sqlSession";
    public static final String BATCH_MAPPER = "mapper";

    /**
     * 2025-09-20
     * 批处理写入，SQL批量执行
     * @param mapperCls
     * @return
     */
    public static Map<String, Object> batchMap(final Class mapperCls){
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        Object mapper = sqlSession.getMapper(mapperCls);
        Map<String, Object> map = new HashMap<>(2);
        map.put(BATCH_SQLSESSION, sqlSession);
        map.put(BATCH_MAPPER, mapper);
        return map;
    }

    /**
     * 2025-09-20
     * 批处理写入，SQL批量执行
     * @param sqlSession
     * @param size 待执行的总记录数
     * @param perSize 每次执行的记录数
     * @param count 记录执行的序号，从0开始
     */
    public static final int batchFlush(final SqlSession sqlSession, final int size, final int perSize, final int count){
        if ((count + 1) % perSize == 0 || count == size - 1) {
            sqlSession.flushStatements();  // 执行批处理
            sqlSession.clearCache();  // 可选，防止内存占用过高
        }
        int nextCount = count + 1;
        return nextCount;
    }

    /**
     * 2025-09-20
     * 批处理写入，SQL批量执行
     * @param sqlSession
     */
    public static final void batchCommit(final SqlSession sqlSession){
        sqlSession.flushStatements();  // 执行批处理
        sqlSession.clearCache();  // 可选，防止内存占用过高
        sqlSession.commit();
        IOUtil.close(sqlSession);
    }

    /**
     * 2025-02-11
     * @param basePackage 模块包全路径名，格式如：com.example.module
     * @return
     */
    public static final MapperScannerConfigurer getMapperScannerConfigurer(final String basePackage) {
        if (StringUtil.isBlank(basePackage)) {
            LogUtil.error("MyBatisUtil_getMapperScannerConfigurer_1", "basePackage为空");
            return null;
        }
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        mapperScannerConfigurer.setBasePackage(basePackage);
        mapperScannerConfigurer.setLazyInitialization("false");
        return mapperScannerConfigurer;
    }

    /**
     * 2025-01-09
     *
     * @param druidDataSource
     * @param dbType
     * @param xmlUrl
     */
    public static final void init(DruidDataSource druidDataSource, final String dbType, final String xmlUrl) {
        init(druidDataSource, dbType, xmlUrl, "no", null, null, null, null, null);
    }

    /**
     * 2025-01-09
     *
     * @param druidDataSource
     * @param dbType          格式如：mysql
     * @param xmlUrl          格式如：com/example/module/星号星号/星号.xml
     * @param cipherEnable
     * @param cipherKey
     * @param cipherIv
     * @param cipherMode
     * @param cipherPadding
     * @param classMap        classMap的构造过程举例：
     *                        Map<Class, List<String>> classMap = new HashMap<>();
     *                        classMap.put(OsUser.class, ListUtil.getList("userNo", "mobile"));
     *                        classMap.put(PdfJiaqianDao.class, ListUtil.getList("username"));
     *                        classMap.put(List.class, ListUtil.getList("USER_NO_", "MOBILE_"));
     */
    public static final void init(final DruidDataSource druidDataSource, final String dbType, final String xmlUrl, final String cipherEnable, final String cipherKey, final String cipherIv, final String cipherMode, final Integer cipherPadding, final Map<Class, List<String>> classMap) {
        setDbType(dbType);
        setDbVersion(MyBatisUtil.dbVersion);
        //
        setDruidDataSource(druidDataSource);
        //
        initJdbcTemplate();
        initDataSourceTransactionManager();
        initNamedParameterJdbcTemplate();
        initMybatisSqlSessionFactoryBean(xmlUrl);
        initSqlSessionFactory();
        initSqlSessionTemplate();
        initBeanNameAutoProxyCreator();
        //
        setCipher(cipherEnable, cipherKey, cipherIv, cipherMode, cipherPadding, classMap);
    }

    /**
     * 2025-06-22
     * 设置数据库类型
     *
     * @param dbType mysql, oracle, sqlserver, db2, dm（信创国产化-达梦）, kingbase（信创国产化-人大金仓）
     */
    public static final void setDbType(final String dbType) {
        String effectiveDbType = StringUtil.getStandardStr(dbType);
        effectiveDbType = StringUtil.toLowerCase(effectiveDbType);
        if (StringUtil.isBlank(effectiveDbType)) {
            effectiveDbType = MyBatisUtil.DB_MYSQL;
        }
        MyBatisUtil.dbType = effectiveDbType;
    }

    /**
     * 2025-06-23
     * 设置数据库版本
     *
     * @param dbVersion
     */
    public static final void setDbVersion(final String dbVersion) {
        String effectiveDbVersion = StringUtil.getStandardStr(dbVersion);
        effectiveDbVersion = StringUtil.toLowerCase(effectiveDbVersion);
        if (StringUtil.isBlank(effectiveDbVersion)) {
            effectiveDbVersion = "8";
        }
        MyBatisUtil.dbVersion = effectiveDbVersion;
    }

    /**
     * 2025-01-09
     */
    public static final void initJdbcTemplate() {
        jdbcTemplate = new JdbcTemplate();
        jdbcTemplate.setDataSource(p_druidDataSource);
    }

    /**
     * 2025-01-09
     */
    public static final void initDataSourceTransactionManager() {
        dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(p_druidDataSource);
    }

    /**
     * 2025-01-09
     */
    public static final void initNamedParameterJdbcTemplate() {
        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(p_druidDataSource);
    }

    /**
     * 2025-01-09
     *
     * @param xmlUrl 格式如：com/example/module/星号星号/星号.xml
     */
    public static final void initMybatisSqlSessionFactoryBean(final String xmlUrl) {
        mybatisSqlSessionFactoryBean = new MybatisSqlSessionFactoryBean();
        mybatisSqlSessionFactoryBean.setMybatisXmlUrl(xmlUrl);
        mybatisSqlSessionFactoryBean.setDataSource(p_druidDataSource);
    }

    /**
     * 2025-01-09
     */
    public static final void initSqlSessionFactory() {
        sqlSessionFactory = mybatisSqlSessionFactoryBean.getSqlSessionFactory();
    }

    /**
     * 2025-01-09
     */
    public static final void initSqlSessionTemplate() {
        sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
    }

    /**
     * 2025-01-09
     */
    public static final void initBeanNameAutoProxyCreator() {
        MyBatisUtil.beanNameAutoProxyCreator = new BeanNameAutoProxyCreator();
        MyBatisUtil.beanNameAutoProxyCreator.setProxyTargetClass(true);
        MyBatisUtil.beanNameAutoProxyCreator.setBeanNames("*ServiceImpl");
        MyBatisUtil.beanNameAutoProxyCreator.setInterceptorNames("transactionInterceptor");
    }

    /**
     * 2025-01-13
     * 数据库表字段加解密
     *
     * @param enable
     * @param key
     * @param iv
     * @param mode
     * @param padding
     * @param classMap classMap的构造过程举例：
     *                 Map<Class, List<String>> classMap = new HashMap<>();
     *                 classMap.put(OsUser.class, ListUtil.getList("userNo", "mobile"));
     *                 classMap.put(PdfJiaqianDao.class, ListUtil.getList("username"));
     *                 classMap.put(List.class, ListUtil.getList("USER_NO_", "MOBILE_"));
     */
    public static final void setCipher(final String enable, final String key, final String iv, final String mode, final Integer padding, final Map<Class, List<String>> classMap) {
        DBCipherUtil.setCipher(enable, key, iv, mode, padding, classMap);
    }

    /**
     * 2025-06-22 封装到底层
     * 2026-07-18 druid调优
     * @param druidDataSource
     */
    public static final void setDruidDataSource(final DruidDataSource druidDataSource) {
        String driverClassName = MyBatisUtil.getDriverClassName();
        druidDataSource.setDriverClassName(driverClassName);
        if (StringUtil.equalsOfOne(MyBatisUtil.dbType, DB_ORACLE, DB_DM)) {
            druidDataSource.setPoolPreparedStatements(true);  // 打开PSCache，并且指定每个连接上PSCache的大小
            druidDataSource.setMaxPoolPreparedStatementPerConnectionSize(50);  // 每个连接缓存的PS数量，开启后需设此值
        } else {
            druidDataSource.setPoolPreparedStatements(false);  // 打开PSCache，并且指定每个连接上PSCache的大小
        }
        String validationQuery = "SELECT 1";
        if (StringUtil.equalsOfOne(MyBatisUtil.dbType, DB_ORACLE)) {
            validationQuery = "SELECT 1 FROM DUAL";
        }
        druidDataSource.setValidationQuery(validationQuery);
        druidDataSource.setName("druidDataSource-crabapple");
        druidDataSource.setInitialSize(30);  // 初始连接数，避免启动时突发压力。小于等于minIdle
        druidDataSource.setMinIdle(30);  // 最小空闲连接数，防止低负载时连接不足。小于等于maxActive/2
        druidDataSource.setMaxActive(200);  // 最大活跃连接数，过高会导致DB竞争加剧。小于等于DB的max_connections的80%
        druidDataSource.setMaxWait(1000L * 30 * 1);  // 获取连接最大等待时间，避免无限等待导致线程卡死
        druidDataSource.setConnectTimeout(1000 * 60 * 1);  // 单位毫秒
        druidDataSource.setSocketTimeout(1000 * 60 * 1);  // 单位毫秒
        druidDataSource.setMinEvictableIdleTimeMillis(1000L * 60 * 10);  // 连接在池中保持空闲的最小时间，超过此时间可能被回收（需结合检测线程）。单位是毫秒。
        druidDataSource.setMaxEvictableIdleTimeMillis(1000L * 60 * 60 * 5);  // 连接在池中保持空闲的最大时间，超过此时间强制释放。单位是毫秒。
        druidDataSource.setTimeBetweenEvictionRunsMillis(1000L * 30 * 1);  // 空闲连接检测间隔，需低于DB超时时间。单位是毫秒。该值必须小于参数keepAliveBetweenTimeMillis
        druidDataSource.setValidationQueryTimeout(1 * 5 * 1);  // 单次心跳验证超过该时间就直接熔断断开，防探活卡死
        druidDataSource.setKeepAlive(true);  // 启用保活机制。
        druidDataSource.setKeepAliveBetweenTimeMillis(1000L * 60 * 2);  // 多久保活一次。单位毫秒。该值必须大于参数 timeBetweenEvictionRunsMillis
        druidDataSource.setRemoveAbandoned(true);  // 启用泄露回收。
        druidDataSource.setRemoveAbandonedTimeout(1 * 60 * 20);  // 未关闭则回收。单位秒
        druidDataSource.setLogAbandoned(true);
        druidDataSource.setBreakAfterAcquireFailure(false);
        druidDataSource.setTimeBetweenConnectErrorMillis(1000L * 30 * 1);  // 为数据库恢复留出足够的时间间隔，避免请求长时间堆积
        druidDataSource.setConnectionErrorRetryAttempts(2);  // 发生连接错误时重试次数
        druidDataSource.setTestWhileIdle(true);  // 空闲时检测连接有效性，平衡性能与安全。
        druidDataSource.setTestOnBorrow(false);  // 获取连接时检测，影响性能，非高可靠场景关闭。
        druidDataSource.setTestOnReturn(false);  // 归还连接时检测，同样建议关闭。
        druidDataSource.setAsyncInit(true);  // 异步初始化连接池，加速应用启动。
        druidDataSource.setUseUnfairLock(true);  // 使用非公平锁提升并发性能（高并发场景）。
        //
        p_druidDataSource = druidDataSource;
    }

    public static final DruidDataSource getDruidDataSource() {
        return p_druidDataSource;
    }

    /**
     * 2025-01-19
     * 标准SQL语句
     * 统一改为大写SQL语句
     * 统一删除SQL语句中的特殊字符
     *
     * @param boundSql
     * @return
     */
    public static final String setStandardSQL(final BoundSql boundSql) {
        String sql = boundSql.getSql();  // 获取到原始sql语句
        if (sql == null || "".equals(sql)) {
            return null;
        }
        //
        try {
            sql = sql.trim();
            sql = sql.replaceAll("\\s+", " ");
            final StringBuilder bufferSql = new StringBuilder(sql);
            int length = bufferSql.length();
            if (bufferSql.lastIndexOf(";") == length - 1) {
                bufferSql.deleteCharAt(length - 1);
            }
            sql = bufferSql.toString();
            sql = sql.trim();
            sql = sql.toUpperCase();  // 统一改为大写SQL语句
            sql = StringUtil.getPureStr(sql);  // 统一删除SQL语句中的特殊字符
            sql = sql.replaceAll("(?si)\\s+", " ");
            final Class cls = boundSql.getClass();
            final Field field = cls.getDeclaredField("sql");
            field.setAccessible(true);
            field.set(boundSql, sql);
            return sql;
        } catch (Throwable throwable) {
            LogUtil.error("MyBatisUtil_setStandardSQL_t1", throwable, sql);
            return null;
        }
    }

    /**
     * 2025-01-19
     *
     * @param invocation
     * @return
     */
    public static final Object proceed(final Invocation invocation) {
        try {
            final Object proceedObject = invocation.proceed();
            return proceedObject;
        } catch (Throwable throwable) {
            if (throwable instanceof InvocationTargetException) {
                InvocationTargetException invocationTargetException = (InvocationTargetException) throwable;
                Throwable targetException = invocationTargetException.getTargetException();
                if (targetException instanceof SQLSyntaxErrorException) {
                    SQLSyntaxErrorException sqlSyntaxErrorException = (SQLSyntaxErrorException) targetException;
                    LogUtil.error("MyBatisUtil_proceed_t1", sqlSyntaxErrorException);
                    return null;
                }
            }
            LogUtil.error("MyBatisUtil_proceed_t5", throwable);
            return null;
        }
    }

    /**
     * 2025-01-19
     *
     * @param invocation
     * @param queryArgs
     * @param paramMap
     * @param methodName
     * @param standardSQL
     * @return
     */
    public static final Object proceed(final Invocation invocation, final Object[] queryArgs, final Map<String, Object> paramMap, final String methodName, final String standardSQL) {
        try {
            if (CollectionUtil.isBlank(paramMap)) {
                final Object proceedObject = invocation.proceed();
                return proceedObject;
            }
            final Object proceedObject = invocation.proceed();
            return proceedObject;
        } catch (Throwable throwable) {
            if (throwable instanceof InvocationTargetException) {
                InvocationTargetException invocationTargetException = (InvocationTargetException) throwable;
                Throwable targetException = invocationTargetException.getTargetException();
                if (targetException instanceof SQLSyntaxErrorException) {
                    SQLSyntaxErrorException sqlSyntaxErrorException = (SQLSyntaxErrorException) targetException;
                    LogUtil.error("MyBatisUtil_proceed_t11", sqlSyntaxErrorException, paramMap, methodName, standardSQL);
                    return null;
                }
                if (targetException instanceof SQLException) {
                    SQLException sqlException = (SQLException) targetException;
                    LogUtil.error("MyBatisUtil_proceed_t12", sqlException, paramMap, methodName, standardSQL);
                    return null;
                }
            }
            LogUtil.error("MyBatisUtil_proceed_t15", throwable, paramMap, methodName, standardSQL);
            return null;
        }
    }

    /**
     * 2024-07-27
     *
     * @param databaseName
     * @return
     */
    public static final Map<String, Object> getTableInfoMap(final String databaseName, final String dbType) {
        Map<String, Object> databaseMap = getTableInfoMap(databaseName, "*", dbType);
        return databaseMap;
    }

    /**
     * 2024-07-27
     *
     * @param databaseName
     * @param tableNames
     * @return
     */
    public static final Map<String, Object> getTableInfoMap(String databaseName, String tableNames, final String dbType) {
        if (databaseName != null && !databaseName.matches("[a-zA-Z0-9_]+")) {
            LogUtil.error("MyBatisUtil_getTableInfoMap_sec1", "databaseName包含非法字符", databaseName);
            return null;
        }
        // 信创国产化-达梦/人大金仓 使用Oracle兼容系统视图
        if (StringUtil.equalsOfOne(dbType, DB_DM, DB_KINGBASE)) {
            databaseName = StringUtil.trim(databaseName);
            String selectSQL = "SELECT\n" +
                    "    t.TABLE_NAME,\n" +
                    "    cc.COLUMN_NAME as COLUMN_NAME ,\n" +
                    "    col.DATA_TYPE,\n" +
                    "    col.DATA_LENGTH\n" +
                    "FROM\n" +
                    "    (SELECT TABLE_NAME, OWNER\n" +
                    "     FROM ALL_TABLES\n" +
                    "     WHERE OWNER = '" + databaseName + "' ";
            tableNames = StringUtil.deleteAll(tableNames, " ");
            if (StringUtil.isBlank(tableNames)) {
                tableNames = "*";
            }
            if (!"*".equals(tableNames)) {
                String[] tableNameArray = tableNames.split(",");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" AND TABLE_NAME IN ( ");
                for (int i = 0; i < tableNameArray.length; i++) {
                    String tableName = tableNameArray[i];
                    if(tableName != null && !tableName.matches("[a-zA-Z0-9_]+")){
                        LogUtil.error("MyBatisUtil_getTableInfoMap_sec2", "tableName包含非法字符", tableName);
                        return null;
                    }
                    if (i == 0) {
                        stringBuilder.append("'").append(tableName).append("'");
                        continue;
                    }
                    stringBuilder.append(",'").append(tableName).append("'");
                }
                stringBuilder.append(" ) ");
                String str = stringBuilder.toString();
                selectSQL += str;
            }
            selectSQL +=") t\n" +
                    "JOIN\n" +
                    "    ALL_CONSTRAINTS ac\n" +
                    "    ON t.TABLE_NAME = ac.TABLE_NAME\n" +
                    "   AND t.OWNER = ac.OWNER\n" +
                    "   AND ac.CONSTRAINT_TYPE = 'P'\n" +
                    "JOIN\n" +
                    "    ALL_CONS_COLUMNS cc\n" +
                    "    ON ac.CONSTRAINT_NAME = cc.CONSTRAINT_NAME\n" +
                    "   AND ac.OWNER = cc.OWNER\n" +
                    "JOIN\n" +
                    "    ALL_TAB_COLUMNS col\n" +
                    "    ON col.OWNER = cc.OWNER\n" +
                    "   AND col.TABLE_NAME = cc.TABLE_NAME\n" +
                    "   AND col.COLUMN_NAME = cc.COLUMN_NAME\n" +
                    "ORDER BY\n" +
                    "    t.TABLE_NAME, cc.POSITION;";
            final List<Map<String, Object>> allColumnList = jdbcTemplate.queryForList(selectSQL);
            if (CollectionUtil.isBlank(allColumnList)) {
                return null;
            }
            Map<String, Map<String, Object>> tableMap = new HashMap<>();
            for (Map<String, Object> columnMap : allColumnList) {
                String tableName = MapUtil.get(columnMap, "TABLE_NAME");
                String columnName = MapUtil.get(columnMap, "COLUMN_NAME");
                String columnType = MapUtil.get(columnMap, "DATA_TYPE");
                BigDecimal columnLength = MapUtil.get(columnMap, "DATA_LENGTH");
                if (StringUtil.containOf(tableName, "?") || StringUtil.containOf(columnName, "?")) {
                    LogUtil.error("MyBatisUtil_getTableInfoMap_1", "名称不能含有问号", databaseName, tableName, columnName);
                    continue;
                }
                if (RegexUtil.inChineseOfOne(tableName, columnName)) {
                    LogUtil.error("MyBatisUtil_getTableInfoMap_2", "名称不能含有中文", databaseName, tableName, columnName);
                    continue;
                }
                List<Map<String, Object>> columnList = new ArrayList<>();
                columnList.add(columnMap);
                Map<String, Object> tMap = new HashMap<>();
                tMap.put("primaryName", columnName);
                tMap.put("primaryType", columnType);
                tMap.put("tableName", tableName);
                tMap.put("columnList", columnList);
                tableMap.put(tableName, tMap);
            }
            //
            Map<String, Object> databaseMap = new HashMap<>(2);
            databaseMap.put("databaseName", databaseName);
            databaseMap.put("tableMap", tableMap);
            return databaseMap;
        } else {
            databaseName = StringUtil.trim(databaseName);
            String selectSQL = "select * from information_schema.columns where TABLE_SCHEMA = '" + databaseName + "' ";
            tableNames = StringUtil.deleteAll(tableNames, " ");
            if (StringUtil.isBlank(tableNames)) {
                tableNames = "*";
            }
            if (!"*".equals(tableNames)) {
                String[] tableNameArray = tableNames.split(",");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" AND TABLE_NAME IN ( ");
                for (int i = 0; i < tableNameArray.length; i++) {
                    String tableName = tableNameArray[i];
                    if(tableName != null && !tableName.matches("[a-zA-Z0-9_]+")){
                        LogUtil.error("MyBatisUtil_getTableInfoMap_sec3", "tableName包含非法字符", tableName);
                        return null;
                    }
                    if (i == 0) {
                        stringBuilder.append("'").append(tableName).append("'");
                        continue;
                    }
                    stringBuilder.append(",'").append(tableName).append("'");
                }
                stringBuilder.append(" ) ");
                String str = stringBuilder.toString();
                selectSQL += str;
            }
            final List<Map<String, Object>> allColumnList = jdbcTemplate.queryForList(selectSQL);
            if (CollectionUtil.isBlank(allColumnList)) {
                return null;
            }
            Map<String, Map<String, Object>> tableMap = new HashMap<>();
            for (Map<String, Object> columnMap : allColumnList) {
                String tableName = MapUtil.get(columnMap, "TABLE_NAME");
                String columnName = MapUtil.get(columnMap, "COLUMN_NAME");
                String columnType = MapUtil.get(columnMap, "DATA_TYPE");
                String columnKey = MapUtil.get(columnMap, "COLUMN_KEY");
                String primaryName = "";
                if (StringUtil.equals(columnKey, "PRI")) {
                    primaryName = columnName;
                }
                if (StringUtil.containOf(tableName, "?") || StringUtil.containOf(columnName, "?")) {
                    LogUtil.error("MyBatisUtil_getTableInfoMap_1", "名称不能含有问号", databaseName, tableName, columnName);
                    continue;
                }
                if (RegexUtil.inChineseOfOne(tableName, columnName)) {
                    LogUtil.error("MyBatisUtil_getTableInfoMap_2", "名称不能含有中文", databaseName, tableName, columnName);
                    continue;
                }
                if (!tableMap.containsKey(tableName)) {
                    List<Map<String, Object>> columnList = new ArrayList<>();
                    columnList.add(columnMap);
                    Map<String, Object> tMap = new HashMap<>();
                    if (!StringUtil.isBlank(primaryName)) {
                        tMap.put("primaryName", primaryName);
                        tMap.put("primaryType", columnType);
                    }
                    tMap.put("tableName", tableName);
                    tMap.put("columnList", columnList);
                    tableMap.put(tableName, tMap);
                    continue;
                }
                Map<String, Object> tMap = tableMap.get(tableName);
                if (!StringUtil.isBlank(primaryName)) {
                    tMap.put("primaryName", primaryName);
                    tMap.put("primaryType", columnType);
                }
                List<Map<String, Object>> columnList = (List<Map<String, Object>>) tMap.get("columnList");
                columnList.add(columnMap);
            }
            // 删除没有主键的表
            List<String> noPrimaryTableList = new ArrayList<>();
            Set<String> tableSet = tableMap.keySet();
            Iterator<String> tableIterator = tableSet.iterator();
            while (tableIterator.hasNext()) {
                String tableName = tableIterator.next();
                Map<String, Object> tMap = tableMap.get(tableName);
                String primaryName = MapUtil.get(tMap, "primaryName");
                if (StringUtil.isBlank(primaryName)) {
                    LogUtil.error("MyBatisUtil_getTableInfoMap_3", "该表没有主键，将从集合中移除！", databaseName, tableName);
                    noPrimaryTableList.add(tableName);
                }
            }
            for (String tableName : noPrimaryTableList) {
                tableMap.remove(tableName);
            }
            noPrimaryTableList.clear();
            //
            Map<String, Object> databaseMap = new HashMap<>(2);
            databaseMap.put("databaseName", databaseName);
            databaseMap.put("tableMap", tableMap);
            return databaseMap;
        }

    }

    /**
     * 2025-09-18
     * 2026-06-02 信创国产化-达梦 补充MySQL分支兼容
     * 获取指定数据库表列信息
     *
     * @return
     */
    public static final  List<Map<String, Object>>  getColumnInfoMap(String databaseName, String tableName, String dbType) {
        if (StringUtil.isBlankOfOne(databaseName, tableName, dbType)) {
            return null;
        }
        if(databaseName != null && !databaseName.matches("[a-zA-Z0-9_]+")){
            LogUtil.error("MyBatisUtil_getColumnInfoMap_sec1", "databaseName包含非法字符", databaseName);
            return null;
        }
        if(tableName != null && !tableName.matches("[a-zA-Z0-9_]+")){
            LogUtil.error("MyBatisUtil_getColumnInfoMap_sec2", "tableName包含非法字符", tableName);
            return null;
        }
        // 信创国产化-达梦/人大金仓 使用达梦系统视图
        if (StringUtil.equalsOfOne(dbType, DB_DM, DB_KINGBASE)) {
            String getcolumnListSQL = "SELECT " +
                    "    COLUMN_NAME,           " +//列名
                    "    DATA_TYPE,             " +//数据类型
                    "    DATA_LENGTH,           " +//字段长度
                    "    DATA_PRECISION,        " +//数值精度
                    "    DATA_SCALE,            " +//小数位数
                    "    NULLABLE,              " +//是否可为空：Y 可为空，N 不可为空
                    "    COLUMN_ID              " +//在表中的顺序
                    "FROM " +
                    "    ALL_TAB_COLUMNS WHERE  OWNER = '"+databaseName+"'        " +// 模式名
                    "    AND TABLE_NAME = '"+tableName+"' " +// 表名
                    "ORDER BY    COLUMN_ID;";
            List<Map<String, Object>>   columnList =  jdbcTemplate.queryForList(getcolumnListSQL);
            if (CollectionUtil.isBlank(columnList)) {
                return null;
            }
            return columnList;
        }
        // MySQL/SQLServer/DB2 使用information_schema系统视图
        String getcolumnListSQL = "SELECT " +
                "    COLUMN_NAME,           " +//列名
                "    DATA_TYPE,             " +//数据类型
                "    CHARACTER_MAXIMUM_LENGTH AS DATA_LENGTH, " +//字段长度
                "    NUMERIC_PRECISION AS DATA_PRECISION, " +//数值精度
                "    NUMERIC_SCALE AS DATA_SCALE, " +//小数位数
                "    IS_NULLABLE,           " +//是否可为空
                "    ORDINAL_POSITION AS COLUMN_ID " +//在表中的顺序
                "FROM " +
                "    information_schema.columns WHERE TABLE_SCHEMA = '"+databaseName+"' " +
                "    AND TABLE_NAME = '"+tableName+"' " +
                "ORDER BY ORDINAL_POSITION";
        List<Map<String, Object>> columnList = jdbcTemplate.queryForList(getcolumnListSQL);
        if (CollectionUtil.isBlank(columnList)) {
            return null;
        }
        return columnList;
    }
    /**
     * 2024-07-27
     * 2026-06-02 信创国产化-达梦 兼容达梦数据库主键查询
     * 获取指定数据库表的主键列名称
     *
     * @return
     */
    public static final String getPrimaryName(final String tableName) {
        if(tableName != null && !tableName.matches("[a-zA-Z0-9_]+")){
            LogUtil.error("MyBatisUtil_getPrimaryName_sec1", "tableName包含非法字符", tableName);
            return null;
        }
        try {
            // 信创国产化-达梦/人大金仓 使用系统视图查询主键
            if(StringUtil.equalsOfOne(MyBatisUtil.dbType, DB_DM, DB_KINGBASE)){
                String selectSQL = "SELECT" +
                        "    cc.COLUMN_NAME as Column_name," +
                        "    'PRIMARY' as Key_name" +
                        " FROM USER_CONSTRAINTS c" +
                        " JOIN USER_CONS_COLUMNS cc ON c.CONSTRAINT_NAME = cc.CONSTRAINT_NAME" +
                        " WHERE c.CONSTRAINT_TYPE = 'P'" +
                        " AND c.TABLE_NAME = '" + tableName + "'";
                List<Map<String, Object>> selectList = jdbcTemplate.queryForList(selectSQL);
                if (CollectionUtil.isBlank(selectList)) {
                    return null;
                }
                Map<String, Object> map = selectList.get(0);
                String primaryName = MapUtil.get(map, "COLUMN_NAME");
                return primaryName;
            }
            // MySQL/SQLServer/DB2 使用SHOW INDEXES语法
            List<Map<String, Object>> selectList = jdbcTemplate.queryForList("show indexes from " + tableName);
            if (CollectionUtil.isBlank(selectList)) {
                return null;
            }
            for (int i = 0; i < selectList.size(); i++) {
                Map<String, Object> map = selectList.get(i);
                if (CollectionUtil.isBlank(map)) {
                    continue;
                }
                String keyName = MapUtil.get(map, "Key_name");
                if (StringUtil.equals(keyName, "PRIMARY")) {
                    String primaryName = MapUtil.get(map, "Column_name");
                    return primaryName;
                }
            }
            return null;
        } catch (Throwable throwable) {
            LogUtil.error("MyBatisUtil_getPrimaryName_t1", throwable, "", tableName);
            return null;
        }
    }

    /**
     * 2024-07-27
     * 2026-06-02 信创国产化-达梦 兼容达梦数据库无主键表查询
     * 获取没有主键的表集合
     */
    public static final List<String> getTableListNoPrimary(final String databaseName) {
        List<String> tableList = getTableList(databaseName);
        if (CollectionUtil.isBlank(tableList)) {
            return new ArrayList<>();
        }
        //
        List<String> noPrimaryTableList = new ArrayList<>();
        // 按表循环，每个表循环一次
        for (String tableName : tableList) {
            try {
                // 信创国产化-达梦/人大金仓 使用系统视图查询主键
                if(StringUtil.equalsOfOne(MyBatisUtil.dbType, DB_DM, DB_KINGBASE)){
                    if(databaseName != null && !databaseName.matches("[a-zA-Z0-9_]+")){
                        LogUtil.error("MyBatisUtil_getTableListNoPrimary_sec1", "databaseName包含非法字符", databaseName);
                        return null;
                    }
                    if(tableName != null && !tableName.matches("[a-zA-Z0-9_]+")){
                        LogUtil.error("MyBatisUtil_getTableListNoPrimary_sec2", "tableName包含非法字符", tableName);
                        return null;
                    }
                    String selectSQL = "SELECT COUNT(*) CNT FROM USER_CONSTRAINTS" +
                            " WHERE CONSTRAINT_TYPE = 'P'" +
                            " AND TABLE_NAME = '" + tableName + "'";
                    List<Map<String, Object>> rowList = jdbcTemplate.queryForList(selectSQL);
                    if (CollectionUtil.isBlank(rowList)) {
                        noPrimaryTableList.add(tableName);
                        continue;
                    }
                    Map<String, Object> map = rowList.get(0);
                    Object cntObj = MapUtil.get(map, "CNT");
                    if(cntObj == null){
                        noPrimaryTableList.add(tableName);
                        continue;
                    }
                    Long cnt = Long.valueOf(cntObj.toString());
                    if(cnt <= 0){
                        noPrimaryTableList.add(tableName);
                    }
                    continue;
                }
                // MySQL/SQLServer/DB2 使用SHOW INDEXES语法
                List<Map<String, Object>> rowList = jdbcTemplate.queryForList("show indexes from " + tableName);
                if (CollectionUtil.isBlank(rowList)) {
                    noPrimaryTableList.add(tableName);
                    continue;
                }
                boolean exist = false;
                for (int i = 0; i < rowList.size(); i++) {
                    Map<String, Object> map = rowList.get(i);
                    if (CollectionUtil.isBlank(map)) {
                        continue;
                    }
                    String keyName = MapUtil.get(map, "Key_name");
                    if (StringUtil.equals(keyName, "PRIMARY")) {
                        exist = true;
                        break;
                    }
                }
                if (!exist) {
                    noPrimaryTableList.add(tableName);
                }
            } catch (Throwable throwable) {
                LogUtil.error("MyBatisUtil_getNoPrimaryTableList_t1", throwable, tableName);
                return null;
            }
        }
        //
        if (CollectionUtil.isBlank(noPrimaryTableList)) {
            return noPrimaryTableList;
        }
        return noPrimaryTableList;
    }

    /**
     * 2024-07-28
     * 2026-06-02 信创国产化-达梦 兼容达梦数据库表列表查询
     * 获取指定数据库的所有表
     *
     * @return
     */
    public static final List<String> getTableList(final String databaseName) {
        if (StringUtil.isBlank(databaseName)) {
            return null;
        }
        //
        try {
            List<Map<String, Object>> selectList;
            // 信创国产化-达梦/人大金仓 使用系统视图查询表列表
            if(StringUtil.equalsOfOne(MyBatisUtil.dbType, DB_DM, DB_KINGBASE)){
                if(databaseName != null && !databaseName.matches("[a-zA-Z0-9_]+")){
                    LogUtil.error("MyBatisUtil_getTableList_sec1", "databaseName包含非法字符", databaseName);
                    return null;
                }
                String selectSQL = "SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = '" + databaseName + "'";
                selectList = jdbcTemplate.queryForList(selectSQL);
            }else{
                selectList = jdbcTemplate.queryForList("show tables");
            }
            if (CollectionUtil.isBlank(selectList)) {
                return null;
            }
            List<String> tableList = new ArrayList<>();
            for (int i = 0; i < selectList.size(); i++) {
                Map<String, Object> map = selectList.get(i);
                if (CollectionUtil.isBlank(map)) {
                    continue;
                }
                Set<String> set = map.keySet();
                Iterator<String> iterator = set.iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    String value = MapUtil.get(map, key);
                    tableList.add(value);
                    break;
                }
            }
            return tableList;
        } catch (Throwable throwable) {
            LogUtil.error("MyBatisUtil_getTableList_t1", throwable, databaseName);
            return null;
        }
    }

    /**
     * 2024-07-28
     * 获取指定SQL语句的返回值结果集数量
     *
     * @param selectSQL 如 select (*) from user
     * @param alias     查询结果别名
     * @return
     */
    public static final Integer getSelectSize(final String selectSQL, String alias) {
        if (StringUtil.isBlank(alias)) {
            alias = "count(*)";
        }
        List<Map<String, Object>> totalList = jdbcTemplate.queryForList(selectSQL);
        if (CollectionUtil.isBlank(totalList)) {
            return 0;
        }
        Map<String, Object> totalMap = totalList.get(0);
        if (CollectionUtil.isBlank(totalMap)) {
            return 0;
        }
        Long totalLong = MapUtil.get(totalMap, alias);
        if (CollectionUtil.isBlank(totalLong)) {
            return 0;
        }
        Integer size = totalLong.intValue();
        return size;
    }

    /**
     * 2025-01-09
     * 测试数据库连接
     */
    public static final String testConnection() {
        DruidPooledConnection druidPooledConnection = null;
        try {
            if (p_druidDataSource == null) {
                LogUtil.error("MyBatisUtil_testConnection_1", "druidDataSource对象为空");
                return "";
            }
            druidPooledConnection = p_druidDataSource.getConnection();
            final String dbUrl = p_druidDataSource.getUrl();
            LogUtil.print("MyBatisUtil_testConnection_2", "数据库连接成功", dbUrl);
            return dbUrl;
        } catch (Throwable throwable) {
            final String dbUrl = p_druidDataSource.getUrl();
            LogUtil.error("MyBatisUtil_testConnection_t1", throwable, "数据库连接失败", dbUrl);
            return "";
        } finally {
            IOUtil.close(druidPooledConnection);
        }
    }

    /**
     * 2024-07-15
     * 测试数据库是否连通
     *
     * @param jdbcurl
     * @param username
     * @param password
     */
    public static final boolean testConnection(final String driverClassName, String jdbcurl, String username, String password) {
        Connection connection = null;
        try {
            Class.forName(driverClassName);
            connection = DriverManager.getConnection(jdbcurl, username, password);
            return true;
        } catch (Throwable throwable) {
            LogUtil.error("DataSourceUtil_initMysql_t1", throwable, "数据库连接失败", driverClassName, jdbcurl, username, password);
            return false;
        } finally {
            IOUtil.close(connection);
        }
    }

    /**
     * 2025-06-22
     *
     * @return
     */
    public static final String getDriverClassName() {
        final String driverClassName = getDriverClassName(MyBatisUtil.dbType, MyBatisUtil.dbVersion);
        return driverClassName;
    }

    /**
     * 2025-06-22
     *
     * @param dbType
     * @return
     */
    public static final String getDriverClassName(String dbType, String dbVersion) {
        if (DB_MYSQL.equals(dbType)) {
            if ("5".equals(dbVersion)) {
                return "com.mysql.jdbc.Driver";  // mysql5
            }
            return "com.mysql.cj.jdbc.Driver";  // mysql8
        }
        //
        if (DB_ORACLE.equals(dbType)) {
            return "oracle.jdbc.driver.OracleDriver";
        }
        //
        if (DB_SQLSERVER2005.equals(dbType)) {
            return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        }
        //
        if (DB_DB2.equals(dbType)) {
            return "com.ibm.db2.jcc.DB2Driver";
        }
        // 信创国产化-达梦
        if (DB_DM.equals(dbType)) {
            return "dm.jdbc.driver.DmDriver";
        }
        // 信创国产化-人大金仓
        if (DB_KINGBASE.equals(dbType)) {
            return "com.kingbase8.Driver";
        }
        //
        return "com.mysql.cj.jdbc.Driver";  // mysql8
    }

    /**
     * 2023-08-15
     * 组装sql
     *
     * @return 字符串sql
     * @Param mainSql 主体
     * @Param paramBeanList 查询条件
     * @Param orderBySql 排序
     */
    private static String assembleSql(String mainSql, LinkedList<ParamBean> paramBeanList, String orderBySql) {
        if (StringUtil.isBlank(mainSql)) {
            return mainSql;
        }
        StringBuilder sqlBuilder = new StringBuilder(mainSql);
        sqlBuilder.append(" where 1=1");
        //
        if (!CollectionUtil.isBlank(paramBeanList)) {
            for (ParamBean paramBean : paramBeanList) {
                String paramKey = paramBean.getParamKey();
                String paramValue = paramBean.getParamValue();
                Symbol symbol = paramBean.getSymbol();
                if (StringUtil.isBlank(paramKey) || symbol == null || StringUtil.isBlank(paramValue)) {
                    continue;
                }
                sqlBuilder.append(" and " + paramKey + " " + symbol.getType() + " " + paramValue);
            }
        }
        //
        if (!StringUtil.isBlank(orderBySql)) {
            sqlBuilder.append(" " + orderBySql);
        }
        return sqlBuilder.toString();
    }

    /**
     * 2023-08-23
     * 标准sql
     *
     * @return 字符串sql
     * @Param mainSql 主体
     */
    public static final String standardSql(final String mainSql) {
        return assembleSql(mainSql, null, null);
    }

    /**
     * 2023-08-23
     * 标准sql
     *
     * @return 字符串sql
     * @Param mainSql 主体
     * @Param paramBeanList 查询条件
     */
    public static final String standardSql(final String mainSql, final LinkedList<ParamBean> paramBeanList) {
        return assembleSql(mainSql, paramBeanList, null);
    }

    /**
     * 2023-08-23
     * 标准sql
     *
     * @return 字符串sql
     * @Param mainSql 主体
     * @Param orderBySql 排序
     */
    public static final String standardSql(final String mainSql, final String orderBySql) {
        return assembleSql(mainSql, null, orderBySql);
    }

    /**
     * 2023-08-23
     * 标准sql
     *
     * @return 字符串sql
     * @Param mainSql 主体
     * @Param paramBeanList 查询条件
     * @Param orderBySql 排序
     */
    public static final String standardSql(final String mainSql, final LinkedList<ParamBean> paramBeanList, final String orderBySql) {
        return assembleSql(mainSql, paramBeanList, orderBySql);
    }

    /**
     * 2023-08-23
     * 分页sql
     *
     * @return 字符串sql
     * @Param mainSql 主体
     * @Param paramBeanList 查询条件
     * @Param orderBySql 排序
     * @Param pageIndex
     * @Param pageSize
     */
    /**
     * 2026-06-02 信创国产化-达梦/人大金仓 LIMIT语法兼容
     */
    public static final String pageSql(final String mainSql, final LinkedList<ParamBean> paramBeanList, final String orderBySql, Integer pageIndex, Integer pageSize) {
        String assembleStringSql = assembleSql(mainSql, paramBeanList, orderBySql);
        StringBuilder sqlBuilder = new StringBuilder(assembleStringSql);
        if (pageIndex == null || pageIndex <= 0) {
            pageIndex = PaginationUtil.pageIndex;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = PaginationUtil.pageSize;
        }
        // 信创国产化-达梦/人大金仓 使用ANSI SQL标准语法
        // 注意：Oracle不支持LIMIT...OFFSET，Oracle需使用ROWNUM或OFFSET...FETCH
        if(StringUtil.equalsOfOne(MyBatisUtil.dbType, MyBatisUtil.DB_DM, MyBatisUtil.DB_KINGBASE)){
            sqlBuilder.append(" LIMIT " + pageSize + " OFFSET " + pageIndex);
        }else{
            sqlBuilder.append(" LIMIT " + pageIndex + "," + pageSize);
        }
        return sqlBuilder.toString();
    }

    /**
     * 2023-08-23
     * 时间类型转换
     *
     * @return Date
     * @Param localDateTime
     */
    public static final Date localDateTimeToDate(final LocalDateTime localDateTime) {
        Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        return date;
    }

    /**
     * @return Date
     * @Author 2024-09-19 信创国产化-达梦 兼容达梦数据库的timeStamp类型转换
     * 对象转date
     * @Param object
     */
    public static final Date objectToDate(final Object object) {
        if (object instanceof LocalDateTime) {
            Date date = localDateTimeToDate((LocalDateTime) object);
            return date;
        } else if (object instanceof Timestamp) {
            Timestamp timestamp = (Timestamp) object;
            LocalDateTime localDateTime = timestamp.toLocalDateTime();
            Date date = localDateTimeToDate(localDateTime);
            return date;
        } else if (object instanceof Date) {
            Date date = (Date) object;
            return date;
        }
        return null;
    }

    /**
     * 2023-08-19
     * 转换sql
     *
     * @return 字符串sql
     * @Param params
     */
    public static final String convertSql(final Map<String, Object> params) {
        if (CollectionUtil.isBlank(params)) {
            return "";
        }
        Object sqlObject = params.get("sql");
        if (sqlObject == null) {
            return "";
        }
        try {
            String sqlStr = sqlObject.toString();
            while (sqlStr.contains(keywordStart) && sqlStr.contains(keywordEnd)) {
                int indexStart = sqlStr.indexOf(keywordStart);
                int indexEnd = sqlStr.indexOf(keywordEnd, indexStart);
                String keywordOffset = sqlStr.substring(indexStart, indexEnd + 1);
                // keywordOffset -> #{id, jdbcType = VARCHAR}
                // keywordOffset -> #{id}
                int keywordStartIndex = keywordOffset.indexOf(keywordStart);
                int commaKeywordIndex = keywordOffset.indexOf(commaKeyword);
                String keyword = "";
                if (keywordStartIndex != -1 && commaKeywordIndex != -1) {
                    keyword = keywordOffset.substring(keywordStartIndex + keywordStart.length(), commaKeywordIndex).trim();
                } else {
                    keyword = sqlStr.substring(indexStart + keywordStart.length(), indexEnd).trim();
                }
                Object obj = params.get(keyword);
                String replaceStrSql = "";
                if (obj instanceof String) {
                    // 2023-10-27 数据中包含单引号'时进行转义
                    String objStr = ((String) obj).replaceAll("'", "''");
                    // 2026-10-12 MySQL下SQL拼接未转义反斜杠导致表单保存失败
                    // MySQL把字符串里的反斜杠\当转义符，值里带\会破坏拼好的SQL，先替换成\\再拼入；达梦把\当普通字符，不替换
                    if (p_isMysqlDbType()) {
                        objStr = objStr.replace("\\", "\\\\");
                    }
                    replaceStrSql = sqlStr.replace(keywordOffset, "'" + objStr + "'");
                } else if (obj instanceof Date) {
                    String date2str = TimerUtil.time2str((Date) obj);
                    replaceStrSql = sqlStr.replace(keywordOffset, "'" + date2str + "'");
                } else if (obj instanceof Long && keywordOffset.contains("TIMESTAMP")) {
                    Date date = new Date((Long) obj);
                    String date2str = TimerUtil.time2str(date);
                    replaceStrSql = sqlStr.replace(keywordOffset, "'" + date2str + "'");
                } else {
                    replaceStrSql = sqlStr.replace(keywordOffset, "" + obj + "");
                }
                sqlStr = replaceStrSql;
            }
            return sqlStr;
        } catch (Throwable throwable) {
            LogUtil.error("MyBatisUtil_convertSql_t1", throwable, "sql request param is error, please check it");
            return "";
        }
    }

    /**
     * 2026-10-12 MySQL下SQL拼接未转义反斜杠导致表单保存失败
     * 判断当前连接的数据库是不是MySQL：MySQL把字符串里的反斜杠\当转义符，拼SQL前要把值里的\替换成\\；
     * 达梦等数据库把\当普通字符，不能替换（替换了会把\\两个字符原样写进库）。按连接串前缀判定，结果按连接串缓存
     * @return true表示MySQL
     */
    private static boolean p_isMysqlDbType() {
        if (p_druidDataSource == null) {
            return false;
        }
        String url = p_druidDataSource.getUrl();
        if (StringUtil.isBlank(url)) {
            return false;
        }
        if (!url.equals(s_mysqlDbTypeUrl)) {
            if (url.startsWith("jdbc:mysql")) {
                s_mysqlDbType = Boolean.TRUE;
            } else {
                s_mysqlDbType = Boolean.FALSE;
            }
            s_mysqlDbTypeUrl = url;
        }
        return s_mysqlDbType.booleanValue();
    }

    /**
     * @Author 2023-03-28
     * @Param object
     * @Description 信创国产化-达梦 如果当前为达梦数据库并且需要转换NClob为字符串，就通过代理转换，否则直接进行强转
     * @Return String
     */
    public static final String convertNClobToString(final Object object) {
        if (object == null) {
            return "";
        }
        //
        if (object instanceof NClobProxy && DB_DM.equals(MyBatisUtil.dbType)) {
            NClob rawNClob = ((NClobProxy) object).getRawNClob();
            String json = ((DmdbNClob) rawNClob).data;
            return json;
        }
        //
        String json = (String) object;
        return json;
    }

    /**
     * 2025-09-11
     *
     * @param params
     * @param rowBounds
     * @return
     */
    public static final String getLimitString(final Map<String, Object> params, final RowBounds rowBounds) {
        String sql = PaginationUtil.getLimitString(params, rowBounds);
        return sql;
    }

}
