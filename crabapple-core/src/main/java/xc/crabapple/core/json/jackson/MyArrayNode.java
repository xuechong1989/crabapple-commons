package xc.crabapple.core.json.jackson;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;
import java.util.List;

/**
 * 2024-07-31
 * 解决两个双引号的问题
 */
public final class MyArrayNode extends ArrayNode {
    
    public MyArrayNode(final JsonNodeFactory jsonNodeFactory) {
        super(jsonNodeFactory);
    }

    public MyArrayNode(final JsonNodeFactory jsonNodeFactory, final int capacity) {
        super(jsonNodeFactory, capacity);
    }

    public MyArrayNode(final JsonNodeFactory jsonNodeFactory, final List<JsonNode> childrenList) {
        super(jsonNodeFactory, childrenList);
    }

    /**
     * 2024-07-31
     * @param jsonNode
     * @return
     */
    @Override
    public ArrayNode add(final JsonNode jsonNode){
        JsonNode jsonNode2 = p_convert(jsonNode);
        ArrayNode arrayNode = super.add(jsonNode2);
        return arrayNode;
    }

    /**
     * 2024-07-31
     * @param arrayNode
     * @return
     */
    @Override
    public ArrayNode addAll(final ArrayNode arrayNode){
        if(arrayNode == null){
            return null;
        }
        for(int i = 0; i < arrayNode.size(); i++){
            JsonNode jsonNode = arrayNode.get(i);
            jsonNode = p_convert(jsonNode);
            arrayNode.set(i, jsonNode);
        }
        ArrayNode arrayNode2 = super.addAll(arrayNode);
        return arrayNode2;
    }

    /**
     * 2024-07-31
     * @param index
     * @param jsonNode
     * @return
     */
    public JsonNode mySet(final int index, final JsonNode jsonNode){
        JsonNode jsonNode2 = p_convert(jsonNode);
        jsonNode2 = super.set(index, jsonNode2);
        return jsonNode2;
    }

    /**
     * 2024-07-31
     * @param index
     * @return
     */
    @Override
    public JsonNode get(final int index){
        JsonNode jsonNode = super.get(index);
        //
        jsonNode = p_convert(jsonNode);
        return jsonNode;
    }

    /**
     * 2024-07-31
     * 转换统一方法
     * @param jsonNode
     * @return
     */
    private JsonNode p_convert(final JsonNode jsonNode){
        if(jsonNode instanceof ObjectNode){
            ObjectNode objectNode = (ObjectNode) jsonNode;
            if(objectNode instanceof MyObjectNode){
                MyObjectNode myObjectNode = (MyObjectNode) objectNode;
                return myObjectNode;
            }
            MyObjectNode myObjectNode = JacksonUtil.createMyObjectNode();
            myObjectNode.setAll(objectNode);
            return myObjectNode;
        }
        //
        if(jsonNode instanceof StringNode){
            StringNode stringNode = (StringNode) jsonNode;
            if(stringNode instanceof MyStringNode){
                MyStringNode myTextNode = (MyStringNode) stringNode;
                return myTextNode;
            }
            final String value = stringNode.asText();
            MyStringNode myTextNode = JacksonUtil.createMyStringNode(value);
            return myTextNode;
        }
        //
        return jsonNode;
    }
    
}
