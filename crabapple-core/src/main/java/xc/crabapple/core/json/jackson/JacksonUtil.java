package xc.crabapple.core.json.jackson;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.POJONode;
import tools.jackson.databind.node.StringNode;
import tools.jackson.databind.ser.std.ToStringSerializer;
import tools.jackson.databind.type.TypeFactory;
import xc.crabapple.core.collection.ListUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.data.DataTypeUtil;
import xc.crabapple.core.json.JsonUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.timer.TimerUtil;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 2016-12-02 1.0.0
 * 2024-12-23 针对本类调用的LogUtil类方法，不再传递Object类型的对象（避免间接自动调用toString方法），确保程序不报错，防止递归调用死循环。
 * 基于jackson构建
 * 标准JSON示例：
 * {"k1":"v1", "k2":22, "k3":3.3, "k4":false, "k5":null, "k6":{"kk6":"vv6"}, "k7":["v71", "v72"]}
 */
public final class JacksonUtil {
    // 常量对应不一样配置的objectMapper
    public static final String OBJECTMAPPER_KEY_DEFAULT = "default";
    public static final String OBJECTMAPPER_KEY_MILLISECOND = "millisecond";
    //
    private static Map<String,ObjectMapper> p_objectMapperMap = null;

    /**
     * 2024-05-28
     * 使用该类则必须首先调用该方法
     */
    private static final void p_init() {
        if (p_objectMapperMap != null) {
            return;
        }
        //
        p_objectMapperMap = new HashMap<>();
        //
        ObjectMapper defaultObjectMapper = p_createDefaultObjectMapper();
        p_objectMapperMap.put(OBJECTMAPPER_KEY_DEFAULT,defaultObjectMapper);
        //
        ObjectMapper millisecondObjectMapper = p_createMillisecondObjectMapper();
        p_objectMapperMap.put(OBJECTMAPPER_KEY_MILLISECOND,millisecondObjectMapper);
    }

    /**
     * 2025-02-27
     * 判断检测给定的字符串是否为json格式
     * @param str
     * @return
     */
    public static final boolean isJson(final String str) {
        if(str == null){
            return false;
        }
        String localStr = str.trim();
        if("".equals(localStr)){
            return false;
        }
        if("{}".equals(localStr) || "[]".equals(localStr)){
            return true;
        }
        if(localStr.startsWith("{")){
            if(!localStr.endsWith("}")){
                return false;
            }
            if(localStr.indexOf(":") == -1){
                return false;
            }
        }
        if(localStr.startsWith("[")){
            if(!localStr.endsWith("]")){
                return false;
            }
        }
        try{
            p_init();
            ObjectMapper objectMapper = p_objectMapperMap.get(OBJECTMAPPER_KEY_DEFAULT);
            if (objectMapper == null){
                return false;
            }
            objectMapper.readTree(localStr);
            return true;
        }catch(Throwable throwable){
            return false;
        }
    }

    /**
     * 2024-12-28
     * 方法1：服务器响应给客户端的数据Long类型转为字符串类型，避免客户端接收与处理长整型时精度丢失的问题。
     */
    public static final SimpleModule getSimpleModule() {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        return simpleModule;
    }

    /**
     * 2024-12-28
     * 方法2：服务器响应给客户端的数据Long类型转为字符串类型，避免客户端接收与处理长整型时精度丢失的问题。
     */
    public static final ObjectMapper getObjectMapper() {
        Map<String, ObjectMapper> objectMapperMap = getObjectMapperMap();
        ObjectMapper defualtObjectMapper = objectMapperMap.get(OBJECTMAPPER_KEY_DEFAULT);
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        defualtObjectMapper = defualtObjectMapper.rebuild().addModule(simpleModule).build();
        return defualtObjectMapper;
    }

    /**
     * 2024-09-09 创建默认的ObjectMapper
     * 2026-09-21 显示加签与转换PDF都成功但PDF不显示签章签名
     * mapper底层换用不限读取长度的JsonFactory：签章平台返回的data字段是整份加签后PDF的base64（大附件实例约1.8亿字符），
     * 超过Jackson默认字符串上限1亿字符时readTree抛StreamConstraintsException，json2jsonNode按null返回会被调用方误判为空
     * @return
     */
    private static final ObjectMapper p_createDefaultObjectMapper(){
        ObjectMapper objectMapper = JsonMapper.builder(p_createUnlimitedJsonFactory())
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .build();
        return objectMapper;
    }

    /**
     * 2024-09-09 创建默认的ObjectMapper
     * 2026-09-21 显示加签与转换PDF都成功但PDF不显示签章签名
     * 与默认mapper同口径：底层换用不限读取长度的JsonFactory（原因见p_createDefaultObjectMapper）
     * @return
     */
    private static final ObjectMapper p_createMillisecondObjectMapper(){
        ObjectMapper objectMapper = JsonMapper.builder(p_createUnlimitedJsonFactory())
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .build();
        return objectMapper;
    }

    /**
     * 2026-09-21 显示加签与转换PDF都成功但PDF不显示签章签名
     * 构建不限读取长度的JsonFactory：字符串长度与文档长度上限放开到最大值，供共享ObjectMapper统一使用
     * @return 不限字符串与文档长度上限的JsonFactory
     */
    private static final JsonFactory p_createUnlimitedJsonFactory(){
        StreamReadConstraints streamReadConstraints = StreamReadConstraints.builder().maxStringLength(Integer.MAX_VALUE).maxDocumentLength(Long.MAX_VALUE).build();
        JsonFactory jsonFactory = JsonFactory.builder().streamReadConstraints(streamReadConstraints).build();
        return jsonFactory;
    }

    /**
     * 根据objectMapperKey获取指定的objectMapperKey
     * @param objectMapperKey
     * @return
     */
    private static ObjectMapper p_getObjectMapper(final String objectMapperKey){
        p_init();
        ObjectMapper objectMapper = p_objectMapperMap.get(objectMapperKey);
        if (objectMapper == null){
            LogUtil.error("JacksonUtil_p_getObjectMapper_1","没有获取到objectMapper！", objectMapperKey);
            return null;
        }
        return objectMapper;
    }

    /**
     * 2024-07-31
     * 重要更新，重写原生类
     */
    public static final ObjectNode createObjectNode() {
        ObjectNode objectNode = createObjectNode(OBJECTMAPPER_KEY_DEFAULT);
        return objectNode;
    }

    /**
     * 2024-09-09
     * 重要更新，重写原生类
     * @param objectMapperKey 使用常量，用于使用指定配置的objectMapper例如：OBJECTMAPPER_KEY_DEFAULT
     * @return
     */
    public static final ObjectNode createObjectNode(final String objectMapperKey) {
        ObjectMapper objectMapper = p_getObjectMapper(objectMapperKey);
        JsonNodeFactory jsonNodeFactory = objectMapper.getNodeFactory();
        MyObjectNode myObjectNode = new MyObjectNode(jsonNodeFactory);
        return myObjectNode;
    }

    /**
     * 2024-07-31
     * 创建自定义类
     * @return
     */
    public static final MyObjectNode createMyObjectNode(){
        ObjectNode objectNode = createObjectNode();
        MyObjectNode myObjectNode = (MyObjectNode) objectNode;
        return myObjectNode;
    }

    /**
     * 2024-07-31
     * 重要更新，重写原生类
     */
    public static final StringNode createStringNode(final String value) {
        MyStringNode myObjectNode = new MyStringNode(value);
        return myObjectNode;
    }

    /**
     * 2024-07-31
     * 创建自定义类
     * @return
     */
    public static final MyStringNode createMyStringNode(final String value){
        StringNode stringNode = createStringNode(value);
        MyStringNode myObjectNode = (MyStringNode) stringNode;
        return myObjectNode;
    }

    /**
     * 2024-07-31
     * 重要更新，重写原生类
     */
    public static final ArrayNode createArrayNode() {
        ArrayNode arrayNode = createArrayNode(OBJECTMAPPER_KEY_DEFAULT);
        return arrayNode;
    }

