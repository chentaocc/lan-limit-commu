package com.evideo.project.lanlimit.bean;

/**
 * @ProjectName: evideo-lan-limit
 * @Package: com.evideo.project.lanlimit.bean
 * @ClassName: LanLimitBean
 * @Description: java类作用描述
 * @Author: chentao
 * @CreateDate: 2020/4/14 10:50
 * @Version: 1.0
 */
public class LanLimitBean {

    private boolean enable;

    private int lan_limit;

    public LanLimitBean() {

    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public int getLan_limit() {
        return lan_limit;
    }

    public void setLan_limit(int lan_limit) {
        this.lan_limit = lan_limit;
    }
}
