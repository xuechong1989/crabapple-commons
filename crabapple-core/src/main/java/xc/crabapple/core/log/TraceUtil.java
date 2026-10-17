package xc.crabapple.core.log;

import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 2017-04-21 1.0.0
 */
public final class TraceUtil {

    /**
     * 2018-05-31 1.1.0
     */
    public static final String getStackStr(){
        String result = getStackStr(null, "\n");
        return result;
    }

    /**
     * 2018-05-31 1.1.0
     * @param throwable
     */
    public static final String getStackStr(final Throwable throwable){
        String result = getStackStr(throwable, "\n");
        return result;
    }

    /**
     * 2018-05-31 1.1.0
     * @param splitTag
     */
    public static final String getStackStr(final String splitTag){
        String result = getStackStr(null, splitTag);
        return result;
    }

    /**
     * 2018-05-31 1.1.0
     * 2023-02-17 添加堆栈信息过滤，去除框架等第三方类库的堆栈信息
     * @param throwable
     * @param splitTag
     */
    public static final String getStackStr(final Throwable throwable, final String splitTag){
        StackTraceElement[] list = null;
        String result = "";
        if(throwable != null){
            result = (throwable.toString() + "\n");
            list = throwable.getStackTrace();
        }else{
            list = Thread.currentThread().getStackTrace();
        }
        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        for(StackTraceElement stackTraceElement : list){
            final String clsName = stackTraceElement.getClassName();
            final String methodName = stackTraceElement.getMethodName();
            final Integer lineNumber = stackTraceElement.getLineNumber();
            if(!StringUtil.startsWith(clsName, "xc.crabapple.", "com.example.")){
                continue;
            }
            if(StringUtil.containOfOne(clsName, "$$", "xc.crabapple.core.log.LogUtil", "xc.crabapple.core.log.TraceUtil")){
                continue;
            }
            String line = (clsName + "::" + methodName + "---" + lineNumber);
            if(i == 0){
                stringBuilder.append(line);
            }else{
                stringBuilder.append(splitTag + line);
            }
            i++;
        }
        result += stringBuilder.toString();
        return result;
    }

    /**
     * 2017-04-21 1.0.0
     */
    public static final List<String> getStackList(){
        List<String> result = getStackList(null, 0);
        return result;
    }

    /**
     * 2017-04-21 1.0.0
     * @param exception
     */
    public static final List<String> getStackList(final Exception exception){
        List<String> result = getStackList(exception, 0);
        return result;
    }

    /**
     * 2017-04-21 1.0.0
     * @param startIndex
     */
    public static final List<String> getStackList(final Integer startIndex){
        List<String> result = getStackList(null, startIndex);
        return result;
    }

    /**
     * 2017-04-21 1.0.0
     * 获得程序栈列表
     */
    public static final List<String> getStackList(final Exception exception, final Integer startIndex){
        StackTraceElement[] list = null;
        List<String> result = new ArrayList<>();
        if(exception != null){
            String msg = exception.toString();
            result.add(msg);
            list = exception.getStackTrace();
        }else{
            list = Thread.currentThread().getStackTrace();
        }
        if(CollectionUtil.isBlank(list)){
            return result;
        }
        //
        int n = Math.min(100, list.length);
        Integer localStartIndex = startIndex;
        if(localStartIndex < 0){
            localStartIndex = 0;
        }else if(localStartIndex >= n){
            localStartIndex = n - 1;
        }
        for(int i = localStartIndex; i < n; i++){
            StackTraceElement ste = list[i];
            String line = (ste.getClassName() + "::" + ste.getMethodName() + "()::" + ste.getLineNumber());
            result.add(line);
        }
        return result;
    }

}
