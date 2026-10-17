package xc.crabapple.ssm.mybatis.interceptor;

import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Invocation;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.ObjectUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.cipher.sm4.Sm4Util;
import xc.crabapple.ssm.mybatis.MyBatisUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 2025-01-13
 * 数据库表字段加解密
 */
public final class DBCipherUtil {

    // 加解密算法相关
    private static String p_enable = "no";  // 默认不加解密
    private static String p_key = null;
    private static String p_iv = "";
    private static String p_mode = "CBC";
    private static Integer p_padding = 7;
    private static Map<String, Integer> p_fieldMap = null;

    /**
     * 2025-01-13
     * 数据库表字段加解密
     * @param enable
     * @param key
     * @param iv
     * @param mode
     * @param padding
     * @param classMap
     *        classMap的构造过程举例：
     *        Map<Class, List<String>> classMap = new HashMap<>();
     *        classMap.put(OsUser.class, ListUtil.getList("userNo", "mobile"));
     *        classMap.put(PdfJiaqianDao.class, ListUtil.getList("username"));
     *        classMap.put(List.class, ListUtil.getList("USER_NO_", "MOBILE_"));
     */
    public static final void setCipher(final String enable, final String key, final String iv, final String mode, final Integer padding, final Map<Class, List<String>> classMap){
        String localEnable = StringUtil.trim(enable);
        if(StringUtil.isBlank(localEnable)){
            localEnable = "no";
            p_enable = localEnable;
            return;
        }
        //
        p_enable = localEnable;
        p_key = key;
        p_iv = iv;
        p_mode = mode;
        p_padding = padding;
        //
        if(CollectionUtil.isBlank(classMap)){
            p_fieldMap = null;
        }else{
            p_fieldMap = new HashMap<>();
            Set<Class> classSet = classMap.keySet();
            Iterator<Class> iterator = classSet.iterator();
            while(iterator.hasNext()){
                Class cls = iterator.next();
                if(cls == null){
                    continue;
                }
                final List<String> fieldList = classMap.get(cls);
                if(CollectionUtil.isBlank(fieldList)){
                    continue;
                }
                final String classname = ObjectUtil.getClassname(cls);
                for(int i = 0; i < fieldList.size(); i++){
                    String fieldName = fieldList.get(i);
                    fieldName = StringUtil.trim(fieldName);
                    if(StringUtil.isBlank(fieldName)){
                        continue;
                    }
                    p_fieldMap.put(fieldName, 1);  // 兼容模式，取消了包路径限制
                    if("java.util.List".equals(classname)){
                        continue;
                    }
                    final String kkk = classname + "." + fieldName;
                    p_fieldMap.put(kkk, 1);
                }
            }
        }
    }

    /**
     * 2025-01-13
     * 解密结果集
     * @param invocation
     * @return
     */
    public static final Object decrypt(final Invocation invocation) {
        try {
            if(!"yes".equals(p_enable)){
                final Object proceedObject = MyBatisUtil.proceed(invocation);
                return proceedObject;
            }
            final Object targetObject = invocation.getTarget();
            if(!(targetObject instanceof ResultSetHandler)){
                final Object proceedObject = MyBatisUtil.proceed(invocation);
                return proceedObject;
            }
            ResultSetHandler resultSetHandler = (ResultSetHandler) targetObject;
            Class resultSetHandlerClass = resultSetHandler.getClass();
            Field paramField = resultSetHandlerClass.getDeclaredField("boundSql");
            final boolean accessible = paramField.isAccessible();
            if (!accessible) {
                paramField.setAccessible(true);
            }
            //
            final Object proceedObject = MyBatisUtil.proceed(invocation);
            if (proceedObject instanceof ArrayList) {
                ArrayList<Object> resultList = (ArrayList<Object>) proceedObject;
                if (CollectionUtil.isBlank(resultList)) {
                    return new ArrayList<>();
                }
                final int resultSize = resultList.size();
                List<Object> list = new ArrayList<>(resultSize);
                for (int i = 0; i < resultSize; i++) {
                    Object object = resultList.get(i);
                    Object plaintextObject = p_decryptObject(object);
                    list.add(plaintextObject);
                }
                return list;
            }
            Object plaintextObject = p_decryptObject(proceedObject);
            return plaintextObject;
        }catch (Throwable throwable){
            LogUtil.error("DBCipherUtil_decrypt_t5", throwable, "解密结果集失败");
            return null;
        }
    }

    /**
     * 2025-01-13
     * 解密单个字段
     * @param object
     * @return
     */
    private static Object p_decryptObject(final Object object) {
        if (object == null){
            return null;
        }
        Class objectClass = object.getClass();
        Field[] fields = objectClass.getDeclaredFields();
        for (Field field : fields) {
            final boolean flag = p_checkField(field);
            if(!flag){
                continue;
            }
            try{
                final boolean accessible = field.isAccessible();
                if(!accessible){
                    field.setAccessible(true);
                }
                Object fieldValue = field.get(object);
                if (fieldValue == null) {
                    continue;
                }
                final String fieldValueStr = fieldValue.toString();
                if(StringUtil.isBlank(fieldValueStr)) {
                    continue;
                }
                final String plaintextStr = Sm4Util.decrypt(p_key, p_iv, fieldValueStr, p_mode, p_padding);
                field.set(object, plaintextStr);
            }catch(Throwable throwable){
                final String fieldStr = field.toString();
                LogUtil.error("DBCipherUtil_p_decryptObject_t2", throwable, "解密结果集失败", fieldStr);
            }
        }
        return object;
    }

