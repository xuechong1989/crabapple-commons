package xc.crabapple.core.data;

import java.math.BigInteger;
import xc.crabapple.core.log.LogUtil;

/*
 * 2023-07-25
 * 数据类型转换工具类
 **/
public final class DataTypeConvertUtil {

    private static final char[] HEX_CHAR = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};


    /**
     * 整形转换成网络传输的字节流（字节数组）型数据
     */
    public static final byte[] intToBytes(final int num) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (0xff & (num >> 0));
        bytes[1] = (byte) (0xff & (num >> 8));
        bytes[2] = (byte) (0xff & (num >> 16));
        bytes[3] = (byte) (0xff & (num >> 24));
        return bytes;
    }

    /**
     * 四个字节的字节数据转换成一个整形数据
     */
    public static final int byteToInt(final byte[] bytes) {
        int num = 0;
        int temp;
        temp = (0x000000ff & (bytes[0])) << 0;
        num = num | temp;
        temp = (0x000000ff & (bytes[1])) << 8;
        num = num | temp;
        temp = (0x000000ff & (bytes[2])) << 16;
        num = num | temp;
        temp = (0x000000ff & (bytes[3])) << 24;
        num = num | temp;
        return num;
    }

    /**
     * 长整形转换成网络传输的字节流（字节数组）型数据</p>
     */
    public static final byte[] longToBytes(final long num) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (0xff & (num >> (i * 8)));
        }
        return bytes;
    }

    /**
     * 大数字转换字节流（字节数组）型数据
     */
    public static final byte[] byteConvert32Bytes(final BigInteger n) {
        byte[] tamped = null;
        if (n == null) {
            return null;
        }

        if (n.toByteArray().length == 33) {
            tamped = new byte[32];
            System.arraycopy(n.toByteArray(), 1, tamped, 0, 32);
        } else if (n.toByteArray().length == 32) {
            tamped = n.toByteArray();
        } else {
            tamped = new byte[32];
            for (int i = 0; i < 32 - n.toByteArray().length; i++) {
                tamped[i] = 0;
            }
            System.arraycopy(n.toByteArray(), 0, tamped, 32 - n.toByteArray().length, n.toByteArray().length);
        }
        return tamped;
    }

    /**
     * 换字节流（字节数组）型数据转大数字
     */
    public static final BigInteger byteConvertInteger(final byte[] bytes) {
        if (bytes[0] < 0) {
            byte[] temp = new byte[bytes.length + 1];
            temp[0] = 0;
            System.arraycopy(bytes, 0, temp, 1, bytes.length);
            return new BigInteger(temp);
        }
        return new BigInteger(bytes);
    }

    /**
     * 根据字节数组获得值(十六进制数字)
     */
    public static final String getHexString(final byte[] bytes) {
        return getHexString(bytes, true);
    }

    /**
     * 根据字节数组获得值(十六进制数字)
     */
    public static final String getHexString(final byte[] bytes, final boolean upperCase) {
        String ret = "";
        for (int i = 0; i < bytes.length; i++) {
            ret += Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1);
        }
        return upperCase ? ret.toUpperCase() : ret;
    }

    /**
     * 打印十六进制字符串
     */
    public static final void printHexString(final byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append("0x").append(hex.toUpperCase()).append(",");
        }
        LogUtil.print("DataTypeConvertUtil_printHexString_1", "十六进制字符串", sb.toString());
    }

    /**
     * 将十六进制字符串转换为字节[]
     */
    public static final byte[] hexStringToBytes(final String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        String effectiveHexString = hexString.toUpperCase();
        int length = effectiveHexString.length() / 2;
        char[] hexChars = effectiveHexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    /**
     * 将字符转换为字节
     */
    public static final byte charToByte(final char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    /**
     * 用于建立十六进制字符的输出的小写字符数组
     */
    private static final char[] DIGITS_LOWER = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * 用于建立十六进制字符的输出的大写字符数组
     */
    private static final char[] DIGITS_UPPER = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * 将字节数组转换为十六进制字符数组
     */
    public static final char[] encodeHex(final byte[] data) {
        return encodeHex(data, true);
    }

    /**
     * 将字节数组转换为十六进制字符数组
     */
    public static final char[] encodeHex(final byte[] data, final boolean toLowerCase) {
        return encodeHex(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
    }

    /**
     * 将字节数组转换为十六进制字符数组
     */
    protected static final char[] encodeHex(final byte[] data, final char[] toDigits) {
        int l = data.length;
        char[] out = new char[l << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
            out[j++] = toDigits[0x0F & data[i]];
        }
        return out;
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    public static final String encodeHexString(final byte[] data) {
        return encodeHexString(data, true);
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    public static final String encodeHexString(final byte[] data, final boolean toLowerCase) {
        return encodeHexString(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    protected static final String encodeHexString(final byte[] data, final char[] toDigits) {
        return new String(encodeHex(data, toDigits));
    }

    /**
     * 将十六进制字符数组转换为字节数组
     */
    public static final byte[] decodeHex(final char[] data) {
        int len = data.length;
        if ((len & 0x01) != 0) {
            LogUtil.error("DataTypeConvertUtil_decodeHex_1", "Odd number of characters.");
            return null;
        }
        byte[] out = new byte[len >> 1];
        for (int i = 0, j = 0; j < len; i++) {
            int f = toDigit(data[j], j) << 4;
            j++;
            f = f | toDigit(data[j], j);
            j++;
            out[i] = (byte) (f & 0xFF);
        }
        return out;
    }

    /**
     * 将十六进制字符转换成一个整数
     */
    protected static final int toDigit(final char ch, final int index) {
        int digit = Character.digit(ch, 16);
        if (digit == -1) {
            LogUtil.error("DataTypeConvertUtil_toDigit_1", "Illegal hexadecimal character " + ch + " at index " + index);
            return -1;
        }
        return digit;
    }

    /**
     * 数字字符串转ASCII码字符串
     */
    public static final String StringToAsciiString(final String content) {
        String result = "";
        int max = content.length();
        for (int i = 0; i < max; i++) {
            char c = content.charAt(i);
            String b = Integer.toHexString(c);
            result = result + b;
        }
        return result;
    }

    /**
     * 十六进制转字符串
     */
    public static final String hexStringToString(final String hexString, final int encodeType) {
        String result = "";
        int max = hexString.length() / encodeType;
        for (int i = 0; i < max; i++) {
            char c = (char) hexStringToAlgorism(hexString
                    .substring(i * encodeType, (i + 1) * encodeType));
            result += c;
        }
        return result;
    }

    /**
     * 十六进制字符串装十进制
     */
    public static final int hexStringToAlgorism(final String hex) {
        String effectiveHex = hex.toUpperCase();
        int max = effectiveHex.length();
        int result = 0;
        for (int i = max; i > 0; i--) {
            char c = effectiveHex.charAt(i - 1);
            int algorism = 0;
            if (c >= '0' && c <= '9') {
                algorism = c - '0';
            } else {
                algorism = c - 55;
            }
            result += Math.pow(16, max - i) * algorism;
        }
        return result;
    }

    /**
     * 十六转二进制
     */
    public static final String hexStringToBinary(final String hex) {
        String effectiveHex = hex.toUpperCase();
        String result = "";
        int max = effectiveHex.length();
        for (int i = 0; i < max; i++) {
            char c = effectiveHex.charAt(i);
            switch (c) {
                case '0':
                    result += "0000";
                    break;
                case '1':
                    result += "0001";
                    break;
                case '2':
                    result += "0010";
                    break;
                case '3':
                    result += "0011";
                    break;
                case '4':
                    result += "0100";
                    break;
                case '5':
                    result += "0101";
                    break;
                case '6':
                    result += "0110";
                    break;
                case '7':
                    result += "0111";
                    break;
                case '8':
                    result += "1000";
                    break;
                case '9':
                    result += "1001";
                    break;
                case 'A':
                    result += "1010";
                    break;
                case 'B':
                    result += "1011";
                    break;
                case 'C':
                    result += "1100";
                    break;
                case 'D':
                    result += "1101";
                    break;
                case 'E':
                    result += "1110";
                    break;
                case 'F':
                    result += "1111";
                    break;
            }
        }
        return result;
    }

    /**
     * ASCII码字符串转数字字符串
     */
    public static final String AsciiStringToString(final String content) {
        String result = "";
        int length = content.length() / 2;
        for (int i = 0; i < length; i++) {
            String c = content.substring(i * 2, i * 2 + 2);
            int a = hexStringToAlgorism(c);
            char b = (char) a;
            String d = String.valueOf(b);
            result += d;
        }
        return result;
    }

    /**
     * 将十进制转换为指定长度的十六进制字符串
     */
    public static final String algorismToHexString(final int algorism, final int maxLength) {
        String result = "";
        result = Integer.toHexString(algorism);

        if (result.length() % 2 == 1) {
            result = "0" + result;
        }
        return patchHexString(result.toUpperCase(), maxLength);
    }

    /**
     * 字节数组转为普通字符串（ASCII对应的字符）
     */
    public static final String byteToString(final byte[] bytearray) {
        String result = "";
        char temp;

        int length = bytearray.length;
        for (int i = 0; i < length; i++) {
            temp = (char) bytearray[i];
            result += temp;
        }
        return result;
    }

    /**
     * 二进制字符串转十进制
     */
    public static final int binaryToAlgorism(final String binary) {
        int max = binary.length();
        int result = 0;
        for (int i = max; i > 0; i--) {
            char c = binary.charAt(i - 1);
            int algorism = c - '0';
            result += Math.pow(2, max - i) * algorism;
        }
        return result;
    }

    /**
     * 十进制转换为十六进制字符串
     */
    public static final String algorismToHEXString(final int algorism) {
        String result = "";
        result = Integer.toHexString(algorism);

        if (result.length() % 2 == 1) {
            result = "0" + result;

        }
        result = result.toUpperCase();

        return result;
    }

    /**
     * HEX字符串前补0，主要用于长度位数不足。
     */
    static public final String patchHexString(final String str, final int maxLength) {
        String temp = "";
        for (int i = 0; i < maxLength - str.length(); i++) {
            temp = "0" + temp;
        }
        String effectiveStr = (temp + str).substring(0, maxLength);
        return effectiveStr;
    }

    /**
     * 将一个字符串转换为int
     */
    public static final int parseToInt(final String s, final int defaultInt, final int radix) {
        int i = 0;
        try{
            i = Integer.parseInt(s, radix);
        }catch(Throwable throwable){
            i = defaultInt;
        }
        return i;
    }

    /**
     * 将一个十进制形式的数字字符串转换为int
     */
    public static final int parseToInt(final String s, final int defaultInt) {
        int i = 0;
        try{
            i = Integer.parseInt(s);
        }catch(Throwable throwable){
            i = defaultInt;
        }
        return i;
    }

    /**
     * 十六进制串转化为byte数组
     */
    public static final byte[] hexToByte(final String hex) throws Throwable {
        if(hex == null || hex.trim().equals("")) {
            return new byte[0];
        }
        byte[] bytes = new byte[hex.length() / 2];
        for(int i = 0; i < hex.length() / 2; i++) {
            String subStr = hex.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }
        return bytes;
    }

    /**
     * 字节数组转换为十六进制字符串
     */
    public static final String byteToHex(final byte bytes[]) {
        char[] buf = new char[bytes.length * 2];
        int index = 0;
        for(byte b : bytes) { // 利用位运算进行转换，可以看作方法一的变种
            buf[index++] = HEX_CHAR[b >>> 4 & 0xf];
            buf[index++] = HEX_CHAR[b & 0xf];
        }

        return new String(buf);
    }

    public static final byte[] subByte(final byte[] input, final int startIndex, final int length) {
        byte[] bt = new byte[length];
        for (int i = 0; i < length; i++) {
            bt[i] = input[i + startIndex];
        }
        return bt;
    }

    /**
     * 二进制字符串转换为byte数组,每个字节以","隔开
     **/
    public static final byte[] conver2HexToByte(final String hex2Str) {
        byte[] b = new byte[hex2Str.length() / 8];
        for (int i = 0; i < b.length; i++) {
            b[i] = Long.valueOf(hex2Str.substring(i * 8, (i + 1) * 8), 2).byteValue();
        }
        return b;
    }

}

