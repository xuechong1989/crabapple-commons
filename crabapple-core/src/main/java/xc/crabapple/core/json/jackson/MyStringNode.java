package xc.crabapple.core.json.jackson;

import tools.jackson.databind.node.StringNode;

/**
 * 2024-07-31
 * 解决两个双引号的问题
 */
public final class MyStringNode extends StringNode {

    public MyStringNode(final String value) {
        super(value);
    }

    @Override
    public String toString(){
        String value = super.asText();
        return value;
    }

}
