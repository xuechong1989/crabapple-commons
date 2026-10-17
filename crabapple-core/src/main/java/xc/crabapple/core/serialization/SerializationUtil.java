package xc.crabapple.core.serialization;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

/**
 * 2023-01-24
 * 序列化对象管理类
 */
public final class SerializationUtil {

    // 标准写法
    // public static final long serialVersionUID = 1L;

    /**
     * 2023-01-25
     * 可序列化对象 ==> 字节数组
     * 解决自定义对象作为参数进行HTTP协议传输
     * 可逆处理方法 bytes2serializeObject
     * @param serializable
     * @return
     */
    public static final byte[] serializeObject2bytes(final Serializable serializable){
        byte[] bytes = SerializationUtils.serialize(serializable);
        return bytes;
    }

    /**
     * 2023-01-25
     * 字节数组字符串 ==> 可序列化对象
     * 解决自定义对象作为参数进行HTTP协议传输
     * 可逆处理方法 serializeObject2bytes
     * @param bytes
     * @return
     */
    public static final Serializable bytes2serializeObject(final byte[] bytes){
        Serializable serializable = SerializationUtils.deserialize(bytes);
        return serializable;
    }

    /**
     * 2023-01-24
     * 可序列化对象 ==> 可网络传输的字节数组字符串
     * 解决自定义对象作为参数进行HTTP协议传输
     * 可逆处理方法 bytesStr2serializeObject
     * @param serializable
     * @return
     */
    public static final String serializeObject2bytesStr(final Serializable serializable){
        // 可序列化对象 转为 可网络传输的字节数组
        byte[] bytes = SerializationUtils.serialize(serializable);
        // 解决原始数据的字节数组在网络传输时元素为负数时数据丢失问题
        String bytesStr = Base64.encodeBase64String(bytes);
        return bytesStr;
    }

    /**
     * 2023-01-24
     * 可网络传输的字节数组字符串 ==> 可序列化对象
     * 解决自定义对象作为参数进行HTTP协议传输
     * 可逆处理方法 serializeObject2bytesStr
     * @param bytesStr
     * @return
     */
    public static final Serializable bytesStr2serializeObject(final String bytesStr){
        byte[] bytes = Base64.decodeBase64(bytesStr);
        Serializable serializable = SerializationUtils.deserialize(bytes);
        return serializable;
    }

}