package xc.crabapple.core.file.audio;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v1Tag;
import com.mpatric.mp3agic.Mp3File;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.File;

/**
 * 2020-03-16 5.0.0
 * mp3文件相关处理类
 * 配合mp3tag软件批量处理mp3文件
 * 相关maven依赖
 * <dependency>
 *     <groupId>com.mpatric</groupId>
 *     <artifactId>mp3agic</artifactId>
 *     <version>${mp3agic.version}</version>
 * </dependency>
 */
public final class Mp3Util {

    /**
     * 2020-03-16 5.0.0
     * 设置指定mp3的文件各属性值
     * 为了确保原始数据对新值产生影响，如乱码问题，可以用mp3tag软件把原mp3文件的各属性值，特别是标题都清空掉，本程序仅负责更新标题即可。
     * @param url 指定的mp3文件绝对路径
     * @param toPath 输出目录，如d:/app/tmp
     */
    public static final void setMp3Attribute(final String url, final String toPath){
        String urlLocal = UrlUtil.getStandardPath(url);
        String toPathLocal = UrlUtil.getStandardPath(toPath);
        FileUtil.dirMake(toPathLocal);
        File file = new File(urlLocal);
        setMp3Attribute(file, toPathLocal);
    }

    /**
     * 2020-03-16 5.0.0
     * 设置指定mp3的文件各属性值
     * 为了确保原始数据对新值产生影响，如乱码问题，可以用mp3tag软件把原mp3文件的各属性值，特别是标题都清空掉，本程序仅负责更新标题即可。
     * @param file 指定的mp3文件file对象
     * @param toPath 输出目录，如d:/app/tmp
     */
    public static final void setMp3Attribute(final File file, final String toPath){
        if(file == null){
            LogUtil.warn("xc_crabapple_Mp3Util_setMp3Attribute_1");
            return;
        }
        if(!file.exists()){
            LogUtil.warn("xc_crabapple_Mp3Util_setMp3Attribute_2");
            return;
        }
        if(!file.isFile()){
            LogUtil.warn("xc_crabapple_Mp3Util_setMp3Attribute_3");
            return;
        }
        String fileType = FileUtil.getFileType(file);
        if(StringUtil.isBlank(fileType)){
            LogUtil.warn("xc_crabapple_Mp3Util_setMp3Attribute_4");
            return;
        }
        fileType = StringUtil.toLowerCase(fileType);
        if(!"mp3".equals(fileType)){
            LogUtil.warn("xc_crabapple_Mp3Util_setMp3Attribute_5");
            return;
        }
        if(StringUtil.isBlank(toPath)){
            LogUtil.warn("xc_crabapple_Mp3Util_setMp3Attribute_6");
            return;
        }
        if(!FileUtil.exists(toPath)){
            LogUtil.warn("xc_crabapple_Mp3Util_setMp3Attribute_7");
            return;
        }
        String toPathLocal = UrlUtil.getStandardPath(toPath);
        FileUtil.dirMake(toPathLocal);
        //
        p_setMp3Attribute(file, toPathLocal);
    }

    /**
     * 2020-03-16 5.0.0
     * 设置指定mp3的文件各属性值
     * 为了确保原始数据对新值产生影响，如乱码问题，可以用mp3tag软件把原mp3文件的各属性值，特别是标题都清空掉，本程序仅负责更新标题即可。
     * @param file 指定的mp3文件file对象
     * @param toPath 输出目录，如d:/app/tmp
     */
    private static final void p_setMp3Attribute(final File file, final String toPath){
        try {
            Mp3File mp3file = new Mp3File(file);
            ID3v1 id3v1Tag;
            if(mp3file.hasId3v2Tag()){
                id3v1Tag =  mp3file.getId3v2Tag();
            }else if(mp3file.hasId3v1Tag()){
                id3v1Tag =  mp3file.getId3v1Tag();
            }else{
                id3v1Tag = new ID3v1Tag();
                mp3file.setId3v1Tag(id3v1Tag);
            }
            //
            String title = FileUtil.getFileName(file, false);
            //title = "melody_" + title;
            byte[] bytes = title.getBytes();
            title = new String(bytes, "UTF-8");
            id3v1Tag.setTitle(title);  //标题
            //id3v1Tag.setTrack("5");
            //id3v1Tag.setArtist("demo2020-03-16");  //参与创作的艺术家
            //id3v1Tag.setAlbum("demo2020-03-16");  //唱片集
            //id3v1Tag.setYear("2020");  //年
            //id3v1Tag.setGenre(12);  //项目类型
            //id3v1Tag.setComment("");
            String toUrl = toPath + "/" + file.getName();
            FileUtil.fileDelete(toUrl);
            mp3file.save(toUrl);
        }catch(Throwable throwable){
            String fromUrl = FileUtil.getFileUrl(file);
            String fileName = FileUtil.getFileName(file, true);
            String toUrl = toPath + "/" + fileName;
            FileUtil.fileDelete(toUrl);
            FileUtil.copy(fromUrl, toUrl);
            LogUtil.error("Mp3Util_p_setMp3Attribute_t1", throwable, fromUrl);
        }
    }

}