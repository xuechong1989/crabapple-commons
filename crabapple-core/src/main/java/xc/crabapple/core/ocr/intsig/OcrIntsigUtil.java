package xc.crabapple.core.ocr.intsig;

import tools.jackson.databind.JsonNode;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.file.image.ImageUtil;
import xc.crabapple.core.file.pdf.PdfUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.json.jackson.JacksonUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.RandomUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * 2018-10-29 1.1.0
 * https://imgs-sandbox.intsig.net/icr/demo/recognize_document_v2.htm
 演示账号与密码请自行到官网申请
 jpg,jpeg,png,pdf
 */
public final class OcrIntsigUtil {

    private static final String ADDRESS = "http://imgs-sandbox.intsig.net/icr/recognize_document";
    private static final String USER = "your-account";
    private static final String PASSWORD = "your-password";
    //错误标识符
    public static final String ERROR_FLAG = "crabapple_intsig-ocr_errorFlag";

    /**
     * 2018-10-29 1.1.0
     * @param url 文件绝对路径 jpg,jpeg,png,pdf
     * @param splitFlag 原图中每行结尾的分隔符，默认为\n
     * @return String
     */
    public static final String file2text(final String url, final String splitFlag){
        if(StringUtil.isBlank(url)){
            return ERROR_FLAG + "OcrIntsigUtil_file2text_1, error, url参数为空, url="+url;
        }
        String localSplitFlag = splitFlag;
        if(StringUtil.isBlank(localSplitFlag)){
            localSplitFlag = "\n";
        }
        String type = FileUtil.getFileType(url);
        if(StringUtil.isBlank(type)){
            return ERROR_FLAG + "OcrIntsigUtil_file2text_2, error, url参数文件格式错误, url="+url;
        }
        type = StringUtil.toLowerCase(type);
        if("pdf".equals(type)){
            String result = pdf2text(url, localSplitFlag);
            return result;
        }
        if("jpg".equals(type) || "jpeg".equals(type) || "png".equals(type)){
            String result = img2text(url, localSplitFlag);
            return result;
        }
        return ERROR_FLAG + "OcrIntsigUtil_file2text_3, error, url参数文件格式错误, url="+url;
    }

    /**
     * 2018-10-29 1.1.0
     * @param pdfUrl
     * @param splitFlag 原图中每行结尾的分隔符，默认为\n
     */
    private static final String pdf2text(final String pdfUrl, final String splitFlag){
        List<String> list = PdfUtil.pdf2png(pdfUrl, null, 0);
        if(CollectionUtil.isBlank(list)){
            return ERROR_FLAG + "OcrIntsigUtil_pdf2text_1, error, 文件列表为空, pdfUrl="+pdfUrl;
        }
        int size = list.size();
        if(size <= 0){
            return ERROR_FLAG + "OcrIntsigUtil_pdf2text_2, error, 文件列表为空, pdfUrl="+pdfUrl;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0 ; i < size; i++){
            String imgUrl = list.get(i);
            String value = img2text(imgUrl, splitFlag);
            if(value.indexOf(ERROR_FLAG) != -1){
                return value;
            }
            stringBuilder.append(value);
        }
        String result = stringBuilder.toString();
        return result;
    }

    /**
     * 2018-10-29 1.1.0
     * @param imgUrl 图片地址，如D:/data/cache/tmp/test.jpg
     * @param splitFlag 原图中每行结尾的分隔符，默认为\n
     */
    public static final String img2text(final String imgUrl, final String splitFlag){
        try{
            String result = post(imgUrl);
            if(result.indexOf(ERROR_FLAG) != -1){
                return result;
            }
            JsonNode jsonObject = JacksonUtil.json2jsonNode(result);
            if(jsonObject == null){
                return ERROR_FLAG + "OcrIntsigUtil_img2text_1, error, imgUrl="+imgUrl + ", result="+result;
            }
            JsonNode jsonArray = jsonObject.get("linesText");
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < jsonArray.size(); i++){
                Object object = jsonArray.get(i);
                String value = object.toString();
                stringBuilder.append(value + splitFlag);
            }
            result = stringBuilder.toString();
            return result;
        }catch(Throwable throwable){
            String locaction = "OcrIntsigUtil_img2text_2";
            LogUtil.error(locaction, throwable, imgUrl, splitFlag);
            String msg = ERROR_FLAG + ", " + locaction + ", error, imgUrl="+imgUrl + ", throwable="+throwable.toString();
            return msg;
        }
    }

    /**
     * 2018-10-29 1.1.0
     * @param imgUrl
     * {
     *   "error_msg":"ok",
     *   "lineNum":15,
     *   "error_code":0,
     *   "linesText":[
     *     "新华网",
     *     "WWW.NEWS.CN",
     *     "珠海横琴新区粤澳合作中医药科技产业园研发人员"
     *    ]
     * }
     */
    public static final String post(final String imgUrl){
        File file = new File(imgUrl);
        String params = "user=" + USER + "&password=" + PASSWORD;
        String address = ADDRESS + "?" + params;
        HttpURLConnection httpURLConnection = null;
        OutputStream outputStream = null;
        FileInputStream inputStream = null;
        InputStream inputStream2 = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        String encoding = "UTF-8";
        try{
            URL url = new URL(address);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setRequestMethod("POST");
            outputStream = httpURLConnection.getOutputStream();
            inputStream = new FileInputStream(file);
            byte[] data = new byte[2048];
            int len = 0;
            int sum = 0;
            while ((len = inputStream.read(data))!= -1) {
                outputStream.write(data, 0, len);
                sum = len + sum;
            }
            outputStream.flush();
            int code = httpURLConnection.getResponseCode();
            if (code == 200) {
                inputStream2 = httpURLConnection.getInputStream();
                byteArrayOutputStream = new ByteArrayOutputStream();
                while ((len = inputStream2.read(data))!= -1) {
                    byteArrayOutputStream.write(data, 0, len);
                }
                String result = byteArrayOutputStream.toString(encoding);
                return result;
            }
            //图片尺寸太大问题处理，2019-05-13
            if(code == 406){
                String str = RandomUtil.getStr(24);
                String pathFrom = FileUtil.getPathByUrl(imgUrl);
                String fname = FileUtil.getFileName(imgUrl, false);
                String ftype = FileUtil.getFileType(imgUrl);
                String tmpName = fname + "_" + str + "." + ftype;
                String urlTo = pathFrom + "/" + tmpName;
                boolean flag = ImageUtil.createImg(imgUrl, urlTo, "w", 1600);
                if(flag){
                    String msg = post(urlTo);
                    FileUtil.fileDelete(urlTo);
                    return msg;
                }else{
                    String msg = ERROR_FLAG + ", OcrIntsigUtil_post_3, error, 图片太大且创建缩略图失败, code="+code;
                    return msg;
                }
            }
            String msg = ERROR_FLAG + ", OcrIntsigUtil_post_1, error, 图片太大, code="+code;
            return msg;
        }catch(Throwable throwable){
            String location = "OcrIntsigUtil_post_2";
            LogUtil.error(location, throwable, imgUrl);
            String msg = ERROR_FLAG + ", " + location + ", throwable.toString()="+throwable.toString();
            return msg;
        }finally {
            IOUtil.close(byteArrayOutputStream, inputStream2, inputStream, outputStream);
            if(httpURLConnection != null){
                httpURLConnection.disconnect();
            }
        }
    }

}
