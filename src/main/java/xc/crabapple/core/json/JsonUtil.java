package xc.crabapple.core.json;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.AsciiUtil;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.json.jackson.JacksonUtil;
import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.timer.TimerUtil;

/**
 * 2024-06-22
 * JSON处理类，该类不依赖第三方JSON库
 * 标准JSON示例：
 * {"k1":"v1", "k2":22, "k3":3.3, "k4":false, "k5":null, "k6":{"kk6":"vv6"}, "k7":["v71", "v72"]}
 */
public final class JsonUtil {

    private static final char CHAR_DOUBLE_QUOTE = 34;   // "
    private static final char CHAR_SINGLE_QUOTE = 39;   // '
    private static final char CHAR_BACKSLASH = 92;       // \
    private static final char CHAR_COLON = 58;           // :
    private static final char CHAR_COMMA = 44;           // ,
    private static final char CHAR_LEFT_BRACE = 123;     // {
    private static final char CHAR_RIGHT_BRACKET = 93;   // ]
    private static final char CHAR_LEFT_BRACKET = 91;    // [
    private static final char CHAR_LF = 10;              // \n
    private static final char CHAR_CR = 13;              // \r

    /**
     * 2025-02-27
     * 判断检测给定的字符串是否为json格式
     * @param str
     * @return
     */
    public static final boolean isJson(final String str) {
        boolean flag = JacksonUtil.isJson(str);
        return flag;
    }

    /**
     * 2024-06-25
     * JSON数据过滤，例如：
     *   时间格式过滤处理，TimerUtil.convert2standardFormat()
     * @param json
     */
    public static final String filter(final String json){
        // json = convert2standardJson(json);
//        json = json2object(json);
        boolean doubleFlag = false;  // 双引号开关，CHAR_DOUBLE_QUOTE"
        int length = json.length();
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder stringBuilder2 = new StringBuilder();
        for(int i = 0; i < length; i++){
            final char chr = json.charAt(i);
            if(!doubleFlag){
                if(chr == CHAR_DOUBLE_QUOTE){
                    doubleFlag = true;
                    stringBuilder.append(chr);
                    continue;
                }
            }
            //
            if(doubleFlag){
                if(chr == CHAR_DOUBLE_QUOTE){
                    // 统计前面连续反斜杠的数量，判断是否是转义引号
                    // 规律：奇数个反斜杠后的"是转义引号，偶数个（包括0个）后的"是正常结束引号
                    int backslashCount = 0;
                    int pos = i - 1;
                    while(pos >= 0 && json.charAt(pos) == CHAR_BACKSLASH){
                        backslashCount++;
                        pos--;
                    }
                    if(backslashCount % 2 == 1){
                        stringBuilder2.append(chr);
                        continue;
                    }
                    String kv = stringBuilder2.toString();
                    // 核心代码
                    if(TimerUtil.checkTimeFormat(kv)){
                        kv = TimerUtil.convert2standardFormat(kv);
                    }
                    stringBuilder.append(kv).append(chr);
                    int kvLength = stringBuilder2.length();
                    stringBuilder2.delete(0, kvLength);
                    doubleFlag = false;
                    continue;
                }
                stringBuilder2.append(chr);
                continue;
            }
            //
            stringBuilder.append(chr);
        }
        final String result = stringBuilder.toString();
        return result;
    }

    /**
     * 2024-07-12
     * @param json
     * @return
     */
    public static final String json2object(final String json){
        if(StringUtil.isBlank(json)){
            return json;
        }
        //
        String newJson = StringUtil.replaceAll(json, "\"{", "{");
        newJson = StringUtil.replaceAll(newJson, "}\"", "}");
        newJson = StringUtil.replaceAll(newJson, "\\\"", "\"");
        return newJson;
    }

