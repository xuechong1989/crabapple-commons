package xc.crabapple.core.ocr.baidu;

import tools.jackson.databind.JsonNode;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.UriEncoderUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.file.pdf.PdfUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.json.jackson.JacksonUtil;
import xc.crabapple.core.log.LogUtil;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * 2018-10-29 1.1.0
 * 基于百度云-OCR框架
 * jpg,jpeg,png,bmp,pdf
 * 请自行注册百度智能云并申请OCR应用
 * https://console.bce.baidu.com/ai/
 */
public final class OcrBaiduUtil {

    private static final String API_KEY = "your-api-key";
    private static final String SECRET_KEY = "your-secret-key";
    //通用文字识别（含位置信息版）500次/天免费 不保证并发
    private static final String addressGeneral = "https://aip.baidubce.com/rest/2.0/ocr/v1/general";
    //获取accessToken时所使用的地址
    private static final String addressAccessToken = "https://aip.baidubce.com/oauth/2.0/token";
    //API_KEY,SECRET_KEY这两个参数通过调用百度认证接口，获取accessToken
    //注：第一次执行程序时请先保持此变量为空字符串
    private static String accessToken = "";
    //错误标识符
    public static final String ERROR_FLAG = "crabapple_baidu-ocr_errorFlag";

    /**
     * 2018-10-27 1.1.0
     * @param url 文件绝对路径 jpg,jpeg,png,bmp,pdf
     * @param splitFlag 原图中每行结尾的分隔符，默认为\n
     * @return String
     */
    public static final String file2text(final String url, final String splitFlag){
        if(StringUtil.isBlank(url)){
            return ERROR_FLAG + "OcrBaiduUtil_file2text_1, error, url参数为空, url="+url;
        }
        String localSplitFlag = splitFlag;
        if(StringUtil.isBlank(localSplitFlag)){
            localSplitFlag = "\n";
        }
        String type = FileUtil.getFileType(url);
        if(StringUtil.isBlank(type)){
            return ERROR_FLAG + "OcrBaiduUtil_file2text_2, error, url参数文件格式错误, url="+url;
        }
        type = StringUtil.toLowerCase(type);
        if("pdf".equals(type)){
            String result = pdf2text(url, localSplitFlag);
            return result;
        }
        if("jpg".equals(type) || "jpeg".equals(type) || "png".equals(type) || "bmp".equals(type)){
            String result = img2text(url, localSplitFlag);
            return result;
        }
        return ERROR_FLAG + "OcrBaiduUtil_file2text_3, error, url参数文件格式错误, url="+url;
    }

