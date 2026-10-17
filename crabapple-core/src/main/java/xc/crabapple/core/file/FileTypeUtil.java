package xc.crabapple.core.file;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * 2026-09-04 档案整编不转换模板附件无法预览下载
 * 出口侧文件格式分级与魔数校验单一来源：预览预检接口与正式预览接口统一调用，禁止各自维护白名单（防止判定不同步回归）
 */
public class FileTypeUtil {

    /**
     * 预览分级：PDF，走既有强校验链路后内联输出
     */
    public static final int TYPE_PDF = 1;
    /**
     * 预览分级：A类，图片/文本/音视频，魔数校验通过后内联输出
     */
    public static final int TYPE_INLINE = 2;
    /**
     * 预览分级：B类，office文档，LibreOffice转PDF后内联输出转换产物，原始文件不直接内联
     */
    public static final int TYPE_CONVERT = 3;
    /**
     * 预览分级：C类，默认拒绝内联（含一切未识别格式），仅允许下载
     */
    public static final int TYPE_REJECT = 4;

    /**
     * A类内联白名单：仅允许crabapple的MimeTypeUtil已有MIME映射的格式入列（webp/avif等缺映射格式禁止入列，归C类）
     */
    private static final String[] INLINE_TYPES = {"jpg", "jpeg", "png", "gif", "bmp", "tif", "tiff", "txt", "mp4", "mp3", "wav"};

    /**
     * B类转换白名单：LibreOffice可转换的office格式
     */
    private static final String[] CONVERT_TYPES = {"doc", "docx", "xls", "xlsx", "ppt", "pptx"};

    /**
     * mp3的ID3标签头魔数
     */
    private static final int[] MAGIC_MP3_ID3 = {0x49, 0x44, 0x33};

    /**
     * jpg/jpeg魔数
     */
    private static final int[] MAGIC_JPG = {0xFF, 0xD8, 0xFF};

    /**
     * png魔数
     */
    private static final int[] MAGIC_PNG = {0x89, 0x50, 0x4E, 0x47};

    /**
     * gif魔数
     */
    private static final int[] MAGIC_GIF = {0x47, 0x49, 0x46, 0x38};

    /**
     * bmp魔数
     */
    private static final int[] MAGIC_BMP = {0x42, 0x4D};

    /**
     * tif/tiff小端魔数（II + 2A 00）
     */
    private static final int[] MAGIC_TIF_LE = {0x49, 0x49, 0x2A, 0x00};

    /**
     * tif/tiff大端魔数（MM + 00 2A）
     */
    private static final int[] MAGIC_TIF_BE = {0x4D, 0x4D, 0x00, 0x2A};

    /**
     * mp4的ftyp盒类型魔数（位于文件头偏移4字节处）
     */
    private static final int[] MAGIC_MP4_FTYP = {0x66, 0x74, 0x79, 0x70};

    /**
     * wav的RIFF头魔数
     */
    private static final int[] MAGIC_WAV_RIFF = {0x52, 0x49, 0x46, 0x46};

    /**
     * wav的WAVE标识魔数（位于文件头偏移8字节处）
     */
    private static final int[] MAGIC_WAV_WAVE = {0x57, 0x41, 0x56, 0x45};

    /**
     * 2026-09-04 档案整编不转换模板附件无法预览下载
     * 按扩展名判定预览分级（大小写不敏感；null/空白/未识别一律TYPE_REJECT默认拒绝）
     * @param ext 文件扩展名
     * @return TYPE_PDF/TYPE_INLINE/TYPE_CONVERT/TYPE_REJECT
     */
    public static int getPreviewType(final String ext) {
        if (StringUtil.isBlank(ext)) {
            return TYPE_REJECT;
        }
        String extLower = ext.toLowerCase();
        if ("pdf".equals(extLower)) {
            return TYPE_PDF;
        }
        for (int i = 0; i < INLINE_TYPES.length; i++) {
            if (INLINE_TYPES[i].equals(extLower)) {
                return TYPE_INLINE;
            }
        }
        for (int i = 0; i < CONVERT_TYPES.length; i++) {
            if (CONVERT_TYPES[i].equals(extLower)) {
                return TYPE_CONVERT;
            }
        }
        return TYPE_REJECT;
    }

