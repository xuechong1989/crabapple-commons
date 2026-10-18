package xc.crabapple.core.encoder;

import xc.crabapple.core.log.LogUtil;

/**
 * 2017-04-28 1.0.0
 */
public final class UnicodeUtil{

    /**
     * 2017-04-28 1.0.0
     * 具体语言字符串转换为unicode编码
     */
    public static final String encode(final String str){
        char[] utfBytes = str.toCharArray();
        String result = "";
        for(int byteIndex = 0; byteIndex < utfBytes.length; byteIndex++){
            String hexB = Integer.toHexString(utfBytes[byteIndex]);
            if(hexB.length() <= 2){
                hexB = "00" + hexB;
            }
            result = result + "\\u" + hexB;
        }
        return result;
    }

    /**
     * 2017-04-28 1.0.0
     * unicode编码转换成具体语言字符串
     */
    public static final String decode(final String strUni){
        int start = 0;
        int end = 0;
        final StringBuilder stringBuilder = new StringBuilder();
        while(start > -1){
            end = strUni.indexOf("\\u", start + 2);
            String charStr = "";
            if(end == -1){
                charStr = strUni.substring(start + 2, strUni.length());
            }else{
                charStr = strUni.substring(start + 2, end);
            }
            //16进制parse整形字符串
            char letter = (char)Integer.parseInt(charStr, 16);
            stringBuilder.append((Character) letter).toString();
            start = end;
        }
        String result = stringBuilder.toString();
        return result;
    }

    /**
     * 2017-04-28 1.0.0
     * unicode编码转换成具体语言字符串
     */
    public static final String decode2(final String theString){
        char aChar;
        int len = theString.length();
        StringBuilder outBuffer = new StringBuilder(len);
        for(int x = 0; x < len; ){
            aChar = theString.charAt(x++);
            if(aChar == '\\'){
                aChar = theString.charAt(x++);
                if(aChar == 'u'){
                    int value = 0;
                    for(int i = 0; i < 4; i++){
                        aChar = theString.charAt(x++);
                        switch(aChar){
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                LogUtil.error("UnicodeUtil_decode2_1", "Malformed \\uxxxx encoding", theString);
                                return null;
                        }
                    }
                    outBuffer.append((char)value);
                }else{
                    if(aChar == 't'){
                        aChar = '\t';
                    }
                    else if(aChar == 'r'){
                        aChar = '\r';
                    }
                    else if(aChar == 'n'){
                        aChar = '\n';
                    }
                    else if(aChar == 'f'){
                        aChar = '\f';
                    }
                    outBuffer.append(aChar);
                }
            }else{
                outBuffer.append(aChar);
            }
        }
        String result = outBuffer.toString();
        return result;
    }

}
