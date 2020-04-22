package com.evideo.project.lanlimit.bean;

/**
 * @ProjectName: evideo-lan-limit
 * @Package: com.evideo.project.lanlimit.bean
 * @ClassName: DeviceBean
 * @Description: 设备bean
 * @Author: chentao
 * @CreateDate: 2020/4/13 17:40
 * @Version: 1.0
 */
public class DeviceBean {

    // IP地址
    private String ip;

    // 端口
    private int port;

    // 设备状态
    private String state;

    @Override
    public int hashCode() {
        return ip.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DeviceBean) {
            return this.ip.equals(((DeviceBean)o).getIp());
        }
        return super.equals(o);
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
