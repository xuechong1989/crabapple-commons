package xc.crabapple.core.file.pdf.check;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 关键字检索
 */
public class BoxKeyPosition extends PDFTextStripper {

    private char[] key;
    private String keyStr;
    private byte[] p_bytes;
    private List<float[]> p_pageList = new ArrayList<>();
    private final LinkedList<TextPosition> p_buffer = new LinkedList<>();
    private final LinkedList<int[]> p_bufferCharRange = new LinkedList<>();

    /**
     * 构造方法
     * @param keyWords 需要检索的关键字
     * @param bytes 所检索的PDF文档byte[]流
     * @throws IOException
     */
    public BoxKeyPosition(final String keyWords, final byte[] bytes) {
        super();
        super.setSortByPosition(true);
        p_bytes = bytes;
        this.keyStr = keyWords;
        char[] key = new char[keyWords.length()];
        for (int i = 0; i < keyWords.length(); i++) {
            key[i] = keyWords.charAt(i);
        }
        this.key = key;
    }

    /**
     * 构造方法（复用已加载的PDDocument，避免重复解析PDF）
     * @param keyWords 需要检索的关键字
     */
    public BoxKeyPosition(final String keyWords) {
        super();
        super.setSortByPosition(true);
        this.keyStr = keyWords;
        char[] key = new char[keyWords.length()];
        for (int i = 0; i < keyWords.length(); i++) {
            key[i] = keyWords.charAt(i);
        }
        this.key = key;
    }

    /**
     * 获取构造时传入参数的具体坐标集（内部加载PDF，调用方无需管理PDDocument生命周期）
     * @return 封装关键字坐标所在的集合 ,坐标以float数组封装[0,1,2]对应[横坐标,纵坐标,页码]
     */
    public List<float[]> getPosition() {
        try {
            PDDocument doc = Loader.loadPDF(p_bytes);
            try {
                List<float[]> list = getPosition(doc);
                return list;
            } finally {
                IOUtil.close(doc);
            }
        } catch (Throwable throwable) {
            LogUtil.error("BoxKeyPosition_getPosition_t1", throwable);
            return null;
        }
    }

    /**
     * 使用外部已加载的PDDocument进行关键字检索（调用方负责关闭PDDocument）
     * @param doc 已加载的PDDocument对象
     * @return 封装关键字坐标所在的集合 ,坐标以float数组封装[0,1,2]对应[横坐标,纵坐标,页码]
     */
    public List<float[]> getPosition(final PDDocument doc) {
        List<float[]> list = new ArrayList<>();
        try {
            int pages = doc.getNumberOfPages();
            for (int i = 1; i <= pages; i++) {
                p_pageList.clear();
                p_buffer.clear();
                p_bufferCharRange.clear();
                super.setSortByPosition(true);
                super.setStartPage(i);
                super.setEndPage(i);
                Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
                try {
                    super.writeText(doc, dummy);
                } finally {
                    IOUtil.close(dummy);
                }
                flushBuffer();
                for (float[] li : p_pageList) {
                    li[2] = i;
                }
                list.addAll(p_pageList);
            }
            return list;
        } catch (Throwable throwable) {
            LogUtil.error("BoxKeyPosition_getPosition_t2", throwable);
            return null;
        }
    }

    /**
     * 将缓冲区中积累的TextPosition拼接成完整字符串，用indexOf查找关键字
     */
    private void flushBuffer() {
        if (p_buffer.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        List<TextPosition> tpList = new ArrayList<>(p_buffer);
        List<int[]> rangeList = new ArrayList<>(p_bufferCharRange);
        for (int i = 0; i < tpList.size(); i++) {
            String unicode = tpList.get(i).getUnicode();
            int start = sb.length();
            if (unicode != null) {
                sb.append(unicode);
            }
            rangeList.get(i)[0] = start;
            rangeList.get(i)[1] = sb.length();
        }
        String text = sb.toString();
        if (!text.isEmpty() && !text.contains(keyStr)) {
            LogUtil.info("BoxKeyPosition_flushBuffer_debug", "关键字【" + keyStr + "】本页提取文本片段(前200字)【" + text.substring(0, Math.min(200, text.length())) + "】");
        }
        int searchFrom = 0;
        while (true) {
            int idx = text.indexOf(keyStr, searchFrom);
            if (idx < 0) {
                break;
            }
            int tpIdx = findTpIndex(rangeList, idx);
            if (tpIdx >= 0) {
                TextPosition tp = tpList.get(tpIdx);
                float[] pos = new float[3];
                pos[0] = tp.getX() + key.length * tp.getWidth() / 2;
                pos[1] = tp.getY() - tp.getHeight();
                p_pageList.add(pos);
            }
            searchFrom = idx + 1;
        }
        p_buffer.clear();
        p_bufferCharRange.clear();
    }

    private int findTpIndex(final List<int[]> rangeList, final int charIdx) {
        for (int i = 0; i < rangeList.size(); i++) {
            if (charIdx >= rangeList.get(i)[0] && charIdx < rangeList.get(i)[1]) {
                return i;
            }
        }
        return rangeList.isEmpty() ? -1 : 0;
    }

    @Override
    protected void writeString(final String string, final List<TextPosition> textPositions) {
        for (TextPosition tp : textPositions) {
            p_buffer.add(tp);
            p_bufferCharRange.add(new int[]{0, 0});
        }
    }

    public char[] getKey() {
        return key;
    }

    public void setKey(final char[] key) {
        this.key = key;
    }

    public byte[] getP_bytes() {
        return p_bytes;
    }

    public void setP_bytes(final byte[] p_bytes) {
        this.p_bytes = p_bytes;
    }

}