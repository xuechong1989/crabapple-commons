package xc.crabapple.core.file.office;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.PicturesManager;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hwpf.usermodel.PictureType;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.ooxml.extractor.POIXMLTextExtractor;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xslf.extractor.XSLFExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.w3c.dom.Document;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.ListUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.url.UrlUtil;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 2017-01-01 1.0.0
 * 2024-05-21 poi升级到5.2.5版本后API有变化
 * 2024-09-22 poi升级到5.3.0
 * 办公文档工具类
 */
public final class OfficeUtil {

    /**
     * 2020-08-06 6.4.0
     * 2024-12-04 代码重构
     * 指定文件名或路径是否为office格式
     * @return
     */
    public static final boolean isOffice(final String nameOrUrl) {
        final boolean flag = FileUtil.isOffice(nameOrUrl);
        return flag;
    }

    /**
     * 2020-08-19 6.5.0
     * 2024-12-04 代码重构
     * 指定文件格式是否为office格式
     * @param fileType 文件类型（不带点的后缀），如docx
     */
    public static final boolean isOfficeByFileType(final String fileType) {
        final boolean flag = FileUtil.isOfficeByFileType(fileType);
        return flag;
    }

    /**
     * 2017-01-01 1.0.0
     * 2024-12-04 代码重构
     */
    public static final boolean isWord(final String nameOrUrl) {
        String fileType = FileUtil.getFileType(nameOrUrl);
        fileType = StringUtil.toLowerCase(fileType);
        final boolean flag = isWordByFileType(fileType);
        return flag;
    }

    /**
     * 2020-08-19 6.5.0
     * 2024-12-04 代码重构
     * 是否为word类型的文件
     * @param fileType 文件类型（不带点的后缀），如doc
     */
    public static final boolean isWordByFileType(final String fileType) {
        String fileTypeLocal = StringUtil.toLowerCase(fileType);
        if(StringUtil.equalsOfOne(fileTypeLocal, "doc", "docx")){
            return true;
        }
        return false;
    }

    /**
     * 2017-01-01 1.0.0
     * 2024-12-04 代码重构
     */
    public static final boolean isExcel(final String nameOrUrl) {
        final boolean flag = isExcelByFileType(nameOrUrl);
        return flag;
    }

    /**
     * 2020-08-19 6.5.0
     * 2024-12-04 代码重构
     * 是否为excel类型的文件
     * @param fileType 文件类型（不带点的后缀），如xls
     */
    public static final boolean isExcelByFileType(final String fileType) {
        String fileTypeLocal = StringUtil.toLowerCase(fileType);
        if(StringUtil.equalsOfOne(fileTypeLocal, "xls", "xlsx")){
            return true;
        }
        return false;
    }

    /**
     * 2017-01-01 1.0.0
     * 2024-12-04 代码重构
     */
    public static final boolean isPpt(final String nameOrUrl) {
        final boolean flag = isPptByFileType(nameOrUrl);
        return flag;
    }

    /**
     * 2020-08-19 6.5.0
     * 2024-12-04 代码重构
     * 是否为ppt类型的文件
     * @param fileType 文件类型（不带点的后缀），如ppt
     */
    public static final boolean isPptByFileType(final String fileType) {
        String fileTypeLocal = StringUtil.toLowerCase(fileType);
        if(StringUtil.equalsOfOne(fileTypeLocal, "ppt", "pptx")){
            return true;
        }
        return false;
    }

    /**
     * 2017-01-01 1.0.0
     *
     * @param url
     */
    public static final String readOffice(final String url) {
        if (isWord(url)) {
            String content = readWord(url);
            return content;
        }
        if (isPpt(url)) {
            String content = readPpt(url);
            return content;
        }
        return "";
    }

    /**
     * 2020-09-13 6.6.0
     */
    public static final List<String> readOfficeReturnList(final String url) {
        String txt = readOffice(url);
        String[] array = StringUtil.split(txt, "\n");
        List<String> list = ListUtil.array2list(array);
        return list;
    }

    /**
     * 2017-01-01 1.0.0
     */
    public static final String readWord(final String url) {
        String type = FileUtil.getFileType(url);
        if ("doc".equals(type)) {
            String result = readWord2003(url);
            return result;
        }
        if ("docx".equals(type)) {
            String result = readWord2007(url);
            return result;
        }
        return "";
    }

