package xc.crabapple.core.encoder;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.Base64;
import javax.imageio.ImageIO;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;

/**
 * 2017-01-01 构建base64处理类
 * 2024-11-20 代码整合与重构
 */
public final class Base64Util {

    public static final int NO_OPTIONS = 0;
    public static final int ENCODE = 1;
    public static final int DECODE = 0;
    public static final int DO_BREAK_LINES = 8;
    public static final int URL_SAFE = 16;
    public static final int ORDERED = 32;
    private static final int MAX_LINE_LENGTH = 76;
    private static final  byte EQUALS_SIGN = (byte)'=';
    private static final  byte NEW_LINE = (byte)'\n';
    private static final  byte WHITE_SPACE_ENC = -5; // Indicates white space in encoding
    private static final  byte EQUALS_SIGN_ENC = -1; // Indicates equals sign in encoding
    private static final  byte[] _STANDARD_ALPHABET = {
            (byte)'A', (byte)'B', (byte)'C', (byte)'D', (byte)'E', (byte)'F', (byte)'G',
            (byte)'H', (byte)'I', (byte)'J', (byte)'K', (byte)'L', (byte)'M', (byte)'N',
            (byte)'O', (byte)'P', (byte)'Q', (byte)'R', (byte)'S', (byte)'T', (byte)'U',
            (byte)'V', (byte)'W', (byte)'X', (byte)'Y', (byte)'Z',
            (byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f', (byte)'g',
            (byte)'h', (byte)'i', (byte)'j', (byte)'k', (byte)'l', (byte)'m', (byte)'n',
            (byte)'o', (byte)'p', (byte)'q', (byte)'r', (byte)'s', (byte)'t', (byte)'u',
            (byte)'v', (byte)'w', (byte)'x', (byte)'y', (byte)'z',
            (byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', (byte)'5',
            (byte)'6', (byte)'7', (byte)'8', (byte)'9', (byte)'+', (byte)'/'
    };
    private static final  byte[] _STANDARD_DECODABET = {
            -9,-9,-9,-9,-9,-9,-9,-9,-9,                 // Decimal  0 -  8
            -5,-5,                                      // Whitespace: Tab and Linefeed
            -9,-9,                                      // Decimal 11 - 12
            -5,                                         // Whitespace: Carriage Return
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 14 - 26
            -9,-9,-9,-9,-9,                             // Decimal 27 - 31
            -5,                                         // Whitespace: Space
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,              // Decimal 33 - 42
            62,                                         // Plus sign at decimal 43
            -9,-9,-9,                                   // Decimal 44 - 46
            63,                                         // Slash at decimal 47
            52,53,54,55,56,57,58,59,60,61,              // Numbers zero through nine
            -9,-9,-9,                                   // Decimal 58 - 60
            -1,                                         // Equals sign at decimal 61
            -9,-9,-9,                                      // Decimal 62 - 64
            0,1,2,3,4,5,6,7,8,9,10,11,12,13,            // Letters 'A' through 'N'
            14,15,16,17,18,19,20,21,22,23,24,25,        // Letters 'O' through 'Z'
            -9,-9,-9,-9,-9,-9,                          // Decimal 91 - 96
            26,27,28,29,30,31,32,33,34,35,36,37,38,     // Letters 'a' through 'm'
            39,40,41,42,43,44,45,46,47,48,49,50,51,     // Letters 'n' through 'z'
            -9,-9,-9,-9,-9                              // Decimal 123 - 127
            ,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,       // Decimal 128 - 139
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 140 - 152
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 153 - 165
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 166 - 178
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 179 - 191
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 192 - 204
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 205 - 217
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 218 - 230
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 231 - 243
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9         // Decimal 244 - 255
    };
    private static final  byte[] _URL_SAFE_ALPHABET = {
            (byte)'A', (byte)'B', (byte)'C', (byte)'D', (byte)'E', (byte)'F', (byte)'G',
            (byte)'H', (byte)'I', (byte)'J', (byte)'K', (byte)'L', (byte)'M', (byte)'N',
            (byte)'O', (byte)'P', (byte)'Q', (byte)'R', (byte)'S', (byte)'T', (byte)'U',
            (byte)'V', (byte)'W', (byte)'X', (byte)'Y', (byte)'Z',
            (byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f', (byte)'g',
            (byte)'h', (byte)'i', (byte)'j', (byte)'k', (byte)'l', (byte)'m', (byte)'n',
            (byte)'o', (byte)'p', (byte)'q', (byte)'r', (byte)'s', (byte)'t', (byte)'u',
            (byte)'v', (byte)'w', (byte)'x', (byte)'y', (byte)'z',
            (byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', (byte)'5',
            (byte)'6', (byte)'7', (byte)'8', (byte)'9', (byte)'-', (byte)'_'
    };
    private static final  byte[] _URL_SAFE_DECODABET = {
            -9,-9,-9,-9,-9,-9,-9,-9,-9,                 // Decimal  0 -  8
            -5,-5,                                      // Whitespace: Tab and Linefeed
            -9,-9,                                      // Decimal 11 - 12
            -5,                                         // Whitespace: Carriage Return
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 14 - 26
            -9,-9,-9,-9,-9,                             // Decimal 27 - 31
            -5,                                         // Whitespace: Space
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,              // Decimal 33 - 42
            -9,                                         // Plus sign at decimal 43
            -9,                                         // Decimal 44
            62,                                         // Minus sign at decimal 45
            -9,                                         // Decimal 46
            -9,                                         // Slash at decimal 47
            52,53,54,55,56,57,58,59,60,61,              // Numbers zero through nine
            -9,-9,-9,                                   // Decimal 58 - 60
            -1,                                         // Equals sign at decimal 61
            -9,-9,-9,                                   // Decimal 62 - 64
            0,1,2,3,4,5,6,7,8,9,10,11,12,13,            // Letters 'A' through 'N'
            14,15,16,17,18,19,20,21,22,23,24,25,        // Letters 'O' through 'Z'
            -9,-9,-9,-9,                                // Decimal 91 - 94
            63,                                         // Underscore at decimal 95
            -9,                                         // Decimal 96
            26,27,28,29,30,31,32,33,34,35,36,37,38,     // Letters 'a' through 'm'
            39,40,41,42,43,44,45,46,47,48,49,50,51,     // Letters 'n' through 'z'
            -9,-9,-9,-9,-9                              // Decimal 123 - 127
            ,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 128 - 139
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 140 - 152
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 153 - 165
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 166 - 178
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 179 - 191
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 192 - 204
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 205 - 217
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 218 - 230
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 231 - 243
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9         // Decimal 244 - 255
    };
    private static final  byte[] _ORDERED_ALPHABET = {
            (byte)'-',
            (byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4',
            (byte)'5', (byte)'6', (byte)'7', (byte)'8', (byte)'9',
            (byte)'A', (byte)'B', (byte)'C', (byte)'D', (byte)'E', (byte)'F', (byte)'G',
            (byte)'H', (byte)'I', (byte)'J', (byte)'K', (byte)'L', (byte)'M', (byte)'N',
            (byte)'O', (byte)'P', (byte)'Q', (byte)'R', (byte)'S', (byte)'T', (byte)'U',
            (byte)'V', (byte)'W', (byte)'X', (byte)'Y', (byte)'Z',
            (byte)'_',
            (byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f', (byte)'g',
            (byte)'h', (byte)'i', (byte)'j', (byte)'k', (byte)'l', (byte)'m', (byte)'n',
            (byte)'o', (byte)'p', (byte)'q', (byte)'r', (byte)'s', (byte)'t', (byte)'u',
            (byte)'v', (byte)'w', (byte)'x', (byte)'y', (byte)'z'
    };
    private static final  byte[] _ORDERED_DECODABET = {
            -9,-9,-9,-9,-9,-9,-9,-9,-9,                 // Decimal  0 -  8
            -5,-5,                                      // Whitespace: Tab and Linefeed
            -9,-9,                                      // Decimal 11 - 12
            -5,                                         // Whitespace: Carriage Return
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 14 - 26
            -9,-9,-9,-9,-9,                             // Decimal 27 - 31
            -5,                                         // Whitespace: Space
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,              // Decimal 33 - 42
            -9,                                         // Plus sign at decimal 43
            -9,                                         // Decimal 44
            0,                                          // Minus sign at decimal 45
            -9,                                         // Decimal 46
            -9,                                         // Slash at decimal 47
            1,2,3,4,5,6,7,8,9,10,                       // Numbers zero through nine
            -9,-9,-9,                                   // Decimal 58 - 60
            -1,                                         // Equals sign at decimal 61
            -9,-9,-9,                                   // Decimal 62 - 64
            11,12,13,14,15,16,17,18,19,20,21,22,23,     // Letters 'A' through 'M'
            24,25,26,27,28,29,30,31,32,33,34,35,36,     // Letters 'N' through 'Z'
            -9,-9,-9,-9,                                // Decimal 91 - 94
            37,                                         // Underscore at decimal 95
            -9,                                         // Decimal 96
            38,39,40,41,42,43,44,45,46,47,48,49,50,     // Letters 'a' through 'm'
            51,52,53,54,55,56,57,58,59,60,61,62,63,     // Letters 'n' through 'z'
            -9,-9,-9,-9,-9                                 // Decimal 123 - 127
            ,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 128 - 139
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 140 - 152
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 153 - 165
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 166 - 178
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 179 - 191
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 192 - 204
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 205 - 217
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 218 - 230
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 231 - 243
            -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9         // Decimal 244 - 255
    };

    /**
     * 2024-11-20
     * 方法1：对指定字节数组进行base64编码，返回base64字节数组。（推荐）
     * @param sourceBytes
     * @return
     */
    public static final byte[] encodeBytes2bytes(final byte[] sourceBytes){
        final Base64.Encoder encoder = Base64.getEncoder();
        final byte[] base64Bytes = encoder.encode(sourceBytes);
        return base64Bytes;
    }

    /**
     * 2024-11-20
     * 方法1：对指定字节数组进行base64编码，返回base64字符串。（推荐）
     * @param sourceBytes
     * @return
     */
    public static final String encodeBytes2str(final byte[] sourceBytes){
        final Base64.Encoder encoder = Base64.getEncoder();
        final String base64Str = encoder.encodeToString(sourceBytes);
        return base64Str;
    }

    /**
     * 2024-11-20
     * 方法1：对指定字符串进行base64编码，返回base64字节数组。（推荐）
     * @param sourceStr
     * @return
     */
    public static final byte[] encodeStr2bytes(final String sourceStr){
        final Charset charset = Charset.forName("UTF-8");
        final byte[] base64Bytes = encodeStr2bytes(sourceStr, charset);
        return base64Bytes;
    }

    /**
     * 2024-11-20
     * 方法1：对指定字符串进行base64编码，返回base64字节数组。（推荐）
     * @param sourceStr
     * @param charsetName
     * @return
     */
    public static final byte[] encodeStr2bytes(final String sourceStr, final String charsetName){
        final Charset charset = Charset.forName(charsetName);
        final byte[] base64Bytes = encodeStr2bytes(sourceStr, charset);
        return base64Bytes;
    }

    /**
     * 2024-11-20
     * 方法1：对指定字符串进行base64编码，返回base64字节数组。（推荐）
     * @param sourceStr
     * @param charset
     * @return
     */
    public static final byte[] encodeStr2bytes(final String sourceStr, final Charset charset){
        final byte[] sourceBytes = sourceStr.getBytes(charset);
        final byte[] base64Bytes = encodeBytes2bytes(sourceBytes);
        return base64Bytes;
    }

    /**
     * 2024-11-20
     * 方法1：对指定字符串进行base64编码，返回base64字符串。（推荐）
     * @param sourceStr
     * @return
     */
    public static final String encodeStr2str(final String sourceStr){
        final Charset charset = Charset.forName("UTF-8");
        final String base64Str = encodeStr2str(sourceStr, charset);
        return base64Str;
    }

    /**
     * 2024-11-20
     * 方法1：对指定字符串进行base64编码，返回base64字符串。（推荐）
     * @param sourceStr
     * @param charsetName
     * @return
     */
    public static final String encodeStr2str(final String sourceStr, final String charsetName){
        final Charset charset = Charset.forName(charsetName);
        final String base64Str = encodeStr2str(sourceStr, charset);
        return base64Str;
    }

    /**
     * 2024-11-20
     * 方法1：对指定字符串进行base64编码，返回base64字符串。（推荐）
     * @param sourceStr
     * @param charset
     * @return
     */
    public static final String encodeStr2str(final String sourceStr, final Charset charset){
        final byte[] sourceBytes = sourceStr.getBytes(charset);
        final byte[] base64Bytes = encodeBytes2bytes(sourceBytes);
        final String base64Str = new String(base64Bytes, charset);
        return base64Str;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字节数组进行base64编码，返回base64字节数组。
     * @param sourceBytes
     * @return
     */
    public static final byte[] encodeBytes2bytes2(final byte[] sourceBytes){
        final byte[] base64Bytes = p_encodeBytes2bytes(sourceBytes, 0, sourceBytes.length, NO_OPTIONS);
        return base64Bytes;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字节数组进行base64编码，返回base64字符串。
     * @param sourceBytes
     * @return
     */
    public static final String encodeBytes2str2(final byte[] sourceBytes){
        final Charset charset = Charset.forName("UTF-8");
        final String base64Str = encodeBytes2str2(sourceBytes, charset);
        return base64Str;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字节数组进行base64编码，返回base64字符串。
     * @param sourceBytes
     * @param charsetName
     * @return
     */
    public static final String encodeBytes2str2(final byte[] sourceBytes, final String charsetName){
        final Charset charset = Charset.forName(charsetName);
        final String base64Str = encodeBytes2str2(sourceBytes, charset);
        return base64Str;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字节数组进行base64编码，返回base64字符串。
     * @param sourceBytes
     * @param charset
     * @return
     */
    public static final String encodeBytes2str2(final byte[] sourceBytes, final Charset charset){
        final byte[] base64Bytes = p_encodeBytes2bytes(sourceBytes, 0, sourceBytes.length, NO_OPTIONS);
        final String base64Str = new String(base64Bytes, charset);
        return base64Str;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字符串进行base64编码，返回base64字节数组。
     * @param sourceStr
     * @return
     */
    public static final byte[] encodeStr2bytes2(final String sourceStr){
        final Charset charset = Charset.forName("UTF-8");
        final byte[] base64Bytes = encodeStr2bytes2(sourceStr, charset);
        return base64Bytes;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字符串进行base64编码，返回base64字节数组。
     * @param sourceStr
     * @param charsetName
     * @return
     */
    public static final byte[] encodeStr2bytes2(final String sourceStr, final String charsetName){
        final Charset charset = Charset.forName(charsetName);
        final byte[] base64Bytes = encodeStr2bytes2(sourceStr, charset);
        return base64Bytes;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字符串进行base64编码，返回base64字节数组。
     * @param sourceStr
     * @param charset
     * @return
     */
    public static final byte[] encodeStr2bytes2(final String sourceStr, final Charset charset){
        final byte[] sourceBytes = sourceStr.getBytes(charset);
        final byte[] base64Bytes = p_encodeBytes2bytes(sourceBytes, 0, sourceBytes.length, NO_OPTIONS);
        return base64Bytes;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字符串进行base64编码，返回base64字符串。
     * @param sourceStr
     * @return
     */
    public static final String encodeStr2str2(final String sourceStr){
        final Charset charset = Charset.forName("UTF-8");
        final String base64Str = encodeStr2str2(sourceStr, charset);
        return base64Str;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字符串进行base64编码，返回base64字符串。
     * @param sourceStr
     * @param charsetName
     * @return
     */
    public static final String encodeStr2str2(final String sourceStr, final String charsetName){
        final Charset charset = Charset.forName(charsetName);
        final String base64Str = encodeStr2str2(sourceStr, charset);
        return base64Str;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字符串进行base64编码，返回base64字符串。
     * @param sourceStr
     * @param charset
     * @return
     */
    public static final String encodeStr2str2(final String sourceStr, final Charset charset){
        final byte[] sourceBytes = sourceStr.getBytes(charset);
        final byte[] base64Bytes = encodeBytes2bytes2(sourceBytes);
        final String base64Str = new String(base64Bytes, charset);
        return base64Str;
    }

    /**
     * 2024-11-20
     * 对指定的pdf文件进行base64编码。
     * @param pdfUrl
     * @return
     */
    public static final String encodePdf(final String pdfUrl){
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        File file = new File(pdfUrl);
        try{
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            byteArrayOutputStream = new ByteArrayOutputStream();
            bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream);
            byte[] buffer = new byte[1024];
            int len = bufferedInputStream.read(buffer);
            while (len != -1){
                bufferedOutputStream.write(buffer, 0, len);
                len = bufferedInputStream.read(buffer);
            }
            //刷新此输出流并强制写出所有缓冲的输出字节
            bufferedOutputStream.flush();
            byte[] sourceBytes = byteArrayOutputStream.toByteArray();
            String bate64Str = encodeBytes2str(sourceBytes);
            if(bate64Str == null){
                return null;
            }
            bate64Str = bate64Str.trim();
            return bate64Str;
        }catch(Throwable throwable){
            LogUtil.error("Base64Util_encodePdf_t1", throwable, pdfUrl);
            return null;
        } finally {
            IOUtil.close(bufferedOutputStream, byteArrayOutputStream, bufferedInputStream, fileInputStream);
        }
    }

    /**
     * 2024-11-20
     * 对指定的png文件进行base64编码。
     * @param pngUrl
     * @return
     */
    public static final String encodePng(final String pngUrl){
        File imageFile = new File(pngUrl);
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            BufferedImage bufferedImage = ImageIO.read(imageFile);
            byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
            byte[] sourceBytes = byteArrayOutputStream.toByteArray();
            final String base64Str = encodeBytes2str(sourceBytes);
            return base64Str;
        }catch (Throwable throwable){
            LogUtil.error("Base64Util_encodePng_t1", throwable, pngUrl);
            return null;
        }finally {
            IOUtil.close(byteArrayOutputStream);
        }
    }

    /**
     * 2024-11-20
     * 方法1：对指定字节数组进行base64解码，返回原文字节数组。（推荐）
     * @param base64Bytes
     * @return
     */
    public static final byte[] decodeBytes2bytes(final byte[] base64Bytes){
        if(base64Bytes == null || base64Bytes.length == 0){
            return null;
        }
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] sourceBytes = decoder.decode(base64Bytes);
        return sourceBytes;
    }

    /**
     * 2024-11-20
     * 方法1：对指定字节数组进行base64解码，返回原文字符串。（推荐）
     * @param base64Bytes
     * @return
     */
    public static final String decodeBytes2str(final byte[] base64Bytes){
        if(base64Bytes == null || base64Bytes.length == 0){
            return null;
        }
        final Charset charset = Charset.forName("UTF-8");
        final String base64Str = decodeBytes2str(base64Bytes, charset);
        return base64Str;
    }

    /**
     * 2024-11-20
     * 方法1：对指定字节数组进行base64解码，返回原文字符串。（推荐）
     * @param base64Bytes
     * @param charsetName
     * @return
     */
    public static final String decodeBytes2str(final byte[] base64Bytes, final String charsetName){
        if(base64Bytes == null || base64Bytes.length == 0){
            return null;
        }
        final Charset charset = Charset.forName(charsetName);
        final String base64Str = decodeBytes2str(base64Bytes, charset);
        return base64Str;
    }

    /**
     * 2024-11-20
     * 方法1：对指定字节数组进行base64解码，返回原文字符串。（推荐）
     * @param base64Bytes
     * @param charset
     * @return
     */
    public static final String decodeBytes2str(final byte[] base64Bytes, final Charset charset){
        if(base64Bytes == null || base64Bytes.length == 0){
            return null;
        }
        final byte[] sourceBytes = decodeBytes2bytes(base64Bytes);
        final String base64Str = new String(sourceBytes, charset);
        return base64Str;
    }

    /**
     * 2024-11-20
     * 方法1：对指定字符串进行base64解码，返回原文字节数组。（推荐）
     * @param base64Str
     * @return
     */
    public static final byte[] decodeStr2bytes(final String base64Str){
        if(StringUtil.isBlank(base64Str)){
            return null;
        }
        //
        final Charset charset = Charset.forName("UTF-8");
        final byte[] base64Bytes = base64Str.getBytes(charset);
        final byte[] sourceBytes = decodeBytes2bytes(base64Bytes);
        return sourceBytes;
    }

    /**
     * 2024-11-20
     * 方法1：对指定字符串进行base64解码，返回原文字节数组。（推荐）
     * @param base64Str
     * @param charsetName
     * @return
     */
    public static final byte[] decodeStr2bytes(final String base64Str, final String charsetName){
        if(StringUtil.isBlank(base64Str)){
            return null;
        }
        //
        final Charset charset = Charset.forName(charsetName);
        final byte[] base64Bytes = base64Str.getBytes(charset);
        final byte[] sourceBytes = decodeBytes2bytes(base64Bytes);
        return sourceBytes;
    }

    /**
     * 2024-11-20
     * 方法1：对指定字符串进行base64解码，返回原文字节数组。（推荐）
     * @param base64Str
     * @param charset
     * @return
     */
    public static final byte[] decodeStr2bytes(final String base64Str, final Charset charset){
        if(StringUtil.isBlank(base64Str)){
            return null;
        }
        //
        final byte[] base64Bytes = base64Str.getBytes(charset);
        final byte[] sourceBytes = decodeBytes2bytes(base64Bytes);
        return sourceBytes;
    }

    /**
     * 2024-11-20
     * 方法1：对指定字符串进行base64解码，返回原文字符串。（推荐）
     * @param base64Str
     * @return
     */
    public static final String decodeStr2str(final String base64Str){
        if(StringUtil.isBlank(base64Str)){
            return null;
        }
        //
        final Charset charset = Charset.forName("UTF-8");
        final String sourceStr = decodeStr2str(base64Str, charset);
        return sourceStr;
    }

    /**
     * 2024-11-20
     * 方法1：对指定字符串进行base64解码，返回原文字符串。（推荐）
     * @param base64Str
     * @param charsetName
     * @return
     */
    public static final String decodeStr2str(final String base64Str, final String charsetName){
        if(StringUtil.isBlank(base64Str)){
            return null;
        }
        //
        final Charset charset = Charset.forName(charsetName);
        final String sourceStr = decodeStr2str(base64Str, charset);
        return sourceStr;
    }

    /**
     * 2024-11-20
     * 方法1：对指定字符串进行base64解码，返回原文字符串。（推荐）
     * @param base64Str
     * @param charset
     * @return
     */
    public static final String decodeStr2str(final String base64Str, final Charset charset){
        if(StringUtil.isBlank(base64Str)){
            return null;
        }
        //
        try{
            final byte[] base64Bytes = base64Str.getBytes(charset);
            final byte[] sourceBytes = decodeBytes2bytes(base64Bytes);
            final String sourceStr = new String(sourceBytes, charset);
            return sourceStr;
        }catch(Throwable throwable){
            LogUtil.error("Base64Util_decodeStr2str_t1", throwable, base64Str);
            return null;
        }
    }

    /**
     * 2024-11-20
     * 方法2：对指定字节数组进行base64解码，返回原文字节数组。
     * @param base64Bytes
     * @return
     */
    public static final byte[] decodeBytes2bytes2(final byte[] base64Bytes){
        if(base64Bytes == null || base64Bytes.length == 0){
            return null;
        }
        //
        final byte[] sourceBytes = p_decodeBytes2bytes(base64Bytes, 0, base64Bytes.length, NO_OPTIONS);
        return sourceBytes;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字节数组进行base64解码，返回原文字符串。
     * @param base64Bytes
     * @return
     */
    public static final String decodeBytes2str2(final byte[] base64Bytes){
        if(base64Bytes == null || base64Bytes.length == 0){
            return null;
        }
        //
        final Charset charset = Charset.forName("UTF-8");
        final String base64Str = decodeBytes2str2(base64Bytes, charset);
        return base64Str;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字节数组进行base64解码，返回原文字符串。
     * @param base64Bytes
     * @param charsetName
     * @return
     */
    public static final String decodeBytes2str2(final byte[] base64Bytes, final String charsetName){
        if(base64Bytes == null || base64Bytes.length == 0){
            return null;
        }
        //
        final Charset charset = Charset.forName(charsetName);
        final String base64Str = decodeBytes2str2(base64Bytes, charset);
        return base64Str;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字节数组进行base64解码，返回原文字符串。
     * @param base64Bytes
     * @param charset
     * @return
     */
    public static final String decodeBytes2str2(final byte[] base64Bytes, final Charset charset){
        if(base64Bytes == null || base64Bytes.length == 0){
            return null;
        }
        //
        final byte[] sourceBytes = decodeBytes2bytes2(base64Bytes);
        final String base64Str = new String(sourceBytes, charset);
        return base64Str;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字符串进行base64解码，返回原文字节数组。
     * @param base64Str
     * @return
     */
    public static final byte[] decodeStr2bytes2(final String base64Str){
        if(base64Str == null || base64Str.length() == 0){
            return null;
        }
        //
        final Charset charset = Charset.forName("UTF-8");
        final byte[] base64Bytes = base64Str.getBytes(charset);
        final byte[] sourceBytes = decodeBytes2bytes2(base64Bytes);
        return sourceBytes;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字符串进行base64解码，返回原文字节数组。
     * @param base64Str
     * @param charsetName
     * @return
     */
    public static final byte[] decodeStr2bytes2(final String base64Str, final String charsetName){
        if(base64Str == null || base64Str.length() == 0){
            return null;
        }
        //
        final Charset charset = Charset.forName(charsetName);
        final byte[] base64Bytes = base64Str.getBytes(charset);
        final byte[] sourceBytes = decodeBytes2bytes2(base64Bytes);
        return sourceBytes;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字符串进行base64解码，返回原文字节数组。
     * @param base64Str
     * @param charset
     * @return
     */
    public static final byte[] decodeStr2bytes2(final String base64Str, final Charset charset){
        if(base64Str == null || base64Str.length() == 0){
            return null;
        }
        //
        final byte[] base64Bytes = base64Str.getBytes(charset);
        final byte[] sourceBytes = decodeBytes2bytes2(base64Bytes);
        return sourceBytes;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字符串进行base64解码，返回原文字符串。
     * @param base64Str
     * @return
     */
    public static final String decodeStr2str2(final String base64Str){
        if(base64Str == null || base64Str.length() == 0){
            return null;
        }
        //
        final Charset charset = Charset.forName("UTF-8");
        final String sourceStr = decodeStr2str2(base64Str, charset);
        return sourceStr;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字符串进行base64解码，返回原文字符串。
     * @param base64Str
     * @param charsetName
     * @return
     */
    public static final String decodeStr2str2(final String base64Str, final String charsetName){
        if(base64Str == null || base64Str.length() == 0){
            return null;
        }
        //
        final Charset charset = Charset.forName(charsetName);
        final String sourceStr = decodeStr2str2(base64Str, charset);
        return sourceStr;
    }

    /**
     * 2024-11-20
     * 方法2：对指定字符串进行base64解码，返回原文字符串。
     * @param base64Str
     * @param charset
     * @return
     */
    public static final String decodeStr2str2(final String base64Str, final Charset charset){
        if(base64Str == null || base64Str.length() == 0){
            return null;
        }
        //
        try{
            final byte[] base64Bytes = base64Str.getBytes(charset);
            final byte[] sourceBytes = decodeBytes2bytes2(base64Bytes);
            final String sourceStr = new String(sourceBytes, charset);
            return sourceStr;
        }catch(Throwable throwable){
            LogUtil.error("Base64Util_decodeStr2str2_t1", throwable, base64Str);
            return null;
        }
    }

    public static final boolean isBase64(final byte[] bytes){
        try{
            final byte[] sourceBytes = decodeBytes2bytes(bytes);
            return true;
        }catch(Throwable throwable){
            return false;
        }
    }

    private static final byte[] p_encode3to4(final byte[] source, final int srcOffset, final int numSigBytes, final byte[] destination, final int destOffset, final int options){
        byte[] ALPHABET = p_getAlphabet(options);
        int inBuff = ( numSigBytes > 0 ? ((source[srcOffset] << 24) >>> 8) : 0)
                | ( numSigBytes > 1 ? ((source[srcOffset + 1] << 24) >>> 16) : 0)
                | ( numSigBytes > 2 ? ((source[srcOffset + 2] << 24) >>> 24) : 0);
        switch( numSigBytes){
            case 3:
                destination[destOffset] = ALPHABET[(inBuff >>> 18)];
                destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
                destination[destOffset + 2] = ALPHABET[(inBuff >>> 6) & 0x3f];
                destination[destOffset + 3] = ALPHABET[(inBuff) & 0x3f];
                return destination;
            case 2:
                destination[destOffset] = ALPHABET[(inBuff >>> 18)];
                destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
                destination[destOffset + 2] = ALPHABET[(inBuff >>> 6) & 0x3f];
                destination[destOffset + 3] = EQUALS_SIGN;
                return destination;
            case 1:
                destination[destOffset] = ALPHABET[(inBuff >>> 18)];
                destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
                destination[destOffset + 2] = EQUALS_SIGN;
                destination[destOffset + 3] = EQUALS_SIGN;
                return destination;
            default:
                return destination;
        }
    }

    private static final byte[] p_encodeBytes2bytes(final byte[] source, final int off, final int len, final int options){
        if(source == null ){
            LogUtil.error("Base64Util_p_encodeBytes2bytes_1", "source_is_null");
            return null;
        }
        if(off < 0 ){
            LogUtil.error("Base64Util_p_encodeBytes2bytes_2", "Cannot have negative offset", off);
            return null;
        }
        if(len < 0 ){
            LogUtil.error("Base64Util_p_encodeBytes2bytes_3", "Cannot have length offset", len);
            return null;
        }
        if(off + len > source.length  ){
            LogUtil.error("Base64Util_p_encodeBytes2bytes_4", off, len, source.length);
            return null;
        }
        boolean breakLines = (options & DO_BREAK_LINES) > 0;
        int encLen = ( len / 3 ) * 4 + ( len % 3 > 0 ? 4 : 0);  // Bytes needed for actual encoding
        if(breakLines ){
            encLen +=  encLen / MAX_LINE_LENGTH;  // Plus extra newline characters
        }
        byte[] outBuff = new byte[encLen];
        int d = 0;
        int e = 0;
        int len2 = len - 2;
        int lineLength = 0;
        for( ; d < len2; d+= 3, e+= 4){
            p_encode3to4( source, d+off, 3, outBuff, e, options);
            lineLength +=  4;
            if(breakLines && lineLength >=  MAX_LINE_LENGTH){
                outBuff[e+4] = NEW_LINE;
                e++;
                lineLength = 0;
            }
        }
        if(d < len){
            p_encode3to4( source, d+off, len - d, outBuff, e, options);
            e +=  4;
        }
        if(e <=  outBuff.length - 1 ){
            byte[] finalOut = new byte[e];
            System.arraycopy(outBuff,0, finalOut,0,e);
            return finalOut;
        }else{
            return outBuff;
        }
    }

    private static final byte[] p_decodeBytes2bytes(final byte[] source, final int off, final int len, final int options){
        if(source == null ){
            LogUtil.error("Base64Util_p_decodeBytes2bytes_1", "source_is_null");
            return null;
        }
        if(off < 0 || off + len > source.length ){
            LogUtil.error("Base64Util_p_decodeBytes2bytes_2", source.length, off, len);
            return null;
        }
        if(len == 0 ){
            return new byte[0];
        }else if(len < 4 ){
            LogUtil.error("Base64Util_p_decodeBytes2bytes_3", "Base64-encoded string must have at least four characters, but length specified was ", len);
            return null;
        }
        byte[] DECODABET = p_getDecodabet( options);
        int len34 = len * 3 / 4;  // Estimate on array size
        byte[] outBuff = new byte[len34];  // Upper limit on size of output
        int outBuffPosn = 0;  // Keep track of where we're writing
        byte[] b4 = new byte[4];  // Four byte buffer from source, eliminating white space
        int b4Posn = 0;  // Keep track of four byte input buffer
        int i = 0;  // Source array counter
        byte   sbiDecode = 0;  // Special value from DECODABET
        for(i = off; i < off+len; i++){  // Loop through source
            sbiDecode = DECODABET[source[i]&0xFF];
            if(sbiDecode >=  WHITE_SPACE_ENC){
                if(sbiDecode >=  EQUALS_SIGN_ENC){
                    b4[b4Posn++] = source[i];         // Save non-whitespace
                    if(b4Posn > 3){                  // Time to decode?
                        outBuffPosn +=  p_decode4to3( b4, 0, outBuff, outBuffPosn, options);
                        b4Posn = 0;
                        if(source[i] == EQUALS_SIGN){
                            break;
                        }
                    }
                }
            }else{
                String msg = String.format("Bad Base64 input character decimal %d in array position %d", ((int)source[i])&0xFF, i);
                LogUtil.error("Base64Util_p_decodeBytes2bytes_4", msg);
            }
        }
        byte[] out = new byte[outBuffPosn];
        System.arraycopy( outBuff, 0, out, 0, outBuffPosn);
        return out;
    }

    private static final int p_decode4to3(final byte[] source, final int srcOffset, final byte[] destination, final int destOffset, final int options){
        if(source == null ){
            LogUtil.error("Base64Util_p_decode4to3_1", "Source array was null.");
            return 0;
        }
        if(destination == null ){
            LogUtil.error("Base64Util_p_decode4to3_2", "Destination array was null.");
            return 0;
        }
        if(srcOffset < 0 || srcOffset + 3 >=  source.length ){
            LogUtil.error("Base64Util_p_decode4to3_3", String.format("Source array with length %d cannot have offset of %d and still process four bytes.", source.length, srcOffset));
            return 0;
        }
        if(destOffset < 0 || destOffset +2 >=  destination.length ){
            LogUtil.error("Base64Util_p_decode4to3_4", String.format("Destination array with length %d cannot have offset of %d and still store three bytes.", destination.length, destOffset));
            return 0;
        }
        byte[] DECODABET = p_getDecodabet( options);
        if(source[srcOffset + 2] == EQUALS_SIGN){
            int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18) | ((DECODABET[source[srcOffset+1]] & 0xFF) << 12);
            destination[destOffset] = (byte)(outBuff >>> 16);
            return 1;
        }
        else if(source[srcOffset + 3] == EQUALS_SIGN){
            int outBuff = ((DECODABET[source[srcOffset]] & 0xFF ) << 18) | ((DECODABET[source[srcOffset + 1 ]] & 0xFF ) << 12) | ((DECODABET[source[srcOffset + 2 ]] & 0xFF ) <<  6);
            destination[destOffset] = (byte)( outBuff >>> 16);
            destination[destOffset + 1] = (byte)( outBuff >>> 8);
            return 2;
        }else {
            int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18) | ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12) | ((DECODABET[source[srcOffset + 2]] & 0xFF) << 6) | ((DECODABET[source[srcOffset + 3]] & 0xFF));
            destination[destOffset] = (byte)(outBuff >> 16);
            destination[destOffset + 1] = (byte)(outBuff >> 8);
            destination[destOffset + 2] = (byte)(outBuff);
            return 3;
        }
    }

    private static final byte[] p_getAlphabet(final int options){
        if ((options & URL_SAFE) == URL_SAFE){
            return _URL_SAFE_ALPHABET;
        } else if ((options & ORDERED) == ORDERED){
            return _ORDERED_ALPHABET;
        }else{
            return _STANDARD_ALPHABET;
        }
    }

    private static final byte[] p_getDecodabet(final int options){
        if((options & URL_SAFE) == URL_SAFE){
            return _URL_SAFE_DECODABET;
        }
        if((options & ORDERED) == ORDERED){
            return _ORDERED_DECODABET;
        }
        return _STANDARD_DECODABET;
    }

}