package com.evideo.project.lanlimit.thread;

import com.evideo.project.lanlimit.manager.LanLimitManager;
import com.evideostb.component.logger.EvLog;
import com.evideostb.component.openplatform.ModeConstant;
import com.evideostb.component.utils.configprovider.KeyName;
import com.evideostb.component.utils.configprovider.KmConfigManager;
import com.evideostb.component.utils.constant.LanLimitConstant;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.Charset;

/**
 * @ProjectName: evideo-lan-limit
 * @Package: com.evideo.project.lanlimit.thread
 * @ClassName: DeviceWaitingSearch
 * @Description: 设备等待搜索线程
 * @Author: chentao
 * @CreateDate: 2020/4/14 9:02
 * @Version: 1.0
 */
public class DeviceWaitingSearch extends Thread {

    private final String TAG = DeviceWaitingSearch.class.getSimpleName();

    private static final int DEVICE_FIND_PORT = 9000;
    private static final int RECEIVE_TIME_OUT = 1500; // 接收超时时间，应小于等于主机的超时时间1500
    private static final int RESPONSE_DEVICE_MAX = 200; // 响应设备的最大个数，防止UDP广播攻击

    private static final byte PACKET_TYPE_FIND_DEVICE_REQ_10 = 0x10; // 搜索请求
    private static final byte PACKET_TYPE_FIND_DEVICE_RSP_11 = 0x11; // 搜索响应

    private static final byte PACKET_DATA_TYPE_STATE_20 = 0x20;

    public DeviceWaitingSearch() {

    }

    @Override
    public void run() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(DEVICE_FIND_PORT);
            byte[] data = new byte[32];
            DatagramPacket pack = new DatagramPacket(data, data.length);
            while (true) {
                // 等待主机的搜索
                socket.receive(pack);
                if (verifySearchData(pack)) {
                    byte[] sendData = packData();
                    DatagramPacket sendPack = new DatagramPacket(sendData,
                            sendData.length, pack.getAddress(), pack.getPort());
                    socket.send(sendPack);
//                    socket.setSoTimeout(RECEIVE_TIME_OUT);
//                    try {
//                        socket.receive(pack);
//                        if (verifyCheckData(pack)) {
//                            onDeviceSearched((InetSocketAddress) pack.getSocketAddress());
//                            break;
//                        }
//                    } catch (SocketTimeoutException e) {
//                    }
//                    socket.setSoTimeout(0); // 连接超时还原成无穷大，阻塞式接收
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

//    /**
//     * 当设备被发现时执行
//     */
//    public abstract void onDeviceSearched(InetSocketAddress socketAddr);

    /**
     * 打包响应报文
     * 协议：$ + packType(1) + data(n)
     *  data: 由n组数据，每组的组成结构type(1) + length(4) + data(length)
     *  type类型中包含name、room类型，但name必须在最前面
     */
    private byte[] packData() {
        byte[] data = new byte[32];
        int offset = 0;
        data[offset++] = '$';
        data[offset++] = PACKET_TYPE_FIND_DEVICE_RSP_11;

        byte[] temp = getBytesFromType(PACKET_DATA_TYPE_STATE_20,
                LanLimitManager.getInstance().getState());
        System.arraycopy(temp, 0, data, offset, temp.length);
        offset += temp.length;

        byte[] retVal = new byte[offset];
        System.arraycopy(data, 0, retVal, 0, offset);

        return retVal;
    }

    private byte[] getBytesFromType(byte type, String val) {
        byte[] retVal = new byte[0];
        if (val != null) {
            byte[] valBytes = val.getBytes(Charset.forName("UTF-8"));
            retVal = new byte[5 + valBytes.length];
            retVal[0] = type;
            retVal[1] = (byte) valBytes.length;
            retVal[2] = (byte) (valBytes.length >> 8 );
            retVal[3] = (byte) (valBytes.length >> 16);
            retVal[4] = (byte) (valBytes.length >> 24);
            System.arraycopy(valBytes, 0, retVal, 5, valBytes.length);
        }

        return retVal;
    }

    /**
     * 校验搜索数据
     * 协议：$ + packType(1) + sendSeq(4)
     *  packType - 报文类型
     *  sendSeq - 发送序列
     */
    private boolean verifySearchData(DatagramPacket pack) {
        //当前不是K歌模式不回复
        int mode = KmConfigManager.getInstance().getInt(KeyName.KEY_SUPER_DESKTOP_CURRENT_MODE, ModeConstant.MODE_KTV);
        if (mode != ModeConstant.MODE_KTV) {
            EvLog.i(TAG, "verifySearchData(), is not ktv mode, return.");
            return false;
        }

        //当前处于被限制状态不回复
        String state = LanLimitManager.getInstance().getState();
        if (LanLimitConstant.LAN_LIMIT_DISABLE.equals(state)
                || LanLimitConstant.LAN_LIMIT_UNINIT.equals(state)) {
            EvLog.i(TAG, "verifySearchData(), uninit or disable, return.");
            return false;
        }

        if (pack.getLength() != 6) {
            return false;
        }

        byte[] data = pack.getData();
        int offset = pack.getOffset();
        int sendSeq;
        if (data[offset++] != '$' || data[offset++] != PACKET_TYPE_FIND_DEVICE_REQ_10) {
            return false;
        }
        sendSeq = data[offset++] & 0xFF;
        sendSeq |= (data[offset++] << 8 );
        sendSeq |= (data[offset++] << 16);
        sendSeq |= (data[offset++] << 24);

        return sendSeq >= 1 && sendSeq <= 3;
    }

//    /**
//     * 校验确认数据
//     * 协议：$ + packType(1) + sendSeq(4) + deviceIP(n<=15)
//     *  packType - 报文类型
//     *  sendSeq - 发送序列
//     *  deviceIP - 设备IP，仅确认时携带
//     */
//    private boolean verifyCheckData(DatagramPacket pack) {
//        if (pack.getLength() < 6) {
//            return false;
//        }
//
//        byte[] data = pack.getData();
//        int offset = pack.getOffset();
//        int sendSeq;
//        if (data[offset++] != '$' || data[offset++] != PACKET_TYPE_FIND_DEVICE_CHK_12) {
//            return false;
//        }
//        sendSeq = data[offset++] & 0xFF;
//        sendSeq |= (data[offset++] << 8 );
//        sendSeq |= (data[offset++] << 16);
//        sendSeq |= (data[offset++] << 24);
//        if (sendSeq < 1 || sendSeq > RESPONSE_DEVICE_MAX) {
//            return false;
//        }
//
//        String ip = new String(data, offset, pack.getLength() - offset, Charset.forName("UTF-8"));
//
//        EvLog.i(TAG, "ip from host = " + ip);
//
//        return ip.equals(NetUtil.getLocalIP());
//    }

}
