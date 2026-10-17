package xc.crabapple.ssm.mybatis;

import java.io.Serializable;
import xc.crabapple.core.collection.StringUtil;

/**
 * 描述：动态查询组合参数
 */
public final class QueryParam implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String OP_EQUAL = "EQ";  // 等于
    public static final String OP_NOT_EQUAL = "NEQ";  // 不等于
    public static final String OP_GREAT = "GT";  // 大于
    public static final String OP_GREAT_EQUAL = "GE";  // 大于等于
    public static final String OP_LESS = "LT";  // 小于
    public static final String OP_LESS_EQUAL = "LE";  // 小于等于
    public static final String OP_LIKE = "LK";  // 全模糊
    public static final String OP_LEFT_LIKE = "LEK";  // 左模糊
    public static final String OP_RIGHT_LIKE = "RIK";  // 右模糊
    public static final  String OP_IS_NULL = "ISNULL";  // IS NULL
    public static final  String OP_NOTNULL = "NOTNULL";  // IS NOT NULL
    public static final  String OP_IN = "IN";  // IN

    // 字段的类型
    public static final String FIELD_TYPE_STRING = "S";  // String Type = S
    public static final String FIELD_TYPE_LONG = "L";  // Long Type = L
    public static final String FIELD_TYPE_INTEGER = "I";  // Integer Type = I
    public static final String FIELD_TYPE_SHORT = "SN";  // Short Type = I
    public static final String FIELD_TYPE_FLOAT = "F";  // Float Type = F
    public static final String FIELD_TYPE_DOUBLE = "DB";  // Double Type = DL
    public static final String FIELD_TYPE_DATE = "D";  // Date Type = D
    public static final String FIELD_TYPE_BIGDECIMAL = "BD";  // BigDecimal = BD
    public static final String JOIN_TYPE_INNER = "I";  // INNER JOIN = I
    public static final String JOIN_TYPE_LEFT = "L";  // LEFT JOIN = L
    public static final String JOIN_TYPE_RIGHT = "R";  // RIGHT JOIN = R

    // 属性
    private String fieldName;  // 字段名称
    private String opType;  // 比较操作类型 如小于或大于或等于
    private Object value;  // 值
    private String fieldType;  // 字段数据类型

    public QueryParam(){

    }

    public QueryParam(final String fieldName, final String opType, final Object value){
        this.fieldName = fieldName;
        this.opType = opType;
        this.value = value;
    }

    public QueryParam(final String fieldName, final String fieldType, final String opType, final Object value){
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.opType = opType;
        this.value = value;
    }

    public String getFieldName(){
        return fieldName;
    }

    public void setFieldName(final String fieldName){
        this.fieldName = fieldName;
    }

    public String getOpType(){
        return opType;
    }

    public void setOpType(final String opType){
        this.opType = opType;
    }

    public Object getValue(){
        if(OP_LIKE.equals(opType)){
            if(!this.value.toString().startsWith("%")){
                value = "%" + value + "%";
            }
        }
        if(OP_LEFT_LIKE.equals(opType)){
            if(!this.value.toString().startsWith("%")){
                value = "%" + value;
            }
        }
        if(OP_RIGHT_LIKE.equals(opType)){
            if(!this.value.toString().endsWith("%")){
                value = value + "%";
            }
        }
        return value;
    }

    public void setValue(final Object value){
        this.value = value;
    }

    public String getFieldType(){
        return fieldType;
    }

    public void setFieldType(final String fieldType){
        this.fieldType = fieldType;
    }

    public String getFieldParam(){
        String name = fieldName.replace(".", "");
        if (opType == null){
            opType = OP_EQUAL;
        } else {
            opType = opType.toUpperCase();
        }
        //日期类型
        //if(!FIELD_TYPE_STRING.equals(fieldType)){
        if(OP_LESS_EQUAL.equals(opType) || OP_LESS.equals(opType)){
            return name +"_END";
        }
        else if(OP_GREAT.equals(opType) || OP_GREAT_EQUAL.equals(opType) ){
            return name +"_START";
        }
        //}
        return name;
    }

    public String getSql(){
        if (opType == null){
            opType = OP_EQUAL;
        } else {
            opType = opType.toUpperCase();
        }
        String fieldParam = "#{" + getFieldParam() + "}";
        String sql = fieldName;
        if (OP_EQUAL.equals(opType)){
            sql +=  " = " + fieldParam;
        }
        else if (OP_NOT_EQUAL.equals(opType)){
            sql +=  " != " + fieldParam;
        }
        else if (OP_LESS.equals(opType)){
            sql +=  " < " + fieldParam;
        } else if (OP_LESS_EQUAL.equals(opType)){
            sql +=  " <=  " + fieldParam;
        } else if (OP_GREAT.equals(opType)){
            sql +=  " > " + fieldParam;
        } else if (OP_GREAT_EQUAL.equals(opType)){
            sql +=  " >=  " + fieldParam;
        } else if (OP_LEFT_LIKE.equals(opType)){
            sql +=  " like " + fieldParam;
        } else if (OP_RIGHT_LIKE.equals(opType)){
            sql +=  " like  " + fieldParam;
        } else if (OP_LIKE.equals(opType)){
            sql +=  " like  " + fieldParam;
        } else if (OP_IS_NULL.equals(opType)){
            sql +=  " is null " ;
        } else if (OP_NOTNULL.equals(opType)){
            sql +=  " is not null";
        }
        else if (OP_IN.equals(opType)){
            String tmp = getInSql();
            if(StringUtil.isBlank(tmp)){
                return "";
            }
            sql+= tmp;
        }
        else {
            sql +=  "  =  " + fieldParam;
        }
        return sql;
    }

    private String getInSql(){
        if( this.value == null) return "";
        if(this.value instanceof String){
            return " IN (" +this.value +")";
        }
        if(this.value.getClass().isArray()){
            Object[] ary = (Object[]) this.value;
            String str = "";
            for(int i = 0;i<ary.length;i++){
                Object o = ary[i];
                if(FIELD_TYPE_STRING.equals(fieldType)){
                    str+= ((i == 0)?"":",") +"'" +o.toString() +"'";
                }
                else{
                    str+= ((i == 0)?"":",") +o.toString() ;
                }
            }
            return " IN (" +str +")";
        }
        return "";
    }

}
