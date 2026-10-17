package xc.crabapple.core.version;

import xc.crabapple.core.collection.StringUtil;

/**
 * 2018-02-13 1.1.0
 * 版本管理工具类
 */
public final class VersionUtil{

    /**
     * 2018-02-13 1.1.0
     * 版本比对大小
     * v1 < v2 return -1
     * v1 = v2 return 0
     * v1 > v2 return 1
     * @param v1 版本1
     * @param v2 版本2
     * @return 版本大小
     */
    public static final int compare(final String v1, final String v2){
        String[] v1Array = StringUtil.split(v1, ".");
        String[] v2Array = StringUtil.split(v2, ".");
        StringBuilder sb1 = new StringBuilder();
        for(int i = 0; i < 3; i++){
            if(i < v1Array.length){
                sb1.append(convert(v1Array[i]));
            }else{
                sb1.append(convert("000"));
            }
        }
        String version1 = sb1.toString();
        StringBuilder sb2 = new StringBuilder();
        for(int i = 0; i < 3; i++){
            if(i < v2Array.length){
                sb2.append(convert(v2Array[i]));
            }else{
                sb2.append(convert("000"));
            }
        }
        String version2 = sb2.toString();
        int iv1 = Integer.parseInt(version1);
        int iv2 = Integer.parseInt(version2);
        if(iv1 == iv2){
            return 0;
        }else if(iv1 > iv2){
            return 1;
        }else{
            return -1;
        }
    }

    /**
     * 2018-02-13 1.1.0
     * 字符串版本补零
     * @param str 版本号
     * @return 补全零
     */
    private static final String convert(final String str){
        int len = 3 - str.length();
        if(len <= 0){
            return str;
        }
        StringBuilder sb = new StringBuilder(3);
        for(int i = 0; i < len; i++){
            sb.append("0");
        }
        sb.append(str);
        return sb.toString();
    }

}
