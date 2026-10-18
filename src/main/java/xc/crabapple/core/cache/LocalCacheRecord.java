package xc.crabapple.core.cache;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32C;

/**
 * 2026-08-20 构建替代redis的本地API持久化缓存框架
 * 日志记录二进制编解码：定长头30字节（统一大端）+ 模块名 + key + value + CRC32C尾部校验
 */
public final class LocalCacheRecord {

    public static final int MAGIC_1 = 0x50;                              // 魔数第1字节 'P'，用于记录定界与部分写入检测
    public static final int MAGIC_2 = 0x43;                              // 魔数第2字节 'C'
    public static final int VERSION = 2;                                 // 记录格式版本（2:value首字节为编码标志；1:纯JDK序列化，仅兼容读取）
    public static final int RECORD_TYPE_PUT = 1;                         // 写入记录
    public static final int RECORD_TYPE_DELETE = 2;                      // 删除记录
    public static final int HEADER_LENGTH = 30;                          // 定长头长度：2魔数+1版本+1类型+8序号+8过期+2模块长+4key长+4value长
    public static final int CRC_LENGTH = 4;                              // 尾部CRC32C校验长度
    public static final int OFFSET_VERSION = 2;                          // 版本字段偏移
    public static final int OFFSET_RECORD_TYPE = 3;                      // 记录类型字段偏移
    public static final int OFFSET_SEQUENCE = 4;                         // 序号字段偏移
    public static final int OFFSET_EXPIRE_AT = 12;                       // 过期时间字段偏移
    public static final int OFFSET_MODULE_LENGTH = 20;                   // 模块名长度字段偏移
    public static final int OFFSET_KEY_LENGTH = 22;                      // key长度字段偏移
    public static final int OFFSET_VALUE_LENGTH = 26;                    // value长度字段偏移
    public static final int MAX_MODULE_LENGTH = 256;                     // 模块名长度上限（UTF-8字节）
    public static final int MAX_KEY_LENGTH = 4096;                       // key长度上限（UTF-8字节）
    public static final int MAX_VALUE_LENGTH = 16 * 1024 * 1024;         // value长度上限（编码后字节）
    public static final int VALUE_CODE_JDK = 1;                          // value编码标志：JDK序列化（通用对象）
    public static final int VALUE_CODE_STRING = 2;                       // value编码标志：String原始UTF-8（免JDK序列化）
    public static final int VALUE_CODE_LONG = 3;                         // value编码标志：Long八字节大端
    public static final int VALUE_CODE_INTEGER = 4;                      // value编码标志：Integer四字节大端
    public static final int VALUE_CODE_BYTES = 5;                        // value编码标志：byte[]原样

    private int recordType;      // 记录类型
    private int dataVersion;     // 记录格式版本（1或2）
    private long sequence;       // 全局单调递增序号
    private long expireAt;       // 过期时间epoch毫秒，0表示永久
    private String moduleName;   // 模块名
    private String key;          // 缓存key
    private byte[] valueBytes;   // value编码字节（V2首字节为编码标志），DELETE记录为空数组

