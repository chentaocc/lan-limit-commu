package com.evideo.project.lanlimit.manager;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.text.TextUtils;
import android.view.Display;

import com.evideo.project.lanlimit.bean.DeviceBean;
import com.evideo.project.lanlimit.bean.LanLimitBean;
import com.evideo.project.lanlimit.presentation.TvLanLimitPresentation;
import com.evideo.project.lanlimit.thread.DeviceSearcher;
import com.evideo.project.lanlimit.thread.DeviceWaitingSearch;
import com.evideo.project.lanlimit.view.DeviceDialog;
import com.evideostb.component.logger.EvLog;
import com.evideostb.component.newdatacenter.NewDataCenterManager;
import com.evideostb.component.openplatform.playctrl.IPlayCtrl;
import com.evideostb.component.utils.NetUtil;
import com.evideostb.component.utils.application.ApplicationUtil;
import com.evideostb.component.utils.configprovider.KeyName;
import com.evideostb.component.utils.configprovider.KmConfigManager;
import com.evideostb.component.utils.constant.LanLimitConstant;
import com.evideostb.servicemanager.component.ServiceManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/**
 * @ProjectName: evideo-lan-limit
 * @Package: com.evideo.project.lanlimit.manager
 * @ClassName: LanLimitManager
 * @Description: java类作用描述
 * @Author: chentao
 * @CreateDate: 2020/4/14 14:14
 * @Version: 1.0
 */
public class LanLimitManager {

    private static final String TAG = LanLimitManager.class.getSimpleName();

    private boolean mIsInit = false;

    private String mState = LanLimitConstant.LAN_LIMIT_UNINIT;

    //连续改变状态次数
    private int mCount = 0;

    private boolean enable;

    private int lan_limit_num;

    private DeviceDialog mDialog = null;

    private Set<DeviceBean> mDeviceSet = new HashSet<>();

    private TvLanLimitPresentation mPresentation;

    private IPlayCtrl mPlayerService = null;

    private Context mContext;

    private static LanLimitManager sInstance = null;

    private LanLimitManager() {
        mContext = ApplicationUtil.getInstance();
        KmConfigManager.getInstance().putString(KeyName.KEY_NAME_LAN_LIMIT_CURRENT_ENABLE, LanLimitConstant.LAN_LIMIT_UNINIT);
        enable = KmConfigManager.getInstance().getBoolean(KeyName.KEY_NAME_LAN_LIMIT_EANBLE, true);
        if (enable) {
            lan_limit_num = KmConfigManager.getInstance().getInt(KeyName.KEY_NAME_LAN_LIMIT_NUM, 10);
        } else {
            lan_limit_num = 0;
        }
    }

    public static LanLimitManager getInstance() {
        if (sInstance == null) {
            synchronized (LanLimitManager.class) {
                if (sInstance == null) {
                    sInstance = new LanLimitManager();
                }
            }
        }

        return sInstance;
    }

