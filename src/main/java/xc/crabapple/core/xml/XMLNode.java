package xc.crabapple.core.xml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import xc.crabapple.core.log.LogUtil;

/**
 * 2026-08-25 归档失败问题
 * 自研轻量XML树节点（废弃dom4j，替代其Document/Element双类型，合一设计）：
 * 文档节点由 XmlUtil.createDocument() 创建（内部标记），addElement 添加根元素/子元素；
 * 文本经 setText 设置后由 XmlUtil 序列化时原样写出（不折叠空格、不去首尾），
 * 彻底规避 dom4j 美化输出折叠文本内连续空格导致EEP声明名与打包文件名不一致的问题
 */
public class XMLNode {

    // 节点名（标签名）
    private String p_name;
    // 文本内容（原样保存原样输出）
    private String p_text = "";
    // 属性表（LinkedHashMap保持添加顺序）
    private Map<String, String> p_attributeMap = new LinkedHashMap<String, String>();
    // 子节点列表（文档序）
    private List<XMLNode> p_childList = new ArrayList<XMLNode>();
    // 父节点（文档节点为null）
    private XMLNode p_parentNode;
    // 文档节点标记（getRootElement/getDocument/selectNodes绝对路径的锚点）
    private boolean p_documentFlag;

    /**
     * 2026-08-25 归档失败问题
     * 包内构造（外部统一经 XmlUtil.createDocument/addElement 创建节点）
     */
    XMLNode(String name, XMLNode parentNode, boolean documentFlag) {
        p_name = name;
        p_parentNode = parentNode;
        p_documentFlag = documentFlag;
    }

    /**
     * 2026-08-25 归档失败问题
     * 添加子元素并返回该子元素（与dom4j同名方法语义一致，支持链式构建）
     * @param elementName 子元素名
     * @return 新建的子元素节点
     */
    public XMLNode addElement(String elementName) {
        XMLNode childNode = new XMLNode(elementName, this, false);
        p_childList.add(childNode);
        return childNode;
    }

    public String getName() {
        return p_name;
    }

    /**
     * 2026-08-25 归档失败问题
     * 设置文本（null按空串处理；序列化时原样写出不折叠不去首尾，见XmlUtil）
     */
    public void setText(String text) {
        // 2026-08-27 归档失败问题
        // dom4j的setText(null)会抛异常阻断；本工具按不抛异常原则静默置空串，但必须打错误日志留痕
        // （真实null文本往往意味着上游数据缺失，如EEP计算机文件名字段为空）
        if (text == null) {
            LogUtil.error("XMLNode_setText_e1", "setText入参为null，已按空串处理", "nodeName", p_name);
        }
        p_text = text == null ? "" : text;
    }

    /**
     * 2026-08-25 归档失败问题
     * 追加文本（与dom4j同名方法语义对齐：在现有文本末尾追加，供BIM数据封装等链式写法使用）
     */
    public void addText(String appendText) {
        String appendValue = appendText == null ? "" : appendText;
        p_text = p_text + appendValue;
    }

    public String getText() {
        return p_text;
    }

    public void addAttribute(String attributeName, String attributeValue) {
        p_attributeMap.put(attributeName, attributeValue);
    }

    public String attributeValue(String attributeName) {
        String attributeValue = p_attributeMap.get(attributeName);
        return attributeValue;
    }

    /**
     * 2026-08-25 归档失败问题
     * 取属性表（包内可见，供XmlUtil序列化按添加顺序遍历；调用方禁止修改返回Map）
     */
    Map<String, String> getAttributeMap() {
        return p_attributeMap;
    }

    /**
     * 2026-08-25 归档失败问题
     * 取第一个同名子元素（无则返回null，与dom4j同名方法语义一致）
     */
    public XMLNode element(String elementName) {
        List<XMLNode> sameNameList = elements(elementName);
        XMLNode firstNode = null;
        if (!sameNameList.isEmpty()) {
            firstNode = sameNameList.get(0);
        }
        return firstNode;
    }

    /**
     * 2026-08-25 归档失败问题
     * 取全部子元素（不复制，调用方禁止增删返回列表，仅遍历）
     */
    public List<XMLNode> elements() {
        return p_childList;
    }

    public List<XMLNode> elements(String elementName) {
        List<XMLNode> resultList = new ArrayList<XMLNode>();
        for (XMLNode childNode : p_childList) {
            String childName = childNode.getName();
            if (elementName.equals(childName)) {
                resultList.add(childNode);
            }
        }
        return resultList;
    }

    public XMLNode getParent() {
        return p_parentNode;
    }

