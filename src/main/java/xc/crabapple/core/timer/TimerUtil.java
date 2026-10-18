package xc.crabapple.core.timer;

import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.regex.RegexUtil;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * 2016-11-27 1.0.0
 * 时间工具类：提供时间格式化、解析、比较、推移、时间戳转换等能力
 * 标准格式常量 FORMATTYPE_M1(yyyy-MM-dd HH:mm:ss.SSS，带毫秒) / FORMATTYPE1(yyyy-MM-dd HH:mm:ss，无毫秒)
 */
public final class TimerUtil{

    // unit
    public static final int MILLISECOND = 1;
    public static final int SECOND = 2;
    public static final int MINUTE = 3;
    public static final int HOUR = 4;
    public static final int DAY = 5;
    public static final int WEEK = 6;
    public static final int MONTH = 7;
    public static final int YEAR = 8;
    // format
    public static final String FORMATTYPE_M1 = "yyyy-MM-dd HH:mm:ss.SSS";  // 23位，空格1，中划线2，冒号2，字符21-23
    public static final String FORMATTYPE_M2 = "yyyy-MM-dd HH:mm:ss:SSS";  // 23位，空格1，中划线2，冒号3，字符21-23
    public static final String FORMATTYPE_M3 = "yyyy-MM-dd-HH-mm-ss-SSS";  // 23位，空格0，中划线6，冒号0，字符23，屏蔽空格冒号，可作为文件名 2020-08-19 6.5.0
    public static final String FORMATTYPE1 = "yyyy-MM-dd HH:mm:ss";  // 19位，空格1，中划线2，冒号2，字符19
    public static final String FORMATTYPE2 = "yyyyMMdd HHmmss";  // 15位，空格1，横中划线杠0，冒号0，字符15
    public static final String FORMATTYPE3 = "yyyyMMddHHmmss";  // 14位，空格0，中划线0，冒号0，字符14
    public static final String FORMATTYPE4 = "yyyy-MM-dd";  // 10位，空格0，中划线2，冒号0，字符10
    public static final String FORMATTYPE5 = "yyyyMMdd";  // 8位，空格0，中划线0，冒号0，字符8
    public static final String FORMATTYPE6 = "HH:mm:ss";  // 8位，空格0，中划线0，冒号2，字符8
    public static final String FORMATTYPE7 = "HH:mm:ss.SSS";  // 12位，空格0，中划线0，冒号3，字符10-12
    public static final String FORMATTYPE9 = "yyyy年MM月dd日";  // 11位，空格0，中划线2，冒号0，字符10
    public static final String FORMATTYPE10 = "dd/MM/yyyy HH:mm:ss";  // 19位，
    public static final String FORMATTYPE11 = "HH:mm";
    //
    private static Map<String, String> p_monthMap = null;
    private static Map<String, String> p_weekMap = null;
    private static String formatType_default = FORMATTYPE_M1;

    /**
     * 2026-08-13 设置时间类默认值
     * @param formatType
     */
    public static final void init(String formatType){
        if(StringUtil.isBlank(formatType)){
            return;
        }
        formatType_default = formatType;
    }

    /**
     * 2025-09-19
     * 04-12月-19 09.44.30. 上午 ==> 2019-12-04 09.44.30.000
     * 17-4月-20 03.26.19. 下午  ==> 2020-04-17 15.26.19.000
     * @param originalDateStr
     * @return
     */
    public static final String dmDate2stdDate(final String originalDateStr) {
        if(StringUtil.isBlank(originalDateStr)) {
            return "";
        }
        if(!StringUtil.containOf(originalDateStr, "月-")){
            return originalDateStr;
        }
        String[] array = StringUtil.split(originalDateStr, " ");
        String first = array[0];
        String second = array[1];
        String third = array[2];
        third = StringUtil.trim(third);
        String[] array2 = StringUtil.split(first, "-");
        String day = array2[0];
        String month = array2[1];
        month = StringUtil.deleteAll(month, "月");
        if(!StringUtil.startsWith(month, "0")){
            if(month.length() == 1){
                month = "0" + month;
            }
        }
        String year = array2[2];
        year = "20" + year;
        //
        String[] array3 = StringUtil.split(second, ".");
        String hour = array3[0];
        String minute = array3[1];
        String second2 = array3[2];
        //
        if("下午".equals(third)){
            int hourInt = Integer.valueOf(hour);
            hourInt += 12;
            hour = String.valueOf(hourInt);
        }
        String result = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second2 + ".000";
        return result;
    }

    /**
     * 2024-06-25
     * 指定的时间格式数据转换为标准时间格式：yyyy-MM-dd HH:mm:ss.SSS
     * 示例： 2024-11-20  ==> 2024-11-20 00:00:00.000
     * @param timeStr
     * @return
     */
    public static final String convert2standardFormat(final String timeStr){
        // 2026-09-01 标准时间格式增强
        // 接入formatType_default全局默认格式（init()可设置）：不含SSS时输出19位无毫秒标准格式（原带毫秒的截断到秒），
        // 含SSS时维持原补毫秒行为；formatType_default默认值FORMATTYPE_M1不变，即默认行为与历史完全一致
        boolean withMillis = StringUtil.containOf(formatType_default, "SSS");
        //
        if(StringUtil.isBlank(timeStr)){
            if(withMillis){
                return "0000-00-00 00:00:00.000";
            }
            return "0000-00-00 00:00:00";
        }
        //
        int timeStrLength = timeStr.length();
        // 只有年-月-日
        if(timeStrLength == 10){
            StringBuilder stringBuilder = new StringBuilder(timeStr);
            if(withMillis){
                stringBuilder.append(" 00:00:00.000");
            }else{
                stringBuilder.append(" 00:00:00");
            }
            String standardOfDate = stringBuilder.toString();
            return standardOfDate;
        }
        String standard = StringUtil.replaceAll(timeStr, "T", " ");
        standard = StringUtil.deleteAll(standard, "Z");
        timeStrLength = standard.length();
        // 默认格式不含SSS：统一输出到秒（19位），原带毫秒的截断；不足19位的畸形值走尾部兜底
        if(!withMillis){
            if(timeStrLength >= 19){
                String standardSecond = standard.substring(0, 19);
                return standardSecond;
            }
        }else{
            // 2024-06-25 12:57:39.123
            if(timeStrLength == 23){
                return standard;
            }
            // 2024-06-25 12:57:39.02
            if(timeStrLength == 22){
                String left = standard.substring(0, 20);
                String millisecond = standard.substring(20, timeStrLength);  // 02
                standard = left + "0" + millisecond;
                return standard;
            }
            // 2024-06-25 12:57:39.2
            if(timeStrLength == 21){
                String left = standard.substring(0, 20);
                String millisecond = standard.substring(20, timeStrLength);  // 2
                standard = left + "00" + millisecond;
                return standard;
            }
            // 没有毫秒时
            if(timeStrLength == 19){
                standard += ".000";
                return standard;
            }
        }
        //
        LogUtil.error("TimerUtil_convert2standardFormat_1", "指定的时间格式无法解析", timeStr, standard);
        return timeStr;
    }

    /**
     * 2024-06-25
     * 校验指定字符串是否为时间格式，下面是校验的格式：
     * 2024-06-25                长度 =            10   ==> 2024-06-25
     * 2024-06-25T12:57:39       长度 = 10+1+8 =   19   ==> 2024-06-25 12:57:39
     * 2024-06-25T12:57:39.2     长度 = 10+1+8+4 = 21   ==> 2024-06-25 12:57:39.2
     * 2024-06-25T12:57:39.02    长度 = 10+1+8+4 = 22   ==> 2024-06-25 12:57:39.02
     * 2024-06-25T12:57:39.123   长度 = 10+1+8+4 = 23   ==> 2024-06-25 12:57:39.123
     * 2024-06-25T12:57:39.123Z  长度 = 10+1+8+5 = 24   ==> 2024-06-25 12:57:39.123
     * @param timeStr
     * @return
     */
    public static final boolean checkTimeFormat(final String timeStr){
        if(StringUtil.isBlank(timeStr)){
            return false;
        }
        //
        final int length = timeStr.length();
        if(!(length == 10 || length == 19 || length == 21 || length == 22 || length == 23 || length == 24)){
            return false;
        }
        // 取值
        //
        String year = timeStr.substring(0, 4);  // 2024
        String separator1 = timeStr.substring(4, 5);  // -
        String month = timeStr.substring(5, 7);  // 01
        String separator2 = timeStr.substring(7, 8);  // -
        String day = timeStr.substring(8, 10);  // 23
        //
        if(!StringUtil.equalsOfAll("-", separator1, separator2)){
            return false;
        }
        if(!RegexUtil.isNumberOfAll(year, month, day)){
            return false;
        }
        //
        int year2 = Integer.parseInt(year);
        int month2 = Integer.parseInt(month);
        int day2 = Integer.parseInt(day);
        if(month2 < 1 || month2 > 12){
            return false;
        }
        int days = daysOfMonth(year2, month2);
        if(day2 < 1 || day2 > days){
            return false;
        }
        if(length == 10){
            return true;
        }
        //
        String hour = timeStr.substring(11, 13);  // 45
        String separator3 = timeStr.substring(13, 14);  // :
        String minute = timeStr.substring(14, 16);  // 67
        String separator4 = timeStr.substring(16, 17);  // :
        String second = timeStr.substring(17, 19);  // 89
        //
        if(!StringUtil.equalsOfAll(":", separator3, separator4)){
            return false;
        }
        if(!RegexUtil.isNumberOfAll(hour, minute, second)){
            return false;
        }
        int hour2 = Integer.parseInt(hour);
        int minute2 = Integer.parseInt(minute);
        int second2 = Integer.parseInt(second);
        if(hour2 < 0 || hour2 > 23){
            return false;
        }
        if(minute2 < 0 || minute2 > 59){
            return false;
        }
        if(second2 < 0 || second2 > 59){
            return false;
        }
        // 带有毫秒
        if(length == 21 || length == 22 || length == 23 || length == 24){
            String separator5 = timeStr.substring(19, 20);  // .
            String millisecond = timeStr.substring(20, length);  // 89
            if(!StringUtil.equalsOfAll(".", separator5)){
                return false;
            }
            if(length == 24){
                millisecond = millisecond.substring(0, 3);
            }
            if(!RegexUtil.isNumberOfAll(millisecond)){
                return false;
            }
            int millisecond2 = Integer.parseInt(millisecond);
            if(millisecond2 < 0 || millisecond2 > 999){
                return false;
            }
        }
        //
        return true;
    }

    /**
     * 2024-06-25
     * 指定年份、月份 对应 的天数
     * @param yearAndMonth 年份-月份，格式如：2024-06
     * @return
     */
    public static final int daysOfMonth(final String yearAndMonth){
        if(StringUtil.isBlank(yearAndMonth)){
            return 0;
        }
        String[] array = StringUtil.split(yearAndMonth, "-");
        String year = array[0];
        String month = array[1];
        int days = daysOfMonth(year, month);
        return days;
    }

    /**
     * 2024-06-25
     * 指定年份、月份 对应 的天数
     * @param year
     * @param month
     * @return
     */
    public static final int daysOfMonth(final String year, final String month){
        if(StringUtil.isBlankOfAll(year, month)){
            return 0;
        }
        if(!RegexUtil.isNumberOfAll(year, month)){
            return 0;
        }
        //
        int year2 = Integer.parseInt(year);
        int month2 = Integer.parseInt(month);
        //
        int days = daysOfMonth(year2, month2);
        return days;
    }

    /**
     * 2024-06-25
     * 指定年份、月份 对应 的天数
     * @param year
     * @param month
     * @return
     */
    public static final int daysOfMonth(final int year, final int month){
        if(month < 1 || month > 12){
            return 0;
        }
        //
        YearMonth yearMonth = YearMonth.of(year, month);
        int days = yearMonth.lengthOfMonth();
        return days;
    }

    /**
     * 2020-10-15 6.7.0
     * 把指定的秒数转换为指定格式，如 getTimeValue(3600, TimerUtil.FORMATTYPE6) ==> 1:00:00
     * @param second 秒
     */
    public static final String getTimeValue(final long second){
        String value = getTimeValue(second, FORMATTYPE6);
        return value;
    }

    /**
     * 2020-10-15 6.7.0
     * 把指定的秒数转换为指定格式，如 getTimeValue(3600, TimerUtil.FORMATTYPE6) ==> 1:00:00
     * @param second 秒
     * @param unit 单位
     */
    public static final String getTimeValue(final long second, final String unit){
        if(FORMATTYPE6.equals(unit)){
            if(second < 60){
                String ss = StringUtil.suplyZero(second, 2);
                return "00:00:" + ss;
            }
            if(second < 3600){
                int minute = (int) second / 60;
                int ssi = (int) second - (minute * 60);
                String minutes = StringUtil.suplyZero(minute, 2);
                String sss = StringUtil.suplyZero(ssi, 2);
                String value = "00:" + minutes + ":" + sss;
                return value;
            }
            int hour = (int) second / 3600;
            int second2 = (int) second - hour * 3600;
            int minute = (int) second2 / 60;
            int ssi = (int) second2 - (minute * 60);
            String minutes = StringUtil.suplyZero(minute, 2);
            String sss = StringUtil.suplyZero(ssi, 2);
            String value = hour + ":" + minutes + ":" + sss;
            return value;
        }
        String value = second + "";
        return value;
    }

    /**
     * 2019-09-03 2.3.6
     * 时间字符串转为XMLGregorianCalendar对象，常用于webservice应用
     * @param str
     */
    public static XMLGregorianCalendar str2xml(String str){
        Date date = str2time(str);
        XMLGregorianCalendar xmlGregorianCalendar = date2xml(date);
        return xmlGregorianCalendar;
    }

    /**
     * 2019-04-27 2.1.3
     * Date对象转为XMLGregorianCalendar对象，常用于webservice应用
     * @param date
     */
    public static XMLGregorianCalendar date2xml(Date date){
        if(date == null){
            return null;
        }
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(date);
        try{
            DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
            XMLGregorianCalendar xmlGregorianCalendar = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
            return xmlGregorianCalendar;
        }catch(Throwable throwable){
            LogUtil.error("ObjectUtil_date2xml_1", throwable);
            return null;
        }
    }

    /**
     * 2019-09-03 2.3.6
     * XMLGregorianCalendar对象转为时间字符串，常用于webservice应用
     */
    public static String xml2str(XMLGregorianCalendar xmlGregorianCalendar){
        if(xmlGregorianCalendar == null){
            return null;
        }
        GregorianCalendar gregorianCalendar = xmlGregorianCalendar.toGregorianCalendar();
        Date date = gregorianCalendar.getTime();
        String str = time2str(date);
        return str;
    }

