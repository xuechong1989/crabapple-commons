package xc.crabapple.core.ocr.baidu2;

import tools.jackson.databind.JsonNode;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.ListUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.file.image.ImageUtil;
import xc.crabapple.core.json.jackson.JacksonUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.math.MathUtil;
import xc.crabapple.core.sort.SortUtil;
import xc.crabapple.core.url.UrlUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 2020-08-07 6.4.0
 * 基于百度OCR私有化部署
 * #百度OCR帮助文档
 * https://cloud.baidu.com/doc/OCR/s/zk3h7xz52
 */
public final class OcrBaidu2Util {

    private static OcrBaidu2ConfigBean ocrBaidu2ConfigBean;

    /**
     * 2020-08-18 6.5.0
     * @param ocrBaidu2ConfigBean
     */
    public static final void init(final OcrBaidu2ConfigBean ocrBaidu2ConfigBean){
        OcrBaidu2Util.ocrBaidu2ConfigBean = ocrBaidu2ConfigBean;
        String protocol = ocrBaidu2ConfigBean.getProtocol();
        String host = ocrBaidu2ConfigBean.getHost();
        LogUtil.print("OcrBaidu2Util_init_1", "OCR ...... 开启， " + protocol + ", " + host);
    }

    public static final List<OcrBaidu2Bean> getList(final String imgUrl){
        List<OcrBaidu2Bean> list = p_getList(imgUrl);
        if(CollectionUtil.isBlank(list)){
            return list;
        }
        List<OcrBaidu2Bean> list2 = ListUtil.copy(list);
        SortUtil.sortList(list2, "x", "asc");
        OcrBaidu2Bean ocrBaidu2Bean = list2.get(0);
        Integer miniX = ocrBaidu2Bean.getX();  //基准x轴
        list2.clear();
        //
        for(OcrBaidu2Bean ocrBaidu2Bean1 : list){
            String content = ocrBaidu2Bean1.getContent();
            Integer chars = content.length();
            Integer x = ocrBaidu2Bean1.getX();
            Integer width = ocrBaidu2Bean1.getWidth();
            Double size = MathUtil.divideReturnDouble(width, chars, 2);  //字号大小
            //Integer chars2 =  StringUtil.getLength(content);
            //Double ddd = MathUtil.divide(width, chars2);  //
            Double count = (x - miniX) / size;
            if(count < 0.5){
                count = 0D;
            }else{
                count += 0.5D;
            }
            StringBuilder emptySb = new StringBuilder();
            if(count > 0){
                Double iii = Math.floor(count);
                ocrBaidu2Bean1.setEmptyCount(iii);
                for(int i = 0; i < iii; i++){
                    emptySb.append("  ");
                }
            }
            String empty = emptySb.toString();
            String exContent = empty + content;
            ocrBaidu2Bean1.setFormatContent(exContent);
        }
        return list;
    }

    /**
     * 2021-04-07 7.5.0
     * 返回内容，包含字符定位信息
     */
    private static final List<OcrBaidu2Bean> p_getList(final String imgUrl){
        List<OcrBaidu2Bean> list = new ArrayList<>();
        String fullText = p_getText(imgUrl);
        if(StringUtil.isBlank(fullText)){
            return list;
        }
        if(StringUtil.startsWith(fullText, "xc_crabapple_error:")){
            OcrBaidu2Bean ocrBaidu2Bean = new OcrBaidu2Bean();
            ocrBaidu2Bean.setContent(fullText);
            ocrBaidu2Bean.setFormatContent(fullText);
            list.add(ocrBaidu2Bean);
            return list;
        }
        JsonNode jsonNode = JacksonUtil.json2jsonNode(fullText);
        if(jsonNode == null){
            return list;
        }
        boolean isArray = jsonNode.isArray();
        if(!isArray){
            return list;
        }
        Iterator<JsonNode> iterator = jsonNode.iterator();
        int i = 0;
        while(iterator.hasNext()){
            JsonNode node = iterator.next();
            JsonNode rect = JacksonUtil.getJsonNode(node, "rect");
            Integer left = JacksonUtil.getInt(rect, "left");
            Integer top =  JacksonUtil.getInt(rect, "top");
            Integer width = JacksonUtil.getInt(rect, "width");
            Integer height = JacksonUtil.getInt(rect, "height");
            String content = JacksonUtil.getString(node, "word");
            //
            OcrBaidu2Bean ocrBaidu2Bean = new OcrBaidu2Bean();
            i++;
            ocrBaidu2Bean.setIndex(i);
            if(left != null){
                ocrBaidu2Bean.setX(left);
            }
            if(top != null){
                ocrBaidu2Bean.setY(top);
            }
            if(width != null){
                ocrBaidu2Bean.setWidth(width);
            }
            if(height != null){
                ocrBaidu2Bean.setHeight(height);
            }
            if(!StringUtil.isBlank(content)){
                ocrBaidu2Bean.setContent(content);
            }
            list.add(ocrBaidu2Bean);
        }
        return list;
    }

    /**
     * 2020-06-19 6.2.0
     * 获取全文本
     */
    private static final String p_getText(final String imgUrl){
        if(StringUtil.isBlank(imgUrl)){
            final String location = "xc_crabapple_OcrBaidu2Util_p_getText_1";
            LogUtil.error(location, imgUrl);
            return "xc_crabapple_error: imgUrl_is_null, imgUrl="+imgUrl + ", ---" + location;
        }
        String localImgUrl = UrlUtil.getStandardPath(imgUrl);
        long size = FileUtil.getFileSize(localImgUrl);
        final long TOP_SIZE = 1024000 * 2;
        if(size > TOP_SIZE){
            final String location = "xc_crabapple_OcrBaidu2Util_p_getText_4";
            LogUtil.error(location, localImgUrl, size);
            return "xc_crabapple_error: size_is_too_large, size="+size + ", imgUrl="+localImgUrl + ",---" + location;
        }
        String imgdata = ImageUtil.img2base64(localImgUrl);
        //ClipboardUtil.setString(imgdata);
        //imgdata = Base64Util.encodeStr2str(imgdata);
        //
        String protocol = ocrBaidu2ConfigBean.getProtocol();
        final String host = ocrBaidu2ConfigBean.getHost();
        final String url = protocol + "://" + host + "/GeneralClassifyService/classify";  //?ip_address=192.168.1.100&port=8017
        final String request_str = "type=st_ocrapi&appid=20005&detecttype=LocateRecognize&languagetype=CHN_ENG&imgDirection=setImgDirFlag&recg_type=seq&locate_type=v2";
        final String param = request_str + "&image=" + imgdata;
        return null;
    }

}
