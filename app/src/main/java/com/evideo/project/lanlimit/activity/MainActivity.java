package com.evideo.project.lanlimit.activity;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.View;

import com.evideo.project.lanlimit.R;
import com.evideo.project.lanlimit.presentation.TvLanLimitPresentation;

/**
 * @ProjectName: evideo-lan-limit
 * @Package: com.evideo.project.lanlimit.activity
 * @ClassName: MainActivity
 * @Description: java类作用描述
 * @Author: chentao
 * @CreateDate: 2020/4/14 12:42
 * @Version: 1.0
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void btnClick(View view) {
        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        if (displayManager == null) {
            return;
        }

        Display[] presentationDisplays = displayManager.getDisplays();
        if (presentationDisplays.length > 1) {
            Display display = presentationDisplays[1];
            TvLanLimitPresentation presentation = new TvLanLimitPresentation(this, display);
            presentation.show();
        }
    }

    public void exitClick(View view) {
        finish();
    }
}
