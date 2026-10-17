package xc.crabapple.core.io;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import java.io.InputStream;
import xc.crabapple.core.log.LogUtil;

/**
 * 2024-07-19
 */
public final class MyServletInputStream extends ServletInputStream {

    private final InputStream inputStream;

    public MyServletInputStream(final InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public int read() {
        try{
            int result = inputStream.read();
            return result;
        }catch(Throwable throwable){
            LogUtil.error("SecurityHttpServletRequestWrapper_read_t1", throwable);
            return 0;
        }
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void setReadListener(ReadListener readListener) {

    }

}
