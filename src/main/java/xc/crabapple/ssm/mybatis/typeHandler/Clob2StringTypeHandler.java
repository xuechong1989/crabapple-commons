package xc.crabapple.ssm.mybatis.typeHandler;


import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.ClobTypeHandler;

/**
 * 2024-09-19 信创国产化-达梦 兼容达梦数据库的text类型转换
 */
public final class Clob2StringTypeHandler extends ClobTypeHandler {

    @Override
    public String getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
        String string = rs.getObject(columnName, String.class);
        return string;
    }

    @Override
    public String getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
        String string = rs.getObject(columnIndex, String.class);
        return string;
    }

    @Override
    public String getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
        String string = cs.getObject(columnIndex, String.class);
        return string;
    }
}
