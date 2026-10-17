package xc.crabapple.ssm.mybatis.interceptor;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.data.dao.DaoBase;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.timer.TimerUtil;
import xc.crabapple.ssm.mybatis.MyBatisUtil;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 2025-09-28
 * 打印完整的SQL语句到日志文件中
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare",
                args = {Connection.class, Integer.class})
})
public class DBPrepareInterceptor implements Interceptor {

    @Override
    public Object intercept(final Invocation invocation) throws Throwable {
        Object proceed = invocation.proceed();
        try{
            if (!LogUtil.isEnable(LogUtil.INFO)) {
                return proceed;
            }
            //
            Object target = invocation.getTarget();
            if (target == null) {
                return proceed;
            }
            if (!(target instanceof StatementHandler)) {
                return proceed;
            }
            //
            String methodName = "未找到SQL语句的ID值";
            StatementHandler statementHandler = (StatementHandler) target;
            MappedStatement mappedStatement = getMappedStatement(statementHandler);
            if(mappedStatement != null){
                methodName = mappedStatement.getId();
            }
            //
            BoundSql boundSql = statementHandler.getBoundSql();
            String standardSQL = MyBatisUtil.setStandardSQL(boundSql);
            Object parameterObject = boundSql.getParameterObject();
            if (parameterObject == null) {
                LogUtil.info("DBPrepareInterceptor_intercept_1", "【完整标准SQL语句】", methodName, standardSQL);
                return proceed;
            }
            //
            List<ParameterMapping> parameterMappingList = boundSql.getParameterMappings();
            Configuration configuration = p_getConfiguration(statementHandler);
            if (!CollectionUtil.isBlank(parameterMappingList)) {
                standardSQL = processComplexParameters(standardSQL, parameterObject, parameterMappingList, configuration);
            }
            LogUtil.info("DBPrepareInterceptor_intercept_2", "【完整标准SQL语句】", methodName, standardSQL);
            return proceed;
        }catch (Throwable throwable){
            return proceed;
        }
    }

    /**
     * 获取MappedStatement对象（安全方式）
     */
    private MappedStatement getMappedStatement(final StatementHandler statementHandler) {
        // 1. 首先尝试使用MyBatis的MetaObject（最安全）
        try {
            MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
            if(metaObject != null){
                Object object = metaObject.getValue("delegate.mappedStatement");
                if(object instanceof MappedStatement){
                    MappedStatement mappedStatement = (MappedStatement) object;
                    return mappedStatement;
                }
            }
        } catch (Throwable throwable) {

        }
        //
        try{
            // 2. 处理RoutingStatementHandler的情况
            Object delegate = statementHandler;
            if (statementHandler.getClass().getName().contains("RoutingStatementHandler")) {
                Field delegateField = statementHandler.getClass().getDeclaredField("delegate");
                delegateField.setAccessible(true);
                delegate = delegateField.get(statementHandler);
            }
            // 3. 从真实的StatementHandler中获取mappedStatement
            Field mappedStatementField = delegate.getClass().getDeclaredField("mappedStatement");
            mappedStatementField.setAccessible(true);
            Object object = mappedStatementField.get(delegate);
            if(object instanceof MappedStatement){
                MappedStatement mappedStatement = (MappedStatement) object;
                return mappedStatement;
            }
            return null;
        }catch (Throwable throwable){
            return null;
        }
    }

    /**
     * 处理复杂参数场景
     */
    private String processComplexParameters(final String standardSQL, final Object parameterObject, final List<ParameterMapping> parameterMappingList, final Configuration configuration) {
        // 根据参数类型采用不同的处理策略
        if (parameterObject instanceof DaoBase) {
            String result = p_handleDaoParameters(standardSQL, parameterObject, parameterMappingList, configuration);
            return result;
        }
        //
        if (parameterObject instanceof Map) {
            Map<String, Object> parameterMap = (Map<String, Object>) parameterObject;
            String result = p_handleParameters(standardSQL, parameterMap, parameterMappingList, configuration);
            return result;
        }
        //
        String result = p_handleSimpleParameters(standardSQL, parameterObject, parameterMappingList);
        return result;
    }

    /**
     * 处理自定义对象
     */
    private String p_handleDaoParameters(final String standardSQL, final Object parameterObject, final List<ParameterMapping> parameterMappingList, final Configuration configuration) {
        List<Object> paramValueList = new ArrayList<>();
        for (ParameterMapping mapping : parameterMappingList) {
            String propertyName = mapping.getProperty();
            Object value = p_getPropertyValue(parameterObject, propertyName, configuration);
            paramValueList.add(value);
        }
        String result = p_manualReplaceQuestionMarks(standardSQL, paramValueList);
        return result;
    }

