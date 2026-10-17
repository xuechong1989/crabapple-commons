package xc.crabapple.core.data.dco;

import xc.crabapple.core.data.CrabappleData;

/**
 * 2020-05-13 6.0.0
 * 2024-12-26 统一集成
 * Data Cache Object - DCO - 数据缓存对象
 * 缓存数据对象，与缓存数据映射，如redis中的数据
 * 注意：
 * 1 每个dco类都必须手工书写无参构造方法
 * 2 属性是封装类型时（如其他dco类），对应值不得为空对象null，可在getXXX方法加入判断，空时new一个dco对象
 */
public class DcoBase extends CrabappleData {

    private String serverId = "";  //由于dco都是基于redis做的缓存，故该类定义了该变量，用以存储当前应用程序连接的redis主机

    public DcoBase(){

    }

    public DcoBase(final String serverId){
        this.serverId = serverId;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(final String serverId) {
        this.serverId = serverId;
    }

}