    /**
     * 2019-12-16 3.1.1
     *
     * @param url
     * @return
     */
    public static final String readWord2003(final String url) {
        InputStream inputStream = null;
        WordExtractor wordExtractor = null;
        try {
            inputStream = new FileInputStream(url);
            wordExtractor = new WordExtractor(inputStream);
            String result = wordExtractor.getText();
            return result;
        }catch(Throwable throwable){
            LogUtil.error("OfficeUtil_readWord2003_t1", throwable, url);
            return "";
        } finally {
            IOUtil.close(wordExtractor, inputStream);
        }
    }

    /**
     * 2017-01-01 1.0.0
     *
     * @param url
     * @return
     */
    public static final String readWord2007(final String url) {
        OPCPackage opcPackage = null;
        POIXMLTextExtractor poixmlTextExtractor = null;
        try {
            opcPackage = POIXMLDocument.openPackage(url);
            poixmlTextExtractor = new XWPFWordExtractor(opcPackage);
            String result = poixmlTextExtractor.getText();
            return result;
        }catch(Throwable throwable){
            LogUtil.error("OfficeUtil_readWord2007_t1", throwable, url);
            return "";
        } finally {
            IOUtil.close(poixmlTextExtractor, opcPackage);
        }
    }

    /**
     * 2019-12-17 3.1.1
     * 读取word文件，获取html格式文本内容
     *
     * @param url
     */
    public static final String readWordwithHtmlText(final String url) {
        String type = FileUtil.getFileType(url);
        type = StringUtil.toLowerCase(type);
        if ("doc".equals(type)) {
            String result = readWord2003withHtmlText(url);
            return result;
        }
        if ("docx".equals(type)) {
            LogUtil.error("OfficeUtil_readWordwithHtmlText_1", "不支持word2007及以上版本", url);
            return "";
        }
        return "";
    }