    /**
     * 处理Map参数（包含多个自定义对象）
     */
    private String p_handleParameters(final String standardSQL, final Map<String, Object> paramMap, final List<ParameterMapping> parameterMappingList, final Configuration configuration) {
        if (CollectionUtil.isBlank(parameterMappingList)) {
            return standardSQL;
        }
        List<Object> paramValueList = new ArrayList<>();
        for (ParameterMapping parameterMapping : parameterMappingList) {
            String propertyName = parameterMapping.getProperty();
            Object value = p_getValueFromMap(paramMap, propertyName, configuration);
            paramValueList.add(value);
        }
        String result = p_manualReplaceQuestionMarks(standardSQL, paramValueList);
        return result;
    }

    /**
     * 从Map中获取值（支持嵌套对象）
     */
    private Object p_getValueFromMap(final Map<String, Object> paramMap, final String propertyName, final Configuration configuration) {
        Object value = paramMap.get(propertyName);
        if (value != null) {
            return value;
        }
        // 处理嵌套属性（如user.name）
        if (propertyName.contains(".")) {
            String[] parts = propertyName.split("\\.");
            Object currentObject = paramMap.get(parts[0]);
            if (currentObject != null) {
                try {
                    for (int i = 1; i < parts.length; i++) {
                        currentObject = p_getPropertyValue(currentObject, parts[i], configuration);
                        if (currentObject == null) {
                            break;
                        }
                    }
                    return currentObject;
                } catch (Throwable throwable) {
                    // 忽略错误
                }
            }
        }
        // 处理MyBatis的param1, param2等
        if (propertyName.startsWith("param")) {
            try {
                int paramIndex = Integer.parseInt(propertyName.substring(4));
                return paramMap.get("param" + paramIndex);
            } catch (Throwable throwable) {
                // 忽略
            }
        }
        // 最后尝试遍历所有值
        for (Object mapValue : paramMap.values()) {
            if (mapValue != null) {
                return mapValue;
            }
        }
        return null;
    }

    /**
     * 获取属性值（支持多种方式）
     */
    private Object p_getPropertyValue(final Object obj, final String propertyName, final Configuration configuration) {
        if (obj == null) {
            return null;
        }
        // 1. 使用MyBatis MetaObject（首选）
        try {
            MetaObject metaObject = configuration.newMetaObject(obj);
            Object propertyValue = metaObject.getValue(propertyName);
            return propertyValue;
        } catch (Throwable throwable) {
            // 继续尝试其他方式
        }
        // 2. 使用Java Bean规范
        try {
            String getterName = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
            java.lang.reflect.Method getter = obj.getClass().getMethod(getterName);
            Object propertyValue = getter.invoke(obj);
            return propertyValue;
        } catch (Throwable throwable) {
            // 继续尝试
        }
        // 3. 布尔类型getter
        if (propertyName.startsWith("is")) {
            try {
                java.lang.reflect.Method getter = obj.getClass().getMethod(propertyName);
                return getter.invoke(obj);
            } catch (Throwable throwable) {
                // 忽略
            }
        }
        // 4. 直接字段访问
        try {
            Field field = obj.getClass().getDeclaredField(propertyName);
            field.setAccessible(true);
            Object propertyValue = field.get(obj);
            return propertyValue;
        } catch (Throwable throwable) {
            // 忽略其他错误
        }
        // 5. 查找父类字段
        try {
            Class<?> superClass = obj.getClass().getSuperclass();
            while (superClass != null && superClass != Object.class) {
                try {
                    Field field = superClass.getDeclaredField(propertyName);
                    field.setAccessible(true);
                    return field.get(obj);
                } catch (Throwable throwable) {
                    superClass = superClass.getSuperclass();
                }
            }
        } catch (Throwable throwable) {
            // 忽略
        }
        // 6. 最后手段：返回对象本身
        return obj;
    }

    /**
     * 手动替换问号
     */
    private String p_manualReplaceQuestionMarks(final String standardSQL, final List<Object> paramValueList) {
        if(StringUtil.isBlank(standardSQL)){
            return standardSQL;
        }
        //
        StringBuilder stringBuilder = new StringBuilder();
        int paramIndex = 0;
        for (int i = 0; i < standardSQL.length(); i++) {
            char chr = standardSQL.charAt(i);
            if (chr == '?' && paramIndex < paramValueList.size()) {
                Object propertyValue = paramValueList.get(paramIndex);
                String propertyStr = p_formatParameterValue(propertyValue);
                stringBuilder.append(propertyStr);
                paramIndex++;
            } else {
                stringBuilder.append(chr);
            }
        }
        String result = stringBuilder.toString();
        return result;
    }

