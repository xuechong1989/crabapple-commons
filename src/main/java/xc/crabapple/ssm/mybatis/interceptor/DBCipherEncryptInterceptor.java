package xc.crabapple.ssm.mybatis.interceptor;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import java.sql.PreparedStatement;
import java.util.Properties;

/**
 * 2025-01-13
 * 自定义mybatis插件,加密请求参数
 */
@Intercepts({
    @Signature(type=ParameterHandler.class,
            method="setParameters",
            args=PreparedStatement.class
    )
})
public final class DBCipherEncryptInterceptor implements Interceptor {

    @Override
    public Object intercept(final Invocation invocation) {
        final Object proceedObject = DBCipherUtil.encrypt(invocation);
        return proceedObject;
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