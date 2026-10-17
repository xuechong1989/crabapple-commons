package xc.crabapple.core.chinese.pinyin;

import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.regex.RegexUtil;
import xc.crabapple.core.url.UrlUtil;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 2022-08-23
 * 2025-02-07 代码重构
 * 2025-09-24 声调转换，中文汉字首字母提取时去除声调
 * 中文汉字拼音处理工具类
 */
public final class PinyinUtil {

	private static Map<String, String> p_map;
	private static Map<String, String> p_toneMap;

	/**
	 * 2020-06-17 6.2.0
	 * 中文汉字转为拼音
	 * 示例：
	 *   ("中华人民共和国") ==> zhōng huá rén mín gòng hé guó
	 *   ("中华ABC人民共和国") ==> zhōng huá ABC rén mín gòng hé guó
	 * @param chinese
	 */
	public static final String getPinyin(final String chinese){
		p_init();
		//
		if(StringUtil.isBlank(chinese)){
			return chinese;
		}
		//
		if(p_map.containsKey(chinese)){
			String pinyin = p_map.get(chinese);
			pinyin = StringUtil.replaceAll(pinyin, ",", " ");
			pinyin = StringUtil.replaceAll(pinyin, "  ", " ");
			pinyin = StringUtil.replaceAll(pinyin, "   ", " ");
			pinyin = StringUtil.replaceAll(pinyin, "    ", " ");
			pinyin = StringUtil.trim(pinyin);
			return pinyin;
		}
		// 分词
		String effectiveChinese = p_fenci(chinese, 1);
		effectiveChinese = p_fenci(effectiveChinese, 2);
		//
		final StringBuilder stringBuilder = new StringBuilder();
		boolean flag = true;
		for(int i = 0; i < effectiveChinese.length(); i++){
			final char chr = effectiveChinese.charAt(i);
			final String sss = StringUtil.char2str(chr);
			if(RegexUtil.isChineseOnly(sss)){
				flag = true;
				String pinyin = "xxx";
				if(p_map.containsKey(sss)){
					pinyin = p_map.get(sss);
				}
				if(StringUtil.containOf(pinyin, " ")){
					String[] array = StringUtil.split(pinyin, " ");
					pinyin = array[0];
					pinyin = StringUtil.deleteAll(pinyin, " ");
				}
				if(stringBuilder.length() <= 0){
					stringBuilder.append(pinyin);
				}else{
					stringBuilder.append(" ").append(pinyin);
				}
				continue;
			}
			//
			if(!flag || stringBuilder.length() <= 0){
				stringBuilder.append(sss);
			}else{
				stringBuilder.append(" ").append(sss);
			}
			flag = false;
		}
		String pinyin = stringBuilder.toString();
		pinyin = StringUtil.replaceAll(pinyin, ",", " ");
		pinyin = StringUtil.replaceAll(pinyin, "  ", " ");
		pinyin = StringUtil.replaceAll(pinyin, "   ", " ");
		pinyin = StringUtil.replaceAll(pinyin, "    ", " ");
		pinyin = StringUtil.trim(pinyin);
		return pinyin;
	}

	/**
	 * 2025-02-07
	 * 获取所有汉字集合内容
	 * @return
	 */
	public static final Map<String, String> getMap(){
		return p_map;
	}

	/**
	 * 2025-02-07
	 * 2025-02-18 引入读取jar文件内部的文件内容
	 * 获取所有汉字集合内容
	 * @return
	 */
	public static final List<String> getList(){
		List<String> list = null;
		try{
			final String exJarRootPath = UrlUtil.getExJarRootPath();
			if(StringUtil.lastsWith(exJarRootPath, ".jar")){
				JarFile jarFile = new JarFile(exJarRootPath);
				JarEntry jarEntry = jarFile.getJarEntry("xc/crabapple/core/chinese/pinyin/pinyin.txt");
				InputStream inputStream = jarFile.getInputStream(jarEntry);
				list = IOUtil.inputstream2list(inputStream, true);
			}else{
				// 直接加载本地文件
			}
		}catch (Throwable throwable){
			// LogUtil.error("PinyinUtil_getList_t1", throwable);
			// 直接加载本地文件
			list = null;
		}
		// 直接加载本地文件
		if(CollectionUtil.isBlank(list)){
			URL url2 = PinyinUtil.class.getResource("");
			String path = url2.getPath();
			path = UrlUtil.getStandardPath(path);
			String txtUrl = path + "/pinyin.txt";
			txtUrl = UrlUtil.getStandardPath(txtUrl);
			list = FileUtil.fileReadList(txtUrl);
		}
		//
		if(CollectionUtil.isBlank(list)){
			LogUtil.error("PinyinUtil_getList_1", "中文拼音资源文件未加载");
			return null;
		}
		//
		List<String> finalList = new ArrayList<>();
		final int size = list.size();
		for(int i = 0; i < size; i++) {
			String line = list.get(i);
			line = StringUtil.trim(line);
			line = StringUtil.getPureStr(line);
			if (StringUtil.isBlank(line)) {
				continue;
			}
			finalList.add(line);
		}
		return finalList;
	}