    /**
     * 2026-09-04 档案整编不转换模板附件无法预览下载
     * 文件头魔数校验（只读前16字节，防改名伪造）：txt无固定魔数且text/plain无脚本执行面直接放行；读取异常一律按不匹配处理（默认拒绝）
     * @param ext 文件扩展名
     * @param fileUrl 本地文件绝对路径
     * @return true-内容与扩展名相符 false-不符或无法判定
     */
    public static boolean isMagicNumberMatch(final String ext, final String fileUrl) {
        if (StringUtil.isBlank(ext) || StringUtil.isBlank(fileUrl)) {
            return false;
        }
        if (!FileUtil.exists(fileUrl)) {
            return false;
        }
        String extLower = ext.toLowerCase();
        if ("txt".equals(extLower)) {
            return true;
        }
        byte[] headBytes = new byte[16];
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(fileUrl);
            int readCount = inputStream.read(headBytes);
            if (readCount <= 0) {
                return false;
            }
        } catch (Throwable throwable) {
            LogUtil.error("FileTypeUtil_isMagicNumberMatch_e1", throwable, ext, fileUrl);
            return false;
        } finally {
            IOUtil.close(inputStream);
        }
        return p_isExtMatchHead(extLower, headBytes);
    }

    /**
     * 2026-09-04 档案整编不转换模板附件无法预览下载
     * 净化扩展名用于前端弹窗文案：仅允许1-10位字母数字，其余（含HTML片段）一律显示为"未知类型"，防mini.alert的HTML注入
     * @param ext 原始扩展名
     * @return 净化后可安全展示的扩展名文本
     */
    public static String sanitizeExtForMessage(final String ext) {
        if (StringUtil.isBlank(ext)) {
            return "未知类型";
        }
        boolean isSafe = ext.matches("[a-zA-Z0-9]{1,10}");
        if (!isSafe) {
            return "未知类型";
        }
        return ext.toLowerCase();
    }

    /**
     * 按扩展名分派具体魔数比对
     */
    private static boolean p_isExtMatchHead(final String extLower, final byte[] headBytes) {
        if ("jpg".equals(extLower) || "jpeg".equals(extLower)) {
            return p_isHeadMatch(headBytes, 0, MAGIC_JPG);
        }
        if ("png".equals(extLower)) {
            return p_isHeadMatch(headBytes, 0, MAGIC_PNG);
        }
        if ("gif".equals(extLower)) {
            return p_isHeadMatch(headBytes, 0, MAGIC_GIF);
        }
        if ("bmp".equals(extLower)) {
            return p_isHeadMatch(headBytes, 0, MAGIC_BMP);
        }
        if ("tif".equals(extLower) || "tiff".equals(extLower)) {
            boolean isLittleEndian = p_isHeadMatch(headBytes, 0, MAGIC_TIF_LE);
            boolean isBigEndian = p_isHeadMatch(headBytes, 0, MAGIC_TIF_BE);
            boolean isTifMatch = isLittleEndian || isBigEndian;
            return isTifMatch;
        }
        if ("mp4".equals(extLower)) {
            return p_isHeadMatch(headBytes, 4, MAGIC_MP4_FTYP);
        }
        if ("wav".equals(extLower)) {
            boolean isRiff = p_isHeadMatch(headBytes, 0, MAGIC_WAV_RIFF);
            boolean isWave = p_isHeadMatch(headBytes, 8, MAGIC_WAV_WAVE);
            boolean isWavMatch = isRiff && isWave;
            return isWavMatch;
        }
        if ("mp3".equals(extLower)) {
            boolean isId3 = p_isHeadMatch(headBytes, 0, MAGIC_MP3_ID3);
            if (isId3) {
                return true;
            }
            // 无ID3标签时按MPEG帧同步字判定（FF Ex）
            if (headBytes.length >= 2) {
                int firstByte = headBytes[0] & 0xFF;
                int secondByte = headBytes[1] & 0xFF;
                boolean isFrameSync = firstByte == 0xFF && (secondByte & 0xE0) == 0xE0;
                return isFrameSync;
            }
            return false;
        }
        return false;
    }

    /**
     * 比对文件头指定偏移起的字节序列
     */
    private static boolean p_isHeadMatch(final byte[] headBytes, final int offset, final int[] magic) {
        if (headBytes.length < offset + magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            int byteValue = headBytes[offset + i] & 0xFF;
            if (byteValue != magic[i]) {
                return false;
            }
        }
        return true;
    }

}
