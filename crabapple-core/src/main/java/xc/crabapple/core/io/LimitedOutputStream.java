package xc.crabapple.core.io;

import java.io.OutputStream;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.math.MathUtil;

/**
 * 2019-11-05 3.0.1
 * 限速输出流
 */
public final class LimitedOutputStream extends OutputStream {

    private OutputStream outputStream;
    private long limited = 0;
    private long count = 0;
    private long lastupdatetime = System.currentTimeMillis();

    /**
     * @param outputStream 输出流对象
     * @param limited 限制的最大字节数，默认单位字节（B）
     */
    public LimitedOutputStream(final OutputStream outputStream, final long limited) {
        //
        init(outputStream, limited, "B");
    }

    /**
     * @param outputStream 输出流对象
     * @param limited 限制的最大字节数，默认单位字节（B）
     * @param unit 单位，默认单位字节（B），可选值为B, KB, MB, GB, TB, PB
     */
    public LimitedOutputStream(final OutputStream outputStream, final long limited, final String unit) {
        //
        init(outputStream, limited, unit);
    }

    /**
     * @param outputStream 输出流对象
     * @param limited 限制的最大字节数，默认单位字节（B）
     * @param unit 单位，默认单位字节（B），可选值为B, KB, MB, GB, TB, PB
     */
    private void init(final OutputStream outputStream, final long limited, final String unit){
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
            LogUtil.error("LimitedOutputStream_init_1", "paramsError", localLimited, unit);
            return;
        }
        this.outputStream = outputStream;
        this.limited = localLimited;  //单位KB
    }

    @Override
    public void write(int b){
        try{
            if (limited <= 0) {
                outputStream.write(b);
                return;
            }
            if (count < limited) {
                count++;
                outputStream.write(b);
                return;
            }
            long nowtime = System.currentTimeMillis();
            if (nowtime > lastupdatetime) {
                lastupdatetime = nowtime;
                count = 0;
            } else {
                Thread.sleep(0, 100000);
            }
            outputStream.write(b);
        }catch(Throwable throwable){
            LogUtil.error("LimitedOutputStream_write_t1", throwable);
        }
    }

}
