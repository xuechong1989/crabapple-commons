package xc.crabapple.core.file.video;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.ListUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.timer.TimerUtil;
import xc.crabapple.core.url.UrlUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 2025-02-08
 * 视频文件处理
 */
public final class VideoUtil {

    public static final void video2mp4Path(final String fromPath, final String toPath){
        video2mp4Path(fromPath, toPath, true);
    }

    public static final void video2mp4Path(final String fromPath, final String toPath, final boolean useThread){
        String fromPathLocal = UrlUtil.getStandardPath(fromPath);
        String toPathLocal = UrlUtil.getStandardPath(toPath);
        if(StringUtil.isBlank(fromPathLocal, toPathLocal)){
            LogUtil.error("VideoUtil_video2mp4Path_1", "文件不存在", fromPathLocal, toPathLocal);
            return;
        }
        FileUtil.dirMake(toPathLocal);
        //
        Map<String, List<String>> map = FileUtil.getDirChildrenFirst(fromPathLocal);
        List<String> urlList = map.get("file");
        if(CollectionUtil.isBlank(urlList)){
            return;
        }
        //
        if(useThread){
            final String t_toPath = toPathLocal;
            List<List<String>> splitList = ListUtil.splitList(urlList, 8);
            for(int j = 0; j < splitList.size(); j++){
                final List<String> t_urlList = splitList.get(j);
                new Thread(){
                    @Override
                    public void run(){
                        p_video2mp4Path(t_urlList, t_toPath);
                    }
                }.start();
            }
            return;
        }
        //
        p_video2mp4Path(urlList, toPathLocal);
    }

    private static final void p_video2mp4Path(final List<String> urlList, final String toPath){
        for(int i = 0; i < urlList.size(); i++){
            String fromUrl = urlList.get(i);
            boolean isVideo = FileUtil.isVideo(fromUrl);
            if(!isVideo){
                continue;
            }
            String fromName = UrlUtil.getFileName(fromUrl, false);
            String toUrl = toPath + "/" + fromName + ".mp4";
            if(FileUtil.exists(toUrl) && FileUtil.isRealFile(toUrl)){
                FileUtil.delete(toUrl);
            }
            video2mp4(fromUrl, toUrl);
        }
    }