    public void init() {
        EvLog.i(TAG, "init()");

        // 考虑到开机网络存在未连接的情况，进行每隔5s的10次失败重试
        Disposable disposable = Observable.intervalRange(0, 10, 0, 5, TimeUnit.SECONDS)
                .takeWhile(aLong -> !mIsInit).subscribe(aLong -> {
                    if (!NetUtil.isNetworkAvailable(mContext)) {
                        return;
                    }

                    Gson gson = new Gson();
                    JsonObject configs = NewDataCenterManager.get().getCmsService()
                            .getAppSetting(mContext.getPackageName()).getSettingJson();
                    if (configs != null) {
                        LanLimitBean bean = gson.fromJson(configs.toString(), LanLimitBean.class);
                        if (bean != null) {
                            KmConfigManager.getInstance().putBoolean(KeyName.KEY_NAME_LAN_LIMIT_EANBLE, bean.isEnable());
                            KmConfigManager.getInstance().putInt(KeyName.KEY_NAME_LAN_LIMIT_NUM, bean.getLan_limit());
                            setEnable(bean.isEnable());
                            setLan_limit_num(bean.getLan_limit());
                            mIsInit = true;
                        }
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                });

        // 广播消息接收线程初始化
        new DeviceWaitingSearch().start();
    }

    public void uninit() {
        mState = LanLimitConstant.LAN_LIMIT_UNINIT;
        KmConfigManager.getInstance().putString(KeyName.KEY_NAME_LAN_LIMIT_CURRENT_ENABLE, mState);
        //恢复初始状态
        enableOrderSong();
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public int getLan_limit_num() {
        return lan_limit_num;
    }

    public void setLan_limit_num(int lan_limit_num) {
        if (enable) {
            this.lan_limit_num = lan_limit_num;
        } else {
            this.lan_limit_num = 0;
        }
    }

    public String getState() {
        return mState;
    }

    public void addDevice(DeviceBean bean) {
        if (bean == null) {
            return;
        }

        for (DeviceBean device : mDeviceSet) {
            if (device == null || TextUtils.isEmpty(device.getIp())) {
                continue;
            }

            if (device.getIp().equals(bean.getIp())) {
                return;
            }
        }

        EvLog.i(TAG, "addDevice(), ip = " + bean.getIp());
        EvLog.i(TAG, "addDevice(), state = " + bean.getState());

        mDeviceSet.add(bean);
    }

    private void handleLanLimit() {
        EvLog.i(TAG, "handleLanLimit()");

        int deviceNum = getDeviceNum();
        EvLog.i(TAG, "deviceNum = " + deviceNum);
        if (deviceNum < lan_limit_num) {
            mState = LanLimitConstant.LAN_LIMIT_ENABLE;
        } else {
            mState = LanLimitConstant.LAN_LIMIT_DISABLE;
        }

        String state = KmConfigManager.getInstance().getString(KeyName.KEY_NAME_LAN_LIMIT_CURRENT_ENABLE,
                LanLimitConstant.LAN_LIMIT_UNINIT);
        if (!mState.equals(state)) {
            KmConfigManager.getInstance().putString(KeyName.KEY_NAME_LAN_LIMIT_CURRENT_ENABLE, mState);
            mCount++;
        } else {
            mCount = 0;
        }

        if (LanLimitConstant.LAN_LIMIT_ENABLE.equals(mState)) {
            // 恢复正常点歌
            enableOrderSong();
        } else {
            // 限制点歌
            disableOrderSong();
        }
    }

    public void searchDevices() {
        EvLog.i(TAG, "searchDevices()");
        new DeviceSearcher() {
            @Override
            public void onSearchStart() {
                // 此处延时等待是为了解决局域网内存在大于等于两台机顶盒并发发广播，
                // 导致连接数一会超出10台，一会少于10台问题。
                if (mCount == 3) {
                    mCount = 0;
                    Random random = new Random();
                    long time = random.nextInt(30) * 1000;
                    EvLog.i(TAG, "sleep time = " + time);
                    try {
                        Thread.sleep(time);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mDeviceSet.clear();
//                ApplicationUtil.runOnUi(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (mDialog != null) {
//                            mDialog.dismiss();
//                            mDialog = null;
//                        }
//                    }
//                });
            }

            @Override
            public void onSearchFinish() {
                handleLanLimit();
//                ApplicationUtil.runOnUi(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (mDialog != null) {
//                            mDialog.dismiss();
//                            mDialog = null;
//                        }
//
//                        mDialog = new DeviceDialog(ApplicationUtil.getInstance());
//                        mDialog.show();
//                    }
//                });
            }
        }.start();
    }

    public Set<DeviceBean> getDevices() {
        return mDeviceSet;
    }

    public int getDeviceNum() {
        return mDeviceSet == null ? 0 : mDeviceSet.size();
    }

    private void enableOrderSong() {
        //隐藏TV端遮挡
        ApplicationUtil.runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    //恢复静音
                    if (mPlayerService == null) {
                        mPlayerService = ServiceManager.getInstance().getService(IPlayCtrl.class);
                    }

                    if (mPlayerService != null) {
                        if (mPlayerService.isMute()) {
                            mPlayerService.toggleMute();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (mPresentation != null && mPresentation.isShowing()) {
                    mPresentation.dismiss();
                    mPresentation = null;
                }
            }
        });
    }

    private void disableOrderSong() {
        //显示TV端遮挡
        ApplicationUtil.runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    //启动静音
                    if (mPlayerService == null) {
                        mPlayerService = ServiceManager.getInstance().getService(IPlayCtrl.class);
                    }

                    if (mPlayerService != null) {
                        if (!mPlayerService.isMute()) {
                            mPlayerService.toggleMute();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (mPresentation == null) {
                    DisplayManager displayManager = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
                    if (displayManager == null) {
                        return;
                    }

                    Display[] presentationDisplays = displayManager.getDisplays();
                    if (presentationDisplays.length > 1) {
                        Display display = presentationDisplays[1];
                        mPresentation = new TvLanLimitPresentation(mContext, display);
                    }
                }

                if (mPresentation != null && !mPresentation.isShowing()) {
                    mPresentation.show();
                }
            }
        });
    }

    public void syncState() {
        mPlayerService = ServiceManager.getInstance().getService(IPlayCtrl.class);
        if (mPlayerService == null) {
            return;
        }

        if (LanLimitConstant.LAN_LIMIT_ENABLE.equals(mState)) {
            if (mPlayerService.isMute()) {
                mPlayerService.toggleMute();
            }
        } else {
            if (!mPlayerService.isMute()) {
                mPlayerService.toggleMute();
            }
        }
    }

}