    /**
     * 2019-12-17
     * 读取word2003文件，获取html格式文本内容
     *
     * @param url doc格式原文件
     */
    public static final String readWord2003withHtmlText(final String url) {
        InputStream inputStream = null;
        HWPFDocument wordDocument = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            inputStream = new FileInputStream(url);
            wordDocument = new HWPFDocument(inputStream);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            WordToHtmlConverter wordToHtmlConverter = new WordToHtmlConverter(document);
            // 转化器重写内部方法
            wordToHtmlConverter.setPicturesManager(new PicturesManager() {
                public String savePicture(byte[] content, PictureType pictureType, String suggestedName, float widthInches, float heightInches) {
                    return suggestedName;
                }
            });
            // 转化器开始转化接收到的dom对象，然后从加载了输入文件中的转换器中提取DOM节点
            wordToHtmlConverter.processDocument(wordDocument);
            Document htmlDocument = wordToHtmlConverter.getDocument();
            DOMSource domSource = new DOMSource(htmlDocument);
            byteArrayOutputStream = new ByteArrayOutputStream();
            // 输出流的源头
            StreamResult streamResult = new StreamResult(byteArrayOutputStream);
            // 转化工厂生成序列转化器
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer serializer = transformerFactory.newTransformer();
            // 设置序列化内容格式
            serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty(OutputKeys.METHOD, "html");
            serializer.transform(domSource, streamResult);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            String result = new String(bytes, "UTF-8");
            return result;
        }catch(Throwable throwable){
            LogUtil.error("OfficeUtil_readWord2003withHtmlText_t1", throwable, url);
            return "";
        } finally {
            IOUtil.close(byteArrayOutputStream, wordDocument, inputStream);
        }
    }

    /**
     * 2019-12-17 3.1.1
     * word文件转换为html格式文件
     *
     * @param wordUrl
     * @return
     */
    public static final void word2html(final String wordUrl, final String htmlUrl) {
        String type = FileUtil.getFileType(wordUrl);
        if ("doc".equals(type)) {
            word20032html(wordUrl, htmlUrl);
            return;
        }
        if ("docx".equals(type)) {
            LogUtil.error("OfficeUtil_word2html_1", "不支持word2007及以上版本", wordUrl, htmlUrl);
            return;
        }
    }

    /*
     * 2023-12-13
     *  pdf转word 只能转换成docx格式
     **/
    public static final String pdf2word(final String pdfUrl, final String wordUrl) {
//        if (StringUtil.isBlank(pdfUrl,wordUrl)) {
//            LogUtil.error("OfficeUtil_pdf2word_e1", "pdfUrl或wordUrl为空");
//            return "error";
//        }
//        String pdfType = FileUtil.getFileType(pdfUrl);
//        pdfType = StringUtil.toLowerCase(pdfType);
//        if (!"pdf".equals(pdfType)) {
//            LogUtil.error("OfficeUtil_pdf2word_e2", "pdfUrl类型错误", pdfUrl);
//            return "error";
//        }
//        String wordType = FileUtil.getFileType(wordUrl);
//        wordType = StringUtil.toLowerCase(wordType);
//        if (!"docx".equals(wordType)) {
//            LogUtil.error("OfficeUtil_pdf2word_e3", "wordUrl类型错误", wordUrl);
//            return "error";
//        }
//        //创建一个 PdfDocument 对象
//        PdfDocument doc = new PdfDocument();
//        //加载 PDF 文件
//        doc.loadFromFile(pdfUrl);
//        //将PDF转换为Docx格式文件并保存
//        doc.saveToFile(wordUrl, FileFormat.DOCX);
//        doc.close();
//        return "success";
        //创建PDF2Word转换工具对象
        return "";
    }

    /**
     * 2019-12-17 3.1.1
     * word2003文件转换为html格式文件
     *
     * @param wordUrl
     * @param htmlUrl
     */
    public static final void word20032html(final String wordUrl, final String htmlUrl) {
        String result = readWord2003withHtmlText(wordUrl);
        FileUtil.fileWriteNew(htmlUrl, result, true);
    }

    /**
     * 2020-06-23 6.2.0
     * 注：构建的word文件无法通过本类的方法读取内容
     * html标准格式文件写入word2003，只支持后缀为doc
     *
     * @param htmlUrl html文件绝对路径
     * @param wordUrl doc文件绝对路径
     */
    public static final void html2word2003(final String htmlUrl, final String wordUrl) {
        String htmlStr = p_readHtml(htmlUrl);
        htmlStr2word2003(htmlStr, wordUrl);
    }

    /**
     * 2020-06-23 6.2.0
     *
     * @param htmlUrl
     */
    public static final String p_readHtml(final String htmlUrl) {
        if (StringUtil.isBlank(htmlUrl)) {
            LogUtil.error("OfficeUtil_readHtml_1", "htmlUrl为空");
            return "";
        }
        String type = FileUtil.getFileType(htmlUrl);
        type = StringUtil.toLowerCase(type);
        if (!"html".equals(type) && !"htm".equals(type)) {
            LogUtil.error("OfficeUtil_readHtml_2", "htmlUrl类型错误", htmlUrl);
            return "";
        }
        String content = FileUtil.fileReadText(htmlUrl);
        return content;
    }

    /**
     * 2020-06-23 6.2.0
     * 注：构建的word文件无法通过本类的方法读取内容
     * html标准格式内容写入word2003，只支持后缀为doc
     *
     * @param htmlStr 标准html文件格式的内容，该值必须由<html><body>包裹才可以！</body></html>
     * @param wordUrl doc文件绝对路径
     */
    public static final void htmlStr2word2003(final String htmlStr, final String wordUrl) {
        String htmlStrLocal = htmlStr;
        if (StringUtil.isBlank(htmlStrLocal)) {
            htmlStrLocal = "<html><body></body></html>";
        }
        if (StringUtil.isBlank(wordUrl)) {
            LogUtil.error("OfficeUtil_html2word2003_1", "wordUrl路径为空");
            return;
        }
        String type = FileUtil.getFileType(wordUrl);
        type = StringUtil.toLowerCase(type);
        if (!"doc".equals(type)) {
            LogUtil.error("OfficeUtil_html2word2003_2", "wordUrl文件类型必须为doc格式", wordUrl);
            return;
        }
        String toPath = FileUtil.getPathByUrl(wordUrl);
        FileUtil.dirMake(toPath);
        boolean deleteFlag = FileUtil.fileDelete(wordUrl);
        if (!deleteFlag) {
            LogUtil.error("OfficeUtil_html2word2003_3", "wordUrl文件更新时删除失败", wordUrl);
            return;
        }
        POIFSFileSystem poifsFileSystem = null;
        FileOutputStream fileOutputStream = null;
        ByteArrayInputStream byteArrayInputStream = null;
        try {
            //HTML内容必须被<html><body></body></html>包装
            byte[] b = htmlStrLocal.getBytes("GBK");  //设置为UTF-8乱码
            byteArrayInputStream = new ByteArrayInputStream(b);
            poifsFileSystem = new POIFSFileSystem();
            DirectoryEntry directoryEntry = poifsFileSystem.getRoot();
            directoryEntry.createDocument("WordDocument", byteArrayInputStream);
            fileOutputStream = new FileOutputStream(wordUrl);
            poifsFileSystem.writeFilesystem(fileOutputStream);
        }catch(Throwable throwable){
            LogUtil.error("OfficeUtil_html2word2003_t1", throwable, htmlStrLocal, wordUrl);
        } finally {
            IOUtil.close(fileOutputStream, poifsFileSystem, byteArrayInputStream);
        }
    }

    /**
     * 2017-01-01 1.0.0
     *
     * @param url
     * @return
     */
    public static final String readPpt(final String url) {
        String result = null;
        String fileType = FileUtil.getFileType(url);
        if ("ppt".equals(fileType)) {
            result = readPPT2003(url);
        } else if ("pptx".equals(fileType)) {
            result = readPPT2007(url);
        }
        return result;
    }

    /**
     * 2017-01-01 1.0.0
     * 2024-05-21 poi升级到5.2.5版本后API有变化
     * ppt格式
     */
    public static final String readPPT2003(final String url) {
        InputStream inputStream = null;
        XSLFExtractor xslfExtractor = null;
        try {
            inputStream = new FileInputStream(url);
            XMLSlideShow xmlSlideShow = new XMLSlideShow(inputStream);
            xslfExtractor = new XSLFExtractor(xmlSlideShow);
            String result = xslfExtractor.getText();
            return result;
        }catch(Throwable throwable){
            LogUtil.error("OfficeUtil_readPPT2003_t1", throwable, url);
            return null;
        } finally {
            IOUtil.close(xslfExtractor, inputStream);
        }
    }

    /**
     * 2017-10-18 1.0.0
     * 2024-05-21 poi升级到5.2.5版本后API有变化
     * ppt2007及以后版本
     */
    public static final String readPPT2007(final String url) {
        XMLSlideShow xmlSlideShow = null;
        XSLFExtractor xslfExtractor = null;
        try {
            InputStream inputStream = IOUtil.file2inputStream(url);
            xmlSlideShow = new XMLSlideShow(inputStream);
            xslfExtractor = new XSLFExtractor(xmlSlideShow);
            String result = xslfExtractor.getText();
            result = StringUtil.trim(result);
            return result;
        }catch(Throwable throwable){
            LogUtil.error("OfficeUtil_readPPT2007_t1", throwable, url);
            return null;
        } finally {
            IOUtil.close(xslfExtractor, xmlSlideShow);
        }
    }

    /**
     * 2024-04-13 返回包含完整多sheet的完整的excel数据内容
     * 读取excel文件（支持xls和xlsx）
     */
    public static final Map<String, List<List<String>>> readExcelReturnMap(final String url){
        List<List<List<String>>> list = readExcelReturnList(url);
        if(list == null){
            return null;
        }
        //
        Map<String, List<List<String>>> map = new HashMap<>();
        for(int i = 0; i < list.size(); i++){
            final String key = "sheet_" + i;
            List<List<String>> sheetList = list.get(i);
            map.put(key, sheetList);
        }
        list.clear();
        return map;
    }

    /**
     * 2017-10-18 1.0.0
     * 2020-03-03 5.0.0
     * 2024-04-20 返回包含完整多sheet的完整的excel数据内容
     * 2024-12-30 确保每行的列数相同
     * 读取excel文件（支持xls和xlsx）
     */
    public static final List<List<List<String>>> readExcelReturnList(final String excelUrl){
        String type = FileUtil.getFileType(excelUrl);
        type = StringUtil.toLowerCase(type);
        InputStream inputStream = null;
        try {
            inputStream = IOUtil.file2inputStream(excelUrl);
            List<List<List<String>>> list = readExcelReturnList(inputStream, type);
            return list;
        }catch(Throwable throwable){
            LogUtil.error("OfficeUtil_readExcelReturnList_t1", throwable, excelUrl);
            return null;
        } finally {
            IOUtil.close(inputStream);
        }
    }

    /**
     * 2017-10-18 1.0.0
     * 2020-03-03 5.0.0
     * 2024-04-20 返回包含完整多sheet的完整的excel数据内容
     * 2024-12-30 确保每行的列数相同
     * 2024-12-31 提取输入流作为参数
     * 读取excel文件（支持xls和xlsx）
     */
    public static final List<List<List<String>>> readExcelReturnList(final InputStream inputStream, final String type){
        String typeLocal = StringUtil.toLowerCase(type);
        //
        if("xls".equals(typeLocal)){
            List<List<List<String>>> list = new ArrayList<>();
            HSSFWorkbook hssfWorkbook = null;
            try{
                hssfWorkbook = new HSSFWorkbook(inputStream);
                for (int numSheets = 0; numSheets < hssfWorkbook.getNumberOfSheets(); numSheets++) {
                    if (hssfWorkbook.getSheetAt(numSheets) == null) {
                        continue;
                    }
                    List<List<String>> sheetList = new ArrayList<>();
                    list.add(sheetList);
                    Sheet aSheet = hssfWorkbook.getSheetAt(numSheets);  //获得一个sheet
                    int columnCountMax = 0;
                    for (int rowNumOfSheet = 0; rowNumOfSheet <= aSheet.getLastRowNum(); rowNumOfSheet++) {
                        Row row = aSheet.getRow(rowNumOfSheet);
                        if (row == null) {
                            continue;
                        }
                        List<String> rowList = new ArrayList<>();
                        sheetList.add(rowList);
                        Row aRow = aSheet.getRow(rowNumOfSheet);  //获得一个行
                        int num = aRow.getLastCellNum();
                        for (int cellNumOfRow = 0; cellNumOfRow < num; cellNumOfRow++) {
                            Cell aCell = aRow.getCell(cellNumOfRow);  //获得列值
                            String value = excelConvertCell(aCell);
                            if (StringUtil.isBlank(value)) {
                                value = "";
                            }
                            rowList.add(value);
                        }
                        int rowSize = rowList.size();
                        columnCountMax = Math.max(columnCountMax, rowSize);
                    }
                    for(int i = 0; i < sheetList.size(); i++){
                        List<String> rowList = sheetList.get(i);
                        int rowSize = rowList.size();
                        if(columnCountMax == rowSize){
                            continue;
                        }
                        int cha = columnCountMax - rowSize;
                        for(int j = 0; j < cha; j++){
                            rowList.add("");
                        }
                    }
                }
                return list;
            }catch(Throwable throwable){
                LogUtil.error("OfficeUtil_readExcelReturnList_t2", throwable);
                return null;
            } finally {
                IOUtil.close(hssfWorkbook, inputStream);
            }
        }
        //
        if("xlsx".equals(typeLocal)){
            List<List<List<String>>> list = new ArrayList<>();
            XSSFWorkbook xssfWorkbook = null;
            try{
                xssfWorkbook = new XSSFWorkbook(inputStream);
                for (int numSheets = 0; numSheets < xssfWorkbook.getNumberOfSheets(); numSheets++) {
                    if (xssfWorkbook.getSheetAt(numSheets) == null) {
                        continue;
                    }
                    List<List<String>> sheetList = new ArrayList<>();
                    list.add(sheetList);
                    Sheet aSheet = xssfWorkbook.getSheetAt(numSheets);  //获得一个sheet
                    int columnCountMax = 0;
                    for (int rowNumOfSheet = 0; rowNumOfSheet <= aSheet.getLastRowNum(); rowNumOfSheet++) {
                        Row row = aSheet.getRow(rowNumOfSheet);
                        if (row == null) {
                            continue;
                        }
                        List<String> rowList = new ArrayList<>();
                        sheetList.add(rowList);
                        Row aRow = aSheet.getRow(rowNumOfSheet);  //获得一个行
                        int num = aRow.getLastCellNum();
                        for (int cellNumOfRow = 0; cellNumOfRow < num; cellNumOfRow++) {
                            Cell aCell = aRow.getCell(cellNumOfRow);  //获得列值
                            String value = excelConvertCell(aCell);
                            if (StringUtil.isBlank(value)) {
                                value = "";
                            }
                            rowList.add(value);
                        }
                        int rowSize = rowList.size();
                        columnCountMax = Math.max(columnCountMax, rowSize);
                    }
                    for(int i = 0; i < sheetList.size(); i++){
                        List<String> rowList = sheetList.get(i);
                        int rowSize = rowList.size();
                        if(columnCountMax == rowSize){
                            continue;
                        }
                        int cha = columnCountMax - rowSize;
                        for(int j = 0; j < cha; j++){
                            rowList.add("");
                        }
                    }
                }
                return list;
            }catch(Throwable throwable){
                LogUtil.error("OfficeUtil_readExcelReturnList_t3", throwable);
                return null;
            } finally {
                IOUtil.close(xssfWorkbook);
            }
        }
        //
        LogUtil.error("OfficeUtil_readExcelReturnList_100", "excelUrl参数类型错误");
        return null;
    }

    /**
     * 2024-04-20 封装为通用的标准方法
     * 创建并写入excel文件（支持xls和xlsx）
     */
    public static final boolean writeExcelFromMap(final String excelUrl, final Map<String, List<List<String>>> map){
        List<List<List<String>>> list = new ArrayList<>();
        for(int i = 0; i < 100000000; i++){
            final String key = "sheet_" + i;
            if(!map.containsKey(key)){
                break;
            }
            List<List<String>> sheetList = map.get(key);
            list.add(sheetList);
        }
        //
        final boolean flag = writeExcelFromList(excelUrl, list);
        return flag;
    }

    /**
     * 2020-03-10 5.0.0
     * 2024-04-20 封装为通用的标准方法
     * 创建并写入excel文件（支持xls和xlsx）
     */
    public static final boolean writeExcelFromList(final String excelUrl, final List<List<List<String>>> list) {
        String type = FileUtil.getFileType(excelUrl);
        type = StringUtil.toLowerCase(type);
        //
        if("xls".equals(type)){
            HSSFWorkbook hssfWorkbook = new HSSFWorkbook();  //创建工作薄
            for(int i = 0; i < list.size(); i++){
                HSSFSheet hssfSheet = hssfWorkbook.createSheet("sheet_" + i);  // 创建工作空间
                HSSFFont hssfFont = hssfWorkbook.createFont();
                hssfFont.setColor(HSSFFont.COLOR_NORMAL);
                hssfFont.setBold(true);
                hssfFont.setFontHeightInPoints((short) 16);
                HSSFCellStyle hssfCellStyle = hssfWorkbook.createCellStyle();
                hssfCellStyle.setFillForegroundColor((short) 13);  // 设置背景色
                //
                List<List<String>> sheetList = list.get(i);
                for (int j = 0; j < sheetList.size(); j++) {
                    HSSFRow hssfRow = hssfSheet.createRow(j);
                    List<String> rowList = sheetList.get(j);
                    for(int k = 0; k < rowList.size(); k++){
                        String row = rowList.get(k);
                        HSSFCell hssfCell = hssfRow.createCell(k);
                        hssfCell.setCellValue(row);
                    }
                }
            }
            //
            FileUtil.fileDelete(excelUrl);
            FileUtil.fileWriteNew(excelUrl, "", false);
            OutputStream outputStream = null;
            try {
                outputStream = FileUtil.file2outputStream(excelUrl);
                hssfWorkbook.write(outputStream);
                return true;
            }catch(Throwable throwable){
                LogUtil.error("OfficeUtil_writeExcelFromList_t1", throwable, excelUrl);
                return false;
            } finally {
                IOUtil.close(outputStream, hssfWorkbook);
            }
        }
        //
        if("xlsx".equals(type)){
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook();  //创建工作薄
            for(int i = 0; i < list.size(); i++){
                XSSFSheet xssfSheet = xssfWorkbook.createSheet("sheet_" + i);  // 创建工作空间
                XSSFFont xssfFont = xssfWorkbook.createFont();
                xssfFont.setColor(HSSFFont.COLOR_NORMAL);
                xssfFont.setBold(true);
                xssfFont.setFontHeightInPoints((short) 16);
                XSSFCellStyle xssfCellStyle = xssfWorkbook.createCellStyle();
                xssfCellStyle.setFillForegroundColor((short) 13);  // 设置背景色
                //
                List<List<String>> pageList = list.get(i);
                for (int j = 0; j < pageList.size(); j++) {
                    XSSFRow xssfRow = xssfSheet.createRow(j);
                    List<String> rowList = pageList.get(j);
                    for(int k = 0; k < rowList.size(); k++){
                        String row = rowList.get(k);
                        XSSFCell xssfCell = xssfRow.createCell(k);
                        xssfCell.setCellValue(row);
                    }
                }
            }
            //
            FileUtil.fileDelete(excelUrl);
            FileUtil.fileWriteNew(excelUrl, "", false);
            OutputStream outputStream = null;
            try {
                outputStream = FileUtil.file2outputStream(excelUrl);
                xssfWorkbook.write(outputStream);
                return true;
            }catch(Throwable throwable){
                LogUtil.error("OfficeUtil_writeExcelFromList_t2", throwable, excelUrl);
                return false;
            } finally {
                IOUtil.close(outputStream, xssfWorkbook);
            }
        }
        //
        LogUtil.error("OfficeUtil_writeExcelFromList_100", "excelUrl参数类型错误", excelUrl);
        return false;
    }

    /**
     * 2024-04-21
     * 拆分excel文件
     * @param excelUrl 指定的本地EXCEL文件路径
     * @param pageSize 每页多少条（行）数据
     */
    public static final boolean splitExcel(final String excelUrl, final int pageSize){
        if(!FileUtil.isRealFile(excelUrl)){
            return false;
        }
        String path = UrlUtil.getPathByUrl(excelUrl);
        String name = UrlUtil.getFileName(excelUrl, false);
        String type = FileUtil.getFileType(excelUrl);
        String tmpType = StringUtil.toLowerCase(type);
        if(!StringUtil.equalsOfOne(tmpType, "xls", "xlsx")){
            return false;
        }
        if(pageSize <= 0){
            return false;
        }
        //
        List<List<List<String>>> list = readExcelReturnList(excelUrl);
        List<List<String>> sheetList = list.get(0);
        int pageSizeLocal = pageSize;
        if(pageSizeLocal > sheetList.size()){
            pageSizeLocal = sheetList.size();
        }
        List<List<List<String>>> newList = new ArrayList<>();
        List<List<String>> newSheetList = new ArrayList<>();
        for(int i = 0; i < sheetList.size(); i++){
            List<String> rowList = sheetList.get(i);
            newSheetList.add(rowList);
            if((i + 1) % pageSizeLocal == 0){
                newList.add(newSheetList);
                newSheetList = new ArrayList<>();
            }
        }
        int sheetSize = newSheetList.size();
        if(sheetSize > 0 && sheetSize < pageSizeLocal){
            newList.add(newSheetList);
        }
        for(int i = 0; i < newList.size(); i++){
            String ii = StringUtil.suplyZero((i + 1), 4);
            String newUrl = path + "/" + name + "-" + ii + "." + type;
            List<List<List<String>>> tmpList = new ArrayList<>();
            List<List<String>> sheet2List = newList.get(i);
            tmpList.add(sheet2List);
            FileUtil.fileDelete(newUrl);
            boolean flag = writeExcelFromList(newUrl, tmpList);
            if(!flag){
                return false;
            }
        }
        //
        if(newSheetList != null){
            newSheetList.clear();
        }
        if(newList != null){
            newList.clear();
        }
        if(sheetList != null){
            sheetList.clear();
        }
        if(list != null){
            list.clear();
        }
        //
        return true;
    }

    /**
     * 2017-10-18 1.0.0
     *
     * @param cell
     * @return
     */
    public static final String excelConvertCell(final Cell cell) {
        NumberFormat formater = NumberFormat.getInstance();
        formater.setGroupingUsed(false);
        if (cell == null) {
            return "";
        }
        String cellValue = "";
        // 具体数值对应关系可参见：org.apache.poi.ss.usermodel.Cell
        CellType cellType = cell.getCellType();
        switch (cellType) {
            case NUMERIC:  // 0  HSSFCell.CELL_TYPE_NUMERIC
                double dCellValue = cell.getNumericCellValue();
                cellValue = formater.format(dCellValue);
                break;
            case STRING:  // 1  HSSFCell.CELL_TYPE_STRING
                cellValue = cell.getStringCellValue();
                break;
            case FORMULA:  // 2  HSSFCell.CELL_TYPE_FORMULA
                cellValue = "";
                break;
            case BLANK:  // 3  HSSFCell.CELL_TYPE_BLANK
                cellValue = cell.getStringCellValue();
                break;
            case BOOLEAN:  // 4  HSSFCell.CELL_TYPE_BOOLEAN
                boolean bCellValue = cell.getBooleanCellValue();
                Boolean b2CellValue = Boolean.valueOf(bCellValue);
                cellValue = b2CellValue.toString();
                break;
            case ERROR:  // 5  HSSFCell.CELL_TYPE_ERROR
                byte byteCellValue = cell.getErrorCellValue();
                cellValue = String.valueOf(byteCellValue);
                break;
            default:
                cellValue = "";
        }
        if (cellValue == null) {
            cellValue = "";
        }
        cellValue = StringUtil.trim(cellValue);
        return cellValue;
    }

    /**
     * 2023-09-28
     * 创建一个word文件
     *
     * @param url
     * @param list
     */
    public static final void createWord(final String url, final List<String> list) {
        XWPFDocument document = null;
        FileOutputStream out = null;
        try {
            document = new XWPFDocument();
            out = new FileOutputStream(url);
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleParagraphRun = titleParagraph.createRun();
            titleParagraphRun.setText("我是标题");
            titleParagraphRun.setColor("000000");
            titleParagraphRun.setFontSize(20);
            //
            if (!CollectionUtil.isBlank(list)) {
                for (int i = 0; i < list.size(); i++) {
                    final String content = list.get(i);
                    XWPFParagraph firstParagraph = document.createParagraph();
                    XWPFRun run = firstParagraph.createRun();
                    run.setText(content);
                    run.setColor("FFFF00");
                    run.setFontSize(16);
                    CTShd cTShd = run.getCTR().addNewRPr().addNewShd();
                    cTShd.setVal(STShd.CLEAR);
                    cTShd.setFill("000000");
                    //
                    XWPFRun run2 = firstParagraph.createRun();
                    run2.setText("hello" + i);
                    run2.setColor("000000");
                    run2.setFontSize(16);
                    XWPFParagraph paragraph1 = document.createParagraph();
                    XWPFRun paragraphRun1 = paragraph1.createRun();
                    paragraphRun1.setText("\r");
                }
            }
            //
            if(false){
//                CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
//                XWPFHeaderFooterPolicy policy = new XWPFHeaderFooterPolicy(document, sectPr);
//                // 添加页眉
//                CTP ctpHeader = (CTP) CTP.Factory.newInstance();
//                CTR ctrHeader = ctpHeader.addNewR();
//                CTText ctHeader = ctrHeader.addNewT();
//                String headerText = "我是页眉";
//                ctHeader.setStringValue(headerText);
//                XWPFParagraph headerParagraph = new XWPFParagraph(ctpHeader, document);
//                headerParagraph.setAlignment(ParagraphAlignment.RIGHT);
//                XWPFParagraph[] parsHeader = new XWPFParagraph[1];
//                parsHeader[0] = headerParagraph;
//                policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT, parsHeader);
//                // 添加页脚
//                CTP ctpFooter = (CTP) CTP.Factory.newInstance();
//                CTR ctrFooter = ctpFooter.addNewR();
//                CTText ctFooter = ctrFooter.addNewT();
//                String footerText = "我是页脚";
//                ctFooter.setStringValue(footerText);
//                XWPFParagraph footerParagraph = new XWPFParagraph(ctpFooter, document);
//                headerParagraph.setAlignment(ParagraphAlignment.CENTER);
//                XWPFParagraph[] parsFooter = new XWPFParagraph[1];
//                parsFooter[0] = footerParagraph;
//                policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT, parsFooter);
            }
            document.write(out);
        }catch(Throwable throwable){
            LogUtil.error("OfficeUtil_createWord_t1", throwable, url);
        }finally {
            IOUtil.close(out, document);
        }
    }

    /**
     * 2020-03-28 5.0.0
     * 文本生成word表格
     *
     * @param list       源文件内容
     * @param index
     * @param startIndex
     * @param endIndex
     */
    public static final void createWord(final List<String> list, final int index, final int startIndex, final int endIndex) {
        String prefix = "英语词频表_" + StringUtil.suplyZero(index, 3);
        String aaa = StringUtil.suplyZero(startIndex + 1, 5);
        String bbb = StringUtil.suplyZero(endIndex, 5);
        String urlTo = "e:/app/" + prefix + "_" + aaa + "-" + bbb + ".docx";
        //生成并写入doc文件前，要提前生成空doc文件
        //FileUtil.copy("E:/app/000.docx", urlTo);  //000.docx是模板文件，内容是左对齐的5行及以上的空行
        File file = new File(urlTo);
        FileInputStream fileInputStream = null;
        XWPFDocument xwp = null;
        FileOutputStream fileOutputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            xwp = new XWPFDocument(fileInputStream);
            XWPFParagraph paragraph = xwp.getParagraphs().get(0);
            paragraph.removeRun(0);
            paragraph.insertNewRun(0).setText("");
            paragraph = xwp.getParagraphs().get(1);
            paragraph.insertNewRun(0).setText("aaa");
            paragraph.setAlignment(ParagraphAlignment.CENTER);
            Map<String, String> map = new HashMap<>();
            //
            for (int i = startIndex; i < endIndex; i++) {
                String line = list.get(i);
                line = StringUtil.getPureStr(line);
                line = StringUtil.trim(line);
                String word = StringUtil.split(line, " ## ")[0];
                String yinbiao = StringUtil.split(line, " ## ")[1];
                String content = map.get(word);
                if (StringUtil.isBlank(content)) {
                    content = "";
                }
                XWPFTable tab = xwp.createTable(6, 1);
                //tab.setCellMargins(50, 0, 50, 2500);  // top, left, bottom, right
                tab.getRow(0).getCell(0).setText("★★★★★  " + (i + 1) + "  " + word + "  " + yinbiao + "  -xxx");
                tab.getRow(1).getCell(0).setText(content);
                tab.getRow(2).getCell(0).setText("[syn]  ");
                tab.getRow(3).getCell(0).setText("[opp]  ");
                tab.getRow(4).getCell(0).setText("[par]  ");
                tab.getRow(5).getCell(0).setText("[ass]  ");
                xwp.insertTable(0, tab);
            }
            //
            fileOutputStream = new FileOutputStream(urlTo);
            xwp.write(fileOutputStream);
        }catch(Throwable throwable){
            LogUtil.error("OfficeUtil_createWord_t2", throwable, urlTo);
        }finally {
            IOUtil.close(fileOutputStream, xwp, fileInputStream);
        }
    }

}