    private LocalCacheRecord(){

    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 编码一条完整日志记录（当前版本格式；完整说明见带版本重载）
     * @param recordType 记录类型，RECORD_TYPE_PUT或RECORD_TYPE_DELETE
     * @param sequence 全局单调递增序号
     * @param expireAt 过期时间epoch毫秒，0表示永久
     * @param moduleName 模块名
     * @param key 缓存key
     * @param valueBytes value序列化字节，DELETE记录传null
     * @return 完整记录字节；模块名或key超长时返回null
     */
    public static final byte[] encode(final int recordType, final long sequence, final long expireAt, final String moduleName, final String key, final byte[] valueBytes){
        byte[] recordBytes = encode(VERSION, recordType, sequence, expireAt, moduleName, key, valueBytes);
        return recordBytes;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 2026-08-29 本地持久化缓存框架问题修复
     * 2026-09-28 本地缓存压缩重写写出非法记录导致重启清空缓存重新登录
     * 按指定记录版本编码（Compaction对V1历史懒条目按V1原样写回，零转换零丢弃）
     * CRC覆盖范围：VERSION至VALUE全部字节（不含魔数与CRC自身）
     * @param version 记录格式版本（1或2）
     * @param recordType 记录类型，RECORD_TYPE_PUT或RECORD_TYPE_DELETE
     * @param sequence 全局单调递增序号
     * @param expireAt 过期时间epoch毫秒，0表示永久
     * @param moduleName 模块名
     * @param key 缓存key
     * @param valueBytes value序列化字节，DELETE记录传null
     * @return 完整记录字节；版本号不是1或2、模块名或key超长时返回null
     */
    public static final byte[] encode(final int version, final int recordType, final long sequence, final long expireAt, final String moduleName, final String key, final byte[] valueBytes){
        // 2026-09-28 本地缓存压缩重写写出非法记录导致重启清空缓存重新登录
        // 版本号校验：不是1或2直接拒绝编码，非法版本号的坏记录不允许写进数据文件（调用方对null记日志跳过该条）
        if(version != 1 && version != 2){
            return null;
        }
        byte[] moduleBytes = moduleName.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if(moduleBytes.length > MAX_MODULE_LENGTH){
            return null;
        }
        if(keyBytes.length > MAX_KEY_LENGTH){
            return null;
        }
        byte[] localValueBytes = valueBytes;
        if(localValueBytes == null){
            localValueBytes = new byte[0];
        }
        // 组装：定长头 + 模块名 + key + value + CRC
        int totalLength = HEADER_LENGTH + moduleBytes.length + keyBytes.length + localValueBytes.length + CRC_LENGTH;
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        buffer.put((byte) MAGIC_1);
        buffer.put((byte) MAGIC_2);
        buffer.put((byte) version);
        buffer.put((byte) recordType);
        buffer.putLong(sequence);
        buffer.putLong(expireAt);
        buffer.putShort((short) moduleBytes.length);
        buffer.putInt(keyBytes.length);
        buffer.putInt(localValueBytes.length);
        buffer.put(moduleBytes);
        buffer.put(keyBytes);
        buffer.put(localValueBytes);
        // CRC32C从VERSION开始计算（跳过2字节魔数），不覆盖CRC自身
        CRC32C crc32c = new CRC32C();
        crc32c.update(buffer.array(), OFFSET_VERSION, buffer.position() - OFFSET_VERSION);
        int crcValue = (int) crc32c.getValue();
        buffer.putInt(crcValue);
        byte[] recordBytes = buffer.array();
        return recordBytes;
    }

    /**
     * 2026-08-20 构建替代redis的本地API持久化缓存框架
     * 解码并校验一条完整记录字节：魔数、版本、CRC32C、长度一致性、类型合法性
     * @param recordBytes 完整记录字节
     * @return 解析结果；任一校验失败返回null（调用方据此截断，禁止静默返回错误数据）
     */
    public static final LocalCacheRecord decode(final byte[] recordBytes){
        if(recordBytes == null){
            return null;
        }
        if(recordBytes.length < HEADER_LENGTH + CRC_LENGTH){
            return null;
        }
        if((recordBytes[0] & 0xFF) != MAGIC_1){
            return null;
        }
        if((recordBytes[1] & 0xFF) != MAGIC_2){
            return null;
        }
        int versionValue = recordBytes[OFFSET_VERSION] & 0xFF;
        if(versionValue != 1 && versionValue != 2){
            return null;
        }
        // 重算CRC32C与尾部存储值比对
        int crcCoverLength = recordBytes.length - CRC_LENGTH - OFFSET_VERSION;
        CRC32C crc32c = new CRC32C();
        crc32c.update(recordBytes, OFFSET_VERSION, crcCoverLength);
        int calcCrcValue = (int) crc32c.getValue();
        ByteBuffer crcBuffer = ByteBuffer.wrap(recordBytes, recordBytes.length - CRC_LENGTH, CRC_LENGTH);
        int storedCrcValue = crcBuffer.getInt();
        if(calcCrcValue != storedCrcValue){
            return null;
        }
        // 解析定长头中的长度字段
        ByteBuffer headerBuffer = ByteBuffer.wrap(recordBytes, 0, HEADER_LENGTH);
        int recordTypeValue = headerBuffer.get(OFFSET_RECORD_TYPE) & 0xFF;
        long sequenceValue = headerBuffer.getLong(OFFSET_SEQUENCE);
        long expireAtValue = headerBuffer.getLong(OFFSET_EXPIRE_AT);
        int moduleLength = headerBuffer.getShort(OFFSET_MODULE_LENGTH) & 0xFFFF;
        int keyLength = headerBuffer.getInt(OFFSET_KEY_LENGTH);
        int valueLength = headerBuffer.getInt(OFFSET_VALUE_LENGTH);
        // 长度合法性：非负且不超过上限，防止伪造长度导致越界与OOM
        if(keyLength < 0 || keyLength > MAX_KEY_LENGTH){
            return null;
        }
        if(valueLength < 0 || valueLength > MAX_VALUE_LENGTH){
            return null;
        }
        if(moduleLength > MAX_MODULE_LENGTH){
            return null;
        }
        // 长度一致性与类型合法性校验
        int expectedLength = HEADER_LENGTH + moduleLength + keyLength + valueLength + CRC_LENGTH;
        if(expectedLength != recordBytes.length){
            return null;
        }
        if(recordTypeValue != RECORD_TYPE_PUT && recordTypeValue != RECORD_TYPE_DELETE){
            return null;
        }
        // 解析模块名、key、value
        LocalCacheRecord record = new LocalCacheRecord();
        record.dataVersion = versionValue;
        record.recordType = recordTypeValue;
        record.sequence = sequenceValue;
        record.expireAt = expireAtValue;
        ByteBuffer bodyBuffer = ByteBuffer.wrap(recordBytes, HEADER_LENGTH, recordBytes.length - HEADER_LENGTH - CRC_LENGTH);
        byte[] moduleBytes = new byte[moduleLength];
        bodyBuffer.get(moduleBytes);
        byte[] keyBytes = new byte[keyLength];
        bodyBuffer.get(keyBytes);
        byte[] localValueBytes = new byte[valueLength];
        bodyBuffer.get(localValueBytes);
        record.moduleName = new String(moduleBytes, StandardCharsets.UTF_8);
        record.key = new String(keyBytes, StandardCharsets.UTF_8);
        record.valueBytes = localValueBytes;
        return record;
    }

    public int getRecordType() {
        int value = this.recordType;
        return value;
    }

    public int getDataVersion() {
        int value = this.dataVersion;
        return value;
    }

    public long getSequence() {
        long value = this.sequence;
        return value;
    }

    public long getExpireAt() {
        long value = this.expireAt;
        return value;
    }

    public String getModuleName() {
        String value = this.moduleName;
        return value;
    }

    public String getKey() {
        String value = this.key;
        return value;
    }

    public byte[] getValueBytes() {
        byte[] value = this.valueBytes;
        return value;
    }

}