    /**
     * 2026-08-25 归档失败问题
     * 取根元素（文档节点返回其第一个子元素；元素节点返回所在文档的根元素，与dom4j同名方法语义一致）
     */
    public XMLNode getRootElement() {
        XMLNode documentNode = getDocument();
        XMLNode rootElement = null;
        if (documentNode != null && !documentNode.p_childList.isEmpty()) {
            rootElement = documentNode.p_childList.get(0);
        }
        return rootElement;
    }

    /**
     * 2026-08-25 归档失败问题
     * 沿父链取所在文档节点（与dom4j同名方法语义一致）
     */
    public XMLNode getDocument() {
        XMLNode currentNode = this;
        while (currentNode != null && !currentNode.p_documentFlag) {
            currentNode = currentNode.p_parentNode;
        }
        return currentNode;
    }

    /**
     * 2026-08-25 归档失败问题
     * 简化路径查询（覆盖业务全部10处用法，不实现谓词/函数等完整XPath）：
     * 支持 /a/b（文档绝对路径）、//a（全树后代）、a/b（相对子级）、a//b（相对+后代）四类，
     * 按节点名精确匹配，返回文档序结果
     * @param path 路径表达式
     * @return 匹配节点列表（无匹配返回空列表）
     */
    public List<XMLNode> selectNodes(String path) {
        List<XMLNode> resultList = new ArrayList<XMLNode>();
        if (path == null || path.length() == 0) {
            return resultList;
        }
        // 解析路径段：绝对标志与段序列
        boolean[] absoluteHolder = new boolean[1];
        List<PathSegment> segmentList = parsePath(path, absoluteHolder);
        if (segmentList.isEmpty()) {
            return resultList;
        }
        // 绝对路径从文档节点起匹配（其子级即根元素），相对路径从本节点起匹配
        XMLNode startNode = this;
        if (absoluteHolder[0]) {
            XMLNode documentNode = getDocument();
            if (documentNode != null) {
                startNode = documentNode;
            }
        }
        matchChildren(startNode, segmentList, 0, resultList);
        return resultList;
    }

    /**
     * 2026-08-25 归档失败问题
     * 解析路径为段序列：前导/（非//）记录绝对标志，路径中间的//使下一段带后代标志
     */
    private static List<PathSegment> parsePath(String path, boolean[] absoluteHolder) {
        List<PathSegment> segmentList = new ArrayList<PathSegment>();
        String[] partArray = path.split("/");
        boolean pendingDescendant = false;
        for (int i = 0; i < partArray.length; i++) {
            String part = partArray[i];
            if (part.length() == 0) {
                if (i == 0) {
                    absoluteHolder[0] = true;
                    continue;
                }
                pendingDescendant = true;
                continue;
            }
            PathSegment segment = new PathSegment(pendingDescendant, part);
            segmentList.add(segment);
            pendingDescendant = false;
        }
        return segmentList;
    }

    /**
     * 2026-08-25 归档失败问题
     * 从当前节点的子级起逐段匹配；末段命中即计入结果
     */
    private static void matchChildren(XMLNode currentNode, List<PathSegment> segmentList, int segmentIndex, List<XMLNode> resultList) {
        PathSegment segment = segmentList.get(segmentIndex);
        boolean lastSegment = segmentIndex == segmentList.size() - 1;
        if (segment.p_descendantFlag) {
            collectDescendants(currentNode, segment.p_segmentName, segmentList, segmentIndex, resultList);
            return;
        }
        for (XMLNode childNode : currentNode.p_childList) {
            String childName = childNode.getName();
            if (!segment.p_segmentName.equals(childName)) {
                continue;
            }
            if (lastSegment) {
                resultList.add(childNode);
            } else {
                matchChildren(childNode, segmentList, segmentIndex + 1, resultList);
            }
        }
    }

    /**
     * 2026-08-25 归档失败问题
     * 在当前节点整个子树（不含自身）中深搜同名节点，命中末段计入结果
     */
    private static void collectDescendants(XMLNode currentNode, String segmentName, List<PathSegment> segmentList, int segmentIndex, List<XMLNode> resultList) {
        boolean lastSegment = segmentIndex == segmentList.size() - 1;
        for (XMLNode childNode : currentNode.p_childList) {
            String childName = childNode.getName();
            if (segmentName.equals(childName)) {
                if (lastSegment) {
                    resultList.add(childNode);
                } else {
                    matchChildren(childNode, segmentList, segmentIndex + 1, resultList);
                }
            }
            collectDescendants(childNode, segmentName, segmentList, segmentIndex, resultList);
        }
    }

    /**
     * 2026-08-25 归档失败问题
     * 路径段：p_descendantFlag=true表示该段为后代搜索（//），否则为直接子级匹配
     */
    private static class PathSegment {
        private boolean p_descendantFlag;
        private String p_segmentName;

        PathSegment(boolean descendantFlag, String segmentName) {
            p_descendantFlag = descendantFlag;
            p_segmentName = segmentName;
        }
    }
}