    /**
     * XMLGregorianCalendar对象转为时间对象，常用于webservice应用
     */
    public static Date xml2date(XMLGregorianCalendar xmlGregorianCalendar){
        if(xmlGregorianCalendar == null){
            return null;
        }
        GregorianCalendar gregorianCalendar = xmlGregorianCalendar.toGregorianCalendar();
        Date date = gregorianCalendar.getTime();
        return date;
    }

    /**
     * 2024-11-22
     * @return
     */
    public static final String str2str(){
        final String toTimeStr = str2str(null, null);
        return toTimeStr;
    }

    /**
     * 2024-11-22
     * @param fromTimeStr
     * @return
     */
    public static final String str2str(final String fromTimeStr){
        final String toTimeStr = str2str(fromTimeStr, null);
        return toTimeStr;
    }

    /**
     * 2017-05-27 1.0.0
     * 2018-06-08 1.1.0
     * 2019-04-27 2.2.0
     * 2019-09-03 2.3.6
     * 2024-11-20
     * 2025-09-22 代码重构
     * @param fromTimeStr
     * @param toFormatType
     * @return
     */
    public static final String str2str(String fromTimeStr, String toFormatType){
        if(StringUtil.isBlank(fromTimeStr)){
            fromTimeStr = str2std(null);
        }
        final String stdTimeStr = str2std(fromTimeStr);
        String fromFormatType = getFormatType(stdTimeStr);
        // 2026-08-14 系统与所有数据库表均统一时间格式
        // 缺陷修复：str2std 对畸形格式(如 2025-01-01 00:00.000)返回空串时 getFormatType 返回 null，
        // ofPattern(null) 会裸抛 NullPointerException(实测 p_time str2std_117 用例)，与 str2std 失败返回空串的语义对齐
        if(StringUtil.isBlank(fromFormatType)){
            return "";
        }
        DateTimeFormatter fromDateTimeFormatter = DateTimeFormatter.ofPattern(fromFormatType);
        LocalDateTime localDateTime = LocalDateTime.parse(stdTimeStr, fromDateTimeFormatter);
        if(StringUtil.isBlank(toFormatType)){
            toFormatType = formatType_default;
        }
        DateTimeFormatter toDateTimeFormatter = DateTimeFormatter.ofPattern(toFormatType);
        final String toTimeStr = localDateTime.format(toDateTimeFormatter);
        return toTimeStr;
    }