    /**
     * 2025-02-08
     * 其他视频格式转换为mp4格式
     * @param fromUrl
     * @param toUrl
     */
    public static final void video2mp4(final String fromUrl, final String toUrl) {
        FFmpegFrameGrabber ffmpegFrameGrabber = null;
        FFmpegFrameRecorder ffmpegFrameRecorder = null;
        try {
            File fromFile = new File(fromUrl);
            ffmpegFrameGrabber = new FFmpegFrameGrabber(fromFile);
            ffmpegFrameGrabber.start();
            int width = ffmpegFrameGrabber.getImageWidth();
            int height = ffmpegFrameGrabber.getImageHeight();
            File toFile = new File(toUrl);
            ffmpegFrameRecorder = new FFmpegFrameRecorder(toFile, width, height);
            ffmpegFrameRecorder.setAudioBitrate(ffmpegFrameGrabber.getAudioBitrate());
            ffmpegFrameRecorder.setAudioChannels(1);
//            ffmpegFrameRecorder.setAudioCodec(ffmpegFrameGrabber.getAudioCodec());
//            ffmpegFrameRecorder.setAudioCodecName(ffmpegFrameGrabber.getAudioCodecName());
            ffmpegFrameRecorder.setAudioMetadata(ffmpegFrameGrabber.getAudioMetadata());
            ffmpegFrameRecorder.setAudioOptions(ffmpegFrameGrabber.getAudioOptions());
            ffmpegFrameRecorder.setAudioQuality(ffmpegFrameGrabber.getAudioBitrate());
            ffmpegFrameRecorder.setAudioSideData(ffmpegFrameGrabber.getAudioSideData());
            ffmpegFrameRecorder.setVideoBitrate(ffmpegFrameGrabber.getVideoBitrate());
            ffmpegFrameRecorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            ffmpegFrameRecorder.setVideoCodecName(ffmpegFrameGrabber.getVideoCodecName());
            ffmpegFrameRecorder.setVideoMetadata(ffmpegFrameGrabber.getVideoMetadata());
            ffmpegFrameRecorder.setVideoOptions(ffmpegFrameGrabber.getVideoOptions());
            ffmpegFrameRecorder.setVideoQuality(ffmpegFrameGrabber.getVideoBitrate());
            ffmpegFrameRecorder.setVideoSideData(ffmpegFrameGrabber.getVideoSideData());
            ffmpegFrameRecorder.setImageWidth(ffmpegFrameGrabber.getImageWidth());
            ffmpegFrameRecorder.setImageHeight(ffmpegFrameGrabber.getImageHeight());
            ffmpegFrameRecorder.setAspectRatio(ffmpegFrameGrabber.getAspectRatio());
            ffmpegFrameRecorder.setDisplayRotation(ffmpegFrameGrabber.getDisplayRotation());
            ffmpegFrameRecorder.setFormat("mp4");
            ffmpegFrameRecorder.setFrameNumber(ffmpegFrameGrabber.getFrameNumber());
            ffmpegFrameRecorder.setFrameRate(ffmpegFrameGrabber.getFrameRate());
            ffmpegFrameRecorder.setTimestamp(TimerUtil.getStamp());
            ffmpegFrameRecorder.start();
            while(true){
                Frame frame = ffmpegFrameGrabber.grab();
                if(frame == null){
                    break;
                }
                ffmpegFrameRecorder.record(frame);
            }
        } catch (Throwable throwable) {
            LogUtil.error("VideoUtil_video2mp4_t1", throwable, fromUrl, toUrl);
        } finally {
            try {
                if (ffmpegFrameRecorder!= null) {
                    ffmpegFrameRecorder.stop();
                    ffmpegFrameRecorder.release();
                }
                if (ffmpegFrameGrabber!= null) {
                    ffmpegFrameGrabber.stop();
                    ffmpegFrameGrabber.release();
                }
            } catch (Throwable throwable) {
                LogUtil.error("VideoUtil_video2mp4_t2", throwable, fromUrl, toUrl);
            }
        }
    }

    /**
     * 2025-02-08
     * @param fromPath
     * @param watermarkUrl
     * @param toPath
     */
    public static final void addWatermarkPath(final String fromPath, final String watermarkUrl, final String toPath){
        addWatermarkPath(fromPath, watermarkUrl, 1F, toPath, true);
    }

    /**
     * 2025-02-08
     * @param fromPath
     * @param watermarkUrl
     * @param watermarkAlpha
     * @param toPath
     */
    public static final void addWatermarkPath(final String fromPath, final String watermarkUrl, final float watermarkAlpha, final String toPath){
        addWatermarkPath(fromPath, watermarkUrl, watermarkAlpha, toPath, true);
    }

    /**
     * 2025-02-08
     * @param fromPath
     * @param watermarkUrl
     * @param watermarkAlpha
     * @param toPath
     * @param useThread
     */
    public static final void addWatermarkPath(final String fromPath, final String watermarkUrl, final float watermarkAlpha, final String toPath, final boolean useThread){
        String fromPathLocal = UrlUtil.getStandardPath(fromPath);
        String watermarkUrlLocal = UrlUtil.getStandardPath(watermarkUrl);
        float watermarkAlphaLocal = watermarkAlpha;
        if(watermarkAlphaLocal <= 0F){
            watermarkAlphaLocal = 1F;
        }
        String toPathLocal = UrlUtil.getStandardPath(toPath);
        if(StringUtil.isBlank(fromPathLocal, watermarkUrlLocal, toPathLocal)){
            LogUtil.error("VideoUtil_addWatermarkFromPath_1", "文件不存在", fromPathLocal, watermarkUrlLocal, watermarkAlphaLocal, toPathLocal);
            return;
        }
        FileUtil.dirMake(toPathLocal);
        //
        Map<String, List<String>> map = FileUtil.getDirChildrenFirst(fromPathLocal);
        List<String> urlList = map.get("file");
        if(CollectionUtil.isBlank(urlList)){
            return;
        }
        //
        if(useThread){
            final String t_watermarkUrl = watermarkUrlLocal;
            final float t_watermarkAlpha = watermarkAlphaLocal;
            final String t_toPath = toPathLocal;
            List<List<String>> splitList = ListUtil.splitList(urlList, 8);
            for(int j = 0; j < splitList.size(); j++){
                final List<String> t_urlList = splitList.get(j);
                new Thread(){
                    @Override
                    public void run(){
                        p_addWatermarkPath(t_urlList, t_watermarkUrl, t_watermarkAlpha, t_toPath);
                    }
                }.start();
            }
            return;
        }
        //
        p_addWatermarkPath(urlList, watermarkUrlLocal, watermarkAlphaLocal, toPathLocal);
    }

