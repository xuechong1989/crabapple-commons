package xc.crabapple.ssm.fs.mongodb;

import xc.crabapple.core.collection.StringUtil;

/**
 * xuechong 20170226 1.0.0
 */
public class YczqRunnable implements Runnable{

    private String poolName;  //当前线程属于哪个线程池
    private String name;  //名称，任务的唯一标志
    private Integer status = 1;  //1-waiting, 2-running, 0-completed, -1-exit, -2-销毁
    private Boolean autoDestroy = true;  //是否自动销毁
    private Object data;  //数据对象

    public YczqRunnable(){

    }

    public YczqRunnable(String name){
        this.name = name;
    }

    public YczqRunnable(String poolName, String name){
        this.poolName = poolName;
        this.name = name;
    }

    @Override
    public void run(){

    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public void setAutoDestroy(Boolean autoDestroy) {
        this.autoDestroy = autoDestroy;
    }

    protected void runBefore(){
        this.status = 1;
    }

    /**
     * 顺利执行成功并正常结束
     */
    protected void runCompleted(){
        this.status = 0;
    }

    /**
     * 非正常退出
     */
    protected void runExit(){
        this.status = -1;
    }

    /**
     * 销毁当前对象
     */
    public void destroy(){
        if(!StringUtil.isBlank(poolName) && !StringUtil.isBlank(name)){
            YczqRunnable yczqRunnable = ThreadPoolsUtil.getRunnable(poolName, name);
            if(yczqRunnable != null){
                String runnableName = yczqRunnable.getName();
                if(name.equals(runnableName)){
                    ThreadPoolsUtil.removeRunnable(poolName, name);
                }
            }
        }
        name = null;
        status = -2;
        Thread.interrupted();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStatus(){
        return this.status;
    }

    public void setStatus(Integer status){
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean getAutoDestroy() {
        return this.autoDestroy;
    }

    public void setAutoDestroy(boolean autoDestroy) {
        this.autoDestroy = autoDestroy;
    }

}
