package xc.crabapple.core.encoder;

import java.util.ArrayList;
import java.util.List;
import xc.crabapple.core.collection.ListUtil;
import xc.crabapple.core.collection.StringUtil;

/**
 * 2023-07-30
 * ASCII码使用指定的7位或8位的二进制数组合来表示128或256种可能的字符。 (128=2^7)
 * 标准ASCII码也叫基础ASCII码，使用7位二进制数（最高的1位二进制为0）来表示所有的大写和小写字母，数字0到9、标点符号，
 * 以及在美式英语中使用的特殊控制符号。具体对照情况如下表，这128个字符可以分为以下两种：（使用ASCII码对应的十进制数表示顺序）
 * 扩展ASCII码是用8位二进制数表示的，它在标准ASCII码的表的基础上增加了128个字符，一共可以表示256个字符。
 *
 * 1、非打印控制字符：[0,32]和127，共33个，这33个是控制字符或通信专用字符。用来控制打印机等外围设备。127表示删除命令。
 * 2、打印字符：[33,126]表示打印字符，共94个，这些字符都是在电脑键盘上可以找到的。下面是常用的asciiCode码（用十进制数表示）：
 *
 * 【非打印控制字符】
 * 0空字符\o
 * 1开始标题
 * 2正文开始
 * 3正文结束
 * 4传输结束
 * 5请求
 * 6收到通知/响应
 * 7响铃\a
 * 8退格\b
 * 9水平制表符\t
 * 10换行符\n
 * 11垂直制表符\v
 * 12换页符f
 * 13回车符\r
 * 14不用切换
 * 15启用切换
 * 16数据链路转义
 * 17设备控制 1
 * 18设备控制 2
 * 19设备控制 3
 * 20设备控制 4
 * 21拒绝接收/无响应
 * 22同步空闲
 * 23传输块结束
 * 24取消
 * 25已到介质末端/介质存储已满
 * 26替补/替换
 * 27溢出/逃离/取消
 * 28文件分隔符
 * 29组分隔符
 * 30记录分隔符
 * 31单元分隔符
 * 32空字符串（即空格键space）
 * 【打印控制字符---常规符号】    33!  34"(\")  35#  36$  37%  38&  39'(\')  40(  41)  42*  43+  44,  45-  46.  47/
 * 【打印控制字符---阿拉伯数字】  480  491  502  513  524  535  546  557  568  579
 * 【打印控制字符---常规符号】    58:  59;  60<  61=  62>  63?(\?)  64@
 * 【打印控制字符---大写英文字母】 65A  66B  67C  68D  69E  70F  71G  72H  73I  74J  75K  76L  77M  78N  79O  80P  81Q  82R  83S  84T  85U  86V  87W  88X  89Y  90Z
 * 【打印控制字符---常规符号】    91[  92\(\\)  93]  94^  95_  96`
 * 【打印控制字符---小写英文字母】 97a  98b  99c  100d  101e  102f  103g  104h  105i  106j  107k  108l  109m  110n  111o  112p  113q  114r  115s  116t  117u  118v  119w  120x  121y  122z
 * 【打印控制字符---常规符号】    123{  124|  125}  126~
 * 【非打印控制字符】            127删除
 *
 * 转移字符      ASCII码         含义
 * \o          0               空字符
 * \a          7               响铃
 * \b          8               退格符
 * \t          9               水平制表符
 * \n          10              换行符
 * \v          11              垂直制表符
 * \f          12              换页符
 * \r          13              回车符
 * \"          34              双引号
 * \'          39              单引号
 * \?          63              问号字符
 * \\          92              反斜杠
 * \ddd        三位八进制        任意字符
 * \xhh        二位十六进制      任意字符
 */
public final class AsciiUtil {

