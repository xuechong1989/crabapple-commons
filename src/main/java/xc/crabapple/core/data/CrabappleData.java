package xc.crabapple.core.data;

import xc.crabapple.core.json.jackson.JacksonUtil;

import java.io.Serializable;

/**
 * 2024-12-26
 * crabapple数据对象基类
 */
public class CrabappleData implements Cloneable, Serializable {

    public static final long serialVersionUID = 1L;

    public CrabappleData(){

    }

    @Override
    public String toString(){
        final String json = JacksonUtil.obj2json(this);
        return json;
    }

}