    /**
     * 2024-09-09
     * 重要更新，重写原生类
     * @param objectMapperKey 使用常量，用于使用指定配置的objectMapper例如：OBJECTMAPPER_KEY_DEFAULT
     * @return
     */
    public static final ArrayNode createArrayNode(final String objectMapperKey) {
        ObjectMapper objectMapper = p_getObjectMapper(objectMapperKey);
        JsonNodeFactory jsonNodeFactory = objectMapper.getNodeFactory();
        MyArrayNode myArrayNode = new MyArrayNode(jsonNodeFactory);
        return myArrayNode;
    }

    /**
     * 2024-07-31
     * 创建自定义类
     */
    public static final MyArrayNode createMyArrayNode(){
        ArrayNode arrayNode = createArrayNode();
        MyArrayNode myArrayNode = (MyArrayNode) arrayNode;
        return myArrayNode;
    }

    /**
     * 2024-07-31
     * objectNode转换为myobjectNode
     * @param jsonNode
     * @return
     */
    public static final JsonNode updateJsonNode(final JsonNode jsonNode){
        if(jsonNode == null){
            return null;
        }
        if(jsonNode instanceof ArrayNode){
            ArrayNode arrayNode = (ArrayNode) jsonNode;
            arrayNode = updateArrayNode(arrayNode);
            return arrayNode;
        }
        if(jsonNode instanceof ObjectNode){
            ObjectNode objectNode = (ObjectNode) jsonNode;
            objectNode = updateObjectNode(objectNode);
            return objectNode;
        }
        if(jsonNode instanceof StringNode){
            StringNode stringNode = (StringNode) jsonNode;
            stringNode = updateStringNode(stringNode);
            return stringNode;
        }
        return jsonNode;
    }

    /**
     * 2024-07-31
     * objectNode转换为myobjectNode
     * @param objectNode
     * @return
     */
    public static final MyObjectNode updateObjectNode(final ObjectNode objectNode){
        if(objectNode == null){
            return null;
        }
        if(objectNode instanceof MyObjectNode){
            MyObjectNode myObjectNode = (MyObjectNode) objectNode;
            return myObjectNode;
        }
        MyObjectNode myObjectNode = createMyObjectNode();
        myObjectNode.setAll(objectNode);
        return myObjectNode;
    }

    /**
     * 2024-07-31
     * @param stringNode
     * @return
     */
    public static final MyStringNode updateStringNode(final StringNode stringNode){
        if(stringNode == null){
            return null;
        }
        if (stringNode instanceof MyStringNode) {
            MyStringNode myTextNode = (MyStringNode) stringNode;
            return myTextNode;
        }
        final String value = stringNode.asText();
        MyStringNode myTextNode = createMyStringNode(value);
        return myTextNode;
    }

    /**
     * 2024-07-31
     * @param arrayNode
     * @return
     */
    public static final ArrayNode updateArrayNode(final ArrayNode arrayNode){
        for(int i = 0; i < arrayNode.size(); i++){
            JsonNode childJsonNode = arrayNode.get(i);
            if(childJsonNode instanceof ObjectNode){
                if(childJsonNode instanceof MyObjectNode){
                    continue;
                }
                ObjectNode objectNode = (ObjectNode) childJsonNode;
                objectNode = updateObjectNode(objectNode);
                arrayNode.set(i, objectNode);
            }
        }
        return arrayNode;
    }

    /**
     * 2024-06-25
     * 遍历所有JSON节点
     * @param json
     * @return
     */
    public static final List<JsonNode> getAllJsonNode(final String json){
        List<JsonNode> list = p_getAllJsonNode(json, null, null);
        return list;
    }

