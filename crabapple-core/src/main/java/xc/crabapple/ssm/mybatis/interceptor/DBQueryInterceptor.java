package xc.crabapple.ssm.mybatis.interceptor;

import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.ssm.mybatis.MyBatisUtil;
import xc.crabapple.ssm.mybatis.pagination.PaginationUtil;

import java.util.Map;

/**
 * 2022-04-14
 * 2025-01-13 集成指定数据库表字段解密
 * 标准SQL查询拦截器
 */
@Intercepts({
    @Signature(type=Executor.class,
            method="query",
            args={MappedStatement.class,Object.class,RowBounds.class,ResultHandler.class}
    )
})
public final class DBQueryInterceptor implements Interceptor{

    public DBQueryInterceptor(){

    }

    @Override
    public Object intercept(final Invocation invocation) {
        Object[] queryArgs = null;
        MappedStatement mappedStatement = null;
        String methodName = null;
        Map<String, Object> paramMap = null;
        String standardSQL = "";
        try {
            queryArgs = invocation.getArgs();
            mappedStatement = (MappedStatement) queryArgs[0];
            methodName = mappedStatement.getId();
            paramMap = (Map<String, Object>) queryArgs[1];
            final BoundSql boundSql = mappedStatement.getBoundSql(paramMap);
            standardSQL = MyBatisUtil.setStandardSQL(boundSql);
            //
            if(paramMap instanceof MapperMethod.ParamMap){
                Object object = MyBatisUtil.proceed(invocation, queryArgs, paramMap, methodName, standardSQL);
                return object;
            }
            // 物理分页
            final Object object = PaginationUtil.getPageList(invocation, mappedStatement, boundSql, paramMap, queryArgs, methodName, standardSQL);
            return object;
        }catch (Throwable throwable){
            LogUtil.error("DBSelectInterceptor_intercept_t1", throwable, methodName, paramMap, standardSQL);
            return null;
        }
    }

    @Override
    public Object plugin(final Object targetObject){
        final Object pluginObject = Plugin.wrap(targetObject, this);
        return pluginObject;
    }

}