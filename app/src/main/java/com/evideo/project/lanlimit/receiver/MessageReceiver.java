package com.evideo.project.lanlimit.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.evideo.project.lanlimit.manager.LanLimitManager;
import com.evideo.project.lanlimit.service.LanLimitService;
import com.evideostb.component.logger.EvLog;
import com.evideostb.component.openplatform.BroadcastConstant;
import com.evideostb.component.openplatform.ModeConstant;
import com.evideostb.component.utils.configprovider.KeyName;
import com.evideostb.component.utils.configprovider.KmConfigManager;

/**
 * @ProjectName: evideo-lan-limit
 * @Package: com.evideo.project.lanlimit.receiver
 * @ClassName: MessageReceiver
 * @Description: java类作用描述
 * @Author: chentao
 * @CreateDate: 2020/4/16 15:05
 * @Version: 1.0
 */
public class MessageReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        if (BroadcastConstant.ACTION_CONTROL_CENTER_MODE_SWITCH.equals(intent.getAction())) {
            int mode = KmConfigManager.getInstance().getInt(KeyName.KEY_SUPER_DESKTOP_CURRENT_MODE, ModeConstant.MODE_KTV);
            if (mode != ModeConstant.MODE_KTV) {
                LanLimitManager.getInstance().uninit();
                context.stopService(new Intent(context, LanLimitService.class));
            }
        }
    }
}
