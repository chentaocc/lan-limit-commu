package com.evideo.project.lanlimit.thread;

import com.evideo.project.lanlimit.bean.DeviceBean;
import com.evideo.project.lanlimit.manager.LanLimitManager;
import com.evideostb.component.logger.EvLog;
import com.evideostb.component.utils.NetUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;


/**
 * @ProjectName: evideo-lan-limit
 * @Package: com.evideo.project.lanlimit.thread
 * @ClassName: DeviceSearcher
 * @Description: 设备搜索线程
 * @Author: chentao
 * @CreateDate: 2020/4/13 17:43
 * @Version: 1.0
 */
public abstract class DeviceSearcher extends Thread {

    private static final String TAG = DeviceSearcher.class.getSimpleName();

    private static final int DEVICE_FIND_PORT = 9000;
    private static final int RECEIVE_TIME_OUT = 2000; // 接收超时时间
    private static final int RESPONSE_DEVICE_MAX = 30; // 响应设备的最大个数，防止UDP广播攻击

    private static final byte PACKET_TYPE_FIND_DEVICE_REQ_10 = 0x10; // 搜索请求
    private static final byte PACKET_TYPE_FIND_DEVICE_RSP_11 = 0x11; // 搜索响应

    private static final byte PACKET_DATA_TYPE_STATE_20 = 0x20;

    private DatagramSocket hostSocket;

    private byte mPackType;

    private String mDeviceIP;

    public DeviceSearcher() {

    }

    @Override
    public void run() {
        try {
            // UI显示
            onSearchStart();

            hostSocket = new DatagramSocket();
            // 设置接收超时时间
            hostSocket.setSoTimeout(RECEIVE_TIME_OUT);

            byte[] sendData = new byte[32];
            InetAddress broadIP = InetAddress.getByName("255.255.255.255");
            DatagramPacket sendPack = new DatagramPacket(sendData, sendData.length, broadIP, DEVICE_FIND_PORT);

            for (int i = 0; i < 1; i++) {
                // 发送搜索广播
                mPackType = PACKET_TYPE_FIND_DEVICE_REQ_10;
                sendPack.setData(packData(i + 1));
                hostSocket.send(sendPack);

                // 监听来信
                byte[] receData = new byte[32];
                DatagramPacket recePack = new DatagramPacket(receData, receData.length);
                try {
                    // 最多接收200个，或超时跳出循环
                    int rspCount = RESPONSE_DEVICE_MAX;
                    while (rspCount-- > 0) {
                        recePack.setData(receData);
                        hostSocket.receive(recePack);
                        if (recePack.getLength() > 0) {
                            mDeviceIP = recePack.getAddress().getHostAddress();
                            if (parsePack(recePack)) {
                                EvLog.i(TAG, "device ip = " + mDeviceIP);
                            }
                        }
                    }
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                }
            }

            onSearchFinish();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (hostSocket != null) {
                hostSocket.close();
            }
        }

    }

    /**
     * 搜索开始时执行
     */
    public abstract void onSearchStart();

    /**
     * 搜索结束后执行
     */
    public abstract void onSearchFinish();

    /**
     * 解析报文
     * 协议：$ + packType(1) + data(n)
     *  data: 由n组数据，每组的组成结构type(1) + length(4) + data(length)
     *  type类型中包含name、room类型，但name必须在最前面
     */
    private boolean parsePack(DatagramPacket pack) {
        if (pack == null || pack.getAddress() == null) {
            return false;
        }

        String ip = pack.getAddress().getHostAddress();
        String localIp = NetUtil.getLocalIP();
        if (localIp.trim().equals(ip.trim())) {
            return false;
        }

        int port = pack.getPort();
        int dataLen = pack.getLength();
        int offset = 0;
        byte packType;
        byte type;
        int len;
        DeviceBean device = null;

        if (dataLen < 2) {
            return false;
        }
        byte[] data = new byte[dataLen];
        System.arraycopy(pack.getData(), pack.getOffset(), data, 0, dataLen);
        if (data[offset++] != '$') {
            return false;
        }

        packType = data[offset++];
        if (packType != PACKET_TYPE_FIND_DEVICE_RSP_11) {
            return false;
        }

        while (offset + 5 < dataLen) {
            type = data[offset++];
            len = data[offset++] & 0xFF;
            len |= (data[offset++] << 8);
            len |= (data[offset++] << 16);
            len |= (data[offset++] << 24);

            if (offset + len > dataLen) {
                break;
            }

            switch (type) {
                case PACKET_DATA_TYPE_STATE_20:
                    device = new DeviceBean();
                    device.setState(new String(data, offset, len, Charset.forName("UTF-8")));
                    device.setIp(ip);
                    device.setPort(port);
                    break;
                default:
                    break;
            }
            offset += len;
        }

        if (device != null) {
            LanLimitManager.getInstance().addDevice(device);
            return true;
        }

        return false;
    }

    /**
     * 打包搜索报文
     * 协议：$ + packType(1) + sendSeq(4) + [deviceIP(n<=15)]
     *  packType - 报文类型
     *  sendSeq - 发送序列
     *  deviceIP - 设备IP，仅确认时携带
     */
    private byte[] packData(int seq) {
        byte[] data = new byte[1024];
        int offset = 0;

        data[offset++] = '$';

        data[offset++] = mPackType;

        seq = seq == 3 ? 1 : ++seq; // can't use findSeq++
        data[offset++] = (byte) seq;
        data[offset++] = (byte) (seq >> 8 );
        data[offset++] = (byte) (seq >> 16);
        data[offset++] = (byte) (seq >> 24);

        byte[] result = new byte[offset];
        System.arraycopy(data, 0, result, 0, offset);

        return result;
    }
}
