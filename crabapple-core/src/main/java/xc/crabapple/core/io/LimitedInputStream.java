package xc.crabapple.core.io;

import java.io.InputStream;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.math.MathUtil;

/**
 * 2019-11-05 3.0.1
 * 限速输入流
 */
public final class LimitedInputStream extends InputStream{

    private InputStream inputStream;
    private long limited = 0;
    private long count = 0;
    private long lastupdatetime = System.currentTimeMillis();

    /**
     * @param inputStream 输入流对象
     * @param limited 限制的最大字节数，单位字节（B），默认为0表示不限速
     */
    public LimitedInputStream(final InputStream inputStream, final long limited){
        init(inputStream, limited, "B");
    }

    /**
     * @param inputStream 输入流对象
     * @param limited 限制的最大字节数，单位字节（B），默认为0表示不限速
     * @param unit 单位，默认单位字节（B），可选值为B, KB, MB, GB, TB, PB
     */
    public LimitedInputStream(final InputStream inputStream, final long limited, final String unit){
        //
        init(inputStream, limited, unit);
    }

    /**
     * @param inputStream 输入流对象
     * @param limited 限制的最大字节数，单位字节（B），默认为0表示不限速
     * @param unit 单位，默认单位字节（B），可选值为B, KB, MB, GB, TB, PB
     */
    private void init(final InputStream inputStream, final long limited, final String unit){
        //统一都要转换成KB
        long localLimited = limited;
        if("B".equals(unit)){
            localLimited = (long) MathUtil.byte2kb(localLimited);
        }else if("KB".equals(unit)){
            //
        }else if("MB".equals(unit)){
            localLimited *= 1024;
        }else if("GB".equals(unit)){
            localLimited *= 1024 * 1024;
        }else if("TB".equals(unit)){
            localLimited *= 1024 * 1024 * 1024;
        }else if("PB".equals(unit)){
            localLimited *= 1024 * 1024 * 1024 * 1024;
        }else{
            LogUtil.error("LimitedInputStream_init_1", "paramsError", localLimited, unit);
            return;
        }
        this.inputStream = inputStream;
        this.limited = localLimited;  //单位KB
    }

    @Override
    public int read(){
        try{
            if(limited == 0){
                return inputStream.read();
            }
            if(count < limited){
                count++;
                int result = inputStream.read();
                return result;
            }
            long nowtime = System.currentTimeMillis();
            if(nowtime > lastupdatetime){
                lastupdatetime = nowtime;
                count = 0;
            }else{
                Thread.sleep(0, 100000);
            }
            int result = inputStream.read();
            return result;
        }catch(Throwable throwable){
            LogUtil.error("LimitedInputStream_read_t1", throwable);
            return 0;
        }
    }

}