    /**
     * 2024-09-20
     * 获取所有asciicode码对应的字符串
     * @return
     */
    public static final String getAllAsciiStr(){
        final StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i <= 127; i++){
            String sss = asciiCode2sss(i);
            stringBuilder.append(sss);
        }
        final String str = stringBuilder.toString();
        return str;
    }

    /**
     * 2024-09-20
     * 获取所有asciicode码中可打印的码对应的字符串
     * @return
     */
    public static final String getAllPrintableStr(){
        final StringBuilder stringBuilder = new StringBuilder();
        for(int i = 33; i <= 126; i++){
            String sss = asciiCode2sss(i);
            stringBuilder.append(sss);
        }
        final String str = stringBuilder.toString();
        return str;
    }

    /**
     * 2023-08-10
     * 返回所有可打印ascii码中非数字大小写字母的字符，[33-47, 58-64, 91-96, 123-126]
     */
    public static final String getSpecialStr(){
        final StringBuilder stringBuilder = new StringBuilder();
        final String[] array = {"33-47", "58-64", "91-96", "123-126"};
        for(String element : array){
            String[] arr = StringUtil.split(element, "-");
            String startStr = arr[0];
            int start = Integer.parseInt(startStr);
            String lastStr = arr[1];
            int last = Integer.parseInt(lastStr);
            for(int i = start; i <= last; i++){
                String sss = asciiCode2sss(i);
                stringBuilder.append(sss);
            }
        }
        final String specialStr = stringBuilder.toString();
        return specialStr;
    }

    /**
     * 2023-08-10
     * 返回所有ascii所有大写字母，[65-90]
     */
    public static final String getUpperStr(){
        final StringBuilder stringBuilder = new StringBuilder();
        for(int i = 65; i <= 90; i++){
            String sss = asciiCode2sss(i);
            stringBuilder.append(sss);
        }
        final String upperStr = stringBuilder.toString();
        return upperStr;
    }

    /**
     * 2023-08-10
     * 指定的字符串中是否全部为ascii大写字母，[65-90]
     */
    public static final boolean isUpperStr(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        for(int i = 0; i < str.length(); i++){
            final int asciiCode = str2asciiCode(str,i);
            if(asciiCode < 65 || asciiCode > 90){
                return false;
            }
        }
        return true;
    }

    /**
     * 2023-08-10
     * 指定的字符串中是否含有ascii大写字母，[65-90]
     */
    public static final boolean inUpperStr(final String str){
        for(int i = 0; i < str.length(); i++){
            final int asciiCode = str2asciiCode(str,i);
            if(asciiCode >= 65 && asciiCode <= 90){
                return true;
            }
        }
        return false;
    }

    /**
     * 2023-08-10
     * 返回所有ascii所有小写字母，[97-122]
     */
    public static final String getLowerStr(){
        final StringBuilder stringBuilder = new StringBuilder();
        for(int i = 97; i <= 122; i++){
            String sss = asciiCode2sss(i);
            stringBuilder.append(sss);
        }
        final String lowerStr = stringBuilder.toString();
        return lowerStr;
    }

    /**
     * 2023-08-10
     * 指定的字符串中是否全部为ascii小写字母，[97-122]
     */
    public static final boolean isLowerStr(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        for(int i = 0; i < str.length(); i++){
            final int asciiCode = str2asciiCode(str,i);
            if(asciiCode < 97 || asciiCode > 122){
                return false;
            }
        }
        return true;
    }

    /**
     * 2023-08-10
     * 指定的字符串中是否含有ascii小写字母，[97-122]
     */
    public static final boolean inLowerStr(final String str){
        for(int i = 0; i < str.length(); i++){
            final int asciiCode = str2asciiCode(str,i);
            if(asciiCode >= 97 && asciiCode <= 122){
                return true;
            }
        }
        return false;
    }

    /**
     * 2023-08-10
     * 返回所有ascii数字，[48-57]
     */
    public static final String getNumberStr(){
        final StringBuilder stringBuilder = new StringBuilder();
        for(int i = 48; i <= 57; i++){
            String sss = asciiCode2sss(i);
            stringBuilder.append(sss);
        }
        final String numberStr = stringBuilder.toString();
        return numberStr;
    }

    /**
     * 2023-08-10
     * 指定的字符串中是否全部为ascii数字，[48-57]
     */
    public static final boolean isNumberStr(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        for(int i = 0; i < str.length(); i++){
            final int asciiCode = str2asciiCode(str,i);
            if(asciiCode < 48 || asciiCode > 57){
                return false;
            }
        }
        return true;
    }

    /**
     * 2023-08-10
     * 指定的字符串中是否含有ascii数字，[48-57]
     */
    public static final boolean inNumberStr(final String str){
        for(int i = 0; i < str.length(); i++){
            final int asciiCode = str2asciiCode(str,i);
            if(asciiCode >= 48 && asciiCode <= 57){
                return true;
            }
        }
        return false;
    }

    /**
     * 2023-08-10
     * 指定的字符串中是否全部为ascii特殊字符，[33-47, 58-64, 91-96, 123-126]
     */
    public static final boolean isSpecialStr(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        final String specialStr = getSpecialStr();
        for(int i = 0; i < specialStr.length(); i++){
            final String special = StringUtil.strAt(specialStr, i);  // 单个特殊字符
            if(!StringUtil.containOf(str, special)){
                return false;
            }
        }
        return true;
    }

    /**
     * 2023-08-10
     * 指定的字符串中是否含有ascii特殊字符，[33-47, 58-64, 91-96, 123-126]
     */
    public static final boolean inSpecialStr(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        final String specialStr = getSpecialStr();
        for(int i = 0; i < specialStr.length(); i++){
            final String special = StringUtil.strAt(specialStr, i);  // 单个特殊字符
            if(StringUtil.containOf(str, special)){
                return true;
            }
        }
        return false;
    }

    /**
     * 2024-06-21
     * 指定的char是否为ASCII打印字符
     * 打印字符：[33,126]表示打印字符，共94个，这些字符都是在电脑键盘上可以找到的。
     */
    public static final boolean isPrintable(final char chr){
        if(33 <= chr && chr <= 126){
            return true;
        }
        return false;
    }

    /**
     * 2024-06-21
     * 指定的字符是否为ASCII打印字符
     * 打印字符：[33,126]表示打印字符，共94个，这些字符都是在电脑键盘上可以找到的。
     */
    public static final boolean isPrintable(final String sss){
        char chr = sss.charAt(0);
        if(33 <= chr && chr <= 126){
            return true;
        }
        return false;
    }

    /**
     * 2023-08-08
     * 2024-06-22 重命名，修复判断可打印级字符逻辑的BUG，严格限定非打印ASCII码的范围
     * 判断指定字符串是否全部都是可打印的asciiCode码对应的字符
     * @param str
     * @return
     */
    public static final boolean isPrintableAll(final String str){
        if(StringUtil.isBlank(str)){
            return false;
        }
        //
        for(int i = 0; i < str.length(); i++){
            char chr = str.charAt(i);
            boolean flag = isPrintable(chr);
            if(!flag){
                return false;
            }
        }
        //
        return true;
    }

    /**
     * 2023-08-08
     * 获取随机ascii码，即：随机获取指定位数的asciiCode码对应字符（asciiCode码33-126）
     * @param length
     * @return
     */
    public static final String generate(final int length){
        if(length <= 0){
            return null;
        }
        //
        List<Integer> list = new ArrayList<>();
        for(int i = 33; i <= 126; i++){
            list.add(i);
        }
        StringBuilder stringBuilder = new StringBuilder();
        List<Integer> randomList = ListUtil.getRandomList(list, length, true);
        for(int i = 0; i < randomList.size(); i++){
            int random = randomList.get(i);
            String sss = asciiCode2sss(random);
            stringBuilder.append(sss);
        }
        String asciiStr = stringBuilder.toString();
        return asciiStr;
    }

    /**
     * 2020-03-31 5.0.0
     * 字符串转asciiCode码，之间用英文逗号分隔
     * @param str
     */
    public static final String str2asciiCodeStr(final String str){
        if(str == null){
            return "0";
        }
        //
        final StringBuilder stringBuilder = new StringBuilder();
        final int length = str.length();
        for(int i = 0; i < length; i++) {
            final String sss = StringUtil.strAt(str, i);
            Integer asciiCode = sss2asciiCode(sss);
            if(i != length - 1){
                stringBuilder.append(asciiCode).append(",");
                continue;
            }
            stringBuilder.append(asciiCode);
        }
        final String asciiCode = stringBuilder.toString();
        return asciiCode;
    }

    /**
     * 2023-08-10
     * 获取指定字符串中指定索引的字符对应的asciiCode码
     * @param str
     * @param index
     * @return
     */
    public static final String str2asciiCodeSSS(final String str, final int index){
        char chr = str.charAt(index);
        final String sss = StringUtil.char2str(chr);
        return sss;
    }

    /**
     * 2023-08-10
     * 获取指定字符串中指定索引的字符对应的asciiCode码
     * @param str
     * @param index
     * @return
     */
    public static final int str2asciiCode(final String str, final int index){
        char chr = str.charAt(index);
        final int asciiCode = (char) chr;
        return asciiCode;
    }

    /**
     * 2023-07-31
     * 单个字符转单个asciiCode码
     */
    public static final int char2asciiCode(final char chr){
        final int asciiCode = (char) chr;
        return asciiCode;
    }

    /**
     * 2020-03-31 5.0.0
     * 单个字符转单个asciiCode码
     * @param sss
     */
    public static final int sss2asciiCode(final String sss){
        if(sss == null){
            return 0;
        }
        //
        final char chr = StringUtil.str2char(sss);
        final int asciiCode = (char) chr;
        return asciiCode;
    }

    /**
     * 2020-03-31 5.0.0
     * 单个asciiCode码转单个字符
     * @param asciiCode
     */
    public static final String asciiCode2sss(final int asciiCode){
        final char chr = (char) asciiCode;
        final String sss = StringUtil.char2str(chr);
        return sss;
    }

}