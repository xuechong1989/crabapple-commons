package xc.crabapple.core.file.html;

import in.wilsonl.minifyhtml.Configuration;
import in.wilsonl.minifyhtml.MinifyHtml;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.url.UrlUtil;

/**
 * 2020-06-23 6.2.0
 */
public final class HtmlUtil {

    /**
     * 2025-02-28
     * @param fromUrl
     * @param toUrl
     * @return
     */
    public static final boolean mini(final String fromUrl, final String toUrl){
        boolean flag = mini(fromUrl, toUrl, false);
        return flag;
    }

    /**
     * 2025-02-28
     * @param fromUrl
     * @param toUrl
     * @param cover
     * @return
     */
    public static final boolean mini(final String fromUrl, final String toUrl, final boolean cover){
        String fromUrlLocal = UrlUtil.getStandardPath(fromUrl);
        String toUrlLocal = UrlUtil.getStandardPath(toUrl);
        if(!cover){
            if(FileUtil.exists(toUrlLocal)){
                LogUtil.error("HtmlUtil_mini_2", "toUrl文件已存在", fromUrlLocal, toUrlLocal, cover);
            }
        }
        //
        String originalHtml = FileUtil.fileReadText(fromUrlLocal);
        try {
            String miniText = originalHtml;
            // 对html与css进行压缩，删除注释
            Configuration config = new Configuration.Builder()
                    .setKeepHtmlAndHeadOpeningTags(true)
                    .setKeepComments(false)
                    .setMinifyJs(true)
                    .setMinifyCss(true).setKeepClosingTags(true).setKeepSsiComments(false)
                    .build();
            miniText = MinifyHtml.minify(miniText,  config);
            // 处理javascript代码
            String[] array = StringUtil.split(miniText, "\n");
            StringBuilder stringBuilder = new StringBuilder();
            for(int i = 0; i < array.length; i++) {
                String line = array[i];
                line = StringUtil.trimPure(line);
                if(StringUtil.isBlank(line)){
                    continue;
                }
                if(StringUtil.startsWith(line, "//")){
                    continue;
                }
                if(StringUtil.startsWith(line, "/*") && StringUtil.lastsWith(line, "*/")){
                    continue;
                }
                if(StringUtil.startsWith(line, "<!doctypehtml>")){
                    line = StringUtil.replaceAll(line, "<!doctypehtml>", "<!DOCTYPE html>");
                    line = StringUtil.replaceAll(line, "<script>", "\n<script>\n");
                    stringBuilder.append(line).append("\n");
                    continue;
                }
                if(StringUtil.startsWith(line, "/*")){
                    line = StringUtil.replaceAll(line, "/*", "\n/*\n");
                    stringBuilder.append(line).append("\n");
                    continue;
                }
                if(StringUtil.lastsWith(line, "/*")){
                    line = StringUtil.replaceAll(line, "/*", "\n/*");
                    stringBuilder.append(line).append("\n");
                    continue;
                }
                if(StringUtil.startsWith(line, "*/")){
                    line = StringUtil.replaceAll(line, "*/", "\n*/\n");
                    stringBuilder.append(line).append("\n");
                    continue;
                }
                if(StringUtil.startsWith(line, "*")){
                    line = StringUtil.replaceAll(line, "*/var", "\n*/\nvar");
                    stringBuilder.append(line).append("\n");
                    continue;
                }
                if(StringUtil.lastsWith(line, "*/")){
                    line = StringUtil.replaceAll(line, "*/", "\n*/\n");
                    stringBuilder.append(line).append("\n");
                    continue;
                }
                stringBuilder.append(line).append("\n");
            }
            miniText = stringBuilder.toString();
            array = StringUtil.split(miniText, "\n");
            stringBuilder = new StringBuilder();
            boolean commentStart = false;
            for(int i = 0; i < array.length; i++) {
                String line = array[i];
                line = StringUtil.trim(line);
                if(StringUtil.isBlank(line)){
                    continue;
                }
                //
                if(StringUtil.startsWith(line, "//")){
                    continue;
                }
                if(StringUtil.startsWith(line, "/*")){
                    commentStart = true;
                    continue;
                }
                if(commentStart){
                    //
                    if(StringUtil.startsWith(line, "*/")){
                        commentStart = false;
                        continue;
                    }
                    continue;
                }
                stringBuilder.append(line);
            }
            miniText = stringBuilder.toString();
            // 是否覆盖
            if(cover){
                FileUtil.fileDelete(toUrlLocal);
            }
            boolean flag = FileUtil.fileWriteNew(toUrlLocal, miniText, true);
            return flag;
        } catch (Throwable throwable) {
            LogUtil.error("HtmlUtil_mini_t1", throwable, fromUrlLocal, toUrlLocal);
            return false;
        }
    }

