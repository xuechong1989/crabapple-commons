package xc.crabapple.core.collection;

import java.util.Date;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang3.time.DateUtils;
import xc.crabapple.core.log.LogUtil;

/**
 * 2024-08-16 整理
 * 日期格式的转换处理
 */
public final class ObjectConverter implements Converter {

    public ObjectConverter(){
        
    }

    /**
     * 值转化
     */
    @Override
    public Object convert(final Class type, final Object value){
        if(value == null){
            return null;
        }
        String name = type.getName();
        if(StringUtil.equals(name, "java.util.Date") && value instanceof Date){
        	return value;
        }
        String dateStr = value.toString();
        if (dateStr.length() > 19){
            dateStr = dateStr.substring(0, 19);
        }
        try {
            Date date = DateUtils.parseDate(dateStr, new String[]{"yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss"});
            return date;
        } catch (Throwable throwable){
            LogUtil.error("BeanDateConverter_convert_e", throwable, "解析日期错误", type, value, name, dateStr);
            return null;
        }
    }

}