    /**
     * 2024-06-22
     * 源JSON转换为纯净版JSON，即把KEY值和VALUE值之外的非打印ASCII字符去除，整体一行JSON
     * @param json
     * @return
     */
    public static final String pure2standardJson(final String json){
        StringBuilder stringBuilder = new StringBuilder();
        boolean doubleFlag = false;  // 双引号开关，CHAR_DOUBLE_QUOTE"
        boolean singleFlag = false;  // 单引号开关，CHAR_SINGLE_QUOTE'
        // CHAR_BACKSLASH\反斜杠  CHAR_COLON:冒号  CHAR_RIGHT_BRACKET]右中括号
        final int length = json.length();
        for(int i = 0; i < length; i++){
            final char chr = json.charAt(i);
            if(!singleFlag){
                if(chr == CHAR_DOUBLE_QUOTE){  // "
                    stringBuilder.append(chr);
                    if(!doubleFlag){
                        doubleFlag = true;
                        continue;
                    }
                    // 统计前面连续反斜杠的数量，判断是否是转义引号
                    int backslashCount = 0;
                    int pos = i - 1;
                    while(pos >= 0 && json.charAt(pos) == CHAR_BACKSLASH){backslashCount++; pos--;}
                    if(backslashCount % 2 == 1){continue;}
                    doubleFlag = false;
                    continue;
                }
            }
            // 在双引号没有启动时进行单引号所有判断
            if(!doubleFlag){
                if(chr == CHAR_SINGLE_QUOTE){  // '
                    if(!singleFlag){
                        stringBuilder.append("\"");
                        singleFlag = true;
                        continue;
                    }
                    // 统计前面连续反斜杠的数量，判断是否是转义引号
                    int backslashCount = 0;
                    int pos = i - 1;
                    while(pos >= 0 && json.charAt(pos) == CHAR_BACKSLASH){backslashCount++; pos--;}
                    if(backslashCount % 2 == 1){stringBuilder.append(chr); continue;}

                    stringBuilder.append("\"");
                    singleFlag = false;
                    continue;
                }
            }
            //
            // 双引号启动后 START
            if(doubleFlag){
                stringBuilder.append(chr);
                continue;
            }
            // 双引号启动后 END
            // 单引号启动后 START
            if(singleFlag){
                stringBuilder.append(chr);
                continue;
            }
            // 单引号启动后 END
            //
            // 双引号、单引号之外 START
            boolean printable = AsciiUtil.isPrintable(chr);
            if(printable){
                stringBuilder.append(chr);
            }
            // 双引号、单引号之外 END
        }
        final String standardJson = stringBuilder.toString();
        return standardJson;
    }