    /**
     * 2023-03-17
     * 获取指定CSS(style)样式的值，只能获取第一个style
     * @param style 格式如 text-align:center;background-color:#000000;
     * @param styleName 格式如 text-align
     * @return
     */
    public static final String getTargetStyle(final String style, final String styleName){
        String styleLocal = style;
        if(!StringUtil.containOf(styleLocal, ";")){
            styleLocal += ";";
        }
        String[] array = StringUtil.split(styleLocal, ";");
        for(String kv : array){
            if(!StringUtil.containOf(kv, ":")){
                continue;
            }
            String[] arr = StringUtil.split(kv, ":");
            String key = arr[0];
            key = StringUtil.trim(key);
            if(!styleName.equals(key)){
                continue;
            }
            String value = arr[1];
            value = StringUtil.trim(value);
            return value;
        }
        return "";
    }

    /**
     * 2022-06-06
     * 获取标准XHTML代码片段
     * @param originalHtml
     * @return
     */
    public static final String getStandardFragmentHtml(final String originalHtml){
        //
        Document document = getStandardDocument(originalHtml);
        //
        String standardHtml = getFragmentHtml(document, originalHtml);
        //
        return standardHtml;
    }

    /**
     * 2022-06-06
     * 获取标准XHTML代码片段
     * @param originalHtml
     * @param tagName
     * @param deleteAttributes
     * @return
     */
    public static final String getStandardFragmentHtml(final String originalHtml, final String tagName, final String[] deleteAttributes){
        //
        Document document = getStandardDocument(originalHtml);
        //
        document = deleteAttrs(document, tagName, deleteAttributes);
        //
        String standardHtml = getFragmentHtml(document, originalHtml);
        //
        return standardHtml;
    }

    /**
     * 2022-06-06
     * @param fragmentHtml
     * @return
     */
    public static final Document getStandardDocument(final String fragmentHtml){
        // 删除所有注释
        String htmlStr = deleteComments(fragmentHtml);
        // 交由jsoup处理
        Document document = Jsoup.parseBodyFragment(htmlStr);
        // 转为标准XHTML
        setDocument2XHTML(document);
        //
        return document;
    }

    /**
     * 2022-06-06
     * 获得标准结果
     * @param document
     * @param originalHtml
     * @return
     */
    public static final String getFragmentHtml(final Document document, final String originalHtml){
        Element element = document.body();
        String result = element.toString();
        // 若原数据有<body>
        if(StringUtil.containOf(originalHtml, "<body>")){
            return result;
        }
        //
        result = StringUtil.deleteAll(result, "<body>", "</body>");
        //
        while(StringUtil.startsWith(result, "\n")){
            result = StringUtil.deleteFirst(result, "\n");
        }
        result = StringUtil.trim(result);
        //
        return result;
    }

    /**
     * 2022-06-06
     * @param originalHtml
     * @param tagName
     * @param attributes
     * @return
     */
    public static final String deleteAttrs(final String originalHtml, final String tagName, final String[] attributes){
        Document document = Jsoup.parseBodyFragment(originalHtml);
        //
        document = deleteAttrs(document, tagName, attributes);
        //
        String result = getFragmentHtml(document, originalHtml);
        return result;
    }

    /**
     * 2022-06-06
     * 删除指定标签的指定属性
     * @param document
     * @param tagName 标签名称，如div
     * @param attributes 要删除的属性集合
     */
    public static final Document deleteAttrs(final Document document, final String tagName, final String[] attributes){
        if(CollectionUtil.isBlank(attributes)){
            return document;
        }
        for(String attribute : attributes){
            document.select(tagName).removeAttr(attribute);
        }
        return document;
    }

    /**
     * 2022-06-06
     * 基于jsoup
     * 把原HTML片段转为XHTML，即严格语法格式，如所有标签都闭合结尾 />
     */
    public static final Document setDocument2XHTML(final Document document){
        Document.OutputSettings outputSettings = document.outputSettings();
        outputSettings.syntax(Document.OutputSettings.Syntax.xml);
        outputSettings.escapeMode(Entities.EscapeMode.xhtml);
        outputSettings.charset("UTF-8");
        return document;
    }

