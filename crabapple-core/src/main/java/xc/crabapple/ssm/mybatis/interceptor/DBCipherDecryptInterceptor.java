package xc.crabapple.ssm.mybatis.interceptor;

import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import java.sql.Statement;
import java.util.Properties;

/**
 * 2025-01-13
 * 自定义mybatis插件,解密结果集
 */
@Intercepts({
    @Signature(type=ResultSetHandler.class,
            method="handleResultSets",
            args={Statement.class}
    )
})
public final class DBCipherDecryptInterceptor implements Interceptor {

    @Override
    public Object intercept(final Invocation invocation) {
        final Object plaintextObject = DBCipherUtil.decrypt(invocation);
        return plaintextObject;
    }

    @Override
    public Object plugin(final Object object) {
        final Object pluginObject = Plugin.wrap(object, this);
        return pluginObject;
    }

    @Override
    public void setProperties(final Properties properties) {

    }

}