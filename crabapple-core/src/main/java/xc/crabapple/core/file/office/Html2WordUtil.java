package xc.crabapple.core.file.office;

import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.AltChunkType;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.Base64Util;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Base64;

/**
 * Html 转 Word 工具类
 */

public final class Html2WordUtil {

    /**
     * `html` 转 `word` 【注`doc`生成的html中的图片路径中中文是被转义处理过的，再生成word时图片便看不了，需单独做处理，`docx`无此问题】 【 注：此方式会丢失一定格式 】
     *
     * @param html: html内容
     * @return base64Encoded: word文件的base64格式
     */

    public static final String html2Word(final String html) {
        String base64Encoded = null;
        ByteArrayInputStream inputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        try{
            //转换为字节数组
            byte htmlBytes[] = html.getBytes();
            inputStream = new ByteArrayInputStream(htmlBytes);
            //转换为word文件(docx)
            POIFSFileSystem poifs = new POIFSFileSystem();
            DirectoryEntry directory = poifs.getRoot();
            directory.createDocument("WordDocument", inputStream);
            byteArrayOutputStream = new ByteArrayOutputStream();
            poifs.writeFilesystem(byteArrayOutputStream);
            // 使用 Base64 编码器进行转换
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            base64Encoded = Base64Util.encodeBytes2str(byteArray);
            if (StringUtil.isBlank(base64Encoded)) {
                return null;
            }
            return base64Encoded;
        }catch(Throwable throwable){
            LogUtil.error("Html2WordUtil_html2Word_t96", throwable);
            return null;
        } finally {
            IOUtil.close(inputStream, byteArrayOutputStream);
        }
    }

    /*
    2024-03-01
    通过docx4j实现html转docx
     */
    public static final String html2word(final String htmlContent, final String fileName) {
        String base64Encoded = null;
        try{
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
            MainDocumentPart mdp = wordMLPackage.getMainDocumentPart();
            mdp.addAltChunk(AltChunkType.Html, htmlContent.getBytes());
            wordMLPackage.save(new File(fileName));
        }catch(Throwable throwable){
            LogUtil.error("Html2WordUtil_html2Word_t73", throwable, "转换失败");
        }
        FileInputStream fis = null;
        ByteArrayOutputStream baos = null;
        try{
            fis = new FileInputStream(fileName);
            baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int bytesRead;
            // 从FileInputStream读取到buffer，然后写入到ByteArrayOutputStream
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] byteArray = baos.toByteArray();
            base64Encoded = Base64Util.encodeBytes2str(byteArray);
            FileUtil.fileDelete(fileName);
        }catch(Throwable throwable){
            LogUtil.error("Html2WordUtil_html2Word_t87", throwable, "获取base64失败");
        } finally {
            IOUtil.close(fis, baos);
        }
        return base64Encoded;
    }

    public static final void html2word2(final String htmlPath, final String wordPath) {
        String commonsCssStr = FileUtil.fileReadText(htmlPath);
        try{
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
            MainDocumentPart mdp = wordMLPackage.getMainDocumentPart();
            mdp.addAltChunk(AltChunkType.Html, commonsCssStr.getBytes());
            wordMLPackage.save(new File(wordPath));
        }catch(Throwable throwable){
            LogUtil.error("Html2WordUtil_html2Word_t87", throwable, "获取base64失败");
        }
    }

}
