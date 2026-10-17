package xc.crabapple.ssm.mybatis;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.ssm.mybatis.interceptor.DBCipherDecryptInterceptor;
import xc.crabapple.ssm.mybatis.interceptor.DBCipherEncryptInterceptor;
import xc.crabapple.ssm.mybatis.interceptor.DBPrepareInterceptor;
import xc.crabapple.ssm.mybatis.interceptor.DBQueryInterceptor;
import xc.crabapple.ssm.mybatis.typeHandler.Clob2StringTypeHandler;
import xc.crabapple.ssm.mybatis.typeHandler.TimeStamp2LocalDateTimeTypeHandler;

import javax.sql.DataSource;
import java.sql.Clob;
import java.sql.Timestamp;

/**
 * 2022-03-16 override
 */
public final class MybatisSqlSessionFactoryBean extends SqlSessionFactoryBean {

    private String mybatisXmlUrl;
    private SqlSessionFactory sqlSessionFactory = null;

    public MybatisSqlSessionFactoryBean(){

    }

    /**
     * 框架会自动调用，无需手动调用
     */
    @Override
    protected SqlSessionFactory buildSqlSessionFactory(){
        if(sqlSessionFactory != null){
            return sqlSessionFactory;
        }
        try{
            sqlSessionFactory = super.buildSqlSessionFactory();
            Configuration configuration = sqlSessionFactory.getConfiguration();
            configuration.setCallSettersOnNulls(true);
            configuration.setJdbcTypeForNull(JdbcType.NULL);
            //
            DBPrepareInterceptor dbPrepareInterceptor = new DBPrepareInterceptor();
            configuration.addInterceptor(dbPrepareInterceptor);
            DBQueryInterceptor dbQueryInterceptor = new DBQueryInterceptor();
            configuration.addInterceptor(dbQueryInterceptor);
            DBCipherEncryptInterceptor dbCipherEncryptInterceptor = new DBCipherEncryptInterceptor();
            configuration.addInterceptor(dbCipherEncryptInterceptor);
            DBCipherDecryptInterceptor dbCipherDecryptInterceptor = new DBCipherDecryptInterceptor();
            configuration.addInterceptor(dbCipherDecryptInterceptor);
            //
            // 2024-09-19 这个可以统一处理mybatis中返回map类型的value的数据类型，对于直接使用sqlSessionTemplate执行的sql无效
            // 以下注册register方法传参生成数据结构TypeHandlerRegistry.Map<Type, Map<JdbcType, TypeHandler<?>>> typeHandlerMap的解释：
            // 第一个对应的是Type
            // 第二个对应的是JdbcType
            // 第三个对应的是TypeHandler
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            // 2024-09-19 信创国产化-达梦 兼容达梦数据库的timeStamp类型转换 数据库timeStamp转为localDateTime
            typeHandlerRegistry.register(Timestamp.class, JdbcType.TIMESTAMP, TimeStamp2LocalDateTimeTypeHandler.class);
            typeHandlerRegistry.register(Timestamp.class, null, TimeStamp2LocalDateTimeTypeHandler.class);
            // 2024-09-19 信创国产化-达梦 兼容达梦数据库的text类型转换 数据库中是text在java代码中是Clob将clob转化为string
            typeHandlerRegistry.register(Clob.class, JdbcType.CLOB, Clob2StringTypeHandler.class);
            typeHandlerRegistry.register(Clob.class, null, Clob2StringTypeHandler.class);
            // 2023-04-07 根据配置设置SqlSessionFactoryBean的databaseId类型,如果获取类型为空，打印error日志
            if(StringUtil.isBlank(MyBatisUtil.dbType)){
                LogUtil.error("MybatisSqlSessionFactoryBean_buildSqlSessionFactory_1", "db.type IS NULL");
            }
            configuration.setDatabaseId(MyBatisUtil.dbType);
            String[] mappingLocationArray = {"classpath*:" + mybatisXmlUrl};
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            //遍历解析
            for(int i = 0; i < mappingLocationArray.length; i++){
                final String mappingLocation = mappingLocationArray[i];
                if(mappingLocation == null){
                    continue;
                }
                try{
                    Resource[] mappingAllLocations = resolver.getResources(mappingLocation);
                    p_loadMapping(configuration, mappingAllLocations);
                }catch(Throwable throwable){
                    LogUtil.error("MyBatisSessionFactoryBean_buildSqlSessionFactory_e2", throwable);
                }
            }
            SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();
            sqlSessionFactory = sqlSessionFactoryBuilder.build(configuration);
            return sqlSessionFactory;
        }catch(Throwable throwable){
            LogUtil.error("MybatisSqlSessionFactoryBean_buildSqlSessionFactory_t1", throwable, MyBatisUtil.dbType, mybatisXmlUrl);
            return null;
        }
    }

    private void p_loadMapping(final Configuration configuration, final Resource[] mappingAllLocations){
        if(CollectionUtil.isBlank(mappingAllLocations)){
            return;
        }
        //
        for (Resource mapperLocation : mappingAllLocations){
            if (mapperLocation == null){
                continue;
            }
            try {
                XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(
                        mapperLocation.getInputStream(),
                        configuration,
                        mapperLocation.toString(),
                        configuration.getSqlFragments());
                xmlMapperBuilder.parse();
            }catch(Throwable throwable){
                LogUtil.error("MyBatisSessionFactoryBean_p_loadMapping_t1", throwable, "解析错误", mapperLocation, MyBatisUtil.dbType, mybatisXmlUrl);
            } finally {
                ErrorContext errorContext = ErrorContext.instance();
                errorContext.reset();
            }
        }
    }

    public SqlSessionFactory getSqlSessionFactory() {
        if(sqlSessionFactory == null){
            buildSqlSessionFactory();
        }
        return sqlSessionFactory;
    }

    @Override
    public void setDataSource(final DataSource dataSource) {
        super.setDataSource(dataSource);
    }

    public String getMybatisXmlUrl() {
        return mybatisXmlUrl;
    }

    public void setMybatisXmlUrl(final String mybatisXmlUrl) {
        this.mybatisXmlUrl = mybatisXmlUrl;
    }

}