    /**
     * 2024-06-22
     *
     * 把不标准格式的JSON转为标准格式，如：
     * 1、KEY没有双引号包裹
     * 2、KEY单引号包裹
     * 3、数组最后一个元素后面有逗号
     *
     * 性能测试：解析并转换为标准格式JSON
     * 280MB大小JSON数据：耗时约1.8秒
     * 500MB大小JSON数据：耗时约3秒
     *
     * @param json
     * @return
     */
    public static final String convert2standardJson(final String json){
        StringBuilder stringBuilder = new StringBuilder();
        boolean doubleFlag = false;  // 双引号开关，CHAR_DOUBLE_QUOTE"
        boolean singleFlag = false;  // 单引号开关，CHAR_SINGLE_QUOTE'
        // CHAR_BACKSLASH\反斜杠  CHAR_COLON:冒号  CHAR_RIGHT_BRACKET]右中括号
        final int length = json.length();
        for(int i = 0; i < length; i++){
            final char chr = json.charAt(i);
            if(!singleFlag){
                if(chr == CHAR_DOUBLE_QUOTE){  // "
                    if(!doubleFlag){
                        stringBuilder.append(chr);
                        doubleFlag = true;
                        continue;
                    }
                    // doubleFlag为true时，需要判断这是值的结束引号还是新键的开始引号
                    // 统计前面连续反斜杠的数量，判断是否是转义引号
                    // 规律：奇数个反斜杠后的"是转义引号，偶数个（包括0个）后的"是正常结束引号
                    int backslashCount = 0;
                    int pos = i - 1;
                    while(pos >= 0 && json.charAt(pos) == CHAR_BACKSLASH){  // \
                        backslashCount++;
                        pos--;
                    }
                    // 如果是奇数个反斜杠，则是转义引号，继续
                    if(backslashCount % 2 == 1){
                        stringBuilder.append(chr);
                        continue;
                    }
                    // 检查这是否真的是值的结束引号
                    // 如果前一个可打印字符是 , { [ ，且中间有换行符，则这个"可能是新键/值的开始引号（说明前面的值没有闭合引号）
                    // 注意：必须有换行符才判断为新键开始，否则可能是值中包含逗号的情况（如 "Hello, World!"）
                    int j = 1;
                    char prevPrintable = 0;
                    boolean hasNewline = false;  // 逗号/括号和引号之间是否有换行符
                    while(i - j >= 0){
                        char prevChr = json.charAt(i - j);
                        if(prevChr == CHAR_LF || prevChr == CHAR_CR){  // \n 或 \r
                            hasNewline = true;
                        }
                        if(AsciiUtil.isPrintable(prevChr)){
                            prevPrintable = prevChr;
                            break;
                        }
                        j++;
                    }
                    // , { [  (CHAR_COMMA是逗号, CHAR_LEFT_BRACE是左大括号, CHAR_LEFT_BRACKET是左中括号)
                    // 只有当中间有换行符时，才认为是新键的开始引号
                    if(hasNewline && (prevPrintable == CHAR_COMMA || prevPrintable == CHAR_LEFT_BRACE || prevPrintable == CHAR_LEFT_BRACKET)){
                        // 当前值没有正确闭合，这个"是新键/值的开始引号
                        // 在stringBuilder中逆向找到对应的 , { [ 的位置并在其前面插入闭合引号
                        int sbLen = stringBuilder.length();
                        for(int k = sbLen - 1; k >= 0; k--){
                            char sbChr = stringBuilder.charAt(k);
                            if(sbChr == prevPrintable){  // 找到对应的 , { [
                                stringBuilder.insert(k, "\"");  // 在其前面插入闭合引号
                                break;
                            }
                        }
                        stringBuilder.append(chr);  // 添加当前的 " 作为新键/值的开始引号
                        // doubleFlag保持true，因为这是新的开始引号
                        continue;
                    }
                    // 正常的结束引号
                    stringBuilder.append(chr);
                    doubleFlag = false;
                    continue;
                }
            }
            // 在双引号没有启动时进行单引号所有判断
            if(!doubleFlag){
                if(chr == 39){  // '
                    if(!singleFlag){
                        stringBuilder.append("\"");
                        singleFlag = true;
                        continue;
                    }
                    // 统计前面连续反斜杠的数量，判断是否是转义单引号
                    int backslashCount = 0;
                    int pos = i - 1;
                    while(pos >= 0 && json.charAt(pos) == CHAR_BACKSLASH){  // \
                        backslashCount++;
                        pos--;
                    }
                    // 如果是奇数个反斜杠，则是转义单引号，继续
                    if(backslashCount % 2 == 1){
                        stringBuilder.append(chr);
                        continue;
                    }
                    stringBuilder.append("\"");
                    singleFlag = false;
                    continue;
                }
            }
            //
            // 双引号启动后 START
            if(doubleFlag){
                stringBuilder.append(chr);
                continue;
            }
            // 双引号启动后 END
            //
            // 单引号启动后 START
            if(singleFlag){
                stringBuilder.append(chr);
                continue;
            }
            // 单引号启动后 END
            //
            // 双引号、单引号之外 START
            // 处理KEY没有引号的问题
            // :
            if(chr == CHAR_COLON){
                // 逆向遍历，检测是否使用引号包裹
                boolean keyyinhaoFlag = true;  // key是否由双引号或单引号包裹，true为是，false为否
                int j = 0;
                while(true){
                    j++;
                    char beforeChr = json.charAt(i - j);
                    boolean printable = AsciiUtil.isPrintable(beforeChr);
                    if(printable){
                        // " '
                        if(beforeChr != CHAR_DOUBLE_QUOTE && beforeChr != CHAR_SINGLE_QUOTE){
                            keyyinhaoFlag = false;
                        }
                        break;
                    }
                }
                if(!keyyinhaoFlag){
                    StringBuilder stringBuilder2 = new StringBuilder();
                    j = 0;
                    int nonprintableCount = 0;  // 不可打印ASCII字符的数量
                    int printableCount = 0;  // 可打印字符的数量
                    while(true){
                        j++;
                        char beforeChr = json.charAt(i - j);
                        boolean printable = AsciiUtil.isPrintable(beforeChr);
                        if(printable){
                            // , {
                            if(beforeChr != CHAR_COMMA && beforeChr != CHAR_LEFT_BRACE){
                                if(printableCount == 0){
                                    stringBuilder2.append("\"").append(beforeChr);
                                }else{
                                    stringBuilder2.append(beforeChr);
                                }
                                printableCount++;
                            }else{
                                stringBuilder2.append("\"");
                                break;
                            }
                        }else{
                            if(printableCount == 0){
                                nonprintableCount++;
                                stringBuilder2.append(beforeChr);
                            }else{
                                stringBuilder2.append("\"");
                                break;
                            }
                        }
                    }
                    stringBuilder2 = stringBuilder2.reverse();
                    stringBuilder.delete(stringBuilder.length() - nonprintableCount - printableCount, stringBuilder.length());
                    stringBuilder.append(stringBuilder2).append(chr);
                    continue;
                }
            }
            // ] 逆向遍历，处理数组最后一个元素末尾有英文逗号的问题
            if(chr == CHAR_RIGHT_BRACKET){
                StringBuilder stringBuilder2 = new StringBuilder();
                int j = 0;
                int nonprintableCount = 0;  // 不可打印ASCII字符的数量
                while(true){
                    j++;
                    char beforeChr = json.charAt(i - j);
                    boolean printable = AsciiUtil.isPrintable(beforeChr);
                    if(printable){
                        // ,
                        if(beforeChr != CHAR_COMMA){
                            stringBuilder2.append(beforeChr);
                        }
                        break;
                    }else{
                        nonprintableCount++;
                        stringBuilder2.append(beforeChr);
                    }
                }
                stringBuilder2 = stringBuilder2.reverse();
                stringBuilder.delete(stringBuilder.length() - nonprintableCount - 1, stringBuilder.length());
                stringBuilder.append(stringBuilder2).append(chr);
                continue;
            }
            //
            stringBuilder.append(chr);
            // 双引号、单引号之外 END
        }
        final String standardJson = stringBuilder.toString();
        return standardJson;
    }