	/**
	 * 2025-02-07
	 * 获取所有不是多音字的汉字集合内容
	 * @return
	 */
	public static final Map<String, String> getSingleMap(){
		final Map<String, String> singleMap = new HashMap<>();
		final List<String> multiList = getSingleList();
		for(int i = 0; i < multiList.size(); i++){
			final String line = multiList.get(i);
			final String[] array = StringUtil.split(line, "=");
			final String hanzi = array[0];
			final String pinyin = array[1];
			singleMap.put(hanzi, pinyin);
		}
		return singleMap;
	}

	/**
	 * 2025-02-07
	 * 获取所有不是多音字的汉字集合内容
	 * @return
	 */
	public static final List<String> getSingleList(){
		final List<String> singleList = new ArrayList<>();
		final List<String> list = getList();
		for(int i = 0; i < list.size(); i++) {
			String line = list.get(i);
			String[] array = StringUtil.split(line, "=");
			String hanzi = array[0];
			hanzi = StringUtil.deleteAll(hanzi, " ");
			int length = hanzi.length();
			if(length != 1){
				continue;
			}
			String pinyin = array[1];
			pinyin = StringUtil.trim(pinyin);
			if(StringUtil.containOf(pinyin, " ")){
				continue;
			}
			singleList.add(line);
		}
		return singleList;
	}

	/**
	 * 2025-02-07
	 * 获取所有多音字的汉字集合内容
	 * @return
	 */
	public static final Map<String, String> getMultiMap(){
		final Map<String, String> multiMap = new HashMap<>();
		final List<String> multiList = getMultiList();
		for(int i = 0; i < multiList.size(); i++){
			final String line = multiList.get(i);
			final String[] array = StringUtil.split(line, "=");
			final String hanzi = array[0];
			final String pinyin = array[1];
			multiMap.put(hanzi, pinyin);
		}
		return multiMap;
	}

	/**
	 * 2025-02-07
	 * 获取所有多音字的汉字集合内容
	 * @return
	 */
	public static final List<String> getMultiList(){
		final List<String> multiList = new ArrayList<>();
		final List<String> list = getList();
		for(int i = 0; i < list.size(); i++) {
			String line = list.get(i);
			String[] array = StringUtil.split(line, "=");
			String hanzi = array[0];
			hanzi = StringUtil.deleteAll(hanzi, " ");
			int length = hanzi.length();
			if(length != 1){
				continue;
			}
			String pinyin = array[1];
			pinyin = StringUtil.trim(pinyin);
			if(!StringUtil.containOf(pinyin, " ")){
				continue;
			}
			multiList.add(line);
		}
		return multiList;
	}

	/**
	 * 2025-02-04
	 * @param chinese
	 * @param startIndex 值为1表示[0-1, 2-3, ...]，值为2表示[1-2, 3-4, ...]
	 * @return
	 */
	private static final String p_fenci(final String chinese, final int startIndex){
		final int length = chinese.length();
		if(length <= 2){
			return chinese;
		}
		final StringBuilder stringBuilder = new StringBuilder();
		int effectiveStartIndex = startIndex;
		if(effectiveStartIndex == 2){
			char preChr = chinese.charAt(0);
			stringBuilder.append(preChr);
		}
		while(true){
			if(effectiveStartIndex >= length){
				break;
			}
			char preChr = chinese.charAt(effectiveStartIndex - 1);
			String preStr = preChr + "";
			char curChr = chinese.charAt(effectiveStartIndex);
			String curStr = curChr + "";
			String ci = preStr + curStr;
			if(p_map.containsKey(ci)){
				ci = p_map.get(ci);
				ci = " " + ci + " ";
			}
			stringBuilder.append(ci);
			effectiveStartIndex += 2;
		}
		if(effectiveStartIndex == length){
			char preChr = chinese.charAt(length - 1);
			stringBuilder.append(preChr);
		}
		String pinyin = stringBuilder.toString();
		pinyin = StringUtil.trim(pinyin);
		return pinyin;
	}

	/**
	 * 2025-09-24
	 * 带声调的字符转为不带声调的字符
	 * @param tone ā á ǎ à ē é ě è ī í ǐ ì ō ó ǒ ò ū ú ǔ ù Ā Á Ǎ À Ē É Ě È Ī Í Ǐ Ì Ō Ó Ǒ Ò Ū Ú Ǔ Ù
	 * @return
	 */
	public static final String tone2none(final String tone){
		String none = tone2none(tone, false);
		return none;
	}

