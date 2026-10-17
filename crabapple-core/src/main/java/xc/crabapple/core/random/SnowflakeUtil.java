package xc.crabapple.core.random;

import xc.crabapple.core.collection.StringUtil;

/**
 * 2019-07-23 2.3.3
 * 前身是RandomLong18 2017-07-20 1.0.0
 */
public final class SnowflakeUtil {

    private static Snowflake snowflake;
    private static Long workerId = 0L;  //取值范围[0-31]
    private static Long datacenterId = 0L;  //取值范围[0-31]

    public static final void init(){
        //
        init("0_0");
    }

    /**
     * 2019-11-05 3.0.1
     * @param dbpk 项目的工作id和数据中心id的组合，一般该参数在具体项目的配置文件中配置，格式如1_1, 取值范围为[0-31]_[0-31]
     */
    public static final void init(final String dbpk){
        String[] array = StringUtil.split(dbpk, "_");
        String workerIdTmp = array[0];
        workerId = Long.valueOf(workerIdTmp);
        String datacenterIdTmp = array[1];
        datacenterId = Long.valueOf(datacenterIdTmp);
        p_init(workerId, datacenterId, 0L);
    }

    /**
     * 2019-07-22 2.3.3
     * 初始化数据库主键生成器，基于snowflake
     * @param workerId
     * @param datacenterId
     * @param sequence
     */
    private static final void p_init(final long workerId, final long datacenterId, final long sequence){
        if(snowflake == null){
            snowflake = new Snowflake(workerId, datacenterId, sequence);
        }
    }

    /**
     * 2019-07-23 2.3.3
     * 数据库主键生成器，基于snowflake
     * 执行此方法前务必要首先执行init方法
     */
    public static final Long getLong(){
        if(snowflake == null){
            init("0_0");
        }
        long id = snowflake.getId();
        return id;
    }

    /**
     * 2022-07-07
     */
    public static final String getStr(){
        Long id = getLong();
        String str = id + "";
        return str;
    }

}