    private String p_formatParameterValue(final Object value) {
        if (value == null){
            return "NULL";
        }
        if (value instanceof Number){
            return value.toString();
        }
        if (value instanceof Boolean){
            return (Boolean) value ? "1" : "0";
        }
        if (value instanceof Date) {
            Date date = (Date) value;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(TimerUtil.FORMATTYPE_M1);
            String format = simpleDateFormat.format(date);
            return "'" + format + "'";
        }
        String valueStr = value.toString();
        valueStr = valueStr.replace("'", "''");
        valueStr = "'" + valueStr + "'";
        return valueStr;
    }

    private Configuration p_getConfiguration(final StatementHandler statementHandler) {
        try {
            MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
            MappedStatement ms = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
            return ms.getConfiguration();
        } catch (Throwable throwable) {
            LogUtil.error("DBPrepareInterceptor_p_getConfiguration_t1", throwable);
            return new Configuration();
        }
    }

    /**
     * 处理简单参数（基本类型、字符串、数组等）
     */
    private String p_handleSimpleParameters(final String standardSQL, final Object parameterObject, final List<ParameterMapping> parameterMappings) {
        List<Object> paramValueList = new ArrayList<>();
        if (parameterObject == null) {
            // 如果参数为null，所有参数都设为null
            for (int i = 0; i < parameterMappings.size(); i++) {
                paramValueList.add(null);
            }
        } else if (parameterObject.getClass().isArray()) {
            // 数组类型参数
            p_handleArrayParameters(paramValueList, parameterObject);
        } else if (parameterObject instanceof Collection) {
            // 集合类型参数
            p_handleCollectionParameters(paramValueList, (Collection<?>) parameterObject);
        } else if (p_isBasicType(parameterObject)) {
            // 基本类型参数
            p_handleBasicTypeParameters(paramValueList, parameterObject, parameterMappings);
        } else {
            // 其他类型，降级处理
            p_handleFallbackParameters(paramValueList, parameterObject, parameterMappings);
        }
        String result = p_manualReplaceQuestionMarks(standardSQL, paramValueList);
        return result;
    }

    /**
     * 处理数组类型参数
     */
    private void p_handleArrayParameters(final List<Object> paramValueList, final Object array) {
        if (array instanceof Object[]) {
            Collections.addAll(paramValueList, (Object[]) array);
            return;
        }
        //
        if (array instanceof int[]) {
            for (int value : (int[]) array) {
                paramValueList.add(value);
            }
            return;
        }
        //
        if (array instanceof long[]) {
            for (long value : (long[]) array) {
                paramValueList.add(value);
            }
            return;
        }
        //
        if (array instanceof double[]) {
            for (double value : (double[]) array) {
                paramValueList.add(value);
            }
            return;
        }
        //
        if (array instanceof boolean[]) {
            for (boolean value : (boolean[]) array) {
                paramValueList.add(value);
            }
            return;
        }
        //
        if (array instanceof char[]) {
            for (char value : (char[]) array) {
                paramValueList.add(value);
            }
            return;
        }
        //
        if (array instanceof byte[]) {
            for (byte value : (byte[]) array) {
                paramValueList.add(value);
            }
            return;
        }
        //
        if (array instanceof float[]) {
            for (float value : (float[]) array) {
                paramValueList.add(value);
            }
            return;
        }
        //
        if (array instanceof short[]) {
            for (short value : (short[]) array) {
                paramValueList.add(value);
            }
            return;
        }
    }

    /**
     * 处理集合类型参数
     */
    private void p_handleCollectionParameters(final List<Object> paramValues, final Collection<?> collection) {
        paramValues.addAll(collection);
    }

    /**
     * 处理基本类型参数
     */
    private void p_handleBasicTypeParameters(final List<Object> paramValues, final Object parameterObject, final List<ParameterMapping> parameterMappings) {
        if(parameterMappings == null){
            return;
        }
        // 对于基本类型，所有参数映射都使用同一个值
        for (int i = 0; i < parameterMappings.size(); i++) {
            paramValues.add(parameterObject);
        }
    }

    /**
     * 降级处理参数
     */
    private void p_handleFallbackParameters(final List<Object> paramValues, final Object parameterObject, final List<ParameterMapping> parameterMappings) {
        if(parameterMappings == null){
            return;
        }
        // 对于未知类型，尝试转换为合适的值
        for (int i = 0; i < parameterMappings.size(); i++) {
            paramValues.add(parameterObject);
        }
    }

    /**
     * 判断是否为基本类型
     */
    private boolean p_isBasicType(final Object obj) {
        if (obj == null) {
            return false;
        }
        Class<?> clazz = obj.getClass();
        if (clazz.isPrimitive() || obj instanceof String || obj instanceof Number || obj instanceof Boolean || obj instanceof Character || obj instanceof Date) {
            return true;
        }
        return false;
    }

    @Override
    public Object plugin(final Object target) {
        Object object = Plugin.wrap(target, this);
        return object;
    }

    @Override
    public void setProperties(final Properties properties) {

    }

}