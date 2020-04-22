package com.evideo.project.lanlimit.view;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.evideo.project.lanlimit.R;
import com.evideo.project.lanlimit.bean.DeviceBean;
import com.evideo.project.lanlimit.manager.LanLimitManager;
import com.evideostb.component.logger.EvLog;
import com.evideostb.component.utils.NetUtil;
import com.evideostb.component.utils.constant.LanLimitConstant;
import com.evideostb.uicomponent.dialog.BaseTypeDialog;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @ProjectName: evideo-lan-limit
 * @Package: com.evideo.project.lanlimit.view
 * @ClassName: DeviceDialog
 * @Description: java类作用描述
 * @Author: chentao
 * @CreateDate: 2020/4/15 9:32
 * @Version: 1.0
 */
public class DeviceDialog extends BaseTypeDialog {

    private static final String TAG = DeviceDialog.class.getSimpleName();

    private ImageView mCloseBtn;

    private ListView mListView;

    private Context mContext;

    public DeviceDialog(Context context) {
        super(context);
        mContext = context;
        initDialog();
        initView();
        initData();
    }

    private void initDialog() {
        Window dialogWindow = this.getWindow();
        dialogWindow.setGravity(Gravity.CENTER);
        dialogWindow.setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.y = -45;
        dialogWindow.setAttributes(lp);
        setCanceledOnTouchOutside(true);
    }

    private void initView() {
        setContentView(R.layout.dialog_device);

        mCloseBtn = (ImageView) findViewById(R.id.dialog_device_close_btn);
        mCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        mListView = (ListView) findViewById(R.id.dialog_device_list_view);
    }

    private void initData() {
        Set<DeviceBean> beans = LanLimitManager.getInstance().getDevices();
        if (LanLimitConstant.LAN_LIMIT_ENABLE.equals(LanLimitManager.getInstance().getState())) {
            DeviceBean device = new DeviceBean();
            device.setState(LanLimitConstant.LAN_LIMIT_ENABLE);
            device.setIp(NetUtil.getLocalIP().trim());
            beans.add(device);
        }

        if (beans == null || beans.size() == 0) {
            return;
        }

        List<Map<String, String>> list = new ArrayList<>();
        for (DeviceBean bean : beans) {
            if (bean == null) {
                continue;
            }

            Map<String, String> map = new HashMap<>();
            map.put("ip", "ip:  " + bean.getIp());
            map.put("state", "state:  " + bean.getState());
            list.add(map);
        }

        // 使用SimpleAdapter适配器
        ListAdapter listAdapter =
                new SimpleAdapter(mContext, list,
                        android.R.layout.simple_list_item_2,
                        new String[]{"ip", "state"},
                        new int[]{android.R.id.text1, android.R.id.text2});
        mListView.setAdapter(listAdapter);
    }
}
