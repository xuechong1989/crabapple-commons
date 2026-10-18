package xc.crabapple.ssm.fs.mongodb;

import xc.crabapple.core.log.LogUtil;
import org.bson.Document;
import org.bson.types.Binary;

/**
 * xuechong 20180518
 * 多线程中的传参，推荐使用值传递
 */
public class MongoReadRunnable extends YczqRunnable {

    private int count;
    private volatile byte[] buffer;
    private volatile boolean p_error = false;  //解密是否出错

    /**
     * @param count 从0开始
     * @param data
     */
    public MongoReadRunnable(int count, Object data) {
        this.count = count;
        this.setData(data);
    }

    @Override
    public void run() {
        try {
            Document document = (Document) getData();
            Binary content = (Binary) document.get("content");
            byte[] bufferEncrypt = content.getData();
            buffer = AESUtil.decryptBytes(MongoUtil.getEncryptKey(), bufferEncrypt, AESUtil.KEY256);
        } catch (Throwable throwable) {
            p_error = true;
            buffer = null;
            LogUtil.error("ReadRunnableB_run_102", throwable);
        } finally {
            super.runCompleted();
        }
    }

    public boolean isError() {
        return p_error;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

}
