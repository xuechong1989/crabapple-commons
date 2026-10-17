package xc.crabapple.ssm.mybatis.typeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.ObjectTypeHandler;

/**
 * 2024-09-19 信创国产化-达梦 兼容达梦数据库的timeStamp类型转换
 */
public final class TimeStamp2LocalDateTimeTypeHandler extends ObjectTypeHandler {

    @Override
    public void setNonNullParameter(final PreparedStatement ps, final int i, final Object parameter, final JdbcType jdbcType) throws SQLException {
        Timestamp timestamp = (Timestamp) parameter;
        ps.setTimestamp(i, timestamp);
    }

    @Override
    public LocalDateTime getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
        Object object = rs.getObject(columnName);
        if (Objects.isNull(object)) {
            return null;
        }
        if (object instanceof Timestamp) {
            return ((Timestamp) object).toLocalDateTime();
        }
        return rs.getObject(columnName, LocalDateTime.class);
    }

    @Override
    public LocalDateTime getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
        Object object = rs.getObject(columnIndex);
        if (Objects.isNull(object)) {
            return null;
        }
        if (object instanceof Timestamp) {
            return ((Timestamp) object).toLocalDateTime();
        }
        return rs.getObject(columnIndex, LocalDateTime.class);
    }

    @Override
    public LocalDateTime getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
        Object object = cs.getObject(columnIndex);
        if (Objects.isNull(object)) {
            return null;
        }
        if (object instanceof Timestamp) {
            return ((Timestamp) object).toLocalDateTime();
        }
        return cs.getObject(columnIndex, LocalDateTime.class);
    }
}
