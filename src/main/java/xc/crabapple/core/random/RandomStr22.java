package xc.crabapple.core.random;

import java.util.UUID;

/**
 * 2017-07-19 1.0.0
 * 生成22位字符串
 */
public final class RandomStr22{

    /**
     * 2017-07-19 1.0.0
     * Base64编码
     */
    private static final char[] encode(final byte[] data){
        // 采用URL Base64字符，即把"+/"换成"-_"
        final char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_=".toCharArray();
        char[] out = new char[((data.length + 2) / 3) * 4];
        boolean quad, trip;
        for(int i = 0, index = 0; i < data.length; i += 3, index += 4){
            quad = trip = false;
            int val = (0xFF & (int)data[i]);
            val <<= 8;
            if((i + 1) < data.length){
                val |= (0xFF & (int)data[i + 1]);
                trip = true;
            }
            val <<= 8;
            if((i + 2) < data.length){
                val |= (0xFF & (int)data[i + 2]);
                quad = true;
            }
            out[index + 3] = alphabet[(quad ? (val & 0x3F) : 64)];
            val >>= 6;
            out[index + 2] = alphabet[(trip ? (val & 0x3F) : 64)];
            val >>= 6;
            out[index + 1] = alphabet[val & 0x3F];
            val >>= 6;
            out[index + 0] = alphabet[val & 0x3F];
        }
        return out;
    }

    /**
     * 2017-07-19 1.0.0
     * 转成字节
     */
    private static final byte[] toBytes(){
        UUID uuid = UUID.randomUUID();
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] buffer = new byte[16];
        for(int i = 0; i < 8; i++){
            buffer[i] = (byte)((msb >>> 8 * (7 - i)) & 0xFF);
            buffer[i + 8] = (byte)((lsb >>> 8 * (7 - i)) & 0xFF);
        }
        return buffer;
    }

    /**
     * 2017-07-19 1.0.0
     */
    public static final String getUUID(){
        char[] res = encode(toBytes());
        String str = new String(res, 0, res.length - 2);
        return str;
    }

    /**
     * 2017-07-19 1.0.0
     * 将随机UUID转换成22位字符串
     */
    public static final String getStr(){
        // 采用URL Base64字符，即把"+/"换成"-_"
        final char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_=".toCharArray();
        //
        UUID uuid = UUID.randomUUID();
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        char[] out = new char[24];
        int tmp = 0, idx = 0;
        // 基础写法
        /*tmp = (int) ((msb >>> 40) & 0xffffff);
        out[idx + 3] = alphabet[tmp & 0x3f];
        tmp >>= 6;
        out[idx + 2] = alphabet[tmp & 0x3f];
        tmp >>= 6;
        out[idx + 1] = alphabet[tmp & 0x3f];
        tmp >>= 6;
        out[idx] = alphabet[tmp & 0x3f];
        idx += 4;
        tmp = (int) ((msb >>> 16) & 0xffffff);
        out[idx + 3] = alphabet[tmp & 0x3f];
        tmp >>= 6;
        out[idx + 2] = alphabet[tmp & 0x3f];
        tmp >>= 6;
        out[idx + 1] = alphabet[tmp & 0x3f];
        tmp >>= 6;
        out[idx] = alphabet[tmp & 0x3f];
        idx += 4;
        tmp = (int) (((msb & 0xffff) << 8) | (lsb >>> 56 & 0xff));
        out[idx + 3] = alphabet[tmp & 0x3f];
        tmp >>= 6;
        out[idx + 2] = alphabet[tmp & 0x3f];
        tmp >>= 6;
        out[idx + 1] = alphabet[tmp & 0x3f];
        tmp >>= 6;
        out[idx] = alphabet[tmp & 0x3f];
        idx += 4;
        tmp = (int) ((lsb >>> 32) & 0xffffff);
        out[idx + 3] = alphabet[tmp & 0x3f];
        tmp >>= 6;
        out[idx + 2] = alphabet[tmp & 0x3f];
        tmp >>= 6;
        out[idx + 1] = alphabet[tmp & 0x3f];
        tmp >>= 6;
        out[idx] = alphabet[tmp & 0x3f];
        idx += 4;
        tmp = (int) ((lsb >>> 8) & 0xffffff);
        out[idx + 3] = alphabet[tmp & 0x3f];
        tmp >>= 6;
        out[idx + 2] = alphabet[tmp & 0x3f];
        tmp >>= 6;
        out[idx + 1] = alphabet[tmp & 0x3f];
        tmp >>= 6;
        out[idx] = alphabet[tmp & 0x3f];
        idx += 4;
        tmp = (int) (lsb & 0xff);
        out[idx + 3] = alphabet[64];
        out[idx + 2] = alphabet[64];
        tmp <<= 4;
        out[idx + 1] = alphabet[tmp & 0x3f];
        tmp >>= 6;
        out[idx] = alphabet[tmp & 0x3f];
        idx += 4;*/
        // 循环写法
        int bit = 0, bt1 = 8, bt2 = 8;
        int mask = 0x00, offsetm = 0, offsetl = 0;
        for(; bit < 16; bit += 3, idx += 4){
            offsetm = 64 - (bit + 3) * 8;
            offsetl = 0;
            tmp = 0;
            if(bt1 > 3){
                mask = (1 << 8 * 3) - 1;
            }else if(bt1 >= 0){
                mask = (1 << 8 * bt1) - 1;
                bt2 -= 3 - bt1;
            }else{
                mask = (1 << 8 * ((bt2 > 3) ? 3 : bt2)) - 1;
                bt2 -= 3;
            }
            if(bt1 > 0){
                bt1 -= 3;
                tmp = (int)((offsetm < 0) ? msb : (msb >>> offsetm) & mask);
                if(bt1 < 0){
                    tmp <<= Math.abs(offsetm);
                    mask = (1 << 8 * Math.abs(bt1)) - 1;
                }
            }
            if(offsetm < 0){
                offsetl = 64 + offsetm;
                tmp |= ((offsetl < 0) ? lsb : (lsb >>> offsetl)) & mask;
            }
            if(bit == 15){
                out[idx + 3] = alphabet[64];
                out[idx + 2] = alphabet[64];
                tmp <<= 4;
            }else{
                out[idx + 3] = alphabet[tmp & 0x3f];
                tmp >>= 6;
                out[idx + 2] = alphabet[tmp & 0x3f];
                tmp >>= 6;
            }
            out[idx + 1] = alphabet[tmp & 0x3f];
            tmp >>= 6;
            out[idx] = alphabet[tmp & 0x3f];
        }
        final String randomStr = new String(out, 0, 22);
        return randomStr;
    }

}