    /**
     * 2025-01-14
     * 进行sm4加密
     * @param invocation
     */
    public static final Object encrypt(final Invocation invocation) {
        try {
            if(!"yes".equals(p_enable)){
                final Object proceedObject = MyBatisUtil.proceed(invocation);
                return proceedObject;
            }
            final Object targetObject = invocation.getTarget();
            if (!(targetObject instanceof ParameterHandler)) {
                final Object proceedObject = MyBatisUtil.proceed(invocation);
                return proceedObject;
            }
            //
            ParameterHandler parameterHandler = (ParameterHandler) targetObject;
            Field paramField = parameterHandler.getClass().getDeclaredField("parameterObject");
            if (!paramField.isAccessible()) {
                paramField.setAccessible(true);
            }
            final Object paramObject = paramField.get(parameterHandler);
            if (paramObject == null) {
                final Object proceedObject = MyBatisUtil.proceed(invocation);
                return proceedObject;
            }
            //
            if (paramObject instanceof Map || paramObject instanceof MapperMethod.ParamMap) {
                Map paramMap = (Map) paramObject;
                p_encryptMap(paramMap);
                final Object proceedObject = MyBatisUtil.proceed(invocation);
                return proceedObject;
            }
            p_encryptObject(paramObject);
            final Object proceedObject = MyBatisUtil.proceed(invocation);
            return proceedObject;
        }catch (Throwable throwable){
            LogUtil.error("DBCipherUtil_encrypt_t5", throwable, "加密结果集失败");
            return null;
        }
    }

    /**
     * 2025-01-14
     * 进行sm4加密
     * @param paramMap
     */
    private static void p_encryptMap(final Map paramMap) {
        try{
            final Set<String> ketSet = paramMap.keySet();
            final Iterator<String> iterator = ketSet.iterator();
            while(iterator.hasNext()){
                String key = iterator.next();
                key = StringUtil.trim(key);
                if(key == null || "".equals(key)){
                    continue;
                }
                final boolean flag = p_checkField(key);
                if(!flag){
                    continue;
                }
                final Object object = paramMap.get(key);
                if(object == null){
                    continue;
                }
                String objectStr = object.toString();
                objectStr = StringUtil.trim(objectStr);
                if(StringUtil.isBlank(objectStr)) {
                    continue;
                }
                objectStr = objectStr.replace("%", "");
                final String encryptObjectStr = Sm4Util.encrypt(p_key, p_iv, objectStr, p_mode, p_padding);
                paramMap.put(key, encryptObjectStr);
            }
        }catch (Throwable throwable){
            LogUtil.error("DBCipherUtil_p_encryptMap_t1", throwable, "加密结果集失败");
        }
    }

    /**
     * 2025-01-14
     * 字段校验
     * @param object
     */
    private static void p_encryptObject(final Object object) {
        if (object == null){
            return;
        }
        Class objectClass = object.getClass();
        Field[] fields = objectClass.getDeclaredFields();
        for (Field field : fields) {
            final boolean flag = p_checkField(field);
            if(!flag){
                continue;
            }
            try{
                final boolean accessible = field.isAccessible();
                if(!accessible){
                    field.setAccessible(true);
                }
                Object fieldValue = field.get(object);
                if (fieldValue == null) {
                    continue;
                }
                String fileValueStr = fieldValue.toString();
                if(StringUtil.isBlank(fileValueStr)) {
                    continue;
                }
                fileValueStr = fileValueStr.replace("%", "");
                String encryptValue = Sm4Util.encrypt(p_key, p_iv, fileValueStr, p_mode, p_padding);
                field.set(object, encryptValue);
            }catch(Throwable throwable){
                final String fieldStr = field.toString();
                LogUtil.error("DBCipherUtil_p_encryptObject_t1", throwable, "加密结果集失败", fieldStr);
            }
        }
    }

    /**
     * 2025-01-14
     * @param object
     * @return
     */
    private static boolean p_checkField(final Object object){
        if(p_fieldMap == null){
            return false;
        }
        //
        if(object instanceof String){
            final String fullname = (String) object;
            if(p_fieldMap.containsKey(fullname)){
                return true;
            }
            return false;
        }
        //
        if(object instanceof Field){
            final Field field = (Field) object;
            if(field == null){
                return false;
            }
            // 格式举例：public java.lang.String com.example.efile.common.domain.dao.OsProjectManagent.projectId
            final String fieldStr = field.toString();
            final String[] array = fieldStr.split(" ");
            if(array == null){
                return false;
            }
            final int length = array.length;
            if(length <= 0){
                return false;
            }
            // 格式举例：com.example.efile.common.domain.dao.OsProjectManagent.projectId
            String fullname = array[length - 1];
            fullname = StringUtil.trim(fullname);
            if(StringUtil.isBlank(fullname)){
                return false;
            }
//            if(StringUtil.containOfOne(fullname, "java.")){
//                return false;
//            }
            if(!p_fieldMap.containsKey(fullname)){
                return false;
            }
            //
            final int modifiers = field.getModifiers();
            if(Modifier.isAbstract(modifiers) || Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                return false;
            }
        }
        //
        return true;
    }

}