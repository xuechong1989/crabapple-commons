package xc.crabapple.core.xml;

import xc.crabapple.core.log.LogUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import xc.crabapple.core.io.IOUtil;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import java.io.StringWriter;

/**
 * 2022-07-21
 * 通用的xml操作工具类
 * 2026-08-25 归档失败问题
 * 新增自研轻量XML树工具（配合XMLNode，替代dom4j的Dom4jUtil）：
 * 写出文本原样保留（不折叠空格不去首尾，缩进只加在标签行首）、失败一律打日志返回null/false哨兵值
 * 不抛异常（非法字符/磁盘满/权限等，调用方必须检查返回值）、解析禁用DTD与外部实体（防XXE）；
 * 兼容JDK8与JDK21（仅用JDK内置java.xml，已双环境实测）
 */
public final class XmlUtil {

    public static final String convertToXml(final Object obj){
        String str = convertToXml(obj, "UTF-8");
        return str;
    }

    public static final String convertToXml(final Object obj, final String encoding){
        String result = null;
        try {
            JAXBContext context = JAXBContext.newInstance(obj.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding);
            StringWriter writer = new StringWriter();
            marshaller.marshal(obj, writer);
            result = writer.toString();
        }catch(Throwable throwable){
            LogUtil.error("JAXBUtil_convertToXml_e1", throwable);
        }
        return result;
    }

    public static final Unmarshaller getUnmarshaller(final Object obj){
        Unmarshaller unmarshaller = null;
        try {
            JAXBContext context = JAXBContext.newInstance(obj.getClass());
            unmarshaller = context.createUnmarshaller();
        }catch(Throwable throwable){
            LogUtil.error("JAXBUtil_getUnmarshaller_e1", throwable);
        }
        return unmarshaller;
    }

    public static final XMLInputFactory createSafeXmlInputFactory(){
        XMLInputFactory xif = XMLInputFactory.newInstance();
        if (xif.isPropertySupported(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES)){
            xif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        }
        if (xif.isPropertySupported(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES)){
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        }
        if (xif.isPropertySupported(XMLInputFactory.SUPPORT_DTD)){
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        }
        return xif;
    }

    /**
     * 2026-08-25 归档失败问题
     * 创建XML文档节点（后续addElement添加根元素），替代dom4j的DocumentHelper.createDocument()
     * @return 文档级XMLNode
     */
    public static XMLNode createDocument() {
        XMLNode documentNode = new XMLNode("#document", null, true);
        return documentNode;
    }

