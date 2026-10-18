package xc.crabapple.core.ocr.tess4j;

import net.sourceforge.tess4j.Tesseract;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.ListUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.file.pdf.PdfUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.thread.Business;
import xc.crabapple.core.thread.ThreadUtil;
import xc.crabapple.core.url.UrlUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 2018-11-08 1.1.0
 * 2024-05-16 代码重构
 *
 * tesseract下载地址
 * https://digi.bib.uni-mannheim.de/tesseract/
 *
 * windows10环境安装tesseract
 * 1、安装tesseract-ocr-w64-setup-5.3.3-2024-05-16.exe 到目录 D:/app/tesseract
 * 2、解压语言模型包（tessdata_best-4.1.0.zip、tessdata-4.1.0.zip） 到目录 D:/app/tesseract
 *
 * # 语言模型
 * tesseract支持一百多种语言识别，可以从Traineddata语言模型说明下载页面选择自己想要识别的.traineddata格式的语言模型文件下载。
 *
 * tessdata语言模型包下载地址
 * https://tessdata.projectnaptha.com/
 *
 * （1）三大语言模型
 * tesseract在GitHub上的有三个独立的语言模型存储库tessdata、tessdata-best、tessdata-fast他们分别都存储了语言模型，他们的区别是：
 * 语言模型           如何训练得到的                        速度                    识别准确性                    是否支持旧版     是否支持再训练
 * tessdata         传统+LSTM（并整合tessdata-best）      比tessdata-best 更快     比tessdata-best 准确度稍低    支持           不支持
 * tessdata-best    仅LSTM（基于langdata）               最慢                     最准确                       不支持         支持
 * tessdata-fast    比tessdata-best更小的LSTM网络整合     最快的                    最不准确                     不支持         不支持
 *
 * 我测试识别一张多文字图片的时候，tessdata-best 效果最好但花了快10秒，tessdata 花了3秒但效果稍微差一点。你可以根据自己的需要去选择下载语言模型文件，
 * 我这里选择的是tessdata-best库里下载chi_sim.traineddata （简体中文）、eng.traineddata （英文模型）。
 *
 * （2）特殊语言模型
 * 识别图片里的各种方向的文字osd.traineddata模型
 * 如果你想识别图片里的各种数学公式、方程可以下载 equ.traineddata模型
 */
public final class Tess4jUtil {

    private static String p_datapath = "D:/app/tesseract/tessdata_best-4.1.0";

    public static final void setDatapath(final String datapath){
        p_datapath = datapath;
    }

    /**
     * 2024-05-24
     * 多线程批量转换
     * @param urlList
     * @param targetTxtPath
     */
    public static final void filelist2text(final List<String> urlList, final String targetTxtPath){
        filelist2text(p_datapath, urlList, targetTxtPath, 8);
    }

    /**
     * 2024-05-24
     * 多线程批量转换
     * @param urlList
     * @param targetTxtPath
     * @param threadNum
     */
    public static final void filelist2text(final List<String> urlList, final String targetTxtPath, final int threadNum){
        filelist2text(p_datapath, urlList, targetTxtPath, threadNum);
    }

    /**
     * 2024-05-24
     * 多线程批量转换
     * @param datapath
     * @param urlList
     * @param targetTxtPath
     */
    public static final void filelist2text(final String datapath, final List<String> urlList, final String targetTxtPath){
        filelist2text(datapath, urlList, targetTxtPath, 8);
    }

    /**
     * 2024-05-24
     * 多线程批量转换
     * @param datapath
     * @param urlList
     * @param targetTxtPath
     * @param threadNum
     */
    public static final void filelist2text(final String datapath, final List<String> urlList, final String targetTxtPath, final int threadNum){
        if(threadNum <= 1){
            file2textfileList(datapath, urlList, targetTxtPath);
            return;
        }
        //
        List<List<String>> groupList = ListUtil.split(urlList, threadNum);
        for(int i = 0; i < groupList.size(); i++){
            final List<String> urlList2 = groupList.get(i);
            ThreadUtil.start(new Business("Tess4jUtil_filelist2text_1", false){
                @Override
                public void process(){
                    file2textfileList(datapath, urlList2, targetTxtPath);
                }
            });
        }
    }

