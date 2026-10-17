package xc.crabapple.core.timer;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

/**
 * 2016-12-10 1.0.0
 */
public class SnowTask extends TimerTask{

    private String name = null;  //名称
    private Date dateStart = null;  //任务实体生成时间
    private Long exeAcount = 0L;  //已执行次数
    private Long exeAcountMax = 0L;  //最大执行次数，0表示无限次
    private Long delay = null;  //<1>互斥，优先级高，间隔时间，秒
    private String points = null;  //<1>互斥，优先级低，执行时间点列表，null表示无效，用英文逗号隔开标准格式****-**-** 04:00:00,2020-12-10 09:17:12,...
    private Boolean status = true;  //状态，是否在循环列表中，true表示在，false表示不在
    //
    private List<Map<String, Object>> p_pointList = null;

    public SnowTask(final String name){
        //
        this.name = name;
    }

    /**
     * 2017-03-24 1.0.0
     */
    @Override
    public void run(){
        //间隔时间
        if(delay != null){
            executeI();
            return;
        }
        //时间点
        String sdnow = TimerUtil.now(TimerUtil.FORMATTYPE1);
        String ny1 = sdnow.substring(0, 1);
        String ny2 = sdnow.substring(1, 2);
        String ny3 = sdnow.substring(2, 3);
        String ny4 = sdnow.substring(3, 4);
        String nM1 = sdnow.substring(5, 6);
        String nM2 = sdnow.substring(6, 7);
        String nd1 = sdnow.substring(8, 9);
        String nd2 = sdnow.substring(9, 10);
        String nh1 = sdnow.substring(11, 12);
        String nh2 = sdnow.substring(12, 13);
        String nm1 = sdnow.substring(14, 15);
        String nm2 = sdnow.substring(15, 16);
        String ns1 = sdnow.substring(17, 18);
        String ns2 = sdnow.substring(18, 19);
        //
        for(Map<String, Object> map : p_pointList){
            String year1 = (String) map.get("year1");
            String year2 = (String) map.get("year2");
            String year3 = (String) map.get("year3");
            String year4 = (String) map.get("year4");
            String month1 = (String) map.get("month1");
            String month2 = (String) map.get("month2");
            String day1 = (String) map.get("day1");
            String day2 = (String) map.get("day2");
            String hour1 = (String) map.get("hour1");
            String hour2 = (String) map.get("hour2");
            String minute1 = (String) map.get("minute1");
            String minute2 = (String) map.get("minute2");
            String second1 = (String) map.get("second1");
            String second2 = (String) map.get("second2");
            Boolean fyear1 = (Boolean) map.get("fyear1");
            Boolean fyear2 = (Boolean) map.get("fyear2");
            Boolean fyear3 = (Boolean) map.get("fyear3");
            Boolean fyear4 = (Boolean) map.get("fyear4");
            Boolean fmonth1 = (Boolean) map.get("fmonth1");
            Boolean fmonth2 = (Boolean) map.get("fmonth2");
            Boolean fday1 = (Boolean) map.get("fday1");
            Boolean fday2 = (Boolean) map.get("fday2");
            Boolean fhour1 = (Boolean) map.get("fhour1");
            Boolean fhour2 = (Boolean) map.get("fhour2");
            Boolean fminute1 = (Boolean) map.get("fminute1");
            Boolean fminute2 = (Boolean) map.get("fminute2");
            Boolean fsecond1 = (Boolean) map.get("fsecond1");
            Boolean fsecond2 = (Boolean) map.get("fsecond2");
            Boolean f1 = fyear1 || ny1.equals(year1);
            Boolean f2 = fyear2 || ny2.equals(year2);
            Boolean f3 = fyear3 || ny3.equals(year3);
            Boolean f4 = fyear4 || ny4.equals(year4);
            Boolean f5 = fmonth1 || nM1.equals(month1);
            Boolean f6 = fmonth2 || nM2.equals(month2);
            Boolean f7 = fday1 || nd1.equals(day1);
            Boolean f8 = fday2 || nd2.equals(day2);
            Boolean f9 = fhour1 || nh1.equals(hour1);
            Boolean f10 = fhour2 || nh2.equals(hour2);
            Boolean f11 = fminute1 || nm1.equals(minute1);
            Boolean f12 = fminute2 || nm2.equals(minute2);
            Boolean f13 = fsecond1 || ns1.equals(second1);
            Boolean f14 = fsecond2 || ns2.equals(second2);
            //
            if(f1 && f2 && f3 && f4 && f5 && f6 && f7 && f8 && f9 && f10 && f11 && f12 && f13 && f14){
                executeP();
            }
        }
    }