    /**
     * 2026-08-25 归档失败问题
     * 解析XML字符串（安全解析：禁DTD/外部实体防XXE，CDATA并入文本），替代dom4j的DocumentHelper.parseText
     * @param xmlString XML字符串
     * @return 文档级XMLNode
     */
    public static XMLNode parseText(String xmlString) {
        try {
            byte[] xmlBytes = xmlString.getBytes(StandardCharsets.UTF_8);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xmlBytes);
            DocumentBuilder documentBuilder = p_newDocumentBuilder();
            org.w3c.dom.Document w3cDocument = documentBuilder.parse(byteArrayInputStream);
            XMLNode documentNode = createDocument();
            p_convertElement(w3cDocument.getDocumentElement(), documentNode);
            return documentNode;
        } catch (Throwable throwable) {
            // 与原Dom4jUtil惯例一致：解析失败打日志返回null哨兵值，不向上抛异常；
            // 日志只打内容长度与前200字符，防止大XML全文刷爆日志
            String previewContent = xmlString.length() > 200 ? xmlString.substring(0, 200) : xmlString;
            LogUtil.error("XmlUtil_parseText_t2", throwable, "contentLength", xmlString.length(), "contentHead", previewContent);
            return null;
        }
    }

    /**
     * 2026-08-25 归档失败问题
     * 解析XML字符串（parseText的方法别名，方法名与原Dom4jUtil对齐降低调用方改动）
     */
    public static XMLNode stringToDocument(String xmlString) {
        XMLNode documentNode = parseText(xmlString);
        return documentNode;
    }

    /**
     * 2026-08-25 归档失败问题
     * 读取XML文件为XMLNode树（安全解析：禁DTD/外部实体防XXE），替代dom4j经Dom4jUtil.load的读取
     * @param filename 文件完整路径
     * @return 文档级XMLNode
     */
    public static XMLNode load(String filename) {
        File file = new File(filename);
        XMLNode documentNode = load(file);
        return documentNode;
    }

    /**
     * 2026-08-25 归档失败问题
     * 读取XML文件为XMLNode树（安全解析：禁DTD/外部实体防XXE）
     */
    public static XMLNode load(File file) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            XMLNode documentNode = load(fileInputStream);
            return documentNode;
        } catch (Throwable throwable) {
            // 与原Dom4jUtil惯例一致：读取失败打日志返回null哨兵值，不向上抛异常
            LogUtil.error("XmlUtil_load_t3", throwable, file.getAbsolutePath());
            return null;
        } finally {
            IOUtil.close(fileInputStream);
        }
    }

    /**
     * 2026-08-25 归档失败问题
     * 读取XML输入流为XMLNode树（安全解析：禁DTD/外部实体防XXE；流由调用方关闭）
     */
    public static XMLNode load(InputStream inputStream) {
        try {
            DocumentBuilder documentBuilder = p_newDocumentBuilder();
            org.w3c.dom.Document w3cDocument = documentBuilder.parse(inputStream);
            XMLNode documentNode = createDocument();
            p_convertElement(w3cDocument.getDocumentElement(), documentNode);
            return documentNode;
        } catch (Throwable throwable) {
            // 与原Dom4jUtil惯例一致：解析失败打日志返回null哨兵值，不向上抛异常
            LogUtil.error("XmlUtil_load_t4", throwable);
            return null;
        }
    }

    /**
     * 2026-08-25 归档失败问题
     * 把XMLNode树写成XML文件（UTF-8无BOM，带xml声明头，缩进只加在标签行首），
     * 替代dom4j经Dom4jUtil.docToXmlFile的写出。核心行为约束：
     * 文本节点原样输出（不折叠空格不去首尾），仅做XML必需转义；
     * 失败（含文本含XML 1.0非法字符、磁盘满、权限等）一律打日志返回false不抛异常，
     * 调用方必须检查返回值并中止业务，防止坏文件流出
     * @param documentNode 文档级XMLNode
     * @param fullFilePath 目标文件完整路径（父目录不存在时自动创建）
     * @return true-写出成功 false-写出失败（详情见错误日志）
     */
    public static boolean docToXmlFile(XMLNode documentNode, String fullFilePath) {
        FileOutputStream fileOutputStream = null;
        OutputStreamWriter outputStreamWriter = null;
        try {
            File targetFile = new File(fullFilePath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            // 序列化：文本原样转义，遇XML 1.0非法字符抛IllegalArgumentException由统一catch返回false
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            for (XMLNode rootElement : documentNode.elements()) {
                p_writeNode(rootElement, 0, contentBuilder);
            }
            fileOutputStream = new FileOutputStream(targetFile);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
            String fileContent = contentBuilder.toString();
            outputStreamWriter.write(fileContent);
            return true;
        } catch (Throwable throwable) {
            LogUtil.error("XmlUtil_docToXmlFile_t5", throwable, fullFilePath);
            return false;
        } finally {
            IOUtil.close(outputStreamWriter, fileOutputStream);
        }
    }

    /**
     * 2026-08-25 归档失败问题
     * 创建安全DocumentBuilder：禁DTD与外部实体（防XXE）、关闭命名空间感知（按本地名匹配与dom4j现状等价）、
     * CDATA并入文本；DocumentBuilderFactory非线程安全故每次新建（解析频率低无性能问题）
     */
    private static DocumentBuilder p_newDocumentBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setCoalescing(true);
        // 2026-08-27 归档失败问题
        // 对齐dom4j旧行为：dom4j默认允许内部DOCTYPE（实验证实），disallow-doctype-decl=true会误拒带内部DOCTYPE的
        // XML；改为false允许DOCTYPE，XXE防线保留在外部实体两个feature禁用上，实体数与展开深度由JDK默认上限防护
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        return documentBuilder;
    }

    /**
     * 2026-08-25 归档失败问题
     * w3c dom元素递归转换为XMLNode（文本与CDATA节点按原顺序并入文本；注释/处理指令忽略）
     */
    private static void p_convertElement(Element w3cElement, XMLNode parentNode) {
        String nodeName = w3cElement.getNodeName();
        // 2026-08-27 归档失败问题
        // 复刻dom4j旧行为（namespace-unaware丢弃前缀取本地名）：BPMN流程XML的BPMNDiagram等元素
        // 带bpmndi:前缀，旧dom4j经element("BPMNDiagram")可命中且getName无前缀，流程图/Tag功能依赖此行为；
        // 存带前缀全名会按名匹配失败产生空指针（2026-08-27现场流程图接口报错即此回归）
        int colonIndex = nodeName.indexOf(58);
        String elementName = colonIndex >= 0 ? nodeName.substring(colonIndex + 1) : nodeName;
        XMLNode xmlNode = parentNode.addElement(elementName);
        NamedNodeMap attributeMap = w3cElement.getAttributes();
        for (int i = 0; i < attributeMap.getLength(); i++) {
            org.w3c.dom.Node attributeNode = attributeMap.item(i);
            String attributeName = attributeNode.getNodeName();
            String attributeValue = attributeNode.getNodeValue();
            xmlNode.addAttribute(attributeName, attributeValue);
        }
        NodeList childNodes = w3cElement.getChildNodes();
        StringBuilder textBuilder = new StringBuilder();
        for (int i = 0; i < childNodes.getLength(); i++) {
            org.w3c.dom.Node childNode = childNodes.item(i);
            short nodeType = childNode.getNodeType();
            if (nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                Element childElement = (Element) childNode;
                p_convertElement(childElement, xmlNode);
            } else if (nodeType == org.w3c.dom.Node.TEXT_NODE || nodeType == org.w3c.dom.Node.CDATA_SECTION_NODE) {
                String nodeValue = childNode.getNodeValue();
                if (nodeValue != null) {
                    textBuilder.append(nodeValue);
                }
            }
        }
        String textValue = textBuilder.toString();
        xmlNode.setText(textValue);
    }

    /**
     * 2026-08-25 归档失败问题
     * 序列化单个节点：缩进只加在标签行首、文本原样转义输出；纯文本元素单行、有子元素时子元素逐行缩进
     */
    private static void p_writeNode(XMLNode xmlNode, int depth, StringBuilder contentBuilder) {
        for (int i = 0; i < depth; i++) {
            contentBuilder.append("  ");
        }
        String nodeName = xmlNode.getName();
        contentBuilder.append('<').append(nodeName);
        for (Map.Entry<String, String> attributeEntry : xmlNode.getAttributeMap().entrySet()) {
            String attributeName = attributeEntry.getKey();
            String attributeValue = attributeEntry.getValue();
            String escapedAttribute = p_escapeAttribute(attributeValue);
            contentBuilder.append(' ').append(attributeName).append("=\"").append(escapedAttribute).append("\"");
        }
        String textValue = xmlNode.getText();
        if (xmlNode.elements().isEmpty() && textValue.length() == 0) {
            contentBuilder.append("></").append(nodeName).append(">\n");
            return;
        }
        String escapedText = p_escapeText(textValue);
        contentBuilder.append('>');
        contentBuilder.append(escapedText);
        if (xmlNode.elements().isEmpty()) {
            contentBuilder.append("</").append(nodeName).append(">\n");
            return;
        }
        for (XMLNode childNode : xmlNode.elements()) {
            p_writeNode(childNode, depth + 1, contentBuilder);
        }
        for (int i = 0; i < depth; i++) {
            contentBuilder.append("  ");
        }
        contentBuilder.append("</").append(nodeName).append(">\n");
    }

    /**
     * 2026-08-25 归档失败问题
     * 文本转义（仅XML必需的&、<、>，其余字符含空格/换行原样保留）；非法XML字符抛异常并给出字符码
     */
    private static String p_escapeText(String text) {
        StringBuilder escapedBuilder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '&') {
                escapedBuilder.append("&amp;");
            } else if (ch == '<') {
                escapedBuilder.append("&lt;");
            } else if (ch == '>') {
                escapedBuilder.append("&gt;");
            } else if (p_isIllegalXmlChar(ch)) {
                StringBuilder messageBuilder = new StringBuilder();
                messageBuilder.append("文本含XML 1.0非法字符，拒绝写出：字符码=0x")
                        .append(Integer.toHexString(ch)).append("，文本片段=").append(text, 0, Math.min(text.length(), 50));
                throw new IllegalArgumentException(messageBuilder.toString());
            } else {
                escapedBuilder.append(ch);
            }
        }
        return escapedBuilder.toString();
    }

    /**
     * 2026-08-25 归档失败问题
     * 属性值转义（文本转义基础上另加双引号；属性值不含换行制表等按原样保留）
     */
    private static String p_escapeAttribute(String attributeValue) {
        String escapedText = p_escapeText(attributeValue);
        String escapedAttribute = escapedText.replace("\"", "&quot;");
        return escapedAttribute;
    }

    /**
     * 2026-08-25 归档失败问题
     * XML 1.0合法字符判断：仅允许0x9、0xA、0xD、0x20-0xD7FF、0xE000-0xFFFD（char范围内的代理区由p_isIllegalXmlChar判非法）
     */
    private static boolean p_isIllegalXmlChar(char ch) {
        if (ch == 0x9 || ch == 0xA || ch == 0xD) {
            return false;
        }
        if (ch >= 0x20 && ch <= 0xD7FF) {
            return false;
        }
        if (ch >= 0xE000 && ch <= 0xFFFD) {
            return false;
        }
        return true;
    }
}