	/**
	 * 2025-09-24
	 * 带声调的字符转为不带声调的字符
	 * @param tone ā á ǎ à ē é ě è ī í ǐ ì ō ó ǒ ò ū ú ǔ ù Ā Á Ǎ À Ē É Ě È Ī Í Ǐ Ì Ō Ó Ǒ Ò Ū Ú Ǔ Ù
	 * @param toUpperCase 返回值是否强制转为大写字符
	 * @return
	 */
	public static final String tone2none(final String tone, final boolean toUpperCase){
		if(p_toneMap == null){
			p_toneMap = new HashMap<>(15);
			p_toneMap.put("ā", "a");
			p_toneMap.put("á", "a");
			p_toneMap.put("ǎ", "a");
			p_toneMap.put("à", "a");
			p_toneMap.put("ē", "e");
			p_toneMap.put("é", "e");
			p_toneMap.put("ě", "e");
			p_toneMap.put("è", "e");
			p_toneMap.put("ī", "i");
			p_toneMap.put("í", "i");
			p_toneMap.put("ǐ", "i");
			p_toneMap.put("ì", "i");
			p_toneMap.put("ō", "o");
			p_toneMap.put("ó", "o");
			p_toneMap.put("ǒ", "o");
			p_toneMap.put("ò", "o");
			p_toneMap.put("ū", "u");
			p_toneMap.put("ú", "u");
			p_toneMap.put("ǔ", "u");
			p_toneMap.put("ù", "u");
		}
		String effectiveTone = StringUtil.trim(tone);
		// 小写
		if(p_toneMap.containsKey(effectiveTone)){
			String none = p_toneMap.get(effectiveTone);
			if(toUpperCase){
				none = none.toUpperCase();
			}
			return none;
		}
		// 大写
		effectiveTone = effectiveTone.toLowerCase();
		if(p_toneMap.containsKey(effectiveTone)){
			String none = p_toneMap.get(effectiveTone);
			none = none.toUpperCase();
			return none;
		}
		//
		return effectiveTone;
	}

	/**
	 * 2025-09-24
	 * 获取指定中文汉字的首字母
	 * @param chinese
	 * @return
	 */
	public static final String getPinyinFirst(final String chinese){
		String first = getPinyinFirst(chinese, false);
		return first;
	}

	/**
	 * 2020-06-17 6.2.0
	 * 2025-02-18 代码重构
	 * 获取指定中文汉字的首字母
	 * 示例：
	 *   ("中华人民共和国") ==> zhrmghg
	 *   ("中华ABC人民共和国") ==> zhABCrmghg
	 *   ("声像档案") ==> sxda
	 *   ("西安欧洲", true) ==> XAOZ
	 * @param chinese
	 * @param toUpperCase 返回值是否强制转为大写字符
	 */
	public static final String getPinyinFirst(final String chinese, final boolean toUpperCase){
		p_init();
		//
		if(StringUtil.isBlank(chinese)){
			return chinese;
		}
		//
		StringBuilder stringBuilder = new StringBuilder();
		for(int i = 0; i < chinese.length(); i++) {
			final char chr = chinese.charAt(i);
			final String sss = StringUtil.char2str(chr);
			if(RegexUtil.isChineseOnly(sss) || StringUtil.equalsOfOne(sss, "〇")){
				String pinyin = getPinyin(sss);
				if(StringUtil.isBlank(pinyin)){
					stringBuilder.append(" ");
					continue;
				}
				char chr2 = pinyin.charAt(0);
				String sss2 = StringUtil.char2str(chr2);
				sss2 = tone2none(sss2, false);
				stringBuilder.append(sss2);
				continue;
			}
			stringBuilder.append(sss);
		}
		String result = stringBuilder.toString();
		if(toUpperCase){
			result = result.toUpperCase();
		}
		return result;
	}

	/**
	 * 2020-06-17 6.2.0
	 * 初始化
	 */
	private static final void p_init(){
		if(p_map != null){
			return;
		}
		List<String> list = getList();
		final int size = list.size();
		p_map = new HashMap<>(size);
		//
		for(int i = 0; i < size; i++){
			String line = list.get(i);
			line = StringUtil.trim(line);
			line = StringUtil.getPureStr(line);
			if (StringUtil.isBlank(line)) {
				continue;
			}
			if(!StringUtil.containOf(line, "=")){
				LogUtil.error("PinyinUtil_p_init_1", "中文拼音资源文件格式错误", line);
				return;
			}
			String[] array = StringUtil.split(line, "=");
			String hanzi = array[0];
			hanzi = StringUtil.deleteAll(hanzi, " ");
			String pinyin = array[1];
			pinyin = StringUtil.trim(pinyin);
			p_map.put(hanzi, pinyin);
		}
	}

}