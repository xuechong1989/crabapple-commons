package xc.crabapple.core.device;

import java.io.Serializable;

/**
 * 2019-07-30 2.3.4
 * 当前操作系统的线程信息
 */
public final class ThreadInfo implements Serializable {

    public static final long serialVersionUID = 1L;

    private Integer amount;

    public ThreadInfo(){
        settings();
    }

    private void settings(){
        amount = computeAmount();
    }

    @Override
    public String toString() {
        return "ThreadInfo{" +
                "amount=" + amount +
                '}';
    }

    /**
     * 2017-03-09 1.0.0
     * 获取线程数
     */
    private Integer computeAmount(){
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        ThreadGroup topGroup = group;
        //遍历线程组树，获取根线程组
        while(group != null){
            topGroup = group;
            group = group.getParent();
        }
        //激活的线程数加倍
        int estimatedSize = topGroup.activeCount() * 2;
        Thread[] slackList = new Thread[estimatedSize];
        //获取根线程组的所有线程
        int actualSize = topGroup.enumerate(slackList);
        //copy into a list that is the exact size
        Thread[] list = new Thread[actualSize];
        System.arraycopy(slackList, 0, list, 0, actualSize);
        Integer n = list.length;
        return n;
    }

    public Integer getAmount() {
        return amount;
    }

}
