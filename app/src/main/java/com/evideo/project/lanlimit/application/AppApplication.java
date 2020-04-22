package com.evideo.project.lanlimit.application;

import android.app.Application;
import android.text.TextUtils;

import com.evideo.project.lanlimit.manager.LanLimitManager;
import com.evideostb.component.openplatform.playctrl.IPlayCtrl;
import com.evideostb.component.utils.application.ApplicationUtil;
import com.evideostb.servicemanager.component.IServiceBindListener;
import com.evideostb.servicemanager.component.ServiceManager;

/**
 * @ProjectName: evideo-lan-limit
 * @Package: com.evideo.project.lanlimit
 * @ClassName: AppApplication
 * @Description: java类作用描述
 * @Author: chentao
 * @CreateDate: 2020/4/13 16:55
 * @Version: 1.0
 */
public class AppApplication extends Application {

    private IServiceBindListener mListener = new IServiceBindListener() {
        @Override
        public void onServiceBind(String s, boolean b) {
            if (TextUtils.isEmpty(s) || !b) {
                return;
            }

            if (IPlayCtrl.class.getName().equals(s)) {
                LanLimitManager.getInstance().syncState();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        ApplicationUtil.initInstance(this);
        LanLimitManager.getInstance().init();
        ServiceManager.getInstance().attach(this);
        ServiceManager.getInstance().setServiceBindListener(mListener);
        ServiceManager.getInstance().bind(IPlayCtrl.class);
    }

}
