package xc.crabapple.core.file.pdf.check;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import xc.crabapple.core.collection.ArrayUtil;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.Base64Util;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 2025-09-23
 * pdf文件内的元素解析与检测
 */
public final class PdfCheckUtil {

    /**
     * 2025-09-23
     * @param pdfBase64
     * @param keywords
     * @return
     */
    public static final int checkKeywordByBase64(final String pdfBase64, final String keywords) {
        LogUtil.info("PdfCheckUtil_checkKeywordByBase64_1", "即将检测PDF文件", pdfBase64, keywords);
        //
        byte[] pdfBytes = Base64Util.decodeStr2bytes(pdfBase64);
        int result = checkKeywordByBytes(pdfBytes, keywords);
        return result;
    }

    /**
     * 2025-09-23
     * @param pdfUrl
     * @param keywords
     * @return
     */
    public static final int checkKeywordByByUrl(final String pdfUrl, final String keywords) {
        LogUtil.info("PdfCheckUtil_checkKeywordByByUrl_1", "即将检测PDF文件", pdfUrl, keywords);
        //
        String pdfUrlLocal = UrlUtil.getStandardPath(pdfUrl);
        byte[] pdfBytes = FileUtil.file2bytes(pdfUrlLocal);
        int result = checkKeywordByBytes(pdfBytes, keywords);
        return result;
    }

    /**
     * 2025-09-23
     * @param pdfBytes
     * @param keywords
     * @return
     */
    public static final int checkKeywordByBytes(final byte[] pdfBytes, final String keywords) {
        if (CollectionUtil.isBlank(pdfBytes)) {
            LogUtil.error("PdfCheckUtil_checkKeywordByBytes_18", "PDF为空", keywords);
            return 0;
        }
        String keywordsLocal = StringUtil.getStandardStr(keywords);
        if (StringUtil.isBlank(keywordsLocal)) {
            LogUtil.error("PdfCheckUtil_checkKeywordByBytes_12", "关键字列表为空", keywordsLocal);
            return 0;
        }
        //
        final String[] keywordArray = removeRepeatKeywords(keywordsLocal);
        final int keywordLength = keywordArray.length;
        keywordsLocal = ArrayUtil.toString(keywordArray);
        PDDocument pdDocument = null;
        PdfReader itextReader = null;
        try {
            pdDocument = Loader.loadPDF(pdfBytes);
            // 获取关键字坐标
            Map<Integer, List<float[]>> map = new HashMap<>();
            LogUtil.info("PdfCheckUtil_checkKeywordByBytes_1", "关键字列表【" + keywordsLocal + "】", "关键字数量【" + keywordLength + "】");
            for (int i = 0; i < keywordLength; i++) {
                String keyword = keywordArray[i];
                keyword = StringUtil.trim(keyword);
                BoxKeyPosition boxKeyPosition = new BoxKeyPosition(keyword);
                List<float[]> list = boxKeyPosition.getPosition(pdDocument);
                if (CollectionUtil.isBlank(list)) {
                    LogUtil.info("PdfCheckUtil_checkKeywordByBytes_5", "PDFBox未找到关键字【" + keyword + "】，尝试iText降级提取");
                    if (itextReader == null) {
                        itextReader = new PdfReader(new ByteArrayInputStream(pdfBytes));
                    }
                    list = checkKeywordByItext(itextReader, keyword);
                }
                if (CollectionUtil.isBlank(list)) {
                    LogUtil.error("PdfCheckUtil_checkKeywordByBytes_4", "关键字【" + keyword + "】没有找到", "关键字列表【" + keywordsLocal + "】", "关键字数量【" + keywordLength + "】");
                    return 0;
                }
                map.put(i, list);
                int size = list.size();
                StringBuilder stringBuilder = new StringBuilder();
                for (int j = 0; j < size; j++) {
                    stringBuilder.append("[");
                    float[] floatArray = list.get(j);
                    float fx = floatArray[0];
                    float fy = floatArray[1];
                    float fp = floatArray[2];
                    int fpInt = (int) fp;
                    stringBuilder.append("第").append(fpInt).append("页");
                    stringBuilder.append(", ").append(fx).append(", ").append(fy);
                    if (j < size - 1) {
                        stringBuilder.append("], ");
                        continue;
                    }
                    stringBuilder.append("]");
                }
                String details = stringBuilder.toString();
                LogUtil.info("PdfCheckUtil_checkKeywordByBytes_2", "关键字【" + keyword + "】匹配" + size + "处", details);
            }
            List<float[]> floatList = new ArrayList<>();
            int total = 0;
            for (Integer i : map.keySet()) {
                List<float[]> list = map.get(i);
                float[] idx = list.get(0);
                floatList.add(idx);
                total += list.size();
            }
            int size = floatList.size();
            if (size > 0) {
                LogUtil.info("PdfCheckUtil_checkKeywordByBytes_53", "共匹配" + total + "处关键字", "关键字列表【" + keywordsLocal + "】", "关键字数量【" + keywordLength + "】", size);
                return size;
            }
            // 未找到关键词
            floatList.clear();
            Iterator iterator = map.keySet().iterator();
            while (iterator.hasNext()) {
                Integer i = (Integer) iterator.next();
                List<float[]> list = map.get(i);
                float[] idx = list.get(0);
                floatList.add(idx);
            }
            size = floatList.size();
            if (size > 0) {
                LogUtil.info("PdfCheckUtil_checkKeywordByBytes_5", "共匹配" + total + "处关键字", "关键字列表【" + keywordsLocal + "】", "关键字数量【" + keywordLength + "】", size);
                return size;
            }
            // 未找到关键词
            LogUtil.error("PdfCheckUtil_checkKeywordByBytes_7", "共匹配" + total + "处关键字", "关键字列表【" + keywordsLocal + "】", "关键字数量【" + keywordLength + "】", size);
            return size;
        } catch (Throwable throwable) {
            LogUtil.error("PdfCheckUtil_checkKeywordByBytes_99", throwable, "关键字列表【" + keywordsLocal + "】", "关键字数量【" + keywordLength + "】");
            return 0;
        } finally {
            IOUtil.close(pdDocument);
            if (itextReader != null) {
                itextReader.close();
            }
        }
    }

