package xc.crabapple.core.chinese;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 2017-06-22 1.0.0
 */
public final class ChineseUtil {

    /**
     * 2022-10-15
     * 生成所有的汉字，包括简体中文和繁体中文
     */
    public static final Map<String, String> getAll() {
        Map<String, String> map = new HashMap<>();
        for(char c = '\u4e00', i = 0; c <= '\u9fa5'; c++, i++){
            String chineseStr = StringUtil.char2str(c);
            map.put(chineseStr, chineseStr);
        }
        return map;
    }

    /**
     * 2017-06-22 1.0.0
     * 随机生成一个中文
     */
    public static final String getRandomCH(final Integer n) {
        String result = "";
        for (int i = 0; i < n; i++) {
            int hightPos, lowPos;  //定义高低位
            Random random = new Random();
            hightPos = (176 + Math.abs(random.nextInt(39)));  //获取高位值
            lowPos = (161 + Math.abs(random.nextInt(93)));  //获取低位值
            byte[] b = new byte[2];
            b[0] = ((Integer) hightPos).byteValue();
            b[1] = ((Integer) lowPos).byteValue();
            try {
                String sss = new String(b, "GBK");  //转成中文
                result += sss;
            }catch(Throwable throwable){
                LogUtil.error("ChineseUtil_getRandomCH_t1", throwable, n);
            }
        }
        return result;
    }

}