    /**
     * 2025-09-21
     * 各时间格式包括时间戳字符串转为标准格式字符串
     * @param fromTimeStr
     * @return
     */
    public static final String str2std(String fromTimeStr){
        if(StringUtil.isBlank(fromTimeStr)){
            fromTimeStr = System.currentTimeMillis() + "";
        }
        if(p_monthMap == null){
            p_monthMap = new HashMap<>(12);
            p_monthMap.put("Jan", "01");
            p_monthMap.put("Feb", "02");
            p_monthMap.put("Mar", "03");
            p_monthMap.put("Apr", "04");
            p_monthMap.put("May", "05");
            p_monthMap.put("Jun", "06");
            p_monthMap.put("Jul", "07");
            p_monthMap.put("Aug", "08");
            p_monthMap.put("Sep", "09");
            p_monthMap.put("Oct", "10");
            p_monthMap.put("Nov", "11");
            p_monthMap.put("Dec", "12");
            p_weekMap = new HashMap<>(7);
            p_weekMap.put("Mon", "1");
            p_weekMap.put("Tues", "2");
            p_weekMap.put("Wed", "3");
            p_weekMap.put("Thur", "4");
            p_weekMap.put("Fri", "5");
            p_weekMap.put("Sat", "6");
            p_weekMap.put("Sun", "7");
        }
        fromTimeStr = StringUtil.getPureStr(fromTimeStr);
        fromTimeStr = StringUtil.trim(fromTimeStr);
        if(StringUtil.isBlank(fromTimeStr)){
            LogUtil.error("TimerUtil_str2std_1", "时间参数为空", fromTimeStr);
            return "";
        }
        //
        final int length = fromTimeStr.length();
        //
        if(length == 8){
            // yyyyMMdd  ==>  20250921  // 8
            if(RegexUtil.isNumber(fromTimeStr)){
                String year = fromTimeStr.substring(0, 4);
                String month = fromTimeStr.substring(4, 6);
                String day = fromTimeStr.substring(6, 8);
                String stdTimeStr = year + "-" + month + "-" + day + " 00:00:00.000";
                return stdTimeStr;
            }
            String tag1 = fromTimeStr.substring(2, 3);
            String tag2 = fromTimeStr.substring(5, 6);
            if(StringUtil.equalsOfAll(":", tag1, tag2)){
                LocalDate localDate = LocalDate.now();
                int year = localDate.getYear();
                int month = localDate.getMonthValue();
                String monthStr = StringUtil.suplyZero(month, 2);
                int day = localDate.getDayOfMonth();
                String dayStr = StringUtil.suplyZero(day, 2);
                String stdTimeStr = year + "-" + monthStr + "-" + dayStr + " " + fromTimeStr + ".000";
                return stdTimeStr;
            }
            LogUtil.error("TimerUtil_str2std_8", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        // 2026-4-11
        if(length == 9){
            String yearPrefix = fromTimeStr.substring(0, 2);
            String tag1 = fromTimeStr.substring(4, 5);
            String tag2 = fromTimeStr.substring(6, 7);
            if("20".equals(yearPrefix) && (StringUtil.equalsOfAll("-", tag1, tag2) || StringUtil.equalsOfAll("/", tag1, tag2))){
                String year = fromTimeStr.substring(0, 4);
                String month = fromTimeStr.substring(5, 6);
                month = "0" + month;
                String day = fromTimeStr.substring(7, 9);
                String stdTimeStr = year + "-" + month + "-" + day + " 00:00:00.000";
                return stdTimeStr;
            }
            LogUtil.error("TimerUtil_str2std_9", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 10){
            // yyyy-MM-dd  ==>  2025-09-21  // 10
            // yyyy/MM/dd  ==>  2025/09/21  // 10
            String tag1 = fromTimeStr.substring(4, 5);
            String tag2 = fromTimeStr.substring(7, 8);
            if(StringUtil.equalsOfAll("-", tag1, tag2) || StringUtil.equalsOfAll("/", tag1, tag2)){
                String year = fromTimeStr.substring(0, 4);
                String month = fromTimeStr.substring(5, 7);
                String day = fromTimeStr.substring(8, 10);
                String stdTimeStr = year + "-" + month + "-" + day + " 00:00:00.000";
                return stdTimeStr;
            }
            // dd-MM-yyyy  ==>  21-09-2025  // 10
            // dd/MM/yyyy  ==>  21/09/2025  // 10
            tag1 = fromTimeStr.substring(2, 3);
            tag2 = fromTimeStr.substring(5, 6);
            if(StringUtil.equalsOfAll("-", tag1, tag2) || StringUtil.equalsOfAll("/", tag1, tag2)){
                String year = fromTimeStr.substring(6, 10);
                String month = fromTimeStr.substring(3, 5);
                String day = fromTimeStr.substring(0, 2);
                String stdTimeStr = year + "-" + month + "-" + day + " 00:00:00.000";
                return stdTimeStr;
            }
            // 10位秒级时间戳
            if(RegexUtil.isNumber(fromTimeStr)){
                fromTimeStr += "000";
                // 2026-08-14 系统与所有数据库表均统一时间格式
                // 缺陷修复：传 FORMATTYPE_M1 时 getStrByStamp 输出已含毫秒(.000)，再拼 ".000" 会得 27 位双毫秒导致下游解析失败(实测 str2time() 无参即触发)；
                // 改传 FORMATTYPE1 输出 19 位无毫秒，拼接 ".000" 后恢复为标准 23 位
                String stdTimeStr = getStrByStamp(fromTimeStr, FORMATTYPE1);
                stdTimeStr += ".000";
                return stdTimeStr;
            }
            LogUtil.error("TimerUtil_str2std_10", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 11){
            // dd-MMM-yyyy  ==>  21-Sep-2025  // 11
            // dd/MMM/yyyy  ==>  21/Sep/2025  // 11
            // dd.MMM.yyyy  ==>  21.Sep.2025  // 11
            String tag1 = fromTimeStr.substring(2, 3);
            String tag2 = fromTimeStr.substring(6, 7);
            String tag3 = fromTimeStr.substring(3, 6);
            if((StringUtil.containOfAll("-", tag1, tag2) || StringUtil.equalsOfAll("/", tag1, tag2) || StringUtil.equalsOfAll(".", tag1, tag2))){
                if(p_monthMap.containsKey(tag3)){
                    String year = fromTimeStr.substring(7, 11);
                    String month = p_monthMap.get(tag3);
                    String day = fromTimeStr.substring(0, 2);
                    String stdTimeStr = year + "-" + month + "-" + day + " 00:00:00.000";
                    return stdTimeStr;
                }
            }
            // yyyy年MM月dd日  ==>  2025年09月21日  // 11
            if(StringUtil.containOfOne(fromTimeStr, "年", "月", "日")){
                String year = fromTimeStr.substring(0, 4);
                String month = fromTimeStr.substring(5, 7);
                String day = fromTimeStr.substring(8, 10);
                String stdTimeStr = year + "-" + month + "-" + day + " 00:00:00.000";
                return stdTimeStr;
            }
            LogUtil.error("TimerUtil_str2std_11", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 12){
            String tag1 = fromTimeStr.substring(2, 3);
            String tag2 = fromTimeStr.substring(5, 6);
            String tag3 = fromTimeStr.substring(8, 9);
            if(StringUtil.containOfAll(":", tag1, tag2)){
                if(".".equals(tag3)){
                    LocalDate localDate = LocalDate.now();
                    int year = localDate.getYear();
                    int month = localDate.getMonthValue();
                    String monthStr = StringUtil.suplyZero(month, 2);
                    int day = localDate.getDayOfMonth();
                    String dayStr = StringUtil.suplyZero(day, 2);
                    String stdTimeStr = year + "-" + monthStr + "-" + dayStr + " " + fromTimeStr;
                    return stdTimeStr;
                }
            }
            LogUtil.error("TimerUtil_str2std_12", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 13){
            if(RegexUtil.isNumber(fromTimeStr)){
                // 13位毫秒级时间戳
                if(RegexUtil.isNumber(fromTimeStr)){
                    String first = fromTimeStr.substring(0, 10) + "000";
                    String millisecond = fromTimeStr.substring(10, 13);
                    // 2026-08-14 系统与所有数据库表均统一时间格式
                    // 缺陷修复：传 FORMATTYPE_M1 时 getStrByStamp 输出已含毫秒(.000)，再拼 "." + millisecond 会得 27 位双毫秒；
                    // 改传 FORMATTYPE1 输出 19 位无毫秒，拼接后恢复为标准 23 位
                    String stdTimeStr = getStrByStamp(first, FORMATTYPE1);
                    stdTimeStr += "." + millisecond;
                    return stdTimeStr;
                }
            }
            LogUtil.error("TimerUtil_str2std_13", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        // 2026-08-14 系统与所有数据库表均统一时间格式
        // 新增未补零日期时间格式：yyyy-M-d H:m:s(月/日/时/分/秒各段1~2位组合，总长14~18) 如 2023-9-11 9:43:54、2025-10-6 12:6:47
        // 解析方式与下方各长度分支同款(substring定位截段+拼接返回)，数字段补零到2位；
        // 注意：不满足特征时不返回空而是继续走下方既有分支(14位yyyyMMddHHmmss/15位时间段/16位T格式不受影响)，全不匹配由各分支或兜底报未知格式
        if(length >= 14 && length <= 18){
            int spaceIdx = fromTimeStr.indexOf(' ');
            // 空格位置 = 年4+分隔1+月(1~2)+分隔1+日(1~2)，只可能是 8/9/10
            if(spaceIdx >= 8 && spaceIdx <= 10){
                String tag1 = fromTimeStr.substring(4, 5);
                // 第二个日期分隔符位置只可能是 6(月1位)/7(月2位)，且与第一个相同
                int tag2Idx = -1;
                if(StringUtil.equals(tag1, fromTimeStr.substring(6, 7))){
                    tag2Idx = 6;
                }else if(StringUtil.equals(tag1, fromTimeStr.substring(7, 8))){
                    tag2Idx = 7;
                }
                int colon1Idx = fromTimeStr.indexOf(':', spaceIdx);
                int colon2Idx = colon1Idx > -1 ? fromTimeStr.indexOf(':', colon1Idx + 1) : -1;
                if(tag2Idx > -1 && StringUtil.equalsOfOne(tag1, "-", "/", ".") && colon1Idx > spaceIdx && colon2Idx > colon1Idx){
                    String year = fromTimeStr.substring(0, 4);
                    String month = fromTimeStr.substring(5, tag2Idx);
                    String day = fromTimeStr.substring(tag2Idx + 1, spaceIdx);
                    String hour = fromTimeStr.substring(spaceIdx + 1, colon1Idx);
                    String minute = fromTimeStr.substring(colon1Idx + 1, colon2Idx);
                    String second = fromTimeStr.substring(colon2Idx + 1);
                    // 年4位+月/日/时/分/秒各1~2位纯数字才补零拼接(秒含毫秒点等不满足则继续走下方分支)
                    if(RegexUtil.isNumber(year) && RegexUtil.isNumber(month) && RegexUtil.isNumber(day)
                            && RegexUtil.isNumber(hour) && RegexUtil.isNumber(minute) && RegexUtil.isNumber(second)
                            && month.length() <= 2 && day.length() <= 2 && hour.length() <= 2
                            && minute.length() <= 2 && second.length() <= 2){
                        String stdTimeStr = year + "-" + StringUtil.suplyZero(Integer.parseInt(month), 2) + "-" + StringUtil.suplyZero(Integer.parseInt(day), 2)
                                + " " + StringUtil.suplyZero(Integer.parseInt(hour), 2) + ":" + StringUtil.suplyZero(Integer.parseInt(minute), 2)
                                + ":" + StringUtil.suplyZero(Integer.parseInt(second), 2) + ".000";
                        return stdTimeStr;
                    }
                }
            }
        }
        //
        if(length == 14){
            // yyyyMMddHHmmss  ==>  20250921143045  // 14
            if(RegexUtil.isNumber(fromTimeStr)){
                String year = fromTimeStr.substring(0, 4);
                String month = fromTimeStr.substring(4, 6);
                String day = fromTimeStr.substring(6, 8);
                String hour = fromTimeStr.substring(8, 10);
                String minute = fromTimeStr.substring(10, 12);
                String second = fromTimeStr.substring(12, 14);
                String stdTimeStr = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second + ".000";
                return stdTimeStr;
            }
            LogUtil.error("TimerUtil_str2std_14", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 15){
            String tag1 = fromTimeStr.substring(2, 3);
            String tag2 = fromTimeStr.substring(5, 6);
            String tag3 = fromTimeStr.substring(8, 9);
            if(StringUtil.containOfAll(":", tag1, tag2)){
                if(".".equals(tag3)){
                    LocalDate localDate = LocalDate.now();
                    int year = localDate.getYear();
                    int month = localDate.getMonthValue();
                    String monthStr = StringUtil.suplyZero(month, 2);
                    int day = localDate.getDayOfMonth();
                    String dayStr = StringUtil.suplyZero(day, 2);
                    fromTimeStr = fromTimeStr.substring(0, 12);
                    String stdTimeStr = year + "-" + monthStr + "-" + dayStr + " " + fromTimeStr;
                    return stdTimeStr;
                }
            }
            LogUtil.error("TimerUtil_str2std_15", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 16){
            // yyyyMMddTHHmmssZ  ==>  20240922T143000Z  // 16
            String tag1 = fromTimeStr.substring(8, 9);
            if(StringUtil.equals(tag1, "T") && StringUtil.lastsWith(fromTimeStr, "Z")){
                String year = fromTimeStr.substring(0, 4);
                String month = fromTimeStr.substring(4, 6);
                String day = fromTimeStr.substring(6, 8);
                String hour = fromTimeStr.substring(9, 11);
                String minute = fromTimeStr.substring(11, 13);
                String second = fromTimeStr.substring(13, 15);
                String stdTimeStr = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second + ".000";
                return stdTimeStr;
            }
            LogUtil.error("TimerUtil_str2std_16", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 19){
            // yyyy-MM-dd HH:mm:ss  ==>  2025-09-21 14:30:45  // 19
            // yyyy/MM/dd HH:mm:ss  ==>  2025/09/21 14:30:45  // 19
            // yyyy.MM.dd HH:mm:ss  ==>  2025.09.21 14:30:45  // 19
            // yyyy MM dd HH:mm:ss  ==>  2025.09.21 14:30:45  // 19
            // 2026-05-11T00:00:00
            // 2026/05/11T00:00:00
            // 2026.05.11T00:00:00
            // 2026-05-11 00:00:00
            // 2026/05/11 00:00:00
            // 2026.05.11 00:00:00
            String tag1 = fromTimeStr.substring(4, 5);
            String tag2 = fromTimeStr.substring(7, 8);
            String tag3 = fromTimeStr.substring(10, 11);
            String tag4 = fromTimeStr.substring(13, 14);
            String tag5 = fromTimeStr.substring(16, 17);
            String year = fromTimeStr.substring(0, 4);
            String month = fromTimeStr.substring(5, 7);
            String day = fromTimeStr.substring(8, 10);
            String hour = fromTimeStr.substring(11, 13);
            String minute = fromTimeStr.substring(14, 16);
            String second = fromTimeStr.substring(17, 19);
            if((StringUtil.containOfAll("-", tag1, tag2) || StringUtil.containOfAll("/", tag1, tag2) || StringUtil.containOfAll(".", tag1, tag2) || StringUtil.containOfAll(" ", tag1, tag2))){
                if(StringUtil.equalsOfOne(tag3, " ", "T") && StringUtil.containOfAll(":", tag4, tag5)){
                    String stdTimeStr = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second + ".000";
                    return stdTimeStr;
                }
            }
            // dd-MM-yyyy HH:mm:ss  ==>  21-09-2025 14:30:45  // 19
            // dd/MM/yyyy HH:mm:ss  ==>  21/09/2025 14:30:45  // 19
            // dd.MM.yyyy HH:mm:ss  ==>  21.09.2025 14:30:45  // 19
            // dd MM yyyy HH:mm:ss  ==>  21 09 2025 14:30:45  // 19
            tag1 = fromTimeStr.substring(2, 3);
            tag2 = fromTimeStr.substring(5, 6);
            tag3 = fromTimeStr.substring(10, 11);
            tag4 = fromTimeStr.substring(13, 14);
            tag5 = fromTimeStr.substring(16, 17);
            year = fromTimeStr.substring(6, 10);
            month = fromTimeStr.substring(3, 5);
            day = fromTimeStr.substring(0, 2);
            if((StringUtil.containOfAll("-", tag1, tag2) || StringUtil.containOfAll("/", tag1, tag2) || StringUtil.containOfAll(".", tag1, tag2) || StringUtil.containOfAll(" ", tag1, tag2))){
                if(StringUtil.equalsOfOne(tag3, " ", "T") && StringUtil.containOfAll(":", tag4, tag5)){
                    String stdTimeStr = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second + ".000";
                    return stdTimeStr;
                }
            }
            LogUtil.error("TimerUtil_str2std_19", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 20){
            String tag1 = fromTimeStr.substring(2, 3);
            String tag2 = fromTimeStr.substring(6, 7);
            String tag3 = fromTimeStr.substring(11, 12);
            String tag4 = fromTimeStr.substring(14, 15);
            String tag5 = fromTimeStr.substring(17, 18);
            String year = fromTimeStr.substring(7, 11);
            String month = fromTimeStr.substring(3, 6);
            if(p_monthMap.containsKey(month)){
                month = p_monthMap.get(month);
                String day = fromTimeStr.substring(0, 2);
                String hour = fromTimeStr.substring(12, 14);
                String minute = fromTimeStr.substring(15, 17);
                String second = fromTimeStr.substring(18, 20);
                // dd-MMM-yyyy HH:mm:ss  ==>  21-Sep-2025 14:30:45  // 20
                // dd/MMM/yyyy HH:mm:ss  ==>  21/Sep/2025 14:30:45  // 20
                // dd.MMM.yyyy HH:mm:ss  ==>  21.Sep.2025 14:30:45  // 20
                // dd MMM yyyy HH:mm:ss  ==>  21 Sep 2025 14:30:45  // 20
                if((StringUtil.containOfAll("-", tag1, tag2) || StringUtil.containOfAll("/", tag1, tag2) || StringUtil.containOfAll(".", tag1, tag2) || StringUtil.containOfAll(" ", tag1, tag2))){
                    if(StringUtil.equalsOfOne(tag3, " ", "T") && StringUtil.containOfAll(":", tag4, tag5)){
                        String stdTimeStr = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second + ".000";
                        return stdTimeStr;
                    }
                }
            }
            // YYYY-MM-DDTHH:MM:SSZ  ==>  2023-10-05T14:30:45Z  // 20
            if(StringUtil.lastsWith(fromTimeStr, "Z")){
                tag1 = fromTimeStr.substring(10, 11);
                if(StringUtil.equalsOfOne(tag1, " ", "T")){
                    fromTimeStr = StringUtil.replaceAll(fromTimeStr, "T", " ");
                    fromTimeStr = StringUtil.deleteAll(fromTimeStr, "Z");
                    String stdTimeStr = fromTimeStr + ".000";
                    return stdTimeStr;
                }
            }
            // 2026-08-14 系统与所有数据库表均统一时间格式
            // 新增缺秒畸形格式：yyyy-MM-dd HH:mm.SSS(实测 T06 样本 2025-01-01 00:00.000，分之后用点直接接毫秒，秒缺失按 00 补)
            // yyyy/MM/dd HH:mm.SSS、yyyy.MM.dd HH:mm.SSS、yyyy MM dd HH:mm.SSS 同款处理
            {
                String ntag1 = fromTimeStr.substring(4, 5);
                String ntag2 = fromTimeStr.substring(7, 8);
                String ntag3 = fromTimeStr.substring(10, 11);
                String ntag4 = fromTimeStr.substring(13, 14);
                String ntag5 = fromTimeStr.substring(16, 17);
                if((StringUtil.containOfAll("-", ntag1, ntag2) || StringUtil.containOfAll("/", ntag1, ntag2) || StringUtil.containOfAll(".", ntag1, ntag2) || StringUtil.containOfAll(" ", ntag1, ntag2))){
                    if(StringUtil.equalsOfOne(ntag3, " ", "T") && ":".equals(ntag4) && ".".equals(ntag5)){
                        String nyear = fromTimeStr.substring(0, 4);
                        String nmonth = fromTimeStr.substring(5, 7);
                        String nday = fromTimeStr.substring(8, 10);
                        String nhour = fromTimeStr.substring(11, 13);
                        String nminute = fromTimeStr.substring(14, 16);
                        String nms = fromTimeStr.substring(17, 20);
                        String stdTimeStr = nyear + "-" + nmonth + "-" + nday + " " + nhour + ":" + nminute + ":00." + nms;
                        return stdTimeStr;
                    }
                }
            }
            LogUtil.error("TimerUtil_str2std_20", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 21){
            // dd-MM-yy hh.mm.ss. a  ==>  25-9月-25 09.50.49. 上午  // 21
            if(StringUtil.containOf(fromTimeStr, "月") && (StringUtil.lastsWith(fromTimeStr, "上午") || StringUtil.lastsWith(fromTimeStr, "下午"))){
                String stdTimeStr = dmDate2stdDate(fromTimeStr);
                return stdTimeStr;
            }
            String tag1 = fromTimeStr.substring(4, 5);
            String tag2 = fromTimeStr.substring(7, 8);
            String tag3 = fromTimeStr.substring(10, 11);
            String tag4 = fromTimeStr.substring(13, 14);
            String tag5 = fromTimeStr.substring(16, 17);
            String tag6 = fromTimeStr.substring(19, 20);
            String last = fromTimeStr.substring(20, 21);
            int lastInt = Integer.parseInt(last);
            String millisecond = StringUtil.suplyZero(lastInt, 3);
            String first = fromTimeStr.substring(0, 19);
            if ((StringUtil.containOfAll("-", tag1, tag2) || StringUtil.containOfAll("/", tag1, tag2) || StringUtil.containOfAll(".", tag1, tag2) || StringUtil.containOfAll(" ", tag1, tag2))) {
                if(StringUtil.equalsOfOne(tag3, " ", "T") && StringUtil.containOfAll(":", tag4, tag5)){
                    // yyyy-MM-dd HH:mm:ss.S  ==>  2025-06-12 09:38:09.3  // 23
                    // yyyy/MM/dd HH:mm:ss.S  ==>  2025-06-12 09:38:09.3  // 23
                    // yyyy.MM.dd HH:mm:ss.S  ==>  2025-06-12 09:38:09.3  // 23
                    // yyyy MM dd HH:mm:ss.S  ==>  2025-06-12 09:38:09.3  // 23
                    // yyyy-MM-dd HH:mm:ss:S  ==>  2025-06-12 09:38:09:3  // 23
                    // yyyy/MM/dd HH:mm:ss:S  ==>  2025-06-12 09:38:09:3  // 23
                    // yyyy.MM.dd HH:mm:ss:S  ==>  2025-06-12 09:38:09:3  // 23
                    // yyyy MM dd HH:mm:ss:S  ==>  2025-06-12 09:38:09:3  // 23
                    if(StringUtil.equalsOfOne(tag6, ".", ":")){
                        String stdTimeStr = first + "." + millisecond;
                        return stdTimeStr;
                    }
                }
            }
            LogUtil.error("TimerUtil_str2std_23", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 22){
            // yyyy-MM-dd hh:mm:ss a  ==>  2025-09-21 02:30:45 PM  // 22
            // yyyy/MM/dd hh:mm:ss a  ==>  2025/09/21 02:30:45 PM  // 22
            // yyyy.MM.dd hh:mm:ss a  ==>  2025.09.21 02:30:45 PM  // 22
            // yyyy MM dd hh:mm:ss a  ==>  2025 09 21 02:30:45 PM  // 22
            String tag1 = fromTimeStr.substring(4, 5);
            String tag2 = fromTimeStr.substring(7, 8);
            String tag3 = fromTimeStr.substring(10, 11);
            String tag4 = fromTimeStr.substring(13, 14);
            String tag5 = fromTimeStr.substring(16, 17);
            String tag6 = fromTimeStr.substring(19, 20);
            String year = fromTimeStr.substring(0, 4);
            String month = fromTimeStr.substring(5, 7);
            String day = fromTimeStr.substring(8, 10);
            String hour = fromTimeStr.substring(11, 13);
            String minute = fromTimeStr.substring(14, 16);
            String second = fromTimeStr.substring(17, 19);
            String day2 = fromTimeStr.substring(20, 22);
            if(StringUtil.containOfAll("-", tag1, tag2) || StringUtil.containOfAll("/", tag1, tag2) || StringUtil.containOfAll(".", tag1, tag2) || StringUtil.containOfAll(" ", tag1, tag2)){
                if(StringUtil.equalsOfAll(" ", tag3, tag6) && StringUtil.containOfAll(":", tag4, tag5)){
                    if(StringUtil.equals("AM", day2)){
                        String stdTimeStr = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second + ".000";
                        return stdTimeStr;
                    }else if(StringUtil.equals("PM", day2)){
                        Integer hourInt = Integer.valueOf(hour);
                        hourInt += 12;
                        String stdTimeStr = year + "-" + month + "-" + day + " " + hourInt + ":" + minute + ":" + second + ".000";
                        return stdTimeStr;
                    }
                }
                if(StringUtil.equalsOfOne(tag6, ".", ":")){
                    String last = fromTimeStr.substring(20, 22);
                    int lastInt = Integer.parseInt(last);
                    String millisecond = StringUtil.suplyZero(lastInt, 3);
                    String first = fromTimeStr.substring(0, 19);
                    String stdTimeStr = first + "." + millisecond;
                    return stdTimeStr;
                }
            }
            // dd-MMM-yy hh.mm.ss. a  ==>  04-12月-19 09.44.30. 上午  // 22
            if(StringUtil.containOf(fromTimeStr, "月") && (StringUtil.lastsWith(fromTimeStr, "上午") || StringUtil.lastsWith(fromTimeStr, "下午"))){
                String stdTimeStr = dmDate2stdDate(fromTimeStr);
                return stdTimeStr;
            }
            LogUtil.error("TimerUtil_str2std_22", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 23) {
            String tag1 = fromTimeStr.substring(4, 5);
            String tag2 = fromTimeStr.substring(7, 8);
            String tag3 = fromTimeStr.substring(10, 11);
            String tag4 = fromTimeStr.substring(13, 14);
            String tag5 = fromTimeStr.substring(16, 17);
            String tag6 = fromTimeStr.substring(19, 20);
            if ((StringUtil.containOfAll("-", tag1, tag2) || StringUtil.containOfAll("/", tag1, tag2) || StringUtil.containOfAll(".", tag1, tag2) || StringUtil.containOfAll(" ", tag1, tag2))) {
                if(StringUtil.equalsOfOne(tag3, " ", "T") && StringUtil.containOfAll(":", tag4, tag5)){
                    // yyyy-MM-dd HH:mm:ss.SSS  ==>  2025-06-12 09:38:09.476  // 23
                    // yyyy/MM/dd HH:mm:ss.SSS  ==>  2025-06-12 09:38:09.476  // 23
                    // yyyy.MM.dd HH:mm:ss.SSS  ==>  2025-06-12 09:38:09.476  // 23
                    // yyyy MM dd HH:mm:ss.SSS  ==>  2025-06-12 09:38:09.476  // 23
                    if(".".equals(tag6)){
                        String stdTimeStr = fromTimeStr;
                        return stdTimeStr;
                    }
                    // yyyy-MM-dd HH:mm:ss:SSS  ==>  2025-06-12 09:38:09:476  // 23
                    // yyyy/MM/dd HH:mm:ss:SSS  ==>  2025-06-12 09:38:09:476  // 23
                    // yyyy.MM.dd HH:mm:ss:SSS  ==>  2025-06-12 09:38:09:476  // 23
                    // yyyy MM dd HH:mm:ss:SSS  ==>  2025-06-12 09:38:09:476  // 23
                    if (":".equals(tag6)) {
                        String first = fromTimeStr.substring(0, 19);
                        String second = fromTimeStr.substring(20, 23);
                        String stdTimeStr = first + "." + second;
                        return stdTimeStr;
                    }
                }
            }
            LogUtil.error("TimerUtil_str2std_23", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        // 2026-08-14 系统与所有数据库表均统一时间格式
        // 补充 24 位 ISO 8601 UTC 格式支持
        // 输入格式：yyyy-MM-dd'T'HH:mm:ss.SSS'Z' ==>  2020-02-21T00:40:49.000Z  // 24
        // 输出格式：yyyy-MM-dd HH:mm:ss.SSS       ==>  2020-02-21 00:40:49.000   // 23
        if(length == 24){
            String tag1 = fromTimeStr.substring(4, 5);
            String tag2 = fromTimeStr.substring(7, 8);
            String tag3 = fromTimeStr.substring(10, 11);
            String tag4 = fromTimeStr.substring(13, 14);
            String tag5 = fromTimeStr.substring(16, 17);
            String tag6 = fromTimeStr.substring(19, 20);
            String tag7 = fromTimeStr.substring(23, 24);
            if ((StringUtil.containOfAll("-", tag1, tag2) || StringUtil.containOfAll("/", tag1, tag2))) {
                if(StringUtil.equalsOfOne(tag3, "T") && StringUtil.containOfAll(":", tag4, tag5) && ".".equals(tag6) && "Z".equals(tag7)){
                    // 截取前19位并将T替换为空格(2020-02-21T00:40:49 → 2020-02-21 00:40:49)，再拼接毫秒位
                    String first = fromTimeStr.substring(0, 19);
                    first = StringUtil.replaceAll(first, "T", " ");
                    String second = fromTimeStr.substring(20, 23);
                    // 2026-08-14 系统与所有数据库表均统一时间格式
                    // 用户决策改为东八区：Z 结尾为 UTC 时刻，+8 小时换算为东八区时间(plusHours 自动处理跨天/跨月/跨年)，
                    // 输入 2020-02-21T00:40:49.000Z → 输出 2020-02-21 08:40:49.000
                    LocalDateTime localDateTime = LocalDateTime.parse(first, DateTimeFormatter.ofPattern(FORMATTYPE1));
                    localDateTime = localDateTime.plusHours(8);
                    String stdTimeStr = localDateTime.format(DateTimeFormatter.ofPattern(FORMATTYPE1)) + "." + second;
                    return stdTimeStr;
                }
            }
            LogUtil.error("TimerUtil_str2std_24", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 25){
            // yyyy-MM-dd'T'HH:mm:ssXXX  ==>  2025-09-21T14:30:45+08:00  // 25
            if(StringUtil.lastsWith(fromTimeStr, "+08:00") && StringUtil.containOf(fromTimeStr, "T")){
                fromTimeStr = StringUtil.deleteLast(fromTimeStr, "+08:00");
                fromTimeStr = StringUtil.replaceAll(fromTimeStr, "T", " ");
                String stdTimeStr = fromTimeStr + ".000";
                return stdTimeStr;
            }
            LogUtil.error("TimerUtil_str2std_25", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 26){
            String tag1 = fromTimeStr.substring(4, 5);
            String tag2 = fromTimeStr.substring(7, 8);
            String tag3 = fromTimeStr.substring(10, 11);
            String tag4 = fromTimeStr.substring(13, 14);
            String tag5 = fromTimeStr.substring(16, 17);
            String tag6 = fromTimeStr.substring(19, 20);
            if ((StringUtil.containOfAll("-", tag1, tag2) || StringUtil.containOfAll("/", tag1, tag2) || StringUtil.containOfAll(".", tag1, tag2) || StringUtil.containOfAll(" ", tag1, tag2))) {
                if(StringUtil.equalsOfOne(tag3, " ", "T") && StringUtil.containOfAll(":", tag4, tag5)){
                    // yyyy-MM-dd HH:mm:ss.SSSSSS  ==>  2025-06-12 09:38:09.123456  // 26
                    if(".".equals(tag6)){
                        String stdTimeStr = fromTimeStr.substring(0, 23);
                        return stdTimeStr;
                    }
                    // yyyy-MM-dd HH:mm:ss:SSSSSS  ==>  2025-06-12 09:38:09:123456  // 26
                    if (":".equals(tag6)) {
                        String first = fromTimeStr.substring(0, 19);
                        String second = fromTimeStr.substring(20, 23);
                        String stdTimeStr = first + "." + second;
                        return stdTimeStr;
                    }
                }
            }
            LogUtil.error("TimerUtil_str2std_26", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 27){
            String tag1 = fromTimeStr.substring(4, 5);
            String tag2 = fromTimeStr.substring(7, 8);
            String tag3 = fromTimeStr.substring(10, 11);
            String tag4 = fromTimeStr.substring(13, 14);
            String tag5 = fromTimeStr.substring(16, 17);
            String tag6 = fromTimeStr.substring(19, 20);
            if ((StringUtil.containOfAll("-", tag1, tag2) || StringUtil.containOfAll("/", tag1, tag2) || StringUtil.containOfAll(".", tag1, tag2) || StringUtil.containOfAll(" ", tag1, tag2))) {
                if(StringUtil.equalsOfOne(tag3, " ", "T") && StringUtil.containOfAll(":", tag4, tag5)){
                    // yyyy-MM-dd HH:mm:ss.SSSSSSS  ==>  2025-06-12 09:38:09.1234567  // 27
                    if (".".equals(tag6)) {
                        String stdTimeStr = fromTimeStr.substring(0, 23);
                        return stdTimeStr;
                    }
                    // yyyy-MM-dd HH:mm:ss:SSSSSSS  ==>  2025-06-12 09:38:09:1234567  // 27
                    if (":".equals(tag6)) {
                        String first = fromTimeStr.substring(0, 19);
                        String second = fromTimeStr.substring(20, 23);
                        String stdTimeStr = first + "." + second;
                        return stdTimeStr;
                    }
                }
            }
            LogUtil.error("TimerUtil_str2std_27", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 28){
            // yyyy-MM-dd HH:mm:ss.SSSZ  ==>  2025-09-21 14:30:45.123+0800  // 28
            if(StringUtil.lastsWith(fromTimeStr, "+0800")){
                fromTimeStr = StringUtil.deleteLast(fromTimeStr, "+0800");
                String stdTimeStr = fromTimeStr;
                return stdTimeStr;
            }
            if(StringUtil.containOf(fromTimeStr, " CST ")){
                fromTimeStr = StringUtil.deleteAll(fromTimeStr, "CST ");
                String tag1 = fromTimeStr.substring(13, 14);
                String tag2 = fromTimeStr.substring(16, 17);
                String tag3 = fromTimeStr.substring(3, 4);
                String tag4 = fromTimeStr.substring(7, 8);
                String tag5 = fromTimeStr.substring(10, 11);
                String tag6 = fromTimeStr.substring(19, 20);
                // EEE MMM dd HH:mm:ss z yyyy  ==>  Sun Sep 21 14:30:45 CST 2025  // 28
                if(StringUtil.containOfAll(":", tag1, tag2) && StringUtil.containOfAll(" ", tag3, tag4, tag5, tag6)){
                    String year = fromTimeStr.substring(20, 24);
                    String month = fromTimeStr.substring(4, 7);
                    month = p_monthMap.get(month);
                    String day = fromTimeStr.substring(8, 10);
                    String hour = fromTimeStr.substring(11, 13);
                    String minute = fromTimeStr.substring(14, 16);
                    String second = fromTimeStr.substring(17, 19);
                    String stdTimeStr = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second + ".000";
                    return stdTimeStr;
                }
            }
            // Sat Jun 13 00:00:00 CS  2020
            if(StringUtil.containOf(fromTimeStr, " CS  ")){
                String tag1 = fromTimeStr.substring(3, 4);
                String tag2 = fromTimeStr.substring(7, 8);
                String tag3 = fromTimeStr.substring(10, 11);
                String tag4 = fromTimeStr.substring(19, 20);
                String tag5 = fromTimeStr.substring(22, 23);
                if(StringUtil.equalsOfAll(" ", tag1, tag2, tag3, tag4, tag5)){
                    String year = fromTimeStr.substring(24, 28);
                    String month = fromTimeStr.substring(4, 7);
                    month = p_monthMap.get(month);
                    String day = fromTimeStr.substring(8, 10);
                    String hour = fromTimeStr.substring(11, 13);
                    String minute = fromTimeStr.substring(14, 16);
                    String second = fromTimeStr.substring(17, 19);
                    String stdTimeStr = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second + ".000";
                    return stdTimeStr;
                }
            }
            LogUtil.error("TimerUtil_str2std_28", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 29){
            // yyyy-MM-dd'T'HH:mm:ss.SSSXXX  ==>  2025-09-21T14:30:45.123+08:00  // 29
            // yyyy-MM-dd'T'HH:mm:ss.SSSXXX  ==>  2025-09-25T07:16:09.466+00:00  // 29
            String tag1 = fromTimeStr.substring(4, 5);
            String tag2 = fromTimeStr.substring(7, 8);
            String tag3 = fromTimeStr.substring(10, 11);
            String tag4 = fromTimeStr.substring(13, 14);
            String tag5 = fromTimeStr.substring(16, 17);
            String tag6 = fromTimeStr.substring(19, 20);
            String tag7 = fromTimeStr.substring(23, 24);
            if ((StringUtil.containOfAll("-", tag1, tag2) || StringUtil.containOfAll("/", tag1, tag2) || StringUtil.containOfAll(".", tag1, tag2) || StringUtil.containOfAll(" ", tag1, tag2))) {
                if(StringUtil.equalsOfOne(tag3, " ", "T") && StringUtil.containOfAll(":", tag4, tag5) && ".".equals(tag6) && "+".equals(tag7)){
                    fromTimeStr = StringUtil.replaceAll(fromTimeStr, "T", " ");
                    String[] array = StringUtil.split(fromTimeStr, "+");
                    String stdTimeStr = array[0];
                    return stdTimeStr;
                }
            }
            LogUtil.error("TimerUtil_str2std_29", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 30){
            // YYYY-MM-DDTHH:MM:SS.SSSSSSSSSZ  ==>  2025-09-25T07:14:30.227722900Z  // 30
            String tag1 = fromTimeStr.substring(4, 5);
            String tag2 = fromTimeStr.substring(7, 8);
            String tag3 = fromTimeStr.substring(10, 11);
            String tag4 = fromTimeStr.substring(13, 14);
            String tag5 = fromTimeStr.substring(16, 17);
            String tag6 = fromTimeStr.substring(19, 20);
            String tag7 = fromTimeStr.substring(29, 30);
            if ((StringUtil.containOfAll("-", tag1, tag2) || StringUtil.containOfAll("/", tag1, tag2) || StringUtil.containOfAll(".", tag1, tag2) || StringUtil.containOfAll(" ", tag1, tag2))) {
                if(StringUtil.equalsOfOne(tag3, " ", "T") && StringUtil.containOfAll(":", tag4, tag5) && ".".equals(tag6) && "Z".equals(tag7)){
                    fromTimeStr = StringUtil.replaceAll(fromTimeStr, "T", " ");
                    String stdTimeStr = fromTimeStr.substring(0, 23);
                    return stdTimeStr;
                }
            }
            LogUtil.error("TimerUtil_str2std_30", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 32){
            if(StringUtil.lastsWith(fromTimeStr, "+08:00")){
                fromTimeStr = StringUtil.deleteLast(fromTimeStr, "+08:00");
                String tag1 = fromTimeStr.substring(4, 5);
                String tag2 = fromTimeStr.substring(7, 8);
                String tag3 = fromTimeStr.substring(10, 11);
                String tag4 = fromTimeStr.substring(13, 14);
                String tag5 = fromTimeStr.substring(16, 17);
                String tag6 = fromTimeStr.substring(19, 20);
                if ((StringUtil.containOfAll("-", tag1, tag2) || StringUtil.containOfAll("/", tag1, tag2) || StringUtil.containOfAll(".", tag1, tag2) || StringUtil.containOfAll(" ", tag1, tag2)) && StringUtil.equalsOfAll("T", tag3) && StringUtil.containOfAll(":", tag4, tag5)) {
                    // yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX  ==>  2023-10-05T14:30:45.123456+08:00  // 32
                    if (".".equals(tag6)) {
                        fromTimeStr = StringUtil.replaceAll(fromTimeStr, "T", " ");
                        String stdTimeStr = fromTimeStr.substring(0, 23);
                        return stdTimeStr;
                    }
                    // yyyy-MM-dd'T'HH:mm:ss:SSSSSSXXX  ==>  2023-10-05T14:30:45:123456+08:00  // 32
                    if (":".equals(tag6)) {
                        fromTimeStr = StringUtil.replaceAll(fromTimeStr, "T", " ");
                        String first = fromTimeStr.substring(0, 19);
                        String second = fromTimeStr.substring(20, 23);
                        String stdTimeStr = first + "." + second;
                        return stdTimeStr;
                    }
                }
            }
            LogUtil.error("TimerUtil_str2std_32", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 34){
            if(StringUtil.lastsWith(fromTimeStr, " +08:00")){
                fromTimeStr = StringUtil.deleteLast(fromTimeStr, " +08:00");
                String tag1 = fromTimeStr.substring(4, 5);
                String tag2 = fromTimeStr.substring(7, 8);
                String tag3 = fromTimeStr.substring(10, 11);
                String tag4 = fromTimeStr.substring(13, 14);
                String tag5 = fromTimeStr.substring(16, 17);
                String tag6 = fromTimeStr.substring(19, 20);
                if ((StringUtil.containOfAll("-", tag1, tag2) || StringUtil.containOfAll("/", tag1, tag2) || StringUtil.containOfAll(".", tag1, tag2) || StringUtil.containOfAll(" ", tag1, tag2)) && StringUtil.equalsOfOne(tag3, " ") && StringUtil.containOfAll(":", tag4, tag5)) {
                    // yyyy-MM-dd'T'HH:mm:ss.SSSSSSSXXX  ==> 2023-10-05 14:30:45.1234567 +08:00  // 34
                    if (".".equals(tag6)) {
                        String stdTimeStr = fromTimeStr.substring(0, 23);
                        return stdTimeStr;
                    }
                    // yyyy-MM-dd'T'HH:mm:ss:SSSSSSSXXX  ==>  2023-10-05 14:30:45:1234567 +08:00  // 34
                    if (":".equals(tag6)) {
                        String first = fromTimeStr.substring(0, 19);
                        String second = fromTimeStr.substring(20, 23);
                        String stdTimeStr = first + "." + second;
                        return stdTimeStr;
                    }
                }
            }
            LogUtil.error("TimerUtil_str2std_34", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        if(length == 42){
            // 2025-10-23
            // Thu Oct 23 2025 00:00:00 GMT+0800 (中国标准时间)
            if(StringUtil.lastsWith(fromTimeStr, " GMT+0800 (中国标准时间)")){
                fromTimeStr = StringUtil.deleteLast(fromTimeStr, " GMT+0800 (中国标准时间)");
                String tag1 = fromTimeStr.substring(3, 4);
                String tag2 = fromTimeStr.substring(7, 8);
                String tag3 = fromTimeStr.substring(10, 11);
                String tag4 = fromTimeStr.substring(15, 16);
                String tag5 = fromTimeStr.substring(18, 19);
                String tag6 = fromTimeStr.substring(21, 22);
                if(StringUtil.equalsOfAll(" ", tag1, tag2, tag3, tag4) && StringUtil.equalsOfAll(":", tag5, tag6)) {
                    String month = fromTimeStr.substring(4, 7);
                    if(p_monthMap.containsKey(month)){
                        month = p_monthMap.get(month);
                        String day = fromTimeStr.substring(8, 10);
                        String year = fromTimeStr.substring(11, 15);
                        String hms = fromTimeStr.substring(16, 24);
                        String stdTimeStr = year + "-" + month + "-" + day + " " + hms + ".000";
                        return stdTimeStr;
                    }
                }
            }
            LogUtil.error("TimerUtil_str2std_42", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        if(length == 44){
            // EEE MMM dd HH:mm:ss zzz yyyy  ==>  Sun Sep 21 14:30:45 China Standard Time 2025  // 44
            if(StringUtil.containOf(fromTimeStr, " China Standard Time ")){
                fromTimeStr = StringUtil.deleteAll(fromTimeStr, "China Standard Time ");
                String tag1 = fromTimeStr.substring(13, 14);
                String tag2 = fromTimeStr.substring(16, 17);
                String tag3 = fromTimeStr.substring(3, 4);
                String tag4 = fromTimeStr.substring(7, 8);
                String tag5 = fromTimeStr.substring(10, 11);
                String tag6 = fromTimeStr.substring(19, 20);
                // EEE MMM dd HH:mm:ss z yyyy  ==>  Sun Sep 21 14:30:45 CST 2025  // 28
                if(StringUtil.containOfAll(":", tag1, tag2) && StringUtil.containOfAll(" ", tag3, tag4, tag5, tag6)){
                    String year = fromTimeStr.substring(20, 24);
                    String month = fromTimeStr.substring(4, 7);
                    month = p_monthMap.get(month);
                    String day = fromTimeStr.substring(8, 10);
                    String hour = fromTimeStr.substring(11, 13);
                    String minute = fromTimeStr.substring(14, 16);
                    String second = fromTimeStr.substring(17, 19);
                    String stdTimeStr = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second + ".000";
                    return stdTimeStr;
                }
            }
            LogUtil.error("TimerUtil_str2std_44", "未知的时间格式", fromTimeStr, length);
            return "";
        }
        //
        LogUtil.error("TimerUtil_str2std_99", "未知的时间格式", fromTimeStr, length);
        return "";
    }

    /**
     * 2024-11-22
     * @return
     */
    public static final Date str2time(){
        final Date toTimeDate = str2time(null);
        return toTimeDate;
    }


    /**
     * 2017-05-27 1.0.0
     * 2024-11-20 代码重构
     * 2025-06-24 格式匹配兼容
     * @param fromTimeStr 实际时间
     * @return
     */
    public static Date str2time(String fromTimeStr) {
        if(StringUtil.isBlank(fromTimeStr)){
            fromTimeStr = System.currentTimeMillis() + "";
        }
        try{
            String stdTimeStr = str2str(fromTimeStr);
            final TimeZone timeZone = TimeZone.getTimeZone("GMT+8");  // 2025-07-23 设置为本地时区
            Locale locale = Locale.CHINA;  // 2025-07-23
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formatType_default, locale);
            simpleDateFormat.setTimeZone(timeZone);
            simpleDateFormat.setLenient(false);
            Date date = simpleDateFormat.parse(stdTimeStr);
            return date;
        }catch (Throwable throwable){
            LogUtil.error("TimerUtil_str2time_t3", throwable);
            return null;
        }
    }

    /**
     * 2024-11-20
     * 获取当前时间字符串
     * @return
     */
    public static final String now(){
        String nowStr = time2str(null, null);
        return nowStr;
    }

    /**
     * 2024-11-20
     * 获取当前时间字符串
     * @return
     */
    public static final String now(final String toFormatType){
        String nowStr = time2str(null, toFormatType);
        return nowStr;
    }

    /**
     * 2017-05-27 1.0.0
     * 2018-06-08 1.1.0
     * 2019-04-27 2.2.0
     * 2019-09-03 2.3.6
     * 2024-11-20 代码重构
     * @return
     */
    public static final String time2str(){
        String toTimeStr = time2str(null, null);
        return toTimeStr;
    }

    /**
     * 2017-05-27 1.0.0
     * 2018-06-08 1.1.0
     * 2019-04-27 2.2.0
     * 2019-09-03 2.3.6
     * 2024-11-20 代码重构
     * @param toFormatType
     * @return
     */
    public static final String time2str(final String toFormatType){
        String toTimeStr = time2str(null, toFormatType);
        return toTimeStr;
    }

    /**
     * 2017-05-27 1.0.0
     * 2018-06-08 1.1.0
     * 2019-04-27 2.2.0
     * 2019-09-03 2.3.6
     * 2024-11-20 代码重构
     * @param fromDate
     * @return
     */
    public static final String time2str(final Date fromDate){
        String toTimeStr = time2str(fromDate, null);
        return toTimeStr;
    }

    /**
     * 2017-05-27 1.0.0
     * 2018-06-08 1.1.0
     * 2019-04-27 2.2.0
     * 2019-09-03 2.3.6
     * 2024-11-20 代码重构
     * @param fromDate
     * @param toFormatType
     * @return
     */
    public static final String time2str(Date fromDate, String toFormatType){
        if(fromDate == null){
            LocalDateTime fromLocalDateTime = LocalDateTime.now();
            fromDate = localDateTime2date(fromLocalDateTime);
        }
        if(StringUtil.isBlank(toFormatType)){
            toFormatType = formatType_default;
        }
        //
        LocalDateTime fromLocalDateTime = date2localDateTime(fromDate);
        DateTimeFormatter toDateTimeFormatter = DateTimeFormatter.ofPattern(toFormatType);
        String toTimeStr = fromLocalDateTime.format(toDateTimeFormatter);
        return toTimeStr;
    }

    /**
     * 2024-11-20
     * Date对象转为LocalDateTime对象
     * @param date
     * @return
     */
    public static final LocalDateTime date2localDateTime(final Date date){
        // 2026-08-13 拆链式连写为逐步局部变量，便于调试与统一风格
        Instant instant = date.toInstant();
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zonedDateTime = instant.atZone(zoneId);
        LocalDateTime fromLocalDateTime = zonedDateTime.toLocalDateTime();
        return fromLocalDateTime;
    }

    /**
     * 2024-11-20
     * LocalDateTime对象转为Date对象
     * @param localDateTime
     * @return
     */
    public static final Date localDateTime2date(final LocalDateTime localDateTime){
        // 2026-08-13 拆链式连写为逐步局部变量，便于调试与统一风格
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);
        Instant instant = zonedDateTime.toInstant();
        Date date = Date.from(instant);
        return date;
    }

    /**
     * 2020-05-28 6.0.0
     * 2020-05-28T00:00:00+08:00 => 2020-05-28 00:00:00(即格式FORMATTYPE1)
     */
    public static String formatTransfer(String dateStr){
        if(StringUtil.isBlank(dateStr)){
            return "";
        }
        if(!StringUtil.containOf(dateStr, "T") && !StringUtil.containOf(dateStr, "+")){
            return dateStr;
        }
        String[] array = StringUtil.split(dateStr, "T");
        String first = array[0];
        String second = array[1];
        String[] arr = StringUtil.split(second, "+");
        String hms = arr[0];
        String after = first + " " + hms;
        return after;
    }

    /**
     * 2018-11-15 1.1.0
     * @param day 如2018-11-15
     * @param time 如23:59:59
     * @param formatType
     * @return 2018-11-15 ==> 2018-11-15 23:59:59
     */
    public static String getStrFormat(String day, String time, String formatType){
        String[] array = StringUtil.split(time, ":");
        Integer hour = 0;
        if(array.length > 0){
            hour = Integer.valueOf(array[0]);
        }
        Integer minute = 0;
        if(array.length > 1){
            minute = Integer.valueOf(array[1]);
        }
        Integer second = 0;
        if(array.length > 2){
            second = Integer.valueOf(array[2]);
        }
        Integer millisecond = 0;
        if(array.length > 3){
            millisecond = Integer.valueOf(array[3]);
        }
        Date date = str2time(day);
        date = setCalendar("hour", date, hour);
        date = setCalendar("minute", date, minute);
        date = setCalendar("second", date, second);
        date = setCalendar("millisecond", date, millisecond);
        String result = time2str(date, formatType);
        return result;
    }

    /**
     * 2018-08-11 1.1.0
     * @return 返回13位的时间戳
     */
    public static long getStamp(){
        long value = System.currentTimeMillis();
        return value;
    }

    /**
     * 2023-06-13 获取指定时间的时间戳
     * @param date
     * @return 返回13位的时间戳
     */
    public static long getStampByDate(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        long stamp = calendar.getTimeInMillis();
        return stamp;
    }

    /**
     * 2023-06-13 获取指定时间的时间戳
     * @param dateStr 格式如 2023-06-13 14:42:23.987 或 2023-06-13 14:42:23
     * @return 返回13位的时间戳
     */
    public static long getStampByDateStr(String dateStr){
        // 先尝试 FORMATTYPE0(带毫秒，兼容旧数据)
        DateFormat dateFormat0 = new SimpleDateFormat(formatType_default);
        try{
            Date date = dateFormat0.parse(dateStr);
            long stamp = getStampByDate(date);
            return stamp;
        }catch(Throwable throwable0){
            DateFormat dateFormat1 = new SimpleDateFormat(formatType_default);
            try{
                Date date = dateFormat1.parse(dateStr);
                long stamp = getStampByDate(date);
                return stamp;
            }catch(Throwable throwable1){
                LogUtil.error("TimerUtil_getStampByDateStr_e1", "获取指定时间的时间戳失败", dateStr);
                return 0;
            }
        }
    }

    /**
     * 2017-05-27 1.0.0
     * 时间戳转时间格式
     * 传入指定毫秒数，返回对应的时间格式
     * @param stamp 该毫秒数是按照从格林尼治时间1970年1月1日0时0分0秒0毫秒以来来计算的
     * @return 返回格式如：2018-04-12 13:01:50
     */
    public static String getStrByStamp(Long stamp){
        String str = getStrByStamp(stamp, formatType_default);
        return str;
    }

    /**
     * 2022-02-16
     * 时间戳转时间格式
     * 传入指定毫秒数，返回对应的时间格式
     * @param stamp 该毫秒数是按照从格林尼治时间1970年1月1日0时0分0秒0毫秒以来来计算的
     * @param formatType 时间格式
     * @return 返回格式如：2018-04-12 13:01:50
     */
    public static String getStrByStamp(Long stamp, String formatType){
        if(stamp == null){
            return "";
        }
        // 2026-08-13 拆连写 stamp.toString().length() 为逐步局部变量
        String stampStr = stamp.toString();
        int length = stampStr.length();
        if(length == 10){
            String str = stampStr + "000";
            stamp = Long.parseLong(str);
            stampStr = stamp.toString();
            length = stampStr.length();
        }
        if(length != 13){
            LogUtil.error("TimerUtil_getStampByStamp_99", "时间戳位数错误", stamp, length, formatType);
            return "";
        }
        SimpleDateFormat format = new SimpleDateFormat(formatType);
        String str = format.format(stamp);
        return str;
    }

    /**
     * 2025-09-22
     * @param stamp
     * @param formatType
     * @return
     */
    public static String getStrByStamp(String stamp, String formatType){
        Long stampL = Long.valueOf(stamp);
        String str = getStrByStamp(stampL, formatType);
        return str;
    }

    /**
     * 2018-04-12 1.1.0
     * 时间戳转时间格式
     * 传入指定毫秒数，返回对应的时间格式
     * @param stamp 该毫秒数是按照从格林尼治时间1970年1月1日0时0分0秒0毫秒以来来计算的
     * @return 返回格式如：2018-04-12 13:01:50
     */
    public static String getStrByStamp(String stamp){
        Long stampL = Long.valueOf(stamp);
        String str = getStrByStamp(stampL);
        return str;
    }

    /**
     * 2017-08-05 1.0.0
     * 传入指定毫秒数，返回对应的时间格式
     * @param stamp 该毫秒数是按照从格林尼治时间1970年1月1日0时0分0秒0毫秒以来来计算的
     * @return 2017-08-05
     */
    public static Date getDateByStamp(Long stamp){
        SimpleDateFormat format = new SimpleDateFormat(formatType_default);
        Long time = Long.valueOf(stamp.toString());
        String str = format.format(time);
        try{
            Date date = format.parse(str);
            return date;
        }catch(Throwable throwable){
            LogUtil.error("TimerUtil_getDateByStamp_1", throwable, stamp);
        }
        return null;
    }

    /**
     * 2020-05-30 6.0.0
     * @param sdate2 当前时间更早或相等返回true，否则返回false
     * 参数若为null，则表示是当前时间
     */
    public static boolean compareTo(String sdate2){
        boolean flag = compareTo(null, sdate2);
        return flag;
    }

    /**
     * 2017-05-27 1.0.0
     * 对比两个时间，参数1更早或相等返回true，否则返回false
     * 参数若为null，则表示是当前时间
     */
    public static boolean compareTo(String sdate1, String sdate2){
        Date date1;
        if(sdate1 == null){
            date1 = new Date();
        }else{
            date1 = str2time(sdate1);
        }
        if(StringUtil.isBlank(sdate2)){
            return true;
        }
        Date date2 = str2time(sdate2);
        long time1 = date1.getTime();
        long time2 = date2.getTime();
        if(time1 <= time2){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 2017-05-27 1.0.0
     * 得到现在小时，格式如12
     */
    public static String getHour(){
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat(formatType_default);
        String dateString = format.format(date);
        String hour = dateString.substring(11, 13);
        return hour;
    }

    /**
     * 2017-05-27 1.0.0
     * 得到现在分钟，格式如11
     */
    public static String getMinute(){
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat(formatType_default);
        String dateString = format.format(date);
        String minute = dateString.substring(14, 16);
        return minute;
    }

    /**
     * 2017-05-27 1.0.0
     * 2个小时时间的差值,必须保证二个时间都是"HH:MM"的格式，返回分钟字符型
     */
    public static String getTwoHours(String st1, String st2){
        String[] kk = null;
        String[] jj = null;
        kk = StringUtil.split(st1, ":");
        jj = StringUtil.split(st2, ":");
        if(Integer.parseInt(kk[0]) < Integer.parseInt(jj[0])){
            return "0";
        }else{
            double y = Double.parseDouble(kk[0]) + Double.parseDouble(kk[1]) / 60;
            double u = Double.parseDouble(jj[0]) + Double.parseDouble(jj[1]) / 60;
            if((y - u) > 0){
                return y - u + "";
            }else{
                return "0";
            }
        }
    }

    /**
     * 2017-05-27 1.0.0
     * 得到2个日期间的间隔天数
     * @param startTime 开始时间，格式如2018-12-07
     * @param endTime 结束时间，格式如2018-12-07
     */
    public static String getTwoDays(String startTime, String endTime){
        SimpleDateFormat myFormatter = new SimpleDateFormat(FORMATTYPE4);
        long day = 0;
        try{
            Date date = myFormatter.parse(startTime);
            Date mydate = myFormatter.parse(endTime);
            day = (date.getTime() - mydate.getTime()) / (24 * 60 * 60 * 1000);
        }catch(Throwable throwable){
            return "";
        }
        return day + "";
    }

    /**
     * 2017-05-27 1.0.0
     * 两个时间之间的间隔时间（秒）
     * getTwoTimes("2017-02-02 02:02:01", "2017-02-02 02:02:02") ==> 1
     * @param datestr1 起始时间，字符串时间，格式如："2017-02-02 01:01:01"
     * @param datestr2 结束时间，字符串时间，格式如："2017-02-02 02:02:02"
     * @return (long) 间隔时间，秒
     */
    public static long getTwoTimes(String datestr1, String datestr2){
        if(datestr1 == null || datestr1.equals("")){
            return 0;
        }
        if(datestr2 == null || datestr2.equals("")){
            return 0;
        }
        //转换为标准时间
        SimpleDateFormat myFormatter = new SimpleDateFormat(formatType_default);
        Date date1 = null;
        Date date2 = null;
        try{
            date1 = myFormatter.parse(datestr1);
            date2 = myFormatter.parse(datestr2);
        }catch(Throwable throwable){
            LogUtil.error("TimerUtil_getTwoTimes_1", throwable, datestr1, datestr2);
        }
        long day = (date2.getTime() - date1.getTime()) / (1000);
        return day;
    }

    /**
     * 2020-08-19 6.5.0
     * 时间前推或后推分钟,其中JJ表示分钟
     * @param str 基准时间点（8中标准格式之一），格式如2018-12-07 18:01:53，不传则使用当前时间
     * @param amount 向前或向后的时间数值
     * @param unit 时间单位，从MILLISECOND到YEAR
     */
    public static String moveTime(String str, Integer amount, Integer unit){
        if(StringUtil.isBlank(str)){
            str = now(formatType_default);
        }
        Long amount2 = Long.valueOf(amount);
        String result = moveTime(str, amount2, unit);
        return result;
    }

    /**
     * 2017-05-27 1.0.0
     * 时间前推或后推分钟,其中JJ表示分钟
     * @param str 基准时间点（8中标准格式之一），格式如2018-12-07 18:01:53，不传则使用当前时间
     * @param amount 向前或向后的时间数值
     * @param unit 时间单位，从MILLISECOND到YEAR
     */
    public static String moveTime(String str, Long amount, Integer unit){
        if(StringUtil.isBlank(str)){
            str = time2str(formatType_default);
        }
        // 2026-08-14 系统与所有数据库表均统一时间格式
        // 缺陷修复：26/27 位微秒纳秒格式(如 2024-09-12 11:17:16.333000)无 getFormatType 分支，
        // 误用默认 M1 的 SSS 贪婪解析 6 位毫秒(333000ms=5分33秒)致偏移结果错(实测 +5秒 得 +5分38秒)；
        // 先 str2std 归一为 23 位标准格式(26/27 位截前 3 位毫秒，其他格式 str2std 原样返回零影响)再走既有解析
        str = str2std(str);
        String formatType = getFormatType(str);
        SimpleDateFormat format = new SimpleDateFormat(formatType);
        try{
            Date date = format.parse(str);
            long change = date.getTime();  //此数值的单位为毫秒
            if(unit == MILLISECOND){
                change += amount;
                date.setTime(change);
                String result = format.format(date);
                return result;
            }
            if(unit == SECOND){
                amount *= 1000L;
                change += amount;
                date.setTime(change);
                String result = format.format(date);
                return result;
            }
            if(unit == MINUTE){
                amount *= 60L;
                amount *= 1000L;
                change += amount;
                date.setTime(change);
                String result = format.format(date);
                return result;
            }
            if(unit == HOUR){
                amount *= 60L;
                amount *= 60L;
                amount *= 1000L;
                change += amount;
                date.setTime(change);
                String result = format.format(date);
                return result;
            }
            if(unit == DAY){
                amount *= 24L;
                amount *= 60L;
                amount *= 60L;
                amount *= 1000L;
                change += amount;
                date.setTime(change);
                String result = format.format(date);
                return result;
            }
            if(unit == WEEK){
                amount *= 7L;
                amount *= 24L;
                amount *= 60L;
                amount *= 60L;
                amount *= 1000L;
                change += amount;
                date.setTime(change);
                String result = format.format(date);
                return result;
            }
            if(unit == MONTH){
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formatType);
                Date dt = simpleDateFormat.parse(str);
                calendar.setTime(dt);
                int mount = amount.intValue();
                calendar.add(Calendar.MONTH, mount);
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;
                String smonth = StringUtil.suplyZero(month, 2);
                String suffix = str.substring(7, str.length());
                String result = year + "-" + smonth + suffix;
                return result;
            }
            if(unit == YEAR){
                String aa = str.substring(0, 4);
                int year = Integer.parseInt(aa);
                year += amount;
                String suffix = str.substring(4, str.length());
                String result = year + suffix;
                return result;
            }
            return null;
        }catch(Throwable throwable){
            LogUtil.error("TimerUtil_moveTime_1", throwable, str, amount, unit);
            return null;
        }
    }

    /**
     * 2017-05-27 1.0.0
     * （包含两边）两个日期之间的时间集合，返回格式如：{"2017-04-08", "2017-04-09", "2017-04-10", "2017-04-11"}
     * @param dateStart 开始时间，格式如：2017-04-08
     * @param dateEnd   结束时间，格式如：2017-04-11
     */
    public static List<String> getDateScope(String dateStart, String dateEnd){
        List<String> list = new ArrayList<>();
        String dateStartNew = convert1(dateStart);
        String dateEndNew = convert1(dateEnd);
        if(dateStartNew.equals(dateEndNew)){
            list.add(dateEnd);
        }else{
            list.add(dateStart);
            long i = 1;
            while(true){
                String dateStartNewPlus = moveTime(dateStartNew, i, DAY);
                if(dateEndNew.equals(dateStartNewPlus)){
                    list.add(convert2(dateStartNewPlus));
                    break;
                }else{
                    list.add(convert2(dateStartNewPlus));
                }
                i++;
            }
        }
        return list;
    }

    /**
     * 2017-04-10 1.0.0
     * 2017-04-10 ==> 2017-04-10
     */
    public static String convert1(String dateStr){
        dateStr = dateStr.substring(0, 4) + dateStr.substring(5, 7) + dateStr.substring(8, 10);
        return dateStr;
    }

    /**
     * 2017-04-10 1.0.0
     * 2017-04-10 ==> 2017-04-10
     */
    public static String convert2(String dateStr){
        dateStr = dateStr.substring(0, 4) + "-" + dateStr.substring(4, 6) + "-" + dateStr.substring(6, 8);
        return dateStr;
    }

    /**
     * 2017-05-27 1.0.0
     * 返回美国时间格式 26 Apr 2006
     */
    public static String getEDate(String str){
        SimpleDateFormat formatter = new SimpleDateFormat(FORMATTYPE4);
        ParsePosition pos = new ParsePosition(0);
        Date date = formatter.parse(str, pos);
        String j = date.toString();
        String[] k = StringUtil.split(j, " ");
        String sdate = k[2] + k[1].toUpperCase() + k[5].substring(2, 4);
        return sdate;
    }

    /**
     * 2017-05-27 1.0.0
     * 获取指定月份的最后一天的日期
     * @param sdate 格式如2018-01
     * 2018-01 ==> 2018-01-31
     */
    public static String getDateOfMonthEnd(String sdate){
        String str = sdate.substring(0, 7);
        String month = sdate.substring(5, 7);
        int mon = Integer.parseInt(month);
        if(mon == 1 || mon == 3 || mon == 5 || mon == 7 || mon == 8 || mon == 10 || mon == 12){
            str += "-31";
        }else if(mon == 4 || mon == 6 || mon == 9 || mon == 11){
            str += "-30";
        }else{
            if(isLeapYear(sdate)){
                str += "-29";
            }else{
                str += "-28";
            }
        }
        return str;
    }

    /**
     * 2017-05-27 1.0.0
     * 判断二个时间是否在同一个周
     */
    public static boolean isSameWeek(Date date1, Date date2){
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        int subYear = cal1.get(Calendar.YEAR) - cal2.get(Calendar.YEAR);
        if(0 == subYear){
            if(cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)){
                return true;
            }
        }else if(1 == subYear && 11 == cal2.get(Calendar.MONTH)){
            //若12月的最后一周横跨来年第一周的话则最后一周即算做来年的第一周
            if(cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)){
                return true;
            }
        }else if(-1 == subYear && 11 == cal1.get(Calendar.MONTH)){
            if(cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)){
                return true;
            }
        }
        return false;
    }

    /**
     * 2017-05-27 1.0.0
     * 产生周序列，即得到当前时间是其所在年份的第几周，从1开始
     * getSeqWeek() ==> 2018_50
     */
    public static String getSeqWeek(){
        Calendar c = Calendar.getInstance(Locale.CHINA);
        int week = c.get(Calendar.WEEK_OF_YEAR);
        String sweek = StringUtil.suplyZero(week, 2);
        int year = c.get(Calendar.YEAR);
        return year + "_" + sweek;
    }

    /**
     * 2017-05-27 1.0.0
     * 获得一个日期所在的周的星期几的日期，如要找出2002年2月3日所在周的星期一是几号
     * @param sdate 格式如2018-12-10
     * @param week 星期几，取值范围为1-7
     * getWeek("2018-12-10", "2") ==> 2018-12-11
     */
    public static String getWeek(String sdate, String week){
        Date date = str2time(sdate);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if("1".equals(week)){
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);  //返回星期一所在的日期
        }else if("2".equals(week)){
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);  //返回星期二所在的日期
        }else if("3".equals(week)){
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);  //返回星期三所在的日期
        }else if("4".equals(week)){
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);  //返回星期四所在的日期
        }else if("5".equals(week)){
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);  //返回星期五所在的日期
        }else if("6".equals(week)){
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);  //返回星期六所在的日期
        }else if("0".equals(week)){
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);  //返回星期日所在的日期
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FORMATTYPE4);
        date = calendar.getTime();
        String result = simpleDateFormat.format(date);
        return result;
    }

    /**
     * 2017-05-27 1.0.0
     * 根据一个日期，返回是星期几的字符串
     * @param sdate 格式如2018-12-10
     * getWeek("2018-12-10") ==> 星期一
     */
    public static String getWeek(String sdate){
        Date date = str2time(sdate);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE");
        date = calendar.getTime();
        String week = simpleDateFormat.format(date);
        return week;
    }

    /**
     * 2019-11-19 3.0.2
     * 今天是星期几
     */
    public static String getWeek(){
        String snow = now();
        String week = getWeek(snow);
        return week;
    }

    /**
     * 2017-05-27 1.0.0
     * 一个公历月有多少天
     * @param year 年份
     * @param month 月份
     * daysWithMonth(2018, 1) ==> 31
     */
    public static int daysWithMonth(Integer year, Integer month){
        int[] days_in_month = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        int manyDayNum = days_in_month[month - 1];
        if(isLeapYear(String.valueOf(year)) && month == 2){
            manyDayNum++;
        }
        return manyDayNum;
    }

    /**
     * 2017-05-27 1.0.0
     * 是否为闰年，保证前四个字符为正常年份即可
     * @param sdate 只要保证前4位字符格式如2018即可
     * isLeapYear("2018") ==> false
     */
    public static boolean isLeapYear(String sdate){
        int year = Integer.parseInt(sdate.substring(0, 4));
        if((year % 4 == 0 && year % 100 != 0) || year % 400 == 0){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 2017-05-27 1.0.0
     * 指定月份在日历表中的第1行星期日的日期
     * @param sdate 只要保证前7位字符格式如2018-12即可
     * getNowMonth("2018-12") ==> 2018-11-25
     */
    public static String getNowMonth(String sdate){
        sdate = sdate.substring(0, 7);
        sdate = sdate + "-01";  //取该时间所在月的一号
        Date date = str2time(sdate);  //得到这个月的1号是星期几
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int u = c.get(Calendar.DAY_OF_WEEK);
        Long a = Long.valueOf(1 - u);
        String newday = moveTime(sdate, a, DAY);
        return newday;
    }

    /**
     * 2017-05-27 1.0.0
     * 根据字符串格式自动判断ormatType类型
     * @param str 具体时间，格式为标准0-7的其中之一
     * getFormatType("2018-12-10") ==> yyyy-MM-dd
     */
    public static final String getFormatType(String str){
        if(StringUtil.isBlank(str)){
            return null;
        }
        str = StringUtil.getPureStr(str);
        str = StringUtil.trim(str);
        int kongge = StringUtil.getCount(str, " ");
        int henggang = StringUtil.getCount(str, "-");
        int maohao = StringUtil.getCount(str, ":");
        int length = str.length();
        if(kongge == 1){
            if(henggang == 2){
                if(maohao == 2 && length == 19){
                    return FORMATTYPE1;
                }
                if(maohao == 2 && length == 23){
                    return FORMATTYPE_M1;
                }
                if(maohao == 2 && (length == 21 || length == 22)){
                    return formatType_default;
                }
                if(maohao == 3 && (length == 21 || length == 22)){
                    return FORMATTYPE_M2;
                }
                // 2026-08-14 系统与所有数据库表均统一时间格式
                // 缺陷修复：FORMATTYPE_M2 本身是 23 位(yyyy-MM-dd HH:mm:ss:SSS，冒号3)，但上方分支只识别 21/22 位(毫秒1~2位)漏了 23 位完整形，
                // 导致 23 位冒号毫秒串(如 2025-06-12 09:38:09:476)被误判为默认 M1 点号格式，moveTime 用点号 pattern 解析冒号串必然 Unparseable date(实测 p_time moveTime 用例报错)
                if(maohao == 3 && length == 23){
                    return FORMATTYPE_M2;
                }
                return formatType_default;
            }
            if(henggang == 0 && maohao == 0 && length == 15){
                return FORMATTYPE2;
            }
            return formatType_default;
        }
        if(kongge == 0){
            if(henggang == 0){
                if(maohao == 0){
                    if(length == 8){
                        return FORMATTYPE5;
                    }
                    if(length == 11 && StringUtil.containOfAll(str, "年", "月", "日")){
                        return FORMATTYPE9;
                    }
                    if(length == 14){
                        return FORMATTYPE3;
                    }
                    if(length == 23){
                        return FORMATTYPE_M3;
                    }
                    return formatType_default;
                }
                if(maohao == 1 && length == 5){
                    return FORMATTYPE11;
                }
                if(maohao == 2 && length == 8){
                    return FORMATTYPE6;
                }
                if(maohao == 3 && (length == 10 || length == 11 || length == 12)){
                    return FORMATTYPE7;
                }
                return formatType_default;
            }
            if(henggang == 2){
                if(maohao == 0 && length == 10){
                    return FORMATTYPE4;
                }
                return formatType_default;
            }
            return formatType_default;
        }
        return formatType_default;
    }

    /**
     * 2017-05-27 1.0.0
     * 如：3600000毫秒转为小时格式(h) => 1:0:0.000
     * @param type 转换后的时间格式，分为：Y, M, d, h, m, s, x
     * @param num  原时间段，单位为毫秒
     */
    public static String timeQuantum(String type, long num){
        String result = "";
        if("Y".equals(type)){  //Y-M-d_h:m:s.xxx

        }else if("M".equals(type)){  //M-d h:m:s.xxx

        }else if("d".equals(type)){  //86400000, d h:m:s.xxx
            if(num >= 86400000){
                result = String.valueOf(num);
                String aaa = result.substring(0, result.length() - 3);  //s
                long bbb = Long.valueOf(aaa) / 86400;  //d
                long ccc = num - bbb * 86400000;
                result = bbb + "_" + timeQuantum("h", ccc);
            }else{
                result = "0_" + timeQuantum("m", num);
            }
        }else if("h".equals(type)){  //3600000, h:m:s.xxx
            if(num >= 3600000){
                result = String.valueOf(num);
                String aaa = result.substring(0, result.length() - 3);  //s
                long bbb = Long.valueOf(aaa) / 3600;  //h
                long ccc = num - bbb * 3600000;
                result = bbb + ":" + timeQuantum("m", ccc);
            }else{
                result = "0:" + timeQuantum("m", num);
            }
        }else if("m".equals(type)){  //60000, m:s.xxx
            if(num >= 60000){
                result = String.valueOf(num);
                String aaa = result.substring(0, result.length() - 3);  //s
                long bbb = Long.valueOf(aaa) / 60;  //m
                long ccc = num - bbb * 60000;
                result = bbb + ":" + timeQuantum("s", ccc);
            }else{
                result = "0:" + timeQuantum("s", num);
            }
        }else if("s".equals(type)){  //1000, s.xxx
            if(num >= 1000){
                result = String.valueOf(num);
                String aaa = result.substring(0, result.length() - 3);
                String bbb = result.substring(result.length() - 3, result.length());
                result = aaa + "." + bbb;
            }else{
                result = String.valueOf(num);
                if(result.length() == 1){
                    result = "0" + result;
                }else if(result.length() == 2){
                    result = "0" + result;
                }
                result = "0." + result;
            }

        }else if("x".equals(type)){  //1
            result = String.valueOf(num);
        }
        return result;
    }

    /**
     * 2017-05-27 1.0.0
     * @param type
     * @param date
     * @param amount
     */
    public static Date setCalendar(String type, Date date, Integer amount){
        if(date == null){
            LogUtil.error("TimerUtil_setCalendar_3", "日期对象不能为空", type, date, amount);
            return null;
        }
        int calendarField = -100;
        if("year".equals(type)){
            calendarField = Calendar.YEAR;
        }else if("month".equals(type)){
            calendarField = Calendar.MONTH;
        }else if("day".equals(type)){
            calendarField = Calendar.DAY_OF_MONTH;
        }else if("hour".equals(type)){
            calendarField = Calendar.HOUR_OF_DAY;
        }else if("minute".equals(type)){
            calendarField = Calendar.MINUTE;
        }else if("second".equals(type)){
            calendarField = Calendar.SECOND;
        }else if("millisecond".equals(type)){
            calendarField = Calendar.MILLISECOND;
        }else{
            LogUtil.error("TimerUtil_setCalendar_4", "TimerUtil_calendarField值不能为-100", type, date, amount);
            return null;
        }
        // getInstance() returns a new object, so this method is thread safe.
        Calendar calendar = Calendar.getInstance();
        calendar.setLenient(false);
        calendar.setTime(date);
        calendar.set(calendarField, amount);
        Date result = calendar.getTime();
        return result;
    }

    // ======================================================================
    // 以下为 DateUtil 合并进来的方法（2026-08-13 合并 DateUtil/TimeZoneUtil 到 TimerUtil）
    // 原 DateUtil.now() 返回 Date，与 TimerUtil.now() 返回 String 冲突，重命名为 nowDate()
    // ======================================================================

    // DateUtil 的格式化常量
    public static final String DATE_FORMAT_FULL = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT_TIME = "HH:mm:ss";
    public static final String DATE_FORMAT_MON = "yyyy-MM";
    public static final String DATE_FORMAT_YMD = "yyyy-MM-dd";
    public static final String DATE_FORMAT_DATETIME_NO_SECOND = "yyyy-MM-dd HH:mm";
    public static final String DATE_FORMAT_TIME_NO_SECOND = "HH:mm";
    public static final String DATE_FORMAT_TIMESTAMP = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * 2026-08-13 合并自 DateUtil
     * 按指定 pattern 创建 DateFormat
     * @param pattern 格式串
     * @return DateFormat 对象
     */
    private static final DateFormat p_createFormat(final String pattern) {
        DateFormat dateFormat = new SimpleDateFormat(pattern);
        return dateFormat;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 获取短日期格式(yyyy-MM-dd)的 DateFormat
     * @return DateFormat 对象
     */
    public static final DateFormat getDateFormat() {
        DateFormat dateFormat = p_createFormat(DATE_FORMAT_YMD);
        return dateFormat;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 获取完整日期时间格式(yyyy-MM-dd HH:mm:ss)的 DateFormat
     * @return DateFormat 对象
     */
    public static final DateFormat getDateTimeFormat() {
        DateFormat dateFormat = p_createFormat(DATE_FORMAT_FULL);
        return dateFormat;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 获取无秒日期时间格式(yyyy-MM-dd HH:mm)的 DateFormat
     * @return DateFormat 对象
     */
    public static final DateFormat getDateTimeNoSecondFormat() {
        DateFormat dateFormat = p_createFormat(DATE_FORMAT_DATETIME_NO_SECOND);
        return dateFormat;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 获取时间格式(HH:mm:ss)的 DateFormat
     * @return DateFormat 对象
     */
    public static final DateFormat getTimeFormat() {
        DateFormat dateFormat = p_createFormat(DATE_FORMAT_TIME);
        return dateFormat;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 获取无秒时间格式(HH:mm)的 DateFormat
     * @return DateFormat 对象
     */
    public static final DateFormat getTimeNoSecondFormat() {
        DateFormat dateFormat = p_createFormat(DATE_FORMAT_TIME_NO_SECOND);
        return dateFormat;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 获取时间戳格式(yyyy-MM-dd HH:mm:ss.SSS)的 DateFormat
     * @return DateFormat 对象
     */
    public static final DateFormat getTimestampFormat() {
        DateFormat dateFormat = p_createFormat(DATE_FORMAT_TIMESTAMP);
        return dateFormat;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 解析日期字符串，自动识别 Timestamp/Time/sql.Date 类型
     * @param dateString 日期字符串
     * @return Date 对象，解析失败返回 null
     */
    public static final Date parse(final String dateString) {
        try {
            String trimmed = dateString.trim();
            if ((trimmed.indexOf(" ") > 0) && (trimmed.indexOf(".") > 0)) {
                Date parsed = getTimestampFormat().parse(dateString);
                Date result = new Timestamp(parsed.getTime());
                return result;
            }
            if (trimmed.indexOf(" ") > 0) {
                if (trimmed.indexOf(":") != trimmed.lastIndexOf(":")) {
                    Date parsed = getDateTimeFormat().parse(dateString);
                    Date result = new Timestamp(parsed.getTime());
                    return result;
                }
                Date parsed = getDateTimeNoSecondFormat().parse(dateString);
                Date result = new Timestamp(parsed.getTime());
                return result;
            }
            if (trimmed.indexOf(":") > 0) {
                if (trimmed.indexOf(":") != trimmed.lastIndexOf(":")) {
                    Date parsed = getTimeFormat().parse(dateString);
                    Date result = new Time(parsed.getTime());
                    return result;
                }
                Date parsed = getTimeNoSecondFormat().parse(dateString);
                Date result = new Time(parsed.getTime());
                return result;
            }
            Date parsed = getDateFormat().parse(dateString);
            Date result = new java.sql.Date(parsed.getTime());
            return result;
        } catch (Throwable throwable) {
            LogUtil.error("TimerUtil_parse_t1", throwable);
            return null;
        }
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 格式化 Date 为字符串，按实际类型(Timestamp/Time/sql.Date/Date)选格式
     * @param date 日期对象
     * @return 格式化后的字符串
     */
    public static final String format(final Date date) {
        if ((date instanceof Timestamp)) {
            String result = getTimestampFormat().format(date);
            return result;
        }
        if ((date instanceof Time)) {
            String result = getTimeFormat().format(date);
            return result;
        }
        if ((date instanceof java.sql.Date)) {
            String result = getDateFormat().format(date);
            return result;
        }
        String result = getDateTimeFormat().format(date);
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 按指定 style 格式化 Date
     * @param date 日期对象
     * @param style 格式串
     * @return 格式化后的字符串
     */
    public static final String format(final Date date, final String style) {
        DateFormat dateFormat = p_createFormat(style);
        String result = dateFormat.format(date);
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 解析日期字符串的时间部分
     * @param dateString 日期字符串
     * @return Date 对象
     */
    public static final Date formatParseTime(final String dateString) {
        try {
            String trimmed = dateString.trim();
            if (trimmed.indexOf(" ") > 0) {
                String[] d = trimmed.split(" ");
                if (trimmed.indexOf(":") != trimmed.lastIndexOf(":")) {
                    Date parsed = getTimeFormat().parse(d[1]);
                    Date result = new Timestamp(parsed.getTime());
                    return result;
                }
                Date parsed = getTimeNoSecondFormat().parse(d[1]);
                Date result = new Timestamp(parsed.getTime());
                return result;
            }
            if (trimmed.indexOf(":") > 0) {
                if (trimmed.indexOf(":") != trimmed.lastIndexOf(":")) {
                    Date parsed = getTimeFormat().parse(dateString);
                    Date result = new Time(parsed.getTime());
                    return result;
                }
                Date parsed = getTimeNoSecondFormat().parse(dateString);
                Date result = new Time(parsed.getTime());
                return result;
            }
            Date parsed = getDateTimeFormat().parse(dateString);
            Date result = new java.sql.Date(parsed.getTime());
            return result;
        } catch (Throwable throwable) {
            LogUtil.error("TimerUtil_formatParseTime_t1", throwable);
            return null;
        }
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 按 yyyy-MM-dd 解析日期字符串
     * @param dateString 日期字符串
     * @return Date 对象
     */
    public static final Date formatParseDate(final String dateString) {
        try {
            DateFormat dateFormat = getDateFormat();
            Date result = dateFormat.parse(dateString);
            return result;
        } catch (Throwable throwable) {
            LogUtil.error("TimerUtil_formatParseDate_t1", throwable);
            return null;
        }
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 按指定 style 格式化当前时间
     * @param style 格式串，空则用 DATE_FORMAT_FULL
     * @return 格式化后的字符串
     */
    public static final String getNowByString(final String style) {
        String effectiveStyle = style;
        if ((null == effectiveStyle) || ("".equals(effectiveStyle))) {
            effectiveStyle = DATE_FORMAT_FULL;
        }
        String result = format(new Date(), effectiveStyle);
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 取日期字符串的日期部分(空格或T前)
     * @param dateString 日期字符串
     * @return 日期部分字符串
     */
    public static final String stringDate(final String dateString) {
        String date = "";
        if (dateString.indexOf("T") == -1) {
            String[] s = dateString.split(" ");
            date = s[0];
        } else {
            String[] s = dateString.split("T");
            date = s[0];
        }
        return date;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 按指定 format 格式化 Date，format 为空则用 DATE_FORMAT_FULL
     * @param date 日期对象
     * @param format 格式串
     * @return 格式化后的字符串
     */
    public static final String formatDate(final Date date, final String format) {
        String effectiveFormat = format;
        if (CollectionUtil.isBlank(effectiveFormat)) {
            effectiveFormat = DATE_FORMAT_FULL;
        }
        DateFormat dateFormat = p_createFormat(effectiveFormat);
        String result = dateFormat.format(date);
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 按 DATE_FORMAT_FULL 格式化 Date
     * @param date 日期对象
     * @return 格式化后的字符串
     */
    public static final String formatDate(final Date date) {
        String result = formatDate(date, "");
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 设置指定时间为当天的最初时间（00时00分00秒）
     * @param date 日期对象
     * @return 设置后的 Date
     */
    public static final Date setStartDay(final Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date result = cal.getTime();
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 设置指定时间为当天的结束时间（23时59分59秒）
     * @param date 日期对象
     * @return 设置后的 Date
     */
    public static final Date setEndDay(final Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date result = cal.getTime();
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 获得当前日期是当年的第几周
     * @return 周数
     */
    public static final int getCurWeekOfYear() {
        Calendar cal = Calendar.getInstance();
        int week = cal.get(Calendar.WEEK_OF_YEAR);
        return week;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 获得指定日期是当年的第几周
     * @param time 日期对象
     * @return 周数
     */
    public static final int getWeekOfYear(final Date time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        int week = cal.get(Calendar.WEEK_OF_YEAR);
        return week;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 获得当前日期中的月中的日号
     * @return 日号
     */
    public static final int getCurDay() {
        int day = getDay();
        return day;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 返回当前时间的日号
     * @return 日号
     */
    public static final int getDay() {
        Date date = new Date();
        int day = getDay(date);
        return day;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 获得指定日期的日号
     * @param date 日期对象
     * @return 日号
     */
    public static final int getDay(final Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return day;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 获得当前日期的月份
     * @return 月份(1-12)
     */
    public static final int getCurMonth() {
        int month = getMonth();
        return month;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 返回当前时间的月份
     * @return 月份(1-12)
     */
    public static final int getMonth() {
        Date date = new Date();
        int month = getMonth(date);
        month++;
        return month;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 获得指定日期的月份(0-11，Calendar 原始值)
     * @param date 日期对象
     * @return 月份(0-11)
     */
    public static final int getMonth(final Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH);
        return month;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 获得当前日期的年份
     * @return 年份
     */
    public static final int getCurYear() {
        int year = getYear();
        return year;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 返回当前时间的年份
     * @return 年份
     */
    public static final int getYear() {
        Date date = new Date();
        int year = getYear(date);
        return year;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 获得指定日期的年份
     * @param date 日期对象
     * @return 年份
     */
    public static final int getYear(final Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        return year;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 将UNIX时间戳(秒)转成指定格式的字符串
     * @param timestampString 秒级时间戳字符串
     * @param formats 格式串，空则用 DATE_FORMAT_FULL
     * @return 格式化后的字符串
     */
    public static final String timeStamp2Date(final String timestampString, final String formats) {
        String effectiveFormats = formats;
        if (CollectionUtil.isBlank(effectiveFormats)) {
            effectiveFormats = DATE_FORMAT_FULL;
        }
        Long timestamp = Long.parseLong(timestampString) * 1000;
        String date = new SimpleDateFormat(effectiveFormats, Locale.CHINA).format(new Date(timestamp));
        return date;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 将UNIX时间戳(秒)转成 Date
     * @param timestampString 秒级时间戳字符串
     * @return Date 对象
     */
    public static final Date timeStamp2Date(final String timestampString) {
        Long timestamp = Long.parseLong(timestampString) * 1000;
        Date date = new Date(timestamp);
        return date;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 给指定日期增加/减少天数
     * @param date 日期对象
     * @param day 天数(正为加，负为减)
     * @return 计算后的 Date
     */
    public static final Date addDay(final Date date, final int day) {
        Calendar ca = Calendar.getInstance();
        ca.setTime(date);
        ca.add(Calendar.DAY_OF_MONTH, day);
        Date result = ca.getTime();
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 判断 date 是否在 startTime 和 endTime 之间(不含边界)
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param date 待判断的时间
     * @return 在区间内返回 true
     */
    public static final boolean isBetween(final Date startTime, final Date endTime, final Date date) {
        if ((date.after(startTime)) && (date.before(endTime))) {
            return true;
        }
        return false;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 将 time 的年月日替换为 date 的年月日
     * @param date 提供年月日的日期
     * @param time 提供时分秒的日期
     * @return 合成后的 Date
     */
    public static final Date getTime(final Date date, final Date time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        Calendar ca = Calendar.getInstance();
        ca.setTime(time);
        ca.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        Date result = ca.getTime();
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 获得指定日期是星期几(中文)
     * @param date 日期对象
     * @return 星期天/星期一/.../星期六
     */
    public static final String getWeekOfDate(final Date date) {
        String[] p_weekDays = {"星期天", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0) {
            w = 0;
        }
        String result = p_weekDays[w];
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 计算两个时间之间的分钟差(含小数)
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分钟差
     */
    public static final double betweenMinute(final Date startTime, final Date endTime) {
        Long seconds = Long.valueOf((endTime.getTime() - startTime.getTime()) / 1000L);
        double s = seconds.longValue() % 60.0D / 60.0D;
        DecimalFormat df = new DecimalFormat("0.00");
        double sec = Double.parseDouble(df.format(s));
        Long minute = Long.valueOf(seconds.longValue() / 60L);
        double result = minute.longValue() + sec;
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 计算两个时间之间的小时差(含小数)
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 小时差
     */
    public static final double betweenHour(final Date startTime, final Date endTime) {
        Long minutes = Long.valueOf((endTime.getTime() - startTime.getTime()) / 1000L / 60L);
        double m = minutes.longValue() % 60.0D / 60.0D;
        DecimalFormat df = new DecimalFormat("0.00");
        double min = Double.parseDouble(df.format(m));
        Long hour = Long.valueOf(minutes.longValue() / 60L);
        double result = hour.longValue() + min;
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 计算两个时间之间的小时差(扣除休息分钟数)
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param restMinutes 休息分钟数
     * @return 小时差
     */
    public static final double betweenHour(final Date startTime, final Date endTime, final Double restMinutes) {
        Long minutes = Long.valueOf((endTime.getTime() - startTime.getTime()) / 1000L / 60L);
        Long ms = Long.valueOf((long) (minutes.longValue() - restMinutes.doubleValue()));
        double m = ms.longValue() % 60.0D / 60.0D;
        DecimalFormat df = new DecimalFormat("0.00");
        double min = Double.parseDouble(df.format(m));
        Long hour = Long.valueOf(ms.longValue() / 60L);
        double result = hour.longValue() + min;
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 获取两个日期之间的所有日期字符串数组(含起止)
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日期字符串数组
     */
    public static final String[] getDaysBetweenDate(final Date startTime, final Date endTime) {
        String[] dateArr = null;
        try {
            Integer day = Integer.valueOf(daysBetween(startTime, endTime));
            dateArr = new String[day.intValue() + 1];
            for (int i = 0; i < dateArr.length; i++) {
                if (i == 0) {
                    dateArr[i] = formatDate(startTime);
                } else {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(startTime);
                    calendar.add(Calendar.DAY_OF_MONTH, i);
                    Date calendarDate = calendar.getTime();
                    dateArr[i] = formatDate(calendarDate);
                }
            }
        } catch (Throwable throwable) {
            LogUtil.error("TimerUtil_getDaysBetweenDate_t1", throwable);
        }
        return dateArr;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 获取指定日期推移 field/amount 后的 Date
     * @param date 基准日期
     * @param field Calendar 字段(如 Calendar.HOUR_OF_DAY)
     * @param amount 推移量
     * @return 推移后的 Date
     */
    public static final Date getNextTime(final Date date, final int field, final int amount) {
        Calendar ca = Calendar.getInstance();
        ca.setTime(date);
        ca.add(field, amount);
        Date result = ca.getTime();
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 给指定日期增加/减少小时
     * @param date 日期对象
     * @param hour 小时数
     * @return 计算后的 Date
     */
    public static final Date addHour(final Date date, final int hour) {
        Date result = getNextTime(date, Calendar.HOUR_OF_DAY, hour);
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 给指定日期增加/减少小时(含分钟秒小数)
     * @param date 日期对象
     * @param time 小时数(含小数)
     * @return 计算后的 Date
     */
    public static final Date addHour(final Date date, final double time) {
        int hour = (int) time;
        double minute1 = (time - hour) * 60.0D;
        int minute = (int) minute1;
        int second = (int) ((minute1 - minute) * 60.0D);
        Date dateHour = getNextTime(date, Calendar.HOUR_OF_DAY, hour);
        Date dateMinute = getNextTime(dateHour, Calendar.MINUTE, minute);
        Date result = getNextTime(dateMinute, Calendar.SECOND, second);
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 给指定日期增加/减少分钟
     * @param date 日期对象
     * @param minute 分钟数
     * @return 计算后的 Date
     */
    public static final Date addMinute(final Date date, final int minute) {
        Date result = getNextTime(date, Calendar.MINUTE, minute);
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 计算两个日期之间相差的天数
     * @param smdate 较小的时间
     * @param bdate 较大的时间
     * @return 相差天数
     */
    public static final int daysBetween(final Date smdate, final Date bdate) {
        long days = (bdate.getTime() - smdate.getTime()) / (1000 * 3600 * 24);
        int result = (int) days;
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 返回当前日期时间的字符串(yyyy-MM-dd HH:mm:ss)
     * @return 格式化后的字符串
     */
    public static final String currentDatetime() {
        DateFormat datetimeFormat = p_createFormat(DATE_FORMAT_FULL);
        String result = datetimeFormat.format(nowDate());
        return result;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 返回当前时间的 Date 对象(原 DateUtil.now，因与 TimerUtil.now 返回String冲突，重命名为 nowDate)
     * @return 当前时间 Date 对象
     */
    public static final Date nowDate() {
        Date date = new Date();
        return date;
    }

    /**
     * 2026-08-13 合并自 DateUtil
     * 按指定 pattern 解析日期字符串
     * @param datetime 日期字符串
     * @param pattern 格式串
     * @return Date 对象，解析失败返回 null
     */
    public static final Date parseDatetime(final String datetime, final String pattern) {
        try {
            DateFormat format = p_createFormat(pattern);
            Date result = format.parse(datetime);
            return result;
        } catch (Throwable throwable) {
            LogUtil.error("TimerUtil_parseDatetime_t2", throwable);
            return null;
        }
    }

    // ======================================================================
    // 以下为 TimeZoneUtil 合并进来的方法（2026-08-13）
    // ======================================================================

    /**
     * 2023-10-09 合并自 TimeZoneUtil
     * 设置系统默认时区为上海时区，即东8区
     */
    public static final void setDefaultTimeZone(){
        setDefaultTimeZone("GMT+8");
    }

    /**
     * 2023-10-09 合并自 TimeZoneUtil
     * 设置系统默认时区
     * @param id 时区id，如 GMT+8
     */
    public static final void setDefaultTimeZone(final String id){
        TimeZone timeZone = TimeZone.getTimeZone(id);
        TimeZone.setDefault(timeZone);
    }

    /**
     * 2023-10-09 合并自 TimeZoneUtil
     * 返回所有时区id，如 Asia/Shanghai、America/Chicago、GMT+8 等
     * @return 时区id集合
     */
    public static final Set<String> getAvailableZoneIds(){
        Set<String> set = ZoneId.getAvailableZoneIds();
        return set;
    }

    /**
     * 2023-10-09 合并自 TimeZoneUtil
     * 获取系统当前时区，如 Asia/Shanghai、America/Chicago、GMT+8 等
     * @return 时区id字符串
     */
    public static final String getCurrentTimeZoneId(){
        ZoneId zoneId = ZoneId.systemDefault();
        String id = zoneId.getId();
        return id;
    }

    /**
     * 2026-08-13 
     * 兼容旧版 DateUtil.parseDate 方法名（08版 DateUtil 有此方法，21版合并时名为 formatParseDate）
     * 转发到 formatParseDate，保证向后兼容
     * @param dateString 日期字符串
     * @return Date 对象
     */
    public static final Date parseDate(final String dateString) {
        Date date = formatParseDate(dateString);
        return date;
    }

    /**
     * 2026-08-13 
     * 兼容旧版 TimerUtil.date2str 方法名（旧版本用此名，新版已重命名为 time2str）
     * 转发到 time2str，保证向后兼容
     * @param formatType 格式串
     * @return 当前时间格式化字符串
     */
    public static final String date2str(final String formatType) {
        String result = time2str(formatType);
        return result;
    }

    /**
     * 2026-08-13 
     * 兼容旧版 TimerUtil.date2str() 无参版（旧版本签名）
     * 转发到 time2str()（默认格式）
     * @return 当前时间格式化字符串
     */
    public static final String date2str() {
        String result = time2str();
        return result;
    }

    /**
     * 2026-08-13 
     * 兼容旧版 TimerUtil.date2str(Date) 方法名
     * 转发到 time2str，保证向后兼容
     * @param date 日期对象
     * @return 格式化字符串
     */
    public static final String date2str(final Date date) {
        String result = time2str(date);
        return result;
    }

    /**
     * 2026-08-13 
     * 兼容旧版 TimerUtil.date2str(Date, String) 双参数版（旧版本签名）
     * 转发到 time2str(Date, String)
     * @param date 日期对象
     * @param formatType 格式串
     * @return 格式化字符串
     */
    public static final String date2str(final Date date, final String formatType) {
        String result = time2str(date, formatType);
        return result;
    }

    /**
     * 2026-08-13 
     * 兼容旧版 TimerUtil.str2time(String, String) 双参数版（08版有此签名，21版重构为单参数）
     * 先用 str2str 按指定格式解析，再用 str2time 转为 Date
     * @param fromTimeStr 时间字符串
     * @param fromFormatType 解析格式
     * @return Date 对象
     */
    public static final Date str2time(final String fromTimeStr, final String fromFormatType) {
        // 2026-08-13 用 str2std 归一化(兼容各种格式)再 str2time 解析
        String stdTimeStr = str2std(fromTimeStr);
        Date date = str2time(stdTimeStr);
        return date;
    }

    /**
     * 2026-08-13 
     * 兼容旧版 DateUtil.parseDate(String, String) 双参数版
     * 转发到 parseDatetime（按指定 pattern 解析）
     * @param datetime 日期字符串
     * @param pattern 格式串
     * @return Date 对象
     */
    public static final Date parseDate(final String datetime, final String pattern) {
        Date date = parseDatetime(datetime, pattern);
        return date;
    }

    /**
     * 2026-08-13 
     * 兼容旧版 TimerUtil.str2date(String) 方法名（旧版本方法名）
     * 转发到 str2time（字符串转Date）
     * @param fromTimeStr 时间字符串
     * @return Date 对象
     */
    public static final Date str2date(final String fromTimeStr) {
        Date date = str2time(fromTimeStr);
        return date;
    }

    /**
     * 2026-08-13
     * @param date 日期对象
     * @return 格式化字符串
     */
    public static final String formaDatetTime(final Date date) {
        String result = time2str(date);
        return result;
    }

    /**
     * 2026-08-13 
     * 兼容旧版 TimerUtil.getDisplayTime(Integer) 方法名（08版用此名，21版叫 getTimeValue）
     * 转发到 getTimeValue，把秒数转为 HH:mm:ss 格式
     * @param second 秒数
     * @return 格式化时间字符串
     */
    public static final String getDisplayTime(final Integer second) {
        long secondLong = second.longValue();
        String result = getTimeValue(secondLong);
        return result;
    }

    /**
     * 2026-08-13 
     * 兼容旧版 DateUtil.parse(String, String) 双参数版
     * 按指定 pattern 解析日期字符串
     * @param dateString 日期字符串
     * @param pattern 格式串
     * @return Date 对象
     */
    public static final Date parse(final String dateString, final String pattern) {
        Date date = parseDatetime(dateString, pattern);
        return date;
    }

}