    /**
     * 2024-06-25
     * 遍历所有JSON节点（递归）
     * @param json
     * @param jsonNode
     * @param list
     * @return
     */
    private static List<JsonNode> p_getAllJsonNode(final String json, JsonNode jsonNode, List<JsonNode> list) {
        if(list == null){
            list = new ArrayList<>();
            jsonNode = json2jsonNode(json);
        }
        if (jsonNode.isValueNode()) {
            list.add(jsonNode);
            return list;
        }
        if (jsonNode.isObject()) {
            var iterator = jsonNode.properties().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                JsonNode jsonNode1 = entry.getValue();
                p_getAllJsonNode(null, jsonNode1, list);
            }
        }
        if (jsonNode.isArray()) {
            Iterator<JsonNode> iterator = jsonNode.iterator();
            while (iterator.hasNext()) {
                JsonNode jsonNode1 = iterator.next();
                p_getAllJsonNode(null, jsonNode1, list);
            }
        }
        //
        return list;
    }

    /**
     * 2024-06-22
     * 检测指定JSON是否为标准JSON格式
     * @param json
     */
    public static final boolean checkStandardJson(final String json) {
        boolean flag = checkStandardJson(json, false);
        return flag;
    }

    /**
     * 2024-06-22
     * 检测指定JSON是否为标准JSON格式
     * @param json
     * @param showError 是否打印日志
     */
    public static final boolean checkStandardJson(final String json, final boolean showError) {
        if (json == null || "".equals(json)) {
            return false;
        }
        //
        // 2026-09-21 显示加签与转换PDF都成功但PDF不显示签章签名
        // 2025-12-01那行约束对象构建后从未挂到mapper上（死代码，实际仍受默认上限限制），本次真正挂上；
        // 失败日志的json参数改为截断输出（超长字符串全量入日志一次可写数百MB磁盘）
        ObjectMapper objectMapper = JsonMapper.builder(p_createUnlimitedJsonFactory()).build();
        //
        try{
            objectMapper.readTree(json);
            return true;
        }catch(Throwable throwable){
            if(showError){
                String jsonCutText = p_cutLogStr(json);
                LogUtil.error("JacksonUtil_checkStandardJson_t1", throwable, "指定字符串不是标准JSON格式", jsonCutText, showError);
            }
            return false;
        }
    }

    /**
     * 2022-08-24
     * 判定字符串形式的json数组是否为空
     */
    public static final boolean isJsonArrayEmpty(String json) {
        if (StringUtil.isBlank(json)) {
            return true;
        }
        json = JsonUtil.convert2standardJson(json);
        json = JsonUtil.filter(json);
        JsonNode jsonNode = json2jsonNode(json);
        if (jsonNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) jsonNode;
            int size = arrayNode.size();
            if (size == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 2024-07-13
     * @param object
     * @return
     */
    public static final String getString(final Object object){
        if(object == null){
            return null;
        }
        if(object instanceof JsonNode){
            final JsonNode jsonNode = (JsonNode) object;
            final String value = getString(jsonNode);
            return value;
        }
        //
        String value = object.toString();
        return value;
    }

    /**
     * 2020-08-12 6.4.0
     * 简化对JsonNode对象的解析
     * @param jsonNode
     */
    public static final String getString(final JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }
        final JsonNodeType nodeType = jsonNode.getNodeType();
        final String nodeTypeStr = nodeType.toString();
        //
        if(StringUtil.equalsOfOne(nodeTypeStr,
                JsonNodeType.NULL.toString()
        )){
            return null;
        }
        // string number boolean
        if(StringUtil.equalsOfOne(nodeTypeStr,
                JsonNodeType.STRING.toString(),
                JsonNodeType.NUMBER.toString(),
                JsonNodeType.BOOLEAN.toString()
        )){
            String value = jsonNode.asText();
            return value;
        }
        if(jsonNode instanceof StringNode){
            String value = jsonNode.asText();
            return value;
        }
        // object array
        String value = jsonNode.toString();
        return value;
    }

    /**
     * 2020-05-13 6.0.0
     * 简化对JsonNode对象的解析
     * @param jsonNode
     * @param name
     */
    public static final String getString(final JsonNode jsonNode, final String name) {
        if (jsonNode == null) {
            return null;
        }
        JsonNode childJsonNode = jsonNode.get(name);
        final String childValue = getString(childJsonNode);
        if(childValue == null){
            return null;
        }
        return childValue;
    }

    /**
     * 2024-06-25
     * @param jsonNode
     * @param name
     * @param defaultReturn
     */
    public static final String getString(final JsonNode jsonNode, final String name, final String defaultReturn) {
        if (jsonNode == null) {
            return defaultReturn;
        }
        JsonNode childJsonNode = jsonNode.get(name);
        if (childJsonNode == null) {
            return defaultReturn;
        }
        final String childValue = getString(childJsonNode);
        if(childValue == null){
            return null;
        }
        return childValue;
    }

    /**
     * 2023-02-06
     * @param arrayNode
     * @param index
     */
    public static final String getString(final ArrayNode arrayNode, final int index) {
        if (arrayNode == null) {
            return null;
        }
        if (index < 0 || index >= arrayNode.size()) {
            return null;
        }
        JsonNode childJsonNode = arrayNode.get(index);
        final String childValue = getString(childJsonNode);
        if(childValue == null){
            return null;
        }
        return childValue;
    }

    /**
     * 2024-06-25
     * 简化对JsonNode对象的解析
     */
    public static final Long getLong(JsonNode jsonNode) {
        if (jsonNode == null) {
            return 0L;
        }
        final String value = getString(jsonNode);
        if(StringUtil.isBlank(value)){
            return 0L;
        }
        final Long result = Long.valueOf(value);
        return result;
    }

    /**
     * 2020-05-13 6.0.0
     * 简化对JsonNode对象的解析
     */
    public static final Long getLong(final JsonNode jsonNode, final String name) {
        if (jsonNode == null) {
            return 0L;
        }
        if (StringUtil.isBlank(name)) {
            return 0L;
        }
        JsonNode childJsonNode = jsonNode.get(name);
        final Long childValue = getLong(childJsonNode);
        return childValue;
    }

    /**
     * 2024-06-25
     * @param arrayNode
     * @param index
     */
    public static final Long getLong(final ArrayNode arrayNode, final int index) {
        if (arrayNode == null) {
            return 0L;
        }
        if (index < 0 || index >= arrayNode.size()) {
            return 0L;
        }
        JsonNode childJsonNode = arrayNode.get(index);
        final Long childValue = getLong(childJsonNode);
        return childValue;
    }

    /**
     * 2024-06-25
     * 简化对JsonNode对象的解析
     */
    public static final Integer getInt(JsonNode jsonNode) {
        if (jsonNode == null) {
            return 0;
        }
        final String value = getString(jsonNode);
        if(StringUtil.isBlank(value)){
            return 0;
        }
        final Integer result = Integer.valueOf(value);
        return result;
    }

    /**
     * 2024-06-25
     * 简化对JsonNode对象的解析
     */
    public static final Integer getInt(final JsonNode jsonNode, final String name) {
        if (jsonNode == null) {
            return 0;
        }
        if (StringUtil.isBlank(name)) {
            return 0;
        }
        JsonNode childJsonNode = jsonNode.get(name);
        final Integer childValue = getInt(childJsonNode);
        return childValue;
    }

    /**
     * 2024-06-25
     * @param arrayNode
     * @param index
     */
    public static final int getInt(final ArrayNode arrayNode, final int index) {
        if (arrayNode == null) {
            return 0;
        }
        if (index < 0 || index >= arrayNode.size()) {
            return 0;
        }
        JsonNode childJsonNode = arrayNode.get(index);
        final int childValue = getInt(childJsonNode);
        return childValue;
    }

    /**
     * 2024-06-25
     * @param jsonNode
     */
    public static final Boolean getBoolean(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }
        final JsonNodeType nodeType = jsonNode.getNodeType();
        final String nodeTypeStr = nodeType.toString();
        //
        if(StringUtil.equalsOfOne(nodeTypeStr,
                JsonNodeType.NULL.toString()
        )){
            return false;
        }
        if(StringUtil.equalsOfOne(nodeTypeStr,
                JsonNodeType.BOOLEAN.toString()
        )){
            Boolean childValue = jsonNode.asBoolean();
            return childValue;
        }
        return false;
    }

    /**
     * 2024-06-25
     * @param jsonNode
     * @param name
     */
    public static final Boolean getBoolean(final JsonNode jsonNode, final String name) {
        if (jsonNode == null) {
            return false;
        }
        if (StringUtil.isBlank(name)) {
            return false;
        }
        //
        JsonNode childJsonNode = jsonNode.get(name);
        final Boolean childValue = getBoolean(childJsonNode);
        return childValue;
    }

    /**
     * 2023-02-06
     * @param arrayNode
     * @param index
     */
    public static final Boolean getBoolean(final ArrayNode arrayNode, final int index) {
        if (arrayNode == null) {
            return null;
        }
        if (arrayNode.size() <= index) {
            return null;
        }
        //
        JsonNode childJsonNode = arrayNode.get(index);
        final Boolean childValue = getBoolean(childJsonNode);
        return childValue;
    }

    /**
     * 2024-06-26
     * @param jsonNode
     * @param name
     * @return
     */
    public static final ObjectNode getObjectNode(final JsonNode jsonNode, final String name){
        if (jsonNode == null) {
            return null;
        }
        if (StringUtil.isBlank(name)) {
            return null;
        }
        //
        JsonNode childJsonNode = jsonNode.get(name);
        if(childJsonNode == null){
            return null;
        }
        if(childJsonNode instanceof ObjectNode){
            ObjectNode objectNode = (ObjectNode) childJsonNode;
            objectNode = updateObjectNode(objectNode);
            return objectNode;
        }
        //
        return null;
    }

    /**
     * 2024-06-26
     * @param jsonNode
     * @param name
     * @return
     */
    public static final ArrayNode getArrayNode(final JsonNode jsonNode, final String name){
        if (jsonNode == null) {
            return null;
        }
        if (StringUtil.isBlank(name)) {
            return null;
        }
        //
        JsonNode childJsonNode = jsonNode.get(name);
        if(childJsonNode == null){
            return null;
        }
        if(childJsonNode instanceof ArrayNode){
            ArrayNode arrayNode = (ArrayNode) childJsonNode;
            arrayNode = updateArrayNode(arrayNode);
            return arrayNode;
        }
        //
        return null;
    }

    /**
     * 2024-07-18
     * 从ArrayNode数组中获取子级ArrayNode对象
     * @param arrayNode
     * @param index
     * @return
     */
    public static final ArrayNode getArrayNode(final ArrayNode arrayNode, final int index){
        final JsonNode childJsonNode = getJsonNode(arrayNode, index);
        if(childJsonNode instanceof ArrayNode){
            ArrayNode childArrayNode = (ArrayNode) childJsonNode;
            childArrayNode = updateArrayNode(childArrayNode);
            return childArrayNode;
        }
        return null;
    }

    /**
     * 2024-06-26
     * @param jsonNode
     * @param name
     */
    public static final JsonNode getJsonNode(final JsonNode jsonNode, final String name){
        if (jsonNode == null) {
            return null;
        }
        if (StringUtil.isBlank(name)) {
            return null;
        }
        //
        JsonNode childJsonNode = jsonNode.get(name);
        childJsonNode = updateJsonNode(childJsonNode);
        return childJsonNode;
    }

    /**
     * 2024-07-18
     * 从ArrayNode数组中获取子级JsonNode对象
     * @param arrayNode
     * @param index
     * @return
     */
    public static final JsonNode getJsonNode(final ArrayNode arrayNode, final int index){
        if(arrayNode == null){
            return null;
        }
        if(arrayNode.isEmpty()){
            return null;
        }
        if(index < 0){
            return null;
        }
        final int size = arrayNode.size();
        if(index >= size){
            return null;
        }
        JsonNode childJsonNode = arrayNode.get(index);
        if(childJsonNode == null){
            return null;
        }
        childJsonNode = updateJsonNode(childJsonNode);
        return childJsonNode;
    }

    public static final Date getDate(final JsonNode jsonNode, final String name) {
        if (jsonNode == null) {
            return null;
        }
        if (StringUtil.isBlank(name)) {
            return null;
        }
        JsonNode childJsonNode = jsonNode.get(name);
        if (childJsonNode == null) {
            return null;
        }
        if (childJsonNode.getNodeType().equals(JsonNodeType.NULL)) {
            return null;
        }
        childJsonNode = updateJsonNode(childJsonNode);
        String str = childJsonNode.asText();
        SimpleDateFormat sf = new SimpleDateFormat("EEE MMM dd hh:mm:ss z yyyy", Locale.ENGLISH);
        Date date = null;
        try{
            date = sf.parse(str);
        }catch(Throwable throwable){
            LogUtil.error("JacksonUtil_getDate_t154", throwable, "jackson转换时间类型失败", name);
        }
        return date;
    }

    /**
     * 2024-06-26
     * @param jsonNode
     * @param name
     */
    public static final File getFile(final JsonNode jsonNode, final String name){
        if (jsonNode == null) {
            return null;
        }
        if (StringUtil.isBlank(name)) {
            return null;
        }
        //
        JsonNode childJsonNode = jsonNode.get(name);
        if(childJsonNode == null){
            return null;
        }
        if(childJsonNode instanceof POJONode){
            POJONode pojoNode = (POJONode) childJsonNode;
            if(pojoNode == null){
                return null;
            }
            Object pojoObject = pojoNode.getPojo();
            if(pojoObject == null){
                return null;
            }
            if(pojoObject instanceof File){
                File file = (File) pojoObject;
                return file;
            }
        }
        //
        return null;
    }

    /**
     * 2023-02-06
     * @param arrayNode
     * @param index
     */
    public static final byte[] getBytes(final ArrayNode arrayNode, final int index) {
        if(arrayNode == null){
            return null;
        }
        JsonNode jsonNode = arrayNode.get(index);
        if(jsonNode == null){
            return null;
        }
        jsonNode = updateJsonNode(jsonNode);
        try{
            byte[] bytes = jsonNode.binaryValue();
            return bytes;
        }catch(Throwable throwable){
            LogUtil.error("JacksonUtil_getBytes_t1", throwable, index);
            return null;
        }
    }

    /**
     * 2024-06-27
     * @param bytes
     * @return
     */
    public static final ObjectNode bytes2objectNode(final byte[] bytes){
        ObjectNode objectNode = bytes2objectNode(OBJECTMAPPER_KEY_DEFAULT, bytes);
        return objectNode;
    }

    /**
     * 2024-09-09
     * 重要更新，重写原生类
     * @param objectMapperKey 使用常量，用于使用指定配置的objectMapper例如：OBJECTMAPPER_KEY_DEFAULT
     * @param bytes
     * @return
     */
    public static final ObjectNode bytes2objectNode(final String objectMapperKey, final byte[] bytes){
        ObjectMapper objectMapper = p_getObjectMapper(objectMapperKey);
        try{
            ObjectNode objectNode = (ObjectNode) objectMapper.readTree(bytes);
            objectNode = updateObjectNode(objectNode);
            return objectNode;
        }catch(Throwable throwable){
            LogUtil.error("JacksonUtil_bytes2objectNode_t1", throwable, "bytes字节数组转ObjectNode对象失败", objectMapperKey);
            return null;
        }
    }

    /**
     * 2023-02-06
     * @param arrayNode
     * @param index
     */
    public static final String[] getStringArray(ArrayNode arrayNode, final int index) {
        arrayNode = updateArrayNode(arrayNode);
        JsonNode jsonNode = arrayNode.get(index);
        List<String> list = new ArrayList<>();
        if (jsonNode instanceof ArrayNode) {
            ArrayNode arrayNode1 = (ArrayNode) jsonNode;
            Iterator<JsonNode> iterator = arrayNode1.iterator();
            while (iterator.hasNext()) {
                JsonNode jsonNode1 = iterator.next();
                String str = jsonNode1.asText();
                list.add(str);
            }
        }
        String[] array = ListUtil.list2array(list);
        return array;
    }

    /**
     * 2023-02-19
     * 获取自定义序列化对象
     */
    public static final <T extends Serializable> T getObject(final ArrayNode arrayNode, final int index, final Class<?> cls) {
        JsonNode jsonNode = arrayNode.get(index);
        if (jsonNode instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            objectNode = updateObjectNode(objectNode);
            String str = objectNode.toString();
            T t = json2bean(str, cls);
            return t;
        }
        T t = (T) jsonNode;
        return t;
    }

    /**
     * 2021-01-11 7.2.0
     * 支持递归子孙节点的自动解析
     * @param jsonNode jsonNode
     * @param nodeStr    body.userList
     * @return 若返回值是ArrayNode时每个元素的类型为ObjectNode
     */
    public static final <T extends Serializable> T getJsonNodeByChain(final JsonNode jsonNode, final String nodeStr) {
        String result = getString(jsonNode);
        T t = getJsonNodeByChain(result, nodeStr);
        return t;
    }

    /**
     * 2020-11-21 7.0.0
     * 支持递归子孙节点的自动解析
     * @param json 源字符串
     * @param nodeStr 格式如body.userList
     * @return 若返回值是ArrayNode时每个元素的类型为ObjectNode
     */
    public static final <T extends Serializable> T getJsonNodeByChain(final String json, final String nodeStr) {
        String[] array = StringUtil.split(nodeStr, ".");
        String localJson = json;
        for (int i = 0; i < array.length; i++) {
            String nodeName = array[i];
            if (StringUtil.isBlank(nodeName)) {
                continue;
            }
            if (StringUtil.isBlank(localJson)) {
                continue;
            }
            JsonNode jsonNode = json2jsonNode(localJson);
            if (jsonNode == null) {
                continue;
            }
            localJson = getString(jsonNode, nodeName);
        }
        if (StringUtil.isBlank(localJson)) {
            return null;
        }
        JsonNode jsonNode = json2jsonNode(localJson);
        if (jsonNode == null) {
            return null;
        }
        T t = (T) jsonNode;
        return t;
    }

    public static final ObjectNode obj2objectNode(final Object object) {
        if (object == null) {
            return null;
        }
        String json = obj2json(OBJECTMAPPER_KEY_DEFAULT, object, "no");
        ObjectNode objectNode = (ObjectNode) json2jsonNode(json);
        objectNode = updateObjectNode(objectNode);
        return objectNode;
    }

    /**
     * 2016-12-02 1.0.0
     * 在key:value结构中，value数据类型会保留
     */
    public static final String obj2json(final Object object) {
        if (object == null) {
            return null;
        }
        String json = obj2json(OBJECTMAPPER_KEY_DEFAULT, object, "no");
        return json;
    }

    /**
     * 2020-05-12 6.0.0
     * @param object
     * @param isRetain 结果中是否整型转为字符串类型，yes-保留原有格式，no-整型转字符串，注意，如果不转字符串则客户端js程序18位及以上整型数会溢出
     */
    public static final String obj2json(final Object object, final String isRetain) {
        String json = obj2json(OBJECTMAPPER_KEY_DEFAULT, object, isRetain);
        return json;
    }

    /**
     * 2024-09-09
     * 2024-12-23 添加基础数据类型判断，若为基础数据类型或对应数组则直接toString
     * 重要更新，重写原生类
     * @param objectMapperKey 使用常量，用于使用指定配置的objectMapper例如：OBJECTMAPPER_KEY_DEFAULT
     * @param object
     * @param isRetain 结果中是否整型转为字符串类型，yes-保留原有格式，no-整型转字符串，注意，如果不转字符串则客户端js程序18位及以上整型数会溢出
     * @return
     */
    public static final String obj2json(final String objectMapperKey, final Object object, final String isRetain) {
        if(object == null){
            return "";
        }
        if(DataTypeUtil.isBaseSingleType(object)){
            final String str = object.toString();
            return str;
        }
        //
        ObjectMapper objectMapper = p_getObjectMapper(objectMapperKey);
        try{
            if ("yes".equals(isRetain)) {
                String json = objectMapper.writeValueAsString(object);
                if (json == null) {
                    return null;
                }
                return json;
            }
            String json = objectMapper.writeValueAsString(object);
            if (json == null) {
                return null;
            }
            return json;
        }catch(Throwable throwable){
            LogUtil.error("JacksonUtil_obj2json_t1", throwable, "一般对象转json字符串失败", objectMapperKey, isRetain);
            return null;
        }
    }

    /* 将Java对象转换为包含该对象的ArrayNode。
     * 注意：此方法假设对象应被包裹在一个数组中，即使它是一个单一对象。
     * @param object 要转换的Java对象。
     * @return 包含输入对象的ArrayNode。
     */
    public static final JsonNode obj2jsonNode(final Object object) {
        JsonNode jsonNode = obj2jsonNode(OBJECTMAPPER_KEY_DEFAULT, object);
        return jsonNode;
    }

    /**
     * 2024-09-09
     * 2024-12-23 添加基础数据类型判断
     * 将Java对象转换为包含该对象的ArrayNode。
     * 注意：此方法假设对象应被包裹在一个数组中，即使它是一个单一对象。
     * @param object 要转换的Java对象。
     * @param objectMapperKey 使用常量，用于使用指定配置的objectMapper例如：OBJECTMAPPER_KEY_DEFAULT
     * @return 包含输入对象的ArrayNode。
     */
    public static final JsonNode obj2jsonNode(final String objectMapperKey, final Object object) {
        if (object == null) {
            return null;
        }
        if(DataTypeUtil.isBaseType(object)){
            return null;
        }
        // 首先序列化对象为JSON字符串
        String json = null;
        try{
            ObjectMapper objectMapper = p_getObjectMapper(objectMapperKey);
            json = objectMapper.writeValueAsString(object);
            // 然后使用ObjectMapper读取这个字符串为JsonNode，并手动构造ArrayNode包含这个节点
            JsonNode jsonNode = objectMapper.readTree(json);
            jsonNode = updateJsonNode(jsonNode);
            return jsonNode;
        }catch(Throwable throwable){
            // 处理可能的异常，例如对象无法序列化
            LogUtil.error("JacksonUtil_obj2jsonNode_t452", throwable, "对象转JsonNode对象失败", objectMapperKey, json);
        }
        return null;
    }

    /**
     * 2022-08-23
     */
    public static final String format(final String json) {
        String localJson = StringUtil.jsonFormat(json);
        return localJson;
    }

    /**
     * 2024-05-28
     * @param oldObjectNode
     * @param newObjectNode
     */
    public static final ObjectNode copyProperties(final ObjectNode oldObjectNode, final ObjectNode newObjectNode) {
        if (oldObjectNode == null) {
            return null;
        }
        // 将源ObjectNode的所有属性复制到目标ObjectNode
        MyObjectNode localNewObjectNode = updateObjectNode(newObjectNode);
        localNewObjectNode.setAll(oldObjectNode);
        return localNewObjectNode;
    }

    public static final ObjectNode copyProperties(final ObjectNode oldObjectNode) {
        if (oldObjectNode == null) {
            return null;
        }
        // 将源ObjectNode的所有属性复制到目标ObjectNode
        ObjectNode newObjectNode = JsonNodeFactory.instance.objectNode();
        newObjectNode.setAll(oldObjectNode);
        return newObjectNode;
    }

    public static final String copyProperties(final String oldJson, final String newJson) {
        String json = copyProperties(OBJECTMAPPER_KEY_DEFAULT, oldJson, newJson);
        return json;
    }

    /**
     * 2024-09-09
     * @param objectMapperKey 使用常量，用于使用指定配置的objectMapper例如：OBJECTMAPPER_KEY_DEFAULT
     * @param oldJson
     * @param newJson
     * @return
     */
    public static final String copyProperties(final String objectMapperKey, final String oldJson, final String newJson) {
        ObjectMapper objectMapper = p_getObjectMapper(objectMapperKey);
        if (StringUtil.isBlank(oldJson)) {
            return null;
        }
        String oldJson2 = null;
        String oldJson3 = null;
        String newJson2 = null;
        String newJson3 = null;
        try{
            oldJson2 = JsonUtil.convert2standardJson(oldJson);
            oldJson3 = JsonUtil.filter(oldJson2);
            ObjectNode oldNode = (ObjectNode) objectMapper.readTree(oldJson3);
            oldNode = updateObjectNode(oldNode);
            newJson2 = JsonUtil.convert2standardJson(newJson);
            newJson3 = JsonUtil.filter(newJson2);
            JsonNode newNode = objectMapper.readTree(newJson3);
            Set<Map.Entry<String, JsonNode>> set = newNode.properties();
            var keysIter = set.iterator();
            while (keysIter.hasNext()) {
                var fieldEntry = keysIter.next(); String fieldName = fieldEntry.getKey();
                JsonNode jsonNode = getJsonNode(newNode, fieldName);
                oldNode.set(fieldName, jsonNode);
            }
            String oldNodeStr = getString(oldNode);
            return oldNodeStr;
        }catch(Throwable throwable){
            LogUtil.error("JacksonUtil_copyProperties_t3", throwable, "复制properties失败", objectMapperKey, oldJson, oldJson2, oldJson3, newJson, newJson2, newJson3);
            return null;
        }
    }

    /**
     * 2016-12-02 1.0.0
     */
    public static final <T extends Object> T json2bean(final String json, final Class cls) {
        T t = json2bean(OBJECTMAPPER_KEY_DEFAULT, json, cls);
        return t;
    }

    /**
     * 2024-09-09
     * @param objectMapperKey 使用常量，用于使用指定配置的objectMapper例如：OBJECTMAPPER_KEY_DEFAULT
     * @return
     */
    public static final <T extends Object> T json2bean(String objectMapperKey, String json, Class cls) {
        ObjectMapper objectMapper = p_getObjectMapper(objectMapperKey);
        if (StringUtil.isBlank(json)) {
            return null;
        }
        //
        try{
            json = JsonUtil.convert2standardJson(json);
            json = JsonUtil.filter(json);
            Object object = objectMapper.readValue(json, cls);
            if(object == null){
                return null;
            }
            T t = (T) object;
            return t;
        }catch(Throwable throwable){
            LogUtil.error("JacksonUtil_json2bean_t1", throwable, "json字符串转bean对象失败", objectMapperKey, json, cls);
            return null;
        }
    }

    public static <T> T jsonNode2bean(JsonNode jsonNode, Class cls) {
        T t = jsonNode2bean(OBJECTMAPPER_KEY_DEFAULT, jsonNode, cls);
        return t;
    }

    /**
     * 2024-09-09
     * @param objectMapperKey 使用常量，用于使用指定配置的objectMapper例如：OBJECTMAPPER_KEY_DEFAULT
     * @param jsonNode
     * @param cls
     * @return
     * @param <T>
     */
    public static <T> T jsonNode2bean(final String objectMapperKey, JsonNode jsonNode, Class cls) {
        ObjectMapper objectMapper = p_getObjectMapper(objectMapperKey);
        if (jsonNode == null) {
            return null;
        }
        jsonNode = updateJsonNode(jsonNode);
        //
        try{
            Object object = objectMapper.convertValue(jsonNode, cls);
            if(object == null){
                return null;
            }
            T t = (T) object;
            return t;
        }catch(Throwable throwable){
            LogUtil.error("JacksonUtil_jsonNode2bean_t1", throwable, "JsonNode对象转bean对象失败", objectMapperKey, cls);
            return null;
        }
    }

    /**
     * 2020-05-12 6.0.0
     * Map类型转Bean
     */
    public static final <T extends Object> T map2bean(Map map, Class cls) {
        if (map == null) {
            return null;
        }
        String json = obj2json(map);
        if (StringUtil.isBlank(json)) {
            return null;
        }
        Object object = json2bean(json, cls);
        T t = (T) object;
        return t;
    }

    /**
     * 2024-06-27
     * @param map
     * @return
     */
    public static final String map2json(Map map){
        String json = obj2json(map);
        return json;
    }

    /**
     * 2024-06-27
     * @param map
     * @return
     */
    public static final JsonNode map2jsonNode(Map map){
        String json = obj2json(map);
        JsonNode jsonNode = json2jsonNode(json);
        return jsonNode;
    }

    /**
     * 2024-07-31
     */
    public static final ObjectNode map2objectNode(Map map){
        String json = obj2json(map);
        ObjectNode objectNode = json2objectNode(json);
        return objectNode;
    }

    /**
     * 2024-06-27
     * @param json
     * @return
     */
    public static final ObjectNode json2objectNode(final String json){
        JsonNode jsonNode = json2jsonNode(json);
        jsonNode = updateJsonNode(jsonNode);
        if(jsonNode instanceof ObjectNode){
            ObjectNode objectNode = (ObjectNode) jsonNode;
            objectNode = updateObjectNode(objectNode);
            return objectNode;
        }
        return null;
    }

    /**
     * 2016-12-02 1.0.0
     * @param json
     * @return JsonNode表示对象或者数组均可，通过getNodeType()方法判断其代表的实际数据类型
     */
    public static final JsonNode json2jsonNode(String json) {
        JsonNode jsonNode = json2jsonNode(OBJECTMAPPER_KEY_DEFAULT, json);
        return jsonNode;
    }

    /**
     * 2026-09-29 显示加签与转换PDF都成功但PDF不显示签章签名
     * json2jsonNode重载：超大标准JSON直接解析，供签章平台返回这类字段值可达上亿字符的报文使用
     * （来源教训：手工截取不识别JSON转义，平台把base64结尾的==转义成\u003d\u003d后截出带反斜杠的串，Base64解码报Illegal base64 character 5c）
     * 与两参版本唯一的差异：不做convert2standardJson与filter两道全文预处理（每道多一次全文拷贝，超大报文拖累明显）。
     * 其余行为与既有版本完全一致：解析失败记error日志（截断防巨型日志）后返回null、不外抛，保持json2jsonNode方法族的既有契约
     * @param json 标准JSON字符串（非标准JSON仍走两参版本的预处理链）
     * @param directParseFlag true=直接解析不预处理；false=行为与单参版本完全一致
     * @return JsonNode；json为空或解析失败返回null
     */
    public static final JsonNode json2jsonNode(final String json, final boolean directParseFlag) {
        // 开关关闭时走单参版本，行为与既有调用完全一致
        if (!directParseFlag) {
            JsonNode jsonNode = json2jsonNode(json);
            return jsonNode;
        }
        // 空串返回null，与既有版本口径一致
        if (StringUtil.isBlank(json)) {
            return null;
        }
        ObjectMapper objectMapper = p_getObjectMapper(OBJECTMAPPER_KEY_DEFAULT);
        JsonNode jsonNode = null;
        try {
            // 直接解析（不经过两道预处理），解析器底层为不限长度的JsonFactory
            jsonNode = objectMapper.readTree(json);
            jsonNode = updateJsonNode(jsonNode);
            return jsonNode;
        } catch (Throwable throwable) {
            // 解析失败与既有json2jsonNode同口径：error日志留痕后返回null，不外抛
            String jsonCutText = p_cutLogStr(json);
            LogUtil.error("JacksonUtil_json2jsonNode_2", throwable, "json字符串直接解析失败(不预处理重载)", jsonCutText);
            return null;
        }
    }

    /**
     * 2024-09-09
     * @param objectMapperKey 使用常量，用于使用指定配置的objectMapper例如：OBJECTMAPPER_KEY_DEFAULT
     * @param json
     * @return JsonNode表示对象或者数组均可，通过getNodeType()方法判断其代表的实际数据类型
     */
    public static final JsonNode json2jsonNode(final String objectMapperKey, final String json) {
        ObjectMapper objectMapper = p_getObjectMapper(objectMapperKey);
        if (StringUtil.isBlank(json)) {
            return null;
        }
        //
        String json2 = null;
        String json3 = null;
        try{
            json2 = JsonUtil.convert2standardJson(json);
            json3 = JsonUtil.filter(json2);
            JsonNode jsonNode = objectMapper.readTree(json3);
            jsonNode = updateJsonNode(jsonNode);
            return jsonNode;
        }catch(Throwable throwable){
            // 2026-09-21 显示加签与转换PDF都成功但PDF不显示签章签名
            // 三个json参数全量入日志曾在现场把单条error日志写到542MB（1.8亿字符×3份）并阻塞业务线程约50秒，改为截断输出
            String jsonCutText = p_cutLogStr(json);
            String json2CutText = p_cutLogStr(json2);
            String json3CutText = p_cutLogStr(json3);
            LogUtil.error("JacksonUtil_json2jsonNode_1", throwable, "json字符串转JsonNode对象失败", objectMapperKey, jsonCutText, json2CutText, json3CutText);
            return null;
        }
    }

    /**
     * 2026-09-21 显示加签与转换PDF都成功但PDF不显示签章签名
     * 异常日志用的超长字符串截断：只记总字符数与前1000字符，避免整份PDF的base64之类的大对象全量写进error日志
     * @param str 待截断的字符串（可为null）
     * @return 入参为null时返回null（由日志框架按空对象标记输出）；不超过1000字符时原样返回；超长时返回"总字符数=N,前1000字符=..."格式文本
     */
    private static final String p_cutLogStr(final String str){
        if(str == null){
            return null;
        }
        int length = str.length();
        if(length <= 1000){
            return str;
        }
        String headStr = str.substring(0, 1000);
        String cutStr = "总字符数=" + length + ",前1000字符=" + headStr;
        return cutStr;
    }

    /**
     * 2024-05-28
     * 将JSON字符串转换为指定类型的Java对象数组。
     * @param <T>        目标类型的元素
     * @param json JSON字符串
     * @param cls        目标类型数组的Class引用，例如：String[].class
     * @return 解析后的Java对象数组
     */
    public static final <T> List<T> json2list(String json, Class<T> cls) {
        List<T> list = json2list(OBJECTMAPPER_KEY_DEFAULT, json, cls);
        return list;
    }

    /**
     * 2024-09-09
     * 将JSON字符串转换为指定类型的Java对象数组。
     * @param objectMapperKey 使用常量，用于使用指定配置的objectMapper例如：OBJECTMAPPER_KEY_DEFAULT
     * @param <T>        目标类型的元素
     * @param json JSON字符串
     * @param cls        目标类型数组的Class引用，例如：String[].class
     * @return 解析后的Java对象数组
     */
    public static final <T> List<T> json2list(final String objectMapperKey, String json, Class<T> cls) {
        ObjectMapper objectMapper = p_getObjectMapper(objectMapperKey);
        if (StringUtil.isBlank(json)) {
            return null;
        }
        try{
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            JavaType javaType = typeFactory.constructCollectionType(List.class, cls);
            json = JsonUtil.convert2standardJson(json);
            json = JsonUtil.filter(json);
            List<T> list = objectMapper.readValue(json, javaType);
            return list;
        }catch(Throwable throwable){
            LogUtil.error("JacksonUtil_json2list_t700", throwable, "json字符串转List对象失败", objectMapperKey, json, cls);
            return null;
        }
    }

    /**
     * 2024-05-28
     * 把Json字符串转换成Map返回
     * @param json JSON字符串
     * @return 转换后的Map < String, Object>对象
     */
    public static final Map<String, Object> json2map2(String json) {
        Map<String, Object> map = json2map2(OBJECTMAPPER_KEY_DEFAULT, json);
        return map;
    }

    /**
     * 2024-09-09
     * 把Json字符串转换成Map返回
     * @param objectMapperKey 使用常量，用于使用指定配置的objectMapper例如：OBJECTMAPPER_KEY_DEFAULT
     * @param json JSON字符串
     * @return 转换后的Map < String, Object>对象
     */
    public static final Map<String, Object> json2map2(final String objectMapperKey, final String json) {
        ObjectMapper objectMapper = p_getObjectMapper(objectMapperKey);
        if (StringUtil.isBlank(json)) {
            return null;
        }
        String json2 = null;
        String json3 = null;
        try{
            Map<String, Object> map = new HashMap<>();
            json2 = JsonUtil.convert2standardJson(json);
            json3 = JsonUtil.filter(json2);
            ArrayNode jsonArray = (ArrayNode) objectMapper.readTree(json3);
            jsonArray = updateArrayNode(jsonArray);
            for (JsonNode jsonNode : jsonArray) {
                if (!jsonNode.isObject()) {
                    continue;
                }
                ObjectNode objectNode = (ObjectNode) jsonNode;
                objectNode = updateObjectNode(objectNode);
                String name = getString(objectNode, "name");
                String value = getString(objectNode, "value");
                String type = getString(objectNode, "type");
                if ("null".equals(value)) {
                    continue;
                }
                Object val;
                if (type.equals("Number")) {
                    val = Double.parseDouble(value);
                } else if (type.equals("Date")) {
                    val = TimerUtil.str2time(value);
                } else {
                    val = value;
                }
                map.put(name, val);
            }
            return map;
        }catch(Throwable throwable){
            LogUtil.error("JacksonUtil_json2map2_t771", throwable, "json字符串转Map对象失败", objectMapperKey, json, json2, json3);
            return null;
        }
    }

    /**
     * 2024-05-28
     * 将JSON字符串转换为Map<String, Object>对象。
     * @param json JSON字符串
     * @return 转换后的Map对象
     */
    public static final Map<String, Object> json2map(String json) {
        Map<String, Object> map = json2map(OBJECTMAPPER_KEY_DEFAULT, json);
        return map;
    }

    /**
     * 2024-09-09
     * 将JSON字符串转换为Map<String, Object>对象。
     * @param objectMapperKey 使用常量，用于使用指定配置的objectMapper例如：OBJECTMAPPER_KEY_DEFAULT
     * @param json JSON字符串
     * @return 转换后的Map对象
     */
    public static final Map<String, Object> json2map(final String objectMapperKey, final String json) {
        ObjectMapper objectMapper = p_getObjectMapper(objectMapperKey);
        if (StringUtil.isBlank(json)) {
            return null;
        }
        try{
            String json2 = JsonUtil.convert2standardJson(json);
            String json3 = JsonUtil.filter(json2);
            Map<String, Object> map = objectMapper.readValue(json3, Map.class);
            return map;
        }catch(Throwable throwable){
            LogUtil.error("JacksonUtil_json2map_t719", throwable, "json字符串转Map对象失败", objectMapperKey, json);
            return null;
        }
    }

    /**
     * 2024-06-19 json字符串转换为指定类型结构对象
     * @param json 需要转换的字符串
     * @param typeReference 类型对象 例如 TypeReference<ConcurrentHashMap<String, ConcurrentHashMap<String, List<DrawBaseEntity>>>> concurrentHashMapTypeReference = new TypeReference<ConcurrentHashMap<String, ConcurrentHashMap<String, List<DrawBaseEntity>>>>(){};
     * 返回数据结构就是<ConcurrentHashMap<String, ConcurrentHashMap<String, List<DrawBaseEntity>>>>
     */
    public static final <T> T json2map(String json, TypeReference<T> typeReference) {
        T t = json2map(OBJECTMAPPER_KEY_DEFAULT, json, typeReference);
        return t;
    }

    /**
     * 2024-09-09 json字符串转换为指定类型结构对象
     * @param objectMapperKey 使用常量，用于使用指定配置的objectMapper例如：OBJECTMAPPER_KEY_DEFAULT
     * @param json 需要转换的字符串
     * @param typeReference 类型对象 例如 TypeReference<ConcurrentHashMap<String, ConcurrentHashMap<String, List<DrawBaseEntity>>>> concurrentHashMapTypeReference = new TypeReference<ConcurrentHashMap<String, ConcurrentHashMap<String, List<DrawBaseEntity>>>>(){};
     * 返回数据结构就是<ConcurrentHashMap<String, ConcurrentHashMap<String, List<DrawBaseEntity>>>>
     */
    public static final <T> T json2map(final String objectMapperKey, String json, TypeReference<T> typeReference) {
        ObjectMapper objectMapper = p_getObjectMapper(objectMapperKey);
        if (StringUtil.isBlank(json) || typeReference == null) {
            return null;
        }
        try{
            json = JsonUtil.convert2standardJson(json);
            json = JsonUtil.filter(json);
            Object object = objectMapper.readValue(json, typeReference);
            if(object instanceof JsonNode){
                JsonNode jsonNode = (JsonNode) object;
                jsonNode = updateJsonNode(jsonNode);
                T t = (T) jsonNode;
                return t;
            }
            T t = (T) object;
            return t;
        }catch(Throwable throwable){
            LogUtil.error("JacksonUtil_json2map_t1", throwable, "json字符串转Map对象失败", objectMapperKey, json, typeReference);
            return null;
        }
    }

    public static Map<String, Object> objectNode2map(String preKey, ObjectNode objectNode) {
        if (objectNode == null) {
            return null;
        }
        if (StringUtil.isBlank(preKey)) {
            return null;
        }
        objectNode = updateObjectNode(objectNode);
        Map<String, Object> map = new HashMap();
        var keySetIter = objectNode.properties().iterator();
        while (keySetIter.hasNext()) {
            var entry2 = keySetIter.next(); String key = entry2.getKey();
            JsonNode jsonNode = getJsonNode(objectNode, key);
            map.put(preKey + key, jsonNode);
        }
        return map;
    }

    /**
     * 2024-07-31 重写
     * @param objectNode
     * @return
     */
    public static Map<String, Object> objectNode2map(ObjectNode objectNode) {
        if (objectNode == null) {
            return null;
        }
        objectNode = updateObjectNode(objectNode);
        final String json = obj2json(objectNode);
        if(StringUtil.isBlank(json)){
            return null;
        }
        Map<String, Object> map = json2map(json);
        return map;
    }

    /**
     * 2024-06-26
     * JsonNode转化为Map
     * @param jsonNode
     */
    public static final Map<String, Object> jsonNode2map(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }
        jsonNode = updateJsonNode(jsonNode);
        var keysIter2 = jsonNode.properties().iterator();
        Map<String, Object> map = new HashMap<>();
        while (keysIter2.hasNext()) {
            var entry3 = keysIter2.next(); String fieldName = entry3.getKey();
            String value = getString(jsonNode, fieldName);
            map.put(fieldName, value);
        }
        return map;
    }

    /**
     * 2024-06-27
     * @param obj
     * @return
     */
    public static final ArrayNode obj2arrayNode(final Object obj){
        String json = obj2json(obj);
        if (StringUtil.isBlank(json)) {
            return null;
        }
        ArrayNode arrayNode = json2arrayNode(json);
        arrayNode = updateArrayNode(arrayNode);
        return arrayNode;
    }

    /**
     * 2024-06-27
     * @param json
     * @return
     */
    public static final ArrayNode json2arrayNode(String json) {
        ArrayNode jsonNodes = json2arrayNode(OBJECTMAPPER_KEY_DEFAULT, json);
        return jsonNodes;
    }

    /**
     * 2024-09-09
     * @param objectMapperKey 使用常量，用于使用指定配置的objectMapper例如：OBJECTMAPPER_KEY_DEFAULT
     * @param json
     * @return
     */
    public static final ArrayNode json2arrayNode(final String objectMapperKey, String json) {
        ObjectMapper objectMapper = p_getObjectMapper(objectMapperKey);
        if (StringUtil.isBlank(json)) {
            return null;
        }
        try{
            json = JsonUtil.convert2standardJson(json);
            json = JsonUtil.filter(json);
            ArrayNode arrayNode = objectMapper.readValue(json, ArrayNode.class);
            arrayNode = updateArrayNode(arrayNode);
            return arrayNode;
        }catch(Throwable throwable){
            LogUtil.error("JacksonUtil_json2arrayNode_t719", throwable, "json字符串转ArrayNode对象失败", objectMapperKey, json);
            return null;
        }
    }

    /**
     * 2024-05-28
     * 将JSON字符串转换为List<Map<String, Object>>对象。
     * @param json JSON字符串
     * @return 转换后的List<Map < String, Object>>对象
     */
    public static final List<Map<String, Object>> json2listMap(String json) {
        List<Map<String, Object>> maps = json2listMap(OBJECTMAPPER_KEY_DEFAULT, json);
        return maps;
    }

    /**
     * 2024-09-09
     * 将JSON字符串转换为List<Map<String, Object>>对象。
     * @param objectMapperKey 使用常量，用于使用指定配置的objectMapper例如：OBJECTMAPPER_KEY_DEFAULT
     * @param json JSON字符串
     * @return 转换后的List<Map < String, Object>>对象
     */
    public static final List<Map<String, Object>> json2listMap(final String objectMapperKey, String json) {
        ObjectMapper objectMapper = p_getObjectMapper(objectMapperKey);
        if (StringUtil.isBlank(json)) {
            return null;
        }
        try{
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            JavaType javaType = typeFactory.constructCollectionType(List.class, Map.class);
            json = JsonUtil.convert2standardJson(json);
            json = JsonUtil.filter(json);
            List<Map<String, Object>> list = objectMapper.readValue(json, javaType);
            return list;
        }catch(Throwable throwable){
            LogUtil.error("JacksonUtil_json2listMap_t739", throwable, "json字符串转ListMap对象失败", objectMapperKey, json);
            return null;
        }
    }

    /**
     * 从JsonNode（ObjectNode）中删除指定的键
     * @param jsonNode ObjectNode对象
     * @param name  要删除的键
     */
    public static void remove(JsonNode jsonNode, String name) {
        if (jsonNode == null) {
            return;
        }
        if(StringUtil.isBlank(name)){
            return;
        }
        if(jsonNode instanceof ObjectNode){
            ObjectNode objectNode = (ObjectNode) jsonNode;
            objectNode.remove(name);
        }
    }

    /**
     * 从JsonNode（ObjectNode）中删除指定的键
     * @param node ObjectNode对象
     * @param name 元素名称
     * @param list 需要存入的list
     */
    public static <T> void putList(ObjectNode node, String name, List<T> list) {
        putList(OBJECTMAPPER_KEY_DEFAULT,node,name,list);
    }

    /**
     * 2024-09-09
     * 从JsonNode（ObjectNode）中删除指定的键
     * @param objectMapperKey 使用常量，用于使用指定配置的objectMapper例如：OBJECTMAPPER_KEY_DEFAULT
     * @param node ObjectNode对象
     * @param name 元素名称
     * @param list 需要存入的list
     */
    public static <T> void putList(String objectMapperKey,ObjectNode node, String name, List<T> list) {
        ObjectMapper objectMapper = p_getObjectMapper(objectMapperKey);
        // 将List转换为ArrayNode
        ArrayNode arrayNode = objectMapper.convertValue(list, ArrayNode.class);
        arrayNode = updateArrayNode(arrayNode);
        // 将ArrayNode放入ObjectNode
        node.set(name, arrayNode);
    }

    /**
     * 向arrayNode中添加任意元素
     * @param arrayNode ArrayNode对象
     * @param obj       任意元素,只能是对象
     */
    public static <T> void putObj2arrayNode(ArrayNode arrayNode, Object obj) {
        putObj2arrayNode(OBJECTMAPPER_KEY_DEFAULT,arrayNode,obj);
    }

    /**
     * 2024-09-09
     * 向arrayNode中添加任意元素
     * @param objectMapperKey 使用常量，用于使用指定配置的objectMapper例如：OBJECTMAPPER_KEY_DEFAULT
     * @param arrayNode ArrayNode对象
     * @param obj       任意元素,只能是对象
     */
    public static <T> void putObj2arrayNode(String objectMapperKey,ArrayNode arrayNode, Object obj) {
        ObjectMapper objectMapper = p_getObjectMapper(objectMapperKey);
        arrayNode = updateArrayNode(arrayNode);
        // 将MyObject转换为JsonNode
        ObjectNode objectNode = objectMapper.valueToTree(obj);
        objectNode = updateObjectNode(objectNode);
        // 将JsonNode添加到ArrayNode中
        arrayNode.add(objectNode);
    }

    public static ArrayNode jsonNode2arrayNode(JsonNode jsonNode) {
        if (jsonNode == null) {
            return createArrayNode();
        }
        String json = jsonNode.toString();
        if (StringUtil.startsWith(json, "[") && StringUtil.lastsWith(json, "]")) {
            if (jsonNode.isArray()) {
                return (ArrayNode) jsonNode;
            }
        }
        ArrayNode arrayNode = json2arrayNode("[" + json + "]");
        arrayNode = updateArrayNode(arrayNode);
        return arrayNode;
    }

    /**
     * @param arrayNode 需要排序的集合
     * @param sortName 根据的字段名
     * @param sortType 升序=asc   降序=desc
     */
    public static ArrayNode sortArrayNode(ArrayNode arrayNode, String sortName, String sortType){
        arrayNode = updateArrayNode(arrayNode);
        // 将ArrayNode转换为List<ObjectNode>以便排序
        List<ObjectNode> nodeList = new ArrayList<>();
        for (JsonNode jsonNode : arrayNode) {
            if(jsonNode == null){
                continue;
            }
            if (jsonNode instanceof ObjectNode) {
                ObjectNode objectNode = (ObjectNode) jsonNode;
                nodeList.add(objectNode);
            }
        }
        Comparator<JsonNode> comparator = new Comparator<JsonNode>(){
            @Override
            public int compare(JsonNode o1, JsonNode o2){
                int v1 = getInt(o1, sortName);
                int v2 = getInt(o2, sortName);
                return Integer.compare(v1, v2);
            }
        };
        if ("desc".equals(sortType)) {
            nodeList.sort(comparator);
        }else {
            nodeList.sort(Collections.reverseOrder(comparator));
        }
        // 重新构建排序后的ArrayNode
        final String json = obj2json(nodeList);
        ArrayNode arrayNode1 = json2arrayNode(json);
        arrayNode1 = updateArrayNode(arrayNode1);
        return arrayNode1;
    }

    /**
     * 去重
     * @param arrayNode
     * @return
     */
    public static ArrayNode arrayNodeDistinct(ArrayNode arrayNode){
        // 将ArrayNode转换为List<ObjectNode>
        String json = obj2json(arrayNode);
        List<JsonNode> nodeList = json2list(json,JsonNode.class);
        // 使用HashSet进行去重
        Set<JsonNode> uniqueNodes = new HashSet<>(nodeList);
        // 将去重后的Set转回ArrayNode
        MyArrayNode myArrayNode = createMyArrayNode();
        for(JsonNode node : uniqueNodes){
            myArrayNode.add(node);
        }
        return myArrayNode;
    }

    /**
     * 2024-12-28
     * @return
     */
    public static final Map<String, ObjectMapper> getObjectMapperMap(){
        p_init();
        return p_objectMapperMap;
    }

}