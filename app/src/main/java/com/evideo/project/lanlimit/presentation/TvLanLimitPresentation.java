package com.evideo.project.lanlimit.presentation;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;

import com.evideo.project.lanlimit.R;
import com.evideostb.component.logger.EvLog;

/**
 * @ProjectName: evideo-lan-limit
 * @Package: com.evideo.project.lanlimit.presentation
 * @ClassName: TvLanLimitPresentation
 * @Description: java类作用描述
 * @Author: chentao
 * @CreateDate: 2020/4/17 15:27
 * @Version: 1.0
 */
public class TvLanLimitPresentation extends Presentation {

    private static final String TAG = TvLanLimitPresentation.class.getSimpleName();

    public TvLanLimitPresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EvLog.i(TAG, "onCreate()");
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
        getWindow().setBackgroundDrawableResource(R.color.transparent_background);
        getWindow().setWindowAnimations(R.style.dialog_anim_style);
        setContentView(R.layout.presentation_tv_lan_limit);
        setCancelable(false);
    }
}