    /**
     * 2025-02-08
     * @param urlList
     * @param watermarkUrl
     * @param watermarkAlpha
     * @param toPath
     */
    private static final void p_addWatermarkPath(final List<String> urlList, final String watermarkUrl, final float watermarkAlpha, final String toPath){
        for(int i = 0; i < urlList.size(); i++){
            String fromUrl = urlList.get(i);
            boolean isVideo = FileUtil.isVideo(fromUrl);
            if(!isVideo){
                continue;
            }
            String fromName = UrlUtil.getFileName(fromUrl, true);
            String toUrl = toPath + "/" + fromName;
            if(FileUtil.exists(toUrl) && FileUtil.isRealFile(toUrl)){
                FileUtil.delete(toUrl);
            }
            addWatermark(fromUrl, watermarkUrl, watermarkAlpha, toUrl);
        }
    }

    /**
     * 2025-02-08
     * @param fromUrl
     * @param watermarkUrl
     * @param toUrl
     */
    public static final void addWatermark(final String fromUrl, final String watermarkUrl, final String toUrl){
        addWatermark(fromUrl, watermarkUrl, 1F, toUrl);
    }

    /**
     * 2025-02-08
     * 添加水印，通过当前主线程实现
     * 支持视频格式：mov,mp4,m4a,3gp,3g2,mj2
     * @param fromUrl
     * @param watermarkUrl
     * @param toUrl
     */
    public static final void addWatermark(final String fromUrl, final String watermarkUrl, final float watermarkAlpha, final String toUrl) {
        String fromUrlLocal = UrlUtil.getStandardPath(fromUrl);
        String watermarkUrlLocal = UrlUtil.getStandardPath(watermarkUrl);
        float watermarkAlphaLocal = watermarkAlpha;
        if(watermarkAlphaLocal <= 0F){
            watermarkAlphaLocal = 1F;
        }
        String toUrlLocal = UrlUtil.getStandardPath(toUrl);
        if(StringUtil.isBlank(fromUrlLocal, watermarkUrlLocal, toUrlLocal)){
            LogUtil.error("VideoUtil_addWatermark_1", "文件不存在", fromUrlLocal, watermarkUrlLocal, watermarkAlphaLocal, toUrlLocal);
            return;
        }
        //
        FFmpegFrameGrabber ffmpegFrameGrabber = null;
        FFmpegFrameRecorder ffmpegFrameRecorder = null;
        try {
            File watermarkFile = new File(watermarkUrlLocal);
            BufferedImage bufferedImage = ImageIO.read(watermarkFile);
            ffmpegFrameGrabber = new FFmpegFrameGrabber(fromUrlLocal);
            ffmpegFrameGrabber.start();
            ffmpegFrameRecorder = new FFmpegFrameRecorder(toUrlLocal, ffmpegFrameGrabber.getImageWidth(), ffmpegFrameGrabber.getImageHeight());
            Map<String, String> audioMetadataMap = ffmpegFrameGrabber.getAudioMetadata();
            if(audioMetadataMap == null){
                audioMetadataMap = new HashMap<>();
            }
            audioMetadataMap.put("author", "雪地漫步2020");
            audioMetadataMap.put("creation_time", TimerUtil.now());
            Map<String, String> videoMetadataMap = ffmpegFrameGrabber.getVideoMetadata();
            if(videoMetadataMap == null){
                videoMetadataMap = new HashMap<>();
            }
            videoMetadataMap.put("author", "雪地漫步2020");
            videoMetadataMap.put("creation_time", TimerUtil.now());
            ffmpegFrameRecorder.setAudioBitrate(ffmpegFrameGrabber.getAudioBitrate());
            ffmpegFrameRecorder.setAudioChannels(ffmpegFrameGrabber.getAudioChannels());
            ffmpegFrameRecorder.setAudioCodec(ffmpegFrameGrabber.getAudioCodec());
            ffmpegFrameRecorder.setAudioCodecName(ffmpegFrameGrabber.getAudioCodecName());
            ffmpegFrameRecorder.setAudioMetadata(audioMetadataMap);
            ffmpegFrameRecorder.setAudioOptions(ffmpegFrameGrabber.getAudioOptions());
            ffmpegFrameRecorder.setAudioQuality(ffmpegFrameGrabber.getAudioBitrate());
            ffmpegFrameRecorder.setAudioSideData(ffmpegFrameGrabber.getAudioSideData());
            ffmpegFrameRecorder.setVideoBitrate(ffmpegFrameGrabber.getVideoBitrate());
            ffmpegFrameRecorder.setVideoCodec(ffmpegFrameGrabber.getVideoCodec());
            ffmpegFrameRecorder.setVideoCodecName(ffmpegFrameGrabber.getVideoCodecName());
            ffmpegFrameRecorder.setVideoMetadata(videoMetadataMap);
            ffmpegFrameRecorder.setVideoOptions(ffmpegFrameGrabber.getVideoOptions());
            ffmpegFrameRecorder.setVideoQuality(ffmpegFrameGrabber.getVideoBitrate());
            ffmpegFrameRecorder.setVideoSideData(ffmpegFrameGrabber.getVideoSideData());
            ffmpegFrameRecorder.setImageWidth(ffmpegFrameGrabber.getImageWidth());
            ffmpegFrameRecorder.setImageHeight(ffmpegFrameGrabber.getImageHeight());
            ffmpegFrameRecorder.setAspectRatio(ffmpegFrameGrabber.getAspectRatio());
            ffmpegFrameRecorder.setDisplayRotation(ffmpegFrameGrabber.getDisplayRotation());
            ffmpegFrameRecorder.setFormat(ffmpegFrameGrabber.getFormat());
            ffmpegFrameRecorder.setFrameNumber(ffmpegFrameGrabber.getFrameNumber());
            ffmpegFrameRecorder.setFrameRate(ffmpegFrameGrabber.getFrameRate());
            ffmpegFrameRecorder.setTimestamp(TimerUtil.getStamp());
//            ffmpegFrameRecorder.setSampleFormat(ffmpegFrameGrabber.getSampleFormat());
//            ffmpegFrameRecorder.setPixelFormat(ffmpegFrameGrabber.getPixelFormat());
            ffmpegFrameRecorder.start();
            Java2DFrameConverter java2DFrameConverter = new Java2DFrameConverter();
            while (true) {
                Frame frame = ffmpegFrameGrabber.grab();
                if(frame == null){
                    break;
                }
                if (frame.image != null) {
                    BufferedImage bufferedImage2 = java2DFrameConverter.convert(frame);
                    Graphics2D graphics2D = bufferedImage2.createGraphics();
                    int x = bufferedImage2.getWidth() - bufferedImage.getWidth() - 10;
                    int y = bufferedImage2.getHeight() - bufferedImage.getHeight() - 10;
                    AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, watermarkAlphaLocal);
                    graphics2D.setComposite(alphaComposite);
                    graphics2D.drawImage(bufferedImage, x, y, null);
                    graphics2D.dispose();
                    Frame processedFrame = java2DFrameConverter.convert(bufferedImage2);
                    ffmpegFrameRecorder.record(processedFrame);
                } else {
                    ffmpegFrameRecorder.record(frame);
                }
            }
            ffmpegFrameRecorder.stop();
            ffmpegFrameRecorder.release();
            ffmpegFrameGrabber.stop();
            ffmpegFrameGrabber.release();
        }catch (Throwable throwable){
            LogUtil.error("VideoUtil_addWatermark_t1", throwable, fromUrlLocal, watermarkUrlLocal, watermarkAlphaLocal, toUrlLocal);
        }finally {
            IOUtil.close(ffmpegFrameRecorder, ffmpegFrameGrabber);
        }
    }

}