    /**
     * 2022-06-06
     * 删除指定html片段中的所有注释<!-- xxx -->
     * @param fragmentHtml
     * @return
     */
    public static final String deleteComments(final String fragmentHtml){
        if(StringUtil.isBlank(fragmentHtml)){
            return "";
        }
        String fragmentHtmlLocal = fragmentHtml;
        int firstIndex = fragmentHtmlLocal.indexOf("<!--");
        if(firstIndex == -1){
            return fragmentHtmlLocal;
        }
        //
        while(firstIndex != -1){
            String first = fragmentHtmlLocal.substring(0, firstIndex);
            int secondIndex = fragmentHtmlLocal.indexOf("-->");
            if(secondIndex == -1){
                return first;
            }
            String second = fragmentHtmlLocal.substring(secondIndex + 3, fragmentHtmlLocal.length());
            fragmentHtmlLocal = first + second;
            //
            firstIndex = fragmentHtmlLocal.indexOf("<!--");
        }
        return fragmentHtmlLocal;
    }

    /**
     * 2022-05-26
     * html代码片段解析器
     * @param htmlStr
     * @return
     * 删除 data-options, onclick
     */
    public static final String htmlParser(final String htmlStr){
        // 单行化
        String htmlStrLocal = StringUtil.getPureStr(htmlStr);
        htmlStrLocal = StringUtil.trim(htmlStrLocal);
        for(int i = 5; i > 0; i--){
            StringBuilder emptySb = new StringBuilder();
            for(int j = i; j > 0; j--){
                emptySb.append(" ");
            }
            String emptyStr = emptySb.toString();
            htmlStrLocal = StringUtil.replaceAll(htmlStrLocal, ">"+emptyStr+"<", "><");
        }
        //
        boolean flag = false;
        StringBuilder stringBuilder = new StringBuilder();
        String[] array = StringUtil.split(htmlStrLocal, ">");
        for(String line : array){
//            if(StringUtil.startsWith(line, " ") || StringUtil.lastsWith(line, " ")){
//            }
            // 左右空格也不能轻易删除
            String tmpLine = StringUtil.trim(line);
            if(StringUtil.isBlank(tmpLine)){
                continue;
            }
            if(StringUtil.containOf(tmpLine, "<!--")){
                flag = true;
                continue;
            }
            if(flag){
                if(StringUtil.containOf(tmpLine, "-->")){
                    flag = false;
                }
                continue;
            }
            if(StringUtil.containOf(tmpLine, "type=\"hidden\"")){
                continue;
            }
            if(StringUtil.startsWith(tmpLine, "<br")){
                if(!StringUtil.lastsWith(tmpLine, "/")){
                    stringBuilder.append(line).append("/>");
                    continue;
                }
            }
            if(StringUtil.startsWith(tmpLine, "<img ")){
                if(!StringUtil.lastsWith(tmpLine, "/")){
                    stringBuilder.append(line).append("/>");
                    continue;
                }
            }
            if(StringUtil.startsWith(tmpLine, "<input ")){
                if(!StringUtil.lastsWith(tmpLine, "/")){
                    stringBuilder.append(line).append("/>");
                    continue;
                }
            }
            stringBuilder.append(line).append(">");
        }
        htmlStrLocal = stringBuilder.toString();
//        // 删除元素
//        for(int i = 0; i < 20; i++){
//            htmlStr = StringUtil.deleteAll(htmlStr, "<div class=\"noPrint\"><input type=\"checkbox\" id=\"tab_"+i+"\" name=\"chkPrint\" checked=\"\"><label for=\"tab_"+i+"\">打印</label></div>");
//        }
        //
        return htmlStrLocal;
    }

    /**
     * 2020-06-23 6.2.0
     * @param htmlUrl
     */
    public static final String readHtml(final String htmlUrl){
        if(StringUtil.isBlank(htmlUrl)){
            LogUtil.error("HtmlUtil_readHtml_1", "htmlUrl为空");
            return "";
        }
        String type = FileUtil.getFileType(htmlUrl);
        type = StringUtil.toLowerCase(type);
        if(!"html".equals(type) && !"htm".equals(type)){
            LogUtil.error("HtmlUtil_readHtml_2", "htmlUrl类型错误", htmlUrl);
            return "";
        }
        String content = FileUtil.fileReadText(htmlUrl);
        return content;
    }

}
