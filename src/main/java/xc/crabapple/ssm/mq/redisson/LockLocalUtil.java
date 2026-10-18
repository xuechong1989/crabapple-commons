package xc.crabapple.ssm.mq.redisson;

import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.log.LogUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2020-12-12 7.1.0
 * 本地锁
 */
public final class LockLocalUtil {

    private static Map<String, LockLocal> p_map = null;

    public static final void init(){
        LogUtil.print("LockLocalUtil_init_1", "同步锁 ...... 本地锁");
        p_map = new ConcurrentHashMap<>(256);
    }

    /**
     * 2020-12-12 7.1.0
     * @param id 一般一个方法用一个相同的id
     */
    public static final String lock(final String id){
        final String tmpId = lock(id, 0);
        return tmpId;
    }

    /**
     * 2020-12-12 7.1.0
     * @param id 一般一个方法用一个相同的id
     * @param timeout 超时时间，单位毫秒
     * @return 返回id
     */
    public static final String lock(final String id, final long timeout){
        String localId = id;
        if(StringUtil.isBlank(localId)){
            localId = "xc_crabapple_lock_local";
        }
        long localTimeout = timeout;
        if(localTimeout <= 0){
            localTimeout = 3000;
        }
        LockLocal newLock = new LockLocal();
        LockLocal existing = p_map.putIfAbsent(localId, newLock);
        LockLocal localLock;
        if(existing != null){
            localLock = existing;
        }else{
            localLock = newLock;
        }
        localLock.lock(localTimeout);
        //
        localLock.setId(localId);
        return localId;
    }

    /**
     *
     * @param id 一般一个方法用一个相同的id
     */
    public static final void unlock(final String id){
        final LockLocal localLock = p_map.get(id);
        if(localLock == null){
            return;
        }
        localLock.unlock();
    }

    public static final List<Thread> getBlockThreadList() {
        //
        return null;
    }

}
