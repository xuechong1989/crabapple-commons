package xc.crabapple.core.data.dao;

import xc.crabapple.core.data.CrabappleData;
import xc.crabapple.core.random.SnowflakeUtil;

/**
 * 2019-11-06 1.0.0
 * 2024-12-26 统一集成
 * Data Access Object - DAO - 数据访问对象
 * 数据库单表与java对象映射关系
 */
public class DaoBase extends CrabappleData {

    private Long primaryKey;

    public DaoBase(){

    }

    public DaoBase(final boolean initial){
        if(initial){
            primaryKey = SnowflakeUtil.getLong();
        }
    }

    public Long getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(final Long primaryKey) {
        this.primaryKey = primaryKey;
    }

}