    /**
     * 2018-10-27 1.1.0
     * @param pdfUrl
     * @param splitFlag 原图中每行结尾的分隔符，默认为\n
     */
    private static final String pdf2text(final String pdfUrl, final String splitFlag){
        List<String> list = PdfUtil.pdf2png(pdfUrl, null, 0);
        if(CollectionUtil.isBlank(list)){
            return ERROR_FLAG + "OcrBaiduUtil_pdf2text_1, error, 文件列表为空, pdfUrl="+pdfUrl;
        }
        int size = list.size();
        if(size <= 0){
            return ERROR_FLAG + "OcrBaiduUtil_pdf2text_2, error, 文件列表为空, pdfUrl="+pdfUrl;
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
     */
    private static final String img2text(final String imgUrl, final String splitFlag){
        try{
            String result = post(imgUrl);
            if(result.indexOf(ERROR_FLAG) != -1){
                return result;
            }
            if(StringUtil.isBlank(result)){
                return ERROR_FLAG + "OcrBaiduUtil_img2text_1, error, result为空, imgUrl="+imgUrl;
            }
            JsonNode jsonObject = JacksonUtil.json2jsonNode(result);
            if(jsonObject == null){
                return ERROR_FLAG + "OcrBaiduUtil_img2text_2, errro, 返回值不是标准result格式, imgUrl="+imgUrl + ", result="+result;
            }
            JsonNode jsonArray = jsonObject.get("words_result");
            if(jsonArray == null){
                Object errorObj = jsonObject.get("error_code");
                if(errorObj == null){
                    return ERROR_FLAG + "OcrBaiduUtil_img2text_3, errro,, imgUrl="+imgUrl + ", result="+result;
                }
                return ERROR_FLAG + "OcrBaiduUtil_img2text_4, error, imgUrl="+imgUrl + jsonObject.toString();
            }
            int n = jsonArray.size();
            if(n <= 0){
                return ERROR_FLAG + "OcrBaiduUtil_img2text_5, error, imgUrl="+imgUrl;
            }
            StringBuilder stringBuilder = new StringBuilder();
            for(int i = 0; i < n; i++){
                JsonNode jsonObject1 = jsonArray.get(i);
                Object object = jsonObject1.get("words");
                if(object == null){
                    continue;
                }
                String line = object.toString();
                stringBuilder.append(line).append(splitFlag);
            }
            String text = stringBuilder.toString();
            return text;
        }catch(Throwable throwable){
            String location = "OcrBaiduUtil_img2text_6";
            LogUtil.error(location, throwable);
            String msg = ERROR_FLAG + ", " + location + ", imgUrl="+imgUrl + ", throwable="+throwable.toString();
            return msg;
        }
    }

    /**
     * 2018-10-29 1.1.0
     */
    private static final String post(final String imgUrl){
        HttpURLConnection httpURLConnection = null;
        OutputStream outputStream = null;
        DataOutputStream dataOutputStream = null;
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try{
            byte[] imgBytes = FileUtil.fileReadWithBytes(imgUrl);
            String imgStr = encode(imgBytes);
            String key = UriEncoderUtil.encodeURIComponent("image");
            String value = UriEncoderUtil.encodeURIComponent(imgStr);
            String params = key + "=" + value;
            String contentType = "application/x-www-form-urlencoded";
            String encoding = "UTF-8";
            if (addressGeneral.contains("nlp")) {
                encoding = "GBK";
            }
            String accessToken = getAccessToken();
            String address = addressGeneral + "?access_token=" + accessToken;
            URL url = new URL(address);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type", contentType);
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);
            outputStream = httpURLConnection.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);
            byte[] bytes = params.getBytes(encoding);
            dataOutputStream.write(bytes);
            dataOutputStream.flush();
            httpURLConnection.connect();
            inputStream = httpURLConnection.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream, encoding);
            bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder resultBuilder = new StringBuilder();
            String getLine;
            while ((getLine = bufferedReader.readLine()) != null) {
                resultBuilder.append(getLine);
            }
            String result = resultBuilder.toString();
            return result;
        }catch(Throwable throwable){
            String location = "OcrBaiduUtil_post_1";
            LogUtil.error(location, throwable, imgUrl);
            String msg = ERROR_FLAG + ", " + location + ", imgUrl="+imgUrl + ", throwable="+throwable.toString();
            return msg;
        }finally {
            IOUtil.close(bufferedReader, inputStreamReader, inputStream, dataOutputStream, outputStream);
            if(httpURLConnection != null){
                httpURLConnection.disconnect();
            }
        }
    }

    /**
     * 2018-10-29 1.1.0
     * 获取API访问token
     * 该token有一定的有效期，需要自行管理，当失效时需重新获取
     * grant_type为固定参数
     * 线上环境access_token有过期时间， 客户端可自行缓存，过期后重新获取。
     */
    private static final String getAccessToken() {
        if(!StringUtil.isBlank(accessToken)){
            return accessToken;
        }
        String params = "grant_type=client_credentials" + "&client_id=" + API_KEY + "&client_secret=" + SECRET_KEY;
        String fullUrl = addressAccessToken + "?" + params;
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader in = null;
        try{
            URL realUrl = new URL(fullUrl);
            connection = (HttpURLConnection) realUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.connect();
            inputStream = connection.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);
            in = new BufferedReader(inputStreamReader);
            StringBuilder resultBuilder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                resultBuilder.append(line);
            }
            String result = resultBuilder.toString();
            JsonNode jsonNode = JacksonUtil.json2jsonNode(result);
            accessToken = JacksonUtil.getString(jsonNode, "access_token");
            return accessToken;
        }catch(Throwable throwable){
            LogUtil.error("OcrBaiduUtil_getAccessToken_t1", throwable, fullUrl);
        }finally{
            IOUtil.close(in, inputStreamReader, inputStream);
            if(connection != null){
                connection.disconnect();
            }
        }
        return "";
    }

    //Base64
    private static final char last2byte = (char) Integer.parseInt("00000011", 2);
    private static final char last4byte = (char) Integer.parseInt("00001111", 2);
    private static final char last6byte = (char) Integer.parseInt("00111111", 2);
    private static final char lead6byte = (char) Integer.parseInt("11111100", 2);
    private static final char lead4byte = (char) Integer.parseInt("11110000", 2);
    private static final char lead2byte = (char) Integer.parseInt("11000000", 2);
    private static final char[] encodeTable = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};

    private static final String encode(final byte[] from) {
        StringBuilder to = new StringBuilder((int) ((double) from.length * 1.34D) + 3);
        int num = 0;
        char currentByte = 0;
        int i;
        for (i = 0; i < from.length; ++i) {
            for (num %= 8; num < 8; num += 6) {
                switch (num) {
                    case 0:
                        currentByte = (char) (from[i] & lead6byte);
                        currentByte = (char) (currentByte >>> 2);
                    case 1:
                    case 3:
                    case 5:
                    default:
                        break;
                    case 2:
                        currentByte = (char) (from[i] & last6byte);
                        break;
                    case 4:
                        currentByte = (char) (from[i] & last4byte);
                        currentByte = (char) (currentByte << 2);
                        if (i + 1 < from.length) {
                            currentByte = (char) (currentByte | (from[i + 1] & lead2byte) >>> 6);
                        }
                        break;
                    case 6:
                        currentByte = (char) (from[i] & last2byte);
                        currentByte = (char) (currentByte << 4);
                        if (i + 1 < from.length) {
                            currentByte = (char) (currentByte | (from[i + 1] & lead4byte) >>> 4);
                        }
                }
                to.append(encodeTable[currentByte]);
            }
        }
        if (to.length() % 4 != 0) {
            for (i = 4 - to.length() % 4; i > 0; --i) {
                to.append("=");
            }
        }
        return to.toString();
    }

}