    /**
     * 2024-06-22
     * 创建超大JSON文件
     */
    public static final void createHugeStandardJson(final String toJsonUrl){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[\n");
        int size = 100000;
        for(int i = 1; i <= size; i++){
            String id = RandomUtil.getStrWithStamp(64);
            String singleNode = "{\n" +
                    "\"modelId\":\"id_"+id+"\",\n" +
                    "\"entityId\":9491431,\n" +
                    "\"domainStr\":\"暖通\",\n" +
                    "\"type\":\"排烟管\",\n" +
                    "\"version\":1,\n" +
                    "\"uuid\":\"406fe015-dc3a-4d81-a1b3-409fbb00c09a-0090d3e7\",\n" +
                    "\"relation\":\"-1\",\n" +
                    "\"viewId\":0,\n" +
                    "\"domain\":5,\n" +
                    "\"name\":\"Rectangular Duct : 排烟管\",\n" +
                    "\"_id\":\"65212b37dcc7ce00880346d3\",\n" +
                    "\"categoryStr\":\"风管\",\n" +
                    "\"category\":304,\n" +
                    "\"floor\":\"A_B3_-15.000\",\n" +
                    "\"properties\":[\n" +
                    "{\"value\":\"Rectangular Duct\",\"key\":\"族\",\"group\":\"其他\"},\n" +
                    "{\"value\":\"Rectangular Duct : 排烟管\",\"key\":\"族与类型\",\"group\":\"其他\"},\n" +
                    "{\"value\":\"Ducts\",\"key\":\"类别\",\"group\":\"其他\"},\n" +
                    "{\"value\":\"排烟管\",\"key\":\"类型\",\"group\":\"其他\"},\n" +
                    "{\"value\":\"排烟管\",\"key\":\"类型 ID\",\"group\":\"其他\"},\n" +
                    "{\"value\":\"0 mm\",\"key\":\"内衬厚度\",\"group\":\"内衬\"},\n" +
                    "{\"value\":\"1600 mmx400 mm\",\"key\":\"可用大小\",\"group\":\"内衬\"},\n" +
                    "{\"value\":\"1600\",\"key\":\"宽度\",\"group\":\"尺寸标注\"},\n" +
                    "{\"value\":\"1600x400\",\"key\":\"尺寸\",\"group\":\"尺寸标注\"},\n" +
                    "{\"value\":\"848\",\"key\":\"长度\",\"group\":\"尺寸标注\"},\n" +
                    "{\"value\":\"400\",\"key\":\"高度\",\"group\":\"尺寸标注\"},\n" +
                    "{\"value\":\"1\",\"key\":\"剖面\",\"group\":\"机械\"},\n" +
                    "{\"value\":\"0\",\"key\":\"尺寸锁定\",\"group\":\"机械\"},\n" +
                    "{\"value\":\"827\",\"key\":\"当量直径\",\"group\":\"机械\"},\n" +
                    "{\"value\":\"0.000000\",\"key\":\"损耗系数\",\"group\":\"机械\"},\n" +
                    "{\"value\":\"640\",\"key\":\"水力直径\",\"group\":\"机械\"},\n" +
                    "{\"value\":\"Exhaust Air\",\"key\":\"系统分类\",\"group\":\"机械\"},\n" +
                    "{\"value\":\"P(Y)F 18\",\"key\":\"系统名称\",\"group\":\"机械\"},\n" +
                    "{\"value\":\"M_平时排风兼排烟系统\",\"key\":\"系统类型\",\"group\":\"机械\"},\n" +
                    "{\"value\":\"P(Y)F\",\"key\":\"系统缩写\",\"group\":\"机械\"},\n" +
                    "{\"value\":\"3\",\"key\":\"面积\",\"group\":\"机械\"},\n" +
                    "{\"value\":\"0.0 m³/h\",\"key\":\"其他流量\",\"group\":\"机械 - 流量\"},\n" +
                    "{\"value\":\"0.0 Pa\",\"key\":\"压降\",\"group\":\"机械 - 流量\"},\n" +
                    "{\"value\":\"0.00 Pa/m\",\"key\":\"摩擦\",\"group\":\"机械 - 流量\"},\n" +
                    "{\"value\":\"0.0 m³/h\",\"key\":\"流量\",\"group\":\"机械 - 流量\"},\n" +
                    "{\"value\":\"0.0 m/s\",\"key\":\"速度\",\"group\":\"机械 - 流量\"},\n" +
                    "{\"value\":\"0.000000\",\"key\":\"雷诺数\",\"group\":\"机械 - 流量\"},\n" +
                    "{\"value\":\"0.0 Pa\",\"key\":\"风压\",\"group\":\"机械 - 流量\"},\n" +
                    "{\"value\":\"(none)\",\"key\":\"设计选项\",\"group\":\"标识数据\"},\n" +
                    "{\"value\":\"2550\",\"key\":\"中间高程\",\"group\":\"约束\"},\n" +
                    "{\"value\":\"A_B3_-15.000\",\"key\":\"参照标高\",\"group\":\"约束\"},\n" +
                    "{\"value\":\"0.00%\",\"key\":\"坡度\",\"group\":\"约束\"},\n" +
                    "{\"value\":\"Top\",\"key\":\"垂直对正\",\"group\":\"约束\"},\n" +
                    "{\"value\":\"2350\",\"key\":\"底部高程\",\"group\":\"约束\"},\n" +
                    "{\"value\":\"Center\",\"key\":\"水平对正\",\"group\":\"约束\"},\n" +
                    "{\"value\":\"2550\",\"key\":\"端点中间高程\",\"group\":\"约束\"},\n" +
                    "{\"value\":\"2550\",\"key\":\"起点中间高程\",\"group\":\"约束\"},\n" +
                    "{\"value\":\"2750\",\"key\":\"顶部高程\",\"group\":\"约束\"},\n" +
                    "{\"value\":\"1600 mmx400 mm\",\"key\":\"总体大小\",\"group\":\"绝缘层\"},\n" +
                    "{\"value\":\"0 mm\",\"key\":\"隔热层厚度\",\"group\":\"绝缘层\"},\n" +
                    "{\"value\":\"新构造\",\"key\":\"创建的阶段\",\"group\":\"阶段化\"},\n" +
                    "{\"value\":\"(none)\",\"key\":\"拆除的阶段\",\"group\":\"阶段化\"}]\n" +
                    "}";
            if(i < size){
                stringBuilder.append(singleNode).append(",\n");
                continue;
            }
            stringBuilder.append(singleNode).append("\n]");
        }
        final String result = stringBuilder.toString();
        FileUtil.delete(toJsonUrl);
        FileUtil.fileWriteNew(toJsonUrl, result, false);
    }

}