    /**
     * 2017-03-24 1.0.0
     * 具体任务子类实现，时间间隔执行，interval
     */
    public void executeI(){
        String location = "xc_crabapple_SnowTask_executeI_1";
        LogUtil.error(location, "executeI_must_be_overrided");
    }

    /**
     * 2017-03-24 1.0.0
     * 具体任务子类实现，时间点执行，point
     */
    public void executeP(){
        String location = "xc_crabapple_SnowTask_executeP_1";
        LogUtil.error(location, "executeP_must_be_overrided");
    }

    public String getPoints(){
        return points;
    }

    /**
     * @param points 如果含有两个及以上的时间点，则含有*号的时间点无效
     */
    public void setPoints(final String points){
        if(StringUtil.isBlank(points)){
            return;
        }
        if(p_pointList == null){
            p_pointList = new ArrayList<>();
        }
        String[] pointArray = StringUtil.split(points, ",");
        for(String point : pointArray){
            point = StringUtil.trim(point);
            if(StringUtil.isBlank(point)){
                continue;
            }
            Map<String, Object> pointMap = p_getPointMap(point);
            p_pointList.add(pointMap);
        }
    }

    private final Map<String, Object> p_getPointMap(final String point){
        String year1 = point.substring(0, 1);
        String year2 = point.substring(1, 2);
        String year3 = point.substring(2, 3);
        String year4 = point.substring(3, 4);
        String month1 = point.substring(5, 6);
        String month2 = point.substring(6, 7);
        String day1 = point.substring(8, 9);
        String day2 = point.substring(9, 10);
        String hour1 = point.substring(11, 12);
        String hour2 = point.substring(12, 13);
        String minute1 = point.substring(14, 15);
        String minute2 = point.substring(15, 16);
        String second1 = point.substring(17, 18);
        String second2 = point.substring(18, 19);
        //
        Map<String, Object> map = new HashMap<>(32);
        map.put("year1", year1);
        map.put("year2", year2);
        map.put("year3", year3);
        map.put("year4", year4);
        map.put("month1", month1);
        map.put("month2", month2);
        map.put("day1", day1);
        map.put("day2", day2);
        map.put("hour1", hour1);
        map.put("hour2", hour2);
        map.put("minute1", minute1);
        map.put("minute2", minute2);
        map.put("second1", second1);
        map.put("second2", second2);
        map.put("fyear1", "*".equals(year1));
        map.put("fyear2", "*".equals(year2));
        map.put("fyear3", "*".equals(year3));
        map.put("fyear4", "*".equals(year4));
        map.put("fmonth1", "*".equals(month1));
        map.put("fmonth2", "*".equals(month2));
        map.put("fday1", "*".equals(day1));
        map.put("fday2", "*".equals(day2));
        map.put("fhour1", "*".equals(hour1));
        map.put("fhour2", "*".equals(hour2));
        map.put("fminute1", "*".equals(minute1));
        map.put("fminute2", "*".equals(minute2));
        map.put("fsecond1", "*".equals(second1));
        map.put("fsecond2", "*".equals(second2));
        return map;
    }

    public String getName(){
        return name;
    }

    public void setName(final String name){
        this.name = name;
    }

    public Date getDateStart(){
        return dateStart;
    }

    public void setDateStart(final Date dateStart){
        this.dateStart = dateStart;
    }

    public Long getExeAcount(){
        return exeAcount;
    }

    public void setExeAcount(final Long exeAcount){
        this.exeAcount = exeAcount;
    }

    public Long getExeAcountMax(){
        return exeAcountMax;
    }

    public void setExeAcountMax(final Long exeAcountMax){
        this.exeAcountMax = exeAcountMax;
    }

    public Long getDelay(){
        return delay;
    }

    public void setDelay(final Long delay){
        this.delay = delay;
    }

    public Boolean isStatus(){
        return status;
    }

    public void setStatus(final Boolean status){
        this.status = status;
    }

}