    /**
     * PDFBox提取失败时，使用iText作为降级方案提取关键字（复用已打开的PdfReader）
     * @param reader 已打开的PdfReader，由调用方负责关闭
     * @param keyword
     * @return
     */
    private static final List<float[]> checkKeywordByItext(final PdfReader reader, final String keyword) {
        try {
            int pages = reader.getNumberOfPages();
            List<float[]> result = new ArrayList<>();
            for (int i = 1; i <= pages; i++) {
                String text = PdfTextExtractor.getTextFromPage(reader, i);
                if (text != null && text.contains(keyword)) {
                    float[] pos = new float[]{0f, 0f, (float) i};
                    result.add(pos);
                    LogUtil.info("PdfCheckUtil_checkKeywordByItext_ok", "iText在第" + i + "页找到关键字【" + keyword + "】");
                }
            }
            if (CollectionUtil.isBlank(result)) {
                LogUtil.error("PdfCheckUtil_checkKeywordByItext_fail", "iText也未找到关键字【" + keyword + "】");
            }
            return result;
        } catch (Throwable throwable) {
            LogUtil.error("PdfCheckUtil_checkKeywordByItext_err", throwable, "关键字【" + keyword + "】");
            return null;
        }
    }

    /**
     * 2025-09-23
     * @param keywords
     * @return
     */
    public static final String[] removeRepeatKeywords(final String keywords) {
        if(StringUtil.isBlank(keywords)) {
            String[] array = {};
            return array;
        }
        //
        String keywordsLocal = StringUtil.trim(keywords, ";");
        String[] keywordArray = keywordsLocal.split(";");
        if(CollectionUtil.isBlank(keywordArray)) {
            String[] array = {};
            return array;
        }
        //
        keywordArray = ArrayUtil.removeRepeat(keywordArray);
        return keywordArray;
    }

}