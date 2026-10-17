package xc.crabapple.core.random;

import xc.crabapple.core.log.LogUtil;

/**
 * 2019-07-23 2.3.3
 * Long型主键算法
 * Twitter_Snowflake
 *  SnowFlake的结构如下(每部分用-分开)
 *  0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000
 *  1位标识，由于long基本类型在Java中是带符号的，最高位是符号位，正数是0，负数是1，所以id一般是正数，最高位是0
 *  41位时间截(毫秒级)，注意，41位时间截不是存储当前时间的时间截，而是存储时间截的差值（当前时间截 - 开始时间截)
 *  得到的值），这里的的开始时间截，一般是我们的id生成器开始使用的时间，由我们程序来指定的（如下下面程序IdWorker类的startTime属性）。
 *  41位的时间截，可以使用69年，年T = (1L << 41) / (1000L * 60 * 60 * 24 * 365) = 69
 *  10位的数据机器位，可以部署在1024个节点，包括5位datacenterId和5位workerId
 *  12位序列，毫秒内的计数，12位的计数顺序号支持每个节点每毫秒(同一机器，同一时间截)产生4096个ID序号
 *  加起来刚好64位，为一个Long型。
 *  SnowFlake的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分)，并且效率较高。
 *  经测试，SnowFlake每秒能够产生26万ID左右。
 */
public final class Snowflake {

    private final long workerId;  //取值范围[0-31]
    private final long datacenterId;  //取值范围[0-31]
    private final long idepoch;

    private final long workerIdBits = 5L;
    private final long datacenterIdBits = 5L;
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    private final long sequenceBits = 12L;
    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    private long lastTimestamp = -1L;
    private long sequence;

    public Snowflake(final long workerId, final long datacenterId, final long sequence) {
        this(workerId, datacenterId, sequence, 1344322705519L);
    }

    public Snowflake(final long workerId, final long datacenterId, final long sequence, final long idepoch) {
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.sequence = sequence;
        this.idepoch = idepoch;
        if (workerId < 0 || workerId > maxWorkerId) {
            LogUtil.error("Snowflake_1", "workerId is illegal", workerId);
            return;
        }
        if (datacenterId < 0 || datacenterId > maxDatacenterId) {
            LogUtil.error("Snowflake_2", "datacenterId is illegal", workerId);
        }
    }

    public long getId() {
        long id = nextId();
        return id;
    }

    private long nextId() {
        synchronized(this){
            long timestamp = timeGen();
            if (timestamp < lastTimestamp) {
                LogUtil.error("Snowflake_nextId_1", "Clock moved backwards");
                return 0;
            }
            if (lastTimestamp == timestamp) {
                sequence = (sequence + 1) & sequenceMask;
                if (sequence == 0) {
                    timestamp = tilNextMillis(lastTimestamp);
                }
            } else {
                sequence = 0;
            }
            lastTimestamp = timestamp;
            long id = ((timestamp - idepoch) << timestampLeftShift)
                    | (datacenterId << datacenterIdShift)
                    | (workerId << workerIdShift)
                    | sequence;
            return id;
        }
    }

    private long tilNextMillis(final long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    public long getDatacenterId() {
        return datacenterId;
    }

    public long getWorkerId() {
        return workerId;
    }

    public long getTime() {
        return System.currentTimeMillis();
    }

}