    /**
     * 2024-05-24
     * 批量转换
     * @param datapath
     * @param urlList
     * @param targetTxtPath
     * @return
     */
    public static final List<String> file2textfileList(final String datapath, final List<String> urlList, final String targetTxtPath){
        List<String> resultList = new ArrayList<>();
        for(int i = 0; i < urlList.size(); i++){
            String url = urlList.get(i);
            String name = FileUtil.getFileName(url, false);
            String targetTxtUrl = targetTxtPath + "/" + name + ".txt";
            if(FileUtil.exists(targetTxtUrl)){
                resultList.add(targetTxtPath);
                continue;
            }
            String result = file2textfile(datapath, url, targetTxtUrl);
            resultList.add(result);
        }
        return resultList;
    }

    /**
     * 2024-05-24
     * @param datapath
     * @param url
     * @param targetTxtUrl
     * @return
     */
    public static final String file2textfile(final String datapath, final String url, final String targetTxtUrl){
        StringBuilder stringBuilder = new StringBuilder();
        List<List<String>> list = file2text(datapath, url);
        for (int i = 0; i < list.size(); i++){
            List<String> tempList = list.get(i);
            for (int j = 0; j < tempList.size(); j++){
                String line = tempList.get(j);
                stringBuilder.append(line).append("\n");
            }
        }
        String result = stringBuilder.toString();
        if(!StringUtil.isBlank(result)){
            FileUtil.delete(targetTxtUrl);
            FileUtil.fileWriteNew(targetTxtUrl, result, false);
            return targetTxtUrl;
        }
        return targetTxtUrl;
    }

    /**
     * 2024-05-16
     * @param url
     * @return
     */
    public static final List<List<String>> file2text(final String url){
        List<List<String>> resultList = file2text(p_datapath, url);
        return resultList;
    }

    /**
     * 2024-05-16
     * @param datapath
     * @param url
     * @return
     */
    public static final List<List<String>> file2text(final String datapath, final String url){
        File file = new File(url);
        if(!file.exists()){
            return null;
        }
        if(!file.isFile()){
            return null;
        }
        //
        String fileType = FileUtil.getFileType(url);
        fileType = StringUtil.toLowerCase(fileType);
        //
        if(StringUtil.equalsOfOne(fileType, "jpg", "jpeg", "png", "gif")){
            List<List<String>> resultList = new ArrayList<>();
            List<String> list = p_img2list(url);
            resultList.add(list);
            return resultList;
        }
        //
        if(StringUtil.equals(fileType, "pdf")){
            List<String> imgUrlList = PdfUtil.pdf2jpg(url);
            if(CollectionUtil.isBlank(imgUrlList)){
                return null;
            }
            List<List<String>> resultList = new ArrayList<>();
            for(int i = 0; i < imgUrlList.size(); i++){
                String imgUrl = imgUrlList.get(i);
                List<String> list = p_img2list(datapath, imgUrl);
                resultList.add(list);
            }
            final String imgUrl = imgUrlList.get(0);
            final String imgPath = UrlUtil.getPathByUrl(imgUrl);
            FileUtil.delete(imgPath);
            return resultList;
        }
        //
        LogUtil.error("Tess4jUtil_file2text_2", "文件格式错误", datapath, url);
        return null;
    }

    /**
     * 2024-05-16
     * @param imgUrl
     * @return
     */
    private static final List<String> p_img2list(final String imgUrl){
        List<String> list = p_img2list(p_datapath, imgUrl);
        return list;
    }

    /**
     * 2024-05-16
     * @param datapath
     * @param imgUrl
     * @return
     */
    private static final List<String> p_img2list(final String datapath, final String imgUrl){
        try{
            File file = new File(imgUrl);
            BufferedImage bufferedImage = ImageIO.read(file);
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(datapath);  // 加载训练字体库文件
            tesseract.setLanguage("chi_sim");  // 设置为简体中文
            tesseract.setPageSegMode(1);  // 使用OSD进行自动页面分割以进行图像处理
            tesseract.setOcrEngineMode(1);  // 设置引擎模式是神经网络LSTM引擎
            String result = tesseract.doOCR(bufferedImage);
            result = StringUtil.deleteAll(result, " ");
            String[] array = result.split("\n");
            List<String> list = ListUtil.array2list(array);  // 单页结果
            return list;
        }catch(Throwable throwable){
            LogUtil.error("Tess4jUtil_p_img2list_t1", throwable, "图片转文字失败");
            return null;
        }
    }

}