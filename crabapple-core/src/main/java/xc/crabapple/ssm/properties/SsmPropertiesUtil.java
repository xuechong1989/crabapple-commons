package xc.crabapple.ssm.properties;

import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import xc.crabapple.core.file.properties.PropertiesUtil;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 2025-01-07
 * 在Spring框架中，属性占位符是XML或属性配置文件中用来表示值的变量。运行时，Bean后处理器会用实际值替换属性占位符。
 * PropertySourcesPlaceholderConfigurer是一个Bean后处理器，它通过查找和替换属性源中的相应值来解析 ${mail.host} 等占位符。
 */
public final class SsmPropertiesUtil {

    /**
     * 2025-01-07
     * @return
     */
    public static final PropertySourcesPlaceholderConfigurer getPropertySourcesPlaceholderConfigurer(){
        Properties properties = new Properties();
        Map<String, String> propertiesMap = PropertiesUtil.getPropertiesMap();
        Set<String> set = propertiesMap.keySet();
        Iterator<String> iterator = set.iterator();
        while(iterator.hasNext()){
            String key = iterator.next();
            String value = propertiesMap.get(key);
            properties.setProperty(key, value);
        }
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setProperties(properties);
        return propertySourcesPlaceholderConfigurer;
    }

}