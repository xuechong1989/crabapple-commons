package xc.crabapple.core.random;

import xc.crabapple.core.log.LogUtil;

import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Enumeration;

/**
 * 2017-07-19 1.0.0
 * 生成24位字符串
 */
public final class RandomStr24 implements Comparable<RandomStr24>{

    private static boolean p_inited = false;
    private final int _time;
    private final int _machine;
    private final int _inc;
    private boolean _new;
    private static int _nextInc = (new java.util.Random()).nextInt();
    private static final String _incLock = new String("IdGenerator._incLock");
    private static int _gentime = _flip((int)(System.currentTimeMillis() / 1000));
    private static Thread _timeFixer;
    private static int _genmachine;

    public RandomStr24(){
        init();
        _time = _gentime;
        _machine = _genmachine;
        synchronized(_incLock){
            _inc = _nextInc++;
        }
        _new = true;
    }

    /**
     * 2017-07-19 1.0.0
     */
    public static final void init(){
        if(p_inited){
            return;
        }
        p_inited = true;
        //
        try{
            final int machinePiece;
            StringBuilder sb = new StringBuilder();
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while(e.hasMoreElements()){
                NetworkInterface ni = e.nextElement();
                sb.append(ni.toString());
            }
            machinePiece = sb.toString().hashCode() << 16;
            final int processPiece = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().hashCode() & 0xFFFF;
            _genmachine = machinePiece | processPiece;
        }catch(Throwable throwable){
            LogUtil.error("RandomStr24_init_t1", throwable);
            return;
        }
        _timeFixer = new Thread("IdGenerator-TimeFixer"){
            public void run(){
                while(true){
                    try{
                        Thread.sleep(499);
                    }catch(Throwable throwable){
                        LogUtil.error("RandomStr24_init_t1", throwable, "");
                    }
                    _gentime = _flip((int)(System.currentTimeMillis() / 1000));
                }
            }
        };
        _timeFixer.setDaemon(true);
        _timeFixer.start();
    }

    /**
     * 2017-07-19 1.0.0
     * 调用该方法获取24位字符串ID
     */
    public static final String getStr(){
        RandomStr24 randomStr24 = new RandomStr24();
        String str = randomStr24.toString();
        return str;
    }

    /**
     * 2017-07-19 1.0.0
     */
    public int hashCode(){
        return _inc;
    }

    /**
     * 2017-07-19 1.0.0
     */
    private String toStringMongod(){
        byte b[] = toByteArray();
        StringBuilder buf = new StringBuilder(24);
        for(int i = 0; i < b.length; i++){
            int x = b[i] & 0xFF;
            String s = Integer.toHexString(x);
            if(s.length() == 1)
                buf.append("0");
            buf.append(s);
        }
        return buf.toString();
    }

    /**
     * 2017-07-19 1.0.0
     */
    public byte[] toByteArray(){
        byte b[] = new byte[12];
        ByteBuffer bb = ByteBuffer.wrap(b);
        bb.putInt(_inc);
        bb.putInt(_machine);
        bb.putInt(_time);
        reverse(b);
        return b;
    }

    /**
     * 2017-07-19 1.0.0
     * @param b
     */
    public static final void reverse(final byte[] b){
        for(int i = 0; i < b.length / 2; i++){
            byte t = b[i];
            b[i] = b[b.length - (i + 1)];
            b[b.length - (i + 1)] = t;
        }
    }

    /**
     * 2017-07-19 1.0.0
     * @param s
     * @param p
     */
    public static final String _pos(final String s, final int p){
        String localS = s.substring(p * 2, (p * 2) + 2);
        return localS;
    }

    /**
     * 2017-07-19 1.0.0
     */
    public String toString(){
        String str = toStringMongod();
        return str;
    }

    /**
     * 2017-07-19 1.0.0
     * @param id
     */
    public int compareTo(final RandomStr24 id){
        if(id == null){
            return -1;
        }
        long xx = id.getTime() - getTime();
        if(xx > 0){
            return -1;
        }else if(xx < 0){
            return 1;
        }
        int x = id._machine - _machine;
        if(x != 0){
            return -x;
        }
        x = id._inc - _inc;
        if(x != 0){
            return -x;
        }
        return 0;
    }

    /**
     * 2017-07-19 1.0.0
     */
    public int getMachine(){
        //
        return _machine;
    }

    /**
     * 2017-07-19 1.0.0
     */
    public long getTime(){
        long z = _flip(_time);
        z = z * 1000;
        return z;
    }

    /**
     * 2017-07-19 1.0.0
     */
    public int getInc(){
        //
        return _inc;
    }

    /**
     * 2017-07-19 1.0.0
     * @param x
     */
    public static final int _flip(final int x){
        byte b[] = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(x);
        bb.flip();
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getInt();
    }

}
