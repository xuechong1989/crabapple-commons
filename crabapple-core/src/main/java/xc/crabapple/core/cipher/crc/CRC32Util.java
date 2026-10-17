package xc.crabapple.core.cipher.crc;

import xc.crabapple.core.log.LogUtil;

import java.util.zip.CRC32;

/**
 * 2017-07-29 1.0.0
 */
public final class CRC32Util{

    /**
     * 2017-07-29 1.0.0
     * @param str
     */
    public static final long getCRC32(final String str){
        CRC32 crc32 = new CRC32();
        long value = -1;
        try{
            byte[] bytes = str.getBytes("UTF-8");
            crc32.update(bytes);
            value = crc32.getValue();
        }catch(Throwable throwable){
            LogUtil.error("CRC32Util_getCRC32_t1", throwable, str);
        }
        return value;
    }

}