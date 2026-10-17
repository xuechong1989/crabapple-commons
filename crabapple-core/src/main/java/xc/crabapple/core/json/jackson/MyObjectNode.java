package xc.crabapple.core.json.jackson;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;
import java.util.Map;

/**
 * 2024-07-31
 * 解决两个双引号的问题
 */
public final class MyObjectNode extends ObjectNode {

    public MyObjectNode(final JsonNodeFactory jsonNodeFactory) {
        super(jsonNodeFactory);
    }

    public MyObjectNode(final JsonNodeFactory jsonNodeFactory, final Map<String, JsonNode> childrenMap) {
        super(jsonNodeFactory, childrenMap);
    }

    /**
     * 2024-07-31
     * @param name
     * @return
     */
    @Override
    public JsonNode get(final String name){
        JsonNode jsonNode = super.get(name);
        jsonNode = p_convert(jsonNode);
        return jsonNode;
    }

    /**
     * 2024-07-31
     * @param index
     * @return
     */
    @Override
    public JsonNode get(final int index){
        JsonNode jsonNode = super.get(index);
        jsonNode = p_convert(jsonNode);
        return jsonNode;
    }

    /**
     * 2024-07-31
     * @return
     */
    @Override
    public String toString(){
        JsonNode jsonNode = p_convert(this);
        if(jsonNode instanceof MyStringNode){
            MyStringNode myTextNode = (MyStringNode) jsonNode;
            String value = myTextNode.toString();
            return value;
        }
        //
        String value = super.toString();
        return value;
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